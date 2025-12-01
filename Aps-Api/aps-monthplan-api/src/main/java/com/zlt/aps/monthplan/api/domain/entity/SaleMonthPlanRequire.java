package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：SaleMonthPlanRequire.java
 * 描    述：月度生产需求计划对象 t_mp_product_require_plan
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-14
 */

@Data
@TableName(value = "T_MP_PRODUCT_REQUIRE_PLAN")
@ApiModel(value = "月度生产需求计划对象", description = "月度生产需求计划对象 ")
public class SaleMonthPlanRequire extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 需求计划版本号
     */
    @Excel(name = "ui.data.column.require.monthPlanVersion")
    @ApiModelProperty(value = "需求计划版本号", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.require.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.require.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 分厂编码
     */
    @Excel(name = "ui.data.column.require.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂编码", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 库位类别 1 内销 2 外销 3 OE
     */
    @Excel(name = "ui.data.column.require.locationType", dictType = "biz_stor_type")
    @ApiModelProperty(value = "库位类别 1 内销 2 外销 3 OE", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;

    /**
     * 渠道
     */
    @Excel(name = "ui.data.column.require.channel", dictType = "biz_channel_type")
    @ApiModelProperty(value = "渠道", name = "channel")
    @TableField(value = "CHANNEL")
    private String channel;

    /**
     * 品牌
     */
    @Excel(name = "ui.data.column.require.brand", dictType = "biz_brand_type")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /**
     * 物料编码
     */
    @Excel(name = "ui.data.column.require.productCode")
    @ApiModelProperty(value = "物料编码", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 物料描述
     */
    @Excel(name = "ui.data.column.require.productDesc")
    @ApiModelProperty(value = "物料描述", name = "productDesc")
    @TableField(value = "PRODUCT_DESC")
    private String productDesc;

    /**
     * 品名编码
     */
    @Excel(name = "ui.data.column.require.productTypeCode", dictType = "biz_product_name")
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
     * 分类标识 0 一般贸易 1 EUDR 2 非EUDR
     */
    // @Excel(name = "ui.data.column.require.tradeMode")
    @ApiModelProperty(value = "分类标识 0 一般贸易 1 EUDR 2 非EUDR", name = "tradeMode")
    @TableField(value = "TRADE_MODE")
    private String tradeMode;

    /**
     * 是否重要客户 0 不重要 1 重要
     */
    @Excel(name = "ui.data.column.require.isImportantCustom", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否重要客户 0 不重要 1 重要", name = "isImportantCustom")
    @TableField(value = "IS_IMPORTANT_CUSTOM")
    private Integer isImportantCustom;

    /**
     * 是否必保计划 0 不必保 1 必保
     */
    @Excel(name = "ui.data.column.require.isEnsurePlan", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否必保计划 0 不必保 1 必保", name = "isEnsurePlan")
    @TableField(value = "IS_ENSURE_PLAN")
    private Integer isEnsurePlan;

    /**
     * 是否紧急订单 0 不紧急 1 紧急
     */
    @Excel(name = "ui.data.column.require.isEmergency", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否紧急订单 0 不紧急 1 紧急", name = "isEmergency")
    @TableField(value = "IS_EMERGENCY")
    private Integer isEmergency;

    /**
     * 是否欠产 0 不欠产 1欠产
     */
    @Excel(name = "ui.data.column.require.isDebitPlan", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否欠产 0 不欠产 1欠产", name = "isDebitPlan")
    @TableField(value = "IS_DEBIT_PLAN")
    private Integer isDebitPlan;

    /**
     * 是否备货 0 不是 1 是
     */
    @Excel(name = "ui.data.column.require.isStockUp", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否备货 0 不是 1 是", name = "isStockUp")
    @TableField(value = "IS_STOCK_UP")
    private Integer isStockUp;

    /**
     * 期望交期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.require.deliveryDateDue", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "期望交期", name = "deliveryDateDue")
    @TableField(value = "DELIVERY_DATE_DUE")
    private Date deliveryDateDue;

    /**
     * 需要排产数量
     */
    @Excel(name = "ui.data.column.require.planQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "需要排产数量", name = "planQty")
    @TableField(value = "PLAN_QTY")
    private Long planQty;

    /**
     * 合并的订单号-多个以,隔开
     */
    @Excel(name = "ui.data.column.require.orderNo")
    @ApiModelProperty(value = "合并的订单号-多个以,隔开", name = "orderNo")
    @TableField(value = "ORDER_NO")
    private String orderNo;

    /**
     * 寸口（保留2位小数）
     */
    @Excel(name = "ui.data.column.require.proSize")
    @ApiModelProperty(value = "寸口", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private BigDecimal proSize;

    /**
     * 规格
     */
    @Excel(name = "ui.data.column.require.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /**
     * 花纹
     */
    @Excel(name = "ui.data.column.require.pattern")
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /**
     * 层级
     */
    // @Excel(name = "ui.data.column.require.hierarchy")
    @ApiModelProperty(value = "层级", name = "hierarchy")
    @TableField(value = "HIERARCHY")
    private String hierarchy;

    /**
     * 是否EXCEL导入（0：默认不是，1：是）
     */
    // @Excel(name = "ui.data.column.require.isImport", readConverterExp = "0=：默认不是，1：是")
    @ApiModelProperty(value = "是否EXCEL导入", name = "isImport")
    @TableField(value = "IS_IMPORT")
    private Integer isImport;

    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;
    /**
     * 中间存储值，不存表
     */
    @TableField(exist = false)
    private Long qty;

    /**
     * 中间存储值，不存表
     */
    @TableField(exist = false)
    private boolean needProduct = true;

    /**
     * 是否有交期：0 否，1 是
     */
    @ApiModelProperty(value = "是否有交期", name = "isDeliveryDateDue")
    @TableField(exist = false)
    private Integer isDeliveryDateDue;

    /**
     * 导入更新的key值
     * 分厂+年+月+物料编码+库位类别+品牌+渠道
     * + 是否重要客户 + 是否必保计划 + 是否紧急订单 + 是否欠产 + 期望交期 + 是否备货
     *
     * @return
     */
    public String getImportUpdateKey() {
        String keyFormat = "%s|*|%d|*|%d|*|%s|*|%s|*|%s|*|%s|*|%d|*|%d|*|%d|*|%d|*|%s|*|%d";
        String dateStr = "";
        if (null != deliveryDateDue) {
            dateStr = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, deliveryDateDue);
        }
        return String.format(keyFormat, factoryCode, year, month, productCode, locationType, brand, channel, isImportantCustom, isEnsurePlan
                , isEmergency, isDebitPlan, dateStr, isStockUp);
    }

    /**
     * 合并，去除客户维度
     * 按分厂 + 库位 + 物料编码 + 渠道 + 品牌
     * + 是否重要客户 + 是否必保计划 + 是否紧急订单 + 是否欠产 + 期望交期
     *
     * @return
     */
    public String getMergeGroupKey() {
        String mergeKeyFormat = "%s|*|%s|*|%s|*|%s|*|%s|*|%d|*|%d|*|%d|*|%d|*|%s";
        String dateStr = "";
        if (null != deliveryDateDue) {
            dateStr = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, deliveryDateDue);
        }
        return String.format(mergeKeyFormat, factoryCode, locationType, productCode, channel, brand, isImportantCustom, isEnsurePlan, isEmergency, isDebitPlan, dateStr);
    }
}
