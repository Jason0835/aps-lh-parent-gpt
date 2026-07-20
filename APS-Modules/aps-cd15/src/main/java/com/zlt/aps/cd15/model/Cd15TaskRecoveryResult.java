package com.zlt.aps.cd15.model;

import lombok.Builder;
import lombok.Data;

/** 遗留运行中任务补偿汇总。 */
@Data
@Builder
public class Cd15TaskRecoveryResult {

    /** 扫描任务数。 */
    private int scannedCount;

    /** 补偿为失败的任务数。 */
    private int failedCount;

    /** 未达到补偿条件而跳过的任务数。 */
    private int skippedCount;
}
