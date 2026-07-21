package com.zlt.aps.tc.engine.service;

import com.zlt.aps.tc.engine.domain.TcScheduleContext;

/**
 * 胎侧待排任务排序步骤服务。
 *
 * <p>负责按策略建立稳定的待排顺序。骨架阶段只定义入口，不写死排序算法。</p>
 */
public interface ITcTaskSortService {

    /**
     * 执行待排任务排序。
     *
     * @param context 胎侧排程上下文，方法会按实现调整待排任务顺序
     */
    void sort(TcScheduleContext context);
}
