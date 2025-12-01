package com.zlt.aps.monthplan.api.domain.entity;

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
 * 文件名称：MdmStockUpPlan.java
 * 描    述：备货计划对象 t_mdm_stock_up_plan
 *
 * @author hsc
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：hsc
 * 修改内容：...
 * @date 2025-02-18
 */

@ApiModel(value = "备货计划对象", description = "备货计划对象 ")
@Data
@TableName(value = "T_MDM_STOCK_UP_PLAN")
public class MdmStockUpPlan extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 分厂编码 - 默认AH01
     */
    @Excel(name = "ui.data.column.mdmStockUpPlan.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂编码 - 默认AH01", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.mdmStockUpPlan.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.mdmStockUpPlan.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 物料编码
     */
    @Excel(name = "ui.data.column.mdmStockUpPlan.productCode")
    @ApiModelProperty(value = "物料编码", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 物料描述
     */
    @Excel(name = "ui.data.column.mdmStockUpPlan.productDesc")
    @ApiModelProperty(value = "物料描述", name = "productDesc")
    @TableField(value = "PRODUCT_DESC")
    private String productDesc;

    /**
     * 库位类别 1 内销 2 外销
     */
    @Excel(name = "ui.data.column.mdmStockUpPlan.locationType", dictType = "biz_stor_type")
    @ApiModelProperty(value = "库位类别 1 内销 2 外销", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private Integer locationType;

    /**
     * 月销量平均方式 3：近三个月 6:：近6个月 12：近12个月 24：近24个月 36：近36个月
     */
    @Excel(name = "ui.data.column.mdmStockUpPlan.averageType", dictType = "month_range")
    @ApiModelProperty(value = "月销量平均方式 3：近三个月 6:：近6个月 12：近12个月 24：近24个月 36：近36个月", name = "averageType")
    @TableField(value = "AVERAGE_TYPE")
    private Integer averageType;

    /**
     * 月销量平均值
     */
    @Excel(name = "ui.data.column.mdmStockUpPlan.averageValue", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "月销量平均值", name = "averageValue")
    @TableField(value = "AVERAGE_VALUE")
    private Integer averageValue;

    /**
     * 备货系数值
     */
    @Excel(name = "ui.data.column.mdmStockUpPlan.factor")
    @ApiModelProperty(value = "备货系数值", name = "factor")
    @TableField(value = "FACTOR")
    private BigDecimal factor;

    /**
     * 备货量 = 月销量平均值 * 备货系数 取整
     */
    @Excel(name = "ui.data.column.mdmStockUpPlan.stockQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "备货量 = 月销量平均值 * 备货系数 取整", name = "stockQty")
    @TableField(value = "STOCK_QTY")
    private Long stockQty;

    /**
     * 备货时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    // @Excel(name = "ui.data.column.mdmStockUpPlan.stockTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "备货时间", name = "stockTime")
    @TableField(value = "STOCK_TIME")
    private Date stockTime;

    /**
     * 备货人
     */
    // @Excel(name = "ui.data.column.mdmStockUpPlan.stockoist")
    @ApiModelProperty(value = "备货人", name = "stockoist")
    @TableField(value = "STOCKOIST")
    private String stockoist;

    /**
     * 审批时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    // @Excel(name = "ui.data.column.mdmStockUpPlan.approveTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "审批时间", name = "approveTime")
    @TableField(value = "APPROVE_TIME")
    private Date approveTime;

    /**
     * 审批人
     */
    // @Excel(name = "ui.data.column.mdmStockUpPlan.approver")
    @ApiModelProperty(value = "审批人", name = "approver")
    @TableField(value = "APPROVER")
    private String approver;

    /**
     * 创建人名称
     */
    @ApiModelProperty(value = "创建人名称", name = "createByName")
    @TableField(exist = false)
    private String createByName;
    
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
}