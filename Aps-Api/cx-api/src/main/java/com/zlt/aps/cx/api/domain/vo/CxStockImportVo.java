package com.zlt.aps.cx.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：CxStock.java
 * 描    述：成型库存信息对象 t_cx_stock
 *@author zlt
 *@date 2025-02-17
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "成型库存信息对象", description = "成型库存信息对象 ")
@Data
@TableName(value = "T_CX_STOCK")
public class CxStockImportVo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 分厂编号 */
    @Excel(name = "ui.data.column.cxStock.factoryCode", dictType = "biz_factory_name")
    @ImportExcelValidated(required = true,maxLength = 30)
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 库存日期，格式：yyyy-MM-dd */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.cxStock.stockDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "库存日期，格式：yyyy-MM-dd", name = "stockDate")
    @TableField(value = "STOCK_DATE")
    private Date stockDate;

    /** 胎胚代码 */
    @Excel(name = "ui.data.column.cxStock.embryoCode")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "胎胚代码", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /** 库存量 */
    @Excel(name = "ui.data.column.cxStock.stockNum")
    @ImportExcelValidated(required = true,digits = true,max = 999999)
    @ApiModelProperty(value = "库存量", name = "stockNum")
    @TableField(value = "STOCK_NUM")
    private Integer stockNum;

    /** 超期库存 */
    // @Excel(name = "ui.data.column.cxStock.overTimeStock")
    @ApiModelProperty(value = "超期库存", name = "overTimeStock")
    @TableField(value = "OVER_TIME_STOCK")
    private Integer overTimeStock;

    /** 修正数量 */
    //   @Excel(name = "ui.data.column.cxStock.modifyNum")
    @ApiModelProperty(value = "修正数量", name = "modifyNum")
    @TableField(value = "MODIFY_NUM")
    private Integer modifyNum;

    /** 不良数量 */
//    @Excel(name = "ui.data.column.cxStock.badNum")
    @ApiModelProperty(value = "不良数量", name = "badNum")
    @TableField(value = "BAD_NUM")
    private Integer badNum;


    /** 是否收尾SKU：0-否，1-是 */
//    @Excel(name = "ui.data.column.cxStock.isEndingSku", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否收尾SKU：0-否，1-是", name = "isEndingSku")
    @TableField(value = "IS_ENDING_SKU")
    private String isEndingSku;



    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.data.column.stock.remark")
    private String remark;




}
