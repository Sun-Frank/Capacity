package com.capics.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "simulation_snapshot")
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
    private String source;

    @Column(length = 20, nullable = false)
    private String dimension;

    @Column(name = "lines_data", columnDefinition = "TEXT")
    private String linesData;

    @Column(columnDefinition = "TEXT")
    private String dates;

    @Column(name = "date_labels", columnDefinition = "TEXT")
    private String dateLabels;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public String getCreatedBy() { return this.createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getFileName() { return this.fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getVersion() { return this.version; }
    public void setVersion(String version) { this.version = version; }
    public String getSnapshotName() { return this.snapshotName; }
    public void setSnapshotName(String snapshotName) { this.snapshotName = snapshotName; }
    public String getSource() { return this.source; }
    public void setSource(String source) { this.source = source; }
    public String getDimension() { return this.dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }
    public String getLinesData() { return this.linesData; }
    public void setLinesData(String linesData) { this.linesData = linesData; }
    public String getDates() { return this.dates; }
    public void setDates(String dates) { this.dates = dates; }
    public String getDateLabels() { return this.dateLabels; }
    public void setDateLabels(String dateLabels) { this.dateLabels = dateLabels; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
