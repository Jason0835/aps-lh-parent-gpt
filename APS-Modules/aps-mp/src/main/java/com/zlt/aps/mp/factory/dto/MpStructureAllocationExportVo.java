package com.zlt.aps.mp.factory.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 结构转产表导出明细
 *
 * @author hak
 *
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MpStructureAllocationExportVo extends MpStructureAllocation {
    private static final long serialVersionUID = 1L;

    /**
     * 切换次序，决定导出背景颜色深度，数值约大颜色越深
     */
    private Integer changeRank;

    /**
     * 导出数据类型，1：明细记录，2：胎胚种类数，3：小计、4：总计
     */
    private String dataType;

    /**
     * 结构类型 01 周期结构 02 常规结构，数据字典：structure_type
     */
    @Excel(name = "ui.data.column.mpStructureAllocation.structureType", dictType = "structure_type")
    private String structureType;

    /**
     * 产品品类 数据字典：biz_product_type  全钢 PCR 半钢
     */
    private String productTypeCode;

    /**
     * 设备类型，数据字典：cx_machine_type_code
     */
    @Excel(name = "ui.data.column.mpStructureAllocation.cxMachineTypeCode", dictType = "cx_machine_type_code")
    private String cxMachineTypeCode;

    /**
     * 净需求(不含暂缓)
     */
    private Integer unPostponeNetQty;

    /**
     * 实际排产
     */
    @Excel(name = "ui.data.column.mpStructureAllocation.totalQty")
    private Integer totalQty;

    /**
     * 未排产量
     */
    @Excel(name = "ui.data.column.mpStructureAllocation.differenceQty")
    private Integer differenceQty;

    /**
     * 英寸
     */
    private String proSize;

    /**
     * DAY_1
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day1", cellType = Excel.ColumnType.NUMERIC)
    private Integer day1;

    /**
     * DAY_2
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day2", cellType = Excel.ColumnType.NUMERIC)
    private Integer day2;

    /**
     * DAY_3
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day3", cellType = Excel.ColumnType.NUMERIC)
    private Integer day3;

    /**
     * DAY_4
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day4", cellType = Excel.ColumnType.NUMERIC)
    private Integer day4;

    /**
     * DAY_5
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day5", cellType = Excel.ColumnType.NUMERIC)
    private Integer day5;

    /**
     * DAY_6
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day6", cellType = Excel.ColumnType.NUMERIC)
    private Integer day6;

    /**
     * DAY_7
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day7", cellType = Excel.ColumnType.NUMERIC)
    private Integer day7;

    /**
     * DAY_8
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day8", cellType = Excel.ColumnType.NUMERIC)
    private Integer day8;

    /**
     * DAY_9
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day9", cellType = Excel.ColumnType.NUMERIC)
    private Integer day9;

    /**
     * DAY_10
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day10", cellType = Excel.ColumnType.NUMERIC)
    private Integer day10;

    /**
     * DAY_11
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day11", cellType = Excel.ColumnType.NUMERIC)
    private Integer day11;

    /**
     * DAY_12
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day12", cellType = Excel.ColumnType.NUMERIC)
    private Integer day12;

    /**
     * DAY_13
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day13", cellType = Excel.ColumnType.NUMERIC)
    private Integer day13;

    /**
     * DAY_14
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day14", cellType = Excel.ColumnType.NUMERIC)
    private Integer day14;

    /**
     * DAY_15
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day15", cellType = Excel.ColumnType.NUMERIC)
    private Integer day15;

    /**
     * DAY_16
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day16", cellType = Excel.ColumnType.NUMERIC)
    private Integer day16;

    /**
     * DAY_17
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day17", cellType = Excel.ColumnType.NUMERIC)
    private Integer day17;

    /**
     * DAY_18
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day18", cellType = Excel.ColumnType.NUMERIC)
    private Integer day18;

    /**
     * DAY_19
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day19", cellType = Excel.ColumnType.NUMERIC)
    private Integer day19;

    /**
     * DAY_20
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day20", cellType = Excel.ColumnType.NUMERIC)
    private Integer day20;

    /**
     * DAY_21
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day21", cellType = Excel.ColumnType.NUMERIC)
    private Integer day21;

    /**
     * DAY_22
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day22", cellType = Excel.ColumnType.NUMERIC)
    private Integer day22;

    /**
     * DAY_23
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day23", cellType = Excel.ColumnType.NUMERIC)
    private Integer day23;

    /**
     * DAY_24
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day24", cellType = Excel.ColumnType.NUMERIC)
    private Integer day24;

    /**
     * DAY_25
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day25", cellType = Excel.ColumnType.NUMERIC)
    private Integer day25;

    /**
     * DAY_26
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day26", cellType = Excel.ColumnType.NUMERIC)
    private Integer day26;

    /**
     * DAY_27
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day27", cellType = Excel.ColumnType.NUMERIC)
    private Integer day27;

    /**
     * DAY_28
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day28", cellType = Excel.ColumnType.NUMERIC)
    private Integer day28;

    /**
     * DAY_29
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day29", cellType = Excel.ColumnType.NUMERIC)
    private Integer day29;

    /**
     * DAY_30
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day30", cellType = Excel.ColumnType.NUMERIC)
    private Integer day30;

    /**
     * DAY_31
     */
    @Excel(name = "ui.data.column.facMonthPlanProdResult.day31", cellType = Excel.ColumnType.NUMERIC)
    private Integer day31;
}
