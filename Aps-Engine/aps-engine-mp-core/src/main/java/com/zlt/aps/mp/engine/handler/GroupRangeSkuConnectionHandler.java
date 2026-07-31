package com.zlt.aps.mp.engine.handler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zlt.aps.mp.engine.daylimit.GroupPlanCxLhCapacityLimitHelper;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxContinueSkuInfoHelper;
import com.zlt.aps.mp.engine.domain.dto.EarliestConclusionLhGroupHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.dto.SkuDayProductionInfoHelper;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.enums.ContinueTypeEnum;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.utils.BeanCopyUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 结构内排产-Sku前后衔接业务处理器
 *
 * @author ZLT
 * @date 20260727
 */
public class GroupRangeSkuConnectionHandler {

    /**
     * 根据最早的收尾日，获取在同规格同花纹、共生胎同模具阶段(换活字块)
     * 收尾续作Sku信息
     *
     * @param context                   排产上下文
     * @param earliestConclusionLhGroup 最早收尾硫化组信息
     * @param productionPlanInfo        分组信息对象
     * @param continueType              类型
     * @param continueSkuMap            续作Sku信息对象集合
     */
    public static Map<String, CxContinueSkuInfoHelper> getRemainderContinueSkuInfoByEarliestConclusionDay(Context context,
                                                                                                          EarliestConclusionLhGroupHelper earliestConclusionLhGroup,
                                                                                                          ProductionPlanGroupInfo productionPlanInfo,
                                                                                                          ContinueTypeEnum continueType,
                                                                                                          Map<String, CxContinueSkuInfoHelper> continueSkuMap) {
        if (null == earliestConclusionLhGroup || null == productionPlanInfo) {
            return Collections.emptyMap();
        }
        if (!(ContinueTypeEnum.SAME_SPECIFICATIONS_PATTERN == continueType || ContinueTypeEnum.SAME_EMBRYO_CODE_SHARE_MOULD == continueType)) {
            //非(同规格同花纹、共生胎同模具)
            return Collections.emptyMap();
        }
        //最早可上机日
        Integer startDay = earliestConclusionLhGroup.getClosingDay();
        Integer endDay = earliestConclusionLhGroup.getEndDay();
        if (null == startDay || null == endDay) {
            return Collections.emptyMap();
        }
        if (startDay > endDay) {
            return Collections.emptyMap();
        }
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo = productionPlanInfo.getDayProductionLimitInfo();
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return Collections.emptyMap();
        }
        GroupPlanCxLhCapacityLimitHelper dayProductionInfo = dayProductionLimitInfo.get(startDay);
        if (null == dayProductionInfo) {
            return Collections.emptyMap();
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //复制续作信息
        Map<String, CxContinueSkuInfoHelper> cloneContinueInfo = buildCloneOriginInfoByContinueInfo(continueSkuMap);
        Map<String, List<SkuDayProductionInfoHelper>> skuProductionDetailInfo = dayProductionInfo.getSkuProductionDetailInfo();
        if (productionContext.isCycleFirstProductionDay(startDay) && CollectionUtils.isEmpty(skuProductionDetailInfo)) {
            //第一天没有排产续作信息
            return cloneContinueInfo;
        }
        //20260730+ 需要根据前一天排产信息，构建续作机台数即修改cloneContinueInfo中的模具数
        Map<String, Map<String, CxContinueSkuInfoHelper>> groupContinueSkuInfoMap = getGroupByContinueType(continueType, cloneContinueInfo);
        if (CollectionUtils.isEmpty(groupContinueSkuInfoMap)) {
            return Collections.emptyMap();
        }
        GroupPlanCxLhCapacityLimitHelper previousDayProductionInfo = null;
        Integer previousDay = productionPlanInfo.getPreviousDay(startDay);
        if (null != previousDay) {
            previousDayProductionInfo = dayProductionLimitInfo.get(previousDay);
        }
        Map<String, CxContinueSkuInfoHelper> remainder = findRemainderByDayProductionInfo(context, continueType, groupContinueSkuInfoMap, previousDayProductionInfo, startDay, skuProductionDetailInfo);
        return remainder;
    }

    /**
     * 根据最早的收尾日，获取在同规格同花纹、共生胎同模具阶段(换活字块)
     * 收尾续作Sku信息
     *
     * @param context                   排产上下文
     * @param earliestConclusionLhGroup 最早收尾硫化组信息
     * @param productionPlanInfo        分组信息对象
     * @param continueType              类型
     * @param continueSkuMap            续作Sku信息对象集合
     */
    public static Map<String, CxContinueSkuInfoHelper> getRemainderContinueSkuInfoByEarliestConclusionDayComparisonInit(Context context,
                                                                                                                        EarliestConclusionLhGroupHelper earliestConclusionLhGroup,
                                                                                                                        ProductionPlanGroupInfo productionPlanInfo,
                                                                                                                        ContinueTypeEnum continueType,
                                                                                                                        Map<String, CxContinueSkuInfoHelper> continueSkuMap) {
        if (null == earliestConclusionLhGroup || null == productionPlanInfo) {
            return Collections.emptyMap();
        }
        if (!(ContinueTypeEnum.SAME_SPECIFICATIONS_PATTERN == continueType || ContinueTypeEnum.SAME_EMBRYO_CODE_SHARE_MOULD == continueType)) {
            //非(同规格同花纹、共生胎同模具)
            return Collections.emptyMap();
        }
        //最早可上机日
        Integer startDay = earliestConclusionLhGroup.getClosingDay();
        Integer endDay = earliestConclusionLhGroup.getEndDay();
        if (null == startDay || null == endDay) {
            return Collections.emptyMap();
        }
        if (startDay > endDay) {
            return Collections.emptyMap();
        }
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo = productionPlanInfo.getDayProductionLimitInfo();
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return Collections.emptyMap();
        }
        GroupPlanCxLhCapacityLimitHelper dayProductionInfo = dayProductionLimitInfo.get(startDay);
        if (null == dayProductionInfo) {
            return Collections.emptyMap();
        }
        Map<String, List<SkuDayProductionInfoHelper>> skuProductionDetailInfo = dayProductionInfo.getSkuProductionDetailInfo();
        if (CollectionUtils.isEmpty(skuProductionDetailInfo)) {
            Map<String, CxContinueSkuInfoHelper> remainder = buildCloneOriginInfoByContinueInfo(continueSkuMap);
            return remainder;
        }
        Map<String, CxContinueSkuInfoHelper> remainder = findRemainderByDayProductionInfo(context, continueType, skuProductionDetailInfo, continueSkuMap);
        return remainder;
    }

    /**
     * 构建续作Sku收尾余量的Sku信息，以月末续作进行衔接
     *
     * @param context                 排产上下文
     * @param continueType            同规格同花纹或是共生胎同模具
     * @param skuProductionDetailInfo 排产Sku信息
     * @param continueSkuMap          续作Sku信息
     * @return
     */
    private static Map<String, CxContinueSkuInfoHelper> findRemainderByDayProductionInfo(Context context, ContinueTypeEnum continueType, Map<String, List<SkuDayProductionInfoHelper>> skuProductionDetailInfo, Map<String, CxContinueSkuInfoHelper> continueSkuMap) {
        if (CollectionUtils.isEmpty(continueSkuMap)) {
            return Collections.emptyMap();
        }
        if (CollectionUtils.isEmpty(skuProductionDetailInfo)) {
            return buildCloneOriginInfoByContinueInfo(continueSkuMap);
        }
        Map<String, Map<String, CxContinueSkuInfoHelper>> groupMap = getGroupByContinueType(continueType, continueSkuMap);
        if (CollectionUtils.isEmpty(groupMap)) {
            return Collections.emptyMap();
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, CxContinueSkuInfoHelper> hasConclusionRemainderInfo = Maps.newHashMap();
        groupMap.forEach((groupKey, continueSkuInfoMap) -> {
            if (CollectionUtils.isEmpty(continueSkuInfoMap)) {
                return;
            }
            List<CxContinueSkuInfoHelper> sameKeyContinueSkuInfo = Lists.newArrayList(continueSkuInfoMap.values());
            int sumMoldNumber = sameKeyContinueSkuInfo.stream().mapToInt(CxContinueSkuInfoHelper::getMouldNumber).sum();
            CxContinueSkuInfoHelper anyOne = sameKeyContinueSkuInfo.get(BigDecimal.ZERO.intValue());
            int usedMoldNumber = getUsedWholeNumber(productionContext, continueType, skuProductionDetailInfo, anyOne);
            if (sumMoldNumber > usedMoldNumber) {
                Map<String, CxContinueSkuInfoHelper> cloneInfo = buildCloneOriginInfoByContinueInfo(continueSkuInfoMap);
                hasConclusionRemainderInfo.putAll(cloneInfo);
            }
        });
        return hasConclusionRemainderInfo;
    }

    /**
     * 查找续作Sku可接活字块的续作信息，需要根据前日排产的续作Sku信息
     * 如果有一天断开没有接上，则后面也认为不可接上
     *
     * @param context                   排产上下文
     * @param continueType              同规格同花纹或是共生胎同模具
     * @param originGroup               上个月月末的续作信息
     * @param previousDayProductionInfo 前日排产信息
     * @param productionDay             排产日
     * @param skuProductionDetailInfo   排产日排产信息
     * @return
     */
    private static Map<String, CxContinueSkuInfoHelper> findRemainderByDayProductionInfo(Context context,
                                                                                         ContinueTypeEnum continueType,
                                                                                         Map<String, Map<String, CxContinueSkuInfoHelper>> originGroup,
                                                                                         GroupPlanCxLhCapacityLimitHelper previousDayProductionInfo,
                                                                                         Integer productionDay,
                                                                                         Map<String, List<SkuDayProductionInfoHelper>> skuProductionDetailInfo) {
        if (CollectionUtils.isEmpty(originGroup) || null == productionDay) {
            return Collections.emptyMap();
        }
        Map<String, ContinueRemainderSkuInfoHelper> remainderSkuInfo = getContinueRemainderSkuInfoByDay(context, continueType, originGroup, previousDayProductionInfo, productionDay);
        if (CollectionUtils.isEmpty(remainderSkuInfo)) {
            return Collections.emptyMap();
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, CxContinueSkuInfoHelper> hasConclusionRemainderInfo = Maps.newHashMap();
        remainderSkuInfo.forEach((groupKey, continueSkuInfo) -> {
            Map<String, CxContinueSkuInfoHelper> continueSkuInfoMap = continueSkuInfo.getContinueSkuInfo();
            if (CollectionUtils.isEmpty(continueSkuInfoMap)) {
                return;
            }
            List<CxContinueSkuInfoHelper> sameKeyContinueSkuInfo = Lists.newArrayList(continueSkuInfoMap.values());
            int sumMoldNumber = continueSkuInfo.getSumMoldNumber();
            CxContinueSkuInfoHelper anyOne = sameKeyContinueSkuInfo.get(BigDecimal.ZERO.intValue());
            int usedMoldNumber = getUsedWholeNumber(productionContext, continueType, skuProductionDetailInfo, anyOne);
            if (sumMoldNumber > usedMoldNumber) {
                Map<String, CxContinueSkuInfoHelper> cloneInfo = buildCloneOriginInfoByContinueInfo(continueSkuInfoMap);
                hasConclusionRemainderInfo.putAll(cloneInfo);
            }
        });
        return hasConclusionRemainderInfo;
    }

    /**
     * 获取可接活字块的续作Sku信息
     * 即续作Sku空出硫化机台
     * 不能直接使用上个月月末，需要看前日
     * 前日如果没有续接上则认为后续也不可续接
     *
     * @param context       排产上下文
     * @param continueType  阶段
     * @param originGroup   原始续作信息
     * @param productionDay 排产日
     * @return
     */
    private static Map<String, ContinueRemainderSkuInfoHelper> getContinueRemainderSkuInfoByDay(Context context,
                                                                                                ContinueTypeEnum continueType,
                                                                                                Map<String, Map<String, CxContinueSkuInfoHelper>> originGroup,
                                                                                                GroupPlanCxLhCapacityLimitHelper previousDayProductionInfo,
                                                                                                Integer productionDay) {
        if (null == productionDay || CollectionUtils.isEmpty(originGroup)) {
            return Collections.emptyMap();
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //续作-首日
        if (productionContext.isCycleFirstProductionDay(productionDay)) {
            return buildByInitContinueSkuInfo(originGroup);
        }
        //根据前日排产信息构建
        if (null == previousDayProductionInfo) {
            return Collections.emptyMap();
        }
        Map<String, List<SkuDayProductionInfoHelper>> previousDayDetailInfo = previousDayProductionInfo.getSkuProductionDetailInfo();
        if (CollectionUtils.isEmpty(previousDayDetailInfo)) {
            return Collections.emptyMap();
        }
        Map<String, ContinueRemainderSkuInfoHelper> previousUsed = Maps.newHashMap();
        originGroup.forEach((groupKey, continueSkuInfoMap) -> {
            if (CollectionUtils.isEmpty(continueSkuInfoMap)) {
                return;
            }
            List<CxContinueSkuInfoHelper> sameKeyContinueSkuInfo = Lists.newArrayList(continueSkuInfoMap.values());
            CxContinueSkuInfoHelper anyOne = sameKeyContinueSkuInfo.get(BigDecimal.ZERO.intValue());
            int usedMoldNumber = getUsedNumber(productionContext, continueType, previousDayDetailInfo, anyOne);
            if (usedMoldNumber <= BigDecimal.ZERO.intValue()) {
                return;
            }
            ContinueRemainderSkuInfoHelper singleKey = ContinueRemainderSkuInfoHelper.buildContinueRemainderSkuInfo(groupKey, usedMoldNumber, continueSkuInfoMap);
            previousUsed.put(groupKey, singleKey);
        });
        if (CollectionUtils.isEmpty(previousUsed)) {
            return Collections.emptyMap();
        }
        return previousUsed;
    }

    /**
     * 获取续作Sku使用的模具数(全硫化机台)
     * 即满产或是不可当天换活字块
     *
     * @param productionContext       排产上下文
     * @param continueType            同规格同花纹或是共生胎同模具
     * @param skuProductionDetailInfo 日排产信息
     * @param continueSkuInfo         续作Sku信息
     * @return
     */
    private static int getUsedWholeNumber(TbrProductionContext productionContext, ContinueTypeEnum continueType, Map<String, List<SkuDayProductionInfoHelper>> skuProductionDetailInfo, CxContinueSkuInfoHelper continueSkuInfo) {
        if (null == continueSkuInfo) {
            return BigDecimal.ZERO.intValue();
        }
        if (CollectionUtils.isEmpty(skuProductionDetailInfo)) {
            return BigDecimal.ZERO.intValue();
        }
        int usedWholeNumber = BigDecimal.ZERO.intValue();
        for (Map.Entry<String, List<SkuDayProductionInfoHelper>> entry : skuProductionDetailInfo.entrySet()) {
            MonthPlanProductionRequirePlanVo productionSku = productionContext.getBaseSkuInfoByPlan(entry.getKey());
            //不存在则跳过
            if (null == productionSku) {
                continue;
            }
            int sumUsedWholeMoldNumber = getUsedMoldNumber(productionContext, entry, continueSkuInfo, true);
            //同规格同花纹
            if (ContinueTypeEnum.SAME_SPECIFICATIONS_PATTERN == continueType) {
                if (productionSku.isSameSpecificationsAndPattern(continueSkuInfo)) {
                    usedWholeNumber = usedWholeNumber + sumUsedWholeMoldNumber;
                }
                continue;
            }
            //共生胎同模具
            if (ContinueTypeEnum.SAME_EMBRYO_CODE_SHARE_MOULD == continueType) {
                usedWholeNumber = usedWholeNumber + sumUsedWholeMoldNumber;
            }
        }
        return usedWholeNumber;
    }

    /**
     * 获取续作Sku在skuProductionDetailInfo使用的模具数
     *
     * @param productionContext       排产上下文
     * @param continueType            同规格同花纹或是共生胎同模具
     * @param skuProductionDetailInfo 日排产信息
     * @param continueSkuInfo         续作Sku信息
     * @return
     */
    private static int getUsedNumber(TbrProductionContext productionContext, ContinueTypeEnum continueType, Map<String, List<SkuDayProductionInfoHelper>> skuProductionDetailInfo, CxContinueSkuInfoHelper continueSkuInfo) {
        if (null == continueSkuInfo) {
            return BigDecimal.ZERO.intValue();
        }
        if (CollectionUtils.isEmpty(skuProductionDetailInfo)) {
            return BigDecimal.ZERO.intValue();
        }
        int usedNumber = BigDecimal.ZERO.intValue();
        for (Map.Entry<String, List<SkuDayProductionInfoHelper>> entry : skuProductionDetailInfo.entrySet()) {
            MonthPlanProductionRequirePlanVo productionSku = productionContext.getBaseSkuInfoByPlan(entry.getKey());
            if (null == productionSku) {
                continue;
            }
            int sumUsedMoldNumber = getUsedMoldNumber(productionContext, entry, continueSkuInfo, false);
            //同规格同花纹
            if (ContinueTypeEnum.SAME_SPECIFICATIONS_PATTERN == continueType) {
                if (productionSku.isSameSpecificationsAndPattern(continueSkuInfo)) {
                    usedNumber = usedNumber + sumUsedMoldNumber;
                }
                continue;
            }
            //共生胎同模具
            if (ContinueTypeEnum.SAME_EMBRYO_CODE_SHARE_MOULD == continueType) {
                usedNumber = usedNumber + sumUsedMoldNumber;
            }
        }
        return usedNumber;
    }

    /**
     * 根据续作Sku信息，获取日排产中对应同规格同花纹或是共生胎同模具使用的硫化机台数：即模具数
     *
     * @param productionContext 排产上下文
     * @param entry             日排产Sku明细
     * @param continueSkuInfo   续作Sku信息
     * @param isWholeLhMachine  是否获取满台硫化机 true 表示取满台 false表示取所有
     * @return
     */
    private static int getUsedMoldNumber(TbrProductionContext productionContext,
                                         Map.Entry<String, List<SkuDayProductionInfoHelper>> entry,
                                         CxContinueSkuInfoHelper continueSkuInfo,
                                         boolean isWholeLhMachine) {
        if (null == entry || null == continueSkuInfo) {
            return BigDecimal.ZERO.intValue();
        }
        String materialDesc = entry.getKey();
        MonthPlanProductionRequirePlanVo productionSku = productionContext.getBaseSkuInfoByPlan(materialDesc);
        //不存在或是主花纹不一致，则跳过
        if (null == productionSku || !continueSkuInfo.getMainPattern().equals(productionSku.getMainPattern())) {
            return BigDecimal.ZERO.intValue();
        }
        //没有排产信息
        List<SkuDayProductionInfoHelper> productionDetailList = entry.getValue();
        if (CollectionUtils.isEmpty(productionDetailList)) {
            return BigDecimal.ZERO.intValue();
        }
        if (isWholeLhMachine) {
            productionDetailList = productionDetailList.stream().filter(singleLhMachine -> singleLhMachine.isFullLhMachineByContinue(productionContext)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(productionDetailList)) {
                return BigDecimal.ZERO.intValue();
            }
        }
        return productionDetailList.stream().mapToInt(SkuDayProductionInfoHelper::getUsedMoldNumber).sum();
    }

    /**
     * 根据初始的续作信息，构建可接活字块续作信息集合
     *
     * @param originGroupInfo 按阶段分组续作信息
     * @return
     */
    private static Map<String, ContinueRemainderSkuInfoHelper> buildByInitContinueSkuInfo(Map<String, Map<String, CxContinueSkuInfoHelper>> originGroupInfo) {
        if (CollectionUtils.isEmpty(originGroupInfo)) {
            return Collections.emptyMap();
        }
        Map<String, ContinueRemainderSkuInfoHelper> result = Maps.newHashMap();
        originGroupInfo.forEach((groupKey, continueSkuInfoMap) -> {
            if (CollectionUtils.isEmpty(continueSkuInfoMap)) {
                return;
            }
            List<CxContinueSkuInfoHelper> sameKeyContinueSkuInfo = Lists.newArrayList(continueSkuInfoMap.values());
            int sumMoldNumber = sameKeyContinueSkuInfo.stream().mapToInt(CxContinueSkuInfoHelper::getMouldNumber).sum();
            ContinueRemainderSkuInfoHelper singleGroupRemainder = ContinueRemainderSkuInfoHelper.buildContinueRemainderSkuInfo(groupKey, sumMoldNumber, continueSkuInfoMap);
            result.put(groupKey, singleGroupRemainder);
        });
        return result;
    }

    /**
     * 根据续作阶段，获取对应分组信息
     *
     * @param continueType   续作阶段：同规格同花纹 共生胎同模具
     * @param continueSkuMap 续作Sku信息
     * @return
     */
    private static Map<String, Map<String, CxContinueSkuInfoHelper>> getGroupByContinueType(ContinueTypeEnum continueType, Map<String, CxContinueSkuInfoHelper> continueSkuMap) {
        if (ContinueTypeEnum.SAME_SPECIFICATIONS_PATTERN == continueType) {
            //同规格同花纹
            return getSameSpecificationsPatternContinueSkuInfo(continueSkuMap);
        }
        if (ContinueTypeEnum.SAME_EMBRYO_CODE_SHARE_MOULD == continueType) {
            //共生胎同模具
            return getSameMainPatternContinueSkuInfo(continueSkuMap);
        }
        return Collections.emptyMap();
    }

    /**
     * 获取相同主花纹下的续作Sku信息
     *
     * @param groupAllContinueSkuInfo
     * @return
     */
    private static Map<String, Map<String, CxContinueSkuInfoHelper>> getSameMainPatternContinueSkuInfo(Map<String, CxContinueSkuInfoHelper> groupAllContinueSkuInfo) {
        if (CollectionUtils.isEmpty(groupAllContinueSkuInfo)) {
            return Collections.emptyMap();
        }
        Map<String, Map<String, CxContinueSkuInfoHelper>> mainPatternMap = Maps.newHashMap();
        groupAllContinueSkuInfo.forEach((materialDesc, continueSkuInfo) -> {
            String mainPattern = continueSkuInfo.getMainPattern();
            Map<String, CxContinueSkuInfoHelper> sameMainPatternInfo = mainPatternMap.get(mainPattern);
            if (null == sameMainPatternInfo) {
                sameMainPatternInfo = Maps.newHashMap();
                mainPatternMap.put(mainPattern, sameMainPatternInfo);
            }
            sameMainPatternInfo.put(materialDesc, continueSkuInfo);
        });
        return mainPatternMap;
    }

    /**
     * 获取相同规格+花纹下的续作Sku信息
     *
     * @param groupAllContinueSkuInfo
     * @return
     */
    private static Map<String, Map<String, CxContinueSkuInfoHelper>> getSameSpecificationsPatternContinueSkuInfo(Map<String, CxContinueSkuInfoHelper> groupAllContinueSkuInfo) {
        if (CollectionUtils.isEmpty(groupAllContinueSkuInfo)) {
            return Collections.emptyMap();
        }
        Map<String, Map<String, CxContinueSkuInfoHelper>> specificationsPatternMap = Maps.newHashMap();
        groupAllContinueSkuInfo.forEach((materialDesc, continueSkuInfo) -> {
            String specificationsPattern = continueSkuInfo.getSpecificationsPatternKey();
            Map<String, CxContinueSkuInfoHelper> sameSpecificationsPatternInfo = specificationsPatternMap.get(specificationsPattern);
            if (null == sameSpecificationsPatternInfo) {
                sameSpecificationsPatternInfo = Maps.newHashMap();
                specificationsPatternMap.put(specificationsPattern, sameSpecificationsPatternInfo);
            }
            sameSpecificationsPatternInfo.put(materialDesc, continueSkuInfo);
        });
        return specificationsPatternMap;
    }

    /**
     * 构建初始的续作Sku信息
     *
     * @param continueSkuMap 初始续作Sku信息
     * @return
     */
    private static Map<String, CxContinueSkuInfoHelper> buildCloneOriginInfoByContinueInfo(Map<String, CxContinueSkuInfoHelper> continueSkuMap) {
        if (CollectionUtils.isEmpty(continueSkuMap)) {
            return Collections.emptyMap();
        }
        Map<String, CxContinueSkuInfoHelper> cloneMap = Maps.newHashMap();
        continueSkuMap.forEach((materialDesc, continueSkuInfo) -> {
            CxContinueSkuInfoHelper clone = BeanCopyUtils.copyBean(continueSkuInfo, CxContinueSkuInfoHelper.class);
            cloneMap.put(materialDesc, clone);
        });
        return continueSkuMap;
    }

}
