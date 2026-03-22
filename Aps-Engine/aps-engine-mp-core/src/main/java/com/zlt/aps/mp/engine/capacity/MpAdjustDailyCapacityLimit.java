package com.zlt.aps.mp.engine.capacity;

import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.api.domain.vo.FactoryMonthPlanFinalAdjustVo;

/**
 * 周程滚动日产能限制
 * @author Sandy
 * @date 2025/12/24
 */
public class MpAdjustDailyCapacityLimit extends AbstractDailyCapacityLimit
{
    @Override
    public Integer getDayVulcanizationQty(BaseEntity mpFinalVo) {
        if (mpFinalVo instanceof FactoryMonthPlanFinalAdjustVo) {
            FactoryMonthPlanFinalAdjustVo finalVo = (FactoryMonthPlanFinalAdjustVo) mpFinalVo;
            // 日硫化量 = 单模硫化量 * 2；
            return finalVo.getDayVulcanizationQty() * 2;
        } else if (mpFinalVo instanceof FactoryMonthPlanMouldDayResult) {
            FactoryMonthPlanMouldDayResult finalVo = (FactoryMonthPlanMouldDayResult) mpFinalVo;
            // 日硫化量本身就是双模产能
            return finalVo.getDayVulcanizationQty();
        }
        return 0;
    }

    @Override
    public String getEmbryoCodeField() {
        return super.getEmbryoCodeField();
    }
}
