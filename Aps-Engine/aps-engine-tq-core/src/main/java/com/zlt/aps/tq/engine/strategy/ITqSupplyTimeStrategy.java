package com.zlt.aps.tq.engine.strategy;

import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;

/**
 * 胎圈供应时长计算策略接口。
 *
 * <p>S2.1 库存预测阶段调用，根据预计库存计算库存供应时长（小时）。</p>
 *
 * <p>已知实现：</p>
 * <ul>
 *   <li>{@code BY_STOCK}：算法1-线下手工排产，库存保证班数 = 14点预计库存 / 胎圈每班需求量</li>
 *   <li>{@code BY_SHIFT}：算法2-系统算法，14点预计库存逐班递减直到库存不够</li>
 * </ul>
 *
 * @author APS
 */
public interface ITqSupplyTimeStrategy {

    /**
     * 获取策略编码（用于参数 {@code TQ_SUPPLY_TIME_STRATEGY_CODE} 路由）。
     *
     * @return 策略编码
     */
    String getStrategyCode();

    /**
     * 计算库存供应时长（小时）。
     *
     * <p>计算结果应直接写入 {@link TqScheduleResultVo#setSupplyTime(Double)}。</p>
     *
     * @param scheduleVo 排程结果 VO（含预计库存、成型各班计划量）
     * @param stockQty   预计库存
     * @param context    排程上下文（用于读取参数和写入规则证据）
     */
    void calcSupplyTime(TqScheduleResultVo scheduleVo, Double stockQty, TqScheduleContext context);
}
