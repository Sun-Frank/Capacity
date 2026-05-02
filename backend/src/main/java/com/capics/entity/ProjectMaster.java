package com.capics.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_master")
public class ProjectMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String customer;

    @Column(name = "product_platform", length = 100)
    private String productPlatform;

    @Column(name = "vehicle_config", length = 100)
    private String vehicleConfig;

    @Column(name = "product_description", length = 255, nullable = false)
    private String productDescription;

    @Column(length = 100)
    private String bws;

    @Column(length = 50)
    private String version;

    @Column(length = 50)
    private String createdBy;

    private LocalDateTime createdAt;

    @Column(length = 50)
    private String updatedBy;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
