package com.capics.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
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
}
