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

import java.math.BigDecimal;
import java.util.Date;

/**
 * 胎圈排程预警记录实体类
 *
 * <p>用于记录胎圈排程过程中触发的库存预警和完成量预警信息，
 * 支持预警追溯、处理状态追踪和统计分析。</p>
 *
 * @author APS
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_TQ_WARNING_RECORD")
@ApiModel(value = "胎圈排程预警记录", description = "胎圈排程预警记录")
public class TqWarningRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编码
     */
    @Excel(name = "ui.data.column.tqWarningRecord.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
    @TableField("FACTORY_CODE")
    private String factoryCode;

    /**
     * 预警类型：1-库存预警 2-完成量预警
     */
    @Excel(name = "ui.data.column.tqWarningRecord.warningType", dictType = "tq_warning_type")
    @ApiModelProperty(value = "预警类型：1-库存预警 2-完成量预警", name = "warningType")
    @TableField("WARNING_TYPE")
    private String warningType;

    /**
     * 胎圈编码
     */
    @Excel(name = "ui.data.column.tqWarningRecord.beadCode")
    @ApiModelProperty(value = "胎圈编码", name = "beadCode")
    @TableField("BEAD_CODE")
    private String beadCode;

    /**
     * 机台编码（完成量预警时使用）
     */
    @Excel(name = "ui.data.column.tqWarningRecord.machineCode")
    @ApiModelProperty(value = "机台编码", name = "machineCode")
    @TableField("MACHINE_CODE")
    private String machineCode;

    /**
     * 排程日期（完成量预警时使用）
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @Excel(name = "ui.data.column.tqWarningRecord.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @TableField("SCHEDULE_DATE")
    private Date scheduleDate;

    /**
     * 班次索引（完成量预警时使用，1~6）
     */
    @Excel(name = "ui.data.column.tqWarningRecord.shiftIndex")
    @ApiModelProperty(value = "班次索引", name = "shiftIndex")
    @TableField("SHIFT_INDEX")
    private Integer shiftIndex;

    /**
     * 预警级别：1-低 2-中 3-高
     */
    @Excel(name = "ui.data.column.tqWarningRecord.warningLevel", dictType = "warning_level")
    @ApiModelProperty(value = "预警级别：1-低 2-中 3-高", name = "warningLevel")
    @TableField("WARNING_LEVEL")
    private String warningLevel;

    /**
     * 预警标题
     */
    @Excel(name = "ui.data.column.tqWarningRecord.warningTitle")
    @ApiModelProperty(value = "预警标题", name = "warningTitle")
    @TableField("WARNING_TITLE")
    private String warningTitle;

    /**
     * 预警内容
     */
    @Excel(name = "ui.data.column.tqWarningRecord.warningContent")
    @ApiModelProperty(value = "预警内容", name = "warningContent")
    @TableField("WARNING_CONTENT")
    private String warningContent;

    /**
     * 计划量（完成量预警时使用）
     */
    @Excel(name = "ui.data.column.tqWarningRecord.planQty")
    @ApiModelProperty(value = "计划量", name = "planQty")
    @TableField("PLAN_QTY")
    private Integer planQty;

    /**
     * 完成量（完成量预警时使用）
     */
    @Excel(name = "ui.data.column.tqWarningRecord.finishQty")
    @ApiModelProperty(value = "完成量", name = "finishQty")
    @TableField("FINISH_QTY")
    private BigDecimal finishQty;

    /**
     * 完成率（完成量预警时使用）
     */
    @Excel(name = "ui.data.column.tqWarningRecord.finishRate")
    @ApiModelProperty(value = "完成率", name = "finishRate")
    @TableField("FINISH_RATE")
    private BigDecimal finishRate;

    /**
     * 库存量（库存预警时使用）
     */
    @Excel(name = "ui.data.column.tqWarningRecord.stockNum")
    @ApiModelProperty(value = "库存量", name = "stockNum")
    @TableField("STOCK_NUM")
    private BigDecimal stockNum;

    /**
     * 预警阈值（库存预警阈值或完成量预警比例）
     */
    @Excel(name = "ui.data.column.tqWarningRecord.threshold")
    @ApiModelProperty(value = "预警阈值", name = "threshold")
    @TableField("THRESHOLD")
    private BigDecimal threshold;

    /**
     * 预警数据JSON（完整的预警上下文数据）
     */
    @Excel(name = "ui.data.column.tqWarningRecord.warningData")
    @ApiModelProperty(value = "预警数据JSON", name = "warningData")
    @TableField("WARNING_DATA")
    private String warningData;

    /**
     * 处理状态：0-未处理 1-已处理 2-处理中
     */
    @Excel(name = "ui.data.column.tqWarningRecord.status", dictType = "warning_status")
    @ApiModelProperty(value = "处理状态：0-未处理 1-已处理 2-处理中", name = "status")
    @TableField("STATUS")
    private String status;

    /**
     * 处理人
     */
    @Excel(name = "ui.data.column.tqWarningRecord.handler")
    @ApiModelProperty(value = "处理人", name = "handler")
    @TableField("HANDLER")
    private String handler;

    /**
     * 处理时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Excel(name = "ui.data.column.tqWarningRecord.handleTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "处理时间", name = "handleTime")
    @TableField("HANDLE_TIME")
    private Date handleTime;

    /**
     * 处理意见
     */
    @Excel(name = "ui.data.column.tqWarningRecord.handleOpinion")
    @ApiModelProperty(value = "处理意见", name = "handleOpinion")
    @TableField("HANDLE_OPINION")
    private String handleOpinion;

    /**
     * 是否已通知：0-否 1-是
     */
    @Excel(name = "ui.data.column.tqWarningRecord.notified", dictType = "sys_yes_no")
    @ApiModelProperty(value = "是否已通知：0-否 1-是", name = "notified")
    @TableField("NOTIFIED")
    private Integer notified;

    /**
     * 通知时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Excel(name = "ui.data.column.tqWarningRecord.notifyTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "通知时间", name = "notifyTime")
    @TableField("NOTIFY_TIME")
    private Date notifyTime;
}
