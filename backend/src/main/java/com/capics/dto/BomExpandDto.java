package com.capics.dto;

import java.math.BigDecimal;
import java.util.List;

public class BomExpandDto {
    private String itemNumber;
    private List<BomComponent> components;

    public static class BomComponent {
        private String componentNumber;
        private String lineCode;
        private BigDecimal quantity;
        private BigDecimal ct;
        private BigDecimal oee;
        private Integer workers;

        public String getComponentNumber() {
            return this.componentNumber;
        }

        public void setComponentNumber(String componentNumber) {
            this.componentNumber = componentNumber;
        }

        public String getLineCode() {
            return this.lineCode;
        }

        public void setLineCode(String lineCode) {
            this.lineCode = lineCode;
        }

        public BigDecimal getQuantity() {
            return this.quantity;
        }

        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
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

        public Integer getWorkers() {
            return this.workers;
        }

        public void setWorkers(Integer workers) {
            this.workers = workers;
        }
    }

    public String getItemNumber() {
        return this.itemNumber;
    }

    public void setItemNumber(String itemNumber) {
        this.itemNumber = itemNumber;
    }

    public List<BomComponent> getComponents() {
        return this.components;
    }

    public void setComponents(List<BomComponent> components) {
        this.components = components;
    }

}
