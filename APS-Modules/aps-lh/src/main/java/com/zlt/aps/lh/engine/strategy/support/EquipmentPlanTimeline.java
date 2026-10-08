package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.MachineMaintenanceWindowDTO;
import com.zlt.aps.lh.api.domain.dto.PrecisionMaintenanceJudgeDTO;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;

import java.util.Date;
import java.util.Objects;

/**
 * 设备计划的只读时间约束。仅保存判定身份和时间值，不复制SKU选机、排量及扣账模型。
 * 日期内部以毫秒保存，适配与快照均不共享可变Date。
 */
public final class EquipmentPlanTimeline {

    /** 有效判定返回的唯一计划主键。 */
    private final Long precisionPlanId;
    /** 判定返回的运行态机台，不为配对侧重新查计划。 */
    private final String sourceMachineCode;
    /** L/R共用的物理机台。 */
    private final String physicalMachineCode;
    /** 原始判定开始、结束；均为绝对时间。 */
    private final long maintenanceStartMillis;
    private final long maintenanceEndMillis;
    /** 单场景保养后上胶囊及预热完成边界；组合、撤销和快照均保留该冻结值。 */
    private final long maintenanceReadyMillis;
    /** 公共首检开始；并行切换较晚完成时可以晚于原始保养结束。 */
    private final long inspectionStartMillis;
    private final long inspectionEndMillis;
    /** 判定原始强制下机结论。 */
    private final boolean forceDown;
    /** 生成本时间计划的稳定输入指纹。 */
    private final String inputVersion;

    /**
     * SKU尚未确定时只冻结保养及预热边界，不预占固定首检时长。
     * @param judge 原始精度判定
     * @param maintenanceReadyTime 上胶囊及预热完成时间
     * @param inputVersion 输入指纹
     */
    public EquipmentPlanTimeline(PrecisionMaintenanceJudgeDTO judge,
                                 Date maintenanceReadyTime, String inputVersion) {
        if (Objects.isNull(judge) || judge.getDurationHours() <= 0
                || Objects.isNull(judge.getPrecisionPlanId())
                || Objects.isNull(judge.getPrecisionStartTime()) || Objects.isNull(judge.getPrecisionEndTime())
                || !judge.getPrecisionStartTime().before(judge.getPrecisionEndTime())
                || Objects.isNull(maintenanceReadyTime) || maintenanceReadyTime.before(judge.getPrecisionEndTime())) {
            throw new IllegalArgumentException("设备计划缺少有效判定或上胶囊预热完成边界");
        }
        this.precisionPlanId = judge.getPrecisionPlanId();
        this.sourceMachineCode = judge.getMachineCode();
        this.physicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(sourceMachineCode);
        this.maintenanceStartMillis = judge.getPrecisionStartTime().getTime();
        this.maintenanceEndMillis = judge.getPrecisionEndTime().getTime();
        this.maintenanceReadyMillis = maintenanceReadyTime.getTime();
        this.inspectionStartMillis = maintenanceReadyMillis;
        this.inspectionEndMillis = maintenanceReadyMillis;
        this.forceDown = judge.isForceDown();
        this.inputVersion = inputVersion;
    }

    /** 原始判定身份及保养区间保持不变，只替换已解析的公共首检区间。 */
    private EquipmentPlanTimeline(EquipmentPlanTimeline source, Date inspectionStart, Date inspectionEnd) {
        if (Objects.isNull(inspectionStart) || Objects.isNull(inspectionEnd)
                || inspectionStart.before(source.getMaintenanceReadyTime()) || inspectionStart.after(inspectionEnd)) {
            throw new IllegalArgumentException("公共首检必须在上胶囊及预热完成后执行且区间有效");
        }
        this.precisionPlanId = source.precisionPlanId;
        this.sourceMachineCode = source.sourceMachineCode;
        this.physicalMachineCode = source.physicalMachineCode;
        this.maintenanceStartMillis = source.maintenanceStartMillis;
        this.maintenanceEndMillis = source.maintenanceEndMillis;
        this.maintenanceReadyMillis = source.maintenanceReadyMillis;
        this.inspectionStartMillis = inspectionStart.getTime();
        this.inspectionEndMillis = inspectionEnd.getTime();
        this.forceDown = source.forceDown;
        this.inputVersion = source.inputVersion;
    }

    /** @param inspectionStart 公共首检开始 @param inspectionEnd 公共首检结束 @return 独立只读副本 */
    public EquipmentPlanTimeline withInspection(Date inspectionStart, Date inspectionEnd) {
        return new EquipmentPlanTimeline(this, inspectionStart, inspectionEnd);
    }

    /**
     * 为单侧运行态生成独立实际窗口；容量适配器仍负责生成临时容量窗口。
     * @param machineCode 被约束的运行态机台
     * @return 不共享可变日期的实际维护窗口
     */
    public MachineMaintenanceWindowDTO toMaintenanceWindow(String machineCode) {
        MachineMaintenanceWindowDTO window = new MachineMaintenanceWindowDTO();
        window.setPrecisionPlanId(precisionPlanId);
        window.setMachineCode(machineCode);
        window.setEquipmentPlanManaged(true);
        window.setMaintenanceType("精度计划");
        window.setPlanDate(LhScheduleTimeUtil.clearTime(this.getMaintenanceStartTime()));
        window.setMaintenanceStartTime(this.getMaintenanceStartTime());
        window.setMaintenanceEndTime(this.getMaintenanceEndTime());
        window.setMaintenanceReadyTime(this.getMaintenanceReadyTime());
        window.setFirstInspectionStartTime(this.getInspectionStartTime());
        window.setFirstInspectionEndTime(this.getProductionResumeTime());
        window.setProductionResumeTime(this.getProductionResumeTime());
        window.setProductionCutoffTime(this.getMaintenanceStartTime());
        window.setForceDown(forceDown);
        window.setTriggerReason(forceDown ? "精度计划到期强制下机" : "确认收尾后精度保养");
        return window;
    }

    /** @return 有效判定返回的计划主键 */
    public Long getPrecisionPlanId() {
        return precisionPlanId;
    }
    /** @return 有效判定来源运行侧 */
    public String getSourceMachineCode() {
        return sourceMachineCode;
    }
    /** @return 物理机台编码 */
    public String getPhysicalMachineCode() {
        return physicalMachineCode;
    }
    /** @return 判定输入指纹 */
    public String getInputVersion() {
        return inputVersion;
    }
    /** @return 是否强制下机 */
    public boolean isForceDown() {
        return forceDown;
    }
    /** @return 原始维护开始的独立日期 */
    public Date getMaintenanceStartTime() {
        return new Date(maintenanceStartMillis);
    }
    /** @return 原始维护结束的独立日期 */
    public Date getMaintenanceEndTime() {
        return new Date(maintenanceEndMillis);
    }
    /** @return 精度保养后上胶囊及预热完成的独立日期 */
    public Date getMaintenanceReadyTime() {
        return new Date(maintenanceReadyMillis);
    }
    /** @return 公共首检开始的独立日期 */
    public Date getInspectionStartTime() {
        return new Date(inspectionStartMillis);
    }
    /** @return 已分配首检的结束；尚未关联SKU时仅表示准备完成边界 */
    public Date getProductionResumeTime() {
        return new Date(inspectionEndMillis);
    }
}
