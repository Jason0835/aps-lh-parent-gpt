package com.zlt.aps.tm.engine.service;

import com.zlt.aps.tm.engine.domain.TmScheduleContext;

/**
 * 胎面需求量和计划量计算步骤服务。
 *
 * <p>负责调用需求量和计划量策略，输出待排任务草稿的计划量分量。骨架阶段不落具体算法。</p>
 */
public interface ITmPlanCalcService {

    /**
     * 执行计划量计算。
     *
     * @param context 胎面排程上下文，方法会按实现补充待排任务计划量
     */
    void calculate(TmScheduleContext context);
}
