package com.mss301.order.controller;

import com.mss301.order.entity.Order;
import com.mss301.order.service.SagaRecoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("recovery")
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class SagaRecoveryController {
    private final SagaRecoveryService recovery;

    @PostMapping("/{id}/compensate")
    public Order compensate(@PathVariable Long id) { return recovery.compensate(id); }
}
