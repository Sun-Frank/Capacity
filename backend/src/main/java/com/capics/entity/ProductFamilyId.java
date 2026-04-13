package com.capics.entity;

import java.io.Serializable;
import java.util.Objects;

public class ProductFamilyId implements Serializable {
    private String familyCode;
    private String lineCode;

    public ProductFamilyId() {
    }

    public ProductFamilyId(String familyCode, String lineCode) {
        this.familyCode = familyCode;
        this.lineCode = lineCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductFamilyId that = (ProductFamilyId) o;
        return Objects.equals(familyCode, that.familyCode) &&
                Objects.equals(lineCode, that.lineCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(familyCode, lineCode);
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
}
