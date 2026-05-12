package com.zlt.aps.cx.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "修改明细计划量请求对象")
public class ScheduleUpdateDetailPlanQtyVo {

    @ApiModelProperty(value = "明细ID", required = true)
    private Long detailId;

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

    // ==================== 库存可供硫化时长 ====================

    @ApiModelProperty(value = "一班库存可供硫化时长")
    private BigDecimal class1StockHours;

    @ApiModelProperty(value = "二班库存可供硫化时长")
    private BigDecimal class2StockHours;

    @ApiModelProperty(value = "三班库存可供硫化时长")
    private BigDecimal class3StockHours;

    @ApiModelProperty(value = "四班库存可供硫化时长")
    private BigDecimal class4StockHours;

    @ApiModelProperty(value = "五班库存可供硫化时长")
    private BigDecimal class5StockHours;

    @ApiModelProperty(value = "六班库存可供硫化时长")
    private BigDecimal class6StockHours;

    @ApiModelProperty(value = "七班库存可供硫化时长")
    private BigDecimal class7StockHours;

    @ApiModelProperty(value = "八班库存可供硫化时长")
    private BigDecimal class8StockHours;

    // ==================== 顺位 ====================

    @ApiModelProperty(value = "一班顺位")
    private Integer class1Sequence;

    @ApiModelProperty(value = "二班顺位")
    private Integer class2Sequence;

    @ApiModelProperty(value = "三班顺位")
    private Integer class3Sequence;

    @ApiModelProperty(value = "四班顺位")
    private Integer class4Sequence;

    @ApiModelProperty(value = "五班顺位")
    private Integer class5Sequence;

    @ApiModelProperty(value = "六班顺位")
    private Integer class6Sequence;

    @ApiModelProperty(value = "七班顺位")
    private Integer class7Sequence;

    @ApiModelProperty(value = "八班顺位")
    private Integer class8Sequence;

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
