package com.zlt.aps.gsq.engine.handler;

import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import com.zlt.aps.gsq.engine.vo.GsqTotalPlanQtyVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * S2.3: 钢丝圈计划量计算Handler。
 *
 * <p>Phase 1 重构：从原 S2 GsqDemandCalcHandler 中拆分出来，对齐胎圈 TqPlanQtyCalcHandler。</p>
 *
 * <p>核心逻辑：</p>
 * <ol>
 *   <li>聚合6班次总计划量统计</li>
 *   <li>校验计划量合理性（取整与工装限制等后续迭代补充）</li>
 * </ol>
 *
 * <p>与胎圈的差异：胎圈 S2.3 包含6班滚动计算、备库触发判断与分摊、取整与工装限制等复杂逻辑；
 * 钢丝圈的计划量由 S1 阶段从胎圈BOM分解直接得到，本步骤仅做聚合统计。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class GsqPlanQtyCalcHandler extends AbsGsqScheduleStepHandler {

    @Override
    protected String getStepName() {
        return "S2.3-计划量计算";
    }

    @Override
    protected void doHandle(GsqScheduleContext context) {
        // 1. 聚合6班次总计划量统计
        aggregateTotalPlanQty(context);

        log.info("[S2.3] 计划量计算完成, 排程记录数: {}", context.getScheduleList().size());
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
        log.info("[S2.3] 6班次总计划量统计完成, 总量: {}", grandTotal);
    }
}
