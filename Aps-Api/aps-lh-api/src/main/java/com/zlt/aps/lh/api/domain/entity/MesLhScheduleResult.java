package com.zlt.aps.lh.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MesLhScheduleResult.java
 * 描    述：硫化排程下发接口对象 t_mes_lh_schedule_result
 *@author zlt
 *@date 2025-03-18
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "硫化排程下发接口对象", description = "硫化排程下发接口对象 ")
@Data
@TableName(value = "T_MES_LH_SCHEDULE_RESULT")
public class MesLhScheduleResult extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 分厂编号 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.factoryCode")
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 自动排程批次号信息，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.batchNo")
    @ApiModelProperty(value = "自动排程批次号信息，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号", name = "batchNo")
    @TableField(value = "BATCH_NO")
    private String batchNo;

    /** 工单号，自动生成（工序+日期+三位顺序号001,002） */
    @Excel(name = "ui.data.column.mesLhScheduleResult.orderNo", readConverterExp = "工=序+日期+三位顺序号001,002")
    @ApiModelProperty(value = "工单号，自动生成", name = "orderNo")
    @TableField(value = "ORDER_NO")
    private String orderNo;

    /** 月度计划单号 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.monthPlanNo")
    @ApiModelProperty(value = "月度计划单号", name = "monthPlanNo")
    @TableField(value = "MONTH_PLAN_NO")
    private String monthPlanNo;

    /** 硫化机台编号 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.lhMachineCode")
    @ApiModelProperty(value = "硫化机台编号", name = "lhMachineCode")
    @TableField(value = "LH_MACHINE_CODE")
    private String lhMachineCode;

    /** 存储当前左右模情况，如果非单模单规格的则可为空，单模单规格则存储对应的模信息，如：存储内容，L/R、L1/R1 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.leftRightMold")
    @ApiModelProperty(value = "存储当前左右模情况，如果非单模单规格的则可为空，单模单规格则存储对应的模信息，如：存储内容，L/R、L1/R1", name = "leftRightMold")
    @TableField(value = "LEFT_RIGHT_MOLD")
    private String leftRightMold;

    /** 硫化机台名称 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.lhMachineName")
    @ApiModelProperty(value = "硫化机台名称", name = "lhMachineName")
    @TableField(value = "LH_MACHINE_NAME")
    private String lhMachineName;

    /** 物料编码 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.productCode")
    @ApiModelProperty(value = "物料编码", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /** 规格代码 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.specCode")
    @ApiModelProperty(value = "规格代码", name = "specCode")
    @TableField(value = "SPEC_CODE")
    private String specCode;

    /** 胎胚代码 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.embryoCode")
    @ApiModelProperty(value = "胎胚代码", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /** 胎胚库存 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.embryoStock")
    @ApiModelProperty(value = "胎胚库存", name = "embryoStock")
    @TableField(value = "EMBRYO_STOCK")
    private Integer embryoStock;

    /** 规格描述信息 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.specDesc")
    @ApiModelProperty(value = "规格描述信息", name = "specDesc")
    @TableField(value = "SPEC_DESC")
    private String specDesc;

    /** 硫化时长 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.lhTime")
    @ApiModelProperty(value = "硫化时长", name = "lhTime")
    @TableField(value = "LH_TIME")
    private Integer lhTime;

    /** 日计划数量 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.dailyPlanQty")
    @ApiModelProperty(value = "日计划数量", name = "dailyPlanQty")
    @TableField(value = "DAILY_PLAN_QTY")
    private Integer dailyPlanQty;

    /** 月度计划模数 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.mpMoldQty")
    @ApiModelProperty(value = "月度计划模数", name = "mpMoldQty")
    @TableField(value = "MP_MOLD_QTY")
    private Integer mpMoldQty;

    /** 使用模数 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.moldQty")
    @ApiModelProperty(value = "使用模数", name = "moldQty")
    @TableField(value = "MOLD_QTY")
    private Integer moldQty;

    /** 单班单模产能 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.singleMoldShiftQty")
    @ApiModelProperty(value = "单班单模产能", name = "singleMoldShiftQty")
    @TableField(value = "SINGLE_MOLD_SHIFT_QTY")
    private Integer singleMoldShiftQty;

    /** 模具信息 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.moldInfo")
    @ApiModelProperty(value = "模具信息", name = "moldInfo")
    @TableField(value = "MOLD_INFO")
    private String moldInfo;

    /** 成型法 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.mouldMethod")
    @ApiModelProperty(value = "成型法", name = "mouldMethod")
    @TableField(value = "MOULD_METHOD")
    private String mouldMethod;

    /** BOM版本 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.bomVersion")
    @ApiModelProperty(value = "BOM版本", name = "bomVersion")
    @TableField(value = "BOM_VERSION")
    private String bomVersion;

    /** 排程日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mesLhScheduleResult.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    /** 生产状态:0-未生产；1-生产中；2-生产完成 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.productionStatus")
    @ApiModelProperty(value = "生产状态:0-未生产；1-生产中；2-生产完成", name = "productionStatus")
    @TableField(value = "PRODUCTION_STATUS")
    private String productionStatus;

    /** 一班计划量 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.class1PlanQty")
    @ApiModelProperty(value = "一班计划量", name = "class1PlanQty")
    @TableField(value = "CLASS1_PLAN_QTY")
    private Integer class1PlanQty;

    /** 一班计划开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mesLhScheduleResult.class1StartTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "一班计划开始时间", name = "class1StartTime")
    @TableField(value = "CLASS1_START_TIME")
    private Date class1StartTime;

    /** 一班计划结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mesLhScheduleResult.class1EndTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "一班计划结束时间", name = "class1EndTime")
    @TableField(value = "CLASS1_END_TIME")
    private Date class1EndTime;

    /** 一班原因分析 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.class1Analysis")
    @ApiModelProperty(value = "一班原因分析", name = "class1Analysis")
    @TableField(value = "CLASS1_ANALYSIS")
    private String class1Analysis;

    /** 一班完成量 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.class1FinishQty")
    @ApiModelProperty(value = "一班完成量", name = "class1FinishQty")
    @TableField(value = "CLASS1_FINISH_QTY")
    private Integer class1FinishQty;

    /** 二班计划量 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.class2PlanQty")
    @ApiModelProperty(value = "二班计划量", name = "class2PlanQty")
    @TableField(value = "CLASS2_PLAN_QTY")
    private Integer class2PlanQty;

    /** 二班计划开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mesLhScheduleResult.class2StartTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "二班计划开始时间", name = "class2StartTime")
    @TableField(value = "CLASS2_START_TIME")
    private Date class2StartTime;

    /** 二班计划结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mesLhScheduleResult.class2EndTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "二班计划结束时间", name = "class2EndTime")
    @TableField(value = "CLASS2_END_TIME")
    private Date class2EndTime;

    /** 二班原因分析 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.class2Analysis")
    @ApiModelProperty(value = "二班原因分析", name = "class2Analysis")
    @TableField(value = "CLASS2_ANALYSIS")
    private String class2Analysis;

    /** 二班完成量 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.class2FinishQty")
    @ApiModelProperty(value = "二班完成量", name = "class2FinishQty")
    @TableField(value = "CLASS2_FINISH_QTY")
    private Integer class2FinishQty;

    /** 三班计划量 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.class3PlanQty")
    @ApiModelProperty(value = "三班计划量", name = "class3PlanQty")
    @TableField(value = "CLASS3_PLAN_QTY")
    private Integer class3PlanQty;

    /** 三班计划开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mesLhScheduleResult.class3StartTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "三班计划开始时间", name = "class3StartTime")
    @TableField(value = "CLASS3_START_TIME")
    private Date class3StartTime;

    /** 三班计划结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mesLhScheduleResult.class3EndTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "三班计划结束时间", name = "class3EndTime")
    @TableField(value = "CLASS3_END_TIME")
    private Date class3EndTime;

    /** 三班原因分析 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.class3Analysis")
    @ApiModelProperty(value = "三班原因分析", name = "class3Analysis")
    @TableField(value = "CLASS3_ANALYSIS")
    private String class3Analysis;

    /** 三班完成量 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.class3FinishQty")
    @ApiModelProperty(value = "三班完成量", name = "class3FinishQty")
    @TableField(value = "CLASS3_FINISH_QTY")
    private Integer class3FinishQty;

    /** 次日一班计划量 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.class4PlanQty")
    @ApiModelProperty(value = "次日一班计划量", name = "class4PlanQty")
    @TableField(value = "CLASS4_PLAN_QTY")
    private Integer class4PlanQty;

    /** 次日一班计划开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mesLhScheduleResult.class4StartTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "次日一班计划开始时间", name = "class4StartTime")
    @TableField(value = "CLASS4_START_TIME")
    private Date class4StartTime;

    /** 次日一班计划结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mesLhScheduleResult.class4EndTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "次日一班计划结束时间", name = "class4EndTime")
    @TableField(value = "CLASS4_END_TIME")
    private Date class4EndTime;

    /** 次日一班原因分析 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.class4Analysis")
    @ApiModelProperty(value = "次日一班原因分析", name = "class4Analysis")
    @TableField(value = "CLASS4_ANALYSIS")
    private String class4Analysis;

    /** 次日一班完成量 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.class4FinishQty")
    @ApiModelProperty(value = "次日一班完成量", name = "class4FinishQty")
    @TableField(value = "CLASS4_FINISH_QTY")
    private Integer class4FinishQty;

    /** 次日二班计划量 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.class5PlanQty")
    @ApiModelProperty(value = "次日二班计划量", name = "class5PlanQty")
    @TableField(value = "CLASS5_PLAN_QTY")
    private Integer class5PlanQty;

    /** 次日二班计划开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mesLhScheduleResult.class5StartTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "次日二班计划开始时间", name = "class5StartTime")
    @TableField(value = "CLASS5_START_TIME")
    private Date class5StartTime;

    /** 次日二班计划结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mesLhScheduleResult.class5EndTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "次日二班计划结束时间", name = "class5EndTime")
    @TableField(value = "CLASS5_END_TIME")
    private Date class5EndTime;

    /** 次日二班原因分析 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.class5Analysis")
    @ApiModelProperty(value = "次日二班原因分析", name = "class5Analysis")
    @TableField(value = "CLASS5_ANALYSIS")
    private String class5Analysis;

    /** 次日二班完成量 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.class5FinishQty")
    @ApiModelProperty(value = "次日二班完成量", name = "class5FinishQty")
    @TableField(value = "CLASS5_FINISH_QTY")
    private Integer class5FinishQty;

    /** 次日三班计划量 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.class6PlanQty")
    @ApiModelProperty(value = "次日三班计划量", name = "class6PlanQty")
    @TableField(value = "CLASS6_PLAN_QTY")
    private Integer class6PlanQty;

    /** 次日三班计划开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mesLhScheduleResult.class6StartTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "次日三班计划开始时间", name = "class6StartTime")
    @TableField(value = "CLASS6_START_TIME")
    private Date class6StartTime;

    /** 次日三班计划结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mesLhScheduleResult.class6EndTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "次日三班计划结束时间", name = "class6EndTime")
    @TableField(value = "CLASS6_END_TIME")
    private Date class6EndTime;

    /** 次日三班原因分析 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.class6Analysis")
    @ApiModelProperty(value = "次日三班原因分析", name = "class6Analysis")
    @TableField(value = "CLASS6_ANALYSIS")
    private String class6Analysis;

    /** 次日三班完成量 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.class6FinishQty")
    @ApiModelProperty(value = "次日三班完成量", name = "class6FinishQty")
    @TableField(value = "CLASS6_FINISH_QTY")
    private Integer class6FinishQty;

    /** 是否交期，0--否，1--是 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.isDelivery")
    @ApiModelProperty(value = "是否交期，0--否，1--是", name = "isDelivery")
    @TableField(value = "IS_DELIVERY")
    private String isDelivery;

    /** 下发时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mesLhScheduleResult.issuedDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "下发时间", name = "issuedDate")
    @TableField(value = "ISSUED_DATE")
    private Date issuedDate;

    /** 是否下发 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.issuedStatus")
    @ApiModelProperty(value = "是否下发", name = "issuedStatus")
    @TableField(value = "ISSUED_STATUS")
    private String issuedStatus;

    /** 目标系统标识 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.targetSystemIdentification")
    @ApiModelProperty(value = "目标系统标识", name = "targetSystemIdentification")
    @TableField(value = "TARGET_SYSTEM_IDENTIFICATION")
    private String targetSystemIdentification;

    /** 版本号 */
    @Excel(name = "ui.data.column.mesLhScheduleResult.dataVersion")
    @ApiModelProperty(value = "版本号", name = "dataVersion")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;
}