package ru.cloudwithout.bankgateway;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class GatewayRoutingIntegrationTest {

    @RegisterExtension
    static WireMockExtension accountsService = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void registerRoutes(DynamicPropertyRegistry registry) {
        registry.add("ROUTES_ACCOUNTS_URI", accountsService::baseUrl);
    }

    @LocalServerPort
    private int port;

    @MockitoBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUpWebTestClient() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void shouldRouteAccountsRequestThroughGateway() {
        accountsService.stubFor(get(urlPathEqualTo("/accounts/login"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "login": "test",
                                  "firstLastName": "Иван Иванович",
                                  "birthDate": "1990-01-01",
                                  "sum": 100.00,
                                  "accounts": [],
                                  "errors": []
                                }
                                """)));

        when(reactiveJwtDecoder.decode(any())).thenReturn(Mono.just(
                org.springframework.security.oauth2.jwt.Jwt.withTokenValue("test-token")
                        .header("alg", "none")
                        .subject("gateway-test")
                        .claim("realm_access", Map.of("roles", List.of("SERVICE")))
                        .build()
        ));

        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/accounts/login")
                        .queryParam("login", "test")
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.login").isEqualTo("test")
                .jsonPath("$.sum").isEqualTo(100.00);

        accountsService.verify(1, getRequestedFor(urlPathEqualTo("/accounts/login")));
    }
}