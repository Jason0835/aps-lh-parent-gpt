package com.zlt.aps.mp.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 月计划导出数据类型枚举
 * @author hak
 *
 */
@Getter
@AllArgsConstructor
public enum MonthPlanExportDataTypeEnum {
    RECORD("1","明细记录"),
    EMBRYO_TYPE_COUNT("2","胎胚种类数"),
    SUBTOTAL("3","小计"),
    TOTAL("4","总计");
    final String code;
    final String desc;
}
