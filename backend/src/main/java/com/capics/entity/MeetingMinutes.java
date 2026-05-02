package com.capics.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_minutes")
public class MeetingMinutes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    private String mpsVersion;

    @Column(nullable = false)
    private Integer itemNo;

    @Column(length = 2000)
    private String minutes;

    @Column(length = 255)
    private String remark;

    @Column(length = 50)
    private String productNumber;

    @Column(length = 255)
    private String productDescription;

    @Column(length = 50)
    private String lineCode;

    @Column(length = 100)
    private String adjustmentField;

    @Column(length = 255)
    private String beforeValue;

    @Column(length = 255)
    private String afterValue;

    @Column(length = 100)
    private String ownerName;

    @Column(length = 50)
    private String status;

    @Column(length = 50)
    private String updatedBy;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

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

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
