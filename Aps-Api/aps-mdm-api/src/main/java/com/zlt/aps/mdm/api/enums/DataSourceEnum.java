package com.zlt.aps.mdm.api.enums;

import lombok.Getter;

/**
 * 数据来源类型枚举
 *
 * @author APS Team
 */
@Getter
public enum DataSourceEnum implements PublicEnum {

    /**
     * 手工录入
     */
    MANUAL("手工录入", "0"),

    /**
     * 系统生成
     */
    SYSTEM("系统生成", "1");

    private String name;

    private String code;

    DataSourceEnum(String name, String code) {
        this.name = name;
        this.code = code;
    }

    /**
     * 根据code获取枚举
     *
     * @param code 编码
     * @return 枚举对象
     */
    public static DataSourceEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (DataSourceEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 根据code获取名称
     *
     * @param code 编码
     * @return 名称
     */
    public static String getNameByCode(String code) {
        DataSourceEnum type = getByCode(code);
        return type != null ? type.getName() : null;
    }
}
