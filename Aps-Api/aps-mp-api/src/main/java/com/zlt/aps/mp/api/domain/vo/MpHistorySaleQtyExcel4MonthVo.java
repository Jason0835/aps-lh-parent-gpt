package com.zlt.aps.mp.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 历史销售记录Excel实体VO类
 *
 * @author hsc
 * @since 2025/2/14
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MpHistorySaleQtyExcel4MonthVo extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 年份
     */
    @ApiModelProperty(value = "年份")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.year")
    @ImportExcelValidated(required = true, number = true)
    private Integer year;

    /**
     * 月份
     */
    @ApiModelProperty(value = "月份")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.month")
    @ImportExcelValidated(required = true, number = true, min = 1, max = 12)
    private Integer month;

    /**
     * SAP代码
     */
    @ApiModelProperty(value = "SAP代码")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.sapCode")
    @ImportExcelValidated(required = true)
    private String sapCode;

    /**
     * 内销销售订单量
     */
    @ApiModelProperty(value = "内销销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.domesticSalesOrderCount")
//    @ImportExcelValidated(required = true, number = true)
    private Long domesticSalesOrderCount;

    /**
     * 外销销售订单量
     */
    @ApiModelProperty(value = "外销销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.foreignSalesOrderCount")
//    @ImportExcelValidated(required = true, number = true)
    private Long foreignSalesOrderCount;

    /**
     * OE销售订单量
     */
    @ApiModelProperty(value = "OE销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.oeSalesOrderCount")
//    @ImportExcelValidated(required = true, number = true)
    private Long oeSalesOrderCount;

    /**
     * 内销销售数量
     */
    @ApiModelProperty(value = "内销销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.domesticSalesCount")
//    @ImportExcelValidated(required = true, number = true)
    private Long domesticSalesCount;

    /**
     * 外销销售数量
     */
    @ApiModelProperty(value = "外销销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.foreignSalesCount")
//    @ImportExcelValidated(required = true, number = true)
    private Long foreignSalesCount;

    /**
     * OE销售数量
     */
    @ApiModelProperty(value = "OE销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.oeSalesCount")
//    @ImportExcelValidated(required = true, number = true)
    private Long oeSalesCount;

    /**
     * 规格描述信息
     */
    @ApiModelProperty(value = "规格描述")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.specDesc")
    private String specDesc;

    /**
     * 规格描述信息
     */
    @ApiModelProperty(value = "备注")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.remark")
    private String remark;

    /**
     * 创建者
     */
    @ApiModelProperty("创建者")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.createBy")
    private String createBy;

    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.createTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
