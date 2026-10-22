package com.mss301.order.events;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;

@Component
@Profile("choreography")
@RequiredArgsConstructor
public class EventBus {
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;

    public void publish(SagaEvent event) {
        try {
            kafka.send("saga.events", event.orderId().toString(), mapper.writeValueAsString(event)).get(15, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing saga event", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot publish saga event", ex);
        }
    }

    public SagaEvent read(String json) { return mapper.readValue(json, SagaEvent.class); }
}
