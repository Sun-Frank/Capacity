package com.capics.dto;

public class WmsBomConfigDto {
    private String loginUrl;
    private String username;
    private String password;
    private Boolean passwordConfigured;
    private String updatedBy;
    private String updatedAt;

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getPasswordConfigured() {
        return passwordConfigured;
    }

    public void setPasswordConfigured(Boolean passwordConfigured) {
        this.passwordConfigured = passwordConfigured;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
