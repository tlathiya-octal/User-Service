package com.ecommerce.user.consumers;

import com.ecommerce.events.UserCreatedEvent;
import com.ecommerce.events.UserCreatedEvent;
import com.ecommerce.user.event.CorrelationIdProvider;
import com.ecommerce.user.service.UserService;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredEventConsumer {

    private final UserService userService;
    private final CorrelationIdProvider correlationIdProvider;

    @KafkaListener(
            topics = "${app.kafka.topics.user-created}",
            containerFactory = "userRegisteredKafkaListenerContainerFactory"
    )
    public void handle(
            UserCreatedEvent event,
            @Header(name = CorrelationIdProvider.HEADER_NAME, required = false) byte[] correlationIdHeader,
            @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic) {
        String correlationId = resolveCorrelationId(correlationIdHeader);
        correlationIdProvider.set(correlationId);
        long start = System.nanoTime();
        try {
            userService.handleUserRegistered(event);
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            log.info("Event consumed topic={} eventType={} durationMs={} correlationId={}",
                    topic, event.getClass().getSimpleName(), durationMs, correlationId);
        } catch (Exception ex) {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            log.error("Event processing failed topic={} eventType={} durationMs={} correlationId={}",
                    topic, event.getClass().getSimpleName(), durationMs, correlationId, ex);
            throw ex;
        } finally {
            correlationIdProvider.clear();
        }
    }

    private String resolveCorrelationId(byte[] header) {
        if (header == null || header.length == 0) {
            return correlationIdProvider.currentOrGenerate();
        }
        return new String(header, StandardCharsets.UTF_8);
    }
}
