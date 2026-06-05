package ru.cloudwithout.transferservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import io.micrometer.core.instrument.MeterRegistry;
import ru.cloudwithout.commonmodels.common.dto.CommonResponse;
import ru.cloudwithout.transferservice.client.AccountsClient;
import ru.cloudwithout.transferservice.kafka.NotificationKafkaProducer;
import ru.cloudwithout.transferservice.support.SecurityTestSupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.cloudwithout.transferservice.support.SecurityTestSupport.bearerToken;

@SpringBootTest
@AutoConfigureMockMvc
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry meterRegistry;

    @MockitoBean
    private AccountsClient accountsClient;

    @MockitoBean
    private NotificationKafkaProducer notificationKafkaProducer;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUpServiceRole() {
        SecurityTestSupport.stubJwtDecoder(jwtDecoder, "SERVICE");
    }

    @Test
    void transferShouldReturnAccountsServiceResponse() throws Exception {
        when(accountsClient.transfer("test", 100, "alex")).thenReturn(response("test", "900.00"));

        mockMvc.perform(post("/transfer")
                        .with(bearerToken())
                        .param("from", "test")
                        .param("to", "alex")
                        .param("value", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sum").value(900.00));

        verify(notificationKafkaProducer).send("test", "transfer", "Обработан запрос transfer-service: from=test, to=alex, value=100");
    }

    @Test
    void transferFailureShouldIncrementMetric() throws Exception {
        CommonResponse response = response("test", "900.00");
        response.setErrors(List.of("Недостаточно средств на счёте"));
        when(accountsClient.transfer("test", 100, "alex")).thenReturn(response);

        mockMvc.perform(post("/transfer")
                        .with(bearerToken())
                        .param("from", "test")
                        .param("to", "alex")
                        .param("value", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[0]").value("Недостаточно средств на счёте"));

        assertThat(meterRegistry.get("bank.transfer.failed")
                .tag("from", "test")
                .tag("to", "alex")
                .counter()
                .count()).isEqualTo(1.0);
    }

    @Test
    void transferShouldBeForbiddenWithoutServiceRole() throws Exception {
        SecurityTestSupport.stubJwtDecoder(jwtDecoder, "USER");

        mockMvc.perform(post("/transfer")
                        .with(bearerToken())
                        .param("from", "test")
                        .param("to", "alex")
                        .param("value", "100"))
                .andExpect(status().isForbidden());
    }

    @Test
    void transferShouldBeUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(post("/transfer")
                        .param("from", "test")
                        .param("to", "alex")
                        .param("value", "100"))
                .andExpect(status().isUnauthorized());
    }

    private CommonResponse response(String login, String sum) {
        CommonResponse response = new CommonResponse(List.of());
        response.setLogin(login);
        response.setFirstLastName("Иван Иванович");
        response.setBirthDate(LocalDate.of(1990, 1, 1));
        response.setSum(new BigDecimal(sum));
        response.setErrors(List.of());
        return response;
    }
}
