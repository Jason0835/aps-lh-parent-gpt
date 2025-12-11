package com.zlt.aps.monthplan.api.domain.entity;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.ruoyi.common.core.web.domain.BaseEntity;


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

@ApiModel(value = "历史销售记录对象", description = "历史销售记录对象 ")
@Data
public class MpHistorySaleRecord extends BaseEntity{

    private static final long serialVersionUID = 1L;

     /** 年份 */
    @Excel(name = "ui.data.column.historySaleRecord.year")
    @ApiModelProperty(value = "年份", name = "year")
    private Integer year;

    /** 月份 */
    @Excel(name = "ui.data.column.historySaleRecord.month")
    @ApiModelProperty(value = "月份", name = "month")
    private Integer month;

    /** 区域 */
    @Excel(name = "ui.data.column.historySaleRecord.areaCode")
    @ApiModelProperty(value = "区域", name = "areaCode")
    private String areaCode;

    /** 物料编码 */
    @Excel(name = "ui.data.column.historySaleRecord.productCode")
    @ApiModelProperty(value = "物料编码", name = "productCode")
    private String productCode;

    /** 销量 */
    @Excel(name = "ui.data.column.historySaleRecord.saleQty")
    @ApiModelProperty(value = "销量", name = "saleQty")
    private Integer saleQty;

    /** 生成日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.historySaleRecord.generationDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "生成日期", name = "generationDate")
    private Date generationDate;

}