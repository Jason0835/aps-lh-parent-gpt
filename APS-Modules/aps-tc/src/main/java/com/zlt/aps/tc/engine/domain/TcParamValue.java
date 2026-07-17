package com.zlt.aps.tc.engine.domain;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

/**
 * 胎侧排程参数快照值。
 *
 * <p>用于保存一次排程中命中的参数值、默认值和来源，供需求计算、计划量计算和解释快照复用。
 * 该对象只承载参数快照，不修改任务链。</p>
 */
@Data
public class TcParamValue {

    /** 参数编码 */
    private String paramCode;

    /** 参数值 */
    private String paramValue;

    /** 默认值 */
    private String defaultValue;

    /** 参数来源说明 */
    private String source;

    /**
     * 获取有效参数值。
     *
     * @return 优先返回参数值，参数值为空时返回默认值
     */
    public String getEffectiveValue() {
        return StrUtil.isNotBlank(paramValue) ? paramValue : defaultValue;
    }
}
