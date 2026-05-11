package ru.cloudwithout.accountsservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.cloudwithout.accountsservice.model.Account;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByLogin(String login);
}
