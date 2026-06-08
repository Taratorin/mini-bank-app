package ru.cloudwithout.accountsservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.cloudwithout.accountsservice.service.AccountMoneyService;
import ru.cloudwithout.accountsservice.service.AccountProfileService;
import ru.cloudwithout.accountsservice.support.AccountsIntegrationTest;
import ru.cloudwithout.commonmodels.common.dto.CashAction;
import ru.cloudwithout.commonmodels.common.dto.CommonResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
    private AccountProfileService accountProfileService;

    @MockitoBean
    private AccountMoneyService accountMoneyService;

    @Test
    @WithMockUser(roles = "SERVICE")
    void getByLoginShouldReturnAccountJson() throws Exception {
        when(accountProfileService.getAccount("test")).thenReturn(response("test", "Иван Иванович", "100.00"));

        mockMvc.perform(get("/accounts/login").param("login", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("test"))
                .andExpect(jsonPath("$.sum").value(100.00));
    }

    @Test
    @WithMockUser(roles = "SERVICE")
    void editCashShouldCallService() throws Exception {
        when(accountMoneyService.editCash("test", 200, CashAction.DEPOSIT)).thenReturn(response("test", "Иван Иванович", "300.00"));

        mockMvc.perform(post("/accounts/cash")
                        .param("login", "test")
                        .param("value", "200")
                        .param("action", "DEPOSIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sum").value(300.00));

        verify(accountMoneyService).editCash("test", 200, CashAction.DEPOSIT);
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