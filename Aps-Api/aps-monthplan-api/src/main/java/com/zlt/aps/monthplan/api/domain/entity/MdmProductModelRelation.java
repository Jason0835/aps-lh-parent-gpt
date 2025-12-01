package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmProductModelRelation.java
 * 描    述：SAP与模具关系对象 t_mdm_product_model_relation
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-18
 */

@ApiModel(value = "SAP与模具关系对象", description = "SAP与模具关系对象 ")
@Data
@TableName(value = "T_MDM_PRODUCT_MODEL_RELATION")
public class MdmProductModelRelation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 分厂编码
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmMoldingMachineCls.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂编码", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 物料编号
     */
    @ImportExcelValidated(required = true, maxLength = 20)
    @Excel(name = "ui.data.column.relation.productCode")
    @ApiModelProperty(value = "物料编号", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 规格描述
     */
    @Excel(name = "ui.data.column.relation.productDesc")
    @ApiModelProperty(value = "规格描述", name = "productDesc")
    @TableField(value = "PRODUCT_DESC")
    private String productDesc;

    /**
     * 规格代号
     */
    @ImportExcelValidated(required = true, maxLength = 20)
    @Excel(name = "ui.data.column.relation.specCode")
    @ApiModelProperty(value = "规格代号", name = "specCode")
    @TableField(value = "SPEC_CODE")
    private String specCode;

    /**
     * 模具号
     */
    @ImportExcelValidated(required = true, maxLength = 40)
    @Excel(name = "ui.data.column.relation.mouldCode")
    @ApiModelProperty(value = "模具号", name = "mouldCode")
    @TableField(value = "MOULD_CODE")
    private String mouldCode;

    /**
     * 规格
     */
    @Excel(name = "ui.data.column.relation.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /**
     * 花纹
     */
    @Excel(name = "ui.data.column.relation.pattern")
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /**
     * 品牌
     */
    @Excel(name = "ui.data.column.relation.brand", dictType = "biz_brand_type")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /**
     * 模具大类
     */
//    @Excel(name = "ui.data.column.relation.mouldCategory")
    @ApiModelProperty(value = "模具大类", name = "mouldCategory")
    @TableField(value = "MOULD_CATEGORY")
    private String mouldCategory;

    //    @Excel(name = "ui.data.column.docProductALevel.remark")
    @ApiModelProperty("备注")
    @TableField(exist = false)
    private String remark;

    @TableField(exist = false)
    private Integer isDelete;

    @TableField(exist = false)
    private Integer index;

    @TableField(exist = false)
    private Integer shareNum;

    public String getUnikey() {
        return getProductCode() + getMouldCode();
    }

    /**
     * 更新重复Key
     * SAP代码，规格代号，模具号
     *
     * @return
     */
    public String getUpdateGroupKey() {
        String duplicateKeyFormat = "%s|*|%s|*|%s";
        return String.format(duplicateKeyFormat, getProductCode(), getSpecCode(), getMouldCode());
    }

    /**
     * 模具
     */
    @ApiModelProperty(value = "模具", name = "mouldNo")
    @TableField(exist = false)
    private String mouldNo;

    /**
     * 成型法
     */
    @ApiModelProperty(value = "成型法", name = "mouldMethod")
    @TableField(exist = false)
    private String mouldMethod;

    /**
     * 胎胚代码
     */
    @ApiModelProperty(value = "胎胚代码", name = "embryoCode")
    @TableField(exist = false)
    private String embryoCode;

    /**
     * 寸口
     */
    @ApiModelProperty(value = "寸口", name = "proSize")
    @TableField(exist = false)
    private String proSize;
}
