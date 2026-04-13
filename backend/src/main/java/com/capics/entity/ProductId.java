package com.capics.entity;

import java.io.Serializable;
import java.util.Objects;

public class ProductId implements Serializable {
    private String itemNumber;
    private String lineCode;

    public ProductId() {
    }

    public ProductId(String itemNumber, String lineCode) {
        this.itemNumber = itemNumber;
        this.lineCode = lineCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductId that = (ProductId) o;
        return Objects.equals(itemNumber, that.itemNumber) &&
                Objects.equals(lineCode, that.lineCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemNumber, lineCode);
    }

    public String getItemNumber() {
        return this.itemNumber;
    }

    public void setItemNumber(String itemNumber) {
        this.itemNumber = itemNumber;
    }

    public String getLineCode() {
        return this.lineCode;
    }

    public void setLineCode(String lineCode) {
        this.lineCode = lineCode;
    }
}
