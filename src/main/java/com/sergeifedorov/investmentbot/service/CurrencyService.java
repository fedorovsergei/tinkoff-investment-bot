package com.sergeifedorov.investmentbot.service;

import com.sergeifedorov.investmentbot.util.PropertyValues;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static ru.tinkoff.piapi.contract.v1.OrderType.ORDER_TYPE_MARKET;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyService {

    private final PropertyValues propertyValues;
    private final AccountService accountService;
    private InvestApi api;
    private static final int SCALE = 9;

    @PostConstruct
    public void postConstructor() {
        String token = propertyValues.getSecretToken();
//        String token = propertyValues.getSecretTokenSandbox();
        api = InvestApi.create(token);
    }

    /**
     * Алгоритм анализа рынка и выставления заявок
     */
    @Scheduled(cron = "0 0/1 * * * *")
    public void tradeTick() {
        LocalDateTime nowDateTime = LocalDateTime.now();
        propertyValues.getFigis().forEach(figi -> {
            double shortCut = getAverage(nowDateTime.minusMinutes(propertyValues.getShortPeriod()), figi).doubleValue();
            double longCut = getAverage(nowDateTime.minusMinutes(propertyValues.getLongPeriod()), figi).doubleValue();

            if (longCut != 0.00 && shortCut != 0.00) {
                double difference = longCut / shortCut * 100 - 100;

                log.info("================");
                log.info("короткое значение: " + shortCut);
                log.info("длинное значение: " + longCut);
                log.info("разница значений: " + difference);

                if (difference > propertyValues.getDifferenceValue()) {
                    log.info("купили за " + (getLastPriceValue(getLastPrice(figi))));
                    log.info(buy(figi, propertyValues.getBuySize()).getMessage());
                } else if (difference < propertyValues.getDifferenceValue() * -1) {
                    log.info("продали за " + (getLastPriceValue(getLastPrice(figi))));
                    log.info(sell(figi, propertyValues.getBuySize()).getMessage());
                } else {
                    log.info("Находимся в коридоре, сделок не было");
                }
                log.info("================");
            }
        });
    }

    private BigDecimal getAverage(LocalDateTime start, String figi) {
        List<HistoricCandle> historicCandles = api.getMarketDataService()
                .getCandlesSync(figi, start.atZone(ZoneId.systemDefault()).toInstant(), Instant.now(), CandleInterval.CANDLE_INTERVAL_1_MIN);
        if (historicCandles.isEmpty()) {
            return BigDecimal.valueOf(0.00);
        }
        BigDecimal sumCandles = historicCandles.stream()
                .map(candle -> new BigDecimal(candle.getHigh().getUnits() + "." + candle.getHigh().getNano()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal countCandles = new BigDecimal(String.valueOf(historicCandles.size()));
        return sumCandles.setScale(SCALE, RoundingMode.HALF_EVEN).divide(countCandles, RoundingMode.HALF_EVEN);
    }

    private LastPrice getLastPrice(String figi) {
        return api.getMarketDataService().getLastPricesSync(Collections.singleton(figi)).get(0);
    }

    private BigDecimal getLastPriceValue(LastPrice lastPrices) {
        return new BigDecimal(lastPrices.getPrice().getUnits() + "." + lastPrices.getPrice().getNano());
    }

    private PostOrderResponse buy(String figi, int quantity) {
        return submittingApplication(OrderDirection.ORDER_DIRECTION_BUY, figi, quantity);
    }

    private PostOrderResponse sell(String figi, int quantity) {
        return submittingApplication(OrderDirection.ORDER_DIRECTION_SELL, figi, quantity);
    }

    private PostOrderResponse submittingApplication(OrderDirection operation, String figi, int quantity) {
        Quotation lastPrice = getLastPrice(figi).getPrice();
        Quotation quotation = Quotation.newBuilder().setUnits(lastPrice.getUnits() * quantity).setNano(lastPrice.getNano() * quantity).build();
        return api.getOrdersService().postOrderSync(figi, quantity, quotation, operation,
                accountService.getActiveAccount().getId(), ORDER_TYPE_MARKET, propertyValues.getOrderId());
    }
}
