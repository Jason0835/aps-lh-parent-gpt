package com.zlt.aps.monthplan.api.domain.entity;

import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmStructureLhRatio.java
 * 描    述：成型结构硫化配比对象 t_mdm_structure_lh_ratio
 *@author zlt
 *@date 2025-12-08
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "成型结构硫化配比对象", description = "成型结构硫化配比对象 ")
@Data
@TableName(value = "T_MDM_STRUCTURE_LH_RATIO")
public class MdmStructureLhRatio extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 工厂编号 */
    @Excel(name = "ui.data.column.mdmStructureLhRatio.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 成型机类型 */
    @ImportExcelValidated(required = true, maxLength = 20)
    @Excel(name = "ui.data.column.mdmStructureLhRatio.cxMachineBrandCode", dictType = "biz_machine_brand")
    @ApiModelProperty(value = "成型机类型", name = "cxMachineBrandCode")
    @TableField(value = "CX_MACHINE_BRAND_CODE")
    private String cxMachineBrandCode;

    /** 结构 */
    @ImportExcelValidated(required = true, maxLength = 64)
    @Excel(name = "ui.data.column.mdmStructureLhRatio.structureName")
    @ApiModelProperty(value = "结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /** 最大硫化机台数 */
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 99)
    @Excel(name = "ui.data.column.mdmStructureLhRatio.lhMachineMaxQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "最大硫化机台数", name = "lhMachineMaxQty")
    @TableField(value = "LH_MACHINE_MAX_QTY")
    private Integer lhMachineMaxQty;

    /** 最大胎胚数 */
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 99)
    @Excel(name = "ui.data.column.mdmStructureLhRatio.maxEmbryoQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "最大胎胚数", name = "maxEmbryoQty")
    @TableField(value = "MAX_EMBRYO_QTY")
    private Integer maxEmbryoQty;

    @Excel(name = "ui.data.column.mdmStructureLhRatio.remark")
    @ApiModelProperty("备注")
    @TableField(value = "REMARK")
    private String remark;


}