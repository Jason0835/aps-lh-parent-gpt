package com.zlt.aps.factory.utils;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.CxLhProductionHelper;
import com.zlt.aps.factory.domain.dto.LhProductionQtyHelper;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

/**
 * 成型硫化模具排产工具类
 *
 * @author ZLT
 * @date 20251221
 */
@Slf4j
public class CxLhMouldProductionUtils {

    /**
     * 采用双模，在startDay~endDay进行排产
     *
     * @param context               排产上下文
     * @param lhProductionQtyHelper 排产基础信息
     * @param startDay              排产开始日
     * @param endDay                排产结束日
     * @param doubleMouldList       排产的双模模具
     * @param skuProductionPlanList sku的排产计划
     */
    public static void lhProductionHandler(Context context, LhProductionQtyHelper lhProductionQtyHelper, Integer startDay, Integer endDay, List<ProductionMouldInfoVo> doubleMouldList, List<MonthPlanProductionRequirePlanVo> skuProductionPlanList) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Long sumProductionQty = lhProductionQtyHelper.getSumProductionQty();
        Long realSumProductionQty = lhProductionQtyHelper.getRealSumProductionQty();
        Long dayMaxProductionQty = lhProductionQtyHelper.getDayMaxProductionQty();
        CxLhProductionHelper cxLhGroup = lhProductionQtyHelper.getCxLhGroup();
        CxMachineBaseInfoVo cxMachineInfo = lhProductionQtyHelper.getCxMachineInfo();
        String cxMachineCode = cxMachineInfo.getCxMachineCode();
        String skuMaterialDesc = skuProductionPlanList.get(BigDecimal.ZERO.intValue()).getMaterialDesc();
        //进行排产
        for (int day = startDay; day <= endDay; day++) {
            if (sumProductionQty <= BigDecimal.ZERO.longValue()) {
                break;
            }
            //停工日跳过
            if (context.getStopDays().contains(day)) {
                continue;
            }
            //todo 需要考虑首日：换活字块，换模场景，此时双模日硫化量会有变化
            Long realDayProductionQty = Math.min(sumProductionQty, dayMaxProductionQty);
            realSumProductionQty = realSumProductionQty + realDayProductionQty;
            sumProductionQty = sumProductionQty - realDayProductionQty;
            //todo 判断模具是否排产完毕
            boolean isDayFinish = true;
            Integer productionDay = day;
            doubleMouldList.forEach(productionMould -> productionMould.addProductionInfo(productionDay, cxLhGroup, isDayFinish, realDayProductionQty, dayMaxProductionQty, cxMachineCode, skuProductionPlanList));
            //更新硫化组日期和日排产量
            cxLhGroup.setProductionQty(realDayProductionQty);
            cxLhGroup.setProductionDay(day);
            cxLhGroup.setDayMaxProductionQty(dayMaxProductionQty);
            cxMachineInfo.getCxLhRatioMap().put(cxLhGroup.getLhGroupNo(), cxLhGroup);
            //记录已排产量及损耗量
            productionContext.addSkuProductionAndWastageQty(skuMaterialDesc, realDayProductionQty, BigDecimal.ZERO.longValue());
        }
        //更新还需排产量及实际排产量
        lhProductionQtyHelper.setSumProductionQty(sumProductionQty);
        lhProductionQtyHelper.setRealSumProductionQty(realSumProductionQty);
    }

    private CxLhMouldProductionUtils(){

    }
}
