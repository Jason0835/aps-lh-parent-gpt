package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

/**
 * TM/TC 机台规则上下文公共运行态模型。
 *
 * @param <T> 当前待排任务类型
 * @param <C> 排程上下文类型
 */
@Data
public class ScheduleMachineRuleContext<T, C> {

    /** 当前待排任务。 */
    private T taskDraft;

    /** 当前排程上下文。 */
    private C scheduleContext;
}
