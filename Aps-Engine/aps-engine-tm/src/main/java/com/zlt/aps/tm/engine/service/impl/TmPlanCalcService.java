package com.zlt.aps.tm.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 胎面需求量和计划量默认计算步骤服务。
 *
 * <p>通过 {@link TmStrategyRegistry} 获取计划量策略，替代直接 new 策略对象。
 * 计划量策略编码从上下文参数读取（参数码 {@code TM_PLAN_QTY_STRATEGY}，缺省 {@code "DEFAULT"}）。
 * 计划量计算使用库存预测中的 rollingStockQty（14点预计库存）。</p>
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

        // 初始化 per-tread 剩余可抵扣库存（初值取6点库存净值），逐班递减供库存抵扣使用
        Map<String, BigDecimal> remainingStockMap = new HashMap<>();
        if (stockForecastMap != null) {
            for (Map.Entry<String, TmStockForecast> entry : stockForecastMap.entrySet()) {
                BigDecimal sixClock = entry.getValue().getSixClockStockQty();
                remainingStockMap.put(entry.getKey(), sixClock != null ? sixClock : BigDecimal.ZERO);
            }
        }
        context.setRemainingStockMap(remainingStockMap);

        // 防御性稳定排序：按胎面编码、班次顺序升序，保证同胎面任务按班次递进抵扣库存（最终排产顺序由 TASK_SORT 步骤重排）
        context.getTaskDraftList().sort(Comparator
                .comparing(TmTaskDraft::getTreadCode, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TmTaskDraft::getShiftOrder, Comparator.nullsLast(Comparator.naturalOrder())));

        for (TmTaskDraft task : context.getTaskDraftList()) {
            // 从库存预测结果中获取 rollingStockQty 并设置到任务中
            if (stockForecastMap != null && task.getTreadCode() != null) {
                TmStockForecast forecast = stockForecastMap.get(task.getTreadCode());
                if (forecast != null) {
                    task.setRollingStockQty(forecast.getRollingStockQty());
                    task.setSixClockStockQty(forecast.getSixClockStockQty());
                }
            }

            // 旧骨架数据只提供 demandQty 时，将其作为当前班基础需求，避免默认策略按空值计算为 0。
            if (task.getCurrentShiftDemandQty() == null && task.getDemandQty() != null) {
                task.setCurrentShiftDemandQty(task.getDemandQty());
            }

            // 通过需求量策略计算库存保证缺口、基础需求量和供应时长，供排序和计划量策略复用。
            TmDemandQtyResult demandQtyResult = demandQtyStrategy.calculate(buildDemandQtyInput(task), context);
            applyDemandQtyResult(task, demandQtyResult);
            addNewSpecTrace(context, task);
            addDemandTrace(context, task, demandQtyAlgorithmCode);
            // 打印需求量计算公式
            log.info("[TM_DEMAND_QTY_CALC] treadCode={}, shiftOrder={}, 算法编码={}, 保证范围内成型胎面需求量【guardDemandQty】-当前班开始滚动库存【rollingStockQty】=库存保证缺口【stockGapQty】，max(当前班成型胎面需求量【currentShiftDemandQty】，库存保证缺口【stockGapQty】)=需求量【demandQty】",
                    task.getTreadCode(), task.getShiftOrder(), demandQtyAlgorithmCode);
            log.info("[TM_DEMAND_QTY_CALC_DETAIL] treadCode={}, shiftOrder={}, guardDemandQty={}, rollingStockQty={}, stockGapQty={}, currentShiftDemandQty={}, demandQty={}",
                    task.getTreadCode(), task.getShiftOrder(),
                    task.getGuardDemandQty(), task.getRollingStockQty(), task.getStockGapQty(),
                    task.getCurrentShiftDemandQty(), task.getDemandQty());
            // 打印供应时长计算公式
            log.info("[TM_DEMAND_QTY_SUPPLY] treadCode={}, shiftOrder={}, 供应时长【supplyHours】小时=当前班开始滚动库存【rollingStockQty】/(保证范围内成型胎面需求量【guardDemandQty】/保证范围总小时数【guardRangeHours】)",
                    task.getTreadCode(), task.getShiftOrder());
            log.info("[TM_DEMAND_QTY_SUPPLY_DETAIL] treadCode={}, shiftOrder={}, supplyHours={}, rollingStockQty={}, guardDemandQty={}, guardRangeHours={}",
                    task.getTreadCode(), task.getShiftOrder(),
                    task.getSupplyHours(), task.getRollingStockQty(),
                    task.getGuardDemandQty(), task.getGuardRangeHours());

            // 已有计划量表示上游已完成特殊业务调整，此处保持不变。
            if (task.getPlanQty() == null) {
                TmPlanQtyResult planQtyResult = planQtyStrategy.calculate(task, context);
                applyPlanQtyResult(task, planQtyResult);
            }
            addPlanQtyTrace(context, task, planQtyStrategyCode);
            // 打印计划量计算公式
            if (task.getPlanQty() != null) {
                log.info("[TM_PLAN_QTY_CALC] treadCode={}, shiftOrder={}, 策略编码={}, 计划量计算路径={}",
                        task.getTreadCode(), task.getShiftOrder(), planQtyStrategyCode, task.getCalcFormulaDesc());
                log.info("[TM_PLAN_QTY_CALC_DETAIL] treadCode={}, shiftOrder={}, demandQty={}, stockDeductQty={}, baseDemandQty={}, lossAddQty={}, toolLimitAdjustQty={}, minStartAdjustQty={}, tailRoundAdjustQty={}, capacityAdjustQty={}, planQty={}",
                        task.getTreadCode(), task.getShiftOrder(),
                        task.getDemandQty(), task.getStockDeductQty(), task.getBaseDemandQty(),
                        task.getLossAddQty(), task.getToolLimitAdjustQty(),
                        task.getMinStartAdjustQty(), task.getTailRoundAdjustQty(),
                        task.getCapacityAdjustQty(), task.getPlanQty());
            }
        }
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
     * @param task 任务草稿
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
        task.setStockGapQty(result.getStockGapQty());
        task.setDemandQty(result.getDemandQty());
        task.setGuardShiftCount(result.getGuardShiftCount());
        task.setSupplyHours(result.getSupplyHours());
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
        task.setMinStartAdjustQty(result.getMinStartAdjustQty());
        task.setTailRoundAdjustQty(result.getTailRoundAdjustQty());
        task.setCapacityAdjustQty(result.getCapacityAdjustQty());
        task.setPlanQty(result.getFinalPlanQty());
        task.setCalcFormulaDesc(result.getCalcFormulaDesc());
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
