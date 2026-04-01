package com.capics.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BomExpandDto {
    private String itemNumber;
    private List<BomComponent> components;

    @Data
    public static class BomComponent {
        private String componentNumber;
        private String lineCode;
        private BigDecimal quantity;
        private BigDecimal ct;
        private BigDecimal oee;
        private Integer workers;
    }
}
