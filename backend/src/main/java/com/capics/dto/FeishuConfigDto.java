package com.capics.dto;

public class FeishuConfigDto {
    private String apiUrl;
    private String appId;
    private String appSecret;
    private Boolean appSecretConfigured;
    private String updatedBy;
    private String updatedAt;

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getAppSecret() { return appSecret; }
    public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
    public Boolean getAppSecretConfigured() { return appSecretConfigured; }
    public void setAppSecretConfigured(Boolean appSecretConfigured) { this.appSecretConfigured = appSecretConfigured; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
