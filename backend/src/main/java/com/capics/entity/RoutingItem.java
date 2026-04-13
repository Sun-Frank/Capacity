package com.capics.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "routing_item")
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

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoutingId() {
        return this.routingId;
    }

    public void setRoutingId(Long routingId) {
        this.routingId = routingId;
    }

    public String getComponentNumber() {
        return this.componentNumber;
    }

    public void setComponentNumber(String componentNumber) {
        this.componentNumber = componentNumber;
    }

    public String getLineCode() {
        return this.lineCode;
    }

    public void setLineCode(String lineCode) {
        this.lineCode = lineCode;
    }

    public Integer getBomLevel() {
        return this.bomLevel;
    }

    public void setBomLevel(Integer bomLevel) {
        this.bomLevel = bomLevel;
    }

    public BigDecimal getBomQuantity() {
        return this.bomQuantity;
    }

    public void setBomQuantity(BigDecimal bomQuantity) {
        this.bomQuantity = bomQuantity;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
