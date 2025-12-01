package com.zlt.aps.cxlh.cx.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：AutoScheduleLog.java
 * 描    述：成型自动排程日志对象 t_auto_schedule_log
 *@author zlt
 *@date 2025-03-07
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "成型自动排程日志对象", description = "成型自动排程日志对象 ")
@Data
@TableName(value = "T_AUTO_SCHEDULE_LOG")
// @KeySequence(value = "SEQ__SCHEDULE_LOG")
public class AutoScheduleLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 工序code：11-硫化、1-成型、2-胎面、3-胎侧、4-内衬、5-胎圈、6-钢丝圈、7-15度裁断、8-90裁断、9-90度裁断、10-纤维压延 */
    // @Excel(name = "ui.data.column.autoScheduleLog.procedureCode")
    @ApiModelProperty(value = "工序code：11-硫化、1-成型、2-胎面、3-胎侧、4-内衬、5-胎圈、6-钢丝圈、7-15度裁断、8-90裁断、9-90度裁断、10-纤维压延", name = "procedureCode")
    @TableField(value = "PROCEDURE_CODE")
    private String procedureCode;

    /** 批次号 */
    @Excel(name = "ui.data.column.autoScheduleLog.batchNo")
    @ApiModelProperty(value = "批次号", name = "batchNo")
    @TableField(value = "BATCH_NO")
    private String batchNo;

    /** 工单号 */
    @Excel(name = "ui.data.column.autoScheduleLog.orderNo")
    @ApiModelProperty(value = "工单号", name = "orderNo")
    @TableField(value = "ORDER_NO")
    private String orderNo;

    /** 标题 */
    @Excel(name = "ui.data.column.autoScheduleLog.title")
    @ApiModelProperty(value = "标题", name = "title")
    @TableField(value = "TITLE")
    private String title;

    /** 日志明细 */
    @Excel(name = "ui.data.column.autoScheduleLog.logDetail")
    @ApiModelProperty(value = "日志明细", name = "logDetail")
    @TableField(value = "LOG_DETAIL")
    private String logDetail;

    /**  */
    // @Excel(name = "ui.data.column.autoScheduleLog.delFlag")
    @ApiModelProperty(value = "", name = "delFlag")
    @TableField(value = "DEL_FLAG")
    private String delFlag;


}