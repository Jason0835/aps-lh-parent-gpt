package com.zlt.aps.tm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 胎面排程结果表 实体类
 */
@ApiModel(value = "胎面排程结果表对象", description = "胎面排程结果表对象")
@Data
@TableName(value = "T_TM_SCHEDULE_RESULT")
public class TmScheduleResult extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 工厂编号 */
    @Excel(name = "ui.data.column.tm.scheduleResult.factoryCode")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 批次号 */
    @Excel(name = "ui.data.column.tm.scheduleResult.batchNo")
    @ApiModelProperty(value = "批次号", name = "batchNo")
    @TableField(value = "BATCH_NO")
    private String batchNo;

    /** 排程日期 */
    @Excel(name = "ui.data.column.tm.scheduleResult.scheduleDate")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    /** 机台编码 */
    @Excel(name = "ui.data.column.tm.scheduleResult.machineCode")
    @ApiModelProperty(value = "机台编码", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    /** 胎面编码 */
    @Excel(name = "ui.data.column.tm.scheduleResult.treadCode")
    @ApiModelProperty(value = "胎面编码", name = "treadCode")
    @TableField(value = "TREAD_CODE")
    private String treadCode;

    /** 主胶料编码 */
    @Excel(name = "ui.data.column.tm.scheduleResult.glueCode")
    @ApiModelProperty(value = "主胶料编码", name = "glueCode")
    @TableField(value = "GLUE_CODE")
    private String glueCode;

    /** 整条胶料组合编码 */
    @Excel(name = "ui.data.column.tm.scheduleResult.wholeGlueCode")
    @ApiModelProperty(value = "整条胶料组合编码", name = "wholeGlueCode")
    @TableField(value = "WHOLE_GLUE_CODE")
    private String wholeGlueCode;

    /** 胶料顺序 */
    @Excel(name = "ui.data.column.tm.scheduleResult.glueSeq")
    @ApiModelProperty(value = "胶料顺序", name = "glueSeq")
    @TableField(value = "GLUE_SEQ")
    private String glueSeq;

    /** 口型板编码 */
    @Excel(name = "ui.data.column.tm.scheduleResult.mouthPlateCode")
    @ApiModelProperty(value = "口型板编码", name = "mouthPlateCode")
    @TableField(value = "MOUTH_PLATE_CODE")
    private String mouthPlateCode;

    /** 1班顺序 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class1Sequence")
    @ApiModelProperty(value = "1班顺序", name = "class1Sequence")
    @TableField(value = "CLASS1_SEQUENCE")
    private Integer class1Sequence;

    /** 1班计划量 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class1PlanQty")
    @ApiModelProperty(value = "1班计划量", name = "class1PlanQty")
    @TableField(value = "CLASS1_PLAN_QTY")
    private BigDecimal class1PlanQty;

    /** 1班完成量 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class1FinishQty")
    @ApiModelProperty(value = "1班完成量", name = "class1FinishQty")
    @TableField(value = "CLASS1_FINISH_QTY")
    private BigDecimal class1FinishQty;

    /** 1班原因分析 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class1Analysis")
    @ApiModelProperty(value = "1班原因分析", name = "class1Analysis")
    @TableField(value = "CLASS1_ANALYSIS")
    private String class1Analysis;

    /** 2班顺序 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class2Sequence")
    @ApiModelProperty(value = "2班顺序", name = "class2Sequence")
    @TableField(value = "CLASS2_SEQUENCE")
    private Integer class2Sequence;

    /** 2班计划量 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class2PlanQty")
    @ApiModelProperty(value = "2班计划量", name = "class2PlanQty")
    @TableField(value = "CLASS2_PLAN_QTY")
    private BigDecimal class2PlanQty;

    /** 2班完成量 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class2FinishQty")
    @ApiModelProperty(value = "2班完成量", name = "class2FinishQty")
    @TableField(value = "CLASS2_FINISH_QTY")
    private BigDecimal class2FinishQty;

    /** 2班原因分析 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class2Analysis")
    @ApiModelProperty(value = "2班原因分析", name = "class2Analysis")
    @TableField(value = "CLASS2_ANALYSIS")
    private String class2Analysis;

    /** 3班顺序 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class3Sequence")
    @ApiModelProperty(value = "3班顺序", name = "class3Sequence")
    @TableField(value = "CLASS3_SEQUENCE")
    private Integer class3Sequence;

    /** 3班计划量 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class3PlanQty")
    @ApiModelProperty(value = "3班计划量", name = "class3PlanQty")
    @TableField(value = "CLASS3_PLAN_QTY")
    private BigDecimal class3PlanQty;

    /** 3班完成量 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class3FinishQty")
    @ApiModelProperty(value = "3班完成量", name = "class3FinishQty")
    @TableField(value = "CLASS3_FINISH_QTY")
    private BigDecimal class3FinishQty;

    /** 3班原因分析 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class3Analysis")
    @ApiModelProperty(value = "3班原因分析", name = "class3Analysis")
    @TableField(value = "CLASS3_ANALYSIS")
    private String class3Analysis;

    /** 4班顺序 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class4Sequence")
    @ApiModelProperty(value = "4班顺序", name = "class4Sequence")
    @TableField(value = "CLASS4_SEQUENCE")
    private Integer class4Sequence;

    /** 4班计划量 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class4PlanQty")
    @ApiModelProperty(value = "4班计划量", name = "class4PlanQty")
    @TableField(value = "CLASS4_PLAN_QTY")
    private BigDecimal class4PlanQty;

    /** 4班完成量 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class4FinishQty")
    @ApiModelProperty(value = "4班完成量", name = "class4FinishQty")
    @TableField(value = "CLASS4_FINISH_QTY")
    private BigDecimal class4FinishQty;

    /** 4班原因分析 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class4Analysis")
    @ApiModelProperty(value = "4班原因分析", name = "class4Analysis")
    @TableField(value = "CLASS4_ANALYSIS")
    private String class4Analysis;

    /** 5班顺序 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class5Sequence")
    @ApiModelProperty(value = "5班顺序", name = "class5Sequence")
    @TableField(value = "CLASS5_SEQUENCE")
    private Integer class5Sequence;

    /** 5班计划量 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class5PlanQty")
    @ApiModelProperty(value = "5班计划量", name = "class5PlanQty")
    @TableField(value = "CLASS5_PLAN_QTY")
    private BigDecimal class5PlanQty;

    /** 5班完成量 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class5FinishQty")
    @ApiModelProperty(value = "5班完成量", name = "class5FinishQty")
    @TableField(value = "CLASS5_FINISH_QTY")
    private BigDecimal class5FinishQty;

    /** 5班原因分析 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class5Analysis")
    @ApiModelProperty(value = "5班原因分析", name = "class5Analysis")
    @TableField(value = "CLASS5_ANALYSIS")
    private String class5Analysis;

    /** 6班顺序 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class6Sequence")
    @ApiModelProperty(value = "6班顺序", name = "class6Sequence")
    @TableField(value = "CLASS6_SEQUENCE")
    private Integer class6Sequence;

    /** 6班计划量 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class6PlanQty")
    @ApiModelProperty(value = "6班计划量", name = "class6PlanQty")
    @TableField(value = "CLASS6_PLAN_QTY")
    private BigDecimal class6PlanQty;

    /** 6班完成量 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class6FinishQty")
    @ApiModelProperty(value = "6班完成量", name = "class6FinishQty")
    @TableField(value = "CLASS6_FINISH_QTY")
    private BigDecimal class6FinishQty;

    /** 6班原因分析 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class6Analysis")
    @ApiModelProperty(value = "6班原因分析", name = "class6Analysis")
    @TableField(value = "CLASS6_ANALYSIS")
    private String class6Analysis;

    /** 发布状态 */
    @Excel(name = "ui.data.column.tm.scheduleResult.releaseStatus")
    @ApiModelProperty(value = "发布状态", name = "releaseStatus")
    @TableField(value = "RELEASE_STATUS")
    private String releaseStatus;

    /** 数据来源 */
    @Excel(name = "ui.data.column.tm.scheduleResult.dataSource")
    @ApiModelProperty(value = "数据来源", name = "dataSource")
    @TableField(value = "DATA_SOURCE")
    private String dataSource;

    /** 是否收尾任务 */
    @Excel(name = "ui.data.column.tm.scheduleResult.tailFlag", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否收尾任务", name = "tailFlag")
    @TableField(value = "TAIL_FLAG")
    private String tailFlag;
}
