package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：DemoYearPlanFinish.java
 * 描    述：年计划完成量演示对象 t_demo_year_plan_finish
 *@author zlt
 *@date 2025-11-27
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@ApiModel(value = "年计划完成量演示对象", description = "年计划完成量演示对象 ")
@Data
@TableName(value = "T_DEMO_YEAR_PLAN_FINISH")
public class DemoYearPlanFinish extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 年份 */
    @Excel(name = "ui.data.column.demoYearPlanFinish.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /** 月份 */
    @Excel(name = "ui.data.column.demoYearPlanFinish.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /** 月计划排产量 */
    @Excel(name = "ui.data.column.demoYearPlanFinish.planQty")
    @ApiModelProperty(value = "月计划排产量", name = "planQty")
    @TableField(value = "PLAN_QTY")
    private Integer planQty;

    /** 月计划累计完成量 */
    @Excel(name = "ui.data.column.demoYearPlanFinish.finishQty")
    @ApiModelProperty(value = "月计划累计完成量", name = "finishQty")
    @TableField(value = "FINISH_QTY")
    private Integer finishQty;


}
