package com.mss301.order.controller;

import com.mss301.order.outbox.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("outbox")
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OutboxController {
    private final OutboxRepository repository;
    @GetMapping("/{id}/outbox")
    public List<OutboxCommand> commands(@PathVariable Long id) { return repository.findByOrderIdOrderByIdAsc(id); }
}
