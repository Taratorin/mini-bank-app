package ru.cloudwithout.notificationsservice.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import ru.cloudwithout.commonmodels.common.dto.NotificationRequest;

@Component
@Slf4j
public class NotificationKafkaConsumer {

    @KafkaListener(
            topics = "${bank.kafka.topic}",
            groupId = "${bank.kafka.consumer-group}"
    )
    public void consume(NotificationRequest request,
                        @Header(name = "kafka_receivedTopic", required = false) String topic) {
        log.info(
                "Уведомление получено из Kafka: topic={}, service={}, operation={}, message={}",
                topic,
                request.service(),
                request.operation(),
                request.message()
        );
    }
}