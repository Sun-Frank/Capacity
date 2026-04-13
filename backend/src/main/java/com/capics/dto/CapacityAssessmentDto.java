package com.capics.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class CapacityAssessmentDto {
    private String lineCode;
    private String itemNumber;
    private String description;
    private String componentNumber;
    private BigDecimal shiftOutput;
    private Integer shiftWorkers;
    private BigDecimal ct;
    private BigDecimal oee;
    private Map<String, WeeklyLoad> weeklyData;
    private List<String> warnings;

    public static class WeeklyLoad {
        private BigDecimal demand;
        private BigDecimal load;

        public WeeklyLoad() {}

        public WeeklyLoad(BigDecimal demand, BigDecimal load) {
            this.demand = demand;
            this.load = load;
        }

        public BigDecimal getDemand() {
            return this.demand;
        }

        public void setDemand(BigDecimal demand) {
            this.demand = demand;
        }

        public BigDecimal getLoad() {
            return this.load;
        }

        public void setLoad(BigDecimal load) {
            this.load = load;
        }
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

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getComponentNumber() {
        return this.componentNumber;
    }

    public void setComponentNumber(String componentNumber) {
        this.componentNumber = componentNumber;
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

    public Map<String, WeeklyLoad> getWeeklyData() {
        return this.weeklyData;
    }

    public void setWeeklyData(Map<String, WeeklyLoad> weeklyData) {
        this.weeklyData = weeklyData;
    }

    public List<String> getWarnings() {
        return this.warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

}
