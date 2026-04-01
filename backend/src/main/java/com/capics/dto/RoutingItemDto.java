package com.capics.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RoutingItemDto {
    private Long id;
    private Long routingId;
    private String productNumber;      // 成品物料号
    private String routingDescription; // 工艺路线描述
    private String componentNumber;
    private String lineCode;
    private Integer bomLevel;
    private BigDecimal bomQuantity;
    private String createdAt;
    private String updatedBy;
}
