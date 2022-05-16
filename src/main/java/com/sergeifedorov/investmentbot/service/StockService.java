package com.sergeifedorov.investmentbot.service;

import com.sergeifedorov.investmentbot.util.PropertyValues;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.tinkoff.piapi.contract.v1.Share;
import ru.tinkoff.piapi.core.InvestApi;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final PropertyValues propertyValues;
    private InvestApi api;

    @PostConstruct
    public void postConstructor() {
        String token = propertyValues.getSecretToken();
        api = InvestApi.create(token);
    }

    @SneakyThrows
    public List<Share> getAllStock() {
        return api.getInstrumentsService().getAllShares().get();
    }

}
