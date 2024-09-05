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
 * 排程给主计划的预计超欠产对象 t_estimate_exceed_short
 * 
 * @author zlt
 * @date 2021-09-17
 */
@Data
@ApiModel(value = "排程给主计划的预计超欠产对象", description = "排程给主计划的预计超欠产对象 ")
public class CxEstimateExceedShort extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @ApiModelProperty(value = "id")
    private Long id;

    /** 年份(也是下个月计划所属年份) */
    @Excel(name = "ui.data.column.short.year")
    @ApiModelProperty(value = "年份(也是下个月计划所属年份)")
    private Long year;

    /** 计划月份（下个月计划的月份）（1,2,3,4,5,6,7,8,9,10,11,12） */
    @Excel(name = "ui.data.column.short.month", readConverterExp = "下=个月计划的月份")
    @ApiModelProperty(value = "计划月份")
    private Long month;

    /** 分公司代码：招远：8000，6厂直接写死8000 */
    @Excel(name = "ui.data.column.short.companyCode")
    @ApiModelProperty(value = "分公司代码：招远：8000，6厂直接写死8000")
    private String companyCode;

    /** 生产分厂（6厂：L6） */
    @Excel(name = "ui.data.column.short.factoryCode", readConverterExp = "6=厂：L6")
    @ApiModelProperty(value = "生产分厂")
    private String factoryCode;

    /** 物料编号（SAP品号） */
    @Excel(name = "ui.data.column.short.productCode", readConverterExp = "S=AP品号")
    @ApiModelProperty(value = "物料编号")
    private String productCode;

    /** 等级(如：A+0中的A) */
    @Excel(name = "ui.data.column.short.lv")
    @ApiModelProperty(value = "等级(如：A+0中的A)")
    private String lv;

    /** 等级代号(如：A+0中的0) */
    @Excel(name = "ui.data.column.short.levelCode")
    @ApiModelProperty(value = "等级代号(如：A+0中的0)")
    private String levelCode;

    /** 库位（如;海外营销） */
    @Excel(name = "ui.data.column.short.storTypeDesc", readConverterExp = "如=;海外营销")
    @ApiModelProperty(value = "库位")
    private String storTypeDesc;

    /** 库位代号（如：T1） */
    @Excel(name = "ui.data.column.short.storType", readConverterExp = "如=：T1")
    @ApiModelProperty(value = "库位代号")
    private String storType;

    /** 预计超欠产 */
    @Excel(name = "ui.data.column.short.expectedExcessArrears")
    @ApiModelProperty(value = "预计超欠产")
    private Long expectedExcessArrears;

    /** 品名（全钢：TBR,半钢：PCR;6厂是PCR） */
    @Excel(name = "ui.data.column.short.productName", readConverterExp = "全=钢：TBR,半钢：PCR;6厂是PCR")
    @ApiModelProperty(value = "品名")
    private String productName;

    /** 删除标识（0未删除；1已删除） */
    @ApiModelProperty(value = "品名")
    private String delFlag;


}
