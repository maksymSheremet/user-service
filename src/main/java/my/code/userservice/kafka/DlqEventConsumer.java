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
            topics = "user-registered-events.DLT",
            groupId = "user-service-dlq-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDeadLetter(ConsumerRecord<String, Object> record, Acknowledgment acknowledgment) {
        String originalTopic = getHeader(record, "kafka_dlt-original-topic");
        String originalPartition = getHeader(record, "kafka_dlt-original-partition");
        String originalOffset = getHeader(record, "kafka_dlt-original-offset");
        String exceptionMessage = getHeader(record, "kafka_dlt-exception-message");
        String exceptionClass = getHeader(record, "kafka_dlt-exception-fqcn");

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
                record.partition(),
                record.offset(),
                exceptionClass,
                exceptionMessage,
                record.value()
        );

        acknowledgment.acknowledge();
    }

    private String getHeader(ConsumerRecord<?, ?> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        if (header == null) return "N/A";
        return new String(header.value(), StandardCharsets.UTF_8);
    }
}
