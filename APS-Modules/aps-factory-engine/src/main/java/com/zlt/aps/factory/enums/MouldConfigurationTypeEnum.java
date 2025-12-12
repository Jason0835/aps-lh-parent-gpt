package com.zlt.aps.factory.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * 模具关系类型定义
 * 01 SKU与模具关系配置 02 新模具到货计划
 *
 * @author ZLT
 * 20251210
 */
@Getter
public enum MouldConfigurationTypeEnum {
    /**
     * 01 正常模具配置
     */
    NORMAL_CONFIGURATION("01", "正常模具配置"),
    /**
     * 02 新模具到货计划
     */
    NEW_MOULD_Delivery("02", "新模具到货");

    private String type;

    private String desc;

    MouldConfigurationTypeEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    /**
     * 获取模具关系类型
     *
     * @param typeCode 类型编码
     * @return
     */
    public static MouldConfigurationTypeEnum getInstance(String typeCode) {
        if (StringUtils.isBlank(typeCode)) {
            return NORMAL_CONFIGURATION;
        }
        return Arrays.stream(values()).filter(type -> type.getType().equalsIgnoreCase(typeCode)).findFirst().orElse(NORMAL_CONFIGURATION);
    }
}
