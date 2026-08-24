package com.zlt.aps.tm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
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
    @Excel(name = "ui.data.column.tm.scheduleResult.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 批次号 */
    @Excel(name = "ui.data.column.tm.scheduleResult.batchNo")
    @ApiModelProperty(value = "批次号", name = "batchNo")
    @TableField(value = "BATCH_NO")
    private String batchNo;

    /** 工单号 */
    @Excel(name = "ui.data.column.tm.scheduleResult.orderNo")
    @ApiModelProperty(value = "工单号", name = "orderNo")
    @TableField(value = "ORDER_NO")
    private String orderNo;

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

    /** 基部胶编码 */
    @Excel(name = "ui.data.column.tm.scheduleResult.baseGlueCode")
    @ApiModelProperty(value = "基部胶编码", name = "baseGlueCode")
    @TableField(value = "BASE_GLUE_CODE")
    private String baseGlueCode;

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

    /** 胎面长度，单位米 */
    @Excel(name = "ui.data.column.tm.scheduleResult.treadShoulderLength", type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "胎面长度", name = "treadShoulderLength")
    @TableField(value = "TREAD_SHOULDER_LENGTH")
    private BigDecimal treadShoulderLength;

    /** 成型余量，单位条 */
    @Excel(name = "ui.data.column.tm.scheduleResult.cxRemainQty", type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "成型余量", name = "cxRemainQty")
    @TableField(value = "CX_REMAIN_QTY")
    private BigDecimal cxRemainQty;

    /** 成型物料编号，多个编号使用英文逗号分隔 */
    @Excel(name = "ui.data.column.tm.scheduleResult.materialCode", type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "物料编号", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 成型物料描述，多个描述使用英文逗号分隔 */
    @Excel(name = "ui.data.column.tm.scheduleResult.materialDesc", type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /** 胎胚代码，多个代码使用英文逗号分隔 */
    @Excel(name = "ui.data.column.tm.scheduleResult.embryoCode", type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "胎胚代码", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /** 胎胚描述，多个描述使用英文逗号分隔 */
    @Excel(name = "ui.data.column.tm.scheduleResult.mainMaterialDesc", type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "胎胚描述", name = "mainMaterialDesc")
    @TableField(value = "MAIN_MATERIAL_DESC")
    private String mainMaterialDesc;

    /** 成型机台编号，多个编号使用英文逗号分隔 */
    @Excel(name = "ui.data.column.tm.scheduleResult.cxMachineCode", type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "成型机台编号", name = "cxMachineCode")
    @TableField(value = "CX_MACHINE_CODE")
    private String cxMachineCode;

    /** 6 点库存，单位米 */
    @Excel(name = "ui.data.column.tm.scheduleResult.sixClockStockQty", type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "6点库存", name = "sixClockStockQty")
    @TableField(value = "SIX_CLOCK_STOCK_QTY")
    private BigDecimal sixClockStockQty;

    /** 卷曲长度，单位米/条 */
    @Excel(name = "ui.data.column.tm.scheduleResult.curlRollLength", type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "卷曲长度", name = "curlRollLength")
    @TableField(value = "CURL_ROLL_LENGTH")
    private BigDecimal curlRollLength;

    /** 1班顺序 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class1Sequence")
    @ApiModelProperty(value = "1班顺序", name = "class1Sequence")
    @TableField(value = "CLASS1_SEQUENCE")
    private Integer class1Sequence;

    /** 1班预计开始时间 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class1StartTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "1班预计开始时间", name = "class1StartTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "CLASS1_START_TIME")
    private Date class1StartTime;

    /** 1班预计结束时间 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class1EndTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "1班预计结束时间", name = "class1EndTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "CLASS1_END_TIME")
    private Date class1EndTime;

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

    /** 2班预计开始时间 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class2StartTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "2班预计开始时间", name = "class2StartTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "CLASS2_START_TIME")
    private Date class2StartTime;

    /** 2班预计结束时间 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class2EndTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "2班预计结束时间", name = "class2EndTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "CLASS2_END_TIME")
    private Date class2EndTime;

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

    /** 3班预计开始时间 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class3StartTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "3班预计开始时间", name = "class3StartTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "CLASS3_START_TIME")
    private Date class3StartTime;

    /** 3班预计结束时间 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class3EndTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "3班预计结束时间", name = "class3EndTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "CLASS3_END_TIME")
    private Date class3EndTime;

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

    /** 4班预计开始时间 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class4StartTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "4班预计开始时间", name = "class4StartTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "CLASS4_START_TIME")
    private Date class4StartTime;

    /** 4班预计结束时间 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class4EndTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "4班预计结束时间", name = "class4EndTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "CLASS4_END_TIME")
    private Date class4EndTime;

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

    /** 5班预计开始时间 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class5StartTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "5班预计开始时间", name = "class5StartTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "CLASS5_START_TIME")
    private Date class5StartTime;

    /** 5班预计结束时间 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class5EndTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "5班预计结束时间", name = "class5EndTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "CLASS5_END_TIME")
    private Date class5EndTime;

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

    /** 6班预计开始时间 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class6StartTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "6班预计开始时间", name = "class6StartTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "CLASS6_START_TIME")
    private Date class6StartTime;

    /** 6班预计结束时间 */
    @Excel(name = "ui.data.column.tm.scheduleResult.class6EndTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "6班预计结束时间", name = "class6EndTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "CLASS6_END_TIME")
    private Date class6EndTime;

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
    @Excel(name = "ui.data.column.tm.scheduleResult.releaseStatus", dictType = "IS_RELEASE")
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

    /** 是否仅导出空模板，不映射数据库字段。 */
    @ApiModelProperty(value = "是否仅导出空模板", name = "exportTemplate")
    @TableField(exist = false)
    private Boolean exportTemplate;

    /**
     * 调量目标班次，仅用于调量请求，不映射数据库字段。
     *
     * <p>调量请求不再信任前端传入的 classNSequence，后端根据该班次从数据库恢复真实顺序。</p>
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty(value = "调量目标班次", name = "shiftOrder")
    @TableField(exist = false)
    private Integer shiftOrder;
}
