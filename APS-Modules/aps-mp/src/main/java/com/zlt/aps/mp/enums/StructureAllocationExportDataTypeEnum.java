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
    TOTAL("2","ui.data.column.mpStructureAllocation.total"), // 排产合计
    MAX_PRODUCT_QTY("3","ui.data.column.mpStructureAllocation.maxProductQty"), // 最大产能
    ENABLE_COUNT("4","ui.data.column.mpStructureAllocation.enableCount"), // 可用台数
    TOTAL_CHANGE_COUNT("5","ui.data.column.mpStructureAllocation.totalChangeCount"), // 合计切换次数
    TOTAL_PRODUCT_QTY("6","ui.data.column.mpStructureAllocation.totalProductQty"); // 合计
    final String code;
    final String name;
}
