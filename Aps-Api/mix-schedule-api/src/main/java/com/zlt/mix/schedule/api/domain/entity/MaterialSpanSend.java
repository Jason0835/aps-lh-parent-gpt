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
 * 硫磺辅料跨区发送对象 t_material_span_send
 *
 * @author cxy
 * @date 2022-08-30
 */
@ApiModel(value = "硫磺辅料跨区发送对象", description = "硫磺辅料跨区发送对象 ")
@TableName("t_material_span_send")
@KeySequence(value = "seq_t_material_span_send", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialSpanSend extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_T_MATERIAL_SPAN_SEND
     */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_MATERIAL_SPAN_SEND", position = 10)
    private Long id;

    /**
     * 排程日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "schedule.materialSpanSend.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", position = 20)
    private Date scheduleDate;

    /**
     * 委托密炼区(对应数据字典code：MIX_AREA)
     */
    @Excel(name = "schedule.materialSpanSend.entrustMixArea")
    @ImportValidated(name = "schedule.materialSpanSend.entrustMixArea", maxLength = 10)
    @ApiModelProperty(value = "委托密炼区(对应数据字典code：MIX_AREA)", position = 30)
    private String entrustMixArea;

    /**
     * 被委托密炼区(对应数据字典code：MIX_AREA)
     */
    @Excel(name = "schedule.materialSpanSend.entrustedMixArea")
    @ImportValidated(name = "schedule.materialSpanSend.entrustedMixArea", maxLength = 10)
    @ApiModelProperty(value = "被委托密炼区(对应数据字典code：MIX_AREA)", position = 40)
    private String entrustedMixArea;

    /**
     * 物料名称
     */
    @Excel(name = "schedule.materialSpanSend.materialName")
    @ImportValidated(name = "schedule.materialSpanSend.materialName", maxLength = 50)
    @ApiModelProperty(value = "物料名称", position = 50)
    private String materialName;
    
    /**
     * 总发送数量
     */
    @Excel(name = "schedule.materialSpanSend.totalSendQty")
    @ImportValidated(name = "schedule.materialSpanSend.totalSendQty", number = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "总发送数量", position = 60)
    private Long totalSendQty;

    /**
     * 中班发送数量
     */
    @Excel(name = "schedule.materialSpanSend.midSendQty")
    @ImportValidated(name = "schedule.materialSpanSend.midSendQty", number = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "中班发送数量", position = 60)
    private Long midSendQty;

    /**
     * 中班预计需求时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "schedule.materialSpanSend.midExpectDemandTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "中班预计需求时间", position = 70)
    private Date midExpectDemandTime;

    /**
     * 夜班发送数量
     */
    @Excel(name = "schedule.materialSpanSend.nightSendQty")
    @ImportValidated(name = "schedule.materialSpanSend.nightSendQty", number = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "夜班发送数量", position = 80)
    private Long nightSendQty;

    /**
     * 夜班预计需求时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "schedule.materialSpanSend.nightExpectDemandTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "夜班预计需求时间", position = 90)
    private Date nightExpectDemandTime;

    /**
     * 白班发送数量
     */
    @Excel(name = "schedule.materialSpanSend.daySendQty")
    @ImportValidated(name = "schedule.materialSpanSend.daySendQty", number = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "白班发送数量", position = 100)
    private Long daySendQty;

    /**
     * 白班预计需求时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "schedule.materialSpanSend.dayExpectDemandTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "白班预计需求时间", position = 110)
    private Date dayExpectDemandTime;

    /**
     * 发送时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "schedule.materialSpanSend.sendTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "发送时间", position = 120)
    private Date sendTime;

    /**
     * 发送人
     */
    @Excel(name = "schedule.materialSpanSend.sendPerson")
    @ImportValidated(name = "schedule.materialSpanSend.sendPerson", maxLength = 8)
    @ApiModelProperty(value = "发送人", position = 130)
    private String sendPerson;

    /**
     * 状态(0--未接收，1--已接收，对应数据字典：RECEIVE_STATUS)
     */
    @ApiModelProperty(value = "状态(0--未接收，1--已接收，对应数据字典：RECEIVE_STATUS)", position = 140)
    private String receiveStatus;

    /**
     * 是否系统自动创建（0-是，1-不是）
     */
    @ApiModelProperty(value = "是否系统自动创建（0-是，1-不是）", position = 150)
    private String isAuto;

    @ApiModelProperty(value = "硫磺辅料排程ID", position = 160)
    private Long scheduleId;

    /**
     * 备注
     */
    @Excel(name = "ui.data.column.remark")
    @ImportValidated(name = "ui.data.column.remark", maxLength = 300)
    @ApiModelProperty(value = "备注", position = 210)
    private String remark;

    /**
     * 中班接收数量
     */
    @Excel(name = "schedule.materialSpanReceive.midReceiveQty")
    @ImportValidated(name = "schedule.materialSpanReceive.midReceiveQty", number = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "中班接收数量", position = 160)
    @TableField(exist = false)
    private Long midReceiveQty;

    /**
     * 夜班接收数量
     */
    @Excel(name = "schedule.materialSpanReceive.nightReceiveQty")
    @ImportValidated(name = "schedule.materialSpanReceive.nightReceiveQty", number = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "夜班接收数量", position = 170)
    @TableField(exist = false)
    private Long nightReceiveQty;

    /**
     * 夜班接收数量
     */
    @Excel(name = "schedule.materialSpanReceive.dayReceiveQty")
    @ImportValidated(name = "schedule.materialSpanReceive.dayReceiveQty", number = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "夜班接收数量", position = 180)
    @TableField(exist = false)
    private Long dayReceiveQty;

    /**
     * 完成量
     */
    @ApiModelProperty(value = "完成量", position = 190)
    @TableField(exist = false)
    private Double finishQty;

    /**
     * 允许查看的密炼区集合,用于过滤委托密炼区字段
     */
    @ApiModelProperty(value = "允许查看的密炼区集合,用于过滤委托密炼区字段", position = 200)
    @TableField(exist = false)
    private List<String> permissionMixAreaList;
}
