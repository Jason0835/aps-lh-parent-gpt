package com.zlt.aps.xwyy.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "纤维压延排程结果", description = "纤维压延排程结果")
@TableName("t_xwyy_schedule_result")
public class XwyyScheduleResult extends ApsBaseEntity {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("工厂编码")
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.xwyyScheduleResult.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;

    @ApiModelProperty("排程日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("SCHEDULE_DATE")
    @Excel(name = "ui.data.column.xwyyScheduleResult.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    private Date scheduleDate;

    @ApiModelProperty("对应的90度裁断批次号")
    @TableField("CD90_BATCH_NO")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.cd90BatchNo")
    private String cd90BatchNo;

    @ApiModelProperty("批次号，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号")
    @TableField("BATCH_NO")
    @Excel(name = "ui.data.column.xwyyScheduleResult.batchNo")
    private String batchNo;

    @ApiModelProperty("工单号，自动生成（批次号+4位定长自增序号）")
    @TableField("ORDER_NO")
    @Excel(name = "ui.data.column.xwyyScheduleResult.orderNo")
    private String orderNo;

    @ApiModelProperty("帘布大卷编号")
    @TableField("BIG_ROLL_CODE")
    @Excel(name = "ui.data.column.xwyyScheduleResult.bigRollCode")
    private String bigRollCode;

    @ApiModelProperty("原线代码")
    @TableField("ORIGINAL_LINE_CODE")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.originalLineCode")
    private String originalLineCode;

    @ApiModelProperty("机台ID，多个逗号分割")
    @TableField("MACHINE_ID")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.machineId")
    private String machineId;

    @ApiModelProperty("库存供应成型时长，单位：小时")
    @TableField("SUPPLY_TIME")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.supplyTime")
    private BigDecimal supplyTime;

    @ApiModelProperty("前日库存")
    @TableField("YES_STOCK")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.yesStock")
    private BigDecimal yesStock;

    @ApiModelProperty("当日库存")
    @TableField("TODAY_STOCK")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.todayStock")
    private BigDecimal todayStock;

    @ApiModelProperty("日用参考")
    @TableField("DAY_USED")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.dayUsed")
    private BigDecimal dayUsed;

    @ApiModelProperty("白班外厂应支")
    @TableField("DAY_OUT")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.dayOut")
    private BigDecimal dayOut;

    // CLASS1 ~ CLASS8 班次字段（每个班次8个子字段，共64字段）

    @ApiModelProperty("一班排班日期")
    @TableField("CLASS1_SCHEDULE_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class1ScheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    private Date class1ScheduleDate;

    @ApiModelProperty("一班计划量")
    @TableField("CLASS1_PLAN_QTY")
    @Excel(name = "ui.data.column.xwyyScheduleResult.class1PlanQty")
    private BigDecimal class1PlanQty;

    @ApiModelProperty("一班对应成型计划量")
    @TableField("CLASS1_CX_PLAN_QTY")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class1CxPlanQty")
    private BigDecimal class1CxPlanQty;

    @ApiModelProperty("一班完成量")
    @TableField("CLASS1_FINISH_QTY")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class1FinishQty")
    private BigDecimal class1FinishQty;

    @ApiModelProperty("一班生产顺序")
    @TableField("CLASS1_PRODUCE_ORDER")
    @Excel(name = "ui.data.column.xwyyScheduleResult.class1ProduceOrder")
    private BigDecimal class1ProduceOrder;

    @ApiModelProperty("一班完成率")
    @TableField("CLASS1_FINISH_RATE")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class1FinishRate")
    private BigDecimal class1FinishRate;

    @ApiModelProperty("一班系统原因分析")
    @TableField("CLASS1_ANALYSIS")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class1Analysis")
    private String class1Analysis;

    @ApiModelProperty("一班手工原因分析")
    @TableField("CLASS1_ANALYSIS_INPUT")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class1AnalysisInput")
    private String class1AnalysisInput;

    @ApiModelProperty("二班排班日期")
    @TableField("CLASS2_SCHEDULE_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class2ScheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    private Date class2ScheduleDate;

    @ApiModelProperty("二班计划量")
    @TableField("CLASS2_PLAN_QTY")
    @Excel(name = "ui.data.column.xwyyScheduleResult.class2PlanQty")
    private BigDecimal class2PlanQty;

    @ApiModelProperty("二班对应成型计划量")
    @TableField("CLASS2_CX_PLAN_QTY")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class2CxPlanQty")
    private BigDecimal class2CxPlanQty;

    @ApiModelProperty("二班完成量")
    @TableField("CLASS2_FINISH_QTY")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class2FinishQty")
    private BigDecimal class2FinishQty;

    @ApiModelProperty("二班生产顺序")
    @TableField("CLASS2_PRODUCE_ORDER")
    @Excel(name = "ui.data.column.xwyyScheduleResult.class2ProduceOrder")
    private BigDecimal class2ProduceOrder;

    @ApiModelProperty("二班完成率")
    @TableField("CLASS2_FINISH_RATE")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class2FinishRate")
    private BigDecimal class2FinishRate;

    @ApiModelProperty("二班系统原因分析")
    @TableField("CLASS2_ANALYSIS")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class2Analysis")
    private String class2Analysis;

    @ApiModelProperty("二班手工原因分析")
    @TableField("CLASS2_ANALYSIS_INPUT")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class2AnalysisInput")
    private String class2AnalysisInput;

    @ApiModelProperty("三班排班日期")
    @TableField("CLASS3_SCHEDULE_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class3ScheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    private Date class3ScheduleDate;

    @ApiModelProperty("三班计划量")
    @TableField("CLASS3_PLAN_QTY")
    @Excel(name = "ui.data.column.xwyyScheduleResult.class3PlanQty")
    private BigDecimal class3PlanQty;

    @ApiModelProperty("三班对应成型计划量")
    @TableField("CLASS3_CX_PLAN_QTY")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class3CxPlanQty")
    private BigDecimal class3CxPlanQty;

    @ApiModelProperty("三班完成量")
    @TableField("CLASS3_FINISH_QTY")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class3FinishQty")
    private BigDecimal class3FinishQty;

    @ApiModelProperty("三班生产顺序")
    @TableField("CLASS3_PRODUCE_ORDER")
    @Excel(name = "ui.data.column.xwyyScheduleResult.class3ProduceOrder")
    private BigDecimal class3ProduceOrder;

    @ApiModelProperty("三班完成率")
    @TableField("CLASS3_FINISH_RATE")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class3FinishRate")
    private BigDecimal class3FinishRate;

    @ApiModelProperty("三班系统原因分析")
    @TableField("CLASS3_ANALYSIS")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class3Analysis")
    private String class3Analysis;

    @ApiModelProperty("三班手工原因分析")
    @TableField("CLASS3_ANALYSIS_INPUT")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class3AnalysisInput")
    private String class3AnalysisInput;

    @ApiModelProperty("四班排班日期")
    @TableField("CLASS4_SCHEDULE_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class4ScheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    private Date class4ScheduleDate;

    @ApiModelProperty("四班计划量")
    @TableField("CLASS4_PLAN_QTY")
    @Excel(name = "ui.data.column.xwyyScheduleResult.class4PlanQty")
    private BigDecimal class4PlanQty;

    @ApiModelProperty("四班对应成型计划量")
    @TableField("CLASS4_CX_PLAN_QTY")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class4CxPlanQty")
    private BigDecimal class4CxPlanQty;

    @ApiModelProperty("四班完成量")
    @TableField("CLASS4_FINISH_QTY")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class4FinishQty")
    private BigDecimal class4FinishQty;

    @ApiModelProperty("四班生产顺序")
    @TableField("CLASS4_PRODUCE_ORDER")
    @Excel(name = "ui.data.column.xwyyScheduleResult.class4ProduceOrder")
    private BigDecimal class4ProduceOrder;

    @ApiModelProperty("四班完成率")
    @TableField("CLASS4_FINISH_RATE")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class4FinishRate")
    private BigDecimal class4FinishRate;

    @ApiModelProperty("四班系统原因分析")
    @TableField("CLASS4_ANALYSIS")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class4Analysis")
    private String class4Analysis;

    @ApiModelProperty("四班手工原因分析")
    @TableField("CLASS4_ANALYSIS_INPUT")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class4AnalysisInput")
    private String class4AnalysisInput;

    @ApiModelProperty("五班排班日期")
    @TableField("CLASS5_SCHEDULE_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class5ScheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    private Date class5ScheduleDate;

    @ApiModelProperty("五班计划量")
    @TableField("CLASS5_PLAN_QTY")
    @Excel(name = "ui.data.column.xwyyScheduleResult.class5PlanQty")
    private BigDecimal class5PlanQty;

    @ApiModelProperty("五班对应成型计划量")
    @TableField("CLASS5_CX_PLAN_QTY")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class5CxPlanQty")
    private BigDecimal class5CxPlanQty;

    @ApiModelProperty("五班完成量")
    @TableField("CLASS5_FINISH_QTY")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class5FinishQty")
    private BigDecimal class5FinishQty;

    @ApiModelProperty("五班生产顺序")
    @TableField("CLASS5_PRODUCE_ORDER")
    @Excel(name = "ui.data.column.xwyyScheduleResult.class5ProduceOrder")
    private BigDecimal class5ProduceOrder;

    @ApiModelProperty("五班完成率")
    @TableField("CLASS5_FINISH_RATE")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class5FinishRate")
    private BigDecimal class5FinishRate;

    @ApiModelProperty("五班系统原因分析")
    @TableField("CLASS5_ANALYSIS")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class5Analysis")
    private String class5Analysis;

    @ApiModelProperty("五班手工原因分析")
    @TableField("CLASS5_ANALYSIS_INPUT")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class5AnalysisInput")
    private String class5AnalysisInput;

    @ApiModelProperty("六班排班日期")
    @TableField("CLASS6_SCHEDULE_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class6ScheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    private Date class6ScheduleDate;

    @ApiModelProperty("六班计划量")
    @TableField("CLASS6_PLAN_QTY")
    @Excel(name = "ui.data.column.xwyyScheduleResult.class6PlanQty")
    private BigDecimal class6PlanQty;

    @ApiModelProperty("六班对应成型计划量")
    @TableField("CLASS6_CX_PLAN_QTY")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class6CxPlanQty")
    private BigDecimal class6CxPlanQty;

    @ApiModelProperty("六班完成量")
    @TableField("CLASS6_FINISH_QTY")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class6FinishQty")
    private BigDecimal class6FinishQty;

    @ApiModelProperty("六班生产顺序")
    @TableField("CLASS6_PRODUCE_ORDER")
    @Excel(name = "ui.data.column.xwyyScheduleResult.class6ProduceOrder")
    private BigDecimal class6ProduceOrder;

    @ApiModelProperty("六班完成率")
    @TableField("CLASS6_FINISH_RATE")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class6FinishRate")
    private BigDecimal class6FinishRate;

    @ApiModelProperty("六班系统原因分析")
    @TableField("CLASS6_ANALYSIS")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class6Analysis")
    private String class6Analysis;

    @ApiModelProperty("六班手工原因分析")
    @TableField("CLASS6_ANALYSIS_INPUT")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class6AnalysisInput")
    private String class6AnalysisInput;

    @ApiModelProperty("七班排班日期")
    @TableField("CLASS7_SCHEDULE_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class7ScheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    private Date class7ScheduleDate;

    @ApiModelProperty("七班计划量")
    @TableField("CLASS7_PLAN_QTY")
    @Excel(name = "ui.data.column.xwyyScheduleResult.class7PlanQty")
    private BigDecimal class7PlanQty;

    @ApiModelProperty("七班对应成型计划量")
    @TableField("CLASS7_CX_PLAN_QTY")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class7CxPlanQty")
    private BigDecimal class7CxPlanQty;

    @ApiModelProperty("七班完成量")
    @TableField("CLASS7_FINISH_QTY")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class7FinishQty")
    private BigDecimal class7FinishQty;

    @ApiModelProperty("七班生产顺序")
    @TableField("CLASS7_PRODUCE_ORDER")
    @Excel(name = "ui.data.column.xwyyScheduleResult.class7ProduceOrder")
    private BigDecimal class7ProduceOrder;

    @ApiModelProperty("七班完成率")
    @TableField("CLASS7_FINISH_RATE")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class7FinishRate")
    private BigDecimal class7FinishRate;

    @ApiModelProperty("七班系统原因分析")
    @TableField("CLASS7_ANALYSIS")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class7Analysis")
    private String class7Analysis;

    @ApiModelProperty("七班手工原因分析")
    @TableField("CLASS7_ANALYSIS_INPUT")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class7AnalysisInput")
    private String class7AnalysisInput;

    @ApiModelProperty("八班排班日期")
    @TableField("CLASS8_SCHEDULE_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class8ScheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    private Date class8ScheduleDate;

    @ApiModelProperty("八班计划量")
    @TableField("CLASS8_PLAN_QTY")
    @Excel(name = "ui.data.column.xwyyScheduleResult.class8PlanQty")
    private BigDecimal class8PlanQty;

    @ApiModelProperty("八班对应成型计划量")
    @TableField("CLASS8_CX_PLAN_QTY")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class8CxPlanQty")
    private BigDecimal class8CxPlanQty;

    @ApiModelProperty("八班完成量")
    @TableField("CLASS8_FINISH_QTY")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class8FinishQty")
    private BigDecimal class8FinishQty;

    @ApiModelProperty("八班生产顺序")
    @TableField("CLASS8_PRODUCE_ORDER")
    @Excel(name = "ui.data.column.xwyyScheduleResult.class8ProduceOrder")
    private BigDecimal class8ProduceOrder;

    @ApiModelProperty("八班完成率")
    @TableField("CLASS8_FINISH_RATE")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class8FinishRate")
    private BigDecimal class8FinishRate;

    @ApiModelProperty("八班系统原因分析")
    @TableField("CLASS8_ANALYSIS")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class8Analysis")
    private String class8Analysis;

    @ApiModelProperty("八班手工原因分析")
    @TableField("CLASS8_ANALYSIS_INPUT")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.class8AnalysisInput")
    private String class8AnalysisInput;

    @ApiModelProperty("发布状态，0--未发布，1--已发布，2-发布失败，3-发布中，4-超时失败，5-待发布；对应数据字典：IS_RELEASE")
    @TableField("IS_RELEASE")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.isRelease", dictType = "IS_RELEASE")
    private String isRelease;

    @ApiModelProperty("生产状态：0-未生产；1-生产中；2-生产完成")
    @TableField("PRODUCTION_STATUS")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.productionStatus", dictType = "PRODUCTION_STATUS")
    private String productionStatus;

    @ApiModelProperty("数据来源：0>自动排程；1>APS插单；2>导入")
    @TableField("DATA_SOURCE")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.dataSource", dictType = "DATA_SOURCE")
    private String dataSource;

    @ApiModelProperty("额外计划量标识：0无，1有额外计划量")
    @TableField("EXTRA_PLAN_FLAG")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.extraPlanFlag", dictType = "EXTRA_PLAN_FLAG")
    private String extraPlanFlag;

    @ApiModelProperty("发布成功计数器，每次发布成功进行累加")
    @TableField("PUBLISH_SUCCESS_COUNT")
    private Integer publishSuccessCount;

    @ApiModelProperty("最新一次发布成功时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("NEWEST_PUBLISH_TIME")
    private Date newestPublishTime;

    @ApiModelProperty("原线提醒，0：不提醒，1：提醒")
    @TableField("ORIGINAL_REMIND_FLAG")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.originalRemindFlag", dictType = "ORIGINAL_REMIND_FLAG")
    private String originalRemindFlag;

    @ApiModelProperty("原线卷数")
    @TableField("ORIGINAL_LINE_QTY_NUM")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.originalLineQtyNum")
    private BigDecimal originalLineQtyNum;

    @ApiModelProperty("原线品牌")
    @TableField("RUBBER_CODE")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.rubberCode")
    private String rubberCode;

    @ApiModelProperty("原线品牌个数")
    @TableField("RUBBER_CAR_NUMBER")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.rubberCarNumber")
    private BigDecimal rubberCarNumber;

    @ApiModelProperty("原线品牌")
    @TableField("ORIGINAL_BRAND")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.originalBrand")
    private String originalBrand;

    @ApiModelProperty("原线品牌个数")
    @TableField("ORIGINAL_BRAND_NUM")
        // @Excel(name = "ui.data.column.xwyyScheduleResult.originalBrandNum")
    private BigDecimal originalBrandNum;

    @ApiModelProperty("备注")
    @TableField("REMARK")
        // @Excel(name = "ui.common.column.remark")
    private String remark;

    // 非数据库字段
    @ApiModelProperty("是否强制重新生成")
    @TableField(exist = false)
    private Boolean forceRegenerate;
}
