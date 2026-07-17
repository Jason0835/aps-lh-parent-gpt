package com.zlt.aps.tc.engine.strategy;

import com.zlt.aps.tc.engine.domain.TcPlanQtyResult;
import com.zlt.aps.tc.engine.domain.TcScheduleContext;
import com.zlt.aps.tc.engine.domain.TcTaskDraft;

/**
 * 胎侧计划量算法策略接口。
 *
 * <p>用于计算库存抵扣、损耗补偿、工装限制、收尾补正和最终计划量。策略不直接写数据库。</p>
 */
public interface ITcPlanQtyStrategy {

    /**
     * 获取策略编码。
     *
     * @return 策略编码
     */
    String getStrategyCode();

    /**
     * 计算计划量。
     *
     * @param draft   待排任务草稿
     * @param context 胎侧排程上下文
     * @return 计划量计算结果
     */
    TcPlanQtyResult calculate(TcTaskDraft draft, TcScheduleContext context);
}
