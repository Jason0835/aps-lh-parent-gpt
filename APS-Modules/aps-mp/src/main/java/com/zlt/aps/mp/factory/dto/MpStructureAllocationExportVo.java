package com.zlt.aps.mp.factory.dto;

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
    private String structureType;
    
    /**
     * 产品品类 数据字典：biz_product_type  全钢 PCR 半钢
     */
    private String productTypeCode;

    /**
     * 设备类型，数据字典：cx_machine_type_code
     */
    private String cxMachineTypeCode;

    /**
     * 实际排产
     */
    private Integer totalQty;

    /**
     * 未排产量
     */
    private Integer differenceQty;

    /**
     * DAY_1
     */
    private Integer day1;

    /**
     * DAY_2
     */
    private Integer day2;

    /**
     * DAY_3
     */
    private Integer day3;

    /**
     * DAY_4
     */
    private Integer day4;

    /**
     * DAY_5
     */
    private Integer day5;

    /**
     * DAY_6
     */
    private Integer day6;

    /**
     * DAY_7
     */
    private Integer day7;

    /**
     * DAY_8
     */
    private Integer day8;

    /**
     * DAY_9
     */
    private Integer day9;

    /**
     * DAY_10
     */
    private Integer day10;

    /**
     * DAY_11
     */
    private Integer day11;

    /**
     * DAY_12
     */
    private Integer day12;

    /**
     * DAY_13
     */
    private Integer day13;

    /**
     * DAY_14
     */
    private Integer day14;

    /**
     * DAY_15
     */
    private Integer day15;

    /**
     * DAY_16
     */
    private Integer day16;

    /**
     * DAY_17
     */
    private Integer day17;

    /**
     * DAY_18
     */
    private Integer day18;

    /**
     * DAY_19
     */
    private Integer day19;

    /**
     * DAY_20
     */
    private Integer day20;

    /**
     * DAY_21
     */
    private Integer day21;

    /**
     * DAY_22
     */
    private Integer day22;

    /**
     * DAY_23
     */
    private Integer day23;

    /**
     * DAY_24
     */
    private Integer day24;

    /**
     * DAY_25
     */
    private Integer day25;

    /**
     * DAY_26
     */
    private Integer day26;

    /**
     * DAY_27
     */
    private Integer day27;

    /**
     * DAY_28
     */
    private Integer day28;

    /**
     * DAY_29
     */
    private Integer day29;

    /**
     * DAY_30
     */
    private Integer day30;

    /**
     * DAY_31
     */
    private Integer day31;
}
