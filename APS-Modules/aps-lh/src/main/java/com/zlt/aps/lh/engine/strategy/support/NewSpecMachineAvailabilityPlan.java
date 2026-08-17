package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;

import java.util.Date;

/**
 * 新增 SKU 候选机台的无副作用真实可开产计划。
 *
 * <p>对象生命周期仅限“当前 SKU × 当前选机回合”。候选排序、逐班筛选和过程日志读取
 * 同一对象；正式资源提交后立即释放，避免保存全量 SKU×机台矩阵导致堆内存放大。</p>
 *
 * @author APS
 */
public class NewSpecMachineAvailabilityPlan {

    /** 候选机台。 */
    private final MachineScheduleDTO machine;

    /** 是否通过完整时间轴预演。 */
    private final boolean available;

    /** 不可用原因。 */
    private final String unavailableReason;

    /** 前 SKU 或现有占用真实收尾时间。 */
    private final Date occupationEndTime;

    /** 精度、维修等处理后的机台准备就绪时间。 */
    private final Date machineReadyTime;

    /** 实际可开始换模或换活字块的时间。 */
    private final Date changeoverStartTime;

    /** 换模或换活字块结束时间。 */
    private final Date changeoverEndTime;

    /** 选机日志展示用的换模或换活字块完成时间（从机台收尾时间出发，只避让停机与20:00-06:00禁换模约束，豁免换模均衡配额）。 */
    private final Date traceChangeoverEndTime;

    /** 正式生产门禁时间。 */
    private final Date productionNotBeforeTime;

    /** 综合全部约束后的真实可开产时间。 */
    private final Date machineAvailableProductionTime;

    /** 真实可开产时间严格按[start,end)命中的目标班次。 */
    private final LhShiftConfigVO targetShift;

    /** 与候选时间轴同源的首检分摊计划。 */
    private final FirstInspectionAllocationPlan firstInspectionPlan;

    public NewSpecMachineAvailabilityPlan(
            MachineScheduleDTO machine,
            boolean available,
            String unavailableReason,
            Date occupationEndTime,
            Date machineReadyTime,
            Date changeoverStartTime,
            Date changeoverEndTime,
            Date productionNotBeforeTime,
            Date machineAvailableProductionTime,
            LhShiftConfigVO targetShift,
            FirstInspectionAllocationPlan firstInspectionPlan,
            Date traceChangeoverEndTime) {
        this.machine = machine;
        this.available = available;
        this.unavailableReason = unavailableReason;
        this.occupationEndTime = occupationEndTime;
        this.machineReadyTime = machineReadyTime;
        this.changeoverStartTime = changeoverStartTime;
        this.changeoverEndTime = changeoverEndTime;
        this.traceChangeoverEndTime = traceChangeoverEndTime;
        this.productionNotBeforeTime = productionNotBeforeTime;
        this.machineAvailableProductionTime = machineAvailableProductionTime;
        this.targetShift = targetShift;
        this.firstInspectionPlan = firstInspectionPlan;
    }

    public MachineScheduleDTO getMachine() {
        return machine;
    }

    public boolean isAvailable() {
        return available;
    }

    public String getUnavailableReason() {
        return unavailableReason;
    }

    public Date getOccupationEndTime() {
        return occupationEndTime;
    }

    public Date getMachineReadyTime() {
        return machineReadyTime;
    }

    public Date getChangeoverStartTime() {
        return changeoverStartTime;
    }

    public Date getChangeoverEndTime() {
        return changeoverEndTime;
    }

    public Date getTraceChangeoverEndTime() {
        return traceChangeoverEndTime;
    }

    public Date getProductionNotBeforeTime() {
        return productionNotBeforeTime;
    }

    public Date getMachineAvailableProductionTime() {
        return machineAvailableProductionTime;
    }

    public LhShiftConfigVO getTargetShift() {
        return targetShift;
    }

    public FirstInspectionAllocationPlan getFirstInspectionPlan() {
        return firstInspectionPlan;
    }
}
