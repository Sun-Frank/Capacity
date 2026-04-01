package com.capics.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
public class MrpReportDto {
    private String itemNumber;
    private String description;
    private Map<String, BigDecimal> weeklyDemand;
    private Map<String, BigDecimal> monthlyDemand;
}
