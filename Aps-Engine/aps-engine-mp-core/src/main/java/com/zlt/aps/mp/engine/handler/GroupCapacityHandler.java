package com.zlt.aps.mp.engine.handler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.api.domain.entity.MpSkuMoldCapacityAllocateLog;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.daylimit.MouldAllocationInfoVo;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductMouldInfoVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanStructureLhRatioVo;
import com.zlt.aps.mp.engine.enums.CycleProductionModeEnum;
import com.zlt.aps.mp.engine.logrecorder.PlanRequireLogRecorder;
import com.zlt.aps.mp.engine.logrecorder.TbrProductionGroupLogRecorder;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.engine.scheduling.cxcapacity.ProductionCapacityParamConfiguration;
import lombok.RequiredArgsConstructor;
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
 * 分组对象产能处理器
 * 结构粗算所需成型机台数，同时会构建出分组信息对象
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
@RequiredArgsConstructor
public class GroupCapacityHandler {

    private final MoldCapacityLimitAllocateHandler moldCapacityLimitAllocateHandler;

    /**
     * 6、粗算结构成型机台数
     *
     * @param productionContext     排产上下文
     * @param requirePlanList       需求计划
     * @param isHandlerMoldCapacity 是否需要处理模具产能 true 表示要处理 false表示不用处理
     * @return 结构分组
     */
    public Map<String, ProductionPlanGroupInfo> calculateStructureCxMachineNumber(TbrProductionContext productionContext, List<MonthPlanProductionRequirePlanVo> requirePlanList, boolean isHandlerMoldCapacity) {
        if (CollectionUtils.isEmpty(requirePlanList)) {
            return Collections.emptyMap();
        }
        Map<String, ProductionPlanGroupInfo> allGroupInfo = buildGroupPlanInfoMap(productionContext, requirePlanList, isHandlerMoldCapacity);
        //20260711+ 根据周期结构排产模式设置排产量
        adjustSumPlanQtyByCycleProductionMode(productionContext, allGroupInfo);
        return allGroupInfo;
    }

    /**
     * @param productionContext     排产上下文
     * @param requirePlanList       需求计划
     * @param isHandlerMoldCapacity 是否处理模具产能
     * @return
     */
    private Map<String, ProductionPlanGroupInfo> buildGroupPlanInfoMap(TbrProductionContext productionContext, List<MonthPlanProductionRequirePlanVo> requirePlanList, boolean isHandlerMoldCapacity) {
        if (CollectionUtils.isEmpty(requirePlanList)) {
            return Collections.emptyMap();
        }
        Map<String, ProductionPlanGroupInfo> groupInfoMap = Maps.newHashMap();
        // 需求计划按照结构分组
        Map<String, List<MonthPlanProductionRequirePlanVo>> mapGroupByStructureName = requirePlanList.stream().filter(item -> StringUtils.isNotBlank(item.getStructureName())).collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getStructureName));
        if (CollectionUtils.isEmpty(mapGroupByStructureName)) {
            return Collections.emptyMap();
        }
        ProductionCapacityParamConfiguration paramConfiguration = productionContext.getBaseDataContainer().getParamConfiguration();
        BigDecimal heightPrioritySkuProductionRatio = paramConfiguration.getHeightPriorityProductionMode();
        //得到结构主花纹下最大可用模具数-按物料描述分组取最大
        Map<String, Integer> maxEnableMouldNumberMap = calculateMaxEnableMouldNumber(productionContext);
        Map<String, MonthPlanStructureLhRatioVo> minLhRatioMap = getMinLhRatioMap(productionContext);
        List<MpSkuMoldCapacityAllocateLog> moldCapacityResultList = Lists.newArrayList();
        Integer minProductionDays = paramConfiguration.getMinProductionDays();
        mapGroupByStructureName.forEach((structureName, skuPlanList) -> {
            //创建分组对象信息--初始
            ProductionPlanGroupInfo groupInfo = ProductionPlanGroupInfo.createInitByGroupList(productionContext, structureName, productionContext.getProductType(), skuPlanList);
            groupInfoMap.put(structureName, groupInfo);
            //20260206 特殊材料的结构检测
            groupInfo.checkSpecialMaterialData(productionContext);
            Map<String, MonthPlanStructureLhRatioVo> cxMachineLhRationMap = getCxMachineLhRationMap(structureName, productionContext);
            if (CollectionUtils.isEmpty(cxMachineLhRationMap)) {
                log.info(TbrProductionGroupLogRecorder.addGroupLhRatioEmptyLog(productionContext, structureName));
                groupInfo.setNoProductionNoCxMachineLhRatio();
            }
            groupInfo.setCxMachineLhRationMap(cxMachineLhRationMap);
            //20260515+ 增加模具产能分摊计算
            if (isHandlerMoldCapacity) {
                List<MpSkuMoldCapacityAllocateLog> singleGroupResultList = moldCapacityLimitAllocateHandler.moldCapacityAllocate(groupInfo, skuPlanList, maxEnableMouldNumberMap, productionContext);
                if (!CollectionUtils.isEmpty(singleGroupResultList)) {
                    moldCapacityResultList.addAll(singleGroupResultList);
                }
            }
            //计算结构净需求量总量，用以评估成型机台数
            groupInfo.setSumPlanQty(calculateEffectiveRequireByMouldCapacity(groupInfo, skuPlanList, maxEnableMouldNumberMap, productionContext));
            //20260430+ 设置是否按高优先级先排产，按结构高优先级需求占比
            BigDecimal groupHeightRequireRatio = groupInfo.getHeightRequireRatio();
            PlanRequireLogRecorder.addGroupHeightRequireRatioLog(productionContext, groupInfo.getGroupName(), groupHeightRequireRatio, groupInfo.getSumPlanQty(), groupInfo.getSumHeightRequireQty());
            skuPlanList.forEach(singlePlan -> {
                if (groupHeightRequireRatio.compareTo(heightPrioritySkuProductionRatio) >= BigDecimal.ZERO.intValue()) {
                    singlePlan.setIsPriorityHeight(YesOrNoEnum.YES.getValue());
                }
            });
            //（4）粗算每个结构需要的成型机台数，公式核算：成型机台数 = 结构净需求量/硫化机数<最小配比>*日硫化量<取最小>*月度天数<工作日历>；
            if (!minLhRatioMap.containsKey(structureName)) {
                groupInfo.setNeedCxCapacityMachineCount(BigDecimal.ZERO);
            } else {
                // 结构最小硫化配比
                groupInfo.setMinLhMachineCount(minLhRatioMap.get(structureName).getLhMachineMaxQty());
                groupInfo.setNeedCxCapacityMachineCount(calculateCxMachineNumber(groupInfo, productionContext));
            }
            Integer minAllocationDays = groupInfo.getMinAllocationDays(productionContext);
            //估算的机台数为零，则设置分配完成
            if (BigDecimal.ZERO.equals(groupInfo.getNeedCxCapacityMachineCount())) {
                setAllocationZero(groupInfo);
                return;
            }
            //加入流程日志，不打印日志文件
            TbrProductionGroupLogRecorder.addGroupCalculateCxMachineCountLog(productionContext, groupInfo);
            //分配天数为零，或是小于最小要求天数，则设置不排产
            if (null != minProductionDays && groupInfo.getTheoryDays() < minProductionDays) {
                groupInfo.setNoProductionNoReachMinProductionDays(minProductionDays);
                return;
            }
            // 特殊材料结构理论分配天数低于5天时，不能强制拉到5天。
            if (null != minAllocationDays && groupInfo.getTheoryDays() < minAllocationDays) {
                groupInfo.setTheoryDays(minAllocationDays);
                groupInfo.setLeftOverNeedAllocationDays(minAllocationDays);
                //重新计算估算的机台数
                BigDecimal newNeedCxCapacityMachineCount = BigDecimal.valueOf(minAllocationDays).divide(BigDecimal.valueOf(productionContext.getMonthDays()), 1, RoundingMode.UP);
                groupInfo.setNeedCxCapacityMachineCount(newNeedCxCapacityMachineCount);
                TbrProductionGroupLogRecorder.addGroupCalculateCxMachineCountLog(productionContext, groupInfo);
            }
        });
        //20260515+ 保存模具产能受限信息
        moldCapacityLimitAllocateHandler.saveMoldCapacityResult(productionContext, moldCapacityResultList);
        return groupInfoMap;
    }

    /**
     * 调整周期结构的总排产量：即净需求量，根据周期结构的排产模式
     * 1、只排产高，则净需求 = 高需求量
     * 2、只排产高+ 中，则净需求 = 实单需求量(高+中)
     * 3、其它不用调整
     *
     * @param productionContext 排产上下文
     * @param allGroupInfoMap   所有分组信息对象集合
     */
    private void adjustSumPlanQtyByCycleProductionMode(TbrProductionContext productionContext, Map<String, ProductionPlanGroupInfo> allGroupInfoMap) {
        if (CollectionUtils.isEmpty(allGroupInfoMap)) {
            return;
        }
        CycleProductionModeEnum cycleProductionMode = productionContext.getBaseDataContainer().getParamConfiguration().getCycleProductionMode();
        allGroupInfoMap.forEach((groupName, groupInfo) -> {
            if (!groupInfo.isCycleType()) {
                //非周期结构跳过
                return;
            }
            //周期结构排产模式：高+中+周期储备
            if (CycleProductionModeEnum.ALL == cycleProductionMode) {
                return;
            }
            handlerCycleGroupByProductionMode(groupInfo, cycleProductionMode);
        });
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
     * 需要分配的机台数为零，则直接标记分配完成
     *
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
                continue;
            }
            // 已存在，比较lhMachineMaxQty
            Integer existingQty = existing.getLhMachineMaxQty();
            if (currentQty < existingQty) {
                // 新的值更小，替换
                resultMap.put(structureName, vo);
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
        //是否有效月份，排产天数不可为空或是小于零
        boolean isEffectiveMonth = null == productionContext.getMaxProductionDays() || productionContext.getMaxProductionDays() <= BigDecimal.ZERO.intValue();
        if (!isEffectiveGroupRequire(groupInfo) || isEffectiveMonth) {
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
    private Map<String, Integer> calculateMaxEnableMouldNumber(TbrProductionContext productionContext) {
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
        Map<String, Integer> result = Maps.newHashMap();
        Map<String, MouldAllocationInfoVo> structureMainPatternAllocationLimit = productionContext.getBaseDataContainer().getGroupMainPatternAllocationLimitMap();
        groupMouldMap.forEach((groupKey, mouldInfoVoList) -> {
            Map<String, Long> materialGroup = mouldInfoVoList.stream().filter(item -> StringUtils.isNotBlank(item.getMaterialDesc())).collect(Collectors.groupingBy(MonthPlanProductMouldInfoVo::getMaterialDesc, Collectors.counting()));
            if (CollectionUtils.isEmpty(materialGroup)) {
                result.put(groupKey, BigDecimal.ZERO.intValue());
                return;
            }
            Integer maxMouldNumber = materialGroup.values().stream().max(Comparator.comparingLong(Long::longValue)).orElse(BigDecimal.ZERO.longValue()).intValue();
            if (!structureMainPatternAllocationLimit.containsKey(groupKey)) {
                result.put(groupKey, maxMouldNumber.intValue());
                return;
            }
            //分配比例与最大数，二者取最小
            Integer limitNumber = structureMainPatternAllocationLimit.get(groupKey).getAllocationQty();
            if (null == limitNumber) {
                result.put(groupKey, maxMouldNumber);
                return;
            }
            Integer maxEnableMouldNumber = Math.min(maxMouldNumber, limitNumber);
            log.info(TbrProductionGroupLogRecorder.addGroupMainPatternMaxMouldNumberLog(productionContext, groupKey, limitNumber, maxEnableMouldNumber));
            result.put(groupKey, maxEnableMouldNumber);
        });
        return result;
    }

    /**
     * 计算有效净需求量，需要考虑模具最大产能
     * 模具最大产能=日硫化量<取最小>*模具数/2 * 月度最大天数，若是共用模，合并计算；
     *
     * @param groupInfo         结构分组
     * @param skuPlanList       同结构需求计划
     * @param productionContext 排产上下文
     * @return 模具最大产能
     */
    private Integer calculateEffectiveRequireByMouldCapacity(ProductionPlanGroupInfo groupInfo, List<MonthPlanProductionRequirePlanVo> skuPlanList, Map<String, Integer> maxEnableMouldNumberMap, TbrProductionContext productionContext) {
        //没有模具，则为零
        if (CollectionUtils.isEmpty(maxEnableMouldNumberMap)) {
            return BigDecimal.ZERO.intValue();
        }
        //剔除不排产的计划 20260626+ 不使用productionFlag(因周期结构不在月周期清单但为续作结构有可能因切换结构限制导致需要延长)
        List<MonthPlanProductionRequirePlanVo> productionPlanList = skuPlanList.stream().filter(productionPlan -> YesOrNoEnum.YES.getCode().equals(productionPlan.getIsProduction()) && StringUtils.isNotBlank(productionPlan.getMainPattern())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(productionPlanList)) {
            return BigDecimal.ZERO.intValue();
        }
        //最小日硫化量
        groupInfo.setMinLhDayCapacityQty(calculateMinLhDayCapacityQty(productionPlanList));
        if (BigDecimal.ZERO.intValue() == groupInfo.getMinLhDayCapacityQty()) {
            return BigDecimal.ZERO.intValue();
        }
        //按主花纹分组
        Map<String, List<MonthPlanProductionRequirePlanVo>> groupMap = productionPlanList.stream().collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getGroupKey));
        if (CollectionUtils.isEmpty(groupMap)) {
            return BigDecimal.ZERO.intValue();
        }
        List<Integer> totalMaxMouldCapacity = Lists.newArrayList();
        //20260430+ 高优先级量对需求量的占比计算需要
        List<Integer> totalHeightRequire = Lists.newArrayList();
        addMainPatternGroupInfo(productionContext, groupInfo, groupMap, maxEnableMouldNumberMap, totalMaxMouldCapacity, totalHeightRequire);
        //20260430+ 增加高优先级需求量
        if (!CollectionUtils.isEmpty(totalHeightRequire)) {
            Integer sumHeightRequireQty = totalHeightRequire.stream().mapToInt(Integer::intValue).sum();
            groupInfo.setSumHeightRequireQty(sumHeightRequireQty);
        } else {
            groupInfo.setSumHeightRequireQty(BigDecimal.ZERO.intValue());
        }
        if (CollectionUtils.isEmpty(totalMaxMouldCapacity)) {
            return BigDecimal.ZERO.intValue();
        }
        //（3）按结构汇总需求量 = SUM(结构向下主花纹模具最大可排产量)；
        Integer sumEffectiveQty = totalMaxMouldCapacity.stream().mapToInt(Integer::intValue).sum();
        //20260325+ 周期结构--获取比例 20260709+ 周期结构排产模式不是3，则周期储备比例为0
        return handlerEffectiveRequireByCycleType(productionContext, groupInfo, sumEffectiveQty, productionPlanList);
    }

    /**
     * 增加在groupInfo内，以主花纹为分组的分组信息
     * 当前为 最大模具产能和高优先级需求量
     * 结构向下：主花纹模具数 = Max(主花纹Sku模具数)
     * 结构向下：主花纹模具满产能 = Min(Sku日硫化量) * 主花纹模具数/2 * 月度最大天数
     * 结构向下：主花纹净需求量 = Sum(Sku净需求量)
     * 则结构向下主花纹最大模具产能 = Min(主花纹模具满产能,主花纹净需求量)
     * 20260709+ 周期结构需要结合周期结构的排产模式，进行计算量
     *
     * @param productionContext       排产上下文
     * @param groupInfo               分组信息对象
     * @param groupMap                主花纹分组信息
     * @param maxEnableMouldNumberMap 结构+主花纹的模具信息
     * @param totalMaxMouldCapacity   结构内各主花纹模具最大产能
     * @param totalHeightRequire      结构内各主花纹高优先级需求量
     */
    private void addMainPatternGroupInfo(TbrProductionContext productionContext,
                                         ProductionPlanGroupInfo groupInfo,
                                         Map<String, List<MonthPlanProductionRequirePlanVo>> groupMap,
                                         Map<String, Integer> maxEnableMouldNumberMap,
                                         List<Integer> totalMaxMouldCapacity,
                                         List<Integer> totalHeightRequire) {
        if (CollectionUtils.isEmpty(groupMap) || null == totalMaxMouldCapacity || null == totalHeightRequire || null == groupInfo) {
            return;
        }
        CycleProductionModeEnum cycleProductionMode = productionContext.getBaseDataContainer().getParamConfiguration().getCycleProductionMode();
        groupMap.forEach((groupKey, requirePlanList) -> {
            if (!maxEnableMouldNumberMap.containsKey(groupKey)) {
                return;
            }
            Integer maxMouldNumber = maxEnableMouldNumberMap.get(groupKey);
            Integer lhMachineCount = maxMouldNumber / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
            if (BigDecimal.ZERO.intValue() == lhMachineCount) {
                return;
            }
            //（1）计算结构向下主花纹模具的最大产能；模具最大产能=日硫化量<取最小>*模具数/2 * 月度最大天数，若是共用模，合并计算；
            Integer maxMouldCapacity = groupInfo.getMinLhDayCapacityQty() * lhMachineCount * ProductionConstant.DOUBLE_MOULD_PRODUCTION * productionContext.getMaxProductionDays();
            // SUM（结构向下主花纹对应的所有SKU的净需求量）20260709+ 周期结构排产模式支持
            Integer sumNetQty = getSumNetQty(cycleProductionMode, groupInfo, requirePlanList);
            //实单量
            Integer sumActualQuantity = requirePlanList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getActualQuantity).sum();
            PlanRequireLogRecorder.addRequireEstimateInfoLog(productionContext, groupKey, sumActualQuantity, sumNetQty, maxMouldCapacity);
            // （2）计算结构向下主花纹模具的最大可排产量 = MIN { SUM（结构向下主花纹对应的所有SKU的净需求量），主花纹模具的最大产能}；
            totalMaxMouldCapacity.add(Math.min(maxMouldCapacity, sumNetQty));

            //20260430+ 高优先级需求量
            Integer sumHeightQty = requirePlanList.stream().filter(x -> x.getHeightCapacityRequireQty() != null).mapToInt(MonthPlanProductionRequirePlanVo::getHeightCapacityRequireQty).sum();
            totalHeightRequire.add(Math.min(sumHeightQty, maxMouldCapacity));
        });
    }

    /**
     * 周期结构需要处理周期排产储备量
     * 20260325+ 周期结构--获取比例，计算周期储备总量在实单*比例 上下
     * 20260709+ 周期结构排产模式不是3，则周期储备比例为0
     *
     * @param productionContext    排产上下文
     * @param groupInfo            分组计划信息
     * @param originEffectiveQty   不处理前的有效总需求
     * @param effectiveRequireList 分组内有效计划信息
     * @return
     */
    private Integer handlerEffectiveRequireByCycleType(TbrProductionContext productionContext,
                                                       ProductionPlanGroupInfo groupInfo,
                                                       Integer originEffectiveQty,
                                                       List<MonthPlanProductionRequirePlanVo> effectiveRequireList) {
        //非周期结构
        if (!groupInfo.isCycleType()) {
            return originEffectiveQty;
        }
        ProductionCapacityParamConfiguration paramConfiguration = productionContext.getBaseDataContainer().getParamConfiguration();
        CycleProductionModeEnum productionMode = paramConfiguration.getCycleProductionMode();
        if (CycleProductionModeEnum.ALL != productionMode) {
            //周期结构排产模式：高、高+中
            groupInfo.setMaxCycleQty(BigDecimal.ZERO.intValue());
            return originEffectiveQty;
        }
        //高+中+周期储备，则需要看周期结构比例
        Integer percent = paramConfiguration.getReservePercent();
        if (null == percent || percent < BigDecimal.ZERO.intValue()) {
            groupInfo.setMaxCycleQty(BigDecimal.ZERO.intValue());
            return originEffectiveQty;
        }
        //所有的实单
        Integer sumActualQuantity = getAllActualQuantity(effectiveRequireList);
        //得到需求上限值(实单 + 实单 * 比例)
        Integer addPercent = percent + ProductionConstant.PERCENTAGE;
        Integer maxCycleQty = BigDecimal.valueOf(sumActualQuantity).multiply(BigDecimal.valueOf(addPercent))
                .divide(BigDecimal.valueOf(ProductionConstant.PERCENTAGE), BigDecimal.ZERO.intValue(), RoundingMode.UP).intValue();
        //记录日志
        Integer sumAllNetQty = effectiveRequireList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getNetQty).sum();
        PlanRequireLogRecorder.addGroupRequireEstimateInfoLog(productionContext, percent, groupInfo.getGroupName(), sumActualQuantity, sumAllNetQty, originEffectiveQty, maxCycleQty);
        //不可超过最大需求量，二者取最小
        originEffectiveQty = Math.min(originEffectiveQty, maxCycleQty);
        //实际储备量上限:不可为负数，可为零
        Integer realMaxCycleQty = originEffectiveQty - sumActualQuantity;
        realMaxCycleQty = Math.max(BigDecimal.ZERO.intValue(), realMaxCycleQty);
        groupInfo.setMaxCycleQty(realMaxCycleQty);
        return originEffectiveQty;
    }

    /**
     * 计算：分组名+主花纹的有效需求计划的净需求总量
     * 非周期结构，所有净需求量
     * 周期结构：排产模式
     * 1、高，只排产高优先级量
     * 2、实单，只排产高+中优先级量
     * 3、也要排产储备，即高+中+周期储备，占产能
     *
     * @param cycleProductionMode              周期结构排产模式
     * @param groupInfo                        分组信息对象
     * @param groupAndMainPatternEffectiveList 分组下某个主花纹的有效净需求集合
     * @return
     */
    private Integer getSumNetQty(CycleProductionModeEnum cycleProductionMode, ProductionPlanGroupInfo groupInfo, List<MonthPlanProductionRequirePlanVo> groupAndMainPatternEffectiveList) {
        if (CollectionUtils.isEmpty(groupAndMainPatternEffectiveList)) {
            return BigDecimal.ZERO.intValue();
        }
        if (!groupInfo.isCycleType()) {
            //非周期结构，直接所有净需求量
            return groupAndMainPatternEffectiveList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getCxCapacityRequireQty).sum();
        }
        //周期结构，只排高
        if (CycleProductionModeEnum.ONLY_HIGH == cycleProductionMode) {
            return groupAndMainPatternEffectiveList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getHeightCapacityRequireQty).sum();
        }
        //周期结构，只排实单，即高+中
        if (CycleProductionModeEnum.ONLY_ACTUAL == cycleProductionMode) {
            return groupAndMainPatternEffectiveList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getActualQuantity).sum();
        }
        //周期结构，排高+中+周期储备
        return groupAndMainPatternEffectiveList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getCxCapacityRequireQty).sum();
    }

    /**
     * 周期结构根据排产模式设置排产量
     *
     * @param groupInfo           分组信息对象
     * @param cycleProductionMode 排产模式
     */
    private void handlerCycleGroupByProductionMode(ProductionPlanGroupInfo groupInfo, CycleProductionModeEnum cycleProductionMode) {
        if (null == groupInfo || null == cycleProductionMode) {
            return;
        }
        List<MonthPlanProductionRequirePlanVo> groupPlanData = groupInfo.getGroupPlanData();
        if (CollectionUtils.isEmpty(groupPlanData)) {
            return;
        }
        List<MonthPlanProductionRequirePlanVo> effectiveList = groupPlanData.stream().filter(single -> YesOrNoEnum.YES.getCode().equals(single.getIsProduction())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(effectiveList)) {
            return;
        }
        Map<String, List<MonthPlanProductionRequirePlanVo>> skuGroupMap = effectiveList.stream().collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc));
        skuGroupMap.forEach((materialDesc, skuPlanList) -> {
            if (CycleProductionModeEnum.ONLY_HIGH == cycleProductionMode) {
                resetProductionQtyByHigh(skuPlanList);
                return;
            }
            if (CycleProductionModeEnum.ONLY_ACTUAL == cycleProductionMode) {
                resetProductionQtyByActual(skuPlanList);
                return;
            }
        });
    }

    /**
     * 只排产高优先级量：
     * 将排产净需求 = 高优先级需求量
     *
     * @param singleSkuList
     */
    private void resetProductionQtyByHigh(List<MonthPlanProductionRequirePlanVo> singleSkuList) {
        if (CollectionUtils.isEmpty(singleSkuList)) {
            return;
        }
        //提取有高需求量的
        List<MonthPlanProductionRequirePlanVo> productionModeList = singleSkuList.stream().filter(singlePlan -> singlePlan.getOriginHeightProductionQty() > BigDecimal.ZERO.intValue()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(productionModeList)) {
            resetProductionQtyZero(singleSkuList);
            return;
        }
        //高需求量，没有的净需求量置为零
        List<MonthPlanProductionRequirePlanVo> needResetZeroNetRequireList = singleSkuList.stream().filter(singlePlan -> singlePlan.getOriginHeightProductionQty() <= BigDecimal.ZERO.intValue()).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(needResetZeroNetRequireList)) {
            resetProductionQtyZero(needResetZeroNetRequireList);
        }
        //处理有高需求量的净需求量
        Integer minQty = productionModeList.get(BigDecimal.ZERO.intValue()).getMinProductionQty();
        Integer sumQty = productionModeList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getOriginHeightProductionQty).sum();
        Integer differenceValue = minQty - sumQty;
        productionModeList.forEach(single -> {
            //净排产量 = 高优先级量
            single.setOriginProductionQty(single.getOriginHeightProductionQty());
            single.setProductionQty(single.getOriginHeightProductionQty());
        });
        if (differenceValue <= BigDecimal.ZERO.intValue()) {
            return;
        }
        //最小批量处理
        productionModeList.forEach(single -> single.setIsProductionBySum(YesOrNoEnum.YES.getValue()));
        MonthPlanProductionRequirePlanVo first = productionModeList.get(BigDecimal.ZERO.intValue());
        Integer productionQty = first.getOriginProductionQty() + differenceValue;
        first.setOriginProductionQty(productionQty);
        first.setProductionQty(productionQty);
    }

    /**
     * 只排产实单：高 + 中
     * 将排产净需求 = 实单量
     *
     * @param singleSkuList
     */
    private void resetProductionQtyByActual(List<MonthPlanProductionRequirePlanVo> singleSkuList) {
        if (CollectionUtils.isEmpty(singleSkuList)) {
            return;
        }
        //提取有实单量的需求
        List<MonthPlanProductionRequirePlanVo> productionModeList = singleSkuList.stream().filter(singlePlan -> singlePlan.getActualQuantity() > BigDecimal.ZERO.intValue()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(productionModeList)) {
            resetProductionQtyZero(singleSkuList);
            return;
        }
        //实单没有的，净需求量置为零
        List<MonthPlanProductionRequirePlanVo> needResetZeroNetRequireList = singleSkuList.stream().filter(singlePlan -> singlePlan.getActualQuantity() <= BigDecimal.ZERO.intValue()).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(needResetZeroNetRequireList)) {
            resetProductionQtyZero(needResetZeroNetRequireList);
        }
        //处理实单净需求量
        Integer minQty = productionModeList.get(BigDecimal.ZERO.intValue()).getMinProductionQty();
        Integer sumQty = productionModeList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getActualQuantity).sum();
        //高优先级损耗量
        Integer highQty = productionModeList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getOriginHeightProductionQty).sum();
        Integer originHighQty = productionModeList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getHeightQty).sum();
        Integer highDifferenceValue = highQty - originHighQty;
        //去除高优先级差值
        Integer differenceValue = minQty - sumQty - highDifferenceValue;
        productionModeList.forEach(single -> {
            //净排产量 = 实单量(高+中)
            Integer initHighQty = single.getOriginHeightProductionQty();
            Integer actualQuantity = single.getActualQuantity();
            actualQuantity = Math.max(initHighQty, actualQuantity);
            single.setOriginProductionQty(actualQuantity);
            single.setProductionQty(actualQuantity);
        });
        if (differenceValue <= BigDecimal.ZERO.intValue() && highQty <= BigDecimal.ZERO.intValue()) {
            //奇数+3，偶数+2
            if ((sumQty & BigDecimal.ONE.intValue()) != BigDecimal.ZERO.intValue()) {
                differenceValue = ProductionConstant.ADD_LOSS_QTY_ODD_NUMBER;
            } else {
                differenceValue = ProductionConstant.ADD_LOSS_QTY_EVEN_NUMBER;
            }
        } else {
            productionModeList.forEach(single -> single.setIsProductionBySum(YesOrNoEnum.YES.getValue()));
        }
        if (differenceValue <= BigDecimal.ZERO.intValue()) {
            return;
        }
        //最小批量或是损耗量(奇数+3，偶数+2)处理:优先：有高加高上
        Comparator sort = Comparator.comparing(MonthPlanProductionRequirePlanVo::getOriginHeightProductionQty).thenComparing(MonthPlanProductionRequirePlanVo::getOriginProductionQty);
        productionModeList.sort(sort);
        MonthPlanProductionRequirePlanVo first = productionModeList.get(BigDecimal.ZERO.intValue());
        Integer initProductionQty = first.getOriginProductionQty();
        initProductionQty = initProductionQty + differenceValue;
        first.setOriginProductionQty(initProductionQty);
        first.setProductionQty(initProductionQty);
    }

    /**
     * 获取实单总量，先按Sku分组
     * 奇数+3 偶数+2
     *
     * @param productionPlanList
     * @return
     */
    private Integer getAllActualQuantity(List<MonthPlanProductionRequirePlanVo> productionPlanList) {
        if (CollectionUtils.isEmpty(productionPlanList)) {
            return BigDecimal.ZERO.intValue();
        }
        Map<String, Integer> skuActualQuantityMap = Maps.newHashMap();
        Map<String, List<MonthPlanProductionRequirePlanVo>> skuMap = productionPlanList.stream().collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc));
        skuMap.forEach((materialDesc, skuDetailList) -> {
            Integer sumActualQuantity = skuDetailList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getActualQuantity).sum();
            if (sumActualQuantity <= BigDecimal.ZERO.intValue()) {
                return;
            }
            if ((sumActualQuantity & BigDecimal.ONE.intValue()) != BigDecimal.ZERO.intValue()) {
                sumActualQuantity = sumActualQuantity + ProductionConstant.ADD_LOSS_QTY_ODD_NUMBER;
            } else {
                sumActualQuantity = sumActualQuantity + ProductionConstant.ADD_LOSS_QTY_EVEN_NUMBER;
            }
            skuActualQuantityMap.put(materialDesc, sumActualQuantity);
        });
        if (CollectionUtils.isEmpty(skuActualQuantityMap)) {
            return BigDecimal.ZERO.intValue();
        }
        return skuActualQuantityMap.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * 是否为有效分组需求计划
     * 1、计划数不可为零
     * 2、最小日硫化量不可为空或是小于零
     * 3、最小硫化机台数不可为空或是小于零
     *
     * @param groupInfo
     * @return
     */
    private boolean isEffectiveGroupRequire(ProductionPlanGroupInfo groupInfo) {
        if (null == groupInfo) {
            return false;
        }
        //计划总数
        if (null == groupInfo.getSumPlanQty() || groupInfo.getSumPlanQty() <= BigDecimal.ZERO.intValue()) {
            return false;
        }
        //最小日硫化量
        if (null == groupInfo.getMinLhDayCapacityQty() || groupInfo.getMinLhDayCapacityQty() <= BigDecimal.ZERO.intValue()) {
            return false;
        }
        //最小硫化机台数
        if (null == groupInfo.getMinLhMachineCount() || groupInfo.getMinLhMachineCount() <= BigDecimal.ZERO.intValue()) {
            return false;
        }
        return true;
    }

    /**
     * 计算结构最小日硫化量
     *
     * @param skuPlanList 分组的Sku计划信息
     * @return 结构最小日硫化量
     */
    private int calculateMinLhDayCapacityQty(List<MonthPlanProductionRequirePlanVo> skuPlanList) {
        //最小日硫化量
        return skuPlanList.stream().filter(item -> item.isEffectiveDayVulcanizationQty()).mapToInt(MonthPlanProductionRequirePlanVo::getDayVulcanizationQty).min().orElse(0);
    }

    /**
     * 将排产净需求置为零
     * 场景：周期结构排产只排产高或是实单，又没有量时，则置为零
     *
     * @param singleSkuList
     */
    private void resetProductionQtyZero(List<MonthPlanProductionRequirePlanVo> singleSkuList) {
        if (CollectionUtils.isEmpty(singleSkuList)) {
            return;
        }
        //置为零
        singleSkuList.forEach(single -> {
            single.setOriginProductionQty(BigDecimal.ZERO.intValue());
            single.setProductionQty(BigDecimal.ZERO.intValue());
        });
    }
}
