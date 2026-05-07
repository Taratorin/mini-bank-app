package ru.cloudwithout.accountsservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cloudwithout.accountsservice.client.NotificationsClient;
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
    private final NotificationsClient notificationsClient;

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
        sendNotificationSafely("edit-account", "Профиль пользователя " + login + " обновлён");

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
            errors.add("Недостаточно средств на счёте");
            sendNotificationSafely(
                    "cash-" + action.name().toLowerCase(),
                    "Операция отклонена для " + login + ": недостаточно средств"
            );
        } else {
            sum = action == CashAction.GET
                    ? sum.subtract(BigDecimal.valueOf(value))
                    : sum.add(BigDecimal.valueOf(value));
            account.setSum(sum);
            accountRepository.save(account);
            info = action == CashAction.GET ? "Снято руб.: " + value : "Положено руб.: " + value;
            sendNotificationSafely("cash-" + action.name().toLowerCase(), "Операция для " + login + ": " + info);
        }

        return getcommonResponse(login, account, errors, info);
    }

    @Transactional
    public CommonResponse transfer(String from, int value, String to) {
        Account accountFrom = accountRepository.findByLogin(from)
                .orElseThrow(() -> new IllegalArgumentException("Аккаунт не найден: " + from));
        Account accountTo = accountRepository.findByLogin(to)
                .orElseThrow(() -> new IllegalArgumentException("Аккаунт не найден: " + to));

        BigDecimal sumFrom = accountFrom.getSum();
        BigDecimal sumTo = accountTo.getSum();

        List<String> errors = new ArrayList<>();
        String info = null;
        if (sumFrom.compareTo(BigDecimal.valueOf(value)) < 0) {
            errors.add("Недостаточно средств на счёте");
            sendNotificationSafely("transfer", "Перевод отклонён для " + from + ": недостаточно средств");
        } else {
            sumFrom = sumFrom.subtract(BigDecimal.valueOf(value));
            accountFrom.setSum(sumFrom);
            accountTo.setSum(sumTo.add(BigDecimal.valueOf(value)));
            accountRepository.saveAll(List.of(accountFrom, accountTo));
            info = "Перевод выполнен успешно";
            sendNotificationSafely("transfer", "Перевод выполнен: from=" + from + ", to=" + to + ", value=" + value);
        }

        return getcommonResponse(from, accountFrom, errors, info);
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

    private void sendNotificationSafely(String operation, String message) {
        try {
            notificationsClient.send(operation, message);
        } catch (Exception exception) {
            log.warn("Не удалось отправить уведомление: operation={}, message={}", operation, message, exception);
        }
    }
}