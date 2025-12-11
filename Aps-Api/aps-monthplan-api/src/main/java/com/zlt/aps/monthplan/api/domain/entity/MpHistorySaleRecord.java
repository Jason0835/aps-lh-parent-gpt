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
 * 文件名称：MpHistorySaleRecord.java
 * 描    述：历史销售记录对象 t_mp_history_sale_record
 *@author yelq
 *@date 2025-12-11
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@ApiModel(value = "历史销售记录对象", description = "历史销售记录对象")
@Data
@TableName(value = "T_MP_HISTORY_SALE_RECORD")
public class MpHistorySaleRecord extends BaseEntity{

    private static final long serialVersionUID = 1L;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.MpHistorySaleRecord.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.MpHistorySaleRecord.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 区域
     */
    @Excel(name = "ui.data.column.MpHistorySaleRecord.areaCode")
    @ApiModelProperty(value = "区域", name = "areaCode")
    @TableField(value = "AREA_CODE")
    private String areaCode;

    /**
     * 物料编码
     */
    @Excel(name = "ui.data.column.MpHistorySaleRecord.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * 销量
     */
    @Excel(name = "ui.data.column.MpHistorySaleRecord.saleQty")
    @ApiModelProperty(value = "销量", name = "saleQty")
    @TableField(value = "SALE_QTY")
    private Integer saleQty;

    /**
     * 生成日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.MpHistorySaleRecord.generationDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "生成日期", name = "generationDate")
    @TableField(value = "GENERATION_DATE")
    private Date generationDate;

}
