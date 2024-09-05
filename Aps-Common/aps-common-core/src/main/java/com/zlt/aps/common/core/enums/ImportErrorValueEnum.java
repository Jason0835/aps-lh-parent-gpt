package com.zlt.aps.common.core.enums;

import com.ruoyi.common.i18n.utils.I18nUtil;

import java.math.BigDecimal;

/**
 * 导入数值错误枚举类
 * @author: Chen
 * @since: 2021/8/9 15:41
 */
public enum ImportErrorValueEnum {
    DOUBLE_VALUE(I18nUtil.getMessage("import.errorValueEnum.message.doubleValue"),Double.MAX_VALUE),
    INTEGER_VALUE(I18nUtil.getMessage("import.errorValueEnum.message.integerValue"),Integer.MAX_VALUE),
    LONG_VALUE(I18nUtil.getMessage("import.errorValueEnum.message.longValue"),Long.MAX_VALUE),
    FLOAT_VALUE(I18nUtil.getMessage("import.errorValueEnum.message.floatValue"),Float.MAX_VALUE),
    BIGDECIMAL_VALUE(I18nUtil.getMessage("import.errorValueEnum.message.bigDecimalValue"), BigDecimal.valueOf(Double.MIN_VALUE)),
    ;

    private final String errorMessage;
    private final Number number;

    ImportErrorValueEnum(String errorMessage, Number number) {
        this.errorMessage = errorMessage;
        this.number = number;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Number getNumber() {
        return number;
    }
}
