package com.mss301.shipping;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ShippingService {
    private final ShipmentRepository repository;

    public record CreateShipmentRequest(Long orderId, String productId, int quantity) { }

    @Transactional
    public Shipment create(CreateShipmentRequest request) {
        var existing = repository.findById(request.orderId());
        if (existing.isPresent()) return existing.get();
        if (request.quantity() > 3) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Shipping supports at most 3 items");
        }
        if (request.orderId() == null || request.quantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order id and positive quantity are required");
        }
        Shipment shipment = new Shipment();
        shipment.setOrderId(request.orderId());
        shipment.setProductId(request.productId());
        shipment.setQuantity(request.quantity());
        shipment.setStatus(Shipment.Status.CREATED);
        return repository.save(shipment);
    }

    @Transactional
    public void cancel(Long id) {
        repository.findById(id).ifPresent(s -> s.setStatus(Shipment.Status.CANCELLED));
    }
}
