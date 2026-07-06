package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.cx.entity.CxMaterialEnding;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.vo.MonthPlanProductLhCapacityVo;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.cx.service.engine.CoreScheduleAlgorithmService;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.MdmMonthSurplus;
import com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio;
import com.zlt.aps.mp.api.domain.entity.MpCxCapacityConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 任务分组服务 — 成型排程 S5.2 阶段
 *
 * <p>将硫化需求转化为成型排程任务，按胎胚+物料维度分组，计算各任务的排产属性。
 * 核心流程：
 * <ol>
 *   <li>S5.2.1 遍历硫化排程结果，为每条记录构建 DailyEmbryoTask</li>
 *   <li>S5.2.2 分配库存（按硫化任务需求比例）</li>
 *   <li>S5.2.3 计算库存可支撑时长（stockHours）</li>
 *   <li>S5.2.4 计算收尾属性（余量、紧急度、优先级）</li>
 *   <li>S5.2.5 计算待排产量（库存对冲 × 损耗率 × 整车取整）</li>
 *   <li>S5.2.6 收尾余量处理（舍弃/按实/补车）</li>
 *   <li>S5.2.7 开停产特殊处理</li>
 *   <li>S5.2.8 试制任务双数处理</li>
 *   <li>S5.2.9 按任务类型分组返回（续作/试制/新增）</li>
 * </ol>
 *
 * @author APS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskGroupService {

    // ==================== 业务阈值常量 ====================

    /** 收尾舍弃阈值默认值：非主销产品余量≤此值时舍弃（条） */
    private static final int DEFAULT_ENDING_DISCARD_THRESHOLD = 2;

    /** 成型余量紧急阈值默认值：成型余量低于此值标记为紧急收尾（条） */
    private static final int DEFAULT_ENDING_URGENT_FORMING_REMAINDER = 400;

    /** 近期收尾天数阈值默认值（10 天内） */
    private static final int DEFAULT_ENDING_DAYS_THRESHOLD = 10;

    /** 紧急收尾天数阈值默认值（3 天内） */
    private static final int DEFAULT_URGENT_ENDING_DAYS = 3;

    /** 推荐机台最大硫化机数默认值 */
    private static final int DEFAULT_MAX_LH_MACHINE_QTY = 10;

    // ==================== 参数配置编码 ====================

    /** 参数编码：收尾舍弃阈值 */
    private static final String PARAM_ENDING_DISCARD_THRESHOLD = "SYS04050001";

    /** 参数编码：成型余量紧急阈值 */
    private static final String PARAM_ENDING_URGENT_FORMING_REMAINDER = "SYS04050002";

    /** 参数编码：近期收尾天数阈值 */
    private static final String PARAM_ENDING_DAYS_THRESHOLD = "SYS04050003";

    /** 参数编码：紧急收尾天数阈值 */
    private static final String PARAM_URGENT_ENDING_DAYS = "SYS04050004";

    private static final String PARAM_EMBRYO_WAREHOUSE_CAPACITY = "SYS04060001";

    private static final String PARAM_EMBRYO_WAREHOUSE_CAPACITY_RATIO = "SYS04060002";

    private static final int DEFAULT_EMBRYO_WAREHOUSE_CAPACITY = 0;

    private static final double DEFAULT_EMBRYO_WAREHOUSE_CAPACITY_RATIO = 0.9;

    /** 库存高水位阈值（小时）：超过此值降低优先级 */
    private static final int STOCK_HIGH_HOURS_THRESHOLD = 18;

    /** 参数编码：可供硫化时长封顶阈值（小时） */
    private static final String PARAM_STOCK_HOURS_CAP = "SYS04080001";

    /** 参数编码：可供硫化时长封顶开关（Y=开启，N=关闭） */
    private static final String PARAM_STOCK_HOURS_CAP_ENABLED = "SYS04080005";

    /** 参数编码：提前生产同英寸切换耗时（小时） */
    private static final String PARAM_ADVANCE_SAME_INCH_SWITCH_HOURS = "SYS04020004";

    /** 参数编码：提前生产不同英寸切换耗时（小时） */
    private static final String PARAM_ADVANCE_DIFF_INCH_SWITCH_HOURS = "SYS04020005";

    /** 默认：同英寸切换耗时（小时） */
    private static final int DEFAULT_SAME_INCH_SWITCH_HOURS = 2;

    /** 默认：不同英寸切换耗时（小时） */
    private static final int DEFAULT_DIFF_INCH_SWITCH_HOURS = 8;

    /** 单个班次总秒数（8小时） */
    private static final int SECONDS_PER_SHIFT = 8 * 60 * 60;

    /** 秒转小时的除数 */
    private static final int SECONDS_PER_HOUR = 3600;

    // ==================== 优先级分值常量 ====================


    /** 有计划量+3天内收尾 基础优先级 */
    private static final int PRIORITY_HAS_PLAN_URGENT = 9000;

    /** 有计划量+10天内收尾 基础优先级 */
    private static final int PRIORITY_HAS_PLAN_NEAR = 8000;

    /** 有计划量+大于10天收尾 基础优先级 */
    private static final int PRIORITY_HAS_PLAN_NORMAL = 7000;

    /** 无计划量+3天内紧急收尾 基础优先级 */
    private static final int PRIORITY_NO_PLAN_URGENT = 6000;

    /** 无计划量+10天内收尾 基础优先级 */
    private static final int PRIORITY_NO_PLAN_NEAR = 5000;

    /** 无计划量+大于10天收尾 基础优先级 */
    private static final int PRIORITY_NO_PLAN_NORMAL = 4000;

    /** 试制量试任务优先级加分（层级内细分：试制量试 > 续作 > 非续作） */
    private static final int PRIORITY_TRIAL = 1500;

    /** 续作任务优先级加分 */
    private static final int PRIORITY_CONTINUE = 800;

    /** 库存第三层细分封顶值（库存扣分上限，确保不覆盖第二层细分） */
    private static final int PRIORITY_STOCK_TIEBREAKER_MAX = 499;

    // ==================== 施工阶段常量 ====================

    /** 施工阶段：试制 */
    private static final String STAGE_TRIAL = "01";

    /** 施工阶段：量试 */
    private static final String STAGE_PRODUCTION_TRIAL = "02";

    // ==================== 依赖注入 ====================

    private final ProductionCalculator productionCalculator;
    private final ScheduleDayTypeHelper scheduleDayTypeHelper;

    // ==================== 内部类 ====================

    /**
     * 任务分组结果
     */
    @lombok.Data
    public static class TaskGroupResult {
        /** 续作任务：当前机台在产的胎胚 */
        private List<CoreScheduleAlgorithmService.DailyEmbryoTask> continueTasks = new ArrayList<>();
        /** 试制任务：试制/量试任务 */
        private List<CoreScheduleAlgorithmService.DailyEmbryoTask> trialTasks = new ArrayList<>();
        /** 新增任务：非续作、非试制的常规任务 */
        private List<CoreScheduleAlgorithmService.DailyEmbryoTask> newTasks = new ArrayList<>();
    }

    // ==================== 公开方法 ====================

    /**
     * S5.2 排程分类与余量计算
     *
     * <p>将硫化任务分为三类：
     * <ul>
     *   <li>续作任务：当前机台在产的胎胚，需要继续生产</li>
     *   <li>试制任务：试制/量试任务</li>
     *   <li>新增任务：非续作、非试制的常规任务</li>
     * </ul>
     *
     * @param context                排程上下文
     * @param machineOnlineEmbryoMap 机台在产胎胚映射
     * @param scheduleDate           排程日期
     * @param dayShifts              当前天的班次配置列表（用于获取对应班次的硫化计划量）
     * @return 任务分组结果
     */
    public TaskGroupResult groupTasks(
            ScheduleContextVo context,
            Map<String, Set<String>> machineOnlineEmbryoMap,
            LocalDate scheduleDate,
            List<CxShiftConfig> dayShifts) {

        TaskGroupResult result = new TaskGroupResult();

        List<LhScheduleResult> lhScheduleResults = context.getLhScheduleResults();
        log.info("【任务分组】收到 {} 条硫化记录", lhScheduleResults.size());

        // 构建基础映射
        Map<String, MdmMaterialInfo> materialMap = buildMaterialMap(context);
        Map<String, CxStock> stockMap = buildStockMap(context);

        // 一次性加载所有参数配置
        int endingDiscardThreshold = getEndingDiscardThreshold(context);
        int endingUrgentFormingRemainder = getEndingUrgentFormingRemainder(context);
        int endingDaysThreshold = getEndingDaysThreshold(context);
        int urgentEndingDays = getUrgentEndingDays(context);
        log.info("【收尾参数配置】收尾舍弃阈值={}, 成型余量紧急阈值={}, 近期收尾天数={}, 紧急收尾天数={}",
                endingDiscardThreshold, endingUrgentFormingRemainder, endingDaysThreshold, urgentEndingDays);

        int stockHoursCap = getStockHoursCap(context);
        boolean stockHoursCapEnabled = isStockHoursCapEnabled(context);
        log.info("【立库管控参数】可供硫化时长封顶阈值={}h, 开关={}", stockHoursCap, stockHoursCapEnabled ? "开启" : "关闭");

        // 判断当前班次是否为开产班次（用于提前过滤关键产品）
        boolean isOpeningShift = false;
        if (dayShifts != null && !dayShifts.isEmpty()) {
            CxShiftConfig currentShift = dayShifts.get(0);
            if (currentShift.getDayShiftOrder() != null) {
                LocalDate currentScheduleDate = context.getCurrentScheduleDate();
                String factoryCode = context.getFactoryCode();
                ScheduleDayTypeHelper.ShiftType st = scheduleDayTypeHelper.determineShiftType(
                        currentScheduleDate, currentShift.getDayShiftOrder(), factoryCode);
                isOpeningShift = st == ScheduleDayTypeHelper.ShiftType.OPEN_START;
            }
        }

        if (machineOnlineEmbryoMap == null) {
            machineOnlineEmbryoMap = new HashMap<>();
        }

        // 获取当前班次的排量（每个班次只处理自己班次有排量的任务）
        final int currentClassIndex = getCurrentClassIndex(dayShifts);

        // 直接遍历每条硫化记录，为每条记录创建独立的任务
        int skippedNullEmbryo = 0;
        int skippedNullTask = 0;
        int skippedVulcanizeSurplusZero = 0;  // 硫化余量<=0跳过的任务数
        int skippedFormingRemainderZero = 0;  // 成型余量<=0跳过的任务数

        // 跟踪每个物料已使用的成型余量（用于多任务共享同一物料的场景）
        Map<String, Integer> materialUsedFormingRemainder = new HashMap<>();
        // 跟踪每个物料已处理的任务列表（用于回溯更新 isLastEndingBatch）
        Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> materialTasksMap = new HashMap<>();

        // 零净需求暂存列表（第一轮完成后按结构分组分配剩余产能）
        List<CoreScheduleAlgorithmService.DailyEmbryoTask> deferredTasks = new ArrayList<>();
        // 第一轮已执行任务列表（非deferred且有plannedProduction>0），用于结束后检查剩余产能并入队R2
        List<CoreScheduleAlgorithmService.DailyEmbryoTask> firstRoundCompletedTasks = new ArrayList<>();

        // 跟踪每个结构的推荐机台产能管控（遍历中累计）
        Map<String, List<MpCxCapacityConfiguration>> structureRecommendedMachinesCache = new HashMap<>();
        Map<String, Integer> structureTotalMaxLhCache = new HashMap<>();
        Map<String, BigDecimal> structureAvgRatioCache = new HashMap<>();
        Map<String, Integer> structureTaskCountMap = new HashMap<>();
        Map<String, Set<String>> structureCountedMachineCodesMap = new HashMap<>();
        Map<String, BigDecimal> structureCumulativeTimeMap = new HashMap<>();
        int skippedCapacityExceeded = 0;

        // 成型胎胚立库库容管控参数
        int warehouseCapacity = getEmbryoWarehouseCapacity(context);
        double warehouseCapacityRatio = getEmbryoWarehouseCapacityRatio(context);
        int warehouseThreshold = (int) Math.floor(warehouseCapacity * warehouseCapacityRatio);

        // 预构建：胎胚维度立库总库存（用于立库库容动态管控）
        Map<String, Integer> embryoTotalStockMap = new HashMap<>();
        if (context.getStocks() != null) {
            for (CxStock stock : context.getStocks()) {
                if (stock.getEmbryoCode() != null && stock.getStockNum() != null && stock.getStockNum() > 0) {
                    embryoTotalStockMap.merge(stock.getEmbryoCode(), stock.getStockNum(), Integer::sum);
                }
            }
        }

        // 预构建：胎胚维度总模数（用于6小时可供硫化时长封顶计算）
        Map<String, Integer> embryoTotalMoldMap = new HashMap<>();
        if (lhScheduleResults != null) {
            for (LhScheduleResult lh : lhScheduleResults) {
                if (lh.getEmbryoCode() != null && lh.getMouldQty() != null && lh.getMouldQty() > 0) {
                    embryoTotalMoldMap.merge(lh.getEmbryoCode(), lh.getMouldQty(), Integer::sum);
                }
            }
        }

        int skippedWarehouseFull = 0;

        Map<String, Integer> shiftFormingOutputMap = new HashMap<>();
        Map<String, Integer> shiftVulcanizingConsumptionMap = new HashMap<>();
        // 任务级硫化消耗（key=lhId），供R2/R3的6h检查使用，避免胎胚聚合总量在多任务共用时误封顶
        Map<Long, Integer> taskVulcConsumptionMap = new HashMap<>();
        int runningTotalProjectedStock = embryoTotalStockMap.values().stream().mapToInt(Integer::intValue).sum();

        if (warehouseCapacity > 0) {
            int cxStockRecordCount = context.getStocks() != null ? (int) context.getStocks().stream()
                    .filter(s -> s.getEmbryoCode() != null && s.getStockNum() != null && s.getStockNum() > 0)
                    .count() : 0;
            log.info("【立库库容管控】参数: 立库总库容={}条, 预警比例={}%, 预警线={}条, 单胎胚可供硫化>{}h即封顶, 立库中有库存的胎胚种类={}种(来自{}条CxStock记录), 当前立库总库存={}条, 剩余可用={}条",
                    warehouseCapacity, (int)(warehouseCapacityRatio * 100), warehouseThreshold,
                    stockHoursCap, embryoTotalStockMap.size(), cxStockRecordCount,
                    runningTotalProjectedStock, Math.max(0, warehouseThreshold - runningTotalProjectedStock));
        }

        // 预计算：哪些结构的全部胎胚都是关键产品（开产班次时不进行过滤，否则整个结构无任务可排）
        Set<String> allKeyProductStructures = new HashSet<>();
        if (isOpeningShift && context.getKeyProductCodes() != null && !context.getKeyProductCodes().isEmpty()) {
            Map<String, Set<String>> structureEmbryoMap = new HashMap<>();
            for (LhScheduleResult lh : lhScheduleResults) {
                if (lh.getEmbryoCode() != null && lh.getStructureName() != null) {
                    structureEmbryoMap.computeIfAbsent(lh.getStructureName(), k -> new HashSet<>()).add(lh.getEmbryoCode());
                }
            }
            for (Map.Entry<String, Set<String>> entry : structureEmbryoMap.entrySet()) {
                boolean allKey = true;
                for (String ec : entry.getValue()) {
                    if (!context.getKeyProductCodes().contains(ec)) {
                        allKey = false;
                        break;
                    }
                }
                if (allKey) {
                    allKeyProductStructures.add(entry.getKey());
                    log.info("开产班次: 结构 {} 全部为关键产品，跳过关键产品过滤", entry.getKey());
                }
            }
        }

        // 构建机台→当日所属结构集合（反查：某机台当日被哪些结构配置占用）
        Map<String, Set<String>> machineToStructuresMap = new HashMap<>();
        if (context.getStructureAllocationMap() != null) {
            int structDayOfMonth = scheduleDate.getDayOfMonth();
            for (Map.Entry<String, List<MpCxCapacityConfiguration>> structEntry : context.getStructureAllocationMap().entrySet()) {
                String structName = structEntry.getKey();
                if (structEntry.getValue() == null) continue;
                for (MpCxCapacityConfiguration config : structEntry.getValue()) {
                    if (config.getBeginDay() != null && config.getEndDay() != null
                            && config.getBeginDay() <= structDayOfMonth && config.getEndDay() >= structDayOfMonth
                            && config.getCxMachineCode() != null) {
                        machineToStructuresMap.computeIfAbsent(config.getCxMachineCode(), k -> new HashSet<>()).add(structName);
                    }
                }
            }
        }

        // 反转 machineOnlineEmbryoMap：机台→当前在产胎胚（用于切换耗时计算时获取英寸）
        Map<String, String> machineCurrentEmbryoMap = new HashMap<>();
        if (machineOnlineEmbryoMap != null) {
            for (Map.Entry<String, Set<String>> embryoEntry : machineOnlineEmbryoMap.entrySet()) {
                String embryoCode = embryoEntry.getKey();
                if (embryoEntry.getValue() != null) {
                    for (String cxCode : embryoEntry.getValue()) {
                        machineCurrentEmbryoMap.put(cxCode, embryoCode);
                    }
                }
            }
        }

        // 机台级占用时间追踪（key = structureName|machineCode → 已占用秒数）
        Map<String, Long> machineOccupiedTimeMap = new HashMap<>();
        // 结构全部收尾标记（结构 → 是否全部收尾）
        Map<String, Boolean> structureFullyEndedMap = new HashMap<>();
        // 提前生产动态已用机台集合
        Set<String> advanceUsedMachineCodes = new HashSet<>();
        // 提前生产结构实际可用产能（结构 → 可用秒数，= 机台数×28800 - 前结构占用 - 切换耗时 - 跨班次遗留）
        Map<String, BigDecimal> structureAdvanceAvailableCapacityMap = new HashMap<>();
        // 跨班次切换剩余耗时（从 context 加载，切换完成后清除）
        Map<String, Long> machineSwitchRemainingMap = context.getMachineSwitchRemainingMap();
        if (machineSwitchRemainingMap == null) {
            machineSwitchRemainingMap = new HashMap<>();
        }

        // 按三层优先级对 lhScheduleResults 排序（降序，高优先级先处理）
        // 多级排序：L1基础分层 → L2类型加成 → L3库存（库存少优先）
        final int sortUrgentDays = getUrgentEndingDays(context);
        final int sortNearDays = getEndingDaysThreshold(context);
        final LocalDate sortScheduleDate = scheduleDate;
        final Map<String, Set<String>> sortMachineOnlineEmbryoMap = machineOnlineEmbryoMap;
        final Map<LhScheduleResult, String> priorityDescMap = new IdentityHashMap<>();
        final List<CxShiftConfig> sortDayShifts = dayShifts;

        Map<LhScheduleResult, Integer> tier1Map = new IdentityHashMap<>();
        Map<LhScheduleResult, Integer> tier2Map = new IdentityHashMap<>();
        Map<LhScheduleResult, Integer> sortStockMap = new IdentityHashMap<>();

        for (LhScheduleResult lh : lhScheduleResults) {
            StringBuilder desc = new StringBuilder();

            boolean hasPlanQty = getShiftPlanQty(lh, sortDayShifts) > 0;

            LocalDate endingDate = findEndingDate(lh.getMaterialCode(), context);
            int daysToEnding = (endingDate != null)
                    ? (int) java.time.temporal.ChronoUnit.DAYS.between(sortScheduleDate, endingDate)
                    : Integer.MAX_VALUE;
            boolean isUrgentEnding = daysToEnding >= 0 && daysToEnding <= sortUrgentDays;
            boolean isNearEnding = daysToEnding >= 0 && daysToEnding <= sortNearDays;

            if (hasPlanQty && isUrgentEnding) {
                tier1Map.put(lh, PRIORITY_HAS_PLAN_URGENT);
                desc.append("有计划+紧急收尾");
            } else if (hasPlanQty && isNearEnding) {
                tier1Map.put(lh, PRIORITY_HAS_PLAN_NEAR);
                desc.append("有计划+近期收尾");
            } else if (hasPlanQty) {
                tier1Map.put(lh, PRIORITY_HAS_PLAN_NORMAL);
                desc.append("有计划+正常");
            } else if (isUrgentEnding) {
                tier1Map.put(lh, PRIORITY_NO_PLAN_URGENT);
                desc.append("无计划+紧急");
            } else if (isNearEnding) {
                tier1Map.put(lh, PRIORITY_NO_PLAN_NEAR);
                desc.append("无计划+近期");
            } else {
                tier1Map.put(lh, PRIORITY_NO_PLAN_NORMAL);
                desc.append("无计划+正常");
            }

            int tier2 = 0;
            String stage = lh.getConstructionStage();
            if (STAGE_TRIAL.equals(stage) || STAGE_PRODUCTION_TRIAL.equals(stage)) {
                tier2 = PRIORITY_TRIAL;
                desc.append("+试制量试");
            }

            List<String> contMachines = findContinueMachines(
                    lh.getMaterialCode(), lh.getEmbryoCode(), sortMachineOnlineEmbryoMap);
            if (!contMachines.isEmpty()) {
                tier2 += PRIORITY_CONTINUE;
                desc.append("+续作");
            }
            tier2Map.put(lh, tier2);

            int stockQty = getCurrentStock(context, lh.getId());
            int stockDeduct = Math.min(stockQty, PRIORITY_STOCK_TIEBREAKER_MAX);
            sortStockMap.put(lh, stockQty);
            desc.append("+库存扣").append(stockDeduct);

            int score = tier1Map.get(lh) + tier2 - stockDeduct;
            priorityDescMap.put(lh, score + "(" + desc + ")");
        }

        // 预计算排序键，避免在比较器中做 IdentityHashMap 查找（可能导致 JIT 优化异常）
        List<Object[]> sortPairs = new ArrayList<>(lhScheduleResults.size());
        for (LhScheduleResult lh : lhScheduleResults) {
            int t1 = tier1Map.get(lh);
            int t2 = tier2Map.get(lh);
            int stock = sortStockMap.get(lh);
            sortPairs.add(new Object[]{lh, t1, t2, stock});
        }
        sortPairs.sort((a, b) -> {
            int cmp = (int) b[1] - (int) a[1]; // L1 降序
            if (cmp != 0) return cmp;
            cmp = (int) b[2] - (int) a[2]; // L2 降序
            if (cmp != 0) return cmp;
            return (int) a[3] - (int) b[3]; // L3 库存升序
        });
        lhScheduleResults.clear();
        for (Object[] pair : sortPairs) {
            lhScheduleResults.add((LhScheduleResult) pair[0]);
        }

        log.info("按三层多级排序完成：L1(有计划+紧急9000>有计划+近期8000>有计划+正常7000>无计划+紧急6000>无计划+近期5000>无计划+正常4000) → L2(试制量试1500>续作800>非续作0) → L3(库存少优先)");

        // 提前生产结构重排：当日有机台的结构在前，无机台的（提前生产）在后
        // 保证提前生产结构处理时，前面正常结构已完整走完R1+R2+R3，可基于实际排产结果判定机台可用性
        List<LhScheduleResult> normalResults = new ArrayList<>();
        List<LhScheduleResult> advanceResults = new ArrayList<>();
        Map<String, Boolean> structHasDayMachineCache = new HashMap<>();
        for (LhScheduleResult lh : lhScheduleResults) {
            String struct = lh.getStructureName();
            boolean hasDayMachine = struct != null && structHasDayMachineCache
                    .computeIfAbsent(struct, k -> !getRecommendedMachinesForStructure(k, scheduleDate, context).isEmpty());
            if (hasDayMachine) {
                normalResults.add(lh);
            } else {
                advanceResults.add(lh);
            }
        }
        if (!advanceResults.isEmpty()) {
            lhScheduleResults.clear();
            lhScheduleResults.addAll(normalResults);
            lhScheduleResults.addAll(advanceResults);
            log.info("【提前生产重排】正常结构任务={}条, 提前生产结构任务={}条, 提前生产结构置后处理",
                    normalResults.size(), advanceResults.size());
        }

        // 2a: 班次计划量筛选 - 仅处理本班次有计划量的记录
        // 本班次无计划量但下班次有计划量的记录，全部纳入本班次排程
        List<LhScheduleResult> activeResults = new ArrayList<>();
        for (LhScheduleResult lh : lhScheduleResults) {
            boolean hasCurrentPlanQty = getShiftPlanQty(lh, dayShifts) > 0;
            if (hasCurrentPlanQty) {
                activeResults.add(lh);
                continue;
            }
            // 本班次无计划量，检查下班次
            int currentClassIdx = getCurrentClassIndex(dayShifts);
            Integer nextShiftPlanQty = getClassPlanQtyByIndex(lh, currentClassIdx + 1);
            if (nextShiftPlanQty != null && nextShiftPlanQty > 0) {
                log.info("[班次筛选-下班次有计划] 胎胚={}, 物料={}, 参与本班次排程",
                        lh.getEmbryoCode(), lh.getMaterialCode());
                activeResults.add(lh);
            }
            // 下班次也无计划量 → 跳过
        }
        if (activeResults.size() < lhScheduleResults.size()) {
            log.info("[班次筛选] 本班次处理 {} / {} 条记录 (跳过 {} 条)",
                    activeResults.size(), lhScheduleResults.size(),
                    lhScheduleResults.size() - activeResults.size());
        }

        // 按结构分组 activeResults（保持排序顺序：正常结构在前，提前生产结构在后）
        Map<String, List<LhScheduleResult>> structureGroupedActive = new LinkedHashMap<>();
        for (LhScheduleResult lh : activeResults) {
            String struct = lh.getStructureName() != null ? lh.getStructureName() : "__NO_STRUCT__";
            structureGroupedActive.computeIfAbsent(struct, k -> new ArrayList<>()).add(lh);
        }
        log.info("【按结构分组】共 {} 个结构", structureGroupedActive.size());

        // 全局防重复集合（跨结构共享）
        Set<CoreScheduleAlgorithmService.DailyEmbryoTask> r2AddedToResultGlobal = new HashSet<>();
        Set<CoreScheduleAlgorithmService.DailyEmbryoTask> r3AddedToResultGlobal = new HashSet<>();

        // 按结构逐个执行 R1→入队→R2→R3
        for (Map.Entry<String, List<LhScheduleResult>> structEntry : structureGroupedActive.entrySet()) {
            String currentStructure = structEntry.getKey();
            List<LhScheduleResult> structActiveResults = structEntry.getValue();

            // 提前生产结构：在R1之前判定机台可用性
            boolean isAdvanceStructure = !structHasDayMachineCache.getOrDefault(currentStructure, true);
            if (isAdvanceStructure) {
                List<MpCxCapacityConfiguration> advanceMachines = resolveAdvanceMachinesByActualStatus(
                        currentStructure, context, machineToStructuresMap, machineCurrentEmbryoMap,
                        machineOccupiedTimeMap, structureFullyEndedMap, advanceUsedMachineCodes,
                        materialMap, structActiveResults, machineSwitchRemainingMap,
                        structureRecommendedMachinesCache, structureCumulativeTimeMap,
                        materialTasksMap, scheduleDate, structureAdvanceAvailableCapacityMap);
                if (advanceMachines.isEmpty()) {
                    log.info("【提前生产】结构={} 无可用未来机台，跳过该结构所有任务", currentStructure);
                    continue;
                }
                // 预存缓存，R1中computeIfAbsent直接命中
                structureRecommendedMachinesCache.put(currentStructure, advanceMachines);
                if (context.getAdvanceProductionMachineMap() == null) {
                    context.setAdvanceProductionMachineMap(new HashMap<>());
                }
                context.getAdvanceProductionMachineMap().put(currentStructure, advanceMachines);
            }

            // 清空每结构的R2/R3状态
            deferredTasks.clear();
            firstRoundCompletedTasks.clear();
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> r2ExitedTasks = new ArrayList<>();

            for (LhScheduleResult lhResult : structActiveResults) {
                if (lhResult.getEmbryoCode() == null) {
                    log.info("[检查0-空胎胚] 跳过：胎胚编码为空");
                    skippedNullEmbryo++;
                    continue;
                }

                String priorityDesc = priorityDescMap.getOrDefault(lhResult, "");
                log.info(">> 处理: 胎胚={}, 物料={}, 结构={}, 优先级={}", lhResult.getEmbryoCode(), lhResult.getMaterialCode(), lhResult.getStructureName(), priorityDesc);

                // 检查1：硫化余量 <= 0，说明该物料已超产，不再需要生产
                String materialCode = lhResult.getMaterialCode();
                if (context.getMonthSurplusMap() != null && materialCode != null) {
                    MdmMonthSurplus monthSurplus = context.getMonthSurplusMap().get(materialCode);
                    if (monthSurplus != null && monthSurplus.getPlanSurplusQty() != null) {
                        int vulcanizeSurplus = monthSurplus.getPlanSurplusQty().intValue();
                        if (vulcanizeSurplus <= 0) {
                            log.info("[检查1-硫化余量] 跳过：物料={}, 硫化余量={} <= 0", materialCode, vulcanizeSurplus);
                            skippedVulcanizeSurplusZero++;
                            continue;
                        }
                    }
                }

                // 检查2：成型余量 <= 0，说明胎胚库存已满足硫化需求，不再需要成型生产
                Integer formingRemainder = getFormingRemainder(materialCode, context);
                if (formingRemainder != null && formingRemainder <= 0) {
                    log.info("[检查2-成型余量] 跳过：物料={}, 成型余量={} <= 0", materialCode, formingRemainder);
                    skippedFormingRemainderZero++;
                    continue;
                }

                // 开产班次提前过滤关键产品（不在分组中保留，直接在循环中跳过）
                // 但如果该结构全部为关键产品，则不进行过滤（避免整个结构无任务可排）
                if (isOpeningShift && context.getKeyProductCodes() != null
                        && lhResult.getEmbryoCode() != null
                        && context.getKeyProductCodes().contains(lhResult.getEmbryoCode())
                        && !allKeyProductStructures.contains(lhResult.getStructureName())) {
                    log.info("[检查3-关键产品] 跳过：开产班次, 胎胚={}, 结构={}非全关键产品", lhResult.getEmbryoCode(), lhResult.getStructureName());
                    continue;
                }


                CoreScheduleAlgorithmService.DailyEmbryoTask task = buildSingleTask(
                        lhResult, materialMap, stockMap, context, dayShifts);
                if (task == null) {
                    log.info("[buildSingleTask返回null] 跳过：胎胚={}", lhResult.getEmbryoCode());
                    skippedNullTask++;
                    continue;
                }

                String embryoCode = lhResult.getEmbryoCode();

                // 判断任务类型
                List<String> continueMachineCodes = findContinueMachines(materialCode, embryoCode, machineOnlineEmbryoMap);
                boolean isContinueTask = !continueMachineCodes.isEmpty();

                String constructionStage = lhResult.getConstructionStage();
                boolean isTrialTask = STAGE_TRIAL.equals(constructionStage);
                boolean isProductionTrial = STAGE_PRODUCTION_TRIAL.equals(constructionStage);

                // 设置任务属性
                task.setIsContinueTask(isContinueTask);
                task.setIsTrialTask(isTrialTask);
                task.setIsProductionTrial(isProductionTrial);
                task.setContinueMachineCodes(continueMachineCodes);
                task.setIsFirstTask(!isContinueTask && !isTrialTask && !isProductionTrial);
                task.setConstructionStage(constructionStage);

                // 将任务添加到物料任务列表（用于回溯更新）
                materialTasksMap.computeIfAbsent(materialCode, k -> new ArrayList<>()).add(task);

                // S5.2.4 计算收尾属性（传入已使用的成型余量）
                int usedRemainder = materialUsedFormingRemainder.getOrDefault(materialCode, 0);
                calculateEndingInfo(task, context, scheduleDate, usedRemainder);

                // S5.2.4.2 暂存待第二轮分配：
                //   零净需求（库存已覆盖需求）→ 按收尾余量一车一车补
                int taskVulcanizeDemand = task.getVulcanizeDemand() != null ? task.getVulcanizeDemand() : 0;
                int taskCurrentStock = task.getCurrentStock() != null ? task.getCurrentStock() : 0;
                int taskNetDemand = Math.max(0, taskVulcanizeDemand - taskCurrentStock);
                boolean isTrialLikeTask = isTrialTask || isProductionTrial;
                boolean shouldDefer = (taskNetDemand == 0 && !isTrialLikeTask);
                if (shouldDefer) {
                    int deferredQty = task.getEndingSurplusQty() != null ? task.getEndingSurplusQty() : 0;
                    task.setDeferredRemainingDemand(deferredQty);
                    task.setPlannedProduction(0);
                    task.setRequiredCars(0);
                    task.setEndingExtraInventory(0);
                    log.info("暂存待第二轮分配: 胎胚={}, 物料={}, 原因=零净需求, 剩余需求={}, 收尾余量={}",
                            embryoCode, materialCode,
                            deferredQty, task.getEndingSurplusQty());
                    // 记录硫化消耗，供R2预处理/R2退出/R3动态stockHours使用
                    int deferredVulcConsumption = getShiftPlanQty(lhResult, dayShifts);
                    taskVulcConsumptionMap.put(lhResult.getId(), deferredVulcConsumption);

                    // 预计算停产封顶约束：零净需求任务跳过了handleOpeningClosingDay，
                    // 需在此预存closingRequiredStock供R2/R3拉量时检查上限
                    // 需覆盖两种场景：①当天停产约束 ②跨天停产封顶（明天有停产）
                    if (dayShifts != null && !dayShifts.isEmpty()) {
                        int deferredDayShiftOrder = dayShifts.get(0).getDayShiftOrder() != null
                                ? dayShifts.get(0).getDayShiftOrder() : 0;
                        LocalDate closingScheduleDate = context.getCurrentScheduleDate();
                        String closingFactoryCode = context.getFactoryCode();
                        boolean deferredNeedsClosingStock = false;
                        String deferredTrigger = "";
                        // 场景①：当天有停产约束（停产班/停产前/停产标识日）
                        boolean deferredIsStopDay = scheduleDayTypeHelper.isStopDay(closingScheduleDate, closingFactoryCode);
                        if (deferredIsStopDay) {
                            deferredNeedsClosingStock = true;
                            deferredTrigger = "当天停产日";
                        } else {
                            ScheduleDayTypeHelper.ShiftType deferredShiftType = scheduleDayTypeHelper.determineShiftType(
                                    closingScheduleDate, deferredDayShiftOrder, closingFactoryCode);
                            boolean deferredIsClosing = deferredShiftType == ScheduleDayTypeHelper.ShiftType.CLOSED;
                            boolean deferredIsBeforeClosing = deferredShiftType == ScheduleDayTypeHelper.ShiftType.BEFORE_CLOSE;
                            boolean deferredIsStopFlagDay = scheduleDayTypeHelper.isStopFlagDay(closingScheduleDate, closingFactoryCode);
                            if (deferredIsClosing || deferredIsBeforeClosing || deferredIsStopFlagDay) {
                                deferredNeedsClosingStock = true;
                                deferredTrigger = "当天停产约束";
                            }
                        }
                        // 场景②：跨天停产封顶（明天有停产 → 今天产量需反推封顶，与handleOpeningClosingDay一致）
                        if (!deferredNeedsClosingStock) {
                            LocalDate deferredNextDay = closingScheduleDate.plusDays(1);
                            if (scheduleDayTypeHelper.hasAnyClosingShift(deferredNextDay, closingFactoryCode)) {
                                deferredNeedsClosingStock = true;
                                deferredTrigger = "跨天(" + deferredNextDay + "有停产)";
                            }
                        }
                        if (deferredNeedsClosingStock) {
                            int deferredClosingStock = calculateClosingRequiredStockV2(
                                    task, context, closingScheduleDate, deferredDayShiftOrder, dayShifts);
                            task.setClosingRequiredStock(deferredClosingStock);
                            log.info("  [停产封顶预存] 胎胚={}, 物料={}, closingRequiredStock={}, 触发={}",
                                    embryoCode, materialCode, deferredClosingStock, deferredTrigger);
                        }
                    }

                    deferredTasks.add(task);
                    continue;
                }

                // S5.2.5 计算待排产量
                calculatePlannedProduction(task, context, scheduleDate);

                // 检查4：月计划推荐的机台总产能管控（后置到计算待排产量之后，使用实际计划量）
                String structureName = lhResult.getStructureName();
                if (structureName != null && context.getStructureAllocationMap() != null) {
                    // 获取推荐机台：优先从缓存获取（提前生产已预解析），否则从当日配置查找
                    List<MpCxCapacityConfiguration> recommendedMachines = structureRecommendedMachinesCache
                            .computeIfAbsent(structureName, k -> getRecommendedMachinesForStructure(k, scheduleDate, context));

                    if (recommendedMachines == null || recommendedMachines.isEmpty()) {
                        log.info("【机台产能管控】结构={}, 胎胚={}, 跳过：无推荐机台配置(当日及未来均未匹配到排产配置)", structureName, lhResult.getEmbryoCode());
                    } else {
                        int totalMaxLh = structureTotalMaxLhCache
                                .computeIfAbsent(structureName, k -> calculateStructureTotalMaxLh(recommendedMachines, k, context));
                        BigDecimal avgRatio = structureAvgRatioCache
                                .computeIfAbsent(structureName, k -> calculateStructureAvgRatio(recommendedMachines, k, context));

                        // 按lhMachineCode去重计数硫化机台数（同一台硫化机L+R模只算1台）
                        // 使用 task.getLhMachineCode()（已通过 extractLhMachineKey 去掉 L/R 后缀）
                        String lhMachineCode = task.getLhMachineCode();
                        String machineKey = (lhMachineCode != null && !lhMachineCode.isEmpty()) ? lhMachineCode : "lhId_" + lhResult.getId();
                        Set<String> countedCodes = structureCountedMachineCodesMap
                                .computeIfAbsent(structureName, k -> new HashSet<>());
                        int currentCount = countedCodes.size();
                        boolean isNewLhMachine = !countedCodes.contains(machineKey);
                        int plannedProduction = task.getPlannedProduction() != null ? task.getPlannedProduction() : 0;

                        if (plannedProduction == 0) {
                            log.info("【机台产能管控】结构={}, 胎胚={}, 计划量=0条, 跳过累计(零需求任务，不占配额)",
                                    structureName, lhResult.getEmbryoCode());
                        } else {
                            if (currentCount >= totalMaxLh) {
                                log.info("[检查4-硫化机数] 跳过：结构={}, 当前{}/上限{}, lhMachineCode={}, machineKey={}, 已计数={}",
                                        structureName, currentCount, totalMaxLh, lhMachineCode, machineKey, countedCodes);
                                skippedCapacityExceeded++;
                                continue;
                            }

                            BigDecimal totalCapacitySeconds = structureAdvanceAvailableCapacityMap.containsKey(structureName)
                                    ? structureAdvanceAvailableCapacityMap.get(structureName)
                                    : BigDecimal.valueOf(recommendedMachines.size())
                                    .multiply(BigDecimal.valueOf(SECONDS_PER_SHIFT));
                            Integer dailyLhCapacity = productionCalculator.getDoubleMoldDailyLhCapacity(materialCode, context);

                            BigDecimal itemTimeSeconds = BigDecimal.ZERO;
                            String timePerTireStr = "-";
                            if (dailyLhCapacity != null && dailyLhCapacity > 0
                                    && avgRatio.compareTo(BigDecimal.ZERO) > 0) {
                                BigDecimal timePerTire = productionCalculator.calculateTimePerTire(avgRatio, dailyLhCapacity);
                                timePerTireStr = timePerTire.stripTrailingZeros().toPlainString();
                                itemTimeSeconds = timePerTire.multiply(BigDecimal.valueOf(plannedProduction));
                                BigDecimal cumulativeTime = structureCumulativeTimeMap.getOrDefault(structureName, BigDecimal.ZERO);

                                if (cumulativeTime.add(itemTimeSeconds).compareTo(totalCapacitySeconds) > 0) {
                                    log.info("[检查4-累计耗时] 跳过：结构={}, 累计={}s > 总产能={}s",
                                            structureName, cumulativeTime.add(itemTimeSeconds).toBigInteger(),
                                            totalCapacitySeconds.toBigInteger());
                                    skippedCapacityExceeded++;
                                    continue;
                                }
                                structureCumulativeTimeMap.put(structureName, cumulativeTime.add(itemTimeSeconds));
                            }
                            if (isNewLhMachine) {
                                countedCodes.add(machineKey);
                            }

                            int newCount = countedCodes.size();
                            BigDecimal newCumulative = structureCumulativeTimeMap.getOrDefault(structureName, BigDecimal.ZERO);
                            BigDecimal remaining = totalCapacitySeconds.subtract(newCumulative);
                            log.info("【机台产能管控】结构={}, 胎胚={}, 已排硫化机={}/{}, 配比={}, 单胎耗时={}s, 计划量={}条, 本项={}s({}h), 累计消耗={}s({}h), 总产能={}s({}h), 剩余={}s({}h), lhMachineCode={}, 左右模={}, 新硫化机={}",
                                    structureName, lhResult.getEmbryoCode(), newCount, totalMaxLh,
                                    avgRatio.stripTrailingZeros().toPlainString(), timePerTireStr,
                                    plannedProduction,
                                    itemTimeSeconds.stripTrailingZeros().toPlainString(),
                                    itemTimeSeconds.divide(BigDecimal.valueOf(SECONDS_PER_HOUR), 1, BigDecimal.ROUND_HALF_UP),
                                    newCumulative.stripTrailingZeros().toPlainString(),
                                    newCumulative.divide(BigDecimal.valueOf(SECONDS_PER_HOUR), 1, BigDecimal.ROUND_HALF_UP),
                                    totalCapacitySeconds.stripTrailingZeros().toPlainString(),
                                    totalCapacitySeconds.divide(BigDecimal.valueOf(SECONDS_PER_HOUR), 1, BigDecimal.ROUND_HALF_UP),
                                    remaining.stripTrailingZeros().toPlainString(),
                                    remaining.divide(BigDecimal.valueOf(SECONDS_PER_HOUR), 1, BigDecimal.ROUND_HALF_UP),
                                    lhMachineCode, lhResult.getLeftRightMould(), isNewLhMachine);
                        }
                    }
                }

                // S5.2.6 收尾余量处理
                handleEndingRemainder(task, context);

                // S5.2.6.1 收尾余量处理后再次检查：如果 handleEndingRemainder 标记了 isLastEndingBatch，需要回溯更新
                if (Boolean.TRUE.equals(task.getIsLastEndingBatch())) {
                    List<CoreScheduleAlgorithmService.DailyEmbryoTask> allTasksForMaterial = materialTasksMap.get(materialCode);
                    if (allTasksForMaterial != null) {
                        for (CoreScheduleAlgorithmService.DailyEmbryoTask prevTask : allTasksForMaterial) {
                            if (prevTask != task && !Boolean.TRUE.equals(prevTask.getIsLastEndingBatch())) {
                                prevTask.setIsLastEndingBatch(true);
                                log.info("回溯更新 isLastEndingBatch（收尾余量处理）: 物料={}, 胎胚={} → true", materialCode, prevTask.getEmbryoCode());
                            }
                        }
                    }
                }

                // S5.2.6.2 立库库容动态管控：两个维度（空间 + 时间）限制产量
                //   【重要】放在打印收尾日志和累加成型余量之前，避免日志和累加使用封顶前的旧值
                //   维度一（空间）：所有胎胚的预计班后库存总和 >= 库容上限 → 本胎胚封顶
                //   维度二（时间）：本胎胚预计班后库存可供硫化时长 > 6h → 本胎胚封顶
                if (embryoCode != null) {
                    Integer pp = task.getPlannedProduction();
                    int originalProduction = pp != null ? pp : 0;
                    int vulcanizingConsumption = getShiftPlanQty(lhResult, dayShifts);
                    taskVulcConsumptionMap.put(lhResult.getId(), vulcanizingConsumption);

                    // 计算加入本任务前：全部胎胚的预计班后总库存
                    int totalProjectedStockBefore = runningTotalProjectedStock;

                    // 加入本任务
                    if (originalProduction > 0) {
                        shiftFormingOutputMap.merge(embryoCode, originalProduction, Integer::sum);
                    }
                    if (vulcanizingConsumption > 0) {
                        shiftVulcanizingConsumptionMap.merge(embryoCode, vulcanizingConsumption, Integer::sum);
                    }

                    int currentStock = embryoTotalStockMap.getOrDefault(embryoCode, 0);
                    int cumFormingOutput = shiftFormingOutputMap.getOrDefault(embryoCode, 0);
                    int cumVulcanizingConsumption = shiftVulcanizingConsumptionMap.getOrDefault(embryoCode, 0);
                    int projectedStock = currentStock + cumFormingOutput - cumVulcanizingConsumption;

                    // 加入后全部胎胚预计总库存 = 加入前 + 本任务净增量
                    int totalProjectedStock = totalProjectedStockBefore + originalProduction - vulcanizingConsumption;

                    // 统一日志：每个任务都打印立库状态
                    //   入立库 = 成型产出 - 硫化消耗（净增量），当硫化消化全部成型产出时入立库=0
                    //   说明：净入立库 = 本任务成型产出(11) - 本任务硫化消耗(16) = -5，是单任务净增量；
                    //        与下方预计班后库存公式(currentStock+cumForming-cumConsumption)中的累计口径不同
                    if (warehouseCapacity > 0) {
                        int netStockChange = originalProduction - vulcanizingConsumption;
                        int warehouseRemainBefore = Math.max(0, warehouseThreshold - totalProjectedStockBefore);
                        int warehouseRemainAfter = Math.max(0, warehouseThreshold - totalProjectedStock);
                        log.info("【立库库容管控】胎胚={}, 胎胚总库存={}条, 本任务成型产出={}条, 本任务硫化消耗={}条, 净入立库={}条(本任务), 累计成型产出={}条, 累计硫化消耗={}条, 立库预警线={}条(总容量={}×{}%), 加入前立库剩余={}条, 加入后立库剩余={}条",
                                embryoCode, currentStock, originalProduction, vulcanizingConsumption, netStockChange,
                                cumFormingOutput, cumVulcanizingConsumption,
                                warehouseThreshold, warehouseCapacity, (int)(warehouseCapacityRatio * 100),
                                warehouseRemainBefore, warehouseRemainAfter);
                    }

                    int tripCapacity = getTripCapacity(task.getStructureName(), task.getEmbryoCode(), context);

                    int maxAllowedProduction = originalProduction;
                    boolean capped = false;
                    StringBuilder capDetail = new StringBuilder();

                    // 维度一（空间）：所有胎胚的预计班后库存总和 >= 库容上限
                    if (warehouseCapacity > 0 && totalProjectedStock >= warehouseThreshold) {
                        int totalOverAmount = totalProjectedStock - warehouseThreshold;
                        int spaceLimit = Math.max(0, originalProduction - totalOverAmount);
                        maxAllowedProduction = Math.min(maxAllowedProduction, spaceLimit);
                        capped = true;
                        capDetail.append("[空间] 全部胎胚预计库存=").append(totalProjectedStock)
                                .append(" >= 库容上限=").append(warehouseThreshold)
                                .append(", 超出=").append(totalOverAmount)
                                .append(", 空间允许产量=max(0,").append(originalProduction).append("-").append(totalOverAmount)
                                .append(")=").append(spaceLimit);
                    }

                    // 维度二（时间6h封顶）已在R1移除：R1的职责是满足硫化需求，
                    // 6h封顶会导致硫化任务无法满足（封顶后不够一车→归零→硫化需求落空）。
                    // 立库空间维度（维度一）仍保留，防止立库溢出。
                    // 6h管控仅在R2中以"事前预估→退出到R3"方式生效，R3无6h限制。

                    if (capped && maxAllowedProduction < originalProduction) {
                        int cappedProduction;
                        if (tripCapacity > 0 && maxAllowedProduction >= tripCapacity) {
                            cappedProduction = (maxAllowedProduction / tripCapacity) * tripCapacity;
                        } else {
                            cappedProduction = 0;
                        }
                        task.setPlannedProduction(cappedProduction);
                        task.setRequiredCars(productionCalculator.calculateRequiredCars(cappedProduction, tripCapacity));
                        task.setEndingExtraInventory(cappedProduction);
                        shiftFormingOutputMap.put(embryoCode,
                                cumFormingOutput - originalProduction + cappedProduction);

                        if (cappedProduction < originalProduction) {
                            String structName = task.getStructureName();
                            if (structName != null) {
                                int timeDiff = originalProduction - cappedProduction;
                                BigDecimal oldCumulative = structureCumulativeTimeMap.getOrDefault(structName, BigDecimal.ZERO);
                                Integer oldDailyLhCap = productionCalculator.getDoubleMoldDailyLhCapacity(materialCode, context);
                                if (oldDailyLhCap != null && oldDailyLhCap > 0) {
                                    BigDecimal avgRatio = structureAvgRatioCache.get(structName);
                                    if (avgRatio == null || avgRatio.compareTo(BigDecimal.ZERO) == 0) {
                                        avgRatio = BigDecimal.ONE;
                                    }
                                    BigDecimal timePerTire = productionCalculator.calculateTimePerTire(avgRatio, oldDailyLhCap);
                                    BigDecimal reduceSeconds = timePerTire.multiply(BigDecimal.valueOf(timeDiff));
                                    BigDecimal newCumulative = oldCumulative.subtract(reduceSeconds);
                                    structureCumulativeTimeMap.put(structName, newCumulative);
                                    log.info("立库封顶回滚机台产能: 结构={}, 胎胚={}, 原产量={}, 封顶后={}, 回滚耗时={}s({}h), 更新后累计={}s",
                                            structName, embryoCode, originalProduction, cappedProduction,
                                            reduceSeconds.stripTrailingZeros().toPlainString(),
                                            reduceSeconds.divide(BigDecimal.valueOf(3600), 2, BigDecimal.ROUND_HALF_UP),
                                            newCumulative.stripTrailingZeros().toPlainString());
                                }
                            }
                        }

                        // R1仅保留维度一（空间）封顶，维度二（时间6h）已移除
                        String capReason = "立库空间不足";
                        log.info("【立库封顶详情】胎胚={}, 原产量={}条, 因{}只能生产{}条, 整车容量={}条, {}→最终计划量={}条, 详情: {}",
                                embryoCode,
                                originalProduction, capReason, maxAllowedProduction, tripCapacity,
                                (tripCapacity > 0 && maxAllowedProduction >= tripCapacity)
                                        ? "向下取整车(" + maxAllowedProduction + "/" + tripCapacity + "=" + (maxAllowedProduction / tripCapacity) + "车)"
                                        : "不够一车→归零",
                                cappedProduction, capDetail.toString());
                    }

                    int actualProduction = task.getPlannedProduction() != null ? task.getPlannedProduction() : 0;
                    runningTotalProjectedStock = totalProjectedStockBefore + actualProduction - vulcanizingConsumption;
                }

                // 打印收尾任务完整信息（封顶后的最终值）
                Integer endingSurplus = task.getEndingSurplusQty();
                if ((endingSurplus != null && endingSurplus < getEndingUrgentFormingRemainder(context))
                        || Boolean.TRUE.equals(task.getIsUrgentEnding())) {
                    int vulcanizeDmd = task.getVulcanizeDemand() != null ? task.getVulcanizeDemand() : 0;
                    int stock = task.getCurrentStock() != null ? task.getCurrentStock() : 0;
                    int netDemand = Math.max(0, vulcanizeDmd - stock);
                    BigDecimal lossRate = context.getLossRate() != null ? context.getLossRate() : BigDecimal.ZERO;
                    boolean isTrialProduction = Boolean.TRUE.equals(task.getIsTrialTask()) || Boolean.TRUE.equals(task.getIsProductionTrial());
                    int tripCap = getTripCapacity(task.getStructureName(), task.getEmbryoCode(), context);
                    log.info("收尾任务[{}]: 剩余余量={}, 收尾日={}, 距收尾={}天, 紧急={}, 近期={}, 最后一批={}",
                            embryoCode, task.getEndingSurplusQty(), task.getEndingDate(),
                            task.getDaysToEnding(), task.getIsUrgentEnding(), task.getIsNearEnding(), task.getIsLastEndingBatch());
                    if (netDemand == 0
                            && endingSurplus != null
                            && endingSurplus > 0
                            && task.getPlannedProduction() != null
                            && task.getPlannedProduction() > 0) {
                        log.info("  排产计算[库存覆盖后收尾补产]: 当前硫化需求已被库存覆盖，收尾余量={}, 补产产量={}",
                                endingSurplus, task.getPlannedProduction());
                    } else if (isTrialProduction) {
                        log.info("  排产计算[试制量试]: (硫化{} - 库存{}) = {}, 不补整车→计划量={}, 车数={}, 车容量={}",
                                vulcanizeDmd, stock, netDemand,
                                task.getPlannedProduction(), task.getRequiredCars(), tripCap);
                    } else {
                        int rawWithLoss = BigDecimal.valueOf(netDemand)
                                .multiply(BigDecimal.ONE.add(lossRate))
                                .setScale(0, BigDecimal.ROUND_UP).intValue();
                        log.info("  排产计算: (硫化{} - 库存{}) = {}, ×(1+损耗{}) = {}, 向上整车({})取整→待排={}, 需车={}, 实际={}",
                                vulcanizeDmd, stock, netDemand, lossRate, rawWithLoss,
                                tripCap, task.getPlannedProduction(), task.getRequiredCars(), task.getEndingExtraInventory());
                    }
                }

                // 共用胎胚：计划量不超过剩余共享成型余量（第三种场景：多任务同物料共用成型余量）
                // handleEndingRemainder已处理收尾任务（isLastEndingBatch），此处仅封顶非收尾任务
                if (!Boolean.TRUE.equals(task.getIsLastEndingBatch())) {
                    Integer r1FormingRemainder = getFormingRemainder(materialCode, context);
                    if (r1FormingRemainder != null) {
                        int r1UsedRemainder = materialUsedFormingRemainder.getOrDefault(materialCode, 0);
                        int r1RemainingForming = r1FormingRemainder - r1UsedRemainder;
                        int r1EndingInventory = task.getEndingExtraInventory() != null ? task.getEndingExtraInventory() : 0;
                        if (r1RemainingForming >= 0 && r1EndingInventory > r1RemainingForming) {
                            log.info("  [R1-成型余量封顶] 物料={}, 计划量={} > 剩余成型余量={}, 封顶至={}",
                                    materialCode, r1EndingInventory, r1RemainingForming, r1RemainingForming);
                            task.setEndingExtraInventory(r1RemainingForming);
                            if (task.getPlannedProduction() != null && task.getPlannedProduction() > r1RemainingForming) {
                                task.setPlannedProduction(r1RemainingForming);
                            }
                        }
                    }
                }

                // 更新已使用的成型余量（累加封顶后的 endingExtraInventory）
                if (task.getEndingExtraInventory() != null && task.getEndingExtraInventory() > 0) {
                    materialUsedFormingRemainder.merge(materialCode, task.getEndingExtraInventory(), Integer::sum);
                    log.info("物料 {} 已使用成型余量累计: {}", materialCode, materialUsedFormingRemainder.get(materialCode));
                }

                // S5.2.7 停产特殊处理
                handleOpeningClosingDay(task, context, dayShifts);
                // S5.2.8 试制任务：产量必须是双数，不补整车
                if (Boolean.TRUE.equals(isTrialTask) || Boolean.TRUE.equals(isProductionTrial)) {
                    Integer pp = task.getPlannedProduction();
                    if (pp != null && pp % 2 != 0) {
                        task.setPlannedProduction(pp - 1);
                        task.setEndingExtraInventory(pp - 1);
                        log.debug("试制量试任务 {} 产量{}为奇数，调整为偶数{}", task.getEmbryoCode(), pp, pp - 1);
                    }
                }

                // S5.2.9 分组
                if (isContinueTask) {
                    result.getContinueTasks().add(task);
                } else if (isTrialTask) {
                    result.getTrialTasks().add(task);
                } else {
                    result.getNewTasks().add(task);
                }

                // 变更2b: 记录第一轮已执行且产量>0的任务（非deferred任务已在此处分组）
                if (task.getPlannedProduction() != null && task.getPlannedProduction() > 0) {
                    firstRoundCompletedTasks.add(task);
                }
            }

            // ==================== 变更2b：第一轮已执行任务入队：检查是否有剩余成型余量 ====================
            if (!firstRoundCompletedTasks.isEmpty()) {
                log.info("【第一轮已执行任务入队】检查 {} 个第一轮任务是否有剩余成型余量", firstRoundCompletedTasks.size());
                int enrolledCount = 0;
                for (CoreScheduleAlgorithmService.DailyEmbryoTask completedTask : firstRoundCompletedTasks) {
                    String ctMaterialCode = completedTask.getMaterialCode();
                    Integer totalFormingRemainder = getFormingRemainder(ctMaterialCode, context);
                    int usedRemainder = materialUsedFormingRemainder.getOrDefault(ctMaterialCode, 0);
                    if (totalFormingRemainder != null) {
                        int remainingSurplus = totalFormingRemainder - usedRemainder;
                        if (remainingSurplus > 0) {
                            completedTask.setDeferredRemainingDemand(remainingSurplus);
                            // plannedProduction 保持第一轮已分配的值
                            deferredTasks.add(completedTask);
                            enrolledCount++;
                            log.info("  第一轮任务入队: 胎胚={}, 物料={}, 总成型余量={}, 已用={}, 剩余产能={}, 第一轮已排产量={}",
                                    completedTask.getEmbryoCode(), ctMaterialCode, totalFormingRemainder,
                                    usedRemainder, remainingSurplus, completedTask.getPlannedProduction());
                        }
                    }
                }
                if (enrolledCount > 0) {
                    log.info("【第一轮已执行任务入队】{} 个任务加入R2暂存队列", enrolledCount);
                }
            }

            // ==================== 第二轮：零净需求暂存任务的剩余产能分配 ====================
            if (!deferredTasks.isEmpty()) {
                int deferredTotal = deferredTasks.size();
                log.info("【第二轮分配】开始处理 {} 个零净需求暂存任务", deferredTotal);
                int deferredAllocated = 0;
                int deferredSkippedCapacity = 0;
                int deferredSkippedForming = 0;
                int deferredSkippedEnding = 0;
                int deferredSkippedWarehouse = 0;
                List<String> skippedFormingList = new ArrayList<>();
                List<String> skippedEndingList = new ArrayList<>();
                List<String> skippedWarehouseList = new ArrayList<>();
                List<String> skippedCapacityList = new ArrayList<>();

                // 跟踪每任务的累计分配：key=胎胚编码, value=[累计产量, 轮次]
                Map<String, int[]> taskRoundTracker = new HashMap<>();

                // 按结构分组
                Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> structureDeferredMap = new LinkedHashMap<>();
                for (CoreScheduleAlgorithmService.DailyEmbryoTask dt : deferredTasks) {
                    String structName = dt.getStructureName() != null ? dt.getStructureName() : "";
                    structureDeferredMap.computeIfAbsent(structName, k -> new ArrayList<>()).add(dt);
                }

                // 计算每个结构的剩余产能
                Map<String, BigDecimal> structureRemainingCapacityMap = new HashMap<>();
                for (String structName : structureDeferredMap.keySet()) {
                    if (structName.isEmpty() || context.getStructureAllocationMap() == null) {
                        structureRemainingCapacityMap.put(structName, BigDecimal.ZERO);
                        log.info("  结构 {} 剩余产能: 无结构配置(产能=0)", structName.isEmpty() ? "(空)" : structName);
                        continue;
                    }
                    List<MpCxCapacityConfiguration> recommendedMachines = structureRecommendedMachinesCache
                            .computeIfAbsent(structName, k -> getRecommendedMachinesForStructure(k, scheduleDate, context));
                    if (recommendedMachines == null || recommendedMachines.isEmpty()) {
                        structureRemainingCapacityMap.put(structName, BigDecimal.ZERO);
                        log.info("  结构 {} 剩余产能: 无推荐机台(产能=0)", structName);
                        continue;
                    }
                    BigDecimal totalCapacitySeconds = structureAdvanceAvailableCapacityMap.containsKey(structName)
                            ? structureAdvanceAvailableCapacityMap.get(structName)
                            : BigDecimal.valueOf(recommendedMachines.size())
                            .multiply(BigDecimal.valueOf(SECONDS_PER_SHIFT));
                    BigDecimal cumulativeTime = structureCumulativeTimeMap.getOrDefault(structName, BigDecimal.ZERO);
                    BigDecimal remaining = totalCapacitySeconds.subtract(cumulativeTime);
                    structureRemainingCapacityMap.put(structName, remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO);
                    log.info("  结构 {} 剩余产能: 总={}s({}h), 第一轮已用={}s({}h), 剩余={}s({}h)",
                            structName, totalCapacitySeconds.toBigInteger(),
                            totalCapacitySeconds.divide(BigDecimal.valueOf(SECONDS_PER_HOUR), 1, BigDecimal.ROUND_HALF_UP),
                            cumulativeTime.toBigInteger(),
                            cumulativeTime.divide(BigDecimal.valueOf(SECONDS_PER_HOUR), 1, BigDecimal.ROUND_HALF_UP),
                            remaining.toBigInteger(),
                            remaining.divide(BigDecimal.valueOf(SECONDS_PER_HOUR), 1, BigDecimal.ROUND_HALF_UP));
                }

                // 按结构独立处理：变更2c - 最小优先策略 + R2预处理 + 6h退出条件
                for (Map.Entry<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> entry : structureDeferredMap.entrySet()) {
                    String structName = entry.getKey();
                    List<CoreScheduleAlgorithmService.DailyEmbryoTask> structTasks = new ArrayList<>(entry.getValue());
                    BigDecimal remainingCapacity = structureRemainingCapacityMap.getOrDefault(structName, BigDecimal.ZERO);

                    log.info("==================== 结构={}, 剩余产能={}s({}h), 任务数={} ====================",
                            structName, remainingCapacity.stripTrailingZeros().toPlainString(),
                            remainingCapacity.divide(BigDecimal.valueOf(SECONDS_PER_HOUR), 1, BigDecimal.ROUND_HALF_UP),
                            structTasks.size());

                    if (remainingCapacity.compareTo(BigDecimal.ZERO) <= 0) {
                        log.info("  结构 {} 剩余产能为0，跳过", structName);
                        continue;
                    }

                    // 计算总产能
                    int totalRecommendedMachines = structureRecommendedMachinesCache.get(structName) != null
                            ? structureRecommendedMachinesCache.get(structName).size() : 0;
                    BigDecimal totalCapacity = structureAdvanceAvailableCapacityMap.containsKey(structName)
                            ? structureAdvanceAvailableCapacityMap.get(structName)
                            : BigDecimal.valueOf(totalRecommendedMachines)
                            .multiply(BigDecimal.valueOf(SECONDS_PER_SHIFT));
                    log.info("  结构={}, 总产能={}s({}h), 推荐机台数={}",
                            structName, totalCapacity.toBigInteger(),
                            totalCapacity.divide(BigDecimal.valueOf(SECONDS_PER_HOUR), 1, BigDecimal.ROUND_HALF_UP),
                            totalRecommendedMachines);

                    // ==================== 变更2c: R2预处理 - 移出当前stockHours已超阈值的任务 ====================
                    Iterator<CoreScheduleAlgorithmService.DailyEmbryoTask> preIter = structTasks.iterator();
                    while (preIter.hasNext()) {
                        CoreScheduleAlgorithmService.DailyEmbryoTask dt = preIter.next();
                        String preMaterialCode = dt.getMaterialCode();

                        // 检查成型余量是否已耗尽
                        Integer preFormingRemainder = getFormingRemainder(preMaterialCode, context);
                        int preUsedRemainder = materialUsedFormingRemainder.getOrDefault(preMaterialCode, 0);
                        if (preFormingRemainder != null && (preFormingRemainder - preUsedRemainder) <= 0) {
                            log.info("  [R2-预处理-成型余量耗尽] 胎胚={}, 物料={}, 已用{}/总量{}, 移入R3退出队列",
                                    dt.getEmbryoCode(), preMaterialCode, preUsedRemainder, preFormingRemainder);
                            r2ExitedTasks.add(dt);
                            preIter.remove();
                            deferredSkippedForming++;
                            skippedFormingList.add(dt.getEmbryoCode() + "/" + preMaterialCode);
                            continue;
                        }

                        // 检查当前预计班后库存可供硫化时长是否已超阈值
                        // 公式与S5.2.6.2维度二一致：分配库存 + 已排产量 - 硫化消耗
                        if (stockHoursCapEnabled && dt.getEmbryoCode() != null) {
                            int preTaskMoldQty = dt.getVulcanizeMoldCount();
                            int preSingleLhCap = productionCalculator.getSingleMoldDailyLhCapacity(preMaterialCode, context);
                            if (preTaskMoldQty > 0 && preSingleLhCap > 0) {
                                int preAllocatedStock = getCurrentStock(context, dt.getLhId());
                                int preProduction = dt.getPlannedProduction() != null ? dt.getPlannedProduction() : 0;
                                int preVulcConsumption = taskVulcConsumptionMap.getOrDefault(dt.getLhId(), 0);
                                int preProjectedStock = preAllocatedStock + preProduction - preVulcConsumption;
                                BigDecimal preStockHours = productionCalculator.calculateStockHours(preProjectedStock, preSingleLhCap, preTaskMoldQty);
                                if (preStockHours.compareTo(BigDecimal.valueOf(stockHoursCap)) > 0) {
                                    log.info("  [R2-预处理-stockHours超限] 胎胚={}, 物料={}, 分配库存={}, 已排产量={}, 硫化消耗={}, 预计班后库存={}条, 可供硫化={}h > {}h, 移入R3退出队列",
                                            dt.getEmbryoCode(), preMaterialCode, preAllocatedStock, preProduction,
                                            preVulcConsumption, preProjectedStock,
                                            preStockHours.setScale(2, BigDecimal.ROUND_HALF_UP), stockHoursCap);
                                    r2ExitedTasks.add(dt);
                                    preIter.remove();
                                }
                            }
                        }
                    }

                    // 更新 entry 为过滤后的列表
                    entry.setValue(structTasks);

                    if (structTasks.isEmpty()) {
                        log.info("  结构 {} R2预处理后无活跃任务，跳过", structName);
                        continue;
                    }
                    log.info("  结构 {} R2预处理后活跃任务数={}, 移入R3队列={}个",
                            structName, structTasks.size(),
                            (int) r2ExitedTasks.stream().filter(t -> structName.equals(t.getStructureName())).count());

                    // ==================== 变更2c: R2最小优先轮询分配 ====================
                    BigDecimal structDeferredTime = BigDecimal.ZERO;
                    int structGlobalRound = 0;

                    while (structDeferredTime.compareTo(remainingCapacity) < 0 && !structTasks.isEmpty()) {
                        structGlobalRound++;
                        int currentRound = structGlobalRound;

                        // 找动态可供硫化时长最小的任务（与预处理/6h退出公式一致：分配库存 + 已排产量 - 硫化消耗）
                        CoreScheduleAlgorithmService.DailyEmbryoTask minTask = null;
                        BigDecimal minStockHours = null;
                        for (CoreScheduleAlgorithmService.DailyEmbryoTask dt : structTasks) {
                            BigDecimal sh = BigDecimal.ZERO;
                            int dtSingleLhCap = productionCalculator.getSingleMoldDailyLhCapacity(dt.getMaterialCode(), context);
                            Integer dtMoldQty = dt.getVulcanizeMoldCount();
                            if (dtMoldQty != null && dtMoldQty > 0 && dtSingleLhCap > 0) {
                                int dtAllocatedStock = getCurrentStock(context, dt.getLhId());
                                int dtProduction = dt.getPlannedProduction() != null ? dt.getPlannedProduction() : 0;
                                int dtVulcCons = taskVulcConsumptionMap.getOrDefault(dt.getLhId(), 0);
                                int dtProjectedStock = dtAllocatedStock + dtProduction - dtVulcCons;
                                sh = productionCalculator.calculateStockHours(dtProjectedStock, dtSingleLhCap, dtMoldQty);
                            }
                            if (minStockHours == null || sh.compareTo(minStockHours) < 0) {
                                minStockHours = sh;
                                minTask = dt;
                            }
                        }
                        if (minTask == null) break;

                        String dtMaterialCode = minTask.getMaterialCode();
                        String dtEmbryoCode = minTask.getEmbryoCode();

                        // 检查：剩余需求
                        int remainingDemand = minTask.getDeferredRemainingDemand() != null ? minTask.getDeferredRemainingDemand() : 0;
                        if (remainingDemand <= 0) {
                            log.info("  [R2-剩余需求<=0] 移除：胎胚={}, stockHours={}h",
                                    minTask.getEmbryoCode(), minStockHours.setScale(2, BigDecimal.ROUND_HALF_UP));
                            structTasks.remove(minTask);
                            deferredSkippedEnding++;
                            skippedEndingList.add(minTask.getEmbryoCode() + "/" + dtMaterialCode);
                            continue;
                        }

                        // 分配一车：min(整车条数, 剩余需求)
                        int tripCapacity = getTripCapacity(minTask.getStructureName(), minTask.getEmbryoCode(), context);
                        int fallbackProduction = Math.min(tripCapacity, remainingDemand);
                        if (fallbackProduction <= 0) {
                            structTasks.remove(minTask);
                            continue;
                        }

                        // 检查：成型余量是否已耗尽（多任务共享同一物料余量）
                        Integer dtFormingRemainder = getFormingRemainder(dtMaterialCode, context);
                        int dtUsedRemainder = materialUsedFormingRemainder.getOrDefault(dtMaterialCode, 0);
                        int dtRemainingForming = dtFormingRemainder != null
                                ? (dtFormingRemainder - dtUsedRemainder) : Integer.MAX_VALUE;
                        // 共用胎胚：剩余需求取per-task需求和共享成型余量的较小值，确保共用物料任务间剩余需求正确递减
                        remainingDemand = Math.min(remainingDemand, dtRemainingForming);

                        // 停产封顶检查：R2补产不能超过停产反推上限
                        Integer r2ClosingRequired = minTask.getClosingRequiredStock();
                        if (r2ClosingRequired != null) {
                            int r2TaskStock = minTask.getCurrentStock() != null ? minTask.getCurrentStock() : 0;
                            int r2ThisShiftNeeded = Math.max(0, r2ClosingRequired - r2TaskStock);
                            int r2TotalProduced = minTask.getPlannedProduction() != null ? minTask.getPlannedProduction() : 0;
                            int r2RemainingClosingCapacity = r2ThisShiftNeeded - r2TotalProduced;
                            if (r2RemainingClosingCapacity <= 0) {
                                log.info("  [R2-停产封顶] 胎胚={}, 已达反推上限(closingRequiredStock={}, stock={}, 已排={}), 移入R3退出队列",
                                        minTask.getEmbryoCode(), r2ClosingRequired, r2TaskStock, r2TotalProduced);
                                minTask.setIsEndProduction(true);
                                r2ExitedTasks.add(minTask);
                                structTasks.remove(minTask);
                                continue;
                            }
                            fallbackProduction = Math.min(fallbackProduction, r2RemainingClosingCapacity);
                            if (fallbackProduction <= 0) {
                                structTasks.remove(minTask);
                                continue;
                            }
                        }

                        if (dtRemainingForming <= 0) {
                            log.info("  [R2-成型余量耗尽] 胎胚={}, 物料={}, 已用{}/总量{}, 收尾移出",
                                    dtEmbryoCode, dtMaterialCode, dtUsedRemainder, dtFormingRemainder);
                            if (minTask.getPlannedProduction() != null && minTask.getPlannedProduction() > 0
                                    && !isTaskAlreadyInResult(minTask, result)
                                    && !r2AddedToResultGlobal.contains(minTask)) {
                                minTask.setEndingExtraInventory(minTask.getPlannedProduction());
                                // 刷新endingSurplusQty为当前剩余成型余量，使handleEndingRemainder能正确判断isLastDay
                                minTask.setEndingSurplusQty(Math.max(0, dtRemainingForming));
                                handleEndingRemainder(minTask, context);
                                if (dtEmbryoCode != null && minTask.getEndingExtraInventory() != null
                                        && minTask.getEndingExtraInventory() > 0) {
                                    int endingDiff = minTask.getEndingExtraInventory() - minTask.getPlannedProduction();
                                    if (endingDiff != 0) {
                                        shiftFormingOutputMap.merge(dtEmbryoCode, endingDiff, Integer::sum);
                                        runningTotalProjectedStock += endingDiff;
                                    }
                                }
                                // 成型余量已耗尽，强制标记最后一批并回溯同物料其他任务
                                minTask.setIsLastEndingBatch(true);
                                log.info("  [R2-成型余量耗尽] 胎胚={} 强制标记 isLastEndingBatch=true", dtEmbryoCode);
                                List<CoreScheduleAlgorithmService.DailyEmbryoTask> allTasksForMaterial = materialTasksMap.get(dtMaterialCode);
                                if (allTasksForMaterial != null) {
                                    for (CoreScheduleAlgorithmService.DailyEmbryoTask prevTask : allTasksForMaterial) {
                                        if (prevTask != minTask && !Boolean.TRUE.equals(prevTask.getIsLastEndingBatch())) {
                                            prevTask.setIsLastEndingBatch(true);
                                            log.info("  [R2-回溯] 物料={}, 胎胚={} isLastEndingBatch→true",
                                                    dtMaterialCode, prevTask.getEmbryoCode());
                                        }
                                    }
                                }
                                boolean dtIsC = Boolean.TRUE.equals(minTask.getIsContinueTask());
                                boolean dtIsT = Boolean.TRUE.equals(minTask.getIsTrialTask());
                                if (dtIsC) result.getContinueTasks().add(minTask);
                                else if (dtIsT) result.getTrialTasks().add(minTask);
                                else result.getNewTasks().add(minTask);
                                r2AddedToResultGlobal.add(minTask);
                            }
                            structTasks.remove(minTask);
                            continue;
                        }
                        // 限制分配量不超过剩余成型余量
                        if (dtRemainingForming < fallbackProduction) {
                            fallbackProduction = dtRemainingForming;
                        }

                        // 检查：立库库容（维度一 — 空间）
                        boolean spaceSkip = false;
                        String spaceSkipReason = "";
                        if (dtEmbryoCode != null && warehouseCapacity > 0
                                && runningTotalProjectedStock >= warehouseThreshold) {
                            spaceSkip = true;
                            spaceSkipReason = "[空间]全部预计=" + runningTotalProjectedStock + ">=上限=" + warehouseThreshold;
                        }
                        if (spaceSkip) {
                            log.info("  [R2-空间(库容超限)] 跳过：胎胚={}, 全部预计={}, 原因: {}",
                                    dtEmbryoCode, runningTotalProjectedStock, spaceSkipReason);
                            deferredSkippedWarehouse++;
                            skippedWarehouseList.add(minTask.getEmbryoCode() + "/" + dtMaterialCode);
                            // 之前已分配过产量的任务需要收尾处理并加入结果
                            if (minTask.getPlannedProduction() != null && minTask.getPlannedProduction() > 0
                                    && !isTaskAlreadyInResult(minTask, result)
                                    && !r2AddedToResultGlobal.contains(minTask)) {
                                minTask.setEndingExtraInventory(minTask.getPlannedProduction());
                                // 刷新endingSurplusQty为当前剩余成型余量，使handleEndingRemainder能正确判断isLastDay
                                minTask.setEndingSurplusQty(Math.max(0, dtRemainingForming));
                                handleEndingRemainder(minTask, context);
                                if (dtEmbryoCode != null && minTask.getEndingExtraInventory() != null
                                        && minTask.getEndingExtraInventory() > 0) {
                                    int endingDiff = minTask.getEndingExtraInventory() - minTask.getPlannedProduction();
                                    if (endingDiff != 0) {
                                        shiftFormingOutputMap.merge(dtEmbryoCode, endingDiff, Integer::sum);
                                        runningTotalProjectedStock += endingDiff;
                                    }
                                }
                                boolean dtIsC = Boolean.TRUE.equals(minTask.getIsContinueTask());
                                boolean dtIsT = Boolean.TRUE.equals(minTask.getIsTrialTask());
                                if (dtIsC) {
                                    result.getContinueTasks().add(minTask);
                                } else if (dtIsT) {
                                    result.getTrialTasks().add(minTask);
                                } else {
                                    result.getNewTasks().add(minTask);
                                }
                                r2AddedToResultGlobal.add(minTask);
                                log.info("  [R2-空间跳过] 胎胚={}, 本轮不分配, 保留首轮产量={}",
                                        minTask.getEmbryoCode(), minTask.getPlannedProduction());
                            }
                            structTasks.remove(minTask);
                            continue;
                        }

                        // 产能检查：计算本项耗时
                        Integer dailyLhCapacity = productionCalculator.getDoubleMoldDailyLhCapacity(dtMaterialCode, context);
                        BigDecimal avgRatio = structureAvgRatioCache
                                .computeIfAbsent(structName, k -> {
                                    List<MpCxCapacityConfiguration> machines = structureRecommendedMachinesCache.get(k);
                                    return machines != null ? calculateStructureAvgRatio(machines, k, context) : BigDecimal.ONE;
                                });
                        BigDecimal tripTime = BigDecimal.ZERO;
                        BigDecimal timePerTire = BigDecimal.ZERO;
                        if (dailyLhCapacity != null && dailyLhCapacity > 0 && avgRatio.compareTo(BigDecimal.ZERO) > 0) {
                            timePerTire = productionCalculator.calculateTimePerTire(avgRatio, dailyLhCapacity);
                            tripTime = timePerTire.multiply(BigDecimal.valueOf(fallbackProduction));
                            if (structDeferredTime.add(tripTime).compareTo(remainingCapacity) > 0) {
                                log.info("  [R2-产能不足] 结构={}, 胎胚={}, 物料={}, 第{}轮, 已用={}s({}h) + 本项={}s({}h) > 剩余产能={}s({}h), break退出",
                                        structName, minTask.getEmbryoCode(), dtMaterialCode, currentRound,
                                        structDeferredTime.toBigInteger(),
                                        structDeferredTime.divide(BigDecimal.valueOf(3600), 1, BigDecimal.ROUND_HALF_UP),
                                        tripTime.toBigInteger(),
                                        tripTime.divide(BigDecimal.valueOf(3600), 1, BigDecimal.ROUND_HALF_UP),
                                        remainingCapacity.toBigInteger(),
                                        remainingCapacity.divide(BigDecimal.valueOf(3600), 1, BigDecimal.ROUND_HALF_UP));
                                deferredSkippedCapacity++;
                                skippedCapacityList.add(minTask.getEmbryoCode() + "/" + dtMaterialCode);
                                break;
                            }
                        }

                        // ==================== 变更2c: 维度二（时间）6h退出条件 ====================
                        boolean exitToR3 = false;
                        if (stockHoursCapEnabled && dtEmbryoCode != null) {
                            int dtTaskMoldQty = minTask.getVulcanizeMoldCount();
                            int dtSingleLhCap = productionCalculator.getSingleMoldDailyLhCapacity(dtMaterialCode, context);
                            if (dtTaskMoldQty > 0 && dtSingleLhCap > 0) {
                                int currentPP = minTask.getPlannedProduction() != null ? minTask.getPlannedProduction() : 0;
                                int taskAllocatedStock = getCurrentStock(context, minTask.getLhId());
                                int taskVulcConsumption = taskVulcConsumptionMap.getOrDefault(minTask.getLhId(), 0);
                                int projectedAfterAdd = taskAllocatedStock + currentPP + fallbackProduction - taskVulcConsumption;
                                BigDecimal afterAddHours = productionCalculator.calculateStockHours(projectedAfterAdd, dtSingleLhCap, dtTaskMoldQty);
                                if (afterAddHours.compareTo(BigDecimal.valueOf(stockHoursCap)) > 0) {
                                    exitToR3 = true;
                                    log.info("  [R2-6h退出] 胎胚={}, 分配库存={}, 已排产量={}, 硫化消耗={}, +本轮{}条 = 预计库存{}条, 可供硫化={}h > {}h, 本轮分配后移入R3",
                                            dtEmbryoCode, taskAllocatedStock, currentPP, taskVulcConsumption,
                                            fallbackProduction, projectedAfterAdd,
                                            afterAddHours.setScale(2, BigDecimal.ROUND_HALF_UP), stockHoursCap);
                                } else {
                                    log.info("  【可供硫化管控】胎胚={}, 分配库存={}, 已排产量={}, 硫化消耗={}, +本轮{}条 = 预计库存{}条, 可供硫化={}h, {}h上限, 未超限→通过",
                                            dtEmbryoCode, taskAllocatedStock, currentPP, taskVulcConsumption,
                                            fallbackProduction, projectedAfterAdd,
                                            afterAddHours.setScale(2, BigDecimal.ROUND_HALF_UP), stockHoursCap);
                                }
                            }
                        }

                        // 确认分配
                        int currentPP = minTask.getPlannedProduction() != null ? minTask.getPlannedProduction() : 0;
                        minTask.setPlannedProduction(currentPP + fallbackProduction);
                        minTask.setRequiredCars(productionCalculator.calculateRequiredCars(minTask.getPlannedProduction(), tripCapacity));
                        minTask.setEndingExtraInventory(minTask.getPlannedProduction());
                        deferredAllocated++;

                        // 更新累计耗时
                        structDeferredTime = structDeferredTime.add(tripTime);

                        // 更新剩余需求
                        int newRemaining = remainingDemand - fallbackProduction;
                        minTask.setDeferredRemainingDemand(newRemaining);

                        // 日志
                        String taskKey = minTask.getEmbryoCode() != null ? minTask.getEmbryoCode() : minTask.getMaterialCode();
                        int[] tracker = taskRoundTracker.computeIfAbsent(taskKey, k -> new int[]{0, 0});
                        tracker[0] += fallbackProduction;
                        tracker[1]++;
                        String roundTimeDisplay = tripTime.compareTo(BigDecimal.ZERO) > 0
                                ? tripTime.divide(BigDecimal.valueOf(3600), 2, BigDecimal.ROUND_HALF_UP) + "h"
                                : "-";
                        log.info("  [R2-第{}轮] 结构={}, 胎胚={}, 物料={}, stockHours={}h, 本轮={}条({}), 累计轮/{}条, 当前计划量={}",
                                currentRound, structName, minTask.getEmbryoCode(), dtMaterialCode,
                                minStockHours.setScale(2, BigDecimal.ROUND_HALF_UP),
                                fallbackProduction, roundTimeDisplay, tracker[0], minTask.getPlannedProduction());

                        // 累加本班次成型产出
                        if (dtEmbryoCode != null && fallbackProduction > 0) {
                            int prevRunning = runningTotalProjectedStock;
                            shiftFormingOutputMap.merge(dtEmbryoCode, fallbackProduction, Integer::sum);
                            runningTotalProjectedStock += fallbackProduction;

                            if (warehouseCapacity > 0) {
                                int dtVulcCons = minTask.getVulcanizeDemand() != null ? minTask.getVulcanizeDemand() : 0;
                                int netStockChange = fallbackProduction - dtVulcCons;
                                int warehouseRemainBefore = Math.max(0, warehouseThreshold - prevRunning);
                                int warehouseRemainAfter = Math.max(0, warehouseThreshold - runningTotalProjectedStock);
                                log.info("  【立库库容管控】胎胚={}, 物料={}, 成型产出={}条, 硫化消耗={}条, 净入立库={}条, 加入前立库剩余={}条, 加入后立库剩余={}条",
                                        dtEmbryoCode, dtMaterialCode, fallbackProduction, dtVulcCons, netStockChange,
                                        warehouseRemainBefore, warehouseRemainAfter);
                            }
                        }

                        // 更新已使用的成型余量
                        if (fallbackProduction > 0) {
                            materialUsedFormingRemainder.merge(dtMaterialCode, fallbackProduction, Integer::sum);
                        }

                        // 6h退出 → 移入 r2ExitedTasks
                        if (exitToR3) {
                            log.info("  [R2-移入R3] 胎胚={}, 当前计划量={}, 剩余需求={}",
                                    minTask.getEmbryoCode(), minTask.getPlannedProduction(), newRemaining);
                            r2ExitedTasks.add(minTask);
                            structTasks.remove(minTask);
                            continue;
                        }

                        // 剩余需求耗尽：收尾处理 + 加入结果列表
                        if (newRemaining <= 0) {
                            minTask.setEndingExtraInventory(minTask.getPlannedProduction());
                            // 刷新endingSurplusQty为当前剩余成型余量，使handleEndingRemainder能正确判断isLastDay
                            Integer r2EndFormingRemainder = getFormingRemainder(dtMaterialCode, context);
                            int r2EndUsedRemainder = materialUsedFormingRemainder.getOrDefault(dtMaterialCode, 0);
                            int r2EndRemainingForming = r2EndFormingRemainder != null
                                    ? Math.max(0, r2EndFormingRemainder - r2EndUsedRemainder) : 0;
                            minTask.setEndingSurplusQty(r2EndRemainingForming);
                            handleEndingRemainder(minTask, context);

                            // 成型余量恰好耗尽时handleEndingRemainder因endingSurplusQty<=0提前返回，
                            // 需在此补充标记isLastEndingBatch
                            if (r2EndRemainingForming <= 0 && !Boolean.TRUE.equals(minTask.getIsLastEndingBatch())) {
                                minTask.setIsLastEndingBatch(true);
                                log.info("  [R2-分配完成] 胎胚={} 强制标记 isLastEndingBatch=true（成型余量耗尽）", dtEmbryoCode);
                            }

                            if (dtEmbryoCode != null && minTask.getEndingExtraInventory() != null
                                    && minTask.getEndingExtraInventory() > 0) {
                                int endingDiff = minTask.getEndingExtraInventory() - minTask.getPlannedProduction();
                                if (endingDiff != 0) {
                                    shiftFormingOutputMap.merge(dtEmbryoCode, endingDiff, Integer::sum);
                                    runningTotalProjectedStock += endingDiff;
                                }
                            }

                            if (Boolean.TRUE.equals(minTask.getIsLastEndingBatch())) {
                                List<CoreScheduleAlgorithmService.DailyEmbryoTask> allTasksForMaterial = materialTasksMap.get(dtMaterialCode);
                                if (allTasksForMaterial != null) {
                                    for (CoreScheduleAlgorithmService.DailyEmbryoTask prevTask : allTasksForMaterial) {
                                        if (prevTask != minTask && !Boolean.TRUE.equals(prevTask.getIsLastEndingBatch())) {
                                            prevTask.setIsLastEndingBatch(true);
                                            log.info("  第二轮回溯更新 isLastEndingBatch: 物料={}, 胎胚={} → true",
                                                    dtMaterialCode, prevTask.getEmbryoCode());
                                        }
                                    }
                                }
                            }

                            if (minTask.getPlannedProduction() != null && minTask.getPlannedProduction() > 0
                                    && !isTaskAlreadyInResult(minTask, result)
                                    && !r2AddedToResultGlobal.contains(minTask)) {
                                boolean dtIsC = Boolean.TRUE.equals(minTask.getIsContinueTask());
                                boolean dtIsT = Boolean.TRUE.equals(minTask.getIsTrialTask());
                                if (dtIsC) {
                                    result.getContinueTasks().add(minTask);
                                } else if (dtIsT) {
                                    result.getTrialTasks().add(minTask);
                                } else {
                                    result.getNewTasks().add(minTask);
                                }
                                r2AddedToResultGlobal.add(minTask);
                            } else if (minTask.getPlannedProduction() == null || minTask.getPlannedProduction() <= 0) {
                                log.info("  [R2-收尾舍弃] 胎胚={}, plannedProduction={}",
                                        minTask.getEmbryoCode(), minTask.getPlannedProduction());
                            }

                            int[] finishTracker = taskRoundTracker.getOrDefault(taskKey, new int[]{0, 0});
                            log.info("  [R2-分配完成] 结构={}, 胎胚={}, 物料={}, 共补{}轮/{}条, 最终计划量={}",
                                    structName, minTask.getEmbryoCode(), dtMaterialCode,
                                    finishTracker[1], finishTracker[0], minTask.getPlannedProduction());
                            structTasks.remove(minTask);
                        }
                    }
                    // 回写R2耗时到 structureCumulativeTimeMap（供R3计算剩余产能）
                    if (structDeferredTime.compareTo(BigDecimal.ZERO) > 0) {
                        structureCumulativeTimeMap.merge(structName, structDeferredTime, BigDecimal::add);
                        log.info("  结构 {} R2回写耗时={}s({}h), 累计={}s({}h)", structName,
                                structDeferredTime.stripTrailingZeros().toPlainString(),
                                structDeferredTime.divide(BigDecimal.valueOf(SECONDS_PER_HOUR), 1, BigDecimal.ROUND_HALF_UP),
                                structureCumulativeTimeMap.get(structName).stripTrailingZeros().toPlainString(),
                                structureCumulativeTimeMap.get(structName).divide(BigDecimal.valueOf(SECONDS_PER_HOUR), 1, BigDecimal.ROUND_HALF_UP));
                    }
                }

                int remainingDeferred = 0;
                for (List<CoreScheduleAlgorithmService.DailyEmbryoTask> remaining : structureDeferredMap.values()) {
                    for (CoreScheduleAlgorithmService.DailyEmbryoTask rt : remaining) {
                        remainingDeferred++;
                        String rtKey = rt.getEmbryoCode() != null ? rt.getEmbryoCode() : rt.getMaterialCode();
                        int[] tracker = taskRoundTracker.getOrDefault(rtKey, new int[]{0, 0});
                        int rtRemaining = rt.getDeferredRemainingDemand() != null ? rt.getDeferredRemainingDemand() : 0;
                        log.info("  [R2-未完成] 结构={}, 胎胚={}, 已补{}轮/{}条, 剩余需求={}, 当前计划量={}",
                                rt.getStructureName(), rt.getEmbryoCode(),
                                tracker[1], tracker[0], rtRemaining, rt.getPlannedProduction());
                        if (rt.getPlannedProduction() != null && rt.getPlannedProduction() > 0
                                && !isTaskAlreadyInResult(rt, result)
                                && !r2AddedToResultGlobal.contains(rt)) {
                            rt.setEndingExtraInventory(rt.getPlannedProduction());
                            handleEndingRemainder(rt, context);
                            String rtEmbryoCode = rt.getEmbryoCode();
                            if (rtEmbryoCode != null && rt.getEndingExtraInventory() != null && rt.getEndingExtraInventory() > 0) {
                                int endingDiff = rt.getEndingExtraInventory() - rt.getPlannedProduction();
                                if (endingDiff != 0) {
                                    shiftFormingOutputMap.merge(rtEmbryoCode, endingDiff, Integer::sum);
                                    runningTotalProjectedStock += endingDiff;
                                }
                            }
                            boolean rtIsContinue = Boolean.TRUE.equals(rt.getIsContinueTask());
                            boolean rtIsTrial = Boolean.TRUE.equals(rt.getIsTrialTask());
                            if (rtIsContinue) {
                                result.getContinueTasks().add(rt);
                            } else if (rtIsTrial) {
                                result.getTrialTasks().add(rt);
                            } else {
                                result.getNewTasks().add(rt);
                            }
                            r2AddedToResultGlobal.add(rt);
                            log.info("  [R2-未完成但已分配] 胎胚={}, 最终计划量={}", rt.getEmbryoCode(), rt.getPlannedProduction());
                        }
                    }
                }
                log.info("【第二轮分配结果】总数:{}个 | 已分配:{}个 | 未完成:{}个 | 跳过:产能不足{}个/成型余量耗尽{}个/收尾余量<=0{}个/立库满{}个",
                        deferredTotal, deferredAllocated, remainingDeferred,
                        deferredSkippedCapacity, deferredSkippedForming, deferredSkippedEnding, deferredSkippedWarehouse);
                if (!skippedCapacityList.isEmpty()) {
                    log.info("  [R2-产能不足明细] {}", String.join(", ", skippedCapacityList));
                }
                if (!skippedFormingList.isEmpty()) {
                    log.info("  [R2-成型余量耗尽明细] {}", String.join(", ", skippedFormingList));
                }
                if (!skippedEndingList.isEmpty()) {
                    log.info("  [R2-收尾余量<=0明细] {}", String.join(", ", skippedEndingList));
                }
                if (!skippedWarehouseList.isEmpty()) {
                    log.info("  [R2-立库满明细] {}", String.join(", ", skippedWarehouseList));
                }
            }

            // ==================== 变更2d: 第三轮（R3）— R2退出任务的无6h限制分配 ====================
            if (!r2ExitedTasks.isEmpty()) {
                log.info("【第三轮分配（R3）】开始处理 {} 个R2退出任务（无6h限制）", r2ExitedTasks.size());
                int r3Allocated = 0;
                int r3SkippedCapacity = 0;
                int r3SkippedWarehouse = 0;
                int r3SkippedForming = 0;
                List<String> r3SkippedCapacityList = new ArrayList<>();
                List<String> r3SkippedWarehouseList = new ArrayList<>();
                List<String> r3SkippedFormingList = new ArrayList<>();

                // 按结构分组 r2ExitedTasks
                Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> r3StructureMap = new LinkedHashMap<>();
                for (CoreScheduleAlgorithmService.DailyEmbryoTask dt : r2ExitedTasks) {
                    String structName = dt.getStructureName() != null ? dt.getStructureName() : "";
                    r3StructureMap.computeIfAbsent(structName, k -> new ArrayList<>()).add(dt);
                }

                // 重新计算每个结构的剩余产能（使用更新后的 structureCumulativeTimeMap）
                Map<String, BigDecimal> r3RemainingCapacityMap = new HashMap<>();
                for (String structName : r3StructureMap.keySet()) {
                    if (structName.isEmpty() || context.getStructureAllocationMap() == null) {
                        r3RemainingCapacityMap.put(structName, BigDecimal.ZERO);
                        continue;
                    }
                    List<MpCxCapacityConfiguration> recommendedMachines = structureRecommendedMachinesCache
                            .computeIfAbsent(structName, k -> getRecommendedMachinesForStructure(k, scheduleDate, context));
                    if (recommendedMachines == null || recommendedMachines.isEmpty()) {
                        r3RemainingCapacityMap.put(structName, BigDecimal.ZERO);
                        continue;
                    }
                    BigDecimal totalCapacitySeconds = structureAdvanceAvailableCapacityMap.containsKey(structName)
                            ? structureAdvanceAvailableCapacityMap.get(structName)
                            : BigDecimal.valueOf(recommendedMachines.size())
                            .multiply(BigDecimal.valueOf(SECONDS_PER_SHIFT));
                    BigDecimal cumulativeTime = structureCumulativeTimeMap.getOrDefault(structName, BigDecimal.ZERO);
                    BigDecimal remaining = totalCapacitySeconds.subtract(cumulativeTime);
                    r3RemainingCapacityMap.put(structName, remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO);
                    log.info("  结构 {} R3剩余产能: 总={}s({}h), 已用={}s({}h), 剩余={}s({}h)",
                            structName, totalCapacitySeconds.toBigInteger(),
                            totalCapacitySeconds.divide(BigDecimal.valueOf(SECONDS_PER_HOUR), 1, BigDecimal.ROUND_HALF_UP),
                            cumulativeTime.toBigInteger(),
                            cumulativeTime.divide(BigDecimal.valueOf(SECONDS_PER_HOUR), 1, BigDecimal.ROUND_HALF_UP),
                            remaining.toBigInteger(),
                            remaining.divide(BigDecimal.valueOf(SECONDS_PER_HOUR), 1, BigDecimal.ROUND_HALF_UP));
                }

                // R3 核心逻辑：最小优先、逐车轮询、结构全局轮次，不检查6h限制
                for (Map.Entry<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> entry : r3StructureMap.entrySet()) {
                    String structName = entry.getKey();
                    List<CoreScheduleAlgorithmService.DailyEmbryoTask> structTasks = new ArrayList<>(entry.getValue());
                    BigDecimal remainingCapacity = r3RemainingCapacityMap.getOrDefault(structName, BigDecimal.ZERO);

                    log.info("==================== R3 结构={}, 剩余产能={}s({}h), 初始任务数={} ====================",
                            structName, remainingCapacity.stripTrailingZeros().toPlainString(),
                            remainingCapacity.divide(BigDecimal.valueOf(SECONDS_PER_HOUR), 1, BigDecimal.ROUND_HALF_UP),
                            structTasks.size());

                    if (remainingCapacity.compareTo(BigDecimal.ZERO) <= 0) {
                        log.info("  R3 结构 {} 剩余产能为0，跳过", structName);
                        continue;
                    }

                    int totalRecommendedMachines = structureRecommendedMachinesCache.get(structName) != null
                            ? structureRecommendedMachinesCache.get(structName).size() : 0;
                    BigDecimal totalCapacity = structureAdvanceAvailableCapacityMap.containsKey(structName)
                            ? structureAdvanceAvailableCapacityMap.get(structName)
                            : BigDecimal.valueOf(totalRecommendedMachines)
                            .multiply(BigDecimal.valueOf(SECONDS_PER_SHIFT));

                    // R3预处理：移出成型余量已耗尽的任务
                    Iterator<CoreScheduleAlgorithmService.DailyEmbryoTask> r3PreIter = structTasks.iterator();
                    while (r3PreIter.hasNext()) {
                        CoreScheduleAlgorithmService.DailyEmbryoTask dt = r3PreIter.next();
                        String preMaterialCode = dt.getMaterialCode();
                        Integer preFormingRemainder = getFormingRemainder(preMaterialCode, context);
                        int preUsedRemainder = materialUsedFormingRemainder.getOrDefault(preMaterialCode, 0);
                        if (preFormingRemainder != null && (preFormingRemainder - preUsedRemainder) <= 0) {
                            log.info("  [R3-预处理-成型余量耗尽] 胎胚={}, 物料={}, 已用{}/总量{}, 跳过",
                                    dt.getEmbryoCode(), preMaterialCode, preUsedRemainder, preFormingRemainder);
                            r3SkippedForming++;
                            r3SkippedFormingList.add(dt.getEmbryoCode() + "/" + preMaterialCode);
                            r3PreIter.remove();
                        }
                    }

                    if (structTasks.isEmpty()) {
                        log.info("  R3 结构 {} 预处理后无任务，跳过", structName);
                        continue;
                    }

                    BigDecimal structDeferredTime = BigDecimal.ZERO;
                    int structGlobalRound = 0;

                    while (structDeferredTime.compareTo(remainingCapacity) < 0 && !structTasks.isEmpty()) {
                        structGlobalRound++;

                        // 最小优先：找当前可供硫化时长最小的任务（动态计算，反映R2/R3已排产量，实现自然轮转）
                        CoreScheduleAlgorithmService.DailyEmbryoTask minTask = null;
                        BigDecimal minStockHours = null;
                        for (CoreScheduleAlgorithmService.DailyEmbryoTask dt : structTasks) {
                            // 动态计算可供硫化时长 = (分配库存 + 已排产量 - 硫化消耗) × 单胎单模时长 / 模数 / 3600
                            BigDecimal sh = BigDecimal.ZERO;
                            int dtSingleLhCap = productionCalculator.getSingleMoldDailyLhCapacity(dt.getMaterialCode(), context);
                            Integer dtMoldQty = dt.getVulcanizeMoldCount();
                            if (dtMoldQty != null && dtMoldQty > 0 && dtSingleLhCap > 0) {
                                int dtAllocatedStock = getCurrentStock(context, dt.getLhId());
                                int dtProduction = dt.getPlannedProduction() != null ? dt.getPlannedProduction() : 0;
                                int dtVulcCons = taskVulcConsumptionMap.getOrDefault(dt.getLhId(), 0);
                                int dtProjectedStock = dtAllocatedStock + dtProduction - dtVulcCons;
                                sh = productionCalculator.calculateStockHours(dtProjectedStock, dtSingleLhCap, dtMoldQty);
                            } else {
                                // 无法动态计算时回退到静态stockHours
                                sh = dt.getStockHours();
                                if (sh == null) sh = BigDecimal.ZERO;
                            }
                            if (minStockHours == null || sh.compareTo(minStockHours) < 0) {
                                minStockHours = sh;
                                minTask = dt;
                            }
                        }
                        if (minTask == null) break;

                        String dtMaterialCode = minTask.getMaterialCode();
                        String dtEmbryoCode = minTask.getEmbryoCode();

                        // 检查：剩余需求
                        int remainingDemand = minTask.getDeferredRemainingDemand() != null ? minTask.getDeferredRemainingDemand() : 0;
                        if (remainingDemand <= 0) {
                            structTasks.remove(minTask);
                            continue;
                        }

                        int tripCapacity = getTripCapacity(minTask.getStructureName(), minTask.getEmbryoCode(), context);
                        int fallbackProduction = Math.min(tripCapacity, remainingDemand);
                        if (fallbackProduction <= 0) {
                            structTasks.remove(minTask);
                            continue;
                        }

                        // 检查：成型余量是否已耗尽（多任务共享同一物料余量）
                        Integer dtFormingRemainder = getFormingRemainder(dtMaterialCode, context);
                        int dtUsedRemainder = materialUsedFormingRemainder.getOrDefault(dtMaterialCode, 0);
                        int dtRemainingForming = dtFormingRemainder != null
                                ? (dtFormingRemainder - dtUsedRemainder) : Integer.MAX_VALUE;
                        // 共用胎胚：剩余需求取per-task需求和共享成型余量的较小值，确保共用物料任务间剩余需求正确递减
                        remainingDemand = Math.min(remainingDemand, dtRemainingForming);

                        // 停产封顶检查：R3补产不能超过停产反推上限
                        Integer r3ClosingRequired = minTask.getClosingRequiredStock();
                        if (r3ClosingRequired != null) {
                            int r3TaskStock = minTask.getCurrentStock() != null ? minTask.getCurrentStock() : 0;
                            int r3ThisShiftNeeded = Math.max(0, r3ClosingRequired - r3TaskStock);
                            int r3TotalProduced = minTask.getPlannedProduction() != null ? minTask.getPlannedProduction() : 0;
                            int r3RemainingClosingCapacity = r3ThisShiftNeeded - r3TotalProduced;
                            if (r3RemainingClosingCapacity <= 0) {
                                log.info("  [R3-停产封顶] 胎胚={}, 已达反推上限(closingRequiredStock={}, stock={}, 已排={}), 移除",
                                        minTask.getEmbryoCode(), r3ClosingRequired, r3TaskStock, r3TotalProduced);
                                minTask.setIsEndProduction(true);
                                structTasks.remove(minTask);
                                continue;
                            }
                            fallbackProduction = Math.min(fallbackProduction, r3RemainingClosingCapacity);
                            if (fallbackProduction <= 0) {
                                structTasks.remove(minTask);
                                continue;
                            }
                        }

                        if (dtRemainingForming <= 0) {
                            log.info("  [R3-成型余量耗尽] 胎胚={}, 物料={}, 已用{}/总量{}, 收尾移出",
                                    dtEmbryoCode, dtMaterialCode, dtUsedRemainder, dtFormingRemainder);
                            if (minTask.getPlannedProduction() != null && minTask.getPlannedProduction() > 0
                                    && !isTaskAlreadyInResult(minTask, result)
                                    && !r3AddedToResultGlobal.contains(minTask) && !r2AddedToResultGlobal.contains(minTask)) {
                                minTask.setEndingExtraInventory(minTask.getPlannedProduction());
                                // 刷新endingSurplusQty为当前剩余成型余量，使handleEndingRemainder能正确判断isLastDay
                                minTask.setEndingSurplusQty(Math.max(0, dtRemainingForming));
                                handleEndingRemainder(minTask, context);
                                if (dtEmbryoCode != null && minTask.getEndingExtraInventory() != null
                                        && minTask.getEndingExtraInventory() > 0) {
                                    int endingDiff = minTask.getEndingExtraInventory() - minTask.getPlannedProduction();
                                    if (endingDiff != 0) {
                                        shiftFormingOutputMap.merge(dtEmbryoCode, endingDiff, Integer::sum);
                                        runningTotalProjectedStock += endingDiff;
                                    }
                                }
                                // 成型余量已耗尽，强制标记最后一批并回溯同物料其他任务
                                minTask.setIsLastEndingBatch(true);
                                log.info("  [R3-成型余量耗尽] 胎胚={} 强制标记 isLastEndingBatch=true", dtEmbryoCode);
                                List<CoreScheduleAlgorithmService.DailyEmbryoTask> allTasksForMaterial = materialTasksMap.get(dtMaterialCode);
                                if (allTasksForMaterial != null) {
                                    for (CoreScheduleAlgorithmService.DailyEmbryoTask prevTask : allTasksForMaterial) {
                                        if (prevTask != minTask && !Boolean.TRUE.equals(prevTask.getIsLastEndingBatch())) {
                                            prevTask.setIsLastEndingBatch(true);
                                            log.info("  [R3-回溯] 物料={}, 胎胚={} isLastEndingBatch→true",
                                                    dtMaterialCode, prevTask.getEmbryoCode());
                                        }
                                    }
                                }
                                boolean dtIsC = Boolean.TRUE.equals(minTask.getIsContinueTask());
                                boolean dtIsT = Boolean.TRUE.equals(minTask.getIsTrialTask());
                                if (dtIsC) result.getContinueTasks().add(minTask);
                                else if (dtIsT) result.getTrialTasks().add(minTask);
                                else result.getNewTasks().add(minTask);
                                r3AddedToResultGlobal.add(minTask);
                            }
                            structTasks.remove(minTask);
                            continue;
                        }
                        // 限制分配量不超过剩余成型余量
                        if (dtRemainingForming < fallbackProduction) {
                            fallbackProduction = dtRemainingForming;
                        }

                        // 检查：立库库容（维度一 — 空间）— R3保留空间检查
                        boolean spaceSkip = false;
                        if (dtEmbryoCode != null && warehouseCapacity > 0
                                && runningTotalProjectedStock >= warehouseThreshold) {
                            spaceSkip = true;
                        }
                        if (spaceSkip) {
                            log.info("  [R3-空间(库容超限)] 跳过：胎胚={}, 全部预计={}>=上限={}",
                                    dtEmbryoCode, runningTotalProjectedStock, warehouseThreshold);
                            r3SkippedWarehouse++;
                            r3SkippedWarehouseList.add(minTask.getEmbryoCode() + "/" + dtMaterialCode);
                            if (minTask.getPlannedProduction() != null && minTask.getPlannedProduction() > 0
                                    && !isTaskAlreadyInResult(minTask, result)
                                    && !r3AddedToResultGlobal.contains(minTask) && !r2AddedToResultGlobal.contains(minTask)) {
                                minTask.setEndingExtraInventory(minTask.getPlannedProduction());
                                handleEndingRemainder(minTask, context);
                                boolean dtIsC = Boolean.TRUE.equals(minTask.getIsContinueTask());
                                boolean dtIsT = Boolean.TRUE.equals(minTask.getIsTrialTask());
                                if (dtIsC) {
                                    result.getContinueTasks().add(minTask);
                                } else if (dtIsT) {
                                    result.getTrialTasks().add(minTask);
                                } else {
                                    result.getNewTasks().add(minTask);
                                }
                                r3AddedToResultGlobal.add(minTask);
                            }
                            structTasks.remove(minTask);
                            continue;
                        }

                        // 产能检查
                        Integer dailyLhCapacity = productionCalculator.getDoubleMoldDailyLhCapacity(dtMaterialCode, context);
                        BigDecimal avgRatio = structureAvgRatioCache
                                .computeIfAbsent(structName, k -> {
                                    List<MpCxCapacityConfiguration> machines = structureRecommendedMachinesCache.get(k);
                                    return machines != null ? calculateStructureAvgRatio(machines, k, context) : BigDecimal.ONE;
                                });
                        if (dailyLhCapacity != null && dailyLhCapacity > 0 && avgRatio.compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal timePerTire = productionCalculator.calculateTimePerTire(avgRatio, dailyLhCapacity);
                            BigDecimal tripTime = timePerTire.multiply(BigDecimal.valueOf(fallbackProduction));
                            if (structDeferredTime.add(tripTime).compareTo(remainingCapacity) > 0) {
                                log.info("  [R3-产能不足] 结构={}, 胎胚={}, 第{}轮, 已用={}s + 本项={}s > 剩余产能={}s, break退出",
                                        structName, minTask.getEmbryoCode(), structGlobalRound,
                                        structDeferredTime.toBigInteger(), tripTime.toBigInteger(),
                                        remainingCapacity.toBigInteger());
                                r3SkippedCapacity++;
                                r3SkippedCapacityList.add(minTask.getEmbryoCode() + "/" + dtMaterialCode);
                                break;
                            }
                            structDeferredTime = structDeferredTime.add(tripTime);
                        }

                        // R3不检查6h限制，直接分配
                        int currentPP = minTask.getPlannedProduction() != null ? minTask.getPlannedProduction() : 0;
                        minTask.setPlannedProduction(currentPP + fallbackProduction);
                        minTask.setRequiredCars(productionCalculator.calculateRequiredCars(minTask.getPlannedProduction(), tripCapacity));
                        minTask.setEndingExtraInventory(minTask.getPlannedProduction());
                        r3Allocated++;

                        // 更新剩余需求
                        int newRemaining = remainingDemand - fallbackProduction;
                        minTask.setDeferredRemainingDemand(newRemaining);

                        log.info("  [R3-第{}轮] 结构={}, 胎胚={}, 物料={}, stockHours={}h, 本轮={}条, 当前计划量={}, 剩余需求={}",
                                structGlobalRound, structName, minTask.getEmbryoCode(), dtMaterialCode,
                                minStockHours.setScale(2, BigDecimal.ROUND_HALF_UP),
                                fallbackProduction, minTask.getPlannedProduction(), newRemaining);

                        // 累加本班次成型产出
                        if (dtEmbryoCode != null && fallbackProduction > 0) {
                            shiftFormingOutputMap.merge(dtEmbryoCode, fallbackProduction, Integer::sum);
                            runningTotalProjectedStock += fallbackProduction;
                        }

                        // 更新已使用的成型余量
                        if (fallbackProduction > 0) {
                            materialUsedFormingRemainder.merge(dtMaterialCode, fallbackProduction, Integer::sum);
                        }

                        // 剩余需求耗尽：收尾处理 + 加入结果列表
                        if (newRemaining <= 0) {
                            minTask.setEndingExtraInventory(minTask.getPlannedProduction());
                            // 刷新endingSurplusQty为当前剩余成型余量，使handleEndingRemainder能正确判断isLastDay
                            Integer r3EndFormingRemainder = getFormingRemainder(dtMaterialCode, context);
                            int r3EndUsedRemainder = materialUsedFormingRemainder.getOrDefault(dtMaterialCode, 0);
                            int r3EndRemainingForming = r3EndFormingRemainder != null
                                    ? Math.max(0, r3EndFormingRemainder - r3EndUsedRemainder) : 0;
                            minTask.setEndingSurplusQty(r3EndRemainingForming);
                            handleEndingRemainder(minTask, context);

                            // 成型余量恰好耗尽时handleEndingRemainder因endingSurplusQty<=0提前返回，
                            // 需在此补充标记isLastEndingBatch并回溯同物料其他任务
                            if (r3EndRemainingForming <= 0 && !Boolean.TRUE.equals(minTask.getIsLastEndingBatch())) {
                                minTask.setIsLastEndingBatch(true);
                                log.info("  [R3-分配完成] 胎胚={} 强制标记 isLastEndingBatch=true（成型余量耗尽）", dtEmbryoCode);
                                List<CoreScheduleAlgorithmService.DailyEmbryoTask> allTasksForMaterial = materialTasksMap.get(dtMaterialCode);
                                if (allTasksForMaterial != null) {
                                    for (CoreScheduleAlgorithmService.DailyEmbryoTask prevTask : allTasksForMaterial) {
                                        if (prevTask != minTask && !Boolean.TRUE.equals(prevTask.getIsLastEndingBatch())) {
                                            prevTask.setIsLastEndingBatch(true);
                                            log.info("  [R3-回溯] 物料={}, 胎胚={} isLastEndingBatch→true",
                                                    dtMaterialCode, prevTask.getEmbryoCode());
                                        }
                                    }
                                }
                            }

                            if (dtEmbryoCode != null && minTask.getEndingExtraInventory() != null
                                    && minTask.getEndingExtraInventory() > 0) {
                                int endingDiff = minTask.getEndingExtraInventory() - minTask.getPlannedProduction();
                                if (endingDiff != 0) {
                                    shiftFormingOutputMap.merge(dtEmbryoCode, endingDiff, Integer::sum);
                                    runningTotalProjectedStock += endingDiff;
                                }
                            }

                            if (minTask.getPlannedProduction() != null && minTask.getPlannedProduction() > 0
                                    && !isTaskAlreadyInResult(minTask, result)
                                    && !r3AddedToResultGlobal.contains(minTask) && !r2AddedToResultGlobal.contains(minTask)) {
                                boolean dtIsC = Boolean.TRUE.equals(minTask.getIsContinueTask());
                                boolean dtIsT = Boolean.TRUE.equals(minTask.getIsTrialTask());
                                if (dtIsC) {
                                    result.getContinueTasks().add(minTask);
                                } else if (dtIsT) {
                                    result.getTrialTasks().add(minTask);
                                } else {
                                    result.getNewTasks().add(minTask);
                                }
                                r3AddedToResultGlobal.add(minTask);
                            }

                            log.info("  [R3-分配完成] 结构={}, 胎胚={}, 物料={}, 第{}轮, 最终计划量={}",
                                    structName, minTask.getEmbryoCode(), dtMaterialCode,
                                    structGlobalRound, minTask.getPlannedProduction());
                            structTasks.remove(minTask);
                        }
                    }
                    // 回写R3耗时到 structureCumulativeTimeMap
                    if (structDeferredTime.compareTo(BigDecimal.ZERO) > 0) {
                        structureCumulativeTimeMap.merge(structName, structDeferredTime, BigDecimal::add);
                        log.info("  结构 {} R3回写耗时={}s({}h), 累计={}s({}h)", structName,
                                structDeferredTime.stripTrailingZeros().toPlainString(),
                                structDeferredTime.divide(BigDecimal.valueOf(SECONDS_PER_HOUR), 1, BigDecimal.ROUND_HALF_UP),
                                structureCumulativeTimeMap.get(structName).stripTrailingZeros().toPlainString(),
                                structureCumulativeTimeMap.get(structName).divide(BigDecimal.valueOf(SECONDS_PER_HOUR), 1, BigDecimal.ROUND_HALF_UP));
                    }
                }

                // R3未完成但已分配的任务：收尾处理 + 加入结果列表
                for (List<CoreScheduleAlgorithmService.DailyEmbryoTask> remaining : r3StructureMap.values()) {
                    for (CoreScheduleAlgorithmService.DailyEmbryoTask rt : remaining) {
                        if (rt.getPlannedProduction() != null && rt.getPlannedProduction() > 0
                                && !isTaskAlreadyInResult(rt, result)
                                && !r3AddedToResultGlobal.contains(rt) && !r2AddedToResultGlobal.contains(rt)) {
                            rt.setEndingExtraInventory(rt.getPlannedProduction());
                            handleEndingRemainder(rt, context);
                            String rtEmbryoCode = rt.getEmbryoCode();
                            if (rtEmbryoCode != null && rt.getEndingExtraInventory() != null
                                    && rt.getEndingExtraInventory() > 0) {
                                int endingDiff = rt.getEndingExtraInventory() - rt.getPlannedProduction();
                                if (endingDiff != 0) {
                                    shiftFormingOutputMap.merge(rtEmbryoCode, endingDiff, Integer::sum);
                                    runningTotalProjectedStock += endingDiff;
                                }
                            }
                            // 回溯更新 isLastEndingBatch（与R1/R2保持一致）
                            if (Boolean.TRUE.equals(rt.getIsLastEndingBatch())) {
                                String rtMaterialCode = rt.getMaterialCode();
                                List<CoreScheduleAlgorithmService.DailyEmbryoTask> allTasksForMaterial = materialTasksMap.get(rtMaterialCode);
                                if (allTasksForMaterial != null) {
                                    for (CoreScheduleAlgorithmService.DailyEmbryoTask prevTask : allTasksForMaterial) {
                                        if (prevTask != rt && !Boolean.TRUE.equals(prevTask.getIsLastEndingBatch())) {
                                            prevTask.setIsLastEndingBatch(true);
                                            log.info("  第三轮回溯更新 isLastEndingBatch(未完成): 物料={}, 胎胚={} → true",
                                                    rtMaterialCode, prevTask.getEmbryoCode());
                                        }
                                    }
                                }
                            }
                            boolean rtIsC = Boolean.TRUE.equals(rt.getIsContinueTask());
                            boolean rtIsT = Boolean.TRUE.equals(rt.getIsTrialTask());
                            if (rtIsC) {
                                result.getContinueTasks().add(rt);
                            } else if (rtIsT) {
                                result.getTrialTasks().add(rt);
                            } else {
                                result.getNewTasks().add(rt);
                            }
                            r3AddedToResultGlobal.add(rt);
                            log.info("  [R3-未完成但已分配] 胎胚={}, 最终计划量={}, 剩余需求={}",
                                    rt.getEmbryoCode(), rt.getPlannedProduction(), rt.getDeferredRemainingDemand());
                        }
                    }
                }

                log.info("【第三轮分配结果（R3）】总数:{}个 | 已分配:{}个 | 跳过:产能不足{}个/成型余量耗尽{}个/立库满{}个",
                        r2ExitedTasks.size(), r3Allocated,
                        r3SkippedCapacity, r3SkippedForming, r3SkippedWarehouse);
                if (!r3SkippedCapacityList.isEmpty()) {
                    log.info("  [R3-产能不足明细] {}", String.join(", ", r3SkippedCapacityList));
                }
                if (!r3SkippedFormingList.isEmpty()) {
                    log.info("  [R3-成型余量耗尽明细] {}", String.join(", ", r3SkippedFormingList));
                }
                if (!r3SkippedWarehouseList.isEmpty()) {
                    log.info("  [R3-立库满明细] {}", String.join(", ", r3SkippedWarehouseList));
                }
            }

            // 结构完成后：计算机台占用 + 判定收尾
            updateMachineOccupationAndEndingStatus(currentStructure, context,
                    structureRecommendedMachinesCache, structureCumulativeTimeMap,
                    machineOccupiedTimeMap, structureFullyEndedMap, materialTasksMap);
        } // 结束结构 for 循环

        // 更新跨班次切换状态到 context
        context.setMachineSwitchRemainingMap(machineSwitchRemainingMap);

        log.info("【任务分组结果】续作:{}个 | 试制:{}个 | 新增:{}个 | 跳过:空胎胚{}个/空任务{}个/硫化余量{}个/成型余量{}个/产能超限{}个/立库满{}个",
                result.getContinueTasks().size(),
                result.getTrialTasks().size(),
                result.getNewTasks().size(),
                skippedNullEmbryo, skippedNullTask, skippedVulcanizeSurplusZero, skippedFormingRemainderZero,
                skippedCapacityExceeded, skippedWarehouseFull);
        return result;
    }

    /**
     * 判断任务是否已在分组结果列表中（防止R2/R3重复添加第一轮已执行任务）
     */
    private boolean isTaskAlreadyInResult(CoreScheduleAlgorithmService.DailyEmbryoTask task, TaskGroupResult result) {
        return result.getContinueTasks().contains(task)
                || result.getTrialTasks().contains(task)
                || result.getNewTasks().contains(task);
    }

    /**
     * S5.2.4 计算收尾属性
     *
     * <p>包括：成型余量、是否收尾任务、是否10天内收尾、是否3天内收尾（紧急）、收尾日
     *
     * @param task           胎胚任务
     * @param context        排程上下文
     * @param scheduleDate   排程日期
     * @param usedRemainder  该物料已使用的成型余量（前面任务已排产的数量）
     */
    public void calculateEndingInfo(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextVo context,
            LocalDate scheduleDate,
            int usedRemainder) {

        String materialCode = task.getMaterialCode();

        // 获取成型余量（从预计算的映射中获取）
        Map<String, Integer> formingRemainderMap = context.getFormingRemainderMap();
        Integer totalFormingRemainder = null;
        Integer vulcanizeSurplusQty = null;

        // 从月计划余量获取硫化余量
        if (context.getMonthSurplusMap() != null) {
            MdmMonthSurplus monthSurplus = context.getMonthSurplusMap().get(materialCode);
            if (monthSurplus != null && monthSurplus.getPlanSurplusQty() != null) {
                vulcanizeSurplusQty = monthSurplus.getPlanSurplusQty().intValue();
            }
        }

        // 获取总成型余量
        if (formingRemainderMap != null && formingRemainderMap.containsKey(materialCode)) {
            totalFormingRemainder = formingRemainderMap.get(materialCode);
        }

        // 计算当前任务的剩余成型余量 = 总成型余量 - 已使用成型余量
        Integer remainingFormingRemainder = null;
        if (totalFormingRemainder != null) {
            remainingFormingRemainder = Math.max(0, totalFormingRemainder - usedRemainder);
            log.info("物料 {} 总成型余量={}, 已使用={}, 剩余={}",
                    materialCode, totalFormingRemainder, usedRemainder, remainingFormingRemainder);
        }

        task.setVulcanizeSurplusQty(vulcanizeSurplusQty);
        task.setEndingSurplusQty(remainingFormingRemainder);  // 使用剩余成型余量

        // 判断是否收尾任务（剩余成型余量 <= 0）
        boolean isEndingTask = remainingFormingRemainder != null && remainingFormingRemainder <= 0;
        task.setIsEndingTask(isEndingTask);

        // 获取收尾日（从物料收尾管理表，该表以物料编码为键）
        LocalDate endingDate = findEndingDate(task.getMaterialCode(), context);
        task.setEndingDate(endingDate);

        if (endingDate != null) {
            int daysToEnding = (int) java.time.temporal.ChronoUnit.DAYS.between(scheduleDate, endingDate);
            task.setDaysToEnding(daysToEnding);

            // 判断是否10天内收尾
            boolean isNearEnding = daysToEnding >= 0 && daysToEnding <= getEndingDaysThreshold(context);
            task.setIsNearEnding(isNearEnding);

            // 判断是否3天内收尾（紧急），或成型余量>=400（库存积压风险）
            boolean isUrgentEnding = (daysToEnding >= 0 && daysToEnding <= getUrgentEndingDays(context))
                    || (remainingFormingRemainder != null && remainingFormingRemainder <= getEndingUrgentFormingRemainder(context));
            task.setIsUrgentEnding(isUrgentEnding);

        } else if (isEndingTask) {
            // 没有收尾日记录但已判定为收尾任务（成型余量<=0），仍需标记为紧急收尾和临近收尾
            task.setIsNearEnding(true);
            task.setIsUrgentEnding(true);
        }

        // 计算优先级
        task.setPriority(calculateTaskPriority(task, context));
    }

    /**
     * 计算任务优先级分数
     *
     * <p>优先级分层规则（从高到低）：
     * <ol>
     *   <li>有计划量 + 3天内紧急收尾</li>
     *   <li>有计划量 + 10天内收尾</li>
     *   <li>有计划量 + 大于10天收尾</li>
     *   <li>无计划量 + 3天内紧急收尾</li>
     *   <li>无计划量 + 10天内收尾</li>
     *   <li>无计划量 + 大于10天收尾</li>
     * </ol>
     *
     * <p>每个层级内部细分排序（试制量试 > 续作 > 非续作 > 库存少的优先），以及叠加细化加分：
     * <ul>
     *   <li>试制量试任务: +1500（层级内最高）</li>
     *   <li>续作任务: +800</li>
     *   <li>库存量扣分: -min(currentStock, 499)（库存越少优先级越高）</li>
     * </ul>
     *
     * @param task    胎胚任务
     * @param context 排程上下文
     * @return 优先级分数（越高越优先）
     */
    public int calculateTaskPriority(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextVo context) {

        int score = 0;

        boolean hasPlanQty = task.getVulcanizeDemand() != null && task.getVulcanizeDemand() > 0;
        boolean isUrgentEnding = Boolean.TRUE.equals(task.getIsUrgentEnding());
        boolean isNearEnding = Boolean.TRUE.equals(task.getIsNearEnding());

        if (hasPlanQty && isUrgentEnding) {
            score += PRIORITY_HAS_PLAN_URGENT;
        } else if (hasPlanQty && isNearEnding) {
            score += PRIORITY_HAS_PLAN_NEAR;
        } else if (hasPlanQty) {
            score += PRIORITY_HAS_PLAN_NORMAL;
        } else if (isUrgentEnding) {
            score += PRIORITY_NO_PLAN_URGENT;
        } else if (isNearEnding) {
            score += PRIORITY_NO_PLAN_NEAR;
        } else {
            score += PRIORITY_NO_PLAN_NORMAL;
        }

        if (Boolean.TRUE.equals(task.getIsTrialTask()) || Boolean.TRUE.equals(task.getIsProductionTrial())) {
            score += PRIORITY_TRIAL;
        }

        if (Boolean.TRUE.equals(task.getIsContinueTask())) {
            score += PRIORITY_CONTINUE;
        }

        // 第三层细分：按库存量排序，库存少的优先
        int stockQty = task.getCurrentStock() != null ? task.getCurrentStock() : 0;
        score -= Math.min(stockQty, PRIORITY_STOCK_TIEBREAKER_MAX);

        return score;
    }

    // ==================== 私有方法 ====================

    /**
     * 获取成型余量
     *
     * <p>从 context 的 formingRemainderMap 中获取（key 是物料编码），如果没有则根据硫化余量和库存计算。
     *
     * @param materialCode 物料编码
     * @param context      排程上下文
     * @return 成型余量，无法计算时返回 null
     */
    private Integer getFormingRemainder(String materialCode, ScheduleContextVo context) {
        if (materialCode == null) {
            return null;
        }

        // 从context.getFormingRemainderMap映射中获取（key 是物料编码）
        Map<String, Integer> formingRemainderMap = context.getFormingRemainderMap();
        if (formingRemainderMap != null && formingRemainderMap.containsKey(materialCode)) {
            return formingRemainderMap.get(materialCode);
        }

        return 0;
    }

    /**
     * 构建物料映射（双索引：materialCode + embryoCode）
     *
     * @param context 排程上下文
     * @return 物料编码/胎胚编码 → 物料信息
     */
    private Map<String, MdmMaterialInfo> buildMaterialMap(ScheduleContextVo context) {
        Map<String, MdmMaterialInfo> map = new HashMap<>();
        if (context.getMaterials() != null) {
            for (MdmMaterialInfo material : context.getMaterials()) {
                if (material.getMaterialCode() != null) {
                    map.put(material.getMaterialCode(), material);
                }
                if (material.getEmbryoCode() != null) {
                    map.put(material.getEmbryoCode(), material);
                }
            }
        }
        log.debug("物料映射构建完成，共 {} 条物料信息", map.size());
        return map;
    }

    /**
     * 构建库存映射
     *
     * @param context 排程上下文
     * @return 胎胚编码 → 库存信息
     */
    private Map<String, CxStock> buildStockMap(ScheduleContextVo context) {
        Map<String, CxStock> map = new HashMap<>();
        if (context.getStocks() != null) {
            for (CxStock stock : context.getStocks()) {
                map.put(stock.getEmbryoCode(), stock);
            }
        }
        return map;
    }

    /**
     * 查找续作机台
     *
     * <p>使用胎胚编码直接查找：
     * machineOnlineEmbryoMap 存储格式: embryoCode → Set&lt;cxCode&gt;
     *
     * <p>说明：由于操作人员导入数据时，materialCode字段被错误地填入了胎胚编号，
     * 因此我们只使用胎胚编码作为匹配键，避免组合键匹配失败。
     *
     * @param materialCode          成品物料编码（未使用，保留参数兼容性）
     * @param embryoCode            胎胚编码
     * @param machineOnlineEmbryoMap 机台在产映射（embryoCode → Set&lt;cxCode&gt;）
     * @return 续作机台编码列表
     */
    private List<String> findContinueMachines(String materialCode, String embryoCode,
                                              Map<String, Set<String>> machineOnlineEmbryoMap) {
        if (embryoCode == null) {
            return Collections.emptyList();
        }
        Set<String> machines = machineOnlineEmbryoMap.get(embryoCode);
        if (machines == null || machines.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(machines);
    }

    /**
     * 为单条硫化记录构建任务
     *
     * <p>每条硫化记录作为独立任务，不再按胎胚合并。
     * 执行 S5.2.1~S5.2.3 的属性计算。
     *
     * @param lhResult             硫化记录
     * @param materialMap          物料映射
     * @param stockMap             库存映射
     * @param context              排程上下文
     * @param currentShiftConfigs  当前班次配置列表
     * @return 构建好的胎胚任务，无效记录返回 null
     */
    private CoreScheduleAlgorithmService.DailyEmbryoTask buildSingleTask(
            LhScheduleResult lhResult,
            Map<String, MdmMaterialInfo> materialMap,
            Map<String, CxStock> stockMap,
            ScheduleContextVo context,
            List<CxShiftConfig> currentShiftConfigs) {

        String embryoCode = lhResult.getEmbryoCode();
        String materialCode = lhResult.getMaterialCode();
        if (embryoCode == null) {
            log.warn("buildSingleTask跳过：embryoCode为null，materialCode={}", materialCode);
            return null;
        }

        int vulcanizeDemand = getShiftPlanQty(lhResult, currentShiftConfigs);

        // 获取分配给该硫化任务的库存（按硫化任务维度分配，共用胎胚库存已按比例分配）
        int currentStock = getCurrentStock(context, lhResult.getId());
        log.info("硫化任务排量: embryoCode={}, vulcanizeDemand={}, currentStock={}",
                embryoCode, vulcanizeDemand, currentStock);

        // 获取物料信息
        // 重要：优先使用 lhResult 中的 materialCode，因为同一个 embryoCode 可能对应多个不同的物料
        String materialCodeFromLh = lhResult.getMaterialCode();
        MdmMaterialInfo material = materialMap.get(embryoCode);

        // 如果 lhResult 中有 materialCode，优先使用；否则从 materialMap 中获取
        String finalMaterialCode = materialCodeFromLh;
        String materialDesc = null;
        String mainMaterialDesc = null;
        String structureNameFromMaterial = null;

        if (finalMaterialCode == null && material != null) {
            // lhResult 中没有 materialCode，从 materialMap 中获取
            finalMaterialCode = material.getMaterialCode();
            materialDesc = material.getMaterialDesc();
            mainMaterialDesc = material.getEmbryoDesc();
            structureNameFromMaterial = material.getStructureName();
        } else if (material != null) {
            // lhResult 中有 materialCode，但保留 material 的其他信息（如描述）
            materialDesc = material.getMaterialDesc();
            mainMaterialDesc = material.getEmbryoDesc();
            structureNameFromMaterial = material.getStructureName();
        }

        String structureName = structureNameFromMaterial != null ? structureNameFromMaterial : lhResult.getStructureName();

        // 构建任务
        CoreScheduleAlgorithmService.DailyEmbryoTask task = new CoreScheduleAlgorithmService.DailyEmbryoTask();
        task.setLhId(lhResult.getId());
        task.setEmbryoCode(embryoCode);
        task.setVulcanizeDemand(vulcanizeDemand);
        task.setCurrentStock(currentStock);
        task.setProductionVersion(lhResult.getProductionVersion());
        task.setMaterialCode(finalMaterialCode);

        if (materialDesc != null) {
            task.setMaterialDesc(materialDesc);
        } else {
            task.setMaterialDesc(finalMaterialCode != null ? finalMaterialCode : embryoCode);
        }

        if (mainMaterialDesc != null) {
            task.setMainMaterialDesc(mainMaterialDesc);
        } else {
            task.setMainMaterialDesc(embryoCode);
        }

        task.setStructureName(structureName);

        task.setDemandQuantity(vulcanizeDemand);
        task.setAssignedQuantity(0);

        // 是否主销产品
        String mainProductCode = task.getMaterialCode();
        task.setIsMainProduct(context.getMainProductCodes() != null
                && mainProductCode != null
                && context.getMainProductCodes().contains(mainProductCode));

        // 硫化机台数和模数：一条LhScheduleResult = 一台硫化机
        task.setVulcanizeMachineCount(1);
        task.setVulcanizeMoldCount(lhResult.getMouldQty() != null ? lhResult.getMouldQty() : 1);
        // 硫化机台编号：L+R模的 lhMachineCode 含模号后缀（如 K1502L/K1502R），
        // 去掉末尾 L/R 后缀得到硫化机台号（如 K1502），用于硫化机台数去重
        task.setLhMachineCode(extractLhMachineKey(lhResult.getLhMachineCode(), lhResult.getLeftRightMould()));

        // S5.2.3 计算库存可供硫化时长
        calculateStockHours(task, lhResult, currentStock, context);

        return task;
    }

    /**
     * 从 lhMachineCode + leftRightMould 提取硫化机台号（去掉末尾 L/R 模号后缀）。
     *
     * <p>同一台硫化机的左右模在数据库中各有独立记录：
     * <ul>
     *   <li>L模：lhMachineCode=K1502L, leftRightMould=L → 台号 K1502</li>
     *   <li>R模：lhMachineCode=K1502R, leftRightMould=R → 台号 K1502</li>
     *   <li>双模：lhMachineCode=K1602,  leftRightMould=LR → 台号 K1602</li>
     * </ul>
     * 仅当 leftRightMould 为 L 或 R（单模）时才去后缀，避免误截双模台号。
     *
     * @param lhMachineCode  原始硫化机台编号（可能含 L/R 后缀）
     * @param leftRightMould 左右模标识（L/R/LR）
     * @return 硫化机台号（去后缀），null/空字符串时返回 null
     */
    private String extractLhMachineKey(String lhMachineCode, String leftRightMould) {
        if (lhMachineCode == null || lhMachineCode.isEmpty()) {
            return null;
        }
        if ("L".equals(leftRightMould) || "R".equals(leftRightMould)) {
            if (lhMachineCode.endsWith("L") || lhMachineCode.endsWith("R")) {
                return lhMachineCode.substring(0, lhMachineCode.length() - 1);
            }
        }
        return lhMachineCode;
    }

    /**
     * 根据班次配置获取硫化记录对应班次的计划量
     *
     * <p>硫化有8个班次(CLASS1-CLASS8)，成型分3天排程。
     * 根据当前班次配置的 classField 字段获取对应的硫化班次计划量。
     *
     * @param lhResult            硫化记录
     * @param currentShiftConfigs 当前班次配置列表
     * @return 对应班次的硫化计划量
     */
    private int getShiftPlanQty(LhScheduleResult lhResult, List<CxShiftConfig> currentShiftConfigs) {
        if (currentShiftConfigs == null || currentShiftConfigs.isEmpty()) {
            return 0;
        }

        for (CxShiftConfig shiftConfig : currentShiftConfigs) {
            String classField = shiftConfig.getClassField();
            if (classField != null && classField.startsWith("CLASS")) {
                try {
                    int classIndex = Integer.parseInt(classField.substring(5));
                    Integer planQty = getClassPlanQtyByIndex(lhResult, classIndex);
                    if (planQty != null && planQty > 0) {
                        return planQty;
                    }
                } catch (NumberFormatException e) {
                    log.warn("无法解析班次字段: {}", classField);
                }
            }
        }
        return 0;
    }

    /**
     * 获取当前班次对应的硫化班次索引
     *
     * <p>从 dayShifts 中获取当前班次的 classField，然后提取班次索引。
     * 例如：CLASS1 -> 1, CLASS2 -> 2, CLASS3 -> 3
     *
     * @param dayShifts 当前班次配置列表
     * @return 班次索引 (1-8)，如果没有有效的班次配置则返回 0
     */
    private int getCurrentClassIndex(List<CxShiftConfig> dayShifts) {
        if (dayShifts == null || dayShifts.isEmpty()) {
            return 0;
        }
        // 获取第一个班次的 classField
        CxShiftConfig shiftConfig = dayShifts.get(0);
        String classField = shiftConfig.getClassField();
        if (classField != null && classField.startsWith("CLASS")) {
            try {
                return Integer.parseInt(classField.replace("CLASS", ""));
            } catch (NumberFormatException e) {
                log.warn("无法解析班次字段: {}", classField);
            }
        }
        return 0;
    }

    /**
     * 根据班次索引获取硫化记录的计划量
     *
     * @param lhResult   硫化记录
     * @param classIndex 班次索引 (1-8)
     * @return 计划量
     */
    private Integer getClassPlanQtyByIndex(LhScheduleResult lhResult, int classIndex) {
        switch (classIndex) {
            case 1: return lhResult.getClass1PlanQty();
            case 2: return lhResult.getClass2PlanQty();
            case 3: return lhResult.getClass3PlanQty();
            case 4: return lhResult.getClass4PlanQty();
            case 5: return lhResult.getClass5PlanQty();
            case 6: return lhResult.getClass6PlanQty();
            case 7: return lhResult.getClass7PlanQty();
            case 8: return lhResult.getClass8PlanQty();
            default: return null;
        }
    }

    /**
     * 获取分配给该硫化任务的库存
     *
     * <p>库存已按硫化任务维度分配，共用胎胚库存按硫化任务需求比例分配。
     * 使用硫化任务的唯一标识 (lhId) 获取当前库存。
     *
     * @param context 排程上下文
     * @param lhId    硫化任务ID
     * @return 当前库存数量
     */
    private int getCurrentStock(ScheduleContextVo context, Long lhId) {
        if (lhId == null) {
            return 0;
        }
        Map<String, Integer> materialStockMap = context.getMaterialStockMap();
        if (materialStockMap == null) {
            log.warn("materialStockMap 为空，无法获取分配给硫化任务 {} 的库存", lhId);
            return 0;
        }
        return materialStockMap.getOrDefault(String.valueOf(lhId), 0);
    }

    /**
     * S5.2.3 计算库存可供硫化时长（stockHours）
     *
     * <p>任务分组阶段：成型产出未知（本次排程的结果），无法按班次动态推算，
     * 因此只基于当前库存计算初始的库存可供硫化时长。
     *
     * <p>计算公式：
     * <pre>
     *   日硫化量 → 从 MonthPlanProductLhCapacityVo 获取
     *   单胎单模硫化时长(秒) = 24×60×60 / 日硫化量
     *   stockHours = 库存 × 单胎单模硫化时长 / 任务的模数 / 3600 (转为小时)
     * </pre>
     *
     * @param task         胎胚任务
     * @param lhResult     硫化排程结果
     * @param currentStock 当前胎胚库存
     * @param context      排程上下文
     */
    private void calculateStockHours(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            LhScheduleResult lhResult,
            int currentStock,
            ScheduleContextVo context) {

        // 1. 从 materialLhCapacityMap 获取该物料的日硫化量（key 是 materialCode，不是 embryoCode）
        Map<String, MonthPlanProductLhCapacityVo> lhCapacityMap = context.getMaterialLhCapacityMap();
        Integer dailyLhCapacity = null;
        int originalDoubleMoldDayVulcanizationQty = 0;
        boolean isStandardCapacity = false;
        if (lhCapacityMap != null) {
            String materialCode = task.getMaterialCode();
            MonthPlanProductLhCapacityVo capacityVo = lhCapacityMap.get(materialCode);
            if (capacityVo != null) {
                if (capacityVo.getDayVulcanizationQty() != null && capacityVo.getDayVulcanizationQty() > 0) {
                    originalDoubleMoldDayVulcanizationQty = capacityVo.getDayVulcanizationQty();
                    dailyLhCapacity = capacityVo.getDayVulcanizationQty() / 2; // 日硫化量是双模的，需要除以2得到单模产量
                    log.info("物料 {} dayVulcanizationQty={}, standardCapacity={}, mesCapacity={}, apsCapacity={}",
                            materialCode, capacityVo.getDayVulcanizationQty(), capacityVo.getStandardCapacity(),
                            capacityVo.getMesCapacity(), capacityVo.getApsCapacity());
                } else if (capacityVo.getStandardCapacity() != null && capacityVo.getStandardCapacity() > 0) {
                    isStandardCapacity = true;
                    dailyLhCapacity = capacityVo.getStandardCapacity();
                    log.info("物料 {} dayVulcanizationQty={}, 回退到standardCapacity={}, mesCapacity={}, apsCapacity={}",
                            materialCode, capacityVo.getDayVulcanizationQty(), capacityVo.getStandardCapacity(),
                            capacityVo.getMesCapacity(), capacityVo.getApsCapacity());
                }
            }
        }

        // 仍然取不到，无法计算
        if (dailyLhCapacity == null || dailyLhCapacity <= 0) {
            log.warn("无法获取物料 {} 的日硫化量，stockHours 无法计算", task.getEmbryoCode());
            task.setStockHours(BigDecimal.ZERO);
            task.setIsStockHighWarning(false);
            return;
        }

        // 2. 单胎单模硫化时长(秒) = 24×60×60 / 日硫化量
        BigDecimal singleTireMoldSeconds = productionCalculator.calculateSingleTireMoldSeconds(dailyLhCapacity);

        // 3. 任务的模数
        Integer taskMoldQty = task.getVulcanizeMoldCount();
        if (taskMoldQty == null || taskMoldQty <= 0) {
            taskMoldQty = lhResult != null && lhResult.getMouldQty() != null ? lhResult.getMouldQty() : 1;
        }

        // 4. 基于当前库存计算库存可供硫化时长
        BigDecimal stockHours = productionCalculator.calculateStockHours(currentStock, dailyLhCapacity, taskMoldQty);

        task.setStockHours(stockHours);

        // 库存预警：超过18小时标记为高库存
        boolean isHighStock = stockHours.compareTo(BigDecimal.valueOf(STOCK_HIGH_HOURS_THRESHOLD)) > 0;
        task.setIsStockHighWarning(isHighStock);

        String logDailyLhCapacity;
        if (originalDoubleMoldDayVulcanizationQty > 0) {
            logDailyLhCapacity = "双模=" + originalDoubleMoldDayVulcanizationQty + ", 单模=" + dailyLhCapacity;
        } else {
            logDailyLhCapacity = "标准=" + dailyLhCapacity;
        }
        // 注意：这里的"库存"是分配给本硫化任务的库存（materialStockMap[lhId]），
        //      共用胎胚时按各任务日硫化量比例分配，数值小于胎胚总库存；
        //      立库管控日志中的"胎胚总库存"是 embryoTotalStockMap[embryoCode]，汇总该胎胚所有 CxStock 记录
        log.info("物料 {} stockHours计算: 日硫化量({}), 单胎单模时长={}s, 模数={}, 任务分配库存={}, 库存可供时长={}h",
                task.getEmbryoCode(), logDailyLhCapacity, singleTireMoldSeconds, taskMoldQty, currentStock, stockHours);
    }

    /**
     /**
     * 查找物料收尾日
     *
     * @param embryoCode 胎胚编码
     * @param context    排程上下文
     * @return 收尾日期，无则返回 null
     */
    /**
     * 从物料收尾管理表获取计划收尾日期
     * @param materialCode 物料编码（CxMaterialEnding.materialCode 存的是物料编码）
     */
    private LocalDate findEndingDate(String materialCode, ScheduleContextVo context) {
        if (context.getMaterialEndings() != null) {
            for (CxMaterialEnding ending : context.getMaterialEndings()) {
                if (materialCode.equals(ending.getMaterialCode())) {
                    return ending.getPlannedEndingDate();
                }
            }
        }
        return null;
    }

    /**
     * S5.2.5 计算待排产量
     *
     * <p>与库存对冲后的计划产量：
     * <pre>
     *   plannedProduction = roundToVehicle(max(0, vulcanizeDemand - currentStock) × (1 + lossRate), tripCapacity)
     * </pre>
     *
     * @param task         胎胚任务
     * @param context      排程上下文
     * @param scheduleDate 排程日期
     */
    private void calculatePlannedProduction(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                            ScheduleContextVo context,
                                            LocalDate scheduleDate) {
        // 停产日：当天产量设为0
        if (scheduleDayTypeHelper.isStopDay(scheduleDate, context.getFactoryCode())) {
            task.setPlannedProduction(0);
            task.setRequiredCars(0);
            task.setEndingExtraInventory(0);
            return;
        }

        int vulcanizeDemand = task.getVulcanizeDemand() != null ? task.getVulcanizeDemand() : 0;
        int currentStock = task.getCurrentStock() != null ? task.getCurrentStock() : 0;

        // Step 1: 与库存对冲，计算净需求
        int netDemand = Math.max(0, vulcanizeDemand - currentStock);
        boolean isTrialLikeTask = Boolean.TRUE.equals(task.getIsTrialTask())
                || Boolean.TRUE.equals(task.getIsProductionTrial());

        if (netDemand == 0 && !isTrialLikeTask) {
            task.setPlannedProduction(0);
            task.setRequiredCars(0);
            task.setEndingExtraInventory(0);
            return;
        }

        // Step 2: 乘以(1 + 损耗率)，但试制任务不考虑损耗率
        int requiredProductionValue;
        if (isTrialLikeTask) {
            // 试制量试任务不计算损耗率
            requiredProductionValue = netDemand;
        } else {
            BigDecimal lossRate = context.getLossRate() != null ? context.getLossRate() : BigDecimal.ZERO;
            BigDecimal requiredProduction = new BigDecimal(netDemand)
                    .multiply(BigDecimal.ONE.add(lossRate))
                    .setScale(0, BigDecimal.ROUND_UP);
            requiredProductionValue = requiredProduction.intValue();
        }
        task.setPlannedProduction(requiredProductionValue);

        // Step 3: 整车取整（试制任务不补整车，直接用实际需求量）
        int tripCapacity = getTripCapacity(task.getStructureName(), task.getEmbryoCode(), context);
        int plannedProduction;
        int requiredCars;
        if (isTrialLikeTask) {
            // 试制量试任务：不补整车，使用实际需求量
            // requiredCars：只要需排产就算1车（哪怕不够整车），避免因0车被跳过
            plannedProduction = requiredProductionValue;
            requiredCars = requiredProductionValue > 0 ? 1 : 0;
        } else {
            // 普通任务：整车取整
            plannedProduction = productionCalculator.roundToVehicle(requiredProductionValue, tripCapacity);
            requiredCars = productionCalculator.calculateRequiredCars(plannedProduction, tripCapacity);
        }
        task.setPlannedProduction(plannedProduction);
        task.setRequiredCars(requiredCars);
        task.setEndingExtraInventory(plannedProduction);
    }

    /**
     * 库存已覆盖当前硫化需求时，如果仍有收尾余量，则补一段受限产量。
     *
     * <p>补产量同时满足：
     * 1. 不超过当前任务剩余收尾量
     * 2. 按硫化消耗换算后不超过 6 小时
     *
     * <p>这条分支仍按整车下，向上限内向下取到可下的最大整车量。
     */
    private int calculateEndingFallbackProduction_REMOVED() {
        return 0;
    }

    /**
     * S5.2.6 收尾余量处理
     *
     * <p>近3天收尾的任务，判断是否今天收尾（endingExtraInventory >= endingSurplusQty）：
     * <ul>
     *   <li>非主销 + 余量≤2条 → 舍弃（plannedProduction=0）</li>
     *   <li>非主销 + 余量&gt;2条 → 按实际量下（不补车）</li>
     *   <li>主销产品 → 不够一车则补足到一车</li>
     * </ul>
     *
     * @param task    胎胚任务
     * @param context 排程上下文
     */
    private void handleEndingRemainder(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                       ScheduleContextVo context) {
        // 仅处理近3天收尾的紧急任务
        if (!Boolean.TRUE.equals(task.getIsUrgentEnding())) {
            return;
        }

        Integer endingExtraInventory = task.getEndingExtraInventory();
        Integer endingSurplusQty = task.getEndingSurplusQty();

        if (endingExtraInventory == null || endingExtraInventory <= 0
                || endingSurplusQty == null || endingSurplusQty <= 0) {
            return;
        }

        // 今天是否最后一天收尾：当天计划量（含整车取整）>= 剩余成型量
        boolean isLastDay = endingExtraInventory >= endingSurplusQty;
        if (!isLastDay) {
            return;
        }

        // 今天最后一天收尾
        int tripCapacity = getTripCapacity(task.getStructureName(), task.getEmbryoCode(), context);
        if (!Boolean.TRUE.equals(task.getIsMainProduct()) && endingSurplusQty <= getEndingDiscardThreshold(context)) {
            // 非主销产品 + 收尾余量≤2条，舍弃当天排产
            task.setPlannedProduction(0);
            task.setRequiredCars(0);
            task.setEndingExtraInventory(0);
            task.setEndingAbandoned(true);
            task.setEndingAbandonedQty(endingSurplusQty);
            task.setIsLastEndingBatch(true);
            log.info("收尾任务 {} 余量{}条被舍弃（非主销+余量≤2）", task.getEmbryoCode(), endingSurplusQty);
        } else if (!Boolean.TRUE.equals(task.getIsMainProduct())) {
            // 非主销产品 + 收尾余量>2条，按实际量下（不补车）
            // endingExtraInventory 设置为实际余量（不取整），用于后续均衡分配时扣除
            task.setEndingExtraInventory(endingSurplusQty);

            // requiredCars 按实际余量计算，不足一车的部分也算1车
            task.setRequiredCars(productionCalculator.calculateRequiredCars(endingSurplusQty, tripCapacity));

            // plannedProduction 保持取整后的值（用于显示），但实际生产按 endingExtraInventory
            task.setIsLastEndingBatch(true);
            log.info("收尾任务 {} 今天最后一批（非主销），余量={}，计划={}，实际生产={}",
                    task.getEmbryoCode(), endingSurplusQty, task.getPlannedProduction(), endingSurplusQty);
        } else {
            // 主销产品最后一批：不够一车则补足到一车
            if (endingSurplusQty > 0 && endingSurplusQty < tripCapacity) {
                task.setPlannedProduction(tripCapacity);
                task.setRequiredCars(1);
                task.setEndingExtraInventory(tripCapacity);
                log.info("收尾任务 {} 主销最后一批不足一车，补足到一车：{}", task.getEmbryoCode(), tripCapacity);
            }
            task.setIsLastEndingBatch(true);
            log.info("收尾任务 {} 今天最后一批（主销），余量={}，计划={}",
                    task.getEmbryoCode(), endingSurplusQty, task.getPlannedProduction());
        }
    }

    /**
     * S5.2.7 开停产特殊处理
     *
     * <p>每个班次处理时调用，顺序判断：
     * <ol>
     *   <li>已停产日（isStopDay）→ 产量=0</li>
     *   <li>当前班次停产/停产前一个班次/停产标识日 → handleClosingDayTaskV2（反推封顶）</li>
     *   <li>开产班次（OPEN_START）→ handleOpeningDayTaskV2（6/24备货）</li>
     *   <li>明天有停产 → 跨天封顶</li>
     * </ol>
     *
     * @param task      胎胚任务
     * @param context   排程上下文
     * @param dayShifts 当前班次配置
     */
    private void handleOpeningClosingDay(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                         ScheduleContextVo context,
                                         List<CxShiftConfig> dayShifts) {
        LocalDate scheduleDate = context.getCurrentScheduleDate();
        String factoryCode = context.getFactoryCode();

        // 获取当前班次信息
        CxShiftConfig currentShift = dayShifts != null && dayShifts.size() == 1 ? dayShifts.get(0) : null;
        int currentDayShiftOrder = currentShift != null && currentShift.getDayShiftOrder() != null
                ? currentShift.getDayShiftOrder() : 0;

        // ==================== 停产日（已停产）：当天产量设为0 ====================
        if (scheduleDayTypeHelper.isStopDay(scheduleDate, factoryCode)) {
            task.setPlannedProduction(0);
            task.setRequiredCars(0);
            task.setEndingExtraInventory(0);
            return;
        }

        // ==================== 停产逻辑调整（v2）====================
        // 每个班次都检查：今天有没有包含停产班次
        // 通过一次 determineShiftType 获取班次类型，避免重复调用
        ScheduleDayTypeHelper.ShiftType shiftType = scheduleDayTypeHelper.determineShiftType(
                scheduleDate, currentDayShiftOrder, factoryCode);
        boolean isCurrentClosingShift = shiftType == ScheduleDayTypeHelper.ShiftType.CLOSED;
        boolean isBeforeClosingShift = shiftType == ScheduleDayTypeHelper.ShiftType.BEFORE_CLOSE;
        // 判断条件3：当前班次本身是否是停产标识日的班次（包含停产班次的当天）
        boolean isStopFlagDayToday = scheduleDayTypeHelper.isStopFlagDay(scheduleDate, factoryCode);

        if (isCurrentClosingShift || isBeforeClosingShift || isStopFlagDayToday) {
            log.info("当前班次事件: 工厂={}, 日期={}, 当天第{}班, 类型={}, 停产标识日={}",
                    factoryCode, scheduleDate, currentDayShiftOrder,
                    isCurrentClosingShift ? "停产班"
                            : isBeforeClosingShift ? "停产前一个班次(下个班次停产)"
                            : "停产标识日",
                    isStopFlagDayToday);
            // 今天包含停产班次，走停产逻辑
            handleClosingDayTaskV2(task, context, scheduleDate, currentDayShiftOrder, dayShifts);
            return;
        }

        // ==================== 停产日前一天封顶 ====================
        // 如果明天有停产班次，需跨天封顶当前班次的产量
        // 避免前一个班次过量生产，导致停产后库存过剩（此检查必须在开产日前，避免被截断）
        LocalDate nextDay = scheduleDate.plusDays(1);
        boolean isNextDayStop = scheduleDayTypeHelper.hasAnyClosingShift(nextDay, factoryCode);

        // ==================== 开产处理（仅 OPEN_START 班次）====================
        boolean isOpening = shiftType == ScheduleDayTypeHelper.ShiftType.OPEN_START;
        if (isOpening) {
            handleOpeningDayTaskV2(task, context, scheduleDate, currentDayShiftOrder, dayShifts);
            if (isNextDayStop) {
                int closingRequiredStock = calculateClosingRequiredStockV2(task, context, scheduleDate, currentDayShiftOrder, dayShifts);
                int currentStock = task.getCurrentStock() != null ? task.getCurrentStock() : 0;
                int thisShiftNeeded = Math.max(0, closingRequiredStock - currentStock);
                if (thisShiftNeeded <= 0) {
                    task.setIsEndProduction(true);
                }
                int normalDemand = task.getPlannedProduction() != null ? task.getPlannedProduction() : 0;
                int cappedProduction = Math.min(normalDemand, thisShiftNeeded);
                log.info("跨天封顶(明天{}有停产,开产日): 胎胚={}, 反推需求={}, 库存={}, 还需={}, 正常需求={}, 封顶={}",
                        nextDay, task.getEmbryoCode(), closingRequiredStock, currentStock,
                        thisShiftNeeded, normalDemand, cappedProduction);
                int tripCapacity = getTripCapacity(task.getStructureName(), task.getEmbryoCode(), context);
                int roundedProduction = productionCalculator.roundToVehicle(cappedProduction, tripCapacity);
                task.setPlannedProduction(roundedProduction);
                task.setEndingExtraInventory(roundedProduction);
                task.setRequiredCars(productionCalculator.calculateRequiredCars(roundedProduction, tripCapacity));
            }
            return;
        }

        if (isNextDayStop) {
            int closingRequiredStock = calculateClosingRequiredStockV2(task, context, scheduleDate, currentDayShiftOrder, dayShifts);
            int currentStock = task.getCurrentStock() != null ? task.getCurrentStock() : 0;
            int thisShiftNeeded = Math.max(0, closingRequiredStock - currentStock);
            if (thisShiftNeeded <= 0) {
                task.setIsEndProduction(true);
            }
            int normalDemand = task.getPlannedProduction() != null ? task.getPlannedProduction() : 0;
            int cappedProduction = Math.min(normalDemand, thisShiftNeeded);
            log.info("跨天封顶(明天{}有停产): 胎胚={}, 反推需求={}, 库存={}, 还需={}, 正常需求={}, 封顶={}",
                    nextDay, task.getEmbryoCode(), closingRequiredStock, currentStock,
                    thisShiftNeeded, normalDemand, cappedProduction);
            int tripCapacity = getTripCapacity(task.getStructureName(), task.getEmbryoCode(), context);
            int roundedProduction = productionCalculator.roundToVehicle(cappedProduction, tripCapacity);
            task.setPlannedProduction(roundedProduction);
            task.setEndingExtraInventory(roundedProduction);
            task.setRequiredCars(productionCalculator.calculateRequiredCars(roundedProduction, tripCapacity));
        }
    }

    /**
     * 停产标识日任务处理：反推封顶
     *
     * <p>核心逻辑：
     * <ol>
     *   <li>根据硫化机停锅时间和班次配置，确定停锅班次 closingShiftOrder</li>
     *   <li>计算成型停机时间 = 硫化停锅时间 - 预留消化时间</li>
     *   <li>反推总量 = 从成型停机到硫化停锅期间硫化消耗的胎胚数</li>
     *   <li>当前班次需生产量 = min(normalDemand, max(0, 反推总量 - currentStock))</li>
     *   <li>如果当前班次 = 停锅班次，不补整车</li>
     * </ol>
     *
     * @param task               胎胚任务
     * @param context            排程上下文
     * @param scheduleDate       排程日期
     * @param currentDayShiftOrder 当前班次序号
     */
    /**
     * 停产任务处理：每个班次检查是否包含停产班次，按反推公式计算
     *
     * <p>调整逻辑：
     * <ul>
     *   <li>每个班次进来都要检查今天有没有包含停产班次</li>
     *   <li>如果包含停产班次，依据硫化停锅时间倒推当前班次到停产班次还需生成的量</li>
     *   <li>反推公式：反推总量 = (停锅时间 - 当前班次开始时间 - 预留消化时间) / 单胎单模时长 × 模数</li>
     *   <li>封顶：取收尾后实需(endingExtraInventory)和反推需求中的较小值</li>
     *   <li>如果任务之前走了收尾余量处理，以停产为优先调整回来</li>
     * </ul>
     *
     * @param task               胎胚任务
     * @param context            排程上下文
     * @param scheduleDate       排程日期
     * @param currentDayShiftOrder 当前班次序号
     * @param dayShifts          当前班次配置
     */
    private void handleClosingDayTaskV2(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                        ScheduleContextVo context,
                                        LocalDate scheduleDate,
                                        int currentDayShiftOrder,
                                        List<CxShiftConfig> dayShifts) {
        // 标记为停产日任务
        task.setIsClosingDayTask(true);

        // ==================== 判断当前班次本身是否为停产班次 ====================
        // 如果当前班次本身是停产班次（day_flag="0"），则不生产，产量为0
        boolean isCurrentClosingShift = scheduleDayTypeHelper.isClosingShift(scheduleDate, currentDayShiftOrder, context.getFactoryCode());
        if (isCurrentClosingShift) {
            log.info("当前班次 {} 是停产班次，产量设为0", currentDayShiftOrder);
            task.setPlannedProduction(0);
            task.setRequiredCars(0);
            task.setEndingExtraInventory(0);
            return;
        }

        // 确定停锅班次
        Integer closingShiftOrder = determineClosingShiftOrder(context);
        task.setClosingShiftOrder(closingShiftOrder);

        if (closingShiftOrder == null) {
            log.warn("停产日 {} 无法确定停锅班次，保持原计划量", scheduleDate);
            return;
        }

        // ==================== 计算反推总量（新公式）====================
        // 反推总量 = (停锅时间 - 当前班次开始时间 - 预留消化时间) / 单胎单模时长 × 模数
        int closingRequiredStock = calculateClosingRequiredStockV2(task, context, scheduleDate, currentDayShiftOrder, dayShifts);
        task.setClosingRequiredStock(closingRequiredStock);

        int currentStock = task.getCurrentStock() != null ? task.getCurrentStock() : 0;
        int endingInventory = task.getEndingExtraInventory() != null ? task.getEndingExtraInventory() : 0;

        // 当前班次到停机时间还需的量
        int thisShiftNeeded = Math.max(0, closingRequiredStock - currentStock);

        // 反推需求已满足，标记结束生产
        if (thisShiftNeeded <= 0) {
            task.setIsEndProduction(true);
        }

        // 封顶：取 endingExtraInventory（收尾/试制调整后的量）和反推需求中的较小值
        // 如果 endingExtraInventory < 反推需求，说明收尾已经在限制了，停产不放大
        int cappedProduction = Math.min(endingInventory, thisShiftNeeded);

        log.info("停产反推封顶: 胎胚={}, 停锅班次=当天第{}班, 反推需胎胚={}, 当前库存={}, 收尾后实需={}, 还需生产={}, 封顶={}, 当前班次=当天第{}班",
                task.getEmbryoCode(), closingShiftOrder, closingRequiredStock,
                currentStock, endingInventory, thisShiftNeeded, cappedProduction, currentDayShiftOrder);

        // ==================== 如果之前走了收尾余量处理，以停产为优先调整回来 ====================
        if (Boolean.TRUE.equals(task.getIsLastEndingBatch())) {
            // 收尾任务被停产逻辑覆盖，需要调整回来
            int tripCapacity = getTripCapacity(task.getStructureName(), task.getEmbryoCode(), context);
            log.info("停产优先于收尾：embryoCode={}, 原endingExtraInventory={}, 调整为cappedProduction={}, 原requiredCars={}",
                    task.getEmbryoCode(), task.getEndingExtraInventory(), cappedProduction, task.getRequiredCars());
            task.setEndingExtraInventory(cappedProduction);
            task.setPlannedProduction(cappedProduction);
            task.setRequiredCars(cappedProduction > 0 ? (cappedCapacity(task, cappedProduction, tripCapacity, closingShiftOrder, currentDayShiftOrder)) : 0);
            return;
        }

        // ==================== 正常停产封顶逻辑 ====================
        int tripCapacity = getTripCapacity(task.getStructureName(), task.getEmbryoCode(), context);

        // 如果当前班次是停锅班次，不补整车（按实量下）
        if (currentDayShiftOrder == closingShiftOrder) {
            // 不补整车：用封顶量直接作为 endingExtraInventory
            if (tripCapacity > 0 && cappedProduction > 0 && cappedProduction % tripCapacity != 0) {
                // 向下取整到整车
                int roundedDown = (cappedProduction / tripCapacity) * tripCapacity;
                // 但停产最后班次可以不整车，保持封顶量
                log.info("停锅班次不补整车: embryoCode={}, cappedProduction={}, 向下整车={}, 保持不整车={}",
                        task.getEmbryoCode(), cappedProduction, roundedDown, cappedProduction);
            }
            task.setPlannedProduction(cappedProduction);
            task.setEndingExtraInventory(cappedProduction);
            task.setRequiredCars(cappedProduction > 0 ? 1 : 0);
        } else if (currentDayShiftOrder < closingShiftOrder) {
            // 停锅班次之前的班次：按封顶量正常排产，整车取整
            int roundedProduction = productionCalculator.roundToVehicle(cappedProduction, tripCapacity);
            task.setPlannedProduction(roundedProduction);
            task.setEndingExtraInventory(roundedProduction);
            task.setRequiredCars(productionCalculator.calculateRequiredCars(roundedProduction, tripCapacity));
        }
        // currentDayShiftOrder > closingShiftOrder 不应出现（已被班次停产跳过）
    }

    /**
     * 辅助方法：根据班次与停锅班次的关系计算 requiredCars
     */
    private int cappedCapacity(CoreScheduleAlgorithmService.DailyEmbryoTask task, int cappedProduction,
                               int tripCapacity, int closingShiftOrder, int currentDayShiftOrder) {
        if (currentDayShiftOrder == closingShiftOrder) {
            return cappedProduction > 0 ? 1 : 0; // 停锅班次不补整车
        } else {
            return productionCalculator.calculateRequiredCars(cappedProduction, tripCapacity);
        }
    }

    /**
     * 计算停产反推总量 V2
     *
     * <p>新公式：反推总量 = (停锅时间 - 当前班次开始时间 - 预留消化时间) / 单胎单模时长 × 模数
     *
     * @param task               胎胚任务
     * @param context            排程上下文
     * @param scheduleDate       排程日期
     * @param currentDayShiftOrder 当前班次序号
     * @param dayShifts          当前班次配置
     * @return 反推总量（条数）
     */
    private int calculateClosingRequiredStockV2(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                                ScheduleContextVo context,
                                                LocalDate scheduleDate,
                                                int currentDayShiftOrder,
                                                List<CxShiftConfig> dayShifts) {
        // 从硫化排程结果反推
        LhScheduleResult lhResult = findLhResultByTask(task, context);
        if (lhResult == null) {
            log.warn("停产反推V2：无法找到胎胚 {} 对应的硫化排程结果，使用默认0", task.getEmbryoCode());
            return 0;
        }

        // 计算单胎单模硫化时长(秒)
        int dailyLhCapacity = getDailyLhCapacity(lhResult, context);
        int moldQty = task.getVulcanizeMoldCount();
        int ratio = getStructureLhRatio(task, context);
        if (dailyLhCapacity <= 0 || ratio <= 0) {
            return 0;
        }
        double singleTireMoldSeconds = (double) 24 * 3600 / ((long) ratio * dailyLhCapacity);

        // ==================== 获取停锅时间（优先使用完整日期时间）====================
        LocalDateTime vulcanizingStopDateTime = context.getVulcanizingStopDateTime();
        LocalDateTime stopTime;
        if (vulcanizingStopDateTime != null) {
            stopTime = vulcanizingStopDateTime;
        } else {
            // 回退：用 vulcanizingStopTimeStr(HH:mm) + 排程日期构造
            String vulcanizingStopTimeStr = context.getVulcanizingStopTimeStr();
            if (vulcanizingStopTimeStr == null || vulcanizingStopTimeStr.isEmpty()) {
                log.warn("停产反推V2：未配置硫化停锅时间，无法计算");
                return 0;
            }
            try {
                String timePart = vulcanizingStopTimeStr.length() >= 5
                        ? vulcanizingStopTimeStr.substring(0, 5) : vulcanizingStopTimeStr;
                stopTime = LocalDateTime.of(scheduleDate,
                        java.time.LocalTime.parse(timePart));
            } catch (Exception e) {
                log.warn("停产反推V2：解析停锅时间失败: {}", vulcanizingStopTimeStr);
                return 0;
            }
        }

        // ==================== 获取当前班次开始时间 ====================
        LocalDateTime shiftStartTime = getShiftStartDateTime(scheduleDate, dayShifts);
        if (shiftStartTime == null) {
            log.warn("停产反推V2：无法获取当前班次 {} 的开始时间", currentDayShiftOrder);
            return 0;
        }

        // 预留消化时间
        int reservedDigestHours = context.getReservedDigestHours() != null ? context.getReservedDigestHours() : 1;

        // ==================== 计算反推总量 ====================
        // 反推总量 = (停锅时间 - 当前班次开始时间 - 预留消化时间) / 单胎单模时长 × 模数
        long durationSeconds = java.time.Duration.between(shiftStartTime, stopTime).getSeconds();
        durationSeconds -= (long) reservedDigestHours * 3600;

        if (durationSeconds <= 0) {
            log.info("停产反推V2: embryoCode={}, 停锅时间{}早于当前班次开始时间{}+消化时间{}h，反推总量=0",
                    task.getEmbryoCode(), stopTime, shiftStartTime, reservedDigestHours);
            return 0;
        }

        int requiredStock = (int) ((double) durationSeconds / singleTireMoldSeconds * moldQty);

        log.info("停产反推总量: 胎胚={}, 单模日硫化量={}, 模数={}, 单胎时长={}s, 停锅={}, 当前班次开始={}, 消化={}h, 可用={}s, 需胎胚={}",
                task.getEmbryoCode(), dailyLhCapacity, moldQty,
                String.format("%.1f", singleTireMoldSeconds), stopTime, shiftStartTime, reservedDigestHours,
                durationSeconds, requiredStock);

        return requiredStock;
    }

    /**
     * 获取班次开始时间（LocalDateTime）
     *
     * @param scheduleDate       排程日期
     * @param dayShiftOrder      班次序号
     * @param context            排程上下文
     * @return 班次开始时间
     */
    private LocalDateTime getShiftStartDateTime(LocalDate scheduleDate, List<CxShiftConfig> dayShifts) {
        if (dayShifts == null || dayShifts.isEmpty()) {
            return null;
        }
        CxShiftConfig currentShift = dayShifts.get(0);
        LocalTime startTime = currentShift.getShiftStartTime();
        if (startTime == null) {
            return null;
        }
        // 跨天班次（isCrossDay=1，如 NIGHT_D2 22:00~05:59）实际开始日期在前一天
        LocalDate startDate = scheduleDate;
        if (currentShift.getIsCrossDay() != null && currentShift.getIsCrossDay() == 1) {
            startDate = scheduleDate.minusDays(1);
        }
        return LocalDateTime.of(startDate, startTime);
    }

    /**
     * 开产日任务处理（开产基准量已在 groupTasks 中提前算出并存于 openingShiftCapacity）
     *
     * <p>逻辑：
     * <ol>
     *   <li>取 groupTasks 中预存的 openingShiftCapacity（开产基准量，向下取整到整车）</li>
     *   <li>与收尾/试制调整后的 endingExtraInventory 比较，取较小值</li>
     * </ol>
     *
     * <p>注意：关键产品已在 groupTasks 中提前过滤，不会进入此方法
     *
     * @param task               胎胚任务
     * @param context            排程上下文
     * @param scheduleDate       排程日期
     * @param currentDayShiftOrder 当前班次序号
     * @param dayShifts          当前班次配置
     */
    private void handleOpeningDayTaskV2(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                        ScheduleContextVo context,
                                        LocalDate scheduleDate,
                                        int currentDayShiftOrder,
                                        List<CxShiftConfig> dayShifts) {
        task.setIsOpeningDayTask(true);

        // 收尾/试制已正常算完（vulcanizeDemand 已在循环中被更新为下游 CLASS 计划量）
        // 开产基准(openingShiftCapacity)为 6/24 兜底值，仅在无实需时使用
        int openingBase = task.getOpeningShiftCapacity() != null ? task.getOpeningShiftCapacity() : 0;
        int endingAdjusted = task.getEndingExtraInventory() != null ? task.getEndingExtraInventory() : 0;

        int finalProduction;
        if (endingAdjusted > 0) {
            // 有实需（正常产量或收尾限制），以实需为准，不受开产基准限制
            finalProduction = endingAdjusted;
        } else if (Boolean.TRUE.equals(task.getIsLastEndingBatch())) {
            // 收尾明确舍弃
            finalProduction = 0;
        } else {
            // 无 CLASS 计划量，用开产基准兜底
            finalProduction = openingBase;
        }

        task.setPlannedProduction(finalProduction);
        task.setEndingExtraInventory(finalProduction);

        int tripCapacity = getTripCapacity(task.getStructureName(), task.getEmbryoCode(), context);
        task.setRequiredCars(productionCalculator.calculateRequiredCars(finalProduction, tripCapacity));

        log.info("开产日排产: 胎胚={},  收尾后实需={}, 最终产量={}, 需车={}",
                task.getEmbryoCode(), endingAdjusted, finalProduction,
                task.getRequiredCars());
    }

    /**
     * 确定停锅班次序号
     *
     * <p>根据硫化机停锅时间（参数配置）和班次时间表，确定停锅时间落在哪个班次。
     *
     * @param context 排程上下文
     * @return 停锅班次的dayShiftOrder，找不到返回null
     */
    private Integer determineClosingShiftOrder(ScheduleContextVo context) {
        LocalDateTime stopDateTime = context.getVulcanizingStopDateTime();
        if (stopDateTime == null) {
            // 回退：只有 HH:mm 格式，用旧方法按时分匹配
            String vulcanizingStopTimeStr = context.getVulcanizingStopTimeStr();
            if (vulcanizingStopTimeStr == null || vulcanizingStopTimeStr.isEmpty()) {
                log.warn("未配置硫化机停锅时间(VULCANIZING_STOP_TIME)，无法确定停锅班次");
                return null;
            }
            List<CxShiftConfig> shiftConfigs = getSortedShiftConfigs(context);
            String timePart = extractTimePart(vulcanizingStopTimeStr);
            return scheduleDayTypeHelper.getShiftOrderByTime(timePart, shiftConfigs);
        }

        // 使用完整日期时间匹配：遍历所有班次，结合排程日期算出每个班次的实际起止时间
        return findShiftOrderByDateTime(stopDateTime, context);
    }

    /**
     * 从时间字符串中提取 HH:mm 格式的时间部分
     * 支持格式： "2026-05-19 05:30" -> "05:30" 或 "05:30" -> "05:30"
     */
    private String extractTimePart(String dateTimeStr) {
        if (dateTimeStr == null) {
            return null;
        }
        String trimmed = dateTimeStr.trim();
        // 如果包含空格，取空格后的部分
        if (trimmed.contains(" ")) {
            String[] parts = trimmed.split("\\s+");
            if (parts.length >= 2) {
                trimmed = parts[parts.length - 1]; // 取最后一部分（时间部分）
            }
        }
        // 格式化，确保是 HH:mm 格式（补零）
        try {
            String[] timeParts = trimmed.split(":");
            if (timeParts.length >= 2) {
                String hour = timeParts[0].trim();
                String minute = timeParts[1].trim();
                // 补零
                if (hour.length() == 1) {
                    hour = "0" + hour;
                }
                if (minute.length() == 1) {
                    minute = "0" + minute;
                }
                return hour + ":" + minute;
            }
        } catch (Exception e) {
            log.warn("解析时间字符串失败: {}", dateTimeStr);
        }
        return trimmed;
    }

    /**
     * 根据完整日期时间查找对应的班次序号
     *
     * <p>遍历所有班次配置，结合排程日期算出每个班次的实际起止时间范围，
     * 找到目标时间落在哪个班次内。支持跨天班次。
     *
     * @param dateTime 目标日期时间（如停锅时间 2026-05-19T05:30）
     * @param context  排程上下文
     * @return 班次序号（dayShiftOrder），找不到返回第一个班次序号
     */
    private Integer findShiftOrderByDateTime(LocalDateTime dateTime, ScheduleContextVo context) {
        LocalDate scheduleDate = context.getScheduleDate();
        if (scheduleDate == null) {
            return null;
        }
        // 排程起始日期：前端传入中间天，往前推1天
        LocalDate scheduleStartDate = scheduleDate.minusDays(1);

        List<CxShiftConfig> allShifts = context.getShiftConfigList();
        if (allShifts == null || allShifts.isEmpty()) {
            return null;
        }

        for (CxShiftConfig shift : allShifts) {
            if (shift.getScheduleDay() == null || shift.getDayShiftOrder() == null) continue;

            LocalDate shiftDate = scheduleStartDate.plusDays(shift.getScheduleDay() - 1);
            LocalTime shiftStartTime = shift.getShiftStartTime();
            LocalTime shiftEndTime = shift.getShiftEndTime();

            LocalDateTime shiftStart = LocalDateTime.of(shiftDate, shiftStartTime);
            LocalDateTime shiftEnd = LocalDateTime.of(shiftDate, shiftEndTime);

            // 跨天班次：endTime <= startTime，结束时间加1天
            if (!shiftEnd.isAfter(shiftStart)) {
                shiftEnd = shiftEnd.plusDays(1);
            }

            if (!dateTime.isBefore(shiftStart) && dateTime.isBefore(shiftEnd)) {
                return shift.getDayShiftOrder();
            }
        }

        // 兜底：取第一个班次
        CxShiftConfig firstShift = allShifts.stream()
                .filter(s -> s.getScheduleDay() != null && s.getDayShiftOrder() != null)
                .min(Comparator.comparingInt(CxShiftConfig::getScheduleDay)
                        .thenComparingInt(CxShiftConfig::getDayShiftOrder))
                .orElse(null);
        return firstShift != null ? firstShift.getDayShiftOrder() : null;
    }

    /**
     * 获取按dayShiftOrder排序的班次配置列表（去重，只取第1天的3个班次）
     */
    private List<CxShiftConfig> getSortedShiftConfigs(ScheduleContextVo context) {
        List<CxShiftConfig> allShifts = context.getShiftConfigList();
        if (allShifts == null || allShifts.isEmpty()) {
            return new ArrayList<>();
        }
        // 只取第1天的班次配置（排程天数不同但班次时间相同）
        return allShifts.stream()
                .filter(c -> c.getScheduleDay() != null && c.getScheduleDay() == 1)
                .sorted(java.util.Comparator.comparingInt(c -> c.getDayShiftOrder() != null ? c.getDayShiftOrder() : 0))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 根据任务查找对应的LhScheduleResult
     */
    private LhScheduleResult findLhResultByTask(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                                ScheduleContextVo context) {
        if (task.getLhId() != null) {
            List<LhScheduleResult> lhResults = context.getLhScheduleResults();
            if (lhResults != null) {
                for (LhScheduleResult lh : lhResults) {
                    if (task.getLhId().equals(lh.getId())) {
                        return lh;
                    }
                }
            }
        }
        // 兜底：按embryoCode匹配
        if (task.getEmbryoCode() != null) {
            List<LhScheduleResult> lhResults = context.getLhScheduleResults();
            if (lhResults != null) {
                for (LhScheduleResult lh : lhResults) {
                    if (task.getEmbryoCode().equals(lh.getEmbryoCode())) {
                        return lh;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 获取日硫化量
     */
    private int getDailyLhCapacity(LhScheduleResult lhResult, ScheduleContextVo context) {
        return resolveSingleMoldDailyLhCapacity(lhResult != null ? lhResult.getMaterialCode() : null, context);
    }

    /**
     * 通过任务获取日硫化量
     */
    private int getDailyLhCapacityByTask(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                         ScheduleContextVo context) {
        return resolveSingleMoldDailyLhCapacity(task != null ? task.getMaterialCode() : null, context);
    }

    /**
     * 获取结构硫化配比
     */
    private int getStructureLhRatio(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                    ScheduleContextVo context) {
        if (context.getStructureLhRatioMap() != null && task.getStructureName() != null) {
            com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio lhRatio =
                    context.getStructureLhRatioMap().get(task.getStructureName());
            if (lhRatio != null && lhRatio.getLhMachineMaxQty() != null && lhRatio.getLhMachineMaxQty() > 0) {
                return lhRatio.getLhMachineMaxQty();
            }
        }
        return 1;
    }

    /**
     * 获取结构胎面整车配置（按结构+胎胚匹配）
     *
     * @param structureName 结构名称
     * @param embryoCode    胎胚编码
     * @param context       排程上下文
     * @return 整车条数
     */
    private int getTripCapacity(String structureName, String embryoCode, ScheduleContextVo context) {
        return productionCalculator.getTripCapacity(structureName, embryoCode, context);
    }

    private int calculateQuantityByStockHours(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                              ScheduleContextVo context,
                                              BigDecimal maxHours) {
        int dailyLhCapacity = getDailyLhCapacityByTask(task, context);
        int moldQty = task.getVulcanizeMoldCount() > 0
                ? task.getVulcanizeMoldCount() : 1;
        if (dailyLhCapacity <= 0 || moldQty <= 0 || maxHours == null
                || maxHours.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        return maxHours
                .multiply(BigDecimal.valueOf(dailyLhCapacity))
                .multiply(BigDecimal.valueOf(moldQty))
                .divide(BigDecimal.valueOf(24), 0, BigDecimal.ROUND_DOWN)
                .intValue();
    }

    private MonthPlanProductLhCapacityVo getMaterialLhCapacityVo(String materialCode,
                                                                 ScheduleContextVo context) {
        if (context.getMaterialLhCapacityMap() == null || materialCode == null) {
            return null;
        }
        return context.getMaterialLhCapacityMap().get(materialCode);
    }

    private int resolveSingleMoldDailyLhCapacity(String materialCode, ScheduleContextVo context) {
        MonthPlanProductLhCapacityVo vo = getMaterialLhCapacityVo(materialCode, context);
        if (vo == null) {
            return 0;
        }
        if (vo.getDayVulcanizationQty() != null && vo.getDayVulcanizationQty() > 0) {
            return vo.getDayVulcanizationQty() / 2;
        }
        if (vo.getStandardCapacity() != null && vo.getStandardCapacity() > 0) {
            return vo.getStandardCapacity();
        }
        return 0;
    }

    // ==================== 参数配置获取方法 ====================

    /**
     * 通用：从参数配置表读取整数参数，失败时返回默认值
     *
     * @param context      排程上下文
     * @param paramCode    参数编码
     * @param defaultValue 默认值
     * @return 参数值
     */
    private int getIntParamValue(ScheduleContextVo context, String paramCode, int defaultValue) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get(paramCode);
            if (config != null && config.getParamValue() != null) {
                try {
                    return Integer.parseInt(config.getParamValue());
                } catch (NumberFormatException e) {
                    log.warn("解析参数 {} 配置失败: {}", paramCode, config.getParamValue());
                }
            }
        }
        return defaultValue;
    }

    /**
     * 获取收尾舍弃阈值：非主销产品余量≤此值时舍弃
     * 优先使用参数配置，否则使用默认值
     */
    private int getEndingDiscardThreshold(ScheduleContextVo context) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get(PARAM_ENDING_DISCARD_THRESHOLD);
            if (config != null && config.getParamValue() != null) {
                try {
                    return Integer.parseInt(config.getParamValue());
                } catch (NumberFormatException e) {
                    log.warn("解析收尾舍弃阈值配置失败: {}", config.getParamValue());
                }
            }
        }
        return DEFAULT_ENDING_DISCARD_THRESHOLD;
    }

    /**
     * 获取可供硫化时长封顶阈值（小时）：预计班后库存可供硫化时长超过此值即封顶产量
     * 优先使用参数配置 SYS04080001，否则使用默认值 6
     */
    private int getStockHoursCap(ScheduleContextVo context) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get(PARAM_STOCK_HOURS_CAP);
            if (config != null && config.getParamValue() != null) {
                try {
                    return Integer.parseInt(config.getParamValue());
                } catch (NumberFormatException e) {
                    log.warn("解析可供硫化时长封顶阈值配置失败: {}", config.getParamValue());
                }
            }
        }
        return 6;
    }

    /**
     * 获取可供硫化时长封顶是否开启
     * 优先使用参数配置 SYS04080005，Y=开启，N=关闭，默认开启
     */
    private boolean isStockHoursCapEnabled(ScheduleContextVo context) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get(PARAM_STOCK_HOURS_CAP_ENABLED);
            if (config != null && config.getParamValue() != null) {
                return "Y".equalsIgnoreCase(config.getParamValue().trim());
            }
        }
        return true;
    }

    /**
     * 获取成型余量紧急阈值：成型余量低于此值标记为紧急收尾
     * 优先使用参数配置，否则使用默认值
     */
    private int getEndingUrgentFormingRemainder(ScheduleContextVo context) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get(PARAM_ENDING_URGENT_FORMING_REMAINDER);
            if (config != null && config.getParamValue() != null) {
                try {
                    return Integer.parseInt(config.getParamValue());
                } catch (NumberFormatException e) {
                    log.warn("解析成型余量紧急阈值配置失败: {}", config.getParamValue());
                }
            }
        }
        return DEFAULT_ENDING_URGENT_FORMING_REMAINDER;
    }

    /**
     * 获取近期收尾天数阈值（10天内）
     * 优先使用参数配置，否则使用默认值
     */
    private int getEndingDaysThreshold(ScheduleContextVo context) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get(PARAM_ENDING_DAYS_THRESHOLD);
            if (config != null && config.getParamValue() != null) {
                try {
                    return Integer.parseInt(config.getParamValue());
                } catch (NumberFormatException e) {
                    log.warn("解析近期收尾天数阈值配置失败: {}", config.getParamValue());
                }
            }
        }
        return DEFAULT_ENDING_DAYS_THRESHOLD;
    }

    /**
     * 获取紧急收尾天数阈值（3天内）
     * 优先使用参数配置，否则使用默认值
     */
    private int getUrgentEndingDays(ScheduleContextVo context) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get(PARAM_URGENT_ENDING_DAYS);
            if (config != null && config.getParamValue() != null) {
                try {
                    return Integer.parseInt(config.getParamValue());
                } catch (NumberFormatException e) {
                    log.warn("解析紧急收尾天数阈值配置失败: {}", config.getParamValue());
                }
            }
        }
        return DEFAULT_URGENT_ENDING_DAYS;
    }

    private List<MpCxCapacityConfiguration> getRecommendedMachinesForStructure(
            String structureName, LocalDate scheduleDate, ScheduleContextVo context) {
        Map<String, List<MpCxCapacityConfiguration>> allocationMap = context.getStructureAllocationMap();
        if (allocationMap == null) {
            return Collections.emptyList();
        }
        List<MpCxCapacityConfiguration> configs = allocationMap.get(structureName);
        if (configs == null || configs.isEmpty()) {
            return Collections.emptyList();
        }
        int dayOfMonth = scheduleDate.getDayOfMonth();
        int dateYear = scheduleDate.getYear();
        int dateMonth = scheduleDate.getMonthValue();
        return configs.stream()
                .filter(c -> c.getBeginDay() != null && c.getEndDay() != null
                        && c.getBeginDay() <= dayOfMonth && c.getEndDay() >= dayOfMonth)
                // 年月匹配：确保取到排程日期所在月份的配置
                .filter(c -> c.getYear() != null && c.getYear() == dateYear
                        && c.getMonth() != null && c.getMonth() == dateMonth)
                // 按机台编码去重（同一机台可能有多条配置记录）
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(MpCxCapacityConfiguration::getCxMachineCode, c -> c, (a, b) -> a, LinkedHashMap::new),
                        m -> new ArrayList<>(m.values())));
    }

    /**
     * 基于实际排产结果判定提前生产机台可用性
     *
     * <p>对于每台未来机台，判定其状态：
     * <ul>
     *   <li>OCCUPIED：前结构有机台占用且未全部收尾 → 不可用</li>
     *   <li>FREE：前结构有机台占用且全部收尾 → 可用，需扣除切换耗时</li>
     *   <li>FREE_AVAILABLE：无前结构占用 → 可用，需检查跨班次遗留切换</li>
     * </ul>
     */
    private List<MpCxCapacityConfiguration> resolveAdvanceMachinesByActualStatus(
            String structureName, ScheduleContextVo context,
            Map<String, Set<String>> machineToStructuresMap,
            Map<String, String> machineCurrentEmbryoMap,
            Map<String, Long> machineOccupiedTimeMap,
            Map<String, Boolean> structureFullyEndedMap,
            Set<String> advanceUsedMachineCodes,
            Map<String, MdmMaterialInfo> materialMap,
            List<LhScheduleResult> structResults,
            Map<String, Long> machineSwitchRemainingMap,
            Map<String, List<MpCxCapacityConfiguration>> structureRecommendedMachinesCache,
            Map<String, BigDecimal> structureCumulativeTimeMap,
            Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> materialTasksMap,
            LocalDate scheduleDate,
            Map<String, BigDecimal> structureAdvanceAvailableCapacityMap) {

        // 1. 从未来配置取候选机台（按实际排程日期过滤）
        Map<String, List<MpCxCapacityConfiguration>> futureMap = context.getFutureStructureAllocationMap();
        if (futureMap == null || futureMap.isEmpty()) return Collections.emptyList();
        List<MpCxCapacityConfiguration> allFutureConfigs = futureMap.get(structureName);
        if (allFutureConfigs == null || allFutureConfigs.isEmpty()) return Collections.emptyList();

        // 按实际排程日期过滤未来机台：
        // - 同月：BEGIN_DAY > 当天
        // - 未来月：全部包含
        int dayOfMonth = scheduleDate.getDayOfMonth();
        int dateYearMonth = scheduleDate.getYear() * 100 + scheduleDate.getMonthValue();
        List<MpCxCapacityConfiguration> futureConfigs = allFutureConfigs.stream()
                .filter(c -> {
                    if (c.getBeginDay() == null || c.getYear() == null || c.getMonth() == null) {
                        return false;
                    }
                    int configYearMonth = c.getYear() * 100 + c.getMonth();
                    if (configYearMonth == dateYearMonth) {
                        return c.getBeginDay() > dayOfMonth;
                    }
                    return configYearMonth > dateYearMonth;
                })
                .collect(Collectors.toList());
        if (futureConfigs.isEmpty()) return Collections.emptyList();

        // 2. 获取未来结构任意任务的物料英寸
        String advanceProSize = null;
        for (LhScheduleResult lh : structResults) {
            MdmMaterialInfo mat = materialMap.get(lh.getMaterialCode());
            if (mat != null && mat.getProSize() != null) {
                advanceProSize = mat.getProSize();
                break;
            }
        }

        // 3. 逐台判定
        List<MpCxCapacityConfiguration> available = new ArrayList<>();
        Set<String> excluded = new HashSet<>();
        long totalAvailableSeconds = 0;  // 累计所有可用机台的可用时间
        for (MpCxCapacityConfiguration config : futureConfigs) {
            String machineCode = config.getCxMachineCode();
            if (machineCode == null) continue;

            // 已被其他提前生产结构分配 → 剔除
            if (advanceUsedMachineCodes.contains(machineCode)) {
                excluded.add(machineCode);
                continue;
            }

            // 反查机台当日所属结构
            Set<String> precedingStructures = machineToStructuresMap.get(machineCode);

            if (precedingStructures == null || precedingStructures.isEmpty()) {
                // 无前结构占用 → FREE_AVAILABLE
                long availableSeconds = SECONDS_PER_SHIFT;
                Long remainingSwitch = machineSwitchRemainingMap.get(machineCode);
                if (remainingSwitch != null && remainingSwitch > 0) {
                    availableSeconds -= remainingSwitch;
                    if (availableSeconds > 0) {
                        machineSwitchRemainingMap.remove(machineCode); // 切换完成，清除
                    }
                }
                if (availableSeconds > 0) {
                    available.add(config);
                    totalAvailableSeconds += availableSeconds;
                } else {
                    excluded.add(machineCode);
                    log.info("【提前生产-跨班次切换未完成】机台={}, 遗留切换={}s, 无可用产能", machineCode, remainingSwitch);
                }
            } else {
                // 有前结构占用 → 检查是否全部收尾
                boolean allFullyEnded = true;
                long maxOccupiedTime = 0;
                String precedingProSize = null;
                for (String precedingStruct : precedingStructures) {
                    Boolean fullyEnded = structureFullyEndedMap.get(precedingStruct);
                    if (!Boolean.TRUE.equals(fullyEnded)) {
                        allFullyEnded = false;
                        break;
                    }
                    // 取该前结构在此机台上的占用时间
                    long occupied = machineOccupiedTimeMap.getOrDefault(precedingStruct + "|" + machineCode, 0L);
                    maxOccupiedTime = Math.max(maxOccupiedTime, occupied);

                    // 获取前结构任意任务的物料英寸
                    if (precedingProSize == null) {
                        precedingProSize = getPrecedingStructureProSize(precedingStruct, materialTasksMap, materialMap);
                    }
                }

                if (!allFullyEnded) {
                    // 前结构未全部收尾 → OCCUPIED
                    excluded.add(machineCode);
                    continue;
                }

                // 前结构全部收尾 → FREE，计算切换耗时（从参数配置读取）
                long switchCost;
                if (precedingProSize != null && precedingProSize.equals(advanceProSize)) {
                    int sameInchHours = getIntParamValue(context, PARAM_ADVANCE_SAME_INCH_SWITCH_HOURS, DEFAULT_SAME_INCH_SWITCH_HOURS);
                    switchCost = sameInchHours * SECONDS_PER_HOUR;
                } else {
                    int diffInchHours = getIntParamValue(context, PARAM_ADVANCE_DIFF_INCH_SWITCH_HOURS, DEFAULT_DIFF_INCH_SWITCH_HOURS);
                    switchCost = diffInchHours * SECONDS_PER_HOUR;
                }

                long availableSeconds = SECONDS_PER_SHIFT - maxOccupiedTime - switchCost;

                if (availableSeconds > 0) {
                    available.add(config);
                    totalAvailableSeconds += availableSeconds;
                    log.info("【提前生产-机台可用产能】机台={}, 前结构占用={}s({}h), 切换={}s({}h), 可用={}s({}h)",
                            machineCode, maxOccupiedTime, maxOccupiedTime / SECONDS_PER_HOUR,
                            switchCost, switchCost / SECONDS_PER_HOUR,
                            availableSeconds, availableSeconds / SECONDS_PER_HOUR);
                } else {
                    // 切换无法完成，记录剩余切换耗时
                    long remainingSwitch = -availableSeconds;
                    machineSwitchRemainingMap.put(machineCode, remainingSwitch);
                    excluded.add(machineCode);
                    log.info("【提前生产-跨班次切换】机台={}, 前结构占用={}s, 切换={}s, 剩余={}s, 遗留切换={}s",
                            machineCode, maxOccupiedTime, switchCost, SECONDS_PER_SHIFT - maxOccupiedTime, remainingSwitch);
                }
            }
        }

        // 4. 日志
        if (!excluded.isEmpty()) {
            log.info("【提前生产-冲突检测】结构={}, 未来推荐机台={}, 剔除={}, 剩余可用={}",
                    structureName,
                    futureConfigs.stream().map(MpCxCapacityConfiguration::getCxMachineCode).collect(Collectors.toList()),
                    excluded,
                    available.stream().map(MpCxCapacityConfiguration::getCxMachineCode).collect(Collectors.toList()));
        }

        // 5. 分配成功 → 追加到 advanceUsedMachineCodes + 记录可用产能
        if (!available.isEmpty()) {
            for (MpCxCapacityConfiguration c : available) {
                advanceUsedMachineCodes.add(c.getCxMachineCode());
            }
            // 存入实际可用产能（供R1/R2/R3产能管控使用，替代 机台数×28800）
            structureAdvanceAvailableCapacityMap.put(structureName, BigDecimal.valueOf(totalAvailableSeconds));
            log.info("【提前生产】结构={}, 分配未来机台={}, 总可用产能={}s({}h)",
                    structureName,
                    available.stream().map(MpCxCapacityConfiguration::getCxMachineCode).collect(Collectors.toList()),
                    totalAvailableSeconds, totalAvailableSeconds / SECONDS_PER_HOUR);
        }
        return available;
    }

    /**
     * 获取前结构任意任务的物料英寸
     */
    private String getPrecedingStructureProSize(String precedingStruct,
                                                Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> materialTasksMap,
                                                Map<String, MdmMaterialInfo> materialMap) {
        for (List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks : materialTasksMap.values()) {
            for (CoreScheduleAlgorithmService.DailyEmbryoTask task : tasks) {
                if (precedingStruct.equals(task.getStructureName())) {
                    MdmMaterialInfo mat = materialMap.get(task.getMaterialCode());
                    if (mat != null && mat.getProSize() != null) {
                        return mat.getProSize();
                    }
                }
            }
        }
        return null;
    }

    /**
     * 结构完成后：计算机台占用时间 + 判定是否全部收尾
     */
    private void updateMachineOccupationAndEndingStatus(
            String structureName, ScheduleContextVo context,
            Map<String, List<MpCxCapacityConfiguration>> structureRecommendedMachinesCache,
            Map<String, BigDecimal> structureCumulativeTimeMap,
            Map<String, Long> machineOccupiedTimeMap,
            Map<String, Boolean> structureFullyEndedMap,
            Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> materialTasksMap) {

        List<MpCxCapacityConfiguration> machines = structureRecommendedMachinesCache.get(structureName);
        if (machines == null || machines.isEmpty()) return;

        BigDecimal cumulativeTime = structureCumulativeTimeMap.getOrDefault(structureName, BigDecimal.ZERO);

        // 平均分配到每台机台
        long perMachineTime = machines.size() > 0
                ? cumulativeTime.divide(BigDecimal.valueOf(machines.size()), 0, BigDecimal.ROUND_DOWN).longValue()
                : 0;

        for (MpCxCapacityConfiguration config : machines) {
            String key = structureName + "|" + config.getCxMachineCode();
            machineOccupiedTimeMap.put(key, perMachineTime);
        }

        // 判定是否全部收尾
        boolean fullyEnded = true;
        for (List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks : materialTasksMap.values()) {
            for (CoreScheduleAlgorithmService.DailyEmbryoTask task : tasks) {
                if (structureName.equals(task.getStructureName())) {
                    if (task.getPlannedProduction() != null && task.getPlannedProduction() > 0
                            && !Boolean.TRUE.equals(task.getIsLastEndingBatch())) {
                        fullyEnded = false;
                        break;
                    }
                }
            }
            if (!fullyEnded) break;
        }
        structureFullyEndedMap.put(structureName, fullyEnded);

        log.info("【结构完成】结构={}, 累计耗时={}s, 每台占用={}s, 全部收尾={}",
                structureName, cumulativeTime, perMachineTime, fullyEnded);
    }

    private int calculateStructureTotalMaxLh(
            List<MpCxCapacityConfiguration> machines, String structureName, ScheduleContextVo context) {
        int total = 0;
        for (MpCxCapacityConfiguration config : machines) {
            Integer maxLh = getMachineLhMaxQty(config.getCxMachineCode(), structureName, context);
            total += (maxLh != null ? maxLh : DEFAULT_MAX_LH_MACHINE_QTY);
        }
        return total;
    }

    private BigDecimal calculateStructureAvgRatio(
            List<MpCxCapacityConfiguration> machines, String structureName, ScheduleContextVo context) {
        if (machines.isEmpty()) {
            return BigDecimal.ONE;
        }
        BigDecimal totalRatio = BigDecimal.ZERO;
        for (MpCxCapacityConfiguration config : machines) {
            Integer ratio = getMachineLhMaxQty(config.getCxMachineCode(), structureName, context);
            totalRatio = totalRatio.add(BigDecimal.valueOf(ratio != null && ratio > 0 ? ratio : 1));
        }
        return totalRatio.divide(BigDecimal.valueOf(machines.size()), 4, java.math.RoundingMode.HALF_UP);
    }

    private Integer getMachineLhMaxQty(String machineCode, String structureName, ScheduleContextVo context) {
        if (context.getStructureLhRatioMap() != null && structureName != null && machineCode != null) {
            Map<String, String> machineTypeCodeMap = context.getMachineTypeCodeMap();
            String machineTypeCode = machineTypeCodeMap != null ? machineTypeCodeMap.get(machineCode) : null;
            if (machineTypeCode != null) {
                MdmStructureLhRatio lhRatio = context.getStructureLhRatioMap().get(machineTypeCode + "|" + structureName);
                if (lhRatio != null && lhRatio.getLhMachineMaxQty() != null && lhRatio.getLhMachineMaxQty() > 0) {
                    return lhRatio.getLhMachineMaxQty();
                }
            }
        }
        return null;
    }

    private int getEmbryoWarehouseCapacity(ScheduleContextVo context) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get(PARAM_EMBRYO_WAREHOUSE_CAPACITY);
            if (config != null && config.getParamValue() != null) {
                try {
                    return Integer.parseInt(config.getParamValue());
                } catch (NumberFormatException e) {
                    log.warn("解析成型胎胚立库库容配置失败: {}", config.getParamValue());
                }
            }
        }
        return DEFAULT_EMBRYO_WAREHOUSE_CAPACITY;
    }

    private double getEmbryoWarehouseCapacityRatio(ScheduleContextVo context) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get(PARAM_EMBRYO_WAREHOUSE_CAPACITY_RATIO);
            if (config != null && config.getParamValue() != null) {
                try {
                    return Double.parseDouble(config.getParamValue());
                } catch (NumberFormatException e) {
                    log.warn("解析成型胎胚立库库容比例配置失败: {}", config.getParamValue());
                }
            }
        }
        return DEFAULT_EMBRYO_WAREHOUSE_CAPACITY_RATIO;
    }
}
