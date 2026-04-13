package com.zlt.aps.cx.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

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
public class CxPrecisionPlan extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.cxPrecisionPlan.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编码")
    @TableField("FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.cxPrecisionPlan.machineCode")
    @ApiModelProperty(value = "机台编码")
    @TableField("MACHINE_CODE")
    private String machineCode;

    @Excel(name = "ui.data.column.cxPrecisionPlan.machineName")
    @ApiModelProperty(value = "机台名称")
    @TableField("MACHINE_NAME")
    private String machineName;

    @Excel(name = "ui.data.column.cxPrecisionPlan.planDate", dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "计划日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("PLAN_DATE")
    private Date planDate;

    @Excel(name = "ui.data.column.cxPrecisionPlan.planShift", dictType = "class_num_three_plan")
    @ApiModelProperty(value = "计划班次：SHIFT_DAY-早班，SHIFT_AFTERNOON-中班")
    @TableField("PLAN_SHIFT")
    private String planShift;

    @Excel(name = "ui.data.column.cxPrecisionPlan.planStartTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "计划开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("PLAN_START_TIME")
    private Date planStartTime;

    @Excel(name = "ui.data.column.cxPrecisionPlan.planEndTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "计划结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("PLAN_END_TIME")
    private Date planEndTime;

    @Excel(name = "ui.data.column.cxPrecisionPlan.estimatedHours")
    @ApiModelProperty(value = "预计时长（小时），默认4小时")
    @TableField("ESTIMATED_HOURS")
    private java.math.BigDecimal estimatedHours;

    @Excel(name = "ui.data.column.cxPrecisionPlan.lastPrecisionDate", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "上次精度日期")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("LAST_PRECISION_DATE")
    private Date lastPrecisionDate;

    @Excel(name = "ui.data.column.cxPrecisionPlan.dueDate", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "到期日期（下次应做精度日期）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("DUE_DATE")
    private Date dueDate;

    @ApiModelProperty(value = "距离到期日剩余天数")
    @TableField("DAYS_TO_DUE")
    private Integer daysToDue;

    @Excel(name = "ui.data.column.cxPrecisionPlan.actualDate", dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "实际执行日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("ACTUAL_DATE")
    private Date actualDate;

    @Excel(name = "ui.data.column.cxPrecisionPlan.remark")
    @ApiModelProperty(value = "备注")
    @TableField("REMARK")
    private String remark;

    @ApiModelProperty(value = "计划日期开始（搜索用）")
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date planDateBegin;

    @ApiModelProperty(value = "计划日期结束（搜索用）")
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date planDateEnd;
}
