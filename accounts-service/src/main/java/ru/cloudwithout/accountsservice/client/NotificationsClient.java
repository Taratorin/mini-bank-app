package ru.cloudwithout.accountsservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.cloudwithout.accountsservice.model.NotificationRequest;

import java.net.URI;

import static org.springframework.security.oauth2.client.web.ClientAttributes.clientRegistrationId;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationsClient {

    private final WebClient notificationsWebClient;

    @Value("${bank.notifications.base-url}")
    private String notificationsBaseUrl;

    public void send(String operation, String message) {
        URI uri = UriComponentsBuilder.fromUriString(notificationsBaseUrl)
                .path("/notifications")
                .build()
                .toUri();
        NotificationRequest request = new NotificationRequest("accounts-service", operation, message);
        log.info("Отправляем уведомление в notifications-service: operation={}, message={}, uri={}", operation, message, uri);
        notificationsWebClient
                .post()
                .uri(uri)
                .attributes(clientRegistrationId("accounts-service"))
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .block();
        log.info("Уведомление отправлено: operation={}", operation);
    }
}