package ru.cloudwithout.accountsservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.cloudwithout.accountsservice.client.NotificationsClient;
import ru.cloudwithout.accountsservice.model.Account;
import ru.cloudwithout.accountsservice.model.CashAction;
import ru.cloudwithout.accountsservice.model.CommonResponse;
import ru.cloudwithout.accountsservice.repository.AccountRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private NotificationsClient notificationsClient;

    @InjectMocks
    private AccountService accountService;

    @Test
    void editCashPutShouldIncreaseBalance() {
        Account serg = account("serg", new BigDecimal("100.00"));
        Account alex = account("alex", new BigDecimal("500.00"));
        when(accountRepository.findByLogin("serg")).thenReturn(Optional.of(serg));
        when(accountRepository.findAll()).thenReturn(List.of(serg, alex));

        CommonResponse response = accountService.editCash("serg", 50, CashAction.PUT);

        assertThat(response.getSum()).isEqualByComparingTo("150.00");
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getInfo()).contains("Положено руб.: 50");
        verify(accountRepository).save(serg);
        verify(notificationsClient).send(any(), any());
    }

    @Test
    void editCashGetShouldReturnErrorWhenInsufficientFunds() {
        Account serg = account("serg", new BigDecimal("40.00"));
        Account alex = account("alex", new BigDecimal("500.00"));
        when(accountRepository.findByLogin("serg")).thenReturn(Optional.of(serg));
        when(accountRepository.findAll()).thenReturn(List.of(serg, alex));

        CommonResponse response = accountService.editCash("serg", 100, CashAction.GET);

        assertThat(response.getSum()).isEqualByComparingTo("40.00");
        assertThat(response.getErrors()).contains("Недостаточно средств на счёте");
        assertThat(response.getInfo()).isNull();
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void transferShouldMoveMoneyBetweenAccounts() {
        Account from = account("serg", new BigDecimal("500.00"));
        Account to = account("alex", new BigDecimal("100.00"));
        when(accountRepository.findByLogin("serg")).thenReturn(Optional.of(from));
        when(accountRepository.findByLogin("alex")).thenReturn(Optional.of(to));
        when(accountRepository.findAll()).thenReturn(List.of(from, to));

        CommonResponse response = accountService.transfer("serg", 120, "alex");

        assertThat(from.getSum()).isEqualByComparingTo("380.00");
        assertThat(to.getSum()).isEqualByComparingTo("220.00");
        assertThat(response.getInfo()).isEqualTo("Перевод выполнен успешно");
        verify(accountRepository).saveAll(anyList());
    }

    private Account account(String login, BigDecimal sum) {
        Account account = new Account();
        account.setLogin(login);
        account.setBirthDate(LocalDate.of(1990, 1, 1));
        account.setFirstLastName(login);
        account.setSum(sum);
        return account;
    }
}