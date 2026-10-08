package com.zlt.aps.lh.api.domain.dto;

import lombok.Data;

import java.util.Date;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * 机台精度保养时间窗口。
 *
 * @author APS
 */
@Data
public class MachineMaintenanceWindowDTO {

    /** 精度保养计划主键，用于排程完成后精确回填计划安排日期 */
    private Long precisionPlanId;
    /** 机台编号 */
    private String machineCode;
    /** 精度/保养类型 */
    private String maintenanceType;
    /** 来源计划日期，由 MES 或设备计划维护 */
    private Date sourcePlanDate;
    /** 计划到期日期；为空时以来源计划日期作为到期日期 */
    private Date dueDate;
    /** 数据源维护的到期天数，精度计划触发和排序只使用该字段 */
    private Integer daysToDue;
    /** APS 最终安排的保养日期 */
    private Date planDate;
    /** 保养开始时间 */
    private Date maintenanceStartTime;
    /** 保养结束时间 */
    private Date maintenanceEndTime;
    /** 是否由设备计划公共编排发布；此类窗口不得再套用旧精度并行、预热和06:00规则。 */
    private boolean equipmentPlanManaged;
    /** 公共首检开始时间，与原始保养结束时间分别保存。 */
    private Date firstInspectionStartTime;
    /** 公共首检结束时间。 */
    private Date firstInspectionEndTime;
    /** 本次维护完整占用结束后的恢复生产边界。 */
    private Date productionResumeTime;
    /** 仅05容量窗口使用：每个来源维修日期的早班起止，跨日合并维修仍保留各自固定量班别。 */
    private Map<Date, Date> repairFixedQtyShiftWindowMap = new LinkedHashMap<>(3);
    /** 当前生产截止时间；公共编排使用判定返回的维护开始，旧窗口保持原契约。 */
    private Date productionCutoffTime;
    /** 是否允许在前SKU自然收尾后、生产截止时间前插排完整小余量SKU */
    private boolean preInsertAllowed;
    /** 是否已接受精度前插排SKU，用于禁止同一物理机台重复填充精度前窗口 */
    private boolean preInsertScheduled;
    /** 是否因到期天数不超过强制阈值而需要执行强制下机 */
    private boolean forceDown;
    /** 触发原因 */
    private String triggerReason;

    /**
     * 复制可变窗口及其日期、维修班次映射，供预演和恢复各自持有独立副本。
     * @return 完整窗口副本
     */
    public MachineMaintenanceWindowDTO copy() {
        MachineMaintenanceWindowDTO target = new MachineMaintenanceWindowDTO();
        target.setPrecisionPlanId(precisionPlanId);
        target.setMachineCode(machineCode);
        target.setMaintenanceType(maintenanceType);
        target.setSourcePlanDate(this.copyDate(sourcePlanDate));
        target.setDueDate(this.copyDate(dueDate));
        target.setDaysToDue(daysToDue);
        target.setPlanDate(this.copyDate(planDate));
        target.setMaintenanceStartTime(this.copyDate(maintenanceStartTime));
        target.setMaintenanceEndTime(this.copyDate(maintenanceEndTime));
        target.setEquipmentPlanManaged(equipmentPlanManaged);
        target.setFirstInspectionStartTime(this.copyDate(firstInspectionStartTime));
        target.setFirstInspectionEndTime(this.copyDate(firstInspectionEndTime));
        target.setProductionResumeTime(this.copyDate(productionResumeTime));
        target.setProductionCutoffTime(this.copyDate(productionCutoffTime));
        target.setPreInsertAllowed(preInsertAllowed);
        target.setPreInsertScheduled(preInsertScheduled);
        target.setForceDown(forceDown);
        target.setTriggerReason(triggerReason);
        if (repairFixedQtyShiftWindowMap != null) {
            repairFixedQtyShiftWindowMap.forEach((start, end) ->
                    target.getRepairFixedQtyShiftWindowMap().put(this.copyDate(start), this.copyDate(end)));
        }
        return target;
    }

    /** @param value 来源日期 @return 独立日期副本 */
    private Date copyDate(Date value) {
        return value == null ? null : new Date(value.getTime());
    }
}
