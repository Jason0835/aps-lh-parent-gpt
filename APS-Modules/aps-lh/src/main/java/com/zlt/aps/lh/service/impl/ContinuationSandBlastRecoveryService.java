package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.DailyCandidateReason;
import com.zlt.aps.lh.engine.strategy.support.DailyNewSpecCandidate;
import com.zlt.aps.lh.engine.strategy.support.DayDrivenScheduleState;
import com.zlt.aps.lh.engine.strategy.support.DayScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.NewSpecEmbryoAvailableTimeResolver;
import com.zlt.aps.lh.util.LhMouldCodeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Objects;

/**
 * 管理仅喷砂来源的原机同料任务，覆盖原模恢复与备用模具置换。
 * 来源窗口、待恢复标识进入清洗运行态快照，数量和跨日绑定沿用现有共享账本。
 *
 * @author APS
 * @since 2026/11/06
 */
@Slf4j
public class ContinuationSandBlastRecoveryService {

    /**
     * 识别明确绑定喷砂下机来源的恢复候选。
     *
     * @param sku 候选物料
     * @return 是否属于专用恢复入口
     */
    public static boolean isRecoveryCandidate(SkuScheduleDTO sku) {
        return Objects.nonNull(sku) && StringUtils.isNotBlank(sku.getOnlySandBlastRequeueSourceMachineCode());
    }

    /**
     * 限定原运行侧，只有原整机粒度候选可携带同来源配对侧一起执行。
     *
     * @param context 本批来源和机台粒度
     * @param sku 恢复候选
     * @param machineCode 拟分配运行侧
     * @return 是否满足原机及原侧约束
     */
    public static boolean allowsMachine(LhScheduleContext context, SkuScheduleDTO sku, String machineCode) {
        String sourceMachine = sku.getOnlySandBlastRequeueSourceMachineCode();
        return (StringUtils.equals(sourceMachine, machineCode)
                || LhSingleControlMachineUtil.isWholeMachineGranularitySku(context, sku))
                && !resolveOriginalMouldCodes(context, sourceMachine, machineCode).isEmpty();
    }

    /**
     * 登记已实际下机且具有剩余需求的恢复候选，以完整来源运行侧幂等登记。
     *
     * @param context 已保存原机、原模及喷砂时间的上下文
     * @param sku 与续作共享数量账本的候选
     */
    public void register(LhScheduleContext context, SkuScheduleDTO sku) {
        String sourceMachine = sku.getOnlySandBlastRequeueSourceMachineCode();
        MachineCleaningWindowDTO event = context.getOnlySandBlastContinuationReleaseWindowMap().get(sourceMachine);
        sku.setFirstAddMachineProductionDate(this.resolveEarliestDate(context, event));
        context.getCleaningPlanRuntimeState().getPendingRecoveryMachineCodes().add(sourceMachine);
        log.info("仅喷砂恢复登记, factoryCode: {}, batchNo: {}, materialCode: {}, productStatus: {}, "
                        + "sourceMachineCode: {}, sourcePlanId: {}, originalMouldCodes: {}, cleanStart: {}, cleanEnd: {}, poolDate: {}",
                context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(), sku.getProductStatus(),
                sourceMachine, event.getSourcePlanId(), event.getMouldCode(), event.getCleanStartTime(),
                event.getCleanEndTime(), sku.getFirstAddMachineProductionDate());
    }

    /**
     * 以T+1首班、喷砂结束及原机真实释放的较晚时点确定最早业务日。
     * 具体换模时段和额度由正式时间轴计算。
     *
     * @param context 当前窗口及原机运行态
     * @param event 已实际下机的来源事件
     * @return 最早可尝试恢复的班次业务日，窗口外保留实际日期供未排收口
     */
    public LocalDate resolveEarliestDate(LhScheduleContext context, MachineCleaningWindowDTO event) {
        Date readyTime = context.resolveOnlySandBlastRecoveryNotBeforeTime(event);
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(event.getLhCode());
        if (Objects.nonNull(machine) && Objects.nonNull(machine.getEstimatedEndTime())
                && machine.getEstimatedEndTime().after(readyTime)) {
            readyTime = machine.getEstimatedEndTime();
        }
        LhShiftConfigVO shift = NewSpecEmbryoAvailableTimeResolver.resolveProductionShift(
                context.getScheduleWindowShifts(), readyTime);
        Date businessDate = Objects.nonNull(shift) ? shift.getWorkDate() : readyTime;
        return businessDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * 为待恢复来源建立单次原机机会，使用现有候选运行态及实时数量视图。
     *
     * @param context 来源事件及待恢复状态
     * @param day 当前业务日
     * @param state 日驱动绑定和候选状态
     * @param sku 恢复候选
     * @param remainingQty 共享生产账本的实时余量
     * @return 当前日可尝试的恢复候选，已完成、失效或尚未到日期时为空
     */
    public DailyNewSpecCandidate buildCandidate(LhScheduleContext context, DayScheduleContext day,
            DayDrivenScheduleState state, SkuScheduleDTO sku, int remainingQty) {
        String sourceMachine = sku.getOnlySandBlastRequeueSourceMachineCode();
        if (!context.getCleaningPlanRuntimeState().getPendingRecoveryMachineCodes().contains(sourceMachine)) {
            // 整机另一侧已经随主侧恢复时结束独立待排副本，实际跨日绑定继续由主侧承接。
            if (!state.hasActiveBinding(sku)) {
                state.complete(sku);
            }
            return null;
        }
        if (remainingQty <= 0 || context.resolveOnlySandBlastRequeueEvents(sku).isEmpty()) {
            context.getCleaningPlanRuntimeState().getPendingRecoveryMachineCodes().remove(sourceMachine);
            state.complete(sku);
            return null;
        }
        MachineCleaningWindowDTO event = context.getOnlySandBlastContinuationReleaseWindowMap().get(sourceMachine);
        if (this.resolveEarliestDate(context, event).isAfter(day.getScheduleDate()) || state.hasActiveBinding(sku)) {
            return null;
        }
        DailyNewSpecCandidate candidate = new DailyNewSpecCandidate(
                MonthPlanDateResolver.buildMaterialStatusKey(sku.getMaterialCode(), sku.getProductStatus()),
                sku, state.resolveCandidateRuntimeState(sku));
        candidate.addReason(DailyCandidateReason.ADD_MACHINE_REQUIREMENT);
        // 每次延期按当前实际尝试日期重新归池，不能继续占用已经过去的喷砂开始日期池。
        candidate.setTargetPlanDate(day.getScheduleDate());
        candidate.setPoolDate(day.getScheduleDate());
        candidate.setRemainingMachineCount(1);
        candidate.setTargetMachineCount(1);
        return candidate;
    }

    /**
     * 解析目标运行侧对应的精确原模，整机配对侧必须属于同一喷砂来源。
     *
     * @param context 来源事件
     * @param sourceMachine 恢复来源运行侧
     * @param targetMachine 本次实际分配运行侧
     * @return 当前侧原模组，跨物理机或来源不一致时为空
     */
    public static List<String> resolveOriginalMouldCodes(LhScheduleContext context,
            String sourceMachine, String targetMachine) {
        MachineCleaningWindowDTO source = context.getOnlySandBlastContinuationReleaseWindowMap().get(sourceMachine);
        MachineCleaningWindowDTO target = context.getOnlySandBlastContinuationReleaseWindowMap().get(targetMachine);
        if (Objects.isNull(source) || Objects.isNull(target)
                || !StringUtils.equals(LhSingleControlMachineUtil.resolvePhysicalMachineCode(sourceMachine),
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(targetMachine))
                || !Objects.equals(source.getSourcePlanId(), target.getSourcePlanId())
                || !Objects.equals(source.getCleanStartTime(), target.getCleanStartTime())
                || !Objects.equals(source.getCleanEndTime(), target.getCleanEndTime())
                || !StringUtils.equals(source.getContinuationMaterialCode(), target.getContinuationMaterialCode())
                || !StringUtils.equals(source.getContinuationProductStatus(), target.getContinuationProductStatus())) {
            return Collections.emptyList();
        }
        return new ArrayList<>(LhMouldCodeUtil.splitMouldCode(target.getMouldCode()));
    }

    /**
     * 解析同源运行侧本次应使用的精确模具，置换读取冻结备用组，恢复读取原组。
     *
     * @param context 当前来源事件
     * @param sourceMachine 候选来源机台
     * @param targetMachine 实际运行侧
     * @return 合法同源侧的目标组，来源不一致时为空
     */
    public static List<String> resolveRecoveryMouldCodes(LhScheduleContext context,
            String sourceMachine, String targetMachine) {
        List<String> originals = resolveOriginalMouldCodes(context, sourceMachine, targetMachine);
        if (originals.isEmpty()) {
            return originals;
        }
        MachineCleaningWindowDTO event = context.getOnlySandBlastContinuationReleaseWindowMap().get(targetMachine);
        if (StringUtils.isNotBlank(event.getDeferredReplacementMouldCode())) {
            return new ArrayList<>(LhMouldCodeUtil.splitMouldCode(event.getDeferredReplacementMouldCode()));
        }
        return originals;
    }

    /**
     * 待置换原机保留给同源任务，成功提交或需求归零后解除预留。
     *
     * @param context 当前清洗运行态
     * @param sku 拟使用机台的候选
     * @param machineCode 目标运行侧
     * @return 当前候选是否被待置换来源阻挡
     */
    public static boolean isReplacementMachineReserved(LhScheduleContext context,
            SkuScheduleDTO sku, String machineCode) {
        for (String source : context.getCleaningPlanRuntimeState().getPendingRecoveryMachineCodes()) {
            MachineCleaningWindowDTO event = context.getOnlySandBlastContinuationReleaseWindowMap().get(source);
            if (event != null && StringUtils.isNotBlank(event.getDeferredReplacementMouldCode())
                    && StringUtils.equals(LhSingleControlMachineUtil.resolvePhysicalMachineCode(source),
                            LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode))
                    && (!isRecoveryCandidate(sku) || !allowsMachine(context, sku, machineCode))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断模具是否仍由其他待恢复来源保留，通用候选不得抢占对应原模。
     *
     * @param context 待恢复来源及当前恢复作用域
     * @param mouldCode 拟使用模具
     * @return 当前调用无权领取该待恢复原模时返回true
     */
    public static boolean isReservedMould(LhScheduleContext context, String mouldCode) {
        String activeSource = context.getCleaningPlanRuntimeState().getActiveRecoverySourceMachineCode();
        for (String sourceMachine : context.getCleaningPlanRuntimeState().getPendingRecoveryMachineCodes()) {
            MachineCleaningWindowDTO event = context.getOnlySandBlastContinuationReleaseWindowMap().get(sourceMachine);
            if (Objects.nonNull(event)
                    && (LhMouldCodeUtil.splitMouldCode(event.getMouldCode()).contains(mouldCode)
                    || LhMouldCodeUtil.splitMouldCode(event.getDeferredReplacementMouldCode()).contains(mouldCode))
                    && (StringUtils.isBlank(activeSource)
                    || resolveOriginalMouldCodes(context, activeSource, sourceMachine).isEmpty())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 依据正式正量结果清理已恢复标识，配对两侧分别核对原模和换模时间。
     * 来源事件保留，后续原模资源重建和最终班次原因继续引用同一事实。
     *
     * @param context 原子提交成功后的上下文
     */
    public void completeCommittedRecoveries(LhScheduleContext context) {
        context.getCleaningPlanRuntimeState().getPendingRecoveryMachineCodes().removeIf(machineCode -> {
            MachineCleaningWindowDTO event = context.getOnlySandBlastContinuationReleaseWindowMap().get(machineCode);
            LhScheduleResult committed = context.getScheduleResultList().stream()
                    .filter(result -> this.matchesRecoveryResult(context, event, machineCode, result))
                    .findFirst().orElse(null);
            boolean restored = Objects.nonNull(committed);
            if (restored && StringUtils.isNotBlank(event.getDeferredReplacementMouldCode())) {
                // 备用模具接替后原组按实际下模时刻释放，输出继续读取原来源与实际新组。
                for (String mouldCode : LhMouldCodeUtil.splitMouldCode(event.getMouldCode())) {
                    context.getPreScheduledMouldReleaseTimeMap().merge(mouldCode,
                            committed.getMouldChangeStartTime(), (previous, current) ->
                                    previous.after(current) ? previous : current);
                }
            }
            if (restored) {
                log.info("仅喷砂恢复成功, factoryCode: {}, batchNo: {}, sourceMachineCode: {}, "
                                + "materialCode: {}, originalMouldCodes: {}, pending: false",
                        context.getFactoryCode(), context.getBatchNo(), machineCode,
                        event.getContinuationMaterialCode(), event.getMouldCode());
            }
            return restored;
        });
    }

    /**
     * 核对同料任务的原机、精确目标模具、实际换模下限及正量事实。
     *
     * @param context 当前业务日窗口及来源资源
     * @param event 来源喷砂事件
     * @param machineCode 原运行侧
     * @param result 已提交结果
     * @return 结果是否完整证明该来源已恢复
     */
    private boolean matchesRecoveryResult(LhScheduleContext context, MachineCleaningWindowDTO event, String machineCode,
            LhScheduleResult result) {
        return StringUtils.equals(machineCode, result.getLhMachineCode())
                && StringUtils.equals(event.getContinuationMaterialCode(), result.getMaterialCode())
                && StringUtils.equals(event.getContinuationProductStatus(), result.getProductStatus())
                && Objects.nonNull(result.getMouldChangeStartTime())
                && !result.getMouldChangeStartTime().before(StringUtils.isNotBlank(event.getDeferredReplacementMouldCode())
                        ? context.resolveOnlySandBlastRecoveryNotBeforeTime(event) : event.getCleanEndTime())
                && new LinkedHashSet<>(resolveRecoveryMouldCodes(context, machineCode, machineCode)).equals(
                        LhMouldCodeUtil.splitMouldCode(result.getMouldCode()))
                && ShiftFieldUtil.resolveScheduledQty(result) > 0;
    }
}
