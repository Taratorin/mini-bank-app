package ru.cloudwithout.transferservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.cloudwithout.transferservice.client.AccountsClient;
import ru.cloudwithout.transferservice.client.NotificationsClient;
import ru.cloudwithout.transferservice.model.CommonResponse;

@RestController
@RequestMapping("/transfer")
@RequiredArgsConstructor
@Slf4j
public class TransferController {

    private final AccountsClient accountsClient;
    private final NotificationsClient notificationsClient;

    @PostMapping()
    @PreAuthorize("hasRole('SERVICE')")
    public CommonResponse transfer(@RequestParam String from,
                                   @RequestParam int value, @RequestParam String to) {
        log.info("Получен запрос transfer-service: from={}, to={}, value={}", from, to, value);
        CommonResponse response = accountsClient.transfer(from, value, to);
        try {
            notificationsClient.send(
                    "transfer",
                    "Обработан запрос transfer-service: from=" + from + ", to=" + to + ", value=" + value
            );
        } catch (Exception exception) {
            log.warn("Не удалось отправить уведомление о переводе: from={}, to={}, value={}", from, to, value, exception);
        }
        log.info("Запрос transfer-service обработан: from={}, to={}, value={}", from, to, value);
        return response;
    }
}