package com.zlt.aps.gsq.engine.handler;

import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.vo.GsqScheduleParams;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import com.zlt.aps.gsq.engine.vo.GsqTotalPlanQtyVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * S2: 钢丝圈需求计算Handler。
 *
 * <p>核心逻辑：</p>
 * <ol>
 *   <li>从胎圈6班次排程结果中，按BOM分解计算钢丝圈6班次需求量</li>
 *   <li>计算库存供应时长（用于S3阶段班次优先级判定）</li>
 *   <li>计算末班（6班）估值（胎圈7班消耗量估值）</li>
 *   <li>聚合6班次总计划量统计</li>
 * </ol>
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
        return "S2-需求计算与机台分配";
    }

    @Override
    protected void doHandle(GsqScheduleContext context) {
        // 1. 计算末班（6班）估值：取胎圈4~6班均值作为7班估值
        calcLastShiftEstimate(context);

        // 2. 聚合6班次总计划量
        aggregateTotalPlanQty(context);

        log.info("[S2] 需求计算完成, 排程记录数: {}", context.getScheduleList().size());
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
            log.info("[S2] 末班估值开关关闭, 钢丝圈6班计划量保持为0");
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

        log.info("[S2] 末班估值计算完成, 估值记录数: {}", context.getLastShiftEstimateMap().size());
    }

    /**
     * 聚合6班次总计划量统计。
     *
     * @param context 排程上下文
     */
    private void aggregateTotalPlanQty(GsqScheduleContext context) {
        GsqTotalPlanQtyVo total = new GsqTotalPlanQtyVo();
        List<GsqScheduleResultVo> list = context.getScheduleList();

        for (GsqScheduleResultVo vo : list) {
            double c1 = vo.getClass1PlanQty() == null ? 0 : vo.getClass1PlanQty();
            double c2 = vo.getClass2PlanQty() == null ? 0 : vo.getClass2PlanQty();
            double c3 = vo.getClass3PlanQty() == null ? 0 : vo.getClass3PlanQty();
            double c4 = vo.getClass4PlanQty() == null ? 0 : vo.getClass4PlanQty();
            double c5 = vo.getClass5PlanQty() == null ? 0 : vo.getClass5PlanQty();
            double c6 = vo.getClass6PlanQty() == null ? 0 : vo.getClass6PlanQty();

            total.setTotalClass1PlanQty(total.getTotalClass1PlanQty() + c1);
            total.setTotalClass2PlanQty(total.getTotalClass2PlanQty() + c2);
            total.setTotalClass3PlanQty(total.getTotalClass3PlanQty() + c3);
            total.setTotalClass4PlanQty(total.getTotalClass4PlanQty() + c4);
            total.setTotalClass5PlanQty(total.getTotalClass5PlanQty() + c5);
            total.setTotalClass6PlanQty(total.getTotalClass6PlanQty() + c6);
        }

        double grandTotal = total.getTotalClass1PlanQty() + total.getTotalClass2PlanQty()
                + total.getTotalClass3PlanQty() + total.getTotalClass4PlanQty()
                + total.getTotalClass5PlanQty() + total.getTotalClass6PlanQty();
        total.setTotalPlanQty(grandTotal);

        context.setTotalPlanQtyVo(total);
        log.info("[S2] 6班次总计划量统计完成, 总量: {}", grandTotal);
    }
}
