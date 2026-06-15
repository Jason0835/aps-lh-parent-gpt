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
    @Excel(name = "ui.data.column.cd90ScheduleResult.scheduleDate")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
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
    /** 库排号 */
    @ApiModelProperty("库排号")
    @TableField("STORAGE_LANE_CODE")
    @Excel(name = "ui.data.column.cd90ScheduleResult.storageLaneCode")
    private String storageLaneCode;
    /** 库存数量 */
    @ApiModelProperty("库存数量")
    @TableField("STOCK_QTY")
    @Excel(name = "ui.data.column.cd90ScheduleResult.stockQty")
    private Double stockQty;
    /** 供应时长 */
    @ApiModelProperty("供应时长")
    @TableField("SUPPLY_TIME")
    @Excel(name = "ui.data.column.cd90ScheduleResult.supplyTime")
    private Double supplyTime;

    // CLASS1
    @TableField("CLASS1_SCHEDULE_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
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
    /** 发布成功计数 */
    @ApiModelProperty("发布成功计数")
    @TableField("PUBLISH_SUCCESS_COUNT")
    private Integer publishSuccessCount;
    /** 最新发布时间 */
    @ApiModelProperty("最新发布时间")
    @TableField("NEWEST_PUBLISH_TIME")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date newestPublishTime;
    /** 收尾规格标记 */
    @ApiModelProperty("收尾规格标记")
    @TableField("CLOSE_OUT_SPEC_FLAG")
    private String closeOutSpecFlag;
    /** 颜色标识 */
    @ApiModelProperty("颜色标识")
    @TableField("COLOR_TAG")
    private String colorTag;
}