package ru.cloudwithout.loadgenerator.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final LoadGeneratorProperties properties;
    private final OAuth2AuthorizedClientManager authorizedClientManager;

    @Bean
    @Qualifier("accountsGatewayWebClient")
    WebClient accountsGatewayWebClient() {
        return buildGatewayWebClient(oauth2Filter("accounts-service"));
    }

    @Bean
    @Qualifier("cashGatewayWebClient")
    WebClient cashGatewayWebClient() {
        return buildGatewayWebClient(oauth2Filter("cash-service"));
    }

    @Bean
    @Qualifier("transferGatewayWebClient")
    WebClient transferGatewayWebClient() {
        return buildGatewayWebClient(oauth2Filter("transfer-service"));
    }

    private WebClient buildGatewayWebClient(ExchangeFilterFunction oauth2Filter) {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("load-generator")
                .maxConnections(512)
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectTimeoutMs())
                .responseTimeout(Duration.ofSeconds(properties.getResponseTimeoutSec()))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(properties.getResponseTimeoutSec()))
                        .addHandlerLast(new WriteTimeoutHandler(properties.getResponseTimeoutSec())));

        return WebClient.builder()
                .baseUrl(properties.getGatewayBaseUrl())
                .filter(oauth2Filter)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    private ExchangeFilterFunction oauth2Filter(String clientRegistrationId) {
        return (request, next) -> Mono.fromCallable(() -> {
                    OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                            .withClientRegistrationId(clientRegistrationId)
                            .principal("load-generator")
                            .build();
                    OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);
                    if (authorizedClient == null) {
                        throw new IllegalStateException(
                                "Не удалось получить OAuth2 token для " + clientRegistrationId);
                    }
                    return ClientRequest.from(request)
                            .headers(headers -> headers.setBearerAuth(
                                    authorizedClient.getAccessToken().getTokenValue()))
                            .build();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(next::exchange);
    }
}