package com.zlt.aps.factory.handler;

import com.tlt.aps.constant.StringConstant;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.daylimit.*;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.*;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import com.zlt.aps.factory.scheduling.cxcapacity.ProductionCapacityParamConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
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
        }
        //更新还需排产量及实际排产量
        lhProductionQtyHelper.setSumProductionQty(sumProductionQty);
        lhProductionQtyHelper.setRealSumProductionQty(realSumProductionQty);
    }

//    /**
//     * 采用双模，在startDay~endDay进行排产
//     * 此时为在机结构对在产机台进行新增Sku排产的场景
//     * 以结构为维度，忽略机台进行排产
//     *
//     * @param context               排产上下文
//     * @param lhProductionQtyHelper 排产基础信息
//     * @param startDay              排产开始日
//     * @param endDay                排产结束日
//     * @param doubleMouldList       排产的双模模具
//     * @param skuProductionPlanList sku的排产计划
//     */
//    public static void lhProductionByGroupHandler(Context context, LhProductionQtyHelper lhProductionQtyHelper, Integer startDay, Integer endDay, List<ProductionMouldInfoVo> doubleMouldList, List<MonthPlanProductionRequirePlanVo> skuProductionPlanList) {
//        TbrProductionContext productionContext = (TbrProductionContext) context;
//        Integer sumProductionQty = lhProductionQtyHelper.getSumProductionQty();
//        Integer realSumProductionQty = lhProductionQtyHelper.getRealSumProductionQty();
//        Integer dayMaxProductionQty = lhProductionQtyHelper.getDayMaxProductionQty();
//        ProductionPlanGroupInfo productionPlanInfo = lhProductionQtyHelper.getProductionPlanInfo();
//        //不关注具体Id，只为拿到生胎等信息
//        MonthPlanProductionRequirePlanVo productionPlan = skuProductionPlanList.get(BigDecimal.ZERO.intValue());
//        Set<Integer> stopDay = context.getStopDays();
//        //得到真正上机日
//        Integer realStartDay = productionPlanInfo.getRealOnlineMachineDay(productionPlan, startDay, endDay);
//        if (null == realStartDay) {
//            //本轮不再参与排产
//            skuProductionPlanList.forEach(singlePlan -> singlePlan.setIsThisRound(YesOrNoEnum.NO.getValue()));
//            return;
//        }
//        ProductionCapacityParamConfiguration paramConfiguration = productionContext.getBaseDataContainer().getParamConfiguration();
//        //进行排产
//        Integer firstDay = null;
//        for (int day = startDay; day <= endDay; day++) {
//            if (sumProductionQty <= BigDecimal.ZERO.longValue()) {
//                break;
//            }
//            //停工日跳过
//            if (stopDay.contains(day)) {
//                continue;
//            }
//            if (null == firstDay) {
//                firstDay = day;
//            }
//            //todo 需要考虑首日：换活字块，换模场景，此时双模日硫化量会有变化
//            Integer realDayProductionQty = Math.min(sumProductionQty, dayMaxProductionQty);
//            Integer theoryProductionQty = realDayProductionQty;
//            //首日换模排产
//            if (firstDay.equals(day)) {
//                realDayProductionQty = Math.min(realDayProductionQty, paramConfiguration.getChangeMouldFirstQty());
//            }
//            Integer lossQty = theoryProductionQty - realDayProductionQty;
//            if (lossQty < BigDecimal.ZERO.intValue()) {
//                lossQty = BigDecimal.ZERO.intValue();
//            }
//            realSumProductionQty = realSumProductionQty + realDayProductionQty;
//            sumProductionQty = sumProductionQty - realDayProductionQty;
//            //判断模具是否排产完毕，首日排产则排产完毕，否则看排产量
//            boolean isDayFinish = firstDay.equals(day) ? true : realDayProductionQty.equals(dayMaxProductionQty);
//            //更新日产信息
//            UpdateDayProductionInfoHelper updateInfo = new UpdateDayProductionInfoHelper(day, realDayProductionQty.intValue(), isDayFinish, lhProductionQtyHelper.getCxMachineInfo(), lossQty);
//            updateDayProductionInfo(productionContext, productionPlanInfo, doubleMouldList, skuProductionPlanList, updateInfo);
//        }
//        //更新还需排产量及实际排产量
//        lhProductionQtyHelper.setSumProductionQty(sumProductionQty);
//        lhProductionQtyHelper.setRealSumProductionQty(realSumProductionQty);
//    }

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
        String skuMaterialDesc = productionPlan.getMaterialDesc();
        //进行排产
        Integer firstDay = null;
        for (int day = startDay; day <= endDay; day++) {
            if (sumProductionQty <= BigDecimal.ZERO.longValue()) {
                break;
            }
            //停工日跳过
            if (stopDay.contains(day)) {
                continue;
            }
            if (null == firstDay) {
                firstDay = day;
            }
            //需要考虑首日：换活字块，换模场景，此时双模日硫化量会有变化
            DayProductionQtyHelper dayProductionInfo = calculateSingleLhGroupQty(context, lhProductionQtyHelper, day, firstDay, startDay, productionPlan);
            Integer lossQty;
            if (dayProductionInfo.isProductionNextDay()) {
                //隔天换模，更新当前排产完毕信息
                doubleMouldList.forEach(productionMould -> productionMould.getFinishDaySet().add(dayProductionInfo.getProductionDay()));
                //记录已排产量及损耗量
                productionContext.addSkuProductionAndWastageQty(skuMaterialDesc, BigDecimal.ZERO.intValue(), dayProductionInfo.getLossQty());
                day = getNextHasProductionDay(day, stopDay);
                if (day > endDay) {
                    break;
                }
                lossQty = dayProductionInfo.getNextDayLossQty();
            } else {
                lossQty = dayProductionInfo.getLossQty();
            }
            Integer dayProductionQty = dayProductionInfo.getProductionQty();
            Integer realDayProductionQty = Math.min(sumProductionQty, dayProductionQty);
            Integer theoryProductionQty = dayProductionQty;
            Integer lossQtyDiffValue = theoryProductionQty - realDayProductionQty;
            lossQty = lossQty - lossQtyDiffValue;
            if (lossQty < BigDecimal.ZERO.intValue()) {
                lossQty = BigDecimal.ZERO.intValue();
            }
            realSumProductionQty = realSumProductionQty + realDayProductionQty;
            sumProductionQty = sumProductionQty - realDayProductionQty;
            //判断模具是否排产完毕，首日排产则排产完毕，否则看排产量
            boolean isDayFinish = dayProductionInfo.isFinish() ? true : realDayProductionQty.equals(dayProductionQty);
            //更新日产信息
            UpdateDayProductionInfoHelper updateInfo = new UpdateDayProductionInfoHelper(day, realDayProductionQty.intValue(), isDayFinish, lhProductionQtyHelper.getCxMachineInfo(), lossQty);
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
        Set<String> usedMouldSet = doubleMouldList.stream().map(ProductionMouldInfoVo::getMouldCode).collect(Collectors.toSet());
        String cxMachineCode = new ArrayList<>(cxMachineInfoSet).get(BigDecimal.ZERO.intValue());
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo().get(cxMachineCode);
        MonthPlanProductionRequirePlanVo productionSkuInfo = skuProductionPlanList.get(BigDecimal.ZERO.intValue());
        String skuMaterialDesc = productionSkuInfo.getMaterialDesc();
        Set<Integer> stopDay = context.getStopDays();
        Integer firstDay = null;
        //进行排产
        for (int day = startDay; day <= endDay; day++) {
            if (sumProductionQty <= BigDecimal.ZERO.longValue()) {
                break;
            }
            //停工日跳过
            if (stopDay.contains(day)) {
                continue;
            }
            if (null == firstDay) {
                firstDay = day;
            }
            //需要考虑首日：换活字块，换模场景，此时双模日硫化量会有变化
            DayProductionQtyHelper dayProductionInfo = calculateSingleLhGroupQty(context, lhProductionQtyHelper, day, firstDay, startDay, productionSkuInfo);
            Integer lossQty;
            if (dayProductionInfo.isProductionNextDay()) {
                //隔天换模，更新当前排产完毕信息
                doubleMouldList.forEach(productionMould -> productionMould.getFinishDaySet().add(dayProductionInfo.getProductionDay()));
                //记录已排产量及损耗量
                productionContext.addSkuProductionAndWastageQty(skuMaterialDesc, BigDecimal.ZERO.intValue(), dayProductionInfo.getLossQty());
                day = getNextHasProductionDay(day, stopDay);
                if (day > endDay) {
                    break;
                }
                lossQty = dayProductionInfo.getNextDayLossQty();
            } else {
                lossQty = dayProductionInfo.getLossQty();
            }
            Integer dayProductionQty = dayProductionInfo.getProductionQty();
            Integer realDayProductionQty = Math.min(sumProductionQty, dayProductionQty);
            Integer theoryProductionQty = dayProductionQty;
//            //首日换模排产
//            if (firstDay.equals(day)) {
//                realDayProductionQty = Math.min(realDayProductionQty, paramConfiguration.getChangeMouldFirstQty());
//            }
            Integer lossQtyDiffValue = theoryProductionQty - realDayProductionQty;
            lossQty = lossQty - lossQtyDiffValue;
            if (lossQty < BigDecimal.ZERO.intValue()) {
                lossQty = BigDecimal.ZERO.intValue();
            }
            realSumProductionQty = realSumProductionQty + realDayProductionQty;
            sumProductionQty = sumProductionQty - realDayProductionQty;
            //判断模具是否排产完毕，首日排产则排产完毕，否则看排产量
            boolean isDayFinish = dayProductionInfo.isFinish() ? true : realDayProductionQty.equals(dayProductionQty);
            //更新模具日产信息
            UpdateDayProductionInfoHelper updateInfo = new UpdateDayProductionInfoHelper(day, realDayProductionQty.intValue(), isDayFinish, cxMachineInfoSet, lossQty);
            updateMouldDayProductionInfo(productionContext, doubleMouldList, skuProductionPlanList, updateInfo);
            //更新硫化组日期和日排产量
            updateCxMachineLhInfo(cxLhGroup, productionSkuInfo, cxMachineInfo, usedMouldSet, day, dayMaxProductionQty, realDayProductionQty);
            //记录已排产量及损耗量
            productionContext.addSkuProductionAndWastageQty(skuMaterialDesc, realDayProductionQty, BigDecimal.ZERO.intValue());
        }
        //更新还需排产量及实际排产量
        lhProductionQtyHelper.setSumProductionQty(sumProductionQty);
        lhProductionQtyHelper.setRealSumProductionQty(realSumProductionQty);
    }

    /**
     * 计算单硫化组的天硫化量
     * 此时不考虑与计划的余量
     *
     * @param context               排产上下文
     * @param lhProductionQtyHelper
     * @param productionDay         排产日
     * @param firstDay              排产首日
     * @param conclusionDay         收尾日
     * @return
     */
    public static DayProductionQtyHelper calculateSingleLhGroupQty(Context context, LhProductionQtyHelper lhProductionQtyHelper, Integer productionDay, Integer firstDay, Integer conclusionDay, MonthPlanProductionRequirePlanVo productionSkuInfo) {
        CxLhProductionHelper cxLhGroup = lhProductionQtyHelper.getCxLhGroup();
        String beforeSku = cxLhGroup.getMaterialDesc();
        String needProductionSku = productionSkuInfo.getMaterialDesc();
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //不是首日
        if (!firstDay.equals(productionDay)) {
            return new DayProductionQtyHelper(productionDay, false, lhProductionQtyHelper.getDayMaxProductionQty(), BigDecimal.ZERO.intValue(), BigDecimal.ZERO.intValue(), false);
        }
        ProductionCapacityParamConfiguration paramConfiguration = productionContext.getBaseDataContainer().getParamConfiguration();
        Integer firstQty = paramConfiguration.getChangeMouldFirstQty();
        //首日非收尾日
        if (!firstDay.equals(conclusionDay)) {
            Integer lossQty = lhProductionQtyHelper.getDayMaxProductionQty() - firstQty;
            return new DayProductionQtyHelper(productionDay, false, firstQty, lossQty, BigDecimal.ZERO.intValue(), true);
        }
        //同Sku，则是不同优先级的衔接
        Integer beforeSkuProductionQty = cxLhGroup.getProductionQty();
        Integer beforeSkuDayMaxQty = cxLhGroup.getDayMaxProductionQty();
        if (needProductionSku.equals(beforeSku)) {
            Integer needProductionQty = beforeSkuDayMaxQty - beforeSkuProductionQty;
            return new DayProductionQtyHelper(productionDay, false, needProductionQty, BigDecimal.ZERO.intValue(), BigDecimal.ZERO.intValue(), false);
        }
        boolean isChangeMould = !productionContext.getBaseDataContainer().isShareMouldSameGroup(beforeSku, needProductionSku);
        if (isChangeMould) {
            //换模
            return buildByChangeMould(productionDay, lhProductionQtyHelper, paramConfiguration);
        }
        //换活字块
        Integer afterSkuProductionQty;
        Integer beforeSkuDiffValue = Math.abs(beforeSkuDayMaxQty - beforeSkuProductionQty);
        if (beforeSkuDiffValue <= paramConfiguration.getChangeTypeBlockQtyDiff()) {
            afterSkuProductionQty = paramConfiguration.getChangeTypeBlockQty();
        } else {
            afterSkuProductionQty = paramConfiguration.getChangeTypeBlockMaxQty();
        }
        Integer lossQty = lhProductionQtyHelper.getDayMaxProductionQty() - beforeSkuDayMaxQty;
        lossQty = lossQty - afterSkuProductionQty;
        if (lossQty < BigDecimal.ZERO.intValue()) {
            lossQty = BigDecimal.ZERO.intValue();
        }
        return new DayProductionQtyHelper(productionDay, false, afterSkuProductionQty, lossQty, BigDecimal.ZERO.intValue(), false);
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
     * 处理提前收尾，导致需要释放的模壳使用量、模具分配比例使用量、胶囊卡盘使用量
     *
     * @param context             排产上下文
     * @param groupPlanInfo       提前收尾结构
     * @param beforeConclusionDay 提前收尾日
     * @param singleMould         模具信息
     */
    public static void handlerBeforeConclusion(Context context, ProductionPlanGroupInfo groupPlanInfo, Integer beforeConclusionDay, ProductionMouldInfoVo singleMould, String materialDesc) {
        if (null == groupPlanInfo || StringUtils.isBlank(materialDesc)) {
            return;
        }
        if (null == beforeConclusionDay || null == singleMould) {
            return;
        }
        List<MonthPlanProductionRequirePlanVo> groupPlanData = groupPlanInfo.getGroupPlanData();
        if (CollectionUtils.isEmpty(groupPlanData)) {
            return;
        }
        List<MonthPlanProductionRequirePlanVo> productionPlanList = groupPlanData.stream().filter(singlePlan -> materialDesc.equals(singlePlan.getMaterialDesc())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(productionPlanList)) {
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        MonthPlanProductionRequirePlanVo productionPlan = productionPlanList.get(BigDecimal.ZERO.intValue());
        //模具分配比例控制对象
        MouldAllocationInfoVo mouldAllocationControlInfo = productionContext.getMouldAllocationInfo(productionPlan);
        //胶囊卡盘数量控制对象
        CapsuleChuckInfoVo capsuleChuckInfo = productionContext.getCapsuleChuckInfo(productionPlan);
        //模壳标准使用量 - 1
        updateMouldShellInfoByMould(productionContext, beforeConclusionDay, singleMould, YesOrNoEnum.NO.getValue());
        //模具分配比例使用量 - 1
        updateMouldAllocationRatioInfoByMould(mouldAllocationControlInfo, beforeConclusionDay, singleMould, YesOrNoEnum.NO.getValue());
        //胶囊卡盘使用量 - 1
        updateCapsuleChuckInfoByMould(capsuleChuckInfo, beforeConclusionDay, singleMould, YesOrNoEnum.NO.getValue());
        //20260122 换模次数 -1
        updateChangeMouldInfoByMould(context, beforeConclusionDay, productionPlan.getMaterialDesc(), singleMould);
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
     * 获取一个可排产日
     *
     * @param productionDay 当前排产日
     * @param stopDay       停工日集合
     * @return
     */
    private static Integer getNextHasProductionDay(Integer productionDay, Set<Integer> stopDay) {
        Integer newProductionDay = productionDay + BigDecimal.ONE.intValue();
        if (stopDay.contains(newProductionDay)) {
            return getNextHasProductionDay(newProductionDay, stopDay);
        }
        return newProductionDay;
    }

    /**
     * 构建日排产信息-换模场景
     * 1、结构上机首日
     * 2、衔接前后规格-换模
     *
     * @param productionDay         排产日
     * @param lhProductionQtyHelper 排产信息
     * @param paramConfiguration    排产参数
     * @return
     */
    private static DayProductionQtyHelper buildByChangeMould(Integer productionDay, LhProductionQtyHelper lhProductionQtyHelper, ProductionCapacityParamConfiguration paramConfiguration) {
        CxLhProductionHelper cxLhGroup = lhProductionQtyHelper.getCxLhGroup();
        String beforeSku = cxLhGroup.getMaterialDesc();
        Integer firstQty = paramConfiguration.getChangeMouldFirstQty();
        //没有前规格，通常为结构上机首日
        if (StringUtils.isBlank(beforeSku)) {
            Integer lossQty = lhProductionQtyHelper.getDayMaxProductionQty() - firstQty;
            return new DayProductionQtyHelper(productionDay, false, firstQty, lossQty, BigDecimal.ZERO.intValue(), true);
        }
        //衔接
        Integer beforeSkuProductionQty = cxLhGroup.getProductionQty();
        Integer beforeSkuDayMaxQty = cxLhGroup.getDayMaxProductionQty();
        Integer lossQty = lhProductionQtyHelper.getDayMaxProductionQty() - beforeSkuDayMaxQty;
        Integer halfQty = beforeSkuDayMaxQty / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        if (beforeSkuProductionQty < halfQty) {
            //当天换模
            lossQty = lossQty - firstQty;
            if (lossQty < BigDecimal.ZERO.intValue()) {
                lossQty = BigDecimal.ZERO.intValue();
            }
            return new DayProductionQtyHelper(productionDay, false, firstQty, lossQty, BigDecimal.ZERO.intValue(), true);
        }
        //隔天换模
        if (lossQty < BigDecimal.ZERO.intValue()) {
            lossQty = BigDecimal.ZERO.intValue();
        }
        Integer nextDayLossQty = lhProductionQtyHelper.getDayMaxProductionQty() - firstQty;
        return new DayProductionQtyHelper(productionDay, true, firstQty, lossQty, nextDayLossQty, true);
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
        SkuDayProductionInfoHelper skuDayProductionInfo = SkuDayProductionInfoHelper.buildEmpty(productionDay, productionPlan, realDayProductionQty, lossQty, usedMouldSet);
        groupPlanInfo.addDayProductionInfo(skuDayProductionInfo);
        //模具排产信息-计划分配
        updateMouldDayProductionInfo(productionContext, doubleMouldList, skuProductionPlanList, updateInfo);
        //记录已排产量及损耗量
        productionContext.addSkuProductionAndWastageQty(skuMaterialDesc, realDayProductionQty, lossQty);
    }

    /**
     * 更新模具的日排产信息，并更新计划的待排产量
     * 更新使用模具的日排产信息
     * 需要对排产量按计划集合的优先级进行分配到具体的计划Id
     *
     * @param productionContext     排产上下文
     * @param doubleMouldList       选中的排产模具
     * @param skuProductionPlanList 排产的Sku计划集合
     * @param updateInfo            日更新信息对象
     */
    private static void updateMouldDayProductionInfo(TbrProductionContext productionContext, List<ProductionMouldInfoVo> doubleMouldList, List<MonthPlanProductionRequirePlanVo> skuProductionPlanList, UpdateDayProductionInfoHelper updateInfo) {
        Integer productionDay = updateInfo.getProductionDay();
        Integer realDayProductionQty = updateInfo.getRealDayProductionQty();
        boolean isDayFinish = updateInfo.isDayFinish();
        Set<String> cxMachineInfo = updateInfo.getUsedCxMachineInfo();
        //模具排产信息-计划分配
        Map<Long, MonthPlanProductionRequirePlanVo> needDeductionMap = skuProductionPlanList.stream().collect(Collectors.toMap(MonthPlanProductionRequirePlanVo::getMonthPlanId, Function.identity()));
        Map<Long, Integer> productionPlanMap = new ProductionPlanDistributor().allocationProductionQty(realDayProductionQty, skuProductionPlanList);
        Set<String> isSelectedSingle = new HashSet<>();
        productionPlanMap.forEach((monthPlanId, planProductionQty) -> {
            MonthPlanProductionRequirePlanVo groupPlan = needDeductionMap.get(monthPlanId);
            doubleMouldList.forEach(productionMould -> productionMould.addProductionInfo(productionDay, groupPlan, isDayFinish, planProductionQty, cxMachineInfo));
            //奇数补充 出现两条单奇数
            ProductionMouldInfoVo firstMould = doubleMouldList.get(BigDecimal.ZERO.intValue());
            ProductionMouldInfoVo secondMould = doubleMouldList.get(BigDecimal.ONE.intValue());
            ProductionMouldInfoVo singleMould;
            if (planProductionQty % ProductionConstant.DOUBLE_MOULD_PRODUCTION != BigDecimal.ZERO.intValue()) {
                if (isSelectedSingle.size() == ProductionConstant.DOUBLE_MOULD_PRODUCTION) {
                    return;
                }
                if (isSelectedSingle.contains(firstMould.getMouldCode())) {
                    singleMould = secondMould;
                } else {
                    singleMould = firstMould;
                }
                isSelectedSingle.add(singleMould.getMouldCode());
                if (null != singleMould) {
                    singleMould.addProductionInfo(productionDay, groupPlan, isDayFinish, ProductionConstant.DOUBLE_MOULD_PRODUCTION, cxMachineInfo);
                }
            }
        });
        MonthPlanProductionRequirePlanVo productionPlan = skuProductionPlanList.get(BigDecimal.ZERO.intValue());
        //模具分配比例控制对象
        MouldAllocationInfoVo mouldAllocationControlInfo = productionContext.getMouldAllocationInfo(productionPlan);
        //胶囊卡盘数量控制对象
        CapsuleChuckInfoVo capsuleChuckInfo = productionContext.getCapsuleChuckInfo(productionPlan);
        doubleMouldList.forEach(singleMould -> {
            //模壳标准使用量 + 1
            updateMouldShellInfoByMould(productionContext, productionDay, singleMould, YesOrNoEnum.YES.getValue());
            //模具分配比例使用量 + 1
            updateMouldAllocationRatioInfoByMould(mouldAllocationControlInfo, productionDay, singleMould, YesOrNoEnum.YES.getValue());
            //胶囊卡盘使用量 + 1
            updateCapsuleChuckInfoByMould(capsuleChuckInfo, productionDay, singleMould, YesOrNoEnum.YES.getValue());
        });


//        //模壳的使用量 更新
//        doubleMouldList.forEach(singleMould -> {
//            String mouldSetCode = singleMould.getMouldSetCode();
//            if (StringUtils.isBlank(mouldSetCode)) {
//                return;
//            }
//            MouldShellBaseInfoVo mouldShellInfo = productionContext.getMouldShellInfo(singleMould);
//            if (null == mouldShellInfo) {
//                return;
//            }
//            mouldShellInfo.addUsedCount(productionDay, singleMould.getMouldCode());
//        });
//        //模具分配使用量更新
//        if (null == mouldAllocationControlInfo) {
//            return;
//        }
//        doubleMouldList.forEach(singleMould -> mouldAllocationControlInfo.addUsedCount(productionDay, singleMould.getMouldCode()));
    }

    /**
     * 更新成型机的硫化组信息
     *
     * @param cxLhGroup            成型对应的硫化组
     * @param productionSkuInfo    排产Sku信息
     * @param cxMachineInfo        成型机台
     * @param usedMouldSet         使用模具
     * @param productionDay        排产日
     * @param dayMaxProductionQty  日最大硫化值
     * @param realDayProductionQty 实际排产值
     */
    private static void updateCxMachineLhInfo(CxLhProductionHelper cxLhGroup, MonthPlanProductionRequirePlanVo productionSkuInfo, CxMachineBaseInfoVo cxMachineInfo, Set<String> usedMouldSet, Integer productionDay, Integer dayMaxProductionQty, Integer realDayProductionQty) {
        //排产信息更新
        cxLhGroup.setProductionQty(realDayProductionQty);
        cxLhGroup.setProductionDay(productionDay);
        cxLhGroup.setDayMaxProductionQty(dayMaxProductionQty);
        cxLhGroup.setMaterialDesc(productionSkuInfo.getMaterialDesc());
        cxLhGroup.setMaterialCode(productionSkuInfo.getMaterialCode());
        cxLhGroup.setEmbryoCode(productionSkuInfo.getEmbryoCode());
        cxLhGroup.setProductionMouldSet(usedMouldSet);
        cxMachineInfo.getCxLhRatioMap().put(cxLhGroup.getLhGroupNo(), cxLhGroup);
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo = cxMachineInfo.getDayProductionLimitInfo();
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return;
        }
        GroupPlanCxLhCapacityLimitHelper dayLimit = dayProductionLimitInfo.get(productionDay);
        if (null == dayLimit) {
            return;
        }
        //更新生胎及模具
        dayLimit.getProductionEmbryoCodeSet().add(productionSkuInfo.getEmbryoCode());
        dayLimit.getProductionMouldSet().addAll(usedMouldSet);
    }

    /**
     * 更新模壳使用量
     * 1、isAdd = 1时，模壳使用量 + 1
     * 2、isAdd = 0时，模壳使用量 - 1
     *
     * @param productionContext 排产上下文
     * @param productionDay     排产日
     * @param singleMould       单副模具
     * @param isAdd             1(使用数 + 1) 0(使用数 - 1)
     */
    private static void updateMouldShellInfoByMould(TbrProductionContext productionContext, Integer productionDay, ProductionMouldInfoVo singleMould, Integer isAdd) {
        //模具使用的模壳标准
        String mouldSetCode = singleMould.getMouldSetCode();
        if (StringUtils.isBlank(mouldSetCode)) {
            return;
        }
        //取得模壳标准控制信息对象
        MouldShellBaseInfoVo mouldShellInfo = productionContext.getMouldShellInfo(singleMould);
        if (null == mouldShellInfo) {
            return;
        }
        //使用数+1
        if (YesOrNoEnum.YES.getValue().equals(isAdd)) {
            mouldShellInfo.addUsedCount(productionDay, singleMould.getMouldCode());
        }
        //使用数-1
        if (YesOrNoEnum.NO.getValue().equals(isAdd)) {
            mouldShellInfo.deductionUsedCount(productionDay, singleMould.getMouldCode());
        }
    }

    /**
     * 更新模具分配比例的使用
     * 1、isAdd = 1时，模具分配比例使用量 + 1
     * 2、isAdd = 0时，模具分配比例使用量 - 1
     *
     * @param mouldAllocationControlInfo 模具分配比例对象
     * @param productionDay              排产日
     * @param singleMould                单副模具
     * @param isAdd                      1(使用数 + 1) 0(使用数 - 1)
     */
    private static void updateMouldAllocationRatioInfoByMould(MouldAllocationInfoVo mouldAllocationControlInfo, Integer productionDay, ProductionMouldInfoVo singleMould, Integer isAdd) {
        if (null == mouldAllocationControlInfo) {
            return;
        }
        if (null == singleMould || StringUtils.isBlank(singleMould.getMouldCode())) {
            return;
        }
        //使用数+1
        if (YesOrNoEnum.YES.getValue().equals(isAdd)) {
            mouldAllocationControlInfo.addUsedCount(productionDay, singleMould.getMouldCode());
        }
        //使用数-1
        if (YesOrNoEnum.NO.getValue().equals(isAdd)) {
            mouldAllocationControlInfo.deductionUsedCount(productionDay, singleMould.getMouldCode());
        }
    }

    /**
     * 更新模具胶囊卡盘使用
     * 1、isAdd = 1时，胶囊卡盘使用量 + 1
     * 2、isAdd = 0时，胶囊卡盘使用量 - 1
     *
     * @param capsuleChuckInfo 模具胶囊卡盘对象
     * @param productionDay    排产日
     * @param singleMould      单副模具
     * @param isAdd            1(使用数 + 1) 0(使用数 - 1)
     */
    private static void updateCapsuleChuckInfoByMould(CapsuleChuckInfoVo capsuleChuckInfo, Integer productionDay, ProductionMouldInfoVo singleMould, Integer isAdd) {
        if (null == capsuleChuckInfo) {
            return;
        }
        if (null == singleMould || StringUtils.isBlank(singleMould.getMouldCode())) {
            return;
        }
        //使用数+1
        if (YesOrNoEnum.YES.getValue().equals(isAdd)) {
            capsuleChuckInfo.addUsedCount(productionDay, singleMould.getMouldCode());
        }
        //使用数-1
        if (YesOrNoEnum.NO.getValue().equals(isAdd)) {
            capsuleChuckInfo.deductionUsedCount(productionDay, singleMould.getMouldCode());
        }
    }

    /**
     * 换模次数-1
     *
     * @param context       排产上下文
     * @param productionDay 排产日
     * @param materialDesc  物料描述
     * @param singleMould   模具信息
     */
    private static void updateChangeMouldInfoByMould(Context context, Integer productionDay, String materialDesc, ProductionMouldInfoVo singleMould) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        DayCapacityLimitVo dayCapacityLimit = productionContext.getBaseDataContainer().getDayCapacityLimit();
        if (null == dayCapacityLimit) {
            return;
        }
        String mouldCode = singleMould.getMouldCode();
        if (StringUtils.isBlank(mouldCode) || StringUtils.isBlank(materialDesc) || null == productionDay) {
            return;
        }
        dayCapacityLimit.deductionChangeMouldUsedQty(context, productionDay, materialDesc, mouldCode);
    }

    private CxLhMouldProductionCalculator() {

    }
}
