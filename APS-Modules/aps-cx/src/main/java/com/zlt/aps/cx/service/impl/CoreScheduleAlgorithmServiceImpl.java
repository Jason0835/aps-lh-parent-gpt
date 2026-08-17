package com.zlt.aps.cx.service.impl;

import com.zlt.aps.common.engine.utils.MonthPlanSurplusCalculator;
import com.zlt.aps.cx.api.domain.entity.CxPrecisionPlan;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.cx.constant.ScheduleConstants;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.config.CxEmbryoLhTime;
import com.zlt.aps.cx.enums.ShiftType;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.mapper.CxEmbryoLhTimeMapper;
import com.zlt.aps.cx.mapper.CxPrecisionPlanMapper;
import com.zlt.aps.cx.mapper.LhScheduleResultMapper;
import com.zlt.aps.cx.mapper.MdmMaterialInfoMapper;
import com.zlt.aps.cx.mapper.MdmSkuConstructionRefMapper;
import com.zlt.aps.cx.service.engine.*;
import com.zlt.aps.cx.vo.DailyEmbryoTask;
import com.zlt.aps.cx.vo.MachineAllocationResult;
import com.zlt.aps.cx.vo.MonthPlanProductLhCapacityVo;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.cx.vo.ShiftScheduleResult;
import com.zlt.aps.cx.vo.ShiftProductionResult;
import com.zlt.aps.cx.vo.TaskAllocation;
import com.zlt.aps.cx.vo.TaskAllocationR;
import com.zlt.aps.cx.vo.TaskDemandSimple;
import com.zlt.aps.cx.vo.TaskGroupResultVo;
import com.zlt.aps.cx.vo.TripRecord;
import com.zlt.aps.cx.vo.MachineAgg;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.mp.api.domain.entity.MdmMonthSurplus;
import com.zlt.aps.mp.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.aps.mp.api.domain.entity.MpCxCapacityConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
 * 核心排程算法编排实现 — S5.2～S5.5，在 {@link ScheduleServiceImpl} 构建上下文之后执行。
 *
 * <h3>职责</h3>
 * <ul>
 *   <li>按<b>班次</b>循环驱动 engine 层（TaskGroupService → 三类 Processor → ShiftScheduleService）。</li>
 *   <li>班次间滚动更新 {@link ScheduleContextVo}（库存、硫化/成型余量、在机胎胚、materialStockMap）。</li>
 *   <li>全部班次结束后汇总 {@link CxScheduleResult} 主表 + {@link CxScheduleDetail} 子表。</li>
 * </ul>
 *
 * <h3>主流程（{@link #executeSchedule}）</h3>
 * <pre>
 * 2.0  试制约束参数（context 已加载）
 * 2.1  ScheduleDayTypeHelper.preloadCache
 * 2.2  排序班次配置（scheduleDay → dayShiftOrder，约 8 班/3 天）
 * 2.3  初始化 machineOnlineEmbryoMap（每班结束后滚动）
 * 2.4  【核心循环】逐班次：
 *        2.4.1 停产整天/停产班 → skip
 *        2.4.2 写入 currentScheduleDay/Date/ShiftConfigs
 *        2.4.3 跨天重置：试制 SKU 日计数、精度计划 applied 标记
 *        2.4.4 executeShiftSchedule（5.2～5.3.7）
 *        2.4.5 updateMachineOnlineStatus
 *        2.4.6 updateContextForNextShift（库存/余量滚动）
 *        2.4.7 detectEarlyAbandonment
 * 2.5  buildFinalScheduleResultsFromShifts  → CLASS1~8 主表
 * 2.6  balanceShiftQuantities              → 日级班次量均衡
 * 2.7  buildScheduleDetailsFromShifts      → 子表
 * 2.8  associateDetailsToResults
 * </pre>
 *
 * <h3>单班次流水线（{@link #executeShiftSchedule}）</h3>
 * <pre>
 * 5.2   TaskGroupService.groupTasks
 * 5.2.1 applyDailyTrialSkuLimit（SKU 上限 / 周日）
 * 5.3.1 ContinueTaskProcessor
 * 5.3.2 TrialTaskProcessor
 * 5.3.3 NewTaskProcessor + BalancingService
 * 5.3.4 合并 continue + new + trial
 * 5.3.5 applyPrecisionPlanSelection
 * 5.3.7 ShiftScheduleService.scheduleTaskToShifts（TaskAllocation → DailyEmbryoTask 反构）
 * </pre>
 *
 * <h3>日期换算</h3>
 * <p>前端 {@code scheduleDate} 为「中间天」；班次实际日期由 {@link ScheduleDayTypeHelper#calculateShiftDate} 计算。
 *
 * @author APS Team
 * @see ScheduleServiceImpl#executeSchedule
 * @see CoreScheduleAlgorithmService
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
    private final CxPrecisionPlanMapper precisionPlanMapper;
    private final LhScheduleResultMapper lhScheduleResultMapper;
    private final MdmMaterialInfoMapper materialInfoMapper;
    private final MdmSkuConstructionRefMapper skuConstructionRefMapper;
    private final CxEmbryoLhTimeMapper embryoLhTimeMapper;

    /** 构造函数注入 */
    @Autowired
    public CoreScheduleAlgorithmServiceImpl(
            @Lazy ContinueTaskProcessor continueTaskProcessor,
            @Lazy TrialTaskProcessor trialTaskProcessor,
            @Lazy NewTaskProcessor newTaskProcessor,
            @Lazy ShiftScheduleService shiftScheduleService,
            @Lazy ProductionCalculator productionCalculator,
            ScheduleDayTypeHelper scheduleDayTypeHelper,
            CxPrecisionPlanMapper precisionPlanMapper,
            LhScheduleResultMapper lhScheduleResultMapper,
            MdmMaterialInfoMapper materialInfoMapper,
            MdmSkuConstructionRefMapper skuConstructionRefMapper,
            CxEmbryoLhTimeMapper embryoLhTimeMapper) {
        this.continueTaskProcessor = continueTaskProcessor;
        this.trialTaskProcessor = trialTaskProcessor;
        this.newTaskProcessor = newTaskProcessor;
        this.shiftScheduleService = shiftScheduleService;
        this.productionCalculator = productionCalculator;
        this.scheduleDayTypeHelper = scheduleDayTypeHelper;
        this.precisionPlanMapper = precisionPlanMapper;
        this.lhScheduleResultMapper = lhScheduleResultMapper;
        this.materialInfoMapper = materialInfoMapper;
        this.skuConstructionRefMapper = skuConstructionRefMapper;
        this.embryoLhTimeMapper = embryoLhTimeMapper;
    }

    private static final int DEFAULT_MAX_TRIAL_SKU_PER_DAY = 2;

    /** 机台班初准备时间（分钟） */
    private static final int DEFAULT_MACHINE_PREPARE_MINUTES = 30;

    /** 单班次物理产能（秒）：8小时班次 */
    private static final int SECONDS_PER_SHIFT = 8 * 60 * 60;

    /** 可供硫化时长硬上限参数编码（SYS04080001，默认6小时，R3事前预估超限移出） */
    private static final String PARAM_STOCK_HOURS_CAP = "SYS04080001";

    /** 可供硫化时长软退出阈值参数编码（SYS04080003，未配置回退硬上限，R2事前预估超限退出） */
    private static final String PARAM_STOCK_HOURS_SOFT_TRIGGER = "SYS04080003";

    /** 可供硫化时长管控开关参数编码（SYS04080005，Y=开启，默认开启） */
    private static final String PARAM_STOCK_HOURS_CAP_ENABLED = "SYS04080005";

    /**
     * 执行完整成型排程（实现 {@link CoreScheduleAlgorithmService#executeSchedule}）。
     *
     * <p>前置条件：{@code context} 已由 {@link ScheduleServiceImpl#executeSchedule} 内构建并传入。
     * 返回主表列表（含关联子表），由 ScheduleServiceImpl 持久化。
     */
    @Override
    public List<CxScheduleResult> executeSchedule(ScheduleContextVo context) {
        log.info("开始执行排程算法，日期: {}", context.getScheduleDate());

        // 1.1 预加载工作日历缓存（ScheduleDayTypeHelper，供开停产/停产跳过判定）
        LocalDate scheduleDate = context.getScheduleDate();
        String factoryCode = context.getFactoryCode();
        int scheduleDays = context.getScheduleDays() != null ? context.getScheduleDays() : ScheduleConstants.DEFAULT_SCHEDULE_DAYS;
        if (scheduleDate != null) {
            scheduleDayTypeHelper.preloadCache(scheduleDate, scheduleDate.plusDays(scheduleDays - 1), factoryCode);
        }

        // 1.2 获取并排序班次配置（scheduleDay → dayShiftOrder，共约8班次/3天）
        List<CxShiftConfig> allShiftConfigs = context.getShiftConfigList();
        if (allShiftConfigs == null || allShiftConfigs.isEmpty()) {
            log.error("班次配置为空，请先调用 buildScheduleContext 加载班次配置");
            return new ArrayList<>();
        }
        List<CxShiftConfig> sortedShiftConfigs = allShiftConfigs.stream()
                .filter(c -> c.getScheduleDay() != null)
                .sorted(Comparator.comparingInt(CxShiftConfig::getScheduleDay)
                        .thenComparingInt(c -> c.getDayShiftOrder() != null ? c.getDayShiftOrder() : 0))
                .collect(Collectors.toList());

        // 1.3 跨班次状态：机台在产胎胚映射（每班次结束后滚动替换，见 2.4.4）
        Map<String, Set<String>> machineOnlineEmbryoMap = context.getMachineOnlineEmbryoMap();
        if (machineOnlineEmbryoMap == null) {
            machineOnlineEmbryoMap = new HashMap<>();
        }

        // 1.3.1 收集每个班次的排产结果（全部班次完成后用于汇总主表/子表）
        List<ShiftScheduleResult> shiftResults = new ArrayList<>();

        // 1.3.2 保存初始胎胚库存快照（排程循环中 updateCxStockEntities 会修改 context.getStocks()，
        //        totalStock 字段需要使用排程前的初始库存，而非排程后的剩余库存）
        Map<String, Integer> initialEmbryoStockMap = new HashMap<>();
        if (context.getStocks() != null) {
            for (CxStock stock : context.getStocks()) {
                if (stock.getEmbryoCode() != null) {
                    initialEmbryoStockMap.merge(stock.getEmbryoCode(), stock.getEffectiveStock(), Integer::sum);
                }
            }
        }

        int lastDay = 0;

        // 2.4 按班次逐个执行排程（核心循环，顺序=班次1→8，不可并行）
        int shiftIndex = 0;
        int totalShifts = sortedShiftConfigs.size();
        for (CxShiftConfig shiftConfig : sortedShiftConfigs) {
            shiftIndex++;
            int day = shiftConfig.getScheduleDay();
            LocalDate currentScheduleDate = scheduleDayTypeHelper.calculateShiftDate(context.getScheduleDate(), shiftConfig);

            // 2.4.1 停产跳过：整天停产或当前班次停产
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
            int historyCount = machineOnlineEmbryoMap.size();
            log.info("【班次开始】#{}/{} | 日期:{} | 班次:{} | 历史胎胚数量:{}",
                    shiftIndex, totalShifts, currentScheduleDate, shiftConfig.getShiftCode(), historyCount);

            // 2.4.2 设置当前班次上下文（TaskGroupService 等 engine 服务通过 context 读取）
            List<CxShiftConfig> singleShiftList = Collections.singletonList(shiftConfig);
            context.setCurrentScheduleDay(day);
            context.setCurrentScheduleDate(currentScheduleDate);
            context.setCurrentShiftConfigs(singleShiftList);

            // 2.4.3 跨天重置：试制SKU日计数 + 精度计划应用标记
            if (day != lastDay) {
                context.setDailyTrialAssignedMaterialCodes(new HashSet<>());
                context.setPrecisionPlanApplied(false);
            }

            // 2.4.4 执行单班次排程（S5.2~S5.3.7，见 executeShiftSchedule）
            ShiftScheduleResult shiftResult = executeShiftSchedule(
                    context, day, shiftConfig, currentScheduleDate, machineOnlineEmbryoMap, shiftResults);
            // 保存当前班次排程前的 materialStockMap 快照（在 updateContextForNextShift 之前）
            // 用于子表 stockHours 计算：每个班次独立使用当时的分配库存
            Map<String, Integer> stockMap = context.getMaterialStockMap();
            shiftResult.setMaterialStockSnapshot(stockMap != null ? new HashMap<>(stockMap) : new HashMap<>());
            shiftResults.add(shiftResult);

            // 2.4.5 更新机台在产状态（本班次分配结果滚动替换，供下一班次续作判定）
            machineOnlineEmbryoMap = updateMachineOnlineStatus(
                    shiftResult.getAllAllocations(), machineOnlineEmbryoMap);

            // 将更新后的机台在产状态存回 context，供下一个班次使用
            context.setMachineOnlineEmbryoMap(new HashMap<>(machineOnlineEmbryoMap));

            // 更新前序班次负荷映射（供下一班次动态保底预留）
            updatePreviousShiftMachineEmbryoLoadMap(context, shiftResult.getAllAllocations());

            // 2.4.6 更新库存/硫化余量/成型余量，供下一班次 TaskGroupService 使用
            updateContextForNextShift(context, shiftResult.getAllAllocations(), singleShiftList, shiftConfig, shiftResult.getShiftProductionResults());

            // 2.4.7 提前检测收尾舍弃（非主销余量≤2 将在下班次被舍弃，本班次提前标识）
            detectEarlyAbandonment(context, shiftResult);

            lastDay = day;
        }

        // 2.4.8 跨班次总产量均衡：确保同一机台在同一天各班次的总产量差异不超过1车
        balanceCrossShiftQuantity(shiftResults, context);

        // 2.5 汇总多班次结果：机台+胎胚+物料 -> CLASS1~8 主表记录
        List<CxScheduleResult> allResults = buildFinalScheduleResultsFromShifts(context, shiftResults, allShiftConfigs, initialEmbryoStockMap);

        // 2.6 处理结构切换时的最早可供硫化时间
        processStructureSwitchLhTime(context, shiftResults, sortedShiftConfigs);

        // 2.7 构建子表（机台+胎胚+车次维度，含库存可供硫化时长和顺位）
        Map<String, List<CxScheduleDetail>> detailGroupMap = buildScheduleDetailsFromShifts(context, shiftResults, allShiftConfigs);
        int totalDetails = detailGroupMap.values().stream().mapToInt(List::size).sum();
        log.info("子表记录构建完成，共 {} 条（按机台+胎胚分组 {} 组）", totalDetails, detailGroupMap.size());

        // 2.8 子表关联到主表（匹配规则：机台编码 + 胎胚代码）
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

    // ==================== 2.6 结构切换最早可供硫化时间 ====================

    /**
     * 2.6 处理结构切换时的最早可供硫化时间。
     *
     * <p>遍历排程结果中所有结构，按"全部收尾"判定分两种场景：
     * <ul>
     *   <li><b>场景1（全部收尾）</b>：结构在所有机台上的末班记录均 isLastEndingBatch=true，
     *       取所有机台中最晚的 planEndTime 作为前结构结束时间，加切换耗时得到最早可供硫化时间。</li>
     *   <li><b>场景2（未全部收尾）</b>：结构在部分机台未收尾，按总生产量/3估算日消耗，
     *       用初始成型余量/日消耗判断消耗班次，取该班次结束时间加切换耗时。</li>
     * </ul>
     *
     * <p>结果写入 T_CX_EMBRYO_LH_TIME 表，先按 factoryCode 删除后插入。
     *
     * @param context           排程上下文
     * @param shiftResults      全部班次排程结果
     * @param sortedShiftConfigs 已排序的班次配置（scheduleDay -> dayShiftOrder）
     */
    private void processStructureSwitchLhTime(ScheduleContextVo context,
                                              List<ShiftScheduleResult> shiftResults,
                                              List<CxShiftConfig> sortedShiftConfigs) {
        String factoryCode = context.getFactoryCode();

        // 1. 构建物料映射
        Map<String, MdmMaterialInfo> materialByEmbryoMap = new HashMap<>();
        Map<String, MdmMaterialInfo> materialByCodeMap = new HashMap<>();
        Map<String, String> materialToStructureMap = new HashMap<>();
        if (context.getMaterials() != null) {
            for (MdmMaterialInfo material : context.getMaterials()) {
                if (material.getMaterialCode() != null) {
                    materialByCodeMap.putIfAbsent(material.getMaterialCode(), material);
                    if (material.getStructureName() != null) {
                        materialToStructureMap.putIfAbsent(material.getMaterialCode(), material.getStructureName());
                    }
                }
                if (material.getEmbryoCode() != null) {
                    materialByEmbryoMap.putIfAbsent(material.getEmbryoCode(), material);
                }
            }
        }

        // 2. 构建结构 -> 初始成型余量映射
        Map<String, Integer> structureRemainderMap = buildStructureFormingRemainderMap(context, materialToStructureMap);

        // 3. 构建机台 -> 未来结构反查映射
        Map<String, List<MpCxCapacityConfiguration>> machineFutureStructureMap = buildMachineFutureStructureMap(context);

        // 4. 读取切换耗时参数
        int sameInchHours = getIntParamValue(context, ScheduleConstants.PARAM_SAME_INCH_SWITCH_HOURS, ScheduleConstants.DEFAULT_SAME_INCH_SWITCH_HOURS);
        int diffInchHours = getIntParamValue(context, ScheduleConstants.PARAM_DIFF_INCH_SWITCH_HOURS, ScheduleConstants.DEFAULT_DIFF_INCH_SWITCH_HOURS);

        // 5. 收集所有 ShiftProductionResult，按结构分组
        // structureName -> machineCode -> List<SPR>（按班次顺序）
        Map<String, Map<String, List<ShiftProductionResult>>> structureMachineSprMap = new LinkedHashMap<>();
        // 同时构建机台级别的结构时间线：machineCode -> 按班次顺序出现的结构列表（去重连续相同）
        Map<String, List<String>> machineStructureTimelineMap = new LinkedHashMap<>();
        for (ShiftScheduleResult sr : shiftResults) {
            if (sr.getShiftProductionResults() == null) continue;
            for (ShiftProductionResult spr : sr.getShiftProductionResults()) {
                if (spr.getQuantity() == null || spr.getQuantity() <= 0) continue;
                String structName = spr.getStructureName();
                String mCode = spr.getMachineCode();
                if (structName == null || mCode == null) continue;
                structureMachineSprMap
                        .computeIfAbsent(structName, k -> new LinkedHashMap<>())
                        .computeIfAbsent(mCode, k -> new ArrayList<>())
                        .add(spr);
                // 构建机台结构时间线（连续相同结构只记录一次）
                List<String> timeline = machineStructureTimelineMap.computeIfAbsent(mCode, k -> new ArrayList<>());
                if (timeline.isEmpty() || !timeline.get(timeline.size() - 1).equals(structName)) {
                    timeline.add(structName);
                }
            }
        }

        log.info("结构切换分析：共 {} 个结构有排产记录", structureMachineSprMap.size());

        // 6. 遍历每个结构，判定场景并计算最早可供硫化时间
        List<CxEmbryoLhTime> records = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<ShiftProductionResult>>> entry : structureMachineSprMap.entrySet()) {
            String structureName = entry.getKey();
            Map<String, List<ShiftProductionResult>> machineSprMap = entry.getValue();

            // 判定是否所有机台都收尾
            boolean allFullyEnded = checkAllMachinesFullyEnded(machineSprMap);

            // 前结构初始成型余量
            Integer structureRemainder = structureRemainderMap.getOrDefault(structureName, 0);

            if (allFullyEnded) {
                // 场景1：全部收尾 -> 结构切换
                CxEmbryoLhTime record = processScenario1(structureName, machineSprMap, context,
                        materialByEmbryoMap, materialByCodeMap, machineFutureStructureMap,
                        machineStructureTimelineMap,
                        sameInchHours, diffInchHours, structureRemainder, factoryCode);
                if (record != null) {
                    records.add(record);
                }
            } else {
                // 场景2：未全部收尾 -> 估算消耗
                CxEmbryoLhTime record = processScenario2(structureName, machineSprMap, context,
                        materialByEmbryoMap, materialByCodeMap, machineFutureStructureMap,
                        sameInchHours, diffInchHours, structureRemainder, factoryCode,
                        sortedShiftConfigs, shiftResults);
                if (record != null) {
                    records.add(record);
                }
            }
        }

        // 7. 仅删除本次有新记录的结构（按 factoryCode + scheduleDate + structure 维度），
        //    返回 null 的结构不进删除范围，保留旧值；其他排程日期的记录也不受影响
        java.sql.Timestamp scheduleDateTs = java.sql.Timestamp.valueOf(context.getScheduleDate().atStartOfDay());
        if (!records.isEmpty()) {
            List<String> processedStructures = records.stream()
                    .map(CxEmbryoLhTime::getStructureName)
                    .collect(Collectors.toList());
            embryoLhTimeMapper.delete(new LambdaQueryWrapper<CxEmbryoLhTime>()
                    .eq(CxEmbryoLhTime::getFactoryCode, factoryCode)
                    .eq(CxEmbryoLhTime::getScheduleDate, scheduleDateTs)
                    .in(CxEmbryoLhTime::getStructureName, processedStructures));
        }
        int skipCount = structureMachineSprMap.size() - records.size();
        for (CxEmbryoLhTime record : records) {
            record.setFactoryCode(factoryCode);
            record.setScheduleDate(scheduleDateTs);
            record.setCreateTime(new Date());
            embryoLhTimeMapper.insert(record);
        }
        log.info("结构切换最早可供硫化时间处理完成，共 {} 条记录（跳过 {} 个 null 结构，保留旧值）",
                records.size(), skipCount);
    }

    /**
     * 检查结构在所有机台上是否都已收尾。
     * <p>判定条件：每个机台该结构下每个胎胚的最后一条有产量记录的 isLastEndingBatch=true。
     *
     * @param machineSprMap 机台->SPR列表映射
     * @return 所有机台所有胎胚都收尾返回true
     */
    private boolean checkAllMachinesFullyEnded(Map<String, List<ShiftProductionResult>> machineSprMap) {
        for (Map.Entry<String, List<ShiftProductionResult>> machineEntry : machineSprMap.entrySet()) {
            List<ShiftProductionResult> sprList = machineEntry.getValue();
            if (sprList.isEmpty()) continue;

            // 按胎胚分组，每个胎胚取最后一条记录检查 isLastEndingBatch
            Map<String, ShiftProductionResult> embryoLastSprMap = new LinkedHashMap<>();
            for (ShiftProductionResult spr : sprList) {
                String embryoCode = spr.getEmbryoCode();
                if (embryoCode != null) {
                    embryoLastSprMap.put(embryoCode, spr); // 后出现的覆盖前面的，保留最后一条
                }
            }
            for (ShiftProductionResult lastSpr : embryoLastSprMap.values()) {
                if (!Boolean.TRUE.equals(lastSpr.getIsLastEndingBatch())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 场景1：全部收尾 -> 结构切换。
     * <p>取所有机台中最晚的 planEndTime 作为前结构结束时间，
     * 查找后结构并计算切换耗时，earliestLhTime = 结束时间 + 切换耗时。
     */
    private CxEmbryoLhTime processScenario1(
            String structureName,
            Map<String, List<ShiftProductionResult>> machineSprMap,
            ScheduleContextVo context,
            Map<String, MdmMaterialInfo> materialByEmbryoMap,
            Map<String, MdmMaterialInfo> materialByCodeMap,
            Map<String, List<MpCxCapacityConfiguration>> machineFutureStructureMap,
            Map<String, List<String>> machineStructureTimelineMap,
            int sameInchHours, int diffInchHours,
            Integer structureRemainder, String factoryCode) {

        // 1. 计算每个机台前结构的结束时间（班次开始时间 + 该班次内该结构的总生产耗时）
        // 取所有机台中最晚的结束时间
        LocalDateTime latestEndTime = null;
        String latestMachineCode = null;
        for (Map.Entry<String, List<ShiftProductionResult>> machineEntry : machineSprMap.entrySet()) {
            String machineCode = machineEntry.getKey();
            List<ShiftProductionResult> sprList = machineEntry.getValue();

            // 按 planStartTime 分组（同一 planStartTime = 同一班次），找到最后一个班次
            LocalDateTime lastShiftPlanStart = null;
            for (ShiftProductionResult spr : sprList) {
                if (spr.getPlanStartTime() == null) continue;
                if (lastShiftPlanStart == null || spr.getPlanStartTime().isAfter(lastShiftPlanStart)) {
                    lastShiftPlanStart = spr.getPlanStartTime();
                }
            }
            if (lastShiftPlanStart == null) continue;

            // 汇总最后一个班次中该结构的所有 SPR 生产耗时
            long totalProductionSeconds = 0;
            int sprCount = 0;
            for (ShiftProductionResult spr : sprList) {
                if (spr.getPlanStartTime() != null && spr.getPlanStartTime().equals(lastShiftPlanStart)
                        && spr.getPlanEndTime() != null) {
                    totalProductionSeconds += java.time.Duration.between(
                            spr.getPlanStartTime(), spr.getPlanEndTime()).getSeconds();
                    sprCount++;
                }
            }

            // 班次开始时间 = planStartTime（已无准备时间偏移）
            LocalDateTime shiftStart = lastShiftPlanStart;
            // 结束时间 = 班次开始时间 + 总生产耗时
            LocalDateTime machineEndTime = shiftStart.plusSeconds(totalProductionSeconds);

            log.info("场景1：机台={}，最后班次planStart={}，班次开始={}，SPR数={}，总生产耗时={}s，结束时间={}",
                    machineCode, lastShiftPlanStart, shiftStart, sprCount, totalProductionSeconds, machineEndTime);

            if (latestEndTime == null || machineEndTime.isAfter(latestEndTime)) {
                latestEndTime = machineEndTime;
                latestMachineCode = machineCode;
            }
        }
        if (latestEndTime == null) {
            log.warn("场景1：结构 {} 无法计算结束时间，跳过", structureName);
            return null;
        }

        // 2. 查找后结构：优先从排程结果中找（机台结构时间线中前结构后面的结构），回退到未来结构配置
        String nextStructureName = findNextStructure(structureName, machineSprMap, machineStructureTimelineMap, machineFutureStructureMap, context, latestMachineCode);

        // 3. 计算切换耗时
        int switchHours = 0;
        if (nextStructureName != null) {
            switchHours = calculateSwitchHours(structureName, nextStructureName,
                    materialByEmbryoMap, materialByCodeMap, context,
                    sameInchHours, diffInchHours);
        }

        // 4. earliestLhTime = 结束时间 + 切换耗时
        LocalDateTime earliestLhTime = latestEndTime.plusHours(switchHours);

        CxEmbryoLhTime record = new CxEmbryoLhTime();
        record.setStructureName(structureName);
        record.setCxMachineCode(latestMachineCode);
        record.setNextStructureName(nextStructureName);
        record.setEndingTime(java.sql.Timestamp.valueOf(latestEndTime));
        record.setEarliestLhTime(java.sql.Timestamp.valueOf(earliestLhTime));
        record.setStructureChangeRemaining(structureRemainder);

        log.info("场景1（全部收尾）：机台={}，结构 {} -> {}，结束时间={}，切换{}h，最早可供硫化时间={}",
                latestMachineCode, structureName, nextStructureName, latestEndTime, switchHours, earliestLhTime);
        return record;
    }

    /**
     * 场景2：未全部收尾 -> 估算消耗。
     * <p>汇总该结构所有机台8班次计划量，日消耗=总生产/3，
     * 用初始成型余量/日消耗判断消耗班次，取该班次结束时间加切换耗时。
     */
    private CxEmbryoLhTime processScenario2(
            String structureName,
            Map<String, List<ShiftProductionResult>> machineSprMap,
            ScheduleContextVo context,
            Map<String, MdmMaterialInfo> materialByEmbryoMap,
            Map<String, MdmMaterialInfo> materialByCodeMap,
            Map<String, List<MpCxCapacityConfiguration>> machineFutureStructureMap,
            int sameInchHours, int diffInchHours,
            Integer structureRemainder, String factoryCode,
            List<CxShiftConfig> sortedShiftConfigs,
            List<ShiftScheduleResult> shiftResults) {

        // 1. 汇总该结构所有机台8班次计划量
        int totalProduction = 0;
        for (List<ShiftProductionResult> sprList : machineSprMap.values()) {
            for (ShiftProductionResult spr : sprList) {
                if (spr.getQuantity() != null) {
                    totalProduction += spr.getQuantity();
                }
            }
        }
        if (totalProduction <= 0) {
            log.warn("场景2：结构 {} 总生产量为0，跳过", structureName);
            return null;
        }

        // 2. 日消耗 = 总生产 / 3
        double dailyConsumption = (double) totalProduction / 3.0;

        // 3. 初始成型余量
        if (structureRemainder <= 0) {
            log.info("场景2：结构 {} 初始成型余量为0或负，无需消耗，跳过", structureName);
            return null;
        }

        // 4. 消耗天数 = 初始成型余量 / 日消耗
        double daysToConsume = structureRemainder / dailyConsumption;
        if (daysToConsume > 3.0) {
            log.info("场景2：结构 {} 初始成型余量{}，日消耗{}，需{}天>3天，无法在排程期内消耗完毕，跳过",
                    structureName, structureRemainder, String.format("%.1f", dailyConsumption), String.format("%.1f", daysToConsume));
            return null;
        }

        // 5. 消耗班次序号 ≈ ceil(初始成型余量 × 8 / 总生产量)
        int targetShiftIndex = (int) Math.ceil((double) structureRemainder * 8 / totalProduction);
        if (targetShiftIndex < 1) targetShiftIndex = 1;
        if (targetShiftIndex > sortedShiftConfigs.size()) {
            targetShiftIndex = sortedShiftConfigs.size();
        }

        // 6. 取该班次的结束时间
        CxShiftConfig targetShiftConfig = sortedShiftConfigs.get(targetShiftIndex - 1);
        LocalDate shiftDate = scheduleDayTypeHelper.calculateShiftDate(context.getScheduleDate(), targetShiftConfig);
        LocalDateTime shiftEndTime = scheduleDayTypeHelper.calculateShiftEndTimeLocal(targetShiftConfig, shiftDate);
        if (shiftEndTime == null) {
            log.warn("场景2：结构 {} 无法计算班次 {} 的结束时间，跳过", structureName, targetShiftConfig.getShiftCode());
            return null;
        }

        // 7. 查找未来结构
        String nextStructureName = findFutureStructureForStructure(structureName, machineFutureStructureMap, machineSprMap, context);

        // 8. 计算切换耗时
        int switchHours = 0;
        if (nextStructureName != null) {
            switchHours = calculateSwitchHours(structureName, nextStructureName,
                    materialByEmbryoMap, materialByCodeMap, context,
                    sameInchHours, diffInchHours);
        }

        // 9. earliestLhTime = 班次结束时间 + 切换耗时
        LocalDateTime earliestLhTime = shiftEndTime.plusHours(switchHours);

        // 10. 记录机台（取该结构下任意一个机台，优先取有未来结构配置的机台）
        String recordMachineCode = null;
        for (String mCode : machineSprMap.keySet()) {
            if (recordMachineCode == null) {
                recordMachineCode = mCode;
            }
            List<MpCxCapacityConfiguration> futures = machineFutureStructureMap.get(mCode);
            if (nextStructureName != null && futures != null
                    && futures.stream().anyMatch(f -> nextStructureName.equals(f.getStructureName()))) {
                recordMachineCode = mCode;
                break;
            }
        }

        CxEmbryoLhTime record = new CxEmbryoLhTime();
        record.setStructureName(structureName);
        record.setCxMachineCode(recordMachineCode);
        record.setNextStructureName(nextStructureName);
        record.setEndingTime(java.sql.Timestamp.valueOf(shiftEndTime));
        record.setEarliestLhTime(java.sql.Timestamp.valueOf(earliestLhTime));
        record.setStructureChangeRemaining(structureRemainder);

        log.info("场景2（估算消耗）：机台={}，结构 {}，总生产={}，日消耗={}，初始余量={}，消耗{}天约第{}班次，结束时间={}，最早可供硫化时间={}",
                recordMachineCode, structureName, totalProduction, String.format("%.1f", dailyConsumption),
                structureRemainder, String.format("%.1f", daysToConsume), targetShiftIndex,
                shiftEndTime, earliestLhTime);
        return record;
    }

    /**
     * 查找后结构：优先从排程结果中找（机台结构时间线中前结构后面的结构），回退到未来结构配置。
     *
     * @param prevStructureName          前结构
     * @param machineSprMap             该结构下各机台的SPR列表
     * @param machineStructureTimelineMap 机台->结构时间线（按班次顺序去重）
     * @param machineFutureStructureMap  机台->未来结构配置列表（按 BEGIN_DAY 升序）
     * @param context                   排程上下文（用于查找前结构 END_DAY）
     * @param preferredMachineCode       优先查找的机台（最晚结束时间的机台）
     * @return 后结构名称，找不到返回null
     */
    private String findNextStructure(String prevStructureName,
                                     Map<String, List<ShiftProductionResult>> machineSprMap,
                                     Map<String, List<String>> machineStructureTimelineMap,
                                     Map<String, List<MpCxCapacityConfiguration>> machineFutureStructureMap,
                                     ScheduleContextVo context,
                                     String preferredMachineCode) {
        // 1. 优先从排程结果中找：在机台结构时间线中，前结构后面的第一个不同结构就是后结构
        // 优先查最晚结束时间的机台，再遍历其他机台
        List<String> orderedMachines = new ArrayList<>();
        if (preferredMachineCode != null) {
            orderedMachines.add(preferredMachineCode);
        }
        for (String mCode : machineSprMap.keySet()) {
            if (!mCode.equals(preferredMachineCode)) {
                orderedMachines.add(mCode);
            }
        }
        for (String machineCode : orderedMachines) {
            List<String> timeline = machineStructureTimelineMap.get(machineCode);
            if (timeline == null || timeline.size() < 2) continue;
            for (int i = 0; i < timeline.size() - 1; i++) {
                if (timeline.get(i).equals(prevStructureName)) {
                    String nextStruct = timeline.get(i + 1);
                    if (!nextStruct.equals(prevStructureName)) {
                        return nextStruct;
                    }
                }
            }
        }

        // 2. 回退到未来结构配置：选取 BEGIN_DAY 严格晚于前结构 END_DAY 的最早未来结构
        //    （未来结构列表已按 year/month/beginDay 升序，避免依赖隐式顺序选错）
        for (String machineCode : orderedMachines) {
            List<MpCxCapacityConfiguration> futures = machineFutureStructureMap.get(machineCode);
            if (futures == null || futures.isEmpty()) continue;
            MpCxCapacityConfiguration prevConfig = findStructureConfig(context, prevStructureName, machineCode);
            int prevEndComposite = (prevConfig != null) ? endComposite(prevConfig) : -1;
            for (MpCxCapacityConfiguration f : futures) {
                if (f.getStructureName() == null || f.getStructureName().equals(prevStructureName)) continue;
                if (prevEndComposite < 0 || beginComposite(f) > prevEndComposite) {
                    return f.getStructureName();
                }
            }
        }
        return null;
    }

    /**
     * 场景2专用：从未来结构配置中查找该结构的后结构。
     * <p>选取 BEGIN_DAY 严格晚于前结构 END_DAY 的最早未来结构（列表已升序）。
     */
    private String findFutureStructureForStructure(String structureName,
                                                   Map<String, List<MpCxCapacityConfiguration>> machineFutureStructureMap,
                                                   Map<String, List<ShiftProductionResult>> machineSprMap,
                                                   ScheduleContextVo context) {
        for (String machineCode : machineSprMap.keySet()) {
            List<MpCxCapacityConfiguration> futures = machineFutureStructureMap.get(machineCode);
            if (futures == null || futures.isEmpty()) continue;
            MpCxCapacityConfiguration prevConfig = findStructureConfig(context, structureName, machineCode);
            int prevEndComposite = (prevConfig != null) ? endComposite(prevConfig) : -1;
            for (MpCxCapacityConfiguration f : futures) {
                if (f.getStructureName() == null || f.getStructureName().equals(structureName)) continue;
                if (prevEndComposite < 0 || beginComposite(f) > prevEndComposite) {
                    return f.getStructureName();
                }
            }
        }
        return null;
    }

    /**
     * 计算切换耗时（小时）：比较前结构 vs 后结构的英寸。
     */
    private int calculateSwitchHours(String prevStructure, String nextStructure,
                                     Map<String, MdmMaterialInfo> materialByEmbryoMap,
                                     Map<String, MdmMaterialInfo> materialByCodeMap,
                                     ScheduleContextVo context,
                                     int sameInchHours, int diffInchHours) {
        String prevProSize = findStructureProSize(prevStructure, context, materialByEmbryoMap, materialByCodeMap);
        String nextProSize = findStructureProSize(nextStructure, context, materialByEmbryoMap, materialByCodeMap);

        if (prevProSize != null && prevProSize.equals(nextProSize)) {
            return sameInchHours;
        }
        return diffInchHours;
    }

    /**
     * 查找结构的英寸（proSize）：从物料主数据中找该结构任意一个物料的 proSize。
     *
     * <p>优先从当前排程上下文的物料（context.getMaterials()）查找；查不到时回退按
     * STRUCTURE_NAME 查询物料主数据。原因：下一个结构的硫化需求可能从排程次日才生效，
     * 未进入当前排程日的物料集合，导致上下文物料里没有该结构，需回退到主数据兜底。
     */
    private String findStructureProSize(String structureName,
                                        ScheduleContextVo context,
                                        Map<String, MdmMaterialInfo> materialByEmbryoMap,
                                        Map<String, MdmMaterialInfo> materialByCodeMap) {
        if (context.getMaterials() != null) {
            for (MdmMaterialInfo material : context.getMaterials()) {
                if (structureName.equals(material.getStructureName()) && material.getProSize() != null) {
                    return material.getProSize();
                }
            }
        }
        // 回退：按结构名查物料主数据，避免下一个结构未加载进上下文导致英寸误判为异英寸
        if (structureName != null) {
            List<MdmMaterialInfo> byStructure = materialInfoMapper.selectList(
                    new LambdaQueryWrapper<MdmMaterialInfo>()
                            .eq(MdmMaterialInfo::getStructureName, structureName)
                            .isNotNull(MdmMaterialInfo::getProSize)
                            .eq(MdmMaterialInfo::getIsDelete, "0")
                            .last("LIMIT 1"));
            if (byStructure != null && !byStructure.isEmpty()
                    && byStructure.get(0).getProSize() != null) {
                return byStructure.get(0).getProSize();
            }
        }
        return null;
    }

    /**
     * 构建结构 -> 初始成型余量映射。
     * <p>从 initialFormingRemainderMap（materialCode -> 成型余量）按结构汇总。
     */
    private Map<String, Integer> buildStructureFormingRemainderMap(ScheduleContextVo context,
                                                                   Map<String, String> materialToStructureMap) {
        Map<String, Integer> result = new HashMap<>();
        Map<String, Integer> initialRemainderMap = context.getInitialFormingRemainderMap();
        if (initialRemainderMap == null || materialToStructureMap == null) {
            return result;
        }
        for (Map.Entry<String, Integer> entry : initialRemainderMap.entrySet()) {
            MdmMonthSurplus surplus = context.getMonthSurplusMap() != null
                    ? context.getMonthSurplusMap().get(entry.getKey()) : null;
            String materialCode = surplus != null ? surplus.getMaterialCode() : null;
            String structureName = materialToStructureMap.get(materialCode);
            if (structureName != null) {
                result.merge(structureName, entry.getValue() != null ? entry.getValue() : 0, Integer::sum);
            }
        }
        return result;
    }

    /**
     * 构建机台 -> 未来结构配置列表（按 BEGIN_DAY 升序）的反查映射。
     * <p>从 futureStructureAllocationMap（structureName -> List<配置>）反查聚合，
     * 同一机台的多个未来结构按 year/month/beginDay 升序排列，
     * 供 findNextStructure 按"严格晚于前结构 END_DAY"选取真正的下一个结构，
     * 避免依赖 DB 返回顺序或 HashMap 遍历顺序导致选错。
     */
    private Map<String, List<MpCxCapacityConfiguration>> buildMachineFutureStructureMap(ScheduleContextVo context) {
        Map<String, List<MpCxCapacityConfiguration>> result = new LinkedHashMap<>();
        Map<String, List<MpCxCapacityConfiguration>> futureMap = context.getFutureStructureAllocationMap();
        if (futureMap == null) {
            return result;
        }
        for (List<MpCxCapacityConfiguration> configs : futureMap.values()) {
            for (MpCxCapacityConfiguration config : configs) {
                if (config.getCxMachineCode() != null) {
                    result.computeIfAbsent(config.getCxMachineCode(), k -> new ArrayList<>()).add(config);
                }
            }
        }
        // 显式排序：year -> month -> beginDay（不依赖 DB/HashMap 隐式顺序）
        result.values().forEach(list -> list.sort((a, b) -> {
            int cmp = Integer.compare(a.getYear(), b.getYear());
            if (cmp != 0) return cmp;
            cmp = Integer.compare(a.getMonth(), b.getMonth());
            if (cmp != 0) return cmp;
            return Integer.compare(a.getBeginDay(), b.getBeginDay());
        }));
        return result;
    }

    /**
     * 查找指定机台上某结构的排产配置（用于获取 END_DAY 等时间信息）。
     * <p>优先从当月 structureAllocationMap 查找，回退到 futureStructureAllocationMap。
     *
     * @return 匹配的配置，找不到返回 null
     */
    private MpCxCapacityConfiguration findStructureConfig(ScheduleContextVo context,
                                                          String structureName, String machineCode) {
        MpCxCapacityConfiguration match = findConfigInStructureMap(context.getStructureAllocationMap(), structureName, machineCode);
        if (match != null) {
            return match;
        }
        return findConfigInStructureMap(context.getFutureStructureAllocationMap(), structureName, machineCode);
    }

    private MpCxCapacityConfiguration findConfigInStructureMap(Map<String, List<MpCxCapacityConfiguration>> map,
                                                               String structureName, String machineCode) {
        if (map == null || machineCode == null) {
            return null;
        }
        List<MpCxCapacityConfiguration> configs = map.get(structureName);
        if (configs == null) {
            return null;
        }
        for (MpCxCapacityConfiguration c : configs) {
            if (machineCode.equals(c.getCxMachineCode())) {
                return c;
            }
        }
        return null;
    }

    /** 配置的"开始序号" year*10000+month*100+beginDay，用于跨月先后比较。 */
    private int beginComposite(MpCxCapacityConfiguration c) {
        return composite(c.getYear(), c.getMonth(), c.getBeginDay());
    }

    /** 配置的"结束序号" year*10000+month*100+endDay，用于跨月先后比较。 */
    private int endComposite(MpCxCapacityConfiguration c) {
        return composite(c.getYear(), c.getMonth(), c.getEndDay());
    }

    private int composite(Integer year, Integer month, Integer day) {
        if (year == null || month == null || day == null) {
            return -1;
        }
        return year * 10000 + month * 100 + day;
    }

    /**
     * 从上下文读取整数参数值。
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
     * 单班次排程编排 — 委托 engine 层完成分组、分配、精排。
     *
     * <p>本方法不修改跨班次持久状态（除返回的 {@link ShiftScheduleResult}）；
     * 库存/余量滚动在 {@link #executeSchedule} 的 2.4.5～2.4.6 执行。
     *
     * <p><b>TaskAllocation → DailyEmbryoTask</b>：精排前反构任务对象；
     * {@code endingExtraInventory} 优先于 {@code quantity}；开停产标志 TaskGroup 已写入时保留，
     * 否则用 {@link ScheduleDayTypeHelper#determineShiftType} 兜底。
     *
     * @return 含 allAllocations + shiftProductionResults + 排程前 materialStock 快照
     */
    private ShiftScheduleResult executeShiftSchedule(
            ScheduleContextVo context,
            int day,
            CxShiftConfig shiftConfig,
            LocalDate scheduleDate,
            Map<String, Set<String>> machineOnlineEmbryoMap,
            List<ShiftScheduleResult> shiftHistory) {

        List<CxShiftConfig> singleShiftList = Collections.singletonList(shiftConfig);
        String factoryCode = context.getFactoryCode();

        log.info("========== 开始执行班次排程，天={}, 日期={}, 班次={} ==========",
                day, scheduleDate, shiftConfig.getShiftCode());

        // 5.2 任务分组（TaskGroupService：R1/R2/R3 + 立库封顶 + 提前生产）
        TaskGroupResultVo taskGroup = taskGroupService.groupTasks(
                context, machineOnlineEmbryoMap, scheduleDate, singleShiftList);
        log.info("任务分组完成：续作 {} 个，试制 {} 个，新增 {} 个",
                taskGroup.getContinueTasks().size(),
                taskGroup.getTrialTasks().size(),
                taskGroup.getNewTasks().size());

        // 5.2.1 单日试制/量试 SKU 上限过滤（胎胚编码去重，默认最多2个）
        applyDailyTrialSkuLimit(context, taskGroup);

        // 5.3.1 续作处理（SYS04070003=Y 时保底预留1台硫化机，否则仅标记交给 NewTaskProcessor）
        List<MachineAllocationResult> continueAllocations = continueTaskProcessor.processContinueTasks(
                taskGroup.getContinueTasks(), context, scheduleDate, singleShiftList, day);
        log.info("续作任务处理完成，机台分配数: {}", continueAllocations.size());

        // 5.3.2 试制处理（空机台优先，不走 DFS 均衡）
        List<MachineAllocationResult> trialAllocations = trialTaskProcessor.processTrialTasks(
                taskGroup.getTrialTasks(), context, scheduleDate, singleShiftList, context.getAvailableMachines());
        log.info("试制任务处理完成，机台分配数: {}", trialAllocations.size());

        // 5.3.3 新增处理（续作剩余+新增合并，调用 BalancingService DFS 均衡）
        List<MachineAllocationResult> newAllocations = newTaskProcessor.processNewTasks(
                taskGroup.getNewTasks(),
                context,
                scheduleDate,
                singleShiftList,
                taskGroup.getContinueTasks(),
                continueAllocations,
                trialAllocations);
        log.info("新增任务处理完成，机台分配数: {}", newAllocations.size());

        // 5.3.4 合并三类机台分配结果
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

        // 执行机台级三轮精排：单机台物理产能封顶 + 胎胚维度限制（替代原结构内调拨均衡）
        balanceMachineQuantityWithinStructure(allAllocations, context);

        // 5.3.5 精度计划挑选与提前扣量（每日首次执行，修改 TaskAllocation 数量）
        applyPrecisionPlanSelection(context, scheduleDate, shiftConfig, allAllocations);

        // 5.3.7 班次精排（逐机台逐任务调用 ShiftScheduleService.scheduleTaskToShifts）
        List<ShiftProductionResult> shiftProductionResults = new ArrayList<>();

        for (MachineAllocationResult allocation : allAllocations) {
            String machineCode = allocation.getMachineCode();
            log.info("========== 对{}机台进行班次排量 ==========", machineCode);
            for (TaskAllocation taskAlloc : allocation.getTaskAllocations()) {
                DailyEmbryoTask task = new DailyEmbryoTask();
                task.setEmbryoCode(taskAlloc.getEmbryoCode());
                task.setMaterialCode(taskAlloc.getMaterialCode());
                task.setMaterialDesc(taskAlloc.getMaterialDesc());
                task.setMainMaterialDesc(taskAlloc.getMainMaterialDesc());
                task.setStructureName(taskAlloc.getStructureName());
                task.setPlannedProduction(taskAlloc.getQuantity());
                task.setEndingExtraInventory(taskAlloc.getEndingExtraInventory() != null
                        ? taskAlloc.getEndingExtraInventory() : taskAlloc.getQuantity());
                task.setIsTrialTask(taskAlloc.getIsTrialTask());
                task.setIsProductionTrial(taskAlloc.getIsProductionTrial());
                task.setIsEndingTask(taskAlloc.getIsEndingTask());
                task.setIsContinueTask(taskAlloc.getIsContinueTask());
                task.setIsLastEndingBatch(taskAlloc.getIsLastEndingBatch());
                task.setIsEndProduction(taskAlloc.getIsEndProduction());
                task.setProductStatus(taskAlloc.getProductStatus());
                task.setConstructionStage(taskAlloc.getConstructionStage());
                task.setEndingAbandoned(taskAlloc.getEndingAbandoned());
                task.setPrecisionDeducted(taskAlloc.getPrecisionDeducted());
                task.setIsFirstTask(taskAlloc.getIsFirstTask());
                task.setIsUrgentEnding(taskAlloc.getIsUrgentEnding());
                task.setIsNearEnding(taskAlloc.getIsNearEnding());
                task.setIsOpeningDayTask(taskAlloc.getIsOpeningDayTask());
                task.setIsClosingDayTask(taskAlloc.getIsClosingDayTask());
                if (task.getIsOpeningDayTask() == null || task.getIsClosingDayTask() == null) {
                    int shiftOrder = shiftConfig.getDayShiftOrder() != null ? shiftConfig.getDayShiftOrder() : 1;
                    ShiftType shiftType = scheduleDayTypeHelper.determineShiftType(scheduleDate, shiftOrder, factoryCode);
                    if (task.getIsOpeningDayTask() == null) {
                        task.setIsOpeningDayTask(shiftType == ShiftType.OPEN_START);
                    }
                    if (task.getIsClosingDayTask() == null) {
                        task.setIsClosingDayTask(shiftType == ShiftType.CLOSED);
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

                List<ShiftProductionResult> taskShiftResults =
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

    // ==================== 5.3.4 机台级三轮精排（单机台物理产能封顶 + 胎胚维度限制） ====================

    /**
     * 5.3.4 机台级三轮精排。
     *
     * <p>复刻 S5.2.R1~R3 三轮模式，维度从"结构"下放到"均衡分配后的机台×胎胚"：
     * <ul>
     *   <li><b>MQ-R1 产能内直排</b>：机台内逐胎胚（续作优先、锚定量降序）按原量直排，
     *       累计耗时 ≤ 机台物理产能（28800s − 排除任务已占），放不下削减到整车倍数；</li>
     *   <li><b>MQ-R2 轮询补量</b>：被削减任务的需求余额，机台产能有富余时按机台内全局轮次
     *       逐车补量，每车事前预估 stockHours，超软阈值（SYS04080003）退出到 R3；</li>
     *   <li><b>MQ-R3 硬上限补量</b>：R2 退出任务按硬上限（SYS04080001）继续逐车补量，
     *       事前预估超硬上限则移出（不再分配）。</li>
     * </ul>
     *
     * <p>产能公式：单胎耗时 = 86400 / (机台配比 × 双模日硫化量)，机台产能 = 28800s。
     * 试制/量试/收尾任务不参与精排，但其排量占用机台物理产能。
     * 跨班次历史不再参与钳制（均衡分配阶段已保证机台归属，物理产能封顶后排量天然稳定）。
     *
     * @param allAllocations 当前班次合并后的机台分配结果（原地修改 quantity/endingExtraInventory）
     * @param context         排程上下文（获取整车容量、日硫化量、参数配置等）
     */
    void balanceMachineQuantityWithinStructure(
            List<MachineAllocationResult> allAllocations,
            ScheduleContextVo context) {

        if (CollectionUtils.isEmpty(allAllocations)) {
            return;
        }

        // 1. 按结构 -> 机台 聚合（试制/量试/收尾任务排除出精排，但其排量占用机台产能）
        Map<String, Map<String, MachineAgg>> structMachineMap = buildStructMachineAgg(allAllocations);
        if (structMachineMap.isEmpty()) {
            return;
        }

        // 2. stockHours 软/硬阈值与开关（与 S5.2.R2/R3 同参数同口径）
        boolean stockHoursCapEnabled = isStockHoursCapEnabled(context);
        int stockHoursHardCap = getStockHoursHardCap(context);
        int stockHoursSoftTrigger = getStockHoursSoftTrigger(context, stockHoursHardCap);

        // 3. 构建 lhId -> mouldQty 映射（stockHours 增量计算用）
        Map<Long, Integer> lhIdToMoldQty = new HashMap<>();
        if (context.getLhScheduleResults() != null) {
            for (LhScheduleResult lh : context.getLhScheduleResults()) {
                if (lh.getId() != null) {
                    int mq = lh.getMouldQty() != null && lh.getMouldQty() > 0 ? lh.getMouldQty() : 1;
                    lhIdToMoldQty.put(lh.getId(), mq);
                }
            }
        }

        log.info("【机台级三轮精排】开始: 结构数={}, stockHoursCap开关={}, 软阈值={}h, 硬上限={}h",
                structMachineMap.size(), stockHoursCapEnabled ? "Y" : "N", stockHoursSoftTrigger, stockHoursHardCap);

        // 4. 逐结构逐机台精排（单机台同样做产能封顶）
        for (Map.Entry<String, Map<String, MachineAgg>> structEntry : structMachineMap.entrySet()) {
            String structure = structEntry.getKey();
            Map<String, MachineAgg> machineAggs = structEntry.getValue();

            int tripCap = getTripCapForStructure(structure, machineAggs, context);
            if (tripCap <= 0) {
                continue;
            }

            for (MachineAgg agg : machineAggs.values()) {
                refineSingleMachine(structure, agg, tripCap, context,
                        stockHoursCapEnabled, stockHoursSoftTrigger, stockHoursHardCap, lhIdToMoldQty);
            }
        }
    }

    /**
     * 单机台三轮精排（MQ-R1 直排 / MQ-R2 软阈值轮询补量 / MQ-R3 硬上限补量）。
     *
     * @param structure            结构名
     * @param agg                  机台聚合
     * @param tripCap              整车容量（条）
     * @param context              排程上下文
     * @param stockHoursCapEnabled 可供硫化时长管控开关
     * @param softTrigger          软阈值（小时）
     * @param hardCap              硬上限（小时）
     * @param lhIdToMoldQty        lhId -> 模数映射
     */
    private void refineSingleMachine(String structure, MachineAgg agg, int tripCap,
                                     ScheduleContextVo context, boolean stockHoursCapEnabled,
                                     int softTrigger, int hardCap, Map<Long, Integer> lhIdToMoldQty) {
        if (agg.eligibleTasks.isEmpty()) {
            return;
        }

        // 机台硫化配比（无配置回退1）
        Integer ratioInt = productionCalculator.getMachineLhMaxQty(agg.machineCode, structure, context);
        BigDecimal machineRatio = (ratioInt != null && ratioInt > 0)
                ? BigDecimal.valueOf(ratioInt) : BigDecimal.ONE;

        // 每胎胚单胎耗时（秒）：86400 / (机台配比 × 双模日硫化量)；日硫化量缺失时无法量化产能，跳过精排保持原量
        // 注意：TaskAllocation 为可变对象（@Data 哈希随字段变化），后续 adjustTaskQty 会改任务量，
        // 必须用 IdentityHashMap（按引用判等），否则削减后按新 hash 查不到导致 NPE
        Map<TaskAllocation, BigDecimal> timePerTireMap = new IdentityHashMap<>();
        for (TaskAllocation ta : agg.eligibleTasks) {
            Integer dailyLhCapacity = productionCalculator.getDoubleMoldDailyLhCapacity(ta.getMaterialCode(), context);
            BigDecimal tpt = (dailyLhCapacity != null && dailyLhCapacity > 0)
                    ? productionCalculator.calculateTimePerTire(machineRatio, dailyLhCapacity)
                    : BigDecimal.ZERO;
            timePerTireMap.put(ta, tpt);
        }
        boolean anyInvalid = timePerTireMap.values().stream().anyMatch(t -> t.compareTo(BigDecimal.ZERO) <= 0);
        if (anyInvalid) {
            log.warn("[MQ精排] 结构={}, 机台={}, 存在日硫化量缺失的胎胚，跳过精排保持原量", structure, agg.machineCode);
            return;
        }

        // 排除任务（试制/量试/收尾）已占产能：量不变但挤占机台物理产能
        BigDecimal occupiedSeconds = BigDecimal.ZERO;
        for (TaskAllocation ta : agg.excludedTasks) {
            Integer dailyLhCapacity = productionCalculator.getDoubleMoldDailyLhCapacity(ta.getMaterialCode(), context);
            if (dailyLhCapacity == null || dailyLhCapacity <= 0) {
                continue;
            }
            BigDecimal tpt = productionCalculator.calculateTimePerTire(machineRatio, dailyLhCapacity);
            occupiedSeconds = occupiedSeconds.add(tpt.multiply(BigDecimal.valueOf(getTaskQty(ta))));
        }
        BigDecimal capacitySeconds = BigDecimal.valueOf(SECONDS_PER_SHIFT).subtract(occupiedSeconds);
        if (capacitySeconds.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("[MQ精排] 结构={}, 机台={}, 排除任务已占满产能({}s)，可参与任务全部削减为0",
                    structure, agg.machineCode, occupiedSeconds.toBigInteger());
            for (TaskAllocation ta : agg.eligibleTasks) {
                int qty = getTaskQty(ta);
                if (qty > 0) {
                    adjustTaskQty(ta, -qty);
                    updateTaskStockHours(ta, -qty, context, lhIdToMoldQty);
                    log.info("[MQ-R1] 削减: 机台={}, 胚胎={}, {} -> 0条 (排除任务占满产能)", agg.machineCode, ta.getEmbryoCode(), qty);
                }
            }
            return;
        }

        log.info("[MQ精排] 结构={}, 机台={}, 总产能={}s({}h), 排除任务已占={}s({}h), 可用={}s({}h), 可参与胎胚数={}, tripCap={}",
                structure, agg.machineCode,
                SECONDS_PER_SHIFT, SECONDS_PER_SHIFT / ScheduleConstants.SECONDS_PER_HOUR,
                occupiedSeconds.toBigInteger(),
                occupiedSeconds.divide(BigDecimal.valueOf(ScheduleConstants.SECONDS_PER_HOUR), 1, BigDecimal.ROUND_HALF_UP),
                capacitySeconds.toBigInteger(),
                capacitySeconds.divide(BigDecimal.valueOf(ScheduleConstants.SECONDS_PER_HOUR), 1, BigDecimal.ROUND_HALF_UP),
                agg.eligibleTasks.size(), tripCap);

        // ==================== MQ-R1：产能内直排（续作优先、锚定量降序） ====================
        List<TaskAllocation> ordered = agg.eligibleTasks.stream()
                .sorted(Comparator
                        .comparing((TaskAllocation t) -> !Boolean.TRUE.equals(t.getIsContinueTask()))
                        .thenComparing(t -> getTaskQty(t), Comparator.reverseOrder()))
                .collect(Collectors.toList());

        // 同上：补量会再次修改任务量，用 IdentityHashMap 防止 hash 漂移产生脏条目
        Map<TaskAllocation, Integer> remainingDemandMap = new IdentityHashMap<>();
        BigDecimal usedSeconds = BigDecimal.ZERO;

        for (TaskAllocation ta : ordered) {
            int qty = getTaskQty(ta);
            if (qty <= 0) {
                continue;
            }
            BigDecimal tpt = timePerTireMap.get(ta);
            BigDecimal needSeconds = tpt.multiply(BigDecimal.valueOf(qty));
            if (usedSeconds.add(needSeconds).compareTo(capacitySeconds) <= 0) {
                usedSeconds = usedSeconds.add(needSeconds);
                continue;
            }
            // 削减：剩余产能能放下的整车条数（向下取整车，不够一车归零）
            BigDecimal available = capacitySeconds.subtract(usedSeconds);
            int maxQty = available.divide(tpt, 0, RoundingMode.FLOOR).intValue();
            int keptQty = Math.min(qty, maxQty / tripCap * tripCap);
            int delta = keptQty - qty;
            if (delta < 0) {
                adjustTaskQty(ta, delta);
                updateTaskStockHours(ta, delta, context, lhIdToMoldQty);
                remainingDemandMap.put(ta, qty - keptQty);
                usedSeconds = usedSeconds.add(tpt.multiply(BigDecimal.valueOf(keptQty)));
                log.info("[MQ-R1] 削减: 机台={}, 胚胎={}, {} -> {}条 (产能不足, 剩余需求={})",
                        agg.machineCode, ta.getEmbryoCode(), qty, keptQty, qty - keptQty);
            }
        }

        // ==================== MQ-R2：富余产能逐车轮询补量（软阈值事前预估，超软退出到R3） ====================
        List<TaskAllocation> r2Queue = remainingDemandMap.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        List<TaskAllocation> r2Exited = new ArrayList<>();

        if (!r2Queue.isEmpty()) {
            int round = 0;
            while (usedSeconds.compareTo(capacitySeconds) < 0 && !r2Queue.isEmpty()) {
                round++;
                boolean anyAction = false;
                Iterator<TaskAllocation> it = r2Queue.iterator();
                while (it.hasNext()) {
                    TaskAllocation ta = it.next();
                    int demand = remainingDemandMap.getOrDefault(ta, 0);
                    if (demand < tripCap) {
                        it.remove();
                        continue;
                    }
                    BigDecimal carSeconds = timePerTireMap.get(ta).multiply(BigDecimal.valueOf(tripCap));
                    if (usedSeconds.add(carSeconds).compareTo(capacitySeconds) > 0) {
                        continue;
                    }
                    // 软阈值事前预估：现有 stockHours + 本车增量 > 软阈值 -> 退出到 R3
                    if (stockHoursCapEnabled && isProjectedStockHoursOver(ta, tripCap, softTrigger, context, lhIdToMoldQty)) {
                        r2Exited.add(ta);
                        it.remove();
                        log.info("[MQ-R2-第{}轮] 退出到R3: 机台={}, 胚胎={}, 预计stockHours超软阈值{}h",
                                round, agg.machineCode, ta.getEmbryoCode(), softTrigger);
                        continue;
                    }
                    adjustTaskQty(ta, tripCap);
                    updateTaskStockHours(ta, tripCap, context, lhIdToMoldQty);
                    usedSeconds = usedSeconds.add(carSeconds);
                    remainingDemandMap.put(ta, demand - tripCap);
                    anyAction = true;
                    log.info("[MQ-R2-第{}轮] 补量: 机台={}, 胚胎={}, +1车({}条), 剩余需求={}",
                            round, agg.machineCode, ta.getEmbryoCode(), tripCap, demand - tripCap);
                }
                if (!anyAction) {
                    break;
                }
            }
        }

        // ==================== MQ-R3：R2退出任务硬上限补量（事前预估超硬上限移出，不再分配） ====================
        if (!r2Exited.isEmpty()) {
            int round = 0;
            while (usedSeconds.compareTo(capacitySeconds) < 0 && !r2Exited.isEmpty()) {
                round++;
                boolean anyAction = false;
                Iterator<TaskAllocation> it = r2Exited.iterator();
                while (it.hasNext()) {
                    TaskAllocation ta = it.next();
                    int demand = remainingDemandMap.getOrDefault(ta, 0);
                    if (demand < tripCap) {
                        it.remove();
                        continue;
                    }
                    BigDecimal carSeconds = timePerTireMap.get(ta).multiply(BigDecimal.valueOf(tripCap));
                    if (usedSeconds.add(carSeconds).compareTo(capacitySeconds) > 0) {
                        continue;
                    }
                    if (stockHoursCapEnabled && isProjectedStockHoursOver(ta, tripCap, hardCap, context, lhIdToMoldQty)) {
                        it.remove();
                        log.info("[MQ-R3-第{}轮] 移出: 机台={}, 胚胎={}, 预计stockHours超硬上限{}h, 不再分配",
                                round, agg.machineCode, ta.getEmbryoCode(), hardCap);
                        continue;
                    }
                    adjustTaskQty(ta, tripCap);
                    updateTaskStockHours(ta, tripCap, context, lhIdToMoldQty);
                    usedSeconds = usedSeconds.add(carSeconds);
                    remainingDemandMap.put(ta, demand - tripCap);
                    anyAction = true;
                    log.info("[MQ-R3-第{}轮] 补量: 机台={}, 胚胎={}, +1车({}条), 剩余需求={}",
                            round, agg.machineCode, ta.getEmbryoCode(), tripCap, demand - tripCap);
                }
                if (!anyAction) {
                    break;
                }
            }
        }

        // 总结日志
        StringBuilder detail = new StringBuilder();
        for (TaskAllocation ta : agg.eligibleTasks) {
            detail.append(ta.getEmbryoCode()).append("=").append(getTaskQty(ta)).append("条, ");
        }
        log.info("[MQ精排] 完成: 结构={}, 机台={}, 产能利用率={}/{}s({}h/{}h), 最终分布: {}",
                structure, agg.machineCode,
                usedSeconds.add(occupiedSeconds).toBigInteger(), SECONDS_PER_SHIFT,
                usedSeconds.add(occupiedSeconds).divide(BigDecimal.valueOf(ScheduleConstants.SECONDS_PER_HOUR), 1, BigDecimal.ROUND_HALF_UP),
                SECONDS_PER_SHIFT / ScheduleConstants.SECONDS_PER_HOUR,
                detail.length() > 0 ? detail.substring(0, detail.length() - 2) : "(无)");
    }

    /**
     * 事前预估：任务现有 stockHours + 本次增量车数对应时长是否超阈值。
     *
     * @param ta           任务
     * @param deltaQty     增量条数（正数）
     * @param threshold    阈值（小时）
     * @param context      排程上下文
     * @param lhIdToMoldQty lhId -> 模数映射
     * @return true 表示预估超阈值
     */
    private boolean isProjectedStockHoursOver(TaskAllocation ta, int deltaQty, int threshold,
                                              ScheduleContextVo context, Map<Long, Integer> lhIdToMoldQty) {
        int singleLhCap = productionCalculator.getSingleMoldDailyLhCapacity(ta.getMaterialCode(), context);
        if (singleLhCap <= 0) {
            return false;
        }
        int moldQty = lhIdToMoldQty.getOrDefault(ta.getLhId(), 1);
        if (moldQty <= 0) {
            return false;
        }
        BigDecimal delta = productionCalculator.calculateStockHours(deltaQty, singleLhCap, moldQty);
        BigDecimal currentSh = ta.getStockHours() != null ? ta.getStockHours() : BigDecimal.ZERO;
        return currentSh.add(delta).compareTo(BigDecimal.valueOf(threshold)) > 0;
    }

    /**
     * 获取可供硫化时长管控开关：SYS04080005，Y=开启，N=关闭，默认开启。
     */
    private boolean isStockHoursCapEnabled(ScheduleContextVo context) {
        CxParamConfig config = context.getParamConfigMap() != null
                ? context.getParamConfigMap().get(PARAM_STOCK_HOURS_CAP_ENABLED) : null;
        if (config != null && config.getParamValue() != null) {
            return "Y".equalsIgnoreCase(config.getParamValue().trim());
        }
        return true;
    }

    /**
     * 获取可供硫化时长硬上限（小时）：SYS04080001，默认 6。
     */
    private int getStockHoursHardCap(ScheduleContextVo context) {
        CxParamConfig config = context.getParamConfigMap() != null
                ? context.getParamConfigMap().get(PARAM_STOCK_HOURS_CAP) : null;
        if (config != null && config.getParamValue() != null) {
            try {
                return Integer.parseInt(config.getParamValue());
            } catch (NumberFormatException e) {
                log.warn("解析可供硫化时长硬上限失败: {}", config.getParamValue());
            }
        }
        return 6;
    }

    /**
     * 获取可供硫化时长软阈值（小时）：SYS04080003，未配置时回退到硬上限。
     */
    private int getStockHoursSoftTrigger(ScheduleContextVo context, int hardCap) {
        CxParamConfig config = context.getParamConfigMap() != null
                ? context.getParamConfigMap().get(PARAM_STOCK_HOURS_SOFT_TRIGGER) : null;
        if (config != null && config.getParamValue() != null) {
            try {
                return Integer.parseInt(config.getParamValue());
            } catch (NumberFormatException e) {
                log.warn("解析可供硫化时长软阈值失败: {}", config.getParamValue());
            }
        }
        return hardCap;
    }

    /**
     * 跨班次总产量均衡：确保同一机台在同一天各班次的总产量差异不超过1车。
     *
     * <p>在班次精排完成后执行，对 ShiftProductionResult 做后处理调整：
     * 从高产班次减1车某胎胚，给低产班次加1车同胎胚，直到差异 <= 1车或无法调拨。
     *
     * <p>约束：
     * - 排除试制/量试/收尾任务
     * - 调拨量必须整车（tripCap 的倍数）
     * - 必须是同一胎胚（giver 和 receiver 班次都有该胎胚的 SPR）
     * - 高产班次该胎胚产量 >= 1车
     *
     * @param shiftResults 所有班次的排程结果
     * @param context 排程上下文
     */
    void balanceCrossShiftQuantity(List<ShiftScheduleResult> shiftResults, ScheduleContextVo context) {
        if (CollectionUtils.isEmpty(shiftResults) || shiftResults.size() < 2) {
            return;
        }

        // 1. 按天分组
        Map<Integer, List<ShiftScheduleResult>> dayShiftMap = new LinkedHashMap<>();
        for (ShiftScheduleResult sr : shiftResults) {
            dayShiftMap.computeIfAbsent(sr.getDay(), k -> new ArrayList<>()).add(sr);
        }

        // 2. 逐天处理
        int totalTransfers = 0;
        for (Map.Entry<Integer, List<ShiftScheduleResult>> dayEntry : dayShiftMap.entrySet()) {
            int day = dayEntry.getKey();
            List<ShiftScheduleResult> dayShifts = dayEntry.getValue();
            if (dayShifts.size() < 2) continue;

            // 3. 按机台分组，收集各班次各胎胚的可参与 SPR
            // machineCode -> shiftCode -> embryoCode -> List<SPR>
            Map<String, Map<String, Map<String, List<ShiftProductionResult>>>> machineMap = new LinkedHashMap<>();
            for (ShiftScheduleResult sr : dayShifts) {
                String shiftCode = sr.getShiftConfig() != null ? sr.getShiftConfig().getShiftCode() : "UNKNOWN";
                if (sr.getShiftProductionResults() == null) continue;
                for (ShiftProductionResult spr : sr.getShiftProductionResults()) {
                    if (spr.getMachineCode() == null || spr.getEmbryoCode() == null) continue;
                    // 排除试制/收尾
                    if (Boolean.TRUE.equals(spr.getIsTrialTask())) continue;
                    if (Boolean.TRUE.equals(spr.getIsEndingTask())) continue;
                    if (Boolean.TRUE.equals(spr.getIsLastEndingBatch())) continue;
                    if (Boolean.TRUE.equals(spr.getSourceTask() != null ? spr.getSourceTask().getIsProductionTrial() : null)) continue;

                    machineMap
                            .computeIfAbsent(spr.getMachineCode(), k -> new LinkedHashMap<>())
                            .computeIfAbsent(shiftCode, k -> new LinkedHashMap<>())
                            .computeIfAbsent(spr.getEmbryoCode(), k -> new ArrayList<>())
                            .add(spr);
                }
            }

            // 4. 逐机台均衡
            for (Map.Entry<String, Map<String, Map<String, List<ShiftProductionResult>>>> machineEntry : machineMap.entrySet()) {
                String machineCode = machineEntry.getKey();
                Map<String, Map<String, List<ShiftProductionResult>>> shiftEmbryoMap = machineEntry.getValue();
                if (shiftEmbryoMap.size() < 2) continue;

                totalTransfers += balanceMachineCrossShift(machineCode, day, shiftEmbryoMap);
            }
        }

        if (totalTransfers > 0) {
            log.info("【跨班次总产量均衡】共执行 {} 次调拨", totalTransfers);
        }
    }

    /**
     * 单机台跨班次均衡：从高产班次调拨整车到低产班次。
     *
     * @param machineCode 机台编码
     * @param day 天
     * @param shiftEmbryoMap shiftCode -> embryoCode -> List<SPR>
     * @return 实际执行的调拨次数
     */
    private int balanceMachineCrossShift(String machineCode, int day,
                                         Map<String, Map<String, List<ShiftProductionResult>>> shiftEmbryoMap) {

        // 获取整车容量（取任一 SPR 的 tripCapacity）
        int tripCap = 0;
        for (Map<String, List<ShiftProductionResult>> embryoMap : shiftEmbryoMap.values()) {
            for (List<ShiftProductionResult> sprs : embryoMap.values()) {
                if (!sprs.isEmpty() && sprs.get(0).getTripCapacity() != null && sprs.get(0).getTripCapacity() > 0) {
                    tripCap = sprs.get(0).getTripCapacity();
                    break;
                }
            }
            if (tripCap > 0) break;
        }
        if (tripCap <= 0) return 0;

        int transferCount = 0;
        int maxIterations = 30; // 安全阀

        while (maxIterations-- > 0) {
            // 计算各班次总产量
            Map<String, Integer> shiftTotals = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, List<ShiftProductionResult>>> shiftEntry : shiftEmbryoMap.entrySet()) {
                int total = 0;
                for (List<ShiftProductionResult> sprs : shiftEntry.getValue().values()) {
                    for (ShiftProductionResult spr : sprs) {
                        total += spr.getQuantity() != null ? spr.getQuantity() : 0;
                    }
                }
                shiftTotals.put(shiftEntry.getKey(), total);
            }

            // 找高产和低产班次
            String maxShift = null, minShift = null;
            int maxQty = Integer.MIN_VALUE, minQty = Integer.MAX_VALUE;
            for (Map.Entry<String, Integer> e : shiftTotals.entrySet()) {
                if (e.getValue() > maxQty) { maxQty = e.getValue(); maxShift = e.getKey(); }
                if (e.getValue() < minQty) { minQty = e.getValue(); minShift = e.getKey(); }
            }

            // 差异 <= 1车，均衡完成
            if (maxShift == null || maxQty - minQty <= tripCap) break;

            // 找共同胎胚（高产班次有 >= 1车可减，低产班次有该胎胚可加）
            Map<String, List<ShiftProductionResult>> maxEmbryoMap = shiftEmbryoMap.get(maxShift);
            Map<String, List<ShiftProductionResult>> minEmbryoMap = shiftEmbryoMap.get(minShift);

            String bestEmbryo = null;
            BigDecimal bestStockHours = null;
            for (String embryo : maxEmbryoMap.keySet()) {
                if (!minEmbryoMap.containsKey(embryo)) continue;
                List<ShiftProductionResult> maxSprs = maxEmbryoMap.get(embryo);
                int maxEmbryoQty = maxSprs.stream().mapToInt(s -> s.getQuantity() != null ? s.getQuantity() : 0).sum();
                if (maxEmbryoQty < tripCap) continue;

                // 选 stockHours 最高的胎胚（库存多的先减）
                for (ShiftProductionResult spr : maxSprs) {
                    BigDecimal sh = spr.getStockHours() != null ? spr.getStockHours() : BigDecimal.ZERO;
                    if (bestStockHours == null || sh.compareTo(bestStockHours) > 0) {
                        bestStockHours = sh;
                        bestEmbryo = embryo;
                    }
                }
            }

            if (bestEmbryo == null) break; // 无可调拨胎胚

            // 执行调拨：高产班次减1车，低产班次加1车
            List<ShiftProductionResult> maxSprs = maxEmbryoMap.get(bestEmbryo);
            List<ShiftProductionResult> minSprs = minEmbryoMap.get(bestEmbryo);

            // 从高产班次的 SPR 中减1车（选 stockHours 最高的 SPR）
            ShiftProductionResult giverSpr = maxSprs.stream()
                    .max(Comparator.comparing(s -> s.getStockHours() != null ? s.getStockHours() : BigDecimal.ZERO))
                    .orElse(null);
            if (giverSpr == null || giverSpr.getQuantity() == null || giverSpr.getQuantity() < tripCap) break;

            giverSpr.setQuantity(giverSpr.getQuantity() - tripCap);

            // 给低产班次的 SPR 加1车（选 stockHours 最低的 SPR）
            ShiftProductionResult receiverSpr = minSprs.stream()
                    .min(Comparator.comparing(s -> s.getStockHours() != null ? s.getStockHours() : BigDecimal.ZERO))
                    .orElse(null);
            if (receiverSpr == null) {
                // 回滚 giver
                giverSpr.setQuantity(giverSpr.getQuantity() + tripCap);
                break;
            }

            receiverSpr.setQuantity(receiverSpr.getQuantity() + tripCap);

            int maxBefore = maxQty;
            int minBefore = minQty;
            log.info("【跨班次均衡】机台={}, 天={}, 调拨1车({}条): {}班(胎胚{},stockHours={}) -> {}班(胎胚{}), "
                            + "{}班 {}->{} , {}班 {}->{}",
                    machineCode, day, tripCap, maxShift, bestEmbryo, bestStockHours,
                    minShift, bestEmbryo,
                    maxShift, maxBefore, maxBefore - tripCap,
                    minShift, minBefore, minBefore + tripCap);

            transferCount++;
        }

        return transferCount;
    }

    /**
     * 按结构 -> 机台 聚合任务。
     *
     * <p>可参与任务（非试制/量试/收尾）计入 {@code eligibleTasks} 进入精排通道；
     * 排除任务计入 {@code excludedTasks}（量不变但占用机台物理产能）。
     *
     * @param allAllocations 机台分配结果列表
     * @return Map&lt;结构名, Map&lt;机台编码, MachineAgg&gt;&gt;
     */
    private Map<String, Map<String, MachineAgg>> buildStructMachineAgg(List<MachineAllocationResult> allAllocations) {
        Map<String, Map<String, MachineAgg>> result = new LinkedHashMap<>();
        for (MachineAllocationResult mar : allAllocations) {
            String machineCode = mar.getMachineCode();
            if (machineCode == null || mar.getTaskAllocations() == null) {
                continue;
            }
            for (TaskAllocation ta : mar.getTaskAllocations()) {
                String structure = ta.getStructureName();
                if (structure == null) {
                    continue;
                }
                MachineAgg agg = result
                        .computeIfAbsent(structure, k -> new LinkedHashMap<>())
                        .computeIfAbsent(machineCode, k -> new MachineAgg(machineCode));
                int qty = getTaskQty(ta);
                int vulc = ta.getVulcanizeMachineCount() != null ? ta.getVulcanizeMachineCount() : 0;
                // 所有任务计入总量和负荷
                agg.actualQty += qty;
                agg.load += vulc;
                // 可参与任务进精排通道；排除任务（试制/量试/收尾）量不变但占用机台物理产能
                if (!isTaskExcluded(ta)) {
                    agg.eligibleQty += qty;
                    agg.eligibleTasks.add(ta);
                    agg.embryoTaskMap.putIfAbsent(ta.getEmbryoCode(), ta);
                } else {
                    agg.excludedTasks.add(ta);
                }
            }
        }
        return result;
    }

    /**
     * 判断任务是否排除出均衡（试制/量试/收尾任务不参与）。
     *
     * @param ta 任务分配
     * @return true 表示排除
     */
    private boolean isTaskExcluded(TaskAllocation ta) {
        return Boolean.TRUE.equals(ta.getIsTrialTask())
                || Boolean.TRUE.equals(ta.getIsProductionTrial())
                || Boolean.TRUE.equals(ta.getIsEndingTask());
    }

    /**
     * 获取任务实际排产量（优先 endingExtraInventory，其次 quantity）。
     *
     * @param ta 任务分配
     * @return 实际条数
     */
    private int getTaskQty(TaskAllocation ta) {
        Integer eei = ta.getEndingExtraInventory();
        if (eei != null) {
            return eei;
        }
        Integer q = ta.getQuantity();
        return q != null ? q : 0;
    }

    /**
     * 调整任务排产量（同步更新 quantity 和 endingExtraInventory）。
     *
     * @param ta    任务分配
     * @param delta 增减量（正=增加，负=减少）
     */
    private void adjustTaskQty(TaskAllocation ta, int delta) {
        int newQty = getTaskQty(ta) + delta;
        ta.setQuantity(newQty);
        ta.setEndingExtraInventory(newQty);
    }

    /**
     * 获取结构整车容量（同结构取任一胎胚查询，无匹配时用默认值）。
     *
     * @param structure   结构名称
     * @param machineAggs 机台聚合
     * @param context     排程上下文
     * @return 整车容量（条）
     */
    private int getTripCapForStructure(String structure, Map<String, MachineAgg> machineAggs, ScheduleContextVo context) {
        for (MachineAgg agg : machineAggs.values()) {
            for (TaskAllocation ta : agg.eligibleTasks) {
                if (ta.getEmbryoCode() != null) {
                    return productionCalculator.getTripCapacity(structure, ta.getEmbryoCode(), context);
                }
            }
        }
        return context.getDefaultTripCapacity() != null ? context.getDefaultTripCapacity() : 12;
    }

    // (已删除) buildShiftHistoryIndex / loadShiftHistoryBound：跨班次历史钳制逻辑，
    // 随 S5.3.MQ 改造为机台级三轮精排后不再需要（均衡分配阶段已定机台归属，物理产能封顶后排量天然稳定）。

    // (已删除) transferWithinStructure：结构内多机台调拨均衡逻辑（含合理量 target / 历史区间钳制），
    // 随 S5.3.MQ 改造为机台级三轮精排（MQ-R1/R2/R3）后不再需要。

    /**
     * 每车调整后更新任务的 stockHours（基于 projectedStock 变化重新计算）。
     * <p>公式：stockHours = projectedStock × 24 / (dailyLhCapacity × moldQty)
     * <br>调整 ±tripCap 条 -> projectedStock ±tripCap -> stockHours ±calculateStockHours(tripCap, singleLhCap, moldQty)
     *
     * @param task          被调整的任务
     * @param deltaQty      调整条数（正=加产，负=减产）
     * @param context       排程上下文
     * @param lhIdToMoldQty lhId -> 模数映射
     */
    private void updateTaskStockHours(TaskAllocation task, int deltaQty, ScheduleContextVo context,
                                      Map<Long, Integer> lhIdToMoldQty) {
        int singleLhCap = productionCalculator.getSingleMoldDailyLhCapacity(task.getMaterialCode(), context);
        if (singleLhCap <= 0) {
            return;
        }
        int moldQty = lhIdToMoldQty.getOrDefault(task.getLhId(), 1);
        if (moldQty <= 0) {
            return;
        }
        BigDecimal delta = productionCalculator.calculateStockHours(Math.abs(deltaQty), singleLhCap, moldQty);
        BigDecimal currentSh = task.getStockHours() != null ? task.getStockHours() : BigDecimal.ZERO;
        if (deltaQty > 0) {
            task.setStockHours(currentSh.add(delta));
        } else {
            task.setStockHours(currentSh.subtract(delta));
        }
    }


    /**
     * 单日试制/量试 SKU 上限与周日约束（直接修改 taskGroup 三个列表）。
     *
     * <p>规则：胎胚编码去重计数 ≤ {@code maxTrialSkuPerDay}；周日且不允许时清空全部试制/量试任务。
     * 跨机台、跨班次在同一 {@code dailyTrialAssignedMaterialCodes} 上累计。
     */
    private void applyDailyTrialSkuLimit(ScheduleContextVo context, TaskGroupResultVo taskGroup) {
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
        List<DailyEmbryoTask> filteredTrialTasks = new ArrayList<>();
        for (DailyEmbryoTask task : taskGroup.getTrialTasks()) {
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
        List<DailyEmbryoTask> filteredNewTasks = new ArrayList<>();
        for (DailyEmbryoTask task : taskGroup.getNewTasks()) {
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
        List<DailyEmbryoTask> filteredContinueTasks = new ArrayList<>();
        for (DailyEmbryoTask task : taskGroup.getContinueTasks()) {
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
        boolean isMorningShift = scheduleDayTypeHelper.isMorningShift(shiftConfig);
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
                        double hourlyCapacity = shiftScheduleService.getMachineHourlyCapacity(
                                allocation.getMachineCode(), ta.getMaterialCode(), ta.getStructureName(), context);
                        if (hourlyCapacity > 0) {
                            int qty = ta.getEndingExtraInventory() != null
                                    ? ta.getEndingExtraInventory()
                                    : (ta.getQuantity() != null ? ta.getQuantity() : 0);
                            taskHours += qty / hourlyCapacity;
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
            double hourlyCapacity = shiftScheduleService.getMachineHourlyCapacity(
                    machineCode, ta.getMaterialCode(), ta.getStructureName(), context);
            if (hourlyCapacity > 0) {
                int qty = ta.getEndingExtraInventory() != null
                        ? ta.getEndingExtraInventory()
                        : (ta.getQuantity() != null ? ta.getQuantity() : 0);
                totalTaskHours += qty / hourlyCapacity;
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

        int classIndex = productionCalculator.parseClassIndex(shiftConfig);
        log.info("精度扣量硫化联动: 机台={}, lhResultCache大小={}, classIndex={}",
                machineCode, lhResultCache.size(), classIndex);
        Set<LhScheduleResult> modifiedLhResults = new HashSet<>();

        for (TaskAllocation taskAlloc : sortedTasks) {
            if (remainingSeconds <= 0) break;

            double hourlyCapacity = shiftScheduleService.getMachineHourlyCapacity(
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
            Integer currentClassPlanObj = productionCalculator.getClassPlanQtyByIndex(lhResult, classIndex);
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
                productionCalculator.appendClassAnalysisByIndex(lhResult, classIndex, precisionNote);
                productionCalculator.setClassPlanQtyByIndex(lhResult, classIndex, totalAvailable);
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

    // parseClassIndex 已移至 ProductionCalculator

    // setClassPlanQtyByIndex 已移至 ProductionCalculator

    // appendClassAnalysisByIndex 已移至 ProductionCalculator


    /**
     * 更新机台在产状态（滚动替换：仅保留本班次分配结果，作为下一班次的续作历史）
     * <p>语义：班次N均衡分配的胎胚仅作为班次N+1的"续作历史"，不累积之前班次和MES在机数据
     */
    private Map<String, Set<String>> updateMachineOnlineStatus(
            List<MachineAllocationResult> allocations,
            Map<String, Set<String>> currentMachineOnlineMap) {

        // 仅用本班次分配结果构建新Map，不拷贝currentMachineOnlineMap（滚动替换而非累积合并）
        Map<String, Set<String>> newMap = new HashMap<>();
        for (MachineAllocationResult allocation : allocations) {
            for (TaskAllocation taskAlloc : allocation.getTaskAllocations()) {
                if (taskAlloc.getEmbryoCode() != null) {
                    newMap.computeIfAbsent(taskAlloc.getEmbryoCode(), k -> new HashSet<>())
                            .add(allocation.getMachineCode());
                }
            }
        }

        log.debug("更新机台在产状态完成（滚动替换），共 {} 个胎胚: {}", newMap.size(), formatMachineEmbryoMap(newMap));
        return newMap;
    }

    /**
     * 更新前序班次机台胎胚负荷映射，供下一班次的保底预留参考。
     *
     * <p>从当前班次的 allAllocations 中提取每个机台每个胎胚的硫化机台数（vulcanizeMachineCount），
     * 按 lhMachineCode 去重后汇总，写入 context.previousShiftMachineEmbryoLoadMap。
     *
     * @param context        排程上下文
     * @param allAllocations 当前班次的全部机台分配结果
     */
    private void updatePreviousShiftMachineEmbryoLoadMap(
            ScheduleContextVo context,
            List<MachineAllocationResult> allAllocations) {
        Map<String, Map<String, Integer>> loadMap = new HashMap<>();
        for (MachineAllocationResult allocation : allAllocations) {
            String machineCode = allocation.getMachineCode();
            for (TaskAllocation taskAlloc : allocation.getTaskAllocations()) {
                String embryoCode = taskAlloc.getEmbryoCode();
                if (embryoCode == null || embryoCode.isEmpty()) {
                    continue;
                }
                int lhCount = taskAlloc.getVulcanizeMachineCount() != null
                        ? taskAlloc.getVulcanizeMachineCount() : 0;
                if (lhCount > 0) {
                    loadMap.computeIfAbsent(machineCode, k -> new HashMap<>())
                            .merge(embryoCode, lhCount, Integer::sum);
                }
            }
        }
        context.setPreviousShiftMachineEmbryoLoadMap(loadMap);
        log.info("更新前序班次负荷映射: {} 台机台, 共 {} 条胎胚记录",
                loadMap.size(), loadMap.values().stream().mapToInt(Map::size).sum());
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


    // ==================== 2.5 主表汇总（机台+胎胚+物料 → CLASS1~8） ====================

    /**
     * 2.5 多班次精排结果汇总为 {@link CxScheduleResult} 主表（机台+胎胚+施工阶段 → CLASS1~8）。
     *
     * <p>步骤：shiftCode→classField 映射 → 按 taskKey 合并各班产量 → 新开规格判定 →
     * 写主表字段 → {@link #fixEndingFlagsPerClass} 修正收尾标记。
     */
    private List<CxScheduleResult> buildFinalScheduleResultsFromShifts(
            ScheduleContextVo context,
            List<ShiftScheduleResult> shiftResults,
            List<CxShiftConfig> allShiftConfigs,
            Map<String, Integer> initialEmbryoStockMap) {

        // 2.5.1 构建 shiftCode+scheduleDay → CLASS 字段映射
        Map<String, String> shiftClassFieldMap = new HashMap<>();
        for (CxShiftConfig shiftConfig : allShiftConfigs) {
            String key = shiftConfig.getShiftCode() + "_" + shiftConfig.getScheduleDay();
            shiftClassFieldMap.put(key, shiftConfig.getClassField());
        }

        // 2.5.1 按 机台|胎胚|施工阶段 三维汇总班次排量（同 CLASS 合并 quantity）
        Map<String, Map<String, ShiftProductionResult>> taskClassSprMap = new LinkedHashMap<>();
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

            for (ShiftProductionResult spr : shiftResult.getShiftProductionResults()) {
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

                String productStatus = spr.getProductStatus() != null ? spr.getProductStatus() : "";
                String constructionStage = spr.getConstructionStage() != null ? spr.getConstructionStage() : "";
                String taskKey = machineCode + "|" + embryoCode + "|" + productStatus + "|" + constructionStage;
                // 独立追踪每个embryo+productStatus+constructionStage下的所有materialCode（不按机台拆分）
                String embryoTaskKey = embryoCode + "|" + productStatus + "|" + constructionStage;
                if (!materialCode.isEmpty()) {
                    taskMaterialCodeMap.computeIfAbsent(embryoTaskKey, k -> new LinkedHashSet<>()).add(materialCode);
                }
                taskClassSprMap.computeIfAbsent(taskKey, k -> new LinkedHashMap<>())
                        .compute(effectiveClassField, (k, existing) -> {
                            if (existing == null) {
                                // 初始化物料收尾追踪集合
                                Set<String> allMats = new LinkedHashSet<>();
                                if (spr.getMaterialCode() != null) {
                                    allMats.add(spr.getMaterialCode());
                                }
                                spr.setAllMaterialCodes(allMats);
                                Set<String> endingMats = new LinkedHashSet<>();
                                if (isSprEnding(spr) && spr.getMaterialCode() != null) {
                                    endingMats.add(spr.getMaterialCode());
                                }
                                spr.setEndingMaterialCodes(endingMats);
                                return spr;
                            }
                            ShiftProductionResult merged = new ShiftProductionResult();
                            merged.setMachineCode(existing.getMachineCode());
                            merged.setEmbryoCode(existing.getEmbryoCode());
                            merged.setMaterialCode(existing.getMaterialCode());
                            merged.setMaterialDesc(existing.getMaterialDesc());
                            merged.setMainMaterialDesc(existing.getMainMaterialDesc());
                            merged.setStructureName(existing.getStructureName());
                            merged.setProductStatus(existing.getProductStatus());
                            merged.setConstructionStage(existing.getConstructionStage());
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
                            DailyEmbryoTask existingTask = existing.getSourceTask();
                            DailyEmbryoTask sprTask = spr.getSourceTask();
                            boolean hasUrgentEnding = (existingTask != null && Boolean.TRUE.equals(existingTask.getIsUrgentEnding()))
                                    || (sprTask != null && Boolean.TRUE.equals(sprTask.getIsUrgentEnding()));
                            if (hasUrgentEnding || existingTask != null) {
                                DailyEmbryoTask mergedTask = new DailyEmbryoTask();
                                mergedTask.setEmbryoCode(existingTask != null ? existingTask.getEmbryoCode() : (sprTask != null ? sprTask.getEmbryoCode() : null));
                                mergedTask.setMaterialCode(existingTask != null ? existingTask.getMaterialCode() : (sprTask != null ? sprTask.getMaterialCode() : null));
                                mergedTask.setProductStatus(existingTask != null ? existingTask.getProductStatus()
                                        : (sprTask != null ? sprTask.getProductStatus() : null));
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
                                // ---- 合并 isLastEndingBatch：从 SPR 自身标记合并（而非 sourceTask），
                                // scheduleEndingTask 已按 isLastProductive 精确设置每个班次的 SPR 标记
                                // sourceTask 层面的 isLastEndingBatch 不再作为判断依据
                                mergedTask.setIsLastEndingBatch(
                                        Boolean.TRUE.equals(existing.getIsLastEndingBatch())
                                                || Boolean.TRUE.equals(spr.getIsLastEndingBatch()));
                                merged.setSourceTask(mergedTask);
                                merged.setIsLastEndingBatch(mergedTask.getIsLastEndingBatch());
                            } else if (sprTask != null) {
                                merged.setSourceTask(sprTask);
                            }

                            // 注意：isLastEndingBatch 不在此处合并，每个班次保持独立状态

                            // ---- 合并物料收尾追踪集合 ----
                            Set<String> allMats = new LinkedHashSet<>();
                            if (existing.getAllMaterialCodes() != null) {
                                allMats.addAll(existing.getAllMaterialCodes());
                            } else if (existing.getMaterialCode() != null) {
                                allMats.add(existing.getMaterialCode());
                            }
                            if (spr.getAllMaterialCodes() != null) {
                                allMats.addAll(spr.getAllMaterialCodes());
                            } else if (spr.getMaterialCode() != null) {
                                allMats.add(spr.getMaterialCode());
                            }
                            merged.setAllMaterialCodes(allMats);

                            Set<String> endingMats = new LinkedHashSet<>();
                            if (existing.getEndingMaterialCodes() != null) {
                                endingMats.addAll(existing.getEndingMaterialCodes());
                            } else if (isSprEnding(existing) && existing.getMaterialCode() != null) {
                                endingMats.add(existing.getMaterialCode());
                            }
                            if (spr.getEndingMaterialCodes() != null) {
                                endingMats.addAll(spr.getEndingMaterialCodes());
                            } else if (isSprEnding(spr) && spr.getMaterialCode() != null) {
                                endingMats.add(spr.getMaterialCode());
                            }
                            merged.setEndingMaterialCodes(endingMats);

                            return merged;
                        });
                taskTotalQtyMap.merge(taskKey, spr.getQuantity() != null ? spr.getQuantity() : 0, Integer::sum);
                if (spr.getStructureName() != null) {
                    taskStructureMap.putIfAbsent(taskKey, spr.getStructureName());
                }
            }
        }

        // ==================== 收尾标记修正：只有每个 taskKey 最后一个有产量的班次才保留收尾标记 ====================
        // scheduleEndingTask 在单班次模式下 isLastProductive 恒为 true，
        // 导致所有班次 SPR 都被标记为收尾。此处汇总后按 CLASS 索引找出真正的最后班次。
        fixEndingFlagsPerClass(taskClassSprMap);

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

        // ---- 胎胚库存映射（使用排程前初始快照，排程循环中 updateCxStockEntities 会修改 context.getStocks()） ----
        Map<String, Integer> embryoStockMap = initialEmbryoStockMap != null ? initialEmbryoStockMap : new HashMap<>();

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

        // ==================== 预计算：机台维度新开规格判定 ====================
        // 定义：同一机台内出现2+种结构时，按班次顺序(CLASS1~8)判定
        //       最早有产量的班次所属结构=前结构，其余结构=后结构(新开规格)
        Set<String> newSpecKeys = new HashSet<>();  // machineCode|structureName
        {
            // 1. 收集每个机台每个结构的最早有产量班次序号
            Map<String, Map<String, Integer>> machineStructEarliestClass = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, ShiftProductionResult>> e : taskClassSprMap.entrySet()) {
                String[] kParts = e.getKey().split("\\|", 3);
                String mCode = kParts[0];
                String structName = taskStructureMap.get(e.getKey());
                if (structName == null || structName.isEmpty()) {
                    continue;
                }
                int earliestClass = Integer.MAX_VALUE;
                for (Map.Entry<String, ShiftProductionResult> ce : e.getValue().entrySet()) {
                    ShiftProductionResult spr = ce.getValue();
                    if (spr != null && spr.getQuantity() != null && spr.getQuantity() > 0) {
                        int classNum = productionCalculator.parseClassIndex(ce.getKey());
                        if (classNum > 0 && classNum < earliestClass) {
                            earliestClass = classNum;
                        }
                    }
                }
                if (earliestClass == Integer.MAX_VALUE) {
                    continue;
                }
                machineStructEarliestClass
                        .computeIfAbsent(mCode, k -> new LinkedHashMap<>())
                        .merge(structName, earliestClass, Math::min);
            }
            // 2. 每个机台：2+种结构时，按最早班次序号排序，第一个=前结构，其余=新开规格
            for (Map.Entry<String, Map<String, Integer>> me : machineStructEarliestClass.entrySet()) {
                Map<String, Integer> structClassMap = me.getValue();
                if (structClassMap.size() < 2) {
                    continue;
                }
                List<Map.Entry<String, Integer>> sorted = new ArrayList<>(structClassMap.entrySet());
                sorted.sort(Map.Entry.comparingByValue());
                for (int i = 1; i < sorted.size(); i++) {
                    newSpecKeys.add(me.getKey() + "|" + sorted.get(i).getKey());
                }
            }
            log.info("新开规格预计算完成: newSpecKeys={}", newSpecKeys);
        }

        for (Map.Entry<String, Map<String, ShiftProductionResult>> entry : taskClassSprMap.entrySet()) {
            String taskKey = entry.getKey();
            Map<String, ShiftProductionResult> classSprMap = entry.getValue();

            String[] parts = taskKey.split("\\|", 4);
            String machineCode = parts[0];
            String embryoCode = parts.length > 1 ? parts[1] : null;
            String productStatus = parts.length > 2 && !parts[2].isEmpty() ? parts[2] : null;
            String constructionStage = parts.length > 3 && !parts[3].isEmpty() ? parts[3] : null;
            // materialCode 从独立的 taskMaterialCodeMap 中获取（embryo级别，不按机台拆分）
            String embryoMaterialKey = embryoCode + "|" + (productStatus != null ? productStatus : "")
                    + "|" + (constructionStage != null ? constructionStage : "");
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

            // ---- 库存信息（直接取胎胚级库存，不依赖任务是否排程） ----
            String embryoLhKey = embryoCode + "|" + (constructionStage != null ? constructionStage : "");
            List<Long> lhIdList = taskLhIdListMap.get(embryoLhKey);
            int totalStock = embryoStockMap.getOrDefault(embryoCode, 0);
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
                if (initialMonthSurplusMap != null && context.getMonthSurplusMap() != null) {
                    for (Map.Entry<String, MdmMonthSurplus> surplusEntry : context.getMonthSurplusMap().entrySet()) {
                        MdmMonthSurplus monthSurplus = surplusEntry.getValue();
                        if (monthSurplus == null || !materialCodeSet.contains(monthSurplus.getMaterialCode())) {
                            continue;
                        }
                        BigDecimal surplus = initialMonthSurplusMap.get(surplusEntry.getKey());
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
                if (initialFormingRemainderMap != null && !materialCodeSet.isEmpty()
                        && context.getMonthSurplusMap() != null) {
                    int totalCxRemain = 0;
                    for (Map.Entry<String, MdmMonthSurplus> surplusEntry : context.getMonthSurplusMap().entrySet()) {
                        MdmMonthSurplus monthSurplus = surplusEntry.getValue();
                        if (monthSurplus == null || !materialCodeSet.contains(monthSurplus.getMaterialCode())) {
                            continue;
                        }
                        Integer cxRemain = initialFormingRemainderMap.get(surplusEntry.getKey());
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

            // ---- 收尾提示 & 颜色标记：遍历班次判断是否有"全部收尾" ----
            boolean hasEndingShift = false;       // 任一班次标记"全部收尾"（与 ANALYSIS="收尾" 口径一致）
            boolean hasTrialOrProductionTrial = false;
            log.info("开始遍历classSprMap.values()设置颜色标记: machineCode={}, embryoCode={}, materialCode={}, classSprMap.size={}",
                    machineCode, embryoCode, materialCode, classSprMap.size());
            int sprIndex = 0;
            for (ShiftProductionResult spr : classSprMap.values()) {
                sprIndex++;
                if (spr == null) {
                    continue;
                }
                // 班次级"全部收尾"判定：endingMaterialCodes 包含 allMaterialCodes（与 buildTaskAnalysis 生成"收尾"文本口径一致）
                Set<String> endingMats = spr.getEndingMaterialCodes();
                if (endingMats != null && !endingMats.isEmpty()) {
                    Set<String> allMats = spr.getAllMaterialCodes();
                    boolean allEnding = (allMats == null || allMats.isEmpty() || endingMats.containsAll(allMats));
                    log.info("  spr[{}] 班次={} endingMats={}, allMats={}, allEnding={}",
                            sprIndex, spr.getShiftCode(), endingMats, allMats, allEnding);
                    if (allEnding) {
                        hasEndingShift = true;
                    }
                }
                // 试制/量试标记从 sourceTask 获取
                DailyEmbryoTask srcTask = spr.getSourceTask();
                if (srcTask != null) {
                    log.info("  spr[{}] sourceTask: isTrialTask={}, isProductionTrial={}",
                            sprIndex, srcTask.getIsTrialTask(), srcTask.getIsProductionTrial());
                    if (Boolean.TRUE.equals(srcTask.getIsTrialTask()) || Boolean.TRUE.equals(srcTask.getIsProductionTrial())) {
                        hasTrialOrProductionTrial = true;
                    }
                }
            }
            // 收尾提示：班次全部收尾 或 成型余量<=0
            if (hasEndingShift || (result.getCxRemainQty() != null && result.getCxRemainQty().compareTo(BigDecimal.ZERO) <= 0)) {
                result.setMarkCloseOutTip("0");
            } else {
                result.setMarkCloseOutTip("1");
            }

            // ---- 新开规格判定（机台维度：同机台2+种结构时，后结构=新开规格） ----
            String effectiveStructName = result.getStructureName() != null ? result.getStructureName() : structureName;
            boolean hasNewSpec = effectiveStructName != null
                    && newSpecKeys.contains(machineCode + "|" + effectiveStructName);

            // ---- 颜色标记（前端展示用） ----
            // orange: 任一班次"全部收尾"（对应 ANALYSIS="收尾"），部分收尾(物料XXX收尾)不标记
            // yellow: 新开规格（机台内2+种结构，后结构）
            log.info("设置颜色标记: machineCode={}, embryoCode={}, materialCode={}, structureName={}, hasTrialOrProductionTrial={}, hasEndingShift={}, hasNewSpec={}, newSpecKeys={}",
                    machineCode, embryoCode, materialCode, effectiveStructName, hasTrialOrProductionTrial, hasEndingShift, hasNewSpec, newSpecKeys);
            if (hasTrialOrProductionTrial) {
                result.setColorTag("blue");
            } else if (hasEndingShift) {
                result.setColorTag("orange");
            } else if (hasNewSpec) {
                result.setColorTag("yellow");
            }

            // ---- 示方书类型从SKU与示方书关系获取（constructionStage映射为trialStatus后匹配） ----
            String recipeType = resolveRecipeType(skuRecipeTypeMap, materialCode, constructionStage);

            // ---- 找到第一个有排量的班次（头班），"新增"文本仅在头班标识 ----
            String firstProductiveClass = null;
            for (Map.Entry<String, ShiftProductionResult> classEntry : classSprMap.entrySet()) {
                ShiftProductionResult spr = classEntry.getValue();
                if (spr != null && spr.getQuantity() != null && spr.getQuantity() > 0) {
                    firstProductiveClass = classEntry.getKey();
                    break;
                }
            }

            // ---- 映射班次排量到 CLASS1~8 ----
            for (Map.Entry<String, ShiftProductionResult> classEntry : classSprMap.entrySet()) {
                boolean isFirstProductive = classEntry.getKey().equals(firstProductiveClass);
                setClassFieldValue(result, classEntry.getKey(), classEntry.getValue(), primaryLh, recipeType, newSpecKeys, isFirstProductive);
            }

            // ---- 班次未排量的栏位补零 ----
            fillDefaultClassValues(result, classSprMap.keySet());

            results.add(result);
        }

        log.info("最终排程结果（按班次合并）：共 {} 条记录（机台+胎胚+SAP物料维度）", results.size());
        return results;
    }

    // ==================== 2.7 子表构建（机台+胎胚+车次 → CLASS1~8） ====================

    /**
     * 2.7 构建子表 — 维度：机台+胎胚+车次；8 班合并为 CLASS1~8 一条 {@link CxScheduleDetail}。
     *
     * <p>三阶段：按班合并车次 → 班内按库存可供硫化时长排序赋顺位 → 按机台|胎胚|车次号合并。
     * stockHours 使用各班 {@link ShiftScheduleResult#getMaterialStockSnapshot}。
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
        // 子表车次合并数：默认1车一条，配置N则N车合并一条
        int tripGroupSize = context.getDetailTripGroupSize() != null && context.getDetailTripGroupSize() > 0
                ? context.getDetailTripGroupSize() : 1;
        // 每个班次的车次记录列表（用于后续排序分配顺位和合并）
        List<List<TripRecord>> perShiftTrips = new ArrayList<>();

        for (ShiftScheduleResult shiftResult : shiftResults) {
            int day = shiftResult.getDay();
            String shiftClassField = shiftResult.getShiftConfig() != null
                    ? shiftResult.getShiftConfig().getClassField() : null;
            List<TripRecord> currentShiftTrips = new ArrayList<>();
            // 本班次的 materialStockMap 快照（排程前的分配库存）
            Map<String, Integer> stockSnapshot = shiftResult.getMaterialStockSnapshot();

            // ---- 合并步骤：按 机台+胎胚+施工阶段 汇总当班排产量（同主表merge逻辑）----
            // mergeKey = machineCode|embryoCode|constructionStage（剔除物料编码维度）
            // 同时收集每个合并组的 lhId 列表（用于查询模数、日硫化量、分配库存、硫化消耗）
            Map<String, ShiftProductionResult> mergedSprMap = new LinkedHashMap<>();
            Map<String, List<Long>> mergeKeyLhIdMap = new HashMap<>();

            for (ShiftProductionResult spr : shiftResult.getShiftProductionResults()) {
                if (spr.getQuantity() == null || spr.getQuantity() <= 0) continue;

                String mCode = spr.getMachineCode();
                String eCode = spr.getEmbryoCode();
                String constructionStage = spr.getConstructionStage() != null ? spr.getConstructionStage() : "";
                String mergeKey = mCode + "|" + eCode + "|" + constructionStage;

                // 收集 lhId
                if (spr.getSourceTask() != null && spr.getSourceTask().getLhId() != null) {
                    mergeKeyLhIdMap.computeIfAbsent(mergeKey, k -> new ArrayList<>())
                            .add(spr.getSourceTask().getLhId());
                }

                ShiftProductionResult existing = mergedSprMap.get(mergeKey);
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
                }
            }

            // ---- 从合并后的排产结果生成车次 ----
            for (ShiftProductionResult spr : mergedSprMap.values()) {
                String embryoCode = spr.getEmbryoCode();
                String materialCode = spr.getMaterialCode() != null ? spr.getMaterialCode() : "";
                String constructionStage = spr.getConstructionStage() != null ? spr.getConstructionStage() : "";
                String mergeKey = spr.getMachineCode() + "|" + embryoCode + "|" + constructionStage;

                // 基于合并组的 lhId 列表，计算 stockHours 所需的四个参数
                List<Long> lhIdList = mergeKeyLhIdMap.getOrDefault(mergeKey, Collections.emptyList());

                // 1. 模数累加：Σ(lhResult.mouldQty)
                // 2. 日硫化量平均值(单模)：Σ(dayVulcanizationQty/2) / lhId数量，按参数模式选择的日硫化量
                // 3. 分配库存合计：Σ(materialStockSnapshot[lhId])
                // 4. 硫化消耗合计：Σ(lhResult.classXPlanQty)，当前班次
                String classField = shiftClassField;
                if (classField == null) {
                    classField = shiftToClassField.getOrDefault(spr.getShiftCode(), spr.getShiftCode());
                }
                int vulcanizeClassIndex = productionCalculator.parseClassIndex(classField);

                int totalMoldQty = 0;
                int totalAllocatedStock = 0;
                int totalVulcanizeConsumption = 0;
                int sumSingleMoldDailyCap = 0;
                int validCapCount = 0;

                for (Long lhId : lhIdList) {
                    LhScheduleResult lhResult = lhResultMap.get(lhId);
                    if (lhResult != null) {
                        // 模数
                        int moldQty = lhResult.getMouldQty() != null ? lhResult.getMouldQty() : 1;
                        totalMoldQty += moldQty;
                        // 硫化消耗（当前班次）
                        Integer consumption = productionCalculator.getClassPlanQtyByIndex(lhResult, vulcanizeClassIndex);
                        if (consumption != null) {
                            totalVulcanizeConsumption += consumption;
                        }
                        // 日硫化量（按参数模式的 dayVulcanizationQty，双模÷2得单模）
                        String lhMaterialCode = lhResult.getMaterialCode();
                        if (lhMaterialCode != null && context.getMaterialLhCapacityMap() != null) {
                            MonthPlanProductLhCapacityVo capVo = context.getMaterialLhCapacityMap().get(lhMaterialCode);
                            if (capVo != null && capVo.getDayVulcanizationQty() != null
                                    && capVo.getDayVulcanizationQty() > 0) {
                                sumSingleMoldDailyCap += capVo.getDayVulcanizationQty() / 2;
                                validCapCount++;
                            }
                        }
                    }
                    // 分配库存（从快照获取）
                    if (stockSnapshot != null) {
                        totalAllocatedStock += stockSnapshot.getOrDefault(String.valueOf(lhId), 0);
                    }
                }

                // 日硫化量平均值（单模）
                int avgSingleMoldDailyCap = validCapCount > 0 ? sumSingleMoldDailyCap / validCapCount : 0;

                // 单胎单模时长(秒) = 86400 / 平均日硫化量(单模)
                double singleTireMoldSeconds = avgSingleMoldDailyCap > 0
                        ? (double) ScheduleConstants.SECONDS_PER_DAY / avgSingleMoldDailyCap : 0;

                // 获取小时产能（用于车次时间计算）
                int hourlyCapacity = 12;
                DailyEmbryoTask task = spr.getSourceTask();
                if (task != null) {
                    if (task.getHourCapacity() != null && task.getHourCapacity() > 0) {
                        hourlyCapacity = task.getHourCapacity();
                    } else {
                        hourlyCapacity = productionCalculator.calculateHourlyCapacity(
                                spr.getMachineCode(), materialCode, task.getStructureName(), context);
                    }
                }

                // 硫化机台数（用于记录字段）
                int vulcanizeMachineCount = task != null && task.getVulcanizeMachineCount() != null
                        ? task.getVulcanizeMachineCount() : 1;

                int tripCapacity = spr.getTripCapacity() != null ? spr.getTripCapacity() : 12;
                int planQty = spr.getQuantity() != null ? spr.getQuantity() : 0;

                // 车次拆分：按 tripGroupSize 车合并为一条记录
                int groupTripCapacity = tripCapacity * tripGroupSize;
                int tripCount = groupTripCapacity > 0
                        ? (planQty + groupTripCapacity - 1) / groupTripCapacity : 0;

                // 为每个车次组创建 TripRecord（车次号从1开始）
                for (int i = 1; i <= tripCount; i++) {
                    // 本车次组的计划量
                    int tripPlanQty = Math.min(groupTripCapacity, planQty - (i - 1) * groupTripCapacity);
                    // 累计成型产出（到本车次为止）
                    int cumulativeForming = Math.min(groupTripCapacity * i, planQty);

                    // 任务预计班后库存 = 分配库存 + 累计成型产出 - 硫化消耗（整个班次）
                    int projectedStock = totalAllocatedStock + cumulativeForming - totalVulcanizeConsumption;

                    // 可供硫化时长 = 预计库存 × 单胎单模时长 / 总模数 / 3600
                    double stockHours = 0;
                    if (totalMoldQty > 0 && singleTireMoldSeconds > 0) {
                        stockHours = (double) projectedStock * singleTireMoldSeconds
                                / ScheduleConstants.SECONDS_PER_HOUR / totalMoldQty;
                    }

                    // 计算车次时间
                    LocalDateTime tripStartTime = null;
                    LocalDateTime tripEndTime = null;
                    if (spr.getPlanStartTime() != null && hourlyCapacity > 0) {
                        LocalDateTime shiftStart = spr.getPlanStartTime();

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
                    record.setVulcanizeMachineCount(vulcanizeMachineCount);

                    currentShiftTrips.add(record);
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

    // TripRecord 已移至 com.zlt.aps.cx.vo.TripRecord

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
     * @param newSpecKeys 新开规格集合（透传给 buildTaskAnalysis）
     * @param isFirstProductive 是否为第一个有排量的班次（头班）
     */
    private void setClassFieldValue(CxScheduleResult result, String classField, ShiftProductionResult spr,
                                    LhScheduleResult primaryLh, String recipeType, Set<String> newSpecKeys,
                                    boolean isFirstProductive) {
        if (classField == null || spr == null) {
            return;
        }

        // 构建原因分析字符串
        String analysis = buildTaskAnalysis(spr, newSpecKeys, isFirstProductive);

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
     * 收尾标记修正：单班次调度模式下 scheduleEndingTask 的 isLastProductive 恒为 true，
     * 导致每个班次的 SPR 都被打上收尾标记。该方法在汇总后找出每个 taskKey 真正最后一个
     * 有产量的班次（CLASS索引最大者），清除其他班次的收尾标记。
     */
    private void fixEndingFlagsPerClass(
            Map<String, Map<String, ShiftProductionResult>> taskClassSprMap) {
        if (taskClassSprMap == null) return;

        for (Map.Entry<String, Map<String, ShiftProductionResult>> entry : taskClassSprMap.entrySet()) {
            Map<String, ShiftProductionResult> classSprMap = entry.getValue();
            if (classSprMap == null || classSprMap.isEmpty()) continue;

            // 找出最后一个有产量（quantity > 0）的 CLASS
            String lastEndingClass = null;
            int lastClassIndex = -1;
            for (int i = 1; i <= 8; i++) {
                String classField = "CLASS" + i;
                ShiftProductionResult spr = classSprMap.get(classField);
                if (spr != null && spr.getQuantity() != null && spr.getQuantity() > 0) {
                    lastEndingClass = classField;
                    lastClassIndex = i;
                }
            }

            if (lastEndingClass == null) continue;

            // 清除非最后班次的收尾标记
            for (Map.Entry<String, ShiftProductionResult> classEntry : classSprMap.entrySet()) {
                if (classEntry.getKey().equals(lastEndingClass)) continue;
                ShiftProductionResult spr = classEntry.getValue();
                if (spr != null) {
                    spr.setIsEndingTask(false);
                    spr.setIsLastEndingBatch(false);
                    if (spr.getEndingMaterialCodes() != null) {
                        spr.getEndingMaterialCodes().clear();
                    }
                }
            }
        }
    }

    /**
     * 判断班次排产结果是否为收尾任务
     * <p>检查 isLastEndingBatch、isEndingTask、endingAbandoned 等标记
     */
    private boolean isSprEnding(ShiftProductionResult spr) {
        if (spr == null) return false;
        boolean isLastEndingBatch = Boolean.TRUE.equals(spr.getIsLastEndingBatch());
        boolean isEndingTask = Boolean.TRUE.equals(spr.getIsEndingTask());
        if (isLastEndingBatch) return true;
        if (isEndingTask) return true;
        // 回退到 sourceTask 检查：仅保留 endingAbandoned（收尾舍弃），
        // isLastEndingBatch 和 isEndingTask 由班次级 SPR 标记决定（scheduleEndingTask 已按 isLastProductive 精确设置）
        DailyEmbryoTask task = spr.getSourceTask();
        if (task != null) {
            boolean endingAbandoned = Boolean.TRUE.equals(task.getEndingAbandoned());
            if (endingAbandoned) {
                return true;
            }
        }
        return false;
    }

    /**
     * 构建任务原因分析字符串
     * <p>根据任务类型组合原因标记，多个原因用逗号分隔
     *
     * @param spr         班次排产结果
     * @param newSpecKeys 新开规格集合（machineCode|structureName），同机台2+种结构时后结构为新开规格
     * @param isFirstProductive 是否为第一个有排量的班次（头班），"新增"仅在头班标识
     */
    private String buildTaskAnalysis(ShiftProductionResult spr, Set<String> newSpecKeys,
                                     boolean isFirstProductive) {
        if (spr == null) {
            return null;
        }

        List<String> reasons = new ArrayList<>();

        // 从 sourceTask 获取详细任务类型
        DailyEmbryoTask task = spr.getSourceTask();
        if (task != null) {
            if (Boolean.TRUE.equals(task.getIsTrialTask())) {
                reasons.add("试制");
            }
            if (Boolean.TRUE.equals(task.getIsProductionTrial())) {
                reasons.add("量试");
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
            // 新增：机台维度新开规格判定（同机台2+种结构时，后结构=新开规格），仅在头班标识
            if (isFirstProductive && newSpecKeys != null && spr.getMachineCode() != null && spr.getStructureName() != null
                    && newSpecKeys.contains(spr.getMachineCode() + "|" + spr.getStructureName())) {
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
            if (Boolean.TRUE.equals(spr.getIsContinueTask())) {
                // 续作任务不标记
            }
        }

        // ---- 收尾标记（基于合并物料追踪：全部收尾→"收尾"，部分收尾→"物料XXX收尾"） ----
        Set<String> endingMats = spr.getEndingMaterialCodes();
        Set<String> allMats = spr.getAllMaterialCodes();
        if (endingMats != null && !endingMats.isEmpty()) {
            boolean allEnding = (allMats == null || allMats.isEmpty() || endingMats.containsAll(allMats));
            if (allEnding) {
                reasons.add("收尾");
            } else {
                for (String mat : endingMats) {
                    reasons.add("物料" + mat + "收尾");
                }
            }
        }

        if (reasons.isEmpty()) {
            return null;
        }

        return String.join(",", reasons);
    }

    // ==================== 2.4.6 班次间上下文滚动（库存 / 余量 / 在机状态） ====================

    /**
     * 班次间上下文滚动入口 — 委托 {@link #updateContextForNextDay}。
     */
    private void updateContextForNextShift(
            ScheduleContextVo context,
            List<MachineAllocationResult> shiftAllocations,
            List<CxShiftConfig> shiftConfigs,
            CxShiftConfig currentShiftConfig,
            List<ShiftProductionResult> shiftProductionResults) {
        // 直接复用 updateContextForNextDay 逻辑，它已经按班次配置计算硫化消耗
        updateContextForNextDay(context, shiftAllocations, shiftConfigs, currentShiftConfig, shiftProductionResults);
    }

    /**
     * 每班次排程后滚动更新 context，供下一班 TaskGroupService 使用。
     *
     * <p><b>链式更新</b>：
     * <ol>
     *   <li>成型产出 — ShiftProductionResult.quantity 按胎胚汇总</li>
     *   <li>硫化消耗 — 按本班 CLASS 字段读 LhScheduleResult 计划量（胎胚+物料维度）</li>
     *   <li>CxStock / materialStockMap — 库存 ± 产出/消耗，共用胎胚按比例重分配</li>
     *   <li>monthSurplusMap — 硫化余量 -= 物料硫化消耗</li>
     *   <li>formingRemainderMap — 成型余量 = 硫化余量 − 胎胚库存汇总</li>
     *   <li>machineOnlineEmbryoMap — 由 executeSchedule 2.4.5 单独更新</li>
     * </ol>
     */
    private void updateContextForNextDay(
            ScheduleContextVo context,
            List<MachineAllocationResult> dayAllocations,
            List<CxShiftConfig> dayShifts,
            CxShiftConfig currentShiftConfig,
            List<ShiftProductionResult> shiftProductionResults) {

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

        // 2.2 按物料+产品状态汇总（用于更新硫化余量）
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
                                                                List<ShiftProductionResult> shiftProductionResults) {
        Map<String, Integer> outputMap = new HashMap<>();

        if (shiftProductionResults == null || shiftProductionResults.isEmpty()) {
            log.warn("【调试】shiftProductionResults 为空，无法计算成型产出");
            return outputMap;
        }

        log.debug("【调试】计算成型产出 - 当前班次={}, shiftProductionResults 数={}",
                currentShiftConfig != null ? currentShiftConfig.getShiftCode() : "未知",
                shiftProductionResults.size());

        // 从 ShiftProductionResult 中汇总成型产出
        for (ShiftProductionResult spr : shiftProductionResults) {
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
            int consumption = productionCalculator.getVulcanizingConsumptionForDay(lhResult, dayShifts);
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
            int consumption = productionCalculator.getVulcanizingConsumptionForDay(lhResult, dayShifts);
            if (consumption > 0) {
                String materialStatusKey = MonthPlanSurplusCalculator.buildMaterialStatusKey(
                        materialCode, lhResult.getProductStatus());
                vulcanizingConsumptionByMaterial.merge(materialStatusKey, consumption, Integer::sum);
            }
        }
    }

    // getVulcanizingConsumptionForDay 已移至 ProductionCalculator

    // getClassIndex 已移至 ProductionCalculator (parseClassIndex)

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

            int totalStock = stock.getEffectiveStock();
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
                if (productionCalculator.isVulcanizeSurplusExhausted(
                        task.getMaterialCode(), task.getProductStatus(), monthSurplusMap)) {
                    log.debug("胎胚 {} 硫化任务 {} 硫化余量<=0，跳过库存分配", embryoCode, taskKey);
                    continue;
                }

                int surplus = productionCalculator.getVulcanizingSurplus(
                        task.getMaterialCode(), task.getProductStatus(), monthSurplusMap);
                int allocatedStock = Math.min(totalStock, surplus);
                materialStockMap.merge(taskKey, allocatedStock, Integer::sum);
                log.debug("胎胚 {} 只对应硫化任务 {}，按状态余量上限分配库存 {}",
                        embryoCode, taskKey, allocatedStock);
            } else {
                // 胎胚对应多个硫化任务，按物料的日硫化量比例分配（与ScheduleServiceImpl.allocateStockByMaterialRatio一致）
                int totalDemand = 0;
                List<TaskDemandSimple> taskDemands = new ArrayList<>();

                for (LhScheduleResult lh : relatedTasks) {
                    String materialCode = lh.getMaterialCode();
                    int dayVulcanizationQty = 0;

                    // 检查硫化余量：如果已超产（<=0），跳过分配
                    if (productionCalculator.isVulcanizeSurplusExhausted(
                            materialCode, lh.getProductStatus(), monthSurplusMap)) {
                        log.debug("胎胚 {} 硫化任务 {} 物料 {} 硫化余量<=0，跳过库存分配",
                                embryoCode, lh.getId(), materialCode);
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

                    taskDemands.add(new TaskDemandSimple(
                            lh.getId(), dayVulcanizationQty, materialCode, lh.getProductStatus()));
                    totalDemand += dayVulcanizationQty;
                }

                if (taskDemands.isEmpty()) {
                    log.debug("胎胚 {} 所有硫化任务均被过滤，跳过分配", embryoCode);
                    continue;
                }

                // 构建分配追踪列表，每个任务记录硫化余量上限
                List<TaskAllocationR> allocations = new ArrayList<>();
                for (TaskDemandSimple td : taskDemands) {
                    int surplus = productionCalculator.getVulcanizingSurplus(
                            td.materialCode, td.productStatus, monthSurplusMap);
                    allocations.add(new TaskAllocationR(td, 0, surplus));
                }

                // 多轮分配：每轮按日硫化量比例分配给尚有容量的物料，最后一个倒扣
                boolean demandZero = (totalDemand == 0);
                int remaining = totalStock;
                for (int round = 1; remaining > 0; round++) {
                    int roundAllocated = distributeRoundSimple(allocations, remaining, demandZero);
                    if (roundAllocated == 0) {
                        // 所有状态账户均达到硫化余量上限，剩余物理库存保留为未分配库存
                        log.debug("胎胚 {} 所有物料状态账户已达硫化余量上限，剩余库存 {} 不再分配",
                                embryoCode, remaining);
                        break;
                    }
                    remaining -= roundAllocated;
                    log.debug("胎胚 {} 第{}轮分配完成，本轮分配 {}，剩余 {}", embryoCode, round, roundAllocated, remaining);
                }

                // 写回分配结果
                for (TaskAllocationR a : allocations) {
                    materialStockMap.merge(a.taskKey, a.allocated, Integer::sum);
                    log.debug("物料编码 {}，胎胚 {} 共用分配：硫化任务 {} 日硫化量 {}，分配库存 {}（硫化余量上限={}）",
                            a.materialCode, embryoCode, a.taskKey, a.demand, a.allocated, a.surplus);
                }
            }
        }

        return materialStockMap;
    }

    /**
     * 多轮分配：每轮按日硫化量比例分配给尚有容量的物料，最后一个倒扣
     */
    private int distributeRoundSimple(List<TaskAllocationR> allocations, int remainingStock, boolean demandZero) {
        Map<String, Integer> allocatedByStatusKey = allocations.stream()
                .collect(Collectors.groupingBy(allocation -> MonthPlanSurplusCalculator.buildMaterialStatusKey(
                                allocation.materialCode, allocation.productStatus),
                        Collectors.summingInt(allocation -> allocation.allocated)));
        List<TaskAllocationR> withCapacity = allocations.stream()
                .filter(allocation -> {
                    String statusKey = MonthPlanSurplusCalculator.buildMaterialStatusKey(
                            allocation.materialCode, allocation.productStatus);
                    return allocatedByStatusKey.getOrDefault(statusKey, 0) < allocation.surplus;
                })
                .collect(Collectors.toList());

        if (withCapacity.isEmpty()) {
            return 0;
        }

        int totalCapacityDemand;
        if (demandZero) {
            totalCapacityDemand = withCapacity.size();
        } else {
            totalCapacityDemand = withCapacity.stream().mapToInt(a -> a.demand).sum();
            if (totalCapacityDemand == 0) {
                totalCapacityDemand = withCapacity.size();
            }
        }

        int roundAllocated = 0;
        for (int i = 0; i < withCapacity.size(); i++) {
            TaskAllocationR a = withCapacity.get(i);
            int add;
            if (i == withCapacity.size() - 1) {
                add = remainingStock - roundAllocated;
            } else {
                if (demandZero) {
                    add = remainingStock / withCapacity.size();
                } else {
                    add = (int) ((long) remainingStock * a.demand / totalCapacityDemand);
                }
            }
            String statusKey = MonthPlanSurplusCalculator.buildMaterialStatusKey(
                    a.materialCode, a.productStatus);
            int cap = a.surplus - allocatedByStatusKey.getOrDefault(statusKey, 0);
            int actual = Math.min(add, cap);
            a.allocated += actual;
            allocatedByStatusKey.merge(statusKey, actual, Integer::sum);
            roundAllocated += actual;
        }
        return roundAllocated;
    }

    // TaskAllocationR 已移至 com.zlt.aps.cx.vo.TaskAllocationR
    // TaskDemandSimple 已移至 com.zlt.aps.cx.vo.TaskDemandSimple

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
            List<ShiftProductionResult> shiftProductionResults,
            Map<String, Integer> vulcanizingConsumptionByMaterial) {
        Set<String> lastBatchMaterials = new HashSet<>();
        if (shiftProductionResults == null || shiftProductionResults.isEmpty()) {
            return lastBatchMaterials;
        }

        for (ShiftProductionResult spr : shiftProductionResults) {
            if (Boolean.TRUE.equals(spr.getIsLastEndingBatch()) && spr.getMaterialCode() != null) {
                String materialCode = spr.getMaterialCode();
                String materialStatusKey = MonthPlanSurplusCalculator.buildMaterialStatusKey(
                        materialCode, spr.getProductStatus());
                if (vulcanizingConsumptionByMaterial != null
                        && vulcanizingConsumptionByMaterial.containsKey(materialStatusKey)) {
                    lastBatchMaterials.add(materialStatusKey);
                    log.info("【收尾锁定】物料状态账户 {} 本班次为最后一批收尾，成型产出={}，硫化消耗={}",
                            materialStatusKey, spr.getQuantity(),
                            vulcanizingConsumptionByMaterial.get(materialStatusKey));
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
            String materialStatusKey = entry.getKey();
            int consumption = entry.getValue();
            MdmMonthSurplus surplus = monthSurplusMap.get(materialStatusKey);
            if (surplus != null && surplus.getPlanSurplusQty() != null) {
                BigDecimal oldSurplus = surplus.getPlanSurplusQty();

                if (lastBatchMaterials != null && lastBatchMaterials.contains(materialStatusKey)) {
                    surplus.setPlanSurplusQty(BigDecimal.ZERO);
                    log.info("  - {}: 最后一批收尾锁定，原余量={}, 硫化消耗={}, 新余量=0",
                            materialStatusKey, oldSurplus, consumption);
                } else {
                    BigDecimal newSurplus = oldSurplus.subtract(BigDecimal.valueOf(consumption));
                    surplus.setPlanSurplusQty(newSurplus);
                    log.info("  - {}: 原余量={}, 硫化消耗={}, 新余量={}",
                            materialStatusKey, oldSurplus, consumption, newSurplus);
                }
            } else {
                log.warn("  - {}: 未找到硫化余量记录或余量为空，消耗={}", materialStatusKey, consumption);
            }
        }
    }

    // calculateHourlyCapacity 已移至 ProductionCalculator

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

        // 按物料+产品状态汇总库存（从 materialStockMap 按硫化任务汇总）
        Map<String, Integer> stockByMaterial = new HashMap<>();
        if (lhResults != null && materialStockMap != null) {
            for (LhScheduleResult lh : lhResults) {
                if (lh.getMaterialCode() != null && lh.getId() != null) {
                    String taskKey = String.valueOf(lh.getId());
                    int stock = materialStockMap.getOrDefault(taskKey, 0);
                    String materialStatusKey = MonthPlanSurplusCalculator.buildMaterialStatusKey(
                            lh.getMaterialCode(), lh.getProductStatus());
                    stockByMaterial.merge(materialStatusKey, stock, Integer::sum);
                }
            }
        }

        // 重算成型余量
        Map<String, Integer> newFormingRemainderMap = new HashMap<>();
        log.info("【步骤5】重算成型余量（物料 → 硫化余量 - 库存 = 成型余量）:");
        for (Map.Entry<String, MdmMonthSurplus> entry : monthSurplusMap.entrySet()) {
            String materialStatusKey = entry.getKey();
            MdmMonthSurplus surplus = entry.getValue();

            if (lastBatchMaterials != null && lastBatchMaterials.contains(materialStatusKey)) {
                newFormingRemainderMap.put(materialStatusKey, 0);
                log.info("  - {}: 最后一批收尾锁定，成型余量=0", materialStatusKey);
                continue;
            }

            int vulcanizingRemainder = surplus.getPlanSurplusQty() != null
                    ? surplus.getPlanSurplusQty().intValue() : 0;
            int materialStock = stockByMaterial.getOrDefault(materialStatusKey, 0);
            int formingRemainder = Math.max(0, vulcanizingRemainder - materialStock);
            newFormingRemainderMap.put(materialStatusKey, formingRemainder);
            log.info("  - {}: 硫化余量={}, 库存={}, 成型余量={}",
                    materialStatusKey, vulcanizingRemainder, materialStock, formingRemainder);
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

        List<ShiftProductionResult> productionResults = shiftResult.getShiftProductionResults();
        if (productionResults == null) {
            return;
        }

        for (Map.Entry<String, Integer> entry : formingRemainderMap.entrySet()) {
            String materialStatusKey = entry.getKey();
            MdmMonthSurplus monthSurplus = context.getMonthSurplusMap() != null
                    ? context.getMonthSurplusMap().get(materialStatusKey) : null;
            if (monthSurplus == null || monthSurplus.getMaterialCode() == null) {
                continue;
            }
            String materialCode = monthSurplus.getMaterialCode();
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
                        String taskStatusKey = MonthPlanSurplusCalculator.buildMaterialStatusKey(
                                ta.getMaterialCode(), ta.getProductStatus());
                        if (materialStatusKey.equals(taskStatusKey)) {
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
            ShiftProductionResult spr = new ShiftProductionResult();
            spr.setMachineCode(foundMachineCode);
            spr.setEmbryoCode(foundTask.getEmbryoCode());
            spr.setMaterialCode(materialCode);
            spr.setMaterialDesc(foundTask.getMaterialDesc());
            spr.setMainMaterialDesc(foundTask.getMainMaterialDesc());
            spr.setStructureName(foundTask.getStructureName());
            spr.setProductStatus(foundTask.getProductStatus());
            spr.setConstructionStage(foundTask.getConstructionStage());
            spr.setQuantity(0);
            spr.setIsEndingTask(true);
            spr.setIsLastEndingBatch(true);

            // 设置 sourceTask 用于 buildTaskAnalysis 构建
            DailyEmbryoTask sourceTask = new DailyEmbryoTask();
            sourceTask.setEmbryoCode(foundTask.getEmbryoCode());
            sourceTask.setMaterialCode(materialCode);
            sourceTask.setProductStatus(foundTask.getProductStatus());
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
}
