package com.capics.dto;

import java.math.BigDecimal;

public class WmsBomRow {
    private String productNumber;
    private String componentNumber;
    private String lineCode;
    private Integer bomLevel;
    private BigDecimal bomQuantity;

    public String getProductNumber() {
        return productNumber;
    }

    public void setProductNumber(String productNumber) {
        this.productNumber = productNumber;
    }

    public String getComponentNumber() {
        return componentNumber;
    }

    public void setComponentNumber(String componentNumber) {
        this.componentNumber = componentNumber;
    }

    public String getLineCode() {
        return lineCode;
    }

    public void setLineCode(String lineCode) {
        this.lineCode = lineCode;
    }

    public Integer getBomLevel() {
        return bomLevel;
    }

    public void setBomLevel(Integer bomLevel) {
        this.bomLevel = bomLevel;
    }

    public BigDecimal getBomQuantity() {
        return bomQuantity;
    }

    public void setBomQuantity(BigDecimal bomQuantity) {
        this.bomQuantity = bomQuantity;
    }
}
