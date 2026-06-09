package ru.cloudwithout.bankui.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.cloudwithout.commonmodels.common.dto.CommonResponse;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.security.oauth2.client.web.ClientAttributes.clientRegistrationId;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountsClient {

    private final WebClient gatewayWebClient;

    @Value("${bank.gateway.base-url}")
    private String gatewayBaseUrl;

    @CircuitBreaker(name = "accountsClient", fallbackMethod = "getAccountByLoginFallback")
    @Retry(name = "accountsClient", fallbackMethod = "getAccountByLoginFallback")
    public CommonResponse getAccountByLogin(String login) {
        URI uri = UriComponentsBuilder.fromUriString(gatewayBaseUrl)
                .path("/accounts/login")
                .queryParam("login", login)
                .build()
                .toUri();
        log.info("Запрашиваем данные аккаунта пользователя {} по адресу {}", login, uri);
        CommonResponse response = gatewayWebClient
                .get()
                .uri(uri)
                .attributes(clientRegistrationId("accounts-service"))
                .retrieve()
                .bodyToMono(CommonResponse.class)
                .block(Duration.ofSeconds(5));
        log.info("Получили ответ по аккаунту пользователя {}", login);
        return response;
    }

    @CircuitBreaker(name = "accountsClient", fallbackMethod = "editAccountFallback")
    @Retry(name = "accountsClient", fallbackMethod = "editAccountFallback")
    public CommonResponse editAccount(String login, String name, LocalDate birthdate) {
        log.info("Отправляем запрос на изменение профиля пользователя {}", login);
        CommonResponse response = gatewayWebClient
                .post()
                .uri(gatewayBaseUrl + "/accounts/edit" + "?login=" + login + "&name=" + name + "&birthdate=" + birthdate)
                .attributes(clientRegistrationId("accounts-service"))
                .retrieve()
                .bodyToMono(CommonResponse.class)
                .block(Duration.ofSeconds(5));
        log.info("Получили ответ после изменения профиля пользователя {}", login);
        return response;
    }

    private CommonResponse getAccountByLoginFallback(String login, Throwable throwable) {
        log.warn("accounts-service временно недоступен при чтении профиля {}", login, throwable);
        return CommonResponse.builder()
                .accounts(List.of())
                .errors(List.of("Сервис аккаунтов временно недоступен"))
                .build();
    }

    private CommonResponse editAccountFallback(String login, String name, LocalDate birthdate, Throwable throwable) {
        log.warn("accounts-service временно недоступен при изменении профиля {}", login, throwable);
        return CommonResponse.builder()
                .accounts(List.of())
                .errors(List.of("Сервис аккаунтов временно недоступен, изменения не сохранены"))
                .build();
    }
}