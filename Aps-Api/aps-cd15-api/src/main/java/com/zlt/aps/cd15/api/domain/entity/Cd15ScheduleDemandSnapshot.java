package com.zlt.aps.cd15.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** 斜裁自动排程逐班需求快照。 */
@Data
@TableName("t_cd15_schedule_demand_snapshot")
public class Cd15ScheduleDemandSnapshot extends BaseEntity implements Serializable {
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
    /** 钢带代码。 */
    @TableField("STEEL_STRIP_CODE")
    private String steelStripCode;
    /** 大卷代码。 */
    @TableField("BIG_ROLL_CODE")
    private String bigRollCode;
    /** 裁断角度。 */
    @TableField("CUTTING_ANGLE")
    private String cuttingAngle;
    /** 斜裁输出班次字段。 */
    @TableField("CLASS_FIELD")
    private String classField;
    /** 成型需求自然班次时间。 */
    @TableField("DEMAND_TIME")
    private Date demandTime;
    /** 本次计算使用的成型需求量。 */
    @TableField("DEMAND_QTY")
    private BigDecimal demandQty;
    /** 来源成型排程批次号。 */
    @TableField("SOURCE_CX_BATCH_NO")
    private String sourceCxBatchNo;
    /** 来源成型计划版本摘要。 */
    @TableField("SOURCE_VERSION")
    private String sourceVersion;
}
