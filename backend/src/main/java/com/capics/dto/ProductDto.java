package com.capics.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductDto {
    private String itemNumber;
    private String lineCode;
    private String familyCode;
    private BigDecimal cycleTime;
    private BigDecimal oee;
    private Integer workerCount;
    private String description;
    private String version;
    private String createdBy;
    private String createdAt;
    private String updatedBy;
    private String updatedAt;
    private String pf;
}
