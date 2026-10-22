package com.mss301.shipping;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
public class ShippingController {
    private final ShippingService service;
    private final ShipmentRepository repository;

    @PostMapping("/create")
    public Shipment create(@RequestBody ShippingService.CreateShipmentRequest request) {
        return service.create(request);
    }

    @PostMapping("/cancel/{orderId}")
    public String cancel(@PathVariable Long orderId) {
        service.cancel(orderId);
        return "CANCELLED";
    }

    @GetMapping
    public List<Shipment> shipments() { return repository.findAll(); }
}
