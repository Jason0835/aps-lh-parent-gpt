package com.zlt.aps.tc.engine.domain.manual;

/**
 * 胎侧人工滚动操作类型。
 */
public enum TcManualRollingOperationEnum {
    /** 人工插单。 */
    INSERT,
    /** 删除任务。 */
    DELETE,
    /** 调整计划量。 */
    CHANGE_QTY,
    /** 转移机台。 */
    CHANGE_MACHINE,
    /** 自动滚动调整。 */
    AUTO_ROLLING
}
