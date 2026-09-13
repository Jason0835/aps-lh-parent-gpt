package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.component.EarlyProductionQuantityCalculator;
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
import com.zlt.aps.lh.engine.strategy.support.MouldResourceAllocationResult;
import com.zlt.aps.lh.engine.strategy.support.MouldResourceContext;
import com.zlt.aps.lh.engine.strategy.support.PendingSkuUnscheduledRule;
import com.zlt.aps.lh.service.ILhDailyMouldCalcService;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 续作完成后的前次交替关系复用阶段。
 * <p>历史只决定按什么顺序尝试既有机台与后物料。每次尝试直接完成一份机台需求的排产，
 * 后续阶段只读取实际结果、剩余需求和资源账本，不读取历史计划或历史优先标记。</p>
 */
@Slf4j
@Service
public class PreviousAlternatePlanReuseService {

    /** 历史日期只用于排序；同日期采用记录主键保持顺序稳定。 */
    private static final Comparator<LhMouldChangePlan> PLAN_ORDER = Comparator
            .comparing(LhMouldChangePlan::getPlanDate, Comparator.nullsLast(Date::compareTo))
            .thenComparing(LhMouldChangePlan::getId, Comparator.nullsLast(Long::compareTo));

    @Resource
    private NewSpecMaterialEligibilityService materialEligibilityService;
    @Resource
    private NewSpecProductionStrategy newSpecProductionStrategy;
    @Resource
    private ScheduleStrategyFactory strategyFactory;
    @Resource
    private ILhDailyMouldCalcService lhDailyMouldCalcService;
    @Resource
    private StructureEndingAlignmentService structureEndingAlignmentService;

    /**
     * 在续作最终收口后执行复用，正常换活字块和新增只能消费更新后的状态。
     * @param context 本次统一排程上下文
     */
    public void reuse(LhScheduleContext context) {
        Map<String, List<LhMouldChangePlan>> index = this.buildPlanIndex(context);
        if (CollectionUtils.isEmpty(index) || CollectionUtils.isEmpty(context.getNewSpecSkuList())) {
            return;
        }
        List<LhShiftConfigVO> shifts = LhScheduleTimeUtil.getScheduleShifts(context, context.getScheduleDate());
        LinkedHashMap<LocalDate, List<LhShiftConfigVO>> dayShifts = LhScheduleTimeUtil.groupByWorkDate(shifts);
        if (CollectionUtils.isEmpty(dayShifts)) {
            return;
        }
        List<String> machineCodes = this.buildRemainingMachinePool(context, shifts);
        if (CollectionUtils.isEmpty(machineCodes)) {
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
        log.info("前次交替复用阶段完成, batchNo: {}, 已占用运行态机台数: {}, 剩余SKU: {}",
                context.getBatchNo(), completedMachineCodes.size(), context.getNewSpecSkuList().size());
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
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
        if (Objects.isNull(machine) || (Objects.nonNull(machine.getEstimatedEndTime())
                && !machine.getEstimatedEndTime().before(day.getDayEndTime()))) {
            return;
        }
        Date availableTime = Objects.nonNull(machine.getEstimatedEndTime())
                ? machine.getEstimatedEndTime() : day.getDayStartTime();
        String beforeMaterial = machine.getCurrentMaterialCode();
        List<LhMouldChangePlan> plans = StringUtils.isEmpty(beforeMaterial) ? Collections.emptyList()
                : index.get(this.buildKey(machineCode, beforeMaterial));
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
                this.fillMachineDemand(context, day, candidate);
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
     * 根据统一目标Map和已提交物理机台数读取一份需求，不从余量自行推算台数。
     * @param context 排程上下文
     * @param day 实际业务日
     * @param candidate 当前需求对象
     */
    private void fillMachineDemand(LhScheduleContext context, DayScheduleContext day, DailyNewSpecCandidate candidate) {
        SkuScheduleDTO sku = candidate.getSku();
        LocalDate requiredDate = EarlyProductionQuantityCalculator.resolveRequiredMachineCountDate(
                context, sku, candidate.getEarlyProductionPreview(), day.getScheduleDate());
        candidate.setTargetMachineCount(lhDailyMouldCalcService.getRequiredMachineCount(
                context, sku.getMaterialCode(), sku.getProductStatus(), requiredDate));
        int scheduledMachineCount = DailyMachineExpansionPlanner.countCommittedMachineDemand(
                context, sku, day.getScheduleDate());
        Set<String> onlySandBlastReleasedMachineCodeSet =
                context.resolveOnlySandBlastReleasedPhysicalMachineCodes(sku);
        if (!CollectionUtils.isEmpty(onlySandBlastReleasedMachineCodeSet)) {
            int activeMachineCount = context.getSkuScheduledMachineCountExcluding(
                    day.getScheduleDate(), sku.getMaterialCode(), sku.getProductStatus(),
                    onlySandBlastReleasedMachineCodeSet);
            candidate.setTargetMachineCount(Math.max(candidate.getTargetMachineCount(),
                    activeMachineCount + Math.max(1, sku.getContinuationShortageMachineCount())));
            // 目标沿用仅喷砂补偿规则，已落实份数仍包含有效前置绑定，不能重新覆盖为当天生产数。
        }
        candidate.setScheduledMachineCount(scheduledMachineCount);
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
     * 冻结前日最近有效批次的全部交替关系，PLAN_DATE只排序，不映射本次班次。
     * @param context 已加载历史计划的上下文
     * @return 机台加前物料对应的有序计划
     */
    Map<String, List<LhMouldChangePlan>> buildPlanIndex(LhScheduleContext context) {
        List<LhMouldChangePlan> source = context.getHistoricalReverseMouldChangePlanList();
        LhMouldChangePlan latest = source.stream().filter(Objects::nonNull)
                .filter(plan -> StringUtils.isNotEmpty(plan.getLhResultBatchNo()))
                .max(Comparator.comparing(LhMouldChangePlan::getCreateTime, Comparator.nullsFirst(Date::compareTo))
                        .thenComparing(LhMouldChangePlan::getId, Comparator.nullsFirst(Long::compareTo))).orElse(null);
        Map<String, List<LhMouldChangePlan>> index = new LinkedHashMap<String, List<LhMouldChangePlan>>(source.size());
        if (Objects.isNull(latest)) {
            return index;
        }
        source.stream().filter(Objects::nonNull)
                .filter(plan -> StringUtils.equals(latest.getLhResultBatchNo(), plan.getLhResultBatchNo()))
                .filter(plan -> StringUtils.isNotEmpty(plan.getLhMachineCode())
                        && StringUtils.isNotEmpty(plan.getBeforeMaterialCode())
                        && StringUtils.isNotEmpty(plan.getAfterMaterialCode()))
                .sorted(PLAN_ORDER).forEach(plan -> index.computeIfAbsent(
                        this.buildKey(plan.getLhMachineCode(), plan.getBeforeMaterialCode()),
                        ignored -> new ArrayList<LhMouldChangePlan>(2)).add(plan));
        return index;
    }

    /**
     * 构建精确运行态机台与前物料键，不合并左右侧历史关系。
     * @param machineCode 机台编码
     * @param beforeMaterial 前物料
     * @return 历史关系索引键
     */
    private String buildKey(String machineCode, String beforeMaterial) {
        return new StringBuilder(64).append(machineCode).append('|').append(beforeMaterial).toString();
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
