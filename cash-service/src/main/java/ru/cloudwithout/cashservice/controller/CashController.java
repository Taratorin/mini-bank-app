package ru.cloudwithout.cashservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.cloudwithout.cashservice.client.AccountsClient;
import ru.cloudwithout.cashservice.model.CommonResponse;
import ru.cloudwithout.cashservice.model.dto.CashAction;

@RestController
@RequestMapping("/cash")
@RequiredArgsConstructor
@Slf4j
public class CashController {

    private final AccountsClient accountsClient;

    @PostMapping()
    @PreAuthorize("hasRole('SERVICE')")
    public CommonResponse editCash(@RequestParam String login,
                                      @RequestParam int value, @RequestParam CashAction action) {
        log.info("Получен запрос на операцию со счетом: login={}, сумма={}, действие={}", login, value, action);
        CommonResponse response = accountsClient.editCash(login, value, action);
        log.info("Запрос на операцию со счетом обработан для пользователя {}", login);
        return response;
    }
}
