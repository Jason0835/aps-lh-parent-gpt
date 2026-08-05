package com.zlt.aps.gsq.engine.domain.manual;

/**
 * 钢丝圈人工滚动操作类型。
 */
public enum GsqManualRollingOperationEnum {

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
