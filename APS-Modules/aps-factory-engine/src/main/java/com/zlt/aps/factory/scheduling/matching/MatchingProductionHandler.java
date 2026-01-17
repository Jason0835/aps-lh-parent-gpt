package com.zlt.aps.factory.scheduling.matching;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.*;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.enums.DayVulcanizationModeEnum;
import com.zlt.aps.factory.enums.MouldRelationTypeEnum;
import com.zlt.aps.factory.enums.ProductionQtyModelEnum;
import com.zlt.aps.factory.handler.CxLhMouldProductionCalculator;
import com.zlt.aps.factory.handler.MouldProductionResultHandler;
import com.zlt.aps.factory.mapper.FactoryMouldingDayResultMapper;
import com.zlt.aps.factory.mapper.MonthPlanRequireMapper;
import com.zlt.aps.factory.mapper.MpStructureAllocationMapper;
import com.zlt.aps.factory.scheduling.BaseDataContainer;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import com.zlt.aps.factory.scheduling.cxcapacity.ProductionCapacityParamConfiguration;
import com.zlt.aps.factory.scheduling.cxcapacity.SkuNeedProductionInfo;
import com.zlt.aps.factory.scheduling.cxcapacity.TbrBeforeProductionGroupLogRecorder;
import com.zlt.aps.factory.scheduling.init.ProductionInitParamConfiguration;
import com.zlt.aps.factory.scheduling.init.TbrProductionInitLogRecorder;
import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import com.zlt.aps.factory.utils.ProductionCycleUtils;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.FactoryParamMapper;
import com.zlt.aps.maindata.utils.FactoryParamUtils;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 搭配排产处理类
 *
 * @author hak
 */
@Slf4j
@Component
public class MatchingProductionHandler {
    @Autowired
    private ProductionSchedulingDataService productionSchedulingDataService;
    @Autowired
    private FactoryMouldingDayResultMapper factoryMouldingDayResultMapper;
    @Autowired
    private MonthPlanRequireMapper monthPlanRequireMapper;
    @Autowired
    private MpStructureAllocationMapper mpStructureAllocationMapper;
    @Autowired
    private FactoryParamMapper factoryParamMapper;
    @Autowired
    private BaseDao baseDao;

    /**
     * 搭配排产（已排产结果入口）
     *
     * @param productionVersion 生产版本
     */
    public void matchingProduction(String productionVersion) {
        // 查询月度计划排产结果
        LambdaQueryWrapper<FactoryMonthPlanMouldDayResult> queryWrapper = new LambdaQueryWrapper<FactoryMonthPlanMouldDayResult>();
        queryWrapper.eq(FactoryMonthPlanMouldDayResult::getProductionVersion, productionVersion);
        List<FactoryMonthPlanMouldDayResult> planList = factoryMouldingDayResultMapper.selectList(queryWrapper);
        this.matchingProduction(planList);
    }

    /**
     * 搭配排产（计划调整入口）
     *
     * @param planList
     */
    public void matchingProduction(List<FactoryMonthPlanMouldDayResult> planList) {
        if (CollectionUtils.isEmpty(planList)) {
            return;
        }
        // 构建上下文等各项参数
        TbrProductionContext productionContext = initProductionContext(planList); // 初始化上下文
        Map<String, MonthPlanProductionRequirePlanVo> requirePlanMap = this.selectRequirePlan(productionContext); // 查询需求计划
        this.buildProductionContext(productionContext, planList, requirePlanMap); // 填充上下文各项必要数据

        List<MonthPlanProductionRequirePlanVo> requirePlanList = new ArrayList<>(requirePlanMap.values()); // 需求计划列表
        List<MonthPlanStructureLhRatioVo> structureLhRatioList = this.getLhRatioConfiguration(productionContext,
                requirePlanList); // 加载结构硫化配比
        productionContext.getBaseDataContainer().setStructureLhRatioList(structureLhRatioList);
        Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap = ProductionPlanGroupInfo
                .statisticsAndEstimateCxAllocationByGroup(productionContext, requirePlanList); // 硫化组
        productionContext.setGroupProductionInfo(estimateGroupCxAllocationMap);
        this.resetBeforeFormalProduction(productionContext, estimateGroupCxAllocationMap);
        Map<String, CxContinueInfoHelper> cxContinueInfoMap = this.getContinueInfo(productionContext);

        // 调用主流程的入口 -> 搭配排程算法
        Map<String, Integer> newSkuQtyMap = this.matchingProduction(productionContext, estimateGroupCxAllocationMap,
                structureLhRatioList, cxContinueInfoMap);

        // 构建排产结果并保存
        this.saveMouldProductionResult(productionContext, planList, newSkuQtyMap);
    }

    /**
     * 搭配排产（主流程入口）
     *
     * @param context                      上下文
     * @param estimateGroupCxAllocationMap 需求计划列表
     * @param structureLhRatioList         成型硫化配比
     * @param cxContinueInfoMap            续作规格
     * @return 本次排产各规格描述的排产数量统计
     */
    public Map<String, Integer> matchingProduction(Context context,
                                                   Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap,
                                                   List<MonthPlanStructureLhRatioVo> structureLhRatioList,
                                                   Map<String, CxContinueInfoHelper> cxContinueInfoMap) {
        log.info(context.getProductionVersion() + "搭配排产start");
        TbrProductionContext productionContext = (TbrProductionContext) context;
        // 按结构分组的模具排产信息
        Map<String, List<CxMouldDayProductionHelper>> mouldProductionGroup = buildMouldProductionGroup(
                productionContext);
//		// 按结构分组的成型排产信息
//		Map<String, List<CxMachineAllocationPlanHelper>> cxAllocationPlanGroup = buildCxAllocationPlanGroup(
//				productionContext);
        // 机构模具配比
        Map<String, MonthPlanStructureLhRatioVo> structureLhRatioMap = structureLhRatioList.stream().collect(
                Collectors.toMap(MonthPlanStructureLhRatioVo::getStructureName, Function.identity(), (r1, r2) -> r1));
        Map<Long, MonthPlanProductionRequirePlanVo> allSinglePlanMap = productionContext.getAllProductionPlan(); // 需求计划
        Map<String, Integer> newSkuQtyMap = new HashMap<>(); // 本次排产各规格的排产数量统计
        // 遍历所有结构
        for (Entry<String, ProductionPlanGroupInfo> entry : estimateGroupCxAllocationMap.entrySet()) {
            String structureName = entry.getKey(); // 分组名称（TBR：结构）
            ProductionPlanGroupInfo groupInfo = entry.getValue();
            CxContinueInfoHelper continueInfo = cxContinueInfoMap.get(structureName); // 续作规格
            MonthPlanStructureLhRatioVo ratioVo = structureLhRatioMap.get(structureName); // 成型硫化配比
            List<CxMouldDayProductionHelper> mouldDayProductionList = mouldProductionGroup.get(structureName);
            // 处理需求计划
            List<MonthPlanProductionRequirePlanVo> productionPlanList = groupInfo.getGroupPlanData();
            productionPlanList.forEach(plan -> plan.setProductionQty(
                    BigDecimalUtils.add(plan.getProductionQty(), plan.getConventionReserveQty()).intValue())); // 待排产量要加上常规储备
            if (CollectionUtils.isEmpty(mouldDayProductionList)) {
                continue; // 成型或模具排程任意一个找不到数据都要跳过这个结构
            }

            // 取出最早成型硫化配比不足的日期
            // 本结构按天汇总的日排产量，计算产量限制以及模具限制，需要按key(日期)排序
            MatchingProductDayHelper limitHelper = this.caculateProductDay(groupInfo, ratioVo, mouldDayProductionList,
                    allSinglePlanMap); // 计算排产日
            Integer startDay = limitHelper.getStartDay();
            Integer endDay = limitHelper.getEndDay();
            if (startDay.compareTo(endDay) > 0) { // 如果开始时间>结束时间，说明该结构满产，直接看下一个结构
                continue;
            }

            // 循环取结构向下所有符合搭配生产条件的sku进行搭配排产
            Set<String> scheduleMaterialDesc = new HashSet<>(); // 记录已排规格，防止重复执行死循环
            do {
                // 获取优先级最高的Sku信息
                String materialDesc = this.getSelectedAddSku(productionContext, startDay, endDay, productionPlanList);
                if (StringUtils.isBlank(materialDesc) || scheduleMaterialDesc.contains(materialDesc)) {
                    break;
                }
                scheduleMaterialDesc.add(materialDesc);
                // 计算需要排产的量
                SkuNeedProductionInfo needProductionInfo = this.getNeedProductionQty(productionPlanList, materialDesc);
                if (null == needProductionInfo) {
                    break;
                }
                // 执行搭配排产算法
                this.matchingSchedule(productionContext, materialDesc, needProductionInfo, newSkuQtyMap, groupInfo,
                        continueInfo, startDay, endDay);
            } while (true);
        }
        log.info(context.getProductionVersion() + "搭配排产end");
        return newSkuQtyMap;
    }

    /**
     * 计算生产日期
     *
     * @param structureName          结构名称
     * @param groupInfo              排程分组对象
     * @param structureLhRatioMap    成型硫化配比
     * @param mouldDayProductionList
     * @param allSinglePlanMap
     * @return
     */
    private MatchingProductDayHelper caculateProductDay(ProductionPlanGroupInfo groupInfo,
                                                        MonthPlanStructureLhRatioVo ratioVo, List<CxMouldDayProductionHelper> mouldDayProductionList,
                                                        Map<Long, MonthPlanProductionRequirePlanVo> allSinglePlanMap) {
        Integer lhMouldQty = ProductionConstant.DOUBLE_MOULD_PRODUCTION; // 硫化机模具配比
        // 按天分组硫化排产
        Map<Integer, List<CxMouldDayProductionHelper>> dayModPlanMap = mouldDayProductionList.stream()
                .collect(Collectors.groupingBy(CxMouldDayProductionHelper::getProductionDate));
        // 按天分组成型排产
//					Map<Integer, List<CxMachineAllocationPlanHelper>> dayCxPlanMap = cxAllocationPlanList.stream().collect(Collectors.groupingBy(CxMachineAllocationPlanHelper::getAllocationDay));;

        // 根据sku\模具等因素取定额数据
//		Integer quota = Optional.ofNullable(requirePlan.getDayVulcanizationQty()).orElse(0); // 单模硫化产能
        // 取出成型机台号
        Integer cxNum = Optional.ofNullable(groupInfo.getAllocationCxMachineCodeSet()).map(Set::size).orElse(0);
        Integer rate = Optional.ofNullable(ratioVo).map(MonthPlanStructureLhRatioVo::getLhMachineMaxQty).orElse(8);// 取出成型配比，默认是8
        Integer lhNum = BigDecimalUtils.multiply(cxNum, rate, true).intValue(); // 最大硫化机数
        Integer maxMouldNum = lhNum * lhMouldQty; // 换算成模具数 = 硫化机* 2（双模排产）
        // 统计每一天的已排产量
        TreeMap<Integer, MatchingPlanLimitHelper> dayPlanMap = new TreeMap<>();
        for (Entry<Integer, List<CxMouldDayProductionHelper>> modPlan : dayModPlanMap.entrySet()) {
            Integer day = modPlan.getKey();
            List<CxMouldDayProductionHelper> planList = modPlan.getValue();
            // 取出单模具产能
            Integer dayVulcanizationQty = allSinglePlanMap.get(CollectionUtils.firstElement(planList).getMonthPlanId())
                    .getDayVulcanizationQty();

            MatchingPlanLimitHelper dayLimit = dayPlanMap.get(day);
            if (dayLimit == null) {
                dayLimit = new MatchingPlanLimitHelper();
                dayPlanMap.put(day, dayLimit);
                dayLimit.setMaxPlanQty(maxMouldNum * dayVulcanizationQty);
                dayLimit.setMaxMouldQty(maxMouldNum);
            }
            Integer productionQty = planList.stream().mapToInt(CxMouldDayProductionHelper::getProductionQty).sum();
            Integer planQty = Optional.ofNullable(dayLimit.getPlanQty()).orElse(0) + productionQty;
            dayLimit.setPlanQty(planQty);
            Integer mouldQty = (int) Math.ceil(planQty / dayVulcanizationQty); // 模具数 = 计划量 / 单模硫化能力
            mouldQty = (int) Math.ceil(mouldQty / lhMouldQty) * lhMouldQty; // 向上取最接近的双模数量
            dayLimit.setMouldQty(mouldQty);
        }
        // 计算可搭配排产时间段
        Integer startDay = dayPlanMap.firstKey(); // 搭配起始日期，初始是结构第一天上机日期
        for (Entry<Integer, MatchingPlanLimitHelper> dayPlanEntry : dayPlanMap.entrySet()) {
            startDay = dayPlanEntry.getKey();
            MatchingPlanLimitHelper dayLimit = dayPlanEntry.getValue();
            // 判断满足以下所有条件，说明有空余，则从当天开始尝试搭配排产
            // 1、计划小于定额
            // 2、模具小于最大可排模具数
            if (dayLimit.isProduct()) {
                break;
            }
        }
        Integer endDay = dayPlanMap.lastKey(); // 结束日期，默认是结构收尾日期
        // 从开始日期到结束日期，检查每一天是否满足配上机的约束条件
        for (int day = startDay; day <= endDay; day++) {
            // 只要有一天不满足条件，直接将结束日期提前到上一天
            MatchingPlanLimitHelper dayLimit = dayPlanMap.get(day);
            if (dayLimit != null && !dayLimit.isProduct()) { // 当天有排产，且不满足生产要求时触发调整
                endDay = day - 1;
                break;
            }
        }
        return new MatchingProductDayHelper(startDay, endDay);
    }

    /**
     * 执行搭配排产算法
     *
     * @param productionContext  上下文
     * @param materialDesc       结构
     * @param needProductionInfo 需排产物料
     * @param newSkuQtyMap       已搭配排产规格数量
     * @param groupInfo          排产计划分组
     * @param continueInfo       首日续作规格
     * @param startDay           排产开始日期
     * @param endDay             排产截至日期
     */
    private void matchingSchedule(TbrProductionContext productionContext, String materialDesc,
                                  SkuNeedProductionInfo needProductionInfo, Map<String, Integer> newSkuQtyMap,
                                  ProductionPlanGroupInfo groupInfo, CxContinueInfoHelper continueInfo, Integer startDay, Integer endDay) {
        List<MonthPlanProductionRequirePlanVo> productionPlanList = groupInfo.getGroupPlanData(); // 本分对应需求计划
        Integer productionQty = needProductionInfo.getSumNeedProductionQty(); // 需求量
        Integer maxProductionQty = needProductionInfo.getDayMaxProductionQty(); // 单机台硫化上限
        List<MatchingMouldDayUsedHelper> mouldDayUsedList = this.caculateMouldDayUsed(productionContext, materialDesc,
                maxProductionQty, startDay, endDay); // 统计每一天所有可用模具
        for (MatchingMouldDayUsedHelper mouldDayUsed : mouldDayUsedList) { // 遍历各模具可用列表
            Integer usedBeginDate = mouldDayUsed.getBeginDate();
            Integer usedEndDate = mouldDayUsed.getEndDate();
            List<ProductionMouldInfoVo> doubleMouldList = mouldDayUsed.getMouldInfoList();

            // 判断是否续作
            List<ProductionMouldInfoVo> newDoubleMouldList = new ArrayList<>(); // 新上模具
            if (usedBeginDate == 1) { // 第一天，判断是否首日续作
                boolean isContinue = this.matchingScheduleFirstDayContinue(productionContext, materialDesc,
                        newSkuQtyMap, groupInfo, continueInfo, productionQty, maxProductionQty, usedBeginDate,
                        usedEndDate, doubleMouldList);
                if (!isContinue) {
                    newDoubleMouldList = doubleMouldList; // 非续作，则都标记为新上模具
                }
            } else { // 非第一天
                List<ProductionMouldInfoVo> continueMouldList = new ArrayList<>(); // 续作模具
                for (ProductionMouldInfoVo mouldInfo : doubleMouldList) {
                    // 判断切换计划前上一天的排产计划
                    List<CxMouldDayProductionHelper> lastDayProductionList = mouldInfo.getDayProductionInfo()
                            .get(usedBeginDate - 1);
                    if (!CollectionUtils.isEmpty(lastDayProductionList)
                            && lastDayProductionList.stream().anyMatch(p -> materialDesc.equals(p.getMaterialDesc()))) { // 上一天有排产，且物料描述一致，说明是续作
                        continueMouldList.add(mouldInfo); // 符合条件的放到续作列表
                    } else {
                        newDoubleMouldList.add(mouldInfo); // 不符合条件的放到新增列表
                    }
                }
                // 执行非首日续作排程算法
                this.matchingScheduleNextDayContinue(productionContext, materialDesc, newSkuQtyMap, groupInfo,
                        productionPlanList, productionQty, maxProductionQty, usedBeginDate, usedEndDate,
                        continueMouldList);
            }

            // 新模排产
            if (!CollectionUtils.isEmpty(newDoubleMouldList)) {
                // 排产量
                Integer sumProductionQty = productionQty;
                Integer dayMaxProductionQty = needProductionInfo.getDayMaxProductionQty();
                Integer realSumProductionQty = newSkuQtyMap.getOrDefault(materialDesc, 0); // 已排产量
                Set<String> cxMachineInfoSet = groupInfo.getAllocationCxMachineCodeSet();
                LhProductionQtyHelper lhProductionQtyHelper = new LhProductionQtyHelper(groupInfo, cxMachineInfoSet,
                        null, sumProductionQty, realSumProductionQty, dayMaxProductionQty);
                CxLhMouldProductionCalculator.lhProductionByGroupHandler(productionContext, lhProductionQtyHelper,
                        usedBeginDate, usedEndDate, newDoubleMouldList, needProductionInfo.getNeedProductionList());
                newSkuQtyMap.put(materialDesc, lhProductionQtyHelper.getRealSumProductionQty()); // 累计已排量
            }
        }
    }

    /**
     * 非首日续作排程算法
     *
     * @param productionContext
     * @param materialDesc
     * @param newSkuQtyMap
     * @param groupInfo
     * @param productionPlanList
     * @param productionQty
     * @param maxProductionQty
     * @param lhMouldQty
     * @param beginDate
     * @param endDate
     * @param continueMouldList
     */
    private void matchingScheduleNextDayContinue(TbrProductionContext productionContext, String materialDesc,
                                                 Map<String, Integer> newSkuQtyMap, ProductionPlanGroupInfo groupInfo,
                                                 List<MonthPlanProductionRequirePlanVo> productionPlanList, Integer productionQty, Integer maxProductionQty,
                                                 Integer beginDate, Integer endDate, List<ProductionMouldInfoVo> continueMouldList) {
        // 续作规格排产
        if (CollectionUtils.isEmpty(continueMouldList)) {
            return;
        }
        Integer sumProductionQty = productionQty; // 待排量 = 需求量
        // 构建续作规格对象
        CxContinueSkuInfoHelper continueSkuInfo = CxContinueSkuInfoHelper.buildContinueProductInfo(materialDesc,
                productionPlanList, new HashMap<>());
        continueSkuInfo.setOnLineCxMachineSet(groupInfo.getAllocationCxMachineCodeSet());
        continueSkuInfo.setDayVulcanizationQty(maxProductionQty);
        continueSkuInfo.setContinueSkuPlanList(productionPlanList);
        for (int productionDay = beginDate; productionDay <= endDate; productionDay++) { // 按顺序每天续作排产
            // 检查当天模具是否有排产，如果有则需要从模具已排量补到上限
            Integer checkDay = productionDay;
            Map<Integer, List<ProductionMouldInfoVo>> capacityMap = this.getMoldRemainingCapacity(continueMouldList,
                    maxProductionQty, checkDay); // 计算所有模具当天的产能剩余量
            for (Entry<Integer, List<ProductionMouldInfoVo>> entrySet : capacityMap.entrySet()) {
                Integer capacityQty = entrySet.getKey(); // 产能剩余量
                List<ProductionMouldInfoVo> remainMouldList = entrySet.getValue(); // 相同剩余产能的模具
                // 模具数换算成机台数
                Integer lhQty = remainMouldList.size() / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
                Integer realProductionQty = NumberUtils.min(sumProductionQty, capacityQty, maxProductionQty); // 取计划量、产能剩余量、最大排产量的最小值
                if (realProductionQty <= 0) {
                    continue;
                }
                CxLhMouldProductionCalculator.continueSkuLhProductionHandler(productionContext, groupInfo,
                        continueSkuInfo, productionDay, realProductionQty, remainMouldList);
                realProductionQty *= lhQty; // 合计计划量时，要乘上硫化机数
                newSkuQtyMap.put(materialDesc, newSkuQtyMap.getOrDefault(materialDesc, 0) + realProductionQty); // 累计已排量
                sumProductionQty -= realProductionQty; // 待排量扣减已排量，要乘上硫化机数
            }
        }
    }

    /**
     * 首日续作排程算法
     *
     * @param productionContext
     * @param materialDesc
     * @param newSkuQtyMap
     * @param groupInfo
     * @param continueInfo
     * @param productionQty
     * @param maxProductionQty
     * @param beginDay
     * @param endDay
     * @param doubleMouldList
     * @return
     */
    private boolean matchingScheduleFirstDayContinue(TbrProductionContext productionContext, String materialDesc,
                                                     Map<String, Integer> newSkuQtyMap, ProductionPlanGroupInfo groupInfo, CxContinueInfoHelper continueInfo,
                                                     Integer productionQty, Integer maxProductionQty, Integer beginDay, Integer endDay,
                                                     List<ProductionMouldInfoVo> doubleMouldList) {
        if (continueInfo == null) {
            return false;
        }
        CxContinueSkuInfoHelper continueSkuInfo = continueInfo.getContinueSkuMouldNumberMap().get(materialDesc);
        if (continueSkuInfo == null || CollectionUtils.isEmpty(continueSkuInfo.getContinueSkuPlanList())) {
            return false;
        }
        Integer sumProductionQty = productionQty; // 待排量 = 需求量
        Integer lhQty = doubleMouldList.size() / ProductionConstant.DOUBLE_MOULD_PRODUCTION; // 模具数换算成机台数
        // 顺序遍历每一个排产日
        for (int productionDay = beginDay; productionDay <= endDay; productionDay++) { // 按顺序每天续作排产
            // 检查当天模具是否有排产，如果有则需要从模具已排量补到上限
            Integer checkDay = productionDay;
            Map<Integer, List<ProductionMouldInfoVo>> capacityMap = this.getMoldRemainingCapacity(doubleMouldList,
                    maxProductionQty, checkDay); // 计算所有模具当天的产能剩余量
            for (Entry<Integer, List<ProductionMouldInfoVo>> entrySet : capacityMap.entrySet()) {
                Integer capacityQty = entrySet.getKey(); // 产能剩余量
                Integer realProductionQty = NumberUtils.min(sumProductionQty, capacityQty, maxProductionQty); // 取计划量、产能剩余量、最大排产量的最小值
                if (realProductionQty <= 0) {
                    continue;
                }
                CxLhMouldProductionCalculator.continueSkuLhProductionHandler(productionContext, groupInfo,
                        continueSkuInfo, productionDay, realProductionQty, doubleMouldList);
                realProductionQty *= lhQty; // 合计计划量时，要乘上硫化机数
                newSkuQtyMap.put(materialDesc, newSkuQtyMap.getOrDefault(materialDesc, 0) + realProductionQty); // 累计已排量
                sumProductionQty -= realProductionQty; // 待排量扣减已排量
            }
        }
        return true;
    }

    /**
     * 计算所有模具当天的产能剩余量
     *
     * @param mouldList           模具列表
     * @param dayVulcanizationQty 最大生产上限
     * @param checkDay            检查日
     * @return key = 剩余产能，value = 相同剩余产能的模具列表
     */
    private Map<Integer, List<ProductionMouldInfoVo>> getMoldRemainingCapacity(List<ProductionMouldInfoVo> mouldList,
                                                                               Integer dayVulcanizationQty, Integer checkDay) {
        Integer lhMouldQty = ProductionConstant.DOUBLE_MOULD_PRODUCTION; // 硫化机模具配比
        Integer dayMoldQty = dayVulcanizationQty / lhMouldQty; // 换算成单模日产能
        Map<Integer, List<ProductionMouldInfoVo>> unProdQtyMap = mouldList.stream().collect(Collectors.groupingBy(m -> {
            List<CxMouldDayProductionHelper> productionList = m.getDayProductionInfo().get(checkDay);
            if (!CollectionUtils.isEmpty(productionList)) { // 有已排量，需要扣减掉计算除剩余的量
                Integer usedQty = productionList.stream().mapToInt(CxMouldDayProductionHelper::getProductionQty).sum();
                Integer remainingQty = dayMoldQty - usedQty; // 剩余量
                return (remainingQty > 0 ? remainingQty : 0) * lhMouldQty; // 换算回硫化机日产能
            }
            return dayMoldQty * lhMouldQty; // 换算回硫化机日产能
        }));
        return unProdQtyMap;
    }

    /**
     * 分别计算每一天指定物料所有可用模具
     *
     * @param productionContext   上下文
     * @param materialDesc        检查物料描述
     * @param dayVulcanizationQty 单硫化机产能
     * @param startDay            开始时间
     * @param endDay              结束时间
     * @return
     */
    private List<MatchingMouldDayUsedHelper> caculateMouldDayUsed(TbrProductionContext productionContext,
                                                                  String materialDesc, Integer dayVulcanizationQty, Integer startDay, Integer endDay) {
        List<MatchingMouldDayUsedHelper> mouldDayUsedList = new ArrayList<>();
        Map<Integer, List<ProductionMouldInfoVo>> canUseMouldMap = new TreeMap<>();
        Integer dayMoldQty = dayVulcanizationQty / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        for (int day = startDay; day <= endDay; day++) {
            // 选择模具
            canUseMouldMap.put(day, this.selectedAllMouldByDay(productionContext, materialDesc, dayMoldQty, day));
        }
        // 遍历每一天的可用模具，与前一天可用模具相同的日期分作一组，然后按组遍历排产
        String currentMouldCode = "";
        Integer currentStartDay = startDay;
        Integer currentEndDay = startDay;
        for (int day = startDay; day <= endDay; day++) {
            List<ProductionMouldInfoVo> canUseMould = canUseMouldMap.get(day);
            String canUseMouldCode = "";
            if (!CollectionUtils.isEmpty(canUseMould)) {
                canUseMouldCode = canUseMould.stream().map(ProductionMouldInfoVo::getMouldCode).sorted()
                        .collect(Collectors.joining(","));
            } else {
                canUseMouldCode = ""; // 没有可用模具，置为空
            }
            if (day == endDay) { // 最后一天，添加可用时间段后直接结束
                mouldDayUsedList.add(new MatchingMouldDayUsedHelper(canUseMould, currentStartDay, day)); // 记录可用时间段
                break;
            }
            if (!StringUtils.isEmpty(currentMouldCode) && !canUseMouldCode.equals(currentMouldCode)) { // 模具号有变化，且原模具号非空，则记录上一个可用时间段
                List<ProductionMouldInfoVo> latestCanUseMould = canUseMouldMap.get(day - 1);
                mouldDayUsedList.add(new MatchingMouldDayUsedHelper(latestCanUseMould, currentStartDay, currentEndDay)); // 记录可用时间段
                currentStartDay = day; // 更新下一组的起始时间
            }
            currentMouldCode = canUseMouldCode;
            currentEndDay = day;
        }
        return mouldDayUsedList;
    }

    /**
     * 构建排产结果并保存
     *
     * @param productionContext
     * @param planList
     * @param newSkuQtyMap
     */
    private void saveMouldProductionResult(TbrProductionContext productionContext,
                                           List<FactoryMonthPlanMouldDayResult> planList, Map<String, Integer> newSkuQtyMap) {
        if (CollectionUtils.isEmpty(newSkuQtyMap)) {
            return;
        }
        // 从上下文取出排产结果
        List<FactoryMonthPlanMouldDayDetail> detailLogList = MouldProductionResultHandler
                .getMouldProductionResult(productionContext).stream()
                .filter(detail -> newSkuQtyMap.containsKey(detail.getMaterialDesc())).collect(Collectors.toList()); // 只过滤出本次排产的规格
        List<FactoryMonthPlanMouldDayResult> dayResultList = MouldProductionResultHandler
                .getSummaryBySkuResult(detailLogList, productionContext);
        if (CollectionUtils.isEmpty(dayResultList)) {
            return;
        }
        Map<String, FactoryMonthPlanMouldDayResult> oldPlanMap = planList.stream()
                .collect(Collectors.toMap(FactoryMonthPlanMouldDayResult::getMaterialCode, Function.identity()));
        dayResultList.stream().filter(plan -> oldPlanMap.containsKey(plan.getMaterialCode())).forEach(plan -> {
            FactoryMonthPlanMouldDayResult oldPlan = oldPlanMap.get(plan.getMaterialCode());
            if (oldPlan != null) {
                plan.setConventionProductionQty(newSkuQtyMap.get(plan.getMaterialDesc()));
                plan.setTotalQty(oldPlan.getTotalQty() + plan.getConventionProductionQty());
                plan.setDifferenceQty(oldPlan.getDifferenceQty());
                plan.setMouldCavityQty(oldPlan.getMouldCavityQty());
                plan.setTypeBlockQty(oldPlan.getTypeBlockQty());
                plan.setFactProdReqQty(oldPlan.getFactProdReqQty());
                plan.setId(oldPlan.getId());
                plan.setBaseVale(plan.getId());
            }

            if (null != plan.getInventorySalesRatio()
                    && plan.getInventorySalesRatio().compareTo(BigDecimal.ZERO) < BigDecimal.ZERO.intValue()) {
                plan.setInventorySalesRatio(BigDecimal.ZERO);
            }
        });
        baseDao.saveBatch(dayResultList);
    }

    /**
     * 获取计划对应结构成型硫化配比信息 计划内的结构
     *
     * @param context         排产上下文
     * @param requirePlanList 需求计划信息
     * @return
     */
    private List<MonthPlanStructureLhRatioVo> getLhRatioConfiguration(Context context,
                                                                      List<MonthPlanProductionRequirePlanVo> requirePlanList) {
        if (CollectionUtils.isEmpty(requirePlanList)) {
            return Collections.emptyList();
        }
        // 提取结构查询条件
        Set<String> structureNameMap = requirePlanList.stream().map(MonthPlanProductionRequirePlanVo::getStructureName)
                .collect(Collectors.toSet());
        List<String> structureNameList = new ArrayList<>(structureNameMap);
        List<MonthPlanStructureLhRatioVo> structureLhRatioList = this.getDataService().getLhRatioInfo(context,
                structureNameList);
        if (CollectionUtils.isEmpty(structureLhRatioList)) {
            return Collections.emptyList();
        }
        // 机型为空值，表示所有机型匹配
        structureLhRatioList.forEach(singleRatio -> {
            if (StringUtils.isNotBlank(singleRatio.getCxMachineTypeCode())) {
                return;
            }
            singleRatio.setCxMachineTypeCode(ProductionConstant.ALL_BRAND_CODE_MATCH);
        });
        // 周期结构硫化配比
        List<CycleStructureMinLhMachineQtyVo> cycleStructureMinLhRatioList = getDataService()
                .getCycleLhRatioInfo(context);
        Map<String, Integer> cycleStructureMinLhRatioMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(cycleStructureMinLhRatioList)) {
            cycleStructureMinLhRatioList.forEach(cycleStructureMinLhRatio -> {
                cycleStructureMinLhRatioMap.put(cycleStructureMinLhRatio.getStructureName(),
                        null == cycleStructureMinLhRatio.getMonthMinLhMachineQty()
                                ? cycleStructureMinLhRatio.getMinLhMachineQty()
                                : cycleStructureMinLhRatio.getMonthMinLhMachineQty());
            });
        }
        // 常规结构的最低硫化配比
        Integer defaultMinLhRatio = ((TbrProductionContext) context).getBaseDataContainer().getParamConfiguration()
                .getNoCycleProductionMinLhMachineNumber();
        structureLhRatioList.forEach(structureLhRatio -> {
            String structureName = structureLhRatio.getStructureName();
            structureLhRatio.setLhMachineMinQty(defaultMinLhRatio);
            // 如果是周期，则换成周期
            if (cycleStructureMinLhRatioMap.containsKey(structureName)) {
                structureLhRatio.setLhMachineMinQty(cycleStructureMinLhRatioMap.get(structureName));
                return;
            }
        });
        return structureLhRatioList;
    }

    /**
     * 加载需求计划列表(合并后)
     *
     * @param productionVersion 月计划生产版本
     * @return
     */
    private Map<String, MonthPlanProductionRequirePlanVo> selectRequirePlan(TbrProductionContext productionContext) {
//		List<MonthPlanProductionRequirePlanVo> requirePlanList = productionSchedulingDataService
//				.getFactoryMonthPlanManufacturing(productionContext);
//		if (!CollectionUtils.isEmpty(requirePlanList)) {
//			return requirePlanList.stream()
//					.collect(Collectors.toMap(MonthPlanProductionRequirePlanVo::getMaterialCode, Function.identity()));
//		}
        Map<String, MonthPlanProductionRequirePlanVo> requirePlanMap = new HashMap<>();
        String productionVersion = productionContext.getProductionVersion();
        String monthPlanVersion = productionContext.getMonthPlanVersion();

        // 加载需求计划
        LambdaQueryWrapper<DpDemandPlan> DemandQueryWrapper = new LambdaQueryWrapper<DpDemandPlan>();
        DemandQueryWrapper.eq(DpDemandPlan::getMonthPlanVersion, monthPlanVersion);
        List<DpDemandPlan> demandPlanList = monthPlanRequireMapper.selectList(DemandQueryWrapper);
        if (CollectionUtils.isEmpty(demandPlanList)) {
            return requirePlanMap;
        }
        ProductionInitParamConfiguration paramConfiguration = createInitParamConfiguration(productionContext);
        Map<String, MonthPlanProductLhCapacityVo> lhCapacityMap = getProductLhCapacityInfo(productionContext,
                paramConfiguration.getDayVulcanizationQtyConfiguration());

        // 需求计划需要按物料号合并各需求量
        for (DpDemandPlan demandPlan : demandPlanList) {
            String materialCode = demandPlan.getMaterialCode();
            MonthPlanProductionRequirePlanVo requirePlan = requirePlanMap.get(materialCode);
            if (requirePlan == null) {// 不存在直接转换
                requirePlan = MonthPlanProductionRequirePlanVo.buildInitProductionPlan(null, productionVersion,
                        demandPlan);
                requirePlan.setHeightLossQty(demandPlan.getMidQty());
                requirePlan.setFactProdReqQty(demandPlan.getNetQty());
                requirePlan.setVulcanizationInfo(lhCapacityMap.get(demandPlan.getMaterialDesc())); // 设置硫化信息
                requirePlan.setInventorySalesRatio(0D);// 默认0
                requirePlanMap.put(materialCode, requirePlan);
                continue;
            }
            // 已存在则合并各项数值
            requirePlan.setNetQty(BigDecimalUtils.add(requirePlan.getNetQty(), demandPlan.getNetQty()).intValue());
            requirePlan.setPostponeNetQty(
                    BigDecimalUtils.add(requirePlan.getPostponeNetQty(), demandPlan.getPostponeNetQty()).intValue());
            requirePlan.setUnPostponeNetQty(BigDecimalUtils
                    .add(requirePlan.getUnPostponeNetQty(), demandPlan.getUnPostponeNetQty()).intValue());
            requirePlan.setHeightQty(
                    BigDecimalUtils.add(requirePlan.getHeightQty(), demandPlan.getHeightQty()).intValue());
            requirePlan.setHeightLossQty(
                    BigDecimalUtils.add(requirePlan.getHeightLossQty(), demandPlan.getHeightQty()).intValue());
            requirePlan.setMidQty(BigDecimalUtils.add(requirePlan.getMidQty(), demandPlan.getMidQty()).intValue());
            requirePlan.setPostponeQty(
                    BigDecimalUtils.add(requirePlan.getPostponeQty(), demandPlan.getPostponeQty()).intValue());
            requirePlan.setCycleReserveQty(
                    BigDecimalUtils.add(requirePlan.getCycleReserveQty(), demandPlan.getCycleReserveQty()).intValue());
            requirePlan.setConventionReserveQty(BigDecimalUtils
                    .add(requirePlan.getConventionReserveQty(), demandPlan.getConventionReserveQty()).intValue());
            requirePlan.setFactProdReqQty(requirePlan.getNetQty()); // 净需求
        }
        return requirePlanMap;
    }

    /**
     * 将定稿计划和需求计划构建成算法要求的上下文结构
     *
     * @param planList       定稿计划
     * @param demandPlanList 需求计划
     * @return
     */
    private TbrProductionContext initProductionContext(List<FactoryMonthPlanMouldDayResult> planList) {
        // 构建上下文对象
        TbrProductionContext productionContext = new TbrProductionContext();
        FactoryMonthPlanMouldDayResult result = CollectionUtils.firstElement(planList);
        productionContext.setProductionVersion(result.getProductionVersion()); // 生产版本号
        productionContext.setMonthPlanVersion(result.getMonthPlanVersion()); // 月需求计划版本
        productionContext.setYear(result.getYear());
        productionContext.setMonth(result.getMonth());
        productionContext.setFactoryCode(result.getFactoryCode());
        productionContext.setProductType(ProductTypeEnum.getEnumByValue(result.getProductTypeCode()));
        productionContext.setLogBuilder(new StringBuilder());
        productionContext.setBaseDataContainer(new BaseDataContainer());

        this.setProductionCycleInfo(productionContext); // 设置生产周期
        this.setMonthProductionDays(productionContext); // 设置生产日
        this.buildGroupMainPatternInfo(productionContext);

        return productionContext;
    }

    /**
     * 将定稿计划和需求计划构建成算法要求的上下文结构
     *
     * @param planList       定稿计划
     * @param demandPlanList 需求计划
     * @return
     */
    private TbrProductionContext buildProductionContext(TbrProductionContext productionContext,
                                                        List<FactoryMonthPlanMouldDayResult> planList,
                                                        Map<String, MonthPlanProductionRequirePlanVo> requirePlanMap) {
        // 构建各项排产过程数据
        BaseDataContainer container = productionContext.getBaseDataContainer();
        container.setSkuMouldRelationMap(this.getProductionMouldInfo(productionContext)); // 模具施工关系
        container.setParamConfiguration(this.createParamConfiguration(productionContext)); // 排程参数
        container.setMouldInfoMap(this.buildMouldInfoMap(productionContext, planList, requirePlanMap)); // 已排模具计划（净需求）
        container.setCxMachineBaseInfo(this.getDataService().getCxMachineBaseInfo(productionContext)); // 已排结构排程
        this.overSixMonthStockHandler(productionContext); // 超6个成品库存信息

        // 各项已排产统计数据
        productionContext.setAllProductionPlan(requirePlanMap.values().stream()
                .collect(Collectors.toMap(MonthPlanProductionRequirePlanVo::getMonthPlanId, Function.identity())));
        productionContext.setAllSkuProductionPlan(requirePlanMap.values().stream()
                .collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc)));
        productionContext.setSkuPlannedQtyMap(planList.stream()
                .collect(Collectors.groupingBy(FactoryMonthPlanMouldDayResult::getStructureName,
                        Collectors.collectingAndThen(Collectors.toList(),
                                list -> list.stream().mapToInt(FactoryMonthPlanMouldDayResult::getTotalQty).sum())))); // 各sku已排产量
        productionContext.setSkuWastageQtyMap(new HashMap<>());

        return productionContext;
    }

    /**
     * 构建模具排产集合
     *
     * @param productionContext
     * @param planList
     * @param requirePlanMap
     * @return
     */
    private Map<String, ProductionMouldInfoVo> buildMouldInfoMap(TbrProductionContext productionContext,
                                                                 List<FactoryMonthPlanMouldDayResult> planList,
                                                                 Map<String, MonthPlanProductionRequirePlanVo> requirePlanMap) {
        Integer lhMouldQty = ProductionConstant.DOUBLE_MOULD_PRODUCTION; // 硫化机模具配比
        Map<String, List<MonthPlanProductMouldInfoVo>> skuMouldRelationMap = productionContext.getBaseDataContainer()
                .getSkuMouldRelationMap(); // 模具sku关系，key=物料描述
        // 构建模具排产数据
        Map<String, ProductionMouldInfoVo> mouldInfoMap = new HashMap<>();
        for (FactoryMonthPlanMouldDayResult plan : planList) {
            // 根据物料取出需求计划
            MonthPlanProductionRequirePlanVo requirePlan = requirePlanMap.get(plan.getMaterialCode());
            if (requirePlan == null) {
                continue;
            }
            List<MonthPlanProductMouldInfoVo> mouldList = skuMouldRelationMap.get(plan.getMaterialDesc());
            if (CollectionUtils.isEmpty(mouldList)) {
                continue;
            }
            // 将相关模具全部添加到模具排产数据中
            for (MonthPlanProductMouldInfoVo mould : mouldList) {
                String mouldCode = mould.getMouldCode();
                ProductionMouldInfoVo productionMouldInfo = mouldInfoMap.get(mouldCode);
                if (productionMouldInfo == null) {
                    MouldRelationTypeEnum relationType = MouldRelationTypeEnum.getInstance(mould.getRelationType());
                    productionMouldInfo = ProductionMouldInfoVo.createEmptyProductionMouldInfo(mould);
                    productionMouldInfo.setProductionDayInfo(productionContext,
                            productionContext.getProductionStartDate()); // 上机时间
                    mouldInfoMap.put(mouldCode, productionMouldInfo);
                }
            }
            Integer beginDay = plan.getBeginDay(); // 排产开始日
            Integer endDay = plan.getEndDay(); // 排产结束日
            // 按天统计排产量，并按产能分配到各个模具上
            Map<Integer, Integer> dayPlanQtyMap = new HashMap<>();
            for (int day = beginDay; day <= endDay; day++) {
                Integer productionQty = (Integer) plan.getFieldValueByFieldName("day" + day); // 根据日期取对应日计划量
                dayPlanQtyMap.put(day, Optional.ofNullable(productionQty).orElse(0)); // 记录当天日计划量
            }
            // 计算所需模具数 = max(每天计划量 / 单模硫化产能)
            Integer dayVulcanizationQty = Optional.ofNullable(requirePlan.getDayVulcanizationQty()).orElse(0); // 获取单模硫化产能
            if (dayVulcanizationQty <= 0) { // 产能不足直接结束
                continue;
            }
            Integer dayPlanQty = dayPlanQtyMap.values().stream().max(Integer::compareTo).orElse(0);
            Integer mouldQty = (int) Math.ceil(dayPlanQty / dayVulcanizationQty); // 模具数
            mouldQty = (int) Math.ceil(mouldQty / lhMouldQty) * lhMouldQty; // 向上取最接近的双模数量
            Integer lhQty = mouldQty / lhMouldQty; // 换算成硫化机台数
            if (mouldQty <= 0) {
                continue;
            }
            Set<String> cxMachineCodeInfo; // 成型机台
            if (StringUtils.isEmpty(plan.getCxMachineCode())) {
                cxMachineCodeInfo = new HashSet<>();
            } else {
                cxMachineCodeInfo = new HashSet<>(Arrays.asList(StringUtils.split(plan.getCxMachineCode(), ","))); // 成型机台
            }

            for (int i = 0; i < lhQty; i++) { // 给每个硫化机安排模具排产
                List<ProductionMouldInfoVo> effectiveList = this.getEffectiveByRange(mouldInfoMap, mouldList, beginDay,
                        endDay, dayVulcanizationQty); // 取出可用模具
                if (CollectionUtils.isEmpty(effectiveList)) {
                    continue;
                }
                effectiveList = effectiveList.stream().limit(lhMouldQty).collect(Collectors.toList());// 每次取两个模具

                // 按天统计排产量，并按产能分配到各个模具上
                for (int day = beginDay; day <= endDay; day++) {
//					boolean isFinishDay = (day == endDay); // 结束日，最后一天
                    boolean isFinishDay = false; // 强制都非收尾
                    Integer productionQty = dayPlanQtyMap.get(day); // 每日排产量日期取对应日排产量
                    if (productionQty <= 0) {
                        continue; // 如果已经分配完，则结束
                    }
                    productionQty = productionQty / lhQty; // 每日排产量日期取对应日排产量 / 硫化机台数
                    for (ProductionMouldInfoVo mouldInfoVo : effectiveList) { // 给每个模具分配排产量
                        mouldInfoVo.addProductionInfo(day, requirePlan, isFinishDay, productionQty, cxMachineCodeInfo);
                    }
                }
                requirePlan.resetProductionDataInfo(); // 重算需求计划的部分栏位
                if (requirePlan.getProductionQty() > 0) {
                    requirePlan.setProductionFlag(YesOrNoEnum.YES.getCode()); // 设置成应生产
                }
            }
        }
        return mouldInfoMap;
    }

//	/**
//	 * 构建成型组
//	 *
//	 * @param productionContext
//	 * @return
//	 */
//	private static Map<String, List<CxMachineAllocationPlanHelper>> buildCxAllocationPlanGroup(
//			TbrProductionContext productionContext) {
//		Map<String, List<CxMachineAllocationPlanHelper>> cxAllocationPlanGroup = new HashMap<>();
//		Map<String, CxMachineBaseInfoVo> cxMachineBaseInfo = productionContext.getBaseDataContainer()
//				.getCxMachineBaseInfo(); // 成型排产结果
//		for (CxMachineBaseInfoVo cxInfo : cxMachineBaseInfo.values()) {
//			for (CxMachineAllocationPlanHelper allocationPlan : cxInfo.getAllocationList()) {
//				String structureName = allocationPlan.getProductionPlanInfo().getGroupName(); // 结构
//				List<CxMachineAllocationPlanHelper> groupList = cxAllocationPlanGroup.get(structureName);
//				if (groupList == null) {
//					groupList = new ArrayList<>();
//					cxAllocationPlanGroup.put(structureName, groupList);
//				}
//				groupList.add(allocationPlan);
//			}
//		}
//		return cxAllocationPlanGroup;
//	}

    /**
     * 构建模具组
     *
     * @param productionContext
     * @return
     */
    private Map<String, List<CxMouldDayProductionHelper>> buildMouldProductionGroup(
            TbrProductionContext productionContext) {
        Map<String, List<CxMouldDayProductionHelper>> mouldProductionGroup = new HashMap<>();
        Map<String, ProductionMouldInfoVo> mouldProductionList = productionContext.getBaseDataContainer()
                .getMouldInfoMap(); // 模具排产结果
        for (ProductionMouldInfoVo production : mouldProductionList.values()) {
            Map<Integer, List<CxMouldDayProductionHelper>> dayProductionInfoMap = production.getDayProductionInfo();
            if (CollectionUtils.isEmpty(dayProductionInfoMap)) {
                continue;
            }
            for (List<CxMouldDayProductionHelper> dayProduction : dayProductionInfoMap.values()) {
                dayProduction.forEach(day -> {
                    String structureName = day.getStructureName(); // 结构
                    List<CxMouldDayProductionHelper> groupList = mouldProductionGroup.get(structureName);
                    if (groupList == null) {
                        groupList = new ArrayList<>();
                        mouldProductionGroup.put(structureName, groupList);
                    }
                    groupList.add(day);
                });
            }
        }
        return mouldProductionGroup;
    }

    /**
     * 从排产计划中挑选出在startDay~endDay能进行排产的sku计划
     * 1、挑选在startDay~endDay还可进行双模排产的sku，需要有未排常规储备量 4、库销比低的优先 5、成品库存超6个月的少的优先
     * 6、如果挑选的sku与其它sku是共用模具，且是存在其它sku最后两副模具(即模具受限)则，需排产量小的优先
     *
     * @param productionContext  排产上下文
     * @param startDay           排产开始日
     * @param endDay             排产结束日
     * @param productionPlanList 排产计划
     * @return
     */
    private String getSelectedAddSku(TbrProductionContext productionContext, Integer startDay, Integer endDay,
                                     List<MonthPlanProductionRequirePlanVo> productionPlanList) {
        if (CollectionUtils.isEmpty(productionPlanList)) {
            return "";
        }
        // 提取所有sku的物料描述
        Set<String> allMaterialDescSet = productionPlanList.stream()
                .map(MonthPlanProductionRequirePlanVo::getMaterialDesc).collect(Collectors.toSet());
        Set<String> enableMaterialDescSet = productionContext
                .getHasMouldCapacity(ProductionConstant.DOUBLE_MOULD_PRODUCTION, allMaterialDescSet, startDay, endDay);
        if (CollectionUtils.isEmpty(enableMaterialDescSet)) {
            return "";
        }
        List<MonthPlanProductionRequirePlanVo> enablePlanList = productionPlanList.stream()
                .filter(plan -> enableMaterialDescSet.contains(plan.getMaterialDesc())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(enablePlanList)) {
            return "";
        }
        // 只看有常规储备的sku
        List<MonthPlanProductionRequirePlanVo> hasReserveQtyPlanList = enablePlanList.stream()
                .filter(s -> s.getConventionReserveQty() > 0).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasReserveQtyPlanList)) {
            return "";
        }
        // 库销比低的优先
        Double minInventorySalesRatio = hasReserveQtyPlanList.stream()
                .filter(plan -> plan.getInventorySalesRatio() != null)
                .mapToDouble(MonthPlanProductionRequirePlanVo::getInventorySalesRatio).min().orElse(0);
        List<MonthPlanProductionRequirePlanVo> minInventorySalesRatioList = hasReserveQtyPlanList.stream()
                .filter(plan -> minInventorySalesRatio.equals(plan.getInventorySalesRatio()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(minInventorySalesRatioList)) {
            minInventorySalesRatioList = hasReserveQtyPlanList;
        }
        // 成品库存超6个月的少的优先
        minInventorySalesRatioList.sort((s1, s2) -> {
            Integer stock1 = productionContext.getOverSixMonthStockMap().getOrDefault((s1.getMaterialDesc()), 0);
            Integer stock2 = productionContext.getOverSixMonthStockMap().getOrDefault((s2.getMaterialDesc()), 0);
            return stock1.compareTo(stock2);
        });
        String materialDesc = CollectionUtils.firstElement(minInventorySalesRatioList).getMaterialDesc();
        Set<String> limitShareMouldSet = productionContext.getLimitShareMouldOtherSku(materialDesc, startDay, endDay);
        if (CollectionUtils.isEmpty(limitShareMouldSet)) {
            return materialDesc;
        }
        List<MonthPlanProductionRequirePlanVo> limitShareList = enablePlanList.stream()
                .filter(plan -> limitShareMouldSet.contains(plan.getMaterialDesc())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(limitShareList)) {
            return materialDesc;
        }
        // 加入自己
        enablePlanList.forEach(plan -> {
            if (materialDesc.equals(plan.getMaterialDesc())) {
                limitShareList.add(plan);
            }
        });
        Map<String, Long> limitGroup = new HashMap<>();
        Map<String, List<MonthPlanProductionRequirePlanVo>> limitShareMap = limitShareList.stream()
                .collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc));
        limitShareMap.forEach((limitMaterial, planList) -> limitGroup.put(limitMaterial,
                planList.stream().mapToLong(MonthPlanProductionRequirePlanVo::getVirtualProductionQty).sum()));
        Optional<Map.Entry<String, Long>> minEntry = limitGroup.entrySet().stream().min(Map.Entry.comparingByValue());
        return minEntry.get().getKey();
    }

    /**
     * 从分组计划中获取有常规储备的Sku(selectedMaterialDesc)
     *
     * @param productionPlanList   分组排产计划(TBR-结构名)
     * @param selectedMaterialDesc 选中的Sku
     * @return
     */
    private SkuNeedProductionInfo getNeedProductionQty(List<MonthPlanProductionRequirePlanVo> productionPlanList,
                                                       String selectedMaterialDesc) {
        if (CollectionUtils.isEmpty(productionPlanList) || StringUtils.isBlank(selectedMaterialDesc)) {
            return null;
        }
        List<MonthPlanProductionRequirePlanVo> selectedPlanList = productionPlanList.stream()
                .filter(plan -> plan.hasSelectedProduction(selectedMaterialDesc)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(selectedPlanList)) {
            return null;
        }
        // 是否有常规储备排产量
        List<MonthPlanProductionRequirePlanVo> heightList = selectedPlanList.stream()
                .filter(plan -> plan.getConventionReserveQty() > BigDecimal.ZERO.longValue())
                .collect(Collectors.toList());
        // 高优先级优先
        if (CollectionUtils.isEmpty(heightList)) {
            return null;
        }
        return new SkuNeedProductionInfo(ProductionQtyModelEnum.RESERVE_QTY, heightList);
    }

    /**
     * 获取初始化业务的参数设定
     *
     * @param productionContext
     * @return
     */
    private ProductionCapacityParamConfiguration createParamConfiguration(TbrProductionContext productionContext) {
        ProductionCapacityParamConfiguration configuration = new ProductionCapacityParamConfiguration();
        List<String> paramCodeList = new ArrayList<>(64);
        // 日排产相关
        paramCodeList.add(MonthPlanEnums.DAY_CHANGE_GROUP_COUNT.getCode());
        paramCodeList.add(MonthPlanEnums.CHANGE_MOULD_LH_MACHINE_NUMBER.getCode());
        paramCodeList.add(MonthPlanEnums.CHANGE_MOULD_FIRST_QTY.getCode());
        paramCodeList.add(MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY_DIFF.getCode());
        paramCodeList.add(MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY.getCode());
        paramCodeList.add(MonthPlanEnums.CHANGE_TYPE_BLOCK_MAX_QTY.getCode());
        paramCodeList.add(MonthPlanEnums.SINGLE_CX_EMBRYO_CODE_COUNT.getCode());
        paramCodeList.add(MonthPlanEnums.DAY_MAX_CAPACITY.getCode());
        paramCodeList.add(MonthPlanEnums.DAY_MIN_CAPACITY.getCode());
        // 排产控制相关
        paramCodeList.add(MonthPlanEnums.SUM_PRODUCTION_QTY.getCode());
        paramCodeList.add(MonthPlanEnums.HEIGHT_DIFF_QTY.getCode());
        paramCodeList.add(MonthPlanEnums.MOULD_SECOND_PRODUCTION.getCode());
        paramCodeList.add(MonthPlanEnums.BOOST_AVERAGE_VALUE.getCode());
        paramCodeList.add(MonthPlanEnums.MAX_BOOST_DAY.getCode());
        paramCodeList.add(MonthPlanEnums.MIN_PRODUCTION_DAYS.getCode());
        paramCodeList.add(MonthPlanEnums.MIN_ALLOCATION_DAYS.getCode());
        paramCodeList.add(MonthPlanEnums.NO_CYCLE_PRODUCTION_MIN_LH_MACHINE_NUMBER.getCode());
        // 降膜排产相关
        paramCodeList.add(MonthPlanEnums.DEDUCT_MOULD_MIN_LH_MACHINE_COUNT.getCode());
        paramCodeList.add(MonthPlanEnums.FIRST_NEAR_DEAD_LINE_DAY.getCode());
        paramCodeList.add(MonthPlanEnums.FIRST_NEAR_DEAD_LINE_MAX_LH_MACHINE_COUNT.getCode());
        paramCodeList.add(MonthPlanEnums.SECOND_NEAR_DEAD_LINE_DAY.getCode());
        paramCodeList.add(MonthPlanEnums.SECOND_NEAR_DEAD_LINE_MAX_LH_MACHINE_COUNT.getCode());
        paramCodeList.add(MonthPlanEnums.LAST_NEAR_DEAD_LINE_DAY.getCode());
        paramCodeList.add(MonthPlanEnums.LAST_NEAR_DEAD_LINE_MAX_LH_MACHINE_COUNT.getCode());
        // 其他
        paramCodeList.add(MonthPlanEnums.SECTION_WIDTH_DIFF_VALUE.getCode());
        // 获取数据
        Map<String, Object> paramConfigurationMap = this.getFactoryParamByCondition(productionContext, paramCodeList);
        if (CollectionUtils.isEmpty(paramConfigurationMap)) {
            return configuration;
        }
        // 其它
        configuration.setSectionWidthDiffValue(
                (Integer) paramConfigurationMap.get(MonthPlanEnums.SECTION_WIDTH_DIFF_VALUE.getCode()));
        // 排产控制相关
        configuration.setMinProductionDays(
                (Integer) paramConfigurationMap.get(MonthPlanEnums.MIN_PRODUCTION_DAYS.getCode()));
        configuration.setMinAllocationDays(
                (Integer) paramConfigurationMap.get(MonthPlanEnums.MIN_ALLOCATION_DAYS.getCode()));
        configuration.setNoCycleProductionMinLhMachineNumber((Integer) paramConfigurationMap
                .get(MonthPlanEnums.NO_CYCLE_PRODUCTION_MIN_LH_MACHINE_NUMBER.getCode()));
        configuration.setMaxBoostDay((Integer) paramConfigurationMap.get(MonthPlanEnums.MAX_BOOST_DAY.getCode()));
        configuration.setBoostAverageValue(
                (Integer) paramConfigurationMap.get(MonthPlanEnums.BOOST_AVERAGE_VALUE.getCode()));
        configuration.setMouldSecondProduction(
                (Integer) paramConfigurationMap.get(MonthPlanEnums.MOULD_SECOND_PRODUCTION.getCode()));
        configuration.setHeightDiffQty((Integer) paramConfigurationMap.get(MonthPlanEnums.HEIGHT_DIFF_QTY.getCode()));
        configuration
                .setSumProductionQty((Integer) paramConfigurationMap.get(MonthPlanEnums.SUM_PRODUCTION_QTY.getCode()));
        // 日排产相关
        configuration.setDayChangeGroupCount(
                (Integer) paramConfigurationMap.get(MonthPlanEnums.DAY_CHANGE_GROUP_COUNT.getCode()));
        configuration.setChangeMouldLhMachineNumber(
                (Integer) paramConfigurationMap.get(MonthPlanEnums.CHANGE_MOULD_LH_MACHINE_NUMBER.getCode()));
        configuration.setChangeMouldFirstQty(
                (Integer) paramConfigurationMap.get(MonthPlanEnums.CHANGE_MOULD_FIRST_QTY.getCode()));
        configuration.setChangeTypeBlockQtyDiff(
                (Integer) paramConfigurationMap.get(MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY_DIFF.getCode()));
        configuration.setChangeTypeBlockQty(
                (Integer) paramConfigurationMap.get(MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY.getCode()));
        configuration.setChangeTypeBlockMaxQty(
                (Integer) paramConfigurationMap.get(MonthPlanEnums.CHANGE_TYPE_BLOCK_MAX_QTY.getCode()));
        configuration.setSingleCxEmbryoCodeCount(
                (Integer) paramConfigurationMap.get(MonthPlanEnums.SINGLE_CX_EMBRYO_CODE_COUNT.getCode()));
        configuration.setDayMaxCapacity((Integer) paramConfigurationMap.get(MonthPlanEnums.DAY_MAX_CAPACITY.getCode()));
        configuration.setDayMinCapacity((Integer) paramConfigurationMap.get(MonthPlanEnums.DAY_MIN_CAPACITY.getCode()));
        // 降膜排产相关
        configuration.setDeductMouldMinLhMachineCount(
                (Integer) paramConfigurationMap.get(MonthPlanEnums.DEDUCT_MOULD_MIN_LH_MACHINE_COUNT.getCode()));
        configuration.setFirstNearDeadLineMaxLhMachineCount((Integer) paramConfigurationMap
                .get(MonthPlanEnums.FIRST_NEAR_DEAD_LINE_MAX_LH_MACHINE_COUNT.getCode()));
        configuration.setFirstNearDeadLineDay(
                (Integer) paramConfigurationMap.get(MonthPlanEnums.FIRST_NEAR_DEAD_LINE_DAY.getCode()));
        configuration.setSecondNearDeadLineMaxLhMachineCount((Integer) paramConfigurationMap
                .get(MonthPlanEnums.SECOND_NEAR_DEAD_LINE_MAX_LH_MACHINE_COUNT.getCode()));
        configuration.setSecondNearDeadLineDay(
                (Integer) paramConfigurationMap.get(MonthPlanEnums.SECOND_NEAR_DEAD_LINE_DAY.getCode()));
        configuration.setLastNearDeadLineMaxLhMachineCount(
                (Integer) paramConfigurationMap.get(MonthPlanEnums.LAST_NEAR_DEAD_LINE_MAX_LH_MACHINE_COUNT.getCode()));
        configuration.setLastNearDeadLineDay(
                (Integer) paramConfigurationMap.get(MonthPlanEnums.LAST_NEAR_DEAD_LINE_DAY.getCode()));
        return configuration;
    }

    /**
     * 获取排程参数设置
     *
     * @param context
     * @param paramCodeList
     * @return
     */
    private Map<String, Object> getFactoryParamByCondition(Context context, List<String> paramCodeList) {
        QueryWrapper<FactoryParam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", context.getFactoryCode());
        queryWrapper.in("PARAM_CODE", paramCodeList);
        queryWrapper.eq(StringUtils.isNotBlank(context.getProductType().getValue()), "PRODUCT_TYPE_CODE",
                context.getProductType().getValue());
        List<FactoryParam> paramConfigurationList = factoryParamMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(paramConfigurationList)) {
            return Collections.emptyMap();
        }
        Map<String, FactoryParam> paramConfigurationMap = paramConfigurationList.stream()
                .collect(Collectors.toMap(FactoryParam::getParamCode, Function.identity()));
        Map<String, Object> paramValueMap = new HashMap<>(paramConfigurationMap.size());
        // 数据类型转换
        paramConfigurationMap.forEach((key, paramConfiguration) -> paramValueMap.put(key,
                FactoryParamUtils.getParamValue(paramConfiguration)));
        return paramValueMap;
    }

    /**
     * 获取需要排产的SKU的模具配置信息 key = materialDesc: value =
     * List<MonthPlanProductMouldInfoVo>
     *
     * @param productionContext
     * @return
     */
    private Map<String, List<MonthPlanProductMouldInfoVo>> getProductionMouldInfo(
            TbrProductionContext productionContext) {
        List<MonthPlanProductMouldInfoVo> allMouldRelationInfoList = new ArrayList<>();
        // 已有模具的配置关系
        List<MonthPlanProductMouldInfoVo> productMouldInfoList = this.getDataService()
                .getEnableProductionMouldInfo(productionContext);
        if (!CollectionUtils.isEmpty(productMouldInfoList)) {
            allMouldRelationInfoList.addAll(productMouldInfoList);
        }
        // 新模具到货计划关系
        List<MonthPlanProductMouldInfoVo> mouldDeliveryList = this.getDataService()
                .getEnableProductionMouldDeliveryInfo(productionContext);
        if (!CollectionUtils.isEmpty(mouldDeliveryList)) {
            allMouldRelationInfoList.addAll(mouldDeliveryList);
        }
        if (CollectionUtils.isEmpty(allMouldRelationInfoList)) {
            return Collections.emptyMap();
        }
        return allMouldRelationInfoList.stream()
                .collect(Collectors.groupingBy(MonthPlanProductMouldInfoVo::getMaterialDesc));
    }

    /**
     * 获取SKU的日硫化产能信息 key = materialDesc: value = MonthPlanProductLhCapacityVo
     *
     * @param productionContext 排产上下文
     * @param mode              日硫化量模式
     * @return
     */
    private Map<String, MonthPlanProductLhCapacityVo> getProductLhCapacityInfo(TbrProductionContext productionContext,
                                                                               DayVulcanizationModeEnum mode) {
        List<MonthPlanProductLhCapacityVo> lhCapacityList = this.getDataService()
                .getProductLhCapacityInfo(productionContext);
        if (CollectionUtils.isEmpty(lhCapacityList)) {
            log.info(TbrProductionInitLogRecorder.addDayLhCapacityInfoEmptyLog(productionContext));
            return Collections.emptyMap();
        }
        // 计算日硫化产能
        lhCapacityList.forEach(lhCapacity -> lhCapacity.calculateDayVulcanizationQty(mode));
        return lhCapacityList.stream().collect(Collectors.toMap(MonthPlanProductLhCapacityVo::getMaterialDesc,
                Function.identity(), (before, after) -> after));
    }

    /**
     * 获取初始化业务的参数设定
     *
     * @param productionContext
     * @return
     */
    private ProductionInitParamConfiguration createInitParamConfiguration(TbrProductionContext productionContext) {
        ProductionInitParamConfiguration configuration = new ProductionInitParamConfiguration();
        List<String> paramCodeList = new ArrayList<>(16);
        paramCodeList.add(MonthPlanEnums.OPEN_PREEMPTION_MOULD.getCode());
        paramCodeList.add(MonthPlanEnums.OPEN_LEVEL_RATIO.getCode());
        paramCodeList.add(MonthPlanEnums.DAY_VULCANIZATION_MODE.getCode());
        Map<String, Object> paramConfigurationMap = this.getDataService().getFactoryParamByCondition(productionContext,
                paramCodeList);
        if (CollectionUtils.isEmpty(paramConfigurationMap)) {
            log.info(TbrProductionInitLogRecorder.addInitParamEmptyLog(productionContext));
            return configuration;
        }
        configuration.setOpenPreemptionMouldCapacity(
                (String) paramConfigurationMap.get(MonthPlanEnums.OPEN_PREEMPTION_MOULD.getCode()));
        configuration.setOpenLevelRatio((String) paramConfigurationMap.get(MonthPlanEnums.OPEN_LEVEL_RATIO.getCode()));
        // 日硫化量获取
        String dayVulcanizationParam = (String) paramConfigurationMap
                .get(MonthPlanEnums.DAY_VULCANIZATION_MODE.getCode());
        if (StringUtils.isBlank(dayVulcanizationParam)) {
            configuration.setDayVulcanizationQtyConfiguration(DayVulcanizationModeEnum.STANDARD_CAPACITY);
        } else {
            configuration
                    .setDayVulcanizationQtyConfiguration(DayVulcanizationModeEnum.getInstance(dayVulcanizationParam));
        }
        return configuration;
    }

    /**
     * 根据模具关系，获取在startDay~endDay有效排产的模具信息
     *
     * @param skuRelationList 配置的模具关系
     * @param startDay        排产开始日
     * @param endDay          排产结束日
     * @return
     */
    private List<ProductionMouldInfoVo> getEffectiveByRange(Map<String, ProductionMouldInfoVo> mouldInfoMap,
                                                            List<MonthPlanProductMouldInfoVo> skuRelationList, Integer startDay, Integer endDay,
                                                            Integer dayVulcanizationQty) {
        List<ProductionMouldInfoVo> effectiveList = new ArrayList<>();
        skuRelationList.forEach(skuRelation -> {
            ProductionMouldInfoVo mouldInfo = mouldInfoMap.get(skuRelation.getMouldCode());
            if (null == mouldInfo) {
                return;
            }
            if (!mouldInfo.isProduction(startDay, endDay)) {
                return;
            }
            // 判断当天是否已满产
            Map<Integer, List<CxMouldDayProductionHelper>> dayProductionInfo = mouldInfo.getDayProductionInfo();
            if (dayProductionInfo != null) {
                for (int day = startDay; day <= endDay; day++) {
                    List<CxMouldDayProductionHelper> productionList = dayProductionInfo.get(day);
                    if (!CollectionUtils.isEmpty(productionList)) {
                        Integer productionQty = productionList.stream()
                                .mapToInt(CxMouldDayProductionHelper::getProductionQty).sum(); // 合计当天的已排量
                        if (productionQty >= dayVulcanizationQty) {
                            return; // 已经满产能，该模具当天不可用
                        }
                    }
                }
            }
            effectiveList.add(mouldInfo);
        });
        return effectiveList;
    }

    /**
     * 设置排产周期信息等信息
     *
     * @param context
     */
    private void setProductionCycleInfo(Context context) {
        Integer cycleStartDay = this.getDataService().getProductionCycleConfiguration(context);
        context.setStartDay(cycleStartDay);
        Integer year = context.getYear();
        Integer month = context.getMonth();
        // 自然月
        if (context.isNaturalMonth()) {
            LocalDate productionMonth = context.getCurrentMonth();
            Integer monthDays = productionMonth.with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth();
            context.setProductionStartDate(com.zlt.aps.factory.utils.DateUtils.getDate(productionMonth));
            context.setProductionEndDate(com.zlt.aps.factory.utils.DateUtils.getDate(year, month, monthDays));
            return;
        }
        // 非自然月
        LocalDate previousMonth = context.getPreviousMonth();
        context.setProductionStartDate(com.zlt.aps.factory.utils.DateUtils.getDate(previousMonth.getYear(),
                previousMonth.getMonthValue(), cycleStartDay));
        context.setProductionEndDate(com.zlt.aps.factory.utils.DateUtils.getDate(year, month, cycleStartDay - 1));
    }

    /**
     * 在正式排产前进行重置数据处理
     *
     * @param context          排产上下文
     * @param allGroupPlanInfo 所有分组计划对象
     */
    private void resetBeforeFormalProduction(TbrProductionContext productionContext,
                                             Map<String, ProductionPlanGroupInfo> allGroupPlanInfo) {
        LambdaQueryWrapper<MpStructureAllocation> queryWrapper = new LambdaQueryWrapper<MpStructureAllocation>();
        queryWrapper.eq(MpStructureAllocation::getProductionVersion, productionContext.getProductionVersion());
        List<MpStructureAllocation> allAllocationList = mpStructureAllocationMapper.selectList(queryWrapper); // 加载结构排产数据
        // 根据分组转产配置，重新构建分组的限制信息
        allGroupPlanInfo.forEach((groupName, groupProductionInfo) -> {
            List<MpStructureAllocation> groupAllocationList;
            if (CollectionUtils.isEmpty(allAllocationList)) {
                groupAllocationList = new ArrayList<>();
            } else {
                groupAllocationList = allAllocationList.stream()
                        .filter(singleAllocation -> groupName.equals(singleAllocation.getStructureName()))
                        .collect(Collectors.toList());
            }
            // 重新设置分配的机台
            Set<String> allocationSet = groupAllocationList.stream().map(MpStructureAllocation::getCxMachineCode)
                    .collect(Collectors.toSet());
            groupProductionInfo.setAllocationCxMachineCodeSet(allocationSet);
            groupProductionInfo.buildDayProductionLimitInfoByStructureAllocation(productionContext,
                    groupAllocationList);
        });
//		// 处理计划的待排产量及排产标记重置
//		Map<Long, MonthPlanProductionRequirePlanVo> allSinglePlanMap = productionContext.getAllProductionPlan();
//		if (!CollectionUtils.isEmpty(allSinglePlanMap)) {
//			allSinglePlanMap.forEach((monthPlanId, singlePlan) -> singlePlan.resetProductionDataInfo());
//		}
//		// 重新构建模具排产信息，全部清空
//		Map<String, ProductionMouldInfoVo> allMouldInfoMap = productionContext.getBaseDataContainer().getMouldInfoMap();
//		if (!CollectionUtils.isEmpty(allMouldInfoMap)) {
//			allMouldInfoMap.forEach((mouldCode, singleMouldInfo) -> {
//				singleMouldInfo.setFinishDaySet(new HashSet<>());
//				singleMouldInfo.setDayProductionInfo(new HashMap<>());
//			});
//		}
    }

    /**
     * 设置工厂的排产日信息 包含 停产日及开停产的产能比例 t_mdm_work_calendar
     *
     * @param context
     */
    private void setMonthProductionDays(Context context) {
        List<ProductionDayInfoVo> productionDayInfoList = this.getDataService().getProductCalendar(context);
        log.info(TbrBeforeProductionGroupLogRecorder.addProductionCalendarLog(context, productionDayInfoList));
        if (CollectionUtils.isEmpty(productionDayInfoList)) {
            context.setStopDays(Collections.emptySet());
            return;
        }
        // 排产开始日
        Date productionStartDate = context.getProductionStartDate();
        // 开产比例设置
        Map<Integer, Integer> startProductionRatioMap = new HashMap<>(context.getMonthDays());
        List<ProductionDayInfoVo> startProductionDays = productionDayInfoList.stream()
                .filter(productionDayInfo -> YesOrNoEnum.YES.getCode().equals(productionDayInfo.getDayFlag()))
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(startProductionDays)) {
            startProductionDays.forEach(startProductionInfo -> {
                Date startProduction = startProductionInfo.getProductionDate();
                Integer startDay = com.zlt.aps.factory.utils.DateUtils.getIntervalDays(productionStartDate,
                        startProduction);
                startProductionRatioMap.put(startDay, startProductionInfo.getRate());
            });
        }
        context.setCapacityRatioMap(startProductionRatioMap);
        // 停产设置
        List<ProductionDayInfoVo> stopDays = productionDayInfoList.stream()
                .filter(productionDayInfo -> YesOrNoEnum.NO.getCode().equals(productionDayInfo.getDayFlag()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(stopDays)) {
            log.info(TbrBeforeProductionGroupLogRecorder.addNoStopCalendarLog(context));
            context.setStopDays(Collections.emptySet());
            return;
        }
        Set<Integer> stopDaySet = new HashSet<>(context.getMonthDays());
        stopDays.forEach(stopProductionInfo -> {
            Date stopProduction = stopProductionInfo.getProductionDate();
            Integer stopDay = com.zlt.aps.factory.utils.DateUtils.getIntervalDays(productionStartDate, stopProduction);
            stopDaySet.add(stopDay);
        });
        context.setStopDays(stopDaySet);
    }

    /**
     * 构建分组+主花纹的模具信息 TBR 为结构
     *
     * @param productionContext 排产上下文
     * @return
     */
    private void buildGroupMainPatternInfo(TbrProductionContext productionContext) {
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        Map<String, List<MonthPlanProductMouldInfoVo>> mouldAssociationMap = baseDataContainer.getSkuMouldRelationMap();
        Map<String, ProductionMouldInfoVo> allMouldMap = baseDataContainer.getMouldInfoMap();
        if (CollectionUtils.isEmpty(mouldAssociationMap) || CollectionUtils.isEmpty(allMouldMap)) {
            baseDataContainer.setGroupMainPatternMouldRelationMap(Collections.emptyMap());
            return;
        }
        List<MonthPlanProductMouldInfoVo> allRelationList = new ArrayList<>();
        mouldAssociationMap.forEach((materialDesc, relationList) -> {
            if (CollectionUtils.isEmpty(relationList)) {
                return;
            }
            allRelationList.addAll(relationList);
        });
        if (CollectionUtils.isEmpty(allRelationList)) {
            baseDataContainer.setGroupMainPatternMouldRelationMap(Collections.emptyMap());
            return;
        }
        Map<String, List<MonthPlanProductMouldInfoVo>> groupMainPatternMap = allRelationList.stream()
                .collect(Collectors.groupingBy(MonthPlanProductMouldInfoVo::getStructureNameAndMainPattern));
        Map<String, List<ProductionMouldInfoVo>> groupMainPatternMouldMap = new HashMap<>();
        groupMainPatternMap.forEach((groupNameAndMainPattern, relationList) -> {
            if (CollectionUtils.isEmpty(relationList)) {
                return;
            }
            List<ProductionMouldInfoVo> groupMainPatternList = new ArrayList<>();
            Set<String> mouldCodeSet = new HashSet<>();
            relationList.forEach(singleRelation -> {
                String mouldCode = singleRelation.getMouldCode();
                if (mouldCodeSet.contains(mouldCode)) {
                    return;
                }
                mouldCodeSet.add(mouldCode);
                if (allMouldMap.containsKey(mouldCode)) {
                    groupMainPatternList.add(allMouldMap.get(mouldCode));
                }
            });
            if (CollectionUtils.isEmpty(groupMainPatternList)) {
                return;
            }
            groupMainPatternMouldMap.put(groupNameAndMainPattern, groupMainPatternList);
        });
        baseDataContainer.setGroupMainPatternMouldRelationMap(groupMainPatternMouldMap);
    }

    /**
     * 加载超6个月的库存信息
     *
     * @param productionContext
     */
    private void overSixMonthStockHandler(TbrProductionContext productionContext) {
        List<MdmProductStock> stockList = getDataService().getMdmProductStock(productionContext);
        // 过滤库存为空的值
        Map<String, Integer> overSixMonthStockMap = stockList.stream()
                .filter(s -> StringUtils.isNotEmpty(s.getMaterialDesc()) && null != s.getStockQty())
                .collect(Collectors.groupingBy(MdmProductStock::getMaterialDesc,
                        Collectors.collectingAndThen(Collectors.toList(),
                                list -> list.stream().filter(s -> ApsConstant.TRUE.equals(s.getIsExceedSixMonth()))
                                        .collect(Collectors.summingInt(MdmProductStock::getStockQty)))));
        productionContext.setOverSixMonthStockMap(overSixMonthStockMap);
    }

    /**
     * 获取materialDesc在某天范围内所有可排产的模具 在多幅的情形下，共用性差的优先，否则编号大的优先
     *
     * @param productionContext 上下文
     * @param materialDesc      物料描述
     * @param dayMoldQty        单模日产能
     * @param day               排产日
     * @return
     */
    private List<ProductionMouldInfoVo> selectedAllMouldByDay(TbrProductionContext productionContext,
                                                              String materialDesc, Integer dayMoldQty, Integer day) {
        Integer lhMouldQty = ProductionConstant.DOUBLE_MOULD_PRODUCTION; // 硫化机模具配比
        if (StringUtils.isBlank(materialDesc)) {
            return Collections.emptyList();
        }
//		Map<Long, MonthPlanProductionRequirePlanVo> allProductionPlanMap = productionContext.getAllProductionPlan();
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        List<MonthPlanProductMouldInfoVo> skuRelationList = baseDataContainer.getSkuMouldRelationMap()
                .get(materialDesc);
        if (CollectionUtils.isEmpty(skuRelationList)) {
            return Collections.emptyList();
        }
        Map<String, ProductionMouldInfoVo> mouldInfoMap = baseDataContainer.getMouldInfoMap();

        List<ProductionMouldInfoVo> effectiveList = new ArrayList<>();
        skuRelationList.forEach(skuRelation -> {
            ProductionMouldInfoVo mouldInfo = mouldInfoMap.get(skuRelation.getMouldCode());
            if (null == mouldInfo) {
                return;
            }
            if (!mouldInfo.isProduction(day, day)) {
                return;
            }
            // 判断当天是否已满产
            Map<Integer, List<CxMouldDayProductionHelper>> dayProductionInfo = mouldInfo.getDayProductionInfo();
            if (dayProductionInfo != null) {
                List<CxMouldDayProductionHelper> productionList = dayProductionInfo.get(day);
                if (!CollectionUtils.isEmpty(productionList)) {
                    Integer productionQty = productionList.stream()
                            .mapToInt(CxMouldDayProductionHelper::getProductionQty).sum(); // 合计当天的已排量
                    if (productionQty >= dayMoldQty) {
                        return; // 已经满产能，该模具当天不可用
                    }
                }
//				if (productionList != null
//				        && productionList.stream().map(p -> Optional.ofNullable(p.getProductionQty()).orElse(0))
//				                .mapToInt(Integer::intValue).sum() > 0) {
//					return; // 模具当天已排计划，跳过
//				}
            }
            effectiveList.add(mouldInfo);
        });
        if (CollectionUtils.isEmpty(effectiveList)) {
            return Collections.emptyList();
        }
        if (effectiveList.size() < lhMouldQty) {
            return Collections.emptyList();
        }
        effectiveList.sort(Comparator.comparing(ProductionMouldInfoVo::getCommonalityValue)
                .thenComparing(ProductionMouldInfoVo::getMouldCode, Comparator.reverseOrder()));
        int lhSize = (effectiveList.size() / lhMouldQty) * lhMouldQty; // 按模具双模排产模式取整
        return effectiveList.subList(BigDecimal.ZERO.intValue(), lhSize); // 限制可用模具
    }

    /**
     * 获取续作排产信息 续作的分组信息(结构)，对应的成型产能机台和续作的SKU，使用模具-硫化机台数 key = structureName(TBR)
     * CxContinueInfoHelper.continueSkuMouldNumberMap = { key = materialDesc : value
     * = 胎胚、硫化机台数(模具数)等}
     *
     * @param context 排产上下文
     * @return
     */
    private Map<String, CxContinueInfoHelper> getContinueInfo(Context context) {
        // 获取前一个月的排产版本信息
        String factoryCode = context.getFactoryCode();
        LocalDate previousMonth = context.getPreviousMonth();
        Integer year = previousMonth.getYear();
        Integer month = previousMonth.getMonthValue();
        MpFactoryProductionVersion previousVersion = getDataService().getFinalVersion(factoryCode, year, month);
        if (null == previousVersion) {
            return Collections.emptyMap();
        }
        // 根据排产版本信息，确认最后一天的排产SKU信息(包含结构、SKU、使用模具数)
        Context previousContext = new Context();
        previousContext.setFactoryCode(factoryCode);
        previousContext.setYear(year);
        previousContext.setMonth(month);
        previousContext.setProductionStartDate(previousVersion.getProductionStartDate());
        previousContext.setProductionEndDate(previousVersion.getProductionEndDate());
        // 获取上个排产周期的工作日历
        List<ProductionDayInfoVo> previousProductionDayInfo = getDataService().getProductCalendar(previousContext);
        // 确认最后排产日
        Integer lastDay = ProductionCycleUtils.getLastProductionDay(previousVersion, previousProductionDayInfo);
        if (lastDay <= BigDecimal.ZERO.intValue()) {
            return Collections.emptyMap();
        }
        // 获取上个排产周期最后排产日的排产信息
        List<ContinueProductInfo> continueProductionInfoList = getDataService().getContinueProductionInfo(factoryCode,
                year, month, lastDay);
        // 获取续作结构--结构转产表
        Map<String, Set<String>> continueGroupInfo = getContinueGroupInfo(context, factoryCode, year, month, lastDay);
        // 构建续作分组信息(TBR为结构，PCR为英寸)
        BaseDataContainer baseDataContainer = ((TbrProductionContext) context).getBaseDataContainer();
        Map<String, CxMachineBaseInfoVo> cxMachineBaseInfo = baseDataContainer.getCxMachineBaseInfo();
        setContinueGroupByProduct(context, continueProductionInfoList, continueGroupInfo);
        // 设置对应的最新成型硫化配比信息
        List<MonthPlanStructureLhRatioVo> structureLhRatioList = baseDataContainer.getStructureLhRatioList();
        Map<String, CxContinueInfoHelper> initMap = CxContinueInfoHelper.createGroupInfo(continueProductionInfoList,
                cxMachineBaseInfo, structureLhRatioList);
        // 删除无在产机台的在机结构-脏数据
        initMap.entrySet().removeIf(entry -> CollectionUtils.isEmpty(entry.getValue().getCxMachineCodeSet()));
        return initMap;
    }

    /**
     * 获取工厂年份-月份的最后一天排产的分组信息 TBR-结构 PCR-英寸、寸别、寸口
     *
     * @param context     排产上下文
     * @param factoryCode 工厂
     * @param year        年份
     * @param month       月份
     * @param lastDay     最后一天
     * @return
     */
    private Map<String, Set<String>> getContinueGroupInfo(Context context, String factoryCode, Integer year,
                                                          Integer month, Integer lastDay) {
        List<ContinueGroupInfo> continueGroupInfoList = getDataService().getContinueGroupInfo(factoryCode, year, month,
                lastDay);
        if (CollectionUtils.isEmpty(continueGroupInfoList)) {
            log.info(TbrBeforeProductionGroupLogRecorder.addContinueSkuEmptyLog(context));
            return Collections.emptyMap();
        }
        Map<String, List<ContinueGroupInfo>> continueGroupInfoMap = continueGroupInfoList.stream()
                .collect(Collectors.groupingBy(ContinueGroupInfo::getGroupName));
        Map<String, Set<String>> continueGroupInfo = new HashMap<>();
        continueGroupInfoMap.forEach((groupName, continueCxMachineInfoList) -> {
            if (CollectionUtils.isEmpty(continueCxMachineInfoList)) {
                return;
            }
            Set<String> continueCxMachineSet = continueCxMachineInfoList.stream()
                    .map(ContinueGroupInfo::getCxMachineCode).collect(Collectors.toSet());
            continueGroupInfo.put(groupName, continueCxMachineSet);
        });
        return continueGroupInfo;
    }

    /**
     * 对续作的Sku设置分组信息 按分组名匹配 TRB为结构 PCR为英寸
     *
     * @param continueSkuInfo   续作的Sku规格
     * @param continueGroupInfo 续作的分组信息-含机台
     */
    private void setContinueGroupByProduct(Context context, List<ContinueProductInfo> continueSkuInfo,
                                           Map<String, Set<String>> continueGroupInfo) {
        if (CollectionUtils.isEmpty(continueGroupInfo) || CollectionUtils.isEmpty(continueGroupInfo)) {
            return;
        }
        continueSkuInfo.forEach(continueSku -> {
            String groupName = continueSku.getGroupName();
            if (StringUtils.isBlank(groupName)) {
                return;
            }
            Set<String> onLineMachineSet = continueGroupInfo.get(groupName);
            log.warn(TbrBeforeProductionGroupLogRecorder.addContinueGroupNoOnLineMachineLog(context, groupName));
            continueSku.setContinueCxMachineCodeSet(onLineMachineSet);
        });
    }

    /**
     * 数据服务
     *
     * @return
     */
    public ProductionSchedulingDataService getDataService() {
        return productionSchedulingDataService;
    }
}
