package ru.cloudwithout.accountsservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cloudwithout.accountsservice.kafka.NotificationKafkaProducer;
import ru.cloudwithout.accountsservice.model.Account;
import ru.cloudwithout.accountsservice.repository.AccountRepository;
import ru.cloudwithout.commonmodels.common.dto.AccountDto;
import ru.cloudwithout.commonmodels.common.dto.CommonResponse;

import java.time.LocalDate;
import java.util.List;

import static java.time.temporal.ChronoUnit.YEARS;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountProfileService {

    private final AccountRepository accountRepository;
    private final NotificationKafkaProducer notificationKafkaProducer;

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
        sendNotificationSafely(login, "edit-account", "Профиль пользователя " + login + " обновлён");

        return getCommonResponse(login, account, null, null);
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

    private void sendNotificationSafely(String login, String operation, String message) {
        try {
            notificationKafkaProducer.send(login, operation, message);
        } catch (Exception exception) {
            log.warn("Не удалось отправить уведомление: login={}, operation={}, message={}",
                    login, operation, message, exception);
        }
    }

    private boolean isBirthdayNotValid(LocalDate birthdate) {
        long years = YEARS.between(birthdate, LocalDate.now());
        return years < 18 || years > 120;
    }
}