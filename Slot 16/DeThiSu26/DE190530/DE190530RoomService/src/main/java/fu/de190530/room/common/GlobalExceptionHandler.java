package fu.de190530.room.common;

import fu.de190530.room.dto.ApiResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> api(ApiException e) {
        return ResponseEntity.status(e.getHttp()).body(ApiResponseDTO.of(e.getStatus(), e.getMessage(), null));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<?> invalid(Exception e) {
        return api(new ApiException(406, 2, "Invalid request data"));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> integrity(DataIntegrityViolationException e) {
        // A concurrent create/update may bypass the early uniqueness check.
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof java.sql.SQLException sql &&
                    (sql.getErrorCode() == 2601 || sql.getErrorCode() == 2627 || "23505".equals(sql.getSQLState()))) {
                return api(new ApiException(226, 3, "Unique value is duplicated"));
            }
            cause = cause.getCause();
        }
        return unexpected(e);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unexpected(Exception e) {
        LOG.error("Request failed", e);
        return api(new ApiException(500, 0, "Internal server error"));
    }
}
