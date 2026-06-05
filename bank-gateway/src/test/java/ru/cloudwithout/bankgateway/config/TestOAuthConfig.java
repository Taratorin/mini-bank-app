package ru.cloudwithout.bankgateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

@Configuration
@Profile({"test", "it"})
public class TestOAuthConfig {

    @Bean
    public ReactiveClientRegistrationRepository reactiveClientRegistrationRepository() {
        return new InMemoryReactiveClientRegistrationRepository(
                clientRegistration("accounts-service"),
                clientRegistration("cash-service"),
                clientRegistration("transfer-service")
        );
    }

    private static ClientRegistration clientRegistration(String clientId) {
        return ClientRegistration.withRegistrationId(clientId)
                .clientId(clientId)
                .clientSecret("test-secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("http://127.0.0.1:1/token")
                .build();
    }
}