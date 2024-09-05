package com.zlt.aps.cx.api.domain.entity;

import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 排程给主计划的成型在产品种对象 t_factory_production_product
 * 
 * @author zlt
 * @date 2021-09-19
 */
@Data
@ApiModel(value = "排程给主计划的成型在产品种对象", description = "排程给主计划的成型在产品种对象 ")
public class CxFactoryProductionProduct extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** ID */
    @Excel(name = "ui.data.column.productingIssue.id")
    @ApiModelProperty(value = "ID")
    private Long id;

    /** 年份 */
    @Excel(name = "ui.data.column.productingIssue.year")
    @ApiModelProperty(value = "年份")
    private Long year;

    /** 生产月份(如：3) */
    @Excel(name = "ui.data.column.productingIssue.month")
    @ApiModelProperty(value = "生产月份(如：3)")
    private Long month;

    /** 物料编号 */
    @Excel(name = "ui.data.column.productingIssue.productCode")
    @ApiModelProperty(value = "物料编号")
    private String productCode;

    /** 成型机编号 */
    @Excel(name = "ui.data.column.productingIssue.machineCode")
    @ApiModelProperty(value = "成型机编号")
    private String machineCode;

    /** 分公司 */
    @Excel(name = "ui.data.column.productingIssue.companyCode")
    @ApiModelProperty(value = "分公司")
    private String companyCode;

    /** 分厂 */
    @Excel(name = "ui.data.column.productingIssue.factoryCode")
    @ApiModelProperty(value = "分厂")
    private String factoryCode;

    /** 施工号（胎胚代码） */
    @Excel(name = "ui.data.column.productingIssue.constructionCode", readConverterExp = "胎=胚代码")
    @ApiModelProperty(value = "施工号")
    private String constructionCode;

    /** 删除标识（0未删除；1已删除） */
    @ApiModelProperty(value = "施工号")
    private String delFlag;


}
