package com.capics.dto;

import java.math.BigDecimal;

public class RoutingItemDto {
    private Long id;
    private Long routingId;
    private String productNumber;
    private String routingDescription;
    private String componentNumber;
    private String lineCode;
    private Integer bomLevel;
    private BigDecimal bomQuantity;
    private String createdAt;
    private String updatedBy;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoutingId() {
        return this.routingId;
    }

    public void setRoutingId(Long routingId) {
        this.routingId = routingId;
    }

    public String getProductNumber() {
        return this.productNumber;
    }

    public void setProductNumber(String productNumber) {
        this.productNumber = productNumber;
    }

    public String getRoutingDescription() {
        return this.routingDescription;
    }

    public void setRoutingDescription(String routingDescription) {
        this.routingDescription = routingDescription;
    }

    public String getComponentNumber() {
        return this.componentNumber;
    }

    public void setComponentNumber(String componentNumber) {
        this.componentNumber = componentNumber;
    }

    public String getLineCode() {
        return this.lineCode;
    }

    public void setLineCode(String lineCode) {
        this.lineCode = lineCode;
    }

    public Integer getBomLevel() {
        return this.bomLevel;
    }

    public void setBomLevel(Integer bomLevel) {
        this.bomLevel = bomLevel;
    }

    public BigDecimal getBomQuantity() {
        return this.bomQuantity;
    }

    public void setBomQuantity(BigDecimal bomQuantity) {
        this.bomQuantity = bomQuantity;
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
}
