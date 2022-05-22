package com.sergeifedorov.investmentbot.service;

import com.sergeifedorov.investmentbot.domain.entity.CandleHistory;
import com.sergeifedorov.investmentbot.domain.entity.TradeTest;
import com.sergeifedorov.investmentbot.repository.CandleHistoryRepo;
import com.sergeifedorov.investmentbot.repository.TradeTestRepo;
import com.sergeifedorov.investmentbot.repository.TradeTestResultRepo;
import com.sergeifedorov.investmentbot.util.PropertyValues;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.tinkoff.piapi.contract.v1.*;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@RestController
@RequestMapping("/test2")
@Slf4j
@RequiredArgsConstructor
@Transactional
class SandboxService {

    private static final int SCALE = 9;
    private final CandleHistoryRepo candleHistoryRepo;
    private final TradeTestResultRepo tradeTestResultRepo;
    private final PropertyValues propertyValues;
    private final TradeTestRepo tradeTestRepo;

    @GetMapping
    void testStrategy() {
        tradeTestResultRepo.deleteAll();
        List<CandleHistory> candleHistories = candleHistoryRepo.findAll();
        for (int i = 100; i < candleHistories.size(); i++) {
            CandleHistory candleHistory = candleHistories.get(i);

            double shortCut = getAverage(candleHistories.subList(i - 15, i)).doubleValue();
            double longCut = getAverage(candleHistories.subList(i - 30, i)).doubleValue();

            if (longCut != 0.00 && shortCut != 0.00) {
                double difference = longCut / shortCut * 100 - 100;

                if (difference > propertyValues.getDifferenceValue()) {
                    buy(candleHistory);

                } else if (difference < propertyValues.getDifferenceValue() * -1) {
                    sell(candleHistory);
                }
            }
        }
        log.info(String.valueOf(tradeTestRepo.getById(999999).getMoney()));
        log.info(String.valueOf(tradeTestRepo.getById(999999).getValue() * new BigDecimal(candleHistories.get(candleHistories.size() - 1).getUnit() + "." + candleHistories.get(candleHistories.size() - 1).getNano()).doubleValue()));
    }

    private void sell(CandleHistory candleHistory) {
        TradeTest tradeTest = tradeTestRepo.getById(999999);
        if (tradeTest.getValue() > 0) {
            tradeTest.setValue(tradeTest.getValue() - 1);
            tradeTest.setMoney(tradeTest.getMoney() + new BigDecimal(candleHistory.getUnit() + "." + candleHistory.getNano()).doubleValue());
            tradeTestRepo.save(tradeTest);
            PostOrderResponse.newBuilder().build();
        }
    }

    private void buy(CandleHistory candleHistory) {
        TradeTest tradeTest = tradeTestRepo.getById(999999);
        if (tradeTest.getMoney() >= new BigDecimal(candleHistory.getUnit() + "." + candleHistory.getNano()).doubleValue()) {
            tradeTest.setValue(tradeTest.getValue() + 1);
            tradeTest.setMoney(tradeTest.getMoney() - new BigDecimal(candleHistory.getUnit() + "." + candleHistory.getNano()).doubleValue());
            tradeTestRepo.save(tradeTest);
            PostOrderResponse.newBuilder().build();
        }
    }

    private BigDecimal getAverage(List<CandleHistory> historicCandles) {
        BigDecimal sumCandles = historicCandles.stream()
                .map(candle -> new BigDecimal(candle.getUnit() + "." + candle.getNano()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal countCandles = new BigDecimal(String.valueOf(historicCandles.size()));
        return sumCandles.setScale(SCALE, RoundingMode.HALF_EVEN).divide(countCandles, RoundingMode.HALF_EVEN);
    }
}
