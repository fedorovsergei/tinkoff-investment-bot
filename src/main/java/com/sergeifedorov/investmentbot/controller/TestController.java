package com.sergeifedorov.investmentbot.controller;

import com.sergeifedorov.investmentbot.service.AccountService;
import com.sergeifedorov.investmentbot.service.CurrencyService;
import com.sergeifedorov.investmentbot.service.StockService;
import com.sergeifedorov.investmentbot.util.PropertyValues;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.tinkoff.piapi.contract.v1.CandleInterval;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final AccountService accountService;
    private final StockService stockService;
    private final CurrencyService currencyService;
    private final PropertyValues propertyValues;

    @GetMapping
    @RequestMapping("/test1")
    public String test1() {
        return accountService.getActiveAccount().toString();
    }

    @GetMapping
    @RequestMapping("/test2")
    public void test2() {
        currencyService.tradeTick();
    }
//
//    @GetMapping
//    @RequestMapping("/test3")
//    public String test3() {
//        return currencyService.getLastPrice().toString();
//    }
//
//    @GetMapping
//    @RequestMapping("/test4")
//    public String test4() {
//        return currencyService.getAverage(LocalDateTime.now().minusDays(1), LocalDateTime.now(), CandleInterval.CANDLE_INTERVAL_1_MIN).toString();
//    }
}
