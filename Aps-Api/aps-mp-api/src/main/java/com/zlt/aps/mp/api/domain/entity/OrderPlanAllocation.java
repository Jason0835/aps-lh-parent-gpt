package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：OrderPlanAllocation.java
 * 描    述：月度销售计划订单分配结果对象 t_mp_order_plan_allocation
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-17
 */

@Data
@TableName(value = "T_MP_ORDER_PLAN_ALLOCATION")
@ApiModel(value = "月度销售计划订单分配结果对象", description = "月度销售计划订单分配结果对象 ")
public class OrderPlanAllocation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 计划版本号
     */
    @Excel(name = "ui.data.column.SaleOrderAllocation.monthPlanVersion")
    @ApiModelProperty(value = "计划版本号", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 订单号
     */
    @Excel(name = "ui.data.column.SaleOrderAllocation.orderNo")
    @ApiModelProperty(value = "订单号", name = "orderNo")
    @TableField(value = "ORDER_NO")
    private String orderNo;

    /**
     * 客户编码
     */
    @Excel(name = "ui.data.column.SaleOrderAllocation.customCode")
    @ApiModelProperty(value = "客户编码", name = "customCode")
    @TableField(value = "CUSTOM_CODE")
    private String customCode;

    /**
     * 客户名称
     */
    @Excel(name = "ui.data.column.SaleOrderAllocation.customName")
    @ApiModelProperty(value = "客户名称", name = "customName")
    @TableField(value = "CUSTOM_NAME")
    private String customName;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.SaleOrderAllocation.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.SaleOrderAllocation.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 分厂编码
     */
    @Excel(name = "ui.data.column.SaleOrderAllocation.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂编码", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 库位类别 1 内销 2 外销 3 OE
     */
    @Excel(name = "ui.data.column.SaleOrderAllocation.locationType", dictType = "biz_stor_type")
    @ApiModelProperty(value = "库位类别 1 内销 2 外销 3 OE", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;

    /**
     * 渠道
     */
    @Excel(name = "ui.data.column.SaleOrderAllocation.channel", dictType = "biz_channel_type")
    @ApiModelProperty(value = "渠道", name = "channel")
    @TableField(value = "CHANNEL")
    private String channel;

    /**
     * 品牌
     */
    @Excel(name = "ui.data.column.SaleOrderAllocation.brand", dictType = "biz_brand_type")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /**
     * 物料编码
     */
    @Excel(name = "ui.data.column.SaleOrderAllocation.productCode")
    @ApiModelProperty(value = "物料编码", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 物料描述
     */
    @Excel(name = "ui.data.column.SaleOrderAllocation.productDesc")
    @ApiModelProperty(value = "物料描述", name = "productDesc")
    @TableField(value = "PRODUCT_DESC")
    private String productDesc;

    /**
     * 订单数量
     */
    @Excel(name = "ui.data.column.SaleOrderAllocation.planQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "订单数量", name = "planQty")
    @TableField(value = "PLAN_QTY")
    private Long planQty;

    /**
     * 分类标识 0 一般贸易 1 EUDR 2 非EUDR
     */
    // @Excel(name = "ui.data.column.SaleOrderAllocation.tradeMode")
    @ApiModelProperty(value = "分类标识 0 一般贸易 1 EUDR 2 非EUDR", name = "tradeMode")
    @TableField(value = "TRADE_MODE")
    private String tradeMode;

    /**
     * 销售人员
     */
    @Excel(name = "ui.data.column.SaleOrderAllocation.salePerson")
    @ApiModelProperty(value = "销售人员", name = "salePerson")
    @TableField(value = "SALE_PERSON")
    private String salePerson;

    /**
     * 是否重要客户 0 不重要 1 重要
     */
    @Excel(name = "ui.data.column.SaleOrderAllocation.isImportantCustom", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否重要客户 0 不重要 1 重要", name = "isImportantCustom")
    @TableField(value = "IS_IMPORTANT_CUSTOM")
    private Integer isImportantCustom;

    /**
     * 是否必保计划 0 不必保 1 必保
     */
    @Excel(name = "ui.data.column.SaleOrderAllocation.isEnsurePlan", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否必保计划 0 不必保 1 必保", name = "isEnsurePlan")
    @TableField(value = "IS_ENSURE_PLAN")
    private Integer isEnsurePlan;

    /**
     * 是否紧急订单 0 不紧急 1 紧急
     */
    @Excel(name = "ui.data.column.SaleOrderAllocation.isEmergency", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否紧急订单 0 不紧急 1 紧急", name = "isEmergency")
    @TableField(value = "IS_EMERGENCY")
    private Integer isEmergency;

    /**
     * 合同号
     */
    @Excel(name = "ui.data.column.SaleOrderAllocation.contractNo")
    @ApiModelProperty(value = "合同号", name = "contractNo")
    @TableField(value = "CONTRACT_NO")
    private String contractNo;

    /**
     * 国家
     */
    @Excel(name = "ui.data.column.SaleOrderAllocation.nation")
    @ApiModelProperty(value = "国家", name = "nation")
    @TableField(value = "NATION")
    private String nation;

    /**
     * 提报日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.SaleOrderAllocation.submissionDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "提报日期", name = "submissionDate")
    @TableField(value = "SUBMISSION_DATE")
    private Date submissionDate;

    /**
     * 期望交期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.SaleOrderAllocation.deliveryDateDue", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "期望交期", name = "deliveryDateDue")
    @TableField(value = "DELIVERY_DATE_DUE")
    private Date deliveryDateDue;

    /**
     * 库存对冲分配数
     */
    @Excel(name = "ui.data.column.SaleOrderAllocation.allocationQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "库存对冲分配数", name = "allocationQty")
    @TableField(value = "ALLOCATION_QTY")
    private Long allocationQty;

    /**
     * 预计需要生产量
     */
    @Excel(name = "ui.data.column.SaleOrderAllocation.produceQtyDue", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "预计需要生产量", name = "produceQtyDue")
    @TableField(value = "PRODUCE_QTY_DUE")
    private Long produceQtyDue;

    /**
     * 寸口（保留2位小数）
     */
    @Excel(name = "ui.data.column.SaleOrderAllocation.proSize")
    @ApiModelProperty(value = "寸口", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private BigDecimal proSize;

    /**
     * 品名编码
     */
    @Excel(name = "ui.data.column.require.productTypeCode")
    @ApiModelProperty(value = "品名编码", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 品名
     */
    // @Excel(name = "ui.data.column.require.productTypeName")
    @ApiModelProperty(value = "品名", name = "productTypeName")
    @TableField(value = "PRODUCT_TYPE_NAME")
    private String productTypeName;

    /**
     * 规格
     */
    @Excel(name = "ui.data.column.SaleOrderAllocation.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /**
     * 花纹
     */
    @Excel(name = "ui.data.column.SaleOrderAllocation.pattern")
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /**
     * 层级
     */
    // @Excel(name = "ui.data.column.SaleOrderAllocation.hierarchy")
    @ApiModelProperty(value = "层级", name = "hierarchy")
    @TableField(value = "HIERARCHY")
    private String hierarchy;

    @TableField(exist = false)
    private String commonType;
    /**
     * 得到分组的key
     * 按分厂+物料编码
     *
     * @return
     */
    public String getGroupKey() {
        String keyFormat = "%s|*|%s";
        return String.format(keyFormat, factoryCode, productCode);
    }

    /**
     * 得到库位分组的key
     * 按分厂+物料编码+库位
     */
    public String getStockGroupKey() {
        String keyFormat = "%s|*|%s|*|%s";
        return String.format(keyFormat, factoryCode, productCode, locationType);
    }

}