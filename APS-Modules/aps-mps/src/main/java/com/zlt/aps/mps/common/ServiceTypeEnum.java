package com.zlt.aps.mps.common;

/**
 * @author Gim
 */
public enum ServiceTypeEnum {
    UNKNOWN("未知"),
    REQUEST("同步服务-请求类型"),
    ISSUED("下发接口"),
    GET("抓取类型"),
    TIMING("定时接口"),
    ;

    private String description;

    // 获取枚举描述
    public String getDescription() {
        return description;
    }

    ServiceTypeEnum(String description) {
        this.description = description;
    }

    public static ServiceTypeEnum valueOf(Integer value) {
        if (null == value || value < 0 || value >= values().length) {
            return UNKNOWN;
        }
        return values()[value];
    }
}
