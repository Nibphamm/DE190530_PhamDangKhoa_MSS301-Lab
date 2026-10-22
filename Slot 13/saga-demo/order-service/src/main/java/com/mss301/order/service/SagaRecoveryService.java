package com.mss301.order.service;

import com.mss301.order.client.*;
import com.mss301.order.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.retry.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Profile("recovery")
@RequiredArgsConstructor
public class SagaRecoveryService {
    private final OrderService orders;
    private final InventoryClient inventory;
    private final PaymentClient payment;
    private final ShippingClient shipping;
    @Value("${saga.shipping.enabled:false}")
    private boolean shippingEnabled;
    private final RetryTemplate retry = new RetryTemplate(RetryPolicy.builder()
            .includes(RestClientException.class).maxRetries(2).delay(Duration.ofSeconds(1)).build());

    // Do not hold a database transaction open across remote calls.
    public synchronized Order compensate(Long id) {
        Order order = orders.findById(id);
        if (order.getSagaStatus() == SagaStatus.COMPENSATED) return order;
        if (order.getSagaStatus() != SagaStatus.COMPENSATING && order.getSagaStatus() != SagaStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only failed or compensating sagas can be recovered");
        }
        orders.updateSagaStatus(id, SagaStatus.COMPENSATING);
        boolean done = true;
        if (shippingEnabled) done &= run(id, "CANCEL_SHIPPING", () -> shipping.cancel(id));
        done &= run(id, "REFUND_PAYMENT", () -> payment.refund(id));
        done &= run(id, "RELEASE_INVENTORY", () -> inventory.release(id));
        if (done) {
            orders.cancelOrder(id);
            orders.log(id, "CANCEL_ORDER", "SUCCESS", "Manual recovery completed");
        }
        return orders.findById(id);
    }

    private boolean run(Long id, String name, Runnable action) {
        AtomicInteger attempts = new AtomicInteger();
        try {
            retry.execute(() -> {
                int attempt = attempts.incrementAndGet();
                try {
                    action.run();
                    orders.log(id, "RECOVERY", "SUCCESS", name + " attempt " + attempt);
                } catch (RestClientException ex) {
                    orders.log(id, "RECOVERY", "FAILED", name + " attempt " + attempt);
                    throw ex;
                }
                return null;
            });
            return true;
        } catch (RetryException ex) {
            return false;
        }
    }
}
