/**
 * Copyright (c) 2008, 智立通（厦门）科技有限公司 All rights reserved。
 */
package com.zlt.aps.lh.engine.strategy.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.constant.LhScheduleParamConstant;
import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineMaintenanceWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.ShiftProductionControlDTO;
import com.zlt.aps.lh.api.domain.dto.ShiftRuntimeState;
import com.zlt.aps.lh.api.domain.dto.SkuDailyPlanQuotaDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SpecialMaterialMatchResult;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.entity.LhUnscheduledResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.ConstructionStageEnum;
import com.zlt.aps.lh.api.enums.MachineStopTypeEnum;
import com.zlt.aps.lh.api.enums.MouldChangeTypeEnum;
import com.zlt.aps.lh.api.enums.NewSpecFailReasonEnum;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.api.enums.ShiftEnum;
import com.zlt.aps.lh.api.enums.SkuScheduleSourceTypeEnum;
import com.zlt.aps.lh.api.enums.SkuTagEnum;
import com.zlt.aps.lh.component.CapsuleReplacementRuleService;
import com.zlt.aps.lh.component.EarlyProductionQuantityCalculator;
import com.zlt.aps.lh.component.EarlyProductionRuntimePlanService;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.component.OrderNoGenerator;
import com.zlt.aps.lh.component.SkuDecrementChecker;
import com.zlt.aps.lh.component.StructureEndingAlignmentDecision;
import com.zlt.aps.lh.component.StructureEndingAlignmentService;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.context.LhScheduleConfig;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.ICapacityCalculateStrategy;
import com.zlt.aps.lh.engine.strategy.IEndingJudgmentStrategy;
import com.zlt.aps.lh.engine.strategy.IFirstInspectionBalanceStrategy;
import com.zlt.aps.lh.engine.strategy.IMachineMatchStrategy;
import com.zlt.aps.lh.engine.strategy.IMouldChangeBalanceStrategy;
import com.zlt.aps.lh.engine.strategy.IProductionStrategy;
import com.zlt.aps.lh.engine.strategy.ISkuPriorityStrategy;
import com.zlt.aps.lh.engine.strategy.ITrialProductionStrategy;
import com.zlt.aps.lh.engine.strategy.ITypeBlockProductionStrategy;
import com.zlt.aps.lh.engine.strategy.support.ActiveMachineBinding;
import com.zlt.aps.lh.engine.strategy.support.DailyCandidateReason;
import com.zlt.aps.lh.engine.strategy.support.DailyMachineExpansionPlanner;
import com.zlt.aps.lh.engine.strategy.support.DailyNewSpecCandidate;
import com.zlt.aps.lh.engine.strategy.support.DailyNewSpecOrderLogCollector;
import com.zlt.aps.lh.engine.strategy.support.DailyNewSpecOrderLogEntry;
import com.zlt.aps.lh.engine.strategy.support.DailyQuotaLedgerBaseline;
import com.zlt.aps.lh.engine.strategy.support.DailySchedulePhase;
import com.zlt.aps.lh.engine.strategy.support.DayTypeBlockReverseSelectionDirective;
import com.zlt.aps.lh.engine.strategy.support.DayDrivenScheduleState;
import com.zlt.aps.lh.engine.strategy.support.DayScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.DeferredScheduleTask;
import com.zlt.aps.lh.engine.strategy.support.EarlyProductionChecker;
import com.zlt.aps.lh.engine.strategy.support.EarlyProductionDecision;
import com.zlt.aps.lh.engine.strategy.support.EarlyProductionRuntimePlan;
import com.zlt.aps.lh.engine.strategy.support.HistoricalReverseSelectionDirective;
import com.zlt.aps.lh.engine.strategy.support.HistoricalResidualCapacityInfo;
import com.zlt.aps.lh.engine.strategy.support.FirstInspectionAllocationPlan;
import com.zlt.aps.lh.engine.strategy.support.FirstInspectionShiftAllocation;
import com.zlt.aps.lh.engine.strategy.support.MachineProductionSegment;
import com.zlt.aps.lh.engine.strategy.support.MachinePriorityMetricSnapshot;
import com.zlt.aps.lh.engine.strategy.support.MachinePriorityTraceSnapshot;
import com.zlt.aps.lh.engine.strategy.support.MachineSelectionDescriptionFormatter;
import com.zlt.aps.lh.engine.strategy.support.MachineScheduleRole;
import com.zlt.aps.lh.engine.strategy.support.MouldResourceAllocationResult;
import com.zlt.aps.lh.engine.strategy.support.MouldResourceContext;
import com.zlt.aps.lh.engine.strategy.support.NewSpecCandidateCache;
import com.zlt.aps.lh.engine.strategy.support.NewSpecEmbryoAvailableTimeResolver;
import com.zlt.aps.lh.engine.strategy.support.NewSpecMachineAvailabilityPlan;
import com.zlt.aps.lh.engine.strategy.support.NewSpecSelectionRealtimeSnapshot;
import com.zlt.aps.lh.engine.strategy.support.PendingSkuUnscheduledRule;
import com.zlt.aps.lh.engine.strategy.support.ProductionQuantityPolicy;
import com.zlt.aps.lh.engine.strategy.support.ScheduleResultBaseline;
import com.zlt.aps.lh.engine.strategy.support.SkuDayScheduleOutcome;
import com.zlt.aps.lh.engine.strategy.support.SmallEndingSurplusSkipRule;
import com.zlt.aps.lh.engine.strategy.support.SpecifiedMachineMatchResult;
import com.zlt.aps.lh.exception.ScheduleErrorCode;
import com.zlt.aps.lh.exception.ScheduleException;
import com.zlt.aps.lh.service.ILhDailyMouldCalcService;
import com.zlt.aps.lh.service.impl.LhMaintenanceScheduleService;
import com.zlt.aps.lh.util.CleaningScheduleRuleUtil;
import com.zlt.aps.lh.util.FirstInspectionQtyUtil;
import com.zlt.aps.lh.util.FirstInspectionAllocationUtil;
import com.zlt.aps.lh.util.TypeBlockRelationUtil;
import com.zlt.aps.lh.util.LeftRightMouldUtil;
import com.zlt.aps.lh.util.LhMachineHardMatchUtil;
import com.zlt.aps.lh.util.LhMultiMachineDistributionUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.LhSpecialMaterialUtil;
import com.zlt.aps.lh.util.MachineCleaningOverlapUtil;
import com.zlt.aps.lh.util.MachineStatusUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.lh.util.ResultDowntimeSummaryUtil;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.lh.util.ShiftProductionControlUtil;
import com.zlt.aps.lh.util.SingleMouldShiftQtyUtil;
import com.zlt.aps.lh.util.SkuDailyPlanQuotaUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuMouldRel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 新增规格排产策略实现。
 *
 * <p>业务定位：</p>
 * <ul>
 *   <li>处理 S4.5 新增 SKU 的选机、换模、首检、开产时间、班次分配和未排原因归集；</li>
 *   <li>支持同 SKU 单机台、多机台、尾量归集、非收尾补满、收尾严格控量和晚班不可换模衔接；</li>
 *   <li>消费 S4.3 初始化的日计划账本，并在胎胚库存裁剪后同步机台运行态；</li>
 *   <li>与机台匹配、换模均衡、首检均衡、目标量解析和局部搜索策略协作完成新增规格排产。</li>
 * </ul>
 *
 * <p>注意：本类方法较长且历史规则较多。维护时应优先局部补注释和小方法，不应改变排序、
 * 机台选择、日计划账本和收尾判断的既有语义。</p>
 *
 * @author APS
 */
@Slf4j
@Component("newSpecProductionStrategy")
public class NewSpecProductionStrategy implements IProductionStrategy {

    private static final String NEW_SPEC_SCHEDULE_TYPE = "02";
    private static final String AUTO_DATA_SOURCE = "0";
    /** 业务标识：是 */
    private static final String YES_FLAG = "1";
    /** 历史交替计划无产品状态时按正规状态归一化 */
    private static final String FORMAL_PRODUCT_STATUS = "S";
    /** 命中SKU减量清单的未排备注（与SkuDecrementChecker文案保持一致） */
    private static final String SKU_DECREMENT_UNSCHEDULED_REASON = "命中SKU减量清单，不进行排产";
    private static final String ZERO_PLAN_UNSCHEDULED_REASON = "新增结果裁剪为0";
    private static final String SHARED_EMBRYO_ZERO_SURPLUS_UNSCHEDULED_REASON =
            "共用胎胚且硫化余量为0";
    /** S4.3无排产目标量未排原因后缀（与ScheduleAdjustHandler模板保持一致，零量归并时按后缀识别保留） */
    private static final String NO_PLAN_QTY_UNSCHEDULED_REASON_SUFFIX = "没有排产目标量，不进行排产";
    /** S4.3余量与胎胚库存均为0未排原因后缀（与ScheduleAdjustHandler模板保持一致，零量归并时按后缀识别保留） */
    private static final String ZERO_SURPLUS_AND_EMBRYO_UNSCHEDULED_REASON_SUFFIX =
            "余量为0且胎胚库存为0，不需要排产";
    private static final String SMALL_ENDING_SURPLUS_UNSCHEDULED_REASON =
            SmallEndingSurplusSkipRule.UNSCHEDULED_REASON;
    private static final String TARGET_SKU_MOULD_ALL_OCCUPIED_UNSCHEDULED_REASON =
            "目标 SKU 模具全部被占用";
    /** 历史指定机台在真实排程窗口内已经没有可生产能力 */
    private static final String HISTORICAL_REVERSE_NO_WINDOW_CAPACITY_REASON =
            "历史指定机台在当前排程窗口无剩余产能";
    /** 提前生产部分成功后因当前结构及日计划机台节奏不再扩机的最终未排原因模板 */
    private static final String EARLY_PRODUCTION_PARTIAL_REMAINING_REASON_TEMPLATE =
            "提前生产已使用正常阶段后的剩余资源，按当前结构及日计划机台节奏不再扩机，"
                    + "剩余%d保留原计划日期";
    /** 结构计划机台数达到上限时，提前生产禁止新增物理机台的原因前缀。 */
    private static final String EARLY_PRODUCTION_STRUCTURE_MACHINE_LIMIT_REASON =
            "同结构计划硫化机台数已达上限，禁止提前生产";
    private static final int NEW_SPEC_CHANGEOVER_PROBE_LIMIT = 16;
    /** 日驱动新增排产固定覆盖 T、T+1、T+2 三个业务日。 */
    private static final int DAY_DRIVEN_SCHEDULE_DAY_COUNT = 3;
    /** 新增排产按日顺序日志合并后唯一标题。 */
    private static final String NEW_SPEC_ORDER_MERGED_LOG_TITLE = "新增排产明细";
    /** 普通新增候选执行“历史班次剩余产能优先”的过程日志标题。 */
    private static final String HISTORY_RESIDUAL_CAPACITY_PREFERENCE_LOG_TITLE =
            "新增SKU历史班次剩余产能优先选机";
    /** 历史班次剩余产能最多向当前目标班次之前回看一天。 */
    private static final int HISTORY_RESIDUAL_LOOKBACK_DAYS = 1;
    /** 新增排产按日顺序日志分节分隔符，相邻业务日之间保留一个空行。 */
    private static final String NEW_SPEC_ORDER_LOG_SECTION_SEPARATOR = "\n\n";
    /** T 日允许写入的班次索引。 */
    private static final List<Integer> DAY_DRIVEN_FIRST_DAY_SHIFT_INDEXES =
            Collections.unmodifiableList(Arrays.asList(1, 2));
    /** T+1 日允许写入的班次索引，夜班 class3 归属 T+1 业务日。 */
    private static final List<Integer> DAY_DRIVEN_SECOND_DAY_SHIFT_INDEXES =
            Collections.unmodifiableList(Arrays.asList(3, 4, 5));
    /** T+2 日允许写入的班次索引，夜班 class6 归属 T+2 业务日。 */
    private static final List<Integer> DAY_DRIVEN_THIRD_DAY_SHIFT_INDEXES =
            Collections.unmodifiableList(Arrays.asList(6, 7, 8));
    /** 日驱动窗口完整班次索引，用于拒绝缺班和重复班次的错误输入。 */
    private static final List<Integer> DAY_DRIVEN_ALL_SHIFT_INDEXES =
            Collections.unmodifiableList(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8));

    private static final Set<String> EMPTY_STRING_SET = Collections.emptySet();
    private static final Map<String, String> EMPTY_STRING_MAP = Collections.emptyMap();
    @Resource
    private OrderNoGenerator orderNoGenerator;
    @Resource
    private IEndingJudgmentStrategy endingJudgmentStrategy;
    @Resource
    private LocalSearchMachineAllocatorStrategy localSearchMachineAllocator;
    @Resource
    private TargetScheduleQtyResolver targetScheduleQtyResolver;
    /** S4.4 与 S4.5 共用的提前生产运行态计划入口。 */
    @Resource
    private EarlyProductionRuntimePlanService earlyProductionRuntimePlanService;
    @Resource
    private SkuDecrementChecker skuDecrementChecker;
    @Resource
    private LhMaintenanceScheduleService maintenanceScheduleService;
    /** 结构收尾对齐实时判断入口，新增选机候选校验、命中标识与在机缓存增量更新共用。 */
    @Resource
    private StructureEndingAlignmentService structureEndingAlignmentService;
    @Resource
    private ITrialProductionStrategy trialProductionStrategy;
    /** 当前业务日待排新增 SKU 的统一重排入口。 */
    @Resource
    private ISkuPriorityStrategy skuPriorityStrategy;
    /** 按天换活字块机台反选的公共匹配能力入口，匹配口径与 S4.4 换活字块主链完全一致。 */
    @Resource
    private ITypeBlockProductionStrategy typeBlockProductionStrategy;
    /** 物料+产品状态+自然日目标总机台数唯一查询入口。 */
    @Resource
    private ILhDailyMouldCalcService lhDailyMouldCalcService;
    /** 胶囊次数累计与换胶囊班次扣减统一入口 */
    @Resource
    private CapsuleReplacementRuleService capsuleReplacementRuleService = new CapsuleReplacementRuleService();

    @Override
    public String getStrategyType() {
        return ScheduleTypeEnum.NEW_SPEC.getCode();
    }

    @Override
    public String getStrategyName() {
        return "newSpecProductionStrategy";
    }

    @Override
    public void scheduleContinuousEnding(LhScheduleContext context) {
        // 新增策略不处理续作收尾，空实现
    }

    @Override
    public void allocateShiftPlanQty(LhScheduleContext context) {
        log.info("新增排产 - 班次计划量分配, 新增排程结果数: {}",
                context.getScheduleResultList().stream().filter(r -> NEW_SPEC_SCHEDULE_TYPE.equals(r.getScheduleType())).count());
        // 班次计划量已在scheduleNewSpecs中随生成结果时分配完毕，此处为空实现
    }

    @Override
    public void adjustEmbryoStock(LhScheduleContext context) {
        log.info("新增排产 - 胎胚库存调整");
        // 按物料编码汇总多机台排产量，再统一做库存裁剪（避免多机台场景下各机台独立比对导致总量超库存）
        Map<String, Integer> materialTotalPlanMap = new LinkedHashMap<>(16);
        Map<String, Integer> materialEmbryoStockMap = new LinkedHashMap<>(16);
        Map<String, List<LhScheduleResult>> materialResultMap = new LinkedHashMap<>(16);
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (!NEW_SPEC_SCHEDULE_TYPE.equals(result.getScheduleType())
                    || "1".equals(result.getIsTypeBlock())) {
                continue;
            }
            if (result.getEmbryoCode() == null) {
                continue;
            }
            SkuScheduleDTO sku = findSkuDto(
                    context, result.getMaterialCode(), result.getProductStatus());
            if (sku == null || sku.getEmbryoStock() < 0) {
                continue;
            }
            int planQty = ShiftFieldUtil.resolveScheduledQty(result);
            materialTotalPlanMap.merge(result.getMaterialCode(), planQty, Integer::sum);
            materialEmbryoStockMap.merge(result.getMaterialCode(), sku.getEmbryoStock(), Math::max);
            materialResultMap.computeIfAbsent(result.getMaterialCode(), key -> new ArrayList<LhScheduleResult>())
                    .add(result);
        }
        // 按汇总计划量统一裁剪同物料的所有结果
        List<LhShiftConfigVO> shifts = LhScheduleTimeUtil.getScheduleShifts(context, context.getScheduleDate());
        for (Map.Entry<String, List<LhScheduleResult>> entry : materialResultMap.entrySet()) {
            String materialCode = entry.getKey();
            int totalPlan = materialTotalPlanMap.getOrDefault(materialCode, 0);
            int embryoStock = materialEmbryoStockMap.getOrDefault(materialCode, 0);
            if (totalPlan <= 0 || totalPlan <= embryoStock) {
                continue;
            }
            if (shouldKeepAllProductStatusFullCapacity(context, entry.getValue())) {
                log.info("正式新增跳过胎胚库存后置裁减, materialCode: {}, totalPlan: {}, embryoStock: {}",
                        materialCode, totalPlan, embryoStock);
                continue;
            }
            // 库存不足时按物料整体裁剪，避免逐条逐班取整导致总量丢失。
            ShiftFieldUtil.scaleGroupedShiftPlanQty(entry.getValue(), shifts, embryoStock);
            for (LhScheduleResult result : entry.getValue()) {
                refreshResultSummary(context, result);
            }
        }
        // 同SKU多机台只拆分排产量，每条结果保留SKU级完整胎胚库存。
        retainMultiMachineEmbryoStock(context);
        finalizeZeroPlanNewSpecResults(context);
        // 新增结果在库存裁剪后需按最终计划量复核收尾语义，避免"未收完却标收尾"。
        refreshNewSpecEndingFlagByResult(context);
        syncMachineStateAfterNewAdjust(context);
        // S4.5 后置步骤均完成后，再按当前待排列表收口结构视图，避免影响本阶段元数据回查。
        context.rebuildStructureSkuMapFromPending(context.getNewSpecSkuList());
    }

    /**
     * 正式新增在非试制场景下保留满班补齐结果，不做胎胚库存后置裁减。
     *
     * @param sku SKU排程DTO
     * @param skuResults 该物料编码对应的新增结果
     * @return true-保留满班结果，不做库存裁减
     */
    private boolean shouldKeepFormalNewSpecFullCapacity(SkuScheduleDTO sku, List<LhScheduleResult> skuResults) {
        if (sku == null || CollectionUtils.isEmpty(skuResults)) {
            return false;
        }
        if (sku.getEmbryoStock() <= 0) {
            return false;
        }
        boolean endingResult = skuResults.stream().anyMatch(result -> result != null && "1".equals(result.getIsEnd()));
        ProductionQuantityPolicy policy = ProductionQuantityPolicy.from(sku, endingResult);
        if (policy.isStrictUpperLimit() && !policy.isEnding()) {
            return false;
        }
        return true;
    }

    /**
     * 判断同物料全部产品状态是否都允许保留满班结果。
     * <p>库存仍按物料整体约束，但严格目标状态不得被其它非严格状态带着跳过库存裁剪。</p>
     *
     * @param context 排程上下文
     * @param materialResults 同物料结果列表
     * @return true-全部状态均允许保留满班结果
     */
    private boolean shouldKeepAllProductStatusFullCapacity(LhScheduleContext context,
                                                           List<LhScheduleResult> materialResults) {
        if (CollectionUtils.isEmpty(materialResults)) {
            return false;
        }
        Map<String, List<LhScheduleResult>> skuResultMap = new LinkedHashMap<String, List<LhScheduleResult>>(4);
        for (LhScheduleResult result : materialResults) {
            if (result == null) {
                continue;
            }
            String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                    result.getMaterialCode(), result.getProductStatus());
            skuResultMap.computeIfAbsent(skuKey, key -> new ArrayList<LhScheduleResult>(2)).add(result);
        }
        for (List<LhScheduleResult> skuResults : skuResultMap.values()) {
            LhScheduleResult firstResult = skuResults.get(0);
            SkuScheduleDTO sku = findSkuDto(
                    context, firstResult.getMaterialCode(), firstResult.getProductStatus());
            if (!shouldKeepFormalNewSpecFullCapacity(sku, skuResults)) {
                return false;
            }
        }
        return !skuResultMap.isEmpty();
    }

    @Override
    public void scheduleReduceMould(LhScheduleContext context) {
        // 新增策略不处理降模，空实现
    }

    @Override
    public void scheduleNewSpecs(LhScheduleContext context,
                                 IMachineMatchStrategy machineMatch,
                                 IMouldChangeBalanceStrategy mouldChangeBalance,
                                 IFirstInspectionBalanceStrategy inspectionBalance,
                                 ICapacityCalculateStrategy capacityCalculate) {
        log.info("新增排产 - 执行新增规格排产, 新增SKU数: {}", context.getNewSpecSkuList().size());

        /*
         * S4.5 在 Handler 中先形成首日排序。进入日驱动主链后，每个业务日还会对该日真实待排
         * SKU 集合复用同一比较器重新排序；延期 SKU 不继承前一日名次、目标班次或候选机台。
         */
        List<LhShiftConfigVO> shifts =
                LhScheduleTimeUtil.getScheduleShifts(context, context.getScheduleDate());
        LinkedHashMap<LocalDate, List<LhShiftConfigVO>> dayShiftMap =
                LhScheduleTimeUtil.groupByWorkDate(shifts);
        // S4.5 按日编排只接受固定 2/3/3 的八班窗口，不能静默跳过 workDate 缺失班次后继续排程。
        validateDayDrivenShiftLayout(shifts, dayShiftMap);
        DayDrivenScheduleState state =
                new DayDrivenScheduleState(context.getNewSpecSkuList());
        rebuildScheduledMachineCountMap(context, shifts);
        Map<String, Integer> unscheduledReasonCountMap = new LinkedHashMap<>(8);
        initializePendingNewSpecSkuTypeCounts(context);
        int scheduledCount = 0;
        int dayIndex = 0;
        int totalDayCount = dayShiftMap.size();
        /*
         * 三天窗口只输出一条“新增排产明细”，此处先按业务日先后暂存每日标题与明细，
         * 待窗口全部编排结束后统一合并写入过程日志，避免继续产生 T/T+1/T+2 三条日志。
         */
        List<String> newSpecOrderLogSections = new ArrayList<String>(totalDayCount);
        /*
         * S4.3 已按当前月 TOTAL_QTY 建立提前生产候选视图。这里必须保留候选态，
         * 使 futurePlanDate 尚未进入阈值的 SKU 能随业务日推进后再尝试激活。
         */
        for (Map.Entry<LocalDate, List<LhShiftConfigVO>> entry : dayShiftMap.entrySet()) {
            dayIndex++;
            DayScheduleContext dayContext = new DayScheduleContext(
                    entry.getKey(), entry.getValue(), dayIndex == 1, dayIndex == totalDayCount);
            // 当前业务日是无台账到货模具、选机预检和模具正式预占的唯一日期口径。
            context.setCurrentScheduleDate(toDate(dayContext.getScheduleDate()));
            refreshMouldResourceAvailability(context);
            state.beginDay();
            log.info("新增排产按日编排开始, batchNo: {}, scheduleDate: {}, dayIndex: {}/{}, "
                            + "shiftIndexes: {}, pendingSkuCount: {}, activeBindingCount: {}",
                    context.getBatchNo(), dayContext.getScheduleDate(), dayIndex, totalDayCount,
                    dayContext.getDayShifts().stream()
                            .map(LhShiftConfigVO::getShiftIndex).collect(Collectors.toList()),
                    state.getPendingSkuListInOriginalOrder().size(),
                    state.getActiveBindings().size());

            /*
             * 每个业务日严格按“在机延续 -> 当天普通新增与到期加机台统一竞争
             * -> 历史遗留 -> 提前生产”执行。
             * 每个阶段只接收当前日班次切片，任何阶段都不能提前写入下一业务日 class 字段。
             */
            scheduledCount += scheduleCurrentBusinessDay(
                    context, dayContext, state, machineMatch, mouldChangeBalance,
                    inspectionBalance, capacityCalculate, shifts, unscheduledReasonCountMap,
                    newSpecOrderLogSections);
            finalizeDayDrivenScheduleDay(context, dayContext, state, shifts);
        }

        // 三天新增排产顺序分节合并为一条过程日志，只调整日志输出，不影响新增排产逻辑。
        this.appendMergedNewSpecOrderProcessLog(context, newSpecOrderLogSections);
        // 三个业务日全部完成后，才把仍未完成的临时延期任务转成窗口级最终未排。
        finalizeWindowUnscheduled(
                context, state, unscheduledReasonCountMap, machineMatch);
        context.getNewSpecSkuList().clear();
        context.getNewSpecSkuList().addAll(state.getPendingSkuListInOriginalOrder());
        // 新增主链结束后统一核查收尾机台，给出尾部产能是否被利用的可对账原因。
        traceReleasedMachineTailCapacityAudit(context, shifts);
        log.info("新增排产完成, 成功: {}, 未排: {}, 原因分布: {}",
                scheduledCount,
                unscheduledReasonCountMap.values().stream().mapToInt(Integer::intValue).sum(),
                unscheduledReasonCountMap);
    }

    /**
     * 校验日驱动新增排产所需的完整班次布局。
     *
     * <p>排程窗口中的晚班自然开始日期会早于其业务归属日期，因而只能按 workDate 分组。
     * 若出现空 workDate、重复班次、业务日不连续或非 2/3/3 布局，继续排程会把后续班次
     * 静默丢失并造成账本、机台和结果不一致，必须在任何资源消费前中断本次排程。</p>
     *
     * @param shifts 排程窗口完整班次
     * @param dayShiftMap 按业务日期稳定分组后的班次
     */
    private void validateDayDrivenShiftLayout(List<LhShiftConfigVO> shifts,
                                              LinkedHashMap<LocalDate, List<LhShiftConfigVO>> dayShiftMap) {
        if (CollectionUtils.isEmpty(shifts)
                || shifts.size() != LhScheduleConstant.MAX_SHIFT_SLOT_COUNT) {
            throw new IllegalStateException("新增排产按日编排班次不完整，必须提供8个班次，实际班次数="
                    + (CollectionUtils.isEmpty(shifts) ? 0 : shifts.size()));
        }
        List<Integer> shiftIndexList = new ArrayList<Integer>(shifts.size());
        for (LhShiftConfigVO shift : shifts) {
            if (Objects.isNull(shift) || Objects.isNull(shift.getShiftIndex())
                    || Objects.isNull(shift.getWorkDate())) {
                throw new IllegalStateException("新增排产按日编排班次缺少班次索引或业务归属日期");
            }
            shiftIndexList.add(shift.getShiftIndex());
        }
        if (!DAY_DRIVEN_ALL_SHIFT_INDEXES.equals(shiftIndexList)) {
            throw new IllegalStateException("新增排产按日编排班次索引必须严格为class1至class8，实际="
                    + shiftIndexList);
        }
        if (Objects.isNull(dayShiftMap) || dayShiftMap.size() != DAY_DRIVEN_SCHEDULE_DAY_COUNT) {
            throw new IllegalStateException("新增排产按日编排必须形成T、T+1、T+2三个业务日，实际业务日数="
                    + (Objects.isNull(dayShiftMap) ? 0 : dayShiftMap.size()));
        }
        LocalDate firstWorkDate = null;
        int dayPosition = 0;
        for (Map.Entry<LocalDate, List<LhShiftConfigVO>> entry : dayShiftMap.entrySet()) {
            if (Objects.isNull(firstWorkDate)) {
                firstWorkDate = entry.getKey();
            }
            LocalDate expectedWorkDate = firstWorkDate.plusDays(dayPosition);
            if (!expectedWorkDate.equals(entry.getKey())) {
                throw new IllegalStateException("新增排产按日编排业务日不连续，期望="
                        + expectedWorkDate + "，实际=" + entry.getKey());
            }
            List<Integer> expectedShiftIndexes = resolveExpectedDayShiftIndexes(dayPosition);
            List<Integer> actualShiftIndexes = entry.getValue().stream()
                    .map(LhShiftConfigVO::getShiftIndex).collect(Collectors.toList());
            if (!expectedShiftIndexes.equals(actualShiftIndexes)) {
                throw new IllegalStateException("新增排产按日编排班次布局错误，业务日=" + entry.getKey()
                        + "，期望=" + expectedShiftIndexes + "，实际=" + actualShiftIndexes);
            }
            dayPosition++;
        }
    }

    /**
     * 获取指定业务日位置应包含的班次索引。
     *
     * @param dayPosition 从0开始的业务日位置
     * @return 当前业务日合法班次索引
     */
    private List<Integer> resolveExpectedDayShiftIndexes(int dayPosition) {
        if (dayPosition == 0) {
            return DAY_DRIVEN_FIRST_DAY_SHIFT_INDEXES;
        }
        if (dayPosition == 1) {
            return DAY_DRIVEN_SECOND_DAY_SHIFT_INDEXES;
        }
        if (dayPosition == 2) {
            return DAY_DRIVEN_THIRD_DAY_SHIFT_INDEXES;
        }
        throw new IllegalArgumentException("新增排产按日编排不存在第" + dayPosition + "个业务日布局");
    }

    /**
     * 刷新已有模具资源上下文的到货可用性。
     *
     * <p>选机硬过滤可能在当前日第一条 SKU 上创建模具资源上下文。后续推进到下一业务日时，
     * 只能刷新到货可用性，必须保留已占用模具和机台绑定运行态。</p>
     *
     * @param context 排程上下文
     */
    private void refreshMouldResourceAvailability(LhScheduleContext context) {
        if (Objects.nonNull(context) && Objects.nonNull(context.getMouldResourceContext())) {
            context.getMouldResourceContext().refreshAvailability(context);
        }
    }

    /**
     * 按固定阶段执行当前业务日新增排产。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日编排上下文
     * @param state 三天窗口共用日驱动状态
     * @param machineMatch 机台匹配策略
     * @param mouldChangeBalance 换模均衡策略
     * @param inspectionBalance 首检均衡策略
     * @param capacityCalculate 产能计算策略
     * @param allShifts 完整排程窗口班次
     * @param unscheduledReasonCountMap 最终未排原因计数
     * @param newSpecOrderLogSections 三天窗口新增排产顺序日志分节暂存列表
     * @return 当前业务日新形成有效上机结果的数量
     */
    private int scheduleCurrentBusinessDay(LhScheduleContext context,
                                           DayScheduleContext dayContext,
                                           DayDrivenScheduleState state,
                                           IMachineMatchStrategy machineMatch,
                                           IMouldChangeBalanceStrategy mouldChangeBalance,
                                           IFirstInspectionBalanceStrategy inspectionBalance,
                                           ICapacityCalculateStrategy capacityCalculate,
                                           List<LhShiftConfigVO> allShifts,
                                           Map<String, Integer> unscheduledReasonCountMap,
                                           List<String> newSpecOrderLogSections) {
        int scheduledCount = 0;

        // 阶段一：前一业务日已上机 SKU 必须先使用原机台连续生产，不重新选机、换模或首检。
        dayContext.setCurrentPhase(DailySchedulePhase.CARRY_OVER);
        scheduledCount += scheduleCarryOverSkus(
                context, dayContext, state, allShifts, false);

        /*
         * 阶段一完成后执行“换活字块检测 + 机台反选物料”：
         * 只读扫描当天候选机台（硬性过滤且可开产时间落在当天）与当天正常待排物料（排除提前生产），
         * 完全复用现有换活字块匹配口径生成机台→物料配对并预留机台；
         * 正常资源竞争阶段会优先处理命中物料，剩余物料/机台仍走原新增排产。
         */
        detectAndRegisterDayTypeBlockReverseSelection(
                context, dayContext, state, machineMatch, mouldChangeBalance,
                inspectionBalance, capacityCalculate,
                DailySchedulePhase.NORMAL_RESOURCE_COMPETITION);

        /*
         * 阶段二：把当天有计划且尚未绑定机台的普通新增，与当天确需新增机台的续作/在机 SKU
         * 合并成一个工作队列，严格按 S4.5 sortRank 统一竞争资源。单个 SKU 轮到后由既有
         * 多机台主循环一次性尝试完当天所需机台，再轮到下一 SKU。
         */
        scheduledCount += scheduleDailyCandidatePhase(
                context, dayContext, state, DailySchedulePhase.NORMAL_RESOURCE_COMPETITION,
                machineMatch, mouldChangeBalance, inspectionBalance, capacityCalculate,
                unscheduledReasonCountMap);
        /*
         * 正常资源竞争阶段收口后统一结算按天换活字块反选指令并释放全部机台预留，
         * 防止预留泄漏到提前生产阶段或下一业务日；次日会按最新机台运行态重新检测。
         */
        finalizeDayTypeBlockReverseSelection(context, dayContext);

        /*
         * 提前生产开始前基于正常阶段最新结果重建 Set 去重统计，相当于冻结正常排程结果和资源占用。
         * 提前生产后续只读取该时点之后的真实剩余资源，禁止回调正常阶段重新选机或释放资源。
         */
        rebuildScheduledMachineCountMap(context, allShifts);
        /*
         * 前一日提前生产形成的在机绑定不能在次日正常阶段之前继续占用产能。
         * 正常阶段完成后，若原机台仍未被正常任务换产，再在提前生产阶段使用原机台续排；
         * 若已被正常任务换产，后续提前候选按最新运行态重新进入既有选机主链。
         */
        dayContext.setCurrentPhase(DailySchedulePhase.EARLY_PRODUCTION);
        scheduledCount += scheduleCarryOverSkus(
                context, dayContext, state, allShifts, true);
        /*
         * S4.4 只处理有当日原始计划的普通换活字块；所有提前生产候选统一后置到这里。
         * 当前日正常任务和提前生产在机延续完成后，再基于实时剩余机台执行同一套换活字块
         * 反选，既不抢占正常资源，也保留同胎胚、同模具和既有选机排序口径。
         */
        detectAndRegisterDayTypeBlockReverseSelection(
                context, dayContext, state, machineMatch, mouldChangeBalance,
                inspectionBalance, capacityCalculate,
                DailySchedulePhase.EARLY_PRODUCTION);
        scheduledCount += scheduleDailyCandidatePhase(
                context, dayContext, state, DailySchedulePhase.EARLY_PRODUCTION,
                machineMatch, mouldChangeBalance, inspectionBalance, capacityCalculate,
                unscheduledReasonCountMap);
        finalizeDayTypeBlockReverseSelection(context, dayContext);
        /*
         * 当天正常和提前生产阶段全部执行完成后，使用同一个采集器生成当前日日志分节并暂存，
         * 供三天窗口结束后统一合并。明细顺序来自各阶段真实主循环的追加顺序；
         * 在机延续阶段未调用采集器，因此不会进入日志。
         */
        this.appendDailyNewSpecOrderLogSection(dayContext, newSpecOrderLogSections);
        return scheduledCount;
    }

    /**
     * 检测并按天登记换活字块机台反选配对。
     *
     * <p>本方法只读构建“当天正常待排物料（排除提前生产）”与“当天候选机台（硬性过滤且
     * 可开产时间落在当天）”，完全复用现有换活字块匹配口径生成机台→物料配对并预留机台；
     * 不写入排程结果、日计划账本或机台运行态。命中物料由正常资源竞争阶段优先落地，
     * 实际切换时间、首检、班次计划量、机台收尾时间和物料账本仍走 S4.5 新增主链。</p>
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日上下文
     * @param state 三天窗口共用日驱动状态
     * @param machineMatch 机台匹配策略
     * @param mouldChangeBalance 换模均衡策略
     * @param inspectionBalance 首检均衡策略
     * @param capacityCalculate 产能计算策略
     * @param phase 当前业务日候选阶段
     */
    private void detectAndRegisterDayTypeBlockReverseSelection(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            DayDrivenScheduleState state,
            IMachineMatchStrategy machineMatch,
            IMouldChangeBalanceStrategy mouldChangeBalance,
            IFirstInspectionBalanceStrategy inspectionBalance,
            ICapacityCalculateStrategy capacityCalculate,
            DailySchedulePhase phase) {
        // 按当前阶段构建待排物料，正常与提前生产分别使用各自准入，但统一复用同一排序和匹配能力。
        List<SkuScheduleDTO> dayMaterials =
                this.buildDayTypeBlockMaterialList(context, dayContext, state, phase);
        if (CollectionUtils.isEmpty(dayMaterials)) {
            log.info("按天换活字块机台反选跳过, batchNo: {}, scheduleDate: {}, phase: {}, "
                            + "原因: 当前阶段无待排物料",
                    context.getBatchNo(), dayContext.getScheduleDate(), phase);
            return;
        }
        // 当天候选机台：对每个物料执行既有硬过滤，再用无副作用真实可开产计划确认落在当天。
        List<MachineScheduleDTO> dayMachines = this.buildDayTypeBlockMachinePool(
                context, dayContext, dayMaterials, machineMatch, mouldChangeBalance,
                inspectionBalance, capacityCalculate);
        if (CollectionUtils.isEmpty(dayMachines)) {
            log.info("按天换活字块机台反选跳过, batchNo: {}, scheduleDate: {}, 当天待排物料: {}, 原因: 无当天候选机台",
                    context.getBatchNo(), dayContext.getScheduleDate(), dayMaterials.size());
            return;
        }
        // 公共匹配能力：完全复用现有换活字块候选判断与机台排序，返回稳定有序配对。
        List<DayTypeBlockReverseSelectionDirective> directives =
                typeBlockProductionStrategy.matchDayTypeBlockReversePairs(
                        context, dayContext.getScheduleDate(), dayMaterials, dayMachines);
        if (CollectionUtils.isEmpty(directives)) {
            log.info("按天换活字块机台反选无命中, batchNo: {}, scheduleDate: {}, 候选机台: {}, 候选物料: {}, 原因: 无同胎胚同模具换活字块配对",
                    context.getBatchNo(), dayContext.getScheduleDate(),
                    dayMachines.size(), dayMaterials.size());
            this.appendDayTypeBlockNoMatchLog(context, dayContext, dayMachines, dayMaterials);
            return;
        }
        for (DayTypeBlockReverseSelectionDirective directive : directives) {
            context.registerDayTypeBlockReverseSelection(directive);
            this.appendDayTypeBlockReverseLog(context, directive, "命中并锁定",
                    "机台反选物料命中换活字块关系，等待新增主链优先落地");
        }
        // 未命中机台逐台登记失败原因，命中机台与物料在指令日志中已完整记录。
        Set<String> matchedMachineCodeSet = new LinkedHashSet<String>(directives.size());
        for (DayTypeBlockReverseSelectionDirective directive : directives) {
            if (Objects.nonNull(directive)
                    && StringUtils.isNotEmpty(directive.getMachineCode())) {
                matchedMachineCodeSet.add(directive.getMachineCode());
            }
        }
        for (MachineScheduleDTO machine : dayMachines) {
            if (Objects.isNull(machine) || StringUtils.isEmpty(machine.getMachineCode())
                    || matchedMachineCodeSet.contains(machine.getMachineCode())) {
                continue;
            }
            log.info("按天换活字块机台反选未命中, batchNo: {}, scheduleDate: {}, machineCode: {}, previousMaterialCode: {}, 原因: 当天待排物料均不满足同胎胚同模具换活字块条件或物料已被其他机台锁定",
                    context.getBatchNo(), dayContext.getScheduleDate(),
                    machine.getMachineCode(),
                    StringUtils.defaultString(machine.getCurrentMaterialCode(), "-"));
        }
        log.info("按天换活字块机台反选完成, batchNo: {}, scheduleDate: {}, phase: {}, "
                        + "候选机台: {}, 候选物料: {}, 命中并锁定: {}, 明细机台: {}, 明细物料: {}",
                context.getBatchNo(), dayContext.getScheduleDate(), phase,
                dayMachines.size(), dayMaterials.size(), directives.size(),
                dayMachines.stream().filter(Objects::nonNull)
                        .map(MachineScheduleDTO::getMachineCode)
                        .collect(Collectors.toList()),
                dayMaterials.stream().filter(Objects::nonNull)
                        .map(SkuScheduleDTO::getMaterialCode)
                        .collect(Collectors.toList()));
    }

    /**
     * 输出按天换活字块机台反选无命中的候选机台与物料明细日志。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日上下文
     * @param dayMachines 当天候选机台
     * @param dayMaterials 当天正常待排物料
     */
    private void appendDayTypeBlockNoMatchLog(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            List<MachineScheduleDTO> dayMachines,
            List<SkuScheduleDTO> dayMaterials) {
        StringBuilder detailBuilder = new StringBuilder(320);
        detailBuilder.append("scheduleTargetDate=")
                .append(LhScheduleTimeUtil.formatDate(context.getScheduleTargetDate()))
                .append(", scheduleDate=").append(dayContext.getScheduleDate())
                .append(", 候选机台=").append(dayMachines.stream().filter(Objects::nonNull)
                        .map(MachineScheduleDTO::getMachineCode)
                        .collect(Collectors.toList()))
                .append(", 候选物料=").append(dayMaterials.stream().filter(Objects::nonNull)
                        .map(SkuScheduleDTO::getMaterialCode)
                        .collect(Collectors.toList()))
                .append(", 是否命中换活字块=否")
                .append(", 失败原因=当天待排物料均不满足同胎胚同模具换活字块条件");
        log.info("按天换活字块机台反选, {}", detailBuilder);
        PriorityTraceLogHelper.appendProcessLog(
                context, "按天换活字块机台反选", detailBuilder.toString());
    }

    /**
     * 构建当天正常待排物料列表。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日上下文
     * @param state 三天窗口共用日驱动状态
     * @param phase 当前业务日候选阶段
     * @return 已按当天 S4.5 优先级排序的物料列表；无候选时返回空列表
     */
    private List<SkuScheduleDTO> buildDayTypeBlockMaterialList(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            DayDrivenScheduleState state,
            DailySchedulePhase phase) {
        List<DailyNewSpecCandidate> candidates = buildDailyCandidateList(
                context, dayContext, state, phase);
        if (CollectionUtils.isEmpty(candidates)) {
            return Collections.emptyList();
        }
        List<SkuScheduleDTO> materialList = new ArrayList<SkuScheduleDTO>(candidates.size());
        for (DailyNewSpecCandidate candidate : candidates) {
            if (Objects.nonNull(candidate) && Objects.nonNull(candidate.getSku())) {
                materialList.add(candidate.getSku());
            }
        }
        // 与当前阶段主循环复用同一排序口径，保证反选优先顺序与实际排产顺序一致。
        this.skuPriorityStrategy.sortNewSpecByPriority(context, materialList);
        return materialList;
    }

    /**
     * 构建当天候选机台并集。
     *
     * <p>对每个当天物料复用 {@link IMachineMatchStrategy#matchMachines} 完成既有硬性过滤，
     * 再通过无副作用的真实可开产计划确认“机台计算后的可开产时间落在当天”，
     * 与正常资源竞争阶段的逐班候选口径保持一致。</p>
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日上下文
     * @param dayMaterials 当天正常待排物料
     * @param machineMatch 机台匹配策略
     * @param mouldChangeBalance 换模均衡策略
     * @param inspectionBalance 首检均衡策略
     * @param capacityCalculate 产能计算策略
     * @return 去重后的当天候选机台列表
     */
    private List<MachineScheduleDTO> buildDayTypeBlockMachinePool(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            List<SkuScheduleDTO> dayMaterials,
            IMachineMatchStrategy machineMatch,
            IMouldChangeBalanceStrategy mouldChangeBalance,
            IFirstInspectionBalanceStrategy inspectionBalance,
            ICapacityCalculateStrategy capacityCalculate) {
        LinkedHashMap<String, MachineScheduleDTO> machineMap =
                new LinkedHashMap<String, MachineScheduleDTO>(16);
        for (SkuScheduleDTO sku : dayMaterials) {
            if (Objects.isNull(sku)) {
                continue;
            }
            boolean isEnding = endingJudgmentStrategy.isCurrentWindowEnding(context, sku);
            /*
             * 提前生产反选发生在正式选机之前，也必须使用与新增主循环相同的准入标记。
             * 正常阶段调用会清理该标记；提前阶段只对已经激活且准入通过的运行视图置为允许，
             * 避免反选预演因缺少门禁状态得到空候选或绕过共享中心自行放行。
             */
            this.refreshNewSpecEarlyProductionAdmission(
                    context, sku, dayContext.getDayShifts(), isEnding,
                    dayContext.getCurrentPhase());
            List<MachineScheduleDTO> hardCandidates = machineMatch.matchMachines(context, sku);
            if (CollectionUtils.isEmpty(hardCandidates)) {
                continue;
            }
            Date candidateProductionNotBeforeTime = NewSpecEmbryoAvailableTimeResolver
                    .resolveSkuProductionGateTime(context, sku, context.getScheduleWindowShifts());
            Date productionNotBeforeTime = NewSpecEmbryoAvailableTimeResolver
                    .resolveProductionNotBeforeTime(context, sku, context.getScheduleWindowShifts());
            // 与正常资源竞争阶段同一口径：可用量取 SKU 运行态账本剩余量，保证首检/产能预演一致。
            int schedulableRemainingQty = resolveSchedulableRemainingQty(context, sku);
            for (MachineScheduleDTO machine : hardCandidates) {
                if (Objects.isNull(machine) || StringUtils.isEmpty(machine.getMachineCode())) {
                    continue;
                }
                NewSpecMachineAvailabilityPlan plan = this.resolveMachineAvailabilityPlan(
                        context, sku, machine, dayContext, capacityCalculate, mouldChangeBalance,
                        inspectionBalance, candidateProductionNotBeforeTime, productionNotBeforeTime,
                        schedulableRemainingQty, 0, null, isEnding);
                if (Objects.isNull(plan) || !plan.isAvailable() || !plan.isPreparationAvailable()
                        || !this.isFormalTargetShiftInDay(plan, dayContext)) {
                    continue;
                }
                machineMap.putIfAbsent(machine.getMachineCode(), machine);
            }
        }
        return new ArrayList<MachineScheduleDTO>(machineMap.values());
    }

    /**
     * 判断可开产计划的正式目标班次是否落在当前业务日。
     *
     * @param plan 真实可开产计划
     * @param dayContext 当前业务日上下文
     * @return true-正式开产班次属于当前业务日；false-不在当天
     */
    private boolean isFormalTargetShiftInDay(NewSpecMachineAvailabilityPlan plan,
                                             DayScheduleContext dayContext) {
        if (Objects.isNull(plan) || Objects.isNull(plan.getFormalTargetShift())
                || Objects.isNull(dayContext)
                || CollectionUtils.isEmpty(dayContext.getDayShifts())) {
            return false;
        }
        Integer formalShiftIndex = plan.getFormalTargetShift().getShiftIndex();
        for (LhShiftConfigVO dayShift : dayContext.getDayShifts()) {
            if (Objects.nonNull(dayShift)
                    && Objects.equals(dayShift.getShiftIndex(), formalShiftIndex)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 把按天换活字块反选命中的物料前置到当前阶段工作队列。
     *
     * <p>命中物料之间仍保持当天 S4.5 相对优先级，未命中物料保持原顺序；
     * 只调整优先处理顺序，不改变 SKU 类型、目标量、机台排序和选机规则。</p>
     *
     * @param context 排程上下文
     * @param workingSkuList 已按当天 S4.5 优先级排序的工作队列
     */
    private void prependDayTypeBlockMatchedSkus(LhScheduleContext context,
                                                List<SkuScheduleDTO> workingSkuList) {
        List<DayTypeBlockReverseSelectionDirective> directives =
                context.getDayTypeBlockReverseSelectionDirectiveList();
        if (CollectionUtils.isEmpty(directives) || CollectionUtils.isEmpty(workingSkuList)) {
            return;
        }
        // 使用对象身份收集当天命中物料，避免同物料多产品状态相互覆盖。
        Set<SkuScheduleDTO> matchedSkuSet =
                Collections.newSetFromMap(new IdentityHashMap<SkuScheduleDTO, Boolean>());
        for (DayTypeBlockReverseSelectionDirective directive : directives) {
            if (Objects.isNull(directive) || directive.isSatisfied()) {
                continue;
            }
            String directiveSkuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                    directive.getMaterialCode(),
                    this.normalizeDayTypeBlockProductStatus(directive.getProductStatus()));
            for (SkuScheduleDTO sku : workingSkuList) {
                if (Objects.isNull(sku)) {
                    continue;
                }
                String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                        sku.getMaterialCode(),
                        this.normalizeDayTypeBlockProductStatus(sku.getProductStatus()));
                if (StringUtils.equals(directiveSkuKey, skuKey)) {
                    matchedSkuSet.add(sku);
                }
            }
        }
        if (CollectionUtils.isEmpty(matchedSkuSet)) {
            return;
        }
        List<SkuScheduleDTO> reordered = new ArrayList<SkuScheduleDTO>(workingSkuList.size());
        for (SkuScheduleDTO sku : workingSkuList) {
            if (matchedSkuSet.contains(sku)) {
                reordered.add(sku);
            }
        }
        for (SkuScheduleDTO sku : workingSkuList) {
            if (!matchedSkuSet.contains(sku)) {
                reordered.add(sku);
            }
        }
        workingSkuList.clear();
        workingSkuList.addAll(reordered);
        LocalDate directiveScheduleDate = null;
        for (DayTypeBlockReverseSelectionDirective directive : directives) {
            if (Objects.nonNull(directive)) {
                directiveScheduleDate = directive.getScheduleDate();
                break;
            }
        }
        log.info("按天换活字块反选命中物料前置, batchNo: {}, scheduleDate: {}, 命中物料数: {}, 前置物料: {}",
                context.getBatchNo(), directiveScheduleDate,
                matchedSkuSet.size(),
                reordered.stream()
                        .filter(matchedSkuSet::contains)
                        .map(SkuScheduleDTO::getMaterialCode)
                        .collect(Collectors.toList()));
    }

    /**
     * 在当前 SKU 的候选列表上应用按天换活字块反选机台优先。
     *
     * <p>命中 SKU 的预留机台置顶优先尝试，其余候选保持原顺序；命中物料已由当天
     * 工作队列前置，当前阶段内其他物料不会先于它占用预留机台。
     * 预留机台不在当前候选列表或正式约束失败时自动回退普通新增，不阻塞其他物料。</p>
     *
     * @param context 排程上下文
     * @param sku 当前新增 SKU
     * @param candidates 当前候选机台列表
     * @return 应用反选优先后的候选列表
     */
    private List<MachineScheduleDTO> applyDayTypeBlockReverseSelection(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            List<MachineScheduleDTO> candidates) {
        List<DayTypeBlockReverseSelectionDirective> directives =
                context.getDayTypeBlockReverseSelectionDirectiveList();
        if (CollectionUtils.isEmpty(directives) || CollectionUtils.isEmpty(candidates)) {
            return candidates;
        }
        String currentSkuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                sku.getMaterialCode(),
                this.normalizeDayTypeBlockProductStatus(sku.getProductStatus()));
        List<MachineScheduleDTO> reservedForCurrent =
                new ArrayList<MachineScheduleDTO>(1);
        for (DayTypeBlockReverseSelectionDirective directive : directives) {
            if (Objects.isNull(directive) || directive.isSatisfied() || directive.isSuccess()) {
                continue;
            }
            String directiveSkuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                    directive.getMaterialCode(),
                    this.normalizeDayTypeBlockProductStatus(directive.getProductStatus()));
            if (!StringUtils.equals(currentSkuKey, directiveSkuKey)) {
                continue;
            }
            // 命中物料轮到时把预留机台置顶优先尝试，并登记“已尝试”，供阶段收口区分失败原因。
            directive.setAttempted(true);
            for (MachineScheduleDTO machine : candidates) {
                if (Objects.nonNull(machine)
                        && StringUtils.equals(machine.getMachineCode(),
                        directive.getMachineCode())) {
                    reservedForCurrent.add(machine);
                }
            }
        }
        if (CollectionUtils.isEmpty(reservedForCurrent)) {
            return candidates;
        }
        List<MachineScheduleDTO> result =
                new ArrayList<MachineScheduleDTO>(candidates.size());
        Set<String> addedMachineCodeSet =
                new LinkedHashSet<String>(candidates.size());
        for (MachineScheduleDTO machine : reservedForCurrent) {
            if (Objects.nonNull(machine)
                    && addedMachineCodeSet.add(machine.getMachineCode())) {
                result.add(machine);
            }
        }
        for (MachineScheduleDTO machine : candidates) {
            if (Objects.nonNull(machine)
                    && addedMachineCodeSet.add(machine.getMachineCode())) {
                result.add(machine);
            }
        }
        log.info("按天换活字块反选预留机台优先尝试, batchNo: {}, materialCode: {}, productStatus: {}, reservedMachines: {}",
                context.getBatchNo(), sku.getMaterialCode(), sku.getProductStatus(),
                reservedForCurrent.stream()
                        .map(MachineScheduleDTO::getMachineCode)
                        .collect(Collectors.toList()));
        return result;
    }

    /**
     * 标记按天换活字块反选指令成功并释放机台预留。
     *
     * <p>必须在新增主链结果与机台运行态提交后调用：此时机台当前物料与收尾时间已由
     * {@code updateMachineState} 同步，物料账本已扣减；本方法只登记指令状态并释放当天
     * 机台预留，保证该机台与物料在当天不再被重复反选或重复排产。</p>
     *
     * @param context 排程上下文
     * @param machineCode 落地机台编码
     * @param sku 当前 SKU
     * @param result 新增主链生成的有效结果
     */
    private void markDayTypeBlockReverseDirectiveSucceeded(
            LhScheduleContext context,
            String machineCode,
            SkuScheduleDTO sku,
            LhScheduleResult result) {
        if (StringUtils.isEmpty(machineCode) || Objects.isNull(sku) || Objects.isNull(result)) {
            return;
        }
        DayTypeBlockReverseSelectionDirective directive =
                this.findDayTypeBlockDirectiveByMachineAndSku(context, machineCode, sku);
        if (Objects.isNull(directive)) {
            return;
        }
        directive.setAttempted(true);
        directive.setSuccess(true);
        directive.setSatisfied(true);
        directive.setResultReason("机台反选换活字块已由新增主链落地，机台收尾时间与物料账本同步更新");
        context.releaseDayTypeBlockReverseSelectedMachine(machineCode);
        this.appendDayTypeBlockReverseLog(context, directive, "成功", directive.getResultReason());
    }

    /**
     * 按机台编码与 SKU 查找当天按天换活字块反选指令。
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @param sku 当前 SKU
     * @return 对应指令；未命中返回 null
     */
    private DayTypeBlockReverseSelectionDirective findDayTypeBlockDirectiveByMachineAndSku(
            LhScheduleContext context,
            String machineCode,
            SkuScheduleDTO sku) {
        if (StringUtils.isEmpty(machineCode) || Objects.isNull(sku)
                || CollectionUtils.isEmpty(
                context.getDayTypeBlockReverseSelectionDirectiveList())) {
            return null;
        }
        String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                sku.getMaterialCode(),
                this.normalizeDayTypeBlockProductStatus(sku.getProductStatus()));
        for (DayTypeBlockReverseSelectionDirective directive
                : context.getDayTypeBlockReverseSelectionDirectiveList()) {
            if (Objects.isNull(directive)) {
                continue;
            }
            String directiveSkuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                    directive.getMaterialCode(),
                    this.normalizeDayTypeBlockProductStatus(directive.getProductStatus()));
            if (StringUtils.equals(machineCode, directive.getMachineCode())
                    && StringUtils.equals(skuKey, directiveSkuKey)) {
                return directive;
            }
        }
        return null;
    }

    /**
     * 结算当前阶段按天换活字块反选指令并释放全部机台预留。
     *
     * <p>在当天正常或提前生产阶段结束后调用：已成功落地指令保持成功状态，其余指令登记
     * 明确失败原因，随后统一清空当前阶段反选状态，避免预留泄漏到下一阶段或下一业务日。</p>
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日上下文
     */
    private void finalizeDayTypeBlockReverseSelection(LhScheduleContext context,
                                                      DayScheduleContext dayContext) {
        List<DayTypeBlockReverseSelectionDirective> directives =
                context.getDayTypeBlockReverseSelectionDirectiveList();
        int directiveCount = CollectionUtils.isEmpty(directives) ? 0 : directives.size();
        if (!CollectionUtils.isEmpty(directives)) {
            for (DayTypeBlockReverseSelectionDirective directive : directives) {
                if (Objects.isNull(directive) || directive.isSatisfied() || directive.isSuccess()) {
                    continue;
                }
                if (directive.isAttempted()) {
                    directive.setResultReason("预留机台在当前阶段未落地，自动释放并回退普通新增主链");
                } else {
                    directive.setResultReason("反选物料已由其他机台满足或当天无排产窗口，自动释放");
                }
                directive.setAttempted(true);
                this.appendDayTypeBlockReverseLog(context, directive, "未落地",
                        directive.getResultReason());
            }
        }
        context.clearDayTypeBlockReverseSelection();
        log.info("按天换活字块反选阶段收口, batchNo: {}, scheduleDate: {}, phase: {}, 指令数: {}",
                context.getBatchNo(), dayContext.getScheduleDate(),
                dayContext.getCurrentPhase(), directiveCount);
    }

    /**
     * 按天换活字块反选物料产品状态归一化。
     *
     * @param productStatus 原始产品状态
     * @return 非空产品状态；空值按正规 S 处理
     */
    private String normalizeDayTypeBlockProductStatus(String productStatus) {
        return StringUtils.isEmpty(productStatus) ? FORMAL_PRODUCT_STATUS : productStatus;
    }

    /**
     * 输出按天换活字块机台反选的应用日志与排程过程日志。
     *
     * @param context 排程上下文
     * @param directive 反选指令
     * @param result 结果状态（命中并锁定/成功/未落地）
     * @param reason 结果说明
     */
    private void appendDayTypeBlockReverseLog(
            LhScheduleContext context,
            DayTypeBlockReverseSelectionDirective directive,
            String result,
            String reason) {
        if (Objects.isNull(directive)) {
            return;
        }
        StringBuilder detailBuilder = new StringBuilder(320);
        detailBuilder.append("scheduleTargetDate=")
                .append(LhScheduleTimeUtil.formatDate(context.getScheduleTargetDate()))
                .append(", scheduleDate=").append(directive.getScheduleDate())
                .append(", machineCode=")
                .append(StringUtils.defaultString(directive.getMachineCode(), "-"))
                .append(", previousMaterialCode=")
                .append(StringUtils.defaultString(directive.getPreviousMaterialCode(), "-"))
                .append(", materialCode=")
                .append(StringUtils.defaultString(directive.getMaterialCode(), "-"))
                .append(", productStatus=")
                .append(StringUtils.defaultString(directive.getProductStatus(), "-"))
                .append(", matchedLayer=")
                .append(StringUtils.defaultString(directive.getMatchedLayer(), "-"))
                .append(", result=").append(StringUtils.defaultString(result, "-"))
                .append(", reason=").append(StringUtils.defaultString(reason, "-"));
        log.info("按天换活字块机台反选, {}", detailBuilder);
        PriorityTraceLogHelper.appendProcessLog(
                context, "按天换活字块机台反选", detailBuilder.toString());
    }

    /**
     * 构建当前业务日新增排产顺序日志分节并暂存，供三天窗口结束后统一合并输出。
     *
     * <p>该分节不受选机优先级跟踪开关控制，每个实际业务日固定生成一节；当天无可排 SKU 时，
     * 采集器输出固定空日说明。分节由调用方在三天窗口结束后合并成一条过程日志，
     * 本方法不直接写库、不新增数据库访问，也不改变排程事务边界。</p>
     *
     * @param dayContext 当前业务日编排上下文
     * @param newSpecOrderLogSections 按业务日先后追加的分节暂存列表
     */
    private void appendDailyNewSpecOrderLogSection(
            DayScheduleContext dayContext,
            List<String> newSpecOrderLogSections) {
        DailyNewSpecOrderLogCollector collector = dayContext.getNewSpecOrderLogCollector();
        String title = collector.buildTitle();
        String detail = collector.buildDetail();
        if (newSpecOrderLogSections != null) {
            newSpecOrderLogSections.add(title + "\n" + detail);
        }
        // 分节已持有完整字符串，立即释放轻量明细集合，避免跨后续业务日继续占用内存。
        collector.clear();
    }

    /**
     * 将三天窗口收集的新增排产顺序分节合并成单条过程日志。
     *
     * <p>合并后标题固定为“新增排产明细”，明细依次保留 T、T+1、T+2 各日标题及原明细，
     * 相邻业务日之间用一个空行分隔。该改动只调整日志呈现，
     * 不读取或修改新增排产业务数据。</p>
     *
     * @param context 排程上下文
     * @param newSpecOrderLogSections 按业务日先后排列的日志分节
     */
    private void appendMergedNewSpecOrderProcessLog(LhScheduleContext context,
                                                    List<String> newSpecOrderLogSections) {
        if (CollectionUtils.isEmpty(newSpecOrderLogSections)) {
            return;
        }
        String mergedDetail = String.join(
                NEW_SPEC_ORDER_LOG_SECTION_SEPARATOR, newSpecOrderLogSections);
        PriorityTraceLogHelper.appendProcessLog(
                context, NEW_SPEC_ORDER_MERGED_LOG_TITLE, mergedDetail);
    }

    /**
     * 记录新增 SKU 实际命中机台时的完整实时选机快照。
     *
     * <p>调用点位于主副结果、机台状态和跨日在机绑定全部提交之后；统计值在正式换模分配和
     * 当前结果写入前已经冻结，因此班次计划量、切换次数和结构机台数均不包含本次结果。
     * 候选描述保持正式选机主链的真实顺序，不执行展示层收尾时间重排。</p>
     *
     * @param context 排程上下文
     * @param sku 当前已命中的新增 SKU
     * @param machine 实际命中机台
     * @param result 当前实际命中的主结果
     * @param pairResult 单控整机配对侧结果；普通机台为空
     * @param realtimeSnapshot 选机前实时统计快照
     * @param realtimeMachineEndingText 命中机台选机前的前序 SKU 收尾明细
     * @param machineSelectionDescription 正式候选顺序及软排序描述
     */
    private void appendNewSpecSelectionRealtimeSnapshotLog(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            MachineScheduleDTO machine,
            LhScheduleResult result,
            LhScheduleResult pairResult,
            NewSpecSelectionRealtimeSnapshot realtimeSnapshot,
            String realtimeMachineEndingText,
            String machineSelectionDescription) {
        if (Objects.isNull(context) || Objects.isNull(sku) || Objects.isNull(machine)
                || Objects.isNull(realtimeSnapshot)) {
            return;
        }
        context.recordNewSpecRealtimeSelectionOrder(
                sku, realtimeSnapshot.getDateOffset(), realtimeSnapshot.getSelectionOrder());
        String selectionOrderText = context.buildNewSpecRealtimeSelectionOrderText(sku);
        // 过程日志与结果表共用同一次冻结快照，禁止为落库重新扫描或重新计算选机状态。
        this.fillNewSpecSelectionRealtimeFields(
                context, sku, result, pairResult, realtimeSnapshot,
                realtimeMachineEndingText, machineSelectionDescription, selectionOrderText);
        String earliestEmbryoAvailableTimeText =
                Objects.isNull(realtimeSnapshot.getEarliestEmbryoAvailableTime())
                        ? StringUtils.EMPTY
                        : LhScheduleTimeUtil.formatDateTime(
                                realtimeSnapshot.getEarliestEmbryoAvailableTime());
        String title = "【" + sku.getMaterialCode() + "】【"
                + StringUtils.defaultString(sku.getProductStatus()) + "】新增选机实时快照";
        StringBuilder detailBuilder = new StringBuilder(
                Math.max(768, StringUtils.length(machineSelectionDescription) + 512));
        detailBuilder.append("批次=").append(context.getBatchNo())
                .append("，工厂=").append(context.getFactoryCode())
                .append("，物料=").append(sku.getMaterialCode())
                .append("，产品状态=").append(sku.getProductStatus())
                .append("，结构=").append(sku.getStructureName())
                .append("，实际命中机台=").append(machine.getMachineCode())
                .append('\n')
                .append("最早胎胚可供硫化时间=").append(earliestEmbryoAvailableTimeText)
                .append('\n')
                .append("实时机台收尾时间=")
                .append(StringUtils.defaultString(realtimeMachineEndingText))
                .append('\n')
                .append("实时班次总计划量=")
                .append(realtimeSnapshot.getRealtimeShiftTotalPlanQty())
                .append('\n')
                .append("实时班次换模/换活字块次数=")
                .append(realtimeSnapshot.getRealtimeShiftChangeCount())
                .append('\n')
                .append("实时结构已排硫化机台数=")
                .append(realtimeSnapshot.getRealtimeStructureMachineCount())
                .append('\n')
                .append("SKU选机描述=")
                .append(StringUtils.defaultString(machineSelectionDescription))
                .append('\n')
                .append("SKU实时选机顺序=")
                .append(StringUtils.defaultString(selectionOrderText));
        PriorityTraceLogHelper.appendProcessLog(context, title, detailBuilder.toString());
    }

    /**
     * 将新增选机实时快照回写到当前主副结果，并刷新同一 SKU 已落地新增结果的跨日选机顺序。
     *
     * @param context 排程上下文
     * @param sku 当前新增 SKU
     * @param result 当前主结果
     * @param pairResult 单控整机配对侧结果
     * @param realtimeSnapshot 选机前实时统计快照
     * @param realtimeMachineEndingText 命中机台前序 SKU 收尾明细
     * @param machineSelectionDescription 正式候选选机描述
     * @param selectionOrderText 当前 SKU 已命中日期的累计顺序文本
     */
    private void fillNewSpecSelectionRealtimeFields(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LhScheduleResult result,
            LhScheduleResult pairResult,
            NewSpecSelectionRealtimeSnapshot realtimeSnapshot,
            String realtimeMachineEndingText,
            String machineSelectionDescription,
            String selectionOrderText) {
        this.fillSingleNewSpecSelectionRealtimeFields(
                result, realtimeSnapshot, realtimeMachineEndingText,
                machineSelectionDescription, selectionOrderText);
        this.fillSingleNewSpecSelectionRealtimeFields(
                pairResult, realtimeSnapshot, realtimeMachineEndingText,
                machineSelectionDescription, selectionOrderText);
        if (Objects.nonNull(result)) {
            context.getNewSpecRealtimeSnapshotResultSet().add(result);
        }
        if (Objects.nonNull(pairResult)) {
            context.getNewSpecRealtimeSnapshotResultSet().add(pairResult);
        }
        /*
         * 同一 SKU 跨 T/T+1/T+2 再次命中时，统一刷新之前已落地的新增快照结果；
         * 只遍历身份集合，不扫描全部排程结果，也不触碰续作和 S4.4 换活字块结果。
         */
        for (LhScheduleResult snapshotResult : context.getNewSpecRealtimeSnapshotResultSet()) {
            if (context.getScheduleResultSourceSkuMap().get(snapshotResult) == sku) {
                snapshotResult.setSkuRealtimeSelectionOrder(selectionOrderText);
            }
        }
    }

    /**
     * 回写单条新增排产结果的实时快照字段。
     *
     * @param result 待回写结果；允许为空
     * @param realtimeSnapshot 选机前实时统计快照
     * @param realtimeMachineEndingText 命中机台前序 SKU 收尾明细
     * @param machineSelectionDescription 正式候选选机描述
     * @param selectionOrderText 当前 SKU 跨日选机顺序
     */
    private void fillSingleNewSpecSelectionRealtimeFields(
            LhScheduleResult result,
            NewSpecSelectionRealtimeSnapshot realtimeSnapshot,
            String realtimeMachineEndingText,
            String machineSelectionDescription,
            String selectionOrderText) {
        if (Objects.isNull(result) || Objects.isNull(realtimeSnapshot)) {
            return;
        }
        result.setEarliestEmbryoAvailableTime(
                realtimeSnapshot.getEarliestEmbryoAvailableTime());
        result.setRealtimeMachineEndingInfo(realtimeMachineEndingText);
        result.setRealtimeShiftTotalPlanQty(
                realtimeSnapshot.getRealtimeShiftTotalPlanQty());
        result.setRealtimeShiftChangeoverCount(
                realtimeSnapshot.getRealtimeShiftChangeCount());
        result.setRealtimeStructureScheduledMachineCount(
                realtimeSnapshot.getRealtimeStructureMachineCount());
        result.setSkuMachineSelectionDesc(machineSelectionDescription);
        result.setSkuRealtimeSelectionOrder(selectionOrderText);
    }

    /**
     * 记录当前 SKU 本次真实进入新增排产主循环的顺序。
     *
     * <p>调用点必须位于 SKU 前置过滤、正式机台候选匹配、当前日加机生效日期和
     * dayN 机台上限全部放行之后；无正式候选或被上述规则过滤的 SKU 不调用本方法。
     * 同一 SKU 跨阶段或跨轮次再次进入会创建新明细，不按物料编码去重。</p>
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日及实际阶段
     * @param sku 当前真实参与排产的 SKU
     * @return 当前遍历对应的可回填日志明细
     */
    private DailyNewSpecOrderLogEntry recordDailyNewSpecOrder(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            SkuScheduleDTO sku) {
        // 日志与正式增机判断读取同一份统一Map，展示的是当前自然日目标总机台数而非新增缺口数。
        LocalDate requiredMachineCountDate = this.resolveRequiredMachineCountDate(
                context, sku, dayContext.getScheduleDate());
        int initialRequiredMachineCount = this.lhDailyMouldCalcService.getRequiredMachineCount(
                context, sku.getMaterialCode(), sku.getProductStatus(), requiredMachineCountDate);
        return dayContext.getNewSpecOrderLogCollector().record(
                sku.getMaterialCode(),
                dayContext.getCurrentPhase(),
                sku.getSourceType(),
                this.resolveOriginalNewSpecDayPlanQty(
                        context, sku, dayContext.getScheduleDate()),
                this.isStructureEarlyProduction(context, sku),
                initialRequiredMachineCount);
    }

    /**
     * 解析统一Map目标机台数的业务来源日。
     * <p>普通SKU使用当前排程自然日；提前生产SKU必须使用中心运行视图记录的未来计划来源日，
     * 避免当前提前日原计划为0时把未来计划目标机台数误读为0。该方法只改变Map查询日期，
     * 不修改提前生产准入、实际开产日、班次或日计划扣账。</p>
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param defaultDate 默认自然日
     * @return 统一Map目标机台数查询日期
     */
    private LocalDate resolveRequiredMachineCountDate(LhScheduleContext context,
                                                      SkuScheduleDTO sku,
                                                      LocalDate defaultDate) {
        if (Objects.isNull(context) || Objects.isNull(sku)) {
            return defaultDate;
        }
        EarlyProductionRuntimePlan runtimePlan = context.getEarlyProductionRuntimePlan(sku);
        if (Objects.nonNull(runtimePlan) && runtimePlan.isActive()
                && Objects.nonNull(runtimePlan.getFuturePlanDate())) {
            return runtimePlan.getFuturePlanDate();
        }
        return defaultDate;
    }

    /**
     * 判断当前 SKU 是否命中现有结构类提前生产决策。
     *
     * <p>这里只读取提前生产中心运行视图中已经形成的判定，不根据当前阶段或未来计划日期重新推导。
     * 普通提前生产仍记录“否”，只有已允许的结构切换、结构收尾提前生产记录“是”。</p>
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @return true-结构提前；false-非结构提前
     */
    private boolean isStructureEarlyProduction(LhScheduleContext context, SkuScheduleDTO sku) {
        EarlyProductionRuntimePlan runtimePlan = context.getEarlyProductionRuntimePlan(sku);
        if (Objects.isNull(runtimePlan) || Objects.isNull(runtimePlan.getDecision())) {
            return false;
        }
        EarlyProductionDecision decision = runtimePlan.getDecision();
        if (!decision.isEarlyProduction() || !decision.isAllowed()) {
            return false;
        }
        return StringUtils.equals(
                EarlyProductionDecision.SCENE_STRUCTURE_SWITCH, decision.getSceneType())
                || StringUtils.equals(
                EarlyProductionDecision.SCENE_STRUCTURE_ENDING, decision.getSceneType());
    }

    /**
     * 解析 SKU 选机日志展示的“类型线上”取值。
     *
     * <p>结构提前优先于来源类型：命中结构切换/结构收尾提前生产时显示“结构提前新增”；
     * 续作加机台候选显示“续作新增”；其余普通完全新增候选显示“完全新增”。
     * 与“新增排产明细”复用同一套来源类型和结构提前判定，仅服务日志展示。</p>
     *
     * @param context 排程上下文
     * @param sku 当前选机 SKU
     * @return SKU 类型展示值：完全新增 / 续作新增 / 结构提前新增
     */
    private String resolveTraceSkuType(LhScheduleContext context, SkuScheduleDTO sku) {
        if (this.isStructureEarlyProduction(context, sku)) {
            return "结构提前新增";
        }
        if (SkuScheduleSourceTypeEnum.isContinuationAddMachine(sku.getSourceType())) {
            return "续作新增";
        }
        return "完全新增";
    }

    /**
     * 读取当前计划日可复用的结构切换提前判定。
     *
     * <p>前一业务日只完成准入判断和延期，不选机、不锁机；到未来计划日的正常资源竞争阶段，
     * 先沿用现有排序选出真实候选机台，再用此前冻结的准入结论判断该机台能否回看前一业务日
     * 安排换模。这样既不会提前占用机台改变全局选机顺序，也不会因当前日已有计划量而丢失
     * “结构由0台切换为有计划机台”的原始业务事实。</p>
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日上下文
     * @param sku 当前 SKU
     * @param earliestEmbryoAvailableTime 最早胎胚可供时间
     * @return 可复用的结构切换提前判定；不满足条件时返回 null
     */
    private EarlyProductionDecision resolveStructureSwitchLookbackDecision(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            SkuScheduleDTO sku,
            Date earliestEmbryoAvailableTime) {
        if (Objects.isNull(context) || Objects.isNull(dayContext) || Objects.isNull(sku)
                || Objects.isNull(earliestEmbryoAvailableTime)
                || dayContext.getCurrentPhase()
                != DailySchedulePhase.NORMAL_RESOURCE_COMPETITION
                || CollectionUtils.isEmpty(context.getScheduleWindowShifts())) {
            return null;
        }
        EarlyProductionRuntimePlan runtimePlan =
                context.getEarlyProductionRuntimePlan(sku);
        EarlyProductionDecision decision = Objects.isNull(runtimePlan)
                ? null : runtimePlan.getDecision();
        LocalDate currentDate = dayContext.getScheduleDate();
        if (Objects.isNull(runtimePlan) || !runtimePlan.isActive()
                || Objects.isNull(runtimePlan.getCurrentDate())
                || !runtimePlan.getCurrentDate().plusDays(1).equals(currentDate)
                || Objects.isNull(decision) || !decision.isEarlyProduction()
                || !decision.isAllowed()
                || !currentDate.equals(decision.getFuturePlanDate())
                || resolveOriginalNewSpecDayPlanQty(context, sku, currentDate) <= 0
                || !EarlyProductionChecker.isStructureSwitchEarlyProduction(
                        context, sku, runtimePlan.getCurrentDate(),
                        decision.getFuturePlanDate())) {
            return null;
        }
        LocalDate embryoBusinessDate = resolveProductionWorkDate(
                context.getScheduleWindowShifts(), earliestEmbryoAvailableTime);
        return currentDate.equals(embryoBusinessDate) ? decision : null;
    }

    /**
     * 判断选定机台的实际换模时间轴是否命中“前一业务日换模、当前计划日生产”。
     *
     * <p>只放宽换模开始时间的当前日窗口限制；换模仍必须位于完整八班窗口内、早于胎胚可供
     * 时间并在当前业务日日终前完成。停机、晚班禁换模、每日换模次数、首检资源和生产产能
     * 仍由原分配链路校验。</p>
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日上下文
     * @param earliestEmbryoAvailableTime 最早胎胚可供时间
     * @param mouldChangeStartTime 换模开始时间
     * @param mouldChangeCompleteTime 换模完成时间
     * @return true-命中跨日回看换模；false-执行原当前日窗口规则
     */
    private boolean isStructureSwitchLookbackTimeline(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            Date earliestEmbryoAvailableTime,
            Date mouldChangeStartTime,
            Date mouldChangeCompleteTime) {
        if (Objects.isNull(context) || Objects.isNull(dayContext)
                || Objects.isNull(earliestEmbryoAvailableTime)
                || Objects.isNull(mouldChangeStartTime)
                || Objects.isNull(mouldChangeCompleteTime)
                || !mouldChangeStartTime.before(mouldChangeCompleteTime)
                || !mouldChangeStartTime.before(earliestEmbryoAvailableTime)
                || !mouldChangeStartTime.before(dayContext.getDayStartTime())
                || dayContext.reachesOrPassesDayEnd(mouldChangeCompleteTime)) {
            return false;
        }
        List<LhShiftConfigVO> windowShifts = context.getScheduleWindowShifts();
        Date windowStartTime = resolveScheduleWindowStartTime(context, windowShifts);
        LocalDate switchBusinessDate = resolveProductionWorkDate(
                windowShifts, mouldChangeStartTime);
        return Objects.nonNull(windowStartTime)
                && !mouldChangeStartTime.before(windowStartTime)
                && dayContext.getScheduleDate().minusDays(1).equals(switchBusinessDate);
    }

    /**
     * 判断选中机台是否允许使用排程窗口内的历史空闲时间提前完成生产准备。
     *
     * <p>该判断只决定换模/换活字块分配器是否可以查看当前业务日前的班次，不直接预占资源，
     * 也不改变候选顺序。必须同时满足：</p>
     * <ul>
     *   <li>机台合法切换就绪时间早于当前业务日开始，且不早于完整排程窗口开始；</li>
     *   <li>本次切换耗时大于0，并可在当前生产业务日日终前完成。</li>
     * </ul>
     *
     * <p>生产门禁不再参与回看判定：正规、小批量 SKU 已无 SKU 类型门禁，胎胚可供时间仅作为
     * 正式开产下限，不得反过来禁止提前换模。这里只做常数次 Date 比较，候选机台仍逐台按现有
     * 分配链校验停机、晚班禁换模、换模配额和首检资源，不建立跨机台时间矩阵。</p>
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日上下文
     * @param switchReadyTime 经过机台就绪、定点和开产模式收口后的合法切换时间
     * @param switchDurationHours 本次换模或换活字块耗时
     * @param productionNotBeforeTime 当前增机业务日或SKU门禁确定的正式生产下限
     * @return true-允许查看窗口内更早班次安排准备；false-仅使用当前业务日班次
     */
    private boolean isProductionPreparationLookbackAllowed(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            Date switchReadyTime,
            int switchDurationHours,
            Date productionNotBeforeTime) {
        if (Objects.isNull(context) || Objects.isNull(dayContext)
                || Objects.isNull(switchReadyTime)
                || switchDurationHours <= 0
                || Objects.isNull(dayContext.getDayStartTime())
                || Objects.isNull(dayContext.getDayEndTime())
                || !switchReadyTime.before(dayContext.getDayStartTime())
                || (Objects.nonNull(productionNotBeforeTime)
                && !switchReadyTime.before(productionNotBeforeTime))) {
            return false;
        }
        Date theoreticalSwitchCompleteTime = LhScheduleTimeUtil.addHours(
                switchReadyTime, switchDurationHours);
        return this.isProductionPreparationLookbackWithinDay(
                context, dayContext, switchReadyTime, theoreticalSwitchCompleteTime);
    }

    /**
     * 解析生产日前准备必须贴近的正式生产下限。
     *
     * <p>续作补偿或普通新增机台存在明确增机业务日时，优先使用该业务日首个生产班次；
     * 普通新增首台没有独立增机日期时，使用当前生产业务日首班。SKU 类型门禁更晚时再取较晚值。
     * 该时间只用于把换模准备延后到最后合法窗口，不改变 dayN 增机日期、正式开产门禁、
     * 胎胚可供时间或候选机台排序。</p>
     *
     * @param context 排程上下文
     * @param dayContext 当前生产业务日上下文
     * @param shifts 当前调用方班次切片
     * @param candidateProductionNotBeforeTime SKU 类型门禁确定的候选生产下限
     * @param addMachineProductionDate 当前机台首次允许增机的业务日期
     * @return 生产日前准备对齐下限
     */
    private Date resolveProductionPreparationNotBeforeTime(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            List<LhShiftConfigVO> shifts,
            Date candidateProductionNotBeforeTime,
            LocalDate addMachineProductionDate) {
        List<LhShiftConfigVO> windowShifts = Objects.isNull(context)
                || CollectionUtils.isEmpty(context.getScheduleWindowShifts())
                ? shifts : context.getScheduleWindowShifts();
        Date addMachineProductionStartTime = Objects.isNull(addMachineProductionDate)
                ? (Objects.isNull(dayContext) ? null : dayContext.getDayStartTime())
                : resolveFirstShiftStartTime(windowShifts, addMachineProductionDate);
        return this.resolveLaterTime(
                candidateProductionNotBeforeTime, addMachineProductionStartTime);
    }

    /**
     * 解析释放续作机台原样重新启用的生产起点。
     *
     * <p>续作加机台仍在目标业务日参加统一SKU排序和选机；当轮最终选回原释放机台且整套模具
     * 未变化时，只豁免换模、换活字块和首检，不豁免增机生效日期。重新启用起点必须同时满足
     * 机台真实就绪时间、当前业务日首班和 {@code firstAddMachineProductionDate} 首班下限。
     * 以2026-08-21为首次增机业务日时，最早只能从class3（2026-08-20 22:00）继续续作，
     * 不得借用T日class2产能。</p>
     *
     * @param context 排程上下文
     * @param dayContext 当前目标业务日上下文
     * @param sku 当前续作加机台补偿SKU
     * @param machineReadyTime 原续作机台真实就绪时间
     * @return 允许重新启用的起点；当前业务日已无可排时间时返回null
     */
    private Date resolveReleasedContinuationReuseStartTime(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            SkuScheduleDTO sku,
            Date machineReadyTime) {
        if (Objects.isNull(context) || Objects.isNull(dayContext)
                || Objects.isNull(sku) || Objects.isNull(machineReadyTime)
                || Objects.isNull(dayContext.getDayStartTime())
                || Objects.isNull(dayContext.getDayEndTime())) {
            return null;
        }
        Date reuseStartTime = this.resolveLaterTime(
                machineReadyTime, dayContext.getDayStartTime());
        reuseStartTime = this.alignAddedMachineProductionStartTime(
                sku, reuseStartTime, context.getScheduleWindowShifts(), 0,
                sku.getFirstAddMachineProductionDate());
        return Objects.nonNull(reuseStartTime)
                && reuseStartTime.before(dayContext.getDayEndTime())
                ? reuseStartTime : null;
    }

    /**
     * 构建续作重新启用本轮允许写入的班次切片。
     *
     * <p>同物料同模具只代表无需切换，不代表可以提前生产。当前业务日只允许写入本日班次，
     * 后续业务日继续由现有跨日续排追加，禁止把增机生效日前一班带入本轮。</p>
     *
     * @param dayContext 当前业务日上下文
     * @return 当前业务日班次副本
     */
    private List<LhShiftConfigVO> resolveReleasedContinuationReuseShifts(
            DayScheduleContext dayContext) {
        List<LhShiftConfigVO> resultShifts = new ArrayList<LhShiftConfigVO>(4);
        if (Objects.nonNull(dayContext)
                && !CollectionUtils.isEmpty(dayContext.getDayShifts())) {
            resultShifts.addAll(dayContext.getDayShifts());
        }
        return resultShifts;
    }

    /**
     * 校验分配后的真实时间轴是否属于“当前业务日前准备、当前业务日内完成”。
     *
     * <p>预判放行后，换模分配器仍可能因晚班禁换模、停机或配额把开始时间顺延；因此日窗口
     * 守卫必须使用实际开始/完成时间再次确认。准备可以落在窗口内任意更早业务日，不限定只回看
     * 前一天；完成时间必须早于当前业务日日终，且开始时间不能越过完整排程窗口起点。</p>
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日上下文
     * @param productionNotBeforeTime 当前增机业务日或SKU门禁确定的正式生产下限
     * @param mouldChangeStartTime 实际准备开始时间
     * @param mouldChangeCompleteTime 实际准备完成时间
     * @return true-真实时间轴允许跨日提交；false-仍执行普通当前日窗口守卫
     */
    private boolean isProductionPreparationLookbackTimeline(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            Date productionNotBeforeTime,
            Date mouldChangeStartTime,
            Date mouldChangeCompleteTime) {
        if (Objects.isNull(context) || Objects.isNull(dayContext)
                || Objects.isNull(mouldChangeStartTime)
                || Objects.isNull(mouldChangeCompleteTime)
                || Objects.isNull(dayContext.getDayStartTime())
                || Objects.isNull(dayContext.getDayEndTime())
                || (Objects.nonNull(productionNotBeforeTime)
                && !mouldChangeStartTime.before(productionNotBeforeTime))) {
            return false;
        }
        return this.isProductionPreparationLookbackWithinDay(
                context, dayContext, mouldChangeStartTime, mouldChangeCompleteTime);
    }

    /**
     * 校验跨日生产准备的统一时间边界。
     *
     * <p>换模或换活字块只要在完整排程窗口内合法开始，并在目标生产业务日日终前完成，
     * 就允许占用生产日前的空闲时间。SKU 生产门禁不再作为准备完成上限，避免机台在中班收尾后
     * 本可连续换模，却被整体顺延到目标业务日早班；胎胚可供时间与 X/T 中班下限仍由后续
     * 正式开产时间计算负责。</p>
     *
     * @param context 排程上下文
     * @param dayContext 当前生产业务日上下文
     * @param preparationStartTime 实际或理论准备开始时间
     * @param preparationCompleteTime 实际或理论准备完成时间
     * @return true-准备时间轴落在允许的跨日边界内；false-不得跨日提交
     */
    private boolean isProductionPreparationLookbackWithinDay(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            Date preparationStartTime,
            Date preparationCompleteTime) {
        if (Objects.isNull(context) || Objects.isNull(dayContext)
                || Objects.isNull(preparationStartTime)
                || Objects.isNull(preparationCompleteTime)
                || Objects.isNull(dayContext.getDayStartTime())
                || Objects.isNull(dayContext.getDayEndTime())
                || !preparationStartTime.before(preparationCompleteTime)
                || !preparationStartTime.before(dayContext.getDayStartTime())
                || dayContext.reachesOrPassesDayEnd(preparationCompleteTime)) {
            return false;
        }
        Date windowStartTime = this.resolveScheduleWindowStartTime(
                context, context.getScheduleWindowShifts());
        return Objects.nonNull(windowStartTime)
                && !preparationStartTime.before(windowStartTime);
    }

    /**
     * 记录结构切换跨日换模最终落地时间轴。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日上下文
     * @param sku 当前 SKU
     * @param machine 落地机台
     * @param machineReadyTime 机台真实空闲时间
     * @param mouldChangeStartTime 换模开始时间
     * @param mouldChangeCompleteTime 换模完成时间
     * @param earliestEmbryoAvailableTime 最早胎胚可供时间
     * @param firstProductionStartTime 实际首个生产时间
     * @param firstInspectionAttributionShift 首检归属班次
     */
    private void appendStructureSwitchLookbackProcessLog(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            SkuScheduleDTO sku,
            MachineScheduleDTO machine,
            Date machineReadyTime,
            Date mouldChangeStartTime,
            Date mouldChangeCompleteTime,
            Date earliestEmbryoAvailableTime,
            Date firstProductionStartTime,
            LhShiftConfigVO firstInspectionAttributionShift) {
        String detail = new StringBuilder(384)
                .append("批次=").append(context.getBatchNo())
                .append("，计划业务日=").append(dayContext.getScheduleDate())
                .append("，物料=").append(sku.getMaterialCode())
                .append("，机台=").append(machine.getMachineCode())
                .append("，机台真实空闲=")
                .append(LhScheduleTimeUtil.formatDateTime(machineReadyTime))
                .append("，换模开始=")
                .append(LhScheduleTimeUtil.formatDateTime(mouldChangeStartTime))
                .append("，换模完成=")
                .append(LhScheduleTimeUtil.formatDateTime(mouldChangeCompleteTime))
                .append("，胎胚最早可供=")
                .append(LhScheduleTimeUtil.formatDateTime(earliestEmbryoAvailableTime))
                .append("，实际开产=")
                .append(LhScheduleTimeUtil.formatDateTime(firstProductionStartTime))
                .append("，首检归属=class")
                .append(Objects.isNull(firstInspectionAttributionShift)
                        ? null : firstInspectionAttributionShift.getShiftIndex())
                .toString();
        log.info("新增SKU结构切换跨日换模已落地, {}", detail);
        PriorityTraceLogHelper.appendProcessLog(
                context, "结构切换提前跨日换模", detail);
    }

    /**
     * 记录普通生产日前跨日准备最终落地时间轴。
     *
     * <p>只有候选已形成有效排产结果后才调用，失败试排不会写入过程日志。日志同时记录机台
     * 空闲、准备开始/完成、统一生产门禁、实际开产和首检班次，可直接核对“提前换模但不提前
     * 生产”是否成立。</p>
     *
     * @param context 排程上下文
     * @param dayContext 当前生产业务日
     * @param sku 当前 SKU
     * @param machine 最终落地机台
     * @param machineReadyTime 机台真实空闲时间
     * @param mouldChangeStartTime 准备开始时间
     * @param mouldChangeCompleteTime 准备完成时间
     * @param preparationProductionNotBeforeTime 增机业务日和SKU门禁共同确定的准备对齐生产下限
     * @param productionNotBeforeTime 正式生产门禁，包含胎胚最早可供时间
     * @param firstProductionStartTime 实际开产时间
     * @param firstInspectionAttributionShift 首检归属班次
     */
    private void appendProductionPreparationLookbackProcessLog(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            SkuScheduleDTO sku,
            MachineScheduleDTO machine,
            Date machineReadyTime,
            Date mouldChangeStartTime,
            Date mouldChangeCompleteTime,
            Date preparationProductionNotBeforeTime,
            Date productionNotBeforeTime,
            Date firstProductionStartTime,
            LhShiftConfigVO firstInspectionAttributionShift) {
        String detail = new StringBuilder(384)
                .append("批次=").append(context.getBatchNo())
                .append("，生产业务日=").append(dayContext.getScheduleDate())
                .append("，物料=").append(sku.getMaterialCode())
                .append("，产品状态=").append(sku.getProductStatus())
                .append("，机台=").append(machine.getMachineCode())
                .append("，机台真实空闲=")
                .append(LhScheduleTimeUtil.formatDateTime(machineReadyTime))
                .append("，准备开始=")
                .append(LhScheduleTimeUtil.formatDateTime(mouldChangeStartTime))
                .append("，准备完成=")
                .append(LhScheduleTimeUtil.formatDateTime(mouldChangeCompleteTime))
                .append("，准备对齐生产下限=")
                .append(LhScheduleTimeUtil.formatDateTime(
                        preparationProductionNotBeforeTime))
                .append("，生产门禁=")
                .append(LhScheduleTimeUtil.formatDateTime(productionNotBeforeTime))
                .append("，实际开产=")
                .append(LhScheduleTimeUtil.formatDateTime(firstProductionStartTime))
                .append("，首检归属=class")
                .append(Objects.isNull(firstInspectionAttributionShift)
                        ? null : firstInspectionAttributionShift.getShiftIndex())
                .toString();
        log.info("新增SKU生产日前跨日准备已落地, {}", detail);
        PriorityTraceLogHelper.appendProcessLog(
                context, "新增SKU生产日前跨日准备", detail);
    }

    /**
     * 使用现有动态扩机结果回填当前业务日目标机台数。
     *
     * <p>{@code requiredMachineCount} 是真实排产规则已经计算出的当前新增阶段目标数；
     * 若 dayN 模拟同时给出了各新增机台生效日期，只统计生效日期不晚于当前业务日的机台，
     * 防止把 T+1、T+2 才允许启用的机台提前记入当天。日期列表为空表示现有规则允许本日
     * 连续竞争这些机台，直接使用完整目标数。</p>
     *
     * @param dayContext 当前业务日
     * @param entry 当前遍历对应日志明细
     * @param requiredMachineCount 现有规则计算的目标机台数
     * @param addMachineProductionDateList 各追加机台现有生效日期列表
     */
    private void updateDailyRequiredMachineCount(
            DayScheduleContext dayContext,
            DailyNewSpecOrderLogEntry entry,
            int requiredMachineCount,
            List<LocalDate> addMachineProductionDateList) {
        if (Objects.isNull(entry) || requiredMachineCount <= 0) {
            return;
        }
        int currentDayRequiredMachineCount = requiredMachineCount;
        if (!CollectionUtils.isEmpty(addMachineProductionDateList)) {
            // 当前候选机台是本轮首台；日期列表只包含模拟中后续新增机台的生效业务日。
            currentDayRequiredMachineCount = 1;
            for (LocalDate productionDate : addMachineProductionDateList) {
                if (Objects.nonNull(productionDate)
                        && !productionDate.isAfter(dayContext.getScheduleDate())) {
                    currentDayRequiredMachineCount++;
                }
            }
            currentDayRequiredMachineCount = Math.min(
                    requiredMachineCount, currentDayRequiredMachineCount);
        }
        entry.updateRequiredMachineCount(currentDayRequiredMachineCount);
    }

    /**
     * 执行当前业务日指定候选阶段。
     *
     * <p>候选列表形成后按当前业务日待排集合重新执行 S4.5 统一排序。阶段执行使用临时工作队列，
     * 当前日暂时失败只从工作队列移除并登记延期，不锁定前一日名次、目标班次或候选机台，
     * 也不删除结构待排视图、不提前写最终未排。</p>
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param state 日驱动状态
     * @param phase 当前阶段
     * @param machineMatch 机台匹配策略
     * @param mouldChangeBalance 换模均衡策略
     * @param inspectionBalance 首检均衡策略
     * @param capacityCalculate 产能计算策略
     * @param unscheduledReasonCountMap 最终未排原因计数
     * @return 当前阶段新形成有效上机结果数量
     */
    private int scheduleDailyCandidatePhase(LhScheduleContext context,
                                            DayScheduleContext dayContext,
                                            DayDrivenScheduleState state,
                                            DailySchedulePhase phase,
                                            IMachineMatchStrategy machineMatch,
                                            IMouldChangeBalanceStrategy mouldChangeBalance,
                                            IFirstInspectionBalanceStrategy inspectionBalance,
                                            ICapacityCalculateStrategy capacityCalculate,
                                            Map<String, Integer> unscheduledReasonCountMap) {
        return scheduleDailyCandidatePhase(
                context, dayContext, state, phase, machineMatch, mouldChangeBalance,
                inspectionBalance, capacityCalculate, unscheduledReasonCountMap, false);
    }

    /**
     * 执行当前业务日指定候选阶段。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param state 日驱动状态
     * @param phase 当前阶段
     * @param machineMatch 机台匹配策略
     * @param mouldChangeBalance 换模均衡策略
     * @param inspectionBalance 首检均衡策略
     * @param capacityCalculate 产能策略
     * @param unscheduledReasonCountMap 最终未排原因计数
     * @param legacyNoFutureOnly true-仅执行无未来计划的历史欠产/收尾遗留任务
     * @return 当前阶段新增有效结果数量
     */
    private int scheduleDailyCandidatePhase(LhScheduleContext context,
                                            DayScheduleContext dayContext,
                                            DayDrivenScheduleState state,
                                            DailySchedulePhase phase,
                                            IMachineMatchStrategy machineMatch,
                                            IMouldChangeBalanceStrategy mouldChangeBalance,
                                            IFirstInspectionBalanceStrategy inspectionBalance,
                                            ICapacityCalculateStrategy capacityCalculate,
                                            Map<String, Integer> unscheduledReasonCountMap,
                                            boolean legacyNoFutureOnly) {
        dayContext.setCurrentPhase(phase);
        List<DailyNewSpecCandidate> candidateList =
                buildDailyCandidateList(context, dayContext, state, phase, legacyNoFutureOnly);
        if (CollectionUtils.isEmpty(candidateList)) {
            return 0;
        }

        List<SkuScheduleDTO> workingSkuList =
                new ArrayList<SkuScheduleDTO>(candidateList.size());
        for (DailyNewSpecCandidate candidate : candidateList) {
            workingSkuList.add(candidate.getSku());
            state.clearDeferredTask(candidate.getSku());
        }
        /*
         * 每个业务日、每个既有业务阶段开始前都重新执行统一 SKU 排序。延期 SKU 只保留
         * 类型、优先级基础属性及剩余目标量，不继承前一日 sortRank；排序完成后才把工作队列
         * 写回上下文，保证后续迭代顺序和过程日志读取的是同一份本日快照。
         */
        this.skuPriorityStrategy.sortNewSpecByPriority(context, workingSkuList);
        /*
         * 正常资源竞争阶段把按天换活字块反选命中的物料前置到工作队列：
         * 命中物料之间仍保持 S4.5 相对优先级，未命中物料顺序不变；
         * 该动作只调整“优先处理顺序”，不改变 SKU 类型、目标量和选机规则。
         */
        if (phase == DailySchedulePhase.NORMAL_RESOURCE_COMPETITION
                || phase == DailySchedulePhase.EARLY_PRODUCTION) {
            prependDayTypeBlockMatchedSkus(context, workingSkuList);
        }
        context.getNewSpecSkuList().clear();
        context.getNewSpecSkuList().addAll(workingSkuList);
        refreshPendingNewSpecSkuTypeCounts(context);

        log.info("新增排产按日阶段开始, batchNo: {}, scheduleDate: {}, phase: {}, candidateCount: {}, "
                        + "candidateMaterials: {}",
                context.getBatchNo(), dayContext.getScheduleDate(), phase, candidateList.size(),
                context.getNewSpecSkuList().stream()
                        .filter(Objects::nonNull)
                        .map(SkuScheduleDTO::getMaterialCode)
                        .collect(Collectors.toList()));

        int scheduledCount = schedulePendingNewSpecs(
                context, machineMatch, mouldChangeBalance, inspectionBalance,
                capacityCalculate, dayContext, state, unscheduledReasonCountMap);
        /*
         * 动态补偿 SKU 若由现有主链在本阶段产生，登记到窗口稳定队列。有有效 sortRank 时
         * 后续轮次及后续业务日继续按 S4.5 全局名次竞争；无名次时稳定排在有名次候选之后。
         */
        for (SkuScheduleDTO sku : context.getNewSpecSkuList()) {
            state.registerPendingSku(sku);
        }
        finalizeDailyCandidateState(context, dayContext, state, candidateList);
        return scheduledCount;
    }

    /**
     * 构建当前业务日指定阶段候选，保持原 SKU 排序不变。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param state 日驱动状态
     * @param phase 当前阶段
     * @return 当前阶段候选列表
     */
    private List<DailyNewSpecCandidate> buildDailyCandidateList(LhScheduleContext context,
                                                                DayScheduleContext dayContext,
                                                                DayDrivenScheduleState state,
                                                                DailySchedulePhase phase) {
        return buildDailyCandidateList(context, dayContext, state, phase, false);
    }

    /**
     * 构建当前业务日指定阶段候选。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param state 日驱动状态
     * @param phase 当前阶段
     * @param legacyNoFutureOnly true-仅构建无未来计划的历史欠产/收尾遗留任务
     * @return 保持原排序的候选列表
     */
    private List<DailyNewSpecCandidate> buildDailyCandidateList(LhScheduleContext context,
                                                                DayScheduleContext dayContext,
                                                                DayDrivenScheduleState state,
                                                                DailySchedulePhase phase,
                                                                boolean legacyNoFutureOnly) {
        List<DailyNewSpecCandidate> candidateList = new ArrayList<DailyNewSpecCandidate>();
        for (SkuScheduleDTO sku : state.getOrderedSkuList()) {
            if (!state.isPending(sku)
                    || state.isCompleted(sku)
                    || state.isFinalUnscheduled(sku)) {
                continue;
            }
            DailyNewSpecCandidate candidate =
                    buildDailyCandidate(
                            context, dayContext, state, phase, sku, legacyNoFutureOnly);
            if (Objects.nonNull(candidate)) {
                candidateList.add(candidate);
            }
        }
        return candidateList;
    }

    /**
     * 判断单个 SKU 是否进入当前业务日指定阶段。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param state 日驱动状态
     * @param phase 当前阶段
     * @param sku 待判断 SKU
     * @return 命中时返回候选；否则返回 null
     */
    private DailyNewSpecCandidate buildDailyCandidate(LhScheduleContext context,
                                                      DayScheduleContext dayContext,
                                                      DayDrivenScheduleState state,
                                                      DailySchedulePhase phase,
                                                      SkuScheduleDTO sku) {
        return buildDailyCandidate(context, dayContext, state, phase, sku, false);
    }

    /**
     * 判断单个 SKU 是否进入当前业务日指定阶段。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param state 日驱动状态
     * @param phase 当前阶段
     * @param sku 待判断 SKU
     * @param legacyNoFutureOnly true-仅判断无未来计划的历史欠产/收尾遗留任务
     * @return 命中时返回候选；否则返回 null
     */
    private DailyNewSpecCandidate buildDailyCandidate(LhScheduleContext context,
                                                      DayScheduleContext dayContext,
                                                      DayDrivenScheduleState state,
                                                      DailySchedulePhase phase,
                                                      SkuScheduleDTO sku,
                                                      boolean legacyNoFutureOnly) {
        LocalDate scheduleDate = dayContext.getScheduleDate();
        int currentDayRemainingQty = resolveDailyPlanRemainingQty(sku, scheduleDate);
        boolean currentDayPlanAllowed = currentDayRemainingQty > 0;
        /*
         * 阶段归属必须读取原始月计划 DAY_N，禁止使用已追加历史欠产或临时前移后的
         * remainingQty/dayPlanQty。该值在整个当前业务日内保持不变。
         */
        boolean currentDayPlanConfigured =
                resolveOriginalNewSpecDayPlanQty(context, sku, scheduleDate) > 0;
        DeferredScheduleTask deferredTask = state.getDeferredTask(sku);
        boolean deferredDue = Objects.nonNull(deferredTask)
                && (Objects.isNull(deferredTask.getNextAttemptDate())
                || !deferredTask.getNextAttemptDate().isAfter(scheduleDate));
        boolean historicalLocked = hasPendingHistoricalReverseDirectiveForSku(context, sku);
        boolean typeBlockTransfer = StringUtils.equals(
                SkuScheduleSourceTypeEnum.TYPE_BLOCK_TO_NEW_SPEC.getCode(), sku.getSourceType());
        boolean boundOnMachine = state.hasActiveBinding(sku);
        boolean futureOnlyEarlyProductionCandidate =
                context.isFutureOnlyEarlyProductionCandidate(sku);

        /*
         * 当前月 TOTAL_QTY=0 的候选禁止进入当天计划、正常加机台和历史欠产/收尾遗留阶段。
         * 即使通用余量、欠产阈值或胎胚库存为正，也只能在阶段一冻结后由提前生产阶段激活。
         */
        if (futureOnlyEarlyProductionCandidate
                && phase != DailySchedulePhase.EARLY_PRODUCTION) {
            return null;
        }

        DailyNewSpecCandidate candidate = new DailyNewSpecCandidate(
                MonthPlanDateResolver.buildMaterialStatusKey(
                        sku.getMaterialCode(), sku.getProductStatus()), sku);
        candidate.setRealtimeDayPlanRemainingQty(currentDayRemainingQty);
        candidate.setBoundOnMachine(boundOnMachine);

        if (phase == DailySchedulePhase.NORMAL_RESOURCE_COMPETITION) {
            boolean continuationAddMachineCandidate =
                    isContinuationAddMachineCandidate(sku);
            boolean ordinaryTodayCandidate = !continuationAddMachineCandidate
                    && !boundOnMachine && currentDayPlanConfigured && currentDayPlanAllowed;
            /*
             * 续作加机台以 S4.4 中心规则固化的 firstAddMachineProductionDate 为权威准入；
             * 普通新增的后续扩机仍要求当前日存在原始 dayN，防止零计划日期误扩机。
             */
            boolean addMachineCandidate = shouldEnterAddMachinePhase(
                    context, dayContext, state, sku)
                    && (continuationAddMachineCandidate || currentDayPlanConfigured);
            /*
             * 已上机 SKU 的当前日额度只能由阶段一在原结果上增量续排；正常竞争阶段只允许
             * 普通未绑定 SKU 首次选机，或已按中心规则到达当前业务日的加机台需求选择新机台。
             * 续作加机台补偿虽然自身没有日驱动绑定，也不得冒充普通新增绕过首次增机日期。
             */
            if (ordinaryTodayCandidate) {
                candidate.addReason(DailyCandidateReason.TODAY_PLAN);
                candidate.setTargetPlanDate(scheduleDate);
            }
            if (addMachineCandidate) {
                candidate.addReason(DailyCandidateReason.ADD_MACHINE_REQUIREMENT);
                candidate.setTargetPlanDate(scheduleDate);
            }
            /*
             * 历史反选和换活字块转新增只负责锁定优先机台，不授予零日计划排产资格。
             * 三类任务都必须先通过当前业务日日计划准入，防止反选失败普通回落后继续
             * 在 dayN=0 的日期排产。
             */
            if (ordinaryTodayCandidate && historicalLocked) {
                candidate.addReason(DailyCandidateReason.ALTERNATE_PLAN_REVERSE_SELECT);
            }
            if (ordinaryTodayCandidate && typeBlockTransfer) {
                candidate.addReason(DailyCandidateReason.TYPE_BLOCK_TRANSFER);
            }
            /*
             * 延期标记只用于补充候选来源，不能独立授予准入资格。尤其是首次增机日在 T+1/T+2
             * 的续作加机台任务，即使上一日已有延期记录，也必须等中心规则日期真正到达。
             */
            if ((ordinaryTodayCandidate || addMachineCandidate) && deferredDue
                    && deferredTask.getSourcePhase()
                    == DailySchedulePhase.NORMAL_RESOURCE_COMPETITION) {
                candidate.addReason(DailyCandidateReason.DEFERRED_FROM_PREVIOUS_DAY);
            }
        } else if (phase == DailySchedulePhase.LEGACY_SHORTAGE_OR_ENDING) {
            if (legacyNoFutureOnly && isLegacyNoFutureNormalCandidate(
                    context, sku, scheduleDate, boundOnMachine)) {
                candidate.addReason(DailyCandidateReason.HISTORY_SHORTAGE_OR_ENDING);
                candidate.setTargetPlanDate(scheduleDate);
            }
        } else if (phase == DailySchedulePhase.EARLY_PRODUCTION) {
            EarlyProductionRuntimePlan runtimePlan = boundOnMachine
                    ? context.getEarlyProductionRuntimePlan(sku)
                    : prepareEarlyProductionRuntimePlan(context, dayContext, sku);
            EarlyProductionDecision decision = Objects.isNull(runtimePlan)
                    || Objects.isNull(runtimePlan.getDecision())
                    ? EarlyProductionDecision.notEarlyProduction(false, "未形成提前生产运行视图")
                    : runtimePlan.getDecision();
            if (!boundOnMachine
                    && !currentDayPlanConfigured
                    && Objects.nonNull(runtimePlan)
                    && runtimePlan.isActive()
                    && decision.isEarlyProduction()
                    && decision.isAllowed()) {
                candidate.addReason(DailyCandidateReason.EARLY_PRODUCTION);
                candidate.setTargetPlanDate(decision.getFuturePlanDate());
            } else if (!boundOnMachine
                    && !currentDayPlanConfigured
                    && decision.isEarlyProduction()
                    && !decision.isAllowed()) {
                /*
                 * 结构机台数等准入硬约束未通过时，不进入选机，但保留当日最后原因。
                 * 三天窗口结束只生成一条最终未排记录，不在每天重复落库。
                 */
                state.defer(new DeferredScheduleTask(
                        sku, scheduleDate, scheduleDate.plusDays(1),
                        DailySchedulePhase.EARLY_PRODUCTION, decision.getReason()));
            }
            if (!boundOnMachine
                    && Objects.nonNull(runtimePlan)
                    && runtimePlan.isActive()
                    && decision.isAllowed()
                    && deferredDue
                    && deferredTask.getSourcePhase() == DailySchedulePhase.EARLY_PRODUCTION) {
                candidate.addReason(DailyCandidateReason.DEFERRED_FROM_PREVIOUS_DAY);
            }
        }
        return candidate.getReasons().isEmpty() ? null : candidate;
    }

    /**
     * 判断零日计划 SKU 是否属于既有真实历史欠产或无未来计划收尾遗留任务。
     *
     * <p>历史欠产/收尾遗留阶段已下线，本方法暂时保留以兼容现有阶段枚举和测试清理节奏，
     * 统一返回 false，确保任何残留调用都不能重新放行遗留任务。后续清理阶段再连同枚举、
     * 候选原因和日志映射一起删除。</p>
     *
     * @param context 排程上下文
     * @param sku 待判断 SKU
     * @param currentDate 当前业务日期
     * @param boundOnMachine 当前业务日是否已有在机绑定
     * @return 固定返回 false
     */
    private boolean isLegacyNoFutureNormalCandidate(LhScheduleContext context,
                                                    SkuScheduleDTO sku,
                                                    LocalDate currentDate,
                                                    boolean boundOnMachine) {
        return false;
    }

    /**
     * 构造并注册提前生产中心化运行视图。
     *
     * <p>运行视图把准入结果、未来计划日、临时前移 dayN 账本和本轮有效目标量绑定在
     * 同一对象中，后续选机、加机台、逐日后看、产能模拟和实际扣账必须读取这一份视图。
     * 原月计划对象、SKU 原始 {@code dailyPlanQuotaMap} 及数据库计划均不修改。</p>
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日编排上下文
     * @param sku 待提前生产 SKU
     * @return 命中提前生产场景后的中心化运行视图；不属于提前生产时返回 null
     */
    private EarlyProductionRuntimePlan prepareEarlyProductionRuntimePlan(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            SkuScheduleDTO sku) {
        if (Objects.isNull(context) || Objects.isNull(dayContext) || Objects.isNull(sku)) {
            return null;
        }
        // S4.5 只负责提供当前业务日，准入、数量、临时 dayN 和实际消费账本统一交给共享服务。
        return earlyProductionRuntimePlanService.prepareRuntimePlan(
                context, sku, dayContext.getScheduleDate());
    }

    /**
     * 判断 SKU 的加机台需求是否已经到达当前业务日。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param state 日驱动状态
     * @param sku SKU
     * @return true-当前日进入加机台阶段
     */
    private boolean shouldEnterAddMachinePhase(LhScheduleContext context,
                                               DayScheduleContext dayContext,
                                               DayDrivenScheduleState state,
                                               SkuScheduleDTO sku) {
        LocalDate currentDate = dayContext.getScheduleDate();
        if (isContinuationAddMachineCandidate(sku)) {
            LocalDate firstAddMachineDate = sku.getFirstAddMachineProductionDate();
            return Objects.isNull(firstAddMachineDate)
                    || !firstAddMachineDate.isAfter(currentDate);
        }
        if (!state.hasActiveBinding(sku) || !needMoreMachine(context, sku)) {
            return false;
        }
        int activeMachineCount = Math.max(1, state.findBindingsBySku(sku).size());
        LocalDate addMachineDate =
                DailyMachineExpansionPlanner.resolveFirstDailyLookAheadAddMachineDate(
                        this.lhDailyMouldCalcService, context, sku, activeMachineCount,
                        ScheduleTypeEnum.NEW_SPEC.getCode());
        // 欠产超阈值等非逐日后看场景由现有主链即时判断，公共方法返回 null 时仍允许进入本阶段。
        return Objects.isNull(addMachineDate) || !addMachineDate.isAfter(currentDate);
    }

    /**
     * 判断当前 SKU 的下一台新增机台是否应等待未来业务日再参与竞争。
     *
     * <p>该门槛只作用于普通新增与续作加机台的统一资源竞争阶段。历史遗留及提前生产
     * 保持既有时间轴语义；普通新增首台解析不到增机日时也允许在当前日尽早开产。</p>
     *
     * @param dayContext 当前业务日及阶段
     * @param addMachineProductionDate 下一台新增机台的业务生效日
     * @return true-当前日停止继续选机并延期到生效日；false-允许继续当前日尝试
     */
    private boolean shouldWaitForFutureAddMachineDate(
            DayScheduleContext dayContext,
            LocalDate addMachineProductionDate) {
        return Objects.nonNull(dayContext)
                && dayContext.getCurrentPhase()
                == DailySchedulePhase.NORMAL_RESOURCE_COMPETITION
                && Objects.nonNull(dayContext.getScheduleDate())
                && Objects.nonNull(addMachineProductionDate)
                && addMachineProductionDate.isAfter(dayContext.getScheduleDate());
    }

    /**
     * 构造下一台新增机台尚未到生效日的延期原因。
     *
     * @param addMachineProductionDate 下一台新增机台生效日
     * @param currentDate 当前业务日
     * @return 可写入过程日志和延期任务的原因
     */
    private String buildFutureAddMachineDeferredReason(
            LocalDate addMachineProductionDate,
            LocalDate currentDate) {
        return new StringBuilder("下一台新增机台需求日期为")
                .append(addMachineProductionDate)
                .append("，当前业务日")
                .append(currentDate)
                .append("不提前参与资源竞争")
                .toString();
    }

    /**
     * 读取 SKU 指定业务日的实时日计划余额。
     *
     * @param sku SKU
     * @param scheduleDate 业务日期
     * @return 实时剩余额度
     */
    private int resolveDailyPlanRemainingQty(SkuScheduleDTO sku, LocalDate scheduleDate) {
        if (Objects.isNull(sku) || Objects.isNull(scheduleDate)
                || CollectionUtils.isEmpty(sku.getDailyPlanQuotaMap())) {
            return 0;
        }
        SkuDailyPlanQuotaDTO quota = sku.getDailyPlanQuotaMap().get(scheduleDate);
        return Objects.isNull(quota) ? 0 : Math.max(0, quota.getRemainingQty());
    }

    /**
     * 读取SKU指定业务日的原始日计划量。
     *
     * <p>增机台和在机延续的日期准入使用原始dayN，避免同日已有机台先消费余额后，
     * 把中心规则已经确定的增机任务误判为零计划；普通首次上机仍使用实时余额防止重复消费。</p>
     *
     * @param sku SKU
     * @param scheduleDate 业务日期
     * @return 当天原始日计划量
     */
    private int resolveDailyPlanQty(SkuScheduleDTO sku, LocalDate scheduleDate) {
        if (Objects.isNull(sku) || Objects.isNull(scheduleDate)
                || CollectionUtils.isEmpty(sku.getDailyPlanQuotaMap())) {
            return 0;
        }
        SkuDailyPlanQuotaDTO quota = sku.getDailyPlanQuotaMap().get(scheduleDate);
        return Objects.isNull(quota) ? 0 : Math.max(0, quota.getDayPlanQty());
    }

    /**
     * 根据当前阶段结果刷新 SKU 的窗口级生命周期。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param state 日驱动状态
     * @param candidateList 本阶段实际候选
     */
    private void finalizeDailyCandidateState(LhScheduleContext context,
                                             DayScheduleContext dayContext,
                                             DayDrivenScheduleState state,
                                             List<DailyNewSpecCandidate> candidateList) {
        for (DailyNewSpecCandidate candidate : candidateList) {
            SkuScheduleDTO sku = candidate.getSku();
            if (state.isCompleted(sku)
                    || state.isFinalUnscheduled(sku)
                    || state.getCurrentDayOutcome(sku)
                    == SkuDayScheduleOutcome.DEFER_TO_NEXT_DAY) {
                continue;
            }
            int productionRemainingQty =
                    getTargetScheduleQtyResolver().resolveProductionRemainingQty(context, sku);
            if (productionRemainingQty <= 0 && !needMoreMachine(context, sku)) {
                /*
                 * 非收尾结果达到当前日目标不代表物理机台已经下机。此类绑定必须保留到后续业务日，
                 * 由延续阶段按未来日实时额度和原机台有效产能继续追加；若在这里解除绑定，后续日
                * 会把同一物料、同一机台误当成新换产并生成重复结果。
                */
                if (state.hasNonEndingBinding(sku)) {
                    state.markScheduledAndCarryOver(sku);
                    // 记录保留原因，便于按批次核对跨日延续未被错误转化为重复换产。
                    log.info("新增SKU非收尾在机绑定保留, batchNo: {}, scheduleDate: {}, materialCode: {}, "
                                    + "reason: 当前日目标已完成但机台未满足下机规则",
                            context.getBatchNo(), dayContext.getScheduleDate(), sku.getMaterialCode());
                    continue;
                }
                state.complete(sku);
                state.removeBindingsBySku(sku);
                continue;
            }
            if (state.hasActiveBinding(sku)) {
                state.markScheduledAndCarryOver(sku);
                continue;
            }
            /*
             * 即使当前已经是 T+2，也只能登记为窗口待收口任务：同一业务日后面仍可能有
             * 加机台或提前生产阶段。最终未排只能由四个阶段全部结束后的窗口收口统一写入。
             */
            deferSkuToNextDay(context, dayContext, state, sku,
                    "当前阶段结束后仍有待排量，转下一业务日继续尝试");
        }
    }

    /**
     * 延续前一业务日已经上机的新增 SKU。
     *
     * <p>延续阶段只在原结果追加当前日班次增量，不重新选机、换模、预占模具和首检。
     * 单控整机左右两侧作为一个物理组同步追加，并按整组口径只消费一次 SKU 账本。</p>
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param state 日驱动状态
     * @param allShifts 完整窗口班次
     * @param earlyProductionOnly true-只处理应后置到提前生产阶段的绑定；false-处理正常在机绑定
     * @return 当前日形成有效增量的在机绑定数量
     */
    private int scheduleCarryOverSkus(LhScheduleContext context,
                                      DayScheduleContext dayContext,
                                      DayDrivenScheduleState state,
                                      List<LhShiftConfigVO> allShifts,
                                      boolean earlyProductionOnly) {
        int scheduledBindingCount = 0;
        List<ActiveMachineBinding> bindingList = state.getActiveBindings();
        for (ActiveMachineBinding binding : bindingList) {
            if (!isActiveBindingConsistent(context, binding)) {
                state.removeBinding(binding);
                continue;
            }
            SkuScheduleDTO sku = binding.getSku();
            boolean shouldRunInEarlyProductionPhase =
                    shouldRunBindingInEarlyProductionPhase(
                            context, dayContext, binding);
            if (earlyProductionOnly != shouldRunInEarlyProductionPhase) {
                continue;
            }
            if (earlyProductionOnly) {
                /*
                 * 前一日提前生产形成的绑定在当前日正常任务完成后才能续排。此时必须按当前日
                 * 最新结构占用重新执行候选物理机台硬控：结构已经排满时，原绑定机台若尚未
                 * 计入当前日结构集合，就不能凭前一日绑定身份把结构机台数推到计划上限之外。
                 */
                String structureLimitReason =
                        this.resolveEarlyProductionStructureMachineLimitReason(
                                context, dayContext, sku, binding.getMachineCode());
                if (StringUtils.isNotEmpty(structureLimitReason)) {
                    this.appendEarlyProductionStructureMachineLimitLog(
                            context, dayContext, sku, binding.getMachineCode(),
                            structureLimitReason);
                    deferSkuToNextDay(
                            context, dayContext, state, sku, structureLimitReason);
                    log.info("提前生产在机绑定跨日续排受结构机台数限制, batchNo: {}, "
                                    + "scheduleDate: {}, materialCode: {}, machineCode: {}, reason: {}",
                            context.getBatchNo(), dayContext.getScheduleDate(),
                            sku.getMaterialCode(), binding.getMachineCode(),
                            structureLimitReason);
                    continue;
                }
            }
            if (!isBusinessDayAdmissionAllowed(
                    context, dayContext, sku, isEndingBinding(binding))) {
                deferSkuToNextDay(context, dayContext, state, sku,
                        "当前业务日日计划为0且未命中合法提前生产");
                log.info("新增在机SKU跨日续排未通过业务日准入, batchNo: {}, scheduleDate: {}, "
                                + "materialCode: {}, machineCode: {}, dayPlanRemainingQty: {}",
                        context.getBatchNo(), dayContext.getScheduleDate(), sku.getMaterialCode(),
                        binding.getMachineCode(),
                        resolveDailyPlanRemainingQty(sku, dayContext.getScheduleDate()));
                continue;
            }
            int productionRemainingQty =
                    getTargetScheduleQtyResolver().resolveProductionRemainingQty(context, sku);
            if (productionRemainingQty <= 0
                    && !needMoreMachine(context, sku)
                    && isEndingBinding(binding)) {
                state.complete(sku);
                state.removeBinding(binding);
                continue;
            }

            int deltaQty = appendCarryOverDayDelta(
                    context, dayContext, binding, allShifts,
                    earlyProductionOnly);
            if (deltaQty > 0) {
                scheduledBindingCount++;
                state.markScheduledAndCarryOver(sku);
                log.info("新增在机SKU跨日续排完成, batchNo: {}, scheduleDate: {}, materialCode: {}, "
                                + "machineCode: {}, pairMachineCode: {}, deltaQty: {}, estimatedEndTime: {}",
                        context.getBatchNo(), dayContext.getScheduleDate(), sku.getMaterialCode(),
                        binding.getMachineCode(), binding.getPairMachineCode(), deltaQty,
                        LhScheduleTimeUtil.formatDateTime(binding.getEstimatedEndTime()));
                continue;
            }
            // T+2 失败也先保留延期原因，避免在同日后续阶段前提前生成最终未排。
            deferSkuToNextDay(context, dayContext, state, sku,
                    "已上机SKU当前业务日受停机、维修或班次管控影响无可用产能");
        }
        return scheduledBindingCount;
    }

    /**
     * 判断跨日在机绑定是否必须后置到当前日提前生产阶段。
     *
     * <p>只有前一业务日已经标记为提前生产、且当前业务日仍无原始 dayN 的绑定需要后置。
     * 当前日已有原始计划时，该 SKU 已转为正常在机任务，继续沿用原阶段一延续语义。</p>
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日上下文
     * @param binding 跨日在机绑定
     * @return true-正常阶段完成后再续排；false-在机延续阶段正常处理
     */
    private boolean shouldRunBindingInEarlyProductionPhase(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            ActiveMachineBinding binding) {
        if (Objects.isNull(context) || Objects.isNull(dayContext)
                || Objects.isNull(binding) || Objects.isNull(binding.getSku())
                || Objects.isNull(binding.getScheduleResult())
                || !StringUtils.equals(
                YES_FLAG, binding.getScheduleResult().getIsEarlyProduction())) {
            return false;
        }
        return resolveOriginalNewSpecDayPlanQty(
                context, binding.getSku(), dayContext.getScheduleDate()) <= 0;
    }

    /**
     * 统一判断SKU是否允许在当前业务日形成新增排产量。
     *
     * <p>当天日计划余额大于0时直接准入；否则必须命中现有提前生产规则。
     * 历史反选、换活字块锁机台、延期任务不构成额外日期准入资格。
     * 唯一例外：收尾 SKU 前一业务日已经真实上机（在机收尾绑定）且仍有生产剩余量时，
     * 允许跨日续排——dayN 只限制新增机台数量，不截断已在机机台的有效产能，
     * 避免“已上机收尾物料只排一天就提前下机、机台空置”的问题。</p>
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param sku SKU
     * @param endingInMachineBinding true-当前 SKU 存在已上机收尾绑定（仅由延续阶段传入）
     * @return true-当前业务日允许排产；false-必须延期
     */
    private boolean isBusinessDayAdmissionAllowed(LhScheduleContext context,
                                                  DayScheduleContext dayContext,
                                                  SkuScheduleDTO sku,
                                                  boolean endingInMachineBinding) {
        if (endingInMachineBinding) {
            int productionRemainingQty =
                    getTargetScheduleQtyResolver().resolveProductionRemainingQty(context, sku);
            if (productionRemainingQty > 0) {
                log.info("新增收尾SKU在机跨日续排准入, scheduleDate: {}, materialCode: {}, "
                                + "productStatus: {}, productionRemainingQty: {}, "
                                + "原因: 已在机收尾绑定按目标量续排，dayN只限制新增机台数",
                        dayContext.getScheduleDate(), sku.getMaterialCode(), sku.getProductStatus(),
                        productionRemainingQty);
                return true;
            }
        }
        EarlyProductionRuntimePlan runtimePlan =
                Objects.isNull(context) ? null : context.getEarlyProductionRuntimePlan(sku);
        if (Objects.nonNull(runtimePlan)) {
            /*
             * 提前生产 SKU 在前一业务日已经真实上机时，后续业务日继续消费同一份
             * 临时前移账本。这里不能重新执行结构准入，否则刚写入的机台统计会让
             * 同一 SKU 在第二天被错误阻断。
             */
            return SkuDailyPlanQuotaUtil.sumRemainingQty(
                    runtimePlan.getShiftedDailyPlanQuotaMap()) > 0;
        }
        if (resolveDailyPlanQty(sku, dayContext.getScheduleDate()) > 0) {
            return true;
        }
        EarlyProductionDecision decision = resolveEarlyProductionDecision(
                context, sku, dayContext.getDayStartTime(), dayContext.getDayShifts(),
                endingJudgmentStrategy.isCurrentWindowEnding(context, sku),
                DailySchedulePhase.EARLY_PRODUCTION);
        return decision.isEarlyProduction() && decision.isAllowed();
    }

    /**
     * 判断在机绑定是否已经按收尾结果占用机台。
     *
     * <p>只有明确标记为收尾的结果在目标量归零后才允许解除绑定；非收尾结果即使当前日额度
     * 已满足，也要继续保留物理在机关系供后续业务日延续。</p>
     *
     * @param binding 在机绑定
     * @return true-收尾绑定；false-非收尾绑定
     */
    private boolean isEndingBinding(ActiveMachineBinding binding) {
        return Objects.nonNull(binding) && binding.isEndingTarget();
    }

    /**
     * 在原新增结果中追加当前业务日班次增量。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param binding 跨日在机绑定
     * @param allShifts 完整窗口班次
     * @param allowFutureQuotaConsumption true-提前生产阶段允许继续消费固定范围未来额度
     * @return 当前日实际新增排产量
     */
    private int appendCarryOverDayDelta(LhScheduleContext context,
                                        DayScheduleContext dayContext,
                                        ActiveMachineBinding binding,
                                        List<LhShiftConfigVO> allShifts,
                                        boolean allowFutureQuotaConsumption) {
        SkuScheduleDTO sku = binding.getSku();
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(binding.getMachineCode());
        if (Objects.isNull(machine)) {
            return 0;
        }
        List<LhShiftConfigVO> dayShifts = dayContext.getDayShifts();
        Date productionStartTime = resolveCarryOverProductionStartTime(
                dayContext, machine, binding);
        int mouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(machine);
        int runtimeShiftCapacity = ShiftCapacityResolverUtil.resolveRuntimeShiftCapacity(
                context, machine, sku.getShiftCapacity());
        productionStartTime = ShiftProductionControlUtil.resolveFirstSchedulableStartIgnoringCleaning(
                context, machine.getMachineCode(), productionStartTime, dayShifts,
                runtimeShiftCapacity, sku.getLhTimeSeconds(), mouldQty);
        if (!dayContext.contains(productionStartTime)) {
            return 0;
        }

        /*
         * 先按物理有效产能形成当前日上限，再结合SKU实时剩余量判断当前增量是否真的是最后收尾块。
         * 结果行 isEnd 是窗口级最终展示字段，不能直接控制中间业务日的班次裁剪。
         */
        Map<Integer, Integer> shiftCapacityMap = calculateShiftCapacityMap(
                context, machine, sku, productionStartTime, null, dayShifts,
                mouldQty, runtimeShiftCapacity, false);
        int maxQtyToDayEnd = sumShiftCapacity(shiftCapacityMap);
        if (maxQtyToDayEnd <= 0) {
            return 0;
        }

        int productionRemainingQty =
                this.resolveActualSurplusRemainingQty(context, sku);
        boolean finalStrictBlock = isFinalStrictCarryOverBlock(
                binding, productionRemainingQty, maxQtyToDayEnd);
        if (finalStrictBlock) {
            // 收尾目标曾被补满规则抬高时，必须先把实际消费账本收敛到真实硫化余量。
            getTargetScheduleQtyResolver().syncProductionRemainingQtyToRemaining(
                    context, sku, productionRemainingQty, "跨日在机真实严格收尾");
        }
        ProductionQuantityPolicy quantityPolicy =
                ProductionQuantityPolicy.from(sku, finalStrictBlock);
        int pendingQty = resolveSchedulableRemainingQty(context, sku);
        if (quantityPolicy.isAllowFillStartedShift() && !quantityPolicy.isEnding()) {
            // 非收尾在机 SKU 继续沿用排满语义，dayN 只负责节奏和加机台判断，不截断原机台有效产能。
            pendingQty = Math.max(pendingQty, maxQtyToDayEnd);
        }
        if (pendingQty <= 0) {
            return 0;
        }

        /*
         * 先在临时结果中形成当前日增量并消费账本，原结果在账本成功后才合并。
         * 这样 capResultByProductionRemainingQty 和日计划消费看到的始终只是 deltaQty，
         * 不会把前一业务日已经扣过的班次数量再次扣减。
         */
        LhScheduleResult deltaResult =
                buildCarryOverDeltaResult(binding.getScheduleResult(), allShifts);
        deltaResult.setIsEnd(finalStrictBlock ? "1" : "0");
        List<MachineMaintenanceWindowDTO> maintenanceWindowList =
                resolveMachineMaintenanceWindowList(context, binding.getMachineCode());
        distributeToShifts(
                context, deltaResult, dayShifts, productionStartTime,
                runtimeShiftCapacity, sku.getLhTimeSeconds(), mouldQty, pendingQty,
                Collections.<MachineCleaningWindowDTO>emptyList(), maintenanceWindowList,
                sku, finalStrictBlock, null, shiftCapacityMap, null, true);
        applyNightNoMouldChangeContinuationFill(
                context, sku, deltaResult, dayShifts, quantityPolicy);
        applyDailyStandardPlanQtyToResult(
                context, sku, deltaResult, dayShifts, runtimeShiftCapacity, null);
        if (finalStrictBlock) {
            context.getEndingFillAllowedOverQtyMap().remove(deltaResult);
            context.getSharedEmbryoEndingStaggerAllowedOverQtyMap().remove(deltaResult);
        }

        LhScheduleResult pairDeltaResult = null;
        /*
         * 跨日续排也是一次独立的日计划消费。结果尚未合并到原行前先快照账本，
         * 若当日额度或实际余量把增量裁为零，必须恢复，不能让失败续排占用后续日资源。
         */
        DailyQuotaLedgerBaseline quotaLedgerBaseline =
                DailyQuotaLedgerBaseline.capture(context, sku);
        int actualDeltaQty;
        if (binding.hasPairMachine()) {
            pairDeltaResult =
                    buildCarryOverDeltaResult(binding.getPairScheduleResult(), allShifts);
            pairDeltaResult.setIsEnd(finalStrictBlock ? "1" : "0");
            copyDayShiftFields(deltaResult, pairDeltaResult, dayShifts);
            actualDeltaQty = this.applyWholeSingleControlBlockToDailyQuota(
                    context, sku, deltaResult, pairDeltaResult, dayShifts,
                    allowFutureQuotaConsumption);
        } else {
            actualDeltaQty = this.applyBlockToDailyQuota(
                    context, sku, deltaResult, dayShifts,
                    allowFutureQuotaConsumption);
        }
        if (actualDeltaQty <= 0) {
            quotaLedgerBaseline.restore(context, sku);
            return 0;
        }

        ScheduleResultBaseline primaryBaseline =
                ScheduleResultBaseline.capture(binding.getScheduleResult(), allShifts);
        ScheduleResultBaseline pairBaseline = binding.hasPairMachine()
                ? ScheduleResultBaseline.capture(binding.getPairScheduleResult(), allShifts) : null;
        mergeDayShiftDelta(
                context, binding.getScheduleResult(), deltaResult, dayShifts, allShifts);
        if (binding.hasPairMachine()) {
            mergeDayShiftDelta(
                    context, binding.getPairScheduleResult(), pairDeltaResult, dayShifts, allShifts);
        }
        int mergedDeltaQty = primaryBaseline.calculatePositiveDelta(
                binding.getScheduleResult(), dayShifts);
        if (binding.hasPairMachine()) {
            mergedDeltaQty += pairBaseline.calculatePositiveDelta(
                    binding.getPairScheduleResult(), dayShifts);
        }
        if (mergedDeltaQty != actualDeltaQty) {
            primaryBaseline.restore(binding.getScheduleResult());
            if (Objects.nonNull(pairBaseline)) {
                pairBaseline.restore(binding.getPairScheduleResult());
            }
            quotaLedgerBaseline.restore(context, sku);
            throw new IllegalStateException("新增排产跨日结果增量与账本消费量不一致, materialCode="
                    + sku.getMaterialCode() + ", actualDeltaQty=" + actualDeltaQty
                    + ", mergedDeltaQty=" + mergedDeltaQty);
        }

        updateCarryOverMachineState(
                context, machine, binding.getScheduleResult(), binding);
        if (binding.hasPairMachine()) {
            MachineScheduleDTO pairMachine =
                    context.getMachineScheduleMap().get(binding.getPairMachineCode());
            updateCarryOverMachineState(
                    context, pairMachine, binding.getPairScheduleResult(), binding);
        }
        recordScheduledMachineForResult(context, binding.getScheduleResult(), dayShifts);
        if (binding.hasPairMachine()) {
            recordScheduledMachineForResult(
                    context, binding.getPairScheduleResult(), dayShifts);
        }
        log.info("新增在机SKU跨日收尾块判定, batchNo: {}, scheduleDate: {}, materialCode: {}, "
                        + "machineCode: {}, endingTarget: {}, productionRemainingQty: {}, "
                        + "currentDayCapacity: {}, finalStrictBlock: {}, actualDeltaQty: {}",
                context.getBatchNo(), dayContext.getScheduleDate(), sku.getMaterialCode(),
                binding.getMachineCode(), binding.isEndingTarget(), productionRemainingQty,
                resolveCarryOverGroupDayCapacity(binding, maxQtyToDayEnd),
                finalStrictBlock, actualDeltaQty);
        return actualDeltaQty;
    }

    /**
     * 判断跨日在机增量是否为当前SKU的最终严格收尾块。
     *
     * <p>严格收尾以当前业务日真实物理产能块和SKU实时剩余量为准，不依赖首次上机时
     * 固化的收尾标签。若剩余量仍大于当日产能，原机台必须连续满产；当本日可以收完时，
     * 必须立即严格收口，避免后置仅补标isEnd却无法撤销超排。</p>
     *
     * @param binding 跨日在机绑定
     * @param productionRemainingQty SKU实时剩余量
     * @param singleMachineDayCapacity 主机台当前业务日有效产能
     * @return true-当前增量为最终严格收尾块；false-继续按在机产能满产
     */
    private boolean isFinalStrictCarryOverBlock(ActiveMachineBinding binding,
                                                int productionRemainingQty,
                                                int singleMachineDayCapacity) {
        if (Objects.isNull(binding)
                || productionRemainingQty <= 0 || singleMachineDayCapacity <= 0) {
            return false;
        }
        return productionRemainingQty
                <= resolveCarryOverGroupDayCapacity(binding, singleMachineDayCapacity);
    }

    /**
     * 判断首次上机的当前物理机台组是否已经进入最终严格收尾块。
     *
     * @param productionRemainingQty SKU实际消费账本剩余量
     * @param singleMachineCapacity 当前候选主机台真实有效产能
     * @param wholeSingleControlUnit 是否为单控整机双侧排产
     * @return true-当前物理组可以完成全部余量，必须严格收尾
     */
    private boolean isFinalStrictProductionBlock(int productionRemainingQty,
                                                 int singleMachineCapacity,
                                                 boolean wholeSingleControlUnit) {
        if (productionRemainingQty <= 0 || singleMachineCapacity <= 0) {
            return false;
        }
        return productionRemainingQty
                <= resolvePhysicalGroupCapacity(singleMachineCapacity, wholeSingleControlUnit);
    }

    /**
     * 解析普通机台或单控整机物理组的合计有效产能。
     *
     * @param singleMachineCapacity 单侧机台有效产能
     * @param wholeSingleControlUnit 是否为单控整机双侧排产
     * @return 物理机台组合计有效产能
     */
    private int resolvePhysicalGroupCapacity(int singleMachineCapacity,
                                             boolean wholeSingleControlUnit) {
        int normalizedCapacity = Math.max(0, singleMachineCapacity);
        return wholeSingleControlUnit ? normalizedCapacity * 2 : normalizedCapacity;
    }

    /**
     * 解析跨日在机物理组当前业务日有效产能。
     *
     * @param binding 跨日在机绑定
     * @param singleMachineDayCapacity 主机台当前业务日有效产能
     * @return 普通机台返回单机产能，单控整机返回L/R两侧合计产能
     */
    private int resolveCarryOverGroupDayCapacity(ActiveMachineBinding binding,
                                                 int singleMachineDayCapacity) {
        int normalizedCapacity = Math.max(0, singleMachineDayCapacity);
        return Objects.nonNull(binding) && binding.hasPairMachine()
                ? normalizedCapacity * 2 : normalizedCapacity;
    }

    /**
     * 创建只承载当前业务日新增班次量的临时结果。
     *
     * @param sourceResult 已落地的跨日原结果
     * @param allShifts 完整窗口班次
     * @return 已复制业务字段但清空全部班次的临时结果
     */
    private LhScheduleResult buildCarryOverDeltaResult(LhScheduleResult sourceResult,
                                                       List<LhShiftConfigVO> allShifts) {
        LhScheduleResult deltaResult = new LhScheduleResult();
        BeanUtil.copyProperties(sourceResult, deltaResult);
        for (LhShiftConfigVO shift : allShifts) {
            ShiftFieldUtil.setShiftPlanQty(
                    deltaResult, shift.getShiftIndex(), 0, null, null);
        }
        deltaResult.setDailyPlanQty(0);
        deltaResult.setSpecEndTime(null);
        deltaResult.setIsChangeMould("0");
        deltaResult.setMouldChangeStartTime(null);
        return deltaResult;
    }

    /**
     * 将临时增量结果的当前日班次字段追加到原结果。
     *
     * @param context 排程上下文
     * @param targetResult 原跨日结果
     * @param deltaResult 当前日临时增量结果
     * @param dayShifts 当前业务日班次
     * @param allShifts 完整窗口班次
     */
    private void mergeDayShiftDelta(LhScheduleContext context,
                                    LhScheduleResult targetResult,
                                    LhScheduleResult deltaResult,
                                    List<LhShiftConfigVO> dayShifts,
                                    List<LhShiftConfigVO> allShifts) {
        for (LhShiftConfigVO shift : dayShifts) {
            int shiftIndex = shift.getShiftIndex();
            int existingQty = resolvePositiveShiftQty(targetResult, shiftIndex);
            int deltaQty = resolvePositiveShiftQty(deltaResult, shiftIndex);
            if (deltaQty <= 0) {
                continue;
            }
            Date existingStartTime =
                    ShiftFieldUtil.getShiftStartTime(targetResult, shiftIndex);
            Date deltaStartTime =
                    ShiftFieldUtil.getShiftStartTime(deltaResult, shiftIndex);
            Date existingEndTime =
                    ShiftFieldUtil.getShiftEndTime(targetResult, shiftIndex);
            Date deltaEndTime =
                    ShiftFieldUtil.getShiftEndTime(deltaResult, shiftIndex);
            Date mergedStartTime = Objects.isNull(existingStartTime)
                    || (Objects.nonNull(deltaStartTime) && deltaStartTime.before(existingStartTime))
                    ? deltaStartTime : existingStartTime;
            Date mergedEndTime = Objects.isNull(existingEndTime)
                    || (Objects.nonNull(deltaEndTime) && deltaEndTime.after(existingEndTime))
                    ? deltaEndTime : existingEndTime;
            ShiftFieldUtil.setShiftPlanQty(
                    targetResult, shiftIndex, existingQty + deltaQty,
                    mergedStartTime, mergedEndTime);
            /*
             * 临时增量结果可能在当前班次触发换胶囊、首检等运行态事实。特别是“换胶囊”备注是
             * 后续重建胶囊运行态、防止再次固定扣量的唯一结果侧依据，不能只留在临时 deltaResult。
             * 因此数量合并成功后，必须按原因项追加到原结果；使用逐项追加可保留原备注并避免重复。
             */
            mergeDayShiftAnalysis(targetResult, deltaResult, shiftIndex);
        }
        refreshResultSummary(context, targetResult);
        // 完整窗口汇总用于再次确认未触碰当前日之外的班次字段。
        targetResult.setDailyPlanQty(calcTotalPlanQty(targetResult, allShifts));
    }

    /**
     * 合并当前业务日班次分析备注。
     *
     * <p>跨日续排先在临时结果中完成班次分配，换胶囊、首检等规则会把实际发生的业务事实
     * 写入临时结果的班次分析。合并结果时必须同步这些事实，否则下一次运行态重建会遗漏
     * 已发生的动作并重复扣减资源。这里按逗号拆分后逐项追加，确保原结果已有备注不被覆盖，
     * 同一事实也不会重复写入。</p>
     *
     * @param targetResult 原跨日结果
     * @param deltaResult 当前业务日临时增量结果
     * @param shiftIndex 当前班次索引
     */
    private void mergeDayShiftAnalysis(LhScheduleResult targetResult,
                                       LhScheduleResult deltaResult,
                                       int shiftIndex) {
        String deltaAnalysis = ShiftFieldUtil.getShiftAnalysis(deltaResult, shiftIndex);
        if (StringUtils.isEmpty(deltaAnalysis)) {
            return;
        }
        String[] analysisItemArray = deltaAnalysis.split(",");
        for (String analysisItem : analysisItemArray) {
            String trimmedAnalysisItem = StringUtils.trim(analysisItem);
            if (StringUtils.isNotEmpty(trimmedAnalysisItem)) {
                ShiftFieldUtil.appendShiftAnalysis(targetResult, shiftIndex, trimmedAnalysisItem);
            }
        }
    }

    /**
     * 将主侧临时结果的当前日班次复制到单控整机配对侧。
     *
     * @param sourceResult 主侧临时结果
     * @param targetResult 配对侧临时结果
     * @param dayShifts 当前业务日班次
     */
    private void copyDayShiftFields(LhScheduleResult sourceResult,
                                    LhScheduleResult targetResult,
                                    List<LhShiftConfigVO> dayShifts) {
        for (LhShiftConfigVO shift : dayShifts) {
            int shiftIndex = shift.getShiftIndex();
            ShiftFieldUtil.setShiftPlanQty(
                    targetResult, shiftIndex,
                    ShiftFieldUtil.getShiftPlanQty(sourceResult, shiftIndex),
                    ShiftFieldUtil.getShiftStartTime(sourceResult, shiftIndex),
                    ShiftFieldUtil.getShiftEndTime(sourceResult, shiftIndex));
        }
        /*
         * 单控整机配对侧在跨日续排时只镜像产量和时间，不复制主侧班次分析。
         * 例如“换胶囊”是物理整机一次动作，项目既有规则只允许在其中一侧结果保留事实备注；
         * 若这里复制到配对侧，会被后置核对视为同一物理机台重复换胶囊。
         */
    }

    /**
     * 解析跨日原机台在当前业务日的首个可生产时刻。
     *
     * @param dayContext 当前业务日
     * @param machine 原机台运行态
     * @param binding 跨日在机绑定
     * @return 不早于日首班且不早于机台预计结束时间的时刻
     */
    private Date resolveCarryOverProductionStartTime(DayScheduleContext dayContext,
                                                     MachineScheduleDTO machine,
                                                     ActiveMachineBinding binding) {
        Date startTime = dayContext.getDayStartTime();
        Date machineEndTime = machine.getEstimatedEndTime();
        Date bindingEndTime = binding.getEstimatedEndTime();
        if (Objects.nonNull(machineEndTime) && machineEndTime.after(startTime)) {
            startTime = machineEndTime;
        }
        if (Objects.nonNull(bindingEndTime) && bindingEndTime.after(startTime)) {
            startTime = bindingEndTime;
        }
        return startTime;
    }

    /**
     * 校验跨日在机绑定仍与真实机台状态和结果列表一致。
     *
     * @param context 排程上下文
     * @param binding 在机绑定
     * @return true-绑定仍有效；false-机台已换产或结果已撤销
     */
    private boolean isActiveBindingConsistent(LhScheduleContext context,
                                              ActiveMachineBinding binding) {
        if (Objects.isNull(binding)
                || !containsResultByIdentity(
                context.getScheduleResultList(), binding.getScheduleResult())) {
            return false;
        }
        MachineScheduleDTO machine =
                context.getMachineScheduleMap().get(binding.getMachineCode());
        if (Objects.isNull(machine)
                || !StringUtils.equals(
                machine.getCurrentMaterialCode(), binding.getSku().getMaterialCode())) {
            return false;
        }
        if (!binding.hasPairMachine()) {
            return true;
        }
        MachineScheduleDTO pairMachine =
                context.getMachineScheduleMap().get(binding.getPairMachineCode());
        return Objects.nonNull(pairMachine)
                && StringUtils.equals(
                pairMachine.getCurrentMaterialCode(), binding.getSku().getMaterialCode())
                && containsResultByIdentity(
                context.getScheduleResultList(), binding.getPairScheduleResult());
    }

    private boolean containsResultByIdentity(List<LhScheduleResult> resultList,
                                             LhScheduleResult targetResult) {
        if (CollectionUtils.isEmpty(resultList) || Objects.isNull(targetResult)) {
            return false;
        }
        for (LhScheduleResult result : resultList) {
            if (result == targetResult) {
                return true;
            }
        }
        return false;
    }

    /**
     * 使用跨日合并后的真实结果刷新原机台状态。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param result 合并后的结果
     * @param binding 在机绑定
     */
    private void updateCarryOverMachineState(LhScheduleContext context,
                                             MachineScheduleDTO machine,
                                             LhScheduleResult result,
                                             ActiveMachineBinding binding) {
        if (Objects.isNull(machine) || Objects.isNull(result)) {
            return;
        }
        machine.setEstimatedEndTime(result.getSpecEndTime());
        binding.setEstimatedEndTime(result.getSpecEndTime());
        registerMachineAssignment(context, machine.getMachineCode(), result);
    }

    /**
     * 当前日临时失败时只移出日工作队列并登记延期。
     *
     * @param context 排程上下文
     * @param iterator 当前日工作队列迭代器
     * @param dayContext 当前业务日
     * @param state 日驱动状态
     * @param sku 当前 SKU
     * @param reason 延期原因
     */
    private void deferCurrentDailyCandidate(LhScheduleContext context,
                                            Iterator<SkuScheduleDTO> iterator,
                                            DayScheduleContext dayContext,
                                            DayDrivenScheduleState state,
                                            SkuScheduleDTO sku,
                                            String reason) {
        this.deferCurrentDailyCandidateUntilDate(
                context, iterator, dayContext, state, sku, reason,
                dayContext.getScheduleDate().plusDays(1));
    }

    /**
     * 将当前候选移出本日工作队列，并延期到业务规则指定日期。
     *
     * <p>用于“下一台新增机台尚未到生效日”场景，防止任务在中间业务日仅因存在延期记录
     * 而绕过 dayN 准入。指定日期早于或等于当前业务日时，统一收敛为下一业务日。</p>
     *
     * @param context 排程上下文
     * @param iterator 当前日工作队列迭代器
     * @param dayContext 当前业务日
     * @param state 日驱动状态
     * @param sku 当前 SKU
     * @param reason 延期原因
     * @param nextAttemptDate 业务规则确定的下一次允许尝试日期
     */
    private void deferCurrentDailyCandidateUntilDate(LhScheduleContext context,
                                                     Iterator<SkuScheduleDTO> iterator,
                                                     DayScheduleContext dayContext,
                                                     DayDrivenScheduleState state,
                                                     SkuScheduleDTO sku,
                                                     String reason,
                                                     LocalDate nextAttemptDate) {
        iterator.remove();
        refreshPendingNewSpecSkuTypeCounts(context);
        this.deferSkuUntilDate(
                context, dayContext, state, sku, reason, nextAttemptDate);
    }

    /**
     * 登记下一业务日延期任务。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param state 日驱动状态
     * @param sku 延期 SKU
     * @param reason 延期原因
     */
    private void deferSkuToNextDay(LhScheduleContext context,
                                   DayScheduleContext dayContext,
                                   DayDrivenScheduleState state,
                                   SkuScheduleDTO sku,
                                   String reason) {
        this.deferSkuUntilDate(
                context, dayContext, state, sku, reason,
                dayContext.getScheduleDate().plusDays(1));
    }

    /**
     * 按指定业务日期登记延期任务。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param state 日驱动状态
     * @param sku 当前 SKU
     * @param reason 延期原因
     * @param nextAttemptDate 下一次允许参与资源竞争的业务日期
     */
    private void deferSkuUntilDate(LhScheduleContext context,
                                   DayScheduleContext dayContext,
                                   DayDrivenScheduleState state,
                                   SkuScheduleDTO sku,
                                   String reason,
                                   LocalDate nextAttemptDate) {
        LocalDate minimumNextAttemptDate = dayContext.getScheduleDate().plusDays(1);
        LocalDate effectiveNextAttemptDate = Objects.nonNull(nextAttemptDate)
                && nextAttemptDate.isAfter(dayContext.getScheduleDate())
                ? nextAttemptDate : minimumNextAttemptDate;
        DeferredScheduleTask task = new DeferredScheduleTask(
                sku,
                dayContext.getScheduleDate(),
                effectiveNextAttemptDate,
                dayContext.getCurrentPhase(),
                reason);
        state.defer(task);
        log.info("新增SKU当前日延期, batchNo: {}, scheduleDate: {}, nextAttemptDate: {}, phase: {}, "
                        + "materialCode: {}, productStatus: {}, reason: {}",
                context.getBatchNo(), dayContext.getScheduleDate(), task.getNextAttemptDate(),
                dayContext.getCurrentPhase(), sku.getMaterialCode(), sku.getProductStatus(), reason);
    }

    /**
     * 当前业务日日终仅收口运行态，不重新初始化或重算整个排程窗口。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param state 日驱动状态
     * @param allShifts 完整窗口班次
     */
    private void finalizeDayDrivenScheduleDay(LhScheduleContext context,
                                              DayScheduleContext dayContext,
                                              DayDrivenScheduleState state,
                                              List<LhShiftConfigVO> allShifts) {
        dayContext.setCurrentPhase(DailySchedulePhase.FINALIZE);
        for (ActiveMachineBinding binding : state.getActiveBindings()) {
            if (!isActiveBindingConsistent(context, binding)) {
                state.removeBinding(binding);
                continue;
            }
            binding.setEstimatedEndTime(binding.getScheduleResult().getSpecEndTime());
        }
        rebuildScheduledMachineCountMap(context, allShifts);
        context.getNewSpecSkuList().clear();
        context.getNewSpecSkuList().addAll(state.getPendingSkuListInOriginalOrder());
        refreshPendingNewSpecSkuTypeCounts(context);
        log.info("新增排产按日编排结束, batchNo: {}, scheduleDate: {}, pendingSkuCount: {}, "
                        + "activeBindingCount: {}, deferredTaskCount: {}, scheduleResultCount: {}, "
                        + "unscheduledCount: {}",
                context.getBatchNo(), dayContext.getScheduleDate(),
                state.getPendingSkuListInOriginalOrder().size(),
                state.getActiveBindings().size(), state.getDeferredTaskCount(),
                context.getScheduleResultList().size(),
                context.getUnscheduledResultList().size());
    }

    /**
     * 三天窗口结束后统一生成最终未排。
     *
     * @param context 排程上下文
     * @param state 日驱动状态
     * @param unscheduledReasonCountMap 未排原因统计
     * @param machineMatch 机台匹配策略，用于写入最后一次未命中诊断快照
     */
    private void finalizeWindowUnscheduled(LhScheduleContext context,
                                           DayDrivenScheduleState state,
                                           Map<String, Integer> unscheduledReasonCountMap,
                                           IMachineMatchStrategy machineMatch) {
        List<SkuScheduleDTO> pendingSkuList =
                new ArrayList<SkuScheduleDTO>(state.getPendingSkuListInOriginalOrder());
        for (SkuScheduleDTO sku : pendingSkuList) {
            if (state.isCompleted(sku) || state.isFinalUnscheduled(sku)) {
                continue;
            }
            int remainingQty =
                    getTargetScheduleQtyResolver().resolveProductionRemainingQty(context, sku);
            if (remainingQty <= 0 && !needMoreMachine(context, sku)) {
                state.complete(sku);
                state.removeBindingsBySku(sku);
                continue;
            }
            DeferredScheduleTask deferredTask = state.getDeferredTask(sku);
            String detailReason;
            if (Objects.nonNull(deferredTask)) {
                // 有明确硬约束时沿用最后一次真实失败原因，不被窗口统一收口文案覆盖。
                detailReason = deferredTask.getReason();
            } else if (hasPartiallyScheduledEarlyProductionResult(context, sku)) {
                /*
                 * 中心运行视图仍在 S4.5 窗口内有效。即使跨日续排清理了延期任务，
                 * 只要已落提前生产结果且仍有余量，就必须按“部分成功”收口，
                 * 禁止误写成“从未命中提前生产候选”。
                 */
                detailReason = buildEarlyProductionPartialRemainingReason(remainingQty);
            } else {
                detailReason = "窗口内未命中当天计划、加机台或提前生产候选";
            }
            /*
             * 胎胚时间超窗是已确认的独立终局业务原因，未排表必须保留统一原文，
             * 便于按原因精确统计；其他延期任务继续沿用现有窗口前缀。
             */
            String finalReason = StringUtils.equals(
                    NewSpecEmbryoAvailableTimeResolver.OUT_OF_SCHEDULE_WINDOW_REASON, detailReason)
                    ? detailReason : "排程窗口最后一日仍未完成，" + detailReason;
            MachinePriorityTraceSnapshot pendingTraceSnapshot =
                    state.getPendingMachinePriorityTrace(sku);
            if (Objects.nonNull(pendingTraceSnapshot)) {
                /*
                 * 三天窗口已经确认当前 SKU 最终未形成任何实际命中时，只写最后一次实时诊断快照。
                 * 中间候选的失败原因继续使用原短日志，避免按天、按阶段重复输出完整候选列表。
                 */
                machineMatch.traceMachinePriorityOrder(
                        context, sku, pendingTraceSnapshot.withNoHit(finalReason));
                state.clearPendingMachinePriorityTrace(sku);
            }
            addUnscheduledResult(
                    context, sku, Math.max(0, remainingQty),
                    finalReason, unscheduledReasonCountMap);
            state.finalizeUnscheduled(sku);
            context.removePendingSkuFromStructureMap(sku);
            context.getNewSpecTypeRuleBlockedMap().remove(sku);
            context.getNewSpecEarlyProductionAllowedMap().remove(sku);
        }
    }

    /**
     * 判断当前 SKU 是否已经形成提前生产结果但仍需保留剩余量。
     *
     * <p>必须同时满足中心运行视图存在和结果标识为提前生产，避免仅凭结果时间或
     * 原始日计划推断，进而把正常新增、续作或换活字块结果误判为提前生产。</p>
     *
     * @param context 排程上下文
     * @param sku 待核对 SKU
     * @return true-当前窗口已经形成该 SKU 的提前生产结果；false-尚未形成
     */
    private boolean hasPartiallyScheduledEarlyProductionResult(
            LhScheduleContext context,
            SkuScheduleDTO sku) {
        if (Objects.isNull(context) || Objects.isNull(sku)
                || Objects.isNull(context.getEarlyProductionRuntimePlan(sku))
                || CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return false;
        }
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (Objects.nonNull(result)
                    && StringUtils.equals("1", result.getIsEarlyProduction())
                    && isSameSku(
                    result.getMaterialCode(), result.getProductStatus(), sku)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 执行当前业务日工作队列中的新增 SKU。
     * <p>工作队列必须按全局排序顺序过滤生成，不再按试制、量试、小批量做单控竞争重排。</p>
     *
     * @param context 排程上下文
     * @param machineMatch 机台匹配策略
     * @param mouldChangeBalance 换模均衡策略
     * @param inspectionBalance 首检均衡策略
     * @param capacityCalculate 产能策略
     * @param dayContext 当前业务日及其班次切片
     * @param state 三天窗口共用日驱动状态
     * @param unscheduledReasonCountMap 未排原因统计
     * @return 本轮新增的成功结果数
     */
    private int schedulePendingNewSpecs(LhScheduleContext context,
                                        IMachineMatchStrategy machineMatch,
                                        IMouldChangeBalanceStrategy mouldChangeBalance,
                                        IFirstInspectionBalanceStrategy inspectionBalance,
                                        ICapacityCalculateStrategy capacityCalculate,
                                        DayScheduleContext dayContext,
                                        DayDrivenScheduleState state,
                                        Map<String, Integer> unscheduledReasonCountMap) {
        int scheduledCount = 0;
        int roundNo = 1;
        List<SkuScheduleDTO> deferredCompensationSkuList = new ArrayList<SkuScheduleDTO>(2);
        while (true) {
            traceActualPendingNewSpecQueue(context, dayContext, roundNo);
            int currentRoundScheduledCount = schedulePendingNewSpecsRound(
                    context, machineMatch, mouldChangeBalance, inspectionBalance, capacityCalculate,
                    dayContext, state, unscheduledReasonCountMap, deferredCompensationSkuList);
            scheduledCount += currentRoundScheduledCount;
            if (CollectionUtils.isEmpty(deferredCompensationSkuList)) {
                return scheduledCount;
            }
            appendDeferredCompensationSkuList(
                    context, state, deferredCompensationSkuList);
            deferredCompensationSkuList.clear();
            roundNo++;
        }
    }

    /**
     * 输出新增主循环真实待排队列。
     * <p>SKU 排序汇总只记录某一时点的排序快照；当续作补偿 SKU 在新增链路中被延后插入下一轮时，
     * 需要额外记录当前轮次真实待排顺序，避免过程日志与实际执行顺序不一致。</p>
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日及资源竞争阶段
     * @param roundNo 新增主循环轮次
     */
    private void traceActualPendingNewSpecQueue(LhScheduleContext context,
                                                DayScheduleContext dayContext,
                                                int roundNo) {
        if (!PriorityTraceLogHelper.isEnabled(context)) {
            return;
        }
        String title = "新增待排队列【实际执行】";
        List<SkuScheduleDTO> pendingSkuList = context.getNewSpecSkuList();
        int skuCount = PriorityTraceLogHelper.sizeOf(pendingSkuList);
        int outputCount = Math.min(LhScheduleConstant.SKU_SORT_TRACE_TOP_N, skuCount);

        StringBuilder detailBuilder = new StringBuilder(1024);
        PriorityTraceLogHelper.appendTitleHeader(detailBuilder, title);
        PriorityTraceLogHelper.appendLine(detailBuilder,
                PriorityTraceLogHelper.kv("当前竞争日期", PriorityTraceLogHelper.formatDateTime(
                                toDate(dayContext.getScheduleDate())))
                        + ", " + PriorityTraceLogHelper.kv("步骤", context.getCurrentStep())
                        + ", " + PriorityTraceLogHelper.kv(
                                "竞争阶段", Objects.isNull(dayContext)
                                        ? null : dayContext.getCurrentPhase())
                        + ", " + PriorityTraceLogHelper.kv("轮次", roundNo)
                        + ", " + PriorityTraceLogHelper.kv("待排SKU数量", skuCount)
                        + ", " + PriorityTraceLogHelper.kv("输出范围", "TOP" + outputCount));
        if (CollectionUtils.isEmpty(pendingSkuList)) {
            PriorityTraceLogHelper.appendLine(detailBuilder, "无可输出的待排SKU");
        } else {
            for (int i = 0; i < outputCount; i++) {
                SkuScheduleDTO sku = pendingSkuList.get(i);
                PriorityTraceLogHelper.appendLine(detailBuilder,
                        "[新增待排队列] 执行序号=" + (i + 1)
                                + ", 全局rank=" + sku.getSortRank()
                                + ", sku=" + PriorityTraceLogHelper.safeText(sku.getMaterialCode())
                                + ", 补偿SKU=" + PriorityTraceLogHelper.oneZero(sku.isContinuousCompensationSku())
                                + ", 首次增机日=" + PriorityTraceLogHelper.safeText(
                                sku.getFirstAddMachineProductionDate())
                                + ", 增机缺口台数=" + sku.getContinuationShortageMachineCount()
                                + ", 增机触发差额=" + sku.getContinuationAddMachineTriggerQty()
                                + ", 目标量=" + sku.resolveTargetScheduleQty()
                                + ", 窗口量=" + PriorityTraceLogHelper.safeText(sku.getWindowPlanQty())
                                + ", 班产=" + PriorityTraceLogHelper.safeText(sku.getShiftCapacity())
                                + ", 阶段=" + resolveConstructionStageDesc(sku)
                                + ", 施工组=" + resolveNewSpecDisplayType(sku)
                                + ", 预计收尾=" + PriorityTraceLogHelper.oneZero(endingJudgmentStrategy.isExpectedEnding(context, sku)));
            }
        }
        PriorityTraceLogHelper.appendTitleFooter(detailBuilder);
        PriorityTraceLogHelper.logSortSummary(log, context, title, detailBuilder.toString());
    }

    /**
     * 记录新增SKU同机台首检顺延重试。
     * <p>该场景只调整同一候选机台的最早切换时间，不重新选择机台，因此不得重复输出候选排序，
     * 也不得消耗“物料编码+产品状态”维度的选机次数。</p>
     *
     * @param context 排程上下文
     * @param sku 当前待排SKU
     * @param machine 当前保持不变的候选机台
     * @param retryReadyTime 本次顺延后的最早切换时间
     */
    private void traceFirstInspectionSameMachineRetry(LhScheduleContext context,
                                                      SkuScheduleDTO sku,
                                                      MachineScheduleDTO machine,
                                                      Date retryReadyTime) {
        if (Objects.isNull(context) || Objects.isNull(sku) || Objects.isNull(machine)) {
            return;
        }
        String title = "【" + PriorityTraceLogHelper.safeText(sku.getMaterialCode())
                + "】【" + PriorityTraceLogHelper.safeText(sku.getProductStatus())
                + "】【" + PriorityTraceLogHelper.safeText(machine.getMachineCode())
                + "】首检顺延重试";
        String detail = "重试机台：" + PriorityTraceLogHelper.safeText(machine.getMachineCode())
                + "｜最早切换时间：" + PriorityTraceLogHelper.formatDateTime(retryReadyTime)
                + "｜选机次数：不递增"
                + "｜说明：前次首检资源或班次总量限制未通过，保持原候选机台及排序重新计算";
        PriorityTraceLogHelper.logSortSummary(log, context, title, detail);
    }

    /**
     * 输出收尾释放机台在 S4.5 新增排产后的尾部产能利用核查日志。
     * <p>该日志只做可观测性补充，不改变候选机台排序和排产结果；用于说明收尾机台
     * 为什么有后续新增结果，或者为什么没有被新增排产承接。</p>
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     */
    private void traceReleasedMachineTailCapacityAudit(LhScheduleContext context, List<LhShiftConfigVO> shifts) {
        if (!PriorityTraceLogHelper.isEnabled(context) || Objects.isNull(context)
                || CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return;
        }
        String title = "释放机台尾部产能核查";
        StringBuilder detailBuilder = new StringBuilder(2048);
        PriorityTraceLogHelper.appendTitleHeader(detailBuilder, title);
        PriorityTraceLogHelper.appendLine(detailBuilder,
                PriorityTraceLogHelper.kv("排程日期", PriorityTraceLogHelper.formatDateTime(context.getScheduleDate()))
                        + ", " + PriorityTraceLogHelper.kv("步骤", context.getCurrentStep()));

        int auditCount = 0;
        for (LhScheduleResult result : context.getScheduleResultList()) {
            // 只核查真正收尾并释放机台的结果，普通非收尾结果不参与尾部产能审计。
            if (!isEndingMachineReleaseResult(result)) {
                continue;
            }
            auditCount++;
            LhScheduleResult nextResult = findNextResultOnSameMachine(context, result);
            MachineScheduleDTO machine = CollectionUtils.isEmpty(context.getMachineScheduleMap())
                    ? null : context.getMachineScheduleMap().get(result.getLhMachineCode());
            int tailShiftCount = countTailShiftAfterRelease(shifts, result.getSpecEndTime());
            String reason = resolveReleasedMachineTailAuditReason(context, machine, result, nextResult, tailShiftCount);
            PriorityTraceLogHelper.appendLine(detailBuilder,
                    "- " + PriorityTraceLogHelper.kv("机台", result.getLhMachineCode())
                            + ", " + PriorityTraceLogHelper.kv("收尾物料", result.getMaterialCode())
                            + ", " + PriorityTraceLogHelper.kv("收尾类型", result.getScheduleType())
                            + ", " + PriorityTraceLogHelper.kv("收尾时间",
                                    PriorityTraceLogHelper.formatDateTime(result.getSpecEndTime()))
                            + ", " + PriorityTraceLogHelper.kv("剩余班次数", tailShiftCount)
                            + ", " + PriorityTraceLogHelper.kv("核查结果", reason));
        }
        if (auditCount == 0) {
            PriorityTraceLogHelper.appendLine(detailBuilder, "无收尾释放机台需要核查");
        }
        PriorityTraceLogHelper.appendTitleFooter(detailBuilder);
        PriorityTraceLogHelper.logSortSummary(log, context, title, detailBuilder.toString());
    }

    /**
     * 判断结果是否为会释放机台的收尾结果。
     *
     * @param result 排程结果
     * @return true-收尾释放结果；false-无需核查
     */
    private boolean isEndingMachineReleaseResult(LhScheduleResult result) {
        return Objects.nonNull(result)
                && StringUtils.isNotEmpty(result.getLhMachineCode())
                && "1".equals(result.getIsEnd())
                && Objects.nonNull(result.getSpecEndTime());
    }

    /**
     * 查找同机台在当前收尾结果后的下一条排产结果。
     *
     * @param context 排程上下文
     * @param releaseResult 收尾释放结果
     * @return 下一条结果；没有则返回 null
     */
    private LhScheduleResult findNextResultOnSameMachine(LhScheduleContext context, LhScheduleResult releaseResult) {
        LhScheduleResult nextResult = null;
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (result == releaseResult || Objects.isNull(result)
                    || !StringUtils.equals(releaseResult.getLhMachineCode(), result.getLhMachineCode())
                    || Objects.isNull(result.getSpecEndTime())
                    || !result.getSpecEndTime().after(releaseResult.getSpecEndTime())) {
                continue;
            }
            if (Objects.isNull(nextResult) || result.getSpecEndTime().before(nextResult.getSpecEndTime())) {
                nextResult = result;
            }
        }
        return nextResult;
    }

    /**
     * 统计收尾时间后的剩余班次数。
     *
     * @param shifts 排程窗口班次
     * @param releaseTime 收尾释放时间
     * @return 收尾后仍覆盖的班次数
     */
    private int countTailShiftAfterRelease(List<LhShiftConfigVO> shifts, Date releaseTime) {
        if (CollectionUtils.isEmpty(shifts) || Objects.isNull(releaseTime)) {
            return 0;
        }
        int count = 0;
        for (LhShiftConfigVO shift : shifts) {
            if (Objects.nonNull(shift) && Objects.nonNull(shift.getShiftEndDateTime())
                    && shift.getShiftEndDateTime().after(releaseTime)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 解析收尾释放机台尾部产能核查原因。
     *
     * @param context 排程上下文
     * @param machine 机台运行态
     * @param releaseResult 收尾释放结果
     * @param nextResult 同机台后续结果
     * @param tailShiftCount 收尾后剩余班次数
     * @return 核查原因
     */
    private String resolveReleasedMachineTailAuditReason(LhScheduleContext context,
                                                         MachineScheduleDTO machine,
                                                         LhScheduleResult releaseResult,
                                                         LhScheduleResult nextResult,
                                                         int tailShiftCount) {
        if (Objects.nonNull(nextResult)) {
            // 同机台已有后续结果，说明尾部产能已被新增或换活字块继续利用。
            return "已利用，后续物料=" + PriorityTraceLogHelper.safeText(nextResult.getMaterialCode())
                    + "，后续类型=" + PriorityTraceLogHelper.safeText(nextResult.getScheduleType())
                    + "，后续完工=" + PriorityTraceLogHelper.formatDateTime(nextResult.getSpecEndTime());
        }
        if (tailShiftCount <= 0) {
            // 收尾时间已经贴近窗口末端，没有可承接的后续班次。
            return "收尾后排程窗口内无剩余班次";
        }
        if (CollectionUtils.isEmpty(context.getNewSpecSkuList())) {
            // 新增主链已经消费完待排队列，此时不再强行制造换模或兜底补排。
            return "新增待排队列已无剩余SKU";
        }
        if (Objects.isNull(machine)) {
            return "机台运行态不存在，无法参与新增候选匹配";
        }
        if (!hasHardMatchedPendingNewSpecSku(context, machine)) {
            // 有剩余待排 SKU，但寸口、模套、特殊材料等硬约束不允许当前机台承接。
            return "剩余新增SKU与机台硬约束不匹配";
        }
        return "存在硬匹配待排SKU但未落地，需查看新增候选机台回裁跳过或新增选机过滤日志";
    }

    /**
     * 判断剩余新增 SKU 中是否存在与机台硬匹配的物料。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @return true-存在硬匹配 SKU；false-不存在
     */
    private boolean hasHardMatchedPendingNewSpecSku(LhScheduleContext context, MachineScheduleDTO machine) {
        if (CollectionUtils.isEmpty(context.getNewSpecSkuList()) || Objects.isNull(machine)) {
            return false;
        }
        for (SkuScheduleDTO sku : context.getNewSpecSkuList()) {
            if (sku == null || sku.resolveTargetScheduleQty() <= 0) {
                continue;
            }
            if (LhMachineHardMatchUtil.isMachineHardMatched(context, sku, machine)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 执行一轮新增 SKU 主排产循环。
     *
     * <p>业务步骤：</p>
     * <ul>
     *   <li>步骤1：按当前待排队列逐个 SKU 处理，先完成收尾、欠产账本和严格目标量判定；</li>
     *   <li>步骤2：调用机台匹配策略得到候选机台，候选只代表可承接，不代表最终一定排产；</li>
     *   <li>步骤3：逐台尝试换模、首检、开产时间和班次产能，失败机台加入排除集合后继续下一台；</li>
     *   <li>步骤4：按日计划账本回裁结果，当前机台不足时保留剩余量继续拆到下一台；</li>
     *   <li>步骤5：排产成功后更新结果、机台状态、机台占用和结构待排视图。</li>
     * </ul>
     *
     * <p>副作用：会修改 {@code context.newSpecSkuList}、{@code scheduleResultList}、
     * {@code scheduleResultSourceSkuMap}、机台运行态、日计划额度账本和机台占用关系。</p>
     *
     * @param context 排程上下文
     * @param machineMatch 机台匹配策略
     * @param mouldChangeBalance 换模均衡策略
     * @param inspectionBalance 首检均衡策略
     * @param capacityCalculate 产能计算策略
     * @param dayContext 当前业务日及其可写班次切片
     * @param state 三天窗口共用日驱动状态
     * @param unscheduledReasonCountMap 未排原因统计
     * @param deferredCompensationSkuList 被新增抢占的续作占位结果转出的补偿 SKU
     * @return 本轮新增的成功排产结果数；仅用于补偿 SKU 是否需要继续下一轮的收敛判断
     */
    private int schedulePendingNewSpecsRound(LhScheduleContext context,
                                             IMachineMatchStrategy machineMatch,
                                             IMouldChangeBalanceStrategy mouldChangeBalance,
                                             IFirstInspectionBalanceStrategy inspectionBalance,
                                             ICapacityCalculateStrategy capacityCalculate,
                                             DayScheduleContext dayContext,
                                             DayDrivenScheduleState state,
                                             Map<String, Integer> unscheduledReasonCountMap,
                                             List<SkuScheduleDTO> deferredCompensationSkuList) {
        int scheduledCount = 0;
        List<LhShiftConfigVO> shifts = dayContext.getDayShifts();
        refreshPendingNewSpecSkuTypeCounts(context);
        Iterator<SkuScheduleDTO> iterator = context.getNewSpecSkuList().iterator();
        while (iterator.hasNext()) {
            SkuScheduleDTO sku = iterator.next();
            /*
             * 每次真实遍历独立持有一条顺序明细：多机台候选重试只回填同一条，跨阶段或
             * 下一轮再次遍历则创建新条目，从而同时满足真实顺序和“不去重”的审计要求。
             */
            DailyNewSpecOrderLogEntry dailyOrderEntry = null;
            boolean currentSkuRemoved = false;
            String dailyDeferredReason = null;
            Date configuredEarliestEmbryoAvailableTime =
                    NewSpecEmbryoAvailableTimeResolver.resolveEarliestAvailableTime(context, sku);
            Date earliestEmbryoAvailableTime =
                    NewSpecEmbryoAvailableTimeResolver.resolveEffectiveEarliestAvailableTime(context, sku);
            boolean embryoAvailableTimeConstrained = Objects.nonNull(earliestEmbryoAvailableTime);
            if (Objects.nonNull(configuredEarliestEmbryoAvailableTime)
                    && Objects.isNull(earliestEmbryoAvailableTime)) {
                log.info("新增SKU胎胚最早可供时间因同结构续作已有有效排产而不生效, "
                                + "batchNo: {}, scheduleDate: {}, materialCode: {}, structureName: {}, "
                                + "configuredEarliestEmbryoAvailableTime: {}",
                        context.getBatchNo(), dayContext.getScheduleDate(), sku.getMaterialCode(),
                        sku.getStructureName(),
                        LhScheduleTimeUtil.formatDateTime(configuredEarliestEmbryoAvailableTime));
            }
            /*
             * 正式生产门禁继续保留“SKU类型门禁与有效胎胚可供时间取较晚值”的原口径，
             * 供正式开产、首检归属和最终产能裁剪使用。候选机台预演另取仅包含SKU类型
             * 门禁的时间，避免胎胚尚未到位时把已经在当前业务日前完成准备的机台顺延到后续日。
             */
            Date productionNotBeforeTime = NewSpecEmbryoAvailableTimeResolver
                    .resolveProductionNotBeforeTime(
                            context, sku, context.getScheduleWindowShifts());
            Date candidateProductionNotBeforeTime = NewSpecEmbryoAvailableTimeResolver
                    .resolveSkuProductionGateTime(
                            context, sku, context.getScheduleWindowShifts());
            // 兜底校验：动态生成的补偿SKU若命中减量清单，写未排并跳过（去重set保证不重复写未排）
            if (skuDecrementChecker.isDecrementHit(context, sku)) {
                boolean written = skuDecrementChecker.handleDecrementHit(context, sku);
                if (written) {
                    unscheduledReasonCountMap.merge(SKU_DECREMENT_UNSCHEDULED_REASON, 1, Integer::sum);
                }
                removeCurrentNewSpecSku(context, iterator, sku);
                state.finalizeUnscheduled(sku);
                log.info("新增主循环兜底拦截命中减量清单SKU, materialCode: {}, 已写入未排: {}", sku.getMaterialCode(), written);
                continue;
            }
            // 续作、换活字块未消费完的 SKU 在此继续参与 S4.5，并统一按小余量优先顺序执行未排判断。
            boolean isEnding = endingJudgmentStrategy.isCurrentWindowEnding(context, sku);
            boolean forceEndingByNoFuturePlan = prepareNewSpecShortageQuota(context, sku);
            boolean smallEndingSurplusRuleEnding = isEnding;
            if (forceEndingByNoFuturePlan) {
                // 窗口和月底均无未来计划时，新增按收尾清量处理，目标量允许结合胎胚库存上调。
                isEnding = true;
                smallEndingSurplusRuleEnding = true;
            } else if (sku.isStrictNewSpecShortageOnly()) {
                /*
                 * 窗口无计划但月底仍有计划时，排产结果仍按非收尾严格补本月历史欠产；
                 * 但收尾小余量规则必须保留原始收尾口径，避免硫化余量已收尾的小尾数绕过未排判断。
                 */
                isEnding = false;
            }
            // 成型胎胚库存收尾优先于SKU收尾判断，直接按胎胚库存严格控量。
            boolean embryoStockEndingTargetApplied = getTargetScheduleQtyResolver()
                    .applyEmbryoStockEndingTargetQtyIfNecessary(context, sku, "新增排产");
            /*
             * 在目标量上调和正式排产前统一判断未排规则。当前月 TOTAL_QTY=0 的提前生产
             * 中心运行视图必须读取实际消费账本剩余量，不能因通用 surplusQty 保持为0而
             * 被错误识别成收尾小余量；普通新增仍保持通用余量口径。
             */
            int smallEndingRuleQty =
                    EarlyProductionQuantityCalculator.resolveSmallEndingRuleQty(
                            context, sku, getTargetScheduleQtyResolver());
            LhUnscheduledResult ruleUnscheduledResult = PendingSkuUnscheduledRule.evaluate(
                    context, sku, smallEndingSurplusRuleEnding,
                    embryoStockEndingTargetApplied, smallEndingRuleQty);
            if (Objects.nonNull(ruleUnscheduledResult)) {
                context.getUnscheduledResultList().add(ruleUnscheduledResult);
                String unscheduledReason = ruleUnscheduledResult.getUnscheduledReason();
                unscheduledReasonCountMap.merge(unscheduledReason, 1, Integer::sum);
                removeCurrentNewSpecSku(context, iterator, sku);
                state.finalizeUnscheduled(sku);
                if (StringUtils.equals(SMALL_ENDING_SURPLUS_UNSCHEDULED_REASON, unscheduledReason)
                        || embryoStockEndingTargetApplied) {
                    getTargetScheduleQtyResolver().removeActiveEmbryoSku(context, sku, unscheduledReason);
                }
                if (StringUtils.equals(SMALL_ENDING_SURPLUS_UNSCHEDULED_REASON, unscheduledReason)) {
                    traceSmallEndingSurplusJudge(
                            context, sku, smallEndingSurplusRuleEnding,
                            smallEndingRuleQty, true);
                }
                log.info("新增SKU命中前置未排规则, materialCode: {}, unscheduledQty: {}, reason: {}",
                        sku.getMaterialCode(), ruleUnscheduledResult.getUnscheduledQty(), unscheduledReason);
                continue;
            }
            /*
             * 统一生产下限（试制/量试中班门禁与胎胚可供时间）只限制实际资源分配，
             * 不能绕过既有减量清单、收尾和前置未排规则。因此前置硬规则完成后、
             * 选机和换模资源预占前再决定是否延期：T/T+1 仅登记下一业务日延期，
             * 窗口最后一日仍未到达时写入统一终局未排原因。
             */
            if (NewSpecEmbryoAvailableTimeResolver.reachesOrPassesDayEnd(
                    productionNotBeforeTime, dayContext.getDayEndTime())) {
                boolean embryoCausedDeferral = NewSpecEmbryoAvailableTimeResolver.reachesOrPassesDayEnd(
                        earliestEmbryoAvailableTime, dayContext.getDayEndTime());
                String productionGateDeferredReason;
                if (embryoCausedDeferral) {
                    productionGateDeferredReason = dayContext.isLastScheduleDay()
                            ? NewSpecEmbryoAvailableTimeResolver.OUT_OF_SCHEDULE_WINDOW_REASON
                            : NewSpecEmbryoAvailableTimeResolver.NOT_AVAILABLE_IN_CURRENT_DAY_REASON;
                } else {
                    productionGateDeferredReason = dayContext.isLastScheduleDay()
                            ? "SKU最早合法开产时间超出排程窗口"
                            : "试制/量试首次正日计划中班尚未进入当前业务日";
                }
                log.info("新增SKU生产门禁尚未进入当前业务日，直接延期且不占用准备资源, "
                                + "batchNo: {}, scheduleDate: {}, materialCode: {}, structureName: {}, "
                                + "productionNotBeforeTime: {}, earliestEmbryoAvailableTime: {}, "
                                + "dayEndTime: {}, reason: {}",
                        context.getBatchNo(), dayContext.getScheduleDate(), sku.getMaterialCode(),
                        sku.getStructureName(),
                        LhScheduleTimeUtil.formatDateTime(productionNotBeforeTime),
                        LhScheduleTimeUtil.formatDateTime(earliestEmbryoAvailableTime),
                        LhScheduleTimeUtil.formatDateTime(dayContext.getDayEndTime()),
                        productionGateDeferredReason);
                deferCurrentDailyCandidate(
                        context, iterator, dayContext, state, sku, productionGateDeferredReason);
                continue;
            }
            // 收尾SKU在排产前上调目标量（考虑胎胚库存），非收尾SKU保持按余量计算的目标量
            boolean sharedEmbryoZeroSurplusEnding = false;
            if (isEnding && !embryoStockEndingTargetApplied) {
                sharedEmbryoZeroSurplusEnding = getTargetScheduleQtyResolver()
                        .isSharedEmbryoZeroSurplusEnding(context, sku);
                getTargetScheduleQtyResolver().upsizeEndingTargetQty(context, sku);
                if (handleSharedEmbryoZeroSurplusEndingIfNecessary(
                        context, iterator, sku, sharedEmbryoZeroSurplusEnding, unscheduledReasonCountMap)) {
                    state.finalizeUnscheduled(sku);
                    continue;
                }
            }
            ProductionQuantityPolicy quantityPolicy = ProductionQuantityPolicy.from(sku, isEnding);
            if (embryoStockEndingTargetApplied) {
                quantityPolicy.setAllowFillStartedShift(false);
                quantityPolicy.setStrictUpperLimit(true);
                quantityPolicy.setFullRunForNonTailMachine(false);
            }
            int substitutionExactScheduleQty =
                    context.resolveSubstitutionExactScheduleQty(sku);
            if (substitutionExactScheduleQty > 0) {
                /*
                 * B 的迁移量来自原续作结果实际截断尾量。它仍需完整经过普通选机、换模、首检、
                 * 停机和日计划扣账，但数量策略必须禁止满班补齐或非尾机满排，防止携带原 B
                 * 其他待排量而少排/超排本次联动组。
                 */
                quantityPolicy.setAllowFillStartedShift(false);
                quantityPolicy.setStrictUpperLimit(true);
                quantityPolicy.setFullRunForNonTailMachine(false);
                sku.setTargetScheduleQty(
                        substitutionExactScheduleQty);
                sku.setPendingQty(
                        substitutionExactScheduleQty);
                sku.setRemainingScheduleQty(
                        substitutionExactScheduleQty);
            }
            sku.setStrictTargetQty(quantityPolicy.isStrictUpperLimit());
            log.info("新增SKU开始排产, materialCode: {}, 结构: {}, 规格: {}, 月计划量: {}, 目标量: {}, "
                            + "day1/day2/day3窗口量: {}, 余量: {}, 胎胚库存: {}, 是否收尾: {}, "
                            + "允许补满已开班次: {}, 严格禁止超排: {}, 非最后机台满排: {}",
                    sku.getMaterialCode(), sku.getStructureName(), sku.getSpecCode(),
                    sku.getMonthPlanQty(), sku.resolveTargetScheduleQty(), sku.getWindowPlanQty(),
                    sku.getSurplusQty(), sku.getEmbryoStock(), isEnding,
                    quantityPolicy.isAllowFillStartedShift(), quantityPolicy.isStrictUpperLimit(),
                    quantityPolicy.isFullRunForNonTailMachine());
            if (shouldSkipNewSpecBecauseContinuousSatisfiesOriginalDayMinimum(context, sku, quantityPolicy)) {
                removeCurrentNewSpecSku(context, iterator, sku);
                state.complete(sku);
                continue;
            }

            if (shouldSkipTrialSku(context, sku)) {
                addUnscheduledResult(context, sku, "试制量试当日不可排产", unscheduledReasonCountMap);
                removeCurrentNewSpecSku(context, iterator, sku);
                state.finalizeUnscheduled(sku);
                continue;
            }

            // 1. 匹配候选机台：只做硬性准入和候选排序，换模/首检/产能在后续逐台试算。
            context.getNewSpecTypeRuleBlockedMap().remove(sku);
            // 选机前按当前日内阶段刷新提前生产准入，非提前生产阶段不得把未来 dayN 当作当前资源。
            refreshNewSpecEarlyProductionAdmission(
                    context, sku, shifts, isEnding, dayContext.getCurrentPhase());
            List<MachineScheduleDTO> candidates = machineMatch.matchMachines(context, sku);
            if (context.isSpecialMaterialSpecifiedSku(sku)
                    || context.isScheduleSubstitutionSku(sku)) {
                /*
                 * S4.5.1 置换复用本新增主链时，禁止执行历史反选机台处理：
                 * A 接管及正式提交的 B 迁移只能尝试预演确认机台；B 的首次迁移预演允许正常选机，
                 * 但必须排除 A 已接管的原物理机台。两种场景都不得改写与本次联动无关的反选状态。
                 */
                candidates = restrictSubstitutionCandidates(
                        context, sku, candidates, machineMatch);
            } else {
                /*
                 * 普通新增排产继续沿用前日交替计划指定机台优先规则；指定机台失败后仍可使用
                 * 原普通候选列表，确保特殊材料置换改造不改变普通新增排产语义。
                 */
                candidates = prioritizeHistoricalReverseSpecifiedMachines(
                        context, sku, candidates, machineMatch);
            }
            /*
             * 按天换活字块反选机台预留：命中 SKU 的预留机台置顶优先尝试，
             * 其余候选保持原顺序；命中物料已前置到当天工作队列，正常竞争阶段内
             * 其他物料不会先于它占用预留机台。预留机台失败时自动回退普通候选列表。
             */
            candidates = applyDayTypeBlockReverseSelection(context, sku, candidates);
            logNewSpecMachineCandidateSnapshot(context, sku, candidates, EMPTY_STRING_SET, null);
            if (candidates.isEmpty()) {
                /*
                 * 初始正式候选为空时仍构建独立日志快照。生产默认策略会从当前实时分配结果中补充
                 * “仅因其它 SKU 占用而暂不可选”的机台；快照不会写回 candidates，也不会改变未排结论。
                 */
                MachinePriorityTraceSnapshot emptyCandidateTraceSnapshot =
                        machineMatch.buildMachinePriorityTraceSnapshot(
                                context, sku, Collections.<MachineScheduleDTO>emptyList(),
                                null, dayContext.getDayEndTime(),
                                getTargetScheduleQtyResolver())
                                .withTraceSkuType(this.resolveTraceSkuType(context, sku));
                String noCandidateReason = resolveNoCandidateMachineReason(context, sku);
                /*
                 * 当前日、当前阶段无正式候选并不等于三天窗口最终未排。只把实时快照保存在日驱动状态，
                 * 后续若实际命中会自动清理；只有窗口最终仍未命中才输出一次完整诊断日志。
                 */
                state.rememberPendingMachinePriorityTrace(
                        sku, emptyCandidateTraceSnapshot);
                log.warn("新增SKU无候选机台, materialCode: {}, 结构: {}, 规格: {}, 寸口: {}, 目标量: {}, 原因: {}",
                        sku.getMaterialCode(), sku.getStructureName(), sku.getSpecCode(),
                        sku.getProSize(), sku.resolveTargetScheduleQty(), noCandidateReason);
                traceNewSpecMachineDecision(context, sku, candidates, null, null,
                        EMPTY_STRING_SET, EMPTY_STRING_MAP,
                        NewSpecFailReasonEnum.MACHINE_SELECTION_FAILED,
                        false, noCandidateReason, null);
                /*
                 * T+2 的当天计划或加机台阶段之后仍有提前生产阶段，资源失败不能提前写最终未排。
                 * 统一登记延期原因，全部阶段结束后由 finalizeWindowUnscheduled 一次性结算。
                 */
                deferCurrentDailyCandidate(context, iterator, dayContext, state, sku,
                        noCandidateReason);
                continue;
            }

            /*
             * 旧局部搜索只生成诊断建议，且内部另行估算换模、首检和产能。新流程禁止平行时间轴
             * 与统一逐班选择并存，因此停止执行该评估；保留空值仅兼容现有决策日志方法签名。
             */
            MachineScheduleDTO localSearchSuggestedMachine = null;
            MachineScheduleDTO preferredTrialMachine = resolvePreferredTrialMachine(context, sku, candidates);

            // 2. 基于策略选择最优机台，失败后排除并继续选择下一台。
            // 多机台拆量：当一台机台产能不足以排完目标量时，继续尝试下一台机台。
            boolean scheduled = false;
            NewSpecFailReasonEnum failReason = NewSpecFailReasonEnum.MACHINE_SELECTION_FAILED;
            Set<String> excludedMachineCodes = new HashSet<>(candidates.size());
            Map<String, String> excludedMachineReasonMap = new LinkedHashMap<>(candidates.size());
            Map<String, String> structureAlignmentExcludedReasonMap =
                    new LinkedHashMap<>(candidates.size());
            // originalTargetScheduleQty 是进入本 SKU 前的业务目标量，用于所有候选失败后恢复原口径。
            Integer originalTargetScheduleQty = sku.getTargetScheduleQty();
            int minimumTargetScheduleQty = resolveFormalNonEndingMinimumTargetQty(context, sku, quantityPolicy);
            if (minimumTargetScheduleQty > 0) {
                // 正规/量试非收尾在满排口径下可临时抬高目标，避免单机台只按 dayN 小目标提前结束。
                sku.setTargetScheduleQty(minimumTargetScheduleQty);
            }
            // baseTargetScheduleQty 是本轮多机台拆量的业务基准，单台失败或继续下一台时按它恢复。
            Integer baseTargetScheduleQty = sku.getTargetScheduleQty();
            Integer finalTargetScheduleQty = baseTargetScheduleQty;
            /*
             * 在机延续阶段已经在这些机台的原结果上完成当前日连续生产。后续选机阶段只能选择真正
             * 尚未绑定的新机台；把绑定机台直接加入本轮排除集合，避免同 SKU 在原机台再次
             * 预占模具、换模、首检并生成重复结果。该排除只限制“新选机”，不解除跨日在机绑定。
             */
            excludeBoundMachinesFromNewSelection(
                    state, sku, candidates, excludedMachineCodes, excludedMachineReasonMap);
            // 初始化多机台拆量剩余量：dayN只做节奏判断，实际拆机按SKU实际消费账本剩余额度收敛。
            int remainingQty = resolveSchedulableRemainingQty(context, sku);
            // 非收尾可溢出场景下，dynamicTargetQty 至少为一个满班产能，
            // 确保 shouldFillSingleMachineToWindowEnd 能按满班产能补足已开班次。
            if (quantityPolicy != null && quantityPolicy.isAllowFillStartedShift() && !quantityPolicy.isEnding()) {
                int shiftCapacity = sku.getShiftCapacity();
                if (shiftCapacity > 0) {
                    remainingQty = Math.max(remainingQty, shiftCapacity);
                }
            }
            // dynamicTargetQty 会随着 dayN 扩机台判断动态收敛，表示当前多机台组还需要消化的窗口目标。
            int dynamicTargetQty = remainingQty;
            sku.setRemainingScheduleQty(remainingQty);
            MachineScheduleDTO finalMachine = null;
            Date finalProductionStartTime = null;
            // 多机台累计调度结果，用于最终按总量、日计划账本和满班超排口径确认排完与否。
            int totalScheduledQty = 0;
            // dayN模拟按新增顺序记录机台生效日期，后续第N台不得提前到其对应业务日之前生产。
            List<LocalDate> addMachineProductionDateList = new ArrayList<LocalDate>(4);
            int originalAddMachineCount =
                    countAvailableCandidateMachines(candidates, excludedMachineCodes);
            int actualAllowedAddMachineCount = 0;
            LhScheduleResult lastScheduledResult = null;
            MachineProductionSegment lastScheduledSegment = null;
            NewSpecCandidateCache candidateCache = NewSpecCandidateCache.from(candidates,
                    machine -> isSingleControlMachine(context, machine.getMachineCode()));
            // 首检资源或SYS0303004不允许当前落点时，记录同机台下一次允许尝试的最早切换时间。
            Map<String, Date> firstInspectionRetryReadyTimeMap = new HashMap<String, Date>(8);
            // 仅标记紧接着发生的同机台首检顺延重试，避免把时间后移重算误记为一次新的选机。
            String pendingFirstInspectionRetryMachineCode = null;
            /*
             * 当前候选的完整实时日志快照。完整观察范围仍采用延迟构建；候选确定时只额外冻结
             * 正式候选的轻量软排序指标，确保正式模具分配不会污染后续同模壳日志。
             */
            MachinePriorityTraceSnapshot pendingCandidateTraceSnapshot = null;
            /*
             * 最近一次已确认命中的选机时点快照。TOP5 日志延迟到整轮结束后才写入，
             * 此时机台运行态可能已被本轮结果推进；使用冻结快照中的收尾时间，保证与
             * “选机优先级顺序”日志、正式选机画像读取同一份时间源。
             */
            MachinePriorityTraceSnapshot lastConfirmedTraceSnapshot = null;
            // 延迟构建暂存的最近一次真实候选输入：有序候选列表、首选机台与当日结束时间（仅引用，零计算）。
            List<MachineScheduleDTO> pendingTraceCandidates = null;
            MachineScheduleDTO pendingTraceSelectedMachine = null;
            Date pendingTraceDayEndTime = null;
            Map<String, MachinePriorityMetricSnapshot> pendingPriorityMetricSnapshotMap =
                    Collections.<String, MachinePriorityMetricSnapshot>emptyMap();
            /*
             * 当前选机回合的真实可开产计划缓存。每台候选只保留一个轻量计划，SKU结束即释放，
             * 禁止构造跨SKU长生命周期矩阵，避免候选较多时放大堆内存。
             */
            Map<String, NewSpecMachineAvailabilityPlan> candidateAvailabilityPlanMap =
                    new LinkedHashMap<String, NewSpecMachineAvailabilityPlan>(
                            Math.max(8, candidates.size() * 2));
            // 当天只竞争中心规则明确要求的新增机台数；下一台生效日在未来时冻结到该日再竞争。
            boolean futureAddMachineDateDeferred = false;
            LocalDate nextAddMachineAttemptDate = null;
            while (true) {
                LocalDate currentAddMachineProductionDate = resolveCurrentAddMachineProductionDate(
                        context, sku, addMachineProductionDateList, actualAllowedAddMachineCount);
                if (this.shouldWaitForFutureAddMachineDate(
                        dayContext, currentAddMachineProductionDate)) {
                    futureAddMachineDateDeferred = true;
                    nextAddMachineAttemptDate = currentAddMachineProductionDate;
                    dailyDeferredReason = this.buildFutureAddMachineDeferredReason(
                            currentAddMachineProductionDate, dayContext.getScheduleDate());
                    log.info("新增SKU下一台机台未到业务生效日，停止当前日继续竞争, batchNo: {}, "
                                    + "scheduleDate: {}, materialCode: {}, productStatus: {}, "
                                    + "globalRank: {}, currentSuccessfulAddMachineCount: {}, "
                                    + "nextAddMachineProductionDate: {}, reason: {}",
                            context.getBatchNo(), dayContext.getScheduleDate(),
                            sku.getMaterialCode(), sku.getProductStatus(), sku.getSortRank(),
                            actualAllowedAddMachineCount, currentAddMachineProductionDate,
                            dailyDeferredReason);
                    break;
                }
                /*
                 * dayN 理论机台数硬上限前置检查：当前 SKU 已落地机台数（含同物料续作、
                 * 换活字块与本轮已排机台）达到上限后，直接停止本轮全部新增机台尝试，
                 * 剩余目标量交由未排/下一滚动窗口承接。
                 * 必须放在候选选择之前，避免“先排一台再停止”在多轮次/多实例下仍多开机台。
                 */
                if (isNewSpecDayNMachineCountCapReached(
                        context, sku, dayContext.getScheduleDate())) {
                    // 已达上限时不再尝试任何新增机台；本轮未形成结果，交由 !scheduled 分支
                    // 统一延期到后续业务日（后续轮次前置检查会再次拦截，窗口收口时落未排）。
                    appendNewSpecDailyRhythmStopProcessLog(context, sku, null,
                            Objects.nonNull(baseTargetScheduleQty)
                                    ? Math.max(0, baseTargetScheduleQty) : totalScheduledQty,
                            totalScheduledQty, "已满足dayN理论机台数上限，停止继续扩机");
                    dailyDeferredReason = "已满足dayN理论机台数上限，停止继续扩机";
                    break;
                }
                /*
                 * 当前 SKU 已通过当天所有前置过滤、加机生效日和 dayN 上限，下一步将使用
                 * 真实候选列表执行选机。首次进入时按实际遍历顺序追加一条，后续同轮机台重试
                 * 只更新该条目标机台数，不重复追加 SKU 顺序。
                 */
                if (Objects.isNull(dailyOrderEntry)) {
                    dailyOrderEntry = this.recordDailyNewSpecOrder(context, dayContext, sku);
                }
                if (Objects.nonNull(dailyOrderEntry)) {
                    dailyOrderEntry.updateRequiredMachineCount(
                            this.lhDailyMouldCalcService.getRequiredMachineCount(
                                    context, sku.getMaterialCode(), sku.getProductStatus(),
                                    dayContext.getScheduleDate()));
                }
                /*
                 * 当前选机回合只采集一次实时统计：结果列表扫描不进入候选机台循环；候选失败重试
                 * 复用该快照，当前 SKU 成功落地后下一台机台回合会重新采集并看到最新状态。
                 * 采集点位于换模/换活字块正式分配之前，确保次数统计不包含本次即将产生的切换。
                 */
                NewSpecSelectionRealtimeSnapshot selectionRealtimeSnapshot =
                        NewSpecSelectionRealtimeSnapshot.capture(
                                context, sku, configuredEarliestEmbryoAvailableTime,
                                dailyOrderEntry, mouldChangeBalance,
                                dayContext.getNewSpecOrderLogCollector().getDateOffset());
                /*
                 * 基础硬约束或当前日尝试失败仍可能在后续业务日恢复，T、T+1 必须保留历史
                 * “机台+后物料”关系；窗口最后一日才结算这类临时失败。整窗产能为0属于
                 * 终局失败，在形成真实可选候选后单独即时结算，不受该日期门槛限制。
                 */
                if (dayContext.isLastScheduleDay()) {
                    finalizeRejectedHistoricalReverseDirectives(
                            context, sku, excludedMachineCodes, excludedMachineReasonMap);
                }
                logNewSpecMachineCandidateSnapshot(context, sku, candidates, excludedMachineCodes, excludedMachineReasonMap);
                /*
                 * 在任何动态置顶、排序和日志输出前，只剔除本轮已失败机台及结构收尾硬排除。
                 * 旧窗口产能属于正式时间轴之前的近似结果，不能再据此提前删除候选；每台硬候选
                 * 是否有产能，统一交给后续真实可开产计划结合换模、首检和设备计划逐班判定。
                 */
                List<MachineScheduleDTO> currentSelectableCandidates =
                        filterCurrentSelectableCandidates(
                                context, sku, candidates, excludedMachineCodes,
                                structureAlignmentExcludedReasonMap);
                /*
                 * 历史班次剩余产能优先只作用于普通新增候选分层。来源班次、剩余区间和可排量
                 * 附着在当前选机回合的统一可开产计划中，不写入长生命周期机台DTO，也不进入
                 * 现有八层Comparator，避免影响其它SKU或既有软排序语义。
                 */
                List<MachineScheduleDTO> candidatesBeforeShiftSelection =
                        currentSelectableCandidates;
                List<MachineScheduleDTO> currentShiftCandidates = Collections.emptyList();
                List<MachineScheduleDTO> historyResidualCapacityCandidates =
                        Collections.emptyList();
                LhShiftConfigVO currentTargetShift = null;
                boolean historyResidualCapacityPreferenceApplied = false;
                boolean historyResidualCapacityPreferenceEvaluated = false;
                String historyResidualCapacityPreferenceSkipReason = null;
                if (!hasPendingHistoricalReverseDirectiveForSku(context, sku)) {
                    /*
                     * 续作补偿锁回、试制/量试/小批量定点预选和按天换活字块反选预留属于既有
                     * 固定规则。固定机台仍在当前真实候选内时保持原优先级，历史剩余产能分层
                     * 不得覆盖这些已确认的固定关系。
                     */
                    historyResidualCapacityPreferenceSkipReason =
                            this.resolveHistoryResidualCapacityPreferenceSkipReason(
                                    context, sku, currentSelectableCandidates, preferredTrialMachine);
                    boolean historyResidualCapacityPreferenceEnabled =
                            StringUtils.isEmpty(historyResidualCapacityPreferenceSkipReason);
                    /*
                     * 统一时间轴先计算全部候选在当前业务日的正式可开产计划，再以普通候选最早
                     * 目标班次为锚点向前24小时扫描历史剩余产能。命中历史池时不再要求候选正式
                     * 开产班次等于锚点班次；无命中时完整回退原逐班筛选逻辑。
                     */
                    currentSelectableCandidates = this.filterByEarliestAvailableShift(
                            context, sku, currentSelectableCandidates, dayContext,
                            capacityCalculate, mouldChangeBalance, inspectionBalance,
                            candidateProductionNotBeforeTime, productionNotBeforeTime,
                            dynamicTargetQty, totalScheduledQty, currentAddMachineProductionDate,
                            isEnding, historyResidualCapacityPreferenceEnabled,
                            candidateAvailabilityPlanMap);
                    currentTargetShift = this.resolveEarliestAvailableTargetShift(
                            dayContext.getDayShifts(), candidateAvailabilityPlanMap);
                    currentShiftCandidates = this.filterCandidatesByFormalTargetShift(
                            candidatesBeforeShiftSelection, currentTargetShift,
                            candidateAvailabilityPlanMap);
                    historyResidualCapacityCandidates = currentSelectableCandidates.stream()
                            .filter(Objects::nonNull)
                            .filter(candidate -> this.isHistoryResidualCapacityCandidate(
                                    candidate, candidateAvailabilityPlanMap))
                            .collect(Collectors.toList());
                    historyResidualCapacityPreferenceEvaluated =
                            historyResidualCapacityPreferenceEnabled
                                    && Objects.nonNull(currentTargetShift);
                    historyResidualCapacityPreferenceApplied =
                            !CollectionUtils.isEmpty(historyResidualCapacityCandidates);
                    if (CollectionUtils.isEmpty(currentSelectableCandidates)) {
                        dailyDeferredReason = "当前业务日全部班次均无真实可开产候选机台";
                    }
                }
                int candidateCountBeforeStructureAlignment =
                        countAvailableCandidateMachines(candidates, excludedMachineCodes);
                if (CollectionUtils.isEmpty(currentSelectableCandidates)
                        && candidateCountBeforeStructureAlignment > 0
                        && structureAlignmentExcludedReasonMap.size()
                        == candidateCountBeforeStructureAlignment) {
                    /*
                     * 全部候选均被结构收尾对齐排除时，记录本次实时判断明细并写入延期原因。
                     * 不把机台加入永久排除集合，后续业务日仍会按最新在机缓存重新判断。
                     */
                    excludedMachineReasonMap.putAll(structureAlignmentExcludedReasonMap);
                    dailyDeferredReason = "结构收尾对齐未找到可承接的同结构机台，当前排除"
                            + structureAlignmentExcludedReasonMap.size() + "台候选机台";
                }
                /*
                 * 指定机台虽然通过基础硬过滤，但若被正式窗口产能计算排除，说明本批整个窗口均无法生产。
                 * 当前轮必须立即把对应反选指令结算失败；否则指令会永久保持未尝试并触发无限下一轮。
                 */
                finalizeUnselectableHistoricalReverseDirectives(
                        context, sku, currentSelectableCandidates);
                MachineScheduleDTO candidateMachine = null;
                List<MachineScheduleDTO> orderedCandidates = new ArrayList<>(candidates.size());
                HistoricalReverseSelectionDirective historicalDirective =
                        findNextHistoricalReverseDirective(
                                context, sku, currentSelectableCandidates, EMPTY_STRING_SET);
                if (Objects.nonNull(historicalDirective)) {
                    candidateMachine = findMachineInList(
                            currentSelectableCandidates, historicalDirective.getEffectiveMachineCode());
                    fillSelectedCandidateOrder(
                            currentSelectableCandidates, candidateMachine, orderedCandidates);
                    log.info("前日交替计划指定机台优先尝试, materialCode: {}, productStatus: {}, "
                                    + "historicalShift: {}, mappedShift: {}, historicalMachine: {}, effectiveMachine: {}",
                            sku.getMaterialCode(), sku.getProductStatus(),
                            historicalDirective.getHistoricalShiftIndex(),
                            historicalDirective.getMappedShiftIndex(),
                            historicalDirective.getMachineCode(),
                            historicalDirective.getEffectiveMachineCode());
                }
                if (candidateMachine == null) {
                    if (!dayContext.isLastScheduleDay()
                            && hasPendingHistoricalReverseDirectiveForSku(context, sku)) {
                        dailyDeferredReason = "历史指定机台当前业务日资源不足，保留固定关系转下一业务日";
                        log.info("历史反选当前日暂缓普通回落, batchNo: {}, scheduleDate: {}, "
                                        + "materialCode: {}, productStatus: {}, reason: {}",
                                context.getBatchNo(), dayContext.getScheduleDate(),
                                sku.getMaterialCode(), sku.getProductStatus(), dailyDeferredReason);
                        break;
                    }
                    /*
                     * 历史指定机台只属于当前 SKU 的选机指令，不能等待其他 SKU 的历史指令后再
                     * 允许当前 SKU 普通回落。否则虽然没有改写集合顺序，实际资源占用顺序仍会
                     * 反向覆盖 Handler 已完成的业务优先级排序。
                     */
                    candidateMachine = selectCandidateMachine(
                            context, sku, candidateCache, currentSelectableCandidates,
                            excludedMachineCodes, machineMatch,
                            preferredTrialMachine, orderedCandidates);
                }
                /*
                 * 调用处补齐当前实际可选作用域：保持选中机台第一、原选机分组相对顺序不变，
                 * 再追加本轮其它实际候选。该列表只为日志完整性补齐，第一台仍是原逻辑已经选中的机台。
                 */
                completeActualCandidateOrder(
                        currentSelectableCandidates, candidateMachine,
                        excludedMachineCodes, orderedCandidates);
                if (Objects.nonNull(currentTargetShift) && log.isInfoEnabled()) {
                    HistoricalResidualCapacityInfo selectedResidualCapacityInfo =
                            this.resolveHistoricalResidualCapacityInfo(
                                    candidateMachine, candidateAvailabilityPlanMap);
                    /*
                     * 每个真实选机回合写应用日志，供 fresh 批次逐轮审计；不写入scheduleLogList，
                     * 避免失败候选和首检重试累计大文本。成功回合仍在结果提交后写数据库过程日志。
                     */
                    log.info("新增SKU历史班次剩余产能优先选机, batchNo: {}, scheduleDate: {}, "
                                    + "materialCode: {}, productStatus: {}, currentShift: {}, sourceShift: {}, "
                                    + "evaluated: {}, applied: {}, reason: {}, currentCandidates: {}, "
                                    + "historyCandidates: {}, finalCandidates: {}, selectedMachine: {}, "
                                    + "residualStartTime: {}, residualEndTime: {}, residualCapacityQty: {}",
                            context.getBatchNo(), context.getCurrentScheduleDate(),
                            sku.getMaterialCode(), sku.getProductStatus(),
                            this.formatShiftIndex(currentTargetShift),
                            Objects.isNull(selectedResidualCapacityInfo)
                                    ? "无" : this.formatShiftIndex(
                                    selectedResidualCapacityInfo.getSourceShift()),
                            historyResidualCapacityPreferenceEvaluated,
                            historyResidualCapacityPreferenceApplied,
                            StringUtils.defaultIfEmpty(historyResidualCapacityPreferenceSkipReason,
                                    historyResidualCapacityPreferenceApplied
                                            ? "命中历史班次剩余产能优先池"
                                            : "无机台命中，回退当前班次全部候选"),
                            this.formatMachineCodes(currentShiftCandidates),
                            this.formatHistoryResidualCapacityCandidates(
                                    historyResidualCapacityCandidates,
                                    candidateAvailabilityPlanMap),
                            this.formatMachineCodes(orderedCandidates),
                            Objects.isNull(candidateMachine) ? "无" : candidateMachine.getMachineCode(),
                            Objects.isNull(selectedResidualCapacityInfo) ? "无"
                                    : LhScheduleTimeUtil.formatDateTime(
                                    selectedResidualCapacityInfo.getAvailableStartTime()),
                            Objects.isNull(selectedResidualCapacityInfo) ? "无"
                                    : LhScheduleTimeUtil.formatDateTime(
                                    selectedResidualCapacityInfo.getAvailableEndTime()),
                            Objects.isNull(selectedResidualCapacityInfo) ? 0
                                    : selectedResidualCapacityInfo.getResidualCapacityQty());
                }
                // candidateMachine 保持原选机方法返回值，日志顺序补齐结果不得反向参与实际排产决策。
                boolean sameMachineFirstInspectionRetry = Objects.nonNull(candidateMachine)
                        && StringUtils.equals(
                                pendingFirstInspectionRetryMachineCode, candidateMachine.getMachineCode());
                if (Objects.nonNull(candidateMachine)) {
                    if (sameMachineFirstInspectionRetry) {
                        /*
                         * 首检资源或班次总量限制只会后移同一机台的切换时间，候选选择并未发生变化。
                         * 本轮不重复输出候选排序、不消耗选机次数，单独记录顺延重试原因供测试核对；
                         * 暂存输入保持首次尝试的快照输入，命中后沿用同一候选顺序。
                         */
                        traceFirstInspectionSameMachineRetry(
                                context, sku, candidateMachine,
                                firstInspectionRetryReadyTimeMap.get(candidateMachine.getMachineCode()));
                    } else {
                        /*
                         * 暂存本轮真实候选输入，并在任何正式模具分配前冻结全部可变软排序指标。
                         * 完整占用观察快照仍延迟到命中或未排收口时构建，避免重复扫描全厂机台。
                         */
                        pendingTraceCandidates = orderedCandidates;
                        pendingTraceSelectedMachine = candidateMachine;
                        pendingTraceDayEndTime = dayContext.getDayEndTime();
                        pendingPriorityMetricSnapshotMap =
                                machineMatch.captureMachinePriorityMetricSnapshots(
                                        context, sku, orderedCandidates);
                    }
                } else {
                    /*
                     * 本轮没有实际机台可试排时，保留最近一次真实候选的暂存输入；
                     * 全程无候选时由收口逻辑按空候选构建一次占用诊断快照。
                     */
                }
                // 当前轮已消费上一轮重试标记；本轮若再次需要顺延，会在对应失败分支重新写入。
                pendingFirstInspectionRetryMachineCode = null;
                if (Objects.isNull(candidateMachine)) {
                    /*
                     * 提前生产结构已达计划机台数、且当前不存在真实可复用候选时，虽然没有机台进入
                     * 后续资源扣减，也必须把“禁止再新增物理机台”的业务原因带到最终未排结果。
                     * 该判断只补充本阶段选机失败原因，不在公共候选入口拦截正常排产或历史欠产。
                     */
                    if (StringUtils.isEmpty(dailyDeferredReason)
                            && CollectionUtils.isEmpty(currentSelectableCandidates)) {
                        String noSelectableMachineLimitReason =
                                this.resolveEarlyProductionStructureMachineLimitReason(
                                        context, dayContext, sku, null);
                        if (StringUtils.isNotEmpty(noSelectableMachineLimitReason)) {
                            dailyDeferredReason = noSelectableMachineLimitReason;
                            this.appendEarlyProductionStructureMachineLimitLog(
                                    context, dayContext, sku, null,
                                    noSelectableMachineLimitReason);
                        }
                    }
                    break;
                }
                String machineCode = candidateMachine.getMachineCode();
                /*
                 * 普通新增候选已经在逐班筛选阶段生成真实可开产计划。正式提交阶段优先读取
                 * 同一轻量计划，禁止日志、选机和结果各自重新推导时间。历史固定机台不经过
                 * 普通逐班筛选，因此允许计划为空并继续沿用其既有专用时间轴。
                 */
                NewSpecMachineAvailabilityPlan selectedAvailabilityPlan =
                        candidateAvailabilityPlanMap.get(machineCode);
                Date releasedContinuationReuseStartTime =
                        Objects.isNull(selectedAvailabilityPlan)
                                ? null : this.resolveReleasedContinuationReuseStartTime(
                                context, dayContext, sku,
                                selectedAvailabilityPlan.getMachineReadyTime());
                boolean substitutionTakeoverWithoutMouldChange =
                        context.isScheduleSubstitutionSku(sku)
                                && Objects.nonNull(context.getScheduleSubstitutionDirective())
                                && context.getScheduleSubstitutionDirective()
                                .isTakeoverWithoutMouldChange();
                // 候选可能来自普通排序，按实际选中机台重新确认本轮是否属于历史指定机台尝试。
                historicalDirective = findHistoricalReverseDirective(
                        context, sku, machineCode, false);
                if (StringUtils.isEmpty(machineCode)) {
                    log.warn("候选机台编码为空，跳过新增SKU排产, materialCode: {}, 目标量: {}",
                            sku.getMaterialCode(), sku.resolveTargetScheduleQty());
                    failReason = selectHigherPriorityFailReason(
                            failReason, NewSpecFailReasonEnum.MACHINE_SELECTION_FAILED);
                    break;
                }
                // 业务日循环已确定模具到货和占用日期，dayN 增机日只能作为非日驱动调用的回退信息。
                refreshCurrentScheduleDate(
                        context, dayContext.getScheduleDate(), sku, currentAddMachineProductionDate);
                MachineScheduleDTO pairSingleControlMachine =
                        resolveWholeSingleControlPairMachine(context, sku, candidateMachine);
                boolean wholeSingleControlUnit = Objects.nonNull(pairSingleControlMachine);
                MouldResourceAllocationResult continuationReusePreviewResult =
                        this.previewMouldResourceForAddMachine(context, sku, candidateMachine);
                MouldResourceAllocationResult pairContinuationReusePreviewResult =
                        wholeSingleControlUnit
                                ? this.previewMouldResourceForAddMachine(
                                context, sku, pairSingleControlMachine)
                                : null;
                /*
                 * 不在续作阶段提前锁定机台。只有轮到当前SKU、原续作机台仍在当轮
                 * 真实候选集中并被选中后，才校验“同物料+同产品状态+原机台整套模具未变”。
                 * 任一条件不成立时仍走普通新增候选和真实换模链路。
                 */
                boolean releasedContinuationReuse =
                        Objects.nonNull(releasedContinuationReuseStartTime)
                                && this.isReleasedContinuationSameMouldReuse(
                                context, sku, candidateMachine,
                                continuationReusePreviewResult)
                                && (!wholeSingleControlUnit
                                || this.isReleasedContinuationPairSameMouldReuse(
                                context, sku, pairSingleControlMachine,
                                pairContinuationReusePreviewResult));
                if (releasedContinuationReuse) {
                    LhShiftConfigVO reuseShift =
                            NewSpecEmbryoAvailableTimeResolver.resolveProductionShift(
                                    context.getScheduleWindowShifts(),
                                    releasedContinuationReuseStartTime);
                    selectedAvailabilityPlan = selectedAvailabilityPlan
                            .withReleasedContinuationReuse(
                                    releasedContinuationReuseStartTime, reuseShift);
                    candidateAvailabilityPlanMap.put(
                            machineCode, selectedAvailabilityPlan);
                }
                boolean takeoverWithoutMouldChange =
                        substitutionTakeoverWithoutMouldChange || releasedContinuationReuse;
                /*
                 * 同胎胚且同模具的异物料切换按换活字块口径处理；同物料原模具续作重启
                 * 已在上方识别为无换模续作，禁止再落成换活字块或正规换模。
                 */
                boolean isTypeBlockRelation = !takeoverWithoutMouldChange
                        && Objects.isNull(historicalDirective)
                        && TypeBlockRelationUtil.isSameEmbryoAndSameMould(
                        context, candidateMachine, sku);
                String inspectionScheduleTypeCode = isTypeBlockRelation
                        ? ScheduleTypeEnum.TYPE_BLOCK.getCode()
                        : releasedContinuationReuse
                        ? ScheduleTypeEnum.CONTINUOUS.getCode()
                        : ScheduleTypeEnum.NEW_SPEC.getCode();
                /*
                 * 结构计划机台数只约束提前生产新增物理机台。校验必须放在候选已经确定、
                 * 模具和胎胚等资源尚未扣减的位置：结构达到计划数后可继续复用本结构已有机台，
                 * 但禁止候选把结构物理机台数继续增加；正常排产及真实历史欠产完全不走此分支。
                 */
                String earlyProductionStructureLimitReason =
                        this.resolveEarlyProductionStructureMachineLimitReason(
                                context, dayContext, sku, candidateMachine.getMachineCode());
                if (StringUtils.isNotEmpty(earlyProductionStructureLimitReason)) {
                    excludedMachineCodes.add(machineCode);
                    candidateCache.removeMachine(machineCode);
                    this.recordExcludedMachineReason(
                            excludedMachineReasonMap, machineCode,
                            earlyProductionStructureLimitReason,
                            null, null, null, null, null, null, null, null, null);
                    this.appendEarlyProductionStructureMachineLimitLog(
                            context, dayContext, sku, candidateMachine.getMachineCode(),
                            earlyProductionStructureLimitReason);
                    dailyDeferredReason = earlyProductionStructureLimitReason;
                    failReason = this.selectHigherPriorityFailReason(
                            failReason, NewSpecFailReasonEnum.MACHINE_SELECTION_FAILED);
                    continue;
                }
                // 原机台原模具续作重启不重复释放/分配模具；普通候选仍执行原正式预占链。
                MouldResourceAllocationResult mouldResourceAllocationResult = releasedContinuationReuse
                        ? continuationReusePreviewResult
                        : tryAllocateMouldResourceForAddMachine(
                        context, sku, candidateMachine,
                        originalAddMachineCount, actualAllowedAddMachineCount);
                MouldResourceAllocationResult pairMouldResourceAllocationResult = null;
                if (!mouldResourceAllocationResult.isAllowed()) {
                    excludedMachineCodes.add(machineCode);
                    candidateCache.removeMachine(machineCode);
                    recordExcludedMachineReason(excludedMachineReasonMap, machineCode,
                            mouldResourceAllocationResult.getSkipReason().getDescription(),
                            null, null, null, null, null, null, null, null, null);
                    failReason = selectHigherPriorityFailReason(
                            failReason, NewSpecFailReasonEnum.MACHINE_SELECTION_FAILED);
                    continue;
                }
                if (wholeSingleControlUnit) {
                    // 正规SKU使用单控机台时，L/R两边必须作为一个物理整机同步预占模具；副侧失败则主侧也回滚。
                    pairMouldResourceAllocationResult = releasedContinuationReuse
                            ? pairContinuationReusePreviewResult
                            : tryAllocateMouldResourceForAddMachine(
                            context, sku, pairSingleControlMachine,
                            originalAddMachineCount, actualAllowedAddMachineCount);
                    if (!pairMouldResourceAllocationResult.isAllowed()) {
                        rollbackMouldResourceAllocation(context, sku, mouldResourceAllocationResult);
                        excludedMachineCodes.add(machineCode);
                        excludedMachineCodes.add(pairSingleControlMachine.getMachineCode());
                        candidateCache.removeMachine(machineCode);
                        candidateCache.removeMachine(pairSingleControlMachine.getMachineCode());
                        recordExcludedMachineReason(excludedMachineReasonMap, machineCode,
                                pairMouldResourceAllocationResult.getSkipReason().getDescription(),
                                null, null, null, null, null, null, null, null, null);
                        failReason = selectHigherPriorityFailReason(
                                failReason, NewSpecFailReasonEnum.MACHINE_SELECTION_FAILED);
                        log.info("双模SKU单控整机副侧模具资源不足，整机候选回滚, materialCode: {}, leftMachine: {}, rightMachine: {}, reason: {}",
                                sku.getMaterialCode(), machineCode, pairSingleControlMachine.getMachineCode(),
                                pairMouldResourceAllocationResult.getSkipReason().getDescription());
                        continue;
                    }
                }

                // 3. 计算机台可开工时间（考虑机台当前预计完工和能力策略约束）
                Date endingTime = Objects.nonNull(selectedAvailabilityPlan)
                        ? selectedAvailabilityPlan.getOccupationEndTime()
                        : resolveMachineOccupationEndTime(context, sku, candidateMachine, shifts);
                if (isEnding) {
                    boolean maintenanceWindowAttached = getMaintenanceScheduleService()
                            .tryAttachMaintenanceAfterFirstEnding(
                                    context, candidateMachine, endingTime);
                    if (maintenanceWindowAttached) {
                        /*
                         * 首次收尾补挂的精度窗口会改变该机台及其他候选的真实可开产时间。
                         * 当前尚未提交换模/首检，只回滚已预占模具并重新进入统一选机，确保
                         * 新精度窗口参与所有候选比较，而不是锁定原机台后局部顺延。
                         */
                        rollbackMouldResourceAllocation(
                                context, sku, mouldResourceAllocationResult,
                                pairMouldResourceAllocationResult);
                        candidateCache.clearCapacityCache();
                        candidateAvailabilityPlanMap.clear();
                        log.info("新增SKU补挂精度窗口后重新执行逐班选机, batchNo: {}, "
                                        + "scheduleDate: {}, materialCode: {}, machineCode: {}, "
                                        + "occupationEndTime: {}",
                                context.getBatchNo(), dayContext.getScheduleDate(),
                                sku.getMaterialCode(), machineCode,
                                LhScheduleTimeUtil.formatDateTime(endingTime));
                        continue;
                    }
                }
                /*
                 * 精度窗口属于硬时间轴，试制SKU也不得清除或覆盖。试制早班换模与精度冲突时，
                 * 后续统一顺延到胶囊预热完成后重新按早班规则寻找合法窗口。
                 */
                // 保养窗口挂载会改变候选机台运行态，提前清理窗口产能缓存，避免后续复用旧产能。
                candidateCache.clearCapacityCache();
                Date machineReadyTime = Objects.nonNull(selectedAvailabilityPlan)
                        ? selectedAvailabilityPlan.getMachineReadyTime()
                        : capacityCalculate.calculateStartTime(context, machineCode, endingTime);
                List<LhShiftConfigVO> schedulingShifts = releasedContinuationReuse
                        ? this.resolveReleasedContinuationReuseShifts(dayContext)
                        : shifts;
                int switchDurationHours;
                if (takeoverWithoutMouldChange) {
                    switchDurationHours = 0;
                } else if (isTypeBlockRelation) {
                    // 同胎胚同模具切换按换活字块耗时计算，保证切换窗口与落库类型一致。
                    switchDurationHours = LhScheduleTimeUtil.getTypeBlockChangeTotalHours(context);
                } else {
                    switchDurationHours = LhScheduleTimeUtil.getMouldChangeTotalHours(context);
                }
                // 本次规则禁止换模与精度计划并行，统一使用已经避开精度及预热窗口的机台就绪时间。
                boolean maintenanceOverlapSwitch = false;
                Date switchReadyTime = machineReadyTime;
                /*
                 * 当前计划日先按原规则完成选机，再读取前一业务日冻结的结构切换准入结论。
                 * 这里只放宽已选机台的换模时间轴，不提前锁机，也不改写候选顺序。
                 */
                EarlyProductionDecision structureSwitchLookbackDecision =
                        this.resolveStructureSwitchLookbackDecision(
                                context, dayContext, sku, earliestEmbryoAvailableTime);
                boolean structureSwitchLookbackAllowed =
                        Objects.nonNull(structureSwitchLookbackDecision);
                if (releasedContinuationReuse) {
                    // 原机台原模具重启没有切换动作，但仍从增机生效业务日首班开始续作。
                    switchReadyTime = this.resolveLaterTime(
                            machineReadyTime, releasedContinuationReuseStartTime);
                } else {
                    switchReadyTime = resolveSpecifyReservedReadyTime(
                            context, sku, machineCode, switchReadyTime);
                    // 试制SKU换模需在早班完成，不受开产模式限制；非试制SKU仍受开产模式约束
                    switchReadyTime = ShiftProductionControlUtil.resolveEarliestSwitchStartTime(
                            context, switchReadyTime, sku);
                }
                Date productionPreparationNotBeforeTime =
                        this.resolveProductionPreparationNotBeforeTime(
                                context, dayContext, shifts, candidateProductionNotBeforeTime,
                                currentAddMachineProductionDate);
                /*
                 * 生产日前准备回看是通用能力，不再只服务结构提前生产。选中机台如果在当前
                 * 生产日之前已经空闲，且按本次真实切换耗时能在当前业务日日终前完成准备，就允许
                 * 在完整八班窗口中寻找换模时点；正式开产仍按换模/首检完成时间与胎胚可供时间取较晚值。
                 */
                boolean productionPreparationLookbackAllowed = structureSwitchLookbackAllowed
                        || this.isProductionPreparationLookbackAllowed(
                                context, dayContext, switchReadyTime,
                                switchDurationHours, productionPreparationNotBeforeTime);
                List<LhShiftConfigVO> switchAlignmentShifts = releasedContinuationReuse
                        ? schedulingShifts
                        : productionPreparationLookbackAllowed
                        ? context.getScheduleWindowShifts() : shifts;
                switchReadyTime = alignNewSpecSwitchReadyTimeToWindowStart(
                        context, switchAlignmentShifts, switchReadyTime);
                // 历史映射班次只作为指定机台首次尝试起点，不继承具体时刻，也不限制本批实际合法班次。
                switchReadyTime = alignHistoricalReverseSwitchReadyTime(
                        context, historicalDirective, switchReadyTime);
                /*
                 * 特殊材料不得早于首个有月计划日计划量的日期换模。置换服务把无副作用预演得到的
                 * 最终允许换模时点写入临时指令，此处在现有停机、开停产和窗口起点校验之后做下限对齐。
                 */
                switchReadyTime = alignSpecialMaterialSubstitutionSwitchReadyTime(
                        context, sku, machineCode, switchReadyTime);
                /*
                 * 生产日前回看场景下，候选预演和正式落班共用不含胎胚门禁的准备时间轴。
                 * 胎胚门禁只在正式生产起点应用，不能反向把候选机台的换模时间推迟到胎胚到位日。
                 */
                if (!releasedContinuationReuse
                        && productionPreparationLookbackAllowed
                        && Objects.nonNull(productionPreparationNotBeforeTime)) {
                    switchReadyTime = delaySwitchReadyTimeCloseToProductionStart(
                            context, sku.getMaterialCode(), machineCode,
                            switchReadyTime, switchDurationHours,
                            productionPreparationNotBeforeTime);
                }

                // 4. 分配换模窗口；晚班不可换模、换模上限和维保重叠都在分配器中统一收口。
                // 基础换模时间永远执行，换模均衡仅在开关开启时介入。
                Date mouldChangeStartTime = null;
                Date mouldChangeCompleteTime = null;
                LhShiftConfigVO firstInspectionAttributionShift = null;
                FirstInspectionAllocationPlan firstInspectionAllocationPlan = null;
                Date firstInspectionAttributionTime = null;
                Date previewInspectionTime = null;
                Date inspectionTime = null;
                Date productionStartTime = null;
                Date theoreticalProductionStartTime = null;
                Date firstProductionStartTime = null;
                Date committedChangeoverPreviewStartTime = null;
                EarlyProductionDecision earlyProductionDecision = null;
                boolean structureSwitchLookbackApplied = false;
                boolean productionPreparationLookbackApplied = false;
                boolean productionStartTimeConstrained = embryoAvailableTimeConstrained;
                boolean firstInspectionRetryRequired = false;
                NewSpecFailReasonEnum switchAllocateFailReason = null;
                // 续作增机补偿的首台与后续机台统一按 dayN 首次增机日对齐换模。
                if (releasedContinuationReuse) {
                    log.info("续作加机台当轮选回原释放机台，按同物料同模具续作重启时间轴处理, "
                                    + "batchNo: {}, scheduleDate: {}, materialCode: {}, productStatus: {}, "
                                    + "machineCode: {}, firstAddMachineProductionDate: {}, "
                                    + "machineReadyTime: {}, reuseStartTime: {}, "
                                    + "effect: 不提前锁机，命中后不换模、不首检、不占用换模配额、不得早于增机生效日",
                            context.getBatchNo(), dayContext.getScheduleDate(),
                            sku.getMaterialCode(), sku.getProductStatus(), machineCode,
                            sku.getFirstAddMachineProductionDate(),
                            LhScheduleTimeUtil.formatDateTime(machineReadyTime),
                            LhScheduleTimeUtil.formatDateTime(switchReadyTime));
                } else if (productionPreparationLookbackAllowed) {
                    log.info("新增SKU保留已选机台真实空闲时间用于生产日前跨日准备, "
                                    + "batchNo: {}, scheduleDate: {}, materialCode: {}, machineCode: {}, "
                                    + "machineReadyTime: {}, switchReadyTime: {}, switchDurationHours: {}, "
                                    + "productionNotBeforeTime: {}, structureLookback: {}, futurePlanDate: {}",
                            context.getBatchNo(), dayContext.getScheduleDate(),
                            sku.getMaterialCode(), machineCode,
                            LhScheduleTimeUtil.formatDateTime(machineReadyTime),
                            LhScheduleTimeUtil.formatDateTime(switchReadyTime),
                            switchDurationHours,
                            LhScheduleTimeUtil.formatDateTime(productionNotBeforeTime),
                            structureSwitchLookbackAllowed,
                            Objects.isNull(structureSwitchLookbackDecision)
                                    ? null : structureSwitchLookbackDecision.getFuturePlanDate());
                } else {
                    switchReadyTime = alignSwitchReadyTimeByAddMachineDate(
                            context, sku, switchReadyTime, shifts, totalScheduledQty,
                            currentAddMachineProductionDate, isEnding,
                            dayContext.getCurrentPhase());
                }
                Date firstInspectionRetryReadyTime = firstInspectionRetryReadyTimeMap.get(machineCode);
                if (Objects.nonNull(firstInspectionRetryReadyTime)
                        && (Objects.isNull(switchReadyTime)
                        || firstInspectionRetryReadyTime.after(switchReadyTime))) {
                    // 前一次首检无法合法落位时，沿用同一候选机台并从下一合法切换时间重新分配。
                    switchReadyTime = firstInspectionRetryReadyTime;
                }
                if (!releasedContinuationReuse
                        && !productionPreparationLookbackAllowed
                        && Objects.nonNull(selectedAvailabilityPlan)
                        && Objects.nonNull(selectedAvailabilityPlan.getChangeoverStartTime())
                        && (Objects.isNull(switchReadyTime)
                        || selectedAvailabilityPlan.getChangeoverStartTime().after(switchReadyTime))) {
                    /*
                     * 选机预演可能因首检资源不足将整段准备顺延到下一班。正式提交直接从预演
                     * 确定的切换起点申请资源，避免先重复申请旧班次、回滚后再二次计算。
                     */
                    switchReadyTime = selectedAvailabilityPlan.getChangeoverStartTime();
                }
                if (takeoverWithoutMouldChange) {
                    /*
                     * 无换模接管或原续作机台原模具重启都不调用换模均衡器，
                     * 不占用每日换模次数、早中班均衡或首检资源；后续仍执行机台产能、
                     * 停机、清洗及班次可排时间计算。
                     */
                    mouldChangeStartTime = releasedContinuationReuse
                            ? null : switchReadyTime;
                    mouldChangeCompleteTime = releasedContinuationReuse
                            ? null : switchReadyTime;
                    productionStartTime = switchReadyTime;
                    firstProductionStartTime = switchReadyTime;
                } else {
                    // B 迁移及普通新增继续调用原换模分配器，晚班禁换模、20:00 后顺延和换模上限保持不变。
                    if (productionPreparationLookbackAllowed) {
                        // 正式占用配额前先预演提交动作，避免在每日最后一个名额上因先提交后预演产生假不一致。
                        committedChangeoverPreviewStartTime =
                                this.previewCommittedNewSpecMouldChangeStartTime(
                                        context, sku, machineCode, switchReadyTime,
                                        switchDurationHours, mouldChangeBalance,
                                        dayContext.getCurrentPhase(), isTypeBlockRelation,
                                        dayContext.getDayEndTime(), true);
                    }
                    mouldChangeStartTime = allocateNewSpecMouldChangeStartTime(
                            context, sku, machineCode, switchReadyTime, switchDurationHours,
                            mouldChangeBalance, dayContext.getCurrentPhase(), isTypeBlockRelation,
                            dayContext.getDayEndTime(), productionPreparationLookbackAllowed);
                    boolean historicalMouldChangeInMappedShift =
                            isHistoricalReverseMouldChangeInMappedShift(
                                    context, historicalDirective, mouldChangeStartTime);
                    if (Objects.nonNull(mouldChangeStartTime)
                            && Objects.nonNull(historicalDirective)
                            && !historicalMouldChangeInMappedShift) {
                        /*
                         * 历史反选继承的是“机台+后物料”关系，历史映射班次只作为本批首次尝试起点。
                         * 停机、保养、换模配额或首检约束导致实际班次后移时，继续在同一指定机台执行主链。
                         */
                        log.info("前日交替计划指定机台按本批资源重算班次, materialCode: {}, machineCode: {}, "
                                        + "historicalShift: {}, mappedShift: {}, actualShift: {}, switchReadyTime: {}",
                                sku.getMaterialCode(), machineCode,
                                historicalDirective.getHistoricalShiftIndex(),
                                historicalDirective.getMappedShiftIndex(),
                                LhScheduleTimeUtil.getShiftIndex(
                                        context, context.getScheduleDate(), mouldChangeStartTime),
                                LhScheduleTimeUtil.formatDateTime(switchReadyTime));
                    }
                    if (mouldChangeStartTime == null) {
                        log.debug("新增SKU换模窗口分配失败, materialCode: {}, 机台: {}, 机台就绪: {}, 目标量: {}",
                                sku.getMaterialCode(), machineCode,
                                LhScheduleTimeUtil.formatDateTime(switchReadyTime), sku.resolveTargetScheduleQty());
                        switchAllocateFailReason = NewSpecFailReasonEnum.MOULD_CHANGE_SHIFT_ALLOCATE_FAILED;
                    }
                    if (mouldChangeStartTime != null) {
                    mouldChangeCompleteTime = LhScheduleTimeUtil.addHours(mouldChangeStartTime, switchDurationHours);
                    structureSwitchLookbackApplied = structureSwitchLookbackAllowed
                            && this.isStructureSwitchLookbackTimeline(
                                    context, dayContext, earliestEmbryoAvailableTime,
                                    mouldChangeStartTime, mouldChangeCompleteTime);
                    productionPreparationLookbackApplied = structureSwitchLookbackApplied
                            || this.isProductionPreparationLookbackTimeline(
                                    context, dayContext,
                                    productionPreparationNotBeforeTime,
                                    mouldChangeStartTime, mouldChangeCompleteTime);
                    /*
                     * 换模均衡器可以顺延到下一业务日。按天编排下，当前阶段只能提交 dayShifts 内的资源；
                     * 普通场景若换模开始或完成越过日窗口，必须回滚本次换模次数和模具预占。
                     * 生产日前回看场景只放宽换模起点可落在当前业务日前；完成时间必须仍
                     * 在当前业务日日终之前，其余日窗口约束保持不变。
                     */
                    if ((!dayContext.contains(mouldChangeStartTime)
                            || dayContext.reachesOrPassesDayEnd(mouldChangeCompleteTime))
                            && !productionPreparationLookbackApplied) {
                        dailyDeferredReason = "换模完成时间超出当前业务日日窗口";
                        rollbackMouldChangeAllocation(
                                context, sku, mouldChangeBalance, mouldChangeStartTime);
                        rollbackMouldResourceAllocation(
                                context, sku, mouldResourceAllocationResult,
                                pairMouldResourceAllocationResult);
                        // 首台当日开产保护：首台换模被均衡/共用胎胚错峰排进“换模后当日零产”的班次
                        // （如 14:00-22:00 中班换模完成正好日终）时，回滚后直接按当天早班重排换模，
                        // 允许首台占用早班（早班次数+1，仍受每日总上限约束），保证首个生产日必须开产。
                        Date sameDayMorningSwitchTime = dayContext.getCurrentPhase()
                                != DailySchedulePhase.EARLY_PRODUCTION
                                ? resolveFirstAllowMouldChangeShiftStartTime(
                                        dayContext.getDayShifts(), dayContext.getScheduleDate())
                                : null;
                        if (isFirstMachineSameDayProductionProtection(
                                context, dayContext, totalScheduledQty, switchDurationHours,
                                switchReadyTime, mouldChangeStartTime, sameDayMorningSwitchTime)) {
                            mouldChangeStartTime = sameDayMorningSwitchTime;
                            mouldChangeCompleteTime = LhScheduleTimeUtil.addHours(
                                    mouldChangeStartTime, switchDurationHours);
                            // 守卫回滚已释放模具；重排早班换模后必须重新预占同一套模具，
                            // 否则落库时同一模具会被本机台与后续候选重复登记导致 S4601 冲突。
                            MouldResourceAllocationResult reallocatedMouldResult =
                                    tryAllocateMouldResourceForAddMachine(
                                            context, sku, candidateMachine, originalAddMachineCount,
                                            actualAllowedAddMachineCount);
                            if (!reallocatedMouldResult.isAllowed()) {
                                excludedMachineCodes.add(machineCode);
                                candidateCache.removeMachine(machineCode);
                                recordExcludedMachineReason(excludedMachineReasonMap, machineCode,
                                        dailyDeferredReason,
                                        machineReadyTime, switchReadyTime,
                                        mouldChangeStartTime, mouldChangeCompleteTime,
                                        null, null, null, null, null);
                                failReason = selectHigherPriorityFailReason(
                                        failReason, NewSpecFailReasonEnum.NO_CAPACITY_IN_SCHEDULE_WINDOW);
                                log.info("新增SKU日窗口守卫回滚当前候选(首台保护模具重占失败), batchNo: {}, "
                                                + "scheduleDate: {}, materialCode: {}, machineCode: {}, "
                                                + "mouldChangeStartTime: {}, mouldChangeCompleteTime: {}",
                                        context.getBatchNo(), dayContext.getScheduleDate(),
                                        sku.getMaterialCode(), machineCode,
                                        LhScheduleTimeUtil.formatDateTime(mouldChangeStartTime),
                                        LhScheduleTimeUtil.formatDateTime(mouldChangeCompleteTime));
                                continue;
                            }
                            mouldResourceAllocationResult = reallocatedMouldResult;
                            if (wholeSingleControlUnit && Objects.nonNull(pairSingleControlMachine)) {
                                // 单控整机主侧重占成功后，副侧也必须重新预占同一套配对模具。
                                MouldResourceAllocationResult reallocatedPairMouldResult =
                                        tryAllocateMouldResourceForAddMachine(
                                                context, sku, pairSingleControlMachine, originalAddMachineCount,
                                                actualAllowedAddMachineCount);
                                if (!reallocatedPairMouldResult.isAllowed()) {
                                    rollbackMouldResourceAllocation(
                                            context, sku, mouldResourceAllocationResult);
                                    excludedMachineCodes.add(machineCode);
                                    excludedMachineCodes.add(pairSingleControlMachine.getMachineCode());
                                    candidateCache.removeMachine(machineCode);
                                    candidateCache.removeMachine(pairSingleControlMachine.getMachineCode());
                                    recordExcludedMachineReason(excludedMachineReasonMap, machineCode,
                                            dailyDeferredReason,
                                            machineReadyTime, switchReadyTime,
                                            mouldChangeStartTime, mouldChangeCompleteTime,
                                            null, null, null, null, null);
                                    failReason = selectHigherPriorityFailReason(
                                            failReason, NewSpecFailReasonEnum.NO_CAPACITY_IN_SCHEDULE_WINDOW);
                                    log.info("新增SKU日窗口守卫回滚当前候选(首台保护副侧模具重占失败), batchNo: {}, "
                                                    + "scheduleDate: {}, materialCode: {}, machineCode: {}, "
                                                    + "pairMachineCode: {}",
                                            context.getBatchNo(), dayContext.getScheduleDate(),
                                            sku.getMaterialCode(), machineCode,
                                            pairSingleControlMachine.getMachineCode());
                                    continue;
                                }
                                pairMouldResourceAllocationResult = reallocatedPairMouldResult;
                            }
                            registerFirstMachineMorningMouldChangeCount(context, mouldChangeStartTime);
                            log.info("新增SKU首台当日开产保护生效, batchNo: {}, scheduleDate: {}, "
                                            + "materialCode: {}, machineCode: {}, mouldChangeStartTime: {}, "
                                            + "mouldChangeCompleteTime: {}, dayEndTime: {}",
                                    context.getBatchNo(), dayContext.getScheduleDate(),
                                    sku.getMaterialCode(), machineCode,
                                    LhScheduleTimeUtil.formatDateTime(mouldChangeStartTime),
                                    LhScheduleTimeUtil.formatDateTime(mouldChangeCompleteTime),
                                    LhScheduleTimeUtil.formatDateTime(dayContext.getDayEndTime()));
                        } else {
                            excludedMachineCodes.add(machineCode);
                            candidateCache.removeMachine(machineCode);
                            recordExcludedMachineReason(excludedMachineReasonMap, machineCode,
                                    dailyDeferredReason,
                                    machineReadyTime, switchReadyTime,
                                    mouldChangeStartTime, mouldChangeCompleteTime,
                                    null, null, null, null, null);
                            failReason = selectHigherPriorityFailReason(
                                    failReason, NewSpecFailReasonEnum.NO_CAPACITY_IN_SCHEDULE_WINDOW);
                            log.info("新增SKU日窗口守卫回滚当前候选, batchNo: {}, scheduleDate: {}, "
                                            + "materialCode: {}, machineCode: {}, mouldChangeStartTime: {}, "
                                            + "mouldChangeCompleteTime: {}, dayEndTime: {}",
                                    context.getBatchNo(), dayContext.getScheduleDate(),
                                    sku.getMaterialCode(), machineCode,
                                    LhScheduleTimeUtil.formatDateTime(mouldChangeStartTime),
                                    LhScheduleTimeUtil.formatDateTime(mouldChangeCompleteTime),
                                    LhScheduleTimeUtil.formatDateTime(dayContext.getDayEndTime()));
                            continue;
                        }
                    }
                    Date expectedChangeoverStartTime = Objects.isNull(selectedAvailabilityPlan)
                            ? null : selectedAvailabilityPlan.getChangeoverStartTime();
                    if (productionPreparationLookbackAllowed
                            && Objects.nonNull(selectedAvailabilityPlan)
                            && selectedAvailabilityPlan.isPreparationAvailable()) {
                        /*
                         * 跨日准备只在普通候选选定后预演一次正式提交动作，禁止把放宽后的
                         * T日中班窗口提前用于全部候选分组并改变原选机顺序。这里与正式分配
                         * 使用同一动作和实时计数，继续保留预演/提交时间轴一致性硬校验。
                         */
                        expectedChangeoverStartTime = committedChangeoverPreviewStartTime;
                    }
                    Date expectedChangeoverEndTime = Objects.isNull(expectedChangeoverStartTime)
                            ? null : LhScheduleTimeUtil.addHours(
                            expectedChangeoverStartTime, switchDurationHours);
                    boolean selectedAvailabilityTimelineMatched =
                            Objects.isNull(selectedAvailabilityPlan)
                                    || (selectedAvailabilityPlan.isAvailable()
                                    && Objects.equals(
                                    expectedChangeoverStartTime,
                                    mouldChangeStartTime)
                                    && Objects.equals(
                                    expectedChangeoverEndTime,
                                    mouldChangeCompleteTime));
                    if (!selectedAvailabilityTimelineMatched) {
                        /*
                         * 普通候选必须严格提交选机阶段已经比较过的同一时间轴。若资源提交返回
                         * 了不同切换时点，继续排产会造成“按早班选机、按中班落库”等前后不一致；
                         * 因此当前机台按一致性硬约束退出本回合，其他候选继续正常竞争。
                         */
                        rollbackMouldChangeAllocation(
                                context, sku, mouldChangeBalance, mouldChangeStartTime);
                        rollbackMouldResourceAllocation(
                                context, sku, mouldResourceAllocationResult,
                                pairMouldResourceAllocationResult);
                        excludedMachineCodes.add(machineCode);
                        candidateCache.removeMachine(machineCode);
                        String inconsistencyReason = "选机预演与正式切换时间不一致";
                        recordExcludedMachineReason(
                                excludedMachineReasonMap, machineCode, inconsistencyReason,
                                endingTime, machineReadyTime,
                                selectedAvailabilityPlan.getChangeoverStartTime(),
                                selectedAvailabilityPlan.getChangeoverEndTime(),
                                null, selectedAvailabilityPlan.getCandidateAvailableProductionTime(),
                                null, null, null);
                        log.warn("新增SKU候选时间轴一致性校验失败, batchNo: {}, scheduleDate: {}, "
                                        + "materialCode: {}, machineCode: {}, previewChangeover: [{}, {}), "
                                        + "actualChangeover: [{}, {})",
                                context.getBatchNo(), dayContext.getScheduleDate(),
                                sku.getMaterialCode(), machineCode,
                                LhScheduleTimeUtil.formatDateTime(
                                        expectedChangeoverStartTime),
                                LhScheduleTimeUtil.formatDateTime(
                                        expectedChangeoverEndTime),
                                LhScheduleTimeUtil.formatDateTime(mouldChangeStartTime),
                                LhScheduleTimeUtil.formatDateTime(mouldChangeCompleteTime));
                        continue;
                    }
                    // 精度窗口与换模禁止重叠；分配器已将冲突换模顺延，首检以真实换模完成点为区间终点向前倒推。
                    maintenanceOverlapSwitch = false;
                    Date maintenanceReadyTime = mouldChangeCompleteTime;
                    boolean plannedRepairAffectingSwitch = ShiftCapacityResolverUtil.isPlannedRepairAffectingSwitch(
                            context, context.getDevicePlanShutList(), machineCode, endingTime,
                            mouldChangeStartTime, mouldChangeCompleteTime);
                    Date plannedRepairReadyTime = ShiftCapacityResolverUtil.resolvePlannedRepairProductionReadyTime(
                            context, context.getDevicePlanShutList(), machineCode, endingTime,
                            mouldChangeStartTime, mouldChangeCompleteTime);
                    /*
                     * 精度计划、正规换模和计划性维修继续按既有并行规则取最晚恢复时间，
                     * 该时刻只约束正式生产；首检数量及时间区间仍由真实切换完成点向前倒推。
                     * 正规8小时换模已包含首检，不再额外增加1小时。
                     */
                    Date firstInspectionBaseTime = maintenanceReadyTime;
                    if (plannedRepairAffectingSwitch && Objects.nonNull(plannedRepairReadyTime)
                            && plannedRepairReadyTime.after(firstInspectionBaseTime)) {
                        firstInspectionBaseTime = plannedRepairReadyTime;
                    }
                    /*
                     * 首检时间区间只能由真实切换结束时间倒推，不能再把维修恢复、胎胚门禁或
                     * 正式生产起点当作首检结束时间。换模与换活字块共用同一无副作用计划；
                     * 计数仍归切换结束班次，数量可按真实重叠时间跨越前序班次。
                     */
                    boolean selectedInspectionTimelineMatched =
                            Objects.nonNull(selectedAvailabilityPlan)
                                    && Objects.equals(
                                    selectedAvailabilityPlan.getChangeoverStartTime(),
                                    mouldChangeStartTime)
                                    && Objects.equals(
                                    selectedAvailabilityPlan.getChangeoverEndTime(),
                                    mouldChangeCompleteTime);
                    firstInspectionAllocationPlan = selectedInspectionTimelineMatched
                            ? selectedAvailabilityPlan.getFirstInspectionPlan()
                            : FirstInspectionAllocationUtil.buildPlan(
                            context, sku, context.getScheduleWindowShifts(),
                            mouldChangeCompleteTime,
                            ShiftCapacityResolverUtil.resolveRuntimeShiftCapacity(
                                    context, candidateMachine, sku.getShiftCapacity()),
                            dynamicTargetQty, inspectionScheduleTypeCode,
                            machineCode, null);
                    if (!firstInspectionAllocationPlan.isValid()) {
                        LhShiftConfigVO failedCountingShift =
                                FirstInspectionQtyUtil.resolveAttributionShift(
                                        context.getScheduleWindowShifts(), mouldChangeCompleteTime);
                        Date nextSwitchReadyTime = this.resolveNextFirstInspectionRetryReadyTime(
                                failedCountingShift, mouldChangeStartTime);
                        firstInspectionRetryReadyTimeMap.put(machineCode, nextSwitchReadyTime);
                        firstInspectionRetryRequired = true;
                        log.info("新增SKU首检时间分摊预演失败，整段准备时间轴顺延后重新竞争, "
                                        + "batchNo: {}, materialCode: {}, machineCode: {}, "
                                        + "switchCompleteTime: {}, reason: {}, nextSwitchReadyTime: {}",
                                context.getBatchNo(), sku.getMaterialCode(), machineCode,
                                LhScheduleTimeUtil.formatDateTime(mouldChangeCompleteTime),
                                firstInspectionAllocationPlan.getInvalidReason(),
                                LhScheduleTimeUtil.formatDateTime(nextSwitchReadyTime));
                    }
                    /*
                     * 胎胚时间或 X/T 中班门禁晚于准备完成时间时，先完整计算现有规则理论开产，
                     * 再施加统一生产下限，最后重新经过停机、班次管控和首检容量校正。
                     * 换模开始、完成时间保持不变；量试仍使用普通首检条数，只有试制继续扣固定2小时。
                     */
                    productionStartTimeConstrained = embryoAvailableTimeConstrained
                            || (Objects.nonNull(productionNotBeforeTime)
                            && productionNotBeforeTime.after(firstInspectionBaseTime));
                    if (productionStartTimeConstrained) {
                        int constrainedMachineMouldQty =
                                ShiftCapacityResolverUtil.resolveMachineMouldQty(candidateMachine);
                        int constrainedRuntimeShiftCapacity =
                                ShiftCapacityResolverUtil.resolveRuntimeShiftCapacity(
                                        context, candidateMachine, sku.getShiftCapacity());
                        Date defaultProductionStartTime = firstInspectionBaseTime;
                        Date trialAdjustedProductionStartTime =
                                FirstInspectionQtyUtil.resolveTrialProductionStartTime(
                                        context, sku, shifts, firstInspectionBaseTime,
                                        defaultProductionStartTime, ScheduleTypeEnum.NEW_SPEC.getCode());
                        theoreticalProductionStartTime =
                                ShiftProductionControlUtil.resolveFirstSchedulableStartIgnoringCleaning(
                                        context, machineCode, trialAdjustedProductionStartTime, shifts,
                                        constrainedRuntimeShiftCapacity, sku.getLhTimeSeconds(),
                                        constrainedMachineMouldQty);
                        earlyProductionDecision = structureSwitchLookbackApplied
                                ? structureSwitchLookbackDecision
                                : resolveEarlyProductionDecision(
                                        context, sku, theoreticalProductionStartTime, shifts,
                                        isEnding, dayContext.getCurrentPhase());
                        // 调用处显式保留既有增机日对齐，再应用统一生产门禁，避免改变 dayN 扩机节奏。
                        theoreticalProductionStartTime = alignProductionStartTimeByAddMachineDate(
                                context, sku, theoreticalProductionStartTime, shifts, totalScheduledQty,
                                currentAddMachineProductionDate, isEnding, earlyProductionDecision);
                        Date constrainedStartTime =
                                NewSpecEmbryoAvailableTimeResolver.resolveActualProductionStartTime(
                                        theoreticalProductionStartTime, productionNotBeforeTime);
                        firstProductionStartTime =
                                ShiftProductionControlUtil.resolveFirstSchedulableStartIgnoringCleaning(
                                        context, machineCode, constrainedStartTime, shifts,
                                        constrainedRuntimeShiftCapacity, sku.getLhTimeSeconds(),
                                        constrainedMachineMouldQty);
                        /*
                         * 首班部分产能不足完整普通首检，或试制扣除固定2小时后无正产量时，
                         * 只顺延首检和生产起点，不回写或推迟已经完成的换模准备动作。
                         */
                        firstProductionStartTime = this.resolveProductionGateConstrainedStartTime(
                                context, candidateMachine, sku, firstProductionStartTime,
                                mouldChangeStartTime, shifts, constrainedMachineMouldQty,
                                constrainedRuntimeShiftCapacity, dynamicTargetQty, isEnding,
                                inspectionScheduleTypeCode);
                        productionStartTime = firstProductionStartTime;
                        firstInspectionAttributionShift =
                                NewSpecEmbryoAvailableTimeResolver.resolveProductionShift(
                                        shifts, firstProductionStartTime);
                        firstInspectionAttributionTime = firstProductionStartTime;
                        /*
                         * 胎胚可供时间把真实开产顺延到换模完成之后时，首检必须落到真实开产班次，
                         * 不能继续沿用按换模完成时间倒推的跨班首检计划，否则首检会被写到开产前的班次。
                         * 这里清空跨班计划，让下游回退到“真实开产班次单班首检”口径。
                         */
                        if (Objects.nonNull(firstProductionStartTime)
                                && Objects.nonNull(mouldChangeCompleteTime)
                                && firstProductionStartTime.after(mouldChangeCompleteTime)) {
                            firstInspectionAllocationPlan = null;
                        }
                    } else {
                        // 生产门禁没有推迟现有时间轴时，继续使用原首检归属和开产语义。
                        firstInspectionAttributionShift =
                                FirstInspectionQtyUtil.resolveFirstInspectionAttributionShift(
                                        context, sku, shifts, firstInspectionBaseTime,
                                        inspectionScheduleTypeCode);
                        firstInspectionAttributionTime =
                                FirstInspectionQtyUtil.resolveFirstInspectionAttributionTime(
                                        context, sku, shifts, firstInspectionBaseTime,
                                        inspectionScheduleTypeCode);
                    }
                    if (Objects.nonNull(firstInspectionAllocationPlan)
                            && firstInspectionAllocationPlan.isValid()
                            && firstInspectionAllocationPlan.getInspectionQty() > 0) {
                        /*
                         * 普通SKU的首检计数和资源落点固定使用切换结束班次；正式生产即使因
                         * 胎胚、维修或清洗后移，也不得把首检条数一起搬到生产开始班次。
                         */
                        firstInspectionAttributionShift =
                                firstInspectionAllocationPlan.getCountingShift();
                        firstInspectionAttributionTime =
                                firstInspectionAllocationPlan.getInspectionEndTime();
                    }
                    if (Objects.nonNull(firstInspectionAllocationPlan)
                            && !firstInspectionAllocationPlan.isValid()) {
                        firstInspectionAttributionTime = null;
                    }
                    firstInspectionAttributionTime = alignInspectionBalanceTimeToAttributionShift(
                            context, sku, shifts, firstInspectionAttributionShift,
                            firstInspectionAttributionTime);
                    if (firstInspectionAttributionTime == null) {
                        log.debug("新增SKU首检归属班次为空, materialCode: {}, 机台: {}, 换模开始: {}, 换模完成: {}",
                                sku.getMaterialCode(), machineCode,
                                LhScheduleTimeUtil.formatDateTime(mouldChangeStartTime),
                                LhScheduleTimeUtil.formatDateTime(mouldChangeCompleteTime));
                        rollbackMouldChangeAllocation(context, sku, mouldChangeBalance, mouldChangeStartTime);
                        mouldChangeStartTime = null;
                        switchAllocateFailReason = NewSpecFailReasonEnum.FIRST_INSPECTION_SHIFT_ALLOCATE_FAILED;
                    } else {
                        /*
                         * 先无副作用预演首检均衡落点。若资源均衡要求后移到其他班次，则当前换模完成时间
                         * 已无法与首检数量归属保持一致，必须回滚本次换模并从下一合法切换时间重试。
                         */
                        previewInspectionTime = inspectionBalance.previewInspection(
                                context, machineCode, firstInspectionAttributionTime);
                        LhShiftConfigVO previewInspectionShift = FirstInspectionQtyUtil
                                .resolveFirstInspectionAttributionShift(
                                        context, sku, context.getScheduleWindowShifts(), previewInspectionTime,
                                        inspectionScheduleTypeCode);
                        if (Objects.isNull(previewInspectionTime) || Objects.isNull(previewInspectionShift)) {
                            log.debug("新增SKU首检预演失败, materialCode: {}, 机台: {}, 换模开始: {}, 换模完成: {}",
                                    sku.getMaterialCode(), machineCode,
                                    LhScheduleTimeUtil.formatDateTime(mouldChangeStartTime),
                                    LhScheduleTimeUtil.formatDateTime(mouldChangeCompleteTime));
                            rollbackMouldChangeAllocation(context, sku, mouldChangeBalance, mouldChangeStartTime);
                            mouldChangeStartTime = null;
                            switchAllocateFailReason = NewSpecFailReasonEnum.FIRST_INSPECTION_SHIFT_ALLOCATE_FAILED;
                        } else if (!Objects.equals(firstInspectionAttributionShift.getShiftIndex(),
                                previewInspectionShift.getShiftIndex())) {
                            firstInspectionRetryRequired = true;
                            Date nextSwitchReadyTime = resolveNextFirstInspectionRetryReadyTime(
                                    firstInspectionAttributionShift, mouldChangeStartTime);
                            firstInspectionRetryReadyTimeMap.put(machineCode, nextSwitchReadyTime);
                            log.info("新增SKU首检资源班次与数量归属班次不一致，回滚候选并顺延切换, "
                                            + "batchNo: {}, materialCode: {}, machineCode: {}, switchCompleteTime: {}, "
                                            + "quantityShift: class{}, resourceShift: class{}, nextSwitchReadyTime: {}",
                                    context.getBatchNo(), sku.getMaterialCode(), machineCode,
                                    LhScheduleTimeUtil.formatDateTime(mouldChangeCompleteTime),
                                    firstInspectionAttributionShift.getShiftIndex(),
                                    previewInspectionShift.getShiftIndex(),
                                    LhScheduleTimeUtil.formatDateTime(nextSwitchReadyTime));
                            rollbackMouldChangeAllocation(context, sku, mouldChangeBalance, mouldChangeStartTime);
                            mouldChangeStartTime = null;
                        } else {
                            /*
                             * 普通 SKU 的正常换模时长已包含首检，首检均衡只占用首检资源，不再推迟生产；
                             * 精度计划重叠时取“换模完成、保养及预热完成”的最大时间；
                             * 试制 SKU 仍按现行规则在早班换模后由中班产能上限控制。
                             */
                            if (!productionStartTimeConstrained) {
                                Date defaultProductionStartTime = firstInspectionBaseTime;
                                // 门禁未推迟时间轴时，试制SKU继续沿用早班换模、同业务日中班开产规则。
                                productionStartTime = FirstInspectionQtyUtil.resolveTrialProductionStartTime(
                                        context, sku, shifts, firstInspectionBaseTime, defaultProductionStartTime,
                                        ScheduleTypeEnum.NEW_SPEC.getCode());
                            }
                            if (plannedRepairAffectingSwitch) {
                                log.info("新增SKU计划性维修时间轴生效, materialCode: {}, machineCode: {}, "
                                                + "switchStartTime: {}, switchEndTime: {}, preheatMinutes: {}, "
                                                + "productionReadyTime: {}, firstInspectionExtraHours: 0",
                                        sku.getMaterialCode(), machineCode,
                                        LhScheduleTimeUtil.formatDateTime(mouldChangeStartTime),
                                        LhScheduleTimeUtil.formatDateTime(mouldChangeCompleteTime),
                                        LhScheduleTimeUtil.getCapsulePreheatMinutes(context),
                                        LhScheduleTimeUtil.formatDateTime(plannedRepairReadyTime));
                            }
                            if (maintenanceOverlapSwitch) {
                                Date maintenanceStartTime = getMaintenanceScheduleService()
                                        .resolveOverlappedMaintenanceStartTime(
                                        context, candidateMachine,
                                        mouldChangeStartTime, mouldChangeCompleteTime);
                                log.info("新增SKU正规换模与精度计划并行时间轴生效, batchNo: {}, scheduleDate: {}, "
                                                + "materialCode: {}, machineCode: {}, switchStartTime: {}, "
                                                + "switchEndTime: {}, maintenanceStartTime: {}, "
                                                + "maintenanceAndPreheatReadyTime: {}, "
                                                + "finalProductionReadyTime: {}, switchHours: {}, "
                                                + "preheatMinutes: {}, analysis: 换模+精度计划",
                                        context.getBatchNo(),
                                        LhScheduleTimeUtil.formatDate(context.getScheduleTargetDate()),
                                        sku.getMaterialCode(), machineCode,
                                        LhScheduleTimeUtil.formatDateTime(mouldChangeStartTime),
                                        LhScheduleTimeUtil.formatDateTime(mouldChangeCompleteTime),
                                        LhScheduleTimeUtil.formatDateTime(maintenanceStartTime),
                                        LhScheduleTimeUtil.formatDateTime(maintenanceReadyTime),
                                        LhScheduleTimeUtil.formatDateTime(firstInspectionBaseTime),
                                        switchDurationHours,
                                        LhScheduleTimeUtil.getCapsulePreheatMinutes(context));
                                String overlapDetail = new StringBuilder(384)
                                        .append("batchNo=").append(context.getBatchNo())
                                        .append("，排程日期=")
                                        .append(LhScheduleTimeUtil.formatDate(context.getScheduleTargetDate()))
                                        .append("，机台=").append(machineCode)
                                        .append("，物料=").append(sku.getMaterialCode())
                                        .append("，换模开始=")
                                        .append(LhScheduleTimeUtil.formatDateTime(mouldChangeStartTime))
                                        .append("，换模结束=")
                                        .append(LhScheduleTimeUtil.formatDateTime(mouldChangeCompleteTime))
                                        .append("，精度开始=")
                                        .append(LhScheduleTimeUtil.formatDateTime(maintenanceStartTime))
                                        .append("，精度及预热完成=")
                                        .append(LhScheduleTimeUtil.formatDateTime(maintenanceReadyTime))
                                        .append("，最终恢复=")
                                        .append(LhScheduleTimeUtil.formatDateTime(firstInspectionBaseTime))
                                        .append("，组合原因=换模+精度计划")
                                        .toString();
                                PriorityTraceLogHelper.appendProcessLog(
                                        context, "换模+精度计划并行时间轴", overlapDetail);
                            }
                            // 清洗与普通换模重叠时只执行换模，开产时间仍按换模/首检规则计算；清洗原因由结果备注单独记录。
                        }
                    }
                }
                }
                if (!takeoverWithoutMouldChange && mouldChangeStartTime == null) {
                    rollbackMouldResourceAllocation(context, sku, mouldResourceAllocationResult,
                            pairMouldResourceAllocationResult);
                    if (firstInspectionRetryRequired) {
                        // 首检只是在当前时间无法落位，保留同一候选机台并按后移时间重新走完整资源链。
                        pendingFirstInspectionRetryMachineCode = machineCode;
                        candidateCache.clearCapacityCache();
                        continue;
                    }
                    excludedMachineCodes.add(machineCode);
                    candidateCache.removeMachine(machineCode);
                    recordExcludedMachineReason(excludedMachineReasonMap, machineCode,
                            switchAllocateFailReason == NewSpecFailReasonEnum.FIRST_INSPECTION_SHIFT_ALLOCATE_FAILED
                                    ? "首检分配失败" : "换模窗口分配失败",
                            machineReadyTime, switchReadyTime, mouldChangeStartTime, mouldChangeCompleteTime,
                            inspectionTime, productionStartTime, null, null, null);
                    failReason = selectHigherPriorityFailReason(
                            failReason, switchAllocateFailReason == null
                                    ? NewSpecFailReasonEnum.MOULD_CHANGE_SHIFT_ALLOCATE_FAILED
                                    : switchAllocateFailReason);
                    continue;
                }
                // 6. 基于首检分配时间生成新增规格排产结果，并校验当日是否有有效产能
                // 普通换模沿用"总时长已含首检"的旧口径；
                // 维保重叠时改为"4小时切换 + 1小时首检"的专用口径。
                int machineMouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(candidateMachine);
                int runtimeShiftCapacity = ShiftCapacityResolverUtil.resolveRuntimeShiftCapacity(
                        context, candidateMachine, sku.getShiftCapacity());
                if (takeoverWithoutMouldChange
                        && Objects.nonNull(firstProductionStartTime)
                        && Objects.nonNull(productionNotBeforeTime)
                        && productionNotBeforeTime.after(firstProductionStartTime)) {
                    /*
                     * 共用模具接管不执行换模和首检资源分配，但它仍然属于新增 SKU 的正式生产。
                     * 因此 X/T 中班门禁及胎胚可供时间不能因“零换模”路径被绕过：保留原接管
                     * 边界作为理论开产时间，只把真实生产起点抬高到统一门禁，再复用现有停机、
                     * 班次管控和运行态产能校正。该分支不新增首检条数，也不占用换模次数。
                     */
                    theoreticalProductionStartTime = firstProductionStartTime;
                    Date constrainedTakeoverStartTime =
                            NewSpecEmbryoAvailableTimeResolver.resolveActualProductionStartTime(
                                    theoreticalProductionStartTime, productionNotBeforeTime);
                    firstProductionStartTime =
                            ShiftProductionControlUtil.resolveFirstSchedulableStartIgnoringCleaning(
                                    context, machineCode, constrainedTakeoverStartTime, schedulingShifts,
                                    runtimeShiftCapacity, sku.getLhTimeSeconds(), machineMouldQty);
                    productionStartTime = firstProductionStartTime;
                    productionStartTimeConstrained = true;
                    firstInspectionAttributionShift =
                            NewSpecEmbryoAvailableTimeResolver.resolveProductionShift(
                                    schedulingShifts, firstProductionStartTime);
                    firstInspectionAttributionTime = firstProductionStartTime;
                    log.info("新增SKU无换模接管应用统一生产门禁, batchNo: {}, scheduleDate: {}, "
                                    + "materialCode: {}, productStatus: {}, machineCode: {}, "
                                    + "theoreticalProductionStartTime: {}, productionNotBeforeTime: {}, "
                                    + "actualProductionStartTime: {}",
                            context.getBatchNo(), dayContext.getScheduleDate(),
                            sku.getMaterialCode(), sku.getProductStatus(), machineCode,
                            LhScheduleTimeUtil.formatDateTime(theoreticalProductionStartTime),
                            LhScheduleTimeUtil.formatDateTime(productionNotBeforeTime),
                            LhScheduleTimeUtil.formatDateTime(firstProductionStartTime));
                }
                if (!productionStartTimeConstrained) {
                    // 生产门禁未推迟时间轴时完整保留现有“首检后可排时间 + 增机日”计算顺序。
                    firstProductionStartTime =
                            ShiftProductionControlUtil.resolveFirstSchedulableStartIgnoringCleaning(
                                    context, machineCode, productionStartTime, schedulingShifts,
                                    runtimeShiftCapacity, sku.getLhTimeSeconds(), machineMouldQty);
                    earlyProductionDecision = releasedContinuationReuse
                            ? null : structureSwitchLookbackApplied
                            ? structureSwitchLookbackDecision
                            : resolveEarlyProductionDecision(
                                    context, sku, firstProductionStartTime, schedulingShifts, isEnding,
                                    dayContext.getCurrentPhase());
                    /*
                     * 原续作机台原模具重启只豁免切换动作，正式开产仍必须经过与普通增机
                     * 相同的 dayN 生效日下限。该对齐不调用提前生产，也不会影响真实换模/
                     * 换活字块在T日中班完成跨日准备的时间轴。
                     */
                    LocalDate productionAlignmentDate = releasedContinuationReuse
                            && Objects.nonNull(sku.getFirstAddMachineProductionDate())
                            ? sku.getFirstAddMachineProductionDate()
                            : currentAddMachineProductionDate;
                    firstProductionStartTime = alignProductionStartTimeByAddMachineDate(
                            context, sku, firstProductionStartTime, schedulingShifts,
                            totalScheduledQty, productionAlignmentDate,
                            isEnding, earlyProductionDecision);
                    theoreticalProductionStartTime = firstProductionStartTime;
                }
                if (Objects.nonNull(selectedAvailabilityPlan)
                        && selectedAvailabilityPlan.isAvailable()
                        && Objects.nonNull(
                        selectedAvailabilityPlan.getCandidateAvailableProductionTime())) {
                    /*
                     * 选机阶段的计划只负责冻结不含胎胚门禁的候选准备时间轴。正式落班必须继续
                     * 使用上方已经按“换模/首检完成时间 + 正式生产门禁”计算出的时间，不能把候选
                     * 预演时间直接覆盖正式生产起点；否则会绕过胎胚最早可供时间。
                     */
                    Date candidateProductionStartTime =
                            selectedAvailabilityPlan.getCandidateAvailableProductionTime();
                    LhShiftConfigVO formalTargetShift =
                            NewSpecEmbryoAvailableTimeResolver.resolveProductionShift(
                                    schedulingShifts, firstProductionStartTime);
                    log.debug("新增SKU正式排产复用选机真实可开产时间, batchNo: {}, "
                            + "scheduleDate: {}, materialCode: {}, machineCode: {}, "
                            + "candidatePreviewTime: {}, formalProductionTime: {}, formalTargetShift: {}",
                            context.getBatchNo(), dayContext.getScheduleDate(),
                            sku.getMaterialCode(), machineCode,
                            LhScheduleTimeUtil.formatDateTime(candidateProductionStartTime),
                            LhScheduleTimeUtil.formatDateTime(firstProductionStartTime),
                            Objects.isNull(formalTargetShift)
                                    ? "无" : "class" + formalTargetShift.getShiftIndex());
                }
                if (firstProductionStartTime == null
                        || !dayContext.contains(firstProductionStartTime)) {
                    dailyDeferredReason = "换模或首检完成后当前业务日已无可开产班次";
                    log.debug("新增SKU排程窗口内无可开产时间, materialCode: {}, 机台: {}, 首检时间: {}, 班产: {}, 硫化时间: {}, 模数: {}",
                            sku.getMaterialCode(), machineCode,
                            LhScheduleTimeUtil.formatDateTime(productionStartTime),
                            sku.getShiftCapacity(), sku.getLhTimeSeconds(), machineMouldQty);
                    inspectionBalance.rollbackInspection(context, inspectionTime);
                    rollbackMouldChangeAllocation(context, sku, mouldChangeBalance, mouldChangeStartTime);
                    rollbackMouldResourceAllocation(context, sku, mouldResourceAllocationResult,
                            pairMouldResourceAllocationResult);
                    excludedMachineCodes.add(machineCode);
                    candidateCache.removeMachine(machineCode);
                    recordExcludedMachineReason(excludedMachineReasonMap, machineCode,
                            "排程窗口内无可开产时间",
                            machineReadyTime, switchReadyTime, mouldChangeStartTime, mouldChangeCompleteTime,
                            inspectionTime, productionStartTime, null, null, null);
                    failReason = selectHigherPriorityFailReason(
                            failReason, NewSpecFailReasonEnum.NO_CAPACITY_IN_SCHEDULE_WINDOW);
                    continue;
                }
                int candidateTargetQty = resolveCandidateTargetQty(
                        context, sku, candidateMachine, mouldChangeStartTime, firstProductionStartTime,
                        schedulingShifts, candidates, dynamicTargetQty, totalScheduledQty,
                        excludedMachineCodes, quantityPolicy);
                Map<Integer, Integer> shiftCapacityMap = calculateShiftCapacityMap(
                        context, candidateMachine, sku, firstProductionStartTime,
                        mouldChangeStartTime, mouldChangeCompleteTime,
                        schedulingShifts, machineMouldQty, runtimeShiftCapacity, isEnding,
                        productionStartTimeConstrained);
                if (takeoverWithoutMouldChange) {
                    // A 接管不产生首检数量或首检产能扣减，沿用机台真实可用产能图。
                } else if (Objects.nonNull(firstInspectionAllocationPlan)
                        && firstInspectionAllocationPlan.isValid()
                        && firstInspectionAllocationPlan.getInspectionQty() > 0) {
                    /*
                     * 普通换模/换活字块直接复用选机阶段形成的跨班首检计划。
                     * 完整窗口用于补入正式开产前班次的首检占用；正常生产产能图仍只包含
                     * 当前业务日可写班次，两者合并后不会重复计算同班产能。
                     */
                    shiftCapacityMap = FirstInspectionQtyUtil
                            .applyFirstInspectionAllocationToCapacityMap(
                                    context.getScheduleWindowShifts(), shiftCapacityMap,
                                    firstInspectionAllocationPlan);
                } else if (productionStartTimeConstrained) {
                    /*
                     * 调用部分班次首检重载：普通SKU首检计入实际开始后的物理总产能，
                     * 试制SKU从同一部分班次继续扣除固定2小时，不允许按完整班产高估。
                     */
                    shiftCapacityMap =
                            FirstInspectionQtyUtil.applyEmbryoAvailableFirstInspectionCapacity(
                                    context, sku, schedulingShifts, firstInspectionAttributionShift,
                                    shiftCapacityMap, runtimeShiftCapacity, dynamicTargetQty,
                                    ScheduleTypeEnum.NEW_SPEC.getCode(), machineCode);
                } else {
                    // 生产门禁未推迟现有时间轴时继续使用原首检产能图，避免扩大正规 SKU 行为变化。
                    shiftCapacityMap = FirstInspectionQtyUtil.applyFirstInspectionQtyToCapacityMap(
                            context, sku, schedulingShifts, firstInspectionAttributionShift, shiftCapacityMap,
                            runtimeShiftCapacity, dynamicTargetQty,
                            ScheduleTypeEnum.NEW_SPEC.getCode(), machineCode);
                }
                // 按SKU结构统一判断是否执行日标准量补差；未命中时保留前序首检和停机等实际扣减结果。
                shiftCapacityMap = applyDailyStandardCapacityAdjust(
                        context, sku, machineCode, schedulingShifts,
                        shiftCapacityMap, runtimeShiftCapacity);
                int maxQtyToWindowEnd = sumShiftCapacity(shiftCapacityMap);
                MachineProductionSegment segment = buildMachineProductionSegment(
                        context, sku, machineCode, mouldChangeStartTime, firstProductionStartTime,
                        maxQtyToWindowEnd, runtimeShiftCapacity, shiftCapacityMap);
                if (releasedContinuationReuse) {
                    segment.setNeedChangeover(false);
                    segment.setChangeoverShiftIndex(-1);
                }
                /*
                 * 收尾必须在候选机台、换模完成点和真实班次产能确定后判定。
                 * 只要SKU真实硫化余量已能被当前物理机台组完整承接，本块立即切换为
                 * 严格收尾，结果、dayN和库存账本后续均以同一真实剩余量提交。
                 */
                int realtimeProductionRemainingQty =
                        this.resolveActualSurplusRemainingQty(context, sku);
                boolean actualFinalStrictBlock = isFinalStrictProductionBlock(
                        realtimeProductionRemainingQty, maxQtyToWindowEnd, wholeSingleControlUnit);
                if (actualFinalStrictBlock) {
                    isEnding = true;
                    quantityPolicy = ProductionQuantityPolicy.from(sku, true);
                    candidateTargetQty = realtimeProductionRemainingQty;
                    // 目标量补满只能用于前置资源规划，最终收尾提交必须恢复到实时硫化余量账本。
                    getTargetScheduleQtyResolver().syncProductionRemainingQtyToRemaining(
                            context, sku, realtimeProductionRemainingQty, "新增真实物理块严格收尾");
                    log.info("新增SKU按真实物理产能块进入严格收尾, batchNo: {}, scheduleDate: {}, "
                                    + "materialCode: {}, machineCode: {}, productionRemainingQty: {}, "
                                    + "physicalGroupCapacity: {}",
                            context.getBatchNo(), dayContext.getScheduleDate(), sku.getMaterialCode(),
                            machineCode, realtimeProductionRemainingQty,
                            resolvePhysicalGroupCapacity(maxQtyToWindowEnd, wholeSingleControlUnit));
                }
                MachineScheduleRole role = resolveMachineScheduleRole(quantityPolicy, totalScheduledQty,
                        maxQtyToWindowEnd, candidateTargetQty);
                segment.setRole(role);
                boolean singleMachineWindowFill = shouldFillSingleMachineToWindowEnd(
                        context, sku, candidateMachine, isEnding, totalScheduledQty,
                        candidateTargetQty, maxQtyToWindowEnd, earlyProductionDecision);
                int machinePlanQty = singleMachineWindowFill
                        ? maxQtyToWindowEnd
                        : resolveMachinePlanQty(context, sku, quantityPolicy, role, segment,
                                candidateTargetQty, totalScheduledQty, maxQtyToWindowEnd, runtimeShiftCapacity);
                if (!singleMachineWindowFill) {
                    machinePlanQty = resolveDynamicMachinePlanQtyByDailyCapacity(
                            context, sku, candidates, excludedMachineCodes, quantityPolicy, segment,
                            candidateMachine, schedulingShifts, capacityCalculate, candidateTargetQty,
                            totalScheduledQty, machinePlanQty, dayContext, dailyOrderEntry);
                }
                if (CollectionUtils.isEmpty(addMachineProductionDateList)
                        && !CollectionUtils.isEmpty(segment.getAddMachineProductionDateList())) {
                    addMachineProductionDateList.addAll(segment.getAddMachineProductionDateList());
                }
                if (segment.getFutureDayDemandMachineCount() > 1) {
                    /*
                     * T+2 后看 T+3 推导出多机台时，本轮目标量必须同步放大到这些机台的窗口有效产能，
                     * 否则第一台满班后 remainingQty 会归零，第二台无法进入现有候选机台主链。
                     */
                    candidateTargetQty = Math.max(candidateTargetQty,
                            segment.getMaxQtyToWindowEnd() * segment.getFutureDayDemandMachineCount());
                }
                log.info("新增SKU候选机台动态分配, materialCode: {}, 机台: {}, 角色: {}, 最大可排量: {}, "
                                + "累计已排: {}, 窗口目标量: {}, 本机台计划量: {}, 换模班次: {}, 开产班次: {}",
                        sku.getMaterialCode(), machineCode, role, maxQtyToWindowEnd, totalScheduledQty,
                        candidateTargetQty, machinePlanQty, segment.getChangeoverShiftIndex(),
                        segment.getStartProductionShiftIndex());
                logNewSpecMachinePlanDecision(sku, quantityPolicy, isEnding, singleMachineWindowFill,
                        candidateTargetQty, maxQtyToWindowEnd, machinePlanQty, null);
                if (machinePlanQty <= 0) {
                    if (segment.isExistingSameMaterialSatisfied()) {
                        log.info("新增SKU已有同物料机台满足dayN规则，跳过当前新增候选, materialCode: {}, "
                                        + "candidateMachine: {}, existingResultCount: {}",
                                sku.getMaterialCode(), machineCode,
                                countExistingSameMaterialResults(context, sku, machineCode));
                        inspectionBalance.rollbackInspection(context, inspectionTime);
                        rollbackMouldChangeAllocation(context, sku, mouldChangeBalance, mouldChangeStartTime);
                        rollbackMouldResourceAllocation(context, sku, mouldResourceAllocationResult,
                                pairMouldResourceAllocationResult);
                        removeCurrentNewSpecSku(context, iterator, sku);
                        currentSkuRemoved = true;
                        scheduled = true;
                        /*
                         * 当前候选没有形成新增结果，只是既有同物料结果已满足 dayN。
                         * 清理待写快照与暂存选机输入，禁止把“无需继续扩机”误记成一次未命中或实际命中选机。
                         */
                        pendingCandidateTraceSnapshot = null;
                        pendingTraceCandidates = null;
                        pendingTraceSelectedMachine = null;
                        pendingTraceDayEndTime = null;
                        state.clearPendingMachinePriorityTrace(sku);
                        if (actualAllowedAddMachineCount <= 0) {
                            /*
                             * 当前遍历最终确认已有同物料机台已满足 dayN，未产生任何新增机台需求；
                             * 因而撤销刚才的观察条目，避免把实际已被现有规则过滤的 SKU 记入每日顺序。
                             */
                            dayContext.getNewSpecOrderLogCollector().remove(dailyOrderEntry);
                        }
                        break;
                    }
                    log.debug("新增SKU动态分配后本机台计划量为0, materialCode: {}, 机台: {}, 目标量: {}, 换模开始: {}, 开产时间: {}",
                            sku.getMaterialCode(), machineCode, candidateTargetQty,
                            LhScheduleTimeUtil.formatDateTime(mouldChangeStartTime),
                            LhScheduleTimeUtil.formatDateTime(firstProductionStartTime));
                    appendNewSpecCandidateRejectedProcessLog(context, sku, machineCode,
                            "动态分配后本机台计划量为0",
                            machineReadyTime, switchReadyTime, mouldChangeStartTime, mouldChangeCompleteTime,
                            firstProductionStartTime, maxQtyToWindowEnd, machinePlanQty, null);
                    inspectionBalance.rollbackInspection(context, inspectionTime);
                    rollbackMouldChangeAllocation(context, sku, mouldChangeBalance, mouldChangeStartTime);
                    rollbackMouldResourceAllocation(context, sku, mouldResourceAllocationResult,
                            pairMouldResourceAllocationResult);
                    excludedMachineCodes.add(machineCode);
                    candidateCache.removeMachine(machineCode);
                    recordExcludedMachineReason(excludedMachineReasonMap, machineCode,
                            "动态分配后本机台计划量为0",
                            machineReadyTime, switchReadyTime, mouldChangeStartTime, mouldChangeCompleteTime,
                            inspectionTime, firstProductionStartTime, maxQtyToWindowEnd, machinePlanQty, null);
                    failReason = selectHigherPriorityFailReason(
                            failReason, NewSpecFailReasonEnum.NO_CAPACITY_IN_SCHEDULE_WINDOW);
                    continue;
                }
                if (!takeoverWithoutMouldChange
                        && Objects.nonNull(firstInspectionAllocationPlan)
                        && firstInspectionAllocationPlan.isValid()
                        && firstInspectionAllocationPlan.getInspectionQty() > machinePlanQty) {
                    /*
                     * 选机阶段只能读取当时的 SKU 总剩余量；动态分机台后，本机台最终目标量
                     * 可能小于“同班次前2台”参数量。首检本身计入目标量，因此必须使用同一
                     * 切换结束点按最终目标量缩短首检区间，禁止目标2条却写入4条首检。
                     * 缩量后首检已覆盖本机台全部目标量，正式生产起点和目标班次不会前移。
                     */
                    FirstInspectionAllocationPlan refinedInspectionPlan =
                            this.resolveFirstInspectionAllocationPlan(
                                    context, sku, candidateMachine,
                                    context.getScheduleWindowShifts(), mouldChangeStartTime,
                                    mouldChangeCompleteTime, runtimeShiftCapacity,
                                    machineMouldQty, machinePlanQty,
                                    inspectionScheduleTypeCode);
                    if (!refinedInspectionPlan.isValid()) {
                        LhShiftConfigVO failedCountingShift =
                                Objects.nonNull(refinedInspectionPlan.getCountingShift())
                                        ? refinedInspectionPlan.getCountingShift()
                                        : FirstInspectionQtyUtil.resolveAttributionShift(
                                                context.getScheduleWindowShifts(),
                                                mouldChangeCompleteTime);
                        Date nextSwitchReadyTime = resolveNextFirstInspectionRetryReadyTime(
                                failedCountingShift, mouldChangeStartTime);
                        firstInspectionRetryReadyTimeMap.put(machineCode, nextSwitchReadyTime);
                        log.info("新增SKU按最终机台目标量复核首检失败，整段准备时间轴顺延, "
                                        + "batchNo: {}, materialCode: {}, machineCode: {}, "
                                        + "machinePlanQty: {}, switchCompleteTime: {}, reason: {}, "
                                        + "nextSwitchReadyTime: {}",
                                context.getBatchNo(), sku.getMaterialCode(), machineCode,
                                machinePlanQty,
                                LhScheduleTimeUtil.formatDateTime(mouldChangeCompleteTime),
                                refinedInspectionPlan.getInvalidReason(),
                                LhScheduleTimeUtil.formatDateTime(nextSwitchReadyTime));
                        rollbackMouldChangeAllocation(
                                context, sku, mouldChangeBalance, mouldChangeStartTime);
                        rollbackMouldResourceAllocation(
                                context, sku, mouldResourceAllocationResult,
                                pairMouldResourceAllocationResult);
                        pendingFirstInspectionRetryMachineCode = machineCode;
                        candidateCache.clearCapacityCache();
                        continue;
                    }
                    log.info("新增SKU首检按最终机台目标量收敛, batchNo: {}, materialCode: {}, "
                                    + "machineCode: {}, 原首检量: {}, 收敛后首检量: {}, "
                                    + "machinePlanQty: {}, 首检区间: [{}, {})",
                            context.getBatchNo(), sku.getMaterialCode(), machineCode,
                            firstInspectionAllocationPlan.getInspectionQty(),
                            refinedInspectionPlan.getInspectionQty(), machinePlanQty,
                            LhScheduleTimeUtil.formatDateTime(
                                    refinedInspectionPlan.getInspectionStartTime()),
                            LhScheduleTimeUtil.formatDateTime(
                                    refinedInspectionPlan.getInspectionEndTime()));
                    firstInspectionAllocationPlan = refinedInspectionPlan;
                    firstInspectionAttributionShift = refinedInspectionPlan.getCountingShift();
                    firstInspectionAttributionTime = refinedInspectionPlan.getInspectionEndTime();
                }
                /*
                 * 首检是新规格实际上机的强制前置条件。候选结果、首检计数和日计划账本提交前，
                 * 先按最终本机台目标量预演首检条数及SYS0303004剩余额度；当前班次放不下时，
                 * 回滚换模和模具预占，并保留同一机台从下一合法切换时间重新竞争。
                 */
                boolean crossShiftInspectionCanLand = Objects.nonNull(firstInspectionAllocationPlan)
                        && firstInspectionAllocationPlan.getInspectionQty() > 0
                        ? this.canLandRequiredFirstInspection(
                                context, sku, firstInspectionAllocationPlan, machineCode)
                        : canLandRequiredFirstInspection(
                                context, sku, firstInspectionAttributionShift, runtimeShiftCapacity,
                                machinePlanQty, machineCode);
                if (!takeoverWithoutMouldChange && !crossShiftInspectionCanLand) {
                    Date nextSwitchReadyTime = resolveNextFirstInspectionRetryReadyTime(
                            firstInspectionAttributionShift, mouldChangeStartTime);
                    firstInspectionRetryReadyTimeMap.put(machineCode, nextSwitchReadyTime);
                    log.info("新增SKU强制首检受SYS0303004限制，回滚候选并顺延切换, "
                                    + "batchNo: {}, materialCode: {}, machineCode: {}, firstInspectionShift: class{}, "
                                    + "nextSwitchReadyTime: {}",
                            context.getBatchNo(), sku.getMaterialCode(), machineCode,
                            firstInspectionAttributionShift.getShiftIndex(),
                            LhScheduleTimeUtil.formatDateTime(nextSwitchReadyTime));
                    rollbackMouldChangeAllocation(context, sku, mouldChangeBalance, mouldChangeStartTime);
                    rollbackMouldResourceAllocation(context, sku, mouldResourceAllocationResult,
                            pairMouldResourceAllocationResult);
                    pendingFirstInspectionRetryMachineCode = machineCode;
                    candidateCache.clearCapacityCache();
                    continue;
                }

                // 预演全部通过后才正式占用首检资源，失败候选不会提前消费首检班次配额。
                if (!takeoverWithoutMouldChange) {
                    inspectionTime = inspectionBalance.allocateInspection(
                            context, machineCode, firstInspectionAttributionTime);
                    LhShiftConfigVO allocatedInspectionShift = FirstInspectionQtyUtil
                            .resolveFirstInspectionAttributionShift(
                                    context, sku, context.getScheduleWindowShifts(), inspectionTime,
                                    inspectionScheduleTypeCode);
                    if (Objects.isNull(inspectionTime) || Objects.isNull(allocatedInspectionShift)
                            || !Objects.equals(firstInspectionAttributionShift.getShiftIndex(),
                            allocatedInspectionShift.getShiftIndex())) {
                        inspectionBalance.rollbackInspection(context, inspectionTime);
                        Date nextSwitchReadyTime = resolveNextFirstInspectionRetryReadyTime(
                                firstInspectionAttributionShift, mouldChangeStartTime);
                        firstInspectionRetryReadyTimeMap.put(machineCode, nextSwitchReadyTime);
                        log.info("新增SKU首检正式分配与预演不一致，回滚候选并顺延切换, "
                                        + "batchNo: {}, materialCode: {}, machineCode: {}, expectedShift: class{}, "
                                        + "actualShift: {}, nextSwitchReadyTime: {}",
                                context.getBatchNo(), sku.getMaterialCode(), machineCode,
                                firstInspectionAttributionShift.getShiftIndex(),
                                Objects.isNull(allocatedInspectionShift)
                                        ? null : "class" + allocatedInspectionShift.getShiftIndex(),
                                LhScheduleTimeUtil.formatDateTime(nextSwitchReadyTime));
                        rollbackMouldChangeAllocation(context, sku, mouldChangeBalance, mouldChangeStartTime);
                        rollbackMouldResourceAllocation(context, sku, mouldResourceAllocationResult,
                                pairMouldResourceAllocationResult);
                        pendingFirstInspectionRetryMachineCode = machineCode;
                        candidateCache.clearCapacityCache();
                        continue;
                    }
                }
                // 从这里开始 targetScheduleQty 临时改为“本机台计划量”，仅用于结果构建和班次分配。
                // 后续失败、继续下一台或本 SKU 结束时必须恢复到业务目标，避免污染后续候选机台。
                dynamicTargetQty = candidateTargetQty;
                sku.setTargetScheduleQty(machinePlanQty);
                takeoverReleasedContinuousPlaceholderIfNeeded(
                        context, machineCode, schedulingShifts, deferredCompensationSkuList);
                LhScheduleResult result = buildNewSpecScheduleResult(
                        context, candidateMachine, sku, firstProductionStartTime, mouldChangeStartTime,
                        mouldChangeCompleteTime, schedulingShifts, machineMouldQty, isEnding,
                        mouldResourceAllocationResult, shiftCapacityMap, firstInspectionAttributionShift,
                        firstInspectionAllocationPlan, productionStartTimeConstrained);
                if (result == null || result.getDailyPlanQty() == null || result.getDailyPlanQty() <= 0) {
                    log.debug("新增SKU结果无有效班次计划量, materialCode: {}, 机台: {}, 目标量: {}, 开产时间: {}",
                            sku.getMaterialCode(), machineCode, sku.resolveTargetScheduleQty(),
                            LhScheduleTimeUtil.formatDateTime(firstProductionStartTime));
                    appendNewSpecCandidateRejectedProcessLog(context, sku, machineCode,
                            "结果无有效班次计划量",
                            machineReadyTime, switchReadyTime, mouldChangeStartTime, mouldChangeCompleteTime,
                            firstProductionStartTime, maxQtyToWindowEnd, machinePlanQty, null);
                    // 无有效产能时回滚首检和换模占用，避免影响后续SKU排产
                    inspectionBalance.rollbackInspection(context, inspectionTime);
                    FirstInspectionQtyUtil.rollbackFirstInspectionSequence(context, firstInspectionAttributionShift);
                    rollbackMouldChangeAllocation(context, sku, mouldChangeBalance, mouldChangeStartTime);
                    rollbackMouldResourceAllocation(context, sku, mouldResourceAllocationResult,
                            pairMouldResourceAllocationResult);
                    // 候选机台失败时恢复原目标量，避免把本次失败收敛值泄漏到后续候选机台。
                    sku.setTargetScheduleQty(baseTargetScheduleQty);
                    excludedMachineCodes.add(machineCode);
                    candidateCache.removeMachine(machineCode);
                    recordExcludedMachineReason(excludedMachineReasonMap, machineCode,
                            "结果无有效班次计划量",
                            machineReadyTime, switchReadyTime, mouldChangeStartTime, mouldChangeCompleteTime,
                            inspectionTime, firstProductionStartTime, maxQtyToWindowEnd, machinePlanQty, null);
                    failReason = selectHigherPriorityFailReason(
                            failReason, NewSpecFailReasonEnum.NO_CAPACITY_IN_SCHEDULE_WINDOW);
                    continue;
                }

                applyTypeBlockRelationToNewSpecResult(
                        context, result, sku, machineCode, candidateMachine,
                        mouldChangeStartTime, isTypeBlockRelation);
                if (releasedContinuationReuse) {
                    this.applyReleasedContinuationReuseToResult(
                            context, result, sku, candidateMachine,
                            mouldResourceAllocationResult, firstProductionStartTime);
                }

                sku.setMouldQty(machineMouldQty);
                applyNightNoMouldChangeContinuationFill(
                        context, sku, result, schedulingShifts, quantityPolicy);
                applyDailyStandardPlanQtyToResult(
                        context, sku, result, schedulingShifts, runtimeShiftCapacity,
                        firstInspectionAllocationPlan);
                if (actualFinalStrictBlock) {
                    // 严格收尾不得继承普通收尾补满或共用胎胚错峰登记的允许超量。
                    context.getEndingFillAllowedOverQtyMap().remove(result);
                    context.getSharedEmbryoEndingStaggerAllowedOverQtyMap().remove(result);
                }
                LhScheduleResult pairResult = wholeSingleControlUnit
                        ? buildWholeSingleControlPairResult(context, result, pairSingleControlMachine, sku,
                                machineMouldQty, pairMouldResourceAllocationResult)
                        : null;
                /*
                 * 精度前插排必须使用进入本候选前的真实生产余量，不能使用满班补齐、模数取整或
                 * 动态扩机后放大的临时目标量。该值在日计划账本扣减前读取，失败时可完整回滚。
                 */
                int precisionPendingQty = getTargetScheduleQtyResolver()
                        .resolveProductionRemainingQty(context, sku);
                // 7. 先消费dayN节奏账本，再落地结果与刷新机台状态；非收尾实际排产由SKU实际消费账本控制。
                // 收尾/试制等严格目标量会被截断；正规/量试非收尾允许记录满班补齐超排。
                /*
                 * 日计划与实际生产账本只在结果最终生成后消费。正常资源竞争及历史遗留阶段
                 * 只能消费当前日及历史欠产，提前生产阶段才允许借用后续 dayN；若裁剪为零，
                 * 必须恢复本次尝试的全部账本写入。
                 */
                DailyQuotaLedgerBaseline quotaLedgerBaseline =
                        DailyQuotaLedgerBaseline.capture(context, sku);
                boolean allowFutureQuotaConsumption =
                        isEarlyProductionPhase(dayContext.getCurrentPhase());
                int machineScheduledQty = wholeSingleControlUnit
                        ? this.applyWholeSingleControlBlockToDailyQuota(
                                context, sku, result, pairResult, schedulingShifts,
                                allowFutureQuotaConsumption, firstInspectionAllocationPlan)
                        : this.applyBlockToDailyQuota(
                                context, sku, result, schedulingShifts,
                                allowFutureQuotaConsumption, firstInspectionAllocationPlan);
                if (machineScheduledQty <= 0) {
                    appendNewSpecCandidateRejectedProcessLog(context, sku, machineCode,
                            "日计划额度回裁后为0",
                            machineReadyTime, switchReadyTime, mouldChangeStartTime, mouldChangeCompleteTime,
                            firstProductionStartTime, maxQtyToWindowEnd, machinePlanQty, machineScheduledQty);
                    inspectionBalance.rollbackInspection(context, inspectionTime);
                    // buildNewSpecScheduleResult 已登记首检顺序，日计划裁零时必须同步撤销该登记。
                    FirstInspectionQtyUtil.rollbackFirstInspectionSequence(context, firstInspectionAttributionShift);
                    rollbackMouldChangeAllocation(context, sku, mouldChangeBalance, mouldChangeStartTime);
                    rollbackMouldResourceAllocation(context, sku, mouldResourceAllocationResult,
                            pairMouldResourceAllocationResult);
                    quotaLedgerBaseline.restore(context, sku);
                    sku.setTargetScheduleQty(baseTargetScheduleQty);
                    remainingQty = resolveSchedulableRemainingQty(context, sku);
                    sku.setRemainingScheduleQty(remainingQty);
                    if (!needMoreMachine(context, sku)) {
                        break;
                    }
                    excludedMachineCodes.add(machineCode);
                    candidateCache.removeMachine(machineCode);
                    recordExcludedMachineReason(excludedMachineReasonMap, machineCode,
                            "日计划额度回裁后为0",
                            machineReadyTime, switchReadyTime, mouldChangeStartTime, mouldChangeCompleteTime,
                            inspectionTime, firstProductionStartTime, maxQtyToWindowEnd, machinePlanQty,
                            machineScheduledQty);
                    failReason = selectHigherPriorityFailReason(
                            failReason, NewSpecFailReasonEnum.NO_CAPACITY_IN_SCHEDULE_WINDOW);
                    continue;
                }
                Date precisionCompletionTime = result.getSpecEndTime();
                if (Objects.nonNull(pairResult) && Objects.nonNull(pairResult.getSpecEndTime())
                        && (Objects.isNull(precisionCompletionTime)
                        || pairResult.getSpecEndTime().after(precisionCompletionTime))) {
                    precisionCompletionTime = pairResult.getSpecEndTime();
                }
                String precisionRejectReason = getMaintenanceScheduleService()
                        .resolvePrecisionCandidateRejectReason(
                                context, candidateMachine, sku, precisionPendingQty, machineScheduledQty,
                                mouldChangeStartTime, firstProductionStartTime, precisionCompletionTime);
                if (StringUtils.isNotEmpty(precisionRejectReason)) {
                    appendNewSpecCandidateRejectedProcessLog(context, sku, machineCode,
                            precisionRejectReason,
                            machineReadyTime, switchReadyTime, mouldChangeStartTime, mouldChangeCompleteTime,
                            firstProductionStartTime, maxQtyToWindowEnd, machinePlanQty, machineScheduledQty);
                    inspectionBalance.rollbackInspection(context, inspectionTime);
                    FirstInspectionQtyUtil.rollbackFirstInspectionSequence(
                            context, firstInspectionAttributionShift);
                    rollbackMouldChangeAllocation(context, sku, mouldChangeBalance, mouldChangeStartTime);
                    rollbackMouldResourceAllocation(context, sku, mouldResourceAllocationResult,
                            pairMouldResourceAllocationResult);
                    quotaLedgerBaseline.restore(context, sku);
                    sku.setTargetScheduleQty(baseTargetScheduleQty);
                    remainingQty = resolveSchedulableRemainingQty(context, sku);
                    sku.setRemainingScheduleQty(remainingQty);
                    excludedMachineCodes.add(machineCode);
                    candidateCache.removeMachine(machineCode);
                    recordExcludedMachineReason(excludedMachineReasonMap, machineCode,
                            precisionRejectReason,
                            machineReadyTime, switchReadyTime, mouldChangeStartTime, mouldChangeCompleteTime,
                            inspectionTime, firstProductionStartTime, maxQtyToWindowEnd, machinePlanQty,
                            machineScheduledQty);
                    failReason = selectHigherPriorityFailReason(
                            failReason, NewSpecFailReasonEnum.NO_CAPACITY_IN_SCHEDULE_WINDOW);
                    continue;
                }
                // class3/class6 晚班按 workDate 归属后续业务日，不能用自然开产日期写提前生产审计。
                LocalDate resultBusinessDate = resolveProductionWorkDate(
                        schedulingShifts, firstProductionStartTime);
                if (Objects.isNull(resultBusinessDate)) {
                    resultBusinessDate = firstProductionStartTime.toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate();
                }
                // 仅对通过既有资源约束且最终有有效计划量的新增结果追加提前生产审计备注。
                if (!releasedContinuationReuse) {
                    appendEarlyProductionRemark(
                            context, result, earlyProductionDecision, resultBusinessDate);
                }
                if (!releasedContinuationReuse
                        && productionPreparationLookbackApplied
                        && Objects.nonNull(selectedAvailabilityPlan)) {
                    /*
                     * 候选顺序沿用原均衡时间轴；正式跨日准备提交成功后，只更新最终确认日志
                     * 中已选机台的实际换模和开产时间，不回写其他候选，也不重新执行选机。
                     */
                    LhShiftConfigVO committedProductionShift =
                            NewSpecEmbryoAvailableTimeResolver.resolveProductionShift(
                                    schedulingShifts, firstProductionStartTime);
                    selectedAvailabilityPlan = selectedAvailabilityPlan
                            .withCommittedPreparationTimeline(
                                    mouldChangeStartTime, mouldChangeCompleteTime,
                                    firstProductionStartTime, committedProductionShift,
                                    firstInspectionAllocationPlan);
                    candidateAvailabilityPlanMap.put(
                            machineCode, selectedAvailabilityPlan);
                }
                /*
                 * 结果最终确认后、提交任何机台运行态与占用关系前，按暂存的选机输入构建一次
                 * 日志快照并冻结选机时点占用/收尾时间。此时 machineAssignmentMap 与机台收尾
                 * 时间尚未被本轮结果改写（updateMachineState/registerMachineAssignment 在其后），
                 * 保证“首候选=实际命中机台”与延迟写不重读运行态的既有语义。
                 */
                pendingCandidateTraceSnapshot = buildConfirmedTraceSnapshot(
                        context, sku, machineMatch,
                        pendingTraceCandidates, pendingTraceSelectedMachine, pendingTraceDayEndTime,
                        pendingPriorityMetricSnapshotMap,
                        candidateAvailabilityPlanMap,
                        orderedCandidates, candidateMachine, dayContext.getDayEndTime());
                lastConfirmedTraceSnapshot = pendingCandidateTraceSnapshot;
                pendingTraceCandidates = null;
                pendingTraceSelectedMachine = null;
                pendingTraceDayEndTime = null;
                pendingPriorityMetricSnapshotMap =
                        Collections.<String, MachinePriorityMetricSnapshot>emptyMap();
                /*
                 * 排程结果尚未写入、机台运行态尚未推进，此时冻结实际命中机台的前序 SKU 收尾明细。
                 * 候选描述只消费已经冻结的正式候选顺序、时间和软排序指标，不重新执行比较。
                 */
                String realtimeMachineEndingText = machineMatch.resolveRealtimeMachineEndingText(
                        context, sku, candidateMachine.getMachineCode());
                String machineSelectionDescription =
                        MachineSelectionDescriptionFormatter.format(pendingCandidateTraceSnapshot);
                context.getScheduleResultList().add(result);
                context.getScheduleResultSourceSkuMap().put(result, sku);
                if (embryoAvailableTimeConstrained) {
                    // 只有结果和日计划账本均已提交成功，才记录胎胚时间实际应用过程日志。
                    appendEmbryoAvailableTimeAppliedProcessLog(
                            context, sku, candidateMachine, result, schedulingShifts,
                            earliestEmbryoAvailableTime, theoreticalProductionStartTime,
                            firstProductionStartTime, mouldChangeStartTime,
                            mouldChangeCompleteTime,
                            firstInspectionAttributionShift, shiftCapacityMap,
                            runtimeShiftCapacity, machinePlanQty);
                }
                if (!releasedContinuationReuse
                        && (structureSwitchLookbackApplied
                        || productionPreparationLookbackApplied)) {
                    /*
                     * 只有结果、日计划账本和实际消费账本全部提交成功后才登记跨日准备事件。
                     * 失败候选不会残留豁免标识；最终复核仍把事件计入每日15次硬上限，
                     * 仅从早8/中7参考分布告警中排除。
                     */
                    context.registerCrossDayPreparationMouldChange(
                            machineCode, mouldChangeStartTime);
                }
                if (structureSwitchLookbackApplied) {
                    // 仅对最终形成有效计划量的结果记录跨日换模，失败候选不会污染过程日志。
                    this.appendStructureSwitchLookbackProcessLog(
                            context, dayContext, sku, candidateMachine,
                            machineReadyTime, mouldChangeStartTime, mouldChangeCompleteTime,
                            earliestEmbryoAvailableTime, firstProductionStartTime,
                            firstInspectionAttributionShift);
                } else if (productionPreparationLookbackApplied) {
                    // 普通正规、试制和量试统一记录生产日前跨日准备，便于核对空闲产能是否被利用。
                    this.appendProductionPreparationLookbackProcessLog(
                            context, dayContext, sku, candidateMachine,
                            machineReadyTime, mouldChangeStartTime, mouldChangeCompleteTime,
                            productionPreparationNotBeforeTime,
                            productionNotBeforeTime, firstProductionStartTime,
                            firstInspectionAttributionShift);
                }
                if (releasedContinuationReuse) {
                    this.appendReleasedContinuationReuseProcessLog(
                            context, dayContext, sku, candidateMachine,
                            mouldResourceAllocationResult, firstProductionStartTime,
                            schedulingShifts);
                }
                if (getMaintenanceScheduleService().shouldMarkPrecisionPreInsert(
                        candidateMachine, mouldChangeStartTime)) {
                    // 结果及账本已经全部通过，正式登记该物理机台的精度前窗口已被占用。
                    getMaintenanceScheduleService().markPrecisionPreInsertScheduled(
                            context, candidateMachine, result);
                    // 新增规格换模已真实消费换模均衡名额，按结果身份保存分配时间；
                    // 最终时间轴撤销时只回退该次真实占用，不能按结果上的通用切换时间推断。
                    context.getPrecisionPreInsertMouldChangeTimeMap().put(
                            result, mouldChangeStartTime);
                    // 保存正式占用的首检时间和归属班次，最终时间轴复核失败时按原资源精确回退。
                    context.getPrecisionPreInsertInspectionTimeMap().put(result, inspectionTime);
                    context.getPrecisionPreInsertInspectionShiftIndexMap().put(
                            result, firstInspectionAttributionShift.getShiftIndex());
                    if (Objects.nonNull(pairResult)) {
                        context.getPrecisionPreInsertResultSet().add(pairResult);
                    }
                }
                if (ShiftCapacityResolverUtil.isPlannedRepairAffectingSwitch(
                        context, context.getDevicePlanShutList(), machineCode, endingTime,
                        mouldChangeStartTime, mouldChangeCompleteTime)) {
                    // 仅在候选最终形成有效结果后写过程日志，避免失败候选污染批次审计记录。
                    Date repairProductionReadyTime = ShiftCapacityResolverUtil.resolvePlannedRepairProductionReadyTime(
                            context, context.getDevicePlanShutList(), machineCode, endingTime,
                            mouldChangeStartTime, mouldChangeCompleteTime);
                    StringBuilder repairTimelineDetail = new StringBuilder(256);
                    PriorityTraceLogHelper.appendLine(repairTimelineDetail,
                            "机台=" + machineCode + ", SKU=" + sku.getMaterialCode() + ", 切换类型=换模");
                    PriorityTraceLogHelper.appendLine(repairTimelineDetail,
                            "切换开始=" + LhScheduleTimeUtil.formatDateTime(mouldChangeStartTime)
                                    + ", 切换结束=" + LhScheduleTimeUtil.formatDateTime(mouldChangeCompleteTime));
                    PriorityTraceLogHelper.appendLine(repairTimelineDetail,
                            "预热分钟数=" + LhScheduleTimeUtil.getCapsulePreheatMinutes(context)
                                    + ", 最早开产=" + LhScheduleTimeUtil.formatDateTime(repairProductionReadyTime)
                                    + ", 实际首个生产=" + LhScheduleTimeUtil.formatDateTime(firstProductionStartTime)
                                    + ", 首检额外等待小时=0");
                    PriorityTraceLogHelper.appendProcessLog(
                            context, "计划性维修与换模重叠时间轴", repairTimelineDetail.toString().trim());
                }
                // 指定机台真正生成有效结果后才登记成功和保护；失败候选不会污染后续普通新增排产。
                markHistoricalReverseDirectiveSucceeded(
                        context, historicalDirective, sku, result);
                /*
                 * 结构收尾对齐实时复核：必须在 updateMachineState 之前判断，
                 * 此时机台当前物料仍为候选前物料，可正确比较前物料结构与待排SKU结构。
                 * 命中且同结构放行时写结果行标识、首个生产班次分析与机台运行态标识；
                 * 结果提交后再增量刷新【结构×班次】在机统计缓存。
                 */
                StructureEndingAlignmentDecision structureEndingAlignmentDecision =
                        structureEndingAlignmentService.evaluateCandidate(
                                context, sku, candidateMachine);
                updateMachineState(context, candidateMachine, sku, result);
                if (structureEndingAlignmentDecision.isTriggered()
                        && structureEndingAlignmentDecision.isAllowed()) {
                    structureEndingAlignmentService.markStructureEndingAligned(
                            candidateMachine, result);
                }
                structureEndingAlignmentService.onResultCommitted(context, result);
                registerMachineAssignment(context, machineCode, result);
                recordScheduledMachineForResult(context, result, schedulingShifts);
                clearSpecifyReservation(context, machineCode, sku.getMaterialCode());
                /*
                 * 按天换活字块反选成功：机台当前物料与收尾时间已由 updateMachineState 同步，
                 * 物料账本已扣减；此处登记指令成功并释放当天机台预留，避免该机台/物料
                 * 在当天或后续业务日被重复反选或重复排产。
                 */
                markDayTypeBlockReverseDirectiveSucceeded(
                        context, machineCode, sku, result);
                if (wholeSingleControlUnit) {
                    // 冻结为双模的SKU必须同时写入配对侧，配对侧沿用主侧整组裁剪后的班次数量。
                    context.getScheduleResultList().add(pairResult);
                    context.getScheduleResultSourceSkuMap().put(pairResult, sku);
                    // 配对侧同样执行结构收尾对齐复核、标识与缓存增量更新。
                    StructureEndingAlignmentDecision pairStructureEndingAlignmentDecision =
                            structureEndingAlignmentService.evaluateCandidate(
                                    context, sku, pairSingleControlMachine);
                    updateMachineState(context, pairSingleControlMachine, sku, pairResult);
                    if (pairStructureEndingAlignmentDecision.isTriggered()
                            && pairStructureEndingAlignmentDecision.isAllowed()) {
                        structureEndingAlignmentService.markStructureEndingAligned(
                                pairSingleControlMachine, pairResult);
                    }
                    structureEndingAlignmentService.onResultCommitted(context, pairResult);
                    registerMachineAssignment(context, pairSingleControlMachine.getMachineCode(), pairResult);
                    recordScheduledMachineForResult(context, pairResult, schedulingShifts);
                    clearSpecifyReservation(context, pairSingleControlMachine.getMachineCode(), sku.getMaterialCode());
                    // 单控整机配对侧若存在独立反选指令，同样登记成功并释放预留。
                    markDayTypeBlockReverseDirectiveSucceeded(
                            context, pairSingleControlMachine.getMachineCode(), sku, pairResult);
                }
                /*
                 * 结果和机台运行态全部提交成功后，才登记跨日在机绑定。下一业务日阶段一直接在该结果
                 * 追加班次量，不重新匹配机台、不重复预占模具，也不重复累计换模和首检资源。
                 * 单控整机两侧登记为同一个绑定，跨日续排时按整组口径只消费一次 SKU 账本。
                 */
                ActiveMachineBinding activeBinding = new ActiveMachineBinding(
                        MonthPlanDateResolver.buildMaterialStatusKey(
                                sku.getMaterialCode(), sku.getProductStatus()),
                        sku,
                        machineCode,
                        wholeSingleControlUnit
                                ? pairSingleControlMachine.getMachineCode() : null,
                        result,
                        pairResult,
                        isEnding);
                state.registerBinding(activeBinding);
                state.markScheduledAndCarryOver(sku);
                /*
                 * 主副结果、机台状态和跨日在机绑定全部提交后，才写实际命中的实时选机快照。
                 * 失败候选及日计划回裁为零的尝试不会留下排查日志或消耗跨日命中顺序。
                 */
                this.appendNewSpecSelectionRealtimeSnapshotLog(
                        context, sku, candidateMachine, result, pairResult,
                        selectionRealtimeSnapshot,
                        realtimeMachineEndingText, machineSelectionDescription);
                /*
                 * 首检分摊在候选构建阶段已经写入结果并推进计数，但候选随后仍可能被日计划账本、
                 * 精度计划或结果有效性校验拒绝。必须等主副结果、机台运行态和跨日在机绑定全部
                 * 提交后，才把选机阶段传递下来的同一份分摊计划写入过程日志，避免失败候选留下
                 * “首检已落地”的伪审计记录；这里禁止为了日志再次计算首检数量和班次。
                 */
                FirstInspectionQtyUtil.appendCommittedFirstInspectionAllocationProcessLog(
                        context, result, firstInspectionAllocationPlan,
                        inspectionScheduleTypeCode);
                /*
                 * 仅在排程结果和机台运行态全部提交成功后记录一次历史班次产能优先决策，避免
                 * 失败候选、首检顺延或同机重试持续向scheduleLogList累积大文本并放大堆内存。
                 */
                if (Objects.nonNull(currentTargetShift)) {
                    this.appendHistoryResidualCapacityPreferenceLog(
                            context, sku, currentTargetShift,
                            currentShiftCandidates, historyResidualCapacityCandidates,
                            historyResidualCapacityPreferenceEvaluated,
                            historyResidualCapacityPreferenceApplied,
                            historyResidualCapacityPreferenceSkipReason,
                            orderedCandidates, candidateMachine,
                            candidateAvailabilityPlanMap);
                }
                /*
                 * 至此排程结果、主副机台占用和跨日在机绑定均已提交，才确认本轮实际命中并写日志。
                 * 快照已在提交机台运行态前构建并冻结；首检同机顺延沿用首次尝试的暂存输入。
                 */
                machineMatch.traceMachinePriorityOrder(
                        context, sku,
                        pendingCandidateTraceSnapshot.withActualHit(machineCode));
                state.markMachinePriorityTraceHit(sku);
                pendingCandidateTraceSnapshot = null;
                // 单控只保留硬准入、L/R粒度、产能及首检折半，不再覆盖统一八层软排序。
                candidateCache.clearCapacityCache();
                scheduledCount++;
                actualAllowedAddMachineCount++;
                scheduled = true;
                finalMachine = candidateMachine;
                finalProductionStartTime = firstProductionStartTime;
                lastScheduledResult = result;
                lastScheduledSegment = segment;
                // 累计本机台实际排产量，递减多机台剩余量；剩余量仍需结合 dayN 账本判断是否继续加机台。
                totalScheduledQty += machineScheduledQty;
                // businessTargetQty 保留进入本 SKU 时的业务目标，避免 dayN 节奏量被当前机台吃完后提前退出。
                int businessTargetQty = Objects.nonNull(baseTargetScheduleQty)
                        ? Math.max(0, baseTargetScheduleQty) : dynamicTargetQty;
                /*
                 * dayN 模拟已确认当前有效机台数满足节奏时，停止结论必须直接控制主循环。
                 * 剩余业务目标由后续滚动窗口承接，普通空闲机台和续作释放尾部机台都不得再次打开。
                 * 同时按统一Map目标总机台数硬上限停止：历史欠产只影响目标量/账本，不突破总机台数。
                 */
                boolean dayNMachineCountCapReached = isNewSpecDayNMachineCountCapReached(
                        context, sku, dayContext.getScheduleDate());
                boolean continueAddMachineBeforeRemaining = needMoreMachine(context, sku)
                        && !segment.isStopAfterCurrentForSmallShortage()
                        && !dayNMachineCountCapReached;
                if (segment.isStopAfterCurrentForSmallShortage()) {
                    // 小额欠产允许滚动时，当前机台已覆盖后续日计划，不再为了首日欠产余额继续扩机。
                    dynamicTargetQty = totalScheduledQty;
                    appendNewSpecDailyRhythmStopProcessLog(context, sku, machineCode,
                            businessTargetQty, totalScheduledQty,
                            "当前有效机台数已满足当前日优先dayN节奏");
                } else if (dayNMachineCountCapReached) {
                    // 达到 dayN 理论机台数上限：剩余目标量交由未排/下一滚动窗口承接，不再打开新增机台。
                    appendNewSpecDailyRhythmStopProcessLog(context, sku, machineCode,
                            businessTargetQty, totalScheduledQty,
                            "已满足dayN理论机台数上限，停止继续扩机");
                }
                if (continueAddMachineBeforeRemaining && dynamicTargetQty < businessTargetQty) {
                    // dayN 判断仍要求扩机时，继续按原业务目标保留下一台机台待排量。
                    dynamicTargetQty = businessTargetQty;
                }
                remainingQty = Math.max(0, dynamicTargetQty - totalScheduledQty);
                sku.setRemainingScheduleQty(remainingQty);
                finalTargetScheduleQty = dynamicTargetQty;
                /*
                 * 小批量待排计数依赖日计划账本剩余额度，当前机台扣账后需要刷新，
                 * 保证同一SKU继续拆机台时单控保护规则读取的是最新待排视图。
                 */
                refreshPendingNewSpecSkuTypeCounts(context);
                /*
                 * 普通换模/换活字块已经形成真实首检区间时，日志必须输出区间起点；
                 * inspectionTime 是首检计数资源的归属时刻，通常等于切换结束点，不能再标成
                 * “首检开始”。试制不生成首检条数，仍沿用既有首检资源时刻用于过程排查。
                 */
                Date actualInspectionStartTime = Objects.nonNull(firstInspectionAllocationPlan)
                        && firstInspectionAllocationPlan.isValid()
                        ? firstInspectionAllocationPlan.getInspectionStartTime() : inspectionTime;
                log.debug("新增排产本机台完成, SKU: {}, 机台: {}, 本机台排产量: {}, 累计已排: {}, 剩余: {}, 满班超排: {}, 机台就绪: {}, 换模开始: {}, 换模结束: {}, 首检开始: {}, 开产时间: {}",
                        sku.getMaterialCode(), machineCode, machineScheduledQty, totalScheduledQty, remainingQty,
                        sku.getShiftFillOverQty(),
                        LhScheduleTimeUtil.formatDateTime(switchReadyTime),
                        LhScheduleTimeUtil.formatDateTime(mouldChangeStartTime),
                        LhScheduleTimeUtil.formatDateTime(mouldChangeCompleteTime),
                        LhScheduleTimeUtil.formatDateTime(actualInspectionStartTime),
                        LhScheduleTimeUtil.formatDateTime(productionStartTime));
                logNewSpecMachinePlanDecision(sku, quantityPolicy, isEnding, singleMachineWindowFill,
                        dynamicTargetQty, maxQtyToWindowEnd, machinePlanQty, machineScheduledQty);
                // 排产前和单台落地后统一使用 dayN 停止标识与机台数上限，避免尾部候选覆盖中心模拟结论。
                boolean continueAddMachineAfterCurrent = needMoreMachine(context, sku)
                        && !segment.isStopAfterCurrentForSmallShortage()
                        && !isNewSpecDayNMachineCountCapReached(
                        context, sku, dayContext.getScheduleDate());
                if (remainingQty <= 0 || !continueAddMachineAfterCurrent) {
                    // 全部排完（总量满足 且 每日额度满足），移出待排队列
                    removeCurrentNewSpecSku(context, iterator, sku);
                    currentSkuRemoved = true;
                    /*
                     * 提前生产可能已经使用一台机台的窗口尾部产能，但按结构及临时前移日计划
                     * 节奏无需继续扩机。此时必须保留“已经提前生产、剩余量回到原计划日期”
                     * 的真实原因，不能在窗口收口时误写成“未命中提前生产候选”。
                     */
                    if (remainingQty > 0
                            && isEarlyProductionPhase(dayContext.getCurrentPhase())) {
                        deferSkuToNextDay(
                                context, dayContext, state, sku,
                                buildEarlyProductionPartialRemainingReason(remainingQty));
                    }
                    if (remainingQty <= 0) {
                        log.info("新增SKU多机台排产全部完成, materialCode: {}, 使用机台数: {}, 总排产量: {}",
                                sku.getMaterialCode(), excludedMachineCodes.size() + 1, totalScheduledQty);
                    } else {
                        log.info("新增SKU日计划额度已满足, materialCode: {}, 使用机台数: {}, 总排产量: {}, "
                                        + "剩余总量: {}, 满班超排: {}",
                                sku.getMaterialCode(), excludedMachineCodes.size() + 1, totalScheduledQty,
                                remainingQty, sku.getShiftFillOverQty());
                    }
                    break;
                }
                // 一台排不完，保留原业务目标量，下一台机台按剩余缺口动态计算本机台计划量。
                // 已成功排产的机台加入排除集合，表示本 SKU 后续拆量不再回头重复尝试同一机台。
                sku.setTargetScheduleQty(baseTargetScheduleQty);
                excludedMachineCodes.add(machineCode);
                candidateCache.removeMachine(machineCode);
                recordExcludedMachineReason(excludedMachineReasonMap, machineCode,
                        "本机台已排产但仍有剩余，继续尝试下一台",
                        machineReadyTime, switchReadyTime, mouldChangeStartTime, mouldChangeCompleteTime,
                        inspectionTime, firstProductionStartTime, maxQtyToWindowEnd, machinePlanQty,
                        machineScheduledQty);
                log.info("新增SKU一台机台未排完，继续尝试下一台, materialCode: {}, 本机台: {}, 已排: {}, 剩余: {}",
                        sku.getMaterialCode(), machineCode, totalScheduledQty, remainingQty);
            }

            sku.setTargetScheduleQty(scheduled ? finalTargetScheduleQty : originalTargetScheduleQty);
            /*
             * 同一物料可能存在多条历史机台指令。前一条成功后若实际复合账本已经归零，
             * 后续指令不能再次消费待排量，明确记为“无余量”后交由正常结束逻辑收口。
             */
            finalizeNoRemainingHistoricalReverseDirectives(context, sku);
            if (scheduled && !currentSkuRemoved && !futureAddMachineDateDeferred
                    && remainingQty > 0 && needMoreMachine(context, sku)) {
                int refillQty = refillScheduledResultAfterAddMachineFailure(
                        context, sku, lastScheduledResult, lastScheduledSegment, shifts, quantityPolicy,
                        remainingQty, isEarlyProductionPhase(dayContext.getCurrentPhase()));
                if (refillQty > 0) {
                    totalScheduledQty += refillQty;
                    remainingQty = Math.max(0, remainingQty - refillQty);
                    sku.setRemainingScheduleQty(remainingQty);
                    if (remainingQty <= 0 || !needMoreMachine(context, sku)) {
                        removeCurrentNewSpecSku(context, iterator, sku);
                        currentSkuRemoved = true;
                        log.info("新增SKU增机台失败后原机台回填已满足目标, materialCode: {}, 总排产量: {}, 剩余: {}",
                                sku.getMaterialCode(), totalScheduledQty, remainingQty);
                    }
                }
            }
            if (scheduled) {
                /*
                 * 同SKU多机台收尾的班次分散/尾量归集已由续作阶段共用胎胚收尾均衡统一处理，
                 * 此处不再执行新增侧后置搬量，避免与均衡结果冲突；原方法逻辑保留供排查。
                 */
                rebuildScheduledMachineCountMap(context, shifts);
            }
            if (!scheduled && futureAddMachineDateDeferred) {
                /*
                 * 理论上首次增机日在未来的候选已在候选构建阶段被拦截；这里保留主循环硬门槛，
                 * 覆盖运行中账本变化后下一台日期后移的场景，不输出“选机失败”误导日志。
                 */
                state.clearPendingMachinePriorityTrace(sku);
                this.deferCurrentDailyCandidateUntilDate(
                        context, iterator, dayContext, state, sku,
                        dailyDeferredReason, nextAddMachineAttemptDate);
                continue;
            }
            if (!scheduled) {
                /*
                 * 当前业务日或阶段未形成结果时，按最近一次真实候选输入构建一次快照并暂存，
                 * 不立即写完整优先级日志。后续业务日成功会清理该快照；三天窗口最终仍未命中时
                 * 只输出一次汇总。此时本 SKU 之后的机台状态尚未落库，占用/收尾时间与试排时点一致。
                 */
                MachinePriorityTraceSnapshot unscheduledTraceSnapshot =
                        buildUnscheduledTraceSnapshot(
                                context, sku, machineMatch,
                                pendingTraceCandidates, pendingTraceSelectedMachine,
                                pendingTraceDayEndTime, pendingPriorityMetricSnapshotMap,
                                candidateAvailabilityPlanMap,
                                dayContext.getDayEndTime());
                /*
                 * 当前业务日没有形成真实候选选择时，不得用“仅日志展示”的空快照覆盖前序业务日
                 * 已经暂存的最后一次真实选机快照；只有从未登记过真实快照时才允许登记空诊断快照，
                 * 保证三天窗口收口日志输出的是 SKU 最后一次真实选机时的候选机台列表。
                 */
                boolean hasRealCandidateSelection = Objects.nonNull(pendingTraceCandidates)
                        || Objects.nonNull(pendingTraceSelectedMachine);
                if (hasRealCandidateSelection
                        || Objects.isNull(state.getPendingMachinePriorityTrace(sku))) {
                    state.rememberPendingMachinePriorityTrace(sku, unscheduledTraceSnapshot);
                }
                // 当前阶段所有候选机台都失败，只登记延期；T+2 仍需保留给同日后续阶段。
                log.warn("新增SKU排产失败, materialCode: {}, 结构: {}, 规格: {}, 目标量: {}, 候选机台数: {}, 排除机台: {}, 原因: {}",
                        sku.getMaterialCode(), sku.getStructureName(), sku.getSpecCode(),
                        sku.resolveTargetScheduleQty(), candidates.size(), excludedMachineCodes,
                        failReason.getDescription());
                traceNewSpecMachineDecision(context, sku, candidates, localSearchSuggestedMachine, null,
                        excludedMachineCodes, excludedMachineReasonMap, failReason, false, null, null);
                String failureReason = StringUtils.isNotEmpty(dailyDeferredReason)
                        ? dailyDeferredReason : resolveScheduleFailureReason(context, sku, failReason);
                deferCurrentDailyCandidate(
                        context, iterator, dayContext, state, sku, failureReason);
                // 多机台尝试但未排部分也记录未排
                if (totalScheduledQty > 0) {
                    log.warn("新增SKU部分成功部分失败, materialCode: {}, 已排: {}, 未排: {}",
                            sku.getMaterialCode(), totalScheduledQty, remainingQty);
                }
            } else {
                if (!currentSkuRemoved && futureAddMachineDateDeferred) {
                    /*
                     * 当前 SKU 已一次性选完本日应加机台数，未来日所需下一台只登记延期，
                     * 不回填当前机台、不继续占用候选，也不在同日队尾重新插队。
                     */
                    this.deferCurrentDailyCandidateUntilDate(
                            context, iterator, dayContext, state, sku,
                            dailyDeferredReason, nextAddMachineAttemptDate);
                } else if (!currentSkuRemoved && remainingQty > 0 && needMoreMachine(context, sku)) {
                    // 即使部分成功（remainingQty > 0 但无更多候选机台），也记录。
                    log.warn("新增SKU多机台排产未全部完成, materialCode: {}, 已排: {}, 剩余: {}, 满班超排: {}, 候选机台已耗尽",
                            sku.getMaterialCode(), totalScheduledQty, remainingQty, sku.getShiftFillOverQty());
                    String remainingReason = "多机台产能不足，剩余" + remainingQty + "未排";
                    // 多机台剩余量同样等待本日全部阶段结束，避免提前生产阶段被跳过。
                    deferCurrentDailyCandidate(
                            context, iterator, dayContext, state, sku, remainingReason);
                } else if (!currentSkuRemoved && remainingQty > 0) {
                    // 总量上仍有剩余（可能来自欠产传导），但日计划额度已满足，移出待排队列
                    log.info("新增SKU日计划额度已满足但总量仍有剩余, materialCode: {}, 已排: {}, 总量剩余: {}, 满班超排: {}",
                            sku.getMaterialCode(), totalScheduledQty, remainingQty, sku.getShiftFillOverQty());
                    removeCurrentNewSpecSku(context, iterator, sku);
                }
                traceNewSpecMachineDecision(context, sku, candidates, localSearchSuggestedMachine, finalMachine,
                        excludedMachineCodes, excludedMachineReasonMap, null, true,
                        PriorityTraceLogHelper.formatDateTime(finalProductionStartTime),
                        lastConfirmedTraceSnapshot);
                if (!CollectionUtils.isEmpty(deferredCompensationSkuList)) {
                    return scheduledCount;
                }
            }
        }
        return scheduledCount;
    }

    /**
     * 构造提前生产部分成功后保留未来计划余量的诊断原因。
     *
     * <p>该原因会随延期任务保留到三天窗口统一收口，确保最终未排记录能够区分
     * “从未命中提前生产”与“已经使用剩余资源但按当前节奏不再扩机”。</p>
     *
     * @param remainingQty 提前生产后仍需保留到原计划日期的数量
     * @return 可直接写入延期任务及最终未排结果的原因
     */
    private String buildEarlyProductionPartialRemainingReason(int remainingQty) {
        return String.format(
                EARLY_PRODUCTION_PARTIAL_REMAINING_REASON_TEMPLATE,
                Math.max(0, remainingQty));
    }

    /**
     * 将当前 SKU 已经在机的绑定机台从本轮新选机候选中排除。
     *
     * <p>绑定机台的连续生产只能由在机延续入口处理：普通绑定在每日第一阶段处理，
     * 提前生产绑定在正常任务完成后的提前阶段处理，并在原结果上追加当天班次。
     * 新选机循环若再次选中同一机台，会把物理连续生产误判为一次新换产，造成重复结果、
     * 重复换模和重复首检。</p>
     *
     * @param state 三天窗口共用日驱动状态
     * @param sku 当前待排 SKU
     * @param candidates 当前 SKU 的完整候选机台
     * @param excludedMachineCodes 本轮已排除机台编码
     * @param excludedMachineReasonMap 本轮机台排除原因
     */
    private void excludeBoundMachinesFromNewSelection(
            DayDrivenScheduleState state,
            SkuScheduleDTO sku,
            List<MachineScheduleDTO> candidates,
            Set<String> excludedMachineCodes,
            Map<String, String> excludedMachineReasonMap) {
        Set<String> boundMachineCodeSet = state.getBoundMachineCodesBySku(sku);
        if (CollectionUtils.isEmpty(boundMachineCodeSet)) {
            return;
        }
        for (String machineCode : boundMachineCodeSet) {
            if (!containsMachine(candidates, machineCode)) {
                continue;
            }
            excludedMachineCodes.add(machineCode);
            recordExcludedMachineReason(
                    excludedMachineReasonMap, machineCode,
                    "已上机绑定机台仅允许原结果连续生产，不参与当前阶段新选机",
                    null, null, null, null, null, null, null, null, null);
        }
        log.info("新增SKU新选机排除已绑定机台, materialCode: {}, productStatus: {}, "
                        + "boundMachineCodes: {}",
                sku.getMaterialCode(), sku.getProductStatus(), boundMachineCodeSet);
    }

    /**
     * 准备新增排产欠产账本。
     * <p>委托公共增机台协作器处理本月欠产入账和窗口无计划判断，保证 S4.4 续作补偿与 S4.5 新增排产口径一致。</p>
     *
     * @param context 排程上下文
     * @param sku 新增排产SKU
     * @return true-当前窗口和月底均无计划，需要按收尾处理
     */
    private boolean prepareNewSpecShortageQuota(LhScheduleContext context, SkuScheduleDTO sku) {
        return DailyMachineExpansionPlanner.prepareShortageQuota(context, sku, "新增排产")
                .isForceEndingByNoFuturePlan();
    }

    /**
     * 判断接口目标日前一日排程结果中该SKU是否已有有效排产量。
     *
     * @param context 排程上下文
     * @param materialCode 物料编码
     * @return true-接口目标日前一日排程结果已排过；false-未排过
     */
    private boolean hasPreviousScheduledResult(LhScheduleContext context, String materialCode) {
        if (Objects.isNull(context) || StringUtils.isEmpty(materialCode)
                || CollectionUtils.isEmpty(context.getTargetPreviousScheduleResultList())) {
            return false;
        }
        for (LhScheduleResult result : context.getTargetPreviousScheduleResultList()) {
            if (Objects.nonNull(result)
                    && StringUtils.equals(materialCode, result.getMaterialCode())
                    && resolveResultScheduledQty(result) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 新增SKU抢占首日无计划释放机台时，撤销原续作占位结果并转补偿SKU到后续新增轮次。
     * <p>这类续作结果只是“若机台未被占用时的回退占位”，一旦本轮新增真正占机，就必须把占位结果撤销，
     * 否则最终结果会同时保留“原续作继续在原机台”与“新增SKU换模占用原机台”两条互斥结果。</p>
     *
     * @param context 排程上下文
     * @param machineCode 当前被新增SKU占用的机台编码
     * @param shifts 排程窗口班次
     * @param deferredCompensationSkuList 延后到下一轮补排的补偿SKU集合
     */
    private void takeoverReleasedContinuousPlaceholderIfNeeded(LhScheduleContext context,
                                                               String machineCode,
                                                               List<LhShiftConfigVO> shifts,
                                                               List<SkuScheduleDTO> deferredCompensationSkuList) {
        if (context == null || StringUtils.isEmpty(machineCode)
                || CollectionUtils.isEmpty(context.getFirstDayNoPlanReleasedContinuousMachineCodeSet())
                || !context.getFirstDayNoPlanReleasedContinuousMachineCodeSet().contains(machineCode)) {
            return;
        }
        List<LhScheduleResult> machineResults = resolveMachineResultsForPlaceholderTakeover(context, machineCode);
        if (CollectionUtils.isEmpty(machineResults)) {
            return;
        }
        List<LhScheduleResult> placeholderResults = new ArrayList<LhScheduleResult>(2);
        for (LhScheduleResult result : machineResults) {
            if (result == null || !StringUtils.equals(machineCode, result.getLhMachineCode())) {
                continue;
            }
            if (isReleasedFirstDayNoPlanPlaceholderResult(context, result)) {
                placeholderResults.add(result);
            }
        }
        if (CollectionUtils.isEmpty(placeholderResults)) {
            return;
        }
        for (LhScheduleResult placeholderResult : placeholderResults) {
            SkuScheduleDTO sourceSku = resolveResultSourceSku(context, placeholderResult);
            if (sourceSku == null) {
                continue;
            }
            restoreContinuousPlaceholderQuota(context, sourceSku);
            getTargetScheduleQtyResolver().restoreProductionRemainingQty(
                    context, sourceSku, resolveResultScheduledQty(placeholderResult),
                    "首日无计划续作占位撤销", placeholderResult.getLhMachineCode());
            appendDeferredContinuousCompensationSku(
                    context, sourceSku, placeholderResult, deferredCompensationSkuList);
            context.getScheduleResultSourceSkuMap().remove(placeholderResult);
            context.getScheduleResultList().remove(placeholderResult);
        }
        removeResultsFromMachineAssignments(context, placeholderResults);
    }

    /**
     * 解析释放续作占位结果所在机台的结果集合。
     * <p>正常主链优先按机台占用索引查找；若索引中未命中占位结果，
     * 继续按结果列表补充扫描当前机台，保持与旧版全量扫描语义一致。</p>
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @return 当前机台结果集合
     */
    private List<LhScheduleResult> resolveMachineResultsForPlaceholderTakeover(LhScheduleContext context,
                                                                               String machineCode) {
        if (context == null || StringUtils.isEmpty(machineCode)) {
            return new ArrayList<LhScheduleResult>(0);
        }
        List<LhScheduleResult> machineResults = null;
        if (!CollectionUtils.isEmpty(context.getMachineAssignmentMap())) {
            machineResults = context.getMachineAssignmentMap().get(machineCode);
        }
        if (hasReleasedPlaceholderResult(context, machineResults, machineCode)) {
            return machineResults;
        }
        List<LhScheduleResult> scannedResults = new ArrayList<LhScheduleResult>(2);
        if (!CollectionUtils.isEmpty(machineResults)) {
            scannedResults.addAll(machineResults);
        }
        if (CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return scannedResults;
        }
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (result != null && StringUtils.equals(machineCode, result.getLhMachineCode())) {
                addMachineResultIfAbsent(scannedResults, result);
            }
        }
        return scannedResults;
    }

    /**
     * 判断机台结果集合中是否包含释放续作占位结果。
     *
     * @param context 排程上下文
     * @param machineResults 机台结果集合
     * @param machineCode 机台编码
     * @return true-包含释放续作占位结果
     */
    private boolean hasReleasedPlaceholderResult(LhScheduleContext context,
                                                 List<LhScheduleResult> machineResults,
                                                 String machineCode) {
        if (CollectionUtils.isEmpty(machineResults)) {
            return false;
        }
        for (LhScheduleResult result : machineResults) {
            if (result != null
                    && StringUtils.equals(machineCode, result.getLhMachineCode())
                    && isReleasedFirstDayNoPlanPlaceholderResult(context, result)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按对象引用去重追加机台结果，避免 Map 与结果列表命中同一结果时重复处理。
     *
     * @param machineResults 机台结果集合
     * @param result 待追加结果
     */
    private void addMachineResultIfAbsent(List<LhScheduleResult> machineResults, LhScheduleResult result) {
        if (result == null) {
            return;
        }
        for (LhScheduleResult existsResult : machineResults) {
            if (existsResult == result) {
                return;
            }
        }
        machineResults.add(result);
    }

    /**
     * 恢复首日无计划续作占位结果提前消费的共享日计划账本。
     * <p>占位结果被新增SKU抢占后，相应额度必须先回滚，再交给补偿SKU重新按新增换模链路消费。</p>
     *
     * @param sourceSku 原续作SKU
     */
    private void restoreContinuousPlaceholderQuota(LhScheduleContext context, SkuScheduleDTO sourceSku) {
        if (sourceSku == null || CollectionUtils.isEmpty(sourceSku.getDailyPlanQuotaMap())) {
            return;
        }
        for (SkuDailyPlanQuotaDTO quota : sourceSku.getDailyPlanQuotaMap().values()) {
            if (quota == null) {
                continue;
            }
            quota.setScheduledQty(0);
            quota.setRemainingQty(Math.max(0, quota.getDayPlanQty()));
            quota.setShiftFillOverQty(0);
            quota.setCarryLossQty(0);
            quota.setFutureBorrowQty(0);
            quota.setActualQty(0);
            quota.setCumulativeQty(0);
            quota.setFinalLossQty(0);
            quota.setCompleted(false);
        }
        SkuDailyPlanQuotaUtil.refreshRollingFields(sourceSku.getDailyPlanQuotaMap());
        DailyMachineExpansionPlanner.syncSharedQuotaEffectiveCarryForwardQty(context, sourceSku, 0);
    }

    /**
     * 为被抢占机台的原续作SKU生成补偿SKU，留待下一轮新增链路重新选机。
     *
     * @param context 排程上下文
     * @param sourceSku 原续作SKU
     * @param placeholderResult 被撤销的占位结果
     * @param deferredCompensationSkuList 延后补排集合
     */
    private void appendDeferredContinuousCompensationSku(LhScheduleContext context,
                                                         SkuScheduleDTO sourceSku,
                                                         LhScheduleResult placeholderResult,
                                                         List<SkuScheduleDTO> deferredCompensationSkuList) {
        if (sourceSku == null || placeholderResult == null || deferredCompensationSkuList == null) {
            return;
        }
        if (hasDeferredContinuousCompensationSku(context, sourceSku, deferredCompensationSkuList)) {
            return;
        }
        int compensationQty = placeholderResult.getDailyPlanQty() != null
                ? placeholderResult.getDailyPlanQty() : 0;
        if (compensationQty <= 0) {
            compensationQty = Math.max(0, sourceSku.resolveTargetScheduleQty());
        }
        if (compensationQty <= 0) {
            return;
        }
        SkuScheduleDTO compensationSku = new SkuScheduleDTO();
        BeanUtil.copyProperties(sourceSku, compensationSku);
        ProductionQuantityPolicy policy = ProductionQuantityPolicy.from(sourceSku, sourceSku.isStrictTargetQty());
        compensationSku.setScheduleType(NEW_SPEC_SCHEDULE_TYPE);
        compensationSku.setContinuousMachineCode(null);
        compensationSku.setPreferredContinuousMachineCode(sourceSku.getContinuousMachineCode());
        compensationSku.setContinuousCompensationSku(true);
        compensationSku.setTargetScheduleQty(compensationQty);
        compensationSku.setPendingQty(compensationQty);
        compensationSku.setRemainingScheduleQty(compensationQty);
        compensationSku.setStrictTargetQty(policy.isStrictUpperLimit());
        compensationSku.setDailyPlanQuotaMap(sourceSku.getDailyPlanQuotaMap());
        deferredCompensationSkuList.add(compensationSku);
    }

    private boolean hasDeferredContinuousCompensationSku(LhScheduleContext context,
                                                         SkuScheduleDTO sourceSku,
                                                         List<SkuScheduleDTO> deferredCompensationSkuList) {
        if (sourceSku == null) {
            return true;
        }
        if (!CollectionUtils.isEmpty(context.getNewSpecSkuList())) {
            for (SkuScheduleDTO pendingSku : context.getNewSpecSkuList()) {
                if (pendingSku != null
                        && StringUtils.equals(sourceSku.getMaterialCode(), pendingSku.getMaterialCode())
                        && StringUtils.equals(StringUtils.trimToEmpty(sourceSku.getProductStatus()),
                        StringUtils.trimToEmpty(pendingSku.getProductStatus()))
                        && pendingSku.getDailyPlanQuotaMap() == sourceSku.getDailyPlanQuotaMap()) {
                    return true;
                }
            }
        }
        if (!CollectionUtils.isEmpty(deferredCompensationSkuList)) {
            for (SkuScheduleDTO pendingSku : deferredCompensationSkuList) {
                if (pendingSku != null
                        && StringUtils.equals(sourceSku.getMaterialCode(), pendingSku.getMaterialCode())
                        && StringUtils.equals(StringUtils.trimToEmpty(sourceSku.getProductStatus()),
                        StringUtils.trimToEmpty(pendingSku.getProductStatus()))
                        && pendingSku.getDailyPlanQuotaMap() == sourceSku.getDailyPlanQuotaMap()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 将补偿 SKU 按 S4.5 全局顺序合并到下一轮待排列表，并刷新待排 SKU 类型计数。
     *
     * <p>已经在本轮完成的资源分配不回滚；补偿 SKU 只与尚未执行的候选竞争。有有效
     * sortRank 时按名次稳定插入，无名次时排在全部有名次候选之后。</p>
     *
     * @param context 排程上下文
     * @param state 三天窗口共用日驱动状态
     * @param deferredCompensationSkuList 延后补排集合
     */
    private void appendDeferredCompensationSkuList(LhScheduleContext context,
                                                   DayDrivenScheduleState state,
                                                   List<SkuScheduleDTO> deferredCompensationSkuList) {
        if (Objects.isNull(context) || Objects.isNull(state)
                || CollectionUtils.isEmpty(deferredCompensationSkuList)) {
            return;
        }
        state.mergePendingSkuListByGlobalOrder(
                context.getNewSpecSkuList(), deferredCompensationSkuList);
        refreshPendingNewSpecSkuTypeCounts(context);
    }

    /**
     * 判断结果是否属于“首日无计划但后续有计划”的释放续作占位结果。
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @return true-释放续作占位结果
     */
    private boolean isReleasedFirstDayNoPlanPlaceholderResult(LhScheduleContext context, LhScheduleResult result) {
        if (context == null || result == null
                || !StringUtils.equals("01", result.getScheduleType())
                || !StringUtils.equals("0", result.getIsTypeBlock())
                || StringUtils.isEmpty(result.getLhMachineCode())
                || CollectionUtils.isEmpty(context.getFirstDayNoPlanReleasedContinuousMachineCodeSet())) {
            return false;
        }
        return context.getFirstDayNoPlanReleasedContinuousMachineCodeSet().contains(result.getLhMachineCode());
    }

    /**
     * 解析排程结果对应的来源SKU。
     * <p>优先命中运行态映射；未注册时回退到物料编码查找，兼容旧测试夹具。</p>
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @return 来源SKU
     */
    private SkuScheduleDTO resolveResultSourceSku(LhScheduleContext context, LhScheduleResult result) {
        if (context == null || result == null) {
            return null;
        }
        SkuScheduleDTO sourceSku = context.getScheduleResultSourceSkuMap().get(result);
        if (sourceSku != null) {
            return sourceSku;
        }
        return findSkuDto(context, result.getMaterialCode(), result.getProductStatus());
    }

    /**
     * 初始化新增待排SKU类型计数，供选机阶段日志和规则排查复用。
     *
     * @param context 排程上下文
     */
    private void initializePendingNewSpecSkuTypeCounts(LhScheduleContext context) {
        refreshPendingNewSpecSkuTypeCounts(context);
        if (context == null) {
            return;
        }
        log.info("新增待排SKU类型计数初始化, 试制SKU: {}, 量试SKU: {}, 小批量SKU: {}, 正规SKU: {}",
                context.getPendingTrialNewSpecSkuCount(),
                context.getPendingMassTrialNewSpecSkuCount(),
                context.getPendingSmallBatchNewSpecSkuCount(),
                context.getPendingFormalNewSpecSkuCount());
    }

    /**
     * 刷新新增待排SKU类型计数。
     * <p>小批量已并入正规组排序；这里仍保留独立计数，只用于日志和规则排查。</p>
     *
     * @param context 排程上下文
     */
    private void refreshPendingNewSpecSkuTypeCounts(LhScheduleContext context) {
        if (context == null) {
            return;
        }
        int formalCount = 0;
        int trialCount = 0;
        int massTrialCount = 0;
        int smallBatchCount = 0;
        for (SkuScheduleDTO pendingSku : context.getNewSpecSkuList()) {
            if (isTrialConstructionStage(pendingSku)) {
                trialCount++;
                continue;
            }
            if (isMassTrialSku(pendingSku)) {
                massTrialCount++;
                continue;
            }
            if (isSmallBatchSku(pendingSku)) {
                if (hasPendingWindowQuotaSmallBatchDemand(pendingSku)) {
                    smallBatchCount++;
                }
                continue;
            }
            formalCount++;
        }
        context.setPendingFormalNewSpecSkuCount(formalCount);
        context.setPendingTrialNewSpecSkuCount(trialCount);
        context.setPendingMassTrialNewSpecSkuCount(massTrialCount);
        context.setPendingSmallBatchNewSpecSkuCount(smallBatchCount);
    }

    /**
     * 判断小批量SKU在当前窗口内是否仍有待排日计划额度。
     * <p>只要窗口内 dayN 全为0，即使SKU类型上属于小批量，也不再计入待排小批量统计。</p>
     *
     * @param sku SKU
     * @return true-窗口内仍有待排额度
     */
    private boolean hasPendingWindowQuotaSmallBatchDemand(SkuScheduleDTO sku) {
        if (!isSmallBatchSku(sku) || CollectionUtils.isEmpty(sku.getDailyPlanQuotaMap())) {
            return false;
        }
        for (SkuDailyPlanQuotaDTO quota : sku.getDailyPlanQuotaMap().values()) {
            if (quota == null) {
                continue;
            }
            if (Math.max(0, quota.getDayPlanQty()) > 0 && Math.max(0, quota.getRemainingQty()) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断最近一次选机是否被SKU类型机台约束清空候选。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @return true-被类型规则清空
     */
    private boolean isTypeRuleBlocked(LhScheduleContext context, SkuScheduleDTO sku) {
        return context != null
                && sku != null
                && Boolean.TRUE.equals(context.getNewSpecTypeRuleBlockedMap().get(sku));
    }

    /**
     * 处理共用胎胚收尾零余量未排。
     * <p>该分支必须在候选机台匹配前完成，避免目标量为0的SKU继续走通用失败链路并覆盖业务未排原因。</p>
     *
     * @param context 排程上下文
     * @param iterator 新增SKU迭代器
     * @param sku 当前SKU
     * @param sharedEmbryoZeroSurplusEnding 是否命中共用胎胚零余量收尾
     * @param unscheduledReasonCountMap 未排原因统计
     * @return true-已写未排并移出待排队列；false-不需要处理
     */
    private boolean handleSharedEmbryoZeroSurplusEndingIfNecessary(LhScheduleContext context,
                                                                   Iterator<SkuScheduleDTO> iterator,
                                                                   SkuScheduleDTO sku,
                                                                   boolean sharedEmbryoZeroSurplusEnding,
                                                                   Map<String, Integer> unscheduledReasonCountMap) {
        if (!sharedEmbryoZeroSurplusEnding || sku.resolveTargetScheduleQty() > 0) {
            return false;
        }
        addUnscheduledResult(context, sku, 0,
                SHARED_EMBRYO_ZERO_SURPLUS_UNSCHEDULED_REASON, unscheduledReasonCountMap);
        getTargetScheduleQtyResolver().removeActiveEmbryoSku(
                context, sku, SHARED_EMBRYO_ZERO_SURPLUS_UNSCHEDULED_REASON);
        removeCurrentNewSpecSku(context, iterator, sku);
        log.info("新增共用胎胚收尾零余量写入未排, materialCode: {}, embryoCode: {}, surplusQty: {}, embryoStock: {}",
                sku.getMaterialCode(), sku.getEmbryoCode(), sku.getSurplusQty(), sku.getEmbryoStock());
        return true;
    }

    /**
     * 输出新增收尾小余量规则判断日志。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param isEnding 是否收尾
     * @param smallEndingRuleQty 收尾小余量规则本轮使用的有效余量
     * @param skipped 是否跳过排产
     */
    private void traceSmallEndingSurplusJudge(LhScheduleContext context,
                                              SkuScheduleDTO sku,
                                              boolean isEnding,
                                              int smallEndingRuleQty,
                                              boolean skipped) {
        if (Objects.isNull(sku) || !isEnding) {
            return;
        }
        int toleranceQty = SmallEndingSurplusSkipRule.resolveToleranceQty(context);
        int previousNightPlanQty = SmallEndingSurplusSkipRule.resolveTargetPreviousT1NightPlanQty(
                context, sku.getMaterialCode());
        boolean previousNightFull = SmallEndingSurplusSkipRule.isTargetPreviousT1NightFull(context, sku);
        boolean runtimeRemainingQtyApplied =
                EarlyProductionQuantityCalculator.shouldUseRuntimeRemainingQtyForSmallEnding(
                        context, sku);
        String quantitySource = runtimeRemainingQtyApplied
                ? "EARLY_PRODUCTION_RUNTIME_REMAINING" : "GENERIC_SURPLUS";
        StringBuilder detail = new StringBuilder(192);
        detail.append("新增收尾小余量业务目标日前一日夜班判断, materialCode: ")
                .append(sku.getMaterialCode())
                .append(", scheduleTargetDate: ")
                .append(LhScheduleTimeUtil.formatDate(context.getScheduleTargetDate()))
                .append(", genericSurplusQty: ")
                .append(Math.max(0, sku.getSurplusQty()))
                .append(", smallEndingRuleQty: ")
                .append(Math.max(0, smallEndingRuleQty))
                .append(", quantitySource: ")
                .append(quantitySource)
                .append(", monthlyHistoryShortageQty: ")
                .append(Math.max(0, sku.getMonthlyHistoryShortageQty()))
                .append(", futurePlanQtyAfterWindow: ")
                .append(Math.max(0, sku.getFutureMonthPlanQtyAfterWindow()))
                .append(", toleranceQty: ")
                .append(toleranceQty)
                .append(", targetPreviousT1NightPlanQty: ")
                .append(previousNightPlanQty)
                .append(", shiftCapacity: ")
                .append(sku.getShiftCapacity())
                .append(", targetPreviousT1NightFull: ")
                .append(previousNightFull)
                .append(", skipSchedule: ")
                .append(skipped);
        if (skipped) {
            detail.append(", unscheduledReason: ").append(SMALL_ENDING_SURPLUS_UNSCHEDULED_REASON);
        }
        log.info(detail.toString());
        PriorityTraceLogHelper.appendProcessLog(context, "新增收尾小余量不排产", detail.toString());
    }

    /**
     * 移除当前新增待排SKU，并同步刷新类型计数。
     * <p>当前SKU排产前可能追加历史欠产并改变日计划账本，出队后使用全量刷新，
     * 避免按出队时可变状态做增量扣减导致单控保护计数偏移。</p>
     *
     * @param context 排程上下文
     * @param iterator 新增SKU迭代器
     * @param sku 当前SKU
     */
    private void removeCurrentNewSpecSku(LhScheduleContext context,
                                         Iterator<SkuScheduleDTO> iterator,
                                         SkuScheduleDTO sku) {
        iterator.remove();
        // 新增SKU出队时同步维护结构待排视图，供结构最低机台数规则准确识别“全部SKU已处理完成”。
        context.removePendingSkuFromStructureMap(sku);
        context.getNewSpecTypeRuleBlockedMap().remove(sku);
        context.getNewSpecEarlyProductionAllowedMap().remove(sku);
        refreshPendingNewSpecSkuTypeCounts(context);
    }

    /**
     * 将当前SKU尚未尝试的历史指定机台提到候选列表头部。
     *
     * <p>普通候选机台仍由原机台匹配策略完整过滤和排序。本方法只调用指定机台硬过滤入口，
     * 把通过约束的历史机台按历史指令顺序前置；不重新计算或改变其他候选的相对顺序。</p>
     *
     * @param context 排程上下文
     * @param sku 当前新增SKU
     * @param normalCandidates 普通选机有序候选
     * @param machineMatch 机台匹配策略
     * @return 指定机台在前、普通候选原顺序在后的候选列表
     */
    private List<MachineScheduleDTO> prioritizeHistoricalReverseSpecifiedMachines(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            List<MachineScheduleDTO> normalCandidates,
            IMachineMatchStrategy machineMatch) {
        List<HistoricalReverseSelectionDirective> directives =
                context.getHistoricalReverseSelectionDirectiveList();
        if (CollectionUtils.isEmpty(directives)) {
            return normalCandidates;
        }
        List<MachineScheduleDTO> specifiedCandidates =
                new ArrayList<MachineScheduleDTO>(directives.size());
        Set<String> specifiedMachineCodeSet = new LinkedHashSet<String>(directives.size());
        for (HistoricalReverseSelectionDirective directive : directives) {
            if (!isPendingHistoricalReverseDirectiveForSku(directive, sku)) {
                continue;
            }
            SpecifiedMachineMatchResult matchResult = machineMatch.matchSpecifiedMachine(
                    context, sku, directive.getMachineCode());
            if (!matchResult.isSuccess()) {
                markHistoricalReverseDirectiveFailed(
                        context, directive, matchResult.getFailureReason());
                continue;
            }
            MachineScheduleDTO specifiedMachine = matchResult.getMachine();
            directive.setEffectiveMachineCode(specifiedMachine.getMachineCode());
            if (specifiedMachineCodeSet.add(specifiedMachine.getMachineCode())) {
                specifiedCandidates.add(specifiedMachine);
            }
        }
        if (CollectionUtils.isEmpty(specifiedCandidates)) {
            return normalCandidates;
        }
        int normalSize = CollectionUtils.isEmpty(normalCandidates) ? 0 : normalCandidates.size();
        List<MachineScheduleDTO> prioritized =
                new ArrayList<MachineScheduleDTO>(specifiedCandidates.size() + normalSize);
        prioritized.addAll(specifiedCandidates);
        if (!CollectionUtils.isEmpty(normalCandidates)) {
            for (MachineScheduleDTO candidate : normalCandidates) {
                if (Objects.nonNull(candidate)
                        && !specifiedMachineCodeSet.contains(candidate.getMachineCode())) {
                    prioritized.add(candidate);
                }
            }
        }
        return prioritized;
    }

    /**
     * 将特殊材料置换排产候选限制为预演确认的续作机台。
     *
     * <p>普通 S4.5 新增排产没有特殊材料指定机台指令时直接返回原候选列表。命中指令后重新调用
     * {@link IMachineMatchStrategy#matchSpecifiedMachine(LhScheduleContext, SkuScheduleDTO, String)}
     * 执行定点、模具、特殊材料、单控等硬约束校验；校验失败返回空列表，禁止回落其他机台。</p>
     *
     * @param context 排程上下文
     * @param sku 当前待排 SKU
     * @param candidates 普通新增和历史反选合并后的候选列表
     * @param machineMatch 机台匹配策略
     * @return 特殊材料置换模式下仅包含指定机台；普通模式返回原列表
     */
    private List<MachineScheduleDTO> restrictSubstitutionCandidates(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            List<MachineScheduleDTO> candidates,
            IMachineMatchStrategy machineMatch) {
        if (Objects.isNull(context) || Objects.isNull(sku)
                || (!context.isSpecialMaterialSpecifiedSku(sku)
                && !context.isScheduleSubstitutionSku(sku))) {
            return candidates;
        }
        List<MachineScheduleDTO> allowedCandidates =
                CollectionUtils.isEmpty(candidates)
                        ? new ArrayList<MachineScheduleDTO>(0)
                        : new ArrayList<MachineScheduleDTO>(candidates);
        if (context.isScheduleSubstitutionSku(sku)
                && Objects.nonNull(context.getScheduleSubstitutionDirective())
                && !CollectionUtils.isEmpty(
                context.getScheduleSubstitutionDirective().getExcludedMachineCodeSet())) {
            // B 迁移预演不得重新选回 A 已经接管的原物理机台或其单控配对侧。
            allowedCandidates.removeIf(machine -> Objects.isNull(machine)
                    || context.getScheduleSubstitutionDirective().getExcludedMachineCodeSet()
                    .contains(machine.getMachineCode()));
        }
        String specifiedMachineCode =
                context.resolveSubstitutionSpecifiedMachineCode(sku);
        if (StringUtils.isEmpty(specifiedMachineCode)) {
            // B 首次迁移预演不锁定新机台，继续使用经过原机台排除后的既有有序候选。
            return allowedCandidates;
        }
        SpecifiedMachineMatchResult matchResult =
                machineMatch.matchSpecifiedMachine(context, sku, specifiedMachineCode);
        if (!matchResult.isSuccess() || Objects.isNull(matchResult.getMachine())) {
            log.info("置换指定机台硬约束校验失败, materialCode: {}, productStatus: {}, "
                            + "machineCode: {}, reason: {}",
                    sku.getMaterialCode(), sku.getProductStatus(), specifiedMachineCode,
                    matchResult.getFailureReason());
            return Collections.emptyList();
        }
        log.info("置换指定机台进入新增主链, materialCode: {}, productStatus: {}, "
                        + "machineCode: {}, 原普通候选数: {}",
                sku.getMaterialCode(), sku.getProductStatus(), specifiedMachineCode,
                allowedCandidates.size());
        return Collections.singletonList(matchResult.getMachine());
    }

    /**
     * 对齐特殊材料置换允许的最早换模时间。
     *
     * @param context 排程上下文
     * @param sku 当前待排 SKU
     * @param machineCode 当前候选机台编码
     * @param switchReadyTime 现有规则计算的机台换模就绪时间
     * @return 不早于特殊材料月计划准入和换模均衡预演结果的最终就绪时间
     */
    private Date alignSpecialMaterialSubstitutionSwitchReadyTime(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            String machineCode,
            Date switchReadyTime) {
        if (Objects.isNull(context) || Objects.isNull(sku)
                || (!context.isSpecialMaterialSpecifiedSku(sku)
                && !context.isScheduleSubstitutionSku(sku))
                || Objects.isNull(context.resolveSubstitutionEarliestSwitchTime(sku))) {
            return switchReadyTime;
        }
        String specifiedMachineCode = context.resolveSubstitutionSpecifiedMachineCode(sku);
        if (StringUtils.isNotEmpty(specifiedMachineCode)
                && !StringUtils.equals(machineCode, specifiedMachineCode)) {
            return switchReadyTime;
        }
        Date earliestSwitchTime = context.resolveSubstitutionEarliestSwitchTime(sku);
        if (Objects.isNull(switchReadyTime) || earliestSwitchTime.after(switchReadyTime)) {
            log.info("置换切换时间按月计划准入或 B 下机时间顺延, materialCode: {}, productStatus: {}, "
                            + "machineCode: {}, 原就绪时间: {}, 最早允许时间: {}",
                    sku.getMaterialCode(), sku.getProductStatus(), machineCode,
                    LhScheduleTimeUtil.formatDateTime(switchReadyTime),
                    LhScheduleTimeUtil.formatDateTime(earliestSwitchTime));
            return earliestSwitchTime;
        }
        return switchReadyTime;
    }

    /**
     * 查找下一条尚未尝试且候选机台可用的历史指令。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param candidates 当前候选机台
     * @param excludedMachineCodes 当前已排除机台
     * @return 下一条指定机台指令；没有返回null
     */
    private HistoricalReverseSelectionDirective findNextHistoricalReverseDirective(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            List<MachineScheduleDTO> candidates,
            Set<String> excludedMachineCodes) {
        for (HistoricalReverseSelectionDirective directive
                : context.getHistoricalReverseSelectionDirectiveList()) {
            if (!isPendingHistoricalReverseDirectiveForSku(directive, sku)
                    || StringUtils.isEmpty(directive.getEffectiveMachineCode())
                    || excludedMachineCodes.contains(directive.getEffectiveMachineCode())
                    || !containsMachine(candidates, directive.getEffectiveMachineCode())) {
                continue;
            }
            return directive;
        }
        return null;
    }

    /**
     * 判断当前SKU是否仍有历史正规换模指定机台待尝试。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @return true-当前轮次仍需执行历史指定机台；false-仅剩普通新增
     */
    private boolean hasPendingHistoricalReverseDirectiveForSku(
            LhScheduleContext context,
            SkuScheduleDTO sku) {
        if (CollectionUtils.isEmpty(context.getHistoricalReverseSelectionDirectiveList())) {
            return false;
        }
        for (HistoricalReverseSelectionDirective directive
                : context.getHistoricalReverseSelectionDirectiveList()) {
            if (isPendingHistoricalReverseDirectiveForSku(directive, sku)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按当前SKU和实际候选机台查找反选指令。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param machineCode 实际候选机台编码
     * @param includeAttempted 是否允许返回已完成指令
     * @return 对应指令；没有返回null
     */
    private HistoricalReverseSelectionDirective findHistoricalReverseDirective(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            String machineCode,
            boolean includeAttempted) {
        for (HistoricalReverseSelectionDirective directive
                : context.getHistoricalReverseSelectionDirectiveList()) {
            if ((!includeAttempted && directive.isAttempted())
                    || !isSameHistoricalReverseSku(directive, sku)
                    || !StringUtils.equals(machineCode, directive.getEffectiveMachineCode())) {
                continue;
            }
            return directive;
        }
        return null;
    }

    /**
     * 结算上一轮已经被新增主链排除的指定机台指令。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param excludedMachineCodes 已排除机台
     * @param excludedMachineReasonMap 机台失败原因
     */
    private void finalizeRejectedHistoricalReverseDirectives(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            Set<String> excludedMachineCodes,
            Map<String, String> excludedMachineReasonMap) {
        if (CollectionUtils.isEmpty(excludedMachineCodes)) {
            return;
        }
        for (HistoricalReverseSelectionDirective directive
                : context.getHistoricalReverseSelectionDirectiveList()) {
            if (!isPendingHistoricalReverseDirectiveForSku(directive, sku)
                    || StringUtils.isEmpty(directive.getEffectiveMachineCode())
                    || !excludedMachineCodes.contains(directive.getEffectiveMachineCode())) {
                continue;
            }
            String reason = excludedMachineReasonMap.get(directive.getEffectiveMachineCode());
            markHistoricalReverseDirectiveFailed(context, directive,
                    StringUtils.isNotEmpty(reason) ? reason : "指定机台未通过新增排产主链约束");
        }
    }

    /**
     * 结算未进入真实可选候选池的历史指定机台。
     *
     * <p>历史指定机台已在候选构建阶段通过基础硬过滤并写入有效机台编码，因此在未被候选失败集合
     * 排除的前提下，未进入 {@code selectableCandidates} 只可能是正式窗口剩余产能不大于0。
     * 该状态覆盖整个排程窗口，继续下一轮不会产生新产能，必须在当前轮明确失败。</p>
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param selectableCandidates 当前真实可选候选
     */
    private void finalizeUnselectableHistoricalReverseDirectives(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            List<MachineScheduleDTO> selectableCandidates) {
        for (HistoricalReverseSelectionDirective directive
                : context.getHistoricalReverseSelectionDirectiveList()) {
            if (!isPendingHistoricalReverseDirectiveForSku(directive, sku)
                    || StringUtils.isEmpty(directive.getEffectiveMachineCode())
                    || containsMachine(selectableCandidates, directive.getEffectiveMachineCode())) {
                continue;
            }
            markHistoricalReverseDirectiveFailed(
                    context, directive, HISTORICAL_REVERSE_NO_WINDOW_CAPACITY_REASON);
            log.info("历史反选指定机台整窗无产能，当前业务日立即释放普通回落, batchNo: {}, "
                            + "scheduleDate: {}, materialCode: {}, productStatus: {}, machineCode: {}, reason: {}",
                    context.getBatchNo(), context.getCurrentScheduleDate(),
                    sku.getMaterialCode(), sku.getProductStatus(),
                    directive.getEffectiveMachineCode(),
                    HISTORICAL_REVERSE_NO_WINDOW_CAPACITY_REASON);
        }
    }

    /**
     * 将当前SKU已无实际余量时尚未尝试的历史指令标记失败。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     */
    private void finalizeNoRemainingHistoricalReverseDirectives(
            LhScheduleContext context,
            SkuScheduleDTO sku) {
        if (getTargetScheduleQtyResolver().resolveProductionRemainingQty(context, sku) > 0) {
            return;
        }
        for (HistoricalReverseSelectionDirective directive
                : context.getHistoricalReverseSelectionDirectiveList()) {
            if (isPendingHistoricalReverseDirectiveForSku(directive, sku)) {
                markHistoricalReverseDirectiveFailed(
                        context, directive, "前序反选已消费全部待排量，当前指定机台不再重复排产");
            }
        }
    }

    /**
     * 将指定机台切换就绪时间下限对齐到历史映射班次开始。
     *
     * @param context 排程上下文
     * @param directive 当前反选指令
     * @param switchReadyTime 现有规则计算的切换就绪时间
     * @return 取现有就绪时间和映射班次开始时间的较晚值
     */
    private Date alignHistoricalReverseSwitchReadyTime(
            LhScheduleContext context,
            HistoricalReverseSelectionDirective directive,
            Date switchReadyTime) {
        if (Objects.isNull(directive)) {
            return switchReadyTime;
        }
        LhShiftConfigVO mappedShift = LhScheduleTimeUtil.getShiftByIndex(
                context, context.getScheduleDate(), directive.getMappedShiftIndex());
        if (Objects.isNull(mappedShift) || Objects.isNull(mappedShift.getShiftStartDateTime())) {
            return switchReadyTime;
        }
        if (Objects.isNull(switchReadyTime)
                || switchReadyTime.before(mappedShift.getShiftStartDateTime())) {
            return mappedShift.getShiftStartDateTime();
        }
        return switchReadyTime;
    }

    /**
     * 判断实际换模开始时间是否落在历史映射班次。
     *
     * <p>该结果仅用于记录本批资源导致的班次重算，不作为正规新增换模的失败条件。</p>
     *
     * @param context 排程上下文
     * @param directive 当前反选指令
     * @param mouldChangeStartTime 当前规则实际分配的换模开始时间
     * @return true-普通候选或落在映射班次；false-指定机台的实际换模班次已重算
     */
    private boolean isHistoricalReverseMouldChangeInMappedShift(
            LhScheduleContext context,
            HistoricalReverseSelectionDirective directive,
            Date mouldChangeStartTime) {
        if (Objects.isNull(directive)) {
            return true;
        }
        return Objects.nonNull(mouldChangeStartTime)
                && LhScheduleTimeUtil.getShiftIndex(
                context, context.getScheduleDate(), mouldChangeStartTime)
                == directive.getMappedShiftIndex();
    }

    /**
     * 标记指定机台新增排产成功，并登记后续保护上下文。
     *
     * @param context 排程上下文
     * @param directive 当前反选指令
     * @param sku 当前SKU
     * @param result 新增主链生成的有效结果
     */
    private void markHistoricalReverseDirectiveSucceeded(
            LhScheduleContext context,
            HistoricalReverseSelectionDirective directive,
            SkuScheduleDTO sku,
            LhScheduleResult result) {
        if (Objects.isNull(directive) || Objects.isNull(result)) {
            return;
        }
        directive.setAttempted(true);
        directive.setSuccess(true);
        directive.setActualChangeType(MouldChangeTypeEnum.REGULAR.getCode());
        directive.setResultReason("指定机台复用新增换模主链排产成功");
        context.registerHistoricalReverseSelectedMachine(
                directive.getMaterialCode(), sku.getProductStatus(), directive.getMachineCode());
        context.protectHistoricalReverseResult(result);
        appendHistoricalReverseNewSpecLog(context, directive, "成功", directive.getResultReason());
    }

    /**
     * 标记指定机台反选失败。
     *
     * @param context 排程上下文
     * @param directive 当前反选指令
     * @param reason 明确失败原因
     */
    private void markHistoricalReverseDirectiveFailed(
            LhScheduleContext context,
            HistoricalReverseSelectionDirective directive,
            String reason) {
        if (Objects.isNull(directive) || directive.isAttempted()) {
            return;
        }
        directive.setAttempted(true);
        directive.setSuccess(false);
        directive.setActualChangeType(MouldChangeTypeEnum.REGULAR.getCode());
        directive.setResultReason(StringUtils.defaultIfEmpty(
                reason, "指定机台未通过新增排产主链约束"));
        appendHistoricalReverseNewSpecLog(
                context, directive, "失败", directive.getResultReason());
    }

    /**
     * 判断指令是否为当前SKU仍待执行的正规换模指令。
     *
     * @param directive 反选指令
     * @param sku 当前SKU
     * @return true-待新增主链执行
     */
    private boolean isPendingHistoricalReverseDirectiveForSku(
            HistoricalReverseSelectionDirective directive,
            SkuScheduleDTO sku) {
        return Objects.nonNull(directive)
                && !directive.isAttempted()
                && StringUtils.equals(MouldChangeTypeEnum.REGULAR.getCode(),
                directive.getActualChangeType())
                && isSameHistoricalReverseSku(directive, sku);
    }

    /**
     * 判断历史指令与当前物料状态是否一致。
     *
     * @param directive 反选指令
     * @param sku 当前SKU
     * @return true-物料和归一化产品状态一致
     */
    private boolean isSameHistoricalReverseSku(
            HistoricalReverseSelectionDirective directive,
            SkuScheduleDTO sku) {
        return Objects.nonNull(directive) && Objects.nonNull(sku)
                && StringUtils.equals(directive.getMaterialCode(), sku.getMaterialCode())
                && StringUtils.equals(normalizeHistoricalReverseProductStatus(
                directive.getProductStatus()),
                normalizeHistoricalReverseProductStatus(sku.getProductStatus()));
    }

    /**
     * 归一化历史反选产品状态。
     *
     * @param productStatus 产品状态
     * @return 空状态按正规S处理
     */
    private String normalizeHistoricalReverseProductStatus(String productStatus) {
        return StringUtils.isEmpty(productStatus)
                ? FORMAL_PRODUCT_STATUS : productStatus;
    }

    /**
     * 输出新增主链中的反选结果日志和过程日志。
     *
     * @param context 排程上下文
     * @param directive 反选指令
     * @param result 结果状态
     * @param reason 结果说明
     */
    private void appendHistoricalReverseNewSpecLog(
            LhScheduleContext context,
            HistoricalReverseSelectionDirective directive,
            String result,
            String reason) {
        String detail = "scheduleTargetDate="
                + LhScheduleTimeUtil.formatDate(context.getScheduleTargetDate())
                + ", historicalShift=" + directive.getHistoricalShiftIndex()
                + ", mappedShift=" + directive.getMappedShiftIndex()
                + ", historicalMachine=" + directive.getMachineCode()
                + ", effectiveMachine=" + directive.getEffectiveMachineCode()
                + ", afterMaterialCode=" + directive.getMaterialCode()
                + ", productStatus=" + directive.getProductStatus()
                + ", result=" + result
                + ", reason=" + reason;
        log.info("前日交替计划指定机台新增排产, {}", detail);
        PriorityTraceLogHelper.appendProcessLog(
                context, "前日交替计划机台反选", detail);
    }

    /**
     * 解析无候选机台时的业务原因。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @return 未排原因
     */
    private String resolveNoCandidateMachineReason(LhScheduleContext context, SkuScheduleDTO sku) {
        String mouldChangeLimitBlockedReason = resolveMouldChangeLimitBlockedReason(context, sku);
        if (StringUtils.isNotEmpty(mouldChangeLimitBlockedReason)) {
            return mouldChangeLimitBlockedReason;
        }
        if (isTypeRuleBlocked(context, sku) && isTrialConstructionStage(sku)) {
            if (LhSingleControlMachineUtil.isWholeMachineGranularitySku(context, sku)) {
                return "试制SKU双模的单控L/R整组与普通机台均无法承接，无法排产";
            }
            return "试制SKU单模只能使用单控机台单边，但当前无可用单控机台或单控机台产能不足，无法排产";
        }
        if (isSpecialMaterialSupportBlocked(context, sku)) {
            return "特殊材料SKU无匹配特殊支持机台，无法排产";
        }
        if (isTargetSkuMouldAllOccupied(context, sku)) {
            return TARGET_SKU_MOULD_ALL_OCCUPIED_UNSCHEDULED_REASON;
        }
        return "无可用硫化机台";
    }

    /**
     * 判断目标SKU的模具是否已全部被当前排程结果占用。
     * <p>候选机台硬过滤会在所有目标SKU模具均被占用时返回空候选，这里只复用相同运行态数据
     * 细化未排原因，不改变新增排产候选筛选和排序规则。</p>
     *
     * @param context 排程上下文
     * @param sku SKU
     * @return true-目标SKU模具全部被占用；false-仍保留原无可用硫化机台原因
     */
    private boolean isTargetSkuMouldAllOccupied(LhScheduleContext context, SkuScheduleDTO sku) {
        if (Objects.isNull(context) || Objects.isNull(sku) || StringUtils.isEmpty(sku.getMaterialCode())) {
            return false;
        }
        List<MdmSkuMouldRel> mouldRelList = context.getSkuMouldRelMap().get(sku.getMaterialCode());
        if (CollectionUtils.isEmpty(mouldRelList)) {
            return false;
        }
        Set<String> skuMouldCodeSet = new LinkedHashSet<String>(mouldRelList.size());
        for (MdmSkuMouldRel mouldRel : mouldRelList) {
            if (Objects.isNull(mouldRel) || StringUtils.isEmpty(mouldRel.getMouldCode())) {
                continue;
            }
            skuMouldCodeSet.add(StringUtils.trim(mouldRel.getMouldCode()));
        }
        if (CollectionUtils.isEmpty(skuMouldCodeSet)) {
            return false;
        }
        Set<String> occupiedMouldCodeSet = collectOccupiedMouldCodes(context);
        return !CollectionUtils.isEmpty(occupiedMouldCodeSet)
                && occupiedMouldCodeSet.containsAll(skuMouldCodeSet);
    }

    /**
     * 汇总当前已排结果占用的模具号。
     *
     * @param context 排程上下文
     * @return 已占用模具号集合
     */
    private Set<String> collectOccupiedMouldCodes(LhScheduleContext context) {
        Set<String> occupiedMouldCodeSet = new HashSet<String>(16);
        if (Objects.isNull(context) || CollectionUtils.isEmpty(context.getMachineAssignmentMap())) {
            return occupiedMouldCodeSet;
        }
        for (List<LhScheduleResult> resultList : context.getMachineAssignmentMap().values()) {
            if (CollectionUtils.isEmpty(resultList)) {
                continue;
            }
            for (LhScheduleResult result : resultList) {
                if (isReleasedFirstDayNoPlanPlaceholderResult(context, result)) {
                    continue;
                }
                if (Objects.isNull(result) || StringUtils.isEmpty(result.getMouldCode())) {
                    continue;
                }
                String[] mouldCodeArray = StringUtils.split(result.getMouldCode(), ",");
                if (Objects.isNull(mouldCodeArray)) {
                    continue;
                }
                for (String mouldCode : mouldCodeArray) {
                    String normalizedMouldCode = StringUtils.trim(mouldCode);
                    if (StringUtils.isNotEmpty(normalizedMouldCode)) {
                        occupiedMouldCodeSet.add(normalizedMouldCode);
                    }
                }
            }
        }
        return occupiedMouldCodeSet;
    }

    /**
     * 解析候选机台尝试失败后的未排原因。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param failReason 失败原因
     * @return 未排原因
     */
    private String resolveScheduleFailureReason(LhScheduleContext context, SkuScheduleDTO sku,
                                                NewSpecFailReasonEnum failReason) {
        String mouldChangeLimitBlockedReason = resolveMouldChangeLimitBlockedReason(context, sku);
        if (StringUtils.isNotEmpty(mouldChangeLimitBlockedReason)) {
            return mouldChangeLimitBlockedReason;
        }
        if (isTrialConstructionStage(sku)
                && LhSingleControlMachineUtil.isSingleSideGranularitySku(context, sku)
                && NewSpecFailReasonEnum.NO_CAPACITY_IN_SCHEDULE_WINDOW == failReason) {
            return "试制SKU单模只能使用单控机台单边，但单控机台已被全局排序更靠前的SKU占用，或当前单控机台产能不足，无法排产";
        }
        if (isTrialConstructionStage(sku)
                && LhSingleControlMachineUtil.isWholeMachineGranularitySku(context, sku)
                && NewSpecFailReasonEnum.NO_CAPACITY_IN_SCHEDULE_WINDOW == failReason) {
            return "试制SKU双模的单控L/R整组与普通机台均无可用产能，无法排产";
        }
        return failReason.getDescription();
    }

    /**
     * 解析换模/换活字块日上限阻塞原因。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @return 阻塞原因，无则返回null
     */
    private String resolveMouldChangeLimitBlockedReason(LhScheduleContext context, SkuScheduleDTO sku) {
        if (context == null || sku == null || StringUtils.isEmpty(sku.getMaterialCode())) {
            return null;
        }
        return context.getMouldChangeLimitBlockedReasonMap().get(sku.getMaterialCode());
    }

    /**
     * 判断是否命中特殊材料支持能力阻塞。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @return true-基础条件可匹配，但缺少特殊支持机台
     */
    private boolean isSpecialMaterialSupportBlocked(LhScheduleContext context, SkuScheduleDTO sku) {
        SpecialMaterialMatchResult matchResult = LhSpecialMaterialUtil.resolveMatchResult(context, sku);
        if (context == null || sku == null || matchResult == null || !matchResult.isSpecial()
                || CollectionUtils.isEmpty(context.getMachineScheduleMap())) {
            return false;
        }
        boolean hasBaseMatchedMachine = false;
        for (MachineScheduleDTO machine : context.getMachineScheduleMap().values()) {
            if (machine == null || !MachineStatusUtil.isEnabled(machine.getStatus())) {
                continue;
            }
            if (!LhMachineHardMatchUtil.isInchInRange(
                    LhMachineHardMatchUtil.parseInch(sku.getProSize()),
                    machine.getDimensionMinimum(), machine.getDimensionMaximum())) {
                continue;
            }
            if (!LhMachineHardMatchUtil.isMouldSetMatched(context, sku, machine)) {
                continue;
            }
            hasBaseMatchedMachine = true;
            if (LhMachineHardMatchUtil.isSpecialMaterialSupported(matchResult, machine)) {
                return false;
            }
        }
        return hasBaseMatchedMachine;
    }

    /**
     * 选择优先级更高的失败原因，便于保留最接近真实阻塞点的未排产原因。
     *
     * @param currentReason 当前失败原因
     * @param candidateReason 新候选失败原因
     * @return 优先级更高的失败原因
     */
    private NewSpecFailReasonEnum selectHigherPriorityFailReason(NewSpecFailReasonEnum currentReason,
                                                                 NewSpecFailReasonEnum candidateReason) {
        return candidateReason.getPriority() >= currentReason.getPriority()
                ? candidateReason : currentReason;
    }

    /**
     * 使用局部搜索选择当前SKU的首选机台。
     * <p>若配置关闭、阈值不命中或搜索失败，返回null并自动回退原贪心流程。</p>
     *
     * @param context 排程上下文
     * @param currentSku 当前SKU
     * @param candidates 候选机台
     * @param shifts 排程班次窗口
     * @param machineMatch 机台匹配策略
     * @param mouldChangeBalance 换模均衡策略
     * @param inspectionBalance 首检均衡策略
     * @param capacityCalculate 产能计算策略
     * @return 局部搜索首选机台；无法给出时返回null
     */
    private MachineScheduleDTO selectPreferredMachineByLocalSearch(LhScheduleContext context,
                                                                   SkuScheduleDTO currentSku,
                                                                   List<MachineScheduleDTO> candidates,
                                                                   List<LhShiftConfigVO> shifts,
                                                                   IMachineMatchStrategy machineMatch,
                                                                   IMouldChangeBalanceStrategy mouldChangeBalance,
                                                                   IFirstInspectionBalanceStrategy inspectionBalance,
                                                                   ICapacityCalculateStrategy capacityCalculate) {
        if (!shouldUseLocalSearch(context, candidates)) {
            return null;
        }
        List<SkuScheduleDTO> windowSkuList = buildLocalSearchWindow(context, currentSku);
        if (CollectionUtils.isEmpty(windowSkuList)) {
            return null;
        }
        IMouldChangeBalanceStrategy localSearchMouldChangeBalance =
                resolveLocalSearchMouldChangeBalance(context, mouldChangeBalance);
        return localSearchMachineAllocator.selectBestMachine(
                context, windowSkuList, candidates, shifts, machineMatch, localSearchMouldChangeBalance,
                inspectionBalance, capacityCalculate);
    }

    /**
     * 解析局部搜索使用的换模分配策略。
     * <p>关闭换模均衡时，评估链路也必须使用基础换模口径，避免机台评估被配额均衡影响。</p>
     *
     * @param context 排程上下文
     * @param mouldChangeBalance 原换模均衡策略
     * @return 局部搜索使用的换模分配策略
     */
    private IMouldChangeBalanceStrategy resolveLocalSearchMouldChangeBalance(
            LhScheduleContext context,
            IMouldChangeBalanceStrategy mouldChangeBalance) {
        if (isChangeoverBalanceEnabled(context)) {
            return mouldChangeBalance;
        }
        return new IMouldChangeBalanceStrategy() {
            @Override
            public boolean hasCapacity(LhScheduleContext ctx, Date targetDate) {
                return true;
            }

            @Override
            public Date allocateMouldChange(LhScheduleContext ctx, String machineCode, Date endingTime) {
                return allocateBasicMouldChangeStartTime(
                        ctx, machineCode, endingTime, LhScheduleTimeUtil.getMouldChangeTotalHours(ctx));
            }

            @Override
            public Date allocateMouldChange(LhScheduleContext ctx,
                                            String machineCode,
                                            Date endingTime,
                                            int switchDurationHours) {
                return allocateBasicMouldChangeStartTime(ctx, machineCode, endingTime, switchDurationHours);
            }

            @Override
            public void rollbackMouldChange(LhScheduleContext ctx, Date allocatedTime) {
                // 基础换模分配不占用均衡配额，无需回滚。
            }

            @Override
            public int getRemainingCapacity(LhScheduleContext ctx, Date targetDate) {
                return Integer.MAX_VALUE;
            }
        };
    }

    /**
     * 按真实可开产时间筛选当天最早目标班次的候选机台。
     *
     * <p>输入列表已按八层软规则排序。本方法只按班次顺序做筛选，不在同班次内重新排序；
     * 每台候选的完整时间轴只计算一次，并写入当前SKU短生命周期计划缓存供正式落地复核。</p>
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param candidates 当前硬过滤及动态过滤后的候选
     * @param dayContext 当前业务日
     * @param capacityCalculate 机台准备时间策略
     * @param mouldChangeBalance 换模均衡策略
     * @param candidateProductionNotBeforeTime 候选预演生产门禁，不包含胎胚最早可供时间
     * @param productionNotBeforeTime 正式生产门禁，包含胎胚最早可供时间
     * @param remainingQty 当前候选目标量
     * @param totalScheduledQty 当前SKU已排量
     * @param addMachineProductionDate 当前追加机台生效日
     * @param isEnding 是否收尾
     * @param historyResidualCapacityPreferenceEnabled 是否启用历史班次剩余产能优先分层
     * @param planMap 当前选机回合计划缓存
     * @return 历史班次分层后的候选；无历史命中时返回当天最早班次候选
     */
    private List<MachineScheduleDTO> filterByEarliestAvailableShift(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            List<MachineScheduleDTO> candidates,
            DayScheduleContext dayContext,
            ICapacityCalculateStrategy capacityCalculate,
            IMouldChangeBalanceStrategy mouldChangeBalance,
            IFirstInspectionBalanceStrategy inspectionBalance,
            Date candidateProductionNotBeforeTime,
            Date productionNotBeforeTime,
            int remainingQty,
            int totalScheduledQty,
            LocalDate addMachineProductionDate,
            boolean isEnding,
            boolean historyResidualCapacityPreferenceEnabled,
            Map<String, NewSpecMachineAvailabilityPlan> planMap) {
        planMap.clear();
        if (CollectionUtils.isEmpty(candidates)) {
            return Collections.emptyList();
        }
        for (MachineScheduleDTO machine : candidates) {
            NewSpecMachineAvailabilityPlan plan = this.resolveMachineAvailabilityPlan(
                    context, sku, machine, dayContext, capacityCalculate, mouldChangeBalance,
                    inspectionBalance,
                    candidateProductionNotBeforeTime, productionNotBeforeTime,
                    remainingQty, totalScheduledQty,
                    addMachineProductionDate, isEnding,
                    historyResidualCapacityPreferenceEnabled);
            planMap.put(machine.getMachineCode(), plan);
        }

        LhShiftConfigVO earliestTargetShift = this.resolveEarliestAvailableTargetShift(
                dayContext.getDayShifts(), planMap);
        if (Objects.isNull(earliestTargetShift)) {
            this.appendMachineAvailabilityProcessLog(
                    context, sku, dayContext, null,
                    Collections.<MachineScheduleDTO>emptyList(), planMap);
            return Collections.emptyList();
        }

        List<MachineScheduleDTO> currentShiftCandidates =
                this.filterCandidatesByFormalTargetShift(
                        candidates, earliestTargetShift, planMap);
        log.info("新增SKU逐班筛选正式可开产候选, batchNo: {}, scheduleDate: {}, materialCode: {}, "
                        + "formalTargetShift: class{}, candidateCount: {}, candidates: {}",
                context.getBatchNo(), dayContext.getScheduleDate(), sku.getMaterialCode(),
                earliestTargetShift.getShiftIndex(), currentShiftCandidates.size(),
                currentShiftCandidates.stream().map(MachineScheduleDTO::getMachineCode)
                        .collect(Collectors.toList()));

        if (historyResidualCapacityPreferenceEnabled) {
            List<MachineScheduleDTO> historyResidualCandidates =
                    this.applyHistoryResidualCapacityPriority(
                            context, sku, candidates, earliestTargetShift,
                            isEnding, planMap);
            if (!CollectionUtils.isEmpty(historyResidualCandidates)) {
                this.appendMachineAvailabilityProcessLog(
                        context, sku, dayContext, earliestTargetShift,
                        historyResidualCandidates, planMap);
                return historyResidualCandidates;
            }
        }
        this.appendMachineAvailabilityProcessLog(
                context, sku, dayContext, earliestTargetShift,
                currentShiftCandidates, planMap);
        return currentShiftCandidates;
    }

    /**
     * 解析普通候选最早存在正式可开产机台的目标班次。
     *
     * @param dayShifts 当前业务日班次
     * @param planMap 当前选机回合真实可开产计划
     * @return 最早正式目标班次；不存在可用计划时返回null
     */
    private LhShiftConfigVO resolveEarliestAvailableTargetShift(
            List<LhShiftConfigVO> dayShifts,
            Map<String, NewSpecMachineAvailabilityPlan> planMap) {
        if (CollectionUtils.isEmpty(dayShifts) || CollectionUtils.isEmpty(planMap)) {
            return null;
        }
        for (LhShiftConfigVO dayShift : dayShifts) {
            if (Objects.isNull(dayShift) || Objects.isNull(dayShift.getShiftIndex())) {
                continue;
            }
            for (NewSpecMachineAvailabilityPlan plan : planMap.values()) {
                if (Objects.nonNull(plan) && plan.isAvailable()
                        && plan.isPreparationAvailable()
                        && Objects.nonNull(plan.getFormalTargetShift())
                        && Objects.equals(plan.getFormalTargetShift().getShiftIndex(),
                        dayShift.getShiftIndex())) {
                    return dayShift;
                }
            }
        }
        return null;
    }

    /**
     * 从原八层排序候选中筛选正式可开产时间属于指定班次的机台。
     *
     * @param candidates 原八层排序候选
     * @param targetShift 正式目标班次
     * @param planMap 当前选机回合真实可开产计划
     * @return 保持原八层相对顺序的班次候选
     */
    private List<MachineScheduleDTO> filterCandidatesByFormalTargetShift(
            List<MachineScheduleDTO> candidates,
            LhShiftConfigVO targetShift,
            Map<String, NewSpecMachineAvailabilityPlan> planMap) {
        if (CollectionUtils.isEmpty(candidates) || Objects.isNull(targetShift)
                || Objects.isNull(targetShift.getShiftIndex())
                || CollectionUtils.isEmpty(planMap)) {
            return Collections.emptyList();
        }
        return candidates.stream()
                .filter(Objects::nonNull)
                .filter(candidate -> {
                    NewSpecMachineAvailabilityPlan plan = planMap.get(candidate.getMachineCode());
                    return Objects.nonNull(plan) && plan.isAvailable()
                            && plan.isPreparationAvailable()
                            && Objects.nonNull(plan.getFormalTargetShift())
                            && Objects.equals(plan.getFormalTargetShift().getShiftIndex(),
                            targetShift.getShiftIndex());
                })
                .collect(Collectors.toList());
    }

    /**
     * 判断当前班次是否存在必须保持的既有固定优先机台。
     *
     * @param context 排程上下文
     * @param sku 当前待排SKU
     * @param candidates 当前班次候选机台
     * @param preferredTrialMachine 试制、量试或小批量定点预选机台
     * @return 跳过历史班次优先筛选的原因；无需跳过时返回null
     */
    private String resolveHistoryResidualCapacityPreferenceSkipReason(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            List<MachineScheduleDTO> candidates,
            MachineScheduleDTO preferredTrialMachine) {
        MachineScheduleDTO preferredContinuousMachine =
                this.resolvePreferredContinuousCompensationMachine(sku, candidates);
        if (Objects.nonNull(preferredContinuousMachine)) {
            return "续作补偿原机台保持固定优先：" + preferredContinuousMachine.getMachineCode();
        }
        if (Objects.nonNull(preferredTrialMachine)
                && this.containsMachine(candidates, preferredTrialMachine.getMachineCode())) {
            return "试制/量试/小批量定点机台保持固定优先："
                    + preferredTrialMachine.getMachineCode();
        }
        for (MachineScheduleDTO candidate : candidates) {
            if (Objects.isNull(candidate) || StringUtils.isEmpty(candidate.getMachineCode())) {
                continue;
            }
            DayTypeBlockReverseSelectionDirective directive =
                    this.findDayTypeBlockDirectiveByMachineAndSku(
                            context, candidate.getMachineCode(), sku);
            if (Objects.nonNull(directive) && directive.isAttempted()
                    && !directive.isSatisfied() && !directive.isSuccess()) {
                return "按天换活字块反选预留机台保持固定优先："
                        + candidate.getMachineCode();
            }
        }
        return null;
    }

    /**
     * 按来源班次从早到晚构建历史剩余产能优先池。
     *
     * <p>入参已经完成硬过滤和八层软排序。方法只按来源班次拆分候选池，同一来源班次
     * 继续按入参顺序遍历，禁止把历史班次写入Comparator。每台机台只登记最早命中的
     * 来源班次，保证“更早历史班次 → 更晚历史班次”的严格优先级。</p>
     *
     * @param context 排程上下文
     * @param sku 当前待排SKU
     * @param candidates 硬过滤后的原八层排序候选
     * @param currentTargetShift 普通候选最早正式目标班次
     * @param isEnding 当前SKU是否收尾
     * @param planMap 当前选机回合统一可开产计划
     * @return 按历史来源班次分层后的候选；无命中时返回空列表
     */
    private List<MachineScheduleDTO> applyHistoryResidualCapacityPriority(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            List<MachineScheduleDTO> candidates,
            LhShiftConfigVO currentTargetShift,
            boolean isEnding,
            Map<String, NewSpecMachineAvailabilityPlan> planMap) {
        if (Objects.isNull(context) || Objects.isNull(sku)
                || CollectionUtils.isEmpty(candidates) || Objects.isNull(currentTargetShift)
                || CollectionUtils.isEmpty(planMap)) {
            return Collections.emptyList();
        }
        List<LhShiftConfigVO> sourceShifts = this.resolveHistoryResidualSourceShifts(
                context, currentTargetShift);
        if (CollectionUtils.isEmpty(sourceShifts)) {
            return Collections.emptyList();
        }
        List<MachineScheduleDTO> historyResidualCandidates =
                new ArrayList<MachineScheduleDTO>(Math.min(16, candidates.size()));
        Set<String> matchedMachineCodes = new HashSet<String>(candidates.size());
        for (int sourceShiftIndex = 0; sourceShiftIndex < sourceShifts.size(); sourceShiftIndex++) {
            LhShiftConfigVO sourceShift = sourceShifts.get(sourceShiftIndex);
            int priorityLevel = sourceShiftIndex + 1;
            for (MachineScheduleDTO candidate : candidates) {
                if (Objects.isNull(candidate) || StringUtils.isEmpty(candidate.getMachineCode())
                        || matchedMachineCodes.contains(candidate.getMachineCode())) {
                    continue;
                }
                NewSpecMachineAvailabilityPlan plan = planMap.get(candidate.getMachineCode());
                if (Objects.isNull(plan) || !plan.isAvailable()
                        || !plan.isPreparationAvailable()) {
                    continue;
                }
                HistoricalResidualCapacityInfo residualCapacityInfo =
                        this.resolveHistoricalResidualCapacityInfo(
                                context, sku, candidate, sourceShift,
                                isEnding, priorityLevel);
                if (Objects.isNull(residualCapacityInfo)) {
                    continue;
                }
                planMap.put(candidate.getMachineCode(),
                        plan.withHistoricalResidualCapacityInfo(residualCapacityInfo));
                matchedMachineCodes.add(candidate.getMachineCode());
                historyResidualCandidates.add(candidate);
            }
        }
        log.info("新增SKU历史班次剩余产能候选分层完成, batchNo: {}, materialCode: {}, "
                        + "currentTargetShift: {}, lookbackStartTime: {}, sourceShifts: {}, "
                        + "historyCandidates: {}",
                context.getBatchNo(), sku.getMaterialCode(),
                this.formatShiftIndex(currentTargetShift),
                LhScheduleTimeUtil.formatDateTime(DateUtil.offsetDay(
                        currentTargetShift.getShiftStartDateTime(),
                        -HISTORY_RESIDUAL_LOOKBACK_DAYS)),
                sourceShifts.stream().map(this::formatShiftIndex).collect(Collectors.toList()),
                this.formatHistoryResidualCapacityCandidates(
                        historyResidualCandidates, planMap));
        return historyResidualCandidates;
    }

    /**
     * 解析当前目标班次之前一天范围内的历史来源班次。
     *
     * <p>统一比较班次绝对起止时间，不读取T/T+1或class编号。仅使用当前批次已经初始化
     * 的排程窗口班次，避免把旧批次持久化结果混入当前运行态。</p>
     *
     * @param context 排程上下文
     * @param currentTargetShift 普通候选最早正式目标班次
     * @return 按开始时间升序排列的历史来源班次
     */
    private List<LhShiftConfigVO> resolveHistoryResidualSourceShifts(
            LhScheduleContext context,
            LhShiftConfigVO currentTargetShift) {
        if (Objects.isNull(context) || Objects.isNull(currentTargetShift)
                || Objects.isNull(currentTargetShift.getShiftStartDateTime())
                || CollectionUtils.isEmpty(context.getScheduleWindowShifts())) {
            return Collections.emptyList();
        }
        Date currentShiftStartTime = currentTargetShift.getShiftStartDateTime();
        Date lookbackStartTime = DateUtil.offsetDay(
                currentShiftStartTime, -HISTORY_RESIDUAL_LOOKBACK_DAYS);
        return context.getScheduleWindowShifts().stream()
                .filter(Objects::nonNull)
                .filter(shift -> Objects.nonNull(shift.getShiftStartDateTime())
                        && Objects.nonNull(shift.getShiftEndDateTime()))
                .filter(shift -> !shift.getShiftStartDateTime().before(lookbackStartTime))
                .filter(shift -> !shift.getShiftEndDateTime().after(currentShiftStartTime))
                .sorted(Comparator.comparing(LhShiftConfigVO::getShiftStartDateTime))
                .collect(Collectors.toList());
    }

    /**
     * 构建机台在指定历史班次内的实时剩余产能画像。
     *
     * @param context 排程上下文
     * @param sku 当前待排SKU
     * @param machine 候选机台
     * @param sourceShift 历史来源班次
     * @param isEnding 当前SKU是否收尾
     * @param priorityLevel 来源班次优先级
     * @return 历史剩余产能画像；无完整一模产能时返回null
     */
    private HistoricalResidualCapacityInfo resolveHistoricalResidualCapacityInfo(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            MachineScheduleDTO machine,
            LhShiftConfigVO sourceShift,
            boolean isEnding,
            int priorityLevel) {
        Date availableStartTime = this.resolveHistoricalShiftContinuousAvailableTime(
                context, sku, machine, sourceShift);
        if (Objects.isNull(availableStartTime)) {
            return null;
        }
        ShiftProductionControlDTO control = ShiftProductionControlUtil.resolveEffectiveControl(
                context, sourceShift, availableStartTime);
        if (Objects.isNull(control) || !control.isCanSchedule()
                || Objects.isNull(control.getEffectiveEndTime())
                || !availableStartTime.before(control.getEffectiveEndTime())) {
            return null;
        }
        int residualCapacityQty = this.resolveHistoricalResidualCapacityQty(
                context, sku, machine, sourceShift, availableStartTime, isEnding);
        long netProductiveSeconds = this.resolveHistoricalResidualNetProductiveSeconds(
                context, sku, machine, availableStartTime,
                control.getEffectiveEndTime());
        if (residualCapacityQty <= 0 || netProductiveSeconds < sku.getLhTimeSeconds()) {
            return null;
        }
        return new HistoricalResidualCapacityInfo(
                sourceShift, availableStartTime, control.getEffectiveEndTime(),
                netProductiveSeconds, residualCapacityQty, priorityLevel);
    }

    /**
     * 解析候选机台在指定历史班次尾部的首个连续可生产时间。
     *
     * @param context 排程上下文
     * @param sku 当前待排SKU，用硫化周期判断机台是否真正还能生产
     * @param machine 候选机台
     * @param sourceShift 历史来源班次
     * @return 首个可连续完成一模的生产时间；无可利用产能时返回null
     */
    private Date resolveHistoricalShiftContinuousAvailableTime(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            MachineScheduleDTO machine,
            LhShiftConfigVO sourceShift) {
        if (Objects.isNull(machine) || StringUtils.isEmpty(machine.getMachineCode())
                || Objects.isNull(sourceShift.getShiftStartDateTime())
                || Objects.isNull(sourceShift.getShiftEndDateTime())
                || sku.getLhTimeSeconds() <= 0) {
            return null;
        }
        Date tailStartTime = this.resolveHistoricalShiftTailStartTime(
                context, sku, machine, sourceShift);
        ShiftProductionControlDTO control = ShiftProductionControlUtil.resolveEffectiveControl(
                context, sourceShift, tailStartTime);
        if (Objects.isNull(control) || !control.isCanSchedule()
                || Objects.isNull(control.getEffectiveStartTime())
                || Objects.isNull(control.getEffectiveEndTime())) {
            return null;
        }
        boolean wholeSingleControlMachine =
                LhSingleControlMachineUtil.isWholeMachineGranularitySku(context, sku)
                        && this.isSingleControlMachine(context, machine.getMachineCode());
        if (!wholeSingleControlMachine) {
            return this.resolveMachineHistoricalShiftContinuousTime(
                    context, sku, machine.getMachineCode(), control.getEffectiveStartTime(),
                    control.getEffectiveEndTime(), sku.getLhTimeSeconds());
        }
        MachineScheduleDTO pairMachine = LhSingleControlMachineUtil.resolvePairMachine(
                context, machine.getMachineCode());
        if (Objects.isNull(pairMachine) || StringUtils.isEmpty(pairMachine.getMachineCode())) {
            return null;
        }
        return this.resolveWholeSingleControlHistoricalShiftContinuousTime(
                context, sku, machine.getMachineCode(), pairMachine.getMachineCode(),
                control.getEffectiveStartTime(), control.getEffectiveEndTime(),
                sku.getLhTimeSeconds());
    }

    /**
     * 解析普通机台在指定窗口内首个可连续完成一模的时间。
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @param startTime 查询开始时间
     * @param endTime 查询结束时间
     * @param lhTimeSeconds 当前SKU硫化周期秒数
     * @return 首个连续可生产时间；无可用窗口时返回null
     */
    private Date resolveMachineHistoricalShiftContinuousTime(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            String machineCode,
            Date startTime,
            Date endTime,
            int lhTimeSeconds) {
        return ShiftCapacityResolverUtil.resolveFirstContinuousProductiveTime(
                context.getDevicePlanShutList(),
                this.resolveEffectiveCleaningWindowList(
                        context, machineCode, sku, null, null),
                this.resolveMachineMaintenanceWindowList(context, machineCode),
                machineCode, startTime, endTime, lhTimeSeconds);
    }

    /**
     * 解析单控整机L/R两侧共同存在的历史班次连续生产窗口。
     *
     * <p>两侧分别复用现有停机、清洗和维护时间轴；游标只向后推进，找到两侧能够从同一时刻
     * 各自连续完成一个硫化周期时返回，避免仅代表侧可生产就误判整机可利用。</p>
     *
     * @param context 排程上下文
     * @param machineCode 当前候选侧机台编码
     * @param pairMachineCode 配对侧机台编码
     * @param startTime 查询开始时间
     * @param endTime 查询结束时间
     * @param lhTimeSeconds 当前SKU硫化周期秒数
     * @return L/R共同连续可生产时间；无共同窗口时返回null
     */
    private Date resolveWholeSingleControlHistoricalShiftContinuousTime(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            String machineCode,
            String pairMachineCode,
            Date startTime,
            Date endTime,
            int lhTimeSeconds) {
        List<MachineCleaningWindowDTO> machineCleaningWindowList =
                this.resolveEffectiveCleaningWindowList(
                        context, machineCode, sku, null, null);
        List<MachineMaintenanceWindowDTO> machineMaintenanceWindowList =
                this.resolveMachineMaintenanceWindowList(context, machineCode);
        List<MachineCleaningWindowDTO> pairCleaningWindowList =
                this.resolveEffectiveCleaningWindowList(
                        context, pairMachineCode, sku, null, null);
        List<MachineMaintenanceWindowDTO> pairMaintenanceWindowList =
                this.resolveMachineMaintenanceWindowList(context, pairMachineCode);
        Date cursorTime = startTime;
        while (Objects.nonNull(cursorTime) && cursorTime.before(endTime)) {
            Date machineAvailableTime = ShiftCapacityResolverUtil.resolveFirstContinuousProductiveTime(
                    context.getDevicePlanShutList(), machineCleaningWindowList,
                    machineMaintenanceWindowList, machineCode,
                    cursorTime, endTime, lhTimeSeconds);
            Date pairAvailableTime = ShiftCapacityResolverUtil.resolveFirstContinuousProductiveTime(
                    context.getDevicePlanShutList(), pairCleaningWindowList,
                    pairMaintenanceWindowList, pairMachineCode,
                    cursorTime, endTime, lhTimeSeconds);
            if (Objects.isNull(machineAvailableTime) || Objects.isNull(pairAvailableTime)) {
                return null;
            }
            Date commonStartTime = this.resolveLaterTime(
                    machineAvailableTime, pairAvailableTime);
            if (commonStartTime.equals(machineAvailableTime)
                    && commonStartTime.equals(pairAvailableTime)) {
                return commonStartTime;
            }
            if (!commonStartTime.after(cursorTime)) {
                return null;
            }
            cursorTime = commonStartTime;
        }
        return null;
    }

    /**
     * 计算候选机台在历史来源班次内的剩余可排量。
     *
     * <p>复用正式新增排产的班产、模数、奇数班产、开停产、停机、清洗、精度和维修
     * 扣减链。整机粒度单控 SKU 同时计算 L/R 两侧并取较小值，避免单侧有产能时误打标。</p>
     *
     * @param context 排程上下文
     * @param sku 当前待排SKU
     * @param machine 候选机台
     * @param sourceShift 历史来源班次
     * @param availableStartTime 首个连续可生产时间
     * @param isEnding 当前SKU是否收尾
     * @return 历史班次剩余可排量
     */
    private int resolveHistoricalResidualCapacityQty(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            MachineScheduleDTO machine,
            LhShiftConfigVO sourceShift,
            Date availableStartTime,
            boolean isEnding) {
        int machineCapacityQty = this.resolveSingleMachineHistoricalResidualCapacityQty(
                context, sku, machine, sourceShift, availableStartTime, isEnding);
        boolean wholeSingleControlMachine =
                LhSingleControlMachineUtil.isWholeMachineGranularitySku(context, sku)
                        && this.isSingleControlMachine(context, machine.getMachineCode());
        if (!wholeSingleControlMachine || machineCapacityQty <= 0) {
            return machineCapacityQty;
        }
        MachineScheduleDTO pairMachine = LhSingleControlMachineUtil.resolvePairMachine(
                context, machine.getMachineCode());
        if (Objects.isNull(pairMachine)) {
            return 0;
        }
        int pairCapacityQty = this.resolveSingleMachineHistoricalResidualCapacityQty(
                context, sku, pairMachine, sourceShift, availableStartTime, isEnding);
        return Math.min(machineCapacityQty, pairCapacityQty);
    }

    /**
     * 复用正式班次产能链计算单台机台的历史剩余可排量。
     *
     * @param context 排程上下文
     * @param sku 当前待排SKU
     * @param machine 当前机台
     * @param sourceShift 历史来源班次
     * @param availableStartTime 首个连续可生产时间
     * @param isEnding 当前SKU是否收尾
     * @return 单台机台剩余可排量
     */
    private int resolveSingleMachineHistoricalResidualCapacityQty(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            MachineScheduleDTO machine,
            LhShiftConfigVO sourceShift,
            Date availableStartTime,
            boolean isEnding) {
        if (Objects.isNull(machine) || Objects.isNull(sourceShift)
                || Objects.isNull(sourceShift.getShiftIndex())) {
            return 0;
        }
        int mouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(machine);
        int runtimeShiftCapacity = ShiftCapacityResolverUtil.resolveRuntimeShiftCapacity(
                context, machine, sku.getShiftCapacity());
        Map<Integer, Integer> capacityMap = this.calculateShiftCapacityMap(
                context, machine, sku, availableStartTime,
                null, null, Collections.singletonList(sourceShift),
                mouldQty, runtimeShiftCapacity, isEnding, false);
        Integer capacityQty = capacityMap.get(sourceShift.getShiftIndex());
        return Objects.isNull(capacityQty) ? 0 : Math.max(0, capacityQty);
    }

    /**
     * 计算历史剩余区间扣减停机、清洗、精度和维修后的净可生产秒数。
     *
     * @param context 排程上下文
     * @param sku 当前待排SKU
     * @param machine 候选机台
     * @param availableStartTime 剩余区间开始时间
     * @param availableEndTime 剩余区间结束时间
     * @return 净可生产秒数；整机粒度单控取L/R较小值
     */
    private long resolveHistoricalResidualNetProductiveSeconds(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            MachineScheduleDTO machine,
            Date availableStartTime,
            Date availableEndTime) {
        long machineProductiveSeconds = this.resolveSingleMachineNetProductiveSeconds(
                context, sku, machine, availableStartTime, availableEndTime);
        boolean wholeSingleControlMachine =
                LhSingleControlMachineUtil.isWholeMachineGranularitySku(context, sku)
                        && this.isSingleControlMachine(context, machine.getMachineCode());
        if (!wholeSingleControlMachine || machineProductiveSeconds <= 0L) {
            return machineProductiveSeconds;
        }
        MachineScheduleDTO pairMachine = LhSingleControlMachineUtil.resolvePairMachine(
                context, machine.getMachineCode());
        if (Objects.isNull(pairMachine)) {
            return 0L;
        }
        long pairProductiveSeconds = this.resolveSingleMachineNetProductiveSeconds(
                context, sku, pairMachine, availableStartTime, availableEndTime);
        return Math.min(machineProductiveSeconds, pairProductiveSeconds);
    }

    /**
     * 计算单台机台在指定区间内的净可生产秒数。
     *
     * @param context 排程上下文
     * @param sku 当前待排SKU
     * @param machine 当前机台
     * @param availableStartTime 区间开始时间
     * @param availableEndTime 区间结束时间
     * @return 净可生产秒数
     */
    private long resolveSingleMachineNetProductiveSeconds(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            MachineScheduleDTO machine,
            Date availableStartTime,
            Date availableEndTime) {
        if (Objects.isNull(machine) || StringUtils.isEmpty(machine.getMachineCode())) {
            return 0L;
        }
        return ShiftCapacityResolverUtil.resolveNetProductiveSeconds(
                context.getDevicePlanShutList(),
                this.resolveEffectiveCleaningWindowList(
                        context, machine.getMachineCode(), sku, null, null),
                this.resolveMachineMaintenanceWindowList(
                        context, machine.getMachineCode()),
                machine.getMachineCode(), availableStartTime, availableEndTime);
    }

    /**
     * 解析历史班次尾部连续空闲区间的起点。
     *
     * <p>普通机台读取当前侧；正规等整机粒度单控SKU继续复用L/R整机口径，取两侧历史班次
     * 实际占用结束时间的较晚值。没有任何占用时从班次开始时间计算。</p>
     *
     * @param context 排程上下文
     * @param sku 当前待排SKU
     * @param machine 候选机台
     * @param sourceShift 历史来源班次
     * @return 历史班次尾部连续空闲起点
     */
    private Date resolveHistoricalShiftTailStartTime(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            MachineScheduleDTO machine,
            LhShiftConfigVO sourceShift) {
        Date tailStartTime = this.resolveMachineHistoricalShiftOccupationEndTime(
                context, machine, sourceShift);
        if (!LhSingleControlMachineUtil.isWholeMachineGranularitySku(context, sku)
                || !this.isSingleControlMachine(context, machine.getMachineCode())) {
            return tailStartTime;
        }
        MachineScheduleDTO pairMachine = LhSingleControlMachineUtil.resolvePairMachine(
                context, machine.getMachineCode());
        if (Objects.isNull(pairMachine)) {
            // 整机粒度无法取得配对侧时不能证明历史班次仍可整机生产，仅取消优先资格。
            return sourceShift.getShiftEndDateTime();
        }
        Date pairTailStartTime = this.resolveMachineHistoricalShiftOccupationEndTime(
                context, pairMachine, sourceShift);
        return this.resolveLaterTime(tailStartTime, pairTailStartTime);
    }

    /**
     * 解析单台机台在指定历史班次最后一次真实占用的结束时间。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param sourceShift 历史来源班次
     * @return 最后占用结束时间；历史班次完全空闲时返回班次开始时间
     */
    private Date resolveMachineHistoricalShiftOccupationEndTime(
            LhScheduleContext context,
            MachineScheduleDTO machine,
            LhShiftConfigVO sourceShift) {
        Date shiftStartTime = sourceShift.getShiftStartDateTime();
        if (Objects.isNull(machine) || StringUtils.isEmpty(machine.getMachineCode())) {
            return shiftStartTime;
        }
        MachineScheduleDTO initialMachine =
                context.getInitialMachineScheduleMap().get(machine.getMachineCode());
        boolean releasedContinuousMachine =
                context.getReleasedContinuousMachineCodeSet().contains(machine.getMachineCode());
        Date initialEndTime = releasedContinuousMachine
                ? machine.getEstimatedEndTime()
                : Objects.nonNull(initialMachine)
                ? initialMachine.getEstimatedEndTime() : machine.getEstimatedEndTime();
        Date latestEndTime = Objects.nonNull(initialEndTime)
                && initialEndTime.after(shiftStartTime) ? initialEndTime : null;
        List<LhScheduleResult> assignedResults =
                context.getMachineAssignmentMap().get(machine.getMachineCode());
        if (!CollectionUtils.isEmpty(assignedResults)) {
            for (LhScheduleResult result : assignedResults) {
                if (Objects.isNull(result) || Objects.isNull(sourceShift.getShiftIndex())) {
                    continue;
                }
                if (this.isReleasedFirstDayNoPlanPlaceholderResult(context, result)) {
                    // 已释放续作占位结果不再代表实时机台占用，保持与新增硬候选口径一致。
                    continue;
                }
                int shiftIndex = sourceShift.getShiftIndex();
                Integer shiftPlanQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
                if (Objects.isNull(shiftPlanQty) || shiftPlanQty <= 0) {
                    continue;
                }
                Date resultEndTime = ShiftFieldUtil.getShiftEndTime(result, shiftIndex);
                if (Objects.isNull(resultEndTime)) {
                    /*
                     * 正计划班次缺少结束时间时无法证明尾部仍可生产，按班次结束处理只取消
                     * 本次优先资格，不会把机台从正式候选中删除。
                     */
                    resultEndTime = sourceShift.getShiftEndDateTime();
                }
                latestEndTime = this.resolveLaterTime(latestEndTime, resultEndTime);
            }
        }
        return Objects.nonNull(latestEndTime) ? latestEndTime : shiftStartTime;
    }

    /**
     * 记录历史班次剩余产能优先的真实候选作用域和最终选机结果。
     *
     * @param context 排程上下文
     * @param sku 当前待排SKU
     * @param currentShift 普通候选最早正式目标班次
     * @param currentShiftCandidates 当前班次普通候选
     * @param historyResidualCandidates 历史班次剩余产能候选
     * @param evaluated 是否执行了历史班次产能判断
     * @param applied 是否应用历史优先池
     * @param skipReason 未执行历史优先池的原因
     * @param orderedCandidates 最终实际选机作用域
     * @param selectedMachine 最终选择机台
     * @param planMap 当前选机回合统一可开产计划
     */
    private void appendHistoryResidualCapacityPreferenceLog(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LhShiftConfigVO currentShift,
            List<MachineScheduleDTO> currentShiftCandidates,
            List<MachineScheduleDTO> historyResidualCandidates,
            boolean evaluated,
            boolean applied,
            String skipReason,
            List<MachineScheduleDTO> orderedCandidates,
            MachineScheduleDTO selectedMachine,
            Map<String, NewSpecMachineAvailabilityPlan> planMap) {
        if (!PriorityTraceLogHelper.isEnabled(context)) {
            return;
        }
        int candidateCount = CollectionUtils.isEmpty(historyResidualCandidates)
                ? 0 : historyResidualCandidates.size();
        int initialCapacity = Math.min(4096, Math.max(512, candidateCount * 48));
        StringBuilder detail = new StringBuilder(initialCapacity);
        PriorityTraceLogHelper.appendLine(detail,
                "批次=" + context.getBatchNo()
                        + "，工厂=" + context.getFactoryCode()
                        + "，排程日期=" + LhScheduleTimeUtil.formatDate(
                        context.getCurrentScheduleDate())
                        + "，SKU=" + sku.getMaterialCode()
                        + "，产品状态=" + StringUtils.defaultIfEmpty(sku.getProductStatus(), "-"));
        PriorityTraceLogHelper.appendLine(detail,
                "当前班次=" + this.formatShiftIndex(currentShift)
                        + "，向前查找范围=1天"
                        + "，已执行判断=" + (evaluated ? 1 : 0)
                        + "，命中并应用=" + (applied ? 1 : 0)
                        + "，跳过/回退原因=" + StringUtils.defaultIfEmpty(skipReason,
                        applied ? "命中历史班次剩余产能优先池"
                                : "无机台命中，回退当前班次全部候选"));
        PriorityTraceLogHelper.appendLine(detail,
                "当前班次全部候选=" + this.formatMachineCodes(currentShiftCandidates));
        PriorityTraceLogHelper.appendLine(detail,
                "历史班次剩余产能候选="
                        + this.formatHistoryResidualCapacityCandidates(
                        historyResidualCandidates, planMap));
        HistoricalResidualCapacityInfo selectedResidualCapacityInfo =
                this.resolveHistoricalResidualCapacityInfo(selectedMachine, planMap);
        PriorityTraceLogHelper.appendLine(detail,
                "最终实际候选顺序=" + this.formatMachineCodes(orderedCandidates)
                        + "，最终选择机台=" + (Objects.isNull(selectedMachine)
                        ? "无" : selectedMachine.getMachineCode())
                        + "，最终来源班次=" + (Objects.isNull(selectedResidualCapacityInfo)
                        ? "当前班次普通候选"
                        : this.formatShiftIndex(selectedResidualCapacityInfo.getSourceShift()))
                        + "，剩余区间=" + (Objects.isNull(selectedResidualCapacityInfo)
                        ? "无" : "[" + LhScheduleTimeUtil.formatDateTime(
                        selectedResidualCapacityInfo.getAvailableStartTime()) + ","
                        + LhScheduleTimeUtil.formatDateTime(
                        selectedResidualCapacityInfo.getAvailableEndTime()) + ")")
                        + "，净可用分钟=" + (Objects.isNull(selectedResidualCapacityInfo)
                        ? 0L : selectedResidualCapacityInfo.getNetProductiveSeconds() / 60L)
                        + "，剩余可排量=" + (Objects.isNull(selectedResidualCapacityInfo)
                        ? 0 : selectedResidualCapacityInfo.getResidualCapacityQty()));
        PriorityTraceLogHelper.appendProcessLog(
                context, HISTORY_RESIDUAL_CAPACITY_PREFERENCE_LOG_TITLE,
                detail.toString().trim());
    }

    /**
     * 格式化班次索引。
     *
     * @param shift 班次
     * @return classN；班次为空时返回无
     */
    private String formatShiftIndex(LhShiftConfigVO shift) {
        return Objects.isNull(shift) || Objects.isNull(shift.getShiftIndex())
                ? "无" : "class" + shift.getShiftIndex();
    }

    /**
     * 使用StringBuilder格式化候选机台编码，避免日志输出额外创建中间List。
     *
     * @param candidates 候选机台
     * @return 机台编码列表文本
     */
    private String formatMachineCodes(List<MachineScheduleDTO> candidates) {
        if (CollectionUtils.isEmpty(candidates)) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder(
                Math.min(2048, Math.max(32, candidates.size() * 8)));
        builder.append('[');
        boolean first = true;
        for (MachineScheduleDTO candidate : candidates) {
            if (Objects.isNull(candidate) || StringUtils.isEmpty(candidate.getMachineCode())) {
                continue;
            }
            if (!first) {
                builder.append(',');
            }
            builder.append(candidate.getMachineCode());
            first = false;
        }
        return builder.append(']').toString();
    }

    /**
     * 判断候选机台是否已经在统一计划中命中历史剩余产能画像。
     *
     * @param candidate 候选机台
     * @param planMap 当前选机回合统一可开产计划
     * @return true-命中历史剩余产能优先池；false-未命中
     */
    private boolean isHistoryResidualCapacityCandidate(
            MachineScheduleDTO candidate,
            Map<String, NewSpecMachineAvailabilityPlan> planMap) {
        return Objects.nonNull(this.resolveHistoricalResidualCapacityInfo(
                candidate, planMap));
    }

    /**
     * 从当前选机回合统一计划中读取候选机台的历史剩余产能画像。
     *
     * @param candidate 候选机台
     * @param planMap 当前选机回合统一可开产计划
     * @return 历史剩余产能画像；候选未命中时返回null
     */
    private HistoricalResidualCapacityInfo resolveHistoricalResidualCapacityInfo(
            MachineScheduleDTO candidate,
            Map<String, NewSpecMachineAvailabilityPlan> planMap) {
        if (Objects.isNull(candidate) || StringUtils.isEmpty(candidate.getMachineCode())
                || CollectionUtils.isEmpty(planMap)) {
            return null;
        }
        NewSpecMachineAvailabilityPlan plan = planMap.get(candidate.getMachineCode());
        return Objects.isNull(plan) ? null : plan.getHistoricalResidualCapacityInfo();
    }

    /**
     * 格式化历史班次剩余产能候选及其来源层级。
     *
     * @param candidates 历史班次剩余产能候选
     * @param planMap 当前选机回合统一可开产计划
     * @return 包含来源班次、剩余区间、净时长、产能和层级的候选文本
     */
    private String formatHistoryResidualCapacityCandidates(
            List<MachineScheduleDTO> candidates,
            Map<String, NewSpecMachineAvailabilityPlan> planMap) {
        if (CollectionUtils.isEmpty(candidates)) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder(
                Math.min(8192, Math.max(256, candidates.size() * 128)));
        builder.append('[');
        boolean first = true;
        for (MachineScheduleDTO candidate : candidates) {
            HistoricalResidualCapacityInfo residualCapacityInfo =
                    this.resolveHistoricalResidualCapacityInfo(candidate, planMap);
            if (Objects.isNull(candidate) || Objects.isNull(residualCapacityInfo)) {
                continue;
            }
            if (!first) {
                builder.append(';');
            }
            builder.append("层级").append(residualCapacityInfo.getPriorityLevel())
                    .append(':').append(candidate.getMachineCode())
                    .append("{来源=").append(this.formatShiftIndex(
                    residualCapacityInfo.getSourceShift()))
                    .append(",区间=[")
                    .append(LhScheduleTimeUtil.formatDateTime(
                            residualCapacityInfo.getAvailableStartTime()))
                    .append(',')
                    .append(LhScheduleTimeUtil.formatDateTime(
                            residualCapacityInfo.getAvailableEndTime()))
                    .append("),净分钟=")
                    .append(residualCapacityInfo.getNetProductiveSeconds() / 60L)
                    .append(",可排量=")
                    .append(residualCapacityInfo.getResidualCapacityQty())
                    .append('}');
            first = false;
        }
        return builder.append(']').toString();
    }

    /**
     * 无副作用计算单台候选机台的真实可开产计划。
     *
     * <p>候选预演时间轴只用于准备完成和选机日志分析；正式候选时间轴在同一机台时间轴上
     * 重新应用正式生产门禁、首检、班次管控和设备计划产能。两条时间轴均不写入正式
     * 首检、日计划或机台占用资源，命中机台后仍由正式排产主链完成最终落地复核。</p>
     *
     * @param context 排程上下文
     * @param sku 当前待排 SKU
     * @param machine 候选机台
     * @param dayContext 当前业务日上下文
     * @param capacityCalculate 机台就绪时间计算策略
     * @param mouldChangeBalance 正式换模均衡策略
     * @param inspectionBalance 首检均衡策略
     * @param candidateProductionNotBeforeTime 候选预演门禁，仅包含 SKU 类型生产门禁
     * @param productionNotBeforeTime 正式生产门禁，包含有效胎胚最早可供时间
     * @param remainingQty 当前候选剩余量
     * @param totalScheduledQty 当前 SKU 已排量
     * @param addMachineProductionDate 当前追加机台生效日
     * @param isEnding 是否收尾
     * @return 携带准备、候选预演和正式生产三套时间口径的无副作用机台计划
     */
    private NewSpecMachineAvailabilityPlan resolveMachineAvailabilityPlan(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            MachineScheduleDTO machine,
            DayScheduleContext dayContext,
            ICapacityCalculateStrategy capacityCalculate,
            IMouldChangeBalanceStrategy mouldChangeBalance,
            IFirstInspectionBalanceStrategy inspectionBalance,
            Date candidateProductionNotBeforeTime,
            Date productionNotBeforeTime,
            int remainingQty,
            int totalScheduledQty,
            LocalDate addMachineProductionDate,
            boolean isEnding) {
        return this.resolveMachineAvailabilityPlan(
                context, sku, machine, dayContext, capacityCalculate, mouldChangeBalance,
                inspectionBalance, candidateProductionNotBeforeTime, productionNotBeforeTime,
                remainingQty, totalScheduledQty, addMachineProductionDate, isEnding, false);
    }

    /**
     * 无副作用计算单台候选机台的真实可开产计划，并按选机阶段决定是否允许历史准备时间轴。
     *
     * @param context 排程上下文
     * @param sku 当前待排SKU
     * @param machine 候选机台
     * @param dayContext 当前业务日上下文
     * @param capacityCalculate 机台就绪时间计算策略
     * @param mouldChangeBalance 正式换模均衡策略
     * @param inspectionBalance 首检均衡策略
     * @param candidateProductionNotBeforeTime 候选预演门禁
     * @param productionNotBeforeTime 正式生产门禁
     * @param remainingQty 当前候选剩余量
     * @param totalScheduledQty 当前SKU已排量
     * @param addMachineProductionDate 当前追加机台生效日
     * @param isEnding 是否收尾
     * @param allowHistoryResidualLookback 是否允许候选预演从当前业务日前一天内开始
     * @return 无副作用机台计划
     */
    private NewSpecMachineAvailabilityPlan resolveMachineAvailabilityPlan(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            MachineScheduleDTO machine,
            DayScheduleContext dayContext,
            ICapacityCalculateStrategy capacityCalculate,
            IMouldChangeBalanceStrategy mouldChangeBalance,
            IFirstInspectionBalanceStrategy inspectionBalance,
            Date candidateProductionNotBeforeTime,
            Date productionNotBeforeTime,
            int remainingQty,
            int totalScheduledQty,
            LocalDate addMachineProductionDate,
            boolean isEnding,
            boolean allowHistoryResidualLookback) {
        Date occupationEndTime = resolveMachineOccupationEndTime(
                context, sku, machine, dayContext.getDayShifts());
        Date machineReadyTime = capacityCalculate.calculateStartTime(
                context, machine.getMachineCode(), occupationEndTime);
        boolean takeoverWithoutMouldChange = context.isScheduleSubstitutionSku(sku)
                && Objects.nonNull(context.getScheduleSubstitutionDirective())
                && context.getScheduleSubstitutionDirective().isTakeoverWithoutMouldChange();
        boolean typeBlockRelation = !takeoverWithoutMouldChange
                && TypeBlockRelationUtil.isSameEmbryoAndSameMould(context, machine, sku);
        int switchDurationHours = takeoverWithoutMouldChange ? 0
                : typeBlockRelation
                ? LhScheduleTimeUtil.getTypeBlockChangeTotalHours(context)
                : LhScheduleTimeUtil.getMouldChangeTotalHours(context);
        Date switchReadyTime = resolveSpecifyReservedReadyTime(
                context, sku, machine.getMachineCode(), machineReadyTime);
        switchReadyTime = ShiftProductionControlUtil.resolveEarliestSwitchStartTime(
                context, switchReadyTime, sku);
        Date productionPreparationNotBeforeTime =
                this.resolveProductionPreparationNotBeforeTime(
                        context, dayContext, dayContext.getDayShifts(), candidateProductionNotBeforeTime,
                        addMachineProductionDate);
        boolean preparationLookbackAllowed = this.isProductionPreparationLookbackAllowed(
                context, dayContext, switchReadyTime, switchDurationHours,
                productionPreparationNotBeforeTime);
        List<LhShiftConfigVO> switchShifts = preparationLookbackAllowed
                ? context.getScheduleWindowShifts() : dayContext.getDayShifts();
        switchReadyTime = alignNewSpecSwitchReadyTimeToWindowStart(
                context, switchShifts, switchReadyTime);
        switchReadyTime = alignSpecialMaterialSubstitutionSwitchReadyTime(
                context, sku, machine.getMachineCode(), switchReadyTime);
        /*
         * 候选预演和正式落班共用不含胎胚门禁的准备时间轴。胎胚门禁只在正式生产起点
         * 应用，不能反向把候选机台的换模时间推迟到胎胚到位日。
         */
        if (preparationLookbackAllowed
                && Objects.nonNull(productionPreparationNotBeforeTime)) {
            switchReadyTime = delaySwitchReadyTimeCloseToProductionStart(
                    context, sku.getMaterialCode(), machine.getMachineCode(),
                    switchReadyTime, switchDurationHours,
                    productionPreparationNotBeforeTime);
        }
        if (!preparationLookbackAllowed) {
            switchReadyTime = alignSwitchReadyTimeByAddMachineDate(
                    context, sku, switchReadyTime, dayContext.getDayShifts(), totalScheduledQty,
                    addMachineProductionDate, isEnding, dayContext.getCurrentPhase());
        }
        if (Objects.isNull(switchReadyTime)) {
            return this.unavailablePlan(machine, "无法确定机台切换就绪时间",
                    occupationEndTime, machineReadyTime, productionNotBeforeTime,
                    candidateProductionNotBeforeTime);
        }
        /*
         * 选机日志展示用的换模/换活字块完成时间：从机台收尾时间出发，只避让停机与
         * 20:00-06:00 禁换模约束，不参与每日换模均衡配额、首检、胎胚可供时间、
         * 班次管控及生产日回看延后等正式排产逻辑。正式换模仍沿用下方 previewMouldChange
         * 的配额分配与时间轴。
         */
        Date traceChangeoverStartTime = this.allocateBasicMouldChangeStartTime(
                context, machine.getMachineCode(), occupationEndTime, switchDurationHours);
        Date traceChangeoverEndTime = Objects.isNull(traceChangeoverStartTime)
                ? null : LhScheduleTimeUtil.addHours(traceChangeoverStartTime, switchDurationHours);

        String inspectionScheduleType = typeBlockRelation
                ? ScheduleTypeEnum.TYPE_BLOCK.getCode() : ScheduleTypeEnum.NEW_SPEC.getCode();
        int machineMouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(machine);
        int runtimeShiftCapacity = ShiftCapacityResolverUtil.resolveRuntimeShiftCapacity(
                context, machine, sku.getShiftCapacity());
        /*
         * 生产日前跨日准备不参与换模均衡配额，但仍必须复用停机、维修、清洗、禁换模和首检
         * 资源校验。该时间轴只用于候选班次筛选，正式生产仍沿用下方原有时间轴重新计算。
         */
        Date preparationAvailableTime = null;
        LhShiftConfigVO preparationTargetShift = null;
        boolean preparationAvailable = false;
        if (preparationLookbackAllowed) {
            preparationAvailableTime = this.resolvePreparationAvailableTime(
                    context, sku, machine, dayContext, switchReadyTime, switchDurationHours,
                    productionPreparationNotBeforeTime,
                    runtimeShiftCapacity, machineMouldQty, inspectionScheduleType,
                    remainingQty, inspectionBalance);
            preparationTargetShift = this.resolvePreparationTargetShift(
                    context, preparationAvailableTime);
            preparationAvailable = Objects.nonNull(preparationAvailableTime)
                    && Objects.nonNull(preparationTargetShift);
        }
        Date changeoverStartTime = null;
        Date changeoverEndTime = null;
        FirstInspectionAllocationPlan inspectionPlan = null;
        Date currentSwitchReadyTime = switchReadyTime;
        boolean inspectionResourceMatched = takeoverWithoutMouldChange;
        for (int attempt = 0; attempt < NEW_SPEC_CHANGEOVER_PROBE_LIMIT; attempt++) {
            if (takeoverWithoutMouldChange) {
                changeoverStartTime = currentSwitchReadyTime;
            } else if (isChangeoverBalanceEnabled(context)) {
                changeoverStartTime = mouldChangeBalance.previewMouldChange(
                        context, machine.getMachineCode(), currentSwitchReadyTime,
                        switchDurationHours, sku,
                        this.resolveNewSpecChangeoverActionType(
                                context, sku, currentSwitchReadyTime,
                                dayContext.getCurrentPhase(), typeBlockRelation,
                                preparationLookbackAllowed),
                        dayContext.getDayEndTime());
            } else {
                changeoverStartTime = allocateBasicMouldChangeStartTime(
                        context, machine.getMachineCode(), currentSwitchReadyTime,
                        switchDurationHours);
            }
            if (Objects.isNull(changeoverStartTime)) {
                break;
            }
            changeoverEndTime = LhScheduleTimeUtil.addHours(
                    changeoverStartTime, switchDurationHours);
            if (dayContext.reachesOrPassesDayEnd(changeoverEndTime)
                    && !preparationLookbackAllowed) {
                return this.unavailablePlan(machine, "切换完成时间超出当前业务日",
                        occupationEndTime, machineReadyTime, productionNotBeforeTime,
                        candidateProductionNotBeforeTime);
            }
            if (takeoverWithoutMouldChange) {
                inspectionResourceMatched = true;
                break;
            }
            inspectionPlan = this.resolveFirstInspectionAllocationPlan(
                    context, sku, machine, context.getScheduleWindowShifts(),
                    changeoverStartTime, changeoverEndTime, runtimeShiftCapacity,
                    machineMouldQty, remainingQty, inspectionScheduleType);
            if (!inspectionPlan.isValid()) {
                /*
                 * 首检时间覆盖或实际产能校验失败时，不能只把正式生产向后移动。
                 * 整段切换准备从下一班重新预演，使该机台重新参加后续目标班次竞争。
                 */
                Date nextSwitchReadyTime = this.resolveNextFirstInspectionRetryReadyTime(
                        FirstInspectionQtyUtil.resolveAttributionShift(
                                context.getScheduleWindowShifts(), changeoverEndTime),
                        changeoverStartTime);
                if (Objects.nonNull(nextSwitchReadyTime)
                        && nextSwitchReadyTime.after(currentSwitchReadyTime)) {
                    currentSwitchReadyTime = nextSwitchReadyTime;
                    continue;
                }
                return new NewSpecMachineAvailabilityPlan(
                        machine, false, inspectionPlan.getInvalidReason(), occupationEndTime,
                        machineReadyTime, changeoverStartTime, changeoverEndTime,
                        productionNotBeforeTime, candidateProductionNotBeforeTime,
                        null, null, inspectionPlan, traceChangeoverEndTime,
                        preparationAvailableTime, preparationTargetShift, preparationAvailable);
            }
            LhShiftConfigVO quantityShift = inspectionPlan.getCountingShift();
            Date quantityAttributionTime = inspectionPlan.getInspectionQty() > 0
                    ? inspectionPlan.getInspectionEndTime()
                    : FirstInspectionQtyUtil.resolveFirstInspectionAttributionTime(
                            context, sku, context.getScheduleWindowShifts(),
                            changeoverEndTime, inspectionScheduleType);
            Date inspectionResourceTime = inspectionBalance.previewInspection(
                    context, machine.getMachineCode(), quantityAttributionTime);
            LhShiftConfigVO inspectionResourceShift = FirstInspectionQtyUtil
                    .resolveFirstInspectionAttributionShift(
                            context, sku, context.getScheduleWindowShifts(),
                            inspectionResourceTime, inspectionScheduleType);
            if (Objects.nonNull(quantityShift) && Objects.nonNull(inspectionResourceShift)
                    && Objects.equals(quantityShift.getShiftIndex(),
                    inspectionResourceShift.getShiftIndex())) {
                inspectionResourceMatched = true;
                break;
            }
            /*
             * 当前班次首检资源无法承接时，整段换模/换活字块及首检顺延到下一班重新预演。
             * 不能只移动正式生产，也不能锁住该机台原目标班次。
             */
            Date nextSwitchReadyTime = this.resolveNextFirstInspectionRetryReadyTime(
                    quantityShift, changeoverStartTime);
            if (Objects.isNull(nextSwitchReadyTime)
                    || !nextSwitchReadyTime.after(currentSwitchReadyTime)) {
                break;
            }
            currentSwitchReadyTime = nextSwitchReadyTime;
        }
        if (Objects.isNull(changeoverStartTime) || Objects.isNull(changeoverEndTime)) {
            return this.unavailablePlan(machine, "换模或换活字块无合法开始时间",
                    occupationEndTime, machineReadyTime, productionNotBeforeTime,
                    candidateProductionNotBeforeTime);
        }
        if (!inspectionResourceMatched) {
            return new NewSpecMachineAvailabilityPlan(
                    machine, false, "首检资源在当前业务日无可承接班次", occupationEndTime,
                    machineReadyTime, changeoverStartTime, changeoverEndTime,
                    productionNotBeforeTime, candidateProductionNotBeforeTime,
                    null, null, inspectionPlan, traceChangeoverEndTime,
                    preparationAvailableTime, preparationTargetShift, preparationAvailable);
        }

        Date preparationReadyTime = changeoverEndTime;
        boolean plannedRepairAffectingSwitch = ShiftCapacityResolverUtil.isPlannedRepairAffectingSwitch(
                context, context.getDevicePlanShutList(), machine.getMachineCode(), occupationEndTime,
                changeoverStartTime, changeoverEndTime);
        Date plannedRepairReadyTime = ShiftCapacityResolverUtil.resolvePlannedRepairProductionReadyTime(
                context, context.getDevicePlanShutList(), machine.getMachineCode(), occupationEndTime,
                changeoverStartTime, changeoverEndTime);
        if (plannedRepairAffectingSwitch && Objects.nonNull(plannedRepairReadyTime)
                && plannedRepairReadyTime.after(preparationReadyTime)) {
            preparationReadyTime = plannedRepairReadyTime;
        }
        Date productionStartTime = FirstInspectionQtyUtil.resolveTrialProductionStartTime(
                context, sku, dayContext.getDayShifts(), changeoverEndTime,
                preparationReadyTime, ScheduleTypeEnum.NEW_SPEC.getCode());
        productionStartTime = NewSpecEmbryoAvailableTimeResolver.resolveActualProductionStartTime(
                productionStartTime, candidateProductionNotBeforeTime);
        /*
         * 正规、小批量已无 SKU 生产门禁，试制/量试仍保留首次正计划日中班下限，
         * 胎胚可供时间继续取较晚值。真实开产时间必须在完整八班窗口内解析，
         * 不能再用当前业务日班次截断，否则 14:00/20:00 会被抬到 T+2 首班 22:00；
         * 候选是否落在当前业务日由下方 dayContext.contains 统一判定。
         */
        productionStartTime = this.resolveFirstActualProductionTime(
                context, machine, sku, changeoverStartTime, changeoverEndTime,
                productionStartTime,
                context.getScheduleWindowShifts(), runtimeShiftCapacity);
        if (Objects.isNull(productionStartTime)) {
            return new NewSpecMachineAvailabilityPlan(
                    machine, false, "班次管控后无可开产时间", occupationEndTime,
                    machineReadyTime, changeoverStartTime, changeoverEndTime,
                    productionNotBeforeTime, candidateProductionNotBeforeTime,
                    null, null, inspectionPlan, traceChangeoverEndTime,
                    preparationAvailableTime, preparationTargetShift, preparationAvailable);
        }
        productionStartTime = NewSpecEmbryoAvailableTimeResolver.resolveActualProductionStartTime(
                productionStartTime, candidateProductionNotBeforeTime);
        productionStartTime = this.resolveFirstActualProductionTime(
                context, machine, sku, changeoverStartTime, changeoverEndTime,
                productionStartTime,
                context.getScheduleWindowShifts(), runtimeShiftCapacity);
        boolean historyResidualLookbackStartAllowed =
                allowHistoryResidualLookback
                        && Objects.nonNull(productionStartTime)
                        && Objects.nonNull(dayContext.getDayStartTime())
                        && productionStartTime.before(dayContext.getDayStartTime())
                        && !productionStartTime.before(DateUtil.offsetDay(
                        dayContext.getDayStartTime(), -HISTORY_RESIDUAL_LOOKBACK_DAYS));
        if (Objects.isNull(productionStartTime)
                || (!dayContext.contains(productionStartTime)
                && !historyResidualLookbackStartAllowed)) {
            return new NewSpecMachineAvailabilityPlan(
                    machine, false, "当前业务日无可开产时间", occupationEndTime,
                    machineReadyTime, changeoverStartTime, changeoverEndTime,
                    productionNotBeforeTime, candidateProductionNotBeforeTime,
                    productionStartTime, null, inspectionPlan, traceChangeoverEndTime,
                    preparationAvailableTime, preparationTargetShift, preparationAvailable);
        }

        /*
         * 历史剩余产能选机允许候选准备时间轴从当前业务日前一天内开始，但正式候选产能
         * 仍只计算当前业务日班次。后续历史来源班次扫描会再次使用实时结果、设备窗口和
         * 完整一模产能确认标识；未命中历史池时继续回退普通当前班次候选。
         */

        boolean productionStartConstrained = Objects.nonNull(candidateProductionNotBeforeTime)
                && candidateProductionNotBeforeTime.after(changeoverEndTime);
        Map<Integer, Integer> capacityMap = this.calculateShiftCapacityMap(
                context, machine, sku, productionStartTime, changeoverStartTime,
                changeoverEndTime,
                dayContext.getDayShifts(), machineMouldQty, runtimeShiftCapacity,
                isEnding, productionStartConstrained);
        if (Objects.nonNull(inspectionPlan) && inspectionPlan.getInspectionQty() > 0) {
            capacityMap = FirstInspectionQtyUtil.applyFirstInspectionAllocationToCapacityMap(
                    context.getScheduleWindowShifts(), capacityMap, inspectionPlan);
        } else if (!takeoverWithoutMouldChange) {
            LhShiftConfigVO trialInspectionShift = FirstInspectionQtyUtil
                    .resolveFirstInspectionAttributionShift(
                            context, sku, dayContext.getDayShifts(), changeoverEndTime,
                            ScheduleTypeEnum.NEW_SPEC.getCode());
            capacityMap = FirstInspectionQtyUtil.applyFirstInspectionQtyToCapacityMap(
                    context, sku, dayContext.getDayShifts(), trialInspectionShift, capacityMap,
                    runtimeShiftCapacity, remainingQty, ScheduleTypeEnum.NEW_SPEC.getCode(),
                    machine.getMachineCode());
        }
        capacityMap = applyDailyStandardCapacityAdjust(
                context, sku, machine.getMachineCode(), dayContext.getDayShifts(),
                capacityMap, runtimeShiftCapacity);

        Date availableProductionTime = this.resolveFirstPositiveCapacityTime(
                context, machine, sku, changeoverStartTime, changeoverEndTime,
                dayContext.getDayShifts(), productionStartTime,
                capacityMap, runtimeShiftCapacity);
        LhShiftConfigVO targetShift = FirstInspectionQtyUtil.resolveAttributionShift(
                dayContext.getDayShifts(), availableProductionTime);
        boolean candidatePreviewAvailable = Objects.nonNull(availableProductionTime)
                && Objects.nonNull(targetShift);
        Date formalAvailableProductionTime = null;
        LhShiftConfigVO formalTargetShift = null;
        boolean formalAvailable = false;
        if (candidatePreviewAvailable) {
            formalAvailableProductionTime = this.resolveFormalAvailableProductionTime(
                    context, sku, machine, dayContext, changeoverStartTime, changeoverEndTime,
                    preparationReadyTime, availableProductionTime, productionNotBeforeTime,
                    runtimeShiftCapacity, machineMouldQty, inspectionPlan,
                    takeoverWithoutMouldChange, inspectionScheduleType, remainingQty, isEnding);
            formalTargetShift = FirstInspectionQtyUtil.resolveAttributionShift(
                    dayContext.getDayShifts(), formalAvailableProductionTime);
            formalAvailable = Objects.nonNull(formalAvailableProductionTime)
                    && Objects.nonNull(formalTargetShift);
        }
        if (!preparationLookbackAllowed) {
            // 非跨日准备场景下，准备完成时间与候选预演时间共用同一条准备时间轴。
            preparationAvailableTime = availableProductionTime;
            preparationTargetShift = targetShift;
            preparationAvailable = candidatePreviewAvailable;
        }
        boolean candidateAvailable = formalAvailable && preparationAvailable;
        String unavailableReason = !candidatePreviewAvailable
                ? "设备计划扣减后无候选预演产能班次"
                : !preparationAvailable ? "准备时间轴校验失败"
                : !formalAvailable ? "正式生产时间轴无可开产班次" : null;
        return new NewSpecMachineAvailabilityPlan(
                machine, candidateAvailable, unavailableReason,
                occupationEndTime, machineReadyTime, changeoverStartTime, changeoverEndTime,
                productionNotBeforeTime, candidateProductionNotBeforeTime,
                availableProductionTime, targetShift, inspectionPlan,
                traceChangeoverEndTime, preparationAvailableTime, preparationTargetShift,
                preparationAvailable,
                candidateAvailable ? formalAvailableProductionTime : null,
                candidateAvailable ? formalTargetShift : null);
    }

    /**
     * 解析候选机台在当前业务日的首个真实可开产时刻。
     *
     * <p>现有班次管控工具负责开停产有效窗口，本方法进一步复用设备停机、清洗、精度、
     * 维修及预热的不可生产区间并集，定位第一段至少能完成一个硫化周期的连续时间。
     * 该时刻既用于选机班次归属，也会由正式排产直接复用，避免设备计划只扣产能却仍把
     * 结果开始时间写在停机区间起点。</p>
     *
     * @param context 排程上下文
     * @param machine 候选机台
     * @param sku 当前 SKU
     * @param changeoverStartTime 换模或换活字块开始时间，用于沿用清洗重叠规则
     * @param changeoverEndTime 换模或换活字块真实完成时间，用于严格限定清洗重叠区间
     * @param requestedStartTime 换模、首检、维修预热及胎胚门禁取最晚后的请求时间
     * @param shifts 当前业务日班次
     * @param runtimeShiftCapacity 运行态班产
     * @return 首个真实可开产时间；当前业务日无完整硫化周期时返回 null
     */
    private Date resolveFirstActualProductionTime(
            LhScheduleContext context,
            MachineScheduleDTO machine,
            SkuScheduleDTO sku,
            Date changeoverStartTime,
            Date changeoverEndTime,
            Date requestedStartTime,
            List<LhShiftConfigVO> shifts,
            int runtimeShiftCapacity) {
        if (Objects.isNull(context) || Objects.isNull(machine) || Objects.isNull(sku)
                || Objects.isNull(requestedStartTime) || CollectionUtils.isEmpty(shifts)
                || runtimeShiftCapacity <= 0 || sku.getLhTimeSeconds() <= 0) {
            return null;
        }
        List<MachineCleaningWindowDTO> cleaningWindowList =
                this.resolveEffectiveCleaningWindowList(
                        context, machine.getMachineCode(), sku,
                        changeoverStartTime, changeoverEndTime);
        List<MachineMaintenanceWindowDTO> maintenanceWindowList =
                this.resolveMachineMaintenanceWindowList(
                        context, machine.getMachineCode());
        Date cursorTime = requestedStartTime;
        for (LhShiftConfigVO shift : shifts) {
            if (Objects.isNull(shift) || Objects.isNull(shift.getShiftStartDateTime())
                    || Objects.isNull(shift.getShiftEndDateTime())
                    || !cursorTime.before(shift.getShiftEndDateTime())) {
                continue;
            }
            Date shiftRequestedTime = cursorTime.after(shift.getShiftStartDateTime())
                    ? cursorTime : shift.getShiftStartDateTime();
            ShiftProductionControlDTO control = ShiftProductionControlUtil.resolveEffectiveControl(
                    context, shift, shiftRequestedTime);
            if (Objects.isNull(control) || !control.isCanSchedule()
                    || Objects.isNull(control.getEffectiveStartTime())
                    || Objects.isNull(control.getEffectiveEndTime())) {
                cursorTime = shift.getShiftEndDateTime();
                continue;
            }
            Date actualProductionTime = ShiftCapacityResolverUtil
                    .resolveFirstContinuousProductiveTime(
                            context.getDevicePlanShutList(), cleaningWindowList,
                            maintenanceWindowList, machine.getMachineCode(),
                            control.getEffectiveStartTime(), control.getEffectiveEndTime(),
                            sku.getLhTimeSeconds());
            if (Objects.nonNull(actualProductionTime)) {
                return actualProductionTime;
            }
            cursorTime = shift.getShiftEndDateTime();
        }
        return null;
    }

    /**
     * 在候选预演时间基础上计算正式可开产时间。
     *
     * <p>候选预演可以忽略胎胚最早可供时间，以便提前完成准备；正式候选班次则必须
     * 重新应用正式生产门禁。若正式门禁推迟到换模完成之后，沿用正式落班的首检和部分
     * 班次产能校正，不直接复用准备班次，也不写入任何正式资源。</p>
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param machine 候选机台
     * @param dayContext 当前业务日上下文
     * @param changeoverStartTime 正式换模开始时间
     * @param changeoverEndTime 正式换模结束时间
     * @param preparationReadyTime 正式生产前的维修/预热就绪时间
     * @param candidateAvailableProductionTime 候选预演可开产时间
     * @param productionNotBeforeTime 正式生产门禁
     * @param runtimeShiftCapacity 运行态班产
     * @param machineMouldQty 运行态模数
     * @param inspectionPlan 候选阶段首检计划
     * @param takeoverWithoutMouldChange 是否无换模接管
     * @param inspectionScheduleType 首检排程类型
     * @param remainingQty 当前候选目标量
     * @param isEnding 是否收尾
     * @return 正式可开产时间；正式时间轴无可用班次时返回 null
     */
    private Date resolveFormalAvailableProductionTime(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            MachineScheduleDTO machine,
            DayScheduleContext dayContext,
            Date changeoverStartTime,
            Date changeoverEndTime,
            Date preparationReadyTime,
            Date candidateAvailableProductionTime,
            Date productionNotBeforeTime,
            int runtimeShiftCapacity,
            int machineMouldQty,
            FirstInspectionAllocationPlan inspectionPlan,
            boolean takeoverWithoutMouldChange,
            String inspectionScheduleType,
            int remainingQty,
            boolean isEnding) {
        if (Objects.isNull(candidateAvailableProductionTime)
                || Objects.isNull(changeoverStartTime)
                || Objects.isNull(changeoverEndTime)
                || runtimeShiftCapacity <= 0 || machineMouldQty <= 0) {
            return null;
        }
        Date formalProductionStartTime =
                NewSpecEmbryoAvailableTimeResolver.resolveActualProductionStartTime(
                        candidateAvailableProductionTime, productionNotBeforeTime);
        formalProductionStartTime = this.resolveFirstActualProductionTime(
                context, machine, sku, changeoverStartTime, changeoverEndTime,
                formalProductionStartTime, context.getScheduleWindowShifts(), runtimeShiftCapacity);
        if (Objects.isNull(formalProductionStartTime)) {
            return null;
        }

        boolean formalProductionStartConstrained = Objects.nonNull(productionNotBeforeTime)
                && Objects.nonNull(preparationReadyTime)
                && productionNotBeforeTime.after(preparationReadyTime);
        if (formalProductionStartConstrained && !takeoverWithoutMouldChange) {
            formalProductionStartTime = this.resolveProductionGateConstrainedStartTime(
                    context, machine, sku, formalProductionStartTime, changeoverStartTime,
                    dayContext.getDayShifts(), machineMouldQty, runtimeShiftCapacity,
                    remainingQty, isEnding, inspectionScheduleType);
        }
        if (Objects.isNull(formalProductionStartTime)
                || !dayContext.contains(formalProductionStartTime)) {
            return null;
        }

        Map<Integer, Integer> formalCapacityMap = this.calculateShiftCapacityMap(
                context, machine, sku, formalProductionStartTime, changeoverStartTime,
                changeoverEndTime, dayContext.getDayShifts(), machineMouldQty,
                runtimeShiftCapacity, isEnding, formalProductionStartConstrained);
        if (formalProductionStartConstrained) {
            if (!takeoverWithoutMouldChange) {
                LhShiftConfigVO formalInspectionShift = FirstInspectionQtyUtil
                        .resolveFirstInspectionAttributionShift(
                                context, sku, dayContext.getDayShifts(),
                                formalProductionStartTime, inspectionScheduleType);
                formalCapacityMap = FirstInspectionQtyUtil
                        .applyEmbryoAvailableFirstInspectionCapacity(
                                context, sku, dayContext.getDayShifts(), formalInspectionShift,
                                formalCapacityMap, runtimeShiftCapacity, remainingQty,
                                inspectionScheduleType, machine.getMachineCode());
            }
        } else if (Objects.nonNull(inspectionPlan) && inspectionPlan.getInspectionQty() > 0) {
            formalCapacityMap = FirstInspectionQtyUtil.applyFirstInspectionAllocationToCapacityMap(
                    context.getScheduleWindowShifts(), formalCapacityMap, inspectionPlan);
        } else if (!takeoverWithoutMouldChange) {
            LhShiftConfigVO inspectionShift = FirstInspectionQtyUtil
                    .resolveFirstInspectionAttributionShift(
                            context, sku, dayContext.getDayShifts(), changeoverEndTime,
                            inspectionScheduleType);
            formalCapacityMap = FirstInspectionQtyUtil.applyFirstInspectionQtyToCapacityMap(
                    context, sku, dayContext.getDayShifts(), inspectionShift, formalCapacityMap,
                    runtimeShiftCapacity, remainingQty, inspectionScheduleType,
                    machine.getMachineCode());
        }
        formalCapacityMap = applyDailyStandardCapacityAdjust(
                context, sku, machine.getMachineCode(), dayContext.getDayShifts(),
                formalCapacityMap, runtimeShiftCapacity);
        return this.resolveFirstPositiveCapacityTime(
                context, machine, sku, changeoverStartTime, changeoverEndTime,
                dayContext.getDayShifts(), formalProductionStartTime,
                formalCapacityMap, runtimeShiftCapacity);
    }

    /**
     * 解析生产日前跨日准备完成时间。
     *
     * <p>准备时间轴只使用基础换模时间，不参与换模均衡配额；但仍复用基础换模中的停机、
     * 晚班禁换模约束，并通过首检资源和设备时间轴校验。该方法只做无副作用预演，不扣减
     * 首检数量、日计划或 SKU 中心账本。</p>
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param machine 候选机台
     * @param dayContext 当前业务日上下文
     * @param switchReadyTime 机台最早可切换时间
     * @param switchDurationHours 换模或换活字块耗时
     * @param productionNotBeforeTime 当前增机业务日或SKU门禁确定的正式生产下限
     * @param runtimeShiftCapacity 运行态班产
     * @param machineMouldQty 运行态模数
     * @param inspectionScheduleType 首检排程类型
     * @param remainingQty 当前候选目标量
     * @param inspectionBalance 首检均衡策略
     * @return 通过准备约束后的准备完成时间；无法完成准备时返回 null
     */
    private Date resolvePreparationAvailableTime(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            MachineScheduleDTO machine,
            DayScheduleContext dayContext,
            Date switchReadyTime,
            int switchDurationHours,
            Date productionNotBeforeTime,
            int runtimeShiftCapacity,
            int machineMouldQty,
            String inspectionScheduleType,
            int remainingQty,
            IFirstInspectionBalanceStrategy inspectionBalance) {
        if (Objects.isNull(context) || Objects.isNull(sku) || Objects.isNull(machine)
                || Objects.isNull(dayContext)
                || Objects.isNull(switchReadyTime) || switchDurationHours <= 0
                || runtimeShiftCapacity <= 0 || machineMouldQty <= 0
                || Objects.isNull(inspectionBalance)) {
            return null;
        }
        Date preparationStartTime = this.allocateBasicMouldChangeStartTime(
                context, machine.getMachineCode(), switchReadyTime, switchDurationHours);
        if (Objects.isNull(preparationStartTime)) {
            return null;
        }
        Date preparationCompleteTime = LhScheduleTimeUtil.addHours(
                preparationStartTime, switchDurationHours);
        if (!this.isProductionPreparationLookbackTimeline(
                context, dayContext, productionNotBeforeTime,
                preparationStartTime, preparationCompleteTime)) {
            return null;
        }
        FirstInspectionAllocationPlan inspectionPlan =
                this.resolveFirstInspectionAllocationPlan(
                        context, sku, machine, context.getScheduleWindowShifts(),
                        preparationStartTime, preparationCompleteTime, runtimeShiftCapacity,
                        machineMouldQty, remainingQty, inspectionScheduleType);
        if (!this.isPreparationInspectionResourceMatched(
                context, sku, machine, inspectionPlan, inspectionScheduleType, inspectionBalance)) {
            return null;
        }
        Date preparationReadyTime = preparationCompleteTime;
        boolean plannedRepairAffectingSwitch = ShiftCapacityResolverUtil
                .isPlannedRepairAffectingSwitch(
                        context, context.getDevicePlanShutList(), machine.getMachineCode(),
                        this.resolveMachineOccupationEndTime(
                                context, sku, machine, context.getScheduleWindowShifts()),
                        preparationStartTime, preparationCompleteTime);
        Date plannedRepairReadyTime = ShiftCapacityResolverUtil
                .resolvePlannedRepairProductionReadyTime(
                        context, context.getDevicePlanShutList(), machine.getMachineCode(),
                        this.resolveMachineOccupationEndTime(
                                context, sku, machine, context.getScheduleWindowShifts()),
                        preparationStartTime, preparationCompleteTime);
        if (plannedRepairAffectingSwitch && Objects.nonNull(plannedRepairReadyTime)
                && plannedRepairReadyTime.after(preparationReadyTime)) {
            preparationReadyTime = plannedRepairReadyTime;
        }
        return this.resolveFirstActualProductionTime(
                context, machine, sku, preparationStartTime, preparationCompleteTime,
                preparationReadyTime, context.getScheduleWindowShifts(), runtimeShiftCapacity);
    }

    /**
     * 校验准备阶段的首检资源是否可承接，不写入首检计数。
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param machine 候选机台
     * @param inspectionPlan 首检计划
     * @param inspectionScheduleType 首检排程类型
     * @param inspectionBalance 首检均衡策略
     * @return true-首检资源可承接；false-不可承接
     */
    private boolean isPreparationInspectionResourceMatched(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            MachineScheduleDTO machine,
            FirstInspectionAllocationPlan inspectionPlan,
            String inspectionScheduleType,
            IFirstInspectionBalanceStrategy inspectionBalance) {
        if (Objects.isNull(inspectionPlan) || !inspectionPlan.isValid()
                || Objects.isNull(inspectionBalance)) {
            return false;
        }
        if (inspectionPlan.getInspectionQty() <= 0) {
            return true;
        }
        Date quantityAttributionTime = inspectionPlan.getInspectionEndTime();
        LhShiftConfigVO quantityShift = inspectionPlan.getCountingShift();
        Date inspectionResourceTime = inspectionBalance.previewInspection(
                context, machine.getMachineCode(), quantityAttributionTime);
        LhShiftConfigVO inspectionResourceShift = FirstInspectionQtyUtil
                .resolveFirstInspectionAttributionShift(
                        context, sku, context.getScheduleWindowShifts(),
                        inspectionResourceTime, inspectionScheduleType);
        return Objects.nonNull(quantityShift) && Objects.nonNull(inspectionResourceShift)
                && Objects.equals(quantityShift.getShiftIndex(), inspectionResourceShift.getShiftIndex());
    }

    /**
     * 解析准备完成时间对应的班次。
     *
     * @param context 排程上下文
     * @param preparationAvailableTime 准备完成时间
     * @return 准备完成班次；无法归属时返回 null
     */
    private LhShiftConfigVO resolvePreparationTargetShift(
            LhScheduleContext context,
            Date preparationAvailableTime) {
        if (Objects.isNull(context) || Objects.isNull(preparationAvailableTime)) {
            return null;
        }
        return FirstInspectionQtyUtil.resolveAttributionShift(
                context.getScheduleWindowShifts(), preparationAvailableTime);
    }

    /**
     * 从设备计划扣减后的产能图中解析首个真实可开产时间。
     *
     * <p>产能图只表明某班仍有可排数量，不能说明班次起点已经能够连续完成一模。
     * 本方法按班次顺序检查所有正产能班次，并复用统一停机、清洗、精度、维修及预热
     * 时间轴定位首个实际生产时刻。清洗重叠只使用真实切换区间，胎胚门禁晚于切换完成
     * 时不得把两者之间的清洗误当成“与换模并行”而跳过。</p>
     *
     * @param context 排程上下文
     * @param machine 候选机台
     * @param sku 当前 SKU
     * @param changeoverStartTime 换模或换活字块开始时间
     * @param changeoverEndTime 换模或换活字块真实完成时间
     * @param shifts 当前业务日班次
     * @param requestedStartTime 已综合切换、首检、维修预热及胎胚门禁的请求时间
     * @param capacityMap 设备计划和首检扣减后的班次产能图
     * @param runtimeShiftCapacity 当前机台运行态班产
     * @return 首个具有正产能且可连续完成一模的真实开产时间；不存在时返回 null
     */
    private Date resolveFirstPositiveCapacityTime(
            LhScheduleContext context,
            MachineScheduleDTO machine,
            SkuScheduleDTO sku,
            Date changeoverStartTime,
            Date changeoverEndTime,
            List<LhShiftConfigVO> shifts,
            Date requestedStartTime,
            Map<Integer, Integer> capacityMap,
            int runtimeShiftCapacity) {
        for (LhShiftConfigVO shift : shifts) {
            if (Math.max(0, capacityMap.getOrDefault(shift.getShiftIndex(), 0)) <= 0
                    || !requestedStartTime.before(shift.getShiftEndDateTime())) {
                continue;
            }
            Date shiftCandidateTime = requestedStartTime.after(shift.getShiftStartDateTime())
                    ? requestedStartTime : shift.getShiftStartDateTime();
            /*
             * 当前班次总产能大于0，不代表班次起点已经可生产。
             * 例如14:00~18:00有清洗或停机时，中班仍有剩余产能，但真实开产
             * 必须是18:00。因此每个正产能班次再次复用设备计划时间轴，且只在
             * 当前班次的半开区间内寻找首个完整硫化周期。
             */
            Date actualProductionTime = this.resolveFirstActualProductionTime(
                    context, machine, sku, changeoverStartTime, changeoverEndTime,
                    shiftCandidateTime,
                    Collections.singletonList(shift), runtimeShiftCapacity);
            if (Objects.nonNull(actualProductionTime)) {
                return actualProductionTime;
            }
        }
        return null;
    }

    private String resolveNewSpecChangeoverActionType(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            Date switchReadyTime,
            DailySchedulePhase phase,
            boolean typeBlockRelation,
            boolean preparationLookbackAllowed) {
        if (typeBlockRelation) {
            return IMouldChangeBalanceStrategy.ACTION_TYPE_BLOCK_CHANGE;
        }
        if (preparationLookbackAllowed
                || isEarlyProductionTargetDayMouldChange(
                context, sku, switchReadyTime, phase)) {
            return IMouldChangeBalanceStrategy.ACTION_EARLY_PRODUCTION_NEW_SPEC_MOULD_CHANGE;
        }
        return IMouldChangeBalanceStrategy.ACTION_NEW_SPEC_MOULD_CHANGE;
    }

    private NewSpecMachineAvailabilityPlan unavailablePlan(
            MachineScheduleDTO machine,
            String reason,
            Date occupationEndTime,
            Date machineReadyTime,
            Date productionNotBeforeTime,
            Date candidateProductionNotBeforeTime) {
        return new NewSpecMachineAvailabilityPlan(
                machine, false, reason, occupationEndTime, machineReadyTime,
                null, null, productionNotBeforeTime, candidateProductionNotBeforeTime,
                null, null, null, null);
    }

    /**
     * 将逐班真实可开产计划写入过程日志，确保最终批次可还原候选筛选过程。
     */
    private void appendMachineAvailabilityProcessLog(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            DayScheduleContext dayContext,
            LhShiftConfigVO selectedShift,
            List<MachineScheduleDTO> selectedCandidates,
            Map<String, NewSpecMachineAvailabilityPlan> planMap) {
        StringBuilder detail = new StringBuilder(Math.max(512, planMap.size() * 160));
        boolean historyResidualCapacityApplied = selectedCandidates.stream()
                .filter(Objects::nonNull)
                .anyMatch(candidate -> this.isHistoryResidualCapacityCandidate(
                        candidate, planMap));
        PriorityTraceLogHelper.appendLine(detail,
                "业务日=" + dayContext.getScheduleDate() + "，SKU=" + sku.getMaterialCode()
                        + "，普通候选最早班次=" + (Objects.isNull(selectedShift)
                        ? "无" : "class" + selectedShift.getShiftIndex())
                        + "，历史剩余产能优先=" + (historyResidualCapacityApplied ? 1 : 0)
                        + "，实际优先候选=" + selectedCandidates.stream()
                        .map(MachineScheduleDTO::getMachineCode).collect(Collectors.toList()));
        for (NewSpecMachineAvailabilityPlan plan : planMap.values()) {
            HistoricalResidualCapacityInfo residualCapacityInfo =
                    plan.getHistoricalResidualCapacityInfo();
            PriorityTraceLogHelper.appendLine(detail,
                    "机台=" + plan.getMachine().getMachineCode()
                            + "，占用收尾=" + LhScheduleTimeUtil.formatDateTime(plan.getOccupationEndTime())
                            + "，机台就绪=" + LhScheduleTimeUtil.formatDateTime(plan.getMachineReadyTime())
                            + "，切换开始=" + LhScheduleTimeUtil.formatDateTime(plan.getChangeoverStartTime())
                            + "，切换结束=" + LhScheduleTimeUtil.formatDateTime(plan.getChangeoverEndTime())
                            + "，生产门禁=" + LhScheduleTimeUtil.formatDateTime(plan.getProductionNotBeforeTime())
                            + "，候选预演门禁=" + LhScheduleTimeUtil.formatDateTime(
                            plan.getCandidateProductionNotBeforeTime())
                            + "，准备完成=" + LhScheduleTimeUtil.formatDateTime(
                            plan.getPreparationAvailableTime())
                            + "，准备班次=" + (Objects.isNull(plan.getPreparationTargetShift())
                            ? "无" : "class" + plan.getPreparationTargetShift().getShiftIndex())
                            + "，准备可用=" + (plan.isPreparationAvailable() ? 1 : 0)
                            + "，首检计划=" + this.formatFirstInspectionAllocationPlan(
                            plan.getFirstInspectionPlan())
                            + "，候选预演可开产=" + LhScheduleTimeUtil.formatDateTime(
                            plan.getCandidateAvailableProductionTime())
                            + "，候选预演班次=" + (Objects.isNull(plan.getTargetShift())
                            ? "无" : "class" + plan.getTargetShift().getShiftIndex())
                            + "，正式可开产=" + LhScheduleTimeUtil.formatDateTime(
                            plan.getFormalAvailableProductionTime())
                            + "，正式归属班次=" + (Objects.isNull(plan.getFormalTargetShift())
                            ? "无" : "class" + plan.getFormalTargetShift().getShiftIndex())
                            + "，历史剩余产能标识="
                            + (plan.isHistoryResidualCapacityCandidate() ? 1 : 0)
                            + "，来源班次=" + (Objects.isNull(residualCapacityInfo)
                            ? "无" : this.formatShiftIndex(residualCapacityInfo.getSourceShift()))
                            + "，来源优先级=" + (Objects.isNull(residualCapacityInfo)
                            ? 0 : residualCapacityInfo.getPriorityLevel())
                            + "，剩余可用区间=" + (Objects.isNull(residualCapacityInfo)
                            ? "无" : "[" + LhScheduleTimeUtil.formatDateTime(
                            residualCapacityInfo.getAvailableStartTime()) + ","
                            + LhScheduleTimeUtil.formatDateTime(
                            residualCapacityInfo.getAvailableEndTime()) + ")")
                            + "，剩余净分钟=" + (Objects.isNull(residualCapacityInfo)
                            ? 0L : residualCapacityInfo.getNetProductiveSeconds() / 60L)
                            + "，剩余可排量=" + (Objects.isNull(residualCapacityInfo)
                            ? 0 : residualCapacityInfo.getResidualCapacityQty())
                            + "，正式可用=" + (plan.isAvailable() ? 1 : 0)
                            + "，原因=" + StringUtils.defaultIfEmpty(plan.getUnavailableReason(), "通过"));
        }
        PriorityTraceLogHelper.appendProcessLog(
                context, "新增SKU逐班真实可开产选机", detail.toString().trim());
    }

    /**
     * 格式化候选时间轴直接携带的首检分摊计划。
     *
     * <p>本方法只读取候选阶段已经计算完成的计划，不再次读取首检参数或重新分摊数量；
     * 因而选机日志中的首检区间、班次数量与候选真实可开产时间始终来自同一个时间轴。</p>
     *
     * @param plan 候选机台首检分摊计划
     * @return 可直接写入过程日志的首检计划摘要
     */
    private String formatFirstInspectionAllocationPlan(FirstInspectionAllocationPlan plan) {
        if (Objects.isNull(plan)) {
            return "无";
        }
        if (!plan.isValid()) {
            return "无效(" + StringUtils.defaultIfEmpty(plan.getInvalidReason(), "原因未知") + ")";
        }
        if (plan.getInspectionQty() <= 0) {
            return "首检条数=0";
        }
        StringBuilder detailBuilder = new StringBuilder(160);
        detailBuilder.append("总量=").append(plan.getInspectionQty())
                .append("，区间=[")
                .append(LhScheduleTimeUtil.formatDateTime(plan.getInspectionStartTime()))
                .append(',')
                .append(LhScheduleTimeUtil.formatDateTime(plan.getInspectionEndTime()))
                .append(")，分摊=");
        for (FirstInspectionShiftAllocation allocation : plan.getShiftAllocations()) {
            if (detailBuilder.charAt(detailBuilder.length() - 1) != '=') {
                detailBuilder.append(';');
            }
            detailBuilder.append("class").append(allocation.getShift().getShiftIndex())
                    .append(':').append(allocation.getQuantity())
                    .append('[')
                    .append(LhScheduleTimeUtil.formatDateTime(allocation.getOverlapStartTime()))
                    .append(',')
                    .append(LhScheduleTimeUtil.formatDateTime(allocation.getOverlapEndTime()))
                    .append(')');
        }
        return detailBuilder.toString();
    }

    /**
     * 选择当前实际尝试的机台，并同步回传同一次选机使用的候选顺序。
     *
     * @param context 排程上下文
     * @param sku 当前待选机SKU
     * @param candidateCache 当前SKU候选缓存，仅用于候选分组诊断复用已有产能结果
     * @param currentSelectableCandidates 已完成硬约束、当前回合排除及真实可开产班次筛选的候选列表
     * @param excludedMachineCodes 已排除机台编码
     * @param machineMatch 机台匹配策略
     * @param preferredTrialMachine 试制、量试或小批量限制作业定点预选机台
     * @param orderedCandidates 本次实际使用的候选顺序输出参数
     * @return 当前实际尝试的机台；无候选时返回null
     */
    private MachineScheduleDTO selectCandidateMachine(LhScheduleContext context,
                                                       SkuScheduleDTO sku,
                                                       NewSpecCandidateCache candidateCache,
                                                       List<MachineScheduleDTO> currentSelectableCandidates,
                                                       Set<String> excludedMachineCodes,
                                                       IMachineMatchStrategy machineMatch,
                                                       MachineScheduleDTO preferredTrialMachine,
                                                       List<MachineScheduleDTO> orderedCandidates) {
        List<MachineScheduleDTO> singleControlCandidates =
                new ArrayList<MachineScheduleDTO>(currentSelectableCandidates.size());
        List<MachineScheduleDTO> normalCandidates =
                new ArrayList<MachineScheduleDTO>(currentSelectableCandidates.size());
        for (MachineScheduleDTO candidate : currentSelectableCandidates) {
            if (Objects.isNull(candidate) || StringUtils.isEmpty(candidate.getMachineCode())
                    || excludedMachineCodes.contains(candidate.getMachineCode())) {
                continue;
            }
            if (isSingleControlMachine(context, candidate.getMachineCode())) {
                singleControlCandidates.add(candidate);
            } else {
                normalCandidates.add(candidate);
            }
        }
        logNewSpecMachineTypeSplit(context, sku, singleControlCandidates, normalCandidates,
                excludedMachineCodes, candidateCache);
        if (shouldOnlyUseSingleControlCandidate(context, sku)) {
            MachineScheduleDTO singleControlMachine = selectCandidateMachineFromScopedList(
                    context, sku, singleControlCandidates, machineMatch, preferredTrialMachine);
            if (singleControlMachine != null) {
                log.info("新增排产{}SKU仅尝试单控机台, materialCode: {}, machineCode: {}",
                        resolveNewSpecSkuType(sku), sku.getMaterialCode(), singleControlMachine.getMachineCode());
                fillSelectedCandidateOrder(singleControlCandidates, singleControlMachine, orderedCandidates);
                return singleControlMachine;
            }
            log.info("新增排产{}SKU单控候选均已排除，不回落普通机台, materialCode: {}",
                    resolveNewSpecSkuType(sku), sku.getMaterialCode());
            return null;
        }
        /*
         * 除“试制单模只能使用单控单边”这一硬约束外，不再按普通/单控机台拆分选择作用域。
         * 当前列表已经限定为同一目标班次，并按同胎胚、同模壳、同规格、胶囊共用、同英寸、
         * 相近英寸、收尾时间、机台编码完成八层排序；实际排产和选机日志必须共同复用这份顺序。
         */
        MachineScheduleDTO selectedMachine = this.selectCandidateMachineFromScopedList(
                context, sku, currentSelectableCandidates,
                machineMatch, preferredTrialMachine);
        this.fillSelectedCandidateOrder(
                currentSelectableCandidates, selectedMachine, orderedCandidates);
        return selectedMachine;
    }

    /**
     * 将当前选机逻辑确定的实际首选机台放到候选列表首位。
     * <p>该方法只复制当前有效作用域并移动已选机台，不重新过滤或排序；实际排产和日志都直接读取
     * 该列表第一台，保证日志首选机台与后续实际尝试机台一致。</p>
     *
     * @param scopedCandidates 当前已完成动态过滤的候选作用域
     * @param selectedMachine 当前选机逻辑确定的首选机台
     * @param orderedCandidates 本次实际使用的候选顺序输出参数
     */
    private void fillSelectedCandidateOrder(List<MachineScheduleDTO> scopedCandidates,
                                            MachineScheduleDTO selectedMachine,
                                            List<MachineScheduleDTO> orderedCandidates) {
        orderedCandidates.clear();
        if (Objects.isNull(selectedMachine) || CollectionUtils.isEmpty(scopedCandidates)) {
            return;
        }
        orderedCandidates.add(selectedMachine);
        for (MachineScheduleDTO candidate : scopedCandidates) {
            if (Objects.isNull(candidate)
                    || StringUtils.equals(candidate.getMachineCode(), selectedMachine.getMachineCode())) {
                continue;
            }
            orderedCandidates.add(candidate);
        }
    }

    /**
     * 在不改变实际首选的前提下补齐本轮正式可选机台顺序。
     *
     * <p>试制单模硬约束或历史/反向推荐可能在子集合内确定首选；普通中心选机则直接使用完整列表。
     * 本方法保留“实际首选 + 原作用域顺序”，再按 {@code currentSelectableCandidates} 的真实顺序
     * 追加未展示机台。方法只复制引用，不重新过滤、评分或选择。</p>
     *
     * @param currentSelectableCandidates 本轮正式可选候选
     * @param selectedMachine 原选机逻辑确定的首选机台
     * @param excludedMachineCodes 本轮动态排除机台编码
     * @param orderedCandidates 原选机作用域顺序及补齐后的输出列表
     */
    private void completeActualCandidateOrder(
            List<MachineScheduleDTO> currentSelectableCandidates,
            MachineScheduleDTO selectedMachine,
            Set<String> excludedMachineCodes,
            List<MachineScheduleDTO> orderedCandidates) {
        if (Objects.isNull(selectedMachine)) {
            orderedCandidates.clear();
            return;
        }
        List<MachineScheduleDTO> scopedOrder =
                new ArrayList<MachineScheduleDTO>(orderedCandidates);
        List<MachineScheduleDTO> completeOrder = new ArrayList<MachineScheduleDTO>(
                Math.max(4, PriorityTraceLogHelper.sizeOf(currentSelectableCandidates)));
        Set<String> addedMachineCodes = new LinkedHashSet<String>(
                Math.max(8, PriorityTraceLogHelper.sizeOf(currentSelectableCandidates) * 2));
        addActualTraceCandidate(
                selectedMachine, excludedMachineCodes, addedMachineCodes, completeOrder);
        for (MachineScheduleDTO candidate : scopedOrder) {
            addActualTraceCandidate(
                    candidate, excludedMachineCodes, addedMachineCodes, completeOrder);
        }
        if (!CollectionUtils.isEmpty(currentSelectableCandidates)) {
            for (MachineScheduleDTO candidate : currentSelectableCandidates) {
                addActualTraceCandidate(
                        candidate, excludedMachineCodes, addedMachineCodes, completeOrder);
            }
        }
        orderedCandidates.clear();
        orderedCandidates.addAll(completeOrder);
    }

    /**
     * 向正式候选日志顺序追加一台未重复且未动态排除的机台。
     *
     * @param candidate 待追加机台
     * @param excludedMachineCodes 本轮动态排除机台编码
     * @param addedMachineCodes 已追加机台编码
     * @param completeOrder 完整正式候选顺序
     */
    private void addActualTraceCandidate(
            MachineScheduleDTO candidate,
            Set<String> excludedMachineCodes,
            Set<String> addedMachineCodes,
            List<MachineScheduleDTO> completeOrder) {
        if (Objects.isNull(candidate) || StringUtils.isEmpty(candidate.getMachineCode())
                || (!CollectionUtils.isEmpty(excludedMachineCodes)
                && excludedMachineCodes.contains(candidate.getMachineCode()))
                || !addedMachineCodes.add(candidate.getMachineCode())) {
            return;
        }
        completeOrder.add(candidate);
    }

    /**
     * 在结果最终确认后构建本次实际命中的选机日志快照。
     *
     * <p>优先使用本轮暂存的选机输入（延迟构建）；未暂存输入时（兼容策略等边界）按当前轮
     * 正式候选补建，保证命中日志始终有快照可写。快照必须在本轮结果提交任何机台运行态与
     * 占用关系之前构建，否则会把本轮新增结果误记成前序占用。</p>
     *
     * @param context 排程上下文
     * @param sku 当前待选机 SKU
     * @param machineMatch 机台匹配策略
     * @param pendingTraceCandidates 暂存的有序候选列表，可能为 null
     * @param pendingTraceSelectedMachine 暂存的首选机台，可能为 null
     * @param pendingTraceDayEndTime 暂存的当日结束时间，可能为 null
     * @param pendingPriorityMetricSnapshotMap 正式模具分配前冻结的软排序指标
     * @param candidateAvailabilityPlanMap 当前选机回合的真实可开产计划缓存
     * @param currentOrderedCandidates 当前轮正式有序候选（暂存为空时的回退输入）
     * @param currentCandidateMachine 当前轮首选机台（回退输入）
     * @param currentDayEndTime 当前业务日结束时间（回退输入）
     * @return 当前选机时点的只读日志快照
     */
    private MachinePriorityTraceSnapshot buildConfirmedTraceSnapshot(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            IMachineMatchStrategy machineMatch,
            List<MachineScheduleDTO> pendingTraceCandidates,
            MachineScheduleDTO pendingTraceSelectedMachine,
            Date pendingTraceDayEndTime,
            Map<String, MachinePriorityMetricSnapshot> pendingPriorityMetricSnapshotMap,
            Map<String, NewSpecMachineAvailabilityPlan> candidateAvailabilityPlanMap,
            List<MachineScheduleDTO> currentOrderedCandidates,
            MachineScheduleDTO currentCandidateMachine,
            Date currentDayEndTime) {
        List<MachineScheduleDTO> traceCandidates = pendingTraceCandidates;
        MachineScheduleDTO traceSelectedMachine = pendingTraceSelectedMachine;
        Date traceDayEndTime = pendingTraceDayEndTime;
        if (Objects.isNull(traceCandidates) && Objects.isNull(traceSelectedMachine)) {
            // 没有暂存输入时按当前轮正式候选补建，保持既有兼容行为。
            traceCandidates = currentOrderedCandidates;
            traceSelectedMachine = currentCandidateMachine;
            traceDayEndTime = currentDayEndTime;
        }
        Map<String, Date> traceChangeoverEndTimeMap =
                this.resolveTraceChangeoverEndTimeMap(
                        traceCandidates, candidateAvailabilityPlanMap);
        Map<String, Date> realAvailableProductionTimeMap =
                this.resolveRealAvailableProductionTimeMap(
                        traceCandidates, candidateAvailabilityPlanMap);
        Map<String, Date> preparationAvailableTimeMap =
                this.resolvePreparationAvailableTimeMap(
                        traceCandidates, candidateAvailabilityPlanMap);
        return machineMatch.buildMachinePriorityTraceSnapshot(
                context, sku, traceCandidates, traceSelectedMachine,
                traceDayEndTime, getTargetScheduleQtyResolver(),
                pendingPriorityMetricSnapshotMap,
                traceChangeoverEndTimeMap, preparationAvailableTimeMap,
                realAvailableProductionTimeMap)
                .withTraceSkuType(this.resolveTraceSkuType(context, sku));
    }

    /**
     * 当日未形成实际结果时，构建并暂存最后一次选机日志快照。
     *
     * <p>有暂存输入时按最近一次真实候选构建；全程无真实候选时按空候选构建一次占用诊断
     * 快照（与旧逻辑一致），保证三天窗口收口未命中时仍能输出可对账的日志。</p>
     *
     * @param context 排程上下文
     * @param sku 当前待选机 SKU
     * @param machineMatch 机台匹配策略
     * @param pendingTraceCandidates 暂存的有序候选列表，可能为 null
     * @param pendingTraceSelectedMachine 暂存的首选机台，可能为 null
     * @param pendingTraceDayEndTime 暂存的当日结束时间，可能为 null
     * @param pendingPriorityMetricSnapshotMap 正式模具分配前冻结的软排序指标
     * @param candidateAvailabilityPlanMap 当前选机回合的真实可开产计划缓存
     * @param currentDayEndTime 当前业务日结束时间
     * @return 本次选机时点的只读日志快照
     */
    private MachinePriorityTraceSnapshot buildUnscheduledTraceSnapshot(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            IMachineMatchStrategy machineMatch,
            List<MachineScheduleDTO> pendingTraceCandidates,
            MachineScheduleDTO pendingTraceSelectedMachine,
            Date pendingTraceDayEndTime,
            Map<String, MachinePriorityMetricSnapshot> pendingPriorityMetricSnapshotMap,
            Map<String, NewSpecMachineAvailabilityPlan> candidateAvailabilityPlanMap,
            Date currentDayEndTime) {
        List<MachineScheduleDTO> traceCandidates = pendingTraceCandidates;
        MachineScheduleDTO traceSelectedMachine = pendingTraceSelectedMachine;
        Date traceDayEndTime = pendingTraceDayEndTime;
        if (Objects.isNull(traceCandidates) && Objects.isNull(traceSelectedMachine)) {
            // 全程无真实候选：按空候选构建，快照入口保持有值，便于未排原因诊断。
            traceCandidates = Collections.<MachineScheduleDTO>emptyList();
            traceDayEndTime = currentDayEndTime;
        }
        Map<String, Date> traceChangeoverEndTimeMap =
                this.resolveTraceChangeoverEndTimeMap(
                        traceCandidates, candidateAvailabilityPlanMap);
        Map<String, Date> realAvailableProductionTimeMap =
                this.resolveRealAvailableProductionTimeMap(
                        traceCandidates, candidateAvailabilityPlanMap);
        Map<String, Date> preparationAvailableTimeMap =
                this.resolvePreparationAvailableTimeMap(
                        traceCandidates, candidateAvailabilityPlanMap);
        return machineMatch.buildMachinePriorityTraceSnapshot(
                context, sku, traceCandidates, traceSelectedMachine,
                traceDayEndTime, getTargetScheduleQtyResolver(),
                pendingPriorityMetricSnapshotMap,
                traceChangeoverEndTimeMap, preparationAvailableTimeMap,
                realAvailableProductionTimeMap)
                .withTraceSkuType(this.resolveTraceSkuType(context, sku));
    }

    /**
     * 从当前选机回合的真实可开产计划缓存中提取日志候选的换模或换活字块完成时间。
     *
     * <p>该时间从机台收尾时间出发，只避让停机与20:00-06:00禁换模约束，不参与每日换模均衡
     * 配额，也不包含首检、胎胚可供时间、试制量试中班下限或设备计划产能，仅用于日志展示
     * “可开产时间（换模/换活字块完成时间）”。未参与逐班筛选的机台不会写入映射，
     * 日志侧回退为“无（未知班次）”。</p>
     *
     * @param traceCandidates 日志候选列表
     * @param candidateAvailabilityPlanMap 当前选机回合的真实可开产计划缓存
     * @return 机台编码到换模或换活字块完成时间的映射
     */
    private Map<String, Date> resolveTraceChangeoverEndTimeMap(
            List<MachineScheduleDTO> traceCandidates,
            Map<String, NewSpecMachineAvailabilityPlan> candidateAvailabilityPlanMap) {
        if (CollectionUtils.isEmpty(traceCandidates)
                || CollectionUtils.isEmpty(candidateAvailabilityPlanMap)) {
            return Collections.emptyMap();
        }
        Map<String, Date> traceChangeoverEndTimeMap =
                new LinkedHashMap<String, Date>(Math.max(4, traceCandidates.size() * 2));
        for (MachineScheduleDTO traceCandidate : traceCandidates) {
            if (Objects.isNull(traceCandidate)
                    || StringUtils.isEmpty(traceCandidate.getMachineCode())) {
                continue;
            }
            NewSpecMachineAvailabilityPlan availabilityPlan =
                    candidateAvailabilityPlanMap.get(traceCandidate.getMachineCode());
            if (Objects.isNull(availabilityPlan)) {
                continue;
            }
            traceChangeoverEndTimeMap.put(
                    traceCandidate.getMachineCode(),
                    availabilityPlan.getTraceChangeoverEndTime());
        }
        return traceChangeoverEndTimeMap;
    }

    /**
     * 从当前选机回合的正式时间轴缓存中提取日志候选的正式可开产时间。
     *
     * <p>该时间与正式候选班次筛选使用的 {@code formalTargetShift} 同源，综合换模、首检、
     * 生产门禁、班次管控及设备计划产能后的正式可开产时刻。未参与逐班筛选的机台不会写入
     * 映射，日志侧回退为“无（未知班次）”。</p>
     *
     * @param traceCandidates 日志候选列表
     * @param candidateAvailabilityPlanMap 当前选机回合的真实可开产计划缓存
     * @return 机台编码到正式可开产时间的映射
     */
    private Map<String, Date> resolveRealAvailableProductionTimeMap(
            List<MachineScheduleDTO> traceCandidates,
            Map<String, NewSpecMachineAvailabilityPlan> candidateAvailabilityPlanMap) {
        if (CollectionUtils.isEmpty(traceCandidates)
                || CollectionUtils.isEmpty(candidateAvailabilityPlanMap)) {
            return Collections.emptyMap();
        }
        Map<String, Date> realAvailableProductionTimeMap =
                new LinkedHashMap<String, Date>(Math.max(4, traceCandidates.size() * 2));
        for (MachineScheduleDTO traceCandidate : traceCandidates) {
            if (Objects.isNull(traceCandidate)
                    || StringUtils.isEmpty(traceCandidate.getMachineCode())) {
                continue;
            }
            NewSpecMachineAvailabilityPlan availabilityPlan =
                    candidateAvailabilityPlanMap.get(traceCandidate.getMachineCode());
            if (Objects.isNull(availabilityPlan)) {
                continue;
            }
            realAvailableProductionTimeMap.put(
                    traceCandidate.getMachineCode(),
                    availabilityPlan.getFormalAvailableProductionTime());
        }
        return realAvailableProductionTimeMap;
    }

    /**
     * 从当前选机回合的准备时间轴缓存中提取准备完成时间。
     *
     * @param traceCandidates 日志候选列表
     * @param candidateAvailabilityPlanMap 当前选机回合的时间轴缓存
     * @return 机台编码到准备完成时间的映射
     */
    private Map<String, Date> resolvePreparationAvailableTimeMap(
            List<MachineScheduleDTO> traceCandidates,
            Map<String, NewSpecMachineAvailabilityPlan> candidateAvailabilityPlanMap) {
        if (CollectionUtils.isEmpty(traceCandidates)
                || CollectionUtils.isEmpty(candidateAvailabilityPlanMap)) {
            return Collections.emptyMap();
        }
        Map<String, Date> preparationAvailableTimeMap =
                new LinkedHashMap<String, Date>(Math.max(4, traceCandidates.size() * 2));
        for (MachineScheduleDTO traceCandidate : traceCandidates) {
            if (Objects.isNull(traceCandidate)
                    || StringUtils.isEmpty(traceCandidate.getMachineCode())) {
                continue;
            }
            NewSpecMachineAvailabilityPlan availabilityPlan =
                    candidateAvailabilityPlanMap.get(traceCandidate.getMachineCode());
            if (Objects.isNull(availabilityPlan)) {
                continue;
            }
            preparationAvailableTimeMap.put(
                    traceCandidate.getMachineCode(), availabilityPlan.getPreparationAvailableTime());
        }
        return preparationAvailableTimeMap;
    }

    /**
     * 过滤本轮已经排除的候选机台。
     *
     * @param candidates 候选机台
     * @param excludedMachineCodes 已排除机台编码
     * @return 可继续参与本轮选机的候选机台
     */
    private List<MachineScheduleDTO> filterExcludedCandidates(List<MachineScheduleDTO> candidates,
                                                              Set<String> excludedMachineCodes) {
        if (CollectionUtils.isEmpty(candidates) || CollectionUtils.isEmpty(excludedMachineCodes)) {
            return candidates;
        }
        List<MachineScheduleDTO> filteredCandidates = new ArrayList<>(candidates.size());
        for (MachineScheduleDTO candidate : candidates) {
            if (candidate == null || StringUtils.isEmpty(candidate.getMachineCode())
                    || excludedMachineCodes.contains(candidate.getMachineCode())) {
                continue;
            }
            filteredCandidates.add(candidate);
        }
        return filteredCandidates;
    }

    /**
     * 校验提前生产候选是否会突破同结构计划硫化机台数。
     *
     * <p>本校验只绑定 {@link DailySchedulePhase#EARLY_PRODUCTION}。结构已达到计划机台数时，
     * 已经计入同结构的物理机台仍可复用，只有会新增物理机台的候选返回限制原因。
     * 调用位置位于候选确定后、模具及其它生产资源扣减前，保证拒绝时不产生资源副作用。</p>
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日上下文
     * @param sku 当前提前生产 SKU
     * @param candidateMachineCode 当前候选机台编码；为空表示当前无真实可复用候选，仅解析失败原因
     * @return 空串表示允许；非空表示禁止提前生产的明确原因
     */
    private String resolveEarlyProductionStructureMachineLimitReason(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            SkuScheduleDTO sku,
            String candidateMachineCode) {
        if (Objects.isNull(context) || Objects.isNull(dayContext) || Objects.isNull(sku)
                || dayContext.getCurrentPhase() != DailySchedulePhase.EARLY_PRODUCTION) {
            return null;
        }
        EarlyProductionRuntimePlan runtimePlan =
                context.getEarlyProductionRuntimePlan(sku);
        if (Objects.isNull(runtimePlan) || !runtimePlan.isActive()
                || Objects.isNull(runtimePlan.getDecision())
                || !runtimePlan.getDecision().isEarlyProduction()) {
            return null;
        }
        LocalDate currentDate = dayContext.getScheduleDate();
        LocalDate futurePlanDate = runtimePlan.getFuturePlanDate();
        if (StringUtils.isNotEmpty(candidateMachineCode)
                && EarlyProductionChecker.canUseMachineForEarlyProduction(
                context, sku, currentDate, futurePlanDate,
                candidateMachineCode)) {
            return null;
        }
        int scheduledStructureMachineCount =
                context.getStructureScheduledMachineCount(
                        currentDate, sku.getStructureName());
        int planMachineCount =
                EarlyProductionChecker.resolveEffectiveStructurePlanMachineCount(
                        context, sku, currentDate, futurePlanDate);
        /*
         * 没有实际候选时，本方法只在结构确已达到计划数后补充组合失败原因；结构尚有新增
         * 机台额度时继续沿用原选机失败原因，避免把普通资源不足误报成结构上限限制。
         */
        if (StringUtils.isEmpty(candidateMachineCode)
                && (planMachineCount <= 0
                || scheduledStructureMachineCount < planMachineCount)) {
            return null;
        }
        String physicalMachineCode =
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                        candidateMachineCode);
        StringBuilder reasonBuilder = new StringBuilder(192)
                .append(EARLY_PRODUCTION_STRUCTURE_MACHINE_LIMIT_REASON)
                .append("，结构=").append(PriorityTraceLogHelper.safeText(sku.getStructureName()))
                .append("，当前已排物理机台数=").append(scheduledStructureMachineCount)
                .append("，计划机台数=").append(planMachineCount);
        if (StringUtils.isEmpty(candidateMachineCode)) {
            reasonBuilder.append("，当前无可复用的同结构物理机台");
        } else {
            reasonBuilder.append("，候选物理机台=")
                    .append(PriorityTraceLogHelper.safeText(physicalMachineCode));
        }
        return reasonBuilder.toString();
    }

    /**
     * 记录提前生产因结构计划机台数达到上限而排除候选机台的过程日志。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日上下文
     * @param sku 当前提前生产 SKU
     * @param candidateMachineCode 被拒绝的候选机台编码；为空表示没有真实可复用候选
     * @param reason 明确限制原因
     */
    private void appendEarlyProductionStructureMachineLimitLog(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            SkuScheduleDTO sku,
            String candidateMachineCode,
            String reason) {
        if (Objects.isNull(context) || Objects.isNull(dayContext) || Objects.isNull(sku)) {
            return;
        }
        EarlyProductionRuntimePlan runtimePlan =
                context.getEarlyProductionRuntimePlan(sku);
        LocalDate futurePlanDate = Objects.isNull(runtimePlan)
                ? null : runtimePlan.getFuturePlanDate();
        int planMachineCount =
                EarlyProductionChecker.resolveEffectiveStructurePlanMachineCount(
                        context, sku, dayContext.getScheduleDate(), futurePlanDate);
        int scheduledStructureMachineCount =
                context.getStructureScheduledMachineCount(
                        dayContext.getScheduleDate(), sku.getStructureName());
        String physicalMachineCode =
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                        candidateMachineCode);
        String detail = new StringBuilder(384)
                .append("batchNo=").append(PriorityTraceLogHelper.safeText(context.getBatchNo()))
                .append(", scheduleDate=")
                .append(LhScheduleTimeUtil.formatDate(context.getScheduleTargetDate()))
                .append(", currentDate=").append(dayContext.getScheduleDate())
                .append(", futurePlanDate=").append(PriorityTraceLogHelper.safeText(futurePlanDate))
                .append(", phase=").append(DailySchedulePhase.EARLY_PRODUCTION)
                .append(", materialCode=").append(sku.getMaterialCode())
                .append(", structureName=").append(PriorityTraceLogHelper.safeText(sku.getStructureName()))
                .append(", candidateMachineCode=")
                .append(PriorityTraceLogHelper.safeText(candidateMachineCode))
                .append(", physicalMachineCode=").append(PriorityTraceLogHelper.safeText(physicalMachineCode))
                .append(", scheduledStructureMachineCount=").append(scheduledStructureMachineCount)
                .append(", planMachineCount=").append(planMachineCount)
                .append(", result=REJECT")
                .append(", reason=").append(PriorityTraceLogHelper.safeText(reason))
                .toString();
        log.info("提前生产结构机台数限制, {}", detail);
        PriorityTraceLogHelper.appendProcessLog(
                context, "提前生产结构机台数限制", detail);
    }

    /**
     * 形成当前真正进入新增选机排序和尝试流程的候选机台列表。
     * <p>硬约束已经由机台匹配策略完成；本方法只排除本轮已失败、已使用及结构收尾对齐
     * 不允许的机台。禁止继续使用旧窗口产能近似值提前删除候选，所有硬候选都必须进入
     * 统一真实可开产时间计算，再由当日逐班筛选决定是否可选。</p>
     *
     * @param context 排程上下文
     * @param sku 当前待选机SKU
     * @param candidates 硬约束过滤后的候选机台
     * @param excludedMachineCodes 当前SKU已排除或已使用机台编码
     * @param structureAlignmentExcludedReasonMap 当前实时结构收尾对齐排除原因
     * @return 当前真实可选候选机台列表
     */
    private List<MachineScheduleDTO> filterCurrentSelectableCandidates(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            List<MachineScheduleDTO> candidates,
            Set<String> excludedMachineCodes,
            Map<String, String> structureAlignmentExcludedReasonMap) {
        if (CollectionUtils.isEmpty(candidates)) {
            return Collections.emptyList();
        }
        if (Objects.nonNull(structureAlignmentExcludedReasonMap)) {
            structureAlignmentExcludedReasonMap.clear();
        }
        List<MachineScheduleDTO> selectableCandidates =
                new ArrayList<MachineScheduleDTO>(candidates.size());
        for (MachineScheduleDTO candidate : candidates) {
            if (Objects.isNull(candidate) || StringUtils.isEmpty(candidate.getMachineCode())
                    || (!CollectionUtils.isEmpty(excludedMachineCodes)
                    && excludedMachineCodes.contains(candidate.getMachineCode()))) {
                continue;
            }
            /*
             * 结构收尾对齐只改变机台可用性，不改变候选顺序：
             * 触发时同结构与确认无实时排程归属的真实空机放行，不同结构及运行态数据异常机台排除；
             * 未触发时保持原选机逻辑。
             */
            StructureEndingAlignmentDecision structureEndingAlignmentDecision =
                    structureEndingAlignmentService.evaluateCandidate(context, sku, candidate);
            if (!structureEndingAlignmentDecision.isAllowed()) {
                if (Objects.nonNull(structureAlignmentExcludedReasonMap)) {
                    structureAlignmentExcludedReasonMap.put(
                            candidate.getMachineCode(),
                            this.buildStructureEndingAlignmentExcludedReason(
                                    structureEndingAlignmentDecision));
                }
                continue;
            }
            selectableCandidates.add(candidate);
        }
        return selectableCandidates;
    }

    /**
     * 构建结构收尾对齐候选排除原因。
     *
     * @param decision 结构收尾对齐判断结果
     * @return 可用于选机日志和最终未排原因定位的排除说明
     */
    private String buildStructureEndingAlignmentExcludedReason(
            StructureEndingAlignmentDecision decision) {
        if (Objects.isNull(decision)) {
            return "结构收尾对齐排除";
        }
        StringBuilder reasonBuilder = new StringBuilder(160);
        reasonBuilder.append("排除原因=结构收尾对齐：")
                .append(PriorityTraceLogHelper.safeText(decision.getExcludedReason()))
                .append(", 前物料=")
                .append(PriorityTraceLogHelper.safeText(decision.getPreviousMaterialCode()))
                .append(", 前物料结构=")
                .append(PriorityTraceLogHelper.safeText(decision.getPreviousStructureName()))
                .append(", 同结构在机数=")
                .append(decision.getInMachineCount())
                .append(", 最低机台数=")
                .append(decision.getMinimumMachineCount());
        return reasonBuilder.toString();
    }

    /**
     * 校验新增机台的模具资源并预占模具。
     * <p>增机台会让同一SKU同时占用多台机台，必须按候选机台模数扣减可用模具数量。
     * 如果模具不足，只能跳过当前候选机台继续尝试后续机台，不能强行生成不满足模具条件的排程结果。</p>
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param candidateMachine 候选机台
     * @param originalAddMachineCount 原候选增机台数量
     * @param actualAllowedAddMachineCount 已成功落地的增机台数量
     * @return 模具资源分配结果
     */
    private MouldResourceAllocationResult tryAllocateMouldResourceForAddMachine(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            MachineScheduleDTO candidateMachine,
            int originalAddMachineCount,
            int actualAllowedAddMachineCount) {
        if (Objects.nonNull(context) && Objects.isNull(context.getCurrentScheduleDate())) {
            // 非日驱动调用没有明确业务日时，才允许按 SKU dayN 或窗口 T 日兜底初始化日期。
            refreshCurrentScheduleDate(context, null, sku, null);
        }
        MouldResourceContext mouldResourceContext = resolveMouldResourceContext(context);
        List<String> forcedMouldCodeList = context.isScheduleSubstitutionSku(sku)
                && Objects.nonNull(context.getScheduleSubstitutionDirective())
                ? context.getScheduleSubstitutionDirective()
                .resolveForcedMouldCodes(candidateMachine.getMachineCode())
                : Collections.<String>emptyList();
        List<String> allowedRelocationMouldCodeList =
                context.isScheduleSubstitutionSku(sku)
                        && Objects.nonNull(context.getScheduleSubstitutionDirective())
                        && context.getScheduleSubstitutionDirective().isContinuationRelocation()
                        ? context.getScheduleSubstitutionDirective()
                        .getAllowedRelocationMouldCodeList()
                        : Collections.<String>emptyList();
        /*
         * 联动置换正式提交必须复用预演确认的精确模具：
         * A 只能继承原机台整套共用模具，B 只能使用预演命中的剩余模具。
         * B 迁移预演只从协调器已排除占用、预占、禁用、不可用及转交模具后的剩余集合中分配。
         */
        MouldResourceAllocationResult allocationResult;
        if (!CollectionUtils.isEmpty(forcedMouldCodeList)) {
            allocationResult = mouldResourceContext.tryAllocateExact(
                    sku.getMaterialCode(), candidateMachine.getMachineCode(),
                    forcedMouldCodeList);
        } else if (!CollectionUtils.isEmpty(allowedRelocationMouldCodeList)) {
            allocationResult = mouldResourceContext.tryAllocateFromAllowed(
                    sku.getMaterialCode(), candidateMachine.getMachineCode(),
                    allowedRelocationMouldCodeList);
        } else {
            allocationResult = mouldResourceContext.tryAllocate(
                    sku.getMaterialCode(), candidateMachine.getMachineCode());
        }
        String productionType = sku.isContinuousCompensationSku() ? "续作排产" : "新增排产";
        if (allocationResult.isAllowed()) {
            log.debug("SKU增机台模具资源校验通过, materialCode: {}, scheduleDate: {}, productionType: {}, "
                            + "machineCode: {}, machineMouldType: {}, requiredMouldQty: {}, "
                            + "availableMouldQty: {}, occupiedMouldQty: {}, remainingAvailableMouldQty: {}, "
                            + "releasedMouldCodes: {}, allocatedMouldCodes: {}",
                    sku.getMaterialCode(), LhScheduleTimeUtil.formatDate(context.getCurrentScheduleDate()), productionType,
                    candidateMachine.getMachineCode(), resolveMachineMouldTypeText(allocationResult.getRequiredMouldQty()),
                    allocationResult.getRequiredMouldQty(), allocationResult.getAvailableMouldQty(),
                    allocationResult.getOccupiedMouldQty(), allocationResult.getRemainingAvailableMouldQty(),
                    allocationResult.getReleasedMouldCodeList(), allocationResult.getAllocatedMouldCodeList());
            return allocationResult;
        }
        log.info("SKU增机台模具资源不足跳过候选机台, materialCode: {}, scheduleDate: {}, productionType: {}, "
                        + "originalAddMachineCount: {}, actualAllowedAddMachineCount: {}, candidateMachineCode: {}, "
                        + "machineMouldType: {}, requiredMouldQty: {}, availableMouldQty: {}, occupiedMouldQty: {}, "
                        + "remainingAvailableMouldQty: {}, occupiedMouldCodes: {}, unavailableMouldCodes: {}, skipReason: {}",
                sku.getMaterialCode(), LhScheduleTimeUtil.formatDate(context.getCurrentScheduleDate()), productionType,
                originalAddMachineCount, actualAllowedAddMachineCount, candidateMachine.getMachineCode(),
                resolveMachineMouldTypeText(allocationResult.getRequiredMouldQty()),
                allocationResult.getRequiredMouldQty(), allocationResult.getAvailableMouldQty(),
                allocationResult.getOccupiedMouldQty(), allocationResult.getRemainingAvailableMouldQty(),
                allocationResult.getOccupiedMouldCodeList(), allocationResult.getUnavailableMouldCodeList(),
                allocationResult.getSkipReason().getDescription());
        return allocationResult;
    }

    /**
     * 无副作用预演候选机台的模具分配结果。
     *
     * <p>续作释放机台是否能够原样重新启用，必须比较机台当前整套绑定模具与本次候选
     * 将要使用的整套模具。这里只读取现有模具资源上下文，不释放、不重新绑定，也不占用
     * 其他SKU的模具；真正的普通新增候选仍由正式分配入口完成资源变更。</p>
     *
     * @param context 排程上下文
     * @param sku 当前候选SKU
     * @param candidateMachine 候选机台
     * @return 与正式普通分配同口径的无副作用预演结果
     */
    private MouldResourceAllocationResult previewMouldResourceForAddMachine(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            MachineScheduleDTO candidateMachine) {
        if (Objects.isNull(context) || Objects.isNull(sku)
                || Objects.isNull(candidateMachine)
                || StringUtils.isEmpty(sku.getMaterialCode())
                || StringUtils.isEmpty(candidateMachine.getMachineCode())) {
            return null;
        }
        return this.resolveMouldResourceContext(context).previewAllocate(
                sku.getMaterialCode(), candidateMachine.getMachineCode());
    }

    /**
     * 获取新增链路模具资源上下文。
     *
     * @param context 排程上下文
     * @return 模具资源上下文
     */
    private MouldResourceContext resolveMouldResourceContext(LhScheduleContext context) {
        if (context.getMouldResourceContext() == null) {
            context.setMouldResourceContext(MouldResourceContext.from(context));
        }
        return context.getMouldResourceContext();
    }

    /**
     * 新增SKU分配模具前刷新上下文当前业务日期。
     * <p>模具资源上下文本身只负责占用和释放，不负责推导排程日期；
     * 因此在策略层进入模具预占前，先把当前候选机台所属业务日写入排程上下文。</p>
     *
     * @param context 排程上下文
     * @param currentBusinessDate 日驱动循环已经确定的当前业务日，非空时优先级最高
     * @param sku 当前新增SKU
     * @param preferredProductionDate dayN扩机台推导出的当前候选生效业务日
     */
    private void refreshCurrentScheduleDate(LhScheduleContext context,
                                            LocalDate currentBusinessDate,
                                            SkuScheduleDTO sku,
                                            LocalDate preferredProductionDate) {
        Date currentScheduleDate = resolveCurrentScheduleDate(
                context, currentBusinessDate, sku, preferredProductionDate);
        if (Objects.nonNull(context) && Objects.nonNull(currentScheduleDate)) {
            context.setCurrentScheduleDate(currentScheduleDate);
        }
    }

    /**
     * 解析新增SKU模具预占前应写入上下文的当前业务日期。
     *
     * @param context 排程上下文
     * @param currentBusinessDate 日驱动循环已经确定的当前业务日，非空时优先级最高
     * @param sku 当前新增SKU
     * @param preferredProductionDate dayN扩机台推导出的当前候选生效业务日
     * @return 当前业务日期，取不到时返回null
     */
    private Date resolveCurrentScheduleDate(LhScheduleContext context,
                                            LocalDate currentBusinessDate,
                                            SkuScheduleDTO sku,
                                            LocalDate preferredProductionDate) {
        /*
         * 模具到货可用性必须跟随当前日循环。dayN 增机日只用于旧调用的回退，不能在 T+1/T+2
         * 排程时把上下文倒写回 SKU 第一条日计划或某个历史增机日期。
         */
        if (Objects.nonNull(currentBusinessDate)) {
            return toDate(currentBusinessDate);
        }
        if (Objects.nonNull(preferredProductionDate)) {
            return toDate(preferredProductionDate);
        }
        if (Objects.nonNull(sku) && !CollectionUtils.isEmpty(sku.getDailyPlanQuotaMap())) {
            for (LocalDate productionDate : sku.getDailyPlanQuotaMap().keySet()) {
                if (Objects.nonNull(productionDate)) {
                    return toDate(productionDate);
                }
            }
        }
        if (Objects.nonNull(context) && Objects.nonNull(context.getScheduleDate())) {
            return LhScheduleTimeUtil.clearTime(context.getScheduleDate());
        }
        return null;
    }

    /**
     * 将业务日期转换为系统默认时区下的当天零点。
     *
     * @param productionDate 业务日期
     * @return 当天零点日期
     */
    private Date toDate(LocalDate productionDate) {
        if (Objects.isNull(productionDate)) {
            return null;
        }
        return Date.from(productionDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 释放候选机台预占模具。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param allocationResult 模具资源分配结果
     */
    private void rollbackMouldResourceAllocation(LhScheduleContext context,
                                                 SkuScheduleDTO sku,
                                                 MouldResourceAllocationResult allocationResult) {
        if (context == null || sku == null || allocationResult == null || !allocationResult.isAllowed()) {
            return;
        }
        resolveMouldResourceContext(context).release(sku.getMaterialCode(), allocationResult);
    }

    /**
     * 回滚单控整机 L/R 两侧模具预占。
     * <p>正规 SKU 使用单控机台时，主侧和副侧是同一个物理整机排产单元；
     * 任一后续约束失败，都必须同时释放两侧已经预占的模具，避免留下半边资源占用。</p>
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param primaryAllocationResult 主侧模具分配结果
     * @param pairAllocationResult 配对侧模具分配结果
     */
    private void rollbackMouldResourceAllocation(LhScheduleContext context,
                                                 SkuScheduleDTO sku,
                                                 MouldResourceAllocationResult primaryAllocationResult,
                                                 MouldResourceAllocationResult pairAllocationResult) {
        rollbackMouldResourceAllocation(context, sku, pairAllocationResult);
        rollbackMouldResourceAllocation(context, sku, primaryAllocationResult);
    }

    /**
     * 解析机台模数文本。
     *
     * @param requiredMouldQty 所需模具数量
     * @return 单模/双模
     */
    private String resolveMachineMouldTypeText(int requiredMouldQty) {
        return requiredMouldQty > 1 ? "双模" : "单模";
    }

    /**
     * 在当前业务作用域内选择首选机台。
     * <p>续作补偿锁回和试制/量试限制作业定点预选属于已确认的外部固定规则，继续保留；除此之外，普通新增候选
     * 必须严格使用“目标班次筛选 + 八层软排序”已经生成的顺序。单机收完、尾量集中和当天空闲只属于
     * 数量分配或诊断维度，禁止在此二次改写首选机台。</p>
     *
     * @param context 排程上下文
     * @param sku 当前待排SKU
     * @param scopedCandidates 已完成硬过滤、窗口过滤和中心排序的候选作用域
     * @param machineMatch 机台匹配策略
     * @param preferredTrialMachine 试制、量试或小批量限制作业定点预选机台
     * @return 当前作用域首选机台；没有候选时返回null
     */
    private MachineScheduleDTO selectCandidateMachineFromScopedList(LhScheduleContext context,
                                                                    SkuScheduleDTO sku,
                                                                    List<MachineScheduleDTO> scopedCandidates,
                                                                    IMachineMatchStrategy machineMatch,
                                                                    MachineScheduleDTO preferredTrialMachine) {
        if (CollectionUtils.isEmpty(scopedCandidates)) {
            return null;
        }
        MachineScheduleDTO preferredContinuousMachine =
                resolvePreferredContinuousCompensationMachine(sku, scopedCandidates);
        if (preferredContinuousMachine != null) {
            if (isContinuationAddMachineCandidate(sku)) {
                // 续作加机台候选已经进入新增统一排序，轮到该SKU时先尝试原续作机台，失败后再回落普通候选。
                log.info("新增排产续作加机台候选优先尝试原续作机台, materialCode: {}, machineCode: {}",
                        sku.getMaterialCode(), preferredContinuousMachine.getMachineCode());
                return preferredContinuousMachine;
            }
            log.info("新增排产补偿SKU优先锁回原续作机台, materialCode: {}, machineCode: {}",
                    sku.getMaterialCode(), preferredContinuousMachine.getMachineCode());
            return preferredContinuousMachine;
        }
        if (preferredTrialMachine != null && containsMachine(scopedCandidates, preferredTrialMachine.getMachineCode())) {
            log.info("新增排产优先尝试试制/量试/小批量限制作业定点机台, materialCode: {}, machineCode: {}",
                    sku.getMaterialCode(), preferredTrialMachine.getMachineCode());
            return preferredTrialMachine;
        }
        // 普通新增选机只认中心排序首位；后续落班逻辑按所选机台真实产能排量并继续处理剩余量。
        return machineMatch.selectBestMachine(context, sku, scopedCandidates, EMPTY_STRING_SET);
    }

    /**
     * 解析续作释放补偿 SKU 在当前选机回合的原续作优先机台。
     *
     * @param sku 当前待排 SKU
     * @param scopedCandidates 当前作用域候选机台
     * @return 可直接锁回的原续作机台；不存在时返回 null
     */
    private MachineScheduleDTO resolvePreferredContinuousCompensationMachine(SkuScheduleDTO sku,
                                                                             List<MachineScheduleDTO> scopedCandidates) {
        if (sku == null || !sku.isContinuousCompensationSku()
                || StringUtils.isEmpty(sku.getPreferredContinuousMachineCode())
                || CollectionUtils.isEmpty(scopedCandidates)) {
            return null;
        }
        /*
         * 续作加机台已经通过日驱动统一资源竞争阶段完成准入，但“轮到该补偿 SKU 时优先尝试原续作机台”
         * 仍是既有选机语义。普通新增中心排序收口不能提前丢失该特殊来源的原机台。
         */
        for (MachineScheduleDTO candidate : scopedCandidates) {
            if (candidate == null) {
                continue;
            }
            if (StringUtils.equals(candidate.getMachineCode(), sku.getPreferredContinuousMachineCode())) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * 判断当轮选中的原续作机台是否可以按同物料、同模具方式重新启用。
     *
     * <p>该判断只在SKU已经完成统一排序、硬过滤、真实班次筛选并实际选中机台之后执行，
     * 不会在S4.4提前锁机。命中时必须同时满足：来源为续作加机台、选中机台就是来源续作机台、
     * 机台已被续作阶段释放、当前在机物料未变化、来源产品状态一致、整套在机模具与本次预演
     * 分配模具完全一致，并且机台没有被其他物料形成有效排产占用。</p>
     *
     * @param context 排程上下文
     * @param sku 当前续作加机台候选
     * @param machine 当轮实际选中机台
     * @param previewAllocationResult 当前机台无副作用模具分配预演
     * @return true-可按原续作机台原模具重新启用；false-继续普通新增换模流程
     */
    private boolean isReleasedContinuationSameMouldReuse(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            MachineScheduleDTO machine,
            MouldResourceAllocationResult previewAllocationResult) {
        if (!this.isContinuationAddMachineCandidate(sku)
                || Objects.isNull(machine)
                || StringUtils.isEmpty(machine.getMachineCode())
                || !StringUtils.equals(
                sku.getPreferredContinuousMachineCode(), machine.getMachineCode())) {
            return false;
        }
        return this.isReleasedContinuationMachineSameMould(
                context, sku, machine, previewAllocationResult);
    }

    /**
     * 判断单控整机配对侧是否与主侧一起满足续作原样重新启用条件。
     *
     * @param context 排程上下文
     * @param sku 当前续作加机台候选
     * @param pairMachine 配对侧机台
     * @param previewAllocationResult 配对侧无副作用模具分配预演
     * @return true-配对侧同样保持原物料原模具；false-整机不得按无换模续作重启
     */
    private boolean isReleasedContinuationPairSameMouldReuse(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            MachineScheduleDTO pairMachine,
            MouldResourceAllocationResult previewAllocationResult) {
        return this.isReleasedContinuationMachineSameMould(
                context, sku, pairMachine, previewAllocationResult);
    }

    /**
     * 校验单台机台的续作来源、占用物料和整套模具是否保持不变。
     *
     * @param context 排程上下文
     * @param sku 当前续作加机台候选
     * @param machine 待校验机台
     * @param previewAllocationResult 无副作用模具分配预演
     * @return true-机台可原样续作；false-必须执行普通换模/换活字块判断
     */
    private boolean isReleasedContinuationMachineSameMould(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            MachineScheduleDTO machine,
            MouldResourceAllocationResult previewAllocationResult) {
        if (Objects.isNull(context) || Objects.isNull(sku) || Objects.isNull(machine)
                || StringUtils.isEmpty(machine.getMachineCode())
                || CollectionUtils.isEmpty(context.getReleasedContinuousMachineCodeSet())
                || !context.getReleasedContinuousMachineCodeSet().contains(machine.getMachineCode())
                || !StringUtils.equals(machine.getCurrentMaterialCode(), sku.getMaterialCode())
                || Objects.isNull(previewAllocationResult)
                || !previewAllocationResult.isAllowed()
                || previewAllocationResult.getRequiredMouldQty() <= 0) {
            return false;
        }
        SkuScheduleDTO sourceSku = this.resolveReleasedContinuationSourceSku(
                context, sku, machine.getMachineCode());
        if (Objects.isNull(sourceSku)) {
            return false;
        }
        Set<String> currentMouldCodeSet = new LinkedHashSet<String>(
                previewAllocationResult.getReleasedMouldCodeList());
        Set<String> targetMouldCodeSet = new LinkedHashSet<String>(
                previewAllocationResult.getAllocatedMouldCodeList());
        if (currentMouldCodeSet.size() != previewAllocationResult.getRequiredMouldQty()
                || !currentMouldCodeSet.equals(targetMouldCodeSet)) {
            return false;
        }
        return !this.hasOtherSkuOccupiedReleasedContinuationMachine(
                context, sku, machine.getMachineCode());
    }

    /**
     * 解析原释放机台对应的续作来源SKU，确保产品状态没有在重新启用过程中发生变化。
     *
     * @param context 排程上下文
     * @param sku 当前续作加机台候选
     * @param machineCode 原释放机台编码
     * @return 同物料、同状态、同原机台的续作来源；不存在时返回null
     */
    private SkuScheduleDTO resolveReleasedContinuationSourceSku(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            String machineCode) {
        if (Objects.isNull(context) || Objects.isNull(sku)
                || StringUtils.isEmpty(machineCode)
                || CollectionUtils.isEmpty(context.getContinuousSkuList())) {
            return null;
        }
        for (SkuScheduleDTO sourceSku : context.getContinuousSkuList()) {
            if (Objects.nonNull(sourceSku)
                    && StringUtils.equals(sourceSku.getMaterialCode(), sku.getMaterialCode())
                    && StringUtils.equals(
                    StringUtils.trimToEmpty(sourceSku.getProductStatus()),
                    StringUtils.trimToEmpty(sku.getProductStatus()))
                    && StringUtils.equals(sourceSku.getContinuousMachineCode(), machineCode)) {
                return sourceSku;
            }
        }
        return null;
    }

    /**
     * 判断释放后是否已有其他SKU在当前机台形成有效生产占用。
     *
     * @param context 排程上下文
     * @param sku 当前续作加机台候选
     * @param machineCode 原释放机台编码
     * @return true-已被其他SKU占用；false-仍可恢复原续作
     */
    private boolean hasOtherSkuOccupiedReleasedContinuationMachine(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            String machineCode) {
        if (Objects.isNull(context) || Objects.isNull(sku)
                || StringUtils.isEmpty(machineCode)) {
            return true;
        }
        Set<LhScheduleResult> checkedResultSet =
                Collections.newSetFromMap(
                        new IdentityHashMap<LhScheduleResult, Boolean>(4));
        List<LhScheduleResult> assignedResultList =
                CollectionUtils.isEmpty(context.getMachineAssignmentMap())
                        ? null : context.getMachineAssignmentMap().get(machineCode);
        if (this.hasOtherSkuOccupiedReleasedContinuationMachine(
                assignedResultList, checkedResultSet, sku, machineCode)) {
            return true;
        }
        return this.hasOtherSkuOccupiedReleasedContinuationMachine(
                context.getScheduleResultList(), checkedResultSet, sku, machineCode);
    }

    /**
     * 扫描结果集合中的异物料或异状态有效占用。
     *
     * @param resultList 待检查结果
     * @param checkedResultSet 已检查结果引用集合
     * @param sku 当前续作加机台候选
     * @param machineCode 原释放机台编码
     * @return true-存在其他SKU有效占用；false-不存在
     */
    private boolean hasOtherSkuOccupiedReleasedContinuationMachine(
            List<LhScheduleResult> resultList,
            Set<LhScheduleResult> checkedResultSet,
            SkuScheduleDTO sku,
            String machineCode) {
        if (CollectionUtils.isEmpty(resultList)) {
            return false;
        }
        for (LhScheduleResult result : resultList) {
            if (Objects.isNull(result) || !checkedResultSet.add(result)
                    || !StringUtils.equals(machineCode, result.getLhMachineCode())
                    || ShiftFieldUtil.resolveScheduledQty(result) <= 0) {
                continue;
            }
            if (!StringUtils.equals(result.getMaterialCode(), sku.getMaterialCode())
                    || !StringUtils.equals(
                    StringUtils.trimToEmpty(result.getProductStatus()),
                    StringUtils.trimToEmpty(sku.getProductStatus()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断当前新增候选是否来源于续作加机台需求。
     *
     * @param sku 当前待排SKU
     * @return true-续作加机台候选，false-其他新增候选
     */
    private boolean isContinuationAddMachineCandidate(SkuScheduleDTO sku) {
        return sku != null && sku.isContinuousCompensationSku()
                && SkuScheduleSourceTypeEnum.isContinuationAddMachine(sku.getSourceType());
    }

    /**
     * 判断是否为试制施工阶段。
     *
     * @param sku 待排SKU
     * @return true-试制阶段
     */
    private boolean isTrialConstructionStage(SkuScheduleDTO sku) {
        return sku != null && StringUtils.equals(ConstructionStageEnum.TRIAL.getCode(), sku.getConstructionStage());
    }

    /**
     * 判断是否为当前配置生效的单控机台。
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @return true-单控机台
     */
    private boolean isSingleControlMachine(LhScheduleContext context, String machineCode) {
        return LhSingleControlMachineUtil.isConfiguredSingleControlMachine(context, machineCode);
    }

    /**
     * 解析冻结为双模的 SKU 单控整机配对侧机台。
     * <p>模式只读取 S4.3 快照，与试制、量试、正规或小批量分类无关。</p>
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param machine 当前候选机台
     * @return 配对侧机台；非整机粒度或配对侧不存在时返回 null
     */
    private MachineScheduleDTO resolveWholeSingleControlPairMachine(LhScheduleContext context,
                                                                    SkuScheduleDTO sku,
                                                                    MachineScheduleDTO machine) {
        if (!LhSingleControlMachineUtil.isWholeMachineGranularitySku(context, sku)
                || Objects.isNull(machine)
                || !isSingleControlMachine(context, machine.getMachineCode())) {
            return null;
        }
        MachineScheduleDTO pairMachine = LhSingleControlMachineUtil.resolvePairMachine(context, machine.getMachineCode());
        if (Objects.isNull(pairMachine)) {
            log.warn("双模SKU单控整机配对侧缺失，当前候选不能单边排产, materialCode: {}, machineCode: {}",
                    sku.getMaterialCode(), machine.getMachineCode());
        }
        return pairMachine;
    }

    /**
     * 判断当前SKU是否应仅尝试单控候选机台。
     * <p>只有冻结为单模的试制SKU禁止普通机台；冻结为双模的试制SKU允许普通机台和
     * 单控 L/R 整组共同进入目标班次内的八层软排序。快照缺失时保持原有从严行为，
     * 避免误落普通机台。</p>
     *
     * @param context 排程上下文
     * @param sku SKU
     * @return true-仅尝试单控候选
     */
    private boolean shouldOnlyUseSingleControlCandidate(LhScheduleContext context, SkuScheduleDTO sku) {
        if (sku == null) {
            return false;
        }
        if (isTrialConstructionStage(sku)) {
            return !LhSingleControlMachineUtil.isWholeMachineGranularitySku(context, sku);
        }
        return false;
    }

    private void logNewSpecMachineCandidateSnapshot(LhScheduleContext context,
                                                    SkuScheduleDTO sku,
                                                    List<MachineScheduleDTO> candidates,
                                                    Set<String> excludedMachineCodes,
                                                    Map<String, String> excludedMachineReasonMap) {
        if (sku == null) {
            return;
        }
        boolean needLog = sku.isSmallBatchValidation()
                || isMassTrialSku(sku)
                || isTrialConstructionStage(sku)
                || containsMachineCode(candidates, "K1501L")
                || containsMachineCode(candidates, "K1501R");
        if (!needLog) {
            return;
        }
        log.info("新增SKU候选快照, materialCode: {}, skuType: {}, surplusQty: {}, remainingQty: {}, smallBatchTotalQtyThreshold: {}, isSmallBatch: {}, "
                        + "待排小批量SKU数: {}, 候选机台: {}, 排除机台: {}, K1501L候选: {}, K1501R候选: {}, 已有排除原因: {}",
                sku.getMaterialCode(), resolveNewSpecSkuType(sku), sku.getSurplusQty(),
                sku.getRemainingScheduleQty(), resolveSmallBatchThreshold(context), sku.isSmallBatchValidation(),
                context == null ? 0 : context.getPendingSmallBatchNewSpecSkuCount(),
                joinMachineCodes(candidates), CollectionUtils.isEmpty(excludedMachineCodes) ? "-" : String.join(",", excludedMachineCodes),
                containsMachineCode(candidates, "K1501L"), containsMachineCode(candidates, "K1501R"),
                CollectionUtils.isEmpty(excludedMachineReasonMap) ? "-" : excludedMachineReasonMap.values());
    }

    private void logNewSpecMachineTypeSplit(LhScheduleContext context,
                                            SkuScheduleDTO sku,
                                            List<MachineScheduleDTO> singleControlCandidates,
                                            List<MachineScheduleDTO> normalCandidates,
                                            Set<String> excludedMachineCodes,
                                            NewSpecCandidateCache candidateCache) {
        if (sku == null) {
            return;
        }
        boolean needLog = sku.isSmallBatchValidation()
                || isMassTrialSku(sku)
                || isTrialConstructionStage(sku)
                || containsMachineCode(singleControlCandidates, "K1501L")
                || containsMachineCode(singleControlCandidates, "K1501R")
                || containsMachineCode(normalCandidates, "K1501L")
                || containsMachineCode(normalCandidates, "K1501R");
        if (!needLog) {
            return;
        }
        log.info("新增SKU选机分组, materialCode: {}, skuType: {}, 待排小批量SKU数: {}, 单控候选: {}, 普通候选: {}, "
                        + "单控剩余产能: {}, K1501L单控: {}, K1501R单控: {}, K1501L普通: {}, K1501R普通: {}, 已排除机台: {}",
                sku.getMaterialCode(), resolveNewSpecSkuType(sku),
                context == null ? 0 : context.getPendingSmallBatchNewSpecSkuCount(),
                joinMachineCodes(singleControlCandidates), joinMachineCodes(normalCandidates),
                resolveMachineCapacitySummary(context, sku, singleControlCandidates, candidateCache),
                containsMachineCode(singleControlCandidates, "K1501L"),
                containsMachineCode(singleControlCandidates, "K1501R"),
                containsMachineCode(normalCandidates, "K1501L"),
                containsMachineCode(normalCandidates, "K1501R"),
                CollectionUtils.isEmpty(excludedMachineCodes) ? "-" : String.join(",", excludedMachineCodes));
    }

    private boolean containsMachineCode(List<MachineScheduleDTO> candidates, String machineCode) {
        if (CollectionUtils.isEmpty(candidates) || StringUtils.isEmpty(machineCode)) {
            return false;
        }
        for (MachineScheduleDTO candidate : candidates) {
            if (candidate != null && StringUtils.equalsIgnoreCase(machineCode, candidate.getMachineCode())) {
                return true;
            }
        }
        return false;
    }

    private String joinMachineCodes(List<MachineScheduleDTO> candidates) {
        if (CollectionUtils.isEmpty(candidates)) {
            return "-";
        }
        StringBuilder builder = new StringBuilder();
        for (MachineScheduleDTO candidate : candidates) {
            if (candidate == null || StringUtils.isEmpty(candidate.getMachineCode())) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(candidate.getMachineCode());
        }
        return builder.length() == 0 ? "-" : builder.toString();
    }

    private String resolveMachineCapacitySummary(LhScheduleContext context,
                                                 SkuScheduleDTO sku,
                                                 List<MachineScheduleDTO> candidates,
                                                 NewSpecCandidateCache candidateCache) {
        if (context == null || sku == null || CollectionUtils.isEmpty(candidates)) {
            return "-";
        }
        StringBuilder builder = new StringBuilder();
        for (MachineScheduleDTO candidate : candidates) {
            if (candidate == null || StringUtils.isEmpty(candidate.getMachineCode())) {
                continue;
            }
            int availableCapacity = resolveCachedMachineAvailableCapacityInWindow(
                    context, sku, candidate, candidateCache);
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(candidate.getMachineCode()).append("=").append(availableCapacity);
        }
        return builder.length() == 0 ? "-" : builder.toString();
    }

    private int resolveSmallBatchThreshold(LhScheduleContext context) {
        return LhScheduleConstant.SMALL_BATCH_SKU_THRESHOLD;
    }

    private boolean containsMachine(List<MachineScheduleDTO> candidates, String machineCode) {
        if (CollectionUtils.isEmpty(candidates) || StringUtils.isEmpty(machineCode)) {
            return false;
        }
        for (MachineScheduleDTO candidate : candidates) {
            if (candidate != null && StringUtils.equals(machineCode, candidate.getMachineCode())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从候选机台列表中查找指定编码的机台。
     *
     * @param candidates 候选机台列表
     * @param machineCode 机台编码
     * @return 匹配的机台;不存在时返回null
     */
    private MachineScheduleDTO findMachineInList(List<MachineScheduleDTO> candidates, String machineCode) {
        if (CollectionUtils.isEmpty(candidates) || StringUtils.isEmpty(machineCode)) {
            return null;
        }
        for (MachineScheduleDTO candidate : candidates) {
            if (candidate != null && StringUtils.equals(machineCode, candidate.getMachineCode())) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * 计算当前机台各班次最大可排量。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param sku SKU
     * @param firstProductionStartTime 首个可生产时间
     * @param mouldChangeStartTime 换模开始时间
     * @param shifts 排程窗口班次
     * @param mouldQty 模台数
     * @param shiftCapacity 运行态班产
     * @param isEnding 是否收尾
     * @return 班次索引到最大可排量的映射
     */
    private Map<Integer, Integer> calculateShiftCapacityMap(LhScheduleContext context,
                                                            MachineScheduleDTO machine,
                                                            SkuScheduleDTO sku,
                                                            Date firstProductionStartTime,
                                                            Date mouldChangeStartTime,
                                                            List<LhShiftConfigVO> shifts,
                                                            int mouldQty,
                                                            int shiftCapacity,
                                                            boolean isEnding) {
        return calculateShiftCapacityMap(
                context, machine, sku, firstProductionStartTime, mouldChangeStartTime, shifts,
                mouldQty, shiftCapacity, isEnding, false);
    }

    /**
     * 计算当前机台各班次最大可排量，并可按统一生产门禁裁剪首个实际生产班次。
     *
     * <p>生产门禁没有推迟现有时间轴时保持原班次产能口径；胎胚可供时间或 X/T 中班门禁
     * 推迟生产时，仅将首个生产班次的有效窗口起点抬高到实际生产开始时间，换模和换活字块
     * 的准备时间仍使用既有时间轴。</p>
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param sku SKU
     * @param firstProductionStartTime 首个可生产时间
     * @param mouldChangeStartTime 换模开始时间
     * @param shifts 排程窗口班次
     * @param mouldQty 模台数
     * @param shiftCapacity 运行态班产
     * @param isEnding 是否收尾
     * @param productionStartTimeConstrained 是否按统一生产门禁裁剪首班生产窗口
     * @return 班次索引到最大可排量的映射
     */
    private Map<Integer, Integer> calculateShiftCapacityMap(LhScheduleContext context,
                                                            MachineScheduleDTO machine,
                                                            SkuScheduleDTO sku,
                                                            Date firstProductionStartTime,
                                                            Date mouldChangeStartTime,
                                                            List<LhShiftConfigVO> shifts,
                                                            int mouldQty,
                                                            int shiftCapacity,
                                                            boolean isEnding,
                                                            boolean productionStartTimeConstrained) {
        /*
         * 兼容不掌握真实切换完成时刻的既有调用。新增逐班选机和正式落地会调用下方重载，
         * 显式传入切换完成时刻，避免胎胚门禁后移时扩大“清洗与换模重叠”的豁免区间。
         */
        return this.calculateShiftCapacityMap(
                context, machine, sku, firstProductionStartTime,
                mouldChangeStartTime, firstProductionStartTime, shifts,
                mouldQty, shiftCapacity, isEnding, productionStartTimeConstrained);
    }

    /**
     * 按真实切换区间计算当前机台各班次最大可排量。
     *
     * @param context 排程上下文
     * @param machine 机台
     * @param sku 当前 SKU
     * @param firstProductionStartTime 首个正式生产请求时间
     * @param mouldChangeStartTime 换模或换活字块开始时间
     * @param mouldChangeEndTime 换模或换活字块真实完成时间
     * @param shifts 排程窗口班次
     * @param mouldQty 模台数
     * @param shiftCapacity 运行态班产
     * @param isEnding 是否收尾
     * @param productionStartTimeConstrained 是否按统一生产门禁裁剪首班生产窗口
     * @return 班次索引到最大可排量的映射
     */
    private Map<Integer, Integer> calculateShiftCapacityMap(LhScheduleContext context,
                                                            MachineScheduleDTO machine,
                                                            SkuScheduleDTO sku,
                                                            Date firstProductionStartTime,
                                                            Date mouldChangeStartTime,
                                                            Date mouldChangeEndTime,
                                                            List<LhShiftConfigVO> shifts,
                                                            int mouldQty,
                                                            int shiftCapacity,
                                                            boolean isEnding,
                                                            boolean productionStartTimeConstrained) {
        Map<Integer, Integer> shiftCapacityMap = new LinkedHashMap<Integer, Integer>(
                CollectionUtils.isEmpty(shifts) ? 0 : shifts.size());
        if (context == null || machine == null || sku == null || firstProductionStartTime == null
                || CollectionUtils.isEmpty(shifts)) {
            return shiftCapacityMap;
        }
        // 计算班次上限时同步清洗专用规则，避免3天内可收尾SKU仍扣干冰/喷砂清洗产能。
        List<MachineCleaningWindowDTO> cleaningWindowList = resolveEffectiveCleaningWindowList(
                context, machine.getMachineCode(), sku, mouldChangeStartTime, mouldChangeEndTime);
        List<MachineMaintenanceWindowDTO> maintenanceWindowList = resolveMachineMaintenanceWindowList(
                context, machine.getMachineCode());
        Date cursorStartTime = firstProductionStartTime;
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
                        context, sku.getStructureName());
        int remainShiftCapacityUpperLimit =
                ShiftCapacityResolverUtil.resolveDailyStandardRemainShiftCapacityUpperLimit(
                        context, sku.getMaterialCode(), shiftCapacity);
        boolean singleControlMachine = isSingleControlMachine(context, machine.getMachineCode());
        boolean started = false;
        for (LhShiftConfigVO shift : shifts) {
            if (!started) {
                if (cursorStartTime.before(shift.getShiftEndDateTime())) {
                    started = true;
                } else {
                    continue;
                }
            }
            ShiftProductionControlDTO control = ShiftProductionControlUtil.resolveEffectiveControl(context, shift, cursorStartTime);
            if (control == null || !control.isCanSchedule()) {
                continue;
            }
            // 仅参数清单内结构允许剩余班次使用独立理论上限；未命中结构始终从原始班产开始扣减。
            int currentShiftCapacity = dailyStandardStructureMatched && !singleControlMachine
                    && ShiftCapacityResolverUtil.isDailyStandardRemainShift(shift, remainShiftType)
                    ? remainShiftCapacityUpperLimit : shiftCapacity;
            int actualShiftPlanQty = ShiftCapacityResolverUtil.resolveActualShiftPlanQty(
                    currentShiftCapacity, shift, configPlusShiftType, ScheduleTypeEnum.NEW_SPEC.getCode());
            boolean oddShiftAdjustEnabled = ShiftCapacityResolverUtil.isOddShiftCapacityAdjustEnabled(
                    currentShiftCapacity, shift, configPlusShiftType, ScheduleTypeEnum.NEW_SPEC.getCode());
            log.debug("奇数班产修正检查, 当前流程: 新增排程, materialCode: {}, machineCode: {}, 参数是否配置: {}, "
                            + "参数值: {}, 配置值是否合法: {}, 是否启用: {}, 未启用原因: {}, 原始班产: {}, "
                            + "班次序号: {}, 当前班别: {}, 当前班次修正后的计划量: {}, 班产落库字段值: {}",
                    sku.getMaterialCode(), machine.getMachineCode(), StringUtils.isNotEmpty(configPlusShiftType),
                    configPlusShiftType,
                    ShiftCapacityResolverUtil.isOddShiftCapacityPlusShiftTypeValid(configPlusShiftType),
                    oddShiftAdjustEnabled,
                    ShiftCapacityResolverUtil.resolveOddShiftCapacityDisabledReason(
                            shiftCapacity, shift, configPlusShiftType, ScheduleTypeEnum.NEW_SPEC.getCode()),
                    shiftCapacity, shift.getShiftIndex(), shift.resolveShiftTypeEnum(), actualShiftPlanQty,
                    shiftCapacity);
            Date effectiveStartTime = control.getEffectiveStartTime();
            Date effectiveEndTime = control.getEffectiveEndTime();
            long shiftDurationSeconds = ShiftCapacityResolverUtil.resolveShiftDurationSeconds(shift);
            if (productionStartTimeConstrained) {
                /*
                 * 班次管控窗口可能从班次起点开始，但统一生产门禁前不得计入首检或正式生产产能。
                 * 仅裁剪当前首个生产班次，后续班次仍使用原完整班产、停机和管控计算链。
                 */
                effectiveStartTime = NewSpecEmbryoAvailableTimeResolver.resolveEffectiveProductionWindowStart(
                        effectiveStartTime, effectiveEndTime, cursorStartTime);
                if (Objects.isNull(effectiveStartTime)) {
                    continue;
                }
                shiftDurationSeconds = NewSpecEmbryoAvailableTimeResolver.resolveProductionWindowSeconds(
                        effectiveStartTime, effectiveEndTime);
            }
            int shiftMaxQty = ShiftCapacityResolverUtil.resolveShiftCapacityWithDowntime(
                    context.getDevicePlanShutList(),
                    cleaningWindowList,
                    maintenanceWindowList,
                    machine.getMachineCode(),
                    effectiveStartTime,
                    effectiveEndTime,
                    currentShiftCapacity,
                    sku.getLhTimeSeconds(),
                    mouldQty,
                    shiftDurationSeconds,
                    dryIceLossQty,
                    dryIceDurationHours,
                    shift,
                    configPlusShiftType,
                    ScheduleTypeEnum.NEW_SPEC.getCode(),
                    plannedRepairFixedQty);
            shiftMaxQty = ShiftProductionControlUtil.deductCapacityByControl(control, shiftMaxQty, mouldQty);
            if (shiftMaxQty <= 0) {
                continue;
            }
            if (oddShiftAdjustEnabled) {
                log.info("奇数班产修正命中, 当前流程: 新增排程, materialCode: {}, machineCode: {}, 参数值: {}, "
                                + "原始班产: {}, 班次序号: {}, 当前班别: {}, 修正后班次计划量: {}, 班产落库字段值: {}",
                        sku.getMaterialCode(), machine.getMachineCode(), configPlusShiftType, shiftCapacity,
                        shift.getShiftIndex(), shift.resolveShiftTypeEnum(), actualShiftPlanQty, shiftCapacity);
            }
            shiftCapacityMap.put(shift.getShiftIndex(), shiftMaxQty);
            cursorStartTime = effectiveEndTime;
        }
        return shiftCapacityMap;
    }

    /**
     * 按统一时间与实际产能口径构建首检分摊计划。
     *
     * <p>第一遍只根据首检总量、小时产量和班次重叠形成真实时间区间；第二遍再把该区间
     * 内的停机、清洗、精度、维修、预热、班次管控及日标准产量折算成实际容量上限。
     * 选机预演和最终机台目标量复核必须共同调用本方法，禁止分别推导首检班次或数量。</p>
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param machine 候选机台
     * @param shifts 完整排程窗口班次
     * @param changeoverStartTime 换模或换活字块开始时间
     * @param changeoverEndTime 换模或换活字块结束时间，也是首检区间结束时间
     * @param runtimeShiftCapacity 运行态班产
     * @param mouldQty 运行态模数
     * @param targetQty 当前机台最多允许消费的目标量，首检包含在该目标量内
     * @param scheduleType 排程类型
     * @return 无副作用首检分摊计划；无效原因由计划对象返回
     */
    private FirstInspectionAllocationPlan resolveFirstInspectionAllocationPlan(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            MachineScheduleDTO machine,
            List<LhShiftConfigVO> shifts,
            Date changeoverStartTime,
            Date changeoverEndTime,
            int runtimeShiftCapacity,
            int mouldQty,
            int targetQty,
            String scheduleType) {
        FirstInspectionAllocationPlan plan = FirstInspectionAllocationUtil.buildPlan(
                context, sku, shifts, changeoverEndTime,
                runtimeShiftCapacity, targetQty, scheduleType,
                machine.getMachineCode(), null);
        if (!plan.isValid() || plan.getInspectionQty() <= 0) {
            return plan;
        }
        Map<Integer, Integer> inspectionCapacityMap =
                this.calculateFirstInspectionAvailableCapacityMap(
                        context, machine, sku, plan, changeoverStartTime,
                        runtimeShiftCapacity, mouldQty, scheduleType);
        return FirstInspectionAllocationUtil.buildPlan(
                context, sku, shifts, changeoverEndTime,
                runtimeShiftCapacity, targetQty, scheduleType,
                machine.getMachineCode(), inspectionCapacityMap);
    }

    /**
     * 计算首检真实时间区间内各班次可供占用的实际产能。
     *
     * <p>首检发生在正式生产之前，不能直接复用“从正式开产时间开始”的产能图。本方法按
     * 首检与班次的实际重叠区间重新套用班次管控、停机、清洗、精度/维修、预热、奇数班产
     * 及日标准产量规则。返回值只用于校验首检能否落位，不提交任何运行态资源。</p>
     *
     * @param context 排程上下文
     * @param machine 候选机台
     * @param sku 当前 SKU
     * @param plan 首检时间分摊计划
     * @param changeoverStartTime 换模或换活字块开始时间
     * @param runtimeShiftCapacity 运行态班产
     * @param mouldQty 运行态模数
     * @param scheduleType 排程类型
     * @return 班次索引到首检区间实际可用产能的映射
     */
    private Map<Integer, Integer> calculateFirstInspectionAvailableCapacityMap(
            LhScheduleContext context,
            MachineScheduleDTO machine,
            SkuScheduleDTO sku,
            FirstInspectionAllocationPlan plan,
            Date changeoverStartTime,
            int runtimeShiftCapacity,
            int mouldQty,
            String scheduleType) {
        Map<Integer, Integer> capacityMap = new LinkedHashMap<Integer, Integer>(4);
        if (Objects.isNull(context) || Objects.isNull(machine) || Objects.isNull(sku)
                || Objects.isNull(plan) || !plan.isValid()
                || CollectionUtils.isEmpty(plan.getShiftAllocations())
                || runtimeShiftCapacity <= 0 || mouldQty <= 0) {
            return capacityMap;
        }
        List<MachineCleaningWindowDTO> cleaningWindowList =
                this.resolveEffectiveCleaningWindowList(
                        context, machine.getMachineCode(), sku,
                        changeoverStartTime, plan.getInspectionEndTime());
        List<MachineMaintenanceWindowDTO> maintenanceWindowList =
                this.resolveMachineMaintenanceWindowList(
                        context, machine.getMachineCode());
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
                    maintenanceWindowList, machine.getMachineCode(),
                    effectiveStartTime, effectiveEndTime, runtimeShiftCapacity,
                    sku.getLhTimeSeconds(), mouldQty, effectiveSeconds,
                    dryIceLossQty, dryIceDurationHours, shift,
                    configPlusShiftType, scheduleType, plannedRepairFixedQty);
            availableQty = ShiftProductionControlUtil.deductCapacityByControl(
                    control, availableQty, mouldQty);
            capacityMap.put(shift.getShiftIndex(), Math.max(0, availableQty));
        }
        return this.applyDailyStandardCapacityAdjust(
                context, sku, machine.getMachineCode(),
                context.getScheduleWindowShifts(), capacityMap,
                runtimeShiftCapacity);
    }

    /**
     * 汇总班次可排产能。
     *
     * @param shiftCapacityMap 班次产能映射
     * @return 合计产能
     */
    private int sumShiftCapacity(Map<Integer, Integer> shiftCapacityMap) {
        if (CollectionUtils.isEmpty(shiftCapacityMap)) {
            return 0;
        }
        int totalQty = 0;
        for (Integer shiftCapacity : shiftCapacityMap.values()) {
            totalQty += shiftCapacity == null ? 0 : Math.max(0, shiftCapacity);
        }
        return Math.max(0, totalQty);
    }

    /**
     * 按SKU日标准产量修正新增排程班次计划量。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param machineCode 机台编码
     * @param shifts 班次列表
     * @param shiftCapacityMap 原班次计划量
     * @param runtimeShiftCapacity 运行态班产
     * @return 修正后的班次计划量
     */
    private Map<Integer, Integer> applyDailyStandardCapacityAdjust(LhScheduleContext context,
                                                                   SkuScheduleDTO sku,
                                                                   String machineCode,
                                                                   List<LhShiftConfigVO> shifts,
                                                                   Map<Integer, Integer> shiftCapacityMap,
                                                                   int runtimeShiftCapacity) {
        boolean dailyStandardStructureMatched =
                ShiftCapacityResolverUtil.isDailyStandardCapacityStructureMatched(
                        context, sku.getStructureName());
        if (!dailyStandardStructureMatched) {
            // 未命中结构直接保留已完成停机、清洗、首检等扣减的班次产能，不再执行日标准量补差。
            return shiftCapacityMap;
        }
        String remainShiftType = ShiftCapacityResolverUtil.resolveDailyStandardCapacityRemainShiftType(context);
        boolean singleControlMachine = isSingleControlMachine(context, machineCode);
        int dailyStandardQty = ShiftCapacityResolverUtil.resolveDailyStandardQty(context, sku.getMaterialCode());
        int remainShiftCapacityUpperLimit =
                ShiftCapacityResolverUtil.resolveDailyStandardRemainShiftCapacityUpperLimit(
                        context, sku.getMaterialCode(), runtimeShiftCapacity);
        Map<Integer, Integer> scopedAdjustedMap = ShiftCapacityResolverUtil.adjustShiftPlanQtyMapByDailyStandard(
                shifts, shiftCapacityMap, shiftCapacityMap, dailyStandardQty, runtimeShiftCapacity,
                remainShiftCapacityUpperLimit,
                remainShiftType, singleControlMachine, ScheduleTypeEnum.NEW_SPEC.getCode());
        /*
         * 当前方法既用于单日正式产能，也用于“前一业务日首检 + 当前业务日正式生产”的
         * 合并产能图。日标准规则只允许修正参数 shifts 覆盖的当前业务日班次；首检真实
         * 覆盖的前序班次虽然不在本次日切片内，仍属于本次目标量和设备占用，绝不能因
         * adjustShiftPlanQtyMapByDailyStandard 只遍历当前日班次而从产能图中丢失。
         * 因此按原映射顺序回填作用域外条目，作用域内条目读取统一日标准修正结果。
         */
        Map<Integer, Integer> adjustedMap = new LinkedHashMap<Integer, Integer>(
                Math.max(shiftCapacityMap.size(), scopedAdjustedMap.size()));
        for (Map.Entry<Integer, Integer> entry : shiftCapacityMap.entrySet()) {
            Integer shiftIndex = entry.getKey();
            adjustedMap.put(shiftIndex, scopedAdjustedMap.containsKey(shiftIndex)
                    ? scopedAdjustedMap.get(shiftIndex) : entry.getValue());
        }
        for (Map.Entry<Integer, Integer> entry : scopedAdjustedMap.entrySet()) {
            adjustedMap.putIfAbsent(entry.getKey(), entry.getValue());
        }
        if (!Objects.equals(shiftCapacityMap, adjustedMap)) {
            log.info("日标准产量班次计划量修正, 当前流程: 新增排程, materialCode: {}, structureName: {}, "
                            + "结构是否命中参数: {}, machineCode: {}, 是否单控机台: {}, "
                            + "SKU日标准产量: {}, 班产: {}, 剩余班次理论上限: {}, "
                            + "日标准产量剩余班次参数值: {}, "
                            + "修正前班次计划量: {}, 修正后班次计划量: {}",
                    sku.getMaterialCode(), sku.getStructureName(), dailyStandardStructureMatched,
                    machineCode, singleControlMachine, dailyStandardQty, runtimeShiftCapacity,
                    remainShiftCapacityUpperLimit, remainShiftType,
                    shiftCapacityMap, adjustedMap);
        }
        return adjustedMap;
    }

    /**
     * 对补满后的新增排程结果再次应用日标准产量规则，避免后置补量突破已修正班次上限。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param result 排程结果
     * @param shifts 班次列表
     * @param runtimeShiftCapacity 运行态班产
     * @param firstInspectionAllocationPlan 选机阶段已确认的首检跨班分摊；跨日续作传 null
     */
    private void applyDailyStandardPlanQtyToResult(LhScheduleContext context,
                                                   SkuScheduleDTO sku,
                                                   LhScheduleResult result,
                                                   List<LhShiftConfigVO> shifts,
                                                   int runtimeShiftCapacity,
                                                   FirstInspectionAllocationPlan firstInspectionAllocationPlan) {
        if (Objects.isNull(context) || Objects.isNull(sku) || Objects.isNull(result)
                || CollectionUtils.isEmpty(shifts) || runtimeShiftCapacity <= 0) {
            return;
        }
        Map<Integer, Integer> rawPlanQtyMap = new LinkedHashMap<Integer, Integer>(shifts.size());
        for (LhShiftConfigVO shift : shifts) {
            Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
            rawPlanQtyMap.put(shift.getShiftIndex(), Objects.isNull(planQty) ? 0 : Math.max(0, planQty));
        }
        // 后置补满后仍复用同一结构准入入口，避免模拟、首次分配和最终落库出现不同口径。
        Map<Integer, Integer> adjustedPlanQtyMap = applyDailyStandardCapacityAdjust(
                context, sku, result.getLhMachineCode(), shifts, rawPlanQtyMap, runtimeShiftCapacity);
        Map<Integer, Integer> protectedPlanQtyMap = new LinkedHashMap<Integer, Integer>(
                adjustedPlanQtyMap);
        Map<Integer, FirstInspectionShiftAllocation> inspectionAllocationMap =
                new LinkedHashMap<Integer, FirstInspectionShiftAllocation>(4);
        if (Objects.nonNull(firstInspectionAllocationPlan)
                && firstInspectionAllocationPlan.isValid()) {
            /*
             * 首检属于目标量且已在选机阶段占用真实班次产能，后置补满回裁只能减少正式生产量，
             * 不能把已提交的首检条数裁掉。若日标准规则低于首检量，至少保留该班首检量；
             * 选机预演已经校验首检本身不突破日标准和物理产能，因此这里不会制造额外超限。
             */
            for (FirstInspectionShiftAllocation allocation
                    : firstInspectionAllocationPlan.getShiftAllocations()) {
                if (Objects.isNull(allocation) || Objects.isNull(allocation.getShift())) {
                    continue;
                }
                int shiftIndex = allocation.getShift().getShiftIndex();
                inspectionAllocationMap.put(shiftIndex, allocation);
                protectedPlanQtyMap.put(shiftIndex, Math.max(
                        Math.max(0, protectedPlanQtyMap.getOrDefault(shiftIndex, 0)),
                        allocation.getQuantity()));
            }
        }
        if (Objects.equals(rawPlanQtyMap, protectedPlanQtyMap)) {
            return;
        }
        int lhTimeSeconds = Objects.isNull(result.getLhTime()) ? 0 : Math.max(0, result.getLhTime());
        int mouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(
                Objects.isNull(result.getMouldQty()) ? 0 : result.getMouldQty());
        // 结果后置修正也必须复用同一清洗过滤口径，避免日标准产量回裁时重新扣除已跳过的清洗。
        List<MachineCleaningWindowDTO> cleaningWindowList = resolveEffectiveCleaningWindowList(
                context, result, resolveFirstPlannedShiftStartTime(result));
        List<MachineMaintenanceWindowDTO> maintenanceWindowList = resolveMachineMaintenanceWindowList(
                context, result.getLhMachineCode());
        for (LhShiftConfigVO shift : shifts) {
            int shiftIndex = shift.getShiftIndex();
            int beforeQty = rawPlanQtyMap.get(shiftIndex);
            Integer adjustedQty = protectedPlanQtyMap.get(shiftIndex);
            int afterQty = Objects.isNull(adjustedQty) ? beforeQty : Math.max(0, adjustedQty);
            if (beforeQty == afterQty) {
                continue;
            }
            FirstInspectionShiftAllocation inspectionAllocation =
                    inspectionAllocationMap.get(shiftIndex);
            Date startTime = ShiftFieldUtil.getShiftStartTime(result, shiftIndex);
            if (Objects.isNull(startTime)) {
                startTime = shift.getShiftStartDateTime();
            }
            Date endTime = null;
            if (Objects.nonNull(inspectionAllocation)
                    && afterQty == inspectionAllocation.getQuantity()) {
                // 当前班正式生产被全部回裁时，恢复首检真实时间区间，不能伪造成整班生产。
                startTime = inspectionAllocation.getOverlapStartTime();
                endTime = inspectionAllocation.getOverlapEndTime();
            } else if (afterQty > 0 && lhTimeSeconds > 0) {
                long secondsNeeded = (long) Math.ceil((double) afterQty / mouldQty) * lhTimeSeconds;
                endTime = ShiftCapacityResolverUtil.resolveCompletionTimeWithDowntimes(
                        context.getDevicePlanShutList(), cleaningWindowList, maintenanceWindowList,
                        result.getLhMachineCode(), startTime, secondsNeeded);
            }
            ShiftFieldUtil.setShiftPlanQty(result, shiftIndex, afterQty,
                    afterQty > 0 ? startTime : null, endTime);
        }
        refreshResultSummary(context, result);
        log.info("日标准产量结果计划量收敛, 当前流程: 新增排程, materialCode: {}, machineCode: {}, "
                        + "修正前班次计划量: {}, 修正后班次计划量: {}",
                sku.getMaterialCode(), result.getLhMachineCode(), rawPlanQtyMap, protectedPlanQtyMap);
    }

    /**
     * 构建机台生产段，用于记录角色判断和关键日志。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param machineCode 机台编码
     * @param mouldChangeStartTime 换模开始时间
     * @param firstProductionStartTime 首个可生产时间
     * @param maxQtyToWindowEnd 最大可排量
     * @param shiftCapacity 运行态班产
     * @return 机台生产段
     */
    private MachineProductionSegment buildMachineProductionSegment(LhScheduleContext context,
                                                                   SkuScheduleDTO sku,
                                                                   String machineCode,
                                                                   Date mouldChangeStartTime,
                                                                   Date firstProductionStartTime,
                                                                   int maxQtyToWindowEnd,
                                                                   int shiftCapacity,
                                                                   Map<Integer, Integer> shiftCapacityMap) {
        MachineProductionSegment segment = new MachineProductionSegment();
        segment.setMachineCode(machineCode);
        segment.setMaterialCode(sku.getMaterialCode());
        segment.setGreenTireGroupKey(sku.getEmbryoCode());
        segment.setNeedChangeover(true);
        segment.setMaxQtyToWindowEnd(maxQtyToWindowEnd);
        segment.setShiftCapacity(shiftCapacity);
        segment.setShiftCapacityMap(CollectionUtils.isEmpty(shiftCapacityMap)
                ? new LinkedHashMap<Integer, Integer>(0)
                : new LinkedHashMap<Integer, Integer>(shiftCapacityMap));
        segment.setChangeoverShiftIndex(LhScheduleTimeUtil.getShiftIndex(
                context, context.getScheduleDate(), mouldChangeStartTime));
        segment.setStartProductionShiftIndex(LhScheduleTimeUtil.getShiftIndex(
                context, context.getScheduleDate(), firstProductionStartTime));
        return segment;
    }

    /**
     * 判断当前机台在多机台补量中的角色。
     *
     * @param policy 排产数量策略
     * @param scheduledQty 当前已排量
     * @param maxQtyToWindowEnd 当前机台最大可排量
     * @param targetQty 窗口目标量
     * @return 机台角色
     */
    private MachineScheduleRole resolveMachineScheduleRole(ProductionQuantityPolicy policy,
                                                           int scheduledQty,
                                                           int maxQtyToWindowEnd,
                                                           int targetQty) {
        if (policy != null && policy.isFullRunForNonTailMachine()
                && scheduledQty + maxQtyToWindowEnd < targetQty) {
            return MachineScheduleRole.FULL_RUN_MACHINE;
        }
        return MachineScheduleRole.TAIL_MACHINE;
    }

    /**
     * 解析新增排产正式/量试非收尾场景的业务目标量。
     * <p>dayN 只参与排产节奏和增机台判断，不再作为非收尾 SKU 的实际排产硬目标。</p>
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param policy 排产数量策略
     * @return 业务目标量
     */
    private int resolveFormalNonEndingMinimumTargetQty(LhScheduleContext context,
                                                       SkuScheduleDTO sku,
                                                       ProductionQuantityPolicy policy) {
        if (sku == null) {
            return 0;
        }
        int businessTargetQty = Math.max(0, sku.resolveTargetScheduleQty());
        if (shouldUseFormalNonEndingMinimumTarget(context, sku, policy)) {
            log.info("新增SKU正式非收尾目标量按业务目标保留, materialCode: {}, businessTargetQty: {}, "
                            + "windowRemainingPlanQty: {}, windowPlanQty: {}, dailyPlanRemainingQty: {}",
                    sku.getMaterialCode(), businessTargetQty, sku.getWindowRemainingPlanQty(),
                    sku.getWindowPlanQty(), SkuDailyPlanQuotaUtil.sumRemainingQty(sku.getDailyPlanQuotaMap()));
        }
        return businessTargetQty;
    }

    /**
     * 判断当前是否使用新增排产正式/量试非收尾业务目标量口径。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param policy 排产数量策略
     * @return true-使用业务目标量并保留 dayN 增机判断
     */
    private boolean shouldUseFormalNonEndingMinimumTarget(LhScheduleContext context,
                                                          SkuScheduleDTO sku,
                                                          ProductionQuantityPolicy policy) {
        if (context == null || sku == null || policy == null) {
            return false;
        }
        if (policy.isStrictUpperLimit() || !policy.isAllowFillStartedShift()) {
            return false;
        }
        return getTargetScheduleQtyResolver().isFullCapacityMode(context);
    }

    /**
     * 根据机台角色计算当前机台计划量。
     *
     * @param policy 排产数量策略
     * @param role 机台角色
     * @param targetQty 窗口目标量
     * @param scheduledQty 当前已排量
     * @param maxQtyToWindowEnd 当前机台最大可排量
     * @param shiftCapacity 运行态班产
     * @return 当前机台计划量
     */
    private int resolveMachinePlanQty(LhScheduleContext context,
                                      SkuScheduleDTO sku,
                                      ProductionQuantityPolicy policy,
                                      MachineScheduleRole role,
                                      MachineProductionSegment segment,
                                      int targetQty,
                                      int scheduledQty,
                                      int maxQtyToWindowEnd,
                                      int shiftCapacity) {
        if (maxQtyToWindowEnd <= 0) {
            return 0;
        }
        if (MachineScheduleRole.FULL_RUN_MACHINE == role) {
            return maxQtyToWindowEnd;
        }
        int remainingQty = Math.max(0, targetQty - scheduledQty);
        if (remainingQty <= 0) {
            return 0;
        }
        int tailFilledQty = resolveTailFillPlanQty(context, sku, policy, role, segment, remainingQty);
        if (tailFilledQty > 0) {
            return Math.min(tailFilledQty, maxQtyToWindowEnd);
        }
        int planQty = policy != null && policy.isAllowFillStartedShift()
                ? roundUpToShiftCapacity(remainingQty, shiftCapacity) : remainingQty;
        return Math.min(planQty, maxQtyToWindowEnd);
    }

    /**
     * 判断当前新增SKU是否允许按单机台补满到窗口结束。
     * <p>仅新增规格主链生效：非收尾、非试制，且当前首个成功机台已能独立覆盖窗口目标量时，直接补满到窗口结束。</p>
     *
     * @param sku SKU
     * @param isEnding 是否收尾
     * @param totalScheduledQty 当前SKU已累计排产量
     * @param candidateTargetQty 当前窗口目标量
     * @param maxQtyToWindowEnd 当前机台最大可排量
     * @return true-按单机台补满窗口处理
     */
    private boolean shouldFillSingleMachineToWindowEnd(LhScheduleContext context,
                                                       SkuScheduleDTO sku,
                                                       MachineScheduleDTO candidateMachine,
                                                       boolean isEnding,
                                                       int totalScheduledQty,
                                                       int candidateTargetQty,
                                                       int maxQtyToWindowEnd,
                                                       EarlyProductionDecision earlyProductionDecision) {
        if (sku == null || isEnding || totalScheduledQty > 0) {
            return false;
        }
        if (StringUtils.equals(ConstructionStageEnum.TRIAL.getCode(), sku.getConstructionStage())) {
            return false;
        }
        if (candidateTargetQty <= 0 || maxQtyToWindowEnd < candidateTargetQty) {
            return false;
        }
        if (isSmallBatchSingleControlMachine(context, sku, candidateMachine)) {
            // 小批量 SKU 优先占用单控运行态机台，命中后应补满该单控侧窗口，避免被后续普通 SKU 截断。
            return true;
        }
        if (isAllowedFuturePlanEarlyProduction(earlyProductionDecision)) {
            /*
             * 后续日计划 SKU 已通过提前生产准入并完成新增换模上机后，
             * 当前机台应保留到窗口结束，避免只按被提前借用的 dayN 小计划截断 C6~C8。
             */
            log.info("提前生产新增换模保留机台到窗口结束, materialCode: {}, machineCode: {}, "
                            + "futurePlanDate: {}, targetQty: {}, maxQtyToWindowEnd: {}",
                    sku.getMaterialCode(), candidateMachine == null ? null : candidateMachine.getMachineCode(),
                    earlyProductionDecision.getFuturePlanDate(), candidateTargetQty, maxQtyToWindowEnd);
            return true;
        }
        if (!CollectionUtils.isEmpty(sku.getDailyPlanQuotaMap())) {
            return hasMultiDayQuotaWindow(sku) && isOnlyPendingNewSpecSku(context);
        }
        if (!isOnlyPendingNewSpecSku(context)) {
            return false;
        }
        return candidateTargetQty > Math.max(0, sku.getPendingQty());
    }

    private boolean isAllowedFuturePlanEarlyProduction(EarlyProductionDecision earlyProductionDecision) {
        return earlyProductionDecision != null
                && earlyProductionDecision.isEarlyProduction()
                && earlyProductionDecision.isAllowed()
                && earlyProductionDecision.getFuturePlanDate() != null;
    }

    /**
     * 判断当前候选是否为小批量 SKU 命中的单控运行态机台。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param candidateMachine 当前候选机台
     * @return true-小批量命中单控运行态机台
     */
    private boolean isSmallBatchSingleControlMachine(LhScheduleContext context,
                                                     SkuScheduleDTO sku,
                                                     MachineScheduleDTO candidateMachine) {
        if (Objects.isNull(sku) || Objects.isNull(candidateMachine) || !sku.isSmallBatchValidation()) {
            return false;
        }
        return LhSingleControlMachineUtil.isConfiguredSingleControlMachine(
                context, candidateMachine.getMachineCode());
    }

    /**
     * 按 dayN 增机生效日或普通首台日计划规则对齐换模就绪时间。
     * <p>续作增机补偿的首台候选也已有明确的增机日，必须优先按该日期对齐，
     * 不得再进入“剩余日计划额度为 0 则顺延”的普通首台路径。</p>
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param switchReadyTime 资源约束计算后的换模就绪时间
     * @param shifts 排程窗口班次
     * @param totalScheduledQty 当前 SKU 累计已排量
     * @param addMachineProductionDate dayN 模拟确定的当前增机生效日
     * @param isEnding 是否收尾
     * @return 对齐后的换模就绪时间
     */
    private Date alignSwitchReadyTimeByAddMachineDate(LhScheduleContext context,
                                                       SkuScheduleDTO sku,
                                                       Date switchReadyTime,
                                                       List<LhShiftConfigVO> shifts,
                                                       int totalScheduledQty,
                                                       LocalDate addMachineProductionDate,
                                                       boolean isEnding,
                                                       DailySchedulePhase phase) {
        if (Objects.nonNull(addMachineProductionDate)) {
            // 增机生效日可能为未来业务日，当天班次切片解析不到目标日班次；
            // 改用全窗口班次解析目标日首个允许换模班次，避免对齐静默失效导致提前换模开产。
            List<LhShiftConfigVO> windowShifts = Objects.isNull(context)
                    ? shifts : context.getScheduleWindowShifts();
            Date alignedSwitchReadyTime = alignAddedMachineSwitchReadyTime(
                    sku, switchReadyTime, windowShifts, totalScheduledQty, addMachineProductionDate);
            log.info("新增SKU按dayN增机生效日对齐换模, materialCode: {}, totalScheduledQty: {}, "
                            + "addMachineProductionDate: {}, beforeSwitchReadyTime: {}, afterSwitchReadyTime: {}",
                    Objects.isNull(sku) ? null : sku.getMaterialCode(), totalScheduledQty,
                    addMachineProductionDate, LhScheduleTimeUtil.formatDateTime(switchReadyTime),
                    LhScheduleTimeUtil.formatDateTime(alignedSwitchReadyTime));
            return alignedSwitchReadyTime;
        }
        if (totalScheduledQty <= 0) {
            return alignFirstMachineSwitchReadyTimeByDailyPlan(
                    context, sku, switchReadyTime, shifts, isEnding, phase);
        }
        return switchReadyTime;
    }

    /**
     * 按 dayN 增机生效日或普通首台日计划规则对齐开产时间。
     * <p>实际资源约束仍可将开产推迟到增机日之后，但已消费的日计划剩余额度不再二次推迟补偿机台。</p>
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param productionStartTime 资源约束计算后的开产时间
     * @param shifts 排程窗口班次
     * @param totalScheduledQty 当前 SKU 累计已排量
     * @param addMachineProductionDate dayN 模拟确定的当前增机生效日
     * @param isEnding 是否收尾
     * @param earlyProductionDecision 提前生产判定结果
     * @return 对齐后的开产时间
     */
    private Date alignProductionStartTimeByAddMachineDate(LhScheduleContext context,
                                                           SkuScheduleDTO sku,
                                                           Date productionStartTime,
                                                           List<LhShiftConfigVO> shifts,
                                                           int totalScheduledQty,
                                                           LocalDate addMachineProductionDate,
                                                           boolean isEnding,
                                                           EarlyProductionDecision earlyProductionDecision) {
        if (Objects.nonNull(addMachineProductionDate)) {
            List<LhShiftConfigVO> windowShifts = Objects.isNull(context)
                    ? shifts : context.getScheduleWindowShifts();
            Date alignedProductionStartTime = alignAddedMachineProductionStartTime(
                    sku, productionStartTime, windowShifts, totalScheduledQty, addMachineProductionDate);
            log.info("新增SKU按dayN增机生效日对齐开产, materialCode: {}, totalScheduledQty: {}, "
                            + "addMachineProductionDate: {}, beforeProductionStartTime: {}, afterProductionStartTime: {}",
                    Objects.isNull(sku) ? null : sku.getMaterialCode(), totalScheduledQty,
                    addMachineProductionDate, LhScheduleTimeUtil.formatDateTime(productionStartTime),
                    LhScheduleTimeUtil.formatDateTime(alignedProductionStartTime));
            return alignedProductionStartTime;
        }
        return alignFirstProductionStartTimeByDailyPlan(
                context, sku, productionStartTime, shifts, isEnding, earlyProductionDecision);
    }

    /**
     * 新增非收尾首日无可用日计划额度时，将首个可排时间推进到首个可承接的生产日。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param firstProductionStartTime 当前首个可排时间
     * @param shifts 排程窗口班次
     * @param isEnding 是否收尾
     * @param earlyProductionDecision 提前生产判定结果
     * @return 调整后的首个可排时间
     */
    private Date alignFirstProductionStartTimeByDailyPlan(LhScheduleContext context,
                                                          SkuScheduleDTO sku,
                                                          Date firstProductionStartTime,
                                                          List<LhShiftConfigVO> shifts,
                                                          boolean isEnding,
                                                          EarlyProductionDecision earlyProductionDecision) {
        Map<LocalDate, SkuDailyPlanQuotaDTO> effectiveQuotaMap =
                Objects.isNull(context) || Objects.isNull(sku)
                        ? null : context.resolveEffectiveDailyPlanQuotaMap(sku);
        if (Objects.isNull(sku) || Objects.isNull(firstProductionStartTime)
                || isEnding || CollectionUtils.isEmpty(effectiveQuotaMap)) {
            return firstProductionStartTime;
        }
        LocalDate productionDate = resolveProductionWorkDate(shifts, firstProductionStartTime);
        if (Objects.isNull(productionDate)) {
            return firstProductionStartTime;
        }
        SkuDailyPlanQuotaDTO currentQuota = effectiveQuotaMap.get(productionDate);
        if (hasSchedulableDailyPlanQuota(sku, currentQuota)) {
            return firstProductionStartTime;
        }
        LocalDate nextPlanDate = resolveNextPositiveDailyPlanDate(
                sku, effectiveQuotaMap, productionDate, resolveScheduleTargetLocalDate(context));
        if (Objects.isNull(nextPlanDate)) {
            return firstProductionStartTime;
        }
        // 提前生产准入优先于“首日无 dayN 顺延”，续作补偿复用同一判定结果。
        if (isAllowedFuturePlanEarlyProduction(earlyProductionDecision)) {
            log.info("新增SKU提前生产准入通过，保留当前业务日开产, materialCode: {}, "
                            + "fromProductionDate: {}, futurePlanDate: {}, firstProductionStartTime: {}",
                    sku.getMaterialCode(), productionDate, nextPlanDate,
                    LhScheduleTimeUtil.formatDateTime(firstProductionStartTime));
            return firstProductionStartTime;
        }
        if (sku.isContinuousCompensationSku()
                && !shouldDelayFirstProductionForNoPlanDate(sku, firstProductionStartTime, isEnding)) {
            return firstProductionStartTime;
        }
        Date nextPlanDateStartTime = resolveFirstShiftStartTime(shifts, nextPlanDate);
        if (Objects.isNull(nextPlanDateStartTime) || !nextPlanDateStartTime.after(firstProductionStartTime)) {
            return firstProductionStartTime;
        }
        log.info("新增SKU首个可排时间按日计划额度顺延, materialCode: {}, compensationSku: {}, "
                        + "fromProductionDate: {}, toProductionDate: {}, fromStartTime: {}, toStartTime: {}",
                sku.getMaterialCode(), sku.isContinuousCompensationSku(), productionDate, nextPlanDate,
                LhScheduleTimeUtil.formatDateTime(firstProductionStartTime),
                LhScheduleTimeUtil.formatDateTime(nextPlanDateStartTime));
        return nextPlanDateStartTime;
    }

    /**
     * 首台机台首日无可用日计划额度时，将换模就绪时间推进到首个可承接生产日内的允许换模班次。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param switchReadyTime 当前换模就绪时间
     * @param shifts 排程窗口班次
     * @param isEnding 是否收尾
     * @param phase 当前业务日内阶段
     * @return 调整后的换模就绪时间
     */
    private Date alignFirstMachineSwitchReadyTimeByDailyPlan(LhScheduleContext context,
                                                             SkuScheduleDTO sku,
                                                             Date switchReadyTime,
                                                             List<LhShiftConfigVO> shifts,
                                                             boolean isEnding,
                                                             DailySchedulePhase phase) {
        Map<LocalDate, SkuDailyPlanQuotaDTO> effectiveQuotaMap =
                Objects.isNull(context) || Objects.isNull(sku)
                        ? null : context.resolveEffectiveDailyPlanQuotaMap(sku);
        if (Objects.isNull(sku) || Objects.isNull(switchReadyTime)
                || isEnding || CollectionUtils.isEmpty(effectiveQuotaMap)) {
            return switchReadyTime;
        }
        LocalDate productionDate = resolveProductionWorkDate(shifts, switchReadyTime);
        if (Objects.isNull(productionDate)) {
            return switchReadyTime;
        }
        SkuDailyPlanQuotaDTO currentQuota = effectiveQuotaMap.get(productionDate);
        if (hasSchedulableDailyPlanQuota(sku, currentQuota)) {
            return switchReadyTime;
        }
        LocalDate nextPlanDate = resolveNextPositiveDailyPlanDate(
                sku, effectiveQuotaMap, productionDate, resolveScheduleTargetLocalDate(context));
        if (Objects.isNull(nextPlanDate)) {
            return switchReadyTime;
        }
        EarlyProductionDecision earlyProductionDecision = resolveEarlyProductionDecision(
                context, sku, switchReadyTime, shifts, isEnding, phase);
        if (isAllowedFuturePlanEarlyProduction(earlyProductionDecision)) {
            /*
             * 正常阶段结束后才会进入提前生产，此时当前业务日剩余换模班次即为真实可用资源。
             * 旧逻辑强制顺延到 T+1 会把“最多提前 N 天”退化为只能提前一天，必须保留
             * 当前业务日就绪时间并继续复用晚班不可换模、换模次数等现有约束。
             */
            log.info("新增SKU提前生产准入通过，保留当前业务日首台换模, materialCode: {}, "
                            + "fromProductionDate: {}, futurePlanDate: {}, switchReadyTime: {}",
                    sku.getMaterialCode(), productionDate,
                    earlyProductionDecision.getFuturePlanDate(),
                    LhScheduleTimeUtil.formatDateTime(switchReadyTime));
            return switchReadyTime;
        }
        if (sku.isContinuousCompensationSku()
                && !shouldDelayFirstProductionForNoPlanDate(sku, switchReadyTime, isEnding)) {
            return switchReadyTime;
        }
        Date nextSwitchReadyTime = resolveFirstAllowMouldChangeShiftStartTime(shifts, nextPlanDate);
        if (Objects.isNull(nextSwitchReadyTime) || !nextSwitchReadyTime.after(switchReadyTime)) {
            return switchReadyTime;
        }
        log.info("新增SKU首台换模日期按上机日顺延, materialCode: {}, fromProductionDate: {}, "
                        + "toProductionDate: {}, fromSwitchReadyTime: {}, toSwitchReadyTime: {}",
                sku.getMaterialCode(), productionDate, nextPlanDate,
                LhScheduleTimeUtil.formatDateTime(switchReadyTime),
                LhScheduleTimeUtil.formatDateTime(nextSwitchReadyTime));
        return nextSwitchReadyTime;
    }

    /**
     * 将后续新增机台的首个可排时间推进到dayN模拟确定的增机业务日。
     *
     * @param sku SKU
     * @param firstProductionStartTime 当前首个可排时间
     * @param shifts 排程窗口班次
     * @param totalScheduledQty 当前SKU累计已排量
     * @param addMachineProductionDate 首次需要增机的业务日期
     * @return 调整后的首个可排时间
     */
    private Date alignAddedMachineProductionStartTime(SkuScheduleDTO sku,
                                                       Date firstProductionStartTime,
                                                       List<LhShiftConfigVO> shifts,
                                                       int totalScheduledQty,
                                                       LocalDate addMachineProductionDate) {
        if (Objects.isNull(firstProductionStartTime) || Objects.isNull(addMachineProductionDate)) {
            return firstProductionStartTime;
        }
        Date addMachineStartTime = resolveFirstShiftStartTime(shifts, addMachineProductionDate);
        if (Objects.isNull(addMachineStartTime) || !addMachineStartTime.after(firstProductionStartTime)) {
            return firstProductionStartTime;
        }
        log.info("新增SKU增机生效日期顺延, materialCode: {}, addMachineProductionDate: {}, "
                        + "fromStartTime: {}, toStartTime: {}",
                Objects.isNull(sku) ? null : sku.getMaterialCode(), addMachineProductionDate,
                LhScheduleTimeUtil.formatDateTime(firstProductionStartTime),
                LhScheduleTimeUtil.formatDateTime(addMachineStartTime));
        return addMachineStartTime;
    }

    /**
     * 将后续新增机台的换模就绪时间推进到增机业务日内首个允许换模班次。
     *
     * @param sku SKU
     * @param switchReadyTime 当前换模就绪时间
     * @param shifts 排程窗口班次
     * @param totalScheduledQty 当前SKU累计已排量
     * @param addMachineProductionDate 首次需要增机的业务日期
     * @return 调整后的换模就绪时间
     */
    private Date alignAddedMachineSwitchReadyTime(SkuScheduleDTO sku,
                                                  Date switchReadyTime,
                                                  List<LhShiftConfigVO> shifts,
                                                  int totalScheduledQty,
                                                  LocalDate addMachineProductionDate) {
        if (Objects.isNull(switchReadyTime) || Objects.isNull(addMachineProductionDate)) {
            return switchReadyTime;
        }
        Date addMachineSwitchReadyTime = resolveFirstAllowMouldChangeShiftStartTime(shifts, addMachineProductionDate);
        if (Objects.isNull(addMachineSwitchReadyTime)) {
            return switchReadyTime;
        }
        if (!addMachineSwitchReadyTime.after(switchReadyTime)) {
            return switchReadyTime;
        }
        log.info("新增SKU增机换模日期按增机日顺延, materialCode: {}, addMachineProductionDate: {}, "
                        + "fromSwitchReadyTime: {}, toSwitchReadyTime: {}",
                Objects.isNull(sku) ? null : sku.getMaterialCode(), addMachineProductionDate,
                LhScheduleTimeUtil.formatDateTime(switchReadyTime),
                LhScheduleTimeUtil.formatDateTime(addMachineSwitchReadyTime));
        return addMachineSwitchReadyTime;
    }

    /**
     * 生成当前候选机台的提前生产判定结果。
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param firstProductionStartTime 候选机台首个可排时间
     * @param shifts 排程窗口班次
     * @param isEnding 是否按 SKU 收尾
     * @param phase 当前业务日内阶段
     * @return 当前候选机台的提前生产判定结果
     */
    private EarlyProductionDecision resolveEarlyProductionDecision(LhScheduleContext context,
                                                                    SkuScheduleDTO sku,
                                                                    Date firstProductionStartTime,
                                                                    List<LhShiftConfigVO> shifts,
                                                                    boolean isEnding,
                                                                    DailySchedulePhase phase) {
        if (!isEarlyProductionPhase(phase)) {
            return EarlyProductionDecision.notEarlyProduction(true, "当前阶段不是提前生产阶段");
        }
        if (Objects.isNull(context) || Objects.isNull(sku)) {
            return EarlyProductionDecision.notEarlyProduction(true, "非提前生产判定范围");
        }
        /*
         * 候选分组时已经完成准入并注册中心运行视图。调用处只读取同一次判定，
         * 禁止随着候选机台时间变化重复调用 Checker，避免结构机台统计被本 SKU
         * 刚写入的结果改变后出现前后不一致。
         */
        EarlyProductionRuntimePlan runtimePlan =
                context.getEarlyProductionRuntimePlan(sku);
        if (Objects.nonNull(runtimePlan) && runtimePlan.isActive()
                && Objects.nonNull(runtimePlan.getDecision())) {
            return runtimePlan.getDecision();
        }
        return EarlyProductionDecision.notEarlyProduction(
                false, "提前生产中心运行视图未初始化");
    }

    /**
     * 判断当前日内阶段是否允许消费后续 dayN 并执行提前生产。
     *
     * @param phase 当前业务日内阶段
     * @return true-提前生产阶段；false-在机续排、统一资源竞争或历史遗留阶段
     */
    private boolean isEarlyProductionPhase(DailySchedulePhase phase) {
        return DailySchedulePhase.EARLY_PRODUCTION == phase;
    }

    /**
     * 刷新新增SKU提前生产选机准入结果。
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param shifts 排程窗口班次
     * @param isEnding 是否按 SKU 收尾
     * @param phase 当前业务日内阶段
     */
    private void refreshNewSpecEarlyProductionAdmission(LhScheduleContext context,
                                                        SkuScheduleDTO sku,
                                                        List<LhShiftConfigVO> shifts,
                                                        boolean isEnding,
                                                        DailySchedulePhase phase) {
        if (Objects.isNull(context) || Objects.isNull(sku)) {
            return;
        }
        context.getNewSpecEarlyProductionAllowedMap().remove(sku);
        // 非提前生产阶段不能把提前生产准入标记传递给选机、换模或日计划扣账逻辑。
        if (!isEarlyProductionPhase(phase)) {
            return;
        }
        EarlyProductionRuntimePlan runtimePlan = context.getEarlyProductionRuntimePlan(sku);
        EarlyProductionDecision decision = Objects.isNull(runtimePlan)
                ? null : runtimePlan.getDecision();
        if (Objects.isNull(runtimePlan) || !runtimePlan.isActive()
                || !isAllowedFuturePlanEarlyProduction(decision)) {
            return;
        }
        context.getNewSpecEarlyProductionAllowedMap().put(sku, Boolean.TRUE);
        log.info("新增SKU提前生产选机准入通过, materialCode: {}, currentDate: {}, "
                        + "futurePlanDate: {}, sceneType: {}",
                sku.getMaterialCode(), runtimePlan.getCurrentDate(),
                decision.getFuturePlanDate(), decision.getSceneType());
    }

    /**
     * 将提前生产结构机台数追加到硫化排程结果备注。
     *
     * @param context 排程上下文
     * @param result 硫化排程结果
     * @param decision 提前生产判定结果
     * @param businessDate 实际开产业务日期
     */
    private void appendEarlyProductionRemark(LhScheduleContext context,
                                              LhScheduleResult result,
                                              EarlyProductionDecision decision,
                                              LocalDate businessDate) {
        if (Objects.isNull(result) || Objects.isNull(decision)) {
            return;
        }
        // 与提前生产备注同源回写标识：命中场景且准入通过的结果统一标记为 1
        if (decision.isEarlyProduction() && decision.isAllowed()) {
            result.setIsEarlyProduction("1");
        }
        String remarkFragment = decision.buildRemark();
        if (StringUtils.isEmpty(remarkFragment) || StringUtils.contains(result.getRemark(), remarkFragment)) {
            return;
        }
        String oldRemark = result.getRemark();
        if (StringUtils.isEmpty(oldRemark)) {
            result.setRemark(remarkFragment);
        } else {
            result.setRemark(new StringBuilder(oldRemark.length() + remarkFragment.length() + 1)
                    .append(oldRemark).append('；').append(remarkFragment).toString());
        }
        log.info("提前生产结果备注追加, factoryCode: {}, businessDate: {}, materialCode: {}, "
                        + "structureName: {}, machineCode: {}, sceneType: {}, remark: {}",
                Objects.isNull(context) ? null : context.getFactoryCode(), businessDate,
                result.getMaterialCode(), result.getStructureName(), result.getLhMachineCode(),
                decision.getSceneType(), result.getRemark());
    }

    private boolean shouldDelayFirstProductionForNoPlanDate(SkuScheduleDTO sku,
                                                            Date firstProductionStartTime,
                                                            boolean isEnding) {
        if (Objects.isNull(sku) || Objects.isNull(firstProductionStartTime) || isEnding
                || CollectionUtils.isEmpty(sku.getDailyPlanQuotaMap())) {
            return false;
        }
        if (sku.isContinuousCompensationSku()) {
            return true;
        }
        if (Math.max(0, sku.getMonthlyHistoryShortageQty()) > 0) {
            return false;
        }
        return !StringUtils.equals(ConstructionStageEnum.TRIAL.getCode(), sku.getConstructionStage());
    }

    private boolean hasSchedulableDailyPlanQuota(SkuScheduleDTO sku, SkuDailyPlanQuotaDTO quota) {
        if (Objects.nonNull(sku) && sku.isContinuousCompensationSku()) {
            // 续作补偿只能承接 S4.4 后剩余的日计划额度，首日已满足时不能在首日借用后续额度换模补量。
            return Objects.nonNull(quota) && Math.max(0, quota.getRemainingQty()) > 0;
        }
        return hasPositiveDailyPlanQuota(quota);
    }

    private boolean hasPositiveDailyPlanQuota(SkuDailyPlanQuotaDTO quota) {
        return Objects.nonNull(quota)
                && (Math.max(0, quota.getDayPlanQty()) > 0 || Math.max(0, quota.getRemainingQty()) > 0);
    }

    private LocalDate resolveNextPositiveDailyPlanDate(SkuScheduleDTO sku,
                                                       Map<LocalDate, SkuDailyPlanQuotaDTO> quotaMap,
                                                       LocalDate productionDate,
                                                       LocalDate windowEndDate) {
        if (CollectionUtils.isEmpty(quotaMap) || Objects.isNull(productionDate)) {
            return null;
        }
        for (Map.Entry<LocalDate, SkuDailyPlanQuotaDTO> entry : quotaMap.entrySet()) {
            LocalDate date = entry.getKey();
            if (Objects.isNull(date) || !date.isAfter(productionDate)
                    || (Objects.nonNull(windowEndDate) && date.isAfter(windowEndDate))) {
                continue;
            }
            if (hasSchedulableDailyPlanQuota(sku, entry.getValue())) {
                return date;
            }
        }
        return null;
    }

    private Date resolveFirstShiftStartTime(List<LhShiftConfigVO> shifts, LocalDate productionDate) {
        if (CollectionUtils.isEmpty(shifts) || Objects.isNull(productionDate)) {
            return null;
        }
        for (LhShiftConfigVO shift : shifts) {
            if (Objects.isNull(shift) || Objects.isNull(shift.getShiftStartDateTime())
                    || Objects.isNull(shift.getWorkDate())) {
                continue;
            }
            LocalDate shiftWorkDate = shift.getWorkDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (productionDate.equals(shiftWorkDate)) {
                return shift.getShiftStartDateTime();
            }
        }
        return null;
    }

    /**
     * 判断首台当日开产保护是否适用。
     * <p>适用前提：本 SKU 本轮尚未形成产量（首台）、换模开始仍在当天但完成越过日终
     * （均衡/共用胎胚错峰把首台排入“换模后当日零产”的班次）、当天早班换模后仍有生产班次、
     * 机台就绪不晚于早班且早班换模次数未超过每日总上限。</p>
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param totalScheduledQty 本 SKU 本轮累计已排量
     * @param switchDurationHours 换模时长
     * @param switchReadyTime 换模就绪时间
     * @param mouldChangeStartTime 均衡分配后的换模开始时间
     * @param sameDayMorningSwitchTime 当天早班换模开始时间
     * @return true-可启用首台当日开产保护
     */
    private boolean isFirstMachineSameDayProductionProtection(LhScheduleContext context,
                                                              DayScheduleContext dayContext,
                                                              int totalScheduledQty,
                                                              int switchDurationHours,
                                                              Date switchReadyTime,
                                                              Date mouldChangeStartTime,
                                                              Date sameDayMorningSwitchTime) {
        if (Objects.isNull(context) || Objects.isNull(dayContext)
                || totalScheduledQty > 0
                || Objects.isNull(sameDayMorningSwitchTime)
                || Objects.isNull(mouldChangeStartTime)
                || Objects.isNull(switchReadyTime)
                || !dayContext.contains(mouldChangeStartTime)) {
            return false;
        }
        // 早班必须比均衡结果更早，且早班换模完成后当天仍有生产班次
        if (!sameDayMorningSwitchTime.before(mouldChangeStartTime)) {
            return false;
        }
        Date morningMouldChangeCompleteTime =
                LhScheduleTimeUtil.addHours(sameDayMorningSwitchTime, switchDurationHours);
        if (Objects.isNull(morningMouldChangeCompleteTime)
                || !morningMouldChangeCompleteTime.before(dayContext.getDayEndTime())) {
            return false;
        }
        // 机台就绪不能晚于早班换模开始
        if (switchReadyTime.after(sameDayMorningSwitchTime)) {
            return false;
        }
        // 当天早班 +1 后不得超过每日换模总上限（早班配额满但总上限未满时允许首台占用早班）
        String dateKey = LhScheduleTimeUtil.formatDate(sameDayMorningSwitchTime);
        int[] counts = context.getDailyMouldChangeCountMap().get(dateKey);
        int totalUsed = counts == null ? 0 : (counts[0] + counts[1]);
        return totalUsed < LhScheduleTimeUtil.getDailyMouldChangeLimit(context);
    }

    /**
     * 为首台当日开产保护手动登记当天早班换模次数。
     * <p>该登记与均衡器早班计数同口径：按日期在早班计数 +1，后续失败回滚时
     * 均衡器按早班时间正确回滚，不重复占用或泄漏配额。</p>
     *
     * @param context 排程上下文
     * @param mouldChangeStartTime 早班换模开始时间
     */
    private void registerFirstMachineMorningMouldChangeCount(LhScheduleContext context,
                                                             Date mouldChangeStartTime) {
        if (Objects.isNull(context) || Objects.isNull(mouldChangeStartTime)) {
            return;
        }
        String dateKey = LhScheduleTimeUtil.formatDate(mouldChangeStartTime);
        int[] counts = context.getDailyMouldChangeCountMap()
                .computeIfAbsent(dateKey, key -> new int[]{0, 0});
        if (LhScheduleTimeUtil.isMorningShift(context, mouldChangeStartTime)) {
            counts[0]++;
        }
    }

    /**
     * 解析指定业务日内首个允许换模的班次开始时间。
     *
     * @param shifts 排程窗口班次
     * @param productionDate 业务日期
     * @return 首个允许换模班次开始时间
     */
    private Date resolveFirstAllowMouldChangeShiftStartTime(List<LhShiftConfigVO> shifts,
                                                            LocalDate productionDate) {
        if (CollectionUtils.isEmpty(shifts) || Objects.isNull(productionDate)) {
            return null;
        }
        for (LhShiftConfigVO shift : shifts) {
            if (Objects.isNull(shift) || Objects.isNull(shift.getShiftStartDateTime())
                    || Objects.isNull(shift.getWorkDate()) || !shift.isAllowMouldChange()) {
                continue;
            }
            LocalDate shiftWorkDate = shift.getWorkDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (productionDate.equals(shiftWorkDate)) {
                return shift.getShiftStartDateTime();
            }
        }
        return null;
    }

    /**
     * 基于当前排程结果重建结构/SKU已排机台统计。
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     */
    private void rebuildScheduledMachineCountMap(LhScheduleContext context, List<LhShiftConfigVO> shifts) {
        if (Objects.isNull(context)) {
            return;
        }
        // S4.5 与 S4.4 共用上下文重建入口，避免两个阶段统计续作占用机台的口径不一致。
        int recordCount = context.rebuildScheduledMachineCountMaps(shifts);
        log.debug("提前生产已排机台统计重建完成, factoryCode: {}, resultCount: {}, recordDateCount: {}",
                context.getFactoryCode(), context.getScheduleResultList().size(), recordCount);
    }

    /**
     * 根据排程结果登记结构/SKU已排机台。
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param shifts 排程窗口班次
     */
    private void recordScheduledMachineForResult(LhScheduleContext context,
                                                 LhScheduleResult result,
                                                 List<LhShiftConfigVO> shifts) {
        recordScheduledMachineForResult(context, result, shifts, true);
    }

    /**
     * 根据排程结果登记结构/SKU已排机台。
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param shifts 排程窗口班次
     * @param logEnabled 是否输出回写日志
     * @return 登记的业务日数量
     */
    private int recordScheduledMachineForResult(LhScheduleContext context,
                                                LhScheduleResult result,
                                                List<LhShiftConfigVO> shifts,
                                                boolean logEnabled) {
        if (Objects.isNull(context) || Objects.isNull(result) || CollectionUtils.isEmpty(shifts)
                || StringUtils.isEmpty(result.getLhMachineCode())) {
            return 0;
        }
        Set<LocalDate> recordedDateSet = new LinkedHashSet<LocalDate>(3);
        for (LhShiftConfigVO shift : shifts) {
            if (Objects.isNull(shift) || Objects.isNull(shift.getShiftIndex())) {
                continue;
            }
            Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
            if (Objects.isNull(planQty) || planQty <= 0) {
                continue;
            }
            LocalDate businessDate = resolveShiftWorkDate(shift);
            if (Objects.nonNull(businessDate)) {
                recordedDateSet.add(businessDate);
            }
        }
        for (LocalDate businessDate : recordedDateSet) {
            context.recordScheduledMachine(businessDate, result.getStructureName(),
                    result.getMaterialCode(), result.getProductStatus(), result.getLhMachineCode());
            if (logEnabled) {
                log.info("新增机台回写提前生产统计, factoryCode: {}, businessDate: {}, materialCode: {}, "
                                + "structureName: {}, machineCode: {}, structureScheduledTotal: {}, skuScheduledTotal: {}",
                        context.getFactoryCode(), businessDate, result.getMaterialCode(), result.getStructureName(),
                        result.getLhMachineCode(),
                        context.getStructureScheduledMachineCount(businessDate, result.getStructureName()),
                        context.getSkuScheduledMachineCount(
                                businessDate, result.getMaterialCode(), result.getProductStatus()));
            }
        }
        return recordedDateSet.size();
    }

    /**
     * 解析生产段开产业务日。
     *
     * @param segment 生产段
     * @param shifts 排程窗口班次
     * @return 开产业务日
     */
    private LocalDate resolveSegmentStartProductionDate(MachineProductionSegment segment,
                                                        List<LhShiftConfigVO> shifts) {
        if (Objects.isNull(segment)) {
            return null;
        }
        LhShiftConfigVO shift = findShiftByIndex(shifts, segment.getStartProductionShiftIndex());
        return resolveShiftWorkDate(shift);
    }

    /**
     * 根据生产时刻解析所属班次的业务日期。
     *
     * @param shifts 排程窗口班次
     * @param productionTime 生产时刻
     * @return 所属班次业务日期；未命中排程窗口返回null
     */
    private LocalDate resolveProductionWorkDate(List<LhShiftConfigVO> shifts, Date productionTime) {
        if (CollectionUtils.isEmpty(shifts) || Objects.isNull(productionTime)) {
            return null;
        }
        for (LhShiftConfigVO shift : shifts) {
            if (Objects.isNull(shift) || Objects.isNull(shift.getShiftStartDateTime())
                    || Objects.isNull(shift.getShiftEndDateTime())) {
                continue;
            }
            if (!productionTime.before(shift.getShiftStartDateTime())
                    && productionTime.before(shift.getShiftEndDateTime())) {
                return resolveShiftWorkDate(shift);
            }
        }
        return null;
    }

    /**
     * 判断当前SKU是否带有多日窗口账本。
     *
     * @param sku SKU
     * @return true-多日窗口账本；false-仅单日或无账本
     */
    private boolean hasMultiDayQuotaWindow(SkuScheduleDTO sku) {
        return sku != null && !CollectionUtils.isEmpty(sku.getDailyPlanQuotaMap())
                && sku.getDailyPlanQuotaMap().size() > 1;
    }

    /**
     * 判断当前新增待排队列是否只剩当前SKU。
     *
     * @param context 排程上下文
     * @return true-只剩一个待排SKU；false-仍有后续SKU需要保留窗口产能
     */
    private boolean isOnlyPendingNewSpecSku(LhScheduleContext context) {
        return context != null && !CollectionUtils.isEmpty(context.getNewSpecSkuList())
                && context.getNewSpecSkuList().size() == 1;
    }

    /**
     * 正规非收尾多机台场景下，若后续 dayN 账本仍有可借额度，
     * 尾机台应补满当前可生产段，避免只排部分班次。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param policy 排产数量策略
     * @param role 机台角色
     * @param segment 当前机台生产段
     * @param remainingQty 本轮窗口剩余目标量
     * @return 尾机台补满量；0-沿用默认尾量逻辑
     */
    private int resolveTailFillPlanQty(LhScheduleContext context,
                                       SkuScheduleDTO sku,
                                       ProductionQuantityPolicy policy,
                                       MachineScheduleRole role,
                                       MachineProductionSegment segment,
                                       int remainingQty) {
        if (sku == null || policy == null || role != MachineScheduleRole.TAIL_MACHINE
                || segment == null || CollectionUtils.isEmpty(segment.getShiftCapacityMap())) {
            return 0;
        }
        if (!policy.isAllowFillStartedShift() || policy.isStrictUpperLimit()) {
            return 0;
        }
        if (!shouldUseFormalNonEndingMinimumTarget(context, sku, policy)) {
            return 0;
        }
        Map<LocalDate, SkuDailyPlanQuotaDTO> effectiveQuotaMap =
                context.resolveEffectiveDailyPlanQuotaMap(sku);
        int remainingQuotaQty = SkuDailyPlanQuotaUtil.sumRemainingQty(effectiveQuotaMap);
        boolean multiDayQuota = hasMultiplePositiveQuotaDays(effectiveQuotaMap);
        if (!multiDayQuota && remainingQuotaQty <= remainingQty) {
            return 0;
        }
        int roundedRemainingQty = roundUpToShiftCapacity(remainingQty, segment.getShiftCapacity());
        int tailFilledQty = roundedRemainingQty + Math.max(0, segment.getShiftCapacity());
        if (tailFilledQty <= roundedRemainingQty) {
            return 0;
        }
        return Math.min(tailFilledQty, segment.getMaxQtyToWindowEnd());
    }

    /**
     * 新增SKU按dayN欠产节奏提前给后续机台留量。
     * <p>该方法只影响新增排产多候选机台场景；收尾场景参与动态拆量但仍严格截断，试制等严格目标场景沿用原有语义。</p>
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param candidates 候选机台
     * @param excludedMachineCodes 已排除机台
     * @param policy 排产数量策略
     * @param segment 当前机台生产段
     * @param targetQty 窗口目标量
     * @param scheduledQty 当前已排量
     * @param defaultPlanQty 原计划量
     * @param dayContext 当前实际业务日，用于限定日志中的当日目标机台数
     * @param dailyOrderEntry 当前真实遍历对应的每日顺序日志明细
     * @return 当前机台计划量
     */
    private int resolveDynamicMachinePlanQtyByDailyCapacity(LhScheduleContext context,
                                                            SkuScheduleDTO sku,
                                                            List<MachineScheduleDTO> candidates,
                                                            Set<String> excludedMachineCodes,
                                                            ProductionQuantityPolicy policy,
                                                            MachineProductionSegment segment,
                                                            MachineScheduleDTO candidateMachine,
                                                            List<LhShiftConfigVO> shifts,
                                                            ICapacityCalculateStrategy capacityCalculate,
                                                            int targetQty,
                                                            int scheduledQty,
                                                            int defaultPlanQty,
                                                            DayScheduleContext dayContext,
                                                            DailyNewSpecOrderLogEntry dailyOrderEntry) {
        if (!shouldUseDailyDynamicMachineAllocation(
                context, sku, candidates, excludedMachineCodes, policy, segment)) {
            return defaultPlanQty;
        }
        int remainingTargetQty = Math.max(0, targetQty - scheduledQty);
        if (remainingTargetQty <= 0 || defaultPlanQty <= 0) {
            return defaultPlanQty;
        }
        int availableMachineCount = countAvailableCandidateMachines(candidates, excludedMachineCodes);
        int requiredMachineCountByDailyCapacity = resolveRequiredMachineCountByDailyCapacity(
                context, sku, candidates, excludedMachineCodes, policy, segment, candidateMachine,
                shifts, capacityCalculate, remainingTargetQty, availableMachineCount);
        /*
         * 直接使用本次真实 dayN 模拟的目标机台数和生效日期回填日志，不为日志重新运行模拟。
         * 后续总目标拆量若推导出更大机台数，会在同一条明细上继续更新。
         */
        this.updateDailyRequiredMachineCount(
                dayContext, dailyOrderEntry, requiredMachineCountByDailyCapacity,
                segment.getAddMachineProductionDateList());
        if (requiredMachineCountByDailyCapacity == 0 && segment.isExistingSameMaterialSatisfied()) {
            // 已有同物料机台满足逐日加机台规则时，当前候选不再因目标剩余继续新增。
            log.info("新增SKU已有同物料机台满足dayN增机台规则，跳过当前新增候选, "
                            + "materialCode: {}, machineCode: {}, remainingTargetQty: {}, "
                            + "remainingScheduleQty: {}, existingSameMaterialSatisfied: true",
                    sku.getMaterialCode(), segment.getMachineCode(), remainingTargetQty,
                    sku.getRemainingScheduleQty());
            appendNewSpecDailyRhythmStopProcessLog(context, sku, segment.getMachineCode(),
                    targetQty, scheduledQty,
                    "已有同物料有效机台满足当前日优先dayN节奏");
            return 0;
        }
        if (shouldFillMachineToWindowEndForFutureDayDemand(
                context, sku, policy, segment, requiredMachineCountByDailyCapacity)) {
            segment.setFutureDayDemandMachineCount(requiredMachineCountByDailyCapacity);
            log.info("新增SKU因T+3日计划需求保留窗口内满班, materialCode: {}, machineCode: {}, "
                            + "remainingTargetQty: {}, maxQtyToWindowEnd: {}, dayN推导机台数: {}",
                    sku.getMaterialCode(), segment.getMachineCode(), remainingTargetQty,
                    segment.getMaxQtyToWindowEnd(), requiredMachineCountByDailyCapacity);
            return segment.getMaxQtyToWindowEnd();
        }
        boolean suppressTotalExpansion = isDailyCapacitySimulationSatisfied(
                sku, requiredMachineCountByDailyCapacity);
        if (MachineScheduleRole.FULL_RUN_MACHINE == segment.getRole()
                && shouldUseFormalNonEndingMinimumTarget(context, sku, policy)
                && hasMultiplePositiveQuotaDays(
                context.resolveEffectiveDailyPlanQuotaMap(sku))
                && !suppressTotalExpansion) {
            return defaultPlanQty;
        }
        boolean needAddMachineByDailyCapacity = requiredMachineCountByDailyCapacity > 1;
        if (!needAddMachineByDailyCapacity) {
            if (suppressTotalExpansion) {
                segment.setStopAfterCurrentForSmallShortage(true);
            }
            if (policy.isAllowFillStartedShift()) {
                log.info("新增SKU尾机台进入非收尾补满判定, materialCode: {}, machineCode: {}, "
                                + "remainingTargetQty: {}, defaultPlanQty: {}, maxQtyToWindowEnd: {}, role: {}",
                        sku.getMaterialCode(), segment.getMachineCode(), remainingTargetQty,
                        defaultPlanQty, segment.getMaxQtyToWindowEnd(), segment.getRole());
                return resolveSettledTailMachinePlanQty(segment, remainingTargetQty, defaultPlanQty);
            }
            log.info("新增SKU当前班次因严格目标量达标停止扩量, materialCode: {}, machineCode: {}, "
                            + "remainingTargetQty: {}, maxQtyToWindowEnd: {}, role: {}",
                    sku.getMaterialCode(), segment.getMachineCode(), remainingTargetQty,
                    segment.getMaxQtyToWindowEnd(), segment.getRole());
            return Math.min(remainingTargetQty, segment.getMaxQtyToWindowEnd());
        }
        if (availableMachineCount <= 1) {
            return defaultPlanQty;
        }
        int requiredMachineCount = Math.min(
                requiredMachineCountByDailyCapacity, availableMachineCount);
        // 总目标拆量同样只回填计算结果；若后续机台尚未到生效日，当前日过滤仍由已有日期列表完成。
        this.updateDailyRequiredMachineCount(
                dayContext, dailyOrderEntry, requiredMachineCount,
                segment.getAddMachineProductionDateList());
        int balancedPlanQty = roundUpToShiftCapacity(
                divideCeiling(remainingTargetQty, requiredMachineCount), segment.getShiftCapacity());
        balancedPlanQty = Math.min(balancedPlanQty, segment.getMaxQtyToWindowEnd());
        balancedPlanQty = Math.min(balancedPlanQty, remainingTargetQty);
        if (balancedPlanQty <= 0) {
            return defaultPlanQty;
        }
        log.info("新增SKU按dayN节奏动态扩机台, materialCode: {}, 当前机台: {}, 已排: {}, 目标: {}, "
                        + "默认计划量: {}, 动态计划量: {}, 可用候选数: {}, 预计机台数: {}, dayN推导机台数: {}",
                sku.getMaterialCode(), segment.getMachineCode(), scheduledQty, targetQty, defaultPlanQty,
                balancedPlanQty, availableMachineCount, requiredMachineCount, requiredMachineCountByDailyCapacity);
        return balancedPlanQty;
    }

    /**
     * 判断是否因 T+3 日计划需求保留当前机台到窗口结束。
     * <p>本规则只适用于欠产未超过阈值的新增排产非收尾 SKU：
     * dayN 模拟已确认需要多机台保障 T+3 日计划时，T+2 的可用班次也应按班产排满；
     * 实际扣账仍不提前消费 T+3，超出 T~T+2 额度的部分沿用原满班补齐账本记录。</p>
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param policy 排产数量策略
     * @param segment 当前机台生产段
     * @param requiredMachineCountByDailyCapacity dayN 模拟推导机台数
     * @return true-当前机台按窗口内有效产能排满；false-沿用原拆量
     */
    private boolean shouldFillMachineToWindowEndForFutureDayDemand(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            ProductionQuantityPolicy policy,
            MachineProductionSegment segment,
            int requiredMachineCountByDailyCapacity) {
        if (context == null || sku == null || policy == null || segment == null
                || requiredMachineCountByDailyCapacity <= 1
                || segment.getMaxQtyToWindowEnd() <= 0
                || policy.isEnding()
                || policy.isStrictUpperLimit()
                || !policy.isAllowFillStartedShift()) {
            return false;
        }
        int threshold = resolveNewSpecShortageAddMachineThreshold(context);
        if (threshold <= 0 || Math.max(0, sku.getMonthlyHistoryShortageQty()) > threshold) {
            return false;
        }
        LocalDate windowEndDate = resolveScheduleTargetLocalDate(context);
        if (Objects.isNull(windowEndDate) || CollectionUtils.isEmpty(sku.getDailyPlanQuotaMap())) {
            return false;
        }
        SkuDailyPlanQuotaDTO nextDayQuota = sku.getDailyPlanQuotaMap().get(windowEndDate.plusDays(1));
        return (Objects.nonNull(nextDayQuota) && Math.max(0, nextDayQuota.getDayPlanQty()) > 0)
                || Math.max(0, sku.getNextDayPlanQtyAfterWindow()) > 0;
    }

    /**
     * 判断 dayN 理论产能模拟是否已经确认当前启用机台满足增机台规则。
     * <p>小欠产模式下，当前日和后一天均按单日理论产能判断；
     * 该结果用于阻断后续按真实换模后窗口缺口继续扩机台。</p>
     *
     * @param sku SKU
     * @param requiredMachineCountByDailyCapacity dayN 模拟推导的当前新增阶段所需机台数
     * @return true-当前机台已满足增机台规则；false-仍允许按缺口继续尝试后续机台
     */
    private boolean isDailyCapacitySimulationSatisfied(SkuScheduleDTO sku,
                                                       int requiredMachineCountByDailyCapacity) {
        return requiredMachineCountByDailyCapacity == 1
                && sku != null
                && sku.getShiftCapacity() > 0
                && !CollectionUtils.isEmpty(sku.getDailyPlanQuotaMap());
    }

    /**
     * 当前窗口 dayN 模拟已确认不需要继续扩机时，尾机台只保留满足剩余需求所需的满班量，
     * 不再额外多吃一整班，避免尾量跨到下一业务日。
     *
     * @param segment 当前机台生产段
     * @param remainingTargetQty 剩余目标量
     * @param defaultPlanQty 原计划量
     * @return 收敛后的计划量
     */
    private int resolveSettledTailMachinePlanQty(MachineProductionSegment segment,
                                                 int remainingTargetQty,
                                                 int defaultPlanQty) {
        if (segment == null || segment.getRole() != MachineScheduleRole.TAIL_MACHINE || defaultPlanQty <= 0) {
            return defaultPlanQty;
        }
        int roundedRemainingQty = roundUpToShiftCapacity(remainingTargetQty, segment.getShiftCapacity());
        if (roundedRemainingQty <= 0 || roundedRemainingQty >= defaultPlanQty) {
            return defaultPlanQty;
        }
        if (defaultPlanQty >= segment.getMaxQtyToWindowEnd()) {
            log.info("新增SKU尾机台保持整段补满, materialCode: {}, machineCode: {}, "
                            + "remainingTargetQty: {}, roundedRemainingQty: {}, defaultPlanQty: {}, maxQtyToWindowEnd: {}",
                    segment.getMaterialCode(), segment.getMachineCode(), remainingTargetQty,
                    roundedRemainingQty, defaultPlanQty, segment.getMaxQtyToWindowEnd());
            return defaultPlanQty;
        }
        return Math.min(roundedRemainingQty, segment.getMaxQtyToWindowEnd());
    }

    /**
     * 记录新增SKU当前机台计划量的最终决策摘要，便于排查单机台补满窗口与严格目标量的差异。
     *
     * @param sku SKU
     * @param policy 排产数量策略
     * @param isEnding 是否收尾
     * @param isSingleMachine 是否命中单机台补满窗口
     * @param targetQty 当前窗口目标量
     * @param maxQtyToWindowEnd 当前机台最大可排量
     * @param finalPlanQty 当前机台最终计划量
     * @param actualScheduledQty 当前机台实际落地量
     */
    private void logNewSpecMachinePlanDecision(SkuScheduleDTO sku,
                                               ProductionQuantityPolicy policy,
                                               boolean isEnding,
                                               boolean isSingleMachine,
                                               int targetQty,
                                               int maxQtyToWindowEnd,
                                               int finalPlanQty,
                                               Integer actualScheduledQty) {
        if (sku == null || policy == null) {
            return;
        }
        log.info("新增SKU机台计划量决策, materialCode: {}, skuType: {}, isEnding: {}, isTrial: {}, "
                        + "isSmallBatch: {}, isSingleMachine: {}, targetQty: {}, maxQtyToWindowEnd: {}, "
                        + "finalPlanQty: {}, actualScheduledQty: {}, allowOverTarget: {}",
                sku.getMaterialCode(), resolveNewSpecSkuType(sku), isEnding,
                StringUtils.equals(ConstructionStageEnum.TRIAL.getCode(), sku.getConstructionStage()),
                sku.isSmallBatchValidation(), isSingleMachine, targetQty, maxQtyToWindowEnd,
                finalPlanQty, actualScheduledQty, policy.isAllowFillStartedShift());
    }

    /**
     * 判断当前SKU是否使用新增多机台动态拆量。
     *
     * @param sku SKU
     * @param candidates 候选机台
     * @param excludedMachineCodes 已排除机台
     * @param policy 排产数量策略
     * @param segment 当前机台生产段
     * @return true-使用动态拆量；false-沿用原逻辑
     */
    private boolean shouldUseDailyDynamicMachineAllocation(LhScheduleContext context,
                                                           SkuScheduleDTO sku,
                                                           List<MachineScheduleDTO> candidates,
                                                           Set<String> excludedMachineCodes,
                                                           ProductionQuantityPolicy policy,
                                                           MachineProductionSegment segment) {
        if (sku == null || policy == null || segment == null) {
            return false;
        }
        if (policy.isStrictUpperLimit() && !policy.isEnding()) {
            return false;
        }
        if (CollectionUtils.isEmpty(context.resolveEffectiveDailyPlanQuotaMap(sku))
                || CollectionUtils.isEmpty(candidates)) {
            return false;
        }
        return candidates.size() > 1 && countAvailableCandidateMachines(candidates, excludedMachineCodes) > 0;
    }

    /**
     * 判断单台机台在当前追补窗口内是否无法消化dayN欠产。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param segment 当前机台生产段
     * @param remainingTargetQty 本轮窗口剩余目标量
     * @return true-需要提前增加机台；false-当前机台可覆盖追补窗口
     */
    private int resolveRequiredMachineCountByDailyCapacity(LhScheduleContext context,
                                                           SkuScheduleDTO sku,
                                                           List<MachineScheduleDTO> candidates,
                                                           Set<String> excludedMachineCodes,
                                                           ProductionQuantityPolicy policy,
                                                           MachineProductionSegment segment,
                                                           MachineScheduleDTO candidateMachine,
                                                           List<LhShiftConfigVO> shifts,
                                                           ICapacityCalculateStrategy capacityCalculate,
                                                           int remainingTargetQty,
                                                           int availableMachineCount) {
        if (Objects.isNull(context) || Objects.isNull(sku) || Objects.isNull(segment)
                || CollectionUtils.isEmpty(candidates) || CollectionUtils.isEmpty(shifts)
                || Objects.isNull(candidateMachine) || Objects.isNull(capacityCalculate)
                || availableMachineCount <= 0) {
            return 0;
        }
        LocalDate productionDate = resolveSegmentStartProductionDate(segment, shifts);
        if (Objects.isNull(productionDate)) {
            productionDate = context.getScheduleDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (!segment.isNeedChangeover()
                && this.isContinuationAddMachineCandidate(sku)
                && Objects.nonNull(sku.getFirstAddMachineProductionDate())) {
            /*
             * 原续作机台原模具重启的增机依据和开产下限都来自
             * firstAddMachineProductionDate 的统一Map目标。这里继续使用同一业务日期统计目标
             * 机台数，避免按自然时间戳或相邻班次误取其他业务日的机台数。
             */
            productionDate = sku.getFirstAddMachineProductionDate();
        } else {
            productionDate = this.resolveRequiredMachineCountDate(
                    context, sku, productionDate);
        }
        if (!this.lhDailyMouldCalcService.hasRequiredMachineCount(
                context, sku.getMaterialCode(), sku.getProductStatus(), productionDate)) {
            log.warn("新增SKU目标机台数Map结果缺失，禁止新增机台, factoryCode: {}, batchNo: {}, "
                            + "materialCode: {}, productStatus: {}, productionDate: {}",
                    context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(),
                    sku.getProductStatus(), productionDate);
            return 0;
        }
        int targetTotalMachineCount = this.lhDailyMouldCalcService.getRequiredMachineCount(
                context, sku.getMaterialCode(), sku.getProductStatus(), productionDate);
        int existingMachineCount = countExistingSameMaterialResults(context, sku, null);
        if (existingMachineCount >= targetTotalMachineCount) {
            segment.setExistingSameMaterialSatisfied(true);
            log.info("新增SKU已有同物料机台满足统一Map目标总机台数, factoryCode: {}, batchNo: {}, "
                            + "materialCode: {}, productStatus: {}, productionDate: {}, machineCode: {}, "
                            + "targetTotalMachineCount: {}, currentEffectiveMachineCount: {}, remainingTargetQty: {}",
                    context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(), sku.getProductStatus(),
                    productionDate, segment.getMachineCode(), targetTotalMachineCount,
                    existingMachineCount, remainingTargetQty);
            return 0;
        }
        int requiredAddMachineCount = Math.min(
                Math.max(0, targetTotalMachineCount - existingMachineCount), availableMachineCount);
        log.info("新增SKU统一Map目标机台数判断, factoryCode: {}, batchNo: {}, materialCode: {}, "
                        + "productStatus: {}, productionDate: {}, machineCode: {}, targetTotalMachineCount: {}, "
                        + "currentEffectiveMachineCount: {}, requiredAddMachineCount: {}, availableMachineCount: {}, "
                        + "remainingTargetQty: {}",
                context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(), sku.getProductStatus(),
                productionDate, segment.getMachineCode(), targetTotalMachineCount, existingMachineCount,
                requiredAddMachineCount, availableMachineCount, remainingTargetQty);
        appendUnifiedRequiredMachineCountProcessLog(
                context, sku, segment, productionDate, targetTotalMachineCount,
                existingMachineCount, requiredAddMachineCount, remainingTargetQty);
        return requiredAddMachineCount;
    }

    /**
     * 追加新增排产统一目标机台数过程日志。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param segment 当前机台生产段
     * @param productionDate 当前业务日
     * @param targetTotalMachineCount Map目标总机台数
     * @param currentEffectiveMachineCount 当前有效机台数
     * @param requiredAddMachineCount 仍需新增机台数
     * @param remainingTargetQty 当前剩余目标量
     */
    private void appendUnifiedRequiredMachineCountProcessLog(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            MachineProductionSegment segment,
            LocalDate productionDate,
            int targetTotalMachineCount,
            int currentEffectiveMachineCount,
            int requiredAddMachineCount,
            int remainingTargetQty) {
        String detail = new StringBuilder(256)
                .append("materialCode=").append(sku.getMaterialCode())
                .append(", productStatus=").append(sku.getProductStatus())
                .append(", productionDate=").append(productionDate)
                .append(", machineCode=").append(segment.getMachineCode())
                .append(", targetTotalMachineCount=").append(targetTotalMachineCount)
                .append(", currentEffectiveMachineCount=").append(currentEffectiveMachineCount)
                .append(", requiredAddMachineCount=").append(requiredAddMachineCount)
                .append(", remainingTargetQty=").append(remainingTargetQty)
                .toString();
        PriorityTraceLogHelper.appendProcessLog(context, "新增SKU统一Map目标机台数判断", detail);
    }

    /**
     * 判断新增 SKU 是否已由同物料纯续作机台满足原始 dayN 最小机台数。
     * <p>S4.5 原始新增列表可能仍保留同物料 SKU，如果 MES 在机续作机台已经覆盖 dayN 节奏，
     * 不能再因为硫化余量、目标剩余或欠产未清零重新新增换模上机。</p>
     *
     * @param context 排程上下文
     * @param sku 当前新增SKU
     * @param quantityPolicy 数量控制策略
     * @return true-跳过新增排产；false-继续走新增选机
     */
    private boolean shouldSkipNewSpecBecauseContinuousSatisfiesOriginalDayMinimum(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            ProductionQuantityPolicy quantityPolicy) {
        if (Objects.isNull(context) || Objects.isNull(sku) || Objects.isNull(quantityPolicy)
                || StringUtils.isEmpty(sku.getMaterialCode())) {
            return false;
        }
        Set<String> continuousMachineCodes = resolvePureContinuousMachineCodes(
                context, sku.getMaterialCode(), sku.getProductStatus());
        if (CollectionUtils.isEmpty(continuousMachineCodes)) {
            return false;
        }
        int dailyStandardQty = resolveNewSpecDailyStandardQty(context, sku);
        if (dailyStandardQty <= 0) {
            return false;
        }
        List<LocalDate> checkDateList = resolveNewSpecDayMinimumCheckDates(context, sku);
        if (!hasPositiveOriginalNewSpecDayPlan(context, sku, checkDateList)) {
            return false;
        }
        // 欠产超过阈值时走强制增机台模式（spec 7），不用逐日后看判断是否跳过新增
        int shortageAddMachineThreshold = DailyMachineExpansionPlanner.resolveShortageAddMachineThreshold(context);
        int historyShortageQty = Math.max(0, sku.getMonthlyHistoryShortageQty());
        if (shortageAddMachineThreshold > 0 && historyShortageQty > shortageAddMachineThreshold) {
            return false;
        }
        // 复用续作公共逐日后看判断，统一新增排产与续作的加机台口径，避免产能口径和末日处理不一致
        LocalDate firstAddMachineDate = DailyMachineExpansionPlanner.resolveFirstDailyLookAheadAddMachineDate(
                this.lhDailyMouldCalcService, context, sku, continuousMachineCodes.size(),
                ScheduleTypeEnum.NEW_SPEC.getCode());
        if (Objects.nonNull(firstAddMachineDate)) {
            log.info("新增SKU同物料续作机台不足，继续新增选机, materialCode: {}, 首次需加机日期: {}, "
                            + "SKU日标准产量: {}, 已有续作机台数: {}, 续作机台: {}, 判断口径: 当前日不足且下一生产日也不足",
                    sku.getMaterialCode(), firstAddMachineDate, dailyStandardQty,
                    continuousMachineCodes.size(), String.join(",", continuousMachineCodes));
            return false;
        }
        log.info("新增SKU跳过，同物料已有纯续作机台满足当前日优先dayN节奏, materialCode: {}, "
                        + "SKU日标准产量: {}, 已有续作机台数: {}, 续作机台: {}, 判断口径: 当前日满足则不因后续dayN单日增大提前加机台",
                sku.getMaterialCode(), dailyStandardQty,
                continuousMachineCodes.size(), String.join(",", continuousMachineCodes));
        appendContinuousSatisfiedNewSpecSkipProcessLog(
                context, sku, dailyStandardQty, continuousMachineCodes.size(), continuousMachineCodes);
        return true;
    }

    /**
     * 判断原始 dayN 是否存在正计划量。
     *
     * @param context 排程上下文
     * @param sku 当前新增SKU
     * @param checkDateList 待检查业务日
     * @return true-存在正计划量；false-没有正计划量
     */
    private boolean hasPositiveOriginalNewSpecDayPlan(LhScheduleContext context,
                                                      SkuScheduleDTO sku,
                                                      List<LocalDate> checkDateList) {
        if (CollectionUtils.isEmpty(checkDateList)) {
            return false;
        }
        for (LocalDate productionDate : checkDateList) {
            if (resolveOriginalNewSpecDayPlanQty(context, sku, productionDate) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 收集同物料纯续作机台编码。
     *
     * @param context 排程上下文
     * @param materialCode 物料编码
     * @param productStatus 产品状态
     * @return 纯续作机台编码集合
     */
    private Set<String> resolvePureContinuousMachineCodes(LhScheduleContext context,
                                                          String materialCode,
                                                          String productStatus) {
        Set<String> machineCodes = new LinkedHashSet<String>(4);
        if (Objects.isNull(context) || StringUtils.isEmpty(materialCode)
                || CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return machineCodes;
        }
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (Objects.isNull(result)
                    || !StringUtils.equals(materialCode, result.getMaterialCode())
                    || !StringUtils.equals(StringUtils.trimToEmpty(productStatus),
                    StringUtils.trimToEmpty(result.getProductStatus()))
                    || !StringUtils.equals(ScheduleTypeEnum.CONTINUOUS.getCode(), result.getScheduleType())
                    || StringUtils.equals("1", result.getIsTypeBlock())
                    || StringUtils.isEmpty(result.getLhMachineCode())
                    || resolveResultScheduledQty(result) <= 0) {
                continue;
            }
            machineCodes.add(result.getLhMachineCode());
        }
        return machineCodes;
    }

    /**
     * 解析新增排产同物料续作保护使用的业务日集合。
     *
     * @param context 排程上下文
     * @param sku 当前新增SKU
     * @return 需要检查的业务日集合
     */
    private List<LocalDate> resolveNewSpecDayMinimumCheckDates(LhScheduleContext context, SkuScheduleDTO sku) {
        Set<LocalDate> productionDateSet = new LinkedHashSet<LocalDate>(4);
        List<LhShiftConfigVO> shifts = context.getScheduleWindowShifts();
        if (CollectionUtils.isEmpty(shifts)) {
            shifts = LhScheduleTimeUtil.getScheduleShifts(context, context.getScheduleDate());
        }
        if (!CollectionUtils.isEmpty(shifts)) {
            for (LhShiftConfigVO shift : shifts) {
                if (Objects.isNull(shift) || Objects.isNull(shift.getWorkDate())) {
                    continue;
                }
                productionDateSet.add(shift.getWorkDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate());
            }
        }
        if (CollectionUtils.isEmpty(productionDateSet) && !CollectionUtils.isEmpty(sku.getDailyPlanQuotaMap())) {
            productionDateSet.addAll(sku.getDailyPlanQuotaMap().keySet());
        }
        return new ArrayList<LocalDate>(productionDateSet);
    }

    /**
     * 解析新增排产使用的原始 dayN 计划量。
     *
     * @param context 排程上下文
     * @param sku 当前新增SKU
     * @param productionDate 业务日
     * @return 原始dayN计划量
     */
    private int resolveOriginalNewSpecDayPlanQty(LhScheduleContext context,
                                                 SkuScheduleDTO sku,
                                                 LocalDate productionDate) {
        int dayPlanQty = MonthPlanDateResolver.resolveDayQty(
                context, sku.getMaterialCode(), sku.getProductStatus(), productionDate);
        if (dayPlanQty > 0 || CollectionUtils.isEmpty(sku.getDailyPlanQuotaMap())) {
            return dayPlanQty;
        }
        SkuDailyPlanQuotaDTO quota = sku.getDailyPlanQuotaMap().get(productionDate);
        return Objects.isNull(quota) ? 0 : Math.max(0, quota.getDayPlanQty());
    }

    /**
     * 解析新增排产同物料续作保护使用的 SKU 日标准产量。
     *
     * @param context 排程上下文
     * @param sku 当前新增SKU
     * @return SKU日标准产量
     */
    private int resolveNewSpecDailyStandardQty(LhScheduleContext context, SkuScheduleDTO sku) {
        int dailyStandardQty = ShiftCapacityResolverUtil.resolveDailyStandardQty(context, sku.getMaterialCode());
        if (dailyStandardQty <= 0) {
            dailyStandardQty = Math.max(0, sku.getDailyCapacity());
        }
        if (dailyStandardQty <= 0) {
            dailyStandardQty = Math.max(0, sku.getShiftCapacity())
                    * LhScheduleConstant.DEFAULT_SHIFTS_PER_DAY;
        }
        return dailyStandardQty;
    }

    /**
     * 记录新增排产因同物料续作满足 dayN 而跳过的过程日志。
     *
     * @param context 排程上下文
     * @param sku 当前新增SKU
     * @param dailyStandardQty SKU日标准产量
     * @param continuousMachineCount 已有纯续作机台数
     * @param continuousMachineCodes 续作机台集合
     */
    private void appendContinuousSatisfiedNewSpecSkipProcessLog(LhScheduleContext context,
                                                                SkuScheduleDTO sku,
                                                                int dailyStandardQty,
                                                                int continuousMachineCount,
                                                                Set<String> continuousMachineCodes) {
        String detail = new StringBuilder(192)
                .append("materialCode=").append(sku.getMaterialCode())
                .append(", dailyStandardQty=").append(dailyStandardQty)
                .append(", continuousMachineCount=").append(continuousMachineCount)
                .append(", continuousMachines=").append(String.join(",", continuousMachineCodes))
                .append(", reason=已有纯续作机台满足当前日优先dayN节奏")
                .toString();
        PriorityTraceLogHelper.appendProcessLog(context, "新增排产同物料续作满足dayN跳过", detail);
    }

    /**
     * 获取新增排产当前自然日的目标总机台数。
     * <p>该值只读取初始化阶段统一Map，不再按dayN、日标准产能、剩余目标量或计划模数推算。</p>
     *
     * @param context 排程上下文
     * @param sku 当前新增 SKU
     * @param productionDate 当前自然日
     * @return Map目标总机台数；结果缺失时返回0
     */
    private int resolveNewSpecDayNMachineCountCap(LhScheduleContext context,
                                                  SkuScheduleDTO sku,
                                                  LocalDate productionDate) {
        if (Objects.isNull(context) || Objects.isNull(sku) || Objects.isNull(productionDate)) {
            return 0;
        }
        LocalDate requiredMachineCountDate = this.resolveRequiredMachineCountDate(
                context, sku, productionDate);
        return this.lhDailyMouldCalcService.getRequiredMachineCount(
                context, sku.getMaterialCode(), sku.getProductStatus(), requiredMachineCountDate);
    }

    /**
     * 判断当前 SKU 已落地机台数是否已达到 dayN 理论机台数硬上限。
     * <p>已达到上限后，多机台主循环不得再打开任何新增机台，剩余目标量交由
     * 未排/下一滚动窗口承接，与“历史欠产不突破 dayN 理论机台数”口径保持一致。</p>
     *
     * @param context 排程上下文
     * @param sku 当前新增 SKU
     * @param productionDate 当前自然日
     * @return true-已达到 dayN 理论机台数上限，停止继续扩机
     */
    private boolean isNewSpecDayNMachineCountCapReached(LhScheduleContext context,
                                                        SkuScheduleDTO sku,
                                                        LocalDate productionDate) {
        LocalDate requiredMachineCountDate = this.resolveRequiredMachineCountDate(
                context, sku, productionDate);
        if (!this.lhDailyMouldCalcService.hasRequiredMachineCount(
                context, sku.getMaterialCode(), sku.getProductStatus(), requiredMachineCountDate)) {
            // 新增属于扩张型动作，Map维度缺失时安全停止，不允许用剩余目标量重新打开机台。
            this.lhDailyMouldCalcService.getRequiredMachineCount(
                    context, sku.getMaterialCode(), sku.getProductStatus(), requiredMachineCountDate);
            return true;
        }
        int dayNMachineCountCap = resolveNewSpecDayNMachineCountCap(
                context, sku, requiredMachineCountDate);
        // 与扩机台模拟的已有同物料机台口径一致：按机台编码去重，单控整机只计 1 台。
        Set<String> existingMachineCodes = new HashSet<String>(8);
        if (Objects.nonNull(context) && !CollectionUtils.isEmpty(context.getScheduleResultList())) {
            for (LhScheduleResult result : context.getScheduleResultList()) {
                if (isExistingSameMaterialActiveResult(context, result, sku, null)
                        && StringUtils.isNotEmpty(result.getLhMachineCode())) {
                    existingMachineCodes.add(LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                            result.getLhMachineCode()));
                }
            }
        }
        boolean reached = existingMachineCodes.size() >= dayNMachineCountCap;
        log.info("新增SKU统一Map目标机台数上限判断, factoryCode: {}, batchNo: {}, materialCode: {}, "
                        + "productStatus: {}, currentProductionDate: {}, requiredMachineCountDate: {}, "
                        + "targetTotalMachineCount: {}, currentEffectiveMachineCount: {}, reached: {}",
                context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(), sku.getProductStatus(),
                productionDate, requiredMachineCountDate, dayNMachineCountCap,
                existingMachineCodes.size(), reached);
        return reached;
    }

    /**
     * 判断模拟日期是否超过窗口结束日。
     *
     * @param productionDate 模拟生产日
     * @param windowEndDate 窗口结束日
     * @return true-超过窗口；false-窗口内或无法判断
     */
    private boolean isAfterSimulationWindowEnd(LocalDate productionDate, LocalDate windowEndDate) {
        return Objects.nonNull(productionDate)
                && Objects.nonNull(windowEndDate)
                && productionDate.isAfter(windowEndDate);
    }

    /**
     * 统计当前 SKU 已落地的同物料结果数。
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param currentMachineCode 当前候选机台
     * @return 已落地同物料结果数
     */
    private int countExistingSameMaterialResults(LhScheduleContext context,
                                                 SkuScheduleDTO sku,
                                                 String currentMachineCode) {
        if (Objects.isNull(context) || Objects.isNull(sku)
                || CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return 0;
        }
        Set<String> physicalMachineCodeSet = new LinkedHashSet<String>(8);
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (isExistingSameMaterialActiveResult(context, result, sku, currentMachineCode)) {
                String physicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                        result.getLhMachineCode());
                if (StringUtils.isNotEmpty(physicalMachineCode)) {
                    physicalMachineCodeSet.add(physicalMachineCode);
                }
            }
        }
        return physicalMachineCodeSet.size();
    }

    /**
     * 按当前已成功排产机台数解析下一台新增机台的生效日期。
     *
     * @param addMachineProductionDateList 新增机台生效日期列表
     * @param scheduledMachineCount 当前已成功排产机台数
     * @return 当前候选作为新增机台时的生效日期；首台或未配置时返回null
     */
    private LocalDate resolveCurrentAddMachineProductionDate(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            List<LocalDate> addMachineProductionDateList,
            int scheduledMachineCount) {
        if (Objects.isNull(sku)) {
            return null;
        }
        int existingMachineCount = countExistingSameMaterialResults(context, sku, null);
        // 续作补偿 SKU：首台补偿（已落地机台数未超过续作机台数）用续作中心链路确定的首次增机日；
        // 后续补偿台（跨轮次重新进入的第 2+ 台）不得复用首台日期，必须按 dayN 逐日推导，
        // 否则第 2 台补偿会在 T 日提前开产（如 3302001717 的 K2204 应为 T+1）。
        if (isContinuationAddMachineCandidate(sku)) {
            int continuationActiveMachineCount = Math.max(0, sku.getContinuationActiveMachineCount());
            if (existingMachineCount <= continuationActiveMachineCount) {
                return sku.getFirstAddMachineProductionDate();
            }
        } else if (existingMachineCount <= 0) {
            // 普通新增首台（已落地同物料机台数为 0）无增机日：首个生产日应尽早开产，不推迟。
            return null;
        }
        /*
         * 普通新增第 2+ 台：优先按原始 dayN 逐日推导“该机台真正被需要的业务日”，
         * 避免扩机模拟日期列表偏移导致提前一天换模开产（如 3302001581 在 8/6 才需要第 2、3 台，
         * 不应在 8/5 提前换模）。只有 dayN 推导为空（如强制欠产模式）时才回退模拟日期列表。
         */
        if (!isContinuationAddMachineCandidate(sku)) {
            LocalDate dayNDerivedDate = DailyMachineExpansionPlanner.resolveFirstOriginalDayPlanAddMachineDate(
                    this.lhDailyMouldCalcService, context, sku, Math.max(1, existingMachineCount),
                    ScheduleTypeEnum.NEW_SPEC.getCode());
            if (Objects.nonNull(dayNDerivedDate)) {
                log.info("新增SKU按dayN逐日推导增机生效日, materialCode: {}, existingMachineCount: {}, "
                                + "dayNDerivedDate: {}, 原因: 普通新增第2+台以原始dayN节奏为准",
                        sku.getMaterialCode(), existingMachineCount, dayNDerivedDate);
                return dayNDerivedDate;
            }
        }
        // 第 N 台（含补偿后续台、普通新增第 2+ 台、跨轮次）：优先使用本轮 dayN 模拟的
        // 增量生效日列表；列表为空（跨轮首台）时用公共 dayN 逐日判断按“已有同物料机台数”推导。
        if (!CollectionUtils.isEmpty(addMachineProductionDateList)
                && scheduledMachineCount > 0
                && scheduledMachineCount <= addMachineProductionDateList.size()) {
            return addMachineProductionDateList.get(scheduledMachineCount - 1);
        }
        return DailyMachineExpansionPlanner.resolveFirstOriginalDayPlanAddMachineDate(
                this.lhDailyMouldCalcService, context, sku, Math.max(1, existingMachineCount),
                ScheduleTypeEnum.NEW_SPEC.getCode());
    }

    /**
     * 构建新增排产严格扣账试算账本快照。
     *
     * @param quotaMap 原日计划账本
     * @param remainingTargetQty 本轮剩余目标量
     * @return 模拟账本
     */
    private Map<LocalDate, SkuDailyPlanQuotaDTO> buildSimulationQuotaMap(
            Map<LocalDate, SkuDailyPlanQuotaDTO> quotaMap,
            int remainingTargetQty) {
        return DailyMachineExpansionPlanner.buildTargetCappedSimulationQuotaMap(quotaMap, remainingTargetQty);
    }

    /**
     * 构建新增排产 dayN 模拟账本快照，并保留 T+3 原始日计划用于 T+2 后看判断。
     * <p>本方法只影响加机台模拟：T+3 计划量用于判断是否保留/新增机台，
     * 实际排产扣账仍沿用 T~T+2 的追补截止日，不提前消耗 T+3 月计划。</p>
     *
     * @param quotaMap 原日计划账本
     * @param remainingTargetQty 本轮剩余目标量
     * @param windowEndDate 排程窗口结束日
     * @return 模拟账本
     */
    private Map<LocalDate, SkuDailyPlanQuotaDTO> buildSimulationQuotaMap(
            SkuScheduleDTO sku,
            int remainingTargetQty,
            LocalDate windowEndDate) {
        Map<LocalDate, SkuDailyPlanQuotaDTO> quotaMap =
                Objects.isNull(sku) ? null : sku.getDailyPlanQuotaMap();
        return buildSimulationQuotaMap(sku, quotaMap, remainingTargetQty, windowEndDate);
    }

    /**
     * 构建新增排产 dayN 扩机台模拟账本快照。
     * <p>dayN 只作为节奏和资源判断依据，不允许被本轮剩余目标量截断；
     * 实际排产量仍由目标量、日计划扣账、胎胚、硫化余量和模具资源控制。</p>
     * <p>提前生产场景传入前移后的临时日计划视图；普通场景仍传入原始日计划账本。</p>
     *
     * @param sku SKU
     * @param quotaMap 模拟来源账本
     * @param remainingTargetQty 本轮剩余目标量
     * @param windowEndDate 排程窗口结束日
     * @return 模拟账本
     */
    private Map<LocalDate, SkuDailyPlanQuotaDTO> buildSimulationQuotaMap(
            SkuScheduleDTO sku,
            Map<LocalDate, SkuDailyPlanQuotaDTO> quotaMap,
            int remainingTargetQty,
            LocalDate windowEndDate) {
        Map<LocalDate, SkuDailyPlanQuotaDTO> simulationQuotaMap =
                buildDailyRhythmSimulationQuotaMap(sku, quotaMap);
        if (Objects.isNull(sku) || quotaMap == sku.getDailyPlanQuotaMap()) {
            keepNextDayPlanForWindowLastDayLookAhead(sku, quotaMap, simulationQuotaMap, windowEndDate);
        }
        return simulationQuotaMap;
    }

    /**
     * 构建不按目标量截断的 dayN 节奏模拟账本。
     *
     * @param sku SKU
     * @param quotaMap 模拟来源账本
     * @return dayN 节奏模拟账本
     */
    private Map<LocalDate, SkuDailyPlanQuotaDTO> buildDailyRhythmSimulationQuotaMap(
            SkuScheduleDTO sku,
            Map<LocalDate, SkuDailyPlanQuotaDTO> quotaMap) {
        if (CollectionUtils.isEmpty(quotaMap)) {
            return new LinkedHashMap<LocalDate, SkuDailyPlanQuotaDTO>(0);
        }
        Map<LocalDate, SkuDailyPlanQuotaDTO> simulationQuotaMap =
                new LinkedHashMap<LocalDate, SkuDailyPlanQuotaDTO>(Math.max(4, quotaMap.size() * 2));
        for (Map.Entry<LocalDate, SkuDailyPlanQuotaDTO> entry : quotaMap.entrySet()) {
            SkuDailyPlanQuotaDTO sourceQuota = entry.getValue();
            if (Objects.isNull(sourceQuota)) {
                continue;
            }
            SkuDailyPlanQuotaDTO quota = new SkuDailyPlanQuotaDTO();
            quota.setMaterialCode(StringUtils.isNotEmpty(sourceQuota.getMaterialCode())
                    ? sourceQuota.getMaterialCode() : Objects.isNull(sku) ? null : sku.getMaterialCode());
            quota.setProductionDate(Objects.nonNull(sourceQuota.getProductionDate())
                    ? sourceQuota.getProductionDate() : entry.getKey());
            quota.setDayPlanQty(Math.max(0, sourceQuota.getDayPlanQty()));
            quota.setRemainingQty(Math.max(0, sourceQuota.getRemainingQty()));
            simulationQuotaMap.put(entry.getKey(), quota);
        }
        return simulationQuotaMap;
    }

    /**
     * 解析新增排产模拟使用的日计划账本。
     * <p>SKU提前生产准入通过后，只在当前加机台模拟中使用按实际提前天数前移的临时日计划视图；
     * 不回写月计划，也不替换 SKU 原始 {@code dailyPlanQuotaMap}。</p>
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param currentProductionDate 当前候选机台生效业务日
     * @param windowEndDate 排程窗口结束日
     * @return 模拟来源账本
     */
    private Map<LocalDate, SkuDailyPlanQuotaDTO> resolveEarlyProductionSimulationQuotaMap(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate currentProductionDate,
            LocalDate windowEndDate) {
        if (Objects.isNull(context) || Objects.isNull(sku)) {
            return Objects.isNull(sku) ? null : sku.getDailyPlanQuotaMap();
        }
        // 调用处统一读取中心运行视图；非提前生产 SKU 会由上下文返回原始 dayN 账本。
        return context.resolveEffectiveDailyPlanQuotaMap(sku);
    }

    /**
     * 解析提前生产模拟剩余目标量。
     * <p>提前生产日计划前移后，加机台判断必须按前移后的 T～T+2 临时计划量计算，避免继续使用 0,46,46 误判。</p>
     *
     * @param sku SKU
     * @param simulationSourceQuotaMap 模拟来源账本
     * @param remainingTargetQty 原剩余目标量
     * @return 模拟剩余目标量
     */
    private int resolveEffectiveSimulationRemainingTargetQty(SkuScheduleDTO sku,
                                                             Map<LocalDate, SkuDailyPlanQuotaDTO> simulationSourceQuotaMap,
                                                             int remainingTargetQty) {
        int targetQty = Math.max(0, remainingTargetQty);
        if (Objects.isNull(sku) || simulationSourceQuotaMap == sku.getDailyPlanQuotaMap()) {
            return targetQty;
        }
        return Math.max(targetQty, SkuDailyPlanQuotaUtil.sumRemainingQty(simulationSourceQuotaMap));
    }

    /**
     * 保留窗口后第一天的原始日计划。
     *
     * @param sourceQuotaMap 原日计划账本
     * @param simulationQuotaMap 模拟账本
     * @param windowEndDate 排程窗口结束日
     */
    private void keepNextDayPlanForWindowLastDayLookAhead(
            SkuScheduleDTO sku,
            Map<LocalDate, SkuDailyPlanQuotaDTO> sourceQuotaMap,
            Map<LocalDate, SkuDailyPlanQuotaDTO> simulationQuotaMap,
            LocalDate windowEndDate) {
        if (Objects.isNull(sku) || CollectionUtils.isEmpty(simulationQuotaMap) || Objects.isNull(windowEndDate)) {
            return;
        }
        LocalDate nextPlanDate = windowEndDate.plusDays(1);
        int sourceDayPlanQty = resolveNextDayPlanQtyAfterWindow(sku, sourceQuotaMap, nextPlanDate);
        if (sourceDayPlanQty <= 0) {
            return;
        }
        SkuDailyPlanQuotaDTO simulationQuota = simulationQuotaMap.get(nextPlanDate);
        if (Objects.isNull(simulationQuota)) {
            simulationQuota = new SkuDailyPlanQuotaDTO();
            simulationQuota.setMaterialCode(sku.getMaterialCode());
            simulationQuota.setProductionDate(nextPlanDate);
            simulationQuotaMap.put(nextPlanDate, simulationQuota);
        } else if (sourceDayPlanQty <= Math.max(0, simulationQuota.getDayPlanQty())) {
            return;
        }
        simulationQuota.setDayPlanQty(sourceDayPlanQty);
        simulationQuota.setRemainingQty(Math.max(Math.max(0, simulationQuota.getRemainingQty()), sourceDayPlanQty));
        log.info("新增SKU dayN模拟保留T+3日计划用于窗口末日后看, materialCode: {}, productionDate: {}, dayPlanQty: {}",
                simulationQuota.getMaterialCode(), nextPlanDate, sourceDayPlanQty);
    }

    /**
     * 解析窗口后第一天日计划量。
     *
     * @param sku SKU
     * @param sourceQuotaMap 原日计划账本
     * @param nextPlanDate 窗口后第一天
     * @return T+3 日计划量
     */
    private int resolveNextDayPlanQtyAfterWindow(SkuScheduleDTO sku,
                                                 Map<LocalDate, SkuDailyPlanQuotaDTO> sourceQuotaMap,
                                                 LocalDate nextPlanDate) {
        if (!CollectionUtils.isEmpty(sourceQuotaMap)) {
            SkuDailyPlanQuotaDTO sourceQuota = sourceQuotaMap.get(nextPlanDate);
            if (Objects.nonNull(sourceQuota) && Math.max(0, sourceQuota.getDayPlanQty()) > 0) {
                return Math.max(0, sourceQuota.getDayPlanQty());
            }
        }
        return Objects.isNull(sku) ? 0 : Math.max(0, sku.getNextDayPlanQtyAfterWindow());
    }

    /**
     * 汇总新增排产模拟使用的 T~T+2 原始月计划量。
     * <p>强制欠产增机台判断需要使用月计划 dayN 汇总，不能使用已追加历史欠产后的 remainingQty，
     * 否则会把历史欠产重复计入窗口计划，导致超阈值 SKU 过度加机台。</p>
     *
     * @param quotaMap 日计划额度账本
     * @return T~T+2 原始月计划量汇总
     */
    private int sumSimulationWindowMonthPlanQty(Map<LocalDate, SkuDailyPlanQuotaDTO> quotaMap) {
        if (CollectionUtils.isEmpty(quotaMap)) {
            return 0;
        }
        int planQty = 0;
        for (SkuDailyPlanQuotaDTO quota : quotaMap.values()) {
            if (quota == null) {
                continue;
            }
            planQty += Math.max(0, quota.getDayPlanQty());
        }
        return Math.max(0, planQty);
    }

    /**
     * 汇总新增排产模拟窗口内的原始月计划量。
     *
     * @param quotaMap 日计划额度账本
     * @param windowEndDate 窗口结束日
     * @return 窗口内原始月计划量汇总
     */
    private int sumSimulationWindowMonthPlanQty(Map<LocalDate, SkuDailyPlanQuotaDTO> quotaMap,
                                                LocalDate windowEndDate) {
        if (CollectionUtils.isEmpty(quotaMap)) {
            return 0;
        }
        int planQty = 0;
        for (Map.Entry<LocalDate, SkuDailyPlanQuotaDTO> entry : quotaMap.entrySet()) {
            if (Objects.isNull(entry.getKey()) || isAfterSimulationWindowEnd(entry.getKey(), windowEndDate)
                    || Objects.isNull(entry.getValue())) {
                continue;
            }
            planQty += Math.max(0, entry.getValue().getDayPlanQty());
        }
        return Math.max(0, planQty);
    }

    /**
     * 构建候选机台日产能模拟列表。
     *
     * @param context 排程上下文
     * @param segment 当前生产段
     * @param quotaMap 模拟账本
     * @param availableMachineCount 可用候选机台数
     * @return 候选机台日产能列表
     */
    private List<Map<LocalDate, Integer>> buildSimulationMachineCapacityList(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            List<MachineScheduleDTO> candidates,
            Set<String> excludedMachineCodes,
            ProductionQuantityPolicy policy,
            MachineProductionSegment currentSegment,
            MachineScheduleDTO currentMachine,
            List<LhShiftConfigVO> shifts,
            ICapacityCalculateStrategy capacityCalculate,
            Map<LocalDate, SkuDailyPlanQuotaDTO> quotaMap,
            List<Map<LocalDate, Integer>> existingMachineCapacityMaps) {
        List<Map<LocalDate, Integer>> machineCapacityList =
                new ArrayList<Map<LocalDate, Integer>>(Math.max(1, candidates.size())
                        + (CollectionUtils.isEmpty(existingMachineCapacityMaps) ? 0 : existingMachineCapacityMaps.size()));
        if (!CollectionUtils.isEmpty(existingMachineCapacityMaps)) {
            machineCapacityList.addAll(existingMachineCapacityMaps);
        }
        for (MachineScheduleDTO candidate : candidates) {
            if (candidate == null || StringUtils.isEmpty(candidate.getMachineCode())) {
                continue;
            }
            if (!CollectionUtils.isEmpty(excludedMachineCodes)
                    && excludedMachineCodes.contains(candidate.getMachineCode())) {
                continue;
            }
            if (StringUtils.equals(candidate.getMachineCode(), currentMachine.getMachineCode())) {
                machineCapacityList.add(buildSimulationCurrentMachineCapacityMap(context, currentSegment, quotaMap));
                continue;
            }
            machineCapacityList.add(buildSimulationCandidateCapacityMap(
                    context, sku, candidate, policy, shifts, capacityCalculate, quotaMap));
        }
        return machineCapacityList;
    }

    /**
     * 收集当前 SKU 在进入 S4.5 前已经落地的同 SKU 机台日产能图。
     * <p>换活字块与本轮前面已排出的新增结果都视为已启用机台，需要参与 dayN 扩机判断。</p>
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param currentMachine 当前候选机台
     * @param shifts 排程窗口班次
     * @param quotaMap 模拟账本
     * @return 已启用机台日产能图列表
     */
    private List<Map<LocalDate, Integer>> buildExistingSameMaterialCapacityMaps(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            MachineScheduleDTO currentMachine,
            List<LhShiftConfigVO> shifts,
            Map<LocalDate, SkuDailyPlanQuotaDTO> quotaMap) {
        List<Map<LocalDate, Integer>> existingMachineCapacityMaps = new ArrayList<Map<LocalDate, Integer>>(4);
        if (context == null || sku == null || CollectionUtils.isEmpty(context.getScheduleResultList())
                || CollectionUtils.isEmpty(shifts) || CollectionUtils.isEmpty(quotaMap)) {
            return existingMachineCapacityMaps;
        }
        Set<String> addedMachineCodes = new HashSet<String>(4);
        String currentMachineCode = currentMachine == null ? null : currentMachine.getMachineCode();
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (!isExistingSameMaterialActiveResult(context, result, sku, currentMachineCode)) {
                continue;
            }
            if (!addedMachineCodes.add(result.getLhMachineCode())) {
                continue;
            }
            Map<LocalDate, Integer> capacityMap = buildExistingResultDailyCapacityMap(result, shifts, quotaMap);
            if (!hasPositiveDailyCapacity(capacityMap)) {
                continue;
            }
            existingMachineCapacityMaps.add(capacityMap);
        }
        return existingMachineCapacityMaps;
    }

    /**
     * 判断结果是否属于当前 SKU 已启用的同 SKU 机台。
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param sku 当前 SKU
     * @param currentMachineCode 当前候选机台编码
     * @return true-属于已启用机台
     */
    private boolean isExistingSameMaterialActiveResult(LhScheduleContext context,
                                                       LhScheduleResult result,
                                                       SkuScheduleDTO sku,
                                                       String currentMachineCode) {
        if (result == null
                || sku == null
                || StringUtils.isEmpty(sku.getMaterialCode())
                || !StringUtils.equals(sku.getMaterialCode(), result.getMaterialCode())
                || !StringUtils.equals(StringUtils.trimToEmpty(sku.getProductStatus()),
                StringUtils.trimToEmpty(result.getProductStatus()))
                || StringUtils.equals(currentMachineCode, result.getLhMachineCode())
                || StringUtils.isEmpty(result.getLhMachineCode())
                || resolveResultScheduledQty(result) <= 0) {
            return false;
        }
        if (StringUtils.equals(NEW_SPEC_SCHEDULE_TYPE, result.getScheduleType())
                || StringUtils.equals(ScheduleTypeEnum.TYPE_BLOCK.getCode(), result.getScheduleType())) {
            return true;
        }
        if (!StringUtils.equals(ScheduleTypeEnum.CONTINUOUS.getCode(), result.getScheduleType())) {
            return false;
        }
        SkuScheduleDTO sourceSku = resolveResultSourceSku(context, result);
        /*
         * 续作补偿转入 S4.5 后，当前补偿 SKU 与来源续作 SKU 共用同一份日计划账本。
         * 只有同账本续作结果才能作为“当前已选机台窗口有效产能”参与扩机判断，
         * 避免同物料但不同月计划/不同补偿来源的续作结果串入产能。
         */
        return sourceSku != null && sourceSku.getDailyPlanQuotaMap() == sku.getDailyPlanQuotaMap();
    }

    /**
     * 将既有结果按业务日折算为 dayN 模拟产能图。
     *
     * @param result 既有排程结果
     * @param shifts 排程窗口班次
     * @param quotaMap 模拟账本
     * @return 该结果的业务日产能图
     */
    private Map<LocalDate, Integer> buildExistingResultDailyCapacityMap(
            LhScheduleResult result,
            List<LhShiftConfigVO> shifts,
            Map<LocalDate, SkuDailyPlanQuotaDTO> quotaMap) {
        Map<LocalDate, Integer> capacityMap =
                new LinkedHashMap<LocalDate, Integer>(Math.max(4, quotaMap.size() * 2));
        for (LocalDate productionDate : quotaMap.keySet()) {
            capacityMap.put(productionDate, 0);
        }
        for (LhShiftConfigVO shift : shifts) {
            if (shift == null || shift.getWorkDate() == null || shift.getShiftIndex() == null) {
                continue;
            }
            LocalDate productionDate = shift.getWorkDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (!capacityMap.containsKey(productionDate)) {
                continue;
            }
            Integer shiftPlanQty = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
            if (shiftPlanQty == null || shiftPlanQty <= 0) {
                continue;
            }
            capacityMap.merge(productionDate, shiftPlanQty, Integer::sum);
        }
        return capacityMap;
    }

    /**
     * 判断日产能图是否存在有效产能。
     *
     * @param capacityMap 日产能图
     * @return true-存在有效产能
     */
    private boolean hasPositiveDailyCapacity(Map<LocalDate, Integer> capacityMap) {
        if (CollectionUtils.isEmpty(capacityMap)) {
            return false;
        }
        for (Integer capacityQty : capacityMap.values()) {
            if (capacityQty != null && capacityQty > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 构建当前候选机台的 dayN 模拟产能图。
     *
     * @param context 排程上下文
     * @param currentSegment 当前机台生产段
     * @param quotaMap 模拟账本
     * @return 当前机台产能图
     */
    private Map<LocalDate, Integer> buildSimulationCurrentMachineCapacityMap(
            LhScheduleContext context,
            MachineProductionSegment currentSegment,
            Map<LocalDate, SkuDailyPlanQuotaDTO> quotaMap) {
        Map<LocalDate, Integer> currentMachineCapacityMap =
                new LinkedHashMap<LocalDate, Integer>(Math.max(4, quotaMap.size() * 2));
        for (LocalDate productionDate : quotaMap.keySet()) {
            currentMachineCapacityMap.put(productionDate,
                    sumSegmentCapacityByWorkDate(context, currentSegment, productionDate));
        }
        return currentMachineCapacityMap;
    }

    /**
     * 构建其他候选机台的 dayN 模拟产能图。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param candidate 候选机台
     * @param policy 排产数量策略
     * @param shifts 排程窗口班次
     * @param capacityCalculate 机台起排策略
     * @param quotaMap 模拟账本
     * @return 候选机台产能图
     */
    private Map<LocalDate, Integer> buildSimulationCandidateCapacityMap(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            MachineScheduleDTO candidate,
            ProductionQuantityPolicy policy,
            List<LhShiftConfigVO> shifts,
            ICapacityCalculateStrategy capacityCalculate,
            Map<LocalDate, SkuDailyPlanQuotaDTO> quotaMap) {
        Map<LocalDate, Integer> capacityMap =
                new LinkedHashMap<LocalDate, Integer>(Math.max(4, quotaMap.size() * 2));
        if (context == null || sku == null || candidate == null || CollectionUtils.isEmpty(shifts)
                || capacityCalculate == null) {
            return capacityMap;
        }
        Date endingTime = candidate.getEstimatedEndTime() != null
                ? candidate.getEstimatedEndTime() : resolveDefaultMachineEndTime(context, shifts);
        Date machineReadyTime = capacityCalculate.calculateStartTime(context, candidate.getMachineCode(), endingTime);
        int switchDurationHours = LhScheduleTimeUtil.getMouldChangeTotalHours(context);
        // 精度计划与换模禁止并行，模拟和正式落地统一使用已避开精度窗口的机台就绪时间。
        Date switchReadyTime = machineReadyTime;
        switchReadyTime = resolveSpecifyReservedReadyTime(context, sku, candidate.getMachineCode(), switchReadyTime);
        // 试制SKU换模需在早班完成，不受开产模式限制
        switchReadyTime = ShiftProductionControlUtil.resolveEarliestSwitchStartTime(
                context, switchReadyTime, sku);
        switchReadyTime = alignNewSpecSwitchReadyTimeToWindowStart(context, shifts, switchReadyTime);
        Date mouldChangeStartTime = switchReadyTime;
        Date mouldChangeCompleteTime = LhScheduleTimeUtil.addHours(mouldChangeStartTime, switchDurationHours);
        Date maintenanceReadyTime = mouldChangeCompleteTime;
        boolean plannedRepairAffectingSwitch = ShiftCapacityResolverUtil.isPlannedRepairAffectingSwitch(
                context, context.getDevicePlanShutList(), candidate.getMachineCode(), endingTime,
                mouldChangeStartTime, mouldChangeCompleteTime);
        Date plannedRepairReadyTime = ShiftCapacityResolverUtil.resolvePlannedRepairProductionReadyTime(
                context, context.getDevicePlanShutList(), candidate.getMachineCode(), endingTime,
                mouldChangeStartTime, mouldChangeCompleteTime);
        Date firstInspectionBaseTime = maintenanceReadyTime;
        if (plannedRepairAffectingSwitch && Objects.nonNull(plannedRepairReadyTime)
                && plannedRepairReadyTime.after(firstInspectionBaseTime)) {
            firstInspectionBaseTime = plannedRepairReadyTime;
        }
        // 候选模拟必须与最终落地一致：并行任务取最晚恢复时间，正常换模不再追加首检小时。
        Date productionStartTime = firstInspectionBaseTime;
        productionStartTime = FirstInspectionQtyUtil.resolveTrialProductionStartTime(
                context, sku, shifts, firstInspectionBaseTime, productionStartTime,
                ScheduleTypeEnum.NEW_SPEC.getCode());
        if (productionStartTime == null) {
            return capacityMap;
        }
        int machineMouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(candidate);
        int runtimeShiftCapacity = ShiftCapacityResolverUtil.resolveRuntimeShiftCapacity(
                context, candidate, sku.getShiftCapacity());
        /*
         * dayN 增机模拟也必须使用换模主链的同一首检时间分摊算法。旧逻辑把全部首检量
         * 压入换模结束班次，会在首检跨业务日时把日产能记到错误日期，进而误判增机数量。
         * 本方法只做无副作用预演；首检计数仍由最终命中的正式结果统一提交。
         */
        FirstInspectionAllocationPlan firstInspectionAllocationPlan =
                this.resolveFirstInspectionAllocationPlan(
                        context, sku, candidate, shifts,
                        mouldChangeStartTime, mouldChangeCompleteTime,
                        runtimeShiftCapacity, machineMouldQty,
                        sku.resolveTargetScheduleQty(), ScheduleTypeEnum.NEW_SPEC.getCode());
        if (!firstInspectionAllocationPlan.isValid()) {
            return capacityMap;
        }
        Date firstProductionStartTime = ShiftProductionControlUtil.resolveFirstSchedulableStartIgnoringCleaning(
                context,
                candidate.getMachineCode(),
                productionStartTime,
                shifts,
                runtimeShiftCapacity,
                sku.getLhTimeSeconds(),
                machineMouldQty);
        if (firstProductionStartTime == null) {
            return capacityMap;
        }
        Map<Integer, Integer> shiftCapacityMap = this.calculateShiftCapacityMap(
                context, candidate, sku, firstProductionStartTime,
                mouldChangeStartTime, mouldChangeCompleteTime,
                shifts, machineMouldQty, runtimeShiftCapacity,
                policy != null && policy.isEnding(), false);
        if (firstInspectionAllocationPlan.getInspectionQty() > 0) {
            shiftCapacityMap = FirstInspectionQtyUtil.applyFirstInspectionAllocationToCapacityMap(
                    shifts, shiftCapacityMap, firstInspectionAllocationPlan);
        } else {
            /*
             * 试制中班继续沿用固定2小时/75%产能例外，其公共计划首检量为0；
             * 这里只复用计数班次应用时间型扣减，不生成任何首检条数。
             */
            shiftCapacityMap = FirstInspectionQtyUtil.applyFirstInspectionQtyToCapacityMap(
                    context, sku, shifts, firstInspectionAllocationPlan.getCountingShift(),
                    shiftCapacityMap, runtimeShiftCapacity, sku.resolveTargetScheduleQty(),
                    ScheduleTypeEnum.NEW_SPEC.getCode(), candidate.getMachineCode());
        }
        // 候选模拟必须与真实新增排产复用同一结构门控，否则会因虚高日标准量产能误选机台。
        shiftCapacityMap = applyDailyStandardCapacityAdjust(
                context, sku, candidate.getMachineCode(), shifts, shiftCapacityMap, runtimeShiftCapacity);
        MachineProductionSegment simulationSegment = buildMachineProductionSegment(
                context, sku, candidate.getMachineCode(), mouldChangeStartTime,
                firstProductionStartTime, sumShiftCapacity(shiftCapacityMap),
                runtimeShiftCapacity, shiftCapacityMap);
        for (LocalDate productionDate : quotaMap.keySet()) {
            capacityMap.put(productionDate, sumSegmentCapacityByWorkDate(
                    context, simulationSegment, productionDate));
        }
        return capacityMap;
    }

    /**
     * 获取新增排产欠产追补判断天数。
     * <p>该值表示当前天发生欠产后，额外向后看几天，不包含当前天。</p>
     *
     * @param context 排程上下文
     * @return 向后观察天数（不含当天）
     */
    private int resolveNewSpecShortageLookAheadDays(LhScheduleContext context) {
        LhScheduleConfig scheduleConfig = context == null ? null : context.getScheduleConfig();
        if (scheduleConfig == null) {
            return LhScheduleConstant.NEW_SPEC_SHORTAGE_LOOK_AHEAD_DAYS;
        }
        return scheduleConfig.getNewSpecShortageLookAheadDays();
    }

    /**
     * 获取新增排产欠产增机台阈值。
     *
     * @param context 排程上下文
     * @return 欠产阈值
     */
    private int resolveNewSpecShortageAddMachineThreshold(LhScheduleContext context) {
        return DailyMachineExpansionPlanner.resolveShortageAddMachineThreshold(context);
    }

    /**
     * 汇总生产段在指定业务日的可排产能。
     *
     * @param context 排程上下文
     * @param segment 当前机台生产段
     * @param productionDate 业务日
     * @return 该业务日产能
     */
    private int sumSegmentCapacityByWorkDate(LhScheduleContext context,
                                             MachineProductionSegment segment,
                                             LocalDate productionDate) {
        if (context == null || segment == null || productionDate == null
                || CollectionUtils.isEmpty(segment.getShiftCapacityMap())) {
            return 0;
        }
        int totalQty = 0;
        for (Map.Entry<Integer, Integer> entry : segment.getShiftCapacityMap().entrySet()) {
            LhShiftConfigVO shift = LhScheduleTimeUtil.getShiftByIndex(
                    context, context.getScheduleDate(), entry.getKey());
            if (shift == null || shift.getWorkDate() == null) {
                continue;
            }
            LocalDate shiftWorkDate = shift.getWorkDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            if (productionDate.equals(shiftWorkDate)) {
                totalQty += entry.getValue() == null ? 0 : Math.max(0, entry.getValue());
            }
        }
        return Math.max(0, totalQty);
    }

    /**
     * 判断当前SKU的 dayN 账本是否跨多个业务日仍存在有效目标量。
     *
     * @param sku SKU
     * @return true-存在多个业务日计划量；false-仅单日目标
     */
    private boolean hasMultiplePositiveQuotaDays(
            Map<LocalDate, SkuDailyPlanQuotaDTO> quotaMap) {
        if (CollectionUtils.isEmpty(quotaMap)) {
            return false;
        }
        int positiveDays = 0;
        for (SkuDailyPlanQuotaDTO quota : quotaMap.values()) {
            if (quota == null) {
                continue;
            }
            int effectiveQty = Math.max(0, quota.getRemainingQty());
            if (effectiveQty <= 0) {
                effectiveQty = Math.max(0, quota.getDayPlanQty());
            }
            if (effectiveQty <= 0) {
                continue;
            }
            positiveDays++;
            if (positiveDays > 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析当前候选机台对应的窗口目标量。
     * <p>满排模式下，当只剩当前一台候选机台时，需要按该机台真实窗口产能收敛目标量，
     * 避免把理论窗口产能直接带入单机结果构造。</p>
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param candidateMachine 当前候选机台
     * @param mouldChangeStartTime 换模开始时间
     * @param firstProductionStartTime 首次可开产时间
     * @param shifts 排程窗口班次
     * @param dynamicTargetQty 当前窗口目标量
     * @param totalScheduledQty 当前SKU已累计排产量
     * @param excludedMachineCodes 已排除机台
     * @param quantityPolicy 数量策略
     * @return 当前候选机台生效的窗口目标量
     */
    private int resolveCandidateTargetQty(LhScheduleContext context,
                                          SkuScheduleDTO sku,
                                          MachineScheduleDTO candidateMachine,
                                          Date mouldChangeStartTime,
                                          Date firstProductionStartTime,
                                          List<LhShiftConfigVO> shifts,
                                          List<MachineScheduleDTO> candidates,
                                          int dynamicTargetQty,
                                          int totalScheduledQty,
                                          Set<String> excludedMachineCodes,
                                          ProductionQuantityPolicy quantityPolicy) {
        if (context == null || sku == null || candidateMachine == null || quantityPolicy == null) {
            return Math.max(0, dynamicTargetQty);
        }
        if (quantityPolicy.isStrictUpperLimit()) {
            return Math.max(0, dynamicTargetQty);
        }
        int availableMachineCount = countAvailableCandidateMachines(candidates, excludedMachineCodes);
        if (totalScheduledQty > 0 || availableMachineCount > 1) {
            return Math.max(0, dynamicTargetQty);
        }
        Integer originalTargetScheduleQty = sku.getTargetScheduleQty();
        sku.setTargetScheduleQty(dynamicTargetQty);
        int refinedTargetQty = getTargetScheduleQtyResolver().refineTargetQtyByMachineCapacity(
                context, sku, candidateMachine, mouldChangeStartTime, firstProductionStartTime,
                shifts, ScheduleTypeEnum.NEW_SPEC.getCode());
        sku.setTargetScheduleQty(originalTargetScheduleQty);
        return Math.max(0, refinedTargetQty);
    }

    /**
     * 统计尚可尝试的候选机台数量。
     *
     * @param candidates 候选机台
     * @param excludedMachineCodes 已排除机台编码
     * @return 可用候选数
     */
    private int countAvailableCandidateMachines(List<MachineScheduleDTO> candidates,
                                                Set<String> excludedMachineCodes) {
        if (CollectionUtils.isEmpty(candidates)) {
            return 0;
        }
        int count = 0;
        for (MachineScheduleDTO candidate : candidates) {
            if (candidate == null || StringUtils.isEmpty(candidate.getMachineCode())) {
                continue;
            }
            if (!CollectionUtils.isEmpty(excludedMachineCodes)
                    && excludedMachineCodes.contains(candidate.getMachineCode())) {
                continue;
            }
            count++;
        }
        return count;
    }

    /**
     * 向上整除。
     *
     * @param dividend 被除数
     * @param divisor 除数
     * @return 向上取整后的商
     */
    private int divideCeiling(int dividend, int divisor) {
        if (dividend <= 0) {
            return 0;
        }
        if (divisor <= 0) {
            return dividend;
        }
        return (dividend + divisor - 1) / divisor;
    }

    /**
     * 将剩余量向上取整到单班产能，表示最后已开班班次补满。
     *
     * @param qty 剩余目标量
     * @param shiftCapacity 单班产能
     * @return 补满后的计划量
     */
    private int roundUpToShiftCapacity(int qty, int shiftCapacity) {
        if (qty <= 0 || shiftCapacity <= 0) {
            return Math.max(0, qty);
        }
        return ((qty + shiftCapacity - 1) / shiftCapacity) * shiftCapacity;
    }

    /**
     * 获取候选机台窗口可用产能。
     * <p>同一SKU的一次选机中，“可单机收完”和“尾量集中”会重复读取同一机台窗口产能，
     * 这里使用当前SKU内短生命周期缓存，成功落地结果后即清空，避免机台运行态变化后复用旧产能。</p>
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param candidate 候选机台
     * @param candidateCache 当前SKU候选机台缓存上下文
     * @return 窗口可用产能
     */
    private int resolveCachedMachineAvailableCapacityInWindow(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            MachineScheduleDTO candidate,
            NewSpecCandidateCache candidateCache) {
        if (candidate == null || StringUtils.isEmpty(candidate.getMachineCode())) {
            return 0;
        }
        if (Objects.nonNull(candidateCache)) {
            Integer cachedCapacity = candidateCache.getCandidateWindowCapacity(
                    candidate.getMachineCode());
            if (Objects.nonNull(cachedCapacity)) {
                return cachedCapacity;
            }
        }
        /*
         * 该摘要属于候选机台诊断，必须与候选预演保持同一口径：只保留试制/量试类型门禁，
         * 不叠加胎胚最早可供时间。正式落班的产能裁剪仍在主排产链使用正式生产门禁。
         * 缓存只保存最终整数容量，命中后不重复扫描日计划和班次，也不保留“机台 × 班次”
         * 明细，控制 CPU 与堆占用。
         */
        Date candidateProductionNotBeforeTime = NewSpecEmbryoAvailableTimeResolver
                .resolveSkuProductionGateTime(
                        context, sku, context.getScheduleWindowShifts());
        int machineCapacity = this.getTargetScheduleQtyResolver()
                .calcMachineAvailableCapacityInWindow(
                        context, sku, candidate, candidateProductionNotBeforeTime);
        if (Objects.nonNull(candidateCache)) {
            candidateCache.putCandidateWindowCapacity(
                    candidate.getMachineCode(), machineCapacity);
        }
        return machineCapacity;
    }

    private MachineScheduleDTO resolvePreferredTrialMachine(LhScheduleContext context,
                                                            SkuScheduleDTO sku,
                                                            List<MachineScheduleDTO> candidates) {
        if (sku == null || CollectionUtils.isEmpty(candidates)) {
            return null;
        }
        if (!shouldPreferTrialMachine(sku)) {
            return null;
        }
        String preferredMachineCode = getTrialProductionStrategy().matchTrialMachine(context, sku);
        if (StringUtils.isEmpty(preferredMachineCode)) {
            return null;
        }
        for (MachineScheduleDTO candidate : candidates) {
            if (candidate != null && StringUtils.equals(preferredMachineCode, candidate.getMachineCode())) {
                return candidate;
            }
        }
        return null;
    }

    private boolean shouldPreferTrialMachine(SkuScheduleDTO sku) {
        if (sku == null) {
            return false;
        }
        if (sku.isSmallBatchValidation()) {
            return true;
        }
        return StringUtils.equals(ConstructionStageEnum.TRIAL.getCode(), sku.getConstructionStage())
                || StringUtils.equals(ConstructionStageEnum.MASS_TRIAL.getCode(), sku.getConstructionStage());
    }

    /**
     * 判断 SKU 是否属于试制/量试。
     *
     * @param sku SKU
     * @return true-试制或量试
     */
    private boolean isTrialOrMassTrialSku(SkuScheduleDTO sku) {
        if (sku == null) {
            return false;
        }
        return StringUtils.equals(ConstructionStageEnum.TRIAL.getCode(), sku.getConstructionStage())
                || StringUtils.equals(ConstructionStageEnum.MASS_TRIAL.getCode(), sku.getConstructionStage());
    }

    /**
     * 判断是否为量试或小批量SKU。
     * <p>isTrial 仅作为试制/量试总标识兼容，不按试制强约束处理。</p>
     *
     * @param sku SKU
     * @return true-量试或小批量
     */
    private boolean isMassTrialOrSmallBatchSku(SkuScheduleDTO sku) {
        return isMassTrialSku(sku) || isSmallBatchSku(sku);
    }

    private boolean shouldDeferSingleControlCompetition(LhScheduleContext context,
                                                        SkuScheduleDTO currentSku,
                                                        List<MachineScheduleDTO> candidates,
                                                        IMachineMatchStrategy machineMatch) {
        if (context == null || currentSku == null || CollectionUtils.isEmpty(candidates)
                || !isMassTrialOrSmallBatchSku(currentSku)
                || !hasAvailableSingleControlCandidate(context, candidates)) {
            return false;
        }
        if (!isStructureAllEndingPriority(context, currentSku)) {
            return false;
        }
        Set<String> currentSingleControlMachineCodes = collectSingleControlMachineCodes(context, candidates);
        if (CollectionUtils.isEmpty(currentSingleControlMachineCodes)) {
            return false;
        }
        for (SkuScheduleDTO pendingSku : context.getNewSpecSkuList()) {
            if (pendingSku == currentSku || !isHigherSingleControlPriority(pendingSku, currentSku)) {
                continue;
            }
            if (shouldSkipTrialSku(context, pendingSku)) {
                continue;
            }
            if (isSameStructureEndingLayer(context, currentSku, pendingSku)
                    && hasSharedSingleControlCandidates(context, pendingSku, currentSingleControlMachineCodes, machineMatch)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAvailableSingleControlCandidate(LhScheduleContext context, List<MachineScheduleDTO> candidates) {
        if (context == null || CollectionUtils.isEmpty(candidates)) {
            return false;
        }
        for (MachineScheduleDTO candidate : candidates) {
            if (candidate != null && isSingleControlMachine(context, candidate.getMachineCode())) {
                return true;
            }
        }
        return false;
    }

    private Set<String> collectSingleControlMachineCodes(LhScheduleContext context, List<MachineScheduleDTO> candidates) {
        Set<String> singleControlMachineCodes = new HashSet<String>(
                CollectionUtils.isEmpty(candidates) ? 0 : candidates.size());
        if (context == null || CollectionUtils.isEmpty(candidates)) {
            return singleControlMachineCodes;
        }
        for (MachineScheduleDTO candidate : candidates) {
            if (candidate == null || StringUtils.isEmpty(candidate.getMachineCode())) {
                continue;
            }
            if (isSingleControlMachine(context, candidate.getMachineCode())) {
                singleControlMachineCodes.add(candidate.getMachineCode());
            }
        }
        return singleControlMachineCodes;
    }

    private boolean hasSharedSingleControlCandidates(LhScheduleContext context,
                                                     SkuScheduleDTO sku,
                                                     Set<String> currentSingleControlMachineCodes,
                                                     IMachineMatchStrategy machineMatch) {
        if (context == null || sku == null || CollectionUtils.isEmpty(currentSingleControlMachineCodes)
                || machineMatch == null) {
            return false;
        }
        Boolean previousBlockedState = context.getNewSpecTypeRuleBlockedMap().get(sku);
        List<MachineScheduleDTO> higherPriorityCandidates = machineMatch.matchMachines(context, sku);
        if (previousBlockedState == null) {
            context.getNewSpecTypeRuleBlockedMap().remove(sku);
        } else {
            context.getNewSpecTypeRuleBlockedMap().put(sku, previousBlockedState);
        }
        if (CollectionUtils.isEmpty(higherPriorityCandidates)) {
            return false;
        }
        for (MachineScheduleDTO candidate : higherPriorityCandidates) {
            if (candidate == null || StringUtils.isEmpty(candidate.getMachineCode())) {
                continue;
            }
            if (currentSingleControlMachineCodes.contains(candidate.getMachineCode())
                    && isSingleControlMachine(context, candidate.getMachineCode())) {
                return true;
            }
        }
        return false;
    }

    private boolean isHigherSingleControlPriority(SkuScheduleDTO pendingSku, SkuScheduleDTO currentSku) {
        return resolveSingleControlCompetitionPriority(pendingSku)
                < resolveSingleControlCompetitionPriority(currentSku);
    }

    private int resolveSingleControlCompetitionPriority(SkuScheduleDTO sku) {
        if (isTrialConstructionStage(sku)) {
            return 0;
        }
        if (isMassTrialSku(sku)) {
            return 1;
        }
        if (isSmallBatchSku(sku)) {
            return 2;
        }
        return Integer.MAX_VALUE;
    }

    private boolean isSameStructureEndingLayer(LhScheduleContext context,
                                               SkuScheduleDTO currentSku,
                                               SkuScheduleDTO pendingSku) {
        if (context == null || currentSku == null || pendingSku == null) {
            return false;
        }
        return hitSingleControlStructureEndingLayer(context, currentSku)
                && hitSingleControlStructureEndingLayer(context, pendingSku);
    }

    /**
     * 判断SKU是否命中单控竞争使用的结构五天内收尾层级。
     * <p>对仍在待排列表中的SKU，沿用现有“同结构SKU全部收尾”的判定；</p>
     * <p>对已排出待排列表的高优先级SKU，退化为校验该SKU自身是否命中结构收尾窗口，保证量试可复用试制刚释放的单控产能。</p>
     *
     * @param context 排程上下文
     * @param targetSku 目标SKU
     * @return true-命中单控竞争结构收尾层级
     */
    private boolean hitSingleControlStructureEndingLayer(LhScheduleContext context, SkuScheduleDTO targetSku) {
        if (context == null || targetSku == null) {
            return false;
        }
        Boolean snapshotResult = context.getNewSpecSingleControlStructureEndingLayerMap().get(targetSku);
        if (snapshotResult != null) {
            return snapshotResult;
        }
        return isStructureAllEndingPriority(context, targetSku);
    }

    private void initializeSingleControlStructureEndingLayerSnapshot(LhScheduleContext context) {
        if (context == null) {
            return;
        }
        Map<SkuScheduleDTO, Boolean> snapshotMap = context.getNewSpecSingleControlStructureEndingLayerMap();
        snapshotMap.clear();
        if (CollectionUtils.isEmpty(context.getNewSpecSkuList())) {
            return;
        }
        for (SkuScheduleDTO sku : context.getNewSpecSkuList()) {
            if (sku == null) {
                continue;
            }
            snapshotMap.put(sku, isStructureAllEndingPriority(context, sku));
        }
    }

    private boolean shouldPrioritizeDeferredSingleControlSku(LhScheduleContext context,
                                                             SkuScheduleDTO currentSku,
                                                             List<SkuScheduleDTO> deferredSkuList) {
        if (context == null || currentSku == null || CollectionUtils.isEmpty(deferredSkuList)) {
            return false;
        }
        boolean hasLowerPriorityDeferredSku = false;
        for (SkuScheduleDTO deferredSku : deferredSkuList) {
            if (deferredSku == null || !isMassTrialOrSmallBatchSku(deferredSku)) {
                continue;
            }
            hasLowerPriorityDeferredSku = true;
            if (isSameStructureEndingLayer(context, deferredSku, currentSku)
                    && isHigherSingleControlPriority(currentSku, deferredSku)) {
                return false;
            }
        }
        if (!hasLowerPriorityDeferredSku) {
            return false;
        }
        for (SkuScheduleDTO deferredSku : deferredSkuList) {
            if (deferredSku != null
                    && isMassTrialOrSmallBatchSku(deferredSku)
                    && !isSameStructureEndingLayer(context, deferredSku, currentSku)) {
                return true;
            }
        }
        return false;
    }

    private boolean isStructureAllEndingPriority(LhScheduleContext context, SkuScheduleDTO targetSku) {
        if (context == null || targetSku == null || StringUtils.isEmpty(targetSku.getStructureName())) {
            return false;
        }
        int structureEndingDays = context.getScheduleConfig() != null
                ? context.getScheduleConfig().getStructureEndingDays()
                : LhScheduleConstant.DEFAULT_STRUCTURE_ENDING_DAYS;
        int totalSkuCount = 0;
        int endingSkuCount = 0;
        int latestEndingDays = -1;
        for (SkuScheduleDTO pendingSku : context.getNewSpecSkuList()) {
            if (pendingSku == null || !StringUtils.equals(targetSku.getStructureName(), pendingSku.getStructureName())) {
                continue;
            }
            totalSkuCount++;
            if (!endingJudgmentStrategy.isStructureEndingForPriority(context, pendingSku)) {
                continue;
            }
            endingSkuCount++;
            int actualEndingDays = endingJudgmentStrategy.calculateEndingDaysForStructurePriority(context, pendingSku);
            if (actualEndingDays >= 0) {
                latestEndingDays = Math.max(latestEndingDays, actualEndingDays);
            }
        }
        return totalSkuCount > 0
                && endingSkuCount == totalSkuCount
                && latestEndingDays >= 0
                && latestEndingDays <= structureEndingDays;
    }

    /**
     * 判断是否为量试SKU。
     * <p>isTrial 仅作为试制/量试总标识兼容，不按试制强约束处理。</p>
     *
     * @param sku SKU
     * @return true-量试
     */
    private boolean isMassTrialSku(SkuScheduleDTO sku) {
        if (sku == null) {
            return false;
        }
        return StringUtils.equals(ConstructionStageEnum.MASS_TRIAL.getCode(), sku.getConstructionStage());
    }

    /**
     * 判断是否为小批量SKU。
     *
     * @param sku SKU
     * @return true-小批量
     */
    private boolean isSmallBatchSku(SkuScheduleDTO sku) {
        return sku != null && sku.isSmallBatchValidation();
    }

    private String resolveConstructionStageDesc(SkuScheduleDTO sku) {
        if (isTrialConstructionStage(sku)) {
            return "试制";
        }
        if (isMassTrialSku(sku)) {
            return "量试";
        }
        if (isSmallBatchSku(sku)) {
            return "小批量";
        }
        return "正式";
    }

    private String resolveNewSpecDisplayType(SkuScheduleDTO sku) {
        if (isTrialConstructionStage(sku)) {
            return "试制组";
        }
        if (isMassTrialSku(sku)) {
            return "量试组";
        }
        return "正规组";
    }

    /**
     * 判断是否为试制、量试或小批量SKU。
     *
     * @param sku SKU
     * @return true-试制、量试或小批量
     */
    private boolean isTrialOrMassTrialOrSmallBatchSku(SkuScheduleDTO sku) {
        return isTrialConstructionStage(sku) || isMassTrialOrSmallBatchSku(sku);
    }

    /**
     * 判断是否为正规SKU。
     *
     * @param sku SKU
     * @return true-正规SKU
     */
    private boolean isFormalSku(SkuScheduleDTO sku) {
        return sku != null && !isTrialOrMassTrialOrSmallBatchSku(sku);
    }

    /**
     * 判断是否启用局部搜索。
     *
     * @param context 排程上下文
     * @param candidates 候选机台列表
     * @return true-启用，false-不启用
     */
    private boolean shouldUseLocalSearch(LhScheduleContext context, List<MachineScheduleDTO> candidates) {
        if (CollectionUtils.isEmpty(candidates)) {
            return false;
        }
        LhScheduleConfig scheduleConfig = context.getScheduleConfig();
        if (scheduleConfig == null || !scheduleConfig.isLocalSearchEnabled()) {
            return false;
        }
        return candidates.size() < scheduleConfig.getLocalSearchMachineThreshold();
    }


    /**
     * 输出新增排产机台决策日志（含SKU基本信息和最终选中原因）。
     *
     * @param context 排程上下文
     * @param sku 待排SKU
     * @param candidates 候选机台列表
     * @param localSearchSuggestedMachine 局部搜索评估机台
     * @param finalMachine 最终选中机台
     * @param excludedMachineCodes 已排除机台编码
     * @param excludedMachineReasonMap 已排除机台原因明细
     * @param failReason 失败原因
     * @param success 是否成功
     * @param startTimeText 开产时间文本或附加说明
     * @param confirmedSnapshot 选机时点冻结的日志快照；为空时回落到机台实时收尾时间
     */
    private void traceNewSpecMachineDecision(LhScheduleContext context, SkuScheduleDTO sku,
                                             List<MachineScheduleDTO> candidates,
                                             MachineScheduleDTO localSearchSuggestedMachine,
                                             MachineScheduleDTO finalMachine,
                                             Set<String> excludedMachineCodes,
                                             Map<String, String> excludedMachineReasonMap,
                                             NewSpecFailReasonEnum failReason,
                                             boolean success,
                                             String startTimeText,
                                             MachinePriorityTraceSnapshot confirmedSnapshot) {
        if (!PriorityTraceLogHelper.isEnabled(context)) {
            return;
        }
        String title = "SKU选机台TOP5候选列表";
        StringBuilder detailBuilder = new StringBuilder(1024);
        PriorityTraceLogHelper.appendTitleHeader(detailBuilder, title);

        // SKU基本信息
        String skuType = resolveNewSpecSkuType(sku);
        boolean isEnding = endingJudgmentStrategy.isExpectedEnding(context, sku);
        PriorityTraceLogHelper.appendLine(detailBuilder,
                PriorityTraceLogHelper.kv("排程日期", PriorityTraceLogHelper.formatDateTime(context.getScheduleDate()))
                        + ", " + PriorityTraceLogHelper.kv("SKU", sku.getMaterialCode())
                        + ", " + PriorityTraceLogHelper.kv("描述", sku.getMaterialDesc())
                        + ", " + PriorityTraceLogHelper.kv("待排产量", sku.resolveTargetScheduleQty())
                        + ", " + PriorityTraceLogHelper.kv("SKU类型", skuType)
                        + ", " + PriorityTraceLogHelper.kv("是否收尾", PriorityTraceLogHelper.oneZero(isEnding))
                        + ", " + PriorityTraceLogHelper.kv("规格", sku.getSpecCode())
                        + ", " + PriorityTraceLogHelper.kv("候选机台总数", PriorityTraceLogHelper.sizeOf(candidates))
                        + ", " + PriorityTraceLogHelper.kv("有效候选数", PriorityTraceLogHelper.sizeOf(candidates))
                        + ", " + PriorityTraceLogHelper.kv("已排除机台", CollectionUtils.isEmpty(excludedMachineCodes)
                        ? "-" : String.join(",", excludedMachineCodes)));

        // TOP5 候选机台
        int topN = LhScheduleConstant.SKU_MACHINE_CANDIDATE_TOP_N;
        int outputCount = Math.min(topN, PriorityTraceLogHelper.sizeOf(candidates));
        if (outputCount > 0) {
            PriorityTraceLogHelper.appendLine(detailBuilder, "TOP" + outputCount + "候选排序:");
            for (int i = 0; i < outputCount; i++) {
                MachineScheduleDTO machine = candidates.get(i);
                /*
                 * TOP5 与详细优先级日志统一读取选机时点冻结的 DTO。仅冻结收尾时间不够：
                 * 排产提交还会把前物料、前规格和英寸推进为本轮 SKU，导致延迟日志误判同规格、同英寸。
                 */
                MachineScheduleDTO traceMachine = Objects.isNull(confirmedSnapshot)
                        ? null : confirmedSnapshot.resolveCandidateSnapshot(machine.getMachineCode());
                MachineScheduleDTO logMachine = Objects.isNull(traceMachine) ? machine : traceMachine;
                boolean isSingleCtrl = this.isSingleControlMachine(
                        context, logMachine.getMachineCode());
                String reasonSuffix = (i == 0 && success && finalMachine != null
                        && StringUtils.equals(logMachine.getMachineCode(), finalMachine.getMachineCode()))
                        ? "最优候选" : ("候选" + (i + 1));
                /*
                 * 优先输出选机时点冻结的收尾时间，避免整轮结束后机台 estimatedEndTime
                 * 已被本轮结果推进，导致 TOP5 日志与正式选机画像口径不一致。
                 */
                Date traceEndingTime = Objects.nonNull(confirmedSnapshot)
                        && confirmedSnapshot.hasPriorityTraceEndingTime(logMachine.getMachineCode())
                        ? confirmedSnapshot.resolvePriorityTraceEndingTime(logMachine.getMachineCode())
                        : logMachine.getEstimatedEndTime();
                PriorityTraceLogHelper.appendLine(detailBuilder,
                        (i + 1)
                                + ". " + PriorityTraceLogHelper.kv("机台", logMachine.getMachineCode())
                                + ", " + PriorityTraceLogHelper.kv("名称", logMachine.getMachineName())
                                + ", " + PriorityTraceLogHelper.kv("单控", PriorityTraceLogHelper.oneZero(isSingleCtrl))
                                + ", " + PriorityTraceLogHelper.kv("收尾时间", PriorityTraceLogHelper.formatDateTime(traceEndingTime))
                                + ", " + PriorityTraceLogHelper.kv("当前在机", logMachine.getPreviousMaterialCode())
                                + ", " + PriorityTraceLogHelper.kv("前规格", logMachine.getPreviousSpecCode())
                                + ", " + PriorityTraceLogHelper.kv("机台顺序", logMachine.getMachineOrder())
                                + ", " + PriorityTraceLogHelper.kv("原因", reasonSuffix));
            }
            if (PriorityTraceLogHelper.sizeOf(candidates) > topN) {
                PriorityTraceLogHelper.appendLine(detailBuilder,
                        "... 共" + PriorityTraceLogHelper.sizeOf(candidates) + "台，仅展示前" + topN + "台");
            }
        }

        appendExcludedMachineReasonTrace(detailBuilder, excludedMachineReasonMap);

        // 局部搜索评估
        if (localSearchSuggestedMachine != null) {
            PriorityTraceLogHelper.appendLine(detailBuilder,
                    "局部搜索评估机台: " + localSearchSuggestedMachine.getMachineCode());
        }

        // 最终选中
        String selectReason = resolveNewSpecMachineSelectReason(context, sku, candidates, finalMachine,
                localSearchSuggestedMachine, excludedMachineCodes);
        PriorityTraceLogHelper.appendLine(detailBuilder,
                PriorityTraceLogHelper.kv("最终选中机台", finalMachine == null ? "-" : finalMachine.getMachineCode())
                        + ", " + PriorityTraceLogHelper.kv("选中原因", selectReason));
        if (success) {
            PriorityTraceLogHelper.appendLine(detailBuilder,
                    "决策结果: 成功, 开产时间=" + PriorityTraceLogHelper.safeText(startTimeText));
        } else {
            PriorityTraceLogHelper.appendLine(detailBuilder,
                    "决策结果: 失败, 原因=" + PriorityTraceLogHelper.safeText(
                            failReason == null ? null : failReason.getDescription())
                            + ", 备注=" + PriorityTraceLogHelper.safeText(startTimeText));
        }
        PriorityTraceLogHelper.appendTitleFooter(detailBuilder);
        String detail = detailBuilder.toString().trim();
        PriorityTraceLogHelper.logSortSummary(log, context, title, detail);
    }

    /**
     * 记录候选机台排除原因明细。
     *
     * @param excludedMachineReasonMap 排除原因明细
     * @param machineCode 机台编码
     * @param reason 排除原因
     * @param machineReadyTime 机台就绪时间
     * @param switchReadyTime 切换就绪时间
     * @param mouldChangeStartTime 换模开始时间
     * @param mouldChangeCompleteTime 换模完成时间
     * @param inspectionTime 首检时间
     * @param productionStartTime 开产时间
     * @param maxQtyToWindowEnd 窗口最大可排量
     * @param machinePlanQty 本机台计划量
     * @param machineScheduledQty 日计划回裁后排产量
     */
    private void recordExcludedMachineReason(Map<String, String> excludedMachineReasonMap,
                                             String machineCode,
                                             String reason,
                                             Date machineReadyTime,
                                             Date switchReadyTime,
                                             Date mouldChangeStartTime,
                                             Date mouldChangeCompleteTime,
                                             Date inspectionTime,
                                             Date productionStartTime,
                                             Integer maxQtyToWindowEnd,
                                             Integer machinePlanQty,
                                             Integer machineScheduledQty) {
        if (excludedMachineReasonMap == null || StringUtils.isEmpty(machineCode)) {
            return;
        }
        StringBuilder reasonBuilder = new StringBuilder(256);
        reasonBuilder.append(PriorityTraceLogHelper.kv("排除原因", reason));
        reasonBuilder.append(", ").append(PriorityTraceLogHelper.kv("机台就绪",
                LhScheduleTimeUtil.formatDateTime(machineReadyTime)));
        reasonBuilder.append(", ").append(PriorityTraceLogHelper.kv("切换就绪",
                LhScheduleTimeUtil.formatDateTime(switchReadyTime)));
        reasonBuilder.append(", ").append(PriorityTraceLogHelper.kv("换模开始",
                LhScheduleTimeUtil.formatDateTime(mouldChangeStartTime)));
        reasonBuilder.append(", ").append(PriorityTraceLogHelper.kv("换模完成",
                LhScheduleTimeUtil.formatDateTime(mouldChangeCompleteTime)));
        reasonBuilder.append(", ").append(PriorityTraceLogHelper.kv("首检",
                LhScheduleTimeUtil.formatDateTime(inspectionTime)));
        reasonBuilder.append(", ").append(PriorityTraceLogHelper.kv("开产",
                LhScheduleTimeUtil.formatDateTime(productionStartTime)));
        reasonBuilder.append(", ").append(PriorityTraceLogHelper.kv("最大可排量", maxQtyToWindowEnd));
        reasonBuilder.append(", ").append(PriorityTraceLogHelper.kv("本机台计划量", machinePlanQty));
        reasonBuilder.append(", ").append(PriorityTraceLogHelper.kv("日计划回裁量", machineScheduledQty));
        excludedMachineReasonMap.put(machineCode, reasonBuilder.toString());
    }

    /**
     * 记录新增候选机台通过换模推导后被计划量回裁的短过程日志。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param machineCode 候选机台
     * @param reason 排除原因
     * @param machineReadyTime 机台就绪时间
     * @param switchReadyTime 可切换时间
     * @param mouldChangeStartTime 换模开始时间
     * @param mouldChangeCompleteTime 换模完成时间
     * @param productionStartTime 开产时间
     * @param maxQtyToWindowEnd 窗口最大可排量
     * @param machinePlanQty 本机台计划量
     * @param machineScheduledQty 日计划回裁后排产量
     */
    private void appendNewSpecCandidateRejectedProcessLog(LhScheduleContext context,
                                                          SkuScheduleDTO sku,
                                                          String machineCode,
                                                          String reason,
                                                          Date machineReadyTime,
                                                          Date switchReadyTime,
                                                          Date mouldChangeStartTime,
                                                          Date mouldChangeCompleteTime,
                                                          Date productionStartTime,
                                                          Integer maxQtyToWindowEnd,
                                                          Integer machinePlanQty,
                                                          Integer machineScheduledQty) {
        if (Objects.isNull(context) || Objects.isNull(sku) || StringUtils.isEmpty(machineCode)) {
            return;
        }
        String detail = new StringBuilder(320)
                .append("scheduleDate=").append(LhScheduleTimeUtil.formatDate(context.getScheduleTargetDate()))
                .append(", materialCode=").append(sku.getMaterialCode())
                .append(", machineCode=").append(machineCode)
                .append(", dailyStandardQty=").append(resolveNewSpecDailyStandardQty(context, sku))
                .append(", machineReadyTime=").append(PriorityTraceLogHelper.formatDateTime(machineReadyTime))
                .append(", switchReadyTime=").append(PriorityTraceLogHelper.formatDateTime(switchReadyTime))
                .append(", mouldChangeStartTime=").append(PriorityTraceLogHelper.formatDateTime(mouldChangeStartTime))
                .append(", mouldChangeCompleteTime=").append(PriorityTraceLogHelper.formatDateTime(mouldChangeCompleteTime))
                .append(", productionStartTime=").append(PriorityTraceLogHelper.formatDateTime(productionStartTime))
                .append(", maxQtyToWindowEnd=").append(PriorityTraceLogHelper.safeText(maxQtyToWindowEnd))
                .append(", machinePlanQty=").append(PriorityTraceLogHelper.safeText(machinePlanQty))
                .append(", machineScheduledQty=").append(PriorityTraceLogHelper.safeText(machineScheduledQty))
                .append(", reason=").append(PriorityTraceLogHelper.safeText(reason))
                .toString();
        PriorityTraceLogHelper.appendProcessLog(context, "新增候选机台回裁跳过", detail);
    }

    /**
     * 记录新增排产因当前日优先 dayN 节奏满足而停止扩机台的决策。
     * <p>日志同时输出已有同物料机台及排程来源、月计划日节奏和理论日产能，
     * 用于证明普通空闲候选和续作释放尾部候选均未进入后续扩机判断。</p>
     *
     * @param context 排程上下文
     * @param sku 当前新增SKU
     * @param candidateMachineCode 当前待判断或刚完成排产的机台编码
     * @param businessTargetQty 本轮业务目标量
     * @param scheduledQty 本轮累计已排量
     * @param reason 停止扩机台原因
     */
    private void appendNewSpecDailyRhythmStopProcessLog(LhScheduleContext context,
                                                        SkuScheduleDTO sku,
                                                        String candidateMachineCode,
                                                        int businessTargetQty,
                                                        int scheduledQty,
                                                        String reason) {
        if (Objects.isNull(context) || Objects.isNull(sku)) {
            return;
        }
        String existingMachineSummary = resolveExistingSameMaterialMachineSummary(context, sku);
        String detail = new StringBuilder(384)
                .append("scheduleDate=").append(LhScheduleTimeUtil.formatDate(context.getScheduleTargetDate()))
                .append(", materialCode=").append(sku.getMaterialCode())
                .append(", candidateMachineCode=").append(PriorityTraceLogHelper.safeText(candidateMachineCode))
                .append(", existingMachines=").append(existingMachineSummary)
                .append(", dayN=").append(resolveDailyPlanRhythmSummary(sku))
                .append(", dailyStandardQty=").append(resolveNewSpecDailyStandardQty(context, sku))
                .append(", theoreticalMachineCount=")
                .append(countExistingSameMaterialResults(context, sku, null))
                .append(", businessTargetQty=").append(businessTargetQty)
                .append(", scheduledQty=").append(scheduledQty)
                .append(", stopAddMachine=true")
                .append(", enterReleasedTailCandidate=false")
                .append(", reason=").append(PriorityTraceLogHelper.safeText(reason))
                .toString();
        PriorityTraceLogHelper.appendProcessLog(context, "新增dayN满足停止扩机台", detail);
    }

    /**
     * 汇总当前 SKU 已落地的有效机台及其排程来源。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @return 机台编码/排程类型列表；无有效结果时返回短横线
     */
    private String resolveExistingSameMaterialMachineSummary(LhScheduleContext context, SkuScheduleDTO sku) {
        if (Objects.isNull(context) || Objects.isNull(sku)
                || CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return "-";
        }
        StringBuilder summaryBuilder = new StringBuilder(64);
        Set<String> addedMachineCodes = new LinkedHashSet<String>(4);
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (!isExistingSameMaterialActiveResult(context, result, sku, null)
                    || !addedMachineCodes.add(result.getLhMachineCode())) {
                continue;
            }
            if (summaryBuilder.length() > 0) {
                summaryBuilder.append(",");
            }
            summaryBuilder.append(result.getLhMachineCode())
                    .append("/")
                    .append(PriorityTraceLogHelper.safeText(result.getScheduleType()));
        }
        return summaryBuilder.length() > 0 ? summaryBuilder.toString() : "-";
    }

    /**
     * 汇总当前 SKU 月计划 dayN 日计划量。
     *
     * @param sku 当前SKU
     * @return 生产日期=日计划量列表；无账本时返回短横线
     */
    private String resolveDailyPlanRhythmSummary(SkuScheduleDTO sku) {
        if (Objects.isNull(sku) || CollectionUtils.isEmpty(sku.getDailyPlanQuotaMap())) {
            return "-";
        }
        StringBuilder summaryBuilder = new StringBuilder(96);
        for (Map.Entry<LocalDate, SkuDailyPlanQuotaDTO> entry : sku.getDailyPlanQuotaMap().entrySet()) {
            if (summaryBuilder.length() > 0) {
                summaryBuilder.append(",");
            }
            summaryBuilder.append(entry.getKey())
                    .append("=")
                    .append(Objects.nonNull(entry.getValue()) ? entry.getValue().getDayPlanQty() : 0);
        }
        return summaryBuilder.toString();
    }

    /**
     * 输出候选机台排除原因明细。
     *
     * @param detailBuilder 日志明细
     * @param excludedMachineReasonMap 排除原因明细
     */
    private void appendExcludedMachineReasonTrace(StringBuilder detailBuilder,
                                                  Map<String, String> excludedMachineReasonMap) {
        if (detailBuilder == null || CollectionUtils.isEmpty(excludedMachineReasonMap)) {
            return;
        }
        PriorityTraceLogHelper.appendLine(detailBuilder, "排除明细:");
        for (Map.Entry<String, String> entry : excludedMachineReasonMap.entrySet()) {
            PriorityTraceLogHelper.appendLine(detailBuilder,
                    "- " + PriorityTraceLogHelper.kv("机台", entry.getKey())
                            + ", " + PriorityTraceLogHelper.safeText(entry.getValue()));
        }
    }

    /**
     * 解析新增排产SKU类型描述。
     *
     * @param sku SKU
     * @return 类型描述
     */
    private static String resolveNewSpecSkuType(SkuScheduleDTO sku) {
        if (sku == null) {
            return "-";
        }
        if (ConstructionStageEnum.TRIAL.getCode().equals(sku.getConstructionStage())) {
            return "试制";
        }
        if (sku.isSmallBatchValidation()) {
            return "小批量";
        }
        if (ConstructionStageEnum.MASS_TRIAL.getCode().equals(sku.getConstructionStage())) {
            return "量试";
        }
        if (ConstructionStageEnum.FORMAL.getCode().equals(sku.getConstructionStage())) {
            return "正式";
        }
        return sku.getConstructionStage() != null ? sku.getConstructionStage() : "-";
    }

    /**
     * 解析新增排产选机台最终选中原因。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param candidates 候选机台列表
     * @param finalMachine 最终选中机台
     * @param localSearchSuggestedMachine 局部搜索评估机台
     * @param excludedMachineCodes 已排除机台编码
     * @return 选中原因
     */
    private static String resolveNewSpecMachineSelectReason(LhScheduleContext context, SkuScheduleDTO sku,
                                                             List<MachineScheduleDTO> candidates,
                                                             MachineScheduleDTO finalMachine,
                                                             MachineScheduleDTO localSearchSuggestedMachine,
                                                             Set<String> excludedMachineCodes) {
        if (finalMachine == null) {
            if (!CollectionUtils.isEmpty(candidates) && !CollectionUtils.isEmpty(excludedMachineCodes)) {
                return "候选机台全部被排除: " + String.join(",", excludedMachineCodes);
            }
            if (CollectionUtils.isEmpty(candidates)) {
                return "无可用候选机台";
            }
            return "机台选择失败";
        }
        List<String> reasons = new ArrayList<>(4);
        // 局部搜索评估命中
        if (localSearchSuggestedMachine != null
                && StringUtils.equals(finalMachine.getMachineCode(), localSearchSuggestedMachine.getMachineCode())) {
            reasons.add("局部搜索评估优");
        }
        // 候选排序首位
        if (!CollectionUtils.isEmpty(candidates)) {
            MachineScheduleDTO first = candidates.get(0);
            if (StringUtils.equals(finalMachine.getMachineCode(), first.getMachineCode())) {
                reasons.add("候选排序首位");
            }
        }
        // 收尾时间最接近
        if (finalMachine.getEstimatedEndTime() != null) {
            reasons.add("收尾时间最近");
        }
        // 排除后候选
        if (!CollectionUtils.isEmpty(excludedMachineCodes)) {
            reasons.add("排除" + excludedMachineCodes.size() + "台后选取");
        }
        if (reasons.isEmpty()) {
            reasons.add("排序兜底");
        }
        return String.join("，", reasons);
    }

    /**
     * 构建局部搜索窗口（当前SKU + 后续若干SKU）。
     *
     * @param context 排程上下文
     * @param currentSku 当前SKU
     * @return 局部搜索SKU窗口
     */
    private List<SkuScheduleDTO> buildLocalSearchWindow(LhScheduleContext context, SkuScheduleDTO currentSku) {
        List<SkuScheduleDTO> allNewSkuList = context.getNewSpecSkuList();
        int skuIndex = allNewSkuList.indexOf(currentSku);
        if (skuIndex < 0) {
            List<SkuScheduleDTO> fallbackList = new ArrayList<>(1);
            fallbackList.add(currentSku);
            return fallbackList;
        }
        int depth = context.getScheduleConfig() != null ? context.getScheduleConfig().getLocalSearchDepth() : 1;
        int endIndex = Math.min(allNewSkuList.size(), skuIndex + depth);
        return new ArrayList<>(allNewSkuList.subList(skuIndex, endIndex));
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建新增规格排程结果，并按班次分配计划量
     */
    private LhScheduleResult buildNewSpecScheduleResult(LhScheduleContext context,
                                                         MachineScheduleDTO machine,
                                                         SkuScheduleDTO sku,
                                                         Date startTime,
                                                         Date mouldChangeStartTime,
                                                         Date mouldChangeEndTime,
                                                         List<LhShiftConfigVO> shifts,
                                                         int mouldQty,
                                                         boolean isEnding,
                                                         MouldResourceAllocationResult mouldResourceAllocationResult) {
        return buildNewSpecScheduleResult(context, machine, sku, startTime, mouldChangeStartTime,
                mouldChangeEndTime, shifts, mouldQty, isEnding, mouldResourceAllocationResult,
                Collections.<Integer, Integer>emptyMap());
    }

    /**
     * 同胎胚同模具切换按换活字块口径回写排程结果。
     *
     * <p>普通新增主链选中机台时已按换活字块耗时和首检归属完成时间分配，这里把结果
     * 排程类型回写为 03、isTypeBlock 置 1，S4.6 据此生成 02-更换活字块交替计划，
     * 避免同胎胚同模具切换被落成正规换模（01）。非换活字块关系时不做任何修改。</p>
     *
     * @param context 排程上下文
     * @param result 已构建的新增排程结果
     * @param sku 当前新增 SKU
     * @param machineCode 选中机台编码
     * @param candidateMachine 选中机台
     * @param mouldChangeStartTime 切换开始时间
     * @param isTypeBlockRelation 是否命中同胎胚同模具换活字块关系
     */
    private void applyTypeBlockRelationToNewSpecResult(LhScheduleContext context,
                                                       LhScheduleResult result,
                                                       SkuScheduleDTO sku,
                                                       String machineCode,
                                                       MachineScheduleDTO candidateMachine,
                                                       Date mouldChangeStartTime,
                                                       boolean isTypeBlockRelation) {
        if (!isTypeBlockRelation) {
            return;
        }
        result.setScheduleType(ScheduleTypeEnum.TYPE_BLOCK.getCode());
        result.setIsTypeBlock("1");
        log.info("新增排产同胎胚同模具按换活字块口径落库, batchNo: {}, materialCode: {}, "
                        + "machineCode: {}, frontMaterialCode: {}, mouldChangeStartTime: {}",
                context.getBatchNo(), sku.getMaterialCode(), machineCode,
                candidateMachine.getCurrentMaterialCode(),
                LhScheduleTimeUtil.formatDateTime(mouldChangeStartTime));
    }

    /**
     * 将当轮选回的原续作机台结果恢复为续作语义。
     *
     * <p>该方法只处理已经通过“同物料、同状态、同原机台、整套模具一致”校验的结果。
     * 排产计算仍复用S4.5统一候选、设备窗口、班产和账本链；这里只统一最终业务身份，确保
     * S4.6不会生成换模/换活字块计划，换模均衡和首检资源也不会出现虚假消费。</p>
     *
     * @param context 排程上下文
     * @param result 已完成班次分配的结果
     * @param sku 当前续作加机台候选
     * @param machine 原释放续作机台
     * @param allocationResult 原机台模具预演结果
     * @param productionStartTime 实际重新启用时间
     */
    private void applyReleasedContinuationReuseToResult(
            LhScheduleContext context,
            LhScheduleResult result,
            SkuScheduleDTO sku,
            MachineScheduleDTO machine,
            MouldResourceAllocationResult allocationResult,
            Date productionStartTime) {
        if (Objects.isNull(context) || Objects.isNull(result)
                || Objects.isNull(sku) || Objects.isNull(machine)) {
            return;
        }
        result.setScheduleType(ScheduleTypeEnum.CONTINUOUS.getCode());
        result.setIsChangeMould("0");
        result.setIsTypeBlock("0");
        result.setMouldChangeStartTime(null);
        log.info("释放续作机台同物料同模具重新启用, factoryCode: {}, batchNo: {}, "
                        + "scheduleDate: {}, materialCode: {}, productStatus: {}, machineCode: {}, "
                        + "firstAddMachineProductionDate: {}, reuseStartTime: {}, mouldCodes: {}, "
                        + "scheduleType: {}, isChangeMould: 0, "
                        + "effect: 不换模、不首检、不占用换模配额、不提前生产",
                context.getFactoryCode(), context.getBatchNo(),
                LhScheduleTimeUtil.formatDate(context.getScheduleTargetDate()),
                sku.getMaterialCode(), sku.getProductStatus(), machine.getMachineCode(),
                sku.getFirstAddMachineProductionDate(),
                LhScheduleTimeUtil.formatDateTime(productionStartTime),
                Objects.isNull(allocationResult)
                        ? Collections.emptyList()
                        : allocationResult.getAllocatedMouldCodeList(),
                result.getScheduleType());
    }

    /**
     * 记录释放续作原样重新启用的批次过程日志。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日上下文
     * @param sku 当前续作加机台候选
     * @param machine 原释放续作机台
     * @param allocationResult 原机台模具预演结果
     * @param productionStartTime 实际重新启用时间
     * @param schedulingShifts 本轮实际写入班次
     */
    private void appendReleasedContinuationReuseProcessLog(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            SkuScheduleDTO sku,
            MachineScheduleDTO machine,
            MouldResourceAllocationResult allocationResult,
            Date productionStartTime,
            List<LhShiftConfigVO> schedulingShifts) {
        if (Objects.isNull(context) || Objects.isNull(dayContext)
                || Objects.isNull(sku) || Objects.isNull(machine)) {
            return;
        }
        LhShiftConfigVO startShift = NewSpecEmbryoAvailableTimeResolver.resolveProductionShift(
                schedulingShifts, productionStartTime);
        String detail = new StringBuilder(384)
                .append("factoryCode=").append(context.getFactoryCode())
                .append(", batchNo=").append(context.getBatchNo())
                .append(", productionBusinessDate=").append(dayContext.getScheduleDate())
                .append(", materialCode=").append(sku.getMaterialCode())
                .append(", productStatus=").append(sku.getProductStatus())
                .append(", machineCode=").append(machine.getMachineCode())
                .append(", preferredContinuousMachineCode=")
                .append(sku.getPreferredContinuousMachineCode())
                .append(", firstAddMachineProductionDate=")
                .append(sku.getFirstAddMachineProductionDate())
                .append(", reuseStartTime=")
                .append(LhScheduleTimeUtil.formatDateTime(productionStartTime))
                .append(", startShift=")
                .append(Objects.isNull(startShift) || Objects.isNull(startShift.getShiftIndex())
                        ? "无" : "class" + startShift.getShiftIndex())
                .append(", currentMouldCodes=")
                .append(Objects.isNull(allocationResult)
                        ? Collections.emptyList()
                        : allocationResult.getReleasedMouldCodeList())
                .append(", reusedMouldCodes=")
                .append(Objects.isNull(allocationResult)
                        ? Collections.emptyList()
                        : allocationResult.getAllocatedMouldCodeList())
                .append(", resultScheduleType=01, isChangeMould=0, firstInspection=0, "
                        + "mouldQuotaConsumed=0, earlyProduction=0")
                .toString();
        PriorityTraceLogHelper.appendProcessLog(
                context, "释放续作机台同物料同模具重新启用", detail);
    }

    /**
     * 构建新增规格排程结果，并按修正后的班次上限分配计划量。
     */
    private LhScheduleResult buildNewSpecScheduleResult(LhScheduleContext context,
                                                         MachineScheduleDTO machine,
                                                         SkuScheduleDTO sku,
                                                         Date startTime,
                                                         Date mouldChangeStartTime,
                                                         Date mouldChangeEndTime,
                                                         List<LhShiftConfigVO> shifts,
                                                         int mouldQty,
                                                         boolean isEnding,
                                                         MouldResourceAllocationResult mouldResourceAllocationResult,
                                                         Map<Integer, Integer> shiftPlanCapacityMap) {
        LhShiftConfigVO firstInspectionAttributionShift = FirstInspectionQtyUtil.resolveFirstInspectionAttributionShift(
                context, sku, shifts, mouldChangeEndTime, ScheduleTypeEnum.NEW_SPEC.getCode());
        return buildNewSpecScheduleResult(context, machine, sku, startTime, mouldChangeStartTime,
                mouldChangeEndTime, shifts, mouldQty, isEnding, mouldResourceAllocationResult,
                shiftPlanCapacityMap, firstInspectionAttributionShift, null, false);
    }

    /**
     * 构建新增规格排程结果，并按修正后的班次上限和首检归属班次分配计划量。
     */
    private LhScheduleResult buildNewSpecScheduleResult(LhScheduleContext context,
                                                         MachineScheduleDTO machine,
                                                         SkuScheduleDTO sku,
                                                         Date startTime,
                                                         Date mouldChangeStartTime,
                                                         Date mouldChangeEndTime,
                                                         List<LhShiftConfigVO> shifts,
                                                         int mouldQty,
                                                         boolean isEnding,
                                                         MouldResourceAllocationResult mouldResourceAllocationResult,
                                                         Map<Integer, Integer> shiftPlanCapacityMap,
                                                         LhShiftConfigVO firstInspectionAttributionShift) {
        return buildNewSpecScheduleResult(
                context, machine, sku, startTime, mouldChangeStartTime, mouldChangeEndTime,
                shifts, mouldQty, isEnding, mouldResourceAllocationResult, shiftPlanCapacityMap,
                firstInspectionAttributionShift, null, false);
    }

    /**
     * 构建新增规格排程结果，并按统一生产门禁标识选择部分班次首检口径。
     *
     * @param context 排程上下文
     * @param machine 候选机台
     * @param sku 当前新增 SKU
     * @param startTime 实际生产开始时间
     * @param mouldChangeStartTime 换模开始时间
     * @param mouldChangeEndTime 换模结束时间
     * @param shifts 排程班次
     * @param mouldQty 模数
     * @param isEnding 是否收尾
     * @param mouldResourceAllocationResult 模具资源分配结果
     * @param shiftPlanCapacityMap 班次计划量上限
     * @param firstInspectionAttributionShift 首检归属班次
     * @param productionStartTimeConstrained 是否启用生产门禁部分班次产能口径
     * @return 新增规格排程结果
     */
    private LhScheduleResult buildNewSpecScheduleResult(
            LhScheduleContext context,
            MachineScheduleDTO machine,
            SkuScheduleDTO sku,
            Date startTime,
            Date mouldChangeStartTime,
            Date mouldChangeEndTime,
            List<LhShiftConfigVO> shifts,
            int mouldQty,
            boolean isEnding,
            MouldResourceAllocationResult mouldResourceAllocationResult,
            Map<Integer, Integer> shiftPlanCapacityMap,
            LhShiftConfigVO firstInspectionAttributionShift,
            FirstInspectionAllocationPlan firstInspectionAllocationPlan,
            boolean productionStartTimeConstrained) {
        LhScheduleResult result = new LhScheduleResult();
        result.setFactoryCode(context.getFactoryCode());
        result.setBatchNo(context.getBatchNo());
        result.setOrderNo(generateOrderNo(context));
        result.setLhMachineCode(machine.getMachineCode());
        result.setLhMachineName(machine.getMachineName());
        result.setLeftRightMould(LeftRightMouldUtil.resolveLeftRightMould(result.getLeftRightMould(), machine.getMachineCode()));
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
        result.setMonthPlanSumTotal(sku.getMonthPlanSumTotal());
        result.setTotalFinishQty(sku.getFinishedQty());
        // 日标准产量：复用上下文 SKU 日硫化产能主数据，无主数据则为 0
        result.setStandardCapacity(ShiftCapacityResolverUtil.resolveDailyStandardQty(
                context, sku.getMaterialCode()));
        // 默认非提前生产，命中后由 appendEarlyProductionRemark 与备注同源置 1
        result.setIsEarlyProduction("0");
        result.setIsEnd(isEnding ? "1" : "0");
        result.setIsDelivery(sku.isDeliveryLocked() ? "1" : "0");
        result.setIsRelease("0");
        result.setDataSource("0");
        result.setIsDelete(0);
        result.setScheduleType(NEW_SPEC_SCHEDULE_TYPE);
        boolean takeoverWithoutMouldChange =
                context.isScheduleSubstitutionSku(sku)
                        && Objects.nonNull(context.getScheduleSubstitutionDirective())
                        && context.getScheduleSubstitutionDirective()
                        .isTakeoverWithoutMouldChange();
        /*
         * A 接管属于续作机台上的同模具无缝继承，结果仍按新增来源 scheduleType=02 记录，
         * 但必须明确标记不换模、不换活字块，避免 S4.6 为 A 生成虚假的模具交替计划。
         */
        result.setIsChangeMould(takeoverWithoutMouldChange ? "0" : "1");
        result.setIsTypeBlock("0");
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
        result.setIsTrial(sku.isTrial() ? "1" : "0");
        result.setMachineOrder(machine.getMachineOrder());
        result.setRealScheduleDate(context.getScheduleDate());
        result.setProductionStatus("0");
        result.setMouldCode(resolveActualMouldCodeForNewSpecResult(
                context, sku, machine, mouldQty, mouldResourceAllocationResult));
        result.setHasSpecialMaterial(LhSpecialMaterialUtil.resolveHasSpecialMaterial(context, sku));
        /*
         * 普通新增和 B 迁移保存真实换模开始时间，供下游换模计划表复用；
         * A 是原机台原模具直接接管，不得伪造换模时间，否则即使 isChangeMould=0，
         * 下游审计和时间轴核对仍会误认为 A 发生过换模。
         */
        result.setMouldChangeStartTime(
                takeoverWithoutMouldChange
                        ? null : mouldChangeStartTime);

        // 按班次分配计划量；试制SKU早班换模后首检任务归属中班，但不生成首检条数，8小时换模耗时不再额外增加。
        int pendingQty = sku.resolveTargetScheduleQty();
        /*
         * 构建结果分班前过滤清洗窗口：只剔除与真实换模区间重叠、按现有规则可并行的清洗。
         * 胎胚可供等生产门禁可能晚于换模结束，不能用正式开产时间扩大重叠判断区间。
         */
        List<MachineCleaningWindowDTO> cleaningWindowList = resolveEffectiveCleaningWindowList(
                context, result.getLhMachineCode(), sku, mouldChangeStartTime, mouldChangeEndTime);
        List<MachineMaintenanceWindowDTO> maintenanceWindowList = resolveMachineMaintenanceWindowList(
                context, result.getLhMachineCode());
        distributeToShifts(context, result, shifts, startTime,
                runtimeShiftCapacity, sku.getLhTimeSeconds(), mouldQty, pendingQty, cleaningWindowList,
                maintenanceWindowList, sku, isEnding, mouldChangeEndTime, shiftPlanCapacityMap,
                firstInspectionAttributionShift, false, productionStartTimeConstrained,
                firstInspectionAllocationPlan);
        boolean plannedRepairAffectingSwitch = ShiftCapacityResolverUtil.isPlannedRepairAffectingSwitch(
                context, context.getDevicePlanShutList(), result.getLhMachineCode(), machine.getEstimatedEndTime(),
                mouldChangeStartTime, mouldChangeEndTime);
        if (plannedRepairAffectingSwitch && Objects.nonNull(firstInspectionAttributionShift)
                && (Objects.isNull(firstInspectionAllocationPlan)
                || firstInspectionAllocationPlan.getInspectionQty() <= 0)) {
            /*
             * 首检工具写首检条数时会先使用标准班次起点；维修场景必须再对齐到
             * max(维修结束, 换模结束)+预热，避免最终结果看起来在预热完成前已经开始首检。
             */
            Date firstInspectionReadyTime = ShiftCapacityResolverUtil.resolvePlannedRepairProductionReadyTime(
                    context, context.getDevicePlanShutList(), result.getLhMachineCode(), machine.getEstimatedEndTime(),
                    mouldChangeStartTime, mouldChangeEndTime);
            ShiftFieldUtil.alignShiftStartTimeNotBefore(
                    result, firstInspectionAttributionShift.getShiftIndex(), firstInspectionReadyTime);
        }
        if (productionStartTimeConstrained && Objects.nonNull(firstInspectionAttributionShift)
                && (Objects.isNull(firstInspectionAllocationPlan)
                || firstInspectionAllocationPlan.getInspectionQty() <= 0)) {
            /*
             * 首检工具默认把班次开始时间写入结果。统一生产门禁路径必须再次抬高到实际生产起点，
             * 防止结果字段表现为胎胚可供或 X/T 中班门禁前已经开始首检/生产。
             */
            ShiftFieldUtil.alignShiftStartTimeNotBefore(
                    result, firstInspectionAttributionShift.getShiftIndex(), startTime);
        }
        refreshResultSummary(context, result);
        return result;
    }

    /**
     * 构建双模 SKU 单控整机配对侧排程结果。
     * <p>配对侧必须与主侧保持相同 SKU、相同开产/结束时间和相同班次计划量；
     * 因此先复制主侧已完成的班次分配，再仅替换机台、左右模、工单号和实际模具号。</p>
     *
     * @param context 排程上下文
     * @param primaryResult 主侧结果
     * @param pairMachine 配对侧机台
     * @param sku 当前SKU
     * @param mouldQty 模台数
     * @param pairAllocationResult 配对侧模具分配结果
     * @return 配对侧结果
     */
    private LhScheduleResult buildWholeSingleControlPairResult(LhScheduleContext context,
                                                               LhScheduleResult primaryResult,
                                                               MachineScheduleDTO pairMachine,
                                                               SkuScheduleDTO sku,
                                                               int mouldQty,
                                                               MouldResourceAllocationResult pairAllocationResult) {
        if (Objects.isNull(primaryResult) || Objects.isNull(pairMachine)) {
            return null;
        }
        LhScheduleResult pairResult = new LhScheduleResult();
        BeanUtil.copyProperties(primaryResult, pairResult);
        pairResult.setOrderNo(generateOrderNo(context));
        pairResult.setLhMachineCode(pairMachine.getMachineCode());
        pairResult.setLhMachineName(pairMachine.getMachineName());
        pairResult.setLeftRightMould(LeftRightMouldUtil.resolveLeftRightMould(
                pairResult.getLeftRightMould(), pairMachine.getMachineCode()));
        pairResult.setMachineOrder(pairMachine.getMachineOrder());
        pairResult.setMouldCode(resolveActualMouldCodeForNewSpecResult(
                context, sku, pairMachine, mouldQty, pairAllocationResult));
        // 主侧已代表物理整机完成一次换胶囊判断，配对侧只复制计划量，不重复展示换胶囊备注。
        for (int shiftIndex = 1; shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
            ShiftFieldUtil.removeShiftAnalysis(
                    pairResult, shiftIndex, CapsuleReplacementRuleService.CAPSULE_REPLACEMENT_ANALYSIS);
            ShiftFieldUtil.removeShiftAnalysis(
                    pairResult, shiftIndex, FirstInspectionQtyUtil.FIRST_INSPECTION_ANALYSIS);
        }
        refreshResultSummary(context, pairResult);
        return pairResult;
    }

    /**
     * 同SKU多机台排产统一收口。
     * <p>先做SKU收尾同班次尾量归集，再做非收尾辅助机台释放，最后才尝试机台尾量错峰。</p>
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param shifts 排程窗口班次
     * @param quantityPolicy 数量策略
     * @param isEnding 是否SKU收尾
     */
    private void adjustSameSkuMultiMachineAllocation(LhScheduleContext context,
                                                     SkuScheduleDTO sku,
                                                     List<LhShiftConfigVO> shifts,
                                                     ProductionQuantityPolicy quantityPolicy,
                                                     boolean isEnding) {
        List<LhScheduleResult> sameSkuResults = collectSameSkuNewSpecResults(context, sku, null);
        if (CollectionUtils.isEmpty(sameSkuResults) || sameSkuResults.size() < 2) {
            return;
        }
        /*
         * 前日交替计划反选结果固定的是“机台+SKU”关系。普通多机台收口可能清空辅助机台、
         * 归集尾量或把收尾尾量搬到另一台，都会破坏该固定关系；只要当前SKU存在反选保护结果，
         * 本轮跳过同SKU后置搬量。结果数量和时间仍由前面的真实主链计算，不复制历史时间。
         */
        for (LhScheduleResult result : sameSkuResults) {
            if (context.isHistoricalReverseProtectedResult(result)) {
                log.info("新增SKU同SKU多机台收口跳过, materialCode: {}, protectedMachine: {}, "
                                + "reason: 前日交替计划机台反选结果需保持机台与SKU关系",
                        sku.getMaterialCode(), result.getLhMachineCode());
                return;
            }
        }
        String beforeSummary = buildSameSkuAllocationSummary(sameSkuResults);
        boolean tailConcentrated = false;
        boolean auxiliaryReleased = false;
        boolean staggered = false;
        if (isEnding) {
            tailConcentrated = concentrateEndingTailWithinSameShift(context, sku, shifts, sameSkuResults);
        }
        // 双模 L/R 是同一物理排产组，不能进入按单台结果释放的辅助机台链路。
        List<LhScheduleResult> independentResults = resolveIndependentPostProcessResults(
                context, sku, sameSkuResults);
        if (!isEnding && independentResults.size() >= 2 && quantityPolicy != null
                && quantityPolicy.isAllowFillStartedShift()
                && !quantityPolicy.isStrictUpperLimit()) {
            auxiliaryReleased = releaseAuxiliaryMachineForNonEnding(
                    context, sku, shifts, quantityPolicy, independentResults);
        }
        staggered = adjustSameSkuMultiMachineEndingStagger(context, sku, shifts);
        if (tailConcentrated || auxiliaryReleased || staggered) {
            refreshNewSpecEndingFlagByResult(context);
            log.info("新增SKU同SKU多机台收口, materialCode: {}, skuType: {}, isEnding: {}, sameSkuMultiMachine: 1, "
                            + "tailConcentrated: {}, auxiliaryReleased: {}, machineTailStaggered: {}, before: {}, after: {}",
                    sku.getMaterialCode(), resolveNewSpecSkuType(sku), isEnding,
                    oneZero(tailConcentrated), oneZero(auxiliaryReleased), oneZero(staggered),
                    beforeSummary, buildSameSkuAllocationSummary(sameSkuResults));
        }
    }

    /**
     * 增机台失败后回填已成功机台的尾部有效产能。
     * <p>动态拆量先给后续候选机台预留产量；当后续候选因模具、换模或窗口资源失败时，
     * 已在机 SKU 应继续吃满当前机台可用尾部产能。回填通过增量结果复用实际消费账本和 dayN 节奏扣账，
     * 避免绕开硫化余量、收尾目标量和日计划审计。</p>
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param result 已成功落地的排程结果
     * @param segment 已成功机台的生产段
     * @param shifts 排程窗口班次
     * @param quantityPolicy 数量策略
     * @param remainingQty 多机台拆量剩余量
     * @param allowFutureQuotaConsumption 是否允许消费未来 dayN 额度
     * @return 实际回填量
     */
    private int refillScheduledResultAfterAddMachineFailure(LhScheduleContext context,
                                                            SkuScheduleDTO sku,
                                                            LhScheduleResult result,
                                                            MachineProductionSegment segment,
                                                            List<LhShiftConfigVO> shifts,
                                                            ProductionQuantityPolicy quantityPolicy,
                                                            int remainingQty,
                                                            boolean allowFutureQuotaConsumption) {
        if (Objects.isNull(context) || Objects.isNull(sku) || Objects.isNull(result)
                || Objects.isNull(segment) || CollectionUtils.isEmpty(shifts) || remainingQty <= 0) {
            return 0;
        }
        if (!StringUtils.equals(sku.getMaterialCode(), result.getMaterialCode())
                || !StringUtils.equals(StringUtils.trimToEmpty(sku.getProductStatus()),
                StringUtils.trimToEmpty(result.getProductStatus()))
                || !StringUtils.equals(result.getLhMachineCode(), segment.getMachineCode())
                || CollectionUtils.isEmpty(segment.getShiftCapacityMap())) {
            return 0;
        }
        if (isWholeSingleControlResult(context, sku, result)) {
            // 当前回填段只描述单侧机台产能，无法证明配对侧在相同班次仍有等量尾部产能。
            // 双模组宁可保留剩余量给后续排程，也不能只增加一侧而破坏 L/R 同步。
            log.info("新增SKU双模组跳过单侧尾部回填, materialCode: {}, machineCode: {}, pairMachineCode: {}, "
                            + "remainingQty: {}, reason: 缺少配对侧等量尾部产能段",
                    sku.getMaterialCode(), result.getLhMachineCode(),
                    LhSingleControlMachineUtil.resolvePairMachineCode(result.getLhMachineCode()), remainingQty);
            return 0;
        }
        int currentScheduledQty = ShiftFieldUtil.resolveScheduledQty(result);
        int availableTailQty = Math.max(0, segment.getMaxQtyToWindowEnd() - currentScheduledQty);
        int refillLimitQty = Math.min(Math.max(0, remainingQty), availableTailQty);
        if (refillLimitQty <= 0) {
            return 0;
        }
        LhScheduleResult deltaResult = buildAddMachineFailureRefillDeltaResult(result);
        int deltaQty = allocateRefillDeltaToShifts(context, sku, deltaResult, result, segment, shifts, refillLimitQty);
        if (deltaQty <= 0) {
            return 0;
        }
        DailyQuotaLedgerBaseline quotaLedgerBaseline =
                DailyQuotaLedgerBaseline.capture(context, sku);
        int actualRefillQty = this.applyBlockToDailyQuota(
                context, sku, deltaResult, shifts, allowFutureQuotaConsumption);
        if (actualRefillQty <= 0) {
            // 回填结果尚未合并，额度裁零时只需恢复本次扣账产生的运行态变更。
            quotaLedgerBaseline.restore(context, sku);
            return 0;
        }
        mergeRefillDeltaResult(result, deltaResult, shifts);
        refreshResultSummary(context, result);
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(result.getLhMachineCode());
        if (Objects.nonNull(machine)) {
            updateMachineState(context, machine, sku, result);
        }
        recordScheduledMachineForResult(context, result, shifts, false);
        log.info("新增SKU增机台候选失败后回填原机台尾部产能, materialCode: {}, machineCode: {}, "
                        + "refillLimitQty: {}, actualRefillQty: {}, beforeQty: {}, afterQty: {}, remainingQty: {}, "
                        + "strictUpperLimit: {}",
                sku.getMaterialCode(), result.getLhMachineCode(), refillLimitQty, actualRefillQty,
                currentScheduledQty, ShiftFieldUtil.resolveScheduledQty(result), remainingQty,
                quantityPolicy != null && quantityPolicy.isStrictUpperLimit());
        return actualRefillQty;
    }

    /**
     * 构建增机台失败回填的增量结果。
     *
     * @param sourceResult 原排程结果
     * @return 增量结果
     */
    private LhScheduleResult buildAddMachineFailureRefillDeltaResult(LhScheduleResult sourceResult) {
        LhScheduleResult deltaResult = new LhScheduleResult();
        deltaResult.setMaterialCode(sourceResult.getMaterialCode());
        deltaResult.setMaterialDesc(sourceResult.getMaterialDesc());
        deltaResult.setStructureName(sourceResult.getStructureName());
        deltaResult.setSpecCode(sourceResult.getSpecCode());
        deltaResult.setSpecDesc(sourceResult.getSpecDesc());
        deltaResult.setEmbryoCode(sourceResult.getEmbryoCode());
        deltaResult.setLhMachineCode(sourceResult.getLhMachineCode());
        deltaResult.setScheduleType(sourceResult.getScheduleType());
        deltaResult.setIsEnd(sourceResult.getIsEnd());
        deltaResult.setIsChangeMould(sourceResult.getIsChangeMould());
        deltaResult.setIsTypeBlock(sourceResult.getIsTypeBlock());
        deltaResult.setMouldQty(sourceResult.getMouldQty());
        deltaResult.setSingleMouldShiftQty(sourceResult.getSingleMouldShiftQty());
        deltaResult.setLhTime(sourceResult.getLhTime());
        return deltaResult;
    }

    /**
     * 将回填增量分配到原机台仍有空余的班次。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param deltaResult 增量结果
     * @param sourceResult 原排程结果
     * @param segment 原机台生产段
     * @param shifts 班次列表
     * @param refillLimitQty 最大回填量
     * @return 增量分配量
     */
    private int allocateRefillDeltaToShifts(LhScheduleContext context,
                                            SkuScheduleDTO sku,
                                            LhScheduleResult deltaResult,
                                            LhScheduleResult sourceResult,
                                            MachineProductionSegment segment,
                                            List<LhShiftConfigVO> shifts,
                                            int refillLimitQty) {
        int mouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(
                sourceResult.getMouldQty() == null ? 0 : sourceResult.getMouldQty());
        if (mouldQty <= 0) {
            return 0;
        }
        int remainingRefillQty = Math.max(0, refillLimitQty);
        int allocatedQty = 0;
        for (LhShiftConfigVO shift : shifts) {
            if (remainingRefillQty <= 0 || Objects.isNull(shift) || Objects.isNull(shift.getShiftIndex())) {
                break;
            }
            Integer shiftCapacity = segment.getShiftCapacityMap().get(shift.getShiftIndex());
            if (Objects.isNull(shiftCapacity) || shiftCapacity <= 0) {
                continue;
            }
            Integer existingQty = ShiftFieldUtil.getShiftPlanQty(sourceResult, shift.getShiftIndex());
            int currentQty = existingQty == null ? 0 : Math.max(0, existingQty);
            int availableQty = Math.max(0, shiftCapacity - currentQty);
            if (availableQty <= 0) {
                continue;
            }
            int shiftRefillQty = ShiftCapacityResolverUtil.normalizeAllocatedShiftQty(
                    Math.min(remainingRefillQty, availableQty), availableQty, mouldQty);
            if (shiftRefillQty <= 0) {
                continue;
            }
            if (!canIncreaseShiftQtyByClassTotalLimit(context, sku, sourceResult, shift.getShiftIndex(), shiftRefillQty,
                    "新增SKU增机台失败后原机台回填")) {
                continue;
            }
            // 回填是在原结果已有产量基础上的真实增量，必须用原结果判断胶囊次数并承载换胶囊备注。
            shiftRefillQty = capsuleReplacementRuleService.resolveActualPlanQty(
                    context, sourceResult, shift, shiftRefillQty, currentQty + availableQty,
                    shift.getShiftStartDateTime(),
                    "新增SKU增机台失败后原机台回填");
            if (shiftRefillQty <= 0) {
                continue;
            }
            Date shiftStartTime = currentQty > 0
                    ? ShiftFieldUtil.getShiftStartTime(sourceResult, shift.getShiftIndex())
                    : shift.getShiftStartDateTime();
            setShiftPlanQty(deltaResult, shift.getShiftIndex(), shiftRefillQty,
                    shiftStartTime, shift.getShiftEndDateTime());
            remainingRefillQty -= shiftRefillQty;
            allocatedQty += shiftRefillQty;
        }
        ShiftFieldUtil.syncDailyPlanQty(deltaResult);
        return allocatedQty;
    }

    /**
     * 将通过扣账后的回填增量合并回原排程结果。
     *
     * @param targetResult 原排程结果
     * @param deltaResult 增量结果
     * @param shifts 班次列表
     */
    private void mergeRefillDeltaResult(LhScheduleResult targetResult,
                                        LhScheduleResult deltaResult,
                                        List<LhShiftConfigVO> shifts) {
        for (LhShiftConfigVO shift : shifts) {
            if (Objects.isNull(shift) || Objects.isNull(shift.getShiftIndex())) {
                continue;
            }
            Integer deltaQty = ShiftFieldUtil.getShiftPlanQty(deltaResult, shift.getShiftIndex());
            if (Objects.isNull(deltaQty) || deltaQty <= 0) {
                continue;
            }
            Integer existingQty = ShiftFieldUtil.getShiftPlanQty(targetResult, shift.getShiftIndex());
            Date existingStartTime = ShiftFieldUtil.getShiftStartTime(targetResult, shift.getShiftIndex());
            Date deltaStartTime = ShiftFieldUtil.getShiftStartTime(deltaResult, shift.getShiftIndex());
            Date deltaEndTime = ShiftFieldUtil.getShiftEndTime(deltaResult, shift.getShiftIndex());
            int mergedQty = Math.max(0, existingQty == null ? 0 : existingQty) + deltaQty;
            setShiftPlanQty(targetResult, shift.getShiftIndex(), mergedQty,
                    existingStartTime == null ? deltaStartTime : existingStartTime, deltaEndTime);
        }
        ShiftFieldUtil.syncDailyPlanQty(targetResult);
    }

    private boolean concentrateEndingTailWithinSameShift(LhScheduleContext context,
                                                         SkuScheduleDTO sku,
                                                         List<LhShiftConfigVO> shifts,
                                                         List<LhScheduleResult> sameSkuResults) {
        if (context == null || sku == null || CollectionUtils.isEmpty(shifts)
                || CollectionUtils.isEmpty(sameSkuResults)) {
            return false;
        }
        Map<Integer, List<LhScheduleResult>> endingShiftResultMap = new LinkedHashMap<>(4);
        for (LhScheduleResult result : sameSkuResults) {
            int lastShiftIndex = resolveLastPlannedShiftIndex(result);
            if (lastShiftIndex <= 0) {
                continue;
            }
            Integer endingQty = ShiftFieldUtil.getShiftPlanQty(result, lastShiftIndex);
            if (endingQty == null || endingQty <= 0) {
                continue;
            }
            endingShiftResultMap.computeIfAbsent(lastShiftIndex, key -> new ArrayList<LhScheduleResult>(2))
                    .add(result);
        }
        boolean adjusted = false;
        for (Map.Entry<Integer, List<LhScheduleResult>> entry : endingShiftResultMap.entrySet()) {
            if (entry.getValue().size() < 2) {
                continue;
            }
            if (concentrateEndingTailOnShift(context, sku, shifts, entry.getKey(), entry.getValue())) {
                adjusted = true;
            }
        }
        return adjusted;
    }

    private boolean concentrateEndingTailOnShift(LhScheduleContext context,
                                                 SkuScheduleDTO sku,
                                                 List<LhShiftConfigVO> shifts,
                                                 int endingShiftIndex,
                                                 List<LhScheduleResult> results) {
        LhShiftConfigVO endingShift = findShiftByIndex(shifts, endingShiftIndex);
        if (endingShift == null || CollectionUtils.isEmpty(results) || results.size() < 2) {
            return false;
        }
        int endingDemandQty = resolveEndingDemandQty(context, results.get(0));
        int scheduledBeforeShift = resolveSameSkuScheduledQtyBeforeShift(results, endingShiftIndex);
        int sameShiftTotalCapacity = resolveSameShiftAvailableCapacity(context, results, endingShift);
        int remainingQty = Math.max(0, endingDemandQty - scheduledBeforeShift);
        if (remainingQty <= 0 || remainingQty >= sameShiftTotalCapacity) {
            return false;
        }
        List<LhScheduleResult> sortedResults = new ArrayList<LhScheduleResult>(results);
        sortedResults.sort(buildSameSkuPrimaryComparator(endingShiftIndex));
        Map<LhScheduleResult, Integer> originalShiftQtyMap = new LinkedHashMap<LhScheduleResult, Integer>(sortedResults.size());
        Map<LhScheduleResult, Integer> targetShiftQtyMap = new LinkedHashMap<LhScheduleResult, Integer>(sortedResults.size());
        Set<String> processedWholeMachineCodeSet = new HashSet<String>(4);
        int remainingToAllocate = remainingQty;
        boolean changed = false;
        for (LhScheduleResult result : sortedResults) {
            Integer originalQty = ShiftFieldUtil.getShiftPlanQty(result, endingShiftIndex);
            if (originalQty == null || originalQty <= 0) {
                continue;
            }
            if (isWholeSingleControlResult(context, sku, result)) {
                String physicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                        result.getLhMachineCode());
                if (!processedWholeMachineCodeSet.add(physicalMachineCode)) {
                    continue;
                }
                LhScheduleResult pairResult = findPairResult(results, result);
                if (Objects.isNull(pairResult)) {
                    // 配对侧缺失交由保存前强校验阻断，不能把缺侧结果继续当普通单机归集。
                    continue;
                }
                Integer pairOriginalQty = ShiftFieldUtil.getShiftPlanQty(pairResult, endingShiftIndex);
                int resolvedPairOriginalQty = Objects.isNull(pairOriginalQty) ? 0 : Math.max(0, pairOriginalQty);
                originalShiftQtyMap.put(result, originalQty);
                originalShiftQtyMap.put(pairResult, resolvedPairOriginalQty);
                int groupShiftCapacity = resolveWholeSingleControlShiftCapacity(
                        context, result, pairResult, endingShift);
                int groupTargetQty = Math.min(Math.max(0, groupShiftCapacity), Math.max(0, remainingToAllocate));
                // 双模组总量必须可均分到 L/R，奇数尾量留给其他独立机台或后续排程。
                groupTargetQty -= groupTargetQty % 2;
                int sideTargetQty = groupTargetQty / 2;
                targetShiftQtyMap.put(result, sideTargetQty);
                targetShiftQtyMap.put(pairResult, sideTargetQty);
                remainingToAllocate = Math.max(0, remainingToAllocate - groupTargetQty);
                changed = changed || sideTargetQty != originalQty
                        || sideTargetQty != resolvedPairOriginalQty;
                continue;
            }
            originalShiftQtyMap.put(result, originalQty);
            int shiftCapacity = Math.max(originalQty, Math.max(
                    resolveAvailableShiftQtyForEndingStagger(context, result, endingShift),
                    resolveResultBaseShiftCapacity(result)));
            int newQty = Math.min(Math.max(0, shiftCapacity), Math.max(0, remainingToAllocate));
            targetShiftQtyMap.put(result, newQty);
            remainingToAllocate = Math.max(0, remainingToAllocate - newQty);
            changed = changed || newQty != originalQty;
        }
        if (!changed) {
            return false;
        }
        if (!canApplyShiftTargetQtyByClassTotalLimit(context, sku, endingShiftIndex, targetShiftQtyMap,
                "新增SKU收尾同班次尾量归集")) {
            return false;
        }
        for (Map.Entry<LhScheduleResult, Integer> targetEntry : targetShiftQtyMap.entrySet()) {
            int newQty = Math.max(0, targetEntry.getValue() == null ? 0 : targetEntry.getValue());
            setShiftPlanQty(targetEntry.getKey(), endingShiftIndex, newQty,
                    newQty > 0 ? endingShift.getShiftStartDateTime() : null, null);
        }
        for (LhScheduleResult result : sortedResults) {
            refreshResultSummary(context, result);
            refreshMachineStateAfterEndingStagger(context, result);
        }
        log.info("新增SKU收尾同班次尾量归集, materialCode: {}, shiftIndex: {}, scheduledBeforeShift: {}, "
                        + "endingDemandQty: {}, sameShiftCapacity: {}, primaryMachine: {}, before: {}, after: {}",
                sku.getMaterialCode(), endingShiftIndex, scheduledBeforeShift, endingDemandQty, sameShiftTotalCapacity,
                sortedResults.isEmpty() ? null : sortedResults.get(0).getLhMachineCode(),
                buildShiftQtySummary(originalShiftQtyMap, endingShiftIndex),
                buildShiftQtySummary(sortedResults, endingShiftIndex));
        return true;
    }

    private boolean releaseAuxiliaryMachineForNonEnding(LhScheduleContext context,
                                                        SkuScheduleDTO sku,
                                                        List<LhShiftConfigVO> shifts,
                                                        ProductionQuantityPolicy quantityPolicy,
                                                        List<LhScheduleResult> sameSkuResults) {
        if (context == null || sku == null || CollectionUtils.isEmpty(shifts)
                || CollectionUtils.isEmpty(sameSkuResults) || sameSkuResults.size() < 2
                || quantityPolicy == null || quantityPolicy.isStrictUpperLimit()) {
            return false;
        }
        Map<LocalDate, List<LhShiftConfigVO>> shiftMapByDate = groupShiftsByWorkDate(shifts);
        if (CollectionUtils.isEmpty(shiftMapByDate)) {
            return false;
        }
        List<LhScheduleResult> sortedResults = new ArrayList<LhScheduleResult>(sameSkuResults);
        sortedResults.sort(buildSameSkuPrimaryComparator(0));
        LhScheduleResult primaryResult = sortedResults.get(0);
        Set<String> releasedMachineCodes = new LinkedHashSet<String>(4);
        Set<String> protectedNightShiftKeySet = new HashSet<String>(4);
        boolean changed = false;
        int carryShortage = 0;
        for (Map.Entry<LocalDate, List<LhShiftConfigVO>> entry : shiftMapByDate.entrySet()) {
            LocalDate productionDate = entry.getKey();
            int requiredQty = Math.max(0, carryShortage + resolveDayPlanQty(sku, productionDate));
            int actualQty = 0;
            for (LhShiftConfigVO shift : entry.getValue()) {
                List<LhScheduleResult> shiftResults = resolveShiftPlannedResults(sortedResults, shift.getShiftIndex());
                if (CollectionUtils.isEmpty(shiftResults)) {
                    continue;
                }
                shiftResults.sort(buildSameSkuPrimaryComparator(shift.getShiftIndex()));
                for (LhScheduleResult result : shiftResults) {
                    Integer shiftQty = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
                    if (shiftQty == null || shiftQty <= 0) {
                        continue;
                    }
                    String machineShiftKey = buildMachineShiftKey(result.getLhMachineCode(), shift.getShiftIndex());
                    if (protectedNightShiftKeySet.contains(machineShiftKey)) {
                        actualQty += shiftQty;
                        log.info("新增SKU辅助机台晚班保留, materialCode: {}, productionDate: {}, shiftIndex: {}, "
                                        + "machine: {}, reason: 中班结束后进入晚班不可换模，不能在同轮释放中清掉已保留晚班",
                                sku.getMaterialCode(), productionDate, shift.getShiftIndex(),
                                result.getLhMachineCode());
                        continue;
                    }
                    boolean primaryMachine = result == primaryResult;
                    boolean necessary = primaryMachine || actualQty < requiredQty;
                    if (!necessary) {
                        if (shouldKeepAuxiliaryShiftForFutureDayDemand(
                                context, sku, shifts, sameSkuResults, result, productionDate)) {
                            actualQty += shiftQty;
                            log.info("新增SKU辅助机台保留, materialCode: {}, productionDate: {}, shiftIndex: {}, "
                                            + "machine: {}, reason: 后续dayN目标仍需当前辅机承接",
                                    sku.getMaterialCode(), productionDate, shift.getShiftIndex(),
                                    result.getLhMachineCode());
                            continue;
                        }
                        boolean nightShiftProtected = applyNightNoMouldChangeContinuationFill(
                                context, sku, result, shifts, quantityPolicy, shift.getShiftIndex());
                        if (nightShiftProtected) {
                            protectedNightShiftKeySet.add(buildMachineShiftKey(
                                    result.getLhMachineCode(), shift.getShiftIndex() + 1));
                        }
                        setShiftPlanQty(result, shift.getShiftIndex(), 0, null, null);
                        refreshResultSummary(context, result);
                        refreshMachineStateAfterEndingStagger(context, result);
                        releasedMachineCodes.add(result.getLhMachineCode());
                        changed = true;
                        log.info("新增SKU辅助机台释放, materialCode: {}, productionDate: {}, shiftIndex: {}, "
                                        + "primaryMachine: {}, releasedMachine: {}, carryShortage: {}, dayPlanQty: {}, "
                                        + "requiredQty: {}, actualQtyBeforeRelease: {}, reason: 当前日目标量+欠产已满足",
                                sku.getMaterialCode(), productionDate, shift.getShiftIndex(), primaryResult.getLhMachineCode(),
                                result.getLhMachineCode(), carryShortage, resolveDayPlanQty(sku, productionDate),
                                requiredQty, actualQty);
                        if (nightShiftProtected) {
                            log.info("新增SKU辅助机台晚班保留, materialCode: {}, releasedMachine: {}, shiftIndex: {}, "
                                            + "reason: 中班结束后进入晚班不可换模，当前SKU继续无换模生产",
                                    sku.getMaterialCode(), result.getLhMachineCode(), shift.getShiftIndex() + 1);
                        }
                        continue;
                    }
                    actualQty += shiftQty;
                }
            }
            carryShortage = Math.max(0, requiredQty - actualQty);
        }
        if (changed) {
            log.info("新增SKU非收尾辅助机台释放汇总, materialCode: {}, primaryMachine: {}, releasedMachines: {}, after: {}",
                    sku.getMaterialCode(), primaryResult.getLhMachineCode(),
                    StringUtils.join(releasedMachineCodes, ","), buildSameSkuAllocationSummary(sortedResults));
        }
        return changed;
    }

    /**
     * 构造机台班次保护键，避免辅助机台释放遍历中把中班后保留的晚班再次清掉。
     *
     * @param machineCode 机台编码
     * @param shiftIndex 班次序号
     * @return 机台班次保护键
     */
    private String buildMachineShiftKey(String machineCode, Integer shiftIndex) {
        return machineCode + "#" + shiftIndex;
    }

    private boolean shouldKeepAuxiliaryShiftForFutureDayDemand(LhScheduleContext context,
                                                               SkuScheduleDTO sku,
                                                               List<LhShiftConfigVO> shifts,
                                                               List<LhScheduleResult> sameSkuResults,
                                                               LhScheduleResult currentResult,
                                                               LocalDate productionDate) {
        if (sku == null || currentResult == null || productionDate == null
                || CollectionUtils.isEmpty(shifts) || CollectionUtils.isEmpty(sameSkuResults)) {
            return false;
        }
        LocalDate nextPlannedWorkDate = resolveNextPositivePlanDate(sku, productionDate);
        if (nextPlannedWorkDate == null) {
            nextPlannedWorkDate = resolveNextPlannedWorkDate(currentResult, shifts, productionDate);
        }
        if (nextPlannedWorkDate == null) {
            nextPlannedWorkDate = productionDate.plusDays(1);
        }
        if (!this.lhDailyMouldCalcService.hasRequiredMachineCount(
                context, sku.getMaterialCode(), sku.getProductStatus(), nextPlannedWorkDate)) {
            return false;
        }
        int targetMachineCount = this.lhDailyMouldCalcService.getRequiredMachineCount(
                context, sku.getMaterialCode(), sku.getProductStatus(), nextPlannedWorkDate);
        int machineCountWithoutCurrent = countDistinctSameSkuMachinesExcludingResult(
                sameSkuResults, currentResult);
        return machineCountWithoutCurrent < targetMachineCount;
    }

    /**
     * 判断窗口末日辅助机台是否需要因 T+3 日计划继续保留。
     * <p>T+3 不进入 T～T+2 实际扣账账本，但同 SKU 多机台收口不能把 dayN 模拟保留下来的机台班次清掉。</p>
     *
     * @param sku 当前 SKU
     * @param shifts 排程窗口班次
     * @param sameSkuResults 同 SKU 结果
     * @param currentResult 当前辅助机台结果
     * @param productionDate 当前生产日
     * @return true-保留当前辅助机台；false-允许按原逻辑释放
     */
    private boolean shouldKeepAuxiliaryShiftForWindowNextDayDemand(LhScheduleContext context,
                                                                   SkuScheduleDTO sku,
                                                                   List<LhShiftConfigVO> shifts,
                                                                   List<LhScheduleResult> sameSkuResults,
                                                                   LhScheduleResult currentResult,
                                                                   LocalDate productionDate) {
        if (sku == null || currentResult == null || productionDate == null
                || CollectionUtils.isEmpty(shifts) || CollectionUtils.isEmpty(sameSkuResults)
                || Math.max(0, sku.getNextDayPlanQtyAfterWindow()) <= 0
                || Math.max(0, sku.getShiftCapacity()) <= 0) {
            return false;
        }
        Map<LocalDate, List<LhShiftConfigVO>> shiftMapByDate = groupShiftsByWorkDate(shifts);
        if (CollectionUtils.isEmpty(shiftMapByDate) || !productionDate.equals(resolveLastWindowWorkDate(shiftMapByDate))) {
            return false;
        }
        LocalDate nextProductionDate = productionDate.plusDays(1);
        if (!this.lhDailyMouldCalcService.hasRequiredMachineCount(
                context, sku.getMaterialCode(), sku.getProductStatus(), nextProductionDate)) {
            return false;
        }
        int requiredMachineCount = this.lhDailyMouldCalcService.getRequiredMachineCount(
                context, sku.getMaterialCode(), sku.getProductStatus(), nextProductionDate);
        int machineCountWithoutCurrent = countDistinctSameSkuMachinesExcludingResult(sameSkuResults, currentResult);
        boolean keep = machineCountWithoutCurrent < requiredMachineCount;
        if (keep) {
            log.info("新增SKU辅助机台保留, materialCode: {}, productionDate: {}, machine: {}, "
                            + "reason: T+3日计划仍需当前辅机承接, nextDayPlanQty: {}, requiredMachineCount: {}, "
                            + "machineCountWithoutCurrent: {}",
                    sku.getMaterialCode(), productionDate, currentResult.getLhMachineCode(),
                    Math.max(0, sku.getNextDayPlanQtyAfterWindow()), requiredMachineCount, machineCountWithoutCurrent);
        }
        return keep;
    }

    /**
     * 解析排程窗口最后一个业务日。
     *
     * @param shiftMapByDate 按业务日分组的班次
     * @return 最后一个业务日
     */
    private LocalDate resolveLastWindowWorkDate(Map<LocalDate, List<LhShiftConfigVO>> shiftMapByDate) {
        LocalDate lastDate = null;
        for (LocalDate productionDate : shiftMapByDate.keySet()) {
            if (productionDate != null && (lastDate == null || productionDate.isAfter(lastDate))) {
                lastDate = productionDate;
            }
        }
        return lastDate;
    }

    /**
     * 统计有效排产班次数。
     *
     * @param shifts 业务日班次
     * @return 有效班次数
     */
    private int resolveSchedulableShiftCount(List<LhShiftConfigVO> shifts) {
        if (CollectionUtils.isEmpty(shifts)) {
            return 0;
        }
        int count = 0;
        for (LhShiftConfigVO shift : shifts) {
            if (shift != null && shift.getShiftIndex() != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * 统计排除当前结果后的同 SKU 机台数。
     *
     * @param sameSkuResults 同 SKU 结果
     * @param excludedResult 当前结果
     * @return 去重机台数
     */
    private int countDistinctSameSkuMachinesExcludingResult(List<LhScheduleResult> sameSkuResults,
                                                            LhScheduleResult excludedResult) {
        if (CollectionUtils.isEmpty(sameSkuResults)) {
            return 0;
        }
        Set<String> machineCodeSet = new HashSet<String>(sameSkuResults.size());
        for (LhScheduleResult result : sameSkuResults) {
            if (result == null || result == excludedResult || StringUtils.isEmpty(result.getLhMachineCode())) {
                continue;
            }
            machineCodeSet.add(result.getLhMachineCode());
        }
        return machineCodeSet.size();
    }

    /**
     * 解析当前生产日之后仍有日计划的最近业务日。
     * <p>新增非收尾增机台可能在 T+1 提前借用 T+2 计划；辅助机台释放时必须按后续 dayN 需求判断，
     * 不能只看辅助机台自身是否已经落到后续日期，否则会把提前承接未来计划的第二台清零。</p>
     *
     * @param sku 当前 SKU
     * @param productionDate 当前生产日
     * @return 后续有计划的最近业务日
     */
    private LocalDate resolveNextPositivePlanDate(SkuScheduleDTO sku, LocalDate productionDate) {
        if (sku == null || productionDate == null || CollectionUtils.isEmpty(sku.getDailyPlanQuotaMap())) {
            return null;
        }
        for (Map.Entry<LocalDate, SkuDailyPlanQuotaDTO> entry : sku.getDailyPlanQuotaMap().entrySet()) {
            LocalDate quotaDate = entry.getKey();
            SkuDailyPlanQuotaDTO quota = entry.getValue();
            if (quotaDate == null || !quotaDate.isAfter(productionDate) || quota == null) {
                continue;
            }
            if (Math.max(0, quota.getDayPlanQty()) > 0 || Math.max(0, quota.getRemainingQty()) > 0) {
                return quotaDate;
            }
        }
        return null;
    }

    private LocalDate resolveNextPlannedWorkDate(LhScheduleResult result,
                                                 List<LhShiftConfigVO> shifts,
                                                 LocalDate currentWorkDate) {
        if (result == null || currentWorkDate == null || CollectionUtils.isEmpty(shifts)) {
            return null;
        }
        LocalDate nextWorkDate = null;
        for (LhShiftConfigVO shift : shifts) {
            LocalDate shiftWorkDate = resolveShiftWorkDate(shift);
            if (shiftWorkDate == null || !shiftWorkDate.isAfter(currentWorkDate)
                    || shift.getShiftIndex() == null) {
                continue;
            }
            Integer shiftQty = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
            if (shiftQty == null || shiftQty <= 0) {
                continue;
            }
            if (nextWorkDate == null || shiftWorkDate.isBefore(nextWorkDate)) {
                nextWorkDate = shiftWorkDate;
            }
        }
        return nextWorkDate;
    }

    /**
     * 同SKU多机台机台收尾时，针对早班/中班同班次收尾做尾量错开。
     * <p>这里处理的是机台尾量，不改变SKU收尾判断；晚班不调整，避免破坏“晚班不能换模”下的有效产能。</p>
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param shifts 排程窗口班次
     * @return true-已调整；false-未调整
     */
    private boolean adjustSameSkuMultiMachineEndingStagger(LhScheduleContext context,
                                                           SkuScheduleDTO sku,
                                                           List<LhShiftConfigVO> shifts) {
        if (context == null || sku == null || CollectionUtils.isEmpty(shifts)
                || CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return false;
        }
        // 双模组不能作为普通 donor/receiver 单侧转移尾量，仅保留可独立处理的普通机台或单模结果。
        List<LhScheduleResult> sameSkuEndingResults = resolveIndependentPostProcessResults(
                context, sku, collectSameSkuNewSpecResults(context, sku, null));
        if (sameSkuEndingResults.size() < 2) {
            return false;
        }
        Map<Integer, List<LhScheduleResult>> endingShiftResultMap = new LinkedHashMap<>(4);
        for (LhScheduleResult result : sameSkuEndingResults) {
            int lastShiftIndex = resolveLastPlannedShiftIndex(result);
            if (lastShiftIndex <= 0) {
                continue;
            }
            endingShiftResultMap.computeIfAbsent(lastShiftIndex, key -> new ArrayList<LhScheduleResult>(2))
                    .add(result);
        }
        boolean adjusted = false;
        for (Map.Entry<Integer, List<LhScheduleResult>> entry : endingShiftResultMap.entrySet()) {
            if (entry.getValue().size() >= 2
                    && tryStaggerSameShiftEnding(context, sku, shifts, entry.getKey(), entry.getValue())) {
                adjusted = true;
            }
        }
        return adjusted;
    }

    private boolean tryStaggerSameShiftEnding(LhScheduleContext context,
                                              SkuScheduleDTO sku,
                                              List<LhShiftConfigVO> shifts,
                                              int endingShiftIndex,
                                              List<LhScheduleResult> results) {
        LhShiftConfigVO endingShift = findShiftByIndex(shifts, endingShiftIndex);
        LhShiftConfigVO nextShift = findShiftByIndex(shifts, endingShiftIndex + 1);
        if (endingShift == null || nextShift == null) {
            return false;
        }
        boolean nightShift = StringUtils.equals(ShiftEnum.NIGHT_SHIFT.getCode(), endingShift.getShiftType());
        log.info("同SKU多机台机台收尾错峰判断, materialCode: {}, 收尾班次: {}, 是否晚班: {}, 是否同SKU多机台收尾: 1",
                sku.getMaterialCode(), endingShiftIndex, nightShift);
        if (nightShift) {
            log.info("同SKU多机台机台收尾错峰跳过, materialCode: {}, 收尾班次: {}, 原因: 晚班不可换模不强制错峰",
                    sku.getMaterialCode(), endingShiftIndex);
            return false;
        }
        if (!isSameWorkDate(endingShift.getWorkDate(), nextShift.getWorkDate())) {
            return false;
        }
        List<LhScheduleResult> sortedResults = new ArrayList<LhScheduleResult>(results);
        sortedResults.sort(buildSameSkuPrimaryComparator(endingShiftIndex));
        LhScheduleResult donor = resolveTailDonorResult(sortedResults, endingShiftIndex);
        LhScheduleResult receiver = resolveTailReceiverResult(context, sku, sortedResults, donor, nextShift);
        if (donor == null || receiver == null) {
            return false;
        }
        Integer donorQty = ShiftFieldUtil.getShiftPlanQty(donor, endingShiftIndex);
        if (donorQty == null || donorQty <= 0) {
            return false;
        }
        int donorShiftCapacity = resolveAvailableShiftQtyForEndingStagger(context, donor, endingShift);
        if (donorShiftCapacity > 0 && donorQty >= donorShiftCapacity) {
            log.info("同SKU多机台机台收尾错峰跳过, materialCode: {}, 释放机台: {}, 收尾班次: {}, "
                            + "原因: 当前班次为满班产量，不属于可释放尾量",
                    sku.getMaterialCode(), donor.getLhMachineCode(), endingShiftIndex);
            return false;
        }
        int nextShiftCapacity = resolveAvailableShiftQtyForEndingStagger(context, receiver, nextShift);
        if (nextShiftCapacity <= 0 || donorQty > nextShiftCapacity) {
            log.info("同SKU多机台机台收尾错峰跳过, materialCode: {}, 承接机台: {}, 需转移: {}, 可用: {}",
                    sku.getMaterialCode(), receiver.getLhMachineCode(), donorQty, nextShiftCapacity);
            return false;
        }
        if (!canIncreaseShiftQtyByClassTotalLimit(context, sku, receiver, nextShift.getShiftIndex(), donorQty,
                "同SKU多机台机台收尾尾量错开")) {
            return false;
        }
        setShiftPlanQty(donor, endingShiftIndex, 0, null, null);
        Integer receiverExistingQty = ShiftFieldUtil.getShiftPlanQty(receiver, nextShift.getShiftIndex());
        int receiverQty = Math.max(0, receiverExistingQty == null ? 0 : receiverExistingQty) + donorQty;
        setShiftPlanQty(receiver, nextShift.getShiftIndex(), receiverQty, nextShift.getShiftStartDateTime(), null);
        refreshResultSummary(context, donor);
        refreshResultSummary(context, receiver);
        refreshMachineStateAfterEndingStagger(context, donor);
        refreshMachineStateAfterEndingStagger(context, receiver);
        log.info("同SKU多机台机台收尾尾量错开, materialCode: {}, 释放机台: {}, 承接机台: {}, "
                        + "原收尾班次: {}, 承接班次: {}, 转移数量: {}",
                sku.getMaterialCode(), donor.getLhMachineCode(), receiver.getLhMachineCode(),
                endingShiftIndex, nextShift.getShiftIndex(), donorQty);
        return true;
    }

    private List<LhScheduleResult> collectSameSkuNewSpecResults(LhScheduleContext context,
                                                                SkuScheduleDTO sku,
                                                                LhScheduleResult currentResult) {
        List<LhScheduleResult> sameSkuResults = new ArrayList<LhScheduleResult>(4);
        if (context == null || sku == null || StringUtils.isEmpty(sku.getMaterialCode())) {
            return sameSkuResults;
        }
        if (!CollectionUtils.isEmpty(context.getScheduleResultList())) {
            for (LhScheduleResult result : context.getScheduleResultList()) {
                if (result == null
                        || !NEW_SPEC_SCHEDULE_TYPE.equals(result.getScheduleType())
                        || "1".equals(result.getIsTypeBlock())
                        || !StringUtils.equals(sku.getMaterialCode(), result.getMaterialCode())
                        || !StringUtils.equals(StringUtils.trimToEmpty(sku.getProductStatus()),
                        StringUtils.trimToEmpty(result.getProductStatus()))
                        || ShiftFieldUtil.resolveScheduledQty(result) <= 0) {
                    continue;
                }
                sameSkuResults.add(result);
            }
        }
        if (currentResult != null
                && NEW_SPEC_SCHEDULE_TYPE.equals(currentResult.getScheduleType())
                && !"1".equals(currentResult.getIsTypeBlock())
                && StringUtils.equals(sku.getMaterialCode(), currentResult.getMaterialCode())
                && StringUtils.equals(StringUtils.trimToEmpty(sku.getProductStatus()),
                StringUtils.trimToEmpty(currentResult.getProductStatus()))
                && !sameSkuResults.contains(currentResult)) {
            sameSkuResults.add(currentResult);
        }
        return sameSkuResults;
    }

    /**
     * 过滤只能按物理组处理的双模 L/R 结果，供普通单机释放和错峰逻辑使用。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param results 同SKU结果
     * @return 可按独立机台处理的结果
     */
    private List<LhScheduleResult> resolveIndependentPostProcessResults(LhScheduleContext context,
                                                                         SkuScheduleDTO sku,
                                                                         List<LhScheduleResult> results) {
        List<LhScheduleResult> independentResults = new ArrayList<LhScheduleResult>(
                CollectionUtils.isEmpty(results) ? 0 : results.size());
        if (CollectionUtils.isEmpty(results)) {
            return independentResults;
        }
        for (LhScheduleResult result : results) {
            if (!isWholeSingleControlResult(context, sku, result)) {
                independentResults.add(result);
            }
        }
        return independentResults;
    }

    /**
     * 判断结果是否属于当前SKU冻结的双模单控物理组。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param result 排程结果
     * @return true-必须与配对侧同步处理
     */
    private boolean isWholeSingleControlResult(LhScheduleContext context,
                                               SkuScheduleDTO sku,
                                               LhScheduleResult result) {
        return Objects.nonNull(context)
                && Objects.nonNull(sku)
                && Objects.nonNull(result)
                && LhSingleControlMachineUtil.isWholeMachineGranularitySku(context, sku)
                && LhSingleControlMachineUtil.isConfiguredSingleControlMachine(
                context, result.getLhMachineCode());
    }

    /**
     * 从同一批结果中查找当前单控侧的配对侧结果。
     *
     * @param results 同SKU结果
     * @param currentResult 当前侧结果
     * @return 配对侧结果；不存在时返回null
     */
    private LhScheduleResult findPairResult(List<LhScheduleResult> results,
                                            LhScheduleResult currentResult) {
        if (CollectionUtils.isEmpty(results) || Objects.isNull(currentResult)) {
            return null;
        }
        String pairMachineCode = LhSingleControlMachineUtil.resolvePairMachineCode(
                currentResult.getLhMachineCode());
        for (LhScheduleResult candidate : results) {
            if (candidate != currentResult
                    && Objects.nonNull(candidate)
                    && StringUtils.equals(pairMachineCode, candidate.getLhMachineCode())
                    && StringUtils.equals(currentResult.getMaterialCode(), candidate.getMaterialCode())) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * 计算双模物理组在指定班次可承接的两侧合计量。
     *
     * @param context 排程上下文
     * @param primaryResult 主侧结果
     * @param pairResult 配对侧结果
     * @param shift 当前班次
     * @return L/R两侧合计可排量
     */
    private int resolveWholeSingleControlShiftCapacity(LhScheduleContext context,
                                                       LhScheduleResult primaryResult,
                                                       LhScheduleResult pairResult,
                                                       LhShiftConfigVO shift) {
        int primaryCapacity = Math.max(resolveResultBaseShiftCapacity(primaryResult),
                resolveAvailableShiftQtyForEndingStagger(context, primaryResult, shift));
        int pairCapacity = Math.max(resolveResultBaseShiftCapacity(pairResult),
                resolveAvailableShiftQtyForEndingStagger(context, pairResult, shift));
        return Math.max(0, primaryCapacity) + Math.max(0, pairCapacity);
    }

    private Comparator<LhScheduleResult> buildSameSkuPrimaryComparator(int shiftIndex) {
        return Comparator
                .comparingInt((LhScheduleResult result) -> {
                    int firstShiftIndex = resolveFirstPlannedShiftIndex(result);
                    return firstShiftIndex > 0 ? firstShiftIndex : Integer.MAX_VALUE;
                })
                .thenComparing((LhScheduleResult left, LhScheduleResult right) ->
                        Integer.compare(resolveResultScheduledQty(right), resolveResultScheduledQty(left)))
                .thenComparing((LhScheduleResult left, LhScheduleResult right) ->
                        Integer.compare(resolveScheduledQtyBeforeShift(right, shiftIndex),
                                resolveScheduledQtyBeforeShift(left, shiftIndex)))
                .thenComparing(result -> StringUtils.defaultString(result.getLhMachineCode()));
    }

    private int resolveScheduledQtyBeforeShift(LhScheduleResult result, int shiftIndex) {
        if (result == null || shiftIndex <= 1) {
            return 0;
        }
        int scheduledQty = 0;
        for (int currentShiftIndex = 1; currentShiftIndex < shiftIndex; currentShiftIndex++) {
            Integer shiftQty = ShiftFieldUtil.getShiftPlanQty(result, currentShiftIndex);
            if (shiftQty != null && shiftQty > 0) {
                scheduledQty += shiftQty;
            }
        }
        return scheduledQty;
    }

    private int resolveSameSkuScheduledQtyBeforeShift(List<LhScheduleResult> results, int shiftIndex) {
        if (CollectionUtils.isEmpty(results) || shiftIndex <= 1) {
            return 0;
        }
        int totalQty = 0;
        for (LhScheduleResult result : results) {
            totalQty += resolveScheduledQtyBeforeShift(result, shiftIndex);
        }
        return totalQty;
    }

    private int resolveSameShiftAvailableCapacity(LhScheduleContext context,
                                                  List<LhScheduleResult> results,
                                                  LhShiftConfigVO shift) {
        if (CollectionUtils.isEmpty(results) || shift == null) {
            return 0;
        }
        int totalCapacity = 0;
        for (LhScheduleResult result : results) {
            Integer currentQty = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
            if (currentQty == null || currentQty <= 0) {
                continue;
            }
            int availableQty = resolveAvailableShiftQtyForEndingStagger(context, result, shift);
            int baseShiftCapacity = resolveResultBaseShiftCapacity(result);
            totalCapacity += Math.max(currentQty, Math.max(availableQty, baseShiftCapacity));
        }
        return totalCapacity;
    }

    private int resolveDayPlanQty(SkuScheduleDTO sku, LocalDate productionDate) {
        if (sku == null || productionDate == null || CollectionUtils.isEmpty(sku.getDailyPlanQuotaMap())) {
            return 0;
        }
        SkuDailyPlanQuotaDTO quota = sku.getDailyPlanQuotaMap().get(productionDate);
        return quota == null ? 0 : Math.max(0, quota.getDayPlanQty());
    }

    private Map<LocalDate, List<LhShiftConfigVO>> groupShiftsByWorkDate(List<LhShiftConfigVO> shifts) {
        Map<LocalDate, List<LhShiftConfigVO>> shiftMap = new LinkedHashMap<LocalDate, List<LhShiftConfigVO>>(4);
        if (CollectionUtils.isEmpty(shifts)) {
            return shiftMap;
        }
        for (LhShiftConfigVO shift : shifts) {
            LocalDate workDate = resolveShiftWorkDate(shift);
            if (workDate == null) {
                continue;
            }
            shiftMap.computeIfAbsent(workDate, key -> new ArrayList<LhShiftConfigVO>(4)).add(shift);
        }
        return shiftMap;
    }

    private LocalDate resolveShiftWorkDate(LhShiftConfigVO shift) {
        if (shift == null || shift.getWorkDate() == null) {
            return null;
        }
        return shift.getWorkDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private List<LhScheduleResult> resolveShiftPlannedResults(List<LhScheduleResult> results, Integer shiftIndex) {
        List<LhScheduleResult> shiftResults = new ArrayList<LhScheduleResult>(4);
        if (CollectionUtils.isEmpty(results) || shiftIndex == null) {
            return shiftResults;
        }
        for (LhScheduleResult result : results) {
            Integer shiftQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
            if (shiftQty != null && shiftQty > 0) {
                shiftResults.add(result);
            }
        }
        return shiftResults;
    }

    private String buildSameSkuAllocationSummary(List<LhScheduleResult> results) {
        if (CollectionUtils.isEmpty(results)) {
            return "-";
        }
        List<String> machineSummaryList = new ArrayList<String>(results.size());
        List<LhScheduleResult> sortedResults = new ArrayList<LhScheduleResult>(results);
        sortedResults.sort(Comparator.comparing(result -> StringUtils.defaultString(result.getLhMachineCode())));
        for (LhScheduleResult result : sortedResults) {
            machineSummaryList.add(buildMachineShiftSummary(result));
        }
        return StringUtils.join(machineSummaryList, "; ");
    }

    private String buildMachineShiftSummary(LhScheduleResult result) {
        if (result == null) {
            return "-";
        }
        StringBuilder builder = new StringBuilder(128);
        builder.append(StringUtils.defaultString(result.getLhMachineCode())).append("[");
        for (int shiftIndex = 1; shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
            if (shiftIndex > 1) {
                builder.append(",");
            }
            Integer shiftQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
            builder.append("C").append(shiftIndex).append("=")
                    .append(shiftQty == null ? 0 : Math.max(0, shiftQty));
        }
        builder.append("]");
        return builder.toString();
    }

    private String buildShiftQtySummary(Map<LhScheduleResult, Integer> shiftQtyMap, int shiftIndex) {
        if (CollectionUtils.isEmpty(shiftQtyMap)) {
            return "-";
        }
        List<String> summaryList = new ArrayList<String>(shiftQtyMap.size());
        for (Map.Entry<LhScheduleResult, Integer> entry : shiftQtyMap.entrySet()) {
            summaryList.add(StringUtils.defaultString(entry.getKey().getLhMachineCode())
                    + "[C" + shiftIndex + "=" + Math.max(0, entry.getValue()) + "]");
        }
        return StringUtils.join(summaryList, "; ");
    }

    private String buildShiftQtySummary(List<LhScheduleResult> results, int shiftIndex) {
        if (CollectionUtils.isEmpty(results)) {
            return "-";
        }
        Map<LhScheduleResult, Integer> shiftQtyMap = new LinkedHashMap<LhScheduleResult, Integer>(results.size());
        for (LhScheduleResult result : results) {
            Integer shiftQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
            shiftQtyMap.put(result, shiftQty == null ? 0 : shiftQty);
        }
        return buildShiftQtySummary(shiftQtyMap, shiftIndex);
    }

    private String oneZero(boolean value) {
        return value ? "1" : "0";
    }

    private LhScheduleResult resolveTailDonorResult(List<LhScheduleResult> results, int endingShiftIndex) {
        if (CollectionUtils.isEmpty(results)) {
            return null;
        }
        List<LhScheduleResult> sortedResults = new ArrayList<LhScheduleResult>(results);
        sortedResults.sort(buildSameSkuPrimaryComparator(endingShiftIndex).reversed());
        for (LhScheduleResult result : sortedResults) {
            Integer qty = ShiftFieldUtil.getShiftPlanQty(result, endingShiftIndex);
            if (qty != null && qty > 0) {
                return result;
            }
        }
        return null;
    }

    private LhScheduleResult resolveTailReceiverResult(LhScheduleContext context,
                                                       SkuScheduleDTO sku,
                                                       List<LhScheduleResult> results,
                                                       LhScheduleResult donor,
                                                       LhShiftConfigVO nextShift) {
        if (CollectionUtils.isEmpty(results) || nextShift == null) {
            return null;
        }
        List<LhScheduleResult> sortedResults = new ArrayList<LhScheduleResult>(results);
        sortedResults.sort(buildSameSkuPrimaryComparator(nextShift.getShiftIndex()));
        for (LhScheduleResult result : sortedResults) {
            if (result == null || result == donor) {
                continue;
            }
            if (isMachineShiftOccupiedByOtherSku(context, sku, result, nextShift)) {
                continue;
            }
            Integer nextShiftQty = ShiftFieldUtil.getShiftPlanQty(result, nextShift.getShiftIndex());
            if (nextShiftQty != null && nextShiftQty > 0) {
                return result;
            }
        }
        for (LhScheduleResult result : sortedResults) {
            if (result == null || result == donor) {
                continue;
            }
            if (!isMachineShiftOccupiedByOtherSku(context, sku, result, nextShift)) {
                return result;
            }
        }
        return null;
    }

    /**
     * 判断两个班次是否归属同一业务日。
     *
     * @param firstWorkDate 第一个班次业务日
     * @param secondWorkDate 第二个班次业务日
     * @return true-同一业务日；false-不同业务日
     */
    private boolean isSameWorkDate(Date firstWorkDate, Date secondWorkDate) {
        if (firstWorkDate == null || secondWorkDate == null) {
            return false;
        }
        LocalDate firstDate = firstWorkDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate secondDate = secondWorkDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return firstDate.equals(secondDate);
    }

    private int resolveAvailableShiftQtyForEndingStagger(LhScheduleContext context,
                                                         LhScheduleResult result,
                                                         LhShiftConfigVO targetShift) {
        if (context == null || result == null || targetShift == null
                || StringUtils.isEmpty(result.getLhMachineCode())
                || result.getLhTime() == null || result.getLhTime() <= 0
                || result.getMouldQty() == null || result.getMouldQty() <= 0) {
            return 0;
        }
        Date shiftStartTime = targetShift.getShiftStartDateTime();
        Date shiftEndTime = targetShift.getShiftEndDateTime();
        if (shiftStartTime == null || shiftEndTime == null || !shiftStartTime.before(shiftEndTime)) {
            return 0;
        }
        ShiftProductionControlDTO control = ShiftProductionControlUtil.resolveEffectiveControl(
                context, targetShift, shiftStartTime);
        if (control == null || !control.isCanSchedule()) {
            return 0;
        }
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(result.getLhMachineCode());
        int baseShiftCapacity = resolveResultBaseShiftCapacity(result);
        int runtimeShiftCapacity = machine == null ? 0 : ShiftCapacityResolverUtil.resolveRuntimeShiftCapacity(
                context, machine, baseShiftCapacity);
        int shiftCapacity = runtimeShiftCapacity > 0 ? runtimeShiftCapacity : baseShiftCapacity;
        if (shiftCapacity <= 0) {
            return 0;
        }
        int dryIceLossQty = context.getParamIntValue(
                LhScheduleParamConstant.DRY_ICE_LOSS_QTY, LhScheduleConstant.DRY_ICE_LOSS_QTY);
        int dryIceDurationHours = context.getParamIntValue(
                LhScheduleParamConstant.DRY_ICE_DURATION_HOURS, LhScheduleConstant.DRY_ICE_DURATION_HOURS);
        int plannedRepairFixedQty = context.getParamIntValue(
                LhScheduleParamConstant.PLANNED_REPAIR_FIXED_QTY, LhScheduleConstant.PLANNED_REPAIR_FIXED_QTY);
        int shiftMaxQty = ShiftCapacityResolverUtil.resolveShiftCapacityWithDowntime(
                context.getDevicePlanShutList(),
                resolveMachineCleaningWindowList(context, result.getLhMachineCode()),
                resolveMachineMaintenanceWindowList(context, result.getLhMachineCode()),
                result.getLhMachineCode(),
                control.getEffectiveStartTime(),
                control.getEffectiveEndTime(),
                shiftCapacity,
                result.getLhTime(),
                result.getMouldQty(),
                ShiftCapacityResolverUtil.resolveShiftDurationSeconds(targetShift),
                dryIceLossQty,
                dryIceDurationHours,
                targetShift,
                ShiftCapacityResolverUtil.resolveOddShiftCapacityPlusShiftType(context),
                ScheduleTypeEnum.NEW_SPEC.getCode(),
                plannedRepairFixedQty);
        int capacityBeforeCapsuleReplacement = Math.max(0,
                ShiftProductionControlUtil.deductCapacityByControl(
                        control, shiftMaxQty, result.getMouldQty()));
        // 收尾错峰及跨班补量必须保留正式落班时已发生的换胶囊固定产能损失。
        return capsuleReplacementRuleService.resolveReplacementShiftCapacityUpperLimit(
                context, result, targetShift, capacityBeforeCapsuleReplacement);
    }

    private int resolveLastPlannedShiftIndex(LhScheduleResult result) {
        for (int shiftIndex = LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex >= 1; shiftIndex--) {
            Integer qty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
            if (qty != null && qty > 0) {
                return shiftIndex;
            }
        }
        return -1;
    }

    private int resolveResultBaseShiftCapacity(LhScheduleResult result) {
        if (result == null) {
            return 0;
        }
        if (result.getSingleMouldShiftQty() != null && result.getSingleMouldShiftQty() > 0) {
            return result.getSingleMouldShiftQty();
        }
        int maxShiftQty = 0;
        for (int shiftIndex = 1; shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
            Integer shiftQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
            if (shiftQty != null && shiftQty > maxShiftQty) {
                maxShiftQty = shiftQty;
            }
        }
        return maxShiftQty;
    }

    /**
     * 晚班不可换模时，当前SKU在本机台无换模续作补下一晚班。
     * <p>非收尾SKU按可用晚班班产补满；收尾SKU只允许在剩余收尾目标量范围内补量，不能超排。</p>
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param result 当前机台结果
     * @param shifts 班次列表
     * @param quantityPolicy 数量控制策略
     * @return true-已保留或补充晚班；false-未命中规则
     */
    private boolean applyNightNoMouldChangeContinuationFill(LhScheduleContext context,
                                                            SkuScheduleDTO sku,
                                                            LhScheduleResult result,
                                                            List<LhShiftConfigVO> shifts,
                                                            ProductionQuantityPolicy quantityPolicy) {
        return applyNightNoMouldChangeContinuationFill(context, sku, result, shifts, quantityPolicy, null);
    }

    private boolean applyNightNoMouldChangeContinuationFill(LhScheduleContext context,
                                                            SkuScheduleDTO sku,
                                                            LhScheduleResult result,
                                                            List<LhShiftConfigVO> shifts,
                                                            ProductionQuantityPolicy quantityPolicy,
                                                            Integer releaseShiftIndex) {
        if (context == null || sku == null || result == null || CollectionUtils.isEmpty(shifts)
                || quantityPolicy == null) {
            return false;
        }
        boolean endingPolicy = quantityPolicy.isEnding();
        if (!endingPolicy && (!quantityPolicy.isAllowFillStartedShift() || quantityPolicy.isStrictUpperLimit())) {
            return false;
        }
        if ("1".equals(result.getIsEnd()) && !endingPolicy) {
            return false;
        }
        int lastShiftIndex = releaseShiftIndex == null ? resolveLastPlannedShiftIndex(result) : releaseShiftIndex;
        if (lastShiftIndex <= 0) {
            return false;
        }
        LhShiftConfigVO currentShift = findShiftByIndex(shifts, lastShiftIndex);
        LhShiftConfigVO nextShift = findShiftByIndex(shifts, lastShiftIndex + 1);
        if (!isAfternoonToNoMouldChangeNightShift(currentShift, nextShift)
                || nextShift.getShiftStartDateTime() == null
                || !LhScheduleTimeUtil.isNoMouldChangeTime(context, nextShift.getShiftStartDateTime())) {
            return false;
        }
        if (isMachineShiftOccupiedByOtherSku(context, sku, result, nextShift)) {
            log.info("晚班不可换模续作补满跳过, materialCode: {}, 机台: {}, 晚班班次: {}, 原因: 下一晚班已被其他SKU占用",
                    sku.getMaterialCode(), result.getLhMachineCode(), nextShift.getShiftIndex());
            return false;
        }
        boolean pendingResultBeforePersist = releaseShiftIndex == null
                && isNewSpecResultPendingPersist(context, result);
        if (releaseShiftIndex == null && !endingPolicy && !pendingResultBeforePersist
                && !isNightContinuationFillNecessary(context, sku, result, shifts, nextShift)) {
            log.info("晚班不可换模续作补满跳过, materialCode: {}, 机台: {}, 晚班班次: {}, 原因: 当前机台为辅助机台且主承接机台已可覆盖当日目标",
                    sku.getMaterialCode(), result.getLhMachineCode(), nextShift.getShiftIndex());
            return false;
        }
        int realSurplusRemainingQty = resolveRealSurplusRemainingQty(context, sku, result);
        if (realSurplusRemainingQty <= 0) {
            return false;
        }
        Integer currentShiftExistingQty = ShiftFieldUtil.getShiftPlanQty(result, currentShift.getShiftIndex());
        int currentShiftBeforeQty = currentShiftExistingQty == null ? 0 : Math.max(0, currentShiftExistingQty);
        int currentShiftAvailableQty = Math.max(0,
                resolveAvailableShiftQtyForEndingStagger(context, result, currentShift) - currentShiftBeforeQty);
        // 晚班不可换模衔接时，当前中班仍可生产的产能先补满，再承接下一晚班。
        int currentShiftFillQty = Math.min(currentShiftAvailableQty, realSurplusRemainingQty);
        int beforeFillRealSurplusRemainingQty = realSurplusRemainingQty;
        if (currentShiftFillQty > 0) {
            if (canIncreaseShiftQtyByClassTotalLimit(context, sku, result, currentShift.getShiftIndex(),
                    currentShiftFillQty, "晚班不可换模当前班次补量")) {
                // 当前班补量先执行换胶囊扣减，扣减差额继续保留在真实余量中供下一晚班排产。
                currentShiftFillQty = capsuleReplacementRuleService.resolveActualPlanQty(
                        context, result, currentShift, currentShiftFillQty,
                        currentShiftBeforeQty + currentShiftAvailableQty,
                        currentShift.getShiftStartDateTime(),
                        "新增排产不可换模当前班补量");
                Date currentShiftStartTime = ShiftFieldUtil.getShiftStartTime(result, currentShift.getShiftIndex());
                setShiftPlanQty(result, currentShift.getShiftIndex(), currentShiftBeforeQty + currentShiftFillQty,
                        currentShiftStartTime == null ? currentShift.getShiftStartDateTime() : currentShiftStartTime,
                        currentShift.getShiftEndDateTime());
                realSurplusRemainingQty = Math.max(0, realSurplusRemainingQty - currentShiftFillQty);
            } else {
                currentShiftFillQty = 0;
            }
        }
        Integer existingQty = ShiftFieldUtil.getShiftPlanQty(result, nextShift.getShiftIndex());
        int currentQty = existingQty == null ? 0 : Math.max(0, existingQty);
        int availableQty = Math.max(0, resolveAvailableShiftQtyForEndingStagger(context, result, nextShift) - currentQty);
        int fillQty = Math.min(availableQty, realSurplusRemainingQty);
        if (fillQty <= 0 && currentShiftFillQty <= 0) {
            return currentQty > 0 || currentShiftBeforeQty > 0;
        }
        if (fillQty > 0) {
            if (canIncreaseShiftQtyByClassTotalLimit(context, sku, result, nextShift.getShiftIndex(), fillQty,
                    "晚班不可换模晚班补量")) {
                // 下一晚班同样属于正式落班增量，实际余量只消费换胶囊扣减后的数量。
                fillQty = capsuleReplacementRuleService.resolveActualPlanQty(
                        context, result, nextShift, fillQty, currentQty + availableQty,
                        nextShift.getShiftStartDateTime(),
                        "新增排产不可换模晚班补量");
                setShiftPlanQty(result, nextShift.getShiftIndex(), currentQty + fillQty,
                        nextShift.getShiftStartDateTime(), null);
            } else {
                fillQty = 0;
            }
        }
        int nightShiftAfterQty = currentQty + fillQty;
        refreshResultSummary(context, result);
        log.info("晚班不可换模续作补满命中, materialCode: {}, 机台: {}, 当前收尾班次: {}, 晚班班次: {}, "
                        + "当前班次补前: {}, 当前班次补后: {}, 晚班补前: {}, 晚班补后: {}, "
                        + "补满数量: {}, 真实余量剩余: {}, 原因: 晚班不可换模且当前SKU可无换模续作",
                sku.getMaterialCode(), result.getLhMachineCode(), lastShiftIndex, nextShift.getShiftIndex(),
                currentShiftBeforeQty, currentShiftBeforeQty + currentShiftFillQty, currentQty, nightShiftAfterQty,
                currentShiftFillQty + fillQty, beforeFillRealSurplusRemainingQty);
        return true;
    }

    /**
     * 判断新增结果是否处于落地前状态。
     * <p>尾机台初始排到中班结束时，结果尚未加入上下文；此时下个晚班不可换模，不能套用已落地辅助机的主机覆盖跳过保护。</p>
     *
     * @param context 排程上下文
     * @param result 当前新增结果
     * @return true-新增结果尚未落地；false-结果已落地或无效
     */
    private boolean isNewSpecResultPendingPersist(LhScheduleContext context, LhScheduleResult result) {
        if (context == null || result == null
                || !NEW_SPEC_SCHEDULE_TYPE.equals(result.getScheduleType())
                || CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return false;
        }
        for (LhScheduleResult scheduleResult : context.getScheduleResultList()) {
            if (scheduleResult == result) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断当前结果是否从中班收尾进入不可换模晚班。
     * <p>中班结束后如果直接下机，后续SKU在晚班无法换模开产；当前SKU已在机，可继续无换模生产晚班。</p>
     *
     * @param currentShift 当前最后有量班次
     * @param nextShift 下一班次
     * @return true-中班后紧接不可换模晚班
     */
    private boolean isAfternoonToNoMouldChangeNightShift(LhShiftConfigVO currentShift,
                                                         LhShiftConfigVO nextShift) {
        return currentShift != null
                && nextShift != null
                && StringUtils.equals(ShiftEnum.AFTERNOON_SHIFT.getCode(), currentShift.getShiftType())
                && nextShift.isNightShift()
                && !nextShift.isAllowMouldChange();
    }

    private boolean isNightContinuationFillNecessary(LhScheduleContext context,
                                                     SkuScheduleDTO sku,
                                                     LhScheduleResult currentResult,
                                                     List<LhShiftConfigVO> shifts,
                                                     LhShiftConfigVO nextShift) {
        List<LhScheduleResult> sameSkuResults = collectSameSkuNewSpecResults(context, sku, currentResult);
        if (sameSkuResults.size() < 2) {
            return true;
        }
        List<LhScheduleResult> sortedResults = new ArrayList<LhScheduleResult>(sameSkuResults);
        sortedResults.sort(buildSameSkuPrimaryComparator(nextShift.getShiftIndex()));
        LhScheduleResult primaryResult = sortedResults.get(0);
        if (currentResult == primaryResult) {
            return true;
        }
        LocalDate productionDate = resolveShiftWorkDate(nextShift);
        if (productionDate == null) {
            return true;
        }
        int dayRequiredQty = resolveSameSkuRequiredQtyForDate(sku, shifts, sameSkuResults, productionDate);
        List<LhShiftConfigVO> sameDateShifts = groupShiftsByWorkDate(shifts).get(productionDate);
        int dayScheduledQty = resolveSameSkuScheduledQtyByShifts(sameSkuResults, sameDateShifts);
        return dayScheduledQty < dayRequiredQty;
    }

    /**
     * 计算当前SKU真实余量扣除已排后的剩余量。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param currentResult 当前结果
     * @return 剩余真实余量
     */
    private int resolveRealSurplusRemainingQty(LhScheduleContext context,
                                               SkuScheduleDTO sku,
                                               LhScheduleResult currentResult) {
        if (sku == null || sku.getSurplusQty() <= 0) {
            return 0;
        }
        int scheduledQty = ShiftFieldUtil.resolveScheduledQty(currentResult);
        if (context != null && !CollectionUtils.isEmpty(context.getScheduleResultList())) {
            for (LhScheduleResult result : context.getScheduleResultList()) {
                if (result == null || result == currentResult
                        || !StringUtils.equals(sku.getMaterialCode(), result.getMaterialCode())
                        || !StringUtils.equals(StringUtils.trimToEmpty(sku.getProductStatus()),
                        StringUtils.trimToEmpty(result.getProductStatus()))) {
                    continue;
                }
                scheduledQty += ShiftFieldUtil.resolveScheduledQty(result);
            }
        }
        return Math.max(0, sku.getSurplusQty() - scheduledQty);
    }

    private int resolveSameSkuRequiredQtyForDate(SkuScheduleDTO sku,
                                                 List<LhShiftConfigVO> shifts,
                                                 List<LhScheduleResult> sameSkuResults,
                                                 LocalDate targetDate) {
        Map<LocalDate, List<LhShiftConfigVO>> shiftMapByDate = groupShiftsByWorkDate(shifts);
        if (CollectionUtils.isEmpty(shiftMapByDate) || targetDate == null) {
            return 0;
        }
        int carryShortage = 0;
        int requiredQty = 0;
        for (Map.Entry<LocalDate, List<LhShiftConfigVO>> entry : shiftMapByDate.entrySet()) {
            LocalDate productionDate = entry.getKey();
            requiredQty = Math.max(0, carryShortage + resolveDayPlanQty(sku, productionDate));
            int actualQty = resolveSameSkuScheduledQtyByShifts(sameSkuResults, entry.getValue());
            if (productionDate.equals(targetDate)) {
                return requiredQty;
            }
            carryShortage = Math.max(0, requiredQty - actualQty);
        }
        return requiredQty;
    }

    private int resolveSameSkuScheduledQtyByShifts(List<LhScheduleResult> sameSkuResults,
                                                   List<LhShiftConfigVO> shifts) {
        if (CollectionUtils.isEmpty(sameSkuResults) || CollectionUtils.isEmpty(shifts)) {
            return 0;
        }
        int totalQty = 0;
        for (LhScheduleResult result : sameSkuResults) {
            for (LhShiftConfigVO shift : shifts) {
                if (shift == null || shift.getShiftIndex() == null) {
                    continue;
                }
                Integer shiftQty = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
                if (shiftQty != null && shiftQty > 0) {
                    totalQty += shiftQty;
                }
            }
        }
        return totalQty;
    }

    private int resolveSameSkuScheduledQtyByShiftsExcludingResult(List<LhScheduleResult> sameSkuResults,
                                                                  List<LhShiftConfigVO> shifts,
                                                                  LhScheduleResult excludedResult) {
        if (CollectionUtils.isEmpty(sameSkuResults) || CollectionUtils.isEmpty(shifts)) {
            return 0;
        }
        int totalQty = 0;
        for (LhScheduleResult result : sameSkuResults) {
            if (result == null || result == excludedResult) {
                continue;
            }
            for (LhShiftConfigVO shift : shifts) {
                if (shift == null || shift.getShiftIndex() == null) {
                    continue;
                }
                Integer shiftQty = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
                if (shiftQty != null && shiftQty > 0) {
                    totalQty += shiftQty;
                }
            }
        }
        return totalQty;
    }

    /**
     * 判断承接机台目标班次是否已被其他SKU占用。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param receiver 承接结果
     * @param targetShift 目标班次
     * @return true-其他SKU已占用
     */
    private boolean isMachineShiftOccupiedByOtherSku(LhScheduleContext context,
                                                     SkuScheduleDTO sku,
                                                     LhScheduleResult receiver,
                                                     LhShiftConfigVO targetShift) {
        if (context == null || sku == null || receiver == null || targetShift == null
                || StringUtils.isEmpty(receiver.getLhMachineCode())
                || targetShift.getShiftIndex() == null) {
            return false;
        }
        List<LhScheduleResult> machineResults = CollectionUtils.isEmpty(context.getMachineAssignmentMap())
                ? null : context.getMachineAssignmentMap().get(receiver.getLhMachineCode());
        if (isMachineShiftOccupiedByOtherSku(machineResults, sku, receiver, targetShift)) {
            return true;
        }
        return isMachineShiftOccupiedByOtherSku(context.getScheduleResultList(), sku, receiver, targetShift);
    }

    /**
     * 判断结果列表中是否存在同机台同班次其他SKU计划。
     *
     * @param results 结果列表
     * @param sku 当前SKU
     * @param receiver 承接结果
     * @param targetShift 目标班次
     * @return true-其他SKU已占用
     */
    private boolean isMachineShiftOccupiedByOtherSku(List<LhScheduleResult> results,
                                                     SkuScheduleDTO sku,
                                                     LhScheduleResult receiver,
                                                     LhShiftConfigVO targetShift) {
        if (CollectionUtils.isEmpty(results)) {
            return false;
        }
        for (LhScheduleResult result : results) {
            if (result == null || result == receiver
                    || !StringUtils.equals(receiver.getLhMachineCode(), result.getLhMachineCode())) {
                continue;
            }
            Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, targetShift.getShiftIndex());
            if (planQty == null || planQty <= 0) {
                continue;
            }
            if (!StringUtils.equals(sku.getMaterialCode(), result.getMaterialCode())) {
                return true;
            }
        }
        return false;
    }

    private LhShiftConfigVO findShiftByIndex(List<LhShiftConfigVO> shifts, int shiftIndex) {
        if (CollectionUtils.isEmpty(shifts)) {
            return null;
        }
        for (LhShiftConfigVO shift : shifts) {
            if (shift != null && shift.getShiftIndex() != null && shift.getShiftIndex() == shiftIndex) {
                return shift;
            }
        }
        return null;
    }

    private void refreshMachineStateAfterEndingStagger(LhScheduleContext context, LhScheduleResult result) {
        if (context == null || result == null || StringUtils.isEmpty(result.getLhMachineCode())) {
            return;
        }
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(result.getLhMachineCode());
        if (machine == null) {
            return;
        }
        List<LhScheduleResult> assignedResults = context.getMachineAssignmentMap().get(result.getLhMachineCode());
        LhScheduleResult latestResult = resolveLatestAssignedResult(context, assignedResults);
        if (latestResult != null) {
            LhScheduleResult previousResult = resolvePreviousAssignedResult(assignedResults, latestResult);
            applyMachineStateFromResult(context, machine, latestResult, previousResult);
            return;
        }
        restoreMachineStateFromInitial(context, result.getLhMachineCode(), machine);
    }

    /**
     * 获取首个有排产量的班次索引。
     *
     * @param result 排程结果
     * @return 班次索引；未找到返回 -1
     */
    private int resolveFirstPlannedShiftIndex(LhScheduleResult result) {
        if (result == null) {
            return -1;
        }
        for (int shiftIndex = 1; shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
            Integer shiftPlanQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
            if (shiftPlanQty != null && shiftPlanQty > 0) {
                return shiftIndex;
            }
        }
        return -1;
    }

    /**
     * 获取首个有排产量班次的开始时间。
     *
     * @param result 排程结果
     * @return 班次开始时间；未找到返回 null
     */
    private Date resolveFirstPlannedShiftStartTime(LhScheduleResult result) {
        int firstPlannedShiftIndex = resolveFirstPlannedShiftIndex(result);
        return firstPlannedShiftIndex > 0
                ? ShiftFieldUtil.getShiftStartTime(result, firstPlannedShiftIndex) : null;
    }

    /**
     * 基于最终计划量复核新增结果收尾标记。
     * <p>口径：仅新增结果生效；按物料编码汇总多机台排产量后，汇总计划量 >= max(硫化余量, 胎胚库存)时记为收尾。</p>
     * <p>多机台场景下，同一SKU在多台机台上的结果共享同一个收尾标记。</p>
     *
     * @param context 排程上下文
     */
    private void refreshNewSpecEndingFlagByResult(LhScheduleContext context) {
        if (context == null || CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return;
        }
        // 按物料状态复合键汇总新增结果，避免S/T/X共用收尾目标。
        Map<String, Integer> skuTotalPlanQtyMap = new LinkedHashMap<>(16);
        Map<String, SkuScheduleDTO> skuMap = new LinkedHashMap<>(16);
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (result == null || !NEW_SPEC_SCHEDULE_TYPE.equals(result.getScheduleType())) {
                continue;
            }
            String materialCode = result.getMaterialCode();
            if (StringUtils.isEmpty(materialCode)) {
                continue;
            }
            String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                    materialCode, result.getProductStatus());
            int planQty = resolveResultScheduledQty(result);
            skuTotalPlanQtyMap.merge(skuKey, planQty, Integer::sum);
            if (!skuMap.containsKey(skuKey)) {
                skuMap.put(skuKey, findSkuDto(context, materialCode, result.getProductStatus()));
            }
        }
        // 基于汇总计划量统一设置同物料所有结果的收尾标记
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (result == null || !NEW_SPEC_SCHEDULE_TYPE.equals(result.getScheduleType())
                    || StringUtils.isEmpty(result.getMaterialCode())) {
                continue;
            }
            String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                    result.getMaterialCode(), result.getProductStatus());
            int totalPlanQty = skuTotalPlanQtyMap.getOrDefault(skuKey, 0);
            SkuScheduleDTO sku = skuMap.get(skuKey);
            result.setIsEnd(endingJudgmentStrategy.isFinalEnding(context, sku, totalPlanQty) ? "1" : "0");
        }
    }

    /**
     * 计算结果行收尾比较量（从SKU DTO取全量值，避免多机台分摊后偏小）。
     * <p>仅收尾SKU才按共用胎胚规则（仅取硫化余量）；非收尾SKU继续按 MAX(余量, 胎胚库存)，
     * 避免共用胎胚导致非收尾SKU的 isEnd 被误翻转为 "1"。</p>
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @return 收尾比较量
     */
    private int resolveEndingDemandQty(LhScheduleContext context, LhScheduleResult result) {
        SkuScheduleDTO sku = findSkuDto(
                context, result.getMaterialCode(), result.getProductStatus());
        int surplusQty = sku != null ? Math.max(0, sku.getSurplusQty())
                : Math.max(0, result.getMouldSurplusQty() == null ? 0 : result.getMouldSurplusQty());
        int embryoStock = sku != null ? Math.max(0, sku.getEmbryoStock())
                : Math.max(0, result.getEmbryoStock() == null ? 0 : result.getEmbryoStock());
        // 仅收尾SKU才按共用胎胚规则（仅取硫化余量），非收尾SKU保持原口径
        if (sku != null
                && SkuTagEnum.ENDING.getCode().equals(sku.getSkuTag())
                && getTargetScheduleQtyResolver().isSharedEmbryoInWindow(context, sku)) {
            return surplusQty;
        }
        return Math.max(surplusQty, embryoStock);
    }

    private int resolveResultScheduledQty(LhScheduleResult result) {
        int scheduledQty = ShiftFieldUtil.resolveScheduledQty(result);
        if (scheduledQty > 0) {
            return scheduledQty;
        }
        return result != null && result.getDailyPlanQty() != null ? Math.max(0, result.getDailyPlanQty()) : 0;
    }

    /**
     * 兼容原有分班入口，并按换模完成时刻解析首检归属班次。
     * <p>主流程已经显式传入维修预热后的首检归属班次；该重载保留给既有调用方及回归测试，
     * 未命中计划性维修时与改造前行为完全一致。</p>
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param shifts 排程班次
     * @param startTime 正式生产开始时间
     * @param shiftCapacity 标准班产
     * @param lhTimeSeconds 硫化周期秒数
     * @param mouldQty 模台数
     * @param remaining 待排量
     * @param cleaningWindowList 清洗窗口
     * @param maintenanceWindowList 保养窗口
     * @param sku SKU排程DTO
     * @param isEnding 是否收尾
     * @param mouldChangeCompleteTime 换模完成时间
     * @param shiftPlanCapacityMap 班次计划量上限
     * @return 未排产的剩余量
     */
    private int distributeToShifts(LhScheduleContext context,
                                   LhScheduleResult result,
                                   List<LhShiftConfigVO> shifts,
                                   Date startTime,
                                   int shiftCapacity,
                                   int lhTimeSeconds,
                                   int mouldQty,
                                   int remaining,
                                   List<MachineCleaningWindowDTO> cleaningWindowList,
                                   List<MachineMaintenanceWindowDTO> maintenanceWindowList,
                                   SkuScheduleDTO sku,
                                   boolean isEnding,
                                   Date mouldChangeCompleteTime,
                                   Map<Integer, Integer> shiftPlanCapacityMap) {
        LhShiftConfigVO firstInspectionAttributionShift = FirstInspectionQtyUtil
                .resolveFirstInspectionAttributionShift(
                        context, sku, shifts, mouldChangeCompleteTime, ScheduleTypeEnum.NEW_SPEC.getCode());
        return distributeToShifts(context, result, shifts, startTime, shiftCapacity, lhTimeSeconds, mouldQty,
                remaining, cleaningWindowList, maintenanceWindowList, sku, isEnding, mouldChangeCompleteTime,
                shiftPlanCapacityMap, firstInspectionAttributionShift, false, false, null);
    }

    /**
     * 将首检均衡基准时间对齐到首检数量归属班次。
     * <p>试制早班切换会把首检调整到中班；当调整后的时间正好等于中班起点时，
     * 根据“边界归前一班”规则增加1秒，避免首检资源仍被计入早班。</p>
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param shifts 排程窗口班次
     * @param attributionShift 首检数量归属班次
     * @param inspectionTime 首检均衡基准时间
     * @return 与数量归属班次一致的首检均衡时间
     */
    private Date alignInspectionBalanceTimeToAttributionShift(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            List<LhShiftConfigVO> shifts,
            LhShiftConfigVO attributionShift,
            Date inspectionTime) {
        if (Objects.isNull(attributionShift) || Objects.isNull(inspectionTime)) {
            return inspectionTime;
        }
        LhShiftConfigVO rawAttributionShift = FirstInspectionQtyUtil.resolveAttributionShift(
                shifts, inspectionTime);
        if (Objects.nonNull(rawAttributionShift)
                && Objects.equals(rawAttributionShift.getShiftIndex(), attributionShift.getShiftIndex())) {
            return inspectionTime;
        }
        Date attributionShiftStartTime = attributionShift.getShiftStartDateTime();
        if (Objects.nonNull(attributionShiftStartTime)
                && !inspectionTime.after(attributionShiftStartTime)) {
            // 班次边界统一采用[start,end)，班次开始整点天然归属于当前班次，无需再增加1秒。
            Date alignedTime = attributionShiftStartTime;
            log.debug("首检均衡时间对齐数量归属班次, batchNo: {}, materialCode: {}, "
                            + "originalTime: {}, attributionShift: class{}, alignedTime: {}",
                    Objects.isNull(context) ? null : context.getBatchNo(),
                    Objects.isNull(sku) ? null : sku.getMaterialCode(),
                    LhScheduleTimeUtil.formatDateTime(inspectionTime), attributionShift.getShiftIndex(),
                    LhScheduleTimeUtil.formatDateTime(alignedTime));
            return alignedTime;
        }
        return inspectionTime;
    }

    /**
     * 解析强制首检受限后的下一有效切换重试时间。
     * <p>当前班次无法容纳强制首检时，说明该班次不允许新规格实际上机。
     * 因此必须从下一班次开始时间重新换模，不能通过反推换模完成时间，
     * 让换模仍占用已超限班次。晚班禁换模、维保及换模次数等真实资源约束，
     * 仍由现有换模分配器在该起点基础上继续后移。</p>
     *
     * @param attributionShift 当前首检归属班次
     * @param currentSwitchStartTime 当前切换开始时间
     * @return 下一次切换最早重试时间
     */
    private Date resolveNextFirstInspectionRetryReadyTime(LhShiftConfigVO attributionShift,
                                                          Date currentSwitchStartTime) {
        if (Objects.isNull(attributionShift)
                || Objects.isNull(attributionShift.getShiftEndDateTime())) {
            return currentSwitchStartTime;
        }
        return attributionShift.getShiftEndDateTime();
    }

    /**
     * 将统一生产门禁约束后的生产起点顺延到可完整承载首检的班次。
     *
     * <p>该方法只移动首检和正式生产起点，不重新分配换模，不占用首检均衡资源。
     * 每次试算均复用正式落班相同的停机、清洗、保养和班次产能方法，避免候选试算
     * 与最终结果出现不同口径。</p>
     *
     * @param context 排程上下文
     * @param machine 候选机台
     * @param sku 当前新增 SKU
     * @param requestedStartTime SKU 类型门禁、胎胚时间与现有理论时间取较晚后的起点
     * @param mouldChangeStartTime 已分配的换模开始时间
     * @param shifts 当前业务日班次
     * @param mouldQty 运行态模数
     * @param runtimeShiftCapacity 运行态完整班产
     * @param remainingQty 当前候选目标量
     * @param isEnding 是否收尾
     * @param inspectionScheduleType 首检排程类型
     * @return 首个可完整承载首检且存在正产量的生产起点；不存在时返回 null
     */
    private Date resolveProductionGateConstrainedStartTime(
            LhScheduleContext context,
            MachineScheduleDTO machine,
            SkuScheduleDTO sku,
            Date requestedStartTime,
            Date mouldChangeStartTime,
            List<LhShiftConfigVO> shifts,
            int mouldQty,
            int runtimeShiftCapacity,
            int remainingQty,
            boolean isEnding,
            String inspectionScheduleType) {
        Date candidateStartTime = requestedStartTime;
        while (Objects.nonNull(candidateStartTime)) {
            LhShiftConfigVO attributionShift =
                    NewSpecEmbryoAvailableTimeResolver.resolveProductionShift(shifts, candidateStartTime);
            if (Objects.isNull(attributionShift)) {
                return null;
            }
            Map<Integer, Integer> capacityMap = this.calculateShiftCapacityMap(
                    context, machine, sku, candidateStartTime, mouldChangeStartTime,
                    shifts, mouldQty, runtimeShiftCapacity, isEnding, true);
            int partialShiftCapacity = Math.max(0, capacityMap.getOrDefault(
                    attributionShift.getShiftIndex(), 0));
            int firstInspectionQty = FirstInspectionQtyUtil.resolvePreviewFirstInspectionQty(
                    context, sku, attributionShift, runtimeShiftCapacity, remainingQty,
                    inspectionScheduleType, machine.getMachineCode());
            int availableCapacity = FirstInspectionQtyUtil.resolveEmbryoAvailableShiftCapacity(
                    context, sku, attributionShift, partialShiftCapacity, firstInspectionQty,
                    runtimeShiftCapacity, inspectionScheduleType, machine.getMachineCode());
            if (availableCapacity > 0) {
                return candidateStartTime;
            }
            log.info("新增SKU生产门禁所在班次不足以完整承载首检，首检和生产整体顺延, "
                            + "batchNo: {}, materialCode: {}, machineCode: {}, classNo: class{}, "
                            + "candidateStartTime: {}, partialShiftCapacity: {}, firstInspectionQty: {}, "
                            + "nextShiftStartTime: {}",
                    context.getBatchNo(), sku.getMaterialCode(), machine.getMachineCode(),
                    attributionShift.getShiftIndex(),
                    LhScheduleTimeUtil.formatDateTime(candidateStartTime),
                    partialShiftCapacity, firstInspectionQty,
                    LhScheduleTimeUtil.formatDateTime(attributionShift.getShiftEndDateTime()));
            // 班次采用左闭右开区间，当前班结束时刻自然归入下一班，再经过既有管控校正。
            candidateStartTime = ShiftProductionControlUtil.resolveFirstSchedulableStartIgnoringCleaning(
                    context, machine.getMachineCode(), attributionShift.getShiftEndDateTime(), shifts,
                    runtimeShiftCapacity, sku.getLhTimeSeconds(), mouldQty);
        }
        return null;
    }

    /**
     * 记录已成功提交的胎胚最早可供时间应用日志。
     *
     * <p>有效生产秒数复用停机、清洗和保养时间交集扣减方法；折算计划量取正式候选
     * 使用的班次产能图，确保应用日志、过程日志与最终落班使用同一口径。</p>
     *
     * @param context 排程上下文
     * @param sku 当前新增 SKU
     * @param machine 落地机台
     * @param result 已提交结果
     * @param shifts 当前业务日班次
     * @param earliestAvailableTime 胎胚最早可供时间
     * @param theoreticalProductionStartTime 现有规则理论开产时间
     * @param actualProductionStartTime 实际生产开始时间
     * @param mouldChangeStartTime 换模开始时间
     * @param mouldChangeCompleteTime 换模实际完成时间
     * @param attributionShift 首检及生产落地班次
     * @param shiftCapacityMap 正式使用的班次产能图
     * @param runtimeShiftCapacity 运行态完整班产
     * @param machinePlanQty 本机台目标量
     */
    private void appendEmbryoAvailableTimeAppliedProcessLog(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            MachineScheduleDTO machine,
            LhScheduleResult result,
            List<LhShiftConfigVO> shifts,
            Date earliestAvailableTime,
            Date theoreticalProductionStartTime,
            Date actualProductionStartTime,
            Date mouldChangeStartTime,
            Date mouldChangeCompleteTime,
            LhShiftConfigVO attributionShift,
            Map<Integer, Integer> shiftCapacityMap,
            int runtimeShiftCapacity,
            int machinePlanQty) {
        if (Objects.isNull(attributionShift)) {
            return;
        }
        ShiftProductionControlDTO control = ShiftProductionControlUtil.resolveEffectiveControl(
                context, attributionShift, actualProductionStartTime);
        long netProductiveSeconds = 0L;
        if (Objects.nonNull(control) && control.isCanSchedule()) {
            Date effectiveProductionStartTime =
                    NewSpecEmbryoAvailableTimeResolver.resolveEffectiveProductionWindowStart(
                            control.getEffectiveStartTime(), control.getEffectiveEndTime(),
                            actualProductionStartTime);
            List<MachineCleaningWindowDTO> cleaningWindowList = resolveEffectiveCleaningWindowList(
                    context, machine.getMachineCode(), sku,
                    mouldChangeStartTime, mouldChangeCompleteTime);
            List<MachineMaintenanceWindowDTO> maintenanceWindowList =
                    resolveMachineMaintenanceWindowList(context, machine.getMachineCode());
            if (Objects.nonNull(effectiveProductionStartTime)) {
                netProductiveSeconds = ShiftCapacityResolverUtil.resolveNetProductiveSeconds(
                        context.getDevicePlanShutList(), cleaningWindowList, maintenanceWindowList,
                        machine.getMachineCode(), effectiveProductionStartTime, control.getEffectiveEndTime());
            }
        }
        int convertedShiftQty = CollectionUtils.isEmpty(shiftCapacityMap)
                ? 0 : Math.max(0, shiftCapacityMap.getOrDefault(attributionShift.getShiftIndex(), 0));
        int firstInspectionQty = FirstInspectionQtyUtil.resolveLastRecordedFirstInspectionQty(
                context, sku, attributionShift, runtimeShiftCapacity, machinePlanQty,
                ScheduleTypeEnum.NEW_SPEC.getCode(), machine.getMachineCode());
        log.info("新增SKU胎胚最早可供时间限制已落地, batchNo: {}, scheduleDate: {}, structureName: {}, "
                        + "materialCode: {}, machineCode: {}, earliestAvailableTime: {}, "
                        + "theoreticalProductionStartTime: {}, actualProductionStartTime: {}, classNo: class{}, "
                        + "netProductiveSeconds: {}, firstInspectionQty: {}, convertedShiftQty: {}, "
                        + "resultDailyPlanQty: {}",
                context.getBatchNo(), LhScheduleTimeUtil.formatDate(context.getScheduleTargetDate()),
                sku.getStructureName(), sku.getMaterialCode(), machine.getMachineCode(),
                LhScheduleTimeUtil.formatDateTime(earliestAvailableTime),
                LhScheduleTimeUtil.formatDateTime(theoreticalProductionStartTime),
                LhScheduleTimeUtil.formatDateTime(actualProductionStartTime),
                attributionShift.getShiftIndex(), netProductiveSeconds, firstInspectionQty,
                convertedShiftQty, result.getDailyPlanQty());
        StringBuilder detail = new StringBuilder(384);
        PriorityTraceLogHelper.appendLine(detail,
                "批次=" + context.getBatchNo() + "，排程日期="
                        + LhScheduleTimeUtil.formatDate(context.getScheduleTargetDate())
                        + "，结构=" + sku.getStructureName() + "，SKU=" + sku.getMaterialCode()
                        + "，机台=" + machine.getMachineCode());
        PriorityTraceLogHelper.appendLine(detail,
                "胎胚最早可供=" + LhScheduleTimeUtil.formatDateTime(earliestAvailableTime)
                        + "，理论可开产=" + LhScheduleTimeUtil.formatDateTime(theoreticalProductionStartTime)
                        + "，实际生产开始=" + LhScheduleTimeUtil.formatDateTime(actualProductionStartTime));
        PriorityTraceLogHelper.appendLine(detail,
                "落地班次=class" + attributionShift.getShiftIndex()
                        + "，有效生产秒数=" + netProductiveSeconds
                        + "，首检量=" + firstInspectionQty
                        + "，折算班次量=" + convertedShiftQty
                        + "，结果计划量=" + result.getDailyPlanQty());
        PriorityTraceLogHelper.appendProcessLog(
                context, "新增排产胎胚最早可供硫化时间", detail.toString().trim());
    }

    /**
     * 无副作用预检强制首检数量是否能落入归属班次。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param attributionShift 首检归属班次
     * @param shiftCapacity 运行态班产
     * @param remainingQty 当前机台最终目标量
     * @param machineCode 候选机台编码
     * @return true-无需首检条数或首检可合法写入；false-受SYS0303004限制需顺延
     */
    private boolean canLandRequiredFirstInspection(LhScheduleContext context,
                                                   SkuScheduleDTO sku,
                                                   LhShiftConfigVO attributionShift,
                                                   int shiftCapacity,
                                                   int remainingQty,
                                                   String machineCode) {
        int previewFirstInspectionQty = FirstInspectionQtyUtil.resolvePreviewFirstInspectionQty(
                context, sku, attributionShift, shiftCapacity, remainingQty,
                ScheduleTypeEnum.NEW_SPEC.getCode(), machineCode);
        if (previewFirstInspectionQty <= 0) {
            // 试制SKU使用固定2小时首检产能，不生成首检条数，不进入SYS0303004数量校验。
            return true;
        }
        if (Objects.isNull(attributionShift)) {
            return false;
        }
        LhScheduleResult previewResult = new LhScheduleResult();
        previewResult.setMaterialCode(Objects.isNull(sku) ? null : sku.getMaterialCode());
        previewResult.setLhMachineCode(machineCode);
        return canIncreaseShiftQtyByClassTotalLimit(
                context, sku, previewResult, attributionShift.getShiftIndex(),
                previewFirstInspectionQty, "新增排产强制首检落位预检");
    }

    /**
     * 无副作用校验跨班首检计划的每个班次数量是否均可落位。
     *
     * <p>首检总量可能覆盖正式开产前的一个或多个班次，必须逐班复用SYS0303004等现有
     * 班次总量限制。任一班次失败时整次准备时间轴顺延，禁止只丢弃受限班次的首检尾量。</p>
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param plan 选机阶段形成的首检分摊计划
     * @param machineCode 候选机台编码
     * @return true-全部班次均可落位；false-计划无效或至少一个班次受限
     */
    private boolean canLandRequiredFirstInspection(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            FirstInspectionAllocationPlan plan,
            String machineCode) {
        if (Objects.isNull(plan) || !plan.isValid()) {
            return false;
        }
        if (plan.getInspectionQty() <= 0) {
            return true;
        }
        LhScheduleResult previewResult = new LhScheduleResult();
        previewResult.setMaterialCode(Objects.isNull(sku) ? null : sku.getMaterialCode());
        previewResult.setLhMachineCode(machineCode);
        for (FirstInspectionShiftAllocation allocation : plan.getShiftAllocations()) {
            if (!canIncreaseShiftQtyByClassTotalLimit(
                    context, sku, previewResult, allocation.getShift().getShiftIndex(),
                    allocation.getQuantity(), "新增排产跨班首检落位预检")) {
                return false;
            }
            ShiftFieldUtil.setShiftPlanQty(
                    previewResult, allocation.getShift().getShiftIndex(), allocation.getQuantity(),
                    allocation.getOverlapStartTime(), allocation.getOverlapEndTime());
        }
        return true;
    }

    /**
     * 将计划量分配到各班次（从开产时间开始）。
     * <p>试制非收尾SKU会根据日计划额度限制每个班次的排产量；计划性维修场景由调用方传入
     * 预热完成后的首检归属班次，避免维修完成时刻被错误用于首检占班。</p>
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param shifts 排程班次
     * @param startTime 正式生产开始时间
     * @param shiftCapacity 标准班产
     * @param lhTimeSeconds 硫化周期秒数
     * @param mouldQty 模台数
     * @param remaining 待排量
     * @param cleaningWindowList 清洗窗口
     * @param maintenanceWindowList 保养及容量专用维修窗口
     * @param sku SKU排程DTO（用于获取日计划额度账本和目标量控制标记）
     * @param isEnding 是否收尾
     * @param mouldChangeCompleteTime 换模完成时间，用于首检结果字段回填
     * @param shiftPlanCapacityMap 已按日标准产量修正的班次计划量上限
     * @param firstInspectionAttributionShift 首检归属班次
     * @param alreadyStartedOnMachine 是否已经在该物理机台上机
     * @return 未排产的剩余量
     */
    private int distributeToShifts(LhScheduleContext context,
                                   LhScheduleResult result,
                                   List<LhShiftConfigVO> shifts,
                                   Date startTime,
                                   int shiftCapacity,
                                   int lhTimeSeconds,
                                   int mouldQty,
                                   int remaining,
                                   List<MachineCleaningWindowDTO> cleaningWindowList,
                                   List<MachineMaintenanceWindowDTO> maintenanceWindowList,
                                   SkuScheduleDTO sku,
                                   boolean isEnding,
                                   Date mouldChangeCompleteTime,
                                   Map<Integer, Integer> shiftPlanCapacityMap,
                                   LhShiftConfigVO firstInspectionAttributionShift,
                                   boolean alreadyStartedOnMachine) {
        return distributeToShifts(
                context, result, shifts, startTime, shiftCapacity, lhTimeSeconds, mouldQty,
                remaining, cleaningWindowList, maintenanceWindowList, sku, isEnding,
                mouldChangeCompleteTime, shiftPlanCapacityMap, firstInspectionAttributionShift,
                alreadyStartedOnMachine, false, null);
    }

    /**
     * 按指定首检口径将新增计划量分配到各班次。
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param shifts 排程班次
     * @param startTime 实际生产开始时间
     * @param shiftCapacity 完整班产
     * @param lhTimeSeconds 硫化周期秒数
     * @param mouldQty 模数
     * @param remaining 待排量
     * @param cleaningWindowList 清洗窗口
     * @param maintenanceWindowList 保养窗口
     * @param sku 当前新增 SKU
     * @param isEnding 是否收尾
     * @param mouldChangeCompleteTime 换模完成时间
     * @param shiftPlanCapacityMap 班次计划量上限
     * @param firstInspectionAttributionShift 首检归属班次
     * @param alreadyStartedOnMachine 是否已在当前物理机台上机
     * @param productionStartTimeConstrained 是否启用统一生产门禁部分班次首检口径
     * @param firstInspectionAllocationPlan 选机阶段已确认的首检跨班分摊计划
     * @return 未排产剩余量
     */
    private int distributeToShifts(
            LhScheduleContext context,
            LhScheduleResult result,
            List<LhShiftConfigVO> shifts,
            Date startTime,
            int shiftCapacity,
            int lhTimeSeconds,
            int mouldQty,
            int remaining,
            List<MachineCleaningWindowDTO> cleaningWindowList,
            List<MachineMaintenanceWindowDTO> maintenanceWindowList,
            SkuScheduleDTO sku,
            boolean isEnding,
            Date mouldChangeCompleteTime,
            Map<Integer, Integer> shiftPlanCapacityMap,
            LhShiftConfigVO firstInspectionAttributionShift,
            boolean alreadyStartedOnMachine,
            boolean productionStartTimeConstrained,
            FirstInspectionAllocationPlan firstInspectionAllocationPlan) {
        if (lhTimeSeconds <= 0 || mouldQty <= 0 || remaining <= 0 || startTime == null) {
            return remaining;
        }
        /*
         * 普通换模首检数量归属口径：
         * 1. 换模8小时已包含首检，不额外增加首检时间；
         * 2. 首检只影响数量归属和班产占用；
         * 3. 非试制归属班次由换模完成时间落点决定，试制早班切换后归同业务日中班；
         * 4. 非试制首检数量参与排产量、余量消耗和班产上限校验；试制首检仅通过中班固定2小时上限体现。
         */
        LhShiftConfigVO firstInspectionShift = firstInspectionAttributionShift;
        boolean useCrossShiftInspectionPlan = Objects.nonNull(firstInspectionAllocationPlan)
                && firstInspectionAllocationPlan.isValid()
                && firstInspectionAllocationPlan.getInspectionQty() > 0;
        Map<Integer, Integer> firstInspectionShiftQtyMap = useCrossShiftInspectionPlan
                ? FirstInspectionAllocationUtil.toShiftQtyMap(firstInspectionAllocationPlan)
                : Collections.<Integer, Integer>emptyMap();
        int previewFirstInspectionQty = useCrossShiftInspectionPlan
                ? firstInspectionAllocationPlan.getInspectionQty()
                : FirstInspectionQtyUtil.resolvePreviewFirstInspectionQty(
                        context, sku, firstInspectionShift, shiftCapacity, remaining,
                        ScheduleTypeEnum.NEW_SPEC.getCode(), result.getLhMachineCode());
        int remainingBeforeFirstInspection = remaining;
        int firstInspectionQty = 0;
        int firstInspectionCapsuleLossQty = 0;
        if (useCrossShiftInspectionPlan) {
            /*
             * 调用处已经按同一计划逐班预检SYS0303004；正式写入再次复用计划中的时间和数量，
             * 不调用旧的“全部首检归一个班次”入口，也不重新读取首检参数。
             */
            firstInspectionQty = FirstInspectionQtyUtil.addFirstInspectionAllocationToResult(
                    context, result, firstInspectionAllocationPlan,
                    ScheduleTypeEnum.NEW_SPEC.getCode());
            if (firstInspectionQty != previewFirstInspectionQty) {
                log.warn("新增SKU跨班首检正式写入与预演不一致，终止当前候选班次分配, "
                                + "batchNo: {}, materialCode: {}, machineCode: {}, 预演量: {}, 实写量: {}",
                        context.getBatchNo(), result.getMaterialCode(), result.getLhMachineCode(),
                        previewFirstInspectionQty, firstInspectionQty);
                return remaining;
            }
        } else if (previewFirstInspectionQty > 0 && Objects.nonNull(firstInspectionShift)) {
            if (!canIncreaseShiftQtyByClassTotalLimit(
                    context, sku, result, firstInspectionShift.getShiftIndex(),
                    previewFirstInspectionQty, "新增排产首检数量归属")) {
                /*
                 * 强制首检无法写入时禁止继续排普通生产。主流程已在候选提交前预检并顺延；
                 * 这里作为最终写入不变量保护，避免后续调用链变化再次产生“换模后无首检生产”。
                 */
                log.warn("新增SKU强制首检无法写入，终止当前候选班次分配, batchNo: {}, "
                                + "materialCode: {}, machineCode: {}, firstInspectionShift: class{}, qty: {}",
                        context.getBatchNo(), result.getMaterialCode(), result.getLhMachineCode(),
                        firstInspectionShift.getShiftIndex(), previewFirstInspectionQty);
                return remaining;
            }
            /*
             * 首检条数属于真实生产量，也可能在首检生产过程中跨越胶囊上限。
             * 此处先记录实际扣减量，后续同班常规产量还要同时扣除这部分班产和需求上限，
             * 防止被扣的2条又在同一个班次以常规产量补回；差额必须保留给下一班继续排产。
             */
            int firstInspectionShiftCapacity = Math.max(previewFirstInspectionQty,
                    shiftPlanCapacityMap.getOrDefault(firstInspectionShift.getShiftIndex(), shiftCapacity));
            Date firstInspectionStartTime = Objects.nonNull(mouldChangeCompleteTime)
                    ? mouldChangeCompleteTime : startTime;
            int adjustedFirstInspectionQty = capsuleReplacementRuleService.resolveActualPlanQty(
                    context, result, firstInspectionShift, previewFirstInspectionQty,
                    firstInspectionShiftCapacity, firstInspectionStartTime, "新增排产首检");
            if (adjustedFirstInspectionQty <= 0) {
                log.warn("新增SKU强制首检经换胶囊规则收口后为0，终止当前候选班次分配, "
                                + "batchNo: {}, materialCode: {}, machineCode: {}, firstInspectionShift: class{}",
                        context.getBatchNo(), result.getMaterialCode(), result.getLhMachineCode(),
                        firstInspectionShift.getShiftIndex());
                return remaining;
            }
            firstInspectionCapsuleLossQty = Math.max(0,
                    previewFirstInspectionQty - adjustedFirstInspectionQty);
            firstInspectionQty = FirstInspectionQtyUtil.addFirstInspectionQtyToResult(
                    context, sku, result, firstInspectionShift, mouldChangeCompleteTime, shiftCapacity,
                    adjustedFirstInspectionQty, ScheduleTypeEnum.NEW_SPEC.getCode());
            if (firstInspectionQty <= 0) {
                log.warn("新增SKU强制首检实际写入为0，终止当前候选班次分配, batchNo: {}, "
                                + "materialCode: {}, machineCode: {}, firstInspectionShift: class{}",
                        context.getBatchNo(), result.getMaterialCode(), result.getLhMachineCode(),
                        firstInspectionShift.getShiftIndex());
                return remaining;
            }
            if (productionStartTimeConstrained) {
                /*
                 * 通用首检工具默认将班次开始时间写入结果。生产门禁推迟首检时，实际开始
                 * 必须使用已校正的生产起点；后续同班正常生产合并时继续保留该时间，
                 * 保证结果不展示胎胚可供或 X/T 中班门禁前的虚假生产时间。
                 */
                Integer writtenFirstInspectionQty = ShiftFieldUtil.getShiftPlanQty(
                        result, firstInspectionShift.getShiftIndex());
                setShiftPlanQty(result, firstInspectionShift.getShiftIndex(),
                        Math.max(0, Objects.isNull(writtenFirstInspectionQty)
                                ? 0 : writtenFirstInspectionQty), startTime, startTime);
            }
        }
        remaining -= firstInspectionQty;
        // 首检阶段可能触发未满产换胶囊，后续常规生产必须使用已合并时间窗口的最新产能口径。
        maintenanceWindowList = resolveMachineMaintenanceWindowList(context, result.getLhMachineCode());
        Map<Integer, ShiftRuntimeState> stateMap = context.getShiftRuntimeStateMap();
        int dryIceLossQty = context.getParamIntValue(
                LhScheduleParamConstant.DRY_ICE_LOSS_QTY, LhScheduleConstant.DRY_ICE_LOSS_QTY);
        int dryIceDurationHours = context.getParamIntValue(
                LhScheduleParamConstant.DRY_ICE_DURATION_HOURS, LhScheduleConstant.DRY_ICE_DURATION_HOURS);
        int plannedRepairFixedQty = context.getParamIntValue(
                LhScheduleParamConstant.PLANNED_REPAIR_FIXED_QTY, LhScheduleConstant.PLANNED_REPAIR_FIXED_QTY);
        String configPlusShiftType = ShiftCapacityResolverUtil.resolveOddShiftCapacityPlusShiftType(context);

        // 试制非收尾SKU在本轮分配内按日期追踪已消费日计划额度，防止同一天多个班次重复消费。
        // 新增排产仅补欠产场景复用该账本做滚动额度预演，避免窗口日计划为0时跨天班次被误裁。
        Map<LocalDate, Integer> trialDailyConsumedMap = null;
        if (shouldApplyStrictNonEndingQuotaLimit(sku, isEnding)) {
            Map<LocalDate, SkuDailyPlanQuotaDTO> quotaMap = sku.getDailyPlanQuotaMap();
            if (quotaMap != null && !quotaMap.isEmpty()) {
                trialDailyConsumedMap = new LinkedHashMap<>(4);
            }
        }

        boolean started = false;
        // SYS0303004仅控制新增SKU起排班次（上机班次），SKU上机后后续班次不再受限制。
        // 首检已排入时不立即标记为上机，需在起排班次循环中完成SYS0303004判断后再标记。
        boolean skuStartedOnMachine = alreadyStartedOnMachine;
        for (LhShiftConfigVO shift : shifts) {
            if (remaining <= 0) {
                break;
            }
            if (!started) {
                if (startTime.before(shift.getShiftEndDateTime())) {
                    started = true;
                } else {
                    continue;
                }
            }

            ShiftProductionControlDTO control = ShiftProductionControlUtil.resolveEffectiveControl(context, shift, startTime);
            if (control == null || !control.isCanSchedule()) {
                logNewSpecShiftSkip(result, shift, remaining, shiftCapacity, 0,
                        0, "班次管控不可排");
                continue;
            }
            Date effectiveStart = control.getEffectiveStartTime();
            Date effectiveEnd = control.getEffectiveEndTime();
            long shiftDurationSeconds = ShiftCapacityResolverUtil.resolveShiftDurationSeconds(shift);
            if (productionStartTimeConstrained) {
                /*
                 * 首检已经按实际生产班次写入结果，常规产能也必须从同一个实际生产起点开始。
                 * 不能因为班次管控窗口从班次开始生效，就重新使用生产门禁前的完整班次产能。
                 */
                effectiveStart = NewSpecEmbryoAvailableTimeResolver.resolveEffectiveProductionWindowStart(
                        effectiveStart, effectiveEnd, startTime);
                if (Objects.isNull(effectiveStart)) {
                    logNewSpecShiftSkip(result, shift, remaining, shiftCapacity, 0,
                            0, "生产门禁后的实际开始时间已到达当前班次结束时间");
                    continue;
                }
                shiftDurationSeconds = NewSpecEmbryoAvailableTimeResolver.resolveProductionWindowSeconds(
                        effectiveStart, effectiveEnd);
            }

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
                    shiftDurationSeconds,
                    dryIceLossQty,
                    dryIceDurationHours,
                    shift,
                    configPlusShiftType,
                    ScheduleTypeEnum.NEW_SPEC.getCode(),
                    plannedRepairFixedQty);
            shiftMaxQty = ShiftProductionControlUtil.deductCapacityByControl(control, shiftMaxQty, mouldQty);
            int currentShiftInspectionQty = Math.max(0,
                    firstInspectionShiftQtyMap.getOrDefault(shift.getShiftIndex(), 0));
            if (useCrossShiftInspectionPlan) {
                int currentShiftCapacityCap = Math.max(0,
                        ShiftCapacityResolverUtil.resolveActualShiftPlanQty(
                                shiftCapacity, shift, configPlusShiftType,
                                ScheduleTypeEnum.NEW_SPEC.getCode()));
                shiftMaxQty = Math.min(shiftMaxQty,
                        Math.max(0, currentShiftCapacityCap - currentShiftInspectionQty));
            } else {
                shiftMaxQty = FirstInspectionQtyUtil.resolveNormalCapacityAfterFirstInspection(
                        context, sku, shift, shiftMaxQty,
                        Objects.isNull(firstInspectionShift) ? -1 : firstInspectionShift.getShiftIndex(),
                        firstInspectionQty,
                        shiftCapacity, ScheduleTypeEnum.NEW_SPEC.getCode(), result.getLhMachineCode(),
                        productionStartTimeConstrained);
            }
            boolean isCurrentShiftFirstInspectionShift = useCrossShiftInspectionPlan
                    ? currentShiftInspectionQty > 0
                    : Objects.nonNull(firstInspectionShift)
                    && Objects.equals(firstInspectionShift.getShiftIndex(), shift.getShiftIndex());
            if (isCurrentShiftFirstInspectionShift && firstInspectionCapsuleLossQty > 0) {
                /*
                 * 满产数量模式在首检阶段已经触发时，当前班剩余常规产能仍须扣除同一份数量损失。
                 * 未满产时间模式不进入本分支，后续班次产能由换胶囊时间窗口统一重算。
                 */
                shiftMaxQty = Math.max(0, shiftMaxQty - firstInspectionCapsuleLossQty);
                int currentShiftDemandCap = Math.max(0,
                        remainingBeforeFirstInspection - firstInspectionQty - firstInspectionCapsuleLossQty);
                shiftMaxQty = Math.min(shiftMaxQty, currentShiftDemandCap);
            }
            int physicalShiftMaxQty = shiftMaxQty;
            Integer dailyStandardShiftLimit = CollectionUtils.isEmpty(shiftPlanCapacityMap)
                    ? null : shiftPlanCapacityMap.get(shift.getShiftIndex());
            if (Objects.nonNull(dailyStandardShiftLimit)) {
                int effectiveShiftPlanLimit = Math.max(0, dailyStandardShiftLimit);
                if (productionStartTimeConstrained || useCrossShiftInspectionPlan) {
                    /*
                     * 生产门禁首班的产能图表达“首检 + 正常生产”的总上限；首检已先写入结果，
                     * 此处必须扣除已写首检量后再分配正常生产，防止两者相加突破部分班次总产能。
                     */
                    Integer existingShiftPlanQty = ShiftFieldUtil.getShiftPlanQty(
                            result, shift.getShiftIndex());
                    effectiveShiftPlanLimit = Math.max(0, effectiveShiftPlanLimit
                            - Math.max(0, Objects.isNull(existingShiftPlanQty) ? 0 : existingShiftPlanQty));
                }
                // 模拟、目标量和最终落班统一使用日标准产量修正后的班次上限，班产字段保持原值。
                shiftMaxQty = Math.min(shiftMaxQty, effectiveShiftPlanLimit);
            }
            if (shiftMaxQty <= 0) {
                String skipReason = physicalShiftMaxQty <= 0
                        ? "停机/清洗/保养/首检/班次管控扣减后无可用产能"
                        : "日标准产量修正后无可用产能";
                logNewSpecShiftSkip(result, shift, remaining, shiftCapacity,
                        physicalShiftMaxQty, shiftMaxQty, skipReason);
                continue;
            }

            // SYS0303004起排班次判断：SKU尚未上机时不做剩余容量收敛（不部分填充），
            // 完整班产超限时整体顺延或仅保留首检，避免部分填充导致生产不连续。
            // SKU已上机后不再受SYS0303004限制，直接按班产和原有约束排产。

            // 试制非收尾SKU严格按照日计划额度限制班次可排量上限，不允许超出当日计划量补满班次
            if (trialDailyConsumedMap != null) {
                int dailyQuotaCap = sku != null && sku.isStrictNewSpecShortageOnly()
                        ? resolveStrictNewSpecRollingQuotaCap(context, sku, shift.getWorkDate(), mouldQty,
                                trialDailyConsumedMap)
                        : resolveDailyQuotaCap(sku, shift.getWorkDate(), mouldQty, trialDailyConsumedMap);
                if (dailyQuotaCap >= 0) {
                    shiftMaxQty = Math.min(shiftMaxQty, dailyQuotaCap);
                }
                if (shiftMaxQty <= 0) {
                    logNewSpecShiftSkip(result, shift, remaining, shiftCapacity,
                            physicalShiftMaxQty, shiftMaxQty, "试制非收尾日计划额度账本回裁为0");
                    continue;
                }
            }

            int shiftQty = getTargetScheduleQtyResolver().resolveAllocatedShiftQty(
                    context, sku, Math.min(remaining, shiftMaxQty), shiftMaxQty, mouldQty);
            if (shiftQty > 0) {
                // 起排班次（SKU尚未上机）需判断SYS0303004同班次总计划量上限
                if (!skuStartedOnMachine && !canIncreaseShiftQtyByClassTotalLimit(context, sku, result,
                        shift.getShiftIndex(), shiftQty, "新增排产起排班次判断")) {
                    // 完整班产超过SYS0303004上限，判断首检特殊规则
                    if (firstInspectionQty > 0 && isCurrentShiftFirstInspectionShift) {
                        // 首检已排入当前班次（首检归属班次=当前班次），SKU视为已经上机。
                        // 当前班次仅保留首检计划量，不排常规产量，后续班次不再受SYS0303004限制。
                        skuStartedOnMachine = true;
                        logNewSpecShiftSkip(result, shift, remaining, shiftCapacity,
                                physicalShiftMaxQty, shiftMaxQty,
                                "同班次总计划量上限不足，首检已排入，起排班次仅保留首检");
                        continue;
                    } else if (firstInspectionQty > 0) {
                        // 首检已排入更早班次（首检归属班次 < 当前班次），SKU已经上机。
                        // 当前班次不再受SYS0303004限制，直接排常规产量，避免中间班次空量。
                        skuStartedOnMachine = true;
                        log.info("新增排产首检已排入更早班次，SKU已上机，当前班次跳过SYS0303004限制, "
                                + "batchNo: {}, materialCode: {}, machineCode: {}, classNo: class{}",
                                context.getBatchNo(), result.getMaterialCode(),
                                result.getLhMachineCode(), shift.getShiftIndex());
                    } else {
                        // 无首检排入，当前班次不能作为起排班次，顺延到下一个班次继续判断
                        logNewSpecShiftSkip(result, shift, remaining, shiftCapacity,
                                physicalShiftMaxQty, shiftMaxQty,
                                "同班次总计划量上限不足，起排班次顺延");
                        continue;
                    }
                }
                // 起排上限等既有规则通过后再扣换胶囊产能，避免胶囊规则反向改变SKU起排和选机判断。
                shiftQty = capsuleReplacementRuleService.resolveActualPlanQty(
                        context, result, shift, shiftQty, shiftMaxQty, effectiveStart, "新增排产");
                // 未满产换胶囊可能刚登记时间窗口，后续班次必须立即读取最新窗口重新计算产能。
                maintenanceWindowList = resolveMachineMaintenanceWindowList(context, result.getLhMachineCode());
                if (shiftQty <= 0) {
                    logNewSpecShiftSkip(result, shift, remaining, shiftCapacity,
                            physicalShiftMaxQty, shiftMaxQty, "换胶囊固定扣减后本班实际排产量为0");
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
                Integer existingQty = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
                Date existingStartTime = ShiftFieldUtil.getShiftStartTime(result, shift.getShiftIndex());
                int mergedQty = Math.max(0, existingQty == null ? 0 : existingQty) + shiftQty;
                setShiftPlanQty(result, shift.getShiftIndex(), mergedQty,
                        existingStartTime == null ? effectiveStart : existingStartTime, shiftPlanEndTime);
                remaining -= shiftQty;
                // SKU已上机（常规排产已写入），后续班次不再受SYS0303004限制
                skuStartedOnMachine = true;

                // 更新本轮分配内该日已消费的日计划额度
                if (trialDailyConsumedMap != null && shift.getWorkDate() != null) {
                    LocalDate productionDate = shift.getWorkDate().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate();
                    trialDailyConsumedMap.merge(productionDate, shiftQty, Integer::sum);
                }

                startTime = effectiveEnd;

                if (!CollectionUtils.isEmpty(stateMap)) {
                    ShiftRuntimeState st = stateMap.get(shift.getShiftIndex());
                    if (st != null) {
                        st.setRemainingCapacity(Math.max(0, shiftMaxQty - shiftQty));
                    }
                }
            } else {
                logNewSpecShiftSkip(result, shift, remaining, shiftCapacity,
                        physicalShiftMaxQty, shiftMaxQty, "目标量或硫化余量账本回裁为0");
            }
        }
        return remaining;
    }

    /**
     * 记录新增排产班次跳过原因，便于核对已换模上机 SKU 中间空班是否存在硬约束。
     *
     * @param result 新增排程结果
     * @param shift 当前班次
     * @param remaining 当前剩余目标量
     * @param shiftCapacity 原始班产
     * @param physicalShiftMaxQty 停机/清洗/保养/首检/班次管控扣减后的物理可用产能
     * @param finalShiftMaxQty 日标准或日计划账本修正后的最终可排产能
     * @param skipReason 跳过原因
     */
    private void logNewSpecShiftSkip(LhScheduleResult result,
                                     LhShiftConfigVO shift,
                                     int remaining,
                                     int shiftCapacity,
                                     int physicalShiftMaxQty,
                                     int finalShiftMaxQty,
                                     String skipReason) {
        if (Objects.isNull(result) || Objects.isNull(shift)) {
            return;
        }
        log.info("连续排产班次跳过诊断, 当前流程: 新增排产, materialCode: {}, machineCode: {}, 班次: {}, "
                        + "剩余余量: {}, 原始班产: {}, 班次物理可用产能: {}, 最终班次可用产能: {}, "
                        + "是否跳过: {}, 跳过原因: {}",
                result.getMaterialCode(), result.getLhMachineCode(), shift.getShiftIndex(), remaining,
                shiftCapacity, physicalShiftMaxQty, finalShiftMaxQty, true, skipReason);
    }

    /**
     * 解析新增排产仅补历史欠产时的滚动额度上限。
     * <p>该场景 T～T+2 日计划为0，欠产额度只追加在首日账本；班次跨天时仍应消费同一欠产池，
     * 不能按后续工作日0计划直接截断。</p>
     *
     * @param context 排程上下文
     * @param sku SKU排程DTO
     * @param workDate 班次归属工作日
     * @param mouldQty 模台数
     * @param trialDailyConsumedMap 本轮分配内已预占额度
     * @return 滚动额度上限，-1表示无需限制
     */
    private int resolveStrictNewSpecRollingQuotaCap(LhScheduleContext context,
                                                    SkuScheduleDTO sku,
                                                    Date workDate,
                                                    int mouldQty,
                                                    Map<LocalDate, Integer> trialDailyConsumedMap) {
        if (workDate == null || sku == null || CollectionUtils.isEmpty(sku.getDailyPlanQuotaMap())) {
            return -1;
        }
        LocalDate productionDate = workDate.toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        if (!sku.getDailyPlanQuotaMap().containsKey(productionDate)) {
            return 0;
        }
        Map<LocalDate, SkuDailyPlanQuotaDTO> trialQuotaMap = buildSimulationQuotaMap(
                sku.getDailyPlanQuotaMap(), SkuDailyPlanQuotaUtil.sumRemainingQty(sku.getDailyPlanQuotaMap()));
        replayTrialConsumedQuota(context, trialQuotaMap, trialDailyConsumedMap);
        int totalRemainingQty = SkuDailyPlanQuotaUtil.sumRemainingQty(trialQuotaMap);
        if (totalRemainingQty <= 0) {
            return 0;
        }
        int dailyQuotaCap = SkuDailyPlanQuotaUtil.consumeRollingQuota(
                trialQuotaMap, productionDate, totalRemainingQty,
                resolveLookAheadEndDate(context, trialQuotaMap, productionDate));
        int resolvedMouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(mouldQty);
        if (resolvedMouldQty > 1) {
            dailyQuotaCap = (dailyQuotaCap / resolvedMouldQty) * resolvedMouldQty;
        }
        return Math.max(dailyQuotaCap, 0);
    }

    /**
     * 将本轮已分配班次量回放到模拟账本，供后续班次计算剩余额度。
     *
     * @param context 排程上下文
     * @param trialQuotaMap 模拟日计划账本
     * @param trialDailyConsumedMap 本轮分配内已预占额度
     */
    private void replayTrialConsumedQuota(LhScheduleContext context,
                                          Map<LocalDate, SkuDailyPlanQuotaDTO> trialQuotaMap,
                                          Map<LocalDate, Integer> trialDailyConsumedMap) {
        if (CollectionUtils.isEmpty(trialQuotaMap) || CollectionUtils.isEmpty(trialDailyConsumedMap)) {
            return;
        }
        for (LocalDate productionDate : trialQuotaMap.keySet()) {
            Integer consumedQty = trialDailyConsumedMap.get(productionDate);
            if (consumedQty == null || consumedQty <= 0) {
                continue;
            }
            SkuDailyPlanQuotaUtil.consumeRollingQuota(
                    trialQuotaMap, productionDate, consumedQty,
                    resolveLookAheadEndDate(context, trialQuotaMap, productionDate));
        }
    }

    /**
     * 解析试制非收尾SKU在某工作日的日计划额度上限。
     * <p>从SKU的日计划额度账本中读取该日期的剩余额度，并扣除本轮已消费量，
     * 防止同一天多个班次重复消费。多模场景下按模台数对齐。</p>
     *
     * @param sku                  SKU排程DTO
     * @param workDate             班次归属工作日
     * @param mouldQty             模台数
     * @param trialDailyConsumedMap 本轮分配内按日期已消费量追踪
     * @return 日计划额度上限，-1表示无需限制
     */
    private int resolveDailyQuotaCap(SkuScheduleDTO sku, Date workDate, int mouldQty,
                                      Map<LocalDate, Integer> trialDailyConsumedMap) {
        if (workDate == null) {
            return -1;
        }
        Map<LocalDate, SkuDailyPlanQuotaDTO> quotaMap = sku.getDailyPlanQuotaMap();
        if (quotaMap == null || quotaMap.isEmpty()) {
            return -1;
        }
        LocalDate productionDate = workDate.toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        SkuDailyPlanQuotaDTO quota = quotaMap.get(productionDate);
        if (quota == null) {
            // 该日期不在月计划范围内，不允许排产
            return 0;
        }
        int dailyRemaining = Math.max(0, quota.getRemainingQty());
        // 扣除本轮分配中该日期已消费的额度
        if (trialDailyConsumedMap != null) {
            Integer consumed = trialDailyConsumedMap.get(productionDate);
            if (consumed != null) {
                dailyRemaining = Math.max(0, dailyRemaining - consumed);
            }
        }
        if (dailyRemaining <= 0) {
            return 0;
        }
        // 多模场景下按模台数对齐，确保分配量可被机台实际生产
        int resolvedMouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(mouldQty);
        if (resolvedMouldQty > 1) {
            dailyRemaining = (dailyRemaining / resolvedMouldQty) * resolvedMouldQty;
        }
        return Math.max(dailyRemaining, 0);
    }

    private void setShiftPlanQty(LhScheduleResult result, int shiftIndex, int qty, Date startTime, Date endTime) {
        ShiftFieldUtil.setShiftPlanQty(result, shiftIndex, qty, startTime, endTime);
    }

    private int calcTotalPlanQty(LhScheduleResult result, List<LhShiftConfigVO> shifts) {
        int total = 0;
        for (LhShiftConfigVO s : shifts) {
            Integer q = ShiftFieldUtil.getShiftPlanQty(result, s.getShiftIndex());
            total += (q != null ? q : 0);
        }
        return total;
    }

    /**
     * 判断当前班次增加指定计划量后是否仍满足同班次总计划量上限。
     *
     * @param context 排程上下文
     * @param sku 当前新增SKU
     * @param result 当前排程结果
     * @param shiftIndex 班次索引
     * @param incrementQty 本次拟增加计划量
     * @param action 业务动作说明
     * @return true-允许写入；false-超过同班次总计划量上限，需要跳过本次写入
     */
    private boolean canIncreaseShiftQtyByClassTotalLimit(LhScheduleContext context,
                                                         SkuScheduleDTO sku,
                                                         LhScheduleResult result,
                                                         Integer shiftIndex,
                                                         int incrementQty,
                                                         String action) {
        if (Objects.isNull(shiftIndex) || incrementQty <= 0) {
            return true;
        }
        int classTotalQtyLimit = resolveClassTotalQtyLimit(context);
        if (classTotalQtyLimit <= 0) {
            return true;
        }
        // 复用剩余容量口径判断增量是否超限，起排班次判断和后置重分配统一使用
        int remainingCapacity = resolveClassTotalRemainingCapacity(context, result, shiftIndex);
        if (remainingCapacity >= incrementQty) {
            return true;
        }
        int currentClassScheduledQty = classTotalQtyLimit - remainingCapacity;
        int projectedQty = currentClassScheduledQty + incrementQty;
        logClassTotalQtyLimitSkip(context, sku, result, shiftIndex, currentClassScheduledQty,
                incrementQty, projectedQty, classTotalQtyLimit, action);
        return false;
    }

    /**
     * 判断一组同班次重分配后的总计划量是否满足同班次总计划量上限。
     *
     * @param context 排程上下文
     * @param sku 当前新增SKU
     * @param shiftIndex 班次索引
     * @param targetQtyMap 重分配后的结果班次量
     * @param action 业务动作说明
     * @return true-允许重分配；false-超过上限，需要保持原分布
     */
    private boolean canApplyShiftTargetQtyByClassTotalLimit(LhScheduleContext context,
                                                            SkuScheduleDTO sku,
                                                            Integer shiftIndex,
                                                            Map<LhScheduleResult, Integer> targetQtyMap,
                                                            String action) {
        if (Objects.isNull(shiftIndex) || CollectionUtils.isEmpty(targetQtyMap)) {
            return true;
        }
        int classTotalQtyLimit = resolveClassTotalQtyLimit(context);
        if (classTotalQtyLimit <= 0) {
            return true;
        }
        int projectedQty = 0;
        if (Objects.nonNull(context) && !CollectionUtils.isEmpty(context.getScheduleResultList())) {
            for (LhScheduleResult scheduleResult : context.getScheduleResultList()) {
                if (Objects.isNull(scheduleResult)) {
                    continue;
                }
                if (targetQtyMap.containsKey(scheduleResult)) {
                    Integer targetQty = targetQtyMap.get(scheduleResult);
                    projectedQty += Math.max(0, Objects.isNull(targetQty) ? 0 : targetQty);
                    continue;
                }
                projectedQty += resolvePositiveShiftQty(scheduleResult, shiftIndex);
            }
        }
        for (Map.Entry<LhScheduleResult, Integer> entry : targetQtyMap.entrySet()) {
            if (isResultPersistedInContext(context, entry.getKey())) {
                continue;
            }
            Integer targetQty = entry.getValue();
            projectedQty += Math.max(0, Objects.isNull(targetQty) ? 0 : targetQty);
        }
        if (projectedQty <= classTotalQtyLimit) {
            return true;
        }
        int currentClassScheduledQty = resolveClassShiftScheduledQty(context, shiftIndex);
        int increaseQty = Math.max(0, projectedQty - currentClassScheduledQty);
        LhScheduleResult logResult = resolveFirstTargetResult(targetQtyMap);
        logClassTotalQtyLimitSkip(context, sku, logResult, shiftIndex,
                currentClassScheduledQty, increaseQty, projectedQty, classTotalQtyLimit, action);
        return false;
    }

    /**
     * 解析同班次重分配日志使用的代表结果。
     *
     * @param targetQtyMap 重分配后的结果班次量
     * @return 用于输出机台号的排程结果
     */
    private LhScheduleResult resolveFirstTargetResult(Map<LhScheduleResult, Integer> targetQtyMap) {
        if (CollectionUtils.isEmpty(targetQtyMap)) {
            return null;
        }
        for (LhScheduleResult result : targetQtyMap.keySet()) {
            if (Objects.nonNull(result)) {
                return result;
            }
        }
        return null;
    }

    /**
     * 解析同班次总计划量上限配置。
     *
     * @param context 排程上下文
     * @return 上限值，<=0 表示不限制
     */
    private int resolveClassTotalQtyLimit(LhScheduleContext context) {
        if (Objects.isNull(context)) {
            return LhScheduleConstant.CLASS_TOTAL_QTY_UP_LIMIT;
        }
        return context.getParamIntValue(LhScheduleParamConstant.CLASS_TOTAL_QTY_UP_LIMIT,
                LhScheduleConstant.CLASS_TOTAL_QTY_UP_LIMIT);
    }

    /**
     * 汇总当前已排结果中指定班次的计划量。
     *
     * @param context 排程上下文
     * @param shiftIndex 班次索引
     * @return 已排总量
     */
    private int resolveClassShiftScheduledQty(LhScheduleContext context, Integer shiftIndex) {
        if (Objects.isNull(context) || Objects.isNull(shiftIndex)
                || CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return 0;
        }
        int totalQty = 0;
        for (LhScheduleResult scheduleResult : context.getScheduleResultList()) {
            totalQty += resolvePositiveShiftQty(scheduleResult, shiftIndex);
        }
        return totalQty;
    }

    /**
     * 解析指定班次的同班次总计划量上限剩余容量。
     * <p>与 {@link #canIncreaseShiftQtyByClassTotalLimit} 统一口径：统计当前排程上下文内
     * 同班次已排总量，未持久化结果需叠加当前结果已有量避免重复计算。
     * 起排班次判断时用于校验完整班产是否超限，不再做部分填充收敛。</p>
     *
     * @param context 排程上下文
     * @param result 当前排程结果
     * @param shiftIndex 班次索引
     * @return 剩余可排容量；上限<=0表示不限制时返回 Integer.MAX_VALUE
     */
    private int resolveClassTotalRemainingCapacity(LhScheduleContext context,
                                                   LhScheduleResult result,
                                                   Integer shiftIndex) {
        int classTotalQtyLimit = resolveClassTotalQtyLimit(context);
        if (classTotalQtyLimit <= 0 || Objects.isNull(shiftIndex)) {
            return Integer.MAX_VALUE;
        }
        int currentClassScheduledQty = resolveClassShiftScheduledQty(context, shiftIndex);
        if (!isResultPersistedInContext(context, result)) {
            currentClassScheduledQty += resolvePositiveShiftQty(result, shiftIndex);
        }
        return Math.max(0, classTotalQtyLimit - currentClassScheduledQty);
    }

    /**
     * 获取结果行指定班次的正向计划量。
     *
     * @param result 排程结果
     * @param shiftIndex 班次索引
     * @return 班次计划量
     */
    private int resolvePositiveShiftQty(LhScheduleResult result, Integer shiftIndex) {
        if (Objects.isNull(result) || Objects.isNull(shiftIndex)) {
            return 0;
        }
        Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
        return Math.max(0, Objects.isNull(planQty) ? 0 : planQty);
    }

    /**
     * 判断结果对象是否已经加入当前排程结果列表。
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @return true-已加入；false-尚未加入
     */
    private boolean isResultPersistedInContext(LhScheduleContext context, LhScheduleResult result) {
        if (Objects.isNull(context) || Objects.isNull(result)
                || CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return false;
        }
        for (LhScheduleResult scheduleResult : context.getScheduleResultList()) {
            if (scheduleResult == result) {
                return true;
            }
        }
        return false;
    }

    /**
     * 打印同班次总计划量超限跳过日志。
     */
    private void logClassTotalQtyLimitSkip(LhScheduleContext context,
                                           SkuScheduleDTO sku,
                                           LhScheduleResult result,
                                           Integer shiftIndex,
                                           int currentClassScheduledQty,
                                           int planQty,
                                           int projectedQty,
                                           int classTotalQtyLimit,
                                           String action) {
        log.info("新增排产班次总计划量超过上限，跳过当前班次, batchNo: {}, materialCode: {}, "
                        + "machineCode: {}, classNo: class{}, 已排总量: {}, 拟排量: {}, 预计总量: {}, 上限: {}, 动作: {}",
                Objects.isNull(context) ? null : context.getBatchNo(),
                Objects.nonNull(sku) ? sku.getMaterialCode()
                        : (Objects.isNull(result) ? null : result.getMaterialCode()),
                Objects.isNull(result) ? null : result.getLhMachineCode(), shiftIndex,
                currentClassScheduledQty, planQty, projectedQty, classTotalQtyLimit, action);
    }

    /**
     * 刷新结果行的汇总计划量和规格结束时间。
     *
     * @param context 排程上下文
     * @param result 排程结果
     */
    private void refreshResultSummary(LhScheduleContext context, LhScheduleResult result) {
        if (result == null) {
            return;
        }
        ShiftFieldUtil.syncDailyPlanQty(result);
        List<LhShiftConfigVO> shifts = context.getScheduleWindowShifts();
        if (CollectionUtils.isEmpty(shifts)) {
            shifts = LhScheduleTimeUtil.getScheduleShifts(context, context.getScheduleDate());
        }
        int lhTimeSeconds = result.getLhTime() != null ? result.getLhTime() : 0;
        int mouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(
                result.getMouldQty() != null ? result.getMouldQty() : 0);
        synchronizeMissingShiftEndTimes(
                context, result, shifts, lhTimeSeconds, mouldQty);
        Date specEndTime = calcSpecEndTime(context, result, shifts, lhTimeSeconds, mouldQty);
        result.setSpecEndTime(specEndTime);
        result.setTdaySpecEndTime(specEndTime);
        syncResultDowntimeSummary(context, result);
    }

    /**
     * 补齐结果裁剪或跨日合并后缺失的班次结束时间。
     *
     * <p>班次数量、班次结束时间、规格结束时间和机台运行态必须来自同一最终结果。
     * 只要班次有实际产量且开始时间存在，就按硫化周期、模台数及停机窗口重算结束时间，
     * 禁止仅刷新SPEC_END_TIME而留下班次结束时间为空。</p>
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param shifts 排程窗口班次
     * @param lhTimeSeconds 硫化周期秒数
     * @param mouldQty 模台数
     */
    private void synchronizeMissingShiftEndTimes(LhScheduleContext context,
                                                 LhScheduleResult result,
                                                 List<LhShiftConfigVO> shifts,
                                                 int lhTimeSeconds,
                                                 int mouldQty) {
        if (Objects.isNull(context) || Objects.isNull(result)
                || CollectionUtils.isEmpty(shifts)
                || lhTimeSeconds <= 0 || mouldQty <= 0) {
            return;
        }
        for (LhShiftConfigVO shift : shifts) {
            Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
            Date shiftStartTime = ShiftFieldUtil.getShiftStartTime(result, shift.getShiftIndex());
            Date shiftEndTime = ShiftFieldUtil.getShiftEndTime(result, shift.getShiftIndex());
            if (Objects.isNull(planQty) || planQty <= 0
                    || Objects.isNull(shiftStartTime) || Objects.nonNull(shiftEndTime)) {
                continue;
            }
            long productionSeconds =
                    (long) Math.ceil((double) planQty / mouldQty) * lhTimeSeconds;
            Date recalculatedEndTime = ShiftCapacityResolverUtil.resolveCompletionTimeWithDowntimes(
                    context.getDevicePlanShutList(),
                    resolveEffectiveCleaningWindowList(
                            context, result, resolveFirstPlannedShiftStartTime(result)),
                    resolveMachineMaintenanceWindowList(context, result.getLhMachineCode()),
                    result.getLhMachineCode(), shiftStartTime, productionSeconds);
            ShiftFieldUtil.setShiftPlanQty(
                    result, shift.getShiftIndex(), planQty, shiftStartTime, recalculatedEndTime);
            log.info("新增排产班次结束时间补齐, batchNo: {}, materialCode: {}, machineCode: {}, "
                            + "classNo: class{}, planQty: {}, shiftStartTime: {}, shiftEndTime: {}",
                    context.getBatchNo(), result.getMaterialCode(), result.getLhMachineCode(),
                    shift.getShiftIndex(), planQty,
                    LhScheduleTimeUtil.formatDateTime(shiftStartTime),
                    LhScheduleTimeUtil.formatDateTime(recalculatedEndTime));
        }
    }

    /**
     * 新增排产库存裁剪后，将零计划结果移出排程结果并转为未排。
     *
     * @param context 排程上下文
     */
    private void finalizeZeroPlanNewSpecResults(LhScheduleContext context) {
        if (context == null || CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return;
        }
        Map<String, Integer> zeroPlanQtyMap = new LinkedHashMap<>(8);
        Map<String, SkuScheduleDTO> zeroPlanSkuMap = new LinkedHashMap<>(8);
        List<LhScheduleResult> zeroPlanResults = new ArrayList<>(8);
        for (LhScheduleResult result : context.getScheduleResultList()) {
            /*
             * 换活字块不需要零计划量裁剪。释放续作原样重启虽最终按续作01落库，
             * 但它是S4.5中生成并消费新增账本的结果，仍必须进入本后置零量收口；
             * 否则库存或日计划后置裁为0时会遗留零量续作结果。
             */
            if ((!NEW_SPEC_SCHEDULE_TYPE.equals(result.getScheduleType())
                    && !this.isReleasedContinuationReuseResult(context, result))
                    || "1".equals(result.getIsTypeBlock())) {
                continue;
            }
            if (result.getDailyPlanQty() != null && result.getDailyPlanQty() > 0) {
                continue;
            }
            SkuScheduleDTO sku = findSkuDto(
                    context, result.getMaterialCode(), result.getProductStatus());
            invalidateHistoricalReverseResultAfterPostAdjust(context, result);
            result.setSpecEndTime(null);
            result.setTdaySpecEndTime(null);
            zeroPlanResults.add(result);
            if (StringUtils.isEmpty(result.getMaterialCode())) {
                continue;
            }
            int unscheduledQty = resolveRemainingUnscheduledQty(context, sku);
            if (unscheduledQty > 0) {
                String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                        result.getMaterialCode(), result.getProductStatus());
                zeroPlanQtyMap.putIfAbsent(skuKey, unscheduledQty);
                zeroPlanSkuMap.putIfAbsent(skuKey, sku);
            }
        }
        for (Map.Entry<String, Integer> entry : zeroPlanQtyMap.entrySet()) {
            mergeUnscheduledResultBySku(context, zeroPlanSkuMap.get(entry.getKey()), entry.getValue());
        }
        if (!CollectionUtils.isEmpty(zeroPlanResults)) {
            context.getScheduleResultList().removeAll(zeroPlanResults);
            removeResultsFromMachineAssignments(context, zeroPlanResults);
        }
        normalizeUnscheduledResultsBySku(context);
    }

    /**
     * 判断结果是否为S4.5当轮选回原释放机台形成的续作重新启用结果。
     *
     * @param context 排程上下文
     * @param result 待判断结果
     * @return true-S4.5管理的续作重新启用结果；false-普通S4.4续作或其他结果
     */
    private boolean isReleasedContinuationReuseResult(
            LhScheduleContext context,
            LhScheduleResult result) {
        if (Objects.isNull(context) || Objects.isNull(result)
                || !StringUtils.equals(
                ScheduleTypeEnum.CONTINUOUS.getCode(), result.getScheduleType())
                || !StringUtils.equals("0", result.getIsChangeMould())) {
            return false;
        }
        SkuScheduleDTO sourceSku = context.getScheduleResultSourceSkuMap().get(result);
        return this.isContinuationAddMachineCandidate(sourceSku)
                && StringUtils.equals(
                sourceSku.getPreferredContinuousMachineCode(), result.getLhMachineCode());
    }

    /**
     * 后置胎胚库存裁剪把反选结果裁为0时，撤销成功登记并记录最终失败。
     *
     * <p>反选不得突破胎胚库存硬约束。结果在主链落地时可能暂时成功，但后置物料级库存统一
     * 裁剪后若变为0，最终关系已经不存在，必须撤销保护和机台登记，避免后续误判为反选成功。</p>
     *
     * @param context 排程上下文
     * @param result 被裁为0的新增结果
     */
    private void invalidateHistoricalReverseResultAfterPostAdjust(
            LhScheduleContext context,
            LhScheduleResult result) {
        if (!context.isHistoricalReverseProtectedResult(result)) {
            return;
        }
        HistoricalReverseSelectionDirective directive = null;
        for (HistoricalReverseSelectionDirective currentDirective
                : context.getHistoricalReverseSelectionDirectiveList()) {
            if (StringUtils.equals(currentDirective.getMaterialCode(), result.getMaterialCode())
                    && StringUtils.equals(normalizeHistoricalReverseProductStatus(
                    currentDirective.getProductStatus()),
                    normalizeHistoricalReverseProductStatus(result.getProductStatus()))
                    && StringUtils.equals(currentDirective.getEffectiveMachineCode(),
                    result.getLhMachineCode())) {
                directive = currentDirective;
                break;
            }
        }
        if (Objects.nonNull(directive)) {
            directive.setSuccess(false);
            directive.setResultReason("反选结果被胎胚库存后置硬约束裁剪为0");
            context.unregisterHistoricalReverseSelectedMachine(
                    directive.getMaterialCode(), directive.getProductStatus(), directive.getMachineCode());
            appendHistoricalReverseNewSpecLog(
                    context, directive, "失败", directive.getResultReason());
        }
        context.unprotectHistoricalReverseResult(result);
    }

    private Date calcSpecEndTime(LhScheduleContext context,
                                 LhScheduleResult result,
                                 List<LhShiftConfigVO> shifts,
                                 int lhTimeSeconds,
                                 int mouldQty) {
        if (lhTimeSeconds <= 0 || mouldQty <= 0) {
            return null;
        }
        for (int i = shifts.size() - 1; i >= 0; i--) {
            LhShiftConfigVO shift = shifts.get(i);
            Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
            if (planQty == null || planQty <= 0) {
                continue;
            }
            Date shiftEnd = ShiftFieldUtil.getShiftEndTime(result, shift.getShiftIndex());
            if (shiftEnd != null) {
                return shiftEnd;
            }
            Date shiftStart = ShiftFieldUtil.getShiftStartTime(result, shift.getShiftIndex());
            if (shiftStart == null) {
                return shift.getShiftEndDateTime();
            }
            long secondsNeeded = (long) Math.ceil((double) planQty / mouldQty) * lhTimeSeconds;
            // 完工时间重算沿用结果级清洗过滤，避免被已跳过的清洗窗口再次顺延。
            List<MachineCleaningWindowDTO> cleaningWindowList = resolveEffectiveCleaningWindowList(
                    context, result, resolveFirstPlannedShiftStartTime(result));
            return ShiftCapacityResolverUtil.resolveCompletionTimeWithDowntimes(
                    context.getDevicePlanShutList(),
                    cleaningWindowList,
                    resolveMachineMaintenanceWindowList(context, result.getLhMachineCode()),
                    result.getLhMachineCode(),
                    shiftStart,
                    secondsNeeded);
        }
        return null;
    }

    /**
     * 机台缺失预计完工时刻时，回退到排程窗口基准时间，避免依赖系统当前时刻导致排程漂移。
     *
     * @param context 排程上下文
     * @param shifts 排程班次窗口
     * @return 默认机台结束时间
     */
    private Date resolveDefaultMachineEndTime(LhScheduleContext context, List<LhShiftConfigVO> shifts) {
        if (!CollectionUtils.isEmpty(shifts) && shifts.get(0).getShiftStartDateTime() != null) {
            return shifts.get(0).getShiftStartDateTime();
        }
        if (context != null && context.getScheduleDate() != null) {
            return context.getScheduleDate();
        }
        return new Date();
    }

    /**
     * 新增换模只能从当前排程窗口首班开始发起，不能借用窗口外的空闲时段提前换模。
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     * @param switchReadyTime 当前候选机台的可切换时间
     * @return 与排程窗口首班对齐后的可切换时间
     */
    private Date alignNewSpecSwitchReadyTimeToWindowStart(LhScheduleContext context,
                                                          List<LhShiftConfigVO> shifts,
                                                          Date switchReadyTime) {
        if (switchReadyTime == null) {
            return null;
        }
        Date windowStartTime = resolveScheduleWindowStartTime(context, shifts);
        if (windowStartTime != null && switchReadyTime.before(windowStartTime)) {
            return windowStartTime;
        }
        return switchReadyTime;
    }

    /**
     * 解析当前排程窗口首班开始时间。
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     * @return 窗口首班开始时间
     */
    private Date resolveScheduleWindowStartTime(LhScheduleContext context, List<LhShiftConfigVO> shifts) {
        if (!CollectionUtils.isEmpty(shifts) && shifts.get(0).getShiftStartDateTime() != null) {
            return shifts.get(0).getShiftStartDateTime();
        }
        if (context != null && context.getScheduleDate() != null) {
            return context.getScheduleDate();
        }
        return null;
    }

    private List<MachineCleaningWindowDTO> resolveMachineCleaningWindowList(LhScheduleContext context, String machineCode) {
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
        if (machine == null || CollectionUtils.isEmpty(machine.getCleaningWindowList())) {
            return new ArrayList<>();
        }
        return machine.getCleaningWindowList();
    }

    private List<MachineMaintenanceWindowDTO> resolveMachineMaintenanceWindowList(LhScheduleContext context, String machineCode) {
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
        List<MachineMaintenanceWindowDTO> maintenanceWindowList = machine == null
                ? new ArrayList<>() : machine.getMaintenanceWindowList();
        // 新增规格的候选产能、目标量预演和最终落点统一扣除维修及SYS0307009预热窗口。
        return ShiftCapacityResolverUtil.resolveCapacityMaintenanceWindowList(
                context, context.getDevicePlanShutList(), machineCode, maintenanceWindowList);
    }

    /**
     * 获取机台真实精度保养窗口，仅供最终停机摘要展示。
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
        Date mouldChangeCompleteTime = Objects.nonNull(result.getMouldChangeStartTime())
                ? LhScheduleTimeUtil.addHours(result.getMouldChangeStartTime(),
                LhScheduleTimeUtil.getMouldChangeTotalHours(context)) : firstPlannedShiftStartTime;
        // 新增换模与清洗重叠时清洗不额外占用时间，但必须用原始清洗窗口按真实换模 8 小时窗口补充“清洗+换模”原因备注。
        ResultDowntimeSummaryUtil.appendCleaningMouldChangeAnalysis(
                result,
                resolveMachineCleaningWindowList(context, result.getLhMachineCode()),
                result.getMouldChangeStartTime(),
                mouldChangeCompleteTime,
                scheduleWindowShifts);
    }

    /**
     * 解析新增换模结果在排产阶段需要生效的清洗窗口。
     *
     * @param context 排程上下文
     * @param machineCode 机台编号
     * @param switchStartTime 换模开始时间
     * @param firstProductionStartTime 首个可排产开始时间
     * @return 有效清洗窗口列表
     */
    private List<MachineCleaningWindowDTO> resolveEffectiveCleaningWindowList(LhScheduleContext context,
                                                                              String machineCode,
                                                                              SkuScheduleDTO sku,
                                                                              Date switchStartTime,
                                                                              Date firstProductionStartTime) {
        List<MachineCleaningWindowDTO> cleaningWindowList = resolveMachineCleaningWindowList(context, machineCode);
        if (CleaningScheduleRuleUtil.shouldSkipCleaningBySkuEnding(context, sku)) {
            return new ArrayList<>(0);
        }
        return new ArrayList<>(MachineCleaningOverlapUtil.excludeOverlapWindows(
                cleaningWindowList, switchStartTime, firstProductionStartTime));
    }

    /**
     * 解析新增结果在排产阶段需要生效的清洗窗口。
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param firstProductionStartTime 首个可排产开始时间
     * @return 有效清洗窗口列表
     */
    private List<MachineCleaningWindowDTO> resolveEffectiveCleaningWindowList(LhScheduleContext context,
                                                                              LhScheduleResult result,
                                                                              Date firstProductionStartTime) {
        if (Objects.isNull(result)) {
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
     * 解析清洗与切换重叠过滤使用的切换结束时间。
     *
     * <p>新增换模首检可能落在换模开始班次，导致首个有计划量班次早于真实换模完成时间；
     * 清洗+换模只能按真实 8 小时换模窗口判断，不能用首检班次开始时间截断。</p>
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
                    LhScheduleTimeUtil.getMouldChangeTotalHours(context));
        }
        return firstProductionStartTime;
    }

    /**
     * 解析新增换模结果实际使用的模具号。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param machine 当前机台
     * @param mouldQty 机台模数
     * @param allocationResult 模具资源分配结果
     * @return 实际使用模具号，多个逗号分隔
     */
    private String resolveActualMouldCodeForNewSpecResult(LhScheduleContext context,
                                                          SkuScheduleDTO sku,
                                                          MachineScheduleDTO machine,
                                                          int mouldQty,
                                                          MouldResourceAllocationResult allocationResult) {
        int requiredMouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(mouldQty);
        if (allocationResult == null || CollectionUtils.isEmpty(allocationResult.getAllocatedMouldCodeList())) {
            log.info("新增排产结果实际模具号为空, materialCode: {}, machineCode: {}, requiredMouldQty: {}",
                    sku.getMaterialCode(), machine.getMachineCode(), requiredMouldQty);
            return null;
        }
        if (allocationResult.getAllocatedMouldCodeList().size() < requiredMouldQty) {
            log.info("新增排产结果实际模具数量不足, materialCode: {}, machineCode: {}, requiredMouldQty: {}, "
                            + "allocatedMouldCodes: {}",
                    sku.getMaterialCode(), machine.getMachineCode(), requiredMouldQty,
                    allocationResult.getAllocatedMouldCodeList());
            return null;
        }
        String actualMouldCode = StringUtils.join(allocationResult.getAllocatedMouldCodeList(), ",");
        log.debug("新增排产结果写入实际模具号, batchNo: {}, materialCode: {}, machineCode: {}, "
                        + "requiredMouldQty: {}, actualMouldCode: {}",
                context.getBatchNo(), sku.getMaterialCode(), machine.getMachineCode(), requiredMouldQty, actualMouldCode);
        return actualMouldCode;
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

    private LhMaintenanceScheduleService getMaintenanceScheduleService() {
        return maintenanceScheduleService != null
                ? maintenanceScheduleService
                : new LhMaintenanceScheduleService();
    }

    private ITrialProductionStrategy getTrialProductionStrategy() {
        return trialProductionStrategy != null
                ? trialProductionStrategy
                : new DefaultTrialProductionStrategy();
    }

    /**
     * 判断试制量试SKU当日是否跳过。
     *
     * @param context 排程上下文
     * @param sku 新增SKU
     * @return true-跳过排产
     */
    private boolean shouldSkipTrialSku(LhScheduleContext context, SkuScheduleDTO sku) {
        return false;
    }

    /**
     * 分配同胎胚错峰后的换模时间。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param machineCode 机台编码
     * @param switchReadyTime 机台可换模时间
     * @param switchDurationHours 换模时长
     * @param mouldChangeBalance 换模均衡策略
     * @return 换模开始时间；无法分配时返回 null
     */
    private Date allocateGreenTireAwareMouldChange(LhScheduleContext context,
                                                   SkuScheduleDTO sku,
                                                   String machineCode,
                                                   Date switchReadyTime,
                                                   int switchDurationHours,
                                                   IMouldChangeBalanceStrategy mouldChangeBalance) {
        if (sku == null || StringUtils.isEmpty(sku.getEmbryoCode())) {
            if (sku != null) {
                log.debug("SKU胎胚编码为空，跳过同胎胚换模错开判断, materialCode: {}, machineCode: {}",
                        sku.getMaterialCode(), machineCode);
            }
            return mouldChangeBalance.allocateMouldChange(
                    context, machineCode, switchReadyTime, switchDurationHours,
                    sku, IMouldChangeBalanceStrategy.ACTION_NEW_SPEC_MOULD_CHANGE);
        }
        // 先把已有结果里的同胎胚换模班次回填到占用表，避免新增规格只感知本轮登记的占用。
        preloadGreenTireChangeoverOccupancy(context);
        Date cursorTime = switchReadyTime;
        for (int attempt = 0; attempt < LhScheduleConstant.MAX_SHIFT_SLOT_COUNT * 2; attempt++) {
            Date allocatedTime = mouldChangeBalance.allocateMouldChange(
                    context, machineCode, cursorTime, switchDurationHours,
                    sku, IMouldChangeBalanceStrategy.ACTION_NEW_SPEC_MOULD_CHANGE);
            if (allocatedTime == null) {
                return null;
            }
            int shiftIndex = LhScheduleTimeUtil.getShiftIndex(context, context.getScheduleDate(), allocatedTime);
            if (!hasGreenTireChangeoverConflict(context, sku.getEmbryoCode(), shiftIndex, sku.getMaterialCode())) {
                registerGreenTireChangeoverShift(context, sku.getEmbryoCode(), shiftIndex);
                return allocatedTime;
            }
            mouldChangeBalance.rollbackMouldChange(context, allocatedTime);
            Date nextProbeTime = resolveNextChangeoverProbeTime(context, shiftIndex, allocatedTime);
            log.info("同胎胚换模班次冲突，顺延换模, materialCode: {}, embryoCode: {}, machineCode: {}, "
                            + "冲突班次: {}, 原换模时间: {}, 顺延探测时间: {}",
                    sku.getMaterialCode(), sku.getEmbryoCode(), machineCode, shiftIndex,
                    LhScheduleTimeUtil.formatDateTime(allocatedTime),
                    LhScheduleTimeUtil.formatDateTime(nextProbeTime));
            if (nextProbeTime == null) {
                return null;
            }
            cursorTime = nextProbeTime;
        }
        log.warn("同胎胚换模错开失败，超过窗口探测上限, materialCode: {}, embryoCode: {}, machineCode: {}",
                sku.getMaterialCode(), sku.getEmbryoCode(), machineCode);
        return null;
    }

    /**
     * 回填已有排程结果中的同胎胚换模班次占用。
     *
     * @param context 排程上下文
     */
    private void preloadGreenTireChangeoverOccupancy(LhScheduleContext context) {
        if (context == null || CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return;
        }
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (!shouldTrackGreenTireChangeoverResult(result)) {
                continue;
            }
            Date changeoverStartTime = resolveExistingGreenTireChangeoverStartTime(result);
            if (changeoverStartTime == null) {
                continue;
            }
            int shiftIndex = LhScheduleTimeUtil.getShiftIndex(context, context.getScheduleDate(), changeoverStartTime);
            if (shiftIndex <= 0) {
                continue;
            }
            registerGreenTireChangeoverShift(context, result.getEmbryoCode(), shiftIndex);
        }
    }

    /**
     * 判断结果是否需要参与同胎胚换模占用回填。
     *
     * @param result 排程结果
     * @return true-需要参与；false-跳过
     */
    private boolean shouldTrackGreenTireChangeoverResult(LhScheduleResult result) {
        return result != null
                && "1".equals(result.getIsChangeMould())
                && StringUtils.isNotEmpty(result.getEmbryoCode())
                && resolveExistingGreenTireScheduledQty(result) > 0;
    }

    /**
     * 解析已有换模结果的计划量。
     *
     * @param result 排程结果
     * @return 计划量
     */
    private int resolveExistingGreenTireScheduledQty(LhScheduleResult result) {
        int scheduledQty = ShiftFieldUtil.resolveScheduledQty(result);
        if (scheduledQty > 0) {
            return scheduledQty;
        }
        return result.getDailyPlanQty() != null ? Math.max(0, result.getDailyPlanQty()) : 0;
    }

    /**
     * 解析已有换模结果应占用的换模开始时间。
     *
     * @param result 排程结果
     * @return 换模开始时间
     */
    private Date resolveExistingGreenTireChangeoverStartTime(LhScheduleResult result) {
        if (result == null) {
            return null;
        }
        if (result.getMouldChangeStartTime() != null) {
            return result.getMouldChangeStartTime();
        }
        Date productionStartTime = resolveExistingProductionStartTime(result);
        if (productionStartTime != null) {
            return productionStartTime;
        }
        return result.getSpecEndTime();
    }

    /**
     * 解析已有结果的首个开产时间，供缺少真实换模时间的结果复用。
     *
     * @param result 排程结果
     * @return 首个开产时间
     */
    private Date resolveExistingProductionStartTime(LhScheduleResult result) {
        List<Date> startTimes = new ArrayList<Date>(LhScheduleConstant.MAX_SHIFT_SLOT_COUNT);
        if (result.getClass1StartTime() != null) {
            startTimes.add(result.getClass1StartTime());
        }
        if (result.getClass2StartTime() != null) {
            startTimes.add(result.getClass2StartTime());
        }
        if (result.getClass3StartTime() != null) {
            startTimes.add(result.getClass3StartTime());
        }
        if (result.getClass4StartTime() != null) {
            startTimes.add(result.getClass4StartTime());
        }
        if (result.getClass5StartTime() != null) {
            startTimes.add(result.getClass5StartTime());
        }
        if (result.getClass6StartTime() != null) {
            startTimes.add(result.getClass6StartTime());
        }
        if (result.getClass7StartTime() != null) {
            startTimes.add(result.getClass7StartTime());
        }
        if (result.getClass8StartTime() != null) {
            startTimes.add(result.getClass8StartTime());
        }
        return startTimes.stream().min(Date::compareTo).orElse(null);
    }

    /**
     * 回滚换模均衡占用及同胎胚换模班次占用。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param mouldChangeBalance 换模均衡策略
     * @param mouldChangeStartTime 换模开始时间
     */
    private void rollbackMouldChangeAllocation(LhScheduleContext context,
                                               SkuScheduleDTO sku,
                                               IMouldChangeBalanceStrategy mouldChangeBalance,
                                               Date mouldChangeStartTime) {
        if (isChangeoverBalanceEnabled(context) && mouldChangeBalance != null) {
            mouldChangeBalance.rollbackMouldChange(context, mouldChangeStartTime);
        }
        rollbackGreenTireChangeoverShift(context, sku, mouldChangeStartTime);
    }

    /**
     * 在“生产日前跨日准备”场景下，把换模开始时间尽量延后，使换模完成后贴近真实开产时间。
     *
     * <p>当准备门禁晚于原机台可切换时间时，机台换模完成后可能长时间空等。这里在
     * 20:00(含)-次日06:00(不含) 禁换模约束内，把换模开始点延后到
     * {@code productionNotBeforeTime - switchDurationHours}；若该点落入禁换模窗口，
     * 则回退到最晚可开始点。该方法只调整换模开始时间，不放松禁换模或晚班约束。</p>
     *
     * @param context 排程上下文
     * @param materialCode 物料编码
     * @param machineCode 机台编码
     * @param switchReadyTime 原机台可切换时间
     * @param switchDurationHours 换模耗时
     * @param productionNotBeforeTime 调用方指定的准备门禁时间
     * @return 延后后的机台可切换时间；无延后空间时返回原值
     */
    private Date delaySwitchReadyTimeCloseToProductionStart(LhScheduleContext context,
                                                            String materialCode,
                                                            String machineCode,
                                                            Date switchReadyTime,
                                                            int switchDurationHours,
                                                            Date productionNotBeforeTime) {
        if (Objects.isNull(switchReadyTime) || Objects.isNull(productionNotBeforeTime)
                || switchDurationHours <= 0) {
            return switchReadyTime;
        }
        // 理想换模开始点 = 生产下限 - 换模时长，使换模完成后尽量紧挨真实开产。
        Date desiredStartTime = LhScheduleTimeUtil.addHours(productionNotBeforeTime, -switchDurationHours);
        if (Objects.isNull(desiredStartTime) || !desiredStartTime.after(switchReadyTime)) {
            return switchReadyTime;
        }
        // 理想开始点落入 20:00(含)-次日06:00(不含) 禁换模窗口时，回退到最晚可开始点。
        if (LhScheduleTimeUtil.isNoMouldChangeTime(context, desiredStartTime)) {
            desiredStartTime = LhScheduleTimeUtil.resolveLatestMouldChangeStartTime(context, desiredStartTime);
        }
        if (Objects.nonNull(desiredStartTime) && desiredStartTime.after(switchReadyTime)) {
            log.info("新增SKU换模延后贴近真实开产, batchNo: {}, materialCode: {}, machineCode: {}, "
                            + "原可切换: {}, 延后开始: {}, 换模时长: {}, 生产下限: {}",
                    context.getBatchNo(), materialCode, machineCode,
                    LhScheduleTimeUtil.formatDateTime(switchReadyTime),
                    LhScheduleTimeUtil.formatDateTime(desiredStartTime),
                    switchDurationHours,
                    LhScheduleTimeUtil.formatDateTime(productionNotBeforeTime));
            return desiredStartTime;
        }
        return switchReadyTime;
    }

    /**
     * 解析新增排产的换模开始时间。
     * <p>基础换模耗时、停机重叠和晚班不可换模永远保留；换模均衡配额仅在开关开启时生效。</p>
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @param switchReadyTime 机台可切换时间
     * @param switchDurationHours 换模耗时
     * @param mouldChangeBalance 换模均衡策略
     * @param phase 当前业务日内阶段
     * @param isTypeBlock 是否按换活字块口径分配切换时间
     * @param productionPreparationLookbackAllowed 是否允许选定机台回看窗口内更早班次完成准备
     * @return 实际换模开始时间；无法安排时返回 null
     */
    private Date allocateNewSpecMouldChangeStartTime(LhScheduleContext context,
                                                     SkuScheduleDTO sku,
                                                     String machineCode,
                                                     Date switchReadyTime,
                                                     int switchDurationHours,
                                                     IMouldChangeBalanceStrategy mouldChangeBalance,
                                                     DailySchedulePhase phase,
                                                     boolean isTypeBlock,
                                                     Date businessDayEndTime,
                                                     boolean productionPreparationLookbackAllowed) {
        if (isChangeoverBalanceEnabled(context)) {
            /*
             * 正式提交必须与候选预演使用同一动作类型。生产日前跨日准备使用独立动作，
             * 只受每日总上限约束；若仍按提前生产动作提交，会重新套用早8中7参考分布，
             * 导致预演14:00、正式提交却顺延到次日06:00，触发时间轴一致性校验回滚。
             */
            String actionType = this.resolveCommittedNewSpecChangeoverActionType(
                    context, sku, switchReadyTime, phase, isTypeBlock,
                    productionPreparationLookbackAllowed);
            return mouldChangeBalance.allocateMouldChange(
                    context, machineCode, switchReadyTime, switchDurationHours,
                    sku, actionType, businessDayEndTime);
        }
        return allocateBasicMouldChangeStartTime(context, machineCode, switchReadyTime, switchDurationHours);
    }

    /**
     * 无副作用预演当轮已选机台正式提交的跨日准备换模时间。
     *
     * <p>普通候选分组仍使用原早8中7均衡动作，避免跨日放宽改变既有选机顺序；只有候选
     * 已确定后才使用正式跨日准备动作预演一次，并与随后正式提交做严格时间轴一致性校验。</p>
     *
     * @param context 排程上下文
     * @param sku 当前已选SKU
     * @param machineCode 已选机台编码
     * @param switchReadyTime 贴近生产下限后的切换就绪时间
     * @param switchDurationHours 切换耗时
     * @param mouldChangeBalance 换模均衡策略
     * @param phase 当前业务日阶段
     * @param isTypeBlock 是否换活字块
     * @param businessDayEndTime 当前业务日日终
     * @param productionPreparationLookbackAllowed 是否命中跨日准备
     * @return 正式提交动作的预演开始时间；无合法窗口返回null
     */
    private Date previewCommittedNewSpecMouldChangeStartTime(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            String machineCode,
            Date switchReadyTime,
            int switchDurationHours,
            IMouldChangeBalanceStrategy mouldChangeBalance,
            DailySchedulePhase phase,
            boolean isTypeBlock,
            Date businessDayEndTime,
            boolean productionPreparationLookbackAllowed) {
        if (!isChangeoverBalanceEnabled(context)) {
            return allocateBasicMouldChangeStartTime(
                    context, machineCode, switchReadyTime, switchDurationHours);
        }
        String actionType = this.resolveCommittedNewSpecChangeoverActionType(
                context, sku, switchReadyTime, phase, isTypeBlock,
                productionPreparationLookbackAllowed);
        return mouldChangeBalance.previewMouldChange(
                context, machineCode, switchReadyTime, switchDurationHours,
                sku, actionType, businessDayEndTime);
    }

    /**
     * 解析已选机台正式提交时使用的换模动作。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param switchReadyTime 切换就绪时间
     * @param phase 当前业务日阶段
     * @param isTypeBlock 是否换活字块
     * @param productionPreparationLookbackAllowed 是否命中生产日前准备
     * @return 正式提交动作类型
     */
    private String resolveCommittedNewSpecChangeoverActionType(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            Date switchReadyTime,
            DailySchedulePhase phase,
            boolean isTypeBlock,
            boolean productionPreparationLookbackAllowed) {
        if (isTypeBlock) {
            return IMouldChangeBalanceStrategy.ACTION_TYPE_BLOCK_CHANGE;
        }
        if (productionPreparationLookbackAllowed) {
            return IMouldChangeBalanceStrategy.ACTION_CROSS_DAY_PREPARATION_MOULD_CHANGE;
        }
        return this.resolveNewSpecChangeoverActionType(
                context, sku, switchReadyTime, phase, false, false);
    }

    /**
     * 判断当前换模是否为后续日计划提前到目标业务日的首台新增换模。
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param switchReadyTime 换模就绪时间
     * @param phase 当前业务日内阶段
     * @return true-提前生产目标日首台换模；false-普通新增换模
     */
    private boolean isEarlyProductionTargetDayMouldChange(LhScheduleContext context,
                                                          SkuScheduleDTO sku,
                                                          Date switchReadyTime,
                                                          DailySchedulePhase phase) {
        if (!isEarlyProductionPhase(phase)) {
            return false;
        }
        if (Objects.isNull(context) || Objects.isNull(sku)
                || Objects.isNull(switchReadyTime)) {
            return false;
        }
        // 调用处直接复用候选分组阶段已经冻结的提前生产中心判定，不再按 T 日特判。
        EarlyProductionDecision earlyProductionDecision = resolveEarlyProductionDecision(
                context, sku, switchReadyTime, context.getScheduleWindowShifts(), false, phase);
        return isAllowedFuturePlanEarlyProduction(earlyProductionDecision);
    }

    /**
     * 基础换模时间分配。
     * <p>关闭换模均衡时，只保留停机重叠与晚班不可换模约束，不再校验早/中班及日累计换模配额。</p>
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @param switchReadyTime 机台可切换时间
     * @param switchDurationHours 换模耗时
     * @return 实际换模开始时间；无法安排时返回 null
     */
    private Date allocateBasicMouldChangeStartTime(LhScheduleContext context,
                                                   String machineCode,
                                                   Date switchReadyTime,
                                                   int switchDurationHours) {
        if (switchReadyTime == null) {
            return null;
        }
        Date adjustedTime = switchReadyTime;
        for (int attempt = 0; attempt < NEW_SPEC_CHANGEOVER_PROBE_LIMIT; attempt++) {
            Date downtimeAdjustedTime = resolveDowntimeAdjustedMouldChangeStartTime(
                    context, machineCode, adjustedTime, switchDurationHours);
            if (downtimeAdjustedTime != null && downtimeAdjustedTime.after(adjustedTime)) {
                adjustedTime = downtimeAdjustedTime;
                continue;
            }
            if (LhScheduleTimeUtil.isNoMouldChangeTime(context, adjustedTime)) {
                adjustedTime = LhScheduleTimeUtil.resolveNextMorningAfterNoMouldChangeWindow(context, adjustedTime);
                continue;
            }
            return adjustedTime;
        }
        log.warn("新增排产基础换模时间分配失败, machineCode: {}, switchReadyTime: {}, switchDurationHours: {}",
                machineCode, LhScheduleTimeUtil.formatDateTime(switchReadyTime), switchDurationHours);
        return null;
    }

    /**
     * 扣除设备停机后的最早换模开始时间。
     * <p>05计划性维修允许与换模并行，后续统一按最大结束时间追加SYS0307009预热；
     * 其他停机类型继续顺延，确保关闭换模均衡开关时也与启用路径保持一致。</p>
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @param candidateStartTime 候选换模开始时间
     * @param switchDurationHours 换模耗时
     * @return 停机顺延后的开始时间
     */
    private Date resolveDowntimeAdjustedMouldChangeStartTime(LhScheduleContext context,
                                                             String machineCode,
                                                             Date candidateStartTime,
                                                             int switchDurationHours) {
        if (context == null
                || StringUtils.isEmpty(machineCode)
                || candidateStartTime == null
                || CollectionUtils.isEmpty(context.getDevicePlanShutList())) {
            return candidateStartTime;
        }
        Date candidateEndTime = LhScheduleTimeUtil.addHours(candidateStartTime, switchDurationHours);
        Date latestOverlapEndTime = null;
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
        return latestOverlapEndTime != null ? latestOverlapEndTime : candidateStartTime;
    }

    /**
     * 判断新增排产是否启用换模均衡。
     *
     * @param context 排程上下文
     * @return true-启用；false-关闭
     */
    private boolean isChangeoverBalanceEnabled(LhScheduleContext context) {
        LhScheduleConfig scheduleConfig = context != null ? context.getScheduleConfig() : null;
        if (scheduleConfig == null) {
            return LhScheduleConstant.ENABLE_CHANGEOVER_BALANCE == 1;
        }
        return scheduleConfig.isChangeoverBalanceEnabled();
    }

    /**
     * 判断同胎胚换模班次是否冲突。
     *
     * @param context 排程上下文
     * @param greenTireGroupKey 胎胚分组Key
     * @param shiftIndex 班次索引
     * @return true-冲突，false-不冲突
     */
    private boolean hasGreenTireChangeoverConflict(LhScheduleContext context,
                                                   String greenTireGroupKey,
                                                   int shiftIndex,
                                                   String materialCode) {
        if (context == null || StringUtils.isEmpty(greenTireGroupKey) || shiftIndex <= 0) {
            return false;
        }
        Set<Integer> occupiedShiftSet = context.getGreenTireChangeoverShiftMap().get(greenTireGroupKey);
        if (CollectionUtils.isEmpty(occupiedShiftSet) || !occupiedShiftSet.contains(shiftIndex)) {
            return false;
        }
        return hasOtherSkuGreenTireChangeoverOccupancy(context, greenTireGroupKey, shiftIndex, materialCode);
    }

    /**
     * 判断指定班次上的同胎胚换模占用是否来自其他SKU。
     *
     * @param context 排程上下文
     * @param greenTireGroupKey 胎胚分组Key
     * @param shiftIndex 班次索引
     * @param materialCode 当前SKU物料编码
     * @return true-存在其他SKU占用；false-仅当前SKU占用
     */
    private boolean hasOtherSkuGreenTireChangeoverOccupancy(LhScheduleContext context,
                                                            String greenTireGroupKey,
                                                            int shiftIndex,
                                                            String materialCode) {
        if (context == null || CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return true;
        }
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (!shouldTrackGreenTireChangeoverResult(result)
                    || !StringUtils.equals(greenTireGroupKey, result.getEmbryoCode())) {
                continue;
            }
            Date changeoverStartTime = resolveExistingGreenTireChangeoverStartTime(result);
            if (changeoverStartTime == null) {
                continue;
            }
            int occupiedShiftIndex = LhScheduleTimeUtil.getShiftIndex(
                    context, context.getScheduleDate(), changeoverStartTime);
            if (occupiedShiftIndex != shiftIndex) {
                continue;
            }
            if (!StringUtils.equals(materialCode, result.getMaterialCode())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 登记同胎胚换模班次占用。
     *
     * @param context 排程上下文
     * @param greenTireGroupKey 胎胚分组Key
     * @param shiftIndex 班次索引
     */
    private void registerGreenTireChangeoverShift(LhScheduleContext context,
                                                  String greenTireGroupKey,
                                                  int shiftIndex) {
        if (context == null || StringUtils.isEmpty(greenTireGroupKey) || shiftIndex <= 0) {
            return;
        }
        context.getGreenTireChangeoverShiftMap()
                .computeIfAbsent(greenTireGroupKey, key -> new HashSet<Integer>(4))
                .add(shiftIndex);
    }

    /**
     * 回滚同胎胚换模班次占用。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param mouldChangeStartTime 换模开始时间
     */
    private void rollbackGreenTireChangeoverShift(LhScheduleContext context,
                                                  SkuScheduleDTO sku,
                                                  Date mouldChangeStartTime) {
        if (context == null || sku == null || StringUtils.isEmpty(sku.getEmbryoCode())
                || mouldChangeStartTime == null) {
            return;
        }
        int shiftIndex = LhScheduleTimeUtil.getShiftIndex(context, context.getScheduleDate(), mouldChangeStartTime);
        Set<Integer> occupiedShiftSet = context.getGreenTireChangeoverShiftMap().get(sku.getEmbryoCode());
        if (CollectionUtils.isEmpty(occupiedShiftSet)) {
            return;
        }
        occupiedShiftSet.remove(shiftIndex);
        if (occupiedShiftSet.isEmpty()) {
            context.getGreenTireChangeoverShiftMap().remove(sku.getEmbryoCode());
        }
    }

    /**
     * 获取下一次换模探测时间。
     *
     * @param context 排程上下文
     * @param shiftIndex 当前冲突班次索引
     * @param allocatedTime 当前换模时间
     * @return 下一探测时间
     */
    private Date resolveNextChangeoverProbeTime(LhScheduleContext context, int shiftIndex, Date allocatedTime) {
        if (context == null || shiftIndex <= 0) {
            return null;
        }
        LhShiftConfigVO shift = LhScheduleTimeUtil.getShiftByIndex(context, context.getScheduleDate(), shiftIndex);
        if (shift != null && shift.getShiftEndDateTime() != null) {
            return shift.getShiftEndDateTime();
        }
        return LhScheduleTimeUtil.addHours(allocatedTime, 1);
    }

    /**
     * 解析定点机台挤量后预留的机台就绪时间。
     *
     * @param context 排程上下文
     * @param sku 新增SKU
     * @param machineCode 机台编码
     * @param machineReadyTime 原机台就绪时间
     * @return 生效后的机台就绪时间
     */
    private Date resolveSpecifyReservedReadyTime(LhScheduleContext context,
                                                 SkuScheduleDTO sku,
                                                 String machineCode,
                                                 Date machineReadyTime) {
        if (context == null || sku == null || StringUtils.isEmpty(machineCode)) {
            return machineReadyTime;
        }
        String reservedMaterialCode = context.getSpecifyMachineReservedMaterialMap().get(machineCode);
        Date reservedSwitchStartTime = context.getSpecifyMachineReservedSwitchStartTimeMap().get(machineCode);
        if (!StringUtils.equals(reservedMaterialCode, sku.getMaterialCode()) || reservedSwitchStartTime == null) {
            return machineReadyTime;
        }
        if (machineReadyTime == null || reservedSwitchStartTime.after(machineReadyTime)) {
            log.info("新增排产使用定点机台挤量预留时间, machineCode: {}, materialCode: {}, readyTime: {}",
                    machineCode, sku.getMaterialCode(), LhScheduleTimeUtil.formatDateTime(reservedSwitchStartTime));
            return reservedSwitchStartTime;
        }
        return machineReadyTime;
    }

    /**
     * 清理定点机台挤量预留信息。
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @param sku 来源SKU
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
     * 解析机台新增换模接续起点。
     *
     * @param context 排程上下文
     * @param machine 候选机台
     * @param shifts 排程窗口班次
     * @return 机台已占用结束时间
     */
    private Date resolveMachineOccupationEndTime(LhScheduleContext context,
                                                 MachineScheduleDTO machine,
                                                 List<LhShiftConfigVO> shifts) {
        Date machineEndTime = Objects.nonNull(machine) ? machine.getEstimatedEndTime() : null;
        Date assignedEndTime = Objects.nonNull(machine)
                ? resolveLatestAssignedEndTime(context, machine.getMachineCode()) : null;
        Date occupationEndTime = resolveLaterTime(machineEndTime, assignedEndTime);
        if (Objects.nonNull(occupationEndTime)) {
            return occupationEndTime;
        }
        return resolveDefaultMachineEndTime(context, shifts);
    }

    /**
     * 按 SKU 粒度解析新增换模接续起点。
     * <p>正规 SKU 使用单控机台时，左右侧必须作为整机同步占用，因此最早接续时间取 L/R 两侧
     * 预计结束时间和当前已登记结果结束时间中的较晚值；单边粒度 SKU 仍沿用当前侧时间。</p>
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param machine 候选机台
     * @param shifts 排程窗口班次
     * @return 机台已占用结束时间
     */
    private Date resolveMachineOccupationEndTime(LhScheduleContext context,
                                                 SkuScheduleDTO sku,
                                                 MachineScheduleDTO machine,
                                                 List<LhShiftConfigVO> shifts) {
        Date currentSideEndTime = resolveMachineOccupationEndTime(
                context, machine, shifts);
        if (!LhSingleControlMachineUtil.isWholeMachineGranularitySku(context, sku)
                || Objects.isNull(machine)
                || !isSingleControlMachine(context, machine.getMachineCode())) {
            return currentSideEndTime;
        }
        MachineScheduleDTO pairMachine = LhSingleControlMachineUtil.resolvePairMachine(context, machine.getMachineCode());
        Date pairSideEndTime = resolveMachineOccupationEndTime(
                context, pairMachine, shifts);
        return resolveLaterTime(currentSideEndTime, pairSideEndTime);
    }

    /**
     * 获取同一机台已登记有效结果的最新结束时间。
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @return 最新有效结果结束时间
     */
    private Date resolveLatestAssignedEndTime(LhScheduleContext context, String machineCode) {
        if (Objects.isNull(context) || StringUtils.isEmpty(machineCode)) {
            return null;
        }
        List<LhScheduleResult> assignedResultList =
                context.getMachineAssignmentMap().get(machineCode);
        if (CollectionUtils.isEmpty(assignedResultList)) {
            return null;
        }
        Date latestEndTime = null;
        for (LhScheduleResult result : assignedResultList) {
            if (Objects.isNull(result)
                    || Objects.isNull(result.getDailyPlanQty())
                    || result.getDailyPlanQty() <= 0) {
                continue;
            }
            Date effectiveEndTime = this.resolveResultEffectiveOccupationEndTime(result);
            if (Objects.nonNull(effectiveEndTime)
                    && (Objects.isNull(latestEndTime)
                    || effectiveEndTime.after(latestEndTime))) {
                latestEndTime = effectiveEndTime;
            }
        }
        return latestEndTime;
    }

    /**
     * 解析结果行的真实物理占用结束时间。
     *
     * <p>{@code SPEC_END_TIME} 是结果汇总字段，历史结果可能因整模取整或后置时间轴校正而早于
     * 最后一个正计划班次的结束时间。新物料换模必须取两者较晚值，否则会在前物料仍生产时
     * 提前占用机台。只读取正计划班次，避免空班次残留时间误延长机台占用。</p>
     *
     * @param result 已登记机台结果
     * @return 汇总结束时间与最后正计划班次结束时间的较晚值
     */
    private Date resolveResultEffectiveOccupationEndTime(LhScheduleResult result) {
        if (Objects.isNull(result)) {
            return null;
        }
        Date effectiveEndTime = result.getSpecEndTime();
        for (int shiftIndex = 1;
                shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT;
                shiftIndex++) {
            Integer shiftPlanQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
            Date shiftEndTime = ShiftFieldUtil.getShiftEndTime(result, shiftIndex);
            if (Objects.nonNull(shiftPlanQty) && shiftPlanQty > 0
                    && Objects.nonNull(shiftEndTime)
                    && (Objects.isNull(effectiveEndTime)
                    || shiftEndTime.after(effectiveEndTime))) {
                effectiveEndTime = shiftEndTime;
            }
        }
        return effectiveEndTime;
    }

    /**
     * 获取两个时间中较晚的一个。
     *
     * @param first 第一个时间
     * @param second 第二个时间
     * @return 较晚时间
     */
    private Date resolveLaterTime(Date first, Date second) {
        if (Objects.isNull(first)) {
            return second;
        }
        if (Objects.isNull(second)) {
            return first;
        }
        return first.after(second) ? first : second;
    }

    private void updateMachineState(LhScheduleContext context, MachineScheduleDTO machine, SkuScheduleDTO sku, LhScheduleResult result) {
        cacheInitialMachineState(context, machine);
        machine.setPreviousMaterialCode(machine.getCurrentMaterialCode());
        machine.setPreviousMaterialDesc(machine.getCurrentMaterialDesc());
        // 机台切换到新物料时清除旧物料的结构收尾对齐运行态标识，避免污染后续判断与审计。
        machine.setStructureEndingAligned(false);
        machine.setCurrentMaterialCode(sku.getMaterialCode());
        machine.setCurrentMaterialDesc(sku.getMaterialDesc());
        machine.setPreviousSpecCode(sku.getSpecCode());
        machine.setPreviousProSize(sku.getProSize());
        machine.setEstimatedEndTime(result.getSpecEndTime());
    }

    /**
     * 在首次更新机台状态前缓存初始快照，便于零计划回滚。
     *
     * @param context 排程上下文
     * @param machine 机台
     */
    private void cacheInitialMachineState(LhScheduleContext context, MachineScheduleDTO machine) {
        if (context == null || machine == null || StringUtils.isEmpty(machine.getMachineCode())) {
            return;
        }
        if (context.getInitialMachineScheduleMap().containsKey(machine.getMachineCode())) {
            return;
        }
        MachineScheduleDTO snapshot = new MachineScheduleDTO();
        snapshot.setMachineCode(machine.getMachineCode());
        snapshot.setMachineName(machine.getMachineName());
        snapshot.setCurrentMaterialCode(machine.getCurrentMaterialCode());
        snapshot.setCurrentMaterialDesc(machine.getCurrentMaterialDesc());
        snapshot.setPreviousMaterialCode(machine.getPreviousMaterialCode());
        snapshot.setPreviousMaterialDesc(machine.getPreviousMaterialDesc());
        snapshot.setPreviousSpecCode(machine.getPreviousSpecCode());
        snapshot.setPreviousProSize(machine.getPreviousProSize());
        snapshot.setEstimatedEndTime(machine.getEstimatedEndTime());
        snapshot.setMachineOrder(machine.getMachineOrder());
        snapshot.setMaxMoldNum(machine.getMaxMoldNum());
        snapshot.setCapsuleUsageCount(machine.getCapsuleUsageCount());
        context.getInitialMachineScheduleMap().put(machine.getMachineCode(), snapshot);
    }

    /**
     * 生成工单号（使用线程安全的OrderNoGenerator）
     */
    private String generateOrderNo(LhScheduleContext context) {
        return orderNoGenerator.generateOrderNo(context.getScheduleTargetDate());
    }

    /**
     * 添加未排产记录
     */
    private void addUnscheduledResult(LhScheduleContext context, SkuScheduleDTO sku, String reason) {
        addUnscheduledResult(context, sku, sku.resolveTargetScheduleQty(), reason);
    }

    /**
     * 添加未排产记录
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param unscheduledQty 未排数量
     * @param reason 未排原因
     */
    private void addUnscheduledResult(LhScheduleContext context, SkuScheduleDTO sku,
                                      int unscheduledQty, String reason) {
        if (unscheduledQty <= 0) {
            removeZeroQtyUnscheduledResult(context, sku);
            log.info("新增SKU实际剩余量为0，跳过未排记录, batchNo: {}, materialCode: {}, productStatus: {}, reason: {}",
                    context.getBatchNo(), sku.getMaterialCode(), sku.getProductStatus(), reason);
            return;
        }
        LhUnscheduledResult unscheduled = new LhUnscheduledResult();
        unscheduled.setFactoryCode(context.getFactoryCode());
        unscheduled.setBatchNo(context.getBatchNo());
        unscheduled.setMaterialCode(sku.getMaterialCode());
        unscheduled.setProductStatus(sku.getProductStatus());
        unscheduled.setMaterialDesc(sku.getMaterialDesc());
        unscheduled.setScheduleDate(context.getScheduleTargetDate());
        unscheduled.setUnscheduledReason(reason);
        unscheduled.setUnscheduledQty(Math.max(0, unscheduledQty));
        unscheduled.setStructureName(sku.getStructureName());
        unscheduled.setMainMaterialDesc(sku.getMainMaterialDesc());
        unscheduled.setSpecCode(sku.getSpecCode());
        unscheduled.setEmbryoCode(sku.getEmbryoCode());
        unscheduled.setMouldQty(sku.getMouldQty());
        unscheduled.setDataSource(AUTO_DATA_SOURCE);
        unscheduled.setIsDelete(0);
        context.getUnscheduledResultList().add(unscheduled);
        // 命中胎胚库存硬目标的新增SKU进入未排后，必须退出运行态有效集合，触发同胎胚剩余SKU二次分摊。
        if (getTargetScheduleQtyResolver().isEmbryoStockEnding(context, sku)) {
            getTargetScheduleQtyResolver().removeActiveEmbryoSku(context, sku, reason);
        }
        log.debug("新增SKU未排产, SKU: {}, 未排数量: {}, 原因: {}",
                sku.getMaterialCode(), Math.max(0, unscheduledQty), reason);
    }

    /**
     * 添加未排产记录并累计原因分布
     */
    private void addUnscheduledResult(LhScheduleContext context, SkuScheduleDTO sku, String reason,
                                      Map<String, Integer> reasonCountMap) {
        int unscheduledQty = sku.resolveTargetScheduleQty();
        addUnscheduledResult(context, sku, unscheduledQty, reason);
        if (unscheduledQty > 0) {
            reasonCountMap.merge(reason, 1, Integer::sum);
        }
    }

    /**
     * 添加指定数量的未排产记录并累计原因分布。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param unscheduledQty 未排数量
     * @param reason 未排原因
     * @param reasonCountMap 原因分布
     */
    private void addUnscheduledResult(LhScheduleContext context, SkuScheduleDTO sku,
                                      int unscheduledQty, String reason,
                                      Map<String, Integer> reasonCountMap) {
        addUnscheduledResult(context, sku, unscheduledQty, reason);
        if (unscheduledQty > 0) {
            reasonCountMap.merge(reason, 1, Integer::sum);
        }
    }

    /**
     * 清理同物料、同产品状态下已经生成的零量未排记录。
     *
     * @param context 排程上下文
     * @param sku SKU
     */
    private void removeZeroQtyUnscheduledResult(LhScheduleContext context, SkuScheduleDTO sku) {
        if (Objects.isNull(context) || Objects.isNull(sku)
                || CollectionUtils.isEmpty(context.getUnscheduledResultList())) {
            return;
        }
        context.getUnscheduledResultList().removeIf(unscheduledResult ->
                Objects.nonNull(unscheduledResult)
                        && StringUtils.equals(sku.getMaterialCode(), unscheduledResult.getMaterialCode())
                        && StringUtils.equals(StringUtils.trimToEmpty(sku.getProductStatus()),
                        StringUtils.trimToEmpty(unscheduledResult.getProductStatus()))
                        && (Objects.isNull(unscheduledResult.getUnscheduledQty())
                        || unscheduledResult.getUnscheduledQty() <= 0));
    }

    /**
     * 将排产块的班次数量同步到SKU实际消费账本和dayN节奏账本。
     * <p>先按SKU实际消费账本裁剪结果，避免同物料多入口重复消费；再按班次归属日期消费dayN节奏额度。
     * 如果班次产能大于dayN节奏剩余额度，非收尾结果保留实际排产量并记录满班补齐超排量。</p>
     *
     * @param context 排程上下文
     * @param sku SKU排程DTO
     * @param result 排程结果
     * @param shifts 排程窗口班次列表
     * @param allowFutureQuotaConsumption 是否允许消费未来 dayN 额度
     * @return 实际落地的排产量
     */
    private int applyBlockToDailyQuota(LhScheduleContext context,
                                       SkuScheduleDTO sku,
                                       LhScheduleResult result,
                                       List<LhShiftConfigVO> shifts,
                                       boolean allowFutureQuotaConsumption) {
        return this.applyBlockToDailyQuota(
                context, sku, result, shifts, allowFutureQuotaConsumption, null);
    }

    /**
     * 将新增排产块同步到日计划与SKU实际消费账本，并保护切换阶段首检。
     *
     * <p>首检已在正式生产前按真实时间发生，即使其覆盖班次的dayN额度不足，
     * 后置严格回裁也只能减少该班随后的正式生产量，不得删除已分摊首检。
     * 无首检计划的跨日续排、回填等旧调用继续通过上方重载保持原口径。</p>
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param result 待扣账的排程结果
     * @param shifts 本次参与扣账的班次
     * @param allowFutureQuotaConsumption 是否允许提前生产阶段消费未来 dayN
     * @param firstInspectionAllocationPlan 选机与结果共用的首检分摊计划
     * @return 回裁后实际排产量
     */
    private int applyBlockToDailyQuota(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LhScheduleResult result,
            List<LhShiftConfigVO> shifts,
            boolean allowFutureQuotaConsumption,
            FirstInspectionAllocationPlan firstInspectionAllocationPlan) {
        /*
         * 正式生产只写当前业务日班次，但首检可能向前覆盖上一业务日班次。目标量、dayN
         * 和实际消费账本都必须看到这些真实首检班次，否则结果中虽然存在首检条数，账本却只
         * 扣当前日正式产量。无跨日首检时该方法原样返回调用方班次，不扩大旧调用影响范围。
         */
        List<LhShiftConfigVO> accountingShifts = this.resolveFirstInspectionAccountingShifts(
                shifts, firstInspectionAllocationPlan);
        int cappedQty = getTargetScheduleQtyResolver().capResultByProductionRemainingQty(
                context, sku, result, accountingShifts, "新增排产");
        if (cappedQty <= 0) {
            return 0;
        }
        if (!FirstInspectionQtyUtil.isFirstInspectionAllocationRetained(
                result, firstInspectionAllocationPlan, 1)) {
            log.warn("新增SKU实际消费账本裁剪后无法完整保留首检，拒绝当前候选, "
                            + "batchNo: {}, materialCode: {}, machineCode: {}",
                    context.getBatchNo(), sku.getMaterialCode(), result.getLhMachineCode());
            return 0;
        }
        Map<LocalDate, SkuDailyPlanQuotaDTO> quotaMap =
                context.resolveEffectiveDailyPlanQuotaMap(sku);
        if (quotaMap == null || quotaMap.isEmpty()) {
            refreshResultSummary(context, result);
            int actualQty = result.getDailyPlanQty() != null ? result.getDailyPlanQty() : 0;
            getTargetScheduleQtyResolver().deductProductionRemainingQty(
                    context, sku, actualQty, "新增排产", result.getLhMachineCode());
            return actualQty;
        }
        int totalShiftFillOverQty = 0;
        for (LhShiftConfigVO shift : accountingShifts) {
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
            /*
             * 在机续排、当天计划和增机台阶段只能消费当前业务日及历史欠产；
             * 只有提前生产阶段才允许向后借用 dayN，避免正常资源竞争阶段抢占未来计划资源。
             */
            int consumed = SkuDailyPlanQuotaUtil.consumeRollingQuota(
                    quotaMap, productionDate, planQty,
                    resolveQuotaConsumeEndDate(
                            context, quotaMap, productionDate, allowFutureQuotaConsumption));
            int overQty = planQty - consumed;
            if (overQty > 0) {
                boolean endingResult = "1".equals(result.getIsEnd());
                // 收尾结果必须严格截断，且不再记录满班补齐超排；
                // 试制等严格目标量场景仍需回裁，但保留超排账本用于追踪被截掉的补满量。
                if (endingResult || shouldApplyStrictNonEndingQuotaLimit(sku, endingResult)) {
                    /*
                     * 严格日计划只回裁正式生产；该班已按真实时间发生的首检是
                     * 不可裁的切换阶段产量。回裁后若仅余首检，公共方法同时恢复其
                     * 真实重叠时间区间，保证结果与过程日志一致。
                     */
                    FirstInspectionQtyUtil.trimShiftPlanQtyPreservingInspection(
                            result, shift.getShiftIndex(), consumed,
                            firstInspectionAllocationPlan, 1);
                    if (endingResult) {
                        continue;
                    }
                }
                // 无法冲抵的部分记录为满班补齐超排量
                quota.setShiftFillOverQty(quota.getShiftFillOverQty() + overQty);
                totalShiftFillOverQty += overQty;
                log.debug("班次满班补齐超排, materialCode: {}, 日期: {}, 班次: {}, 排产量: {}, 超排: {}",
                        sku.getMaterialCode(), productionDate, shift.getShiftIndex(), planQty, overQty);
            }
        }
        if (totalShiftFillOverQty > 0) {
            sku.setShiftFillOverQty(sku.getShiftFillOverQty() + totalShiftFillOverQty);
            // 同步写入上下文累加器，确保SKU从待排列表移除后汇总日志仍可读取
            String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                    sku.getMaterialCode(), sku.getProductStatus());
            context.getSkuShiftFillOverQtyMap().merge(skuKey, totalShiftFillOverQty, Integer::sum);
        }
        refreshResultSummary(context, result);
        int actualQty = result.getDailyPlanQty() != null ? result.getDailyPlanQty() : 0;
        getTargetScheduleQtyResolver().deductProductionRemainingQty(
                context, sku, actualQty, "新增排产", result.getLhMachineCode());
        return actualQty;
    }

    /**
     * 将双模 SKU 单控整机结果同步消费到日计划和实际排产账本。
     * <p>整机排产必须保证 L/R 两边班次计划量完全一致，因此先构造一条“整机组结果”
     * 按两边合计量做账本裁剪和日计划扣减，再把裁剪后的组数量均分回两侧。
     * 如果账本只允许奇数尾量，为了保证左右一致，按可成对的偶数量落地，剩余 1 条留给后续未排/滚动处理。</p>
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param primaryResult 主侧结果
     * @param pairResult 配对侧结果
     * @param shifts 排程窗口班次
     * @param allowFutureQuotaConsumption 是否允许消费未来 dayN 额度
     * @return L/R 两侧合计实际排产量
     */
    private int applyWholeSingleControlBlockToDailyQuota(LhScheduleContext context,
                                                         SkuScheduleDTO sku,
                                                         LhScheduleResult primaryResult,
                                                         LhScheduleResult pairResult,
                                                         List<LhShiftConfigVO> shifts,
                                                         boolean allowFutureQuotaConsumption) {
        return this.applyWholeSingleControlBlockToDailyQuota(
                context, sku, primaryResult, pairResult, shifts,
                allowFutureQuotaConsumption, null);
    }

    /**
     * 按L/R整机合计口径扣账，并保护两侧已发生的首检数量。
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param primaryResult 单控主侧结果
     * @param pairResult 单控配对侧结果
     * @param shifts 班次列表
     * @param allowFutureQuotaConsumption 是否允许消费未来 dayN
     * @param firstInspectionAllocationPlan 单侧首检分摊计划；整机合计时按2倍保护
     * @return L/R 两侧合计实际排产量
     */
    private int applyWholeSingleControlBlockToDailyQuota(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LhScheduleResult primaryResult,
            LhScheduleResult pairResult,
            List<LhShiftConfigVO> shifts,
            boolean allowFutureQuotaConsumption,
            FirstInspectionAllocationPlan firstInspectionAllocationPlan) {
        if (Objects.isNull(primaryResult) || Objects.isNull(pairResult)) {
            return 0;
        }
        // 单控整机同样把两侧跨日首检班次并入账本作用域，再按整机偶数量统一扣账。
        List<LhShiftConfigVO> accountingShifts = this.resolveFirstInspectionAccountingShifts(
                shifts, firstInspectionAllocationPlan);
        LhScheduleResult groupResult = buildWholeSingleControlGroupResult(primaryResult);
        int cappedQty = getTargetScheduleQtyResolver().capResultByProductionRemainingQty(
                context, sku, groupResult, accountingShifts, "新增排产-单控整机");
        if (cappedQty <= 0) {
            copyWholeSingleControlGroupQtyToSides(context, groupResult, primaryResult, pairResult);
            return 0;
        }
        /*
         * 单控整机最终会均分到 L/R 两侧，SKU 实际消费账本裁剪后若留下奇数，直接除二会少落 1 条。
         * 因而必须先把整机组结果收敛为偶数，再进入 dayN 扣账，确保实际结果、SKU账本和dayN账本
         * 三者始终使用同一个整机数量口径。
         */
        int pairedCappedQty = retainWholeSingleControlPairedQty(groupResult);
        if (pairedCappedQty <= 0) {
            copyWholeSingleControlGroupQtyToSides(context, groupResult, primaryResult, pairResult);
            return 0;
        }
        if (!FirstInspectionQtyUtil.isFirstInspectionAllocationRetained(
                groupResult, firstInspectionAllocationPlan, 2)) {
            log.warn("新增SKU单控整机账本裁剪后无法完整保留L/R首检，拒绝当前候选, "
                            + "batchNo: {}, materialCode: {}, machineCode: {}",
                    context.getBatchNo(), sku.getMaterialCode(), primaryResult.getLhMachineCode());
            copyWholeSingleControlGroupQtyToSides(
                    context, groupResult, primaryResult, pairResult);
            return 0;
        }
        Map<LocalDate, SkuDailyPlanQuotaDTO> quotaMap =
                context.resolveEffectiveDailyPlanQuotaMap(sku);
        if (CollectionUtils.isEmpty(quotaMap)) {
            copyWholeSingleControlGroupQtyToSides(context, groupResult, primaryResult, pairResult);
            int actualQty = resolveWholeSingleControlActualQty(context, primaryResult, pairResult);
            getTargetScheduleQtyResolver().deductProductionRemainingQty(
                    context, sku, actualQty, "新增排产-单控整机", primaryResult.getLhMachineCode());
            return actualQty;
        }
        int totalShiftFillOverQty = 0;
        for (LhShiftConfigVO shift : accountingShifts) {
            Integer groupPlanQty = ShiftFieldUtil.getShiftPlanQty(groupResult, shift.getShiftIndex());
            if (Objects.isNull(groupPlanQty) || groupPlanQty <= 0 || Objects.isNull(shift.getWorkDate())) {
                continue;
            }
            LocalDate productionDate = shift.getWorkDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            SkuDailyPlanQuotaDTO quota = quotaMap.get(productionDate);
            if (Objects.isNull(quota)) {
                continue;
            }
            boolean endingResult = StringUtils.equals("1", primaryResult.getIsEnd());
            boolean strictPairQuota = endingResult
                    || shouldApplyStrictNonEndingQuotaLimit(sku, endingResult);
            LocalDate quotaConsumeEndDate = resolveQuotaConsumeEndDate(
                    context, quotaMap, productionDate, allowFutureQuotaConsumption);
            int quotaConsumeRequestQty = groupPlanQty;
            if (strictPairQuota) {
                /*
                 * 严格收尾、严格试制等场景不能保留满班超排。先无副作用预演可消费额度并取偶数，
                 * 再正式扣账，避免可用额度为 15 时先消费 15、实际 L/R 只能各落 7 条的账本失配。
                 */
                int previewConsumableQty = SkuDailyPlanQuotaUtil.previewRollingQuotaConsumableQty(
                        quotaMap, productionDate, groupPlanQty, quotaConsumeEndDate);
                quotaConsumeRequestQty = previewConsumableQty - previewConsumableQty % 2;
            }
            int consumed = SkuDailyPlanQuotaUtil.consumeRollingQuota(
                    quotaMap, productionDate, quotaConsumeRequestQty, quotaConsumeEndDate);
            int overQty = groupPlanQty - consumed;
            if (overQty > 0) {
                if (strictPairQuota) {
                    /*
                     * 整机首检为单侧分摊的2倍，因此保护后天然为偶数，可稳定
                     * 均分回L/R两侧；严格回裁不得把任一侧已发生的首检删掉。
                     */
                    FirstInspectionQtyUtil.trimShiftPlanQtyPreservingInspection(
                            groupResult, shift.getShiftIndex(), consumed,
                            firstInspectionAllocationPlan, 2);
                    if (endingResult) {
                        continue;
                    }
                }
                quota.setShiftFillOverQty(quota.getShiftFillOverQty() + overQty);
                totalShiftFillOverQty += overQty;
                log.debug("单控整机班次满班补齐超排, materialCode: {}, 日期: {}, 班次: {}, 整机排产量: {}, 超排: {}",
                        sku.getMaterialCode(), productionDate, shift.getShiftIndex(), groupPlanQty, overQty);
            }
        }
        if (totalShiftFillOverQty > 0) {
            sku.setShiftFillOverQty(sku.getShiftFillOverQty() + totalShiftFillOverQty);
            String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                    sku.getMaterialCode(), sku.getProductStatus());
            context.getSkuShiftFillOverQtyMap().merge(skuKey, totalShiftFillOverQty, Integer::sum);
        }
        copyWholeSingleControlGroupQtyToSides(context, groupResult, primaryResult, pairResult);
        int actualQty = resolveWholeSingleControlActualQty(context, primaryResult, pairResult);
        getTargetScheduleQtyResolver().deductProductionRemainingQty(
                context, sku, actualQty, "新增排产-单控整机", primaryResult.getLhMachineCode());
        return actualQty;
    }

    /**
     * 合并当前正式生产班次与首检实际覆盖班次，形成结果扣账的完整时间作用域。
     *
     * <p>新增日驱动主链按天传入2/3/3班次切片；当下一业务日的换模准备向前覆盖前一日时，
     * 首检会写入该切片之外的 class 字段。这里按完整排程窗口的自然班次顺序做去重合并，
     * 使目标量裁剪、日计划消费和单控整机扣账都使用结果中的全部真实正量。没有有效首检
     * 或首检未跨出当前切片时直接返回原列表，避免给跨日续排、尾部回填增加额外扫描。</p>
     *
     * @param productionShifts 当前业务日正式生产班次
     * @param firstInspectionAllocationPlan 本次真实首检分摊计划
     * @return 按完整排程窗口顺序排列的扣账班次
     */
    private List<LhShiftConfigVO> resolveFirstInspectionAccountingShifts(
            List<LhShiftConfigVO> productionShifts,
            FirstInspectionAllocationPlan firstInspectionAllocationPlan) {
        if (Objects.isNull(firstInspectionAllocationPlan)
                || !firstInspectionAllocationPlan.isValid()
                || CollectionUtils.isEmpty(firstInspectionAllocationPlan.getShiftAllocations())) {
            return productionShifts;
        }
        Map<Integer, LhShiftConfigVO> accountingShiftMap =
                new LinkedHashMap<Integer, LhShiftConfigVO>(8);
        if (!CollectionUtils.isEmpty(productionShifts)) {
            productionShifts.stream()
                    .filter(Objects::nonNull)
                    .filter(shift -> Objects.nonNull(shift.getShiftIndex()))
                    .forEach(shift -> accountingShiftMap.putIfAbsent(
                            shift.getShiftIndex(), shift));
        }
        int productionShiftCount = accountingShiftMap.size();
        for (FirstInspectionShiftAllocation allocation
                : firstInspectionAllocationPlan.getShiftAllocations()) {
            if (Objects.nonNull(allocation) && Objects.nonNull(allocation.getShift())
                    && Objects.nonNull(allocation.getShift().getShiftIndex())) {
                accountingShiftMap.putIfAbsent(
                        allocation.getShift().getShiftIndex(), allocation.getShift());
            }
        }
        boolean inspectionOutsideProductionSlice = accountingShiftMap.size() > productionShiftCount;
        if (!inspectionOutsideProductionSlice) {
            return productionShifts;
        }
        return accountingShiftMap.values().stream()
                .sorted(Comparator.comparing(
                        LhShiftConfigVO::getShiftStartDateTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    /**
     * 将单控整机组结果统一收敛为可均分到 L/R 两侧的偶数数量。
     *
     * <p>该方法只处理 SKU 实际消费账本已裁剪后的奇数尾量，不改变排程班次顺序。
     * 奇数尾量保留在 SKU 实际剩余账本中，供后续班次、未排或后续排程继续处理。</p>
     *
     * @param groupResult 单控整机组结果
     * @return 收敛后的整机组总排产量
     */
    private int retainWholeSingleControlPairedQty(LhScheduleResult groupResult) {
        if (Objects.isNull(groupResult)) {
            return 0;
        }
        boolean adjusted = false;
        for (int shiftIndex = 1; shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
            Integer groupQty = ShiftFieldUtil.getShiftPlanQty(groupResult, shiftIndex);
            if (Objects.isNull(groupQty) || groupQty <= 0 || groupQty % 2 == 0) {
                continue;
            }
            int pairedQty = groupQty - 1;
            ShiftFieldUtil.setShiftPlanQty(groupResult, shiftIndex, pairedQty,
                    ShiftFieldUtil.getShiftStartTime(groupResult, shiftIndex),
                    ShiftFieldUtil.getShiftEndTime(groupResult, shiftIndex));
            adjusted = true;
        }
        if (adjusted) {
            ShiftFieldUtil.syncDailyPlanQty(groupResult);
            log.info("单控整机按L/R成对收敛SKU实际消费账本裁剪尾量, materialCode: {}, machineCode: {}, pairedQty: {}",
                    groupResult.getMaterialCode(), groupResult.getLhMachineCode(), groupResult.getDailyPlanQty());
        }
        return Objects.nonNull(groupResult.getDailyPlanQty()) ? groupResult.getDailyPlanQty() : 0;
    }

    /**
     * 构建单控整机组结果，班次计划量为左右两侧合计量。
     *
     * @param sideResult 单侧结果
     * @return 整机组结果
     */
    private LhScheduleResult buildWholeSingleControlGroupResult(LhScheduleResult sideResult) {
        LhScheduleResult groupResult = new LhScheduleResult();
        BeanUtil.copyProperties(sideResult, groupResult);
        for (int shiftIndex = 1; shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
            Integer sideQty = ShiftFieldUtil.getShiftPlanQty(sideResult, shiftIndex);
            if (Objects.isNull(sideQty) || sideQty <= 0) {
                ShiftFieldUtil.setShiftPlanQty(groupResult, shiftIndex, sideQty,
                        ShiftFieldUtil.getShiftStartTime(sideResult, shiftIndex),
                        ShiftFieldUtil.getShiftEndTime(sideResult, shiftIndex));
                continue;
            }
            ShiftFieldUtil.setShiftPlanQty(groupResult, shiftIndex, sideQty * 2,
                    ShiftFieldUtil.getShiftStartTime(sideResult, shiftIndex),
                    ShiftFieldUtil.getShiftEndTime(sideResult, shiftIndex));
        }
        ShiftFieldUtil.syncDailyPlanQty(groupResult);
        return groupResult;
    }

    /**
     * 将整机组班次数量同步回 L/R 两侧。
     *
     * @param context 排程上下文
     * @param groupResult 整机组结果
     * @param primaryResult 主侧结果
     * @param pairResult 配对侧结果
     */
    private void copyWholeSingleControlGroupQtyToSides(LhScheduleContext context,
                                                       LhScheduleResult groupResult,
                                                       LhScheduleResult primaryResult,
                                                       LhScheduleResult pairResult) {
        for (int shiftIndex = 1; shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
            Integer groupQty = ShiftFieldUtil.getShiftPlanQty(groupResult, shiftIndex);
            int sideQty = Objects.isNull(groupQty) || groupQty <= 0 ? 0 : groupQty / 2;
            Date shiftStartTime = sideQty > 0 ? ShiftFieldUtil.getShiftStartTime(groupResult, shiftIndex) : null;
            Date shiftEndTime = sideQty > 0 ? ShiftFieldUtil.getShiftEndTime(groupResult, shiftIndex) : null;
            ShiftFieldUtil.setShiftPlanQty(primaryResult, shiftIndex, sideQty, shiftStartTime, shiftEndTime);
            ShiftFieldUtil.setShiftPlanQty(pairResult, shiftIndex, sideQty, shiftStartTime, shiftEndTime);
        }
        refreshResultSummary(context, primaryResult);
        refreshResultSummary(context, pairResult);
    }

    /**
     * 解析单控整机两侧合计实际排产量。
     *
     * @param context 排程上下文
     * @param primaryResult 主侧结果
     * @param pairResult 配对侧结果
     * @return 两侧合计排产量
     */
    private int resolveWholeSingleControlActualQty(LhScheduleContext context,
                                                   LhScheduleResult primaryResult,
                                                   LhScheduleResult pairResult) {
        refreshResultSummary(context, primaryResult);
        refreshResultSummary(context, pairResult);
        int primaryQty = Objects.nonNull(primaryResult.getDailyPlanQty()) ? primaryResult.getDailyPlanQty() : 0;
        int pairQty = Objects.nonNull(pairResult.getDailyPlanQty()) ? pairResult.getDailyPlanQty() : 0;
        return primaryQty + pairQty;
    }

    /**
     * 判断非收尾结果是否需要按严格日计划额度回裁。
     * <p>正式 SKU 可能因结构收尾判断带上 strict 标记，但最终结果仍是非收尾；
     * 这类场景应保留满班补齐量，避免单机可满足时被日计划账本裁空后续班次。</p>
     *
     * @param sku SKU排程DTO
     * @param endingResult 当前结果是否收尾
     * @return true-非收尾也需要严格回裁；false-允许保留满班补齐量
     */
    private boolean shouldApplyStrictNonEndingQuotaLimit(SkuScheduleDTO sku, boolean endingResult) {
        if (Objects.isNull(sku) || endingResult || !sku.isStrictTargetQty()) {
            return false;
        }
        return StringUtils.equals(ConstructionStageEnum.TRIAL.getCode(), sku.getConstructionStage())
                || sku.isStrictNewSpecShortageOnly();
    }

    /**
     * 解析新增排产实际扣账允许追补的截止日期。
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
                quotaMap, productionDate, resolveNewSpecShortageLookAheadDays(context),
                resolveScheduleTargetLocalDate(context));
    }

    /**
     * 解析当前排产块可消费的 dayN 截止日期。
     *
     * <p>滚动欠产始终允许先消费生产日及之前的额度；是否允许继续借用后续日计划由日内阶段决定。
     * 这样当天计划和增机台不会抢占提前生产阶段应竞争的未来额度。</p>
     *
     * @param context 排程上下文
     * @param quotaMap 日计划账本
     * @param productionDate 实际生产业务日
     * @param allowFutureQuotaConsumption 是否允许借用未来日计划
     * @return 本次日计划消费截止日期
     */
    private LocalDate resolveQuotaConsumeEndDate(LhScheduleContext context,
                                                 Map<LocalDate, SkuDailyPlanQuotaDTO> quotaMap,
                                                 LocalDate productionDate,
                                                 boolean allowFutureQuotaConsumption) {
        if (!allowFutureQuotaConsumption) {
            return productionDate;
        }
        // 提前生产临时账本已经按固定截止日有界构造，直接消费到其最后稀疏节点。
        return SkuDailyPlanQuotaUtil.resolveLastQuotaDate(quotaMap);
    }

    /**
     * 解析排程窗口 T 日。
     *
     * @param context 排程上下文
     * @return 排程窗口 T 日
     */
    private LocalDate resolveScheduleWindowStartLocalDate(LhScheduleContext context) {
        if (Objects.isNull(context)) {
            return null;
        }
        if (!CollectionUtils.isEmpty(context.getScheduleWindowShifts())) {
            for (LhShiftConfigVO shift : context.getScheduleWindowShifts()) {
                if (Objects.nonNull(shift) && Objects.nonNull(shift.getWorkDate())) {
                    return shift.getWorkDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                }
            }
        }
        if (Objects.isNull(context.getScheduleDate())) {
            return null;
        }
        return context.getScheduleDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * 解析排程请求对应的目标业务日。
     *
     * @param context 排程上下文
     * @return 目标业务日
     */
    private LocalDate resolveScheduleBusinessLocalDate(LhScheduleContext context) {
        if (Objects.isNull(context) || Objects.isNull(context.getScheduleTargetDate())) {
            return null;
        }
        return context.getScheduleTargetDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * 解析排程窗口结束业务日期。
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
     * 判断SKU是否需要继续尝试下一台机台排产。
     * <p>同时检查总量剩余和日计划额度剩余，两者都满足时才不需要继续。</p>
     *
     * @param sku SKU排程DTO
     * @return true-需要继续多机台排产，false-已满足
     */
    private boolean needMoreMachine(LhScheduleContext context, SkuScheduleDTO sku) {
        return DailyMachineExpansionPlanner.needMoreMachine(context, sku);
    }

    /**
     * 判断小额历史欠产是否允许继续向后滚动，不再为清欠产追加新机台。
     *
     * @param context 排程上下文
     * @param sku SKU排程DTO
     * @return true-后续日计划已满足，可停止扩机台
     */
    private boolean isSmallShortageRollingSatisfied(LhScheduleContext context, SkuScheduleDTO sku) {
        return DailyMachineExpansionPlanner.isSmallShortageRollingSatisfied(context, sku);
    }

    /**
     * 判断当前SKU是否属于欠产未超阈值的普通新增排产场景。
     *
     * @param context 排程上下文
     * @param sku SKU排程DTO
     * @return true-小额欠产允许后续滚动；false-仍按原有目标量扩机台
     */
    private boolean shouldAllowSmallShortageRolling(LhScheduleContext context, SkuScheduleDTO sku) {
        return DailyMachineExpansionPlanner.shouldAllowSmallShortageRolling(context, sku);
    }

    /**
     * 判断除首日以外的后续日计划额度是否已经满足。
     *
     * @param sku SKU排程DTO
     * @return true-后续日期无剩余额度；false-仍有后续日计划未满足
     */
    private boolean isFutureQuotaSatisfied(SkuScheduleDTO sku) {
        return DailyMachineExpansionPlanner.isFutureQuotaSatisfied(sku);
    }

    /**
     * 解析新增规格本轮可继续落结果的剩余量。
     * <p>按需求排产时，目标量保留月计划需求口径；多机台拆量则按日计划账本剩余额度收敛，
     * 确保窗口总量封顶由账本统一控制。</p>
     *
     * @param sku SKU排程DTO
     * @return 本轮可继续排产量
     */
    private int resolveSchedulableRemainingQty(LhScheduleContext context, SkuScheduleDTO sku) {
        if (sku == null) {
            return 0;
        }
        int substitutionExactScheduleQty = Objects.isNull(context)
                ? 0 : context.resolveSubstitutionExactScheduleQty(sku);
        if (substitutionExactScheduleQty > 0) {
            // B 联动迁移必须只消费本组截断尾量，原 B 的其他未排余额继续留在共享运行账本。
            return substitutionExactScheduleQty;
        }
        ProductionQuantityPolicy policy = ProductionQuantityPolicy.from(sku, sku.isStrictTargetQty());
        if (policy.isStrictUpperLimit() && sku.getSurplusQty() > 0) {
            /*
             * 收尾目标可能因满班、共用胎胚等前置规划被抬高，但真正可提交量只能取
             * “初始硫化余量－本批次已落地量”，不能继续沿用被抬高后的 targetScheduleQty。
             */
            return resolveStrictSurplusRemainingQty(context, sku);
        }
        if (!policy.isStrictUpperLimit()) {
            // 正规/量试非收尾的 dayN 只作为节奏与资源判断依据，实际排产量按SKU运行态账本共享扣减。
            return getTargetScheduleQtyResolver().resolveProductionRemainingQty(context, sku);
        }
        if (!StringUtils.equals(ConstructionStageEnum.TRIAL.getCode(), sku.getConstructionStage())
                && !sku.isStrictNewSpecShortageOnly()) {
            // 收尾目标、硫化余量、胎胚库存等严格业务目标不得被 dayN 或窗口计划改小。
            return sku.resolveTargetScheduleQty();
        }
        Map<LocalDate, SkuDailyPlanQuotaDTO> effectiveQuotaMap =
                Objects.isNull(context) ? sku.getDailyPlanQuotaMap()
                        : context.resolveEffectiveDailyPlanQuotaMap(sku);
        int remainingQuotaQty = SkuDailyPlanQuotaUtil.sumRemainingQty(effectiveQuotaMap);
        if (remainingQuotaQty > 0) {
            int windowRemainingQty = resolveWindowRemainingQty(context, sku);
            return Math.min(sku.resolveTargetScheduleQty(), Math.min(remainingQuotaQty, windowRemainingQty));
        }
        return sku.resolveTargetScheduleQty();
    }

    /**
     * 解析当前SKU尚未落地的真实硫化余量。
     *
     * <p>收尾规划阶段允许临时放大目标量用于选机，但最终结果必须按初始硫化余量扣除
     * 本批次同物料、同产品状态在续作、换活字块和新增阶段已经落地的全部结果。
     * 单控整机L/R两侧分别落结果，因此这里按结果行实际班次量合计，保证跨阶段和整机口径都不会漏扣。</p>
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @return 尚未落地的真实硫化余量
     */
    private int resolveActualSurplusRemainingQty(LhScheduleContext context, SkuScheduleDTO sku) {
        if (Objects.isNull(sku)) {
            return 0;
        }
        if (sku.getSurplusQty() <= 0) {
            return getTargetScheduleQtyResolver().resolveProductionRemainingQty(context, sku);
        }
        /*
         * 提前生产收尾 SKU 的严格上限是“未来计划月真实余量 + 当前业务月前日欠产”。
         * 中心运行视图存在时必须读取其冻结目标，不能退回仅包含未来月余量的 surplusQty。
         */
        EarlyProductionRuntimePlan runtimePlan = Objects.isNull(context)
                ? null : context.getEarlyProductionRuntimePlan(sku);
        int strictTargetQty;
        if (Objects.nonNull(runtimePlan) && runtimePlan.isActive()
                && Objects.nonNull(runtimePlan.getDecision())
                && runtimePlan.getDecision().isAllowed()) {
            /*
             * 只有已通过准入并完成临时账本初始化的激活运行视图才使用冻结目标；
             * 未激活视图（准入失败或尚未进入提前阈值）不得覆盖 SKU 真实硫化余量，
             * 否则会把严格收尾目标错误收敛为0。
             */
            strictTargetQty = Math.max(0, runtimePlan.getEffectiveTargetQty());
            log.info("严格收尾目标量使用提前生产运行视图, materialCode: {}, productStatus: {}, "
                            + "effectiveTargetQty: {}, surplusQty: {}",
                    sku.getMaterialCode(), sku.getProductStatus(),
                    runtimePlan.getEffectiveTargetQty(), Math.max(0, sku.getSurplusQty()));
        } else {
            if (Objects.nonNull(runtimePlan)) {
                log.info("提前生产运行视图未激活，严格收尾目标量回退SKU硫化余量, materialCode: {}, "
                                + "productStatus: {}, active: {}, effectiveTargetQty: {}, surplusQty: {}",
                        sku.getMaterialCode(), sku.getProductStatus(), runtimePlan.isActive(),
                        runtimePlan.getEffectiveTargetQty(), Math.max(0, sku.getSurplusQty()));
            }
            strictTargetQty = Math.max(0, sku.getSurplusQty());
        }
        if (Objects.isNull(context) || CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return strictTargetQty;
        }
        int scheduledQty = context.getScheduleResultList().stream()
                .filter(Objects::nonNull)
                .filter(result -> StringUtils.equals(
                        sku.getMaterialCode(), result.getMaterialCode()))
                .filter(result -> StringUtils.equals(
                        StringUtils.trimToEmpty(sku.getProductStatus()),
                        StringUtils.trimToEmpty(result.getProductStatus())))
                .mapToInt(ShiftFieldUtil::resolveScheduledQty)
                .sum();
        int resultRemainingQty = Math.max(0, strictTargetQty - scheduledQty);
        log.info("真实硫化余量跨阶段核对, materialCode: {}, productStatus: {}, strictTargetQty: {}, "
                        + "本批次全部阶段已排量: {}, 真实硫化余量剩余: {}",
                sku.getMaterialCode(), sku.getProductStatus(), strictTargetQty,
                scheduledQty, resultRemainingQty);
        return resultRemainingQty;
    }

    /**
     * 解析当前SKU严格结果提交时允许继续落地的数量。
     *
     * <p>真实硫化余量负责判断是否进入最终严格收尾；中心实际消费账本只在已经确认严格
     * 收尾后参与防重复消费。两个口径必须分开，避免续作补偿的窗口账本尾量把普通非收尾
     * 在机块误判成真实收尾。</p>
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @return 严格结果提交时允许继续落地的数量
     */
    private int resolveStrictSurplusRemainingQty(LhScheduleContext context, SkuScheduleDTO sku) {
        int actualSurplusRemainingQty = this.resolveActualSurplusRemainingQty(context, sku);
        int productionRemainingQty = getTargetScheduleQtyResolver()
                .resolveProductionRemainingQty(context, sku);
        int strictRemainingQty = Math.min(
                Math.max(0, actualSurplusRemainingQty), Math.max(0, productionRemainingQty));
        log.info("严格收尾账本剩余量核对, materialCode: {}, productStatus: {}, "
                        + "真实硫化余量剩余: {}, 中心账本剩余: {}, 最终可排剩余: {}",
                Objects.isNull(sku) ? null : sku.getMaterialCode(),
                Objects.isNull(sku) ? null : sku.getProductStatus(),
                actualSurplusRemainingQty, productionRemainingQty, strictRemainingQty);
        return strictRemainingQty;
    }

    /**
     * 解析新增规格本轮可继续落结果的剩余量。
     * <p>仅保留给历史单元测试的无上下文入口；真实排程必须调用带上下文方法，
     * 以便按“物料+产品状态”共享运行态账本。</p>
     *
     * @param sku SKU排程DTO
     * @return 本轮可继续排产量
     */
    private int resolveSchedulableRemainingQty(SkuScheduleDTO sku) {
        return resolveSchedulableRemainingQty(null, sku);
    }

    /**
     * 解析窗口总量封顶后的剩余可排量。
     *
     * @param context 排程上下文
     * @param sku SKU排程DTO
     * @return 窗口剩余可排量
     */
    private int resolveWindowRemainingQty(LhScheduleContext context, SkuScheduleDTO sku) {
        Map<LocalDate, SkuDailyPlanQuotaDTO> quotaMap =
                Objects.isNull(context) ? sku.getDailyPlanQuotaMap()
                        : context.resolveEffectiveDailyPlanQuotaMap(sku);
        if (sku.getWindowPlanQty() <= 0 || CollectionUtils.isEmpty(quotaMap)) {
            return Integer.MAX_VALUE;
        }
        int scheduledQty = quotaMap.values().stream()
                .filter(day -> day != null)
                .mapToInt(day -> Math.max(0, day.getScheduledQty()))
                .sum();
        return Math.max(0, sku.getWindowPlanQty() - scheduledQty);
    }

    /**
     * 注册机台排程分配记录
     */
    private void registerMachineAssignment(LhScheduleContext context, String machineCode, LhScheduleResult result) {
        context.getMachineAssignmentMap()
                .computeIfAbsent(machineCode, k -> new ArrayList<>())
                .add(result);
    }

    /**
     * 在所有SKU列表中查找指定materialCode的DTO
     */
    private SkuScheduleDTO findSkuDto(LhScheduleContext context, String materialCode) {
        if (context == null || StringUtils.isEmpty(materialCode)) {
            return null;
        }
        for (SkuScheduleDTO sku : context.getNewSpecSkuList()) {
            if (materialCode.equals(sku.getMaterialCode())) {
                return sku;
            }
        }
        for (SkuScheduleDTO sku : context.getContinuousSkuList()) {
            if (materialCode.equals(sku.getMaterialCode())) {
                return sku;
            }
        }
        if (!CollectionUtils.isEmpty(context.getStructureSkuMap())) {
            for (List<SkuScheduleDTO> skuList : context.getStructureSkuMap().values()) {
                if (CollectionUtils.isEmpty(skuList)) {
                    continue;
                }
                for (SkuScheduleDTO sku : skuList) {
                    if (materialCode.equals(sku.getMaterialCode())) {
                        return sku;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 按物料编码与产品状态精确查找SKU。
     *
     * @param context 排程上下文
     * @param materialCode 物料编码
     * @param productStatus 产品状态
     * @return 精确匹配的SKU，未找到返回null
     */
    private SkuScheduleDTO findSkuDto(LhScheduleContext context,
                                      String materialCode,
                                      String productStatus) {
        if (context == null || StringUtils.isEmpty(materialCode)) {
            return null;
        }
        String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(materialCode, productStatus);
        SkuScheduleDTO indexedSku = context.getAllSkuScheduleDtoMap().get(skuKey);
        if (indexedSku != null) {
            return indexedSku;
        }
        for (SkuScheduleDTO sku : context.getNewSpecSkuList()) {
            if (isSameSku(materialCode, productStatus, sku)) {
                return sku;
            }
        }
        for (SkuScheduleDTO sku : context.getContinuousSkuList()) {
            if (isSameSku(materialCode, productStatus, sku)) {
                return sku;
            }
        }
        return null;
    }

    /**
     * 判断物料编码和产品状态是否同时一致。
     *
     * @param materialCode 物料编码
     * @param productStatus 产品状态
     * @param sku 待比较SKU
     * @return true-同一业务SKU
     */
    private boolean isSameSku(String materialCode, String productStatus, SkuScheduleDTO sku) {
        return sku != null
                && StringUtils.equals(materialCode, sku.getMaterialCode())
                && StringUtils.equals(StringUtils.trimToEmpty(productStatus),
                StringUtils.trimToEmpty(sku.getProductStatus()));
    }

    /**
     * 计算新增零计划结果转未排时的剩余待排数量。
     *
     * @param context 排程上下文
     * @param materialCode 物料编码
     * @return 未排数量
     */
    private int resolveRemainingUnscheduledQty(LhScheduleContext context, SkuScheduleDTO sku) {
        if (sku == null) {
            return 0;
        }
        int targetScheduleQty = sku.resolveTargetScheduleQty();
        int retainedQty = resolveEffectiveScheduledQty(
                context, sku.getMaterialCode(), sku.getProductStatus());
        return Math.max(targetScheduleQty - retainedQty, 0);
    }

    /**
     * 统计同物料仍保留在新增结果列表中的有效计划量。
     *
     * @param context 排程上下文
     * @param materialCode 物料编码
     * @param productStatus 产品状态
     * @return 有效计划量
     */
    private int resolveEffectiveScheduledQty(LhScheduleContext context,
                                             String materialCode,
                                             String productStatus) {
        if (context == null || StringUtils.isEmpty(materialCode) || CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return 0;
        }
        int totalQty = 0;
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (result == null
                    || !StringUtils.equals(materialCode, result.getMaterialCode())
                    || !StringUtils.equals(StringUtils.trimToEmpty(productStatus),
                    StringUtils.trimToEmpty(result.getProductStatus()))
                    || !NEW_SPEC_SCHEDULE_TYPE.equals(result.getScheduleType())
                    || "1".equals(result.getIsTypeBlock())  // 排除换活字块
                    || result.getDailyPlanQty() == null
                    || result.getDailyPlanQty() <= 0) {
                continue;
            }
            totalQty += result.getDailyPlanQty();
        }
        return totalQty;
    }

    /**
     * 按物料维度写入或合并未排结果。
     *
     * @param context 排程上下文
     * @param sku 来源SKU
     * @param unscheduledQty 未排数量
     */
    private void mergeUnscheduledResultBySku(LhScheduleContext context, SkuScheduleDTO sku, int unscheduledQty) {
        if (context == null || sku == null || StringUtils.isEmpty(sku.getMaterialCode()) || unscheduledQty <= 0) {
            return;
        }
        LhUnscheduledResult existing = findUnscheduledResultBySku(
                context, sku.getMaterialCode(), sku.getProductStatus());
        if (existing != null) {
            int existingQty = existing.getUnscheduledQty() != null ? existing.getUnscheduledQty() : 0;
            existing.setUnscheduledQty(existingQty + unscheduledQty);
            if (StringUtils.isEmpty(existing.getUnscheduledReason())) {
                existing.setUnscheduledReason(ZERO_PLAN_UNSCHEDULED_REASON);
            }
            return;
        }
        LhUnscheduledResult unscheduled = new LhUnscheduledResult();
        unscheduled.setFactoryCode(context.getFactoryCode());
        unscheduled.setBatchNo(context.getBatchNo());
        unscheduled.setScheduleDate(context.getScheduleTargetDate());
        unscheduled.setMaterialCode(sku.getMaterialCode());
        unscheduled.setProductStatus(sku.getProductStatus());
        unscheduled.setMaterialDesc(sku.getMaterialDesc());
        unscheduled.setStructureName(sku.getStructureName());
        unscheduled.setMainMaterialDesc(sku.getMainMaterialDesc());
        unscheduled.setSpecCode(sku.getSpecCode());
        unscheduled.setEmbryoCode(sku.getEmbryoCode());
        unscheduled.setMouldQty(sku.getMouldQty());
        unscheduled.setUnscheduledQty(unscheduledQty);
        unscheduled.setUnscheduledReason(ZERO_PLAN_UNSCHEDULED_REASON);
        unscheduled.setDataSource(AUTO_DATA_SOURCE);
        unscheduled.setIsDelete(0);
        context.getUnscheduledResultList().add(unscheduled);
    }

    /**
     * 查找已存在的未排结果。
     *
     * @param context 排程上下文
     * @param materialCode 物料编码
     * @param productStatus 产品状态
     * @return 未排结果
     */
    private LhUnscheduledResult findUnscheduledResultBySku(LhScheduleContext context,
                                                           String materialCode,
                                                           String productStatus) {
        if (context == null || CollectionUtils.isEmpty(context.getUnscheduledResultList())) {
            return null;
        }
        for (LhUnscheduledResult unscheduledResult : context.getUnscheduledResultList()) {
            if (StringUtils.equals(materialCode, unscheduledResult.getMaterialCode())
                    && StringUtils.equals(StringUtils.trimToEmpty(productStatus),
                    StringUtils.trimToEmpty(unscheduledResult.getProductStatus()))) {
                return unscheduledResult;
            }
        }
        return null;
    }

    /**
     * 对未排结果按物料编码与产品状态去重合并。
     *
     * @param context 排程上下文
     */
    private void normalizeUnscheduledResultsBySku(LhScheduleContext context) {
        if (context == null || CollectionUtils.isEmpty(context.getUnscheduledResultList())) {
            return;
        }
        Map<String, LhUnscheduledResult> mergedMap = new LinkedHashMap<>(context.getUnscheduledResultList().size());
        for (LhUnscheduledResult unscheduledResult : context.getUnscheduledResultList()) {
            if (unscheduledResult == null
                    || StringUtils.isEmpty(unscheduledResult.getMaterialCode())) {
                continue;
            }
            // 零量未排中的“日计划准入拦截”与“S4.3前置剔除（共用胎胚零余量、无排产目标量、
            // 余量与胎胚库存均为0）”代表“有SKU但无排产任务”，是明确的未排记录，必须保留落库；
            // 其余零量/负量残留视为排产主链已消纳完毕，按原逻辑跳过。
            if (Objects.isNull(unscheduledResult.getUnscheduledQty())
                    || (unscheduledResult.getUnscheduledQty() <= 0
                    && !isRetainableZeroQtyUnscheduledReason(unscheduledResult.getUnscheduledReason()))) {
                continue;
            }
            String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                    unscheduledResult.getMaterialCode(), unscheduledResult.getProductStatus());
            if (!mergedMap.containsKey(skuKey)) {
                mergedMap.put(skuKey, unscheduledResult);
                continue;
            }
            LhUnscheduledResult existing = mergedMap.get(skuKey);
            int existingQty = existing.getUnscheduledQty() != null ? existing.getUnscheduledQty() : 0;
            int currentQty = unscheduledResult.getUnscheduledQty() != null ? unscheduledResult.getUnscheduledQty() : 0;
            existing.setUnscheduledQty(existingQty + currentQty);
            if (StringUtils.isEmpty(existing.getUnscheduledReason())) {
                existing.setUnscheduledReason(unscheduledResult.getUnscheduledReason());
            }
        }
        context.getUnscheduledResultList().clear();
        context.getUnscheduledResultList().addAll(mergedMap.values());
    }

    /**
     * 判断未排原因是否为日计划准入拦截类。
     * <p>含非续作SKU日计划准入（{@link PendingSkuUnscheduledRule#DAILY_PLAN_ADMISSION_UNSCHEDULED_REASON}）
     * 与续作试制量试日计划准入（{@link PendingSkuUnscheduledRule#CONTINUOUS_TRIAL_DAILY_PLAN_ADMISSION_UNSCHEDULED_REASON}）。
     * 该类未排结果未排数量恒为0，代表窗口内无日计划量、本就无排产任务，
     * 归并去重时不得因数量为0而丢弃，否则SKU不会出现在未排结果表中。</p>
     *
     * @param reason 未排原因
     * @return true-准入拦截类原因；false-其他
     */
    private boolean isDailyPlanAdmissionUnscheduledReason(String reason) {
        return StringUtils.equals(PendingSkuUnscheduledRule.DAILY_PLAN_ADMISSION_UNSCHEDULED_REASON, reason)
                || StringUtils.equals(PendingSkuUnscheduledRule.CONTINUOUS_TRIAL_DAILY_PLAN_ADMISSION_UNSCHEDULED_REASON, reason);
    }

    /**
     * 判断零量未排记录是否属于必须保留落库的明确未排原因。
     * <p>除日计划准入拦截类外，S4.3阶段前置剔除产生的零量未排同样代表“有SKU但无排产任务”，
     * 包括：共用胎胚且硫化余量为0预剔除、无排产目标量、余量与胎胚库存均为0三类。
     * 若归并去重时因数量为0丢弃，SKU会在排程结果表和未排结果表同时消失，无法对账。</p>
     *
     * @param reason 未排原因
     * @return true-必须保留的零量未排原因；false-其他
     */
    private boolean isRetainableZeroQtyUnscheduledReason(String reason) {
        if (StringUtils.isEmpty(reason)) {
            return false;
        }
        // 日计划准入拦截类原有白名单保持不变
        if (isDailyPlanAdmissionUnscheduledReason(reason)) {
            return true;
        }
        // S4.3共用胎胚零余量预剔除未排（未排数量恒为0）
        if (StringUtils.equals(SHARED_EMBRYO_ZERO_SURPLUS_UNSCHEDULED_REASON, reason)) {
            return true;
        }
        // S4.3无目标量未排原因为“物料：xxx + 固定后缀”模板，按后缀识别
        return StringUtils.endsWith(reason, NO_PLAN_QTY_UNSCHEDULED_REASON_SUFFIX)
                || StringUtils.endsWith(reason, ZERO_SURPLUS_AND_EMBRYO_UNSCHEDULED_REASON_SUFFIX);
    }

    /**
     * 将被移除的零计划结果同步从机台分配记录中清理掉。
     *
     * @param context 排程上下文
     * @param resultsToRemove 待移除结果
     */
    private void removeResultsFromMachineAssignments(LhScheduleContext context, List<LhScheduleResult> resultsToRemove) {
        if (context == null
                || CollectionUtils.isEmpty(resultsToRemove)
                || CollectionUtils.isEmpty(context.getMachineAssignmentMap())) {
            return;
        }
        Iterator<Map.Entry<String, List<LhScheduleResult>>> iterator =
                context.getMachineAssignmentMap().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<LhScheduleResult>> entry = iterator.next();
            List<LhScheduleResult> assignedResults = entry.getValue();
            if (CollectionUtils.isEmpty(assignedResults)) {
                iterator.remove();
                continue;
            }
            assignedResults.removeAll(resultsToRemove);
            if (assignedResults.isEmpty()) {
                iterator.remove();
            }
        }
    }

    /**
     * 回写多机台新增结果的SKU完整胎胚库存。
     * <p>同SKU多机台仅拆分排产量，不进入共用胎胚库存分摊。</p>
     *
     * @param context 排程上下文
     */
    private void retainMultiMachineEmbryoStock(LhScheduleContext context) {
        if (context == null || CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return;
        }
        // 按物料状态复合键汇总新增结果（排除换活字块）。
        Map<String, List<LhScheduleResult>> skuResultsMap = new LinkedHashMap<>(16);
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (!NEW_SPEC_SCHEDULE_TYPE.equals(result.getScheduleType())
                    || "1".equals(result.getIsTypeBlock())) {
                continue;
            }
            if (StringUtils.isEmpty(result.getMaterialCode())) {
                continue;
            }
            if (result.getDailyPlanQty() == null || result.getDailyPlanQty() <= 0) {
                continue;
            }
            String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                    result.getMaterialCode(), result.getProductStatus());
            skuResultsMap.computeIfAbsent(skuKey, k -> new ArrayList<>()).add(result);
        }
        // 同一物料的每条新增机台结果统一保留SKU级胎胚库存。
        for (Map.Entry<String, List<LhScheduleResult>> entry : skuResultsMap.entrySet()) {
            List<LhScheduleResult> materialResults = entry.getValue();
            if (materialResults.size() <= 1) {
                continue;
            }
            LhScheduleResult firstResult = materialResults.get(0);
            String materialCode = firstResult.getMaterialCode();
            SkuScheduleDTO sku = findSkuDto(
                    context, materialCode, firstResult.getProductStatus());
            if (sku == null) {
                continue;
            }
            int totalEmbryoStock = Math.max(0, sku.getEmbryoStock());
            // 同SKU多机台只拆分排产量，每条结果都保留SKU已分配的完整胎胚库存。
            LhMultiMachineDistributionUtil.retainFullEmbryoStockForSingleMaterial(
                    materialResults, totalEmbryoStock);
            log.debug("多机台新增胎胚库存完整回写完成, materialCode: {}, 机台数: {}, SKU胎胚库存: {}",
                    materialCode, materialResults.size(), totalEmbryoStock);
        }
    }

    /**
     * 新增零计划结果移除后，按最终保留结果重新同步机台状态。
     *
     * @param context 排程上下文
     */
    private void syncMachineStateAfterNewAdjust(LhScheduleContext context) {
        if (context == null || CollectionUtils.isEmpty(context.getMachineScheduleMap())) {
            return;
        }
        for (Map.Entry<String, MachineScheduleDTO> entry : context.getMachineScheduleMap().entrySet()) {
            String machineCode = entry.getKey();
            MachineScheduleDTO machine = entry.getValue();
            List<LhScheduleResult> assignedResults = context.getMachineAssignmentMap().get(machineCode);
            LhScheduleResult latestResult = resolveLatestAssignedResult(context, assignedResults);
            if (latestResult != null) {
                LhScheduleResult previousResult = resolvePreviousAssignedResult(assignedResults, latestResult);
                applyMachineStateFromResult(context, machine, latestResult, previousResult);
                continue;
            }
            restoreMachineStateFromInitial(context, machineCode, machine);
        }
    }

    /**
     * 查找机台当前保留的最新有效结果。
     *
     * @param context 排程上下文
     * @param assignedResults 机台已分配结果
     * @return 最新有效结果
     */
    private LhScheduleResult resolveLatestAssignedResult(LhScheduleContext context,
                                                         List<LhScheduleResult> assignedResults) {
        if (CollectionUtils.isEmpty(assignedResults)) {
            return null;
        }
        return assignedResults.stream()
                .filter(result -> result != null
                        && (result.getDailyPlanQty() != null && result.getDailyPlanQty() > 0)
                        && result.getSpecEndTime() != null)
                .max(Comparator.comparing(LhScheduleResult::getSpecEndTime))
                .orElse(null);
    }

    /**
     * 查找机台当前保留结果中的上一条有效结果。
     *
     * @param assignedResults 机台保留结果
     * @param latestResult 最新有效结果
     * @return 上一条有效结果
     */
    private LhScheduleResult resolvePreviousAssignedResult(List<LhScheduleResult> assignedResults,
                                                           LhScheduleResult latestResult) {
        if (CollectionUtils.isEmpty(assignedResults) || latestResult == null) {
            return null;
        }
        return assignedResults.stream()
                .filter(result -> result != null
                        && result != latestResult
                        && result.getDailyPlanQty() != null
                        && result.getDailyPlanQty() > 0
                        && result.getSpecEndTime() != null)
                .max(Comparator.comparing(LhScheduleResult::getSpecEndTime))
                .orElse(null);
    }

    /**
     * 使用最新有效结果回写机台状态。
     *
     * @param machine 机台
     * @param result 最新有效结果
     */
    private void applyMachineStateFromResult(LhScheduleContext context,
                                             MachineScheduleDTO machine,
                                             LhScheduleResult result,
                                             LhScheduleResult previousResult) {
        if (context == null || machine == null || result == null) {
            return;
        }
        String previousMaterialCode = null;
        String previousMaterialDesc = null;
        if (previousResult != null) {
            previousMaterialCode = previousResult.getMaterialCode();
            previousMaterialDesc = previousResult.getMaterialDesc();
        } else if (StringUtils.isNotEmpty(machine.getMachineCode())) {
            MachineScheduleDTO initialMachine = context.getInitialMachineScheduleMap().get(machine.getMachineCode());
            if (initialMachine != null) {
                previousMaterialCode = initialMachine.getCurrentMaterialCode();
                previousMaterialDesc = initialMachine.getCurrentMaterialDesc();
            }
        }
        SkuScheduleDTO sku = findSkuDto(
                context, result.getMaterialCode(), result.getProductStatus());
        machine.setCurrentMaterialCode(result.getMaterialCode());
        machine.setCurrentMaterialDesc(result.getMaterialDesc());
        machine.setPreviousMaterialCode(previousMaterialCode);
        machine.setPreviousMaterialDesc(previousMaterialDesc);
        machine.setPreviousSpecCode(result.getSpecCode());
        machine.setPreviousProSize(sku != null ? sku.getProSize() : null);
        machine.setEstimatedEndTime(result.getSpecEndTime());
        // 按结果行标识同步机台运行态的结构收尾对齐标识。
        machine.setStructureEndingAligned(
                "1".equals(result.getIsStructureMinMachineRetained()));
    }

    /**
     * 当前机台无有效排程结果时，回退到初始化快照。
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @param machine 机台
     */
    private void restoreMachineStateFromInitial(LhScheduleContext context, String machineCode, MachineScheduleDTO machine) {
        if (context == null || machine == null || StringUtils.isEmpty(machineCode)) {
            return;
        }
        MachineScheduleDTO initialMachine = context.getInitialMachineScheduleMap().get(machineCode);
        if (initialMachine == null) {
            return;
        }
        machine.setCurrentMaterialCode(initialMachine.getCurrentMaterialCode());
        machine.setCurrentMaterialDesc(initialMachine.getCurrentMaterialDesc());
        machine.setPreviousMaterialCode(initialMachine.getPreviousMaterialCode());
        machine.setPreviousMaterialDesc(initialMachine.getPreviousMaterialDesc());
        machine.setPreviousSpecCode(initialMachine.getPreviousSpecCode());
        machine.setPreviousProSize(initialMachine.getPreviousProSize());
        machine.setEstimatedEndTime(initialMachine.getEstimatedEndTime());
        machine.setStructureEndingAligned(false);
    }
}
