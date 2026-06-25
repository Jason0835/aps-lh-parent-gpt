package com.zlt.aps.tm.engine.domain;

import lombok.Data;

/**
 * 胎面转机台目标位置对象。
 *
 * <p>用于描述转机台后的目标班次和插入锚点。该对象只承载位置参数，不修改任务链。</p>
 */
@Data
public class TmTransferPosition {

    /** 目标班次顺序 */
    private Integer shiftOrder;

    /** 目标锚点任务标识 */
    private String anchorTaskId;
}
