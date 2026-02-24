package com.zlt.aps.mdm.api.domain.entity;

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
 * 文件名称：MdmMoldingMachineCls.java
 * 描    述：基础数据-成型机类型主对象 t_mdm_molding_machine_cls
 *@author zlt
 *@date 2025-02-18
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@ApiModel(value = "基础数据-成型机类型主对象", description = "基础数据-成型机类型主对象")
@Data
@TableName(value = "T_MDM_MOLDING_MACHINE_CLS")
public class MdmMoldingMachineCls extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 分厂编号 */
     @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmMoldingMachineCls.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 类别编码 */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmMoldingMachineCls.moldingMachineClassCode")
    @ApiModelProperty(value = "类别编码", name = "moldingMachineClassCode")
    @TableField(value = "MOLDING_MACHINE_CLASS_CODE")
    private String moldingMachineClassCode;

    /** 类别名称 */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmMoldingMachineCls.moldingMachineClassName")
    @ApiModelProperty(value = "类别名称", name = "moldingMachineClassName")
    @TableField(value = "MOLDING_MACHINE_CLASS_NAME")
    private String moldingMachineClassName;

    /** 成型法:来源于数据字典molding_method */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmMoldingMachineCls.mouldMethod", dictType = "molding_method")
    @ApiModelProperty(value = "成型法:来源于数据字典molding_method", name = "mouldMethod")
    @TableField(value = "MOULD_METHOD")
    private Integer mouldMethod;

    /** 成型硫化比：是指硫化排产时，此类型成型机最多可排产多少台成型机 */
//    @Excel(name = "ui.data.column.mdmMoldingMachineCls.ratio")
    @ApiModelProperty(value = "成型硫化比：是指硫化排产时，此类型成型机最多可排产多少台成型机", name = "ratio")
    @TableField(value = "RATIO")
    private Integer ratio;

    /** 排产算法:1-1次法，2-2次法 */
//    @ImportExcelValidated(required = true)
//    @Excel(name = "ui.data.column.mdmMoldingMachineCls.productionMode", dictType = "scheduling_algorithm")
    @ApiModelProperty(value = "排产算法:1-1次法，2-2次法", name = "productionMode")
    @TableField(value = "PRODUCTION_MODE")
    private Integer productionMode;

    /** 是否封存:0-可用，1-封存 */
    @ImportExcelValidated(required = true)
//    @Excel(name = "ui.data.column.mdmMoldingMachineCls.isClosed", dictType = "is_sealed")
    @ApiModelProperty(value = "是否封存，字典：is_sealed", name = "isClosed")
    @TableField(value = "IS_CLOSED")
    private Integer isClosed;


}