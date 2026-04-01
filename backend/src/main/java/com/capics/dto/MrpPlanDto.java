package com.capics.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MrpPlanDto {
    private Long id;
    private String itemNumber;
    private String description;
    private String site;
    private String productionLine;
    private LocalDate releaseDate;
    private LocalDate dueDate;
    private BigDecimal quantityScheduled;
    private BigDecimal quantityCompleted;
    private String routingCode;
    private String version;
    private String createdBy;
    private String fileName;
    private LocalDate createdAt;
    private String updatedBy;
    private LocalDate updatedAt;
}
