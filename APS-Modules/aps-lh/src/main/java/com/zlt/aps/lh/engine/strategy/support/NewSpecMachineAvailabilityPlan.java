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
     * 目标业务日跨日准备提案的资源竞争班次。
     *
     * <p>普通提案为空并继续按正式开产班次竞争；当目标日中班完成换模、正式生产落到
     * 紧邻下一夜班时，保存换模开始所在班次，避免把同一候选错误归到下一业务日。</p>
     */
    private final LhShiftConfigVO sourceDayResourceShift;

    /** 是否为“目标业务日准备、紧邻下一夜班正式生产”的跨日提案。 */
    private final boolean sourceDayCrossDayPreparation;

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

    /** 与候选时间轴同源的冻结首检时间轴；旧入口为空。 */
    private final FirstInspectionTimelinePlan firstInspectionTimelinePlan;

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
        this(machine, available, unavailableReason, occupationEndTime, machineReadyTime,
                changeoverStartTime, changeoverEndTime, productionNotBeforeTime,
                candidateProductionNotBeforeTime, candidateAvailableProductionTime, targetShift,
                firstInspectionPlan, traceChangeoverEndTime, preparationAvailableTime,
                preparationTargetShift, preparationAvailable, formalAvailableProductionTime,
                formalTargetShift, historicalResidualCapacityInfo,
                firstInspectionDeferredByClassTotalLimit, null, false);
    }

    /**
     * 创建携带目标日跨日准备竞争班次的完整候选计划。
     *
     * @param machine 候选机台
     * @param available 正式候选是否可用
     * @param unavailableReason 不可用原因
     * @param occupationEndTime 前序占用结束时间
     * @param machineReadyTime 机台准备就绪时间
     * @param changeoverStartTime 换模开始时间
     * @param changeoverEndTime 换模完成时间
     * @param productionNotBeforeTime 正式生产门禁
     * @param candidateProductionNotBeforeTime 候选预演门禁
     * @param candidateAvailableProductionTime 候选预演可开产时间
     * @param targetShift 候选预演班次
     * @param firstInspectionPlan 首检计划
     * @param traceChangeoverEndTime 日志展示换模完成时间
     * @param preparationAvailableTime 准备完成时间
     * @param preparationTargetShift 准备完成班次
     * @param preparationAvailable 准备时间轴是否可用
     * @param formalAvailableProductionTime 正式可开产时间
     * @param formalTargetShift 正式开产班次
     * @param historicalResidualCapacityInfo 历史剩余产能画像
     * @param firstInspectionDeferredByClassTotalLimit 首检是否因班次总量顺延
     * @param sourceDayResourceShift 目标日准备动作参与竞争的班次
     * @param sourceDayCrossDayPreparation 是否为目标日跨日准备提案
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
            boolean firstInspectionDeferredByClassTotalLimit,
            LhShiftConfigVO sourceDayResourceShift,
            boolean sourceDayCrossDayPreparation) {
        this(machine, available, unavailableReason, occupationEndTime, machineReadyTime,
                changeoverStartTime, changeoverEndTime, productionNotBeforeTime,
                candidateProductionNotBeforeTime, candidateAvailableProductionTime, targetShift,
                firstInspectionPlan, traceChangeoverEndTime, preparationAvailableTime,
                preparationTargetShift, preparationAvailable, formalAvailableProductionTime,
                formalTargetShift, historicalResidualCapacityInfo,
                firstInspectionDeferredByClassTotalLimit, sourceDayResourceShift,
                sourceDayCrossDayPreparation, null);
    }

    /**
     * 创建携带冻结首检时间轴的完整候选计划。
     *
     * @param machine 候选机台
     * @param available 是否可用
     * @param unavailableReason 不可用原因
     * @param occupationEndTime 前序占用结束时间
     * @param machineReadyTime 机台准备就绪时间
     * @param changeoverStartTime 切换开始时间
     * @param changeoverEndTime 切换完成时间
     * @param productionNotBeforeTime 正式生产门禁
     * @param candidateProductionNotBeforeTime 候选预演门禁
     * @param candidateAvailableProductionTime 候选预演可开产时间
     * @param targetShift 候选预演班次
     * @param firstInspectionPlan 首检分摊计划
     * @param traceChangeoverEndTime 日志展示切换完成时间
     * @param preparationAvailableTime 准备完成时间
     * @param preparationTargetShift 准备完成班次
     * @param preparationAvailable 准备是否可用
     * @param formalAvailableProductionTime 正式可开产时间
     * @param formalTargetShift 正式开产班次
     * @param historicalResidualCapacityInfo 历史剩余产能画像
     * @param firstInspectionDeferredByClassTotalLimit 首检是否顺延
     * @param sourceDayResourceShift 跨日准备资源班次
     * @param sourceDayCrossDayPreparation 是否跨日准备
     * @param firstInspectionTimelinePlan 冻结首检时间轴
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
            boolean firstInspectionDeferredByClassTotalLimit,
            LhShiftConfigVO sourceDayResourceShift,
            boolean sourceDayCrossDayPreparation,
            FirstInspectionTimelinePlan firstInspectionTimelinePlan) {
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
        this.firstInspectionTimelinePlan = firstInspectionTimelinePlan;
        this.preparationAvailableTime = preparationAvailableTime;
        this.preparationTargetShift = preparationTargetShift;
        this.preparationAvailable = preparationAvailable;
        this.historicalResidualCapacityInfo = historicalResidualCapacityInfo;
        this.firstInspectionDeferredByClassTotalLimit = firstInspectionDeferredByClassTotalLimit;
        this.sourceDayResourceShift = sourceDayResourceShift;
        this.sourceDayCrossDayPreparation = sourceDayCrossDayPreparation;
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
                firstInspectionDeferredByClassTotalLimit,
                sourceDayResourceShift, sourceDayCrossDayPreparation);
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
     * 在不重建其它时间轴的前提下冻结首检时间轴。
     *
     * @param timelinePlan 候选预演生成的时间轴
     * @return 携带同一时间轴的新候选计划
     */
    public NewSpecMachineAvailabilityPlan withFirstInspectionTimelinePlan(
            FirstInspectionTimelinePlan timelinePlan) {
        return new NewSpecMachineAvailabilityPlan(
                machine, available, unavailableReason, occupationEndTime, machineReadyTime,
                changeoverStartTime, changeoverEndTime, productionNotBeforeTime,
                candidateProductionNotBeforeTime, candidateAvailableProductionTime, targetShift,
                firstInspectionPlan, traceChangeoverEndTime, preparationAvailableTime,
                preparationTargetShift, preparationAvailable, formalAvailableProductionTime,
                formalTargetShift, historicalResidualCapacityInfo,
                firstInspectionDeferredByClassTotalLimit,
                sourceDayResourceShift, sourceDayCrossDayPreparation, timelinePlan);
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
                firstInspectionDeferredByClassTotalLimit,
                sourceDayResourceShift, sourceDayCrossDayPreparation);
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
                formalTargetShift, historicalResidualCapacityInfo, true,
                sourceDayResourceShift, sourceDayCrossDayPreparation);
    }

    /**
     * 将当前合法时间轴标记为目标业务日跨日准备提案。
     *
     * @param resourceShift 换模开始所在的目标日资源班次
     * @return 携带跨日准备竞争口径的新计划对象
     */
    public NewSpecMachineAvailabilityPlan withSourceDayCrossDayPreparation(
            LhShiftConfigVO resourceShift) {
        boolean crossDayAvailable = available && Objects.nonNull(resourceShift)
                && Objects.nonNull(formalTargetShift);
        return new NewSpecMachineAvailabilityPlan(
                machine, crossDayAvailable,
                crossDayAvailable ? unavailableReason : "目标日跨日准备资源班次为空",
                occupationEndTime, machineReadyTime, changeoverStartTime, changeoverEndTime,
                productionNotBeforeTime, candidateProductionNotBeforeTime,
                candidateAvailableProductionTime, targetShift, firstInspectionPlan,
                traceChangeoverEndTime, preparationAvailableTime, preparationTargetShift,
                preparationAvailable, formalAvailableProductionTime, formalTargetShift,
                historicalResidualCapacityInfo, firstInspectionDeferredByClassTotalLimit,
                resourceShift, crossDayAvailable);
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
     * 获取当前Machine×SKU真正开始占用结构机台名额的时间。
     *
     * <p>首检属于开产时取首检开始时间；无计件首检时取正式可开产时间。</p>
     *
     * @return 生产占用开始时间；计划不可用时返回null
     */
    public Date getProductionOccupationStartTime() {
        if (Objects.nonNull(firstInspectionTimelinePlan)
                && Objects.nonNull(firstInspectionTimelinePlan.getProductionOccupationStartTime())) {
            return firstInspectionTimelinePlan.getProductionOccupationStartTime();
        }
        if (Objects.nonNull(firstInspectionPlan)
                && firstInspectionPlan.isValid()
                && firstInspectionPlan.getInspectionQty() > 0
                && Objects.nonNull(firstInspectionPlan.getInspectionStartTime())) {
            return firstInspectionPlan.getInspectionStartTime();
        }
        return formalAvailableProductionTime;
    }

    /**
     * 获取当前Machine×SKU真正开始占用结构机台名额的班次。
     *
     * <p>首检可跨班时取首个正量分摊班次；无计件首检时取正式生产班次。</p>
     *
     * @return 生产占用班次；计划不可用时返回null
     */
    public LhShiftConfigVO getProductionOccupationShift() {
        if (Objects.nonNull(firstInspectionTimelinePlan)
                && Objects.nonNull(firstInspectionTimelinePlan.getProductionOccupationShift())) {
            return firstInspectionTimelinePlan.getProductionOccupationShift();
        }
        if (Objects.nonNull(firstInspectionPlan)
                && firstInspectionPlan.isValid()
                && firstInspectionPlan.getInspectionQty() > 0
                && !firstInspectionPlan.getShiftAllocations().isEmpty()) {
            FirstInspectionShiftAllocation firstAllocation =
                    firstInspectionPlan.getShiftAllocations().get(0);
            if (Objects.nonNull(firstAllocation)) {
                return firstAllocation.getShift();
            }
        }
        return formalTargetShift;
    }

    /**
     * 获取机台驱动本轮实际使用的资源竞争班次。
     *
     * @return 目标日跨日准备返回换模资源班次；普通场景返回正式开产班次
     */
    public LhShiftConfigVO getCompetitionTargetShift() {
        return sourceDayCrossDayPreparation
                ? sourceDayResourceShift : this.getProductionOccupationShift();
    }

    /**
     * 获取目标日跨日准备的换模资源班次。
     *
     * @return 资源班次；普通计划为空
     */
    public LhShiftConfigVO getSourceDayResourceShift() {
        return sourceDayResourceShift;
    }

    /**
     * 判断当前计划是否为目标业务日准备、紧邻下一夜班生产。
     *
     * @return true-目标日跨日准备；false-普通或生产日前回看计划
     */
    public boolean isSourceDayCrossDayPreparation() {
        return sourceDayCrossDayPreparation;
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
     * 获取候选预演冻结的首检时间轴。
     *
     * @return 冻结时间轴；旧调用链或无首检场景可能为空
     */
    public FirstInspectionTimelinePlan getFirstInspectionTimelinePlan() {
        return firstInspectionTimelinePlan;
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
