package com.zlt.aps.mp.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryMonthPlanProductionFinalResult.java
 * 描    述：工厂月生产计划-最终排产计划定稿对象 t_mp_month_plan_prod_final
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "T_MP_MONTH_PLAN_PROD_FINAL")
@ApiModel(value = "工厂月生产计划-最终排产计划定稿对象", description = "工厂月生产计划-最终排产计划定稿对象")
public class FactoryMonthPlanProductionFinal4AdjustVo extends FactoryMonthPlanProductionFinalResult {

    /** 排产净需求 */
    @Excel(name = "ui.data.column.demandPlanSum.netQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "排产净需求", name = "netQty")
    @TableField(value = "NET_QTY")
    private Integer netQty;

    /** 高优先级 */
    @Excel(name = "ui.data.column.demandPlanSum.heightQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "高优先级", name = "heightQty")
    @TableField(value = "HEIGHT_QTY")
    private Integer heightQty;

    /** 中优先级 */
    @Excel(name = "ui.data.column.demandPlanSum.midQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "中优先级", name = "midQty")
    @TableField(value = "MID_QTY")
    private Integer midQty;

    /** 周期排产储备 */
    @Excel(name = "ui.data.column.demandPlanSum.cycleReserveQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "周期排产储备", name = "cycleReserveQty")
    @TableField(value = "CYCLE_RESERVE_QTY")
    private Integer cycleReserveQty;

    /** 常规储备 */
    @Excel(name = "ui.data.column.demandPlanSum.conventionReserveQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "常规储备", name = "conventionReserveQty")
    @TableField(value = "CONVENTION_RESERVE_QTY")
    private Integer conventionReserveQty;

    /** 是否锁定上机日期：0-否，1-是 */
    @Excel(name = "ui.data.column.mpAdjustResult.isLockSchedule", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否锁定上机日期：0-否，1-是", name = "isLockSchedule")
    @TableField(value = "IS_LOCK_SCHEDULE")
    private String isLockSchedule;

    /** 是否含特殊物料 */
    @Excel(name = "ui.data.column.mpAdjustResult.hasSpecialMaterial")
    @ApiModelProperty(value = "是否含特殊物料", name = "hasSpecialMaterial")
    @TableField(value = "HAS_SPECIAL_MATERIAL")
    private String hasSpecialMaterial;

    /** 版本规则：ADJ+年月日+3位流水号； */
//    @Excel(name = "ui.data.column.mpAdjustResult.version")
    @ApiModelProperty(value = "版本规则：ADJ+年月日+3位流水号；", name = "version")
    @TableField(value = "VERSION")
    private String version;
}
