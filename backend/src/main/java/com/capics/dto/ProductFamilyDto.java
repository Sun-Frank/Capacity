package com.capics.dto;

import java.math.BigDecimal;

public class ProductFamilyDto {
    private String familyCode;
    private String lineCode;
    private String codingRule;
    private BigDecimal cycleTime;
    private BigDecimal oee;
    private Integer workerCount;
    private String version;
    private String createdBy;
    private String createdAt;
    private String updatedBy;
    private String updatedAt;
    private String description;
    private String pf;

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

    public String getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedBy() {
        return this.updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
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
