package ru.cloudwithout.transferservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.cloudwithout.commonmodels.common.dto.CommonResponse;
import ru.cloudwithout.transferservice.client.AccountsClient;
import ru.cloudwithout.transferservice.client.NotificationsClient;

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
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountsClient accountsClient;

    @MockitoBean
    private NotificationsClient notificationsClient;

    @Test
    @WithMockUser(roles = "SERVICE")
    void transferShouldReturnAccountsServiceResponse() throws Exception {
        when(accountsClient.transfer("test", 100, "alex")).thenReturn(response("test", "900.00"));

        mockMvc.perform(post("/transfer")
                        .param("from", "test")
                        .param("to", "alex")
                        .param("value", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sum").value(900.00));

        verify(notificationsClient).send("transfer", "Обработан запрос transfer-service: from=test, to=alex, value=100");
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
