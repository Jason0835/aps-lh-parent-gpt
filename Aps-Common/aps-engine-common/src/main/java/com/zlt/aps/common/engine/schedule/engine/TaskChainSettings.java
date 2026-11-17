package com.zlt.aps.common.engine.schedule.engine;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 任务链公共引擎使用的领域常量集合。 */
@Getter
@AllArgsConstructor
public final class TaskChainSettings {

    /**
     * 班次编码前缀（如 TM_SHIFT_ / TC_SHIFT_）。
     */
    private final String shiftCodePrefix;
    /** 自动排程尾插操作标识。 */
    private final String autoAppendOperation;
    /** 自动排程头插操作标识。 */
    private final String autoPrependOperation;
    /** 人工插单操作标识。 */
    private final String manualInsertOperation;
    /** 人工删单操作标识。 */
    private final String manualDeleteOperation;
    /** 人工转机台操作标识。 */
    private final String manualTransferOperation;
    /** 人工调量操作标识。 */
    private final String changeQtyOperation;
}
