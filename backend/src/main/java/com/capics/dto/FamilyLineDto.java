package com.capics.dto;

import lombok.Data;

@Data
public class FamilyLineDto {
    private String familyCode;
    private String lineCode;
    private String createdBy;
    private String createdAt;
    private String updatedBy;
    private String updatedAt;
    private String description;
}
