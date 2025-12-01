package com.zlt.mix.schedule.api.domain.entity;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.zlt.mix.common.core.annotation.ImportValidated;

/**
 * 胶料跨区发送对象 t_glue_span_send
 *
 * @author chen
 * @date 2022-08-15
 */
@ApiModel(value = "胶料跨区发送对象", description = "胶料跨区发送对象 ")
@TableName("t_glue_span_send")
@KeySequence(value = "seq_t_glue_span_send", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class GlueSpanSend extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID，对应自增序列为：SEQ_T_GLUE_SPAN_SEND */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_GLUE_SPAN_SEND", position = 10)
    private Long id;

    /** 排程日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "schedule.glueSpanSend.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", position = 20)
    private Date scheduleDate;

    /** 委托密炼区(对应数据字典code：MIX_AREA) */
    @Excel(name = "schedule.glueSpanSend.entrustMixArea")
    @ImportValidated(name = "schedule.glueSpanSend.entrustMixArea", maxLength=10)
    @ApiModelProperty(value = "委托密炼区(对应数据字典code：MIX_AREA)", position = 30)
    private String entrustMixArea;

    /** 被委托密炼区(对应数据字典code：MIX_AREA) */
    @Excel(name = "schedule.glueSpanSend.entrustedMixArea")
    @ImportValidated(name = "schedule.glueSpanSend.entrustedMixArea", maxLength=10)
    @ApiModelProperty(value = "被委托密炼区(对应数据字典code：MIX_AREA)", position = 40)
    private String entrustedMixArea;

    /** 胶料名称 */
    @Excel(name = "schedule.glueSpanSend.glue")
    @ImportValidated(name = "schedule.glueSpanSend.glue", maxLength=30)
    @ApiModelProperty(value = "胶料名称", position = 50)
    private String glue;

    /** 发送数量 */
    @Excel(name = "schedule.glueSpanSend.sendQty")
    @ImportValidated(name = "schedule.glueSpanSend.sendQty", number=true, min=0, max=9999999)
    @ApiModelProperty(value = "发送数量", position = 60)
    private Long sendQty;

    /** 预计需求时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "schedule.glueSpanSend.expectDemandTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "预计需求时间", position = 70)
    private Date expectDemandTime;

    /** 发送时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "schedule.glueSpanSend.sendTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "发送时间", position = 80)
    private Date sendTime;

    /** 发送人 */
    @Excel(name = "schedule.glueSpanSend.sendPerson")
    @ImportValidated(name = "schedule.glueSpanSend.sendPerson", maxLength=8)
    @ApiModelProperty(value = "发送人", position = 90)
    private String sendPerson;

    /** 状态(0--未接收，1--已接收，对应数据字典：RECEIVE_STATUS) */
    @ApiModelProperty(value = "状态(0--未接收，1--已接收，对应数据字典：RECEIVE_STATUS)", position = 100)
    private String receiveStatus;

    /** 数据来源(0-分解胶料需求量，1--终炼母炼日计划) */
    @ApiModelProperty(value = "数据来源(0-分解胶料需求量，1--终炼母炼日计划)", position = 110)
    private String source;

    @ApiModelProperty(value = "是否系统自动创建（0-是，1-不是）", position = 110)
    private String isAuto;

    @ApiModelProperty(value = "排程ID", position = 120)
    private Long scheduleId;

    /**
     * 完成量
     */
    @ApiModelProperty(value = "完成量", position = 120)
    @TableField(exist = false)
    private Double finishQty;

    /** 备注 */
    @Excel(name = "ui.data.column.remark")
    @ImportValidated(name = "ui.data.column.remark", maxLength=300)
    @ApiModelProperty(value = "备注", position = 170)
    private String remark;

    /**
     * 接收数量
     */
    @ApiModelProperty(value = "接收数量", position = 130)
    @TableField(exist = false)
    private Long receiveQty;

    @ApiModelProperty(value = "分解胶料计划机台", position = 130)
    @TableField(exist = false)
    private String decomposeMachineCode;

    /**
     * 允许查看的密炼区集合,用于过滤委托密炼区字段
     */
    @ApiModelProperty(value = "允许查看的密炼区集合,用于过滤委托密炼区字段", position = 140)
    @TableField(exist = false)
    private List<String> permissionMixAreaList;
}
