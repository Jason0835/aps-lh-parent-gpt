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

/** 15度斜裁排程库排分配明细。 */
@Data
@ApiModel(value = "15度斜裁排程库排分配明细", description = "15度斜裁排程库排分配明细")
@TableName("t_cd15_schedule_lane_allocation")
public class Cd15ScheduleLaneAllocation extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编码 */
    @ApiModelProperty("工厂编码")
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.cd15ScheduleLaneAllocation.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;

    /** 排程日期 */
    @ApiModelProperty("排程日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("SCHEDULE_DATE")
    @Excel(name = "ui.data.column.cd15ScheduleLaneAllocation.scheduleDate")
    private Date scheduleDate;

    /** 排程批次号 */
    @ApiModelProperty("排程批次号")
    @TableField("BATCH_NO")
    @Excel(name = "ui.data.column.cd15ScheduleLaneAllocation.batchNo")
    private String batchNo;

    /** 对应排程结果ID */
    @ApiModelProperty("对应排程结果ID")
    @TableField("SCHEDULE_RESULT_ID")
    @Excel(name = "ui.data.column.cd15ScheduleLaneAllocation.scheduleResultId")
    private Long scheduleResultId;

    /** 工单号 */
    @ApiModelProperty("工单号")
    @TableField("ORDER_NO")
    @Excel(name = "ui.data.column.cd15ScheduleLaneAllocation.orderNo")
    private String orderNo;

    /** 分裁组号 */
    @ApiModelProperty("分裁组号")
    @TableField("GROUP_NO")
    @Excel(name = "ui.data.column.cd15ScheduleLaneAllocation.groupNo")
    private String groupNo;

    /** 班次字段 */
    @ApiModelProperty("班次字段")
    @TableField("CLASS_FIELD")
    @Excel(name = "ui.data.column.cd15ScheduleLaneAllocation.classField")
    private String classField;

    /** 班次排程日期 */
    @ApiModelProperty("班次排程日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("SHIFT_SCHEDULE_DATE")
    @Excel(name = "ui.data.column.cd15ScheduleLaneAllocation.shiftScheduleDate")
    private Date shiftScheduleDate;

    /** 库排号 */
    @ApiModelProperty("库排号")
    @TableField("STORAGE_LANE_CODE")
    @Excel(name = "ui.data.column.cd15ScheduleLaneAllocation.storageLaneCode")
    private String storageLaneCode;

    /** 钢带代码 */
    @ApiModelProperty("钢带代码")
    @TableField("STEEL_STRIP_CODE")
    @Excel(name = "ui.data.column.cd15ScheduleLaneAllocation.steelStripCode")
    private String steelStripCode;

    /** 大卷代码 */
    @ApiModelProperty("大卷代码")
    @TableField("BIG_ROLL_CODE")
    @Excel(name = "ui.data.column.cd15ScheduleLaneAllocation.bigRollCode")
    private String bigRollCode;

    /** 裁断角度 */
    @ApiModelProperty("裁断角度")
    @TableField("CUTTING_ANGLE")
    @Excel(name = "ui.data.column.cd15ScheduleLaneAllocation.cuttingAngle")
    private String cuttingAngle;

    /** 机台编码 */
    @ApiModelProperty("机台编码")
    @TableField("MACHINE_CODE")
    @Excel(name = "ui.data.column.cd15ScheduleLaneAllocation.machineCode")
    private String machineCode;

    /** 分配数量 */
    @ApiModelProperty("分配数量")
    @TableField("ALLOCATED_QTY")
    @Excel(name = "ui.data.column.cd15ScheduleLaneAllocation.allocatedQty")
    private BigDecimal allocatedQty;

    /** 分配车数 */
    @ApiModelProperty("分配车数")
    @TableField("ALLOCATED_CART_COUNT")
    @Excel(name = "ui.data.column.cd15ScheduleLaneAllocation.allocatedCartCount")
    private Integer allocatedCartCount;

    /** 分配顺序 */
    @ApiModelProperty("分配顺序")
    @TableField("ALLOCATION_ORDER")
    @Excel(name = "ui.data.column.cd15ScheduleLaneAllocation.allocationOrder")
    private Integer allocationOrder;
}