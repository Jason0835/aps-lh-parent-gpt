package com.zlt.aps.tq.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 胎圈排程结果实体类（新版）
 *
 * @author APS
 */
@ApiModel(value = "胎圈排程结果对象(新)", description = "胎圈排程结果表实体对象")
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "T_TQ_SCHEDULE_RESULT")
public class TqNewScheduleResult extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 排程日期(胎圈生产第一天=D+1) */
    @Excel(name = "ui.data.column.tqNewScheduleResult.scheduleDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    /** 成型批次号 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.cxBatchNo")
    @ApiModelProperty(value = "成型批次号", name = "cxBatchNo")
    @TableField(value = "CX_BATCH_NO")
    private String cxBatchNo;

    /** 胎圈批次号 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.batchNo")
    @ApiModelProperty(value = "胎圈批次号", name = "batchNo")
    @TableField(value = "BATCH_NO")
    private String batchNo;

    /** 工单号 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.orderNo")
    @ApiModelProperty(value = "工单号", name = "orderNo")
    @TableField(value = "ORDER_NO")
    private String orderNo;

    /** 胎圈代码 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.beadCode")
    @ApiModelProperty(value = "胎圈代码", name = "beadCode")
    @TableField(value = "BEAD_CODE")
    private String beadCode;

    /** 钢丝圈代码 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.steelRingCode")
    @ApiModelProperty(value = "钢丝圈代码", name = "steelRingCode")
    @TableField(value = "STEEL_RING_CODE")
    private String steelRingCode;

    /** 三角胶口型板代码 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.triangleGlueCode")
    @ApiModelProperty(value = "三角胶口型板代码", name = "triangleGlueCode")
    @TableField(value = "TRIANGLE_GLUE_CODE")
    private String triangleGlueCode;

    /** 英寸 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.proSize")
    @ApiModelProperty(value = "英寸", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private String proSize;

    /** 机台编号 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.machineCode")
    @ApiModelProperty(value = "机台编号", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    /** 月计划剩余量 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.monthSurplusQty")
    @ApiModelProperty(value = "月计划剩余量", name = "monthSurplusQty")
    @TableField(value = "MONTH_SURPLUS_QTY")
    private Integer monthSurplusQty;

    /** 1班顺序 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.class1Sequence")
    @ApiModelProperty(value = "1班顺序", name = "class1Sequence")
    @TableField(value = "CLASS1_SEQUENCE")
    private Integer class1Sequence;

    /** 1班计划量 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.class1PlanQty")
    @ApiModelProperty(value = "1班计划量", name = "class1PlanQty")
    @TableField(value = "CLASS1_PLAN_QTY")
    private Integer class1PlanQty;

    /** 1班完成量 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.class1FinishQty")
    @ApiModelProperty(value = "1班完成量", name = "class1FinishQty")
    @TableField(value = "CLASS1_FINISH_QTY")
    private Integer class1FinishQty;

    /** 1班原因分析 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.class1Analysis")
    @ApiModelProperty(value = "1班原因分析", name = "class1Analysis")
    @TableField(value = "CLASS1_ANALYSIS")
    private String class1Analysis;

    /** 2班顺序 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.class2Sequence")
    @ApiModelProperty(value = "2班顺序", name = "class2Sequence")
    @TableField(value = "CLASS2_SEQUENCE")
    private Integer class2Sequence;

    /** 2班计划量 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.class2PlanQty")
    @ApiModelProperty(value = "2班计划量", name = "class2PlanQty")
    @TableField(value = "CLASS2_PLAN_QTY")
    private Integer class2PlanQty;

    /** 2班完成量 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.class2FinishQty")
    @ApiModelProperty(value = "2班完成量", name = "class2FinishQty")
    @TableField(value = "CLASS2_FINISH_QTY")
    private Integer class2FinishQty;

    /** 2班原因分析 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.class2Analysis")
    @ApiModelProperty(value = "2班原因分析", name = "class2Analysis")
    @TableField(value = "CLASS2_ANALYSIS")
    private String class2Analysis;

    /** 3班顺序 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.class3Sequence")
    @ApiModelProperty(value = "3班顺序", name = "class3Sequence")
    @TableField(value = "CLASS3_SEQUENCE")
    private Integer class3Sequence;

    /** 3班计划量 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.class3PlanQty")
    @ApiModelProperty(value = "3班计划量", name = "class3PlanQty")
    @TableField(value = "CLASS3_PLAN_QTY")
    private Integer class3PlanQty;

    /** 3班完成量 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.class3FinishQty")
    @ApiModelProperty(value = "3班完成量", name = "class3FinishQty")
    @TableField(value = "CLASS3_FINISH_QTY")
    private Integer class3FinishQty;

    /** 3班原因分析 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.class3Analysis")
    @ApiModelProperty(value = "3班原因分析", name = "class3Analysis")
    @TableField(value = "CLASS3_ANALYSIS")
    private String class3Analysis;

    /** 4班顺序 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.class4Sequence")
    @ApiModelProperty(value = "4班顺序", name = "class4Sequence")
    @TableField(value = "CLASS4_SEQUENCE")
    private Integer class4Sequence;

    /** 4班计划量 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.class4PlanQty")
    @ApiModelProperty(value = "4班计划量", name = "class4PlanQty")
    @TableField(value = "CLASS4_PLAN_QTY")
    private Integer class4PlanQty;

    /** 4班完成量 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.class4FinishQty")
    @ApiModelProperty(value = "4班完成量", name = "class4FinishQty")
    @TableField(value = "CLASS4_FINISH_QTY")
    private Integer class4FinishQty;

    /** 4班原因分析 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.class4Analysis")
    @ApiModelProperty(value = "4班原因分析", name = "class4Analysis")
    @TableField(value = "CLASS4_ANALYSIS")
    private String class4Analysis;

    /** 5班顺序 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.class5Sequence")
    @ApiModelProperty(value = "5班顺序", name = "class5Sequence")
    @TableField(value = "CLASS5_SEQUENCE")
    private Integer class5Sequence;

    /** 5班计划量 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.class5PlanQty")
    @ApiModelProperty(value = "5班计划量", name = "class5PlanQty")
    @TableField(value = "CLASS5_PLAN_QTY")
    private Integer class5PlanQty;

    /** 5班完成量 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.class5FinishQty")
    @ApiModelProperty(value = "5班完成量", name = "class5FinishQty")
    @TableField(value = "CLASS5_FINISH_QTY")
    private Integer class5FinishQty;

    /** 5班原因分析 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.class5Analysis")
    @ApiModelProperty(value = "5班原因分析", name = "class5Analysis")
    @TableField(value = "CLASS5_ANALYSIS")
    private String class5Analysis;

    /** 6班顺序 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.class6Sequence")
    @ApiModelProperty(value = "6班顺序", name = "class6Sequence")
    @TableField(value = "CLASS6_SEQUENCE")
    private Integer class6Sequence;

    /** 6班计划量 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.class6PlanQty")
    @ApiModelProperty(value = "6班计划量", name = "class6PlanQty")
    @TableField(value = "CLASS6_PLAN_QTY")
    private Integer class6PlanQty;

    /** 6班完成量 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.class6FinishQty")
    @ApiModelProperty(value = "6班完成量", name = "class6FinishQty")
    @TableField(value = "CLASS6_FINISH_QTY")
    private Integer class6FinishQty;

    /** 6班原因分析 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.class6Analysis")
    @ApiModelProperty(value = "6班原因分析", name = "class6Analysis")
    @TableField(value = "CLASS6_ANALYSIS")
    private String class6Analysis;

    /** 数据来源：0-自动排程，1-插单，2-导入 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.dataSource", dictType = "TQ_DATA_SOURCE")
    @ApiModelProperty(value = "数据来源：0-自动排程，1-插单，2-导入", name = "dataSource")
    @TableField(value = "DATA_SOURCE")
    private String dataSource;

    /** 是否发布 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.isRelease", dictType = "IS_RELEASE")
    @ApiModelProperty(value = "是否发布", name = "isRelease")
    @TableField(value = "IS_RELEASE")
    private String isRelease;

    /** 库存量 */
    @Excel(name = "ui.data.column.tqNewScheduleResult.stockQty")
    @ApiModelProperty(value = "库存量", name = "stockQty")
    @TableField(value = "STOCK_QTY")
    private Integer stockQty;

    /** 排程日期查询（精确匹配） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField(exist = false)
    private Date scheduleDateQuery;

    /** 机台名称（反显） */
    @TableField(exist = false)
    private String machineName;
}
