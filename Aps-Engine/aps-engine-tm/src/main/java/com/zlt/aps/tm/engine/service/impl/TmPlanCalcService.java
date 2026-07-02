package com.zlt.aps.tm.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.tm.api.enums.TmScheduleErrorCodeEnum;
import com.zlt.aps.tm.engine.domain.*;
import com.zlt.aps.tm.engine.service.ITmPlanCalcService;
import com.zlt.aps.tm.engine.strategy.ITmDemandQtyStrategy;
import com.zlt.aps.tm.engine.strategy.ITmPlanQtyStrategy;
import com.zlt.aps.tm.engine.strategy.TmStrategyRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 胎面需求量和计划量默认计算步骤服务。
 *
 * <p>通过 {@link TmStrategyRegistry} 获取计划量策略，替代直接 new 策略对象。
 * 计划量策略编码从上下文参数读取（参数码 {@code TM_PLAN_QTY_STRATEGY}，缺省 {@code "DEFAULT"}）。
 * 计划量计算使用当前任务班初 rollingStockQty，同一胎面按班次逐班回写交接班库存。</p>
 */
@Slf4j
@Service
public class TmPlanCalcService implements ITmPlanCalcService {

    /** 计划量策略编码参数码 */
    private static final String PARAM_PLAN_QTY_STRATEGY = "TM_PLAN_QTY_STRATEGY";

    /** 需求量算法开关参数码 */
    private static final String PARAM_ALGORITHM_SWITCH = "TM_ALGORITHM_SWITCH";

    /** 默认计划量策略编码 */
    private static final String DEFAULT_PLAN_QTY_STRATEGY = "DEFAULT";

    /** 默认需求量算法编码 */
    private static final String DEFAULT_ALGORITHM_CODE = "1";

    private final TmStrategyRegistry strategyRegistry;

    /**
     * 创建计划量计算服务。
     *
     * @param strategyRegistry 胎面策略注册表
     */
    public TmPlanCalcService(TmStrategyRegistry strategyRegistry) {
        this.strategyRegistry = strategyRegistry;
    }

    @Override
    public void calculate(TmScheduleContext context) {
        if (context == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_CONTEXT_EMPTY.getDefaultMessage());
        }
        if (CollUtil.isEmpty(context.getTaskDraftList())) {
            return;
        }

        // 获取库存预测结果
        Map<String, TmStockForecast> stockForecastMap = context.getStockForecastMap();

        // 读取计划量策略编码，缺省 DEFAULT
        String planQtyStrategyCode = readParam(context, PARAM_PLAN_QTY_STRATEGY, DEFAULT_PLAN_QTY_STRATEGY);
        ITmPlanQtyStrategy planQtyStrategy = strategyRegistry.getPlanQtyStrategy(planQtyStrategyCode);
        String demandQtyAlgorithmCode = readAlgorithmCode(context);
        ITmDemandQtyStrategy demandQtyStrategy = strategyRegistry.getDemandQtyStrategy(demandQtyAlgorithmCode);

        // 初始化 per-tread 班初滚动库存（初值取14点预计库存），逐班回写交接班库存。
        Map<String, BigDecimal> remainingStockMap = new HashMap<>();
        if (stockForecastMap != null) {
            for (Map.Entry<String, TmStockForecast> entry : stockForecastMap.entrySet()) {
                BigDecimal rollingStock = entry.getValue().getRollingStockQty();
                remainingStockMap.put(entry.getKey(), rollingStock != null ? rollingStock : BigDecimal.ZERO);
            }
        }
        context.setRemainingStockMap(remainingStockMap);

        // 防御性稳定排序：先按班次、再按胎面编码升序，保证全局工装池和同胎面库存都按任务顺序滚动。
        context.getTaskDraftList().sort(Comparator
                .comparing(TmTaskDraft::getShiftOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TmTaskDraft::getTreadCode, Comparator.nullsLast(Comparator.naturalOrder())));
        BigDecimal remainingToolQty = this.initializeGlobalAvailableToolQty(context, stockForecastMap);

        for (TmTaskDraft task : context.getTaskDraftList()) {
            // 6点库存保留预测快照；班初滚动库存必须从上一任务回写的交接班库存读取。
            if (stockForecastMap != null && task.getTreadCode() != null) {
                TmStockForecast forecast = stockForecastMap.get(task.getTreadCode());
                if (forecast != null) {
                    task.setSixClockStockQty(forecast.getSixClockStockQty());
                }
            }
            if (task.getTreadCode() != null) {
                BigDecimal rollingStock = remainingStockMap.get(task.getTreadCode());
                if (rollingStock == null) {
                    rollingStock = nvl(task.getRollingStockQty());
                    remainingStockMap.put(task.getTreadCode(), rollingStock);
                }
                task.setRollingStockQty(rollingStock);
            }
            // 旧骨架数据只提供 demandQty 时，将其作为当前班基础需求，避免默认策略按空值计算为 0。
            if (task.getCurrentShiftDemandQty() == null && task.getDemandQty() != null) {
                task.setCurrentShiftDemandQty(task.getDemandQty());
            }

            // 计划量策略只读取当前任务班初全局可用工装，工装池滚动状态由本服务统一维护。
            task.setAvailableToolQty(remainingToolQty);
            BigDecimal beforeRollingStockQty = task.getRollingStockQty();
            BigDecimal beforeAvailableToolQty = remainingToolQty;

            // 通过需求量策略计算库存保证缺口、基础需求量和供应时长，供排序和计划量策略复用。
            TmDemandQtyResult demandQtyResult = demandQtyStrategy.calculate(buildDemandQtyInput(task), context);
            applyDemandQtyResult(task, demandQtyResult);
            addNewSpecTrace(context, task);
            addExperimentSpecTrace(context, task);
            addDemandTrace(context, task, demandQtyAlgorithmCode);
            // 打印需求量计算公式和关键中间量，便于按批次和业务键还原计划量入口。
            log.info("[TM_DEMAND_QTY_CALC] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, businessKey={}, treadCode={}, shiftOrder={}, algorithmCode={}, formula=currentShiftDemandQty-rollingStockQty=>currentShiftStockGapQty,guardDemandQty-rollingStockQty=>stockGapQty,max(currentShiftStockGapQty,stockGapQty)=>demandQty",
                    context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), formatScheduleDate(context),
                    task.getBusinessKey(), task.getTreadCode(), task.getShiftOrder(), demandQtyAlgorithmCode);
            log.info("[TM_DEMAND_QTY_CALC_DETAIL] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, businessKey={}, treadCode={}, shiftOrder={}, guardDemandQty={}, rollingStockQty={}, currentShiftStockGapQty={}, stockGapQty={}, currentShiftDemandQty={}, demandQty={}",
                    context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), formatScheduleDate(context),
                    task.getBusinessKey(), task.getTreadCode(), task.getShiftOrder(),
                    task.getGuardDemandQty(), task.getRollingStockQty(), task.getCurrentShiftStockGapQty(), task.getStockGapQty(),
                    task.getCurrentShiftDemandQty(), task.getDemandQty());
            // 打印供应时长计算公式和关键中间量，便于解释排序中的库存紧急度。
            log.info("[TM_DEMAND_QTY_SUPPLY] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, businessKey={}, treadCode={}, shiftOrder={}, formula=supplyHours=rollingStockQty/(guardDemandQty/guardRangeHours)",
                    context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), formatScheduleDate(context),
                    task.getBusinessKey(), task.getTreadCode(), task.getShiftOrder());
            log.info("[TM_DEMAND_QTY_SUPPLY_DETAIL] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, businessKey={}, treadCode={}, shiftOrder={}, supplyHours={}, rollingStockQty={}, guardDemandQty={}, guardRangeHours={}",
                    context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), formatScheduleDate(context),
                    task.getBusinessKey(), task.getTreadCode(), task.getShiftOrder(),
                    task.getSupplyHours(), task.getRollingStockQty(),
                    task.getGuardDemandQty(), task.getGuardRangeHours());

            // 已有计划量表示上游已完成特殊业务调整，此处保持不变。
            if (task.getPlanQty() == null) {
                TmPlanQtyResult planQtyResult = planQtyStrategy.calculate(task, context);
                applyPlanQtyResult(task, planQtyResult);
            }
            remainingToolQty = this.updateGlobalToolState(task, remainingToolQty);
            updateRollingStockState(context, task);
            addPlanQtyTrace(context, task, planQtyStrategyCode);
            // 打印计划量计算公式、分量和滚动状态，减少人工二次推导。
            if (task.getPlanQty() != null) {
                log.info("[TM_PLAN_QTY_CALC] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, businessKey={}, treadCode={}, shiftOrder={}, strategyCode={}, calcFormulaDesc={}",
                        context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), formatScheduleDate(context),
                        task.getBusinessKey(), task.getTreadCode(), task.getShiftOrder(), planQtyStrategyCode,
                        task.getCalcFormulaDesc());
                log.info("[TM_PLAN_QTY_CALC_DETAIL] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, businessKey={}, treadCode={}, shiftOrder={}, demandQty={}, stockDeductQty={}, baseDemandQty={}, lossAddQty={}, toolLimitAdjustQty={}, toolOverflowQty={}, minStartAdjustQty={}, tailRoundAdjustQty={}, capacityAdjustQty={}, availableToolQty={}, toolUsedQty={}, remainingToolQty={}, planStockQty={}, planQty={}, calcFormulaDesc={}",
                        context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), formatScheduleDate(context),
                        task.getBusinessKey(), task.getTreadCode(), task.getShiftOrder(),
                        task.getDemandQty(), task.getStockDeductQty(), task.getBaseDemandQty(),
                        task.getLossAddQty(), task.getToolLimitAdjustQty(), task.getToolOverflowQty(),
                        task.getMinStartAdjustQty(), task.getTailRoundAdjustQty(),
                        task.getCapacityAdjustQty(), task.getAvailableToolQty(),
                        task.getToolUsedQty(), task.getRemainingToolQty(), task.getPlanStockQty(), task.getPlanQty(),
                        task.getCalcFormulaDesc());
                log.info("[TM_PLAN_QTY_STATE] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, businessKey={}, treadCode={}, shiftOrder={}, beforeRollingStockQty={}, afterRollingStockQty={}, beforeAvailableToolQty={}, afterRemainingToolQty={}, planStockQty={}, planQty={}",
                        context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), formatScheduleDate(context),
                        task.getBusinessKey(), task.getTreadCode(), task.getShiftOrder(), beforeRollingStockQty,
                        context.getRemainingStockMap().get(task.getTreadCode()), beforeAvailableToolQty,
                        task.getRemainingToolQty(), task.getPlanStockQty(), task.getPlanQty());
            }
        }
    }

    /**
     * 格式化排程日期，避免日志中直接打印Date对象造成排查口径不统一。
     *
     * @param context 排程上下文
     * @return yyyy-MM-dd格式日期；日期为空时返回null
     */
    private String formatScheduleDate(TmScheduleContext context) {
        return context == null || context.getScheduleDate() == null ? null : DateUtil.formatDate(context.getScheduleDate());
    }

    /**
     * 写入新规格判断和提前排产窗口证据。
     *
     * @param context 排程上下文
     * @param task    任务草稿
     */
    private void addNewSpecTrace(TmScheduleContext context, TmTaskDraft task) {
        TmNewSpecInfo info = task.getNewSpecInfo();
        if (info == null) {
            return;
        }
        Map<String, Object> detectEvidence = new LinkedHashMap<>();
        detectEvidence.put("newSpec", info.getNewSpec());
        detectEvidence.put("lookbackDays", info.getLookbackDays());
        detectEvidence.put("lookbackDaysSource", info.getLookbackDaysSource());
        detectEvidence.put("previousStockDate", info.getPreviousStockDate());
        detectEvidence.put("previousDayStockQty", info.getPreviousDayStockQty());
        detectEvidence.put("previousDayStockExists", info.getPreviousDayStockExists());
        detectEvidence.put("historyStartDate", info.getHistoryStartDate());
        detectEvidence.put("historyEndDate", info.getHistoryEndDate());
        detectEvidence.put("historySchedulePlanExists", info.getHistorySchedulePlanExists());
        detectEvidence.put("reason", info.getReason());
        traceOf(context, task).addRuleHit("NEW_SPEC_DETECT", Boolean.TRUE.equals(info.getNewSpec()) ? "PASS" : "SKIP", detectEvidence);
        if (!info.isNewSpecHit()) {
            return;
        }
        Map<String, Object> windowEvidence = new LinkedHashMap<>();
        windowEvidence.put("advanceShiftCount", info.getAdvanceShiftCount());
        windowEvidence.put("advanceShiftCountSource", info.getAdvanceShiftCountSource());
        windowEvidence.put("normalTargetShift", info.getNormalTargetShift());
        windowEvidence.put("adjustedTargetShift", info.getAdjustedTargetShift());
        windowEvidence.put("adjustedTargetWindow", info.getAdjustedTargetWindow());
        windowEvidence.put("demandShift", info.getDemandShift());
        windowEvidence.put("demandQty", info.getDemandQty());
        traceOf(context, task).addRuleHit("NEW_SPEC_ADVANCE_WINDOW", "PASS", windowEvidence);
    }

    /**
     * 写入实验规格判断和固定计划量证据。
     *
     * @param context 排程上下文
     * @param task    任务草稿
     */
    private void addExperimentSpecTrace(TmScheduleContext context, TmTaskDraft task) {
        TmExperimentSpecInfo info = task.getExperimentSpecInfo();
        if (info == null) {
            return;
        }
        Map<String, Object> detectEvidence = new LinkedHashMap<>();
        detectEvidence.put("experimentSpec", info.getExperimentSpec());
        detectEvidence.put("lookbackDays", info.getLookbackDays());
        detectEvidence.put("lookbackDaysSource", info.getLookbackDaysSource());
        detectEvidence.put("scheduleDate", info.getScheduleDate());
        detectEvidence.put("experimentPlanDate", info.getExperimentPlanDate());
        detectEvidence.put("monthPlanDayQty", info.getMonthPlanDayQty());
        detectEvidence.put("monthPlanIds", info.getMonthPlanIds());
        detectEvidence.put("productionNos", info.getProductionNos());
        detectEvidence.put("embryoCodes", info.getEmbryoCodes());
        detectEvidence.put("mergedToExistingTask", info.getMergedToExistingTask());
        detectEvidence.put("reason", info.getReason());
        traceOf(context, task).addRuleHit("EXPERIMENT_SPEC_DETECT",
                info.isExperimentSpecHit() ? "PASS" : "SKIP", detectEvidence);
        if (!info.isExperimentSpecHit()) {
            return;
        }
        Map<String, Object> planQtyEvidence = new LinkedHashMap<>();
        planQtyEvidence.put("planQty", info.getPlanQty());
        planQtyEvidence.put("planQtySource", info.getPlanQtySource());
        planQtyEvidence.put("finalTaskPlanQty", task.getPlanQty());
        planQtyEvidence.put("currentShiftDemandQty", task.getCurrentShiftDemandQty());
        planQtyEvidence.put("guardDemandQty", task.getGuardDemandQty());
        traceOf(context, task).addRuleHit("EXPERIMENT_SPEC_PLAN_QTY", "PASS", planQtyEvidence);
    }
    /**
     * 写入需求量计算规则证据。
     *
     * @param context              排程上下文
     * @param task                 任务草稿
     * @param demandAlgorithmCode  需求量算法编码
     */
    private void addDemandTrace(TmScheduleContext context, TmTaskDraft task, String demandAlgorithmCode) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("algorithmCode", demandAlgorithmCode);
        evidence.put("currentShiftDemandQty", task.getCurrentShiftDemandQty());
        evidence.put("guardDemandQty", task.getGuardDemandQty());
        evidence.put("rollingStockQty", task.getRollingStockQty());
        evidence.put("currentShiftStockGapQty", task.getCurrentShiftStockGapQty());
        evidence.put("stockGapQty", task.getStockGapQty());
        evidence.put("demandQty", task.getDemandQty());
        evidence.put("sourceOrderNos", task.getSourceOrderNos());
        traceOf(context, task).addRuleHit("DEMAND_QTY_CALC", "PASS", evidence);
    }

    /**
     * 写入计划量计算规则证据。
     *
     * @param context             排程上下文
     * @param task                任务草稿
     * @param planQtyStrategyCode 计划量策略编码
     */
    private void addPlanQtyTrace(TmScheduleContext context, TmTaskDraft task, String planQtyStrategyCode) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("strategyCode", planQtyStrategyCode);
        evidence.put("planQty", task.getPlanQty());
        evidence.put("demandQty", task.getDemandQty());
        evidence.put("stockDeductQty", task.getStockDeductQty());
        evidence.put("planStockQty", task.getPlanStockQty());
        evidence.put("tailFlag", task.getTailFlag());
        evidence.put("toolOverflowQty", task.getToolOverflowQty());
        evidence.put("totalToolQty", task.getTotalToolQty());
        evidence.put("availableToolQty", task.getAvailableToolQty());
        evidence.put("toolUsedQty", task.getToolUsedQty());
        evidence.put("remainingToolQty", task.getRemainingToolQty());
        evidence.put("curlRollLength", task.getCurlRollLength());
        evidence.put("lossRate", task.getLossRate());
        evidence.put("calcFormulaDesc", task.getCalcFormulaDesc());
        traceOf(context, task).addRuleHit("PLAN_QTY_CALC", "PASS", evidence);
    }

    /**
     * 获取任务规则证据对象，不存在时创建。
     *
     * @param context 排程上下文
     * @param task    任务草稿
     * @return 规则证据对象
     */
    private TmRuleTrace traceOf(TmScheduleContext context, TmTaskDraft task) {
        return context.getRuleTraceMap().computeIfAbsent(task.getBusinessKey(), key -> new TmRuleTrace());
    }

    /**
     * 根据任务草稿构建需求量策略输入。
     *
     * @param task    任务草稿
     * @return 需求量策略输入
     */
    private TmDemandQtyInput buildDemandQtyInput(TmTaskDraft task) {
        TmDemandQtyInput input = new TmDemandQtyInput();
        input.setTreadCode(task.getTreadCode());
        input.setCurrentShiftDemandQty(task.getCurrentShiftDemandQty());
        input.setGuardDemandQty(task.getGuardDemandQty());
        input.setRollingStockQty(task.getRollingStockQty());
        input.setGuardShiftCount(task.getGuardShiftCount());
        input.setGuardRangeHours(task.getGuardRangeHours());
        return input;
    }

    /**
     * 将需求量策略结果回填到任务草稿。
     *
     * @param task   任务草稿
     * @param result 需求量策略结果
     */
    private void applyDemandQtyResult(TmTaskDraft task, TmDemandQtyResult result) {
        if (result == null) {
            return;
        }
        task.setCurrentShiftDemandQty(result.getCurrentShiftDemandQty());
        task.setGuardDemandQty(result.getGuardDemandQty());
        task.setRollingStockQty(result.getRollingStockQty());
        task.setCurrentShiftStockGapQty(result.getCurrentShiftStockGapQty());
        task.setStockGapQty(result.getStockGapQty());
        task.setDemandQty(result.getDemandQty());
        task.setGuardShiftCount(result.getGuardShiftCount());
        task.setSupplyHours(result.getSupplyHours());
    }

    /**
     * 初始化全局可用工装数量。
     *
     * <p>首个任务的可用工装数量等于总工装数量减去所有胎面14点预计库存折算的占用工装数量。工装数量是全局池，
     * 因此不能按单个胎面重复使用总工装数量。</p>
     *
     * @param context          排程上下文
     * @param stockForecastMap 胎面库存预测结果
     * @return 首个任务计算前的全局可用工装数量；未配置总工装时返回 null 表示不启用工装限制
     */
    private BigDecimal initializeGlobalAvailableToolQty(TmScheduleContext context, Map<String, TmStockForecast> stockForecastMap) {
        BigDecimal totalToolQty = this.resolveGlobalTotalToolQty(context);
        if (totalToolQty == null) {
            return null;
        }
        Map<String, TmTaskDraft> representativeTaskMap = new LinkedHashMap<>();
        for (TmTaskDraft task : context.getTaskDraftList()) {
            if (task.getTreadCode() != null && !representativeTaskMap.containsKey(task.getTreadCode())) {
                representativeTaskMap.put(task.getTreadCode(), task);
            }
        }
        BigDecimal initialUsedToolQty = BigDecimal.ZERO;
        for (Map.Entry<String, TmTaskDraft> entry : representativeTaskMap.entrySet()) {
            BigDecimal curlLength = this.resolveCurlLength(entry.getValue());
            if (curlLength.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal forecastStockQty = this.resolveForecastRollingStock(entry.getKey(), entry.getValue(), stockForecastMap);
            initialUsedToolQty = initialUsedToolQty.add(forecastStockQty.divide(curlLength, 6, RoundingMode.HALF_UP));
        }
        return totalToolQty.subtract(initialUsedToolQty).max(BigDecimal.ZERO).setScale(6, RoundingMode.HALF_UP);
    }

    /**
     * 解析全局总工装数量，并校验同一轮排程携带的总工装数量一致。
     *
     * @param context 排程上下文
     * @return 全局总工装数量；未配置时返回 null
     */
    private BigDecimal resolveGlobalTotalToolQty(TmScheduleContext context) {
        BigDecimal totalToolQty = null;
        for (TmTaskDraft task : context.getTaskDraftList()) {
            if (task.getTotalToolQty() == null) {
                continue;
            }
            if (totalToolQty == null) {
                totalToolQty = task.getTotalToolQty();
                continue;
            }
            if (totalToolQty.compareTo(task.getTotalToolQty()) != 0) {
                throw new ServiceException("胎面自动排程总工装数量不一致，无法计算全局工装池");
            }
        }
        return totalToolQty;
    }

    /**
     * 解析胎面14点预计库存。
     *
     * @param treadCode        胎面编码
     * @param task             任务草稿
     * @param stockForecastMap 胎面库存预测结果
     * @return 14点预计库存，空值按0处理
     */
    private BigDecimal resolveForecastRollingStock(String treadCode, TmTaskDraft task, Map<String, TmStockForecast> stockForecastMap) {
        if (stockForecastMap != null) {
            TmStockForecast forecast = stockForecastMap.get(treadCode);
            if (forecast != null && forecast.getRollingStockQty() != null) {
                return forecast.getRollingStockQty();
            }
        }
        return nvl(task.getRollingStockQty());
    }

    /**
     * 按当前任务计划量和当前班成型需求量滚动全局工装池，生产增加占用，成型消耗库存释放占用。
     *
     * @param task                    任务草稿
     * @param currentAvailableToolQty 当前任务计算前全局可用工装数量
     * @return 当前任务计算后的全局剩余工装数量
     */
    private BigDecimal updateGlobalToolState(TmTaskDraft task, BigDecimal currentAvailableToolQty) {
        if (currentAvailableToolQty == null) {
            return null;
        }
        BigDecimal curlLength = this.resolveCurlLength(task);
        if (curlLength.compareTo(BigDecimal.ZERO) <= 0) {
            task.setToolUsedQty(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
            task.setRemainingToolQty(currentAvailableToolQty);
            return currentAvailableToolQty;
        }
        BigDecimal netUsedToolQty = nvl(task.getPlanQty()).subtract(nvl(task.getCurrentShiftDemandQty()))
                .divide(curlLength, 6, RoundingMode.HALF_UP);
        BigDecimal remainingToolQty = currentAvailableToolQty.subtract(netUsedToolQty).max(BigDecimal.ZERO);
        if (task.getTotalToolQty() != null) {
            remainingToolQty = remainingToolQty.min(task.getTotalToolQty());
        }
        remainingToolQty = remainingToolQty.setScale(6, RoundingMode.HALF_UP);
        task.setToolUsedQty(netUsedToolQty);
        task.setRemainingToolQty(remainingToolQty);
        return remainingToolQty;
    }

    /**
     * 解析卷曲长度。
     *
     * @param task 任务草稿
     * @return 卷曲长度，无法取得时返回0
     */
    private BigDecimal resolveCurlLength(TmTaskDraft task) {
        if (task.getCurlRollLength() != null && task.getCurlRollLength().compareTo(BigDecimal.ZERO) > 0) {
            return task.getCurlRollLength();
        }
        return nvl(task.getDefaultCurlRollLength());
    }
    /**
     * 将计划量策略结果回填到任务草稿，便于解释表落库。
     *
     * @param task   任务草稿
     * @param result 计划量策略结果
     */
    private void applyPlanQtyResult(TmTaskDraft task, TmPlanQtyResult result) {
        if (result == null) {
            return;
        }
        task.setBaseDemandQty(result.getBaseDemandQty());
        task.setLossAddQty(result.getLossAddQty());
        task.setToolLimitAdjustQty(result.getToolLimitAdjustQty());
        task.setToolOverflowQty(result.getToolOverflowQty());
        task.setMinStartAdjustQty(result.getMinStartAdjustQty());
        task.setTailRoundAdjustQty(result.getTailRoundAdjustQty());
        task.setCapacityAdjustQty(result.getCapacityAdjustQty());
        task.setPlanQty(result.getFinalPlanQty());
        task.setCalcFormulaDesc(result.getCalcFormulaDesc());
    }


    /**
     * 回写同一胎面的下一任务班初库存状态。
     *
     * @param context 胎面排程上下文
     * @param task    任务草稿
     */
    private void updateRollingStockState(TmScheduleContext context, TmTaskDraft task) {
        if (context == null || context.getRemainingStockMap() == null || task == null || task.getTreadCode() == null) {
            return;
        }
        BigDecimal handoverStock = task.getPlanStockQty();
        if (handoverStock == null && task.getPlanQty() != null) {
            handoverStock = nvl(task.getRollingStockQty()).add(nvl(task.getPlanQty()))
                    .subtract(nvl(task.getCurrentShiftDemandQty())).max(BigDecimal.ZERO);
            task.setPlanStockQty(handoverStock);
        }
        context.getRemainingStockMap().put(task.getTreadCode(), nvl(handoverStock));
    }
    /**
     * 空值转 0。
     *
     * @param value 原始数值
     * @return 非空数值
     */
    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 从上下文读取参数值，缺省时返回默认值。
     *
     * @param context      胎面排程上下文
     * @param paramCode    参数编码
     * @param defaultValue 缺省值
     * @return 参数有效值
     */
    private String readParam(TmScheduleContext context, String paramCode, String defaultValue) {
        TmParamValue paramValue = context.getParamMap().get(paramCode);
        if (paramValue == null || StrUtil.isBlank(paramValue.getEffectiveValue())) {
            return defaultValue;
        }
        return paramValue.getEffectiveValue();
    }

    /**
     * 读取需求量算法参数 TM_ALGORITHM_SWITCH。
     *
     * @param context 胎面排程上下文
     * @return 需求量算法编码
     */
    public String readAlgorithmCode(TmScheduleContext context) {
        return readParam(context, PARAM_ALGORITHM_SWITCH, DEFAULT_ALGORITHM_CODE);
    }
}
