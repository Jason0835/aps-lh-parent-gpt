package com.zlt.aps.monthplan.api.domain.vo;

import com.zlt.aps.monthplan.api.domain.entity.PlanOrderSortConfiguration;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;


/**
 * 对冲顺序配置VO类
 *
 * @author hsc
 * @since 2025/2/21
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PlanOrderSortConfigurationVo {


    /**
     * 库存第一对冲顺序配置
     */
    private List<PlanOrderSortConfiguration> firstStockHedgingSortConfigurations;

    /**
     * 库存第二对冲顺序配置
     */
    private List<PlanOrderSortConfiguration> secondStockHedgingSortConfigurations;

    /**
     * 月份第一排产顺序配置集合
     */
    private List<PlanOrderSortConfiguration> firstPlanOrderSortConfigurations;

    /**
     * 月份第二排产顺序配置集合
     */
    private List<PlanOrderSortConfiguration> secondPlanOrderSortConfigurations;

    /**
     * 月份第三排产顺序配置集合
     */
    private List<PlanOrderSortConfiguration> thirdPlanOrderSortConfigurations;
}
