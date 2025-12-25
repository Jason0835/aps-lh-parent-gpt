package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpOverdueSku.java
 * 描    述：超期SKU对象 t_mp_overdue_sku
 *@author yelq
 *@date 2025-12-12
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@ApiModel(value = "超期SKU对象", description = "超期SKU对象")
@Data
@TableName(value = "t_mdm_overdue_sku")
public class MpOverdueSku extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 生产分厂编号 */
    @Excel(name = "ui.data.column.overdueSku.factoryCode")
    @ApiModelProperty(value = "生产分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 年份 */
    @Excel(name = "ui.data.column.overdueSku.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /** 月份 */
    @Excel(name = "ui.data.column.overdueSku.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /** MES物料编码 */
    @Excel(name = "ui.data.column.overdueSku.mesMaterialCode")
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /** 物料编码 */
    @Excel(name = "ui.data.column.overdueSku.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 物料描述 */
    @Excel(name = "ui.data.column.overdueSku.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /** 年周号 */
    @Excel(name = "ui.data.column.overdueSku.weekYear")
    @ApiModelProperty(value = "年周号", name = "weekYear")
    @TableField(value = "WEEK_YEAR")
    private String weekYear;

    /** 库存日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.overdueSku.stockDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "库存日期", name = "stockDate")
    @TableField(value = "STOCK_DATE")
    private Date stockDate;

    /** 超期常规储备排产日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.overdueSku.overdueRegularDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "超期常规储备排产日期", name = "overdueRegularDate")
    @TableField(value = "OVERDUE_REGULAR_DATE")
    private Date overdueRegularDate;

    /** 是否超期常规储备排产 */
    @Excel(name = "ui.data.column.overdueSku.isOverdueRegular")
    @ApiModelProperty(value = "是否超期常规储备排产", name = "isOverdueRegular")
    @TableField(value = "IS_OVERDUE_REGULAR")
    private String isOverdueRegular;

    /** 超期周期储备排产日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.overdueSku.overdueCycleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "超期周期储备排产日期", name = "overdueCycleDate")
    @TableField(value = "OVERDUE_CYCLE_DATE")
    private Date overdueCycleDate;

    /** 是否超期周期排产 */
    @Excel(name = "ui.data.column.overdueSku.isOverdueCycle")
    @ApiModelProperty(value = "是否超期周期排产", name = "isOverdueCycle")
    @TableField(value = "IS_OVERDUE_CYCLE")
    private String isOverdueCycle;

}
