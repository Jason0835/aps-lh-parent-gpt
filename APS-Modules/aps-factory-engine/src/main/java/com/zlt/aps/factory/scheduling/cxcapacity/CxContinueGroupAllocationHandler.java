package com.zlt.aps.factory.scheduling.cxcapacity;

import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.deduct.DeductMouldScheduler;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.*;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductMouldInfoVo;
import com.zlt.aps.factory.domain.vo.MouldShellBaseInfoVo;
import com.zlt.aps.factory.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.factory.enums.ContinueTypeEnum;
import com.zlt.aps.factory.enums.CxMachineLimitTypeEnum;
import com.zlt.aps.factory.handler.CxLhMouldProductionCalculator;
import com.zlt.aps.factory.handler.SkuMouldSelector;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import com.zlt.aps.monthplan.api.domain.deduct.DailyScheduleVo;
import com.zlt.aps.monthplan.api.domain.deduct.DeductMouldVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 成型在机分组分配
 * TBR 在机结构成型机台产能分配
 * PCR 在机寸口成型机台产品分配
 *
 * @author ZLT
 * @date 20251227
 */
@Slf4j
public class CxContinueGroupAllocationHandler {

    /**
     * 对在机分组进行在机机台产能分配，并排产其续作部分
     * TBR 分组为结构
     * PCR 分组为英寸
     *
     * @param context          排产上下文
     * @param allGroupPlanInfo 所有分组计划信息集合对象
     * @param allContinueInfo  所有在产分组的续作Sku信息集合对象
     * @return
     */
    public static List<CxMachineAllocationPlanHelper> allocationContinueAndProductionContinue(Context context, Map<String, ProductionPlanGroupInfo> allGroupPlanInfo, Map<String, CxContinueInfoHelper> allContinueInfo) {
        if (CollectionUtils.isEmpty(allContinueInfo)) {
            log.info(TbrProductionGroupLogRecorder.addContinueSkuNoContinueGroupProductionLog(context));
            return Collections.emptyList();
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, MouldShellBaseInfoVo> mouldShellMap = productionContext.getBaseDataContainer().getMouldShellMap();
        List<CxMachineAllocationPlanHelper> allAllocationResult = new ArrayList<>();
        allContinueInfo.forEach((structureName, cxContinueInfo) -> {
            ProductionPlanGroupInfo groupPlan = allGroupPlanInfo.get(structureName);
            if (null == groupPlan) {
                return;
            }
            List<CxMachineAllocationPlanHelper> singleGroupAllocationResult = allocationProductionCxMachineAndProductionContinue(productionContext, groupPlan, cxContinueInfo, mouldShellMap);
            if (CollectionUtils.isEmpty(singleGroupAllocationResult)) {
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
     * @param mouldShellMap     模壳信息
     */
    private static List<CxMachineAllocationPlanHelper> allocationProductionCxMachineAndProductionContinue(Context context, ProductionPlanGroupInfo groupPlanInfo, CxContinueInfoHelper groupContinueInfo, Map<String, MouldShellBaseInfoVo> mouldShellMap) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //粗算得到的机台
        BigDecimal needCount = groupPlanInfo.getNeedCxCapacityMachineCount();
        Set<String> productionCxMachineCodeSet = groupContinueInfo.getCxMachineCodeSet();
        Integer productionCount = productionCxMachineCodeSet.size();
        //1、排产续作部分（续作Sku高优先级排产、同规格同花纹高优级排产、同生胎同模具高优级排产）
        productionContinue(productionContext, groupPlanInfo, groupContinueInfo, mouldShellMap);
        /**
         * 2、在产机台的分配
         * 2.1、在产分组需求粗算所需机台数 > 在产机台数，则表示还需增机台，则在产机台直接按满产算
         * 2.2、如果=，则表示刚好，也直接按满产算
         * 2.3、在产分组需求粗算所需机台数 < 在产机台数
         *
         */
        if (needCount.compareTo(BigDecimal.valueOf(productionCount)) >= BigDecimal.ZERO.intValue()) {
            return buildAllProductionCxMachineResult(context, groupPlanInfo, groupContinueInfo);
        }
        //2、最后确定在产机台各自收尾时间点及分配
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
     * @param mouldShellMap     模壳信息
     */
    private static void productionContinue(Context context, ProductionPlanGroupInfo groupPlanInfo, CxContinueInfoHelper groupContinueInfo, Map<String, MouldShellBaseInfoVo> mouldShellMap) {
//        Map<Integer, CxLhProductionHelper> cxLhRatioMap = groupPlanInfo.getCxLhRatioMap();
//        if (CollectionUtils.isEmpty(cxLhRatioMap)) {
//            //todo 记录日志
//            return;
//        }
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
        productionContinueSku(productionContext, groupPlanInfo, continueSkuInfoMap);
        //2、接着进行同规格同花纹的续作高优先级部分进行模拟排产
        Integer monthDays = context.getMonthDays();
        CxContinueProductionHandler.productionContinueByType(context, groupPlanInfo, ContinueTypeEnum.SAME_SPECIFICATIONS_PATTERN, monthDays, continueSkuInfoMap, mouldShellMap);
        //3、接着进行共生胎，同模具的续作高优级部分进行模拟排产
        CxContinueProductionHandler.productionContinueByType(context, groupPlanInfo, ContinueTypeEnum.SAME_EMBRYO_CODE_SHARE_MOULD, monthDays, continueSkuInfoMap, mouldShellMap);
    }

    /**
     * 构建所有在产机台分配给分组计划
     *
     * @param groupPlanInfo     分组计划信息
     * @param groupContinueInfo 分组计划对应的在产信息
     * @return
     */
    private static List<CxMachineAllocationPlanHelper> buildAllProductionCxMachineResult(Context context, ProductionPlanGroupInfo groupPlanInfo, CxContinueInfoHelper groupContinueInfo) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Set<String> productionCxMachineCodeSet = groupContinueInfo.getCxMachineCodeSet();
        Integer monthDays = context.getMonthDays();
        Map<String, CxMachineBaseInfoVo> allCxMachineMap = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        List<ProductGroupCxCapacityInfo> cxCapacityInfoList = groupContinueInfo.getCxCapacityInfoList();
        Map<String, ProductGroupCxCapacityInfo> groupCxCapacityInfoMap = cxCapacityInfoList.stream().collect(Collectors.toMap(ProductGroupCxCapacityInfo::getCxMachineCode, Function.identity()));
        List<CxMachineAllocationPlanHelper> allocationList = new ArrayList<>();
        productionCxMachineCodeSet.forEach(cxMachineCode -> {
            CxMachineBaseInfoVo cxMachineInfo = allCxMachineMap.get(cxMachineCode);
            Integer allocationDays = cxMachineInfo.getMaxProductionDays();
            cxMachineInfo.setRemainingDays(BigDecimal.ZERO.intValue());
            ProductGroupCxCapacityInfo capacityInfo = groupCxCapacityInfoMap.get(cxMachineCode);
            CxMachineAllocationPlanHelper helper = new CxMachineAllocationPlanHelper(cxMachineInfo.getCxMachineCode(), groupPlanInfo, capacityInfo, groupContinueInfo.getContinueSkuMouldNumberMap(), allocationDays, BigDecimal.ONE.intValue(), monthDays);
            cxMachineInfo.addAllocationPlanInfo(helper);
            allocationList.add(helper);
        });
        return allocationList;
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
    private static List<CxMachineAllocationPlanHelper> buildContinueCxMachineAllocationResult(Context context, ProductionPlanGroupInfo groupPlanInfo, CxContinueInfoHelper groupContinueInfo) {
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
        cxCapacityInfoList.sort(Comparator.comparing(ProductGroupCxCapacityInfo::getMaxLhMachineCount)
                .thenComparing(ProductGroupCxCapacityInfo::getCxMachineCode));
        Integer releaseCount = initReleaseInfo.getReleaseMachineCount();
        List<ProductGroupCxCapacityInfo> effectiveList;
        if (null != releaseCount && releaseCount > BigDecimal.ZERO.intValue() && releaseCount < productionCount) {
            int endIndex = cxCapacityInfoList.size() - releaseCount - BigDecimal.ONE.intValue();
            effectiveList = cxCapacityInfoList.subList(BigDecimal.ZERO.intValue(), endIndex);
        } else {
            effectiveList = cxCapacityInfoList;
        }
        Integer minAllocationDays = initReleaseInfo.getEarliestConclusionDay();
        Integer sumDays = groupPlanInfo.getTheoryDays();
        Map<String, CxMachineBaseInfoVo> allCxMachineInfoMap = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        Map<String, CxContinueSkuInfoHelper> continueSkuMap = groupContinueInfo.getContinueSkuMouldNumberMap();
        Integer monthDays = productionContext.getMonthDays();
        List<CxMachineAllocationPlanHelper> allocationList = new ArrayList<>();
        //如果是月初，则直接前面的整月分配，后续的取余
        if (ProductionConstant.MONTH_START_DAY.equals(minAllocationDays)) {
            for (ProductGroupCxCapacityInfo cxCapacityInfo : effectiveList) {
                CxMachineBaseInfoVo cxMachineInfo = allCxMachineInfoMap.get(cxCapacityInfo.getCxMachineCode());
                Integer remainingDays = cxMachineInfo.getRemainingDays();
                Integer allocationDay = Math.min(sumDays, remainingDays);
                cxMachineInfo.setRemainingDays(remainingDays - allocationDay);
                CxMachineAllocationPlanHelper helper = CxCapacityAllocationHandler.createAllocationPlanHelper(cxMachineInfo, cxCapacityInfo, groupPlanInfo, continueSkuMap, allocationDay, ProductionConstant.MONTH_START_DAY, monthDays);
                cxMachineInfo.addAllocationPlanInfo(helper);
                allocationList.add(helper);
                sumDays = sumDays - allocationDay;
            }
            return allocationList;
        }
        //至少每台都需要分配最低天数
        Integer sumMinAllocationDays = minAllocationDays * effectiveList.size();
        Integer leftOverSplitDays = sumDays - sumMinAllocationDays;
        for (ProductGroupCxCapacityInfo cxCapacityInfo : effectiveList) {
            CxMachineBaseInfoVo cxMachineInfo = allCxMachineInfoMap.get(cxCapacityInfo.getCxMachineCode());
            Integer remainingDays = cxMachineInfo.getRemainingDays() - minAllocationDays;
            Integer leftOverAllocationDay = Math.min(leftOverSplitDays, remainingDays);
            Integer allocationDay = leftOverAllocationDay + minAllocationDays;
            cxMachineInfo.setRemainingDays(remainingDays - leftOverAllocationDay);
            CxMachineAllocationPlanHelper helper = CxCapacityAllocationHandler.createAllocationPlanHelper(cxMachineInfo, cxCapacityInfo, groupPlanInfo, continueSkuMap, allocationDay, ProductionConstant.MONTH_START_DAY, monthDays);
            cxMachineInfo.addAllocationPlanInfo(helper);
            allocationList.add(helper);
            leftOverSplitDays = leftOverSplitDays - leftOverAllocationDay;
        }
        return allocationList;
    }

    /**
     * 续作Sku使用续作模具排产
     * 可能需要进行降膜排产
     *
     * @param context            排产上下文
     * @param groupPlanInfo      分组计划信息对象
     * @param continueSkuInfoMap 续作Sku信息
     */
    static void productionContinueSku(TbrProductionContext context, ProductionPlanGroupInfo groupPlanInfo, Map<String, CxContinueSkuInfoHelper> continueSkuInfoMap) {
        Set<Integer> stopDays = context.getStopDays();
        Integer monthDays = context.getMonthDays();
        ProductionCapacityParamConfiguration paramConfiguration = context.getBaseDataContainer().getParamConfiguration();
        //续作Sku轮询排产
        String groupName = groupPlanInfo.getGroupName();
        continueSkuInfoMap.forEach((materialDesc, cxContinueSkuInfo) -> {
            log.info(TbrMouldProductionLogRecorder.addContinueSkuStartMouldLog(context, groupName, materialDesc));
            if (!cxContinueSkuInfo.hasProduction()) {
                log.info(TbrMouldProductionLogRecorder.addContinueSkuNoProductionQtyLog(context, groupName, materialDesc));
                return;
            }
            Integer maxDayQty = cxContinueSkuInfo.getMaxDaySingleLhMachineQty();
            //1、降膜排产
            DeductMouldVo deductMould = DeductMouldScheduler.createDeductMouldBySku(monthDays, stopDays, new HashSet<>(), paramConfiguration, cxContinueSkuInfo);
            List<DailyScheduleVo> resultList = DeductMouldScheduler.scheduleProduction(deductMould);
            //分配结果
            if (CollectionUtils.isEmpty(resultList)) {
                //记录日志
                log.info(TbrMouldProductionLogRecorder.addContinueSkuNoProductionResultLog(context, groupName, materialDesc));
                return;
            }
            //挑选的模具 本次使用最多模具数，不一定与续作模具数相等，但不会超
            Integer maxMouldNumber = resultList.stream().mapToInt(DailyScheduleVo::getSkuMachines).max().getAsInt() * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
            List<ProductionMouldInfoVo> selectMouldList = SkuMouldSelector.getContinueSkuMouldNumberInit(context, materialDesc, maxMouldNumber);
            //2、将排产结果，逐日分配到模具上，按排产日由小到大排序
            resultList.sort(Comparator.comparing(DailyScheduleVo::getScheduleDate));
            resultList.forEach(dailySchedule -> {
                //使用的硫化机台数-即模具数
                Integer lhMachineCount = dailySchedule.getSkuMachines();
                Integer sumProductionQty = dailySchedule.getSkuQuantity();
                Integer productionDay = dailySchedule.getScheduleDate();
                //按双模放置
                for (int lhGroupNo = BigDecimal.ONE.intValue(); lhGroupNo <= lhMachineCount; lhGroupNo++) {
                    Integer productionQty = Math.min(sumProductionQty, maxDayQty);
                    Integer startIndex = (lhGroupNo - BigDecimal.ONE.intValue()) * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
                    Integer endIndex = lhGroupNo * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
                    List<ProductionMouldInfoVo> doubleMouldList = selectMouldList.subList(startIndex, endIndex);
                    CxLhMouldProductionCalculator.continueSkuLhProductionHandler(context, groupPlanInfo, cxContinueSkuInfo, productionDay, productionQty, doubleMouldList);
                    sumProductionQty = sumProductionQty - productionQty;
                }
            });
        });
    }

    /**
     * 续作Sku使用续作模具排产
     * 可能需要进行降膜排产
     *
     * @param context            排产上下文
     * @param groupPlanInfo      分组计划信息对象
     * @param continueSkuInfoMap 续作Sku信息
     * @param assignedLhGroupNo  选中的硫化组集合
     */
    private static void productionContinueSku(TbrProductionContext context, ProductionPlanGroupInfo groupPlanInfo, Map<String, CxContinueSkuInfoHelper> continueSkuInfoMap, Set<Integer> assignedLhGroupNo) {
        Set<Integer> stopDays = context.getStopDays();
        Integer monthDays = context.getMonthDays();
        ProductionCapacityParamConfiguration paramConfiguration = context.getBaseDataContainer().getParamConfiguration();
        continueSkuInfoMap.forEach((materialDesc, cxContinueSkuInfo) -> {
            Integer maxDayQty = cxContinueSkuInfo.getMaxDaySingleLhMachineQty().intValue();
            //降膜排产
            DeductMouldVo deductMould = DeductMouldScheduler.createDeductMouldBySku(monthDays, stopDays, new HashSet<>(), paramConfiguration, cxContinueSkuInfo);
            List<DailyScheduleVo> resultList = DeductMouldScheduler.scheduleProduction(deductMould);
            //分配结果
            if (CollectionUtils.isEmpty(resultList)) {
                //todo 记录日志
                return;
            }
            //挑选的模具 使用最大的模具数
            Integer maxMouldNumber = resultList.stream().mapToInt(DailyScheduleVo::getSkuMachines).max().getAsInt() * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
            List<ProductionMouldInfoVo> selectMouldList = SkuMouldSelector.getContinueSkuMouldNumberInit(context, materialDesc, maxMouldNumber);
//            List<CxLhProductionHelper> allocationGroupList = ContinueSkuCalculator.continueSkuAllocationLhGroup(context, groupPlanInfo, assignedLhGroupNo, cxContinueSkuInfo);
//            if (CollectionUtils.isEmpty(allocationGroupList)) {
//                //todo 记录日志
//                return;
//            }
//            //按硫化组编号排序
//            allocationGroupList.sort(Comparator.comparing(CxLhProductionHelper::getLhGroupNo));
            //按排产日进行排序
            resultList.sort(Comparator.comparing(DailyScheduleVo::getScheduleDate));
            resultList.forEach(dailySchedule -> {
                Integer lhMachineCount = dailySchedule.getSkuMachines();
                Integer sumProductionQty = dailySchedule.getSkuQuantity();
                Integer productionDay = dailySchedule.getScheduleDate();
                for (int lhGroupNo = BigDecimal.ONE.intValue(); lhGroupNo <= lhMachineCount; lhGroupNo++) {
                    Integer productionQty = Math.min(sumProductionQty, maxDayQty);
                    Integer startIndex = (lhGroupNo - BigDecimal.ONE.intValue()) * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
                    Integer endIndex = lhGroupNo * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
                    List<ProductionMouldInfoVo> doubleMouldList = selectMouldList.subList(startIndex, endIndex);
                    CxLhMouldProductionCalculator.continueSkuLhProductionHandler(context, groupPlanInfo, cxContinueSkuInfo, productionDay, productionQty, doubleMouldList);
                    sumProductionQty = sumProductionQty - productionQty;
                }

//                List<CxLhProductionHelper> selectedLhGroupList = allocationGroupList.subList(BigDecimal.ZERO.intValue(), lhMachineCount);
//                for (CxLhProductionHelper lhGroupInfo : selectedLhGroupList) {
//                    Integer productionQty = Math.min(sumProductionQty, maxDayQty);
//                    CxLhMouldProductionCalculator.continueSkuLhProductionHandler(context, groupPlanInfo, cxContinueSkuInfo, productionDay, productionQty, lhGroupInfo);
//                    sumProductionQty = sumProductionQty - productionQty;
//                }
            });
        });
    }

    /**
     * @param context
     * @param groupPlanInfo
     * @param groupContinueInfo
     * @return
     */
    private static List<CxMachineAllocationPlanHelper> buildProductionCxMachineResult(Context context, ProductionPlanGroupInfo groupPlanInfo, CxContinueInfoHelper groupContinueInfo) {
        BigDecimal needCount = groupPlanInfo.getNeedCxCapacityMachineCount();
        //最少需要的机台数
        Integer needMinCxMachineCount = needCount.setScale(0, RoundingMode.UP).intValue();
        //续作Sku高优先级排产
        Set<String> productionCxMachineCodeSet = groupContinueInfo.getCxMachineCodeSet();
        List<ProductGroupCxCapacityInfo> cxCapacityInfoList = groupContinueInfo.getCxCapacityInfoList();


        Integer productionCount = productionCxMachineCodeSet.size();


        return null;
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
    private static void getContinueMachineRelease(Context context, CxContinueMachineReleaseHelper release, Integer needWholeCount, Integer continueMachineCount, ProductionPlanGroupInfo groupPlanInfo, CxContinueInfoHelper groupContinueInfo) {
        //相等，则表示在产机台不能释放
        if (needWholeCount.equals(continueMachineCount)) {
            //先不看扣减机台
            Integer deductionMachineCount = BigDecimal.ZERO.intValue();
            Integer deductionDay = getMinDeductionMachineDay(deductionMachineCount, groupPlanInfo, groupContinueInfo);
            //理论会出现在第一天，此时尝试扣减一台，看最低要求生产天数
            if (null != deductionDay && context.isCycleFirstProductionDay(deductionDay)) {
                deductionMachineCount = deductionMachineCount + BigDecimal.ONE.intValue();
                deductionDay = getMinDeductionMachineDay(deductionMachineCount, groupPlanInfo, groupContinueInfo);
            }
            //不释放
            release.setReleaseMachineCount(BigDecimal.ZERO.intValue());
            release.setEarliestConclusionDay(deductionDay);
            return;
        }
        Integer releaseCount = release.getReleaseMachineCount();
        release.setDeductionMachineCount(releaseCount);
        //需要机台数 + 释放机台数 = 在产机台数
        if (needWholeCount + releaseCount == continueMachineCount) {
            //逐步释放
            Integer deductionMachineCount = release.getDeductionMachineCount();
            Integer deductionDay = getMinDeductionMachineDay(deductionMachineCount, groupPlanInfo, groupContinueInfo);
            //理论会出现在第一天，此时尝试扣减一台，看最低要求生产天数
            if (null != deductionDay && context.isCycleFirstProductionDay(deductionDay)) {
                deductionMachineCount = deductionMachineCount + BigDecimal.ONE.intValue();
                deductionDay = getMinDeductionMachineDay(deductionMachineCount, groupPlanInfo, groupContinueInfo);
            }
            //释放台数
            release.setReleaseMachineCount(releaseCount);
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
            getContinueMachineRelease(context, release, needWholeCount, continueMachineCount, groupPlanInfo, groupContinueInfo);
        }
        //不能减，则再加回，看最先收尾时间点
        if (null == deductionDay) {
            deductionMachineCount = deductionMachineCount - BigDecimal.ONE.intValue();
            deductionDay = getMinDeductionMachineDay(deductionMachineCount, groupPlanInfo, groupContinueInfo);
        }
        //没有一台可释放，表示续作都有排产
        release.setReleaseMachineCount(BigDecimal.ZERO.intValue());
        release.setEarliestConclusionDay(deductionDay);
        return;
    }

    /**
     * 获取可减机台的最早日期
     *
     * @param deductionMachineCount 减机台数
     * @param groupPlanInfo         分组计划信息对象
     * @param groupContinueInfo     续作Sku信息对象
     * @return
     */
    private static Integer getMinDeductionMachineDay(Integer deductionMachineCount, ProductionPlanGroupInfo groupPlanInfo, CxContinueInfoHelper groupContinueInfo) {
        List<GroupDayProductionSummaryHelper> summaryList = groupPlanInfo.getGroupProductionSummary();
        Integer maxEmbryoCount = groupContinueInfo.getDeductionCountLimitValue(deductionMachineCount, CxMachineLimitTypeEnum.MAX_EMBRYO_SIZE);
        Integer maxLhGroupCount = groupContinueInfo.getDeductionCountLimitValue(deductionMachineCount, CxMachineLimitTypeEnum.MAX_LH_COUNT);
        //提取胎胚种类数，模具数都符合的的记录
        List<GroupDayProductionSummaryHelper> canDeductionList = summaryList.stream().filter(summary -> summary.isMatchDeductionMachine(maxEmbryoCount, maxLhGroupCount)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(canDeductionList)) {
            return null;
        }
        canDeductionList.sort(Comparator.comparing(GroupDayProductionSummaryHelper::getProductionDay));
        return canDeductionList.get(BigDecimal.ZERO.intValue()).getProductionDay();
    }

}
