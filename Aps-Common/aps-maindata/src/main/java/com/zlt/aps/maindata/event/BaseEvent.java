package com.zlt.aps.maindata.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Date;
import java.util.UUID;

/**
 * 事件的基类，封装通用属性
 *
 * @author zlt
 */
@Getter
public abstract class BaseEvent extends ApplicationEvent {

    /**
     * 事件唯一标识
     */
    private final String eventId;

    /**
     * 事件创建时间
     */
    private final Date createTime;

    /**
     * 事件模块类型，对应字典：work_calendar_proc
     * 01-月计划,02-硫化,03-成型,04-胎面,05-胎侧,06-内衬,07-垫胶,08-胎圈,09-钢丝圈,10-斜裁,11-直裁,12-压延,15-零度,16-密炼
     */
    private final String eventModuleType;

    /**
     * 操作人（可选）
     */
    private final String operator;

    public BaseEvent(Object source, String eventModuleType, String operator) {
        super(source);
        this.eventId = UUID.randomUUID().toString();
        this.createTime = new Date();
        this.eventModuleType = eventModuleType;
        this.operator = operator;
    }

}
