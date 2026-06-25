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

import java.io.Serializable;
import java.util.Date;

/**
 * 胎圈排程滚动更新日志明细实体
 *
 * <p>记录每次滚动更新中，每条排程记录的变更前后值，支持变更追溯审计。</p>
 * <p>关联 T_TQ_ROLLING_LOG 主表（一对多）。</p>
 *
 * @author APS
 * @since 2026-06-22
 */
@ApiModel(value = "胎圈排程滚动更新日志明细", description = "胎圈排程滚动更新日志明细表")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_TQ_ROLLING_LOG_DETAIL")
public class TqRollingLogDetail extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关联主表ID（T_TQ_ROLLING_LOG.ID） */
    @ApiModelProperty(value = "关联主表ID", name = "logId")
    @TableField(value = "LOG_ID")
    private Long logId;

    /** 排程记录ID（T_TQ_SCHEDULE_RESULT.ID） */
    @ApiModelProperty(value = "排程记录ID", name = "scheduleId")
    @TableField(value = "SCHEDULE_ID")
    private Long scheduleId;

    /** 机台编号 */
    @Excel(name = "ui.data.column.tqRollingLogDetail.machineCode")
    @ApiModelProperty(value = "机台编号", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    /** 胎圈代码 */
    @Excel(name = "ui.data.column.tqRollingLogDetail.beadCode")
    @ApiModelProperty(value = "胎圈代码", name = "beadCode")
    @TableField(value = "BEAD_CODE")
    private String beadCode;

    /** 班次索引（1~6） */
    @Excel(name = "ui.data.column.tqRollingLogDetail.shiftIndex")
    @ApiModelProperty(value = "班次索引（1~6）", name = "shiftIndex")
    @TableField(value = "SHIFT_INDEX")
    private Integer shiftIndex;

    /** 变更字段（START_TIME/END_TIME/SEQUENCE/TASK_STATUS/PLAN_QTY） */
    @Excel(name = "ui.data.column.tqRollingLogDetail.fieldName")
    @ApiModelProperty(value = "变更字段", name = "fieldName")
    @TableField(value = "FIELD_NAME")
    private String fieldName;

    /** 变更前值 */
    @Excel(name = "ui.data.column.tqRollingLogDetail.beforeValue")
    @ApiModelProperty(value = "变更前值", name = "beforeValue")
    @TableField(value = "BEFORE_VALUE")
    private String beforeValue;

    /** 变更后值 */
    @Excel(name = "ui.data.column.tqRollingLogDetail.afterValue")
    @ApiModelProperty(value = "变更后值", name = "afterValue")
    @TableField(value = "AFTER_VALUE")
    private String afterValue;

    /** 变更类型：1-时间，2-顺序，3-状态，4-计划量 */
    @Excel(name = "ui.data.column.tqRollingLogDetail.changeType", dictType = "TQ_ROLLING_CHANGE_TYPE")
    @ApiModelProperty(value = "变更类型：1-时间，2-顺序，3-状态，4-计划量", name = "changeType")
    @TableField(value = "CHANGE_TYPE")
    private String changeType;

    /** 变更原因 */
    @Excel(name = "ui.data.column.tqRollingLogDetail.changeReason")
    @ApiModelProperty(value = "变更原因", name = "changeReason")
    @TableField(value = "CHANGE_REASON")
    private String changeReason;

    /** 分公司编码 */
    @ApiModelProperty(value = "分公司编码", name = "companyCode")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    /** 厂别 */
    @Excel(name = "ui.data.column.tqRollingLogDetail.factoryCode")
    @ApiModelProperty(value = "厂别", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;
}
