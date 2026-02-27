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
            topics = "user-registered-events",
            groupId = "user-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleUserRegistered(ConsumerRecord<String, UserRegisteredEvent> record,
                                     Acknowledgment acknowledgment) {
        UserRegisteredEvent event = record.value();

        log.info("Received USER_REGISTERED: userId={}, email={}, partition={}, offset={}",
                event.userId(), event.email(), record.partition(), record.offset());

        userService.createProfileFromEvent(event);

        acknowledgment.acknowledge();

        log.info("USER_REGISTERED processed: userId={}", event.userId());
    }
}
