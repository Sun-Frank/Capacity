package com.capics.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RoutingDto {
    private Long id;
    private String productNumber;
    private String description;
    private String version;
    private String createdBy;
    private String createdAt;
    private String updatedBy;
    private String updatedAt;
}
