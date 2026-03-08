package my.code.userservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.code.userservice.dto.event.UserRegisteredEvent;
import my.code.userservice.service.UserService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final UserService userService;

    @KafkaListener(
            topics = "${kafka.topic.user-registered}",
            groupId = "user-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleUserRegistered(ConsumerRecord<String, UserRegisteredEvent> consumerRecord,
                                     Acknowledgment acknowledgment) {
        UserRegisteredEvent event = consumerRecord.value();

        if (event == null) {
            log.warn("Received null event for key={}, partition={}, offset={} — skipping",
                    consumerRecord.key(), consumerRecord.partition(), consumerRecord.offset());
            acknowledgment.acknowledge();
            return;
        }

        log.info("Received USER_REGISTERED: userId={}, email={}, partition={}, offset={}",
                event.userId(), event.email(), consumerRecord.partition(), consumerRecord.offset());

        userService.createProfileFromEvent(event);

        acknowledgment.acknowledge();

        log.info("USER_REGISTERED processed: userId={}", event.userId());
    }
}
