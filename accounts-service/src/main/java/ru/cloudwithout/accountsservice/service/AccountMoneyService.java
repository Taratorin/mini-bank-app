package ru.cloudwithout.accountsservice.service;

import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import ru.cloudwithout.accountsservice.kafka.NotificationPublisher;
import ru.cloudwithout.accountsservice.model.Account;
import ru.cloudwithout.accountsservice.repository.AccountRepository;
import ru.cloudwithout.commonmodels.common.dto.AccountDto;
import ru.cloudwithout.commonmodels.common.dto.CashAction;
import ru.cloudwithout.commonmodels.common.dto.CommonResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountMoneyService {

    private static final BigDecimal MAX_SUM = new BigDecimal("99999999.99");

    private final AccountRepository accountRepository;
    private final NotificationPublisher notificationPublisher;

    @Transactional
    public CommonResponse editCash(String login, int value, CashAction action) {
        Account account = accountRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Аккаунт не найден: " + login));

        BigDecimal sum = account.getSum();
        List<String> errors = new ArrayList<>();
        String info = null;

        if (action == CashAction.WITHDRAW && sum.compareTo(BigDecimal.valueOf(value)) < 0) {
            errors.add("Недостаточно средств на счёте");
            notificationPublisher.sendAfterCommit(login, "cash-" + action.name().toLowerCase(),
                    "Операция отклонена для " + login + ": недостаточно средств");
        } else {
            BigDecimal newSum = action == CashAction.WITHDRAW
                    ? sum.subtract(BigDecimal.valueOf(value))
                    : sum.add(BigDecimal.valueOf(value));
            if (newSum.compareTo(MAX_SUM) > 0) {
                errors.add("Сумма на счёте превышает допустимый лимит (99 999 999,99 руб.)");
                notificationPublisher.sendAfterCommit(login, "cash-" + action.name().toLowerCase(),
                        "Операция отклонена для " + login + ": превышен лимит счёта");
            } else {
                account.setSum(newSum);
                accountRepository.save(account);
                info = action == CashAction.WITHDRAW ? "Снято руб.: " + value : "Положено руб.: " + value;
                notificationPublisher.sendAfterCommit(login, "cash-" + action.name().toLowerCase(),
                        "Операция для " + login + ": " + info);
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
            notificationPublisher.sendAfterCommit(from, "transfer", "Перевод отклонён для " + from + ": недостаточно средств");
        } else {
            BigDecimal newSumFrom = sumFrom.subtract(BigDecimal.valueOf(value));
            BigDecimal newSumTo = sumTo.add(BigDecimal.valueOf(value));
            if (newSumTo.compareTo(MAX_SUM) > 0) {
                errors.add("Сумма на счёте получателя превышает допустимый лимит (99 999 999,99 руб.)");
                notificationPublisher.sendAfterCommit(from, "transfer", "Перевод отклонён для " + to + ": превышен лимит счёта");
            } else {
                accountFrom.setSum(newSumFrom);
                accountTo.setSum(newSumTo);
                try {
                    accountRepository.saveAll(List.of(accountFrom, accountTo));
                    accountRepository.flush();
                    info = "Перевод выполнен успешно";
                    notificationPublisher.sendAfterCommit(from, "transfer",
                            "Перевод выполнен: from=" + from + ", to=" + to + ", value=" + value);
                } catch (OptimisticLockException | ObjectOptimisticLockingFailureException e) {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    errors.add("Не удалось выполнить операцию, попробуйте снова");
                    notificationPublisher.sendAfterCommit(from, "transfer", "Не удалось выполнить операцию");
                }
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

}