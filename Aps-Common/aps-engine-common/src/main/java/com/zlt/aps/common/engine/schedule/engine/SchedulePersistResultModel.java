package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** TM/TC 排程持久化汇总公共运行态模型。 */
@Data
public class SchedulePersistResultModel implements ScheduleQualityPersistSummary {
    protected int resultCount;
    protected int explainCount;
    protected int unplannedCount;
    protected int errorCount;
    protected List<String> errorMsgList = new ArrayList<>();
    protected String lastErrorMsg;

    public void addErrorMsg(String errorMsg) {
        errorCount++;
        lastErrorMsg = errorMsg;
        errorMsgList.add(errorMsg);
    }
}
