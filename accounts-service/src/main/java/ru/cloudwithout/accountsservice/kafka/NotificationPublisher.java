package ru.cloudwithout.accountsservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationPublisher {

    private final NotificationKafkaProducer notificationKafkaProducer;

    public void sendAfterCommit(String login, String operation, String message) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sendSafely(login, operation, message);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendSafely(login, operation, message);
            }
        });
    }

    private void sendSafely(String login, String operation, String message) {
        try {
            notificationKafkaProducer.send(login, operation, message);
        } catch (Exception exception) {
            log.warn("Не удалось отправить уведомление: login={}, operation={}, message={}",
                    login, operation, message, exception);
        }
    }
}