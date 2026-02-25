package com.zlt.aps.mp.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 备货计划Excel实体VO类
 *
 * @author hsc
 * @since 2025/2/19
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class StockUpPlanExcelVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.mdmStockUpPlan.year")
    @ImportExcelValidated(required = true, digits = true, min = 1000, max = 9999)
    @ApiModelProperty(value = "年份", name = "year")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.mdmStockUpPlan.month")
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 12)
    @ApiModelProperty(value = "月份", name = "month")
    private Integer month;

    /**
     * 分厂
     */
    @Excel(name = "ui.data.column.stockUpPlanExcelVo.factoryCode", dictType = "biz_factory_name")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "分厂", name = "factoryCode")
    private String factoryCode;

    /**
     * 物料编码
     */
    @Excel(name = "ui.data.column.stockUpPlanExcelVo.productCode")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "物料编码", name = "productCode")
    private String productCode;

    @ApiModelProperty(value = "物料描述", name = "productDesc")
    private String productDesc;

    /**
     * 库位类别 1 内销 2 外销
     */
    @Excel(name = "ui.data.column.stockUpPlanExcelVo.locationType", dictType = "biz_stor_type")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "库位类别 1 内销 2 外销", name = "locationType")
    private Integer locationType;

    /**
     * 月销量平均值
     */
    @Excel(name = "ui.data.column.stockUpPlanExcelVo.averageValue")
    @ImportExcelValidated(required = true, digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "月销量平均值", name = "averageValue")
    private Integer averageValue;

    /**
     * 备货系数值
     */
    @Excel(name = "ui.data.column.mdmStockUpPlan.factor")
    @ImportExcelValidated(required = true, min = 0, max = 100)
    @ApiModelProperty(value = "备货系数", name = "factor")
    private BigDecimal factor;


    /**
     * 备货时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    // @Excel(name = "ui.data.column.mdmStockUpPlan.stockTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "备货时间", name = "stockTime")
    private Date stockTime;

    /**
     * 备货人
     */
    // @Excel(name = "ui.data.column.mdmStockUpPlan.stockoist")
    // @ImportExcelValidated(maxLength = 32)
    @ApiModelProperty(value = "备货人", name = "stockoist")
    private String stockoist;

    /**
     * 审核时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    // @Excel(name = "ui.data.column.mdmStockUpPlan.approveTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "审核时间", name = "approveTime")
    private Date approveTime;

    /**
     * 审核人
     */
    // @Excel(name = "ui.data.column.mdmStockUpPlan.approver")
    // @ImportExcelValidated(maxLength = 32)
    @ApiModelProperty(value = "审核人", name = "approver")
    private String approver;
}
