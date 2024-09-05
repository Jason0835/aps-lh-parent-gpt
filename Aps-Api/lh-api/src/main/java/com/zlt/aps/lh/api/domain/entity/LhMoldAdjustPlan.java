package com.zlt.aps.lh.api.domain.entity;

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
 * 硫化模具调整计划对象 t_lh_mold_adjust_plan
 *
 * @author chen
 * @date 2022-03-23
 */
@ApiModel(value = "硫化模具调整计划对象", description = "硫化模具调整计划对象 ")
@Data
@EqualsAndHashCode(callSuper = true)
public class LhMoldAdjustPlan extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应序列SEQ_LH_MOLD_ADJUST_PLAN
     */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 对应MES单据号数据
     */
    @Excel(name = "ui.data.column.moldAdjustPlan.orderNo")
    @ApiModelProperty(value = "对应MES单据号数据")
    private String orderNo;

    /**
     * 类型（类型）
     */
    @Excel(name = "ui.data.column.moldAdjustPlan.type")
    @ApiModelProperty(value = "类型")
    private String type;

    /**
     * 硫化机台编号
     */
    @Excel(name = "ui.data.column.moldAdjustPlan.lhMachineCode")
    @ApiModelProperty(value = "硫化机台编号")
    private String lhMachineCode;

    /**
     * 计划日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "ui.data.column.moldAdjustPlan.planDate", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "计划日期")
    private Date planDate;

    /**
     * 换模时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "ui.data.column.moldAdjustPlan.changeMoldTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "换模时间")
    private Date changeMoldTime;

    /**
     * 0:未装配；1:已装配
     */
    @Excel(name = "ui.data.column.moldAdjustPlan.moldStatus", dictType = "mold_status")
    @ApiModelProperty(value = "0:未装配；1:已装配")
    private String moldStatus;

    /**
     * 0:未通过；1:已通过
     */
    @Excel(name = "ui.data.column.moldAdjustPlan.auditStatus", dictType = "audit_status")
    @ApiModelProperty(value = "0:未通过；1:已通过")
    private String auditStatus;

    /**
     * 0：模具准备；1停模等
     */
    @Excel(name = "ui.data.column.moldAdjustPlan.process", dictType = "process")
    @ApiModelProperty(value = "0：模具准备；1停模等")
    private Long process;

    /**
     * 0:未执行；1:已完成
     */
    @Excel(name = "ui.data.column.moldAdjustPlan.executionStatus", dictType = "execution_status")
    @ApiModelProperty(value = "0:未执行；1:已完成")
    private String executionStatus;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "ui.data.column.moldAdjustPlan.beginTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "开始时间")
    private Date beginTime;

    /**
     * 完成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "ui.data.column.moldAdjustPlan.finishTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "完成时间")
    private Date finishTime;

    /**
     * 左外胎品号
     */
    @Excel(name = "ui.data.column.moldAdjustPlan.leftSapCode")
    @ApiModelProperty(value = "左外胎品号")
    private String leftSapCode;

    /**
     * 右外胎品号
     */
    @Excel(name = "ui.data.column.moldAdjustPlan.rightSapCode")
    @ApiModelProperty(value = "右外胎品号")
    private String rightSapCode;

    /**
     * 左胎胚品号
     */
    @Excel(name = "ui.data.column.moldAdjustPlan.leftEmbryoSapCode")
    @ApiModelProperty(value = "左胎胚品号")
    private String leftEmbryoSapCode;

    /**
     * 右胎胚品号
     */
    @Excel(name = "ui.data.column.moldAdjustPlan.rightEmbryoSapCode")
    @ApiModelProperty(value = "右胎胚品号")
    private String rightEmbryoSapCode;

    /**
     * 左规格描述
     */
    @Excel(name = "ui.data.column.moldAdjustPlan.leftSpecDesc")
    @ApiModelProperty(value = "左规格描述")
    private String leftSpecDesc;

    /**
     * 右规格描述
     */
    @Excel(name = "ui.data.column.moldAdjustPlan.rightSpecDesc")
    @ApiModelProperty(value = "右规格描述")
    private String rightSpecDesc;

    /**
     * 原左规格描述
     */
    @Excel(name = "ui.data.column.moldAdjustPlan.leftBeforeSpecDesc")
    @ApiModelProperty(value = "原左规格描述")
    private String leftBeforeSpecDesc;

    /**
     * 原右规格描述
     */
    @Excel(name = "ui.data.column.moldAdjustPlan.rightBeforeSpecDesc")
    @ApiModelProperty(value = "原右规格描述")
    private String rightBeforeSpecDesc;

    /**
     * 左胎胚代号
     */
    @Excel(name = "ui.data.column.moldAdjustPlan.leftEmbryoCode")
    @ApiModelProperty(value = "左胎胚代号")
    private String leftEmbryoCode;

    /**
     * 右胎胚代号
     */
    @Excel(name = "ui.data.column.moldAdjustPlan.rightEmbryoCode")
    @ApiModelProperty(value = "右胎胚代号")
    private String rightEmbryoCode;

    /**
     * 左蒸锅
     */
    @Excel(name = "ui.data.column.moldAdjustPlan.leftStream")
    @ApiModelProperty(value = "左蒸锅")
    private Long leftStream;

    /**
     * 右蒸锅
     */
    @Excel(name = "ui.data.column.moldAdjustPlan.rightStream")
    @ApiModelProperty(value = "右蒸锅")
    private Long rightStream;

    /**
     * 左模具信息
     */
    @Excel(name = "ui.data.column.moldAdjustPlan.leftMoldInfo")
    @ApiModelProperty(value = "左模具信息")
    private String leftMoldInfo;

    /**
     * 右模具信息
     */
    @Excel(name = "ui.data.column.moldAdjustPlan.rightMoldInfo")
    @ApiModelProperty(value = "右模具信息")
    private String rightMoldInfo;

    /**
     * 模具装配人
     */
    @Excel(name = "ui.data.column.moldAdjustPlan.moldAssembler")
    @ApiModelProperty(value = "模具装配人")
    private String moldAssembler;

    /**
     * 模具装配时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "ui.data.column.moldAdjustPlan.moldAssembleTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "模具装配时间")
    private Date moldAssembleTime;

    /**
     * 审核人
     */
    @Excel(name = "ui.data.column.moldAdjustPlan.auditor")
    @ApiModelProperty(value = "审核人")
    private String auditor;

    /**
     * 审核时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "ui.data.column.moldAdjustPlan.auditTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "审核时间")
    private Date auditTime;

    /**
     * 审核意见
     */
    @Excel(name = "ui.data.column.moldAdjustPlan.auditSuggest")
    @ApiModelProperty(value = "审核意见")
    private String auditSuggest;

    /**
     * 备注
     */
    @Excel(name = "ui.common.column.remark")
    @ImportValidated(maxLength = 300)
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;
}
