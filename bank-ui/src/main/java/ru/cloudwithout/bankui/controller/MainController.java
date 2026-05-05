package ru.cloudwithout.bankui.controller;

import jakarta.annotation.Nullable;
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
import ru.cloudwithout.bankui.model.dto.CashAction;
import ru.cloudwithout.bankui.controller.stub.AccountStub;
import ru.cloudwithout.bankui.model.CommonResponse;

import java.time.LocalDate;
import java.util.List;

import static java.time.temporal.ChronoUnit.YEARS;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MainController {

    // TODO: Удалить заглушку, так как используется только для ознакомительных целей
    private final AccountStub accountStub;
    private final AccountsClient accountsClient;
    private final CashClient cashClient;

    @GetMapping
    public String index() {
        return "redirect:/account";
    }

    @GetMapping("/account")
    public String getAccount(Model model, @AuthenticationPrincipal OidcUser oidcUser) {
        String login = oidcUser.getPreferredUsername();
        log.info("Открываем страницу аккаунта пользователя {}", login);
        if (login == null) {
            fillModel(model, null, List.of("login not found in request"), null);
        } else {
            CommonResponse resp = accountsClient.getAccountByLogin(login);
            if (resp == null) {
                fillModel(model, null, List.of("accounts service return null data"), null);
            } else {
                fillModel(model, resp, null, null);
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
            fillModel(model, null, List.of("login not found in request"), null);
        } else {
            if (isBirthdayNotValid(birthdate)) {
                fillModel(model, null, List.of("birthday is not valid"), "user's age must be between 18 and 120 years old");
            } else {
                CommonResponse resp = accountsClient.editAccount(login, name, birthdate);
                if (resp == null) {
                    fillModel(model, null, List.of("accounts service return null data"), null);
                } else {
                    fillModel(model, resp, null, null);
                }
            }
        }
        log.info("Данные профиля обновлены для пользователя {}", login);
        return "main";
    }

    /**
     * POST /cash.
     * Что нужно сделать:
     * 1. Сходить в сервис cash через Gateway API для снятия/пополнения счета текущего аккаунта по REST
     * 2. Заполнить модель main.html полученными из ответа данными
     * 3. Текущего пользователя можно получить из контекста Security
     * <p>
     * Параметры:
     * 1. value - сумма списания
     * 2. action - GET (снять), PUT (пополнить)
     */
    @PostMapping("/cash")
    public String editCash(
            Model model, @AuthenticationPrincipal OidcUser oidcUser,
            @RequestParam("value") int value,
            @RequestParam("action") CashAction action
    ) {
        // TODO: Заменить на то, что описано в комментарии к методу
        accountStub.editCash(model, value, action);

        String login = oidcUser.getPreferredUsername();
        log.info("Запрос по операции со счетом: login={}, сумма={}, действие={}", login, value, action);
        if (login == null) {
            fillModel(model, null, List.of("login not found in request"), null);
        } else {
            CommonResponse resp = cashClient.editCash(login, value, action);
            if (resp == null) {
                fillModel(model, null, List.of("accounts service return null data"), null);
            } else {
                if (!resp.getErrors().isEmpty()) {
                    fillModel(model, resp, resp.getErrors(), null);
                } else {
                    fillModel(model, resp, null, resp.getInfo());
                }
            }
        }
        log.info("Операция со счетом обработана для пользователя {}", login);
        return "main";
    }

    /**
     * POST /transfer.
     * Что нужно сделать:
     * 1. Сходить в сервис accounts через Gateway API для перевода со счета текущего аккаунта на счет другого аккаунта по REST
     * 2. Заполнить модель main.html полученными из ответа данными
     * 3. Текущего пользователя можно получить из контекста Security
     * <p>
     * Параметры:
     * 1. value - сумма списания
     * 2. login - логин пользователя получателя
     */
    @PostMapping("/transfer")
    public String transfer(
            Model model,
            @RequestParam("value") int value,
            @RequestParam("login") String login
    ) {
        // TODO: Заменить на то, что описано в комментарии к методу
        accountStub.transfer(model, value, login);

        return "main";
    }

    private void fillModel(
            Model model, CommonResponse commonResponse,
            @Nullable List<String> errors,
            @Nullable String info) {
        if (commonResponse != null) {
            model.addAttribute("name", commonResponse.getFirstLastName());
            model.addAttribute("birthdate", commonResponse.getBirthDate());
            model.addAttribute("sum", commonResponse.getSum());
            model.addAttribute("accounts", commonResponse.getAccounts());
        }
        model.addAttribute("errors", errors);
        model.addAttribute("info", info);
    }

    private boolean isBirthdayNotValid(LocalDate birthdate) {
        long years = YEARS.between(birthdate, LocalDate.now());
        return years < 18 || years > 120;
    }
}