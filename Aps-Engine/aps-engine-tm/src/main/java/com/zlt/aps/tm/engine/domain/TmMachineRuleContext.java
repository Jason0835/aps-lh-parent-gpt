package com.zlt.aps.tm.engine.domain;

import lombok.Data;

/**
 * 胎面机台规则上下文。
 *
 * <p>用于向机台过滤规则和评分策略传递当前任务和排程上下文。该对象只聚合入参，
 * 不直接修改任务链。</p>
 */
@Data
public class TmMachineRuleContext {

    /** 当前待排任务 */
    private TmTaskDraft taskDraft;

    /** 排程上下文 */
    private TmScheduleContext scheduleContext;
}
