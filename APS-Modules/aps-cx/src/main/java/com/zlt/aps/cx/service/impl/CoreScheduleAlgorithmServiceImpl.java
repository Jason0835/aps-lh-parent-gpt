package com.zlt.aps.cx.service.impl;

import com.zlt.aps.cx.api.domain.entity.CxPrecisionPlan;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.mapper.CxPrecisionPlanMapper;
import com.zlt.aps.cx.mapper.LhScheduleResultMapper;
import com.zlt.aps.cx.mapper.MdmSkuConstructionRefMapper;
import com.zlt.aps.cx.service.engine.*;
import com.zlt.aps.cx.vo.MonthPlanProductLhCapacityVo;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.mp.api.domain.entity.MdmMonthSurplus;
import com.zlt.aps.mp.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 核心排程算法服务实现类
 *
 * <p>负责排程主流程编排，具体业务逻辑委托给各专门服务：
 * <ul>
 *   <li>{@link TaskGroupService} - 任务分组与属性计算</li>
 *   <li>{@link ContinueTaskProcessor} - 续作任务处理</li>
 *   <li>{@link TrialTaskProcessor} - 试制任务处理</li>
 *   <li>{@link NewTaskProcessor} - 新增任务处理（含量试约束）</li>
 *   <li>{@link ShiftScheduleService} - 班次精排</li>
 *   <li>{@link BalancingService} - 班次间生产量均衡</li>
 * </ul>
 *
 * <p>排程主流程：
 * <ol>
 *   <li>按天循环排程（共排8个班次，约3天）</li>
 *   <li>每天：任务分组 → 续作处理 → 试制处理 → 新增处理 → 班次精排</li>
 *   <li>每天排完后更新上下文（库存/余量/在机信息）</li>
 *   <li>汇总多天结果，按 机台+胎胚+物料编号 维度生成单表排程数据</li>
 * </ol>
 *
 * @author APS Team
 */
@Slf4j
@Service
public class CoreScheduleAlgorithmServiceImpl implements CoreScheduleAlgorithmService {

    /** taskGroupService 使用 @Lazy 延迟注入，打破循环依赖 */
    @Autowired
    @Lazy
    private TaskGroupService taskGroupService;
    private final ContinueTaskProcessor continueTaskProcessor;
    private final TrialTaskProcessor trialTaskProcessor;
    private final NewTaskProcessor newTaskProcessor;
    private final ShiftScheduleService shiftScheduleService;
    private final ProductionCalculator productionCalculator;
    private final ScheduleDayTypeHelper scheduleDayTypeHelper;
    private final BalancingService balancingService;
    private final CxPrecisionPlanMapper precisionPlanMapper;
    private final LhScheduleResultMapper lhScheduleResultMapper;
    private final MdmSkuConstructionRefMapper skuConstructionRefMapper;

    /** 构造函数注入 */
    @Autowired
    public CoreScheduleAlgorithmServiceImpl(
            @Lazy ContinueTaskProcessor continueTaskProcessor,
            @Lazy TrialTaskProcessor trialTaskProcessor,
            @Lazy NewTaskProcessor newTaskProcessor,
            @Lazy ShiftScheduleService shiftScheduleService,
            @Lazy ProductionCalculator productionCalculator,
            ScheduleDayTypeHelper scheduleDayTypeHelper,
            @Lazy BalancingService balancingService,
            CxPrecisionPlanMapper precisionPlanMapper,
            LhScheduleResultMapper lhScheduleResultMapper,
            MdmSkuConstructionRefMapper skuConstructionRefMapper) {
        this.continueTaskProcessor = continueTaskProcessor;
        this.trialTaskProcessor = trialTaskProcessor;
        this.newTaskProcessor = newTaskProcessor;
        this.shiftScheduleService = shiftScheduleService;
        this.productionCalculator = productionCalculator;
        this.scheduleDayTypeHelper = scheduleDayTypeHelper;
        this.balancingService = balancingService;
        this.precisionPlanMapper = precisionPlanMapper;
        this.lhScheduleResultMapper = lhScheduleResultMapper;
        this.skuConstructionRefMapper = skuConstructionRefMapper;
    }

    /** 默认排程天数 */
    private static final int DEFAULT_SCHEDULE_DAYS = 3;

    /** 一天总秒数 */
    private static final int SECONDS_PER_DAY = 24 * 60 * 60;

    /** 秒转小时的除数 */
    private static final int SECONDS_PER_HOUR = 3600;

    /** 排程起始偏移天数：前端传入中间天，需要往前推1天开始排产 */
    private static final int SCHEDULE_START_OFFSET_DAYS = 1;

    private static final int DEFAULT_MAX_TRIAL_SKU_PER_DAY = 2;

    @Override
    public List<CxScheduleResult> executeSchedule(ScheduleContextVo context) {
        log.info("开始执行排程算法，日期: {}", context.getScheduleDate());

        int maxTrialSkuPerDay = context.getMaxTrialSkuPerDay() != null ? context.getMaxTrialSkuPerDay() : DEFAULT_MAX_TRIAL_SKU_PER_DAY;
        boolean trialAllowedOnSunday = context.getTrialAllowedOnSunday() != null ? context.getTrialAllowedOnSunday() : false;

        // 预加载工作日历缓存，避免后续频繁数据库查询
        LocalDate scheduleDate = context.getScheduleDate();
        String factoryCode = context.getFactoryCode();
        int scheduleDays = context.getScheduleDays() != null ? context.getScheduleDays() : DEFAULT_SCHEDULE_DAYS;
        if (scheduleDate != null) {
            scheduleDayTypeHelper.preloadCache(scheduleDate, scheduleDate.plusDays(scheduleDays - 1), factoryCode);
        }

        // 使用 ScheduleServiceImpl.buildScheduleContext 中已加载的班次配置
        List<CxShiftConfig> allShiftConfigs = context.getShiftConfigList();
        if (allShiftConfigs == null || allShiftConfigs.isEmpty()) {
            log.error("班次配置为空，请先调用 buildScheduleContext 加载班次配置");
            return new ArrayList<>();
        }

        // 按排程天数和班次序号排序，确保按 班次1→班次2→...→班次8 顺序处理
        List<CxShiftConfig> sortedShiftConfigs = allShiftConfigs.stream()
                .filter(c -> c.getScheduleDay() != null)
                .sorted(Comparator.comparingInt(CxShiftConfig::getScheduleDay)
                        .thenComparingInt(c -> c.getDayShiftOrder() != null ? c.getDayShiftOrder() : 0))
                .collect(Collectors.toList());

        // 收集每个班次的排产结果
        List<ShiftScheduleResult> shiftResults = new ArrayList<>();

        // 记录机台在产状态（跨班次持续更新）
        Map<String, Set<String>> machineOnlineEmbryoMap = context.getMachineOnlineEmbryoMap();
        if (machineOnlineEmbryoMap == null) {
            machineOnlineEmbryoMap = new HashMap<>();
        }

        // 已处理的天的集合（用于判断是否需要做停产日检查）
        Set<Integer> processedDays = new HashSet<>();
        // 记录上一个班次的天数，用于判断是否跨天
        int lastDay = 0;

        // 按班次逐个执行排程
        int shiftIndex = 0;
        int totalShifts = sortedShiftConfigs.size();
        for (CxShiftConfig shiftConfig : sortedShiftConfigs) {
            shiftIndex++;
            int day = shiftConfig.getScheduleDay();
            LocalDate currentScheduleDate = context.getScheduleDate()
                    .minusDays(SCHEDULE_START_OFFSET_DAYS).plusDays(day - 1);

            // 检查当前天是否是整天停产
            if (scheduleDayTypeHelper.isFullDayStopped(currentScheduleDate, factoryCode)) {
                log.info("第 {} 天日期 {} 整天停产，跳过第 {} 个班次 {} 的排程", day, currentScheduleDate, shiftIndex, shiftConfig.getShiftCode());
                continue;
            }

            // 检查当前班次是否停产
            Integer dayShiftOrder = shiftConfig.getDayShiftOrder();
            if (dayShiftOrder != null && scheduleDayTypeHelper.isShiftStopped(currentScheduleDate, dayShiftOrder, factoryCode)) {
                log.info("第 {} 天日期 {} 第 {} 个班次 {}(dayShiftOrder={}) 停产，跳过该班次排程",
                        day, currentScheduleDate, shiftIndex, shiftConfig.getShiftCode(), dayShiftOrder);
                continue;
            }

            // 获取历史胎胚数量用于日志
            int historyCount = machineOnlineEmbryoMap != null ? machineOnlineEmbryoMap.size() : 0;
            log.info("【班次开始】#{}/{} | 日期:{} | 班次:{} | 历史胎胚数量:{}",
                    shiftIndex, totalShifts, currentScheduleDate, shiftConfig.getShiftCode(), historyCount);

            // 设置当前班次的上下文
            List<CxShiftConfig> singleShiftList = Collections.singletonList(shiftConfig);
            context.setCurrentScheduleDay(day);
            context.setCurrentScheduleDate(currentScheduleDate);
            context.setCurrentShiftConfigs(singleShiftList);

            // 跨天时重置试制/量试单日SKU上限计数（单日最多2个试制+量试SKU）
            if (day != lastDay) {
                context.setDailyTrialAssignedMaterialCodes(new HashSet<>());
                context.setPrecisionPlanApplied(false);
                context.setSupplementDailyRemainingMap(new HashMap<>());
            }

            // 执行该班次的排程
            ShiftScheduleResult shiftResult = executeShiftSchedule(
                    context, day, shiftConfig, currentScheduleDate, machineOnlineEmbryoMap);
            shiftResults.add(shiftResult);

            // 更新机台在产状态
            machineOnlineEmbryoMap = updateMachineOnlineStatus(
                    shiftResult.getAllAllocations(), machineOnlineEmbryoMap);

            // 将更新后的机台在产状态存回 context，供下一个班次使用
            context.setMachineOnlineEmbryoMap(new HashMap<>(machineOnlineEmbryoMap));

            // 更新库存和硫化余量，供下一个班次排程使用
            updateContextForNextShift(context, shiftResult.getAllAllocations(), singleShiftList, shiftConfig, shiftResult.getShiftProductionResults());

            // 提前检测：剩余成型余量在下一班次会被舍弃（≤2条且非主销），提前在本班次标识
            detectEarlyAbandonment(context, shiftResult);

            lastDay = day;
            processedDays.add(day);
        }

        // ==================== 合并多班次结果：每个机台一条记录，8个班次映射到CLASS1~8 ====================
        List<CxScheduleResult> allResults = buildFinalScheduleResultsFromShifts(context, shiftResults, allShiftConfigs);

        // ==================== 班次量均衡：按结构班产标准，最大硫化机数胎胚调整班次均衡 ====================
        balanceShiftQuantities(context, shiftResults, allShiftConfigs);

        // ==================== 构建子表：按"机台+胎胚+车次"维度，8个班次合并一条，计算库存可供硫化时长和顺位 ====================
        Map<String, List<CxScheduleDetail>> detailGroupMap = buildScheduleDetailsFromShifts(context, shiftResults, allShiftConfigs);
        int totalDetails = detailGroupMap.values().stream().mapToInt(List::size).sum();
        log.info("子表记录构建完成，共 {} 条（按机台+胎胚分组 {} 组）", totalDetails, detailGroupMap.size());

        // ==================== 将子表明细关联到主表（通过机台+胎胚匹配） ====================
        associateDetailsToResults(allResults, detailGroupMap);

        log.info("排程算法执行完成，共 {} 个班次，总机台数: {}", shiftIndex, allResults.size());
        return allResults;
    }

    /**
     * 将子表明细关联到主表结果
     * <p>匹配规则：机台编码 + 胎胚代码 一致
     *
     * @param allResults      主表结果列表
     * @param detailGroupMap  子表分组（key=机台编码|胎胚代码, value=该分组下的子表明细列表）
     */
    private void associateDetailsToResults(List<CxScheduleResult> allResults,
                                           Map<String, List<CxScheduleDetail>> detailGroupMap) {
        if (detailGroupMap.isEmpty()) {
            return;
        }

        int matched = 0;
        for (CxScheduleResult result : allResults) {
            String machineCode = result.getCxMachineCode() != null ? result.getCxMachineCode() : "";
            String embryoCode = result.getEmbryoCode() != null ? result.getEmbryoCode() : "";
            String key = machineCode + "|" + embryoCode;
            List<CxScheduleDetail> details = detailGroupMap.get(key);
            if (details != null) {
                result.setDetails(details);
                matched += details.size();
            }
        }
        int totalDetails = detailGroupMap.values().stream().mapToInt(List::size).sum();
        log.info("子表关联主表完成：子表 {} 条，成功关联 {} 条", totalDetails, matched);
    }

    /**
     * 执行单天排程
     *
     * <p>排程流程：
     * <ol>
     *   <li>S5.2 任务分组：续作/试制/新增三类</li>
     *   <li>S5.3 处理续作任务</li>
     *   <li>S5.3 处理试制任务（独立处理，特殊约束）</li>
     *   <li>S5.3 处理新增任务（合并续作+新增，重新均衡）</li>
     *   <li>S5.3.7 班次排产</li>
     * </ol>
     *
     * @return 班次排产结果列表 + 机台分配结果列表
     */
    private ShiftScheduleResult executeShiftSchedule(
            ScheduleContextVo context,
            int day,
            CxShiftConfig shiftConfig,
            LocalDate scheduleDate,
            Map<String, Set<String>> machineOnlineEmbryoMap) {

        List<CxShiftConfig> singleShiftList = Collections.singletonList(shiftConfig);
        String factoryCode = context.getFactoryCode();

        log.info("========== 开始执行班次排程，天={}, 日期={}, 班次={} ==========",
                day, scheduleDate, shiftConfig.getShiftCode());

        // ==================== 第一步：S5.2 任务分组（单班次） ====================
        TaskGroupService.TaskGroupResult taskGroup = taskGroupService.groupTasks(
                context, machineOnlineEmbryoMap, scheduleDate, singleShiftList);
        log.info("任务分组完成：续作 {} 个，试制 {} 个，新增 {} 个",
                taskGroup.getContinueTasks().size(),
                taskGroup.getTrialTasks().size(),
                taskGroup.getNewTasks().size());

        // ==================== 第一步附加：单日试制/量试SKU上限过滤（单日最多2个） ====================
        applyDailyTrialSkuLimit(context, taskGroup);

        // ==================== 第二步：S5.3 处理续作任务 ====================
        List<MachineAllocationResult> continueAllocations = continueTaskProcessor.processContinueTasks(
                taskGroup.getContinueTasks(), context, scheduleDate, singleShiftList, day);
        log.info("续作任务处理完成，机台分配数: {}", continueAllocations.size());

        // ==================== 第三步：S5.3 处理试制任务（独立处理） ====================
        List<MachineAllocationResult> trialAllocations = trialTaskProcessor.processTrialTasks(
                taskGroup.getTrialTasks(), context, scheduleDate, singleShiftList, context.getAvailableMachines());
        log.info("试制任务处理完成，机台分配数: {}", trialAllocations.size());

        // ==================== 第四步：S5.3 处理新增任务（续作剩余需求+新增统一均衡） ====================
        List<MachineAllocationResult> newAllocations = newTaskProcessor.processNewTasks(
                taskGroup.getNewTasks(),
                context,
                scheduleDate,
                singleShiftList,
                taskGroup.getContinueTasks(),
                continueAllocations,
                trialAllocations);
        log.info("新增任务处理完成，机台分配数: {}", newAllocations.size());

        // ==================== 第五步：合并分配结果 ====================
        List<MachineAllocationResult> allAllocations = new ArrayList<>();
        allAllocations.addAll(continueAllocations);
        allAllocations.addAll(newAllocations);
        allAllocations.addAll(trialAllocations);

        log.info("班次分配前检查: 总分配数={} (续作={}, 新增={}, 试制={})",
                allAllocations.size(), continueAllocations.size(), newAllocations.size(), trialAllocations.size());
        // 检查量试任务重复
        for (MachineAllocationResult mar : allAllocations) {
            for (TaskAllocation ta : mar.getTaskAllocations()) {
                if (Boolean.TRUE.equals(ta.getIsProductionTrial())) {
                    log.info("量试分配检查: 机台={}, 胎胚={}, 物料={}, 数量={}, isContinue={}",
                            mar.getMachineCode(), ta.getEmbryoCode(), ta.getMaterialCode(),
                            ta.getQuantity(), ta.getIsContinueTask());
                }
            }
        }

        // ==================== 精度计划挑选与提前扣量（每日首次执行） ====================
        applyPrecisionPlanSelection(context, scheduleDate, shiftConfig, allAllocations);

        // ==================== 第六步：S5.3.7 班次排产（单个班次，无需跨班次均衡） ====================
        List<ShiftScheduleService.ShiftProductionResult> shiftProductionResults = new ArrayList<>();

        for (MachineAllocationResult allocation : allAllocations) {
            String machineCode = allocation.getMachineCode();
            log.info("========== 对{}机台进行班次排量 ==========", machineCode);
            for (TaskAllocation taskAlloc : allocation.getTaskAllocations()) {
                CoreScheduleAlgorithmService.DailyEmbryoTask task = new CoreScheduleAlgorithmService.DailyEmbryoTask();
                task.setEmbryoCode(taskAlloc.getEmbryoCode());
                task.setMaterialCode(taskAlloc.getMaterialCode());
                task.setMaterialDesc(taskAlloc.getMaterialDesc());
                task.setMainMaterialDesc(taskAlloc.getMainMaterialDesc());
                task.setStructureName(taskAlloc.getStructureName());
                task.setPlannedProduction(taskAlloc.getQuantity());
                // 优先使用 endingExtraInventory（实际需生产量），如果没有则用 quantity
                task.setEndingExtraInventory(taskAlloc.getEndingExtraInventory() != null
                        ? taskAlloc.getEndingExtraInventory() : taskAlloc.getQuantity());
                task.setIsTrialTask(taskAlloc.getIsTrialTask());
                task.setIsProductionTrial(taskAlloc.getIsProductionTrial());
                task.setIsEndingTask(taskAlloc.getIsEndingTask());
                task.setIsContinueTask(taskAlloc.getIsContinueTask());
                task.setIsLastEndingBatch(taskAlloc.getIsLastEndingBatch());  // 设置是否收尾最后一批
                task.setIsEndProduction(taskAlloc.getIsEndProduction());  // 设置是否结束生产
                task.setConstructionStage(taskAlloc.getConstructionStage());  // 设置施工阶段
                task.setEndingAbandoned(taskAlloc.getEndingAbandoned());  // 设置收尾是否被舍弃
                task.setPrecisionDeducted(taskAlloc.getPrecisionDeducted());  // 设置精度扣量标记
                task.setIsFirstTask(taskAlloc.getIsFirstTask());  // 设置是否首任务（新开规格）
                task.setIsUrgentEnding(taskAlloc.getIsUrgentEnding());  // 设置是否紧急收尾
                task.setIsNearEnding(taskAlloc.getIsNearEnding());  // 设置是否临近收尾
                // 优先保留 TaskGroupService 设置的标记，仅 null 时用班次类型兜底
                task.setIsOpeningDayTask(taskAlloc.getIsOpeningDayTask());
                task.setIsClosingDayTask(taskAlloc.getIsClosingDayTask());
                if (task.getIsOpeningDayTask() == null || task.getIsClosingDayTask() == null) {
                    int shiftOrder = shiftConfig.getDayShiftOrder() != null ? shiftConfig.getDayShiftOrder() : 1;
                    ScheduleDayTypeHelper.ShiftType shiftType = scheduleDayTypeHelper.determineShiftType(scheduleDate, shiftOrder, factoryCode);
                    if (task.getIsOpeningDayTask() == null) {
                        task.setIsOpeningDayTask(shiftType == ScheduleDayTypeHelper.ShiftType.OPEN_START);
                    }
                    if (task.getIsClosingDayTask() == null) {
                        task.setIsClosingDayTask(shiftType == ScheduleDayTypeHelper.ShiftType.CLOSED);
                    }
                }
                task.setStockHours(taskAlloc.getStockHours());
                task.setPriority(taskAlloc.getPriority());
                task.setLhId(taskAlloc.getLhId());

                // 计算需要的车数（使用实际待排产量）
                int tripCapacity = productionCalculator.getTripCapacity(taskAlloc.getStructureName(), taskAlloc.getEmbryoCode(), context);
                int actualQty = taskAlloc.getEndingExtraInventory() != null ? taskAlloc.getEndingExtraInventory() : taskAlloc.getQuantity();
                int cars = tripCapacity > 0 ? (int) Math.ceil((double) actualQty / tripCapacity) : 0;
                task.setRequiredCars(cars);

                // 打印精排任务日志
                String taskType;
                if (Boolean.TRUE.equals(taskAlloc.getIsContinueTask())) {
                    taskType = "续作任务";
                } else if (Boolean.TRUE.equals(taskAlloc.getIsTrialTask())) {
                    taskType = "试制任务";
                } else if (Boolean.TRUE.equals(taskAlloc.getIsProductionTrial())) {
                    taskType = "量试任务";
                } else {
                    taskType = "新增任务";
                }
                log.info("  【{}】物料={} | 规格={}",
                        taskType, taskAlloc.getMaterialCode() + "/" + taskAlloc.getMaterialDesc() + "/" + taskAlloc.getEmbryoCode(),
                        taskAlloc.getStructureName());
                log.info("    → 主物料={} | 待排={}条/{}车(每车{}条) | 库存={}h | 硫化机={}台",
                        taskAlloc.getMainMaterialDesc(),
                        actualQty, cars, tripCapacity,
                        String.format("%.1f", taskAlloc.getStockHours()),
                        task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 0);

                List<ShiftScheduleService.ShiftProductionResult> taskShiftResults =
                        shiftScheduleService.scheduleTaskToShifts(task, machineCode, context, singleShiftList, scheduleDate);
                shiftProductionResults.addAll(taskShiftResults);
            }
        }
        log.info("【班次完成】共分配 {} 条排产记录", shiftProductionResults.size());

        // 注意：精度计划扣量和班次量均衡在 executeSchedule 主流程的 L193-197 统一调用
        // 这里每个班次独立排程，不需要额外处理

        // 封装该班次排产结果
        ShiftScheduleResult shiftResult = new ShiftScheduleResult();
        shiftResult.setDay(day);
        shiftResult.setScheduleDate(scheduleDate);
        shiftResult.setShiftConfig(shiftConfig);
        shiftResult.setAllAllocations(allAllocations);
        shiftResult.setShiftProductionResults(shiftProductionResults);

        log.info("========== 班次排程完成，天={}, 班次={} ==========\n", day, shiftConfig.getShiftCode());
        return shiftResult;
    }

    /**
     * 单日试制/量试SKU上限过滤
     *
     * <p>单日最多 maxTrialSkuPerDay 个试制+量试SKU（按胎胚编码计），
     * 跨机台、跨班次统一上限。超过上限的试制/量试任务直接跳过不排产。
     *
     * @param context   排程上下文（含当日已分配SKU集合）
     * @param taskGroup 任务分组结果（直接修改列表）
     */
    private void applyDailyTrialSkuLimit(ScheduleContextVo context, TaskGroupService.TaskGroupResult taskGroup) {
        // ==================== 周日不安排试制/量试 ====================
        LocalDate scheduleDate = context.getCurrentScheduleDate();
        boolean sundayTrialBlocked = scheduleDate != null && scheduleDate.getDayOfWeek() == DayOfWeek.SUNDAY
                && !context.getTrialAllowedOnSunday();
        if (sundayTrialBlocked) {
            int continueTrialCount = (int) taskGroup.getContinueTasks().stream()
                    .filter(t -> Boolean.TRUE.equals(t.getIsTrialTask()) || Boolean.TRUE.equals(t.getIsProductionTrial()))
                    .count();
            int trialCount = taskGroup.getTrialTasks().size();
            int productionTrialCount = (int) taskGroup.getNewTasks().stream()
                    .filter(t -> Boolean.TRUE.equals(t.getIsProductionTrial())).count();
            log.info("周日不安排试制/量试，移除全部试制任务和量试任务: 试制{}个, 量试{}个, 续作试制{}个",
                    trialCount, productionTrialCount, continueTrialCount);
            taskGroup.getTrialTasks().clear();
            taskGroup.getNewTasks().removeIf(t -> Boolean.TRUE.equals(t.getIsProductionTrial()));
            taskGroup.getContinueTasks().removeIf(t ->
                    Boolean.TRUE.equals(t.getIsTrialTask()) || Boolean.TRUE.equals(t.getIsProductionTrial()));
            return;
        }

        int maxTrialSku = context.getMaxTrialSkuPerDay() != null ? context.getMaxTrialSkuPerDay() : DEFAULT_MAX_TRIAL_SKU_PER_DAY;

        Set<String> dailySet = context.getDailyTrialAssignedMaterialCodes();
        if (dailySet == null) {
            dailySet = new HashSet<>();
            context.setDailyTrialAssignedMaterialCodes(dailySet);
        }

        int initialSize = dailySet.size();

        // 过滤试制任务
        List<CoreScheduleAlgorithmService.DailyEmbryoTask> filteredTrialTasks = new ArrayList<>();
        for (CoreScheduleAlgorithmService.DailyEmbryoTask task : taskGroup.getTrialTasks()) {
            String mc = task.getMaterialCode();
            if (dailySet.contains(mc) || dailySet.size() < maxTrialSku) {
                dailySet.add(mc);
                filteredTrialTasks.add(task);
            } else {
                log.warn("试制任务 物料{} 已超过单日上限{}个SKU，跳过", mc, maxTrialSku);
            }
        }
        taskGroup.getTrialTasks().clear();
        taskGroup.getTrialTasks().addAll(filteredTrialTasks);

        // 过滤量试任务（在newTasks中）
        List<CoreScheduleAlgorithmService.DailyEmbryoTask> filteredNewTasks = new ArrayList<>();
        for (CoreScheduleAlgorithmService.DailyEmbryoTask task : taskGroup.getNewTasks()) {
            if (Boolean.TRUE.equals(task.getIsProductionTrial())) {
                String mc = task.getMaterialCode();
                if (dailySet.contains(mc) || dailySet.size() < maxTrialSku) {
                    dailySet.add(mc);
                    filteredNewTasks.add(task);
                } else {
                    log.warn("量试任务 物料{} 已超过单日上限{}个SKU，跳过", mc, maxTrialSku);
                }
            } else {
                filteredNewTasks.add(task);
            }
        }
        taskGroup.getNewTasks().clear();
        taskGroup.getNewTasks().addAll(filteredNewTasks);

        // 过滤续作中的试制/量试任务——续作是前面班次已排产过的，直接放行不占新增SKU名额
        List<CoreScheduleAlgorithmService.DailyEmbryoTask> filteredContinueTasks = new ArrayList<>();
        for (CoreScheduleAlgorithmService.DailyEmbryoTask task : taskGroup.getContinueTasks()) {
            if (Boolean.TRUE.equals(task.getIsTrialTask()) || Boolean.TRUE.equals(task.getIsProductionTrial())) {
                String mc = task.getMaterialCode();
                // 已在dailySet中的（前面班次已排产），直接放行；不在的也不占新名额，直接放行
                dailySet.add(mc);
                filteredContinueTasks.add(task);
                log.debug("续作{}任务 物料{} 直接放行（续作不占新增SKU名额）",
                        Boolean.TRUE.equals(task.getIsProductionTrial()) ? "量试" : "试制", mc);
            } else {
                filteredContinueTasks.add(task);
            }
        }
        taskGroup.getContinueTasks().clear();
        taskGroup.getContinueTasks().addAll(filteredContinueTasks);

        if (dailySet.size() > initialSize) {
            log.info("单日试制/量试SKU上限过滤: 当日已分配 {} / {} 个SKU", dailySet.size(), maxTrialSku);
        }
    }

    /**
     * 精度计划挑选与提前扣量
     *
     * <p>在班次排产前，从当日精度计划中挑选需执行的机台（1~2台），
     * 并对选中机台的任务提前扣减4小时产能。每日仅执行一次。
     *
     * <p>挑选规则：
     * <ol>
     *   <li>优先选计划日期≤当日的紧急计划，最多2台（按planDate升序）</li>
     *   <li>无紧急计划时，选1台未来计划（planDate在(当天, 当天+提前天数]范围内）
     *       中精度扣量对硫化任务影响最小的机台提前执行：
     *       优先选空闲时间多（实际需扣量少）的机台，空闲相同时选可供硫化时长高的</li>
     * </ol>
     *
     * <p>每日截止日期动态计算：当天日期 + precisionAdvanceDays（可配置，默认3天）。
     * 例如排5月18日时只选 planDate≤5月21日的，排5月19日只选 planDate≤5月22日的。
     *
     * <p>扣量规则：选中机台按任务stockHours降序，逐任务扣减4小时产能。
     * 扣量后检查总可用量（库存+剩余产量）是否大于当前班次硫化需求，
     * 若不够则回写 LhScheduleResult 下调对应 CLASS 计划量。
     */
    private void applyPrecisionPlanSelection(
            ScheduleContextVo context,
            LocalDate scheduleDate,
            CxShiftConfig shiftConfig,
            List<MachineAllocationResult> allAllocations) {

        if (context.isPrecisionPlanApplied()) {
            return;
        }

        // 精度计划只能安排在早班（06:00开始），夜班和中班不执行
        boolean isMorningShift = isMorningShift(shiftConfig);
        if (!isMorningShift) {
            log.info("精度计划跳过: 当前班次{}不是早班，精度计划只安排在早班",
                    shiftConfig.getShiftCode());
            return;
        }

        List<CxPrecisionPlan> precisionPlans = context.getPrecisionPlans();
        if (precisionPlans == null || precisionPlans.isEmpty()) {
            context.setPrecisionPlanApplied(true);
            return;
        }

        List<CxPrecisionPlan> uncompleted = precisionPlans.stream()
                .filter(p -> !"1".equals(p.getCompletionStatus()) && p.getPlanDate() != null && p.getScheduleDate() == null)
                .sorted(Comparator.comparing(CxPrecisionPlan::getPlanDate))
                .collect(Collectors.toList());

        if (uncompleted.isEmpty()) {
            context.setPrecisionPlanApplied(true);
            return;
        }

        // 将planDate转换为LocalDate做日期级比较，避免Date/Timestamp类型不一致导致after()判断错误
        java.time.ZoneId zoneId = java.time.ZoneId.systemDefault();
        List<CxPrecisionPlan> urgentPlans = uncompleted.stream()
                .filter(p -> {
                    LocalDate planLocalDate = p.getPlanDate().toInstant().atZone(zoneId).toLocalDate();
                    return !planLocalDate.isAfter(scheduleDate);
                })
                .collect(Collectors.toList());

        LocalDate dayCutoffDate = scheduleDate.plusDays(context.getPrecisionAdvanceDays());
        List<CxPrecisionPlan> futurePlans = uncompleted.stream()
                .filter(p -> {
                    LocalDate planLocalDate = p.getPlanDate().toInstant().atZone(zoneId).toLocalDate();
                    return planLocalDate.isAfter(scheduleDate) && !planLocalDate.isAfter(dayCutoffDate);
                })
                .collect(Collectors.toList());

        log.info("精度计划筛选: 当天={}, 截止={}, 紧急{}条, 未来{}条",
                scheduleDate, dayCutoffDate, urgentPlans.size(), futurePlans.size());

        List<CxPrecisionPlan> selectedPlans = new ArrayList<>();

        if (!urgentPlans.isEmpty()) {
            selectedPlans = urgentPlans.stream()
                    .limit(2)
                    .collect(Collectors.toList());
            log.info("精度计划挑选: 紧急计划, 选中 {} 台机台: {}",
                    selectedPlans.size(),
                    selectedPlans.stream().map(CxPrecisionPlan::getMachineCode).collect(Collectors.toList()));
        } else if (!futurePlans.isEmpty()) {
            Map<String, BigDecimal> machineTotalStockHours = new HashMap<>();
            Map<String, Double> machineTaskHours = new HashMap<>();
            int shiftHours = shiftConfig.getShiftHours() != null && shiftConfig.getShiftHours() > 0
                    ? shiftConfig.getShiftHours() : 8;

            for (MachineAllocationResult allocation : allAllocations) {
                BigDecimal total = BigDecimal.ZERO;
                double taskHours = 0;
                if (allocation.getTaskAllocations() != null) {
                    for (TaskAllocation ta : allocation.getTaskAllocations()) {
                        if (ta.getStockHours() != null) {
                            total = total.add(ta.getStockHours());
                        }
                        int hourlyCapacity = shiftScheduleService.getMachineHourlyCapacity(
                                allocation.getMachineCode(), ta.getMaterialCode(), ta.getStructureName(), context);
                        if (hourlyCapacity > 0) {
                            int qty = ta.getEndingExtraInventory() != null
                                    ? ta.getEndingExtraInventory()
                                    : (ta.getQuantity() != null ? ta.getQuantity() : 0);
                            taskHours += (double) qty / hourlyCapacity;
                        }
                    }
                }
                machineTotalStockHours.merge(allocation.getMachineCode(), total, BigDecimal::add);
                machineTaskHours.merge(allocation.getMachineCode(), taskHours, Double::sum);
            }

            Map<String, Double> machineIdleHours = new HashMap<>();
            for (Map.Entry<String, Double> entry : machineTaskHours.entrySet()) {
                machineIdleHours.put(entry.getKey(), shiftHours - entry.getValue());
                log.info("精度空闲计算: 机台={}, taskHours={}h, idle={}h, stockHours={}",
                        entry.getKey(),
                        String.format("%.1f", entry.getValue()),
                        String.format("%.1f", shiftHours - entry.getValue()),
                        String.format("%.1f", machineTotalStockHours.getOrDefault(entry.getKey(), BigDecimal.ZERO)));
            }

            CxPrecisionPlan bestPlan = null;
            double bestDeductionNeeded = Double.MAX_VALUE;
            BigDecimal bestStockHours = BigDecimal.ZERO;
            for (CxPrecisionPlan plan : futurePlans) {
                double idleH = machineIdleHours.getOrDefault(plan.getMachineCode(), (double) shiftHours);
                double deductionNeeded = Math.max(0, 4.0 - idleH);
                BigDecimal stockH = machineTotalStockHours.getOrDefault(plan.getMachineCode(), BigDecimal.ZERO);

                log.info("精度计划候选: 机台={}, planDate={}, 空闲{}h, 需扣{}h, 可供硫化{}h",
                        plan.getMachineCode(), plan.getPlanDate(),
                        String.format("%.1f", idleH), String.format("%.1f", deductionNeeded),
                        String.format("%.1f", stockH));

                if (deductionNeeded < bestDeductionNeeded
                        || (Math.abs(deductionNeeded - bestDeductionNeeded) < 0.01
                        && stockH.compareTo(bestStockHours) > 0)) {
                    bestDeductionNeeded = deductionNeeded;
                    bestStockHours = stockH;
                    bestPlan = plan;
                }
            }

            if (bestPlan != null) {
                selectedPlans.add(bestPlan);
                log.info("精度计划挑选: 未来计划提前执行, 选中机台 {} (空闲{}h, 需扣{}h, 可供硫化{}h)",
                        bestPlan.getMachineCode(),
                        String.format("%.1f", machineIdleHours.getOrDefault(bestPlan.getMachineCode(), (double) shiftHours)),
                        String.format("%.1f", bestDeductionNeeded),
                        String.format("%.1f", bestStockHours));
            } else {
                log.info("精度计划挑选: 未来计划无可排任务的机台，跳过");
            }
        }

        for (CxPrecisionPlan plan : selectedPlans) {
            applyPrecisionHourDeduction(context, scheduleDate, shiftConfig, plan.getMachineCode(), allAllocations);

            LocalTime shiftStartTime = LocalTime.parse(shiftConfig.getStartTime());
            LocalDate effectiveDate = scheduleDate;
            if (shiftConfig.getIsCrossDay() != null && shiftConfig.getIsCrossDay() == 1) {
                effectiveDate = scheduleDate.minusDays(1);
            }
            LocalDateTime precisionDateTime = LocalDateTime.of(effectiveDate, shiftStartTime);
            Date scheduleDateValue = Date.from(precisionDateTime.atZone(ZoneId.systemDefault()).toInstant());
            plan.setScheduleDate(scheduleDateValue);
            precisionPlanMapper.updateById(plan);

            log.info("精度计划回填: 机台={}, planDate={}, scheduleDate={}",
                    plan.getMachineCode(), plan.getPlanDate(), precisionDateTime);
        }

        context.setPrecisionPlanApplied(true);
    }

    /**
     * 对指定机台的任务扣减精度产能，并检查硫化需求是否仍能满足
     *
     * <p>优先使用机台空闲时间抵扣，不足部分再从任务扣减。
     * <ol>
     *   <li>计算机台总班次时长和任务已占用时间</li>
     *   <li>空闲时间 = 班次时长 - 任务已占用时间</li>
     *   <li>需扣减时间 = max(0, 4h - 空闲时间)</li>
     *   <li>按任务stockHours降序，逐任务扣减需扣减的时间</li>
     * </ol>
     * <p>硫化检查：扣量后检查"库存+剩余产量 >= 当前CLASS硫化计划量"。
     * 若不够，回写 LhScheduleResult 下调当前班次的 CLASS 计划量。
     */
    private void applyPrecisionHourDeduction(
            ScheduleContextVo context,
            LocalDate scheduleDate,
            CxShiftConfig shiftConfig,
            String machineCode,
            List<MachineAllocationResult> allAllocations) {

        MachineAllocationResult machineAllocation = null;
        for (MachineAllocationResult allocation : allAllocations) {
            if (machineCode.equals(allocation.getMachineCode())) {
                machineAllocation = allocation;
                break;
            }
        }

        if (machineAllocation == null || machineAllocation.getTaskAllocations() == null
                || machineAllocation.getTaskAllocations().isEmpty()) {
            log.info("精度扣量: 机台 {} 无任务分配，跳过扣量", machineCode);
            return;
        }

        int shiftHours = shiftConfig.getShiftHours() != null && shiftConfig.getShiftHours() > 0
                ? shiftConfig.getShiftHours() : 8;

        double totalTaskHours = 0;
        for (TaskAllocation ta : machineAllocation.getTaskAllocations()) {
            int hourlyCapacity = shiftScheduleService.getMachineHourlyCapacity(
                    machineCode, ta.getMaterialCode(), ta.getStructureName(), context);
            if (hourlyCapacity > 0) {
                int qty = ta.getEndingExtraInventory() != null
                        ? ta.getEndingExtraInventory()
                        : (ta.getQuantity() != null ? ta.getQuantity() : 0);
                totalTaskHours += (double) qty / hourlyCapacity;
            }
        }

        double idleHours = shiftHours - totalTaskHours;
        log.info("精度扣量: 机台={} 班次{}h, 任务占用{}h, 空闲{}h",
                machineCode, shiftHours, String.format("%.1f", totalTaskHours),
                String.format("%.1f", idleHours));

        if (idleHours >= 4.0) {
            log.info("精度扣量: 机台 {} 空闲时间({}h)足够覆盖精度计划(4h)，无需扣量",
                    machineCode, String.format("%.1f", idleHours));
            return;
        }

        double deductionHours = 4.0 - idleHours;
        final double totalDeductionSeconds = deductionHours * 3600;
        double remainingSeconds = totalDeductionSeconds;

        log.info("精度扣量: 机台 {} 空闲不足，需从任务扣减 {}h 产能",
                machineCode, String.format("%.1f", deductionHours));

        List<TaskAllocation> sortedTasks = machineAllocation.getTaskAllocations().stream()
                .sorted((a, b) -> {
                    BigDecimal sa = a.getStockHours() != null ? a.getStockHours() : BigDecimal.ZERO;
                    BigDecimal sb = b.getStockHours() != null ? b.getStockHours() : BigDecimal.ZERO;
                    return sb.compareTo(sa);
                })
                .collect(Collectors.toList());

        Map<Long, LhScheduleResult> lhResultCache = buildLhResultIdMap(context);
        Map<String, Integer> materialStockMap = context.getMaterialStockMap();

        int classIndex = parseClassIndex(shiftConfig);
        log.info("精度扣量硫化联动: 机台={}, lhResultCache大小={}, classIndex={}",
                machineCode, lhResultCache.size(), classIndex);
        Set<LhScheduleResult> modifiedLhResults = new HashSet<>();

        for (TaskAllocation taskAlloc : sortedTasks) {
            if (remainingSeconds <= 0) break;

            int hourlyCapacity = shiftScheduleService.getMachineHourlyCapacity(
                    machineCode, taskAlloc.getMaterialCode(), taskAlloc.getStructureName(), context);
            if (hourlyCapacity <= 0) continue;

            double secondsPerTire = 3600.0 / hourlyCapacity;

            int currentQty = taskAlloc.getEndingExtraInventory() != null
                    ? taskAlloc.getEndingExtraInventory()
                    : (taskAlloc.getQuantity() != null ? taskAlloc.getQuantity() : 0);
            if (currentQty <= 0) continue;

            int maxDeductTires = (int) (remainingSeconds / secondsPerTire);
            int actualDeduct = Math.min(maxDeductTires, currentQty);
            int newQty = currentQty - actualDeduct;

            taskAlloc.setQuantity(taskAlloc.getQuantity() != null ? taskAlloc.getQuantity() - actualDeduct : 0);
            taskAlloc.setEndingExtraInventory(newQty);
            if (actualDeduct > 0) {
                taskAlloc.setPrecisionDeducted(true);
            }

            remainingSeconds -= actualDeduct * secondsPerTire;

            log.info("精度扣量: 机台={}, 胎胚={}, stockHours={}h, 原量={}, 小时产能={}, 扣{}条, 新量={}",
                    machineCode, taskAlloc.getEmbryoCode(),
                    taskAlloc.getStockHours() != null ? String.format("%.1f", taskAlloc.getStockHours()) : "0",
                    currentQty, hourlyCapacity, actualDeduct, newQty);

            Long lhId = taskAlloc.getLhId();
            LhScheduleResult lhResult = lhId != null ? lhResultCache.get(lhId) : null;
            if (lhResult == null || classIndex <= 0) {
                log.info("  硫化联动跳过: 胎胚={}, lhId={}, lhResult={}, classIndex={}",
                        taskAlloc.getEmbryoCode(), lhId, lhResult != null ? "已找到" : "未找到", classIndex);
                continue;
            }
            Integer currentClassPlanObj = getClassPlanQtyByIndex(lhResult, classIndex);
            if (currentClassPlanObj == null || currentClassPlanObj <= 0) {
                log.info("  硫化联动跳过: 胎胚={}, CLASS{}硫化计划量为0或空({}), 无需联动",
                        taskAlloc.getEmbryoCode(), classIndex, currentClassPlanObj);
                continue;
            }
            int currentClassPlan = currentClassPlanObj;
            int stock = (materialStockMap != null)
                    ? materialStockMap.getOrDefault(String.valueOf(lhId), 0)
                    : 0;
            int totalAvailable = stock + newQty;
            if (totalAvailable < currentClassPlan) {
                String precisionNote = String.format("成型精度影响: 库存%d+产量%d=%d<硫化计划%d, 缺口%d条",
                        stock, newQty, totalAvailable, currentClassPlan, currentClassPlan - totalAvailable);
                appendClassAnalysisByIndex(lhResult, classIndex, precisionNote);
                setClassPlanQtyByIndex(lhResult, classIndex, totalAvailable);
                modifiedLhResults.add(lhResult);
                log.info("  硫化联动写入原因分析+更新计划量: 胎胚={}, lhId={}, CLASS{}硫化计划{}→{}, 库存={}+扣后产量={}={}, 缺口={}条, 原因={}",
                        taskAlloc.getEmbryoCode(), lhId, classIndex,
                        currentClassPlan, totalAvailable, stock, newQty, totalAvailable, currentClassPlan - totalAvailable, precisionNote);
            } else {
                log.info("  硫化联动检查通过: 胎胚={}, lhId={}, CLASS{}硫化计划={}, 库存={}+扣后产量={}={}, 供应充足无缺口",
                        taskAlloc.getEmbryoCode(), lhId, classIndex,
                        currentClassPlan, stock, newQty, totalAvailable);
            }
        }

        // 持久化硫化需求调整到数据库
        if (!modifiedLhResults.isEmpty()) {
            for (LhScheduleResult lhResult : modifiedLhResults) {
                lhScheduleResultMapper.updateById(lhResult);
            }
            log.info("精度扣量: 已更新 {} 条硫化排程结果到数据库", modifiedLhResults.size());
        }

        log.info("精度扣量完成: 机台={}, 需扣{}h, 实扣{}h",
                machineCode, String.format("%.1f", deductionHours),
                String.format("%.1f", (totalDeductionSeconds - remainingSeconds) / 3600));
    }

    private Map<Long, LhScheduleResult> buildLhResultIdMap(ScheduleContextVo context) {
        Map<Long, LhScheduleResult> map = new HashMap<>();
        List<LhScheduleResult> lhResults = context.getLhScheduleResults();
        if (lhResults != null) {
            for (LhScheduleResult lh : lhResults) {
                if (lh.getId() != null) {
                    map.put(lh.getId(), lh);
                }
            }
        }
        return map;
    }

    private int parseClassIndex(CxShiftConfig shiftConfig) {
        if (shiftConfig == null || shiftConfig.getClassField() == null) {
            return 0;
        }
        String cf = shiftConfig.getClassField();
        if (cf.startsWith("CLASS")) {
            try {
                return Integer.parseInt(cf.substring(5));
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

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

    private void setClassPlanQtyByIndex(LhScheduleResult lhResult, int classIndex, int value) {
        switch (classIndex) {
            case 1: lhResult.setClass1PlanQty(value); break;
            case 2: lhResult.setClass2PlanQty(value); break;
            case 3: lhResult.setClass3PlanQty(value); break;
            case 4: lhResult.setClass4PlanQty(value); break;
            case 5: lhResult.setClass5PlanQty(value); break;
            case 6: lhResult.setClass6PlanQty(value); break;
            case 7: lhResult.setClass7PlanQty(value); break;
            case 8: lhResult.setClass8PlanQty(value); break;
            default: break;
        }
    }

    private void appendClassAnalysisByIndex(LhScheduleResult lhResult, int classIndex, String text) {
        String original;
        switch (classIndex) {
            case 1: original = lhResult.getClass1Analysis();
                lhResult.setClass1Analysis(original != null && !original.isEmpty() ? original + "," + text : text); break;
            case 2: original = lhResult.getClass2Analysis();
                lhResult.setClass2Analysis(original != null && !original.isEmpty() ? original + "," + text : text); break;
            case 3: original = lhResult.getClass3Analysis();
                lhResult.setClass3Analysis(original != null && !original.isEmpty() ? original + "," + text : text); break;
            case 4: original = lhResult.getClass4Analysis();
                lhResult.setClass4Analysis(original != null && !original.isEmpty() ? original + "," + text : text); break;
            case 5: original = lhResult.getClass5Analysis();
                lhResult.setClass5Analysis(original != null && !original.isEmpty() ? original + "," + text : text); break;
            case 6: original = lhResult.getClass6Analysis();
                lhResult.setClass6Analysis(original != null && !original.isEmpty() ? original + "," + text : text); break;
            case 7: original = lhResult.getClass7Analysis();
                lhResult.setClass7Analysis(original != null && !original.isEmpty() ? original + "," + text : text); break;
            case 8: original = lhResult.getClass8Analysis();
                lhResult.setClass8Analysis(original != null && !original.isEmpty() ? original + "," + text : text); break;
            default: break;
        }
    }

    /**
     * 精度计划扣量处理（已废弃，改为 applyPrecisionPlanSelection 每日首次执行）
     *
     * <p>业务规则：
     * <ul>
     *   <li>1. 从CxPrecisionPlan获取未完成的精度计划（planDate <= 排程日期+提前天数，actualDate为空）</li>
     *   <li>2. 将机台按其下所有任务的可供硫化时间降序排序</li>
     *   <li>3. 检查排序靠前的机台是否在精度计划列表中</li>
     *   <li>4. 若可供硫化时长 >= 4小时：硫化不停机，只扣成型4小时产能</li>
     *   <li>5. 若可供硫化时长 < 4小时：硫化产能减半（扣4/8=1/2），对应任务计划量减半</li>
     *   <li>6. 每天最多安排2台机器做精度</li>
     * </ul>
     *
     * @param context      排程上下文（含精度计划列表）
     * @param shiftResults 所有班次的排产结果
     */
    private void applyPrecisionPlanDeduction(ScheduleContextVo context,
                                             List<ShiftScheduleResult> shiftResults) {
        List<CxPrecisionPlan> precisionPlans = context.getPrecisionPlans();
        if (precisionPlans == null || precisionPlans.isEmpty()) {
            log.info("无精度计划需要处理，跳过精度扣量");
            return;
        }

        // 精度计划按机台编码索引
        Set<String> precisionMachineCodes = precisionPlans.stream()
                .map(CxPrecisionPlan::getMachineCode)
                .collect(Collectors.toSet());

        log.info("精度计划涉及机台：{}", precisionMachineCodes);

        // 按天分组处理（每天最多2台做精度）
        Map<Integer, List<ShiftScheduleResult>> dayShiftMap = shiftResults.stream()
                .collect(Collectors.groupingBy(ShiftScheduleResult::getDay));

        for (Map.Entry<Integer, List<ShiftScheduleResult>> dayEntry : dayShiftMap.entrySet()) {
            int day = dayEntry.getKey();
            List<ShiftScheduleResult> dayShifts = dayEntry.getValue();

            // 收集该天所有机台，按可供硫化时间降序排序
            // 可供硫化时间 = 机台下所有任务stockHours的总和
            Map<String, BigDecimal> machineStockHoursMap = new LinkedHashMap<>();
            Map<String, List<ShiftScheduleService.ShiftProductionResult>> machineResultsMap = new LinkedHashMap<>();

            for (ShiftScheduleResult shiftResult : dayShifts) {
                if (shiftResult.getShiftProductionResults() == null) continue;
                for (ShiftScheduleService.ShiftProductionResult result : shiftResult.getShiftProductionResults()) {
                    String machineCode = result.getMachineCode();
                    machineStockHoursMap.merge(machineCode,
                            result.getStockHours() != null ? result.getStockHours() : BigDecimal.ZERO,
                            BigDecimal::add);
                    machineResultsMap.computeIfAbsent(machineCode, k -> new ArrayList<>()).add(result);
                }
            }

            // 按可供硫化时间降序排序机台
            List<String> sortedMachines = machineStockHoursMap.entrySet().stream()
                    .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            // 精度扣量：最多2台机器
            int precisionMachineCount = 0;
            for (String machineCode : sortedMachines) {
                if (!precisionMachineCodes.contains(machineCode)) continue;
                if (precisionMachineCount >= 2) break;

                BigDecimal totalStockHours = machineStockHoursMap.get(machineCode);
                List<ShiftScheduleService.ShiftProductionResult> machineResults = machineResultsMap.get(machineCode);

                log.info("精度扣量：天={}, 机台={}, 可供硫化时长={}h", day, machineCode, totalStockHours);

                if (totalStockHours.compareTo(BigDecimal.valueOf(4)) >= 0) {
                    // 可供硫化时长 >= 4小时：硫化不停机，只扣成型4小时产能
                    // 成型精度4小时意味着该机台对应的任务需要扣4小时产能
                    for (ShiftScheduleService.ShiftProductionResult result : machineResults) {
                        if (result.getHourCapacity() != null && result.getHourCapacity() > 0) {
                            // 扣4小时产能 = hourCapacity * 4 条
                            int deduction = result.getHourCapacity() * 4;
                            int newQty = Math.max(0, result.getQuantity() - deduction);
                            log.info("  精度扣量（硫化不停机）：embryoCode={}, 原量={}, 扣量={}, 新量={}",
                                    result.getEmbryoCode(), result.getQuantity(), deduction, newQty);
                            result.setQuantity(newQty);
                        }
                    }
                } else {
                    // 可供硫化时长 < 4小时：硫化产能减半（扣4/8=1/2）
                    // 对应机台里面那些任务，每个判断可供硫化时长小于4的要扣一半产能
                    for (ShiftScheduleService.ShiftProductionResult result : machineResults) {
                        BigDecimal taskStockHours = result.getStockHours() != null ? result.getStockHours() : BigDecimal.ZERO;
                        if (taskStockHours.compareTo(BigDecimal.valueOf(4)) < 0) {
                            // 可供硫化时长 < 4小时，扣一半产能
                            int halfQty = result.getQuantity() / 2;
                            int newQty = result.getQuantity() - halfQty;
                            log.info("  精度扣量（硫化减半）：embryoCode={}, 原量={}, 扣量={}, 新量={}, 可供硫化时长={}h",
                                    result.getEmbryoCode(), result.getQuantity(), halfQty, newQty, taskStockHours);
                            result.setQuantity(newQty);
                        }
                    }
                }

                precisionMachineCount++;
            }

            log.info("精度扣量处理完成：天={}, 共处理 {} 台机器", day, precisionMachineCount);
        }
    }

    /**
     * 班次量均衡
     *
     * <p>业务规则：
     * <ul>
     *   <li>1. 按结构向下，找到绑定最大硫化机数的胎胚计划</li>
     *   <li>2. 通过整车调整使每个班次的计划量趋于平衡</li>
     *   <li>3. 最后一个班次的计划量 = 总量 - SUM(第1条到倒数第2条的计划量)</li>
     * </ul>
     *
     * <p>示例：
     * <pre>
     * 序号  物料   硫化机数  班次计划量(夜-早-中)  均衡后的班次计划量
     * 1    胎胚1   1        11-22-11              11-22-11
     * 2    胎胚2   2        32-32-32              32-32-32
     * 3    胎胚3   5        76-86-76              86-76-86（整车调整使班次均衡）
     * </pre>
     *
     * @param context        排程上下文
     * @param shiftResults   所有班次的排产结果
     * @param allShiftConfigs 所有班次配置
     */
    private void balanceShiftQuantities(ScheduleContextVo context,
                                        List<ShiftScheduleResult> shiftResults,
                                        List<CxShiftConfig> allShiftConfigs) {
        // 按天分组排产结果
        Map<Integer, List<ShiftScheduleResult>> dayShiftMap = shiftResults.stream()
                .collect(Collectors.groupingBy(ShiftScheduleResult::getDay));

        for (Map.Entry<Integer, List<ShiftScheduleResult>> dayEntry : dayShiftMap.entrySet()) {
            int day = dayEntry.getKey();
            List<ShiftScheduleResult> dayShifts = dayEntry.getValue();

            if (dayShifts.size() < 2) continue; // 至少2个班次才需要均衡

            // 收集该天所有排产结果，按胎胚编码分组
            // embryoCode -> List<ShiftProductionResult>（按班次顺序）
            Map<String, List<ShiftScheduleService.ShiftProductionResult>> embryoByShiftMap = new LinkedHashMap<>();
            for (ShiftScheduleResult shiftResult : dayShifts) {
                if (shiftResult.getShiftProductionResults() == null) continue;
                for (ShiftScheduleService.ShiftProductionResult result : shiftResult.getShiftProductionResults()) {
                    embryoByShiftMap.computeIfAbsent(result.getEmbryoCode(), k -> new ArrayList<>()).add(result);
                }
            }

            // 找到绑定最大硫化机数的胎胚
            String maxMachineEmbryo = null;
            int maxMachineCount = 0;
            for (Map.Entry<String, List<ShiftScheduleService.ShiftProductionResult>> entry : embryoByShiftMap.entrySet()) {
                String embryoCode = entry.getKey();
                List<ShiftScheduleService.ShiftProductionResult> results = entry.getValue();
                if (results.isEmpty()) continue;

                // 从sourceTask获取硫化机数
                int vulcanizeMachineCount = 0;
                if (results.get(0).getSourceTask() != null && results.get(0).getSourceTask().getVulcanizeMachineCount() != null) {
                    vulcanizeMachineCount = results.get(0).getSourceTask().getVulcanizeMachineCount();
                }
                if (vulcanizeMachineCount > maxMachineCount) {
                    maxMachineCount = vulcanizeMachineCount;
                    maxMachineEmbryo = embryoCode;
                }
            }

            if (maxMachineEmbryo == null) {
                log.info("天={}：未找到绑定最大硫化机数的胎胚，跳过均衡", day);
                continue;
            }

            List<ShiftScheduleService.ShiftProductionResult> targetResults = embryoByShiftMap.get(maxMachineEmbryo);
            log.info("班次均衡：天={}, 最大硫化机数胎胚={}, 硫化机数={}", day, maxMachineEmbryo, maxMachineCount);

            // 对该胎胚的各班次计划量进行均衡
            // 均衡策略：通过整车调整使各班次量趋于平衡
            // 最后一个班次 = 总量 - SUM(第1条到倒数第2条的计划量)
            int totalActualQty = targetResults.stream()
                    .mapToInt(r -> r.getQuantity() != null ? r.getQuantity() : 0)
                    .sum();

            if (totalActualQty <= 0) continue;

            // 获取整车条数
            int tripCapacity = targetResults.get(0).getTripCapacity() != null ? targetResults.get(0).getTripCapacity() : 1;
            if (tripCapacity <= 0) tripCapacity = 1;

            // 计算每个班次应该分配的平均量（按整车取整）
            int avgPerShift = totalActualQty / targetResults.size();
            int avgCarsPerShift = avgPerShift / tripCapacity;

            // 重新分配：前N-1个班次按整车取整，最后一个班次用余额
            int allocatedTotal = 0;
            for (int i = 0; i < targetResults.size() - 1; i++) {
                ShiftScheduleService.ShiftProductionResult result = targetResults.get(i);
                int balancedQty = avgCarsPerShift * tripCapacity;
                result.setQuantity(balancedQty);
                allocatedTotal += balancedQty;
                log.info("  班次均衡：班次索引={}, 胚胎={}, 均衡后量={}", i, maxMachineEmbryo, balancedQty);
            }

            // 最后一个班次 = 总量 - 已分配
            ShiftScheduleService.ShiftProductionResult lastResult = targetResults.get(targetResults.size() - 1);
            int lastQty = totalActualQty - allocatedTotal;
            lastResult.setQuantity(lastQty);
            log.info("  班次均衡：最后班次，胚胎={}, 均衡后量={}（余额）", maxMachineEmbryo, lastQty);
        }
    }

    /**
     * 更新机台在产状态
     */
    private Map<String, Set<String>> updateMachineOnlineStatus(
            List<MachineAllocationResult> allocations,
            Map<String, Set<String>> currentMachineOnlineMap) {

        Map<String, Set<String>> newMap = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : currentMachineOnlineMap.entrySet()) {
            newMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }

        for (MachineAllocationResult allocation : allocations) {
            for (TaskAllocation taskAlloc : allocation.getTaskAllocations()) {
                if (taskAlloc.getEmbryoCode() != null) {
                    newMap.computeIfAbsent(taskAlloc.getEmbryoCode(), k -> new HashSet<>())
                            .add(allocation.getMachineCode());
                }
            }
        }

        log.debug("更新机台在产状态完成，共 {} 个胎胚: {}", newMap.size(), formatMachineEmbryoMap(newMap));
        return newMap;
    }

    /**
     * 格式化机台胚胎映射用于日志输出
     */
    private String formatMachineEmbryoMap(Map<String, Set<String>> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(entry.getKey()).append("→[");
            boolean firstItem = true;
            for (String item : entry.getValue()) {
                if (!firstItem) sb.append(",");
                sb.append(item);
                firstItem = false;
            }
            sb.append("]");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 单班次排产结果
     */
    public static class ShiftScheduleResult {
        /** 排产日（1-3），与 CxShiftConfig.scheduleDay 对应 */
        private int day;
        /** 排产日期 */
        private LocalDate scheduleDate;
        /** 该班次的班次配置 */
        private CxShiftConfig shiftConfig;
        /** 该班次所有机台的任务分配结果（包含续作/新任务/试制任务分配） */
        private List<MachineAllocationResult> allAllocations;
        /** 该班次的精排结果（包含班次级别的车数/数量） */
        private List<ShiftScheduleService.ShiftProductionResult> shiftProductionResults;

        public int getDay() { return day; }
        public void setDay(int day) { this.day = day; }
        public LocalDate getScheduleDate() { return scheduleDate; }
        public void setScheduleDate(LocalDate scheduleDate) { this.scheduleDate = scheduleDate; }
        public CxShiftConfig getShiftConfig() { return shiftConfig; }
        public void setShiftConfig(CxShiftConfig shiftConfig) { this.shiftConfig = shiftConfig; }
        public List<MachineAllocationResult> getAllAllocations() { return allAllocations; }
        public void setAllAllocations(List<MachineAllocationResult> allAllocations) { this.allAllocations = allAllocations; }
        public List<ShiftScheduleService.ShiftProductionResult> getShiftProductionResults() { return shiftProductionResults; }
        public void setShiftProductionResults(List<ShiftScheduleService.ShiftProductionResult> shiftProductionResults) { this.shiftProductionResults = shiftProductionResults; }
    }

    /**
     * 按班次排程后合并结果（与 buildFinalScheduleResults 逻辑一致，但输入是 ShiftScheduleResult）
     */
    private List<CxScheduleResult> buildFinalScheduleResultsFromShifts(
            ScheduleContextVo context,
            List<ShiftScheduleResult> shiftResults,
            List<CxShiftConfig> allShiftConfigs) {

        // 构建 shiftCode+scheduleDay → classField 的映射
        Map<String, String> shiftClassFieldMap = new HashMap<>();
        for (CxShiftConfig shiftConfig : allShiftConfigs) {
            String key = shiftConfig.getShiftCode() + "_" + shiftConfig.getScheduleDay();
            shiftClassFieldMap.put(key, shiftConfig.getClassField());
        }

        // ==================== 按 机台+胎胚+SAP物料 三维维度汇总班次排量 ====================
        Map<String, Map<String, ShiftScheduleService.ShiftProductionResult>> taskClassSprMap = new LinkedHashMap<>();
        Map<String, Integer> taskTotalQtyMap = new LinkedHashMap<>();
        Map<String, String> taskStructureMap = new LinkedHashMap<>();
        Map<String, List<Long>> taskLhIdListMap = new LinkedHashMap<>();
        Map<String, Set<String>> taskMaterialCodeMap = new LinkedHashMap<>();

        for (ShiftScheduleResult shiftResult : shiftResults) {
            int day = shiftResult.getDay();
            // 直接从 ShiftScheduleResult 获取 classField，无需查表
            String classField = shiftResult.getShiftConfig() != null
                    ? shiftResult.getShiftConfig().getClassField() : null;
            if (classField == null) {
                // 回退：从映射表查找
                String shiftCode = shiftResult.getShiftConfig() != null
                        ? shiftResult.getShiftConfig().getShiftCode() : null;
                if (shiftCode != null) {
                    classField = shiftClassFieldMap.get(shiftCode + "_" + day);
                }
            }
            log.info("主表合并班次: day={}, classField={}, shiftCode={}, productionResults={}",
                    day, classField,
                    shiftResult.getShiftConfig() != null ? shiftResult.getShiftConfig().getShiftCode() : null,
                    shiftResult.getShiftProductionResults() != null ? shiftResult.getShiftProductionResults().size() : 0);

            for (ShiftScheduleService.ShiftProductionResult spr : shiftResult.getShiftProductionResults()) {
                String machineCode = spr.getMachineCode();
                String embryoCode = spr.getEmbryoCode();
                String materialCode = spr.getMaterialCode() != null ? spr.getMaterialCode() : "";

                // 优先使用从 ShiftScheduleResult 获取的 classField
                String effectiveClassFieldTmp = classField;
                if (effectiveClassFieldTmp == null) {
                    String shiftCode = spr.getShiftCode();
                    String shiftKey = shiftCode + "_" + day;
                    effectiveClassFieldTmp = shiftClassFieldMap.get(shiftKey);
                }

                if (effectiveClassFieldTmp == null) {
                    log.warn("未找到班次映射: shiftCode={}, day={}", spr.getShiftCode(), day);
                    continue;
                }
                final String effectiveClassField = effectiveClassFieldTmp;

                String taskKey = machineCode + "|" + embryoCode + "|" + (spr.getConstructionStage() != null ? spr.getConstructionStage() : "");
                // 独立追踪每个embryo+constructionStage下的所有materialCode（不按机台拆分）
                String embryoTaskKey = embryoCode + "|" + (spr.getConstructionStage() != null ? spr.getConstructionStage() : "");
                if (!materialCode.isEmpty()) {
                    taskMaterialCodeMap.computeIfAbsent(embryoTaskKey, k -> new LinkedHashSet<>()).add(materialCode);
                }
                taskClassSprMap.computeIfAbsent(taskKey, k -> new LinkedHashMap<>())
                        .compute(effectiveClassField, (k, existing) -> {
                            if (existing == null) {
                                return spr;
                            }
                            ShiftScheduleService.ShiftProductionResult merged = new ShiftScheduleService.ShiftProductionResult();
                            merged.setMachineCode(existing.getMachineCode());
                            merged.setEmbryoCode(existing.getEmbryoCode());
                            merged.setMaterialCode(existing.getMaterialCode());
                            merged.setMaterialDesc(existing.getMaterialDesc());
                            merged.setMainMaterialDesc(existing.getMainMaterialDesc());
                            merged.setStructureName(existing.getStructureName());
                            merged.setShiftCode(effectiveClassField);
                            merged.setQuantity((existing.getQuantity() != null ? existing.getQuantity() : 0)
                                    + (spr.getQuantity() != null ? spr.getQuantity() : 0));
                            merged.setTripNo(existing.getTripNo());
                            merged.setTripCapacity(existing.getTripCapacity());
                            merged.setStockHours(existing.getStockHours());
                            merged.setSequence(existing.getSequence());
                            merged.setPlanStartTime(existing.getPlanStartTime());
                            merged.setPlanEndTime(existing.getPlanEndTime());

                            // ---- 合并 sourceTask：任一记录有 isUrgentEnding=true 则保留 ----
                            CoreScheduleAlgorithmService.DailyEmbryoTask existingTask = existing.getSourceTask();
                            CoreScheduleAlgorithmService.DailyEmbryoTask sprTask = spr.getSourceTask();
                            boolean hasUrgentEnding = (existingTask != null && Boolean.TRUE.equals(existingTask.getIsUrgentEnding()))
                                    || (sprTask != null && Boolean.TRUE.equals(sprTask.getIsUrgentEnding()));
                            if (hasUrgentEnding || existingTask != null) {
                                CoreScheduleAlgorithmService.DailyEmbryoTask mergedTask = new CoreScheduleAlgorithmService.DailyEmbryoTask();
                                mergedTask.setEmbryoCode(existingTask != null ? existingTask.getEmbryoCode() : (sprTask != null ? sprTask.getEmbryoCode() : null));
                                mergedTask.setMaterialCode(existingTask != null ? existingTask.getMaterialCode() : (sprTask != null ? sprTask.getMaterialCode() : null));
                                mergedTask.setIsUrgentEnding(hasUrgentEnding);
                                mergedTask.setIsNearEnding((existingTask != null && Boolean.TRUE.equals(existingTask.getIsNearEnding()))
                                        || (sprTask != null && Boolean.TRUE.equals(sprTask.getIsNearEnding())));
                                mergedTask.setIsEndingTask((existingTask != null && Boolean.TRUE.equals(existingTask.getIsEndingTask()))
                                        || (sprTask != null && Boolean.TRUE.equals(sprTask.getIsEndingTask())));
                                mergedTask.setIsTrialTask((existingTask != null && Boolean.TRUE.equals(existingTask.getIsTrialTask()))
                                        || (sprTask != null && Boolean.TRUE.equals(sprTask.getIsTrialTask())));
                                mergedTask.setIsFirstTask((existingTask != null && Boolean.TRUE.equals(existingTask.getIsFirstTask()))
                                        || (sprTask != null && Boolean.TRUE.equals(sprTask.getIsFirstTask())));
                                mergedTask.setIsContinueTask((existingTask != null && Boolean.TRUE.equals(existingTask.getIsContinueTask()))
                                        || (sprTask != null && Boolean.TRUE.equals(sprTask.getIsContinueTask())));
                                // ---- 合并 isOpeningDayTask：任一记录有 isOpeningDayTask=true 则保留 ----
                                mergedTask.setIsOpeningDayTask((existingTask != null && Boolean.TRUE.equals(existingTask.getIsOpeningDayTask()))
                                        || (sprTask != null && Boolean.TRUE.equals(sprTask.getIsOpeningDayTask())));
                                // ---- 合并 isClosingDayTask：任一记录有 isClosingDayTask=true 则保留 ----
                                mergedTask.setIsClosingDayTask((existingTask != null && Boolean.TRUE.equals(existingTask.getIsClosingDayTask()))
                                        || (sprTask != null && Boolean.TRUE.equals(sprTask.getIsClosingDayTask())));
                                // ---- 合并 isProductionTrial：任一记录有 isProductionTrial=true 则保留 ----
                                mergedTask.setIsProductionTrial((existingTask != null && Boolean.TRUE.equals(existingTask.getIsProductionTrial()))
                                        || (sprTask != null && Boolean.TRUE.equals(sprTask.getIsProductionTrial())));
                                // ---- 合并 isEndProduction：任一记录有 isEndProduction=true 则保留 ----
                                mergedTask.setIsEndProduction((existingTask != null && Boolean.TRUE.equals(existingTask.getIsEndProduction()))
                                        || (sprTask != null && Boolean.TRUE.equals(sprTask.getIsEndProduction())));
                                // ---- 合并 endingAbandoned：任一记录有 endingAbandoned=true 则保留 ----
                                mergedTask.setEndingAbandoned((existingTask != null && Boolean.TRUE.equals(existingTask.getEndingAbandoned()))
                                        || (sprTask != null && Boolean.TRUE.equals(sprTask.getEndingAbandoned())));
                                // ---- 合并 isLastEndingBatch：任一记录有 isLastEndingBatch=true 则保留 ----
                                mergedTask.setIsLastEndingBatch(
                                        Boolean.TRUE.equals(existingTask != null ? existingTask.getIsLastEndingBatch() : null)
                                                || Boolean.TRUE.equals(sprTask != null ? sprTask.getIsLastEndingBatch() : null));
                                merged.setSourceTask(mergedTask);
                                // 同时设置 spr 的 isLastEndingBatch（buildTaskAnalysis 使用 spr.getIsLastEndingBatch()）
                                merged.setIsLastEndingBatch(mergedTask.getIsLastEndingBatch());
                            } else if (sprTask != null) {
                                merged.setSourceTask(sprTask);
                            }

                            // 注意：isLastEndingBatch 不在此处合并，每个班次保持独立状态
                            return merged;
                        });
                taskTotalQtyMap.merge(taskKey, spr.getQuantity() != null ? spr.getQuantity() : 0, Integer::sum);
                if (spr.getStructureName() != null) {
                    taskStructureMap.putIfAbsent(taskKey, spr.getStructureName());
                }
            }
        }

        // 从 ShiftScheduleResult 的 allAllocations 中收集 lhId 信息
        for (ShiftScheduleResult shiftResult : shiftResults) {
            for (MachineAllocationResult allocation : shiftResult.getAllAllocations()) {
                if (allocation.getTaskAllocations() != null) {
                    for (TaskAllocation taskAlloc : allocation.getTaskAllocations()) {
                        String embryoCode = taskAlloc.getEmbryoCode();
                        String materialCode = taskAlloc.getMaterialCode() != null ? taskAlloc.getMaterialCode() : "";
                        String constructionStage = taskAlloc.getConstructionStage() != null ? taskAlloc.getConstructionStage() : "";
                        String embryoLhKey = embryoCode + "|" + constructionStage;
                        if (taskAlloc.getLhId() != null) {
                            taskLhIdListMap.computeIfAbsent(embryoLhKey, k -> new ArrayList<>()).add(taskAlloc.getLhId());
                        }
                    }
                }
            }
        }

        // ==================== 构建辅助查询映射（复用逻辑） ====================
        Map<String, MdmMoldingMachine> machineMap = new HashMap<>();
        if (context.getAvailableMachines() != null) {
            for (MdmMoldingMachine machine : context.getAvailableMachines()) {
                machineMap.put(machine.getCxMachineCode(), machine);
            }
        }

        Map<String, MdmMaterialInfo> materialByCodeMap = new HashMap<>();
        Map<String, MdmMaterialInfo> materialByEmbryoMap = new HashMap<>();
        if (context.getMaterials() != null) {
            for (MdmMaterialInfo material : context.getMaterials()) {
                if (material.getMaterialCode() != null) {
                    materialByCodeMap.putIfAbsent(material.getMaterialCode(), material);
                }
                if (material.getEmbryoCode() != null) {
                    materialByEmbryoMap.putIfAbsent(material.getEmbryoCode(), material);
                }
            }
        }

        Map<Long, LhScheduleResult> lhByIdMap = new HashMap<>();
        Map<String, List<LhScheduleResult>> materialCodeToLhMap = new HashMap<>();
        if (context.getLhScheduleResults() != null) {
            for (LhScheduleResult lh : context.getLhScheduleResults()) {
                if (lh.getId() != null) {
                    lhByIdMap.put(lh.getId(), lh);
                }
                if (lh.getMaterialCode() != null) {
                    materialCodeToLhMap.computeIfAbsent(lh.getMaterialCode(), k -> new ArrayList<>()).add(lh);
                }
            }
        }

        // ---- SKU与示方书关系映射（materialCode+constructionStage -> embryoType） ----
        Map<String, String> skuRecipeTypeMap = new HashMap<>();
        try {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MdmSkuConstructionRef> skuQueryWrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            skuQueryWrapper.select(MdmSkuConstructionRef::getMaterialCode,
                    MdmSkuConstructionRef::getTrialStatus,
                    MdmSkuConstructionRef::getEmbryoType);
            skuQueryWrapper.eq(MdmSkuConstructionRef::getIsDelete, 0);
            List<MdmSkuConstructionRef> skuRefList = skuConstructionRefMapper.selectList(skuQueryWrapper);
            if (skuRefList != null) {
                for (MdmSkuConstructionRef ref : skuRefList) {
                    if (ref.getMaterialCode() != null && ref.getTrialStatus() != null && ref.getEmbryoType() != null) {
                        String mapKey = ref.getMaterialCode() + "|" + ref.getTrialStatus();
                        skuRecipeTypeMap.putIfAbsent(mapKey, ref.getEmbryoType());
                    }
                }
            }
            log.info("SKU与示方书关系映射加载完成，共 {} 条记录", skuRecipeTypeMap.size());
        } catch (Exception e) {
            log.warn("加载SKU与示方书关系映射失败，将回退使用constructionStage作为recipeType: {}", e.getMessage());
        }

        // ==================== 构建最终的 CxScheduleResult 列表 ====================
        List<CxScheduleResult> results = new ArrayList<>();
        LocalDate startDate = context.getScheduleDate();
        String dateStr = startDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String cxBatchNo = "CXPC" + dateStr;  // 同一批次共用批次号
        int orderSeq = 0;

        for (Map.Entry<String, Map<String, ShiftScheduleService.ShiftProductionResult>> entry : taskClassSprMap.entrySet()) {
            String taskKey = entry.getKey();
            Map<String, ShiftScheduleService.ShiftProductionResult> classSprMap = entry.getValue();

            String[] parts = taskKey.split("\\|", 3);
            String machineCode = parts[0];
            String embryoCode = parts.length > 1 ? parts[1] : null;
            String constructionStage = parts.length > 2 && !parts[2].isEmpty() ? parts[2] : null;
            // materialCode 从独立的 taskMaterialCodeMap 中获取（embryo级别，不按机台拆分）
            String embryoMaterialKey = embryoCode + "|" + (constructionStage != null ? constructionStage : "");
            Set<String> materialCodeSet = taskMaterialCodeMap.get(embryoMaterialKey);
            String materialCode = null;
            if (materialCodeSet != null && !materialCodeSet.isEmpty()) {
                materialCode = String.join(",", materialCodeSet);
            }
            String structureName = taskStructureMap.get(taskKey);

            CxScheduleResult result = new CxScheduleResult();

            // ---- 排程日期 ----
            result.setScheduleDate(java.sql.Timestamp.valueOf(startDate.atStartOfDay()));

            // ---- 机台信息 ----
            result.setCxMachineCode(machineCode);
            MdmMoldingMachine machine = machineMap.get(machineCode);
            if (machine != null) {
                result.setCxMachineName(machine.getMachineName());
                result.setCxMachineType(machine.getCxMachineBrandCode());
            }

            // ---- 胎胚信息 ----
            if (embryoCode != null) {
                result.setEmbryoCode(embryoCode);
                MdmMaterialInfo materialByEmbryo = materialByEmbryoMap.get(embryoCode);
                if (materialByEmbryo != null) {
                    result.setMainMaterialDesc(materialByEmbryo.getEmbryoDesc());
                    if (materialByEmbryo.getProSize() != null) {
                        try {
                            result.setSpecDimension(new BigDecimal(materialByEmbryo.getProSize()));
                        } catch (NumberFormatException e) {
                            log.debug("无法解析寸口: {}", materialByEmbryo.getProSize());
                        }
                    }
                    result.setStructureName(materialByEmbryo.getStructureName() != null ? materialByEmbryo.getStructureName() : structureName);
                } else {
                    result.setStructureName(structureName);
                }
            }

            // ---- 物料信息 ----
            if (materialCode != null) {
                result.setMaterialCode(materialCode);
                MdmMaterialInfo materialByCode = materialByCodeMap.get(materialCode);
                if (materialByCode != null) {
                    result.setMaterialDesc(materialByCode.getMaterialDesc());
                    result.setBomDataVersion(materialByCode.getEmbryoNo());
                    if (materialByCode.getStructureName() != null) {
                        result.setStructureName(materialByCode.getStructureName());
                    }
                }
            }

            // ---- 库存信息（求和合并多个lhId的库存，embryo级别不按机台拆分） ----
            String embryoLhKey = embryoCode + "|" + (constructionStage != null ? constructionStage : "");
            List<Long> lhIdList = taskLhIdListMap.get(embryoLhKey);
            int totalStock = 0;
            if (lhIdList != null && !lhIdList.isEmpty()) {
                List<Long> distinctLhIdList = lhIdList.stream().distinct().collect(Collectors.toList());
                Map<String, Integer> stockMap = context.getInitialMaterialStockMap();
                for (Long lhId : distinctLhIdList) {
                    if (stockMap != null) {
                        Integer stock = stockMap.get(String.valueOf(lhId));
                        totalStock += (stock != null ? stock : 0);
                    }
                }
            }
            result.setTotalStock(new BigDecimal(totalStock));

            // ---- 硫化信息（合并多个硫化任务） ----
            List<LhScheduleResult> allLhResults = new ArrayList<>();
            if (lhIdList != null && !lhIdList.isEmpty()) {
                // 去重：同一个lhId可能在多个班次分配中重复出现
                List<Long> distinctLhIdList = lhIdList.stream().distinct().collect(Collectors.toList());
                for (Long lhId : distinctLhIdList) {
                    LhScheduleResult lh = lhByIdMap.get(lhId);
                    if (lh != null) {
                        allLhResults.add(lh);
                    }
                }
            }
            // 如果没有lhId，尝试按物料查找
            if (allLhResults.isEmpty() && materialCodeToLhMap != null && materialCode != null) {
                List<LhScheduleResult> related = materialCodeToLhMap.get(materialCode);
                if (related != null) {
                    allLhResults.addAll(related);
                }
            }

            // 第一个硫化任务（用于lhClassQty和lhRemainQty）
            LhScheduleResult primaryLh = allLhResults.isEmpty() ? null : allLhResults.get(0);

            if (!allLhResults.isEmpty()) {
                // lhScheduleIds: 去重后逗号分隔合并
                String lhIds = lhIdList != null ? lhIdList.stream()
                        .distinct()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")) : null;
                result.setLhScheduleIds(lhIds);

                // lhMachineCode: 逗号分隔合并
                String lhMachineCodes = allLhResults.stream()
                        .map(LhScheduleResult::getLhMachineCode)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.joining(","));
                result.setLhMachineCode(lhMachineCodes);

                // lhMachineName: 逗号分隔合并
                String lhMachineNames = allLhResults.stream()
                        .map(LhScheduleResult::getLhMachineName)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.joining(","));
                result.setLhMachineName(lhMachineNames);

                // lhMachineQty: 求和合并
                int totalLhMachineQty = 0;
                for (LhScheduleResult lh : allLhResults) {
                    if (lh.getMouldQty() != null) {
                        totalLhMachineQty += lh.getMouldQty();
                    }
                }
                result.setLhMachineQty(new BigDecimal(totalLhMachineQty));

                // lhClassQty: 只取第一个
                if (primaryLh != null && primaryLh.getSingleMouldShiftQty() != null) {
                    result.setLhClassQty(new BigDecimal(primaryLh.getSingleMouldShiftQty()));
                }

                // lhRemainQty: 对合并后的所有materialCode求和（剔除维度后可能多个物料合并）使用初始快照
                Map<String, BigDecimal> initialMonthSurplusMap = context.getInitialMonthSurplusMap();
                BigDecimal totalLhRemain = null;
                if (initialMonthSurplusMap != null) {
                    for (String mc : materialCodeSet) {
                        BigDecimal surplus = initialMonthSurplusMap.get(mc);
                        if (surplus != null) {
                            totalLhRemain = (totalLhRemain == null)
                                    ? surplus : totalLhRemain.add(surplus);
                        }
                    }
                }
                if (totalLhRemain == null && embryoCode != null) {
                    // 兜底：按胎胚编码查找
                    BigDecimal surplus = initialMonthSurplusMap != null ? initialMonthSurplusMap.get(embryoCode) : null;
                    if (surplus != null) {
                        totalLhRemain = surplus;
                    }
                }
                if (totalLhRemain != null) {
                    result.setLhRemainQty(totalLhRemain);
                }
            }

            // ---- 成型余量（重新计算 = lhRemainQty - totalStock，多物料求和 - 总库存） ----
            BigDecimal lhRemainQty = result.getLhRemainQty();
            if (lhRemainQty != null) {
                result.setCxRemainQty(lhRemainQty.subtract(result.getTotalStock()));
            } else {
                // 如果没有lhRemainQty，对合并后的所有materialCode求和
                Map<String, Integer> initialFormingRemainderMap = context.getInitialFormingRemainderMap();
                if (initialFormingRemainderMap != null && !materialCodeSet.isEmpty()) {
                    int totalCxRemain = 0;
                    for (String mc : materialCodeSet) {
                        Integer cxRemain = initialFormingRemainderMap.get(mc);
                        if (cxRemain != null) {
                            totalCxRemain += cxRemain;
                        }
                    }
                    result.setCxRemainQty(new BigDecimal(totalCxRemain));
                }
            }

            // ---- 胎胚总计划量 ----
            int totalQty = taskTotalQtyMap.getOrDefault(taskKey, 0);
            result.setProductNum(new BigDecimal(totalQty));

            // ---- 状态字段 ----
            result.setProductionStatus("0");
            result.setIsRelease("0");
            result.setDataSource("0");
            result.setFactoryCode(context.getFactoryCode());
            result.setCreateTime(new Date());

            // ---- 成型批次号 & 工单号 ----
            orderSeq++;
            String orderNo = "CXGD" + dateStr + String.format("%03d", orderSeq);
            result.setCxBatchNo(cxBatchNo);
            result.setOrderNo(orderNo);

            // ---- 收尾提示 ----
            boolean isUrgentEnding = false;
            boolean hasTrialOrProductionTrial = false;
            boolean hasFirstTask = false;
            boolean hasNearEnding = false;
            log.info("开始遍历classSprMap.values()设置颜色标记: machineCode={}, embryoCode={}, materialCode={}, classSprMap.size={}",
                    machineCode, embryoCode, materialCode, classSprMap.size());
            int sprIndex = 0;
            for (ShiftScheduleService.ShiftProductionResult spr : classSprMap.values()) {
                sprIndex++;
                if (spr != null) {
                    CoreScheduleAlgorithmService.DailyEmbryoTask srcTask = spr.getSourceTask();
                    if (srcTask != null) {
                        log.info("  spr[{}] sourceTask: isFirstTask={}, isUrgentEnding={}, isNearEnding={}, isTrialTask={}, isProductionTrial={}",
                                sprIndex, srcTask.getIsFirstTask(), srcTask.getIsUrgentEnding(), srcTask.getIsNearEnding(),
                                srcTask.getIsTrialTask(), srcTask.getIsProductionTrial());
                        if (Boolean.TRUE.equals(srcTask.getIsUrgentEnding())) {
                            isUrgentEnding = true;
                        }
                        if (Boolean.TRUE.equals(srcTask.getIsNearEnding())) {
                            hasNearEnding = true;
                        }
                        if (Boolean.TRUE.equals(srcTask.getIsTrialTask()) || Boolean.TRUE.equals(srcTask.getIsProductionTrial())) {
                            hasTrialOrProductionTrial = true;
                        }
                        if (Boolean.TRUE.equals(srcTask.getIsFirstTask())) {
                            hasFirstTask = true;
                        }
                    }
                }
            }
            if (isUrgentEnding || (result.getCxRemainQty() != null && result.getCxRemainQty().compareTo(BigDecimal.ZERO) <= 0)) {
                result.setMarkCloseOutTip("0");
            } else {
                result.setMarkCloseOutTip("1");
            }

            // ---- 颜色标记（前端展示用） ----
            log.info("设置颜色标记: machineCode={}, embryoCode={}, materialCode={}, hasTrialOrProductionTrial={}, isUrgentEnding={}, hasNearEnding={}, hasFirstTask={}",
                    machineCode, embryoCode, materialCode, hasTrialOrProductionTrial, isUrgentEnding, hasNearEnding, hasFirstTask);
            if (hasTrialOrProductionTrial) {
                result.setColorTag("blue");
            } else if (isUrgentEnding || hasNearEnding) {
                result.setColorTag("orange");
            } else if (hasFirstTask) {
                result.setColorTag("yellow");
            }

            // ---- 示方书类型从SKU与示方书关系获取（constructionStage映射为trialStatus后匹配） ----
            String recipeType = resolveRecipeType(skuRecipeTypeMap, materialCode, constructionStage);

            // ---- 映射班次排量到 CLASS1~8 ----
            for (Map.Entry<String, ShiftScheduleService.ShiftProductionResult> classEntry : classSprMap.entrySet()) {
                setClassFieldValue(result, classEntry.getKey(), classEntry.getValue(), primaryLh, recipeType);
            }

            // ---- 班次未排量的栏位补零 ----
            fillDefaultClassValues(result, classSprMap.keySet());

            results.add(result);
        }

        log.info("最终排程结果（按班次合并）：共 {} 条记录（机台+胎胚+SAP物料维度）", results.size());
        return results;
    }

    /**
     * 按班次排程后构建子表记录
     *
     * <p>核心逻辑：
     * <ul>
     *   <li>维度：机台 + 胎胚 + 车次</li>
     *   <li>8个班次合并到一条记录（CLASS1~CLASS8）</li>
     *   <li>顺位规则：同一胎胚内按库存可供硫化时长从小到大排序</li>
     *   <li>预警规则：库存可供硫化时长 > 18小时（可配置）时预警</li>
     * </ul>
     *
     * <p>公式：胎胚预计库存可供硫化时长 = （胎胚实时库存 + 计划量）/ 硫化机数 / 单台模数
     *
     * @return 分组子表（key=机台编码|胎胚代码, value=该分组下的子表明细列表）
     */
    private Map<String, List<CxScheduleDetail>> buildScheduleDetailsFromShifts(
            ScheduleContextVo context,
            List<ShiftScheduleResult> shiftResults,
            List<CxShiftConfig> allShiftConfigs) {

        if (shiftResults == null || shiftResults.isEmpty()) {
            return Collections.emptyMap();
        }

        // 构建班次配置映射：shiftCode → classField
        Map<String, String> shiftToClassField = new HashMap<>();
        if (allShiftConfigs != null) {
            for (CxShiftConfig cfg : allShiftConfigs) {
                shiftToClassField.put(cfg.getShiftCode(), cfg.getClassField());
            }
        }

        // 硫化结果映射（用于获取硫化消耗）
        Map<Long, LhScheduleResult> lhResultMap = new HashMap<>();
        if (context.getLhScheduleResults() != null) {
            for (LhScheduleResult lh : context.getLhScheduleResults()) {
                if (lh.getId() != null) {
                    lhResultMap.put(lh.getId(), lh);
                }
            }
        }

        // 获取库存预警阈值（默认18小时）
        int stockHoursWarningThreshold = context.getStockHoursWarningThreshold() != null
                ? context.getStockHoursWarningThreshold() : 18;

        // ==================== 第一阶段：按班次合并排产结果后生成子表车次 ====================
        // 与主表逻辑一致：同一机台+胎胚+物料在一个班次内有多条排产结果时，先合并数量再拆车次
        // 库存跟踪器：按 胎胚+物料 维度跟踪累计成型量和硫化消耗（用于stockHours计算）
        Map<String, EmbryoTripTracker> embryoTrackers = new LinkedHashMap<>();
        // 每个班次的车次记录列表（用于后续排序分配顺位和合并）
        List<List<TripRecord>> perShiftTrips = new ArrayList<>();

        for (ShiftScheduleResult shiftResult : shiftResults) {
            int day = shiftResult.getDay();
            String shiftClassField = shiftResult.getShiftConfig() != null
                    ? shiftResult.getShiftConfig().getClassField() : null;
            List<TripRecord> currentShiftTrips = new ArrayList<>();

            // ---- 合并步骤：按 机台+胎胚+物料 汇总当班排产量（同主表merge逻辑）----
            // mergeKey = machineCode|embryoCode|constructionStage（剔除物料编码维度）
            Map<String, ShiftScheduleService.ShiftProductionResult> mergedSprMap = new LinkedHashMap<>();

            for (ShiftScheduleService.ShiftProductionResult spr : shiftResult.getShiftProductionResults()) {
                if (spr.getQuantity() == null || spr.getQuantity() <= 0) continue;

                String mCode = spr.getMachineCode();
                String eCode = spr.getEmbryoCode();
                String constructionStage = spr.getConstructionStage() != null ? spr.getConstructionStage() : "";
                String mergeKey = mCode + "|" + eCode + "|" + constructionStage;

                ShiftScheduleService.ShiftProductionResult existing = mergedSprMap.get(mergeKey);
                if (existing == null) {
                    mergedSprMap.put(mergeKey, spr);
                } else {
                    // 累加数量
                    existing.setQuantity((existing.getQuantity() != null ? existing.getQuantity() : 0)
                            + (spr.getQuantity() != null ? spr.getQuantity() : 0));
                    // OR合并任务标记
                    existing.setIsEndingTask(Boolean.TRUE.equals(existing.getIsEndingTask())
                            || Boolean.TRUE.equals(spr.getIsEndingTask()));
                    existing.setIsTrialTask(Boolean.TRUE.equals(existing.getIsTrialTask())
                            || Boolean.TRUE.equals(spr.getIsTrialTask()));
                    existing.setIsLastEndingBatch(Boolean.TRUE.equals(existing.getIsLastEndingBatch())
                            || Boolean.TRUE.equals(spr.getIsLastEndingBatch()));
                    // 保留有值的tripCapacity
                    if (existing.getTripCapacity() == null && spr.getTripCapacity() != null) {
                        existing.setTripCapacity(spr.getTripCapacity());
                    }
                    // 保留有值的shiftCode
                    if (existing.getShiftCode() == null && spr.getShiftCode() != null) {
                        existing.setShiftCode(spr.getShiftCode());
                    }
                    // 累加 sourceTask 的 vulcanizeMachineCount 和 vulcanizeMoldCount
                    CoreScheduleAlgorithmService.DailyEmbryoTask existingTask = existing.getSourceTask();
                    CoreScheduleAlgorithmService.DailyEmbryoTask sprTask = spr.getSourceTask();
                    if (existingTask != null && sprTask != null) {
                        Integer existingVmc = existingTask.getVulcanizeMachineCount();
                        Integer sprVmc = sprTask.getVulcanizeMachineCount();
                        if (existingVmc != null && sprVmc != null) {
                            existingTask.setVulcanizeMachineCount(existingVmc + sprVmc);
                        }
                        Integer existingVmd = existingTask.getVulcanizeMoldCount();
                        Integer sprVmd = sprTask.getVulcanizeMoldCount();
                        if (existingVmd != null && sprVmd != null) {
                            existingTask.setVulcanizeMoldCount(existingVmd + sprVmd);
                        }
                    }
                }
            }

            // ---- 从合并后的排产结果生成车次 ----
            for (ShiftScheduleService.ShiftProductionResult spr : mergedSprMap.values()) {
                String embryoCode = spr.getEmbryoCode();
                String materialCode = spr.getMaterialCode() != null ? spr.getMaterialCode() : "";
                // embryoKey 用于 tracker 查找，剔除 materialCode 维度（与合并维度一致）
                String constructionStage = spr.getConstructionStage() != null ? spr.getConstructionStage() : "";
                String embryoKey = embryoCode + "|" + constructionStage;

                EmbryoTripTracker tracker = embryoTrackers.computeIfAbsent(embryoKey,
                        k -> new EmbryoTripTracker(embryoCode, materialCode));

                CoreScheduleAlgorithmService.DailyEmbryoTask task = spr.getSourceTask();
                if (task != null) {
                    // 每个班次都从当班合并后的task中获取，覆盖之前的值
                    if (task.getVulcanizeMachineCount() != null) {
                        tracker.setVulcanizeMachineCount(task.getVulcanizeMachineCount());
                    }
                    if (task.getVulcanizeMoldCount() != null) {
                        tracker.setVulcanizeMoldCount(task.getVulcanizeMoldCount());
                    }
                    // 每个班次开始时，更新beginStock为上一班次结束时的库存（currentStock），
                    // 并重置累计值，使stockHours反映当前班次的实时库存水位
                    if (tracker.getBeginStock() != null) {
                        int previousStock = tracker.getCurrentStock();
                        tracker.setBeginStock(previousStock);
                        tracker.setCumulativeForming(0);
                        tracker.setCumulativeVulcanize(0);
                    }
                    if (task.getCurrentStock() != null && tracker.getBeginStock() == null) {
                        tracker.setBeginStock(task.getCurrentStock());
                    }
                    if (task.getHourCapacity() != null && task.getHourCapacity() > 0) {
                        tracker.setHourlyCapacity(task.getHourCapacity());
                    } else {
                        int hourlyCapacity = calculateHourlyCapacity(
                                spr.getMachineCode(), materialCode, task.getStructureName(), context);
                        tracker.setHourlyCapacity(hourlyCapacity);
                    }
                    // 从 materialLhCapacityMap 获取日硫化量（与 TaskGroupService 逻辑一致）
                    if (tracker.getDailyLhCapacity() == null && context.getMaterialLhCapacityMap() != null) {
                        MonthPlanProductLhCapacityVo capacityVo = context.getMaterialLhCapacityMap().get(materialCode);
                        if (capacityVo != null) {
                            if (capacityVo.getDayVulcanizationQty() != null && capacityVo.getDayVulcanizationQty() > 0) {
                                // 日硫化量是双模的，需要除以2得到单模产量
                                tracker.setDailyLhCapacity(capacityVo.getDayVulcanizationQty() / 2);
                            } else if (capacityVo.getStandardCapacity() != null && capacityVo.getStandardCapacity() > 0) {
                                tracker.setDailyLhCapacity(capacityVo.getStandardCapacity());
                            }
                        }
                    }
                }

                int tripCapacity = spr.getTripCapacity() != null ? spr.getTripCapacity() : 12;
                int planQty = spr.getQuantity() != null ? spr.getQuantity() : 0;

                int tripCount = (planQty + tripCapacity - 1) / tripCapacity;

                Long lhId = null;
                LhScheduleResult lhResult = null;
                if (spr.getSourceTask() != null) {
                    lhId = spr.getSourceTask().getLhId();
                    if (lhId != null) {
                        lhResult = lhResultMap.get(lhId);
                    }
                }

                String classField = shiftClassField;
                if (classField == null) {
                    classField = shiftToClassField.getOrDefault(spr.getShiftCode(), spr.getShiftCode());
                }
                int vulcanizeClassIndex = getClassIndex(classField);
                Integer vulcanizeClassConsumptionObj = (lhResult != null)
                        ? getClassPlanQtyByIndex(lhResult, vulcanizeClassIndex) : null;
                int vulcanizeClassConsumption = (vulcanizeClassConsumptionObj != null)
                        ? vulcanizeClassConsumptionObj : 0;

                // 为每个车次创建 TripRecord（车次号从1开始，按机台+胎胚+物料维度独立编号）
                for (int i = 1; i <= tripCount; i++) {
                    int tripPlanQty = Math.min(tripCapacity, planQty - (i - 1) * tripCapacity);

                    // 【先更新成型累计】：stockHours 反映排完本车次之后的库存水位
                    tracker.addFormingProduction(tripPlanQty);

                    // 计算当前车次后的库存可供硫化时长（= 期初 + 包含本车次在内的累计成型 - 硫化累计）
                    int currentStock = tracker.getCurrentStock();
                    double stockHours = calculateStockHours(
                            currentStock, tracker.getVulcanizeMoldCount(),
                            tracker.getDailyLhCapacity());

                    // 计算车次时间
                    LocalDateTime tripStartTime = null;
                    LocalDateTime tripEndTime = null;
                    if (spr.getPlanStartTime() != null && tracker.getHourlyCapacity() > 0) {
                        LocalDateTime shiftStart = spr.getPlanStartTime();
                        int hourlyCapacity = tracker.getHourlyCapacity();

                        int cumulativeBeforeTrip = 0;
                        for (TripRecord existingTrip : currentShiftTrips) {
                            if (existingTrip.getMachineCode().equals(spr.getMachineCode())
                                    && existingTrip.getEmbryoCode().equals(embryoCode)
                                    && existingTrip.getMaterialCode().equals(materialCode)
                                    && existingTrip.getTripNo() < i) {
                                cumulativeBeforeTrip += existingTrip.getPlanQty();
                            }
                        }

                        long minutesBefore = (long) cumulativeBeforeTrip * 60 / hourlyCapacity;
                        long minutesForTrip = (long) tripPlanQty * 60 / hourlyCapacity;

                        tripStartTime = shiftStart.plusMinutes(minutesBefore);
                        tripEndTime = shiftStart.plusMinutes(minutesBefore + minutesForTrip);
                    }

                    TripRecord record = new TripRecord();
                    record.setEmbryoCode(embryoCode);
                    record.setMaterialCode(materialCode);
                    record.setMachineCode(spr.getMachineCode());
                    record.setDay(day);
                    record.setShiftCode(spr.getShiftCode());
                    record.setClassField(classField);
                    record.setTripNo(i);
                    record.setTripCapacity(tripCapacity);
                    record.setPlanQty(tripPlanQty);
                    record.setStockHours(BigDecimal.valueOf(stockHours).setScale(2, RoundingMode.HALF_UP));
                    record.setPlanStartTime(tripStartTime);
                    record.setPlanEndTime(tripEndTime);
                    record.setIsTrialTask(Boolean.TRUE.equals(spr.getIsTrialTask()));
                    record.setIsEndingTask(Boolean.TRUE.equals(spr.getIsEndingTask()));
                    record.setVulcanizeMachineCount(tracker.getVulcanizeMachineCount());

                    currentShiftTrips.add(record);

                    // 最后一个车次才加硫化消耗（影响下一个班次）
                    if (i == tripCount && vulcanizeClassConsumption > 0) {
                        tracker.addVulcanizeConsumption(vulcanizeClassConsumption);
                    }
                }
            }

            perShiftTrips.add(currentShiftTrips);
        }

        // ==================== 第二阶段：每个班次内独立排序分配顺位 ====================
        // 顺位规则：同机台同班次内，所有胎胚车次按库存可供硫化时长从小到大统一排序，每个班次独立从1开始
        for (List<TripRecord> shiftTrips : perShiftTrips) {
            // 按机台分组
            Map<String, List<TripRecord>> byMachine = shiftTrips.stream()
                    .collect(Collectors.groupingBy(TripRecord::getMachineCode, LinkedHashMap::new, Collectors.toList()));

            for (Map.Entry<String, List<TripRecord>> machineEntry : byMachine.entrySet()) {
                List<TripRecord> machineTrips = machineEntry.getValue();

                // 按库存可供硫化时长从小到大排序（所有任务均参与）
                machineTrips.sort(Comparator.comparingDouble(a -> a.getStockHours().doubleValue()));

                // 分配班次内独立顺位
                int sequence = 1;
                for (TripRecord trip : machineTrips) {
                    trip.setSequence(sequence++);

                    // 预警：库存可供硫化时长 > 阈值
                    if (trip.getStockHours().doubleValue() > stockHoursWarningThreshold) {
                        log.warn("胎胚 {} 物料 {} 车次{} 库存可供硫化时长 {}h 超过预警阈值 {}h，库存水位过高！",
                                trip.getEmbryoCode(), trip.getMaterialCode(), trip.getTripNo(),
                                trip.getStockHours(), stockHoursWarningThreshold);
                    }
                }
            }
        }

        // ==================== 第三阶段：按 机台+胎胚+物料+车次号 维度合并8班次到一条记录 ====================
        // key = machineCode|embryoCode，用于关联主表
        Map<String, List<CxScheduleDetail>> resultGroupMap = new LinkedHashMap<>();

        // 合并键：machineCode|embryoCode|tripNo → CxScheduleDetail（剔除物料编码维度���
        Map<String, CxScheduleDetail> mergedDetails = new LinkedHashMap<>();

        for (List<TripRecord> shiftTrips : perShiftTrips) {
            for (TripRecord trip : shiftTrips) {
                String mergeKey = trip.getMachineCode() + "|" + trip.getEmbryoCode()
                        + "|" + trip.getTripNo();

                CxScheduleDetail detail = mergedDetails.computeIfAbsent(mergeKey, k -> {
                    CxScheduleDetail d = new CxScheduleDetail();
                    d.setCxMachineCode(trip.getMachineCode());
                    d.setEmbryoCode(trip.getEmbryoCode());
                    d.setMaterialCode(trip.getMaterialCode());
                    d.setTripNo(String.valueOf(trip.getTripNo()));
                    d.setTripCapacity(BigDecimal.valueOf(trip.getTripCapacity()));
                    return d;
                });

                // 将该车次数据填充到对应班次的CLASS字段
                setDetailClassField(detail, trip.getClassField(), trip);
            }
        }

        // 按机台+胎胚分组（剔除物料编码维度）
        for (CxScheduleDetail detail : mergedDetails.values()) {
            String groupKey = detail.getCxMachineCode() + "|" + detail.getEmbryoCode();
            resultGroupMap.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(detail);
        }

        // 打印前5条验证数据
        int printCount = 0;
        for (Map.Entry<String, List<CxScheduleDetail>> entry : resultGroupMap.entrySet()) {
            for (CxScheduleDetail d : entry.getValue()) {
                if (printCount >= 5) break;
                log.info("子表明细[{}]: groupKey={}, machine={}, embryo={}, material={}, tripNo={}, tripCapacity={}, CLASS1=[PLAN={},HOURS={},SEQ={}], CLASS2=[PLAN={},HOURS={},SEQ={}], CLASS3=[PLAN={},HOURS={},SEQ={}], CLASS4=[PLAN={},HOURS={},SEQ={}], CLASS5=[PLAN={},HOURS={},SEQ={}], CLASS6=[PLAN={},HOURS={},SEQ={}], CLASS7=[PLAN={},HOURS={},SEQ={}], CLASS8=[PLAN={},HOURS={},SEQ={}]",
                        printCount, entry.getKey(), d.getCxMachineCode(), d.getEmbryoCode(), d.getMaterialCode(), d.getTripNo(), d.getTripCapacity(),
                        d.getClass1PlanQty(), d.getClass1StockHours(), d.getClass1Sequence(),
                        d.getClass2PlanQty(), d.getClass2StockHours(), d.getClass2Sequence(),
                        d.getClass3PlanQty(), d.getClass3StockHours(), d.getClass3Sequence(),
                        d.getClass4PlanQty(), d.getClass4StockHours(), d.getClass4Sequence(),
                        d.getClass5PlanQty(), d.getClass5StockHours(), d.getClass5Sequence(),
                        d.getClass6PlanQty(), d.getClass6StockHours(), d.getClass6Sequence(),
                        d.getClass7PlanQty(), d.getClass7StockHours(), d.getClass7Sequence(),
                        d.getClass8PlanQty(), d.getClass8StockHours(), d.getClass8Sequence());
                printCount++;
            }
        }

        return resultGroupMap;
    }

    /**
     * 计算库存可供硫化时长（小时）
     *
     * <p>正确公式：
     * <pre>
     *   单胎单模硫化时长(秒) = 86400 / 日硫化量
     *   库存可供硫化时长(小时) = (当前库存 + 成型累计) × 单胎单模硫化时长 / 3600 / 硫化机数 / 单台模数
     * </pre>
     *
     * <p>其中当前库存 = 期初库存 + 成型累计 - 硫化累计
     *
     * @param currentStock      当前库存（期初库存 + 成型累计 - 硫化累计）
     * @param vulcanizeMoldCount   单台模数
     * @param dailyLhCapacity   日硫化量（单模）
     * @return 库存可供硫化时长（小时）
     */
    private double calculateStockHours(int currentStock, int vulcanizeMoldCount,
                                       Integer dailyLhCapacity) {
        if (vulcanizeMoldCount <= 0 || dailyLhCapacity == null || dailyLhCapacity <= 0) {
            return 0;
        }
        double singleTireMoldSeconds = (double) SECONDS_PER_DAY / dailyLhCapacity;

        return (double) currentStock * singleTireMoldSeconds
                / SECONDS_PER_HOUR / vulcanizeMoldCount;
    }

    /**
     * 内部类：胎胚车次追踪器
     * <p>用于递推计算每个班次开始前的库存
     */
    private static class EmbryoTripTracker {
        private final String embryoCode;
        private final String materialCode;
        private Integer beginStock;  // 期初库存（首次设置后不再变）
        private int currentStock;     // 当前库存（= 期初 + 成型累计 - 硫化累计）
        private int cumulativeForming;     // 成型累计生产
        private int cumulativeVulcanize;   // 硫化累计消耗
        private int vulcanizeMachineCount = 1;
        private int vulcanizeMoldCount = 1;
        private Integer dailyLhCapacity;   // 日硫化量（单模）
        private int hourlyCapacity = 12;   // 小时产能（条/小时）

        EmbryoTripTracker(String embryoCode, String materialCode) {
            this.embryoCode = embryoCode;
            this.materialCode = materialCode;
        }

        void setBeginStock(Integer beginStock) {
            this.beginStock = beginStock;
            this.currentStock = beginStock;
        }

        Integer getBeginStock() { return beginStock; }
        String getEmbryoCode() { return embryoCode; }
        String getMaterialCode() { return materialCode; }
        int getVulcanizeMachineCount() { return vulcanizeMachineCount; }
        void setVulcanizeMachineCount(int count) { this.vulcanizeMachineCount = count; }
        int getVulcanizeMoldCount() { return vulcanizeMoldCount; }
        void setVulcanizeMoldCount(int count) { this.vulcanizeMoldCount = count; }
        Integer getDailyLhCapacity() { return dailyLhCapacity; }
        void setDailyLhCapacity(Integer capacity) { this.dailyLhCapacity = capacity; }

        int getCurrentStock() {
            return (beginStock != null ? beginStock : 0) + cumulativeForming - cumulativeVulcanize;
        }

        int getCumulativeForming() { return cumulativeForming; }
        int getCumulativeVulcanize() { return cumulativeVulcanize; }
        void setCumulativeForming(int val) { this.cumulativeForming = val; }
        void setCumulativeVulcanize(int val) { this.cumulativeVulcanize = val; }
        void addFormingProduction(int qty) { this.cumulativeForming += qty; }
        void addVulcanizeConsumption(int qty) { this.cumulativeVulcanize += qty; }
        int getHourlyCapacity() { return hourlyCapacity; }
        void setHourlyCapacity(int capacity) { this.hourlyCapacity = capacity > 0 ? capacity : 12; }
    }

    /**
     * 设置子表记录的车次字段
     */
    private void setDetailClassField(CxScheduleDetail detail, String classField, TripRecord trip) {
        if (classField == null || trip == null) {
            return;
        }
        switch (classField) {
            case "CLASS1":
                detail.setClass1PlanQty(BigDecimal.valueOf(trip.getPlanQty()));
                detail.setClass1StockHours(trip.getStockHours());
                detail.setClass1Sequence(trip.getSequence());
                detail.setClass1PlanStartTime(trip.getPlanStartTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanStartTime()) : null);
                detail.setClass1PlanEndTime(trip.getPlanEndTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanEndTime()) : null);
                break;
            case "CLASS2":
                detail.setClass2PlanQty(BigDecimal.valueOf(trip.getPlanQty()));
                detail.setClass2StockHours(trip.getStockHours());
                detail.setClass2Sequence(trip.getSequence());
                detail.setClass2PlanStartTime(trip.getPlanStartTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanStartTime()) : null);
                detail.setClass2PlanEndTime(trip.getPlanEndTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanEndTime()) : null);
                break;
            case "CLASS3":
                detail.setClass3PlanQty(BigDecimal.valueOf(trip.getPlanQty()));
                detail.setClass3StockHours(trip.getStockHours());
                detail.setClass3Sequence(trip.getSequence());
                detail.setClass3PlanStartTime(trip.getPlanStartTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanStartTime()) : null);
                detail.setClass3PlanEndTime(trip.getPlanEndTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanEndTime()) : null);
                break;
            case "CLASS4":
                detail.setClass4PlanQty(BigDecimal.valueOf(trip.getPlanQty()));
                detail.setClass4StockHours(trip.getStockHours());
                detail.setClass4Sequence(trip.getSequence());
                detail.setClass4PlanStartTime(trip.getPlanStartTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanStartTime()) : null);
                detail.setClass4PlanEndTime(trip.getPlanEndTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanEndTime()) : null);
                break;
            case "CLASS5":
                detail.setClass5PlanQty(BigDecimal.valueOf(trip.getPlanQty()));
                detail.setClass5StockHours(trip.getStockHours());
                detail.setClass5Sequence(trip.getSequence());
                detail.setClass5PlanStartTime(trip.getPlanStartTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanStartTime()) : null);
                detail.setClass5PlanEndTime(trip.getPlanEndTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanEndTime()) : null);
                break;
            case "CLASS6":
                detail.setClass6PlanQty(BigDecimal.valueOf(trip.getPlanQty()));
                detail.setClass6StockHours(trip.getStockHours());
                detail.setClass6Sequence(trip.getSequence());
                detail.setClass6PlanStartTime(trip.getPlanStartTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanStartTime()) : null);
                detail.setClass6PlanEndTime(trip.getPlanEndTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanEndTime()) : null);
                break;
            case "CLASS7":
                detail.setClass7PlanQty(BigDecimal.valueOf(trip.getPlanQty()));
                detail.setClass7StockHours(trip.getStockHours());
                detail.setClass7Sequence(trip.getSequence());
                detail.setClass7PlanStartTime(trip.getPlanStartTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanStartTime()) : null);
                detail.setClass7PlanEndTime(trip.getPlanEndTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanEndTime()) : null);
                break;
            case "CLASS8":
                detail.setClass8PlanQty(BigDecimal.valueOf(trip.getPlanQty()));
                detail.setClass8StockHours(trip.getStockHours());
                detail.setClass8Sequence(trip.getSequence());
                detail.setClass8PlanStartTime(trip.getPlanStartTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanStartTime()) : null);
                detail.setClass8PlanEndTime(trip.getPlanEndTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanEndTime()) : null);
                break;
            default:
                log.warn("未知的 CLASS_FIELD: {}", classField);
        }
    }

    /**
     * 内部类：车次记录（用于计算顺位）
     */
    private static class TripRecord {
        private String embryoCode;
        private String materialCode;
        private String machineCode;
        private int day;
        private String shiftCode;
        private String classField;
        private int tripNo;
        private int tripCapacity;
        private int planQty;
        private BigDecimal stockHours;
        private LocalDateTime planStartTime;
        private LocalDateTime planEndTime;
        private boolean isTrialTask;
        private boolean isEndingTask;
        private int vulcanizeMachineCount;
        private int sequence;

        // getters and setters
        public String getEmbryoCode() { return embryoCode; }
        public void setEmbryoCode(String embryoCode) { this.embryoCode = embryoCode; }
        public String getMaterialCode() { return materialCode; }
        public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
        public String getMachineCode() { return machineCode; }
        public void setMachineCode(String machineCode) { this.machineCode = machineCode; }
        public int getDay() { return day; }
        public void setDay(int day) { this.day = day; }
        public String getShiftCode() { return shiftCode; }
        public void setShiftCode(String shiftCode) { this.shiftCode = shiftCode; }
        public String getClassField() { return classField; }
        public void setClassField(String classField) { this.classField = classField; }
        public int getTripNo() { return tripNo; }
        public void setTripNo(int tripNo) { this.tripNo = tripNo; }
        public int getTripCapacity() { return tripCapacity; }
        public void setTripCapacity(int tripCapacity) { this.tripCapacity = tripCapacity; }
        public int getPlanQty() { return planQty; }
        public void setPlanQty(int planQty) { this.planQty = planQty; }
        public BigDecimal getStockHours() { return stockHours; }
        public void setStockHours(BigDecimal stockHours) { this.stockHours = stockHours; }
        public LocalDateTime getPlanStartTime() { return planStartTime; }
        public void setPlanStartTime(LocalDateTime planStartTime) { this.planStartTime = planStartTime; }
        public LocalDateTime getPlanEndTime() { return planEndTime; }
        public void setPlanEndTime(LocalDateTime planEndTime) { this.planEndTime = planEndTime; }
        public boolean getIsTrialTask() { return isTrialTask; }
        public void setIsTrialTask(boolean isTrialTask) { this.isTrialTask = isTrialTask; }
        public boolean getIsEndingTask() { return isEndingTask; }
        public void setIsEndingTask(boolean isEndingTask) { this.isEndingTask = isEndingTask; }
        public int getVulcanizeMachineCount() { return vulcanizeMachineCount; }
        public void setVulcanizeMachineCount(int vulcanizeMachineCount) { this.vulcanizeMachineCount = vulcanizeMachineCount; }
        public int getSequence() { return sequence; }
        public void setSequence(int sequence) { this.sequence = sequence; }
    }

    /**
     * 解析示方书类型：将 constructionStage 映射为 trialStatus 后从SKU关系匹配
     *
     * <p>constructionStage → trialStatus 映射：
     * <ul>
     *   <li>01（试制） → X</li>
     *   <li>02（量试） → T</li>
     *   <li>03（正式） → S</li>
     * </ul>
     *
     * <p>降级规则（匹配不到时降级重试）：
     * <ul>
     *   <li>S（正式）：依次尝试 S → T → X</li>
     *   <li>T（量试）：依次尝试 T → X</li>
     *   <li>X（试制）：仅尝试 X</li>
     * </ul>
     *
     * <p>多物料合并（含逗号）时，仅取第一个物料编码匹配。
     *
     * @param skuRecipeTypeMap  SKU关系映射（materialCode|trialStatus → embryoType）
     * @param materialCode      物料编码（多个逗号分隔时仅取第一个）
     * @param constructionStage 施工阶段（01/02/03）
     * @return 示方书类型，匹配不上则返回 null
     */
    private String resolveRecipeType(Map<String, String> skuRecipeTypeMap, String materialCode, String constructionStage) {
        if (materialCode == null || materialCode.isEmpty()) {
            return null;
        }

        // constructionStage → trialStatus 映射
        String trialStatus;
        if ("01".equals(constructionStage)) {
            trialStatus = "X";
        } else if ("02".equals(constructionStage)) {
            trialStatus = "T";
        } else {
            trialStatus = "S";
        }

        // 定义降级顺序
        String[] stagesToTry;
        if ("S".equals(trialStatus)) {
            stagesToTry = new String[]{"S", "T", "X"};
        } else if ("T".equals(trialStatus)) {
            stagesToTry = new String[]{"T", "X"};
        } else {
            stagesToTry = new String[]{"X"};
        }

        // 多物料合并时仅取第一个
        String firstMaterial = materialCode;
        int commaIdx = materialCode.indexOf(',');
        if (commaIdx > 0) {
            firstMaterial = materialCode.substring(0, commaIdx);
        }

        for (String stage : stagesToTry) {
            String skuKey = firstMaterial + "|" + stage;
            String embryoType = skuRecipeTypeMap.get(skuKey);
            if (embryoType != null) {
                log.debug("物料{}施工阶段{}→trialStatus={}匹配示方书类型: {}", firstMaterial, constructionStage, stage, embryoType);
                return embryoType;
            }
        }

        log.debug("物料{}施工阶段{}→trialStatus={}经降级匹配仍未找到，recipeType留空", firstMaterial, constructionStage, trialStatus);
        return null;
    }

    /**
     * 按 CLASS_FIELD 设置对应的班次计划量
     * <p>将 ShiftProductionResult 的计划量字段设置到 CxScheduleResult 的 CLASSn 列：
     * PLAN_QTY、ANALYSIS（原因分析）
     *
     * <p>原因分析标记规则：
     * <ul>
     *   <li>试制任务 → "试制"</li>
     *   <li>收尾任务 → "收尾"</li>
     *   <li>开产任务 → "开产"</li>
     *   <li>停产任务 → "停产"</li>
     *   <li>量试任务 → "量试"</li>
     *   <li>新增任务 → "新增"</li>
     *   <li>多个原因可叠加，如 "试制,收尾"</li>
     * </ul>
     *
     * @param result    排程结果记录
     * @param classField CLASS1~CLASS8 班次字段标识
     * @param spr       班次排产结果
     */
    private void setClassFieldValue(CxScheduleResult result, String classField, ShiftScheduleService.ShiftProductionResult spr,
                                    LhScheduleResult primaryLh, String recipeType) {
        if (classField == null || spr == null) {
            return;
        }

        // 构建原因分析字符串
        String analysis = buildTaskAnalysis(spr);

        // 计划量（无值给0）
        BigDecimal planQty = spr.getQuantity() != null ? new BigDecimal(spr.getQuantity()) : BigDecimal.ZERO;
        // 完成量默认给0
        BigDecimal finishQty = BigDecimal.ZERO;
        // 示方书编号：取硫化任务的制造示方书号
        String recipeNo = (primaryLh != null) ? primaryLh.getEmbryoNo() : null;

        switch (classField) {
            case "CLASS1":
                result.setClass1PlanQty(planQty);
                result.setClass1FinishQty(finishQty);
                result.setClass1RecipeNo(recipeNo);
                result.setClass1RecipeType(recipeType);
                if (analysis != null) { result.setClass1Analysis(analysis); }
                break;
            case "CLASS2":
                result.setClass2PlanQty(planQty);
                result.setClass2FinishQty(finishQty);
                result.setClass2RecipeNo(recipeNo);
                result.setClass2RecipeType(recipeType);
                if (analysis != null) { result.setClass2Analysis(analysis); }
                break;
            case "CLASS3":
                result.setClass3PlanQty(planQty);
                result.setClass3FinishQty(finishQty);
                result.setClass3RecipeNo(recipeNo);
                result.setClass3RecipeType(recipeType);
                if (analysis != null) { result.setClass3Analysis(analysis); }
                break;
            case "CLASS4":
                result.setClass4PlanQty(planQty);
                result.setClass4FinishQty(finishQty);
                result.setClass4RecipeNo(recipeNo);
                result.setClass4RecipeType(recipeType);
                if (analysis != null) { result.setClass4Analysis(analysis); }
                break;
            case "CLASS5":
                result.setClass5PlanQty(planQty);
                result.setClass5FinishQty(finishQty);
                result.setClass5RecipeNo(recipeNo);
                result.setClass5RecipeType(recipeType);
                if (analysis != null) { result.setClass5Analysis(analysis); }
                break;
            case "CLASS6":
                result.setClass6PlanQty(planQty);
                result.setClass6FinishQty(finishQty);
                result.setClass6RecipeNo(recipeNo);
                result.setClass6RecipeType(recipeType);
                if (analysis != null) { result.setClass6Analysis(analysis); }
                break;
            case "CLASS7":
                result.setClass7PlanQty(planQty);
                result.setClass7FinishQty(finishQty);
                result.setClass7RecipeNo(recipeNo);
                result.setClass7RecipeType(recipeType);
                if (analysis != null) { result.setClass7Analysis(analysis); }
                break;
            case "CLASS8":
                result.setClass8PlanQty(planQty);
                result.setClass8FinishQty(finishQty);
                result.setClass8RecipeNo(recipeNo);
                result.setClass8RecipeType(recipeType);
                if (analysis != null) { result.setClass8Analysis(analysis); }
                break;
            default:
                log.warn("未知的 CLASS_FIELD: {}", classField);
        }
    }

    /**
     * 填充未排产班次的默认值（PLAN_QTY=0, FINISH_QTY=0）
     */
    private void fillDefaultClassValues(CxScheduleResult result, Set<String> filledClasses) {
        BigDecimal zero = BigDecimal.ZERO;
        if (!filledClasses.contains("CLASS1")) { result.setClass1PlanQty(zero); result.setClass1FinishQty(zero); }
        if (!filledClasses.contains("CLASS2")) { result.setClass2PlanQty(zero); result.setClass2FinishQty(zero); }
        if (!filledClasses.contains("CLASS3")) { result.setClass3PlanQty(zero); result.setClass3FinishQty(zero); }
        if (!filledClasses.contains("CLASS4")) { result.setClass4PlanQty(zero); result.setClass4FinishQty(zero); }
        if (!filledClasses.contains("CLASS5")) { result.setClass5PlanQty(zero); result.setClass5FinishQty(zero); }
        if (!filledClasses.contains("CLASS6")) { result.setClass6PlanQty(zero); result.setClass6FinishQty(zero); }
        if (!filledClasses.contains("CLASS7")) { result.setClass7PlanQty(zero); result.setClass7FinishQty(zero); }
        if (!filledClasses.contains("CLASS8")) { result.setClass8PlanQty(zero); result.setClass8FinishQty(zero); }
    }

    /**
     * 构建任务原因分析字符串
     * <p>根据任务类型组合原因标记，多个原因用逗号分隔
     */
    private String buildTaskAnalysis(ShiftScheduleService.ShiftProductionResult spr) {
        if (spr == null) {
            return null;
        }

        List<String> reasons = new ArrayList<>();

        // 从 sourceTask 获取详细任务类型
        CoreScheduleAlgorithmService.DailyEmbryoTask task = spr.getSourceTask();
        // 调试日志：打印isLastEndingBatch值
        log.info("buildTaskAnalysis: embryo={}, spr.isLastEndingBatch={}, task.isLastEndingBatch={}",
                spr.getEmbryoCode(), spr.getIsLastEndingBatch(), task != null ? task.getIsLastEndingBatch() : "task is null");
        if (task != null) {
            if (Boolean.TRUE.equals(task.getIsTrialTask())) {
                reasons.add("试制");
            }
            if (Boolean.TRUE.equals(task.getIsProductionTrial())) {
                reasons.add("量试");
            }
            if (Boolean.TRUE.equals(spr.getIsLastEndingBatch())) {
                reasons.add("收尾");
            }
            // 兜底：合并记录的 spr.getIsLastEndingBatch 可能为null，但 sourceTask.getIsEndingTask 可能为true
            if (Boolean.TRUE.equals(task.getIsEndingTask()) && !reasons.contains("收尾")) {
                reasons.add("收尾");
            }
            // 舍弃标记兜底：非主销余量≤2舍弃时，强制标记收尾
            if (Boolean.TRUE.equals(task.getEndingAbandoned()) && !reasons.contains("收尾")) {
                reasons.add("收尾");
            }
            if (Boolean.TRUE.equals(task.getIsOpeningDayTask())) {
                reasons.add("开产");
            }
            if (Boolean.TRUE.equals(task.getIsClosingDayTask())) {
                reasons.add("停产");
            }
            if (Boolean.TRUE.equals(task.getIsEndProduction())) {
                reasons.add("结束生产");
            }
            if (Boolean.TRUE.equals(task.getIsFirstTask()) && !Boolean.TRUE.equals(task.getIsContinueTask())) {
                // 新增任务（非续作的首次任务）
                reasons.add("新增");
            }
            if (Boolean.TRUE.equals(task.getPrecisionDeducted())) {
                reasons.add("精度");
            }
        }

        // 如果 sourceTask 为空，回退到 ShiftProductionResult 的标记
        if (task == null) {
            if (Boolean.TRUE.equals(spr.getIsTrialTask())) {
                reasons.add("试制");
            }
            if (Boolean.TRUE.equals(spr.getIsEndingTask())) {
                reasons.add("收尾");
            }
            if (Boolean.TRUE.equals(spr.getIsContinueTask())) {
                // 续作任务不标记
            }
        }

        if (reasons.isEmpty()) {
            log.info("buildTaskAnalysis: embryo={}, analysis=null (reasons empty)", spr.getEmbryoCode());
            return null;
        }

        String result = String.join(",", reasons);
        log.info("buildTaskAnalysis: embryo={}, analysis={}", spr.getEmbryoCode(), result);
        return result;
    }

    /**
     * 更新一个班次排程后的库存和硫化余量，供下一个班次排程使用
     * <p>逻辑与 updateContextForNextDay 一致，只是按单个班次执行
     *
     * @param context                排程上下文
     * @param shiftAllocations       该班次的机台分配结果
     * @param shiftConfigs           该班次的配置
     * @param currentShiftConfig     当前班次配置（用于确定取哪个 CLASS 字段）
     * @param shiftProductionResults 当前班次的成型排产结果（用于计算成型产出）
     */
    private void updateContextForNextShift(
            ScheduleContextVo context,
            List<MachineAllocationResult> shiftAllocations,
            List<CxShiftConfig> shiftConfigs,
            CxShiftConfig currentShiftConfig,
            List<ShiftScheduleService.ShiftProductionResult> shiftProductionResults) {
        // 直接复用 updateContextForNextDay 逻辑，它已经按班次配置计算硫化消耗
        updateContextForNextDay(context, shiftAllocations, shiftConfigs, currentShiftConfig, shiftProductionResults);
    }

    /**
     * 每天/每班次排程后更新上下文中的库存和硫化余量，供下一天/下一班次排程使用
     *
     * <p>更新逻辑：
     * <ol>
     *   <li>计算当天成型产出（按胎胚编码汇总 ShiftProductionResult.quantity）</li>
     *   <li>计算当天硫化消耗（按胎胚编码汇总，根据当天班次CLASS字段获取硫化计划量）</li>
     *   <li>更新materialStockMap：每条硫化任务的库存 = 原库存 - 硫化消耗 + 比例分配的成型产出</li>
     *   <li>更新monthSurplusMap：硫化余量 -= 当天硫化消耗</li>
     *   <li>重算formingRemainderMap：成型余量 = 硫化余量 - 库存</li>
     * </ol>
     *
     * @param context                排程上下文
     * @param dayAllocations         当天排程结果
     * @param dayShifts              当天班次配置
     * @param currentShiftConfig     当前班次配置（用于确定取哪个 CLASS 字段）
     * @param shiftProductionResults 当前班次的成型排产结果（用于计算成型产出）
     */
    private void updateContextForNextDay(
            ScheduleContextVo context,
            List<MachineAllocationResult> dayAllocations,
            List<CxShiftConfig> dayShifts,
            CxShiftConfig currentShiftConfig,
            List<ShiftScheduleService.ShiftProductionResult> shiftProductionResults) {

        LocalDate scheduleDate = context.getCurrentScheduleDate();
        int currentDay = context.getCurrentScheduleDay();

        // 提取班次名称（如 DAY_D1, NIGHT_N1 等）
        String shiftName = "未知";
        if (dayShifts != null && !dayShifts.isEmpty()) {
            CxShiftConfig firstShift = dayShifts.get(0);
            if (firstShift.getShiftCode() != null) {
                shiftName = firstShift.getShiftCode();
            } else if (firstShift.getShiftName() != null) {
                shiftName = firstShift.getShiftName();
            }
        }

        log.info("\n========== 第 {} 天 - {} 班排程后上下文更新 (日期: {}) ==========",
                currentDay, shiftName, scheduleDate);

        // 1. 计算当天成型产出（按胎胚编码汇总，从 ShiftProductionResult.quantity 获取）
        Map<String, Integer> formingOutputMap = calculateFormingOutputByEmbryo(dayAllocations, context, currentShiftConfig, shiftProductionResults);
        log.info("【步骤1】成型产出汇总（胎胚 → 产出量，来自 ShiftProductionResult）:");
        for (Map.Entry<String, Integer> entry : formingOutputMap.entrySet()) {
            log.info("  - {}: {} 条", entry.getKey(), entry.getValue());
        }

        // 2. 计算当天硫化消耗
        // 2.1 按胎胚编码汇总（用于更新CxStock）
        Map<String, Integer> vulcanizingConsumptionByEmbryo = new HashMap<>();
        Map<Long, Integer> vulcanizingConsumptionByLhId = new HashMap<>();
        calculateVulcanizingConsumption(context.getLhScheduleResults(), dayShifts,
                vulcanizingConsumptionByEmbryo, vulcanizingConsumptionByLhId);
        log.info("【步骤2】硫化消耗汇总（胎胚 → 消耗量）:");
        for (Map.Entry<String, Integer> entry : vulcanizingConsumptionByEmbryo.entrySet()) {
            log.info("  - {}: {} 条", entry.getKey(), entry.getValue());
        }

        // 2.2 按物料编码汇总（用于更新硫化余量）
        Map<String, Integer> vulcanizingConsumptionByMaterial = new HashMap<>();
        calculateVulcanizingConsumptionByMaterial(context.getLhScheduleResults(), dayShifts,
                vulcanizingConsumptionByMaterial);
        log.info("【步骤2】硫化消耗汇总（物料 → 消耗量）:");
        for (Map.Entry<String, Integer> entry : vulcanizingConsumptionByMaterial.entrySet()) {
            log.info("  - {}: {}", entry.getKey(), entry.getValue());
        }

        // 2.5. 先更新 CxStock 实体中的 stockNum（计算新库存 = 原库存 + 成型产出 - 硫化消耗）
        log.info("【步骤2.5】更新胎胚库存表（CxStock），计算新库存...");
        updateCxStockEntities(context, formingOutputMap, vulcanizingConsumptionByEmbryo);

        // 3.5. 收集本班次最后一批=true的物料编码（收尾锁定）
        Set<String> lastBatchMaterials = collectLastBatchMaterials(shiftProductionResults, vulcanizingConsumptionByMaterial);

        // 4. 先更新硫化余量，确保分配库存时使用最新余量（余量<=0时跳过分配）
        log.info("【步骤3】更新硫化余量（monthSurplusMap）...");
        updateMonthSurplus(context, vulcanizingConsumptionByMaterial, lastBatchMaterials);

        // 3. 重新按日硫化量比例分配库存给硫化任务（使用更新后的库存和硫化余量）
        log.info("【步骤4】按日硫化量比例重新分配库存（materialStockMap）...");
        reallocateStockByDayVulcanizationCapacity(context, dayShifts, scheduleDate);

        // 6. 重算 formingRemainderMap（成型余量 = 硫化余量 - 库存）
        log.info("【步骤5】重算成型余量（formingRemainderMap）...");
        recalculateFormingRemainder(context, lastBatchMaterials);

        log.info("========== 第 {} 天 - {} 班上下文更新完成 ==========\n",
                currentDay, shiftName);
    }

    /**
     * 计算当天成型产出，按胎胚编码汇总
     *
     * <p>成型产出 = 从 ShiftProductionResult.quantity 汇总（这是成型机台实际生产的数量）
     *
     * @param dayAllocations         当天机台分配结果（未使用，保留参数兼容性）
     * @param context                排程上下文
     * @param currentShiftConfig     当前班次配置
     * @param shiftProductionResults 当前班次的成型排产结果
     * @return 胎胚编码 → 成型产出量
     */
    private Map<String, Integer> calculateFormingOutputByEmbryo(List<MachineAllocationResult> dayAllocations,
                                                                ScheduleContextVo context,
                                                                CxShiftConfig currentShiftConfig,
                                                                List<ShiftScheduleService.ShiftProductionResult> shiftProductionResults) {
        Map<String, Integer> outputMap = new HashMap<>();

        if (shiftProductionResults == null || shiftProductionResults.isEmpty()) {
            log.warn("【调试】shiftProductionResults 为空，无法计算成型产出");
            return outputMap;
        }

        log.debug("【调试】计算成型产出 - 当前班次={}, shiftProductionResults 数={}",
                currentShiftConfig != null ? currentShiftConfig.getShiftCode() : "未知",
                shiftProductionResults.size());

        // 从 ShiftProductionResult 中汇总成型产出
        for (ShiftScheduleService.ShiftProductionResult spr : shiftProductionResults) {
            String embryoCode = spr.getEmbryoCode();
            Integer qty = spr.getQuantity();

            if (embryoCode != null && qty != null && qty > 0) {
                log.debug("  - 胎胚={}, 物料={}, quantity={}, machineCode={}",
                        embryoCode, spr.getMaterialCode(), qty, spr.getMachineCode());
                outputMap.merge(embryoCode, qty, Integer::sum);
            }
        }

        // 打印汇总统计
        log.info("【调试】成型产出汇总详情（来自 ShiftProductionResult，班次={}）:",
                currentShiftConfig != null ? currentShiftConfig.getShiftCode() : "未知");
        for (Map.Entry<String, Integer> entry : outputMap.entrySet()) {
            log.info("  - {}: {} 条", entry.getKey(), entry.getValue());
        }

        return outputMap;
    }

    /**
     * 计算当天硫化消耗
     *
     * <p>根据当天班次配置的CLASS字段，获取每条硫化记录对应的计划量作为硫化消耗
     *
     * @param lhResults                     硫化排程结果列表
     * @param dayShifts                     当天班次配置
     * @param vulcanizingConsumptionByEmbryo 输出：胎胚编码 → 硫化消耗量
     * @param vulcanizingConsumptionByLhId   输出：硫化任务ID → 硫化消耗量
     */
    private void calculateVulcanizingConsumption(
            List<LhScheduleResult> lhResults,
            List<CxShiftConfig> dayShifts,
            Map<String, Integer> vulcanizingConsumptionByEmbryo,
            Map<Long, Integer> vulcanizingConsumptionByLhId) {

        if (lhResults == null || dayShifts == null || dayShifts.isEmpty()) {
            return;
        }

        for (LhScheduleResult lhResult : lhResults) {
            String embryoCode = lhResult.getEmbryoCode();
            if (embryoCode == null) {
                continue;
            }

            // 获取当天班次对应的硫化计划量
            int consumption = getVulcanizingConsumptionForDay(lhResult, dayShifts);
            if (consumption > 0) {
                vulcanizingConsumptionByEmbryo.merge(embryoCode, consumption, Integer::sum);
                if (lhResult.getId() != null) {
                    vulcanizingConsumptionByLhId.merge(lhResult.getId(), consumption, Integer::sum);
                }
            }
        }
    }

    /**
     * 计算当天硫化消耗，按物料编码汇总
     *
     * @param lhResults                              硫化排程结果列表
     * @param dayShifts                              当天班次配置
     * @param vulcanizingConsumptionByMaterial       输出：物料编码 → 硫化消耗量
     */
    private void calculateVulcanizingConsumptionByMaterial(
            List<LhScheduleResult> lhResults,
            List<CxShiftConfig> dayShifts,
            Map<String, Integer> vulcanizingConsumptionByMaterial) {

        if (lhResults == null || dayShifts == null || dayShifts.isEmpty()) {
            return;
        }

        for (LhScheduleResult lhResult : lhResults) {
            String materialCode = lhResult.getMaterialCode();
            if (materialCode == null) {
                continue;
            }

            // 获取当天班次对应的硫化计划量
            int consumption = getVulcanizingConsumptionForDay(lhResult, dayShifts);
            if (consumption > 0) {
                vulcanizingConsumptionByMaterial.merge(materialCode, consumption, Integer::sum);
            }
        }
    }

    /**
     * 获取硫化记录在指定班次的计划量（即当天的硫化消耗）
     *
     * @param lhResult  硫化记录
     * @param dayShifts 当天班次配置
     * @return 该硫化记录在当天班次的计划量之和
     */
    private int getVulcanizingConsumptionForDay(LhScheduleResult lhResult, List<CxShiftConfig> dayShifts) {
        int total = 0;
        for (CxShiftConfig shiftConfig : dayShifts) {
            String classField = shiftConfig.getClassField();
            if (classField != null && classField.startsWith("CLASS")) {
                try {
                    int classIndex = Integer.parseInt(classField.substring(5));
                    Integer planQty = getClassPlanQtyByIndex(lhResult, classIndex);
                    if (planQty != null && planQty > 0) {
                        total += planQty;
                    }
                } catch (NumberFormatException e) {
                    log.warn("无法解析班次字段: {}", classField);
                }
            }
        }
        return total;
    }

    /**
     * 根据班次字段获取班次索引
     *
     * @param classField 班次字段（如 "CLASS1"）
     * @return 班次索引 (1-8)，解析失败返回 0
     */
    private int getClassIndex(String classField) {
        if (classField != null && classField.startsWith("CLASS")) {
            try {
                return Integer.parseInt(classField.substring(5));
            } catch (NumberFormatException e) {
                log.warn("无法解析班次字段: {}", classField);
            }
        }
        return 0;
    }

    /**
     * 按日硫化量比例重新分配库存给硫化任务
     *
     * <p>流程：
     * <ol>
     *   <li>使用更新后的 CxStock（新库存 = 原库存 + 成型产出 - 硫化消耗）</li>
     *   <li>调用 ScheduleServiceImpl.allocateStockByMaterialRatio 按日硫化量比例分配</li>
     *   <li>更新 context.materialStockMap</li>
     * </ol>
     *
     * @param context        排程上下文
     * @param dayShifts      当天班次配置
     * @param scheduleDate   排程日期
     */
    private void reallocateStockByDayVulcanizationCapacity(
            ScheduleContextVo context,
            List<CxShiftConfig> dayShifts,
            LocalDate scheduleDate) {

        // 获取更新后的库存列表
        List<CxStock> stocks = context.getStocks();
        if (stocks == null || stocks.isEmpty()) {
            log.warn("【步骤3】CxStock 为空，无法重新分配库存");
            return;
        }

        // 获取硫化排程结果
        List<LhScheduleResult> lhScheduleResults = context.getLhScheduleResults();
        if (lhScheduleResults == null || lhScheduleResults.isEmpty()) {
            log.warn("【步骤3】LhScheduleResults 为空，无法重新分配库存");
            return;
        }

        // 获取物料日硫化产能映射
        Map<String, MonthPlanProductLhCapacityVo> materialLhCapacityMap = context.getMaterialLhCapacityMap();
        if (materialLhCapacityMap == null || materialLhCapacityMap.isEmpty()) {
            log.warn("【步骤3】materialLhCapacityMap 为空，无法按日硫化量比例分配");
            return;
        }

        Map<String, Integer> newMaterialStockMap = allocateStockByMaterialRatioSimple(
                stocks, lhScheduleResults, dayShifts, scheduleDate, materialLhCapacityMap,
                context.getMonthSurplusMap());

        // 更新 context
        context.setMaterialStockMap(newMaterialStockMap);
        log.info("【步骤3】materialStockMap 重新分配完成，共 {} 条记录", newMaterialStockMap.size());
    }

    /**
     * 简化的按日硫化量比例分配库存方法
     * （从 ScheduleServiceImpl.allocateStockByMaterialRatio 复制而来）
     *
     * @param monthSurplusMap 月度硫化余量映射（硫化余量<=0的任务跳过分配）
     */
    private Map<String, Integer> allocateStockByMaterialRatioSimple(
            List<CxStock> stocks,
            List<LhScheduleResult> lhScheduleResults,
            List<CxShiftConfig> dayShifts,
            LocalDate scheduleDate,
            Map<String, MonthPlanProductLhCapacityVo> materialLhCapacityMap,
            Map<String, MdmMonthSurplus> monthSurplusMap) {

        Map<String, Integer> materialStockMap = new HashMap<>();

        for (CxStock stock : stocks) {
            String embryoCode = stock.getEmbryoCode();
            if (embryoCode == null) {
                continue;
            }

            int totalStock = stock.getStockNum() != null ? stock.getStockNum() : 0;
            if (totalStock <= 0) {
                continue;
            }

            // 找到该胎胚对应的所有硫化任务
            List<LhScheduleResult> relatedTasks = new ArrayList<>();
            for (LhScheduleResult lh : lhScheduleResults) {
                if (embryoCode.equals(lh.getEmbryoCode())) {
                    relatedTasks.add(lh);
                }
            }

            if (relatedTasks.isEmpty()) {
                log.debug("胎胚 {} 没有对应的硫化任务，跳过", embryoCode);
                continue;
            }

            if (relatedTasks.size() == 1) {
                // 胎胚只对应一个硫化任务，直接分配全部库存
                LhScheduleResult task = relatedTasks.get(0);
                String taskKey = String.valueOf(task.getId());

                // 检查硫化余量：如果已超产（<=0），跳过分配
                if (isVulcanizeSurplusExhausted(task.getMaterialCode(), monthSurplusMap)) {
                    log.debug("胎胚 {} 硫化任务 {} 硫化余量<=0，跳过库存分配", embryoCode, taskKey);
                    continue;
                }

                materialStockMap.merge(taskKey, totalStock, Integer::sum);
                log.debug("胎胚 {} 只对应硫化任务 {}，分配库存 {}", embryoCode, taskKey, totalStock);
            } else {
                // 胎胚对应多个硫化任务，按物料的日硫化量比例分配
                int totalDemand = 0;
                List<TaskDemandSimple> taskDemands = new ArrayList<>();

                for (LhScheduleResult lh : relatedTasks) {
                    String materialCode = lh.getMaterialCode();
                    int dayVulcanizationQty = 0;

                    // 检查硫化余量：如果已超产（<=0），跳过分配
                    if (isVulcanizeSurplusExhausted(materialCode, monthSurplusMap)) {
                        log.debug("胎胚 {} 硫化任务 {} 物料 {} 硫化余量<=0，跳过库存分配",
                                embryoCode, lh.getId(), materialCode);
                        continue;
                    }

                    // 优先用班次计划量来判断当前班次是否排产
                    ShiftPlanResultSimple shiftResult = getShiftPlanQtyWithShiftNameSimple(lh, dayShifts, scheduleDate);
                    if (shiftResult.planQty <= 0) {
                        log.debug("胎胚 {} 硫化任务 {} 当前班次计划量为0，跳过分配", embryoCode, lh.getId());
                        continue;
                    }

                    // 从 materialLhCapacityMap 获取日硫化量（用于比例计算）
                    if (materialLhCapacityMap != null && materialCode != null) {
                        MonthPlanProductLhCapacityVo capacityVo = materialLhCapacityMap.get(materialCode);
                        if (capacityVo != null) {
                            dayVulcanizationQty = capacityVo.getDayVulcanizationQty() != null
                                    ? capacityVo.getDayVulcanizationQty() : 0;
                        }
                    }

                    if (dayVulcanizationQty <= 0) {
                        log.debug("胎胚 {} 硫化任务 {} 日硫化量=0，跳过分配", embryoCode, lh.getId());
                        continue;
                    }

                    taskDemands.add(new TaskDemandSimple(lh.getId(), dayVulcanizationQty, materialCode));
                    totalDemand += dayVulcanizationQty;
                }

                if (taskDemands.isEmpty()) {
                    log.debug("胎胚 {} 所有硫化任务均被过滤，跳过分配", embryoCode);
                    continue;
                }

                if (totalDemand == 0) {
                    // 总需求为0，平均分配
                    int avgStock = totalStock / taskDemands.size();
                    for (TaskDemandSimple td : taskDemands) {
                        materialStockMap.merge(td.taskKey, avgStock, Integer::sum);
                    }
                    log.debug("胎胚 {} 对应多个硫化任务但总日硫化量为0，平均分配库存 {}", embryoCode, avgStock);
                } else {
                    // 按日硫化量比例分配，最后一条用倒扣
                    int allocatedTotal = 0;

                    for (int i = 0; i < taskDemands.size(); i++) {
                        TaskDemandSimple td = taskDemands.get(i);
                        int currentStock;

                        if (i == taskDemands.size() - 1) {
                            // 最后一个硫化任务分配剩余库存（倒扣）
                            currentStock = totalStock - allocatedTotal;
                        } else {
                            // 按日硫化量比例分配
                            currentStock = (int) ((long) totalStock * td.demand / totalDemand);
                        }

                        materialStockMap.merge(td.taskKey, currentStock, Integer::sum);
                        allocatedTotal += currentStock;

                        log.debug("物料编码 {}，胎胚 {} 共用分配：硫化任务 {} 日硫化量 {}，分配库存 {}",
                                td.materialCode, embryoCode, td.taskKey, td.demand, currentStock);
                    }
                }
            }
        }

        return materialStockMap;
    }

    /**
     * 判断物料的硫化余量是否已耗尽（<=0）
     *
     * @param materialCode    物料编码
     * @param monthSurplusMap 月度硫化余量映射
     * @return true 表示硫化余量已耗尽，应跳过库存分配
     */
    private boolean isVulcanizeSurplusExhausted(String materialCode,
                                                Map<String, MdmMonthSurplus> monthSurplusMap) {
        if (materialCode == null || monthSurplusMap == null) {
            return false;
        }
        MdmMonthSurplus monthSurplus = monthSurplusMap.get(materialCode);
        return monthSurplus != null
                && monthSurplus.getPlanSurplusQty() != null
                && monthSurplus.getPlanSurplusQty().compareTo(BigDecimal.ZERO) <= 0;
    }

    /**
     * 硫化任务需求（内部类）
     */
    private static class TaskDemandSimple {
        String taskKey;
        int demand;
        String materialCode;

        TaskDemandSimple(Long lhId, int demand, String materialCode) {
            this.taskKey = String.valueOf(lhId);
            this.demand = demand;
            this.materialCode = materialCode;
        }
    }

    /**
     * 班次计划量查询结果（内部类）
     */
    private static class ShiftPlanResultSimple {
        int planQty;

        ShiftPlanResultSimple(int planQty) {
            this.planQty = planQty;
        }
    }

    /**
     * 获取硫化任务的班次计划量（简化版）
     */
    private ShiftPlanResultSimple getShiftPlanQtyWithShiftNameSimple(
            LhScheduleResult lhResult, List<CxShiftConfig> dayShifts, LocalDate scheduleDate) {
        int defaultQty = lhResult.getDailyPlanQty() != null ? lhResult.getDailyPlanQty() : 0;
        if (dayShifts == null || dayShifts.isEmpty()) {
            return new ShiftPlanResultSimple(defaultQty);
        }

        // 简单返回第一个班次的计划量
        for (CxShiftConfig shiftConfig : dayShifts) {
            String classField = shiftConfig.getClassField();
            if (classField != null && classField.startsWith("CLASS")) {
                try {
                    int classIndex = Integer.parseInt(classField.substring(5));
                    Integer planQty = getClassPlanQtyByIndex(lhResult, classIndex);
                    if (planQty != null && planQty > 0) {
                        return new ShiftPlanResultSimple(planQty);
                    }
                } catch (NumberFormatException e) {
                    log.warn("无法解析班次字段: {}", classField);
                }
            }
        }

        return new ShiftPlanResultSimple(defaultQty);
    }
    /*
     *
     * <p>逻辑：
     * <ol>
     *   <li>对每条硫化任务，减去当天硫化消耗</li>
     *   <li>对每个胎胚编码的成型产出，按各硫化任务当前库存比例分配</li>
     * </ol>
     *
     * @param context                        排程上下文
     * @param formingOutputMap               胎胚编码 → 成型产出量
     * @param vulcanizingConsumptionByEmbryo 胎胚编码 → 硫化消耗量
     * @param vulcanizingConsumptionByLhId   硫化任务ID → 硫化消耗量
     */
    private void updateMaterialStockMap(
            ScheduleContextVo context,
            Map<String, Integer> formingOutputMap,
            Map<String, Integer> vulcanizingConsumptionByEmbryo,
            Map<Long, Integer> vulcanizingConsumptionByLhId) {

        Map<String, Integer> materialStockMap = context.getMaterialStockMap();
        if (materialStockMap == null) {
            materialStockMap = new HashMap<>();
            context.setMaterialStockMap(materialStockMap);
        }

        List<LhScheduleResult> lhResults = context.getLhScheduleResults();

        // Step 1: 减去每条硫化任务的当天硫化消耗
        log.info("  3.1 扣减硫化消耗（按硫化任务lhId）:");
        for (LhScheduleResult lhResult : lhResults) {
            if (lhResult.getId() == null) {
                continue;
            }
            String taskKey = String.valueOf(lhResult.getId());
            Integer consumption = vulcanizingConsumptionByLhId.get(lhResult.getId());
            if (consumption != null && consumption > 0) {
                int currentStock = materialStockMap.getOrDefault(taskKey, 0);
                int newStock = Math.max(0, currentStock - consumption);
                materialStockMap.put(taskKey, newStock);
                log.debug("    - lhId={}, 胎胚={}, 原库存={}, 消耗={}, 新库存={}",
                        taskKey, lhResult.getEmbryoCode(), currentStock, consumption, newStock);
            }
        }

        // Step 2: 按胎胚编码分组，将成型产出按比例分配给各硫化任务
        // 按 embryoCode 分组硫化任务
        Map<String, List<LhScheduleResult>> embryoToLhMap = new HashMap<>();
        for (LhScheduleResult lhResult : lhResults) {
            if (lhResult.getEmbryoCode() != null && lhResult.getId() != null) {
                embryoToLhMap.computeIfAbsent(lhResult.getEmbryoCode(), k -> new ArrayList<>()).add(lhResult);
            }
        }

        log.info("  3.2 分配成型产出（按胎胚 → 硫化任务）:");
        for (Map.Entry<String, Integer> entry : formingOutputMap.entrySet()) {
            String embryoCode = entry.getKey();
            int formingOutput = entry.getValue();
            if (formingOutput <= 0) {
                continue;
            }

            List<LhScheduleResult> relatedTasks = embryoToLhMap.get(embryoCode);
            if (relatedTasks == null || relatedTasks.isEmpty()) {
                log.warn("    - {}: 成型产出={} 条，但未找到对应硫化任务", embryoCode, formingOutput);
                continue;
            }

            // 计算该胎胚下所有硫化任务的总库存（用于按比例分配）
            int totalAllocated = 0;
            for (LhScheduleResult lh : relatedTasks) {
                String taskKey = String.valueOf(lh.getId());
                totalAllocated += materialStockMap.getOrDefault(taskKey, 0);
            }

            if (totalAllocated <= 0) {
                // 所有任务库存为0，平均分配成型产出
                int avgOutput = formingOutput / relatedTasks.size();
                int remaining = formingOutput - avgOutput * relatedTasks.size();
                log.info("    - {}: 产出={} 条，平均分配到 {} 个任务（每任务 {} 条）",
                        embryoCode, formingOutput, relatedTasks.size(), avgOutput);
                for (int i = 0; i < relatedTasks.size(); i++) {
                    String taskKey = String.valueOf(relatedTasks.get(i).getId());
                    int alloc = avgOutput + (i == 0 ? remaining : 0);
                    materialStockMap.merge(taskKey, alloc, Integer::sum);
                    log.debug("      * lhId={}, 分配={}", taskKey, alloc);
                }
            } else {
                // 按库存比例分配成型产出，最后一个任务用倒扣
                log.info("    - {}: 产出={} 条，按库存比例分配到 {} 个任务（总库存={}）",
                        embryoCode, formingOutput, relatedTasks.size(), totalAllocated);
                int allocatedTotal = 0;
                for (int i = 0; i < relatedTasks.size(); i++) {
                    String taskKey = String.valueOf(relatedTasks.get(i).getId());
                    int currentAlloc = materialStockMap.getOrDefault(taskKey, 0);
                    int outputShare;
                    if (i == relatedTasks.size() - 1) {
                        outputShare = formingOutput - allocatedTotal;
                    } else {
                        outputShare = (int) ((long) formingOutput * currentAlloc / totalAllocated);
                        allocatedTotal += outputShare;
                    }
                    materialStockMap.merge(taskKey, outputShare, Integer::sum);
                    log.debug("      * lhId={}, 当前库存={}, 分配={}", taskKey, currentAlloc, outputShare);
                }
            }
        }

        log.info("  materialStockMap 更新完成，共 {} 条记录", materialStockMap.size());
    }

    /**
     * 更新 CxStock 实体中的 stockNum
     *
     * <p>有效库存 = stockNum - overTimeStock - badNum + modifyNum
     * 所以调整库存时直接修改 stockNum 即可
     *
     * <p>如果某个胎胚在 CxStock 中没有记录但有成型产出，会补充创建新的库存记录
     *
     * @param context                        排程上下文
     * @param formingOutputMap               胎胚编码 → 成型产出量
     * @param vulcanizingConsumptionByEmbryo 胎胚编码 → 硫化消耗量
     */
    private void updateCxStockEntities(
            ScheduleContextVo context,
            Map<String, Integer> formingOutputMap,
            Map<String, Integer> vulcanizingConsumptionByEmbryo) {

        List<CxStock> stocks = context.getStocks();
        if (stocks == null) {
            stocks = new ArrayList<>();
            context.setStocks(stocks);
        }

        // 收集所有涉及的胎胚编码
        Set<String> allEmbryoCodes = new HashSet<>();
        allEmbryoCodes.addAll(formingOutputMap.keySet());
        allEmbryoCodes.addAll(vulcanizingConsumptionByEmbryo.keySet());

        // 构建现有库存映射（胎胚编码 → CxStock）
        Map<String, CxStock> existingStockMap = new HashMap<>();
        for (CxStock stock : stocks) {
            if (stock.getEmbryoCode() != null) {
                existingStockMap.put(stock.getEmbryoCode(), stock);
            }
        }

        // 统计变量（用于汇总日志）
        int totalOriginalStock = 0;  // 现有记录更新前库存之和
        int totalNewStock = 0;       // 所有更新记录的新库存之和
        int countUpdated = 0;        // 更新的记录数
        int countNew = 0;            // 新增的记录数
        int countZeroed = 0;         // 新库存归零的记录数
        int countClamped = 0;        // 钳位的记录数
        int clampLossTotal = 0;      // 钳位导致的累计损失（少减的条数）

        // 遍历所有涉及的胎胚编码
        for (String embryoCode : allEmbryoCodes) {
            int formingOutput = formingOutputMap.getOrDefault(embryoCode, 0);
            int vulcanizingConsumption = vulcanizingConsumptionByEmbryo.getOrDefault(embryoCode, 0);
            int delta = formingOutput - vulcanizingConsumption;

            if (delta == 0) {
                continue;
            }

            CxStock stock = existingStockMap.get(embryoCode);
            if (stock != null) {
                // 已有库存记录，直接更新
                int currentStockNum = stock.getStockNum() != null ? stock.getStockNum() : 0;
                int rawNewStockNum = currentStockNum + delta;
                int newStockNum = Math.max(0, rawNewStockNum);
                stock.setStockNum(newStockNum);
                log.info("  - {}: 原库存={}, 成型产出={}, 硫化消耗={}, 净变化={}, 新库存={}",
                        embryoCode, currentStockNum, formingOutput, vulcanizingConsumption, delta, newStockNum);

                // 钳位告警：原库存+净变化 < 0 被截断为0
                if (rawNewStockNum < 0) {
                    countClamped++;
                    int clampLoss = -rawNewStockNum;
                    clampLossTotal += clampLoss;
                    log.warn("  - {}: 库存钳位！原库存{}+净变化{}={} < 0，实际新库存=0，少减{}条",
                            embryoCode, currentStockNum, delta, rawNewStockNum, clampLoss);
                }

                totalOriginalStock += currentStockNum;
                totalNewStock += newStockNum;
                countUpdated++;
                if (newStockNum == 0) {
                    countZeroed++;
                }
            } else {
                // 没有库存记录，但有成型产出或硫化消耗，需要补充创建
                int newStockNum = Math.max(0, delta);  // 新库存 = 成型产出 - 硫化消耗（不能为负）
                int rawNewStockNum = delta;

                // 钳位告警：净变化 < 0 被截断为0
                if (rawNewStockNum < 0) {
                    countClamped++;
                    int clampLoss = -rawNewStockNum;
                    clampLossTotal += clampLoss;
                    log.warn("  - {}: 新增库存记录钳位！净变化={} < 0，实际新库存=0，少减{}条",
                            embryoCode, delta, clampLoss);
                }

                CxStock newStock = new CxStock();
                newStock.setEmbryoCode(embryoCode);
                newStock.setStockNum(newStockNum);
                newStock.setOverTimeStock(0);
                newStock.setBadNum(0);
                newStock.setModifyNum(0);
                newStock.setIsDelete(0);

                // 设置库存日期（使用当前排程日期）
                LocalDate scheduleDate = context.getCurrentScheduleDate();
                if (scheduleDate != null) {
                    newStock.setStockDate(java.sql.Date.valueOf(scheduleDate));
                }

                stocks.add(newStock);
                existingStockMap.put(embryoCode, newStock);

                log.info("  - {}: 【新增库存记录】成型产出={}, 硫化消耗={}, 净变化={}, 新库存={}",
                        embryoCode, formingOutput, vulcanizingConsumption, delta, newStockNum);

                totalNewStock += newStockNum;
                countNew++;
                if (newStockNum == 0) {
                    countZeroed++;
                }
            }
        }

        // 库存更新汇总日志
        int netDelta = totalNewStock - totalOriginalStock;
        log.info("【库存更新汇总】更新{}条(现有{}+新增{})，归零{}条，钳位{}条(少减{}条)，"
                        + "现有记录更新前库存合计={}，更新后库存合计={}，净变化={}，更新后立库总库存={}条",
                countUpdated + countNew, countUpdated, countNew, countZeroed,
                countClamped, clampLossTotal,
                totalOriginalStock, totalNewStock, netDelta,
                stocks.stream().filter(s -> s.getStockNum() != null && s.getStockNum() > 0)
                        .mapToInt(CxStock::getStockNum).sum());
    }

    /**
     * 收集本班次排产结果中最后一批=true的物料编码
     *
     * <p>从 ShiftProductionResult 中提取 isLastEndingBatch=true 的物料，
     * 用于后续硫化余量和成型余量的锁定（直接置0，不参与库存分配）。
     *
     * @param shiftProductionResults          当前班次的成型排产结果
     * @param vulcanizingConsumptionByMaterial 物料 → 硫化消耗量（用于过滤有实际消耗的物料）
     * @return 最后一批=true且本班次有硫化消耗的物料编码集合
     */
    private Set<String> collectLastBatchMaterials(
            List<ShiftScheduleService.ShiftProductionResult> shiftProductionResults,
            Map<String, Integer> vulcanizingConsumptionByMaterial) {
        Set<String> lastBatchMaterials = new HashSet<>();
        if (shiftProductionResults == null || shiftProductionResults.isEmpty()) {
            return lastBatchMaterials;
        }

        for (ShiftScheduleService.ShiftProductionResult spr : shiftProductionResults) {
            if (Boolean.TRUE.equals(spr.getIsLastEndingBatch()) && spr.getMaterialCode() != null) {
                String materialCode = spr.getMaterialCode();
                if (vulcanizingConsumptionByMaterial != null
                        && vulcanizingConsumptionByMaterial.containsKey(materialCode)) {
                    lastBatchMaterials.add(materialCode);
                    log.info("【收尾锁定】物料 {} 本班次为最后一批收尾，成型产出={}，硫化消耗={}",
                            materialCode, spr.getQuantity(),
                            vulcanizingConsumptionByMaterial.get(materialCode));
                }
            }
        }

        return lastBatchMaterials;
    }

    /**
     * 更新 monthSurplusMap（硫化余量 -= 当天硫化消耗）

     *
     * <p>对于最后一批=true的收尾物料，直接设置硫化余量为0（提前锁定量覆盖全部余量），
     * 不再参与后续库存分配，保证收尾物料不会因为共用胎胚库存被分摊而导致余量残留。
     *
     * @param context                             排程上下文
     * @param vulcanizingConsumptionByMaterial    物料编码 → 硫化消耗量
     * @param lastBatchMaterials                  本班次最后一批=true的物料编码集合
     */
    private void updateMonthSurplus(
            ScheduleContextVo context,
            Map<String, Integer> vulcanizingConsumptionByMaterial,
            Set<String> lastBatchMaterials) {

        Map<String, MdmMonthSurplus> monthSurplusMap = context.getMonthSurplusMap();
        if (monthSurplusMap == null || monthSurplusMap.isEmpty()) {
            log.debug("【步骤4】monthSurplusMap 为空，跳过更新");
            return;
        }

        if (vulcanizingConsumptionByMaterial == null || vulcanizingConsumptionByMaterial.isEmpty()) {
            log.debug("【步骤4】vulcanizingConsumptionByMaterial 为空，跳过更新");
            return;
        }

        // 更新硫化余量：每个物料只更新一次
        log.info("【步骤4】硫化消耗按物料汇总详情:");
        for (Map.Entry<String, Integer> entry : vulcanizingConsumptionByMaterial.entrySet()) {
            String materialCode = entry.getKey();
            int consumption = entry.getValue();
            MdmMonthSurplus surplus = monthSurplusMap.get(materialCode);
            if (surplus != null && surplus.getPlanSurplusQty() != null) {
                BigDecimal oldSurplus = surplus.getPlanSurplusQty();

                if (lastBatchMaterials != null && lastBatchMaterials.contains(materialCode)) {
                    surplus.setPlanSurplusQty(BigDecimal.ZERO);
                    log.info("  - {}: 最后一批收尾锁定，原余量={}, 硫化消耗={}, 新余量=0",
                            materialCode, oldSurplus, consumption);
                } else {
                    BigDecimal newSurplus = oldSurplus.subtract(BigDecimal.valueOf(consumption));
                    surplus.setPlanSurplusQty(newSurplus);
                    log.info("  - {}: 原余量={}, 硫化消耗={}, 新余量={}",
                            materialCode, oldSurplus, consumption, newSurplus);
                }
            } else {
                log.warn("  - {}: 未找到硫化余量记录或余量为空，消耗={}", materialCode, consumption);
            }
        }
    }

    /**
     * 计算机台小时产能
     *
     * <p>参考 ShiftScheduleService.getMachineHourlyCapacity：
     * <ol>
     *   <li>从 materialLhCapacityMap 获取该物料的日硫化量</li>
     *   <li>从 structureLhRatioMap 通过 结构+机型 获取配比 (lhMachineMaxQty)</li>
     *   <li>成型一条胎的时间(s) = 86400 / (配比 × 日硫化量)</li>
     *   <li>小时产能 = 3600 / 成型一条胎的时间(s)</li>
     * </ol>
     *
     * @param machineCode   机台编码
     * @param materialCode  物料编码
     * @param structureName 结构名称
     * @param context       排程上下文
     * @return 小时产能（条/小时）
     */
    private int calculateHourlyCapacity(String machineCode, String materialCode,
                                        String structureName, ScheduleContextVo context) {
        // 1. 获取日硫化量
        Integer dailyLhCapacity = null;
        Map<String, MonthPlanProductLhCapacityVo> lhCapacityMap = context.getMaterialLhCapacityMap();
        if (lhCapacityMap != null && materialCode != null) {
            MonthPlanProductLhCapacityVo capacityVo = lhCapacityMap.get(materialCode);
            if (capacityVo != null) {
                dailyLhCapacity = capacityVo.getDayVulcanizationQty();
            }
        }

        // 2. 获取配比
        int ratio = 1;
        if (context.getStructureLhRatioMap() != null && structureName != null && machineCode != null) {
            Map<String, String> machineTypeCodeMap = context.getMachineTypeCodeMap();
            String machineTypeCode = machineTypeCodeMap != null ? machineTypeCodeMap.get(machineCode) : null;
            if (machineTypeCode != null) {
                MdmStructureLhRatio lhRatio = context.getStructureLhRatioMap().get(machineTypeCode + "|" + structureName);
                if (lhRatio != null && lhRatio.getLhMachineMaxQty() != null && lhRatio.getLhMachineMaxQty() > 0) {
                    ratio = lhRatio.getLhMachineMaxQty();
                }
            }
        }

        if (dailyLhCapacity != null && dailyLhCapacity > 0) {
            // 3. 成型一条胎的时间(s) = 86400 / (配比 × 日硫化量)
            BigDecimal timePerTire = BigDecimal.valueOf(86400)
                    .divide(BigDecimal.valueOf((long) ratio * dailyLhCapacity), 2, RoundingMode.HALF_UP);
            // 4. 小时产能 = 3600 / 成型一条胎的时间(s)
            if (timePerTire.compareTo(BigDecimal.ZERO) > 0) {
                return BigDecimal.valueOf(3600)
                        .divide(timePerTire, 0, RoundingMode.FLOOR)
                        .intValue();
            }
        }

        return 12; // 默认值
    }

    /**
     * 重算 formingRemainderMap（成型余量 = 硫化余量 - 库存）
     *
     * <p>对于最后一批=true的收尾物料，成型余量直接设为0（已提前锁定量覆盖全部余量），
     * 确保收尾物料不会因为共用胎胚库存被分摊而导致下个班次仍有余量残留。
     *
     * @param context             排程上下文
     * @param lastBatchMaterials 本班次最后一批=true的物料编码集合
     */
    private void recalculateFormingRemainder(ScheduleContextVo context, Set<String> lastBatchMaterials) {
        Map<String, MdmMonthSurplus> monthSurplusMap = context.getMonthSurplusMap();
        Map<String, Integer> materialStockMap = context.getMaterialStockMap();
        List<LhScheduleResult> lhResults = context.getLhScheduleResults();

        if (monthSurplusMap == null) {
            return;
        }

        // 按物料编码汇总库存（从 materialStockMap 按硫化任务汇总）
        Map<String, Integer> stockByMaterial = new HashMap<>();
        if (lhResults != null && materialStockMap != null) {
            for (LhScheduleResult lh : lhResults) {
                if (lh.getMaterialCode() != null && lh.getId() != null) {
                    String taskKey = String.valueOf(lh.getId());
                    int stock = materialStockMap.getOrDefault(taskKey, 0);
                    stockByMaterial.merge(lh.getMaterialCode(), stock, Integer::sum);
                }
            }
        }

        // 重算成型余量
        Map<String, Integer> newFormingRemainderMap = new HashMap<>();
        log.info("【步骤5】重算成型余量（物料 → 硫化余量 - 库存 = 成型余量）:");
        for (Map.Entry<String, MdmMonthSurplus> entry : monthSurplusMap.entrySet()) {
            String materialCode = entry.getKey();
            MdmMonthSurplus surplus = entry.getValue();

            if (lastBatchMaterials != null && lastBatchMaterials.contains(materialCode)) {
                newFormingRemainderMap.put(materialCode, 0);
                log.info("  - {}: 最后一批收尾锁定，成型余量=0", materialCode);
                continue;
            }

            int vulcanizingRemainder = surplus.getPlanSurplusQty() != null
                    ? surplus.getPlanSurplusQty().intValue() : 0;
            int materialStock = stockByMaterial.getOrDefault(materialCode, 0);
            int formingRemainder = Math.max(0, vulcanizingRemainder - materialStock);
            newFormingRemainderMap.put(materialCode, formingRemainder);
            log.info("  - {}: 硫化余量={}, 库存={}, 成型余量={}",
                    materialCode, vulcanizingRemainder, materialStock, formingRemainder);
        }

        context.setFormingRemainderMap(newFormingRemainderMap);
        log.info("  formingRemainderMap 重算完成，共 {} 条记录", newFormingRemainderMap.size());
    }

    /**
     * 提前检测：当前班次排程后，剩余成型余量在下一班次会被舍弃（非主销+≤2条），
     * 提前在当前班次结果中标识出来，避免下一班次舍弃时数据丢失。
     */
    private void detectEarlyAbandonment(ScheduleContextVo context, ShiftScheduleResult shiftResult) {
        Map<String, Integer> formingRemainderMap = context.getFormingRemainderMap();
        if (formingRemainderMap == null || formingRemainderMap.isEmpty()) {
            return;
        }

        Set<String> mainProductCodes = context.getMainProductCodes();
        int discardThreshold = getEndingDiscardThresholdFromContext(context);

        List<MachineAllocationResult> allAllocations = shiftResult.getAllAllocations();
        if (allAllocations == null || allAllocations.isEmpty()) {
            return;
        }

        List<ShiftScheduleService.ShiftProductionResult> productionResults = shiftResult.getShiftProductionResults();
        if (productionResults == null) {
            return;
        }

        for (Map.Entry<String, Integer> entry : formingRemainderMap.entrySet()) {
            String materialCode = entry.getKey();
            Integer remainder = entry.getValue();
            if (remainder == null || remainder <= 0 || remainder > discardThreshold) {
                continue;
            }
            // 主销产品不舍弃
            if (mainProductCodes != null && mainProductCodes.contains(materialCode)) {
                continue;
            }

            // 在当前班次的分配结果中查找该物料对应的任务
            TaskAllocation foundTask = null;
            String foundMachineCode = null;
            for (MachineAllocationResult ma : allAllocations) {
                if (ma.getTaskAllocations() != null) {
                    for (TaskAllocation ta : ma.getTaskAllocations()) {
                        if (materialCode.equals(ta.getMaterialCode())) {
                            foundTask = ta;
                            foundMachineCode = ma.getMachineCode();
                            break;
                        }
                    }
                }
                if (foundTask != null) {
                    break;
                }
            }

            if (foundTask == null) {
                log.debug("物料 {} 剩余成型余量={}（≤{}且非主销），但本班次未分配此物料，跳过提前标识",
                        materialCode, remainder, discardThreshold);
                continue;
            }

            // 创建占位记录，标识该物料的剩余余量被舍弃
            ShiftScheduleService.ShiftProductionResult spr = new ShiftScheduleService.ShiftProductionResult();
            spr.setMachineCode(foundMachineCode);
            spr.setEmbryoCode(foundTask.getEmbryoCode());
            spr.setMaterialCode(materialCode);
            spr.setMaterialDesc(foundTask.getMaterialDesc());
            spr.setMainMaterialDesc(foundTask.getMainMaterialDesc());
            spr.setStructureName(foundTask.getStructureName());
            spr.setConstructionStage(foundTask.getConstructionStage());
            spr.setQuantity(0);
            spr.setIsEndingTask(true);
            spr.setIsLastEndingBatch(true);

            // 设置 sourceTask 用于 buildTaskAnalysis 构建
            CoreScheduleAlgorithmService.DailyEmbryoTask sourceTask = new CoreScheduleAlgorithmService.DailyEmbryoTask();
            sourceTask.setEmbryoCode(foundTask.getEmbryoCode());
            sourceTask.setMaterialCode(materialCode);
            sourceTask.setIsEndingTask(true);
            sourceTask.setIsLastEndingBatch(true);
            sourceTask.setEndingAbandoned(true);
            sourceTask.setEndingAbandonedQty(remainder);
            spr.setSourceTask(sourceTask);

            productionResults.add(spr);

            // 将成型余量设为0，避免未来班次重复处理
            entry.setValue(0);

            log.info("班次 {} 提前标识舍弃: 物料={}, 胎胚={}, 剩余余量={}条（非主销+≤{}）",
                    shiftResult.getShiftConfig() != null ? shiftResult.getShiftConfig().getShiftCode() : "未知",
                    materialCode, foundTask.getEmbryoCode(), remainder, discardThreshold);
        }
    }

    /**
     * 从参数配置中获取收尾舍弃阈值（默认为2）
     */
    private int getEndingDiscardThresholdFromContext(ScheduleContextVo context) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get("SYS04050001");
            if (config != null && config.getParamValue() != null) {
                try {
                    return Integer.parseInt(config.getParamValue());
                } catch (NumberFormatException e) {
                    log.warn("解析收尾舍弃阈值配置失败: {}", config.getParamValue());
                }
            }
        }
        return 2;
    }

    /**
     * 判断是否是早班
     *
     * <p>精度计划只能安排在早班执行。判断规则：
     * <ul>
     *   <li>班次名称包含"早班"</li>
     *   <li>或班次编码以"DAY_"开头</li>
     *   <li>或班次开始时间在06:00~12:00之间</li>
     * </ul>
     */
    private boolean isMorningShift(CxShiftConfig shiftConfig) {
        if (shiftConfig == null) {
            return false;
        }
        // 规则1：班次名称包含"早班"
        if (shiftConfig.getShiftName() != null && shiftConfig.getShiftName().contains("早班")) {
            return true;
        }
        // 规则2：班次编码以"DAY_"开头（如DAY_D1, DAY_D2, DAY_D3）
        if (shiftConfig.getShiftCode() != null && shiftConfig.getShiftCode().startsWith("DAY_")) {
            return true;
        }
        // 规则3：班次开始时间在06:00~12:00之间
        LocalTime startTime = shiftConfig.getShiftStartTime();
        return !startTime.isBefore(LocalTime.of(6, 0)) && startTime.isBefore(LocalTime.of(12, 0));
    }
}
