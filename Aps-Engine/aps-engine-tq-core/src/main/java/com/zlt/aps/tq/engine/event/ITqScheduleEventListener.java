package com.zlt.aps.tq.engine.event;

/**
 * 胎圈排程事件监听器。
 *
 * <p>对齐胎面 ITmScheduleEventListener，由下游模块实现以消费 TqScheduleEvent。
 * 监听器抛出的异常会原样向上抛出，发布器不吞异常。</p>
 *
 * @author APS
 */
public interface ITqScheduleEventListener {

    /**
     * 处理胎圈排程事件。
     *
     * @param event 胎圈排程事件
     */
    void onEvent(TqScheduleEvent event);
}
