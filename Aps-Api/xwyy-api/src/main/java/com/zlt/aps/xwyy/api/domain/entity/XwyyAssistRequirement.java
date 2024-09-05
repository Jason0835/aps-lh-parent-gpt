package com.zlt.aps.xwyy.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 纤维压延外厂需求对象 t_xwyy_assist_requirement
 *
 * @author chen
 * @date 2022-03-14
 */
@ApiModel(value = "纤维压延外厂需求对象", description = "纤维压延外厂需求对象 ")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_XWYY_ASSIST_REQUIREMENT")
@KeySequence(value = "SEQ_XWYY_SCHEDULE", clazz = Long.class)
public class XwyyAssistRequirement extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_XWYY_SCHEDULE
     */
    @ApiModelProperty(value = "id")
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;

    /**
     * 排程日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期")
    @ImportValidated(required = true, date = true)
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    /**
     * 帘布大卷编号
     */
    @Excel(name = "ui.data.column.xwyy.scheduleResult.bigRollCode")
    @ApiModelProperty(value = "帘布大卷编号")
    @ImportValidated(required = true, isCode = true, maxLength = 20)
    @TableField(value = "BIG_ROLL_CODE")
    private String bigRollCode;

    /**
     * 班白(10点-0点)计划量
     */
    @Excel(name = "ui.data.column.assistRequirement.midPlan")
    @ApiModelProperty(value = "班白(10点-0点)计划量")
    @ImportValidated(required = true, digits = true, min = 0, max = 9999999)
    @TableField(value = "DAY_PLAN_QTY")
    private BigDecimal dayPlanQty;

    /**
     * 夜班(0点-10点)计划量
     */
    @Excel(name = "ui.data.column.assistRequirement.nightPlan")
    @ApiModelProperty(value = "夜班(0点-10点)计划量")
    @ImportValidated(required = true, digits = true, min = 0, max = 9999999)
    @TableField(value = "NIGHT_PLAN_QTY")
    private BigDecimal nightPlanQty;
    
    /**
     * 当日库存
     */
    @Excel(name = "ui.data.column.scheduleResult.todayStock")
    @ApiModelProperty(value = "当日库存")
    @ImportValidated(required = true, digits = true, min = 0, max = 9999999)
    @TableField(value = "TODAY_STOCK")
    private BigDecimal todayStock;
    
	/**
	 * 白班外厂应支
	 */
	@ImportValidated(number = true, min = 0, max = 9999999, isInteger = true)
	@Excel(name = "ui.data.column.scheduleResult.dayOut")
	@ApiModelProperty(value = "白班外厂应支")
    @TableField(value = "DAY_OUT")
	private BigDecimal dayOut;

	/**
	 * 5厂中班计划量
	 */
	@ImportValidated(required = true, number = true, min = 0, max = 9999999, isInteger = true)
	@Excel(name = "ui.data.column.scheduleResult.fac5Class1Plan")
	@ApiModelProperty(value = "5厂中班计划量")
    @TableField(value = "FAC5_CLASS1_PLAN")
	private BigDecimal fac5Class1Plan;

	/**
	 * 5厂夜班计划量
	 */
	@ImportValidated(required = true, number = true, min = 0, max = 9999999, isInteger = true)
	@Excel(name = "ui.data.column.scheduleResult.fac5Class2Plan")
	@ApiModelProperty(value = "5厂夜班计划量")
    @TableField(value = "FAC5_CLASS2_PLAN")
	private BigDecimal fac5Class2Plan;

	/**
	 * 5厂白班计划量
	 */
	@ImportValidated(required = true, number = true, min = 0, max = 9999999, isInteger = true)
	@Excel(name = "ui.data.column.scheduleResult.fac5Class3Plan")
	@ApiModelProperty(value = "5厂白班计划量")
    @TableField(value = "FAC5_CLASS3_PLAN")
	private BigDecimal fac5Class3Plan;

    /**
     * 备注
     */
    @Excel(name = "ui.common.column.remark")
    @ImportValidated(maxLength = 300)
    @ApiModelProperty(value = "备注", position = 500)
    @TableField("REMARK")
    private String remark;
}
