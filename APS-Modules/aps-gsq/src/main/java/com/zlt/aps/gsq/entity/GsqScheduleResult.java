package com.zlt.aps.gsq.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.JdbcType;
import org.springframework.data.annotation.Transient;

import java.util.Date;

/**
 * <p>
 * 钢丝圈排程结果表
 * </p>
 *
 * @author chen
 * @since 2021-06-21
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_GSQ_SCHEDULE_RESULT")
@ApiModel(value = "GsqScheduleResult对象", description = "钢丝圈排程结果表")
@KeySequence(value = "SEQ_GSQ_SCHEDULE", clazz = Long.class)
public class GsqScheduleResult extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_GSQ_SCHEDULE")
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "排程日期")
    @TableField("SCHEDULE_DATE")
    private Date scheduleDate;

    @ApiModelProperty(value = "对应的成型批次号")
    @TableField("TQ_BATCH_NO")
    private String tqBatchNo;

    @ApiModelProperty(value = "批次号，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号")
    @TableField("BATCH_NO")
    private String batchNo;

    @ApiModelProperty(value = "工单号，自动生成（批次号+4位定长自增序号）")
    @TableField("ORDER_NO")
    private String orderNo;

    @ApiModelProperty(value = "钢丝类型")
    @TableField("STEEL_TYPE")
    private String steelType;

    @ApiModelProperty(value = "钢丝圈代码")
    @TableField("STEEL_RING_CODE")
    private String steelRingCode;

    @ApiModelProperty(value = "寸口")
    @TableField(exist = false)
    private String dimension;

    @ApiModelProperty(value = "排列")
    @Transient
    @TableField("RANK")
    private String rank;

    @ApiModelProperty(value = "机台id")
    @TableField("MACHINE_Id")
    private String machineId;

    @ApiModelProperty(value = "单耗")
    @TableField("UNIT_CONSUME")
    private Double unitConsume;

    @ApiModelProperty(value = "库存数量")
    @TableField("STOCK_QTY")
    private Double stockQty;

    @ApiModelProperty(value = "库存供应成型时长，单位：小时")
    @TableField("SUPPLY_TIME")
    private Double supplyTime;

    @ApiModelProperty(value = "中班(16点-24点)计划量(条)")
    @TableField("MID_PLAN_QTY")
    private Double midPlanQty;

    @ApiModelProperty(value = "中班(16点-24点)完成量")
    @TableField("MID_FINISH_QTY")
    private Double midFinishQty;

    @ApiModelProperty(value = "中班(16点-24点)生产顺序")
    @TableField(value = "MID_PRODUCE_ORDER",updateStrategy = FieldStrategy.IGNORED,jdbcType= JdbcType.INTEGER)
    private Integer midProduceOrder;

    @ApiModelProperty(value = "中班(16点-24点)完成率")
    @TableField("MID_FINISH_RATE")
    private Double midFinishRate;

    @ApiModelProperty(value = "中班(16点-24点)系统原因分析")
    @TableField("MID_SYS_ANALYSIS")
    private String midSysAnalysis;

    @ApiModelProperty(value = "中班(16点-24点)手动输入原因分析")
    @TableField("MID_HAND_ANALYSIS")
    private String midHandAnalysis;

    @ApiModelProperty(value = "夜班(0点-8点)计划量(条)")
    @TableField("NIGHT_PLAN_QTY")
    private Double nightPlanQty;

    @ApiModelProperty(value = "夜班(0点-8点)完成量")
    @TableField("NIGHT_FINISH_QTY")
    private Double nightFinishQty;

    @ApiModelProperty(value = "夜班(0点-8点)生产顺序")
    @TableField(value = "NIGHT_PRODUCE_ORDER",updateStrategy = FieldStrategy.IGNORED,jdbcType= JdbcType.INTEGER)
    private Integer nightProduceOrder;

    @ApiModelProperty(value = "夜班(0点-8点)完成率")
    @TableField("NIGHT_FINISH_RATE")
    private Double nightFinishRate;

    @ApiModelProperty(value = "夜班(0点-8点)系统原因分析")
    @TableField("NIGHT_SYS_ANALYSIS")
    private String nightSysAnalysis;

    @ApiModelProperty(value = "夜班(0点-8点)手动输入原因分析")
    @TableField("NIGHT_HAND_ANALYSIS")
    private String nightHandAnalysis;

    @ApiModelProperty(value = "白班(8点-16点)计划量(条)")
    @TableField("DAY_PLAN_QTY")
    private Double dayPlanQty;

    @ApiModelProperty(value = "白班(8点-16点)完成量")
    @TableField("DAY_FINISH_QTY")
    private Double dayFinishQty;

    @ApiModelProperty(value = "白班(8点-16点)生产顺序")
    @TableField("DAY_PRODUCE_ORDER")
    private Integer dayProduceOrder;

    @ApiModelProperty(value = "白班(8点-16点)完成率")
    @TableField(value = "DAY_FINISH_RATE",updateStrategy = FieldStrategy.IGNORED,jdbcType= JdbcType.INTEGER)
    private Double dayFinishRate;

    @ApiModelProperty(value = "白班(8点-16点)系统原因分析")
    @TableField("DAY_SYS_ANALYSIS")
    private String daySysAnalysis;

    @ApiModelProperty(value = "白班(8点-16点)手动输入原因分析")
    @TableField("DAY_HAND_ANALYSIS")
    private String dayHandAnalysis;

    @ApiModelProperty(value = "对应成型一班的计划量")
    @TableField("CX_CLASS1_PLAN")
    private Double cxClass1Plan;

    @ApiModelProperty(value = "对应成型二班的计划量")
    @TableField("CX_CLASS2_PLAN")
    private Double cxClass2Plan;

    @ApiModelProperty(value = "对应成型三班的计划量")
    @TableField("CX_CLASS3_PLAN")
    private Double cxClass3Plan;

    @ApiModelProperty(value = "对应成型次一班的计划量")
    @TableField("CX_CLASS4_PLAN")
    private Double cxClass4Plan;

    @ApiModelProperty(value = "对应成型次二班的计划量")
    @TableField("CX_CLASS5_PLAN")
    private Double cxClass5Plan;

    @ApiModelProperty(value = "收尾提示标识(0:提示收尾；1:不需要提示)")
    @TableField("MARK_CLOSE_OUT_TIP")
    private String markCloseOutTip;

    @ApiModelProperty(value = "关联汇总表年份", position = 600)
    @TableField(exist = false)
    private String year;

    @ApiModelProperty(value = "关联汇总表月份", position = 600)
    @TableField(exist = false)
    private String month;

    @ApiModelProperty(value = "是否发布")
    @TableField("IS_RELEASE")
    private String isRelease;

    @ApiModelProperty(value = "生产状态")
    @TableField("PRODUCTION_STATUS")
    private String productionStatus;

    @ApiModelProperty(value = "数据来源：0>自动排程；1>APS插单；2>导入；")
    private String dataSource;

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
}
