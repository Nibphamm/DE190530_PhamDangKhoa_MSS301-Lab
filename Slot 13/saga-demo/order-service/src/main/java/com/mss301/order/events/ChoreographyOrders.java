package com.mss301.order.events;

import com.mss301.order.dto.CreateOrderRequest;
import com.mss301.order.entity.*;
import com.mss301.order.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("choreography")
@RequiredArgsConstructor
public class ChoreographyOrders implements OrderPlacement {
    private final OrderService orders;
    private final EventBus events;

    @Override
    public Order placeOrder(CreateOrderRequest request) {
        Order order = orders.createPendingOrder(request);
        orders.updateSagaStatus(order.getId(), SagaStatus.EXECUTING);
        orders.log(order.getId(), "CREATE_ORDER", "SUCCESS", "Publish OrderCreatedEvent");
        events.publish(new SagaEvent("OrderCreatedEvent", order.getId(), order.getCustomerId(),
                order.getProductId(), order.getQuantity(), order.getAmount(), null));
        return orders.findById(order.getId());
    }

    @KafkaListener(topics = "saga.events", groupId = "saga-order")
    @Transactional
    public void receive(String json) {
        SagaEvent event = events.read(json);
        if (!java.util.Set.of("InventoryReservedEvent", "InventoryFailedEvent", "PaymentCompletedEvent",
                "PaymentFailedEvent", "InventoryReleasedEvent").contains(event.type())) return;
        Order order = orders.findById(event.orderId());
        if (order.getSagaStatus() == SagaStatus.COMPLETED || order.getSagaStatus() == SagaStatus.COMPENSATED) return;
        switch (event.type()) {
            case "InventoryReservedEvent" -> orders.log(order.getId(), "RESERVE_INVENTORY", "SUCCESS", event.type());
            case "InventoryFailedEvent" -> {
                orders.markFailed(order.getId(), event.reason());
                orders.log(order.getId(), "SAGA", "FAILED", event.reason());
                orders.cancelOrder(order.getId());
                orders.log(order.getId(), "CANCEL_ORDER", "SUCCESS", "Inventory did not reserve");
            }
            case "PaymentCompletedEvent" -> {
                orders.log(order.getId(), "PROCESS_PAYMENT", "SUCCESS", event.type());
                orders.confirmOrder(order.getId());
                orders.log(order.getId(), "CONFIRM_ORDER", "SUCCESS", "Choreography COMPLETED");
            }
            case "PaymentFailedEvent" -> {
                orders.markFailed(order.getId(), event.reason());
                orders.updateSagaStatus(order.getId(), SagaStatus.COMPENSATING);
                orders.log(order.getId(), "SAGA", "FAILED", event.reason());
            }
            case "InventoryReleasedEvent" -> {
                orders.log(order.getId(), "COMPENSATION", "SUCCESS", "InventoryReleasedEvent");
                orders.cancelOrder(order.getId());
                orders.log(order.getId(), "CANCEL_ORDER", "SUCCESS", "Choreography COMPENSATED");
            }
            default -> { }
        }
    }
}
