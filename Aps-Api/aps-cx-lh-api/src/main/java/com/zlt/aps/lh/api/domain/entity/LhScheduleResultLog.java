package com.zlt.aps.lh.api.domain.entity;

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
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhScheduleResultLog.java
 * 描    述：硫化排程结果日志对象 t_lh_schedule_result_log
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-27
 */

@ApiModel(value = "硫化排程结果日志对象", description = "硫化排程结果日志对象 ")
@Data
@TableName(value = "T_LH_SCHEDULE_RESULT_LOG")
public class LhScheduleResultLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 分厂编号
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.factoryCode")
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 自动排程批次号信息，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.batchNo")
    @ApiModelProperty(value = "自动排程批次号信息，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号", name = "batchNo")
    @TableField(value = "BATCH_NO")
    private String batchNo;

    /**
     * 工单号，自动生成（工序+日期+三位顺序号001,002）
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.orderNo", readConverterExp = "工=序+日期+三位顺序号001,002")
    @ApiModelProperty(value = "工单号，自动生成", name = "orderNo")
    @TableField(value = "ORDER_NO")
    private String orderNo;

    /** 月度计划单号 */
    @ApiModelProperty(value = "月度计划单号", name = "MONTH_PLAN_NO")
    @TableField(value = "MONTH_PLAN_NO")
    private String monthPlanNo;

    /** 月度计划版本号 */
    @ApiModelProperty(value = "月度计划版本号", name = "MONTH_PLAN_VERSION")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 硫化机台编号
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.lhMachineCode")
    @ApiModelProperty(value = "硫化机台编号", name = "lhMachineCode")
    @TableField(value = "LH_MACHINE_CODE")
    private String lhMachineCode;

    /**
     * 存储当前左右模情况，如果非单模单规格的则可为空，单模单规格则存储对应的模信息，如：存储内容，L/R、L1/R1
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.leftRightMold")
    @ApiModelProperty(value = "存储当前左右模情况，如果非单模单规格的则可为空，单模单规格则存储对应的模信息，如：存储内容，L/R、L1/R1", name = "leftRightMold")
    @TableField(value = "LEFT_RIGHT_MOLD")
    private String leftRightMold;

    /**
     * 硫化机台名称
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.lhMachineName")
    @ApiModelProperty(value = "硫化机台名称", name = "lhMachineName")
    @TableField(value = "LH_MACHINE_NAME")
    private String lhMachineName;

    /**
     * 物料编码
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.productCode")
    @ApiModelProperty(value = "物料编码", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * SAP品号信息
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.specsCode")
    @ApiModelProperty(value = "SAP品号信息", name = "specsCode")
    @TableField(value = "SPECS_CODE")
    private String specsCode;

    /**
     * 胎胚代码
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.embryoCode")
    @ApiModelProperty(value = "胎胚代码", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /**
     * 胎胚库存
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.embryoStock")
    @ApiModelProperty(value = "胎胚库存", name = "embryoStock")
    @TableField(value = "EMBRYO_STOCK")
    private Integer embryoStock;

    /**
     * 规格描述信息
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.specDesc")
    @ApiModelProperty(value = "规格描述信息", name = "specDesc")
    @TableField(value = "SPEC_DESC")
    private String specDesc;

    /**
     * 库存地点
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.stockArea")
    @ApiModelProperty(value = "库存地点", name = "stockArea")
    @TableField(value = "STOCK_AREA")
    private String stockArea;

    /**
     * 硫化时长
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.lhTime")
    @ApiModelProperty(value = "硫化时长", name = "lhTime")
    @TableField(value = "LH_TIME")
    private BigDecimal lhTime;

    /**
     * 日计划数量
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.dailyPlanQty")
    @ApiModelProperty(value = "日计划数量", name = "dailyPlanQty")
    @TableField(value = "DAILY_PLAN_QTY")
    private Integer dailyPlanQty;

    /**
     * 排程日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.lhScheduleResultLog.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    /**
     * 生产状态:0-未生产；1-生产中；2-生产完成
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.productionStatus")
    @ApiModelProperty(value = "生产状态:0-未生产；1-生产中；2-生产完成", name = "productionStatus")
    @TableField(value = "PRODUCTION_STATUS")
    private String productionStatus;

    /**
     * 一班计划量
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.class1PlanQty")
    @ApiModelProperty(value = "一班计划量", name = "class1PlanQty")
    @TableField(value = "CLASS1_PLAN_QTY")
    private Integer class1PlanQty;

    /**
     * 一班计划开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.lhScheduleResultLog.class1StartTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "一班计划开始时间", name = "class1StartTime")
    @TableField(value = "CLASS1_START_TIME")
    private Date class1StartTime;

    /**
     * 一班计划结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.lhScheduleResultLog.class1EndTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "一班计划结束时间", name = "class1EndTime")
    @TableField(value = "CLASS1_END_TIME")
    private Date class1EndTime;

    /**
     * 一班原因分析
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.class1Analysis")
    @ApiModelProperty(value = "一班原因分析", name = "class1Analysis")
    @TableField(value = "CLASS1_ANALYSIS")
    private String class1Analysis;

    /**
     * 一班完成量
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.class1FinishQty")
    @ApiModelProperty(value = "一班完成量", name = "class1FinishQty")
    @TableField(value = "CLASS1_FINISH_QTY")
    private Integer class1FinishQty;

    /**
     * 二班计划量
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.class2PlanQty")
    @ApiModelProperty(value = "二班计划量", name = "class2PlanQty")
    @TableField(value = "CLASS2_PLAN_QTY")
    private Integer class2PlanQty;

    /**
     * 二班计划开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.lhScheduleResultLog.class2StartTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "二班计划开始时间", name = "class2StartTime")
    @TableField(value = "CLASS2_START_TIME")
    private Date class2StartTime;

    /**
     * 二班计划结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.lhScheduleResultLog.class2EndTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "二班计划结束时间", name = "class2EndTime")
    @TableField(value = "CLASS2_END_TIME")
    private Date class2EndTime;

    /**
     * 二班原因分析
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.class2Analysis")
    @ApiModelProperty(value = "二班原因分析", name = "class2Analysis")
    @TableField(value = "CLASS2_ANALYSIS")
    private String class2Analysis;

    /**
     * 二班完成量
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.class2FinishQty")
    @ApiModelProperty(value = "二班完成量", name = "class2FinishQty")
    @TableField(value = "CLASS2_FINISH_QTY")
    private Integer class2FinishQty;

    /**
     * 三班计划量
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.class3PlanQty")
    @ApiModelProperty(value = "三班计划量", name = "class3PlanQty")
    @TableField(value = "CLASS3_PLAN_QTY")
    private Integer class3PlanQty;

    /**
     * 三班计划开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.lhScheduleResultLog.class3StartTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "三班计划开始时间", name = "class3StartTime")
    @TableField(value = "CLASS3_START_TIME")
    private Date class3StartTime;

    /**
     * 三班计划结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.lhScheduleResultLog.class3EndTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "三班计划结束时间", name = "class3EndTime")
    @TableField(value = "CLASS3_END_TIME")
    private Date class3EndTime;

    /**
     * 三班原因分析
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.class3Analysis")
    @ApiModelProperty(value = "三班原因分析", name = "class3Analysis")
    @TableField(value = "CLASS3_ANALYSIS")
    private String class3Analysis;

    /**
     * 三班完成量
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.class3FinishQty")
    @ApiModelProperty(value = "三班完成量", name = "class3FinishQty")
    @TableField(value = "CLASS3_FINISH_QTY")
    private Integer class3FinishQty;

    /**
     * 次日一班计划量
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.class4PlanQty")
    @ApiModelProperty(value = "次日一班计划量", name = "class4PlanQty")
    @TableField(value = "CLASS4_PLAN_QTY")
    private Integer class4PlanQty;

    /**
     * 次日一班计划开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.lhScheduleResultLog.class4StartTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "次日一班计划开始时间", name = "class4StartTime")
    @TableField(value = "CLASS4_START_TIME")
    private Date class4StartTime;

    /**
     * 次日一班计划结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.lhScheduleResultLog.class4EndTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "次日一班计划结束时间", name = "class4EndTime")
    @TableField(value = "CLASS4_END_TIME")
    private Date class4EndTime;

    /**
     * 次日一班原因分析
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.class4Analysis")
    @ApiModelProperty(value = "次日一班原因分析", name = "class4Analysis")
    @TableField(value = "CLASS4_ANALYSIS")
    private String class4Analysis;

    /**
     * 次日一班完成量
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.class4FinishQty")
    @ApiModelProperty(value = "次日一班完成量", name = "class4FinishQty")
    @TableField(value = "CLASS4_FINISH_QTY")
    private Integer class4FinishQty;

    /**
     * 次日二班计划量
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.class5PlanQty")
    @ApiModelProperty(value = "次日二班计划量", name = "class5PlanQty")
    @TableField(value = "CLASS5_PLAN_QTY")
    private Integer class5PlanQty;

    /**
     * 次日二班计划开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.lhScheduleResultLog.class5StartTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "次日二班计划开始时间", name = "class5StartTime")
    @TableField(value = "CLASS5_START_TIME")
    private Date class5StartTime;

    /**
     * 次日二班计划结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.lhScheduleResultLog.class5EndTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "次日二班计划结束时间", name = "class5EndTime")
    @TableField(value = "CLASS5_END_TIME")
    private Date class5EndTime;

    /**
     * 次日二班原因分析
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.class5Analysis")
    @ApiModelProperty(value = "次日二班原因分析", name = "class5Analysis")
    @TableField(value = "CLASS5_ANALYSIS")
    private String class5Analysis;

    /**
     * 次日二班完成量
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.class5FinishQty")
    @ApiModelProperty(value = "次日二班完成量", name = "class5FinishQty")
    @TableField(value = "CLASS5_FINISH_QTY")
    private Integer class5FinishQty;

    /**
     * 次日三班计划量
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.class6PlanQty")
    @ApiModelProperty(value = "次日三班计划量", name = "class6PlanQty")
    @TableField(value = "CLASS6_PLAN_QTY")
    private Integer class6PlanQty;

    /**
     * 次日三班计划开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.lhScheduleResultLog.class6StartTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "次日三班计划开始时间", name = "class6StartTime")
    @TableField(value = "CLASS6_START_TIME")
    private Date class6StartTime;

    /**
     * 次日三班计划结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.lhScheduleResultLog.class6EndTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "次日三班计划结束时间", name = "class6EndTime")
    @TableField(value = "CLASS6_END_TIME")
    private Date class6EndTime;

    /**
     * 次日三班原因分析
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.class6Analysis")
    @ApiModelProperty(value = "次日三班原因分析", name = "class6Analysis")
    @TableField(value = "CLASS6_ANALYSIS")
    private String class6Analysis;

    /**
     * 次日三班完成量
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.class6FinishQty")
    @ApiModelProperty(value = "次日三班完成量", name = "class6FinishQty")
    @TableField(value = "CLASS6_FINISH_QTY")
    private Integer class6FinishQty;

    /**
     * 是否交期，0--否，1--是
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.isDelivery")
    @ApiModelProperty(value = "是否交期，0--否，1--是", name = "isDelivery")
    @TableField(value = "IS_DELIVERY")
    private String isDelivery;

    /**
     * 是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.isRelease")
    @ApiModelProperty(value = "是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE", name = "isRelease")
    @TableField(value = "IS_RELEASE")
    private String isRelease;

    /**
     * 发布成功计数器，每次发布成功进行累加。如果大于1发，发布状态只能到待发布
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.publishSuccessCount")
    @ApiModelProperty(value = "发布成功计数器，每次发布成功进行累加。如果大于1发，发布状态只能到待发布", name = "publishSuccessCount")
    @TableField(value = "PUBLISH_SUCCESS_COUNT")
    private Integer publishSuccessCount;

    /**
     * 保留最新的一次发布成功时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.lhScheduleResultLog.newestPublishTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "保留最新的一次发布成功时间", name = "newestPublishTime")
    @TableField(value = "NEWEST_PUBLISH_TIME")
    private Date newestPublishTime;

    /**
     * 数据来源：0&gt;自动排程；1&gt;插单；2：导入。插单数据可以进行计划调整
     */
    @Excel(name = "ui.data.column.lhScheduleResultLog.dataSource")
    @ApiModelProperty(value = "数据来源：0&gt;自动排程；1&gt;插单；2：导入。插单数据可以进行计划调整", name = "dataSource")
    @TableField(value = "DATA_SOURCE")
    private String dataSource;


}