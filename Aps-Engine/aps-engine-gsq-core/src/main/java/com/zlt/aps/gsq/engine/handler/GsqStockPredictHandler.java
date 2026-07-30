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
 *   <li>计算末班（6班）估值：取胎圈4~6班均值作为7班估值，BOM分解得到钢丝圈6班需求</li>
 *   <li>标记供应时长不足的规格（低于阈值时记录规则证据）</li>
 * </ol>
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

        // 2. 计算末班估值
        calcLastShiftEstimate(context);

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

    /**
     * 计算末班估值：当胎圈7班实际消耗量未知时，取胎圈4~6班均值作为估值。
     *
     * <p>估值策略：取胎圈4/5/6班的均值，作为胎圈7班的预估消耗量，
     * 进而BOM分解得到钢丝圈6班的需求估值。</p>
     *
     * @param context 排程上下文
     */
    private void calcLastShiftEstimate(GsqScheduleContext context) {
        GsqScheduleParams params = context.getParams();
        // 末班估值开关
        if (!"1".equals(params.getLastShiftEstimateEnabled())) {
            log.info("[S2.1] 末班估值开关关闭, 钢丝圈6班计划量保持为0");
            return;
        }

        int estimateClassCount = params.getLastShiftEstimateClassCount() == null ? 3 : params.getLastShiftEstimateClassCount();
        if (estimateClassCount <= 0) {
            estimateClassCount = 3;
        }

        for (GsqScheduleResultVo vo : context.getScheduleList()) {
            // 取胎圈4~6班均值作为7班估值
            double tqClass4 = vo.getTqClass4Plan() == null ? 0 : vo.getTqClass4Plan();
            double tqClass5 = vo.getTqClass5Plan() == null ? 0 : vo.getTqClass5Plan();
            double tqClass6 = vo.getTqClass6Plan() == null ? 0 : vo.getTqClass6Plan();

            double sum = tqClass4 + tqClass5 + tqClass6;
            double avg = sum / estimateClassCount;

            // BOM分解得到钢丝圈6班需求估值
            double bomQty = context.getBomDecomposeMap().getOrDefault(vo.getSteelRingCode(), 1D);
            double gsqClass6Estimate = avg * bomQty;

            vo.setTqClass7Plan(avg);
            vo.setClass6PlanQty(gsqClass6Estimate);
            context.getLastShiftEstimateMap().put(vo.getSteelRingCode(), gsqClass6Estimate);

            // 末班估值规则证据
            Map<String, Object> evidence = new HashMap<>();
            evidence.put("steelRingCode", vo.getSteelRingCode());
            evidence.put("tqClass4", tqClass4);
            evidence.put("tqClass5", tqClass5);
            evidence.put("tqClass6", tqClass6);
            evidence.put("avgClass7Estimate", avg);
            evidence.put("bomQty", bomQty);
            evidence.put("gsqClass6Estimate", gsqClass6Estimate);
            context.getRuleTrace(vo.getSteelRingCode()).addRuleHit(
                    GsqScheduleRuleCodeEnum.LAST_SHIFT_ESTIMATE,
                    GsqScheduleRuleResultEnum.HIT, evidence);
        }

        log.info("[S2.1] 末班估值计算完成, 估值记录数: {}", context.getLastShiftEstimateMap().size());
    }
}
