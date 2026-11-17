package com.zlt.aps.common.engine.schedule.engine;
import lombok.Data;
/** TM/TC 排程参数快照公共模型。 */
@Data
public class ScheduleParamValueModel {
    /**
     * 系统参数编码。
     */
    protected String paramCode;
    /** 系统参数名称。 */
    protected String paramName;
    /** 系统参数实际配置值。 */
    protected String paramValue;
    /** 系统参数默认值。 */
    protected String defaultValue;
    /** 参数取值来源说明（如数据库配置、代码默认值）。 */
    protected String source;
    /**
     * 加载阶段按自动排程规则处理后的有效值；为空时沿用原始配置。
     */
    protected String normalizedValue;
    /**
     * 可选原始配置值列表，用于开关拒绝非法或重复配置时保留证据。
     */
    protected java.util.List<String> configuredValues;

    /**
     * 获取当前最终生效的参数值，优先返回规范化值。
     *
     * @return 生效的参数值，可能为空
     */
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
