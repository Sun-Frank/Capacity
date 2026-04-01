package com.capics.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "line_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineConfig {

    @Id
    @Column(length = 50)
    private String lineCode;

    @Column
    private Integer workingDaysPerWeek = 5;

    @Column
    private Integer shiftsPerDay = 2;

    @Column(precision = 4, scale = 1)
    private BigDecimal hoursPerShift = new BigDecimal("8.0");

    @Column
    private Boolean isActive = true;

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
