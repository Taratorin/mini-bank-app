package ru.cloudwithout.transferservice.client;

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
import java.util.List;

import static org.springframework.security.oauth2.client.web.ClientAttributes.clientRegistrationId;


@Component
@RequiredArgsConstructor
@Slf4j
public class AccountsClient {

    private final WebClient accountsWebClient;

    @Value("${bank.accounts.base-url}")
    private String accountsBaseUrl;

    @Retry(name = "accountsClient", fallbackMethod = "transferFallback")
    @CircuitBreaker(name = "accountsClient", fallbackMethod = "transferFallback")
    public CommonResponse transfer(String from, int value, String to) {
        URI uri = UriComponentsBuilder.fromUriString(accountsBaseUrl)
                .path("/accounts/transfer")
                .queryParam("from", from)
                .queryParam("value", value)
                .queryParam("to", to)
                .build()
                .toUri();
        log.info("Отправляем запрос на перевод денежных средств: от={}, сумма={}, к={}, адрес={}",
                from, value, to, uri);
        CommonResponse response = accountsWebClient
                .post()
                .uri(uri)
                .attributes(clientRegistrationId("transfer-service"))
                .retrieve()
                .bodyToMono(CommonResponse.class)
                .block();
        log.info("Получили ответ по операции со счетом пользователя {}", from);
        return response;
    }

    private CommonResponse transferFallback(String from, int value, String to, Throwable throwable) {
        log.warn("accounts-service временно недоступен: from={}, to={}, value={}", from, to, value, throwable);
        return CommonResponse.builder()
                .accounts(List.of())
                .errors(List.of("Сервис аккаунтов временно недоступен, перевод не выполнен"))
                .build();
    }
}