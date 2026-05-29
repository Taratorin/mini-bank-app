package ru.cloudwithout.transferservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

@Configuration
public class TestOAuthConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration transferService = ClientRegistration
                .withRegistrationId("transfer-service")
                .clientId("transfer-service")
                .clientSecret("test-secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("http://127.0.0.1:1/token")
                .build();
        return new InMemoryClientRegistrationRepository(transferService);
    }
}