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
    //%
    private Double commission = 0.3;
    //%
    private Double tax = 13.0;

    @SneakyThrows
    public BigDecimal getAverage(LocalDateTime start, LocalDateTime end, CandleInterval interval) {
        List<HistoricCandle> historicCandles = api.getMarketDataService().getCandles(propertyValues.getFigi().stream().findFirst().get(), start.toInstant(ZoneOffset.UTC), end.toInstant(ZoneOffset.UTC), interval).get();
        List<BigDecimal> collect = historicCandles.stream()
                .map(candle -> new BigDecimal(candle.getHigh().getUnits() + "." + candle.getHigh().getNano()))
                .toList();
        BigDecimal average = new BigDecimal(0);
        for (BigDecimal b : collect) {
            average = average.add(b);
        }
        BigDecimal result = average.divide(new BigDecimal(String.valueOf(collect.size())), new MathContext(9, RoundingMode.HALF_EVEN));

        return result;
    }

    public void startTrade() {
        double shortPrice = getAverage(LocalDateTime.now().minusDays(1), LocalDateTime.now(), CandleInterval.CANDLE_INTERVAL_1_MIN).doubleValue();
        double longPrice = getAverage(LocalDateTime.now().minusDays(10), LocalDateTime.now(), CandleInterval.CANDLE_INTERVAL_1_MIN).doubleValue();
        if (longPrice > (shortPrice + tax) / 100 * 103) {
            //продаем
        }
        else if (longPrice / 100 * 103 < shortPrice) {
            //покупаем
        }
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
