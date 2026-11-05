package com.zlt.aps.common.engine.domain;

import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 硫化余量计算：Sku硫化月计划分解到具体日期的计划信息
 *
 * @author ZLT
 * @date 20261103
 */
@Data
public class SkuLhMonthPlanInfoHelper implements Serializable {
    /**
     * 计算的Sku对象
     */
    private FactoryMonthPlanProductionFinalResult skuInfo;

    /**
     * 所有排产日计划，包含没有排产量
     */
    private List<SkuLhDayMonthPlanQtyVo> allProductionDayList;
}
