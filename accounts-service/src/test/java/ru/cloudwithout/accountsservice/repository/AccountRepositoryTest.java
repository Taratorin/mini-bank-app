package ru.cloudwithout.accountsservice.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import ru.cloudwithout.accountsservice.model.Account;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void findByLoginShouldReturnAccountFromSeedData() {
        Account persisted = new Account();
        persisted.setLogin("test");
        persisted.setFirstLastName("Иван Иванович");
        persisted.setBirthDate(LocalDate.of(1990, 1, 1));
        persisted.setSum(new BigDecimal("100.00"));
        accountRepository.save(persisted);

        var account = accountRepository.findByLogin("test");

        assertThat(account).isPresent();
        assertThat(account.get().getFirstLastName()).isEqualTo("Иван Иванович");
    }
}