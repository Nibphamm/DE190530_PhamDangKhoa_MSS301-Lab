package com.mss301.order.saga;

import com.mss301.order.client.*;
import com.mss301.order.dto.CreateOrderRequest;
import com.mss301.order.entity.*;
import com.mss301.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderSagaOrchestratorTest {
    @Mock OrderService orders;
    @Mock InventoryClient inventory;
    @Mock PaymentClient payment;
    @Mock ShippingClient shipping;
    OrderSagaOrchestrator saga;
    final CreateOrderRequest request = new CreateOrderRequest("C1", "P2", 4, 80);

    @BeforeEach
    void setup() {
        saga = new OrderSagaOrchestrator(orders, inventory, payment, shipping);
        Order order = new Order();
        order.setId(1L);
        when(orders.createPendingOrder(request)).thenReturn(order);
        when(orders.findById(1L)).thenReturn(order);
    }

    @Test
    void completesInBusinessOrder() {
        saga.placeOrder(request);
        InOrder flow = inOrder(orders, inventory, payment);
        flow.verify(orders).createPendingOrder(request);
        flow.verify(inventory).reserve(1L, "P2", 4);
        flow.verify(payment).pay(1L, "C1", 80);
        flow.verify(orders).confirmOrder(1L);
        verify(inventory, never()).release(anyLong());
        verify(payment, never()).refund(anyLong());
    }

    @Test
    void paymentFailureReleasesInventoryBeforeCancelling() {
        doThrow(new RestClientException("offline")).when(payment).pay(1L, "C1", 80);
        saga.placeOrder(request);
        InOrder flow = inOrder(orders, inventory);
        flow.verify(orders).markFailed(eq(1L), contains("offline"));
        flow.verify(orders).updateSagaStatus(1L, SagaStatus.COMPENSATING);
        flow.verify(inventory).release(1L);
        flow.verify(orders).cancelOrder(1L);
        verify(payment, never()).refund(anyLong());
    }

    @Test
    void inventoryFailureDoesNotCompensateUnfinishedStep() {
        doThrow(new RestClientException("stock")).when(inventory).reserve(1L, "P2", 4);
        saga.placeOrder(request);
        verify(inventory, never()).release(anyLong());
        verifyNoInteractions(payment);
        verify(orders).cancelOrder(1L);
    }

    @Test
    void shippingFailureRefundsBeforeReleasing() {
        ReflectionTestUtils.setField(saga, "shippingEnabled", true);
        doThrow(new RestClientException("shipping rejected")).when(shipping).create(1L, "P2", 4);
        saga.placeOrder(request);
        InOrder undo = inOrder(payment, inventory, orders);
        undo.verify(payment).refund(1L);
        undo.verify(inventory).release(1L);
        undo.verify(orders).cancelOrder(1L);
    }

    @Test
    void retriesOnlyThreeTimesAndLeavesOrderCompensating() {
        doThrow(new RestClientException("payment offline")).when(payment).pay(1L, "C1", 80);
        doThrow(new RestClientException("inventory offline")).when(inventory).release(1L);
        saga.placeOrder(request);
        verify(inventory, times(3)).release(1L);
        verify(orders, times(3)).log(eq(1L), eq("COMPENSATION"), eq("FAILED"), anyString());
        verify(orders, never()).cancelOrder(anyLong());
    }

    @Test
    void retryCanRecoverAfterTransientFailure() {
        doThrow(new RestClientException("payment offline")).when(payment).pay(1L, "C1", 80);
        doThrow(new RestClientException("temporary")).doNothing().when(inventory).release(1L);
        saga.placeOrder(request);
        verify(inventory, times(2)).release(1L);
        verify(orders).cancelOrder(1L);
    }

    @Test
    void timeoutProfileRefundsEvenWhenPaymentAcknowledgementIsLost() {
        ReflectionTestUtils.setField(saga, "timeoutSafe", true);
        doThrow(new RestClientException("read timed out")).when(payment).pay(1L, "C1", 80);
        saga.placeOrder(request);
        InOrder undo = inOrder(payment, inventory, orders);
        undo.verify(payment).refund(1L);
        undo.verify(inventory).release(1L);
        undo.verify(orders).cancelOrder(1L);
    }
}
