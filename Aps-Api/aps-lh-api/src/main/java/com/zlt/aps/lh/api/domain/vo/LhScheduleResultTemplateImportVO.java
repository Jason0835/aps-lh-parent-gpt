package com.zlt.aps.lh.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 硫化排程结果模板反向导入行数据。
 *
 * <p>该对象只用于承接固定模板中的一行明细数据，避免把多级表头、汇总行和横向班次列强行映射到数据库实体。</p>
 *
 * @author APS
 */
@Data
public class LhScheduleResultTemplateImportVO extends BaseEntity {



    /**
     * 分厂编号
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.factoryCode")
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 批次号（规则：LHPC+年月日+3位流水号）
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.batchNo")
    @ApiModelProperty(value = "批次号", name = "batchNo")
    @TableField(value = "BATCH_NO")
    private String batchNo;

    /**
     * 唯一工单号（规则：LHGD+年月日+3位流水号）
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.orderNo")
    @ApiModelProperty(value = "唯一工单号", name = "orderNo")
    @TableField(value = "ORDER_NO")
    private String orderNo;

    /**
     * 硫化机台编号
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.lhMachineCode")
    @ApiModelProperty(value = "硫化机台编号", name = "lhMachineCode")
    @TableField(value = "LH_MACHINE_CODE")
    private String lhMachineCode;

    /**
     * 左右模（L:左模；R:右模；LR:双模）
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.leftRightMould")
    @ApiModelProperty(value = "左右模", name = "leftRightMould")
    @TableField(value = "LEFT_RIGHT_MOULD")
    private String leftRightMould;

    /**
     * 硫化机台名称
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.lhMachineName")
    @ApiModelProperty(value = "硫化机台名称", name = "lhMachineName")
    @TableField(value = "LH_MACHINE_NAME")
    private String lhMachineName;

    /**
     * 物料编号
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.materialCode")
    @ApiModelProperty(value = "物料编号", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * 规格代码
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.specCode")
    @ApiModelProperty(value = "规格代码", name = "specCode")
    @TableField(value = "SPEC_CODE")
    private String specCode;

    /**
     * 胎胚代码
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.embryoCode")
    @ApiModelProperty(value = "胎胚代码", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /**
     * 产品结构
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.structureName")
    @ApiModelProperty(value = "产品结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /**
     * 物料描述
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /**
     * 主物料(胎胚描述)
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.mainMaterialDesc")
    @ApiModelProperty(value = "胎胚描述", name = "mainMaterialDesc")
    @TableField(value = "MAIN_MATERIAL_DESC")
    private String mainMaterialDesc;

    /**
     * 胎胚库存
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.embryoStock")
    @ApiModelProperty(value = "胎胚库存", name = "embryoStock")
    @TableField(value = "EMBRYO_STOCK")
    private Integer embryoStock;

    /**
     * 规格描述信息
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.specDesc")
    @ApiModelProperty(value = "规格描述信息", name = "specDesc")
    @TableField(value = "SPEC_DESC")
    private String specDesc;

    /**
     * 硫化时长 单位：秒
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.lhTime")
    @ApiModelProperty(value = "硫化时长（秒）", name = "lhTime")
    @TableField(value = "LH_TIME")
    private Integer lhTime;

    /**
     * 日计划数量
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.dailyPlanQty")
    @ApiModelProperty(value = "日计划数量", name = "dailyPlanQty")
    @TableField(value = "DAILY_PLAN_QTY")
    private Integer dailyPlanQty;

    /**
     * 排程日期
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.scheduleDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    /**
     * 规格结束时间
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.specEndTime")
    @ApiModelProperty(value = "规格结束时间", name = "specEndTime")
    @TableField(value = "SPEC_END_TIME")
    private Date specEndTime;

    /**
     * 生产状态:0-未生产；1-生产中；2-生产完成
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.productionStatus")
    @ApiModelProperty(value = "生产状态:0-未生产；1-生产中；2-生产完成", name = "productionStatus")
    @TableField(value = "PRODUCTION_STATUS")
    private String productionStatus;

    /**
     * 1班计划量
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class1PlanQty")
    @ApiModelProperty(value = "1班计划量", name = "class1PlanQty")
    @TableField(value = "CLASS1_PLAN_QTY")
    private Integer class1PlanQty;

    /**
     * 1班计划开始时间
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class1StartTime")
    @ApiModelProperty(value = "1班计划开始时间", name = "class1StartTime")
    @TableField(value = "CLASS1_START_TIME")
    private Date class1StartTime;

    /**
     * 1班计划结束时间
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class1EndTime")
    @ApiModelProperty(value = "1班计划结束时间", name = "class1EndTime")
    @TableField(value = "CLASS1_END_TIME")
    private Date class1EndTime;

    /**
     * 1班原因分析
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class1Analysis")
    @ApiModelProperty(value = "1班原因分析", name = "class1Analysis")
    @TableField(value = "CLASS1_ANALYSIS")
    private String class1Analysis;

    /**
     * 1班完成量
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class1FinishQty")
    @ApiModelProperty(value = "1班完成量", name = "class1FinishQty")
    @TableField(value = "CLASS1_FINISH_QTY")
    private Integer class1FinishQty;

    /**
     * 1班是否收尾 0-正常 1-收尾
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class1IsEnd", dictType = "biz_end_type")
    @ApiModelProperty(value = "1班是否收尾 0-正常 1-收尾", name = "class1IsEnd")
    @TableField(value = "CLASS1_IS_END")
    private String class1IsEnd;

    /**
     * 1班硫化示方书类型
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class1LhType", dictType = "lh_trial_status")
    @ApiModelProperty(value = "1班硫化示方书类型", name = "class1LhType")
    @TableField(value = "CLASS1_LH_TYPE")
    private String class1LhType;

    /**
     * 1班示方类型
     */
    @ApiModelProperty(value = "1班示方类型", name = "class1MouldMethod")
    @TableField(value = "CLASS1_MOULD_METHOD")
    private String class1MouldMethod;

    /**
     * 2班计划量
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class2PlanQty")
    @ApiModelProperty(value = "2班计划量", name = "class2PlanQty")
    @TableField(value = "CLASS2_PLAN_QTY")
    private Integer class2PlanQty;

    /**
     * 2班计划开始时间
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class2StartTime")
    @ApiModelProperty(value = "2班计划开始时间", name = "class2StartTime")
    @TableField(value = "CLASS2_START_TIME")
    private Date class2StartTime;

    /**
     * 2班计划结束时间
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class2EndTime")
    @ApiModelProperty(value = "2班计划结束时间", name = "class2EndTime")
    @TableField(value = "CLASS2_END_TIME")
    private Date class2EndTime;

    /**
     * 2班原因分析
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class2Analysis")
    @ApiModelProperty(value = "2班原因分析", name = "class2Analysis")
    @TableField(value = "CLASS2_ANALYSIS")
    private String class2Analysis;

    /**
     * 2班完成量
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class2FinishQty")
    @ApiModelProperty(value = "2班完成量", name = "class2FinishQty")
    @TableField(value = "CLASS2_FINISH_QTY")
    private Integer class2FinishQty;

    /**
     * 2班是否收尾 0-正常 1-收尾
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class2IsEnd", dictType = "biz_end_type")
    @ApiModelProperty(value = "2班是否收尾 0-正常 1-收尾", name = "class2IsEnd")
    @TableField(value = "CLASS2_IS_END")
    private String class2IsEnd;

    /**
     * 2班硫化示方书类型
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class2LhType", dictType = "lh_trial_status")
    @ApiModelProperty(value = "2班硫化示方书类型", name = "class2LhType")
    @TableField(value = "CLASS2_LH_TYPE")
    private String class2LhType;

    /**
     * 2班示方类型
     */
    @ApiModelProperty(value = "2班示方类型", name = "class2MouldMethod")
    @TableField(value = "CLASS2_MOULD_METHOD")
    private String class2MouldMethod;

    /**
     * 3班计划量
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class3PlanQty")
    @ApiModelProperty(value = "3班计划量", name = "class3PlanQty")
    @TableField(value = "CLASS3_PLAN_QTY")
    private Integer class3PlanQty;

    /**
     * 3班计划开始时间
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class3StartTime")
    @ApiModelProperty(value = "3班计划开始时间", name = "class3StartTime")
    @TableField(value = "CLASS3_START_TIME")
    private Date class3StartTime;

    /**
     * 3班计划结束时间
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class3EndTime")
    @ApiModelProperty(value = "3班计划结束时间", name = "class3EndTime")
    @TableField(value = "CLASS3_END_TIME")
    private Date class3EndTime;

    /**
     * 3班原因分析
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class3Analysis")
    @ApiModelProperty(value = "3班原因分析", name = "class3Analysis")
    @TableField(value = "CLASS3_ANALYSIS")
    private String class3Analysis;

    /**
     * 3班完成量
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class3FinishQty")
    @ApiModelProperty(value = "3班完成量", name = "class3FinishQty")
    @TableField(value = "CLASS3_FINISH_QTY")
    private Integer class3FinishQty;

    /**
     * 3班是否收尾 0-正常 1-收尾
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class3IsEnd", dictType = "biz_end_type")
    @ApiModelProperty(value = "3班是否收尾 0-正常 1-收尾", name = "class3IsEnd")
    @TableField(value = "CLASS3_IS_END")
    private String class3IsEnd;

    /**
     * 3班硫化示方书类型
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class3LhType", dictType = "lh_trial_status")
    @ApiModelProperty(value = "3班硫化示方书类型", name = "class3LhType")
    @TableField(value = "CLASS3_LH_TYPE")
    private String class3LhType;

    /**
     * 3班示方类型
     */
    @ApiModelProperty(value = "3班示方类型", name = "class3MouldMethod")
    @TableField(value = "CLASS3_MOULD_METHOD")
    private String class3MouldMethod;

    /**
     * 4班计划量
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class4PlanQty")
    @ApiModelProperty(value = "4班计划量", name = "class4PlanQty")
    @TableField(value = "CLASS4_PLAN_QTY")
    private Integer class4PlanQty;

    /**
     * 4班计划开始时间
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class4StartTime")
    @ApiModelProperty(value = "4班计划开始时间", name = "class4StartTime")
    @TableField(value = "CLASS4_START_TIME")
    private Date class4StartTime;

    /**
     * 4班计划结束时间
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class4EndTime")
    @ApiModelProperty(value = "4班计划结束时间", name = "class4EndTime")
    @TableField(value = "CLASS4_END_TIME")
    private Date class4EndTime;

    /**
     * 4班原因分析
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class4Analysis")
    @ApiModelProperty(value = "4班原因分析", name = "class4Analysis")
    @TableField(value = "CLASS4_ANALYSIS")
    private String class4Analysis;

    /**
     * 4班完成量
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class4FinishQty")
    @ApiModelProperty(value = "4班完成量", name = "class4FinishQty")
    @TableField(value = "CLASS4_FINISH_QTY")
    private Integer class4FinishQty;

    /**
     * 4班是否收尾 0-正常 1-收尾
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class4IsEnd", dictType = "biz_end_type")
    @ApiModelProperty(value = "4班是否收尾 0-正常 1-收尾", name = "class4IsEnd")
    @TableField(value = "CLASS4_IS_END")
    private String class4IsEnd;

    /**
     * 4班硫化示方书类型
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class4LhType", dictType = "lh_trial_status")
    @ApiModelProperty(value = "4班硫化示方书类型", name = "class4LhType")
    @TableField(value = "CLASS4_LH_TYPE")
    private String class4LhType;

    /**
     * 4班示方类型
     */
    @ApiModelProperty(value = "4班示方类型", name = "class4MouldMethod")
    @TableField(value = "CLASS4_MOULD_METHOD")
    private String class4MouldMethod;

    /**
     * 5班计划量
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class5PlanQty")
    @ApiModelProperty(value = "5班计划量", name = "class5PlanQty")
    @TableField(value = "CLASS5_PLAN_QTY")
    private Integer class5PlanQty;

    /**
     * 5班计划开始时间
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class5StartTime")
    @ApiModelProperty(value = "5班计划开始时间", name = "class5StartTime")
    @TableField(value = "CLASS5_START_TIME")
    private Date class5StartTime;

    /**
     * 5班计划结束时间
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class5EndTime")
    @ApiModelProperty(value = "5班计划结束时间", name = "class5EndTime")
    @TableField(value = "CLASS5_END_TIME")
    private Date class5EndTime;

    /**
     * 5班原因分析
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class5Analysis")
    @ApiModelProperty(value = "5班原因分析", name = "class5Analysis")
    @TableField(value = "CLASS5_ANALYSIS")
    private String class5Analysis;

    /**
     * 5班完成量
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class5FinishQty")
    @ApiModelProperty(value = "5班完成量", name = "class5FinishQty")
    @TableField(value = "CLASS5_FINISH_QTY")
    private Integer class5FinishQty;

    /**
     * 5班是否收尾 0-正常 1-收尾
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class5IsEnd", dictType = "biz_end_type")
    @ApiModelProperty(value = "5班是否收尾 0-正常 1-收尾", name = "class5IsEnd")
    @TableField(value = "CLASS5_IS_END")
    private String class5IsEnd;

    /**
     * 5班硫化示方书类型
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class5LhType", dictType = "lh_trial_status")
    @ApiModelProperty(value = "5班硫化示方书类型", name = "class5LhType")
    @TableField(value = "CLASS5_LH_TYPE")
    private String class5LhType;

    /**
     * 5班示方类型
     */
    @ApiModelProperty(value = "5班示方类型", name = "class5MouldMethod")
    @TableField(value = "CLASS5_MOULD_METHOD")
    private String class5MouldMethod;

    /**
     * 6班计划量
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class6PlanQty")
    @ApiModelProperty(value = "6班计划量", name = "class6PlanQty")
    @TableField(value = "CLASS6_PLAN_QTY")
    private Integer class6PlanQty;

    /**
     * 6班计划开始时间
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class6StartTime")
    @ApiModelProperty(value = "6班计划开始时间", name = "class6StartTime")
    @TableField(value = "CLASS6_START_TIME")
    private Date class6StartTime;

    /**
     * 6班计划结束时间
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class6EndTime")
    @ApiModelProperty(value = "6班计划结束时间", name = "class6EndTime")
    @TableField(value = "CLASS6_END_TIME")
    private Date class6EndTime;

    /**
     * 6班原因分析
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class6Analysis")
    @ApiModelProperty(value = "6班原因分析", name = "class6Analysis")
    @TableField(value = "CLASS6_ANALYSIS")
    private String class6Analysis;

    /**
     * 6班完成量
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class6FinishQty")
    @ApiModelProperty(value = "6班完成量", name = "class6FinishQty")
    @TableField(value = "CLASS6_FINISH_QTY")
    private Integer class6FinishQty;

    /**
     * 6班是否收尾 0-正常 1-收尾
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class6IsEnd", dictType = "biz_end_type")
    @ApiModelProperty(value = "6班是否收尾 0-正常 1-收尾", name = "class6IsEnd")
    @TableField(value = "CLASS6_IS_END")
    private String class6IsEnd;

    /**
     * 6班硫化示方书类型
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class6LhType", dictType = "lh_trial_status")
    @ApiModelProperty(value = "6班硫化示方书类型", name = "class6LhType")
    @TableField(value = "CLASS6_LH_TYPE")
    private String class6LhType;

    /**
     * 6班示方类型
     */
    @ApiModelProperty(value = "6班示方类型", name = "class6MouldMethod")
    @TableField(value = "CLASS6_MOULD_METHOD")
    private String class6MouldMethod;

    /**
     * 7班计划量
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class7PlanQty")
    @ApiModelProperty(value = "7班计划量", name = "class7PlanQty")
    @TableField(value = "CLASS7_PLAN_QTY")
    private Integer class7PlanQty;

    /**
     * 7班计划开始时间
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class7StartTime")
    @ApiModelProperty(value = "7班计划开始时间", name = "class7StartTime")
    @TableField(value = "CLASS7_START_TIME")
    private Date class7StartTime;

    /**
     * 7班计划结束时间
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class7EndTime")
    @ApiModelProperty(value = "7班计划结束时间", name = "class7EndTime")
    @TableField(value = "CLASS7_END_TIME")
    private Date class7EndTime;

    /**
     * 7班原因分析
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class7Analysis")
    @ApiModelProperty(value = "7班原因分析", name = "class7Analysis")
    @TableField(value = "CLASS7_ANALYSIS")
    private String class7Analysis;

    /**
     * 7班完成量
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class7FinishQty")
    @ApiModelProperty(value = "7班完成量", name = "class7FinishQty")
    @TableField(value = "CLASS7_FINISH_QTY")
    private Integer class7FinishQty;

    /**
     * 7班是否收尾 0-正常 1-收尾
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class7IsEnd", dictType = "biz_end_type")
    @ApiModelProperty(value = "7班是否收尾 0-正常 1-收尾", name = "class7IsEnd")
    @TableField(value = "CLASS7_IS_END")
    private String class7IsEnd;

    /**
     * 7班硫化示方书类型
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class7LhType", dictType = "lh_trial_status")
    @ApiModelProperty(value = "7班硫化示方书类型", name = "class7LhType")
    @TableField(value = "CLASS7_LH_TYPE")
    private String class7LhType;

    /**
     * 7班示方类型
     */
    @ApiModelProperty(value = "7班示方类型", name = "class7MouldMethod")
    @TableField(value = "CLASS7_MOULD_METHOD")
    private String class7MouldMethod;

    /**
     * 8班计划量
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class8PlanQty")
    @ApiModelProperty(value = "8班计划量", name = "class8PlanQty")
    @TableField(value = "CLASS8_PLAN_QTY")
    private Integer class8PlanQty;

    /**
     * 8班计划开始时间
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class8StartTime")
    @ApiModelProperty(value = "8班计划开始时间", name = "class8StartTime")
    @TableField(value = "CLASS8_START_TIME")
    private Date class8StartTime;

    /**
     * 8班计划结束时间
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class8EndTime")
    @ApiModelProperty(value = "8班计划结束时间", name = "class8EndTime")
    @TableField(value = "CLASS8_END_TIME")
    private Date class8EndTime;

    /**
     * 8班原因分析
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class8Analysis")
    @ApiModelProperty(value = "8班原因分析", name = "class8Analysis")
    @TableField(value = "CLASS8_ANALYSIS")
    private String class8Analysis;

    /**
     * 8班完成量
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class8FinishQty")
    @ApiModelProperty(value = "8班完成量", name = "class8FinishQty")
    @TableField(value = "CLASS8_FINISH_QTY")
    private Integer class8FinishQty;

    /**
     * 8班是否收尾 0-正常 1-收尾
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class8IsEnd", dictType = "biz_end_type")
    @ApiModelProperty(value = "8班是否收尾 0-正常 1-收尾", name = "class8IsEnd")
    @TableField(value = "CLASS8_IS_END")
    private String class8IsEnd;

    /**
     * 8班硫化示方书类型
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class8LhType", dictType = "lh_trial_status")
    @ApiModelProperty(value = "8班硫化示方书类型", name = "class8LhType")
    @TableField(value = "CLASS8_LH_TYPE")
    private String class8LhType;

    /**
     * 8班示方类型
     */
    @ApiModelProperty(value = "8班示方类型", name = "class8MouldMethod")
    @TableField(value = "CLASS8_MOULD_METHOD")
    private String class8MouldMethod;

    /**
     * 是否交期，0--否，1--是
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.isDelivery")
    @ApiModelProperty(value = "是否交期，0--否，1--是", name = "isDelivery")
    @TableField(value = "IS_DELIVERY")
    private String isDelivery;

    /**
     * 是否发布，0-未发布，1-已发布，2-发布失败，3-超时失败，4-待发布
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.isRelease")
    @ApiModelProperty(value = "是否发布，0-未发布，1-已发布，2-发布失败，3-超时失败，4-待发布", name = "isRelease")
    @TableField(value = "IS_RELEASE")
    private String isRelease;

    /**
     * 发布成功计数器
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.publishSuccessCount")
    @ApiModelProperty(value = "发布成功计数器", name = "publishSuccessCount")
    @TableField(value = "PUBLISH_SUCCESS_COUNT")
    private BigDecimal publishSuccessCount;

    /**
     * 保留最新的一次发布成功时间
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.newestPublishTime")
    @ApiModelProperty(value = "最新发布成功时间", name = "newestPublishTime")
    @TableField(value = "NEWEST_PUBLISH_TIME")
    private Date newestPublishTime;

    /**
     * 数据来源：0 自动排程；1 插单；2 导入
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.dataSource")
    @ApiModelProperty(value = "数据来源：0 自动排程；1 插单；2 导入", name = "dataSource")
    @TableField(value = "DATA_SOURCE")
    private String dataSource;

    /**
     * 使用模数
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.mouldQty")
    @ApiModelProperty(value = "使用模数", name = "mouldQty")
    @TableField(value = "MOULD_QTY")
    private Integer mouldQty;

    /**
     * 单班硫化量
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.singleMouldShiftQty")
    @ApiModelProperty(value = "单班硫化量", name = "singleMouldShiftQty")
    @TableField(value = "SINGLE_MOULD_SHIFT_QTY")
    private Integer singleMouldShiftQty;

    /**
     * 模具信息 JSON字符串
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.mouldInfo")
    @ApiModelProperty(value = "模具信息 JSON字符串", name = "mouldInfo")
    @TableField(value = "MOULD_INFO")
    private String mouldInfo;

    /**
     * 硫化方式
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.mouldMethod")
    @ApiModelProperty(value = "硫化方式", name = "mouldMethod")
    @TableField(value = "MOULD_METHOD")
    private String mouldMethod;

    /**
     * 示方类型（字典 lh_trial_status：S-正规示方，T-量试示方，X-试验示方）
     */
    @ApiModelProperty(value = "示方类型", name = "trialStatus")
    @TableField(value = "TRIAL_STATUS")
    private String trialStatus;

    /**
     * 制造示方书号
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.embryoNo")
    @ApiModelProperty(value = "制造示方书号", name = "embryoNo")
    @TableField(value = "EMBRYO_NO")
    private String embryoNo;

    /**
     * 文字示方书号
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.textNo")
    @ApiModelProperty(value = "文字示方书号", name = "textNo")
    @TableField(value = "TEXT_NO")
    private String textNo;

    /**
     * 硫化示方书号
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.lhNo")
    @ApiModelProperty(value = "硫化示方书号", name = "lhNo")
    @TableField(value = "LH_NO")
    private String lhNo;

    /**
     * 月计划需求版本
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.monthPlanVersion")
    @ApiModelProperty(value = "月计划需求版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 机台排序号
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.machineOrder")
    @ApiModelProperty(value = "机台排序号", name = "machineOrder")
    @TableField(value = "MACHINE_ORDER")
    private Integer machineOrder;

    /**
     * 是否试制量试
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.isTrial")
    @ApiModelProperty(value = "是否试制量试", name = "isTrial")
    @TableField(value = "IS_TRIAL")
    private String isTrial;

    /**
     * 实际排程日期
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.realScheduleDate")
    @ApiModelProperty(value = "实际排程日期", name = "realScheduleDate")
    @TableField(value = "REAL_SCHEDULE_DATE")
    private Date realScheduleDate;

    /**
     * T日规格结束时间
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.tdaySpecEndTime")
    @ApiModelProperty(value = "T日规格结束时间", name = "tdaySpecEndTime")
    @TableField(value = "TDAY_SPEC_END_TIME")
    private Date tdaySpecEndTime;

    /**
     * 是否首排
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.isFirst")
    @ApiModelProperty(value = "是否首排", name = "isFirst")
    @TableField(value = "IS_FIRST")
    private String isFirst;

    /**
     * 硫化余量
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.mouldSurplusQty")
    @ApiModelProperty(value = "硫化余量", name = "mouldSurplusQty")
    @TableField(value = "MOULD_SURPLUS_QTY")
    private Integer mouldSurplusQty;

    /**
     * 是否收尾
     */
    @ApiModelProperty(value = "是否收尾", name = "isEnd")
    @TableField(value = "IS_END")
    private String isEnd;

    /**
     * 月计划排产版本
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.productionVersion")
    @ApiModelProperty(value = "月计划排产版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 模具号 多个以逗号分隔
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.mouldCode")
    @ApiModelProperty(value = "模具号 多个以逗号分隔", name = "mouldCode")
    @TableField(value = "MOULD_CODE")
    private String mouldCode;

    /**
     * 是否拆分
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.isSplit")
    @ApiModelProperty(value = "是否拆分", name = "isSplit")
    @TableField(value = "IS_SPLIT")
    private String isSplit;

    /**
     * 排程顺序
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.scheduleOrder")
    @ApiModelProperty(value = "排程顺序", name = "scheduleOrder")
    @TableField(value = "SCHEDULE_ORDER")
    private String scheduleOrder;

    /**
     * 排程类型 01-续作 02-新增
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.scheduleType", dictType = "lh_schedule_type")
    @ApiModelProperty(value = "排程类型 01-续作 02-新增", name = "scheduleType")
    @TableField(value = "SCHEDULE_TYPE")
    private String scheduleType;

    /**
     * 是否换模
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.isChangeMould")
    @ApiModelProperty(value = "是否换模", name = "isChangeMould")
    @TableField(value = "IS_CHANGE_MOULD")
    private String isChangeMould;

    /**
     * 总计划数量
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.totalDailyPlanQty")
    @ApiModelProperty(value = "总计划数量", name = "totalDailyPlanQty")
    @TableField(value = "TOTAL_DAILY_PLAN_QTY")
    private Integer totalDailyPlanQty;


    /**
     * 是否换活字块
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.isTypeBlock")
    @ApiModelProperty(value = "是否换活字块", name = "isTypeBlock")
    @TableField(value = "IS_TYPE_BLOCK")
    private String isTypeBlock;


    /**
     * 删除标识（0未删除；1已删除）
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.isDelete")
    @ApiModelProperty(value = "删除标识（0未删除；1已删除）", name = "isDelete")
    @TableField(value = "IS_DELETE")
    private Integer isDelete;

    /**
     * 替换胶囊数左右模
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.replaceCapsuleCountLeftRight")
    @ApiModelProperty(value = "替换胶囊数左右模", name = "replaceCapsuleCountLeftRight")
    @TableField(value = "REPLACE_CAPSULE_COUNT_LEFT_RIGHT")
    private String replaceCapsuleCountLeftRight;

    /**
     * 1班顺序
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class1Order")
    @ApiModelProperty(value = "1班顺序", name = "class1Order")
    @TableField(value = "CLASS1_ORDER")
    private Integer class1Order;

    /**
     * 1班左右模
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class1LeftRightMould")
    @ApiModelProperty(value = "1班左右模", name = "class1LeftRightMould")
    @TableField(value = "CLASS1_LEFT_RIGHT_MOULD")
    private String class1LeftRightMould;

    /**
     * 1班类型
     */
    @ApiModelProperty(value = "1班类型", name = "class1Type")
    @TableField(value = "CLASS1_TYPE")
    private String class1Type;

    /**
     * 1班点
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class1Dot")
    @ApiModelProperty(value = "1班点", name = "class1Dot")
    @TableField(value = "CLASS1_DOT")
    private String class1Dot;

    /**
     * 2班顺序
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class2Order")
    @ApiModelProperty(value = "2班顺序", name = "class2Order")
    @TableField(value = "CLASS2_ORDER")
    private Integer class2Order;

    /**
     * 2班左右模
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class2LeftRightMould")
    @ApiModelProperty(value = "2班左右模", name = "class2LeftRightMould")
    @TableField(value = "CLASS2_LEFT_RIGHT_MOULD")
    private String class2LeftRightMould;

    /**
     * 2班类型
     */
    @ApiModelProperty(value = "2班类型", name = "class2Type")
    @TableField(value = "CLASS2_TYPE")
    private String class2Type;

    /**
     * 2班点
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class2Dot")
    @ApiModelProperty(value = "2班点", name = "class2Dot")
    @TableField(value = "CLASS2_DOT")
    private String class2Dot;

    /**
     * 3班顺序
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class3Order")
    @ApiModelProperty(value = "3班顺序", name = "class3Order")
    @TableField(value = "CLASS3_ORDER")
    private Integer class3Order;

    /**
     * 3班左右模
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class3LeftRightMould")
    @ApiModelProperty(value = "3班左右模", name = "class3LeftRightMould")
    @TableField(value = "CLASS3_LEFT_RIGHT_MOULD")
    private String class3LeftRightMould;

    /**
     * 3班类型
     */
    @ApiModelProperty(value = "3班类型", name = "class3Type")
    @TableField(value = "CLASS3_TYPE")
    private String class3Type;

    /**
     * 3班点
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class3Dot")
    @ApiModelProperty(value = "3班点", name = "class3Dot")
    @TableField(value = "CLASS3_DOT")
    private String class3Dot;

    /**
     * 4班顺序
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class4Order")
    @ApiModelProperty(value = "4班顺序", name = "class4Order")
    @TableField(value = "CLASS4_ORDER")
    private Integer class4Order;

    /**
     * 4班左右模
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class4LeftRightMould")
    @ApiModelProperty(value = "4班左右模", name = "class4LeftRightMould")
    @TableField(value = "CLASS4_LEFT_RIGHT_MOULD")
    private String class4LeftRightMould;

    /**
     * 4班类型
     */
    @ApiModelProperty(value = "4班类型", name = "class4Type")
    @TableField(value = "CLASS4_TYPE")
    private String class4Type;

    /**
     * 4班点
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class4Dot")
    @ApiModelProperty(value = "4班点", name = "class4Dot")
    @TableField(value = "CLASS4_DOT")
    private String class4Dot;

    /**
     * 5班顺序
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class5Order")
    @ApiModelProperty(value = "5班顺序", name = "class5Order")
    @TableField(value = "CLASS5_ORDER")
    private Integer class5Order;

    /**
     * 5班左右模
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class5LeftRightMould")
    @ApiModelProperty(value = "5班左右模", name = "class5LeftRightMould")
    @TableField(value = "CLASS5_LEFT_RIGHT_MOULD")
    private String class5LeftRightMould;

    /**
     * 5班类型
     */
    @ApiModelProperty(value = "5班类型", name = "class5Type")
    @TableField(value = "CLASS5_TYPE")
    private String class5Type;

    /**
     * 5班点
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class5Dot")
    @ApiModelProperty(value = "5班点", name = "class5Dot")
    @TableField(value = "CLASS5_DOT")
    private String class5Dot;

    /**
     * 6班顺序
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class6Order")
    @ApiModelProperty(value = "6班顺序", name = "class6Order")
    @TableField(value = "CLASS6_ORDER")
    private Integer class6Order;

    /**
     * 6班左右模
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class6LeftRightMould")
    @ApiModelProperty(value = "6班左右模", name = "class6LeftRightMould")
    @TableField(value = "CLASS6_LEFT_RIGHT_MOULD")
    private String class6LeftRightMould;

    /**
     * 6班类型
     */
    @ApiModelProperty(value = "6班类型", name = "class6Type")
    @TableField(value = "CLASS6_TYPE")
    private String class6Type;

    /**
     * 6班点
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class6Dot")
    @ApiModelProperty(value = "6班点", name = "class6Dot")
    @TableField(value = "CLASS6_DOT")
    private String class6Dot;

    /**
     * 7班顺序
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class7Order")
    @ApiModelProperty(value = "7班顺序", name = "class7Order")
    @TableField(value = "CLASS7_ORDER")
    private Integer class7Order;

    /**
     * 7班左右模
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class7LeftRightMould")
    @ApiModelProperty(value = "7班左右模", name = "class7LeftRightMould")
    @TableField(value = "CLASS7_LEFT_RIGHT_MOULD")
    private String class7LeftRightMould;

    /**
     * 7班类型
     */
    @ApiModelProperty(value = "7班类型", name = "class7Type")
    @TableField(value = "CLASS7_TYPE")
    private String class7Type;

    /**
     * 7班点
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class7Dot")
    @ApiModelProperty(value = "7班点", name = "class7Dot")
    @TableField(value = "CLASS7_DOT")
    private String class7Dot;

    /**
     * 8班顺序
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class8Order")
    @ApiModelProperty(value = "8班顺序", name = "class8Order")
    @TableField(value = "CLASS8_ORDER")
    private Integer class8Order;

    /**
     * 8班左右模
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class8LeftRightMould")
    @ApiModelProperty(value = "8班左右模", name = "class8LeftRightMould")
    @TableField(value = "CLASS8_LEFT_RIGHT_MOULD")
    private String class8LeftRightMould;

    /**
     * 8班类型
     */
    @ApiModelProperty(value = "8班类型", name = "class8Type")
    @TableField(value = "CLASS8_TYPE")
    private String class8Type;

    /**
     * 8班点
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.class8Dot")
    @ApiModelProperty(value = "8班点", name = "class8Dot")
    @TableField(value = "CLASS8_DOT")
    private String class8Dot;

    /**
     * 夜班计划总量
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.nightPlanQtyTotal")
    @ApiModelProperty(value = "夜班计划总量", name = "nightPlanQtyTotal")
    @TableField(value = "NIGHT_PLAN_QTY_TOTAL")
    private Integer nightPlanQtyTotal;

    /**
     * 夜班完成总量
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.nightFinishQtyTotal")
    @ApiModelProperty(value = "夜班完成总量", name = "nightFinishQtyTotal")
    @TableField(value = "NIGHT_FINISH_QTY_TOTAL")
    private Integer nightFinishQtyTotal;

    /**
     * 总计划数量公式
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.totalPlanQtyFormula")
    @ApiModelProperty(value = "总计划数量公式", name = "totalPlanQtyFormula")
    @TableField(value = "TOTAL_PLAN_QTY_FORMULA")
    private String totalPlanQtyFormula;

    /**
     * CX机台编号
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.cxMachineCode")
    @ApiModelProperty(value = "CX机台编号", name = "cxMachineCode")
    @TableField(value = "CX_MACHINE_CODE")
    private String cxMachineCode;

    /**
     * 硫化产量今天夜班
     */
    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.todayNightFinishQty")
    @ApiModelProperty(value = "硫化产量今天夜班", name = "todayNightFinishQty")
    @TableField(value = "TODAY_NIGHT_FINISH_QTY")
    private BigDecimal todayNightFinishQty;


    @Excel(name = "ui.data.column.lhScheduleResultTemplateImportVO.remark")
    @ApiModelProperty(value = "备注", name = "remark")
    @TableField(value = "REMARK")
    private String remark;


    /**
     * 产品状态
     */
//    @Excel(name = "ui.data.column.lhScheduleResult.constructionStage", dictType = "biz_construction_stage")
    @ApiModelProperty(value = "产品状态 X 试验示方 T 量试示方 S 正规示方", name = "changedTrialStatus")
    @TableField(value = "CHANGED_TRIAL_STATUS")
    private String changedTrialStatus;

}
