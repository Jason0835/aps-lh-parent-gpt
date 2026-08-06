package com.zlt.aps.tq.engine.domain.manual;

/**
 * 胎圈人工滚动操作类型。
 */
public enum TqManualRollingOperationEnum {

    /** 人工插单 */
    INSERT,

    /** 删除任务 */
    DELETE,

    /** 调整计划量 */
    CHANGE_QTY,

    /** 转移机台 */
    CHANGE_MACHINE,

    /** 自动滚动调整 */
    AUTO_ROLLING
}
