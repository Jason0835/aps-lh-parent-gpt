package com.zlt.aps.factory.enums;

import java.util.Arrays;

/**
 * 工厂TBR模具排产日志环节枚举定义
 *
 * @author ZLT
 * 20251210
 */
public enum TbrMouldProductionLogType {
    /**
     * 10-01 开始初始化
     */
    START_INIT("10-01", "开始初始化"),
    /**
     * 10-04 初始化结束
     */
    INIT_COMPLETE("10-04", "初始化结束"),
    /**
     * 10-09 初始化存储结束
     */
    SAVE_INIT("10-09", "初始化存储结束"),
    /**
     * 99 一键排产结束
     */
    WHOLE_PRODUCTION_END("99", "一键排产结束"),
    /**
     * 00 unknown
     */
    UNKNOWN_LOG("00", "unknown");

    private String typeValue;

    private String desc;

    TbrMouldProductionLogType(String typeValue, String desc) {
        this.typeValue = typeValue;
        this.desc = desc;
    }

    /**
     * 得到排产阶段枚举类
     *
     * @param logType
     * @return
     */
    public static TbrMouldProductionLogType getInstance(String logType) {
        if (null == logType) {
            return UNKNOWN_LOG;
        }
        return Arrays.stream(values()).filter(production -> production.getTypeValue().equals(logType)).findFirst().orElse(UNKNOWN_LOG);
    }

    public String getTypeValue() {
        return typeValue;
    }

    public String getDesc() {
        return desc;
    }
}
