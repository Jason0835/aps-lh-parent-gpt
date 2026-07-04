package com.zlt.aps.cd90.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** 直裁定时滚动调整日志。 */
@Data
@ApiModel(value = "直裁定时滚动调整日志", description = "记录滚动前后差异和完整快照")
@TableName("t_cd90_schedule_rolling_adjust_log")
public class Cd90ScheduleRollingAdjustLog extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编码。 */
    @TableField("FACTORY_CODE") private String factoryCode;
    /** 滚动任务ID。 */
    @TableField("TASK_ID") private String taskId;
    /** 原排程批次号。 */
    @TableField("BATCH_NO") private String batchNo;
    /** 排程日期。 */
    @TableField("SCHEDULE_DATE") private Date scheduleDate;
    /** 目标班次编码。 */
    @TableField("TARGET_SHIFT_CODE") private String targetShiftCode;
    /** 滚动任务项稳定业务键。 */
    @TableField("ROLLING_ITEM_KEY") private String rollingItemKey;
    /** 对应排程结果ID。 */
    @TableField("SCHEDULE_RESULT_ID") private Long scheduleResultId;
    /** 帘布代号。 */
    @TableField("CLOTH_CODE") private String clothCode;
    /** 大卷编号。 */
    @TableField("BIG_ROLL_CODE") private String bigRollCode;
    /** 调整类型。 */
    @TableField("ADJUST_TYPE") private String adjustType;
    /** 调整前班次序号。 */
    @TableField("OLD_CLASS_INDEX") private Integer oldClassIndex;
    /** 调整后班次序号。 */
    @TableField("NEW_CLASS_INDEX") private Integer newClassIndex;
    /** 调整前生产顺序。 */
    @TableField("OLD_PRODUCE_ORDER") private Integer oldProduceOrder;
    /** 调整后生产顺序。 */
    @TableField("NEW_PRODUCE_ORDER") private Integer newProduceOrder;
    /** 调整前计划量。 */
    @TableField("OLD_PLAN_QTY") private BigDecimal oldPlanQty;
    /** 调整后计划量。 */
    @TableField("NEW_PLAN_QTY") private BigDecimal newPlanQty;
    /** 调整前机台。 */
    @TableField("OLD_MACHINE_CODE") private String oldMachineCode;
    /** 调整后机台。 */
    @TableField("NEW_MACHINE_CODE") private String newMachineCode;
    /** 调整原因编码。 */
    @TableField("REASON_CODE") private String reasonCode;
    /** 决策及限制原因JSON。 */
    @TableField("REASON_DETAIL") private String reasonDetail;
    /** 本次滚动输入版本。 */
    @TableField("INPUT_VERSION") private String inputVersion;
    /** 快照结构版本。 */
    @TableField("SNAPSHOT_SCHEMA_VERSION") private String snapshotSchemaVersion;
    /** 调整前完整快照JSON。 */
    @TableField("BEFORE_SNAPSHOT_JSON") private String beforeSnapshotJson;
    /** 调整后完整快照JSON。 */
    @TableField("AFTER_SNAPSHOT_JSON") private String afterSnapshotJson;
}
