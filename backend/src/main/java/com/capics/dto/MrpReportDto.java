package com.capics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class MrpReportDto {
    private String itemNumber;
    private String description;
    private Map<String, BigDecimal> weeklyDemand;
    private Map<String, BigDecimal> monthlyDemand;

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

    public Map<String, BigDecimal> getWeeklyDemand() {
        return this.weeklyDemand;
    }

    public void setWeeklyDemand(Map<String, BigDecimal> weeklyDemand) {
        this.weeklyDemand = weeklyDemand;
    }

    public Map<String, BigDecimal> getMonthlyDemand() {
        return this.monthlyDemand;
    }

    public void setMonthlyDemand(Map<String, BigDecimal> monthlyDemand) {
        this.monthlyDemand = monthlyDemand;
    }
}
