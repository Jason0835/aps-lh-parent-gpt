package com.zlt.aps.factory.handler;

import com.tlt.aps.constant.StringConstant;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.CxContinueSkuInfoHelper;
import com.zlt.aps.factory.domain.dto.CxLhProductionHelper;
import com.zlt.aps.factory.domain.dto.LhProductionQtyHelper;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 成型硫化模具排产计算器
 *
 * @author ZLT
 * @date 20251221
 */
@Slf4j
public class CxLhMouldProductionCalculator {

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
        ProductionPlanGroupInfo productionPlanInfo = lhProductionQtyHelper.getProductionPlanInfo();
        Set<Integer> stopDay = context.getStopDays();
        //进行排产
        for (int day = startDay; day <= endDay; day++) {
            if (sumProductionQty <= BigDecimal.ZERO.longValue()) {
                break;
            }
            //停工日跳过
            if (stopDay.contains(day)) {
                continue;
            }
            //todo 需要考虑首日：换活字块，换模场景，此时双模日硫化量会有变化
            Long realDayProductionQty = Math.min(sumProductionQty, dayMaxProductionQty);
            realSumProductionQty = realSumProductionQty + realDayProductionQty;
            sumProductionQty = sumProductionQty - realDayProductionQty;
            //todo 判断模具是否排产完毕
            boolean isDayFinish = true;
            Integer productionDay = day;
            doubleMouldList.forEach(productionMould -> productionMould.addProductionInfo(productionDay, productionPlanInfo, cxLhGroup, isDayFinish, realDayProductionQty, dayMaxProductionQty, cxMachineCode, skuProductionPlanList));
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

    public static void lhProductionByLhGroupHandler(Context context, LhProductionQtyHelper lhProductionQtyHelper, Integer startDay, Integer endDay, List<ProductionMouldInfoVo> doubleMouldList, List<MonthPlanProductionRequirePlanVo> skuProductionPlanList) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Long sumProductionQty = lhProductionQtyHelper.getSumProductionQty();
        Long realSumProductionQty = lhProductionQtyHelper.getRealSumProductionQty();
        Long dayMaxProductionQty = lhProductionQtyHelper.getDayMaxProductionQty();
        CxLhProductionHelper cxLhGroup = lhProductionQtyHelper.getCxLhGroup();
        String cxMachineCode = String.join(StringConstant.COMMA, cxLhGroup.getCxMachineInfo());
        String skuMaterialDesc = skuProductionPlanList.get(BigDecimal.ZERO.intValue()).getMaterialDesc();
        ProductionPlanGroupInfo productionPlanInfo = lhProductionQtyHelper.getProductionPlanInfo();
        Set<Integer> stopDay = context.getStopDays();
        //进行排产
        for (int day = startDay; day <= endDay; day++) {
            if (sumProductionQty <= BigDecimal.ZERO.longValue()) {
                break;
            }
            //停工日跳过
            if (stopDay.contains(day)) {
                continue;
            }
            //todo 需要考虑首日：换活字块，换模场景，此时双模日硫化量会有变化
            Long realDayProductionQty = Math.min(sumProductionQty, dayMaxProductionQty);
            realSumProductionQty = realSumProductionQty + realDayProductionQty;
            sumProductionQty = sumProductionQty - realDayProductionQty;
            //todo 判断模具是否排产完毕
            boolean isDayFinish = true;
            Integer productionDay = day;
            doubleMouldList.forEach(productionMould -> productionMould.addProductionInfo(productionDay, productionPlanInfo, cxLhGroup, isDayFinish, realDayProductionQty, dayMaxProductionQty, cxMachineCode, skuProductionPlanList));
            //更新硫化组日期和日排产量
            cxLhGroup.setProductionQty(realDayProductionQty);
            cxLhGroup.setProductionDay(day);
            cxLhGroup.setDayMaxProductionQty(dayMaxProductionQty);
            //记录已排产量及损耗量
            productionContext.addSkuProductionAndWastageQty(skuMaterialDesc, realDayProductionQty, BigDecimal.ZERO.longValue());
        }
        //更新还需排产量及实际排产量
        lhProductionQtyHelper.setSumProductionQty(sumProductionQty);
        lhProductionQtyHelper.setRealSumProductionQty(realSumProductionQty);
    }

    /**
     * 续作Sku使用续作模具进行排产
     *
     * @param context         排产上下文
     * @param groupPlanInfo   分组排产计划
     * @param continueSkuInfo 续作Sku信息
     * @param productionDay   排产日
     * @param productionQty   日排产量
     * @param lhGroupInfo     硫化组信息
     */
    public static void continueSkuLhProductionHandler(Context context, ProductionPlanGroupInfo groupPlanInfo, CxContinueSkuInfoHelper continueSkuInfo, Integer productionDay, Integer productionQty, CxLhProductionHelper lhGroupInfo) {
        Set<Integer> stopDay = context.getStopDays();
        if (stopDay.contains(productionDay)) {
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        List<ProductionMouldInfoVo> doubleMouldList = new ArrayList<>();
        Set<String> productionMouldSet = lhGroupInfo.getProductionMouldSet();
        Map<String, ProductionMouldInfoVo> mouldInfoMap = productionContext.getBaseDataContainer().getMouldInfoMap();
        productionMouldSet.forEach(mouldCode -> doubleMouldList.add(mouldInfoMap.get(mouldCode)));
        Long dayMaxProductionQty = continueSkuInfo.getDayVulcanizationQty() * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        boolean isDayFinish = productionQty >= dayMaxProductionQty ? true : false;
        String cxMachineCode = String.join(StringConstant.COMMA, continueSkuInfo.getOnLineCxMachineSet());
        doubleMouldList.forEach(productionMould -> productionMould.addProductionInfo(productionDay, groupPlanInfo, lhGroupInfo, isDayFinish, Long.valueOf(productionQty), dayMaxProductionQty, cxMachineCode, continueSkuInfo.getContinueSkuPlanList()));
        //更新硫化组日期和日排产量
        lhGroupInfo.setProductionQty(Long.valueOf(productionQty));
        lhGroupInfo.setProductionDay(productionDay);
        lhGroupInfo.setDayMaxProductionQty(dayMaxProductionQty);
    }

    private CxLhMouldProductionCalculator() {

    }
}
