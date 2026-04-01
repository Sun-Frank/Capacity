package com.capics.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.Serializable;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductFamilyId implements Serializable {
    private String familyCode;
    private String lineCode;

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
}
