package com.capics.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "routing_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoutingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long routingId;

    @Column(length = 50, nullable = false)
    private String componentNumber;

    @Column(length = 50, nullable = false)
    private String lineCode;

    @Column
    private Integer bomLevel;

    @Column(precision = 10, scale = 2)
    private BigDecimal bomQuantity = BigDecimal.ONE;

    @Column
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (bomQuantity == null) {
            bomQuantity = BigDecimal.ONE;
        }
    }
}
