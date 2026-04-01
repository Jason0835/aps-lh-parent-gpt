package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

import java.math.BigDecimal;
import java.util.Date;

@ApiModel(value = "已出库未扫描版本对象", description = "已出库未扫描版本对象")
@Data
@TableName(value = "T_DP_SHIPPED_NOT_SCAN_VERSION")
public class DpShippedNotScanVersion extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.dpShippedNotScanVersion.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.dpShippedNotScanVersion.year", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    @Excel(name = "ui.data.column.dpShippedNotScanVersion.month", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    @Excel(name = "ui.data.column.dpShippedNotScanVersion.requireVersion")
    @ApiModelProperty(value = "需求版本号", name = "requireVersion")
    @TableField(value = "REQUIRE_VERSION")
    private String requireVersion;

    @Excel(name = "ui.data.column.dpShippedNotScanVersion.saleBillNo")
    @ApiModelProperty(value = "DN号", name = "saleBillNo")
    @TableField(value = "SALE_BILL_NO")
    private String saleBillNo;

    @Excel(name = "ui.data.column.dpShippedNotScanVersion.saleOrderNo")
    @ApiModelProperty(value = "出运单号", name = "saleOrderNo")
    @TableField(value = "SALE_ORDER_NO")
    private String saleOrderNo;

    @ApiModelProperty(value = "销售组织编码", name = "saleOrg")
    @TableField(value = "SALE_ORG")
    private String saleOrg;

    @Excel(name = "ui.data.column.dpShippedNotScanVersion.saleOrgName")
    @ApiModelProperty(value = "销售组织名称", name = "saleOrgName")
    @TableField(value = "SALE_ORG_NAME")
    private String saleOrgName;

    @Excel(name = "ui.data.column.dpShippedNotScanVersion.sellTo")
    @ApiModelProperty(value = "客户编码", name = "sellTo")
    @TableField(value = "SELL_TO")
    private String sellTo;

    @Excel(name = "ui.data.column.dpShippedNotScanVersion.billId")
    @ApiModelProperty(value = "出库单号", name = "billId")
    @TableField(value = "BILL_ID")
    private String billId;

    @ApiModelProperty(value = "MES物料号", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    @Excel(name = "ui.data.column.dpShippedNotScanVersion.sapCode")
    @ApiModelProperty(value = "NC物料号", name = "sapCode")
    @TableField(value = "SAP_CODE")
    private String sapCode;

    @Excel(name = "ui.data.column.dpShippedNotScanVersion.materialName")
    @ApiModelProperty(value = "物料描述", name = "materialName")
    @TableField(value = "MATERIAL_NAME")
    private String materialName;

    @Excel(name = "ui.data.column.dpShippedNotScanVersion.dot")
    @ApiModelProperty(value = "年周号要求", name = "dot")
    @TableField(value = "DOT")
    private String dot;

    @Excel(name = "ui.data.column.dpShippedNotScanVersion.scanAmount", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "扫描数量", name = "scanAmount")
    @TableField(value = "SCAN_AMOUNT")
    private BigDecimal scanAmount;

    @Excel(name = "ui.data.column.dpShippedNotScanVersion.outAmount", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "计划数量", name = "outAmount")
    @TableField(value = "OUT_AMOUNT")
    private BigDecimal outAmount;

    @Excel(name = "ui.data.column.dpShippedNotScanVersion.noscanAmount", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "未扫描数量", name = "noscanAmount")
    @TableField(value = "NOSCAN_AMOUNT")
    private BigDecimal noscanAmount;

    @Excel(name = "ui.data.column.dpShippedNotScanVersion.saleItemNo")
    @ApiModelProperty(value = "SCM行内码", name = "saleItemNo")
    @TableField(value = "SALE_ITEM_NO")
    private String saleItemNo;
}
