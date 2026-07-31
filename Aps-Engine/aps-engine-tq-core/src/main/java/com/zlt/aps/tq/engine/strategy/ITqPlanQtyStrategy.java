package com.zlt.aps.tq.engine.strategy;

import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import com.zlt.aps.tq.engine.vo.TqTotalPlanQtyVo;

/**
 * 胎圈计划量计算策略接口。
 *
 * <p>S2.3 计划量计算阶段调用，基于 {@link ITqDemandQtyStrategy} 产出的备库触发标记和收尾判断，
 * 完成 6 班滚动计划量计算与备库总量分摊。</p>
 *
 * <p>已知实现：</p>
 * <ul>
 *   <li>{@code DEFAULT}：默认计划量算法（沿用原 TqDemandCalcHandler 中的 6 班滚动计算逻辑）</li>
 * </ul>
 *
 * @author APS
 */
public interface ITqPlanQtyStrategy {

    /**
     * 获取策略编码（用于参数 {@code TQ_PLAN_QTY_STRATEGY_CODE} 路由）。
     *
     * @return 策略编码
     */
    String getStrategyCode();

    /**
     * 执行 6 班计划量计算。
     *
     * <p>本方法应直接修改 {@link TqScheduleResultVo} 中的 {@code class1PlanQty ~ class6PlanQty} 字段，
     * 并把各班计划量累加到 {@link TqTotalPlanQtyVo}。</p>
     *
     * @param scheduleVo     排程结果 VO（应已包含 S2.2 产出的备库触发标记和收尾判断）
     * @param totalPlanQtyVo 总计划量统计 VO（按班次累加）
     * @param context        排程上下文（用于读取参数、写入规则证据）
     */
    void calcPlanQty(TqScheduleResultVo scheduleVo, TqTotalPlanQtyVo totalPlanQtyVo, TqScheduleContext context);
}
