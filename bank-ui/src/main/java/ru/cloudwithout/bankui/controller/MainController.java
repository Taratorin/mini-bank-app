package ru.cloudwithout.bankui.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.cloudwithout.bankui.client.AccountsClient;
import ru.cloudwithout.bankui.client.CashClient;
import ru.cloudwithout.bankui.client.TransferClient;
import ru.cloudwithout.bankui.exception.UiModelException;
import ru.cloudwithout.bankui.model.CommonResponse;
import ru.cloudwithout.bankui.model.MainPageModelFiller;
import ru.cloudwithout.bankui.model.dto.CashAction;

import java.time.LocalDate;
import java.util.List;

import static java.time.temporal.ChronoUnit.YEARS;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MainController {

    private final AccountsClient accountsClient;
    private final CashClient cashClient;
    private final TransferClient transferClient;
    private final MainPageModelFiller mainPageModelFiller;

    @GetMapping
    public String index() {
        return "redirect:/account";
    }

    @GetMapping("/account")
    public String getAccount(Model model, @AuthenticationPrincipal OidcUser oidcUser) {
        String login = oidcUser.getPreferredUsername();
        log.info("Открываем страницу аккаунта пользователя {}", login);
        if (login == null) {
            throw new UiModelException("Login отсутствует в аутентифицированном запросе");
        } else {
            CommonResponse resp = accountsClient.getAccountByLogin(login);
            if (resp == null) {
                throw new UiModelException("accounts-service вернул пустой ответ");
            } else {
                mainPageModelFiller.fillModel(model, resp, null, null);
            }
        }
        log.info("Страница аккаунта подготовлена для пользователя {}", login);
        return "main";
    }

    @PostMapping("/account")
    public String editAccount(
            Model model, @AuthenticationPrincipal OidcUser oidcUser,
            @RequestParam("name") String name, @RequestParam("birthdate") LocalDate birthdate
    ) {
        String login = oidcUser.getPreferredUsername();
        log.info("Запрос на изменение профиля: login={}, имя={}, дата рождения={}", login, name, birthdate);
        if (login == null) {
            throw new UiModelException("Login отсутствует в аутентифицированном запросе");
        } else {
            if (isBirthdayNotValid(birthdate)) {
                throw new UiModelException(
                        List.of("birthday is not valid"),
                        "user's age must be between 18 and 120 years old"
                );
            } else {
                CommonResponse resp = accountsClient.editAccount(login, name, birthdate);
                if (resp == null) {
                    throw new UiModelException("accounts-service вернул пустой ответ");
                } else {
                    mainPageModelFiller.fillModel(model, resp, null, null);
                }
            }
        }
        log.info("Данные профиля обновлены для пользователя {}", login);
        return "main";
    }

    @PostMapping("/cash")
    public String editCash(
            Model model, @AuthenticationPrincipal OidcUser oidcUser,
            @RequestParam("value") @Positive int value,
            @RequestParam("action") CashAction action
    ) {
        String login = oidcUser.getPreferredUsername();
        log.info("Запрос по операции со счетом: login={}, сумма={}, действие={}", login, value, action);
        if (login == null) {
            throw new UiModelException("Login отсутствует в аутентифицированном запросе");
        } else {
            CommonResponse resp = cashClient.editCash(login, value, action);
            if (resp == null) {
                throw new UiModelException("cash-service вернул пустой ответ");
            } else {
                if (!resp.getErrors().isEmpty()) {
                    mainPageModelFiller.fillModel(model, resp, resp.getErrors(), null);
                } else {
                    mainPageModelFiller.fillModel(model, resp, null, resp.getInfo());
                }
            }
        }
        log.info("Операция со счетом обработана для пользователя {}", login);
        return "main";
    }

    @PostMapping("/transfer")
    public String transfer(
            Model model, @AuthenticationPrincipal OidcUser oidcUser,
            @RequestParam("value") @Positive int value,
            @RequestParam("login") String login
    ) {
        String from = oidcUser.getPreferredUsername();
        log.info("Запрос на перевод: from={}, to={}, сумма={}", from, login, value);
        if (from == null) {
            throw new UiModelException("Login отправителя отсутствует в аутентифицированном запросе");
        }
        if (login == null) {
            throw new UiModelException("Login получателя отсутствует в запросе");
        } else {
            CommonResponse resp = transferClient.transfer(from, value, login);
            if (resp == null) {
                throw new UiModelException("transfer-service вернул пустой ответ");
            } else {
                if (!resp.getErrors().isEmpty()) {
                    mainPageModelFiller.fillModel(model, resp, resp.getErrors(), null);
                } else {
                    mainPageModelFiller.fillModel(model, resp, null, resp.getInfo());
                }
            }
        }
        log.info("Перевод обработан: from={}, to={}, сумма={}", from, login, value);
        return "main";
    }

    private boolean isBirthdayNotValid(LocalDate birthdate) {
        long years = YEARS.between(birthdate, LocalDate.now());
        return years < 18 || years > 120;
    }
}