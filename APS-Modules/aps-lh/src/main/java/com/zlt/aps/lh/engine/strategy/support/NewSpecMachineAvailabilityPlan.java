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

    /** 当前选机回合命中的历史班次剩余产能画像；未命中时为空。 */
    private final HistoricalResidualCapacityInfo historicalResidualCapacityInfo;

    /**
     * 首检是否因同班次总计划量上限顺延。
     *
     * <p>该标识只表示首检和正式生产起点后移，已合法分配的换模开始、完成时间保持不变。</p>
     */
    private final boolean firstInspectionDeferredByClassTotalLimit;

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
                available ? targetShift : null, null, false);
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
                available ? targetShift : null, null, false);
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
        this(machine, available, unavailableReason, occupationEndTime, machineReadyTime,
                changeoverStartTime, changeoverEndTime, productionNotBeforeTime,
                candidateProductionNotBeforeTime, candidateAvailableProductionTime, targetShift,
                firstInspectionPlan, traceChangeoverEndTime, preparationAvailableTime,
                preparationTargetShift, preparationAvailable, formalAvailableProductionTime,
                formalTargetShift, null, false);
    }

    /**
     * 创建带历史班次剩余产能画像的完整候选计划。
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
     * @param historicalResidualCapacityInfo 历史班次剩余产能画像
     * @param firstInspectionDeferredByClassTotalLimit 首检是否因同班次总计划量上限顺延
     */
    private NewSpecMachineAvailabilityPlan(
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
            LhShiftConfigVO formalTargetShift,
            HistoricalResidualCapacityInfo historicalResidualCapacityInfo,
            boolean firstInspectionDeferredByClassTotalLimit) {
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
        this.historicalResidualCapacityInfo = historicalResidualCapacityInfo;
        this.firstInspectionDeferredByClassTotalLimit = firstInspectionDeferredByClassTotalLimit;
    }

    /**
     * 在不修改原候选时间轴的前提下附加历史班次剩余产能画像。
     *
     * @param residualCapacityInfo 历史班次剩余产能画像
     * @return 携带历史剩余产能画像的新计划对象
     */
    public NewSpecMachineAvailabilityPlan withHistoricalResidualCapacityInfo(
            HistoricalResidualCapacityInfo residualCapacityInfo) {
        return new NewSpecMachineAvailabilityPlan(
                machine, available, unavailableReason, occupationEndTime, machineReadyTime,
                changeoverStartTime, changeoverEndTime, productionNotBeforeTime,
                candidateProductionNotBeforeTime, candidateAvailableProductionTime, targetShift,
                firstInspectionPlan, traceChangeoverEndTime, preparationAvailableTime,
                preparationTargetShift, preparationAvailable, formalAvailableProductionTime,
                formalTargetShift, residualCapacityInfo,
                firstInspectionDeferredByClassTotalLimit);
    }

    /**
     * 将当轮已经选中的候选计划切换为原续作机台原模具重新启用时间轴。
     *
     * <p>该方法只供正式选机完成后的日志和提交链复用，不参与前置候选筛选，因此不会
     * 提前锁定原续作机台。重新启用没有换模和首检，候选、准备及正式生产时间统一使用
     * 实际续作起点，避免选机日志继续展示已经被取消的虚假换模时间。</p>
     *
     * @param reuseStartTime 原续作机台实际重新启用时间
     * @param reuseShift 重新启用时间所属班次
     * @return 无换模、无首检的续作重新启用计划
     */
    public NewSpecMachineAvailabilityPlan withReleasedContinuationReuse(
            Date reuseStartTime,
            LhShiftConfigVO reuseShift) {
        return new NewSpecMachineAvailabilityPlan(
                machine, Objects.nonNull(reuseStartTime) && Objects.nonNull(reuseShift), null,
                occupationEndTime, machineReadyTime, null, null,
                productionNotBeforeTime, candidateProductionNotBeforeTime,
                reuseStartTime, reuseShift, null, null,
                reuseStartTime, reuseShift,
                Objects.nonNull(reuseStartTime) && Objects.nonNull(reuseShift),
                reuseStartTime, reuseShift, historicalResidualCapacityInfo, false);
    }

    /**
     * 将当轮已选机台计划切换为正式提交成功的跨日准备时间轴。
     *
     * <p>候选分组仍保留原换模均衡口径，只有机台最终选定且跨日准备提交成功后才调用
     * 本方法更新正式换模、准备完成和正式开产时间。选机日志中的粗略基础换模完成时间
     * 保留候选预演时的原值，避免命中机台与未命中机台出现不同展示口径。</p>
     *
     * @param committedChangeoverStartTime 正式换模开始时间
     * @param committedChangeoverEndTime 正式换模完成时间
     * @param committedProductionStartTime 正式开产时间
     * @param committedProductionShift 正式开产班次
     * @param committedInspectionPlan 正式首检计划
     * @return 正式提交后的跨日准备计划
     */
    public NewSpecMachineAvailabilityPlan withCommittedPreparationTimeline(
            Date committedChangeoverStartTime,
            Date committedChangeoverEndTime,
            Date committedProductionStartTime,
            LhShiftConfigVO committedProductionShift,
            FirstInspectionAllocationPlan committedInspectionPlan) {
        boolean committedAvailable = Objects.nonNull(committedChangeoverStartTime)
                && Objects.nonNull(committedChangeoverEndTime)
                && Objects.nonNull(committedProductionStartTime)
                && Objects.nonNull(committedProductionShift);
        return new NewSpecMachineAvailabilityPlan(
                machine, committedAvailable, committedAvailable ? null : unavailableReason,
                occupationEndTime, machineReadyTime,
                committedChangeoverStartTime, committedChangeoverEndTime,
                productionNotBeforeTime, candidateProductionNotBeforeTime,
                committedProductionStartTime, committedProductionShift,
                committedInspectionPlan, traceChangeoverEndTime,
                committedProductionStartTime, committedProductionShift,
                committedAvailable, committedProductionStartTime,
                committedProductionShift, historicalResidualCapacityInfo,
                firstInspectionDeferredByClassTotalLimit);
    }

    /**
     * 标记首检因同班次总计划量上限顺延。
     *
     * <p>仅补充时间轴决策标识，不修改已经计算完成的候选、换模、准备和正式生产时间。</p>
     *
     * @return 携带首检班次总量顺延标识的新计划对象
     */
    public NewSpecMachineAvailabilityPlan withFirstInspectionClassTotalDeferral() {
        return new NewSpecMachineAvailabilityPlan(
                machine, available, unavailableReason, occupationEndTime, machineReadyTime,
                changeoverStartTime, changeoverEndTime, productionNotBeforeTime,
                candidateProductionNotBeforeTime, candidateAvailableProductionTime, targetShift,
                firstInspectionPlan, traceChangeoverEndTime, preparationAvailableTime,
                preparationTargetShift, preparationAvailable, formalAvailableProductionTime,
                formalTargetShift, historicalResidualCapacityInfo, true);
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
     * 判断首检是否因同班次总计划量上限顺延。
     *
     * @return true-保留换模，仅顺延首检和正式生产；false-未触发该规则
     */
    public boolean isFirstInspectionDeferredByClassTotalLimit() {
        return firstInspectionDeferredByClassTotalLimit;
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

    /**
     * 判断当前候选是否命中历史班次剩余产能优先池。
     *
     * @return true-命中；false-未命中
     */
    public boolean isHistoryResidualCapacityCandidate() {
        return Objects.nonNull(historicalResidualCapacityInfo);
    }

    /**
     * 获取历史班次剩余产能画像。
     *
     * @return 历史班次剩余产能画像；未命中时返回null
     */
    public HistoricalResidualCapacityInfo getHistoricalResidualCapacityInfo() {
        return historicalResidualCapacityInfo;
    }
}
