package com.sergeifedorov.investmentbot.service;

import com.sergeifedorov.investmentbot.util.PropertyValues;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;

import javax.annotation.PostConstruct;
import javax.security.auth.login.AccountNotFoundException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final PropertyValues propertyValues;
    private InvestApi api;

    @PostConstruct
    public void postConstructor() {
        String token = propertyValues.getSecretToken();
        api = InvestApi.create(token);
    }

    @SneakyThrows
    public Account getActiveAccount() {
        Optional<Account> optional = api.getUserService().getAccounts().get().stream()
                .filter(account -> account.getStatus().equals(AccountStatus.ACCOUNT_STATUS_OPEN)
                        && account.getType().equals(AccountType.ACCOUNT_TYPE_TINKOFF))
                .findFirst();
        if (optional.isEmpty()) throw new AccountNotFoundException();
        return optional.get();
    }

    @SneakyThrows
    public List<Asset> getActive() {
        return api.getInstrumentsService().getAssets().get();
    }

    @SneakyThrows
    public GetInfoResponse getInfo() {
        return api.getUserService().getInfo().get();
    }
}
