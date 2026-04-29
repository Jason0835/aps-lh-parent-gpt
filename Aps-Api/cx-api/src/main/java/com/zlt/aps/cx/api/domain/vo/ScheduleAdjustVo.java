package com.zlt.aps.cx.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 调量请求VO
 *
 * @author APS Team
 */
@Data
@ApiModel(value = "调量请求对象")
public class ScheduleAdjustVo {

    @ApiModelProperty(value = "排程记录ID", required = true)
    private Long id;

    // ==================== 计划量 ====================

    @ApiModelProperty(value = "一班计划量（早班D1=T日）")
    private BigDecimal class1PlanQty;

    @ApiModelProperty(value = "二班计划量（中班D1=T日）")
    private BigDecimal class2PlanQty;

    @ApiModelProperty(value = "三班计划量（夜班D2=T+1日）")
    private BigDecimal class3PlanQty;

    @ApiModelProperty(value = "四班计划量（早班D2=T+1日）")
    private BigDecimal class4PlanQty;

    @ApiModelProperty(value = "五班计划量（中班D2=T+1日）")
    private BigDecimal class5PlanQty;

    @ApiModelProperty(value = "六班计划量（夜班D3=T+2日）")
    private BigDecimal class6PlanQty;

    @ApiModelProperty(value = "七班计划量（早班D3=T+2日）")
    private BigDecimal class7PlanQty;

    @ApiModelProperty(value = "八班计划量（中班D3=T+2日）")
    private BigDecimal class8PlanQty;

    // ==================== 完成量（校验用） ====================

    @ApiModelProperty(value = "一班完成量（校验用）")
    private BigDecimal class1FinishQty;

    @ApiModelProperty(value = "二班完成量（校验用）")
    private BigDecimal class2FinishQty;

    @ApiModelProperty(value = "三班完成量（校验用）")
    private BigDecimal class3FinishQty;

    @ApiModelProperty(value = "四班完成量（校验用）")
    private BigDecimal class4FinishQty;

    @ApiModelProperty(value = "五班完成量（校验用）")
    private BigDecimal class5FinishQty;

    @ApiModelProperty(value = "六班完成量（校验用）")
    private BigDecimal class6FinishQty;

    @ApiModelProperty(value = "七班完成量（校验用）")
    private BigDecimal class7FinishQty;

    @ApiModelProperty(value = "八班完成量（校验用）")
    private BigDecimal class8FinishQty;

    // ==================== 示方书类型 ====================

    @ApiModelProperty(value = "一班示方书类型")
    private String class1RecipeType;

    @ApiModelProperty(value = "二班示方书类型")
    private String class2RecipeType;

    @ApiModelProperty(value = "三班示方书类型")
    private String class3RecipeType;

    @ApiModelProperty(value = "四班示方书类型")
    private String class4RecipeType;

    @ApiModelProperty(value = "五班示方书类型")
    private String class5RecipeType;

    @ApiModelProperty(value = "六班示方书类型")
    private String class6RecipeType;

    @ApiModelProperty(value = "七班示方书类型")
    private String class7RecipeType;

    @ApiModelProperty(value = "八班示方书类型")
    private String class8RecipeType;

    // ==================== 示方书编号 ====================

    @ApiModelProperty(value = "一班示方书编号")
    private String class1RecipeNo;

    @ApiModelProperty(value = "二班示方书编号")
    private String class2RecipeNo;

    @ApiModelProperty(value = "三班示方书编号")
    private String class3RecipeNo;

    @ApiModelProperty(value = "四班示方书编号")
    private String class4RecipeNo;

    @ApiModelProperty(value = "五班示方书编号")
    private String class5RecipeNo;

    @ApiModelProperty(value = "六班示方书编号")
    private String class6RecipeNo;

    @ApiModelProperty(value = "七班示方书编号")
    private String class7RecipeNo;

    @ApiModelProperty(value = "八班示方书编号")
    private String class8RecipeNo;

    // ==================== 原因分析手工输入 ====================

    @ApiModelProperty(value = "一班原因分析手工输入")
    private String class1AnalysisInput;

    @ApiModelProperty(value = "二班原因分析手工输入")
    private String class2AnalysisInput;

    @ApiModelProperty(value = "三班原因分析手工输入")
    private String class3AnalysisInput;

    @ApiModelProperty(value = "四班原因分析手工输入")
    private String class4AnalysisInput;

    @ApiModelProperty(value = "五班原因分析手工输入")
    private String class5AnalysisInput;

    @ApiModelProperty(value = "六班原因分析手工输入")
    private String class6AnalysisInput;

    @ApiModelProperty(value = "七班原因分析手工输入")
    private String class7AnalysisInput;

    @ApiModelProperty(value = "八班原因分析手工输入")
    private String class8AnalysisInput;
}
