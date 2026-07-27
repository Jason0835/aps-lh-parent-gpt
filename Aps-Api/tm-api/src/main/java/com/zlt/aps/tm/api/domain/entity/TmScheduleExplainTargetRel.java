package com.zlt.aps.tm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 胎面来源解释与排程结果或未排片段关联实体。
 */
@Data
@TableName("T_TM_SCHEDULE_EXPLAIN_TARGET_REL")
public class TmScheduleExplainTargetRel extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 工厂编码 */
    @TableField("FACTORY_CODE")
    private String factoryCode;

    /** 批次号 */
    @TableField("BATCH_NO")
    private String batchNo;

    /** 排程日期 */
    @TableField("SCHEDULE_DATE")
    private Date scheduleDate;

    /** 来源解释主键 */
    @TableField("EXPLAIN_ID")
    private Long explainId;

    /** 计划量汇总组业务键 */
    @TableField("PLAN_GROUP_KEY")
    private String planGroupKey;

    /** 来源任务业务键 */
    @TableField("SOURCE_TASK_BUSINESS_KEY")
    private String sourceTaskBusinessKey;

    /** 目标类型，RESULT 或 UNPLANNED */
    @TableField("TARGET_TYPE")
    private String targetType;

    /** 结果或未排记录主键 */
    @TableField("TARGET_ID")
    private Long targetId;

    /** 拆分目标任务业务键 */
    @TableField("TARGET_BUSINESS_KEY")
    private String targetBusinessKey;

    /** 目标班次顺序 */
    @TableField("SHIFT_ORDER")
    private Integer shiftOrder;

    /** 目标机台编码 */
    @TableField("MACHINE_CODE")
    private String machineCode;

    /** 当前来源分摊到目标片段的数量 */
    @TableField("ALLOCATED_QTY")
    private BigDecimal allocatedQty;
}
