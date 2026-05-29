package ru.cloudwithout.transferservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.client.WebClient;
import ru.cloudwithout.commonmodels.common.dto.NotificationRequest;

@SpringBootTest
class TransferServiceApplicationTests {

    @MockitoBean
    private WebClient accountsWebClient;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private KafkaTemplate<String, NotificationRequest> kafkaTemplate;

    @Test
    void contextLoads() {
    }

}
