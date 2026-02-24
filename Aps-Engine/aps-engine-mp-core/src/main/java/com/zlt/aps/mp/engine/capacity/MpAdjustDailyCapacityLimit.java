package com.zlt.aps.mp.engine.capacity;

import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanFinalAdjustVo;

/**
 * 周程滚动日产能限制
 * @author Sandy
 * @date 2025/12/24
 */
public class MpAdjustDailyCapacityLimit extends AbstractDailyCapacityLimit
{
    @Override
    public Integer getDayVulcanizationQty(BaseEntity mpFinalVo) {
        FactoryMonthPlanFinalAdjustVo finalVo = (FactoryMonthPlanFinalAdjustVo)mpFinalVo;
        // 日硫化量 = 单模硫化量 * 2；
        return finalVo.getDayVulcanizationQty() * 2;
    }

    @Override
    public String getEmbryoCodeField() {
        return super.getEmbryoCodeField();
    }
}
