package com.capics.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class LineConfigDto {
    private String lineCode;
    private Integer workingDaysPerWeek;
    private Integer shiftsPerDay;
    private BigDecimal hoursPerShift;
    private Boolean isActive;
    private String createdBy;
    private String createdAt;
    private String updatedBy;
    private String updatedAt;
}
