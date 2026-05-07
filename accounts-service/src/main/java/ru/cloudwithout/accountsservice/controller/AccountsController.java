package ru.cloudwithout.accountsservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.cloudwithout.accountsservice.model.CashAction;
import ru.cloudwithout.accountsservice.model.CommonResponse;
import ru.cloudwithout.accountsservice.service.AccountService;

import java.time.LocalDate;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountsController {

    private final AccountService accountService;

    @GetMapping("/login")
    @PreAuthorize("hasRole('SERVICE')")
    public CommonResponse getByLogin(@RequestParam String login) {
        log.info("Получен запрос данных аккаунта пользователя {}", login);
        CommonResponse response = accountService.getAccount(login);
        log.info("Данные аккаунта пользователя {} сформированы", login);
        return response;
    }

    @PostMapping("/edit")
    @PreAuthorize("hasRole('SERVICE')")
    public CommonResponse editAccount(@RequestParam String login,
                                      @RequestParam String name, @RequestParam LocalDate birthdate) {
        log.info("Получен запрос на изменение профиля: login={}, имя={}, дата рождения={}", login, name, birthdate);
        CommonResponse response = accountService.editAccount(login, name, birthdate);
        log.info("Профиль пользователя {} обновлен", login);
        return response;
    }

    @PostMapping("/cash")
    @PreAuthorize("hasRole('SERVICE')")
    public CommonResponse editCash(@RequestParam String login,
                                   @RequestParam int value, @RequestParam CashAction action) {
        log.info("Получен запрос по операции со счетом: login={}, сумма={}, действие={}", login, value, action);
        CommonResponse response = accountService.editCash(login, value, action);
        log.info("Операция со счетом выполнена для пользователя {}", login);
        return response;
    }


    @PostMapping("/transfer")
    @PreAuthorize("hasRole('SERVICE')")
    public CommonResponse transfer(@RequestParam String from,
                                   @RequestParam int value, @RequestParam String to) {
        log.info("Получен запрос на перевод: from={}, to={}, value={}", from, to, value);
        CommonResponse response = accountService.transfer(from, value, to);
        log.info("Запрос на перевод обработан: from={}, to={}, value={}", from, to, value);
        return response;
    }
}
