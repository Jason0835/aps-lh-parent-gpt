package com.zlt.aps.gsq.engine.handler;

import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.vo.GsqScheduleParams;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * S2.2: 钢丝圈需求量计算Handler。
 *
 * <p>核心逻辑：</p>
 * <ol>
 *   <li>从胎圈6班次排程结果中，按BOM分解计算钢丝圈6班次需求量</li>
 *   <li>计算末班（6班）估值（胎圈7班消耗量估值，取胎圈4~6班均值）</li>
 * </ol>
 *
 * <p>Phase 3 重构：从原 S2 拆分，库存预测移至 S2.1（GsqStockPredictHandler），
 * 计划量聚合移至 S2.3（GsqPlanQtyCalcHandler），与胎圈 TQ 阶段粒度对齐。</p>
 *
 * <p>班次对应关系：</p>
 * <ul>
 *   <li>钢丝圈1班（D中） ← 胎圈2班（D+1夜）</li>
 *   <li>钢丝圈2班（D+1夜） ← 胎圈3班（D+1早）</li>
 *   <li>钢丝圈3班（D+1早） ← 胎圈4班（D+1中）</li>
 *   <li>钢丝圈4班（D+1中） ← 胎圈5班（D+2夜）</li>
 *   <li>钢丝圈5班（D+2夜） ← 胎圈6班（D+2早）</li>
 *   <li>钢丝圈6班（D+2早） ← 胎圈7班（D+2中，估值）</li>
 * </ul>
 *
 * @author APS
 */
@Slf4j
@Component
public class GsqDemandCalcHandler extends AbsGsqScheduleStepHandler {

    @Override
    protected String getStepName() {
        return "S2.2-需求量计算";
    }

    @Override
    protected void doHandle(GsqScheduleContext context) {
        // 1. 计算末班（6班）估值：取胎圈4~6班均值作为7班估值
        calcLastShiftEstimate(context);

        log.info("[S2.2] 需求量计算完成, 排程记录数: {}", context.getScheduleList().size());
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
            log.info("[S2.2] 末班估值开关关闭, 钢丝圈6班计划量保持为0");
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
        }

        log.info("[S2.2] 末班估值计算完成, 估值记录数: {}", context.getLastShiftEstimateMap().size());
    }
}
