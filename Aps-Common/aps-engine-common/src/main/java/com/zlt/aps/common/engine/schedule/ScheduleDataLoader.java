package com.zlt.aps.common.engine.schedule;

/**
 * 自动排程数据加载端口。
 *
 * <p>公共排程模板只依赖该端口，不依赖胎面或胎侧的 Mapper、实体和 SQL。
 * 具体业务模块负责按照自身数据表结构完成加载，并将结果写入领域上下文。</p>
 *
 * @param <C> 排程上下文类型
 */
@FunctionalInterface
public interface ScheduleDataLoader<C> {

    /**
     * 加载自动排程所需的参数、需求、库存、机台、工装及历史任务数据。
     *
     * @param context 排程上下文
     */
    void load(C context);
}
