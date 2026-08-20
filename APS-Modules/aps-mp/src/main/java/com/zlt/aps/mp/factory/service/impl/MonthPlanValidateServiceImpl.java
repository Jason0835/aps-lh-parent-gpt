package com.zlt.aps.mp.factory.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.BusiConstant;
import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.enums.ProductTypeEnum;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.mp.api.domain.capacity.MpDailyCapacityLimitVo;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.engine.basedata.assemble.appoint.GroupAppointDataHandler;
import com.zlt.aps.mp.engine.basedata.assemble.cyclegroup.CycleGroupDataHandler;
import com.zlt.aps.mp.engine.basedata.assemble.datalist.GroupListHandler;
import com.zlt.aps.mp.engine.basedata.assemble.history.ProductionHistoryHandler;
import com.zlt.aps.mp.engine.capacity.MpMonthPlanDailyCapacityLimit;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.handler.SimulateResultHelper;
import com.zlt.aps.mp.engine.handler.embryobalance.DayEmbryoUsedInfo;
import com.zlt.aps.mp.engine.handler.embryobalance.EmbryoUsedLhMachineInfo;
import com.zlt.aps.mp.engine.handler.embryobalance.GroupCxMachineConfiguration;
import com.zlt.aps.mp.engine.scheduling.AbstractDataLoaderService;
import com.zlt.aps.mp.engine.scheduling.BaseDataContainer;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.engine.scheduling.cxcapacity.ProductionCapacityParamConfiguration;
import com.zlt.aps.mp.engine.service.DpRequireDataService;
import com.zlt.aps.mp.engine.service.MonthProductionDataService;
import com.zlt.aps.mp.engine.service.ProductionMdmDataService;
import com.zlt.aps.mp.factory.helper.GroupValidateEmbryoAllocationHelper;
import com.zlt.aps.mp.factory.service.MonthPlanValidateService;
import com.zlt.common.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 月度计划数据校验业务
 * 当前校验胎胚是否可进行多机台分配
 *
 * @author ZLT
 * @date 20260818
 */
@Slf4j
@Service
public class MonthPlanValidateServiceImpl extends AbstractDataLoaderService implements MonthPlanValidateService {

    public MonthPlanValidateServiceImpl(GroupListHandler groupListHandler,
                                        ProductionMdmDataService dataService,
                                        GroupAppointDataHandler groupAppointHandler,
                                        DpRequireDataService dpRequireDataService,
                                        CycleGroupDataHandler cycleGroupDataHandler,
                                        ProductionHistoryHandler productionHistoryHandler,
                                        MonthProductionDataService monthProductionDataService) {
        super(groupListHandler, dataService, groupAppointHandler, dpRequireDataService, cycleGroupDataHandler, productionHistoryHandler, monthProductionDataService);
    }

    @Override
    public void run(Context context, Object userObj) {

    }

    @Override
    public void validateEmbryoAllocation(String monthPlanVersion,
                                         String productVersion,
                                         Map<Integer, MpDailyCapacityLimitVo> dailyCapacityMap,
                                         List<FactoryMonthPlanMouldDayResult> monthPlanList) {
        if (CollectionUtils.isEmpty(monthPlanList)) {
            return;
        }
        //构建组装使用的参数
        FactoryMonthPlanMouldDayResult arbitrary = monthPlanList.get(BigDecimal.ZERO.intValue());
        TbrProductionContext productionContext = buildContext(arbitrary, monthPlanVersion, productVersion);
        loadInitData(productionContext);
        //转产分配表
        List<MpStructureAllocation> allAllocationList = getMonthProductionDataService().getStructureAllocationInfoByProductionVersion(productionContext);
        //构建各分组，每日各胎胚使用硫化机台数信息
        List<GroupValidateEmbryoAllocationHelper> groupDayEmbryoUsedList = buildGroupDayEmbryoUsedInfo(productionContext, dailyCapacityMap, monthPlanList);
        if (CollectionUtils.isEmpty(groupDayEmbryoUsedList)) {
            return;
        }
        String errorFormat = I18nUtil.getMessage("alg.data.mp.checkEmbryoTypeBalance");
        //构建各分组，每日的限制信息
        addGroupDayLimitInfo(productionContext, groupDayEmbryoUsedList, allAllocationList);
        StringBuilder errorInfo = new StringBuilder();
        groupDayEmbryoUsedList.forEach(singleGroup -> {
            String groupName = singleGroup.getGroupName();
            Map<Integer, DayEmbryoUsedInfo> dayLimitMap = singleGroup.getDayProductionLimitInfo();
            if (CollectionUtils.isEmpty(dayLimitMap)) {
                return;
            }
            Set<Integer> errorDaySet = Sets.newHashSet();
            dayLimitMap.forEach((productionDay, dayLimitValidate) -> {
                boolean validateResult = dayLimitValidate.checkIsBalanceAllocation();
                if (!validateResult) {
                    errorDaySet.add(productionDay);
                }
            });
            if (CollectionUtils.isEmpty(errorDaySet)) {
                return;
            }
            List<Integer> errorDayList = Lists.newArrayList(errorDaySet);
            errorDayList.sort(Comparator.comparing(Integer::intValue));
            String daysInfo = errorDayList.stream().map(String::valueOf).collect(Collectors.joining(StringConstant.COMMA));
            errorInfo.append(String.format(errorFormat, groupName, daysInfo)).append(BusiConstant.WeekRollAdjust.SPLIT_FRONT_NEW_LINE);
        });
        if (!StringUtil.isEmptyWithTrim(errorInfo.toString())) {
            throw new BusinessException(errorInfo.toString());
        }
    }

    /**
     * 构建初始化对象
     *
     * @param arbitrary
     * @param monthPlanVersion
     * @param productVersion
     * @return
     */
    private TbrProductionContext buildContext(FactoryMonthPlanMouldDayResult arbitrary, String monthPlanVersion, String productVersion) {
        String productTypeCode = arbitrary.getProductTypeCode();
        String factoryCode = arbitrary.getFactoryCode();
        Integer year = arbitrary.getYear();
        Integer month = arbitrary.getMonth();
        Context context = new Context();
        context.setFactoryCode(factoryCode);
        context.setYear(year);
        context.setMonth(month);
        context.setMonthPlanVersion(monthPlanVersion);
        context.setProductionVersion(productVersion);
        context.setPrefixVersion(StringUtils.EMPTY);
        context.setProductType(ProductTypeEnum.getEnumByValue(productTypeCode));
        TbrProductionContext productionContext = (TbrProductionContext) buildProductionContext(context);
        //基础数据容器存储
        productionContext.setBaseDataContainer(new BaseDataContainer());
        productionContext.setSimulateResult(new SimulateResultHelper());
        context.setProductionVersion(productionContext.createNewProductionVersion());
        context.setOperationWorkNo(productionContext.createNewOperationWorkNo());
        resetTbrInitLogRecorderInfo(productionContext, context);
        return productionContext;
    }

    /**
     * 加载数据
     *
     * @param productionContext
     */
    private void loadInitData(TbrProductionContext productionContext) {
        //获取排产参数设定
        ProductionCapacityParamConfiguration paramConfiguration = createParamConfiguration(productionContext);
        if (null == paramConfiguration) {
            paramConfiguration = new ProductionCapacityParamConfiguration();
        }
        productionContext.getBaseDataContainer().setParamConfiguration(paramConfiguration);
        //获取周期内的生产日历信息
        setMonthProductionDays(productionContext);
        //获取成型机台信息--日产信息
        Map<String, CxMachineBaseInfoVo> cxMachineBaseInfo = getDataService().getCxMachineBaseInfo(productionContext);
        productionContext.getBaseDataContainer().setCxMachineBaseInfo(cxMachineBaseInfo);
    }

    /**
     * 构建各分组，每日各胎胚占用硫化机台数信息
     *
     * @param productionContext
     * @param dailyCapacityMap
     * @param monthPlanList
     * @return
     */
    private List<GroupValidateEmbryoAllocationHelper> buildGroupDayEmbryoUsedInfo(TbrProductionContext productionContext, Map<Integer, MpDailyCapacityLimitVo> dailyCapacityMap, List<FactoryMonthPlanMouldDayResult> monthPlanList) {
        if (CollectionUtils.isEmpty(monthPlanList)) {
            return Collections.emptyList();
        }
        MpMonthPlanDailyCapacityLimit calculator = new MpMonthPlanDailyCapacityLimit();
        Map<String, Object> paramMap = new ProductionPlanGroupInfo().composeDailyCapacityParamMap(productionContext);
        Integer monthDays = productionContext.getMonthDays();
        //按分组名分组
        Map<String, List<FactoryMonthPlanMouldDayResult>> groupNameGroupMap = monthPlanList.stream().collect(Collectors.groupingBy(FactoryMonthPlanMouldDayResult::getStructureName));
        List<GroupValidateEmbryoAllocationHelper> groupEmbryoUsedInfoList = Lists.newArrayList();
        groupNameGroupMap.forEach((groupName, allProductionSkuList) -> {
            if (CollectionUtils.isEmpty(allProductionSkuList)) {
                return;
            }
            GroupValidateEmbryoAllocationHelper groupInfo = GroupValidateEmbryoAllocationHelper.buildEmpty(groupName);
            groupEmbryoUsedInfoList.add(groupInfo);
            //总的使用硫化机台数
            Map<Integer, DayEmbryoUsedInfo> dayEmbryoUsedInfoMap = getDaySumUsedInfo(groupName, calculator, allProductionSkuList, monthDays, dailyCapacityMap, paramMap);
            groupInfo.setDayProductionLimitInfo(dayEmbryoUsedInfoMap);
            //分胎胚统计使用硫化机台数
            Map<Integer, List<EmbryoUsedLhMachineInfo>> dayEmbryoUsedLhMachineInfoMap = getSplitEmbryoStatisticsUsedLhMachines(calculator, allProductionSkuList, monthDays, dailyCapacityMap, paramMap);
            if (CollectionUtils.isEmpty(dayEmbryoUsedLhMachineInfoMap)) {
                groupInfo.setDayEmbryoUsedLhMachineInfoMap(Collections.emptyMap());
                return;
            }
            groupInfo.setDayEmbryoUsedLhMachineInfoMap(dayEmbryoUsedLhMachineInfoMap);
        });
        if (CollectionUtils.isEmpty(groupEmbryoUsedInfoList)) {
            return Collections.emptyList();
        }
        return groupEmbryoUsedInfoList;
    }

    /**
     * 增加日排产的成型机台限制信息
     *
     * @param productionContext
     * @param groupDayEmbryoUsedList
     * @param allAllocationList
     */
    private void addGroupDayLimitInfo(TbrProductionContext productionContext,
                                      List<GroupValidateEmbryoAllocationHelper> groupDayEmbryoUsedList,
                                      List<MpStructureAllocation> allAllocationList) {
        if (CollectionUtils.isEmpty(allAllocationList) || CollectionUtils.isEmpty(groupDayEmbryoUsedList)) {
            return;
        }
        ProductionPlanGroupInfo handler = new ProductionPlanGroupInfo();
        //特殊处理，多台需要加减机台数
        Map<String, Integer> extraMap = productionContext.getBaseDataContainer().getParamConfiguration().getExtraMap();
        groupDayEmbryoUsedList.forEach(singleGroup -> {
            String groupName = singleGroup.getGroupName();
            Integer extraLhMachines;
            if (extraMap.containsKey(groupName)) {
                extraLhMachines = extraMap.get(groupName);
            } else {
                extraLhMachines = BigDecimal.ZERO.intValue();
            }
            List<MpStructureAllocation> groupAllocationList = allAllocationList.stream().filter(singleAllocation -> singleGroup.getGroupName().equals(singleAllocation.getStructureName())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(groupAllocationList)) {
                return;
            }
            Map<Integer, DayEmbryoUsedInfo> dayProductionLimitInfo = singleGroup.getDayProductionLimitInfo();
            Map<Integer, List<EmbryoUsedLhMachineInfo>> dayEmbryoUsedLhMachineInfoMap = singleGroup.getDayEmbryoUsedLhMachineInfoMap();
            Map<Integer, DayEmbryoUsedInfo> realDayProductionLimit = Maps.newHashMap();
            Set<Integer> allProductionDayInfo = handler.getAllProductionDay(productionContext, groupAllocationList);
            allProductionDayInfo.forEach(productionDay -> {
                List<MpStructureAllocation> dayAllocationInfo = handler.getProductionConfigurationByDay(productionContext, productionDay, groupAllocationList);
                if (CollectionUtils.isEmpty(dayAllocationInfo)) {
                    return;
                }
                DayEmbryoUsedInfo usedInfo = dayProductionLimitInfo.get(productionDay);
                List<GroupCxMachineConfiguration> cxMachineConfigurationList = buildGroupDayConfiguration(productionContext, groupName, dayAllocationInfo);
                boolean isMoreCxMachine = cxMachineConfigurationList.size() > BigDecimal.ONE.intValue();
                Integer realExtraLhMachines = extraLhMachines;
                if (!isMoreCxMachine) {
                    realExtraLhMachines = BigDecimal.ZERO.intValue();
                }
                Integer sumUsedLhMachines;
                if (null == usedInfo) {
                    sumUsedLhMachines = BigDecimal.ZERO.intValue();
                } else {
                    sumUsedLhMachines = usedInfo.getSumUsedLhMachines();
                }
                List<EmbryoUsedLhMachineInfo> embryoUsedInfo;
                if (CollectionUtils.isEmpty(dayEmbryoUsedLhMachineInfoMap)) {
                    embryoUsedInfo = Collections.emptyList();
                } else {
                    embryoUsedInfo = dayEmbryoUsedLhMachineInfoMap.get(productionDay);
                }
                realDayProductionLimit.put(productionDay, new DayEmbryoUsedInfo(productionDay, groupName, realExtraLhMachines, sumUsedLhMachines, cxMachineConfigurationList, embryoUsedInfo));
            });
            singleGroup.setDayProductionLimitInfo(realDayProductionLimit);
        });
        return;
    }

    /**
     * 得到分组下总的使用硫化机台数信息
     *
     * @param groupName            分组名：TBR-结构
     * @param calculator           计算器
     * @param allProductionSkuList 分组下所有排产Sku信息
     * @param monthDays            月份最大天数
     * @param dailyCapacityMap     日产限制
     * @param paramMap             参数
     * @return
     */
    private Map<Integer, DayEmbryoUsedInfo> getDaySumUsedInfo(String groupName,
                                                              MpMonthPlanDailyCapacityLimit calculator,
                                                              List<FactoryMonthPlanMouldDayResult> allProductionSkuList,
                                                              Integer monthDays,
                                                              Map<Integer, MpDailyCapacityLimitVo> dailyCapacityMap,
                                                              Map<String, Object> paramMap) {
        if (CollectionUtils.isEmpty(allProductionSkuList)) {
            return Collections.emptyMap();
        }
        Map<Integer, DayEmbryoUsedInfo> dayEmbryoUsedInfoMap = Maps.newHashMap();
        for (int i = ProductionConstant.MONTH_START_DAY; i <= monthDays; i++) {
            MpDailyCapacityLimitVo dayLimit = dailyCapacityMap.get(i);
            calculator.calcLhMachinesWithEmbryoTypes(allProductionSkuList, i, dayLimit, paramMap, null, null);
            Integer sumUsedLhMachines = dayLimit.getUsedLhMachines();
            Integer usedEmbryoTypes = dayLimit.getUsedEmbryoTypes();
            if (null == sumUsedLhMachines || null == usedEmbryoTypes) {
                continue;
            }
            if (sumUsedLhMachines <= BigDecimal.ZERO.intValue() || usedEmbryoTypes <= BigDecimal.ZERO.intValue()) {
                continue;
            }
            DayEmbryoUsedInfo dayInfo = new DayEmbryoUsedInfo(i, groupName, BigDecimal.ZERO.intValue(), sumUsedLhMachines, Collections.emptyList(), Collections.emptyList());
            dayEmbryoUsedInfoMap.put(i, dayInfo);
        }
        if (CollectionUtils.isEmpty(dayEmbryoUsedInfoMap)) {
            return Collections.emptyMap();
        }
        return dayEmbryoUsedInfoMap;
    }

    /**
     * 获取分胎胚的使用硫化机台数
     *
     * @param calculator           计算器
     * @param allProductionSkuList 分组同胎胚下所有排产Sku信息
     * @param monthDays            月最大天数
     * @param dailyCapacityMap     日限制信息
     * @param paramMap             参数信息
     * @return
     */
    private Map<Integer, List<EmbryoUsedLhMachineInfo>> getSplitEmbryoStatisticsUsedLhMachines(MpMonthPlanDailyCapacityLimit calculator,
                                                                                               List<FactoryMonthPlanMouldDayResult> allProductionSkuList,
                                                                                               Integer monthDays,
                                                                                               Map<Integer, MpDailyCapacityLimitVo> dailyCapacityMap,
                                                                                               Map<String, Object> paramMap) {
        Map<String, Map<Integer, Integer>> embryoDayUsedInfoMap = Maps.newHashMap();
        //按胎胚分组
        Map<String, List<FactoryMonthPlanMouldDayResult>> embryoGroupMap = allProductionSkuList.stream().collect(Collectors.groupingBy(FactoryMonthPlanMouldDayResult::getEmbryoCode));
        if (CollectionUtils.isEmpty(embryoGroupMap)) {
            return Collections.emptyMap();
        }
        embryoGroupMap.forEach((embryoCode, sameEmbryoCodeSkuInfo) -> {
            if (StringUtils.isBlank(embryoCode) || CollectionUtils.isEmpty(sameEmbryoCodeSkuInfo)) {
                return;
            }
            //3. 循环计算日产能
            Map<Integer, Integer> dayUsedInfoMap = Maps.newHashMap();
            for (int i = ProductionConstant.MONTH_START_DAY; i <= monthDays; i++) {
                MpDailyCapacityLimitVo dayLimit = dailyCapacityMap.get(i);
                Integer usedLhMachines = getUsedLhMachines(i, sameEmbryoCodeSkuInfo, calculator, paramMap, dayLimit);
                if (null == usedLhMachines || usedLhMachines <= BigDecimal.ZERO.intValue()) {
                    continue;
                }
                dayUsedInfoMap.put(i, usedLhMachines);
            }
            if (CollectionUtils.isEmpty(dayUsedInfoMap)) {
                return;
            }
            embryoDayUsedInfoMap.put(embryoCode, dayUsedInfoMap);
        });
        Map<Integer, List<EmbryoUsedLhMachineInfo>> dayEmbryoUsedLhMachineInfoMap = buildDayEmbryoUsedInfo(embryoDayUsedInfoMap);
        if (CollectionUtils.isEmpty(dayEmbryoUsedLhMachineInfoMap)) {
            return Collections.emptyMap();
        }
        return dayEmbryoUsedLhMachineInfoMap;
    }

    /**
     * 构建日分配成型信息
     *
     * @param productionContext 排产上下文
     * @param groupName         分组名
     * @param dayAllocationInfo 分配机台信息
     * @return
     */
    private List<GroupCxMachineConfiguration> buildGroupDayConfiguration(TbrProductionContext productionContext, String groupName, List<MpStructureAllocation> dayAllocationInfo) {
        if (CollectionUtils.isEmpty(dayAllocationInfo) || StringUtils.isBlank(groupName)) {
            return Collections.emptyList();
        }
        Map<String, GroupCxMachineConfiguration> cxMachineConfigurationMap = Maps.newHashMap();
        dayAllocationInfo.forEach(singleCxMachineInfo -> {
            String cxMachineCode = singleCxMachineInfo.getCxMachineCode();
            CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineInfoByCode(cxMachineCode);
            if (null == cxMachineInfo) {
                return;
            }
            GroupCxMachineConfiguration configuration = cxMachineConfigurationMap.get(cxMachineCode);
            if (null != configuration) {
                return;
            }
            Integer maxEmbryoCodeCount = singleCxMachineInfo.getMaxEmbryoCodeCount();
            Integer maxLhMachineCount = singleCxMachineInfo.getMaxLhMachineCount();
            Set<String> fixedEmbryoCodeInfo = cxMachineInfo.getFixedEmbryoCodeInfo();
            configuration = new GroupCxMachineConfiguration(cxMachineCode, groupName, maxEmbryoCodeCount, maxLhMachineCount, maxLhMachineCount, fixedEmbryoCodeInfo);
            cxMachineConfigurationMap.put(cxMachineCode, configuration);
        });
        if (CollectionUtils.isEmpty(cxMachineConfigurationMap)) {
            return Collections.emptyList();
        }
        return Lists.newArrayList(cxMachineConfigurationMap.values());
    }

    /**
     * 统计使用的硫化机台数
     *
     * @param productionDay     排产日
     * @param productionSkuList 排产Sku信息
     * @param calculator        计算器
     * @param paramMap          参数信息
     * @param dayLimit          日限制信息
     * @return
     */
    private Integer getUsedLhMachines(Integer productionDay, List<FactoryMonthPlanMouldDayResult> productionSkuList, MpMonthPlanDailyCapacityLimit calculator, Map<String, Object> paramMap, MpDailyCapacityLimitVo dayLimit) {
        if (dayLimit == null || null == productionDay || CollectionUtils.isEmpty(productionSkuList)) {
            return null;
        }
        calculator.calcLhMachinesWithEmbryoTypes(productionSkuList, productionDay, dayLimit, paramMap, null, null);
        Integer usedLhMachines = dayLimit.getUsedLhMachines();
        if (null == usedLhMachines || usedLhMachines <= BigDecimal.ZERO.intValue()) {
            return null;
        }
        //单胎胚，没有胎胚种类数，则没有值
        Integer usedEmbryoTypes = dayLimit.getUsedEmbryoTypes();
        if (null == usedEmbryoTypes || usedEmbryoTypes <= BigDecimal.ZERO.intValue()) {
            return null;
        }


        return usedLhMachines;
    }

    /**
     * 转化成按日，各胎胚使用硫化机台数
     *
     * @param groupAllEmbryoUsedMap
     * @return
     */
    private Map<Integer, List<EmbryoUsedLhMachineInfo>> buildDayEmbryoUsedInfo(Map<String, Map<Integer, Integer>> groupAllEmbryoUsedMap) {
        if (CollectionUtils.isEmpty(groupAllEmbryoUsedMap)) {
            return Collections.emptyMap();
        }
        Map<Integer, Map<String, EmbryoUsedLhMachineInfo>> dayEmbryoUsedInfoMap = Maps.newHashMap();
        groupAllEmbryoUsedMap.forEach((embryoCode, dayUsedInfo) -> {
            if (CollectionUtils.isEmpty(dayUsedInfo)) {
                return;
            }
            dayUsedInfo.forEach((day, usedLhMachines) -> {
                Map<String, EmbryoUsedLhMachineInfo> embryoUsedInfo = dayEmbryoUsedInfoMap.get(day);
                if (null == embryoUsedInfo) {
                    embryoUsedInfo = Maps.newHashMap();
                    dayEmbryoUsedInfoMap.put(day, embryoUsedInfo);
                }
                EmbryoUsedLhMachineInfo embryoDayUsedInfo = embryoUsedInfo.get(embryoCode);
                if (null == embryoDayUsedInfo) {
                    embryoDayUsedInfo = new EmbryoUsedLhMachineInfo(embryoCode, usedLhMachines);
                    embryoUsedInfo.put(embryoCode, embryoDayUsedInfo);
                    return;
                }
            });
        });
        if (CollectionUtils.isEmpty(dayEmbryoUsedInfoMap)) {
            return Collections.emptyMap();
        }
        Map<Integer, List<EmbryoUsedLhMachineInfo>> dayEmbryoUsedInfoResult = Maps.newHashMap();
        dayEmbryoUsedInfoMap.forEach((day, embryoUsedInfoMap) -> {
            if (CollectionUtils.isEmpty(embryoUsedInfoMap)) {
                return;
            }
            dayEmbryoUsedInfoResult.put(day, Lists.newArrayList(embryoUsedInfoMap.values()));
        });
        return dayEmbryoUsedInfoResult;
    }
}
