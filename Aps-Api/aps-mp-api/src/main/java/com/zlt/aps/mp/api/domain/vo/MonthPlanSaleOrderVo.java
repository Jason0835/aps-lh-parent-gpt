package com.zlt.aps.mp.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanSaleOrder.java
 * 描    述：月度销售计划订单对象 t_mp_month_sale_plan
 *@author ZLT
 *@date 2025-02-14
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：ZLT
 *     修改内容：...
 */

@Data
@ApiModel(value = "月度销售计划订单对象Vo", description = "月度销售计划订单对象Vo ")
public class MonthPlanSaleOrderVo implements Serializable {

    private static final long serialVersionUID = 1L;

     /** 分厂编码 */
    @Excel(name = "ui.data.column.sale.factoryCode", sort = 5, dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂编码", name = "factoryCode")
    private String factoryCode;

    /** 订单号 */
    @Excel(name = "ui.data.column.sale.orderNo", sort = 22)
    @ApiModelProperty(value = "订单号", name = "orderNo")
    private String orderNo;

    /** 客户编码 */
    @Excel(name = "ui.data.column.sale.customCode", sort = 14)
    @ApiModelProperty(value = "客户编码", name = "customCode")
    private String customCode;

    /** 客户名称 */
    @Excel(name = "ui.data.column.sale.customName", sort = 15)
    @ApiModelProperty(value = "客户名称", name = "customName")
    private String customName;

    /** 年份 */
    @Excel(name = "ui.data.column.sale.year", sort = 1)
    @ApiModelProperty(value = "年份", name = "year")
    private Integer year;

    /** 月份 */
    @Excel(name = "ui.data.column.sale.month", sort = 2)
    @ApiModelProperty(value = "月份", name = "month")
    private Integer month;

    /** 库位类别 1 内销 2 外销 3 OE */
    @Excel(name = "ui.data.column.sale.locationType", sort = 3, dictType = "biz_stor_type")
    @ApiModelProperty(value = "库位类别 1 内销 2 外销 3 OE", name = "locationType")
    private String locationType;

    /** 渠道 */
    @Excel(name = "ui.data.column.sale.channel", sort = 4, dictType = "biz_channel_type")
    @ApiModelProperty(value = "渠道", name = "channel")
    private String channel;

    /** 品牌 */
    @Excel(name = "ui.data.column.sale.brand", sort = 7, dictType = "biz_brand_type")
    @ApiModelProperty(value = "品牌", name = "brand")
    private String brand;

    /** 物料编码 */
    @Excel(name = "ui.data.column.sale.productCode", sort = 6)
    @ApiModelProperty(value = "物料编码", name = "productCode")
    private String productCode;

    /** 物料描述 */
    @Excel(name = "ui.data.column.sale.productDesc", sort = 8)
    @ApiModelProperty(value = "物料描述", name = "productDesc")
    private String productDesc;

    /** 寸口（保留2位小数） */
    @Excel(name = "ui.data.column.sale.proSize", sort = 11)
    @ApiModelProperty(value = "寸口", name = "proSize")
    private BigDecimal proSize;

    /** 品名编码 */
    @Excel(name = "ui.data.column.require.productTypeCode")
    @ApiModelProperty(value = "品名编码", name = "productTypeCode")
    private String productTypeCode;

    /** 品名 */
    @Excel(name = "ui.data.column.require.productTypeName")
    @ApiModelProperty(value = "品名", name = "productTypeName")
    private String productTypeName;

    /** 规格 */
    @Excel(name = "ui.data.column.sale.specifications", sort = 9)
    @ApiModelProperty(value = "规格", name = "specifications")
    private String specifications;

    /** 花纹 */
    @Excel(name = "ui.data.column.sale.pattern", sort = 10)
    @ApiModelProperty(value = "花纹", name = "pattern")
    private String pattern;

    /** 层级 */
    @Excel(name = "ui.data.column.sale.hierarchy")
    @ApiModelProperty(value = "层级", name = "hierarchy")
    private String hierarchy;

    /** 订单数量 */
    @Excel(name = "ui.data.column.sale.planQty", sort = 12)
    @ApiModelProperty(value = "订单数量", name = "planQty")
    private Long planQty;

    /** 分类标识 0 一般贸易 1 EUDR 2 非EUDR */
    @Excel(name = "ui.data.column.sale.tradeMode")
    @ApiModelProperty(value = "分类标识 0 一般贸易 1 EUDR 2 非EUDR", name = "tradeMode")
    private String tradeMode;

    /** 销售人员 */
    @Excel(name = "ui.data.column.sale.salePerson", sort = 13)
    @ApiModelProperty(value = "销售人员", name = "salePerson")
    private String salePerson;

    /** 是否重要客户 0 不重要 1 重要 */
    @Excel(name = "ui.data.column.sale.isImportantCustom", sort = 16, dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否重要客户 0 不重要 1 重要", name = "isImportantCustom")
    private Integer isImportantCustom;

    /** 是否必保计划 0 不必保 1 必保 */
    @Excel(name = "ui.data.column.sale.isEnsurePlan", sort = 17, dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否必保计划 0 不必保 1 必保", name = "isEnsurePlan")
    private Integer isEnsurePlan;

    /** 是否紧急订单 0 不紧急 1 紧急 */
    @Excel(name = "ui.data.column.sale.isEmergency",sort = 18, dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否紧急订单 0 不紧急 1 紧急", name = "isEmergency")
    private Integer isEmergency;

    /** 合同号 */
    @Excel(name = "ui.data.column.sale.contractNo",sort = 19)
    @ApiModelProperty(value = "合同号", name = "contractNo")
    private String contractNo;

    /** 国家 */
    @Excel(name = "ui.data.column.sale.nation",sort = 20)
    @ApiModelProperty(value = "国家", name = "nation")
    private String nation;

    /** 提报日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.sale.submissionDate", sort = 21, dateFormat = "yyyy/MM/dd")
    @ApiModelProperty(value = "提报日期", name = "submissionDate")
    private Date submissionDate;

    /** 期望交期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.sale.deliveryDateDue", sort = 23, dateFormat = "yyyy/MM/dd")
    @ApiModelProperty(value = "期望交期", name = "deliveryDateDue")
    private Date deliveryDateDue;
    /**
     * 备注
     */
    @Excel(name = "ui.data.column.sale.remark", sort = 24)
    @ApiModelProperty(value = "备注", name = "remark")
    private String remark;

    @Excel(name = "ui.data.column.sale.createBy", sort = 25,  dateFormat = "yyyy/MM/dd")
    @ApiModelProperty("创建者")
    private String createBy;

    @JsonFormat( pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "ui.data.column.sale.createTime", sort = 26, dateFormat = "yyyy/MM/dd")
    @ApiModelProperty("创建日期")
    private Date createTime;
}