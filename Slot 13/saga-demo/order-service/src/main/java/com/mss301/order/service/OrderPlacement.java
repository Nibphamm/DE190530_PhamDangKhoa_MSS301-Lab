package com.mss301.order.service;

import com.mss301.order.dto.CreateOrderRequest;
import com.mss301.order.entity.Order;

public interface OrderPlacement {
    Order placeOrder(CreateOrderRequest request);
}
