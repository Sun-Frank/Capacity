package com.capics.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product")
@IdClass(ProductId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @Column(length = 50)
    private String itemNumber;

    @Id
    @Column(length = 50)
    private String lineCode;

    @Column(length = 50)
    private String familyCode;

    @Column(precision = 10, scale = 2)
    private BigDecimal cycleTime;

    @Column(precision = 5, scale = 2)
    private BigDecimal oee;

    @Column
    private Integer workerCount;

    @Column(length = 255)
    private String description;

    @Column(length = 20)
    private String version;

    @Column(length = 50)
    private String createdBy;

    @Column
    private LocalDateTime createdAt;

    @Column(length = 50)
    private String updatedBy;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
