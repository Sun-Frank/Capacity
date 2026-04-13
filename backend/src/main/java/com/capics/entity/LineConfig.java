package com.capics.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "line_config")
public class LineConfig {

    @Id
    @Column(length = 50)
    private String lineCode;

    @Column
    private Integer workingDaysPerWeek = 5;

    @Column
    private Integer shiftsPerDay = 2;

    @Column(precision = 4, scale = 1)
    private BigDecimal hoursPerShift = new BigDecimal("8.0");

    @Column
    private Boolean isActive = true;

    @Column(length = 50)
    private String createdBy;

    @Column
    private LocalDateTime createdAt;

    @Column(length = 50)
    private String updatedBy;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getLineCode() {
        return this.lineCode;
    }

    public void setLineCode(String lineCode) {
        this.lineCode = lineCode;
    }

    public Integer getWorkingDaysPerWeek() {
        return this.workingDaysPerWeek;
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

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedBy() {
        return this.updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
