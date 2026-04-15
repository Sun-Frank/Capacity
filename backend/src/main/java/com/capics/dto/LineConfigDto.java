package com.capics.dto;

import java.math.BigDecimal;

public class LineConfigDto {
    private String lineCode;
    private String lineName;
    private Integer workingDaysPerWeek;
    private Integer shiftsPerDay;
    private BigDecimal hoursPerShift;
    private Boolean isActive;
    private String createdBy;
    private String createdAt;
    private String updatedBy;
    private String updatedAt;

    public String getLineCode() {
        return this.lineCode;
    }

    public void setLineCode(String lineCode) {
        this.lineCode = lineCode;
    }

    public Integer getWorkingDaysPerWeek() {
        return this.workingDaysPerWeek;
    }

    public String getLineName() {
        return this.lineName;
    }

    public void setLineName(String lineName) {
        this.lineName = lineName;
    }

    public void setWorkingDaysPerWeek(Integer workingDaysPerWeek) {
        this.workingDaysPerWeek = workingDaysPerWeek;
    }

    public Integer getShiftsPerDay() {
        return this.shiftsPerDay;
    }

    public void setShiftsPerDay(Integer shiftsPerDay) {
        this.shiftsPerDay = shiftsPerDay;
    }

    public BigDecimal getHoursPerShift() {
        return this.hoursPerShift;
    }

    public void setHoursPerShift(BigDecimal hoursPerShift) {
        this.hoursPerShift = hoursPerShift;
    }

    public Boolean getIsActive() {
        return this.isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getCreatedBy() {
        return this.createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedBy() {
        return this.updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
