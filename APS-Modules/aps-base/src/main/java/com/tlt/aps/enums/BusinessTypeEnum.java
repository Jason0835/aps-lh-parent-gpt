package com.tlt.aps.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * 业务类型枚举
 *
 * @author hsc
 * @since 2025/2/26
 */
@Getter
public enum BusinessTypeEnum {

    /**
     * 01 库存冲销
     */
    MONTHPLAN_PRODUCTION("01", "月度计划排产");

    private String code;
    private String remark;

    BusinessTypeEnum(String code, String remark) {
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
}
