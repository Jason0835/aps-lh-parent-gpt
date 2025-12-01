package com.zlt.mix.schedule.api.domain.entity;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 排程操作日志对象 t_schedule_oper_log
 *
 * @author chen
 * @date 2022-07-13
 */
@ApiModel(value = "排程操作日志对象", description = "排程操作日志对象 ")
@TableName("t_schedule_oper_log")
@KeySequence(value = "SEQ_T_SCHEDULE_OPER_LOG", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class ScheduleOperLog extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_T_SCHEDULE_OPER_LOG
     */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_SCHEDULE_OPER_LOG", position = 10)
    private Long id;
    /**
     * 排程类型(0--终炼母炼日计划排程、1--硫磺辅料日计划排程，对应数据字典：SCHEDULE_TYPE)
     */
    @Excel(name = "schedule.scheduleOperLog.scheduleType")
    @ApiModelProperty(value = "排程类型(0--终炼母炼日计划排程、1--硫磺辅料日计划排程，对应数据字典：SCHEDULE_TYPE)", position = 20)
    private String scheduleType;
    /**
     * 排程ID，对应排产表的ID
     */
    @Excel(name = "schedule.scheduleOperLog.scheduleId")
    @ApiModelProperty(value = "排程ID，对应排产表的ID", position = 30)
    private Long scheduleId;
    /**
     * 操作类型：0--转机台、1--调量、2--插单、3--调序、4--自动排程、5--排程发布。对应数据字典：MIX_DISPATCHER_OPER_TYPE
     */
    @Excel(name = "schedule.scheduleOperLog.operType")
    @ApiModelProperty(value = "操作类型：0--转机台、1--调量、2--插单、3--调序、4--自动排程、5--排程发布。对应数据字典：MIX_DISPATCHER_OPER_TYPE", position = 40)
    private String operType;
    /**
     * 排程日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "schedule.scheduleOperLog.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", position = 50)
    private Date scheduleDate;
    /**
     * 密炼区
     */
    @Excel(name = "schedule.scheduleOperLog.mixArea")
    @ApiModelProperty(value = "密炼区", position = 60)
    private String mixArea;
    /**
     * 胶料代码
     */
    @Excel(name = "schedule.scheduleOperLog.materialCode")
    @ApiModelProperty(value = "胶料代码", position = 70)
    private String materialCode;
    /**
     * 操作前机台CODE,多个逗号分割
     */
    @Excel(name = "schedule.scheduleOperLog.beforeMachineCode")
    @ApiModelProperty(value = "操作前机台CODE,多个逗号分割", position = 80)
    private String beforeMachineCode;
    /**
     * 操作前配方类型
     */
    @Excel(name = "schedule.scheduleOperLog.beforeRecipeType", sort = 200)
    @ApiModelProperty(value = "操作前配方类型", position = 85)
    private String beforeRecipeType;
    /**
     * 操作前配方类型名称
     */
    @Excel(name = "schedule.scheduleOperLog.beforeRecipeTypeName", sort = 200)
    @ApiModelProperty(value = "操作前配方类型名称", position = 85)
    private String beforeRecipeTypeName;
    /**
     * 操作前配方版本号
     */
    @Excel(name = "schedule.scheduleOperLog.beforeRecipeVersionId", sort = 85)
    @ApiModelProperty(value = "操作前配方版本号", position = 100)
    private String beforeRecipeVersionId;
    /**
     * 操作前配方阶段(对应数据字典：PRODUCT_STAGE)
     */
    @Excel(name = "schedule.scheduleOperLog.beforeRecipeStage", sort = 200)
    @ApiModelProperty(value = "操作前配方阶段(对应数据字典：PRODUCT_STAGE)", position = 85)
    private String beforeRecipeStage;
    /**
     * 操作前中班计划量
     */
    @Excel(name = "schedule.scheduleOperLog.beforeMidPlan")
    @ApiModelProperty(value = "操作前中班计划量", position = 90)
    private Double beforeMidPlan;
    /**
     * 操作前中班顺序
     */
    @Excel(name = "schedule.scheduleOperLog.beforeMidOrder")
    @ApiModelProperty(value = "操作前中班顺序", position = 100)
    private Integer beforeMidOrder;
    /**
     * 操作前夜班计划量
     */
    @Excel(name = "schedule.scheduleOperLog.beforeNightPlan")
    @ApiModelProperty(value = "操作前夜班计划量", position = 110)
    private Double beforeNightPlan;
    /**
     * 操作前夜班顺序
     */
    @Excel(name = "schedule.scheduleOperLog.beforeNightOrder")
    @ApiModelProperty(value = "操作前夜班顺序", position = 120)
    private Integer beforeNightOrder;
    /**
     * 操作前白班计划量
     */
    @Excel(name = "schedule.scheduleOperLog.beforeDayPlan")
    @ApiModelProperty(value = "操作前白班计划量", position = 130)
    private Double beforeDayPlan;
    /**
     * 操作前白班顺序
     */
    @Excel(name = "schedule.scheduleOperLog.beforeDayOrder")
    @ApiModelProperty(value = "操作前白班顺序", position = 140)
    private Integer beforeDayOrder;
    /**
     * 操作后机台CODE,多个逗号分割
     */
    @Excel(name = "schedule.scheduleOperLog.afterMachineCode")
    @ApiModelProperty(value = "操作后机台CODE,多个逗号分割", position = 150)
    private String afterMachineCode;
    /**
     * 操作后配方类型
     */
    @Excel(name = "schedule.scheduleOperLog.afterRecipeType", sort = 200)
    @ApiModelProperty(value = "操作后配方类型", position = 85)
    private String afterRecipeType;
    /**
     * 操作后配方类型名称
     */
    @Excel(name = "schedule.scheduleOperLog.afterRecipeTypeName", sort = 200)
    @ApiModelProperty(value = "操作后配方类型名称", position = 85)
    private String afterRecipeTypeName;
    /**
     * 操作后配方版本号
     */
    @Excel(name = "schedule.scheduleOperLog.afterRecipeVersionId", sort = 85)
    @ApiModelProperty(value = "操作后配方版本号", position = 100)
    private String afterRecipeVersionId;
    /**
     * 操作后配方阶段(对应数据字典：PRODUCT_STAGE)
     */
    @Excel(name = "schedule.scheduleOperLog.afterRecipeStage", sort = 200)
    @ApiModelProperty(value = "操作后配方阶段(对应数据字典：PRODUCT_STAGE)", position = 85)
    private String afterRecipeStage;
    /**
     * 操作后中班计划量
     */
    @Excel(name = "schedule.scheduleOperLog.afterMidPlan")
    @ApiModelProperty(value = "操作后中班计划量", position = 160)
    private Double afterMidPlan;
    /**
     * 操作后中班顺序
     */
    @Excel(name = "schedule.scheduleOperLog.afterMidOrder")
    @ApiModelProperty(value = "操作后中班顺序", position = 170)
    private Integer afterMidOrder;
    /**
     * 操作后夜班计划量
     */
    @Excel(name = "schedule.scheduleOperLog.afterNightPlan")
    @ApiModelProperty(value = "操作后夜班计划量", position = 180)
    private Double afterNightPlan;
    /**
     * 操作后夜班顺序
     */
    @Excel(name = "schedule.scheduleOperLog.afterNightOrder")
    @ApiModelProperty(value = "操作后夜班顺序", position = 190)
    private Integer afterNightOrder;
    /**
     * 操作后白班计划量
     */
    @Excel(name = "schedule.scheduleOperLog.afterDayPlan")
    @ApiModelProperty(value = "操作后白班计划量", position = 200)
    private Double afterDayPlan;
    /**
     * 操作后白班顺序
     */
    @Excel(name = "schedule.scheduleOperLog.afterDayOrder")
    @ApiModelProperty(value = "操作后白班顺序", position = 210)
    private Integer afterDayOrder;
    /**
     * 备注
     */
    @Excel(name = "schedule.scheduleOperLog.remark")
    @ApiModelProperty(value = "备注", position = 220)
    private String remark;
    /**
     * 操作IP
     */
    @Excel(name = "schedule.scheduleOperLog.operIp")
    @ApiModelProperty(value = "操作者的IP", position = 230)
    private String operIp;
    /**
     * 操作前发布状态
     */
    @Excel(name = "schedule.scheduleOperLog.beforeReleaseStatus")
    @ApiModelProperty(value = "操作前发布状态，0--未发布，1--已发布，2-发布失败，3-发布中，4-超时失败，5-待发布。对应数据字典为：RELEASE_STATUS", position = 240)
    private String beforeReleaseStatus;
    /**
     * 操作后发布状态
     */
    @Excel(name = "schedule.scheduleOperLog.afterReleaseStatus")
    @ApiModelProperty(value = "操作后发布状态，0--未发布，1--已发布，2-发布失败，3-发布中，4-超时失败，5-待发布。对应数据字典为：RELEASE_STATUS", position = 250)
    private String afterReleaseStatus;

}
