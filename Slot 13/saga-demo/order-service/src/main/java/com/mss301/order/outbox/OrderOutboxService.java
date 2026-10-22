package com.mss301.order.outbox;

import com.mss301.order.dto.CreateOrderRequest;
import com.mss301.order.entity.*;
import com.mss301.order.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import static com.mss301.order.outbox.OutboxCommand.Command.*;
import static com.mss301.order.outbox.OutboxCommand.State.*;

@Service
@Profile("outbox")
@RequiredArgsConstructor
public class OrderOutboxService implements OrderPlacement {
    private final OrderService orders;
    private final OutboxRepository repository;

    @Override
    @Transactional
    public Order placeOrder(CreateOrderRequest request) {
        Order order = orders.createPendingOrder(request);
        orders.updateSagaStatus(order.getId(), SagaStatus.EXECUTING);
        orders.log(order.getId(), "CREATE_ORDER", "SUCCESS", "Order and RESERVE command committed together");
        repository.save(new OutboxCommand(order.getId(), RESERVE));
        return order;
    }

    @Transactional
    public Optional<OutboxCommand> claim() {
        // A crashed worker's PROCESSING command becomes eligible again after its lease expires.
        var next = repository.findFirstByStateInAndAvailableAtBeforeOrderByIdAsc(
                List.of(PENDING, PROCESSING), LocalDateTime.now());
        next.ifPresent(command -> {
            command.setState(PROCESSING);
            command.setAttempts(command.getAttempts() + 1);
            command.setAvailableAt(LocalDateTime.now().plusSeconds(30));
        });
        return next;
    }

    @Transactional
    public void complete(Long id) {
        OutboxCommand command = repository.findById(id).orElseThrow();
        if (command.getState() == DONE) return;
        Long orderId = command.getOrderId();
        command.setState(DONE);
        switch (command.getCommand()) {
            case RESERVE -> { orders.log(orderId, "RESERVE_INVENTORY", "SUCCESS", "Outbox delivered RESERVE"); enqueue(orderId, PAY); }
            case PAY -> { orders.log(orderId, "PROCESS_PAYMENT", "SUCCESS", "Outbox delivered PAY"); enqueue(orderId, CONFIRM); }
            case CONFIRM -> { orders.confirmOrder(orderId); orders.log(orderId, "CONFIRM_ORDER", "SUCCESS", "Outbox saga COMPLETED"); }
            case REFUND -> { orders.log(orderId, "COMPENSATION", "SUCCESS", "REFUND_PAYMENT"); enqueue(orderId, RELEASE); }
            case RELEASE -> { orders.log(orderId, "COMPENSATION", "SUCCESS", "RELEASE_INVENTORY"); enqueue(orderId, CANCEL); }
            case CANCEL -> { orders.cancelOrder(orderId); orders.log(orderId, "CANCEL_ORDER", "SUCCESS", "Outbox saga COMPENSATED"); }
        }
    }

    @Transactional
    public void failed(Long id, String reason) {
        OutboxCommand command = repository.findById(id).orElseThrow();
        if (command.getState() == DONE) return;
        command.setLastError(reason.length() > 2000 ? reason.substring(0, 2000) : reason);
        Long orderId = command.getOrderId();
        if (command.getCommand() == RESERVE || command.getCommand() == PAY) {
            command.setState(FAILED);
            orders.markFailed(orderId, reason);
            orders.log(orderId, "SAGA", "FAILED", reason);
            orders.updateSagaStatus(orderId, SagaStatus.COMPENSATING);
            // Remote acknowledgement may have been lost after commit: undo both steps safely.
            enqueue(orderId, REFUND);
        } else {
            orders.log(orderId, "COMPENSATION", "FAILED", command.getCommand() + " attempt " + command.getAttempts());
            command.setState(command.getAttempts() >= 3 ? FAILED : PENDING);
            command.setAvailableAt(LocalDateTime.now().plusSeconds(1));
        }
    }

    private void enqueue(Long orderId, OutboxCommand.Command command) {
        repository.save(new OutboxCommand(orderId, command));
    }
}
