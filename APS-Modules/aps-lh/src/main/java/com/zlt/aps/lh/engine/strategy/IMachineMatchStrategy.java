/**
 * Copyright (c) 2008, 智立通（厦门）科技有限公司 All rights reserved。
 */
package com.zlt.aps.lh.engine.strategy;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.MachinePriorityMetricSnapshot;
import com.zlt.aps.lh.engine.strategy.support.MachinePriorityTraceSnapshot;
import com.zlt.aps.lh.engine.strategy.support.SpecifiedMachineMatchResult;

import java.util.Date;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 机台匹配策略接口
 * <p>为新上机的SKU匹配最优的可用机台</p>
 *
 * @author APS
 */
public interface IMachineMatchStrategy {

    /**
     * 匹配可用硫化机台
     * <p>
     * 规则：先复用模具、胶囊、特殊物料、单控粒度等既有硬性约束保留合法机台，
     * 再严格按照“同胎胚、同模壳、同规格、胶囊共用、同英寸、相近英寸、机台编码”
     * 七层软规则排序。该入口只负责硬过滤和同层软排序；候选机台的真实可开产时间、
     * 逐班筛选及跨天重排由新增排产日驱动主链统一处理。
     * </p>
     *
     * @param context 排程上下文
     * @param sku     待排产SKU
     * @return 候选机台列表(按优先级排序)
     */
    List<MachineScheduleDTO> matchMachines(LhScheduleContext context, SkuScheduleDTO sku);

    /**
     * 校验并返回指定机台。
     *
     * <p>该入口复用普通新增选机的全部硬过滤和单控粒度规则，但不执行七层软排序、
     * 逐班候选筛选和最优机台选择。适用于业务已经固定“机台+SKU”关系的反选场景。</p>
     *
     * @param context 排程上下文
     * @param sku 待排产SKU
     * @param machineCode 历史计划指定的机台编码
     * @return 指定机台匹配结果，失败时包含明确业务原因
     */
    default SpecifiedMachineMatchResult matchSpecifiedMachine(LhScheduleContext context,
                                                              SkuScheduleDTO sku,
                                                              String machineCode) {
        return SpecifiedMachineMatchResult.failed("当前机台匹配策略不支持指定机台模式");
    }

    /**
     * 判断 SKU 在单控模式冻结时是否至少存在一个满足静态硬约束的单控侧。
     * <p>该方法只复用机台、模具、胶囊、特殊物料和窗口准入，不应用尚未冻结的单模/双模规则，
     * 也不受本轮后续 SKU 动态占用顺序影响。</p>
     *
     * @param context 排程上下文
     * @param sku 待冻结模式的SKU
     * @return true-至少存在一个可参与排产的单控侧
     */
    default boolean hasEligibleSingleControlSide(LhScheduleContext context, SkuScheduleDTO sku) {
        // 测试替身和非默认策略未参与正式 S4.3 冻结时明确返回不可参与；生产默认策略必须覆盖该方法。
        return false;
    }

    /**
     * 判断指定单控侧是否满足 SKU 的静态硬约束。
     *
     * @param context 排程上下文
     * @param sku 待排SKU
     * @param machineCode 指定单控侧机台编码
     * @return true-满足静态硬约束
     */
    default boolean isEligibleSingleControlSide(LhScheduleContext context,
                                                SkuScheduleDTO sku,
                                                String machineCode) {
        // 非默认策略必须显式实现，避免无依据放宽双模配对侧约束。
        return false;
    }

    /**
     * 从候选机台中选择最优机台
     *
     * @param context 排程上下文
     * @param sku 待排产SKU
     * @param candidates 候选机台列表
     * @param excludedMachineCodes 已尝试失败需排除的机台编码集合，只读参数，策略实现不得修改该集合
     * @return 最优机台, 无候选时返回null
     */
    MachineScheduleDTO selectBestMachine(LhScheduleContext context,
                                         SkuScheduleDTO sku,
                                         List<MachineScheduleDTO> candidates,
                                         Set<String> excludedMachineCodes);

    /**
     * 记录当前新增 SKU 实际使用的候选机台优先级顺序。
     * <p>调用方必须传入已经完成过滤、排序及动态选机调整的最终列表，
     * 本方法只负责输出日志，不得重新执行业务过滤、正式选机排序或修改候选集合；
     * 如需调整日志展示顺序，必须复制独立列表处理。</p>
     *
     * @param context 排程上下文
     * @param sku 当前待选机 SKU
     * @param orderedCandidates 本次实际选机使用的有序候选列表
     */
    default void traceMachinePriorityOrder(LhScheduleContext context,
                                           SkuScheduleDTO sku,
                                           List<MachineScheduleDTO> orderedCandidates) {
        // 测试替身和非默认策略不具备完整排序指标，保持空实现；生产默认策略负责写入明细日志。
    }

    /**
     * 构建当前选机时点的优先级日志快照。
     *
     * <p>默认实现只包装正式可选集合，保证非默认策略和既有测试替身继续兼容。
     * 生产默认策略会额外读取实时机台占用结果，补充“仅因其它 SKU 占用而暂不可选”的机台；
     * 快照不得写回正式候选集合，也不得触发模具、产能或结果资源扣减。</p>
     *
     * @param context 排程上下文
     * @param sku 当前待选机 SKU
     * @param actualOrderedCandidates 正式选机主链本轮使用的有序候选
     * @param actualSelectedMachine 正式选机主链确定的本轮首选机台
     * @param currentDayEndTime 当前业务日结束时间，用于复用停产保机约束
     * @param targetScheduleQtyResolver 正式产能计算组件（保留接口签名兼容；默认策略已不再使用产能试算）
     * @return 当前选机时点的只读日志快照
     */
    default MachinePriorityTraceSnapshot buildMachinePriorityTraceSnapshot(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            List<MachineScheduleDTO> actualOrderedCandidates,
            MachineScheduleDTO actualSelectedMachine,
            Date currentDayEndTime,
            TargetScheduleQtyResolver targetScheduleQtyResolver) {
        // 调用处明确传入正式候选和实际首选；默认策略外不扩展观察范围，避免改变未知策略语义。
        return MachinePriorityTraceSnapshot.fromActualCandidates(
                actualOrderedCandidates, actualSelectedMachine);
    }

    /**
     * 构建携带正式模具分配前软排序指标的选机日志快照。
     *
     * <p>默认实现继续调用原快照入口，保证非默认策略和测试替身无需同步修改。
     * 生产默认策略会将调用方已冻结的指标并入完整日志观察快照。</p>
     *
     * @param context 排程上下文
     * @param sku 当前待选机 SKU
     * @param actualOrderedCandidates 正式选机主链本轮使用的有序候选
     * @param actualSelectedMachine 正式选机主链确定的本轮首选机台
     * @param currentDayEndTime 当前业务日结束时间
     * @param targetScheduleQtyResolver 正式产能计算组件
     * @param priorityMetricSnapshotMap 正式模具分配前冻结的软排序指标
     * @return 当前选机时点的只读日志快照
     */
    default MachinePriorityTraceSnapshot buildMachinePriorityTraceSnapshot(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            List<MachineScheduleDTO> actualOrderedCandidates,
            MachineScheduleDTO actualSelectedMachine,
            Date currentDayEndTime,
            TargetScheduleQtyResolver targetScheduleQtyResolver,
            Map<String, MachinePriorityMetricSnapshot> priorityMetricSnapshotMap) {
        return this.buildMachinePriorityTraceSnapshot(
                context, sku, actualOrderedCandidates, actualSelectedMachine,
                currentDayEndTime, targetScheduleQtyResolver);
    }

    /**
     * 构建同时携带换模/换活字块完成时间与真实可开产时间的完整选机日志快照。
     *
     * <p>两个时间均由新增排产日驱动主链在逐班筛选时计算，并与正式落地复用同一份
     * {@code NewSpecMachineAvailabilityPlan}。默认策略外仍回落到不含这两个时间的既有入口，
     * 保证测试替身与非默认策略无需同步实现。</p>
     *
     * @param context 排程上下文
     * @param sku 当前待选机 SKU
     * @param actualOrderedCandidates 正式选机主链本轮有序候选
     * @param actualSelectedMachine 正式选机主链确定的本轮首选机台
     * @param currentDayEndTime 当前业务日结束时间
     * @param targetScheduleQtyResolver 产能计算组件
     * @param priorityMetricSnapshotMap 正式模具分配前冻结的软排序指标
     * @param traceChangeoverEndTimeMap 机台编码到换模或换活字块完成时间的映射
     * @param realAvailableProductionTimeMap 机台编码到真实可开产时间的映射
     * @return 当前选机时点的只读日志快照
     */
    default MachinePriorityTraceSnapshot buildMachinePriorityTraceSnapshot(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            List<MachineScheduleDTO> actualOrderedCandidates,
            MachineScheduleDTO actualSelectedMachine,
            Date currentDayEndTime,
            TargetScheduleQtyResolver targetScheduleQtyResolver,
            Map<String, MachinePriorityMetricSnapshot> priorityMetricSnapshotMap,
            Map<String, Date> traceChangeoverEndTimeMap,
            Map<String, Date> realAvailableProductionTimeMap) {
        return this.buildMachinePriorityTraceSnapshot(
                context, sku, actualOrderedCandidates, actualSelectedMachine,
                currentDayEndTime, targetScheduleQtyResolver, priorityMetricSnapshotMap);
    }

    /**
     * 在正式模具分配前冻结候选机台软排序指标。
     *
     * <p>默认策略外不具备完整排序指标，返回空映射即可；该方法不得修改模具、机台或产能运行态。</p>
     *
     * @param context 排程上下文
     * @param sku 当前待选机 SKU
     * @param orderedCandidates 本轮正式有序候选
     * @return 机台编码到软排序指标快照的映射
     */
    default Map<String, MachinePriorityMetricSnapshot> captureMachinePriorityMetricSnapshots(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            List<MachineScheduleDTO> orderedCandidates) {
        return Collections.emptyMap();
    }

    /**
     * 记录当前新增 SKU 已确认结果的完整选机优先级日志快照。
     *
     * <p>调用方必须先完成排程结果、机台占用和跨日在机绑定提交，再传入已标记实际命中的快照；
     * 三天窗口最终未排时，传入已标记未命中的最后一次快照。仅在候选选择时创建、尚未确认结果的
     * 快照不得调用本方法落库，避免 dayN 停止扩机及其它中间失败产生重复日志。</p>
     *
     * <p>默认实现回落到原有列表日志入口，使既有测试替身无需同步实现新接口。
     * 生产默认策略覆盖本方法后，会输出机台类型、实时占用、实际范围、实际命中及完整排序依据。</p>
     *
     * @param context 排程上下文
     * @param sku 当前待选机 SKU
     * @param traceSnapshot 已确认命中或最终未命中的日志快照
     */
    default void traceMachinePriorityOrder(LhScheduleContext context,
                                           SkuScheduleDTO sku,
                                           MachinePriorityTraceSnapshot traceSnapshot) {
        List<MachineScheduleDTO> orderedCandidates = Objects.isNull(traceSnapshot)
                ? null : traceSnapshot.getOrderedCandidates();
        // 调用旧入口是兼容既有测试替身的必要边界，不重新过滤、排序或修改候选集合。
        this.traceMachinePriorityOrder(context, sku, orderedCandidates);
    }

    /**
     * 输出续作排产后全量启用机台排序日志（不依赖具体SKU）。
     * <p>排除续作排满机台、保留续作收尾机台，按"单控优先->收尾时间->普通机台优先->特殊支持能力数"排序。</p>
     *
     * @param context 排程上下文
     */
    void traceEnabledMachineSort(LhScheduleContext context);
}
