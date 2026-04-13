package com.capics.dto;

public class MeetingMinutesDto {
    private Long id;
    private String mpsVersion;
    private Integer itemNo;
    private String minutes;
    private String remark;
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
