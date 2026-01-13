package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmCxMachineFixed.java
 * 描    述：成型固定机台对象 t_mdm_cx_machine_fixed
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-12
 */
@ApiModel(value = "成型固定机台对象", description = "成型固定机台对象")
@Data
@TableName(value = "T_MDM_CX_MACHINE_FIXED")
public class MdmCxMachineFixed extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmCxMachineFixed.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 成型机编码
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmCxMachineFixed.cxMachineCode")
    @ApiModelProperty(value = "成型机编码", name = "cxMachineCode")
    @TableField(value = "CX_MACHINE_CODE")
    private String cxMachineCode;

    /**
     * 固定结构1 多个以,分隔拼接
     */
    @ImportExcelValidated(maxLength = 500)
    @Excel(name = "ui.data.column.mdmCxMachineFixed.fixedStructure1")
    @ApiModelProperty(value = "固定结构1 多个以,分隔拼接", name = "fixedStructure1")
    @TableField(value = "FIXED_STRUCTURE1", updateStrategy = FieldStrategy.IGNORED)
    private String fixedStructure1;

    /**
     * 固定结构2 多个以,分隔拼接
     */
    @ImportExcelValidated(maxLength = 500)
    @Excel(name = "ui.data.column.mdmCxMachineFixed.fixedStructure2")
    @ApiModelProperty(value = "固定结构2 多个以,分隔拼接", name = "fixedStructure2")
    @TableField(value = "FIXED_STRUCTURE2", updateStrategy = FieldStrategy.IGNORED)
    private String fixedStructure2;

    /**
     * 固定结构3 多个以,分隔拼接
     */
    @ImportExcelValidated(maxLength = 500)
    @Excel(name = "ui.data.column.mdmCxMachineFixed.fixedStructure3")
    @ApiModelProperty(value = "固定结构3 多个以,分隔拼接", name = "fixedStructure3")
    @TableField(value = "FIXED_STRUCTURE3", updateStrategy = FieldStrategy.IGNORED)
    private String fixedStructure3;

    /**
     * 固定SKU  多个以,分隔拼接
     */
    @ImportExcelValidated(maxLength = 500)
    @Excel(name = "ui.data.column.mdmCxMachineFixed.fixedMaterialCode")
    @ApiModelProperty(value = "固定SKU  多个以,分隔拼接", name = "fixedMaterialCode")
    @TableField(value = "FIXED_MATERIAL_CODE", updateStrategy = FieldStrategy.IGNORED)
    private String fixedMaterialCode;

    /**
     * 不可作业结构  多个以,分隔拼接
     */
    @ImportExcelValidated(maxLength = 500)
    @Excel(name = "ui.data.column.mdmCxMachineFixed.disableStructure")
    @ApiModelProperty(value = "不可作业结构  多个以,分隔拼接", name = "disableStructure")
    @TableField(value = "DISABLE_STRUCTURE", updateStrategy = FieldStrategy.IGNORED)
    private String disableStructure;

    /**
     * 不可作业SKU  多个以,分隔拼接
     */
    @ImportExcelValidated(maxLength = 500)
    @Excel(name = "ui.data.column.mdmCxMachineFixed.disableMaterialCode")
    @ApiModelProperty(value = "不可作业SKU  多个以,分隔拼接", name = "disableMaterialCode")
    @TableField(value = "DISABLE_MATERIAL_CODE", updateStrategy = FieldStrategy.IGNORED)
    private String disableMaterialCode;

}
