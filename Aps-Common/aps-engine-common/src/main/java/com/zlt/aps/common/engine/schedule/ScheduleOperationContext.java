package com.zlt.aps.common.engine.schedule;

import lombok.Data;

import java.util.Date;

/**
 * 排程任务链操作上下文。
 *
 * <p>用于向链表修改方法传递操作人、操作原因和追踪标识，后续日志与解释信息可通过
 * 同一上下文串联。该对象只承载上下文数据，不修改任务链。</p>
 */
@Data
public class ScheduleOperationContext {

    /** 操作人 */
    private String operator;

    /** 操作原因 */
    private String reason;

    /** 追踪标识 */
    private String traceId;

    /** 操作时间 */
    private Date operateTime;

    /**
     * 创建排程操作上下文。
     *
     * @param operator 操作人
     * @param reason   操作原因
     * @param traceId  追踪标识
     */
    public ScheduleOperationContext(String operator, String reason, String traceId) {
        this.operator = operator;
        this.reason = reason;
        this.traceId = traceId;
        this.operateTime = new Date();
    }
}
