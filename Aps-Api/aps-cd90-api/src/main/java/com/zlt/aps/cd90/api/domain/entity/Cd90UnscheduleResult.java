package com.zlt.aps.cd90.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** 直裁自动排程未排结果。 */
@Data
@ApiModel(value = "直裁未排结果", description = "直裁自动排程未排结果")
@TableName("t_cd90_unschedule_result")
public class Cd90UnscheduleResult extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 工厂编码 */
    @ApiModelProperty("工厂编码")
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.cd90UnscheduleResult.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;
    /** 排程日期 */
    @ApiModelProperty("排程日期")
    @TableField("SCHEDULE_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.cd90UnscheduleResult.scheduleDate")
    private Date scheduleDate;
    /** 帘布代号 */
    @ApiModelProperty("帘布代号")
    @TableField("CLOTH_CODE")
    @Excel(name = "ui.data.column.cd90UnscheduleResult.clothCode")
    private String clothCode;
    /** 帘布大卷编号 */
    @ApiModelProperty("帘布大卷编号")
    @TableField("BIG_ROLL_CODE")
    @Excel(name = "ui.data.column.cd90UnscheduleResult.bigRollCode")
    private String bigRollCode;
    /** 本轮需求数量（米） */
    @ApiModelProperty("本轮需求数量（米）")
    @TableField("DEMAND_QTY")
    @Excel(name = "ui.data.column.cd90UnscheduleResult.demandQty")
    private Double demandQty;
    /** 本轮已安排数量（米） */
    @ApiModelProperty("本轮已安排数量（米）")
    @TableField("SCHEDULED_QTY")
    @Excel(name = "ui.data.column.cd90UnscheduleResult.scheduledQty")
    private Double scheduledQty;
    /** 最终未排数量（米） */
    @ApiModelProperty("最终未排数量（米）")
    @TableField("UNSCHEDULED_QTY")
    @Excel(name = "ui.data.column.cd90UnscheduleResult.unscheduledQty")
    private Double unscheduledQty;
    /** 首次失败阶段 */
    @ApiModelProperty("首次失败阶段")
    @TableField("FAIL_STAGE")
    @Excel(name = "ui.data.column.cd90UnscheduleResult.failStage", dictType = "UNSCHEDULE_FAIL_STAGE")
    private String failStage;
    /** 标准化未排原因编码 */
    @ApiModelProperty("标准化未排原因编码")
    @TableField("REASON_CODE")
    @Excel(name = "ui.data.column.cd90UnscheduleResult.reasonCode", dictType = "UNSCHEDULE_REASON")
    private String reasonCode;
    /** 同一规格失败原因顺序 */
    @ApiModelProperty("同一规格失败原因顺序，从1开始递增")
    @TableField("REASON_ORDER")
    @Excel(name = "ui.data.column.cd90UnscheduleResult.reasonOrder")
    private Integer reasonOrder;
    /** 是否主原因：1-是，0-否 */
    @ApiModelProperty("是否主原因：1-是，0-否")
    @TableField("IS_PRIMARY_REASON")
    private String primaryReason;
    /** 未排原因说明 */
    @ApiModelProperty("未排原因说明")
    @TableField("UNSCHEDULED_REASON")
    @Excel(name = "ui.data.column.cd90UnscheduleResult.unscheduledReason")
    private String unscheduledReason;
    /** 约束过滤前的候选机台编码 */
    @ApiModelProperty("约束过滤前的候选机台编码")
    @TableField("CANDIDATE_MACHINE_CODES")
    @Excel(name = "ui.data.column.cd90UnscheduleResult.candidateMachineCodes")
    private String candidateMachineCodes;
    /** 所属排程批次号 */
    @ApiModelProperty("所属排程批次号")
    @TableField("BATCH_NO")
    @Excel(name = "ui.data.column.cd90UnscheduleResult.batchNo")
    private String batchNo;
    /** 数据来源：0-自动排程；1-插单；2-导入 */
    @ApiModelProperty("数据来源：0-自动排程；1-插单；2-导入")
    @TableField("DATA_SOURCE")
    private String dataSource;
    /** 处理时间 */
    @ApiModelProperty("处理时间")
    @TableField("PROCESSED_TIME")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date processedTime;

    /** 备注 */
    @ApiModelProperty(value = "备注", name = "remark")
    @TableField("REMARK")
    @Excel(name = "ui.common.column.remark")
    private String remark;
}
