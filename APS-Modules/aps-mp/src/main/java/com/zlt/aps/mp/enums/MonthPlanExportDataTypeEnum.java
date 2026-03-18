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
    RECORD("1","ui.data.column.factoryMonthPlanMouldDayResult.export.dataType.record"), // 明细记录
    EMBRYO_TYPE_COUNT("2","ui.data.column.factoryMonthPlanMouldDayResult.export.dataType.embryoTypeCount"), // 胎胚种类数
    LH_MACHINES("3","ui.data.column.factoryMonthPlanMouldDayResult.export.dataType.lhMachines"), // 硫化机台数
    SUBTOTAL("4","ui.data.column.factoryMonthPlanMouldDayResult.export.dataType.subtotal"), // 小计
    TOTAL("5","ui.data.column.factoryMonthPlanMouldDayResult.export.dataType.total"), // 合计
    CHANGE_MOULDS("6","ui.data.column.factoryMonthPlanMouldDayResult.export.dataType.changeMoulds"); // 换模次数
    final String code;
    final String name;
}
