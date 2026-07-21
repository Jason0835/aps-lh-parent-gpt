package com.zlt.aps.tc.engine.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 胎侧排程落库结果。
 *
 * <p>用于返回结果表、解释表、未排任务和异常数量。该对象只承载汇总，不控制事务。</p>
 */
@Data
public class TcPersistResult {

    /** 结果写入数量 */
    private int resultCount;

    /** 解释写入数量 */
    private int explainCount;

    /** 未排任务数量 */
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
