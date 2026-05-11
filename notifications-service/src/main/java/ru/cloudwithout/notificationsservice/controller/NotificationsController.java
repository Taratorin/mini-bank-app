package ru.cloudwithout.notificationsservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.cloudwithout.notificationsservice.model.NotificationRequest;

@RestController
@RequestMapping("/notifications")
@Slf4j
public class NotificationsController {

    @PostMapping
    @PreAuthorize("hasRole('SERVICE')")
    public void notify(@RequestBody NotificationRequest request) {
        log.info(
                "Уведомление: service={}, operation={}, message={}",
                request.service(),
                request.operation(),
                request.message()
        );
    }
}
