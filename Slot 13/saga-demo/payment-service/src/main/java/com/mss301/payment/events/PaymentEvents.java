package com.mss301.payment.events;

import com.mss301.payment.dto.PaymentRequest;
import com.mss301.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@Profile("choreography")
@RequiredArgsConstructor
public class PaymentEvents {
    private final PaymentService payment;
    private final EventBus events;

    @KafkaListener(topics = "saga.events", groupId = "saga-payment")
    public void receive(String json) {
        SagaEvent event = events.read(json);
        if (!event.type().equals("InventoryReservedEvent")) return;
        SagaEvent result;
        try {
            payment.pay(new PaymentRequest(event.orderId(), event.customerId(), event.amount()));
            result = event.withType("PaymentCompletedEvent", null);
        } catch (ResponseStatusException ex) {
            result = event.withType("PaymentFailedEvent", ex.getStatusCode().value() + " - " + ex.getReason());
        }
        events.publish(result);
    }
}
