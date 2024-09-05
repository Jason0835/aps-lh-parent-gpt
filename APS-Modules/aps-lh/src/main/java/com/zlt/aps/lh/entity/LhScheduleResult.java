package com.zlt.aps.lh.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * <p>
 * 硫化排程结果表
 * </p>
 *
 * @author chen
 * @since 2021-07-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_LH_SCHEDULE_RESULT")
@ApiModel(value = "LhScheduleResult对象", description = "硫化排程结果表")
@KeySequence(value = "SEQ_LH_SCHEDULE", clazz = Long.class)
public class LhScheduleResult extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "自动排程批次号信息，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号")
    @TableField("BATCH_NO")
    private String batchNo;

    @ApiModelProperty(value = "工单号，自动生成（工序+日期+三位顺序号001,002）")
    @TableField("ORDER_NO")
    private String orderNo;

    @ApiModelProperty(value = "硫化机台编号")
    @TableField("LH_MACHINE_CODE")
    private String lhMachineCode;

    @ApiModelProperty(value = "硫化机台名称")
    @TableField("LH_MACHINE_NAME")
    private String lhMachineName;

    @ApiModelProperty(value = "SAP品号信息")
    @TableField("SAP_CODE")
    private String sapCode;

    @ApiModelProperty(value = "规格描述信息")
    @TableField("SPEC_DESC")
    private String specDesc;

    @ApiModelProperty(value = "库区信息")
    @TableField("STOCK_AREA")
    private String stockArea;

    @ApiModelProperty(value = "硫化时长")
    @TableField("LH_TIME")
    private Double lhTime;

    @ApiModelProperty(value = "日计划数量")
    @TableField("DAILY_PLAN_QTY")
    private Long dailyPlanQty;

    @ApiModelProperty(value = "排程日期")
    @TableField("SCHEDULE_DATE")
    private Date scheduleDate;

    @ApiModelProperty(value = "生产状态:0-未生产；1-生产中；2-生产完成")
    @TableField("PRODUCTION_STATUS")
    private String productionStatus;

    @ApiModelProperty(value = "一班计划量")
    @TableField("CLASS1_PLAN_QTY")
    private Integer class1PlanQty;

    @ApiModelProperty(value = "一班原因分析")
    @TableField("CLASS1_ANALYSIS")
    private String class1Analysis;

    /** 一班原因分析手工录入 */
    @ApiModelProperty(value = "一班原因分析手工录入")
    @TableField("CLASS1_ANALYSIS_INPUT")
    private String class1AnalysisInput;

    @ApiModelProperty(value = "一班完成量")
    @TableField("CLASS1_FINISH_QTY")
    private Long class1FinishQty;

    @ApiModelProperty(value = "二班计划量")
    @TableField("CLASS2_PLAN_QTY")
    private Integer class2PlanQty;

    @ApiModelProperty(value = "二班原因分析")
    @TableField("CLASS2_ANALYSIS")
    private String class2Analysis;

    /** 二班原因分析手工录入 */
    @ApiModelProperty(value = "二班原因分析手工录入")
    @TableField("CLASS2_ANALYSIS_INPUT")
    private String class2AnalysisInput;

    @ApiModelProperty(value = "二班完成量")
    @TableField("CLASS2_FINISH_QTY")
    private Long class2FinishQty;

    @ApiModelProperty(value = "三班计划量")
    @TableField("CLASS3_PLAN_QTY")
    private Integer class3PlanQty;

    @ApiModelProperty(value = "三班原因分析")
    @TableField("CLASS3_ANALYSIS")
    private String class3Analysis;

    /** 三班原因分析手工录入 */
    @ApiModelProperty(value = "三班原因分析手工录入")
    @TableField("CLASS3_ANALYSIS_INPUT")
    private String class3AnalysisInput;

    @ApiModelProperty(value = "三班完成量")
    @TableField("CLASS3_FINISH_QTY")
    private Long class3FinishQty;

    @ApiModelProperty(value = "是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE")
    @TableField("IS_RELEASE")
    private String isRelease;

    @ApiModelProperty(value = "排程记录id数组")
    @TableField(exist = false)
    private Long[] ids;

    @ApiModelProperty(value = "发布成功计数器，每点击一次发布并成功的话，计数器累加")
    private Integer publishSuccessCount;

    /**
     * 最新发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "最新发布时间")
    private Date newestPublishTime;

    /**
     * Joran 2022-03-14 存储当前左右模情况，如果非单模单规格的则可为空，单模单规格则存储对应的模信息，如：存储内容，L/R、L1/R1
     */
    @ApiModelProperty(value = "左右模")
    private String leftRightMold;

    @ApiModelProperty(value = "胎胚代码")
    private String embryoCode;
}
