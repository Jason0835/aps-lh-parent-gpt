package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：VulcanizingMachine.java
 * 描    述：基础数据-硫化机档案对象 t_mdm_vulcanizing_machine
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-20
 */

@Data
@TableName(value = "T_MDM_VULCANIZING_MACHINE")
@ApiModel(value = "基础数据-硫化机档案对象", description = "基础数据-硫化机档案对象 ")
public class VulcanizingMachine extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 分厂编号
     */
    @Excel(name = "ui.data.column.VulcanizingMachine.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂编号，字典：biz_factory_name", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 硫化线ID
     */
//    @Excel(name = "ui.data.column.VulcanizingMachine.lineId")
    @ApiModelProperty(value = "硫化线ID", name = "lineId")
    @TableField(value = "LINE_ID")
    private Long lineId;

    /**
     * 硫化线编号
     */
    @Excel(name = "ui.data.column.VulcanizingMachine.lineCode")
    @ApiModelProperty(value = "硫化线编号", name = "lineCode")
    @TableField(value = "LINE_CODE")
    private String lineCode;

    /**
     * 硫化机编码
     */
    @Excel(name = "ui.data.column.VulcanizingMachine.vulcanizingMachineCode")
    @ApiModelProperty(value = "硫化机编码", name = "vulcanizingMachineCode")
    @TableField(value = "VULCANIZING_MACHINE_CODE")
    private String vulcanizingMachineCode;

    /**
     * 胎别，按胎别决定排产策略，全钢，半钢
     */
    @Excel(name = "ui.data.column.VulcanizingMachine.productTypeCode", dictType = "biz_product_type")
    @ApiModelProperty(value = "胎别，按胎别决定排产策略，全钢，半钢，字典：biz_product_type", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 模台数
     */
    @Excel(name = "ui.data.column.VulcanizingMachine.mouldNum")
    @ApiModelProperty(value = "模台数", name = "mouldNum")
    @TableField(value = "MOULD_NUM")
    private Integer mouldNum;

    /**
     * 模台类型:字段值为空时不限制， 模具类型之间用","分隔
     */
    @Excel(name = "ui.data.column.VulcanizingMachine.mouldType", dictType = "biz_mould_Type")
    @ApiModelProperty(value = "模台类型:字段值为空时不限制， 模具类型之间用,分隔，字典：biz_mould_Type", name = "mouldType")
    @TableField(value = "MOULD_TYPE")
    private String mouldType;

    /**
     * 单开模:0-否，1-是
     */
    @Excel(name = "ui.data.column.VulcanizingMachine.single", dictType = "biz_yes_no")
    @ApiModelProperty(value = "单开模，字典：biz_yes_no", name = "single")
    @TableField(value = "SINGLE")
    private Integer single;

    /**
     * 是否封存:0-可用，1-封存
     */
    @Excel(name = "ui.data.column.VulcanizingMachine.isClosed", dictType = "is_sealed")
    @ApiModelProperty(value = "是否封存，字典：is_sealed", name = "isClosed")
    @TableField(value = "IS_CLOSED")
    private Integer isClosed;

    /**
     * 硫化机日状态，0启用，1禁用，字典：STATUS
     */
    @Excel(name = "ui.data.column.VulcanizingMachine.dayStatus", dictType = "STATUS")
    @ApiModelProperty(value = "硫化机日状态，0启用，1禁用，字典：STATUS", name = "dayStatus")
    @TableField(value = "DAY_STATUS")
    private String dayStatus;

}
