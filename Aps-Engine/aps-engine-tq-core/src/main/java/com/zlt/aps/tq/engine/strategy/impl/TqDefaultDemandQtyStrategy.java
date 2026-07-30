package com.zlt.aps.tq.engine.strategy.impl;

import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.enums.TqScheduleRuleCodeEnum;
import com.zlt.aps.tq.engine.enums.TqScheduleRuleResultEnum;
import com.zlt.aps.tq.engine.strategy.ITqDemandQtyStrategy;
import com.zlt.aps.tq.engine.strategy.TqDemandCalcHelper;
import com.zlt.aps.tq.engine.vo.TqScheduleParams;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 胎圈需求量默认策略。
 *
 * <p>对应原 {@code TqDemandCalcHandler.computeTqPlanQty} 开头的收尾判断逻辑：</p>
 * <ol>
 *   <li>汇总 6 班总需排产量（成型 3~8 班消耗 × 系数）</li>
 *   <li>调用 {@link TqDemandCalcHelper#checkCloseOutByEmbryo} 判断是否收尾规格</li>
 *   <li>设置 closeOutSpecFlag（0=收尾，1=非收尾）</li>
 * </ol>
 *
 * <p>本策略产出的是 {@code closeOutSpecFlag}，供后续 S2.3 计划量策略使用。</p>
 *
 * <p>注意：备库触发判断和 6 班滚动计划量计算保留在 {@link TqDefaultPlanQtyStrategy} 中，
 * 因为它们与计划量计算强耦合（备库触发依赖于每班排产后的可用库存状态）。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class TqDefaultDemandQtyStrategy implements ITqDemandQtyStrategy {

    /** 策略编码：DEFAULT */
    private static final String STRATEGY_CODE = "DEFAULT";

    @Override
    public String getStrategyCode() {
        return STRATEGY_CODE;
    }

    @Override
    public void calcDemandQty(TqScheduleResultVo scheduleVo, TqScheduleContext context) {
        TqScheduleParams params = context.getParams();
        double coefficient = params.getDemandCoefficient() == null ? 2D : params.getDemandCoefficient();

        // 汇总成型 3~8 班的消耗量（胎圈 1~6 班对应供应成型 3~8 班）
        double totalCxConsume = BigDecimalUtil.add(
                scheduleVo.getCxClass3Plan() == null ? 0 : scheduleVo.getCxClass3Plan(),
                BigDecimalUtil.add(
                        scheduleVo.getCxClass4Plan() == null ? 0 : scheduleVo.getCxClass4Plan(),
                        BigDecimalUtil.add(
                                scheduleVo.getCxClass5Plan() == null ? 0 : scheduleVo.getCxClass5Plan(),
                                BigDecimalUtil.add(
                                        scheduleVo.getCxClass6Plan() == null ? 0 : scheduleVo.getCxClass6Plan(),
                                        BigDecimalUtil.add(
                                                scheduleVo.getCxClass7Plan() == null ? 0 : scheduleVo.getCxClass7Plan(),
                                                scheduleVo.getCxClass8Plan() == null ? 0 : scheduleVo.getCxClass8Plan())))));

        // 6 班总需排产量 = 成型 3~8 班消耗量 × 系数
        double totalTqDemand = BigDecimalUtil.mul(totalCxConsume, coefficient);

        // 收尾判断（基于胎胚关联汇总）：月计划余量 <= 6 班总需排产量 → 收尾规格
        boolean isCloseOutSpec = TqDemandCalcHelper.checkCloseOutByEmbryo(scheduleVo, context, totalTqDemand);
        scheduleVo.setCloseOutSpecFlag(isCloseOutSpec ? "0" : "1"); // 0=收尾，1=非收尾

        // 记录规则证据
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("totalCxConsume", totalCxConsume);
        evidence.put("coefficient", coefficient);
        evidence.put("totalTqDemand", totalTqDemand);
        evidence.put("isCloseOutSpec", isCloseOutSpec);
        evidence.put("closeOutSpecFlag", scheduleVo.getCloseOutSpecFlag());
        TqDemandCalcHelper.addRuleTrace(context, scheduleVo.getBeadCode(),
                TqScheduleRuleCodeEnum.CLOSE_OUT_JUDGE, TqScheduleRuleResultEnum.HIT, evidence);

        log.info("[S2.2] 物料 {} 收尾判断完成：totalTqDemand={}, closeOutSpecFlag={}",
                scheduleVo.getBeadCode(), totalTqDemand, scheduleVo.getCloseOutSpecFlag());
    }
}
