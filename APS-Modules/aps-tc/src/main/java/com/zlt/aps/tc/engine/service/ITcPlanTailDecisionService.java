package com.zlt.aps.tc.engine.service;

import com.zlt.aps.tc.engine.domain.TcTaskDraft;

import java.util.List;

/**
 * 胎侧计划量汇总组收尾判定服务。
 *
 * <p>后续月计划余量来源确定后，仅替换该接口实现，不改变计划量汇总主流程。</p>
 */
public interface ITcPlanTailDecisionService {

    /**
     * 将收尾判定结果写入汇总生产任务。
     *
     * @param aggregateTask 汇总生产任务
     * @param sourceTaskList 原始来源任务
     */
    void applyTailDecision(TcTaskDraft aggregateTask, List<TcTaskDraft> sourceTaskList);
}
