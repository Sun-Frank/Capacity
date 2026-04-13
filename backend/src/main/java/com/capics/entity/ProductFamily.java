package com.capics.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_family")
@IdClass(ProductFamilyId.class)
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

    public String getFamilyCode() {
        return this.familyCode;
    }

    public void setFamilyCode(String familyCode) {
        this.familyCode = familyCode;
    }

    public String getLineCode() {
        return this.lineCode;
    }

    public void setLineCode(String lineCode) {
        this.lineCode = lineCode;
    }

    public String getCodingRule() {
        return this.codingRule;
    }

    public void setCodingRule(String codingRule) {
        this.codingRule = codingRule;
    }

    public BigDecimal getCycleTime() {
        return this.cycleTime;
    }

    public void setCycleTime(BigDecimal cycleTime) {
        this.cycleTime = cycleTime;
    }

    public BigDecimal getOee() {
        return this.oee;
    }

    public void setOee(BigDecimal oee) {
        this.oee = oee;
    }

    public Integer getWorkerCount() {
        return this.workerCount;
    }

    public void setWorkerCount(Integer workerCount) {
        this.workerCount = workerCount;
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

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPf() {
        return this.pf;
    }

    public void setPf(String pf) {
        this.pf = pf;
    }
}
