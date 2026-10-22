package com.mss301.order.outbox;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbox", uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "command"}))
@Getter @Setter @NoArgsConstructor
public class OutboxCommand {
    public enum Command { RESERVE, PAY, CONFIRM, REFUND, RELEASE, CANCEL }
    public enum State { PENDING, PROCESSING, DONE, FAILED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long orderId;
    @Enumerated(EnumType.STRING)
    private Command command;
    @Enumerated(EnumType.STRING)
    private State state;
    private int attempts;
    private LocalDateTime availableAt;
    @Column(length = 2000)
    private String lastError;

    public OutboxCommand(Long orderId, Command command) {
        this.orderId = orderId;
        this.command = command;
        this.state = State.PENDING;
        this.availableAt = LocalDateTime.now();
    }
}
