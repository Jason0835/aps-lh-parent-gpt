package com.zlt.aps.mp.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 结构转产表导出数据类型枚举
 * @author hak
 *
 */
@Getter
@AllArgsConstructor
public enum StructureAllocationExportDataTypeEnum {
    RECORD("1","ui.data.column.factoryMonthPlanMouldDayResult.export.dataType.record"), // 明细记录
    TOTAL("2","ui.data.column.factoryMonthPlanMouldDayResult.export.dataType.embryoTypeCount"), // 排产合计
    MAX_PRODUCT_QTY("3","ui.data.column.factoryMonthPlanMouldDayResult.export.dataType.lhMachines"), // 最大产能
    ENABLE_COUNT("4","ui.data.column.factoryMonthPlanMouldDayResult.export.dataType.subtotal"), // 可用台数
    TOTAL_CHANGE_COUNT("4","ui.data.column.factoryMonthPlanMouldDayResult.export.dataType.subtotal"); // 合计切换次数
    final String code;
    final String name;
}
