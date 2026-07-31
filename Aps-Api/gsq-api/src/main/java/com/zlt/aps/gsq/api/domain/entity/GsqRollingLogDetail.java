package com.zlt.aps.gsq.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 钢丝圈排程滚动更新日志明细实体
 *
 * <p>记录每次滚动更新中，每条排程记录的变更前后值，支持变更追溯审计。</p>
 * <p>关联 T_GSQ_ROLLING_LOG 主表（一对多）。</p>
 *
 * @author APS
 * @since 2026-07-20
 */
@ApiModel(value = "钢丝圈排程滚动更新日志明细", description = "钢丝圈排程滚动更新日志明细表")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_GSQ_ROLLING_LOG_DETAIL")
public class GsqRollingLogDetail extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关联主表ID（T_GSQ_ROLLING_LOG.ID） */
    @ApiModelProperty(value = "关联主表ID", name = "logId")
    @TableField(value = "LOG_ID")
    private Long logId;

    /** 排程记录ID（T_GSQ_SCHEDULE_RESULT.ID） */
    @ApiModelProperty(value = "排程记录ID", name = "scheduleId")
    @TableField(value = "SCHEDULE_ID")
    private Long scheduleId;

    /** 机台编号 */
    @Excel(name = "ui.data.column.gsqRollingLogDetail.machineCode")
    @ApiModelProperty(value = "机台编号", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    /** 钢丝圈代码 */
    @Excel(name = "ui.data.column.gsqRollingLogDetail.steelRingCode")
    @ApiModelProperty(value = "钢丝圈代码", name = "steelRingCode")
    @TableField(value = "STEEL_RING_CODE")
    private String steelRingCode;

    /** 班次索引（1~6） */
    @Excel(name = "ui.data.column.gsqRollingLogDetail.shiftIndex")
    @ApiModelProperty(value = "班次索引（1~6）", name = "shiftIndex")
    @TableField(value = "SHIFT_INDEX")
    private Integer shiftIndex;

    /** 变更字段（START_TIME/END_TIME/SEQUENCE/TASK_STATUS/PLAN_QTY） */
    @Excel(name = "ui.data.column.gsqRollingLogDetail.fieldName")
    @ApiModelProperty(value = "变更字段", name = "fieldName")
    @TableField(value = "FIELD_NAME")
    private String fieldName;

    /** 变更前值 */
    @Excel(name = "ui.data.column.gsqRollingLogDetail.beforeValue")
    @ApiModelProperty(value = "变更前值", name = "beforeValue")
    @TableField(value = "BEFORE_VALUE")
    private String beforeValue;

    /** 变更后值 */
    @Excel(name = "ui.data.column.gsqRollingLogDetail.afterValue")
    @ApiModelProperty(value = "变更后值", name = "afterValue")
    @TableField(value = "AFTER_VALUE")
    private String afterValue;

    /** 变更类型：1-时间，2-顺序，3-状态，4-计划量 */
    @Excel(name = "ui.data.column.gsqRollingLogDetail.changeType", dictType = "TQ_ROLLING_CHANGE_TYPE")
    @ApiModelProperty(value = "变更类型：1-时间，2-顺序，3-状态，4-计划量", name = "changeType")
    @TableField(value = "CHANGE_TYPE")
    private String changeType;

    /** 变更原因 */
    @Excel(name = "ui.data.column.gsqRollingLogDetail.changeReason")
    @ApiModelProperty(value = "变更原因", name = "changeReason")
    @TableField(value = "CHANGE_REASON")
    private String changeReason;

    /** 分公司编码 */
    @ApiModelProperty(value = "分公司编码", name = "companyCode")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    /** 厂别 */
    @Excel(name = "ui.data.column.gsqRollingLogDetail.factoryCode")
    @ApiModelProperty(value = "厂别", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;
}
