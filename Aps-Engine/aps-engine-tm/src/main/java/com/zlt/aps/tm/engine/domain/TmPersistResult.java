package com.zlt.aps.tm.engine.domain;

import lombok.Data;

/**
 * 胎面排程落库结果。
 *
 * <p>用于返回结果表、解释表、未排任务和异常数量。该对象只承载汇总，不控制事务。</p>
 */
@Data
public class TmPersistResult {

    /** 结果写入数量 */
    private int resultCount;

    /** 解释写入数量 */
    private int explainCount;

    /** 未排任务数量 */
    private int unplannedCount;

    /** 异常数量 */
    private int errorCount;
}
