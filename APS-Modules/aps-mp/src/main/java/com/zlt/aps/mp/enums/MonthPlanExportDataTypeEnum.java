package com.zlt.aps.mp.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 月计划导出数据类型枚举
 *
 * @author hak
 */
@Getter
@AllArgsConstructor
public enum MonthPlanExportDataTypeEnum {
    /**
     * 1 明细记录
     */
    RECORD("1", "ui.data.column.factoryMonthPlanMouldDayResult.export.dataType.record"),
    /**
     * 2 胎胚种类数
     */
    EMBRYO_TYPE_COUNT("2", "ui.data.column.factoryMonthPlanMouldDayResult.export.dataType.embryoTypeCount"),
    /**
     * 3 硫化机台数
     */
    LH_MACHINES("3", "ui.data.column.factoryMonthPlanMouldDayResult.export.dataType.lhMachines"),
    /**
     * 4 小计
     */
    SUBTOTAL("4", "ui.data.column.factoryMonthPlanMouldDayResult.export.dataType.subtotal"),
    /**
     * 5 合计
     */
    TOTAL("5", "ui.data.column.factoryMonthPlanMouldDayResult.export.dataType.total"),
    /**
     * 6 换模次数
     */
    CHANGE_MOULDS("6", "ui.data.column.factoryMonthPlanMouldDayResult.export.dataType.changeMoulds");
    final String code;
    final String name;
}
