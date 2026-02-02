package com.zlt.aps.factory.check;

import com.tlt.aps.constant.FactoryConstant;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.common.utils.PubUtil;

import java.util.List;

/**
 * 日总产能限制检查
 * @author Sandy
 * @date 2026-01-29
 */
public class DayTotalCapacityChecker implements IProductionCheck {

    /**
     * 总计划量
     */
    private Integer totalPlanQty;

    /**
     * 总产能限制
     */
    private Integer dayTotalCapacityLimit;

    /**
     * 检查天
     */
    private Integer checkDay;

    /**
     * 定稿记录列表
     */
    private List<FactoryMonthPlanFinalAdjustVo> mpPlanFinalAdjustList;


    public DayTotalCapacityChecker(List<FactoryMonthPlanFinalAdjustVo> mpPlanFinalAdjustList, Integer dayTotalCapacityLimit,Integer checkDay){
        this.mpPlanFinalAdjustList = mpPlanFinalAdjustList;
        this.dayTotalCapacityLimit = dayTotalCapacityLimit;
        this.checkDay = checkDay;
    }

    @Override
    public boolean doCheck() {
        if (PubUtil.isEmpty(mpPlanFinalAdjustList)){
            return true;
        }
        //1.计算检查日的汇总值
        String dayField = FactoryConstant.DAY_FIELD + checkDay;
        this.totalPlanQty = mpPlanFinalAdjustList.stream().mapToInt(x-> {
            return x.getFieldValueByFieldName(dayField) == null ? 0: (Integer)x.getFieldValueByFieldName(dayField);
        }).sum();


        //2.检查日的汇总值 小于等于 日总产能限制
        return totalPlanQty <= dayTotalCapacityLimit;
    }

    public Integer getTotalPlanQty(){
        return this.totalPlanQty;
    }

}
