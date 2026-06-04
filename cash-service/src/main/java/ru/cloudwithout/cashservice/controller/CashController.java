package ru.cloudwithout.cashservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.cloudwithout.cashservice.client.AccountsClient;
import ru.cloudwithout.cashservice.kafka.NotificationKafkaProducer;
import ru.cloudwithout.commonmodels.common.dto.CashAction;
import ru.cloudwithout.commonmodels.common.dto.CommonResponse;

@RestController
@RequestMapping("/cash")
@RequiredArgsConstructor
@Slf4j
public class CashController {

    private final AccountsClient accountsClient;
    private final NotificationKafkaProducer notificationKafkaProducer;

    @PostMapping()
    @PreAuthorize("hasRole('SERVICE')")
    public CommonResponse editCash(@RequestParam String login,
                                   @RequestParam int value, @RequestParam CashAction action) {
        log.info("Получен запрос на операцию со счетом: login={}, сумма={}, действие={}", login, value, action);
        CommonResponse response = accountsClient.editCash(login, value, action);
        try {
            notificationKafkaProducer.send(
                    "cash-" + action.name().toLowerCase(),
                    "Обработан запрос cash-service для " + login + ", value=" + value
            );
        } catch (Exception exception) {
            log.warn("Не удалось отправить уведомление: login={}, action={}, value={}", login, action, value, exception);
        }
        log.info("Запрос на операцию со счетом обработан для пользователя {}", login);
        return response;
    }
}