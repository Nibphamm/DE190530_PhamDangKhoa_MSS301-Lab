package fu.de190530.room.dto;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public record ApiResponseDTO<T>(int status, String message, T data, String timestamp) {
    public static <T> ApiResponseDTO<T> of(int status, String message, T data) {
        return new ApiResponseDTO<>(status, message, data,
                Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
    }
}
