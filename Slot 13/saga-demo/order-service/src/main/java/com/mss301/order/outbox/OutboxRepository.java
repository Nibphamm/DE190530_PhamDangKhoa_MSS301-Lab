package com.mss301.order.outbox;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.time.LocalDateTime;
import java.util.*;

public interface OutboxRepository extends JpaRepository<OutboxCommand, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OutboxCommand> findFirstByStateInAndAvailableAtBeforeOrderByIdAsc(
            Collection<OutboxCommand.State> states, LocalDateTime now);
    List<OutboxCommand> findByOrderIdOrderByIdAsc(Long orderId);
}
