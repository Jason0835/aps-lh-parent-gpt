package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.domain.CommonBusiEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：CxMonthStock.java
 * 描    述：成型工序胎胚月结库存对象 t_cx_month_stock
 *@author zlt
 *@date 2025-02-17
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "成型工序胎胚月结库存对象", description = "成型工序胎胚月结库存对象 ")
@Data
@TableName(value = "T_CX_MONTH_STOCK")
@KeySequence(value = "SEQ_ONTH_STOCK")
public class CxMonthStock extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 分厂编号 */
    @Excel(name = "ui.data.column.lhMonthPlanSurplus.factoryCode")
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

     /** 库存所属月份：yyyy-mm */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.cxMonthStock.stockMonth", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "库存所属月份：yyyy-mm", name = "stockMonth")
    @TableField(value = "STOCK_MONTH")
    private Date stockMonth;

    /** BOM信息中所使用的版本 */
    @Excel(name = "ui.data.column.cxMonthStock.bomDataVersion")
    @ApiModelProperty(value = "BOM信息中所使用的版本", name = "bomDataVersion")
    @TableField(value = "BOM_DATA_VERSION")
    private String bomDataVersion;

    /** 胎胚代码 */
    @Excel(name = "ui.data.column.cxMonthStock.embryoCode")
    @ApiModelProperty(value = "胎胚代码", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /** 库存量 */
    @Excel(name = "ui.data.column.cxMonthStock.stockNum")
    @ApiModelProperty(value = "库存量", name = "stockNum")
    @TableField(value = "STOCK_NUM")
    private Integer stockNum;

    /** 超期库存 */
    @Excel(name = "ui.data.column.cxMonthStock.overTimeStock")
    @ApiModelProperty(value = "超期库存", name = "overTimeStock")
    @TableField(value = "OVER_TIME_STOCK")
    private Integer overTimeStock;


}