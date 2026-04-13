package com.capics.dto;

import java.math.BigDecimal;

public class ManpowerPlanDto {
    private Long id;
    private String lineClass;
    private String belongTo;
    private BigDecimal manpowerFactor;
    private String planDate;
    private String remark;
    private String updatedBy;
    private String updatedAt;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLineClass() {
        return this.lineClass;
    }

    public void setLineClass(String lineClass) {
        this.lineClass = lineClass;
    }

    public String getBelongTo() {
        return this.belongTo;
    }

    public void setBelongTo(String belongTo) {
        this.belongTo = belongTo;
    }

    public BigDecimal getManpowerFactor() {
        return this.manpowerFactor;
    }

    public void setManpowerFactor(BigDecimal manpowerFactor) {
        this.manpowerFactor = manpowerFactor;
    }

    public String getPlanDate() {
        return this.planDate;
    }

    public void setPlanDate(String planDate) {
        this.planDate = planDate;
    }

    public String getRemark() {
        return this.remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
