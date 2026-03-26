package com.zlt.aps.itf.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * MES成型排程结果中间表实体
 * 对应表：MES_CX_SCHEDULE_RESULT
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@TableName(value = "MES_CX_SCHEDULE_RESULT")
@ApiModel(value = "MES成型排程结果中间表实体", description = "MES成型排程结果中间表")
public class MesCxScheduleResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID", name = "id")
    @TableField(value = "ID")
    private Long id;

    /**
     * 成型批次号
     */
    @ApiModelProperty(value = "成型批次号", name = "cxBatchNo")
    @TableField(value = "CX_BATCH_NO")
    private String cxBatchNo;

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
     * 成型机台编号
     */
    @ApiModelProperty(value = "成型机台编号", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    /**
     * 成型机台名称
     */
    @ApiModelProperty(value = "成型机台名称", name = "machineName")
    @TableField(value = "MACHINE_NAME")
    private String machineName;

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
     * 可用模具数量
     */
    @ApiModelProperty(value = "可用模具数量", name = "availableMoldQty")
    @TableField(value = "AVAILABLE_MOLD_QTY")
    private BigDecimal availableMoldQty;

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
     * 物料描述
     */
    @ApiModelProperty(value = "物料描述", name = "specDesc")
    @TableField(value = "SPEC_DESC")
    private String specDesc;

    /**
     * 成型胎胚物料编码
     */
    @ApiModelProperty(value = "成型胎胚物料编码", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /**
     * 成型胎胚物料描述
     */
    @ApiModelProperty(value = "成型胎胚物料描述", name = "embryoSpecDesc")
    @TableField(value = "EMBRYO_SPEC_DESC")
    private String embryoSpecDesc;

    // ========== 一班（夜班） ==========
    /**
     * 一班顺序
     */
    @ApiModelProperty(value = "一班顺序", name = "class1PlanQtySeq")
    @TableField(value = "CLASS1_PLAN_QTY_SEQ")
    private BigDecimal class1PlanQtySeq;

    /**
     * 夜班原因分析手工输入
     */
    @ApiModelProperty(value = "夜班原因分析手工输入", name = "class1AnalysisInput")
    @TableField(value = "CLASS1_ANALYSIS_INPUT")
    private String class1AnalysisInput;

    /**
     * 夜班原因分析
     */
    @ApiModelProperty(value = "夜班原因分析", name = "class1Analysis")
    @TableField(value = "CLASS1_ANALYSIS")
    private String class1Analysis;

    /**
     * 夜班计划数
     */
    @ApiModelProperty(value = "夜班计划数", name = "class1PlanQty")
    @TableField(value = "CLASS1_PLAN_QTY")
    private BigDecimal class1PlanQty;

    /**
     * 夜班示方类型
     */
    @ApiModelProperty(value = "夜班示方类型", name = "class1ExampleType")
    @TableField(value = "CLASS1_EXAMPLE_TYPE")
    private String class1ExampleType;

    /**
     * 夜班示方号
     */
    @ApiModelProperty(value = "夜班示方号", name = "class1ExampleNo")
    @TableField(value = "CLASS1_EXAMPLE_NO")
    private String class1ExampleNo;

    // ========== 二班（早班） ==========
    /**
     * 二班顺序
     */
    @ApiModelProperty(value = "二班顺序", name = "class2PlanQtySeq")
    @TableField(value = "CLASS2_PLAN_QTY_SEQ")
    private BigDecimal class2PlanQtySeq;

    /**
     * 早班原因分析手工输入
     */
    @ApiModelProperty(value = "早班原因分析手工输入", name = "class2AnalysisInput")
    @TableField(value = "CLASS2_ANALYSIS_INPUT")
    private String class2AnalysisInput;

    /**
     * 早班原因分析
     */
    @ApiModelProperty(value = "早班原因分析", name = "class2Analysis")
    @TableField(value = "CLASS2_ANALYSIS")
    private String class2Analysis;

    /**
     * 早班计划数
     */
    @ApiModelProperty(value = "早班计划数", name = "class2PlanQty")
    @TableField(value = "CLASS2_PLAN_QTY")
    private BigDecimal class2PlanQty;

    /**
     * 早班示方类型
     */
    @ApiModelProperty(value = "早班示方类型", name = "class2ExampleType")
    @TableField(value = "CLASS2_EXAMPLE_TYPE")
    private String class2ExampleType;

    /**
     * 早班示方号
     */
    @ApiModelProperty(value = "早班示方号", name = "class2ExampleNo")
    @TableField(value = "CLASS2_EXAMPLE_NO")
    private String class2ExampleNo;

    // ========== 三班（中班） ==========
    /**
     * 三班顺序
     */
    @ApiModelProperty(value = "三班顺序", name = "class3PlanQtySeq")
    @TableField(value = "CLASS3_PLAN_QTY_SEQ")
    private BigDecimal class3PlanQtySeq;

    /**
     * 中班原因分析手工输入
     */
    @ApiModelProperty(value = "中班原因分析手工输入", name = "class3AnalysisInput")
    @TableField(value = "CLASS3_ANALYSIS_INPUT")
    private String class3AnalysisInput;

    /**
     * 中班原因分析
     */
    @ApiModelProperty(value = "中班原因分析", name = "class3Analysis")
    @TableField(value = "CLASS3_ANALYSIS")
    private String class3Analysis;

    /**
     * 中班计划数
     */
    @ApiModelProperty(value = "中班计划数", name = "class3PlanQty")
    @TableField(value = "CLASS3_PLAN_QTY")
    private BigDecimal class3PlanQty;

    /**
     * 中班示方类型
     */
    @ApiModelProperty(value = "中班示方类型", name = "class3ExampleType")
    @TableField(value = "CLASS3_EXAMPLE_TYPE")
    private String class3ExampleType;

    /**
     * 中班示方号
     */
    @ApiModelProperty(value = "中班示方号", name = "class3ExampleNo")
    @TableField(value = "CLASS3_EXAMPLE_NO")
    private String class3ExampleNo;

    /**
     * 版本号
     */
    @ApiModelProperty(value = "版本号", name = "dataVersion")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    /**
     * 分公司编码
     */
    @ApiModelProperty(value = "分公司编码", name = "companyCode")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    /**
     * 厂别
     */
    @ApiModelProperty(value = "厂别", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;
}
