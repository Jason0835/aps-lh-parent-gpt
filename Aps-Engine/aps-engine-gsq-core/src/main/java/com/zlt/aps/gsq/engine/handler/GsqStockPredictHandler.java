package com.zlt.aps.gsq.engine.handler;

import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.domain.GsqRuleTraceItem;
import com.zlt.aps.gsq.engine.enums.GsqScheduleRuleCodeEnum;
import com.zlt.aps.gsq.engine.enums.GsqScheduleRuleResultEnum;
import com.zlt.aps.gsq.engine.vo.GsqScheduleParams;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * S2.1: 钢丝圈库存预测Handler。
 *
 * <p>Phase 1 重构：从原 S2 GsqDemandCalcHandler 中拆分出来，对齐胎圈 TqStockPredictHandler。</p>
 *
 * <p>核心逻辑：</p>
 * <ol>
 *   <li>计算库存供应时长（当前预计库存 / 胎圈每班消耗量）</li>
 *   <li>标记供应时长不足的规格（低于阈值时记录规则证据）</li>
 * </ol>
 *
 * <p>末班估值（需求量）已拆分至 S2.2 {@code GsqDemandCalcHandler}，与胎圈 TQ 阶段粒度对齐。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class GsqStockPredictHandler extends AbsGsqScheduleStepHandler {

    /** 默认供应时长警告阈值（班次） */
    private static final double DEFAULT_SUPPLY_TIME_THRESHOLD = 3.0;

    @Override
    protected String getStepName() {
        return "S2.1-库存预测";
    }

    @Override
    protected void doHandle(GsqScheduleContext context) {
        // 1. 计算库存供应时长
        calcSupplyDuration(context);

        log.info("[S2.1] 库存预测完成, 排程记录数: {}", context.getScheduleList().size());
    }

    /**
     * 计算库存供应时长：当前预计库存 / 胎圈每班消耗量。
     *
     * <p>供应时长用于 S3 阶段的班次优先级判定（供应时长越短，优先级越高）。</p>
     *
     * @param context 排程上下文
     */
    private void calcSupplyDuration(GsqScheduleContext context) {
        GsqScheduleParams params = context.getParams();
        double threshold = DEFAULT_SUPPLY_TIME_THRESHOLD;
        if (params != null && params.getSupplyTimeThreshold() != null && params.getSupplyTimeThreshold() > 0) {
            threshold = params.getSupplyTimeThreshold();
        }

        for (GsqScheduleResultVo vo : context.getScheduleList()) {
            double planStock = vo.getPlanStockQty() == null ? 0 : vo.getPlanStockQty();
            double tqPerShift = vo.getTqClass2Plan() == null ? 0 : vo.getTqClass2Plan();

            double supplyTime;
            if (tqPerShift <= 0) {
                supplyTime = Double.MAX_VALUE;
            } else {
                supplyTime = planStock / tqPerShift;
            }
            vo.setSupplyTime(supplyTime);

            // 供应时长低于阈值时记录规则证据
            if (supplyTime < threshold && supplyTime != Double.MAX_VALUE) {
                Map<String, Object> evidence = new HashMap<>();
                evidence.put("steelRingCode", vo.getSteelRingCode());
                evidence.put("supplyTime", supplyTime);
                evidence.put("threshold", threshold);
                evidence.put("planStockQty", planStock);
                evidence.put("tqPerShift", tqPerShift);
                context.getRuleTrace(vo.getSteelRingCode()).addRuleHit(
                        GsqScheduleRuleCodeEnum.STOCK_PREDICT,
                        GsqScheduleRuleResultEnum.TRIGGER, evidence);
            }
        }

        log.info("[S2.1] 供应时长计算完成, 阈值: {}班次", threshold);
    }
}
