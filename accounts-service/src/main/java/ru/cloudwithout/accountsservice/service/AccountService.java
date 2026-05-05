package ru.cloudwithout.accountsservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cloudwithout.accountsservice.model.Account;
import ru.cloudwithout.accountsservice.model.AccountDto;
import ru.cloudwithout.accountsservice.model.CashAction;
import ru.cloudwithout.accountsservice.model.CommonResponse;
import ru.cloudwithout.accountsservice.repository.AccountRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;

    public CommonResponse getAccount(String login) {
        Account account = accountRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Аккаунт не найден: " + login));

        return getcommonResponse(login, account, null, null);
    }

    public CommonResponse editAccount(String login, String name, LocalDate birthdate) {
        Account account = accountRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Аккаунт не найден: " + login));
        account.setFirstLastName(name);
        account.setBirthDate(birthdate);
        accountRepository.save(account);

        return getcommonResponse(login, account, null, null);
    }

    @Transactional
    public CommonResponse editCash(String login, int value, CashAction action) {
        Account account = accountRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Аккаунт не найден: " + login));

        BigDecimal sum = account.getSum();

        List<String> errors = new ArrayList<>();
        String info = null;
        if (action == CashAction.GET && sum.compareTo(BigDecimal.valueOf(value)) < 0) {
            errors.add("Недостаточно средств на счету");
        } else {
            sum = action == CashAction.GET
                    ? sum.subtract(BigDecimal.valueOf(value))
                    : sum.add(BigDecimal.valueOf(value));
            account.setSum(sum);
            accountRepository.save(account);
            info = action == CashAction.GET ? "Снято руб.: " + value : "Положено руб.: " + value;
        }

        return getcommonResponse(login, account, errors, info);
    }

    private CommonResponse getcommonResponse(String login, Account account, List<String> errors, String info) {
        List<AccountDto> others = accountRepository.findAll().stream()
                .filter(a -> !a.getLogin().equals(login))
                .map(a -> new AccountDto(a.getLogin(), a.getFirstLastName()))
                .toList();

        return CommonResponse.builder()
                .login(account.getLogin())
                .firstLastName(account.getFirstLastName())
                .birthDate(account.getBirthDate())
                .sum(account.getSum())
                .accounts(others)
                .errors(errors)
                .info(info)
                .build();
    }
}