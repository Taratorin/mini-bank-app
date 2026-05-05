package ru.cloudwithout.bankui.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.cloudwithout.bankui.model.dto.CashAction;
import ru.cloudwithout.bankui.model.CommonResponse;

import java.net.URI;

import static org.springframework.security.oauth2.client.web.ClientAttributes.clientRegistrationId;

@Component
@RequiredArgsConstructor
@Slf4j
public class CashClient {

    private final WebClient gatewayWebClient;

    @Value("${bank.gateway.base-url}")
    private String gatewayBaseUrl;

    public CommonResponse editCash(String login, int value, CashAction action) {
        URI uri = UriComponentsBuilder.fromUriString(gatewayBaseUrl)
                .path("/cash")
                .queryParam("login", login)
                .queryParam("value", value)
                .queryParam("action", action)
                .build()
                .toUri();
        log.info("Отправляем запрос по операции со счетом: login={}, сумма={}, действие={}, адрес={}",
                login, value, action, uri);
        CommonResponse response = gatewayWebClient
                .post()
                .uri(uri)
                .attributes(clientRegistrationId("cash-service"))
                .retrieve()
                .bodyToMono(CommonResponse.class)
                .block();
        log.info("Получили ответ по операции со счетом пользователя {}", login);
        return response;
    }
}