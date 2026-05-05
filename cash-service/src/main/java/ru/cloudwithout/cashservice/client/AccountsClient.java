package ru.cloudwithout.cashservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.cloudwithout.cashservice.model.CommonResponse;
import ru.cloudwithout.cashservice.model.dto.CashAction;

import java.net.URI;

import static org.springframework.security.oauth2.client.web.ClientAttributes.clientRegistrationId;


@Component
@RequiredArgsConstructor
@Slf4j
public class AccountsClient {

    private final WebClient accountsWebClient;

    @Value("${bank.accounts.base-url}")
    private String accountsBaseUrl;

    public CommonResponse editCash(String login, int value, CashAction action) {
        URI uri = UriComponentsBuilder.fromUriString(accountsBaseUrl)
                .path("/accounts/cash")
                .queryParam("login", login)
                .queryParam("value", value)
                .queryParam("action", action)
                .build()
                .toUri();
        log.info("Отправляем запрос в accounts-service: login={}, сумма={}, действие={}, адрес={}",
                login, value, action, uri);
        CommonResponse response = accountsWebClient
                .post()
                .uri(uri)
                .attributes(clientRegistrationId("accounts-service"))
                .retrieve()
                .bodyToMono(CommonResponse.class)
                .block();
        log.info("Получили ответ от accounts-service для пользователя {}", login);
        return response;
    }
}
