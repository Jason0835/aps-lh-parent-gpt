package com.zlt.aps.mp.engine.scheduling.cxcapacity;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.daylimit.MouldAllocationInfoVo;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.handler.*;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 不同分组-模具比例分配调整
 * 在续作阶段进行调整-在排产完续作Sku之后，同规格同花纹和同模具之前
 *
 * @author ZLT
 * @date 20260420
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DifferentGroupMoldAllocationAdjustHandler {

    private final DayProductionResultInfoHandler dayProductionResultInfoHandler;

    /**
     * 校验不同分组共用模具在续作Sku排产阶段
     * 按分配的比例进行调整
     *
     * @param context
     */
    public void checkMoldRatioAllocation(Context context) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Integer firstDay = productionContext.getCycleFirstProductionDay();
        if (null == firstDay) {
            return;
        }
        Integer moldRatioAdjustDay = productionContext.getBaseDataContainer().getParamConfiguration().getMoldAllocationRatioCycle();
        if (null == moldRatioAdjustDay) {
            return;
        }
        //1.1 构建模具分配比例的降膜周期内日使用次数
        initMoldRatioDeductInfo(productionContext, moldRatioAdjustDay);
        //1.2 获取结构+主花纹的分配信息
        Map<String, MouldAllocationInfoVo> groupMainPatternAllocationLimitMap = productionContext.getBaseDataContainer().getGroupMainPatternAllocationLimitMap();
        if (CollectionUtils.isEmpty(groupMainPatternAllocationLimitMap)) {
            return;
        }
        //1.3 获取对应结构+主花纹的首日排产信息
        Map<String, Map<String, SkuDayUsedMoldInfoHelper>> groupMainPatternUsedInfoMap = dayProductionResultInfoHandler.getFirstDayProductionMoldNumberByGroupPattern(productionContext, firstDay);
        //1.4 构建以主花纹为分组的分组计划的模具分配数以及续作使用的模具数
        Map<String, DifferentGroupMainPatternAllocationInfoHelper> groupMainPatternInfo = buildDifferentInfo(productionContext, groupMainPatternAllocationLimitMap, groupMainPatternUsedInfoMap);
        //1.5 获取达到初步调整条件的主花纹：初始调整的条件(即一个结构增加模具，同时一个结构减少模具)
        Map<String, List<DifferentGroupMainPatternChangeHelper>> needAdjustMainPatternMap = getReachAdjustBaseCondition(productionContext, groupMainPatternInfo);
        if (CollectionUtils.isEmpty(needAdjustMainPatternMap)) {
            return;
        }
        //1.6 确认主花纹下：Sku的减模日
        List<ContinueSkuDeductInfoHelper> allDeductSkuList = Lists.newArrayList();
        needAdjustMainPatternMap.forEach((mainPattern, changeList) -> {
            DifferentGroupMainPatternAllocationInfoHelper mainPatternDifferentInfo = groupMainPatternInfo.get(mainPattern);
            if (null == mainPatternDifferentInfo) {
                return;
            }
            List<DifferentGroupMainPatternChangeHelper> subList = changeList.stream().filter(singleChange -> singleChange.getChangeNumber() < BigDecimal.ZERO.intValue() && singleChange.getContinueUsedNumber() > BigDecimal.ZERO.intValue()).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(subList)) {
                return;
            }
            subList.forEach(groupDeductInfo -> {
                List<ContinueSkuDeductInfoHelper> deductSkuList = getNeedDeductSkuInfo(productionContext, mainPattern, groupDeductInfo);
                if (CollectionUtils.isEmpty(deductSkuList)) {
                    return;
                }
                allDeductSkuList.addAll(deductSkuList);
            });
        });
        if (CollectionUtils.isEmpty(allDeductSkuList)) {
            return;
        }
        setContinueSkuForceDeductInfo(productionContext, allDeductSkuList);
    }

    /**
     * 构建模具分配比例调整周期内的调整次数
     *
     * @param productionContext  排产上下文
     * @param moldRatioAdjustDay 周期天
     */
    private void initMoldRatioDeductInfo(TbrProductionContext productionContext, Integer moldRatioAdjustDay) {
        if (null == moldRatioAdjustDay) {
            return;
        }
        Integer maxDay = productionContext.getMonthDays();
        if (moldRatioAdjustDay < ProductionConstant.MONTH_START_DAY || moldRatioAdjustDay > maxDay) {
            return;
        }
        Set<Integer> stopDaySet = Optional.ofNullable(productionContext.getStopDays()).orElse(Collections.emptySet());
        Integer deductDay = productionContext.getCycleFirstProductionDay() + BigDecimal.ONE.intValue();
        int daySize = BigDecimal.ONE.intValue();
        Map<Integer, MoldRatioDayDeductHelper> moldRatioDeductMap = Maps.newHashMap();
        for (; deductDay <= maxDay; ) {
            if (stopDaySet.contains(deductDay)) {
                continue;
            }
            if (daySize >= moldRatioAdjustDay) {
                break;
            }
            MoldRatioDayDeductHelper dayDeductHelper = new MoldRatioDayDeductHelper(deductDay, BigDecimal.ZERO.intValue());
            moldRatioDeductMap.put(deductDay, dayDeductHelper);
            deductDay = deductDay + BigDecimal.ONE.intValue();
            daySize = daySize + BigDecimal.ONE.intValue();
        }
        productionContext.setMoldRatioDeductMap(moldRatioDeductMap);
    }

    /**
     * 按主花纹构建信息
     * 包含：分配信息、续作使用信息
     *
     * @param productionContext                  排产上下文
     * @param groupMainPatternAllocationLimitMap 比例分配信息
     * @param groupMainPatternUsedInfoMap        首日使用信息
     * @return
     */
    private Map<String, DifferentGroupMainPatternAllocationInfoHelper> buildDifferentInfo(TbrProductionContext productionContext, Map<String, MouldAllocationInfoVo> groupMainPatternAllocationLimitMap, Map<String, Map<String, SkuDayUsedMoldInfoHelper>> groupMainPatternUsedInfoMap) {
        if (CollectionUtils.isEmpty(groupMainPatternAllocationLimitMap)) {
            return Collections.emptyMap();
        }
        //按主花纹分组
        Map<String, List<MouldAllocationInfoVo>> mainPatternMap = groupMainPatternAllocationLimitMap.values().stream().collect(Collectors.groupingBy(MouldAllocationInfoVo::getMainPattern));
        if (CollectionUtils.isEmpty(mainPatternMap)) {
            return Collections.emptyMap();
        }
        Map<String, DifferentGroupMainPatternAllocationInfoHelper> differentInfoMap = Maps.newHashMap();
        mainPatternMap.forEach((mainPattern, allocationListInfo) -> {
            if (CollectionUtils.isEmpty(allocationListInfo)) {
                return;
            }
            DifferentGroupMainPatternAllocationInfoHelper singleMainPatternInfo = new DifferentGroupMainPatternAllocationInfoHelper();
            singleMainPatternInfo.setMainPattern(mainPattern);
            Map<String, MouldAllocationInfoVo> groupMap = Maps.newHashMap();
            Map<String, Map<String, SkuDayUsedMoldInfoHelper>> groupSkuUsedMap = Maps.newHashMap();
            allocationListInfo.forEach(singleAllocation -> {
                String groupName = singleAllocation.getStructureName();
                groupMap.put(groupName, singleAllocation);
                String groupMainPatternKey = singleAllocation.getDuplicateKey();
                if (StringUtils.isBlank(groupMainPatternKey)) {
                    return;
                }
                Map<String, SkuDayUsedMoldInfoHelper> skuUsedDetailMap = groupMainPatternUsedInfoMap.get(groupMainPatternKey);
                if (CollectionUtils.isEmpty(skuUsedDetailMap)) {
                    return;
                }
                groupSkuUsedMap.put(groupName, skuUsedDetailMap);
            });
            singleMainPatternInfo.setGroupNameAllocationInfoMap(groupMap);
            singleMainPatternInfo.setGroupNameProductionInfoMap(groupSkuUsedMap);
            differentInfoMap.put(mainPattern, singleMainPatternInfo);
        });
        return differentInfoMap;
    }

    /**
     * 检查是否达到调整条件
     * 主花纹下，有分组需要增模，则另有分组就需要能减模
     * 减模时，如果没有使用模具数，则表示可直接增处理，不用调整续作Sku
     *
     * @param productionContext    排产上下文
     * @param groupMainPatternInfo 差异信息
     */
    private Map<String, List<DifferentGroupMainPatternChangeHelper>> getReachAdjustBaseCondition(TbrProductionContext productionContext, Map<String, DifferentGroupMainPatternAllocationInfoHelper> groupMainPatternInfo) {
        if (CollectionUtils.isEmpty(groupMainPatternInfo)) {
            return Collections.emptyMap();
        }
        Map<String, List<DifferentGroupMainPatternChangeHelper>> needAdjustMainPatternMap = Maps.newHashMap();
        groupMainPatternInfo.forEach((mainPattern, differentInfo) -> {
            Map<String, MouldAllocationInfoVo> groupMap = differentInfo.getGroupNameAllocationInfoMap();
            if (CollectionUtils.isEmpty(groupMap)) {
                return;
            }
            Integer groupSize = groupMap.keySet().size();
            if (groupSize <= BigDecimal.ONE.intValue()) {
                return;
            }
            List<DifferentGroupMainPatternChangeHelper> changeList = Lists.newArrayList();
            Map<String, Map<String, SkuDayUsedMoldInfoHelper>> groupNameProductionInfoMap = differentInfo.getGroupNameProductionInfoMap();
            groupMap.forEach((groupName, allocationInfo) -> {
                Integer allocationNumber = allocationInfo.getAllocationQty();
                Map<String, SkuDayUsedMoldInfoHelper> allUsedInfo = groupNameProductionInfoMap.get(groupName);
                Integer usedNumber = BigDecimal.ZERO.intValue();
                if (!CollectionUtils.isEmpty(allUsedInfo)) {
                    usedNumber = allUsedInfo.values().stream().mapToInt(SkuDayUsedMoldInfoHelper::getUsedMoldNumber).sum();
                }
                DifferentGroupMainPatternChangeHelper changeInfo = new DifferentGroupMainPatternChangeHelper(mainPattern, groupName, allocationNumber, usedNumber);
                changeList.add(changeInfo);
            });
            if (CollectionUtils.isEmpty(changeList)) {
                return;
            }
            List<DifferentGroupMainPatternChangeHelper> addList = changeList.stream().filter(singleChange -> singleChange.getChangeNumber() > BigDecimal.ZERO.intValue()).collect(Collectors.toList());
            List<DifferentGroupMainPatternChangeHelper> subList = changeList.stream().filter(singleChange -> singleChange.getChangeNumber() < BigDecimal.ZERO.intValue() && singleChange.getContinueUsedNumber() > BigDecimal.ZERO.intValue()).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(addList) || CollectionUtils.isEmpty(subList)) {
                return;
            }
            needAdjustMainPatternMap.put(mainPattern, changeList);
        });
        return needAdjustMainPatternMap;
    }

    /**
     * 获取某个主花纹下可降膜Sku信息
     *
     * @param productionContext 排产上下文
     * @param mainPattern       主花纹信息
     * @param groupDeductInfo   需降膜信息
     */
    private List<ContinueSkuDeductInfoHelper> getNeedDeductSkuInfo(TbrProductionContext productionContext, String mainPattern, DifferentGroupMainPatternChangeHelper groupDeductInfo) {
        String groupName = groupDeductInfo.getGroupName();
        Integer maxMoldNumber = groupDeductInfo.getAllocationNumber();
        List<ContinueSkuDayUsedMouldInfoHelper> surpassList = dayProductionResultInfoHandler.getGreaterThanAllocationMouldNumberByGroupPattern(productionContext, groupName, mainPattern, maxMoldNumber);
        if (CollectionUtils.isEmpty(surpassList)) {
            return Collections.emptyList();
        }
        List<ContinueSkuDayUsedMouldInfoHelper> canDeductSkuInfo = getCanDeductSkuInfo(productionContext, surpassList);
        if (CollectionUtils.isEmpty(canDeductSkuInfo)) {
            return Collections.emptyList();
        }
        Map<Integer, List<ContinueSkuDayUsedMouldInfoHelper>> canDeductDayMap = canDeductSkuInfo.stream().collect(Collectors.groupingBy(ContinueSkuDayUsedMouldInfoHelper::getProductionDay));
        List<MoldRatioDayDeductHelper> deductInfoList = getSortDeductDayInfo(productionContext);
        if (CollectionUtils.isEmpty(deductInfoList)) {
            return Collections.emptyList();
        }
        List<ContinueSkuDeductInfoHelper> deductSkuList = Lists.newArrayList();
        Integer deductMoldNumber = Math.abs(groupDeductInfo.getChangeNumber());
        Set<Integer> roundDaySet = new HashSet<>();
        for (MoldRatioDayDeductHelper deductDayInfo : deductInfoList) {
            Integer deductDay = deductDayInfo.getDeductDay();
            //获取在deductDay可降膜的Sku信息
            List<ContinueSkuDayUsedMouldInfoHelper> needDeductSkuInfo = getNeedDeductSkuInfo(canDeductDayMap, deductDay, deductSkuList);
            if (CollectionUtils.isEmpty(needDeductSkuInfo)) {
                roundDaySet.add(deductDay);
                continue;
            }
            if (deductMoldNumber <= BigDecimal.ZERO.intValue()) {
                break;
            }
            if (roundDaySet.contains(deductDay)) {
                break;
            }
            roundDaySet = new HashSet<>();
            //剩余模具数多的优先降膜
            needDeductSkuInfo.sort(Comparator.comparing(ContinueSkuDayUsedMouldInfoHelper::getLeftOverMouldNumber, Comparator.reverseOrder()));
            ContinueSkuDayUsedMouldInfoHelper findSku = needDeductSkuInfo.get(BigDecimal.ZERO.intValue());
            deductMoldNumber = deductMoldNumber - ProductionConstant.DOUBLE_MOULD_PRODUCTION;
            deductDayInfo.addDeductOneCount();
            ContinueSkuDeductInfoHelper deductContinueSku = new ContinueSkuDeductInfoHelper(findSku.getMaterialDesc(), deductDay, ProductionConstant.DOUBLE_MOULD_PRODUCTION);
            deductSkuList.add(deductContinueSku);
        }
        if (CollectionUtils.isEmpty(deductInfoList)) {
            return Collections.emptyList();
        }
        return getRealDeductSkuInfo(deductSkuList);
    }

    /**
     * 设置续作Sku强制降膜信息
     *
     * @param productionContext 排产上下文
     * @param allDeductSkuList  强制降膜信息集合
     */
    private void setContinueSkuForceDeductInfo(TbrProductionContext productionContext, List<ContinueSkuDeductInfoHelper> allDeductSkuList) {
        if (CollectionUtils.isEmpty(allDeductSkuList)) {
            productionContext.setForceDeductSkuMap(Collections.emptyMap());
            return;
        }
        Map<String, List<ContinueSkuDeductInfoHelper>> skuGroupMap = allDeductSkuList.stream().collect(Collectors.groupingBy(ContinueSkuDeductInfoHelper::getMaterialDesc));
        Map<String, Map<Integer, Integer>> forceDeductSkuMap = Maps.newHashMap();
        skuGroupMap.forEach((materialDesc, detailInfo) -> {
            if (CollectionUtils.isEmpty(detailInfo)) {
                return;
            }
            Map<Integer, Integer> dayDeductInfoMap = Maps.newHashMap();
            detailInfo.forEach(singleDayInfo -> {
                Integer deductDay = singleDayInfo.getDeductDay();
                Integer sumDeductNumber = dayDeductInfoMap.get(deductDay);
                if (null == sumDeductNumber) {
                    sumDeductNumber = BigDecimal.ZERO.intValue();
                }
                sumDeductNumber = sumDeductNumber + singleDayInfo.getDeductMoldNumber();
                dayDeductInfoMap.put(deductDay, sumDeductNumber);
            });
            if (CollectionUtils.isEmpty(dayDeductInfoMap)) {
                return;
            }
            forceDeductSkuMap.put(materialDesc, dayDeductInfoMap);
        });
        if (CollectionUtils.isEmpty(forceDeductSkuMap)) {
            productionContext.setForceDeductSkuMap(Collections.emptyMap());
            return;
        }
        productionContext.setForceDeductSkuMap(forceDeductSkuMap);
    }

    /**
     * 获取可减模排的Sku信息
     *
     * @param productionContext 排产上下文
     * @param surpassList       超出分配比例的排产信息
     * @return
     */
    private List<ContinueSkuDayUsedMouldInfoHelper> getCanDeductSkuInfo(TbrProductionContext productionContext, List<ContinueSkuDayUsedMouldInfoHelper> surpassList) {
        if (CollectionUtils.isEmpty(surpassList)) {
            return Collections.emptyList();
        }
        Map<Integer, MoldRatioDayDeductHelper> moldRatioDeductMap = productionContext.getMoldRatioDeductMap();
        if (CollectionUtils.isEmpty(moldRatioDeductMap)) {
            return Collections.emptyList();
        }
        Set<Integer> needDeductDay = moldRatioDeductMap.keySet();
        List<ContinueSkuDayUsedMouldInfoHelper> canDeductList = Lists.newArrayList();
        surpassList.forEach(singleSkuInfo -> {
            Integer usedNumber = singleSkuInfo.getUsedMouldNumber();
            if (usedNumber <= ProductionConstant.DOUBLE_MOULD_PRODUCTION) {
                return;
            }
            if (!needDeductDay.contains(singleSkuInfo.getProductionDay())) {
                return;
            }
            canDeductList.add(singleSkuInfo);
        });
        return canDeductList;
    }

    /**
     * 排序，从降膜次数小到大，日期从大到小
     *
     * @param productionContext
     * @return
     */
    private List<MoldRatioDayDeductHelper> getSortDeductDayInfo(TbrProductionContext productionContext) {
        Map<Integer, MoldRatioDayDeductHelper> moldRatioDeductMap = productionContext.getMoldRatioDeductMap();
        if (CollectionUtils.isEmpty(moldRatioDeductMap)) {
            return Collections.emptyList();
        }
        List<MoldRatioDayDeductHelper> allInfo = moldRatioDeductMap.values().stream().collect(Collectors.toList());
        allInfo.sort(Comparator.comparing(MoldRatioDayDeductHelper::getDeductCount).thenComparing(MoldRatioDayDeductHelper::getDeductDay, Comparator.reverseOrder()));
        return allInfo;
    }

    /**
     * 获取能降膜的Sku信息
     *
     * @param canDeductDayMap 可降膜Sku信息集合
     * @param deductDay       降膜日
     * @param deductSkuList   已经降膜Sku信息
     * @return
     */
    private List<ContinueSkuDayUsedMouldInfoHelper> getNeedDeductSkuInfo(Map<Integer, List<ContinueSkuDayUsedMouldInfoHelper>> canDeductDayMap, Integer deductDay, List<ContinueSkuDeductInfoHelper> deductSkuList) {
        if (CollectionUtils.isEmpty(canDeductDayMap) || null == deductDay) {
            return Collections.emptyList();
        }
        List<ContinueSkuDayUsedMouldInfoHelper> needDeductSkuInfo = canDeductDayMap.get(deductDay);
        if (CollectionUtils.isEmpty(needDeductSkuInfo)) {
            return Collections.emptyList();
        }
        needDeductSkuInfo.forEach(singleSku -> {
            if (CollectionUtils.isEmpty(deductSkuList)) {
                singleSku.setPlanDeductNumber(BigDecimal.ZERO.intValue());
                return;
            }
            //累计同Sku后续的降膜数：因降膜都是从前面开始的，如 6号降膜2副，7号降膜两副，则7号剩余的模具数 = 使用模具数 - Sum(6,7)降膜数
            List<ContinueSkuDeductInfoHelper> planDeductList = deductSkuList.stream().filter(single -> singleSku.getMaterialDesc().equals(single.getMaterialDesc()) && single.getDeductDay() >= deductDay).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(planDeductList)) {
                singleSku.setPlanDeductNumber(BigDecimal.ZERO.intValue());
                return;
            }
            Integer sumDeductNumber = planDeductList.stream().mapToInt(ContinueSkuDeductInfoHelper::getDeductMoldNumber).sum();
            singleSku.setPlanDeductNumber(sumDeductNumber);
        });
        //剩余模具>0表示还可降膜
        List<ContinueSkuDayUsedMouldInfoHelper> resultList = needDeductSkuInfo.stream().filter(singleSku -> singleSku.getLeftOverMouldNumber() > BigDecimal.ZERO.intValue()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(resultList)) {
            return Collections.emptyList();
        }
        return resultList;
    }

    /**
     * 获取降膜Sku的信息
     * 后面的降膜日需要累计前面日降膜信息
     *
     * @param deductSkuInfo 降膜信息
     * @return
     */
    private List<ContinueSkuDeductInfoHelper> getRealDeductSkuInfo(List<ContinueSkuDeductInfoHelper> deductSkuInfo) {
        if (CollectionUtils.isEmpty(deductSkuInfo)) {
            return Collections.emptyList();
        }
        List<ContinueSkuDeductInfoHelper> handlerInfo = Lists.newArrayList();
        //按Sku分组
        Map<String, List<ContinueSkuDeductInfoHelper>> skuDeductInfoMap = deductSkuInfo.stream().collect(Collectors.groupingBy(ContinueSkuDeductInfoHelper::getMaterialDesc));
        skuDeductInfoMap.forEach((materialDesc, detailInfoList) -> {
            //降膜日从小大到排序
            detailInfoList.sort(Comparator.comparing(ContinueSkuDeductInfoHelper::getDeductDay));
            detailInfoList.forEach(singleInfo -> {
                Integer deductDay = singleInfo.getDeductDay();
                //获取前面是否有降膜，有则需要累加到后面的日期，如 5号降两模、6号再降两模，则5号降膜数为2，而6号相对之前就降了4模
                List<ContinueSkuDeductInfoHelper> allDeductList = detailInfoList.stream().filter(find -> find.getDeductDay() <= deductDay).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(allDeductList)) {
                    return;
                }
                Integer sumDeductMoldNumber = allDeductList.stream().mapToInt(ContinueSkuDeductInfoHelper::getDeductMoldNumber).sum();
                handlerInfo.add(new ContinueSkuDeductInfoHelper(materialDesc, deductDay, sumDeductMoldNumber));
            });
        });
        if (CollectionUtils.isEmpty(handlerInfo)) {
            return Collections.emptyList();
        }
        return handlerInfo;
    }

}
