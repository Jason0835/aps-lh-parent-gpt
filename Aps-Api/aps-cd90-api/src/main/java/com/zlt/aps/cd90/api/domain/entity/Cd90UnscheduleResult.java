package com.zlt.aps.cd90.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** 直裁自动排程未排结果。 */
@Data
@TableName("t_cd90_unschedule_result")
public class Cd90UnscheduleResult extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableField("FACTORY_CODE") private String factoryCode;
    @TableField("SCHEDULE_DATE") private Date scheduleDate;
    @TableField("CLOTH_CODE") private String clothCode;
    @TableField("BIG_ROLL_CODE") private String bigRollCode;
    @TableField("DEMAND_QTY") private Double demandQty;
    @TableField("SCHEDULED_QTY") private Double scheduledQty;
    @TableField("UNSCHEDULED_QTY") private Double unscheduledQty;
    @TableField("FAIL_STAGE") private String failStage;
    @TableField("REASON_CODE") private String reasonCode;
    @TableField("REASON_ORDER") private Integer reasonOrder;
    @TableField("IS_PRIMARY_REASON") private String primaryReason;
    @TableField("UNSCHEDULED_REASON") private String unscheduledReason;
    @TableField("CANDIDATE_MACHINE_CODES") private String candidateMachineCodes;
    @TableField("BATCH_NO") private String batchNo;
    @TableField("DATA_SOURCE") private String dataSource;
    @TableField("PROCESSED_TIME") private Date processedTime;
}
