package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;

import java.util.Date;
import java.util.Objects;

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

    /** 正式生产门禁时间，包含胎胚最早可供时间。 */
    private final Date productionNotBeforeTime;

    /**
     * 候选预演生产门禁时间。
     *
     * <p>仅保留试制/量试等 SKU 类型门禁，不包含胎胚最早可供时间；正规、小批量
     * SKU 在候选预演阶段通常为空。</p>
     */
    private final Date candidateProductionNotBeforeTime;

    /**
     * 候选预演可开产时间。
     *
     * <p>该时间只描述不叠加正式胎胚门禁的候选预演结果，供准备分析和兼容旧日志读取。
     * 正式候选筛选使用 {@link #formalAvailableProductionTime}。</p>
     */
    private final Date candidateAvailableProductionTime;

    /** 候选预演可开产时间严格按[start,end)命中的班次。 */
    private final LhShiftConfigVO targetShift;

    /**
     * 正式生产时间轴计算得到的首个可开产时间。
     *
     * <p>该时间在候选预演时间基础上重新应用正式生产门禁，并保留正式设备计划、首检
     * 和班次产能约束。正式候选班次筛选只能使用该时间对应的班次。</p>
     */
    private final Date formalAvailableProductionTime;

    /** 正式生产时间轴命中的班次。 */
    private final LhShiftConfigVO formalTargetShift;

    /**
     * 不使用换模均衡配额时，经过停机、维修、清洗、禁换模和首检资源校验后的准备完成时间。
     * <p>该时间只服务候选机台的准备班次筛选，不代表正式生产时间，也不触发计划量或账本扣减。</p>
     */
    private final Date preparationAvailableTime;

    /** 准备完成时间对应的班次。 */
    private final LhShiftConfigVO preparationTargetShift;

    /** 准备时间轴是否通过完整准备约束。 */
    private final boolean preparationAvailable;

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
            Date candidateProductionNotBeforeTime,
            Date candidateAvailableProductionTime,
            LhShiftConfigVO targetShift,
            FirstInspectionAllocationPlan firstInspectionPlan,
            Date traceChangeoverEndTime) {
        this(machine, available, unavailableReason, occupationEndTime, machineReadyTime,
                changeoverStartTime, changeoverEndTime, productionNotBeforeTime,
                candidateProductionNotBeforeTime, candidateAvailableProductionTime, targetShift,
                firstInspectionPlan, traceChangeoverEndTime, candidateAvailableProductionTime,
                targetShift, available && Objects.nonNull(candidateAvailableProductionTime)
                        && Objects.nonNull(targetShift),
                available ? candidateAvailableProductionTime : null,
                available ? targetShift : null);
    }

    /**
     * 创建同时携带准备时间轴和正式生产时间轴的机台计划。
     *
     * @param machine 候选机台
     * @param available 正式候选是否可用
     * @param unavailableReason 正式候选不可用原因
     * @param occupationEndTime 前序占用结束时间
     * @param machineReadyTime 机台准备就绪时间
     * @param changeoverStartTime 正式候选换模开始时间
     * @param changeoverEndTime 正式候选换模结束时间
     * @param productionNotBeforeTime 正式生产门禁
     * @param candidateProductionNotBeforeTime 候选预演门禁
     * @param candidateAvailableProductionTime 候选预演正式生产时间
     * @param targetShift 候选预演正式生产班次
     * @param firstInspectionPlan 正式候选首检计划
     * @param traceChangeoverEndTime 粗略展示换模完成时间
     * @param preparationAvailableTime 准备完成时间
     * @param preparationTargetShift 准备完成班次
     * @param preparationAvailable 准备时间轴是否可用
     */
    public NewSpecMachineAvailabilityPlan(
            MachineScheduleDTO machine,
            boolean available,
            String unavailableReason,
            Date occupationEndTime,
            Date machineReadyTime,
            Date changeoverStartTime,
            Date changeoverEndTime,
            Date productionNotBeforeTime,
            Date candidateProductionNotBeforeTime,
            Date candidateAvailableProductionTime,
            LhShiftConfigVO targetShift,
            FirstInspectionAllocationPlan firstInspectionPlan,
            Date traceChangeoverEndTime,
            Date preparationAvailableTime,
            LhShiftConfigVO preparationTargetShift,
            boolean preparationAvailable) {
        this(machine, available, unavailableReason, occupationEndTime, machineReadyTime,
                changeoverStartTime, changeoverEndTime, productionNotBeforeTime,
                candidateProductionNotBeforeTime, candidateAvailableProductionTime, targetShift,
                firstInspectionPlan, traceChangeoverEndTime, preparationAvailableTime,
                preparationTargetShift, preparationAvailable,
                available ? candidateAvailableProductionTime : null,
                available ? targetShift : null);
    }

    /**
     * 创建同时携带准备时间轴、候选预演时间轴和正式生产时间轴的机台计划。
     *
     * @param machine 候选机台
     * @param available 正式候选是否可用
     * @param unavailableReason 正式候选不可用原因
     * @param occupationEndTime 前序占用结束时间
     * @param machineReadyTime 机台准备就绪时间
     * @param changeoverStartTime 正式候选换模开始时间
     * @param changeoverEndTime 正式候选换模结束时间
     * @param productionNotBeforeTime 正式生产门禁
     * @param candidateProductionNotBeforeTime 候选预演门禁
     * @param candidateAvailableProductionTime 候选预演时间
     * @param targetShift 候选预演班次
     * @param firstInspectionPlan 正式候选首检计划
     * @param traceChangeoverEndTime 粗略展示换模完成时间
     * @param preparationAvailableTime 准备完成时间
     * @param preparationTargetShift 准备完成班次
     * @param preparationAvailable 准备时间轴是否可用
     * @param formalAvailableProductionTime 正式可开产时间
     * @param formalTargetShift 正式可开产班次
     */
    public NewSpecMachineAvailabilityPlan(
            MachineScheduleDTO machine,
            boolean available,
            String unavailableReason,
            Date occupationEndTime,
            Date machineReadyTime,
            Date changeoverStartTime,
            Date changeoverEndTime,
            Date productionNotBeforeTime,
            Date candidateProductionNotBeforeTime,
            Date candidateAvailableProductionTime,
            LhShiftConfigVO targetShift,
            FirstInspectionAllocationPlan firstInspectionPlan,
            Date traceChangeoverEndTime,
            Date preparationAvailableTime,
            LhShiftConfigVO preparationTargetShift,
            boolean preparationAvailable,
            Date formalAvailableProductionTime,
            LhShiftConfigVO formalTargetShift) {
        this.machine = machine;
        this.available = available;
        this.unavailableReason = unavailableReason;
        this.occupationEndTime = occupationEndTime;
        this.machineReadyTime = machineReadyTime;
        this.changeoverStartTime = changeoverStartTime;
        this.changeoverEndTime = changeoverEndTime;
        this.traceChangeoverEndTime = traceChangeoverEndTime;
        this.productionNotBeforeTime = productionNotBeforeTime;
        this.candidateProductionNotBeforeTime = candidateProductionNotBeforeTime;
        this.candidateAvailableProductionTime = candidateAvailableProductionTime;
        this.targetShift = targetShift;
        this.formalAvailableProductionTime = formalAvailableProductionTime;
        this.formalTargetShift = formalTargetShift;
        this.firstInspectionPlan = firstInspectionPlan;
        this.preparationAvailableTime = preparationAvailableTime;
        this.preparationTargetShift = preparationTargetShift;
        this.preparationAvailable = preparationAvailable;
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

    /**
     * 获取候选预演生产门禁时间。
     *
     * @return 不包含胎胚最早可供时间的候选预演门禁
     */
    public Date getCandidateProductionNotBeforeTime() {
        return candidateProductionNotBeforeTime;
    }

    /**
     * 获取候选预演可开产时间。
     *
     * @return 不包含胎胚最早可供时间的候选预演时间
     */
    public Date getCandidateAvailableProductionTime() {
        return candidateAvailableProductionTime;
    }

    /**
     * 兼容已有调用方读取候选预演时间。
     *
     * @return 候选预演时间
     */
    public Date getMachineAvailableProductionTime() {
        return this.getCandidateAvailableProductionTime();
    }

    public LhShiftConfigVO getTargetShift() {
        return targetShift;
    }

    /**
     * 获取正式生产时间轴的首个可开产时间。
     *
     * @return 正式可开产时间；正式时间轴不可用时返回 null
     */
    public Date getFormalAvailableProductionTime() {
        return formalAvailableProductionTime;
    }

    /**
     * 获取正式生产时间轴命中的班次。
     *
     * @return 正式可开产班次；正式时间轴不可用时返回 null
     */
    public LhShiftConfigVO getFormalTargetShift() {
        return formalTargetShift;
    }

    /**
     * 获取与候选时间轴同源的首检分摊计划。
     *
     * @return 首检分摊计划
     */
    public FirstInspectionAllocationPlan getFirstInspectionPlan() {
        return firstInspectionPlan;
    }

    /**
     * 获取准备时间轴完成时间。
     *
     * @return 准备完成时间；不可用时返回 null
     */
    public Date getPreparationAvailableTime() {
        return preparationAvailableTime;
    }

    /**
     * 获取准备时间轴完成班次。
     *
     * @return 准备完成班次；不可用时返回 null
     */
    public LhShiftConfigVO getPreparationTargetShift() {
        return preparationTargetShift;
    }

    /**
     * 判断准备时间轴是否通过校验。
     *
     * @return true-准备可用；false-准备不可用
     */
    public boolean isPreparationAvailable() {
        return preparationAvailable;
    }
}
