package com.zlt.aps.cx.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 成型前日计划增补对象 t_cx_last_day_supple_plan
 *
 * @author chen
 * @date 2022-02-09
 */
@ApiModel(value = "成型前日计划增补对象", description = "成型前日计划增补对象 ")
@Data
@EqualsAndHashCode(callSuper = true)
public class CxLastDaySupplePlanDto extends CxScheduleResult {

    private static final long serialVersionUID = 1L;

    /**
     * 成型增补计划批次号
     */
    @Excel(name = "ui.data.column.plan.suppleBatchNo")
    @ApiModelProperty(value = "成型增补计划批次号")
    private String suppleBatchNo;

    /**
     * 库存地点中文描述
     */
    @Excel(name = "ui.data.column.plan.storageLocationDesc")
    @ApiModelProperty(value = "库存地点中文描述")
    private String storageLocationDesc;

    /**
     * 计划增补量
     */
    @Excel(name = "ui.data.column.plan.supplePlanQty")
    @ApiModelProperty(value = "计划增补量")
    private Long supplePlanQty;

    @Excel(name = "ui.data.column.status", dictType = "supply_plan_status")
    @ApiModelProperty(value = "状态，0:未确认；1:已确认")
    private String status;

    @Excel(name = "ui.data.column.cx.lastDaySupplyPlan.confirmSuppleStatus", dictType = "supply_plan_status")
    @ApiModelProperty(value = "确认增补状态，0:未确认；1:已确认")
    private String confirmSuppleStatus;

    @Excel(name = "ui.data.column.cx.lastDaySupplyPlan.planSort")
    @ApiModelProperty(value = "计划生产顺序")
    private Integer planSort;
}
