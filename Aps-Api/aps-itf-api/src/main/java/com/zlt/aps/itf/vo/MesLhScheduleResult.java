package com.zlt.aps.itf.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * MES硫化排程结果中间表实体
 * 对应表：MES_LH_SCHEDULE_RESULT
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@TableName(value = "MES_LH_SCHEDULE_RESULT")
@ApiModel(value = "MES硫化排程结果中间表实体", description = "MES硫化排程结果中间表")
public class MesLhScheduleResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID", name = "id")
    @TableField(value = "ID")
    private Long id;

    /**
     * 硫化批次号
     */
    @ApiModelProperty(value = "硫化批次号", name = "lhBatchNo")
    @TableField(value = "LH_BATCH_NO")
    private String lhBatchNo;

    /**
     * 工单号
     */
    @ApiModelProperty(value = "工单号", name = "orderNo")
    @TableField(value = "ORDER_NO")
    private String orderNo;

    /**
     * 排程日期
     */
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private LocalDate scheduleDate;

    /**
     * 硫化机台编号
     */
    @ApiModelProperty(value = "硫化机台编号", name = "lhMachineCode")
    @TableField(value = "LH_MACHINE_CODE")
    private String lhMachineCode;

    /**
     * 硫化机台名称
     */
    @ApiModelProperty(value = "硫化机台名称", name = "lhMachineName")
    @TableField(value = "LH_MACHINE_NAME")
    private String lhMachineName;

    /**
     * 左右模
     */
    @ApiModelProperty(value = "左右模", name = "leftRightMold")
    @TableField(value = "LEFT_RIGHT_MOLD")
    private String leftRightMold;

    /**
     * 物料编码（NC）
     */
    @ApiModelProperty(value = "物料编码（NC）", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * 物料编码（MES）
     */
    @ApiModelProperty(value = "物料编码（MES）", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /**
     * 规格代码
     */
    @ApiModelProperty(value = "规格代码", name = "specCode")
    @TableField(value = "SPEC_CODE")
    private String specCode;

    /**
     * 规格描述
     */
    @ApiModelProperty(value = "规格描述", name = "specDesc")
    @TableField(value = "SPEC_DESC")
    private String specDesc;

    /**
     * 日计划数量
     */
    @ApiModelProperty(value = "日计划数量", name = "dailyPlanQty")
    @TableField(value = "DAILY_PLAN_QTY")
    private Integer dailyPlanQty;

    /**
     * 1班计划数量序号
     */
    @ApiModelProperty(value = "1班计划数量序号", name = "class1PlanQtySeq")
    @TableField(value = "CLASS1_PLAN_QTY_SEQ")
    private Integer class1PlanQtySeq;

    /**
     * 1班分析投入
     */
    @ApiModelProperty(value = "1班分析投入", name = "class1AnalysisInput")
    @TableField(value = "CLASS1_ANALYSIS_INPUT")
    private String class1AnalysisInput;

    /**
     * 1班分析
     */
    @ApiModelProperty(value = "1班分析", name = "class1Analysis")
    @TableField(value = "CLASS1_ANALYSIS")
    private String class1Analysis;

    /**
     * 1班计划数量
     */
    @ApiModelProperty(value = "1班计划数量", name = "class1PlanQty")
    @TableField(value = "CLASS1_PLAN_QTY")
    private Integer class1PlanQty;

    /**
     * 1班示例类型
     */
    @ApiModelProperty(value = "1班示例类型", name = "class1ExampleType")
    @TableField(value = "CLASS1_EXAMPLE_TYPE")
    private String class1ExampleType;

    /**
     * 1班示例编号
     */
    @ApiModelProperty(value = "1班示例编号", name = "class1ExampleNo")
    @TableField(value = "CLASS1_EXAMPLE_NO")
    private String class1ExampleNo;

    /**
     * 2班计划数量序号
     */
    @ApiModelProperty(value = "2班计划数量序号", name = "class2PlanQtySeq")
    @TableField(value = "CLASS2_PLAN_QTY_SEQ")
    private Integer class2PlanQtySeq;

    /**
     * 2班分析投入
     */
    @ApiModelProperty(value = "2班分析投入", name = "class2AnalysisInput")
    @TableField(value = "CLASS2_ANALYSIS_INPUT")
    private String class2AnalysisInput;

    /**
     * 2班分析
     */
    @ApiModelProperty(value = "2班分析", name = "class2Analysis")
    @TableField(value = "CLASS2_ANALYSIS")
    private String class2Analysis;

    /**
     * 2班计划数量
     */
    @ApiModelProperty(value = "2班计划数量", name = "class2PlanQty")
    @TableField(value = "CLASS2_PLAN_QTY")
    private Integer class2PlanQty;

    /**
     * 2班示例类型
     */
    @ApiModelProperty(value = "2班示例类型", name = "class2ExampleType")
    @TableField(value = "CLASS2_EXAMPLE_TYPE")
    private String class2ExampleType;

    /**
     * 2班示例编号
     */
    @ApiModelProperty(value = "2班示例编号", name = "class2ExampleNo")
    @TableField(value = "CLASS2_EXAMPLE_NO")
    private String class2ExampleNo;

    /**
     * 3班计划数量序号
     */
    @ApiModelProperty(value = "3班计划数量序号", name = "class3PlanQtySeq")
    @TableField(value = "CLASS3_PLAN_QTY_SEQ")
    private Integer class3PlanQtySeq;

    /**
     * 3班分析投入
     */
    @ApiModelProperty(value = "3班分析投入", name = "class3AnalysisInput")
    @TableField(value = "CLASS3_ANALYSIS_INPUT")
    private String class3AnalysisInput;

    /**
     * 3班分析
     */
    @ApiModelProperty(value = "3班分析", name = "class3Analysis")
    @TableField(value = "CLASS3_ANALYSIS")
    private String class3Analysis;

    /**
     * 3班计划数量
     */
    @ApiModelProperty(value = "3班计划数量", name = "class3PlanQty")
    @TableField(value = "CLASS3_PLAN_QTY")
    private Integer class3PlanQty;

    /**
     * 3班示例类型
     */
    @ApiModelProperty(value = "3班示例类型", name = "class3ExampleType")
    @TableField(value = "CLASS3_EXAMPLE_TYPE")
    private String class3ExampleType;

    /**
     * 3班示例编号
     */
    @ApiModelProperty(value = "3班示例编号", name = "class3ExampleNo")
    @TableField(value = "CLASS3_EXAMPLE_NO")
    private String class3ExampleNo;

    /**
     * 硫化时长
     */
    @ApiModelProperty(value = "硫化时长", name = "lhTime")
    @TableField(value = "LH_TIME")
    private Integer lhTime;

    /**
     * 数据版本
     */
    @ApiModelProperty(value = "数据版本", name = "dataVersion")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    /**
     * 公司代码
     */
    @ApiModelProperty(value = "公司代码", name = "companyCode")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    /**
     * 工厂代码
     */
    @ApiModelProperty(value = "工厂代码", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 预留字段1
     */
    @ApiModelProperty(value = "预留字段1", name = "reserve1")
    @TableField(value = "RESERVE1")
    private String reserve1;

    /**
     * 预留字段2
     */
    @ApiModelProperty(value = "预留字段2", name = "reserve2")
    @TableField(value = "RESERVE2")
    private String reserve2;

    /**
     * 预留字段3
     */
    @ApiModelProperty(value = "预留字段3", name = "reserve3")
    @TableField(value = "RESERVE3")
    private String reserve3;

    /**
     * 预留字段4
     */
    @ApiModelProperty(value = "预留字段4", name = "reserve4")
    @TableField(value = "RESERVE4")
    private String reserve4;

    /**
     * 预留字段5
     */
    @ApiModelProperty(value = "预留字段5", name = "reserve5")
    @TableField(value = "RESERVE5")
    private String reserve5;
}
