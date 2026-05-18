package ru.cloudwithout.accountsservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.cloudwithout.accountsservice.model.CashAction;
import ru.cloudwithout.accountsservice.model.CommonResponse;
import ru.cloudwithout.accountsservice.service.AccountService;
import ru.cloudwithout.accountsservice.support.AccountsIntegrationTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
class AccountsControllerTest extends AccountsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @Test
    @WithMockUser(roles = "SERVICE")
    void getByLoginShouldReturnAccountJson() throws Exception {
        when(accountService.getAccount("test")).thenReturn(response("test", "Иван Иванович", "100.00"));

        mockMvc.perform(get("/accounts/login").param("login", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("test"))
                .andExpect(jsonPath("$.sum").value(100.00));
    }

    @Test
    @WithMockUser(roles = "SERVICE")
    void editCashShouldCallService() throws Exception {
        when(accountService.editCash("test", 200, CashAction.PUT)).thenReturn(response("test", "Иван Иванович", "300.00"));

        mockMvc.perform(post("/accounts/cash")
                        .param("login", "test")
                        .param("value", "200")
                        .param("action", "PUT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sum").value(300.00));

        verify(accountService).editCash("test", 200, CashAction.PUT);
    }

    @Test
    @WithMockUser(roles = "USER")
    void endpointShouldBeForbiddenWithoutServiceRole() throws Exception {
        assertThatThrownBy(() -> mockMvc.perform(get("/accounts/login").param("login", "test")))
                .hasMessageContaining("Access Denied");
    }

    private CommonResponse response(String login, String name, String sum) {
        return CommonResponse.builder()
                .login(login)
                .firstLastName(name)
                .birthDate(LocalDate.of(1990, 1, 1))
                .sum(new BigDecimal(sum))
                .accounts(List.of())
                .errors(List.of())
                .info(null)
                .build();
    }
}