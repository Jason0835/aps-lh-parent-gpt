package com.zlt.aps.nc.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 内衬排程日志实体类
 *
 * @author zlt
 * @date 2026-06-23
 */
@ApiModel(value = "内衬排程日志对象", description = "内衬排程日志表实体对象")
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "T_NC_SCHEDULE_PROCESS_LOG")
public class NcScheduleProcessLog extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 批次号
     */
    @Excel(name = "ui.data.column.lhScheduleProcessLog.batchNo")
    @ApiModelProperty(value = "批次号", name = "batchNo")
    @TableField(value = "BATCH_NO")
    private String batchNo;

    /**
     * 日志明细
     */
    @Excel(name = "ui.data.column.lhScheduleProcessLog.logDetail")
    @ApiModelProperty(value = "日志明细", name = "logDetail")
    @TableField(value = "LOG_DETAIL")
    private String logDetail;
}
