package com.zlt.aps.lh.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhDispatcherLog.java
 * 描    述：硫化调度员排程操作日志对象 t_lh_dispatcher_log
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-21
 */
@ApiModel(value = "硫化调度员排程操作日志对象", description = "硫化调度员排程操作日志对象 ")
@Data
public class LhDispatcherLogVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 排程ID，对应排产表的ID
     */
    @ApiModelProperty(value = "排程ID，对应排产表的ID", name = "scheduleId")
    @TableField(value = "SCHEDULE_ID")
    private Long scheduleId;

    /**
     * 是否变更机台
     */
    private String changeMachine;

    /**
     * 是否更改一班计划
     */
    private String changeClass1Plan;

    /**
     * 是否更改二班计划
     */
    private String changeClass2Plan;

    /**
     * 是否更改三班计划
     */
    private String changeClass3Plan;

    /**
     * 是否更改四班计划
     */
    private String changeClass4Plan;

    /**
     * 是否更改五班计划
     */
    private String changeClass5Plan;

    /**
     * 是否更改六班计划
     */
    private String changeClass6Plan;
}