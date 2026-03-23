package com.zlt.aps.mp.engine.scheduling.cxcapacity;

import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.*;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductMouldInfoVo;
import com.zlt.aps.mp.engine.enums.ContinueTypeEnum;
import com.zlt.aps.mp.engine.enums.CxMachineLimitTypeEnum;
import com.zlt.aps.mp.engine.enums.ProductionStageEnum;
import com.zlt.aps.mp.engine.logrecorder.TbrBeforeProductionGroupLogRecorder;
import com.zlt.aps.mp.engine.logrecorder.TbrProductionGroupLogRecorder;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 在产机台-产能分配测算
 * TBR 在产机台根据在机结构的续作部分进行各在产机台的收尾点测算
 * PCR 在产机台根据在机寸口的续作部分进行各在产机台的收尾点测算
 * 先TBR业务
 * 1、排产续作部分
 * 1.1、先排产续作Sku部分
 * 1.2、在排产续作Sku的同规格同花纹的高优先级部分
 * 1.3、最后排产续作Sku的同生胎同模具的高优先级部分
 * 模拟排产完毕后，有以下场景:
 * 1、如果在机结构需求估算所需机台数 >= 结构在产机台数，则不需要测算，在产机台直接满台分配
 * 2、如果结构在产机台数 = 1, 也不需要测算，直接按在机结构所需天数进行分配。
 * 3、如果在机结构需求估算所需机台数 < 结构在产机台数，则测试需要进行测试。通过模拟续作Sku的收尾
 * 来确定在产机台的收尾点
 * 1、模拟排产续作SKu -> 续作Sku同规格同花纹 -> 续作Sku共生胎同模具
 * 2、排产后，看最先满足收尾时排产日，即为各在产机台最低要求上机天数(胎胚种类数、硫化机台配比数)
 * 2.1、如果日 = 1，则表示可以在月初就进行释放机台，此时按配比最大的释放，再次获取最先满足收尾是排产日
 * 2.2、如果日 ！= 1，则表示至少都需要生产到到最早收尾的天数。
 * 2.2.1、剩余需分配天数 = 需求天数 - (在产机台数 * 最低排产天数)
 *
 * @author ZLT
 * @date 20251227
 */
@Slf4j
@Component
public class ProductionCxMachineCalculationHandler {

    /**
     * 对在机分组进行在机机台产能分配
     * 通过模拟排产其续作部分来确定
     * TBR 分组为结构
     * PCR 分组为英寸
     *
     * @param context          排产上下文
     * @param allGroupPlanInfo 所有分组计划信息集合对象
     * @param allContinueInfo  所有在产分组的续作Sku信息集合对象
     * @return
     */
    public List<CxMachineAllocationPlanHelper> allocationContinueAndProductionContinue(Context context, Map<String, ProductionPlanGroupInfo> allGroupPlanInfo, Map<String, CxContinueInfoHelper> allContinueInfo) {
        if (CollectionUtils.isEmpty(allContinueInfo)) {
            log.info(TbrProductionGroupLogRecorder.addContinueSkuNoContinueGroupProductionLog(context));
            return Collections.emptyList();
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        List<CxMachineAllocationPlanHelper> allAllocationResult = new ArrayList<>();
        //按在机结构分组--排产在机结构的续作Sku在 在产机台的排产，并得到各在产机台的产能分配
        allContinueInfo.forEach((structureName, cxContinueInfo) -> {
            ProductionPlanGroupInfo groupPlan = allGroupPlanInfo.get(structureName);
            if (null == groupPlan) {
                return;
            }
            List<CxMachineAllocationPlanHelper> singleGroupAllocationResult = allocationProductionCxMachineAndProductionContinue(productionContext, groupPlan, cxContinueInfo);
            if (CollectionUtils.isEmpty(singleGroupAllocationResult)) {
                log.info(TbrBeforeProductionGroupLogRecorder.addContinueGroupNoOnLineMachineLog(context, structureName, null, null));
                return;
            }
            allAllocationResult.addAll(singleGroupAllocationResult);
        });
        if (CollectionUtils.isEmpty(allAllocationResult)) {
            return Collections.emptyList();
        }
        return allAllocationResult;
    }

    /**
     * 对在机结构分配成型产能分配
     * 在机结构从现有的在产机台中，根据粗算所需机台，分配各在产机台的产能
     * 1、先进行续作高优先级部分排产
     * 2、构建在机结构分配在产机台的产能
     * 2.1、如果估算所需机台数>=在产机台数，则直接分配
     * 2.2、如果估算所需机台数<在产机台数，则需要根据续作高优先级部分模拟排产的结果
     * 在满足最低分配天数的条件下进行在产机台的分配
     *
     * @param context           排产上下文
     * @param groupPlanInfo     分组计划信息对象
     * @param groupContinueInfo 分组计划对应的续作Sku信息
     */
    private List<CxMachineAllocationPlanHelper> allocationProductionCxMachineAndProductionContinue(Context context, ProductionPlanGroupInfo groupPlanInfo, CxContinueInfoHelper groupContinueInfo) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Set<String> productionCxMachineCodeSet = groupContinueInfo.getCxMachineCodeSet();
        if (CollectionUtils.isEmpty(productionCxMachineCodeSet)) {
            return Collections.emptyList();
        }
        /**
         * 2、在产机台的分配
         * 2.1、在产机台数 = 1时，直接分配多少就是多少
         * 2.2、在产分组需求粗算所需机台数 > 在产机台数，则表示还需增机台，则在产机台直接按满产算
         * 2.3、如果=，则表示刚好，也直接按满产算
         * 2.4、在产分组需求粗算所需机台数 < 在产机台数
         *
         */
        //粗算得到的机台
        BigDecimal needCount = groupPlanInfo.getNeedCxCapacityMachineCount();
        Integer productionCount = productionCxMachineCodeSet.size();
        //在产机台数<=在机结构所需机台数，则此时不用测算，机台直接分配
        if (needCount.compareTo(BigDecimal.valueOf(productionCount)) >= BigDecimal.ZERO.intValue()) {
            //在机分组还需要增加机台
            List<CxMachineAllocationPlanHelper> buildResult = buildAllProductionCxMachineResult(context, groupPlanInfo, groupContinueInfo);
            if (needCount.compareTo(BigDecimal.valueOf(productionCount)) > BigDecimal.ZERO.intValue()) {
                return buildResult;
            }
            //相等 20260109 标记结构成型产能分配完成
            groupPlanInfo.setIsAllocationFinish(YesOrNoEnum.YES.getValue());
            return buildResult;
        }
        //如果在产机台数只有一台的情形下，直接分配
        if (BigDecimal.ONE.intValue() == productionCount) {
            List<CxMachineAllocationPlanHelper> buildResult = buildNeedProductionCxMachineResult(context, groupPlanInfo, groupContinueInfo);
            if (groupPlanInfo.getLeftOverNeedAllocationDays() <= BigDecimal.ZERO.intValue()) {
                groupPlanInfo.setIsAllocationFinish(YesOrNoEnum.YES.getValue());
            }
            return buildResult;
        }
        //1、排产续作部分（续作Sku高优先级排产、同规格同花纹高优级排产、同生胎同模具高优级排产）
        productionContinue(productionContext, groupPlanInfo, groupContinueInfo);
        //2、在产机台数有多台情形下，最后确定在产机台各自收尾时间点及分配
        return buildContinueCxMachineAllocationResult(context, groupPlanInfo, groupContinueInfo);
    }

    /**
     * 排产续作部分-模拟在机结构的续作Sku高优先级部分，用以确定在产机台的分配信息
     * 1、首先排产续作Sku的高优先级部分
     * 2、排产续作Sku的同规格同花纹的高优先级部分
     * 3、最后排产续作Sku的同生胎同模具的高优先级部分
     *
     * @param context           排产上下文
     * @param groupPlanInfo     分组计划信息对象
     * @param groupContinueInfo 分组计划-续作Sku信息对象
     */
    private void productionContinue(Context context, ProductionPlanGroupInfo groupPlanInfo, CxContinueInfoHelper groupContinueInfo) {
        Map<String, CxContinueSkuInfoHelper> continueSkuInfoMap = groupContinueInfo.getContinueSkuMouldNumberMap();
        if (CollectionUtils.isEmpty(continueSkuInfoMap)) {
            //todo 记录日志
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //修正续作Sku的模具数
        continueSkuInfoMap.forEach((materialDesc, cxContinueSkuInfo) -> {
            List<MonthPlanProductMouldInfoVo> mouldList = productionContext.getBaseDataContainer().getSkuMouldRelationMap().get(materialDesc);
            if (CollectionUtils.isEmpty(mouldList)) {
                //todo 记录日志-sku没有模具
                return;
            }
            Integer mouldNumber = cxContinueSkuInfo.getMouldNumber();
            Integer maxNumber = mouldList.size();
            //超出可用模具
            if (mouldNumber > maxNumber) {
                cxContinueSkuInfo.setMouldNumber(maxNumber);
            }
        });
        //1、先使用续作Sku的高优先级部分进行模拟排产
        CxContinueProductionHandler.productionContinueSku(productionContext, ProductionStageEnum.CALCULATION_STAGE, groupPlanInfo, continueSkuInfoMap);
        //2、接着进行同规格同花纹的续作高优先级部分进行模拟排产
        Integer monthDays = context.getMonthDays();
        CxContinueProductionHandler.productionContinueByType(context, ProductionStageEnum.CALCULATION_STAGE, groupPlanInfo, ContinueTypeEnum.SAME_SPECIFICATIONS_PATTERN, monthDays, continueSkuInfoMap);
        //3、接着进行共生胎，同模具的续作高优级部分进行模拟排产
        CxContinueProductionHandler.productionContinueByType(context, ProductionStageEnum.CALCULATION_STAGE, groupPlanInfo, ContinueTypeEnum.SAME_EMBRYO_CODE_SHARE_MOULD, monthDays, continueSkuInfoMap);
    }

    /**
     * 构建在产机台分配估算的分组计划
     *
     * @param groupPlanInfo     分组计划信息
     * @param groupContinueInfo 分组计划对应的在产信息
     * @return
     */
    private List<CxMachineAllocationPlanHelper> buildNeedProductionCxMachineResult(Context context, ProductionPlanGroupInfo groupPlanInfo, CxContinueInfoHelper groupContinueInfo) {
        Set<String> productionCxMachineCodeSet = groupContinueInfo.getCxMachineCodeSet();
        if (CollectionUtils.isEmpty(productionCxMachineCodeSet) || productionCxMachineCodeSet.size() > BigDecimal.ONE.intValue()) {
            return Collections.emptyList();
        }
        List<CxMachineAllocationPlanHelper> allocationList = new ArrayList<>();
        productionCxMachineCodeSet.forEach(cxMachineCode -> {
            CxMachineAllocationPlanHelper allocationHelper = buildAllocationProductionCxMachineResult(context, groupPlanInfo, groupContinueInfo, cxMachineCode, groupPlanInfo.getTheoryDays());
            allocationList.add(allocationHelper);
        });
        return allocationList;
    }

    /**
     * 构建所有在产机台产能全部分配给分组计划
     *
     * @param groupPlanInfo     分组计划信息
     * @param groupContinueInfo 分组计划对应的在产信息
     * @return
     */
    private List<CxMachineAllocationPlanHelper> buildAllProductionCxMachineResult(Context context, ProductionPlanGroupInfo groupPlanInfo, CxContinueInfoHelper groupContinueInfo) {
        Set<String> productionCxMachineCodeSet = groupContinueInfo.getCxMachineCodeSet();
        if (CollectionUtils.isEmpty(productionCxMachineCodeSet)) {
            return Collections.emptyList();
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, CxMachineBaseInfoVo> allCxMachineMap = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        List<CxMachineAllocationPlanHelper> allocationList = new ArrayList<>();
        productionCxMachineCodeSet.forEach(cxMachineCode -> {
            CxMachineBaseInfoVo cxMachineInfo = allCxMachineMap.get(cxMachineCode);
            Integer allocationDays = cxMachineInfo.getMaxProductionDays();
            allocationList.add(buildAllocationProductionCxMachineResult(context, groupPlanInfo, groupContinueInfo, cxMachineCode, allocationDays));
        });
        return allocationList;
    }

    /**
     * 构建在产机台分配给分组计划产能allocationDays
     *
     * @param groupPlanInfo     分组计划信息
     * @param groupContinueInfo 分组计划对应的在产信息
     * @param cxMachineCode     机台
     * @param allocationDays    分配天数
     * @return
     */
    private CxMachineAllocationPlanHelper buildAllocationProductionCxMachineResult(Context context, ProductionPlanGroupInfo groupPlanInfo, CxContinueInfoHelper groupContinueInfo, String cxMachineCode, Integer allocationDays) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Integer monthDays = context.getMonthDays();
        Map<String, CxMachineBaseInfoVo> allCxMachineMap = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        List<ProductGroupCxCapacityInfo> cxCapacityInfoList = groupContinueInfo.getCxCapacityInfoList();
        Map<String, ProductGroupCxCapacityInfo> groupCxCapacityInfoMap = cxCapacityInfoList.stream().collect(Collectors.toMap(ProductGroupCxCapacityInfo::getCxMachineCode, Function.identity()));
        CxMachineBaseInfoVo cxMachineInfo = allCxMachineMap.get(cxMachineCode);
        //20260131 分配天数，成型机剩余天数与分配天数取最小
        Integer remainingDays = cxMachineInfo.getRemainingDays();
        allocationDays = Math.min(remainingDays, allocationDays);
        //更新分组剩余分配量-更新特殊材料库存
        productionContext.updateSpecialMaterialInfoMap(groupPlanInfo, allocationDays);
        groupPlanInfo.updateLeftOverNeedAllocationDays(allocationDays);
        ProductGroupCxCapacityInfo capacityInfo = groupCxCapacityInfoMap.get(cxMachineCode);
        CxMachineAllocationPlanHelper helper = CxCapacityAllocationHandler.createAllocationPlanHelper(cxMachineInfo, capacityInfo, groupPlanInfo, groupContinueInfo.getContinueSkuMouldNumberMap(), allocationDays, BigDecimal.ONE.intValue(), monthDays);
        //机台增加分配信息
        cxMachineInfo.addAllocationPlanInfo(context, helper);
        return helper;
    }

    /**
     * 在续作模拟排产完毕后，构建在产机台的产能分配信息
     * 优先按配比多，机台编号大的先释放原则
     *
     * @param context           排产上下文
     * @param groupPlanInfo     分组排产计划信息对象
     * @param groupContinueInfo 分组排产的续作信息对象
     * @return
     */
    private List<CxMachineAllocationPlanHelper> buildContinueCxMachineAllocationResult(Context context, ProductionPlanGroupInfo groupPlanInfo, CxContinueInfoHelper groupContinueInfo) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //根据续作模拟排产信息，构建在产机台的分配，分配的机台数及最低分配的生产天数
        Set<String> productionCxMachineCodeSet = groupContinueInfo.getCxMachineCodeSet();
        Integer productionCount = productionCxMachineCodeSet.size();
        List<ProductGroupCxCapacityInfo> cxCapacityInfoList = groupContinueInfo.getCxCapacityInfoList();
        BigDecimal needCount = groupPlanInfo.getNeedCxCapacityMachineCount();
        Integer needWholeCount = needCount.setScale(BigDecimal.ZERO.intValue(), RoundingMode.UP).intValue();
        CxContinueMachineReleaseHelper initReleaseInfo = new CxContinueMachineReleaseHelper(BigDecimal.ZERO.intValue(), BigDecimal.ZERO.intValue());
        getContinueMachineRelease(context, initReleaseInfo, needWholeCount, productionCount, groupPlanInfo, groupContinueInfo);
        //根据分配信息，优先释放配比大的，成型编号大的
        List<ProductGroupCxCapacityInfo> effectiveList = getKeepCxMachineList(cxCapacityInfoList, initReleaseInfo);
        Integer minAllocationDays = initReleaseInfo.getEarliestConclusionDay();
        Integer sumDays = groupPlanInfo.getTheoryDays();
        Map<String, CxMachineBaseInfoVo> allCxMachineInfoMap = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        Map<String, CxContinueSkuInfoHelper> continueSkuMap = groupContinueInfo.getContinueSkuMouldNumberMap();
        Integer monthDays = productionContext.getMonthDays();
        List<CxMachineAllocationPlanHelper> allocationList = new ArrayList<>();
        Integer leftOverSplitDays;
        //如果是月初，则直接前面的整月分配，后续的取余
        if (ProductionConstant.MONTH_START_DAY.equals(minAllocationDays)) {
            leftOverSplitDays = sumDays;
            minAllocationDays = BigDecimal.ZERO.intValue();
        } else {
            //至少每台都需要分配最低天数
            if(null == minAllocationDays){
                minAllocationDays = context.getMaxProductionDays();
            }
            Integer sumMinAllocationDays = minAllocationDays * effectiveList.size();
            leftOverSplitDays = sumDays - sumMinAllocationDays;
            if (leftOverSplitDays < BigDecimal.ZERO.intValue()) {
                leftOverSplitDays = BigDecimal.ZERO.intValue();
            }
        }
        for (ProductGroupCxCapacityInfo cxCapacityInfo : effectiveList) {
            CxMachineBaseInfoVo cxMachineInfo = allCxMachineInfoMap.get(cxCapacityInfo.getCxMachineCode());
            Integer remainingDays = cxMachineInfo.getRemainingDays() - minAllocationDays;
            Integer leftOverAllocationDay = Math.min(leftOverSplitDays, remainingDays);
            Integer allocationDay = leftOverAllocationDay + minAllocationDays;
            if (allocationDay <= BigDecimal.ZERO.intValue()) {
                break;
            }
            // 更新特殊材料库存
            productionContext.updateSpecialMaterialInfoMap(groupPlanInfo, allocationDay);
            groupPlanInfo.updateLeftOverNeedAllocationDays(allocationDay);
            CxMachineAllocationPlanHelper helper = CxCapacityAllocationHandler.createAllocationPlanHelper(cxMachineInfo, cxCapacityInfo, groupPlanInfo, continueSkuMap, allocationDay, ProductionConstant.MONTH_START_DAY, monthDays);
            cxMachineInfo.addAllocationPlanInfo(context, helper);
            allocationList.add(helper);
            leftOverSplitDays = leftOverSplitDays - leftOverAllocationDay;
            if (leftOverSplitDays <= BigDecimal.ZERO.intValue()) {
                leftOverSplitDays = BigDecimal.ZERO.intValue();
            }
        }
        //20260109 标记分组计划分配完毕
        groupPlanInfo.setIsAllocationFinish(YesOrNoEnum.YES.getValue());
        return allocationList;
    }

    /**
     * 获取在机结构，可释放机台数和续作收尾时间点
     *
     * @param context              排产上下文
     * @param release              续作释放信息机台
     * @param needWholeCount       在机结构估算需要的整数机台
     * @param continueMachineCount 在机结构在产机台数
     * @param groupPlanInfo        在机结构计划信息
     * @param groupContinueInfo    在机结构续作信息
     * @return
     */
    private void getContinueMachineRelease(Context context, CxContinueMachineReleaseHelper release, Integer needWholeCount, Integer continueMachineCount, ProductionPlanGroupInfo groupPlanInfo, CxContinueInfoHelper groupContinueInfo) {
        //相等，则表示在产机台不能释放
        if (needWholeCount.equals(continueMachineCount)) {
            handlerSameWholeCxMachine(context, release, groupPlanInfo, groupContinueInfo);
            return;
        }
        Integer releaseCount = release.getReleaseMachineCount();
        release.setDeductionMachineCount(releaseCount);
        //需要机台数 + 释放机台数 = 在产机台数
        if (needWholeCount + releaseCount == continueMachineCount) {
            //能进入此处，则前面一定是月初释放，则继续尝试再减一台：逐步释放
            Integer deductionMachineCount = release.getDeductionMachineCount() + BigDecimal.ONE.intValue();
            Integer deductionDay = getMinDeductionMachineDay(deductionMachineCount, groupPlanInfo, groupContinueInfo);
            if (null == deductionDay) {
                //表示不能再减，直接返回
                return;
            }
            //释放台数
            release.setReleaseMachineCount(deductionMachineCount);
            release.setEarliestConclusionDay(deductionDay);
            return;
        }
        Integer deductionMachineCount = release.getDeductionMachineCount();
        //先尝试减一台：+1
        deductionMachineCount = deductionMachineCount + BigDecimal.ONE.intValue();
        Integer deductionDay = getMinDeductionMachineDay(deductionMachineCount, groupPlanInfo, groupContinueInfo);
        //月初可释放
        if (null != deductionDay && context.isCycleFirstProductionDay(deductionDay)) {
            release.setReleaseMachineCount(deductionMachineCount);
            release.setEarliestConclusionDay(deductionDay);
            getContinueMachineRelease(context, release, needWholeCount, continueMachineCount, groupPlanInfo, groupContinueInfo);
            return;
        }
        //不能减，则再加回，看最先收尾时间点
        if (null == deductionDay) {
            deductionMachineCount = deductionMachineCount - BigDecimal.ONE.intValue();
            if (deductionMachineCount < BigDecimal.ZERO.intValue()) {
                deductionMachineCount = BigDecimal.ZERO.intValue();
            }
            deductionDay = getMinDeductionMachineDay(deductionMachineCount, groupPlanInfo, groupContinueInfo);
        }
        release.setReleaseMachineCount(deductionMachineCount);
        release.setEarliestConclusionDay(deductionDay);
        return;
    }

    /**
     * 处理在产机台数与所需机台整数台相等的情形，此时表示机台数不能释放，看各台最小的分配天数
     *
     * @param context           排产上下文
     * @param release           释放机台信息
     * @param groupPlanInfo     分组计划
     * @param groupContinueInfo 续作信息
     */
    private void handlerSameWholeCxMachine(Context context, CxContinueMachineReleaseHelper release, ProductionPlanGroupInfo groupPlanInfo, CxContinueInfoHelper groupContinueInfo) {
        //先不扣减
        Integer deductionMachineCount = BigDecimal.ZERO.intValue();
        Integer deductionDay = getMinDeductionMachineDay(deductionMachineCount, groupPlanInfo, groupContinueInfo);
        if (null != deductionDay && context.isCycleFirstProductionDay(deductionDay)) {
            //理论会出现在第一天，此时尝试扣减一台，看最低要求生产天数
            deductionMachineCount = deductionMachineCount + BigDecimal.ONE.intValue();
            Integer newDeductionDay = getMinDeductionMachineDay(deductionMachineCount, groupPlanInfo, groupContinueInfo);
            if (null != newDeductionDay) {
                deductionDay = newDeductionDay;
            }
        }
        if (null == deductionDay) {
            deductionDay = context.getMaxProductionDays();
        }
        //不释放
        release.setReleaseMachineCount(BigDecimal.ZERO.intValue());
        release.setEarliestConclusionDay(deductionDay);
    }

    /**
     * 根据释放机台情况，按成型硫化配比大->机台编号大的优先释放原则，
     * 得到需要保留的机台集合
     *
     * @param cxCapacityInfoList 在机结构在产机台集合
     * @param initReleaseInfo    可释放信息
     * @return
     */
    private List<ProductGroupCxCapacityInfo> getKeepCxMachineList(List<ProductGroupCxCapacityInfo> cxCapacityInfoList, CxContinueMachineReleaseHelper initReleaseInfo) {
        if (CollectionUtils.isEmpty(cxCapacityInfoList)) {
            return Collections.emptyList();
        }
        if (null == initReleaseInfo) {
            return cxCapacityInfoList;
        }
        Integer releaseCount = initReleaseInfo.getReleaseMachineCount();
        if (releaseCount <= BigDecimal.ZERO.intValue()) {
            return cxCapacityInfoList;
        }
        //根据分配信息，优先释放配比大的，成型编号大的
        cxCapacityInfoList.sort(Comparator.comparing(ProductGroupCxCapacityInfo::getMaxLhMachineCount)
                .thenComparing(ProductGroupCxCapacityInfo::getCxMachineCode));
        Integer minAllocationDays = initReleaseInfo.getEarliestConclusionDay();
        //月初就可释放，则直接释放对应
        if (ProductionConstant.MONTH_START_DAY.equals(minAllocationDays)) {
            Integer keepCount = cxCapacityInfoList.size() - releaseCount;
            return cxCapacityInfoList.subList(BigDecimal.ZERO.intValue(), keepCount);
        }
        //月初不能释放，则需要看情况，如果releaseCount>1则表示月初可释放 releaseCount - 1台，其它台最少需要分配minAllocationDays
        if (releaseCount > BigDecimal.ONE.intValue()) {
            Integer keepCount = cxCapacityInfoList.size() - releaseCount + BigDecimal.ONE.intValue();
            return cxCapacityInfoList.subList(BigDecimal.ZERO.intValue(), keepCount);
        }
        return cxCapacityInfoList;
    }

    /**
     * 获取可减机台的最早日期
     *
     * @param deductionMachineCount 减机台数
     * @param groupPlanInfo         分组计划信息对象
     * @param groupContinueInfo     续作Sku信息对象
     * @return
     */
    private Integer getMinDeductionMachineDay(Integer deductionMachineCount, ProductionPlanGroupInfo groupPlanInfo, CxContinueInfoHelper groupContinueInfo) {
        List<GroupDayProductionSummaryHelper> summaryList = groupPlanInfo.getGroupProductionSummary();
        Integer maxEmbryoCount = groupContinueInfo.getDeductionCountLimitValue(deductionMachineCount, CxMachineLimitTypeEnum.MAX_EMBRYO_SIZE);
        Integer maxLhGroupCount = groupContinueInfo.getDeductionCountLimitValue(deductionMachineCount, CxMachineLimitTypeEnum.MAX_LH_COUNT);
        //提取胎胚种类数，硫化机台数都符合的的记录
        List<GroupDayProductionSummaryHelper> canDeductionList = summaryList.stream().filter(summary -> summary.isMatchDeductionMachine(maxEmbryoCount, maxLhGroupCount)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(canDeductionList)) {
            return null;
        }
        canDeductionList.sort(Comparator.comparing(GroupDayProductionSummaryHelper::getProductionDay));
        Integer startDay = canDeductionList.get(BigDecimal.ZERO.intValue()).getProductionDay();
        return startDay;
    }

}
