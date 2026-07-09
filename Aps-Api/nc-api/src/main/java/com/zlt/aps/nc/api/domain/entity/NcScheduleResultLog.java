package com.zlt.aps.nc.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 内衬胶排程结果对象 nc_schedule_result
 *
 * @author zlt
 * @date 2026-06-24
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_NC_SCHEDULE_RESULT_LOG")
@ApiModel(value = "内衬胶排程结果日志对象", description = "内衬胶排程结果日志对象 ")
public class NcScheduleResultLog extends NcScheduleResult {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
}
