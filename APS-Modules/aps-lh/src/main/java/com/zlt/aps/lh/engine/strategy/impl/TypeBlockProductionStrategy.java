/**
 * Copyright (c) 2008, 智立通（厦门）科技有限公司 All rights reserved。
 */
package com.zlt.aps.lh.engine.strategy.impl;

import cn.hutool.core.bean.BeanUtil;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.constant.LhScheduleParamConstant;
import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineMaintenanceWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.ShiftProductionControlDTO;
import com.zlt.aps.lh.api.domain.dto.ShiftRuntimeState;
import com.zlt.aps.lh.api.domain.dto.SkuDailyPlanQuotaDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.entity.LhUnscheduledResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.CleaningTypeEnum;
import com.zlt.aps.lh.api.enums.ConstructionStageEnum;
import com.zlt.aps.lh.api.enums.MachineStopTypeEnum;
import com.zlt.aps.lh.api.enums.MouldChangeTypeEnum;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.component.CapsuleReplacementRuleService;
import com.zlt.aps.lh.component.EarlyProductionRuntimePlanService;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.component.OrderNoGenerator;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.ICapacityCalculateStrategy;
import com.zlt.aps.lh.engine.strategy.IEndingJudgmentStrategy;
import com.zlt.aps.lh.engine.strategy.IFirstInspectionBalanceStrategy;
import com.zlt.aps.lh.engine.strategy.IMachineMatchStrategy;
import com.zlt.aps.lh.engine.strategy.IMouldChangeBalanceStrategy;
import com.zlt.aps.lh.engine.strategy.ITypeBlockProductionStrategy;
import com.zlt.aps.lh.engine.strategy.support.DailyMachineExpansionPlanner;
import com.zlt.aps.lh.engine.strategy.support.DailyMachineShortageQuotaPlan;
import com.zlt.aps.lh.engine.strategy.support.DailyQuotaLedgerBaseline;
import com.zlt.aps.lh.engine.strategy.support.DayTypeBlockReverseSelectionDirective;
import com.zlt.aps.lh.engine.strategy.support.EarlyProductionChecker;
import com.zlt.aps.lh.engine.strategy.support.EarlyProductionDecision;
import com.zlt.aps.lh.engine.strategy.support.EarlyProductionRuntimePlan;
import com.zlt.aps.lh.engine.strategy.support.FirstInspectionAllocationPlan;
import com.zlt.aps.lh.engine.strategy.support.FirstInspectionShiftAllocation;
import com.zlt.aps.lh.engine.strategy.support.NewSpecEmbryoAvailableTimeResolver;
import com.zlt.aps.lh.engine.strategy.support.PendingSkuUnscheduledRule;
import com.zlt.aps.lh.engine.strategy.support.SpecifiedMachineScheduleResult;
import com.zlt.aps.lh.service.ILhDailyMouldCalcService;
import com.zlt.aps.lh.service.impl.LhMaintenanceScheduleService;
import com.zlt.aps.lh.util.CleaningScheduleRuleUtil;
import com.zlt.aps.lh.util.FirstInspectionQtyUtil;
import com.zlt.aps.lh.util.FirstInspectionAllocationUtil;
import com.zlt.aps.lh.util.TypeBlockRelationUtil;
import com.zlt.aps.lh.util.LeftRightMouldUtil;
import com.zlt.aps.lh.util.LhMachineHardMatchUtil;
import com.zlt.aps.lh.util.LhMouldCodeUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.LhSpecialMaterialUtil;
import com.zlt.aps.lh.util.LhSpecifyMachineUtil;
import com.zlt.aps.lh.util.MachineCleaningOverlapUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.lh.util.ResultDowntimeSummaryUtil;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.lh.util.ShiftProductionControlUtil;
import com.zlt.aps.lh.util.SingleMouldShiftQtyUtil;
import com.zlt.aps.lh.util.SkuDailyPlanQuotaUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
import com.zlt.aps.mdm.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuMouldRel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 换活字块排产子策略。
 *
 * <p>业务定位：</p>
 * <ul>
 *   <li>在 S4.4 续作收尾后，根据机台收尾时间寻找可以更换活字块衔接的新增 SKU；</li>
 *   <li>优先使用收尾机台，必要时使用在机前规格兜底机台；</li>
 *   <li>候选 SKU 需要满足同胎胚、同模具、可更换活字块、机台硬性准入等条件；</li>
 *   <li>结果落地后仍作为排程结果进入统一日计划账本、胎胚库存和后置校验链路。</li>
 * </ul>
 *
 * <p>注意：本类只处理“无需走新增换模主链”的换活字块衔接。若候选物料存在定点机台或新增换模
 * 更适合承接，会回流到 S4.5 新增规格链路。</p>
 *
 * @author APS
 */
@Slf4j
@Component("typeBlockProductionStrategy")
public class TypeBlockProductionStrategy implements ITypeBlockProductionStrategy {

    private static final String CONTINUOUS_SCHEDULE_TYPE = ScheduleTypeEnum.CONTINUOUS.getCode();
    private static final String TYPE_BLOCK_DRY_ICE_CLEANING_ANALYSIS = "干冰清洗+换活字块";
    private static final String TYPE_BLOCK_SAND_BLAST_CLEANING_ANALYSIS = "喷砂清洗+换活字块";
    private static final String TYPE_BLOCK_TRIGGER_ENDING = "收尾触发";
    private static final String TYPE_BLOCK_TRIGGER_FIRST_DAY_NO_PLAN_RELEASE =
            "续作首日无计划释放触发";
    private static final String TYPE_BLOCK_TRIGGER_SMALL_ENDING_SURPLUS_RELEASE =
            "续作收尾小余量释放触发";
    private static final String TYPE_BLOCK_TRIGGER_CONTINUOUS_RELEASE =
            "续作释放触发";
    private static final String TYPE_BLOCK_TRIGGER_FALLBACK = "在机前规格兜底触发";
    private static final String TYPE_BLOCK_SKIP_REASON_T1_NOT_END =
            "T-1 最新记录未收尾，跳过兜底反查";
    private static final String TYPE_BLOCK_SKIP_REASON_LIMIT_SPECIFY_RESERVED =
            "机台存在需走新增换模链路的定点物料，当前阶段预留给S4.5";
    private static final String SHARED_EMBRYO_ZERO_SURPLUS_UNSCHEDULED_REASON =
            "共用胎胚且硫化余量为0";
    private static final String YES_FLAG = "1";
    private static final String NO_FLAG = "0";
    private static final String AUTO_DATA_SOURCE = "0";
    private static final int TYPE_BLOCK_SWITCH_MAX_ATTEMPTS = 16;

    @Resource
    private OrderNoGenerator orderNoGenerator;
    @Resource
    private IEndingJudgmentStrategy endingJudgmentStrategy;
    @Resource
    private TargetScheduleQtyResolver targetScheduleQtyResolver;
    /** S4.4 与 S4.5 共用的提前生产运行态计划入口。 */
    @Resource
    private EarlyProductionRuntimePlanService earlyProductionRuntimePlanService;
    @Resource
    private LhMaintenanceScheduleService maintenanceScheduleService;
    @Resource
    private IMouldChangeBalanceStrategy mouldChangeBalanceStrategy;
    @Resource
    private IFirstInspectionBalanceStrategy firstInspectionBalanceStrategy;
    @Resource
    private ICapacityCalculateStrategy capacityCalculateStrategy;
    @Resource
    private IMachineMatchStrategy machineMatchStrategy;
    /** 物料+产品状态+自然日目标总机台数唯一查询入口。 */
    @Resource
    private ILhDailyMouldCalcService lhDailyMouldCalcService;
    /** 胶囊次数累计与换胶囊班次扣减统一入口 */
    @Resource
    private CapsuleReplacementRuleService capsuleReplacementRuleService = new CapsuleReplacementRuleService();

    /**
     * 执行换活字块排产。
     *
     * @param context 排程上下文
     */
    @Override
    public void scheduleTypeBlockChange(LhScheduleContext context) {
        log.info("换活字块排产开始, 机台数: {}", context.getMachineScheduleMap().size());

        List<LhShiftConfigVO> shifts = LhScheduleTimeUtil.getScheduleShifts(context, context.getScheduleDate());
        /*
         * S4.4 的提前生产机台数门禁必须先纳入 S4.1～S4.3 已落地的续作结果。
         * 该口径与 S4.5 共用上下文重建方法，避免换活字块阶段把已占用机台数误读为0。
         */
        int scheduledMachineRecordDateCount =
                context.rebuildScheduledMachineCountMaps(shifts);
        log.info("换活字块已排机台统计重建完成, factoryCode: {}, batchNo: {}, "
                        + "resultCount: {}, recordDateCount: {}",
                context.getFactoryCode(), context.getBatchNo(),
                context.getScheduleResultList().size(), scheduledMachineRecordDateCount);

        // 基于续作收尾回写后的真实收尾时间，按机台收尾先后衔接换活字块。
        // 只有已标记收尾且有预计完工时刻的机台，才代表当前活字块可切换到下一规格。
        List<MachineScheduleDTO> endingMachines = context.getMachineScheduleMap().values().stream()
                .filter(m -> isTypeBlockEligibleMachine(context, m)
                        && !context.isContinuousStopHoldMachine(m.getMachineCode()))
                .collect(Collectors.toList());
        endingMachines.sort(Comparator.comparing(MachineScheduleDTO::getEstimatedEndTime));
        Map<String, String> machineTriggerSourceMap = new HashMap<>(Math.max(16, endingMachines.size() * 2));
        List<MachineScheduleDTO> candidateMachines = new ArrayList<>(endingMachines);
        for (MachineScheduleDTO endingMachine : endingMachines) {
            machineTriggerSourceMap.put(endingMachine.getMachineCode(), TYPE_BLOCK_TRIGGER_ENDING);
        }

        List<MachineScheduleDTO> releasedMachines = resolveReleasedTypeBlockMachines(context);
        releasedMachines.sort(Comparator.comparing(
                MachineScheduleDTO::getEstimatedEndTime, Comparator.nullsLast(Comparator.naturalOrder())));
        for (MachineScheduleDTO releasedMachine : releasedMachines) {
            String machineCode = releasedMachine.getMachineCode();
            if (StringUtils.isEmpty(machineCode) || machineTriggerSourceMap.containsKey(machineCode)) {
                continue;
            }
            candidateMachines.add(releasedMachine);
            String triggerSource = resolveReleasedTypeBlockTriggerSource(context, machineCode);
            machineTriggerSourceMap.put(machineCode, triggerSource);
        }

        List<MachineScheduleDTO> fallbackMachines = resolveTypeBlockFallbackMachines(context);
        fallbackMachines.sort(Comparator.comparing(MachineScheduleDTO::getEstimatedEndTime));
        for (MachineScheduleDTO fallbackMachine : fallbackMachines) {
            String machineCode = fallbackMachine.getMachineCode();
            if (StringUtils.isEmpty(machineCode) || machineTriggerSourceMap.containsKey(machineCode)) {
                continue;
            }
            candidateMachines.add(fallbackMachine);
            machineTriggerSourceMap.put(machineCode, TYPE_BLOCK_TRIGGER_FALLBACK);
        }
        // 换活字块只处理窗口结束前已真实收尾或释放的机台；窗口上界之外的机台保留给 S4.5 新增排产。
        candidateMachines = filterScheduleWindowEndingMachines(context, candidateMachines);
        for (MachineScheduleDTO candidateMachine : candidateMachines) {
            String triggerSource = machineTriggerSourceMap.get(candidateMachine.getMachineCode());
            if (StringUtils.equals(TYPE_BLOCK_TRIGGER_FIRST_DAY_NO_PLAN_RELEASE, triggerSource)
                    || StringUtils.equals(TYPE_BLOCK_TRIGGER_SMALL_ENDING_SURPLUS_RELEASE, triggerSource)
                    || StringUtils.equals(TYPE_BLOCK_TRIGGER_CONTINUOUS_RELEASE, triggerSource)) {
                log.info("续作释放机台进入换活字块匹配, machineCode: {}, currentMaterialCode: {}, triggerSource: {}",
                        candidateMachine.getMachineCode(), candidateMachine.getCurrentMaterialCode(), triggerSource);
            }
        }
        traceEndingMachineOrder(context, candidateMachines, machineTriggerSourceMap);
        log.info("换活字块候选机台准备完成, 收尾机台: {}, 续作释放机台: {}, 兜底机台: {}, 候选机台: {}, 待排新增SKU: {}",
                endingMachines.size(), releasedMachines.size(), fallbackMachines.size(), candidateMachines.size(),
                context.getNewSpecSkuList().size());

        // completedMachineMap 记录本轮不再尝试的机台，避免同一机台在一轮中反复失败重试。
        Map<String, Boolean> completedMachineMap = new HashMap<>(Math.max(16, candidateMachines.size() * 2));
        int typeBlockScheduledCount = 0;
        while (!CollectionUtils.isEmpty(context.getNewSpecSkuList())) {
            List<MachineScheduleDTO> activeMachines = buildActiveMachineList(
                    context, candidateMachines, machineTriggerSourceMap, completedMachineMap);
            if (CollectionUtils.isEmpty(activeMachines)) {
                log.warn("换活字块无可继续尝试机台, 待排新增SKU: {}, 已完成机台: {}",
                        context.getNewSpecSkuList().size(), completedMachineMap.size());
                break;
            }
            activeMachines.sort((leftMachine, rightMachine) -> compareTypeBlockMachine(
                    context, leftMachine, rightMachine, machineTriggerSourceMap));

            boolean scheduledInCurrentRound = false;
            for (MachineScheduleDTO machine : activeMachines) {
                String machineCode = machine.getMachineCode();
                SkuScheduleDTO limitSpecifySku = selectLimitSpecifySkuByMachine(context, machine);
                // 定点物料若更适合走新增换模主链，则当前收尾机台预留给 S4.5，不在 S4.4 抢先换活字块。
                if (shouldReserveMachineForSpecifyNewSpec(context, machine, limitSpecifySku, shifts)) {
                    completedMachineMap.put(machineCode, true);
                    log.info("收尾机台预留给定点物料新增换模链路, machineCode: {}, materialCode: {}",
                            machineCode, limitSpecifySku.getMaterialCode());
                    continue;
                }
                SkuScheduleDTO specifySku = isTypeBlockCandidate(context, machine, limitSpecifySku)
                        ? limitSpecifySku : null;
                if (specifySku != null && appendSpecifyTypeBlockResult(
                        context, machine, specifySku, shifts, completedMachineMap, activeMachines)) {
                    clearSpecifyReservation(context, machineCode, specifySku.getMaterialCode());
                    scheduledInCurrentRound = true;
                    typeBlockScheduledCount++;
                    break;
                }

                // 基于同胎胚、同模具、同主花纹等条件筛选可换活字块候选。
                // 该阶段只允许不更换整套模具的轻量衔接，完整换模能力评估必须留给 S4.5 新增主链。
                List<SkuScheduleDTO> typeBlockCandidates = filterTypeBlockCandidates(
                        context, machine);
                if (StringUtils.equals(TYPE_BLOCK_TRIGGER_FIRST_DAY_NO_PLAN_RELEASE,
                        machineTriggerSourceMap.get(machineCode))) {
                    log.info("释放机台换活字块候选SKU列表, machineCode: {}, currentMaterialCode: {}, candidates: {}",
                            machineCode, machine.getCurrentMaterialCode(), buildSkuCodeSummary(typeBlockCandidates));
                }
                String matchedLayer = !CollectionUtils.isEmpty(typeBlockCandidates) ? "同胎胚+同模具" : "未命中";
                if (CollectionUtils.isEmpty(typeBlockCandidates)) {
                    log.debug("换活字块未匹配到SKU, 机台: {}, 触发来源: {}, 候选数: {}",
                            machineCode, machineTriggerSourceMap.get(machineCode),
                            typeBlockCandidates.size());
                    traceTypeBlockDecision(context, machine, typeBlockCandidates,
                            null, matchedLayer, false, null, null, machineTriggerSourceMap.get(machineCode),
                            "未匹配到满足换活字块条件的候选SKU");
                    completedMachineMap.put(machineCode, true);
                    continue;
                }
                boolean precisionPreInsertSearch = getMaintenanceScheduleService()
                        .hasOpenPrecisionPreInsertWindow(machine);
                SkuScheduleDTO typeBlockSku = null;
                boolean success = false;
                for (SkuScheduleDTO candidateSku : typeBlockCandidates) {
                    // 普通换活字块保持历史行为只尝试排序第一名；精度前窗口才按同一既有顺序遍历全部候选。
                    if (!precisionPreInsertSearch && Objects.nonNull(typeBlockSku)) {
                        break;
                    }
                    typeBlockSku = candidateSku;
                    if (shouldReserveMachineForNewSpecPath(context, machine, typeBlockSku, shifts)) {
                        log.info("候选SKU需走新增换模主链，当前阶段不执行换活字块, machineCode: {}, materialCode: {}",
                                machineCode, typeBlockSku.getMaterialCode());
                        if (precisionPreInsertSearch) {
                            continue;
                        }
                        break;
                    }
                    if (endingJudgmentStrategy.isCurrentWindowEnding(context, typeBlockSku)) {
                        getMaintenanceScheduleService().tryAttachMaintenanceAfterFirstEnding(
                                context, machine, machine.getEstimatedEndTime());
                    }
                    Date typeBlockSwitchStartTime = allocateTypeBlockSwitchStartTime(
                            context, machine, typeBlockSku, machine.getEstimatedEndTime());
                    Date typeBlockStartTime = resolveTypeBlockProductionStartTime(
                            context, machine, typeBlockSku, machine.getEstimatedEndTime(),
                            typeBlockSwitchStartTime, shifts);
                    int eligibleMachineCount = countEligibleTypeBlockMachines(
                            context, typeBlockSku, activeMachines);
                    StringBuilder failureReason = new StringBuilder(128);
                    success = appendTypeBlockResultWithRollback(
                            context, machine, typeBlockSku, typeBlockStartTime,
                            typeBlockSwitchStartTime, shifts,
                            eligibleMachineCount == 1, failureReason);
                    traceTypeBlockDecision(context, machine, typeBlockCandidates,
                            typeBlockSku, matchedLayer, success, typeBlockSwitchStartTime,
                            typeBlockStartTime, machineTriggerSourceMap.get(machineCode),
                            failureReason.toString());
                    if (success) {
                        break;
                    }
                    log.warn("换活字块排产失败, 机台: {}, materialCode: {}, 结构: {}, 开始时间: {}, "
                                    + "匹配层级: {}, 失败原因: {}, 是否继续精度前候选: {}",
                            machineCode, typeBlockSku.getMaterialCode(), typeBlockSku.getStructureName(),
                            LhScheduleTimeUtil.formatDateTime(typeBlockStartTime), matchedLayer,
                            StringUtils.isNotEmpty(failureReason.toString())
                                    ? failureReason.toString() : "-", precisionPreInsertSearch);
                }
                if (!success) {
                    completedMachineMap.put(machineCode, true);
                    continue;
                }
                if (StringUtils.equals(TYPE_BLOCK_TRIGGER_FIRST_DAY_NO_PLAN_RELEASE,
                        machineTriggerSourceMap.get(machineCode))) {
                    log.info("释放机台换活字块最终选中SKU, machineCode: {}, sourceMaterialCode: {}, selectedMaterialCode: {}",
                            machineCode, machine.getPreviousMaterialCode(), typeBlockSku.getMaterialCode());
                }
                scheduledInCurrentRound = true;
                typeBlockScheduledCount++;
                if (!machine.isEnding()) {
                    completedMachineMap.put(machineCode, true);
                }
                // 每轮仅落一条结果，随后按更新后的机台收尾时间重新排序，避免旧排序继续影响后续衔接。
                // 这样同一机台连续换活字块时，会按最新完工时刻重新参与下一轮竞争。
                break;
            }
            if (!scheduledInCurrentRound) {
                log.warn("本轮换活字块未产生排程结果, 候选机台: {}, 待排新增SKU: {}",
                        activeMachines.size(), context.getNewSpecSkuList().size());
                break;
            }
        }
        log.info("换活字块排产结束, 新增结果数: {}, 剩余新增SKU: {}, 当前排程结果数: {}",
                typeBlockScheduledCount, context.getNewSpecSkuList().size(),
                context.getScheduleResultList().size());
    }

    /**
     * 按天换活字块机台反选匹配：给定当天候选机台与当天待排物料，返回稳定有序的机台→物料配对。
     *
     * <p>本方法只做无副作用匹配，不执行时间分配、首检、计数登记或结果写入：</p>
     * <ul>
     *   <li>机台顺序完全复用现有换活字块排序（同业务日下按切换就绪时间→收尾时间→机台编码）；</li>
     *   <li>物料按调用方传入的当天 S4.5 优先级顺序取首位，保持候选判断口径与 S4.4 一致；</li>
     *   <li>每个机台只锁定一个物料，每个物料需求只被一台机台锁定，结果稳定确定；</li>
     *   <li>实际切换时间、首检、班次计划量、机台收尾时间和物料账本仍由 S4.5 新增主链统一计算。</li>
     * </ul>
     *
     * @param context 排程上下文
     * @param scheduleDate 反选所属业务日
     * @param dayMaterials 当天待排物料（已按当天 S4.5 优先级排序，不含提前生产物料）
     * @param dayMachines 当天候选机台（已通过现有硬性过滤且可开产时间落在当天）
     * @return 稳定有序的按天换活字块反选指令；无匹配时返回空列表
     */
    @Override
    public List<DayTypeBlockReverseSelectionDirective> matchDayTypeBlockReversePairs(
            LhScheduleContext context,
            LocalDate scheduleDate,
            List<SkuScheduleDTO> dayMaterials,
            List<MachineScheduleDTO> dayMachines) {
        if (Objects.isNull(context) || Objects.isNull(scheduleDate)
                || CollectionUtils.isEmpty(dayMaterials) || CollectionUtils.isEmpty(dayMachines)) {
            return Collections.emptyList();
        }
        // 机台顺序沿用现有换活字块排序；同业务日下结束业务日一致，触发来源统一按默认值，
        // 实际比较退化为“切换就绪时间→收尾时间→机台编码”，与 S4.4 窗口内同日竞争口径一致。
        List<MachineScheduleDTO> orderedMachines = new ArrayList<MachineScheduleDTO>(dayMachines);
        orderedMachines.sort((leftMachine, rightMachine) -> compareTypeBlockMachine(
                context, leftMachine, rightMachine, Collections.<String, String>emptyMap()));
        // 物料顺序由调用方按当天 S4.5 优先级传入，本方法保持该顺序不做二次排序。
        Set<String> lockedMaterialKeySet = new LinkedHashSet<String>(dayMaterials.size());
        List<DayTypeBlockReverseSelectionDirective> result =
                new ArrayList<DayTypeBlockReverseSelectionDirective>(
                        Math.min(dayMachines.size(), dayMaterials.size()));
        for (MachineScheduleDTO machine : orderedMachines) {
            if (Objects.isNull(machine) || StringUtils.isEmpty(machine.getMachineCode())) {
                continue;
            }
            for (SkuScheduleDTO sku : dayMaterials) {
                if (Objects.isNull(sku) || StringUtils.isEmpty(sku.getMaterialCode())
                        || sku.resolveTargetScheduleQty() <= 0) {
                    continue;
                }
                // 每个物料需求只被一台机台反选锁定，避免同一物料重复占用多个预留机台。
                String materialStatusKey = MonthPlanDateResolver.buildMaterialStatusKey(
                        sku.getMaterialCode(), sku.getProductStatus());
                if (lockedMaterialKeySet.contains(materialStatusKey)) {
                    continue;
                }
                // 完全复用现有换活字块候选判断：同胎胚同模具 + 机台硬性准入 + 当前物料不相同。
                if (!isTypeBlockCandidate(context, machine, sku, false)) {
                    continue;
                }
                DayTypeBlockReverseSelectionDirective directive =
                        new DayTypeBlockReverseSelectionDirective();
                directive.setScheduleDate(scheduleDate);
                directive.setMachineCode(machine.getMachineCode());
                directive.setPreviousMaterialCode(machine.getCurrentMaterialCode());
                directive.setMaterialCode(sku.getMaterialCode());
                directive.setProductStatus(sku.getProductStatus());
                directive.setSkuSortRank(sku.getSortRank());
                directive.setMatchedLayer("同胎胚+同模具");
                result.add(directive);
                lockedMaterialKeySet.add(materialStatusKey);
                break;
            }
        }
        return result;
    }

    /**
     * 在历史指定机台上尝试换活字块排产。
     *
     * <p>历史计划的交替类型只用于检索范围，不直接继承。本方法先按当前机台物料、胎胚、
     * 模具和活字块关系重新判断实际是否属于换活字块；不满足时返回“不适用”，由反选策略
     * 转交新增换模主链。满足后继续复用现有切换资源、首检、班次产能、结果构建及账本更新。
     * 历史映射班次是切换开始班次硬约束，历史具体时间不会参与计算。</p>
     *
     * @param context 排程上下文
     * @param machine 历史指定机台
     * @param sku 当前实际账本选中的SKU状态
     * @param mappedShiftIndex 历史班次映射后的当前班次
     * @return 指定机台换活字块执行结果
     */
    @Override
    public SpecifiedMachineScheduleResult tryScheduleSpecifiedMachine(
            LhScheduleContext context,
            MachineScheduleDTO machine,
            SkuScheduleDTO sku,
            int mappedShiftIndex) {
        if (Objects.isNull(context) || Objects.isNull(machine) || Objects.isNull(sku)) {
            return SpecifiedMachineScheduleResult.failed(
                    MouldChangeTypeEnum.TYPE_BLOCK.getCode(),
                    "排程上下文、指定机台或目标SKU为空");
        }
        if (!isTypeBlockCandidate(context, machine, sku)) {
            return SpecifiedMachineScheduleResult.notApplicable("当前机台物料与目标SKU不满足换活字块条件");
        }
        LhShiftConfigVO mappedShift = LhScheduleTimeUtil.getShiftByIndex(
                context, context.getScheduleDate(), mappedShiftIndex);
        if (Objects.isNull(mappedShift) || Objects.isNull(mappedShift.getShiftStartDateTime())
                || Objects.isNull(mappedShift.getShiftEndDateTime())) {
            return SpecifiedMachineScheduleResult.failed(
                    MouldChangeTypeEnum.TYPE_BLOCK.getCode(),
                    "历史班次无法映射到本批次有效班次");
        }
        Date machineEndTime = machine.getEstimatedEndTime();
        if (Objects.isNull(machineEndTime)) {
            return SpecifiedMachineScheduleResult.failed(
                    MouldChangeTypeEnum.TYPE_BLOCK.getCode(),
                    "历史指定机台没有可用于重新计算的收尾时间");
        }
        Date alignedEndTime = machineEndTime.before(mappedShift.getShiftStartDateTime())
                ? mappedShift.getShiftStartDateTime() : machineEndTime;
        if (!alignedEndTime.before(mappedShift.getShiftEndDateTime())) {
            return SpecifiedMachineScheduleResult.failed(
                    MouldChangeTypeEnum.TYPE_BLOCK.getCode(),
                    "历史指定机台在映射班次内已无可用切换时间");
        }

        List<LhShiftConfigVO> shifts =
                LhScheduleTimeUtil.getScheduleShifts(context, context.getScheduleDate());
        int resultStartIndex = context.getScheduleResultList().size();
        int unscheduledStartIndex = context.getUnscheduledResultList().size();
        boolean pendingBefore = context.getNewSpecSkuList().contains(sku);
        Integer originalTargetQty = sku.getTargetScheduleQty();
        Integer originalRemainingQty = sku.getRemainingScheduleQty();

        Date switchStartTime = allocateTypeBlockSwitchStartTime(
                context, machine, sku, alignedEndTime);
        if (Objects.isNull(switchStartTime)
                || switchStartTime.before(mappedShift.getShiftStartDateTime())
                || !switchStartTime.before(mappedShift.getShiftEndDateTime())) {
            if (Objects.nonNull(switchStartTime)) {
                getMouldChangeBalanceStrategy().rollbackMouldChange(context, switchStartTime);
            }
            return SpecifiedMachineScheduleResult.failed(
                    MouldChangeTypeEnum.TYPE_BLOCK.getCode(),
                    "按本批机台状态重新分配后，换活字块开始时间未落在历史映射班次");
        }
        Date productionStartTime = resolveTypeBlockProductionStartTime(
                context, machine, sku, alignedEndTime, switchStartTime, shifts);
        StringBuilder failureReason = new StringBuilder(128);
        boolean success = appendTypeBlockResultWithRollback(
                context, machine, sku, productionStartTime, switchStartTime, shifts,
                true, failureReason);
        if (!success) {
            /*
             * 指定机台失败只代表本次反选失败，不得提前写入未排或把SKU移出普通新增队列。
             * 换活字块主链已经回滚切换配额，这里只恢复其可能写入的待排上下文。
             */
            clearAddedUnscheduledResults(context, unscheduledStartIndex);
            sku.setTargetScheduleQty(originalTargetQty);
            sku.setRemainingScheduleQty(originalRemainingQty);
            if (pendingBefore && !context.getNewSpecSkuList().contains(sku)) {
                context.getNewSpecSkuList().add(sku);
            }
            return SpecifiedMachineScheduleResult.failed(
                    MouldChangeTypeEnum.TYPE_BLOCK.getCode(),
                    StringUtils.isNotEmpty(failureReason.toString())
                            ? failureReason.toString() : "换活字块主链未生成有效排程结果");
        }
        LhScheduleResult result = findSpecifiedTypeBlockResult(
                context, machine, sku, resultStartIndex);
        if (Objects.isNull(result)) {
            return SpecifiedMachineScheduleResult.failed(
                    MouldChangeTypeEnum.TYPE_BLOCK.getCode(),
                    "换活字块主链返回成功但未找到对应排程结果");
        }
        return SpecifiedMachineScheduleResult.success(
                result, MouldChangeTypeEnum.TYPE_BLOCK.getCode());
    }

    /**
     * 在特殊材料置换选定的续作机台上尝试换活字块排产。
     *
     * <p>候选关系、切换均衡、20:00 后禁换模、设备停机、首检、班次产能和账本扣减全部复用
     * 正式换活字块主链。与历史反选不同，本入口不锁死某一个班次，允许现有切换规则从
     * {@code earliestSwitchTime} 起继续顺延到下一个合法班次。</p>
     *
     * @param context 排程上下文
     * @param machine 特殊材料准备接管的续作机台
     * @param sku 特殊材料 SKU
     * @param earliestSwitchTime 置换预演得出的最早允许切换时间
     * @return 换活字块执行结果；不满足同胎胚、同模具等关系时返回不适用
     */
    /**
     * 无副作用判断特殊材料置换是否适用换活字块时长。
     *
     * @param context 排程上下文
     * @param machine 特殊材料准备接管的续作机台
     * @param sku 特殊材料 SKU
     * @return true-满足现有换活字块关系；false-应按正规换模预演
     */
    @Override
    public boolean isSpecialMaterialSubstitutionTypeBlockApplicable(
            LhScheduleContext context,
            MachineScheduleDTO machine,
            SkuScheduleDTO sku) {
        // 只复用现有候选判断且关闭决策日志，不执行时间分配、计数登记或结果写入。
        return Objects.nonNull(context)
                && Objects.nonNull(machine)
                && Objects.nonNull(sku)
                && isTypeBlockCandidate(context, machine, sku, false);
    }

    @Override
    public SpecifiedMachineScheduleResult tryScheduleSpecialMaterialSubstitution(
            LhScheduleContext context,
            MachineScheduleDTO machine,
            SkuScheduleDTO sku,
            Date earliestSwitchTime) {
        if (Objects.isNull(context) || Objects.isNull(machine) || Objects.isNull(sku)
                || Objects.isNull(earliestSwitchTime)) {
            return SpecifiedMachineScheduleResult.failed(
                    MouldChangeTypeEnum.TYPE_BLOCK.getCode(),
                    "排程上下文、指定机台、目标SKU或最早切换时间为空");
        }
        if (!isTypeBlockCandidate(context, machine, sku)) {
            return SpecifiedMachineScheduleResult.notApplicable(
                    "当前机台物料与特殊材料SKU不满足换活字块条件");
        }
        Date machineEndTime = machine.getEstimatedEndTime();
        if (Objects.isNull(machineEndTime)) {
            return SpecifiedMachineScheduleResult.failed(
                    MouldChangeTypeEnum.TYPE_BLOCK.getCode(),
                    "特殊材料置换机台没有可用于计算的续作下机时间");
        }
        Date alignedEndTime = machineEndTime.before(earliestSwitchTime)
                ? earliestSwitchTime : machineEndTime;
        int resultStartIndex = context.getScheduleResultList().size();
        int unscheduledStartIndex = context.getUnscheduledResultList().size();
        boolean pendingBefore = context.getNewSpecSkuList().contains(sku);
        Integer originalTargetQty = sku.getTargetScheduleQty();
        Integer originalRemainingQty = sku.getRemainingScheduleQty();

        // 调用正式切换分配入口，由现有规则决定早班、中班或顺延到下一允许班次。
        Date switchStartTime = allocateTypeBlockSwitchStartTime(
                context, machine, sku, alignedEndTime);
        if (Objects.isNull(switchStartTime)) {
            return SpecifiedMachineScheduleResult.failed(
                    MouldChangeTypeEnum.TYPE_BLOCK.getCode(),
                    "特殊材料置换未找到合法换活字块时间");
        }
        List<LhShiftConfigVO> shifts =
                LhScheduleTimeUtil.getScheduleShifts(context, context.getScheduleDate());
        Date productionStartTime = resolveTypeBlockProductionStartTime(
                context, machine, sku, alignedEndTime, switchStartTime, shifts);
        StringBuilder failureReason = new StringBuilder(128);
        boolean success = appendTypeBlockResultWithRollback(
                context, machine, sku, productionStartTime, switchStartTime, shifts,
                true, failureReason);
        if (!success) {
            // 正式主链已回滚换模/首检配额，此处只恢复其可能写入的待排运行态。
            clearAddedUnscheduledResults(context, unscheduledStartIndex);
            sku.setTargetScheduleQty(originalTargetQty);
            sku.setRemainingScheduleQty(originalRemainingQty);
            if (pendingBefore && !context.getNewSpecSkuList().contains(sku)) {
                context.getNewSpecSkuList().add(sku);
            }
            return SpecifiedMachineScheduleResult.failed(
                    MouldChangeTypeEnum.TYPE_BLOCK.getCode(),
                    StringUtils.isNotEmpty(failureReason.toString())
                            ? failureReason.toString() : "换活字块主链未生成有效排程结果");
        }
        LhScheduleResult result = findSpecifiedTypeBlockResult(
                context, machine, sku, resultStartIndex);
        if (Objects.isNull(result)) {
            return SpecifiedMachineScheduleResult.failed(
                    MouldChangeTypeEnum.TYPE_BLOCK.getCode(),
                    "换活字块主链返回成功但未找到特殊材料置换结果");
        }
        return SpecifiedMachineScheduleResult.success(
                result, MouldChangeTypeEnum.TYPE_BLOCK.getCode());
    }

    /**
     * 清除指定机台尝试期间新增的未排结果。
     *
     * @param context 排程上下文
     * @param originalSize 尝试前未排结果数量
     */
    private void clearAddedUnscheduledResults(LhScheduleContext context, int originalSize) {
        if (context.getUnscheduledResultList().size() > originalSize) {
            context.getUnscheduledResultList().subList(
                    originalSize, context.getUnscheduledResultList().size()).clear();
        }
    }

    /**
     * 查找本次指定机台换活字块新生成的主结果。
     *
     * @param context 排程上下文
     * @param machine 指定机台
     * @param sku 目标SKU
     * @param resultStartIndex 尝试前结果数量
     * @return 对应排程结果；未找到返回null
     */
    private LhScheduleResult findSpecifiedTypeBlockResult(LhScheduleContext context,
                                                          MachineScheduleDTO machine,
                                                          SkuScheduleDTO sku,
                                                          int resultStartIndex) {
        for (int index = resultStartIndex; index < context.getScheduleResultList().size(); index++) {
            LhScheduleResult result = context.getScheduleResultList().get(index);
            if (Objects.nonNull(result)
                    && StringUtils.equals(machine.getMachineCode(), result.getLhMachineCode())
                    && StringUtils.equals(sku.getMaterialCode(), result.getMaterialCode())
                    && StringUtils.equals(StringUtils.trimToEmpty(sku.getProductStatus()),
                    StringUtils.trimToEmpty(result.getProductStatus()))) {
                return result;
            }
        }
        return null;
    }

    /**
     * 过滤在排程窗口结束前已真实收尾或释放的换活字块机台。
     *
     * <p>窗口最后班次结束时间为硬上界，收尾时间等于或晚于上界时，换活字块后已无有效生产时间，
     * 必须留给后续排程处理，避免生成越窗结果。</p>
     *
     * @param context 排程上下文
     * @param candidateMachines 原候选机台
     * @return 窗口内仍有换活字块后生产时间的候选机台
     */
    private List<MachineScheduleDTO> filterScheduleWindowEndingMachines(
            LhScheduleContext context, List<MachineScheduleDTO> candidateMachines) {
        if (CollectionUtils.isEmpty(candidateMachines)) {
            return new ArrayList<MachineScheduleDTO>(0);
        }
        List<MachineScheduleDTO> windowEndingMachines = new ArrayList<>(candidateMachines.size());
        List<String> skippedMachineSummaries = new ArrayList<>(candidateMachines.size());
        for (MachineScheduleDTO machine : candidateMachines) {
            if (hasTypeBlockProductionTimeInWindow(context, machine)) {
                windowEndingMachines.add(machine);
                continue;
            }
            skippedMachineSummaries.add(String.format("%s@%s",
                    machine == null ? "-" : machine.getMachineCode(),
                    machine == null ? "-" : LhScheduleTimeUtil.formatDateTime(machine.getEstimatedEndTime())));
        }
        if (!CollectionUtils.isEmpty(skippedMachineSummaries)) {
            log.info("窗口内无换活字块后生产时间的机台跳过, 窗口结束: {}, 过滤前候选: {}, 跳过机台数: {}, 跳过明细: {}",
                    LhScheduleTimeUtil.formatDateTime(resolveScheduleWindowEndTime(context)), candidateMachines.size(),
                    skippedMachineSummaries.size(), String.join(",", skippedMachineSummaries));
        }
        return windowEndingMachines;
    }

    /**
     * 判断机台最新预计收尾或释放时间之后，排程窗口内是否仍有换活字块生产时间。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @return true-窗口内仍有生产时间，false-已到达或越过窗口上界
     */
    private boolean hasTypeBlockProductionTimeInWindow(LhScheduleContext context, MachineScheduleDTO machine) {
        Date windowEndTime = resolveScheduleWindowEndTime(context);
        if (Objects.isNull(windowEndTime)
                || Objects.isNull(machine)
                || Objects.isNull(machine.getEstimatedEndTime())) {
            return false;
        }
        return machine.getEstimatedEndTime().before(windowEndTime);
    }

    /**
     * 解析排程窗口最后一个班次的结束时间。
     *
     * @param context 排程上下文
     * @return 窗口结束时间；班次未初始化时返回 null
     */
    private Date resolveScheduleWindowEndTime(LhScheduleContext context) {
        if (Objects.isNull(context) || CollectionUtils.isEmpty(context.getScheduleWindowShifts())) {
            return null;
        }
        List<LhShiftConfigVO> shifts = context.getScheduleWindowShifts();
        LhShiftConfigVO lastShift = shifts.get(shifts.size() - 1);
        return Objects.isNull(lastShift) ? null : lastShift.getShiftEndDateTime();
    }

    /**
     * 构建本轮可尝试的机台列表。
     *
     * @param context 排程上下文
     * @param candidateMachines 候选机台
     * @param machineTriggerSourceMap 机台触发来源
     * @param completedMachineMap 已完成机台
     * @return 可尝试机台
     */
    private List<MachineScheduleDTO> buildActiveMachineList(LhScheduleContext context,
                                                            List<MachineScheduleDTO> candidateMachines,
                                                            Map<String, String> machineTriggerSourceMap,
                                                            Map<String, Boolean> completedMachineMap) {
        List<MachineScheduleDTO> activeMachines = new ArrayList<>(candidateMachines.size());
        for (MachineScheduleDTO machine : candidateMachines) {
            if (machine == null || StringUtils.isEmpty(machine.getMachineCode())) {
                continue;
            }
            String machineCode = machine.getMachineCode();
            if (Boolean.TRUE.equals(completedMachineMap.get(machineCode))) {
                continue;
            }
            if (!hasTypeBlockProductionTimeInWindow(context, machine)) {
                completedMachineMap.put(machineCode, true);
                log.info("机台最新完工时间已到达排程窗口上界，停止连续换活字块并交由新增排产, machineCode: {}, estimatedEndTime: {}, windowEndTime: {}",
                        machineCode, LhScheduleTimeUtil.formatDateTime(machine.getEstimatedEndTime()),
                        LhScheduleTimeUtil.formatDateTime(resolveScheduleWindowEndTime(context)));
                continue;
            }
            activeMachines.add(machine);
        }
        return activeMachines;
    }

    /**
     * 判断机台是否允许进入换活字块衔接候选。
     *
     * <p>机台当前规格本次排程窗口内运行结束即允许衔接下一规格，不要求前物料在
     * SKU 级收尾（结果行 IS_END=1）。避免同胎胚同模具切换因前物料月计划未收尾
     * 被挡在 S4.4 之外，最终由 S4.5 按正规换模（01）误落库。</p>
     *
     * @param context 排程上下文
     * @param machine 当前机台
     * @return true-机台当前规格运行在窗口内结束，允许进入换活字块衔接
     */
    private boolean isTypeBlockEligibleMachine(LhScheduleContext context, MachineScheduleDTO machine) {
        if (Objects.isNull(machine) || Objects.isNull(machine.getEstimatedEndTime())) {
            return false;
        }
        // SKU 级收尾机台直接放行；非 SKU 级收尾但机台当前规格在窗口内运行结束的机台同样放行。
        return machine.isEnding() || hasTypeBlockProductionTimeInWindow(context, machine);
    }

    /**
     * 比较换活字块候选机台顺序。
     *
     * @param leftMachine 左机台
     * @param rightMachine 右机台
     * @param machineTriggerSourceMap 机台触发来源
     * @return 排序结果
     */
    private int compareTypeBlockMachine(LhScheduleContext context,
                                        MachineScheduleDTO leftMachine,
                                        MachineScheduleDTO rightMachine,
                                        Map<String, String> machineTriggerSourceMap) {
        /*
         * 换活字块首先按机台真实收尾所属业务日竞争：T 日优先于 T+1，T+1 优先于 T+2。
         * 同一业务日内继续沿用既有触发来源、切换就绪时间和机台顺序，避免改变原日内语义。
         */
        LocalDate leftEndingBusinessDate =
                this.resolveTypeBlockEndingBusinessDate(context, leftMachine);
        LocalDate rightEndingBusinessDate =
                this.resolveTypeBlockEndingBusinessDate(context, rightMachine);
        int endingBusinessDateCompare = this.compareNullableBusinessDate(
                leftEndingBusinessDate, rightEndingBusinessDate);
        if (endingBusinessDateCompare != 0) {
            return endingBusinessDateCompare;
        }
        String leftTriggerSource = machineTriggerSourceMap.get(leftMachine.getMachineCode());
        String rightTriggerSource = machineTriggerSourceMap.get(rightMachine.getMachineCode());
        int triggerOrderCompare = Integer.compare(
                resolveTypeBlockTriggerOrder(leftTriggerSource),
                resolveTypeBlockTriggerOrder(rightTriggerSource));
        if (triggerOrderCompare != 0) {
            return triggerOrderCompare;
        }
        Date leftReadyTime = resolveTypeBlockSortReadyTime(context, leftMachine);
        Date rightReadyTime = resolveTypeBlockSortReadyTime(context, rightMachine);
        if (leftReadyTime == null && rightReadyTime != null) {
            return 1;
        }
        if (leftReadyTime != null && rightReadyTime == null) {
            return -1;
        }
        if (leftReadyTime != null && rightReadyTime != null) {
            int readyTimeCompare = leftReadyTime.compareTo(rightReadyTime);
            if (readyTimeCompare != 0) {
                return readyTimeCompare;
            }
        }
        Date leftEndTime = leftMachine.getEstimatedEndTime();
        Date rightEndTime = rightMachine.getEstimatedEndTime();
        if (leftEndTime == null && rightEndTime == null) {
            return compareMachineIdentity(leftMachine, rightMachine);
        }
        if (leftEndTime == null) {
            return 1;
        }
        if (rightEndTime == null) {
            return -1;
        }
        int endTimeCompare = leftEndTime.compareTo(rightEndTime);
        if (endTimeCompare != 0) {
            return endTimeCompare;
        }
        return compareMachineIdentity(leftMachine, rightMachine);
    }

    /**
     * 解析机台预计收尾时刻所属的排程业务日。
     *
     * <p>夜班跨自然日时必须以班次 workDate 为准；恰好落在 06:00、14:00、22:00
     * 边界的时刻归入下一班，但前后班的业务日口径保持项目现有班次配置。</p>
     *
     * @param context 排程上下文
     * @param machine 候选机台
     * @return 收尾所属业务日；无法命中班次时按自然日返回
     */
    private LocalDate resolveTypeBlockEndingBusinessDate(
            LhScheduleContext context,
            MachineScheduleDTO machine) {
        if (Objects.isNull(machine) || Objects.isNull(machine.getEstimatedEndTime())) {
            return null;
        }
        Date endingTime = machine.getEstimatedEndTime();
        if (Objects.nonNull(context) && !CollectionUtils.isEmpty(context.getScheduleWindowShifts())) {
            for (LhShiftConfigVO shift : context.getScheduleWindowShifts()) {
                if (Objects.isNull(shift)
                        || Objects.isNull(shift.getShiftStartDateTime())
                        || Objects.isNull(shift.getShiftEndDateTime())
                        || Objects.isNull(shift.getWorkDate())) {
                    continue;
                }
                if (!endingTime.before(shift.getShiftStartDateTime())
                        && endingTime.before(shift.getShiftEndDateTime())) {
                    return shift.getWorkDate().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate();
                }
            }
        }
        LocalDate endingDate = endingTime.toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        if (Objects.nonNull(context) && !CollectionUtils.isEmpty(context.getScheduleWindowShifts())) {
            LhShiftConfigVO firstShift = context.getScheduleWindowShifts().get(0);
            if (Objects.nonNull(firstShift) && Objects.nonNull(firstShift.getWorkDate())) {
                LocalDate windowStartDate = firstShift.getWorkDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();
                // T 前已释放的兜底机台统一视为 T 日候选，不得形成额外的 T-1 排序层级。
                if (endingDate.isBefore(windowStartDate)) {
                    return windowStartDate;
                }
            }
        }
        return endingDate;
    }

    /**
     * 比较可空业务日期，空日期排在有效日期之后。
     *
     * @param leftDate 左业务日
     * @param rightDate 右业务日
     * @return 比较结果
     */
    private int compareNullableBusinessDate(LocalDate leftDate, LocalDate rightDate) {
        if (Objects.isNull(leftDate) && Objects.isNull(rightDate)) {
            return 0;
        }
        if (Objects.isNull(leftDate)) {
            return 1;
        }
        if (Objects.isNull(rightDate)) {
            return -1;
        }
        return leftDate.compareTo(rightDate);
    }

    /**
     * 追加定点物料换活字块结果。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param specifySku 定点物料
     * @param shifts 班次
     * @param completedMachineMap 已完成机台
     * @param activeMachines 当前轮可尝试机台
     * @return true-追加成功
     */
    private boolean appendSpecifyTypeBlockResult(LhScheduleContext context,
                                                 MachineScheduleDTO machine,
                                                 SkuScheduleDTO specifySku,
                                                 List<LhShiftConfigVO> shifts,
                                                 Map<String, Boolean> completedMachineMap,
                                                 List<MachineScheduleDTO> activeMachines) {
        Date typeBlockSwitchStartTime = allocateTypeBlockSwitchStartTime(
                context, machine, specifySku, machine.getEstimatedEndTime());
        Date typeBlockStartTime = resolveTypeBlockProductionStartTime(
                context, machine, specifySku, machine.getEstimatedEndTime(), typeBlockSwitchStartTime, shifts);
        int eligibleMachineCount = countEligibleTypeBlockMachines(context, specifySku, activeMachines);
        StringBuilder failureReason = new StringBuilder(128);
        boolean success = appendTypeBlockResultWithRollback(
                context, machine, specifySku, typeBlockStartTime, typeBlockSwitchStartTime, shifts,
                eligibleMachineCount == 1, failureReason);
        if (success) {
            if (!machine.isEnding()) {
                completedMachineMap.put(machine.getMachineCode(), true);
            }
            log.info("收尾机台命中定点物料衔接, machineCode: {}, materialCode: {}, startTime: {}",
                    machine.getMachineCode(), specifySku.getMaterialCode(),
                    LhScheduleTimeUtil.formatDateTime(typeBlockStartTime));
            return true;
        }
        log.debug("定点物料衔接失败，继续原衔接匹配, machineCode: {}, materialCode: {}, 失败原因: {}",
                machine.getMachineCode(), specifySku.getMaterialCode(),
                StringUtils.isNotEmpty(failureReason.toString()) ? failureReason.toString() : "-");
        return false;
    }

    /**
     * 解析换活字块触发来源排序。
     *
     * @param triggerSource 触发来源
     * @return 排序值
     */
    private int resolveTypeBlockTriggerOrder(String triggerSource) {
        if (StringUtils.equals(TYPE_BLOCK_TRIGGER_ENDING, triggerSource)) {
            return 0;
        }
        if (StringUtils.equals(TYPE_BLOCK_TRIGGER_FIRST_DAY_NO_PLAN_RELEASE, triggerSource)) {
            return 1;
        }
        if (StringUtils.equals(TYPE_BLOCK_TRIGGER_CONTINUOUS_RELEASE, triggerSource)) {
            return 1;
        }
        if (StringUtils.equals(TYPE_BLOCK_TRIGGER_FALLBACK, triggerSource)) {
            return 2;
        }
        return 3;
    }

    /**
     * 判断机台是否需要预留给需走新增换模链路的定点物料。
     *
     * @param context 排程上下文
     * @param machine 当前机台
     * @param specifySku 定点物料
     * @param shifts 排程窗口班次
     * @return true-当前阶段应预留，false-不预留
     */
    private boolean shouldReserveMachineForSpecifyNewSpec(LhScheduleContext context,
                                                          MachineScheduleDTO machine,
                                                          SkuScheduleDTO specifySku,
                                                          List<LhShiftConfigVO> shifts) {
        if (specifySku == null || !shouldPreferNewSpecPath(context, machine, specifySku)) {
            return false;
        }
        boolean schedulable = canScheduleSpecifySkuByNewSpecPath(
                context, machine, specifySku, shifts, machine.getEstimatedEndTime());
        if (!schedulable) {
            return false;
        }
        log.debug("机台命中需走新增换模链路的定点物料预留, machineCode: {}, materialCode: {}, reason: {}",
                machine.getMachineCode(), specifySku.getMaterialCode(), TYPE_BLOCK_SKIP_REASON_LIMIT_SPECIFY_RESERVED);
        return true;
    }

    /**
     * 判断普通候选是否应预留到新增换模主链处理。
     *
     * @param context 排程上下文
     * @param machine 当前机台
     * @param sku 候选SKU
     * @param shifts 排程窗口班次
     * @return true-当前阶段应预留，false-仍可在S4.4处理
     */
    private boolean shouldReserveMachineForNewSpecPath(LhScheduleContext context,
                                                       MachineScheduleDTO machine,
                                                       SkuScheduleDTO sku,
                                                       List<LhShiftConfigVO> shifts) {
        if (sku == null || !shouldPreferNewSpecPath(context, machine, sku)) {
            return false;
        }
        return canScheduleSpecifySkuByNewSpecPath(
                context, machine, sku, shifts, machine.getEstimatedEndTime());
    }

    /**
     * 选择当前机台配置的限制作业定点SKU。
     *
     * @param context 排程上下文
     * @param machine 当前机台
     * @return 定点SKU，未命中返回null
     */
    private SkuScheduleDTO selectLimitSpecifySkuByMachine(LhScheduleContext context, MachineScheduleDTO machine) {
        if (context == null || machine == null || StringUtils.isEmpty(machine.getMachineCode())
                || CollectionUtils.isEmpty(context.getNewSpecSkuList())) {
            return null;
        }
        String machineCode = machine.getMachineCode();
        for (SkuScheduleDTO sku : context.getNewSpecSkuList()) {
            if (sku == null || StringUtils.isEmpty(sku.getMaterialCode()) || sku.resolveTargetScheduleQty() <= 0) {
                continue;
            }
            if (LhSpecifyMachineUtil.isLimitSpecifyMachine(context, machineCode, sku.getMaterialCode())) {
                return sku;
            }
        }
        return null;
    }

    /**
     * 清理定点机台挤量预留信息。
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @param materialCode 物料编码
     */
    private void clearSpecifyReservation(LhScheduleContext context, String machineCode, String materialCode) {
        if (context == null || StringUtils.isEmpty(machineCode)) {
            return;
        }
        String reservedMaterialCode = context.getSpecifyMachineReservedMaterialMap().get(machineCode);
        if (StringUtils.isEmpty(materialCode) || StringUtils.equals(materialCode, reservedMaterialCode)) {
            context.getSpecifyMachineReservedMaterialMap().remove(machineCode);
            context.getSpecifyMachineReservedSwitchStartTimeMap().remove(machineCode);
        }
    }

    /**
     * 过滤满足换活字块条件的候选SKU。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @return 候选SKU
     */
    private List<SkuScheduleDTO> filterTypeBlockCandidates(LhScheduleContext context,
                                                           MachineScheduleDTO machine) {
        List<SkuScheduleDTO> candidateList = new ArrayList<>(context.getNewSpecSkuList().size());
        for (SkuScheduleDTO sku : context.getNewSpecSkuList()) {
            if (isTypeBlockCandidate(context, machine, sku, false)) {
                candidateList.add(sku);
            }
        }
        return candidateList;
    }

    /**
     * 按月度计划SKU排序结果选择候选首位。
     *
     * @param candidates 候选SKU
     * @return 选中SKU
     */
    private SkuScheduleDTO selectPreferredSkuFromCandidates(List<SkuScheduleDTO> candidates) {
        if (CollectionUtils.isEmpty(candidates)) {
            return null;
        }
        return candidates.get(0);
    }

    /**
     * 判断SKU是否满足换活字块条件：同胎胚且同模具。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param sku SKU
     * @return true-满足条件
     */
    private boolean isTypeBlockCandidate(LhScheduleContext context,
                                         MachineScheduleDTO machine,
                                         SkuScheduleDTO sku) {
        return isTypeBlockCandidate(context, machine, sku, true);
    }

    /**
     * 判断SKU是否满足换活字块条件。
     * <p>条件：同胎胚且同模具，则允许换活字块。</p>
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param sku SKU
     * @param writeDecisionLog 是否输出判断日志
     * @return true-满足条件
     */
    private boolean isTypeBlockCandidate(LhScheduleContext context,
                                         MachineScheduleDTO machine,
                                         SkuScheduleDTO sku,
                                         boolean writeDecisionLog) {
        if (sku == null) {
            return false;
        }
        String machineMaterialCode = Objects.isNull(machine) ? null : machine.getCurrentMaterialCode();
        if (StringUtils.isNotEmpty(machineMaterialCode)
                && StringUtils.equals(machineMaterialCode, sku.getMaterialCode())) {
            log.info("换活字块候选跳过, machineCode: {}, currentMaterialCode: {}, candidateMaterialCode: {}, reason: 当前物料与候选物料相同",
                    machine.getMachineCode(), machineMaterialCode, sku.getMaterialCode());
            return false;
        }
        if (!isMachineHardMatched(context, machine, sku)) {
            log.debug("换活字块候选SKU未通过机台硬性准入, machineCode: {}, materialCode: {}",
                    machine == null ? null : machine.getMachineCode(), sku.getMaterialCode());
            return false;
        }
        return canChangeLetterBlock(context, machine, sku, writeDecisionLog);
    }

    /**
     * 判断机台当前物料与候选SKU是否为相同胎胚。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param sku SKU
     * @return true-相同胎胚
     */
    private boolean isSameEmbryo(LhScheduleContext context, MachineScheduleDTO machine, SkuScheduleDTO sku) {
        return isSameCarcass(context, machine, sku);
    }

    /**
     * 判断机台当前物料与候选SKU是否为相同胎胚描述。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param sku SKU
     * @return true-相同胎胚描述
     */
    private boolean isSameEmbryoDesc(LhScheduleContext context, MachineScheduleDTO machine, SkuScheduleDTO sku) {
        String machineEmbryoDesc = normalizeCompareToken(resolveMachineEmbryoDesc(context, machine));
        String skuEmbryoDesc = normalizeCompareToken(resolveSkuEmbryoDesc(context, sku));
        return StringUtils.isNotEmpty(machineEmbryoDesc)
                && StringUtils.equals(machineEmbryoDesc, skuEmbryoDesc);
    }

    /**
     * 判断机台当前物料与候选SKU是否同胎胚。
     * <p>胎胚代码和胎胚描述只要命中其一即可。</p>
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param sku SKU
     * @return true-同胎胚
     */
    private boolean isSameCarcass(LhScheduleContext context, MachineScheduleDTO machine, SkuScheduleDTO sku) {
        String machineEmbryoCode = normalizeCompareToken(resolveMachineEmbryoCode(context, machine));
        String skuEmbryoCode = normalizeCompareToken(sku == null ? null : sku.getEmbryoCode());
        boolean sameEmbryoCode = StringUtils.isNotEmpty(machineEmbryoCode)
                && StringUtils.equals(machineEmbryoCode, skuEmbryoCode);
        if (sameEmbryoCode) {
            return true;
        }
        String machineEmbryoDesc = normalizeCompareToken(resolveMachineEmbryoDesc(context, machine));
        String skuEmbryoDesc = normalizeCompareToken(resolveSkuEmbryoDesc(context, sku));
        return StringUtils.isNotEmpty(machineEmbryoDesc)
                && StringUtils.equals(machineEmbryoDesc, skuEmbryoDesc);
    }

    /**
     * 判断机台当前物料与候选SKU是否存在相同模具。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param sku SKU
     * @return true-同模具
     */
    private boolean isSameMold(LhScheduleContext context, MachineScheduleDTO machine, SkuScheduleDTO sku) {
        if (machine == null || sku == null) {
            return false;
        }
        Set<String> machineMouldCodeSet = resolveMouldCodeSet(context, machine.getCurrentMaterialCode());
        Set<String> skuMouldCodeSet = resolveMouldCodeSet(context, sku.getMaterialCode());
        if (CollectionUtils.isEmpty(machineMouldCodeSet) || CollectionUtils.isEmpty(skuMouldCodeSet)) {
            return false;
        }
        for (String mouldCode : machineMouldCodeSet) {
            if (skuMouldCodeSet.contains(mouldCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断当前在机SKU与候选SKU是否允许换活字块。
     * <p>条件：同胎胚且同模具，则允许换活字块。</p>
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param sku SKU
     * @param writeDecisionLog 是否输出判断日志
     * @return true-允许换活字块
     */
    private boolean canChangeLetterBlock(LhScheduleContext context,
                                         MachineScheduleDTO machine,
                                         SkuScheduleDTO sku,
                                         boolean writeDecisionLog) {
        String machineEmbryoCode = normalizeCompareToken(resolveMachineEmbryoCode(context, machine));
        String skuEmbryoCode = normalizeCompareToken(sku == null ? null : sku.getEmbryoCode());
        String machineEmbryoDesc = normalizeCompareToken(resolveMachineEmbryoDesc(context, machine));
        String skuEmbryoDesc = normalizeCompareToken(resolveSkuEmbryoDesc(context, sku));
        Set<String> machineMouldCodeSet = machine == null
                ? new LinkedHashSet<>(0) : resolveMouldCodeSet(context, machine.getCurrentMaterialCode());
        Set<String> skuMouldCodeSet = sku == null
                ? new LinkedHashSet<>(0) : resolveMouldCodeSet(context, sku.getMaterialCode());
        boolean sameCarcass = (StringUtils.isNotEmpty(machineEmbryoCode)
                && StringUtils.equals(machineEmbryoCode, skuEmbryoCode))
                || (StringUtils.isNotEmpty(machineEmbryoDesc)
                && StringUtils.equals(machineEmbryoDesc, skuEmbryoDesc));
        boolean sameMold = false;
        if (!CollectionUtils.isEmpty(machineMouldCodeSet) && !CollectionUtils.isEmpty(skuMouldCodeSet)) {
            for (String mouldCode : machineMouldCodeSet) {
                if (skuMouldCodeSet.contains(mouldCode)) {
                    sameMold = true;
                    break;
                }
            }
        }
        // 换活字块关系判定统一走共享工具，保证 S4.4 与 S4.5 使用同一口径。
        boolean matched = TypeBlockRelationUtil.isSameEmbryoAndSameMould(context, machine, sku);
        if (writeDecisionLog) {
            log.info("[换活字块匹配判断] 机台编码: {}, 在机SKU: {}, 候选SKU: {}, 在机胎胚代码: {}, 候选胎胚代码: {}, "
                            + "在机胎胚描述: {}, 候选胎胚描述: {}, 同胎胚: {}, 在机模具号集合: {}, 候选模具号集合: {}, "
                            + "同模具: {}, 是否可换活字块: {}",
                    machine == null ? null : machine.getMachineCode(),
                    machine == null ? null : machine.getCurrentMaterialCode(),
                    sku == null ? null : sku.getMaterialCode(),
                    machineEmbryoCode, skuEmbryoCode, machineEmbryoDesc, skuEmbryoDesc,
                    sameCarcass, machineMouldCodeSet, skuMouldCodeSet, sameMold,
                    matched);
        }
        return matched;
    }

    /**
     * 判断机台当前物料与候选SKU是否为相同主花纹。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param sku SKU
     * @return true-相同主花纹
     */
    private boolean isSameMainPatternStrict(LhScheduleContext context, MachineScheduleDTO machine, SkuScheduleDTO sku) {
        String machineMainPattern = resolveMachineMainPatternStrict(context, machine);
        String skuMainPattern = resolveSkuMainPatternStrict(context, sku);
        // 主花纹按严格口径比较，不回退到普通花纹字段。
        return StringUtils.isNotEmpty(machineMainPattern)
                && StringUtils.equals(machineMainPattern, skuMainPattern);
    }

    /**
     * 判断机台当前物料与候选SKU是否为相同规格。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param sku SKU
     * @return true-相同规格
     */
    private boolean isSameSpec(LhScheduleContext context, MachineScheduleDTO machine, SkuScheduleDTO sku) {
        String machineSpecCode = normalizeCompareToken(resolveMachineSpecCode(context, machine));
        String skuSpecCode = normalizeCompareToken(sku.getSpecCode());
        return StringUtils.isNotEmpty(machineSpecCode)
                && StringUtils.equals(machineSpecCode, skuSpecCode);
    }

    /**
     * 计算换活字块开产时间。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @return 开产时间
     */
    private Date calcTypeBlockStartTime(LhScheduleContext context, MachineScheduleDTO machine) {
        if (machine == null) {
            return null;
        }
        Date switchStartTime = calcTypeBlockSwitchStartTime(context, machine, machine.getEstimatedEndTime());
        return resolveTypeBlockProductionStartTime(context, machine, machine.getEstimatedEndTime(), switchStartTime);
    }

    private Date calcTypeBlockSwitchStartTime(LhScheduleContext context, MachineScheduleDTO machine) {
        if (machine == null) {
            return null;
        }
        return calcTypeBlockSwitchStartTime(context, machine, machine.getEstimatedEndTime());
    }

    /**
     * 基于指定收尾时间计算换活字块开产时间。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param estimatedEndTime 预计收尾时间
     * @return 开产时间
     */
    private Date calcTypeBlockStartTime(LhScheduleContext context,
                                        MachineScheduleDTO machine,
                                        Date estimatedEndTime) {
        if (machine == null || estimatedEndTime == null) {
            return null;
        }
        Date switchStartTime = calcTypeBlockSwitchStartTime(context, machine, estimatedEndTime);
        return resolveTypeBlockProductionStartTime(context, machine, estimatedEndTime, switchStartTime);
    }

    /**
     * 基于指定收尾时间计算换活字块开始时间。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param estimatedEndTime 预计收尾时间
     * @return 换活字块开始时间
     */
    private Date calcTypeBlockSwitchStartTime(LhScheduleContext context,
                                              MachineScheduleDTO machine,
                                              Date estimatedEndTime) {
        if (machine == null || estimatedEndTime == null) {
            return null;
        }
        return allocateTypeBlockSwitchStartTime(context, machine, estimatedEndTime);
    }

    /**
     * 基于指定收尾时间分配换活字块开始时间。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param estimatedEndTime 预计收尾时间
     * @return 换活字块开始时间
     */
    private Date allocateTypeBlockSwitchStartTime(LhScheduleContext context,
                                                  MachineScheduleDTO machine,
                                                  Date estimatedEndTime) {
        return allocateTypeBlockSwitchStartTime(context, machine, null, estimatedEndTime);
    }

    /**
     * 基于指定收尾时间分配换活字块开始时间。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param sku 当前换活字块SKU
     * @param estimatedEndTime 预计收尾时间
     * @return 换活字块开始时间
     */
    private Date allocateTypeBlockSwitchStartTime(LhScheduleContext context,
                                                  MachineScheduleDTO machine,
                                                  SkuScheduleDTO sku,
                                                  Date estimatedEndTime) {
        if (machine == null || estimatedEndTime == null) {
            return null;
        }
        // 传入sku以便试制SKU豁免开产模式限制
        Date switchReadyTime = resolveTypeBlockSwitchReadyTime(context, machine, estimatedEndTime, sku);
        if (switchReadyTime == null) {
            return null;
        }
        int switchDurationHours = resolveTypeBlockSwitchDurationHours(
                context, machine, estimatedEndTime, switchReadyTime);
        return getMouldChangeBalanceStrategy().allocateMouldChange(
                context,
                machine.getMachineCode(),
                switchReadyTime,
                switchDurationHours,
                sku,
                IMouldChangeBalanceStrategy.ACTION_TYPE_BLOCK_CHANGE);
    }

    /**
     * 基于指定收尾时间计算换活字块理论就绪时间。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param estimatedEndTime 预计收尾时间
     * @return 理论可切换时间
     */
    /**
     * 基于指定收尾时间计算换活字块理论就绪时间。
     * <p>试制SKU换活字块同样需在早班完成，不受开产模式限制。</p>
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param estimatedEndTime 预计收尾时间
     * @param sku 当前排产SKU，用于判断是否试制SKU豁免开产模式限制
     * @return 理论可切换时间
     */
    private Date resolveTypeBlockSwitchReadyTime(LhScheduleContext context,
                                                 MachineScheduleDTO machine,
                                                 Date estimatedEndTime,
                                                 SkuScheduleDTO sku) {
        if (machine == null || estimatedEndTime == null) {
            return null;
        }
        Date rawSwitchStartTime = resolveAllowedSwitchStartTime(
                context, machine.getMachineCode(), estimatedEndTime);
        if (rawSwitchStartTime == null) {
            return null;
        }
        Date switchReadyTime;
        if (getMaintenanceScheduleService().shouldApplyMaintenanceOverlapSwitchRule(context, machine, rawSwitchStartTime)) {
            switchReadyTime = getMaintenanceScheduleService().resolveMaintenanceEndTime(context, machine);
        } else {
            switchReadyTime = getMaintenanceScheduleService().delaySwitchStartByMaintenance(
                    machine, rawSwitchStartTime, LhScheduleTimeUtil.getTypeBlockChangeTotalHours(context));
        }
        // 试制SKU换活字块需在早班完成，不受开产模式限制；非试制SKU仍受开产模式约束
        switchReadyTime = ShiftProductionControlUtil.resolveEarliestSwitchStartTime(
                context, switchReadyTime, sku);
        return getMaintenanceScheduleService().delaySwitchStartByMaintenance(
                machine, switchReadyTime, LhScheduleTimeUtil.getTypeBlockChangeTotalHours(context));
    }

    private Date resolveTypeBlockSortReadyTime(LhScheduleContext context, MachineScheduleDTO machine) {
        if (machine == null || machine.getEstimatedEndTime() == null) {
            return null;
        }
        // 排序预览场景不区分SKU类型，传入null使用原有逻辑
        return resolveTypeBlockSwitchReadyTime(context, machine, machine.getEstimatedEndTime(), null);
    }

    private int resolveTypeBlockSwitchDurationHours(LhScheduleContext context,
                                                    MachineScheduleDTO machine,
                                                    Date estimatedEndTime,
                                                    Date switchStartTime) {
        return LhScheduleTimeUtil.getTypeBlockChangeTotalHours(context);
    }

    /**
     * 基于换活字块开始时间计算开产时间。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param estimatedEndTime 预计收尾时间
     * @param switchStartTime 换活字块开始时间
     * @return 开产时间
     */
    private Date resolveTypeBlockProductionStartTime(LhScheduleContext context,
                                                     MachineScheduleDTO machine,
                                                     Date estimatedEndTime,
                                                     Date switchStartTime) {
        if (switchStartTime == null) {
            return null;
        }
        int switchDurationHours = LhScheduleTimeUtil.getTypeBlockChangeTotalHours(context);
        Date switchCompleteTime = LhScheduleTimeUtil.addHours(switchStartTime, switchDurationHours);
        boolean plannedRepairAffectingSwitch = Objects.nonNull(machine)
                && ShiftCapacityResolverUtil.isPlannedRepairAffectingSwitch(
                context, context.getDevicePlanShutList(), machine.getMachineCode(), estimatedEndTime,
                switchStartTime, switchCompleteTime);
        Date productionStartTime;
        if (plannedRepairAffectingSwitch) {
            /*
             * 计划性维修与换活字块允许并行，完成点取两者最大值后完整追加SYS0307009预热。
             * 预热完成时刻只作为正式生产下限；首检时间区间仍以真实换活字块完成点为终点
             * 向前倒推，不再沿用精度保养重叠的额外1小时等待。
             */
            productionStartTime = ShiftCapacityResolverUtil.resolvePlannedRepairProductionReadyTime(
                    context, context.getDevicePlanShutList(), machine.getMachineCode(), estimatedEndTime,
                    switchStartTime, switchCompleteTime);
            log.info("换活字块计划性维修时间轴生效, machineCode: {}, switchStartTime: {}, "
                            + "switchEndTime: {}, preheatMinutes: {}, productionReadyTime: {}, "
                            + "firstInspectionExtraHours: 0",
                    machine.getMachineCode(), LhScheduleTimeUtil.formatDateTime(switchStartTime),
                    LhScheduleTimeUtil.formatDateTime(switchCompleteTime),
                    LhScheduleTimeUtil.getCapsulePreheatMinutes(context),
                    LhScheduleTimeUtil.formatDateTime(productionStartTime));
        } else {
            productionStartTime = switchCompleteTime;
        }
        return resolveCleaningOverlapProductionStartTime(machine, switchStartTime, productionStartTime);
    }

    /**
     * 清洗与换活字块实际重叠时，开产时间取两者最晚结束时间。
     *
     * @param machine 机台
     * @param switchStartTime 换活字块开始时间
     * @param productionStartTime 原换活字块完成后的开产时间
     * @return 合并清洗重叠后的开产时间
     */
    private Date resolveCleaningOverlapProductionStartTime(MachineScheduleDTO machine,
                                                           Date switchStartTime,
                                                           Date productionStartTime) {
        if (Objects.isNull(machine)
                || CollectionUtils.isEmpty(machine.getCleaningWindowList())
                || Objects.isNull(switchStartTime)
                || Objects.isNull(productionStartTime)
                || !switchStartTime.before(productionStartTime)) {
            return productionStartTime;
        }
        Date resolvedStartTime = productionStartTime;
        for (MachineCleaningWindowDTO cleaningWindow : machine.getCleaningWindowList()) {
            if (Objects.isNull(cleaningWindow)
                    || Objects.isNull(cleaningWindow.getCleanStartTime())
                    || Objects.isNull(cleaningWindow.getCleanEndTime())
                    || !cleaningWindow.getCleanStartTime().before(cleaningWindow.getCleanEndTime())) {
                continue;
            }
            // 只有清洗与换活字块实际相交时才并行取最大结束时间，未重叠场景不改变原开产时间。
            if (cleaningWindow.getCleanStartTime().before(productionStartTime)
                    && cleaningWindow.getCleanEndTime().after(switchStartTime)
                    && cleaningWindow.getCleanEndTime().after(resolvedStartTime)) {
                resolvedStartTime = cleaningWindow.getCleanEndTime();
            }
        }
        return resolvedStartTime;
    }

    /**
     * 基于 SKU 施工阶段解析换活字块后的实际开产时间。
     *
     * <p>普通换活字块仍按切换完成班次开产；试制 SKU 若切换完成归属早班，
     * 则生产开始时间后移到同业务日中班，真实换活字块开始/完成时间不挪动。</p>
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param sku SKU
     * @param estimatedEndTime 预计收尾时间
     * @param switchStartTime 换活字块开始时间
     * @param shifts 排程窗口班次
     * @return 开产时间
     */
    private Date resolveTypeBlockProductionStartTime(LhScheduleContext context,
                                                     MachineScheduleDTO machine,
                                                     SkuScheduleDTO sku,
                                                     Date estimatedEndTime,
                                                     Date switchStartTime,
                                                     List<LhShiftConfigVO> shifts) {
        Date defaultProductionStartTime = resolveTypeBlockProductionStartTime(
                context, machine, estimatedEndTime, switchStartTime);
        Date switchCompleteTime = resolveTypeBlockSwitchCompleteTime(
                context, machine, switchStartTime, defaultProductionStartTime);
        Date firstInspectionBaseTime = resolveTypeBlockFirstInspectionBaseTime(
                context, machine, estimatedEndTime, switchStartTime, switchCompleteTime);
        return FirstInspectionQtyUtil.resolveTrialProductionStartTime(
                context, sku, shifts, firstInspectionBaseTime, defaultProductionStartTime,
                ScheduleTypeEnum.TYPE_BLOCK.getCode());
    }

    /**
     * 解析换活字块后试制生产门禁使用的兼容基准时刻。
     * <p>该方法只服务试制固定中班开产例外：命中计划性维修时按
     * max(维修结束, 换活字块结束)+SYS0307009 确定正式生产下限。普通首检数量、计数班次
     * 和跨班分摊不读取该时刻，始终以真实换活字块完成时间为首检区间终点。</p>
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param estimatedEndTime 切换前预计收尾时间
     * @param switchStartTime 换活字块开始时间
     * @param switchCompleteTime 换活字块完成时间
     * @return 试制正式生产门禁的兼容基准时刻
     */
    private Date resolveTypeBlockFirstInspectionBaseTime(LhScheduleContext context,
                                                         MachineScheduleDTO machine,
                                                         Date estimatedEndTime,
                                                         Date switchStartTime,
                                                         Date switchCompleteTime) {
        if (Objects.isNull(machine)
                || !ShiftCapacityResolverUtil.isPlannedRepairAffectingSwitch(
                context, context.getDevicePlanShutList(), machine.getMachineCode(), estimatedEndTime,
                switchStartTime, switchCompleteTime)) {
            return switchCompleteTime;
        }
        return ShiftCapacityResolverUtil.resolvePlannedRepairProductionReadyTime(
                context, context.getDevicePlanShutList(), machine.getMachineCode(), estimatedEndTime,
                switchStartTime, switchCompleteTime);
    }

    /**
     * 解析允许发起换活字块的开始时间。
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @param endingTime 收尾时间
     * @return 允许切换开始时间
     */
    private Date resolveAllowedSwitchStartTime(LhScheduleContext context,
                                               String machineCode,
                                               Date endingTime) {
        if (endingTime == null) {
            return null;
        }
        Date adjustedTime = endingTime;
        for (int attempt = 0; attempt < TYPE_BLOCK_SWITCH_MAX_ATTEMPTS; attempt++) {
            Date downtimeAdjustedTime = resolveDowntimeAdjustedSwitchStartTime(
                    context, machineCode, adjustedTime);
            if (downtimeAdjustedTime.after(adjustedTime)) {
                adjustedTime = downtimeAdjustedTime;
                continue;
            }
            if (!LhScheduleTimeUtil.isNoMouldChangeTime(context, adjustedTime)) {
                return adjustedTime;
            }
            adjustedTime = LhScheduleTimeUtil.resolveNextMorningAfterNoMouldChangeWindow(context, adjustedTime);
        }
        log.warn("换活字块切换起点达到最大尝试次数, 机台: {}, 原始时间: {}",
                machineCode, LhScheduleTimeUtil.formatDateTime(endingTime));
        return adjustedTime;
    }

    private int compareMachineIdentity(MachineScheduleDTO leftMachine, MachineScheduleDTO rightMachine) {
        int machineOrderCompare = Integer.compare(leftMachine.getMachineOrder(), rightMachine.getMachineOrder());
        if (machineOrderCompare != 0) {
            return machineOrderCompare;
        }
        return Comparator.nullsLast(String::compareTo)
                .compare(leftMachine.getMachineCode(), rightMachine.getMachineCode());
    }

    /**
     * 根据停机窗口顺延换活字块切换起点。
     * <p>05计划性维修允许与换活字块并行，实际开产点由统一维修时间轴在切换完成后追加预热；
     * 其他停机类型仍按原逻辑顺延到停机结束。</p>
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @param candidateStartTime 候选开始时间
     * @return 顺延后开始时间
     */
    private Date resolveDowntimeAdjustedSwitchStartTime(LhScheduleContext context,
                                                        String machineCode,
                                                        Date candidateStartTime) {
        if (context == null
                || StringUtils.isEmpty(machineCode)
                || candidateStartTime == null) {
            return candidateStartTime;
        }
        Date candidateEndTime = LhScheduleTimeUtil.addHours(
                candidateStartTime, LhScheduleTimeUtil.getTypeBlockChangeTotalHours(context));
        Date latestOverlapEndTime = null;
        if (!CollectionUtils.isEmpty(context.getDevicePlanShutList())) {
            for (MdmDevicePlanShut planShut : context.getDevicePlanShutList()) {
                if (planShut == null
                        || !StringUtils.equals(machineCode, planShut.getMachineCode())
                        || StringUtils.equals(MachineStopTypeEnum.PLANNED_REPAIR.getCode(),
                        planShut.getMachineStopType())
                        || planShut.getBeginDate() == null
                        || planShut.getEndDate() == null
                        || !planShut.getBeginDate().before(planShut.getEndDate())) {
                    continue;
                }
                if (!candidateStartTime.before(planShut.getEndDate())
                        || !planShut.getBeginDate().before(candidateEndTime)) {
                    continue;
                }
                if (latestOverlapEndTime == null || planShut.getEndDate().after(latestOverlapEndTime)) {
                    latestOverlapEndTime = planShut.getEndDate();
                }
            }
        }
        return latestOverlapEndTime != null ? latestOverlapEndTime : candidateStartTime;
    }

    /**
     * 追加换活字块排程结果。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param sku SKU
     * @param startTime 开产时间
     * @param switchStartTime 换活字块开始时间
     * @param shifts 班次
     * @param structureSwitchEarlyProductionCandidateDate 结构切换提前的原计算开产业务日；其他场景为 null
     * @param isSingleMachine 是否单机换活字块
     * @param failureReason 失败原因收集器
     * @return true-成功
     */
    private boolean appendFollowUpResult(LhScheduleContext context,
                                         MachineScheduleDTO machine,
                                         SkuScheduleDTO sku,
                                         Date startTime,
                                         Date switchStartTime,
                                         List<LhShiftConfigVO> shifts,
                                         LocalDate structureSwitchEarlyProductionCandidateDate,
                                         boolean isSingleMachine,
                                         StringBuilder failureReason) {
        if (startTime == null) {
            recordTypeBlockAppendFailure(failureReason, "换活字块开产时间为空");
            return false;
        }
        MachineScheduleDTO pairMachine = resolveWholeSingleControlTypeBlockPair(context, machine, sku, failureReason);
        boolean wholeSingleControlUnit = Objects.nonNull(pairMachine);
        if (LhSingleControlMachineUtil.isWholeMachineGranularitySku(context, sku)
                && LhSingleControlMachineUtil.isConfiguredSingleControlMachine(context, machine.getMachineCode())
                && !wholeSingleControlUnit) {
            return false;
        }
        if (wholeSingleControlUnit) {
            // 双模换活字块必须等待 L/R 两侧都释放，切换和开产时间统一取两侧可用时间的较晚值。
            Date pairSwitchStartTime = resolveAllowedSwitchStartTime(
                    context, pairMachine.getMachineCode(), pairMachine.getEstimatedEndTime());
            switchStartTime = resolveLaterDate(switchStartTime, pairSwitchStartTime);
            Date pairProductionStartTime = resolveTypeBlockProductionStartTime(
                    context, pairMachine, sku, pairMachine.getEstimatedEndTime(), switchStartTime, shifts);
            startTime = resolveLaterDate(startTime, pairProductionStartTime);
            log.info("双模换活字块等待配对侧释放, primaryMachine: {}, pairMachine: {}, materialCode: {}, "
                            + "primaryReleaseTime: {}, pairReleaseTime: {}, switchStartTime: {}, productionStartTime: {}",
                    machine.getMachineCode(), pairMachine.getMachineCode(), sku.getMaterialCode(),
                    LhScheduleTimeUtil.formatDateTime(machine.getEstimatedEndTime()),
                    LhScheduleTimeUtil.formatDateTime(pairMachine.getEstimatedEndTime()),
                    LhScheduleTimeUtil.formatDateTime(switchStartTime),
                    LhScheduleTimeUtil.formatDateTime(startTime));
            // 配对侧占用判定必须与等待释放后的真实切换窗口对齐：
            // 配对侧在机/续作物料以及切换开始前已结束的其他物料结果都不构成占用，
            // 只有仍占用到切换窗口内的其他物料才阻断双模换活字块。
            if (hasPairSideConflictingAssignment(context, pairMachine, sku, switchStartTime)) {
                recordTypeBlockAppendFailure(failureReason, "双模换活字块配对侧已被其他SKU占用");
                log.info("双模换活字块配对侧在切换窗口内仍被其他SKU占用, primaryMachine: {}, pairMachine: {}, "
                                + "materialCode: {}, switchStartTime: {}, productionStartTime: {}",
                        machine.getMachineCode(), pairMachine.getMachineCode(), sku.getMaterialCode(),
                        LhScheduleTimeUtil.formatDateTime(switchStartTime),
                        LhScheduleTimeUtil.formatDateTime(startTime));
                return false;
            }
        }
        Date earliestEmbryoAvailableTime = null;
        if (Objects.nonNull(structureSwitchEarlyProductionCandidateDate)) {
            /*
             * 调用方仅将结构切换提前候选传入本分支。普通结构提前和结构收尾提前已在
             * 进入结果追加主链前按原基线时序准备运行视图，不启用本次新增的胎胚门禁、
             * 时间下限、首检归属及部分班次产能口径。
             */
            /*
             * 先按理论开产业务日核验共享准入和候选物理机台硬控，再计算胎胚时间下限。
             * 这样即使胎胚时间把实际开产推到有计划日，历史欠产、结构机台数等原条件
             * 也不能随时间一起被“推迟掉”；只读核验不会创建临时日计划账本。
             */
            String earlyProductionRejectReason = this.evaluateTypeBlockEarlyProductionAdmission(
                    context, machine, sku, structureSwitchEarlyProductionCandidateDate,
                    switchStartTime, startTime);
            if (StringUtils.isNotEmpty(earlyProductionRejectReason)) {
                this.recordTypeBlockAppendFailure(
                        failureReason, earlyProductionRejectReason);
                return false;
            }
            Date configuredEarliestEmbryoAvailableTime =
                    NewSpecEmbryoAvailableTimeResolver.resolveEarliestAvailableTime(context, sku);
            earliestEmbryoAvailableTime =
                    NewSpecEmbryoAvailableTimeResolver.resolveEffectiveEarliestAvailableTime(context, sku);
            if (Objects.nonNull(configuredEarliestEmbryoAvailableTime)
                    && Objects.isNull(earliestEmbryoAvailableTime)) {
                log.info("换活字块SKU胎胚最早可供时间因同结构续作已有有效排产而不生效, "
                                + "batchNo: {}, materialCode: {}, structureName: {}, "
                                + "configuredEarliestEmbryoAvailableTime: {}",
                        context.getBatchNo(), sku.getMaterialCode(), sku.getStructureName(),
                        LhScheduleTimeUtil.formatDateTime(configuredEarliestEmbryoAvailableTime));
            }
            Date actualProductionStartTime = this.resolveTypeBlockEarlyProductionStartTime(
                    context, machine, sku, startTime, switchStartTime,
                    earliestEmbryoAvailableTime, shifts);
            if (Objects.isNull(actualProductionStartTime)) {
                this.recordTypeBlockAppendFailure(
                        failureReason,
                        NewSpecEmbryoAvailableTimeResolver.OUT_OF_SCHEDULE_WINDOW_REASON);
                return false;
            }
            startTime = actualProductionStartTime;
            LocalDate actualProductionWorkDate =
                    this.resolveTypeBlockProductionWorkDate(shifts, startTime);
            if (Objects.isNull(actualProductionWorkDate)) {
                this.recordTypeBlockAppendFailure(
                        failureReason, "胎胚时间约束后的换活字块开产业务日无法解析");
                return false;
            }
            int actualDayPlanQty = this.resolveTypeBlockOriginalDayPlanQty(
                    context, sku, actualProductionWorkDate);
            if (actualDayPlanQty <= 0) {
                /*
                 * 实际开产业务日仍无原始 dayN 时，继续使用原共享服务激活临时前移账本；
                 * 若胎胚时间已把生产推到计划日，则只保留理论日只读准入，不得错误前移计划。
                 */
                String actualDateRejectReason = this.prepareTypeBlockEarlyProduction(
                        context, machine, sku, actualProductionWorkDate,
                        switchStartTime, startTime);
                if (StringUtils.isNotEmpty(actualDateRejectReason)) {
                    this.recordTypeBlockAppendFailure(
                            failureReason, actualDateRejectReason);
                    return false;
                }
            }
        }
        // 成型胎胚库存收尾优先按胎胚库存严格控量，避免被零目标或共用胎胚零余量规则提前拦截。
        boolean embryoStockEndingTargetApplied = getTargetScheduleQtyResolver()
                .applyEmbryoStockEndingTargetQtyIfNecessary(context, sku, "换活字块");
        if (sku.resolveTargetScheduleQty() <= 0) {
            recordTypeBlockAppendFailure(failureReason, "换活字块目标量为0");
            log.info("换活字块目标量为0，跳过排产, machineCode: {}, materialCode: {}",
                    machine.getMachineCode(), sku.getMaterialCode());
            return false;
        }
        if (isSharedEmbryoZeroSurplusSku(context, sku)) {
            recordTypeBlockAppendFailure(failureReason, SHARED_EMBRYO_ZERO_SURPLUS_UNSCHEDULED_REASON);
            addSharedEmbryoZeroSurplusUnscheduledResult(context, sku);
            context.getNewSpecSkuList().remove(sku);
            context.removePendingSkuFromStructureMap(sku);
            getTargetScheduleQtyResolver().removeActiveEmbryoSku(
                    context, sku, SHARED_EMBRYO_ZERO_SURPLUS_UNSCHEDULED_REASON);
            log.info("换活字块共用胎胚余量为0，跳过排产并移出待排队列, machineCode: {}, materialCode: {}, "
                            + "embryoCode: {}, surplusQty: {}, embryoStock: {}",
                    machine.getMachineCode(), sku.getMaterialCode(), sku.getEmbryoCode(),
                    sku.getSurplusQty(), sku.getEmbryoStock());
            return false;
        }
        // 换活字块与新增排产共用历史欠产账本口径，并保留“后续无计划强制收尾”的判断结果。
        DailyMachineShortageQuotaPlan shortageQuotaPlan =
                DailyMachineExpansionPlanner.prepareShortageQuota(context, sku, "换活字块");
        // 保存原目标量和严格目标量标识，换活字块单台试算失败时必须完整恢复，不能污染 S4.5 新增排产。
        Integer originalTargetScheduleQty = sku.getTargetScheduleQty();
        int originalRemainingScheduleQty = sku.getRemainingScheduleQty();
        boolean originalStrictTargetQty = sku.isStrictTargetQty();
        // 精度前插排必须一次排完整真实余量，保存换活字块目标调整前的统一生产余量口径。
        int precisionPendingQty = getTargetScheduleQtyResolver()
                .resolveProductionRemainingQty(context, sku);
        boolean isEnding = endingJudgmentStrategy.isCurrentWindowEnding(context, sku);
        boolean smallEndingRuleEnding = isEnding || shortageQuotaPlan.isForceEndingByNoFuturePlan();
        // S4.4 在正式生成换活字块结果前执行同一前置未排规则，避免提前消费本应进入未排的SKU。
        if (handlePendingSkuUnscheduledRuleIfNecessary(context, machine, sku, smallEndingRuleEnding,
                embryoStockEndingTargetApplied, failureReason)) {
            return false;
        }
        boolean typeBlockExpansionContinuation = hasScheduledTypeBlockResult(context, sku);
        applySingleMachineTypeBlockTargetRule(context, machine, sku, startTime, switchStartTime, shifts,
                isEnding, isSingleMachine, typeBlockExpansionContinuation, embryoStockEndingTargetApplied);
        int adoptedTargetQty = sku.resolveTargetScheduleQty();
        int machineMouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(machine);
        sku.setMouldQty(machineMouldQty);
        /*
         * 构建阶段必须直接使用并返回同一份首检时间分摊计划；调用方仅在结果、账本和机台状态
         * 全部落地后写过程日志。使用容量为1的输出集合，避免正式提交时为了日志重新计算一次
         * 首检，导致首检计数或班次产能已变化后出现选机、结果、日志三套口径。
         */
        List<FirstInspectionAllocationPlan> firstInspectionAllocationPlanOutput =
                new ArrayList<FirstInspectionAllocationPlan>(1);
        LhScheduleResult result = buildScheduleResult(
                context, machine, sku, startTime, switchStartTime, shifts,
                machineMouldQty, isEnding, earliestEmbryoAvailableTime,
                firstInspectionAllocationPlanOutput);
        if (result == null || result.getDailyPlanQty() == null || result.getDailyPlanQty() <= 0) {
            int dailyPlanQty = result == null || result.getDailyPlanQty() == null ? 0 : result.getDailyPlanQty();
            recordTypeBlockAppendFailure(failureReason, "换活字块结果班次量为0");
            log.info("换活字块结果班次量为0，跳过落地, machineCode: {}, materialCode: {}, startTime: {}, dailyPlanQty: {}",
                    machine.getMachineCode(), sku.getMaterialCode(),
                    LhScheduleTimeUtil.formatDateTime(startTime), dailyPlanQty);
            if (Objects.nonNull(result)) {
                /*
                 * buildScheduleResult 返回 null 表示首检时间分摊在提交计数前已失败，此时没有
                 * 消费“同班次前2台”序号，不能回滚，否则会误减同机台同班次的既有事件。
                 * 只有已经生成结果但最终班次量被裁为0时，才回滚本次已提交的首检计数。
                 */
                this.rollbackTypeBlockFirstInspectionSequence(
                        context, machine, sku, switchStartTime, startTime,
                        shifts, earliestEmbryoAvailableTime);
            }
            sku.setTargetScheduleQty(originalTargetScheduleQty);
            sku.setRemainingScheduleQty(originalRemainingScheduleQty);
            sku.setStrictTargetQty(originalStrictTargetQty);
            return false;
        }
        result.setScheduleType(ScheduleTypeEnum.TYPE_BLOCK.getCode());
        result.setIsChangeMould(YES_FLAG);
        result.setIsTypeBlock(YES_FLAG);
        result.setMouldCode(resolveTypeBlockActualMouldCode(context, machine, sku));
        // 换活字块虽然不是新增规格换模，但下游换模计划仍按真实切换开始时间生成。
        result.setMouldChangeStartTime(switchStartTime);
        result.setIsEnd(isEnding ? YES_FLAG : NO_FLAG);

        // 换活字块结果即便非收尾，也必须补齐可计算完工时刻，避免结果校验失败。
        Date actualCompletionTime = resolveActualCompletionTime(context, result);
        if (actualCompletionTime == null) {
            recordTypeBlockAppendFailure(failureReason, "换活字块实际完工时间为空");
            log.info("换活字块实际完工时间为空，跳过落地, machineCode: {}, materialCode: {}, startTime: {}, dailyPlanQty: {}",
                    machine.getMachineCode(), sku.getMaterialCode(),
                    LhScheduleTimeUtil.formatDateTime(startTime), result.getDailyPlanQty());
            rollbackTypeBlockFirstInspectionSequence(
                    context, machine, sku, switchStartTime, startTime,
                    shifts, earliestEmbryoAvailableTime);
            return false;
        }
        result.setSpecEndTime(actualCompletionTime);
        result.setTdaySpecEndTime(actualCompletionTime);
        applyTypeBlockCleaningAnalysis(context, result, shifts);

        LhScheduleResult pairResult = wholeSingleControlUnit
                ? buildWholeSingleControlTypeBlockPairResult(
                context, result, pairMachine, sku, machineMouldQty, shifts)
                : null;
        int precisionPlannedQty = wholeSingleControlUnit
                ? ShiftFieldUtil.resolveScheduledQty(result) + ShiftFieldUtil.resolveScheduledQty(pairResult)
                : ShiftFieldUtil.resolveScheduledQty(result);
        Date precisionCompletionTime = actualCompletionTime;
        if (Objects.nonNull(pairResult) && Objects.nonNull(pairResult.getSpecEndTime())
                && pairResult.getSpecEndTime().after(precisionCompletionTime)) {
            precisionCompletionTime = pairResult.getSpecEndTime();
        }
        String precisionRejectReason = getMaintenanceScheduleService()
                .resolvePrecisionCandidateRejectReason(
                        context, machine, sku, precisionPendingQty, precisionPlannedQty,
                        switchStartTime, startTime, precisionCompletionTime);
        if (StringUtils.isNotEmpty(precisionRejectReason)) {
            recordTypeBlockAppendFailure(failureReason, precisionRejectReason);
            rollbackTypeBlockFirstInspectionSequence(
                    context, machine, sku, switchStartTime, startTime,
                    shifts, earliestEmbryoAvailableTime);
            sku.setTargetScheduleQty(originalTargetScheduleQty);
            sku.setRemainingScheduleQty(originalRemainingScheduleQty);
            sku.setStrictTargetQty(originalStrictTargetQty);
            return false;
        }

        // 换活字块结果按日计划账本回裁，收尾严格截断，避免超产。
        // 非收尾正规/量试可保留满班补齐口径；成功后的剩余缺口只做换活字块残量审计。
        DailyQuotaLedgerBaseline precisionQuotaBaseline =
                DailyQuotaLedgerBaseline.capture(context, sku);
        FirstInspectionAllocationPlan committedFirstInspectionAllocationPlan =
                CollectionUtils.isEmpty(firstInspectionAllocationPlanOutput)
                        ? null : firstInspectionAllocationPlanOutput.get(0);
        int quotaTrimmedQty = wholeSingleControlUnit
                ? this.applyWholeSingleControlTypeBlockToDailyQuota(
                        context, sku, result, pairResult, shifts,
                        committedFirstInspectionAllocationPlan)
                : this.applyTypeBlockToDailyQuota(
                        context, sku, result, shifts,
                        committedFirstInspectionAllocationPlan, 1);
        if (quotaTrimmedQty <= 0) {
            recordTypeBlockAppendFailure(failureReason, "换活字块日计划账本回裁后为0");
            log.info("换活字块日计划账本回裁后为0, 跳过落地, machineCode: {}, materialCode: {}, 原排产量: {}",
                    machine.getMachineCode(), sku.getMaterialCode(), result.getDailyPlanQty());
            // 回裁过程可能已经触碰临时前移 dayN、实际余量或胎胚账本，失败时必须完整恢复。
            precisionQuotaBaseline.restore(context, sku);
            rollbackTypeBlockFirstInspectionSequence(
                    context, machine, sku, switchStartTime, startTime,
                    shifts, earliestEmbryoAvailableTime);
            sku.setTargetScheduleQty(originalTargetScheduleQty);
            sku.setRemainingScheduleQty(originalRemainingScheduleQty);
            sku.setStrictTargetQty(originalStrictTargetQty);
            return false;
        }
        int finalPrecisionPlannedQty = wholeSingleControlUnit
                ? ShiftFieldUtil.resolveScheduledQty(result) + ShiftFieldUtil.resolveScheduledQty(pairResult)
                : ShiftFieldUtil.resolveScheduledQty(result);
        Date finalPrecisionCompletionTime = result.getSpecEndTime();
        if (Objects.nonNull(pairResult) && Objects.nonNull(pairResult.getSpecEndTime())
                && (Objects.isNull(finalPrecisionCompletionTime)
                || pairResult.getSpecEndTime().after(finalPrecisionCompletionTime))) {
            finalPrecisionCompletionTime = pairResult.getSpecEndTime();
        }
        String finalPrecisionRejectReason = getMaintenanceScheduleService()
                .resolvePrecisionCandidateRejectReason(
                        context, machine, sku, precisionPendingQty, finalPrecisionPlannedQty,
                        switchStartTime, startTime, finalPrecisionCompletionTime);
        if (StringUtils.isNotEmpty(finalPrecisionRejectReason)) {
            // dayN回裁可能把原本完整的候选缩成部分数量，必须按扣账前快照恢复，
            // 禁止以“少排一点”的方式占用精度前空闲时间。
            precisionQuotaBaseline.restore(context, sku);
            recordTypeBlockAppendFailure(failureReason, finalPrecisionRejectReason);
            rollbackTypeBlockFirstInspectionSequence(
                    context, machine, sku, switchStartTime, startTime,
                    shifts, earliestEmbryoAvailableTime);
            sku.setTargetScheduleQty(originalTargetScheduleQty);
            sku.setRemainingScheduleQty(originalRemainingScheduleQty);
            sku.setStrictTargetQty(originalStrictTargetQty);
            return false;
        }

        /*
         * 结果和机台分配尚未提交，此时冻结换活字块选机命中机台的前序 SKU 收尾明细。
         * 通用胎胚时间直接读取既有结构 Map；新增排产专用五个字段继续保持为空。
         */
        this.fillTypeBlockRealtimeCommonFields(
                context, sku, machine, result, pairResult);
        context.getScheduleResultList().add(result);
        context.getScheduleResultSourceSkuMap().put(result, sku);
        if (getMaintenanceScheduleService().shouldMarkPrecisionPreInsert(
                machine, switchStartTime)) {
            getMaintenanceScheduleService().markPrecisionPreInsertScheduled(
                    context, machine, result);
            Date precisionSwitchCompleteTime = resolveTypeBlockSwitchCompleteTime(
                    context, machine, switchStartTime, startTime);
            LhShiftConfigVO precisionInspectionShift =
                    this.resolveTypeBlockFirstInspectionAttributionShift(
                            context, sku, shifts, precisionSwitchCompleteTime);
            if (Objects.nonNull(precisionInspectionShift)) {
                // 换活字块只消费班次首检顺序，不消费新增规格早/中班首检均衡额度。
                context.getPrecisionPreInsertInspectionShiftIndexMap().put(
                        result, precisionInspectionShift.getShiftIndex());
            }
            if (Objects.nonNull(pairResult)) {
                context.getPrecisionPreInsertResultSet().add(pairResult);
            }
        }
        Date switchCompleteTime = resolveTypeBlockSwitchCompleteTime(
                context, machine, switchStartTime, startTime);
        if (ShiftCapacityResolverUtil.isPlannedRepairAffectingSwitch(
                context, context.getDevicePlanShutList(), machine.getMachineCode(),
                machine.getEstimatedEndTime(), switchStartTime, switchCompleteTime)) {
            // 换活字块结果已通过数量和账本校验后再写过程日志，确保记录对应最终落地时间轴。
            Date repairProductionReadyTime = ShiftCapacityResolverUtil.resolvePlannedRepairProductionReadyTime(
                    context, context.getDevicePlanShutList(), machine.getMachineCode(),
                    machine.getEstimatedEndTime(), switchStartTime, switchCompleteTime);
            StringBuilder repairTimelineDetail = new StringBuilder(256);
            PriorityTraceLogHelper.appendLine(repairTimelineDetail,
                    "机台=" + machine.getMachineCode() + ", SKU=" + sku.getMaterialCode()
                            + ", 切换类型=换活字块");
            PriorityTraceLogHelper.appendLine(repairTimelineDetail,
                    "切换开始=" + LhScheduleTimeUtil.formatDateTime(switchStartTime)
                            + ", 切换结束=" + LhScheduleTimeUtil.formatDateTime(switchCompleteTime));
            PriorityTraceLogHelper.appendLine(repairTimelineDetail,
                    "预热分钟数=" + LhScheduleTimeUtil.getCapsulePreheatMinutes(context)
                            + ", 最早开产=" + LhScheduleTimeUtil.formatDateTime(repairProductionReadyTime)
                            + ", 实际首个生产=" + LhScheduleTimeUtil.formatDateTime(startTime)
                            + ", 首检额外等待小时=0");
            PriorityTraceLogHelper.appendProcessLog(
                    context, "计划性维修与换活字块重叠时间轴", repairTimelineDetail.toString().trim());
        }
        registerMachineAssignment(context, machine.getMachineCode(), result);
        this.recordTypeBlockScheduledMachineForResult(context, result, shifts);
        updateMachineState(context, machine, sku, result);
        if (wholeSingleControlUnit) {
            context.getScheduleResultList().add(pairResult);
            context.getScheduleResultSourceSkuMap().put(pairResult, sku);
            registerMachineAssignment(context, pairMachine.getMachineCode(), pairResult);
            this.recordTypeBlockScheduledMachineForResult(context, pairResult, shifts);
            updateMachineState(context, pairMachine, sku, pairResult);
            log.info("双模换活字块L/R同步落地, materialCode: {}, primaryMachine: {}, pairMachine: {}, "
                            + "switchStartTime: {}, productionStartTime: {}, totalScheduledQty: {}",
                    sku.getMaterialCode(), machine.getMachineCode(), pairMachine.getMachineCode(),
                    LhScheduleTimeUtil.formatDateTime(switchStartTime),
                    LhScheduleTimeUtil.formatDateTime(startTime), quotaTrimmedQty);
        }
        if (Objects.nonNull(committedFirstInspectionAllocationPlan)) {
            /*
             * 到此主副结果、日计划账本和机台运行态均已成功提交，才允许记录最终首检分摊。
             * 失败候选会在前面的回滚分支直接返回，因此不会留下与最终结果不一致的过程日志。
             */
            FirstInspectionQtyUtil.appendCommittedFirstInspectionAllocationProcessLog(
                    context, result, committedFirstInspectionAllocationPlan,
                    ScheduleTypeEnum.TYPE_BLOCK.getCode());
        }
        int scheduledQty = quotaTrimmedQty;
        // 换活字块结果可能被“物料+产品状态”实际消费账本裁剪，残量必须同时受目标缺口和账本剩余约束。
        int remainingQty = resolveTypeBlockResidualQty(
                context, sku, adoptedTargetQty, scheduledQty);
        this.completeSuccessfulTypeBlockSchedule(
                context, machine, sku, adoptedTargetQty, scheduledQty,
                remainingQty, originalStrictTargetQty);
        return true;
    }

    /**
     * 回写换活字块结果共用的最早胎胚时间和实时机台收尾明细。
     *
     * <p>调用点位于结果加入列表及机台状态推进之前，保证收尾明细只包含前序结果；
     * 新增排产专用的班次负荷、切换次数、结构机台数、候选描述和实时顺序不在 S4.4 回写。</p>
     *
     * @param context 排程上下文
     * @param sku 当前换活字块 SKU
     * @param machine 实际命中机台
     * @param result 主结果
     * @param pairResult 单控整机配对侧结果
     */
    private void fillTypeBlockRealtimeCommonFields(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            MachineScheduleDTO machine,
            LhScheduleResult result,
            LhScheduleResult pairResult) {
        if (Objects.isNull(context) || Objects.isNull(sku) || Objects.isNull(machine)) {
            return;
        }
        Date earliestEmbryoAvailableTime =
                NewSpecEmbryoAvailableTimeResolver.resolveEarliestAvailableTime(context, sku);
        String realtimeMachineEndingInfo = machineMatchStrategy.resolveRealtimeMachineEndingText(
                context, sku, machine.getMachineCode());
        this.fillSingleTypeBlockRealtimeCommonFields(
                result, earliestEmbryoAvailableTime, realtimeMachineEndingInfo);
        this.fillSingleTypeBlockRealtimeCommonFields(
                pairResult, earliestEmbryoAvailableTime, realtimeMachineEndingInfo);
    }

    /**
     * 回写单条换活字块结果的通用实时排查字段。
     *
     * @param result 待回写结果；允许为空
     * @param earliestEmbryoAvailableTime 最早胎胚可供硫化时间
     * @param realtimeMachineEndingInfo 命中机台前序 SKU 收尾明细
     */
    private void fillSingleTypeBlockRealtimeCommonFields(
            LhScheduleResult result,
            Date earliestEmbryoAvailableTime,
            String realtimeMachineEndingInfo) {
        if (Objects.isNull(result)) {
            return;
        }
        result.setEarliestEmbryoAvailableTime(
                Objects.isNull(earliestEmbryoAvailableTime)
                        ? null : new Date(earliestEmbryoAvailableTime.getTime()));
        result.setRealtimeMachineEndingInfo(realtimeMachineEndingInfo);
    }

    /**
     * 收口已经成功形成结果的换活字块 SKU。
     *
     * <p>成功完整量和成功部分量都必须退出通用新增待排队列；只有完全没有形成有效结果的失败状态
     * 才继续保留在新增队列。成功部分量通过独立过程日志保留审计证据，下一滚动窗口再按月计划与
     * 已排结果重算，不得进入当前批次新增 SKU 排序。</p>
     *
     * @param context 排程上下文
     * @param machine 已成功承接的机台
     * @param sku 已成功换活字块的 SKU
     * @param adoptedTargetQty 本次采用目标量
     * @param scheduledQty 本次实际排产量
     * @param remainingQty 本次未覆盖残量
     * @param originalStrictTargetQty 原严格目标量标识
     */
    private void completeSuccessfulTypeBlockSchedule(
            LhScheduleContext context,
            MachineScheduleDTO machine,
            SkuScheduleDTO sku,
            int adoptedTargetQty,
            int scheduledQty,
            int remainingQty,
            boolean originalStrictTargetQty) {
        /*
         * 换活字块一旦成功形成有效结果，无论是否覆盖完整目标量，都已完成本窗口的上机方式决策。
         * 必须先移出通用新增待排队列，禁止同一 SKU 再进入 S4.5 排序、换模和新增排序日志。
         * 未覆盖量保留在月计划/实际消费账本中，由下一滚动窗口重新加载，不在当前窗口伪装成新增 SKU。
         */
        context.getNewSpecSkuList().remove(sku);
        context.removePendingSkuFromStructureMap(sku);
        if (remainingQty > 0) {
            // 成功部分量保留明确残量状态，仅用于当前批次审计，不再参与通用新增排产。
            sku.setTargetScheduleQty(remainingQty);
            sku.setRemainingScheduleQty(remainingQty);
            sku.setStrictTargetQty(originalStrictTargetQty);
            this.appendTypeBlockPartialResidualProcessLog(
                    context, machine, sku, adoptedTargetQty, scheduledQty, remainingQty);
            log.info("换活字块成功部分量，残量退出当前窗口通用新增排产, machineCode: {}, materialCode: {}, "
                            + "adoptedTargetQty: {}, scheduledQty: {}, residualQty: {}, outcome: SUCCESS_PARTIAL",
                    machine.getMachineCode(), sku.getMaterialCode(), adoptedTargetQty,
                    scheduledQty, remainingQty);
            return;
        }
        log.debug("换活字块成功完整量, 机台: {}, SKU: {}, 目标量: {}, 已排: {}, 剩余: {}, outcome: SUCCESS_COMPLETE",
                machine.getMachineCode(), sku.getMaterialCode(), adoptedTargetQty,
                scheduledQty, remainingQty);
    }

    /**
     * 记录换活字块成功部分量的独立残量审计。
     *
     * <p>该记录只说明本次换活字块已成功但未覆盖完整目标量，不属于新增 SKU 排序日志，
     * 也不创建通用新增未排记录。下一滚动窗口会根据月计划与已排结果重新计算真实余量。</p>
     *
     * @param context 排程上下文
     * @param machine 已成功承接的机台
     * @param sku 已成功换活字块的 SKU
     * @param adoptedTargetQty 本次采用目标量
     * @param scheduledQty 本次实际排产量
     * @param residualQty 本次未覆盖残量
     */
    private void appendTypeBlockPartialResidualProcessLog(
            LhScheduleContext context,
            MachineScheduleDTO machine,
            SkuScheduleDTO sku,
            int adoptedTargetQty,
            int scheduledQty,
            int residualQty) {
        if (Objects.isNull(context) || Objects.isNull(machine) || Objects.isNull(sku)) {
            return;
        }
        String detail = new StringBuilder(256)
                .append("scheduleDate=")
                .append(LhScheduleTimeUtil.formatDate(context.getScheduleTargetDate()))
                .append(", materialCode=").append(sku.getMaterialCode())
                .append(", productStatus=").append(sku.getProductStatus())
                .append(", machineCode=").append(machine.getMachineCode())
                .append(", adoptedTargetQty=").append(adoptedTargetQty)
                .append(", scheduledQty=").append(scheduledQty)
                .append(", residualQty=").append(residualQty)
                .append(", outcome=SUCCESS_PARTIAL")
                .append(", nextAction=下一滚动窗口按月计划与已排结果重算")
                .toString();
        PriorityTraceLogHelper.appendProcessLog(
                context, "换活字块成功部分量残量", detail);
    }

    /**
     * 按结果实际有量业务日登记结构和 SKU 已排机台。
     *
     * <p>S4.4 可能连续处理多个提前生产 SKU，首个换活字块成功后必须立即更新物理机台
     * 统计，后续候选才能正确执行“包含本次后不得超过计划硫化机台数”的硬约束。</p>
     *
     * @param context 排程上下文
     * @param result 已落地换活字块结果
     * @param shifts 排程窗口班次
     */
    private void recordTypeBlockScheduledMachineForResult(
            LhScheduleContext context,
            LhScheduleResult result,
            List<LhShiftConfigVO> shifts) {
        if (Objects.isNull(context) || Objects.isNull(result)
                || CollectionUtils.isEmpty(shifts)
                || StringUtils.isEmpty(result.getLhMachineCode())) {
            return;
        }
        Set<LocalDate> recordedDateSet = new LinkedHashSet<LocalDate>(3);
        for (LhShiftConfigVO shift : shifts) {
            if (Objects.isNull(shift) || Objects.isNull(shift.getShiftIndex())
                    || Objects.isNull(shift.getWorkDate())) {
                continue;
            }
            Integer planQty = ShiftFieldUtil.getShiftPlanQty(
                    result, shift.getShiftIndex());
            if (Objects.nonNull(planQty) && planQty > 0) {
                recordedDateSet.add(shift.getWorkDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate());
            }
        }
        for (LocalDate businessDate : recordedDateSet) {
            context.recordScheduledMachine(
                    businessDate, result.getStructureName(), result.getMaterialCode(),
                    result.getProductStatus(), result.getLhMachineCode());
        }
    }

    /**
     * 计算换活字块成功后未覆盖的独立残量。
     *
     * @param context 排程上下文
     * @param sku 当前业务SKU
     * @param adoptedTargetQty 换活字块采用的目标量
     * @param scheduledQty 本次实际排产量
     * @return 同时受目标缺口和本产品状态实际消费账本约束的残量
     */
    private int resolveTypeBlockResidualQty(LhScheduleContext context,
                                            SkuScheduleDTO sku,
                                            int adoptedTargetQty,
                                            int scheduledQty) {
        int targetRemainingQty = Math.max(0, adoptedTargetQty - scheduledQty);
        int ledgerRemainingQty = targetScheduleQtyResolver.resolveProductionRemainingQty(context, sku);
        return Math.min(targetRemainingQty, ledgerRemainingQty);
    }

    /**
     * 处理换活字块候选SKU的前置未排规则。
     *
     * <p>规则顺序统一为“收尾小余量优先、仅历史欠产其次”。命中后立即写入未排并移出待排队列，
     * 不再生成换活字块结果和换模计划。</p>
     *
     * @param context 排程上下文
     * @param machine 当前换活字块机台
     * @param sku 当前候选SKU
     * @param smallEndingRuleEnding 是否按收尾小余量规则视为收尾
     * @param embryoStockEndingTargetApplied 是否已命中成型胎胚库存收尾目标
     * @param failureReason 换活字块失败原因
     * @return true-已写入未排并终止换活字块；false-继续原排程逻辑
     */
    private boolean handlePendingSkuUnscheduledRuleIfNecessary(LhScheduleContext context,
                                                               MachineScheduleDTO machine,
                                                               SkuScheduleDTO sku,
                                                               boolean smallEndingRuleEnding,
                                                               boolean embryoStockEndingTargetApplied,
                                                               StringBuilder failureReason) {
        LhUnscheduledResult unscheduledResult = PendingSkuUnscheduledRule.evaluate(
                context, sku, smallEndingRuleEnding, embryoStockEndingTargetApplied);
        if (Objects.isNull(unscheduledResult)) {
            return false;
        }
        context.getUnscheduledResultList().add(unscheduledResult);
        context.getNewSpecSkuList().remove(sku);
        context.removePendingSkuFromStructureMap(sku);
        String unscheduledReason = unscheduledResult.getUnscheduledReason();
        recordTypeBlockAppendFailure(failureReason, unscheduledReason);
        getTargetScheduleQtyResolver().removeActiveEmbryoSku(context, sku, unscheduledReason);
        log.info("换活字块候选SKU命中前置未排规则，跳过排产并移出待排队列, machineCode: {}, "
                        + "materialCode: {}, unscheduledQty: {}, reason: {}",
                Objects.nonNull(machine) ? machine.getMachineCode() : null,
                sku.getMaterialCode(), unscheduledResult.getUnscheduledQty(), unscheduledReason);
        return true;
    }

    /**
     * 判断是否为共用胎胚零余量SKU。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @return true-共用胎胚且硫化余量小于等于0；false-不命中
     */
    private boolean isSharedEmbryoZeroSurplusSku(LhScheduleContext context, SkuScheduleDTO sku) {
        if (context == null || sku == null || sku.getSurplusQty() > 0
                || StringUtils.isEmpty(sku.getMaterialCode())) {
            return false;
        }
        if (getTargetScheduleQtyResolver().isEmbryoStockEnding(context, sku)) {
            return false;
        }
        return Boolean.TRUE.equals(context.getMaterialSharedEmbryoMap().get(sku.getMaterialCode()));
    }

    /**
     * 写入换活字块共用胎胚零余量未排结果。
     *
     * @param context 排程上下文
     * @param sku SKU
     */
    private void addSharedEmbryoZeroSurplusUnscheduledResult(LhScheduleContext context, SkuScheduleDTO sku) {
        LhUnscheduledResult unscheduled = new LhUnscheduledResult();
        unscheduled.setFactoryCode(context.getFactoryCode());
        unscheduled.setBatchNo(context.getBatchNo());
        unscheduled.setMaterialCode(sku.getMaterialCode());
        unscheduled.setProductStatus(sku.getProductStatus());
        unscheduled.setMaterialDesc(sku.getMaterialDesc());
        unscheduled.setScheduleDate(context.getScheduleTargetDate());
        unscheduled.setUnscheduledReason(SHARED_EMBRYO_ZERO_SURPLUS_UNSCHEDULED_REASON);
        unscheduled.setUnscheduledQty(0);
        unscheduled.setStructureName(sku.getStructureName());
        unscheduled.setMainMaterialDesc(sku.getMainMaterialDesc());
        unscheduled.setSpecCode(sku.getSpecCode());
        unscheduled.setEmbryoCode(sku.getEmbryoCode());
        unscheduled.setMouldQty(sku.getMouldQty());
        unscheduled.setDataSource(AUTO_DATA_SOURCE);
        unscheduled.setIsDelete(0);
        context.getUnscheduledResultList().add(unscheduled);
    }

    /**
     * 回滚换活字块首检数量顺序计数。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param sku SKU
     * @param switchStartTime 换活字块开始时间
     * @param fallbackStartTime 开产时间兜底值
     * @param shifts 班次
     * @param earliestEmbryoAvailableTime 结构切换提前命中的最早胎胚可供时间；其他换活字块为 null
     */
    private void rollbackTypeBlockFirstInspectionSequence(LhScheduleContext context,
                                                          MachineScheduleDTO machine,
                                                          SkuScheduleDTO sku,
                                                          Date switchStartTime,
                                                          Date fallbackStartTime,
                                                          List<LhShiftConfigVO> shifts,
                                                          Date earliestEmbryoAvailableTime) {
        Date switchCompleteTime = resolveTypeBlockSwitchCompleteTime(
                context, machine, switchStartTime, fallbackStartTime);
        FirstInspectionQtyUtil.rollbackFirstInspectionSequence(
                context, this.resolveTypeBlockFirstInspectionAttributionShift(
                        context, sku, shifts, switchCompleteTime));
    }

    /**
     * 解析换活字块结果与失败回滚共用的首检归属班次。
     *
     * <p>首检属于换活字块准备阶段，数量计数班次只由真实切换完成时间决定；胎胚最早
     * 可供时间仅约束正式生产，不能移动首检计数。该方法同时供精度前插登记和失败回滚
     * 调用，保证两处与正式结果使用同一半开区间口径。</p>
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param shifts 排程班次
     * @param firstInspectionBaseTime 真实换活字块完成时间
     * @return 首检归属班次；排程窗口内无可归属班次时返回 null
     */
    private LhShiftConfigVO resolveTypeBlockFirstInspectionAttributionShift(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            List<LhShiftConfigVO> shifts,
            Date firstInspectionBaseTime) {
        return FirstInspectionQtyUtil.resolveFirstInspectionAttributionShift(
                context, sku, shifts, firstInspectionBaseTime,
                ScheduleTypeEnum.TYPE_BLOCK.getCode());
    }

    /**
     * 判断当前SKU是否已经落过换活字块结果。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @return true-已经落过换活字块结果
     */
    private boolean hasScheduledTypeBlockResult(LhScheduleContext context, SkuScheduleDTO sku) {
        if (context == null || sku == null || StringUtils.isEmpty(sku.getMaterialCode())
                || CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return false;
        }
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (result == null) {
                continue;
            }
            if (StringUtils.equals(sku.getMaterialCode(), result.getMaterialCode())
                    && StringUtils.equals(StringUtils.trimToEmpty(sku.getProductStatus()),
                    StringUtils.trimToEmpty(result.getProductStatus()))
                    && StringUtils.equals(ScheduleTypeEnum.TYPE_BLOCK.getCode(), result.getScheduleType())
                    && StringUtils.equals(YES_FLAG, result.getIsTypeBlock())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 追加换活字块结果，并在失败时回滚已占用的模具切换配额。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param sku SKU
     * @param startTime 开产时间
     * @param switchStartTime 切换开始时间
     * @param shifts 班次
     * @return true-成功
     */
    private boolean appendTypeBlockResultWithRollback(LhScheduleContext context,
                                                      MachineScheduleDTO machine,
                                                      SkuScheduleDTO sku,
                                                      Date startTime,
                                                      Date switchStartTime,
                                                      List<LhShiftConfigVO> shifts,
                                                      boolean isSingleMachine,
                                                      StringBuilder failureReason) {
        /*
         * 先按原计算开产时间识别普通换活字块与提前生产候选，保持原 dayN 判定口径不变。
         * 普通结构提前和结构收尾提前继续在结果追加前按原时序激活运行视图；只有结构切换
         * 提前需要等待配对侧释放后再计算胎胚时间下限，因此将其候选日交给结果追加主链。
         */
        LocalDate productionWorkDate = resolveTypeBlockProductionWorkDate(shifts, startTime);
        if (Objects.isNull(productionWorkDate)) {
            this.recordTypeBlockAppendFailure(
                    failureReason, "换活字块实际开产业务日无法从班次窗口解析");
            if (Objects.nonNull(switchStartTime)) {
                getMouldChangeBalanceStrategy().rollbackMouldChange(context, switchStartTime);
            }
            return false;
        }
        int originalDayPlanQty = resolveTypeBlockOriginalDayPlanQty(
                context, sku, productionWorkDate);
        LocalDate structureSwitchEarlyProductionCandidateDate = null;
        boolean success = true;
        if (originalDayPlanQty <= 0) {
            LocalDate firstFuturePlanDate = EarlyProductionChecker.resolveFirstFuturePlanDate(
                    context, sku, productionWorkDate);
            if (this.shouldDeferEarlyProductionToDailyNewSpec(
                    context, firstFuturePlanDate)) {
                String deferredReason = "提前生产必须等待当前业务日正常新增任务完成后执行";
                this.recordTypeBlockAppendFailure(failureReason, deferredReason);
                log.info("换活字块提前生产后置到按日新增主链, factoryCode: {}, batchNo: {}, "
                                + "materialCode: {}, productionWorkDate: {}, futurePlanDate: {}, "
                                + "windowEndDate: {}, reason: {}",
                        context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(),
                        productionWorkDate, firstFuturePlanDate,
                        this.resolveScheduleTargetLocalDate(context), deferredReason);
                success = false;
            } else {
                boolean structureSwitchEarlyProduction =
                        EarlyProductionChecker.isStructureSwitchEarlyProduction(
                                context, sku, productionWorkDate, firstFuturePlanDate);
                if (structureSwitchEarlyProduction) {
                    structureSwitchEarlyProductionCandidateDate = productionWorkDate;
                } else {
                    // 非结构切换提前严格复用本次需求调整前的运行视图准备时点和失败回滚路径。
                    String earlyProductionRejectReason = this.prepareTypeBlockEarlyProduction(
                            context, machine, sku, productionWorkDate,
                            switchStartTime, startTime);
                    success = StringUtils.isEmpty(earlyProductionRejectReason);
                    if (!success) {
                        this.recordTypeBlockAppendFailure(
                                failureReason, earlyProductionRejectReason);
                    }
                }
            }
        }
        if (success) {
            // 普通 dayN 和三类提前生产继续共用既有结果构造、实际余量及日计划扣账主链。
            success = appendFollowUpResult(
                    context, machine, sku, startTime, switchStartTime, shifts,
                    structureSwitchEarlyProductionCandidateDate,
                    isSingleMachine, failureReason);
        }
        if (!success && switchStartTime != null) {
            // 换活字块结果落地失败时，回滚本轮已占用的切换配额。
            getMouldChangeBalanceStrategy().rollbackMouldChange(context, switchStartTime);
        }
        return success;
    }

    /**
     * 判断换活字块提前生产是否应统一后置到 S4.5 提前生产阶段。
     *
     * <p>S4.4 位于 S4.5 按日正常新增之前，任何原始日计划为0的未来 SKU 若在这里直接
     * 落地，都会先于对应业务日正常任务占用机台，并可能在跨日续排后突破结构计划机台数。
     * 因此只要存在未来来源计划日，就保留 SKU 在新增队列中；S4.5 每日正常任务完成后，
     * 再复用同一换活字块关系、选机排序和提前生产中心运行视图使用真实剩余资源。</p>
     *
     * @param context 排程上下文
     * @param futurePlanDate 提前生产最近来源日
     * @return true-后置到正常任务之后；false-保持现有 S4.4 处理
     */
    private boolean shouldDeferEarlyProductionToDailyNewSpec(
            LhScheduleContext context,
            LocalDate futurePlanDate) {
        return Objects.nonNull(context) && Objects.nonNull(futurePlanDate);
    }

    /**
     * 计算换活字块结构切换提前受胎胚时间和既有班次管控约束后的实际开产时间。
     *
     * <p>先复用现有时间解析器计算
     * {@code max(原规则理论开产时间, 当前生效的最早胎胚可供硫化时间)}，再复用班次管控工具顺延到
     * 首个真正具有硫化产能的时刻。换活字块开始和完成时间只作为日志与准备动作保留，
     * 不因胎胚尚未可供而后移。</p>
     *
     * @param context 排程上下文
     * @param machine 当前候选机台
     * @param sku 当前 SKU
     * @param theoreticalProductionStartTime 原换活字块、维修、清洗和试制规则计算的开产时间
     * @param switchStartTime 换活字块开始时间
     * @param earliestEmbryoAvailableTime 最早胎胚可供硫化时间
     * @param shifts 排程班次
     * @return 实际允许开产时间；排程窗口内无可排时间时返回 null
     */
    private Date resolveTypeBlockEarlyProductionStartTime(
            LhScheduleContext context,
            MachineScheduleDTO machine,
            SkuScheduleDTO sku,
            Date theoreticalProductionStartTime,
            Date switchStartTime,
            Date earliestEmbryoAvailableTime,
            List<LhShiftConfigVO> shifts) {
        Date requestedProductionStartTime =
                NewSpecEmbryoAvailableTimeResolver.resolveActualProductionStartTime(
                        theoreticalProductionStartTime, earliestEmbryoAvailableTime);
        int machineMouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(machine);
        int runtimeShiftCapacity = ShiftCapacityResolverUtil.resolveRuntimeShiftCapacity(
                context, machine, sku.getShiftCapacity());
        Date actualProductionStartTime = null;
        Date candidateStartTime = requestedProductionStartTime;
        while (Objects.nonNull(candidateStartTime)) {
            candidateStartTime =
                    ShiftProductionControlUtil.resolveFirstSchedulableStartIgnoringCleaning(
                            context, machine.getMachineCode(), candidateStartTime,
                            shifts, runtimeShiftCapacity, sku.getLhTimeSeconds(), machineMouldQty);
            LhShiftConfigVO attributionShift =
                    NewSpecEmbryoAvailableTimeResolver.resolveProductionShift(
                            shifts, candidateStartTime);
            if (Objects.isNull(attributionShift)) {
                break;
            }
            /*
             * 复用换活字块最终落班使用的停机、清洗、保养、日标准产量和部分班产计算，
             * 再复用现有胎胚首检工具校验当前部分班次能否完整承载首检。容量不足时
             * 首检与正式生产整体移到下一班，换活字块时间仍保持不动。
             */
            LhScheduleResult capacityPreviewResult = new LhScheduleResult();
            capacityPreviewResult.setMaterialCode(sku.getMaterialCode());
            capacityPreviewResult.setStructureName(sku.getStructureName());
            capacityPreviewResult.setLhMachineCode(machine.getMachineCode());
            List<MachineCleaningWindowDTO> cleaningWindowList =
                    new ArrayList<>(MachineCleaningOverlapUtil.excludeOverlapWindows(
                            machine.getCleaningWindowList(), switchStartTime, candidateStartTime));
            List<MachineMaintenanceWindowDTO> maintenanceWindowList =
                    this.resolveMachineMaintenanceWindowList(context, machine.getMachineCode());
            Map<Integer, Integer> shiftCapacityMap =
                    this.calculateDailyStandardShiftCapacityMap(
                            context, capacityPreviewResult, shifts, candidateStartTime,
                            runtimeShiftCapacity, sku.getLhTimeSeconds(), machineMouldQty,
                            cleaningWindowList, maintenanceWindowList);
            Map<Integer, Integer> firstInspectionCapacityMap =
                    FirstInspectionQtyUtil.applyEmbryoAvailableFirstInspectionCapacity(
                            context, sku, shifts, attributionShift, shiftCapacityMap,
                            runtimeShiftCapacity, sku.resolveTargetScheduleQty(),
                            ScheduleTypeEnum.TYPE_BLOCK.getCode(), machine.getMachineCode());
            int availableShiftCapacity = Math.max(0, firstInspectionCapacityMap.getOrDefault(
                    attributionShift.getShiftIndex(), 0));
            if (availableShiftCapacity > 0) {
                actualProductionStartTime = candidateStartTime;
                break;
            }
            log.info("换活字块提前生产胎胚可供班次不足以完整承载首检，首检和生产整体顺延, "
                            + "factoryCode: {}, batchNo: {}, materialCode: {}, machineCode: {}, "
                            + "classNo: class{}, candidateStartTime: {}, nextShiftStartTime: {}",
                    context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(),
                    machine.getMachineCode(), attributionShift.getShiftIndex(),
                    LhScheduleTimeUtil.formatDateTime(candidateStartTime),
                    LhScheduleTimeUtil.formatDateTime(attributionShift.getShiftEndDateTime()));
            candidateStartTime = attributionShift.getShiftEndDateTime();
        }
        log.info("换活字块提前生产胎胚时间下限计算, factoryCode: {}, batchNo: {}, "
                        + "materialCode: {}, structureName: {}, machineCode: {}, switchStartTime: {}, "
                        + "theoreticalProductionStartTime: {}, earliestEmbryoAvailableTime: {}, "
                        + "actualProductionStartTime: {}",
                context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(),
                sku.getStructureName(), machine.getMachineCode(),
                LhScheduleTimeUtil.formatDateTime(switchStartTime),
                LhScheduleTimeUtil.formatDateTime(theoreticalProductionStartTime),
                LhScheduleTimeUtil.formatDateTime(earliestEmbryoAvailableTime),
                LhScheduleTimeUtil.formatDateTime(actualProductionStartTime));
        return actualProductionStartTime;
    }

    /**
     * 只读核验换活字块理论开产业务日的提前生产准入及候选机台硬控。
     *
     * <p>该入口专用于胎胚时间把实际生产推到另一业务日的场景。理论业务日仍必须满足
     * 结构时间、欠产、计划机台和结构收尾等全部共享条件，但只读核验不得初始化目标量、
     * 前移 dayN 或注册实际消费账本；实际开产业务日仍无计划时，再由
     * {@link #prepareTypeBlockEarlyProduction(LhScheduleContext, MachineScheduleDTO,
     * SkuScheduleDTO, LocalDate, Date, Date)} 激活原运行视图。</p>
     *
     * @param context 排程上下文
     * @param machine 当前候选机台
     * @param sku 目标 SKU
     * @param productionWorkDate 原规则理论开产业务日
     * @param switchStartTime 换活字块开始时刻
     * @param productionStartTime 受胎胚约束后的实际开产时刻
     * @return 空串表示允许；非空为明确拒绝原因
     */
    private String evaluateTypeBlockEarlyProductionAdmission(
            LhScheduleContext context,
            MachineScheduleDTO machine,
            SkuScheduleDTO sku,
            LocalDate productionWorkDate,
            Date switchStartTime,
            Date productionStartTime) {
        if (Objects.isNull(productionWorkDate)) {
            return "换活字块理论开产业务日无法从班次窗口解析";
        }
        if (!this.isTimeInScheduleWindow(context, switchStartTime)
                || !this.isTimeInScheduleWindow(context, productionStartTime)) {
            return "提前生产换活字块或开产时刻超出排程窗口";
        }
        if (Objects.isNull(earlyProductionRuntimePlanService)) {
            return "提前生产运行态计划服务未初始化";
        }
        // 调用共享服务的只读入口，禁止在理论日校验阶段复制 Checker 或创建临时账本。
        EarlyProductionDecision decision =
                earlyProductionRuntimePlanService.evaluateEarlyProductionAdmission(
                        context, sku, productionWorkDate);
        return this.resolveTypeBlockEarlyProductionAdmissionRejectReason(
                context, machine, sku, productionWorkDate, decision, true);
    }

    /**
     * 为零原始日计划的换活字块候选准备提前生产运行视图并执行候选机台硬控。
     *
     * <p>换活字块和开产时刻必须同时位于本批窗口；结构机台数达到月计划上限后，
     * 只允许复用已经计入目标结构的物理机台。准入失败只回滚当前换活字块时间配额，
     * 不删除已激活运行视图，使后续合法机台或 S4.5 可以继续消费同一账本。</p>
     *
     * @param context 排程上下文
     * @param machine 当前候选机台
     * @param sku 目标 SKU
     * @param productionWorkDate 实际开产业务日
     * @param switchStartTime 换活字块开始时刻
     * @param productionStartTime 开产时刻
     * @return 空串表示允许；非空为明确拒绝原因
     */
    private String prepareTypeBlockEarlyProduction(
            LhScheduleContext context,
            MachineScheduleDTO machine,
            SkuScheduleDTO sku,
            LocalDate productionWorkDate,
            Date switchStartTime,
            Date productionStartTime) {
        if (Objects.isNull(productionWorkDate)) {
            return "换活字块实际开产业务日无法从班次窗口解析";
        }
        if (!this.isTimeInScheduleWindow(context, switchStartTime)
                || !this.isTimeInScheduleWindow(context, productionStartTime)) {
            return "提前生产换活字块或开产时刻超出排程窗口";
        }
        if (Objects.isNull(earlyProductionRuntimePlanService)) {
            return "提前生产运行态计划服务未初始化";
        }
        EarlyProductionRuntimePlan runtimePlan =
                earlyProductionRuntimePlanService.prepareRuntimePlan(
                        context, sku, productionWorkDate);
        EarlyProductionDecision decision = Objects.isNull(runtimePlan)
                ? null : runtimePlan.getDecision();
        boolean runtimePlanReady = Objects.nonNull(runtimePlan)
                && runtimePlan.isActive()
                && Objects.nonNull(runtimePlan.getFuturePlanDate());
        String admissionRejectReason =
                this.resolveTypeBlockEarlyProductionAdmissionRejectReason(
                        context, machine, sku, productionWorkDate,
                        decision, runtimePlanReady);
        if (StringUtils.isNotEmpty(admissionRejectReason)) {
            return admissionRejectReason;
        }
        context.getNewSpecEarlyProductionAllowedMap().put(sku, Boolean.TRUE);
        log.info("换活字块提前生产准入通过, factoryCode: {}, batchNo: {}, materialCode: {}, "
                        + "machineCode: {}, productionWorkDate: {}, futurePlanDate: {}, "
                        + "switchStartTime: {}, productionStartTime: {}, structureName: {}",
                context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(),
                machine.getMachineCode(), productionWorkDate, decision.getFuturePlanDate(),
                LhScheduleTimeUtil.formatDateTime(switchStartTime),
                LhScheduleTimeUtil.formatDateTime(productionStartTime), sku.getStructureName());
        return StringUtils.EMPTY;
    }

    /**
     * 统一收口换活字块提前生产的共享准入结论与候选物理机台限制。
     *
     * <p>只读理论日核验和运行视图激活必须使用同一拒绝原因、同一结构物理机台口径，
     * 避免胎胚时间跨日后出现一条路径允许、另一条路径拒绝。</p>
     *
     * @param context 排程上下文
     * @param machine 当前候选机台
     * @param sku 目标 SKU
     * @param productionWorkDate 待核验业务日
     * @param decision 共享提前生产准入结论
     * @param runtimePlanReady 是否已形成可消费的运行视图；只读核验固定传 true
     * @return 空串表示允许；非空为明确拒绝原因
     */
    private String resolveTypeBlockEarlyProductionAdmissionRejectReason(
            LhScheduleContext context,
            MachineScheduleDTO machine,
            SkuScheduleDTO sku,
            LocalDate productionWorkDate,
            EarlyProductionDecision decision,
            boolean runtimePlanReady) {
        if (!runtimePlanReady || Objects.isNull(decision)
                || !decision.isEarlyProduction() || !decision.isAllowed()
                || Objects.isNull(decision.getFuturePlanDate())) {
            String decisionReason = Objects.isNull(decision)
                    ? "未命中窗口内未来日计划"
                    : decision.getReason();
            log.info("换活字块提前生产准入未通过, factoryCode: {}, batchNo: {}, "
                            + "materialCode: {}, machineCode: {}, productionWorkDate: {}, reason: {}",
                    context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(),
                    machine.getMachineCode(), productionWorkDate, decisionReason);
            return new StringBuilder("换活字块理论开产业务日原始日计划量为0，提前生产准入未通过：")
                    .append(StringUtils.defaultString(decisionReason, "无明确原因"))
                    .toString();
        }
        boolean machineAllowed = EarlyProductionChecker.canUseMachineForEarlyProduction(
                context, sku, productionWorkDate, decision.getFuturePlanDate(),
                machine.getMachineCode());
        if (!machineAllowed) {
            int scheduledMachineCount = context.getStructureScheduledMachineCount(
                    productionWorkDate, sku.getStructureName());
            int planMachineCount = EarlyProductionChecker.resolveEffectiveStructurePlanMachineCount(
                    context, sku, productionWorkDate, decision.getFuturePlanDate());
            boolean exceededPlanMachineCount =
                    planMachineCount > 0 && scheduledMachineCount > planMachineCount;
            String reasonPrefix = exceededPlanMachineCount
                    ? "同结构当前已排物理机台数超过计划硫化机台数，禁止提前生产，结构="
                    : "同结构计划硫化机台数已达上限，禁止提前生产新增物理机台，结构=";
            String reason = new StringBuilder(reasonPrefix)
                    .append(PriorityTraceLogHelper.safeText(sku.getStructureName()))
                    .append("，当前已排物理机台数=").append(scheduledMachineCount)
                    .append("，计划机台数=").append(planMachineCount)
                    .append("，候选机台=")
                    .append(PriorityTraceLogHelper.safeText(machine.getMachineCode()))
                    .toString();
            log.info("换活字块提前生产结构机台数限制, factoryCode: {}, batchNo: {}, "
                            + "materialCode: {}, productionWorkDate: {}, futurePlanDate: {}, reason: {}",
                    context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(),
                    productionWorkDate, decision.getFuturePlanDate(), reason);
            return reason;
        }
        return StringUtils.EMPTY;
    }

    /**
     * 判断时刻是否位于本批排程窗口内。
     *
     * @param context 排程上下文
     * @param dateTime 待判断时刻
     * @return true-大于等于首班开始且早于末班结束
     */
    private boolean isTimeInScheduleWindow(LhScheduleContext context, Date dateTime) {
        if (Objects.isNull(context) || Objects.isNull(dateTime)
                || CollectionUtils.isEmpty(context.getScheduleWindowShifts())) {
            return false;
        }
        LhShiftConfigVO firstShift = context.getScheduleWindowShifts().get(0);
        LhShiftConfigVO lastShift = context.getScheduleWindowShifts()
                .get(context.getScheduleWindowShifts().size() - 1);
        if (Objects.isNull(firstShift) || Objects.isNull(lastShift)
                || Objects.isNull(firstShift.getShiftStartDateTime())
                || Objects.isNull(lastShift.getShiftEndDateTime())) {
            return false;
        }
        return !dateTime.before(firstShift.getShiftStartDateTime())
                && dateTime.before(lastShift.getShiftEndDateTime());
    }

    /**
     * 读取换活字块实际开产业务日的原始日计划量。
     *
     * <p>该方法只读取月计划原始 dayN，不读取 SKU 运行态剩余量或提前生产临时账本。
     * 调用处必须在写入换活字块结果及扣减资源前执行，避免换活字块主动拉取未来 SKU。</p>
     *
     * @param context 排程上下文
     * @param sku 换活字块目标 SKU
     * @param productionWorkDate 实际开产业务日期
     * @return 原始日计划量；上下文、SKU 或业务日期缺失时返回 0
     */
    private int resolveTypeBlockOriginalDayPlanQty(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate productionWorkDate) {
        if (Objects.isNull(context) || Objects.isNull(sku)
                || Objects.isNull(productionWorkDate)) {
            return 0;
        }
        return Math.max(0, MonthPlanDateResolver.resolveDayQty(
                context, sku.getMaterialCode(), sku.getProductStatus(),
                productionWorkDate));
    }

    /**
     * 根据实际开产时刻解析换活字块结果所属业务日期。
     *
     * @param shifts 排程窗口班次
     * @param startTime 实际开产时刻
     * @return 命中的班次业务日期；未命中返回 null
     */
    private LocalDate resolveTypeBlockProductionWorkDate(
            List<LhShiftConfigVO> shifts,
            Date startTime) {
        if (CollectionUtils.isEmpty(shifts) || Objects.isNull(startTime)) {
            return null;
        }
        for (LhShiftConfigVO shift : shifts) {
            if (Objects.isNull(shift)
                    || Objects.isNull(shift.getShiftStartDateTime())
                    || Objects.isNull(shift.getShiftEndDateTime())
                    || Objects.isNull(shift.getWorkDate())) {
                continue;
            }
            if (!startTime.before(shift.getShiftStartDateTime())
                    && startTime.before(shift.getShiftEndDateTime())) {
                return shift.getWorkDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();
            }
        }
        return null;
    }

    /**
     * 解析双模换活字块配对侧机台。
     *
     * <p>本方法只负责“配对侧是否存在且满足静态硬约束”；配对侧是否被其他SKU占用
     * 由 appendFollowUpResult 在计算等待释放后的切换时间后，按时间窗重叠口径统一判定，
     * 避免把配对侧本次窗口的在机/续作物料误判为占用。</p>
     *
     * @param context 排程上下文
     * @param machine 当前侧机台
     * @param sku 当前SKU
     * @param failureReason 失败原因载体
     * @return 配对侧机台；非双模或非单控机台返回null
     */
    private MachineScheduleDTO resolveWholeSingleControlTypeBlockPair(LhScheduleContext context,
                                                                       MachineScheduleDTO machine,
                                                                       SkuScheduleDTO sku,
                                                                       StringBuilder failureReason) {
        if (Objects.isNull(context) || Objects.isNull(machine) || Objects.isNull(sku)
                || !LhSingleControlMachineUtil.isWholeMachineGranularitySku(context, sku)
                || !LhSingleControlMachineUtil.isConfiguredSingleControlMachine(context, machine.getMachineCode())) {
            return null;
        }
        MachineScheduleDTO pairMachine = LhSingleControlMachineUtil.resolvePairMachine(
                context, machine.getMachineCode());
        if (Objects.isNull(pairMachine)) {
            recordTypeBlockAppendFailure(failureReason, "双模换活字块缺少L/R配对侧机台");
            return null;
        }
        if (Objects.isNull(machineMatchStrategy)
                || !machineMatchStrategy.isEligibleSingleControlSide(context, sku, pairMachine.getMachineCode())) {
            recordTypeBlockAppendFailure(failureReason, "双模换活字块配对侧未通过机台、模具、胶囊或窗口约束");
            return null;
        }
        return pairMachine;
    }

    /**
     * 判断双模换活字块配对侧在切换窗口内是否仍被其他SKU占用。
     *
     * <p>双模换活字块会等待 L/R 两侧都释放后再同步切换，因此判定必须以等待后的
     * 实际切换开始时间为基准：</p>
     * <ul>
     *   <li>与候选物料相同的结果不构成占用；</li>
     *   <li>其它物料结果在切换开始前已结束的不构成占用；</li>
     *   <li>缺少结束时间的配对侧在机结果，以机台释放时间（收尾时间）作为等待基准，
     *       释放时间不晚于切换开始时同样不构成占用；</li>
     *   <li>其余仍占用到切换窗口内的其他物料结果才真正阻断双模换活字块。</li>
     * </ul>
     *
     * @param context 排程上下文
     * @param pairMachine 配对侧机台
     * @param sku 当前换活字块候选SKU
     * @param switchStartTime 等待配对侧释放后的实际切换开始时间
     * @return true-存在冲突占用；false-可以同步切换
     */
    private boolean hasPairSideConflictingAssignment(LhScheduleContext context,
                                                     MachineScheduleDTO pairMachine,
                                                     SkuScheduleDTO sku,
                                                     Date switchStartTime) {
        if (Objects.isNull(context) || Objects.isNull(pairMachine) || Objects.isNull(sku)) {
            return false;
        }
        List<LhScheduleResult> pairAssignments =
                context.getMachineAssignmentMap().get(pairMachine.getMachineCode());
        if (CollectionUtils.isEmpty(pairAssignments)) {
            return false;
        }
        for (LhScheduleResult assignment : pairAssignments) {
            if (Objects.isNull(assignment) || Objects.isNull(assignment.getDailyPlanQty())
                    || assignment.getDailyPlanQty() <= 0
                    || StringUtils.equals(sku.getMaterialCode(), assignment.getMaterialCode())) {
                continue;
            }
            Date assignmentEndTime = assignment.getSpecEndTime();
            if (Objects.nonNull(assignmentEndTime) && !assignmentEndTime.after(switchStartTime)) {
                // 该结果在切换开始前已结束，不占用切换窗口，放行等待同步切换。
                continue;
            }
            if (Objects.isNull(assignmentEndTime)
                    && StringUtils.equals(pairMachine.getCurrentMaterialCode(), assignment.getMaterialCode())
                    && Objects.nonNull(pairMachine.getEstimatedEndTime())
                    && !pairMachine.getEstimatedEndTime().after(switchStartTime)) {
                // 配对侧在机结果缺少结束时间时，以机台释放时间作为等待基准，释放后同样放行。
                continue;
            }
            return true;
        }
        return false;
    }

    /**
     * 构建双模换活字块配对侧结果。
     *
     * @param context 排程上下文
     * @param primaryResult 主侧结果
     * @param pairMachine 配对侧机台
     * @param sku 当前SKU
     * @param mouldQty 单侧模数
     * @param shifts 排程班次
     * @return 与主侧班次、时间完全一致的配对侧结果
     */
    private LhScheduleResult buildWholeSingleControlTypeBlockPairResult(LhScheduleContext context,
                                                                        LhScheduleResult primaryResult,
                                                                        MachineScheduleDTO pairMachine,
                                                                        SkuScheduleDTO sku,
                                                                        int mouldQty,
                                                                        List<LhShiftConfigVO> shifts) {
        LhScheduleResult pairResult = new LhScheduleResult();
        BeanUtil.copyProperties(primaryResult, pairResult);
        pairResult.setOrderNo(generateOrderNo(context));
        pairResult.setLhMachineCode(pairMachine.getMachineCode());
        pairResult.setLhMachineName(pairMachine.getMachineName());
        pairResult.setLeftRightMould(LeftRightMouldUtil.resolveLeftRightMould(
                pairResult.getLeftRightMould(), pairMachine.getMachineCode()));
        pairResult.setMachineOrder(pairMachine.getMachineOrder());
        pairResult.setMouldCode(resolveTypeBlockActualMouldCode(context, pairMachine, sku));
        // 主侧已代表物理整机执行一次换胶囊，配对侧结果不得复制出第二条换胶囊备注。
        for (int shiftIndex = 1; shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
            ShiftFieldUtil.removeShiftAnalysis(
                    pairResult, shiftIndex, CapsuleReplacementRuleService.CAPSULE_REPLACEMENT_ANALYSIS);
            ShiftFieldUtil.removeShiftAnalysis(
                    pairResult, shiftIndex, FirstInspectionQtyUtil.FIRST_INSPECTION_ANALYSIS);
        }
        refreshResultSummary(context, pairResult, shifts);
        return pairResult;
    }

    /**
     * 获取两个日期中的较晚值。
     *
     * @param first 第一个日期
     * @param second 第二个日期
     * @return 较晚日期
     */
    private Date resolveLaterDate(Date first, Date second) {
        if (Objects.isNull(first)) {
            return second;
        }
        if (Objects.isNull(second)) {
            return first;
        }
        return first.after(second) ? first : second;
    }

    /**
     * 记录换活字块追加失败原因。
     *
     * @param failureReason 失败原因载体
     * @param reason 当前失败原因
     */
    private void recordTypeBlockAppendFailure(StringBuilder failureReason, String reason) {
        if (failureReason == null || StringUtils.isEmpty(reason)) {
            return;
        }
        if (failureReason.length() > 0) {
            failureReason.append("；");
        }
        failureReason.append(reason);
    }

    /**
     * 单机台换活字块目标量决策。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param sku SKU
     * @param startTime 开产时间
     * @param switchStartTime 切换开始时间
     * @param shifts 班次
     * @param isEnding 是否收尾
     * @param isSingleMachine 是否单机台
     * @param typeBlockExpansionContinuation 是否多机台续排剩余量
     */
    private void applySingleMachineTypeBlockTargetRule(LhScheduleContext context,
                                                       MachineScheduleDTO machine,
                                                       SkuScheduleDTO sku,
                                                       Date startTime,
                                                       Date switchStartTime,
                                                       List<LhShiftConfigVO> shifts,
                                                       boolean isEnding,
                                                       boolean isSingleMachine,
                                                       boolean typeBlockExpansionContinuation,
                                                       boolean embryoStockEndingTargetApplied) {
        if (sku == null || machine == null) {
            return;
        }
        int originalTargetQty = sku.resolveTargetScheduleQty();
        int windowCapacityQty = startTime == null ? 0
                : getTargetScheduleQtyResolver().calcMachineAvailableCapacityByStartTime(
                context, sku, machine, switchStartTime, startTime, shifts,
                ScheduleTypeEnum.TYPE_BLOCK.getCode());
        String appliedRule = "沿用原规则";
        if (embryoStockEndingTargetApplied) {
            appliedRule = "成型胎胚库存收尾-直接按胎胚库存";
        } else if (typeBlockExpansionContinuation) {
            sku.setStrictTargetQty(isEnding || sku.isStrictTargetQty());
            appliedRule = "多机台续排剩余目标量";
        } else if (isSingleMachine && isEnding) {
            getTargetScheduleQtyResolver().upsizeEndingTargetQty(context, sku);
            appliedRule = getTargetScheduleQtyResolver().isSharedEmbryoInWindow(context, sku)
                    ? "单机台收尾共用胎胚仅按余量" : "单机台收尾MAX(余量,胎胚库存)";
        } else if (isSingleMachine && getTargetScheduleQtyResolver().isFullCapacityMode(context)) {
            boolean newSpecExpansionAvailable = !DailyMachineExpansionPlanner.isDailyLookAheadCapacitySatisfied(
                    this.lhDailyMouldCalcService, context, sku, 1,
                    ScheduleTypeEnum.TYPE_BLOCK.getCode())
                    && hasSchedulableNewSpecExpansionMachine(context, machine, sku, shifts);
            int adoptedTargetQty = resolveSingleMachineTypeBlockTargetQty(
                    sku, windowCapacityQty, newSpecExpansionAvailable);
            sku.setTargetScheduleQty(adoptedTargetQty);
            sku.setRemainingScheduleQty(adoptedTargetQty);
            sku.setStrictTargetQty(false);
            appliedRule = newSpecExpansionAvailable
                    ? "单机台换活字块承接+记录新增扩机需求残量"
                    : resolveSingleMachineWindowRuleName(sku, adoptedTargetQty, windowCapacityQty);
        } else if (isEnding) {
            sku.setStrictTargetQty(true);
            appliedRule = isSingleMachine ? "单机台收尾严格原目标" : "多机台沿用原规则";
        } else if (!isSingleMachine) {
            appliedRule = "多机台沿用原规则";
        } else if (!getTargetScheduleQtyResolver().isFullCapacityMode(context)) {
            appliedRule = "按需求模式沿用原规则";
        }
        log.info("S4.4换活字块目标量决策, scene: typeBlock, materialCode: {}, machineCode: {}, isSingleMachine: {}, "
                        + "isEnding: {}, surplusQty: {}, embryoStock: {}, originalTargetQty: {}, windowCapacityQty: {}, "
                        + "adoptedTargetQty: {}, rule: {}",
                sku.getMaterialCode(), machine.getMachineCode(), isSingleMachine, isEnding,
                Math.max(0, sku.getSurplusQty()), Math.max(0, sku.getEmbryoStock()), originalTargetQty,
                windowCapacityQty, sku.resolveTargetScheduleQty(), appliedRule);
    }

    /**
     * 解析单机台换活字块在非收尾场景下的目标量。
     *
     * <p>若当前窗口仍存在新增换模扩机能力，则保留完整窗口账本需求量，用于换活字块
     * 部分成功后的真实残量审计；该信号只决定本次采用目标量，不表示残量重新进入 S4.5。
     * 换活字块一旦形成有效结果，完整成功和部分成功都按既定业务规则退出当前批次通用
     * 新增队列，残量只写独立过程日志并由下一滚动窗口重新计算。没有扩机能力时，继续
     * 沿用当前单机台满排窗口口径。</p>
     *
     * @param sku SKU
     * @param windowCapacityQty 当前机台窗口产能
     * @param newSpecExpansionAvailable 是否存在可承接的新增换模机台
     * @return 目标量
     */
    private int resolveSingleMachineTypeBlockTargetQty(SkuScheduleDTO sku,
                                                       int windowCapacityQty,
                                                       boolean newSpecExpansionAvailable) {
        int adoptedTargetQty = Math.max(0, windowCapacityQty);
        if (newSpecExpansionAvailable) {
            adoptedTargetQty = Math.max(adoptedTargetQty, resolveTypeBlockExpansionDemandQty(sku));
        }
        int surplusQty = sku == null ? 0 : Math.max(0, sku.getSurplusQty());
        if (surplusQty > 0 && surplusQty < adoptedTargetQty) {
            return surplusQty;
        }
        return adoptedTargetQty;
    }

    /**
     * 解析单机台换活字块在可扩机场景下应保留的窗口需求量。
     *
     * @param sku SKU
     * @return 窗口需求量
     */
    private int resolveTypeBlockExpansionDemandQty(SkuScheduleDTO sku) {
        if (sku == null) {
            return 0;
        }
        int quotaDemandQty = SkuDailyPlanQuotaUtil.sumRemainingQty(sku.getDailyPlanQuotaMap());
        int windowRemainingQty = Math.max(0, sku.getWindowRemainingPlanQty());
        if (windowRemainingQty > 0) {
            quotaDemandQty = quotaDemandQty > 0 ? Math.min(quotaDemandQty, windowRemainingQty) : windowRemainingQty;
        }
        if (quotaDemandQty > 0) {
            return quotaDemandQty;
        }
        int windowPlanQty = Math.max(0, sku.getWindowPlanQty());
        if (windowPlanQty > 0) {
            return windowPlanQty;
        }
        return Math.max(0, sku.resolveTargetScheduleQty());
    }

    /**
     * 判断当前换活字块 SKU 是否仍有可承接的新增换模机台。
     *
     * @param context 排程上下文
     * @param currentMachine 当前换活字块机台
     * @param sku SKU
     * @param shifts 班次窗口
     * @return true-存在可承接机台
     */
    private boolean hasSchedulableNewSpecExpansionMachine(LhScheduleContext context,
                                                          MachineScheduleDTO currentMachine,
                                                          SkuScheduleDTO sku,
                                                          List<LhShiftConfigVO> shifts) {
        if (context == null
                || currentMachine == null
                || sku == null
                || CollectionUtils.isEmpty(shifts)
                || CollectionUtils.isEmpty(context.getMachineScheduleMap())) {
            return false;
        }
        for (MachineScheduleDTO candidateMachine : context.getMachineScheduleMap().values()) {
            if (candidateMachine == null
                    || StringUtils.isEmpty(candidateMachine.getMachineCode())
                    || context.isContinuousStopHoldMachine(candidateMachine.getMachineCode())
                    || StringUtils.equals(candidateMachine.getMachineCode(), currentMachine.getMachineCode())
                    || candidateMachine.getEstimatedEndTime() == null
                    || !isMachineHardMatched(context, candidateMachine, sku)) {
                continue;
            }
            if (canScheduleSpecifySkuByNewSpecPath(
                    context, candidateMachine, sku, shifts, candidateMachine.getEstimatedEndTime())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析单机台满排窗口规则日志名称。
     *
     * @param sku SKU
     * @param adoptedTargetQty 目标量
     * @param windowCapacityQty 当前机台窗口产能
     * @return 规则名称
     */
    private String resolveSingleMachineWindowRuleName(SkuScheduleDTO sku,
                                                      int adoptedTargetQty,
                                                      int windowCapacityQty) {
        if (sku != null && adoptedTargetQty < Math.max(0, windowCapacityQty)) {
            return "单机台非收尾满排窗口(余量封顶)";
        }
        return "单机台非收尾满排窗口";
    }

    /**
     * 统计当前轮可承接指定换活字块 SKU 的机台数。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param activeMachines 当前轮可尝试机台
     * @return 可承接机台数
     */
    private int countEligibleTypeBlockMachines(LhScheduleContext context,
                                               SkuScheduleDTO sku,
                                               List<MachineScheduleDTO> activeMachines) {
        if (context == null || sku == null || CollectionUtils.isEmpty(activeMachines)) {
            return 0;
        }
        int eligibleCount = 0;
        for (MachineScheduleDTO activeMachine : activeMachines) {
            if (activeMachine != null && isTypeBlockCandidate(context, activeMachine, sku, false)) {
                eligibleCount++;
            }
        }
        return eligibleCount;
    }

    /**
     * 判断定点物料在当前机台和窗口内是否可排。
     *
     * @param context 排程上下文
     * @param machine 当前机台
     * @param specifySku 定点物料
     * @param shifts 排程窗口班次
     * @param endingTime 机台切换起点
     * @return true-可排，false-不可排
     */
    private boolean canScheduleSpecifySkuOnMachine(LhScheduleContext context,
                                                   MachineScheduleDTO machine,
                                                   SkuScheduleDTO specifySku,
                                                   List<LhShiftConfigVO> shifts,
                                                   Date endingTime) {
        if (context == null
                || machine == null
                || specifySku == null
                || endingTime == null
                || CollectionUtils.isEmpty(shifts)
                || StringUtils.isEmpty(machine.getMachineCode())) {
            return false;
        }
        if (!isMachineHardMatched(context, machine, specifySku)) {
            log.debug("定点物料预判未通过机台硬性准入, machineCode: {}, materialCode: {}",
                    machine.getMachineCode(), specifySku.getMaterialCode());
            return false;
        }
        if (isTypeBlockCandidate(context, machine, specifySku)) {
            Date typeBlockSwitchStartTime = allocateTypeBlockSwitchStartTime(context, machine, specifySku, endingTime);
            Date typeBlockStartTime = resolveTypeBlockProductionStartTime(
                    context, machine, specifySku, endingTime, typeBlockSwitchStartTime, shifts);
            if (typeBlockStartTime == null || typeBlockSwitchStartTime == null) {
                return false;
            }
            try {
                int refinedTargetQty = getTargetScheduleQtyResolver().refineTargetQtyByMachineCapacity(
                        context,
                        specifySku,
                        machine,
                        typeBlockSwitchStartTime,
                        typeBlockStartTime,
                        shifts,
                        ScheduleTypeEnum.TYPE_BLOCK.getCode());
                if (refinedTargetQty <= 0) {
                    log.debug("定点物料换活字块预判不可排, machineCode: {}, materialCode: {}, startTime: {}",
                            machine.getMachineCode(), specifySku.getMaterialCode(),
                            LhScheduleTimeUtil.formatDateTime(typeBlockStartTime));
                    return false;
                }
                return true;
            } finally {
                getMouldChangeBalanceStrategy().rollbackMouldChange(context, typeBlockSwitchStartTime);
            }
        }
        return canScheduleSpecifySkuByNewSpecPath(context, machine, specifySku, shifts, endingTime);
    }

    /**
     * 判断候选SKU是否应优先走新增换模主链。
     *
     * @param context 排程上下文
     * @param machine 当前机台
     * @param sku 候选SKU
     * @return true-应走新增换模主链
     */
    private boolean shouldPreferNewSpecPath(LhScheduleContext context,
                                             MachineScheduleDTO machine,
                                             SkuScheduleDTO sku) {
        if (!isTypeBlockCandidate(context, machine, sku)) {
            return true;
        }
        return false;
    }

    /**
     * 判断候选SKU是否满足机台硬性准入。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param sku SKU
     * @return true-满足，false-不满足
     */
    private boolean isMachineHardMatched(LhScheduleContext context,
                                         MachineScheduleDTO machine,
                                         SkuScheduleDTO sku) {
        return LhMachineHardMatchUtil.isMachineHardMatched(context, sku, machine);
    }

    /**
     * 按新增换模链路预判定点物料是否可排。
     *
     * @param context 排程上下文
     * @param machine 当前机台
     * @param specifySku 定点物料
     * @param shifts 排程窗口班次
     * @param endingTime 机台切换起点
     * @return true-可排，false-不可排
     */
    private boolean canScheduleSpecifySkuByNewSpecPath(LhScheduleContext context,
                                                       MachineScheduleDTO machine,
                                                       SkuScheduleDTO specifySku,
                                                       List<LhShiftConfigVO> shifts,
                                                       Date endingTime) {
        Date machineReadyTime = getCapacityCalculateStrategy().calculateStartTime(
                context, machine.getMachineCode(), endingTime);
        Date switchReadyTime = machineReadyTime;
        // 试制SKU换模需在早班完成，不受开产模式限制
        switchReadyTime = ShiftProductionControlUtil.resolveEarliestSwitchStartTime(
                context, switchReadyTime, specifySku);
        int switchDurationHours = LhScheduleTimeUtil.getMouldChangeTotalHours(context);
        Date mouldChangeStartTime = getMouldChangeBalanceStrategy().allocateMouldChange(
                context,
                machine.getMachineCode(),
                switchReadyTime,
                switchDurationHours,
                specifySku,
                IMouldChangeBalanceStrategy.ACTION_NEW_SPEC_MOULD_CHANGE);
        if (mouldChangeStartTime == null) {
            log.debug("定点物料新增换模预判不可排, machineCode: {}, materialCode: {}, 原因: 无可用换模窗口",
                    machine.getMachineCode(), specifySku.getMaterialCode());
            return false;
        }
        Date inspectionTime = null;
        try {
            Date mouldChangeCompleteTime = LhScheduleTimeUtil.addHours(mouldChangeStartTime, switchDurationHours);
            boolean plannedRepairAffectingSwitch = ShiftCapacityResolverUtil.isPlannedRepairAffectingSwitch(
                    context, context.getDevicePlanShutList(), machine.getMachineCode(), endingTime,
                    mouldChangeStartTime, mouldChangeCompleteTime);
            Date firstInspectionBaseTime = plannedRepairAffectingSwitch
                    ? ShiftCapacityResolverUtil.resolvePlannedRepairProductionReadyTime(
                    context, context.getDevicePlanShutList(), machine.getMachineCode(), endingTime,
                    mouldChangeStartTime, mouldChangeCompleteTime)
                    : mouldChangeCompleteTime;
            /*
             * 首检计数班次必须由真实换模完成时刻决定。维修结束和预热仅抬高正式生产下限，
             * 不能把已经发生在准备阶段的首检顺序、数量归属一起搬到维修后的班次。
             */
            Date firstInspectionAttributionTime = FirstInspectionQtyUtil.resolveFirstInspectionAttributionTime(
                    context, specifySku, shifts, mouldChangeCompleteTime,
                    ScheduleTypeEnum.NEW_SPEC.getCode());
            if (firstInspectionAttributionTime == null) {
                log.debug("定点物料新增换模预判不可排, machineCode: {}, materialCode: {}, 原因: 首检归属班次为空",
                        machine.getMachineCode(), specifySku.getMaterialCode());
                return false;
            }
            inspectionTime = getFirstInspectionBalanceStrategy().allocateInspection(
                    context, machine.getMachineCode(), firstInspectionAttributionTime);
            if (inspectionTime == null) {
                log.debug("定点物料新增换模预判不可排, machineCode: {}, materialCode: {}, 原因: 首检窗口分配失败",
                        machine.getMachineCode(), specifySku.getMaterialCode());
                return false;
            }
            Date defaultProductionStartTime = plannedRepairAffectingSwitch
                    ? firstInspectionBaseTime : mouldChangeCompleteTime;
            Date productionStartTime = FirstInspectionQtyUtil.resolveTrialProductionStartTime(
                    context, specifySku, shifts, firstInspectionBaseTime, defaultProductionStartTime,
                    ScheduleTypeEnum.NEW_SPEC.getCode());
            if (productionStartTime == null) {
                log.debug("定点物料新增换模预判不可排, machineCode: {}, materialCode: {}, 原因: 试制中班开产时间为空",
                        machine.getMachineCode(), specifySku.getMaterialCode());
                return false;
            }
            int machineMouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(machine);
            int runtimeShiftCapacity = ShiftCapacityResolverUtil.resolveRuntimeShiftCapacity(
                    context, machine, specifySku.getShiftCapacity());
            Date firstProductionStartTime = ShiftProductionControlUtil.resolveFirstSchedulableStartIgnoringCleaning(
                    context,
                    machine.getMachineCode(),
                    productionStartTime,
                    shifts,
                    runtimeShiftCapacity,
                    specifySku.getLhTimeSeconds(),
                    machineMouldQty);
            if (firstProductionStartTime == null) {
                log.debug("定点物料新增换模预判不可排, machineCode: {}, materialCode: {}, 原因: 窗口内无可开产时间",
                        machine.getMachineCode(), specifySku.getMaterialCode());
                return false;
            }
            int refinedTargetQty = getTargetScheduleQtyResolver().refineTargetQtyByMachineCapacity(
                    context, specifySku, machine, mouldChangeStartTime, firstProductionStartTime,
                    shifts, ScheduleTypeEnum.NEW_SPEC.getCode());
            if (refinedTargetQty <= 0) {
                log.debug("定点物料新增换模预判不可排, machineCode: {}, materialCode: {}, 原因: 收敛后目标量为0",
                        machine.getMachineCode(), specifySku.getMaterialCode());
                return false;
            }
            return true;
        } finally {
            if (inspectionTime != null) {
                getFirstInspectionBalanceStrategy().rollbackInspection(context, inspectionTime);
            }
            getMouldChangeBalanceStrategy().rollbackMouldChange(context, mouldChangeStartTime);
        }
    }

    /**
     * 判断当前候选是否需要走真实换模均衡能力。
     *
     * @param context 排程上下文
     * @param machine 当前机台
     * @param sku 候选SKU
     * @return true-需走新增换模主链
     */
    private boolean requiresMouldChangeBalance(LhScheduleContext context,
                                               MachineScheduleDTO machine,
                                               SkuScheduleDTO sku) {
        if (context == null
                || machine == null
                || sku == null
                || StringUtils.isEmpty(machine.getCurrentMaterialCode())
                || StringUtils.isEmpty(sku.getMaterialCode())) {
            return false;
        }
        Set<String> currentMouldCodes = resolveMouldCodeSet(context, machine.getCurrentMaterialCode());
        Set<String> targetMouldCodes = resolveMouldCodeSet(context, sku.getMaterialCode());
        if (CollectionUtils.isEmpty(currentMouldCodes) || CollectionUtils.isEmpty(targetMouldCodes)) {
            return false;
        }
        for (String targetMouldCode : targetMouldCodes) {
            if (currentMouldCodes.contains(targetMouldCode)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 输出衔接机台排序总览日志。
     *
     * @param context 排程上下文
     * @param endingMachines 衔接机台列表
     */
    private void traceEndingMachineOrder(LhScheduleContext context,
                                         List<MachineScheduleDTO> endingMachines,
                                         Map<String, String> machineTriggerSourceMap) {
        if (!PriorityTraceLogHelper.isEnabled(context)) {
            return;
        }
        String title = "衔接机台排序总览【换活字块衔接】";
        int topN = LhScheduleConstant.MACHINE_SORT_TRACE_TOP_N;
        int machineCount = PriorityTraceLogHelper.sizeOf(endingMachines);
        int outputCount = Math.min(topN, machineCount);

        StringBuilder detailBuilder = new StringBuilder(1024);
        PriorityTraceLogHelper.appendTitleHeader(detailBuilder, title);
        PriorityTraceLogHelper.appendLine(detailBuilder,
                PriorityTraceLogHelper.kv("排程日期", PriorityTraceLogHelper.formatDateTime(context.getScheduleDate()))
                        + ", " + PriorityTraceLogHelper.kv("候选机台数量", machineCount)
                        + ", " + PriorityTraceLogHelper.kv("输出范围", "TOP" + outputCount));

        if (CollectionUtils.isEmpty(endingMachines)) {
            PriorityTraceLogHelper.appendLine(detailBuilder, "无可输出的换活字块候选机台");
        } else {
            // 预建换活字块候选标记缓存，避免同机台重复遍历全体SKU
            Map<String, Boolean> canChangeLetterCache = new HashMap<>(Math.min(outputCount, 16));
            for (int i = 0; i < outputCount; i++) {
                MachineScheduleDTO m = endingMachines.get(i);
                canChangeLetterCache.put(m.getMachineCode(), resolveCanChangeLetterFlag(context, m));
            }

            List<String> levelNames = Arrays.asList(
                    "L1_触发来源", "L2_切换就绪时间", "L3_收尾时间");
            for (int i = 0; i < outputCount; i++) {
                MachineScheduleDTO machine = endingMachines.get(i);
                Date estimatedEndTime = machine.getEstimatedEndTime();
                Date readyTime = resolveTypeBlockSortReadyTime(context, machine);
                String triggerSource = machineTriggerSourceMap != null
                        ? machineTriggerSourceMap.get(machine.getMachineCode()) : null;
                int triggerOrder = resolveTypeBlockTriggerOrder(triggerSource);
                String triggerDesc = triggerOrder == 0 ? "收尾触发" : (triggerOrder == 1 ? "兜底触发" : "其他");
                boolean canChangeLetter = Boolean.TRUE.equals(canChangeLetterCache.get(machine.getMachineCode()));
                String machineEmbryoDesc = resolveMachineEmbryoDesc(context, machine);
                String machineEmbryoCode = resolveMachineEmbryoCode(context, machine);
                String machineMainPattern = resolveMachineMainPatternStrict(context, machine);
                String machineSpecCode = resolveMachineSpecCode(context, machine);

                List<String> sortKeyLevels = Arrays.asList(
                        "L1_触发来源=" + triggerDesc,
                        "L2_切换就绪=" + PriorityTraceLogHelper.formatDateTime(readyTime),
                        "L3_收尾时间=" + PriorityTraceLogHelper.formatDateTime(estimatedEndTime));
                String sortKey = PriorityTraceLogHelper.formatSortKey(sortKeyLevels);
                String hitLevel;
                if (triggerOrder == 0) {
                    hitLevel = "命中L1收尾触发优先";
                } else if (triggerOrder == 1) {
                    hitLevel = "命中L1兜底触发";
                } else {
                    hitLevel = "兜底排序";
                }

                PriorityTraceLogHelper.appendLine(detailBuilder,
                        (i + 1)
                                + ". " + PriorityTraceLogHelper.kv("机台", machine.getMachineCode())
                                + ", " + PriorityTraceLogHelper.kv("当前物料", machine.getCurrentMaterialCode())
                                + ", " + PriorityTraceLogHelper.kv("触发来源", triggerDesc)
                                + ", " + PriorityTraceLogHelper.kv("收尾", PriorityTraceLogHelper.oneZero(machine.isEnding()))
                                + ", " + PriorityTraceLogHelper.kv("收尾时间", PriorityTraceLogHelper.formatDateTime(estimatedEndTime))
                                + ", " + PriorityTraceLogHelper.kv("切换就绪时间", PriorityTraceLogHelper.formatDateTime(readyTime))
                                + ", " + PriorityTraceLogHelper.kv("可换活字块", PriorityTraceLogHelper.oneZero(canChangeLetter))
                                + ", " + PriorityTraceLogHelper.kv("胎胚代码", PriorityTraceLogHelper.safeText(machineEmbryoCode))
                                + ", " + PriorityTraceLogHelper.kv("胎胚描述", machineEmbryoDesc)
                                + ", " + PriorityTraceLogHelper.kv("主花纹", machineMainPattern)
                                + ", " + PriorityTraceLogHelper.kv("规格", machineSpecCode)
                                + ", " + PriorityTraceLogHelper.kv("机台顺序", machine.getMachineOrder())
                                + ", " + PriorityTraceLogHelper.kv("SortKey", sortKey)
                                + ", " + PriorityTraceLogHelper.kv("HitLevel", hitLevel));
            }
            if (machineCount > topN) {
                PriorityTraceLogHelper.appendLine(detailBuilder,
                        "... 共" + machineCount + "台，仅展示前" + topN + "台");
            }
        }
        PriorityTraceLogHelper.appendTitleFooter(detailBuilder);
        String detail = detailBuilder.toString().trim();
        PriorityTraceLogHelper.logSortSummary(log, context, title, detail);
    }

    /**
     * 判断机台当前在机物料是否可做换活字块衔接。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @return true-可换活字块
     */
    private boolean resolveCanChangeLetterFlag(LhScheduleContext context, MachineScheduleDTO machine) {
        if (context == null || machine == null || StringUtils.isEmpty(machine.getCurrentMaterialCode())) {
            return false;
        }
        for (SkuScheduleDTO sku : context.getNewSpecSkuList()) {
            if (sku == null || StringUtils.isEmpty(sku.getMaterialCode())) {
                continue;
            }
            if (isTypeBlockCandidate(context, machine, sku, false)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 输出换活字块机台反选SKU决策日志（含TOP5候选SKU列表、过滤统计、SortKey、HitLevel）。
     *
     * @param context 排程上下文
     * @param machine 收尾机台
     * @param candidates 候选SKU
     * @param selectedSku 选中SKU
     * @param matchedLayer 命中层级
     * @param success 是否成功
     * @param switchStartTime 换活字块开始时间
     * @param startTime 开产时间
     * @param triggerSource 触发来源
     */
    private void traceTypeBlockDecision(LhScheduleContext context, MachineScheduleDTO machine,
                                        List<SkuScheduleDTO> candidates,
                                        SkuScheduleDTO selectedSku,
                                        String matchedLayer,
                                        boolean success,
                                        Date switchStartTime,
                                        Date startTime,
                                        String triggerSource,
                                        String failureReason) {
        if (!PriorityTraceLogHelper.isEnabled(context)) {
            return;
        }
        String title = "收尾机台衔接决策【换活字块机台反选SKU TOP5列表】";
        StringBuilder detailBuilder = new StringBuilder(1024);
        PriorityTraceLogHelper.appendTitleHeader(detailBuilder, title);

        String machineEmbryoDesc = resolveMachineEmbryoDesc(context, machine);
        String machineEmbryoCode = resolveMachineEmbryoCode(context, machine);
        String machineMainPattern = resolveMachineMainPatternStrict(context, machine);
        String machineSpecCode = resolveMachineSpecCode(context, machine);

        PriorityTraceLogHelper.appendLine(detailBuilder,
                PriorityTraceLogHelper.kv("排程日期", PriorityTraceLogHelper.formatDateTime(context.getScheduleDate()))
                        + ", " + PriorityTraceLogHelper.kv("当前机台", machine.getMachineCode())
                        + ", " + PriorityTraceLogHelper.kv("当前在机SKU", machine.getCurrentMaterialCode())
                        + ", " + PriorityTraceLogHelper.kv("当前胎胚代码", PriorityTraceLogHelper.safeText(machineEmbryoCode))
                        + ", " + PriorityTraceLogHelper.kv("当前胎胚描述", machineEmbryoDesc)
                        + ", " + PriorityTraceLogHelper.kv("当前主花纹", machineMainPattern)
                        + ", " + PriorityTraceLogHelper.kv("当前规格", machineSpecCode)
                        + ", " + PriorityTraceLogHelper.kv("收尾时间", PriorityTraceLogHelper.formatDateTime(machine.getEstimatedEndTime())));

        int totalCandidates = PriorityTraceLogHelper.sizeOf(candidates);
        int newSpecTotal = PriorityTraceLogHelper.sizeOf(context.getNewSpecSkuList());
        int filteredCount = newSpecTotal - totalCandidates;
        PriorityTraceLogHelper.appendLine(detailBuilder,
                PriorityTraceLogHelper.kv("候选SKU总数", totalCandidates)
                        + ", " + PriorityTraceLogHelper.kv("过滤SKU数", filteredCount)
                        + ", 过滤原因统计: 未满足换活字块准入条件=" + Math.max(0, filteredCount));

        // 输出候选 TOP5（同胎胚+同模具）
        int topN = LhScheduleConstant.TYPE_BLOCK_SKU_CANDIDATE_TOP_N;
        if (!CollectionUtils.isEmpty(candidates)) {
            int outputCount = Math.min(topN, totalCandidates);
            PriorityTraceLogHelper.appendLine(detailBuilder,
                    "候选(同胎胚+同模具) TOP" + outputCount + ":");
            appendSkuCandidateLines(detailBuilder, context, machine, candidates, outputCount);
        }

        // 最终选中
        String selectReason = resolveTypeBlockSelectReason(context, machine, selectedSku, candidates);
        PriorityTraceLogHelper.appendLine(detailBuilder,
                PriorityTraceLogHelper.kv("命中层级", matchedLayer)
                        + ", " + PriorityTraceLogHelper.kv("选中SKU", selectedSku == null ? "-" : selectedSku.getMaterialCode())
                        + ", " + PriorityTraceLogHelper.kv("选中原因", selectReason)
                        + ", " + PriorityTraceLogHelper.kv("衔接结果", success ? "成功" : "未衔接")
                        + ", " + PriorityTraceLogHelper.kv("失败原因", success ? "-" : PriorityTraceLogHelper.safeText(failureReason))
                        + ", " + PriorityTraceLogHelper.kv("换活字块开始时间", PriorityTraceLogHelper.formatDateTime(switchStartTime))
                        + ", " + PriorityTraceLogHelper.kv("开产时间", PriorityTraceLogHelper.formatDateTime(startTime)));

        PriorityTraceLogHelper.appendTitleFooter(detailBuilder);
        String detail = detailBuilder.toString().trim();
        PriorityTraceLogHelper.logSortSummary(log, context, title, detail);
    }

    /**
     * 逐行输出候选SKU明细。
     *
     * @param builder 日志构建器
     * @param context 排程上下文
     * @param machine 机台
     * @param candidates 候选SKU列表
     * @param outputCount 输出数量
     */
    private void appendSkuCandidateLines(StringBuilder builder, LhScheduleContext context,
                                         MachineScheduleDTO machine,
                                         List<SkuScheduleDTO> candidates,
                                         int outputCount) {
        // 机台当前在机模具号集合，仅计算一次
        Set<String> machineMouldCodeSet = resolveMouldCodeSet(context, machine.getCurrentMaterialCode());
        String machineMouldCodes = CollectionUtils.isEmpty(machineMouldCodeSet)
                ? "-" : String.join(",", machineMouldCodeSet);

        for (int i = 0; i < outputCount; i++) {
            SkuScheduleDTO sku = candidates.get(i);
            boolean sameCarcass = isSameCarcass(context, machine, sku);
            boolean sameMold = isSameMold(context, machine, sku);
            boolean canChange = isTypeBlockCandidate(context, machine, sku, false);
            String skuEmbryoDesc = resolveSkuEmbryoDesc(context, sku);

            // SKU所有模具号 及 与机台当前模具的交集
            Set<String> skuMouldCodeSet = resolveMouldCodeSet(context, sku.getMaterialCode());
            String skuMouldCodes = CollectionUtils.isEmpty(skuMouldCodeSet)
                    ? "-" : String.join(",", skuMouldCodeSet);
            String intersectMouldCodes = "-";
            if (!CollectionUtils.isEmpty(machineMouldCodeSet) && !CollectionUtils.isEmpty(skuMouldCodeSet)) {
                List<String> intersectList = new ArrayList<>(machineMouldCodeSet);
                intersectList.retainAll(skuMouldCodeSet);
                intersectMouldCodes = intersectList.isEmpty() ? "-" : String.join(",", intersectList);
            }

            String sortKey = PriorityTraceLogHelper.formatSortKey(Arrays.asList(
                    "L1_同胎胚同模具=" + (sameCarcass && sameMold ? 1 : 0),
                    "L2_物料编码兜底=" + PriorityTraceLogHelper.safeText(sku.getMaterialCode())));
            String hitLevel = sameCarcass && sameMold ? "命中L1同胎胚+同模具" : "-";

            PriorityTraceLogHelper.appendLine(builder,
                    (i + 1)
                            + ". " + PriorityTraceLogHelper.kv("物料编码", sku.getMaterialCode())
                            + ", " + PriorityTraceLogHelper.kv("描述", sku.getMaterialDesc())
                            + ", " + PriorityTraceLogHelper.kv("预计收尾", PriorityTraceLogHelper.oneZero(endingJudgmentStrategy.isExpectedEnding(context, sku)))
                            + ", " + PriorityTraceLogHelper.kv("待排产量", sku.resolveTargetScheduleQty())
                            + ", " + PriorityTraceLogHelper.kv("月计划余量", sku.getSurplusQty())
                            + ", " + PriorityTraceLogHelper.kv("胎胚库存", sku.getEmbryoStock())
                            + ", " + PriorityTraceLogHelper.kv("胎胚代码", PriorityTraceLogHelper.safeText(sku.getEmbryoCode()))
                            + ", " + PriorityTraceLogHelper.kv("胎胚描述", skuEmbryoDesc)
                            + ", " + PriorityTraceLogHelper.kv("规格", sku.getSpecCode())
                            + ", " + PriorityTraceLogHelper.kv("SKU模具号", skuMouldCodes)
                            + ", " + PriorityTraceLogHelper.kv("交集模具号", intersectMouldCodes)
                            + ", " + PriorityTraceLogHelper.kv("同胎胚", PriorityTraceLogHelper.oneZero(sameCarcass))
                            + ", " + PriorityTraceLogHelper.kv("同模具", PriorityTraceLogHelper.oneZero(sameMold))
                            + ", " + PriorityTraceLogHelper.kv("满足换活字块", PriorityTraceLogHelper.oneZero(canChange))
                            + ", " + PriorityTraceLogHelper.kv("SortKey", sortKey)
                            + ", " + PriorityTraceLogHelper.kv("HitLevel", hitLevel));
        }
    }

    /**
     * 解析换活字块选中SKU原因。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param selectedSku 选中SKU
     * @param candidates 候选SKU
     * @return 选中原因
     */
    private String resolveTypeBlockSelectReason(LhScheduleContext context, MachineScheduleDTO machine,
                                                SkuScheduleDTO selectedSku,
                                                List<SkuScheduleDTO> candidates) {
        if (selectedSku == null) {
            return "无候选SKU";
        }
        List<String> reasons = new ArrayList<>(4);
        if (!CollectionUtils.isEmpty(candidates) && candidates.contains(selectedSku)) {
            reasons.add("同胎胚+同模具");
        }
        if (isSameCarcass(context, machine, selectedSku)) {
            reasons.add("胎胚一致");
        }
        if (isSameMold(context, machine, selectedSku)) {
            reasons.add("模具一致");
        }
        if (isTypeBlockCandidate(context, machine, selectedSku, false)) {
            reasons.add("满足换活字块条件");
        }
        if (reasons.isEmpty()) {
            reasons.add("排序首位默认");
        }
        return String.join("，", reasons);
    }

    /**
     * 组装候选SKU编码摘要。
     *
     * @param skuList SKU列表
     * @return 摘要文本
     */
    private String buildSkuCodeSummary(List<SkuScheduleDTO> skuList) {
        if (CollectionUtils.isEmpty(skuList)) {
            return "-";
        }
        List<String> materialCodes = new ArrayList<>(skuList.size());
        for (SkuScheduleDTO sku : skuList) {
            materialCodes.add(PriorityTraceLogHelper.safeText(sku.getMaterialCode()));
        }
        return String.join(",", materialCodes);
    }

    /**
     * 识别释放后可优先参与换活字块的续作机台。
     *
     * <p>包括首日无计划释放机台、续作收尾小余量释放机台，以及窗口内无日计划、续作未形成
     * 有效结果等统一登记的续作释放机台。该入口只扩展 S4.4 换活字块候选机台来源，
     * 不改变 S4.5 新增排序和机台筛选规则。</p>
     *
     * @param context 排程上下文
     * @return 释放机台列表
     */
    private List<MachineScheduleDTO> resolveReleasedTypeBlockMachines(LhScheduleContext context) {
        List<MachineScheduleDTO> releasedMachineList = new ArrayList<>();
        if (context == null
                || CollectionUtils.isEmpty(context.getMachineScheduleMap())) {
            return releasedMachineList;
        }
        Set<String> releasedMachineCodeSet = new LinkedHashSet<String>(16);
        if (!CollectionUtils.isEmpty(context.getFirstDayNoPlanReleasedContinuousMachineCodeSet())) {
            releasedMachineCodeSet.addAll(context.getFirstDayNoPlanReleasedContinuousMachineCodeSet());
        }
        if (!CollectionUtils.isEmpty(context.getTypeBlockReleasedContinuousMachineCodeSet())) {
            releasedMachineCodeSet.addAll(context.getTypeBlockReleasedContinuousMachineCodeSet());
        }
        if (!CollectionUtils.isEmpty(context.getReleasedContinuousMachineCodeSet())) {
            releasedMachineCodeSet.addAll(context.getReleasedContinuousMachineCodeSet());
        }
        for (String machineCode : releasedMachineCodeSet) {
            if (StringUtils.isEmpty(machineCode)) {
                continue;
            }
            if (context.isContinuousStopHoldMachine(machineCode)) {
                log.info("续作停产保机机台跳过换活字块释放候选, machineCode: {}", machineCode);
                continue;
            }
            MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
            if (machine == null
                    || StringUtils.isEmpty(machine.getCurrentMaterialCode())
                    || machine.getEstimatedEndTime() == null) {
                continue;
            }
            if (isMachineAssignedContinuousResult(context, machineCode)) {
                log.info("续作释放机台已有续作分配，跳过换活字块接管, machineCode: {}, currentMaterialCode: {}, triggerSource: {}",
                        machineCode, machine.getCurrentMaterialCode(),
                        resolveReleasedTypeBlockTriggerSource(context, machineCode));
                continue;
            }
            releasedMachineList.add(machine);
        }
        return releasedMachineList;
    }

    /**
     * 解析续作释放机台进入换活字块的触发来源。
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @return 触发来源
     */
    private String resolveReleasedTypeBlockTriggerSource(LhScheduleContext context, String machineCode) {
        if (context != null && StringUtils.isNotEmpty(machineCode)
                && !CollectionUtils.isEmpty(context.getTypeBlockReleasedContinuousMachineCodeSet())
                && context.getTypeBlockReleasedContinuousMachineCodeSet().contains(machineCode)) {
            return TYPE_BLOCK_TRIGGER_SMALL_ENDING_SURPLUS_RELEASE;
        }
        if (context != null && StringUtils.isNotEmpty(machineCode)
                && !CollectionUtils.isEmpty(context.getFirstDayNoPlanReleasedContinuousMachineCodeSet())
                && context.getFirstDayNoPlanReleasedContinuousMachineCodeSet().contains(machineCode)) {
            return TYPE_BLOCK_TRIGGER_FIRST_DAY_NO_PLAN_RELEASE;
        }
        if (context != null && StringUtils.isNotEmpty(machineCode)
                && !CollectionUtils.isEmpty(context.getReleasedContinuousMachineCodeSet())
                && context.getReleasedContinuousMachineCodeSet().contains(machineCode)) {
            return TYPE_BLOCK_TRIGGER_CONTINUOUS_RELEASE;
        }
        return TYPE_BLOCK_TRIGGER_FIRST_DAY_NO_PLAN_RELEASE;
    }

    /**
     * 识别可参与换活字块兜底反查的机台。
     *
     * @param context 排程上下文
     * @return 兜底机台列表
     */
    private List<MachineScheduleDTO> resolveTypeBlockFallbackMachines(LhScheduleContext context) {
        List<MachineScheduleDTO> fallbackMachineList = new ArrayList<>();
        if (context == null
                || CollectionUtils.isEmpty(context.getMachineScheduleMap())
                || CollectionUtils.isEmpty(context.getMachineOnlineInfoMap())) {
            return fallbackMachineList;
        }
        for (MachineScheduleDTO machine : context.getMachineScheduleMap().values()) {
            if (machine == null || machine.isEnding() || machine.getEstimatedEndTime() == null) {
                continue;
            }
            String machineCode = machine.getMachineCode();
            if (StringUtils.isEmpty(machineCode)
                    || context.isContinuousStopHoldMachine(machineCode)
                    || !context.getMachineOnlineInfoMap().containsKey(machineCode)
                    || StringUtils.isEmpty(machine.getCurrentMaterialCode())) {
                continue;
            }
            if (isMachineAssignedContinuousResult(context, machineCode)) {
                continue;
            }
            if (!isTypeBlockFallbackEligibleByPreviousDay(context, machine)) {
                continue;
            }
            fallbackMachineList.add(machine);
        }
        return fallbackMachineList;
    }

    /**
     * 判定机台是否已命中续作分配。
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @return true-已命中续作分配
     */
    private boolean isMachineAssignedContinuousResult(LhScheduleContext context, String machineCode) {
        if (context == null
                || StringUtils.isEmpty(machineCode)
                || CollectionUtils.isEmpty(context.getMachineAssignmentMap())) {
            return false;
        }
        List<LhScheduleResult> assignedResults = context.getMachineAssignmentMap().get(machineCode);
        if (CollectionUtils.isEmpty(assignedResults)) {
            return false;
        }
        for (LhScheduleResult assignedResult : assignedResults) {
            if (assignedResult != null
                    && StringUtils.equals(CONTINUOUS_SCHEDULE_TYPE, assignedResult.getScheduleType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判定兜底机台是否通过 T-1 收尾校验。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @return true-通过校验
     */
    private boolean isTypeBlockFallbackEligibleByPreviousDay(LhScheduleContext context, MachineScheduleDTO machine) {
        if (context == null || machine == null) {
            return false;
        }
        String machineCode = machine.getMachineCode();
        String currentMaterialCode = machine.getCurrentMaterialCode();
        if (StringUtils.isEmpty(machineCode) || StringUtils.isEmpty(currentMaterialCode)) {
            return false;
        }
        LhScheduleResult latestPreviousResult = resolveLatestPreviousResult(context, machineCode, currentMaterialCode);
        if (latestPreviousResult == null || StringUtils.equals(YES_FLAG, latestPreviousResult.getIsEnd())) {
            return true;
        }
        traceTypeBlockFallbackSkip(context, machine, latestPreviousResult, TYPE_BLOCK_SKIP_REASON_T1_NOT_END);
        return false;
    }

    /**
     * 解析 T-1 同机台同SKU的最新一条排程结果。
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @param materialCode 物料编码
     * @return 最新结果
     */
    private LhScheduleResult resolveLatestPreviousResult(LhScheduleContext context,
                                                         String machineCode,
                                                         String materialCode) {
        if (context == null
                || StringUtils.isEmpty(machineCode)
                || StringUtils.isEmpty(materialCode)
                || CollectionUtils.isEmpty(context.getPreviousScheduleResultList())) {
            return null;
        }
        LhScheduleResult latestResult = null;
        Date latestTime = null;
        for (LhScheduleResult previousResult : context.getPreviousScheduleResultList()) {
            if (previousResult == null
                    || !StringUtils.equals(machineCode, previousResult.getLhMachineCode())
                    || !StringUtils.equals(materialCode, previousResult.getMaterialCode())) {
                continue;
            }
            Date currentTime = resolvePreviousResultOrderTime(previousResult);
            if (latestResult == null) {
                latestResult = previousResult;
                latestTime = currentTime;
                continue;
            }
            if (latestTime == null || (currentTime != null && currentTime.after(latestTime))) {
                latestResult = previousResult;
                latestTime = currentTime;
            }
        }
        return latestResult;
    }

    /**
     * 解析 T-1 记录排序时间。
     *
     * @param previousResult T-1排程结果
     * @return 排序时间
     */
    private Date resolvePreviousResultOrderTime(LhScheduleResult previousResult) {
        if (previousResult == null) {
            return null;
        }
        if (previousResult.getSpecEndTime() != null) {
            return previousResult.getSpecEndTime();
        }
        return previousResult.getCreateTime();
    }

    /**
     * 输出兜底机台被跳过的决策日志。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param latestPreviousResult T-1最新结果
     * @param reason 跳过原因
     */
    private void traceTypeBlockFallbackSkip(LhScheduleContext context,
                                            MachineScheduleDTO machine,
                                            LhScheduleResult latestPreviousResult,
                                            String reason) {
        if (machine == null) {
            return;
        }
        log.info("换活字块兜底机台跳过, 机台: {}, 当前物料: {}, 原因: {}",
                machine.getMachineCode(), machine.getCurrentMaterialCode(), reason);
        if (!PriorityTraceLogHelper.isEnabled(context)) {
            return;
        }
        String title = "收尾机台衔接决策";
        StringBuilder detailBuilder = new StringBuilder(384);
        PriorityTraceLogHelper.appendLine(detailBuilder,
                "机台=" + PriorityTraceLogHelper.safeText(machine.getMachineCode())
                        + ", 当前物料=" + PriorityTraceLogHelper.safeText(machine.getCurrentMaterialCode())
                        + ", 触发来源=" + TYPE_BLOCK_TRIGGER_FALLBACK);
        PriorityTraceLogHelper.appendLine(detailBuilder,
                "衔接结果=未衔接, 原因=" + PriorityTraceLogHelper.safeText(reason)
                        + ", T-1最新isEnd=" + PriorityTraceLogHelper.safeText(
                        latestPreviousResult == null ? null : latestPreviousResult.getIsEnd())
                        + ", T-1最新排序时间=" + PriorityTraceLogHelper.formatDateTime(
                        resolvePreviousResultOrderTime(latestPreviousResult)));
        String detail = detailBuilder.toString().trim();
        PriorityTraceLogHelper.appendProcessLog(context, title, detail);
    }

    /**
     * 构建排程结果，分配各班次计划量。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param sku SKU
     * @param startTime 开产时间
     * @param switchStartTime 切换开始时间
     * @param shifts 班次
     * @param mouldQty 模台数
     * @param isEnding 是否收尾
     * @param earliestEmbryoAvailableTime 结构切换提前命中的最早胎胚可供时间；其他换活字块为 null
     * @param firstInspectionAllocationPlanOutput 返回本次实际写入结果的首检分摊计划，容量固定为1；
     *                                            供调用方在结果最终提交后写过程日志
     * @return 排程结果
     */
    private LhScheduleResult buildScheduleResult(LhScheduleContext context,
                                                  MachineScheduleDTO machine,
                                                  SkuScheduleDTO sku,
                                                  Date startTime,
                                                  Date switchStartTime,
                                                  List<LhShiftConfigVO> shifts,
                                                  int mouldQty,
                                                  boolean isEnding,
                                                  Date earliestEmbryoAvailableTime,
                                                  List<FirstInspectionAllocationPlan>
                                                          firstInspectionAllocationPlanOutput) {
        LhScheduleResult result = new LhScheduleResult();
        result.setFactoryCode(context.getFactoryCode());
        result.setBatchNo(context.getBatchNo());
        result.setLhMachineCode(machine.getMachineCode());
        result.setLhMachineName(machine.getMachineName());
        result.setLeftRightMould(LeftRightMouldUtil.resolveLeftRightMould(
                result.getLeftRightMould(), machine.getMachineCode()));
        result.setMaterialCode(sku.getMaterialCode());
        result.setMaterialDesc(sku.getMaterialDesc());
        result.setSpecCode(sku.getSpecCode());
        result.setSpecDesc(sku.getSpecDesc());
        result.setEmbryoCode(sku.getEmbryoCode());
        // 落库口径：库存未知(-1)按0落库，但排程过程仍保留-1语义用于跳过库存裁剪。
        result.setEmbryoStock(Math.max(sku.getEmbryoStock(), 0));
        result.setMainMaterialDesc(sku.getMainMaterialDesc());
        result.setStructureName(sku.getStructureName());
        result.setScheduleDate(context.getScheduleTargetDate());
        result.setLhTime(sku.getLhTimeSeconds());
        result.setMouldQty(mouldQty);
        int runtimeShiftCapacity = ShiftCapacityResolverUtil.resolveRuntimeShiftCapacity(
                context, machine, sku.getShiftCapacity());
        result.setSingleMouldShiftQty(SingleMouldShiftQtyUtil.resolveSingleMouldShiftQty(
                context, sku, machine, mouldQty));
        result.setDailyPlanQty(0);
        result.setTotalDailyPlanQty(sku.getMonthPlanQty());
        result.setMouldSurplusQty(sku.getSurplusQty());
        result.setTotalFinishQty(sku.getFinishedQty());
        // 日标准产量：复用上下文 SKU 日硫化产能主数据，无主数据则为 0
        result.setStandardCapacity(ShiftCapacityResolverUtil.resolveDailyStandardQty(
                context, sku.getMaterialCode()));
        // 普通换活字块默认不是提前生产；命中共享运行视图时由结果审计方法统一改为1并追加备注。
        result.setIsEarlyProduction(NO_FLAG);
        result.setIsEnd(isEnding ? YES_FLAG : NO_FLAG);
        result.setIsDelivery(sku.isDeliveryLocked() ? YES_FLAG : NO_FLAG);
        result.setIsRelease(NO_FLAG);
        result.setDataSource(AUTO_DATA_SOURCE);
        result.setIsDelete(0);
        result.setScheduleType(ScheduleTypeEnum.TYPE_BLOCK.getCode());
        result.setIsTypeBlock(YES_FLAG);
        result.setConstructionStage(sku.getConstructionStage());
        // 产品状态从月计划获取
        result.setProductStatus(sku.getProductStatus());

        // 通过物料编码+产品状态查询SKU与示方书关系获取文字/硫化/制造示方书号
        String embryoNo = null;
        String textNo = null;
        String lhNo = null;
        String lhType = null;
        MdmSkuConstructionRef constructionRef = context.findSkuConstructionRef(
                sku.getMaterialCode(), sku.getProductStatus());
        if (constructionRef != null) {
            embryoNo = constructionRef.getEmbryoNo();
            textNo = constructionRef.getTextNo();
            lhNo = constructionRef.getLhNo();
            lhType = constructionRef.getLhType();
        }
        // 设置1-8班硫化示方书号和硫化示方书类型
        result.setClass1LhNo(lhNo);
        result.setClass1LhType(lhType);
        result.setClass2LhNo(lhNo);
        result.setClass2LhType(lhType);
        result.setClass3LhNo(lhNo);
        result.setClass3LhType(lhType);
        result.setClass4LhNo(lhNo);
        result.setClass4LhType(lhType);
        result.setClass5LhNo(lhNo);
        result.setClass5LhType(lhType);
        result.setClass6LhNo(lhNo);
        result.setClass6LhType(lhType);
        result.setClass7LhNo(lhNo);
        result.setClass7LhType(lhType);
        result.setClass8LhNo(lhNo);
        result.setClass8LhType(lhType);
        // 文字/硫化/制造示方书号回写：关系查不到时置空，以关系值为准
        result.setLhNo(lhNo);
        result.setChangedTrialStatus(lhType);
        result.setEmbryoNo(embryoNo);
        result.setTextNo(textNo);
        result.setMonthPlanVersion(sku.getMonthPlanVersion());
        result.setProductionVersion(sku.getProductionVersion());
        result.setIsTrial(sku.isTrial() ? YES_FLAG : NO_FLAG);
        result.setMachineOrder(machine.getMachineOrder());
        result.setHasSpecialMaterial(LhSpecialMaterialUtil.resolveHasSpecialMaterial(context, sku));

        // 生成工单号。
        result.setOrderNo(generateOrderNo(context));

        int refinedTargetQty = getTargetScheduleQtyResolver().refineTargetQtyByMachineCapacity(
                context, sku, machine, switchStartTime, startTime, shifts,
                ScheduleTypeEnum.TYPE_BLOCK.getCode());
        Date switchCompleteTime = resolveTypeBlockSwitchCompleteTime(
                context, machine, switchStartTime, startTime);
        /*
         * 清洗只有在与真实换活字块区间重叠时才可按现有并行规则剔除。胎胚、维修或
         * 其他生产门禁可能把正式开产继续后移，不能据此扩大“清洗与切换重叠”区间。
         */
        List<MachineCleaningWindowDTO> cleaningWindowList = new ArrayList<>(MachineCleaningOverlapUtil.excludeOverlapWindows(
                machine.getCleaningWindowList(), switchStartTime, switchCompleteTime));
        List<MachineMaintenanceWindowDTO> maintenanceWindowList = resolveMachineMaintenanceWindowList(
                context, machine.getMachineCode());
        /*
         * 首检属于换活字块准备阶段，结束时刻固定等于真实换活字块完成时刻；胎胚最早
         * 可供时间只约束正式生产，不得把已经完成的首检一起推迟到正式生产班次。
         */
        FirstInspectionAllocationPlan firstInspectionAllocationPlan =
                FirstInspectionAllocationUtil.buildPlan(
                        context, sku, shifts, switchCompleteTime,
                        runtimeShiftCapacity, refinedTargetQty,
                        ScheduleTypeEnum.TYPE_BLOCK.getCode(),
                        machine.getMachineCode(), null);
        if (firstInspectionAllocationPlan.isValid()
                && firstInspectionAllocationPlan.getInspectionQty() > 0) {
            Map<Integer, Integer> firstInspectionCapacityMap =
                    this.calculateTypeBlockFirstInspectionAvailableCapacityMap(
                            context, result, sku, firstInspectionAllocationPlan,
                            runtimeShiftCapacity, mouldQty, cleaningWindowList,
                            maintenanceWindowList);
            firstInspectionAllocationPlan = FirstInspectionAllocationUtil.buildPlan(
                    context, sku, shifts, switchCompleteTime,
                    runtimeShiftCapacity, refinedTargetQty,
                    ScheduleTypeEnum.TYPE_BLOCK.getCode(),
                    machine.getMachineCode(), firstInspectionCapacityMap);
        }
        if (!firstInspectionAllocationPlan.isValid()) {
            log.info("换活字块首检时间分摊失败，取消当前结果, batchNo: {}, materialCode: {}, "
                            + "machineCode: {}, switchCompleteTime: {}, reason: {}",
                    context.getBatchNo(), sku.getMaterialCode(), machine.getMachineCode(),
                    LhScheduleTimeUtil.formatDateTime(switchCompleteTime),
                    firstInspectionAllocationPlan.getInvalidReason());
            return null;
        }
        LhShiftConfigVO firstInspectionAttributionShift =
                firstInspectionAllocationPlan.getCountingShift();
        // 正式落班直接复用本次首检分摊计划；试制仍保留“中班固定2小时、首检数量为0”的现有例外。
        int remainingAfterDistribution = distributeToShifts(context, sku, result, shifts, startTime,
                runtimeShiftCapacity, sku.getLhTimeSeconds(), mouldQty, refinedTargetQty, cleaningWindowList,
                maintenanceWindowList, switchCompleteTime, firstInspectionAttributionShift,
                firstInspectionAllocationPlan,
                Objects.nonNull(earliestEmbryoAvailableTime));
        if (firstInspectionAllocationPlan.getInspectionQty() > 0
                && remainingAfterDistribution <= Math.max(
                0, refinedTargetQty - firstInspectionAllocationPlan.getInspectionQty())
                && Objects.nonNull(firstInspectionAllocationPlanOutput)) {
            /*
             * distributeToShifts 只有在首检总量守恒写入后才会先扣减首检目标量；
             * 因此用未分配量判定本次计划确已实际采用，计数顺序变化导致首检写入
             * 失败时不得向上层传出计划。过程日志仍必须等待调用方最终提交。
             */
            firstInspectionAllocationPlanOutput.add(firstInspectionAllocationPlan);
        }
        // 胎胚最早可供时间只校正正式生产起点；首检班次保留其真实准备时间区间。

        refreshResultSummary(context, result, shifts);
        result.setRealScheduleDate(context.getScheduleDate());
        result.setProductionStatus(NO_FLAG);
        this.appendTypeBlockEarlyProductionAudit(
                context, sku, result,
                resolveTypeBlockProductionWorkDate(shifts, startTime));

        return result;
    }

    /**
     * 将换活字块提前生产判定写入结果标识和备注。
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param result 换活字块结果
     * @param productionWorkDate 实际开产业务日
     */
    private void appendTypeBlockEarlyProductionAudit(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LhScheduleResult result,
            LocalDate productionWorkDate) {
        if (Objects.isNull(context) || Objects.isNull(sku) || Objects.isNull(result)) {
            return;
        }
        EarlyProductionRuntimePlan runtimePlan =
                context.getEarlyProductionRuntimePlan(sku);
        EarlyProductionDecision decision = Objects.isNull(runtimePlan)
                ? null : runtimePlan.getDecision();
        if (Objects.isNull(runtimePlan) || !runtimePlan.isActive()
                || Objects.isNull(decision) || !decision.isEarlyProduction()
                || !decision.isAllowed()
                || !Objects.equals(productionWorkDate, runtimePlan.getCurrentDate())
                || this.resolveTypeBlockOriginalDayPlanQty(
                context, sku, productionWorkDate) > 0) {
            return;
        }
        result.setIsEarlyProduction(YES_FLAG);
        String remarkFragment = decision.buildRemark();
        if (StringUtils.isNotEmpty(remarkFragment)
                && !StringUtils.contains(result.getRemark(), remarkFragment)) {
            String currentRemark = result.getRemark();
            result.setRemark(StringUtils.isEmpty(currentRemark)
                    ? remarkFragment
                    : new StringBuilder(currentRemark.length() + remarkFragment.length() + 1)
                    .append(currentRemark).append('；').append(remarkFragment).toString());
        }
        log.info("换活字块提前生产结果审计写入, factoryCode: {}, batchNo: {}, "
                        + "materialCode: {}, machineCode: {}, productionWorkDate: {}, "
                        + "futurePlanDate: {}, isEarlyProduction: {}, remark: {}",
                context.getFactoryCode(), context.getBatchNo(), result.getMaterialCode(),
                result.getLhMachineCode(), productionWorkDate, runtimePlan.getFuturePlanDate(),
                result.getIsEarlyProduction(), result.getRemark());
    }

    /**
     * 向各班次分配计划量。
     *
     * @param context 排程上下文
     * @param sku 当前换活字块SKU，用于统一识别试制首检产能规则
     * @param result 排程结果
     * @param shifts 班次
     * @param startTime 开产时间
     * @param shiftCapacity 单模班产能
     * @param lhTimeSeconds 硫化时间
     * @param mouldQty 模台数
     * @param remaining 剩余目标量
     * @param cleaningWindowList 清洗窗口
     * @param maintenanceWindowList 保养窗口
     * @param switchCompleteTime 换活字块完成时间，用于判定首检归属班次
     * @param firstInspectionAttributionShift 首检归属班次
     * @param firstInspectionAllocationPlan 换活字块首检真实时间分摊计划
     * @param embryoAvailableTimeConstrained 是否启用提前生产胎胚时间部分班次首检口径
     * @return 未排剩余量
     */
    private int distributeToShifts(LhScheduleContext context,
                                   SkuScheduleDTO sku,
                                   LhScheduleResult result,
                                   List<LhShiftConfigVO> shifts,
                                   Date startTime,
                                   int shiftCapacity,
                                   int lhTimeSeconds,
                                   int mouldQty,
                                   int remaining,
                                   List<MachineCleaningWindowDTO> cleaningWindowList,
                                   List<MachineMaintenanceWindowDTO> maintenanceWindowList,
                                   Date switchCompleteTime,
                                   LhShiftConfigVO firstInspectionAttributionShift,
                                   FirstInspectionAllocationPlan firstInspectionAllocationPlan,
                                   boolean embryoAvailableTimeConstrained) {
        if (lhTimeSeconds <= 0 || mouldQty <= 0 || remaining <= 0) {
            return remaining;
        }
        Map<Integer, ShiftRuntimeState> stateMap = context.getShiftRuntimeStateMap();
        int dryIceLossQty = context.getParamIntValue(
                LhScheduleParamConstant.DRY_ICE_LOSS_QTY, LhScheduleConstant.DRY_ICE_LOSS_QTY);
        int dryIceDurationHours = context.getParamIntValue(
                LhScheduleParamConstant.DRY_ICE_DURATION_HOURS, LhScheduleConstant.DRY_ICE_DURATION_HOURS);
        int plannedRepairFixedQty = context.getParamIntValue(
                LhScheduleParamConstant.PLANNED_REPAIR_FIXED_QTY, LhScheduleConstant.PLANNED_REPAIR_FIXED_QTY);
        String configPlusShiftType = ShiftCapacityResolverUtil.resolveOddShiftCapacityPlusShiftType(context);
        // 换活字块班次分配复用统一结构准入，保证与续作、新增排产的理论上限和补差口径一致。
        Map<Integer, Integer> dailyStandardShiftCapacityMap = calculateDailyStandardShiftCapacityMap(
                context, result, shifts, startTime, shiftCapacity, lhTimeSeconds, mouldQty,
                cleaningWindowList, maintenanceWindowList);
        LhShiftConfigVO firstInspectionShift = firstInspectionAttributionShift;
        int firstInspectionShiftIndex = Objects.isNull(firstInspectionShift)
                || Objects.isNull(firstInspectionShift.getShiftIndex()) ? -1 : firstInspectionShift.getShiftIndex();
        Map<Integer, Integer> firstInspectionQtyMap =
                FirstInspectionAllocationUtil.toShiftQtyMap(firstInspectionAllocationPlan);
        int firstInspectionQty = Objects.isNull(firstInspectionAllocationPlan)
                ? 0 : firstInspectionAllocationPlan.getInspectionQty();
        boolean crossShiftInspection = Objects.nonNull(firstInspectionAllocationPlan)
                && firstInspectionAllocationPlan.isValid() && firstInspectionQty > 0;
        if (crossShiftInspection) {
            /*
             * 首检先于正式计划量一次性写入真实覆盖班次并扣减目标量。写入方法同时保证
             * 总量守恒、一次事件只推进一次计数；后续循环只分配正式生产量，禁止重复计入。
             */
            int writtenInspectionQty = FirstInspectionQtyUtil
                    .addFirstInspectionAllocationToResult(
                            context, result, firstInspectionAllocationPlan,
                            ScheduleTypeEnum.TYPE_BLOCK.getCode());
            if (writtenInspectionQty != firstInspectionQty) {
                return remaining;
            }
            remaining = Math.max(0, remaining - writtenInspectionQty);
        }

        boolean started = false;
        for (LhShiftConfigVO shift : shifts) {
            if (remaining <= 0) {
                break;
            }
            if (!started) {
                if (startTime != null && !startTime.before(shift.getShiftEndDateTime())
                        && shift != shifts.get(shifts.size() - 1)) {
                    continue;
                }
                started = true;
            }

            ShiftProductionControlDTO control = ShiftProductionControlUtil.resolveEffectiveControl(context, shift, startTime);
            if (control == null || !control.isCanSchedule()) {
                logTypeBlockShiftSkip(result, shift, remaining, shiftCapacity, 0,
                        0, "班次管控不可排");
                continue;
            }
            Date effectiveStart = control.getEffectiveStartTime();
            Date effectiveEnd = control.getEffectiveEndTime();
            int actualShiftPlanQty = ShiftCapacityResolverUtil.resolveActualShiftPlanQty(
                    shiftCapacity, shift, configPlusShiftType, ScheduleTypeEnum.TYPE_BLOCK.getCode());
            boolean oddShiftAdjustEnabled = ShiftCapacityResolverUtil.isOddShiftCapacityAdjustEnabled(
                    shiftCapacity, shift, configPlusShiftType, ScheduleTypeEnum.TYPE_BLOCK.getCode());
            log.debug("奇数班产修正检查, 当前流程: 换活字块排产, materialCode: {}, machineCode: {}, 参数是否配置: {}, "
                            + "参数值: {}, 配置值是否合法: {}, 是否启用: {}, 未启用原因: {}, 原始班产: {}, "
                            + "班次序号: {}, 当前班别: {}, 当前班次修正后的计划量: {}, 班产落库字段值: {}",
                    result.getMaterialCode(), result.getLhMachineCode(), StringUtils.isNotEmpty(configPlusShiftType),
                    configPlusShiftType,
                    ShiftCapacityResolverUtil.isOddShiftCapacityPlusShiftTypeValid(configPlusShiftType),
                    oddShiftAdjustEnabled,
                    ShiftCapacityResolverUtil.resolveOddShiftCapacityDisabledReason(
                            shiftCapacity, shift, configPlusShiftType, ScheduleTypeEnum.TYPE_BLOCK.getCode()),
                    shiftCapacity, shift.getShiftIndex(), shift.resolveShiftTypeEnum(), actualShiftPlanQty,
                    shiftCapacity);

            int shiftMaxQty = ShiftCapacityResolverUtil.resolveShiftCapacityWithDowntime(
                    context.getDevicePlanShutList(),
                    cleaningWindowList,
                    maintenanceWindowList,
                    result.getLhMachineCode(),
                    effectiveStart,
                    effectiveEnd,
                    shiftCapacity,
                    lhTimeSeconds,
                    mouldQty,
                    ShiftCapacityResolverUtil.resolveShiftDurationSeconds(shift),
                    dryIceLossQty,
                    dryIceDurationHours,
                    shift,
                    configPlusShiftType,
                    ScheduleTypeEnum.TYPE_BLOCK.getCode(),
                    plannedRepairFixedQty);
            shiftMaxQty = ShiftProductionControlUtil.deductCapacityByControl(control, shiftMaxQty, mouldQty);
            int physicalShiftMaxQty = shiftMaxQty;
            shiftMaxQty = dailyStandardShiftCapacityMap.getOrDefault(shift.getShiftIndex(), shiftMaxQty);
            int capacityAfterSwitch;
            if (crossShiftInspection) {
                int currentShiftInspectionQty = Math.max(
                        0, firstInspectionQtyMap.getOrDefault(shift.getShiftIndex(), 0));
                int shiftCapacityCap = ShiftCapacityResolverUtil.resolveActualShiftPlanQty(
                        shiftCapacity, shift, configPlusShiftType,
                        ScheduleTypeEnum.TYPE_BLOCK.getCode());
                /*
                 * 首检和正式生产使用互不重叠的真实时间区间，因此正式生产容量不直接再减
                 * 首检条数；但两者班次合计仍不得突破班产上限。
                 */
                capacityAfterSwitch = Math.min(
                        Math.max(0, shiftMaxQty),
                        Math.max(0, shiftCapacityCap - currentShiftInspectionQty));
                shiftMaxQty = capacityAfterSwitch;
            } else {
                // 试制继续沿用中班固定2小时首检产能规则，不生成首检条数。
                capacityAfterSwitch = FirstInspectionQtyUtil
                        .resolveNormalCapacityAfterFirstInspection(
                                context, sku, shift, shiftMaxQty,
                                firstInspectionShiftIndex, firstInspectionQty,
                                shiftCapacity, ScheduleTypeEnum.TYPE_BLOCK.getCode(),
                                result.getLhMachineCode(), embryoAvailableTimeConstrained);
                shiftMaxQty = capacityAfterSwitch;
            }
            if (shiftMaxQty <= 0) {
                String skipReason = physicalShiftMaxQty <= 0
                        ? "停机/清洗/保养/班次管控扣减后无可用产能"
                        : "日标准产量修正后无可用产能";
                logTypeBlockShiftSkip(result, shift, remaining, shiftCapacity,
                        physicalShiftMaxQty, shiftMaxQty, skipReason);
                continue;
            }
            if (oddShiftAdjustEnabled) {
                log.info("奇数班产修正命中, 当前流程: 换活字块排产, materialCode: {}, machineCode: {}, 参数值: {}, "
                                + "原始班产: {}, 班次序号: {}, 当前班别: {}, 修正后班次计划量: {}, 班产落库字段值: {}",
                        result.getMaterialCode(), result.getLhMachineCode(), configPlusShiftType, shiftCapacity,
                        shift.getShiftIndex(), shift.resolveShiftTypeEnum(), actualShiftPlanQty, shiftCapacity);
            }
            int shiftQty = getTargetScheduleQtyResolver().resolveAllocatedShiftQty(
                    context, result, Math.min(remaining, shiftMaxQty), shiftMaxQty, mouldQty);
            // 目标量、首检和物理产能全部收口后，再按本班实际候选量执行一次换胶囊扣减。
            shiftQty = capsuleReplacementRuleService.resolveActualPlanQty(
                    context, result, shift, shiftQty, shiftMaxQty, effectiveStart, "换活字块排产");
            // 未满产换胶囊可能刚登记时间窗口，后续班次必须立即读取最新窗口重新计算产能。
            maintenanceWindowList = resolveMachineMaintenanceWindowList(context, result.getLhMachineCode());
            if (shiftQty <= 0) {
                logTypeBlockShiftSkip(result, shift, remaining, shiftCapacity,
                        physicalShiftMaxQty, shiftMaxQty, "目标量/硫化余量或换胶囊扣减后为0");
                continue;
            }
            Date shiftPlanEndTime = ShiftCapacityResolverUtil.resolveShiftPlanEndTime(
                    context.getDevicePlanShutList(),
                    cleaningWindowList,
                    maintenanceWindowList,
                    result.getLhMachineCode(),
                    effectiveStart,
                    effectiveEnd,
                    shiftQty,
                    shiftMaxQty);
            if (crossShiftInspection) {
                Integer existingPlanQty = ShiftFieldUtil.getShiftPlanQty(
                        result, shift.getShiftIndex());
                Date existingStartTime = ShiftFieldUtil.getShiftStartTime(
                        result, shift.getShiftIndex());
                Date existingEndTime = ShiftFieldUtil.getShiftEndTime(
                        result, shift.getShiftIndex());
                int mergedPlanQty = Math.max(
                        0, Objects.isNull(existingPlanQty) ? 0 : existingPlanQty) + shiftQty;
                Date mergedStartTime = Objects.isNull(existingStartTime)
                        || effectiveStart.before(existingStartTime)
                        ? effectiveStart : existingStartTime;
                Date mergedEndTime = Objects.isNull(existingEndTime)
                        || shiftPlanEndTime.after(existingEndTime)
                        ? shiftPlanEndTime : existingEndTime;
                setShiftPlanQty(
                        result, shift.getShiftIndex(), mergedPlanQty,
                        mergedStartTime, mergedEndTime);
            } else {
                setShiftPlanQty(
                        result, shift.getShiftIndex(), shiftQty,
                        effectiveStart, shiftPlanEndTime);
            }
            remaining -= shiftQty;
            startTime = null;

            if (!CollectionUtils.isEmpty(stateMap)) {
                ShiftRuntimeState st = stateMap.get(shift.getShiftIndex());
                if (st != null) {
                    st.setRemainingCapacity(Math.max(0, shiftMaxQty - shiftQty));
                }
            }
        }
        return remaining;
    }

    /**
     * 计算换活字块首检真实时间区间内的班次可用产能。
     *
     * <p>先由公共分摊算法确定首检时间区间，再在每段真实重叠时间上复用换活字块现有
     * 停机、清洗、精度/维修、班次管控和日标准产量规则。该结果重新传回公共分摊算法
     * 做容量硬校验，任一班次不足时整次换活字块结果失败，不把尾量挪给其他班次。</p>
     *
     * @param context 排程上下文
     * @param result 当前换活字块结果
     * @param sku 当前 SKU
     * @param plan 初次形成的首检时间计划
     * @param shiftCapacity 运行态班产
     * @param mouldQty 模数
     * @param cleaningWindowList 有效清洗窗口
     * @param maintenanceWindowList 精度及维修容量窗口
     * @return 班次索引到首检区间实际可用产能的映射
     */
    private Map<Integer, Integer> calculateTypeBlockFirstInspectionAvailableCapacityMap(
            LhScheduleContext context,
            LhScheduleResult result,
            SkuScheduleDTO sku,
            FirstInspectionAllocationPlan plan,
            int shiftCapacity,
            int mouldQty,
            List<MachineCleaningWindowDTO> cleaningWindowList,
            List<MachineMaintenanceWindowDTO> maintenanceWindowList) {
        Map<Integer, Integer> capacityMap = new LinkedHashMap<Integer, Integer>(4);
        if (Objects.isNull(context) || Objects.isNull(result) || Objects.isNull(sku)
                || Objects.isNull(plan) || !plan.isValid()
                || CollectionUtils.isEmpty(plan.getShiftAllocations())
                || shiftCapacity <= 0 || mouldQty <= 0) {
            return capacityMap;
        }
        int dryIceLossQty = context.getParamIntValue(
                LhScheduleParamConstant.DRY_ICE_LOSS_QTY,
                LhScheduleConstant.DRY_ICE_LOSS_QTY);
        int dryIceDurationHours = context.getParamIntValue(
                LhScheduleParamConstant.DRY_ICE_DURATION_HOURS,
                LhScheduleConstant.DRY_ICE_DURATION_HOURS);
        int plannedRepairFixedQty = context.getParamIntValue(
                LhScheduleParamConstant.PLANNED_REPAIR_FIXED_QTY,
                LhScheduleConstant.PLANNED_REPAIR_FIXED_QTY);
        String configPlusShiftType =
                ShiftCapacityResolverUtil.resolveOddShiftCapacityPlusShiftType(context);
        for (FirstInspectionShiftAllocation allocation : plan.getShiftAllocations()) {
            LhShiftConfigVO shift = allocation.getShift();
            ShiftProductionControlDTO control = ShiftProductionControlUtil
                    .resolveEffectiveControl(
                            context, shift, allocation.getOverlapStartTime());
            if (Objects.isNull(control) || !control.isCanSchedule()) {
                capacityMap.put(shift.getShiftIndex(), 0);
                continue;
            }
            Date effectiveStartTime = control.getEffectiveStartTime();
            if (Objects.isNull(effectiveStartTime)
                    || effectiveStartTime.before(allocation.getOverlapStartTime())) {
                effectiveStartTime = allocation.getOverlapStartTime();
            }
            Date effectiveEndTime = control.getEffectiveEndTime();
            if (Objects.isNull(effectiveEndTime)
                    || effectiveEndTime.after(allocation.getOverlapEndTime())) {
                effectiveEndTime = allocation.getOverlapEndTime();
            }
            if (!effectiveStartTime.before(effectiveEndTime)) {
                capacityMap.put(shift.getShiftIndex(), 0);
                continue;
            }
            long effectiveSeconds = Math.max(
                    0L, (effectiveEndTime.getTime() - effectiveStartTime.getTime()) / 1000L);
            int availableQty = ShiftCapacityResolverUtil.resolveShiftCapacityWithDowntime(
                    context.getDevicePlanShutList(), cleaningWindowList,
                    maintenanceWindowList, result.getLhMachineCode(),
                    effectiveStartTime, effectiveEndTime, shiftCapacity,
                    sku.getLhTimeSeconds(), mouldQty, effectiveSeconds,
                    dryIceLossQty, dryIceDurationHours, shift,
                    configPlusShiftType, ScheduleTypeEnum.TYPE_BLOCK.getCode(),
                    plannedRepairFixedQty);
            availableQty = ShiftProductionControlUtil.deductCapacityByControl(
                    control, availableQty, mouldQty);
            capacityMap.put(shift.getShiftIndex(), Math.max(0, availableQty));
        }
        if (!ShiftCapacityResolverUtil.isDailyStandardCapacityStructureMatched(
                context, result.getStructureName())) {
            return capacityMap;
        }
        String remainShiftType =
                ShiftCapacityResolverUtil.resolveDailyStandardCapacityRemainShiftType(context);
        int dailyStandardQty = ShiftCapacityResolverUtil.resolveDailyStandardQty(
                context, result.getMaterialCode());
        int remainShiftCapacityUpperLimit = ShiftCapacityResolverUtil
                .resolveDailyStandardRemainShiftCapacityUpperLimit(
                        context, result.getMaterialCode(), shiftCapacity);
        boolean singleControlMachine = LhSingleControlMachineUtil
                .isConfiguredSingleControlMachine(
                        context, result.getLhMachineCode());
        return ShiftCapacityResolverUtil.adjustShiftPlanQtyMapByDailyStandard(
                context.getScheduleWindowShifts(), capacityMap, capacityMap,
                dailyStandardQty, shiftCapacity, remainShiftCapacityUpperLimit,
                remainShiftType, singleControlMachine,
                ScheduleTypeEnum.TYPE_BLOCK.getCode());
    }

    /**
     * 记录换活字块班次跳过原因，便于核对已完成切换 SKU 中间空班是否存在硬约束。
     *
     * @param result 换活字块排程结果
     * @param shift 当前班次
     * @param remaining 当前剩余目标量
     * @param shiftCapacity 原始班产
     * @param physicalShiftMaxQty 停机/清洗/保养/班次管控扣减后的物理可用产能
     * @param finalShiftMaxQty 日标准修正后的最终可排产能
     * @param skipReason 跳过原因
     */
    private void logTypeBlockShiftSkip(LhScheduleResult result,
                                       LhShiftConfigVO shift,
                                       int remaining,
                                       int shiftCapacity,
                                       int physicalShiftMaxQty,
                                       int finalShiftMaxQty,
                                       String skipReason) {
        if (Objects.isNull(result) || Objects.isNull(shift)) {
            return;
        }
        log.info("连续排产班次跳过诊断, 当前流程: 换活字块排产, materialCode: {}, machineCode: {}, 班次: {}, "
                        + "剩余余量: {}, 原始班产: {}, 班次物理可用产能: {}, 最终班次可用产能: {}, "
                        + "是否跳过: {}, 跳过原因: {}",
                result.getMaterialCode(), result.getLhMachineCode(), shift.getShiftIndex(), remaining,
                shiftCapacity, physicalShiftMaxQty, finalShiftMaxQty, true, skipReason);
    }

    /**
     * 解析换活字块完成时间。
     * <p>清洗与换活字块允许并行处理，完成时间取换活字块和清洗的最晚结束时间；
     * 未重叠时仍保持原换活字块完成时间，不影响正常换活字块链路。</p>
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param switchStartTime 换活字块开始时间
     * @param fallbackStartTime 开产时间兜底值
     * @return 换活字块完成时间
     */
    private Date resolveTypeBlockSwitchCompleteTime(LhScheduleContext context,
                                                    MachineScheduleDTO machine,
                                                    Date switchStartTime,
                                                    Date fallbackStartTime) {
        if (Objects.isNull(switchStartTime)) {
            return fallbackStartTime;
        }
        int switchDurationHours = resolveTypeBlockSwitchDurationHours(
                context, machine, Objects.isNull(machine) ? null : machine.getEstimatedEndTime(), switchStartTime);
        Date switchCompleteTime = LhScheduleTimeUtil.addHours(switchStartTime, switchDurationHours);
        // 调用清洗重叠解析，确保喷砂清洗+换活字块按 10 小时、干冰清洗+换活字块按 8 小时口径落首检和备注。
        return resolveCleaningOverlapProductionStartTime(machine, switchStartTime, switchCompleteTime);
    }

    /**
     * 按SKU日标准产量修正换活字块班次最大计划量。
     *
     * @param context 排程上下文
     * @param result 换活字块结果
     * @param shifts 班次列表
     * @param startTime 首个可排开始时间
     * @param shiftCapacity 运行态班产
     * @param lhTimeSeconds 硫化时长
     * @param mouldQty 模台数
     * @param cleaningWindowList 清洗窗口
     * @param maintenanceWindowList 保养窗口
     * @return 修正后的班次最大计划量
     */
    private Map<Integer, Integer> calculateDailyStandardShiftCapacityMap(LhScheduleContext context,
                                                                         LhScheduleResult result,
                                                                         List<LhShiftConfigVO> shifts,
                                                                         Date startTime,
                                                                         int shiftCapacity,
                                                                         int lhTimeSeconds,
                                                                         int mouldQty,
                                                                         List<MachineCleaningWindowDTO> cleaningWindowList,
                                                                         List<MachineMaintenanceWindowDTO> maintenanceWindowList) {
        Map<Integer, Integer> rawShiftCapacityMap = new LinkedHashMap<Integer, Integer>(
                CollectionUtils.isEmpty(shifts) ? 0 : shifts.size());
        if (context == null || result == null || CollectionUtils.isEmpty(shifts)
                || shiftCapacity <= 0 || lhTimeSeconds <= 0 || mouldQty <= 0) {
            return rawShiftCapacityMap;
        }
        int dryIceLossQty = context.getParamIntValue(
                LhScheduleParamConstant.DRY_ICE_LOSS_QTY, LhScheduleConstant.DRY_ICE_LOSS_QTY);
        int dryIceDurationHours = context.getParamIntValue(
                LhScheduleParamConstant.DRY_ICE_DURATION_HOURS, LhScheduleConstant.DRY_ICE_DURATION_HOURS);
        int plannedRepairFixedQty = context.getParamIntValue(
                LhScheduleParamConstant.PLANNED_REPAIR_FIXED_QTY, LhScheduleConstant.PLANNED_REPAIR_FIXED_QTY);
        String configPlusShiftType = ShiftCapacityResolverUtil.resolveOddShiftCapacityPlusShiftType(context);
        String remainShiftType = ShiftCapacityResolverUtil.resolveDailyStandardCapacityRemainShiftType(context);
        boolean dailyStandardStructureMatched =
                ShiftCapacityResolverUtil.isDailyStandardCapacityStructureMatched(
                        context, result.getStructureName());
        int remainShiftCapacityUpperLimit =
                ShiftCapacityResolverUtil.resolveDailyStandardRemainShiftCapacityUpperLimit(
                        context, result.getMaterialCode(), shiftCapacity);
        boolean singleControlMachine = LhSingleControlMachineUtil.isConfiguredSingleControlMachine(
                context, result.getLhMachineCode());
        boolean started = false;
        for (LhShiftConfigVO shift : shifts) {
            if (!started) {
                if (startTime != null && !startTime.before(shift.getShiftEndDateTime())
                        && shift != shifts.get(shifts.size() - 1)) {
                    continue;
                }
                started = true;
            }
            ShiftProductionControlDTO control = ShiftProductionControlUtil.resolveEffectiveControl(context, shift, startTime);
            if (control == null || !control.isCanSchedule()) {
                continue;
            }
            // 仅参数清单内结构允许剩余班次使用独立理论上限；未命中结构始终从原始班产开始扣减。
            int currentShiftCapacity = dailyStandardStructureMatched && !singleControlMachine
                    && ShiftCapacityResolverUtil.isDailyStandardRemainShift(shift, remainShiftType)
                    ? remainShiftCapacityUpperLimit : shiftCapacity;
            int shiftMaxQty = ShiftCapacityResolverUtil.resolveShiftCapacityWithDowntime(
                    context.getDevicePlanShutList(),
                    cleaningWindowList,
                    maintenanceWindowList,
                    result.getLhMachineCode(),
                    control.getEffectiveStartTime(),
                    control.getEffectiveEndTime(),
                    currentShiftCapacity,
                    lhTimeSeconds,
                    mouldQty,
                    ShiftCapacityResolverUtil.resolveShiftDurationSeconds(shift),
                    dryIceLossQty,
                    dryIceDurationHours,
                    shift,
                    configPlusShiftType,
                    ScheduleTypeEnum.TYPE_BLOCK.getCode(),
                    plannedRepairFixedQty);
            shiftMaxQty = ShiftProductionControlUtil.deductCapacityByControl(control, shiftMaxQty, mouldQty);
            rawShiftCapacityMap.put(shift.getShiftIndex(), Math.max(0, shiftMaxQty));
        }
        int dailyStandardQty = ShiftCapacityResolverUtil.resolveDailyStandardQty(context, result.getMaterialCode());
        Map<Integer, Integer> adjustedMap = dailyStandardStructureMatched
                ? ShiftCapacityResolverUtil.adjustShiftPlanQtyMapByDailyStandard(
                        shifts, rawShiftCapacityMap, rawShiftCapacityMap, dailyStandardQty, shiftCapacity,
                        remainShiftCapacityUpperLimit, remainShiftType,
                        singleControlMachine, ScheduleTypeEnum.TYPE_BLOCK.getCode())
                : rawShiftCapacityMap;
        if (!Objects.equals(rawShiftCapacityMap, adjustedMap)) {
            log.info("日标准产量班次计划量修正, 当前流程: 换活字块排产, materialCode: {}, "
                            + "structureName: {}, 结构是否命中参数: {}, machineCode: {}, "
                            + "是否单控机台: {}, SKU日标准产量: {}, 班产: {}, 剩余班次理论上限: {}, "
                            + "日标准产量剩余班次参数值: {}, "
                            + "修正前班次计划量: {}, 修正后班次计划量: {}",
                    result.getMaterialCode(), result.getStructureName(), dailyStandardStructureMatched,
                    result.getLhMachineCode(), singleControlMachine, dailyStandardQty,
                    shiftCapacity, remainShiftCapacityUpperLimit,
                    remainShiftType, rawShiftCapacityMap, adjustedMap);
        }
        return adjustedMap;
    }

    /**
     * 按班次索引设置计划量和开始结束时间。
     *
     * @param result 排程结果
     * @param shiftIndex 班次索引
     * @param qty 计划量
     * @param startTime 开始时间
     * @param endTime 结束时间
     */
    private void setShiftPlanQty(LhScheduleResult result, int shiftIndex, int qty, Date startTime, Date endTime) {
        ShiftFieldUtil.setShiftPlanQty(result, shiftIndex, qty, startTime, endTime);
    }

    /**
     * 计算规格收尾时间。
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param shifts 班次
     * @param lhTimeSeconds 硫化时间
     * @param mouldQty 模台数
     * @param isEnding 是否收尾
     * @return 收尾时间
     */
    private Date calcSpecEndTime(LhScheduleContext context,
                                 LhScheduleResult result,
                                 List<LhShiftConfigVO> shifts,
                                 int lhTimeSeconds,
                                 int mouldQty,
                                 boolean isEnding) {
        if (!isEnding) {
            return null;
        }
        // 找到最后一个有计划量的班次，按真实产量推导完工时刻。
        for (int i = shifts.size() - 1; i >= 0; i--) {
            LhShiftConfigVO shift = shifts.get(i);
            Integer shiftPlanQty = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
            Date shiftStartTime = ShiftFieldUtil.getShiftStartTime(result, shift.getShiftIndex());
            Date shiftEndTime = ShiftFieldUtil.getShiftEndTime(result, shift.getShiftIndex());
            if (shiftEndTime == null) {
                shiftEndTime = shift.getShiftEndDateTime();
            }
            if (shiftPlanQty == null || shiftPlanQty <= 0 || shiftStartTime == null) {
                continue;
            }
            if (lhTimeSeconds <= 0 || mouldQty <= 0) {
                return shiftEndTime;
            }
            long secondsNeeded = (long) Math.ceil((double) shiftPlanQty / mouldQty) * lhTimeSeconds;
            List<MachineCleaningWindowDTO> cleaningWindowList = resolveEffectiveCleaningWindowList(
                    context, result, resolveFirstPlannedShiftStartTime(result));
            Date shiftCompletionTime = ShiftCapacityResolverUtil.resolveCompletionTimeWithDowntimes(
                    context.getDevicePlanShutList(),
                    cleaningWindowList,
                    result.getLhMachineCode(),
                    shiftStartTime,
                    secondsNeeded);
            if (shiftCompletionTime != null) {
                return constrainCompletionWithinShift(shiftCompletionTime, shiftEndTime);
            }
            return shiftEndTime;
        }
        return null;
    }

    /**
     * 刷新结果汇总字段。
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param shifts 班次
     */
    private void refreshResultSummary(LhScheduleContext context, LhScheduleResult result, List<LhShiftConfigVO> shifts) {
        if (result == null) {
            return;
        }
        ShiftFieldUtil.syncDailyPlanQty(result);
        if (result.getDailyPlanQty() == null || result.getDailyPlanQty() <= 0) {
            // 零计划结果不参与完工时刻语义。
            result.setSpecEndTime(null);
            result.setTdaySpecEndTime(null);
            ResultDowntimeSummaryUtil.clearDowntimeSummary(result);
            return;
        }
        int lhTimeSeconds = result.getLhTime() != null ? result.getLhTime() : 0;
        int mouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(
                result.getMouldQty() != null ? result.getMouldQty() : 0);
        Date specEndTime = calcSpecEndTime(context, result, shifts, lhTimeSeconds, mouldQty,
                YES_FLAG.equals(result.getIsEnd()));
        if (specEndTime == null) {
            // 非收尾结果也要保留可推导完工时刻，避免后续校验出现 specEndTime 缺失。
            specEndTime = resolveActualCompletionTime(context, result);
        }
        result.setSpecEndTime(specEndTime);
        result.setTdaySpecEndTime(specEndTime);
        syncResultDowntimeSummary(context, result);
    }

    /**
     * 根据排程结果回写机台状态。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param sku SKU
     * @param result 排程结果
     */
    private void updateMachineState(LhScheduleContext context,
                                    MachineScheduleDTO machine,
                                    SkuScheduleDTO sku,
                                    LhScheduleResult result) {
        machine.setPreviousMaterialCode(machine.getCurrentMaterialCode());
        machine.setPreviousMaterialDesc(machine.getCurrentMaterialDesc());
        machine.setCurrentMaterialCode(sku.getMaterialCode());
        machine.setCurrentMaterialDesc(sku.getMaterialDesc());
        machine.setPreviousSpecCode(sku.getSpecCode());
        machine.setPreviousProSize(sku.getProSize());
        // 机台预计结束时间严格回写为实际完工时间，避免被整班结束时间放大。
        machine.setEstimatedEndTime(resolveActualCompletionTime(context, result));
        machine.setEnding(YES_FLAG.equals(result.getIsEnd()) && result.getSpecEndTime() != null);
    }

    /**
     * 解析排程结果的实际完工时间。
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @return 实际完工时间
     */
    private Date resolveActualCompletionTime(LhScheduleContext context, LhScheduleResult result) {
        if (result == null) {
            return null;
        }
        int lhTimeSeconds = result.getLhTime() != null ? result.getLhTime() : 0;
        int mouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(
                result.getMouldQty() != null ? result.getMouldQty() : 0);
        if (lhTimeSeconds > 0 && mouldQty > 0) {
            Date actualCompletionTime = null;
            List<MachineCleaningWindowDTO> cleaningWindowList = resolveEffectiveCleaningWindowList(
                    context, result, resolveFirstPlannedShiftStartTime(result));
            List<MachineMaintenanceWindowDTO> maintenanceWindowList = resolveMachineMaintenanceWindowList(
                    context, result.getLhMachineCode());
            for (int shiftIndex = 1; shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
                Integer shiftPlanQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
                Date shiftStartTime = ShiftFieldUtil.getShiftStartTime(result, shiftIndex);
                if (shiftPlanQty == null || shiftPlanQty <= 0 || shiftStartTime == null) {
                    continue;
                }
                Date shiftEndTime = ShiftFieldUtil.getShiftEndTime(result, shiftIndex);
                long secondsNeeded = (long) Math.ceil((double) shiftPlanQty / mouldQty) * lhTimeSeconds;
                Date shiftCompletionTime = ShiftCapacityResolverUtil.resolveCompletionTimeWithDowntimes(
                        context.getDevicePlanShutList(),
                        cleaningWindowList,
                        maintenanceWindowList,
                        result.getLhMachineCode(),
                        shiftStartTime,
                        secondsNeeded);
                if (shiftCompletionTime == null) {
                    shiftCompletionTime = shiftEndTime;
                } else {
                    shiftCompletionTime = constrainCompletionWithinShift(shiftCompletionTime, shiftEndTime);
                }
                if (actualCompletionTime == null || shiftCompletionTime.after(actualCompletionTime)) {
                    actualCompletionTime = shiftCompletionTime;
                }
            }
            if (actualCompletionTime != null) {
                return actualCompletionTime;
            }
        }
        return result.getSpecEndTime();
    }

    /**
     * 命中清洗与换活字块组合场景时，写入最后一个重叠班次的原因分析。
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param shifts 班次
     */
    private void applyTypeBlockCleaningAnalysis(LhScheduleContext context,
                                                LhScheduleResult result,
                                                List<LhShiftConfigVO> shifts) {
        if (context == null || result == null || CollectionUtils.isEmpty(shifts)) {
            return;
        }
        Date switchStartTime = result.getMouldChangeStartTime();
        Date productionStartTime = resolveFirstPlannedShiftStartTime(result);
        if (switchStartTime == null || productionStartTime == null) {
            return;
        }
        int firstPlannedShiftIndex = resolveFirstPlannedShiftIndex(result);
        if (firstPlannedShiftIndex <= 0) {
            return;
        }
        // 换活字块完成时间 = 切换开始 + 换活字块总时长；取完成时间与首个生产班次开始时间的较大者
        // 作为重叠判定上界，避免切换开始时间与生产开始时间相同时（零时长区间）重叠检测失效。
        Date switchCompleteTime = LhScheduleTimeUtil.addHours(switchStartTime,
                LhScheduleTimeUtil.getTypeBlockChangeTotalHours(context));
        Date overlapEndTime = switchCompleteTime.after(productionStartTime)
                ? switchCompleteTime : productionStartTime;
        List<MachineCleaningWindowDTO> cleaningWindowList =
                resolveMachineCleaningWindowList(context, result.getLhMachineCode());
        int analysisShiftIndex = MachineCleaningOverlapUtil.resolveLastOverlapShiftIndex(
                shifts, switchStartTime, overlapEndTime);
        if (analysisShiftIndex <= 0) {
            analysisShiftIndex = firstPlannedShiftIndex;
        }
        // 换活字块调用处只写清洗固定枚举原因，不再沿用旧“模具清洗+换活字块”泛化文案。
        if (MachineCleaningOverlapUtil.hasCleaningTypeBlockingOverlap(
                cleaningWindowList, CleaningTypeEnum.DRY_ICE.getCode(), switchStartTime, overlapEndTime)) {
            ShiftFieldUtil.appendShiftAnalysis(result, analysisShiftIndex, TYPE_BLOCK_DRY_ICE_CLEANING_ANALYSIS);
        }
        if (MachineCleaningOverlapUtil.hasCleaningTypeBlockingOverlap(
                cleaningWindowList, CleaningTypeEnum.SAND_BLAST.getCode(), switchStartTime, overlapEndTime)) {
            ShiftFieldUtil.appendShiftAnalysis(result, analysisShiftIndex, TYPE_BLOCK_SAND_BLAST_CLEANING_ANALYSIS);
        }
    }

    /**
     * 获取首个有排产量的班次索引。
     *
     * @param result 排程结果
     * @return 班次索引
     */
    private int resolveFirstPlannedShiftIndex(LhScheduleResult result) {
        if (result == null) {
            return -1;
        }
        for (int shiftIndex = 1; shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
            Integer shiftQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
            if (shiftQty != null && shiftQty > 0) {
                return shiftIndex;
            }
        }
        return -1;
    }

    /**
     * 获取首个有排产量的班次开始时间。
     *
     * @param result 排程结果
     * @return 开始时间
     */
    private Date resolveFirstPlannedShiftStartTime(LhScheduleResult result) {
        int firstPlannedShiftIndex = resolveFirstPlannedShiftIndex(result);
        if (firstPlannedShiftIndex <= 0) {
            return null;
        }
        return ShiftFieldUtil.getShiftStartTime(result, firstPlannedShiftIndex);
    }

    /**
     * 将完工时间限制在当前班次内。
     *
     * @param completionTime 完工时间
     * @param shiftEndTime 班次结束时间
     * @return 限制后时间
     */
    private Date constrainCompletionWithinShift(Date completionTime, Date shiftEndTime) {
        if (completionTime == null || shiftEndTime == null) {
            return completionTime;
        }
        return completionTime.after(shiftEndTime) ? shiftEndTime : completionTime;
    }

    /**
     * 解析机台清洗窗口。
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @return 清洗窗口
     */
    private List<MachineCleaningWindowDTO> resolveMachineCleaningWindowList(LhScheduleContext context, String machineCode) {
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
        if (machine == null || CollectionUtils.isEmpty(machine.getCleaningWindowList())) {
            return new ArrayList<>();
        }
        return machine.getCleaningWindowList();
    }

    /**
     * 解析机台保养窗口。
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @return 保养窗口
     */
    private List<MachineMaintenanceWindowDTO> resolveMachineMaintenanceWindowList(LhScheduleContext context,
                                                                                  String machineCode) {
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
        List<MachineMaintenanceWindowDTO> maintenanceWindowList = machine == null
                ? new ArrayList<>() : machine.getMaintenanceWindowList();
        // 换活字块的容量计算与新增、续作共用计划性维修及预热窗口，避免预演与落地口径不一致。
        return ShiftCapacityResolverUtil.resolveCapacityMaintenanceWindowList(
                context, context.getDevicePlanShutList(), machineCode, maintenanceWindowList);
    }

    /**
     * 获取机台真实精度保养窗口，仅供停机摘要展示。
     *
     * @param context 排程上下文
     * @param machineCode 机台编号
     * @return 真实精度保养窗口，不包含计划性维修容量窗口
     */
    private List<MachineMaintenanceWindowDTO> resolveActualMachineMaintenanceWindowList(
            LhScheduleContext context, String machineCode) {
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
        return machine == null || CollectionUtils.isEmpty(machine.getMaintenanceWindowList())
                ? new ArrayList<>() : machine.getMaintenanceWindowList();
    }

    private List<MdmDevicePlanShut> resolveMachineShutdownWindowList(LhScheduleContext context, String machineCode) {
        if (context == null || CollectionUtils.isEmpty(context.getDevicePlanShutList())
                || StringUtils.isEmpty(machineCode)) {
            return new ArrayList<>();
        }
        List<MdmDevicePlanShut> shutdownWindowList = new ArrayList<>(4);
        for (MdmDevicePlanShut planShut : context.getDevicePlanShutList()) {
            if (planShut != null && StringUtils.equals(machineCode, planShut.getMachineCode())) {
                shutdownWindowList.add(planShut);
            }
        }
        return shutdownWindowList;
    }

    private void syncResultDowntimeSummary(LhScheduleContext context, LhScheduleResult result) {
        if (context == null || result == null) {
            return;
        }
        Date firstPlannedShiftStartTime = resolveFirstPlannedShiftStartTime(result);
        if (firstPlannedShiftStartTime == null || result.getSpecEndTime() == null) {
            ResultDowntimeSummaryUtil.clearDowntimeSummary(result);
            return;
        }
        List<LhShiftConfigVO> scheduleWindowShifts = context.getScheduleWindowShifts();
        ResultDowntimeSummaryUtil.fillDowntimeSummary(
                result,
                resolveActualMachineMaintenanceWindowList(context, result.getLhMachineCode()),
                resolveEffectiveCleaningWindowList(context, result, firstPlannedShiftStartTime),
                resolveMachineShutdownWindowList(context, result.getLhMachineCode()),
                scheduleWindowShifts);
        // 换活字块结果的“清洗+换活字块”备注由 applyTypeBlockCleaningAnalysis 统一处理，
        // 这里不再调用 appendCleaningMouldChangeAnalysis 写“清洗+换模”，避免换活字块场景备注错写为换模。
        if (!YES_FLAG.equals(result.getIsTypeBlock())) {
            Date mouldChangeCompleteTime = Objects.nonNull(result.getMouldChangeStartTime())
                    ? LhScheduleTimeUtil.addHours(result.getMouldChangeStartTime(),
                    LhScheduleTimeUtil.getMouldChangeTotalHours(context)) : firstPlannedShiftStartTime;
            ResultDowntimeSummaryUtil.appendCleaningMouldChangeAnalysis(
                    result,
                    resolveMachineCleaningWindowList(context, result.getLhMachineCode()),
                    result.getMouldChangeStartTime(),
                    mouldChangeCompleteTime,
                    scheduleWindowShifts);
        }
    }

    /**
     * 解析换活字块结果在排产阶段需要生效的清洗窗口。
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param firstProductionStartTime 首个有排产量班次开始时间
     * @return 有效清洗窗口
     */
    private List<MachineCleaningWindowDTO> resolveEffectiveCleaningWindowList(LhScheduleContext context,
                                                                              LhScheduleResult result,
                                                                              Date firstProductionStartTime) {
        if (result == null) {
            return new ArrayList<>(0);
        }
        List<MachineCleaningWindowDTO> cleaningWindowList = resolveMachineCleaningWindowList(
                context, result.getLhMachineCode());
        if (CleaningScheduleRuleUtil.shouldSkipCleaningByResultEnding(result)) {
            return new ArrayList<>(0);
        }
        Date switchEndTime = resolveCleaningSwitchEndTime(context, result, firstProductionStartTime);
        return new ArrayList<>(MachineCleaningOverlapUtil.excludeOverlapWindows(
                cleaningWindowList, result.getMouldChangeStartTime(), switchEndTime));
    }

    /**
     * 解析清洗与换活字块重叠过滤使用的切换结束时间。
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param firstProductionStartTime 首个有计划量班次开始时间
     * @return 清洗重叠过滤使用的切换结束时间
     */
    private Date resolveCleaningSwitchEndTime(LhScheduleContext context,
                                              LhScheduleResult result,
                                              Date firstProductionStartTime) {
        if (Objects.nonNull(result) && Objects.nonNull(result.getMouldChangeStartTime())) {
            return LhScheduleTimeUtil.addHours(result.getMouldChangeStartTime(),
                    LhScheduleTimeUtil.getTypeBlockChangeTotalHours(context));
        }
        return firstProductionStartTime;
    }

    /**
     * 解析机台当前物料胎胚编码。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @return 胎胚编码
     */
    private String resolveMachineEmbryoCode(LhScheduleContext context, MachineScheduleDTO machine) {
        MdmMaterialInfo materialInfo = resolveMachineMaterialInfo(context, machine);
        if (materialInfo != null && StringUtils.isNotEmpty(materialInfo.getEmbryoCode())) {
            return materialInfo.getEmbryoCode();
        }
        SkuScheduleDTO currentSku = findSkuByMaterialCode(context.getContinuousSkuList(), machine.getCurrentMaterialCode());
        return currentSku != null ? currentSku.getEmbryoCode() : null;
    }

    /**
     * 解析机台当前物料胎胚描述。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @return 胎胚描述
     */
    private String resolveMachineEmbryoDesc(LhScheduleContext context, MachineScheduleDTO machine) {
        MdmMaterialInfo materialInfo = resolveMachineMaterialInfo(context, machine);
        if (materialInfo != null && StringUtils.isNotEmpty(materialInfo.getEmbryoDesc())) {
            return normalizeCompareToken(materialInfo.getEmbryoDesc());
        }
        SkuScheduleDTO currentSku = findSkuByMaterialCode(context.getContinuousSkuList(), machine.getCurrentMaterialCode());
        return currentSku != null ? normalizeCompareToken(currentSku.getMainMaterialDesc()) : null;
    }

    /**
     * 解析 SKU 胎胚描述。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @return 胎胚描述
     */
    private String resolveSkuEmbryoDesc(LhScheduleContext context, SkuScheduleDTO sku) {
        if (context == null || sku == null) {
            return null;
        }
        MdmMaterialInfo materialInfo = context.getMaterialInfoMap().get(sku.getMaterialCode());
        if (materialInfo != null && StringUtils.isNotEmpty(materialInfo.getEmbryoDesc())) {
            return normalizeCompareToken(materialInfo.getEmbryoDesc());
        }
        return normalizeCompareToken(sku.getMainMaterialDesc());
    }

    /**
     * 解析机台当前物料规格编码。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @return 规格编码
     */
    private String resolveMachineSpecCode(LhScheduleContext context, MachineScheduleDTO machine) {
        if (StringUtils.isNotEmpty(machine.getPreviousSpecCode())) {
            return machine.getPreviousSpecCode();
        }
        MdmMaterialInfo materialInfo = resolveMachineMaterialInfo(context, machine);
        if (materialInfo != null && StringUtils.isNotEmpty(materialInfo.getSpecifications())) {
            return materialInfo.getSpecifications();
        }
        SkuScheduleDTO currentSku = findSkuByMaterialCode(context.getContinuousSkuList(), machine.getCurrentMaterialCode());
        return currentSku != null ? currentSku.getSpecCode() : null;
    }

    /**
     * 解析机台当前物料花纹键。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @return 花纹键
     */
    private String resolveMachinePatternKey(LhScheduleContext context, MachineScheduleDTO machine) {
        MdmMaterialInfo materialInfo = resolveMachineMaterialInfo(context, machine);
        if (materialInfo != null) {
            return resolvePatternKey(materialInfo.getMainPattern(), materialInfo.getPattern());
        }
        SkuScheduleDTO currentSku = findSkuByMaterialCode(context.getContinuousSkuList(), machine.getCurrentMaterialCode());
        if (currentSku == null) {
            return null;
        }
        return resolvePatternKey(currentSku.getMainPattern(), currentSku.getPattern());
    }

    /**
     * 解析机台当前物料主花纹。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @return 主花纹
     */
    private String resolveMachineMainPatternStrict(LhScheduleContext context, MachineScheduleDTO machine) {
        MdmMaterialInfo materialInfo = resolveMachineMaterialInfo(context, machine);
        if (materialInfo != null && StringUtils.isNotEmpty(materialInfo.getMainPattern())) {
            return normalizeCompareToken(materialInfo.getMainPattern());
        }
        SkuScheduleDTO currentSku = findSkuByMaterialCode(context.getContinuousSkuList(), machine.getCurrentMaterialCode());
        return currentSku != null ? normalizeCompareToken(currentSku.getMainPattern()) : null;
    }

    /**
     * 解析 SKU 主花纹。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @return 主花纹
     */
    private String resolveSkuMainPatternStrict(LhScheduleContext context, SkuScheduleDTO sku) {
        if (context == null || sku == null) {
            return null;
        }
        MdmMaterialInfo materialInfo = context.getMaterialInfoMap().get(sku.getMaterialCode());
        if (materialInfo != null && StringUtils.isNotEmpty(materialInfo.getMainPattern())) {
            return normalizeCompareToken(materialInfo.getMainPattern());
        }
        return normalizeCompareToken(sku.getMainPattern());
    }

    /**
     * 解析机台当前物料基础信息。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @return 物料基础信息
     */
    private MdmMaterialInfo resolveMachineMaterialInfo(LhScheduleContext context, MachineScheduleDTO machine) {
        if (context == null || machine == null || StringUtils.isEmpty(machine.getCurrentMaterialCode())) {
            return null;
        }
        return context.getMaterialInfoMap().get(machine.getCurrentMaterialCode());
    }

    /**
     * 按物料编码查找 SKU。
     *
     * @param skuList SKU列表
     * @param materialCode 物料编码
     * @return SKU
     */
    private SkuScheduleDTO findSkuByMaterialCode(List<SkuScheduleDTO> skuList, String materialCode) {
        if (CollectionUtils.isEmpty(skuList) || StringUtils.isEmpty(materialCode)) {
            return null;
        }
        for (SkuScheduleDTO sku : skuList) {
            if (StringUtils.equals(materialCode, sku.getMaterialCode())) {
                return sku;
            }
        }
        return null;
    }

    /**
     * 解析花纹键。
     *
     * @param mainPattern 主花纹
     * @param pattern 花纹
     * @return 花纹键
     */
    private String resolvePatternKey(String mainPattern, String pattern) {
        if (StringUtils.isNotEmpty(mainPattern)) {
            return mainPattern;
        }
        return StringUtils.isNotEmpty(pattern) ? pattern : null;
    }

    /**
     * 规范化比较文本。
     *
     * @param value 原始文本
     * @return 规范化文本
     */
    private String normalizeCompareToken(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        String normalizedValue = value.trim();
        return StringUtils.isEmpty(normalizedValue) ? null : normalizedValue;
    }

    /**
     * 解析换活字块结果实际使用的模具号。
     * <p>换活字块不是更换整副模具，因此不释放在机模具，也不按新SKU重新分配模具；
     * 结果字段沿用当前机台硫化在机信息中的实际模具号。</p>
     *
     * @param context 排程上下文
     * @param machine 当前机台
     * @param sku 候选SKU
     * @return 实际使用模具号，多个逗号分隔
     */
    private String resolveTypeBlockActualMouldCode(LhScheduleContext context,
                                                   MachineScheduleDTO machine,
                                                   SkuScheduleDTO sku) {
        if (context == null || machine == null) {
            return null;
        }
        int requiredMouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(machine);
        String mouldCode = LhMouldCodeUtil.resolveInMachineMouldCode(context, machine.getMachineCode());
        if (StringUtils.isEmpty(mouldCode)) {
            log.info("换活字块结果在机实际模具号为空, machineCode: {}, currentMaterialCode: {}, materialCode: {}, "
                            + "requiredMouldQty: {}",
                    machine.getMachineCode(), machine.getCurrentMaterialCode(),
                    sku == null ? null : sku.getMaterialCode(), requiredMouldQty);
            return null;
        }
        log.debug("换活字块沿用在机实际模具号, machineCode: {}, currentMaterialCode: {}, materialCode: {}, "
                        + "requiredMouldQty: {}, actualMouldCode: {}",
                machine.getMachineCode(), machine.getCurrentMaterialCode(),
                sku == null ? null : sku.getMaterialCode(), requiredMouldQty, mouldCode);
        return mouldCode;
    }

    /**
     * 解析物料对应的模具编码集合。
     *
     * @param context 排程上下文
     * @param materialCode 物料编码
     * @return 模具编码集合
     */
    private Set<String> resolveMouldCodeSet(LhScheduleContext context, String materialCode) {
        Set<String> mouldCodeSet = new LinkedHashSet<>(4);
        if (context == null
                || StringUtils.isEmpty(materialCode)
                || !context.getSkuMouldRelMap().containsKey(materialCode)) {
            return mouldCodeSet;
        }
        for (MdmSkuMouldRel mouldRel : context.getSkuMouldRelMap().get(materialCode)) {
            String mouldCode = mouldRel == null ? null : normalizeCompareToken(mouldRel.getMouldCode());
            if (StringUtils.isEmpty(mouldCode)) {
                continue;
            }
            mouldCodeSet.add(mouldCode);
        }
        return mouldCodeSet;
    }

    /**
     * 注册机台排程分配记录。
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @param result 排程结果
     */
    private void registerMachineAssignment(LhScheduleContext context, String machineCode, LhScheduleResult result) {
        context.getMachineAssignmentMap()
                .computeIfAbsent(machineCode, k -> new ArrayList<>())
                .add(result);
    }

    /**
     * 生成工单号。
     *
     * @param context 排程上下文
     * @return 工单号
     */
    private String generateOrderNo(LhScheduleContext context) {
        return orderNoGenerator.generateOrderNo(context.getScheduleTargetDate());
    }

    /**
     * 获取目标排产量解析器。
     *
     * @return 目标排产量解析器
     */
    private TargetScheduleQtyResolver getTargetScheduleQtyResolver() {
        return targetScheduleQtyResolver != null
                ? targetScheduleQtyResolver
                : new TargetScheduleQtyResolver();
    }

    /**
     * 获取换模均衡策略。
     *
     * @return 换模均衡策略
     */
    private IMouldChangeBalanceStrategy getMouldChangeBalanceStrategy() {
        return mouldChangeBalanceStrategy;
    }

    /**
     * 获取首检均衡策略。
     *
     * @return 首检均衡策略
     */
    private IFirstInspectionBalanceStrategy getFirstInspectionBalanceStrategy() {
        return firstInspectionBalanceStrategy;
    }

    /**
     * 获取产能计算策略。
     *
     * @return 产能计算策略
     */
    private ICapacityCalculateStrategy getCapacityCalculateStrategy() {
        return capacityCalculateStrategy;
    }

    /**
     * 获取保养排程服务。
     *
     * @return 保养排程服务
     */
    private LhMaintenanceScheduleService getMaintenanceScheduleService() {
        return maintenanceScheduleService != null
                ? maintenanceScheduleService
                : new LhMaintenanceScheduleService();
    }

    /**
     * 换活字块结果按日计划账本回裁。
     * <p>收尾结果严格截断，非收尾超排记录为满班补齐。</p>
     *
     * @param context 排程上下文
     * @param sku SKU排程DTO
     * @param result 排程结果
     * @param shifts 班次列表
     * @param firstInspectionAllocationPlan 与候选结果共用的首检真实时间分摊计划
     * @param inspectionQuantityMultiplier 首检数量倍数；普通结果为1，单控整机合计结果为2
     * @return 回裁后的实际排产量
     */
    private int applyTypeBlockToDailyQuota(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LhScheduleResult result,
            List<LhShiftConfigVO> shifts,
            FirstInspectionAllocationPlan firstInspectionAllocationPlan,
            int inspectionQuantityMultiplier) {
        int cappedQty = getTargetScheduleQtyResolver().capResultByProductionRemainingQty(
                context, sku, result, shifts, "换活字块");
        if (cappedQty <= 0) {
            return 0;
        }
        if (!FirstInspectionQtyUtil.isFirstInspectionAllocationRetained(
                result, firstInspectionAllocationPlan,
                inspectionQuantityMultiplier)) {
            log.warn("换活字块实际消费账本裁剪后无法完整保留首检，拒绝当前候选, "
                            + "batchNo: {}, materialCode: {}, machineCode: {}",
                    context.getBatchNo(), sku.getMaterialCode(), result.getLhMachineCode());
            return 0;
        }
        /*
         * 提前生产必须消费共享的临时前移 dayN；普通换活字块直接消费 SKU 原始账本。
         * 不能让已激活但不属于本结果业务日的临时视图污染原计划日扣账。
         */
        Map<LocalDate, SkuDailyPlanQuotaDTO> quotaMap =
                YES_FLAG.equals(result.getIsEarlyProduction())
                        ? context.resolveEffectiveDailyPlanQuotaMap(sku)
                        : sku.getDailyPlanQuotaMap();
        if (quotaMap == null || quotaMap.isEmpty()) {
            refreshResultSummary(context, result, shifts);
            int actualQty = result.getDailyPlanQty() != null ? result.getDailyPlanQty() : 0;
            getTargetScheduleQtyResolver().deductProductionRemainingQty(
                    context, sku, actualQty, "换活字块", result.getLhMachineCode());
            return actualQty;
        }
        int totalShiftFillOverQty = 0;
        for (LhShiftConfigVO shift : shifts) {
            Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
            if (planQty == null || planQty <= 0) {
                continue;
            }
            Date workDate = shift.getWorkDate();
            if (workDate == null) {
                continue;
            }
            LocalDate productionDate = workDate.toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            SkuDailyPlanQuotaDTO quota = quotaMap.get(productionDate);
            if (quota == null) {
                continue;
            }
            LocalDate lookAheadEndDate = resolveLookAheadEndDate(context, quotaMap, productionDate);
            // 先按只读账本计算本班允许落地量，再按模台数收敛，避免双模回裁出奇数计划量。
            int quotaCap = resolveConsumableRollingQuota(quotaMap, productionDate, planQty, lookAheadEndDate);
            int mouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(
                    result.getMouldQty() != null ? result.getMouldQty() : 0);
            int allowedPlanQty = Math.min(planQty, quotaCap);
            int normalizedPlanQty = getTargetScheduleQtyResolver().resolveAllocatedShiftQty(
                    context, sku, allowedPlanQty, planQty, mouldQty);
            // 按历史欠产、当日计划、受限追补窗口消费同一SKU的日计划账本
            int consumed = normalizedPlanQty > 0
                    ? SkuDailyPlanQuotaUtil.consumeRollingQuota(
                    quotaMap, productionDate, normalizedPlanQty, lookAheadEndDate)
                    : 0;
            int overQty = planQty - consumed;
            if (overQty > 0) {
                boolean endingResult = YES_FLAG.equals(result.getIsEnd());
                // 收尾结果必须严格截断，不再记录满班补齐超排；
                // 试制等严格目标量场景仍需回裁，但保留超排账本用于追踪被截掉的补满量。
                if (endingResult || shouldApplyStrictTypeBlockQuotaLimit(sku, endingResult)) {
                    /*
                     * 收尾/严格目标只能回裁首检之后的正式生产量。换活字块阶段
                     * 已经发生的首检必须保留，且回裁到仅余首检时恢复真实首检区间。
                     */
                    FirstInspectionQtyUtil.trimShiftPlanQtyPreservingInspection(
                            result, shift.getShiftIndex(), consumed,
                            firstInspectionAllocationPlan,
                            inspectionQuantityMultiplier);
                    if (endingResult) {
                        continue;
                    }
                }
                // 无法冲抵的部分记录为满班补齐超排量
                quota.setShiftFillOverQty(quota.getShiftFillOverQty() + overQty);
                totalShiftFillOverQty += overQty;
                log.debug("换活字块班次满班补齐超排, materialCode: {}, 日期: {}, 班次: {}, 排产量: {}, 超排: {}",
                        sku.getMaterialCode(), productionDate, shift.getShiftIndex(), planQty, overQty);
            }
        }
        if (totalShiftFillOverQty > 0) {
            sku.setShiftFillOverQty(sku.getShiftFillOverQty() + totalShiftFillOverQty);
            String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                    sku.getMaterialCode(), sku.getProductStatus());
            context.getSkuShiftFillOverQtyMap().merge(skuKey, totalShiftFillOverQty, Integer::sum);
        }
        refreshResultSummary(context, result, shifts);
        int actualQty = result.getDailyPlanQty() != null ? result.getDailyPlanQty() : 0;
        getTargetScheduleQtyResolver().deductProductionRemainingQty(
                context, sku, actualQty, "换活字块", result.getLhMachineCode());
        return actualQty;
    }

    /**
     * 双模换活字块按 L/R 整组合计量消费日计划和实际待排账本。
     * <p>先把单侧班次量合并成整机总量，复用现有换活字块回裁和扣账逻辑；
     * 回裁完成后再均分回两侧，避免两条结果分别按整机总量重复扣减。</p>
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param primaryResult 主侧结果
     * @param pairResult 配对侧结果
     * @param shifts 排程班次
     * @param firstInspectionAllocationPlan 单侧首检分摊计划；整机扣账时按2倍保护
     * @return L/R两侧合计实际排产量
     */
    private int applyWholeSingleControlTypeBlockToDailyQuota(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LhScheduleResult primaryResult,
            LhScheduleResult pairResult,
            List<LhShiftConfigVO> shifts,
            FirstInspectionAllocationPlan firstInspectionAllocationPlan) {
        if (Objects.isNull(primaryResult) || Objects.isNull(pairResult)) {
            return 0;
        }
        LhScheduleResult groupResult = new LhScheduleResult();
        BeanUtil.copyProperties(primaryResult, groupResult);
        int sideMouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(
                Objects.nonNull(primaryResult.getMouldQty()) ? primaryResult.getMouldQty() : 0);
        groupResult.setMouldQty(sideMouldQty * 2);
        for (int shiftIndex = 1; shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
            Integer sideQty = ShiftFieldUtil.getShiftPlanQty(primaryResult, shiftIndex);
            int groupQty = Objects.isNull(sideQty) || sideQty <= 0 ? 0 : sideQty * 2;
            ShiftFieldUtil.setShiftPlanQty(groupResult, shiftIndex, groupQty,
                    ShiftFieldUtil.getShiftStartTime(primaryResult, shiftIndex),
                    ShiftFieldUtil.getShiftEndTime(primaryResult, shiftIndex));
        }
        ShiftFieldUtil.syncDailyPlanQty(groupResult);
        /*
         * 整机合计结果的班次量是单侧的2倍，因此日计划回裁也必须按2倍
         * 保护首检；回写后每侧恢复为原分摊量，不会重复计入SKU目标。
         */
        int actualQty = this.applyTypeBlockToDailyQuota(
                context, sku, groupResult, shifts,
                firstInspectionAllocationPlan, 2);
        this.copyWholeSingleControlTypeBlockQtyToSides(
                context, groupResult, primaryResult, pairResult, shifts);
        return actualQty;
    }

    /**
     * 将双模整机回裁后的班次量均分回 L/R 两侧。
     *
     * @param context 排程上下文
     * @param groupResult 整机合计结果
     * @param primaryResult 主侧结果
     * @param pairResult 配对侧结果
     * @param shifts 排程班次
     */
    private void copyWholeSingleControlTypeBlockQtyToSides(LhScheduleContext context,
                                                            LhScheduleResult groupResult,
                                                            LhScheduleResult primaryResult,
                                                            LhScheduleResult pairResult,
                                                            List<LhShiftConfigVO> shifts) {
        for (int shiftIndex = 1; shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
            Integer groupQty = ShiftFieldUtil.getShiftPlanQty(groupResult, shiftIndex);
            int sideQty = Objects.isNull(groupQty) || groupQty <= 0 ? 0 : groupQty / 2;
            Date shiftStartTime = sideQty > 0 ? ShiftFieldUtil.getShiftStartTime(groupResult, shiftIndex) : null;
            Date shiftEndTime = sideQty > 0 ? ShiftFieldUtil.getShiftEndTime(groupResult, shiftIndex) : null;
            ShiftFieldUtil.setShiftPlanQty(primaryResult, shiftIndex, sideQty, shiftStartTime, shiftEndTime);
            ShiftFieldUtil.setShiftPlanQty(pairResult, shiftIndex, sideQty, shiftStartTime, shiftEndTime);
        }
        refreshResultSummary(context, primaryResult, shifts);
        refreshResultSummary(context, pairResult, shifts);
    }

    /**
     * 判断换活字块非收尾是否需要按 dayN 节奏账本严格回裁。
     *
     * @param sku SKU排程DTO
     * @param endingResult 是否收尾结果
     * @return true-需要严格回裁；false-非收尾实际排产按SKU实际消费账本控制
     */
    private boolean shouldApplyStrictTypeBlockQuotaLimit(SkuScheduleDTO sku, boolean endingResult) {
        if (endingResult || Objects.isNull(sku)) {
            return false;
        }
        return StringUtils.equals(ConstructionStageEnum.TRIAL.getCode(), sku.getConstructionStage())
                || sku.isStrictNewSpecShortageOnly();
    }

    /**
     * 只读计算当前班次可消费的滚动日计划额度。
     *
     * @param quotaMap 日计划账本
     * @param productionDate 实际生产日期
     * @param planQty 本班计划量
     * @param lookAheadEndDate 允许借用的最晚日期
     * @return 可消费额度
     */
    private int resolveConsumableRollingQuota(Map<LocalDate, SkuDailyPlanQuotaDTO> quotaMap,
                                              LocalDate productionDate,
                                              int planQty,
                                              LocalDate lookAheadEndDate) {
        if (CollectionUtils.isEmpty(quotaMap) || productionDate == null || planQty <= 0) {
            return 0;
        }
        int consumableQty = 0;
        for (Map.Entry<LocalDate, SkuDailyPlanQuotaDTO> entry : quotaMap.entrySet()) {
            if (entry.getKey().isAfter(productionDate)) {
                continue;
            }
            consumableQty += resolveRemainingQuotaQty(entry.getValue(), planQty - consumableQty);
            if (consumableQty >= planQty) {
                return planQty;
            }
        }
        for (Map.Entry<LocalDate, SkuDailyPlanQuotaDTO> entry : quotaMap.entrySet()) {
            if (!entry.getKey().isAfter(productionDate)) {
                continue;
            }
            if (lookAheadEndDate != null && entry.getKey().isAfter(lookAheadEndDate)) {
                continue;
            }
            consumableQty += resolveRemainingQuotaQty(entry.getValue(), planQty - consumableQty);
            if (consumableQty >= planQty) {
                return planQty;
            }
        }
        return Math.max(0, consumableQty);
    }

    /**
     * 解析单日账本剩余额度。
     *
     * @param quota 单日账本
     * @param demandQty 需求量
     * @return 可用额度
     */
    private int resolveRemainingQuotaQty(SkuDailyPlanQuotaDTO quota, int demandQty) {
        if (quota == null || demandQty <= 0) {
            return 0;
        }
        return Math.min(Math.max(0, quota.getRemainingQty()), demandQty);
    }

    /**
     * 解析换活字块实际扣账允许追补的截止日期。
     *
     * @param context 排程上下文
     * @param quotaMap 日计划账本
     * @param productionDate 实际生产日期
     * @return 追补截止日期
     */
    private LocalDate resolveLookAheadEndDate(LhScheduleContext context,
                                              Map<LocalDate, SkuDailyPlanQuotaDTO> quotaMap,
                                              LocalDate productionDate) {
        return SkuDailyPlanQuotaUtil.resolveLookAheadEndDate(
                quotaMap, productionDate, resolveShortageLookAheadDays(context),
                resolveScheduleTargetLocalDate(context));
    }

    /**
     * 解析排程目标业务日期。
     *
     * @param context 排程上下文
     * @return 排程目标业务日期
     */
    private LocalDate resolveScheduleTargetLocalDate(LhScheduleContext context) {
        if (context == null) {
            return null;
        }
        if (!CollectionUtils.isEmpty(context.getScheduleWindowShifts())) {
            for (int index = context.getScheduleWindowShifts().size() - 1; index >= 0; index--) {
                LhShiftConfigVO shift = context.getScheduleWindowShifts().get(index);
                if (shift != null && shift.getWorkDate() != null) {
                    return shift.getWorkDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                }
            }
        }
        if (context.getWindowEndDate() == null) {
            return null;
        }
        return context.getWindowEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * 获取追补观察天数。
     *
     * @param context 排程上下文
     * @return 向后观察天数
     */
    private int resolveShortageLookAheadDays(LhScheduleContext context) {
        if (context == null || context.getScheduleConfig() == null) {
            return LhScheduleConstant.NEW_SPEC_SHORTAGE_LOOK_AHEAD_DAYS;
        }
        return context.getScheduleConfig().getNewSpecShortageLookAheadDays();
    }

    /**
     * 回裁换活字块单个班次计划量，并清空失效的结束时刻。
     *
     * @param result 排程结果
     * @param shiftIndex 班次索引
     * @param trimmedQty 回裁后的计划量
     */
    private void trimTypeBlockShiftPlanQty(LhScheduleResult result, int shiftIndex, int trimmedQty) {
        Date shiftStartTime = ShiftFieldUtil.getShiftStartTime(result, shiftIndex);
        if (trimmedQty <= 0) {
            setShiftPlanQty(result, shiftIndex, 0, null, null);
            return;
        }
        setShiftPlanQty(result, shiftIndex, trimmedQty, shiftStartTime, null);
    }
}
