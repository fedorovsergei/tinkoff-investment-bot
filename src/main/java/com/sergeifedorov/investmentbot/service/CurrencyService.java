package com.sergeifedorov.investmentbot.service;

import com.sergeifedorov.investmentbot.util.PropertyValues;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
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
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyService {

//    Стратегии на индикаторах технического анализа
//    Пример индикатора – скользящая средняя(MA). Это усредненная цена за заданный интервал времени.
//    Алгоритмом строятся две MA, на большом интервале(длинная) и малом (короткая).
//    В момент превышения длинной над короткой - робот продает, в обратном случае – покупает.
//    Конкретные значения большой и малой скользящей средней предлагается задавать в настройках алгоритма, либо рассчитывать на исторических данных.

    private final PropertyValues propertyValues;
    private InvestApi api;
    // Комиссия брокера
    private static final Double COMMISSION = 0.3;
    // Налог
    private static final Double TAX = 13.0;
    // Точность чисел
    private static final int SCALE = 9;
    // Частота среднего значения
    // Высокая частота замедляет работу программы, но дает высокую точность среднего значения
    private static final CandleInterval candleInterval = CandleInterval.CANDLE_INTERVAL_1_MIN;     //todo сделать пару между интервалом и сроком линий Cut
    // Уровень ожидаемого дохода %
    // Чем выше уровень, тем реже будут сделки
    private static final double TACTIC_LVL = 5.0;
    private static final double MIN_PROFIT = (COMMISSION * 2.0) + TACTIC_LVL;
    private static final double MIN_SALE_PROFIT = (COMMISSION * 2.0) + TAX + TACTIC_LVL;

    @PostConstruct
    public void postConstructor() {
        String token = propertyValues.getSecretToken();
//        String token = propertyValues.getSecretTokenSandbox();
        api = InvestApi.create(token);
    }

    @Scheduled(cron = "0 0/1 * * * *")
//    @Scheduled(cron = "0 0/5 * * * *")
    @SneakyThrows
    public void tradeTick() {
        LocalDateTime nowDateTime = LocalDateTime.now();
        propertyValues.getFigi().forEach(figi -> {
            double shortCut = getAverage(nowDateTime.minusMinutes(propertyValues.getShortPeriod()), candleInterval, figi).doubleValue();
            double longCut = getAverage(nowDateTime.minusMinutes(propertyValues.getLongPeriod()), candleInterval, figi).doubleValue();

            System.out.println(shortCut);
            System.out.println(longCut);
            System.out.println(getLastPrice(getLastPriceDTO(figi)));
            double difference = longCut / shortCut * 100 - 100;
            log.info(String.valueOf(difference));
            if (difference > propertyValues.getDifferenceValue()) {
                log.info("купили за " + (getLastPrice(getLastPriceDTO(figi))));
//                api.getOrdersService().postOrder(figi, Quotation()) todo покупка
            } else if (difference < propertyValues.getDifferenceValue() * -1) {
                log.info("продали за " + (getLastPrice(getLastPriceDTO(figi))));
            } else {
                log.info("Находимся в коридоре, сделок не было");
            }
        });
    }

    @SneakyThrows
    public BigDecimal getAverage(LocalDateTime start, CandleInterval interval, String figi) {
        ZonedDateTime zdt = start.atZone(ZoneId.systemDefault());
        List<HistoricCandle> historicCandles = api.getMarketDataService().getCandles(figi, zdt.toInstant(), Instant.now(), interval).get();
        BigDecimal sumCandles = historicCandles.stream().map(candle -> new BigDecimal(candle.getHigh().getUnits() + "." + candle.getHigh().getNano())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal countCandles = new BigDecimal(String.valueOf(historicCandles.size()));
        return sumCandles.setScale(SCALE, RoundingMode.HALF_EVEN).divide(countCandles, RoundingMode.HALF_EVEN);
    }

    @SneakyThrows
    private List<LastPrice> getLastPriceDTO(String figi) {
        return api.getMarketDataService().getLastPrices(Collections.singleton(figi)).get();
    }

    @SneakyThrows
    private BigDecimal getLastPrice(List<LastPrice> lastPrices) {
        return new BigDecimal(lastPrices.get(0).getPrice().getUnits() + "." + lastPrices.get(0).getPrice().getNano());
    }
}
