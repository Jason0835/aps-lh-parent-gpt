package com.zlt.aps.tc.domain.vo;

import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎侧人工操作滚动队列任务。
 */
@Data
public class TcManualRollingTask {

    /** 任务所属横表结果，新增任务没有结果 ID。 */
    private TcScheduleResult result;

    /** 原始任务班次。 */
    private Integer originalShiftOrder;

    /** 原始班内顺序。 */
    private Integer originalSequence;

    /** 最早允许写入的班次；多班插单用于保持用户指定班次，普通滚动任务为空。 */
    private Integer minimumShiftOrder;

    /** 待滚动计划量。 */
    private BigDecimal planQty;

    /** 已完成量，仅在任务首段保留。 */
    private BigDecimal finishQty;

    /** 是否已经写入过完成量。 */
    private boolean finishQtyWritten;
}
