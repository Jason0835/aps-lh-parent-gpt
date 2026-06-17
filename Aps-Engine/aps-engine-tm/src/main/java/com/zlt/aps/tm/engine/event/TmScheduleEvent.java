package com.zlt.aps.tm.engine.event;

import com.zlt.aps.tm.api.enums.TmScheduleEventTypeEnum;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import lombok.Data;

import java.util.Date;

/**
 * 胎面排程事件对象。
 *
 * <p>用于在自动排程、插单、调量、转机台、删除等操作后传递低敏事件摘要。
 * 该对象只承载事件上下文，不修改任务链、不写数据库。</p>
 */
@Data
public class TmScheduleEvent {

    /** 工厂编号 */
    private String factoryCode;

    /** 排程批次号 */
    private String batchNo;

    /** 追踪标识 */
    private String traceId;

    /** 操作人 */
    private String operator;

    /** 事件类型编码 */
    private String eventType;

    /** 事件类型说明 */
    private String eventDesc;

    /** 事件摘要 */
    private String summary;

    /** 事件发生时间 */
    private Date eventTime;

    /**
     * 根据上下文和事件枚举创建事件对象。
     *
     * @param context   胎面排程上下文，可为空
     * @param eventEnum 事件类型枚举
     * @param summary   事件摘要
     * @return 胎面排程事件对象
     * @throws IllegalArgumentException 事件类型为空时抛出
     */
    public static TmScheduleEvent of(TmScheduleContext context, TmScheduleEventTypeEnum eventEnum, String summary) {
        if (eventEnum == null) {
            throw new IllegalArgumentException("胎面排程事件类型不能为空");
        }
        TmScheduleEvent event = new TmScheduleEvent();
        if (context != null) {
            event.setFactoryCode(context.getFactoryCode());
            event.setBatchNo(context.getBatchNo());
            event.setTraceId(context.getTraceId());
            event.setOperator(context.getOperator());
        }
        event.setEventType(eventEnum.getCode());
        event.setEventDesc(eventEnum.getDesc());
        event.setSummary(summary);
        event.setEventTime(new Date());
        return event;
    }
}
