package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanNoticeOrder.java
 * 描    述：月计划调整通知单对象 T_MP_MONTH_PLAN_ADJUST_NOTICE
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-05-21
 */

@Data
@TableName(value = "T_MP_MONTH_PLAN_ADJUST_NOTICE")
@ApiModel(value = "月计划调整通知单对象", description = "月计划调整通知单对象")
public class MonthPlanNoticeOrder extends BaseEntity {

    private static final long serialVersionUID = 1L;
    /**
     * 通知单号
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.noticeNo")
    @ApiModelProperty(value = "调整通知单号", name = "noticeNo")
    @TableField(value = "NOTICE_NO")
    private String noticeNo;

    /**
     * 状态
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.status", dictType = "biz_adjust_status")
    @ApiModelProperty(value = "状态", name = "status")
    @TableField(value = "STATUS")
    private Integer status;
    /**
     * 生产分厂编号
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.factoryCode", dictType = "biz_factory_name")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "生产分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.year")
    @ImportExcelValidated(required = true, digits = true, min = 1000, max = 9999)
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.month")
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 12)
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 销售生产需求计划版本
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.monthPlanVersion")
    @ApiModelProperty(value = "销售生产需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 分厂版本
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.productionVersion")
    @ApiModelProperty(value = "分厂版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 生产物料编号
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.productCode")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "生产物料编号", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 生产规格描述
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.productDesc")
    @ApiModelProperty(value = "生产规格描述", name = "productDesc")
    @TableField(value = "PRODUCT_DESC")
    private String productDesc;

    /**
     * 库位类别 1 内销 2 外销 3 OE
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.locationType", dictType = "biz_stor_type")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "库位类别 1 内销 2 外销 3 OE", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;

    /**
     * 渠道
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.channel", dictType = "biz_channel_type")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "渠道", name = "channel")
    @TableField(value = "CHANNEL")
    private String channel;

    /**
     * 品牌
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.brand", dictType = "biz_brand_type")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /**
     * 寸口
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.proSize")
    @ApiModelProperty(value = "寸口", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private BigDecimal proSize;

    /**
     * 品名编码
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.productTypeCode", dictType = "biz_product_name")
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
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /**
     * 花纹
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.pattern")
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
     * 计划需求量
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.needQty")
    @ImportExcelValidated(required = true, digits = true, max = 99999999)
    @ApiModelProperty(value = "计划需求量", name = "needQty")
    @TableField(value = "NEED_QTY")
    private Long needQty;

    /**
     * 库存分配量
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.stockAllocationQty")
    @ApiModelProperty(value = "库存分配量", name = "stockAllocationQty")
    @TableField(value = "STOCK_ALLOCATION_QTY")
    private Long stockAllocationQty;

    /**
     * 计划调整量
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.planQty")
    @ApiModelProperty(value = "计划调整量", name = "planQty")
    @TableField(value = "PLAN_QTY")
    private Long planQty;

    /**
     * 实际调整量
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.productionQty")
    @ApiModelProperty(value = "实际调整量", name = "productionQty")
    @TableField(value = "PRODUCTION_QTY")
    private Long productionQty;

    /**
     * 是否EXCEL导入（0：默认不是，1：是）
     */
    @ApiModelProperty(value = "是否EXCEL导入", name = "isImport")
    @TableField(value = "IS_IMPORT")
    private Integer isImport;

    /**
     * 备注
     */
    @Excel(name = "ui.data.column.monthPlanNoticeOrder.remark")
    @ApiModelProperty(value = "备注", name = "remark")
    @TableField("REMARK")
    private String remark;
    /**
     * 判断是否重复
     * 分厂、年、月
     * SAP代码、库位、渠道
     *
     * @return
     */
    public String getImportDuplicateKey() {
        String duplicateKeyFormat = "%s|*|%s|*|%s|*|%s|*|%s|*|%s";
        return String.format(duplicateKeyFormat, getFactoryCode(), getYear(), getMonth(),
                getProductCode(), getLocationType(), getChannel());
    }
}