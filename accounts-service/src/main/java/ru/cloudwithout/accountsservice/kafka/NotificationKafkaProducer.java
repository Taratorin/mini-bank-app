package ru.cloudwithout.accountsservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.cloudwithout.commonmodels.common.dto.NotificationRequest;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationKafkaProducer {

    private final KafkaTemplate<String, NotificationRequest> kafkaTemplate;

    @Value("${bank.kafka.topic}")
    private String topic;

    public void send(String login, String operation, String message) {
        NotificationRequest request = new NotificationRequest("accounts-service", operation, message, login);
        log.info("Отправляем уведомление в Kafka: topic={}, operation={}, message={}", topic, operation, message);
        kafkaTemplate.send(topic, request);
        log.info("Уведомление отправлено: operation={}", operation);
    }
}