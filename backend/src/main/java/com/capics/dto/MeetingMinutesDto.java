package com.capics.dto;

public class MeetingMinutesDto {
    private Long id;
    private String mpsVersion;
    private Integer itemNo;
    private String minutes;
    private String remark;
    private String productNumber;
    private String productDescription;
    private String lineCode;
    private String adjustmentField;
    private String beforeValue;
    private String afterValue;
    private String ownerName;
    private String status;
    private String updatedBy;
    private String updatedAt;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMpsVersion() {
        return this.mpsVersion;
    }

    public void setMpsVersion(String mpsVersion) {
        this.mpsVersion = mpsVersion;
    }

    public Integer getItemNo() {
        return this.itemNo;
    }

    public void setItemNo(Integer itemNo) {
        this.itemNo = itemNo;
    }

    public String getMinutes() {
        return this.minutes;
    }

    public void setMinutes(String minutes) {
        this.minutes = minutes;
    }

    public String getRemark() {
        return this.remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getProductNumber() {
        return this.productNumber;
    }

    public void setProductNumber(String productNumber) {
        this.productNumber = productNumber;
    }

    public String getProductDescription() {
        return this.productDescription;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    public String getLineCode() {
        return this.lineCode;
    }

    public void setLineCode(String lineCode) {
        this.lineCode = lineCode;
    }

    public String getAdjustmentField() {
        return this.adjustmentField;
    }

    public void setAdjustmentField(String adjustmentField) {
        this.adjustmentField = adjustmentField;
    }

    public String getBeforeValue() {
        return this.beforeValue;
    }

    public void setBeforeValue(String beforeValue) {
        this.beforeValue = beforeValue;
    }

    public String getAfterValue() {
        return this.afterValue;
    }

    public void setAfterValue(String afterValue) {
        this.afterValue = afterValue;
    }

    public String getOwnerName() {
        return this.ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
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
