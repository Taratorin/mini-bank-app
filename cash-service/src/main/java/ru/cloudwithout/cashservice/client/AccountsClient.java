package ru.cloudwithout.cashservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.cloudwithout.commonmodels.common.dto.CashAction;
import ru.cloudwithout.commonmodels.common.dto.CommonResponse;

import java.net.URI;
import java.util.List;

import static org.springframework.security.oauth2.client.web.ClientAttributes.clientRegistrationId;


@Component
@RequiredArgsConstructor
@Slf4j
public class AccountsClient {

    private final WebClient accountsWebClient;

    @Value("${bank.accounts.base-url}")
    private String accountsBaseUrl;

    @CircuitBreaker(name = "accountsClient", fallbackMethod = "editCashFallback")
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

    private CommonResponse editCashFallback(String login, int value, CashAction action, Throwable throwable) {
        log.warn("accounts-service временно недоступен: login={}, value={}, action={}", login, value, action, throwable);
        CommonResponse response = new CommonResponse(List.of());
        response.setErrors(List.of("Сервис аккаунтов временно недоступен, операция не выполнена"));
        return response;
    }
}
