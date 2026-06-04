package ru.cloudwithout.transferservice.support;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public final class SecurityTestSupport {

    private SecurityTestSupport() {
    }

    public static void stubJwtDecoder(JwtDecoder jwtDecoder, String... roles) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("test-user")
                .claim("realm_access", Map.of("roles", List.of(roles)))
                .build();
        when(jwtDecoder.decode(anyString())).thenReturn(jwt);
    }

    public static RequestPostProcessor bearerToken() {
        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer test-token");
            return request;
        };
    }
}