package com.zlt.aps.tm.engine.strategy;

import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;

import java.util.Comparator;

/**
 * 胎面待排任务排序策略接口。
 *
 * <p>用于构建待排任务优先级比较器。策略只返回比较器，不直接修改任务链。</p>
 */
public interface ITmTaskSortStrategy {

    /**
     * 获取策略编码。
     *
     * @return 策略编码
     */
    String getStrategyCode();

    /**
     * 构建任务排序比较器。
     *
     * @param context 胎面排程上下文
     * @return 任务排序比较器
     */
    Comparator<TmTaskDraft> buildComparator(TmScheduleContext context);
}
