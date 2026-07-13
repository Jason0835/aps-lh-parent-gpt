package com.zlt.aps.lh.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * SKU减量清单实体
 */
@ApiModel(value = "SKU减量清单对象", description = "SKU减量清单表实体对象")
@Data
@TableName(value = "t_lh_sku_decrement")
public class LhSkuDecrement extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂 */
    @Excel(name = "ui.data.column.lhSkuDecrement.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂", name = "factoryCode")
    @TableField("FACTORY_CODE")
    private String factoryCode;

    /** 年份 */
    @Excel(name = "ui.data.column.lhSkuDecrement.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField("YEAR")
    private Integer year;

    /** 月份 */
    @Excel(name = "ui.data.column.lhSkuDecrement.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField("MONTH")
    private Integer month;

    /** 物料编码 */
    @Excel(name = "ui.data.column.lhSkuDecrement.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    /** 物料描述 */
    @Excel(name = "ui.data.column.lhSkuDecrement.materialDesc", width = 40, align = Excel.Align.LEFT)
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField("MATERIAL_DESC")
    private String materialDesc;

    /** 胎胚描述 */
    @Excel(name = "ui.data.column.lhSkuDecrement.embryoDesc", width = 40, align = Excel.Align.LEFT)
    @ApiModelProperty(value = "胎胚描述", name = "embryoDesc")
    @TableField("EMBRYO_DESC")
    private String embryoDesc;

    /** 产品状态 */
    @Excel(name = "ui.data.column.lhSkuDecrement.productStatus", dictType = "lh_trial_status")
    @ApiModelProperty(value = "产品状态", name = "productStatus")
    @TableField("PRODUCT_STATUS")
    private String productStatus;
}
