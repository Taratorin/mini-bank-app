package ru.cloudwithout.accountsservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cloudwithout.accountsservice.client.NotificationsClient;
import ru.cloudwithout.accountsservice.model.Account;
import ru.cloudwithout.accountsservice.repository.AccountRepository;
import ru.cloudwithout.commonmodels.common.dto.AccountDto;
import ru.cloudwithout.commonmodels.common.dto.CashAction;
import ru.cloudwithout.commonmodels.common.dto.CommonResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static java.time.temporal.ChronoUnit.YEARS;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private static final BigDecimal MAX_SUM = new BigDecimal("99999999.99");

    private final AccountRepository accountRepository;
    private final NotificationsClient notificationsClient;

    public CommonResponse getAccount(String login) {
        Account account = accountRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Аккаунт не найден: " + login));

        return getCommonResponse(login, account, null, null);
    }

    @Transactional
    public CommonResponse editAccount(String login, String name, LocalDate birthdate) {
        Account account = accountRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Аккаунт не найден: " + login));

        if (isBirthdayNotValid(birthdate)) {
            throw new IllegalArgumentException("Возраст пользователя должен быть в интервале от 18 до 120 лет");
        }

        account.setFirstLastName(name);
        account.setBirthDate(birthdate);
        accountRepository.save(account);
        sendNotificationSafely("edit-account", "Профиль пользователя " + login + " обновлён");

        return getCommonResponse(login, account, null, null);
    }

    @Transactional
    public CommonResponse editCash(String login, int value, CashAction action) {
        Account account = accountRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Аккаунт не найден: " + login));

        BigDecimal sum = account.getSum();

        List<String> errors = new ArrayList<>();
        String info = null;
        if (action == CashAction.WITHDRAW && sum.compareTo(BigDecimal.valueOf(value)) < 0) {
            errors.add("Недостаточно средств на счёте");
            sendNotificationSafely(
                    "cash-" + action.name().toLowerCase(),
                    "Операция отклонена для " + login + ": недостаточно средств"
            );
        } else {
            BigDecimal newSum = action == CashAction.WITHDRAW
                    ? sum.subtract(BigDecimal.valueOf(value))
                    : sum.add(BigDecimal.valueOf(value));
            if (newSum.compareTo(MAX_SUM) > 0) {
                errors.add("Сумма на счёте превышает допустимый лимит (99 999 999,99 руб.)");
                sendNotificationSafely(
                        "cash-" + action.name().toLowerCase(),
                        "Операция отклонена для " + login + ": превышен лимит счёта"
                );
            } else {
                account.setSum(newSum);
                accountRepository.save(account);
                info = action == CashAction.WITHDRAW ? "Снято руб.: " + value : "Положено руб.: " + value;
                sendNotificationSafely("cash-" + action.name().toLowerCase(), "Операция для " + login + ": " + info);
            }
        }

        return getCommonResponse(login, account, errors, info);
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
            BigDecimal newSumFrom = sumFrom.subtract(BigDecimal.valueOf(value));
            BigDecimal newSumTo = sumTo.add(BigDecimal.valueOf(value));
            if (newSumTo.compareTo(MAX_SUM) > 0) {
                errors.add("Сумма на счёте получателя превышает допустимый лимит (99 999 999,99 руб.)");
                sendNotificationSafely("transfer", "Перевод отклонён для " + to + ": превышен лимит счёта");
            } else {
                accountFrom.setSum(newSumFrom);
                accountTo.setSum(newSumTo);
                accountRepository.saveAll(List.of(accountFrom, accountTo));
                info = "Перевод выполнен успешно";
                sendNotificationSafely("transfer", "Перевод выполнен: from=" + from + ", to=" + to + ", value=" + value);
            }
        }

        return getCommonResponse(from, accountFrom, errors, info);
    }

    private CommonResponse getCommonResponse(String login, Account account, List<String> errors, String info) {
        List<AccountDto> others = accountRepository.findAllByLoginNot(login).stream()
                .map(a -> new AccountDto(a.getLogin(), a.getFirstLastName()))
                .toList();

        log.info("found other accounts: {}", others);

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

    private boolean isBirthdayNotValid(LocalDate birthdate) {
        long years = YEARS.between(birthdate, LocalDate.now());
        return years < 18 || years > 120;
    }
}