package com.capics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MrpPlanDto {
    private Long id;
    private String itemNumber;
    private String description;
    private String site;
    private String productionLine;
    private LocalDate releaseDate;
    private LocalDate dueDate;
    private BigDecimal quantityScheduled;
    private BigDecimal quantityCompleted;
    private String routingCode;
    private String version;
    private String createdBy;
    private String fileName;
    private LocalDate createdAt;
    private String updatedBy;
    private LocalDate updatedAt;

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

    public LocalDate getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedBy() {
        return this.updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDate getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(LocalDate updatedAt) {
        this.updatedAt = updatedAt;
    }
}
