package com.zlt.aps.cd90.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** 直裁自动排程参数快照。 */
@Data
@TableName("t_cd90_schedule_param_snapshot")
public class Cd90ScheduleParamSnapshot extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableField("FACTORY_CODE") private String factoryCode;
    @TableField("SCHEDULE_DATE") private Date scheduleDate;
    @TableField("BATCH_NO") private String batchNo;
    @TableField("PARAM_CODE") private String paramCode;
    @TableField("PARAM_VALUE") private String paramValue;
    @TableField("PARAM_FINGERPRINT") private String paramFingerprint;

    /** 备注 */
    @ApiModelProperty(value = "备注", name = "remark")
    @TableField("REMARK")
    @Excel(name = "ui.common.column.remark")
    private String remark;
}
