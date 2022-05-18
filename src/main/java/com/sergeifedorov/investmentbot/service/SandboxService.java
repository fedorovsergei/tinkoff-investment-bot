package com.sergeifedorov.investmentbot.service;

import com.sergeifedorov.investmentbot.util.PropertyValues;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static ru.tinkoff.piapi.contract.v1.OrderType.ORDER_TYPE_MARKET;

@Service
@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/s")
public class SandboxService {

    private final PropertyValues propertyValues;
    private InvestApi apiSandBox;
    private InvestApi apiReal;
    private static final int SCALE = 9;
    private HistoricCandle historicCandleNow;

    @PostConstruct
    public void postConstructor() {
        apiSandBox = InvestApi.createSandbox(propertyValues.getSecretTokenSandbox());
        apiReal = InvestApi.create(propertyValues.getSecretToken());
    }

    @GetMapping
    @RequestMapping("/test1")
    @SneakyThrows
    public void start() {
//        System.out.println(apiSandBox.getSandboxService().getPositionsSync(getSandBoxAcc()));
//        apiSandBox.getSandboxService().payIn(getSandBoxAcc(), MoneyValue.newBuilder().setCurrency(Currency.getDefaultInstance().getCurrency()).setUnits(10000).build());
    }

    private String getSandBoxAcc() {
        return apiSandBox.getSandboxService().getAccountsSync().get(0).getId();
    }

    /**
     * Алгоритм анализа рынка и выставления заявок
     */
    @GetMapping
    @RequestMapping("/test2")
    public void testData() {
        log.info(apiSandBox.getSandboxService().getPortfolioSync(getSandBoxAcc()).toString());

        String figi = propertyValues.getFigis().stream().findFirst().get();
        List<HistoricCandle> history = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            List<HistoricCandle> list = apiReal.getMarketDataService()
                    .getCandlesSync(figi, LocalDateTime.now().minusDays(31 - i).atZone(ZoneId.systemDefault()).toInstant(),
                            LocalDateTime.now().minusDays(30 - i).atZone(ZoneId.systemDefault()).toInstant(), CandleInterval.CANDLE_INTERVAL_1_MIN);
            history.addAll(list);
        }
        history.sort(Comparator.comparing(historicCandle -> Instant
                .ofEpochSecond(historicCandle.getTime().getSeconds(), historicCandle.getTime().getNanos())
                .atZone(ZoneId.systemDefault())));

        for (int i = 50; i < history.size(); i++) {
            double shortCut = getAverage(history, i, propertyValues.getShortPeriod()).doubleValue();
            double longCut = getAverage(history, i, propertyValues.getLongPeriod()).doubleValue();

            if (longCut != 0.00 && shortCut != 0.00) {
                double difference = longCut / shortCut * 100 - 100;

                if (difference > propertyValues.getDifferenceValue()) {
                    if (!buy(figi, propertyValues.getBuySize()).getMessage().isEmpty()) {
                        log.info("купили за " + (getLastPrice()));
                    }
                } else if (difference < propertyValues.getDifferenceValue() * -1) {
                    if (!sell(figi, propertyValues.getBuySize()).getMessage().isEmpty()) {
                        log.info("продали за " + (getLastPrice()));
                    }
                } else {
                    log.info("Находимся в коридоре, сделок не было");
                }
                log.info("================");
            }
            if (i % 100 == 0) {
                log.info(apiSandBox.getSandboxService().getPortfolioSync(getSandBoxAcc()).toString());
            }
        }

        log.info(apiSandBox.getSandboxService().getPortfolioSync(getSandBoxAcc()).toString());
    }

    private BigDecimal getAverage(List<HistoricCandle> history, int count, int period) {

        List<HistoricCandle> subHistory = history.subList(count - period, count);
        historicCandleNow = history.get(count - 1);

        BigDecimal sumCandles = subHistory.stream()
                .map(candle -> new BigDecimal(candle.getHigh().getUnits() + "." + candle.getHigh().getNano()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal countCandles = new BigDecimal(String.valueOf(subHistory.size()));
        return sumCandles.setScale(SCALE, RoundingMode.HALF_EVEN).divide(countCandles, RoundingMode.HALF_EVEN);
    }

    private BigDecimal getLastPrice() {
        return new BigDecimal(historicCandleNow.getHigh().getUnits() + "." + historicCandleNow.getHigh().getNano());
    }

    private PostOrderResponse buy(String figi, int quantity) {
        return submittingApplication(OrderDirection.ORDER_DIRECTION_BUY, figi, quantity);
    }

    private PostOrderResponse sell(String figi, int quantity) {
        return submittingApplication(OrderDirection.ORDER_DIRECTION_SELL, figi, quantity);
    }

    @SneakyThrows
    private PostOrderResponse submittingApplication(OrderDirection operation, String figi, int quantity) {
        Quotation lastPrice = historicCandleNow.getHigh();
        Quotation quotation = Quotation.newBuilder().setUnits(lastPrice.getUnits() * quantity).setNano(lastPrice.getNano() * quantity).build();
        PostOrderResponse postOrderResponse = apiSandBox.getSandboxService().postOrderSync(figi, propertyValues.getBuySize(), quotation, operation,
                getSandBoxAcc(), ORDER_TYPE_MARKET, propertyValues.getOrderId());
        Thread.sleep(1000);
        return postOrderResponse;
    }
}
