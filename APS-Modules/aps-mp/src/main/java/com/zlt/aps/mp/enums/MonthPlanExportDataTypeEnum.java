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
    LH_MACHINES("3","硫化机台数"),
    SUBTOTAL("4","小计"),
    TOTAL("5","总计");
    final String code;
    final String desc;
}
