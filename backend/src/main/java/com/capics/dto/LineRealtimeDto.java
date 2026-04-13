package com.capics.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLineCode() {
        return this.lineCode;
    }

    public void setLineCode(String lineCode) {
        this.lineCode = lineCode;
    }

    public String getItemNumber() {
        return this.itemNumber;
    }

    public void setItemNumber(String itemNumber) {
        this.itemNumber = itemNumber;
    }

    public String getComponentNumber() {
        return this.componentNumber;
    }

    public void setComponentNumber(String componentNumber) {
        this.componentNumber = componentNumber;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getShiftOutput() {
        return this.shiftOutput;
    }

    public void setShiftOutput(BigDecimal shiftOutput) {
        this.shiftOutput = shiftOutput;
    }

    public Integer getShiftWorkers() {
        return this.shiftWorkers;
    }

    public void setShiftWorkers(Integer shiftWorkers) {
        this.shiftWorkers = shiftWorkers;
    }

    public BigDecimal getCt() {
        return this.ct;
    }

    public void setCt(BigDecimal ct) {
        this.ct = ct;
    }

    public BigDecimal getOee() {
        return this.oee;
    }

    public void setOee(BigDecimal oee) {
        this.oee = oee;
    }

    public String getWeeklyDemand() {
        return this.weeklyDemand;
    }

    public void setWeeklyDemand(String weeklyDemand) {
        this.weeklyDemand = weeklyDemand;
    }

    public String getMrpVersion() {
        return this.mrpVersion;
    }

    public void setMrpVersion(String mrpVersion) {
        this.mrpVersion = mrpVersion;
    }

    public LocalDateTime getCalculatedAt() {
        return this.calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
