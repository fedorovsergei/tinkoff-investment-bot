package com.sergeifedorov.investmentbot;

import com.sergeifedorov.investmentbot.domain.entity.CandleHistory;
import com.sergeifedorov.investmentbot.domain.entity.TradeTestResult;
import com.sergeifedorov.investmentbot.repository.CandleHistoryRepo;
import com.sergeifedorov.investmentbot.repository.TradeTestResultRepo;
import com.sergeifedorov.investmentbot.service.TradeService;
import com.sergeifedorov.investmentbot.util.PropertyValues;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@Slf4j
class InvestmentBotApplicationTests {

    @MockBean
    private InvestApi api;

    @MockBean
    private TradeService tradeService;
    private static final int SCALE = 9;

    @Autowired
    private CandleHistoryRepo candleHistoryRepo;

    @Autowired
    private TradeTestResultRepo tradeTestResultRepo;

    @MockBean
    private PropertyValues propertyValues;

    @Test
    void contextLoads() {
        List<CandleHistory> candleHistories = candleHistoryRepo.findAll();
        for (int i = 100; i < candleHistories.size(); i++) {
            CandleHistory candleHistory = candleHistories.get(i);
            when(tradeService.getLastPrice(anyString())).thenReturn(getLastPrice(candleHistory));
            when(propertyValues.getFigis()).thenReturn(Collections.singleton(getFigi()));
            when(propertyValues.getDifferenceValue()).thenReturn(0.3);
            when(propertyValues.getBuySize()).thenReturn(1);
            when(tradeService.getShortAverage(any())).thenReturn(getAverage(candleHistories.subList(i - 15, i)));
            when(tradeService.getLongAverage(any())).thenReturn(getAverage(candleHistories.subList(i - 30, i)));
            when(tradeService.buy(any(), anyInt())).thenReturn(buy(candleHistories.get(i)));
            when(tradeService.sell(any(), anyInt())).thenReturn(sell(candleHistories.get(i)));

            tradeService.tradeTick();
        }

        List<TradeTestResult> all = tradeTestResultRepo.findAll();
        final BigDecimal[] result = {new BigDecimal(0)};
        all.forEach(tradeTestResult -> {
            if (tradeTestResult.isBuy()) {
                result[0] = result[0].add(new BigDecimal(tradeTestResult.getUnit() + "." + tradeTestResult.getNano()));
            }
            if (!tradeTestResult.isBuy()) {
                result[0] = result[0].subtract(new BigDecimal(tradeTestResult.getUnit() + "." + tradeTestResult.getNano()));
            }
        });
        log.info(String.valueOf(result[0].doubleValue()));
        assertTrue(result[0].doubleValue() > 0);
    }

    private PostOrderResponse sell(CandleHistory candleHistory) {
        tradeTestResultRepo.save(TradeTestResult.builder().date(candleHistory.getDate()).figi(candleHistory.getFigi())
                .isBuy(false).unit(candleHistory.getUnit()).nano(candleHistory.getNano()).build());
        return PostOrderResponse.newBuilder().build();
    }

    private PostOrderResponse buy(CandleHistory candleHistory) {
        tradeTestResultRepo.save(TradeTestResult.builder().date(candleHistory.getDate()).figi(candleHistory.getFigi())
                .isBuy(true).unit(candleHistory.getUnit()).nano(candleHistory.getNano()).build());
        return PostOrderResponse.newBuilder().build();
    }

    private BigDecimal getAverage(List<CandleHistory> historicCandles) {
        BigDecimal sumCandles = historicCandles.stream()
                .map(candle -> new BigDecimal(candle.getUnit() + "." + candle.getNano()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal countCandles = new BigDecimal(String.valueOf(historicCandles.size()));
        return sumCandles.setScale(SCALE, RoundingMode.HALF_EVEN).divide(countCandles, RoundingMode.HALF_EVEN);
    }

    LastPrice getLastPrice(CandleHistory candleHistory) {
        log.info("start generate lastPrice");
        Quotation quotation = Quotation.newBuilder().setUnits(candleHistory.getUnit()).setNano(candleHistory.getNano()).build();
        return LastPrice.newBuilder().setPrice(quotation).setFigi(candleHistory.getFigi()).build();
    }

    private String getFigi() {
        return "BBG004S68CP5";
    }

}
