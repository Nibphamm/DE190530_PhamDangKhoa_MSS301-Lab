package fu.de190530.post.controller;

import fu.de190530.post.dto.*;
import fu.de190530.post.service.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/posts", produces = "application/json")
public class DE190530PostController {
    private final PostService service;
    public DE190530PostController(PostService service) { this.service = service; }
    @PostMapping(consumes = "application/json")
    public ResponseEntity<?> create(@RequestBody PostDTO dto) {
        return ResponseEntity.status(201).body(ApiResponse.of(1, "Post created successfully", service.create(dto)));
    }
    @PutMapping(value = "/{postId}", consumes = "application/json")
    public ApiResponse<?> update(@PathVariable Long postId, @RequestBody PostDTO dto) {
        return ApiResponse.of(1, "Post updated successfully", service.update(postId, dto));
    }
    @GetMapping("/{postId}")
    public ApiResponse<?> detail(@PathVariable Long postId) {
        return ApiResponse.of(1, "Successful", service.detail(postId));
    }
    @DeleteMapping("/{postId}")
    public ApiResponse<?> delete(@PathVariable Long postId) {
        service.delete(postId);
        return ApiResponse.of(1, "Post status updated successfully", null);
    }
    @GetMapping
    public ApiResponse<?> list(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) String advertiserName,
                        @RequestParam(required = false) String status) {
        return ApiResponse.of(1, "Successful", service.list(page, size, advertiserName, status));
    }
}
