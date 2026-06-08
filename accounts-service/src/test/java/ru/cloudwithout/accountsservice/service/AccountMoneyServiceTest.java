package ru.cloudwithout.accountsservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.cloudwithout.accountsservice.kafka.NotificationKafkaProducer;
import ru.cloudwithout.accountsservice.model.Account;
import ru.cloudwithout.accountsservice.repository.AccountRepository;
import ru.cloudwithout.commonmodels.common.dto.CashAction;
import ru.cloudwithout.commonmodels.common.dto.CommonResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountMoneyServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private NotificationKafkaProducer notificationKafkaProducer;

    private AccountMoneyService accountMoneyService;

    @BeforeEach
    void setUp() {
        accountMoneyService = new AccountMoneyService(
                accountRepository,
                notificationKafkaProducer
        );
    }

    @Test
    void editCashPutShouldIncreaseBalance() {
        Account serg = account("serg", new BigDecimal("100.00"));
        Account alex = account("alex", new BigDecimal("500.00"));
        when(accountRepository.findByLogin("serg")).thenReturn(Optional.of(serg));
        when(accountRepository.findAllByLoginNot("serg")).thenReturn(List.of(alex));

        CommonResponse response = accountMoneyService.editCash("serg", 50, CashAction.DEPOSIT);

        assertThat(response.getSum()).isEqualByComparingTo("150.00");
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getInfo()).contains("Положено руб.: 50");
        verify(accountRepository).save(serg);
        verify(notificationKafkaProducer).send(eq("serg"), eq("cash-deposit"), any());
    }

    @Test
    void editCashGetShouldReturnErrorWhenInsufficientFunds() {
        Account serg = account("serg", new BigDecimal("40.00"));
        Account alex = account("alex", new BigDecimal("500.00"));
        when(accountRepository.findByLogin("serg")).thenReturn(Optional.of(serg));
        when(accountRepository.findAllByLoginNot("serg")).thenReturn(List.of(alex));

        CommonResponse response = accountMoneyService.editCash("serg", 100, CashAction.WITHDRAW);

        assertThat(response.getSum()).isEqualByComparingTo("40.00");
        assertThat(response.getErrors()).contains("Недостаточно средств на счёте");
        assertThat(response.getInfo()).isNull();
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void editCashPutShouldReturnErrorWhenSumExceedsLimit() {
        Account serg = account("serg", new BigDecimal("99999990.00"));
        when(accountRepository.findByLogin("serg")).thenReturn(Optional.of(serg));
        when(accountRepository.findAllByLoginNot("serg")).thenReturn(List.of());

        CommonResponse response = accountMoneyService.editCash("serg", 20, CashAction.DEPOSIT);

        assertThat(response.getSum()).isEqualByComparingTo("99999990.00");
        assertThat(response.getErrors()).contains("Сумма на счёте превышает допустимый лимит (99 999 999,99 руб.)");
        assertThat(response.getInfo()).isNull();
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void transferShouldMoveMoneyBetweenAccounts() {
        Account from = account("serg", new BigDecimal("500.00"));
        Account to = account("alex", new BigDecimal("100.00"));
        when(accountRepository.findByLogin("serg")).thenReturn(Optional.of(from));
        when(accountRepository.findByLogin("alex")).thenReturn(Optional.of(to));
        when(accountRepository.findAllByLoginNot("serg")).thenReturn(List.of(to));

        CommonResponse response = accountMoneyService.transfer("serg", 120, "alex");

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