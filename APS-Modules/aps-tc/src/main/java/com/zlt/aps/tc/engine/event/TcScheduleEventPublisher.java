package com.zlt.aps.tc.engine.event;

import cn.hutool.core.collection.CollUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.tc.api.enums.TcScheduleErrorCodeEnum;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 胎侧排程事件发布器。
 *
 * <p>同步通知当前 Spring 容器中注册的排程事件监听器。发布器不吞监听器异常，
 * 避免日志、解释或后续通知失败被静默忽略。</p>
 */
@Component
public class TcScheduleEventPublisher {

    private final List<ITcScheduleEventListener> listeners;

    /**
     * 创建胎侧排程事件发布器。
     *
     * @param listeners 监听器集合，允许为空
     */
    public TcScheduleEventPublisher(@Nullable List<ITcScheduleEventListener> listeners) {
        this.listeners = listeners == null ? new ArrayList<>() : listeners;
    }

    /**
     * 发布胎侧排程事件。
     *
     * @param event 胎侧排程事件
     * @throws ServiceException         事件为空时抛出
     * @throws RuntimeException         任一监听器处理失败时原样抛出
     */
    public void publish(TcScheduleEvent event) {
        if (event == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_PARAM_EMPTY.getDefaultMessage() + ":event");
        }
        if (CollUtil.isEmpty(listeners)) {
            return;
        }
        for (ITcScheduleEventListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}
