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
 * 胶料跨区接收对象 t_glue_span_receive
 *
 * @author chen
 * @date 2022-08-16
 */
@ApiModel(value = "胶料跨区接收对象", description = "胶料跨区接收对象 ")
@TableName("t_glue_span_receive")
@KeySequence(value = "seq_t_glue_span_receive", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class GlueSpanReceive extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_T_GLUE_SPAN_RECEIVE
     */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_GLUE_SPAN_RECEIVE", position = 10)
    private Long id;

    /**
     * 发送记录ID(对应T_GLUE_SPAN_SEND表ID)
     */
    @ApiModelProperty(value = "发送记录ID(对应T_GLUE_SPAN_SEND表ID)", position = 20)
    private Long sendId;

    /**
     * 排程日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "schedule.glueSpanReceive.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", position = 30)
    private Date scheduleDate;

    /**
     * 委托密炼区(对应数据字典code：MIX_AREA)
     */
    @Excel(name = "schedule.glueSpanReceive.entrustMixArea")
    @ImportValidated(name = "schedule.glueSpanReceive.entrustMixArea", maxLength = 10)
    @ApiModelProperty(value = "委托密炼区(对应数据字典code：MIX_AREA)", position = 40)
    private String entrustMixArea;

    /**
     * 被委托密炼区(对应数据字典code：MIX_AREA)
     */
    @Excel(name = "schedule.glueSpanReceive.entrustedMixArea")
    @ImportValidated(name = "schedule.glueSpanReceive.entrustedMixArea", maxLength = 10)
    @ApiModelProperty(value = "被委托密炼区(对应数据字典code：MIX_AREA)", position = 50)
    private String entrustedMixArea;

    /**
     * 胶料名称
     */
    @Excel(name = "schedule.glueSpanReceive.glue")
    @ImportValidated(name = "schedule.glueSpanReceive.glue", maxLength = 30)
    @ApiModelProperty(value = "胶料名称", position = 60)
    private String glue;

    /**
     * 发送数量
     */
    @Excel(name = "schedule.glueSpanReceive.sendQty")
    @ImportValidated(name = "schedule.glueSpanReceive.sendQty", number = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "发送数量", position = 70)
    private Long sendQty;

    /**
     * 接收数量
     */
    @Excel(name = "schedule.glueSpanReceive.receiveQty")
    @ImportValidated(name = "schedule.glueSpanReceive.receiveQty", number = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "接收数量", position = 80)
    private Long receiveQty;

    /**
     * 机台代号
     */
    @Excel(name = "schedule.glueSpanReceive.machineCode")
    @ImportValidated(name = "schedule.glueSpanReceive.machineCode", maxLength = 30)
    @ApiModelProperty(value = "机台代号", position = 90)
    private String machineCode;

    /**
     * 配方类型
     */
    @Excel(name = "schedule.glueSpanReceive.recipeType")
    @ImportValidated(name = "schedule.glueSpanReceive.recipeType", maxLength = 3)
    @ApiModelProperty(value = "配方类型", position = 100)
    private String recipeType;

    /**
     * 配方版本号
     */
    @Excel(name = "schedule.glueSpanReceive.recipeVersionId")
    @ImportValidated(name = "schedule.glueSpanReceive.recipeVersionId", maxLength = 30)
    @ApiModelProperty(value = "配方版本号", position = 110)
    private String recipeVersionId;

    /**
     * 配方阶段(对应数据字典：PRODUCT_STAGE)
     */
    @Excel(name = "schedule.glueSpanReceive.recipeStage")
    @ImportValidated(name = "schedule.glueSpanReceive.recipeStage", maxLength = 10)
    @ApiModelProperty(value = "配方阶段(对应数据字典：PRODUCT_STAGE)", position = 120)
    private String recipeStage;

    /**
     * 预计需求时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "schedule.glueSpanReceive.expectDemandTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "预计需求时间", position = 130)
    private Date expectDemandTime;

    /**
     * 状态(0--未接收，1--已接收，对应数据字典：RECEIVE_STATUS)
     */
    @Excel(name = "schedule.glueSpanReceive.receiveStatus")
    @ImportValidated(name = "schedule.glueSpanReceive.receiveStatus", maxLength = 1)
    @ApiModelProperty(value = "状态(0--未接收，1--已接收，对应数据字典：RECEIVE_STATUS)", position = 140)
    private String receiveStatus;

    /**
     * 接收时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "schedule.glueSpanReceive.receiveTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "接收时间", position = 150)
    private Date receiveTime;

    /**
     * 接收人
     */
    @Excel(name = "schedule.glueSpanReceive.receivePerson")
    @ImportValidated(name = "schedule.glueSpanReceive.receivePerson", maxLength = 8)
    @ApiModelProperty(value = "接收人", position = 160)
    private String receivePerson;

    /**
     * 数据来源(0-分解胶料需求量，1--终炼母炼日计划)
     */
    @ApiModelProperty(value = "数据来源(0-分解胶料需求量，1--终炼母炼日计划)", position = 170)
    private String source;

    @ApiModelProperty(value = "是否系统自动创建（0-是，1-不是）", position = 110)
    private String isAuto;

    @ApiModelProperty(value = "排程ID", position = 120)
    private Long scheduleId;

    /**
     * 备注
     */
    @Excel(name = "schedule.glueSpanReceive.remark")
    @ImportValidated(name = "schedule.glueSpanReceive.remark", maxLength = 300)
    @ApiModelProperty(value = "备注", position = 230)
    private String remark;

    /**
     * 发送人
     */
    @ApiModelProperty(value = "发送人，关联发送表带出", position = 180)
    @TableField(exist = false)
    private String sendPerson;

    /**
     * 机台名称
     */
    @ApiModelProperty(value = "机台名称", position = 190)
    @TableField(exist = false)
    private String machineName;

    /**
     * 配方类型名称(对应T_RECIPE_TYPE表关联出来的RECIPE_TYPE_NAME)
     */
    @ApiModelProperty(value = "配方类型名称(对应T_RECIPE_TYPE表关联出来的RECIPE_TYPE_NAME)", position = 200)
    @TableField(exist = false)
    private String recipeTypeName;

    /**
     * 完成量(对应 T_GLUE_FINISH表关联出来的三班总完成量)
     */
    @ApiModelProperty(value = "完成量(对应 T_GLUE_FINISH表关联出来的三班总完成量)", position = 210)
    @TableField(exist = false)
    private String finishQty;

    /**
     * 允许查看的密炼区集合,用于过滤被委托密炼区字段
     */
    @ApiModelProperty(value = "允许查看的密炼区集合,用于过滤被委托密炼区字段", position = 220)
    @TableField(exist = false)
    private List<String> permissionMixAreaList;
}
