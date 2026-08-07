package com.zlt.aps.gsq.engine.event;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 钢丝圈排程事件发布器。
 *
 * <p>对齐胎圈 TqScheduleEventPublisher，同步通知当前 Spring 容器中注册的排程事件监听器。
 * 发布器不吞监听器异常，避免日志、解释或后续通知失败被静默忽略。</p>
 *
 * <p>当前为简化实现，仅支持 IGsqScheduleEventListener 监听器机制。
 * 后续可扩展为 Spring ApplicationEvent 发布或 MQ 异步通知。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class GsqScheduleEventPublisher {

    private final List<IGsqScheduleEventListener> listeners;

    /**
     * 创建钢丝圈排程事件发布器。
     *
     * @param listeners 监听器集合，允许为空
     */
    public GsqScheduleEventPublisher(@Nullable List<IGsqScheduleEventListener> listeners) {
        this.listeners = listeners == null ? new ArrayList<>() : listeners;
    }

    /**
     * 发布钢丝圈自动滚动完成事件。
     *
     * @param factoryCode      工厂编码
     * @param scheduleDate     排程日期
     * @param targetShiftOrder 目标班次
     * @param batchNo          滚动批次号
     * @param status           状态（SUCCESS/FAILED）
     * @param affectedCount    影响的排程记录数
     * @param beforeStockQty   滚动前预计库存
     * @param afterStockQty    滚动后预计库存
     */
    public void publishAutoRollingEvent(String factoryCode, Date scheduleDate, Integer targetShiftOrder,
                                        String batchNo, String status, int affectedCount,
                                        double beforeStockQty, double afterStockQty) {
        GsqScheduleEvent event = new GsqScheduleEvent();
        event.setFactoryCode(factoryCode);
        event.setScheduleDate(scheduleDate);
        event.setTargetShiftOrder(targetShiftOrder);
        event.setBatchNo(batchNo);
        event.setStatus(status);
        event.setAffectedCount(affectedCount);
        event.setBeforeStockQty(beforeStockQty);
        event.setAfterStockQty(afterStockQty);
        this.publish(event);
    }

    /**
     * 发布钢丝圈排程事件。
     *
     * @param event 钢丝圈排程事件
     */
    public void publish(GsqScheduleEvent event) {
        if (event == null) {
            log.warn("钢丝圈排程事件发布跳过：event 为空");
            return;
        }
        if (CollUtil.isEmpty(this.listeners)) {
            log.debug("钢丝圈排程事件无监听器，event={}", event);
            return;
        }
        for (IGsqScheduleEventListener listener : this.listeners) {
            listener.onEvent(event);
        }
    }
}