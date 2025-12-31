package com.zlt.aps.factory.capacity;

import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 月计划日产能限制
 * @author Sandy
 * @date 2025/12/24
 */
public class MpMonthPlanDailyCapacityLimit extends AbstractDailyCapacityLimit
{
    @Override
    public Integer getDayVulcanizationQty(BaseEntity mpFinalVo) {
        /*FactoryMonthPlanFinalAdjustVo finalVo = (FactoryMonthPlanFinalAdjustVo)mpFinalVo;
        // 日硫化量 = 单模硫化量 * 2；
        return finalVo.getDayVulcanizationQty() * 2;*/
        return 0;
    }

    @Override
    public String getEmbryoCodeField() {
        return super.getEmbryoCodeField();
    }
}
