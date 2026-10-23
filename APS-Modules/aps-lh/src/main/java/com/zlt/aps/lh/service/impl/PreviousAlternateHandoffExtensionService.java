package com.zlt.aps.lh.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.TrialStatusEnum;
import com.zlt.aps.lh.api.enums.MouldChangeTypeEnum;
import com.zlt.aps.lh.api.enums.ShiftEnum;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.DayScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.PreviousAlternatePlanReleaseEvent;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * 原历史复用之后的附加交接决策，仅处理未兑现收尾交接及正规连续计划。
 * 每次仅试排指定组合，失败恢复完整状态，不重跑原历史阶段或时间下机。
 *
 * @author APS
 * @date 2026/10/22
 */
@Service
public class PreviousAlternateHandoffExtensionService {

    /**
     * 为自然收尾后仍未兑现的历史后料评估强制交接，原链成功的组合不再处理。
     *
     * @param context 已完成原历史复用且已扣续作账本的上下文
     * @param release 在快照内截量、重扣消费并回流前料的适配器
     * @param attempt 复用原指定机台正式承接的适配器
     */
    public void recoverUnfulfilled(LhScheduleContext context,
            BiPredicate<PreviousAlternatePlanReleaseEvent, Date> release,
            BiPredicate<PreviousAlternatePlanReleaseEvent, DayScheduleContext> attempt) {
        if (!this.isFormalStage(context)) {
            return;
        }
        Set<Long> submitted = this.submittedPlans(context);
        Map<String, List<LhScheduleResult>> results = context.getScheduleResultList().stream()
                .collect(Collectors.groupingBy(LhScheduleResult::getLhMachineCode));
        List<PreviousAlternatePlanReleaseEvent> events = context.getPreviousAlternateReleaseEventMap().values()
                .stream().filter(event -> !event.isBeforeMaterialNotScheduled()
                        && !Objects.equals(event.getPlan().getBeforeMaterialCode(), event.getPlan().getAfterMaterialCode())
                        && !submitted.contains(event.getPlan().getId()))
                .sorted(Comparator.comparing(PreviousAlternatePlanReleaseEvent::getPlannedShiftStartTime))
                .collect(Collectors.toList());
        for (PreviousAlternatePlanReleaseEvent event : events) {
            List<LhScheduleResult> own = results.get(event.getMachineCode());
            if (submitted.contains(event.getPlan().getId()) || Objects.isNull(own)
                    || context.isContinuousStopHoldMachine(event.getMachineCode())
                    || own.stream().anyMatch(result -> context.hasTimedMachineOffRequest(result)
                    || context.getTimedMachineOffDecisionMap().containsKey(result)
                    || !Objects.equals(result.getMaterialCode(), event.getMaterialCode()))) {
                continue;
            }
            boolean accepted = false;
            if (event.isRemainderFinishDeferred()) {
                accepted = this.trySlots(context, event, event.getPlannedShiftStartTime(), release, attempt);
            } else if (context.getScheduleWindowShifts().stream().anyMatch(shift ->
                    (shift.isMorningShift() || shift.isAfternoonShift())
                            && shift.getShiftEndDateTime().after(event.getOfflineTime())
                            && shift.getShiftStartDateTime().before(event.getPlannedShiftEndTime())
                            && this.isQuotaFull(context, shift))) {
                // 原链已经释放的普通前料只补额度顺延，不再次截量或改变其原下机事实。
                accepted = this.trySlots(context, event, event.getOfflineTime(), (ignored, time) -> true, attempt);
            }
            if (accepted) {
                // 成对提交可能同时兑现另一侧，只有成功后刷新索引，避免下一事件重复截量。
                submitted.addAll(this.submittedPlans(context));
                results = context.getScheduleResultList().stream()
                        .collect(Collectors.groupingBy(LhScheduleResult::getLhMachineCode));
            }
        }
    }

    /**
     * 推进已完成正规前段的连续历史计划，试制量试仍交由原专用链处理。
     *
     * @param context 原历史结果及消费账本
     * @param plans 已冻结并按历史顺序排列的有效计划
     * @param eventFactory 复用原班次解析构造局部事件
     * @param attempt 原指定承接适配器
     */
    public void continueFormalChains(LhScheduleContext context, List<LhMouldChangePlan> plans,
            BiFunction<LhMouldChangePlan, LhScheduleResult, PreviousAlternatePlanReleaseEvent> eventFactory,
            BiPredicate<PreviousAlternatePlanReleaseEvent, DayScheduleContext> attempt) {
        if (!this.isFormalStage(context) || plans.size() < 2) {
            return;
        }
        Set<Long> submitted = this.submittedPlans(context);
        Map<String, LhScheduleResult> lastByMachine = new HashMap<>();
        Map<String, Integer> quantities = new HashMap<>();
        this.indexResults(context, lastByMachine, quantities);
        for (LhMouldChangePlan plan : plans) {
            LhScheduleResult previous = lastByMachine.get(plan.getLhMachineCode());
            if (submitted.contains(plan.getId()) || !this.isCompletedFormalPredecessor(
                    context, plan, previous, quantities)) {
                continue;
            }
            PreviousAlternatePlanReleaseEvent event = eventFactory.apply(plan, previous);
            if (this.trySlots(context, event, previous.getSpecEndTime(), (ignored, time) -> true, attempt)) {
                // 只有成功改变正式结果时刷新索引；失败计划不重复扫描全部结果。
                submitted.addAll(this.submittedPlans(context));
                this.indexResults(context, lastByMachine, quantities);
            }
        }
    }

    /**
     * 仅在正式历史阶段执行扩展，嵌套时间及余量预演保持原输入。
     *
     * @param context 当前排程上下文
     * @return 已完成续作扣账且非预演时为真
     */
    private boolean isFormalStage(LhScheduleContext context) {
        return context.isContinuousDailyQuotaSynced() && !context.isTimedMachineOffLoadPreview()
                && !context.isTimedMachineOffHandoffPreview() && !context.isPreviousAlternateGroupPreview();
    }

    /**
     * 从仍有效的正量结果提取已提交计划，展示来源和失败预演不算完成。
     *
     * @param context 当前正式结果
     * @return 已兑现计划编号集合
     */
    private Set<Long> submittedPlans(LhScheduleContext context) {
        Set<LhScheduleResult> live = Collections.newSetFromMap(new IdentityHashMap<>());
        live.addAll(context.getScheduleResultList());
        return context.getPreviousAlternateResultPlanMap().entrySet().stream()
                .filter(entry -> live.contains(entry.getKey()) && ShiftFieldUtil.resolveScheduledQty(entry.getKey()) > 0)
                .map(entry -> entry.getValue().getId()).collect(Collectors.toSet());
    }

    /**
     * 一次扫描建立每机最后正量及物料状态总量索引。
     *
     * @param context 正式结果上下文
     * @param lastByMachine 待更新的机台末段索引
     * @param quantities 待更新的物料状态数量索引
     */
    private void indexResults(LhScheduleContext context, Map<String, LhScheduleResult> lastByMachine,
            Map<String, Integer> quantities) {
        lastByMachine.clear();
        quantities.clear();
        for (LhScheduleResult result : context.getScheduleResultList()) {
            int quantity = ShiftFieldUtil.resolveScheduledQty(result);
            if (quantity <= 0 || Objects.isNull(result.getSpecEndTime())) {
                continue;
            }
            quantities.merge(MonthPlanDateResolver.buildMaterialStatusKey(
                    result.getMaterialCode(), result.getProductStatus()), quantity, Integer::sum);
            lastByMachine.merge(result.getLhMachineCode(), result,
                    (left, right) -> left.getSpecEndTime().after(right.getSpecEndTime()) ? left : right);
        }
    }

    /**
     * 校验正规前段已真实完成且同机历史顺序衔接，部分排产不得推进后段。
     *
     * @param context 正式来源关联和共享目标
     * @param plan 待承接历史计划
     * @param previous 本机最后正量结果
     * @param quantities 同物料状态累计量
     * @return 正规前段完整完成时为真
     */
    private boolean isCompletedFormalPredecessor(LhScheduleContext context, LhMouldChangePlan plan,
            LhScheduleResult previous, Map<String, Integer> quantities) {
        if (Objects.isNull(previous) || !MouldChangeTypeEnum.containsAnyCode(plan.getChangeMouldType(),
                MouldChangeTypeEnum.REGULAR.getCode(), MouldChangeTypeEnum.TYPE_BLOCK.getCode())
                || (!ShiftEnum.MORNING_SHIFT.getCode().equals(plan.getClassIndex())
                && !ShiftEnum.AFTERNOON_SHIFT.getCode().equals(plan.getClassIndex()))
                || !TrialStatusEnum.FORMAL.getCode().equals(plan.getProductStatus())
                || !TrialStatusEnum.FORMAL.getCode().equals(previous.getProductStatus())
                || !Objects.equals(previous.getMaterialCode(), plan.getBeforeMaterialCode())
                || Objects.equals(plan.getBeforeMaterialCode(), plan.getAfterMaterialCode())) {
            return false;
        }
        LhMouldChangePlan source = context.getPreviousAlternateResultPlanMap().get(previous);
        String key = MonthPlanDateResolver.buildMaterialStatusKey(previous.getMaterialCode(), previous.getProductStatus());
        Integer target = context.getSkuProductionTargetQtyMap().get(key);
        return Objects.nonNull(source) && PreviousAlternatePlanEligibilityService.planOrder().compare(source, plan) < 0
                && Objects.nonNull(target) && target > 0 && quantities.getOrDefault(key, 0) >= target;
    }

    /**
     * 从原允许区间评估，只有真实额度不足才将候选扩展到后续班次。
     * 无额度的班次仅做常数时间读取，不捕获快照、不调用排产。
     *
     * @param context 实时资源上下文
     * @param event 精确历史事件
     * @param earliest 首次允许交接时刻
     * @param release 截量适配器；连续链无需额外截量
     * @param attempt 原正式提交适配器
     * @return 指定后料正量提交时为真
     */
    private boolean trySlots(LhScheduleContext context, PreviousAlternatePlanReleaseEvent event, Date earliest,
            BiPredicate<PreviousAlternatePlanReleaseEvent, Date> release,
            BiPredicate<PreviousAlternatePlanReleaseEvent, DayScheduleContext> attempt) {
        boolean quotaDelayed = false;
        for (LhShiftConfigVO shift : context.getScheduleWindowShifts()) {
            if ((!shift.isMorningShift() && !shift.isAfternoonShift())
                    || !shift.getShiftEndDateTime().after(earliest)) {
                continue;
            }
            if (!shift.getShiftStartDateTime().before(event.getPlannedShiftEndTime()) && !quotaDelayed) {
                break;
            }
            Date cutoff = earliest.after(shift.getShiftStartDateTime()) ? earliest : shift.getShiftStartDateTime();
            if (this.isQuotaFull(context, shift)) {
                quotaDelayed = true;
                this.trace(context, event, cutoff, "额度不足，继续后续允许班次");
                continue;
            }
            if (LhScheduleTimeUtil.isNoMouldChangeTime(context, cutoff)) {
                continue;
            }
            if (this.trySlot(context, event, shift, cutoff, release, attempt)) {
                return true;
            }
            if (!quotaDelayed) {
                // 原指定区间内仍可继续尝试；其他资源失败不能单独获得越过历史截止的资格。
                this.trace(context, event, cutoff, "完整提案未通过，已恢复原生产和资源");
            }
        }
        this.trace(context, event, earliest, "本窗口无合法完整承接，保留原安排");
        return false;
    }

    /**
     * 在单次完整快照内执行前料调整与指定后料，拒绝及异常均恢复。
     *
     * @param context 正式上下文
     * @param event 精确历史事件
     * @param shift 本次允许的准备班次
     * @param cutoff 前料交接时刻
     * @param release 前料调整适配器
     * @param attempt 后料提交适配器
     * @return 完整提交成功时为真
     */
    private boolean trySlot(LhScheduleContext context, PreviousAlternatePlanReleaseEvent event, LhShiftConfigVO shift,
            Date cutoff, BiPredicate<PreviousAlternatePlanReleaseEvent, Date> release,
            BiPredicate<PreviousAlternatePlanReleaseEvent, DayScheduleContext> attempt) {
        List<SkuScheduleDTO> sources = new ArrayList<>(context.getContinuousSkuList());
        sources.addAll(context.getNewSpecSkuList());
        sources.addAll(context.getPreviousAlternateNewSpecCandidates());
        TimedMachineOffPreviewSnapshot snapshot = new TimedMachineOffPreviewSnapshot(context, sources);
        PreviousAlternatePlanReleaseEvent original = BeanUtil.copyProperties(event, PreviousAlternatePlanReleaseEvent.class);
        boolean accepted = false;
        int firstAttemptLog = context.getScheduleLogList().size();
        String rejection = "前料释放前置条件不满足";
        try {
            event.setExtensionPreparationEndTime(shift.getShiftEndDateTime());
            if (release.test(event, cutoff)) {
                DayScheduleContext day = new DayScheduleContext(shift.getWorkDate().toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                        context.getScheduleWindowShifts().stream()
                                .filter(item -> LhScheduleTimeUtil.isSameDay(item.getWorkDate(), shift.getWorkDate()))
                                .collect(Collectors.toList()),
                        LhScheduleTimeUtil.isSameDay(shift.getWorkDate(),
                                context.getScheduleWindowShifts().get(0).getWorkDate()),
                        LhScheduleTimeUtil.isSameDay(shift.getWorkDate(), context.getScheduleWindowShifts()
                                .get(context.getScheduleWindowShifts().size() - 1).getWorkDate()));
                accepted = attempt.test(event, day) && this.hasSubmittedResult(context, event, cutoff);
                if (!accepted) {
                    rejection = this.lastRejection(context, firstAttemptLog);
                }
            }
        } finally {
            if (!accepted) {
                snapshot.restore(context);
                BeanUtil.copyProperties(original, event);
            }
        }
        if (accepted) {
            this.trace(context, event, cutoff, "指定后料已正式提交，前料真实剩余继续参与新增排产");
        } else {
            this.trace(context, event, cutoff, "已恢复本次尝试，原因=" + rejection);
        }
        return accepted;
    }

    /**
     * 在外层回滚前提取本次内核拒绝诊断，只用于日志，不参与候选和资源决策。
     *
     * @param context 本次内核尝试后的上下文
     * @param firstAttemptLog 本次开始前日志条数
     * @return 本次最后一次明确拒绝，或无有效提交的诊断
     */
    private String lastRejection(LhScheduleContext context, int firstAttemptLog) {
        for (int index = context.getScheduleLogList().size() - 1; index >= firstAttemptLog; index--) {
            if ("前日交替指定承接".equals(context.getScheduleLogList().get(index).getTitle())) {
                return context.getScheduleLogList().get(index).getLogDetail();
            }
        }
        return "指定后料未形成精确关联的窗口内正量结果";
    }

    /**
     * 正量结果与精确历史来源同时成立才算成功，防止零量或其他后料冒充兑现。
     *
     * @param context 单次提交后的结果
     * @param event 本次精确事件
     * @param earliest 本次允许的最早交接
     * @return 原计划在本窗口形成合法正量时为真
     */
    private boolean hasSubmittedResult(LhScheduleContext context, PreviousAlternatePlanReleaseEvent event, Date earliest) {
        Date windowEnd = context.getScheduleWindowShifts().get(context.getScheduleWindowShifts().size() - 1)
                .getShiftEndDateTime();
        Set<LhScheduleResult> live = Collections.newSetFromMap(new IdentityHashMap<>());
        live.addAll(context.getScheduleResultList());
        return context.getPreviousAlternateResultPlanMap().entrySet().stream()
                .filter(entry -> Objects.equals(entry.getValue().getId(), event.getPlan().getId()))
                .map(Map.Entry::getKey).filter(live::contains)
                .filter(result -> Objects.equals(result.getLhMachineCode(), event.getMachineCode())
                        && context.matchesPreviousAlternateAfterMaterial(event.getPlan(),
                        result.getMaterialCode(), result.getProductStatus()))
                .anyMatch(result -> ShiftFieldUtil.resolveScheduledQty(result) > 0
                        && Objects.nonNull(result.getMouldChangeStartTime())
                        && !result.getMouldChangeStartTime().before(earliest)
                        && result.getMouldChangeStartTime().before(event.getPreparationDeadline())
                        && Objects.nonNull(result.getSpecEndTime()) && !result.getSpecEndTime().after(windowEnd));
    }

    /**
     * 读取与正式分配器相同的实时合并次数及有效限额，不登记占用。
     *
     * @param context 实时换模账本
     * @param shift 待尝试早班或中班
     * @return 班次或每日额度已满时为真
     */
    boolean isQuotaFull(LhScheduleContext context, LhShiftConfigVO shift) {
        String dateKey = LhScheduleTimeUtil.formatDate(shift.getWorkDate());
        int[] counts = context.getDailyMouldChangeCountMap().getOrDefault(dateKey, new int[]{0, 0});
        int used = shift.isMorningShift() ? counts[0] : counts[1];
        int limit = shift.isMorningShift() ? LhScheduleTimeUtil.getMorningMouldChangeLimit(context, dateKey)
                : LhScheduleTimeUtil.getAfternoonMouldChangeLimit(context, dateKey);
        return used >= limit || counts[0] + counts[1] >= LhScheduleTimeUtil.getDailyMouldChangeLimit(context, dateKey);
    }

    /**
     * 记录扩展决策及精确源计划，原流程日志保持原口径。
     *
     * @param context 当前工厂及批次
     * @param event 精确历史事件
     * @param time 本次交接候选时刻
     * @param reason 决策结果
     */
    private void trace(LhScheduleContext context, PreviousAlternatePlanReleaseEvent event, Date time, String reason) {
        PriorityTraceLogHelper.appendProcessLog(context, "历史指定交接扩展",
                String.format("工厂=%s, 批次=%s, 计划ID=%s, 机台=%s, 前料=%s, 后料=%s, 状态=%s, 候选交接=%s, 结论=%s",
                        context.getFactoryCode(), context.getBatchNo(), event.getPlan().getId(), event.getMachineCode(),
                        event.getPlan().getBeforeMaterialCode(), event.getPlan().getAfterMaterialCode(),
                        event.getPlan().getProductStatus(), LhScheduleTimeUtil.formatDateTime(time), reason));
    }
}
