package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 成型精度计划实体（设备校准）
 *
 * 品质部每周下发精度计划，指定哪些机台什么时候做精度校验。
 * - 每个机台每两个月做一次，每次4小时
 * - 正常提前3天安排
 * - 一天最多做2台
 * - 安排时段：胎胚库存够吃超过一个班→早班(7:30-11:30)；特殊情况→中班(13:00-17:00)
 *
 * 精度期间成型机停机，系统需判断硫化机是否减产：
 * - 胎胚库存够硫化机吃4小时以上→硫化机继续生产
 * - 不够→硫化机减产一半，等精度做完恢复
 *
 * @author APS Team
 */
@Data
@TableName("T_CX_PRECISION_PLAN")
@ApiModel(value = "成型精度计划", description = "成型机台精度校验计划")
public class CxPrecisionPlan extends BaseEntity implements  Serializable {

    private static final long serialVersionUID = 1L;


    @ApiModelProperty(value = "机台编码")
    @TableField("MACHINE_CODE")
    private String machineCode;

    @ApiModelProperty(value = "机台名称")
    @TableField("MACHINE_NAME")
    private String machineName;

    @ApiModelProperty(value = "计划日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("PLAN_DATE")
    private LocalDate planDate;

    @ApiModelProperty(value = "计划班次：SHIFT_DAY-早班，SHIFT_AFTERNOON-中班")
    @TableField("PLAN_SHIFT")
    private String planShift;

    @ApiModelProperty(value = "计划开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("PLAN_START_TIME")
    private LocalDateTime planStartTime;

    @ApiModelProperty(value = "计划结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("PLAN_END_TIME")
    private LocalDateTime planEndTime;

    @ApiModelProperty(value = "预计时长（小时），默认4小时")
    @TableField("ESTIMATED_HOURS")
    private Integer estimatedHours;

    @ApiModelProperty(value = "上次精度日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("LAST_PRECISION_DATE")
    private LocalDate lastPrecisionDate;

    @ApiModelProperty(value = "到期日期（下次应做精度日期）")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("DUE_DATE")
    private LocalDate dueDate;

    @ApiModelProperty(value = "状态：PLANNED-已计划，IN_PROGRESS-进行中，COMPLETED-已完成，CANCELLED-已取消")
    @TableField("STATUS")
    private String status;

    @ApiModelProperty(value = "关联胎胚编码（主要生产的胎胚）")
    @TableField("EMBRYO_CODE")
    private String embryoCode;

    @ApiModelProperty(value = "备注")
    @TableField("REMARK")
    private String remark;

}
