package com.zlt.aps.cx.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 插单请求VO
 *
 * @author APS Team
 */
@Data
@ApiModel(value = "插单请求对象")
public class ScheduleInsertVo {

    /** 分厂编码 */
    @ApiModelProperty(value = "分厂编码", required = true)
    private String factoryCode;

    @ApiModelProperty(value = "排程日期", required = true)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date scheduleDate;

    @ApiModelProperty(value = "机台编码", required = true)
    private String cxMachineCode;

    @ApiModelProperty(value = "机台名称")
    private String cxMachineName;

    @ApiModelProperty(value = "胎胚描述/物料编码", required = true)
    private String embryoCode;

    @ApiModelProperty(value = "NC物料编码", required = true)
    private String materialCode;

    @ApiModelProperty(value = "物料描述")
    private String specDesc;

    @ApiModelProperty(value = "示方版本")
    private String exampleNo;

    // ==================== 其他主表字段 ====================

    @ApiModelProperty(value = "成型批次号")
    private String cxBatchNo;

    @ApiModelProperty(value = "主物料(胎胚描述)")
    private String mainMaterialDesc;

    @ApiModelProperty(value = "结构")
    private String structureName;

    @ApiModelProperty(value = "胎胚库存")
    private BigDecimal totalStock;

    @ApiModelProperty(value = "硫化机台编号")
    private String lhMachineCode;

    @ApiModelProperty(value = "硫化机使用总模数")
    private BigDecimal lhMachineQty;

    @ApiModelProperty(value = "胎胚寸口")
    private BigDecimal specDimension;

    @ApiModelProperty(value = "成型余量")
    private BigDecimal cxRemainQty;

    @ApiModelProperty(value = "硫化余量")
    private BigDecimal lhRemainQty;

    @ApiModelProperty(value = "硫化班产")
    private BigDecimal lhClassQty;

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

    // ==================== 完成量 ====================

    @ApiModelProperty(value = "一班完成量")
    private BigDecimal class1FinishQty;

    @ApiModelProperty(value = "二班完成量")
    private BigDecimal class2FinishQty;

    @ApiModelProperty(value = "三班完成量")
    private BigDecimal class3FinishQty;

    @ApiModelProperty(value = "四班完成量")
    private BigDecimal class4FinishQty;

    @ApiModelProperty(value = "五班完成量")
    private BigDecimal class5FinishQty;

    @ApiModelProperty(value = "六班完成量")
    private BigDecimal class6FinishQty;

    @ApiModelProperty(value = "七班完成量")
    private BigDecimal class7FinishQty;

    @ApiModelProperty(value = "八班完成量")
    private BigDecimal class8FinishQty;

    // ==================== 原因分析 ====================

    @ApiModelProperty(value = "一班原因分析")
    private String class1Analysis;

    @ApiModelProperty(value = "二班原因分析")
    private String class2Analysis;

    @ApiModelProperty(value = "三班原因分析")
    private String class3Analysis;

    @ApiModelProperty(value = "四班原因分析")
    private String class4Analysis;

    @ApiModelProperty(value = "五班原因分析")
    private String class5Analysis;

    @ApiModelProperty(value = "六班原因分析")
    private String class6Analysis;

    @ApiModelProperty(value = "七班原因分析")
    private String class7Analysis;

    @ApiModelProperty(value = "八班原因分析")
    private String class8Analysis;

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
}
