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
 * 胎圈供应时长策略 - 算法2（系统算法，逐班递减）。
 *
 * <p>对应原 {@code TqDemandCalcHandler.computeSupplyTimeByDeduction} 方法：</p>
 * <ul>
 *   <li>从成型1班开始逐班判断</li>
 *   <li>预计库存 - 该班消耗量(×系数) ≥ 0 时，供应时长 + 8 小时</li>
 *   <li>预计库存不足以覆盖该班消耗量时，供应时长加上 (剩余库存/该班消耗×系数) × 8 小时</li>
 * </ul>
 *
 * <p>由 S2.1 {@code TqStockPredictHandler} 通过参数 {@code TQ_SUPPLY_TIME_STRATEGY_CODE=BY_SHIFT}
 * 或兼容旧参数 {@code demandCalcMode!=1}（默认）路由调用。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class TqSupplyTimeByShiftStrategy implements ITqSupplyTimeStrategy {

    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    /** 策略编码：BY_SHIFT（逐班递减） */
    private static final String STRATEGY_CODE = "BY_SHIFT";

    @Override
    public String getStrategyCode() {
        return STRATEGY_CODE;
    }

    @Override
    public void calcSupplyTime(TqScheduleResultVo scheduleVo, Double stockQty, TqScheduleContext context) {
        TqScheduleParams params = context.getParams();
        double coefficient = params.getDemandCoefficient() == null ? 2D : params.getDemandCoefficient();

        // 成型 8 个班的胎圈消耗量（×系数）
        double cxClass1 = TqDemandCalcHelper.mulCxPlan(scheduleVo.getCxClass1Plan(), coefficient);
        double cxClass2 = TqDemandCalcHelper.mulCxPlan(scheduleVo.getCxClass2Plan(), coefficient);
        double cxClass3 = TqDemandCalcHelper.mulCxPlan(scheduleVo.getCxClass3Plan(), coefficient);
        double cxClass4 = TqDemandCalcHelper.mulCxPlan(scheduleVo.getCxClass4Plan(), coefficient);
        double cxClass5 = TqDemandCalcHelper.mulCxPlan(scheduleVo.getCxClass5Plan(), coefficient);
        double cxClass6 = TqDemandCalcHelper.mulCxPlan(scheduleVo.getCxClass6Plan(), coefficient);
        double cxClass7 = TqDemandCalcHelper.mulCxPlan(scheduleVo.getCxClass7Plan(), coefficient);
        double cxClass8 = TqDemandCalcHelper.mulCxPlan(scheduleVo.getCxClass8Plan(), coefficient);

        TqDemandCalcHelper.logSchedule(autoScheduleLogService, scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                "计算库存供应时长前数据",
                TqDemandCalcHelper.logSplit(
                        "具体算法：从成型1班开始逐班判断，预计库存-该班消耗量(×需求系数" + coefficient + ")大于等于0时，供应时长+8小时；预计库存不足以覆盖该班消耗量时，供应时长加上：(剩余库存/该班消耗×系数)*8小时",
                        "物料编号：" + scheduleVo.getBeadCode() + "，预计库存：" + stockQty,
                        "成型1班消耗(×系数)：" + cxClass1 + "，成型2班消耗(×系数)：" + cxClass2,
                        "成型3班消耗(×系数)：" + cxClass3 + "，成型4班消耗(×系数)：" + cxClass4,
                        "成型5班消耗(×系数)：" + cxClass5 + "，成型6班消耗(×系数)：" + cxClass6,
                        "成型7班消耗(×系数)：" + cxClass7 + "，成型8班消耗(×系数)：" + cxClass8));

        double remnantStock = stockQty == null ? 0D : stockQty;

        // 逐班计算供应时长（8小时/班），覆盖成型 8 个班
        double[] cxClassPlans = {cxClass1, cxClass2, cxClass3, cxClass4, cxClass5, cxClass6, cxClass7, cxClass8};
        for (double classPlan : cxClassPlans) {
            remnantStock = BigDecimalUtil.sub(remnantStock, classPlan);
            if (remnantStock >= 0) {
                // 剩余库存仍可覆盖该班消耗，供应时长 + 8 小时
                Double supplyTime = scheduleVo.getSupplyTime() == null ? 0D : scheduleVo.getSupplyTime();
                scheduleVo.setSupplyTime(BigDecimalUtil.add(supplyTime, 8));
            } else {
                // 剩余库存不足以覆盖该班消耗
                Double supplyTime = scheduleVo.getSupplyTime() == null ? 0D : scheduleVo.getSupplyTime();
                double classSupplyTime = BigDecimalUtil.mul(
                        BigDecimalUtil.div(BigDecimalUtil.add(remnantStock, classPlan), classPlan), 8);
                supplyTime = supplyTime + BigDecimalUtil.roundDown(classSupplyTime, 1);
                scheduleVo.setSupplyTime(supplyTime);
                break;
            }
        }

        // 记录规则证据
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("algorithm", "BY_SHIFT");
        evidence.put("stockQty", stockQty);
        evidence.put("coefficient", coefficient);
        evidence.put("supplyTime", scheduleVo.getSupplyTime());
        TqDemandCalcHelper.addRuleTrace(context, scheduleVo.getBeadCode(),
                TqScheduleRuleCodeEnum.SUPPLY_TIME_ALGORITHM, TqScheduleRuleResultEnum.HIT, evidence);
        TqDemandCalcHelper.addRuleTrace(context, scheduleVo.getBeadCode(),
                TqScheduleRuleCodeEnum.SUPPLY_TIME_RESULT, TqScheduleRuleResultEnum.HIT,
                TqDemandCalcHelper.evidence("supplyTime", scheduleVo.getSupplyTime()));

        TqDemandCalcHelper.logSchedule(autoScheduleLogService, scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                "计算库存供应时长结束",
                "物料编号：" + scheduleVo.getBeadCode() + "，库存供应时长=" + scheduleVo.getSupplyTime());
    }
}
