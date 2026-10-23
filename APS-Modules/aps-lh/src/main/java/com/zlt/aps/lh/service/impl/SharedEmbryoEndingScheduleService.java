package com.zlt.aps.lh.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.ContinuationEndingAllocationSnapshot;
import com.zlt.aps.lh.engine.strategy.support.ContinuationEndingMachineProfile;
import com.zlt.aps.lh.engine.strategy.support.ContinuationFinishScheduleResult;
import com.zlt.aps.lh.engine.strategy.support.ContinuationMachineFinishPlan;
import com.zlt.aps.lh.engine.strategy.support.ContinuationRemainderFinishGroup;
import com.zlt.aps.lh.engine.strategy.support.ContinuationResultSupport;
import com.zlt.aps.lh.engine.strategy.support.PreviousAlternatePlanReleaseEvent;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.LhMouldCodeUtil;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 胎胚收尾公共事实的兼容消费入口，不再分配逐机数量或维护独立的完成判定。
 * 数量、节点及释放由现有公共收尾生成，正式消费或设备回裁后一起复核。
 *
 * @author APS
 * @date 2026/10/22
 */
@Component
public class SharedEmbryoEndingScheduleService {

    /**
     * 复用冻结运行模式、来源解析及资源释放登记。
     */
    private final ContinuationResultSupport resultSupport;

    /**
     * 保留无容器直接构造入口，运行状态仅来自调用上下文。
     */
    public SharedEmbryoEndingScheduleService() {
        this(new ContinuationResultSupport());
    }

    /**
     * 注入共用结果支持，不引入策略回调或第二个数量分配器。
     *
     * @param resultSupport 结果及资源状态支持
     */
    @Autowired
    public SharedEmbryoEndingScheduleService(ContinuationResultSupport resultSupport) {
        this.resultSupport = resultSupport;
    }

    /**
     * 正式扣账回裁或设备复评后同步胎胚组、逐机快照及兼容任务视图。
     * 未达到总份额的机台只有已有强制退出事实可以释放，不能凭本机任务完成释放。
     *
     * @param context 已完成本轮数量调整的上下文
     */
    public void refreshCommonFinishFacts(LhScheduleContext context) {
        List<ContinuationRemainderFinishGroup> groups = context.getContinuationRemainderFinishGroups().stream()
                .filter(group -> group.getEndingType() == ContinuationRemainderFinishGroup.EndingType.EMBRYO_STOCK)
                .collect(Collectors.toList());
        if (groups.isEmpty()) {
            return;
        }
        Set<LhScheduleResult> live = Collections.newSetFromMap(new IdentityHashMap<>());
        live.addAll(context.getScheduleResultList());
        Map<String, Integer> scheduled = context.getScheduleResultList().stream()
                .filter(result -> !LhSingleControlMachineUtil.isVirtualMachineCode(result.getLhMachineCode()))
                .collect(Collectors.toMap(result -> MonthPlanDateResolver.buildMaterialStatusKey(
                        result.getMaterialCode(), result.getProductStatus()), ShiftFieldUtil::resolveScheduledQty,
                        Integer::sum, LinkedHashMap::new));
        Date windowEnd = context.getScheduleWindowShifts().get(context.getScheduleWindowShifts().size() - 1).getShiftEndDateTime();
        for (ContinuationRemainderFinishGroup group : groups) {
            String key = MonthPlanDateResolver.buildMaterialStatusKey(
                    group.getSourceSku().getMaterialCode(), group.getSourceSku().getProductStatus());
            int remaining = Math.max(0, group.getFinalTargetQty() - scheduled.getOrDefault(key, 0));
            boolean completed = group.getFinalTargetQty() > 0 && remaining == 0;
            ContinuationFinishScheduleResult previous = group.getOriginalMachineFinishPlan();
            if (Objects.isNull(previous)) {
                continue;
            }
            // 写时复制公共输出，嵌套预演不得改写调用前的完成状态。
            ContinuationFinishScheduleResult updated = BeanUtil.copyProperties(previous, ContinuationFinishScheduleResult.class);
            List<ContinuationMachineFinishPlan> plans = previous.getMachinePlans().stream()
                    .map(plan -> BeanUtil.copyProperties(plan, ContinuationMachineFinishPlan.class)).collect(Collectors.toList());
            for (int position = 0; position < plans.size(); position++) {
                ContinuationEndingMachineProfile profile = group.getMachineProfiles().get(position);
                List<LhScheduleResult> originals = profile.getOriginals();
                int unitQty = originals.stream().filter(live::contains).mapToInt(ShiftFieldUtil::resolveScheduledQty).sum();
                for (LhScheduleResult result : originals) {
                    ContinuationEndingAllocationSnapshot old = context.getContinuationSurplusEndingSnapshotMap().get(result);
                    if (Objects.isNull(old)) {
                        throw new IllegalStateException("公共胎胚收尾缺少已提交机台快照：" + result.getLhMachineCode());
                    }
                    ContinuationEndingAllocationSnapshot snapshot = BeanUtil.copyProperties(old, ContinuationEndingAllocationSnapshot.class);
                    int[] quantities = context.getScheduleWindowShifts().stream().mapToInt(shift -> {
                        Integer value = live.contains(result) ? ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex()) : null;
                        return Objects.isNull(value) ? 0 : value;
                    }).toArray();
                    Date productionEnd = live.contains(result) ? resultSupport.resolveLastPositiveProductionEndTime(result) : null;
                    boolean changed = !java.util.Arrays.equals(old.getShiftQuantities(), quantities)
                            || !Objects.equals(old.getProductionEndTime(), productionEnd);
                    snapshot.setShiftQuantities(quantities);
                    snapshot.setProductionEndTime(productionEnd);
                    if (changed) {
                        // 数量改变后旧规则节点不再证明强制退出；设备硬退出先更新数组的事实仍可保留。
                        snapshot.setFinishState(completed ? ContinuationMachineFinishPlan.FinishState.SURPLUS_COMPLETED
                                : ContinuationMachineFinishPlan.FinishState.WINDOW_UNFINISHED);
                        if (completed) {
                            snapshot.setReleaseTime(profile.physicalReadyTime(profile.switchReadyTime(unitQty)));
                        }
                    }
                    if (!completed && snapshot.getFinishState() == ContinuationMachineFinishPlan.FinishState.SURPLUS_COMPLETED) {
                        snapshot.setFinishState(ContinuationMachineFinishPlan.FinishState.WINDOW_UNFINISHED);
                    }
                    if (snapshot.getFinishState() == ContinuationMachineFinishPlan.FinishState.WINDOW_UNFINISHED) {
                        snapshot.setReleaseTime(windowEnd);
                    }
                    context.getContinuationSurplusEndingSnapshotMap().put(result, snapshot);
                    // 兼容映射仅是公共快照的投影，不能再以本机任务量推导完成。
                    context.getSharedEmbryoStockEndingMachineQuotaMap().put(result, java.util.Arrays.stream(quantities).sum());
                    context.getSharedEmbryoStockEndingMachineReleaseMap().put(result, snapshot.getReleaseTime());
                }
                ContinuationEndingAllocationSnapshot fact = context.getContinuationSurplusEndingSnapshotMap().get(originals.get(0));
                ContinuationMachineFinishPlan plan = plans.get(position);
                plan.setPlanQty(unitQty);
                plan.setFinishState(fact.getFinishState());
                plan.setFinishTime(fact.getProductionEndTime());
                int lastShift = ShiftFieldUtil.resolveLastPlannedShiftIndex(originals.get(0));
                plan.setFinishShiftIndex(lastShift > 0 ? lastShift : null);
                plan.setOfflineTime(fact.getReleaseTime());
            }
            updated.setMachinePlans(plans);
            updated.setRemainingQty(remaining);
            group.setOriginalMachineFinishPlan(updated);
        }
    }

    /**
     * 历史后料只消费公共节点确认的真实释放，计算前、固定状态链及窗口未完成均继续等待。
     *
     * @param context 公共收尾快照及当前结果
     * @param event 历史指定前后料关系
     * @param sourceSku 前料来源
     * @param sourceResults 当前机台前料结果，零量清理后可以为空
     * @param shifts 真实窗口班次
     * @return 可释放时的物理时刻；仍待收尾时保留原事件时刻并设置等待状态
     */
    public Date resolveSharedEmbryoEndingReleaseTime(LhScheduleContext context,
            PreviousAlternatePlanReleaseEvent event, SkuScheduleDTO sourceSku,
            List<LhScheduleResult> sourceResults, List<LhShiftConfigVO> shifts) {
        boolean whole = LhSingleControlMachineUtil.isWholeMachineGranularitySku(context, sourceSku);
        List<LhScheduleResult> sources = context.getContinuationSurplusEndingSnapshotMap().keySet().stream()
                .filter(result -> StringUtils.equals(event.getMaterialCode(), result.getMaterialCode())
                        && StringUtils.equals(event.getProductStatus(), result.getProductStatus()))
                .filter(result -> this.sameUnit(event.getMachineCode(), result.getLhMachineCode(), whole))
                .collect(Collectors.toList());
        List<LhScheduleResult> occupied = context.getScheduleResultList().stream()
                .filter(result -> this.sameUnit(event.getMachineCode(), result.getLhMachineCode(), whole))
                .collect(Collectors.toList());
        Date release = this.resolveConfirmedRelease(context, sources, occupied);
        event.setSharedEmbryoEndingPending(Objects.isNull(release));
        if (Objects.isNull(release)) {
            event.setCompletedStrictEndingReleaseTime(null);
            event.setCompletedEmbryoEndingWindowEndTime(null);
            resultSupport.logPreviousAlternateReleaseDecision(context, event, "公共胎胚收尾未确认释放，保留前料及完整状态链占用");
            return event.getOfflineTime();
        }
        return this.updateConfirmedEvent(context, event, release, shifts);
    }

    /**
     * 将已确认公共释放同步到历史事件，发布阶段复用已有结论，避免逐机重新扫描整批结果。
     *
     * @param context 当前设备约束及日志上下文
     * @param event 物料状态一致的历史事件
     * @param release 公共快照已确认的物理时刻
     * @param shifts 完整窗口班次
     * @return 计入胶囊占用后的真实释放
     */
    private Date updateConfirmedEvent(LhScheduleContext context, PreviousAlternatePlanReleaseEvent event,
            Date release, List<LhShiftConfigVO> shifts) {
        Date ready = ShiftCapacityResolverUtil.resolveCapsuleReplacementReadyTime(context, event.getMachineCode(), release);
        boolean completed = context.getContinuationRemainderFinishGroups().stream()
                .filter(group -> group.getEndingType() == ContinuationRemainderFinishGroup.EndingType.EMBRYO_STOCK)
                .filter(group -> StringUtils.equals(group.getSourceSku().getMaterialCode(), event.getMaterialCode())
                        && StringUtils.equals(group.getSourceSku().getProductStatus(), event.getProductStatus()))
                .anyMatch(group -> group.getFinalTargetQty() > 0 && Objects.nonNull(group.getOriginalMachineFinishPlan())
                        && group.getOriginalMachineFinishPlan().getRemainingQty() == 0);
        event.setSharedEmbryoEndingPending(false);
        event.setOfflineTime(ready);
        event.setCompletedStrictEndingReleaseTime(completed ? ready : null);
        event.setCompletedEmbryoEndingWindowEndTime(completed ? shifts.get(shifts.size() - 1).getShiftEndDateTime() : null);
        resultSupport.logPreviousAlternateReleaseDecision(context, event,
                "按公共胎胚收尾物理释放承接后料；释放=" + LhScheduleTimeUtil.formatDateTime(ready));
        return ready;
    }

    /**
     * 根据公共完成事实及当前量确认一个冻结生产单元的真实释放，固定链资源仍由完整链占用。
     *
     * @param context 公共快照及有效结果
     * @param sources 同物料状态的一个生产单元
     * @param occupied 此冻结生产单元当前的全部有效结果
     * @return 已确认物理释放；无快照、数量变化或仍被固定段占用时返回空
     */
    private Date resolveConfirmedRelease(LhScheduleContext context, List<LhScheduleResult> sources,
            List<LhScheduleResult> occupied) {
        if (sources.isEmpty()) {
            return null;
        }
        Date release = null;
        for (LhScheduleResult result : sources) {
            ContinuationEndingAllocationSnapshot snapshot = context.getContinuationSurplusEndingSnapshotMap().get(result);
            boolean present = occupied.contains(result);
            int[] actual = context.getScheduleWindowShifts().stream().mapToInt(shift -> {
                Integer value = present ? ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex()) : null;
                return Objects.isNull(value) ? 0 : value;
            }).toArray();
            if (Objects.isNull(snapshot) || Objects.isNull(snapshot.getReleaseTime())
                    || (snapshot.getFinishState() != ContinuationMachineFinishPlan.FinishState.SURPLUS_COMPLETED
                    && snapshot.getFinishState() != ContinuationMachineFinishPlan.FinishState.FORCED_RELEASE)
                    || !java.util.Arrays.equals(snapshot.getShiftQuantities(), actual)
                    || !Objects.equals(snapshot.getProductionEndTime(),
                    present ? resultSupport.resolveLastPositiveProductionEndTime(result) : null)) {
                return null;
            }
            release = Objects.isNull(release) || snapshot.getReleaseTime().after(release) ? snapshot.getReleaseTime() : release;
        }
        for (LhScheduleResult other : occupied) {
            if (sources.contains(other)) {
                continue;
            }
            boolean fixedChain = java.util.stream.IntStream.rangeClosed(1, LhScheduleConstant.MAX_SHIFT_SLOT_COUNT)
                    .anyMatch(index -> StringUtils.contains(ShiftFieldUtil.getShiftAnalysis(other, index),
                            LhScheduleConstant.SAME_MATERIAL_STATUS_CONTINUATION_ANALYSIS));
            Date end = resultSupport.resolveLastPositiveProductionEndTime(other);
            if (fixedChain || (ShiftFieldUtil.resolveScheduledQty(other) > 0
                    && (Objects.isNull(end) || end.after(release)))) {
                return null;
            }
        }
        return release;
    }

    /**
     * 发布正式消费后的公共释放；未完成或零份额机台继续占用至窗口末端。
     *
     * @param context 正式扣账后的公共组与机台状态
     */
    public void publishSharedEmbryoEndingMachineRelease(LhScheduleContext context) {
        if (!context.isContinuousDailyQuotaSynced()) {
            return;
        }
        List<LhScheduleResult> tasks = new ArrayList<>(context.getSharedEmbryoStockEndingMachineQuotaMap().keySet());
        if (tasks.isEmpty()) {
            return;
        }
        Set<String> wholeCodes = resultSupport.resolveWholeSingleControlMachineCodes(context, tasks);
        Map<String, List<LhScheduleResult>> units = tasks.stream().collect(Collectors.groupingBy(result ->
                MonthPlanDateResolver.buildMaterialStatusKey(result.getMaterialCode(), result.getProductStatus()) + "|"
                        + (wholeCodes.contains(result.getLhMachineCode())
                        ? LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode()) : result.getLhMachineCode())));
        // 一次构造占用索引，逐机发布只读取本生产单元，不重新扫描全量结果。
        Map<String, List<LhScheduleResult>> occupiedByCode = context.getScheduleResultList().stream()
                .collect(Collectors.groupingBy(LhScheduleResult::getLhMachineCode));
        Map<String, List<LhScheduleResult>> occupiedByPhysical = context.getScheduleResultList().stream()
                .collect(Collectors.groupingBy(result -> LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode())));
        for (List<LhScheduleResult> sides : units.values()) {
            String code = sides.get(0).getLhMachineCode();
            List<LhScheduleResult> occupied = wholeCodes.contains(code)
                    ? occupiedByPhysical.getOrDefault(LhSingleControlMachineUtil.resolvePhysicalMachineCode(code), Collections.emptyList())
                    : occupiedByCode.getOrDefault(code, Collections.emptyList());
            Date release = this.resolveConfirmedRelease(context, sides, occupied);
            for (LhScheduleResult result : sides) {
                MachineScheduleDTO machine = context.getMachineScheduleMap().get(result.getLhMachineCode());
                PreviousAlternatePlanReleaseEvent event = context.getPreviousAlternateReleaseEventMap().get(result.getLhMachineCode());
                boolean sameEvent = Objects.nonNull(event) && StringUtils.equals(event.getMaterialCode(), result.getMaterialCode())
                        && StringUtils.equals(event.getProductStatus(), result.getProductStatus());
                if (Objects.isNull(machine) || context.isContinuousStopHoldMachine(result.getLhMachineCode())
                        || context.getOnlySandBlastContinuationReleaseWindowMap().containsKey(result.getLhMachineCode())
                        || context.getContinuationTemporaryFaultTransferEventMap().containsKey(result.getLhMachineCode())) {
                    continue;
                }
                if (Objects.isNull(release)) {
                    machine.setEnding(false);
                    machine.setEstimatedEndTime(context.getScheduleWindowShifts()
                            .get(context.getScheduleWindowShifts().size() - 1).getShiftEndDateTime());
                    context.getReleasedContinuousMachineCodeSet().remove(result.getLhMachineCode());
                    if (sameEvent) {
                        event.setSharedEmbryoEndingPending(true);
                        event.setCompletedStrictEndingReleaseTime(null);
                        event.setCompletedEmbryoEndingWindowEndTime(null);
                        context.getPreviousAlternateReleasedMachineTimeMap().remove(result.getLhMachineCode());
                        context.getPreviousAlternateReleasedSkuKeyMap().remove(result.getLhMachineCode());
                    }
                    LhMouldCodeUtil.resolveInMachineMouldCodeSet(context, result.getLhMachineCode()).forEach(mould ->
                            context.getPreScheduledMouldReleaseTimeMap().put(mould, machine.getEstimatedEndTime()));
                    continue;
                }
                machine.setEnding(true);
                machine.setEstimatedEndTime(release);
                // 公共画像已按冻结模式成组，逐侧登记，不能用物料级查找把独立侧扩散成整机释放。
                context.getReleasedContinuousMachineCodeSet().add(result.getLhMachineCode());
                LhMouldCodeUtil.resolveInMachineMouldCodeSet(context, result.getLhMachineCode()).forEach(mould ->
                        context.getPreScheduledMouldReleaseTimeMap().put(mould, release));
                if (sameEvent) {
                    this.updateConfirmedEvent(context, event, release, context.getScheduleWindowShifts());
                }
            }
        }
        context.setMouldResourceContext(null);
    }

    /**
     * 根据冻结运行模式比较生产单元，独立侧保留原编码。
     *
     * @param first 第一个运行机台编码
     * @param second 第二个运行机台编码
     * @param whole 是否整机模式
     * @return 是否属于同一冻结生产单元
     */
    private boolean sameUnit(String first, String second, boolean whole) {
        return whole ? StringUtils.equals(LhSingleControlMachineUtil.resolvePhysicalMachineCode(first),
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(second)) : StringUtils.equals(first, second);
    }
}
