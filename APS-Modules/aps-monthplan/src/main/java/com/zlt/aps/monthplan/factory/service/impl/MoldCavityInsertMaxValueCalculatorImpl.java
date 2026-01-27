package com.zlt.aps.monthplan.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tlt.aps.enums.ProductionProcessesTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.BeanCopyUtils;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.ProductionDayInfoVo;
import com.zlt.aps.factory.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.factory.enums.MouldRelationTypeEnum;
import com.zlt.aps.factory.utils.DateUtils;
import com.tlt.aps.enums.ProductTypeEnum;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.mapper.FactoryMonthPlanProductMouldMapper;
import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import com.zlt.aps.maindata.mapper.MdmWorkCalendarEntityMapper;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.MdmWorkCalendar;
import com.zlt.aps.monthplan.api.domain.vo.MoldCavityInsertMaxValueCalculatorVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模具型腔活块可用量最大值计算器 - Service Impl
 * @author 16799 Nick
 */
@Service
@Slf4j
public class MoldCavityInsertMaxValueCalculatorImpl {

    @Resource
    private FactoryMonthPlanProductMouldMapper factoryMonthPlanProductMouldMapper;

    @Resource
    private IFactoryParamService factoryParamService;

    @Resource
    private MdmWorkCalendarEntityMapper mdmWorkCalendarEntityMapper;

    /**
     * 数据提供接口
     */
    @Resource
    private ProductionSchedulingDataService dataService;

    /**
     * 按照传入年月工厂的月度需求计划, 计算型腔活块可用量最大值
     *
     * @param year              年份    :  必须传入  -  抛出异常
     * @param month             月份    :  必须传入  -  抛出异常
     * @param factoryCode       工厂代码:  必须传入  -  抛出异常
     * @param targetDate        指定日期:  可以不传 -- 注意：不传计算当月所有日期
     * @param monthPlanVersion  净需求计划版本号: 可以不传 -- 注意：不传不考虑净需求计划直接取所有
     * @return 计算结果：Map<String, Object> 包含cavityResults和insertResults
     * @throws Exception 抛出异常各自处理
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> moldCavityInsertMaxValueCalculator(Integer year, Integer month, String factoryCode,
                                                                  Date targetDate, String monthPlanVersion) throws Exception {
        // 1. 获取SKU模具配置信息（含新模具）
        Map<String, List<MoldCavityInsertMaxValueCalculatorVo>> mouldRelationMap =
                getProductionMouldInfo(year, month, factoryCode, targetDate, monthPlanVersion);

        if (CollectionUtils.isEmpty(mouldRelationMap)) {
            log.warn("未找到任何模具配置信息，工厂：{}，年月：{}-{}", factoryCode, year, month);
            return Collections.emptyMap();
        }

        // 2. 每个模具计算结合工作日历计算可用日期
        Map<String, ProductionMouldInfoVo> mouldInfoMap =
                createProductionMouldInfo(year, month, factoryCode, mouldRelationMap);

        // 3. 获取净需求计划，补充SKU模具关系的结构
        Context context = new Context();
        context.setFactoryCode(factoryCode);
        context.setMonth(month);
        context.setYear(year);
        if (StringUtils.isNotBlank(monthPlanVersion)) {
            context.setMonthPlanVersion(monthPlanVersion);
        }
        List<DpDemandPlan> demandPlanList = dataService.getFactoryMonthPlan(context);

        // 构建物料描述到结构名的映射
        Map<String, String> materialToStructureMap = new HashMap<>();
        for (DpDemandPlan demandPlan : demandPlanList) {
            List<MoldCavityInsertMaxValueCalculatorVo> mouldRelationList = mouldRelationMap.get(demandPlan.getMaterialDesc());
            if (!CollectionUtils.isEmpty(mouldRelationList)) {
                for (MoldCavityInsertMaxValueCalculatorVo mouldRelation : mouldRelationList) {
                    mouldRelation.setStructureName(demandPlan.getStructureName());
                }
                materialToStructureMap.put(demandPlan.getMaterialDesc(), demandPlan.getStructureName() + demandPlan.getMainPattern());
            }
        }

        // 4. 按照指定日期返回结果
        if (targetDate != null) {
            // 4.1 按照指定日期返回结果
            Map<String, Object> dailyResults = calculateForSpecificDate(year, month, factoryCode, targetDate,
                    mouldInfoMap, materialToStructureMap);
            return dailyResults;
        } else {
            // 4.2 按照月份返回结果
            Map<String, Object> monthlyResults = calculateForMonth(year, month, factoryCode,
                    mouldInfoMap, materialToStructureMap);
            return monthlyResults;
        }
    }

    /**
     * 计算指定日期的型腔活块可用量
     */
    private Map<String, Object> calculateForSpecificDate(Integer year, Integer month, String factoryCode,
                                                         Date targetDate, Map<String, ProductionMouldInfoVo> mouldInfoMap,
                                                         Map<String, String> materialToStructureMap) {
        // 获取排产周期信息
        Integer cycleStartDay = factoryParamService.getMonthStartDay(factoryCode, ProductTypeEnum.WHOLE_STEEL);
        Date productionStartDate;
        Date productionEndDate;

        if (isNaturalMonth(cycleStartDay)) {
            productionStartDate = getNaturalMonthStartDate(year, month);
            productionEndDate = getNaturalMonthEndDate(year, month);
        } else {
            productionStartDate = getCycleStartDate(year, month, cycleStartDay);
            productionEndDate = getCycleEndDate(year, month, cycleStartDay);
        }

        // 检查目标日期是否在排产周期内
        if (targetDate.before(productionStartDate) || targetDate.after(productionEndDate)) {
            log.warn("目标日期{}不在排产周期内，工厂：{}，排产周期：{} 至 {}",
                    targetDate, factoryCode, productionStartDate, productionEndDate);
            return Collections.emptyMap();
        }

        // 计算目标日期是排产周期的第几天（从1开始）
        int dayOfCycle = DateUtils.getIntervalDays(productionStartDate, targetDate) + 1;

        // 获取停产日
        Set<Integer> stopDays = getStopDay(factoryCode, productionStartDate, productionEndDate);

        Map<String, Object> results = new HashMap<>();

        // 如果是停产日，直接返回空结果
        if (stopDays.contains(dayOfCycle)) {
            results.put("cavityResults", Collections.emptyMap());
            results.put("insertResults", Collections.emptyMap());
            results.put("date", targetDate);
            results.put("isProductionDay", false);
            return results;
        }

        // 计算型腔可用量（按结构分组）
        Map<String, Integer> cavityResults = new HashMap<>();
        // 计算活块可用量（按物料描述分组）
        Map<String, Integer> insertResults = new HashMap<>();

        // 临时存储，用于去重
        Map<String, Set<String>> cavityTempMap = new HashMap<>(); // key: structureName, value: mouldCodes
        Map<String, Set<String>> insertTempMap = new HashMap<>(); // key: materialDesc, value: mouldCodes

        // 遍历所有模具
        for (Map.Entry<String, ProductionMouldInfoVo> entry : mouldInfoMap.entrySet()) {
            String mouldCode = entry.getKey();
            ProductionMouldInfoVo mouldInfo = entry.getValue();

            // 检查模具在目标日期是否可用
            if (mouldInfo.getProductionDaySet() != null &&
                    mouldInfo.getProductionDaySet().contains(dayOfCycle)) {

                // 遍历模具关联的物料描述
                for (String materialDesc : mouldInfo.getAssociationMaterialSet()) {
                    String structureNameMailPattern = materialToStructureMap.get(materialDesc);

                    // 计算活块可用量（按物料描述）
                    insertTempMap.computeIfAbsent(materialDesc, k -> new HashSet<>()).add(mouldCode);

                    // 计算型腔可用量（按结构+主花纹）
                    if (StringUtils.isNotBlank(structureNameMailPattern)) {
                        cavityTempMap.computeIfAbsent(structureNameMailPattern, k -> new HashSet<>()).add(mouldCode);
                    }
                }
            }
        }

        // 转换为最终结果
        for (Map.Entry<String, Set<String>> entry : cavityTempMap.entrySet()) {
            cavityResults.put(entry.getKey(), entry.getValue().size());
        }

        for (Map.Entry<String, Set<String>> entry : insertTempMap.entrySet()) {
            insertResults.put(entry.getKey(), entry.getValue().size());
        }

        results.put("cavityResults", cavityResults);
        results.put("insertResults", insertResults);
        results.put("date", targetDate);
        results.put("isProductionDay", true);
        results.put("dayOfCycle", dayOfCycle);

        return results;
    }

    /**
     * 计算整个月份的型腔活块可用量最大值
     */
    private Map<String, Object> calculateForMonth(Integer year, Integer month, String factoryCode,
                                                  Map<String, ProductionMouldInfoVo> mouldInfoMap,
                                                  Map<String, String> materialToStructureMap) {
        // 获取排产周期信息
        Integer cycleStartDay = factoryParamService.getMonthStartDay(factoryCode, ProductTypeEnum.WHOLE_STEEL);
        Date productionStartDate;
        Date productionEndDate;

        if (isNaturalMonth(cycleStartDay)) {
            productionStartDate = getNaturalMonthStartDate(year, month);
            productionEndDate = getNaturalMonthEndDate(year, month);
        } else {
            productionStartDate = getCycleStartDate(year, month, cycleStartDay);
            productionEndDate = getCycleEndDate(year, month, cycleStartDay);
        }

        // 获取整个月份的天数和停产日
        Integer monthDays = getMonthDays(productionStartDate, productionEndDate);
        Set<Integer> stopDays = getStopDay(factoryCode, productionStartDate, productionEndDate);

        // 初始化结果存储
        Map<String, Integer> cavityMaxResults = new HashMap<>();
        Map<String, Integer> insertMaxResults = new HashMap<>();

        // 存储每天的结果，用于详细分析
        Map<Integer, Map<String, Object>> dailyDetails = new HashMap<>();

        // 遍历整个排产周期的每一天
        for (int day = 1; day <= monthDays; day++) {
            // 如果是停产日，跳过
            if (stopDays.contains(day)) {
                Map<String, Object> dayResult = new HashMap<>();
                dayResult.put("cavityCount", 0);
                dayResult.put("insertCount", 0);
                dayResult.put("isProductionDay", false);
                dailyDetails.put(day, dayResult);
                continue;
            }

            // 计算当天的可用量
            Map<String, Set<String>> cavityDayMap = new HashMap<>();
            Map<String, Set<String>> insertDayMap = new HashMap<>();

            // 遍历所有模具
            for (Map.Entry<String, ProductionMouldInfoVo> entry : mouldInfoMap.entrySet()) {
                String mouldCode = entry.getKey();
                ProductionMouldInfoVo mouldInfo = entry.getValue();

                // 检查模具在当前日期是否可用
                if (mouldInfo.getProductionDaySet() != null &&
                        mouldInfo.getProductionDaySet().contains(day)) {

                    // 遍历模具关联的物料描述
                    for (String materialDesc : mouldInfo.getAssociationMaterialSet()) {
                        String structureNameMailPattern = materialToStructureMap.get(materialDesc);

                        // 计算活块可用量
                        insertDayMap.computeIfAbsent(materialDesc, k -> new HashSet<>()).add(mouldCode);

                        // 计算型腔可用量
                        if (StringUtils.isNotBlank(structureNameMailPattern)) {
                            cavityDayMap.computeIfAbsent(structureNameMailPattern, k -> new HashSet<>()).add(mouldCode);
                        }
                    }
                }
            }

            // 更新最大值
            for (Map.Entry<String, Set<String>> entry : cavityDayMap.entrySet()) {
                String structureName = entry.getKey();
                int count = entry.getValue().size();
                cavityMaxResults.put(structureName,
                        Math.max(cavityMaxResults.getOrDefault(structureName, 0), count));
            }

            for (Map.Entry<String, Set<String>> entry : insertDayMap.entrySet()) {
                String materialDesc = entry.getKey();
                int count = entry.getValue().size();
                insertMaxResults.put(materialDesc,
                        Math.max(insertMaxResults.getOrDefault(materialDesc, 0), count));
            }

            // 保存每天详细结果
            Map<String, Object> dayResult = new HashMap<>();
            dayResult.put("cavityCount", cavityDayMap.values().stream().mapToInt(Set::size).sum());
            dayResult.put("insertCount", insertDayMap.values().stream().mapToInt(Set::size).sum());
            dayResult.put("cavityDetail", cavityDayMap);
            dayResult.put("insertDetail", insertDayMap);
            dayResult.put("isProductionDay", true);
            dailyDetails.put(day, dayResult);
        }

        Map<String, Object> results = new HashMap<>();
        results.put("cavityMaxResults", cavityMaxResults);
        results.put("insertMaxResults", insertMaxResults);
        results.put("dailyDetails", dailyDetails);
        results.put("productionStartDate", productionStartDate);
        results.put("productionEndDate", productionEndDate);
        results.put("monthDays", monthDays);
        results.put("stopDays", stopDays);
        results.put("stopDayCount", stopDays.size());

        return results;
    }

    /**
     * 根据物料可用模具关系，构建排产信息
     */
    private Map<String, ProductionMouldInfoVo> createProductionMouldInfo(Integer year, Integer month, String factoryCode,
                                                                         Map<String, List<MoldCavityInsertMaxValueCalculatorVo>> mouldAssociationMap) {
        if (CollectionUtils.isEmpty(mouldAssociationMap)) {
            return Collections.emptyMap();
        }
        Map<String, ProductionMouldInfoVo> mouldInfoMap = new HashMap<>();
        mouldAssociationMap.forEach((materialDesc, associationList) -> {
            if (CollectionUtils.isEmpty(associationList)) {
                return;
            }
            associationList.forEach(associationInfo -> {
                String mouldCode = associationInfo.getMouldCode();
                if (StringUtils.isBlank(mouldCode)) {
                    return;
                }
                MouldRelationTypeEnum relationType = MouldRelationTypeEnum.getInstance(associationInfo.getRelationType());
                ProductionMouldInfoVo productionMouldInfo = mouldInfoMap.get(mouldCode);
                if (null == productionMouldInfo) {
                    productionMouldInfo = createEmptyProductionMouldInfo(mouldCode, relationType);
                    if (null == productionMouldInfo) {
                        return;
                    }
                    // 设置模具的可排产日集合
                    productionMouldInfo.setProductionDaySet(getProductionDayInfo(year, month, factoryCode, relationType, associationInfo.getBoardingDate()));
                    mouldInfoMap.put(mouldCode, productionMouldInfo);
                }
                // 加入关联关系
                productionMouldInfo.getAssociationMaterialSet().add(materialDesc);
            });
        });
        return mouldInfoMap;
    }

    /**
     * 设置模具的可排产日信息
     */
    public Set<Integer> getProductionDayInfo(Integer year, Integer month, String factoryCode,
                                             MouldRelationTypeEnum relationType, Date boardingDate) {
        // 获取工厂周期配置
        Integer cycleStartDay = factoryParamService.getMonthStartDay(factoryCode, ProductTypeEnum.WHOLE_STEEL);
        Date productionStartDate;
        Date productionEndDate;

        // 获取生产开始和结束日期
        if (isNaturalMonth(cycleStartDay)) {
            productionStartDate = getNaturalMonthStartDate(year, month);
            productionEndDate = getNaturalMonthEndDate(year, month);
        } else {
            productionStartDate = getCycleStartDate(year, month, cycleStartDay);
            productionEndDate = getCycleEndDate(year, month, cycleStartDay);
        }

        // 模具关系
        if (relationType == MouldRelationTypeEnum.SKU_RELATION_CONFIGURATION) {
            // 如果是从SKU与模具关系里面获取的
            // 要结合排产周期及停产日，得到排产天集合,返回
            Integer monthDays = getMonthDays(productionStartDate, productionEndDate);
            if (monthDays < BigDecimal.ONE.intValue()) {
                return Collections.emptySet();
            }
            Set<Integer> productionDaySet = new HashSet<>(monthDays);
            Set<Integer> stopDays = getStopDay(factoryCode, productionStartDate, productionEndDate);
            for (int day = ProductionConstant.MONTH_START_DAY; day <= monthDays; day++) {
                if (stopDays.contains(day)) {
                    continue;
                }
                productionDaySet.add(day);
            }
            return productionDaySet;
        }

        if (null == boardingDate) {
            return new HashSet<>();
        }

        // 初始化结果集合
        Set<Integer> productionDaySet = new HashSet<>();
        // 可用时间 = 上机日期 + 1
        int startDay = DateUtils.getIntervalDays(productionStartDate, boardingDate) + BigDecimal.ONE.intValue();
        Integer monthDays = getMonthDays(productionStartDate, productionEndDate);

        Set<Integer> monthStopDaySet = getStopDay(factoryCode, productionStartDate, productionEndDate);
        for (int day = startDay; day <= monthDays; day++) {
            if (monthStopDaySet.contains(day)) {
                continue;
            }
            productionDaySet.add(day);
        }
        return productionDaySet;
    }

    /**
     * 获取停产日
     */
    private Set<Integer> getStopDay(String factoryCode, Date productionStartDate, Date productionEndDate) {
        List<ProductionDayInfoVo> productionDayInfoList = getProductCalendar(factoryCode, productionStartDate, productionEndDate);
        if (CollectionUtils.isEmpty(productionDayInfoList)) {
            return Collections.emptySet();
        }

        // 停产设置
        List<ProductionDayInfoVo> stopDays = productionDayInfoList.stream()
                .filter(productionDayInfo -> YesOrNoEnum.NO.getCode().equals(productionDayInfo.getDayFlag()))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(stopDays)) {
            return Collections.emptySet();
        }

        Set<Integer> stopDaySet = new HashSet<>();
        stopDays.forEach(stopProductionInfo -> {
            Date stopProduction = stopProductionInfo.getProductionDate();
            Integer stopDay = DateUtils.getIntervalDays(productionStartDate, stopProduction) + 1; // 从1开始
            stopDaySet.add(stopDay);
        });
        return stopDaySet;
    }

    /**
     * 获取工作日历信息
     */
    public List<ProductionDayInfoVo> getProductCalendar(String factoryCode, Date productionStartDate, Date productionEndDate) {
        if (StringUtils.isBlank(factoryCode) || null == productionStartDate || null == productionEndDate) {
            return Collections.emptyList();
        }
        QueryWrapper<MdmWorkCalendar> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        queryWrapper.eq("PROC_CODE", ProductionProcessesTypeEnum.MONTH_PLAN.getProcCode());
        queryWrapper.ge("PRODUCTION_DATE", productionStartDate);
        queryWrapper.le("PRODUCTION_DATE", productionEndDate);
        List<MdmWorkCalendar> configurationList = mdmWorkCalendarEntityMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(configurationList)) {
            return Collections.emptyList();
        }
        return BeanCopyUtils.copyBeanList(configurationList, ProductionDayInfoVo.class);
    }

    /**
     * 获取已有模具配置并添加到列表
     */
    private void addExistingMouldInfo(String factoryCode, Integer year, Integer month, String monthPlanVersion,
                                      List<MoldCavityInsertMaxValueCalculatorVo> allMouldRelationInfoList) {
        List<MoldCavityInsertMaxValueCalculatorVo> productMouldInfoList =
                factoryMonthPlanProductMouldMapper.getEnableProductionMouldInfoByNetDemand(factoryCode, year, month, monthPlanVersion);
        if (!CollectionUtils.isEmpty(productMouldInfoList)) {
            allMouldRelationInfoList.addAll(productMouldInfoList);
        }
    }

    /**
     * 获取新模具到货计划并添加到列表
     */
    private void addMouldDeliveryInfoWithDeDuplication(String factoryCode, Integer year, Integer month, String monthPlanVersion,
                                                       List<MoldCavityInsertMaxValueCalculatorVo> allMouldRelationInfoList) {
        Set<String> mouldCodeSet = new HashSet<>();
        Integer cycleStartDay = factoryParamService.getMonthStartDay(factoryCode, ProductTypeEnum.WHOLE_STEEL);
        Date productionStartDate;
        Date productionEndDate;

        // 获取生产开始和结束日期
        if (isNaturalMonth(cycleStartDay)) {
            productionStartDate = getNaturalMonthStartDate(year, month);
            productionEndDate = getNaturalMonthEndDate(year, month);
        } else {
            productionStartDate = getCycleStartDate(year, month, cycleStartDay);
            productionEndDate = getCycleEndDate(year, month, cycleStartDay);
        }

        // 获取新模具到货信息
        List<MoldCavityInsertMaxValueCalculatorVo> mouldDeliveryList =
                factoryMonthPlanProductMouldMapper.getEnableMouldDeliveryInfoByNetDemand(factoryCode, year, month, monthPlanVersion, productionStartDate, productionEndDate);

        // 按照工厂+型腔模号分组汇总
        mouldCodeSet = allMouldRelationInfoList.stream()
                .collect(Collectors.groupingBy(item -> item.getMouldCode() + item.getFactoryCode()))
                .keySet();

        for (MoldCavityInsertMaxValueCalculatorVo mouldInfo : mouldDeliveryList) {
            // 如果不存在，则添加
            if (!mouldCodeSet.contains(mouldInfo.getMouldCode() + mouldInfo.getFactoryCode())) {
                allMouldRelationInfoList.add(mouldInfo);
                mouldCodeSet.add(mouldInfo.getMouldCode() + mouldInfo.getFactoryCode());
            }
        }
    }

    /**
     * 判断是否为自然月
     */
    private boolean isNaturalMonth(Integer cycleStartDay) {
        return cycleStartDay > ProductionConstant.NO_NATURAL_MONTH_MAX_VALUE || cycleStartDay <= ProductionConstant.MONTH_START_DAY;
    }

    /**
     * 获取自然月开始日期
     */
    private Date getNaturalMonthStartDate(Integer year, Integer month) {
        LocalDate monthStart = LocalDate.of(year, month, ProductionConstant.MONTH_START_DAY);
        return DateUtils.getDate(monthStart);
    }

    /**
     * 获取自然月结束日期
     */
    private Date getNaturalMonthEndDate(Integer year, Integer month) {
        LocalDate monthStart = LocalDate.of(year, month, ProductionConstant.MONTH_START_DAY);
        return DateUtils.getDate(monthStart.with(TemporalAdjusters.lastDayOfMonth()));
    }

    /**
     * 获取周期开始日期
     */
    private Date getCycleStartDate(Integer year, Integer month, Integer cycleStartDay) {
        LocalDate previousMonth = LocalDate.of(year, month, cycleStartDay);
        return DateUtils.getDate(previousMonth.getYear(), previousMonth.getMonthValue(), cycleStartDay);
    }

    /**
     * 获取周期结束日期
     */
    private Date getCycleEndDate(Integer year, Integer month, Integer cycleStartDay) {
        return DateUtils.getDate(year, month, cycleStartDay - 1);
    }

    /**
     * 获取排产周期的天数
     */
    public Integer getMonthDays(Date productionStartDate, Date productionEndDate) {
        return DateUtils.getIntervalDays(productionStartDate, productionEndDate) + 1; // 包含开始和结束日期
    }

    /**
     * 获取需要排产的SKU的模具配置信息
     */
    public Map<String, List<MoldCavityInsertMaxValueCalculatorVo>> getProductionMouldInfo(Integer year, Integer month, String factoryCode,
                                                                                          Date targetDate, String monthPlanVersion) {
        List<MoldCavityInsertMaxValueCalculatorVo> allMouldRelationInfoList = new ArrayList<>();

        // 获取已有模具的配置关系
        addExistingMouldInfo(factoryCode, year, month, monthPlanVersion, allMouldRelationInfoList);

        // 获取新模具到货计划并去重
        addMouldDeliveryInfoWithDeDuplication(factoryCode, year, month, monthPlanVersion, allMouldRelationInfoList);

        // 如果列表为空，返回空映射
        if (CollectionUtils.isEmpty(allMouldRelationInfoList)) {
            return Collections.emptyMap();
        }
        // 按 materialDesc 分组并返回
        return allMouldRelationInfoList.stream()
                .collect(Collectors.groupingBy(MoldCavityInsertMaxValueCalculatorVo::getMaterialDesc));
    }


    /**
     * 创建空的排产模具信息
     * 只包含型腔模号及relationType类型
     *
     * @param mouldCode    型腔模号
     * @param relationType 关系类型
     * @return
     */
    public static ProductionMouldInfoVo createEmptyProductionMouldInfo(String mouldCode, MouldRelationTypeEnum relationType) {
        if (StringUtils.isBlank(mouldCode)) {
            return null;
        }
        ProductionMouldInfoVo productionMouldInfo = new ProductionMouldInfoVo();
        productionMouldInfo.setMouldCode(mouldCode);
        if (null == relationType) {
            productionMouldInfo.setRelationType(MouldRelationTypeEnum.SKU_RELATION_CONFIGURATION);
        } else {
            productionMouldInfo.setRelationType(relationType);
        }
        //可排产日信息
        productionMouldInfo.setProductionDaySet(new HashSet<>(64));
        //关联SKU
        productionMouldInfo.setAssociationMaterialSet(new HashSet<>(32));
        //排产完毕日
        productionMouldInfo.setFinishDaySet(new HashSet<>(64));
        return productionMouldInfo;
    }

}