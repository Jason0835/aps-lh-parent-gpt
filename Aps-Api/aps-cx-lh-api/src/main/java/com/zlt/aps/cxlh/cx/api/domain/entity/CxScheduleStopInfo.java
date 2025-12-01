package com.zlt.aps.cxlh.cx.api.domain.entity;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zlt.common.domain.CommonBusiEntity;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：CxScheduleStopInfo.java
 * 描    述：成型机台自动停排信息对象 t_cx_schedule_stop_info
 *@author zlt
 *@date 2025-03-11
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "成型机台自动停排信息对象", description = "成型机台自动停排信息对象 ")
@Data
@TableName(value = "T_CX_SCHEDULE_STOP_INFO")
public class CxScheduleStopInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;


    /**
     * 分厂编号
     */
    @ApiModelProperty(value = "分厂编号")
    private String factoryCode;


    /** 自动排程批次号信息，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号 */
    @Excel(name = "ui.data.column.cxScheduleStopInfo.cxBatchNo")
    @ApiModelProperty(value = "自动排程批次号信息，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号", name = "cxBatchNo")
    @TableField(value = "CX_BATCH_NO")
    private String cxBatchNo;

    /** 外胎规格 */
    @Excel(name = "ui.data.column.cxScheduleStopInfo.spec")
    @ApiModelProperty(value = "外胎规格", name = "spec")
    @TableField(value = "SPEC")
    private String spec;

    /** 外胎代码 */
    @Excel(name = "ui.data.column.cxScheduleStopInfo.sapCode")
    @ApiModelProperty(value = "外胎代码", name = "sapCode")
    @TableField(value = "SAP_CODE")
    private String sapCode;

    /** 规格代码 */
    @ApiModelProperty(value = "规格代码", name = "specCode")
    @TableField(value = "SPEC_CODE")
    @ImportValidated(maxLength = 66)
    @Excel(name = "ui.data.column.scheduleResult.specCode")
    private String specCode;

    /** 胎胚代码 */
    @Excel(name = "ui.data.column.cxScheduleStopInfo.embryoCode")
    @ApiModelProperty(value = "胎胚代码", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /** 成型排程工单号，自动生成，批次号+4位定长自增序号 */
    @Excel(name = "ui.data.column.cxScheduleStopInfo.orderNo")
    @ApiModelProperty(value = "成型排程工单号，自动生成，批次号+4位定长自增序号", name = "orderNo")
    @TableField(value = "ORDER_NO")
    private String orderNo;

    /** 排程日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.cxScheduleStopInfo.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    /** 字典 */
    @Excel(name = "ui.data.column.cxScheduleStopInfo.stopReason")
    @ApiModelProperty(value = "字典", name = "stopReason")
    @TableField(value = "STOP_REASON")
    private String stopReason;

    /**
     * 未排数量
     */
    @Excel(name = "ui.data.column.cxScheduleStopInfo.unScheduleNum")
    @ApiModelProperty(value = "未排数量", name = "unScheduleNum")
    @TableField(value = "UN_SCHEDULE_NUM")
    private Integer unScheduleNum;

    /**
     * 施工版本信息
     */
    @Excel(name = "ui.data.column.productStatus.bomDataVersion")
    private  String bomDataVersion;
}