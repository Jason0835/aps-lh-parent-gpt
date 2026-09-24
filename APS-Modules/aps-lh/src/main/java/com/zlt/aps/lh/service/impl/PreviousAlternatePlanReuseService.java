package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.lh.engine.strategy.support.ContinuationEndingAllocationSnapshot;
import cn.hutool.core.bean.BeanUtil;
import java.time.ZoneId;
import com.zlt.aps.lh.util.LhMouldCodeUtil;
import com.zlt.aps.lh.engine.strategy.support.ContinuationCutoverResult;
import com.zlt.aps.lh.component.UnscheduledResultCollector;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.api.enums.SkuScheduleSourceTypeEnum;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.api.enums.MouldChangeTypeEnum;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.component.StructureEndingAlignmentService;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.factory.ScheduleStrategyFactory;
import com.zlt.aps.lh.engine.strategy.impl.NewSpecProductionStrategy;
import com.zlt.aps.lh.engine.strategy.support.ActiveMachineBinding;
import com.zlt.aps.lh.engine.strategy.support.DailyCandidateReason;
import com.zlt.aps.lh.engine.strategy.support.DailyMachineExpansionPlanner;
import com.zlt.aps.lh.engine.strategy.support.DailyNewSpecCandidate;
import com.zlt.aps.lh.engine.strategy.support.DailySchedulePhase;
import com.zlt.aps.lh.engine.strategy.support.DayScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.ContinuationTemporaryFaultTransferEvent;
import com.zlt.aps.lh.engine.strategy.support.MouldResourceAllocationResult;
import com.zlt.aps.lh.engine.strategy.support.MouldResourceContext;
import com.zlt.aps.lh.engine.strategy.support.PreviousAlternatePlanReleaseEvent;
import com.zlt.aps.lh.engine.strategy.support.PendingSkuUnscheduledRule;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.MachineStatusUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 续作完成后的前次交替关系复用阶段。
 * <p>T/T+1历史动作先按时释放，再共同预检指定承接及等台数置换；其余历史沿用原复用规则。
 * 后续阶段消费正式结果、实际余量与资源时间，不重新放大目标台数。</p>
 */
@Slf4j
@Service
public class PreviousAlternatePlanReuseService {

    @Resource
    private NewSpecMaterialEligibilityService materialEligibilityService;
    @Resource
    private ContinuationCutoverService continuationCutoverService;
    @Resource
    private TargetScheduleQtyResolver targetScheduleQtyResolver;
    @Resource
    private UnscheduledResultCollector unscheduledResultCollector;

    @Resource
    private NewSpecProductionStrategy newSpecProductionStrategy;
    @Resource
    private ScheduleStrategyFactory strategyFactory;
    @Resource
    private PreviousAlternatePlanEligibilityService previousAlternatePlanEligibilityService;
    @Resource
    private StructureEndingAlignmentService structureEndingAlignmentService;
    @Resource
    private ContinuationTemporaryFaultTransferService continuationTemporaryFaultTransferService;

    /**
     * 在续作排量前冻结历史交替动作和实际在机身份。
     *
     * <p>历史前料本次不参与本机续作时从T日首个合法班次准备交替；其他动作沿用计划日边界。
     * 续作排量稳定后，再由真实完工时刻与边界共同确定实际下机时刻。</p>
     *
     * @param context 已完成MES续作识别的上下文
     */
    public void prepareReleaseEvents(LhScheduleContext context) {
        LocalDate firstDate = context.getScheduleDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        this.previousAlternatePlanEligibilityService.buildPlanIndex(context).values().stream()
                .flatMap(List::stream)
                .filter(plan -> Objects.nonNull(plan.getPlanDate()))
                .filter(plan -> MouldChangeTypeEnum.containsAnyCode(plan.getChangeMouldType(),
                        MouldChangeTypeEnum.REGULAR.getCode(),
                        MouldChangeTypeEnum.TYPE_BLOCK.getCode()))
                .filter(plan -> {
                    if (this.isBeforeMaterialNotScheduled(context, plan)) {
                        return true;
                    }
                    LocalDate date = plan.getPlanDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    return date.equals(firstDate) || date.equals(firstDate.plusDays(1));
                })
                .sorted(this.previousAlternatePlanEligibilityService.getPlanOrder())
                .forEach(plan -> {
                    String machineCode = plan.getLhMachineCode();
                    MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
                    if (Objects.isNull(machine) || context.getPreviousAlternateReleaseEventMap().containsKey(machineCode)) {
                        return;
                    }
                    boolean beforeNotScheduled = this.isBeforeMaterialNotScheduled(context, plan);
                    String currentMaterialCode = machine.getCurrentMaterialCode();
                    if (!StringUtils.equals(currentMaterialCode, plan.getBeforeMaterialCode())
                            && !(beforeNotScheduled && StringUtils.equals(currentMaterialCode, plan.getAfterMaterialCode()))) {
                        log.info("前日交替历史前料与实际在机不一致，跳过强制动作, batchNo: {}, planId: {}, "
                                        + "machineCode: {}, historicalBefore: {}, actualOnline: {}, beforeSurplus: {}",
                                context.getBatchNo(), plan.getId(), machineCode, plan.getBeforeMaterialCode(),
                                currentMaterialCode, this.resolveBeforeMaterialSurplus(context, plan));
                        return;
                    }
                    PreviousAlternatePlanReleaseEvent event = new PreviousAlternatePlanReleaseEvent();
                    event.setPlan(plan);
                    event.setMachineCode(machineCode);
                    event.setMaterialCode(currentMaterialCode);
                    event.setBeforeMaterialNotScheduled(beforeNotScheduled);
                    LhMachineOnlineInfo online = context.getMachineOnlineInfoMap().get(machineCode);
                    event.setProductStatus(Objects.nonNull(online) ? online.getProductStatus() : null);
                    event.setOriginalMouldCodes(LhMouldCodeUtil.resolveInMachineMouldCodeSet(context, machineCode));
                    // 单副换模优先下共用性最高的一副，另一副在关联预演前就保持原机占用。
                    this.freezeRetainedMouldCodes(context, event);
                    event.setOfflineTime(this.resolvePreviousAlternateReleaseTime(context, event));
                    log.info("前日交替动作冻结, batchNo: {}, planId: {}, machineCode: {}, historicalBefore: {}, "
                                    + "actualOnline: {}, afterMaterial: {}, beforeSurplus: {}, releaseTime: {}",
                            context.getBatchNo(), plan.getId(), machineCode, plan.getBeforeMaterialCode(),
                            currentMaterialCode, plan.getAfterMaterialCode(),
                            this.resolveBeforeMaterialSurplus(context, plan),
                            LhScheduleTimeUtil.formatDateTime(event.getOfflineTime()));
                    context.getPreviousAlternateReleaseEventMap().put(machineCode, event);
                });
    }

    /**
     * 读取S4.3清理之前冻结的历史前料硫化余量。
     * @param context 排程上下文
     * @param plan 前日交替计划
     * @return 前料各产品状态的原始硫化余量之和，未加载该物料时返回null
     */
    private Integer resolveBeforeMaterialSurplus(LhScheduleContext context, LhMouldChangePlan plan) {
        return context.getInitialMaterialSurplusQtyMap().get(plan.getBeforeMaterialCode());
    }

    /**
     * 判断本机历史前料是否无需在本次续作阶段生产。
     * @param context 排程上下文
     * @param plan 前日交替计划
     * @return 前料零余量，或实际在机已是后料且本机没有前料续作时为true
     */
    private boolean isBeforeMaterialNotScheduled(LhScheduleContext context, LhMouldChangePlan plan) {
        Integer surplusQty = this.resolveBeforeMaterialSurplus(context, plan);
        if (Objects.nonNull(surplusQty) && surplusQty == 0) {
            return true;
        }
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(plan.getLhMachineCode());
        if (Objects.isNull(machine)
                || StringUtils.equals(plan.getBeforeMaterialCode(), plan.getAfterMaterialCode())
                || !StringUtils.equals(machine.getCurrentMaterialCode(), plan.getAfterMaterialCode())) {
            return false;
        }
        // 历史前料仍有遗留余量也不能把后料冒充前料续作；按本机真实续作归属决定是否等待。
        return context.getContinuousSkuList().stream().filter(Objects::nonNull)
                .noneMatch(sku -> StringUtils.equals(plan.getLhMachineCode(), sku.getContinuousMachineCode())
                        && StringUtils.equals(plan.getBeforeMaterialCode(), sku.getMaterialCode()));
    }

    /**
     * 解析前日交替的原物料下机时刻。
     * <p>历史前料本次不参与本机续作时，按本次T日最早允许班次释放；同料按计划日最早允许班次；
     * 仍需生产的异料按计划日20:00之前的最晚合法时刻截断。</p>
     *
     * @param context 排程上下文
     * @param event 已冻结实际在机物料与历史前料余量的交替事件
     * @return 本次原物料下机边界
     */
    private Date resolvePreviousAlternateReleaseTime(
            LhScheduleContext context, PreviousAlternatePlanReleaseEvent event) {
        LhMouldChangePlan plan = event.getPlan();
        if (event.isBeforeMaterialNotScheduled()
                || StringUtils.equals(plan.getBeforeMaterialCode(), plan.getAfterMaterialCode())) {
            Date actionDate = event.isBeforeMaterialNotScheduled()
                    ? context.getScheduleDate() : plan.getPlanDate();
            return LhScheduleTimeUtil.getScheduleShifts(context, context.getScheduleDate()).stream()
                    .filter(Objects::nonNull)
                    .filter(shift -> Objects.nonNull(shift.getWorkDate())
                            && LhScheduleTimeUtil.isSameDay(shift.getWorkDate(), actionDate))
                    .map(LhShiftConfigVO::getShiftStartDateTime)
                    .filter(Objects::nonNull)
                    .filter(startTime -> !LhScheduleTimeUtil.isNoMouldChangeTime(context, startTime))
                    .min(Date::compareTo)
                    .orElseThrow(() -> new IllegalStateException(
                            "前日交替执行日没有可换模班次，计划ID=" + plan.getId()));
        }
        Date noMouldChangeStartTime = LhScheduleTimeUtil.buildTime(
                LhScheduleTimeUtil.clearTime(plan.getPlanDate()),
                LhScheduleTimeUtil.getNoMouldChangeStartHour(context), 0, 0);
        return LhScheduleTimeUtil.resolveLatestMouldChangeStartTime(
                context, noMouldChangeStartTime);
    }

    /**
     * 冻结单副置换中不下机的原模具，共用性相同时按模具号稳定选择下机模具。
     *
     * @param context 本次模具关系快照
     * @param event 已识别实际在机模具的历史事件
     */
    private void freezeRetainedMouldCodes(LhScheduleContext context, PreviousAlternatePlanReleaseEvent event) {
        if (!event.isSingleMouldReplacement()) {
            return;
        }
        Map<String, Integer> sharedSkuCountMap = LhMouldCodeUtil.buildMouldSharedSkuCountMap(context);
        event.setRetainedMouldCodes(event.getOriginalMouldCodes().stream()
                .sorted(Comparator.comparing((String mouldCode) -> sharedSkuCountMap.getOrDefault(mouldCode, 1))
                        .reversed().thenComparing(Comparator.naturalOrder()))
                .skip(PreviousAlternatePlanReleaseEvent.SINGLE_REPLACEMENT_MOULD_QTY)
                .collect(Collectors.toCollection(() -> new LinkedHashSet<String>(event.getOriginalMouldCodes().size()))));
    }

    /**
     * 在续作最终收口后执行复用，正常换活字块和新增只能消费更新后的状态。
     * @param context 本次统一排程上下文
     */
    public void reuse(LhScheduleContext context) {
        Map<String, List<LhMouldChangePlan>> index =
                this.previousAlternatePlanEligibilityService.buildPlanIndex(context);
        if (CollectionUtils.isEmpty(index) || (CollectionUtils.isEmpty(context.getNewSpecSkuList())
                && CollectionUtils.isEmpty(context.getPreviousAlternateReleaseEventMap()))) {
            this.recordUnresolvedTemporaryFaultTransfers(context, CollectionUtils.isEmpty(index)
                    ? "前日最近有效批次没有可复用交替关系，转入正常候选池"
                    : "故障SKU没有剩余待排需求");
            return;
        }
        List<LhShiftConfigVO> shifts = LhScheduleTimeUtil.getScheduleShifts(context, context.getScheduleDate());
        LinkedHashMap<LocalDate, List<LhShiftConfigVO>> dayShifts = LhScheduleTimeUtil.groupByWorkDate(shifts);
        if (CollectionUtils.isEmpty(dayShifts)) {
            this.recordUnresolvedTemporaryFaultTransfers(
                    context, "当前排程窗口没有可执行班次，转入正常候选池");
            return;
        }
        List<String> machineCodes = this.buildRemainingMachinePool(context, shifts);
        if (CollectionUtils.isEmpty(machineCodes)) {
            this.recordUnresolvedTemporaryFaultTransfers(
                    context, "当前排程窗口没有剩余可用机台，转入正常候选池");
            return;
        }
        // 候选快照必须早于任何前置消费，班次9及试制虚拟计划仍能查询最终剩余量。
        context.getNewSpecSkuList().forEach(context::registerNextShiftNewPlanCandidate);
        context.getNewSpecSkuList().stream().filter(PendingSkuUnscheduledRule::isTrialOrMassTrialSku)
                .filter(sku -> sku.getSurplusQty() > 0).forEach(context::registerTrialVirtualMachineCandidate);
        context.rebuildScheduledMachineCountMaps(shifts);
        structureEndingAlignmentService.prepareStructureEndingAlignmentIndex(context);
        if (Objects.isNull(context.getMouldResourceContext())) {
            context.setMouldResourceContext(MouldResourceContext.from(context));
        }
        Date originalBusinessDate = context.getCurrentScheduleDate();
        Set<String> completedMachineCodes = new LinkedHashSet<String>(machineCodes.size());
        List<LhMouldChangePlan> latestPlanList = index.values().stream()
                .flatMap(List::stream)
                .sorted(this.previousAlternatePlanEligibilityService.getPlanOrder())
                .collect(Collectors.toList());
        int dayIndex = 0;
        try {
            for (Map.Entry<LocalDate, List<LhShiftConfigVO>> entry : dayShifts.entrySet()) {
                dayIndex++;
                DayScheduleContext day = new DayScheduleContext(entry.getKey(), entry.getValue(),
                        dayIndex == 1, dayIndex == dayShifts.size());
                context.setCurrentScheduleDate(LhScheduleTimeUtil.clearTime(entry.getValue().get(0).getWorkDate()));
                context.getMouldResourceContext().refreshAvailability(context);
                // 前日复用已在机的组合先连续生产，不能把其次日产能交给普通换活字块抢占。
                newSpecProductionStrategy.continueSpecifiedMachines(context, day);
                // 故障释放SKU先按“历史后物料→目标机台”尝试，失败后仍保留在普通候选池。
                this.tryReuseTemporaryFaultTransfers(
                        context, day, latestPlanList, completedMachineCodes);
                // 同日历史交替先共用一份已释放资源快照预演，再重放确定的模具组合。
                this.tryReuseReleasedGroup(context, day, completedMachineCodes);
                for (String machineCode : machineCodes) {
                    if (!completedMachineCodes.contains(machineCode)) {
                        this.tryReuseMachine(context, day, machineCode, index, completedMachineCodes);
                    }
                }
                context.rebuildScheduledMachineCountMaps(shifts);
            }
        } finally {
            context.setCurrentScheduleDate(originalBusinessDate);
            context.getMouldResourceContext().refreshAvailability(context);
            context.rebuildStructureSkuMapFromPending(context.getNewSpecSkuList());
        }
        this.recordUnresolvedTemporaryFaultTransfers(
                context, "前日交替计划没有可提交的目标机台，转入正常候选池");
        log.info("前次交替复用阶段完成, batchNo: {}, 已占用运行态机台数: {}, 剩余SKU: {}",
                context.getBatchNo(), completedMachineCodes.size(), context.getNewSpecSkuList().size());
    }

    /**
     * 统一记录尚未由前日交替计划落实的故障迁移事件。
     *
     * @param context 排程上下文
     * @param defaultReason 当前阶段默认失败原因
     */
    private void recordUnresolvedTemporaryFaultTransfers(
            LhScheduleContext context, String defaultReason) {
        if (CollectionUtils.isEmpty(context.getContinuationTemporaryFaultTransferEventMap())) {
            return;
        }
        context.getContinuationTemporaryFaultTransferEventMap().values().stream()
                .filter(Objects::nonNull)
                .filter(event -> StringUtils.isEmpty(event.getTargetMachineCode()))
                .forEach(event -> continuationTemporaryFaultTransferService
                        .recordPreviousAlternateNotMatched(context, event,
                                StringUtils.defaultIfEmpty(event.getFailureReason(), defaultReason)));
    }

    /**
     * 优先按历史后物料关系为故障释放SKU尝试目标机台。
     *
     * @param context 排程上下文
     * @param day 当前业务日
     * @param latestPlanList 前日最近有效批次的交替关系
     * @param completedMachineCodes 已由历史阶段落实的机台
     */
    private void tryReuseTemporaryFaultTransfers(LhScheduleContext context, DayScheduleContext day,
            List<LhMouldChangePlan> latestPlanList, Set<String> completedMachineCodes) {
        if (CollectionUtils.isEmpty(context.getContinuationTemporaryFaultTransferEventMap())
                || CollectionUtils.isEmpty(context.getNewSpecSkuList())
                || CollectionUtils.isEmpty(latestPlanList)) {
            return;
        }
        for (ContinuationTemporaryFaultTransferEvent event
                : context.getContinuationTemporaryFaultTransferEventMap().values()) {
            if (Objects.isNull(event) || StringUtils.isNotEmpty(event.getTargetMachineCode())) {
                continue;
            }
            List<SkuScheduleDTO> candidateSkuList = context.getNewSpecSkuList().stream()
                    .filter(Objects::nonNull)
                    .filter(sku -> StringUtils.equals(event.getMaterialCode(), sku.getMaterialCode()))
                    .filter(sku -> StringUtils.equals(StringUtils.trimToEmpty(event.getProductStatus()),
                            StringUtils.trimToEmpty(sku.getProductStatus())))
                    .filter(sku -> sku.getTemporaryFaultSourceMachineCodeSet()
                            .contains(event.getOriginalPhysicalMachineCode()))
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(candidateSkuList)) {
                event.setFailureReason("故障SKU已无待排余量或未进入候选池");
                continue;
            }
            List<LhMouldChangePlan> targetPlanList = latestPlanList.stream()
                    .filter(plan -> StringUtils.equals(event.getMaterialCode(), plan.getAfterMaterialCode()))
                    .filter(plan -> StringUtils.isNotEmpty(plan.getLhMachineCode()))
                    .filter(plan -> !StringUtils.equals(event.getOriginalPhysicalMachineCode(),
                            LhSingleControlMachineUtil.resolvePhysicalMachineCode(plan.getLhMachineCode())))
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(targetPlanList)) {
                event.setFailureReason("前日交替计划后物料不存在当前故障SKU");
                continue;
            }
            for (LhMouldChangePlan plan : targetPlanList) {
                if (this.tryReuseTemporaryFaultMachine(
                        context, day, event, plan, candidateSkuList, completedMachineCodes)) {
                    break;
                }
            }
        }
    }

    /**
     * 在指定历史目标机台上复用完整新增时间轴和硬约束提交故障SKU。
     *
     * @param context 排程上下文
     * @param day 当前业务日
     * @param event 故障迁移事件
     * @param plan 命中的前日交替关系
     * @param candidateSkuList 同物料同状态候选
     * @param completedMachineCodes 已落实历史机台
     * @return 是否形成有效结果
     */
    private boolean tryReuseTemporaryFaultMachine(LhScheduleContext context, DayScheduleContext day,
            ContinuationTemporaryFaultTransferEvent event, LhMouldChangePlan plan,
            List<SkuScheduleDTO> candidateSkuList, Set<String> completedMachineCodes) {
        String machineCode = plan.getLhMachineCode();
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
        if (Objects.isNull(machine) || completedMachineCodes.contains(machineCode)
                || !MachineStatusUtil.isEnabled(machine.getStatus())
                || context.isContinuousStopHoldMachine(machineCode)
                || (Objects.nonNull(machine.getEstimatedEndTime())
                && !machine.getEstimatedEndTime().before(day.getDayEndTime()))) {
            continuationTemporaryFaultTransferService.recordPreviousAlternateAttempt(
                    context, event, machineCode, false, null, "历史目标机台当前不可用");
            return false;
        }
        for (SkuScheduleDTO sku : candidateSkuList) {
            DailyNewSpecCandidate candidate = materialEligibilityService.resolveCandidate(
                    context, sku, day.getScheduleDate());
            this.previousAlternatePlanEligibilityService.fillMachineDemand(
                    context, day.getScheduleDate(), candidate);
            MouldResourceAllocationResult mould = context.getMouldResourceContext().previewAllocate(
                    sku.getMaterialCode(), machineCode,
                    LhScheduleTimeUtil.resolveDayResourceReferenceTime(
                            context, day.getDayShifts().get(0).getWorkDate(), machine));
            String rejection = this.resolveRejection(candidate, mould);
            if (StringUtils.isNotEmpty(rejection)) {
                continuationTemporaryFaultTransferService.recordPreviousAlternateAttempt(
                        context, event, machineCode, false, null, rejection);
                continue;
            }
            Set<LhScheduleResult> beforeResultSet = java.util.Collections.newSetFromMap(
                    new IdentityHashMap<LhScheduleResult, Boolean>(context.getScheduleResultList().size()));
            beforeResultSet.addAll(context.getScheduleResultList());
            day.setCurrentPhase(candidate.hasReason(DailyCandidateReason.EARLY_PRODUCTION)
                    ? DailySchedulePhase.EARLY_PRODUCTION
                    : DailySchedulePhase.NORMAL_RESOURCE_COMPETITION);
            boolean success = newSpecProductionStrategy.executeSpecifiedMachine(
                    context, day, candidate, machine,
                    strategyFactory.getMachineMatchStrategy(), strategyFactory.getMouldChangeBalanceStrategy(),
                    strategyFactory.getFirstInspectionBalanceStrategy(),
                    strategyFactory.getCapacityCalculateStrategy());
            LhScheduleResult transferResult = context.getScheduleResultList().stream()
                    .filter(result -> !beforeResultSet.contains(result))
                    .filter(result -> StringUtils.equals(sku.getMaterialCode(), result.getMaterialCode()))
                    .filter(result -> StringUtils.equals(machineCode, result.getLhMachineCode()))
                    .findFirst().orElse(null);
            String transferMode = Objects.nonNull(transferResult)
                    && StringUtils.equals("1", transferResult.getIsTypeBlock()) ? "换活字块" : "换模";
            continuationTemporaryFaultTransferService.recordPreviousAlternateAttempt(
                    context, event,
                    LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode),
                    success, success ? transferMode : null,
                    success ? null : StringUtils.defaultIfEmpty(candidate.getLastFailure(),
                            "本次时间轴或计划量无法落地"));
            if (success) {
                context.getContinuationTemporaryFaultTransferEventMap().values().stream()
                        .filter(other -> Objects.nonNull(other)
                                && StringUtils.equals(event.getOriginalPhysicalMachineCode(),
                                other.getOriginalPhysicalMachineCode())
                                && StringUtils.equals(event.getMaterialCode(), other.getMaterialCode())
                                && StringUtils.equals(StringUtils.trimToEmpty(event.getProductStatus()),
                                StringUtils.trimToEmpty(other.getProductStatus())))
                        .forEach(other -> {
                            other.setPreviousAlternateMatched(true);
                            other.setPreviousAlternateMachineCode(plan.getLhMachineCode());
                            other.setTargetMachineCode(event.getTargetMachineCode());
                            other.setTransferMode(transferMode);
                            other.setFailureReason(null);
                        });
                if (Objects.nonNull(transferResult)) {
                    completedMachineCodes.add(transferResult.getLhMachineCode());
                } else {
                    completedMachineCodes.add(machineCode);
                }
                return true;
            }
            machine = context.getMachineScheduleMap().get(machineCode);
        }
        return false;
    }

    /**
     * 从全量启用机台构建剩余产能池，同时包含续作尾部和完全空闲机台。
     * @param context 续作完成后的实时上下文
     * @param shifts 本次窗口班次
     * @return 按真实释放时间和编码稳定排列的运行态机台编码
     */
    List<String> buildRemainingMachinePool(LhScheduleContext context, List<LhShiftConfigVO> shifts) {
        Date windowStart = shifts.get(0).getShiftStartDateTime();
        Date windowEnd = shifts.get(shifts.size() - 1).getShiftEndDateTime();
        return context.getMachineScheduleMap().values().stream().filter(Objects::nonNull)
                .filter(machine -> StringUtils.isNotEmpty(machine.getMachineCode()))
                .filter(machine -> MachineStatusUtil.isEnabled(machine.getStatus()))
                .filter(machine -> !context.isContinuousStopHoldMachine(machine.getMachineCode()))
                .filter(machine -> Objects.isNull(machine.getEstimatedEndTime())
                        || machine.getEstimatedEndTime().before(windowEnd))
                .sorted(Comparator.comparing((MachineScheduleDTO machine) ->
                        Objects.nonNull(machine.getEstimatedEndTime()) ? machine.getEstimatedEndTime() : windowStart)
                        .thenComparing(MachineScheduleDTO::getMachineCode))
                .map(MachineScheduleDTO::getMachineCode).collect(Collectors.toList());
    }

    /**
     * 同一机台按历史计划顺序逐条尝试，每次成功只消费实际声明的机台需求。
     * @param context 排程上下文
     * @param day 实际生产业务日
     * @param machineCode 当前机台
     * @param index 本批冻结的历史关系列表
     * @param completedMachines 已成功复用的运行态机台编码，不对物料全局去重
     */
    private void tryReuseMachine(LhScheduleContext context, DayScheduleContext day, String machineCode,
            Map<String, List<LhMouldChangePlan>> index, Set<String> completedMachines) {
        PreviousAlternatePlanReleaseEvent forcedEvent = context.getPreviousAlternateReleaseEventMap().get(machineCode);
        if (Objects.nonNull(forcedEvent)) {
            // 强制交替已在本日关联预检与提交阶段处理，禁止普通历史循环重复尝试。
            return;
        }
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
        if (Objects.isNull(machine) || (Objects.nonNull(machine.getEstimatedEndTime())
                && !machine.getEstimatedEndTime().before(day.getDayEndTime()))) {
            return;
        }
        Date availableTime = Objects.nonNull(machine.getEstimatedEndTime())
                ? machine.getEstimatedEndTime() : day.getDayStartTime();
        String beforeMaterial = machine.getCurrentMaterialCode();
        List<LhMouldChangePlan> plans = StringUtils.isEmpty(beforeMaterial) ? Collections.emptyList()
                : index.get(this.previousAlternatePlanEligibilityService.buildKey(
                        machineCode, beforeMaterial));
        if (CollectionUtils.isEmpty(plans)) {
            this.trace(context, day, machine, beforeMaterial, availableTime, null, 0, null, null, false,
                    StringUtils.isEmpty(beforeMaterial) ? "没有可识别的前物料" : "未匹配到前次交替关系");
            return;
        }
        int order = 0;
        for (LhMouldChangePlan plan : plans) {
            order++;
            if (StringUtils.equals(beforeMaterial, plan.getAfterMaterialCode())) {
                this.trace(context, day, machine, beforeMaterial, availableTime, plan, order, null, null, false, "前后物料相同，无交替关系");
                continue;
            }
            List<SkuScheduleDTO> candidates = context.getNewSpecSkuList().stream().filter(Objects::nonNull)
                    .filter(sku -> StringUtils.equals(plan.getAfterMaterialCode(), sku.getMaterialCode()))
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(candidates)) {
                this.trace(context, day, machine, beforeMaterial, availableTime, plan, order, null, null, false, "后物料不在本次待排范围");
            }
            for (SkuScheduleDTO sku : candidates) {
                DailyNewSpecCandidate candidate = materialEligibilityService.resolveCandidate(context, sku, day.getScheduleDate());
                this.previousAlternatePlanEligibilityService.fillMachineDemand(
                        context, day.getScheduleDate(), candidate);
                MouldResourceAllocationResult mould = context.getMouldResourceContext().previewAllocate(
                        sku.getMaterialCode(), machineCode,
                        LhScheduleTimeUtil.resolveDayResourceReferenceTime(
                                context, day.getDayShifts().get(0).getWorkDate(), machine));
                String rejection = this.resolveRejection(candidate, mould);
                boolean success = false;
                if (StringUtils.isEmpty(rejection)) {
                    day.setCurrentPhase(candidate.hasReason(DailyCandidateReason.EARLY_PRODUCTION)
                            ? DailySchedulePhase.EARLY_PRODUCTION : DailySchedulePhase.NORMAL_RESOURCE_COMPETITION);
                    success = newSpecProductionStrategy.executeSpecifiedMachine(context, day, candidate, machine,
                            strategyFactory.getMachineMatchStrategy(), strategyFactory.getMouldChangeBalanceStrategy(),
                            strategyFactory.getFirstInspectionBalanceStrategy(), strategyFactory.getCapacityCalculateStrategy());
                    rejection = success ? "无" : StringUtils.defaultIfEmpty(candidate.getLastFailure(), "本次时间轴或计划量无法落地");
                }
                this.trace(context, day, machine, beforeMaterial, availableTime, plan, order, candidate, mould, success, rejection);
                if (success) {
                    // 单控整机可能生成左右侧两行；运行态编码用于去重，需求份数仍按物理机台计算。
                    for (ActiveMachineBinding binding : context.getPreScheduledMachineBindingList()) {
                        completedMachines.add(binding.getMachineCode());
                        if (StringUtils.isNotEmpty(binding.getPairMachineCode())) {
                            completedMachines.add(binding.getPairMachineCode());
                        }
                    }
                    return;
                }
                // 失败提交可能恢复Map，下一关系必须重新取得权威机台对象。
                machine = context.getMachineScheduleMap().get(machineCode);
            }
        }
    }

    /**
     * 关联交替在同一资源状态中预检，强制下机已完成，失败关系不会恢复旧续作。
     * @param context 排程上下文
     * @param day 当前业务日
     * @param completed 已提交的机台
     */
    private void tryReuseReleasedGroup(LhScheduleContext context, DayScheduleContext day, Set<String> completed) {
        List<PreviousAlternatePlanReleaseEvent> events = context.getPreviousAlternateReleaseEventMap().values().stream()
                .filter(event -> !completed.contains(event.getMachineCode()))
                .filter(event -> event.getOfflineTime().before(day.getDayEndTime()))
                .sorted(Comparator.comparing(event -> event.getPlan(), previousAlternatePlanEligibilityService.getPlanOrder()))
                .collect(Collectors.toList());
        if (events.isEmpty()) {
            return;
        }
        List<SkuScheduleDTO> sources = new ArrayList<SkuScheduleDTO>(context.getNewSpecSkuList());
        sources.addAll(context.getContinuousSkuList());
        ScheduleSubstitutionAttemptSnapshot snapshot = ScheduleSubstitutionAttemptSnapshot.capture(context, sources);
        Set<String> previewCompleted = new LinkedHashSet<String>(completed);
        Map<String, List<String>> chosenMoulds = new LinkedHashMap<String, List<String>>(events.size());
        boolean originalPreview = context.isPreviousAlternateGroupPreview();
        try {
            context.setPreviousAlternateGroupPreview(true);
            for (PreviousAlternatePlanReleaseEvent event : events) {
                this.tryReuseReleasedMachine(context, day, event, previewCompleted);
            }
            context.getPreviousAlternateResultPlanMap().forEach((result, plan) -> {
                if (!completed.contains(result.getLhMachineCode())) {
                    chosenMoulds.put(result.getLhMachineCode(), new ArrayList<String>(
                            LhMouldCodeUtil.splitMouldCode(result.getMouldCode())));
                }
            });
        } finally {
            snapshot.restore(context);
            context.setPreviousAlternateGroupPreview(originalPreview);
        }
        try {
            context.getPreviousAlternatePlannedMouldMap().putAll(chosenMoulds);
            for (PreviousAlternatePlanReleaseEvent event : events) {
                this.tryReuseReleasedMachine(context, day, event, completed);
            }
        } finally {
            context.getPreviousAlternatePlannedMouldMap().clear();
        }
    }

    /**
     * 强制下机后的指定承接；已满目标台数时先置换一台真实续作承载。
     * 失败恢复到强制下机之后的快照，不撤销必须执行的历史下机。
     * @param context 排程上下文
     * @param day 当前业务日
     * @param event 历史交替事件
     * @param completedMachines 已落实机台
     */
    private void tryReuseReleasedMachine(LhScheduleContext context, DayScheduleContext day,
            PreviousAlternatePlanReleaseEvent event, Set<String> completedMachines) {
        if (!event.getOfflineTime().before(day.getDayEndTime())) {
            return;
        }
        List<SkuScheduleDTO> sources = new ArrayList<SkuScheduleDTO>(context.getNewSpecSkuList());
        sources.addAll(context.getContinuousSkuList());
        Set<String> attemptedKeys = new LinkedHashSet<String>(4);
        for (SkuScheduleDTO source : sources) {
            if (!StringUtils.equals(event.getPlan().getAfterMaterialCode(), source.getMaterialCode())) {
                continue;
            }
            String key = MonthPlanDateResolver.buildMaterialStatusKey(
                    source.getMaterialCode(), source.getProductStatus());
            if (!attemptedKeys.add(key)) {
                continue;
            }
            ScheduleSubstitutionAttemptSnapshot snapshot = ScheduleSubstitutionAttemptSnapshot.capture(context, sources);
            PreviousAlternatePlanReleaseEvent previousEvent = context.getActivePreviousAlternateEvent();
            boolean success = false;
            String reason = "指定组合未形成有效排产";
            try {
                context.setActivePreviousAlternateEvent(event);
                DailyNewSpecCandidate demand = materialEligibilityService.resolveCandidate(context, source, day.getScheduleDate());
                previousAlternatePlanEligibilityService.fillMachineDemand(context, day.getScheduleDate(), demand);
                if (demand.getTargetMachineCount() > 0
                        && demand.getScheduledMachineCount() >= demand.getTargetMachineCount()) {
                    if (!this.releaseReplacementCarrier(context, day, event, source)) {
                        reason = "目标台数已满足且没有可置换的同状态续作承载";
                        continue;
                    }
                }
                SkuScheduleDTO candidateSku = this.buildAlternateCandidate(context, source, day);
                if (candidateSku.getRemainingScheduleQty() <= 0) {
                    reason = "物料真实剩余量为零";
                    continue;
                }
                DailyNewSpecCandidate candidate = materialEligibilityService.resolveCandidate(context, candidateSku, day.getScheduleDate());
                previousAlternatePlanEligibilityService.fillMachineDemand(context, day.getScheduleDate(), candidate);
                if (event.isBeforeMaterialNotScheduled()) {
                    log.info("前日交替前料未排承接, batchNo: {}, planId: {}, machineCode: {}, materialCode: {}, "
                                    + "businessDate: {}, originalDayPlanQty: {}, scheduledMachines: {}, targetMachines: {}",
                            context.getBatchNo(), event.getPlan().getId(), event.getMachineCode(),
                            candidateSku.getMaterialCode(), day.getScheduleDate(), candidate.getOriginalDayPlanQty(),
                            candidate.getScheduledMachineCount(), candidate.getTargetMachineCount());
                }
                candidateSku.setContinuationRequiredMachineCount(candidate.getTargetMachineCount());
                candidateSku.setContinuationActiveMachineCount(candidate.getScheduledMachineCount());
                candidateSku.setContinuationShortageMachineCount(Math.max(0,
                        candidate.getTargetMachineCount() - candidate.getScheduledMachineCount()));
                MachineScheduleDTO machine = context.getMachineScheduleMap().get(event.getMachineCode());
                // 指定交替按真实释放时刻预检准备资源，不能被后料生产日首班抬高到禁换模时段。
                Date resourceTime = this.resolvePreviousAlternateResourceTime(context, event, machine);
                resourceTime = this.resolvePreviousAlternateMouldResourceTime(
                        context, day, candidateSku, machine, resourceTime);
                context.getPreviousAlternateCandidateAvailableTimeMap().put(candidateSku, resourceTime);
                log.info("前日交替准备资源时刻, batchNo: {}, planId: {}, businessDate: {}, machineCode: {}, "
                                + "materialCode: {}, offlineTime: {}, resourceTime: {}",
                        context.getBatchNo(), event.getPlan().getId(), day.getScheduleDate(), event.getMachineCode(),
                        candidateSku.getMaterialCode(), LhScheduleTimeUtil.formatDateTime(event.getOfflineTime()),
                        LhScheduleTimeUtil.formatDateTime(resourceTime));
                MouldResourceAllocationResult allocation = context.getMouldResourceContext()
                        .previewAllocate(candidateSku.getMaterialCode(), event.getMachineCode(), resourceTime);
                reason = this.resolveRejection(candidate, allocation);
                if (StringUtils.isNotEmpty(reason)) {
                    continue;
                }
                Set<LhScheduleResult> before = Collections.newSetFromMap(new IdentityHashMap<LhScheduleResult, Boolean>(16));
                before.addAll(context.getScheduleResultList());
                context.getNewSpecSkuList().add(candidateSku);
                day.setCurrentPhase(candidate.hasReason(DailyCandidateReason.EARLY_PRODUCTION)
                        ? DailySchedulePhase.EARLY_PRODUCTION : DailySchedulePhase.NORMAL_RESOURCE_COMPETITION);
                success = newSpecProductionStrategy.executeSpecifiedMachine(context, day, candidate, machine,
                        strategyFactory.getMachineMatchStrategy(), strategyFactory.getMouldChangeBalanceStrategy(),
                        strategyFactory.getFirstInspectionBalanceStrategy(), strategyFactory.getCapacityCalculateStrategy());
                reason = success ? "指定交替已提交" : candidate.getLastFailure();
                if (success) {
                    context.getScheduleResultList().stream().filter(result -> !before.contains(result))
                            .filter(result -> StringUtils.equals(event.getMachineCode(), result.getLhMachineCode()))
                            .forEach(result -> context.getPreviousAlternateResultPlanMap().put(result, event.getPlan()));
                    completedMachines.add(event.getMachineCode());
                }
            } finally {
                if (!success) {
                    snapshot.restore(context);
                }
                context.setActivePreviousAlternateEvent(previousEvent);
                // 日志位于恢复之后，既保留失败原因，也不把失败预演的生产日志当正式结果。
                this.traceAlternate(context, "前日交替指定承接",
                        String.format("批次=%s, 计划ID=%s, 业务日=%s, 机台=%s, 后物料=%s, 状态=%s, 成功=%s, 原因=%s",
                                context.getBatchNo(), event.getPlan().getId(), day.getScheduleDate(),
                                event.getMachineCode(), source.getMaterialCode(), source.getProductStatus(), success, reason));
            }
            if (success) {
                return;
            }
        }
        if (attemptedKeys.isEmpty()) {
            this.traceAlternate(context, "前日交替指定承接",
                    String.format("批次=%s, 计划ID=%s, 机台=%s, 后物料=%s, 成功=false, 原因=本次无对应有效物料来源",
                            context.getBatchNo(), event.getPlan().getId(), event.getMachineCode(), event.getPlan().getAfterMaterialCode()));
        }
    }

    /**
     * 解析指定交替在本窗口内最早可准备的资源时刻。
     * <p>日计划和目标台数仍按当前生产业务日准入；仅准备动作回看已释放的机台，
     * 不能早于窗口首班、机台实际占用结束或历史事件确定的下机边界。</p>
     *
     * @param context 本次排程窗口及资源状态
     * @param event 已完成续作收口的交替事件
     * @param machine 指定承接机台
     * @return 可继续校验完整模具组的最早准备时刻
     */
    private Date resolvePreviousAlternateResourceTime(LhScheduleContext context,
            PreviousAlternatePlanReleaseEvent event, MachineScheduleDTO machine) {
        Date resourceTime = LhScheduleTimeUtil.resolveWindowResourceReferenceTime(context, machine);
        return event.getOfflineTime().after(resourceTime) ? event.getOfflineTime() : resourceTime;
    }

    /**
     * 解析关联交替可取得完整模具组的最早资源时刻。
     * <p>同日关联事件可能先后释放共用模具。目标机台早于关联模具释放时，不能直接判定模具不足，
     * 应按已登记的真实释放时刻依次重试，并把首个可用时刻继续传入正式换模时间轴。</p>
     *
     * @param context 排程上下文
     * @param day 当前业务日
     * @param sku 历史指定后物料
     * @param machine 历史指定机台
     * @param initialResourceTime 机台自身最早资源时刻
     * @return 同时满足机台和完整模具组的最早资源时刻
     */
    private Date resolvePreviousAlternateMouldResourceTime(
            LhScheduleContext context,
            DayScheduleContext day,
            SkuScheduleDTO sku,
            MachineScheduleDTO machine,
            Date initialResourceTime) {
        if (Objects.isNull(context) || Objects.isNull(day) || Objects.isNull(sku)
                || Objects.isNull(machine) || Objects.isNull(initialResourceTime)
                || Objects.isNull(context.getMouldResourceContext())) {
            return initialResourceTime;
        }
        MouldResourceAllocationResult initialAllocation = context.getMouldResourceContext()
                .previewAllocate(sku.getMaterialCode(), machine.getMachineCode(), initialResourceTime);
        if (initialAllocation.isAllowed()
                || CollectionUtils.isEmpty(context.getPreScheduledMouldReleaseTimeMap())) {
            return initialResourceTime;
        }
        List<Date> releaseTimeList = context.getPreScheduledMouldReleaseTimeMap().values().stream()
                .filter(Objects::nonNull)
                .filter(releaseTime -> releaseTime.after(initialResourceTime))
                .filter(releaseTime -> releaseTime.before(day.getDayEndTime()))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        for (Date releaseTime : releaseTimeList) {
            MouldResourceAllocationResult allocation = context.getMouldResourceContext()
                    .previewAllocate(sku.getMaterialCode(), machine.getMachineCode(), releaseTime);
            if (!allocation.isAllowed()) {
                continue;
            }
            log.info("前日关联交替等待共用模具释放, batchNo: {}, businessDate: {}, materialCode: {}, "
                            + "machineCode: {}, machineAvailableTime: {}, mouldAvailableTime: {}",
                    context.getBatchNo(), day.getScheduleDate(), sku.getMaterialCode(), machine.getMachineCode(),
                    LhScheduleTimeUtil.formatDateTime(initialResourceTime),
                    LhScheduleTimeUtil.formatDateTime(releaseTime));
            return releaseTime;
        }
        return initialResourceTime;
    }

    /**
     * 等台数置换原承载，截断和日账本恢复复用已有服务；不增加目标机台数。
     * @param context 排程上下文
     * @param day 当前业务日
     * @param event 目标交替
     * @param sku 待承接物料
     * @return 是否已经释放一份真实承载
     */
    private boolean releaseReplacementCarrier(LhScheduleContext context, DayScheduleContext day,
            PreviousAlternatePlanReleaseEvent event, SkuScheduleDTO sku) {
        Date offline = event.getOfflineTime().after(day.getDayStartTime()) ? event.getOfflineTime() : day.getDayStartTime();
        LhScheduleResult donor = context.getScheduleResultList().stream()
                .filter(result -> StringUtils.equals(ScheduleTypeEnum.CONTINUOUS.getCode(), result.getScheduleType()))
                .filter(result -> StringUtils.equals(sku.getMaterialCode(), result.getMaterialCode())
                        && StringUtils.equals(sku.getProductStatus(), result.getProductStatus()))
                .filter(result -> !StringUtils.equals(event.getMachineCode(), result.getLhMachineCode()))
                .filter(result -> !context.getPreviousAlternateReleasedMachineTimeMap().containsKey(result.getLhMachineCode()))
                .filter(result -> this.isWholeCarrierCompatible(context, result, sku, offline))
                .filter(result -> Objects.nonNull(result.getSpecEndTime()) && result.getSpecEndTime().after(offline))
                .sorted(Comparator.comparing(LhScheduleResult::getLhMachineCode))
                .findFirst().orElse(null);
        if (Objects.isNull(donor)) {
            return false;
        }
        String machineCode = donor.getLhMachineCode();
        SkuScheduleDTO source = context.getScheduleResultSourceSkuMap().get(donor);
        if (Objects.isNull(source)) {
            return false;
        }
        ContinuationCutoverResult cutover = continuationCutoverService
                .cutoverCurrentContinuation(context, source,
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode), offline);
        if (cutover.getRemovedQty() <= 0) {
            return false;
        }
        // 承载置换属于独立强制下机，快照必须更新为本次真实截断量，不能冒充原余量收尾。
        for (LhScheduleResult retained : cutover.getRetainedResultList()) {
            ContinuationEndingAllocationSnapshot finishSnapshot =
                    context.getContinuationSurplusEndingSnapshotMap().get(retained);
            if (Objects.nonNull(finishSnapshot)) {
                // 替换快照对象，确保失败尝试恢复原映射时不会带回被原地修改的快照。
                ContinuationEndingAllocationSnapshot updated =
                        BeanUtil.copyProperties(finishSnapshot,
                                ContinuationEndingAllocationSnapshot.class);
                updated.setShiftQuantities(context.getScheduleWindowShifts().stream()
                        .mapToInt(shift -> ShiftFieldUtil.getShiftPlanQty(retained, shift.getShiftIndex()))
                        .toArray());
                int lastShift = ShiftFieldUtil.resolveLastPlannedShiftIndex(retained);
                updated.setProductionEndTime(lastShift > 0
                        ? ShiftFieldUtil.getShiftEndTime(retained, lastShift) : null);
                updated.setReleaseTime(offline);
                context.getContinuationSurplusEndingSnapshotMap().put(retained, updated);
            }
        }
        String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(sku.getMaterialCode(), sku.getProductStatus());
        context.getMachineScheduleMap().values().stream()
                .filter(machine -> StringUtils.equals(LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode),
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(machine.getMachineCode())))
                .forEach(machine -> {
                    String code = machine.getMachineCode();
                    LhMouldCodeUtil.resolveInMachineMouldCodeSet(context, code).forEach(mould ->
                            context.getPreScheduledMouldReleaseTimeMap().put(mould, offline));
                    context.getPreviousAlternateReleasedMachineTimeMap().put(code, offline);
                    context.getPreviousAlternateReleasedSkuKeyMap().put(code, skuKey);
                    machine.setPreviousMaterialCode(machine.getCurrentMaterialCode());
                    machine.setCurrentMaterialCode(null);
                    machine.setCurrentMaterialDesc(null);
                    machine.setEstimatedEndTime(offline);
                    machine.setEnding(false);
                });
        context.setMouldResourceContext(MouldResourceContext.from(context));
        this.traceAlternate(context, "前日交替等台数承载置换",
                String.format("批次=%s, 物料=%s, 状态=%s, 原机台=%s, 目标机台=%s, 下机=%s, 恢复量=%s",
                        context.getBatchNo(), sku.getMaterialCode(), sku.getProductStatus(), machineCode,
                        event.getMachineCode(), LhScheduleTimeUtil.formatDateTime(offline), cutover.getRemovedQty()));
        return true;
    }

    /**
     * 物理机台整体迁移只能包含同物料同状态，不能误释放另一单控侧。
     * @param context 排程上下文
     * @param donor 候选承载结果
     * @param sku 待承接物料
     * @param offline 下机时刻
     * @return 整体资源是否属于可迁移的同一物料
     */
    private boolean isWholeCarrierCompatible(LhScheduleContext context, LhScheduleResult donor,
            SkuScheduleDTO sku, Date offline) {
        String physicalCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(donor.getLhMachineCode());
        return context.getScheduleResultList().stream()
                .filter(result -> StringUtils.equals(physicalCode,
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode())))
                .filter(result -> Objects.nonNull(result.getSpecEndTime()) && result.getSpecEndTime().after(offline))
                .allMatch(result -> StringUtils.equals(ScheduleTypeEnum.CONTINUOUS.getCode(), result.getScheduleType())
                        && StringUtils.equals(sku.getMaterialCode(), result.getMaterialCode())
                        && StringUtils.equals(sku.getProductStatus(), result.getProductStatus())
                        && !context.isContinuousStopHoldMachine(result.getLhMachineCode()));
    }

    /**
     * 创建指定承接视图，与原物料共享数量及日计划账本。
     * @param context 排程上下文
     * @param source 原需求
     * @param day 当前业务日
     * @return 候选副本，不改变原续作身份
     */
    private SkuScheduleDTO buildAlternateCandidate(LhScheduleContext context, SkuScheduleDTO source, DayScheduleContext day) {
        SkuScheduleDTO target = new SkuScheduleDTO();
        BeanUtil.copyProperties(source, target);
        target.setContinuousMachineCode(null);
        target.setPreferredContinuousMachineCode(null);
        target.setScheduleType(ScheduleTypeEnum.NEW_SPEC.getCode());
        int remaining = targetScheduleQtyResolver.resolveProductionRemainingQty(context, source);
        target.setTargetScheduleQty(remaining);
        target.setRemainingScheduleQty(remaining);
        target.setPendingQty(remaining);
        target.setDailyPlanQuotaMap(source.getDailyPlanQuotaMap());
        if (StringUtils.isNotEmpty(source.getContinuousMachineCode()) || source.isContinuousCompensationSku()) {
            target.setContinuousCompensationSku(true);
            target.setSourceType(SkuScheduleSourceTypeEnum.CONTINUATION_ADD_MACHINE.getCode());
            target.setFirstAddMachineProductionDate(day.getScheduleDate());
        }
        unscheduledResultCollector.bindDerivedDemand(context, source, target);
        context.getPreviousAlternateCandidateAvailableTimeMap().put(target,
                context.getActivePreviousAlternateEvent().getOfflineTime());
        return target;
    }

    /**
     * 同步记录实际交替尝试的应用日志与过程日志。
     * @param context 排程上下文
     * @param title 业务阶段
     * @param detail 决策与失败原因
     */
    private void traceAlternate(LhScheduleContext context, String title, String detail) {
        String phase = context.isPreviousAlternateGroupPreview() ? "关联预演" : "正式提交";
        log.info("{}, 阶段={}, {}", title, phase, detail);
        PriorityTraceLogHelper.appendProcessLog(context, title, String.format("阶段=%s, %s", phase, detail));
    }

    /**
     * 只检查物料资格、剩余需求份数和统一模具资源，不检查选型优先级。
     * @param candidate 物料资格与需求
     * @param mould 实时模具分配预检
     * @return 拒绝原因，通过时为空
     */
    private String resolveRejection(DailyNewSpecCandidate candidate, MouldResourceAllocationResult mould) {
        if (candidate.getReasons().isEmpty()) {
            return candidate.getLastFailure();
        }
        if (candidate.getTargetMachineCount() <= candidate.getScheduledMachineCount()) {
            return "本次剩余机台需求份数为0";
        }
        return mould.isAllowed() ? null : "剩余有效模具不足：" + mould.getSkipReason();
    }

    /**
     * 输出独立决策证据，成功必须以实际提交为准。
     * @param context 排程上下文
     * @param day 本次实际业务日
     * @param machine 当前机台
     * @param beforeMaterial 尝试前冻结的实时前物料
     * @param availableTime 尝试前的机台释放时间
     * @param plan 历史计划，未命中为空
     * @param order 历史排序序号
     * @param candidate 本次候选，未找到为空
     * @param mould 模具预检，未执行为空
     * @param success 是否实际提交成功
     * @param reason 跳过或执行原因
     */
    private void trace(LhScheduleContext context, DayScheduleContext day, MachineScheduleDTO machine,
            String beforeMaterial, Date availableTime, LhMouldChangePlan plan, int order, DailyNewSpecCandidate candidate,
            MouldResourceAllocationResult mould, boolean success, String reason) {
        int target = Objects.isNull(candidate) ? 0 : candidate.getTargetMachineCount();
        int scheduled = Objects.isNull(candidate) ? 0 : DailyMachineExpansionPlanner.countScheduledPhysicalMachines(
                context, candidate.getSku(), day.getScheduleDate());
        long reservedCount = Objects.isNull(candidate) ? 0 : context.getPreScheduledMachineBindingList().stream()
                .filter(binding -> StringUtils.equals(binding.getSkuKey(), MonthPlanDateResolver.buildMaterialStatusKey(
                        candidate.getSku().getMaterialCode(), candidate.getSku().getProductStatus())))
                .map(binding -> LhSingleControlMachineUtil.resolvePhysicalMachineCode(binding.getMachineCode()))
                .distinct().count();
        String detail = new StringBuilder(512).append("批次=").append(context.getBatchNo())
                .append(", 本次业务日=").append(day.getScheduleDate()).append(", 机台=").append(machine.getMachineCode())
                .append(", 续作后前物料=").append(beforeMaterial)
                .append(", 续作后机台释放时间=").append(availableTime)
                .append(", 本日剩余可用窗口毫秒=").append(Math.max(0L,
                        day.getDayEndTime().getTime() - Math.max(day.getDayStartTime().getTime(), availableTime.getTime())))
                .append(", 提交后机台释放时间=").append(machine.getEstimatedEndTime())
                .append(", 本日窗口结束=").append(day.getDayEndTime())
                .append(", 前次计划日期=").append(Objects.isNull(plan) ? null : plan.getPlanDate())
                .append(", 前次后物料=").append(Objects.isNull(plan) ? null : plan.getAfterMaterialCode())
                .append(", 排序序号=").append(order)
                .append(", 产品状态=").append(Objects.isNull(candidate) ? null : candidate.getSku().getProductStatus())
                .append(", 本次是否可排=").append(Objects.nonNull(candidate) && !candidate.getReasons().isEmpty())
                .append(", 分配前剩余模具数=").append(Objects.isNull(mould) ? 0
                        : Math.max(0, mould.getAvailableMouldQty() - mould.getOccupiedMouldQty()))
                .append(", SKU总需求机台数=").append(target).append(", 已排份数=").append(scheduled)
                .append(", 已预占份数=").append(reservedCount)
                .append(", 本次预占份数=").append(success ? 1 : 0)
                .append(", 剩余需求份数=").append(Math.max(0, target - scheduled))
                .append(", 是否复用成功=").append(success).append(", 跳过原因=").append(reason).toString();
        log.info("前次交替计划复用, {}", detail);
        PriorityTraceLogHelper.appendProcessLog(context, "前次交替计划复用", detail);
    }
}
