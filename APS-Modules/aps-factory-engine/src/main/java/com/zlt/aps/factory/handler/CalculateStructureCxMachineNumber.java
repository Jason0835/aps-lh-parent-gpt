package com.zlt.aps.factory.handler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.daylimit.MouldAllocationInfoVo;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductMouldInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.MonthPlanStructureLhRatioVo;
import com.zlt.aps.factory.logrecorder.TbrProductionGroupLogRecorder;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import com.zlt.aps.factory.scheduling.cxcapacity.ProductionCapacityParamConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 6、粗算结构成型机台数
 * （转换成 结构需排产天数，第二轮需要得到第一轮模拟结果中结构排产天数,包含提前收尾）
 * （1）计算结构向下主花纹模具的最大产能；模具最大产能=日硫化量<取最小>*模具数/2 * 月度最大天数，若是共用模，合并计算；
 * （2）计算结构向下主花纹模具的最大可排产量 = MIN { SUM（结构向下主花纹对应的所有SKU的净需求量），主花纹模具的最大产能}；
 * （3）按结构汇总需求量 = SUM(结构向下主花纹模具最大可排产量)；
 * （4）粗算每个结构需要的成型机台数，公式核算：成型机台数 = 结构净需求量/硫化机数*日硫化量<取最小>*月度天数<工作日历>；
 * 其中，硫化机数：结构与硫化配比.最大硫化机数；若存在赛象或软控，取小的硫化机数。
 * （5）续作成型在机超过粗算机台数，在月初直接释放，机型对应的硫化机台数多的优先下机->成型机台编号大的优先下机；
 * （6）若测算的成型机台数>0.9，则直接满月排。
 *
 * @author Yelq
 */
@Slf4j
@Component
public class CalculateStructureCxMachineNumber {

    /**
     * 6、粗算结构成型机台数
     *
     * @param productionContext 排产上下文
     * @param requirePlanList   需求计划
     * @return 结构分组
     */
    public Map<String, ProductionPlanGroupInfo> calculateStructureCxMachineNumber(TbrProductionContext productionContext, List<MonthPlanProductionRequirePlanVo> requirePlanList) {
        if (CollectionUtils.isEmpty(requirePlanList)) {
            return Collections.emptyMap();
        }
        return buildGroupPlanInfoMap(productionContext, requirePlanList);
    }

    /**
     * @param productionContext
     * @param requirePlanList
     * @return
     */
    private Map<String, ProductionPlanGroupInfo> buildGroupPlanInfoMap(TbrProductionContext productionContext, List<MonthPlanProductionRequirePlanVo> requirePlanList) {
        if (CollectionUtils.isEmpty(requirePlanList)) {
            return Collections.emptyMap();
        }
        Map<String, ProductionPlanGroupInfo> groupInfoMap = Maps.newHashMap();
        // 需求计划按照结构分组
        Map<String, List<MonthPlanProductionRequirePlanVo>> mapGroupByStructureName = requirePlanList.stream().filter(item -> StringUtils.isNotBlank(item.getStructureName())).collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getStructureName));
        if (CollectionUtils.isEmpty(mapGroupByStructureName)) {
            return Collections.emptyMap();
        }
        //得到结构主花纹下最大可用模具数-按物料描述分组取最大
        Map<String, Long> maxEnableMouldNumberMap = calculateMaxEnableMouldNumber(productionContext);
        Map<String, MonthPlanStructureLhRatioVo> minLhRatioMap = getMinLhRatioMap(productionContext);
        ProductionCapacityParamConfiguration paramConfiguration = productionContext.getBaseDataContainer().getParamConfiguration();
        Integer minProductionDays = paramConfiguration.getMinProductionDays();
        Integer minAllocationDays = paramConfiguration.getMinAllocationDays();
        mapGroupByStructureName.forEach((structureName, groupDatas) -> {
            ProductionPlanGroupInfo groupInfo = ProductionPlanGroupInfo.createInitByGroupList(productionContext, structureName, productionContext.getProductType(), groupDatas);
            //20260206 特殊材料的结构检测
            groupInfo.checkSpecialMaterialData(productionContext);
            //是否零度结构
            boolean zeroRack = groupDatas.stream()
                    .anyMatch(plan -> YesOrNoEnum.YES.getCode().equals(plan.getIsZeroRack()));
            groupInfo.setIsZero(zeroRack ? YesOrNoEnum.YES.getCode() : YesOrNoEnum.NO.getCode());
            Map<String, MonthPlanStructureLhRatioVo> cxMachineLhRationMap = getCxMachineLhRationMap(structureName, productionContext);
            if (CollectionUtils.isEmpty(cxMachineLhRationMap)) {
                log.info(TbrProductionGroupLogRecorder.addGroupLhRatioEmptyLog(productionContext, structureName));
                groupInfo.setNoProductionNoCxMachineLhRatio();
            }
            groupInfo.setCxMachineLhRationMap(cxMachineLhRationMap);
            // 计算结构净需求量
            groupInfo.setSumPlanQty(calculateMaxMouldCapacity(groupInfo, groupDatas, maxEnableMouldNumberMap, productionContext));
            //（4）粗算每个结构需要的成型机台数，公式核算：成型机台数 = 结构净需求量/硫化机数*日硫化量<取最小>*月度天数<工作日历>；
            if (!minLhRatioMap.containsKey(structureName)) {
                groupInfo.setNeedCxCapacityMachineCount(BigDecimal.ZERO);
            } else {
                // 结构最小硫化配比
                groupInfo.setMinLhMachineCount(minLhRatioMap.get(structureName).getLhMachineMaxQty());
                groupInfo.setNeedCxCapacityMachineCount(calculateCxMachineNumber(groupInfo, productionContext));
            }
            if (BigDecimal.ZERO.equals(groupInfo.getNeedCxCapacityMachineCount())) {
                setAllocationZero(groupInfo);
            } else {
                log.info(TbrProductionGroupLogRecorder.addGroupCalculateCxMachineCountLog(
                        productionContext, structureName, groupInfo.getSumPlanQty(), groupInfo.getMinLhMachineCount(),
                        groupInfo.getMinLhDayCapacityQty(), groupInfo.getTheoryDays(), groupInfo.getNeedCxCapacityMachineCount()));
                //分配天数为零，或是小于最小要求天数，则设置不排产
                if (null != minProductionDays && groupInfo.getTheoryDays() < minProductionDays) {
                    groupInfo.setNoProductionNoReachMinProductionDays(minProductionDays);
                } else {
                    if (null != minAllocationDays && groupInfo.getTheoryDays() < minAllocationDays) {
                        if (!groupInfo.isSpecialMaterial()) {// 特殊材料结构理论分配天数低于5天时，不能强制拉到5天。
                            groupInfo.setTheoryDays(minAllocationDays);
                            groupInfo.setLeftOverNeedAllocationDays(minAllocationDays);
                            //重新计算估算的机台数
                            BigDecimal newNeedCxCapacityMachineCount = BigDecimal.valueOf(minAllocationDays).divide(BigDecimal.valueOf(productionContext.getMonthDays()), 1, RoundingMode.UP);
                            groupInfo.setNeedCxCapacityMachineCount(newNeedCxCapacityMachineCount);
                            log.info(TbrProductionGroupLogRecorder.addGroupCalculateCxMachineCountLog(
                                    productionContext, structureName, groupInfo.getSumPlanQty(), groupInfo.getMinLhMachineCount(),
                                    groupInfo.getMinLhDayCapacityQty(), groupInfo.getTheoryDays(), groupInfo.getNeedCxCapacityMachineCount()));
                        }
                    }
                }
            }
            groupInfoMap.put(structureName, groupInfo);
        });
        return groupInfoMap;
    }

    /**
     * @param structureName
     * @param productionContext
     * @return
     */
    private Map<String, MonthPlanStructureLhRatioVo> getCxMachineLhRationMap(String structureName, TbrProductionContext productionContext) {
        List<MonthPlanStructureLhRatioVo> structureLhRatioList = productionContext.getBaseDataContainer().getStructureLhRatioList();
        if (CollectionUtils.isEmpty(structureLhRatioList)) {
            return Collections.emptyMap();
        }
        Map<String, MonthPlanStructureLhRatioVo> allCxLhRatioMap = structureLhRatioList.stream().filter(item -> structureName.equals(item.getStructureName())).collect(Collectors.toMap(MonthPlanStructureLhRatioVo::getCxMachineTypeCode, Function.identity(), (before, after) -> after));
        return CollectionUtils.isEmpty(allCxLhRatioMap) ? Collections.emptyMap() : allCxLhRatioMap;
    }

    /**
     * @param groupInfo
     */
    private void setAllocationZero(ProductionPlanGroupInfo groupInfo) {
        groupInfo.setNeedCxCapacityMachineCount(BigDecimal.ZERO);
        groupInfo.setTheoryDays(0);
        groupInfo.setLeftOverNeedAllocationDays(0);
        groupInfo.setIsAllocationFinish(YesOrNoEnum.YES.getValue());
    }

    /**
     * @param productionContext
     * @return
     */
    private Map<String, MonthPlanStructureLhRatioVo> getMinLhRatioMap(TbrProductionContext productionContext) {
        List<MonthPlanStructureLhRatioVo> structureLhRatioList = productionContext.getBaseDataContainer().getStructureLhRatioList();
        if (CollectionUtils.isEmpty(structureLhRatioList)) {
            return Collections.emptyMap();
        }
        Map<String, MonthPlanStructureLhRatioVo> resultMap = new HashMap<>();
        for (MonthPlanStructureLhRatioVo vo : structureLhRatioList) {
            String structureName = vo.getStructureName();
            Integer currentQty = vo.getLhMachineMaxQty();
            if (StringUtils.isBlank(structureName) || null == currentQty) {
                continue;
            }
            MonthPlanStructureLhRatioVo existing = resultMap.get(structureName);
            if (existing == null) {
                // 第一次出现，直接放入
                resultMap.put(structureName, vo);
            } else {
                // 已存在，比较lhMachineMaxQty
                Integer existingQty = existing.getLhMachineMaxQty();
                if (currentQty < existingQty) {
                    // 新的值更小，替换
                    resultMap.put(structureName, vo);
                }
            }
        }
        return resultMap;

    }

    /**
     * （4）粗算每个结构需要的成型机台数，公式核算：成型机台数 = 结构净需求量/硫化机数*日硫化量<取最小>*月度天数<工作日历>；
     *
     * @param groupInfo         结构分组
     * @param productionContext 排产上下文
     * @return 成型机台数
     */
    private BigDecimal calculateCxMachineNumber(ProductionPlanGroupInfo groupInfo, TbrProductionContext productionContext) {
        if (null == groupInfo.getSumPlanQty()
                || null == groupInfo.getMinLhDayCapacityQty()
                || null == groupInfo.getMinLhMachineCount()
                || null == productionContext.getMaxProductionDays()
                || 0 == groupInfo.getSumPlanQty()
                || 0 == groupInfo.getMinLhDayCapacityQty()
                || 0 == groupInfo.getMinLhMachineCount()
                || 0 == productionContext.getMaxProductionDays()
        ) {
            return BigDecimal.ZERO;
        }
        // 有效总需求量 / (SKU最小日硫化量 * 2 * 结构最小硫化配比 * 月度可排产天数),两位小数
        BigDecimal sumPlanQty = BigDecimal.valueOf(groupInfo.getSumPlanQty());
        BigDecimal minLhDayCapacityQty = BigDecimal.valueOf(groupInfo.getMinLhDayCapacityQty());
        BigDecimal minLhMachineCount = BigDecimal.valueOf(groupInfo.getMinLhMachineCount());
        BigDecimal maxProductionDays = BigDecimal.valueOf(productionContext.getMaxProductionDays());
        //单台成型日产能 = 最低硫化机台数 * 最小硫化量(单模) * 2
        BigDecimal singleMinDayCapacity = minLhMachineCount.multiply(minLhDayCapacityQty).multiply(BigDecimal.valueOf(ProductionConstant.DOUBLE_MOULD_PRODUCTION));
        //理论需排产天数
        Integer theoryDays = sumPlanQty.divide(singleMinDayCapacity, 0, RoundingMode.UP).intValue();
        //单台成型月产能 = 单台成型日产能 * 月份可排产天数(排除停产日)
        BigDecimal singleCxMonthCapacity = singleMinDayCapacity.multiply(maxProductionDays);
        BigDecimal machineCount = sumPlanQty.divide(singleCxMonthCapacity, 2, RoundingMode.HALF_UP);
        //取整数部分，向下取整
        BigDecimal integerPart = machineCount.setScale(0, RoundingMode.DOWN);
        //小数部分
        BigDecimal decimalPart = machineCount.subtract(integerPart);
        if (decimalPart.compareTo(BigDecimal.valueOf(ProductionConstant.REPAIR_WHOLE)) > BigDecimal.ZERO.intValue()) {
            integerPart = integerPart.add(BigDecimal.ONE);
            theoryDays = integerPart.multiply(maxProductionDays).intValue();
            groupInfo.setTheoryDays(theoryDays);
            groupInfo.setLeftOverNeedAllocationDays(theoryDays);
            return integerPart;
        }
        groupInfo.setTheoryDays(theoryDays);
        groupInfo.setLeftOverNeedAllocationDays(theoryDays);
        return machineCount.setScale(1, RoundingMode.UP);
    }

    /**
     * 得到结构主花纹下最大可用模具数-按物料描述分组取最大
     *
     * @param productionContext 排产上下文
     * @return 最大可用模具数-按物料描述分组取最大
     */
    private Map<String, Long> calculateMaxEnableMouldNumber(TbrProductionContext productionContext) {
        Map<String, List<MonthPlanProductMouldInfoVo>> skuMouldRelationMap = productionContext.getBaseDataContainer().getSkuMouldRelationMap();
        if (CollectionUtils.isEmpty(skuMouldRelationMap)) {
            return Collections.emptyMap();
        }
        //按结构+主花纹分组模具信息
        Map<String, List<MonthPlanProductMouldInfoVo>> groupMouldMap = skuMouldRelationMap.values().stream().flatMap(Collection::stream)
                .filter(item -> StringUtils.isNotBlank(item.getStructureName()) && StringUtils.isNotBlank(item.getMainPattern()))
                .collect(Collectors.groupingBy(MonthPlanProductMouldInfoVo::getStructureNameAndMainPattern));
        if (CollectionUtils.isEmpty(groupMouldMap)) {
            return Collections.emptyMap();
        }
        Map<String, Long> result = Maps.newHashMap();
        Map<String, MouldAllocationInfoVo> structureMainPatternAllocationLimit = productionContext.getBaseDataContainer().getGroupMainPatternAllocationLimitMap();
        groupMouldMap.forEach((groupKey, mouldInfoVoList) -> {
            Map<String, Long> materialGroup = mouldInfoVoList.stream().filter(item -> StringUtils.isNotBlank(item.getMaterialDesc())).collect(Collectors.groupingBy(MonthPlanProductMouldInfoVo::getMaterialDesc, Collectors.counting()));
            if (CollectionUtils.isEmpty(materialGroup)) {
                result.put(groupKey, 0L);
                return;
            }
            long maxMouldNumber = materialGroup.values().stream().max(Comparator.comparingLong(Long::longValue)).orElse(0L);
            if (!structureMainPatternAllocationLimit.containsKey(groupKey)) {
                result.put(groupKey, maxMouldNumber);
                return;
            }
            //分配比例与最大数，二者取最小
            Integer limitNumber = structureMainPatternAllocationLimit.get(groupKey).getAllocationQty();
            if (null == limitNumber) {
                result.put(groupKey, maxMouldNumber);
                return;
            }
            long maxEnableMouldNumber = Math.min(maxMouldNumber, limitNumber);
            String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构+主花纹：%s 下理论最大模具数：%s 分配模具数：%s 最终最大可使用模具数 %s",
                    productionContext.getFactoryCode(), productionContext.getYear(), productionContext.getMonth(), productionContext.getMonthPlanVersion(), productionContext.getProductionVersion(),
                    groupKey, maxMouldNumber, limitNumber, maxEnableMouldNumber);
            log.info(logContent);
            result.put(groupKey, maxEnableMouldNumber);
        });
        return result;
    }

    /**
     * 计算模具最大产能
     *
     * @param groupInfo         结构分组
     * @param groupDatas        同结构需求计划
     * @param productionContext 排产上下文
     * @return 模具最大产能
     */
    private int calculateMaxMouldCapacity(ProductionPlanGroupInfo groupInfo, List<MonthPlanProductionRequirePlanVo> groupDatas, Map<String, Long> maxEnableMouldNumberMap, TbrProductionContext productionContext) {
        // 模具最大产能=日硫化量<取最小>*模具数/2 * 月度最大天数，若是共用模，合并计算；
        if (CollectionUtils.isEmpty(maxEnableMouldNumberMap)) {
            return 0;
        }
        //剔除不排产的计划
        List<MonthPlanProductionRequirePlanVo> productionPlanList = groupDatas.stream().filter(productionPlan -> YesOrNoEnum.YES.getCode().equals(productionPlan.getProductionFlag()) && StringUtils.isNotBlank(productionPlan.getMainPattern())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(productionPlanList)) {
            return 0;
        }
        groupInfo.setMinLhDayCapacityQty(calculateMinLhDayCapacityQty(productionPlanList));
        if (0 == groupInfo.getMinLhDayCapacityQty()) {
            return 0;
        }
        Map<String, List<MonthPlanProductionRequirePlanVo>> groupMap = productionPlanList.stream().collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getGroupKey));
        if (CollectionUtils.isEmpty(groupMap)) {
            return 0;
        }
        List<Long> totalMaxMouldCapacity = Lists.newArrayList();
        groupMap.forEach((groupKey, requirePlanList) -> {
            if (!maxEnableMouldNumberMap.containsKey(groupKey)) {
                return;
            }
            long maxMouldNumber = maxEnableMouldNumberMap.get(groupKey);
            long lhMachineCount = maxMouldNumber / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
            if (0 == lhMachineCount) {
                return;
            }
            //（1）计算结构向下主花纹模具的最大产能；模具最大产能=日硫化量<取最小>*模具数/2 * 月度最大天数，若是共用模，合并计算；
            long maxMouldCapacity = groupInfo.getMinLhDayCapacityQty() * lhMachineCount * ProductionConstant.DOUBLE_MOULD_PRODUCTION * productionContext.getMaxProductionDays();
            // SUM（结构向下主花纹对应的所有SKU的净需求量）
            long sumNetQty = requirePlanList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getCxCapacityRequireQty).sum();
            // （2）计算结构向下主花纹模具的最大可排产量 = MIN { SUM（结构向下主花纹对应的所有SKU的净需求量），主花纹模具的最大产能}；
            totalMaxMouldCapacity.add(Math.min(maxMouldCapacity, sumNetQty));
        });
        if (CollectionUtils.isEmpty(totalMaxMouldCapacity)) {
            return 0;
        }
        //（3）按结构汇总需求量 = SUM(结构向下主花纹模具最大可排产量)；
        BigDecimal result = BigDecimal.ZERO;
        for (long maxMouldCapacity : totalMaxMouldCapacity) {
            result = result.add(new BigDecimal(maxMouldCapacity));
        }
        return result.intValue();
    }

    /**
     * 计算结构最小日硫化量
     *
     * @param groupDatas 结构计划列表
     * @return 结构最小日硫化量
     */
    private int calculateMinLhDayCapacityQty(List<MonthPlanProductionRequirePlanVo> groupDatas) {
        //最小日硫化量
        return groupDatas.stream().filter(item -> null != item.getDayVulcanizationQty()).mapToInt(MonthPlanProductionRequirePlanVo::getDayVulcanizationQty).min().orElse(0);
    }
}
