package com.zlt.aps.cd90.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 90度裁断排程结果表
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-23
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_CD90_SCHEDULE_RESULT")
@ApiModel(value = "Cd90ScheduleResult对象", description = "90度裁断排程结果表")
//@KeySequence(value = "SEQ_PUBLIC",dbType = DbType.ORACLE)
public class Cd90ScheduleResult extends ApsBaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "排程日期")
    @TableField("SCHEDULE_DATE")
    private LocalDateTime scheduleDate;

//    @ApiModelProperty(value = "对应的成型批次号")
//    @TableField("CX_BATCH_NO")
//    private String cxBatchNo;

    @ApiModelProperty(value = "批次号，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号")
    @TableField("BATCH_NO")
    private String batchNo;

//    @ApiModelProperty(value = "工单号，自动生成（批次号+4位定长自增序号）")
//    @TableField("ORDER_NO")
//    private String orderNo;

    @ApiModelProperty(value = "帘布大卷编号")
    @TableField("BIG_ROLL_CODE")
    private String bigRollCode;

    @ApiModelProperty(value = "帘布代码")
    @TableField("CLOTH_CODE")
    private String clothCode;

    @ApiModelProperty(value = "单耗")
    @TableField("UNIT_CONSUME")
    private BigDecimal unitConsume;

    @ApiModelProperty(value = "机台ID，多个逗号分割")
    @TableField("MACHINE_ID")
    private String machineId;

    @ApiModelProperty(value = "库存数量")
    @TableField("STOCK_QTY")
    private Integer stockQty;

    @ApiModelProperty(value = "库存供应成型时长，单位：小时")
    @TableField("SUPPLY_TIME")
    private BigDecimal supplyTime;

    @ApiModelProperty(value = "中班(12点-24点)计划量")
    @TableField("DAY_PLAN_QTY")
    private Integer dayPlanQty;

    @ApiModelProperty(value = "中班(12点-24点)完成量")
    @TableField("DAY_FINISH_QTY")
    private Integer dayFinishQty;

    @ApiModelProperty(value = "中班(12点-24点)生产顺序")
    @TableField("DAY_PRODUCE_ORDER")
    private Integer dayProduceOrder;

    @ApiModelProperty(value = "中班(12点-24点)完成率")
    @TableField("DAY_FINISH_RATE")
    private BigDecimal dayFinishRate;

    @ApiModelProperty(value = "中班(12点-24点)系统原因分析")
    @TableField("DAY_SYS_ANALYSIS")
    private String daySysAnalysis;

    @ApiModelProperty(value = "中班(12点-24点)手动输入原因分析")
    @TableField("DAY_HAND_ANALYSIS")
    private String dayHandAnalysis;

    @ApiModelProperty(value = "夜班(0点-12点)计划量")
    @TableField("NIGHT_PLAN_QTY")
    private Integer nightPlanQty;

    @ApiModelProperty(value = "夜班(0点-12点)完成量")
    @TableField("NIGHT_FINISH_QTY")
    private Integer nightFinishQty;

    @ApiModelProperty(value = "夜班(0点-12点)生产顺序")
    @TableField("NIGHT_PRODUCE_ORDER")
    private Integer nightProduceOrder;

    @ApiModelProperty(value = "夜班(0点-12点)完成率")
    @TableField("NIGHT_FINISH_RATE")
    private BigDecimal nightFinishRate;

    @ApiModelProperty(value = "夜班(0点-12点)系统原因分析")
    @TableField("NIGHT_SYS_ANALYSIS")
    private String nightSysAnalysis;

    @ApiModelProperty(value = "夜班(0点-12点)手动输入原因分析")
    @TableField("NIGHT_HAND_ANALYSIS")
    private String nightHandAnalysis;

    @ApiModelProperty(value = "对应成型一班的计划量")
    @TableField("CX_CLASS1_PLAN")
    private Integer cxClass1Plan;

    @ApiModelProperty(value = "对应成型二班的计划量")
    @TableField("CX_CLASS2_PLAN")
    private Integer cxClass2Plan;

    @ApiModelProperty(value = "对应成型三班的计划量")
    @TableField("CX_CLASS3_PLAN")
    private Integer cxClass3Plan;

    @ApiModelProperty(value = "对应成型次一班的计划量")
    @TableField("CX_CLASS4_PLAN")
    private Integer cxClass4Plan;

    @ApiModelProperty(value = "对应成型次二班的计划量")
    @TableField("CX_CLASS5_PLAN")
    private Integer cxClass5Plan;

    @ApiModelProperty(value = "是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE")
    @TableField("IS_RELEASE")
    private String isRelease;

    @ApiModelProperty(value = "排程记录id数组")
    @TableField(exist = false)
    private Long[] ids;
}
