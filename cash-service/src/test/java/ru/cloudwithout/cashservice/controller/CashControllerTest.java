package ru.cloudwithout.cashservice.controller;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.cloudwithout.cashservice.client.AccountsClient;
import ru.cloudwithout.cashservice.kafka.NotificationKafkaProducer;
import ru.cloudwithout.cashservice.support.SecurityTestSupport;
import ru.cloudwithout.commonmodels.common.dto.CashAction;
import ru.cloudwithout.commonmodels.common.dto.CommonResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.cloudwithout.cashservice.support.SecurityTestSupport.bearerToken;

@SpringBootTest
@AutoConfigureMockMvc
class CashControllerTest {

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
    void editCashShouldReturnServiceResponse() throws Exception {
        CommonResponse response = response("test", "350.00");
        when(accountsClient.editCash("test", 250, CashAction.DEPOSIT)).thenReturn(response);

        mockMvc.perform(post("/cash")
                        .with(bearerToken())
                        .param("login", "test")
                        .param("value", "250")
                        .param("action", "DEPOSIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sum").value(350.00));

        verify(accountsClient).editCash("test", 250, CashAction.DEPOSIT);
        verify(notificationKafkaProducer).send("test", "cash-deposit", "Обработан запрос cash-service для test, value=250");
    }

    @Test
    void editCashWithdrawFailureShouldIncrementMetric() throws Exception {
        CommonResponse response = response("test", "40.00");
        response.setErrors(List.of("Недостаточно средств на счёте"));
        when(accountsClient.editCash("test", 100, CashAction.WITHDRAW)).thenReturn(response);

        mockMvc.perform(post("/cash")
                        .with(bearerToken())
                        .param("login", "test")
                        .param("value", "100")
                        .param("action", "WITHDRAW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[0]").value("Недостаточно средств на счёте"));

        assertThat(meterRegistry.get("bank.cash.withdraw.failed")
                .tag("login", "test")
                .counter()
                .count()).isEqualTo(1.0);
    }

    @Test
    void editCashShouldBeForbiddenWithoutServiceRole() throws Exception {
        SecurityTestSupport.stubJwtDecoder(jwtDecoder, "USER");

        mockMvc.perform(post("/cash")
                        .with(bearerToken())
                        .param("login", "test")
                        .param("value", "250")
                        .param("action", "DEPOSIT"))
                .andExpect(status().isForbidden());
    }

    @Test
    void editCashShouldBeUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(post("/cash")
                        .param("login", "test")
                        .param("value", "250")
                        .param("action", "DEPOSIT"))
                .andExpect(status().isUnauthorized());
    }

    private CommonResponse response(String login, String sum) {
        CommonResponse response = CommonResponse.builder().accounts(List.of()).build();
        response.setLogin(login);
        response.setFirstLastName("Иван Иванович");
        response.setBirthDate(LocalDate.of(1990, 1, 1));
        response.setSum(new BigDecimal(sum));
        response.setErrors(List.of());
        return response;
    }
}