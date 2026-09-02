package com.zlt.aps.common.engine.schedule.engine;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 任务链公共引擎使用的领域常量集合。 */
@Getter
@AllArgsConstructor
public final class TaskChainSettings {

    private final String shiftCodePrefix;
    private final String autoAppendOperation;
    private final String autoPrependOperation;
    private final String manualInsertOperation;
    private final String manualDeleteOperation;
    private final String manualTransferOperation;
    private final String changeQtyOperation;
}
