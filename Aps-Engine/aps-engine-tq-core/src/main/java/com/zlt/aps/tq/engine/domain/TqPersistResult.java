package com.zlt.aps.tq.engine.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 胎圈排程落库结果。
 *
 * <p>Phase 4 重构新增：对齐胎侧 {@code TcPersistResult}，用于返回结果表、解释表、未排任务和异常数量汇总。
 * 该对象只承载汇总，不控制事务，由 {@code TqResultPersistHandler} 在 S6 阶段统一填充。</p>
 *
 * @author APS
 */
@Data
public class TqPersistResult {

    /** 结果写入数量 */
    private int resultCount;

    /** 解释写入数量（即填充了 explainJson 的记录数） */
    private int explainCount;

    /** 未排任务数量（6个班次均无计划量的无效记录数） */
    private int unplannedCount;

    /** 异常数量 */
    private int errorCount;

    /** 错误信息列表 */
    private List<String> errorMsgList = new ArrayList<>();

    /** 最后一条错误信息 */
    private String lastErrorMsg;

    /**
     * 追加落库错误信息。
     *
     * @param errorMsg 错误信息
     */
    public void addErrorMsg(String errorMsg) {
        errorCount++;
        lastErrorMsg = errorMsg;
        errorMsgList.add(errorMsg);
    }
}
