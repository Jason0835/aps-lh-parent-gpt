package com.zlt.aps.mp.api.domain.entity;

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
 * 描    述：历史销售记录对象 T_MDM_HISTORY_SALE_RECORD
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
@TableName(value = "T_MDM_HISTORY_SALE_RECORD")
public class MpHistorySaleRecord extends BaseEntity{

    private static final long serialVersionUID = 1L;

    /**
     * 工厂
     */
    @Excel(name = "ui.data.column.MpHistorySaleRecord.factoryCode")
    @ApiModelProperty(value = "工厂", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.MpHistorySaleRecord.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "`YEAR`")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.MpHistorySaleRecord.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "`MONTH`")
    private Integer month;

    /**
     * 年月
     */
    @ApiModelProperty(value = "年月", name = "yearMonth")
    @TableField(value = "`YEAR_MONTH`")
    private Integer yearMonth;

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
    @Excel(name = "ui.data.column.MpHistorySaleRecord.genrateDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "生成日期", name = "genrateDate")
    @TableField(value = "GENRATE_DATE")
    private Date genrateDate;

    /**
     * 区域名称国际化字符串
     */
    @ApiModelProperty(value = "区域名称国际化字符串", name = "areaCodeName")
    @TableField(exist = false)
    private String areaCodeName;

    /**
     * 区域名称国际化后
     */
    @ApiModelProperty(value = "区域名称国际化后", name = "areaCodeNameI18n")
    @TableField(exist = false)
    private String areaCodeNameI18n;

    /**
     * area拼接区域编号，前端使用
     */
    @ApiModelProperty(value = "area拼接区域编号，前端使用", name = "areaCodeShow")
    @TableField(exist = false)
    private String areaCodeShow;

    /**
     * month拼接月，前端使用
     */
    @ApiModelProperty(value = "month拼接月，前端使用", name = "monthShow")
    @TableField(exist = false)
    private String monthShow;

    /**
     * 黄色标识，月均销量，前端展示、导出时标识使用
     */
    @ApiModelProperty(value = "黄色标识，月均销量，前端展示、导出时标识使用", name = "yellowColorFlag")
    @TableField(exist = false)
    private String yellowColorFlag;

    /**
     * 近12个月的发货频次
     */
    @ApiModelProperty(value = "近12个月发货频次", name = "deliveryFrequency")
    @TableField(exist = false)
    private Integer deliveryFrequency;
}
