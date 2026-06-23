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
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 胎面需求量和计划量默认计算步骤服务。
 *
 * <p>通过 {@link TmStrategyRegistry} 获取计划量策略，替代直接 new 策略对象。
 * 计划量策略编码从上下文参数读取（参数码 {@code TM_PLAN_QTY_STRATEGY}，缺省 {@code "DEFAULT"}）。
 * 计划量计算使用库存预测中的 rollingStockQty（14点预计库存）。</p>
 */
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
            addDemandTrace(context, task, demandQtyAlgorithmCode);

            // 已有计划量表示上游已完成特殊业务调整，此处保持不变。
            if (task.getPlanQty() == null) {
                TmPlanQtyResult planQtyResult = planQtyStrategy.calculate(task, context);
                applyPlanQtyResult(task, planQtyResult);
            }
            addPlanQtyTrace(context, task, planQtyStrategyCode);
        }
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
