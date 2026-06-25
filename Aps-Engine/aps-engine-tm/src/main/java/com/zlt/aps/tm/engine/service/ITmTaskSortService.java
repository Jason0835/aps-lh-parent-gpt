package com.zlt.aps.tm.engine.service;

import com.zlt.aps.tm.engine.domain.TmScheduleContext;

/**
 * 胎面待排任务排序步骤服务。
 *
 * <p>负责按策略建立稳定的待排顺序。骨架阶段只定义入口，不写死排序算法。</p>
 */
public interface ITmTaskSortService {

    /**
     * 执行待排任务排序。
     *
     * @param context 胎面排程上下文，方法会按实现调整待排任务顺序
     */
    void sort(TmScheduleContext context);
}
