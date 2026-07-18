package com.zlt.aps.cd15.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** 斜裁自动排程参数快照。 */
@Data
@TableName("t_cd15_schedule_param_snapshot")
public class Cd15ScheduleParamSnapshot extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 工厂编码。 */
    @TableField("FACTORY_CODE")
    private String factoryCode;
    /** 排程日期。 */
    @TableField("SCHEDULE_DATE")
    private Date scheduleDate;
    /** 斜裁排程批次号。 */
    @TableField("BATCH_NO")
    private String batchNo;
    /** 参数编码。 */
    @TableField("PARAM_CODE")
    private String paramCode;
    /** 本批次使用的参数值。 */
    @TableField("PARAM_VALUE")
    private String paramValue;
    /** 整体参数指纹。 */
    @TableField("PARAM_FINGERPRINT")
    private String paramFingerprint;
}
