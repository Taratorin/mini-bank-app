package ru.cloudwithout.accountsservice.contract;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.cloudwithout.accountsservice.model.CashAction;
import ru.cloudwithout.accountsservice.model.CommonResponse;
import ru.cloudwithout.accountsservice.service.AccountService;
import ru.cloudwithout.accountsservice.support.AccountsIntegrationTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;

@AutoConfigureMockMvc(addFilters = false)
public abstract class AccountsContractBase extends AccountsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @BeforeEach
    void setupContracts() {
        RestAssuredMockMvc.mockMvc(mockMvc);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "contract-tester",
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))
                )
        );

        when(accountService.getAccount("test"))
                .thenReturn(CommonResponse.builder()
                        .login("test")
                        .firstLastName("Иван Иванович")
                        .birthDate(LocalDate.of(1990, 1, 1))
                        .sum(new BigDecimal("100.00"))
                        .accounts(List.of())
                        .errors(List.of())
                        .info(null)
                        .build());

        when(accountService.editCash("test", 100, CashAction.PUT))
                .thenReturn(CommonResponse.builder()
                        .login("test")
                        .firstLastName("Иван Иванович")
                        .birthDate(LocalDate.of(1990, 1, 1))
                        .sum(new BigDecimal("200.00"))
                        .accounts(List.of())
                        .errors(List.of())
                        .info("Положено руб.: 100")
                        .build());
    }
}