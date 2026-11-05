package com.zlt.aps.common.engine.schedule.engine;
import lombok.Data;
/** TM/TC 排程参数快照公共模型。 */
@Data
public class ScheduleParamValueModel {
    protected String paramCode;
    protected String paramName;
    protected String paramValue;
    protected String defaultValue;
    protected String source;
    /**
     * 加载阶段按自动排程规则处理后的有效值；为空时沿用原始配置。
     */
    protected String normalizedValue;
    /**
     * 可选原始配置值列表，用于开关拒绝非法或重复配置时保留证据。
     */
    protected java.util.List<String> configuredValues;

    public String getEffectiveValue() {
        return normalizedValue != null ? normalizedValue : this.resolveConfiguredValue();
    }

    /**
     * 获取配置值或配置表默认值，供需要保留原始口径的业务使用。
     *
     * @return 原配置有效值，可能为空
     */
    public String resolveConfiguredValue() {
        return paramValue != null && !paramValue.trim().isEmpty() ? paramValue : defaultValue;
    }
}
