package com.zlt.aps.cx.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 车次记录（子表构建用）。
 *
 * @author APS Team
 */
public class TripRecord {
    private String embryoCode;
    private String materialCode;
    private String machineCode;
    private int day;
    private String shiftCode;
    private String classField;
    private int tripNo;
    private int tripCapacity;
    private int planQty;
    private BigDecimal stockHours;
    private LocalDateTime planStartTime;
    private LocalDateTime planEndTime;
    private boolean isTrialTask;
    private boolean isEndingTask;
    private int vulcanizeMachineCount;
    private int sequence;

    // getters and setters
    public String getEmbryoCode() { return embryoCode; }
    public void setEmbryoCode(String embryoCode) { this.embryoCode = embryoCode; }
    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
    public String getMachineCode() { return machineCode; }
    public void setMachineCode(String machineCode) { this.machineCode = machineCode; }
    public int getDay() { return day; }
    public void setDay(int day) { this.day = day; }
    public String getShiftCode() { return shiftCode; }
    public void setShiftCode(String shiftCode) { this.shiftCode = shiftCode; }
    public String getClassField() { return classField; }
    public void setClassField(String classField) { this.classField = classField; }
    public int getTripNo() { return tripNo; }
    public void setTripNo(int tripNo) { this.tripNo = tripNo; }
    public int getTripCapacity() { return tripCapacity; }
    public void setTripCapacity(int tripCapacity) { this.tripCapacity = tripCapacity; }
    public int getPlanQty() { return planQty; }
    public void setPlanQty(int planQty) { this.planQty = planQty; }
    public BigDecimal getStockHours() { return stockHours; }
    public void setStockHours(BigDecimal stockHours) { this.stockHours = stockHours; }
    public LocalDateTime getPlanStartTime() { return planStartTime; }
    public void setPlanStartTime(LocalDateTime planStartTime) { this.planStartTime = planStartTime; }
    public LocalDateTime getPlanEndTime() { return planEndTime; }
    public void setPlanEndTime(LocalDateTime planEndTime) { this.planEndTime = planEndTime; }
    public boolean getIsTrialTask() { return isTrialTask; }
    public void setIsTrialTask(boolean isTrialTask) { this.isTrialTask = isTrialTask; }
    public boolean getIsEndingTask() { return isEndingTask; }
    public void setIsEndingTask(boolean isEndingTask) { this.isEndingTask = isEndingTask; }
    public int getVulcanizeMachineCount() { return vulcanizeMachineCount; }
    public void setVulcanizeMachineCount(int vulcanizeMachineCount) { this.vulcanizeMachineCount = vulcanizeMachineCount; }
    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }
}
