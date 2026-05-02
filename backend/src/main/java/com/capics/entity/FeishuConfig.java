package com.capics.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feishu_config")
public class FeishuConfig {

    @Id
    private Integer id;

    @Column(name = "api_url", length = 255)
    private String apiUrl;

    @Column(name = "app_id", length = 120)
    private String appId;

    @Column(name = "app_secret", columnDefinition = "TEXT")
    private String appSecret;

    @Column(length = 50)
    private String updatedBy;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getAppSecret() { return appSecret; }
    public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
