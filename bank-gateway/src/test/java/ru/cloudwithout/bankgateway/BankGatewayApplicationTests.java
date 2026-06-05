package ru.cloudwithout.bankgateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class BankGatewayApplicationTests {

    @MockitoBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    @Test
    void contextLoads() {
    }

}
