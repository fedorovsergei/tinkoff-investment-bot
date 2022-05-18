package com.sergeifedorov.investmentbot.controller;

import com.sergeifedorov.investmentbot.service.AccountService;
import com.sergeifedorov.investmentbot.service.TradeService;
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
    private final TradeService tradeService;
    private final PropertyValues propertyValues;

    @GetMapping
    @RequestMapping("/get-all-shares")
    public String getAllShares() {
        return stockService.getAllShares().toString();
    }
}
