package com.capics.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "line_realtime")
public class LineRealtime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    private String lineCode;

    @Column(length = 50, nullable = false)
    private String itemNumber;

    @Column(length = 50, nullable = false)
    private String componentNumber;

    @Column(length = 255)
    private String description;

    @Column(precision = 12, scale = 2)
    private BigDecimal shiftOutput;

    @Column
    private Integer shiftWorkers;

    @Column(precision = 10, scale = 2)
    private BigDecimal ct;

    @Column(precision = 7, scale = 4)
    private BigDecimal oee;

    @Column(columnDefinition = "text")
    private String weeklyDemand;

    @Column(length = 10)
    private String mrpVersion;

    @Column
    private LocalDateTime calculatedAt;

    @PrePersist
    protected void onCreate() {
        calculatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLineCode() {
        return this.lineCode;
    }

    public void setLineCode(String lineCode) {
        this.lineCode = lineCode;
    }

    public String getItemNumber() {
        return this.itemNumber;
    }

    public void setItemNumber(String itemNumber) {
        this.itemNumber = itemNumber;
    }

    public String getComponentNumber() {
        return this.componentNumber;
    }

    public void setComponentNumber(String componentNumber) {
        this.componentNumber = componentNumber;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getShiftOutput() {
        return this.shiftOutput;
    }

    public void setShiftOutput(BigDecimal shiftOutput) {
        this.shiftOutput = shiftOutput;
    }

    public Integer getShiftWorkers() {
        return this.shiftWorkers;
    }

    public void setShiftWorkers(Integer shiftWorkers) {
        this.shiftWorkers = shiftWorkers;
    }

    public BigDecimal getCt() {
        return this.ct;
    }

    public void setCt(BigDecimal ct) {
        this.ct = ct;
    }

    public BigDecimal getOee() {
        return this.oee;
    }

    public void setOee(BigDecimal oee) {
        this.oee = oee;
    }

    public String getWeeklyDemand() {
        return this.weeklyDemand;
    }

    public void setWeeklyDemand(String weeklyDemand) {
        this.weeklyDemand = weeklyDemand;
    }

    public String getMrpVersion() {
        return this.mrpVersion;
    }

    public void setMrpVersion(String mrpVersion) {
        this.mrpVersion = mrpVersion;
    }

    public LocalDateTime getCalculatedAt() {
        return this.calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
