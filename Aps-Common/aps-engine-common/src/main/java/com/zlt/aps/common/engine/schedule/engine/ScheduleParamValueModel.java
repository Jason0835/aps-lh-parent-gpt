package com.zlt.aps.common.engine.schedule.engine;
import lombok.Data;
/** TM/TC 排程参数快照公共模型。 */
@Data
public class ScheduleParamValueModel {
    protected String paramCode;
    protected String paramValue;
    protected String defaultValue;
    protected String source;

    public String getEffectiveValue() {
        return paramValue != null && !paramValue.trim().isEmpty() ? paramValue : defaultValue;
    }
}
