package com.zlt.aps.gsq.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 钢丝圈排程滚动更新日志主表实体
 *
 * <p>记录每次滚动更新的触发事件（手动操作或自动定时）。</p>
 * <p>MVP阶段仅使用主表，明细表后续补充。</p>
 *
 * @author APS
 * @since 2026-07-20
 */
@ApiModel(value = "钢丝圈排程滚动更新日志", description = "钢丝圈排程滚动更新日志主表")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_GSQ_ROLLING_LOG")
public class GsqRollingLog extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 滚动批次号（每次滚动唯一） */
    @Excel(name = "ui.data.column.gsqRollingLog.batchNo")
    @ApiModelProperty(value = "滚动批次号", name = "batchNo")
    @TableField(value = "BATCH_NO")
    private String batchNo;

    /** 触发类型：0-自动定时，1-插单，2-转机台，3-调量，4-删除 */
    @Excel(name = "ui.data.column.gsqRollingLog.triggerType", dictType = "TQ_ROLLING_TRIGGER_TYPE")
    @ApiModelProperty(value = "触发类型：0-自动定时，1-插单，2-转机台，3-调量，4-删除", name = "triggerType")
    @TableField(value = "TRIGGER_TYPE")
    private String triggerType;

    /** 触发源排程记录ID（手动操作时） */
    @ApiModelProperty(value = "触发源排程记录ID", name = "triggerSourceId")
    @TableField(value = "TRIGGER_SOURCE_ID")
    private Long triggerSourceId;

    /** 排程日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.gsqRollingLog.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    /** 触发班次索引（1~6） */
    @Excel(name = "ui.data.column.gsqRollingLog.shiftIndex")
    @ApiModelProperty(value = "触发班次索引（1~6）", name = "shiftIndex")
    @TableField(value = "SHIFT_INDEX")
    private Integer shiftIndex;

    /** 触发机台编号 */
    @Excel(name = "ui.data.column.gsqRollingLog.machineCode")
    @ApiModelProperty(value = "触发机台编号", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    /** 触发钢丝圈代码 */
    @Excel(name = "ui.data.column.gsqRollingLog.steelRingCode")
    @ApiModelProperty(value = "触发钢丝圈代码", name = "steelRingCode")
    @TableField(value = "STEEL_RING_CODE")
    private String steelRingCode;

    /** 滚动前预计库存 */
    @Excel(name = "ui.data.column.gsqRollingLog.beforeStockQty")
    @ApiModelProperty(value = "滚动前预计库存", name = "beforeStockQty")
    @TableField(value = "BEFORE_STOCK_QTY")
    private BigDecimal beforeStockQty;

    /** 滚动后预计库存 */
    @Excel(name = "ui.data.column.gsqRollingLog.afterStockQty")
    @ApiModelProperty(value = "滚动后预计库存", name = "afterStockQty")
    @TableField(value = "AFTER_STOCK_QTY")
    private BigDecimal afterStockQty;

    /** 调整原因（向上修正/向下修正/插单/转机台等） */
    @Excel(name = "ui.data.column.gsqRollingLog.adjustReason")
    @ApiModelProperty(value = "调整原因", name = "adjustReason")
    @TableField(value = "ADJUST_REASON")
    private String adjustReason;

    /** 影响的排程记录数 */
    @Excel(name = "ui.data.column.gsqRollingLog.affectedCount")
    @ApiModelProperty(value = "影响的排程记录数", name = "affectedCount")
    @TableField(value = "AFFECTED_COUNT")
    private Integer affectedCount;

    /** 执行状态：0-进行中，1-成功，2-失败 */
    @Excel(name = "ui.data.column.gsqRollingLog.status", dictType = "TQ_ROLLING_STATUS")
    @ApiModelProperty(value = "执行状态：0-进行中，1-成功，2-失败", name = "status")
    @TableField(value = "STATUS")
    private String status;

    /** 失败原因（STATUS=2时填写） */
    @ApiModelProperty(value = "失败原因", name = "errorMsg")
    @TableField(value = "ERROR_MSG")
    private String errorMsg;

    /** 分公司编码 */
    @ApiModelProperty(value = "分公司编码", name = "companyCode")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    /** 厂别 */
    @Excel(name = "ui.data.column.gsqRollingLog.factoryCode")
    @ApiModelProperty(value = "厂别", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;
}
