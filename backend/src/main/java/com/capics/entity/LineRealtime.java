package com.capics.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "line_realtime")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineRealtime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    private String lineCode;

    @Column(length = 50, nullable = false)
    private String itemNumber;

    @Column(length = 50, nullable = false)
    private String componentNumber;

    @Column(length = 255)
    private String description;

    @Column(precision = 12, scale = 2)
    private BigDecimal shiftOutput;

    @Column
    private Integer shiftWorkers;

    @Column(precision = 10, scale = 2)
    private BigDecimal ct;

    @Column(precision = 5, scale = 4)
    private BigDecimal oee;

    @Column(columnDefinition = "jsonb")
    private String weeklyDemand;

    @Column(length = 10)
    private String mrpVersion;

    @Column
    private LocalDateTime calculatedAt;

    @PrePersist
    protected void onCreate() {
        calculatedAt = LocalDateTime.now();
    }
}
