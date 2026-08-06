package com.zlt.aps.tq.engine.event;

import cn.hutool.core.collection.CollUtil;
import com.zlt.aps.tq.api.domain.dto.TqRollingRecalcRequestDTO;
import com.zlt.aps.tq.api.domain.vo.TqRollingRecalcResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 胎圈排程事件发布器。
 *
 * <p>对齐胎面 TmScheduleEventPublisher，同步通知当前 Spring 容器中注册的排程事件监听器。
 * 发布器不吞监听器异常，避免日志、解释或后续通知失败被静默忽略。</p>
 *
 * <p>当前为简化实现，仅支持 ITqScheduleEventListener 监听器机制。
 * 后续可扩展为 Spring ApplicationEvent 发布或 MQ 异步通知。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class TqScheduleEventPublisher {

    private final List<ITqScheduleEventListener> listeners;

    /**
     * 创建胎圈排程事件发布器。
     *
     * @param listeners 监听器集合，允许为空
     */
    public TqScheduleEventPublisher(@Nullable List<ITqScheduleEventListener> listeners) {
        this.listeners = listeners == null ? new ArrayList<>() : listeners;
    }

    /**
     * 发布胎圈自动滚动完成事件。
     *
     * @param request 滚动请求
     * @param response 滚动响应
     */
    public void publishRollingEvent(TqRollingRecalcRequestDTO request, TqRollingRecalcResponseVO response) {
        if (request == null || response == null) {
            log.warn("胎圈排程事件发布跳过：request 或 response 为空");
            return;
        }
        TqScheduleEvent event = new TqScheduleEvent();
        event.setFactoryCode(request.getFactoryCode());
        event.setScheduleDate(request.getScheduleDate());
        event.setTargetShiftOrder(request.getTargetShiftOrder());
        event.setRunKey(response.getRunKey());
        event.setStatus(response.getStatus());
        event.setAdjustedBeadCount(response.getAdjustedBeadCount());
        event.setAffectedResultCount(response.getAffectedResultCount());
        event.setBeforePlanQty(response.getBeforePlanQty());
        event.setAfterPlanQty(response.getAfterPlanQty());
        this.publish(event);
    }

    /**
     * 发布胎圈排程事件。
     *
     * @param event 胎圈排程事件
     */
    public void publish(TqScheduleEvent event) {
        if (event == null) {
            log.warn("胎圈排程事件发布跳过：event 为空");
            return;
        }
        if (CollUtil.isEmpty(this.listeners)) {
            log.debug("胎圈排程事件无监听器，event={}", event);
            return;
        }
        for (ITqScheduleEventListener listener : this.listeners) {
            listener.onEvent(event);
        }
    }
}
