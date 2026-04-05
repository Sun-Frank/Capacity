package com.capics.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_family")
@IdClass(ProductFamilyId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductFamily {

    @Id
    @Column(length = 50)
    private String familyCode;

    @Id
    @Column(length = 50)
    private String lineCode;

    @Column(length = 100)
    private String codingRule;

    @Column(precision = 10, scale = 2)
    private BigDecimal cycleTime;

    @Column(precision = 5, scale = 2)
    private BigDecimal oee;

    @Column
    private Integer workerCount;

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

    @Column(length = 255)
    private String description;

    @Column(name = "pf", length = 100)
    private String pf;

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
