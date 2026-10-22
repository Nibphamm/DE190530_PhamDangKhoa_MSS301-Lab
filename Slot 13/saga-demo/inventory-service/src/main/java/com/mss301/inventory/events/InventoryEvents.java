package com.mss301.inventory.events;

import com.mss301.inventory.dto.ReserveRequest;
import com.mss301.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@Profile("choreography")
@RequiredArgsConstructor
public class InventoryEvents {
    private final InventoryService inventory;
    private final EventBus events;

    @KafkaListener(topics = "saga.events", groupId = "saga-inventory")
    public void receive(String json) {
        SagaEvent event = events.read(json);
        if (event.type().equals("OrderCreatedEvent")) {
            SagaEvent result;
            try {
                inventory.reserve(new ReserveRequest(event.orderId(), event.productId(), event.quantity()));
                result = event.withType("InventoryReservedEvent", null);
            } catch (ResponseStatusException ex) {
                result = event.withType("InventoryFailedEvent", ex.getStatusCode().value() + " - " + ex.getReason());
            }
            // Publication errors must be retried by Kafka, not converted to business failures.
            events.publish(result);
        } else if (event.type().equals("PaymentFailedEvent")) {
            inventory.release(event.orderId());
            events.publish(event.withType("InventoryReleasedEvent", event.reason()));
        }
    }
}
