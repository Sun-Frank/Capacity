package com.capics.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LineRealtimeDto {
    private Long id;
    private String lineCode;
    private String itemNumber;
    private String componentNumber;
    private String description;
    private BigDecimal shiftOutput;
    private Integer shiftWorkers;
    private BigDecimal ct;
    private BigDecimal oee;
    private String weeklyDemand;
    private String mrpVersion;
    private LocalDateTime calculatedAt;
}
