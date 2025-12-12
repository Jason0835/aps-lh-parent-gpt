package com.zlt.aps.factory.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * 工厂日硫化量计算模式枚举定义类
 *
 * @author ZLT
 * 20251210
 */
@Getter
public enum DayVulcanizationModeEnum {
    /**
     * M MES硫化量
     */
    MES_CAPACITY("M", "MES硫化量"),
    /**
     * S 标准硫化量
     */
    STANDARD_CAPACITY("S", "标准硫化量"),
    /**
     * A APS自己计算的硫化量
     */
    APS_CAPACITY("A", "APS自己计算的硫化量");

    private String modeCode;

    private String desc;

    DayVulcanizationModeEnum(String modeCode, String desc) {
        this.modeCode = modeCode;
        this.desc = desc;
    }

    /**
     * 得到计算日硫化量模式枚举实例
     * 如果没有匹配则返回标准模式
     *
     * @param modeCode 模式编码
     * @return
     */
    public static DayVulcanizationModeEnum getInstance(String modeCode) {
        if (StringUtils.isBlank(modeCode)) {
            return STANDARD_CAPACITY;
        }
        return Arrays.stream(values()).filter(mode -> mode.getModeCode().equalsIgnoreCase(modeCode)).findFirst().orElse(STANDARD_CAPACITY);
    }
}
