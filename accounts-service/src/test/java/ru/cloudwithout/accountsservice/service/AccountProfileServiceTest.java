package ru.cloudwithout.accountsservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.cloudwithout.accountsservice.kafka.NotificationKafkaProducer;
import ru.cloudwithout.accountsservice.model.Account;
import ru.cloudwithout.accountsservice.repository.AccountRepository;
import ru.cloudwithout.commonmodels.common.dto.CommonResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountProfileServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private NotificationKafkaProducer notificationKafkaProducer;

    private AccountProfileService accountProfileService;

    @BeforeEach
    void setUp() {
        accountProfileService = new AccountProfileService(
                accountRepository,
                notificationKafkaProducer
        );
    }

    @Test
    void getAccountShouldReturnProfileData() {
        Account serg = account("serg", new BigDecimal("100.00"));
        Account alex = account("alex", new BigDecimal("500.00"));
        when(accountRepository.findByLogin("serg")).thenReturn(Optional.of(serg));
        when(accountRepository.findAllByLoginNot("serg")).thenReturn(List.of(alex));

        CommonResponse response = accountProfileService.getAccount("serg");

        assertThat(response.getLogin()).isEqualTo("serg");
        assertThat(response.getSum()).isEqualByComparingTo("100.00");
        assertThat(response.getAccounts()).hasSize(1);
    }

    @Test
    void editAccountShouldUpdateProfile() {
        Account serg = account("serg", new BigDecimal("100.00"));
        when(accountRepository.findByLogin("serg")).thenReturn(Optional.of(serg));
        when(accountRepository.findAllByLoginNot("serg")).thenReturn(List.of());

        CommonResponse response = accountProfileService.editAccount(
                "serg", "Иван Иванович", LocalDate.of(1990, 1, 1));

        assertThat(response.getFirstLastName()).isEqualTo("Иван Иванович");
        assertThat(response.getBirthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
        verify(accountRepository).save(serg);
        verify(notificationKafkaProducer).send(eq("serg"), eq("edit-account"), any());
    }

    @Test
    void editAccountShouldRejectInvalidBirthdate() {
        Account serg = account("serg", new BigDecimal("100.00"));
        when(accountRepository.findByLogin("serg")).thenReturn(Optional.of(serg));

        assertThatThrownBy(() -> accountProfileService.editAccount(
                "serg", "Иван Иванович", LocalDate.now().minusYears(10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Возраст пользователя");
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
