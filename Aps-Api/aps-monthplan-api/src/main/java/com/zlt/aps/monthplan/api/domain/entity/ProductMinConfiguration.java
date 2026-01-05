package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ProductMinConfiguration.java
 * 描    述：最小批量对象 t_mdm_min_prod
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
@TableName(value = "T_MDM_MIN_PROD")
@ApiModel(value = "最小批量对象", description = "最小批量对象 ")
public class ProductMinConfiguration extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 物料编号
     */
    @Excel(name = "ui.data.column.productMinConfiguration.productCode")
    @ApiModelProperty(value = "物料编号", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 规格描述
     */
    @Excel(name = "ui.data.column.productMinConfiguration.productDesc")
    @ApiModelProperty(value = "规格描述", name = "productDesc")
    @TableField(value = "PRODUCT_DESC")
    private String productDesc;

    /**
     * 分厂编号
     */
    @Excel(name = "ui.data.column.productMinConfiguration.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 产品品类
     */
    @Excel(name = "ui.data.column.productMinConfiguration.productType", dictType = "biz_product_type")
    @ApiModelProperty(value = "产品品类", name = "productType")
    @TableField(value = "PRODUCT_TYPE")
    private String productType;

    /**
     * 最小批量
     */
    @Excel(name = "ui.data.column.productMinConfiguration.minQty")
    @ApiModelProperty(value = "最小批量", name = "minQty")
    @TableField(value = "MIN_QTY")
    private Integer minQty;

    /**
     * 上调控制水位
     */
    @Excel(name = "ui.data.column.productMinConfiguration.upQty")
    @ApiModelProperty(value = "上调控制水位", name = "upQty")
    @TableField(value = "UP_QTY")
    private Integer upQty;

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
     * 通配符匹配的key
     * 按分厂+产品品类
     */
    public String getWildcardKey() {
        String keyFormat = "%s|*|%s";
        return String.format(keyFormat, factoryCode, productType);
    }

}
