package com.zlt.aps.gsq.engine.event;

/**
 * 钢丝圈排程事件监听器。
 *
 * <p>对齐胎圈 ITqScheduleEventListener，由下游模块实现以消费 GsqScheduleEvent。
 * 监听器抛出的异常会原样向上抛出，发布器不吞异常。</p>
 *
 * @author APS
 */
public interface IGsqScheduleEventListener {

    /**
     * 处理钢丝圈排程事件。
     *
     * @param event 钢丝圈排程事件
     */
    void onEvent(GsqScheduleEvent event);
}