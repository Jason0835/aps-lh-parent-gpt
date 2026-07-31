package com.zlt.aps.gsq.api.domain.entity;

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
import java.lang.reflect.Field;
import java.util.Date;

/**
 * 钢丝圈排程结果实体类
 * 6班次制：1班=D日中班，2班=D+1日夜班，3班=D+1日早班，4班=D+1日中班，5班=D+2日夜班，6班=D+2日早班
 * 其中 D+1 = 排程日期（SCHEDULE_DATE）
 *
 * @author APS
 */
@ApiModel(value = "钢丝圈排程结果对象", description = "钢丝圈排程结果表实体对象")
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "T_GSQ_SCHEDULE_RESULT")
public class GsqScheduleResult extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 排程日期(胎圈生产第一天=D+1) */
    @Excel(name = "ui.data.column.gsqScheduleResult.scheduleDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    /** 成型批次号 */
    @Excel(name = "ui.data.column.gsqScheduleResult.cxBatchNo")
    @ApiModelProperty(value = "成型批次号", name = "cxBatchNo")
    @TableField(value = "CX_BATCH_NO")
    private String cxBatchNo;

    /** 钢丝圈批次号 */
    @Excel(name = "ui.data.column.gsqScheduleResult.batchNo")
    @ApiModelProperty(value = "钢丝圈批次号", name = "batchNo")
    @TableField(value = "BATCH_NO")
    private String batchNo;

    /** 工单号 */
    @Excel(name = "ui.data.column.gsqScheduleResult.orderNo")
    @ApiModelProperty(value = "工单号", name = "orderNo")
    @TableField(value = "ORDER_NO")
    private String orderNo;

    /** 钢丝圈代码 */
    @Excel(name = "ui.data.column.gsqScheduleResult.steelRingCode")
    @ApiModelProperty(value = "钢丝圈代码", name = "steelRingCode")
    @TableField(value = "STEEL_RING_CODE")
    private String steelRingCode;

    /** 钢丝缠绕盘代码 */
    @Excel(name = "ui.data.column.gsqScheduleResult.twiningDiscCode")
    @ApiModelProperty(value = "钢丝缠绕盘代码", name = "twiningDiscCode")
    @TableField(value = "TWINING_DISC_CODE")
    private String twiningDiscCode;

    /** 英寸 */
    @Excel(name = "ui.data.column.gsqScheduleResult.proSize")
    @ApiModelProperty(value = "英寸", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private String proSize;

    /** 机台编号 */
    @Excel(name = "ui.data.column.gsqScheduleResult.machineCode")
    @ApiModelProperty(value = "机台编号", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    /** 月计划剩余量 */
    @Excel(name = "ui.data.column.gsqScheduleResult.monthSurplusQty")
    @ApiModelProperty(value = "月计划剩余量", name = "monthSurplusQty")
    @TableField(value = "MONTH_SURPLUS_QTY")
    private Integer monthSurplusQty;

    // ==================== 6个班次的顺序/计划量/完成量/原因分析 ====================

    /** 1班顺序 */
    @Excel(name = "ui.data.column.gsqScheduleResult.class1Sequence")
    @ApiModelProperty(value = "1班顺序", name = "class1Sequence")
    @TableField(value = "CLASS1_SEQUENCE")
    private Integer class1Sequence;

    /** 1班计划量 */
    @Excel(name = "ui.data.column.gsqScheduleResult.class1PlanQty")
    @ApiModelProperty(value = "1班计划量", name = "class1PlanQty")
    @TableField(value = "CLASS1_PLAN_QTY")
    private Integer class1PlanQty;

    /** 1班完成量 */
    @Excel(name = "ui.data.column.gsqScheduleResult.class1FinishQty")
    @ApiModelProperty(value = "1班完成量", name = "class1FinishQty")
    @TableField(value = "CLASS1_FINISH_QTY")
    private Integer class1FinishQty;

    /** 1班原因分析 */
    @Excel(name = "ui.data.column.gsqScheduleResult.class1Analysis")
    @ApiModelProperty(value = "1班原因分析", name = "class1Analysis")
    @TableField(value = "CLASS1_ANALYSIS")
    private String class1Analysis;

    /** 2班顺序 */
    @Excel(name = "ui.data.column.gsqScheduleResult.class2Sequence")
    @ApiModelProperty(value = "2班顺序", name = "class2Sequence")
    @TableField(value = "CLASS2_SEQUENCE")
    private Integer class2Sequence;

    /** 2班计划量 */
    @Excel(name = "ui.data.column.gsqScheduleResult.class2PlanQty")
    @ApiModelProperty(value = "2班计划量", name = "class2PlanQty")
    @TableField(value = "CLASS2_PLAN_QTY")
    private Integer class2PlanQty;

    /** 2班完成量 */
    @Excel(name = "ui.data.column.gsqScheduleResult.class2FinishQty")
    @ApiModelProperty(value = "2班完成量", name = "class2FinishQty")
    @TableField(value = "CLASS2_FINISH_QTY")
    private Integer class2FinishQty;

    /** 2班原因分析 */
    @Excel(name = "ui.data.column.gsqScheduleResult.class2Analysis")
    @ApiModelProperty(value = "2班原因分析", name = "class2Analysis")
    @TableField(value = "CLASS2_ANALYSIS")
    private String class2Analysis;

    /** 3班顺序 */
    @Excel(name = "ui.data.column.gsqScheduleResult.class3Sequence")
    @ApiModelProperty(value = "3班顺序", name = "class3Sequence")
    @TableField(value = "CLASS3_SEQUENCE")
    private Integer class3Sequence;

    /** 3班计划量 */
    @Excel(name = "ui.data.column.gsqScheduleResult.class3PlanQty")
    @ApiModelProperty(value = "3班计划量", name = "class3PlanQty")
    @TableField(value = "CLASS3_PLAN_QTY")
    private Integer class3PlanQty;

    /** 3班完成量 */
    @Excel(name = "ui.data.column.gsqScheduleResult.class3FinishQty")
    @ApiModelProperty(value = "3班完成量", name = "class3FinishQty")
    @TableField(value = "CLASS3_FINISH_QTY")
    private Integer class3FinishQty;

    /** 3班原因分析 */
    @Excel(name = "ui.data.column.gsqScheduleResult.class3Analysis")
    @ApiModelProperty(value = "3班原因分析", name = "class3Analysis")
    @TableField(value = "CLASS3_ANALYSIS")
    private String class3Analysis;

    /** 4班顺序 */
    @Excel(name = "ui.data.column.gsqScheduleResult.class4Sequence")
    @ApiModelProperty(value = "4班顺序", name = "class4Sequence")
    @TableField(value = "CLASS4_SEQUENCE")
    private Integer class4Sequence;

    /** 4班计划量 */
    @Excel(name = "ui.data.column.gsqScheduleResult.class4PlanQty")
    @ApiModelProperty(value = "4班计划量", name = "class4PlanQty")
    @TableField(value = "CLASS4_PLAN_QTY")
    private Integer class4PlanQty;

    /** 4班完成量 */
    @Excel(name = "ui.data.column.gsqScheduleResult.class4FinishQty")
    @ApiModelProperty(value = "4班完成量", name = "class4FinishQty")
    @TableField(value = "CLASS4_FINISH_QTY")
    private Integer class4FinishQty;

    /** 4班原因分析 */
    @Excel(name = "ui.data.column.gsqScheduleResult.class4Analysis")
    @ApiModelProperty(value = "4班原因分析", name = "class4Analysis")
    @TableField(value = "CLASS4_ANALYSIS")
    private String class4Analysis;

    /** 5班顺序 */
    @Excel(name = "ui.data.column.gsqScheduleResult.class5Sequence")
    @ApiModelProperty(value = "5班顺序", name = "class5Sequence")
    @TableField(value = "CLASS5_SEQUENCE")
    private Integer class5Sequence;

    /** 5班计划量 */
    @Excel(name = "ui.data.column.gsqScheduleResult.class5PlanQty")
    @ApiModelProperty(value = "5班计划量", name = "class5PlanQty")
    @TableField(value = "CLASS5_PLAN_QTY")
    private Integer class5PlanQty;

    /** 5班完成量 */
    @Excel(name = "ui.data.column.gsqScheduleResult.class5FinishQty")
    @ApiModelProperty(value = "5班完成量", name = "class5FinishQty")
    @TableField(value = "CLASS5_FINISH_QTY")
    private Integer class5FinishQty;

    /** 5班原因分析 */
    @Excel(name = "ui.data.column.gsqScheduleResult.class5Analysis")
    @ApiModelProperty(value = "5班原因分析", name = "class5Analysis")
    @TableField(value = "CLASS5_ANALYSIS")
    private String class5Analysis;

    /** 6班顺序 */
    @Excel(name = "ui.data.column.gsqScheduleResult.class6Sequence")
    @ApiModelProperty(value = "6班顺序", name = "class6Sequence")
    @TableField(value = "CLASS6_SEQUENCE")
    private Integer class6Sequence;

    /** 6班计划量 */
    @Excel(name = "ui.data.column.gsqScheduleResult.class6PlanQty")
    @ApiModelProperty(value = "6班计划量", name = "class6PlanQty")
    @TableField(value = "CLASS6_PLAN_QTY")
    private Integer class6PlanQty;

    /** 6班完成量 */
    @Excel(name = "ui.data.column.gsqScheduleResult.class6FinishQty")
    @ApiModelProperty(value = "6班完成量", name = "class6FinishQty")
    @TableField(value = "CLASS6_FINISH_QTY")
    private Integer class6FinishQty;

    /** 6班原因分析 */
    @Excel(name = "ui.data.column.gsqScheduleResult.class6Analysis")
    @ApiModelProperty(value = "6班原因分析", name = "class6Analysis")
    @TableField(value = "CLASS6_ANALYSIS")
    private String class6Analysis;

    /** 是否发布 */
    @Excel(name = "ui.data.column.gsqScheduleResult.isRelease", dictType = "IS_RELEASE")
    @ApiModelProperty(value = "是否发布", name = "isRelease")
    @TableField(value = "IS_RELEASE")
    private String isRelease;

    /** 发布成功次数（每点击一次发布并成功的话，计数器累加；删除前校验：必须等于0才允许删除） */
    @Excel(name = "ui.data.column.gsqScheduleResult.publishSuccessCount")
    @ApiModelProperty(value = "发布成功次数", name = "publishSuccessCount")
    @TableField(value = "PUBLISH_SUCCESS_COUNT")
    private Integer publishSuccessCount;

    /** MES计划ID（下发MES成功后回写的MES侧计划ID；非空表示已发送给MES，不允许删除，只能调量） */
    @ApiModelProperty(value = "MES计划ID", name = "mesId")
    @TableField(value = "MES_ID")
    private Long mesId;

    /** 库存量 */
    @Excel(name = "ui.data.column.gsqScheduleResult.stockQty")
    @ApiModelProperty(value = "库存量", name = "stockQty")
    @TableField(value = "STOCK_QTY")
    private Integer stockQty;

    /** 数据来源：0-自动排程，1-插单，2-导入 */
    @Excel(name = "ui.data.column.gsqScheduleResult.dataSource", dictType = "GSQ_DATA_SOURCE")
    @ApiModelProperty(value = "数据来源：0-自动排程，1-插单，2-导入", name = "dataSource")
    @TableField(value = "DATA_SOURCE")
    private String dataSource;

    /** 收尾规格标记，0：收尾，1：非收尾 */
    @Excel(name = "ui.data.column.gsqScheduleResult.closeOutSpecFlag", dictType = "CLOSE_OUT_SPEC_FLAG")
    @ApiModelProperty(value = "收尾规格标记，0：收尾，1：非收尾", name = "closeOutSpecFlag")
    @TableField(value = "CLOSE_OUT_SPEC_FLAG")
    private String closeOutSpecFlag;

    /** 分厂 */
    @Excel(name = "ui.data.column.gsqScheduleResult.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 排程解释JSON（结构化规则证据），Phase 2 重构新增 */
    @ApiModelProperty(value = "排程解释JSON（结构化规则证据）", name = "explainJson")
    @TableField(value = "EXPLAIN_JSON")
    private String explainJson;

    // ==================== 对应胎圈1~6班消耗量（回填自胎圈排程结果） ====================

    /** 对应胎圈1班消耗量 */
    @ApiModelProperty(value = "对应胎圈1班消耗量", name = "tqClass1Plan")
    @TableField(value = "TQ_CLASS1_PLAN")
    private Integer tqClass1Plan;

    /** 对应胎圈2班消耗量 */
    @ApiModelProperty(value = "对应胎圈2班消耗量", name = "tqClass2Plan")
    @TableField(value = "TQ_CLASS2_PLAN")
    private Integer tqClass2Plan;

    /** 对应胎圈3班消耗量 */
    @ApiModelProperty(value = "对应胎圈3班消耗量", name = "tqClass3Plan")
    @TableField(value = "TQ_CLASS3_PLAN")
    private Integer tqClass3Plan;

    /** 对应胎圈4班消耗量 */
    @ApiModelProperty(value = "对应胎圈4班消耗量", name = "tqClass4Plan")
    @TableField(value = "TQ_CLASS4_PLAN")
    private Integer tqClass4Plan;

    /** 对应胎圈5班消耗量 */
    @ApiModelProperty(value = "对应胎圈5班消耗量", name = "tqClass5Plan")
    @TableField(value = "TQ_CLASS5_PLAN")
    private Integer tqClass5Plan;

    /** 对应胎圈6班消耗量 */
    @ApiModelProperty(value = "对应胎圈6班消耗量", name = "tqClass6Plan")
    @TableField(value = "TQ_CLASS6_PLAN")
    private Integer tqClass6Plan;

    // ==================== 6个班次的预计开始/结束时间和任务状态（滚动更新使用） ====================

    /** 1班预计开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "1班预计开始时间", name = "class1StartTime")
    @TableField(value = "CLASS1_START_TIME")
    private Date class1StartTime;

    /** 1班预计结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "1班预计结束时间", name = "class1EndTime")
    @TableField(value = "CLASS1_END_TIME")
    private Date class1EndTime;

    /** 1班任务状态（0-正常，1-已取消，2-已推迟，3-部分完成推迟） */
    @Excel(name = "ui.data.column.gsqScheduleResult.class1TaskStatus", dictType = "GSQ_TASK_STATUS")
    @ApiModelProperty(value = "1班任务状态（0-正常，1-已取消，2-已推迟，3-部分完成推迟）", name = "class1TaskStatus")
    @TableField(value = "CLASS1_TASK_STATUS")
    private String class1TaskStatus;

    /** 2班预计开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "2班预计开始时间", name = "class2StartTime")
    @TableField(value = "CLASS2_START_TIME")
    private Date class2StartTime;

    /** 2班预计结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "2班预计结束时间", name = "class2EndTime")
    @TableField(value = "CLASS2_END_TIME")
    private Date class2EndTime;

    /** 2班任务状态（0-正常，1-已取消，2-已推迟，3-部分完成推迟） */
    @Excel(name = "ui.data.column.gsqScheduleResult.class2TaskStatus", dictType = "GSQ_TASK_STATUS")
    @ApiModelProperty(value = "2班任务状态（0-正常，1-已取消，2-已推迟，3-部分完成推迟）", name = "class2TaskStatus")
    @TableField(value = "CLASS2_TASK_STATUS")
    private String class2TaskStatus;

    /** 3班预计开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "3班预计开始时间", name = "class3StartTime")
    @TableField(value = "CLASS3_START_TIME")
    private Date class3StartTime;

    /** 3班预计结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "3班预计结束时间", name = "class3EndTime")
    @TableField(value = "CLASS3_END_TIME")
    private Date class3EndTime;

    /** 3班任务状态（0-正常，1-已取消，2-已推迟，3-部分完成推迟） */
    @Excel(name = "ui.data.column.gsqScheduleResult.class3TaskStatus", dictType = "GSQ_TASK_STATUS")
    @ApiModelProperty(value = "3班任务状态（0-正常，1-已取消，2-已推迟，3-部分完成推迟）", name = "class3TaskStatus")
    @TableField(value = "CLASS3_TASK_STATUS")
    private String class3TaskStatus;

    /** 4班预计开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "4班预计开始时间", name = "class4StartTime")
    @TableField(value = "CLASS4_START_TIME")
    private Date class4StartTime;

    /** 4班预计结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "4班预计结束时间", name = "class4EndTime")
    @TableField(value = "CLASS4_END_TIME")
    private Date class4EndTime;

    /** 4班任务状态（0-正常，1-已取消，2-已推迟，3-部分完成推迟） */
    @Excel(name = "ui.data.column.gsqScheduleResult.class4TaskStatus", dictType = "GSQ_TASK_STATUS")
    @ApiModelProperty(value = "4班任务状态（0-正常，1-已取消，2-已推迟，3-部分完成推迟）", name = "class4TaskStatus")
    @TableField(value = "CLASS4_TASK_STATUS")
    private String class4TaskStatus;

    /** 5班预计开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "5班预计开始时间", name = "class5StartTime")
    @TableField(value = "CLASS5_START_TIME")
    private Date class5StartTime;

    /** 5班预计结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "5班预计结束时间", name = "class5EndTime")
    @TableField(value = "CLASS5_END_TIME")
    private Date class5EndTime;

    /** 5班任务状态（0-正常，1-已取消，2-已推迟，3-部分完成推迟） */
    @Excel(name = "ui.data.column.gsqScheduleResult.class5TaskStatus", dictType = "GSQ_TASK_STATUS")
    @ApiModelProperty(value = "5班任务状态（0-正常，1-已取消，2-已推迟，3-部分完成推迟）", name = "class5TaskStatus")
    @TableField(value = "CLASS5_TASK_STATUS")
    private String class5TaskStatus;

    /** 6班预计开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "6班预计开始时间", name = "class6StartTime")
    @TableField(value = "CLASS6_START_TIME")
    private Date class6StartTime;

    /** 6班预计结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "6班预计结束时间", name = "class6EndTime")
    @TableField(value = "CLASS6_END_TIME")
    private Date class6EndTime;

    /** 6班任务状态（0-正常，1-已取消，2-已推迟，3-部分完成推迟） */
    @Excel(name = "ui.data.column.gsqScheduleResult.class6TaskStatus", dictType = "GSQ_TASK_STATUS")
    @ApiModelProperty(value = "6班任务状态（0-正常，1-已取消，2-已推迟，3-部分完成推迟）", name = "class6TaskStatus")
    @TableField(value = "CLASS6_TASK_STATUS")
    private String class6TaskStatus;

    // ==================== 非数据库字段（查询参数） ====================

    /** 排程日期查询（精确匹配，非数据库字段） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField(exist = false)
    private Date scheduleDateQuery;

    /** 选中的记录ID列表（逗号分隔，非数据库字段，用于发布时按勾选记录过滤） */
    @TableField(exist = false)
    private String ids;

    /**
     * 按班次字段模板动态读取字段值（用于 class1~6PlanQty/FinishQty/Analysis/Sequence 等批量字段访问）。
     * 遵循项目规范：禁止使用 switch/case 硬编码访问班次字段。
     *
     * @param fieldName Java 字段名（如 "class1PlanQty"）
     * @return 字段值
     */
    public Serializable getFieldValueByFieldName(String fieldName) {
        try {
            Field field = this.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (Serializable) field.get(this);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("钢丝圈排程结果字段不存在: " + fieldName, exception);
        }
    }

    /**
     * 按班次字段模板动态写入字段值。
     *
     * @param fieldName Java 字段名
     * @param value     字段值
     */
    public void setFieldValueByFieldName(String fieldName, Object value) {
        try {
            Field field = this.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(this, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("钢丝圈排程结果字段不存在: " + fieldName, exception);
        }
    }
}
