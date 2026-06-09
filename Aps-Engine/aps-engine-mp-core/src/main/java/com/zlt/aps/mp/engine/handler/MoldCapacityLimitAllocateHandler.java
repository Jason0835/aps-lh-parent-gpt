package com.zlt.aps.mp.engine.handler;

import com.google.common.collect.Lists;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.api.domain.entity.MpSkuMoldCapacityAllocateLog;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.domain.vo.SkuMoldCapacityInfoVo;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.engine.service.MonthProductionDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模具产能受限业务分配
 * 处理器
 *
 * @author ZLT
 * @date 20260515
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MoldCapacityLimitAllocateHandler {

    private final MonthProductionDataService monthProductionDataService;

    /**
     * 对模具产能受限是，对各Sku进行产能分配
     * 按同分组+主花纹估算模具产能
     * 1、先计算同分组+主花纹下的最大模具产能、同分组+主花纹下的高优先级需求量、同分组+主花纹下的净需求需求量
     * 1.1、如果最大模具产能 >= 总需求量，则保持，不用分配
     * 1.2、如果需求超出模具产能，则比较同分组+主花纹下的高优先级需求量
     * 1.2.1、如果总高优先级需求量 <= 模具总产能，则高优先级量保持不变，再看除高优先级外的其它净需求
     * 1.2.1.1 剩余模具产能(模具总产能 - 汇总高优先级需求量)，对其它需求等比例分摊
     * 1.2.2、如果总高优先级需求量 > 模具总产能，则调整高优先级需求量，按比例分摊，同时净需求 = 调整后的高优先级量
     *
     * @param groupInfo         分组
     * @param groupPlanList     同分组下需求计划
     * @param productionContext 排产上下文
     * @return
     */
    public List<MpSkuMoldCapacityAllocateLog> moldCapacityAllocate(ProductionPlanGroupInfo groupInfo, List<MonthPlanProductionRequirePlanVo> groupPlanList, Map<String, Integer> maxEnableMouldNumberMap, TbrProductionContext productionContext) {
        // 模具最大产能=日硫化量<取最小>*模具数/2 * 月度最大天数，若是共用模，合并计算；
        if (null == groupInfo || CollectionUtils.isEmpty(maxEnableMouldNumberMap)) {
            return Collections.emptyList();
        }
        //剔除不排产的计划
        List<MonthPlanProductionRequirePlanVo> productionPlanList = groupPlanList.stream().filter(productionPlan -> YesOrNoEnum.YES.getCode().equals(productionPlan.getProductionFlag()) && StringUtils.isNotBlank(productionPlan.getMainPattern())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(productionPlanList)) {
            return Collections.emptyList();
        }
        //按分组名+主花纹：分组
        Map<String, List<MonthPlanProductionRequirePlanVo>> groupMap = productionPlanList.stream().collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getGroupKey));
        if (CollectionUtils.isEmpty(groupMap)) {
            return Collections.emptyList();
        }
        List<SkuMoldCapacityInfoVo> allocateResultList = Lists.newArrayList();
        groupMap.forEach((groupKey, requirePlanList) -> {
            //同分组名下，同主花纹的计划信息
            if (CollectionUtils.isEmpty(requirePlanList)) {
                return;
            }
            //获取对应的模具产能
            Integer maxMouldCapacity = getGroupMainPatternMaxMoldCapacity(productionContext, groupInfo, maxEnableMouldNumberMap, groupKey);
            if (null == maxMouldCapacity) {
                return;
            }
            List<SkuMoldCapacityInfoVo> skuList = Lists.newArrayList();
            //所有净需求量
            Integer sumNetQty = requirePlanList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getOriginProductionQty).sum();
            //所有高需求量
            Integer sumHeightQty = requirePlanList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getOriginHeightProductionQty).sum();
            Map<String, List<MonthPlanProductionRequirePlanVo>> skuGroupMap = requirePlanList.stream().collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc));
            skuGroupMap.forEach((materialDesc, singleSkuPlanList) -> {
                if (CollectionUtils.isEmpty(singleSkuPlanList)) {
                    return;
                }
                MonthPlanProductionRequirePlanVo skuInfo = singleSkuPlanList.get(BigDecimal.ZERO.intValue());
                Integer skuHeightQty = singleSkuPlanList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getOriginHeightProductionQty).sum();
                Integer skuNetQty = singleSkuPlanList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getOriginProductionQty).sum();
                SkuMoldCapacityInfoVo skuRequireInfo = SkuMoldCapacityInfoVo.buildByBaseInfo(skuInfo);
                skuRequireInfo.setMaxMoldCapacity(maxMouldCapacity);
                skuRequireInfo.setSumProductionQty(sumNetQty);
                skuRequireInfo.setSumHeightProductionQty(sumHeightQty);
                skuRequireInfo.setProductionQty(skuNetQty);
                skuRequireInfo.setHeightProductionQty(skuHeightQty);
                skuList.add(skuRequireInfo);
            });
            if (CollectionUtils.isEmpty(skuList)) {
                return;
            }
            //计算模具受限产能
            List<SkuMoldCapacityInfoVo> groupResultList = moldCapacityAllocateHandler(skuList);
            if (CollectionUtils.isEmpty(groupResultList)) {
                return;
            }
            allocateResultList.addAll(groupResultList);
        });
        if (CollectionUtils.isEmpty(allocateResultList)) {
            return Collections.emptyList();
        }
        //保存数据
        List<MpSkuMoldCapacityAllocateLog> logList = Lists.newArrayList();
        allocateResultList.forEach(singleSku -> {
            MpSkuMoldCapacityAllocateLog log = singleSku.buildLog();
            logList.add(log);
        });
        if (CollectionUtils.isEmpty(logList)) {
            return Collections.emptyList();
        }
        return logList;
    }

    /**
     * 模具产能受限计算
     * 采用等比例分摊：先看总净需求，再看高优先级量需求
     *
     * @param skuList
     */
    public List<SkuMoldCapacityInfoVo> moldCapacityAllocateHandler(List<SkuMoldCapacityInfoVo> skuList) {
        if (CollectionUtils.isEmpty(skuList)) {
            return Collections.emptyList();
        }
        //同一结构+主花纹
        Set<String> groupKeySet = skuList.stream().map(SkuMoldCapacityInfoVo::getGroupAndMainPattern).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(groupKeySet) || groupKeySet.size() != BigDecimal.ONE.intValue()) {
            return Collections.emptyList();
        }
        //等比分摊
        skuList.forEach(singleSku -> singleSku.allocateHandler());
        Integer maxMoldCapacity = skuList.get(BigDecimal.ZERO.intValue()).getMaxMoldCapacity();
        Integer allocateSumQty = skuList.stream().mapToInt(SkuMoldCapacityInfoVo::getAllocateNetQty).sum();
        if (allocateSumQty <= maxMoldCapacity) {
            return skuList;
        }
        //按量排序，最后一个不超:高优先级量多 -> 净需求量多
        Comparator sort = Comparator.comparing(SkuMoldCapacityInfoVo::getAllocateHeightQty, Comparator.reverseOrder())
                .thenComparing(SkuMoldCapacityInfoVo::getAllocateNetQty, Comparator.reverseOrder());
        skuList.sort(sort);
        List<SkuMoldCapacityInfoVo> keepList = Lists.newArrayList();
        Integer accumulateQty = BigDecimal.ZERO.intValue();
        for (SkuMoldCapacityInfoVo singleSku : skuList) {
            accumulateQty = accumulateQty + singleSku.getAllocateNetQty();
            //小于
            if (accumulateQty < maxMoldCapacity) {
                keepList.add(singleSku);
                continue;
            }
            //等于
            if (accumulateQty.equals(maxMoldCapacity)) {
                keepList.add(singleSku);
                break;
            }
            //大于
            Integer diffValue = accumulateQty - maxMoldCapacity;
            Integer allocateNetQty = singleSku.getAllocateNetQty();
            Integer allocateHeightQty = singleSku.getAllocateHeightQty();
            Integer allocateDiffValue = allocateNetQty - allocateHeightQty;
            Integer deductHeight = diffValue - allocateDiffValue;
            if (deductHeight > BigDecimal.ZERO.intValue()) {
                singleSku.setAllocateHeightQty(allocateHeightQty - deductHeight);
            }
            singleSku.setAllocateNetQty(allocateNetQty - diffValue);
            if (singleSku.getAllocateHeightQty() < BigDecimal.ZERO.intValue()) {
                singleSku.setAllocateHeightQty(BigDecimal.ZERO.intValue());
            }
            if (singleSku.getAllocateNetQty() < BigDecimal.ZERO.intValue()) {
                singleSku.setAllocateNetQty(BigDecimal.ZERO.intValue());
            }
            keepList.add(singleSku);
            break;
        }
        return keepList;
    }

    /**
     * 保存数据
     *
     * @param context
     * @param logList
     */
    public void saveMoldCapacityResult(Context context, List<MpSkuMoldCapacityAllocateLog> logList) {
        if (CollectionUtils.isEmpty(logList)) {
            return;
        }
        logList.forEach(log -> {
            log.setFactoryCode(context.getFactoryCode());
            log.setYear(context.getYear());
            log.setMonth(context.getMonth());
            log.setPlanType(context.getPlanType());
            log.setMonthPlanVersion(context.getMonthPlanVersion());
            log.setProductionVersion(context.getProductionVersion());
        });
        monthProductionDataService.saveMoldCapacityLog(logList);
    }

    /**
     * 获取某个分组下某个主花纹的最大模具产能
     *
     * @param productionContext       排产上下文
     * @param groupInfo               分组对象
     * @param maxEnableMouldNumberMap 分组名 + 主花纹下的最大模具产能
     * @param groupKey                分组名 + 主花纹的分组
     * @return
     */
    private Integer getGroupMainPatternMaxMoldCapacity(TbrProductionContext productionContext, ProductionPlanGroupInfo groupInfo, Map<String, Integer> maxEnableMouldNumberMap, String groupKey) {
        if (null == groupInfo || CollectionUtils.isEmpty(maxEnableMouldNumberMap) || StringUtils.isBlank(groupKey)) {
            return null;
        }
        //获取对应的模具
        if (!maxEnableMouldNumberMap.containsKey(groupKey)) {
            return null;
        }
        Integer maxMouldNumber = maxEnableMouldNumberMap.get(groupKey);
        Integer lhMachineCount = maxMouldNumber / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        if (BigDecimal.ZERO.intValue() == lhMachineCount) {
            return null;
        }
        List<MonthPlanProductionRequirePlanVo> groupPlanData = groupInfo.getGroupPlanData();
        if (CollectionUtils.isEmpty(groupPlanData)) {
            return null;
        }
        List<MonthPlanProductionRequirePlanVo> singleMainPatternList = groupPlanData.stream().filter(singlePlan -> groupKey.equals(singlePlan.getGroupKey()) && singlePlan.isEffectiveDayVulcanizationQty()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(singleMainPatternList)) {
            return null;
        }
        Integer dayCapacityQty = singleMainPatternList.get(BigDecimal.ZERO.intValue()).getDayVulcanizationQty();
        return dayCapacityQty * lhMachineCount * ProductionConstant.DOUBLE_MOULD_PRODUCTION * productionContext.getMaxProductionDays();
    }

}
