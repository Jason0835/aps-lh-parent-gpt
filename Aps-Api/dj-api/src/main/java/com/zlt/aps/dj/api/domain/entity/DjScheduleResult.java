package com.zlt.aps.dj.api.domain.entity;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 垫胶排程结果对象 dj_schedule_result
 *
 * @author zlt
 * @date 2026-06-01
 */
@Data
@EqualsAndHashCode(callSuper=false)
@TableName(value = "T_DJ_SCHEDULE_RESULT")
@ApiModel(value = "垫胶排程结果对象", description = "垫胶排程结果对象 ")
public class DjScheduleResult extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 工厂编号 */
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 批次号 */
    @Excel(name = "ui.data.column.dj.scheduleResult.batchNo")
    @ApiModelProperty(value = "批次号", name = "batchNo")
    @TableField(value = "BATCH_NO")
    private String batchNo;

    /**
     * 工单号，自动生成（批次号+4位定长自增序号）
     */
    @Excel(name = "ui.data.column.dj.scheduleResult.orderNo")
    @ApiModelProperty(value = "工单号")
    @TableField(value = "ORDER_NO")
    private String orderNo;

    /** 排程日期 */
    @Excel(name = "ui.data.column.dj.scheduleResult.scheduleDate")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    /** 机台编码 */
    @Excel(name = "ui.data.column.dj.scheduleResult.machineCode")
    @ApiModelProperty(value = "机台", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    /** 垫胶编码 */
    @Excel(name = "ui.data.column.dj.scheduleResult.paddingCode")
    @ApiModelProperty(value = "垫胶", name = "treadCode")
    @TableField("PADDING_CODE")
    private String paddingCode;

    /** 垫胶物料名 */
    @Excel(name = "ui.data.column.dj.scheduleResult.paddingName")
    @ApiModelProperty(value = "垫胶物料名", name = "paddingName")
    @TableField(value = "PADDING_NAME")
    private String paddingName;

    /**
     * 胶料代码
     */
    @Excel(name = "ui.data.column.dj.scheduleResult.glueCode")
    @ApiModelProperty(value = "胶料")
    @TableField("GLUE_CODE")
    private String glueCode;

    /** 口型板编码 */
    @Excel(name = "ui.data.column.dj.scheduleResult.mouthPlateCode")
    @ApiModelProperty(value = "口型板编码", name = "mouthPlateCode")
    @TableField(value = "MOUTH_PLATE_CODE")
    private String mouthPlateCode;

    /** 1班顺序 */
    @Excel(name = "ui.data.column.dj.scheduleResult.class1Sequence")
    @ApiModelProperty(value = "1班顺序", name = "class1Sequence")
    @TableField(value = "CLASS1_SEQUENCE")
    private Integer class1Sequence;

    /** 1班计划量 */
    @Excel(name = "ui.data.column.dj.scheduleResult.class1PlanQty")
    @ApiModelProperty(value = "1班计划量", name = "class1PlanQty")
    @TableField(value = "CLASS1_PLAN_QTY")
    private BigDecimal class1PlanQty;

    /** 1班完成量 */
    @Excel(name = "ui.data.column.dj.scheduleResult.class1FinishQty")
    @ApiModelProperty(value = "1班完成量", name = "class1FinishQty")
    @TableField(exist = false)
    private BigDecimal class1FinishQty;

    /** 1班原因分析 */
    @Excel(name = "ui.data.column.dj.scheduleResult.class1Analysis")
    @ApiModelProperty(value = "1班原因分析", name = "class1Analysis")
    @TableField(value = "CLASS1_ANALYSIS")
    private String class1Analysis;

    /** 2班顺序 */
    @Excel(name = "ui.data.column.dj.scheduleResult.class2Sequence")
    @ApiModelProperty(value = "2班顺序", name = "class2Sequence")
    @TableField(value = "CLASS2_SEQUENCE")
    private Integer class2Sequence;

    /** 2班计划量 */
    @Excel(name = "ui.data.column.dj.scheduleResult.class2PlanQty")
    @ApiModelProperty(value = "2班计划量", name = "class2PlanQty")
    @TableField(value = "CLASS2_PLAN_QTY")
    private BigDecimal class2PlanQty;

    /** 2班完成量 */
    @Excel(name = "ui.data.column.dj.scheduleResult.class2FinishQty")
    @ApiModelProperty(value = "2班完成量", name = "class2FinishQty")
    @TableField(exist = false)
    private BigDecimal class2FinishQty;

    /** 2班原因分析 */
    @Excel(name = "ui.data.column.dj.scheduleResult.class2Analysis")
    @ApiModelProperty(value = "2班原因分析", name = "class2Analysis")
    @TableField(value = "CLASS2_ANALYSIS")
    private String class2Analysis;

    /** 3班顺序 */
    @Excel(name = "ui.data.column.dj.scheduleResult.class3Sequence")
    @ApiModelProperty(value = "3班顺序", name = "class3Sequence")
    @TableField(value = "CLASS3_SEQUENCE")
    private Integer class3Sequence;

    /** 3班计划量 */
    @Excel(name = "ui.data.column.dj.scheduleResult.class3PlanQty")
    @ApiModelProperty(value = "3班计划量", name = "class3PlanQty")
    @TableField(value = "CLASS3_PLAN_QTY")
    private BigDecimal class3PlanQty;

    /** 3班完成量 */
    @Excel(name = "ui.data.column.dj.scheduleResult.class3FinishQty")
    @ApiModelProperty(value = "3班完成量", name = "class3FinishQty")
    @TableField(exist = false)
    private BigDecimal class3FinishQty;

    /** 3班原因分析 */
    @Excel(name = "ui.data.column.dj.scheduleResult.class3Analysis")
    @ApiModelProperty(value = "3班原因分析", name = "class3Analysis")
    @TableField(value = "CLASS3_ANALYSIS")
    private String class3Analysis;

    /** 4班顺序 */
    @Excel(name = "ui.data.column.dj.scheduleResult.class4Sequence")
    @ApiModelProperty(value = "4班顺序", name = "class4Sequence")
    @TableField(value = "CLASS4_SEQUENCE")
    private Integer class4Sequence;

    /** 4班计划量 */
    @Excel(name = "ui.data.column.dj.scheduleResult.class4PlanQty")
    @ApiModelProperty(value = "4班计划量", name = "class4PlanQty")
    @TableField(value = "CLASS4_PLAN_QTY")
    private BigDecimal class4PlanQty;

    /** 4班完成量 */
    @Excel(name = "ui.data.column.dj.scheduleResult.class4FinishQty")
    @ApiModelProperty(value = "4班完成量", name = "class4FinishQty")
    @TableField(exist = false)
    private BigDecimal class4FinishQty;

    /** 4班原因分析 */
    @Excel(name = "ui.data.column.dj.scheduleResult.class4Analysis")
    @ApiModelProperty(value = "4班原因分析", name = "class4Analysis")
    @TableField(value = "CLASS4_ANALYSIS")
    private String class4Analysis;

    /** 5班顺序 */
    @Excel(name = "ui.data.column.dj.scheduleResult.class5Sequence")
    @ApiModelProperty(value = "5班顺序", name = "class5Sequence")
    @TableField(value = "CLASS5_SEQUENCE")
    private Integer class5Sequence;

    /** 5班计划量 */
    @Excel(name = "ui.data.column.dj.scheduleResult.class5PlanQty")
    @ApiModelProperty(value = "5班计划量", name = "class5PlanQty")
    @TableField(value = "CLASS5_PLAN_QTY")
    private BigDecimal class5PlanQty;

    /** 5班完成量 */
    @Excel(name = "ui.data.column.dj.scheduleResult.class5FinishQty")
    @ApiModelProperty(value = "5班完成量", name = "class5FinishQty")
    @TableField(exist = false)
    private BigDecimal class5FinishQty;

    /** 5班原因分析 */
    @Excel(name = "ui.data.column.dj.scheduleResult.class5Analysis")
    @ApiModelProperty(value = "5班原因分析", name = "class5Analysis")
    @TableField(value = "CLASS5_ANALYSIS")
    private String class5Analysis;

    /** 6班顺序 */
    @Excel(name = "ui.data.column.dj.scheduleResult.class6Sequence")
    @ApiModelProperty(value = "6班顺序", name = "class6Sequence")
    @TableField(value = "CLASS6_SEQUENCE")
    private Integer class6Sequence;

    /** 6班计划量 */
    @Excel(name = "ui.data.column.dj.scheduleResult.class6PlanQty")
    @ApiModelProperty(value = "6班计划量", name = "class6PlanQty")
    @TableField(value = "CLASS6_PLAN_QTY")
    private BigDecimal class6PlanQty;

    /** 6班完成量 */
    @Excel(name = "ui.data.column.dj.scheduleResult.class6FinishQty")
    @ApiModelProperty(value = "6班完成量", name = "class6FinishQty")
    @TableField(exist = false)
    private BigDecimal class6FinishQty;

    /** 6班原因分析 */
    @Excel(name = "ui.data.column.dj.scheduleResult.class6Analysis")
    @ApiModelProperty(value = "6班原因分析", name = "class6Analysis")
    @TableField(value = "CLASS6_ANALYSIS")
    private String class6Analysis;

    /** 发布状态 */
    @Excel(name = "ui.data.column.dj.scheduleResult.releaseStatus")
    @ApiModelProperty(value = "发布状态", name = "releaseStatus")
    @TableField(value = "RELEASE_STATUS")
    private String releaseStatus;

    /** 排程首班班次（ClassNumThreePlanEnums.classIndex），如 "03"=中班、"01"=夜班、"02"=早班 */
    @Excel(name = "ui.data.column.dj.scheduleResult.scheduleShiftClass")
    @ApiModelProperty(value = "排程首班班次")
    @TableField(value = "SCHEDULE_SHIFT_CLASS")
    private String scheduleShiftClass;

    /** 数据来源 */
    @ApiModelProperty(value = "数据来源", name = "dataSource")
    @TableField(value = "DATA_SOURCE")
    private String dataSource;

    /** 是否收尾任务 */
    @ApiModelProperty(value = "是否收尾任务", name = "tailFlag")
    @TableField(value = "TAIL_FLAG")
    private String tailFlag;

    /**
     * 对应成型8的计划量
     */
    @ApiModelProperty(value = "对应成型8班的计划量")
    @TableField(exist = false)
    private BigDecimal cxClass8Plan;
    
    @ApiModelProperty(value = "发布成功计数器，每点击一次发布并成功的话，计数器累加")
    private Integer publishSuccessCount;

    /**
     * 库存数量（排程时使用的有效库存）
     */
    @Excel(name = "ui.data.column.scheduleResult.stockQty")
    @ApiModelProperty(value = "库存")
    @TableField(value = "STOCK_QTY")
    private BigDecimal stockQty;

    @Excel(name = "ui.data.column.scheduleResult.monthPlanOs")
    @ApiModelProperty(value = "月计划剩余量")
    @TableField(exist = false)
    private Double monthPlanOs;

    @Excel(name = "ui.data.column.scheduleResult.finish")
    @ApiModelProperty(value = "1班完成率", name = "class1FinishRate")
    @TableField(exist = false)
    private BigDecimal class1FinishRate;

    @Excel(name = "ui.data.column.scheduleResult.finish")
    @ApiModelProperty(value = "2班完成率", name = "class2FinishRate")
    @TableField(exist = false)
    private BigDecimal class2FinishRate;

    @Excel(name = "ui.data.column.scheduleResult.finish")
    @ApiModelProperty(value = "3班完成率", name = "class3FinishRate")
    @TableField(exist = false)
    private BigDecimal class3FinishRate;

    @Excel(name = "ui.data.column.scheduleResult.finish")
    @ApiModelProperty(value = "4班完成率", name = "class4FinishRate")
    @TableField(exist = false)
    private BigDecimal class4FinishRate;

    @Excel(name = "ui.data.column.scheduleResult.finish")
    @ApiModelProperty(value = "5班完成率", name = "class5FinishRate")
    @TableField(exist = false)
    private BigDecimal class5FinishRate;

    @Excel(name = "ui.data.column.scheduleResult.finish")
    @ApiModelProperty(value = "6班完成率", name = "class6FinishRate")
    @TableField(exist = false)
    private BigDecimal class6FinishRate;
    
    @TableField(exist = false)
    private String year;
    
    @TableField(exist = false)
    private String month;

    /** 机台名称（非数据库字段，用于前端展示） */
    @ApiModelProperty(value = "机台名称", name = "machineName")
    @TableField(exist = false)
    private String machineName;

    /** T-1日早班数据（非数据库字段，从 T-1 日排产结果 class3 加载） */
    @ApiModelProperty(value = "T-1日早班顺序", name = "prevDayClass3Sequence")
    @TableField(exist = false)
    private Integer prevDayClass3Sequence;

    @ApiModelProperty(value = "T-1日早班计划量", name = "prevDayClass3PlanQty")
    @TableField(exist = false)
    private BigDecimal prevDayClass3PlanQty;

    @ApiModelProperty(value = "T-1日早班完成量", name = "prevDayClass3FinishQty")
    @TableField(exist = false)
    private BigDecimal prevDayClass3FinishQty;

    @ApiModelProperty(value = "T-1日早班完成率", name = "prevDayClass3FinishRate")
    @TableField(exist = false)
    private BigDecimal prevDayClass3FinishRate;

    @ApiModelProperty(value = "T-1日早班原因分析", name = "prevDayClass3Analysis")
    @TableField(exist = false)
    private String prevDayClass3Analysis;

    /** T-1日中班数据（非数据库字段，从 T-1 日排产结果加载，首班为夜班时使用） */
    @ApiModelProperty(value = "T-1日中班顺序", name = "prevDayClass1Sequence")
    @TableField(exist = false)
    private Integer prevDayClass1Sequence;

    @ApiModelProperty(value = "T-1日中班计划量", name = "prevDayClass1PlanQty")
    @TableField(exist = false)
    private BigDecimal prevDayClass1PlanQty;

    @ApiModelProperty(value = "T-1日中班完成量", name = "prevDayClass1FinishQty")
    @TableField(exist = false)
    private BigDecimal prevDayClass1FinishQty;

    @ApiModelProperty(value = "T-1日中班完成率", name = "prevDayClass1FinishRate")
    @TableField(exist = false)
    private BigDecimal prevDayClass1FinishRate;

    @ApiModelProperty(value = "T-1日中班原因分析", name = "prevDayClass1Analysis")
    @TableField(exist = false)
    private String prevDayClass1Analysis;

    @TableField(exist = false)
    private Long[] ids;

    @TableField(exist = false)
    private Date newestPublishTime;
}
