package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmMoldingMachine.java
 * 描    述：基础数据-成型机档案对象 t_mdm_molding_machine
 *@author zlt
 *@date 2025-02-18
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "基础数据-成型机档案对象", description = "基础数据-成型机档案对象 ")
@Data
@TableName(value = "T_MDM_MOLDING_MACHINE")
public class MdmMoldingMachine extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 分厂编号 */
    @Excel(name = "ui.data.column.mdmMoldingMachine.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 成型机编码 */
    @Excel(name = "ui.data.column.mdmMoldingMachine.moldingMachineCode")
    @ApiModelProperty(value = "成型机编码", name = "moldingMachineCode")
    @TableField(value = "MOLDING_MACHINE_CODE")
    private String moldingMachineCode;

    /** 成型机类型ID */
    @ApiModelProperty(value = "成型机类型ID", name = "moldingMachineClassId")
    @TableField(value = "MOLDING_MACHINE_CLASS_ID")
    private Long moldingMachineClassId;

    /** 成型机类型名称 */
    @ImportExcelValidated(required = true, maxLength = 20)
    @Excel(name = "ui.data.column.docMoldingMachineClsB.moldingMachineClassName")
    @ApiModelProperty(value = "成型机类型名称", name = "moldingMachineClsName")
    @TableField(value = "MOLDING_MACHINE_CLS_NAME")
    private String moldingMachineClsName;

    /** 胎别 */
    @Excel(name = "ui.data.column.mdmMoldingMachine.productTypeCode", dictType = "biz_product_name")
    @ApiModelProperty(value = "胎别", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /** 最小寸口 */
    @Excel(name = "ui.data.column.mdmMoldingMachine.minSize")
    @ApiModelProperty(value = "最小寸口", name = "minSize")
    @TableField(value = "MIN_SIZE")
    private BigDecimal minSize;

    /** 最大寸口 */
    @Excel(name = "ui.data.column.mdmMoldingMachine.maxSize")
    @ApiModelProperty(value = "最大寸口", name = "maxSize")
    @TableField(value = "MAX_SIZE")
    private BigDecimal maxSize;

    /** 班次 */
    @Excel(name = "ui.data.column.mdmMoldingMachine.classes", dictType = "CLASS_SHIFT")
    @ApiModelProperty(value = "班次", name = "classes")
    @TableField(value = "CLASSES")
    private Integer classes;

    /** 是否RF专用:0-可用，1-封存 */
    @Excel(name = "ui.data.column.mdmMoldingMachine.isRfSpecialUse", dictType = "is_sealed")
    @ApiModelProperty(value = "是否RF专用:0-可用，1-封存", name = "isRfSpecialUse")
    @TableField(value = "IS_RF_SPECIAL_USE")
    private Integer isRfSpecialUse;

    /** 胎休布类型 */
    @Excel(name = "ui.data.column.mdmMoldingMachine.carcassClothType", dictType = "biz_carcassCloth_type")
    @ApiModelProperty(value = "胎休布类型", name = "carcassClothType")
    @TableField(value = "CARCASS_CLOTH_TYPE")
    private Integer carcassClothType;

    /** 扁平比最大值*/
    @Excel(name = "ui.data.column.mdmMoldingMachine.moldingDrumMax")
    @ApiModelProperty(value = "扁平比最大值", name = "moldingDrumMax")
    @TableField(value = "MOLDING_DRUM_MAX")
    private String moldingDrumMax;

    /** 扁平比最小值*/
    @Excel(name = "ui.data.column.mdmMoldingMachine.moldingDrumMin")
    @ApiModelProperty(value = "扁平比最小值", name = "moldingDrumMin")
    @TableField(value = "MOLDING_DRUM_MIN")
    private String moldingDrumMin;

    /**
     * 断面宽最大值
     */
    @Excel(name = "ui.data.column.mdmMoldingMachine.sectionWidthMax")
    @ApiModelProperty(value = "断面宽最大值", name = "sectionWidthMax")
    @TableField(value = "SECTION_WIDTH_MAX")
    private Double sectionWidthMax;

    /**
     * 断面宽最小值
     */
    @Excel(name = "ui.data.column.mdmMoldingMachine.sectionWidthMin")
    @ApiModelProperty(value = "断面宽最小值", name = "sectionWidthMin")
    @TableField(value = "SECTION_WIDTH_MIN")
    private Double sectionWidthMin;

    /** 是否封存:0-可用，1-封存 */
    @Excel(name = "ui.data.column.mdmMoldingMachine.isClosed", dictType = "is_sealed")
    @ApiModelProperty(value = "是否封存:0-可用，1-封存", name = "isClosed")
    @TableField(value = "IS_CLOSED")
    private Integer isClosed;

    /** 状态：0启用1禁用，字典：STATUS */
    @Excel(name = "ui.data.column.mdmMoldingMachine.machineStatus", dictType = "STATUS")
    @ApiModelProperty(value = "状态：0启用1禁用，字典：STATUS", name = "machineStatus")
    @TableField(value = "MACHINE_STATUS")
    private String machineStatus;


    /**
     * 限制硫化排号
     */
    @ImportExcelValidated(required = true, isCode = true)
//    @Excel(name = "ui.data.column.docMoldingMachine.lineCode")
    @TableField(exist = false)
    private String lineCode;

    /**
     * 月度计划排产可用状态：0启用1禁用，字典：STATUS
     */
    @Excel(name = "ui.data.column.mdmMoldingMachine.monthPlanStatus", dictType = "STATUS")
    @ApiModelProperty(value = "月度计划排产可用状态：0启用1禁用，字典：STATUS", name = "monthPlanStatus")
    @TableField(value = "MONTH_PLAN_STATUS")
    private String monthPlanStatus;

    /**
     * 成型法
     */
    @ApiModelProperty(value = "成型法，字典：molding_method", name = "mouldMethod")
    @TableField(exist = false)
    private Integer mouldMethod;

    /**
     * 寸口类型子表
     */
    @TableField(exist = false)
    private List<MdmMoldingMachineClsB> mdmMoldingMachineClsBs = new ArrayList<>();
}