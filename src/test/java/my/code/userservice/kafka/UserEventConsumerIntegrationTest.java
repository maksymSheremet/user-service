package my.code.userservice.kafka;

import my.code.userservice.BaseIntegrationTest;
import my.code.userservice.dto.event.UserRegisteredEvent;
import my.code.userservice.repository.UserRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DisplayName("UserEventConsumer Integration")
class UserEventConsumerIntegrationTest extends BaseIntegrationTest {

    private static final String TOPIC = "user-registered-events";
    private static final String DLT_TOPIC = "user-registered-events-dlt";
    private static final Long USER_ID = 100L;
    private static final String EMAIL = "kafka-test@example.com";

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("should create user profile when USER_REGISTERED event received")
    void shouldCreateProfileOnUserRegisteredEvent() {
        KafkaTemplate<String, UserRegisteredEvent> producer = createTestProducer();

        UserRegisteredEvent event = new UserRegisteredEvent(
                USER_ID, EMAIL, "Kafka User", null,
                "Europe/Kyiv", "uk", Instant.now()
        );

        producer.send(TOPIC, String.valueOf(USER_ID), event);
        producer.flush();

        await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(userRepository.existsById(USER_ID)).isTrue();

                    var user = userRepository.findById(USER_ID).orElseThrow();
                    assertThat(user.getEmail()).isEqualTo(EMAIL);
                    assertThat(user.getFullName()).isEqualTo("Kafka User");
                    assertThat(user.getTimezone()).isEqualTo("Europe/Kyiv");
                    assertThat(user.getLanguage()).isEqualTo("uk");
                });
    }

    @Test
    @DisplayName("should handle duplicate events idempotently")
    void shouldHandleDuplicateEventsIdempotently() {
        KafkaTemplate<String, UserRegisteredEvent> producer = createTestProducer();

        UserRegisteredEvent event = new UserRegisteredEvent(
                USER_ID, EMAIL, "Kafka User", null,
                "UTC", "en", Instant.now()
        );

        producer.send(TOPIC, String.valueOf(USER_ID), event);
        producer.send(TOPIC, String.valueOf(USER_ID), event);
        producer.flush();

        await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .until(() -> userRepository.existsById(USER_ID));

        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> userRepository.count() > 0);

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("should route invalid message to Dead Letter Topic")
    void shouldRouteToDlqOnInvalidMessageFormat() {
        KafkaTemplate<String, String> stringProducer = createStringProducer();
        String invalidPayload = "{ not valid json at all...";

        stringProducer.send(TOPIC, String.valueOf(USER_ID), invalidPayload);
        stringProducer.flush();

        try (KafkaConsumer<String, String> dltConsumer = createStringConsumer()) {
            dltConsumer.subscribe(Collections.singletonList(DLT_TOPIC));

            await()
                    .atMost(30, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        var records = dltConsumer.poll(Duration.ofMillis(200));
                        assertThat(records.isEmpty()).isFalse();
                    });
        }

        assertThat(userRepository.count()).isZero();
    }

    private KafkaTemplate<String, UserRegisteredEvent> createTestProducer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BaseIntegrationTest.kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    private KafkaTemplate<String, String> createStringProducer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                BaseIntegrationTest.kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    private KafkaConsumer<String, String> createStringConsumer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BaseIntegrationTest.kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-dlt-consumer-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new KafkaConsumer<>(props);
    }
}