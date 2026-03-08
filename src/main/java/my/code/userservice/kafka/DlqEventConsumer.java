package my.code.userservice.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class DlqEventConsumer {

    @KafkaListener(
            topics = "${kafka.topic.user-registered-dlq}",
            groupId = "user-service-dlq-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDeadLetter(ConsumerRecord<String, Object> consumerRecord, Acknowledgment acknowledgment) {
        String originalTopic = getHeader(consumerRecord, "kafka_dlt-original-topic");
        String originalPartition = getHeader(consumerRecord, "kafka_dlt-original-partition");
        String originalOffset = getHeader(consumerRecord, "kafka_dlt-original-offset");
        String exceptionMessage = getHeader(consumerRecord, "kafka_dlt-exception-message");
        String exceptionClass = getHeader(consumerRecord, "kafka_dlt-exception-fqcn");

        log.error("""
                        Dead letter received:
                          Original topic:     {}
                          Original partition: {}
                          Original offset:    {}
                          DLT partition:      {}
                          DLT offset:         {}
                          Exception:          {}
                          Exception message:  {}
                          Payload:            {}
                        """,
                originalTopic,
                originalPartition,
                originalOffset,
                consumerRecord.partition(),
                consumerRecord.offset(),
                exceptionClass,
                exceptionMessage,
                consumerRecord.value()
        );

        acknowledgment.acknowledge();
    }

    private String getHeader(ConsumerRecord<?, ?> consumerRecord, String headerName) {
        Header header = consumerRecord.headers().lastHeader(headerName);
        if (header == null) return "N/A";
        return new String(header.value(), StandardCharsets.UTF_8);
    }
}
