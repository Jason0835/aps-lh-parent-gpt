package com.zlt.aps.mp.engine.scheduling.matching;

import static com.zlt.aps.common.core.utils.ApsNumberUtils.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.service.ISysConfigService;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.enums.ProductTypeEnum;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.MdmSkuConstructionRefEntityMapper;
import com.zlt.aps.maindata.mapper.MdmSkuLhCapacityEntityMapper;
import com.zlt.aps.mp.api.domain.capacity.MpDailyCapacityLimitVo;
import com.zlt.aps.mp.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.mp.api.domain.entity.DpDemandPlan;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.api.domain.entity.MdmProductStock;
import com.zlt.aps.mp.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.aps.mp.api.domain.entity.MdmSkuLhCapacity;
import com.zlt.aps.mp.api.domain.entity.MdmWorkCalendar;
import com.zlt.aps.mp.api.domain.entity.MpAdjustStructureIn;
import com.zlt.aps.mp.api.domain.entity.MpAdjustStructureOut;
import com.zlt.aps.mp.api.domain.vo.DailyMouldAvailabilityResult;
import com.zlt.aps.mp.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.mp.engine.capacity.MpAdjustDailyCapacityLimit;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.domain.dto.CxContinueInfoHelper;
import com.zlt.aps.mp.engine.domain.vo.MatchingProductionAdjuestVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.enums.DayVulcanizationModeEnum;
import com.zlt.aps.mp.engine.mapper.MonthPlanRequireMapper;
import com.zlt.aps.mp.engine.scheduling.BaseDataContainer;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.engine.scheduling.cxcapacity.ProductionCapacityParamConfiguration;
import com.zlt.aps.mp.engine.service.ProductionMdmDataService;
import com.zlt.common.utils.PubUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 调整搭配处理类
 *
 * @author hak
 */
@Slf4j
@Component
public class MatchingAdjuestProductionHandler {
    @Autowired
    private ProductionMdmDataService productionSchedulingDataService;
    @Autowired
    private MonthPlanRequireMapper monthPlanRequireMapper;
    @Autowired
    private MdmSkuLhCapacityEntityMapper mdmSkuLhCapacityEntityMapper;
    @Autowired
    private MdmSkuConstructionRefEntityMapper mdmSkuConstructionRefEntityMapper;
    @Autowired
    private ISysConfigService sysConfigService;

    @Value("${debug.ignorSkip.matching:false}")
    private Boolean isIgnorSkip;


    /**
     * 初始化月计划调整的必要数据
     *
     * @param contextDTO
     */
    public void initAdjustContextDTO(MpRollAdjustContextDTO contextDTO) {
        TbrProductionContext productionContext = this.initProductionContext(contextDTO); // 初始化上下文
        this.getMdmProductStock(contextDTO, productionContext);
        this.getSkuLhCapacity(contextDTO);
        // 日硫化产能表，key:物料描述
        this.getMdmSkuConstructionRefMap(contextDTO); // 获取SKU与施工关系，key：物料号
    }

    /**
     * 周程滚动的结构内搭配算法
     *
     * @param contextDTO      周程滚动调整上下文
     * @param mpProdFinalList 月计划定稿表列表（只有当前结构的记录）
     * @param isInner         是否结构内调整
     */
    public void matchingAdjustProduction(MpRollAdjustContextDTO contextDTO,
                                         List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList, boolean isInner) {
        try {
            String config = sysConfigService.selectConfigByKey("monthAdjust.skip.matching");
            if (!isIgnorSkip && StringUtils.isNotBlank(config) && Boolean.parseBoolean(config)) {
                return; // 跳过搭配开关打开，则直接返回
            }
        } catch (Exception e) {
            log.error("获取配置失败", e);
        }

        // 1、结构排产的开始不能早于锁定日的校验
        Integer startDay = contextDTO.getStartDay();
        Integer endDay = contextDTO.getEndDay();
        Integer lockEndDay = contextDTO.getLockEndDay();
        Integer realBeginDay = Math.max(lockEndDay + 1, startDay);
        if (endDay <= lockEndDay) { // 结束日在锁定日结束前的结构不搭配
            return;
        }
        // 2、特殊材料可搭配量校验（只有结构内需要考虑）
        boolean isSpecial = isInner && intValue(contextDTO.getSpecStructureTotalQty()) > 0;
        Integer remaindSpecQty = 0;
        if (isSpecial) {
            Integer totalQty = mpProdFinalList.stream().filter(p -> p.getTotalQty() != null).mapToInt(FactoryMonthPlanFinalAdjustVo::getTotalQty).sum();
            remaindSpecQty = contextDTO.getSpecStructureTotalQty() - totalQty;
            if (remaindSpecQty <= 0) {
                return;
            }
        }
        // 3、取调整需求计划
        List<MonthPlanProductionRequirePlanVo> demandPlanList = this.loadRequirePlanList(contextDTO);
        if (CollectionUtils.isEmpty(demandPlanList)) {
            return;
        }
        log.info("周程滚动搭配算法start");
        TbrProductionContext productionContext = this.initProductionContext(contextDTO); // 初始化上下文
        List<MdmProductStock> stockList = this.getMdmProductStock(contextDTO, productionContext);
        productionContext.setOverSixMonthStockMap(this.overSixMonthStockHandler(productionContext, stockList)); // 超6个成品库存信息
        productionContext.getBaseDataContainer().setParamConfiguration(this.buildParam(contextDTO.getParamMap()));
        // 部分list转换成map，方便取数
        Map<String, Integer> stockMap = stockList.stream()
                .filter(s -> StringUtils.isNotEmpty(s.getMaterialDesc())).collect(Collectors
                        .toMap(MdmProductStock::getMaterialDesc, MdmProductStock::getStockQty, (q1, q2) -> q1 + q2)); // key：规格描述，value：库存
        Map<String, FactoryMonthPlanFinalAdjustVo> mpProdFinalMap = this.initMpProdFinalMap(contextDTO, mpProdFinalList, demandPlanList);
        mpProdFinalList.forEach(plan -> this.reCaculateInventorySalesRatio(contextDTO, plan, stockMap)); // 重算库销比
        demandPlanList.forEach(demand -> demand.setStockQty(stockMap.get(demand.getMaterialDesc()))); // 设置库存
        // 按天统计已排产量
        Map<Integer, List<MatchingProductionAdjuestVo>> dayProductionMap = this.buildDayProductionMap(mpProdFinalList,
                startDay, endDay);
        boolean hasMatching = false;
        // 4、反复循环执行搭配扫描
        do {
            // 先执行模具续作分配
            int continueAllocationQty = this.doAllocationAdjuest(contextDTO, productionContext, mpProdFinalList, mpProdFinalMap, demandPlanList, dayProductionMap,
                    realBeginDay, endDay, isSpecial, remaindSpecQty, true);
            // 再执行新增模具分配
            int totalAllocationQty = this.doAllocationAdjuest(contextDTO, productionContext, mpProdFinalList, mpProdFinalMap, demandPlanList, dayProductionMap,
                    realBeginDay, endDay, isSpecial, remaindSpecQty, false);
            if (isSpecial && totalAllocationQty > 0) {
                remaindSpecQty -= totalAllocationQty; // 特殊结构，更新剩余可分配量量
            }
            // 补量或者加模有任意一个搭配上，则标记为已搭配
            if (continueAllocationQty > 0 || totalAllocationQty > 0) {
                hasMatching = true;
            }
            if (totalAllocationQty <= 0) { // 没有增模搭配，结束
                break;
            }
        } while (true);
        log.info("周程滚动搭配算法end");
        // 5、执行搭配后，有任意一个能搭配上，再次尝试补量
        if (hasMatching) {
            this.structureAdjuestBoots(contextDTO, mpProdFinalList);
        }
    }

    /**
     * 收尾补量算法
     *
     * @param contextDTO      上下文
     * @param mpProdFinalList 定稿计划列表
     */
    public void structureAdjuestBoots(MpRollAdjustContextDTO contextDTO,
                                      List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList) {
        Integer matchingBoostDay = (Integer) contextDTO.getParamMap().get(MonthPlanEnums.MATCHING_BOOST_DAY.getCode());
        String boostProductionTypeValue = (String) contextDTO.getParamMap()
                .get(MonthPlanEnums.BOOST_PRODUCTION_TYPE_VALUE.getCode());
        Set<String> boostProductionTypeSet;
        if (StringUtils.isBlank(boostProductionTypeValue)) {
            boostProductionTypeSet = Collections.emptySet();
        } else {
            boostProductionTypeSet = Stream.of(boostProductionTypeValue.split(StringConstant.COMMA))
                    .collect(Collectors.toSet());
        }
        if (matchingBoostDay <= 0 || CollectionUtils.isEmpty(boostProductionTypeSet)) {
            return;
        }
        // 特殊材料可补量计算
        Integer specStructureTotalQty = intValue(contextDTO.getSpecStructureTotalQty());
        boolean isSpecial = specStructureTotalQty > 0;
        Integer unAllocatSpecStructureQty = this.getUnAllocatSpecStructureQty(specStructureTotalQty, mpProdFinalList);
        if (isSpecial && unAllocatSpecStructureQty <= 0) {
            return;
        }
        // 加载上下文中的各项必要数据
        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitMap = contextDTO.getDailyCapacityLimitVoMap(); // 每日产能统计
        Integer startDay = contextDTO.getStartDay();
        Integer lockEndDay = contextDTO.getLockEndDay();
        Integer endDay = contextDTO.getEndDay();
        Integer realStartDay = Math.max(startDay, lockEndDay);
        // 实际开始日期要看补量天数的设置，哪个晚用哪个
        Integer boostStartDay = endDay - (matchingBoostDay - 1); // 开始补量日
        Integer lastBoostStartDay = this.getLastDay(contextDTO, boostStartDay, startDay); // 补量开始日的前一天，用于判断增减模
        Integer checkStartDay = lastBoostStartDay > 0 ? lastBoostStartDay : boostStartDay; // 开始检查日，用于获取包括补量期间以及补量前一天的排产信息
        Integer bootsQty = 0; // 总补量
        if (realStartDay > endDay) {
            return;
        }

        out:
        for (FactoryMonthPlanFinalAdjustVo plan : mpProdFinalList) {
            Integer actualAdjustQty = intValue(plan.getActualAdjustQty()); // 实际调整量
            if (!boostProductionTypeSet.contains(plan.getProductionType())) { // 非主销规格不补量
                continue;
            }
            if (actualAdjustQty <= 0) { // 减量或者不调整的SKU不补量
                continue;
            }
            // 统计补量日期各天排产量
            Map<Integer, Integer> dayProductionQtyMap = this.getDayProductionQtyMap(contextDTO, plan, checkStartDay, endDay);
            // 补量期间任意一天有量，都需要补量
            if (dayProductionQtyMap.values().stream().noneMatch(qty -> qty > 0)) {
                continue;
            }
            // 统计在产硫化机数
            TreeMap<Integer, Integer> bootsDayLhMachineMap = this.getBootsDayLhMachineMap(contextDTO, plan, checkStartDay,
                    endDay, dailyCapacityLimitMap); // 每日已使用硫化机数
            // 统计最大补量机台数，以包括补量天以及前一天的最大硫化机为准
            Integer maxLhMachineCount = bootsDayLhMachineMap.values().stream().max(Integer::compareTo).orElse(0);
            if (maxLhMachineCount <= 0) {
                continue;
            }

            // 遍历补量开始日到收尾日之间的生产量，并尝试开始补量
            for (Integer day: bootsDayLhMachineMap.keySet()) {// 根据日产比例限制产能
                // 如果当天有排产，在不加模的前提下检查是否已经占满
                MpDailyCapacityLimitVo dailyCapacityLimit = dailyCapacityLimitMap.get(day);
                if (dailyCapacityLimit == null) {
                    continue;
                }
                if (day < boostStartDay) { // 补量开始日前的日期跳过不需要补
                    continue;
                }
                // 获取前一天、当天的硫化机数
                Integer lastDay = intValue(bootsDayLhMachineMap.lowerKey(day));
                Integer useMachineCount = bootsDayLhMachineMap.get(day);
                Integer lastUseMachineCount = bootsDayLhMachineMap.getOrDefault(lastDay, 0);
                Integer capacity = plan.getDayVulcanizationQty() * ProductionConstant.DOUBLE_MOULD_PRODUCTION; // 单机产能
                // 检查上一天排产是否符合
                if (lastDay > 0) {
                    if (useMachineCount > lastUseMachineCount) { // 加模，不能补量看下一天
                        continue;
                    }
                    Integer lastProductionQty = dayProductionQtyMap.getOrDefault(lastDay, 0); // 上一天的排产量
                    if (useMachineCount < lastUseMachineCount && lastProductionQty % capacity != 0) { // 减模，且上一天不满排，是有拼模的情况，不补
                        continue;
                    }
                }
                Integer productionQty = dayProductionQtyMap.getOrDefault(day, 0); // 当天已排产量
                Integer allocationQty = productionQty % capacity; // 只补余量，不加模具
                if (allocationQty != 0) { // 当天不满排，需要验证是否有拼机台的情况
                    // 尝试加量后计算机台变化
                    Integer oldLhMachineCount = dailyCapacityLimit.getUsedLhMachines();
                    String dayFieldName = FactoryConstant.DAY_FIELD + day;
                    plan.setFieldValueByFieldName(dayFieldName, productionQty + allocationQty);
                    this.reCalcAdjustDailyCapacityLimit(contextDTO, contextDTO.getFactoryMonthPlanProdFinalList(), plan, day); // 重算量加上后的产能占用
                    boolean isOverLimit = dailyCapacityLimit.getUsedLhMachines() != oldLhMachineCount;
                    // 还原数据
                    plan.setFieldValueByFieldName(dayFieldName, productionQty);
                    this.reCalcAdjustDailyCapacityLimit(contextDTO, contextDTO.getFactoryMonthPlanProdFinalList(), plan, day); // 还原产能占用
                    if (isOverLimit) { // 会导致机台增加，不补
                        continue;
                    }
                }
                
                Integer realAllocationQty = isSpecial ? Math.min(unAllocatSpecStructureQty, allocationQty) : allocationQty; // 如果是特殊材料需要控制不能超过总量
                if (realAllocationQty <= 0) {
                    continue;
                }
                Integer realProductionQty = productionQty + realAllocationQty;
                plan.setFieldValueByFieldName(FactoryConstant.DAY_FIELD + day, realProductionQty);
                plan.setTotalQty(plan.getTotalQty() + realAllocationQty);
                plan.setEndDay(plan.getEndDay() < day ? day : plan.getEndDay());
                // 更新各项统计数据
                bootsQty += realAllocationQty;
                unAllocatSpecStructureQty -= realAllocationQty; // 更新待分配特殊材料总数
                dayProductionQtyMap.put(day, realProductionQty); // 更新当天排产量统计
                contextDTO.getLogDetail().append(String.format("结构:%s,【收尾补量】物料编码:%s,排产日:%s,补量:%s", contextDTO.getStructureName(), plan.getMaterialCode(), day, realAllocationQty)).append(ApsConstant.DIVISION); // 记录日志
                // 如果是特殊结构，且特殊结构已分配完，则结束
                if (isSpecial && unAllocatSpecStructureQty <= 0) {
                    break out;
                }
                reCalcAdjustDailyCapacityLimit(contextDTO, mpProdFinalList, plan, day);
            }
        }
        if (isSpecial) {
            contextDTO.setSpecStructureTotalQty(specStructureTotalQty - bootsQty);
        }
    }
    
    /**
     * 初始化调整计划Map，包含需求计划有但是月计划没有的SKU
     *
     * @param contextDTO
     * @param mpProdFinalList
     * @param demandPlanList
     * @return
     */
    private Map<String, FactoryMonthPlanFinalAdjustVo> initMpProdFinalMap(MpRollAdjustContextDTO contextDTO,
                                                                          List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList,
                                                                          List<MonthPlanProductionRequirePlanVo> demandPlanList) {
        Map<String, MdmSkuLhCapacity> mdmSkuLhCapacityMap = this.getSkuLhCapacity(contextDTO); // 日硫化产能表，key:物料描述
        Map<String, MdmSkuConstructionRef> mdmSkuConstructionRefMap = this.getMdmSkuConstructionRefMap(contextDTO); // 获取SKU与施工关系，key：物料号
        // 构建Map
        Map<String, FactoryMonthPlanFinalAdjustVo> mpProdFinalMap = mpProdFinalList.stream()
                .filter(p -> StringUtils.isNotEmpty(p.getMaterialDesc())).collect(Collectors
                        .toMap(FactoryMonthPlanFinalAdjustVo::getMaterialDesc, Function.identity(), (p1, p2) -> p1)); // key：规格描述

        for (MonthPlanProductionRequirePlanVo demandPlan : demandPlanList) {
            FactoryMonthPlanFinalAdjustVo plan = mpProdFinalMap.get(demandPlan.getMaterialDesc()); // 获取排产结果
            if (plan != null) {
                continue;
            }
            // 如果没有，说明是新增规格，需要新增记录
            FactoryMonthPlanFinalAdjustVo firstPlan = CollectionUtils.firstElement(mpProdFinalList);
            plan = new FactoryMonthPlanFinalAdjustVo();
            if (firstPlan != null) {
                BeanUtils.copyProperties(firstPlan, plan);
            }
            plan.setId(null);
            plan.setMaterialCode(demandPlan.getMaterialCode());
            plan.setMaterialDesc(demandPlan.getMaterialDesc());
            plan.setMainPattern(demandPlan.getMainPattern());
            MdmSkuConstructionRef skuConstructionRef = mdmSkuConstructionRefMap.get(demandPlan.getMaterialCode());
            if (skuConstructionRef != null) {
                plan.setMainMaterialDesc(skuConstructionRef.getMainMaterialDesc());
            }
            String materialCode = demandPlan.getMaterialCode(); // 物料号
            Integer capacity = this.getMdmSkuLhCapacity(contextDTO, materialCode, mdmSkuLhCapacityMap); // 产能
            plan.setDayVulcanizationQty(capacity / ProductionConstant.DOUBLE_MOULD_PRODUCTION); // 单模产能
            plan.setMesMaterialCode(demandPlan.getMesMaterialCode());
            plan.setProductionType(demandPlan.getProductionType());
            plan.setAverageSaleQty(demandPlan.getAverageSaleQty());
            plan.setHeightProductionQty(0);
            plan.setMidProductionQty(0);
            plan.setCycleProductionQty(0);
            plan.setConventionProductionQty(0);
            plan.setPostponeProductionQty(0);
            plan.setDifferenceQty(0);
            plan.setTotalQty(0);
            plan.setBeginDay(null);
            plan.setEndDay(null);
            plan.setAdjustDetailId(null);
            plan.setActualAdjustQty(0);
            this.reCaculateInventorySalesRatio(contextDTO, plan,
                    Collections.singletonMap(demandPlan.getMaterialCode(), demandPlan.getStockQty())); // 计算库销比
            for (int day = FactoryConstant.MONTH_START_DAY; day <= FactoryConstant.MONTH_MAX_DAY; day++) {
                plan.setFieldValueByFieldName(FactoryConstant.DAY_FIELD + day, null); // 清空每天排产量
            }
            mpProdFinalMap.put(demandPlan.getMaterialDesc(), plan);
        }
        return mpProdFinalMap;
    }

    /**
     * 执行调整搭配量分配
     *
     * @param contextDTO        调整上下文
     * @param productionContext 月计划上下文
     * @param mpProdFinalList   排产计划列表
     * @param mpProdFinalMap    排产计划Map
     * @param demandPlanList    需求计划列表，已经按sku合并好
     * @param dayProductionMap  日生产量统计列表
     * @param beginDay          结构调整开始日期
     * @param endDay            结构调整结束日期
     * @param isSpecial         是否特殊结构
     * @param remaindSpecQty    特殊结构剩余量
     * @param isCheckContinue   是否处理续作，每个SKU都是从续作开始检查
     * @return
     */
    private int doAllocationAdjuest(MpRollAdjustContextDTO contextDTO, TbrProductionContext productionContext,
                                    List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList,
                                    Map<String, FactoryMonthPlanFinalAdjustVo> mpProdFinalMap, List<MonthPlanProductionRequirePlanVo> demandPlanList,
                                    Map<Integer, List<MatchingProductionAdjuestVo>> dayProductionMap, Integer beginDay,
                                    Integer endDay, boolean isSpecial, int remaindSpecQty,
                                    boolean isCheckContinue) {
        int totalAllocationQty = 0; // 本次遍历总搭配量
        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitMap = contextDTO.getDailyCapacityLimitVoMap(); // 每日产能统计
        Set<String> scheduleMaterialDesc = new HashSet<>(); // 记录已排规格，防止重复执行死循环
        // 1、最外层循环，每轮处理当前优先级最高的SKU搭配算法
        do {
            // 2、获取最高优先级的可搭配调整规格
            MonthPlanProductionRequirePlanVo needProductPlan = this.getHeightPriorityAdjuestMaterial(demandPlanList, mpProdFinalMap,
                    productionContext, scheduleMaterialDesc);
            if (needProductPlan == null) {
                break;
            }
            String materialDesc = needProductPlan.getMaterialDesc();
            scheduleMaterialDesc.add(materialDesc); // 选中的规格加入已排产列表（无论是否能排上，下次轮询均不再处理该规格）
            FactoryMonthPlanFinalAdjustVo plan = mpProdFinalMap.get(materialDesc); // 获取定稿计划
            if (intValue(plan.getActualAdjustQty()) < 0) { // 减量的SKU不搭配
                continue;
            }
            int capacity = plan.getDayVulcanizationQty() * ProductionConstant.DOUBLE_MOULD_PRODUCTION; // 产能都从final表获取
            boolean isNewPlan = plan.getBeginDay() == null;
            
            // 3、计算可搭配量
            Integer unAllocationQty = needProductPlan.getProductionQty(); // 未搭配量 = 储备池的量
            unAllocationQty = isSpecial? Math.min(unAllocationQty, remaindSpecQty): unAllocationQty; // 如果包含特殊材料，不能超过特殊材料的总数量
            Integer producedQty = 0; // 本次生产量
            // 4、如果是增规格，把规格放到临时列表中，如果后续逻辑能搭配上才加入正式列表，否则舍弃
            List<FactoryMonthPlanFinalAdjustVo> safeList = new ArrayList<>();
            safeList.addAll(mpProdFinalList);
            if (isNewPlan) {
                safeList.add(plan);
            }

            // 5、SKU外层循环，反复扫描该SKU搭配期间的每一天，只要一次扫描能搭配上任意一天，则再重新尝试扫描一次，知道无法搭配上后则结束外层循环
            out: do {
                int startUnAllocationQty = unAllocationQty; // 记录开始扫描前的待搭配量，用于本轮扫描结束后比对是否有
                // 5.1、确认搭配期间，初始限定在结构调整开始时间 至 SKU收尾的后一天
                Integer realBeginDay = plan.getMatchBeginDay() != null? plan.getMatchBeginDay(): beginDay; // 本次循环的开始日期
                Integer realEndDay = this.getRealEndDay(contextDTO, plan, realBeginDay, endDay); // 本次循环的结束日期
                // 5.2、开始循环检查搭配期间的每一天，符合搭配条件的日期则执行搭配
                for (int day = realBeginDay; day <= realEndDay; day++) { // 遍历结构排产日，如果锁定日超过开始i日期，从锁定日下一天开始
                    if (unAllocationQty <= 0) {
                        break out;
                    }
                    // 5.2.1、检查生产日历，停产日不处理
                    if (!this.checkDayCanProduct(contextDTO, day)) {
                        continue;
                    }
                    // 5.2.2、排产参数校验
                    if (!this.checkFactoryParams(contextDTO, plan, day, safeList)) {
                        continue;
                    }
                    // 5.2.3、产能校验
                    MpDailyCapacityLimitVo dailyCapacityLimitVo = dailyCapacityLimitMap.get(day);
                    Integer mouldRemaindCapacity = this.getMouldRemaindCapacity(contextDTO, plan, capacity, day, beginDay, endDay, dayProductionMap, dailyCapacityLimitMap); // 获取模具剩余产能
                    // 5.2.3.1、如果模具产能已满，且当天硫化机已经满足条件，则直接跳过
                    if (mouldRemaindCapacity <= 0
                            && dailyCapacityLimitVo.getMaxLhMachines() <= dailyCapacityLimitVo.getUsedLhMachines()) {
                        continue;
                    }
                    // 5.2.3.2、检查补量是否会超过硫化机数量超限制
                    if (!this.checkOverMachineCountLimit(contextDTO, plan, mouldRemaindCapacity, day, safeList)) {
                        continue;
                    }
                    
                    // 5.2.4、为当天分配搭配量
                    int allocationQty = this.allcatAdjustProductQty(contextDTO, day, beginDay, endDay, plan, safeList,
                            dayProductionMap, unAllocationQty, capacity, dailyCapacityLimitMap, mouldRemaindCapacity, isCheckContinue);
                    if (allocationQty > 0) { // 有分配量，说明成功搭配排产，需要更新相关数据
                        producedQty += allocationQty;
                        unAllocationQty -= allocationQty;
                        if (!isCheckContinue) { // 如果是新增逻辑，则直接结束，走续作逻辑
                            break out;
                        } else if (plan.getMatchEndDay() == realEndDay) { // 如果区间最后一天有排产，且往后结构还没有结束，则继续尝试往后延一天
                            Integer nextEndDay = this.getNextDay(contextDTO, realEndDay, endDay);
                            if (nextEndDay > 0 && nextEndDay <= endDay) {
                                realEndDay = nextEndDay;
                            }
                        }
                    }
                }
                // 5.3、如果本轮循环没有搭配量，则说明SKU已经无法继续搭配，结束SKU外层循环
                if (startUnAllocationQty == unAllocationQty) {
                    break out; // 结束本规格的搭配
                }
            } while (true);
            
            // 6、更新SKU的生产量
            needProductPlan.setProductionQty(needProductPlan.getProductionQty() - producedQty); // 待搭配量
            needProductPlan.setProducedQty(needProductPlan.getProducedQty() + producedQty); // 已搭配量
            totalAllocationQty += producedQty;
            if (isNewPlan && plan.getBeginDay() != null) { // 排上的规格添加导列表中
                mpProdFinalList.add(plan);
            }
            if (!isCheckContinue) { // 如果是新增逻辑，则直接结束，走续作逻辑
                break;
            }
        } while (true);
        return totalAllocationQty;
    }

    /**
     * 排产参数校验
     * 
     * @param contextDTO 上下文
     * @param plan       调整计划
     * @param day        调整日
     * @param safeList   调整计划列表
     * @return
     */
    private boolean checkFactoryParams(MpRollAdjustContextDTO contextDTO, FactoryMonthPlanFinalAdjustVo plan, int day,
                                       List<FactoryMonthPlanFinalAdjustVo> safeList) {
        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitMap = contextDTO.getDailyCapacityLimitVoMap(); // 每日产能统计
        this.reCalcAdjustDailyCapacityLimit(contextDTO, safeList, plan, day); // 先重算产能占用
        MpDailyCapacityLimitVo dailyCapacityLimitVo = dailyCapacityLimitMap.get(day);
        // 1、检查胎胚数是否满足条件
        if (dailyCapacityLimitVo.getMaxEmbryoTypes() <= dailyCapacityLimitVo.getUsedEmbryoTypes()) { // 胎胚数已达上限，则不能继续添加新胎胚
            Set<String> embryoCodes = dailyCapacityLimitVo.getEmbryoCodes();
            if (!embryoCodes.contains(plan.getMainMaterialDesc())) {
                return false;
            }
        }
        // 2、检查当天搭配是否会导致二次上机
        if (!this.checkSecOnlineAdjuest(contextDTO, plan, day)) {
            return false;
        }
        // 3、外销贴牌总量限制
        if (contextDTO.getOemBrandConfigSet().contains(plan.getBrand())
                && intValue(dailyCapacityLimitVo.getRemainOemQty()) < 0) {
            return false;
        }
        return true;
    }

    /**
     * 检查是否超过机台产能
     * 
     * @param contextDTO 上下文
     * @param plan       待调整计划
     * @param addQty     增加量
     * @param day        调整日期
     * @param planList   调整计划列表
     * @return
     */
    private boolean checkOverMachineCountLimit(MpRollAdjustContextDTO contextDTO, FactoryMonthPlanFinalAdjustVo plan,
                                               Integer addQty, int day, List<FactoryMonthPlanFinalAdjustVo> planList) {
        if (addQty <= 0) {
            return true;
        }

        MpDailyCapacityLimitVo dailyCapacityLimitVo = contextDTO.getDailyCapacityLimitVoMap().get(day);
        Integer oldUsedLhMachines = dailyCapacityLimitVo.getUsedLhMachines(); // 保存加量前的硫化机数量
        // 1、尝试加上量后计算产能占用情况
        String dayFieldName = FactoryConstant.DAY_FIELD + day;
        Integer oldValue = intValue(plan.getFieldValueByFieldName(dayFieldName));
        plan.setFieldValueByFieldName(dayFieldName, oldValue + addQty); // 先把量加上
        this.reCalcAdjustDailyCapacityLimit(contextDTO, planList, plan, day); // 尝试计算
        // 2、取出计算后的已使用机台数
        Integer usedLhMachines = dailyCapacityLimitVo.getUsedLhMachines();
        Integer maxLhMachines = dailyCapacityLimitVo.getMaxLhMachines();
        // 3、还原数据
        plan.setFieldValueByFieldName(dayFieldName, oldValue);
        this.reCalcAdjustDailyCapacityLimit(contextDTO, planList, plan, day); // 先把量加上尝试计算
        // 4、校验增加机台数会导致超过限制的，不能搭配
        if (maxLhMachines < usedLhMachines) {
            return false;
        }
        // 5、校验增加的机台数少于剩余硫化机数，不能搭配
        if (usedLhMachines - oldUsedLhMachines > dailyCapacityLimitVo.getRemainLhMachines()) {
            return false; 
        }
        return true;
    }

    /**
     * 本次SKU搭配结束日期<br/>
     * 如果是新增SKU，则为结构调整结束日<br/>
     * 如果是原有SKU，且还没有开始搭配，则为SKU收尾日的后一天<br/>
     * 如果是原有SKU，如果已经开始搭配，则为SKU搭配结束日的后一天<br/>
     * 
     * @param plan           需求计划
     * @param beginDay       搭配开始日
     * @param endDay         结构调整结束日
     * @param lastProductDay 收尾日期
     * @return
     */
    private Integer getRealEndDay(MpRollAdjustContextDTO contextDTO, FactoryMonthPlanFinalAdjustVo plan, Integer beginDay, Integer endDay) {
        if (plan.getEndDay() == null) { // 新增规格，直接返回结构结束日期
            return endDay;
        }
        // 1、如果需求计划的搭配结束日期已经在之前的循环中结算出来则以此为准，否则以SKU的收尾日为准
        Integer realEndDay = plan.getMatchEndDay() != null? plan.getMatchEndDay(): plan.getEndDay();
        // 2、不能早于搭配开始日期
        if (realEndDay < beginDay) {
            realEndDay = beginDay;
        }
        // 3、如果搭配结束日期还早于结构结束日期，则需要看到下一天
        if (realEndDay < endDay) {
            Integer nextDay = this.getNextDay(contextDTO, realEndDay, endDay);
            if (nextDay > 0) {
                realEndDay = nextDay;
            }
        }
        return realEndDay;
    }

    /**
     * 检查调整搭配是否满足二次上机限制限制
     *
     * @param contextDTO 上下文
     * @param plan       排程计划
     * @param checkDay   检查日
     * @return
     */
    private boolean checkSecOnlineAdjuest(MpRollAdjustContextDTO contextDTO, FactoryMonthPlanFinalAdjustVo plan,
                                          int checkDay) {
        boolean isSecOnLine = true;
        Integer skuBeginDay = plan.getBeginDay();
        Integer skuEndDay = plan.getEndDay();
        if (skuBeginDay == null) {
            return true;
        }
        // 1、检查当天排产情况
        Integer dayProductQty = intValue(plan.getFieldValueByFieldName(FactoryConstant.DAY_FIELD + checkDay));
        if (dayProductQty > 0) { // 如果当天没有排产才需要继续检查是否引起二次上机
            return isSecOnLine;
        }
        Integer skuSecondProduction = (Integer) contextDTO.getParamMap().get(MonthPlanEnums.SKU_SECOND_PRODUCTION.getCode());
        Integer dayCount = 0;
        // 2、向前看是否有超出二次上机限制
        for (Integer i = checkDay - 1; i >= skuBeginDay; i--) {
            if (i == 0) {
                break;
            }
            if (!this.checkDayCanProduct(contextDTO, i)) {
                continue;
            }
            dayCount ++;
            Integer checkDayProductQty = intValue(plan.getFieldValueByFieldName(FactoryConstant.DAY_FIELD + i));
            if (checkDayProductQty > 0) { // 如果有排产，则检查是否超过限制
                isSecOnLine = dayCount == 1 || dayCount >= skuSecondProduction || i == skuBeginDay;
                break;
            }
        }
        if (!isSecOnLine) { // 校验不通过，则直接结束
            return isSecOnLine;
        }

        // 3、先向后看是否有超出二次上机限制
        dayCount = 0;
        for (Integer i = checkDay + 1; i <= skuEndDay; i++) {
            if (!this.checkDayCanProduct(contextDTO, i)) {
                continue;
            }
            dayCount ++;
            Integer checkDayProductQty = intValue(plan.getFieldValueByFieldName(FactoryConstant.DAY_FIELD + i));
            if (checkDayProductQty > 0) { // 如果有排产，则检查是否超过限制
                isSecOnLine = dayCount == 1 || dayCount >= skuSecondProduction || i == skuEndDay;
                break;
            }
        }
        return isSecOnLine;
    }

    /**
     * 构建调整计划Map，包括重算各SKU的库销比
     *
     * @param contextDTO      上下文
     * @param mpProdFinalList 调整计划列表
     * @param stockMap        库存
     * @return
     */
    private void reCaculateInventorySalesRatio(MpRollAdjustContextDTO contextDTO, FactoryMonthPlanFinalAdjustVo plan,
                                               Map<String, Integer> stockMap) {
        Integer lockEndDay = contextDTO.getLockEndDay();
        BigDecimal inventorySalesRatio = BigDecimalUtils.valueOf(plan.getInventorySalesRatio());
        int averageSaleQty = intValue(plan.getAverageSaleQty());
        int stock = stockMap.getOrDefault(plan.getMaterialDesc(), 0);
        int planQty = this.getSumPlanQtyLockEndDay(plan, lockEndDay);
        if (averageSaleQty != 0) {
            inventorySalesRatio = BigDecimalUtils.div(stock + planQty, averageSaleQty);
        }
        plan.setInventorySalesRatio(inventorySalesRatio);
    }

    /**
     * 获取特殊结构待分配量
     *
     * @param specStructureTotalQty
     * @param mpProdFinalList
     * @return
     */
    private Integer getUnAllocatSpecStructureQty(Integer specStructureTotalQty,
                                                 List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList) {
        boolean isSpecial = specStructureTotalQty > 0;
        Integer unAllocatSpecStructureTotalQty = 0; // 特殊材料可分配量
        if (isSpecial) {
            Integer totalQty = mpProdFinalList.stream().filter(p -> p.getTotalQty() != null).mapToInt(FactoryMonthPlanFinalAdjustVo::getTotalQty).sum();
            unAllocatSpecStructureTotalQty = specStructureTotalQty - totalQty;

        }
        return unAllocatSpecStructureTotalQty;
    }

    /**
     * 统计补量日期区间的在产硫化机数
     *
     * @param contextDTO            上下文
     * @param plan                  排产记录
     * @param startDay              补量区间开始日
     * @param endDay                补量区间结束日
     * @param dailyCapacityLimitMap 每日产能限制
     * @return
     */
    private TreeMap<Integer, Integer> getBootsDayLhMachineMap(MpRollAdjustContextDTO contextDTO, FactoryMonthPlanFinalAdjustVo plan,
                                                          Integer startDay, Integer endDay,
                                                          Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitMap) {
        TreeMap<Integer, Integer> bootsDayLhMachineMap = new TreeMap<>();
        for (int day = startDay; day <= endDay; day++) {
            bootsDayLhMachineMap.put(day, this.getDayUsedLhMachines(contextDTO, plan, day, dailyCapacityLimitMap));
        }
        return bootsDayLhMachineMap;
    }

    /**
     * 统计各天排产量
     * 
     * @param contextDTO    上下文
     * @param plan          调整计划
     * @param boostStartDay 补量开始日
     * @param endDay        结构结束日
     * @return
     */
    private Map<Integer, Integer> getDayProductionQtyMap(MpRollAdjustContextDTO contextDTO,
                                                         FactoryMonthPlanFinalAdjustVo plan, Integer boostStartDay,
                                                         Integer endDay) {
        Map<Integer, Integer> dayProductionQtyMap = new HashMap<>();
        for (int day = boostStartDay; day <= endDay; day++) {
            Integer dayQty = intValue(plan.getFieldValueByFieldName(FactoryConstant.DAY_FIELD + day));
            dayProductionQtyMap.put(day, dayQty);
        }
        return dayProductionQtyMap;
    }

    /**
     * 获取新的型腔数量
     *
     * @param contextDTO 周程滚动上下文
     * @param mpFinalVo  定稿Vo
     * @param iDay       当前天
     * @return 型腔数量
     */
    private int getNewCavityQty(MpRollAdjustContextDTO contextDTO, FactoryMonthPlanFinalAdjustVo mpFinalVo, int iDay) {
        DailyMouldAvailabilityResult cavity2BlockVo = contextDTO.getCavity2BlockMap().get(iDay);
        if (cavity2BlockVo != null && cavity2BlockVo.getCavityResults() != null) {
            Integer cavityQty = cavity2BlockVo.getCavityResults().get(mpFinalVo.getStructureName() + mpFinalVo.getMainPattern());
            return cavityQty != null ? cavityQty : mpFinalVo.getMouldCavityQty();
        }
        return mpFinalVo.getMouldCavityQty();
    }

    /**
     * 检查模具满足情况
     *
     * @param dailyCapacityLimitVo 产能限制Vo
     * @param cavityQty            型腔数
     * @return true-满足，false-不满足
     */
    private boolean checkMouldSatisfy(MpDailyCapacityLimitVo dailyCapacityLimitVo, int cavityQty) {
        //型腔台数
        int patternCount = cavityQty / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        //主花纹向下所有SKU的模具数量 < 主花纹.型腔数量
        return dailyCapacityLimitVo.getPatternUsedLhMachines() < patternCount;
    }

    /**
     * 重算指定天的日产能限制，包括硫化机台数、胎胚种类数
     *
     * @param contextDTO      周程滚动上下文
     * @param mpProdFinalList 定稿记录列表
     * @param mpProdFinal     定稿记录
     * @param day             排产日
     */
    private void reCalcAdjustDailyCapacityLimit(MpRollAdjustContextDTO contextDTO,
                                                List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList,
                                                FactoryMonthPlanFinalAdjustVo mpProdFinal, int day) {
        MpAdjustDailyCapacityLimit adjustDailyCapacityLimitObj = new MpAdjustDailyCapacityLimit();
        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap = contextDTO.getDailyCapacityLimitVoMap();
        MpDailyCapacityLimitVo daylyCapacityLimit = dailyCapacityLimitVoMap.get(day);
        if (daylyCapacityLimit == null) {
            return;
        }
        adjustDailyCapacityLimitObj.calcLhMachinesWithEmbryoTypes(mpProdFinalList, day, daylyCapacityLimit,
                contextDTO.getParamMap(), mpProdFinal.getMainPattern());
    }

    /**
     * 获取SKU与施工关系
     *
     * @param contextDTO
     * @return
     */
    private Map<String, MdmSkuConstructionRef> getMdmSkuConstructionRefMap(MpRollAdjustContextDTO contextDTO) {
        Map<String, MdmSkuConstructionRef> mdmSkuConstructionRefMap = contextDTO.getMdmSkuConstructionRefMap();
        if (mdmSkuConstructionRefMap == null) {
            LambdaQueryWrapper<MdmSkuConstructionRef> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(MdmSkuConstructionRef::getFactoryCode, contextDTO.getFactoryCode());
            queryWrapper.eq(MdmSkuConstructionRef::getIsDelete, YesOrNoEnum.NO.getValue());
            List<MdmSkuConstructionRef> skuConstructionRefList = mdmSkuConstructionRefEntityMapper
                    .selectList(queryWrapper);
            mdmSkuConstructionRefMap = skuConstructionRefList.stream()
                    .filter(construction -> StringUtils.isNotEmpty(construction.getMaterialCode()))
                    .collect(Collectors.toMap(MdmSkuConstructionRef::getMaterialCode, construction -> construction,
                            (existingVal, newVal) -> newVal));
        }
        return mdmSkuConstructionRefMap;
    }

    /**
     * 获取成品库存
     *
     * @param contextDTO
     * @param productionContext
     * @return
     */
    private List<MdmProductStock> getMdmProductStock(MpRollAdjustContextDTO contextDTO,
                                                     TbrProductionContext productionContext) {
        List<MdmProductStock> stockList = contextDTO.getMdmProductStockList(); // 库存
        if (CollectionUtils.isEmpty(stockList)) {
            stockList = productionSchedulingDataService.getMdmProductStock(productionContext); // 如果没有需要加载库存
            contextDTO.setMdmProductStockList(stockList);
        }
        return stockList;
    }

    /**
     * 获取模具剩余产能
     *
     * @param contextDTO            上下文
     * @param plan                  待搭配计划
     * @param capacity              原产能
     * @param day                   排产日
     * @param beginDay              搭配开始日
     * @param dayProductionMap      每日已排产量列表
     * @param dailyCapacityLimitMap 日产能限制列表
     * @return
     */
    private Integer getMouldRemaindCapacity(MpRollAdjustContextDTO contextDTO, FactoryMonthPlanFinalAdjustVo plan,
                                            int capacity, int day, int beginDay, int endDay,
                                            Map<Integer, List<MatchingProductionAdjuestVo>> dayProductionMap,
                                            Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitMap) {
        String materialDesc = plan.getMaterialDesc();
        // 1、检查当天排产情况
        MatchingProductionAdjuestVo dayProduction = this.getMatchingProductionAdjuest(materialDesc, day,
                dayProductionMap);
        if (dayProduction == null) {
            return 0;
        }
        Integer changeMouldFirstQty = (Integer) contextDTO.getParamMap()
                .get(MonthPlanEnums.CHANGE_MOULD_FIRST_QTY.getCode()); // 换模首日可排产量
        int realCapacity = dayProduction.getIsFirstDay() ? changeMouldFirstQty : capacity;
        if (realCapacity == 0) {
            return 0;
        }
        Integer useCapacity = BigDecimalUtils.valueOf(dayProduction.getProductionQty())
                .remainder(BigDecimalUtils.valueOf(realCapacity)).intValue(); // 计算余数
        if (useCapacity == 1) { // 余数是1只会是因为奇数转偶数引起的，可以忽略
            return 0;
        } else if (useCapacity > 0) { // 余数大于0，说明有硫化机没有排满，优先补满剩余的量
            // 2、先判断后续天数是否满产能排产
            Integer nextDay = this.getNextDay(contextDTO, day, endDay);
            if (nextDay > 0) { // 非收尾日，需要判断下一天产能是否占满
                MpDailyCapacityLimitVo dailyCapacityLimitVo = dailyCapacityLimitMap.get(nextDay);
                if (dailyCapacityLimitVo != null
                        && dailyCapacityLimitVo.getMaxLhMachines() == dailyCapacityLimitVo.getUsedLhMachines()) { // 下一天产能占满，则当天不需要搭配补量
                    return 0;
                }
            }
            // 3、判断如果今天的硫化机数比昨天多，说明有新增模具，不能补量
            Integer lastDay = this.getLastDay(contextDTO, day, beginDay);
            if (lastDay > 0) {
                Integer lastDayUsedLhMachines = this.getDayUsedLhMachines(contextDTO, plan, lastDay,
                        dailyCapacityLimitMap); // 上一天已使用的硫化机数量
                Integer todayDayUsedLhMachines = this.getDayUsedLhMachines(contextDTO, plan, day,
                        dailyCapacityLimitMap); // 当天已使用的硫化机数量
                if (todayDayUsedLhMachines > lastDayUsedLhMachines) { // 今天的硫化机数比昨天多，不补
                    return 0;
                }
            }
            return realCapacity - useCapacity;
        }
        return 0;
    }

    /**
     * 获取指定天指定SKU的调整排产记录
     *
     * @param materialDesc     规格描述
     * @param day              排产日
     * @param dayProductionMap 调整排产记录列表
     * @return
     */
    private MatchingProductionAdjuestVo getMatchingProductionAdjuest(String materialDesc, int day,
                                                                     Map<Integer, List<MatchingProductionAdjuestVo>> dayProductionMap) {
        List<MatchingProductionAdjuestVo> dayProductionList = dayProductionMap.get(day);
        if (CollectionUtils.isEmpty(dayProductionList)) {
            return null;
        }
        return dayProductionList.stream().filter(p -> materialDesc.equals(p.getMaterialDesc())).findFirst()
                .orElse(null);
    }

    /**
     * 获取下一个排产日
     *
     * @param contextDTO
     * @param day
     * @param endDay
     * @return
     */
    private Integer getNextDay(MpRollAdjustContextDTO contextDTO, int day, int endDay) {
        Integer nextDay = 0;
        for (int i = day + 1; i <= endDay; i++) {
            if (this.checkDayCanProduct(contextDTO, i)) { // 下一天是排产日返回，否则跳过看下一天
                nextDay = i;
                break;
            }
        }
        return nextDay;
    }

    /**
     * 获取上一个排产日
     *
     * @param contextDTO
     * @param day
     * @param beginDay
     * @return
     */
    private Integer getLastDay(MpRollAdjustContextDTO contextDTO, int day, int beginDay) {
        Integer lastDay = 0;
        for (int i = day - 1; i >= beginDay; i--) {
            if (this.checkDayCanProduct(contextDTO, i)) { // 下一天是排产日返回，否则跳过看下一天
                lastDay = i;
                break;
            }
        }
        return lastDay;
    }

    /**
     * 根据产能模具获取指定SKU的硫化产能
     *
     * @param contextDTO          上下文
     * @param materialDesc        规格描述
     * @param mdmSkuLhCapacityMap 硫化产能列表
     * @return
     */
    private int getMdmSkuLhCapacity(MpRollAdjustContextDTO contextDTO, String materialDesc,
                                    Map<String, MdmSkuLhCapacity> mdmSkuLhCapacityMap) {
        Integer capacity = 0;
        MdmSkuLhCapacity mdmSkuLhCapacity = mdmSkuLhCapacityMap.get(materialDesc);
        if (mdmSkuLhCapacity == null) {
            throw new BusinessException(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.monthPlanFinalRecord.notDayLhQty"),
                    materialDesc));
        }
        DayVulcanizationModeEnum dayVulcanizationMode = DayVulcanizationModeEnum
                .getInstance((String) contextDTO.getParamMap().get(MonthPlanEnums.DAY_VULCANIZATION_MODE.getCode()));
        if (dayVulcanizationMode == DayVulcanizationModeEnum.MES_CAPACITY) {
            capacity = mdmSkuLhCapacity.getMesCapacity();
        } else if (dayVulcanizationMode == DayVulcanizationModeEnum.STANDARD_CAPACITY) {
            capacity = mdmSkuLhCapacity.getStandardCapacity();
        } else if (dayVulcanizationMode == DayVulcanizationModeEnum.APS_CAPACITY) {
            capacity = mdmSkuLhCapacity.getApsCapacity();
        }
        if (capacity == null) {
            throw new BusinessException(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.monthPlanFinalRecord.notDayLhQty"),
                    materialDesc));
        }
        return capacity;
    }

    /**
     * 加载需求计划
     *
     * @param contextDTO
     * @return
     */
    private List<MonthPlanProductionRequirePlanVo> loadRequirePlanList(MpRollAdjustContextDTO contextDTO) {
        List<DpDemandPlan> demandPlanList = contextDTO.getDpDemandPlanList();
        if (CollectionUtils.isEmpty(demandPlanList)) {
            String monthPlanVersion = null;
            MpAdjustStructureIn mpAdjustStructureIn = CollectionUtils.firstElement(contextDTO.getMpAdjustStructureInList());
            MpAdjustStructureOut mpAdjustStructureOut = CollectionUtils.firstElement(contextDTO.getMpAdjustStructureOutList());
            if (mpAdjustStructureIn != null) {
                monthPlanVersion = mpAdjustStructureIn.getLastMonthPlanVersion();
            } else if (mpAdjustStructureOut != null) {
                monthPlanVersion = mpAdjustStructureOut.getLastMonthPlanVersion();
            }
            if (StringUtils.isEmpty(monthPlanVersion)) {
                return new ArrayList<>(0);
            }
            // 加载需求计划
            LambdaQueryWrapper<DpDemandPlan> demandQueryWrapper = new LambdaQueryWrapper<DpDemandPlan>();
            demandQueryWrapper.eq(DpDemandPlan::getMonthPlanVersion, monthPlanVersion);
            demandQueryWrapper.gt(DpDemandPlan::getConventionReserveQty, 0);
            demandQueryWrapper.isNotNull(DpDemandPlan::getStructureName); // 过滤空结构的数据
            demandPlanList = monthPlanRequireMapper.selectList(demandQueryWrapper);
            contextDTO.setDpDemandPlanList(demandPlanList);
        }
        // 类型转换为月计划需求计划
        List<MonthPlanProductionRequirePlanVo> requirePlanList = demandPlanList.stream()
                .filter(dp -> Objects.equals(contextDTO.getStructureName(), dp.getStructureName())).map(dpPlan -> {
                    MonthPlanProductionRequirePlanVo requirePlan = new MonthPlanProductionRequirePlanVo();
                    BeanUtils.copyProperties(dpPlan, requirePlan);
                    requirePlan.setProductionQty(intValue(requirePlan.getConventionReserveQty()));
                    requirePlan.setProducedQty(0);
                    return requirePlan;
                }).collect(Collectors.toList());

        // 按SKU合并数据
        Map<String, MonthPlanProductionRequirePlanVo> requirePlanMap = requirePlanList.stream().collect(
                Collectors.toMap(MonthPlanProductionRequirePlanVo::getMaterialDesc, Function.identity(), (p1, p2) -> {
                    Integer conventionreserveqty = safeAdd(p1.getConventionReserveQty(), p2.getConventionReserveQty());
                    p1.setConventionReserveQty(conventionreserveqty);
                    p1.setProductionQty(conventionreserveqty);
                    return p1;
                }));
        return new ArrayList<>(requirePlanMap.values());
    }
    
    /**
     * 日硫化产能表
     *
     * @param contextDTO
     * @return
     */
    private Map<String, MdmSkuLhCapacity> getSkuLhCapacity(MpRollAdjustContextDTO contextDTO) {
        Map<String, MdmSkuLhCapacity> mdmSkuLhCapacityMap = contextDTO.getMdmSkuLhCapacityMap(); // 日硫化产能表，key:物料描述
        if (mdmSkuLhCapacityMap == null) {

            LambdaQueryWrapper<MdmSkuLhCapacity> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(MdmSkuLhCapacity::getFactoryCode, contextDTO.getFactoryCode());
            queryWrapper.eq(MdmSkuLhCapacity::getIsDelete, YesOrNoEnum.NO.getValue());
            List<MdmSkuLhCapacity> skuLhCapacityList = mdmSkuLhCapacityEntityMapper.selectList(queryWrapper);

            mdmSkuLhCapacityMap = skuLhCapacityList.stream()
                    .filter(skuLhCapacity -> StringUtils.isNotEmpty(skuLhCapacity.getMaterialCode()))
                    .collect(Collectors.toMap(
                            MdmSkuLhCapacity::getMaterialCode,
                            skuLhCapacity -> skuLhCapacity,
                            (existingVal, newVal) -> newVal
                    ));
            contextDTO.setMdmSkuLhCapacityMap(mdmSkuLhCapacityMap);
        }
        return mdmSkuLhCapacityMap;
    }

    /**
     * 获取最高优先级的可搭配调整规格
     *
     * @param demandPlanList       需求计划列表
     * @param mpProdFinalMap       定稿列表，key：规格描述
     * @param productionContext    上下文
     * @param scheduleMaterialDesc 已排产物料描述
     * @return
     */
    private MonthPlanProductionRequirePlanVo getHeightPriorityAdjuestMaterial(List<MonthPlanProductionRequirePlanVo> demandPlanList,
                                                    Map<String, FactoryMonthPlanFinalAdjustVo> mpProdFinalMap,
                                                    TbrProductionContext productionContext,
                                                    Set<String> scheduleMaterialDesc) {
        return demandPlanList.stream().filter(p -> p.getProductionQty() > 0)
                .filter(p -> !scheduleMaterialDesc.contains(p.getMaterialDesc()))
                .min((p1, p2) -> {
                    // 排序1、优先库销比低的
                    FactoryMonthPlanFinalAdjustVo finalPlan1 = mpProdFinalMap.get(p1.getMaterialDesc());
                    FactoryMonthPlanFinalAdjustVo finalPlan2 = mpProdFinalMap.get(p2.getMaterialDesc());
                    BigDecimal inventorySalesRatio1 = BigDecimal.ZERO;
                    BigDecimal inventorySalesRatio2 = BigDecimal.ZERO;
                    if (finalPlan1 != null) {
                        inventorySalesRatio1 = finalPlan1.getInventorySalesRatio();
                    }
                    if (finalPlan2 != null) {
                        inventorySalesRatio2 = finalPlan2.getInventorySalesRatio();
                    }
                    int result = inventorySalesRatio1.compareTo(inventorySalesRatio2);
                    if (result != 0) {
                        return result;
                    }
                    // 排序2、优先超6个月库存少的
                    Integer sixStock1 = productionContext.getOverSixMonthStockMap().getOrDefault(p1.getMaterialCode(), 0);
                    Integer sixStock2 = productionContext.getOverSixMonthStockMap().getOrDefault(p2.getMaterialCode(), 0);
                    return sixStock1.compareTo(sixStock2);
                }).orElse(null);
    }

    /**
     * 取截至锁定日前的排产量汇总值
     *
     * @param plan
     * @param lockEndDay
     * @return
     */
    private Integer getSumPlanQtyLockEndDay(FactoryMonthPlanFinalAdjustVo plan, Integer lockEndDay) {
        if (plan == null) {
            return 0;
        }
        Integer sumPlanQty = plan.getSumPlanQtyBeforeLockDay();
        if (sumPlanQty == null) {
            sumPlanQty = 0;
            for (int day = 1; day <= lockEndDay; day++) {
                sumPlanQty += intValue(plan.getFieldValueByFieldName(FactoryConstant.DAY_FIELD + day));
            }
            plan.setSumPlanQtyBeforeLockDay(sumPlanQty);
        }
        return sumPlanQty;
    }

    /**
     * 分配搭配生产量
     *
     * @param contextDTO            上下文
     * @param scheduleDay           排产日期
     * @param beginDay              搭配开始日
     * @param beginDay              搭配结束日
     * @param plan                  排产记录
     * @param mpProdFinalList       排产记录列表
     * @param dayProductionMap      日排产信息统计表
     * @param unAllocationQty       未分配搭配量
     * @param capacity              单机台产能
     * @param dailyCapacityLimitMap 产能限制列表
     * @param mouldRemaindCapacity  模具剩余产能
     * @param isCheckContinue       是否仅检查续作
     * @return
     */
    private Integer allcatAdjustProductQty(MpRollAdjustContextDTO contextDTO, Integer scheduleDay, Integer beginDay,
                                           Integer endDay, FactoryMonthPlanFinalAdjustVo plan,
                                           List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList,
                                           Map<Integer, List<MatchingProductionAdjuestVo>> dayProductionMap,
                                           Integer unAllocationQty, Integer capacity,
                                           Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitMap,
                                           Integer mouldRemaindCapacity, boolean isCheckContinue) {
        MpDailyCapacityLimitVo dailyCapacityLimitVo = dailyCapacityLimitMap.get(scheduleDay);
        String materialDesc = plan.getMaterialDesc();
        boolean isRemaindCapacity = mouldRemaindCapacity > 0; // 是否是补模具产能
        int mouldCavityQty = this.getNewCavityQty(contextDTO, plan, scheduleDay); // 总型腔数量
//        int typeBlockQty = cavity2Block.getInsertResults().getOrDefault(mouldKey, 0); // 总活块数量
        
        // 判断当天成型硫化比是否已经满足条件
        List<MatchingProductionAdjuestVo> dayProductionList = dayProductionMap.get(scheduleDay);
        // 统计当天的已排量，判断不能超过最大排产量限制
        if (!this.checkRemainDayTotalCapacity(contextDTO, dailyCapacityLimitVo, scheduleDay)) {
            return 0;
        }
        Integer lastDay = this.getLastDay(contextDTO, scheduleDay, beginDay);
        Integer lastDayUsedLhMachines = this.getDayUsedLhMachines(contextDTO, plan, lastDay, dailyCapacityLimitMap); // 上一天已使用的硫化机数量
        Integer todayDayUsedLhMachines = this.getDayUsedLhMachines(contextDTO, plan, scheduleDay, dailyCapacityLimitMap); // 当天已使用的硫化机数量
        // 如果只检查续作，模具不需要补量，且今天的机台比不比昨天的少，则跳过今天
        if (isCheckContinue && !isRemaindCapacity && lastDayUsedLhMachines <= todayDayUsedLhMachines) {
            return 0;
        } 
        // 根据上一天的排产情况判断是否需要换模
        boolean isChangeMould = false;
        List<MatchingProductionAdjuestVo> lastDayProductionList = dayProductionMap.get(lastDay);
        if (lastDayProductionList == null) {
            isChangeMould = true;
        } else if (isRemaindCapacity) {
            isChangeMould = false; // 补模具产能不需要加模
        } else if (lastDay > 0) {
            if (lastDayProductionList.stream().noneMatch(p -> materialDesc.equals(p.getMaterialDesc()))) {
                isChangeMould = true; //  昨天没有排产，则需要换模具
            } else {
                // 如果有排产，根据排产量计算硫化机数量
                isChangeMould = lastDayUsedLhMachines <= todayDayUsedLhMachines; // 昨天的硫化机数量不超过今天的，需要加模具
            }
        }

        // 如果需要换模，则还需要满足如下条件才允许上机
        if (isChangeMould) {
            // 1、剩余型腔数不足最低排产模具数
            if (!this.checkMouldSatisfy(dailyCapacityLimitVo, mouldCavityQty)) {
                return 0;
            }
            // 2、如果需要换模具，不能超过换模次数限制
            if (dailyCapacityLimitVo.getRemainChangeMould() <= dailyCapacityLimitVo.getUsedChangeMould()) {
                return 0;
            }
            // 3、当天满足上机条件按，但是下一天不满足上机条件的，也不允许上机
            boolean isOk = this.checkNextDayCanContinueProduct(contextDTO, plan, scheduleDay, endDay, mpProdFinalList,
                    unAllocationQty, dailyCapacityLimitMap);

            if (!isOk) {
                return 0;
            }
        }
        // 取出已有的排产计划
        MatchingProductionAdjuestVo dayProduct = dayProductionList.stream()
                .filter(p -> materialDesc.equals(p.getMaterialDesc())).findAny().orElse(null);
        if (dayProduct == null) {
            dayProduct = new MatchingProductionAdjuestVo();
            dayProduct.setMaterialCode(plan.getMaterialCode());
            dayProduct.setMaterialDesc(materialDesc);
            dayProduct.setProductionDate(scheduleDay);
            dayProduct.setProductionQty(0);
            dayProduct.setDayVulcanizationQty(capacity);
            dayProduct.setIsFirstDay(isChangeMould);
            dayProductionList.add(dayProduct);
        }
        
        // 计算排产量
        int allocationQty = capacity; // 本次排产量，默认是双模*模具产能
        if (isChangeMould) { // 如果是换模具，则只能增加首日排产量
            Integer changeMouldFirstQty = new MpAdjustDailyCapacityLimit().getFirstDayQty(
                    contextDTO.getFactoryMonthPlanProdFinalList(), scheduleDay, dailyCapacityLimitVo,
                    contextDTO.getParamMap(), plan.getMainPattern());
            allocationQty = intValue(changeMouldFirstQty); // 每次仅新增一台硫化机
        }
        if (allocationQty <= 0) {
            return 0;
        }
        if (isRemaindCapacity) {
            allocationQty = Math.min(allocationQty, mouldRemaindCapacity); // 如果当天模具有剩余产能的，优先补满
        }
        allocationQty = Math.min(allocationQty, unAllocationQty); // 分配量不能超过未分配量以及剩余产能
        if (((dayProduct.getProductionQty() + allocationQty)& 1) != 0) { // 如果原排产量 + 新排产量为奇数，则排产量需要 + 1
            allocationQty ++;
        }
        Integer oldProductionQty = intValue(plan.getFieldValueByFieldName(FactoryConstant.DAY_FIELD + scheduleDay));
        plan.setFieldValueByFieldName(FactoryConstant.DAY_FIELD + scheduleDay, allocationQty + oldProductionQty);
        plan.setTotalQty(intValue(plan.getTotalQty()) + allocationQty);
        plan.setConventionProductionQty(intValue(plan.getConventionProductionQty()) + allocationQty);
        plan.setActualAdjustQty(intValue(plan.getActualAdjustQty()) + allocationQty);
        if (plan.getBeginDay() == null) {
            plan.setBeginDay(scheduleDay);
        }
        if (plan.getEndDay() == null || plan.getEndDay() < scheduleDay) {
            plan.setEndDay(scheduleDay);
        }
        if (plan.getMatchBeginDay() == null) {
            plan.setMatchBeginDay(scheduleDay); // 设置搭配开始日期
        }
        if (plan.getMatchEndDay() == null || plan.getMatchEndDay() < scheduleDay) {
            plan.setMatchEndDay(scheduleDay); // 设置搭配结束日期
        }
        dayProduct.setProductionQty(dayProduct.getProductionQty() + allocationQty);
        if (!isRemaindCapacity) { // 除了补模具产能以外的场景，需要更新日产能限制
            dailyCapacityLimitVo.setUsedLhMachines(dailyCapacityLimitVo.getUsedLhMachines() + 1); // 更新硫化机使用情况
            dailyCapacityLimitVo.setPatternUsedLhMachines(dailyCapacityLimitVo.getPatternUsedLhMachines() + 1);
            Set<String> embryoCodes = dailyCapacityLimitVo.getEmbryoCodes();
            if (!embryoCodes.contains(plan.getMainMaterialDesc())) {
                embryoCodes.add(plan.getMainMaterialDesc());
                dailyCapacityLimitVo.setUsedEmbryoTypes(embryoCodes.size());
            }
        }
        
        // 更新剩余硫化机台数
        Integer oldLhMachines = dailyCapacityLimitVo.getUsedLhMachines();
        this.reCalcAdjustDailyCapacityLimit(contextDTO, mpProdFinalList, plan, scheduleDay); // 有搭配，则再次重算产能占用
        Integer newLhMachines = dailyCapacityLimitVo.getUsedLhMachines();
        dailyCapacityLimitVo.setRemainLhMachines(safeAdd(dailyCapacityLimitVo.getRemainChangeMould(), oldLhMachines - newLhMachines)); // 使用机台如果有增加则剩余机台数会减少
        
        String scheduleName = isCheckContinue? "补量": "增模";
        String logDetail = String.format("结构:%s,【搭配排产】物料编码:%s,排产日:%s,%s,搭配排产量:%s",contextDTO.getStructureName(),plan.getMaterialCode(),scheduleDay,scheduleName,allocationQty);
        log.debug(logDetail);
        contextDTO.getLogDetail().append(logDetail).append(ApsConstant.DIVISION); // 记录日志
        return allocationQty;
    }

    /**
     * 检查隔天是否可以继续生产
     *
     * @param contextDTO            上下文
     * @param plan                  排产记录
     * @param scheduleDay           排产日
     * @param endDay                搭配结束日
     * @param mpProdFinalList       排产列表
     * @param unAllocationQty       未分配搭配量
     * @param dailyCapacityLimitMap 产能限制列表
     * @return
     */
    private boolean checkNextDayCanContinueProduct(MpRollAdjustContextDTO contextDTO,
                                                   FactoryMonthPlanFinalAdjustVo plan, Integer scheduleDay,
                                                   Integer endDay, List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList,
                                                   Integer unAllocationQty,
                                                   Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitMap) {
        // 取出下一个排产日
        Integer nextDay = this.getNextDay(contextDTO, scheduleDay, endDay);
        // 1、已经是结构收尾日，不搭配
        if (nextDay <= 0) {
            return false;
        }
        this.reCalcAdjustDailyCapacityLimit(contextDTO, mpProdFinalList, plan, nextDay); // 先重算下一个排产日的产能限制
        // 2、校验机台限制
        MpDailyCapacityLimitVo nextDailyCapacityLimitVo = dailyCapacityLimitMap.get(nextDay);
        if (nextDailyCapacityLimitVo.getMaxLhMachines() <= nextDailyCapacityLimitVo.getUsedLhMachines()) { // 如果模具产能已满，且当天硫化机已经满足条件，则直接跳过
            return false;
        }
        // 3、检查胎胚数限制
        if (nextDailyCapacityLimitVo.getMaxEmbryoTypes() <= nextDailyCapacityLimitVo.getUsedEmbryoTypes()) { // 胎胚数已达上限，则不能继续添加新胎胚
            Set<String> embryoCodes = nextDailyCapacityLimitVo.getEmbryoCodes();
            if (!embryoCodes.contains(plan.getEmbryoCode())) {
                return false;
            }
        }
        // 4、校验分配量限制，如果待分配量不足首日的量，不搭配
        Integer firstQty = (Integer) contextDTO.getParamMap().get(MonthPlanEnums.CHANGE_MOULD_FIRST_QTY.getCode());
        if (unAllocationQty <= firstQty) {
            return false;
        }
        // 5、下一天的总剩余产能不足时，不搭配
        if (!this.checkRemainDayTotalCapacity(contextDTO, nextDailyCapacityLimitVo, nextDay)) {
            return false;
        }
        return true;
    }

    /**
     * 获取每日最大产能扣除已排产量后的剩余产能
     *
     * @param contextDTO           上下文
     * @param dailyCapacityLimitVo 产能限制对象
     * @param day                  排产日
     * @return
     */
    private boolean checkRemainDayTotalCapacity(MpRollAdjustContextDTO contextDTO,
                                          MpDailyCapacityLimitVo dailyCapacityLimitVo, Integer day) {
        int dayTotalCapacityLimit = intValue(dailyCapacityLimitVo.getMaxDayProductionQty());
        List<FactoryMonthPlanFinalAdjustVo> mpPlanFinalAdjustList = contextDTO.getFactoryMonthPlanProdFinalList();
        if (CollectionUtils.isEmpty(mpPlanFinalAdjustList)) {
            return true;
        }
        // 1.计算检查日的汇总值
        String dayField = FactoryConstant.DAY_FIELD + day;
        int totalPlanQty = mpPlanFinalAdjustList.stream().filter(Objects::nonNull).mapToInt(x -> {
            Object val = x.getFieldValueByFieldName(dayField);
            return val instanceof Number ? ((Number) val).intValue() : 0;
        }).sum();

        // 2.检查日的汇总值 小于等于 日总产能限制
        return totalPlanQty < dayTotalCapacityLimit;
    }

    /**
     * 获取sku一天的已使用硫化机
     *
     * @param contextDTO            上下文
     * @param plan                  SKU已排产计划
     * @param day                   排产日
     * @param dailyCapacityLimitMap 日产能限制
     * @return
     */
    private Integer getDayUsedLhMachines(MpRollAdjustContextDTO contextDTO, FactoryMonthPlanFinalAdjustVo plan,
                                         Integer day, Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitMap) {
        MpDailyCapacityLimitVo oldDailyCapacityLimitVo = dailyCapacityLimitMap.get(day);
        if (oldDailyCapacityLimitVo == null) {
            return 0;
        }
        MpDailyCapacityLimitVo newDailyCapacityLimitVo = new MpDailyCapacityLimitVo();
        newDailyCapacityLimitVo.setDayOpenCloseFlag(oldDailyCapacityLimitVo.getDayOpenCloseFlag());
        newDailyCapacityLimitVo.setDayProductionRate(oldDailyCapacityLimitVo.getDayProductionRate());
        new MpAdjustDailyCapacityLimit().calcLhMachinesWithEmbryoTypes(Collections.singletonList(plan), day,
                newDailyCapacityLimitVo, contextDTO.getParamMap(), null);
        return newDailyCapacityLimitVo.getUsedLhMachines();
    }

    /**
     * 构建日排产列表，用于搭配排产过程中的拍段逻辑
     *
     * @param mpProdFinalList
     * @param startDay
     * @param endDay
     * @return
     */
    private Map<Integer, List<MatchingProductionAdjuestVo>> buildDayProductionMap(List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList,
                                                                                  Integer startDay, Integer endDay) {
        Map<Integer, List<MatchingProductionAdjuestVo>> dayProductionMap = new HashMap<>();
//        Map<Integer, Integer> dayProductionQtyMap = new HashMap<>();
        for (FactoryMonthPlanFinalAdjustVo plan : mpProdFinalList) {
            String materialDesc = plan.getMaterialDesc();
            MatchingProductionAdjuestVo lastDayProduct = null; // 昨日排产
            for (int day = startDay; day <= endDay; day++) {
                Integer productionQty = intValue(plan.getFieldValueByFieldName(FactoryConstant.DAY_FIELD + day));
                List<MatchingProductionAdjuestVo> dayProductionList = dayProductionMap.get(day);
                if (dayProductionList == null) {
                    dayProductionList = new ArrayList<>();
                    dayProductionMap.put(day, dayProductionList);
                }
                MatchingProductionAdjuestVo dayProduct = dayProductionList.stream()
                        .filter(d -> Objects.equals(d.getMaterialDesc(), materialDesc)).findAny().orElse(null);
                if (dayProduct == null) {
                    dayProduct = new MatchingProductionAdjuestVo();
                    dayProduct.setMaterialCode(plan.getMaterialCode());
                    dayProduct.setMaterialDesc(materialDesc);
                    dayProduct.setProductionDate(day);
                    dayProduct.setProductionQty(0);
                    dayProduct.setDayVulcanizationQty(
                            plan.getDayVulcanizationQty() * ProductionConstant.DOUBLE_MOULD_PRODUCTION);
                    dayProductionList.add(dayProduct);
                }
                int sumProductQty = dayProduct.getProductionQty() + productionQty;
                dayProduct.setProductionQty(sumProductQty);
                dayProduct.setIsFirstDay(lastDayProduct == null || (lastDayProduct.getProductionQty() == 0 && sumProductQty > 0)); // 上一天没有排产，且今天有排产，说明是首日

                lastDayProduct = dayProduct;
            }
        }
        return dayProductionMap;
    }

    /**
     * 检查日历是否可生产
     *
     * @param contextDTO
     * @param day
     * @return
     */
    private boolean checkDayCanProduct(MpRollAdjustContextDTO contextDTO, int day) {
        boolean isProduct = true;
        Map<Integer, MdmWorkCalendar> workCalendarMap = contextDTO.getWorkCalendarMap(); // 工作日历
        MdmWorkCalendar calendar = workCalendarMap.get(day);
        if (calendar != null && YesOrNoEnum.NO.getCode().equals(calendar.getDayFlag())) {
            isProduct = false;
        }
        return isProduct;
    }

    /**
     * 将定稿计划构建成算法要求的上下文结构
     *
     * @param result 定稿计划
     * @return
     */
    private TbrProductionContext initProductionContext(FactoryMonthPlanMouldDayResult result) {
        TbrProductionContext productionContext = new TbrProductionContext();
        productionContext.setProductionVersion(result.getProductionVersion()); // 生产版本号
        productionContext.setMonthPlanVersion(result.getMonthPlanVersion()); // 月需求计划版本
        productionContext.setYear(result.getYear());
        productionContext.setMonth(result.getMonth());
        productionContext.setFactoryCode(result.getFactoryCode());
        productionContext.setProductType(ProductTypeEnum.getEnumByValue(result.getProductTypeCode()));
        productionContext.setLogBuilder(new StringBuilder());
        productionContext.setBaseDataContainer(new BaseDataContainer());
        productionContext.setNoProductionRecordMap(new HashMap<>());
        return productionContext;
    }

    /**
     * 将周程滚动上下文构建成算法要求的上下文结构
     *
     * @param contextDTO 定稿计划
     * @return
     */
    private TbrProductionContext initProductionContext(MpRollAdjustContextDTO contextDTO) {
        FactoryMonthPlanMouldDayResult result = new FactoryMonthPlanMouldDayResult();
        result.setProductionVersion(contextDTO.getProductionVersion());
        result.setMonthPlanVersion(contextDTO.getMonthPlanVersion());
        result.setYear(contextDTO.getMpYear());
        result.setMonth(contextDTO.getMpMonth());
        result.setFactoryCode(contextDTO.getFactoryCode());
        FactoryMonthPlanFinalAdjustVo plan = CollectionUtils.firstElement(contextDTO.getFactoryMonthPlanProdFinalList());
        if (plan != null) {
            result.setProductTypeCode(plan.getProductTypeCode());
        }
        return initProductionContext(result);
    }







    /**
     * 业务的参数封装未配置对象
     *
     * @param paramConfigurationMap
     * @return
     */
    private ProductionCapacityParamConfiguration buildParam(Map<String, Object> paramConfigurationMap) {
        if (CollectionUtils.isEmpty(paramConfigurationMap)) {
            return null;
        }
        ProductionCapacityParamConfiguration configuration = new ProductionCapacityParamConfiguration();
        //排产控制相关
        Object minProductionDaysValue = paramConfigurationMap.get(MonthPlanEnums.MIN_PRODUCTION_DAYS.getCode());
        if (null == minProductionDaysValue) {
            configuration.setMinProductionDays(BigDecimal.ZERO.intValue());
        } else {
            configuration.setMinProductionDays((Integer) minProductionDaysValue);
        }
        configuration.setMinAllocationDays((Integer) paramConfigurationMap.get(MonthPlanEnums.MIN_ALLOCATION_DAYS.getCode()));
        configuration.setNoCycleProductionMinLhMachineNumber((Integer) paramConfigurationMap.get(MonthPlanEnums.NO_CYCLE_PRODUCTION_MIN_LH_MACHINE_NUMBER.getCode()));
        String boostProductionTypeValue = (String) paramConfigurationMap.get(MonthPlanEnums.BOOST_PRODUCTION_TYPE_VALUE.getCode());
        if (StringUtils.isBlank(boostProductionTypeValue)) {
            configuration.setBoostProductionType(Collections.emptySet());
        } else {
            configuration.setBoostProductionType(Stream.of(boostProductionTypeValue.split(StringConstant.COMMA)).collect(Collectors.toSet()));
        }
        configuration.setMaxBoostDay((Integer) paramConfigurationMap.get(MonthPlanEnums.MAX_BOOST_DAY.getCode()));
        configuration.setMatchingBoostDay((Integer) paramConfigurationMap.get(MonthPlanEnums.MATCHING_BOOST_DAY.getCode()));
        configuration.setSkuSecondProduction((Integer) paramConfigurationMap.get(MonthPlanEnums.SKU_SECOND_PRODUCTION.getCode()));
        configuration.setHeightDiffQty((Integer) paramConfigurationMap.get(MonthPlanEnums.HEIGHT_DIFF_QTY.getCode()));
        configuration.setSumProductionQty((Integer) paramConfigurationMap.get(MonthPlanEnums.SUM_PRODUCTION_QTY.getCode()));
        //日排产相关
        configuration.setDayChangeGroupCount((Integer) paramConfigurationMap.get(MonthPlanEnums.DAY_CHANGE_GROUP_COUNT.getCode()));
        configuration.setChangeMouldLhMachineNumber((Integer) paramConfigurationMap.get(MonthPlanEnums.CHANGE_MOULD_LH_MACHINE_NUMBER.getCode()));
        configuration.setChangeMouldFirstQty((Integer) paramConfigurationMap.get(MonthPlanEnums.CHANGE_MOULD_FIRST_QTY.getCode()));
        configuration.setChangeTypeBlockQtyDiff((Integer) paramConfigurationMap.get(MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY_DIFF.getCode()));
        configuration.setChangeTypeBlockQty((Integer) paramConfigurationMap.get(MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY.getCode()));
        configuration.setChangeTypeBlockMaxQty((Integer) paramConfigurationMap.get(MonthPlanEnums.CHANGE_TYPE_BLOCK_MAX_QTY.getCode()));
        configuration.setSingleCxEmbryoCodeCount((Integer) paramConfigurationMap.get(MonthPlanEnums.SINGLE_CX_EMBRYO_CODE_COUNT.getCode()));
        configuration.setDayMaxCapacity((Integer) paramConfigurationMap.get(MonthPlanEnums.DAY_MAX_CAPACITY.getCode()));
        configuration.setDayMinCapacity((Integer) paramConfigurationMap.get(MonthPlanEnums.DAY_MIN_CAPACITY.getCode()));
        configuration.setDeductionLhMachineCount((Integer) paramConfigurationMap.get(MonthPlanEnums.CHANGE_STRUCT_DEC_LH_MACHINES.getCode()));
        //降膜排产相关
        configuration.setDeductMouldMinLhMachineCount((Integer) paramConfigurationMap.get(MonthPlanEnums.DEDUCT_MOULD_MIN_LH_MACHINE_COUNT.getCode()));
        configuration.setFirstNearDeadLineMaxLhMachineCount((Integer) paramConfigurationMap.get(MonthPlanEnums.FIRST_NEAR_DEAD_LINE_MAX_LH_MACHINE_COUNT.getCode()));
        configuration.setFirstNearDeadLineDay((Integer) paramConfigurationMap.get(MonthPlanEnums.FIRST_NEAR_DEAD_LINE_DAY.getCode()));
        configuration.setSecondNearDeadLineMaxLhMachineCount((Integer) paramConfigurationMap.get(MonthPlanEnums.SECOND_NEAR_DEAD_LINE_MAX_LH_MACHINE_COUNT.getCode()));
        configuration.setSecondNearDeadLineDay((Integer) paramConfigurationMap.get(MonthPlanEnums.SECOND_NEAR_DEAD_LINE_DAY.getCode()));
        configuration.setLastNearDeadLineMaxLhMachineCount((Integer) paramConfigurationMap.get(MonthPlanEnums.LAST_NEAR_DEAD_LINE_MAX_LH_MACHINE_COUNT.getCode()));
        configuration.setLastNearDeadLineDay((Integer) paramConfigurationMap.get(MonthPlanEnums.LAST_NEAR_DEAD_LINE_DAY.getCode()));
        //其它
        configuration.setSectionWidthDiffValue((Integer) paramConfigurationMap.get(MonthPlanEnums.SECTION_WIDTH_DIFF_VALUE.getCode()));
        // 周程滚动相关
        configuration.setSingleCxMachineLockDay((Integer) paramConfigurationMap.get(MonthPlanEnums.SINGLE_CX_MACHINE_LOCK_DAYS.getCode()));
        configuration.setMultiCxMachineLockDays((Integer) paramConfigurationMap.get(MonthPlanEnums.MULTI_CX_MACHINE_LOCK_DAYS.getCode()));
        configuration.setWeekRollAdjustDate((String) paramConfigurationMap.get(MonthPlanEnums.WEEK_ROLL_ADJUST_DATE.getCode()));

        return configuration;
    }

    /**
     * 加载超6个月的库存信息
     *
     * @param productionContext
     */
    private Map<String, Integer> overSixMonthStockHandler(TbrProductionContext productionContext, List<MdmProductStock> stockList) {
        // 过滤库存为空的值
        return stockList.stream()
                .filter(s -> StringUtils.isNotEmpty(s.getMaterialDesc()) && null != s.getStockQty())
                .collect(Collectors.groupingBy(MdmProductStock::getMaterialDesc,
                        Collectors.collectingAndThen(Collectors.toList(),
                                list -> list.stream().filter(s -> ApsConstant.TRUE.equals(s.getIsExceedSixMonth()))
                                        .collect(Collectors.summingInt(MdmProductStock::getStockQty)))));
    }




    /**
     * 获取续作机台的结构信息
     * @param cxContinueInfoMap
     * @return Map<成型机台，续作结构>
     */
    protected Map<String,String> getContinueStructureMap(Map<String, CxContinueInfoHelper> cxContinueInfoMap){
        Map<String,String> machineStructureMap = new HashMap<>();
        if (PubUtil.isEmpty(cxContinueInfoMap)){
            return machineStructureMap;
        }
        // 从续作信息中解析出成型机台对应的续作结构
        CxContinueInfoHelper cxContinueInfoHelper;
        for (Map.Entry<String, CxContinueInfoHelper> entry : cxContinueInfoMap.entrySet()) {
            cxContinueInfoHelper = entry.getValue();
            Set<String> cxMachineCodeSet = cxContinueInfoHelper.getCxMachineCodeSet();
            for (String machineCode:cxMachineCodeSet){
                machineStructureMap.put(machineCode,entry.getKey());
            }
        }
        return machineStructureMap;
    }
}
