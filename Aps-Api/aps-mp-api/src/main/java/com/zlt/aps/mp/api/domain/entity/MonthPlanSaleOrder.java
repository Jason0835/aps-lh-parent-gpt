package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.mp.api.enums.SaleOrderSourceTypeEnum;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanSaleOrder.java
 * 描    述：月度销售计划订单对象 t_mp_month_sale_plan
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-14
 */

@Data
@ApiModel(value = "月度销售计划订单对象", description = "月度销售计划订单对象 ")
@TableName(value = "T_MP_MONTH_SALE_PLAN")
public class MonthPlanSaleOrder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 分厂编码
     */
    @Excel(name = "ui.data.column.sale.factoryCode", sort = 5, dictType = "biz_factory_name")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "分厂编码", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 订单号
     */
    @Excel(name = "ui.data.column.sale.orderNo", sort = 22)
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "订单号", name = "orderNo")
    @TableField(value = "ORDER_NO")
    private String orderNo;

    /**
     * 数据来源
     */
    @ApiModelProperty(value = "数据来源", name = "sourceType")
    @TableField(value = "SOURCE_TYPE")
    private Integer sourceType;
    /**
     * 客户编码
     */
    @Excel(name = "ui.data.column.sale.customCode", sort = 14)
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "客户编码", name = "customCode")
    @TableField(value = "CUSTOM_CODE")
    private String customCode;

    /**
     * 客户名称
     */
    @Excel(name = "ui.data.column.sale.customName", sort = 15)
    @ApiModelProperty(value = "客户名称", name = "customName")
    @TableField(value = "CUSTOM_NAME")
    private String customName;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.sale.year", sort = 1)
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.sale.month", sort = 2)
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 库位类别 1 内销 2 外销 3 OE
     */
    @Excel(name = "ui.data.column.sale.locationType", sort = 3, dictType = "biz_stor_type")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "库位类别 1 内销 2 外销 3 OE", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;

    /**
     * 渠道
     */
    @Excel(name = "ui.data.column.sale.channel", sort = 4, dictType = "biz_channel_type")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "渠道", name = "channel")
    @TableField(value = "CHANNEL")
    private String channel;

    /**
     * 品牌
     */
    @Excel(name = "ui.data.column.sale.brand", sort = 7, dictType = "biz_brand_type")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /**
     * 物料编码
     */
    @Excel(name = "ui.data.column.sale.productCode", sort = 6)
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "物料编码", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 物料描述
     */
    @Excel(name = "ui.data.column.sale.productDesc", sort = 8)
    @ApiModelProperty(value = "物料描述", name = "productDesc")
    @TableField(value = "PRODUCT_DESC")
    private String productDesc;

    /**
     * 寸口（保留2位小数）
     */
    @Excel(name = "ui.data.column.sale.proSize", sort = 11)
    @ImportExcelValidated(number = true)
    @ApiModelProperty(value = "寸口", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private BigDecimal proSize;

    /**
     * 品名编码
     */
    @ApiModelProperty(value = "品名编码", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 品名
     */
    @ApiModelProperty(value = "品名", name = "productTypeName")
    @TableField(value = "PRODUCT_TYPE_NAME")
    private String productTypeName;

    /**
     * 规格
     */
    @Excel(name = "ui.data.column.sale.specifications", sort = 9)
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /**
     * 花纹
     */
    @Excel(name = "ui.data.column.sale.pattern", sort = 10)
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /**
     * 层级
     */
    @ApiModelProperty(value = "层级", name = "hierarchy")
    @TableField(value = "HIERARCHY")
    private String hierarchy;
    /**
     * 轮胎类型 取数据字典 TIRE_TYPE的编码
     */
    @ApiModelProperty(value = "轮胎类型 取数据字典 TIRE_TYPE的编码", name = "tireType")
    @TableField(value = "TIRE_TYPE")
    private String tireType;
    /**
     * 公用类型 取数据字典 biz_common_type的编码 1 公用规格 2 外销专用 3 内销专用 4 OE专用
     */
    @ApiModelProperty(value = "物料基础数据获取 1 公用规格 2 外销专用 3 内销专用 4 OE专用", name = "commonType")
    @TableField(value = "COMMON_TYPE")
    private String commonType;
    /**
     * 订单数量
     */
    @Excel(name = "ui.data.column.sale.planQty", sort = 12, cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "订单数量", name = "planQty")
    @TableField(value = "PLAN_QTY")
    private Long planQty;

    /**
     * 分类标识 0 一般贸易 1 EUDR 2 非EUDR
     */
    @ApiModelProperty(value = "分类标识 0 一般贸易 1 EUDR 2 非EUDR", name = "tradeMode")
    @TableField(value = "TRADE_MODE")
    private String tradeMode;

    /**
     * 销售人员
     */
    @Excel(name = "ui.data.column.sale.salePerson", sort = 13)
    @ApiModelProperty(value = "销售人员", name = "salePerson")
    @TableField(value = "SALE_PERSON")
    private String salePerson;

    /**
     * 是否重要客户 0 不重要 1 重要
     */
    @ApiModelProperty(value = "是否重要客户 0 不重要 1 重要", name = "isImportantCustom")
    @TableField(value = "IS_IMPORTANT_CUSTOM")
    private Integer isImportantCustom;

    /**
     * 是否必保计划 0 不必保 1 必保
     */
    @Excel(name = "ui.data.column.sale.isEnsurePlan", sort = 24, dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否必保计划 0 不必保 1 必保", name = "isEnsurePlan")
    @TableField(value = "IS_ENSURE_PLAN")
    private Integer isEnsurePlan;

    /**
     * 是否紧急订单 0 不紧急 1 紧急
     */
    @Excel(name = "ui.data.column.sale.isEmergency", sort = 18, dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否紧急订单 0 不紧急 1 紧急", name = "isEmergency")
    @TableField(value = "IS_EMERGENCY")
    private Integer isEmergency;

    /**
     * 合同号
     */
    @Excel(name = "ui.data.column.sale.contractNo", sort = 19)
    @ApiModelProperty(value = "合同号", name = "contractNo")
    @TableField(value = "CONTRACT_NO")
    private String contractNo;

    /**
     * 国家
     */
    @Excel(name = "ui.data.column.sale.nation", sort = 20)
    @ApiModelProperty(value = "国家", name = "nation")
    @TableField(value = "NATION")
    private String nation;

    /**
     * 提报日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.sale.submissionDate", sort = 21, width = 30, dateFormat = "yyyy/MM/dd")
    @ApiModelProperty(value = "提报日期", name = "submissionDate")
    @TableField(value = "SUBMISSION_DATE")
    private Date submissionDate;

    /**
     * 期望交期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.sale.deliveryDateDue", sort = 23, width = 30, dateFormat = "yyyy/MM/dd")
    @ApiModelProperty(value = "期望交期", name = "deliveryDateDue")
    @TableField(value = "DELIVERY_DATE_DUE")
    private Date deliveryDateDue;

    /**
     * 库位列表排序值
     */
    @TableField(exist = false)
    private Integer locationSortValue;

    /**
     * 数据来源说明
     */
    @ApiModelProperty(value = "数据来源说明", name = "sourceTypeDesc")
    @TableField(exist = false)
    private String sourceTypeDesc;

    /**
     * 导入更新的key值
     * 分厂+年+月+物料编码+库位类别+渠道
     * +客户+订单号
     *
     * @return
     */
    public String getImportUpdateKey() {
        String keyFormat = "%s|*|%d|*|%d|*|%s|*|%s|*|%s|*|%s|*|%s";
        return String.format(keyFormat, factoryCode, year, month, productCode, locationType, channel, customCode, orderNo);
    }

    public String getSourceTypeDesc() {
        SaleOrderSourceTypeEnum sourceTypeEnum = SaleOrderSourceTypeEnum.getInstance(getSourceType());
        return sourceTypeEnum.getDesc();
    }

    /**
     * 以分厂+物料为维度，转换库存
     *
     * @return
     */
    public String getGroupKey() {
        String keyFormat = "%s|*|%s";
        return String.format(keyFormat, factoryCode, productCode);
    }
}