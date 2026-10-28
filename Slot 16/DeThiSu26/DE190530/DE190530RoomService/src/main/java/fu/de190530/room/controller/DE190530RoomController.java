package fu.de190530.room.controller;

import fu.de190530.room.dto.*;
import fu.de190530.room.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/rooms", produces = "application/json")
public class DE190530RoomController {
    private final RoomService service;
    public DE190530RoomController(RoomService service) { this.service = service; }
    @PostMapping(consumes = "application/json")
    public ResponseEntity<?> create(@RequestBody RoomDTO dto) {
        return ResponseEntity.status(201).body(ApiResponseDTO.of(1, "Room created successfully", service.create(dto)));
    }
    @PutMapping(value = "/{roomId}", consumes = "application/json")
    public ApiResponseDTO<?> update(@PathVariable Long roomId, @RequestBody RoomDTO dto) {
        return ApiResponseDTO.of(1, "Room updated successfully", service.update(roomId, dto));
    }
    @GetMapping("/{roomId}")
    public ApiResponseDTO<?> detail(@PathVariable Long roomId) {
        return ApiResponseDTO.of(1, "Successful", service.detail(roomId));
    }
    @DeleteMapping("/{roomId}")
    public ApiResponseDTO<?> delete(@PathVariable Long roomId) {
        service.delete(roomId);
        return ApiResponseDTO.of(1, "Room status updated successfully", null);
    }
    @GetMapping
    public ApiResponseDTO<?> list(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) String roomType,
                        @RequestParam(required = false) String status) {
        return ApiResponseDTO.of(1, "Successful", service.list(page, size, roomType, status));
    }
}
