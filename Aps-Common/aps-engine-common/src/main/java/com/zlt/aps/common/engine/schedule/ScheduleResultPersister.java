package com.zlt.aps.common.engine.schedule;

/**
 * 自动排程结果持久化端口。
 *
 * <p>公共排程模板通过该端口提交结果，具体业务模块负责保存已排任务、未排任务、
 * 解释明细和过程日志，并保留自身的事务边界。</p>
 *
 * @param <C> 排程上下文类型
 */
@FunctionalInterface
public interface ScheduleResultPersister<C> {

    /**
     * 持久化自动排程结果。
     *
     * @param context 排程上下文
     */
    void persist(C context);
}
