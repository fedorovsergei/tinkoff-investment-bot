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

    // Уровень ожидаемого дохода %
    // Чем выше уровень, тем реже будут сделки
    private static final double TACTIC_LVL = 5.0;

    private static final double MIN_PROFIT = (COMMISSION * 2.0) + TACTIC_LVL;
    private static final double MIN_SALE_PROFIT = (COMMISSION * 2.0) + TAX + TACTIC_LVL;

    public void tradeTick() {
        LocalDateTime nowDateTime = LocalDateTime.now();
        double shortCut = getAverage(nowDateTime.minusHours(1), nowDateTime, candleInterval).doubleValue();
        double longCut = getAverage(nowDateTime.minusHours(10), nowDateTime, candleInterval).doubleValue();

        double blabla = longCut / shortCut * 100 - 100;
        if (blabla > 0.3) {

            //покупаем
            return;
        }
        if (blabla < -0.3) {

            //продаем
            return;
        }
        System.out.println("Находимся в коридоре, сделок не было");
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
