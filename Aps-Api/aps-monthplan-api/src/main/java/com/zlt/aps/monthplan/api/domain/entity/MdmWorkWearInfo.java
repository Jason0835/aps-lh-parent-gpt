package com.zlt.aps.monthplan.api.domain.entity;

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
 * 文件名称：MdmWorkWearInfo.java
 * 描    述：成型鼓(工装)台账对象 t_mdm_work_wear_info
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
@ApiModel(value = "成型鼓(工装)台账对象", description = "成型鼓(工装)台账对象")
@Data
@TableName(value = "T_MDM_WORK_WEAR_INFO")
public class MdmWorkWearInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号，字典：biz_factory_name，必填
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmWorkWearInfo.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 成型机类型 数据字典 biz_machine_brand 01 软控 02 赛象 03 青岛贝帆
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmWorkWearInfo.cxMachineBrandCode", dictType = "biz_machine_brand")
    @ApiModelProperty(value = "成型机类型 数据字典 biz_machine_brand 01 软控 02 赛象 03 青岛贝帆", name = "cxMachineBrandCode")
    @TableField(value = "CX_MACHINE_BRAND_CODE")
    private String cxMachineBrandCode;

    /**
     * 类型 数据字典 biz_class_type 01 机械 02 宽基
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmWorkWearInfo.cxMachineTypeCode", dictType = "biz_class_type")
    @ApiModelProperty(value = "类型 数据字典 biz_class_type 01 机械 02 宽基", name = "cxMachineTypeCode")
    @TableField(value = "CX_MACHINE_TYPE_CODE")
    private String cxMachineTypeCode;

    /**
     * 工装名称，必填长度64
     */
    @ImportExcelValidated(required = true, maxLength = 64)
    @Excel(name = "ui.data.column.mdmWorkWearInfo.workWearName")
    @ApiModelProperty(value = "工装名称", name = "workWearName")
    @TableField(value = "WORK_WEAR_NAME")
    private String workWearName;

    /**
     * 规格型号，必填长度64
     */
    @ImportExcelValidated(required = true, maxLength = 64)
    @Excel(name = "ui.data.column.mdmWorkWearInfo.specificationModel")
    @ApiModelProperty(value = "规格型号", name = "specificationModel")
    @TableField(value = "SPECIFICATION_MODEL")
    private String specificationModel;

    /**
     * 工装分类，必填 数据字典 biz_work_type 01 成型鼓 02 胎体鼓
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmWorkWearInfo.workWearType", dictType = "biz_work_type")
    @ApiModelProperty(value = "工装分类 数据字典 biz_work_type 01 成型鼓 02 胎体鼓", name = "workWearType")
    @TableField(value = "WORK_WEAR_TYPE")
    private String workWearType;

    /**
     * 工装状态，必填 数据字典 biz_available_status 1 可用 0 禁用
     */
    @Excel(name = "ui.data.column.mdmWorkWearInfo.workWearStatus", dictType = "biz_available_status")
    @ApiModelProperty(value = "工装状态 biz_available_status 1 可用 0 禁用", name = "workWearStatus")
    @TableField(value = "WORK_WEAR_STATUS")
    private String workWearStatus;

    /**
     * 成型鼓周长下限
     */
    @ImportExcelValidated(digits = true, min = 1, max = 999999)
    @Excel(name = "ui.data.column.mdmWorkWearInfo.perimeterMin")
    @ApiModelProperty(value = "成型鼓周长下限", name = "perimeterMin")
    @TableField(value = "PERIMETER_MIN")
    private Integer perimeterMin;

    /**
     * 成型鼓周长上限
     */
    @ImportExcelValidated(digits = true, min = 1, max = 999999)
    @Excel(name = "ui.data.column.mdmWorkWearInfo.perimeterMax")
    @ApiModelProperty(value = "成型鼓周长上限", name = "perimeterMax")
    @TableField(value = "PERIMETER_MAX")
    private Integer perimeterMax;

    /**
     * 数量，必填，整数，最大9999
     */
    @ImportExcelValidated(required = true, digits = true, max = 9999)
    @Excel(name = "ui.data.column.mdmWorkWearInfo.qty")
    @ApiModelProperty(value = "数量", name = "qty")
    @TableField(value = "QTY")
    private Integer qty;

    /**
     * 单位，必填 数据字典 biz_work_unit 01 套
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmWorkWearInfo.unit", dictType = "biz_work_unit")
    @ApiModelProperty(value = "单位 数据字典 biz_work_unit 01 套", name = "unit")
    @TableField(value = "UNIT")
    private String unit;

    /**
     * 使用机型，长度20
     */
    @ImportExcelValidated(maxLength = 20)
    @Excel(name = "ui.data.column.mdmWorkWearInfo.usedType")
    @ApiModelProperty(value = "使用机型", name = "usedType")
    @TableField(value = "USED_TYPE")
    private String usedType;


}
