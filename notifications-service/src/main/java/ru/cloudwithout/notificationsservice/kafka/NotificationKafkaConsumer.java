package ru.cloudwithout.notificationsservice.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import ru.cloudwithout.commonmodels.common.dto.NotificationRequest;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationKafkaConsumer {

    private final MeterRegistry meterRegistry;

    @KafkaListener(
            topics = "${bank.kafka.topic}",
            groupId = "${bank.kafka.consumer-group}"
    )
    public void consume(NotificationRequest request,
                        @Header(name = "kafka_receivedTopic", required = false) String topic) {
        String login = resolveLogin(request);
        try {
            processNotification(request, topic);
        } catch (Exception exception) {
            meterRegistry.counter("bank.notification.send.failed", "login", login).increment();
            log.error(
                    "Не удалось обработать уведомление: login={}, topic={}, service={}, operation={}",
                    login,
                    topic,
                    request.service(),
                    request.operation(),
                    exception
            );
            throw exception;
        }
    }

    private void processNotification(NotificationRequest request, String topic) {
        log.info(
                "Уведомление получено из Kafka: topic={}, login={}, service={}, operation={}, message={}",
                topic,
                request.login(),
                request.service(),
                request.operation(),
                request.message()
        );
    }

    private String resolveLogin(NotificationRequest request) {
        if (request.login() != null && !request.login().isBlank()) {
            return request.login();
        }
        return "unknown";
    }
}