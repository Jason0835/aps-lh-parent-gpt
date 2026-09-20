package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.engine.strategy.support.StructureSwitchRuntimeState;
import com.zlt.aps.lh.engine.strategy.support.StructureSwitchSchedulingPolicy;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.enums.ScheduleStepEnum;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuDailyPlanQuotaDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhNextShiftNewPlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.component.StructureEndingAlignmentService;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.factory.ScheduleStrategyFactory;
import com.zlt.aps.lh.engine.strategy.ICapacityCalculateStrategy;
import com.zlt.aps.lh.engine.strategy.IFirstInspectionBalanceStrategy;
import com.zlt.aps.lh.engine.strategy.IMachineMatchStrategy;
import com.zlt.aps.lh.engine.strategy.IMouldChangeBalanceStrategy;
import com.zlt.aps.lh.engine.strategy.impl.NewSpecProductionStrategy;
import com.zlt.aps.lh.engine.strategy.impl.DefaultProductionShutdownStrategy;
import com.zlt.aps.lh.engine.strategy.support.DayScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.EarlyProductionRuntimePlan;
import com.zlt.aps.lh.engine.strategy.support.MouldResourceContext;
import com.zlt.aps.lh.engine.strategy.support.UnscheduledResultRuntime;
import com.zlt.aps.lh.mapper.MdmDevicePlanShutMapper;
import com.zlt.aps.lh.mapper.MdmWorkCalendarMapper;
import com.zlt.aps.lh.service.ILhShiftConfigService;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
import com.zlt.aps.mdm.api.domain.entity.MdmWorkCalendar;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

/**
 * 硫化班次9新计划独立后置服务。
 *
 * <p>服务只读取原8班最终上下文，以班次8真实收尾机台和最终生产剩余账本构建独立工作副本，
 * 再复用S4.5机台驱动新增内核完成换模、首检、产能及资源更新。独立结果只转换为
 * {@link LhNextShiftNewPlan}，不会追加或改写原排程结果。</p>
 *
 * @author APS
 */
@Slf4j
@Service
public class LhNextShiftNewPlanService {

    /** 原窗口末班索引。 */
    private static final int SOURCE_ENDING_SHIFT_INDEX = 8;
    /** 独立计算窗口中的准备班槽位。 */
    private static final int PREPARATION_SLOT_INDEX = 7;
    /** 独立计算窗口中的班次9槽位，不扩展原结果实体字段。 */
    private static final int TARGET_SLOT_INDEX = 8;
    /** 虚拟机台编码前缀。 */
    private static final String VIRTUAL_MACHINE_PREFIX = "V";
    /** 候选池日期相对引擎窗口起点T的最小、最大偏移。 */
    private static final int FIRST_POOL_DAY_OFFSET = 2;
    private static final int LAST_POOL_DAY_OFFSET = 4;
    /** 产品状态为空时按正规状态归一化。 */
    private static final String FORMAL_PRODUCT_STATUS = "S";
    /** 班次9结果固定备注。 */
    private static final String NEXT_SHIFT_PLAN_REMARK = "班次9提前生成计划";

    /** 设备停机时间转换与故障分流。 */
    @Resource
    private LhDeviceStopPlanScheduleService deviceStopPlanScheduleService;

    @Resource
    private DefaultProductionShutdownStrategy productionShutdownStrategy;
    @Resource
    private TargetScheduleQtyResolver targetScheduleQtyResolver;
    @Resource
    private ILhShiftConfigService shiftConfigService;
    @Resource
    private ScheduleStrategyFactory strategyFactory;
    @Resource
    private NewSpecProductionStrategy newSpecProductionStrategy;
    @Resource
    private StructureEndingAlignmentService structureEndingAlignmentService;
    @Resource
    private MdmDevicePlanShutMapper devicePlanShutMapper;
    @Resource
    private MdmWorkCalendarMapper workCalendarMapper;
    @Resource
    private LhNextShiftNewPlanPersistenceService persistenceService;

    /**
     * 生成并幂等刷新班次9计划。
     *
     * @param sourceContext 已完成原8班最终持久化的排程上下文
     * @return 保存的班次9计划数量
     */
    public int generateAndReplace(LhScheduleContext sourceContext) {
        Objects.requireNonNull(sourceContext, "原8班排程上下文不能为空");
        LhShiftConfigVO businessTargetShift = shiftConfigService.resolveNextShiftNight(sourceContext);
        LhShiftConfigVO sourceEndingShift = this.resolveSourceEndingShift(sourceContext);
        LocalDate targetBusinessDate = this.toLocalDate(businessTargetShift.getWorkDate());

        Map<String, Date> releaseTimeMap = this.resolveReleasedMachineTimeMap(
                sourceContext, sourceEndingShift);
        List<SkuScheduleDTO> remainingSkuList = this.resolveRemainingSkuList(
                sourceContext, targetBusinessDate);
        log.info("班次9候选准备完成, factoryCode: {}, scheduleDate: {}, batchNo: {}, "
                        + "targetBusinessDate: {}, targetWindow: [{}, {}), releasedMachineCount: {}, "
                        + "remainingSkuCount: {}",
                sourceContext.getFactoryCode(),
                LhScheduleTimeUtil.formatDate(sourceContext.getScheduleTargetDate()),
                sourceContext.getBatchNo(), targetBusinessDate,
                LhScheduleTimeUtil.formatDateTime(businessTargetShift.getShiftStartDateTime()),
                LhScheduleTimeUtil.formatDateTime(businessTargetShift.getShiftEndDateTime()),
                releaseTimeMap.size(), remainingSkuList.size());

        List<LhNextShiftNewPlan> planList = Collections.emptyList();
        Map<Long, Date> stopScheduleDates = Collections.emptyMap();
        if (!CollectionUtils.isEmpty(releaseTimeMap)
                && !CollectionUtils.isEmpty(remainingSkuList)) {
            LhScheduleContext isolatedContext = this.buildIsolatedContext(
                    sourceContext, sourceEndingShift, businessTargetShift,
                    targetBusinessDate, releaseTimeMap, remainingSkuList);
            planList = this.scheduleNextShift(
                    isolatedContext, targetBusinessDate);
            // 与原8班分开回填，只记录已执行的独立窗口；不触发任何故障业务补偿。
            stopScheduleDates = deviceStopPlanScheduleService.resolveScheduledStopDates(isolatedContext);
        } else if (CollectionUtils.isEmpty(releaseTimeMap)) {
            log.info("班次9计划为空, factoryCode: {}, batchNo: {}, reason: 班次8无真实收尾释放机台",
                    sourceContext.getFactoryCode(), sourceContext.getBatchNo());
        } else {
            log.info("班次9计划为空, factoryCode: {}, batchNo: {}, reason: 前置排产后无剩余SKU",
                    sourceContext.getFactoryCode(), sourceContext.getBatchNo());
        }
        List<LhNextShiftNewPlan> validPlanList = planList.stream()
                .filter(Objects::nonNull)
                .filter(plan -> Objects.nonNull(plan.getShiftPlanQty())
                        && plan.getShiftPlanQty().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
        int savedCount = persistenceService.replaceByScope(
                sourceContext.getFactoryCode(),
                LhScheduleTimeUtil.clearTime(sourceContext.getScheduleTargetDate()),
                sourceContext.getBatchNo(), validPlanList, sourceContext.getOperator(), stopScheduleDates);
        log.info("班次9计划生成完成, factoryCode: {}, scheduleDate: {}, batchNo: {}, "
                        + "targetBusinessDate: {}, planCount: {}, savedCount: {}",
                sourceContext.getFactoryCode(),
                LhScheduleTimeUtil.formatDate(sourceContext.getScheduleTargetDate()),
                sourceContext.getBatchNo(), targetBusinessDate, validPlanList.size(), savedCount);
        return savedCount;
    }

    /**
     * 在独立上下文中复用新增排产内核，并转换班次9结果。
     */
    private List<LhNextShiftNewPlan> scheduleNextShift(
            LhScheduleContext isolatedContext,
            LocalDate targetBusinessDate) {
        IMachineMatchStrategy machineMatchStrategy = strategyFactory.getMachineMatchStrategy();
        IMouldChangeBalanceStrategy mouldChangeStrategy = strategyFactory.getMouldChangeBalanceStrategy();
        IFirstInspectionBalanceStrategy inspectionStrategy = strategyFactory.getFirstInspectionBalanceStrategy();
        ICapacityCalculateStrategy capacityStrategy = strategyFactory.getCapacityCalculateStrategy();
        // 池内业务排序由日期池构建器执行，禁止把三个日期池合并后重新全局排序。
        structureEndingAlignmentService.prepareStructureEndingAlignmentIndex(isolatedContext);

        /*
         * 生产日只包含班次9；完整独立窗口仍保留班次8准备槽。
         * 这样原有生产日前准备回看能够识别“班次8释放、班次9生产”，不会把14:00
         * 当作班次9业务日开始而拒绝回看。原8班入口不使用本服务的日上下文。
         */
        DayScheduleContext dayContext = this.buildTargetDayContext(isolatedContext, targetBusinessDate);
        int originalResultCount = isolatedContext.getScheduleResultList().size();
        newSpecProductionStrategy.scheduleIsolatedNextShift(
                isolatedContext, dayContext, targetBusinessDate,
                machineMatchStrategy, mouldChangeStrategy,
                inspectionStrategy, capacityStrategy);

        List<LhNextShiftNewPlan> result = new ArrayList<LhNextShiftNewPlan>();
        for (int index = originalResultCount;
             index < isolatedContext.getScheduleResultList().size(); index++) {
            LhScheduleResult scheduleResult = isolatedContext.getScheduleResultList().get(index);
            Integer targetQty = ShiftFieldUtil.getShiftPlanQty(scheduleResult, TARGET_SLOT_INDEX);
            if (Objects.isNull(targetQty) || targetQty <= 0) {
                continue;
            }
            result.add(this.buildNextShiftPlan(isolatedContext, scheduleResult, targetQty));
            log.info("班次9匹配成功, materialCode: {}, productStatus: {}, machineCode: {}, "
                            + "targetBusinessDate: {}, shiftPlanQty: {}, changeMould: {}, typeBlock: {}",
                    scheduleResult.getMaterialCode(), scheduleResult.getProductStatus(),
                    scheduleResult.getLhMachineCode(), targetBusinessDate, targetQty,
                    scheduleResult.getIsChangeMould(), scheduleResult.getIsTypeBlock());
        }
        isolatedContext.getNewSpecSkuList().stream()
                .filter(Objects::nonNull)
                .filter(sku -> sku.getRemainingScheduleQty() > 0)
                .forEach(sku -> log.info(
                        "班次9SKU未排, materialCode: {}, productStatus: {}, remainingQty: {}, reason: {}",
                        sku.getMaterialCode(), sku.getProductStatus(),
                        sku.getRemainingScheduleQty(),
                        StringUtils.defaultIfEmpty(
                                this.resolveIsolatedUnscheduledReason(isolatedContext, sku),
                                "无满足班次9全部约束的机台")));
        return result;
    }

    /**
     * 构建只允许班次9生产的业务日，班次8准备范围由完整独立窗口提供。
     *
     * @param context 班次9独立上下文
     * @param targetBusinessDate 班次9实际业务日期
     * @return 仅包含目标生产槽的日上下文
     */
    private DayScheduleContext buildTargetDayContext(LhScheduleContext context, LocalDate targetBusinessDate) {
        LhShiftConfigVO targetShift = context.getScheduleWindowShifts().stream()
                .filter(shift -> Objects.equals(TARGET_SLOT_INDEX, shift.getShiftIndex()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("班次9独立窗口缺少目标生产槽"));
        return new DayScheduleContext(targetBusinessDate, Collections.singletonList(targetShift), false, true);
    }

    private String resolveIsolatedUnscheduledReason(
            LhScheduleContext context,
            SkuScheduleDTO sku) {
        return context.getUnscheduledResultList().stream()
                .filter(Objects::nonNull)
                .filter(item -> StringUtils.equals(
                        sku.getMaterialCode(), item.getMaterialCode()))
                .filter(item -> StringUtils.equals(
                        StringUtils.defaultIfEmpty(
                                sku.getProductStatus(), FORMAL_PRODUCT_STATUS),
                        StringUtils.defaultIfEmpty(
                                item.getProductStatus(), FORMAL_PRODUCT_STATUS)))
                .map(item -> item.getUnscheduledReason())
                .filter(StringUtils::isNotEmpty)
                .findFirst()
                .orElse(null);
    }

    /**
     * 构建与原排程状态隔离的工作上下文。
     */
    private LhScheduleContext buildIsolatedContext(
            LhScheduleContext sourceContext,
            LhShiftConfigVO sourceEndingShift,
            LhShiftConfigVO businessTargetShift,
            LocalDate targetBusinessDate,
            Map<String, Date> releaseTimeMap,
            List<SkuScheduleDTO> remainingSkuList) {
        LhScheduleContext isolatedContext = new LhScheduleContext();
        BeanUtil.copyProperties(sourceContext, isolatedContext);
        // 扩展窗口只能写独立副本的停机索引，不能污染原8班已经完成的回填范围。
        isolatedContext.setPlannedRepairSourcePlanMap(new LinkedHashMap<>(sourceContext.getPlannedRepairSourcePlanMap()));
        isolatedContext.setTemporaryFaultWindowMap(new LinkedHashMap<>(sourceContext.getTemporaryFaultWindowMap()));
        // 快照恢复会重建全部可变资源；先切断机台Map引用，避免恢复时清理原上下文。
        isolatedContext.setMachineScheduleMap(new LinkedHashMap<String, MachineScheduleDTO>());
        ScheduleSubstitutionAttemptSnapshot.capture(
                sourceContext, sourceContext.getNextShiftNewPlanCandidateList())
                .copyResourcesTo(isolatedContext);
        // 资源快照只复制可变资源；其余可写运行视图在独立服务中显式切断引用。
        this.detachIndependentViews(isolatedContext, remainingSkuList);
        isolatedContext.setIsolatedNextShiftPlan(true);
        // 只在独立副本使用标准机台分组与动态竞争，原上下文仍停留在最终保存阶段。
        isolatedContext.setCurrentStep(ScheduleStepEnum.S4_5_NEW_PRODUCTION.getCode());

        LhShiftConfigVO preparationSlot = this.copyShift(sourceEndingShift, PREPARATION_SLOT_INDEX);
        LhShiftConfigVO targetSlot = this.copyShift(businessTargetShift, TARGET_SLOT_INDEX);
        isolatedContext.setScheduleWindowShifts(
                new ArrayList<LhShiftConfigVO>(java.util.Arrays.asList(preparationSlot, targetSlot)));
        isolatedContext.setWindowEndDate(businessTargetShift.getWorkDate());
        isolatedContext.setCurrentScheduleDate(
                Date.from(targetBusinessDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        // 独立班次仅复制已提交首班，不让空结果列表重新初始化同一次结构切换。
        Map<String, StructureSwitchRuntimeState> switchBaseline = new LinkedHashMap<>(16);
        if (StructureSwitchSchedulingPolicy.isEnabled(sourceContext)) {
            sourceContext.getStructureSwitchRuntimeMap().forEach((structure, state) -> switchBaseline.put(structure,
                    StructureSwitchSchedulingPolicy.withAdjacentShift(state, sourceEndingShift, businessTargetShift)));
        }
        isolatedContext.setStructureSwitchBaselineRuntimeMap(Collections.unmodifiableMap(switchBaseline));
        isolatedContext.setStructureSwitchRuntimeMap(new LinkedHashMap<>(switchBaseline));
        isolatedContext.setStructureSwitchResultPlanMap(new IdentityHashMap<>());
        isolatedContext.setStructureSwitchAttemptPlanMap(new LinkedHashMap<>(4));
        isolatedContext.setScheduleResultList(new ArrayList<LhScheduleResult>());
        isolatedContext.setScheduleResultSourceSkuMap(
                new IdentityHashMap<LhScheduleResult, SkuScheduleDTO>());
        isolatedContext.setMachineAssignmentMap(new LinkedHashMap<String, List<LhScheduleResult>>());
        isolatedContext.setUnscheduledResultList(new ArrayList<>());
        // 班次9未排诊断运行态必须与原8班完全隔离，禁止反向清理或扩大原窗口分类。
        isolatedContext.setUnscheduledResultRuntime(new UnscheduledResultRuntime());
        isolatedContext.setMouldChangePlanList(new ArrayList<>());
        isolatedContext.setScheduleLogList(new ArrayList<>());
        isolatedContext.setMachineShiftCapacityMap(new LinkedHashMap<String, int[]>());

        releaseTimeMap.forEach((machineCode, releaseTime) -> {
            MachineScheduleDTO machine = isolatedContext.getMachineScheduleMap().get(machineCode);
            if (Objects.nonNull(machine)) {
                machine.setEstimatedEndTime(releaseTime);
                machine.setEnding(true);
            }
        });
        isolatedContext.setNewSpecMachineResourceScopeCodeSet(
                new LinkedHashSet<String>(releaseTimeMap.keySet()));
        isolatedContext.setNewSpecSkuList(new ArrayList<SkuScheduleDTO>(remainingSkuList));
        isolatedContext.setStructureSkuMap(this.buildStructureSkuMap(remainingSkuList));
        isolatedContext.setStructureMinMachineSkuSnapshotMap(
                this.buildStructureSkuMap(remainingSkuList));
        isolatedContext.setAllSkuScheduleDtoMap(remainingSkuList.stream()
                .collect(Collectors.toMap(
                        sku -> MonthPlanDateResolver.buildMaterialStatusKey(
                                sku.getMaterialCode(), sku.getProductStatus()),
                        sku -> sku,
                        (left, right) -> left,
                        LinkedHashMap::new)));
        isolatedContext.setNewSpecTypeRuleBlockedMap(new IdentityHashMap<SkuScheduleDTO, Boolean>());
        isolatedContext.setNewSpecEarlyProductionAllowedMap(
                new IdentityHashMap<SkuScheduleDTO, Boolean>());
        isolatedContext.setNewSpecSingleControlStructureEndingLayerMap(
                new IdentityHashMap<SkuScheduleDTO, Boolean>());
        // 保留只允许提前生产等身份，清除旧业务日激活状态后重新执行班次9准入。
        this.remapEarlyProductionViews(isolatedContext,
                sourceContext.getNextShiftNewPlanCandidateList(), remainingSkuList);
        isolatedContext.setSkuProductionRemainingQtyMap(
                this.buildRemainingQtyMap(remainingSkuList));
        isolatedContext.setSkuProductionTargetQtyMap(
                this.buildRemainingQtyMap(remainingSkuList));
        // 班次9开产下限由独立类型门禁控制，不能伪造胎胚可供时间参与T+4提前准入。
        isolatedContext.setStructureEarliestLhTimeMap(
                new LinkedHashMap<>(sourceContext.getStructureEarliestLhTimeMap()));
        this.extendDevicePlanWindow(isolatedContext, preparationSlot, targetSlot);
        this.extendWorkCalendar(isolatedContext, targetSlot);
        // 槽位8已代表班次9，不能沿用原班次8的结束时间；按目标真实日历重建副本管控。
        // 沿用原已解析开停产绝对时刻，不重跑原8班初始化，也不清空约束换取可排资格。
        isolatedContext.setShiftProductionControlMap(
                productionShutdownStrategy.buildShiftControlMap(isolatedContext));
        LhScheduleTimeUtil.initShiftRuntimeStateMap(
                isolatedContext, isolatedContext.getScheduleWindowShifts());
        isolatedContext.setMouldResourceContext(MouldResourceContext.from(isolatedContext));
        return isolatedContext;
    }

    /**
     * 将已深拷贝的提前生产视图绑定到候选副本，保留跨月身份但不复用旧日准入结果。
     *
     * @param context 独立上下文，运行视图已由资源快照深拷贝
     * @param sourceSkus 最终登记的原候选身份
     * @param skuList 候选副本
     */
    private void remapEarlyProductionViews(LhScheduleContext context,
                                           List<SkuScheduleDTO> sourceSkus,
                                           List<SkuScheduleDTO> skuList) {
        Map<String, EarlyProductionRuntimePlan> sourceViews = new LinkedHashMap<>(skuList.size());
        sourceSkus.forEach(sku -> {
            EarlyProductionRuntimePlan plan = context.getEarlyProductionRuntimePlanMap().get(sku);
            if (Objects.nonNull(sku) && Objects.nonNull(plan)) {
                sourceViews.put(MonthPlanDateResolver.buildMaterialStatusKey(
                        sku.getMaterialCode(), sku.getProductStatus()), plan);
            }
        });
        Map<SkuScheduleDTO, EarlyProductionRuntimePlan> copiedViews = new IdentityHashMap<>(skuList.size());
        skuList.forEach(sku -> {
            EarlyProductionRuntimePlan plan = sourceViews.get(MonthPlanDateResolver.buildMaterialStatusKey(
                    sku.getMaterialCode(), sku.getProductStatus()));
            if (Objects.nonNull(plan)) {
                plan.setActive(false);
                plan.setCurrentDate(null);
                plan.setShiftedDailyPlanQuotaMap(new LinkedHashMap<>(0));
                copiedViews.put(sku, plan);
            }
        });
        context.setEarlyProductionRuntimePlanMap(copiedViews);
    }

    /**
     * 切断资源快照以外的可写视图及原结果、原SKU引用。
     *
     * @param context 已复制资源的独立上下文
     * @param skuList 已深拷贝日计划和模具列表的班次9候选
     */
    private void detachIndependentViews(LhScheduleContext context, List<SkuScheduleDTO> skuList) {
        context.setPreScheduledMachineBindingList(new ArrayList<>(0));
        context.setPreScheduledMouldReleaseTimeMap(new LinkedHashMap<>(0));
        context.setNextShiftNewPlanCandidateList(new ArrayList<>(skuList));
        context.setNextShiftNewPlanPoolDateMap(new LinkedHashMap<>(context.getNextShiftNewPlanPoolDateMap()));
        context.setContinuousSkuList(new ArrayList<>(0));
        context.setTrialVirtualMachineCandidateList(new ArrayList<>(0));
        context.setInitialMachineScheduleMap(new LinkedHashMap<>(context.getInitialMachineScheduleMap()));
        context.setSingleControlInitialTargetQtyMap(new LinkedHashMap<>(context.getSingleControlInitialTargetQtyMap()));
        context.setSingleControlModeSnapshotMap(new LinkedHashMap<>(context.getSingleControlModeSnapshotMap()));
        context.setEmbryoRealtimeStockMap(new LinkedHashMap<>(context.getEmbryoRealtimeStockMap()));
        context.setMaterialSharedEmbryoMap(new LinkedHashMap<>(context.getMaterialSharedEmbryoMap()));
        context.setCarryForwardQtyMap(new LinkedHashMap<>(context.getCarryForwardQtyMap()));
        context.setEarlyProductionDecisionLogCollectorMap(new LinkedHashMap<>(4));
        context.setStructureEarlyProductionAdmissionMap(new LinkedHashMap<>(4));
        context.setStructureShiftInMachineIndex(null);
        context.setDayTypeBlockReverseSelectionDirectiveList(new ArrayList<>(0));
        context.setDayTypeBlockReverseSelectedSkuKeyMap(new LinkedHashMap<>(0));
        context.setSpecialMaterialContinuationResultSnapshot(new LinkedHashSet<>(0));
        context.setSpecialMaterialSubstitutionRecordList(new ArrayList<>(0));
        context.setSharedMouldSubstitutionRecordList(new ArrayList<>(0));
        context.setScheduleSubstitutionDirective(null);
        context.setSpecialMaterialSpecifiedMachineCode(null);
        context.setSpecialMaterialSpecifiedSkuKey(null);
        context.setSpecialMaterialEarliestSwitchTime(null);
        context.setNewSpecRealtimeSelectionOrderMap(new IdentityHashMap<>(16));
        context.setNewSpecRealtimeSnapshotResultSet(new LinkedHashSet<>(0));
        context.setSharedEmbryoEndingStaggerReleaseShiftIndexMap(new IdentityHashMap<>(0));
        context.setSharedEmbryoEndingStaggerReleaseShiftQtyMap(new IdentityHashMap<>(0));
        context.setSharedEmbryoEndingStaggerAllowedOverQtyMap(new IdentityHashMap<>(0));
        context.setEndingFillAllowedOverQtyMap(new IdentityHashMap<>(0));
        context.setEndingFillBeforeQtyMap(new IdentityHashMap<>(0));
        context.setPrecisionPreInsertResultSet(new LinkedHashSet<>(0));
        context.setPrecisionPreInsertInspectionTimeMap(new IdentityHashMap<>(0));
        context.setPrecisionPreInsertMouldChangeTimeMap(new IdentityHashMap<>(0));
        context.setPrecisionPreInsertInspectionShiftIndexMap(new IdentityHashMap<>(0));
        context.setWarningMessageList(new ArrayList<>(0));
        context.setValidationErrorList(new ArrayList<>(0));
        context.setValidationErrorDetailList(new ArrayList<>(0));
    }

    /**
     * 解析班次8内真实收尾且已释放的机台。
     */
    private Map<String, Date> resolveReleasedMachineTimeMap(
            LhScheduleContext context,
            LhShiftConfigVO sourceEndingShift) {
        Map<String, Date> releaseTimeMap = new LinkedHashMap<String, Date>();
        Map<String, LhScheduleResult> latestResultMap =
                new LinkedHashMap<String, LhScheduleResult>();
        Set<String> unresolvedMachineCodes = new LinkedHashSet<>(16);
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (Objects.isNull(result) || StringUtils.isEmpty(result.getLhMachineCode())) {
                continue;
            }
            Integer sourceShiftQty = ShiftFieldUtil.getShiftPlanQty(
                    result, SOURCE_ENDING_SHIFT_INDEX);
            Date endingTime = this.resolveResultEndingTime(result);
            if (Objects.isNull(sourceShiftQty) || sourceShiftQty <= 0) {
                continue;
            }
            if (Objects.isNull(endingTime)) {
                unresolvedMachineCodes.add(result.getLhMachineCode());
                continue;
            }
            latestResultMap.merge(
                    result.getLhMachineCode(), result,
                    (current, candidate) -> {
                        Date currentEndingTime = this.resolveResultEndingTime(current);
                        Date candidateEndingTime = this.resolveResultEndingTime(candidate);
                        return candidateEndingTime.before(currentEndingTime)
                                ? current : candidate;
                    });
        }
        for (LhScheduleResult result : latestResultMap.values()) {
            if (unresolvedMachineCodes.contains(result.getLhMachineCode())
                    || !this.isReleasedInSourceShift(context, result, sourceEndingShift)) {
                continue;
            }
            Date endingTime = this.resolveResultEndingTime(result);
            releaseTimeMap.put(result.getLhMachineCode(), endingTime);
            log.info("班次9候选机台, machineCode: {}, materialCode: {}, productStatus: {}, "
                            + "sourceShift: class8, actualEndingTime: {}",
                    result.getLhMachineCode(), result.getMaterialCode(), result.getProductStatus(),
                    LhScheduleTimeUtil.formatDateTime(endingTime));
        }
        return releaseTimeMap;
    }

    /**
     * 核对最终收尾标记和真实结束时刻，窗口截断不能授予机台释放资格。
     *
     * @param context 原8班最终上下文
     * @param result 机台最后一条班次8有量结果
     * @param sourceEndingShift 原班次8时段
     * @return 是否在班次8内真实释放
     */
    private boolean isReleasedInSourceShift(
            LhScheduleContext context,
            LhScheduleResult result,
            LhShiftConfigVO sourceEndingShift) {
        if (Objects.isNull(result) || StringUtils.isEmpty(result.getLhMachineCode())
                || result.getLhMachineCode().startsWith(VIRTUAL_MACHINE_PREFIX)
                || !this.isRealMachine(context, result.getLhMachineCode())
                || !StringUtils.equals("1", result.getIsEnd())
                || this.resolveLastPositiveShiftIndex(result) != SOURCE_ENDING_SHIFT_INDEX
                || !StringUtils.equals("1", ShiftFieldUtil.getShiftIsEnd(
                result, SOURCE_ENDING_SHIFT_INDEX))) {
            return false;
        }
        String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                result.getMaterialCode(), result.getProductStatus());
        Integer remainingQty = context.getSkuProductionRemainingQtyMap().get(skuKey);
        if (Objects.nonNull(remainingQty) && remainingQty > 0) {
            return false;
        }
        Date endingTime = this.resolveResultEndingTime(result);
        return Objects.nonNull(endingTime)
                && !endingTime.before(sourceEndingShift.getShiftStartDateTime())
                && endingTime.before(sourceEndingShift.getShiftEndDateTime());
    }

    private boolean isRealMachine(LhScheduleContext context, String machineCode) {
        if (context.getMachineInfoMap().containsKey(machineCode)) {
            return true;
        }
        String physicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        return context.getMachineInfoMap().containsKey(physicalMachineCode);
    }

    /**
     * 从最终三个日期池提取实时剩余SKU，并复制本服务需要修改的日计划与模具列表。
     *
     * @param context 全部前置排产完成后的原上下文
     * @param targetBusinessDate 班次9业务日T+3
     * @return 按原身份顺序保留的候选副本，后续由日期池分别排序
     */
    private List<SkuScheduleDTO> resolveRemainingSkuList(
            LhScheduleContext context,
            LocalDate targetBusinessDate) {
        Map<String, SkuScheduleDTO> candidateMap = new LinkedHashMap<String, SkuScheduleDTO>();
        // T以引擎窗口起点为准，保存日期和晚班自然开始日期均不能替代。
        LocalDate baseDate = this.toLocalDate(context.getScheduleDate());
        for (SkuScheduleDTO sourceSku : context.getNextShiftNewPlanCandidateList()) {
            if (Objects.isNull(sourceSku) || StringUtils.isEmpty(sourceSku.getMaterialCode())) {
                continue;
            }
            String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                    sourceSku.getMaterialCode(), sourceSku.getProductStatus());
            LocalDate poolDate = context.getNextShiftNewPlanPoolDateMap().get(skuKey);
            if (Objects.isNull(poolDate) || poolDate.isBefore(baseDate.plusDays(FIRST_POOL_DAY_OFFSET))
                    || poolDate.isAfter(baseDate.plusDays(LAST_POOL_DAY_OFFSET))) {
                continue;
            }
            int remainingQty = this.resolveFinalRemainingQty(context, sourceSku, skuKey);
            if (remainingQty <= 0) {
                log.info("班次9剩余SKU过滤, materialCode: {}, productStatus: {}, "
                                + "reason: 前置真实或虚拟机台已排完",
                        sourceSku.getMaterialCode(), sourceSku.getProductStatus());
                continue;
            }
            SkuScheduleDTO copiedSku = this.copySku(
                    context, sourceSku, targetBusinessDate);
            copiedSku.setSurplusQty(remainingQty);
            copiedSku.setPendingQty(remainingQty);
            copiedSku.setTargetScheduleQty(remainingQty);
            copiedSku.setRemainingScheduleQty(remainingQty);
            candidateMap.putIfAbsent(skuKey, copiedSku);
            log.info("班次9剩余SKU候选, materialCode: {}, productStatus: {}, "
                            + "poolDate: {}, constructionStage: {}, embryoCode: {}, remainingQty: {}",
                    copiedSku.getMaterialCode(), copiedSku.getProductStatus(), poolDate,
                    copiedSku.getConstructionStage(), copiedSku.getEmbryoCode(), remainingQty);
        }
        return new ArrayList<SkuScheduleDTO>(candidateMap.values());
    }

    /**
     * 读取前置最终生产余量，已存在的账本不再重复扣真实或虚拟结果。
     *
     * @param context 原8班最终上下文，只读
     * @param sku 最新候选SKU
     * @param skuKey 物料和产品状态键
     * @return 前置排产后的非负可排余量
     */
    private int resolveFinalRemainingQty(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            String skuKey) {
        Integer ledgerRemainingQty = context.getSkuProductionRemainingQtyMap().get(skuKey);
        if (Objects.nonNull(ledgerRemainingQty)) {
            return Math.max(0, ledgerRemainingQty);
        }
        // 未初始化过生产账本的候选沿用中心初始化口径；传空上下文只读计算，不懒加载原账本。
        int targetQty = targetScheduleQtyResolver.resolveProductionRemainingQty(null, sku);
        int scheduledQty = context.getScheduleResultList().stream()
                .filter(Objects::nonNull)
                .filter(result -> StringUtils.equals(sku.getMaterialCode(), result.getMaterialCode()))
                .filter(result -> StringUtils.equals(
                        StringUtils.defaultIfEmpty(
                                sku.getProductStatus(), FORMAL_PRODUCT_STATUS),
                        StringUtils.defaultIfEmpty(
                                result.getProductStatus(), FORMAL_PRODUCT_STATUS)))
                .mapToInt(ShiftFieldUtil::resolveScheduledQty)
                .sum();
        return Math.max(0, targetQty - scheduledQty);
    }

    private SkuScheduleDTO copySku(
            LhScheduleContext sourceContext,
            SkuScheduleDTO sourceSku,
            LocalDate targetBusinessDate) {
        SkuScheduleDTO copiedSku = new SkuScheduleDTO();
        BeanUtil.copyProperties(sourceSku, copiedSku);
        Map<LocalDate, SkuDailyPlanQuotaDTO> copiedQuotaMap =
                new LinkedHashMap<LocalDate, SkuDailyPlanQuotaDTO>();
        if (!CollectionUtils.isEmpty(sourceSku.getDailyPlanQuotaMap())) {
            sourceSku.getDailyPlanQuotaMap().forEach((productionDate, quota) -> {
                if (Objects.isNull(quota)) {
                    return;
                }
                SkuDailyPlanQuotaDTO copiedQuota = new SkuDailyPlanQuotaDTO();
                BeanUtil.copyProperties(quota, copiedQuota);
                copiedQuotaMap.put(productionDate, copiedQuota);
            });
        }
        if (!copiedQuotaMap.containsKey(targetBusinessDate)) {
            int targetDayPlanQty = MonthPlanDateResolver.resolveDayQty(
                    sourceContext, sourceSku.getMaterialCode(),
                    sourceSku.getProductStatus(), targetBusinessDate);
            SkuDailyPlanQuotaDTO targetQuota = new SkuDailyPlanQuotaDTO();
            targetQuota.setMaterialCode(sourceSku.getMaterialCode());
            targetQuota.setProductionDate(targetBusinessDate);
            targetQuota.setDayPlanQty(targetDayPlanQty);
            targetQuota.setRemainingQty(targetDayPlanQty);
            targetQuota.setCompleted(targetDayPlanQty <= 0);
            copiedQuotaMap.put(targetBusinessDate, targetQuota);
        }
        copiedSku.setDailyPlanQuotaMap(copiedQuotaMap);
        if (Objects.nonNull(sourceSku.getMouldCodeList())) {
            copiedSku.setMouldCodeList(new ArrayList<>(sourceSku.getMouldCodeList()));
        }
        return copiedSku;
    }

    private Map<String, List<SkuScheduleDTO>> buildStructureSkuMap(
            List<SkuScheduleDTO> skuList) {
        return skuList.stream()
                .filter(Objects::nonNull)
                .filter(sku -> StringUtils.isNotEmpty(sku.getStructureName()))
                .collect(Collectors.groupingBy(
                        SkuScheduleDTO::getStructureName,
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private Map<String, Integer> buildRemainingQtyMap(List<SkuScheduleDTO> skuList) {
        Map<String, Integer> remainingQtyMap = new LinkedHashMap<String, Integer>();
        skuList.stream()
                .filter(Objects::nonNull)
                .forEach(sku -> remainingQtyMap.put(
                        MonthPlanDateResolver.buildMaterialStatusKey(
                                sku.getMaterialCode(), sku.getProductStatus()),
                        Math.max(0, sku.getRemainingScheduleQty())));
        return remainingQtyMap;
    }

    /** 扩展设备计划仍复用05/06的统一分流。 */
    private void extendDevicePlanWindow(
            LhScheduleContext context,
            LhShiftConfigVO preparationShift,
            LhShiftConfigVO targetShift) {
        List<MdmDevicePlanShut> additionalPlanList = devicePlanShutMapper.selectList(
                new LambdaQueryWrapper<MdmDevicePlanShut>()
                        .eq(MdmDevicePlanShut::getFactoryCode, context.getFactoryCode())
                        .ge(MdmDevicePlanShut::getBeginDate, preparationShift.getShiftStartDateTime())
                        .le(MdmDevicePlanShut::getBeginDate, targetShift.getShiftEndDateTime()));
        Map<String, MdmDevicePlanShut> planMap = new LinkedHashMap<String, MdmDevicePlanShut>();
        List<MdmDevicePlanShut> sourcePlanList = CollectionUtils.isEmpty(context.getDevicePlanShutList())
                ? Collections.emptyList() : context.getDevicePlanShutList();
        for (MdmDevicePlanShut plan : sourcePlanList) {
            planMap.put(this.buildDevicePlanKey(plan), plan);
        }
        for (MdmDevicePlanShut plan : additionalPlanList) {
            planMap.putIfAbsent(this.buildDevicePlanKey(plan), plan);
        }
        // 班次9与主排程使用相同维修转换和故障隔离，禁止原始06重新进入普通业务列表。
        context.setDevicePlanShutList(deviceStopPlanScheduleService.prepareStopPlans(
                context, new ArrayList<MdmDevicePlanShut>(planMap.values())));
        log.info("班次9设备计划加载完成, factoryCode: {}, targetWindow: [{}, {}), "
                        + "additionalCount: {}, mergedCount: {}",
                context.getFactoryCode(),
                LhScheduleTimeUtil.formatDateTime(preparationShift.getShiftStartDateTime()),
                LhScheduleTimeUtil.formatDateTime(targetShift.getShiftEndDateTime()),
                additionalPlanList.size(), planMap.size());
    }

    /**
     * 补充班次9实际业务日期的工作日历，避免沿用原窗口末日的开停产状态。
     *
     * @param context 班次9独立上下文
     * @param targetShift 班次9计算槽位
     * @return void
     */
    private void extendWorkCalendar(
            LhScheduleContext context,
            LhShiftConfigVO targetShift) {
        Date targetWorkDate = targetShift.getWorkDate();
        Date nextWorkDate = LhScheduleTimeUtil.addDays(targetWorkDate, 1);
        List<MdmWorkCalendar> additionalCalendarList = workCalendarMapper.selectList(
                new LambdaQueryWrapper<MdmWorkCalendar>()
                        .eq(MdmWorkCalendar::getFactoryCode, context.getFactoryCode())
                        .eq(MdmWorkCalendar::getProcCode, LhScheduleConstant.PROC_CODE_LH)
                        .ge(MdmWorkCalendar::getProductionDate, targetWorkDate)
                        .lt(MdmWorkCalendar::getProductionDate, nextWorkDate));
        Map<String, MdmWorkCalendar> calendarMap = new LinkedHashMap<String, MdmWorkCalendar>();
        List<MdmWorkCalendar> sourceCalendarList = CollectionUtils.isEmpty(context.getWorkCalendarList())
                ? Collections.emptyList() : context.getWorkCalendarList();
        for (MdmWorkCalendar calendar : sourceCalendarList) {
            calendarMap.put(this.buildWorkCalendarKey(calendar), calendar);
        }
        for (MdmWorkCalendar calendar : additionalCalendarList) {
            calendarMap.putIfAbsent(this.buildWorkCalendarKey(calendar), calendar);
        }
        context.setWorkCalendarList(new ArrayList<MdmWorkCalendar>(calendarMap.values()));
        log.info("班次9工作日历加载完成, factoryCode: {}, targetBusinessDate: {}, "
                        + "additionalCount: {}, mergedCount: {}",
                context.getFactoryCode(), LhScheduleTimeUtil.formatDate(targetWorkDate),
                additionalCalendarList.size(), calendarMap.size());
    }

    private String buildDevicePlanKey(MdmDevicePlanShut plan) {
        if (Objects.nonNull(plan.getId())) {
            return String.valueOf(plan.getId());
        }
        return new StringBuilder(128)
                .append(plan.getFactoryCode()).append('|')
                .append(plan.getMachineCode()).append('|')
                .append(plan.getMachineStopType()).append('|')
                .append(plan.getBeginDate()).append('|')
                .append(plan.getEndDate())
                .toString();
    }

    private String buildWorkCalendarKey(MdmWorkCalendar calendar) {
        if (Objects.nonNull(calendar.getId())) {
            return String.valueOf(calendar.getId());
        }
        return new StringBuilder(96)
                .append(calendar.getFactoryCode()).append('|')
                .append(calendar.getProcCode()).append('|')
                .append(calendar.getProductionDate())
                .toString();
    }

    /**
     * 转换班次9实际结果，保留原保存范围并同步状态和工艺参数。
     *
     * @param context 独立上下文，沿用原工厂、保存日期和批次
     * @param result 班次9实际新排结果
     * @param targetQty 班次9生产槽实际计划量
     * @return 仅供独立表持久化的计划行
     */
    private LhNextShiftNewPlan buildNextShiftPlan(
            LhScheduleContext context,
            LhScheduleResult result,
            int targetQty) {
        LhNextShiftNewPlan plan = new LhNextShiftNewPlan();
        plan.setFactoryCode(context.getFactoryCode());
        plan.setScheduleDate(LhScheduleTimeUtil.clearTime(context.getScheduleTargetDate()));
        plan.setBatchNo(context.getBatchNo());
        plan.setMaterialCode(result.getMaterialCode());
        plan.setProductStatus(result.getProductStatus());
        plan.setConstructionStage(result.getConstructionStage());
        plan.setEmbryoCode(result.getEmbryoCode());
        plan.setStructureName(result.getStructureName());
        plan.setShiftPlanQty(BigDecimalUtils.valueOf(targetQty));
        // 工艺参数必须取班次9实际新结果，不能沿用班次8前序物料参数。
        plan.setLhTime(result.getLhTime());
        plan.setMouldQty(result.getMouldQty());
        plan.setSingleMouldShiftQty(result.getSingleMouldShiftQty());
        plan.setMachineCode(result.getLhMachineCode());
        plan.setRemark(NEXT_SHIFT_PLAN_REMARK);
        return plan;
    }

    private LhShiftConfigVO resolveSourceEndingShift(LhScheduleContext context) {
        return context.getScheduleWindowShifts().stream()
                .filter(Objects::nonNull)
                .filter(shift -> Objects.equals(
                        SOURCE_ENDING_SHIFT_INDEX, shift.getShiftIndex()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("原8班排程窗口缺少班次8"));
    }

    private LhShiftConfigVO copyShift(LhShiftConfigVO sourceShift, int shiftIndex) {
        LhShiftConfigVO copiedShift = new LhShiftConfigVO();
        BeanUtil.copyProperties(sourceShift, copiedShift);
        copiedShift.setScheduleBaseDate(sourceShift.getScheduleBaseDate());
        copiedShift.setShiftIndex(shiftIndex);
        return copiedShift;
    }

    private Date resolveResultEndingTime(LhScheduleResult result) {
        // 最终班次时间是实际释放证据，缺失时不得用窗口级结束时间代替。
        return ShiftFieldUtil.getShiftEndTime(result, SOURCE_ENDING_SHIFT_INDEX);
    }

    private int resolveLastPositiveShiftIndex(LhScheduleResult result) {
        for (int shiftIndex = LhScheduleConstant.MAX_SHIFT_SLOT_COUNT;
             shiftIndex >= 1; shiftIndex--) {
            Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
            if (Objects.nonNull(planQty) && planQty > 0) {
                return shiftIndex;
            }
        }
        return -1;
    }

    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
