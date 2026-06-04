package ru.cloudwithout.transferservice.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.cloudwithout.commonmodels.common.dto.CommonResponse;
import ru.cloudwithout.commonmodels.common.dto.NotificationRequest;

import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class AccountsClientTest {

    @RegisterExtension
    static WireMockExtension accountsService = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void registerAccountsBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("bank.accounts.base-url", accountsService::baseUrl);
    }

    @Autowired
    private AccountsClient accountsClient;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private KafkaTemplate<String, NotificationRequest> kafkaTemplate;

    @MockitoBean
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @Test
    void transferFallbackShouldReturnErrorWhenAccountsUnavailable() {
        accountsService.stubFor(post(urlPathEqualTo("/accounts/transfer"))
                .willReturn(aResponse().withStatus(503)));

        stubAuthorizedClient();

        CommonResponse response = accountsClient.transfer("test", 100, "alex");

        assertThat(response.getErrors())
                .containsExactly("Сервис аккаунтов временно недоступен, перевод не выполнен");
    }

    private void stubAuthorizedClient() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("transfer-service")
                .clientId("transfer-service")
                .clientSecret("test-secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("http://127.0.0.1:1/token")
                .build();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "access-token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );
        OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(
                registration,
                "transfer-service",
                accessToken
        );
        when(authorizedClientManager.authorize(any())).thenReturn(authorizedClient);
    }
}