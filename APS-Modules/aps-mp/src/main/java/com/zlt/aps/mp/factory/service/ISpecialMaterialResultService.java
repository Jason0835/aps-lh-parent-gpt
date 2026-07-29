package com.zlt.aps.mp.factory.service;

import java.util.List;

import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;

/**
 * 特殊材料排产结果服务接口
 * @author zlt
 *
 */
public interface ISpecialMaterialResultService {
    /**
     * 构建特殊材料排产结果
     * @param planList
     */
    void buildSecialMaterialResult(List<FactoryMonthPlanMouldDayResult> planList);
}
