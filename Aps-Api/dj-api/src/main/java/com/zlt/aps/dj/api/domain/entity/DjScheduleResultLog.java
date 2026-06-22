package com.zlt.aps.dj.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 垫胶排程结果对象 dj_schedule_result_log
 *
 * @author zlt
 * @date 2026-06-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "T_DJ_SCHEDULE_RESULT_LOG")
@ApiModel(value = "垫胶排程结果日志对象", description = "垫胶排程结果日志对象 ")
public class DjScheduleResultLog extends DjScheduleResult {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

}
