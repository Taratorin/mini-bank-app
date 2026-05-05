package ru.cloudwithout.bankui.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.cloudwithout.bankui.model.CommonResponse;

import java.net.URI;
import java.time.LocalDate;

import static org.springframework.security.oauth2.client.web.ClientAttributes.clientRegistrationId;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountsClient {

    private final WebClient gatewayWebClient;

    @Value("${bank.gateway.base-url}")
    private String gatewayBaseUrl;

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
                .block();
        log.info("Получили ответ по аккаунту пользователя {}", login);
        return response;
    }

    public CommonResponse editAccount(String login, String name, LocalDate birthdate) {
        URI uri = UriComponentsBuilder.fromUriString(gatewayBaseUrl)
                .path("/accounts/edit")
                .queryParam("login", login)
                .queryParam("name", name)
                .queryParam("birthdate", birthdate)
                .build()
                .toUri();
        log.info("Отправляем запрос на изменение профиля: login={}, имя={}, дата рождения={}, адрес={}",
                login, name, birthdate, uri);
        CommonResponse response = gatewayWebClient
                .post()
                .uri(uri)
                .attributes(clientRegistrationId("accounts-service"))
                .retrieve()
                .bodyToMono(CommonResponse.class)
                .block();
        log.info("Получили ответ после изменения профиля пользователя {}", login);
        return response;
    }
}
