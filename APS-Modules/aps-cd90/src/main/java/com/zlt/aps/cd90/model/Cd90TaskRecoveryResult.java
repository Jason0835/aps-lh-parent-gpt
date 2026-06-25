package com.zlt.aps.cd90.model;

import lombok.Builder;
import lombok.Data;

/** 外部Job触发遗留任务补偿后的汇总结果。 */
@Data
@Builder
public class Cd90TaskRecoveryResult {
    private int scannedCount;
    private int failedCount;
    private int skippedCount;
}
