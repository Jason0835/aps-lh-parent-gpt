package com.zlt.aps.factory.handler;

import com.tlt.aps.constant.StringConstant;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.*;
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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 成型硫化模具排产计算器
 *
 * @author ZLT
 * @date 20251221
 */
@Slf4j
public class CxLhMouldProductionCalculator {

    /**
     * 续作Sku使用续作模具进行排产
     * 续作Sku使用挑选的双模在productionDay日排产productionQty量
     * 需要进行模具分摊和计划分配
     *
     * @param context         排产上下文
     * @param groupPlanInfo   分组排产计划
     * @param continueSkuInfo 续作Sku信息
     * @param productionDay   排产日
     * @param productionQty   日排产量
     * @param doubleMouldList 模具信息
     */
    public static void continueSkuLhProductionHandler(Context context, ProductionPlanGroupInfo groupPlanInfo, CxContinueSkuInfoHelper continueSkuInfo, Integer productionDay, Integer productionQty, List<ProductionMouldInfoVo> doubleMouldList) {
        Set<Integer> stopDay = context.getStopDays();
        if (stopDay.contains(productionDay)) {
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //日硫化量，理论上productionQty不超过dayMaxProductionQty
        Integer dayMaxProductionQty = continueSkuInfo.getMaxDaySingleLhMachineQty();
        boolean isDayFinish = productionQty >= dayMaxProductionQty ? true : false;
        Set<String> cxMachineCodeInfo = continueSkuInfo.getOnLineCxMachineSet();
        List<MonthPlanProductionRequirePlanVo> continueSkuPlanList = continueSkuInfo.getContinueSkuPlanList();
        //更新日产信息
        UpdateDayProductionInfoHelper updateInfo = new UpdateDayProductionInfoHelper(productionDay, productionQty, isDayFinish, cxMachineCodeInfo, BigDecimal.ZERO.intValue());
        updateDayProductionInfo(productionContext, groupPlanInfo, doubleMouldList, continueSkuPlanList, updateInfo);
    }

    /**
     * 续作硫化排产同规格同花纹
     * 同生胎共模具，此时不判断限制
     * -选中一组
     *
     * @param context                   排产上下文
     * @param earliestConclusionLhGroup 收尾的硫化组信息对象
     * @param lhProductionQtyHelper     硫化排产数量对象
     * @param startDay                  开始排产日
     * @param endDay                    收尾日
     * @param doubleMouldList           选中的模具
     * @param skuProductionPlanList     排产的物料计划集合
     */
    public static void lhProductionByLhGroupHandler(Context context, EarliestConclusionLhGroupHelper earliestConclusionLhGroup, LhProductionQtyHelper lhProductionQtyHelper, Integer startDay, Integer endDay, List<ProductionMouldInfoVo> doubleMouldList, List<MonthPlanProductionRequirePlanVo> skuProductionPlanList) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Integer sumProductionQty = lhProductionQtyHelper.getSumProductionQty();
        Integer realSumProductionQty = lhProductionQtyHelper.getRealSumProductionQty();
        Integer dayMaxProductionQty = lhProductionQtyHelper.getDayMaxProductionQty();
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
            Integer realDayProductionQty = Math.min(sumProductionQty, dayMaxProductionQty);
            realSumProductionQty = realSumProductionQty + realDayProductionQty;
            sumProductionQty = sumProductionQty - realDayProductionQty;
            //todo 判断模具是否排产完毕
            boolean isDayFinish = true;
            //更新日产信息
            UpdateDayProductionInfoHelper updateInfo = new UpdateDayProductionInfoHelper(day, realDayProductionQty.intValue(), isDayFinish, lhProductionQtyHelper.getCxMachineInfo(), BigDecimal.ZERO.intValue());
            updateDayProductionInfo(productionContext, productionPlanInfo, doubleMouldList, skuProductionPlanList, updateInfo);

//            //分组-日排产信息
//            SkuDayProductionInfoHelper skuDayProductionInfo = SkuDayProductionInfoHelper.buildEmpty(productionDay, productionPlan, realDayProductionQty.intValue(), usedMouldSet);
//            productionPlanInfo.addDayProductionInfo(skuDayProductionInfo);
//            //模具排产信息-计划分配
//            Map<Long, MonthPlanProductionRequirePlanVo> needDeductionMap = skuProductionPlanList.stream().collect(Collectors.toMap(MonthPlanProductionRequirePlanVo::getMonthPlanId, Function.identity()));
//            Map<Long, Long> productionPlanMap = new ProductionPlanDistributor().allocationProductionQty(realDayProductionQty, skuProductionPlanList);
//            productionPlanMap.forEach((monthPlanId, planProductionQty) -> {
//                MonthPlanProductionRequirePlanVo groupPlan = needDeductionMap.get(monthPlanId);
//                doubleMouldList.forEach(productionMould -> productionMould.addProductionInfo(productionDay, groupPlan, isDayFinish, planProductionQty, lhProductionQtyHelper.getCxMachineInfo()));
//            });
//            //记录已排产量及损耗量
//            productionContext.addSkuProductionAndWastageQty(skuMaterialDesc, realDayProductionQty, BigDecimal.ZERO.longValue());
        }
        //更新还需排产量及实际排产量
        lhProductionQtyHelper.setSumProductionQty(sumProductionQty);
        lhProductionQtyHelper.setRealSumProductionQty(realSumProductionQty);
    }

    /**
     * 采用双模，在startDay~endDay进行排产
     * 此时为在机结构对在产机台进行新增Sku排产的场景
     * 以结构为维度，忽略机台进行排产
     *
     * @param context               排产上下文
     * @param lhProductionQtyHelper 排产基础信息
     * @param startDay              排产开始日
     * @param endDay                排产结束日
     * @param doubleMouldList       排产的双模模具
     * @param skuProductionPlanList sku的排产计划
     */
    public static void lhProductionByGroupHandler(Context context, LhProductionQtyHelper lhProductionQtyHelper, Integer startDay, Integer endDay, List<ProductionMouldInfoVo> doubleMouldList, List<MonthPlanProductionRequirePlanVo> skuProductionPlanList) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Integer sumProductionQty = lhProductionQtyHelper.getSumProductionQty();
        Integer realSumProductionQty = lhProductionQtyHelper.getRealSumProductionQty();
        Integer dayMaxProductionQty = lhProductionQtyHelper.getDayMaxProductionQty();
        ProductionPlanGroupInfo productionPlanInfo = lhProductionQtyHelper.getProductionPlanInfo();
        //不关注具体Id，只为拿到生胎等信息
        MonthPlanProductionRequirePlanVo productionPlan = skuProductionPlanList.get(BigDecimal.ZERO.intValue());
        Set<Integer> stopDay = context.getStopDays();
        //得到真正上机日
        Integer realStartDay = productionPlanInfo.getRealOnlineMachineDay(productionPlan, startDay, endDay);
        if (null == realStartDay) {
            //本轮不再参与排产
            skuProductionPlanList.forEach(singlePlan -> singlePlan.setIsThisRound(YesOrNoEnum.NO.getValue()));
            return;
        }
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
            Integer realDayProductionQty = Math.min(sumProductionQty, dayMaxProductionQty);
            realSumProductionQty = realSumProductionQty + realDayProductionQty;
            sumProductionQty = sumProductionQty - realDayProductionQty;
            //todo 判断模具是否排产完毕
            boolean isDayFinish = true;
            //更新日产信息
            UpdateDayProductionInfoHelper updateInfo = new UpdateDayProductionInfoHelper(day, realDayProductionQty.intValue(), isDayFinish, lhProductionQtyHelper.getCxMachineInfo(), BigDecimal.ZERO.intValue());
            updateDayProductionInfo(productionContext, productionPlanInfo, doubleMouldList, skuProductionPlanList, updateInfo);
        }
        //更新还需排产量及实际排产量
        lhProductionQtyHelper.setSumProductionQty(sumProductionQty);
        lhProductionQtyHelper.setRealSumProductionQty(realSumProductionQty);
    }

    /**
     * 采用双模，在startDay~endDay进行排产
     * 在新增结构(结构本身是新增或是在机结构增机台场景)
     * 此时是固定到一台
     *
     * @param context               排产上下文
     * @param lhProductionQtyHelper 排产基础信息
     * @param startDay              排产开始日
     * @param endDay                排产结束日
     * @param doubleMouldList       排产的双模模具
     * @param skuProductionPlanList sku的排产计划
     */
    public static void lhProductionByCxMachineHandler(Context context, LhProductionQtyHelper lhProductionQtyHelper, Integer startDay, Integer endDay, List<ProductionMouldInfoVo> doubleMouldList, List<MonthPlanProductionRequirePlanVo> skuProductionPlanList) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Integer sumProductionQty = lhProductionQtyHelper.getSumProductionQty();
        Integer realSumProductionQty = lhProductionQtyHelper.getRealSumProductionQty();
        Integer dayMaxProductionQty = lhProductionQtyHelper.getDayMaxProductionQty();
        CxLhProductionHelper cxLhGroup = lhProductionQtyHelper.getCxLhGroup();
        Set<String> cxMachineInfoSet = lhProductionQtyHelper.getCxMachineInfo();
        String cxMachineCode = new ArrayList<>(cxMachineInfoSet).get(BigDecimal.ZERO.intValue());
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo().get(cxMachineCode);
        String skuMaterialDesc = skuProductionPlanList.get(BigDecimal.ZERO.intValue()).getMaterialDesc();
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
            Integer realDayProductionQty = Math.min(sumProductionQty, dayMaxProductionQty);
            realSumProductionQty = realSumProductionQty + realDayProductionQty;
            sumProductionQty = sumProductionQty - realDayProductionQty;
            //todo 判断模具是否排产完毕
            boolean isDayFinish = true;
            //更新模具日产信息
            UpdateDayProductionInfoHelper updateInfo = new UpdateDayProductionInfoHelper(day, realDayProductionQty.intValue(), isDayFinish, cxMachineInfoSet, BigDecimal.ZERO.intValue());
            updateMouldDayProductionInfo(doubleMouldList, skuProductionPlanList, updateInfo);
            //更新硫化组日期和日排产量
            cxLhGroup.setProductionQty(realDayProductionQty);
            cxLhGroup.setProductionDay(day);
            cxLhGroup.setDayMaxProductionQty(dayMaxProductionQty);
            cxMachineInfo.getCxLhRatioMap().put(cxLhGroup.getLhGroupNo(), cxLhGroup);
            //记录已排产量及损耗量
            productionContext.addSkuProductionAndWastageQty(skuMaterialDesc, realDayProductionQty, BigDecimal.ZERO.intValue());
        }
        //更新还需排产量及实际排产量
        lhProductionQtyHelper.setSumProductionQty(sumProductionQty);
        lhProductionQtyHelper.setRealSumProductionQty(realSumProductionQty);
    }

    @Deprecated
    public static void lhProductionByLhGroupHandler(Context context, LhProductionQtyHelper lhProductionQtyHelper, Integer startDay, Integer endDay, List<ProductionMouldInfoVo> doubleMouldList, List<MonthPlanProductionRequirePlanVo> skuProductionPlanList) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Integer sumProductionQty = lhProductionQtyHelper.getSumProductionQty();
        Integer realSumProductionQty = lhProductionQtyHelper.getRealSumProductionQty();
        Integer dayMaxProductionQty = lhProductionQtyHelper.getDayMaxProductionQty();
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
            Integer realDayProductionQty = Math.min(sumProductionQty, dayMaxProductionQty);
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
            productionContext.addSkuProductionAndWastageQty(skuMaterialDesc, realDayProductionQty, BigDecimal.ZERO.intValue());
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
        Integer dayMaxProductionQty = continueSkuInfo.getDayVulcanizationQty() * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        boolean isDayFinish = productionQty >= dayMaxProductionQty ? true : false;
        String cxMachineCode = String.join(StringConstant.COMMA, continueSkuInfo.getOnLineCxMachineSet());
        doubleMouldList.forEach(productionMould -> productionMould.addProductionInfo(productionDay, groupPlanInfo, lhGroupInfo, isDayFinish, productionQty, dayMaxProductionQty, cxMachineCode, continueSkuInfo.getContinueSkuPlanList()));
        //更新硫化组日期和日排产量
        lhGroupInfo.setProductionQty(productionQty);
        lhGroupInfo.setProductionDay(productionDay);
        lhGroupInfo.setDayMaxProductionQty(dayMaxProductionQty);
    }

    /**
     * 更新日排产信息
     * 1、更新模具的日排产信息
     * 2、更新分组计划的日排产信息
     * 3、更新计划的排产量信息
     *
     * @param productionContext     排产上下文
     * @param groupPlanInfo         分组排产计划对象
     * @param doubleMouldList       选中的排产模具
     * @param skuProductionPlanList 排产的Sku计划集合
     * @param updateInfo            日更新信息对象
     */
    private static void updateDayProductionInfo(TbrProductionContext productionContext, ProductionPlanGroupInfo groupPlanInfo, List<ProductionMouldInfoVo> doubleMouldList, List<MonthPlanProductionRequirePlanVo> skuProductionPlanList, UpdateDayProductionInfoHelper updateInfo) {
        Integer productionDay = updateInfo.getProductionDay();
        Integer realDayProductionQty = updateInfo.getRealDayProductionQty();
        Integer lossQty = updateInfo.getLossQty();
        Set<String> usedMouldSet = doubleMouldList.stream().map(ProductionMouldInfoVo::getMouldCode).collect(Collectors.toSet());
        MonthPlanProductionRequirePlanVo productionPlan = skuProductionPlanList.get(BigDecimal.ZERO.intValue());
        String skuMaterialDesc = productionPlan.getMaterialDesc();
        //分组-日排产信息
        SkuDayProductionInfoHelper skuDayProductionInfo = SkuDayProductionInfoHelper.buildEmpty(productionDay, productionPlan, realDayProductionQty, usedMouldSet);
        groupPlanInfo.addDayProductionInfo(skuDayProductionInfo);
        //模具排产信息-计划分配
        updateMouldDayProductionInfo(doubleMouldList, skuProductionPlanList, updateInfo);
        //记录已排产量及损耗量
        productionContext.addSkuProductionAndWastageQty(skuMaterialDesc, realDayProductionQty, lossQty);
    }

    /**
     * 更新模具的日排产信息，并更新计划的待排产量
     * 更新使用模具的日排产信息
     * 需要对排产量按计划集合的优先级进行分配到具体的计划Id
     *
     * @param doubleMouldList       选中的排产模具
     * @param skuProductionPlanList 排产的Sku计划集合
     * @param updateInfo            日更新信息对象
     */
    private static void updateMouldDayProductionInfo(List<ProductionMouldInfoVo> doubleMouldList, List<MonthPlanProductionRequirePlanVo> skuProductionPlanList, UpdateDayProductionInfoHelper updateInfo) {
        Integer productionDay = updateInfo.getProductionDay();
        Integer realDayProductionQty = updateInfo.getRealDayProductionQty();
        boolean isDayFinish = updateInfo.isDayFinish();
        Set<String> cxMachineInfo = updateInfo.getUsedCxMachineInfo();
        //模具排产信息-计划分配
        Map<Long, MonthPlanProductionRequirePlanVo> needDeductionMap = skuProductionPlanList.stream().collect(Collectors.toMap(MonthPlanProductionRequirePlanVo::getMonthPlanId, Function.identity()));
        Map<Long, Integer> productionPlanMap = new ProductionPlanDistributor().allocationProductionQty(realDayProductionQty, skuProductionPlanList);
        productionPlanMap.forEach((monthPlanId, planProductionQty) -> {
            MonthPlanProductionRequirePlanVo groupPlan = needDeductionMap.get(monthPlanId);
            doubleMouldList.forEach(productionMould -> productionMould.addProductionInfo(productionDay, groupPlan, isDayFinish, planProductionQty, cxMachineInfo));
        });
    }

    private CxLhMouldProductionCalculator() {

    }
}
