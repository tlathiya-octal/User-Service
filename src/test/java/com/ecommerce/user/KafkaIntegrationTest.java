package com.ecommerce.user;

import com.ecommerce.events.UserCreatedEvent;
import com.ecommerce.events.UserRegisteredEvent;
import com.ecommerce.user.config.KafkaTopicsProperties;
import com.ecommerce.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"user.registered", "user.created"})
class KafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaTopicsProperties kafkaTopicsProperties;

    @Test
    void userRegisteredEventCreatesProfileAndEmitsUserCreatedEvent() {
        UUID userId = UUID.randomUUID();
        UserRegisteredEvent event = new UserRegisteredEvent(userId, "new-user@example.com", Instant.now());

        kafkaTemplate.send(kafkaTopicsProperties.getUserRegistered(), event);

        await()
                .atMost(Duration.ofSeconds(10))
                .until(() -> userRepository.findByEmailIgnoreCase("new-user@example.com").isPresent());

        var savedUser = userRepository.findByEmailIgnoreCase("new-user@example.com").orElseThrow();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "user-service-test-consumer",
                "false",
                embeddedKafkaBroker
        );
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        JsonDeserializer<UserCreatedEvent> deserializer = new JsonDeserializer<>(UserCreatedEvent.class, objectMapper);
        deserializer.addTrustedPackages("com.ecommerce.events");

        Consumer<String, UserCreatedEvent> consumer = new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new StringDeserializer(),
                deserializer
        ).createConsumer();

        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, kafkaTopicsProperties.getUserCreated());
        ConsumerRecord<String, UserCreatedEvent> record = KafkaTestUtils.getSingleRecord(
                consumer,
                kafkaTopicsProperties.getUserCreated()
        );

        assertThat(record.value()).isNotNull();
        assertThat(record.value().userId()).isEqualTo(savedUser.getId());
        assertThat(record.value().email()).isEqualTo("new-user@example.com");

        consumer.close();
    }
}
