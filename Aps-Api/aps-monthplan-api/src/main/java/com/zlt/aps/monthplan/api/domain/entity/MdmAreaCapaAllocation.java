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
 * 文件名称：MdmAreaCapaAllocation.java
 * 描    述：区域产能分配对象 t_mdm_area_capa_allocation
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-08
 */
@ApiModel(value = "区域产能分配对象", description = "区域产能分配对象")
@Data
@TableName(value = "T_DP_AREA_CAPACITY_CONFIG")
public class MdmAreaCapaAllocation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号，字典：biz_factory_name
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmAreaCapaAllocation.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编号，字典：biz_factory_name", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 产品品类，字典：biz_product_type
     */
    @Excel(name = "ui.data.column.mdmAreaCapaAllocation.productTypeCode", dictType = "biz_product_type")
    @ApiModelProperty(value = "产品品类，字典：biz_product_type", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 年份
     */
    @ImportExcelValidated(required = true, digits = true, min = 0, max = 9999)
    @Excel(name = "ui.data.column.mdmAreaCapaAllocation.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 12)
    @Excel(name = "ui.data.column.mdmAreaCapaAllocation.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 区域
     */
//    @ImportExcelValidated(required = true, maxLength = 10)
//    @Excel(name = "ui.data.column.mdmAreaCapaAllocation.areaCode")
    @ApiModelProperty(value = "区域", name = "areaCode")
    @TableField(value = "AREA_CODE")
    private String areaCode;

    /**
     * 产能分配
     */
    @ImportExcelValidated(required = true, digits = true, min = 0, max = 999999)
    @Excel(name = "ui.data.column.mdmAreaCapaAllocation.capacityAllocation")
    @ApiModelProperty(value = "产能分配", name = "capacityAllocation")
    @TableField(value = "CAPACITY_ALLOCATION")
    private BigDecimal capacityAllocation;

    /**
     * 复制时源工厂编号，字典：biz_factory_name
     */
    @ApiModelProperty(value = "复制时源工厂编号，字典：biz_factory_name", name = "sourceFactoryCode")
    @TableField(exist = false)
    private String sourceFactoryCode;

    /**
     * 复制时源年份
     */
    @ApiModelProperty(value = "复制时源年份", name = "sourceYear")
    @TableField(exist = false)
    private Integer sourceYear;

    /**
     * 复制时源月份
     */
    @ApiModelProperty(value = "复制时源月份", name = "sourceMonth")
    @TableField(exist = false)
    private Integer sourceMonth;

    /**
     * 复制时目标工厂编号，字典：biz_factory_name
     */
    @ApiModelProperty(value = "复制时目标工厂编号，字典：biz_factory_name", name = "targetFactoryCode")
    @TableField(exist = false)
    private String targetFactoryCode;

    /**
     * 复制时目标年份
     */
    @ApiModelProperty(value = "复制时目标年份", name = "targetYear")
    @TableField(exist = false)
    private Integer targetYear;

    /**
     * 复制时目标月份
     */
    @ApiModelProperty(value = "复制时目标月份", name = "targetMonth")
    @TableField(exist = false)
    private Integer targetMonth;

    /**
     * 区域名称
     */
    @ApiModelProperty(value = "区域名称", name = "areaCodeName")
    @TableField(exist = false)
    private String areaCodeName;

    /**
     * 区域名称国际化
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmAreaCapaAllocation.areaCode")
    @ApiModelProperty(value = "区域名称国际化", name = "areaCodeName")
    @TableField(exist = false)
    private String areaCodeNameI18n;
}
