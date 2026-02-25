package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmProductConstruction.java
 * 描    述：SAP与施工对照对象 t_mdm_product_construction
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-25
 */

@ApiModel(value = "SAP与施工对照对象", description = "SAP与施工对照对象 ")
@Data
@TableName(value = "T_MDM_PRODUCT_CONSTRUCTION")
public class MdmProductConstruction extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 分厂编号
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmProductConstruction.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 物料编号
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmProductConstruction.productCode")
    @ApiModelProperty(value = "物料编号", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 规格代号
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmProductConstruction.specCode")
    @ApiModelProperty(value = "规格代号", name = "specCode")
    @TableField(value = "SPEC_CODE")
    private String specCode;

    /**
     * 施工代号
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.constructionCode")
    @ApiModelProperty(value = "施工代号", name = "constructionCode")
    @TableField(value = "CONSTRUCTION_CODE")
    private String constructionCode;

    /**
     * 胎胚号
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.embryoCode")
    @ApiModelProperty(value = "胎胚号", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /**
     * 生产版本
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.productionVersion")
    @ApiModelProperty(value = "生产版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 成型法
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmProductConstruction.mouldMethod", dictType = "MACHINE_TYPE")
    @ApiModelProperty(value = "成型法", name = "mouldMethod")
    @TableField(value = "MOULD_METHOD")
    private String mouldMethod;

    /**
     * BOM版本
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.bomVersion")
    @ApiModelProperty(value = "BOM版本", name = "bomVersion")
    @TableField(value = "BOM_VERSION")
    private String bomVersion;

    /**
     * 合模压力
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.mouldClampingPressure")
    @ApiModelProperty(value = "合模压力", name = "mouldClampingPressure")
    @TableField(value = "MOULD_CLAMPING_PRESSURE")
    @ImportExcelValidated(required = true)
    private BigDecimal mouldClampingPressure;

    /**
     * 夏季机械硫化时间--单位秒
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.summerCuringTime")
    @ApiModelProperty(value = "夏季机械硫化时间", name = "curingTime")
    @TableField(value = "CURING_TIME")
    @NotNull(message = "夏季机械硫化时间")
    @ImportExcelValidated(required = true)
    private Integer curingTime;

    /**
     * 冬季机械硫化时间--单位秒
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.winterCuringTime")
    @ApiModelProperty(value = "冬季机械硫化时间", name = "curingTime2")
    @TableField(value = "CURING_TIME2")
    @NotNull(message = "冬季机械硫化时间不能空")
    @ImportExcelValidated(required = true)
    private Integer curingTime2;

    /**
     * 夏季液压硫化时间--单位秒
     */
//    @Excel(name = "ui.data.column.mdmProductConstruction.summerHydraulicPressureCuringTime")
    @ApiModelProperty(value = "夏季液压硫化时间", name = "hydraulicPressureCuringTime")
    @TableField(value = "HYDRAULIC_PRESSURE_CURING_TIME")
    @NotNull(message = "夏季液压硫化时间")
    @ImportExcelValidated(required = true)
    private Integer hydraulicPressureCuringTime;

    /**
     * 冬季液压硫化时间--单位秒
     */
//    @Excel(name = "ui.data.column.mdmProductConstruction.winterHydraulicPressureCuringTime")
    @ApiModelProperty(value = "冬季液压硫化时间", name = "hydraulicPressureCuringTime2")
    @TableField(value = "HYDRAULIC_PRESSURE_CURING_TIME2")
    @NotNull(message = "冬季液压硫化时间")
    @ImportExcelValidated(required = true)
    private Integer hydraulicPressureCuringTime2;

    /**
     * 模具型腔
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.moldCavity")
    @ApiModelProperty(value = "模具型腔", name = "moldCavity")
    @TableField(value = "MOLD_CAVITY")
    private String moldCavity;

    @TableField(exist = false)
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间开始时间", name = "createStartTime")
    @TableField(exist = false)
    private Date createStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间结束时间", name = "createEndTime")
    @TableField(exist = false)
    private Date createEndTime;

    /**
     * 更新重复Key
     * 生胎代码与规格代码
     *
     * @return
     */
    public String getUpdateGroupKey() {
        String duplicateKeyFormat = "%s|*|%s";
        return String.format(duplicateKeyFormat, getEmbryoCode(), getSpecCode());
    }

    /**
     * 根据月份获取对应的硫化时间
     *
     * @param month       当前月份
     * @param summerMonth 夏季月份
     * @param winterMonth 冬季月份
     * @return
     */
    public BigDecimal getRealCuringTime(Integer month, Integer summerMonth, Integer winterMonth) {
        if (null == month || null == summerMonth || null == winterMonth) {
            return null;
        }
        //取夏季硫化时间
        if (summerMonth <= month && month < winterMonth) {
            if (null == curingTime) {
                return null;
            }
            return BigDecimal.valueOf(curingTime);
        }
        //取冬季硫化时间
        if (null == curingTime2) {
            return null;
        }
        return BigDecimal.valueOf(curingTime2);
    }

    /**
     * 是否检测异常配置，字典：biz_yes_no，0-否，1-是
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "是否检测异常配置，字典：biz_yes_no，0-否，1-是", name = "isCheckAbnormal")
    private String isCheckAbnormal;
}
