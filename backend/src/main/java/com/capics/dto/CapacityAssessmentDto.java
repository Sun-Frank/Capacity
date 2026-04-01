package com.capics.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
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

    @Data
    public static class WeeklyLoad {
        private BigDecimal demand;
        private BigDecimal load;

        public WeeklyLoad() {}

        public WeeklyLoad(BigDecimal demand, BigDecimal load) {
            this.demand = demand;
            this.load = load;
        }
    }
}
