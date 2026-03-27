package com.zlt.aps.mp.engine.check;

import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.mp.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.common.utils.PubUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * OEM总产能限制检查
 * @author Sandy
 * @date 2026-01-29
 */
public class OemTotalCapacityChecker implements IProductionCheck {

    /**
     * OEM总计划量
     */
    private Integer totalPlanQty;

    /**
     * OEM总产能限制(剩余)
     */
    private Integer totalCapacityLimit;


    /**
     * 当前结构 定稿记录列表
     */
    private List<FactoryMonthPlanFinalAdjustVo> mpPlanFinalAdjustList;


    public OemTotalCapacityChecker(List<FactoryMonthPlanFinalAdjustVo> mpPlanFinalAdjustList, Integer totalCapacityLimit){
        this.mpPlanFinalAdjustList = mpPlanFinalAdjustList;
        this.totalCapacityLimit = totalCapacityLimit;
    }

    @Override
    public boolean doCheck() {
        if (PubUtil.isEmpty(mpPlanFinalAdjustList)){
            return true;
        }
        if (totalCapacityLimit == 0){
            return false;
        }
        //1.计算检查日的汇总值
        String dayField = FactoryConstant.DAY_FIELD + 1;
        List<FactoryMonthPlanFinalAdjustVo> safeList = new ArrayList<>(mpPlanFinalAdjustList);
        for (FactoryMonthPlanFinalAdjustVo prodFinal:safeList){
            //prodFinal
        }
        this.totalPlanQty = safeList.stream()
                .filter(Objects::nonNull)
                .mapToInt(x -> {
                    Object val = x.getFieldValueByFieldName(dayField);
                    return val instanceof Number ? ((Number) val).intValue() : 0;
                }).sum();

        //2.检查贴牌汇总值 小于等于 贴牌总产能限制
        return totalPlanQty <= totalCapacityLimit;
    }

    public Integer getTotalPlanQty(){
        return this.totalPlanQty;
    }

}
