package ru.cloudwithout.bankgateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                // Отключаем CSRF (для stateless API-шлюза не требуется)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // Настраиваем правила авторизации
                .authorizeExchange(exchanges -> exchanges
                        // Открываем доступ к /actuator/**
                        .pathMatchers("/actuator/**").permitAll()
                        // Остальные запросы требуют аутентификации
                        .anyExchange().authenticated()
                )
                // Настраиваем Gateway как Resource Server, который проверяет JWT
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
