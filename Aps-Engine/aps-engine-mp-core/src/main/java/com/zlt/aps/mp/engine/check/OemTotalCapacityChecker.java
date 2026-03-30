package com.zlt.aps.mp.engine.check;

import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.enums.YesOrNoEnum;
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
        //1.计算检查汇总值
        String dayField;
        this.totalPlanQty = 0;
        List<FactoryMonthPlanFinalAdjustVo> safeList = new ArrayList<>(mpPlanFinalAdjustList);
        for (FactoryMonthPlanFinalAdjustVo prodFinal:safeList){
            if (YesOrNoEnum.YES.getCode().equals(prodFinal.getOemFlag())){
                //若 OEM标识 = Y
                for (int i = FactoryConstant.MONTH_START_DAY; i <= FactoryConstant.MONTH_MAX_DAY; i++){
                    dayField = FactoryConstant.DAY_FIELD + i;
                    if (prodFinal.getFieldValueByFieldName(dayField) == null){
                        continue;
                    }
                    this.totalPlanQty += (Integer) prodFinal.getFieldValueByFieldName(dayField);
                }
            }
        }

        //2.检查贴牌汇总值 小于等于 贴牌总产能限制
        return totalPlanQty <= totalCapacityLimit;
    }

    public Integer getTotalPlanQty(){
        return this.totalPlanQty;
    }

}
