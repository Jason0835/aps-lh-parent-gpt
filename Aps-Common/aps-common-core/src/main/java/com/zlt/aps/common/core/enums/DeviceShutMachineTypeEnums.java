package com.zlt.aps.common.core.enums;

import com.ruoyi.common.utils.StringUtils;
import lombok.Getter;

/**
 * device_shut_machine_type字典枚举
 *
 * @author Chen
 * @since 2025/12/26
 */
@Getter
public enum DeviceShutMachineTypeEnums {

    /**
     * 硫化
     */
    LH("01", "硫化"),

    /**
     * 成型
     */
    CX("02", "成型"),

    /**
     * 压出
     */
    YC("03", "压出"),

    /**
     * 裁断
     */
    CD("04", "裁断"),

    /**
     * 压延
     */
    YY("05", "压延"),

    /**
     * 密炼
     */
    ML("06", "密炼"),

    ;

    private final String code;
    private final String name;

    DeviceShutMachineTypeEnums(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据类型获取对应的名称
     *
     * @param code 类型
     * @return 结果
     */
    public static DeviceShutMachineTypeEnums getNameByCode(String code) {
        if (StringUtils.isEmpty(code)) {
            return null;
        }
        for (DeviceShutMachineTypeEnums enums : DeviceShutMachineTypeEnums.values()) {
            if (enums.getCode().equals(code)) {
                return enums;
            }
        }
        return null;
    }
}
