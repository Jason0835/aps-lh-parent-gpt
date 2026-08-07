package com.zlt.aps.tq.engine.domain;

import lombok.Data;

/**
 * 胎圈转机台目标位置。
 */
@Data
public class TqTransferPosition {

    /** 目标机台编码 */
    private String machineCode;

    /** 目标班次（1~6） */
    private Integer shiftOrder;

    /** 锚点任务ID（在锚点之后插入；为空时追加链尾） */
    private String anchorTaskId;
}
