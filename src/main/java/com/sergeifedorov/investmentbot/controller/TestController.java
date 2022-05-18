package com.sergeifedorov.investmentbot.controller;

import com.sergeifedorov.investmentbot.service.AccountService;
import com.sergeifedorov.investmentbot.service.CurrencyService;
import com.sergeifedorov.investmentbot.service.StockService;
import com.sergeifedorov.investmentbot.util.PropertyValues;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping
    @RequestMapping("/test3")
    public String test3() {
        return currencyService.getAllCurrencies().toString();
    }

    @GetMapping
    @RequestMapping("/test4")
    public void test4() {
        currencyService.buy();
    }

    @GetMapping
    @RequestMapping("/test5")
    public void test5() {
        currencyService.sell();
    }

    @GetMapping
    @RequestMapping("/test6")
    public String test6() {
       return accountService.getInfo().toString();
    }
}
