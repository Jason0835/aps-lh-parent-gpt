package com.zlt.aps.cd90.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** 直裁排程库排分配明细。 */
@Data
@TableName("t_cd90_schedule_lane_allocation")
public class Cd90ScheduleLaneAllocation extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 工厂编码。 */
    @TableField("FACTORY_CODE") private String factoryCode;
    /** 排程日期。 */
    @TableField("SCHEDULE_DATE") private Date scheduleDate;
    /** 排程批次号。 */
    @TableField("BATCH_NO") private String batchNo;
    /** 排程主结果ID。 */
    @TableField("SCHEDULE_RESULT_ID") private Long scheduleResultId;
    /** 排程工单号。 */
    @TableField("ORDER_NO") private String orderNo;
    /** 班次字段。 */
    @TableField("CLASS_FIELD") private String classField;
    /** 班次实际排班日期。 */
    @TableField("SHIFT_SCHEDULE_DATE") private Date shiftScheduleDate;
    /** 库排编码。 */
    @TableField("STORAGE_LANE_CODE") private String storageLaneCode;
    /** 帘布代号。 */
    @TableField("CLOTH_CODE") private String clothCode;
    /** 分配数量。 */
    @TableField("ALLOCATED_QTY") private Double allocatedQty;
    /** 分配车数。 */
    @TableField("ALLOCATED_CART_COUNT") private Integer allocatedCartCount;
    /** 分配顺序。 */
    @TableField("ALLOCATION_ORDER") private Integer allocationOrder;

    /** 备注 */
    @ApiModelProperty(value = "备注", name = "remark")
    @TableField("REMARK")
    @Excel(name = "ui.common.column.remark")
    private String remark;
}
