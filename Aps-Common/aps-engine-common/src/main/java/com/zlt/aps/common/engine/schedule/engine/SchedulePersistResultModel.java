package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** TM/TC 排程持久化汇总公共运行态模型。 */
@Data
public class SchedulePersistResultModel implements ScheduleQualityPersistSummary {
    /**
     * 落库排程结果主表记录数。
     */
    protected int resultCount;
    /** 落库排程解释记录数。 */
    protected int explainCount;
    /** 落库未排任务记录数。 */
    protected int unplannedCount;
    /** 落库过程发生的错误次数。 */
    protected int errorCount;
    /** 落库错误消息列表。 */
    protected List<String> errorMsgList = new ArrayList<>();
    /** 最近一次落库错误消息。 */
    protected String lastErrorMsg;

    /**
     * 记录一条落库错误信息并累加错误计数。
     *
     * @param errorMsg 错误信息文本
     */
    public void addErrorMsg(String errorMsg) {
        errorCount++;
        lastErrorMsg = errorMsg;
        errorMsgList.add(errorMsg);
    }
}
