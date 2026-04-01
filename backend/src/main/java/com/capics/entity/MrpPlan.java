package com.capics.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mrp_plan")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MrpPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    private String itemNumber;

    @Column(length = 255)
    private String description;

    @Column(length = 50, nullable = false)
    private String site;

    @Column(length = 100)
    private String productionLine;

    @Column(nullable = false)
    private LocalDate releaseDate;

    @Column
    private LocalDate dueDate;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal quantityScheduled;

    @Column(precision = 18, scale = 2)
    private BigDecimal quantityCompleted = BigDecimal.ZERO;

    @Column(length = 50)
    private String routingCode;

    @Column(length = 10, nullable = false)
    private String version;

    @Column(length = 50)
    private String createdBy;

    @Column(length = 255)
    private String fileName;

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
        if (quantityCompleted == null) {
            quantityCompleted = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
