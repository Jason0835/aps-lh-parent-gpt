package com.zlt.aps.cd90.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import com.fasterxml.jackson.annotation.JsonFormat;


import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Date;

@Data
@ApiModel(value = "直裁排程结果", description = "直裁排程结果")
@TableName("t_cd90_schedule_result")
public class Cd90ScheduleResult extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 工厂编码 */
    @ApiModelProperty("工厂编码")
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.cd90ScheduleResult.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;
    /** 排程日期 */
    @ApiModelProperty("排程日期")
    @TableField("SCHEDULE_DATE")
    @Excel(name = "ui.data.column.cd90ScheduleResult.scheduleDate",dateFormat = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date scheduleDate;
    /** 成型批次号 */
    @ApiModelProperty("成型批次号")
    @TableField("CX_BATCH_NO")
    @Excel(name = "ui.data.column.cd90ScheduleResult.cxBatchNo")
    private String cxBatchNo;
    /** 批次号 */
    @ApiModelProperty("批次号")
    @TableField("BATCH_NO")
    @Excel(name = "ui.data.column.cd90ScheduleResult.batchNo")
    private String batchNo;
    /** 工单号 */
    @ApiModelProperty("工单号")
    @TableField("ORDER_NO")
    @Excel(name = "ui.data.column.cd90ScheduleResult.orderNo")
    private String orderNo;
    /** 帘布大卷编号 */
    @ApiModelProperty("帘布大卷编号")
    @TableField("BIG_ROLL_CODE")
    @Excel(name = "ui.data.column.cd90ScheduleResult.bigRollCode")
    private String bigRollCode;
    /** 帘布代号 */
    @ApiModelProperty("帘布代号")
    @TableField("CLOTH_CODE")
    @Excel(name = "ui.data.column.cd90ScheduleResult.clothCode")
    private String clothCode;
    /** 单耗 */
    @ApiModelProperty("单耗")
    @TableField("UNIT_CONSUME")
    @Excel(name = "ui.data.column.cd90ScheduleResult.unitConsume")
    private Double unitConsume;
    /** 机台编码 */
    @ApiModelProperty("机台编码")
    @TableField("MACHINE_CODE")
    @Excel(name = "ui.data.column.cd90ScheduleResult.machineCode")
    private String machineCode;
    /** 库排号,多库排用逗号拼接;完整分配见t_cd90_schedule_lane_allocation */
    @ApiModelProperty("库排号,多库排用逗号拼接;完整分配见t_cd90_schedule_lane_allocation")
    @TableField("STORAGE_LANE_CODE")
    @Excel(name = "ui.data.column.cd90ScheduleResult.storageLaneCode")
    private String storageLaneCode;
//    /** 库存数量 */
//    @ApiModelProperty("库存数量")
//    @TableField("STOCK_QTY")
//    @Excel(name = "ui.data.column.cd90ScheduleResult.stockQty")
//    private Double stockQty;
    /** 供应时长 */
    @ApiModelProperty("供应时长")
    @TableField("SUPPLY_TIME")
    @Excel(name = "ui.data.column.cd90ScheduleResult.supplyTime")
    private Double supplyTime;

    // CLASS1
    @TableField("CLASS1_SCHEDULE_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date class1ScheduleDate;
    @TableField("CLASS1_CX_PLAN_QTY")
    private Double class1CxPlanQty;
    @TableField("CLASS1_PLAN_QTY")
    @Excel(name = "ui.data.column.cd90ScheduleResult.class1PlanQty")
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

    // CLASS2
    @TableField("CLASS2_SCHEDULE_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date class2ScheduleDate;
    @TableField("CLASS2_CX_PLAN_QTY")
    private Double class2CxPlanQty;
    @TableField("CLASS2_PLAN_QTY")
    @Excel(name = "ui.data.column.cd90ScheduleResult.class2PlanQty")
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

    // CLASS3
    @TableField("CLASS3_SCHEDULE_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date class3ScheduleDate;
    @TableField("CLASS3_CX_PLAN_QTY")
    private Double class3CxPlanQty;
    @TableField("CLASS3_PLAN_QTY")
    @Excel(name = "ui.data.column.cd90ScheduleResult.class3PlanQty")
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

    // CLASS4
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

    // CLASS5
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

    // CLASS6
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

    // CLASS7
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

    // CLASS8
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

    /** 发布状态 */
    @ApiModelProperty("发布状态")
    @TableField("IS_RELEASE")
    @Excel(name = "ui.data.column.cd90ScheduleResult.isRelease", dictType = "IS_RELEASE")
    private String isRelease;
    /** 收尾提示标识 */
    @ApiModelProperty("收尾提示标识")
    @TableField("MARK_CLOSE_OUT_TIP")
    private String markCloseOutTip;
    /** 生产状态 */
    @ApiModelProperty("生产状态")
    @TableField("PRODUCTION_STATUS")
    private String productionStatus;
    /** 数据来源 */
    @ApiModelProperty("数据来源")
    @TableField("DATA_SOURCE")
    private String dataSource;
    /** 是否人工锁定：0否，1是 */
    @ApiModelProperty("是否人工锁定：0否，1是")
    @TableField("IS_LOCKED")
    private Integer isLocked;
    /** 是否确认覆盖可替换旧排程，仅用于自动排程请求 */
    @ApiModelProperty("是否确认覆盖可替换旧排程")
    @TableField(exist = false)
    private Boolean forceRegenerate;
    /** 发布成功计数 */
    @ApiModelProperty("发布成功计数")
    @TableField("PUBLISH_SUCCESS_COUNT")
    private Integer publishSuccessCount;
    /** 最新发布时间 */
    @ApiModelProperty("最新发布时间")
    @TableField("NEWEST_PUBLISH_TIME")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date newestPublishTime;
    /** 收尾规格标记 */
    @ApiModelProperty("收尾规格标记")
    @TableField("CLOSE_OUT_SPEC_FLAG")
    private String closeOutSpecFlag;
    /** 颜色标识 */
    @ApiModelProperty("颜色标识")
    @TableField("COLOR_TAG")
    private String colorTag;

    /**
     * 按CLASS字段模板动态读取班次值。
     *
     * @param fieldName Java字段名
     * @return 字段值
     */
    public Serializable getFieldValueByFieldName(String fieldName) {
        try {
            Field field = this.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (Serializable) field.get(this);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("直裁排程结果字段不存在: " + fieldName, exception);
        }
    }

    /**
     * 按CLASS字段模板动态写入班次值。
     *
     * @param fieldName Java字段名
     * @param value 字段值
     */
    public void setFieldValueByFieldName(String fieldName, Object value) {
        try {
            Field field = this.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(this, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("直裁排程结果字段不存在: " + fieldName, exception);
        }
    }
}
