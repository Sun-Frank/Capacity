package com.capics.dto;

public class ProjectMasterDto {
    private Long id;
    private String customer;
    private String productPlatform;
    private String vehicleConfig;
    private String productDescription;
    private String bws;
    private String version;
    private String createdBy;
    private String createdAt;
    private String updatedBy;
    private String updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomer() { return customer; }
    public void setCustomer(String customer) { this.customer = customer; }
    public String getProductPlatform() { return productPlatform; }
    public void setProductPlatform(String productPlatform) { this.productPlatform = productPlatform; }
    public String getVehicleConfig() { return vehicleConfig; }
    public void setVehicleConfig(String vehicleConfig) { this.vehicleConfig = vehicleConfig; }
    public String getProductDescription() { return productDescription; }
    public void setProductDescription(String productDescription) { this.productDescription = productDescription; }
    public String getBws() { return bws; }
    public void setBws(String bws) { this.bws = bws; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
