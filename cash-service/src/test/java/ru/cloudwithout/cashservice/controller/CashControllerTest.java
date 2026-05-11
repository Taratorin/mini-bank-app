package ru.cloudwithout.cashservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.cloudwithout.cashservice.client.AccountsClient;
import ru.cloudwithout.cashservice.client.NotificationsClient;
import ru.cloudwithout.cashservice.model.CommonResponse;
import ru.cloudwithout.cashservice.model.dto.CashAction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class CashControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountsClient accountsClient;

    @MockitoBean
    private NotificationsClient notificationsClient;

    @Test
    @WithMockUser(roles = "SERVICE")
    void editCashShouldReturnServiceResponse() throws Exception {
        CommonResponse response = response("test", "350.00");
        when(accountsClient.editCash("test", 250, CashAction.PUT)).thenReturn(response);

        mockMvc.perform(post("/cash")
                        .param("login", "test")
                        .param("value", "250")
                        .param("action", "PUT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sum").value(350.00));

        verify(accountsClient).editCash("test", 250, CashAction.PUT);
        verify(notificationsClient).send("cash-put", "Обработан запрос cash-service для test, value=250");
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