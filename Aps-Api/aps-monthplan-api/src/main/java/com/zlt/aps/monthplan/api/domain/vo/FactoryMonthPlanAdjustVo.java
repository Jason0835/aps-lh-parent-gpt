package com.zlt.aps.monthplan.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 分厂月份计划调整参数对象
 *
 * @author ZLT
 * @date 20250320
 */
@Data
@ApiModel(value = "分厂月份计划调整参数对象", description = "分厂月份计划调整参数对象")
public class FactoryMonthPlanAdjustVo implements Serializable {

    /**
     * 调整的定稿版本信息
     */
    private FactoryMonthPlanFinalVersionInfoVo finalVersion;

    /**
     * 调整的计划信息-包含新增规格，调增规格，调减规格
     */
    @ApiModelProperty(value = "调整的计划信息-包含新增规格，调增规格，调减规格", name = "adjustPlan")
    private FactoryMonthPlanProdFinalVo adjustPlan;

    /**
     * 调整控制信息
     */
    private MonthPlanAdjustInfoVo controlInfo;
}
