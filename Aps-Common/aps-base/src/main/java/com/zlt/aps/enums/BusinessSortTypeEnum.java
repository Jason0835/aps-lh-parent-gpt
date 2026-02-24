package com.zlt.aps.enums;

import org.apache.commons.lang3.StringUtils;

/**
 * 业务排序枚举定义类
 *
 * @author ZLT
 * 20250217
 */
public enum BusinessSortTypeEnum {
    /**
     * 01 库存冲销
     */
    STOCK_HEDGING("01", "库存冲销"),
    /**
     * 02 生产排产
     */
    PRODUCE_PRODUCTION("02", "生产排产");

    private String code;
    private String remark;

    BusinessSortTypeEnum(String code, String remark) {
        this.code = code;
        this.remark = remark;
    }

    /**
     * 根据业务编码，获取对应的业务排序枚举实例对象
     *
     * @param code
     * @return
     */
    public static BusinessSortTypeEnum getInstance(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        for (BusinessSortTypeEnum businessType : BusinessSortTypeEnum.values()) {
            if (businessType.getCode().equals(code)) {
                return businessType;
            }
        }
        return null;
    }

    /**
     * 编码
     *
     * @return
     */
    public String getCode() {
        return code;
    }

    /**
     * 备注说明
     *
     * @return
     */
    public String getRemark() {
        return remark;
    }
}
