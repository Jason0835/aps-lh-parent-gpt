package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpFactoryProductionVersion.java
 * 描    述：S2-0206.排产配置_指定上机配置对象 t_mp_appoint_configuration
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2026-08-14
 */

@Data
@TableName(value = "T_MP_APPOINT_CONFIGURATION")
@ApiModel(value = "S2-0206.排产配置_指定上机配置", description = "S2-0206.排产配置_指定上机配置 ")
public class MpFactoryAppointConfiguration extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号
     */
    @Excel(name = "ui.data.column.MpFactoryAppointConfiguration.factoryCode")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 产品分类 数据字典：biz_product_type TBR 全钢 PCR 半钢
     */
    @Excel(name = "ui.data.column.MpFactoryAppointConfiguration.productTypeCode")
    @ApiModelProperty(value = "产品分类 数据字典：biz_product_type TBR 全钢 PCR 半钢", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.MpFactoryAppointConfiguration.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.MpFactoryAppointConfiguration.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 产品结构
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.MpFactoryAppointConfiguration.structureName")
    @ApiModelProperty(value = "产品结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /**
     * 成型机编码
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.MpFactoryAppointConfiguration.cxMachineCode")
    @ApiModelProperty(value = "成型机编码", name = "cxMachineCode")
    @TableField(value = "CX_MACHINE_CODE")
    private String cxMachineCode;
    /**
     * 开始上机日期
     */
    @Excel(name = "ui.data.column.MpFactoryAppointConfiguration.beginDate", dateFormat = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "开始上机日期", name = "beginDate")
    @TableField(value = "BEGIN_DATE")
    private Date beginDate;

    /**
     * 开始上机日
     */
    @Excel(name = "ui.data.column.MpFactoryAppointConfiguration.beginDay", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "开始上机日", name = "beginDay")
    @TableField(value = "BEGIN_DAY")
    private Integer beginDay;

    /**
     * 上机最大天数
     */
    @Excel(name = "ui.data.column.MpFactoryAppointConfiguration.allotDays", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "上机最大天数", name = "allotDays")
    @TableField(value = "ALLOT_DAYS")
    private Integer allotDays;

}