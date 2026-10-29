package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.component.StructureEarlyProductionAdmission;
import com.zlt.aps.lh.component.StructureMinMachineRetentionService;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.DailySchedulePhase;
import com.zlt.aps.lh.engine.strategy.support.DayScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.PreviousAlternatePlanReleaseEvent;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

/**
 * 为最终零产量的历史前料尝试较早早班承接，完整提案成功后才改变释放事实。
 *
 * @author APS
 * @date 2026/10/24
 */
@Service
public class PreviousAlternateZeroProductionAdvanceService {

    /**
     * 复用已有早中班和每日合并交替额度的只读检查。
     */
    @Resource
    private PreviousAlternateHandoffExtensionService handoffExtensionService;

    /**
     * 按原结构物理机台口径复核新增早班占用，不授予历史超结构上限例外。
     */
    @Resource
    private StructureMinMachineRetentionService structureMinMachineRetentionService;

    /**
     * 在单次完整快照中执行提前释放及原指定承接，拒绝或异常恢复原历史安排。
     *
     * @param context 已完成续作最终扣账的上下文
     * @param preparationDay 当前准备业务日
     * @param event 本机精确历史交替事件
     * @param attempt 原指定机台跨生产日承接内核，不修改外层已完成集合
     * @return 较早早班形成精确正量并通过原资源约束及结构上限时为真
     */
    public boolean tryAdvance(LhScheduleContext context, DayScheduleContext preparationDay,
            PreviousAlternatePlanReleaseEvent event,
            BiPredicate<PreviousAlternatePlanReleaseEvent, DayScheduleContext> attempt) {
        if (!context.isContinuousDailyQuotaSynced() || !event.isBeforeMaterialNotScheduled()
                || Objects.isNull(event.getOfflineTime())) {
            return false;
        }
        LhShiftConfigVO shift = this.resolveEarlierMorning(context, preparationDay, event);
        if (Objects.isNull(shift) || handoffExtensionService.isQuotaFull(context, shift)
                || !this.isEligible(context, event)) {
            return false;
        }
        if (Stream.concat(context.getPreviousAlternateNewSpecCandidates().stream(),
                context.getContinuousSkuList().stream())
                .noneMatch(source -> context.matchesPreviousAlternateAfterMaterial(
                        event.getPlan(), source.getMaterialCode(), source.getProductStatus()))) {
            return false;
        }
        Date readyTime = ShiftCapacityResolverUtil.resolveCapsuleReplacementReadyTime(
                context, event.getMachineCode(), shift.getShiftStartDateTime());
        if (!readyTime.before(shift.getShiftEndDateTime())) {
            return false;
        }
        List<SkuScheduleDTO> sources = new ArrayList<>(context.getContinuousSkuList());
        sources.addAll(context.getNewSpecSkuList());
        sources.addAll(context.getPreviousAlternateNewSpecCandidates());
        // 廉价资格和配额检查通过后才捕获一次完整快照；原内核负责所有资源和需求决策。
        long attemptStartNanos = System.nanoTime();
        TimedMachineOffPreviewSnapshot snapshot = new TimedMachineOffPreviewSnapshot(context, sources);
        Date originalRelease = event.getOfflineTime();
        Date originalBusinessDate = context.getCurrentScheduleDate();
        DailySchedulePhase originalPhase = preparationDay.getCurrentPhase();
        int firstAttemptLog = context.getScheduleLogList().size();
        boolean accepted = false;
        String reason = "提前承接异常，已恢复原历史安排";
        try {
            this.publishRelease(context, event, readyTime);
            if (!attempt.test(event, preparationDay)) {
                reason = this.resolveRejection(context, firstAttemptLog);
                return false;
            }
            accepted = this.hasEarlierPositiveResult(context, event, shift, readyTime, originalRelease);
            reason = accepted ? "较早早班正量及完整资源校验通过" : "较早早班无精确正量或结构机台上限复核未通过";
            return accepted;
        } finally {
            if (!accepted) {
                snapshot.restore(context);
            }
            context.setCurrentScheduleDate(originalBusinessDate);
            preparationDay.setCurrentPhase(originalPhase);
            this.trace(context, event, readyTime, originalRelease, accepted, reason,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - attemptStartNanos));
        }
    }

    /**
     * 只允许最终无生产且没有独立释放决定的真实前料进入提前承接。
     *
     * @param context 当前续作、收尾和机台状态
     * @param event 待核对的精确历史事件
     * @return 可继续检查较早准备班次时为真
     */
    private boolean isEligible(LhScheduleContext context, PreviousAlternatePlanReleaseEvent event) {
        if (event.resolveProductionRole()
                != PreviousAlternatePlanReleaseEvent.ProductionRole.BEFORE_MATERIAL_PRODUCTION
                || event.isSupplementalReleasePending() || event.isSharedEmbryoEndingPending()
                || event.isRemainderFinishDeferred() || Objects.nonNull(event.getCompletedStrictEndingReleaseTime())
                || Objects.nonNull(event.getCompletedEmbryoEndingWindowEndTime())
                || Objects.nonNull(event.getForcedHandoffReleaseTime())
                || Objects.nonNull(event.getConfirmedTimedHandoffWindowEndTime())) {
            return false;
        }
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(event.getMachineCode());
        if (Objects.isNull(machine) || Objects.isNull(event.getNoProductionStartTime())
                || !event.getOfflineTime().equals(event.getNoProductionStartTime())
                || (Objects.nonNull(machine.getEstimatedEndTime())
                && machine.getEstimatedEndTime().after(event.getOfflineTime()))) {
            return false;
        }
        String physicalMachine = LhSingleControlMachineUtil.resolvePhysicalMachineCode(event.getMachineCode());
        // L/R按整机检查；任何正量、停产保机、时间下机请求或06故障迁移都保留原释放流程。
        return context.getMachineScheduleMap().keySet().stream()
                .filter(code -> StringUtils.equals(physicalMachine,
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(code)))
                .noneMatch(code -> context.isContinuousStopHoldMachine(code)
                        || Objects.nonNull(context.getTimedMachineOffBoundary(code)))
                && context.getScheduleResultList().stream()
                .noneMatch(result -> StringUtils.equals(physicalMachine,
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode()))
                        && ShiftFieldUtil.resolveScheduledQty(result) > 0)
                && context.getTimedMachineOffRequests().stream()
                .noneMatch(request -> request.getProfile().getOriginals().stream()
                        .anyMatch(result -> StringUtils.equals(physicalMachine,
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode()))))
                && context.getContinuationTemporaryFaultTransferEventMap().values().stream()
                .noneMatch(transfer -> StringUtils.equals(physicalMachine, transfer.getOriginalPhysicalMachineCode())
                        || StringUtils.equals(physicalMachine,
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(transfer.getTargetMachineCode())))
                && context.getTemporaryFaultWindowMap().values().stream()
                .noneMatch(window -> StringUtils.equals(physicalMachine,
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(window.getMachineCode())));
    }

    /**
     * 仅选择当前准备日早于原释放边界的早班，不扫描其他业务日重新安排历史动作。
     *
     * @param context 固定排程窗口
     * @param preparationDay 当前准备业务日
     * @param event 保留原释放和历史截止的事件
     * @return 可提前尝试的早班；没有机会时返回空
     */
    private LhShiftConfigVO resolveEarlierMorning(LhScheduleContext context, DayScheduleContext preparationDay,
            PreviousAlternatePlanReleaseEvent event) {
        return context.getScheduleWindowShifts().stream()
                .filter(LhShiftConfigVO::isMorningShift)
                .filter(shift -> preparationDay.getScheduleDate().equals(shift.getWorkDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate()))
                .filter(shift -> !shift.getShiftEndDateTime().after(event.getOfflineTime()))
                .filter(shift -> shift.getShiftStartDateTime().before(event.getPreparationDeadline()))
                .findFirst().orElse(null);
    }

    /**
     * 在尝试快照内同步机台、历史事件和实际下模模具的释放时刻，不扣减生产数量。
     *
     * @param context 本次尝试使用的资源账本
     * @param event 原历史事件，失败时随完整快照恢复
     * @param readyTime 较早早班内经过胶囊占用校验的就绪时间
     */
    private void publishRelease(LhScheduleContext context, PreviousAlternatePlanReleaseEvent event, Date readyTime) {
        event.setOfflineTime(readyTime);
        event.setNoProductionStartTime(readyTime);
        context.getPreviousAlternateReleasedMachineTimeMap().put(event.getMachineCode(), readyTime);
        context.getPreviousAlternateReleasedSkuKeyMap().put(event.getMachineCode(),
                MonthPlanDateResolver.buildMaterialStatusKey(event.getMaterialCode(), event.getProductStatus()));
        event.getOriginalMouldCodes().stream()
                .filter(mould -> !event.getRetainedMouldCodes().contains(mould))
                .forEach(mould -> context.getPreScheduledMouldReleaseTimeMap().put(mould, readyTime));
        context.getMachineScheduleMap().get(event.getMachineCode()).setEstimatedEndTime(readyTime);
        // 原模具资源上下文已缓存历史释放，重建后才能按本次真实时刻取得完整模具组。
        context.setMouldResourceContext(null);
    }

    /**
     * 精确历史来源、较早班次正量及结构上限同时成立才接受本次完整提交。
     *
     * @param context 原承接内核提交后的上下文
     * @param event 本次精确历史事件
     * @param shift 较早早班
     * @param readyTime 本次允许的最早释放
     * @param originalRelease 原历史释放边界
     * @return 后料在较早早班有真实正量且结构未超限时为真
     */
    private boolean hasEarlierPositiveResult(LhScheduleContext context, PreviousAlternatePlanReleaseEvent event,
            LhShiftConfigVO shift, Date readyTime, Date originalRelease) {
        return context.getPreviousAlternateResultPlanMap().entrySet().stream()
                .filter(entry -> entry.getValue() == event.getPlan())
                .map(Map.Entry::getKey)
                .filter(result -> StringUtils.equals(event.getMachineCode(), result.getLhMachineCode())
                        && StringUtils.equals(event.getPlan().getAfterMaterialCode(), result.getMaterialCode())
                        && StringUtils.equals(event.getPlan().getProductStatus(), result.getProductStatus()))
                .filter(result -> Objects.nonNull(result.getMouldChangeStartTime())
                        && !result.getMouldChangeStartTime().before(readyTime)
                        && result.getMouldChangeStartTime().before(originalRelease))
                .anyMatch(result -> this.hasPositiveQuantityWithinLimit(context, result, shift));
    }

    /**
     * 复核实际早班量和时间，并按公共物理机台统计确认结构额度。
     *
     * @param context 已提交结果及结构在机索引
     * @param result 精确指定后料结果
     * @param shift 较早早班
     * @return 早班有正量且结构额度充足时为真
     */
    private boolean hasPositiveQuantityWithinLimit(LhScheduleContext context, LhScheduleResult result,
            LhShiftConfigVO shift) {
        Integer quantity = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
        Date start = ShiftFieldUtil.getShiftStartTime(result, shift.getShiftIndex());
        Date end = ShiftFieldUtil.getShiftEndTime(result, shift.getShiftIndex());
        if (Objects.isNull(quantity) || quantity <= 0 || Objects.isNull(start) || Objects.isNull(end)
                || start.before(shift.getShiftStartDateTime()) || !start.before(end)
                || end.after(shift.getShiftEndDateTime())) {
            return false;
        }
        int limit = context.getStructurePlanMachineCount(shift.getWorkDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate(), result.getStructureName());
        StructureEarlyProductionAdmission statistics = structureMinMachineRetentionService
                .resolveEffectiveStructureMachineStatistics(context, result.getStructureName(), shift);
        String physicalMachine = LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode());
        int newMachineDelta = statistics.getScheduledPhysicalMachineCodes().contains(physicalMachine) ? 0 : 1;
        return limit > 0 && statistics.getScheduledPhysicalMachineCodes().size() + newMachineDelta <= limit;
    }

    /**
     * 恢复前读取本次内核最后一条指定承接拒绝，避免复制被撤销的生产过程日志。
     *
     * @param context 本次尝试后的过程日志
     * @param firstAttemptLog 本次尝试开始前的日志数
     * @return 最后一条明确拒绝，缺少拒绝记录时返回内核未提交说明
     */
    private String resolveRejection(LhScheduleContext context, int firstAttemptLog) {
        for (int index = context.getScheduleLogList().size() - 1; index >= firstAttemptLog; index--) {
            if ("前日交替指定承接".equals(context.getScheduleLogList().get(index).getTitle())) {
                return context.getScheduleLogList().get(index).getLogDetail();
            }
        }
        return "原指定承接内核未形成有效提交";
    }

    /**
     * 恢复或提交后记录独立尝试结论，不保留失败提案的正式生产证据。
     *
     * @param context 当前工厂及批次
     * @param event 原历史事件
     * @param readyTime 本次尝试起点
     * @param originalRelease 原历史释放边界
     * @param accepted 是否接受较早完整承接
     * @param reason 正式成功或恢复前提取的拒绝说明
     * @param elapsedMillis 含快照捕获及必要恢复的实际耗时毫秒
     */
    private void trace(LhScheduleContext context, PreviousAlternatePlanReleaseEvent event, Date readyTime,
            Date originalRelease, boolean accepted, String reason, long elapsedMillis) {
        PriorityTraceLogHelper.appendProcessLog(context, "零产量前料提前承接",
                String.format("工厂=%s, 批次=%s, 计划ID=%s, 机台=%s, 前料=%s, 后料=%s, 状态=%s, "
                                + "原释放=%s, 尝试释放=%s, 成功=%s, 阶段=%s, 耗时毫秒=%s, 原因=%s",
                        context.getFactoryCode(), context.getBatchNo(), event.getPlan().getId(),
                        event.getMachineCode(), event.getMaterialCode(), event.getPlan().getAfterMaterialCode(),
                        event.getPlan().getProductStatus(), LhScheduleTimeUtil.formatDateTime(originalRelease),
                        LhScheduleTimeUtil.formatDateTime(readyTime), accepted,
                        context.isPreviousAlternateGroupPreview() ? "关联预演" : "正式提交", elapsedMillis, reason));
    }
}
