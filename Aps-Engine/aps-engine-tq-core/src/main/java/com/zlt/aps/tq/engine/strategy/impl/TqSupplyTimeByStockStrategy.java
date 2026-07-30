package com.zlt.aps.tq.engine.strategy.impl;

import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.enums.TqScheduleRuleCodeEnum;
import com.zlt.aps.tq.engine.enums.TqScheduleRuleResultEnum;
import com.zlt.aps.tq.engine.strategy.ITqSupplyTimeStrategy;
import com.zlt.aps.tq.engine.strategy.TqDemandCalcHelper;
import com.zlt.aps.tq.engine.vo.TqScheduleParams;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 胎圈供应时长策略 - 算法1（线下手工排产）。
 *
 * <p>对应原 {@code TqDemandCalcHandler.computeSupplyTime} 中 {@code demandCalcMode=1} 分支：</p>
 * <ul>
 *   <li>胎圈每班需求量 = 成型三班最大计划量 × 系数</li>
 *   <li>库存保证班数 = 14点预计库存 / 胎圈每班需求量</li>
 *   <li>供应时长 = 保证班数 × 8 小时</li>
 * </ul>
 *
 * <p>由 S2.1 {@code TqStockPredictHandler} 通过参数 {@code TQ_SUPPLY_TIME_STRATEGY_CODE=BY_STOCK}
 * 或兼容旧参数 {@code demandCalcMode=1} 路由调用。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class TqSupplyTimeByStockStrategy implements ITqSupplyTimeStrategy {

    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    /** 策略编码：BY_STOCK（库存/单班需求量） */
    private static final String STRATEGY_CODE = "BY_STOCK";

    @Override
    public String getStrategyCode() {
        return STRATEGY_CODE;
    }

    @Override
    public void calcSupplyTime(TqScheduleResultVo scheduleVo, Double stockQty, TqScheduleContext context) {
        TqScheduleParams params = context.getParams();
        double coefficient = params.getDemandCoefficient() == null ? 2D : params.getDemandCoefficient();

        // 成型三班最大值
        double cxClass1 = scheduleVo.getCxClass1Plan() == null ? 0 : scheduleVo.getCxClass1Plan();
        double cxClass2 = scheduleVo.getCxClass2Plan() == null ? 0 : scheduleVo.getCxClass2Plan();
        double cxClass3 = scheduleVo.getCxClass3Plan() == null ? 0 : scheduleVo.getCxClass3Plan();
        double maxCxPlan = Math.max(Math.max(cxClass1, cxClass2), cxClass3);
        double tqPerClassDemand = BigDecimalUtil.mul(maxCxPlan, coefficient);

        double stock = stockQty == null ? 0D : stockQty;
        double guaranteeShifts = tqPerClassDemand > 0
                ? BigDecimalUtil.div(stock, tqPerClassDemand, 1) : 999;
        double supplyTime = BigDecimalUtil.mul(guaranteeShifts, 8);

        scheduleVo.setSupplyTime(supplyTime);

        // 记录规则证据
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("algorithm", "BY_STOCK");
        evidence.put("stockQty", stock);
        evidence.put("maxCxPlan", maxCxPlan);
        evidence.put("coefficient", coefficient);
        evidence.put("tqPerClassDemand", tqPerClassDemand);
        evidence.put("guaranteeShifts", guaranteeShifts);
        evidence.put("supplyTime", supplyTime);
        TqDemandCalcHelper.addRuleTrace(context, scheduleVo.getBeadCode(),
                TqScheduleRuleCodeEnum.SUPPLY_TIME_ALGORITHM, TqScheduleRuleResultEnum.HIT, evidence);
        TqDemandCalcHelper.addRuleTrace(context, scheduleVo.getBeadCode(),
                TqScheduleRuleCodeEnum.SUPPLY_TIME_RESULT, TqScheduleRuleResultEnum.HIT,
                TqDemandCalcHelper.evidence("supplyTime", supplyTime));

        TqDemandCalcHelper.logSchedule(autoScheduleLogService, scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                "计算库存供应时长（算法1-简单除法）",
                "物料编号：" + scheduleVo.getBeadCode() + "，预计库存：" + stock
                        + "，成型三班最大值：" + maxCxPlan + "，胎圈每班需求量：" + tqPerClassDemand
                        + "，保证班数：" + guaranteeShifts + "，供应时长：" + supplyTime);
    }
}
