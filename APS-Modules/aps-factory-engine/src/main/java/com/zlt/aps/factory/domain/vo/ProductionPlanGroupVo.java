package com.zlt.aps.factory.domain.vo;

import com.tlt.aps.enums.ProductionFirstSortOptionsEnum;
import lombok.Data;

import java.util.List;

/**
 * 计划分组信息对象
 *
 * @author ZLT
 * @date 20250219
 */
@Data
public class ProductionPlanGroupVo {
    /**
     * 分组信息
     */
    private ProductionFirstSortOptionsEnum group;
    /**
     * 分组计划信息
     */
    List<MonthPlanManufacturingRequirementVo> groupPlanList;
}
