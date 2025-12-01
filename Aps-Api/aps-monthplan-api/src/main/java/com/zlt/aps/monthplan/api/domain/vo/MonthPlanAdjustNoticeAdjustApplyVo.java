package com.zlt.aps.monthplan.api.domain.vo;

import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 月计划调整通知单应用调整返回值对象
 *
 * @author ZLT
 * @date 20250625
 */
@Data
@ApiModel(value = "月计划调整通知单应用调整返回值对象", description = "月计划调整通知单应用调整返回值对象")
public class MonthPlanAdjustNoticeAdjustApplyVo implements Serializable {
    /**
     * 计划调减量换成对应SAP的可增加量
     */
    @ApiModelProperty(value = "计划调减量换成对应SAP的可增加量", name = "addAdjustQty")
    private Long addAdjustQty;
    /**
     * 计划调减后的数据
     */
    @ApiModelProperty(value = "计划调减后的数据", name = "updateData")
    private FactoryMonthPlanProdFinal updateData;

}
