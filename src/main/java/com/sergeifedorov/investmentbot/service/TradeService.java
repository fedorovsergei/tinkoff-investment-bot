package com.sergeifedorov.investmentbot.service;

import com.sergeifedorov.investmentbot.repository.CandleHistoryRepo;
import com.sergeifedorov.investmentbot.repository.TradeTestResultRepo;
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
public class TradeService {

    private final PropertyValues propertyValues;
    private final AccountService accountService;
    private InvestApi api;
    private static final int SCALE = 9;
    private final CandleHistoryRepo candleHistoryRepo;
    private final TradeTestResultRepo tradeTestResultRepo;

    @PostConstruct
    public void postConstructor() {
        String token = propertyValues.getSecretToken();
        api = InvestApi.create(token);
    }

    /**
     * Алгоритм анализа рынка и выставления заявок
     */
    @Scheduled(cron = "${time-update}")
    public void tradeTick() {
        log.info("start trade tick");
        propertyValues.getFigis().forEach(figi -> {
            double shortCut = getShortAverage(figi).doubleValue();
            double longCut = getLongAverage(figi).doubleValue();

            log.info(String.valueOf(shortCut));
            log.info(String.valueOf(longCut));
            System.out.println(shortCut);
            System.out.println(longCut);
            if (longCut != 0.00 && shortCut != 0.00) {
                double difference = longCut / shortCut * 100 - 100;
                System.out.println(difference);
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

    public BigDecimal getLongAverage(String figi) {
        LocalDateTime start = LocalDateTime.now().minusMinutes(propertyValues.getShortPeriod());
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

    public BigDecimal getShortAverage(String figi) {
        LocalDateTime start = LocalDateTime.now().minusMinutes(propertyValues.getLongPeriod());
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

    public LastPrice getLastPrice(String figi) {
        return api.getMarketDataService().getLastPricesSync(Collections.singleton(figi)).get(0);
    }

    public BigDecimal getLastPriceValue(LastPrice lastPrices) {
        return new BigDecimal(lastPrices.getPrice().getUnits() + "." + lastPrices.getPrice().getNano());
    }

    public PostOrderResponse buy(String figi, int quantity) {
        return submittingApplication(OrderDirection.ORDER_DIRECTION_BUY, figi, quantity);
    }

    public PostOrderResponse sell(String figi, int quantity) {
        return submittingApplication(OrderDirection.ORDER_DIRECTION_SELL, figi, quantity);
    }

    public PostOrderResponse submittingApplication(OrderDirection operation, String figi, int quantity) {
        Quotation lastPrice = getLastPrice(figi).getPrice();
        Quotation quotation = Quotation.newBuilder().setUnits(lastPrice.getUnits() * quantity).setNano(lastPrice.getNano() * quantity).build();
        return api.getOrdersService().postOrderSync(figi, quantity, quotation, operation,
                accountService.getActiveAccount().getId(), ORDER_TYPE_MARKET, propertyValues.getOrderId());
    }
}
