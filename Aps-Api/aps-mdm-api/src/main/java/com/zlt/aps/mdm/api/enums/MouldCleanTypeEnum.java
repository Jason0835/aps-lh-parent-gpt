package com.zlt.aps.mdm.api.enums;

import lombok.Getter;

/**
 * 模具清洗类型枚举
 *
 * @author APS Team
 */
@Getter
public enum MouldCleanTypeEnum implements PublicEnum {

    /**
     * 干冰清洗
     */
    DRY_ICE("干冰清洗", "01"),

    /**
     * 喷砂清洗
     */
    SAND_BLAST("喷砂清洗", "02");

    private String name;

    private String code;

    MouldCleanTypeEnum(String name, String code) {
        this.name = name;
        this.code = code;
    }

    /**
     * 根据code获取枚举
     *
     * @param code 编码
     * @return 枚举对象
     */
    public static MouldCleanTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (MouldCleanTypeEnum type : values()) {
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
        MouldCleanTypeEnum type = getByCode(code);
        return type != null ? type.getName() : null;
    }
}
