package com.capics.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "simulation_snapshot")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_by", length = 50, nullable = false)
    private String createdBy;

    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @Column(length = 50, nullable = false)
    private String version;

    @Column(name = "snapshot_name", length = 100, nullable = false)
    private String snapshotName;

    @Column(length = 20, nullable = false)
    private String source; // static or dynamic

    @Column(length = 20, nullable = false)
    private String dimension; // week or month

    @Column(name = "lines_data", columnDefinition = "TEXT")
    private String linesData; // JSON: 产线 LOAD 矩阵

    @Column(columnDefinition = "TEXT")
    private String dates; // JSON: 日期列表

    @Column(name = "date_labels", columnDefinition = "TEXT")
    private String dateLabels; // JSON: 日期标签映射

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
