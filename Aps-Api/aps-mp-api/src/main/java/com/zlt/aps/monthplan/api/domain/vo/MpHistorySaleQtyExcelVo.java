package com.zlt.aps.monthplan.api.domain.vo;

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
public class MpHistorySaleQtyExcelVo extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 年份
     */
    @ApiModelProperty(value = "年份")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.year")
    @ImportExcelValidated(required = true, number = true)
    private Integer year;

    /**
     * SAP代码
     */
    @ApiModelProperty(value = "SAP代码")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.sapCode")
    @ImportExcelValidated(required = true)
    private String sapCode;

    /**
     * 1月份内销销售订单量
     */
    @ApiModelProperty(value = "1月份内销销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.domesticSalesOrderCount1", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long domesticSalesOrderCount1;

    /**
     * 1月份外销销售订单量
     */
    @ApiModelProperty(value = "1月份外销销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.foreignSalesOrderCount1", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long foreignSalesOrderCount1;

    /**
     * 1月份OE销售订单量
     */
    @ApiModelProperty(value = "1月份OE销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.oeSalesOrderCount1", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long oeSalesOrderCount1;

    /**
     * 1月份内销销售数量
     */
    @ApiModelProperty(value = "1月份内销销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.domesticSalesCount1", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long domesticSalesCount1;

    /**
     * 1月份外销销售数量
     */
    @ApiModelProperty(value = "1月份外销销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.foreignSalesCount1", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long foreignSalesCount1;

    /**
     * 1月份OE销售数量
     */
    @ApiModelProperty(value = "1月份OE销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.oeSalesCount1", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long oeSalesCount1;

    /**
     * 2月份内销销售订单量
     */
    @ApiModelProperty(value = "2月份内销销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.domesticSalesOrderCount2", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long domesticSalesOrderCount2;

    /**
     * 2月份外销销售订单量
     */
    @ApiModelProperty(value = "2月份外销销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.foreignSalesOrderCount2", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long foreignSalesOrderCount2;

    /**
     * 2月份OE销售订单量
     */
    @ApiModelProperty(value = "2月份OE销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.oeSalesOrderCount2", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long oeSalesOrderCount2;

    /**
     * 2月份内销销售数量
     */
    @ApiModelProperty(value = "2月份内销销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.domesticSalesCount2", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long domesticSalesCount2;

    /**
     * 2月份外销销售数量
     */
    @ApiModelProperty(value = "2月份外销销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.foreignSalesCount2", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long foreignSalesCount2;

    /**
     * 2月份OE销售数量
     */
    @ApiModelProperty(value = "2月份OE销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.oeSalesCount2", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long oeSalesCount2;

    /**
     * 3月份内销销售订单量
     */
    @ApiModelProperty(value = "3月份内销销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.domesticSalesOrderCount3", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long domesticSalesOrderCount3;

    /**
     * 3月份外销销售订单量
     */
    @ApiModelProperty(value = "3月份外销销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.foreignSalesOrderCount3", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long foreignSalesOrderCount3;

    /**
     * 3月份OE销售订单量
     */
    @ApiModelProperty(value = "3月份OE销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.oeSalesOrderCount3", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long oeSalesOrderCount3;

    /**
     * 3月份内销销售数量
     */
    @ApiModelProperty(value = "3月份内销销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.domesticSalesCount3", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long domesticSalesCount3;

    /**
     * 3月份外销销售数量
     */
    @ApiModelProperty(value = "3月份外销销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.foreignSalesCount3", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long foreignSalesCount3;

    /**
     * 3月份OE销售数量
     */
    @ApiModelProperty(value = "3月份OE销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.oeSalesCount3", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long oeSalesCount3;

    /**
     * 4月份内销销售订单量
     */
    @ApiModelProperty(value = "4月份内销销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.domesticSalesOrderCount4", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long domesticSalesOrderCount4;

    /**
     * 4月份外销销售订单量
     */
    @ApiModelProperty(value = "4月份外销销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.foreignSalesOrderCount4", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long foreignSalesOrderCount4;

    /**
     * 4月份OE销售订单量
     */
    @ApiModelProperty(value = "4月份OE销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.oeSalesOrderCount4", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long oeSalesOrderCount4;

    /**
     * 4月份内销销售数量
     */
    @ApiModelProperty(value = "4月份内销销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.domesticSalesCount4", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long domesticSalesCount4;

    /**
     * 4月份外销销售数量
     */
    @ApiModelProperty(value = "4月份外销销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.foreignSalesCount4", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long foreignSalesCount4;

    /**
     * 4月份OE销售数量
     */
    @ApiModelProperty(value = "4月份OE销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.oeSalesCount4", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long oeSalesCount4;

    /**
     * 5月份内销销售订单量
     */
    @ApiModelProperty(value = "5月份内销销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.domesticSalesOrderCount5", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long domesticSalesOrderCount5;

    /**
     * 5月份外销销售订单量
     */
    @ApiModelProperty(value = "5月份外销销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.foreignSalesOrderCount5", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long foreignSalesOrderCount5;

    /**
     * 5月份OE销售订单量
     */
    @ApiModelProperty(value = "5月份OE销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.oeSalesOrderCount5", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long oeSalesOrderCount5;

    /**
     * 5月份内销销售数量
     */
    @ApiModelProperty(value = "5月份内销销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.domesticSalesCount5", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long domesticSalesCount5;

    /**
     * 5月份外销销售数量
     */
    @ApiModelProperty(value = "5月份外销销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.foreignSalesCount5", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long foreignSalesCount5;

    /**
     * 5月份OE销售数量
     */
    @ApiModelProperty(value = "5月份OE销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.oeSalesCount5", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long oeSalesCount5;

    /**
     * 6月份内销销售订单量
     */
    @ApiModelProperty(value = "6月份内销销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.domesticSalesOrderCount6", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long domesticSalesOrderCount6;

    /**
     * 6月份外销销售订单量
     */
    @ApiModelProperty(value = "6月份外销销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.foreignSalesOrderCount6", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long foreignSalesOrderCount6;

    /**
     * 6月份OE销售订单量
     */
    @ApiModelProperty(value = "6月份OE销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.oeSalesOrderCount6", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long oeSalesOrderCount6;

    /**
     * 6月份内销销售数量
     */
    @ApiModelProperty(value = "6月份内销销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.domesticSalesCount6", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long domesticSalesCount6;

    /**
     * 6月份外销销售数量
     */
    @ApiModelProperty(value = "6月份外销销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.foreignSalesCount6", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long foreignSalesCount6;

    /**
     * 6月份OE销售数量
     */
    @ApiModelProperty(value = "6月份OE销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.oeSalesCount6", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long oeSalesCount6;

    /**
     * 7月份内销销售订单量
     */
    @ApiModelProperty(value = "7月份内销销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.domesticSalesOrderCount7", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long domesticSalesOrderCount7;

    /**
     * 7月份外销销售订单量
     */
    @ApiModelProperty(value = "7月份外销销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.foreignSalesOrderCount7", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long foreignSalesOrderCount7;

    /**
     * 7月份OE销售订单量
     */
    @ApiModelProperty(value = "7月份OE销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.oeSalesOrderCount7", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long oeSalesOrderCount7;

    /**
     * 7月份内销销售数量
     */
    @ApiModelProperty(value = "7月份内销销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.domesticSalesCount7", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long domesticSalesCount7;

    /**
     * 7月份外销销售数量
     */
    @ApiModelProperty(value = "7月份外销销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.foreignSalesCount7", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long foreignSalesCount7;

    /**
     * 7月份OE销售数量
     */
    @ApiModelProperty(value = "7月份OE销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.oeSalesCount7", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long oeSalesCount7;

    /**
     * 8月份内销销售订单量
     */
    @ApiModelProperty(value = "8月份内销销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.domesticSalesOrderCount8", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long domesticSalesOrderCount8;

    /**
     * 8月份外销销售订单量
     */
    @ApiModelProperty(value = "8月份外销销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.foreignSalesOrderCount8", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long foreignSalesOrderCount8;

    /**
     * 8月份OE销售订单量
     */
    @ApiModelProperty(value = "8月份OE销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.oeSalesOrderCount8", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long oeSalesOrderCount8;

    /**
     * 8月份内销销售数量
     */
    @ApiModelProperty(value = "8月份内销销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.domesticSalesCount8", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long domesticSalesCount8;

    /**
     * 8月份外销销售数量
     */
    @ApiModelProperty(value = "8月份外销销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.foreignSalesCount8", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long foreignSalesCount8;

    /**
     * 8月份OE销售数量
     */
    @ApiModelProperty(value = "8月份OE销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.oeSalesCount8", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long oeSalesCount8;

    /**
     * 9月份内销销售订单量
     */
    @ApiModelProperty(value = "9月份内销销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.domesticSalesOrderCount9", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long domesticSalesOrderCount9;

    /**
     * 9月份外销销售订单量
     */
    @ApiModelProperty(value = "9月份外销销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.foreignSalesOrderCount9", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long foreignSalesOrderCount9;

    /**
     * 9月份OE销售订单量
     */
    @ApiModelProperty(value = "9月份OE销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.oeSalesOrderCount9", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long oeSalesOrderCount9;

    /**
     * 9月份内销销售数量
     */
    @ApiModelProperty(value = "9月份内销销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.domesticSalesCount9", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long domesticSalesCount9;

    /**
     * 9月份外销销售数量
     */
    @ApiModelProperty(value = "9月份外销销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.foreignSalesCount9", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long foreignSalesCount9;

    /**
     * 9月份OE销售数量
     */
    @ApiModelProperty(value = "9月份OE销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.oeSalesCount9", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long oeSalesCount9;

    /**
     * 10月份内销销售订单量
     */
    @ApiModelProperty(value = "10月份OE销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.domesticSalesOrderCount10", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long domesticSalesOrderCount10;

    /**
     * 10月份外销销售订单量
     */
    @ApiModelProperty(value = "10月份外销销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.foreignSalesOrderCount10", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long foreignSalesOrderCount10;

    /**
     * 10月份OE销售订单量
     */
    @ApiModelProperty(value = "10月份OE销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.oeSalesOrderCount10", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long oeSalesOrderCount10;

    /**
     * 10月份内销销售数量
     */
    @ApiModelProperty(value = "10月份内销销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.domesticSalesCount10", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long domesticSalesCount10;

    /**
     * 10月份外销销售数量
     */
    @ApiModelProperty(value = "10月份外销销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.foreignSalesCount10", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long foreignSalesCount10;

    /**
     * 10月份OE销售数量
     */
    @ApiModelProperty(value = "10月份OE销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.oeSalesCount10", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long oeSalesCount10;

    /**
     * 11月份内销销售订单量
     */
    @ApiModelProperty(value = "11月份内销销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.domesticSalesOrderCount11", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long domesticSalesOrderCount11;

    /**
     * 11月份外销销售订单量
     */
    @ApiModelProperty(value = "11月份外销销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.foreignSalesOrderCount11", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long foreignSalesOrderCount11;

    /**
     * 11月份OE销售订单量
     */
    @ApiModelProperty(value = "11月份OE销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.oeSalesOrderCount11", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long oeSalesOrderCount11;

    /**
     * 11月份内销销售数量
     */
    @ApiModelProperty(value = "11月份内销销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.domesticSalesCount11", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long domesticSalesCount11;

    /**
     * 11月份外销销售数量
     */
    @ApiModelProperty(value = "11月份外销销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.foreignSalesCount11", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long foreignSalesCount11;

    /**
     * 11月份OE销售数量
     */
    @ApiModelProperty(value = "11月份OE销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.oeSalesCount11", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long oeSalesCount11;

    /**
     * 12月份内销销售订单量
     */
    @ApiModelProperty(value = "12月份内销销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.domesticSalesOrderCount12", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long domesticSalesOrderCount12;

    /**
     * 12月份外销销售订单量
     */
    @ApiModelProperty(value = "12月份外销销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.foreignSalesOrderCount12", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long foreignSalesOrderCount12;

    /**
     * 12月份OE销售订单量
     */
    @ApiModelProperty(value = "12月份OE销售订单量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.oeSalesOrderCount12", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long oeSalesOrderCount12;

    /**
     * 12月份内销销售数量
     */
    @ApiModelProperty(value = "12月份内销销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.domesticSalesCount12", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long domesticSalesCount12;

    /**
     * 12月份外销销售数量
     */
    @ApiModelProperty(value = "12月份外销销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.foreignSalesCount12", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long foreignSalesCount12;

    /**
     * 12月份OE销售数量
     */
    @ApiModelProperty(value = "12月份OE销售数量")
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.oeSalesCount12", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, number = true)
    private Long oeSalesCount12;

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
    @Excel(name = "ui.data.column.MpHistorySaleQtyExcelVo.createTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
