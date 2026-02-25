package com.zlt.aps.mp.api.domain.entity;

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
 * 文件名称：EstimateExceedShort.java
 * 描    述：预计超欠产对象 t_mdm_estimate_exceed_short
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-18
 */

@Data
@TableName(value = "T_MDM_ESTIMATE_EXCEED_SHORT")
@ApiModel(value = "预计超欠产对象", description = "预计超欠产对象 ")
public class EstimateExceedShort extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.estimateExceedShort.year")
    @ImportExcelValidated(required = true, digits = true, min = 1000, max = 9999)
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.estimateExceedShort.month")
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 12)
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 分厂编号
     */
    @Excel(name = "ui.data.column.estimateExceedShort.factoryCode", dictType = "biz_factory_name")
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 物料编号
     */
    @Excel(name = "ui.data.column.estimateExceedShort.productCode")
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @ApiModelProperty(value = "物料编号", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 库位类别
     */
    @Excel(name = "ui.data.column.estimateExceedShort.locationType", dictType = "biz_stor_type")
    @ImportExcelValidated(required = true, isCode = true, maxLength = 10)
    @ApiModelProperty(value = "库位类别", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;

    // /**
    //  * 等级码
    //  */
    // @Excel(name = "ui.data.column.estimateExceedShort.levelCode", dictType = "sys_quality_level")
    // @ImportExcelValidated(required = true, isCode = true, maxLength = 10)
    // @ApiModelProperty(value = "等级码", name = "levelCode")
    // @TableField(value = "LEVEL_CODE")
    // private String levelCode;

    /**
     * 品名
     */
    @ApiModelProperty(value = "品名", name = "productName")
    @TableField(value = "PRODUCT_NAME")
    private String productName;

    /**
     * 寸口（保留2位小数）
     */
    @ApiModelProperty(value = "寸口", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private BigDecimal proSize;

    /**
     * 超欠产
     */
    @Excel(name = "ui.data.column.estimateExceedShort.exceedShortQty")
    @ImportExcelValidated(required = true, number = true, maxLength = 8)
    @ApiModelProperty(value = "超欠产", name = "exceedShortQty")
    @TableField(value = "EXCEED_SHORT_QTY")
    private Integer exceedShortQty;

    @ApiModelProperty(value = "是否EXCEL导入", name = "isImport")
    @TableField(value = "IS_IMPORT")
    private Integer isImport;

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