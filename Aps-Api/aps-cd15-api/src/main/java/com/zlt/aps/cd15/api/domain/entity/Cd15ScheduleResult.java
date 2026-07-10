package com.zlt.aps.cd15.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Date;

/**
 * 斜裁排程结果。
 */
@Data
@ApiModel(value = "斜裁排程结果", description = "斜裁排程结果")
@TableName("t_cd15_schedule_result")
public class Cd15ScheduleResult extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编码 */
    @ApiModelProperty("工厂编码")
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.cd15ScheduleResult.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;

    /** 排程日期 */
    @ApiModelProperty("排程日期")
    @TableField("SCHEDULE_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.cd15ScheduleResult.scheduleDate", dateFormat = "yyyy-MM-dd")
    private Date scheduleDate;

    /** 斜裁批次号 */
    @ApiModelProperty("斜裁批次号")
    @TableField("CD15_BATCH_NO")
    @Excel(name = "ui.data.column.cd15ScheduleResult.cd15BatchNo")
    private String cd15BatchNo;

    /** 成型批次号 */
    @ApiModelProperty("成型批次号")
    @TableField("CX_BATCH_NO")
    @Excel(name = "ui.data.column.cd15ScheduleResult.cxBatchNo")
    private String cxBatchNo;

    /** 工单号 */
    @ApiModelProperty("工单号")
    @TableField("ORDER_NO")
    @Excel(name = "ui.data.column.cd15ScheduleResult.orderNo")
    private String orderNo;

    /** 分裁组合号 */
    @ApiModelProperty("分裁组合号")
    @TableField("GROUP_NO")
    @Excel(name = "ui.data.column.cd15ScheduleResult.groupNo")
    private String groupNo;

    /** 发布状态 */
    @ApiModelProperty("发布状态")
    @TableField("RELEASE_STATUS")
    @Excel(name = "ui.data.column.cd15ScheduleResult.releaseStatus", dictType = "IS_RELEASE")
    private String releaseStatus;

    /** 生产状态 */
    @ApiModelProperty("生产状态")
    @TableField("PRODUCTION_STATUS")
    private String productionStatus;

    /** 大卷编号 */
    @ApiModelProperty("大卷编号")
    @TableField("BIG_ROLL_CODE")
    @Excel(name = "ui.data.column.cd15ScheduleResult.bigRollCode")
    private String bigRollCode;

    /** 裁断角度 */
    @ApiModelProperty("裁断角度")
    @TableField("CUTTING_ANGLE")
    @Excel(name = "ui.data.column.cd15ScheduleResult.cuttingAngle")
    private String cuttingAngle;

    /** 机台编码 */
    @ApiModelProperty("机台编码")
    @TableField("MACHINE_CODE")
    @Excel(name = "ui.data.column.cd15ScheduleResult.machineCode")
    private String machineCode;

    /** 机台名称 */
    @ApiModelProperty("机台名称")
    @TableField("MACHINE_NAME")
    @Excel(name = "ui.data.column.cd15ScheduleResult.machineName")
    private String machineName;

    /** 库排号 */
    @ApiModelProperty("库排号")
    @TableField("STORAGE_LANE_CODE")
    @Excel(name = "ui.data.column.cd15ScheduleResult.storageLaneCode")
    private String storageLaneCode;

    /** 钢带代码 */
    @ApiModelProperty("钢带代码")
    @TableField("STEEL_STRIP_CODE")
    @Excel(name = "ui.data.column.cd15ScheduleResult.steelStripCode")
    private String steelStripCode;

    /** 库存数量 */
    @ApiModelProperty("库存数量")
    @TableField("STOCK_QTY")
    @Excel(name = "ui.data.column.cd15ScheduleResult.stockQty")
    private Double stockQty;

    /** 月计划剩余量 */
    @ApiModelProperty("月计划剩余量")
    @TableField("MONTH_SURPLUS_QTY")
    @Excel(name = "ui.data.column.cd15ScheduleResult.monthSurplusQty")
    private Double monthSurplusQty;

    /** 裁断模式 */
    @ApiModelProperty("裁断模式")
    @TableField("CUT_MODE")
    private String cutMode;

    /** 数据来源 */
    @ApiModelProperty("数据来源")
    @TableField("SOURCE_TYPE")
    private String sourceType;

    /** 是否人工锁定 */
    @ApiModelProperty("是否人工锁定")
    @TableField("IS_LOCKED")
    private String isLocked;

    /** 是否确认覆盖旧排程，仅自动排程请求使用 */
    @ApiModelProperty("是否确认覆盖旧排程")
    @TableField(exist = false)
    private Boolean forceRegenerate;

    @TableField("CLASS1_SCHEDULE_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date class1ScheduleDate;
    @TableField("CLASS1_CX_PLAN_QTY")
    private Double class1CxPlanQty;
    @TableField("CLASS1_PLAN_QTY")
    private Double class1PlanQty;
    @TableField("CLASS1_FINISH_QTY")
    private Double class1FinishQty;
    @TableField("CLASS1_PRODUCE_ORDER")
    private Integer class1ProduceOrder;
    @TableField("CLASS1_FINISH_RATE")
    private Double class1FinishRate;
    @TableField("CLASS1_ANALYSIS")
    private String class1Analysis;
    @TableField("CLASS1_ANALYSIS_INPUT")
    private String class1AnalysisInput;

    @TableField("CLASS2_SCHEDULE_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date class2ScheduleDate;
    @TableField("CLASS2_CX_PLAN_QTY")
    private Double class2CxPlanQty;
    @TableField("CLASS2_PLAN_QTY")
    private Double class2PlanQty;
    @TableField("CLASS2_FINISH_QTY")
    private Double class2FinishQty;
    @TableField("CLASS2_PRODUCE_ORDER")
    private Integer class2ProduceOrder;
    @TableField("CLASS2_FINISH_RATE")
    private Double class2FinishRate;
    @TableField("CLASS2_ANALYSIS")
    private String class2Analysis;
    @TableField("CLASS2_ANALYSIS_INPUT")
    private String class2AnalysisInput;

    @TableField("CLASS3_SCHEDULE_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date class3ScheduleDate;
    @TableField("CLASS3_CX_PLAN_QTY")
    private Double class3CxPlanQty;
    @TableField("CLASS3_PLAN_QTY")
    private Double class3PlanQty;
    @TableField("CLASS3_FINISH_QTY")
    private Double class3FinishQty;
    @TableField("CLASS3_PRODUCE_ORDER")
    private Integer class3ProduceOrder;
    @TableField("CLASS3_FINISH_RATE")
    private Double class3FinishRate;
    @TableField("CLASS3_ANALYSIS")
    private String class3Analysis;
    @TableField("CLASS3_ANALYSIS_INPUT")
    private String class3AnalysisInput;

    @TableField("CLASS4_SCHEDULE_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date class4ScheduleDate;
    @TableField("CLASS4_CX_PLAN_QTY")
    private Double class4CxPlanQty;
    @TableField("CLASS4_PLAN_QTY")
    private Double class4PlanQty;
    @TableField("CLASS4_FINISH_QTY")
    private Double class4FinishQty;
    @TableField("CLASS4_PRODUCE_ORDER")
    private Integer class4ProduceOrder;
    @TableField("CLASS4_FINISH_RATE")
    private Double class4FinishRate;
    @TableField("CLASS4_ANALYSIS")
    private String class4Analysis;
    @TableField("CLASS4_ANALYSIS_INPUT")
    private String class4AnalysisInput;

    @TableField("CLASS5_SCHEDULE_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date class5ScheduleDate;
    @TableField("CLASS5_CX_PLAN_QTY")
    private Double class5CxPlanQty;
    @TableField("CLASS5_PLAN_QTY")
    private Double class5PlanQty;
    @TableField("CLASS5_FINISH_QTY")
    private Double class5FinishQty;
    @TableField("CLASS5_PRODUCE_ORDER")
    private Integer class5ProduceOrder;
    @TableField("CLASS5_FINISH_RATE")
    private Double class5FinishRate;
    @TableField("CLASS5_ANALYSIS")
    private String class5Analysis;
    @TableField("CLASS5_ANALYSIS_INPUT")
    private String class5AnalysisInput;

    @TableField("CLASS6_SCHEDULE_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date class6ScheduleDate;
    @TableField("CLASS6_CX_PLAN_QTY")
    private Double class6CxPlanQty;
    @TableField("CLASS6_PLAN_QTY")
    private Double class6PlanQty;
    @TableField("CLASS6_FINISH_QTY")
    private Double class6FinishQty;
    @TableField("CLASS6_PRODUCE_ORDER")
    private Integer class6ProduceOrder;
    @TableField("CLASS6_FINISH_RATE")
    private Double class6FinishRate;
    @TableField("CLASS6_ANALYSIS")
    private String class6Analysis;
    @TableField("CLASS6_ANALYSIS_INPUT")
    private String class6AnalysisInput;

    @TableField("CLASS7_SCHEDULE_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date class7ScheduleDate;
    @TableField("CLASS7_CX_PLAN_QTY")
    private Double class7CxPlanQty;
    @TableField("CLASS7_PLAN_QTY")
    private Double class7PlanQty;
    @TableField("CLASS7_FINISH_QTY")
    private Double class7FinishQty;
    @TableField("CLASS7_PRODUCE_ORDER")
    private Integer class7ProduceOrder;
    @TableField("CLASS7_FINISH_RATE")
    private Double class7FinishRate;
    @TableField("CLASS7_ANALYSIS")
    private String class7Analysis;
    @TableField("CLASS7_ANALYSIS_INPUT")
    private String class7AnalysisInput;

    @TableField("CLASS8_SCHEDULE_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date class8ScheduleDate;
    @TableField("CLASS8_CX_PLAN_QTY")
    private Double class8CxPlanQty;
    @TableField("CLASS8_PLAN_QTY")
    private Double class8PlanQty;
    @TableField("CLASS8_FINISH_QTY")
    private Double class8FinishQty;
    @TableField("CLASS8_PRODUCE_ORDER")
    private Integer class8ProduceOrder;
    @TableField("CLASS8_FINISH_RATE")
    private Double class8FinishRate;
    @TableField("CLASS8_ANALYSIS")
    private String class8Analysis;
    @TableField("CLASS8_ANALYSIS_INPUT")
    private String class8AnalysisInput;

    /**
     * 按 CLASS 字段模板动态读取班次值。
     *
     * @param fieldName Java 字段名
     * @return 字段值
     */
    public Serializable getFieldValueByFieldName(String fieldName) {
        try {
            Field field = this.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (Serializable) field.get(this);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("斜裁排程结果字段不存在: " + fieldName, exception);
        }
    }

    /**
     * 按 CLASS 字段模板动态写入班次值。
     *
     * @param fieldName Java 字段名
     * @param value 字段值
     */
    public void setFieldValueByFieldName(String fieldName, Object value) {
        try {
            Field field = this.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(this, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("斜裁排程结果字段不存在: " + fieldName, exception);
        }
    }
}
