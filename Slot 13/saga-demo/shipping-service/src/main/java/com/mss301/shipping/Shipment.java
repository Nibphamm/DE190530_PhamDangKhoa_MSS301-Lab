package com.mss301.shipping;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor
public class Shipment {
    @Id
    private Long orderId;
    private String productId;
    private int quantity;
    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status { CREATED, CANCELLED }
}
