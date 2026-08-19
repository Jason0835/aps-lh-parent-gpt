package com.zlt.aps.cd90.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** 直裁自动排程需求快照。 */
@Data
@TableName("t_cd90_schedule_demand_snapshot")
public class Cd90ScheduleDemandSnapshot extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableField("FACTORY_CODE") private String factoryCode;
    @TableField("SCHEDULE_DATE") private Date scheduleDate;
    @TableField("BATCH_NO") private String batchNo;
    @TableField("CLOTH_CODE") private String clothCode;
    @TableField("CLASS_FIELD") private String classField;
    @TableField("DEMAND_TIME") private Date demandTime;
    @TableField("DEMAND_QTY") private BigDecimal demandQty;
    @TableField("SOURCE_CX_BATCH_NO") private String sourceCxBatchNo;
    @TableField("SOURCE_VERSION") private String sourceVersion;

    /** 备注 */
    @ApiModelProperty(value = "备注", name = "remark")
    @TableField("REMARK")
    @Excel(name = "ui.common.column.remark")
    private String remark;
}
