package fu.de190530.user.controller;

import fu.de190530.user.dto.*;
import fu.de190530.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/users", produces = "application/json")
public class DE190530UserController {
    private final UserService service;
    public DE190530UserController(UserService service) { this.service = service; }
    @PostMapping(consumes = "application/json")
    public ResponseEntity<?> create(@RequestBody UserDTO dto) {
        return ResponseEntity.status(201).body(ApiResponse.of(1, "User created successfully", service.create(dto)));
    }
    @PutMapping(value = "/{userId}", consumes = "application/json")
    public ApiResponse<?> update(@PathVariable Long userId, @RequestBody UserDTO dto) {
        return ApiResponse.of(1, "User updated successfully", service.update(userId, dto));
    }
    @GetMapping("/{userId}")
    public ApiResponse<?> detail(@PathVariable Long userId) {
        return ApiResponse.of(1, "Successful", service.detail(userId));
    }
    @DeleteMapping("/{userId}")
    public ApiResponse<?> delete(@PathVariable Long userId) {
        service.delete(userId);
        return ApiResponse.of(1, "User status updated successfully", null);
    }
    @GetMapping
    public ApiResponse<?> list(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) String role,
                        @RequestParam(required = false) String status) {
        return ApiResponse.of(1, "Successful", service.list(page, size, role, status));
    }
}
