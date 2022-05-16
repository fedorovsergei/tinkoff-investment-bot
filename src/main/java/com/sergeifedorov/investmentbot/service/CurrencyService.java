package com.sergeifedorov.investmentbot.service;

import com.sergeifedorov.investmentbot.util.PropertyValues;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.Currency;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.tinkoff.piapi.core.InvestApi;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
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
    private static final CandleInterval candleInterval = CandleInterval.CANDLE_INTERVAL_1_MIN;
    //todo сделать пару между интервалом и сроком линий Cut

    // Кол-во одинаковых операций подряд (купил...купил...купил...)
    private static final short maxStreak = 2;
    private static final short countStreak = 0;

    // Уровень ожидаемого дохода %
    // Чем выше уровень, тем реже будут сделки
    private static final double tacticLvl = 5.0;

    private static final double minProfit = (COMMISSION * 3.0) + TAX + tacticLvl;



    public void tradeTick() {
        LocalDateTime nowDateTime = LocalDateTime.now();
        double shortCut = getAverage(nowDateTime.minusDays(1), nowDateTime, candleInterval).doubleValue();
        double longCut = getAverage(nowDateTime.minusDays(10), nowDateTime, candleInterval).doubleValue();

        if (longCut > (shortCut * minProfit)) {
            //продаем
            return;
        }
        if ((shortCut * minProfit) > longCut) {
            //покупаем
            return;
        }
        // Находимся в коридоре, сделок не было
    }

    @SneakyThrows
    public BigDecimal getAverage(LocalDateTime start, LocalDateTime end, CandleInterval interval) {
        List<HistoricCandle> historicCandles = api.getMarketDataService().getCandles(
                propertyValues.getFigi().stream().findFirst().get(), start.toInstant(ZoneOffset.UTC), end.toInstant(ZoneOffset.UTC), interval).get();

        BigDecimal sumCandles = historicCandles.stream()
                .map(candle -> new BigDecimal(candle.getHigh().getUnits() + "." + candle.getHigh().getNano()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal countCandles = new BigDecimal(String.valueOf(historicCandles.size()));

        return sumCandles.setScale(SCALE, RoundingMode.HALF_EVEN).divide(countCandles, RoundingMode.HALF_EVEN);
    }

    @PostConstruct
    public void postConstructor() {
//        String token = propertyValues.getSecretToken();
        String token = propertyValues.getSecretTokenSandbox();
        api = InvestApi.create(token);
    }

    @SneakyThrows
    public List<Currency> getAllCurrency() {
        return api.getInstrumentsService().getAllCurrencies().get();
    }

    @SneakyThrows
    public List<LastPrice> getLastPrice() {
        return api.getMarketDataService().getLastPrices(propertyValues.getFigi()).get();
    }
}
