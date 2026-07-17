package com.zlt.aps.tc.engine.event;

/**
 * 胎侧排程事件监听器接口。
 *
 * <p>用于扩展插单、删除、转机台、调量、自动排程等事件后的日志、解释快照或通知动作。
 * 监听器异常不由发布器吞掉，调用方可按事务边界决定回滚策略。</p>
 */
@FunctionalInterface
public interface ITcScheduleEventListener {

    /**
     * 处理胎侧排程事件。
     *
     * @param event 胎侧排程事件对象
     * @throws RuntimeException 监听器处理失败时可直接抛出，由上层事务入口处理
     */
    void onEvent(TcScheduleEvent event);
}
