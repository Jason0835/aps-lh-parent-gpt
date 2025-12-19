package com.zlt.aps.monthplan.api.domain.dto;


import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal_JY;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustStructureIn;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author Sandy
 * @version 1.0
 * @Description 周程滚动调整上下文对象
 * @date 2025/12/19
 */
@Data
public class MpRollAdjustContextDTO implements Serializable {


    private static final long serialVersionUID = 8736122348031246577L;

    @ApiModelProperty(value = "月度计划年份")
    private Integer mpYear;

    @ApiModelProperty(value = "月度计划月份")
    private Integer mpMonth;

    @ApiModelProperty(value = "结构内调整记录")
    private List<MpAdjustStructureIn> mpAdjustStructureInList;

    @ApiModelProperty(value = "月计划调整最终结果表")
    private List<FactoryMonthPlanProdFinal> factoryMonthPlanProdFinalList;

    @ApiModelProperty(value = "排程过程日志")
    private StringBuilder logDetail;
}
