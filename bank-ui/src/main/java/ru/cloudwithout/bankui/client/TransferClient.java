package ru.cloudwithout.bankui.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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
public class TransferClient {

    private final WebClient gatewayWebClient;

    @Value("${bank.gateway.base-url}")
    private String gatewayBaseUrl;

    @CircuitBreaker(name = "transferClient", fallbackMethod = "transferFallback")
    public CommonResponse transfer(String from, int value, String to) {
        URI uri = UriComponentsBuilder.fromUriString(gatewayBaseUrl)
                .path("/transfer")
                .queryParam("from", from)
                .queryParam("value", value)
                .queryParam("to", to)
                .build()
                .toUri();
        log.info("Отправляем запрос на перевод денежных средств: от={}, сумма={}, к={}, адрес={}",
                from, value, to, uri);
        CommonResponse response = gatewayWebClient
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
        log.warn("transfer-service временно недоступен: from={}, to={}, value={}", from, to, value, throwable);
        CommonResponse response = new CommonResponse(List.of());
        response.setErrors(List.of("Сервис переводов временно недоступен, перевод не выполнен"));
        return response;
    }
}