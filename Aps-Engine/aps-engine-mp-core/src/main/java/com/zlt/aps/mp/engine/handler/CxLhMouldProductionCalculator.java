package com.zlt.aps.mp.engine.handler;

import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.engine.check.SkuSecondChecker;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.daylimit.*;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.*;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.mp.engine.enums.ContinueTypeEnum;
import com.zlt.aps.mp.engine.logrecorder.TbrBoostQtyProductionLogRecorder;
import com.zlt.aps.mp.engine.logrecorder.TbrMouldProductionLogRecorder;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.engine.scheduling.cxcapacity.ProductionCapacityParamConfiguration;
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
        Set<Integer> openDay = context.getProductionDayAfterStop();
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //日硫化量，理论上productionQty不超过dayMaxProductionQty
        Integer dayMaxProductionQty = continueSkuInfo.getMaxDaySingleLhMachineQty();
        boolean isDayFinish = productionQty >= dayMaxProductionQty ? true : false;
        Set<String> cxMachineCodeInfo = continueSkuInfo.getOnLineCxMachineSet();
        List<MonthPlanProductionRequirePlanVo> continueSkuPlanList = continueSkuInfo.getContinueSkuPlanList();
        String skuMaterialDesc = continueSkuPlanList.get(BigDecimal.ZERO.intValue()).getMaterialDesc();
        //20260127 开产日-量放一半
        Integer lossQty = BigDecimal.ZERO.intValue();
        if (openDay.contains(productionDay)) {
            Integer openMaxQty = context.getOpenDayMaxQty(productionDay, dayMaxProductionQty);
            Integer theoryProductionQty = productionQty;
            productionQty = Math.min(productionQty, openMaxQty);
            lossQty = theoryProductionQty - productionQty;
        }
        //todo 20260211 特殊材料消耗库存量比较，库存量与realDayProductionQty取最小
        Integer lossQtyDiffValue = productionQty;
        Integer specialMaterialLimitQty = productionContext.getSpecialMaterialProductionQtyBySku(groupPlanInfo, productionQty);
        if (specialMaterialLimitQty <= BigDecimal.ZERO.intValue()) {
            continueSkuPlanList.forEach(singlePlan -> singlePlan.setIsThisRound(YesOrNoEnum.NO.getValue()));
            productionContext.addSkuProductionLimitInfo(skuMaterialDesc, MouldProductionLimitTypeEnum.SPECIAL_MATERIAL_STOCK_LIMIT);
            return;
        }
        productionQty = specialMaterialLimitQty;
        lossQtyDiffValue = lossQtyDiffValue - productionQty;
        lossQty = lossQty - lossQtyDiffValue;
        //更新日产信息
        UpdateDayProductionInfoHelper updateInfo = new UpdateDayProductionInfoHelper(productionDay, productionQty, isDayFinish, cxMachineCodeInfo, lossQty);
        updateDayProductionInfo(productionContext, groupPlanInfo, doubleMouldList, continueSkuPlanList, updateInfo);
    }

    /**
     * 采用双模，在startDay~endDay进行排产
     * 此时为在机结构对在产机台进行新增Sku排产的场景
     * 以结构为维度，(多台成型机台-忽略机台)进行排产
     *
     * @param context               排产上下文
     * @param lhProductionQtyHelper 排产基础信息
     * @param startDay              排产开始日
     * @param endDay                排产结束日
     * @param doubleMouldList       排产的双模模具
     * @param skuProductionPlanList sku的排产计划
     * @param continueType          续作Sku排产类型(包含同规格同花纹、共生胎同模具，非续作SKU)
     */
    public static void lhProductionByGroupHandler(Context context, LhProductionQtyHelper lhProductionQtyHelper, Integer startDay, Integer endDay, List<ProductionMouldInfoVo> doubleMouldList, List<MonthPlanProductionRequirePlanVo> skuProductionPlanList, ContinueTypeEnum continueType) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Integer sumProductionQty = lhProductionQtyHelper.getSumProductionQty();
        Integer realSumProductionQty = lhProductionQtyHelper.getRealSumProductionQty();
        ProductionPlanGroupInfo productionPlanInfo = lhProductionQtyHelper.getProductionPlanInfo();
        //不关注具体Id，只为拿到生胎等信息
        MonthPlanProductionRequirePlanVo productionPlan = skuProductionPlanList.get(BigDecimal.ZERO.intValue());
        Set<Integer> stopDay = context.getStopDays();
        Set<Integer> openDay = context.getProductionDayAfterStop();
        Set<Integer> replenishmentDay = context.getReplenishmentDay();
        Integer dayLhQty = productionPlan.getMaxDaySingleLhMachineQty();
        //非续作需要重新判断胎胚种类数及配比限制
        if (ContinueTypeEnum.NO_CONTINUE == continueType) {
            //得到真正上机日
            Integer realStartDay = productionPlanInfo.getRealOnlineMachineDay(productionPlan, startDay, endDay);
            if (null == realStartDay) {
                //本轮不再参与排产
                skuProductionPlanList.forEach(singlePlan -> singlePlan.setIsThisRound(YesOrNoEnum.NO.getValue()));
                return;
            }
            //SKU二次上机检查 sandy+ 20260129
            if (!checkSecOnline(productionPlanInfo, productionContext, productionPlan, realStartDay)) {
                skuProductionPlanList.forEach(singlePlan -> singlePlan.setIsThisRound(YesOrNoEnum.NO.getValue()));
                return;
            }
        }
        //逐日进行排产，从起始日到结构收尾日
        String skuMaterialDesc = productionPlan.getMaterialDesc();
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
            dayProductionInfo.updateDoubleProductionQty();
            Integer lossQty;
            if (ContinueTypeEnum.SAME_EMBRYO_CODE_SHARE_MOULD == continueType && dayProductionInfo.isProductionNextDay()) {
                //隔天换模，更新当前排产完毕信息
                doubleMouldList.forEach(productionMould -> productionMould.getFinishDaySet().add(dayProductionInfo.getProductionDay()));
                //记录已排产量及损耗量
                productionContext.addSkuProductionAndWastageQty(skuMaterialDesc, BigDecimal.ZERO.intValue(), dayProductionInfo.getLossQty());
                day = context.getNextHasProductionDay(day, stopDay);
                if (day > endDay) {
                    break;
                }
                //处理续作换活字块的换膜
                handlerDayShareMouldChangeMould(productionContext, day, skuMaterialDesc, doubleMouldList);
                lossQty = dayProductionInfo.getNextDayLossQty();
            } else {
                lossQty = dayProductionInfo.getLossQty();
            }
            Integer dayProductionQty = dayProductionInfo.getProductionQty();
            Integer realDayProductionQty = Math.min(sumProductionQty, dayProductionQty);
            Integer theoryProductionQty = realDayProductionQty;
            //20260127 开产日-量放一半
            if (openDay.contains(day)) {
                Integer openMaxQty = context.getOpenDayMaxQty(day, dayLhQty);
                realDayProductionQty = Math.min(realDayProductionQty, openMaxQty);
            }
            //todo 20260211 特殊材料消耗库存量比较，库存量与realDayProductionQty取最小
            Integer specialMaterialLimitQty = productionContext.getSpecialMaterialProductionQtyBySku(productionPlanInfo, realDayProductionQty);
            if (specialMaterialLimitQty <= BigDecimal.ZERO.intValue()) {
                skuProductionPlanList.forEach(singlePlan -> singlePlan.setIsThisRound(YesOrNoEnum.NO.getValue()));
                productionContext.addSkuProductionLimitInfo(skuMaterialDesc, MouldProductionLimitTypeEnum.SPECIAL_MATERIAL_STOCK_LIMIT);
                break;
            }
            realDayProductionQty = specialMaterialLimitQty;
            Integer lossQtyDiffValue = dayProductionQty - realDayProductionQty;
            lossQty = lossQty - lossQtyDiffValue;
            if (lossQty < BigDecimal.ZERO.intValue()) {
                lossQty = BigDecimal.ZERO.intValue();
            }
            realSumProductionQty = realSumProductionQty + realDayProductionQty;
            sumProductionQty = sumProductionQty - realDayProductionQty;
            //判断模具是否排产完毕，首日排产则排产完毕，否则看排产量
            boolean isDayFinish = dayProductionInfo.isFinish() ? true : theoryProductionQty.equals(dayProductionQty);
            //更新日产信息
            UpdateDayProductionInfoHelper updateInfo = new UpdateDayProductionInfoHelper(day, realDayProductionQty, isDayFinish, lhProductionQtyHelper.getCxMachineInfo(), lossQty);
            updateDayProductionInfo(productionContext, productionPlanInfo, doubleMouldList, skuProductionPlanList, updateInfo);
            //20260128 月底补量
            if (isBoostQtyHandler(replenishmentDay, day, sumProductionQty)) {
                BoostProductionInfoHelper boostInfo = BoostProductionInfoHelper.builder(productionPlan, doubleMouldList, productionPlanInfo, null, null, lhProductionQtyHelper.getCxMachineInfo(), day, realDayProductionQty, isDayFinish, endDay);
                boostQtyByNextBoostDay(productionContext, boostInfo);
            }
        }
        //更新还需排产量及实际排产量
        lhProductionQtyHelper.setSumProductionQty(sumProductionQty);
        lhProductionQtyHelper.setRealSumProductionQty(realSumProductionQty);
    }

    /**
     * 采用双模，在startDay~endDay进行排产
     * 在新增结构(结构本身是新增或是在机结构增机台场景)
     * 此时是固定到一台，只在模拟排产阶段使用
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
        Set<Integer> openDay = context.getProductionDayAfterStop();
        Set<Integer> replenishmentDay = context.getReplenishmentDay();
        Integer dayLhQty = productionSkuInfo.getMaxDaySingleLhMachineQty();
        ProductionPlanGroupInfo productionPlanInfo = lhProductionQtyHelper.getProductionPlanInfo();
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
            dayProductionInfo.updateDoubleProductionQty();
            Integer lossQty;
            if (dayProductionInfo.isProductionNextDay()) {
//                //隔天换模，更新当前排产完毕信息
//                doubleMouldList.forEach(productionMould -> productionMould.getFinishDaySet().add(dayProductionInfo.getProductionDay()));
//                //记录已排产量及损耗量
//                productionContext.addSkuProductionAndWastageQty(skuMaterialDesc, BigDecimal.ZERO.intValue(), dayProductionInfo.getLossQty());
//                Integer beforeDay = day;
//                day = context.getNextHasProductionDay(day, stopDay);
//                handlerNextDayChangeMould(productionContext, beforeDay, day, endDay, skuMaterialDesc, doubleMouldList);
//                if (day > endDay) {
//                    break;
//                }
                lossQty = dayProductionInfo.getNextDayLossQty();
            } else {
                lossQty = dayProductionInfo.getLossQty();
            }
            Integer dayProductionQty = dayProductionInfo.getProductionQty();
            Integer realDayProductionQty = Math.min(sumProductionQty, dayProductionQty);
            Integer theoryProductionQty = realDayProductionQty;
            //20260127 开产日-量放一半
            if (openDay.contains(day)) {
                Integer openMaxQty = context.getOpenDayMaxQty(day, dayLhQty);
                realDayProductionQty = Math.min(realDayProductionQty, openMaxQty);
            }
            //todo 20260211 特殊材料消耗库存量比较，库存量与realDayProductionQty取最小
            Integer specialMaterialLimitQty = productionContext.getSpecialMaterialProductionQtyBySku(productionPlanInfo, realDayProductionQty);
            if (specialMaterialLimitQty <= BigDecimal.ZERO.intValue()) {
                skuProductionPlanList.forEach(singlePlan -> singlePlan.setIsThisRound(YesOrNoEnum.NO.getValue()));
                productionContext.addSkuProductionLimitInfo(skuMaterialDesc, MouldProductionLimitTypeEnum.SPECIAL_MATERIAL_STOCK_LIMIT);
                break;
            }
            realDayProductionQty = specialMaterialLimitQty;
            Integer lossQtyDiffValue = dayProductionQty - realDayProductionQty;
            lossQty = lossQty - lossQtyDiffValue;
            if (lossQty < BigDecimal.ZERO.intValue()) {
                lossQty = BigDecimal.ZERO.intValue();
            }
            realSumProductionQty = realSumProductionQty + realDayProductionQty;
            sumProductionQty = sumProductionQty - realDayProductionQty;
            //判断模具是否排产完毕，首日排产则排产完毕，否则看排产量
            boolean isDayFinish = dayProductionInfo.isFinish() ? true : theoryProductionQty.equals(dayProductionQty);
            //更新模具日产信息
            UpdateDayProductionInfoHelper updateInfo = new UpdateDayProductionInfoHelper(day, realDayProductionQty, isDayFinish, cxMachineInfoSet, lossQty);
            updateMouldDayProductionInfo(productionContext, lhProductionQtyHelper.getProductionPlanInfo(), doubleMouldList, skuProductionPlanList, updateInfo);
            //更新硫化组日期和日排产量
            updateCxMachineLhInfo(cxLhGroup, productionSkuInfo, cxMachineInfo, usedMouldSet, dayMaxProductionQty, updateInfo);
            //记录已排产量及损耗量
            productionContext.addSkuProductionAndWastageQty(skuMaterialDesc, realDayProductionQty, BigDecimal.ZERO.intValue());
            //月底补量
            if (isBoostQtyHandler(replenishmentDay, day, sumProductionQty)) {
                BoostProductionInfoHelper boostInfo = BoostProductionInfoHelper.builder(productionSkuInfo, doubleMouldList, productionPlanInfo, cxMachineInfo, cxLhGroup, cxMachineInfoSet, day, realDayProductionQty, isDayFinish, endDay);
                boostQtyByNextBoostDay(productionContext, boostInfo);
            }
        }
        //更新还需排产量及实际排产量
        lhProductionQtyHelper.setSumProductionQty(sumProductionQty);
        lhProductionQtyHelper.setRealSumProductionQty(realSumProductionQty);
    }

    /**
     * 计算单硫化组的天硫化量
     * 此时不考虑与计划的余量
     * 1、非首日排产，则表示续作，
     * 排产量 = 日硫化量
     * 损耗量 = 0
     * 2、首日排产即为结构收尾日
     * 排产量 = 首日排产量参数
     * 损耗量 = 日硫化量 - 排产量
     * 3、首日排产，前Sku=后Sku，则为同Sku不同优先级的衔接排产
     * 排产量 = 日硫化量 - 前Sku排产量
     * 损耗量 = 0
     * 4、首日排产，前Sku！=后Sku，需要判断是否为同生胎共模具
     * 4.1、如果不是同生胎共模具，则为换模
     * 参见buildByChangeMould的说明
     * 4.2、否则为换活字块，需计算前Sku排产量与前Sku日硫化量的差值
     * 4.2.1、如果差值 <= 参数值，
     * 排产量 = 小于差值的排产量参数
     * 损耗量 = 后Sku日硫化量 - 前Sku排产量 - 排产量
     * 4.2.2、如果差值 > 参数值
     * 排产量 = 大于差值的排产量参数
     * 损耗量 = 后Sku日硫化量 - 前Sku排产量 - 排产量
     *
     * @param context               排产上下文
     * @param lhProductionQtyHelper 硫化排产信息(前后Sku信息)
     * @param productionDay         排产日
     * @param firstDay              排产首日
     * @param conclusionDay         收尾日
     * @return
     */
    public static DayProductionQtyHelper calculateSingleLhGroupQty(Context context, LhProductionQtyHelper lhProductionQtyHelper, Integer productionDay, Integer firstDay, Integer conclusionDay, MonthPlanProductionRequirePlanVo productionSkuInfo) {
        //不是首日
        if (!firstDay.equals(productionDay)) {
            return new DayProductionQtyHelper(productionDay, false, lhProductionQtyHelper.getDayMaxProductionQty(), BigDecimal.ZERO.intValue(), BigDecimal.ZERO.intValue(), false);
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //首日排产量参数
        ProductionCapacityParamConfiguration paramConfiguration = productionContext.getBaseDataContainer().getParamConfiguration();
        Integer firstQty = paramConfiguration.getChangeMouldFirstQty();
        //首日非前Sku收尾日
        if (!firstDay.equals(conclusionDay)) {
            Integer lossQty = lhProductionQtyHelper.getDayMaxProductionQty() - firstQty;
            return new DayProductionQtyHelper(productionDay, false, firstQty, lossQty, BigDecimal.ZERO.intValue(), true);
        }
        CxLhProductionHelper cxLhGroup = lhProductionQtyHelper.getCxLhGroup();
        String beforeSku = cxLhGroup.getBeforeSku().getMaterialDesc();
        String needProductionSku = productionSkuInfo.getMaterialDesc();
        //同Sku，则是不同优先级的衔接
        Integer beforeSkuDayMaxQty = cxLhGroup.getBeforeSku().getDayMaxQty();
        //前Sku的排产量
        Integer beforeSkuProductionQty = cxLhGroup.getBeforeSku().getProductionQty();
        if (needProductionSku.equals(beforeSku)) {
            Integer needProductionQty = beforeSkuDayMaxQty - beforeSkuProductionQty;
            return new DayProductionQtyHelper(productionDay, false, needProductionQty, BigDecimal.ZERO.intValue(), BigDecimal.ZERO.intValue(), false);
        }
        boolean isChangeMould = !productionContext.getBaseDataContainer().isShareMouldSameGroup(beforeSku, needProductionSku);
        if (isChangeMould) {
            //换模
            return buildByChangeMould(productionDay, lhProductionQtyHelper, paramConfiguration);
        }
        Integer changeTypeBlockQtyDiff = paramConfiguration.getChangeTypeBlockQtyDiff();
        //当天损耗量
        Integer lossQty = lhProductionQtyHelper.getDayMaxProductionQty() - beforeSkuProductionQty;
        //前Sku排产量与前Sku日硫化量的差值
        Integer beforeSkuDiffValue = Math.abs(beforeSkuDayMaxQty - beforeSkuProductionQty);
        //差值 > 参数值，表示可以当天换活字块，排量 = changeTypeBlockMaxQty
        if (beforeSkuDiffValue >= changeTypeBlockQtyDiff) {
            Integer afterSkuProductionQty = paramConfiguration.getChangeTypeBlockMaxQty();
            //损耗量 = 日硫化量 - 前Sku排产量 - 自己排产量
            lossQty = lossQty - afterSkuProductionQty;
            if (lossQty < BigDecimal.ZERO.intValue()) {
                lossQty = BigDecimal.ZERO.intValue();
            }
            return new DayProductionQtyHelper(productionDay, false, afterSkuProductionQty, lossQty, BigDecimal.ZERO.intValue(), true);
        }
        //差值 <= 参数值，表示隔天换活字块，排量 = changeTypeBlockQty
        if (lossQty < BigDecimal.ZERO.intValue()) {
            lossQty = BigDecimal.ZERO.intValue();
        }
        //隔天换活字块，则隔天损耗量 = 日硫化量 - 排产量
        Integer afterSkuProductionQty = paramConfiguration.getChangeTypeBlockQty();
        Integer nextDayLossQty = lhProductionQtyHelper.getDayMaxProductionQty() - afterSkuProductionQty;
        if (lossQty < BigDecimal.ZERO.intValue()) {
            lossQty = BigDecimal.ZERO.intValue();
        }
        return new DayProductionQtyHelper(productionDay, true, afterSkuProductionQty, lossQty, nextDayLossQty, true);
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
     * 检查二次上机
     *
     * @param productionPlanInfo 排产计划信息
     * @param productionContext  排产上下文
     * @param productionPlan     排产计划信息
     * @param realStartDay       上机日
     * @return true-允许二次上机，false-不允许二次上机
     */
    private static boolean checkSecOnline(ProductionPlanGroupInfo productionPlanInfo, TbrProductionContext productionContext,
                                          MonthPlanProductionRequirePlanVo productionPlan, Integer realStartDay) {
        List<Integer> dayList = productionPlanInfo.getProductionDaySetBySku(productionPlan.getMaterialDesc());
        if (CollectionUtils.isEmpty(dayList)) {
            return true;
        }
        Set<Integer> productionDaySet = dayList.stream().collect(Collectors.toSet());
        if (productionDaySet.contains(realStartDay)) {
            return true;
        }
        //降序,第一个元素最大
        dayList.sort(Comparator.reverseOrder());
        Integer lastCloseDay = dayList.get(0);
        int skuSecondProductionDays = productionContext.getBaseDataContainer().getParamConfiguration().getSkuSecondProduction();
        SkuSecondChecker skuSecondChecker = new SkuSecondChecker(realStartDay, lastCloseDay, skuSecondProductionDays);
        return skuSecondChecker.doCheck();
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
        updateMouldDayProductionInfo(productionContext, groupPlanInfo, doubleMouldList, skuProductionPlanList, updateInfo);
        //记录已排产量及损耗量
        productionContext.addSkuProductionAndWastageQty(skuMaterialDesc, realDayProductionQty, lossQty);
    }

    /**
     * 判断是否需要进行补量处理
     * 如果还有待排产量，则不用补量
     * 如果productionDay不再补量天数集合replenishmentDay中也不用补量
     *
     * @param replenishmentDay  可补量的天数
     * @param productionDay     排产日
     * @param needProductionQty 还需排产量
     * @return
     */
    private static boolean isBoostQtyHandler(Set<Integer> replenishmentDay, Integer productionDay, Integer needProductionQty) {
        if (needProductionQty > BigDecimal.ZERO.intValue()) {
            return false;
        }
        if (null == productionDay || CollectionUtils.isEmpty(replenishmentDay)) {
            return false;
        }
        if (!replenishmentDay.contains(productionDay)) {
            return false;
        }
        return true;
    }

    /**
     * 从起始天开始，进行补量
     *
     * @param productionContext 排产上下文
     * @param boostInfo         补量信息
     */
    private static void boostQtyByNextBoostDay(TbrProductionContext productionContext, BoostProductionInfoHelper boostInfo) {
        Set<Integer> replenishmentDay = productionContext.getReplenishmentDay();
        if (CollectionUtils.isEmpty(replenishmentDay)) {
            return;
        }
        MonthPlanProductionRequirePlanVo productionSkuInfo = boostInfo.getProductionSkuInfo();
        ProductionCapacityParamConfiguration paramConfiguration = productionContext.getBaseDataContainer().getParamConfiguration();
        if (!productionSkuInfo.hasBoostQty(paramConfiguration.getBoostProductionType())) {
            return;
        }
        Set<String> cxMachineInfoSet = boostInfo.getCxMachineInfoSet();
        Integer startBoostDay = boostInfo.getStartBoostDay();
        Integer endBoostDay = boostInfo.getEndBoostDay();
        Integer maxLhQty = productionSkuInfo.getMaxDaySingleLhMachineQty();
        String skuMaterialDesc = productionSkuInfo.getMaterialDesc();
        CxLhProductionHelper cxLhGroup = boostInfo.getCxLhGroup();
        boolean isSingleCxMachine = boostInfo.isSingleCxMachine();
        ProductionPlanGroupInfo productionPlanInfo = boostInfo.getProductionPlanInfo();
        CxMachineBaseInfoVo cxMachineInfo = boostInfo.getCxMachineInfo();
        List<ProductionMouldInfoVo> doubleMouldList = boostInfo.getDoubleMouldList();
        Set<String> usedMouldSet = doubleMouldList.stream().map(ProductionMouldInfoVo::getMouldCode).collect(Collectors.toSet());
        //如果是开产日，跳过从下一天开始
        Set<Integer> openDay = productionContext.getProductionDayAfterStop();
        Integer realStartBoostDay = startBoostDay;
        if (openDay.contains(startBoostDay) || boostInfo.isStartFinish()) {
            realStartBoostDay = realStartBoostDay + BigDecimal.ONE.intValue();
        }
        for (Integer singleReplenishmentDay = realStartBoostDay; singleReplenishmentDay <= endBoostDay; singleReplenishmentDay++) {
            if (!replenishmentDay.contains(singleReplenishmentDay)) {
                continue;
            }
            //补量值
            Integer boostDayQty;
            if (singleReplenishmentDay.equals(startBoostDay)) {
                boostDayQty = maxLhQty - boostInfo.getStartPlannedQty();
            } else {
                boostDayQty = maxLhQty;
            }
            log.info(TbrBoostQtyProductionLogRecorder.addBoostQtyProductionPlanLog(productionContext, productionSkuInfo, singleReplenishmentDay, boostDayQty, usedMouldSet));
            if (!isSingleCxMachine && null != productionPlanInfo) {
                //分组-日排产信息
                SkuDayProductionInfoHelper skuDayProductionInfo = SkuDayProductionInfoHelper.buildEmpty(singleReplenishmentDay, productionSkuInfo, boostDayQty, BigDecimal.ZERO.intValue(), usedMouldSet);
                productionPlanInfo.addDayProductionInfo(skuDayProductionInfo);
            }
            //更新模具日产信息
            UpdateDayProductionInfoHelper updateInfo = new UpdateDayProductionInfoHelper(singleReplenishmentDay, boostDayQty, true, cxMachineInfoSet, BigDecimal.ZERO.intValue());
            updateMouldDayProductionInfo(productionContext, productionPlanInfo, doubleMouldList, productionSkuInfo, updateInfo);
            if (isSingleCxMachine && null != cxMachineInfo && null != cxLhGroup) {
                //更新硫化组日期和日排产量
                updateCxMachineLhInfo(cxLhGroup, productionSkuInfo, cxMachineInfo, usedMouldSet, maxLhQty, updateInfo);
            }
            //记录已排产量及损耗量
            productionContext.addSkuProductionAndWastageQty(skuMaterialDesc, boostDayQty, BigDecimal.ZERO.intValue());
        }
    }

    /**
     * 构建日排产信息-换模场景
     * 1、结构上机首日
     * 2、衔接前后规格-换模
     * 排产量及损耗量计算
     * 1、结构上机首日，则直接当天换模
     * 排产量 = 首日排产量参数
     * 损耗量 = 日硫化量 - 首日排产量
     * 2、衔接前后规格
     * 2.1、判断前Sku排产量与前Sku日硫化量/2的大小
     * 2.1.1、如果排产量小于1/2的日硫化量，则当天换模
     * 排产量 = 首日排产量量
     * 损耗量 = 日硫化量 - 前Sku排产量 - 排产量
     * 2.1.2、如果排产量大于1/2的日硫化量，则隔天换模
     * 当天排产量 = 0
     * 当天损耗量 = 日硫化量 - 前Sku排产量
     * 隔天排产量 = 首日排产量
     * 隔天损耗量 = 日硫化量 - 隔天排产量
     *
     * @param productionDay         排产日
     * @param lhProductionQtyHelper 排产信息
     * @param paramConfiguration    排产参数
     * @return
     */
    private static DayProductionQtyHelper buildByChangeMould(Integer productionDay, LhProductionQtyHelper lhProductionQtyHelper, ProductionCapacityParamConfiguration paramConfiguration) {
        CxLhProductionHelper cxLhGroup = lhProductionQtyHelper.getCxLhGroup();
        String beforeSku = cxLhGroup.getBeforeSku().getMaterialDesc();
        Integer firstQty = paramConfiguration.getChangeMouldFirstQty();
        //没有前规格，通常为结构上机首日
        if (StringUtils.isBlank(beforeSku)) {
            Integer lossQty = lhProductionQtyHelper.getDayMaxProductionQty() - firstQty;
            return new DayProductionQtyHelper(productionDay, false, firstQty, lossQty, BigDecimal.ZERO.intValue(), true);
        }
        //衔接
        Integer beforeSkuProductionQty = cxLhGroup.getBeforeSku().getProductionQty();
        Integer beforeSkuDayMaxQty = cxLhGroup.getBeforeSku().getDayMaxQty();
        //当天损耗量 = 日硫化量 - 前Sku排产量
        Integer lossQty = lhProductionQtyHelper.getDayMaxProductionQty() - beforeSkuProductionQty;
        Integer halfQty = beforeSkuDayMaxQty / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        if (beforeSkuProductionQty < halfQty) {
            //当天换模 损耗量 = 当天损耗量 - 首日排产量
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
        //隔天换模，则隔天损耗量 = 日硫化量 - 首日排产量
        Integer nextDayLossQty = lhProductionQtyHelper.getDayMaxProductionQty() - firstQty;
        return new DayProductionQtyHelper(productionDay, true, firstQty, lossQty, nextDayLossQty, true);
    }

    /**
     * 前提是都有换模能力：隔天换模的处理
     *
     * @param productionContext 排产上下文
     * @param beforeDay         前一天换模
     * @param realChangeDay     真实换模日
     * @param endDay            结束日
     * @param materialDesc      物料信息
     * @param doubleMouldList   模具信息
     */
    private static void handlerNextDayChangeMould(TbrProductionContext productionContext, Integer beforeDay, Integer realChangeDay, Integer endDay, String materialDesc, List<ProductionMouldInfoVo> doubleMouldList) {
        if (null == beforeDay || null == realChangeDay || null == endDay) {
            return;
        }
        if (StringUtils.isBlank(materialDesc) || CollectionUtils.isEmpty(doubleMouldList)) {
            return;
        }
        String mouldCode = doubleMouldList.get(BigDecimal.ZERO.intValue()).getMouldCode();
        //前天换模次数-1
        Set<String> mouldCodeSet = doubleMouldList.stream().map(ProductionMouldInfoVo::getMouldCode).collect(Collectors.toSet());
        DayCapacityLimitVo changeMouldLimitHandler = productionContext.getBaseDataContainer().getDayCapacityLimit();
        if (realChangeDay > endDay) {
            return;
        }
        //前天-1
        changeMouldLimitHandler.deductionChangeMouldUsedQty(productionContext, beforeDay, materialDesc, mouldCode);
        //隔天+1
        changeMouldLimitHandler.addChangeMouldUsedQty(productionContext, realChangeDay, materialDesc, mouldCodeSet);
    }

    /**
     * 续作换活字块的处理
     *
     * @param productionContext 排产上下文
     * @param changeMouldDay    换模日
     * @param materialDesc      物料描述
     * @param doubleMouldList   使用模具
     */
    private static void handlerDayShareMouldChangeMould(TbrProductionContext productionContext, Integer changeMouldDay, String materialDesc, List<ProductionMouldInfoVo> doubleMouldList) {
        if (null == changeMouldDay || StringUtils.isBlank(materialDesc) || CollectionUtils.isEmpty(doubleMouldList)) {
            return;
        }
        Set<String> mouldCodeSet = doubleMouldList.stream().map(ProductionMouldInfoVo::getMouldCode).collect(Collectors.toSet());
        DayCapacityLimitVo changeMouldLimitHandler = productionContext.getBaseDataContainer().getDayCapacityLimit();
        //隔天+1
        changeMouldLimitHandler.addChangeMouldUsedQty(productionContext, changeMouldDay, materialDesc, mouldCodeSet);
    }

    /**
     * 更新模具的日排产信息，并更新计划的待排产量
     * 更新使用模具的日排产信息
     * 需要对排产量按计划集合的优先级进行分配到具体的计划Id
     *
     * @param productionContext     排产上下文
     * @param groupPlanInfo         分组计划
     * @param doubleMouldList       选中的排产模具
     * @param skuProductionPlanList 排产的Sku计划集合
     * @param updateInfo            日更新信息对象
     */
    private static void updateMouldDayProductionInfo(TbrProductionContext productionContext, ProductionPlanGroupInfo groupPlanInfo, List<ProductionMouldInfoVo> doubleMouldList, List<MonthPlanProductionRequirePlanVo> skuProductionPlanList, UpdateDayProductionInfoHelper updateInfo) {
        Integer productionDay = updateInfo.getProductionDay();
        Integer realDayProductionQty = updateInfo.getRealDayProductionQty();
        boolean isDayFinish = updateInfo.isDayFinish();
        Set<String> cxMachineInfo = updateInfo.getUsedCxMachineInfo();
        MonthPlanProductionRequirePlanVo productionPlan = skuProductionPlanList.get(BigDecimal.ZERO.intValue());
        //20260129 排产顺序计数器
        SkuProductionCounter productionCounter = productionContext.getProductionCounter();
        if (null != productionCounter) {
            productionCounter.addProductionSku(productionPlan.getMaterialDesc());
        }
        //模具排产信息-计划分配
        Map<Long, MonthPlanProductionRequirePlanVo> needDeductionMap = skuProductionPlanList.stream().collect(Collectors.toMap(MonthPlanProductionRequirePlanVo::getMonthPlanId, Function.identity()));
        Map<Long, Integer> productionPlanMap = new ProductionPlanDistributor().allocationProductionQty(realDayProductionQty, skuProductionPlanList);
        Map<Long, MonthPlanProductionRequirePlanVo> oddNumberMap = new HashMap<>();
        productionPlanMap.forEach((monthPlanId, planProductionQty) -> {
            MonthPlanProductionRequirePlanVo groupPlan = needDeductionMap.get(monthPlanId);
            doubleMouldList.forEach(productionMould -> productionMould.addProductionInfo(productionDay, groupPlan, isDayFinish, planProductionQty, cxMachineInfo));
            //出现剩余奇数计划
            if ((planProductionQty & BigDecimal.ONE.intValue()) != BigDecimal.ZERO.intValue()) {
                oddNumberMap.put(monthPlanId, groupPlan);
            }
        });
        //奇数补充 成对出现
        handlerLeftOverOddNumberPlan(productionContext, oddNumberMap, doubleMouldList, productionDay, isDayFinish, cxMachineInfo);
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
        //双模排产量-增加日产能使用量
        addDayCapacityQtyByMould(productionContext, productionDay, productionPlan, updateInfo, doubleMouldList);
        //todo 20260211 更新特殊材料的库存消耗量
        productionContext.updateSpecialMaterialInfoSkuAllocateQty(groupPlanInfo, realDayProductionQty);
    }

    /**
     * 处理成对出现的奇数计划排产
     *
     * @param productionContext 排产上下文
     * @param oddNumberMap      奇数计划
     * @param doubleMouldList   双模
     * @param productionDay     排产日
     * @param isDayFinish       是否排产完毕
     * @param cxMachineInfo     成型机台
     */
    private static void handlerLeftOverOddNumberPlan(TbrProductionContext productionContext, Map<Long, MonthPlanProductionRequirePlanVo> oddNumberMap, List<ProductionMouldInfoVo> doubleMouldList, Integer productionDay, boolean isDayFinish, Set<String> cxMachineInfo) {
        if (CollectionUtils.isEmpty(oddNumberMap) || CollectionUtils.isEmpty(doubleMouldList)) {
            return;
        }
        List<MonthPlanProductionRequirePlanVo> oddNumberPlanList = oddNumberMap.values().stream().collect(Collectors.toList());
        int planSize = oddNumberPlanList.size();
        if ((planSize & BigDecimal.ONE.intValue()) != BigDecimal.ZERO.intValue()) {
            return;
        }
        MonthPlanProductionRequirePlanVo plan = oddNumberPlanList.get(BigDecimal.ZERO.intValue());
        String groupName = plan.getStructureName();
        String materialDesc = plan.getMaterialDesc();
        String mouldInfo = doubleMouldList.stream().map(ProductionMouldInfoVo::getMouldCode).collect(Collectors.joining(StringConstant.COMMA));
        TbrMouldProductionLogRecorder.addMouldProductionLeftOverOddNumberPlan(productionContext, groupName, materialDesc, mouldInfo, productionDay, planSize);
        int roundSize = oddNumberPlanList.size() / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        for (int roundIndex = BigDecimal.ZERO.intValue(); roundIndex < roundSize; roundIndex++) {
            int startIndex = roundIndex * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
            int endIndex = startIndex + BigDecimal.ONE.intValue();
            ProductionMouldInfoVo firstMould = doubleMouldList.get(BigDecimal.ZERO.intValue());
            MonthPlanProductionRequirePlanVo firstPlan = oddNumberPlanList.get(startIndex);
            firstMould.addProductionInfo(productionDay, firstPlan, isDayFinish, ProductionConstant.DOUBLE_MOULD_PRODUCTION, cxMachineInfo);
            ProductionMouldInfoVo secondMould = doubleMouldList.get(BigDecimal.ONE.intValue());
            MonthPlanProductionRequirePlanVo secondPlan = oddNumberPlanList.get(endIndex);
            secondMould.addProductionInfo(productionDay, secondPlan, isDayFinish, ProductionConstant.DOUBLE_MOULD_PRODUCTION, cxMachineInfo);
        }
    }

    /**
     * 补量时，更新模具的日排产信息
     * 更新使用模具的日信息
     * 1、模壳标准的占用
     * 2、模具分配比例占用
     * 3、胶囊卡盘的占用
     * 4、日产能的占用
     *
     * @param productionContext 排产上下文
     * @param groupPlanInfo     排产分组
     * @param doubleMouldList   选中的排产模具
     * @param singlePlan        排产的Sku计划
     * @param updateInfo        日更新信息对象
     */
    private static void updateMouldDayProductionInfo(TbrProductionContext productionContext, ProductionPlanGroupInfo groupPlanInfo, List<ProductionMouldInfoVo> doubleMouldList, MonthPlanProductionRequirePlanVo singlePlan, UpdateDayProductionInfoHelper updateInfo) {
        Integer productionDay = updateInfo.getProductionDay();
        Integer realDayProductionQty = updateInfo.getRealDayProductionQty();
        boolean isDayFinish = updateInfo.isDayFinish();
        Set<String> cxMachineInfo = updateInfo.getUsedCxMachineInfo();
        doubleMouldList.forEach(productionMould -> productionMould.addProductionInfo(productionDay, singlePlan, isDayFinish, realDayProductionQty, cxMachineInfo));
        //模具分配比例控制对象
        MouldAllocationInfoVo mouldAllocationControlInfo = productionContext.getMouldAllocationInfo(singlePlan);
        //胶囊卡盘数量控制对象
        CapsuleChuckInfoVo capsuleChuckInfo = productionContext.getCapsuleChuckInfo(singlePlan);
        doubleMouldList.forEach(singleMould -> {
            //模壳标准使用量 + 1
            updateMouldShellInfoByMould(productionContext, productionDay, singleMould, YesOrNoEnum.YES.getValue());
            //模具分配比例使用量 + 1
            updateMouldAllocationRatioInfoByMould(mouldAllocationControlInfo, productionDay, singleMould, YesOrNoEnum.YES.getValue());
            //胶囊卡盘使用量 + 1
            updateCapsuleChuckInfoByMould(capsuleChuckInfo, productionDay, singleMould, YesOrNoEnum.YES.getValue());
        });
        //双模排产量-增加日产能使用量
        addDayCapacityQtyByMould(productionContext, productionDay, singlePlan, updateInfo, doubleMouldList);
        //todo 20260211 特殊材料的库存消耗量
        productionContext.updateSpecialMaterialInfoSkuAllocateQty(groupPlanInfo, realDayProductionQty);
    }

    /**
     * 更新成型机的硫化组信息
     * 及日排产信息
     *
     * @param cxLhGroup           成型对应的硫化组
     * @param productionSkuInfo   排产Sku信息
     * @param cxMachineInfo       成型机台
     * @param usedMouldSet        使用模具
     * @param dayMaxProductionQty 日最大硫化值
     */
    private static void updateCxMachineLhInfo(CxLhProductionHelper cxLhGroup, MonthPlanProductionRequirePlanVo productionSkuInfo, CxMachineBaseInfoVo cxMachineInfo, Set<String> usedMouldSet, Integer dayMaxProductionQty, UpdateDayProductionInfoHelper updateInfo) {
        Integer productionDay = updateInfo.getProductionDay();
        Integer realDayProductionQty = updateInfo.getRealDayProductionQty();
        //排产信息更新
        String materialDesc = productionSkuInfo.getMaterialDesc();
//        cxLhGroup.setProductionQty(realDayProductionQty);
        cxLhGroup.setProductionDay(productionDay);
//        cxLhGroup.setDayMaxProductionQty(dayMaxProductionQty);
//        cxLhGroup.setMaterialDesc(materialDesc);
//        cxLhGroup.setMaterialCode(productionSkuInfo.getMaterialCode());
//        cxLhGroup.setEmbryoCode(productionSkuInfo.getEmbryoCode());
//        cxLhGroup.setProductionMouldSet(usedMouldSet);
        BeforeSkuProductionInfo beforeSku = BeforeSkuProductionInfo.createByProductionPlan(productionSkuInfo, realDayProductionQty, productionDay, usedMouldSet);
        cxLhGroup.setBeforeSku(beforeSku);
        cxMachineInfo.getCxLhRatioMap().put(cxLhGroup.getLhGroupNo(), cxLhGroup);
        //成型机台-日排产信息
        Integer lossQty = updateInfo.getLossQty();
        SkuDayProductionInfoHelper skuDayProductionInfo = SkuDayProductionInfoHelper.buildEmpty(productionDay, productionSkuInfo, realDayProductionQty, lossQty, usedMouldSet);
        cxMachineInfo.addDayProductionInfo(skuDayProductionInfo);
    }

    /**
     * 更新日产能排产量，以模具+Sku维度
     *
     * @param productionContext 排产上下文
     * @param productionDay     排产日
     * @param productionPlan    排产计划(不关注具体ID)
     * @param updateInfo        排产信息
     * @param doubleMould       使用模具
     */
    private static void addDayCapacityQtyByMould(TbrProductionContext productionContext, Integer productionDay, MonthPlanProductionRequirePlanVo productionPlan, UpdateDayProductionInfoHelper updateInfo, List<ProductionMouldInfoVo> doubleMould) {
        if (null == productionDay || null == productionPlan || CollectionUtils.isEmpty(doubleMould) || null == updateInfo) {
            return;
        }
        Set<String> doubleMouldCode = doubleMould.stream().map(ProductionMouldInfoVo::getMouldCode).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(doubleMouldCode) || ProductionConstant.DOUBLE_MOULD_PRODUCTION != doubleMouldCode.size()) {
            return;
        }
        DayCapacityLimitVo dayCapacityLimit = productionContext.getBaseDataContainer().getDayCapacityLimit();
        if (null == dayCapacityLimit) {
            return;
        }
        dayCapacityLimit.addSkuDayProductionQty(productionContext, productionDay, productionPlan, doubleMould, updateInfo.getRealDayProductionQty(), updateInfo.getLossQty());
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
