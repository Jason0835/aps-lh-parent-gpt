package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ProductionDayChangeMould.java
 * 描    述：分厂月生产计划排产过程-硫化-硫化-换模次数对象 t_mp_proc_day_change_mould
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-19
 */

@Data
@TableName(value = "T_MP_PROC_DAY_CHANGE_MOULD")
@ApiModel(value = "分厂月生产计划排产过程-换模信息对象", description = "分厂月生产计划排产过程-换模信息对象")
public class ProductionDayChangeMould extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 生产分厂编号
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.factoryCode")
    @ApiModelProperty(value = "生产分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 销售生产需求计划版本
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.monthPlanVersion")
    @ApiModelProperty(value = "销售生产需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 分厂版本
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.productionVersion")
    @ApiModelProperty(value = "分厂版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 月计划ID
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.monthPlanId")
    @ApiModelProperty(value = "月计划ID", name = "monthPlanId")
    @TableField(value = "MONTH_PLAN_ID")
    private Long monthPlanId;

    /**
     * 生产物料编号
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.productCode")
    @ApiModelProperty(value = "生产物料编号", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 生产产品描述
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.productDesc")
    @ApiModelProperty(value = "生产产品描述", name = "productDesc")
    @TableField(value = "PRODUCT_DESC")
    private String productDesc;

    /**
     * 规格
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /**
     * 花纹
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.pattern")
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /**
     * 层级
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.hierarchy")
    @ApiModelProperty(value = "层级", name = "hierarchy")
    @TableField(value = "HIERARCHY")
    private String hierarchy;

    /**
     * 等级码
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.levelCode")
    @ApiModelProperty(value = "等级码", name = "levelCode")
    @TableField(value = "LEVEL_CODE")
    private String levelCode;

    /**
     * 等级名称
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.levelName")
    @ApiModelProperty(value = "等级名称", name = "levelName")
    @TableField(value = "LEVEL_NAME")
    private String levelName;

    /**
     * 品名编码
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.productTypeCode")
    @ApiModelProperty(value = "品名编码", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 品名
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.productTypeName")
    @ApiModelProperty(value = "品名", name = "productTypeName")
    @TableField(value = "PRODUCT_TYPE_NAME")
    private String productTypeName;

    /**
     * 是否EXCEL导入（0：默认不是，1：是）
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.isImport", readConverterExp = "0=：默认不是，1：是")
    @ApiModelProperty(value = "是否EXCEL导入", name = "isImport")
    @TableField(value = "IS_IMPORT")
    private Integer isImport;

    /**
     * 硫化排产顺序
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.sequence")
    @ApiModelProperty(value = "硫化排产顺序", name = "sequence")
    @TableField(value = "SEQUENCE")
    private Integer sequence;

    /**
     * 成型机ID
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.moldingMachineId")
    @ApiModelProperty(value = "成型机ID", name = "moldingMachineId")
    @TableField(value = "MOLDING_MACHINE_ID")
    private Long moldingMachineId;

    /**
     * 成型机编号
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.moldingMachineCode")
    @ApiModelProperty(value = "成型机编号", name = "moldingMachineCode")
    @TableField(value = "MOLDING_MACHINE_CODE")
    private String moldingMachineCode;

    /**
     * 硫化机ID
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.vulcanizinnMachineId")
    @ApiModelProperty(value = "硫化机ID", name = "vulcanizinnMachineId")
    @TableField(value = "VULCANIZINN_MACHINE_ID")
    private Long vulcanizinnMachineId;

    /**
     * 硫化机编号
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.vulcanizingMachineCode")
    @ApiModelProperty(value = "硫化机编号", name = "vulcanizingMachineCode")
    @TableField(value = "VULCANIZING_MACHINE_CODE")
    private String vulcanizingMachineCode;

    /**
     * 硫化机模号:L-左边,R-右边
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.mouldNo")
    @ApiModelProperty(value = "硫化机模号:L-左边,R-右边", name = "mouldNo")
    @TableField(value = "MOULD_NO")
    private String mouldNo;

    /**
     * 模具号
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.mouldCode")
    @ApiModelProperty(value = "模具号", name = "mouldCode")
    @TableField(value = "MOULD_CODE")
    private String mouldCode;

    /**
     * 源成型机ID
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.srcMoldingMachineId")
    @ApiModelProperty(value = "源成型机ID", name = "srcMoldingMachineId")
    @TableField(value = "SRC_MOLDING_MACHINE_ID")
    private Long srcMoldingMachineId;

    /**
     * 源成型机编号
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.srcMoldingMachineCode")
    @ApiModelProperty(value = "源成型机编号", name = "srcMoldingMachineCode")
    @TableField(value = "SRC_MOLDING_MACHINE_CODE")
    private String srcMoldingMachineCode;

    /**
     * 源硫化机ID
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.srcVulcanizinnMachineId")
    @ApiModelProperty(value = "源硫化机ID", name = "srcVulcanizinnMachineId")
    @TableField(value = "SRC_VULCANIZINN_MACHINE_ID")
    private Long srcVulcanizinnMachineId;

    /**
     * 源硫化机编号
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.srcVulcanizingMachineCode")
    @ApiModelProperty(value = "源硫化机编号", name = "srcVulcanizingMachineCode")
    @TableField(value = "SRC_VULCANIZING_MACHINE_CODE")
    private String srcVulcanizingMachineCode;

    /**
     * 源硫化机模号:L-左边,R-右边
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.srcMouldNo")
    @ApiModelProperty(value = "源硫化机模号:L-左边,R-右边", name = "srcMouldNo")
    @TableField(value = "SRC_MOULD_NO")
    private String srcMouldNo;

    /**
     * 数据类型：0-日换模次数，1-日使用模具数
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.dataType")
    @ApiModelProperty(value = "数据类型：0-日换模次数，1-日使用模具数", name = "dataType")
    @TableField(value = "DATA_TYPE")
    private Integer dataType;

    /**
     * DAY_1
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day1")
    @ApiModelProperty(value = "DAY_1", name = "day1")
    @TableField(value = "DAY_1")
    private Integer day1;

    /**
     * DAY_2
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day2")
    @ApiModelProperty(value = "DAY_2", name = "day2")
    @TableField(value = "DAY_2")
    private Integer day2;

    /**
     * DAY_3
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day3")
    @ApiModelProperty(value = "DAY_3", name = "day3")
    @TableField(value = "DAY_3")
    private Integer day3;

    /**
     * DAY_4
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day4")
    @ApiModelProperty(value = "DAY_4", name = "day4")
    @TableField(value = "DAY_4")
    private Integer day4;

    /**
     * DAY_5
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day5")
    @ApiModelProperty(value = "DAY_5", name = "day5")
    @TableField(value = "DAY_5")
    private Integer day5;

    /**
     * DAY_6
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day6")
    @ApiModelProperty(value = "DAY_6", name = "day6")
    @TableField(value = "DAY_6")
    private Integer day6;

    /**
     * DAY_7
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day7")
    @ApiModelProperty(value = "DAY_7", name = "day7")
    @TableField(value = "DAY_7")
    private Integer day7;

    /**
     * DAY_8
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day8")
    @ApiModelProperty(value = "DAY_8", name = "day8")
    @TableField(value = "DAY_8")
    private Integer day8;

    /**
     * DAY_9
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day9")
    @ApiModelProperty(value = "DAY_9", name = "day9")
    @TableField(value = "DAY_9")
    private Integer day9;

    /**
     * DAY_10
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day10")
    @ApiModelProperty(value = "DAY_10", name = "day10")
    @TableField(value = "DAY_10")
    private Integer day10;

    /**
     * DAY_11
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day11")
    @ApiModelProperty(value = "DAY_11", name = "day11")
    @TableField(value = "DAY_11")
    private Integer day11;

    /**
     * DAY_12
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day12")
    @ApiModelProperty(value = "DAY_12", name = "day12")
    @TableField(value = "DAY_12")
    private Integer day12;

    /**
     * DAY_13
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day13")
    @ApiModelProperty(value = "DAY_13", name = "day13")
    @TableField(value = "DAY_13")
    private Integer day13;

    /**
     * DAY_14
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day14")
    @ApiModelProperty(value = "DAY_14", name = "day14")
    @TableField(value = "DAY_14")
    private Integer day14;

    /**
     * DAY_15
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day15")
    @ApiModelProperty(value = "DAY_15", name = "day15")
    @TableField(value = "DAY_15")
    private Integer day15;

    /**
     * DAY_16
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day16")
    @ApiModelProperty(value = "DAY_16", name = "day16")
    @TableField(value = "DAY_16")
    private Integer day16;

    /**
     * DAY_17
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day17")
    @ApiModelProperty(value = "DAY_17", name = "day17")
    @TableField(value = "DAY_17")
    private Integer day17;

    /**
     * DAY_18
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day18")
    @ApiModelProperty(value = "DAY_18", name = "day18")
    @TableField(value = "DAY_18")
    private Integer day18;

    /**
     * DAY_19
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day19")
    @ApiModelProperty(value = "DAY_19", name = "day19")
    @TableField(value = "DAY_19")
    private Integer day19;

    /**
     * DAY_20
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day20")
    @ApiModelProperty(value = "DAY_20", name = "day20")
    @TableField(value = "DAY_20")
    private Integer day20;

    /**
     * DAY_21
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day21")
    @ApiModelProperty(value = "DAY_21", name = "day21")
    @TableField(value = "DAY_21")
    private Integer day21;

    /**
     * DAY_22
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day22")
    @ApiModelProperty(value = "DAY_22", name = "day22")
    @TableField(value = "DAY_22")
    private Integer day22;

    /**
     * DAY_23
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day23")
    @ApiModelProperty(value = "DAY_23", name = "day23")
    @TableField(value = "DAY_23")
    private Integer day23;

    /**
     * DAY_24
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day24")
    @ApiModelProperty(value = "DAY_24", name = "day24")
    @TableField(value = "DAY_24")
    private Integer day24;

    /**
     * DAY_25
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day25")
    @ApiModelProperty(value = "DAY_25", name = "day25")
    @TableField(value = "DAY_25")
    private Integer day25;

    /**
     * DAY_26
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day26")
    @ApiModelProperty(value = "DAY_26", name = "day26")
    @TableField(value = "DAY_26")
    private Integer day26;

    /**
     * DAY_27
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day27")
    @ApiModelProperty(value = "DAY_27", name = "day27")
    @TableField(value = "DAY_27")
    private Integer day27;

    /**
     * DAY_28
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day28")
    @ApiModelProperty(value = "DAY_28", name = "day28")
    @TableField(value = "DAY_28")
    private Integer day28;

    /**
     * DAY_29
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day29")
    @ApiModelProperty(value = "DAY_29", name = "day29")
    @TableField(value = "DAY_29")
    private Integer day29;

    /**
     * DAY_30
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day30")
    @ApiModelProperty(value = "DAY_30", name = "day30")
    @TableField(value = "DAY_30")
    private Integer day30;

    /**
     * DAY_31
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.day31")
    @ApiModelProperty(value = "DAY_31", name = "day31")
    @TableField(value = "DAY_31")
    private Integer day31;

    /**
     * 库位类别 1 内销 2 外销 3 OE
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.locationType")
    @ApiModelProperty(value = "库位类别 1 内销 2 外销 3 OE", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private Integer locationType;

    /**
     * 渠道
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.channel")
    @ApiModelProperty(value = "渠道", name = "channel")
    @TableField(value = "CHANNEL")
    private String channel;

    /**
     * 品牌
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.brand")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /**
     * 寸口
     */
    @Excel(name = "ui.data.column.ProductionDayChangeMould.proSize")
    @ApiModelProperty(value = "寸口", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private BigDecimal proSize;

}