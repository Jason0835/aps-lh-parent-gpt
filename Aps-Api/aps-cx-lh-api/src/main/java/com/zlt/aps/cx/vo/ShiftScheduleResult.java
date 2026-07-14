package com.zlt.aps.cx.vo;

import com.zlt.aps.cx.entity.config.CxShiftConfig;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 班次排程结果。
 *
 * @author APS Team
 */
public class ShiftScheduleResult {
    /** 排产日（1-3），与 CxShiftConfig.scheduleDay 对应 */
    private int day;
    /** 排产日期 */
    private LocalDate scheduleDate;
    /** 该班次的班次配置 */
    private CxShiftConfig shiftConfig;
    /** 该班次所有机台的任务分配结果（包含续作/新任务/试制任务分配） */
    private List<MachineAllocationResult> allAllocations;
    /** 该班次的精排结果（包含班次级别的车数/数量） */
    private List<ShiftProductionResult> shiftProductionResults;
    /** 该班次排程前的 materialStockMap 快照（lhId -> 分配库存），用于子表 stockHours 计算 */
    private Map<String, Integer> materialStockSnapshot;

    public int getDay() { return day; }
    public void setDay(int day) { this.day = day; }
    public LocalDate getScheduleDate() { return scheduleDate; }
    public void setScheduleDate(LocalDate scheduleDate) { this.scheduleDate = scheduleDate; }
    public CxShiftConfig getShiftConfig() { return shiftConfig; }
    public void setShiftConfig(CxShiftConfig shiftConfig) { this.shiftConfig = shiftConfig; }
    public List<MachineAllocationResult> getAllAllocations() { return allAllocations; }
    public void setAllAllocations(List<MachineAllocationResult> allAllocations) { this.allAllocations = allAllocations; }
    public List<ShiftProductionResult> getShiftProductionResults() { return shiftProductionResults; }
    public void setShiftProductionResults(List<ShiftProductionResult> shiftProductionResults) { this.shiftProductionResults = shiftProductionResults; }
    public Map<String, Integer> getMaterialStockSnapshot() { return materialStockSnapshot; }
    public void setMaterialStockSnapshot(Map<String, Integer> materialStockSnapshot) { this.materialStockSnapshot = materialStockSnapshot; }
}
