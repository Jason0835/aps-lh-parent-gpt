package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuDailyPlanQuotaDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.component.CapsuleReplacementRuleService;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.component.StructureEndingAlignmentService;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.factory.ScheduleStrategyFactory;
import com.zlt.aps.lh.engine.strategy.IFirstInspectionBalanceStrategy;
import com.zlt.aps.lh.engine.strategy.IMachineMatchStrategy;
import com.zlt.aps.lh.engine.strategy.IMouldChangeBalanceStrategy;
import com.zlt.aps.lh.engine.strategy.ISkuPriorityStrategy;
import com.zlt.aps.lh.engine.strategy.support.ActiveMachineBinding;
import com.zlt.aps.lh.engine.strategy.support.DailyNewSpecCandidate;
import com.zlt.aps.lh.engine.strategy.support.DailySchedulePhase;
import com.zlt.aps.lh.engine.strategy.support.DayDrivenScheduleState;
import com.zlt.aps.lh.engine.strategy.support.DayScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.DeferredScheduleTask;
import com.zlt.aps.lh.engine.strategy.support.MachineSkuMatchResult;
import com.zlt.aps.lh.engine.strategy.support.MouldResourceContext;
import com.zlt.aps.lh.engine.strategy.support.ScheduleSubstitutionDirective;
import com.zlt.aps.lh.util.FirstInspectionQtyUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.lh.util.SkuDailyPlanQuotaUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 特殊 SKU 对当前业务日新增排产结果执行原子置换。
 *
 * <p>置换单位是新增排产形成的 {@link ActiveMachineBinding}，不是整台机台时间轴。机台在
 * 新增结果之前已经形成的续作结果只用于确定释放边界，续作班次数量、收尾标识和结束时间均不修改。</p>
 *
 * <p>性能约束：每个业务日只从日驱动绑定构建一次轻量候选列表，不生成 Machine×SKU 笛卡尔积；
 * 单候选先执行只读硬匹配，只有真正提交时才捕获一个完整运行态快照，候选结束后立即释放引用，
 * 禁止缓存多个深快照导致大批量排程内存放大。</p>
 *
 * @author APS
 */
@Slf4j
@Service
public class NewSpecResultSubstitutionCoordinator {

    /** 被普通新增占用机台改由特殊 SKU 接管后的延期原因。 */
    private static final String REPLACED_NEW_SPEC_DEFER_REASON =
            "新增排产机台被更高优先级特殊SKU置换，恢复待排并顺延后续业务日";

    @Resource
    private ScheduleStrategyFactory strategyFactory;
    @Resource
    private SpecifiedNewSpecSchedulingService specifiedNewSpecSchedulingService;
    @Resource
    private TargetScheduleQtyResolver targetScheduleQtyResolver;
    @Resource
    private StructureEndingAlignmentService structureEndingAlignmentService;
    @Resource
    private CapsuleReplacementRuleService capsuleReplacementRuleService;
    @Resource
    private ISkuPriorityStrategy skuPriorityStrategy;

    /**
     * 按特殊 SKU 既有顺序执行新增结果置换。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日上下文
     * @param state 三天窗口日驱动状态
     * @param specialCandidates 正常竞争后仍剩余的特殊候选
     */
    public void substitute(LhScheduleContext context,
                           DayScheduleContext dayContext,
                           DayDrivenScheduleState state,
                           List<DailyNewSpecCandidate> specialCandidates) {
        if (Objects.isNull(context) || Objects.isNull(dayContext) || Objects.isNull(state)
                || CollectionUtils.isEmpty(specialCandidates)) {
            return;
        }
        List<ActiveMachineBinding> candidateBindings = this.collectCurrentDayNewSpecBindings(
                context, dayContext, state);
        if (CollectionUtils.isEmpty(candidateBindings)) {
            this.appendStageSummary(context, dayContext, specialCandidates.size(), 0, 0,
                    "当前业务日没有可置换的新增排产占用机台");
            return;
        }
        Set<SkuScheduleDTO> protectedSpecialSkuSet = this.resolveProtectedSpecialSkuSet(
                context, candidateBindings);
        Set<String> consumedPhysicalMachineCodeSet = new LinkedHashSet<String>(
                Math.max(16, candidateBindings.size() * 2));
        int attemptedMachineCount = 0;
        int successMachineCount = 0;
        IMachineMatchStrategy machineMatchStrategy = strategyFactory.getMachineMatchStrategy();
        for (DailyNewSpecCandidate specialCandidate : specialCandidates) {
            if (Objects.isNull(specialCandidate) || Objects.isNull(specialCandidate.getSku())) {
                continue;
            }
            SkuScheduleDTO specialSku = specialCandidate.getSku();
            while (specialCandidate.getRemainingMachineCount() > 0
                    && targetScheduleQtyResolver.resolveProductionRemainingQty(
                    context, specialSku) > 0) {
                ActiveMachineBinding selectedBinding = this.selectBestBinding(
                        context, dayContext, specialSku, candidateBindings,
                        protectedSpecialSkuSet, consumedPhysicalMachineCodeSet,
                        machineMatchStrategy);
                if (Objects.isNull(selectedBinding)) {
                    break;
                }
                attemptedMachineCount++;
                String physicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                        selectedBinding.getMachineCode());
                consumedPhysicalMachineCodeSet.add(physicalMachineCode);
                if (!this.trySubstituteOneBinding(
                        context, dayContext, state, specialCandidate, selectedBinding)) {
                    continue;
                }
                successMachineCount++;
                specialCandidate.consumeMachineOpportunity();
            }
        }
        this.appendStageSummary(context, dayContext, specialCandidates.size(),
                attemptedMachineCount, successMachineCount, "特殊SKU新增结果置换阶段完成");
    }

    /**
     * 一次性收集当前业务日由新增排产新占用的机台绑定。
     *
     * <p>跨日前一日形成的新增绑定已经进入当天延续阶段，不属于“当天新增排产后续占用”，
     * 因而要求新增结果的换模开始或首个正量班次开始时间落在当前业务日窗口内。</p>
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param state 日驱动状态
     * @return 当前日可进入置换筛选的新增绑定
     */
    private List<ActiveMachineBinding> collectCurrentDayNewSpecBindings(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            DayDrivenScheduleState state) {
        List<ActiveMachineBinding> resultList = new ArrayList<ActiveMachineBinding>(16);
        for (ActiveMachineBinding binding : state.getActiveBindings()) {
            if (!this.isCurrentDayNewSpecBinding(context, dayContext, binding)) {
                continue;
            }
            resultList.add(binding);
        }
        resultList.sort(Comparator
                .comparingInt((ActiveMachineBinding binding) -> this.resolveVictimSortRank(binding)).reversed()
                .thenComparing(ActiveMachineBinding::getMachineCode,
                        Comparator.nullsLast(String::compareTo)));
        return resultList;
    }

    private boolean isCurrentDayNewSpecBinding(LhScheduleContext context,
                                               DayScheduleContext dayContext,
                                               ActiveMachineBinding binding) {
        if (Objects.isNull(binding) || Objects.isNull(binding.getScheduleResult())
                || !context.getScheduleResultList().contains(binding.getScheduleResult())
                || !ScheduleTypeEnum.NEW_SPEC.getCode().equals(
                binding.getScheduleResult().getScheduleType())) {
            return false;
        }
        Date switchStartTime = this.resolveBindingSwitchStartTime(dayContext, binding);
        return dayContext.contains(switchStartTime)
                && this.resolveCurrentDayPlanQty(dayContext, binding) > 0;
    }

    private int resolveVictimSortRank(ActiveMachineBinding binding) {
        SkuScheduleDTO sku = Objects.isNull(binding) ? null : binding.getSku();
        return Objects.isNull(sku) || sku.getSortRank() <= 0
                ? Integer.MAX_VALUE : sku.getSortRank();
    }

    private Set<SkuScheduleDTO> resolveProtectedSpecialSkuSet(
            LhScheduleContext context,
            List<ActiveMachineBinding> bindingList) {
        List<SkuScheduleDTO> victimSkuList = new ArrayList<SkuScheduleDTO>(bindingList.size());
        for (ActiveMachineBinding binding : bindingList) {
            if (Objects.nonNull(binding) && Objects.nonNull(binding.getSku())) {
                victimSkuList.add(binding.getSku());
            }
        }
        return skuPriorityStrategy.resolveSpecialNewSpecSkus(context, victimSkuList);
    }

    /**
     * 在不构造候选矩阵的前提下选择当前特殊 SKU 的最佳新增占用绑定。
     *
     * <p>先复用机台反向硬匹配和六层匹配结果，再在相同匹配层级下优先置换排序名次靠后的
     * 普通新增 SKU，最后按机台编码稳定收口。已排成功的其他特殊 SKU 不作为被置换对象。</p>
     */
    private ActiveMachineBinding selectBestBinding(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            SkuScheduleDTO specialSku,
            List<ActiveMachineBinding> candidateBindings,
            Set<SkuScheduleDTO> protectedSpecialSkuSet,
            Set<String> consumedPhysicalMachineCodeSet,
            IMachineMatchStrategy machineMatchStrategy) {
        ActiveMachineBinding bestBinding = null;
        int bestMatchPriority = Integer.MAX_VALUE;
        int bestVictimRank = Integer.MIN_VALUE;
        for (ActiveMachineBinding binding : candidateBindings) {
            if (Objects.isNull(binding) || binding.getSku() == specialSku
                    || protectedSpecialSkuSet.contains(binding.getSku())) {
                continue;
            }
            String physicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                    binding.getMachineCode());
            if (consumedPhysicalMachineCodeSet.contains(physicalMachineCode)
                    || !this.isCurrentDayNewSpecBinding(context, dayContext, binding)) {
                continue;
            }
            MachineScheduleDTO machine = context.getMachineScheduleMap().get(binding.getMachineCode());
            if (Objects.isNull(machine)) {
                continue;
            }
            MachineSkuMatchResult matchResult = machineMatchStrategy.matchSkuOnMachine(
                    context, machine, specialSku);
            if (Objects.isNull(matchResult) || !matchResult.isMatched()
                    || Objects.isNull(matchResult.getMatchLevel())) {
                continue;
            }
            int matchPriority = matchResult.getMatchLevel().getPriority();
            int victimRank = this.resolveVictimSortRank(binding);
            if (Objects.isNull(bestBinding)
                    || matchPriority < bestMatchPriority
                    || (matchPriority == bestMatchPriority && victimRank > bestVictimRank)
                    || (matchPriority == bestMatchPriority && victimRank == bestVictimRank
                    && this.compareMachineCode(binding, bestBinding) < 0)) {
                bestBinding = binding;
                bestMatchPriority = matchPriority;
                bestVictimRank = victimRank;
            }
        }
        return bestBinding;
    }

    private int compareMachineCode(ActiveMachineBinding left,
                                   ActiveMachineBinding right) {
        String leftCode = Objects.isNull(left) ? null : left.getMachineCode();
        String rightCode = Objects.isNull(right) ? null : right.getMachineCode();
        return Comparator.nullsLast(String::compareTo).compare(leftCode, rightCode);
    }

    /**
     * 对单个新增绑定执行“撤销普通新增结果 -> 特殊 SKU 指定机台重排 -> 更新外层绑定”。
     */
    private boolean trySubstituteOneBinding(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            DayDrivenScheduleState state,
            DailyNewSpecCandidate specialCandidate,
            ActiveMachineBinding victimBinding) {
        SkuScheduleDTO specialSku = specialCandidate.getSku();
        SkuScheduleDTO victimSku = victimBinding.getSku();
        ScheduleSubstitutionAttemptSnapshot snapshot =
                ScheduleSubstitutionAttemptSnapshot.capture(
                        context, Arrays.asList(specialSku, victimSku));
        try {
            Date switchReadyTime = this.removeVictimBindingResult(
                    context, dayContext, victimBinding);
            if (Objects.isNull(switchReadyTime)) {
                snapshot.restore(context);
                return false;
            }
            this.restoreMachineStateBeforeVictim(
                    context, victimBinding, specialSku, switchReadyTime);
            structureEndingAlignmentService.prepareStructureEndingAlignmentIndex(context);
            capsuleReplacementRuleService.rebuildRuntimeState(context, null);
            context.setMouldResourceContext(MouldResourceContext.from(context));

            ScheduleSubstitutionDirective directive = this.buildSpecifiedMachineDirective(
                    specialSku, victimBinding.getMachineCode(), switchReadyTime);
            List<LhScheduleResult> newResultList = specifiedNewSpecSchedulingService.schedule(
                    context, specialSku, directive);
            ActiveMachineBinding targetBinding = this.buildTargetBinding(
                    context, dayContext, specialSku, victimBinding, newResultList);
            if (Objects.isNull(targetBinding)) {
                snapshot.restore(context);
                structureEndingAlignmentService.prepareStructureEndingAlignmentIndex(context);
                return false;
            }

            /*
             * 外层日驱动状态在隔离新增排产期间没有被修改。所有可能失败的结果和账本操作完成后，
             * 最后一次性替换绑定并恢复被置换 SKU 的待排生命周期，避免额外复制整个状态对象。
             */
            state.registerBinding(targetBinding);
            state.markScheduledAndCarryOver(specialSku);
            if (state.hasActiveBinding(victimSku)) {
                state.markScheduledAndCarryOver(victimSku);
            } else {
                state.registerPendingSku(victimSku);
                state.defer(new DeferredScheduleTask(
                        victimSku, dayContext.getScheduleDate(),
                        dayContext.getScheduleDate().plusDays(1),
                        DailySchedulePhase.SPECIAL_SKU,
                        REPLACED_NEW_SPEC_DEFER_REASON));
            }
            this.appendSuccessLog(
                    context, dayContext, specialSku, victimSku,
                    victimBinding, targetBinding, switchReadyTime);
            return true;
        } catch (RuntimeException exception) {
            snapshot.restore(context);
            structureEndingAlignmentService.prepareStructureEndingAlignmentIndex(context);
            log.error("特殊SKU新增结果置换异常，候选状态已完整恢复, batchNo: {}, businessDate: {}, "
                            + "specialMaterialCode: {}, victimMaterialCode: {}, machineCode: {}",
                    context.getBatchNo(), dayContext.getScheduleDate(),
                    specialSku.getMaterialCode(), victimSku.getMaterialCode(),
                    victimBinding.getMachineCode(), exception);
            return false;
        }
    }

    /**
     * 撤销被置换新增绑定的全部结果贡献并返回机台重新具备切换条件的时间。
     */
    private Date removeVictimBindingResult(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            ActiveMachineBinding binding) {
        List<LhScheduleResult> victimResultList = new ArrayList<LhScheduleResult>(2);
        victimResultList.add(binding.getScheduleResult());
        if (binding.hasPairMachine()) {
            victimResultList.add(binding.getPairScheduleResult());
        }
        Date switchReadyTime = this.resolveBindingSwitchStartTime(dayContext, binding);
        if (!dayContext.contains(switchReadyTime)) {
            return null;
        }
        SkuScheduleDTO victimSku = binding.getSku();
        int removedTotalQty = 0;
        Map<LocalDate, Integer> removedQtyByDate = new LinkedHashMap<LocalDate, Integer>(4);
        /*
         * 换模、首检和同胎胚班次占用必须在删除结果关联快照前回滚；精度前插结果的精确时间
         * 保存在结果身份 Map 中，先删除引用会丢失唯一可逆依据。
         */
        this.rollbackVictimChangeoverResources(
                context, victimSku, binding.getScheduleResult());
        for (LhScheduleResult result : victimResultList) {
            if (Objects.isNull(result) || !context.getScheduleResultList().contains(result)) {
                return null;
            }
            this.removeScheduledMachineRegistration(context, result);
            for (LhShiftConfigVO shift : context.getScheduleWindowShifts()) {
                if (Objects.isNull(shift) || Objects.isNull(shift.getShiftIndex())) {
                    continue;
                }
                int shiftQty = Math.max(0, Objects.isNull(
                        ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex()))
                        ? 0 : ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex()));
                if (shiftQty <= 0) {
                    continue;
                }
                removedTotalQty += shiftQty;
                this.releaseMachineShiftCapacity(
                        context, result.getLhMachineCode(), shift.getShiftIndex(), shiftQty);
                LocalDate productionDate = this.toLocalDate(shift.getWorkDate());
                if (Objects.nonNull(productionDate)) {
                    removedQtyByDate.merge(productionDate, shiftQty, Integer::sum);
                }
            }
            this.removeResultReferences(context, result);
        }
        if (removedTotalQty <= 0) {
            return null;
        }
        targetScheduleQtyResolver.restoreProductionRemainingQty(
                context, victimSku, removedTotalQty,
                "特殊SKU置换撤销普通新增结果", binding.getMachineCode());
        this.restoreRemovedDailyQuota(
                context, victimSku, removedQtyByDate);
        return switchReadyTime;
    }

    private Date resolveBindingSwitchStartTime(
            DayScheduleContext dayContext,
            ActiveMachineBinding binding) {
        LhScheduleResult result = Objects.isNull(binding) ? null : binding.getScheduleResult();
        if (Objects.isNull(result)) {
            return null;
        }
        if (Objects.nonNull(result.getMouldChangeStartTime())) {
            return result.getMouldChangeStartTime();
        }
        for (LhShiftConfigVO shift : dayContext.getDayShifts()) {
            Date startTime = ShiftFieldUtil.getShiftStartTime(result, shift.getShiftIndex());
            Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
            if (Objects.nonNull(planQty) && planQty > 0 && Objects.nonNull(startTime)) {
                return startTime;
            }
        }
        return null;
    }

    private int resolveCurrentDayPlanQty(
            DayScheduleContext dayContext,
            ActiveMachineBinding binding) {
        int planQty = this.resolveResultDayPlanQty(dayContext, binding.getScheduleResult());
        if (binding.hasPairMachine()) {
            planQty += this.resolveResultDayPlanQty(dayContext, binding.getPairScheduleResult());
        }
        return planQty;
    }

    private int resolveResultDayPlanQty(
            DayScheduleContext dayContext,
            LhScheduleResult result) {
        if (Objects.isNull(result)) {
            return 0;
        }
        int planQty = 0;
        for (LhShiftConfigVO shift : dayContext.getDayShifts()) {
            Integer shiftQty = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
            planQty += Math.max(0, Objects.isNull(shiftQty) ? 0 : shiftQty);
        }
        return planQty;
    }

    private void rollbackVictimChangeoverResources(
            LhScheduleContext context,
            SkuScheduleDTO victimSku,
            LhScheduleResult primaryResult) {
        IMouldChangeBalanceStrategy mouldChangeStrategy =
                strategyFactory.getMouldChangeBalanceStrategy();
        IFirstInspectionBalanceStrategy inspectionStrategy =
                strategyFactory.getFirstInspectionBalanceStrategy();
        Date mouldChangeStartTime = primaryResult.getMouldChangeStartTime();
        boolean precisionInserted = context.getPrecisionPreInsertResultSet().contains(primaryResult);
        boolean changeoverAllocated = precisionInserted
                || StringUtils.equals("1", primaryResult.getIsChangeMould())
                || StringUtils.equals("1", primaryResult.getIsTypeBlock());
        if (precisionInserted) {
            Date precisionMouldChangeTime = context.getPrecisionPreInsertMouldChangeTimeMap()
                    .get(primaryResult);
            Date precisionInspectionTime = context.getPrecisionPreInsertInspectionTimeMap()
                    .get(primaryResult);
            Integer precisionShiftIndex = context.getPrecisionPreInsertInspectionShiftIndexMap()
                    .get(primaryResult);
            mouldChangeStrategy.rollbackMouldChange(context, precisionMouldChangeTime);
            inspectionStrategy.rollbackInspection(context, precisionInspectionTime);
            FirstInspectionQtyUtil.rollbackFirstInspectionSequence(
                    context, this.resolveShift(context, precisionShiftIndex));
        } else if (changeoverAllocated) {
            mouldChangeStrategy.rollbackMouldChange(context, mouldChangeStartTime);
            LhShiftConfigVO firstInspectionShift = this.resolveFirstInspectionShift(
                    context, primaryResult);
            if (Objects.nonNull(firstInspectionShift)) {
                inspectionStrategy.rollbackInspection(
                        context, firstInspectionShift.getShiftStartDateTime());
                FirstInspectionQtyUtil.rollbackFirstInspectionSequence(
                        context, firstInspectionShift);
            }
        }
        if (changeoverAllocated) {
            this.rollbackGreenTireChangeoverShift(
                    context, victimSku, mouldChangeStartTime);
        }
    }

    private LhShiftConfigVO resolveFirstInspectionShift(
            LhScheduleContext context,
            LhScheduleResult result) {
        for (LhShiftConfigVO shift : context.getScheduleWindowShifts()) {
            if (Objects.isNull(shift) || Objects.isNull(shift.getShiftIndex())) {
                continue;
            }
            String analysis = ShiftFieldUtil.getShiftAnalysis(
                    result, shift.getShiftIndex());
            if (StringUtils.contains(analysis,
                    FirstInspectionQtyUtil.FIRST_INSPECTION_ANALYSIS)) {
                return shift;
            }
        }
        return null;
    }

    private LhShiftConfigVO resolveShift(
            LhScheduleContext context,
            Integer shiftIndex) {
        if (Objects.isNull(shiftIndex)) {
            return null;
        }
        for (LhShiftConfigVO shift : context.getScheduleWindowShifts()) {
            if (Objects.nonNull(shift) && Objects.equals(
                    shiftIndex, shift.getShiftIndex())) {
                return shift;
            }
        }
        return null;
    }

    private void rollbackGreenTireChangeoverShift(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            Date mouldChangeStartTime) {
        if (Objects.isNull(sku) || StringUtils.isEmpty(sku.getEmbryoCode())
                || Objects.isNull(mouldChangeStartTime)) {
            return;
        }
        int shiftIndex = LhScheduleTimeUtil.getShiftIndex(
                context, context.getScheduleDate(), mouldChangeStartTime);
        Set<Integer> occupiedShiftSet = context.getGreenTireChangeoverShiftMap()
                .get(sku.getEmbryoCode());
        if (CollectionUtils.isEmpty(occupiedShiftSet)) {
            return;
        }
        occupiedShiftSet.remove(shiftIndex);
        if (CollectionUtils.isEmpty(occupiedShiftSet)) {
            context.getGreenTireChangeoverShiftMap().remove(sku.getEmbryoCode());
        }
    }

    private void releaseMachineShiftCapacity(
            LhScheduleContext context,
            String machineCode,
            int shiftIndex,
            int releasedQty) {
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
        int[] machineCapacity = Objects.isNull(machine)
                ? null : machine.getShiftRemainingCapacity();
        int[] contextCapacity = context.getMachineShiftCapacityMap().get(machineCode);
        if (Objects.nonNull(machineCapacity) && shiftIndex < machineCapacity.length) {
            machineCapacity[shiftIndex] += releasedQty;
        }
        if (Objects.nonNull(contextCapacity) && contextCapacity != machineCapacity
                && shiftIndex < contextCapacity.length) {
            contextCapacity[shiftIndex] += releasedQty;
        }
    }

    private void removeResultReferences(
            LhScheduleContext context,
            LhScheduleResult result) {
        context.getScheduleResultList().remove(result);
        context.getScheduleResultSourceSkuMap().remove(result);
        context.getSharedEmbryoEndingStaggerReleaseShiftIndexMap().remove(result);
        context.getSharedEmbryoEndingStaggerReleaseShiftQtyMap().remove(result);
        context.getSharedEmbryoEndingStaggerAllowedOverQtyMap().remove(result);
        context.getEndingFillAllowedOverQtyMap().remove(result);
        context.getEndingFillBeforeQtyMap().remove(result);
        context.getNewSpecRealtimeSnapshotResultSet().remove(result);
        context.getPrecisionPreInsertResultSet().remove(result);
        context.getPrecisionPreInsertInspectionTimeMap().remove(result);
        context.getPrecisionPreInsertMouldChangeTimeMap().remove(result);
        context.getPrecisionPreInsertInspectionShiftIndexMap().remove(result);
        context.getHistoricalReverseProtectedResultSet().remove(result);
        List<LhScheduleResult> assignmentList = context.getMachineAssignmentMap()
                .get(result.getLhMachineCode());
        if (Objects.nonNull(assignmentList)) {
            assignmentList.remove(result);
            if (CollectionUtils.isEmpty(assignmentList)) {
                context.getMachineAssignmentMap().remove(result.getLhMachineCode());
            }
        }
        context.getMouldChangePlanList().removeIf(plan -> this.isVictimMouldChangePlan(plan, result));
    }

    private boolean isVictimMouldChangePlan(
            LhMouldChangePlan plan,
            LhScheduleResult result) {
        return Objects.nonNull(plan)
                && StringUtils.equals(plan.getLhMachineCode(), result.getLhMachineCode())
                && StringUtils.equals(plan.getAfterMaterialCode(), result.getMaterialCode())
                && (Objects.isNull(result.getMouldChangeStartTime())
                || Objects.equals(plan.getChangeTime(), result.getMouldChangeStartTime()));
    }

    private void removeScheduledMachineRegistration(
            LhScheduleContext context,
            LhScheduleResult result) {
        for (LhShiftConfigVO shift : context.getScheduleWindowShifts()) {
            if (Objects.isNull(shift) || Objects.isNull(shift.getShiftIndex())) {
                continue;
            }
            Integer planQty = ShiftFieldUtil.getShiftPlanQty(
                    result, shift.getShiftIndex());
            LocalDate businessDate = this.toLocalDate(shift.getWorkDate());
            if (Objects.isNull(planQty) || planQty <= 0 || Objects.isNull(businessDate)) {
                continue;
            }
            this.removeMachineCode(
                    context.getSkuScheduledMachineCodeMap(), businessDate,
                    MonthPlanDateResolver.buildMaterialStatusKey(
                            result.getMaterialCode(), result.getProductStatus()),
                    result.getLhMachineCode());
            this.removeMachineCode(
                    context.getStructureScheduledMachineCodeMap(), businessDate,
                    result.getStructureName(), result.getLhMachineCode());
        }
    }

    private void removeMachineCode(
            Map<LocalDate, Map<String, Set<String>>> targetMap,
            LocalDate businessDate,
            String key,
            String machineCode) {
        if (Objects.isNull(businessDate) || StringUtils.isEmpty(key)
                || CollectionUtils.isEmpty(targetMap)) {
            return;
        }
        Map<String, Set<String>> valueMap = targetMap.get(businessDate);
        if (CollectionUtils.isEmpty(valueMap)) {
            return;
        }
        Set<String> machineCodeSet = valueMap.get(key);
        if (CollectionUtils.isEmpty(machineCodeSet)) {
            return;
        }
        machineCodeSet.remove(machineCode);
        if (CollectionUtils.isEmpty(machineCodeSet)) {
            valueMap.remove(key);
        }
        if (CollectionUtils.isEmpty(valueMap)) {
            targetMap.remove(businessDate);
        }
    }

    /**
     * 恢复被撤销新增结果消费的日计划账本。
     */
    private void restoreRemovedDailyQuota(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            Map<LocalDate, Integer> removedQtyByDate) {
        Map<LocalDate, SkuDailyPlanQuotaDTO> quotaMap =
                context.resolveEffectiveDailyPlanQuotaMap(sku);
        if (CollectionUtils.isEmpty(quotaMap) || CollectionUtils.isEmpty(removedQtyByDate)) {
            return;
        }
        List<Map.Entry<LocalDate, Integer>> removedDateEntryList =
                new ArrayList<Map.Entry<LocalDate, Integer>>(removedQtyByDate.entrySet());
        removedDateEntryList.sort(Map.Entry.<LocalDate, Integer>comparingByKey().reversed());
        for (Map.Entry<LocalDate, Integer> removedDateEntry : removedDateEntryList) {
            LocalDate productionDate = removedDateEntry.getKey();
            int pendingRestoreQty = Math.max(0, removedDateEntry.getValue());
            if (pendingRestoreQty <= 0) {
                continue;
            }
            List<Map.Entry<LocalDate, SkuDailyPlanQuotaDTO>> quotaEntryList =
                    new ArrayList<Map.Entry<LocalDate, SkuDailyPlanQuotaDTO>>(quotaMap.entrySet());
            Collections.reverse(quotaEntryList);
            int restoredQuotaQty = 0;
            int restoredFutureBorrowQty = 0;
            for (Map.Entry<LocalDate, SkuDailyPlanQuotaDTO> quotaEntry : quotaEntryList) {
                if (restoredQuotaQty >= pendingRestoreQty || Objects.isNull(quotaEntry.getValue())) {
                    break;
                }
                SkuDailyPlanQuotaDTO quota = quotaEntry.getValue();
                int restoredQty = Math.min(
                        Math.max(0, quota.getScheduledQty()),
                        pendingRestoreQty - restoredQuotaQty);
                if (restoredQty <= 0) {
                    continue;
                }
                quota.setScheduledQty(quota.getScheduledQty() - restoredQty);
                quota.setRemainingQty(quota.getRemainingQty() + restoredQty);
                restoredQuotaQty += restoredQty;
                if (quotaEntry.getKey().isAfter(productionDate)) {
                    restoredFutureBorrowQty += restoredQty;
                }
            }
            SkuDailyPlanQuotaDTO productionQuota = quotaMap.get(productionDate);
            if (Objects.nonNull(productionQuota)) {
                productionQuota.setActualQty(Math.max(
                        0, productionQuota.getActualQty() - restoredQuotaQty));
                productionQuota.setFutureBorrowQty(Math.max(
                        0, productionQuota.getFutureBorrowQty() - restoredFutureBorrowQty));
            }
            int restoredShiftFillOverQty = Math.max(
                    0, pendingRestoreQty - restoredQuotaQty);
            this.restoreShiftFillOverQty(
                    context, sku, productionQuota, restoredShiftFillOverQty);
        }
        SkuDailyPlanQuotaUtil.refreshRollingFields(quotaMap);
    }

    private void restoreShiftFillOverQty(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            SkuDailyPlanQuotaDTO productionQuota,
            int restoredQty) {
        if (restoredQty <= 0) {
            return;
        }
        if (Objects.nonNull(productionQuota)) {
            productionQuota.setShiftFillOverQty(Math.max(
                    0, productionQuota.getShiftFillOverQty() - restoredQty));
        }
        sku.setShiftFillOverQty(Math.max(0, sku.getShiftFillOverQty() - restoredQty));
        String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                sku.getMaterialCode(), sku.getProductStatus());
        int accumulatedQty = context.getSkuShiftFillOverQtyMap().getOrDefault(skuKey, 0);
        context.getSkuShiftFillOverQtyMap().put(
                skuKey, Math.max(0, accumulatedQty - restoredQty));
    }

    /**
     * 将机台运行态恢复到被置换新增结果接管之前，续作结果本身保持只读。
     */
    private void restoreMachineStateBeforeVictim(
            LhScheduleContext context,
            ActiveMachineBinding victimBinding,
            SkuScheduleDTO targetSku,
            Date switchReadyTime) {
        List<String> machineCodeList = new ArrayList<String>(2);
        machineCodeList.add(victimBinding.getMachineCode());
        if (victimBinding.hasPairMachine()) {
            machineCodeList.add(victimBinding.getPairMachineCode());
        }
        for (String machineCode : machineCodeList) {
            MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
            if (Objects.isNull(machine)) {
                continue;
            }
            LhScheduleResult previousResult = this.resolvePreviousMachineResult(
                    context, machineCode, switchReadyTime);
            SkuScheduleDTO previousSku = Objects.isNull(previousResult)
                    ? null : context.getScheduleResultSourceSkuMap().get(previousResult);
            MachineScheduleDTO initialMachine = context.getInitialMachineScheduleMap().get(machineCode);
            if (Objects.nonNull(previousResult)) {
                machine.setCurrentMaterialCode(previousResult.getMaterialCode());
                machine.setCurrentMaterialDesc(previousResult.getMaterialDesc());
                machine.setEstimatedEndTime(previousResult.getSpecEndTime());
                machine.setEnding(true);
                if (Objects.nonNull(previousSku)) {
                    machine.setPreviousSpecCode(previousSku.getSpecCode());
                    machine.setPreviousProSize(previousSku.getProSize());
                }
            } else if (Objects.nonNull(initialMachine)) {
                machine.setCurrentMaterialCode(initialMachine.getCurrentMaterialCode());
                machine.setCurrentMaterialDesc(initialMachine.getCurrentMaterialDesc());
                machine.setPreviousMaterialCode(initialMachine.getPreviousMaterialCode());
                machine.setPreviousMaterialDesc(initialMachine.getPreviousMaterialDesc());
                machine.setPreviousSpecCode(initialMachine.getPreviousSpecCode());
                machine.setPreviousProSize(initialMachine.getPreviousProSize());
                machine.setEstimatedEndTime(switchReadyTime);
                machine.setEnding(initialMachine.isEnding());
            } else {
                machine.setEstimatedEndTime(switchReadyTime);
            }
            machine.setNextMaterialCode(targetSku.getMaterialCode());
        }
    }

    private LhScheduleResult resolvePreviousMachineResult(
            LhScheduleContext context,
            String machineCode,
            Date switchReadyTime) {
        LhScheduleResult previousResult = null;
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (Objects.isNull(result) || Objects.isNull(result.getSpecEndTime())
                    || !StringUtils.equals(machineCode, result.getLhMachineCode())
                    || result.getSpecEndTime().after(switchReadyTime)
                    || ShiftFieldUtil.resolveScheduledQty(result) <= 0) {
                continue;
            }
            if (Objects.isNull(previousResult)
                    || result.getSpecEndTime().after(previousResult.getSpecEndTime())) {
                previousResult = result;
            }
        }
        return previousResult;
    }

    private ScheduleSubstitutionDirective buildSpecifiedMachineDirective(
            SkuScheduleDTO sku,
            String machineCode,
            Date earliestSwitchTime) {
        ScheduleSubstitutionDirective directive = new ScheduleSubstitutionDirective();
        directive.setSkuKey(MonthPlanDateResolver.buildMaterialStatusKey(
                sku.getMaterialCode(), sku.getProductStatus()));
        directive.setSpecifiedMachineCode(machineCode);
        directive.setEarliestSwitchTime(earliestSwitchTime);
        directive.setTakeoverWithoutMouldChange(false);
        directive.setContinuationRelocation(false);
        return directive;
    }

    private ActiveMachineBinding buildTargetBinding(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            SkuScheduleDTO targetSku,
            ActiveMachineBinding victimBinding,
            List<LhScheduleResult> newResultList) {
        if (CollectionUtils.isEmpty(newResultList)) {
            return null;
        }
        String targetPhysicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                victimBinding.getMachineCode());
        List<LhScheduleResult> physicalResultList = new ArrayList<LhScheduleResult>(2);
        for (LhScheduleResult result : newResultList) {
            if (Objects.nonNull(result)
                    && StringUtils.equals(targetPhysicalMachineCode,
                    LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode()))
                    && this.resolveResultDayPlanQty(dayContext, result) > 0) {
                physicalResultList.add(result);
            }
        }
        if (CollectionUtils.isEmpty(physicalResultList)) {
            return null;
        }
        LhScheduleResult primaryResult = physicalResultList.stream()
                .filter(result -> StringUtils.equals(
                        victimBinding.getMachineCode(), result.getLhMachineCode()))
                .findFirst().orElse(physicalResultList.get(0));
        LhScheduleResult pairResult = physicalResultList.stream()
                .filter(result -> result != primaryResult)
                .findFirst().orElse(null);
        return new ActiveMachineBinding(
                MonthPlanDateResolver.buildMaterialStatusKey(
                        targetSku.getMaterialCode(), targetSku.getProductStatus()),
                targetSku,
                primaryResult.getLhMachineCode(),
                Objects.isNull(pairResult) ? null : pairResult.getLhMachineCode(),
                primaryResult,
                pairResult,
                StringUtils.equals("1", primaryResult.getIsEnd()));
    }

    private void appendSuccessLog(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            SkuScheduleDTO specialSku,
            SkuScheduleDTO victimSku,
            ActiveMachineBinding victimBinding,
            ActiveMachineBinding targetBinding,
            Date switchReadyTime) {
        String detail = new StringBuilder(320)
                .append("batchNo=").append(context.getBatchNo())
                .append(", businessDate=").append(dayContext.getScheduleDate())
                .append(", machineCode=").append(victimBinding.getMachineCode())
                .append(", pairMachineCode=").append(victimBinding.getPairMachineCode())
                .append(", victimMaterialCode=").append(victimSku.getMaterialCode())
                .append(", victimProductStatus=").append(victimSku.getProductStatus())
                .append(", specialMaterialCode=").append(specialSku.getMaterialCode())
                .append(", specialProductStatus=").append(specialSku.getProductStatus())
                .append(", switchReadyTime=")
                .append(LhScheduleTimeUtil.formatDateTime(switchReadyTime))
                .append(", targetEndTime=")
                .append(LhScheduleTimeUtil.formatDateTime(
                        targetBinding.getScheduleResult().getSpecEndTime()))
                .append(", continuationResultEffect=保留不变")
                .toString();
        log.info("特殊SKU新增结果置换成功, {}", detail);
        PriorityTraceLogHelper.appendProcessLog(
                context, "特殊SKU新增结果置换", detail);
    }

    private void appendStageSummary(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            int specialSkuCount,
            int attemptedMachineCount,
            int successMachineCount,
            String reason) {
        String detail = new StringBuilder(192)
                .append("batchNo=").append(context.getBatchNo())
                .append(", businessDate=").append(dayContext.getScheduleDate())
                .append(", specialSkuCount=").append(specialSkuCount)
                .append(", attemptedMachineCount=").append(attemptedMachineCount)
                .append(", successMachineCount=").append(successMachineCount)
                .append(", reason=").append(reason)
                .toString();
        log.info("特殊SKU新增结果置换阶段完成, {}", detail);
        PriorityTraceLogHelper.appendProcessLog(
                context, "特殊SKU新增结果置换汇总", detail);
    }

    private LocalDate toLocalDate(Date date) {
        return Objects.isNull(date) ? null
                : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
