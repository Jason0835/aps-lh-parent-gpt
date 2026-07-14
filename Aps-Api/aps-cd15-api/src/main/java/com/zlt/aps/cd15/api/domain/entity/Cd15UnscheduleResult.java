package com.zlt.aps.cd15.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** CD15斜裁自动排程未排结果。 */
@Data
@ApiModel(value = "CD15斜裁未排结果", description = "CD15斜裁自动排程未排结果")
@TableName("t_cd15_unschedule_result")
public class Cd15UnscheduleResult extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编码 */
    @ApiModelProperty("工厂编码")
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.cd15UnscheduleResult.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;

    /** 排程日期 */
    @ApiModelProperty("排程日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("SCHEDULE_DATE")
    @Excel(name = "ui.data.column.cd15UnscheduleResult.scheduleDate")
    private Date scheduleDate;

    /** 钢带代码 */
    @ApiModelProperty("钢带代码")
    @TableField("STEEL_STRIP_CODE")
    @Excel(name = "ui.data.column.cd15UnscheduleResult.steelStripCode")
    private String steelStripCode;

    /** 大卷代码 */
    @ApiModelProperty("大卷代码")
    @TableField("BIG_ROLL_CODE")
    @Excel(name = "ui.data.column.cd15UnscheduleResult.bigRollCode")
    private String bigRollCode;

    /** 裁断角度 */
    @ApiModelProperty("裁断角度")
    @TableField("CUTTING_ANGLE")
    @Excel(name = "ui.data.column.cd15UnscheduleResult.cuttingAngle")
    private String cuttingAngle;

    /** 分裁组号 */
    @ApiModelProperty("分裁组号")
    @TableField("GROUP_NO")
    @Excel(name = "ui.data.column.cd15UnscheduleResult.groupNo")
    private String groupNo;

    /** 工单号 */
    @ApiModelProperty("工单号")
    @TableField("ORDER_NO")
    @Excel(name = "ui.data.column.cd15UnscheduleResult.orderNo")
    private String orderNo;

    /** 机台编码 */
    @ApiModelProperty("机台编码")
    @TableField("MACHINE_CODE")
    @Excel(name = "ui.data.column.cd15UnscheduleResult.machineCode")
    private String machineCode;

    /** 班次字段 */
    @ApiModelProperty("班次字段")
    @TableField("CLASS_FIELD")
    @Excel(name = "ui.data.column.cd15UnscheduleResult.classField")
    private String classField;

    /** 本轮需求数量 */
    @ApiModelProperty("本轮需求数量")
    @TableField("DEMAND_QTY")
    @Excel(name = "ui.data.column.cd15UnscheduleResult.demandQty")
    private BigDecimal demandQty;

    /** 本轮已排数量 */
    @ApiModelProperty("本轮已排数量")
    @TableField("SCHEDULED_QTY")
    @Excel(name = "ui.data.column.cd15UnscheduleResult.scheduledQty")
    private BigDecimal scheduledQty;

    /** 最终未排数量 */
    @ApiModelProperty("最终未排数量")
    @TableField("UNSCHEDULED_QTY")
    @Excel(name = "ui.data.column.cd15UnscheduleResult.unscheduledQty")
    private BigDecimal unscheduledQty;

    /** 首次失败阶段 */
    @ApiModelProperty("首次失败阶段")
    @TableField("FAIL_STAGE")
    @Excel(name = "ui.data.column.cd15UnscheduleResult.failStage", dictType = "UNSCHEDULE_FAIL_STAGE")
    private String failStage;

    /** 标准化未排原因编码 */
    @ApiModelProperty("标准化未排原因编码")
    @TableField("UNSCHEDULE_REASON_CODE")
    @Excel(name = "ui.data.column.cd15UnscheduleResult.unscheduleReasonCode", dictType = "UNSCHEDULE_REASON")
    private String unscheduleReasonCode;

    /** 同一规格失败原因顺序 */
    @ApiModelProperty("同一规格失败原因顺序，从1开始递增")
    @TableField("REASON_ORDER")
    @Excel(name = "ui.data.column.cd15UnscheduleResult.reasonOrder")
    private Integer reasonOrder;

    /** 是否主原因 */
    @ApiModelProperty("是否主原因：1-是，0-否")
    @TableField("IS_PRIMARY_REASON")
    private String primaryReason;

    /** 未排原因说明 */
    @ApiModelProperty("未排原因说明")
    @TableField("UNSCHEDULED_REASON")
    @Excel(name = "ui.data.column.cd15UnscheduleResult.unscheduledReason")
    private String unscheduledReason;

    /** 约束过滤前的候选机台编码 */
    @ApiModelProperty("约束过滤前的候选机台编码")
    @TableField("CANDIDATE_MACHINE_CODES")
    @Excel(name = "ui.data.column.cd15UnscheduleResult.candidateMachineCodes")
    private String candidateMachineCodes;

    /** 所属排程批次号 */
    @ApiModelProperty("所属排程批次号")
    @TableField("BATCH_NO")
    @Excel(name = "ui.data.column.cd15UnscheduleResult.batchNo")
    private String batchNo;

    /** 数据来源 */
    @ApiModelProperty("数据来源：1-自动排程，2-插单，3-导入")
    @TableField("DATA_SOURCE")
    private String dataSource;

    /** 处理时间 */
    @ApiModelProperty("处理时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("PROCESSED_TIME")
    private Date processedTime;
}