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
        Map<String, List<SkuDayProductionInfoHelper>> skuProductionDetailInfo = dayProductionInfo.getSkuProductionDetailInfo();
        if (CollectionUtils.isEmpty(skuProductionDetailInfo)) {
            Map<String, CxContinueSkuInfoHelper> remainder = buildOriginInfoByContinueInfo(continueSkuMap);
            return remainder;
        }
        Map<String, CxContinueSkuInfoHelper> remainder = findRemainderByDayProductionInfo(context, continueType, skuProductionDetailInfo, continueSkuMap);
        return remainder;
    }

    /**
     * 构建续作Sku收尾余量的Sku信息
     *
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
            return buildOriginInfoByContinueInfo(continueSkuMap);
        }
        Map<String, Map<String, CxContinueSkuInfoHelper>> groupMap;
        if (ContinueTypeEnum.SAME_SPECIFICATIONS_PATTERN == continueType) {
            //同规格同花纹
            groupMap = getSameSpecificationsPatternContinueSkuInfo(continueSkuMap);
        } else if (ContinueTypeEnum.SAME_EMBRYO_CODE_SHARE_MOULD == continueType) {
            //共生胎同模具
            groupMap = getSameMainPatternContinueSkuInfo(continueSkuMap);
        } else {
            groupMap = Maps.newHashMap();
        }
        if (CollectionUtils.isEmpty(groupMap)) {
            return Collections.emptyMap();
        }
        Map<String, CxContinueSkuInfoHelper> hasConclusionRemainderInfo = Maps.newHashMap();
        groupMap.forEach((groupKey, continueSkuInfoMap) -> {
            if (CollectionUtils.isEmpty(continueSkuInfoMap)) {
                return;
            }
            List<CxContinueSkuInfoHelper> sameKeyContinueSkuInfo = Lists.newArrayList(continueSkuInfoMap.values());
            int sumMoldNumber = sameKeyContinueSkuInfo.stream().mapToInt(CxContinueSkuInfoHelper::getMouldNumber).sum();
            CxContinueSkuInfoHelper anyOne = sameKeyContinueSkuInfo.get(BigDecimal.ZERO.intValue());
            int usedMoldNumber = getUsedWholeNumber(context, continueType, skuProductionDetailInfo, anyOne);
            if (sumMoldNumber > usedMoldNumber) {
                Map<String, CxContinueSkuInfoHelper> cloneInfo = buildOriginInfoByContinueInfo(continueSkuInfoMap);
                hasConclusionRemainderInfo.putAll(cloneInfo);
            }
        });
        return hasConclusionRemainderInfo;
    }

    /**
     * 获取续作Sku使用的模具数(全量)
     *
     * @param context                 排产上下文
     * @param continueType            同规格同花纹或是共生胎同模具
     * @param skuProductionDetailInfo 日排产信息
     * @param continueSkuInfo         续作Sku信息
     * @return
     */
    private static int getUsedWholeNumber(Context context, ContinueTypeEnum continueType, Map<String, List<SkuDayProductionInfoHelper>> skuProductionDetailInfo, CxContinueSkuInfoHelper continueSkuInfo) {
        if (null == continueSkuInfo) {
            return BigDecimal.ZERO.intValue();
        }
        if (CollectionUtils.isEmpty(skuProductionDetailInfo)) {
            return BigDecimal.ZERO.intValue();
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        int usedWholeNumber = BigDecimal.ZERO.intValue();
        for (Map.Entry<String, List<SkuDayProductionInfoHelper>> entry : skuProductionDetailInfo.entrySet()) {
            String materialDesc = entry.getKey();
            MonthPlanProductionRequirePlanVo productionSku = productionContext.getBaseSkuInfoByPlan(materialDesc);
            //不存在或是主花纹不一致，则跳过
            if (null == productionSku || !continueSkuInfo.getMainPattern().equals(productionSku.getMainPattern())) {
                continue;
            }
            List<SkuDayProductionInfoHelper> productionDetailList = entry.getValue();
            if (CollectionUtils.isEmpty(productionDetailList)) {
                continue;
            }
            List<SkuDayProductionInfoHelper> wholeLhMachineList = productionDetailList.stream().filter(singleLhMachine -> singleLhMachine.isFullLhMachineByContinue(productionContext)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(wholeLhMachineList)) {
                continue;
            }
            int sumUsedMoldNumber = wholeLhMachineList.stream().mapToInt(SkuDayProductionInfoHelper::getUsedMoldNumber).sum();
            //同规格同花纹
            if (ContinueTypeEnum.SAME_SPECIFICATIONS_PATTERN == continueType) {
                if (productionSku.isSameSpecificationsAndPattern(continueSkuInfo)) {
                    usedWholeNumber = usedWholeNumber + sumUsedMoldNumber;
                }
                continue;
            }
            //共生胎同模具
            if (ContinueTypeEnum.SAME_EMBRYO_CODE_SHARE_MOULD == continueType) {
                usedWholeNumber = usedWholeNumber + sumUsedMoldNumber;
            }
        }
        return usedWholeNumber;
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
    private static Map<String, CxContinueSkuInfoHelper> buildOriginInfoByContinueInfo(Map<String, CxContinueSkuInfoHelper> continueSkuMap) {
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
