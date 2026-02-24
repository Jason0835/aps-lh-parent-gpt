package com.zlt.aps.mdm.api.domain.entity;

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
 * 文件名称：MdmMoldingMachineClsB.java
 * 描    述：基础数据-成型机类型子对象 t_mdm_molding_machine_cls_b
 *@author zlt
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "基础数据-成型机类型子对象", description = "基础数据-成型机类型子对象")
@Data
@TableName(value = "T_MDM_MOLDING_MACHINE_CLS_B")
public class MdmMoldingMachineClsB extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 成型机类型主键 */
    @ApiModelProperty(value = "成型机类型主键，主表ID", name = "moldingMachineClassId")
    @TableField(value = "MOLDING_MACHINE_CLASS_ID")
    private Long moldingMachineClassId;

    @Excel(name = "ui.data.column.mdmMoldingMachineClsB.moldingMachineClassCode")
    @ApiModelProperty(value = "成型机类别编码", name = "moldingMachineClassCode")
    @TableField(exist = false)
    private String moldingMachineClassCode;

    /** 寸口 */
    @Excel(name = "ui.data.column.mdmMoldingMachineClsB.proSize")
    @ApiModelProperty(value = "寸口", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private BigDecimal proSize;

    /** 定额产能 */
    @Excel(name = "ui.data.column.mdmMoldingMachineClsB.productionQuotaQty")
    @ApiModelProperty(value = "定额产能", name = "productionQuotaQty")
    @TableField(value = "PRODUCTION_QUOTA_QTY")
    private Integer productionQuotaQty;

    /**
     * 成型与硫化配比
     */
    @Excel(name = "ui.data.column.mdmMoldingMachineClsB.moldingSulfurizationRatio")
    @ApiModelProperty(value = "成型与硫化配比", name = "moldingSulfurizationRatio")
    @TableField(value = "MOLDING_SULFURIZATION_RATIO")
    private BigDecimal moldingSulfurizationRatio;


    /** 成型机班次定额：排程算法使用*/
    @TableField(exist = false)
    private Integer class1MachineQty;
    @TableField(exist = false)
    private Integer class2MachineQty;
    @TableField(exist = false)
    private Integer class3MachineQty;

    /**
     * 成型法，月度计划使用
     */
    @TableField(exist = false)
    private Integer mouldMethod;
}
