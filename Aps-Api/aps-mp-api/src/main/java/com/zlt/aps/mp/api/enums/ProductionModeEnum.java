package com.zlt.aps.mp.api.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 月计划-排产模式
 * 1 交付优先
 * 2 效率优先
 *
 * @author ZLT
 * 20260611
 */
@Getter
public enum ProductionModeEnum {
    /**
     * 1 交付优先
     */
    DELIVER_PRIORITY(1, "交付优先", "ui.data.production.mode.deliver.priority"),
    /**
     * 2 效率优先
     */
    EFFICIENCY_PRIORITY(2, "效率优先", "ui.data.production.mode.efficiency.priority");

    private Integer mode;

    private String desc;

    private String i18nKey;

    ProductionModeEnum(Integer mode, String desc, String i18nKey) {
        this.mode = mode;
        this.desc = desc;
        this.i18nKey = i18nKey;
    }

    /**
     * 获取排产模式对象实例
     *
     * @param mode
     * @return
     */
    public static ProductionModeEnum getInstance(Integer mode) {
        if (null == mode) {
            return null;
        }
        return Arrays.stream(values()).filter(productionMode -> productionMode.getMode().equals(mode)).findFirst().orElse(null);
    }
}
