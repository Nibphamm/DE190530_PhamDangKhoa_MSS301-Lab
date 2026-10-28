package fu.de190530.reservation.controller;

import fu.de190530.reservation.dto.*;
import fu.de190530.reservation.service.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/reservations", produces = "application/json")
public class DE190530ReservationController {
    private final ReservationService service;
    public DE190530ReservationController(ReservationService service) { this.service = service; }
    @PostMapping(consumes = "application/json")
    public ResponseEntity<?> create(@RequestBody ReservationDTO dto) {
        return ResponseEntity.status(201).body(ApiResponseDTO.of(1, "Reservation created successfully", service.create(dto)));
    }
    @PutMapping(value = "/{reservationId}", consumes = "application/json")
    public ApiResponseDTO<?> update(@PathVariable Long reservationId, @RequestBody ReservationDTO dto) {
        return ApiResponseDTO.of(1, "Reservation updated successfully", service.update(reservationId, dto));
    }
    @GetMapping("/{reservationId}")
    public ApiResponseDTO<?> detail(@PathVariable Long reservationId) {
        return ApiResponseDTO.of(1, "Successful", service.detail(reservationId));
    }
    @DeleteMapping("/{reservationId}")
    public ApiResponseDTO<?> delete(@PathVariable Long reservationId) {
        service.delete(reservationId);
        return ApiResponseDTO.of(1, "Reservation status updated successfully", null);
    }
    @GetMapping
    public ApiResponseDTO<?> list(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) String guestName,
                        @RequestParam(required = false) String status) {
        return ApiResponseDTO.of(1, "Successful", service.list(page, size, guestName, status));
    }
}
