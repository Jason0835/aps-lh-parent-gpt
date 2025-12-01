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
 * 硫磺辅料跨区接收对象 t_material_span_receive
 *
 * @author cxy
 * @date 2022-08-30
 */
@ApiModel(value = "硫磺辅料跨区接收对象", description = "硫磺辅料跨区接收对象 ")
@TableName("t_material_span_receive")
@KeySequence(value = "seq_t_material_span_receive", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialSpanReceive extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_T_MATERIAL_SPAN_RECEIVE
     */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_MATERIAL_SPAN_RECEIVE", position = 10)
    private Long id;

    /**
     * 发送记录ID(对应T_GLUE_SPAN_SEND表ID)
     */
    @Excel(name = "schedule.materialSpanReceive.sendId")
    @ImportValidated(name = "schedule.materialSpanReceive.sendId", number = true, min = 0, max = 999999999)
    @ApiModelProperty(value = "发送记录ID(对应T_GLUE_SPAN_SEND表ID)", position = 20)
    private Long sendId;

    /**
     * 排程日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "schedule.materialSpanReceive.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", position = 30)
    private Date scheduleDate;

    /**
     * 委托密炼区(对应数据字典code：MIX_AREA)
     */
    @Excel(name = "schedule.materialSpanReceive.entrustMixArea")
    @ImportValidated(name = "schedule.materialSpanReceive.entrustMixArea", maxLength = 10)
    @ApiModelProperty(value = "委托密炼区(对应数据字典code：MIX_AREA)", position = 40)
    private String entrustMixArea;

    /**
     * 被委托密炼区(对应数据字典code：MIX_AREA)
     */
    @Excel(name = "schedule.materialSpanReceive.entrustedMixArea")
    @ImportValidated(name = "schedule.materialSpanReceive.entrustedMixArea", maxLength = 10)
    @ApiModelProperty(value = "被委托密炼区(对应数据字典code：MIX_AREA)", position = 50)
    private String entrustedMixArea;

    /**
     * 物料名称
     */
    @Excel(name = "schedule.materialSpanReceive.materialName")
    @ImportValidated(name = "schedule.materialSpanReceive.materialName", maxLength = 50)
    @ApiModelProperty(value = "物料名称", position = 60)
    private String materialName;
    
    /**
     * 总发送数量
     */
    @Excel(name = "schedule.materialSpanReceive.totalSendQty")
    @ImportValidated(name = "schedule.materialSpanSend.totalSendQty", number = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "总发送数量", position = 60)
    private Long totalSendQty;
    
    /**
     * 总发送数量
     */
    @Excel(name = "schedule.materialSpanReceive.totalReceiveQty")
    @ImportValidated(name = "schedule.materialSpanSend.totalSendQty", number = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "总接收数量", position = 60)
    private Long totalReceiveQty;

    /**
     * 中班发送数量
     */
    @Excel(name = "schedule.materialSpanReceive.midSendQty")
    @ImportValidated(name = "schedule.materialSpanReceive.midSendQty", number = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "中班发送数量", position = 70)
    private Long midSendQty;

    /**
     * 中班接收数量
     */
    @Excel(name = "schedule.materialSpanReceive.midReceiveQty")
    @ImportValidated(name = "schedule.materialSpanReceive.midReceiveQty", number = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "中班接收数量", position = 80)
    private Long midReceiveQty;

    /**
     * 中班预计需求时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "schedule.materialSpanReceive.midExpectDemandTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "中班预计需求时间", position = 90)
    private Date midExpectDemandTime;

    /**
     * 夜班发送数量
     */
    @Excel(name = "schedule.materialSpanReceive.nightSendQty")
    @ImportValidated(name = "schedule.materialSpanReceive.nightSendQty", number = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "夜班发送数量", position = 100)
    private Long nightSendQty;

    /**
     * 夜班接收数量
     */
    @Excel(name = "schedule.materialSpanReceive.nightReceiveQty")
    @ImportValidated(name = "schedule.materialSpanReceive.nightReceiveQty", number = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "夜班接收数量", position = 110)
    private Long nightReceiveQty;

    /**
     * 夜班预计需求时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "schedule.materialSpanReceive.nightExpectDemandTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "夜班预计需求时间", position = 120)
    private Date nightExpectDemandTime;

    /**
     * 白班发送数量
     */
    @Excel(name = "schedule.materialSpanReceive.daySendQty")
    @ImportValidated(name = "schedule.materialSpanReceive.daySendQty", number = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "白班发送数量", position = 130)
    private Long daySendQty;

    /**
     * 夜班接收数量
     */
    @Excel(name = "schedule.materialSpanReceive.dayReceiveQty")
    @ImportValidated(name = "schedule.materialSpanReceive.dayReceiveQty", number = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "夜班接收数量", position = 140)
    private Long dayReceiveQty;

    /**
     * 白班预计需求时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "schedule.materialSpanReceive.dayExpectDemandTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "白班预计需求时间", position = 150)
    private Date dayExpectDemandTime;

    /**
     * 机台代号
     */
    @Excel(name = "schedule.materialSpanReceive.machineCode")
    @ImportValidated(name = "schedule.materialSpanReceive.machineCode", maxLength = 30)
    @ApiModelProperty(value = "机台代号", position = 160)
    private String machineCode;

    /**
     * 配方类型
     */
    @Excel(name = "schedule.materialSpanReceive.recipeType")
    @ImportValidated(name = "schedule.materialSpanReceive.recipeType", maxLength = 3)
    @ApiModelProperty(value = "配方类型", position = 170)
    private String recipeType;

    /**
     * 配方版本号
     */
    @Excel(name = "schedule.materialSpanReceive.recipeVersionId")
    @ImportValidated(name = "schedule.materialSpanReceive.recipeVersionId", maxLength = 30)
    @ApiModelProperty(value = "配方版本号", position = 180)
    private String recipeVersionId;

    /**
     * 配方阶段(对应数据字典：RECIPE_STAGE)
     */
    @Excel(name = "schedule.materialSpanReceive.recipeStage")
    @ImportValidated(name = "schedule.materialSpanReceive.recipeStage", maxLength = 10)
    @ApiModelProperty(value = "配方阶段(对应数据字典：RECIPE_STAGE)", position = 190)
    private String recipeStage;

    /**
     * 状态(0--未接收，1--已接收，对应数据字典：RECEIVE_STATUS)
     */
    @ApiModelProperty(value = "状态(0--未接收，1--已接收，对应数据字典：RECEIVE_STATUS)", position = 210)
    private String receiveStatus;

    /**
     * 接收时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "schedule.materialSpanReceive.receiveTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "接收时间", position = 220)
    private Date receiveTime;

    /**
     * 接收人
     */
    @Excel(name = "schedule.materialSpanReceive.receivePerson")
    @ImportValidated(name = "schedule.materialSpanReceive.receivePerson", maxLength = 8)
    @ApiModelProperty(value = "接收人", position = 230)
    private String receivePerson;

    /**
     * 是否系统自动创建（0-是，1-不是）
     */
    @ApiModelProperty(value = "是否系统自动创建（0-是，1-不是）", position = 240)
    private String isAuto;

    @ApiModelProperty(value = "硫磺辅料排程ID", position = 250)
    private Long scheduleId;

    /**
     * 备注
     */
    @Excel(name = "ui.data.column.remark")
    @ImportValidated(name = "ui.data.column.remark", maxLength = 300)
    @ApiModelProperty(value = "备注", position = 300)
    private String remark;

    /**
     * 发送人
     */
    @ApiModelProperty(value = "发送人，关联发送表带出", position = 250)
    @TableField(exist = false)
    private String sendPerson;

    /**
     * 机台名称
     */
    @ApiModelProperty(value = "机台名称", position = 260)
    @TableField(exist = false)
    private String machineName;

    /**
     * 配方类型名称(对应T_RECIPE_TYPE表关联出来的RECIPE_TYPE_NAME)
     */
    @ApiModelProperty(value = "配方类型名称(对应T_RECIPE_TYPE表关联出来的RECIPE_TYPE_NAME)", position = 270)
    @TableField(exist = false)
    private String recipeTypeName;

    /**
     * 完成量
     */
    @ApiModelProperty(value = "完成量", position = 280)
    @TableField(exist = false)
    private Double finishQty;

    /**
     * 允许查看的密炼区集合,用于过滤被委托密炼区字段
     */
    @ApiModelProperty(value = "允许查看的密炼区集合,用于过滤被委托密炼区字段", position = 290)
    @TableField(exist = false)
    private List<String> permissionMixAreaList;
}
