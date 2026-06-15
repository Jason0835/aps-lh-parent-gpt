package com.zlt.aps.tm.engine.event;

import cn.hutool.core.collection.CollUtil;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 胎面排程事件发布器。
 *
 * <p>同步通知当前 Spring 容器中注册的排程事件监听器。发布器不吞监听器异常，
 * 避免日志、解释或后续通知失败被静默忽略。</p>
 */
@Component
public class TmScheduleEventPublisher {

    private final List<ITmScheduleEventListener> listeners;

    /**
     * 创建胎面排程事件发布器。
     *
     * @param listeners 监听器集合，允许为空
     */
    public TmScheduleEventPublisher(@Nullable List<ITmScheduleEventListener> listeners) {
        this.listeners = listeners == null ? new ArrayList<>() : listeners;
    }

    /**
     * 发布胎面排程事件。
     *
     * @param event 胎面排程事件
     * @throws IllegalArgumentException 事件为空时抛出
     * @throws RuntimeException         任一监听器处理失败时原样抛出
     */
    public void publish(TmScheduleEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("胎面排程事件不能为空");
        }
        if (CollUtil.isEmpty(listeners)) {
            return;
        }
        for (ITmScheduleEventListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}
