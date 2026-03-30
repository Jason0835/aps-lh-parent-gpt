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

@ApiModel(value = "出库未扫描订单对象", description = "出库未扫描订单对象")
@Data
@TableName(value = "T_MDM_OUTBOUNT_ORDERS_NOT_SCAN")
public class MdmOutbountOrdersNotScan extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "DN号", name = "saleBillNo")
    @TableField(value = "SALE_BILL_NO")
    private String saleBillNo;

    @ApiModelProperty(value = "出运单号", name = "saleOrderNo")
    @TableField(value = "SALE_ORDER_NO")
    private String saleOrderNo;

    @ApiModelProperty(value = "销售组织编码", name = "saleOrg")
    @TableField(value = "SALE_ORG")
    private String saleOrg;

    @ApiModelProperty(value = "销售组织名称", name = "saleOrgName")
    @TableField(value = "SALE_ORG_NAME")
    private String saleOrgName;

    @ApiModelProperty(value = "客户编码", name = "sellTo")
    @TableField(value = "SELL_TO")
    private String sellTo;

    @ApiModelProperty(value = "出库单号", name = "billId")
    @TableField(value = "BILL_ID")
    private String billId;

    @ApiModelProperty(value = "MES物料号", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    @ApiModelProperty(value = "NC物料号", name = "sapCode")
    @TableField(value = "SAP_CODE")
    private String sapCode;

    @ApiModelProperty(value = "物料描述", name = "materialName")
    @TableField(value = "MATERIAL_NAME")
    private String materialName;

    @ApiModelProperty(value = "年周号要求", name = "dot")
    @TableField(value = "DOT")
    private String dot;

    @ApiModelProperty(value = "扫描数量", name = "scanAmount")
    @TableField(value = "SCAN_AMOUNT")
    private BigDecimal scanAmount;

    @ApiModelProperty(value = "计划数量", name = "outAmount")
    @TableField(value = "OUT_AMOUNT")
    private BigDecimal outAmount;

    @ApiModelProperty(value = "未扫描数量", name = "noscanAmount")
    @TableField(value = "NOSCAN_AMOUNT")
    private BigDecimal noscanAmount;

    @ApiModelProperty(value = "SCM行内码", name = "saleItemNo")
    @TableField(value = "SALE_ITEM_NO")
    private BigDecimal saleItemNo;

    @ApiModelProperty(value = "分公司", name = "companyCode")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    @ApiModelProperty(value = "分厂", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;
}
