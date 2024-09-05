package com.zlt.aps.cx.engine.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 成型增补计划用到mes获取到的模具变动单实体
 */
@Data
public class CxEngineMesMoldAdjustPlan  extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应序列SEQ_LH_MOLD_ADJUST_PLAN
     */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 对应MES单据号数据
     */
    @ApiModelProperty(value = "对应MES单据号数据")
    private String orderNo;

    /**
     * 类型（类型）
     */
    @ApiModelProperty(value = "类型")
    private String type;

    /**
     * 硫化机台编号
     */
    @ApiModelProperty(value = "硫化机台编号")
    private String lhMachineCode;

    /**
     * 计划日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "计划日期")
    private Date planDate;

    /**
     * 换模时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "换模时间")
    private Date changeMoldTime;

    /**
     * 0:未装配；1:已装配
     */
    @ApiModelProperty(value = "0:未装配；1:已装配")
    private String moldStatus;

    /**
     * 0:未通过；1:已通过
     */
    @ApiModelProperty(value = "0:未通过；1:已通过")
    private String auditStatus;

    /**
     * 0：模具准备；1停模等
     */
    @ApiModelProperty(value = "0：模具准备；1停模等")
    private Long process;

    /**
     * 0:未执行；1:已完成
     */
    @ApiModelProperty(value = "0:未执行；1:已完成")
    private String executionStatus;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "开始时间")
    private Date beginTime;

    /**
     * 完成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "完成时间")
    private Date finishTime;

    /**
     * 左外胎品号
     */
    @ApiModelProperty(value = "左外胎品号")
    private String leftSapCode;

    /**
     * 右外胎品号
     */
    @ApiModelProperty(value = "右外胎品号")
    private String rightSapCode;

    /**
     * 左胎胚品号
     */
    @ApiModelProperty(value = "左胎胚品号")
    private String leftEmbryoSapCode;

    /**
     * 右胎胚品号
     */
    @ApiModelProperty(value = "右胎胚品号")
    private String rightEmbryoSapCode;

    /**
     * 左规格描述
     */
    @ApiModelProperty(value = "左规格描述")
    private String leftSpecDesc;

    /**
     * 右规格描述
     */
    @ApiModelProperty(value = "右规格描述")
    private String rightSpecDesc;

    /**
     * 原左规格描述
     */
    @ApiModelProperty(value = "原左规格描述")
    private String leftBeforeSpecDesc;

    /**
     * 原右规格描述
     */
    @ApiModelProperty(value = "原右规格描述")
    private String rightBeforeSpecDesc;

    /**
     * 左胎胚代号
     */
    @ApiModelProperty(value = "左胎胚代号")
    private String leftEmbryoCode;

    /**
     * 右胎胚代号
     */
    @ApiModelProperty(value = "右胎胚代号")
    private String rightEmbryoCode;

    /**
     * 左蒸锅
     */
    @ApiModelProperty(value = "左蒸锅")
    private Long leftStream;

    /**
     * 右蒸锅
     */
    @ApiModelProperty(value = "右蒸锅")
    private Long rightStream;

    /**
     * 左模具信息
     */
    @ApiModelProperty(value = "左模具信息")
    private String leftMoldInfo;

    /**
     * 右模具信息
     */
    @ApiModelProperty(value = "右模具信息")
    private String rightMoldInfo;

    /**
     * 模具装配人
     */
    @ApiModelProperty(value = "模具装配人")
    private String moldAssembler;

    /**
     * 模具装配时间
     */
    @ApiModelProperty(value = "模具装配时间")
    private Date moldAssembleTime;

    /**
     * 审核人
     */
    @ApiModelProperty(value = "审核人")
    private String auditor;

    /**
     * 审核时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "审核时间")
    private Date auditTime;

    /**
     * 审核意见
     */
    @ApiModelProperty(value = "审核意见")
    private String auditSuggest;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;

    /**
     * 检索日期条件
     */
    private String planDateStr;
}
