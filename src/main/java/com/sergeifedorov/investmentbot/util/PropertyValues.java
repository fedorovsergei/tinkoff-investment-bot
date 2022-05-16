package com.sergeifedorov.investmentbot.util;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@Getter
public class PropertyValues {

    @Value("${secret-token-sandbox}")
    private String secretTokenSandbox;
    @Value("${secret-token}")
    private String secretToken;
    @Value("${figi}")
    private Collection<String> figi;

}