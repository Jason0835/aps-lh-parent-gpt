package com.zlt.aps.common.engine.enums;

/**
 * 机台范围条件枚举
 * <p>
 * 用于 T_DJ_DEPTH_CONFIG（垫胶备库班数与供成型机数配置）表中 MACHINE_RANGE 字段，
 * 定义成型机台数的匹配条件。
 * </p>
 */
public enum MachineRangeEnum {

    /** 小于 */
    LT("LT", "小于"),
    /** 小于等于 */
    LE("LE", "小于等于"),
    /** 等于 */
    EQ("EQ", "等于"),
    /** 大于等于 */
    GE("GE", "大于等于"),
    /** 大于 */
    GT("GT", "大于");

    private final String code;
    private final String description;

    MachineRangeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取枚举编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取枚举描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据编码获取枚举
     *
     * @param code 编码（LT/LE/EQ/GE/GT）
     * @return 匹配的枚举，未匹配返回 null
     */
    public static MachineRangeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (MachineRangeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 判断指定成型机台数是否满足当前范围条件
     *
     * @param cxMachineCount 成型机台数
     * @param configQty      配置的参考台数值
     * @return true 表示满足条件
     */
    public boolean matches(int cxMachineCount, int configQty) {
        switch (this) {
            case LT:
                return cxMachineCount < configQty;
            case LE:
                return cxMachineCount <= configQty;
            case EQ:
                return cxMachineCount == configQty;
            case GE:
                return cxMachineCount >= configQty;
            case GT:
                return cxMachineCount > configQty;
            default:
                return false;
        }
    }
}
