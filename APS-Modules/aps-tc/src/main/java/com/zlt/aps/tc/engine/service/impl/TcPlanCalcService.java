package com.zlt.aps.tc.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.enums.TcScheduleErrorCodeEnum;
import com.zlt.aps.tc.api.enums.TcScheduleRuleCodeEnum;
import com.zlt.aps.tc.api.enums.TcScheduleRuleResultEnum;
import com.zlt.aps.tc.api.enums.TcScheduleStrategyEnum;
import com.zlt.aps.tc.engine.domain.*;
import com.zlt.aps.tc.engine.service.ITcPlanCalcService;
import com.zlt.aps.tc.engine.strategy.ITcDemandQtyStrategy;
import com.zlt.aps.tc.engine.strategy.ITcPlanQtyStrategy;
import com.zlt.aps.tc.engine.strategy.TcStrategyRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 胎侧需求量和计划量默认计算步骤服务。
 *
 * <p>通过 {@link TcStrategyRegistry} 获取计划量策略，替代直接 new 策略对象。
 * 计划量策略编码从上下文参数读取，参数键和默认策略分别由
 * {@link TcScheduleConstants#PARAM_PLAN_QTY_STRATEGY}、{@link TcScheduleStrategyEnum#DEFAULT} 统一定义。
 * 计划量计算使用当前任务班初 rollingStockQty，同一胎侧按班次逐班回写交接班库存。</p>
 */
@Slf4j
@Service
public class TcPlanCalcService implements ITcPlanCalcService {

    private final TcStrategyRegistry strategyRegistry;

    /**
     * 创建计划量计算服务。
     *
     * @param strategyRegistry 胎侧策略注册表
     */
    public TcPlanCalcService(TcStrategyRegistry strategyRegistry) {
        this.strategyRegistry = strategyRegistry;
    }

    @Override
    public void calculate(TcScheduleContext context) {
        if (context == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_CONTEXT_EMPTY.getDefaultMessage());
        }
        if (CollUtil.isEmpty(context.getTaskDraftList())) {
            return;
        }

        // 获取库存预测结果
        Map<String, TcStockForecast> stockForecastMap = context.getStockForecastMap();

        // 读取计划量策略编码，缺省 DEFAULT
        String planQtyStrategyCode = readParam(context, TcScheduleConstants.PARAM_PLAN_QTY_STRATEGY,
                TcScheduleStrategyEnum.DEFAULT.getCode());
        ITcPlanQtyStrategy planQtyStrategy = strategyRegistry.getPlanQtyStrategy(planQtyStrategyCode);
        String demandQtyAlgorithmCode = readAlgorithmCode(context);
        ITcDemandQtyStrategy demandQtyStrategy = strategyRegistry.getDemandQtyStrategy(demandQtyAlgorithmCode);

        // 初始化 per-sidewall 班初滚动库存（初值取14点预计库存），逐班回写交接班库存。
        Map<String, BigDecimal> remainingStockMap = new HashMap<>();
        if (stockForecastMap != null) {
            for (Map.Entry<String, TcStockForecast> entry : stockForecastMap.entrySet()) {
                BigDecimal rollingStock = entry.getValue().getRollingStockQty();
                remainingStockMap.put(entry.getKey(), rollingStock != null ? rollingStock : BigDecimal.ZERO);
            }
        }
        context.setRemainingStockMap(remainingStockMap);

        // 防御性稳定排序：先按班次、再按胎侧编码升序，保证全局工装池和同胎侧库存都按任务顺序滚动。
        context.getTaskDraftList().sort(Comparator
                .comparing(TcTaskDraft::getShiftOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TcTaskDraft::getSidewallCode, Comparator.nullsLast(Comparator.naturalOrder())));
        BigDecimal remainingToolQty = this.initializeGlobalAvailableToolQty(context, stockForecastMap);
        context.setInitialAvailableToolQty(remainingToolQty);
        context.setCurrentAvailableToolQty(remainingToolQty);

        for (TcTaskDraft task : context.getTaskDraftList()) {
            // 6点库存保留预测快照；班初滚动库存必须从上一任务回写的交接班库存读取。
            if (stockForecastMap != null && task.getSidewallCode() != null) {
                TcStockForecast forecast = stockForecastMap.get(task.getSidewallCode());
                if (forecast != null) {
                    task.setSixClockStockQty(forecast.getSixClockStockQty());
                }
            }
            if (task.getSidewallCode() != null) {
                BigDecimal rollingStock = remainingStockMap.get(task.getSidewallCode());
                if (rollingStock == null) {
                    rollingStock = nvl(task.getRollingStockQty());
                    remainingStockMap.put(task.getSidewallCode(), rollingStock);
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
            TcDemandQtyResult demandQtyResult = demandQtyStrategy.calculate(buildDemandQtyInput(task), context);
            applyDemandQtyResult(task, demandQtyResult);
            addNewSpecTrace(context, task);
            addExperimentSpecTrace(context, task);
            addDemandTrace(context, task, demandQtyAlgorithmCode);
            // 打印需求量计算公式和关键中间量，便于按批次和业务键还原计划量入口。
            log.info("[TC_DEMAND_QTY_CALC] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, businessKey={}, sidewallCode={}, shiftOrder={}, algorithmCode={}, formula=currentShiftDemandQty-rollingStockQty=>currentShiftStockGapQty,guardDemandQty-rollingStockQty=>stockGapQty,max(currentShiftStockGapQty,stockGapQty)=>demandQty",
                    context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), formatScheduleDate(context),
                    task.getBusinessKey(), task.getSidewallCode(), task.getShiftOrder(), demandQtyAlgorithmCode);
            log.info("[TC_DEMAND_QTY_CALC_DETAIL] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, businessKey={}, sidewallCode={}, shiftOrder={}, guardDemandQty={}, rollingStockQty={}, currentShiftStockGapQty={}, stockGapQty={}, currentShiftDemandQty={}, demandQty={}",
                    context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), formatScheduleDate(context),
                    task.getBusinessKey(), task.getSidewallCode(), task.getShiftOrder(),
                    task.getGuardDemandQty(), task.getRollingStockQty(), task.getCurrentShiftStockGapQty(), task.getStockGapQty(),
                    task.getCurrentShiftDemandQty(), task.getDemandQty());
            // 打印供应时长计算公式和关键中间量，便于解释排序中的库存紧急度。
            log.info("[TC_DEMAND_QTY_SUPPLY] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, businessKey={}, sidewallCode={}, shiftOrder={}, formula=supplyHours=rollingStockQty/(guardDemandQty/guardRangeHours)",
                    context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), formatScheduleDate(context),
                    task.getBusinessKey(), task.getSidewallCode(), task.getShiftOrder());
            log.info("[TC_DEMAND_QTY_SUPPLY_DETAIL] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, businessKey={}, sidewallCode={}, shiftOrder={}, supplyHours={}, rollingStockQty={}, guardDemandQty={}, guardRangeHours={}",
                    context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), formatScheduleDate(context),
                    task.getBusinessKey(), task.getSidewallCode(), task.getShiftOrder(),
                    task.getSupplyHours(), task.getRollingStockQty(),
                    task.getGuardDemandQty(), task.getGuardRangeHours());

            // 已有计划量表示上游已完成特殊业务调整，此处保持不变。
            if (task.getPlanQty() == null) {
                TcPlanQtyResult planQtyResult = planQtyStrategy.calculate(task, context);
                applyPlanQtyResult(task, planQtyResult);
            }
            this.applyStartupThreshold(context, task);
            this.calculateLatestStartPriority(context, task);
            task.setToolUsedQty(BigDecimal.ZERO.setScale(TcScheduleConstants.DECIMAL_CALCULATION_SCALE,
                    RoundingMode.HALF_UP));
            task.setRemainingToolQty(remainingToolQty);
            context.setCurrentAvailableToolQty(remainingToolQty);
            updateRollingStockState(context, task);
            addPlanQtyTrace(context, task, planQtyStrategyCode);
            // 打印计划量计算公式、分量和滚动状态，减少人工二次推导。
            if (task.getPlanQty() != null) {
                log.info("[TC_PLAN_QTY_CALC] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, businessKey={}, sidewallCode={}, shiftOrder={}, strategyCode={}, calcFormulaDesc={}",
                        context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), formatScheduleDate(context),
                        task.getBusinessKey(), task.getSidewallCode(), task.getShiftOrder(), planQtyStrategyCode,
                        task.getCalcFormulaDesc());
                log.info("[TC_PLAN_QTY_CALC_DETAIL] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, businessKey={}, sidewallCode={}, shiftOrder={}, demandQty={}, stockDeductQty={}, baseDemandQty={}, lossAddQty={}, toolLimitAdjustQty={}, toolOverflowQty={}, minStartAdjustQty={}, tailRoundAdjustQty={}, capacityAdjustQty={}, availableToolQty={}, toolUsedQty={}, remainingToolQty={}, planStockQty={}, planQty={}, calcFormulaDesc={}",
                        context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), formatScheduleDate(context),
                        task.getBusinessKey(), task.getSidewallCode(), task.getShiftOrder(),
                        task.getDemandQty(), task.getStockDeductQty(), task.getBaseDemandQty(),
                        task.getLossAddQty(), task.getToolLimitAdjustQty(), task.getToolOverflowQty(),
                        task.getMinStartAdjustQty(), task.getTailRoundAdjustQty(),
                        task.getCapacityAdjustQty(), task.getAvailableToolQty(),
                        task.getToolUsedQty(), task.getRemainingToolQty(), task.getPlanStockQty(), task.getPlanQty(),
                        task.getCalcFormulaDesc());
                log.info("[TC_PLAN_QTY_STATE] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, businessKey={}, sidewallCode={}, shiftOrder={}, beforeRollingStockQty={}, afterRollingStockQty={}, beforeAvailableToolQty={}, afterRemainingToolQty={}, planStockQty={}, planQty={}",
                        context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), formatScheduleDate(context),
                        task.getBusinessKey(), task.getSidewallCode(), task.getShiftOrder(), beforeRollingStockQty,
                        context.getRemainingStockMap().get(task.getSidewallCode()), beforeAvailableToolQty,
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
    private String formatScheduleDate(TcScheduleContext context) {
        return context == null || context.getScheduleDate() == null ? null : DateUtil.formatDate(context.getScheduleDate());
    }

    /**
     * 对整日停产后的首个开班应用计划量阈值上限。
     *
     * @param context 排程上下文
     * @param task    当前任务
     */
    private void applyStartupThreshold(TcScheduleContext context, TcTaskDraft task) {
        if (context.getStartupShiftOrderSet() == null
                || !context.getStartupShiftOrderSet().contains(task.getShiftOrder())) {
            return;
        }
        BigDecimal threshold = this.readDecimalParam(context,
                TcScheduleConstants.PARAM_OPEN_SHIFT_THRESHOLD, BigDecimal.ONE);
        BigDecimal originalPlanQty = nvl(task.getPlanQty());
        BigDecimal planQtyLimit = nvl(task.getCurrentShiftDemandQty()).multiply(threshold)
                .subtract(nvl(task.getRollingStockQty())).max(BigDecimal.ZERO);
        BigDecimal finalPlanQty = originalPlanQty.min(planQtyLimit);
        task.setPlanQty(finalPlanQty);
        task.setPlanStockQty(nvl(task.getRollingStockQty()).add(finalPlanQty)
                .subtract(nvl(task.getCurrentShiftDemandQty())).max(BigDecimal.ZERO));
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("ruleCode", TcScheduleRuleCodeEnum.STARTUP_THRESHOLD_ADJUST.getCode());
        evidence.put("date", this.formatScheduleDate(context));
        evidence.put("sourceShiftOrder", task.getShiftOrder());
        evidence.put("targetShiftOrder", task.getShiftOrder());
        evidence.put("shiftOrder", task.getShiftOrder());
        evidence.put("currentShiftDemandQty", task.getCurrentShiftDemandQty());
        evidence.put("rollingStockQty", task.getRollingStockQty());
        evidence.put("threshold", threshold);
        evidence.put("originalPlanQty", originalPlanQty);
        evidence.put("planQtyLimit", planQtyLimit);
        evidence.put("adjustedQty", finalPlanQty.subtract(originalPlanQty));
        evidence.put("finalPlanQty", finalPlanQty);
        traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.STARTUP_THRESHOLD_ADJUST,
                finalPlanQty.compareTo(originalPlanQty) < 0
                        ? TcScheduleRuleResultEnum.PASS : TcScheduleRuleResultEnum.SKIP,
                evidence);
    }
    /**
     * 计算库存不足时间、预计生产时长和最晚开始时间，并写入排序规则证据。
     *
     * <p>统一默认速度未配置或非正数、班次开始时间无法解析时不阻断排程，
     * 仅记录跳过原因并保持既有排序结果。</p>
     *
     * @param context 排程上下文
     * @param task    当前任务
     */
    private void calculateLatestStartPriority(TcScheduleContext context, TcTaskDraft task) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        BigDecimal defaultSpeed = this.readDecimalParam(context,
                TcScheduleConstants.PARAM_DEFAULT_PRODUCTION_SPEED, BigDecimal.ZERO);
        BigDecimal standingHours = this.readDecimalParam(context,
                TcScheduleConstants.PARAM_PROCESS_STANDING_HOURS, BigDecimal.ZERO);
        evidence.put("defaultProductionSpeed", defaultSpeed);
        evidence.put("processStandingHours", standingHours);
        evidence.put("planQty", task.getPlanQty());
        evidence.put("supplyHours", task.getSupplyHours());
        Date shiftStartTime = this.resolveShiftStartTime(context, task.getShiftOrder());
        evidence.put("shiftStartTime", shiftStartTime);
        if (defaultSpeed.compareTo(BigDecimal.ZERO) <= 0) {
            evidence.put("reason", TcScheduleConstants.SKIP_REASON_DEFAULT_PRODUCTION_SPEED_NON_POSITIVE);
            traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.LATEST_START_PRIORITY,
                    TcScheduleRuleResultEnum.SKIP, evidence);
            return;
        }
        if (shiftStartTime == null) {
            evidence.put("reason", TcScheduleConstants.SKIP_REASON_SHIFT_START_TIME_INVALID);
            traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.LATEST_START_PRIORITY,
                    TcScheduleRuleResultEnum.SKIP, evidence);
            return;
        }
        BigDecimal supplyHours = nvl(task.getSupplyHours());
        BigDecimal estimatedProductionHours = nvl(task.getPlanQty())
                .divide(defaultSpeed, TcScheduleConstants.DECIMAL_CALCULATION_SCALE, RoundingMode.HALF_UP);
        Date stockShortageTime = this.offsetHours(shiftStartTime, supplyHours);
        Date latestStartTime = this.offsetHours(stockShortageTime,
                standingHours.add(estimatedProductionHours).negate());
        task.setStockShortageTime(stockShortageTime);
        task.setEstimatedProductionHours(estimatedProductionHours);
        task.setLatestStartTime(latestStartTime);
        evidence.put("stockShortageTime", stockShortageTime);
        evidence.put("estimatedProductionHours", estimatedProductionHours);
        evidence.put("latestStartTime", latestStartTime);
        evidence.put("formula", "shiftStart+supplyHours-standingHours-planQty/defaultSpeed");
        traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.LATEST_START_PRIORITY,
                TcScheduleRuleResultEnum.PASS, evidence);
    }

    /**
     * 解析任务班次开始时间，第二天三个班次按班次顺序偏移一天。
     *
     * @param context    排程上下文
     * @param shiftOrder 六班任务顺序
     * @return 班次开始时间；配置缺失或格式非法时返回 null
     */
    private Date resolveShiftStartTime(TcScheduleContext context, Integer shiftOrder) {
        if (context == null || context.getScheduleDate() == null || shiftOrder == null
                || context.getShiftTimeWindowMap() == null) {
            return null;
        }
        TcShiftTimeWindow window = context.getShiftTimeWindowMap().get(shiftOrder);
        if (window == null || StrUtil.isBlank(window.getPlanStartTime())) {
            return null;
        }
        try {
            Date shiftDate = DateUtil.offsetDay(context.getScheduleDate(), (shiftOrder - 1) / 3);
            return DateUtil.parse(DateUtil.formatDate(shiftDate) + " " + window.getPlanStartTime());
        } catch (RuntimeException exception) {
            log.warn("[TC_LATEST_START_PRIORITY] batchNo={}, traceId={}, shiftOrder={}, planStartTime={}, reason=SHIFT_START_PARSE_FAILED",
                    context.getBatchNo(), context.getTraceId(), shiftOrder, window.getPlanStartTime(), exception);
            return null;
        }
    }

    /**
     * 按小时偏移时间。
     *
     * @param source 原时间
     * @param hours  偏移小时数，可为负数
     * @return 偏移后的时间
     */
    private Date offsetHours(Date source, BigDecimal hours) {
        long offsetMillis = hours.multiply(BigDecimal.valueOf(TcScheduleConstants.MILLIS_PER_HOUR))
                .setScale(0, RoundingMode.HALF_UP).longValue();
        return new Date(source.getTime() + offsetMillis);
    }

    /**
     * 读取非必填数值参数，无法解析时使用缺省值。
     *
     * @param context      排程上下文
     * @param paramCode    参数编码
     * @param defaultValue 缺省值
     * @return 参数数值
     */
    private BigDecimal readDecimalParam(TcScheduleContext context, String paramCode, BigDecimal defaultValue) {
        String value = readParam(context, paramCode, null);
        if (StrUtil.isBlank(value)) {
            return defaultValue;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            log.warn("[TC_PARAM_PARSE] batchNo={}, traceId={}, paramCode={}, paramValue={}, reason=INVALID_DECIMAL",
                    context.getBatchNo(), context.getTraceId(), paramCode, value, exception);
            return defaultValue;
        }
    }
    /**
     * 写入新规格判断和提前排产窗口证据。
     *
     * @param context 排程上下文
     * @param task    任务草稿
     */
    private void addNewSpecTrace(TcScheduleContext context, TcTaskDraft task) {
        TcNewSpecInfo info = task.getNewSpecInfo();
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
        traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.NEW_SPEC_DETECT,
                Boolean.TRUE.equals(info.getNewSpec())
                        ? TcScheduleRuleResultEnum.PASS : TcScheduleRuleResultEnum.SKIP,
                detectEvidence);
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
        traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.NEW_SPEC_ADVANCE_WINDOW,
                TcScheduleRuleResultEnum.PASS, windowEvidence);
    }

    /**
     * 写入实验规格判断和固定计划量证据。
     *
     * @param context 排程上下文
     * @param task    任务草稿
     */
    private void addExperimentSpecTrace(TcScheduleContext context, TcTaskDraft task) {
        TcExperimentSpecInfo info = task.getExperimentSpecInfo();
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
        traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.EXPERIMENT_SPEC_DETECT,
                info.isExperimentSpecHit() ? TcScheduleRuleResultEnum.PASS : TcScheduleRuleResultEnum.SKIP,
                detectEvidence);
        if (!info.isExperimentSpecHit()) {
            return;
        }
        Map<String, Object> planQtyEvidence = new LinkedHashMap<>();
        planQtyEvidence.put("planQty", info.getPlanQty());
        planQtyEvidence.put("planQtySource", info.getPlanQtySource());
        planQtyEvidence.put("finalTaskPlanQty", task.getPlanQty());
        planQtyEvidence.put("currentShiftDemandQty", task.getCurrentShiftDemandQty());
        planQtyEvidence.put("guardDemandQty", task.getGuardDemandQty());
        traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.EXPERIMENT_SPEC_PLAN_QTY,
                TcScheduleRuleResultEnum.PASS, planQtyEvidence);
    }
    /**
     * 写入需求量计算规则证据。
     *
     * @param context              排程上下文
     * @param task                 任务草稿
     * @param demandAlgorithmCode  需求量算法编码
     */
    private void addDemandTrace(TcScheduleContext context, TcTaskDraft task, String demandAlgorithmCode) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("algorithmCode", demandAlgorithmCode);
        evidence.put("currentShiftDemandQty", task.getCurrentShiftDemandQty());
        evidence.put("guardDemandQty", task.getGuardDemandQty());
        evidence.put("rollingStockQty", task.getRollingStockQty());
        evidence.put("currentShiftStockGapQty", task.getCurrentShiftStockGapQty());
        evidence.put("stockGapQty", task.getStockGapQty());
        evidence.put("demandQty", task.getDemandQty());
        evidence.put("sourceOrderNos", task.getSourceOrderNos());
        traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.DEMAND_QTY_CALC,
                TcScheduleRuleResultEnum.PASS, evidence);
    }

    /**
     * 写入计划量计算规则证据。
     *
     * @param context             排程上下文
     * @param task                任务草稿
     * @param planQtyStrategyCode 计划量策略编码
     */
    private void addPlanQtyTrace(TcScheduleContext context, TcTaskDraft task, String planQtyStrategyCode) {
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
        evidence.put("lossRate", task.getResolvedLossRate() == null ? task.getLossRate() : task.getResolvedLossRate());
        evidence.put("lossMatchLevel", task.getLossMatchLevel());
        evidence.put("lossMatchSource", task.getLossMatchSource());
        evidence.put("preLossPlanQty", task.getPreLossPlanQty());
        evidence.put("planQtyBeforeToolLimit", task.getPlanQtyBeforeToolLimit());
        evidence.put("calcFormulaDesc", task.getCalcFormulaDesc());
        traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.PLAN_QTY_CALC,
                TcScheduleRuleResultEnum.PASS, evidence);
    }

    /**
     * 获取任务规则证据对象，不存在时创建。
     *
     * @param context 排程上下文
     * @param task    任务草稿
     * @return 规则证据对象
     */
    private TcRuleTrace traceOf(TcScheduleContext context, TcTaskDraft task) {
        return context.getRuleTraceMap().computeIfAbsent(task.getBusinessKey(), key -> new TcRuleTrace());
    }

    /**
     * 根据任务草稿构建需求量策略输入。
     *
     * @param task    任务草稿
     * @return 需求量策略输入
     */
    private TcDemandQtyInput buildDemandQtyInput(TcTaskDraft task) {
        TcDemandQtyInput input = new TcDemandQtyInput();
        input.setSidewallCode(task.getSidewallCode());
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
    private void applyDemandQtyResult(TcTaskDraft task, TcDemandQtyResult result) {
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
     * <p>首个任务的可用工装数量等于总工装数量减去所有胎侧14点预计库存折算的占用工装数量。工装数量是全局池，
     * 因此不能按单个胎侧重复使用总工装数量。</p>
     *
     * @param context          排程上下文
     * @param stockForecastMap 胎侧库存预测结果
     * @return 首个任务计算前的全局可用工装数量；未配置总工装时返回 null 表示不启用工装限制
     */
    private BigDecimal initializeGlobalAvailableToolQty(TcScheduleContext context, Map<String, TcStockForecast> stockForecastMap) {
        BigDecimal totalToolQty = this.resolveGlobalTotalToolQty(context);
        if (totalToolQty == null) {
            return null;
        }
        Map<String, TcTaskDraft> representativeTaskMap = new LinkedHashMap<>();
        for (TcTaskDraft task : context.getTaskDraftList()) {
            if (task.getSidewallCode() != null && !representativeTaskMap.containsKey(task.getSidewallCode())) {
                representativeTaskMap.put(task.getSidewallCode(), task);
            }
        }
        BigDecimal initialUsedToolQty = BigDecimal.ZERO;
        for (Map.Entry<String, TcTaskDraft> entry : representativeTaskMap.entrySet()) {
            BigDecimal curlLength = this.resolveCurlLength(entry.getValue());
            if (curlLength.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal forecastStockQty = this.resolveForecastRollingStock(entry.getKey(), entry.getValue(), stockForecastMap);
            initialUsedToolQty = initialUsedToolQty.add(forecastStockQty.divide(curlLength,
                    TcScheduleConstants.DECIMAL_CALCULATION_SCALE, RoundingMode.HALF_UP));
        }
        BigDecimal vehicleRate = this.readDecimalParam(context, TcScheduleConstants.PARAM_VEHICLE_RATE,
                BigDecimal.ONE).max(BigDecimal.ZERO);
        return totalToolQty.subtract(initialUsedToolQty).max(BigDecimal.ZERO)
                .multiply(vehicleRate)
                .setScale(TcScheduleConstants.DECIMAL_CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 解析全局总工装数量，并校验同一轮排程携带的总工装数量一致。
     *
     * @param context 排程上下文
     * @return 全局总工装数量；未配置时返回 null
     */
    private BigDecimal resolveGlobalTotalToolQty(TcScheduleContext context) {
        BigDecimal totalToolQty = null;
        for (TcTaskDraft task : context.getTaskDraftList()) {
            if (task.getTotalToolQty() == null) {
                continue;
            }
            if (totalToolQty == null) {
                totalToolQty = task.getTotalToolQty();
                continue;
            }
            if (totalToolQty.compareTo(task.getTotalToolQty()) != 0) {
                throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.toolTotalMismatch"));
            }
        }
        return totalToolQty;
    }

    /**
     * 解析胎侧14点预计库存。
     *
     * @param sidewallCode        胎侧编码
     * @param task             任务草稿
     * @param stockForecastMap 胎侧库存预测结果
     * @return 14点预计库存，空值按0处理
     */
    private BigDecimal resolveForecastRollingStock(String sidewallCode, TcTaskDraft task, Map<String, TcStockForecast> stockForecastMap) {
        if (stockForecastMap != null) {
            TcStockForecast forecast = stockForecastMap.get(sidewallCode);
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
    private BigDecimal updateGlobalToolState(TcTaskDraft task, BigDecimal currentAvailableToolQty) {
        if (currentAvailableToolQty == null) {
            return null;
        }
        BigDecimal curlLength = this.resolveCurlLength(task);
        if (curlLength.compareTo(BigDecimal.ZERO) <= 0) {
            task.setToolUsedQty(BigDecimal.ZERO.setScale(TcScheduleConstants.DECIMAL_CALCULATION_SCALE,
                    RoundingMode.HALF_UP));
            task.setRemainingToolQty(currentAvailableToolQty);
            return currentAvailableToolQty;
        }
        BigDecimal netUsedToolQty = nvl(task.getPlanQty()).subtract(nvl(task.getCurrentShiftDemandQty()))
                .divide(curlLength, TcScheduleConstants.DECIMAL_CALCULATION_SCALE, RoundingMode.HALF_UP);
        BigDecimal remainingToolQty = currentAvailableToolQty.subtract(netUsedToolQty).max(BigDecimal.ZERO);
        if (task.getTotalToolQty() != null) {
            remainingToolQty = remainingToolQty.min(task.getTotalToolQty());
        }
        remainingToolQty = remainingToolQty.setScale(TcScheduleConstants.DECIMAL_CALCULATION_SCALE,
                RoundingMode.HALF_UP);
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
    private BigDecimal resolveCurlLength(TcTaskDraft task) {
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
    private void applyPlanQtyResult(TcTaskDraft task, TcPlanQtyResult result) {
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
        task.setPreLossPlanQty(result.getPreLossPlanQty());
        task.setPlanQtyBeforeToolLimit(result.getPlanQtyBeforeToolLimit());
        task.setPlanQty(result.getFinalPlanQty());
        task.setCalcFormulaDesc(result.getCalcFormulaDesc());
    }


    /**
     * 回写同一胎侧的下一任务班初库存状态。
     *
     * @param context 胎侧排程上下文
     * @param task    任务草稿
     */
    private void updateRollingStockState(TcScheduleContext context, TcTaskDraft task) {
        if (context == null || context.getRemainingStockMap() == null || task == null || task.getSidewallCode() == null) {
            return;
        }
        BigDecimal handoverStock = task.getPlanStockQty();
        if (handoverStock == null && task.getPlanQty() != null) {
            handoverStock = nvl(task.getRollingStockQty()).add(nvl(task.getPlanQty()))
                    .subtract(nvl(task.getCurrentShiftDemandQty())).max(BigDecimal.ZERO);
            task.setPlanStockQty(handoverStock);
        }
        context.getRemainingStockMap().put(task.getSidewallCode(), nvl(handoverStock));
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
     * @param context      胎侧排程上下文
     * @param paramCode    参数编码
     * @param defaultValue 缺省值
     * @return 参数有效值
     */
    private String readParam(TcScheduleContext context, String paramCode, String defaultValue) {
        TcParamValue paramValue = context.getParamMap().get(paramCode);
        if (paramValue == null || StrUtil.isBlank(paramValue.getEffectiveValue())) {
            return defaultValue;
        }
        return paramValue.getEffectiveValue();
    }

    /**
     * 读取需求量算法参数 TC_ALGORITHM_SWITCH。
     *
     * @param context 胎侧排程上下文
     * @return 需求量算法编码
     */
    public String readAlgorithmCode(TcScheduleContext context) {
        return readParam(context, TcScheduleConstants.PARAM_ALGORITHM_SWITCH,
                TcScheduleConstants.DEFAULT_ALGORITHM_SWITCH);
    }
}
