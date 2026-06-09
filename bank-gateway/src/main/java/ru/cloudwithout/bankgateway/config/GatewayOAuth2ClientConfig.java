package ru.cloudwithout.bankgateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayOAuth2ClientConfig {

    @Bean
    public ReactiveOAuth2AuthorizedClientManager reactiveOAuth2AuthorizedClientManager(
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            ReactiveOAuth2AuthorizedClientService authorizedClientService) {

        ReactiveOAuth2AuthorizedClientProvider jwtPassthroughProvider = context -> {
            Authentication principal = context.getPrincipal();
            if (!(principal instanceof JwtAuthenticationToken jwtAuth)) {
                return Mono.empty();
            }

            var registration = context.getClientRegistration();
            if (registration == null) {
                return Mono.empty();
            }

            var jwt = jwtAuth.getToken();
            OAuth2AccessToken accessToken = new OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    jwt.getTokenValue(),
                    jwt.getIssuedAt(),
                    jwt.getExpiresAt());
            return Mono.just(new org.springframework.security.oauth2.client.OAuth2AuthorizedClient(
                    registration, principal.getName(), accessToken));
        };

        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientService);
        manager.setAuthorizedClientProvider(jwtPassthroughProvider);
        return manager;
    }

    @Bean
    public ReactiveOAuth2AuthorizedClientService reactiveOAuth2AuthorizedClientService(
            ReactiveClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrationRepository);
    }
}