package com.capics.dto;

import lombok.Data;
import java.util.List;

@Data
public class RoutingFullDto {
    private Long id;
    private String productNumber;
    private String description;
    private String version;
    private String createdBy;
    private String createdAt;
    private String updatedBy;
    private String updatedAt;
    private List<RoutingItemDto> items;
}
