package com.zlt.aps.mdm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
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
public class CxPrecisionPlan extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.cxPrecisionPlan.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编码")
    @TableField("FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.cxPrecisionPlan.accuracyType", dictType = "MACHINE_ACCURACY_TYPE")
    @ApiModelProperty(value = "精度类型")
    @TableField("ACCURACY_TYPE")
    private String accuracyType;

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
    private LocalDate planDate;

    @Excel(name = "ui.data.column.cxPrecisionPlan.planShift", dictType = "SHIFT_TYPE")
    @ApiModelProperty(value = "计划班次：SHIFT_DAY-早班，SHIFT_AFTERNOON-中班")
    @TableField("PLAN_SHIFT")
    private String planShift;

    @Excel(name = "ui.data.column.cxPrecisionPlan.planStartTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "计划开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("PLAN_START_TIME")
    private LocalDateTime planStartTime;

    @Excel(name = "ui.data.column.cxPrecisionPlan.planEndTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "计划结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("PLAN_END_TIME")
    private LocalDateTime planEndTime;

    @Excel(name = "ui.data.column.cxPrecisionPlan.estimatedHours")
    @ApiModelProperty(value = "预计时长（小时），默认4小时")
    @TableField("ESTIMATED_HOURS")
    private Integer estimatedHours;

    @Excel(name = "ui.data.column.cxPrecisionPlan.lastPrecisionDate", dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "上次精度日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("LAST_PRECISION_DATE")
    private LocalDate lastPrecisionDate;

    @Excel(name = "ui.data.column.cxPrecisionPlan.dueDate", dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "到期日期（下次应做精度日期）")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("DUE_DATE")
    private LocalDate dueDate;

    @Excel(name = "ui.data.column.cxPrecisionPlan.status", dictType = "PRECISION_PLAN_STATUS")
    @ApiModelProperty(value = "状态：PLANNED-已计划，IN_PROGRESS-进行中，COMPLETED-已完成，CANCELLED-已取消")
    @TableField("STATUS")
    private String status;

    @Excel(name = "ui.data.column.cxPrecisionPlan.arrangeReason", dictType = "PRECISION_ARRANGE_REASON")
    @ApiModelProperty(value = "安排原因：SCHEDULED-正常安排，URGENT-紧急安排，RESCHEDULED-重排")
    @TableField("ARRANGE_REASON")
    private String arrangeReason;

    @Excel(name = "ui.data.column.cxPrecisionPlan.affectVulcanize", readConverterExp = "0=否,1=是")
    @ApiModelProperty(value = "是否影响硫化：0-否 1-是")
    @TableField("AFFECT_VULCANIZE")
    private Integer affectVulcanize;

    @Excel(name = "ui.data.column.cxPrecisionPlan.vulcanizeReduceRatio")
    @ApiModelProperty(value = "硫化减产比例（0-1），0表示不减产，0.5表示减半")
    @TableField("VULCANIZE_REDUCE_RATIO")
    private java.math.BigDecimal vulcanizeReduceRatio;

    @Excel(name = "ui.data.column.cxPrecisionPlan.embryoCode")
    @ApiModelProperty(value = "关联胎胚编码（主要生产的胎胚）")
    @TableField("EMBRYO_CODE")
    private String embryoCode;

    @Excel(name = "ui.data.column.cxPrecisionPlan.remark")
    @ApiModelProperty(value = "备注")
    @TableField("REMARK")
    private String remark;

    @ApiModelProperty(value = "计划日期开始（搜索用）")
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planDateBegin;

    @ApiModelProperty(value = "计划日期结束（搜索用）")
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planDateEnd;

    @ApiModelProperty(value = "实际日期开始（搜索用）")
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime actualDateBegin;

    @ApiModelProperty(value = "实际日期结束（搜索用）")
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime actualDateEnd;

    @ApiModelProperty(value = "实际完成日期")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("ACTUAL_DATE")
    private LocalDateTime actualDate;
}
