package com.capics.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mrp_plan")
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

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItemNumber() {
        return this.itemNumber;
    }

    public void setItemNumber(String itemNumber) {
        this.itemNumber = itemNumber;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSite() {
        return this.site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getProductionLine() {
        return this.productionLine;
    }

    public void setProductionLine(String productionLine) {
        this.productionLine = productionLine;
    }

    public LocalDate getReleaseDate() {
        return this.releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public LocalDate getDueDate() {
        return this.dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getQuantityScheduled() {
        return this.quantityScheduled;
    }

    public void setQuantityScheduled(BigDecimal quantityScheduled) {
        this.quantityScheduled = quantityScheduled;
    }

    public BigDecimal getQuantityCompleted() {
        return this.quantityCompleted;
    }

    public void setQuantityCompleted(BigDecimal quantityCompleted) {
        this.quantityCompleted = quantityCompleted;
    }

    public String getRoutingCode() {
        return this.routingCode;
    }

    public void setRoutingCode(String routingCode) {
        this.routingCode = routingCode;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCreatedBy() {
        return this.createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getFileName() {
        return this.fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedBy() {
        return this.updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
