package fu.de190530.reservation.client;

import fu.de190530.reservation.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "room-service", url = "${room.service.url}")
public interface RoomClient {
    @GetMapping("/api/rooms/{id}")
    ApiResponseDTO<RoomDTO> detail(@PathVariable("id") Long id);
}
