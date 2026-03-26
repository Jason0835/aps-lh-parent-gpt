package com.zlt.aps.mp.engine.check;

import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.mp.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.common.utils.PubUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 日硫化机台数限制检查
 * @author Sandy
 * @date 2026-01-29
 */
public class DayTotalLhMachinesChecker implements IProductionCheck {

    /**
     * 合计硫化机台数
     */
    private Integer totalLhMachinesQty;

    /**
     * 总硫化机台数限制
     */
    private Integer dayTotalLhMachinesLimit;

    /**
     * 检查天
     */
    private Integer checkDay;

    /**
     * 定稿记录列表
     */
    private List<FactoryMonthPlanFinalAdjustVo> mpPlanFinalAdjustList;


    public DayTotalLhMachinesChecker(List<FactoryMonthPlanFinalAdjustVo> mpPlanFinalAdjustList, Integer dayTotalLhMachinesLimit, Integer checkDay){
        this.mpPlanFinalAdjustList = mpPlanFinalAdjustList;
        this.dayTotalLhMachinesLimit = dayTotalLhMachinesLimit;
        this.checkDay = checkDay;
    }

    @Override
    public boolean doCheck() {
        if (PubUtil.isEmpty(mpPlanFinalAdjustList)){
            return true;
        }
        if (dayTotalLhMachinesLimit == 0){
            return false;
        }
        //1.计算检查日的汇总值
        String dayField = FactoryConstant.DAY_FIELD + checkDay;
        List<FactoryMonthPlanFinalAdjustVo> safeList = new ArrayList<>(mpPlanFinalAdjustList);
        this.totalLhMachinesQty = safeList.stream()
                .filter(Objects::nonNull)
                .mapToInt(x -> {
                    Object val = x.getFieldValueByFieldName(dayField);
                    return val instanceof Number ? ((Number) val).intValue() : 0;
                }).sum();

        //2.检查日的汇总值 小于等于 日总产能限制
        return totalLhMachinesQty <= dayTotalLhMachinesLimit;
    }

    public Integer getTotalLhMachinesQty(){
        return this.totalLhMachinesQty;
    }

}
