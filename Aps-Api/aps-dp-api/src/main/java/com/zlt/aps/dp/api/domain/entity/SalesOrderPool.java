package com.zlt.aps.dp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Date;

import static com.alibaba.fastjson.util.TypeUtils.isNumber;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：SalesOrderPool.java
 * 描    述：销售订单池对象 t_mp_sales_order_pool
 *@author zlt
 *@date 2025-12-04
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@Data
@TableName(value = "T_DP_SALES_ORDER_POOL")
@ApiModel(value = "销售订单池对象", description = "销售订单池对象 ")
public class SalesOrderPool extends BaseEntity{

    private static final long serialVersionUID = 1L;

    private static final String ZERO_YEAR_WEEK = "0000";

     /** 工厂编码 默认116 */
    @Excel(name = "ui.data.column.SalesOrderPool.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编码 默认116", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 产品品类，数据字典：biz_product_type，TBR 全钢 PCR 半钢 */
    @Excel(name = "ui.data.column.SalesOrderPool.productType", dictType = "biz_product_type")
    @ApiModelProperty(value = "产品品类，数据字典：biz_product_type，TBR 全钢 PCR 半钢", name = "productType")
    @TableField(value = "PRODUCT_TYPE")
    private String productType;

    /** 订单优先级，数据字典：biz_order_type，1 高优先级 3 中优先级 5 暂缓订单 */
    @Excel(name = "ui.data.column.SalesOrderPool.orderPriority", dictType = "biz_order_type")
    @ApiModelProperty(value = "订单优先级，数据字典：biz_order_type，1 高优先级 3 中优先级 5 暂缓订单7储备", name = "orderPriority")
    @TableField(value = "ORDER_PRIORITY")
    private String orderPriority;

    /** 区域 */
    @Excel(name = "ui.data.column.SalesOrderPool.area")
    @ApiModelProperty(value = "区域", name = "area")
    @TableField(value = "AREA")
    private String area;

    /** 客户 */
    @Excel(name = "ui.data.column.SalesOrderPool.salCode")
    @ApiModelProperty(value = "客户", name = "salCode")
    @TableField(value = "SAL_CODE")
    private String salCode;
    
    /** 内外销 */
    @Excel(name = "ui.data.column.SalesOrderPool.locationType", dictType = "biz_stor_type")
    @ApiModelProperty(value = "内外销", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;

    /** 客户国别 */
    @Excel(name = "ui.data.column.SalesOrderPool.salNCode")
    @ApiModelProperty(value = "客户国别", name = "salNCode")
    @TableField(value = "SAL_N_CODE")
    private String salNCode;

    /** 目的国 */
    @Excel(name = "ui.data.column.SalesOrderPool.natCode")
    @ApiModelProperty(value = "目的国", name = "natCode")
    @TableField(value = "NAT_CODE")
    private String natCode;

    /** 品牌 */
    @Excel(name = "ui.data.column.SalesOrderPool.brand", dictType = "biz_brand_type")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /** PO号 */
    @Excel(name = "ui.data.column.SalesOrderPool.salCodePo")
    @ApiModelProperty(value = "PO号", name = "salCodePo")
    @TableField(value = "SAL_CODE_PO")
    private String salCodePo;

    /** 提报日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.SalesOrderPool.billDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "提报日期", name = "billDate")
    @TableField(value = "BILL_DATE")
    private Date billDate;

    /** NC物料编码 */
    @Excel(name = "ui.data.column.SalesOrderPool.oriMaterialCode")
    @ApiModelProperty(value = "物料编码", name = "oriMaterialCode")
    @TableField(value = "ORI_MATERIAL_CODE")
    private String oriMaterialCode;

    /** 物料描述 */
    @Excel(name = "ui.data.column.SalesOrderPool.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /** 数量 */
    @Excel(name = "ui.data.column.SalesOrderPool.ordQty")
    @ApiModelProperty(value = "数量", name = "ordQty")
    @TableField(value = "ORD_QTY")
    private BigDecimal ordQty;

    /** 年周号 */
    @Excel(name = "ui.data.column.SalesOrderPool.weekYear")
    @ApiModelProperty(value = "年周号", name = "weekYear")
    @TableField(value = "WEEK_YEAR")
    private String weekYear;

    /** 均匀性，1 是 0 否 */
//    @Excel(name = "ui.data.column.SalesOrderPool.isUniformity", dictType = "biz_yes_no")
    @ApiModelProperty(value = "均匀性，1 是 0 否", name = "isUniformity")
    @TableField(value = "IS_UNIFORMITY")
    private String isUniformity;

    /** 动平衡，1 是 0 否 */
//    @Excel(name = "ui.data.column.SalesOrderPool.isDynamicBalance", dictType = "biz_yes_no")
    @ApiModelProperty(value = "动平衡，1 是 0 否", name = "isDynamicBalance")
    @TableField(value = "IS_DYNAMIC_BALANCE")
    private String isDynamicBalance;

    /** EUDR，1 是 0 否 */
    @Excel(name = "ui.data.column.SalesOrderPool.isEudr", dictType = "biz_yes_no")
    @ApiModelProperty(value = "EUDR，1 是 0 否", name = "isEudr")
    @TableField(value = "IS_EUDR")
    private String isEudr;

    /** 发货模式，数据字典：biz_deliver_goods_type，01 分批交货 02 整单发货 */
    @Excel(name = "ui.data.column.SalesOrderPool.deliverGoodsType", dictType = "biz_deliver_goods_type")
    @ApiModelProperty(value = "发货模式，数据字典：biz_deliver_goods_type，01 分批交货 02 整单发货", name = "deliverGoodsType")
    @TableField(value = "DELIVER_GOODS_TYPE")
    private String deliverGoodsType;

    /** 供应链优先级 */
    @Excel(name = "ui.data.column.SalesOrderPool.scmPriority", dictType = "biz_scm_type")
    @ApiModelProperty(value = "供应链优先级", name = "scmPriority")
    @TableField(value = "SCM_PRIORITY")
    private String scmPriority;

    /** SCM行ID */
    @ApiModelProperty(value = "SCM行ID", name = "scmDetailId")
    @TableField(value = "SCM_DETAIL_ID")
    private Long scmDetailId;

    /** 更新时间，用于导出展示 */
    @Excel(name = "ui.data.column.SalesOrderPool.updateTime")
    @TableField(exist = false)
    private String updateTimeExport;

    /** 订单状态，0-关单，1-正常 */
    @ApiModelProperty(value = "订单状态，0-关单，1-正常", name = "orderStatus")
    @TableField(value = "ORDER_STATUS")
    private String orderStatus;

    /** 年份 */
    @TableField(exist = false)
    private Integer year;

    /** 月份 */
    @TableField(exist = false)
    private Integer month;

    /**
     * 提报日期开始时间
     */
    @ApiModelProperty(value = "提报日期开始时间", name = "billDateStartTime")
    @TableField(exist = false)
    private String billDateStartTime;

    /**
     * 提报日期结束时间
     */
    @ApiModelProperty(value = "提报日期结束时间", name = "billDateEndTime")
    @TableField(exist = false)
    private String billDateEndTime;

    // 缓存供应链优先级整数值
    @TableField(exist = false)
    private transient Integer cachedScmPriority;
    // 缓存年周号整数值（已转换格式）
    @TableField(exist = false)
    private transient Integer cachedWeekYearInt;

    public Integer getCachedScmPriority() {
        if (cachedScmPriority == null && StringUtils.isNotBlank(this.scmPriority)) {
            try {
                cachedScmPriority = Integer.parseInt(this.scmPriority.trim());
            } catch (NumberFormatException e) {
                cachedScmPriority = null;
            }
        }
        return cachedScmPriority;
    }

    public Integer getCachedWeekYearInt() {
        if (cachedWeekYearInt == null && StringUtils.isNotBlank(this.weekYear)
            && !ZERO_YEAR_WEEK.equals(this.weekYear) &&  isNumber(this.weekYear)) {
            // 原转换逻辑：substring(2) + substring(0,2)
            String transformed = this.weekYear.substring(2) + this.weekYear.substring(0, 2);
            try {
                cachedWeekYearInt = Integer.parseInt(transformed);
            } catch (NumberFormatException e) {
                cachedWeekYearInt = null;
            }
        }
        return cachedWeekYearInt;
    }

    /**
     * 以分厂+物料为维度，转换销售订单
     */
    public String getGroupKey() {
        String keyFormat = "%s|*|%s";
        return String.format(keyFormat, factoryCode, materialDesc);
    }

    /**
     * 是按年周号 + 动平衡 + 均匀性匹配的库存数
     */
    public String getStockGroupKey() {
        String keyFormat = "%s|*|%s|*|%s";
        return String.format(keyFormat, weekYear, isDynamicBalance,isUniformity);
    }

    public String getStockWithoutOrderGroupKey() {
        String keyFormat = "%s|%s|*|%s|*|%s";
        return String.format(keyFormat, oriMaterialCode,weekYear, isDynamicBalance,isUniformity);
    }
}
