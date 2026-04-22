package com.capics.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ct_line_data")
public class CtLineData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "col_b", nullable = false, length = 255)
    private String colB;

    @Column(name = "col_c", nullable = false, length = 255)
    private String colC;

    @Column(name = "col_d", nullable = false, length = 255)
    private String colD;

    @Column(name = "col_f", nullable = false, length = 255)
    private String colF;

    @Column(name = "col_i", nullable = false, length = 255)
    private String colI;

    @Column(name = "col_p", nullable = false, length = 255)
    private String colP;

    @Column(name = "col_w", length = 255)
    private String colW;

    @Column(name = "col_x", length = 255)
    private String colX;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "updated_at")
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getColB() {
        return colB;
    }

    public void setColB(String colB) {
        this.colB = colB;
    }

    public String getColC() {
        return colC;
    }

    public void setColC(String colC) {
        this.colC = colC;
    }

    public String getColD() {
        return colD;
    }

    public void setColD(String colD) {
        this.colD = colD;
    }

    public String getColF() {
        return colF;
    }

    public void setColF(String colF) {
        this.colF = colF;
    }

    public String getColI() {
        return colI;
    }

    public void setColI(String colI) {
        this.colI = colI;
    }

    public String getColP() {
        return colP;
    }

    public void setColP(String colP) {
        this.colP = colP;
    }

    public String getColW() {
        return colW;
    }

    public void setColW(String colW) {
        this.colW = colW;
    }

    public String getColX() {
        return colX;
    }

    public void setColX(String colX) {
        this.colX = colX;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

