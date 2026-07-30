package com.zlt.aps.tq.engine.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 胎圈排程规则执行结果枚举。
 *
 * <p>定义规则命中的执行结果，用于结构化证据 {@link com.zlt.aps.tq.engine.domain.TqRuleTrace} 落库。</p>
 *
 * @author APS
 */
@Getter
@AllArgsConstructor
public enum TqScheduleRuleResultEnum {

    /** 命中（规则生效，对结果产生影响） */
    HIT("HIT", "命中"),
    /** 未命中（规则未生效，未影响结果） */
    MISS("MISS", "未命中"),
    /** 跳过（因前置条件不满足而跳过） */
    SKIP("SKIP", "跳过"),
    /** 调整（规则对结果产生调整） */
    ADJUST("ADJUST", "调整"),
    /** 触发（规则触发后续动作） */
    TRIGGER("TRIGGER", "触发");

    /** 结果编码 */
    private final String code;

    /** 结果描述 */
    private final String description;

    /**
     * 根据编码获取枚举
     *
     * @param code 结果编码
     * @return 结果枚举，未找到返回 null
     */
    public static TqScheduleRuleResultEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (TqScheduleRuleResultEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
