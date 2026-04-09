package com.zlt.aps.mp.engine.scheduling.matching;

import static com.zlt.aps.common.core.utils.ApsNumberUtils.*;

import com.alibaba.fastjson.JSON;
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
import com.zlt.aps.maindata.mapper.MpMonthPlanStatisticsEntityMapper;
import com.zlt.aps.mp.api.domain.capacity.MpDailyCapacityLimitVo;
import com.zlt.aps.mp.api.domain.entity.*;
import com.zlt.aps.mp.api.domain.vo.MpDayProductionStatisticsDetailVo;
import com.zlt.aps.mp.engine.capacity.MpMonthPlanDailyCapacityLimit;
import com.zlt.aps.mp.engine.check.SkuSecondChecker;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.daylimit.*;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.*;
import com.zlt.aps.mp.engine.domain.vo.*;
import com.zlt.aps.mp.engine.enums.ContinueTypeEnum;
import com.zlt.aps.mp.engine.enums.DayVulcanizationModeEnum;
import com.zlt.aps.mp.engine.enums.ProductionQtyModelEnum;
import com.zlt.aps.mp.engine.handler.CalculateStructureCxMachineNumber;
import com.zlt.aps.mp.engine.handler.CxLhMouldProductionCalculator;
import com.zlt.aps.mp.engine.handler.MouldProductionResultHandler;
import com.zlt.aps.mp.engine.logrecorder.TbrBeforeProductionGroupLogRecorder;
import com.zlt.aps.mp.engine.mapper.FactoryMonthPlanMouldDayDetailMapper;
import com.zlt.aps.mp.engine.mapper.FactoryMouldingDayResultMapper;
import com.zlt.aps.mp.engine.mapper.MonthPlanRequireMapper;
import com.zlt.aps.mp.engine.mapper.MpStructureAllocationMapper;
import com.zlt.aps.mp.engine.scheduling.BaseDataContainer;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.engine.scheduling.cxcapacity.ProductionCapacityParamConfiguration;
import com.zlt.aps.mp.engine.scheduling.cxcapacity.SkuNeedProductionInfo;
import com.zlt.aps.mp.engine.scheduling.init.ProductionInitParamConfiguration;
import com.zlt.aps.mp.engine.service.DpRequireDataService;
import com.zlt.aps.mp.engine.service.MonthProductionDataService;
import com.zlt.aps.mp.engine.service.ProductionMdmDataService;
import com.zlt.aps.mp.engine.utils.MouldRelationDeduplicator;
import com.zlt.aps.mp.engine.utils.ProductionCycleUtils;
import com.zlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 搭配排产处理类
 *
 * @author hak
 */
@Slf4j
@Component
public class MatchingProductionHandler {
    @Autowired
    private ProductionMdmDataService productionSchedulingDataService;
    @Autowired
    private DpRequireDataService dpRequireDataService;
    @Autowired
    private MonthProductionDataService monthProductionDataService;
    @Autowired
    private ISysConfigService sysConfigService;
    @Autowired
    private CalculateStructureCxMachineNumber calculateStructureCxMachineNumber;
    @Autowired
    private FactoryMouldingDayResultMapper factoryMouldingDayResultMapper;
    @Autowired
    private FactoryMonthPlanMouldDayDetailMapper factoryMonthPlanMouldDayDetailMapper;
    @Autowired
    private MonthPlanRequireMapper monthPlanRequireMapper;
    @Autowired
    private MpStructureAllocationMapper mpStructureAllocationMapper;
    @Autowired
    private MpMonthPlanStatisticsEntityMapper mpMonthPlanStatisticsEntityMapper;
    @Autowired
    private BaseDao baseDao;

    @Value("${debug.ignorSkip.matching:false}")
    private Boolean isIgnorSkip;

    /**
     * 搭配排产（已排产结果入口）
     *
     * @param productionVersion 生产版本
     */
    public void matchingProduction(String productionVersion, TbrProductionContext productionContext) {
        try {
            String config = sysConfigService.selectConfigByKey("monthPlan.skip.matching");
            if (!isIgnorSkip && StringUtils.isNotBlank(config) && Boolean.parseBoolean(config)) {
//            if (true) {
                if (productionContext != null) {
                    baseDao.saveBatch(this.buildProductionStatisticsList(productionContext)); // 跳过搭配也要保存统计
                }
                return; // 跳过搭配开关打开，则直接返回
            }
        } catch (Exception e) {
            log.error("获取配置失败", e);
        }
        // 查询月度计划排产结果
        LambdaQueryWrapper<FactoryMonthPlanMouldDayResult> queryWrapper = new LambdaQueryWrapper<FactoryMonthPlanMouldDayResult>();
        queryWrapper.eq(FactoryMonthPlanMouldDayResult::getProductionVersion, productionVersion);
        List<FactoryMonthPlanMouldDayResult> planList = factoryMouldingDayResultMapper.selectList(queryWrapper);

        LambdaQueryWrapper<FactoryMonthPlanMouldDayDetail> detailQueryWrapper = new LambdaQueryWrapper<FactoryMonthPlanMouldDayDetail>();
        detailQueryWrapper.eq(FactoryMonthPlanMouldDayDetail::getProductionVersion, productionVersion);
        List<FactoryMonthPlanMouldDayDetail> detailLogList = factoryMonthPlanMouldDayDetailMapper
                .selectList(detailQueryWrapper);

        this.matchingProduction(planList, detailLogList);
    }

    /**
     * 搭配排产（计划调整入口）
     *
     * @param planList
     */
    public void matchingProduction(List<FactoryMonthPlanMouldDayResult> planList, List<FactoryMonthPlanMouldDayDetail> detailLogList) {
        if (CollectionUtils.isEmpty(planList)) {
            return;
        }
        // 构建上下文等各项参数
        TbrProductionContext productionContext = this.initProductionContext(planList); // 初始化上下文
        List<MonthPlanProductionRequirePlanVo> requirePlanList = this.selectRequirePlan(productionContext, detailLogList); // 查询需求计划
        this.buildProductionContext(productionContext, planList, detailLogList, requirePlanList); // 填充上下文各项必要数据

        Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap = calculateStructureCxMachineNumber.calculateStructureCxMachineNumber(productionContext, requirePlanList); // 分配成型产能
        productionContext.setGroupProductionInfo(estimateGroupCxAllocationMap);
        this.resetBeforeFormalProduction(productionContext, estimateGroupCxAllocationMap);
        Map<String, CxContinueInfoHelper> cxContinueInfoMap = this.getContinueInfo(productionContext); // 加载续作规格
        // 初始化换模次数
        this.initChangeMouldUsedQty(productionContext);
        // 初始化结构的每日生产统计
        this.initDayProductionInfo(productionContext);
        // 初始化特殊材料库存已占用数据
        this.initSpecialMaterialInfo(productionContext, planList, estimateGroupCxAllocationMap);

        // 调用主流程的入口 -> 实单补量排程算法
        Map<String, Integer> factProdReqMap = this.matchingProduction(productionContext, estimateGroupCxAllocationMap,
                cxContinueInfoMap, true);
        // 调用主流程的入口 -> 搭配排程算法（储备）
        Map<String, Integer> matchingQtyMap = this.matchingProduction(productionContext, estimateGroupCxAllocationMap,
                cxContinueInfoMap, false);

        // 构建排产结果并保存
        this.saveMouldProductionResult(productionContext, planList, detailLogList, factProdReqMap, matchingQtyMap);
    }

    /**
     * 初始化特殊材料库存已占用数据
     * @param productionContext
     * @param planList
     * @param estimateGroupCxAllocationMap
     */
    private void initSpecialMaterialInfo(TbrProductionContext productionContext,
                                         List<FactoryMonthPlanMouldDayResult> planList,
                                         Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap) {
        Map<String, Map<Long, SpecialMaterialInfoVo>> specialMaterialInfoMap = productionContext.getSpecialMaterialInfoMap(); // 初始化特殊材料库存
        for (FactoryMonthPlanMouldDayResult result: planList) {
            ProductionPlanGroupInfo groupInfo = estimateGroupCxAllocationMap.get(result.getStructureName());
            if (groupInfo == null) {
                continue;
            }
            if (!groupInfo.isSpecialMaterial()) {
                continue;
            }
            Map<String, BigDecimal> materialInfoMap = groupInfo.getEmbryoSpecialMaterialInfoMap();
            if (materialInfoMap == null) {
                continue;
            }
            for (Entry<String, BigDecimal> entry: materialInfoMap.entrySet()) {
                String materialCode = entry.getKey();
                BigDecimal useQty = entry.getValue();
                if (useQty == null) {
                    continue;
                }
                Map<Long, SpecialMaterialInfoVo> stockMap = specialMaterialInfoMap.get(materialCode);
                if (stockMap == null) {
                    continue;
                }
                SpecialMaterialInfoVo stockInfo = stockMap.values().iterator().next();
                Long oldQty = stockInfo.getSumSkuAllocateQty();
                Long newQty = BigDecimalUtils.multiply(result.getTotalQty(), useQty).longValue();
                stockInfo.setSumSkuAllocateQty(safeAdd(newQty, oldQty));
            }
        }
    }
    
    /**
     * 搭配排产（主流程入口）
     *
     * @param context                      上下文
     * @param estimateGroupCxAllocationMap 需求计划列表
     * @param cxContinueInfoMap            续作规格
     * @param isActualOrder                是否实单
     * @return 本次排产各规格描述的排产数量统计
     */
    public Map<String, Integer> matchingProduction(Context context,
                                                   Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap,
                                                   Map<String, CxContinueInfoHelper> cxContinueInfoMap,
                                                   boolean isActualOrder) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        String scheduleLogName = isActualOrder? "实单补量": "搭配排产";
        this.addTempLog(productionContext, context.getProductionVersion() + scheduleLogName + "start");
        productionContext.setIsActualOrder(isActualOrder); // 切换实单排产和搭配排产模式
        // 按结构分组的模具排产信息
        Map<String, List<CxMouldDayProductionHelper>> mouldProductionGroup = this
                .buildMouldProductionGroup(productionContext);
        // 机构模具配比
        Map<String, MonthPlanStructureLhRatioVo> structureLhRatioMap = productionContext.getBaseDataContainer()
                .getStructureLhRatioList().stream().collect(Collectors
                        .toMap(MonthPlanStructureLhRatioVo::getStructureName, Function.identity(), (r1, r2) -> r1));
        Map<Long, MonthPlanProductionRequirePlanVo> allSinglePlanMap = productionContext.getAllProductionPlan(); // 需求计划
        Map<String, Integer> newSkuQtyMap = new HashMap<>(); // 本次排产各规格的排产数量统计
        // 遍历所有结构
        for (Entry<String, ProductionPlanGroupInfo> entry : estimateGroupCxAllocationMap.entrySet()) {
            String structureName = entry.getKey(); // 分组名称（TBR：结构）
            ProductionPlanGroupInfo groupInfo = entry.getValue();
            CxContinueInfoHelper continueInfo = cxContinueInfoMap.get(structureName); // 续作规格
            MonthPlanStructureLhRatioVo ratioVo = structureLhRatioMap.get(structureName); // 成型硫化配比
            List<CxMouldDayProductionHelper> mouldDayProductionList = mouldProductionGroup.get(structureName);
            if (CollectionUtils.isEmpty(mouldDayProductionList)) {
                continue; // 成型或模具排程任意一个找不到数据都要跳过这个结构
            }
            // 处理需求计划
            List<MonthPlanProductionRequirePlanVo> productionPlanList = groupInfo.getGroupPlanData();

            // 处理特殊材料
            Integer allcateMaxDay = groupInfo.getDayProductionLimitInfo().keySet().stream().max(Integer::compareTo).orElse(0);
            Integer allcateMinDay = groupInfo.getDayProductionLimitInfo().keySet().stream().min(Integer::compareTo).orElse(0);
            if (!isActualOrder && allcateMaxDay > allcateMinDay) {
                productionContext.updateSpecialMaterialInfoMap(groupInfo, allcateMaxDay - allcateMinDay);
                Integer productionQty = productionPlanList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getProductionQty).sum();
                productionContext.updateSpecialMaterialInfoSkuAllocateQty(groupInfo, productionQty); // 占用
                Integer reserveQty = productionPlanList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getConventionReserveQty).sum();
                Integer remainQty = productionContext.getSpecialMaterialBatchRemainQty(groupInfo, reserveQty, false); // 剩余量
                if (reserveQty < remainQty) { // 如果储备量少于剩余量，则需要补够剩余量，优先补到有富余产能，且搭配量最打的规格上
                    Integer unAllocateQty = remainQty - reserveQty;
                    MonthPlanProductionRequirePlanVo plan = productionPlanList.stream().max(Comparator.comparing(MonthPlanProductionRequirePlanVo::getConventionReserveQty)).orElse(null);
                    plan.setConventionReserveQty(unAllocateQty);
                }
            }
            // 统计那些SKU已排产
            productionPlanList.forEach(plan -> {
                // 计算剩余可搭配量，实单取实际需求量 - 已生产量；非实单取储备量
                Integer remainMatchingQty = isActualOrder ? plan.getFactProdReqQty() - plan.getProducedQty()
                        : plan.getConventionReserveQty();
                remainMatchingQty = remainMatchingQty > 0? remainMatchingQty: 0;
                // 因搭配量只有一条计划，故而可以直接处理奇数，遇到奇数直接+1
                if (!isActualOrder) {
                    remainMatchingQty = (remainMatchingQty & 1) == 0 ? remainMatchingQty : remainMatchingQty + 1;
                }
                if (remainMatchingQty > 0) {
                    plan.setProductionFlag(YesOrNoEnum.YES.getCode()); // 设置成应生产
                }
                plan.setProductionQty(remainMatchingQty);
                plan.setHeightProductionQty(0); // 高优先级
                if (plan.getDayVulcanizationQty() == null) { // 硫化日产能空的赋值为0，防止报错
                    plan.setDayVulcanizationQty(0);
                }
            });

            // 取出最早成型硫化配比不足的日期
            // 本结构按天汇总的日排产量，计算产量限制以及模具限制，需要按key(日期)排序
            TreeMap<Integer, MatchingPlanLimitHelper> limitMap = this.caculateProductDay(productionContext, groupInfo,
                    ratioVo, mouldDayProductionList, allSinglePlanMap, continueInfo); // 计算排产日
            Integer startDay = limitMap.firstKey();
            Integer endDay = limitMap.lastKey();
            if (startDay.compareTo(endDay) > 0) { // 如果开始时间>结束时间，说明该结构满产，直接看下一个结构
                continue;
            }
            String checkMaterialDesc = null; // 检查指定的SKU，用于新模排产后执行模具续作算法
            do {
                // 续作规格搭配排产
                this.matchingScheduleContinue(productionContext, newSkuQtyMap, groupInfo, limitMap, checkMaterialDesc);
                // 新增模具搭配排产
                checkMaterialDesc = this.matchingScheduleNewMould(productionContext, newSkuQtyMap, groupInfo,
                        continueInfo, limitMap);
                if (StringUtils.isEmpty(checkMaterialDesc)) { // 如果有新增模具，则再跑一次续作；没有新增模具则结束。
                    break;
                }
            } while (true);
        }
        this.addTempLog(productionContext, context.getProductionVersion() + scheduleLogName + "end");
        return newSkuQtyMap;
    }

    /**
     * 新增模具搭配排产
     *
     * @param productionContext 上下文
     * @param newSkuQtyMap      各SKU搭配量汇总列表
     * @param groupInfo         结构
     * @param continueInfo      续作SKU信息
     * @param limitMap          产能限制i列表
     * @return 返回有新增模具的SKU
     */
    private String matchingScheduleNewMould(TbrProductionContext productionContext,
                                                 Map<String, Integer> newSkuQtyMap, ProductionPlanGroupInfo groupInfo,
                                                 CxContinueInfoHelper continueInfo,
                                                 TreeMap<Integer, MatchingPlanLimitHelper> limitMap) {
        TreeMap<Integer, MatchingPlanLimitHelper> copyLimitMap = new TreeMap<>(limitMap); // 先复制一份产能限制列表，筛选SKU时会根据本次轮询对列表进行删减
        // 循环取结构向下所有符合搭配生产条件的SKU进行搭配排产
        Set<String> scheduleMaterialDesc = new HashSet<>(); // 记录已排规格，防止重复执行死循环
        do {
            List<MonthPlanProductionRequirePlanVo> productionPlanList = this.getMatchStartDayPlan(productionContext,
                    groupInfo, copyLimitMap, scheduleMaterialDesc); // 获取符合开始日期的规格，包括校验二次上机和换模能力，同时删减不符合条件的产能限制列表
            if (productionPlanList.isEmpty()) { // 没有符合条件的SKU，直接结束
                break;
            }
            // 从处理过的产能限制列表中获取开始时间结束时间
            Integer startDay = copyLimitMap.firstKey();
            Integer endDay = copyLimitMap.lastKey();
            // 获取优先级最高的SKU信息
            String materialDesc = this.getSelectedAddSku(productionContext, startDay, endDay, productionPlanList,
                    scheduleMaterialDesc, null);
            if (StringUtils.isBlank(materialDesc)) {
                break;
            }
            scheduleMaterialDesc.add(materialDesc);
            // 计算需要排产的量
            SkuNeedProductionInfo needProductionInfo = this.getNeedProductionQty(productionPlanList, materialDesc);
            if (null == needProductionInfo) {
                continue;
            }
            // 执行搭配排产算法
            boolean isAddMould = this.matchingScheduleNewSchedule(productionContext, materialDesc, needProductionInfo, newSkuQtyMap, groupInfo,
                    continueInfo, copyLimitMap);
            if (isAddMould) {
                return materialDesc; // 只要有新增模具，则直接结束走续作逻辑
            }
        } while (true);
        return null;
    }

    /**
     * 获取首日
     *
     * @param productionContext
     * @return
     */
    private Integer getFirstDay(TbrProductionContext productionContext) {
        for (int day = FactoryConstant.MONTH_START_DAY; day <= FactoryConstant.MONTH_MAX_DAY; day++) {
            if (productionContext.getStopDays().contains(day)) {
                continue;
            }
            return day;
        }
        return FactoryConstant.MONTH_START_DAY;
    }

    /**
     * 获取符合开始日期的规格，包括校验二次上机和换模能力，同时删减不符合条件的产能限制列表
     *
     * @param productionContext    上下文
     * @param groupInfo            结构信息
     * @param limitMap             产能限制列表
     * @param scheduleMaterialDesc 已轮询过的SKU，防止死循环
     * @return
     */
    private List<MonthPlanProductionRequirePlanVo> getMatchStartDayPlan(TbrProductionContext productionContext,
                                                                        ProductionPlanGroupInfo groupInfo,
                                                                        TreeMap<Integer, MatchingPlanLimitHelper> limitMap,
                                                                        Set<String> scheduleMaterialDesc) {
        // 取出还未轮询过的列表
        List<MonthPlanProductionRequirePlanVo> productionPlanList = groupInfo.getGroupPlanData().stream()
                .filter(plan -> !scheduleMaterialDesc.contains(plan.getMaterialDesc())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(productionPlanList)) {
            return productionPlanList;
        }
        List<MonthPlanProductionRequirePlanVo> realProductionPlanList = new ArrayList<>();
        Integer startDay = limitMap.firstKey();
        Integer endDay = limitMap.lastKey();
        for (int day = startDay; day <= endDay; day++) {
            startDay = day; // 开始时间等于当前校验时间，如果以下校验不通过，则开始日期推后一天
            for (MonthPlanProductionRequirePlanVo plan : productionPlanList) {
                // 如果是实单补量，不能补未排产的sku
//                if (isActualOrder && !plan.getIsSkuProduced()) {
//                    continue;
//                }
                String materialDesc = plan.getMaterialDesc();
                if (scheduleMaterialDesc.contains(materialDesc)) {
                    continue;
                }
                // 检查如果符合二次上机，则从该天开始，否则推后一天继续校验
                if (!this.checkSecOnline(groupInfo, productionContext, materialDesc, day)) {
                    continue; // 校验不通过，看下一天
                }
                // 判断剩余可换模次数
                DayCapacityLimitVo dayCapacityLimit = productionContext.getBaseDataContainer().getDayCapacityLimit();
                Set<Integer> hasChangeMouldDaySet = dayCapacityLimit.getHasChangeMouldProductionDay(productionContext);
                if (CollectionUtils.isEmpty(hasChangeMouldDaySet)) { // 达到换模次数限制，不通过
                    continue; // 校验不通过，看下一天
                }
                realProductionPlanList.add(plan);
            }
            if (CollectionUtils.isEmpty(realProductionPlanList)) {
                limitMap.remove(day); // 当天没有一个SKU符合条件的，移除当天的产能限制列表
            } else {
                break; // 有任意一个SKU符合条件，直接结束
            }
        }
        return realProductionPlanList;
    }

    /**
     * 符合搭配条件的日期在机模具试制
     * 
     * @param productionContext 上下文
     * @param newSkuQtyMap      搭配新增排产列表
     * @param groupInfo         结构
     * @param limitMap          排产限制列表
     * @param isActualOrder     是否实单补量
     * @param checkMaterialDesc 指定检查的SKU，有指定的话只检查该SKU是否可续作
     */
    private void matchingScheduleContinue(TbrProductionContext productionContext, Map<String, Integer> newSkuQtyMap,
                                          ProductionPlanGroupInfo groupInfo,
                                          TreeMap<Integer, MatchingPlanLimitHelper> limitMap,
                                          String checkMaterialDesc) {
        Integer startDay = limitMap.firstKey();
        Integer endDay = limitMap.lastKey();
        List<MonthPlanProductionRequirePlanVo> groupPlanData = groupInfo.getGroupPlanData();
        Map<String, List<MonthPlanProductionRequirePlanVo>> productionPlanMap = groupPlanData.stream()
                .collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc));
        Set<String> scheduleMaterialDesc = new HashSet<>(); // 记录已排规格，防止重复执行死循环
        do {
            // 1、获取优先级最高的SKU信息
            String materialDesc = this.getSelectedAddSku(productionContext, startDay, endDay, groupPlanData,
                    scheduleMaterialDesc, checkMaterialDesc);
            if (StringUtils.isBlank(materialDesc)) {
                break;
            }
            scheduleMaterialDesc.add(materialDesc);
            // 2、计算需要排产的量
            SkuNeedProductionInfo needProductionInfo = this.getNeedProductionQty(groupPlanData, materialDesc);
            if (null == needProductionInfo) {
                continue;
            }
            List<MonthPlanProductionRequirePlanVo> productionPlanList = productionPlanMap.get(materialDesc);
            MonthPlanProductionRequirePlanVo plan = CollectionUtils.firstElement(productionPlanList);
            Integer realStartDay = plan.getMatchBeginDay() != null? plan.getMatchBeginDay(): startDay; // 本次循环的开始日期，如果是续作且开始搭配日不为空，则以此为准，否则从接口开始日开始检索
            Integer realEndDay = this.getRealEndDay(productionContext, plan, realStartDay, endDay); // 本次循环的结束日期
            
            Integer maxProductionQty = needProductionInfo.getDayMaxProductionQty();
            // 3、SKU外层循环，反复扫描该SKU搭配期间的每一天，只要一次扫描能搭配上任意一天，则再重新尝试扫描一次，直到无法搭配上后则结束外层循环
            do {
                // 4、统计每一天所有可用模具
                List<MatchingMouldDayUsedHelper> mouldDayUsedList = this.caculateMouldDayUsed(productionContext,
                        materialDesc, maxProductionQty, realStartDay, realEndDay);
                int beginProductQty = newSkuQtyMap.getOrDefault(materialDesc, 0); // 记录内层循环开始时的搭配量
                // 5、遍历各模具可用列表，取出符合排产条件的日期以及模具
                for (MatchingMouldDayUsedHelper mouldDayUsed : mouldDayUsedList) {
                    Integer usedBeginDate = mouldDayUsed.getBeginDate();
                    Integer usedEndDate = mouldDayUsed.getEndDate();
                    // 5.1、检查SKU是否还有待排产的需求量，没有则结束本SKU的检查
                    Integer productionQty = needProductionInfo.getSumNeedProductionQty(); // 需求量
                    // 5.1.1、收尾前两天，如果是常销规格，即使需求量不足，也要尝试补满
                    boolean isBoost = this.checkIsBoost(productionContext, usedBeginDate, realEndDay,
                            productionPlanList.get(0));
                    if (!isBoost && productionQty <= 0) { // 非实单或者非常销规格，只要余量不足就直接跳过
                        break;
                    }
                    // 5.3、当天已经无法添加排产，跳过
                    MatchingPlanLimitHelper limitHelper = limitMap.get(usedBeginDate);
                    if (limitHelper == null || !limitHelper.isProduct()) {
                        continue;
                    }
                    // 5.4、获取可以排产模具列表
                    List<ProductionMouldInfoVo> continueMouldList = this.getContinueMouldList(productionContext,
                            groupInfo, materialDesc, mouldDayUsed, limitMap, productionQty, isBoost);
                    // 5.5、执行非首日续作排程算法
                    if (!CollectionUtils.isEmpty(continueMouldList)) {
                        continueMouldList.stream().forEach(mould -> {
                            if (this.checkMouldHasProductMaterial(mould, usedBeginDate, materialDesc)) { // 当天没有排产该规格，则当天的排产模具数+1
                                limitHelper.setMouldQty(limitHelper.getMouldQty() + 1);
                            }
                        });
                        // 5.5.1、获取实际日产量
                        Integer realMaxProductionQty = this.getRealDayMaxProductionQty(productionContext, usedBeginDate,
                                maxProductionQty);
                        // 5.5.2、续做排产
                        Integer totalProductionQty = this.matchingScheduleNextDayContinue(productionContext,
                                materialDesc, newSkuQtyMap, groupInfo, productionPlanList, productionQty, isBoost,
                                realMaxProductionQty, usedBeginDate, usedEndDate, continueMouldList);
                        // 5.5.3、更新计划的剩余排产量
                        this.updatePlanRemainQty(productionPlanList, totalProductionQty);
                        productionQty = productionQty > totalProductionQty ? productionQty - totalProductionQty : 0;
                        limitHelper.setPlanQty(limitHelper.getPlanQty() + totalProductionQty);
                        if (totalProductionQty > 0) {
                            String mainPattern = CollectionUtils.firstElement(productionPlanList).getMainPattern(); // 主花纹
                            groupInfo.reCalcMpDailyCapacityLimitByDay(productionContext, usedBeginDate, mainPattern); // 重新计算统计产能
                            this.updateMatchDay(productionPlanList, usedBeginDate); // 更新搭配日期
                            if (plan.getMatchEndDay() == realEndDay) { // 如果区间最后一天有排产，且往后结构还没有结束，则继续尝试往后延一天
                                Integer nextEndDay = this.getNextDay(productionContext, realEndDay, endDay);
                                if (nextEndDay > 0 && nextEndDay <= endDay) {
                                    realEndDay = nextEndDay;
                                }
                            }
                        }
                    }
                }
                // 6、如果本轮循环没有更新搭配量，则说明SKU已经无法继续搭配，结束SKU外层循环
                int newProductQty = newSkuQtyMap.getOrDefault(materialDesc, 0);
                if (beginProductQty == newProductQty) {
                    break;
                }
            } while (true);
        } while (true);
    }

    /**
     * 更新搭配日期
     * @param productionPlanList
     * @param day
     */
    private void updateMatchDay(List<MonthPlanProductionRequirePlanVo> productionPlanList, Integer day) {
        productionPlanList.forEach(item -> {
            if (item.getMatchBeginDay() == null) {
                item.setMatchBeginDay(day);
            }
            if (item.getMatchEndDay() == null || item.getMatchEndDay() < day) {
                item.setMatchEndDay(day);
            }
        });
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
    private Integer getRealEndDay(TbrProductionContext productionContext, MonthPlanProductionRequirePlanVo plan, Integer beginDay, Integer endDay) {
        // 1、如果需求计划的搭配结束日期已经在之前的循环中结算出来则以此为准，否则以SKU的收尾日为准
        Integer realEndDay = plan.getMatchEndDay() != null? plan.getMatchEndDay(): endDay;
        // 2、不能早于搭配开始日期
        if (realEndDay < beginDay) {
            realEndDay = beginDay;
        }
        // 3、如果搭配结束日期还早于结构结束日期，则需要看到下一天
        if (realEndDay < endDay) {
            Integer nextDay = this.getNextDay(productionContext, realEndDay, endDay);
            if (nextDay > 0) {
                realEndDay = nextDay;
            }
        }
        return realEndDay;
    }

    /**
     * 检查是否需要补量
     * 
     * @param productionContext 上下文
     * @param day               排产日
     * @param endDay            结构结束日
     * @param requirePlan       需求计划
     * @return
     */
    private boolean checkIsBoost(TbrProductionContext productionContext, Integer day, Integer endDay,
                                 MonthPlanProductionRequirePlanVo requirePlan) {
//        Boolean isActualOrder = productionContext.getIsActualOrder();
//        ProductionCapacityParamConfiguration param = productionContext.getBaseDataContainer().getParamConfiguration();
//        Integer maxBoostDay = param.getMaxBoostDay();
//        Set<String> productionTypeSet = param.getBoostProductionType();
//        // 实单且常销规格，需要收尾补量
//        return isActualOrder && productionTypeSet.contains(requirePlan.getProductionType())
//                && endDay - day <= maxBoostDay;
        return false;
    }

    /**
     * 获取可续作模具模具的列表
     * 
     * @param productionContext 上下文
     * @param groupInfo         结构信息
     * @param materialDesc      排产SKU
     * @param mouldDayUsed      模具日可使用情况
     * @param limitMap          排产限制列表
     * @param productionQty     剩余可搭配量
     * @param isBoots           是否收尾补量
     * @return
     */
    private List<ProductionMouldInfoVo> getContinueMouldList(TbrProductionContext productionContext,
                                                             ProductionPlanGroupInfo groupInfo, String materialDesc,
                                                             MatchingMouldDayUsedHelper mouldDayUsed,
                                                             TreeMap<Integer, MatchingPlanLimitHelper> limitMap,
                                                             Integer productionQty,
                                                             Boolean isBoost) {
        Integer startDay = limitMap.firstKey(); // 结构排产区间开始日
        Integer endDay = limitMap.lastKey(); // 结构排产区间结束日
        Integer day = mouldDayUsed.getBeginDate(); // 排产日
        Integer lastDay = this.getLastDay(productionContext, day, startDay); // 上一个排产日
        MatchingPlanLimitHelper limitHelper = limitMap.get(day); // 当天排产限制
        
        // 1、取出物料描述涉及的需求计划
        List<MonthPlanProductionRequirePlanVo> groupPlanData = groupInfo.getGroupPlanData(); // 需求计划
        Map<String, List<MonthPlanProductionRequirePlanVo>> productionPlanMap = groupPlanData.stream()
                .collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc)); // 需求按计划物料描述分组
        List<MonthPlanProductionRequirePlanVo> productionPlanList = productionPlanMap.get(materialDesc);
        List<ProductionMouldInfoVo> continueMouldList = new ArrayList<>(); // 续作模具
        List<ProductionMouldInfoVo> twoMouldList = new ArrayList<>(ProductionConstant.DOUBLE_MOULD_PRODUCTION); // 一次添加双模
        // 2、从可用模具中挑选两付符合条件的模具
        List<ProductionMouldInfoVo> mouldList = mouldDayUsed.getMouldInfoList();
        for (ProductionMouldInfoVo mouldInfo : mouldList) {
            inner:
            // 2.1、模具上一天有排产该SKU
            if (this.checkMouldHasProductMaterial(mouldInfo, lastDay, materialDesc)) { // 上一天有排产该物料，说明是续作
                // 2.2、检查当天是否有排产该SKU
                boolean hasProduct = this.checkMouldHasProductMaterial(mouldInfo, day, materialDesc); // 模具今天是否有生产该规格
                // 2.2.1、如果没有排产该SKU,相当于要加模，需要判断排产参数、上一天模具是否已经收尾
                if (!hasProduct) {
                    // 2.2.1.1、排产参数校验
                    if (!this.checkProductParam(productionContext, productionPlanList, groupInfo, materialDesc, day, startDay, endDay)) {
                        break inner;
                    }
                    // 2.2.1.2、最大模具数校验
                    if (limitHelper.getMaxMouldQty() <= limitHelper.getMouldQty()) {
                        break inner;
                    }
                    // 2.2.1.3、判断上一天该模具是否已经收尾
                    MonthPlanProductionRequirePlanVo firstPlan = productionPlanList.get(0);
                    if (this.checkLastDayIsWrapUp(productionContext, groupInfo, mouldInfo, firstPlan, materialDesc, lastDay, startDay)) {
                        break inner;
                    }
                    // 2.2.1.4、最大硫化机数校验
                    if (!this.checkLhMachineCount(productionContext, groupInfo, day)) {
                        continue;
                    }
                } 
                // 2.2.2、如果有排产该SKU,相当于要补模具的余量，需要模拟当天的排产，判断是否存在拼机台的情况，如果是拼机台可能会再加量后导致硫化机台数发生变化
                // 以下条件只有在模具即将选满，且模具本身有排产的的时候才需要校验
                else if (twoMouldList.size() + 1 == ProductionConstant.DOUBLE_MOULD_PRODUCTION) {
                    // 2.2.2.1、候选模具全部放到一个列表中，一同执行模拟排产
                    MonthPlanProductionRequirePlanVo firstPlan = productionPlanList.get(0);
                    List<ProductionMouldInfoVo> checkMouldList = new ArrayList<>(ProductionConstant.DOUBLE_MOULD_PRODUCTION);
                    checkMouldList.add(mouldInfo);
                    checkMouldList.addAll(twoMouldList);
                    this.simulatedMould(productionContext, groupInfo, checkMouldList, firstPlan, day, endDay, productionQty, isBoost);
                    // 2.2.2.2、判断排产结果是否出现硫化机台数超出限制
                    GroupPlanCxLhCapacityLimitHelper limit = groupInfo.getDayProductionLimitInfo().get(day);
                    int maxLhMachineCount = limit != null? limit.getMaxLhMachineCount(): 0;
                    MpDailyCapacityLimitVo dailyLimit = groupInfo.getDailyCapacityLimitVoMap().get(day);
                    int usedLhMachines = dailyLimit != null? dailyLimit.getUsedLhMachines(): 0;
                    // 2.2.2.3、检查是否超总硫化机数量
                    Integer allUsedLhMachines = productionContext.getGroupProductionInfo().values().stream()
                            .map(g -> g.getDailyCapacityLimitVoMap().get(day)).filter(Objects::nonNull)
                            .mapToInt(MpDailyCapacityLimitVo::getUsedLhMachines).sum();
                    Integer allMaxLhMachines = productionContext.getBaseDataContainer().getLhMachineInfoList().size();
                    // 2.2.2.4、完成判断后重算当天的排产统计，防止后续使用有问题
                    groupInfo.reCalcMpDailyCapacityLimitByDay(productionContext, day, firstPlan.getMainPattern());
                    // 2.2.2.5、判断结构机台数
                    if (usedLhMachines > maxLhMachineCount) {
                        break inner;
                    }
                    // 2.2.2.6、判断总机台数
                    if (allUsedLhMachines > allMaxLhMachines) {
                        break inner;
                    }
                }
                // 2.3、符合排产条件校验的模具添加到列表中
                twoMouldList.add(mouldInfo);
            }
            // 2.4、有两幅符合排产条件的模具就开始执行续作排产
            if (twoMouldList.size() == ProductionConstant.DOUBLE_MOULD_PRODUCTION) {
                continueMouldList.addAll(twoMouldList);
                twoMouldList.clear();
            }
        }
        return continueMouldList;
    }

    /**
     * 判断当天该模具是否已经收尾
     * 
     * @param productionContext 上下文
     * @param groupInfo         结构信息
     * @param mouldInfo         待校验模具
     * @param materialDesc      规格
     * @param day               排产日
     * @param startDay          开始日期
     * @return
     */
    private boolean checkLastDayIsWrapUp(TbrProductionContext productionContext, ProductionPlanGroupInfo groupInfo,
                              ProductionMouldInfoVo mouldInfo, MonthPlanProductionRequirePlanVo requirePlan, String materialDesc, Integer day, Integer startDay) {
        if (day <= 0) {
            return false;
        }
        // 计算模具当天的已排产量，如果没有达到日产，则需要判断是收尾、还是增模
        Integer dayProductionQty = this.sumMouldMaterialProductQty(mouldInfo, day, materialDesc);
        Integer dayVulcanizationQty = requirePlan.getDayVulcanizationQty(); // 日产
        Integer singleMouldVulcanizationQty = dayVulcanizationQty / ProductionConstant.DOUBLE_MOULD_PRODUCTION; // 单模硫化日产
        if (dayProductionQty <= 0 || dayProductionQty >= singleMouldVulcanizationQty) { // 昨天有排产，且没有排满
            return false;
        }
        // 查看前天如果也有排产，说明是减模，不可以续作
        Integer lastDay = this.getLastDay(productionContext, day, startDay);
        if (lastDay <= 0) {
            return false;
        }
        if (this.sumMouldMaterialProductQty(mouldInfo, lastDay, materialDesc) > 0) {
            return true;
        }
        return false;
    }

    /**
     * 模拟排模具
     * 
     * @param productionContext 上下文
     * @param groupInfo         结构信息
     * @param mouldInfo         模具信息
     * @param twoMouldList      已选模具
     * @param requirePlan       待排产需求计划
     * @param day               排产日
     * @param endDay            结构排产结束日期
     * @param productionQty     剩余可排产量
     * @param isBoots           是否收尾补量
     */
    private void simulatedMould(TbrProductionContext productionContext, ProductionPlanGroupInfo groupInfo,
                                List<ProductionMouldInfoVo> twoMouldList,
                                MonthPlanProductionRequirePlanVo requirePlan, Integer day, Integer endDay,
                                Integer productionQty, Boolean isBoots) {
        Integer singleMouldVulcanizationQty = requirePlan.getDayVulcanizationQty(); // 单模硫化日产 = 硫化日产 / 双模
        Integer dayVulcanizationQty = singleMouldVulcanizationQty * ProductionConstant.DOUBLE_MOULD_PRODUCTION; // 硫化日产 = 单模硫化日产 * 双模
        String materialDesc = requirePlan.getMaterialDesc();
        // 1、将结构排产信息转换成模具日排产信息
        Map<String, FactoryMonthPlanMouldDayResult> resultMap = this.convertMouldDayResult(groupInfo, endDay);
        // 2、计算模具当天的剩余产能，大于0的才能排产
        Integer dayProductionQty = twoMouldList.stream().mapToInt(m -> this.sumMouldMaterialProductQty(m, day, materialDesc)).sum();
        Integer remainQty = dayVulcanizationQty > dayProductionQty? dayVulcanizationQty - dayProductionQty: 0; // 日硫化量 - 模具已排量
        if (remainQty <= 0) {
            return;
        }
        // 3、获取规格描述对应的排产记录，如果没有则需要新增一笔
        FactoryMonthPlanMouldDayResult mpMouldDayResult = resultMap.get(materialDesc);
        if (mpMouldDayResult == null) {
            mpMouldDayResult = new FactoryMonthPlanMouldDayResult();
            mpMouldDayResult.setStructureName(requirePlan.getStructureName());
            mpMouldDayResult.setMaterialDesc(requirePlan.getMaterialDesc());
            mpMouldDayResult.setMaterialCode(requirePlan.getMaterialCode());
            mpMouldDayResult.setEmbryoCode(requirePlan.getEmbryoCode());
            mpMouldDayResult.setMainMaterialDesc(requirePlan.getMainMaterialDesc());
            mpMouldDayResult.setMainPattern(requirePlan.getMainPattern());
            mpMouldDayResult.setDayVulcanizationQty(singleMouldVulcanizationQty);
            resultMap.put(materialDesc, mpMouldDayResult);
        }
        // 4、更新已生产量
        String fieldName = FactoryConstant.DAY_FIELD + day;
        Integer produceQty = intValue(mpMouldDayResult.getFieldValueByFieldName(fieldName));
        // 5、计算可生产量，如果是收尾补量，则直接补到产能上限
        Integer canProductQty = isBoots? Math.min(productionQty, remainQty): remainQty;
        mpMouldDayResult.setFieldValueByFieldName(fieldName, produceQty + canProductQty);
        // 6、模拟运算统计排产数据
        MpMonthPlanDailyCapacityLimit dailyCapacityLimitObj = new MpMonthPlanDailyCapacityLimit();
        Map<String, Object> paramMap = groupInfo.composeDailyCapacityParamMap(productionContext);
        List<FactoryMonthPlanMouldDayResult> mouldDayResultList = new ArrayList<>(resultMap.values());
        MpDailyCapacityLimitVo daylyCapacityLimitlt = groupInfo.getDailyCapacityLimitVoMap().get(day);
        dailyCapacityLimitObj.calcLhMachinesWithEmbryoTypes(mouldDayResultList, day, daylyCapacityLimitlt, paramMap, mpMouldDayResult.getMainPattern());
    }
    

    /**
     * 转换模具排产结果
     *
     * @param groupInfo 结构
     * @param endDay    结束日
     * @return 模具排产结果列表，KEY：规格描述，VALUE：排产结果
     */
    private Map<String, FactoryMonthPlanMouldDayResult> convertMouldDayResult(ProductionPlanGroupInfo groupInfo,
                                                                              Integer endDay) {
        String structureName = groupInfo.getGroupName();
        GroupPlanCxLhCapacityLimitHelper capacityLimitHelper;
        Map<String, FactoryMonthPlanMouldDayResult> mpProdFinalMap = new HashMap<>();
        for (int i = ProductionConstant.MONTH_START_DAY; i <= endDay; i++) {
            String dayField = FactoryConstant.DAY_FIELD + i;
            capacityLimitHelper = groupInfo.getDayProductionLimitInfo().get(i);
            if (capacityLimitHelper == null) {
                continue;
            }
            Map<String, SkuDayProductionInfoHelper> productionSkuQtyMap = capacityLimitHelper.getProductionSkuQtyInfo();
            if (PubUtil.isEmpty(productionSkuQtyMap)) {
                continue;
            }

            // 组装模具日排产结果
            productionSkuQtyMap.forEach((materialDesc, skuProductionInfo) -> {
                FactoryMonthPlanMouldDayResult mpMouldDayResult = mpProdFinalMap.get(skuProductionInfo.getMaterialDesc());
                if (mpMouldDayResult == null) {
                    mpMouldDayResult = new FactoryMonthPlanMouldDayResult();
                    mpMouldDayResult.setStructureName(structureName);
                    mpMouldDayResult.setMaterialCode(skuProductionInfo.getMaterialCode());
                    mpMouldDayResult.setMaterialDesc(skuProductionInfo.getMaterialDesc());
                    mpMouldDayResult.setEmbryoCode(skuProductionInfo.getEmbryoCode());
                    mpMouldDayResult.setMainMaterialDesc(skuProductionInfo.getMainMaterialDesc());
                    mpMouldDayResult.setMainPattern(skuProductionInfo.getMainPattern());
                    mpMouldDayResult.setDayVulcanizationQty(skuProductionInfo.getDayVulcanizationQty());
                }
                mpMouldDayResult.setFieldValueByFieldName(dayField, skuProductionInfo.getSumProductionQty());
                mpProdFinalMap.put(skuProductionInfo.getMaterialDesc(), mpMouldDayResult);
            });
        }
        return mpProdFinalMap;
    }

    /**
     * 更新计划的剩余排产量
     * @param productionPlanList
     * @param totalProductionQty
     */
    private void updatePlanRemainQty(List<MonthPlanProductionRequirePlanVo> productionPlanList, Integer totalProductionQty) {
        Integer unProductQty = totalProductionQty;
        for (MonthPlanProductionRequirePlanVo plan : productionPlanList) {
            Integer remainQty = plan.getProductionQty();
            if (remainQty > unProductQty) {
                remainQty -= unProductQty;
                unProductQty = 0;
            } else {
                remainQty = 0;
                unProductQty -= remainQty;
            }
            plan.setProductionQty(remainQty);
            if (unProductQty == 0) {
                break;
            }
        }
    }

    /**
     * 检查模具某天是否可排产指定规格
     *
     * @param mouldInfo    待检查模具
     * @param day          待检查排产日
     * @param materialDesc 指定规格，如果传空值，则有任意一个规格有排产都符合条件
     * @return
     */
    private boolean checkMouldCanProductMaterial(ProductionMouldInfoVo mouldInfo, Integer day, String materialDesc) {
        if (day <= 0) {
            return false;
        }
        List<CxMouldDayProductionHelper> dayProduction = mouldInfo.getDayProductionInfo().get(day);
        if (dayProduction == null) {
            return true; // 模具空闲，说明可生产
        }
        return dayProduction.stream()
                .filter(s -> s.getMaterialDesc().equals(materialDesc))
                .mapToInt(CxMouldDayProductionHelper::getProductionQty).sum() > 0; // 模具不空闲，但是生产的是同一个规格，可以尝试补量
    }

    /**
     * 检查模具某天是否有排产指定规格
     *
     * @param mouldInfo    待检查模具
     * @param day          待检查排产日
     * @param materialDesc 指定规格，如果传空值，则有任意一个规格有排产都符合条件
     * @return
     */
    private boolean checkMouldHasProductMaterial(ProductionMouldInfoVo mouldInfo, Integer day, String materialDesc) {
        return sumMouldMaterialProductQty(mouldInfo, day, materialDesc) > 0;
    }
    
    /**
     * 合计模具当天有排产指定SKU的量
     * 
     * @param mouldInfo
     * @param day
     * @param materialDesc
     * @return
     */
    private Integer sumMouldMaterialProductQty(ProductionMouldInfoVo mouldInfo, Integer day, String materialDesc) {
        if (day <= 0) {
            return 0;
        }
        if (StringUtils.isEmpty(materialDesc)) {
            return 0;
        }
        List<CxMouldDayProductionHelper> dayProduction = mouldInfo.getDayProductionInfo().get(day);
        if (dayProduction == null) {
            return 0; // 模具空闲，没生成该SKU
        }
        return dayProduction.stream().filter(s -> s.getMaterialDesc().equals(materialDesc))
                .mapToInt(CxMouldDayProductionHelper::getProductionQty).sum(); // 模具不空闲，但是生产的是同一个规格，可以尝试补量
    }
    
    /**
     * 计算生产日期
     *
     * @param productionContext      上下文
     * @param groupInfo              排程分组对象
     * @param ratioVo                成型硫化配比
     * @param mouldDayProductionList 模具日计划列表
     * @param allSinglePlanMap       需求计划列表
     * @param continueInfo           续作规格
     * @return
     */
    private TreeMap<Integer, MatchingPlanLimitHelper> caculateProductDay(TbrProductionContext productionContext,
                                                                         ProductionPlanGroupInfo groupInfo,
                                                                         MonthPlanStructureLhRatioVo ratioVo,
                                                                         List<CxMouldDayProductionHelper> mouldDayProductionList,
                                                                         Map<Long, MonthPlanProductionRequirePlanVo> allSinglePlanMap,
                                                                         CxContinueInfoHelper continueInfo) {
        Integer lhMouldQty = ProductionConstant.DOUBLE_MOULD_PRODUCTION; // 硫化机模具配比
        // 按天分组硫化排产
        Map<Integer, List<CxMouldDayProductionHelper>> dayModPlanMap = mouldDayProductionList.stream()
                .collect(Collectors.groupingBy(CxMouldDayProductionHelper::getProductionDate));
        Integer cxNum = Optional.ofNullable(groupInfo.getAllocationCxMachineCodeSet()).map(Set::size).orElse(0); // 成型机台数
        Integer rate = Optional.ofNullable(ratioVo).map(MonthPlanStructureLhRatioVo::getLhMachineMaxQty).orElse(0);// 硫化成型配比
        Integer firstQty = intValue(
                productionContext.getBaseDataContainer().getParamConfiguration().getChangeMouldFirstQty()); // 新模首日排产量（双模）
        Integer firtOneMouldQty = firstQty / lhMouldQty; // 单模首日排产量

        // 统计每一天的已排产量
        TreeMap<Integer, MatchingPlanLimitHelper> dayPlanMap = new TreeMap<>();
        for (Entry<Integer, List<CxMouldDayProductionHelper>> modPlan : dayModPlanMap.entrySet()) {
            Integer day = modPlan.getKey();
            List<CxMouldDayProductionHelper> planList = modPlan.getValue();
            
            GroupPlanCxLhCapacityLimitHelper limist = groupInfo.getDayProductionLimitInfo().get(day);
            Integer lhNum = limist != null? limist.getMaxLhMachineCount(): BigDecimalUtils.multiply(cxNum, rate, true).intValue();
            Integer maxMouldNum = lhNum * lhMouldQty; // 换算成模具数 = 硫化机* 2（双模排产）

            Integer maxPlanQty = 0; // 当前最大可排产量
            Integer mouldQty = 0; // 已排模具数
            Integer productionQty = 0; // 已排产量
            Integer maxDayVulcanizationQty = 0;
            Integer lastDay = this.getLastDay(productionContext, day, 1);

            Set<String> mouldCodeSet = new HashSet<>();
            Integer firstDayMouldQty = 0; // 加模数量
            Integer spliceMouldQty = 0; // 可拼机台数量
            for (CxMouldDayProductionHelper dayPlan : planList) {
                // 取出单模具产能
                Integer dayVulcanizationQty = allSinglePlanMap.get(dayPlan.getMonthPlanId()).getDayVulcanizationQty();
                // 如果是新增模具，需要限制产能
                // 检查有几个新增模具，新增模具只能排限制个数
                boolean isContinue = false;
                if (day == this.getFirstDay(productionContext) && continueInfo != null) { // 首日
                    CxContinueSkuInfoHelper continueSkuInfo = continueInfo.getContinueSkuMouldNumberMap()
                            .get(dayPlan.getMaterialDesc());
                    if (continueSkuInfo != null) {
                        isContinue = true;
                    }
                } else { // 非首日，先看前一天数量
                    List<CxMouldDayProductionHelper> mouldDayList = dayModPlanMap.get(lastDay);
                    isContinue = !CollectionUtils.isEmpty(mouldDayList) && mouldDayList.stream()
                            .anyMatch(p -> Objects.equals(dayPlan.getMaterialDesc(), p.getMaterialDesc()));
                }
                int mouldPlanQty = intValue(dayPlan.getProductionQty());
                if (mouldPlanQty <= 0) { // 当天没有排产量的跳过
                    continue;
                }
                firstDayMouldQty += isContinue ? 0 : 1; // 记录新增模具数
                spliceMouldQty += isContinue && dayPlan.getProductionQty() < dayVulcanizationQty - firtOneMouldQty ? 1 : 0; // 续作模具，且排产量低于产能 - 首日排产量的，记录可拼机台数量
                productionQty += mouldPlanQty;
                maxDayVulcanizationQty = Math.max(maxDayVulcanizationQty, dayVulcanizationQty);
                mouldCodeSet.add(dayPlan.getMouldCode());
            }
            // 同一天如果既有可拼硫化机数，也有新增模具，判定为拼机台，可以减掉这部分新增模具
            int deductMouldQty = Math.min(spliceMouldQty, firstDayMouldQty); // 既有可拼机台模具数，又有新增模具数时，取最小值作为拼机台排产数，这部分模具两模只能算一模，需要扣减掉
            mouldQty = mouldCodeSet.size() - deductMouldQty;
            maxPlanQty = maxMouldNum * maxDayVulcanizationQty;
            // 记录统计值
            MatchingPlanLimitHelper dayLimit = new MatchingPlanLimitHelper();
            dayLimit.setMaxPlanQty(maxPlanQty);
            dayLimit.setMaxMouldQty(maxMouldNum);
            dayLimit.setPlanQty(productionQty);
            dayLimit.setMouldQty(mouldQty);
            dayPlanMap.put(day, dayLimit);
        }
        return this.buildProductDayLimitMap(productionContext, groupInfo, allSinglePlanMap, dayModPlanMap,
                firtOneMouldQty, rate, dayPlanMap);
    }

    /**
     * 构建排产日每日限制
     *
     * @param productionContext 上下文
     * @param groupInfo         结构排产
     * @param allSinglePlanMap  需求计划列表
     * @param dayModPlanMap     按天分组硫化排产
     * @param maxMouldNum       每天最大模具数，根据日硫化产能计算而来
     * @param firtOneMouldQty   单模首日排产量
     * @param dayPlanMap        每一天的已排产量统计
     * @return key：排产日，value：排产限制
     */
    private TreeMap<Integer, MatchingPlanLimitHelper> buildProductDayLimitMap(TbrProductionContext productionContext,
                                                                              ProductionPlanGroupInfo groupInfo,
                                                                              Map<Long, MonthPlanProductionRequirePlanVo> allSinglePlanMap,
                                                                              Map<Integer, List<CxMouldDayProductionHelper>> dayModPlanMap,
                                                                              Integer firtOneMouldQty, Integer rate,
                                                                              TreeMap<Integer, MatchingPlanLimitHelper> dayPlanMap) {
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
        Integer cxNum = Optional.ofNullable(groupInfo.getAllocationCxMachineCodeSet()).map(Set::size).orElse(0); // 成型机台数
        Integer limitDay = groupInfo.getDayProductionLimitInfo().keySet().stream().max(Integer::compareTo).orElse(0);
        Integer endDay = Math.max(limitDay, dayPlanMap.lastKey()); // 结束日期，默认是结构收尾日期
        // 同结构的最大单模硫化量
        Integer dayVulcanizationQty = allSinglePlanMap.values().stream()
                .filter(m -> groupInfo.getGroupName().equals(m.getStructureName()))
                .map(MonthPlanProductionRequirePlanVo::getDayVulcanizationQty).filter(Objects::nonNull)
                .max(Integer::compareTo).orElse(0);
        // 统计每一天的已排产量
        TreeMap<Integer, MatchingPlanLimitHelper> usedPlanMap = new TreeMap<>();
        for (int day = startDay; day <= endDay; day++) {
            GroupPlanCxLhCapacityLimitHelper limist = groupInfo.getDayProductionLimitInfo().get(day);
            Integer lhNum = limist != null? limist.getMaxLhMachineCount(): BigDecimalUtils.multiply(cxNum, rate, true).intValue();
            Integer maxMouldNum = lhNum * ProductionConstant.DOUBLE_MOULD_PRODUCTION; // 换算成模具数 = 硫化机* 2（双模排产）
            MatchingPlanLimitHelper dayLimit = dayPlanMap.get(day);
            if (dayLimit == null) { // 如果为空，说明是结构预留的的天数，需要添加
                List<CxMouldDayProductionHelper> mouldDayList = dayModPlanMap.get(this.getLastDay(productionContext, day, startDay));
                boolean isContinue = !CollectionUtils.isEmpty(mouldDayList);
                BigDecimal unit = BigDecimalUtils.valueOf(isContinue ? dayVulcanizationQty : firtOneMouldQty); // 单模每日最大排产量：续作，直接按最大满产排；非续作只能按新模首日排产
                Integer maxPlanQty = BigDecimalUtils.multiply(maxMouldNum, unit).intValue(); // 最大可排产量 = 最大模具数 * 最大日硫化量
                dayLimit = new MatchingPlanLimitHelper();
                dayLimit.setMaxPlanQty(maxPlanQty);
                dayLimit.setMaxMouldQty(maxMouldNum);
                dayLimit.setMouldQty(0);
                dayLimit.setPlanQty(0);
            }
            usedPlanMap.put(day, dayLimit);
        }
        return usedPlanMap;
    }

    /**
     * 执行新增SKU搭配排产算法
     *
     * @param productionContext  上下文
     * @param materialDesc       结构
     * @param needProductionInfo 需排产物料
     * @param newSkuQtyMap       已搭配排产规格数量
     * @param groupInfo          排产计划分组
     * @param continueInfo       首日续作规格
     * @param limitMap           排产限制
     * @return 是否有新增模具
     */
    private boolean matchingScheduleNewSchedule(TbrProductionContext productionContext, String materialDesc,
                                                    SkuNeedProductionInfo needProductionInfo,
                                                    Map<String, Integer> newSkuQtyMap,
                                                    ProductionPlanGroupInfo groupInfo,
                                                    CxContinueInfoHelper continueInfo,
                                                    TreeMap<Integer, MatchingPlanLimitHelper> limitMap) {
        Integer lhMouldQty = ProductionConstant.DOUBLE_MOULD_PRODUCTION; // 硫化机模具配比
        String structureName = groupInfo.getGroupName();
        List<MonthPlanProductionRequirePlanVo> productionPlanList = needProductionInfo.getNeedProductionList();
        // 1、非实单搭配时，如果结构有设置模具分配比例，需要限制不允许超过模具分配数
        int allocationMouldNum = lhMouldQty; 
        if (!productionContext.getIsActualOrder()) {
            MouldAllocationInfoVo mouldAllocationControlInfo = productionContext
                    .getMouldAllocationInfo(CollectionUtils.firstElement(productionPlanList));
            if (mouldAllocationControlInfo != null) {
                // 获取本结构已分配模具列表
                List<ProductionMouldInfoVo> allocationMouldList = productionContext.getBaseDataContainer()
                        .getMouldInfoMap().values().stream()
                        .filter(m -> m.getDayProductionInfo().values().stream()
                                .anyMatch(planList -> planList.stream()
                                        .anyMatch(p -> structureName.equals(p.getStructureName()))))
                        .collect(Collectors.toList());
                allocationMouldNum = mouldAllocationControlInfo.getAllocationQty() - allocationMouldList.size();
            }
        }
        if (allocationMouldNum < ProductionConstant.DOUBLE_MOULD_PRODUCTION) { // 模具可分配数量小于最小上机模具数，则直接结束
            return false;
        }

        // 2、检查最早可以在哪一天开始加模
        int startDay = limitMap.firstKey();
        int endDay = limitMap.lastKey();
        MonthPlanProductionRequirePlanVo plan = CollectionUtils.firstElement(productionPlanList);
        Integer realStartDay = plan.getMatchBeginDay() != null? plan.getMatchBeginDay(): startDay; // 本次循环的开始日期，如果是续作且开始搭配日不为空，则以此为准，否则从接口开始日开始检索
        Integer realEndDay = this.getRealEndDay(productionContext, plan, realStartDay, endDay); // 本次循环的结束日期
        Integer productionQty = needProductionInfo.getSumNeedProductionQty(); // 需求量
        Integer maxProductionQty = needProductionInfo.getDayMaxProductionQty(); // 单机台硫化上限
        List<MatchingMouldDayUsedHelper> mouldDayUsedList = this.caculateMouldDayUsed(productionContext, materialDesc,
                maxProductionQty, realStartDay, realEndDay); // 统计每一天所有可用模具

        for (MatchingMouldDayUsedHelper mouldDayUsed : mouldDayUsedList) { // 遍历各模具可用列表
            Integer usedBeginDate = mouldDayUsed.getBeginDate();
            Integer usedEndDate = mouldDayUsed.getEndDate();
            List<ProductionMouldInfoVo> doubleMouldList = mouldDayUsed.getMouldInfoList();
            if (CollectionUtils.isEmpty(doubleMouldList)) {
                continue;
            }
            // 检查是否满足排产条件
            // 1、排产参数检查
            if (!this.checkProductParam(productionContext, productionPlanList, groupInfo, materialDesc, usedBeginDate, realStartDay, realEndDay)) {
                continue;
            }
            // 2、检查换模次数检查
            DayCapacityLimitHelper dayCapacityLimit = productionContext.getBaseDataContainer().getDayCapacityLimit().getDayCapacityLimitMap().get(usedBeginDate);
            if (dayCapacityLimit != null && dayCapacityLimit.getLeftOverUsedChangeMouldQty() <= 0) {
                continue;
            }
            // 3、次日是否满足排产参数检查
            Integer nextDay = this.getNextDay(productionContext, usedBeginDate, realEndDay);
            if (nextDay <= 0 || !this.checkProductParam(productionContext, productionPlanList, groupInfo, materialDesc, nextDay, realStartDay, realEndDay)) {
                continue;
            }
            // 4、检查次日的模具是否满足条件续作
            if (!this.checkCanAddMould(materialDesc, doubleMouldList, nextDay, limitMap)) {
                continue;
            }
            // 5、检查不超过最大硫化机数
            // 5.1、检查当天一天不能超过最大硫化机数
            if (!this.checkLhMachineCount(productionContext, groupInfo, usedBeginDate)) {
                continue;
            }
            // 5.2、检查下一天也不能超过最大硫化机数
            if (nextDay > 0 && !this.checkLhMachineCount(productionContext, groupInfo, nextDay)) {
                continue;
            }
            // 6、检查下一天是否还有量，如果需要补量则不需要检查
            boolean isBoost = this.checkIsBoost(productionContext, usedBeginDate, realEndDay, productionPlanList.get(0)); 
            if (!isBoost && productionQty <= productionContext.getBaseDataContainer().getParamConfiguration().getChangeMouldFirstQty()) {
                continue; // 如果剩余排产量不足首日排产量，则结束
            }
            
            // 根据剩余可排模具限制模具数量
            MatchingPlanLimitHelper limitHelper = limitMap.get(usedBeginDate);
            Integer newMouldNum = limitHelper.getMaxMouldQty() - limitHelper.getMouldQty(); // 可新增模具数
            newMouldNum = BigDecimalUtils.least(newMouldNum, allocationMouldNum, lhMouldQty).intValue(); // 一次最多新增一台硫化机

            List<ProductionMouldInfoVo> newDoubleMouldList = new ArrayList<>(); // 新上模具
            if (newMouldNum > 0) { // 可新增模具
                List<ProductionMouldInfoVo> twoMouldList = new ArrayList<>(); // 一次添加双模
                for (ProductionMouldInfoVo mould : doubleMouldList) {
                    if (newMouldNum == 0) {
                        break;
                    }
                    List<CxMouldDayProductionHelper> dayProductionList = mould.getDayProductionInfo().get(usedBeginDate);
                    if (CollectionUtils.isEmpty(dayProductionList)) { // 当天没有排产才添加模具，一次加两幅
                        twoMouldList.add(mould);
                        newMouldNum--;
                    }
                    if (twoMouldList.size() == ProductionConstant.DOUBLE_MOULD_PRODUCTION) { // 凑够双模才添加
                        newDoubleMouldList.addAll(twoMouldList);
                        twoMouldList.clear();
                        break;
                    }
                }
            }

            // 新模排产
            if (!CollectionUtils.isEmpty(newDoubleMouldList)) {
                // 排产量
                Integer sumProductionQty = (productionQty & 1) == 0 ? productionQty : productionQty + 1; // 处理奇数，遇到奇数直接+1;

                Integer dayMaxProductionQty = this.getRealDayMaxProductionQty(productionContext, usedBeginDate, needProductionInfo.getDayMaxProductionQty()); // 获取实际日产量
                Integer realSumProductionQty = newSkuQtyMap.getOrDefault(materialDesc, 0); // 已排产量
                Set<String> cxMachineInfoSet = groupInfo.getAllocationCxMachineCodeSet();
                Integer lastDay = this.getLastDay(productionContext, usedBeginDate, realStartDay);
                // 查找模具上一天的生产计划，并构建硫化分组
                CxLhProductionHelper cxLhGroup = CxLhProductionHelper.createEmptyLhGroup(groupInfo.getGroupName(),
                        1, cxMachineInfoSet);
                for (ProductionMouldInfoVo mould : newDoubleMouldList) {
                    List<CxMouldDayProductionHelper> latestPlanList = mould.getDayProductionInfo().get(lastDay); // 上一天计划
                    CxMouldDayProductionHelper production = CollectionUtils.firstElement(latestPlanList);
                    if (production != null) {
                        BeforeSkuProductionInfo beforeSku = BeforeSkuProductionInfo.createBySku(production.getMaterialDesc(), production.getMaterialCode()
                                , null, BigDecimal.ZERO.intValue(), dayMaxProductionQty, Collections.singleton(mould.getMouldCode()));
                        cxLhGroup.setBeforeSku(beforeSku);
                    }
                }

                // 走新模排产逻辑
                LhProductionQtyHelper lhProductionQtyHelper = new LhProductionQtyHelper(groupInfo, cxMachineInfoSet,
                        cxLhGroup, sumProductionQty, realSumProductionQty, dayMaxProductionQty);
                CxLhMouldProductionCalculator.lhProductionByGroupHandler(productionContext, lhProductionQtyHelper,
                        usedBeginDate, usedEndDate, newDoubleMouldList, needProductionInfo.getNeedProductionList(),
                        ContinueTypeEnum.NO_CONTINUE);
                Integer realProductionQty = lhProductionQtyHelper.getRealSumProductionQty() - realSumProductionQty;
                if (realProductionQty > 0) {
                    Set<String> useMouldSet = newDoubleMouldList.stream().map(ProductionMouldInfoVo::getMouldCode).distinct().collect(Collectors.toSet());
                    String scheduleLogName = productionContext.getIsActualOrder()? "实单补量": "搭配排产";
                    this.addTempLog(productionContext, String.format("结构:%s,【%s】,%s日,规格:%s,新增模具%s,排产量:%s", structureName, scheduleLogName, usedBeginDate, materialDesc, useMouldSet, realProductionQty));
                    // 更新换模数
                    Set<String> mouldCodeSet = newDoubleMouldList.stream().map(ProductionMouldInfoVo::getMouldCode).distinct().collect(Collectors.toSet());
                    productionContext.getBaseDataContainer().getDayCapacityLimit().addChangeMouldUsedQty(productionContext, usedBeginDate, materialDesc, mouldCodeSet);
                    this.updatePlanRemainQty(productionPlanList, realProductionQty); // 更新需求计划的未排产量
                    newSkuQtyMap.put(materialDesc, lhProductionQtyHelper.getRealSumProductionQty()); // 累计已排量
                    this.updateMatchDay(productionPlanList, usedBeginDate); // 更新搭配日期
                    // 更新模具与排产量的累计量
                    limitHelper.setMouldQty(limitHelper.getMouldQty() + newDoubleMouldList.size());
                    limitHelper.setPlanQty(limitHelper.getPlanQty() + realProductionQty);
                    String mainPattern = CollectionUtils.firstElement(productionPlanList).getMainPattern(); // 主花纹
                    groupInfo.reCalcMpDailyCapacityLimitByDay(productionContext, usedBeginDate, mainPattern); // 重新计算统计产能
                    return true; // 新增模具后直接结束，后面走续作逻辑
                }
            }
        }
        return false;
    }

    /**
     * 检查硫化机台数
     * 
     * @param productionContext  上下文
     * @param groupInfo          结构信息
     * @param mouldProductionMap 每日模具排产记录
     * @param 检查日期
     * @return
     */
    private boolean checkLhMachineCount(TbrProductionContext productionContext, ProductionPlanGroupInfo groupInfo,
                                        Integer day) {
        // 检查是否超结构最大硫化机数
        MpDailyCapacityLimitVo dayLimitVo = groupInfo.getDailyCapacityLimitVoMap().get(day);
        GroupPlanCxLhCapacityLimitHelper limit = groupInfo.getDayProductionLimitInfo().get(day);
        if (limit != null && dayLimitVo != null) {
            Integer usedLhMachines = intValue(dayLimitVo.getUsedLhMachines());
            Integer maxLhMachines = intValue(limit.getMaxLhMachineCount());
            if (usedLhMachines >= maxLhMachines) {
                return false;
            }
        }
        // 检查是否超总硫化机数量
        Integer usedLhMachines = productionContext.getGroupProductionInfo().values().stream()
                .map(g -> g.getDailyCapacityLimitVoMap().get(day)).filter(Objects::nonNull)
                .mapToInt(MpDailyCapacityLimitVo::getUsedLhMachines).sum();
        Integer maxLhMachines = productionContext.getBaseDataContainer().getLhMachineInfoList().size();
        return usedLhMachines < maxLhMachines;
    }

    /**
     * 检查SKU是否还可以加模生产
     * @param materialDesc
     * @param doubleMouldList
     * @param day
     * @param limitMap
     * @return
     */
    private boolean checkCanAddMould(String materialDesc, List<ProductionMouldInfoVo> doubleMouldList, Integer day,
                                     TreeMap<Integer, MatchingPlanLimitHelper> limitMap) {
        MatchingPlanLimitHelper nextDayLimitHelper = limitMap.get(day);
        if (nextDayLimitHelper.getMaxMouldQty() <= nextDayLimitHelper.getMouldQty()) {
            // 判断下一天如果没有可增加模具的空间，跳过
            return false;
        }
        Integer canUseMouldCount = 0;
        for (ProductionMouldInfoVo mouldInfo : doubleMouldList) {
            if (this.checkMouldCanProductMaterial(mouldInfo, day, materialDesc)) { // 模具有排产本规格，模具数+1
                canUseMouldCount ++;
            }
            if (canUseMouldCount >= ProductionConstant.DOUBLE_MOULD_PRODUCTION) { // 只要满足双模，则说明可
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否可排产
     *
     * @param productionContext 上下文
     * @param requireList       本次排产列表
     * @param groupInfo         排产结构
     * @param day               排产日
     * @return
     */
    private boolean checkProductParam(TbrProductionContext productionContext,
                                      List<MonthPlanProductionRequirePlanVo> requireList,
                                      ProductionPlanGroupInfo groupInfo, String materialDesc,
                                      Integer day, Integer beginDay, Integer endDay) {
        MonthPlanProductionRequirePlanVo firstPlan = CollectionUtils.firstElement(requireList);
        // 1、统计当天的总排产量，如果已经达到最大排产量则直接取消
        if (!this.checkRealProductQty(productionContext, day)) {
            return false;
        }
        // 2、检查胎胚种类数
        // 统计当天的已排产胎胚
        MpDailyCapacityLimitVo dayLimitVo = groupInfo.getDailyCapacityLimitVoMap().get(day);
        if (dayLimitVo != null) {
            Set<String> embryoCodeSet = dayLimitVo.getEmbryoCodes();
            String embryoCode = firstPlan.getMainMaterialDesc();
            if (!embryoCodeSet.contains(embryoCode)) {
                GroupPlanCxLhCapacityLimitHelper limit = groupInfo.getDayProductionLimitInfo().get(day);
                if (limit != null && limit.getMaxEmbryoCodeCount() != null
                        && embryoCodeSet.size() >= limit.getMaxEmbryoCodeCount()) {
                    return false; // 已经达到最大胎胚数，跳过
                }
            }
        }
        // 3、判断品牌是OEM则需要控制总量
        if (DayCapacityLimitHelper.checkIsOemBrand(productionContext, firstPlan.getBrand())) {
            DayCapacityLimitVo changeMouldLimitHandler = productionContext.getBaseDataContainer().getDayCapacityLimit(); // 每日产能限制
            Set<Integer> oemBrandSet = changeMouldLimitHandler.getEnableOemBrandProductionRange(productionContext,
                    firstPlan); // 获取是否还有可排产日，如果没有了就说明已经达到上限
            if (CollectionUtils.isEmpty(oemBrandSet)) {
                return false;
            }

        }
        // 4、检查是否二次上机
        if (!this.checkIsSecOnlineSku(productionContext, materialDesc, day, beginDay, endDay)) {
            return false;
        }
        return true;
    }

    /**
     * 检查如果SKU是否超出二次排产限制
     * 
     * @param productionContext 上下文
     * @param materialDesc      检查SKU
     * @param day               排产日期
     * @param beginDay          结构排产开始日
     * @param endDay            结构排产结束日
     * @return
     */
    private boolean checkIsSecOnlineSku(TbrProductionContext productionContext, String materialDesc, Integer day,
                                             Integer beginDay, Integer endDay) {
        boolean isSecOnLine = true;
        int skuSecondProduction = productionContext.getBaseDataContainer().getParamConfiguration()
                .getSkuSecondProduction();
        // 2、先向前看是否有超出二次上机限制
        Integer dayCount = 0;
        for (Integer i = day - 1; i >= beginDay; i--) {
            if (i == 0) {
                break;
            }
            if (productionContext.getStopDays().contains(i)) {
                continue;
            }
            dayCount ++;
            Integer checkDayProductQty = this.sumMaterialProductQty(productionContext, materialDesc, i);
            if (checkDayProductQty > 0) { // 如果有排产，则检查是否超过限制
                isSecOnLine = dayCount == 1 || dayCount >= skuSecondProduction || i == beginDay;
                break;
            }
        }
        if (!isSecOnLine) { // 校验不通过，则直接结束
            return isSecOnLine;
        }
        // 3、再向后看是否有超出二次上机限制
        dayCount = 0;
        for (Integer i = day + 1; i <= endDay; i++) {
            if (productionContext.getStopDays().contains(i)) {
                continue;
            }
            dayCount ++;
            Integer checkDayProductQty = this.sumMaterialProductQty(productionContext, materialDesc, i);
            if (checkDayProductQty > 0) { // 如果有排产，则检查是否超过限制
                isSecOnLine = dayCount == 1 || dayCount >= skuSecondProduction || i == endDay;
                break;
            }
        }
        return isSecOnLine;
    }

    /**
     * 汇总物料日产量
     * 
     * @param productionContext 上下文
     * @param materialDesc      物料描述
     * @param day               排产日
     * @return
     */
    private Integer sumMaterialProductQty(TbrProductionContext productionContext, String materialDesc, Integer day) {
        Integer dayTotalProductQty = productionContext.getGroupProductionInfo().values().stream().mapToInt(groupInfo -> {
            GroupPlanCxLhCapacityLimitHelper capacityLimitHelper = groupInfo.getDayProductionLimitInfo().get(day);
            if (capacityLimitHelper == null) {
                return 0;
            }
            Map<String, SkuDayProductionInfoHelper> productionSkuQtyMap = capacityLimitHelper.getProductionSkuQtyInfo();
            if (PubUtil.isEmpty(productionSkuQtyMap)) {
                return 0;
            }
            SkuDayProductionInfoHelper skuProductionInfo = productionSkuQtyMap.get(materialDesc);
            if (skuProductionInfo == null) {
                return 0;
            }
            return intValue(skuProductionInfo.getSumProductionQty());
        }).sum();
        return dayTotalProductQty;
    }

    /**
     * 获取实际的排产数量，需要受每日最大排产量控制
     *
     * @param productionContext 上下文
     * @param dayMaxCapacity    最大产能
     * @param day               排产日
     * @return
     */
    private boolean checkRealProductQty(TbrProductionContext productionContext, Integer day) {
        Integer dayMaxCapacity = productionContext.getBaseDataContainer().getParamConfiguration().getDayMaxCapacity(); // 每日最大产能
        // 统计当天总已排产量
        Integer dayTotalProductQty = productionContext.getBaseDataContainer().getMouldInfoMap().values().stream()
                .mapToInt(m -> {
                    List<CxMouldDayProductionHelper> dayPlan = m.getDayProductionInfo().get(day);
                    if (CollectionUtils.isEmpty(dayPlan)) {
                        return 0;
                    }
                    return dayPlan.stream().mapToInt(CxMouldDayProductionHelper::getProductionQty).sum();
                }).sum();
        // 计算实际产能，例如开产日需要乘比例
        Integer realDayMaxCapacity = dayMaxCapacity;
        Integer rate = productionContext.getCapacityRatioMap().get(day); // 排产比例
        if (rate != null) { // 如果有设置开产比例，需要给日产能打折 = 产能 * 比例
            realDayMaxCapacity = BigDecimalUtils.multiply(dayMaxCapacity, BigDecimalUtils.percentages2Decimals(rate))
                    .setScale(0, RoundingMode.DOWN).intValue();
        }
        return realDayMaxCapacity > dayTotalProductQty; // 如果还有产能剩余，允许排产
    }

    /**
     * 根据日产比例计算实际可用产能
     *
     * @param dayMaxProductionQty 最大产能
     * @param rate                产能比例
     * @return
     */
    private Integer getRealDayMaxProductionQty(TbrProductionContext productionContext, Integer day,
                                               Integer dayMaxProductionQty) {
        Integer rate = productionContext.getCapacityRatioMap().get(day);
        if (rate == null) {
            return dayMaxProductionQty;
        }
        // 如果有设置开产比例，需要给日产能打折 = 产能 * 比例
        BigDecimal tempDayVulcanizationQty = BigDecimalUtils.multiply(dayMaxProductionQty,
                BigDecimalUtils.percentages2Decimals(rate));
        tempDayVulcanizationQty = tempDayVulcanizationQty.setScale(0, RoundingMode.DOWN); // 小数部分向下取整，但是至少一台
        Integer realDayVulcanizationQty = tempDayVulcanizationQty.intValue();
        if (realDayVulcanizationQty <= 0) {
            realDayVulcanizationQty = ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        }
        return realDayVulcanizationQty;
    }

    /**
     * 模具排序，按今天或上一天有排本规格的模具优先，其次已排产量高的优先
     *
     * @param materialDesc 规格描述
     * @param day          当天
     * @param lastDay      前一天
     * @param mould1       模具1
     * @param mould2       模具2
     * @return
     */
    private int usedMouldCompare(String materialDesc, Integer day, Integer lastDay,
                                 ProductionMouldInfoVo mould1, ProductionMouldInfoVo mould2) {
        // 1、校验当天模具已排规格，有同规格的优先
        int result = this.usedMouldSameMaterialComparator(mould1, mould2, materialDesc, day);
        if (result != 0) {
            return result;
        }
        // 2、校验前一天模具已排规格，有同规格的优先
        result = this.usedMouldSameMaterialComparator(mould1, mould2, materialDesc, lastDay);
        if (result != 0) {
            return result;
        }
        // 3、当天已排量高的优先
        result = this.usedMouldProductionQtyComparator(mould1, mould2, day);
        if (result != 0) {
            return result;
        }
        // 4、模具号排序
        result = mould1.getMouldCode().compareTo(mould2.getMouldCode());
        return result;
    }

    /**
     * 模具已排产量比较器，高的优先
     * 
     * @param mould1 模具1
     * @param mould2 模具2
     * @param day    排产日
     * @return
     */
    private int usedMouldProductionQtyComparator(ProductionMouldInfoVo mould1, ProductionMouldInfoVo mould2,
                                                 Integer day) {
        List<CxMouldDayProductionHelper> dayProduction1 = mould1.getDayProductionInfo().get(day);
        List<CxMouldDayProductionHelper> dayProduction2 = mould2.getDayProductionInfo().get(day);
        Integer productQty1 = 0;
        if (!CollectionUtils.isEmpty(dayProduction1)) {
            productQty1 = dayProduction1.stream().mapToInt(CxMouldDayProductionHelper::getProductionQty).sum();
        }
        Integer productQty2 = 0;
        if (!CollectionUtils.isEmpty(dayProduction1)) {
            productQty2 = dayProduction2.stream().mapToInt(CxMouldDayProductionHelper::getProductionQty).sum();
        }
        return productQty2.compareTo(productQty1); // 已排产量高的优先
    }

    /**
     * 模具已排规格匹配度比较器，高的优先
     * 
     * @param mould1       模具1
     * @param mould2       模具2
     * @param materialDesc 排产规格
     * @param day          排产日
     * @return
     */
    private int usedMouldSameMaterialComparator(ProductionMouldInfoVo mould1, ProductionMouldInfoVo mould2,
                                                String materialDesc, Integer day) {
        List<CxMouldDayProductionHelper> dayProduction1 = mould1.getDayProductionInfo().get(day);
        List<CxMouldDayProductionHelper> dayProduction2 = mould2.getDayProductionInfo().get(day);
        Boolean sameMaterial1 = dayProduction1 != null
                && dayProduction1.stream().anyMatch(s -> Objects.equals(materialDesc, s.getMaterialDesc()));
        Boolean sameMaterial2 = dayProduction2 != null
                && dayProduction2.stream().anyMatch(s -> Objects.equals(materialDesc, s.getMaterialDesc()));
        return sameMaterial2.compareTo(sameMaterial1); // boolean是true比false大，因此需要倒序
    }

    /**
     * 非首日续作排程算法
     *
     * @param productionContext  上下文
     * @param materialDesc       排产规格描述
     * @param newSkuQtyMap       搭配量统计列表
     * @param groupInfo          结构信息
     * @param productionPlanList 同SK需求计划列表
     * @param productionQty      剩余可排产量
     * @param isBoost            是否收尾补量
     * @param maxProductionQty   日硫化量
     * @param beginDay           结构排产开始日期
     * @param endDay             结构排产结束日期
     * @param continueMouldList  本次需要排产的模具
     */
    private Integer matchingScheduleNextDayContinue(TbrProductionContext productionContext, String materialDesc,
                                                    Map<String, Integer> newSkuQtyMap,
                                                    ProductionPlanGroupInfo groupInfo,
                                                    List<MonthPlanProductionRequirePlanVo> productionPlanList,
                                                    Integer productionQty, Boolean isBoost, Integer maxProductionQty,
                                                    Integer beginDay, Integer endDay,
                                                    List<ProductionMouldInfoVo> continueMouldList) {
        // 续作规格排产
        if (CollectionUtils.isEmpty(continueMouldList)) {
            return 0;
        }
        Integer lhMouldQty = ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        Integer sumProductionQty = productionQty;
        // 构建续作规格对象
        CxContinueSkuInfoHelper continueSkuInfo = CxContinueSkuInfoHelper.buildContinueProductInfo(materialDesc,
                productionPlanList, new HashMap<>());
        continueSkuInfo.setOnLineCxMachineSet(groupInfo.getAllocationCxMachineCodeSet());
        continueSkuInfo.setDayVulcanizationQty(maxProductionQty);
        continueSkuInfo.setContinueSkuPlanList(productionPlanList);
        Integer totalProductionQty = 0;
        for (int productionDay = beginDay; productionDay <= endDay; productionDay++) { // 按顺序每天续作排产
            // 检查当天模具是否有排产，如果有则需要从模具已排量补到上限
            Integer checkDay = productionDay;
            Map<Integer, List<ProductionMouldInfoVo>> capacityMap = this.getMoldRemainingCapacity(continueMouldList,
                    maxProductionQty, checkDay); // 计算所有模具当天的产能剩余量
            for (Entry<Integer, List<ProductionMouldInfoVo>> entrySet : capacityMap.entrySet()) {
                Integer capacityQty = entrySet.getKey(); // 产能剩余量
                List<ProductionMouldInfoVo> remainMouldList = entrySet.getValue(); // 相同剩余产能的模具
                if (remainMouldList.size() < lhMouldQty) { // 有模具，但是不足最低上模数的跳过
                    continue;
                }
                // 模具数换算成机台数
                Integer lhQty = remainMouldList.size() / lhMouldQty;
                Integer realProductionQty = isBoost? capacityQty: NumberUtils.min(sumProductionQty, capacityQty, maxProductionQty); // 收尾补量，直接按剩余产能排产；其余情况取计划量、产能剩余量、最大排产量的最小值
                if (realProductionQty <= 0) {
                    continue;
                }
                realProductionQty = (realProductionQty & 1) == 0 ? realProductionQty : realProductionQty + 1; // 处理奇数，遇到奇数直接+1
                if (!isBoost) {
                    CxLhMouldProductionCalculator.continueSkuLhProductionHandler(productionContext, groupInfo,
                            continueSkuInfo, productionDay, realProductionQty, remainMouldList);
                } else {
                    Set<String> cxMachineInfoSet = groupInfo.getAllocationCxMachineCodeSet();
                    Integer lastDay = this.getLastDay(productionContext, productionDay, 1); // 上一天
                    CxLhProductionHelper cxLhGroup = CxLhProductionHelper.createEmptyLhGroup(groupInfo.getGroupName(),
                            1, cxMachineInfoSet);
                    for (ProductionMouldInfoVo mould : remainMouldList) {
                        List<CxMouldDayProductionHelper> latestPlanList = mould.getDayProductionInfo().get(lastDay); // 上一天计划
                        CxMouldDayProductionHelper production = CollectionUtils.firstElement(latestPlanList);
                        if (production != null) {
                            BeforeSkuProductionInfo beforeSku = BeforeSkuProductionInfo.createBySku(production.getMaterialDesc(), production.getMaterialCode()
                                    , null, BigDecimal.ZERO.intValue(), capacityQty, Collections.singleton(mould.getMouldCode()));
                            cxLhGroup.setBeforeSku(beforeSku);
                        }
                    }
                    LhProductionQtyHelper lhProductionQtyHelper = new LhProductionQtyHelper(groupInfo, cxMachineInfoSet,
                            cxLhGroup, sumProductionQty, realProductionQty, capacityQty);
                    CxLhMouldProductionCalculator.lhProductionByGroupHandler(productionContext, lhProductionQtyHelper,
                            beginDay, endDay, remainMouldList, productionPlanList, ContinueTypeEnum.SAME_SKU);
                }
                realProductionQty *= lhQty; // 合计计划量时，要乘上硫化机数
                newSkuQtyMap.put(materialDesc, newSkuQtyMap.getOrDefault(materialDesc, 0) + realProductionQty); // 累计已排量
                sumProductionQty -= realProductionQty; // 待排量扣减已排量，要乘上硫化机数
                totalProductionQty += realProductionQty;
                String scheduleLogName = productionContext.getIsActualOrder()? "实单补量": "搭配排产";
                this.addTempLog(productionContext, String.format("结构:%s,【%s】,%s日,规格:%s,模具续作,排产量:%s", groupInfo.getGroupName(), scheduleLogName, beginDay, materialDesc, realProductionQty));
            }
        }
        return totalProductionQty;
    }
    
    /**
     * 保存特殊材料列表
     * @param productionContext
     */
    public void saveSpecialMaterialResult(TbrProductionContext productionContext) {
        List<SpecialMaterialResult> specialMaterialList = this.buildSpecialMaterialResultList(productionContext);
        baseDao.saveBatch(specialMaterialList);
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
                                                                               Integer dayVulcanizationQty,
                                                                               Integer checkDay) {
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
                                                                  String materialDesc, Integer dayVulcanizationQty,
                                                                  int startDay, int endDay) {
        List<MatchingMouldDayUsedHelper> mouldDayUsedList = new ArrayList<>();
        Integer dayMoldQty = dayVulcanizationQty / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        // 遍历每一天的可用模具，与前一天可用模具相同的日期分作一组，然后按组遍历排产
        for (int day = startDay; day <= endDay; day++) {
            if (productionContext.getStopDays().contains(day)) { // 跳过停产日
                continue;
            }

            // 根据日产能比例计算实际日产能
            Integer realDayMoldQty = dayMoldQty;
            Integer realDayVulcanizationQty = this.getRealDayMaxProductionQty(productionContext, day,
                    dayVulcanizationQty);
            if (realDayVulcanizationQty.compareTo(dayVulcanizationQty) != 0) { // 如果日产能有发生变化，计算实际日单模产量
                realDayMoldQty = realDayVulcanizationQty / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
            }
            List<ProductionMouldInfoVo> canUseMould = this.selectedAllMouldByDay(productionContext, materialDesc,
                    realDayMoldQty, day);
            if (!CollectionUtils.isEmpty(canUseMould)) {
                mouldDayUsedList.add(new MatchingMouldDayUsedHelper(canUseMould, day, day, realDayVulcanizationQty)); // 记录可用时间段
            }
        }
        return mouldDayUsedList;
    }

    /**
     * 构建排产结果并保存
     *
     * @param productionContext 上下文
     * @param resultList        原排产结果
     * @param detailList        原排产明细
     * @param factProdReqMap    实单补量
     * @param matchingQtyMap    搭配补量
     */
    @Transactional
    public void saveMouldProductionResult(TbrProductionContext productionContext,
                                          List<FactoryMonthPlanMouldDayResult> resultList,
                                          List<FactoryMonthPlanMouldDayDetail> detailList,
                                          Map<String, Integer> factProdReqMap, Map<String, Integer> matchingQtyMap) {
        // 保存搭配调整后的排产记录
        if (!CollectionUtils.isEmpty(matchingQtyMap) || !CollectionUtils.isEmpty(factProdReqMap)) {
            // 1、从上下文取出排产结果
            List<FactoryMonthPlanMouldDayDetail> detailLogList = MouldProductionResultHandler
                    .getMouldProductionResult(productionContext).stream()
                    .filter(detail -> matchingQtyMap.containsKey(detail.getMaterialDesc())
                            || factProdReqMap.containsKey(detail.getMaterialDesc())) // 只过滤出本次涉及排产的规格
                    .collect(Collectors.toList());
            List<FactoryMonthPlanMouldDayResult> dayResultList = MouldProductionResultHandler
                    .getSummaryBySkuResult(detailLogList, productionContext);
            if (!CollectionUtils.isEmpty(dayResultList)) {
                // 2、构建待保存的排产结果
                List<FactoryMonthPlanMouldDayResult> mouldResultList = this.buildMouldResultList(productionContext,
                        dayResultList, resultList, factProdReqMap, matchingQtyMap);
                List<FactoryMonthPlanMouldDayDetail> detailResultList = this.buildDetailResultList(detailLogList, detailList,
                        productionContext, factProdReqMap, matchingQtyMap);
                baseDao.saveBatch(detailResultList);
                baseDao.saveBatch(mouldResultList);
            }
        }
        // 保存统计信息以及特殊材料排产记录
        List<MpMonthPlanStatistics> statisticsList = this.buildProductionStatisticsList(productionContext); // 排产统计信息
        log.info(productionContext.getTempLogBuilder().toString());
        
        baseDao.saveBatch(statisticsList);
        this.saveSpecialMaterialResult(productionContext);
//        this.updateMatchingProductionLog(productionContext); // 更新排产日志
    }

    /**
     * 构建特殊材料排产记录
     * @param productionContext 上下文
     * @return
     */
    private List<SpecialMaterialResult> buildSpecialMaterialResultList(TbrProductionContext productionContext) {
        Map<String, Map<Long, SpecialMaterialInfoVo>> specialMaterialInfoMap = productionContext.getSpecialMaterialInfoMap();
        List<SpecialMaterialResult> specialMaterialList = new ArrayList<>();
        for (Map<Long, SpecialMaterialInfoVo> stockMap: specialMaterialInfoMap.values()) {
            for (SpecialMaterialInfoVo stock: stockMap.values()) {
                Long totalQty = longValue(stock.getSumSkuAllocateQty());
                if (totalQty <= 0) {
                    continue;
                }
                if (StringUtils.isEmpty(stock.getMaterialDesc())) {
                    continue;
                }
                SpecialMaterialResult result = new SpecialMaterialResult();
                result.setFactoryCode(productionContext.getFactoryCode());
                result.setMonthPlanVersion(productionContext.getMonthPlanVersion());
                result.setProductionVersion(productionContext.getProductionVersion());
                result.setMaterialCode(stock.getMaterialCode());
                result.setMaterialDesc(stock.getMaterialDesc());
                result.setStandardLength(stock.getStandardLength());
                result.setOriStandardLength(stock.getOriStandardLength());
                result.setTotalQty(totalQty);
                specialMaterialList.add(result);
            }
        }
        return specialMaterialList;
    }

    /**
     * 构建排产统计信息
     *
     * @param productionContext 上下文
     * @return
     */
    private List<MpMonthPlanStatistics> buildProductionStatisticsList(TbrProductionContext productionContext) {
        // 1、加载本次版本已生成的统计记录
        LambdaQueryWrapper<MpMonthPlanStatistics> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MpMonthPlanStatistics::getFactoryCode, productionContext.getFactoryCode());
        queryWrapper.eq(MpMonthPlanStatistics::getIsDelete, YesOrNoEnum.NO.getValue());
        queryWrapper.eq(MpMonthPlanStatistics::getProductionVersion, productionContext.getProductionVersion());
        List<MpMonthPlanStatistics> oldProductionStatisticsList = mpMonthPlanStatisticsEntityMapper
                .selectList(queryWrapper);
        Map<String, MpMonthPlanStatistics> oldProductionStatisticsMap = oldProductionStatisticsList.stream()
                .filter(s -> StringUtils.isNoneEmpty(s.getStructureName())).collect(
                        Collectors.toMap(MpMonthPlanStatistics::getStructureName, Function.identity(), (s1, s2) -> s1));

        Map<String, SkuDayProductionInfoHelper> skuDayProductionInfoHelperMap;
        // 2、根据结构取出本次需要保存的统计信息
        List<MpMonthPlanStatistics> productionStatisticsList = new ArrayList<>();
        // 2.1、遍历所有结构
        for (Entry<String, ProductionPlanGroupInfo> entry: productionContext.getGroupProductionInfo().entrySet()) {
            String structureName = entry.getKey();
            ProductionPlanGroupInfo groupPlanInfo = entry.getValue();
            groupPlanInfo.reCalcMpDailyCapacityLimit(productionContext); // 重算结构的统计数据
            Map<Integer, MpDailyCapacityLimitVo> daylyCapacityLimitMap = groupPlanInfo.getDailyCapacityLimitVoMap();
            if (daylyCapacityLimitMap == null) {
                continue;
            }
            // 2.1.1、取出已有的各结构统计信息，没有则新增
            MpMonthPlanStatistics statistics = oldProductionStatisticsMap.get(structureName);
            if (statistics == null) {
                statistics = new MpMonthPlanStatistics();
                statistics.setFactoryCode(productionContext.getFactoryCode());
                statistics.setYear(productionContext.getYear());
                statistics.setMonth(productionContext.getMonth());
                statistics.setYearMonth(productionContext.getFullYearAndMonth());
                statistics.setMonthPlanVersion(productionContext.getMonthPlanVersion());
                statistics.setProductionVersion(productionContext.getProductionVersion());
                statistics.setLastMonthPlanVersion(productionContext.getMonthPlanVersion());
                MonthPlanProductionRequirePlanVo singlePlan = groupPlanInfo.getGroupPlanData().get(BigDecimal.ZERO.intValue());
                statistics.setProSize(singlePlan.getProSize());
                statistics.setStructureType(singlePlan.getStructureType());
                statistics.setProductTypeCode(singlePlan.getProductTypeCode());
                statistics.setStructureName(structureName);
                oldProductionStatisticsMap.put(structureName, statistics);
            }
            statistics.setBaseVale(statistics.getId());
            int totalQty,totalOemQty;
            // 2.1.2、遍历日排产限制
            for (Integer day: daylyCapacityLimitMap.keySet()) {
                MpDailyCapacityLimitVo daylyCapacityLimit = daylyCapacityLimitMap.get(day);
                if (daylyCapacityLimit == null) {
                    continue;
                }
                // 2.1.2.2、构建当天的产能统计
                MpDayProductionStatisticsDetailVo statisticsDetailVo = new MpDayProductionStatisticsDetailVo();
                statisticsDetailVo.setEmbryoCount(daylyCapacityLimit.getEmbryoCodes().size());
                statisticsDetailVo.setLhMachines(daylyCapacityLimit.getUsedLhMachines());
                statisticsDetailVo.setChangeMould(daylyCapacityLimit.getUsedChangeMould());
                skuDayProductionInfoHelperMap = groupPlanInfo.getDayProductionLimitInfo().get(day).getProductionSkuQtyInfo();
                totalQty = 0;
                totalOemQty = 0;
                for (Entry<String, SkuDayProductionInfoHelper> entrySku : skuDayProductionInfoHelperMap.entrySet()){
                    totalQty += entrySku.getValue().getSumProductionQty();
                    if (DayCapacityLimitHelper.checkIsOemBrand(productionContext, entrySku.getValue().getBrand())){
                        totalOemQty += entrySku.getValue().getSumProductionQty();
                    }
                }
                statisticsDetailVo.setTotalQty(totalQty);
                statisticsDetailVo.setOemQty(totalOemQty);
                statistics.setFieldValueByFieldName(FactoryConstant.DAY_FIELD + day, JSON.toJSONString(statisticsDetailVo));
            }
            productionStatisticsList.add(statistics);
        }
        return productionStatisticsList;
    }

    /**
     * 初始化换模次数
     *
     * @param productionContext
     */
    private void initChangeMouldUsedQty(TbrProductionContext productionContext) {
        // 1、将模具排产记录按天 -> 规格 -> 模具排产记录的维度统计
        Map<Integer, Map<String, List<CxMouldDayProductionHelper>>> mouldDayproductMap = new HashMap<>();
        productionContext.getBaseDataContainer().getMouldInfoMap().values().stream().forEach(mouldInfo -> {
            for (Entry<Integer, List<CxMouldDayProductionHelper>> entry : mouldInfo.getDayProductionInfo().entrySet()) {
                Integer day = entry.getKey();
                Map<String, List<CxMouldDayProductionHelper>> materialDayProductionInfoMap = entry.getValue().stream().collect(Collectors.groupingBy(CxMouldDayProductionHelper::getMaterialDesc));
                for (Entry<String, List<CxMouldDayProductionHelper>> materialEntry : materialDayProductionInfoMap.entrySet()) {
                    String materialDesc = materialEntry.getKey();
                    for (CxMouldDayProductionHelper dayProduction : materialEntry.getValue()) {
                        Map<String, List<CxMouldDayProductionHelper>> targetMaterialDayProductionInfoMap = mouldDayproductMap.get(day);
                        if (targetMaterialDayProductionInfoMap == null) {
                            targetMaterialDayProductionInfoMap = new HashMap<>();
                            mouldDayproductMap.put(day, targetMaterialDayProductionInfoMap);
                        }
                        List<CxMouldDayProductionHelper> targetDayProductionInfoList = targetMaterialDayProductionInfoMap.get(materialDesc);
                        if (targetDayProductionInfoList == null) {
                            targetDayProductionInfoList = new ArrayList<>();
                            targetMaterialDayProductionInfoMap.put(materialDesc, targetDayProductionInfoList);
                        }
                        targetDayProductionInfoList.add(dayProduction);
                    }
                }
            }
        });

        // 2、遍历统计好的每一天每一个规格的模具排产记录，根据排产量、上一天排产记录等因素确认当天是否发生了换模
        Integer changeMouldFirstQty = productionContext.getBaseDataContainer().getParamConfiguration()
                .getChangeMouldFirstQty() / ProductionConstant.DOUBLE_MOULD_PRODUCTION; // 首日排产数
        Map<String, ProductionMouldInfoVo> mouldInfoMap = productionContext.getBaseDataContainer().getMouldInfoMap();
        DayCapacityLimitVo changeMouldLimitHandler = productionContext.getBaseDataContainer().getDayCapacityLimit(); // 每日产能限制
        for (Entry<Integer, Map<String, List<CxMouldDayProductionHelper>>> entry : mouldDayproductMap.entrySet()) {
            Integer day = entry.getKey();
            Integer lastDay = this.getLastDay(productionContext, day, 1); // 上一天
            Map<String, List<CxMouldDayProductionHelper>> materialDayProductionInfoMap = entry.getValue();
            // 2.1、遍历当天的排产规格
            for (Entry<String, List<CxMouldDayProductionHelper>> materialDayProductionInfoEntry : materialDayProductionInfoMap
                    .entrySet()) {
                String materialDesc = materialDayProductionInfoEntry.getKey();
                List<CxMouldDayProductionHelper> dayProductionList = materialDayProductionInfoEntry.getValue();

                Map<String, Integer> mouldDayProductionQtyMap = dayProductionList.stream().collect(
                        Collectors.groupingBy(CxMouldDayProductionHelper::getMouldCode, Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream().mapToInt(CxMouldDayProductionHelper::getProductionQty).sum())));
                // 2.1.1、查找出当天的换模具编号
                Set<String> changeMouldCodeSet = dayProductionList.stream().filter(p -> {
                    boolean isChange = false;
                    ProductionMouldInfoVo mouldInfo = mouldInfoMap.get(p.getMouldCode());
                    List<CxMouldDayProductionHelper> dayMouldDayProductionList = mouldInfo.getDayProductionInfo()
                            .get(lastDay);
                    // 2.1.1.1、判断模具如果上一天在没有排产该规格，说明是换模
                    Set<String> lastMaterialDesc = new HashSet<>();
                    if (!CollectionUtils.isEmpty(dayMouldDayProductionList)) {
                        lastMaterialDesc = dayMouldDayProductionList.stream()
                                .map(CxMouldDayProductionHelper::getMaterialDesc).distinct()
                                .collect(Collectors.toSet());
                    }
                    // 2.1.1.2、判断本月首日，如果排产数量等于首日排产量，说明是换模
                    Integer dayProductionQty = mouldDayProductionQtyMap.getOrDefault(p.getMouldCode(), 0);
                    if (day == this.getFirstDay(productionContext)) { // 首日，如果排产量是首日排产量，则算换模
                        if (dayProductionQty.intValue() == changeMouldFirstQty.intValue()) {
                            isChange = true;
                        }
                    } else if (!lastMaterialDesc.contains(materialDesc)) { // 非首日，上一天规格没有生产本规格，则算换模
                        isChange = true;
                    }
                    return isChange;
                }).map(CxMouldDayProductionHelper::getMouldCode).distinct().collect(Collectors.toSet());
                Set<String> mouldCodeSet = new HashSet<>();
                // 2.2.1、更新换模次数
                for (String mouldCode : changeMouldCodeSet) {
                    mouldCodeSet.add(mouldCode);
                    // 双模具排产，按两幅两幅的更新换模次数
                    if (mouldCodeSet.size() < ProductionConstant.DOUBLE_MOULD_PRODUCTION) {
                        continue;
                    }
                    changeMouldLimitHandler.addChangeMouldUsedQty(productionContext, day, materialDesc, mouldCodeSet);
                    mouldCodeSet.clear();
                }
            }
        }
    }

    /**
     * 初始化结构的每日生产统计
     * @param productionContext
     */
    private void initDayProductionInfo(TbrProductionContext productionContext) {
        Map<String, ProductionPlanGroupInfo> allGroupPlanList = productionContext.getGroupProductionInfo();
        Map<String, List<CxMouldDayProductionHelper>> mouldProductionGroup = this.buildMouldProductionGroup(productionContext);
        Map<Long, MonthPlanProductionRequirePlanVo> productionPlanMap = productionContext.getAllProductionPlan();
        for (ProductionPlanGroupInfo groupPlanInfo : allGroupPlanList.values()) {
            groupPlanInfo.initMpDailyCapacityLimit(productionContext); // 初始化结构
            String structureName = groupPlanInfo.getGroupName();
            List<CxMouldDayProductionHelper> mouldProductionList = mouldProductionGroup.get(structureName);
            if (CollectionUtils.isEmpty(mouldProductionList)) {
                continue;
            }
            Map<Integer, List<CxMouldDayProductionHelper>> mouldProductionDayMap = mouldProductionList.stream()
                    .collect(Collectors.groupingBy(CxMouldDayProductionHelper::getProductionDate));
            for (Entry<Integer, List<CxMouldDayProductionHelper>> allEntry : mouldProductionDayMap.entrySet()) {
                Integer day = allEntry.getKey();
                List<CxMouldDayProductionHelper> productionList = allEntry.getValue();

                productionList.stream().collect(Collectors.groupingBy(CxMouldDayProductionHelper::getMonthPlanId)).entrySet().forEach(entry -> {
                    MonthPlanProductionRequirePlanVo productionPlan = productionPlanMap.get(entry.getKey());
                    if (productionPlan == null) {
                        return;
                    }
                    Set<String> usedMouldSet = entry.getValue().stream().map(CxMouldDayProductionHelper::getMouldCode).distinct().collect(Collectors.toSet());
                    Integer realDayProductionQty = entry.getValue().stream().mapToInt(CxMouldDayProductionHelper::getProductionQty).sum();
                    SkuDayProductionInfoHelper skuDayProductionInfo = SkuDayProductionInfoHelper.buildEmpty(day, productionPlan, realDayProductionQty, 0, usedMouldSet);
                    groupPlanInfo.addDayProductionInfo(productionContext, skuDayProductionInfo);
                });
            }
            groupPlanInfo.reCalcMpDailyCapacityLimit(productionContext); // 重新计算统计产能
        }
    }

    /**
     * 构建待保存的排程明细记录
     *
     * @param detailLogList     排产日志列表
     * @param detailList        原排产日志列表
     * @param productionContext 上下文
     * @param factProdReqMap    实单补量
     * @param matchingQtyMap    搭配补量
     * @return
     */
    private List<FactoryMonthPlanMouldDayDetail> buildDetailResultList(List<FactoryMonthPlanMouldDayDetail> detailLogList,
                                                                       List<FactoryMonthPlanMouldDayDetail> detailList,
                                                                       TbrProductionContext productionContext,
                                                                       Map<String, Integer> factProdReqMap,
                                                                       Map<String, Integer> matchingQtyMap) {
        List<FactoryMonthPlanMouldDayDetail> detailResultList = new ArrayList<>();
        LambdaQueryWrapper<FactoryMonthPlanMouldDayDetail> queryWrapper = new LambdaQueryWrapper<FactoryMonthPlanMouldDayDetail>();
        queryWrapper.eq(FactoryMonthPlanMouldDayDetail::getProductionVersion, productionContext.getProductionVersion());
        Map<String, FactoryMonthPlanMouldDayDetail> oldDetailMap = detailList.stream().collect(Collectors
                .toMap(detail -> this.getMouldKey(detail), Function.identity(), (detail1, detail2) -> detail1));
        for (FactoryMonthPlanMouldDayDetail detail : detailLogList) {
            if (!matchingQtyMap.containsKey(detail.getMaterialDesc())
                    && !factProdReqMap.containsKey(detail.getMaterialDesc())) {
                continue;
            }
            detailResultList.add(detail);
            FactoryMonthPlanMouldDayDetail oldDetail = oldDetailMap.get(this.getMouldKey(detail));
            if (oldDetail != null) {
                detail.setId(oldDetail.getId());
                detail.setBaseVale(detail.getId());
            }
            if (null == detail.getAverageSaleQty()) {
                detail.setInventorySalesRatio(null);
                continue;
            }
            if (null == detail.getInventorySalesRatio()) {
                continue;
            }
            if (detail.getInventorySalesRatio().compareTo(BigDecimal.ZERO) < BigDecimal.ZERO.intValue()) {
                detail.setInventorySalesRatio(BigDecimal.ZERO);
            }
        }
        return detailResultList;
    }

    /**
     * 获取排产明细以及对应的模具号
     *
     * @param detail
     * @return
     */
    private String getMouldKey(FactoryMonthPlanMouldDayDetail detail) {
        return GenerageMapKeyUtils.createMapKey(detail.getMonthPlanId(), detail.getMouldCode());
    }

    /**
     * 构建待保存的排程结果记录
     *
     * @param productionContext 上下文
     * @param dayResultList     新排产结果
     * @param resultList        旧排产结果
     * @param factProdReqMap    实单补量
     * @param matchingQtyMap    搭配补量
     * @return
     */
    private List<FactoryMonthPlanMouldDayResult> buildMouldResultList(TbrProductionContext productionContext,
                                                                      List<FactoryMonthPlanMouldDayResult> dayResultList,
                                                                      List<FactoryMonthPlanMouldDayResult> resultList,
                                                                      Map<String, Integer> factProdReqMap,
                                                                      Map<String, Integer> matchingQtyMap) {
        List<FactoryMonthPlanMouldDayResult> saveResultList = new ArrayList<>();
        Map<String, FactoryMonthPlanMouldDayResult> oldPlanMap = resultList.stream()
                .collect(Collectors.toMap(FactoryMonthPlanMouldDayResult::getMaterialCode, Function.identity()));
        Long productionSequence = resultList.stream().map(FactoryMonthPlanMouldDayResult::getProductionSequence)
                .filter(Objects::nonNull).max(Long::compareTo).orElse(0L);
        
        // 合并需求计划
        Map<String, MonthPlanProductionRequirePlanVo> requireMap = productionContext.getAllProductionPlan().values()
                .stream().collect(Collectors.toMap(MonthPlanProductionRequirePlanVo::getMaterialCode,
                        Function.identity(), (p1, p2) -> {
                            p1.setHeightLossQty(safeAdd(p1.getHeightLossQty(), p2.getHeightLossQty()));
                            p1.setMidQty(safeAdd(p1.getMidQty(), p2.getMidQty()));
                            p1.setPostponeQty(safeAdd(p1.getPostponeQty(), p2.getPostponeQty()));
                            return p1;
                        }));
        
        for (FactoryMonthPlanMouldDayResult plan : dayResultList) {
            Integer factProdQty = factProdReqMap.getOrDefault(plan.getMaterialDesc(), 0); // 实单补量
            Integer matchingQty = matchingQtyMap.getOrDefault(plan.getMaterialDesc(), 0); // 搭配补量
            if (factProdQty <= 0 && matchingQty <= 0) { // 非搭配排产涉及的sku，不处理
                continue;
            }
            FactoryMonthPlanMouldDayResult firstPlan = CollectionUtils.firstElement(resultList);
            // 原有记录有同规格的更新（有ID）；没有的说明是新搭配的规格，需要新增（无ID）
            FactoryMonthPlanMouldDayResult oldPlan = oldPlanMap.get(plan.getMaterialCode());
            if (oldPlan != null) {
                MonthPlanProductionRequirePlanVo requirePlan = requireMap.get(oldPlan.getMaterialCode());
                if (requirePlan != null) {
                    oldPlan.setHeightQty(requirePlan.getHeightQty());
                    oldPlan.setMidLossQty(requirePlan.getMidQty());
                    oldPlan.setPostponeQty(requirePlan.getPostponeQty());
                }
                plan.setConventionProductionQty(matchingQty);
                plan.setTotalQty(oldPlan.getTotalQty() + matchingQty + factProdQty);
                Integer temFactProdQty = factProdQty + oldPlan.getTotalQty();
                // 高优先级
                Integer diffQty = this.allocationProductionQty(plan, oldPlan, factProdQty, temFactProdQty, "heightQty", "heightProductionQty");
                temFactProdQty -= diffQty;
                // 中优先级
                diffQty = this.allocationProductionQty(plan, oldPlan, factProdQty, temFactProdQty, "midLossQty", "midProductionQty");
                temFactProdQty -= diffQty;
                // 暂缓
                diffQty = this.allocationProductionQty(plan, oldPlan, factProdQty, temFactProdQty, "postponeQty", "postponeProductionQty");
                temFactProdQty -= diffQty;
                // 差异
                plan.setDifferenceQty(oldPlan.getDifferenceQty() - factProdQty);
                plan.setCycleProductionQty(oldPlan.getCycleProductionQty());
                plan.setMouldCavityQty(oldPlan.getMouldCavityQty());
                plan.setTypeBlockQty(oldPlan.getTypeBlockQty());
                plan.setFactProdReqQty(oldPlan.getFactProdReqQty());
                plan.setReason(oldPlan.getReason());
                plan.setId(oldPlan.getId());
            } else {
                plan.setConventionProductionQty(matchingQty + factProdQty);
                plan.setTotalQty(plan.getConventionProductionQty());
                Integer temFactProdQty = factProdQty;
                // 高优先级
                Integer diffQty = this.allocationProductionQty(plan, oldPlan, factProdQty, temFactProdQty, "heightQty", "heightProductionQty");
                temFactProdQty -= diffQty;
                // 中优先级
                diffQty = this.allocationProductionQty(plan, oldPlan, factProdQty, temFactProdQty, "midLossQty", "midProductionQty");
                temFactProdQty -= diffQty;
                // 暂缓
                diffQty = this.allocationProductionQty(plan, oldPlan, factProdQty, temFactProdQty, "postponeQty", "postponeProductionQty");
                temFactProdQty -= diffQty;
                plan.setCycleProductionQty(0);
                if (plan.getProductionSequence() == null) {
                    productionSequence++;
                    plan.setProductionSequence(productionSequence);
                }
                plan.setDifferenceQty(0);
                plan.setFactProdReqQty(0);
                plan.setReason(null);
            }
            plan.setCreateBy(firstPlan.getCreateBy());
            plan.setCreateTime(firstPlan.getCreateTime());
            plan.setUpdateBy(firstPlan.getUpdateBy());
            plan.setUpdateTime(firstPlan.getUpdateTime());

            if (null != plan.getInventorySalesRatio()
                    && plan.getInventorySalesRatio().compareTo(BigDecimal.ZERO) < BigDecimal.ZERO.intValue()) {
                plan.setInventorySalesRatio(BigDecimal.ZERO);
            }
            saveResultList.add(plan);
        }
        return saveResultList;
    }

    /**
     * 分配排产数量
     * 
     * @param plan             新排产记录
     * @param oldPlan          旧排产记录
     * @param newProductQty    排产量
     * @param reqFieldName     需求量字段名
     * @param productFieldName 排产量字段名
     * @return
     */
    private Integer allocationProductionQty(FactoryMonthPlanMouldDayResult plan, FactoryMonthPlanMouldDayResult oldPlan,
                                            Integer factProdQty, Integer newProductQty, String reqFieldName,
                                            String productFieldName) {
        Integer diffQty = 0; // 未分配量
        if (oldPlan == null) {
            plan.setFieldValueByFieldName(productFieldName, 0);
            return diffQty;
        }
        Integer productQty = intValue(oldPlan.getFieldValueByFieldName(productFieldName));
        if (factProdQty > 0) {
            Integer reqQty = intValue(oldPlan.getFieldValueByFieldName(reqFieldName));
            if (newProductQty > 0 && reqQty > productQty) {
                diffQty = Math.min(reqQty - productQty, newProductQty);
                plan.setFieldValueByFieldName(productFieldName, productQty + diffQty);
            }
        } else {
            plan.setFieldValueByFieldName(productFieldName, productQty);
        }
        return diffQty;
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
        List<MonthPlanStructureLhRatioVo> structureLhRatioList = getDataService().getLhRatioInfo(context,
                structureNameList);
//        log.info(TbrBeforeProductionGroupLogRecorder.addReaderCxLhGroupRatioLog(context, structureLhRatioList));
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
        List<CycleStructureMinLhMachineQtyVo> cycleStructureMinLhRatioList = dpRequireDataService
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
     * 加载需求计划列表
     * 
     * @param productionContext 上下文
     * @param detailLogList     模具排产日志表
     * @return
     */
    private List<MonthPlanProductionRequirePlanVo> selectRequirePlan(TbrProductionContext productionContext,
                                                                     List<FactoryMonthPlanMouldDayDetail> detailLogList) {
        List<MonthPlanProductionRequirePlanVo> requirePlanList = new ArrayList<>();
        String productionVersion = productionContext.getProductionVersion();
        String monthPlanVersion = productionContext.getMonthPlanVersion();
        Map<String, List<MonthPlanProductConstructionInfoVo>> constructionInfoMap = getProductionConstructionInfo(productionContext);
        Map<Long, List<FactoryMonthPlanMouldDayDetail>> detailLogMap = detailLogList.stream()
                .filter(d -> d.getMonthPlanId() != null)
                .collect(Collectors.groupingBy(FactoryMonthPlanMouldDayDetail::getMonthPlanId));

        // 加载需求计划
        LambdaQueryWrapper<DpDemandPlan> demandQueryWrapper = new LambdaQueryWrapper<DpDemandPlan>();
        demandQueryWrapper.eq(DpDemandPlan::getMonthPlanVersion, monthPlanVersion);
        demandQueryWrapper.isNotNull(DpDemandPlan::getStructureName); // 过滤空结构的数据
        List<DpDemandPlan> demandPlanList = monthPlanRequireMapper.selectList(demandQueryWrapper);
        if (CollectionUtils.isEmpty(demandPlanList)) {
            return new ArrayList<>(0);
        }
        ProductionInitParamConfiguration paramConfiguration = this.createInitParamConfiguration(productionContext);
        Map<String, MonthPlanProductLhCapacityVo> lhCapacityMap = this.getProductLhCapacityInfo(productionContext,
                paramConfiguration.getDayVulcanizationQtyConfiguration());

        // 需求计划需要按物料号合并各需求量
        for (DpDemandPlan demandPlan : demandPlanList) {
            String materialCode = demandPlan.getMaterialCode();
            MonthPlanProductionRequirePlanVo requirePlan = MonthPlanProductionRequirePlanVo.buildInitProductionPlan(null, productionVersion,
                        demandPlan);
            requirePlan.setFactProdReqQty(demandPlan.getNetQty());
            requirePlan.setHeightLossQty(demandPlan.getHeightQty());
            requirePlan.setHeightQty(demandPlan.getHeightQty());
            requirePlan.setMidQty(demandPlan.getMidQty());
            requirePlan.setPostponeQty(demandPlan.getPostponeQty());
            requirePlan.setVulcanizationInfo(lhCapacityMap.get(demandPlan.getMaterialDesc())); // 设置硫化信息
            requirePlan.setInventorySalesRatio(0D);// 默认0
            requirePlan.setConstructionInfo(constructionInfoMap.get(materialCode)); //加载施工
            requirePlan.setMonthPlanId(demandPlan.getId());
            List<FactoryMonthPlanMouldDayDetail> detailLogs = detailLogMap.get(demandPlan.getId());
            int productionQty = 0;
            if (!CollectionUtils.isEmpty(detailLogs)) {
                productionQty = detailLogs.stream().filter(d -> d.getTotalQty() != null)
                        .mapToInt(FactoryMonthPlanMouldDayDetail::getTotalQty).sum();
            }
            requirePlan.setOriginProductionQty(productionQty);
            requirePlan.setProductionQty(0);
            requirePlan.setProducedQty(productionQty);
            requirePlan.resetProductionDataInfo();
            requirePlanList.add(requirePlan);
        }
        return requirePlanList;
    }

    /**
     * 将定稿计划和需求计划构建成算法要求的上下文结构
     *
     * @param planList 定稿计划
     * @return
     */
    private TbrProductionContext initProductionContext(List<FactoryMonthPlanMouldDayResult> planList) {
        // 构建上下文对象
        FactoryMonthPlanMouldDayResult result = CollectionUtils.firstElement(planList);
        TbrProductionContext productionContext = this.initProductionContext(result);

        BaseDataContainer container = productionContext.getBaseDataContainer();
        container.setParamConfiguration(this.createParamConfiguration(productionContext)); // 排程参数
        this.setProductionCycleInfo(productionContext); // 设置生产周期
        this.setMonthProductionDays(productionContext); // 设置生产日
        this.buildDayCapacityLimitInfo(productionContext); // 初始化日产能限制
        productionContext.setTempLogBuilder(new StringBuilder()); // 初始化临时日志

        return productionContext;
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
     * 将定稿计划和需求计划构建成算法要求的上下文结构
     *
     * @param productionContext 上下文
     * @param planList          定稿计划
     * @param detailLogList     模具排产日志计划
     * @param requirePlanList   需求计划
     * @param cxContinueInfoMap 续作规格列表
     * @return
     */
    private TbrProductionContext buildProductionContext(TbrProductionContext productionContext,
                                                        List<FactoryMonthPlanMouldDayResult> planList,
                                                        List<FactoryMonthPlanMouldDayDetail> detailLogList,
                                                        List<MonthPlanProductionRequirePlanVo> requirePlanList) {
        // 构建各项排产过程数据
        BaseDataContainer container = productionContext.getBaseDataContainer();
        Map<Long, MonthPlanProductionRequirePlanVo> requirePlanMap = requirePlanList.stream()
                .collect(Collectors.toMap(MonthPlanProductionRequirePlanVo::getMonthPlanId, Function.identity()));

        container.setStructureLhRatioList(this.getLhRatioConfiguration(productionContext, requirePlanList)); // 结构硫化配比
        container.setSkuMouldRelationMap(this.getProductionMouldInfo(productionContext)); // 模具施工关系
        container.setCxMachineBaseInfo(this.getDataService().getCxMachineBaseInfo(productionContext)); // 成型机基础信息加载
        container.setMouldInfoMap(this.buildMouldInfoMap(productionContext, detailLogList, requirePlanMap)); // 已排模具计划
        container.setGroupMainPatternAllocationLimitMap(this.getGroupMainPatternAllocationInfo(productionContext)); // 结构模具分配配比
        container.setLhMachineInfoList(getDataService().listLhMachineInfo(productionContext)); // 加载硫化机
        this.specialMaterialInfoHandler(productionContext);
//        this.buildCxLhRatioMap(productionContext, container.getMouldInfoMap(), requirePlanMap); // 构建成型硫化组
        productionContext.setOverSixMonthStockMap(this.overSixMonthStockHandler(productionContext,
                getDataService().getMdmProductStock(productionContext))); // 超6个成品库存信息
        this.fillMouldRelationStructureName(productionContext, requirePlanList); // 补充模具关系中的物料结构名
        this.buildGroupMainPatternInfo(productionContext); // 构建结构+主花纹的模具信息
        productionContext.setContinueStructureMap(this.getContinueStructureMap(this.getContinueInfo(productionContext))); // 设置成型机台续作结构
        // 各项已排产统计数据
        productionContext.setAllProductionPlan(requirePlanList.stream()
                .collect(Collectors.toMap(MonthPlanProductionRequirePlanVo::getMonthPlanId, Function.identity())));
        productionContext.setAllSkuProductionPlan(requirePlanList.stream()
                .collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc)));
        productionContext.setSkuPlannedQtyMap(planList.stream()
                .collect(Collectors.groupingBy(FactoryMonthPlanMouldDayResult::getStructureName,
                        Collectors.collectingAndThen(Collectors.toList(),
                                list -> list.stream().mapToInt(FactoryMonthPlanMouldDayResult::getTotalQty).sum())))); // 各sku已排产量
        productionContext.setSkuWastageQtyMap(new HashMap<>());
        
        return productionContext;
    }

    /**
     * 根据计划的物料描述，补充模具关系中的物料结构名
     *
     * @param requirePlanList
     * @param productionContext
     */
    private void fillMouldRelationStructureName(TbrProductionContext productionContext,
                                                List<MonthPlanProductionRequirePlanVo> requirePlanList) {
        Set<String> isSetStructureNameSet = new HashSet<>();
        try {
            Map<String, List<MonthPlanProductMouldInfoVo>> finalMouldRelationMap = productionContext
                    .getBaseDataContainer().getSkuMouldRelationMap();
            requirePlanList.forEach(requirePlan -> {
                String materialDesc = requirePlan.getMaterialDesc();
                if (StringUtils.isBlank(materialDesc)) {
                    return;
                }
                if (isSetStructureNameSet.contains(materialDesc)) {
                    return;
                }
                isSetStructureNameSet.add(materialDesc);
                List<MonthPlanProductMouldInfoVo> mouldRelationList = finalMouldRelationMap
                        .get(requirePlan.getMaterialDesc());
                if (CollectionUtils.isEmpty(mouldRelationList)) {
                    return;
                }
                mouldRelationList.forEach(mouldRelation -> {
                    mouldRelation.setStructureName(requirePlan.getStructureName());
                });
            });
        } catch (Exception e) {
            log.error("根据计划的物料描述补充模具关系中的物料结构名失败", e);
        }
    }

    /**
     * 构建模具排产集合
     *
     * @param productionContext
     * @param detailLogList
     * @param requirePlanMap
     * @return
     */
    private Map<String, ProductionMouldInfoVo> buildMouldInfoMap(TbrProductionContext productionContext,
                                                                 List<FactoryMonthPlanMouldDayDetail> detailLogList,
                                                                 Map<Long, MonthPlanProductionRequirePlanVo> requirePlanMap) {
        Map<String, List<MonthPlanProductMouldInfoVo>> skuMouldRelationMap = productionContext.getBaseDataContainer()
                .getSkuMouldRelationMap(); // 模具sku关系，key=物料描述
        // 构建模具排产数据
        Map<String, ProductionMouldInfoVo> mouldInfoMap = new HashMap<>();

        // 先模具初始化
        List<MonthPlanProductMouldInfoVo> allProductMouldInfoList = new ArrayList<>();
        skuMouldRelationMap.values().forEach(list -> allProductMouldInfoList.addAll(list));
        Date boardingDate = productionContext.getProductionStartDate();
        for (MonthPlanProductMouldInfoVo mould : allProductMouldInfoList) {
            String mouldCode = mould.getMouldCode();
            ProductionMouldInfoVo productionMouldInfo = mouldInfoMap.get(mouldCode);
            if (productionMouldInfo == null) {
                productionMouldInfo = ProductionMouldInfoVo.createEmptyProductionMouldInfo(mould);
                productionMouldInfo.setProductionDayInfo(productionContext, boardingDate);
                productionMouldInfo.setDayProductionInfo(new HashMap<>()); // 先初始化日排程列表
                mouldInfoMap.put(mouldCode, productionMouldInfo);
            }
        }

        for (FactoryMonthPlanMouldDayDetail detail : detailLogList) {
            MonthPlanProductionRequirePlanVo requirePlan = requirePlanMap.get(detail.getMonthPlanId());
            if (requirePlan == null) {
                continue;
            }
            String mouldCode = detail.getMouldCode();
            if (StringUtils.isEmpty(mouldCode)) {
                continue;
            }
            List<MonthPlanProductMouldInfoVo> mouldList = skuMouldRelationMap.get(detail.getMaterialDesc());
            if (CollectionUtils.isEmpty(mouldList)) {
                continue;
            }
            ProductionMouldInfoVo productionMouldInfo = mouldInfoMap.get(mouldCode);
            if (productionMouldInfo == null) {
                continue;
            }
            Integer beginDay = Optional.ofNullable(detail.getBeginDay()).orElse(1);
            Integer endDay = Optional.ofNullable(detail.getEndDay()).orElse(productionContext.getProductionEndDay());
            Set<String> cxMachineCodeInfo; // 成型机台
            if (StringUtils.isEmpty(detail.getCxMachineCode())) {
                cxMachineCodeInfo = new HashSet<>();
            } else {
                cxMachineCodeInfo = new HashSet<>(Arrays.asList(StringUtils.split(detail.getCxMachineCode(), StringConstant.COMMA))); // 成型机台
            }
            for (int day = beginDay; day <= endDay; day++) {
                Integer productionQty = (Integer) detail.getFieldValueByFieldName(FactoryConstant.DAY_FIELD + day); // 根据日期取对应日计划量
                if (productionQty == null || productionQty <= 0) {
                    continue;
                }
                productionQty = productionQty * ProductionConstant.DOUBLE_MOULD_PRODUCTION; // 双模排产
                productionMouldInfo.addProductionInfo(day, requirePlan, false, productionQty,
                        cxMachineCodeInfo);
            }
        }
        return mouldInfoMap;
    }


    /**
     * 构建模具组
     *
     * @param productionContext
     * @return
     */
    private Map<String, List<CxMouldDayProductionHelper>> buildMouldProductionGroup(TbrProductionContext productionContext) {
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
     * @param productionContext    排产上下文
     * @param startDay             排产开始日
     * @param endDay               排产结束日
     * @param productionPlanList   排产计划
     * @param scheduleMaterialDesc 已排SKU
     * @param checkMaterialDesc 指定检查的SKU，有指定的话只检查该SKU是否可排产
     * @return
     */
    private String getSelectedAddSku(TbrProductionContext productionContext, Integer startDay, Integer endDay,
                                     List<MonthPlanProductionRequirePlanVo> productionPlanList,
                                     Set<String> scheduleMaterialDesc, String checkMaterialDesc) {
        if (CollectionUtils.isEmpty(productionPlanList)) {
            return StringUtils.EMPTY;
        }
        if (scheduleMaterialDesc.contains(checkMaterialDesc)) { // 有指定SKU，且该SKU已经检查过，则直接结束
            return StringUtils.EMPTY;
        }
        List<MonthPlanProductionRequirePlanVo> enablePlanList = productionPlanList;
        // 只看有常规储备的sku
        List<MonthPlanProductionRequirePlanVo> hasReserveQtyPlanList = enablePlanList.stream()
                .filter(s -> StringUtils.isEmpty(checkMaterialDesc) || Objects.equals(s.getMaterialDesc(), checkMaterialDesc)) // 如果有指定SKU，则只检查该SKU
                .filter(s -> !scheduleMaterialDesc.contains(s.getMaterialDesc())) // 已经排过的跳过
                .filter(s -> s.getProductionQty() > 0).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasReserveQtyPlanList)) {
            return StringUtils.EMPTY;
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
                .filter(plan -> plan.getProductionQty() > BigDecimal.ZERO.longValue())
                .collect(Collectors.toList());
        // 高优先级优先
        if (CollectionUtils.isEmpty(heightList)) {
            return null;
        }
        return new SkuNeedProductionInfo(ProductionQtyModelEnum.REMAIN_MATCHING_QTY, heightList);
    }

    /**
     * 获取初始化业务的参数设定
     *
     * @param productionContext
     * @return
     */
    private ProductionCapacityParamConfiguration createParamConfiguration(TbrProductionContext productionContext) {
        List<String> paramCodeList = new ArrayList<>(64);
        //日排产相关
        paramCodeList.add(MonthPlanEnums.DAY_CHANGE_GROUP_COUNT.getCode());
        paramCodeList.add(MonthPlanEnums.CHANGE_MOULD_LH_MACHINE_NUMBER.getCode());
        paramCodeList.add(MonthPlanEnums.CHANGE_MOULD_FIRST_QTY.getCode());
        paramCodeList.add(MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY_DIFF.getCode());
        paramCodeList.add(MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY.getCode());
        paramCodeList.add(MonthPlanEnums.CHANGE_TYPE_BLOCK_MAX_QTY.getCode());
        paramCodeList.add(MonthPlanEnums.SINGLE_CX_EMBRYO_CODE_COUNT.getCode());
        paramCodeList.add(MonthPlanEnums.DAY_MAX_CAPACITY.getCode());
        paramCodeList.add(MonthPlanEnums.DAY_MIN_CAPACITY.getCode());
        //排产控制相关
        paramCodeList.add(MonthPlanEnums.SUM_PRODUCTION_QTY.getCode());
        paramCodeList.add(MonthPlanEnums.HEIGHT_DIFF_QTY.getCode());
        paramCodeList.add(MonthPlanEnums.SKU_SECOND_PRODUCTION.getCode());
        paramCodeList.add(MonthPlanEnums.BOOST_PRODUCTION_TYPE_VALUE.getCode());
        paramCodeList.add(MonthPlanEnums.MATCHING_BOOST_DAY.getCode());
        paramCodeList.add(MonthPlanEnums.MAX_BOOST_DAY.getCode());
        paramCodeList.add(MonthPlanEnums.MIN_PRODUCTION_DAYS.getCode());
        paramCodeList.add(MonthPlanEnums.MIN_ALLOCATION_DAYS.getCode());
        paramCodeList.add(MonthPlanEnums.NO_CYCLE_PRODUCTION_MIN_LH_MACHINE_NUMBER.getCode());
        paramCodeList.add(MonthPlanEnums.OEM_BRAND_CONFIG.getCode());
        paramCodeList.add(MonthPlanEnums.OEM_BRAND_CAPACITY.getCode());
        paramCodeList.add(MonthPlanEnums.RESERVE_PERCENT.getCode());
        //降膜排产相关
        paramCodeList.add(MonthPlanEnums.DEDUCT_MOULD_MIN_LH_MACHINE_COUNT.getCode());
        paramCodeList.add(MonthPlanEnums.FIRST_NEAR_DEAD_LINE_DAY.getCode());
        paramCodeList.add(MonthPlanEnums.FIRST_NEAR_DEAD_LINE_MAX_LH_MACHINE_COUNT.getCode());
        paramCodeList.add(MonthPlanEnums.SECOND_NEAR_DEAD_LINE_DAY.getCode());
        paramCodeList.add(MonthPlanEnums.SECOND_NEAR_DEAD_LINE_MAX_LH_MACHINE_COUNT.getCode());
        paramCodeList.add(MonthPlanEnums.LAST_NEAR_DEAD_LINE_DAY.getCode());
        paramCodeList.add(MonthPlanEnums.LAST_NEAR_DEAD_LINE_MAX_LH_MACHINE_COUNT.getCode());
        //其他
        paramCodeList.add(MonthPlanEnums.SECTION_WIDTH_DIFF_VALUE.getCode());
        paramCodeList.add(MonthPlanEnums.CHANGE_STRUCT_DEC_LH_MACHINES.getCode());
        paramCodeList.add(MonthPlanEnums.SPECIAL_MATERIAL_CODE.getCode());
        //获取数据
        Map<String, Object> paramConfigurationMap = getDataService().getFactoryParamByCondition(productionContext, paramCodeList);
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
        //外销贴牌-品牌配置
        String oemBrandConfig = (String) paramConfigurationMap.get(MonthPlanEnums.OEM_BRAND_CONFIG.getCode());
        if (StringUtils.isBlank(oemBrandConfig)) {
            configuration.setOemBrandConfig(Collections.emptySet());
        } else {
            configuration.setOemBrandConfig(Stream.of(oemBrandConfig.split(StringConstant.COMMA)).collect(Collectors.toSet()));
        }
        //外销贴牌-总产量配置，单位条
        configuration.setOemBrandCapacity((Integer) paramConfigurationMap.get(MonthPlanEnums.OEM_BRAND_CAPACITY.getCode()));
        //周期储备量占实单的比例(%)
        configuration.setReservePercent((Integer) paramConfigurationMap.get(MonthPlanEnums.RESERVE_PERCENT.getCode()));

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
        Object deductionLhMachineValue = paramConfigurationMap.get(MonthPlanEnums.CHANGE_STRUCT_DEC_LH_MACHINES.getCode());
        Integer deductionLhMachine = Optional.ofNullable((Integer) deductionLhMachineValue).orElse(BigDecimal.ZERO.intValue());
        configuration.setDeductionLhMachineCount(deductionLhMachine);
        //特殊原材料
        Object specialMaterialCodeValue = paramConfigurationMap.get(MonthPlanEnums.SPECIAL_MATERIAL_CODE.getCode());
        String specialMaterialCode = (String) Optional.ofNullable(specialMaterialCodeValue).orElse("");
        if (StringUtils.isBlank(specialMaterialCode)) {
            configuration.setSpecialMaterialCodeSet(Collections.emptySet());
        } else {
            configuration.setSpecialMaterialCodeSet(Stream.of(specialMaterialCode.split(StringConstant.COMMA)).collect(Collectors.toSet()));
        }
        return configuration;
    }
    
    /**
     * 检查二次上机
     *
     * @param productionPlanInfo 排产计划信息
     * @param productionContext  排产上下文
     * @param materialDesc       规格描述
     * @param startDay           上机日
     * @return true-允许二次上机，false-不允许二次上机
     */
    private boolean checkSecOnline(ProductionPlanGroupInfo productionPlanInfo, TbrProductionContext productionContext,
                                   String materialDesc, Integer startDay) {
        List<Integer> dayList = productionPlanInfo.getProductionDaySetBySku(materialDesc);
        if (CollectionUtils.isEmpty(dayList)) {
            return true;
        }
        if (dayList.contains(startDay)) {
            return true;
        }
        // 取最大的天数
        Integer lastCloseDay = dayList.stream().max(Integer::compareTo).get();
        int skuSecondProductionDays = productionContext.getBaseDataContainer().getParamConfiguration().getSkuSecondProduction();
        SkuSecondChecker skuSecondChecker = new SkuSecondChecker(startDay, lastCloseDay, skuSecondProductionDays);
        return skuSecondChecker.doCheck();
    }

    /**
     * 获取需要排产的SKU的模具配置信息 key = materialDesc: value =
     * List<MonthPlanProductMouldInfoVo>
     *
     * @param productionContext
     * @return
     */
    private Map<String, List<MonthPlanProductMouldInfoVo>> getProductionMouldInfo(TbrProductionContext productionContext) {
        // 已有模具的配置关系
        List<MonthPlanProductMouldInfoVo> productMouldInfoList = this.getDataService().getEnableProductionMouldInfo(productionContext);
        // 新模具到货计划关系
        List<MonthPlanProductMouldInfoVo> mouldDeliveryList = this.getDataService().getEnableProductionMouldDeliveryInfo(productionContext);

        List<MonthPlanProductMouldInfoVo> allMouldRelationInfoList = MouldRelationDeduplicator.deduplicateAndMerge(productMouldInfoList, mouldDeliveryList, productionContext);
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
//            log.info(TbrProductionInitLogRecorder.addDayLhCapacityInfoEmptyLog(productionContext));
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
//            log.info(TbrProductionInitLogRecorder.addInitParamEmptyLog(productionContext));
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
            context.setProductionStartDate(com.zlt.aps.mp.engine.utils.DateUtils.getDate(productionMonth));
            context.setProductionEndDate(com.zlt.aps.mp.engine.utils.DateUtils.getDate(year, month, monthDays));
            return;
        }
        // 非自然月
        LocalDate previousMonth = context.getPreviousMonth();
        context.setProductionStartDate(com.zlt.aps.mp.engine.utils.DateUtils.getDate(previousMonth.getYear(),
                previousMonth.getMonthValue(), cycleStartDay));
        context.setProductionEndDate(com.zlt.aps.mp.engine.utils.DateUtils.getDate(year, month, cycleStartDay - 1));
    }

    /**
     * 在正式排产前进行重置数据处理
     *
     * @param productionContext 排产上下文
     * @param allGroupPlanInfo  所有分组计划对象
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
            Integer maxCycleQty = groupProductionInfo.getGroupPlanData().stream()
                    .mapToInt(MonthPlanProductionRequirePlanVo::getCycleReserveQty).sum();
            groupProductionInfo.setMaxCycleQty(maxCycleQty);
        });
//      // 处理计划的待排产量及排产标记重置
//      Map<Long, MonthPlanProductionRequirePlanVo> allSinglePlanMap = productionContext.getAllProductionPlan();
//      if (!CollectionUtils.isEmpty(allSinglePlanMap)) {
//          allSinglePlanMap.forEach((monthPlanId, singlePlan) -> singlePlan.resetProductionDataInfo());
//      }
//      // 重新构建模具排产信息，全部清空
//      Map<String, ProductionMouldInfoVo> allMouldInfoMap = productionContext.getBaseDataContainer().getMouldInfoMap();
//      if (!CollectionUtils.isEmpty(allMouldInfoMap)) {
//          allMouldInfoMap.forEach((mouldCode, singleMouldInfo) -> {
//              singleMouldInfo.setFinishDaySet(new HashSet<>());
//              singleMouldInfo.setDayProductionInfo(new HashMap<>());
//          });
//      }
    }

    /**
     * 设置工厂的排产日信息 包含 停产日及开停产的产能比例 t_mdm_work_calendar
     *
     * @param context
     */
    private void setMonthProductionDays(Context context) {
        List<ProductionDayInfoVo> productionDayInfoList = this.getDataService().getProductCalendar(context);
//        log.info(TbrBeforeProductionGroupLogRecorder.addReaderProductionCalendarLog(context, productionDayInfoList));
        Integer maxBoostDays = ((TbrProductionContext) context).getBaseDataContainer().getParamConfiguration().getMaxBoostDay();
        if (CollectionUtils.isEmpty(productionDayInfoList)) {
            context.setCapacityRatioMap(Collections.emptyMap());
            context.setStopDays(Collections.emptySet(), maxBoostDays);
            throw new BusinessException(I18nUtil.getMessage("alg.data.production.noConfigurationCalendar"));
        }
        // 排产开始日
        Date productionStartDate = context.getProductionStartDate();
        // 开产比例设置
        Map<Integer, Integer> startProductionRatioMap = new HashMap<>(context.getMonthDays());
        List<ProductionDayInfoVo> startProductionDays = productionDayInfoList.stream().filter(productionDayInfo -> YesOrNoEnum.YES.getCode().equals(productionDayInfo.getDayFlag())).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(startProductionDays)) {
            startProductionDays.forEach(startProductionInfo -> {
                Date startProduction = startProductionInfo.getProductionDate();
                Integer startDay = com.zlt.aps.mp.engine.utils.DateUtils.getIntervalDays(productionStartDate, startProduction);
                startProductionRatioMap.put(startDay, startProductionInfo.getRate());
            });
        }
        context.setCapacityRatioMap(startProductionRatioMap);
        // 停产设置
        List<ProductionDayInfoVo> stopDays = productionDayInfoList.stream().filter(productionDayInfo -> YesOrNoEnum.NO.getCode().equals(productionDayInfo.getDayFlag())).collect(Collectors.toList());
//        log.info(TbrBeforeProductionGroupLogRecorder.addReaderStopCalendarLog(context, stopDays));
        if (CollectionUtils.isEmpty(stopDays)) {
            context.setStopDays(Collections.emptySet(), maxBoostDays);
            return;
        }
        Set<Integer> stopDaySet = new HashSet<>(context.getMonthDays());
        stopDays.forEach(stopProductionInfo -> {
            Date stopProduction = stopProductionInfo.getProductionDate();
            Integer stopDay = com.zlt.aps.mp.engine.utils.DateUtils.getIntervalDays(productionStartDate, stopProduction);
            stopDaySet.add(stopDay);
        });
        context.setStopDays(stopDaySet, maxBoostDays);
    }

    /**
     * 构建日产能限制对象信息
     *
     * @param productionContext 排产上下文
     */
    private void buildDayCapacityLimitInfo(TbrProductionContext productionContext) {
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        DayCapacityLimitVo dayCapacityLimit = new DayCapacityLimitVo(Collections.emptyMap());
        Set<Integer> productionDayList = productionContext.getProductionDay();
        if (CollectionUtils.isEmpty(productionDayList)) {
            baseDataContainer.setDayCapacityLimit(dayCapacityLimit);
            return;
        }
        Set<Integer> openDay = productionContext.getProductionDayAfterStop();
        ProductionCapacityParamConfiguration paramConfiguration = baseDataContainer.getParamConfiguration();
        Map<Integer, DayCapacityLimitHelper> dayCapacityLimitMap = new HashMap<>(productionDayList.size());
        Map<Integer, Integer> startProductionRatioMap = productionContext.getCapacityRatioMap();
        productionDayList.forEach(productionDay -> {
            Integer ratio = startProductionRatioMap.get(productionDay);
            //20260127 开产时，只是量放一半，日产限制还是放大到100
            if (openDay.contains(productionDay)) {
                ratio = ProductionConstant.PERCENTAGE;
            }
            DayCapacityLimitHelper dayInitLimit = DayCapacityLimitHelper.createInit(productionDay, paramConfiguration, ratio);
            dayCapacityLimitMap.put(productionDay, dayInitLimit);
        });
        dayCapacityLimit.updateWholeDayLimitInfo(dayCapacityLimitMap);
        baseDataContainer.setDayCapacityLimit(dayCapacityLimit);
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
//      Map<Long, MonthPlanProductionRequirePlanVo> allProductionPlanMap = productionContext.getAllProductionPlan();
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();

        List<MonthPlanProductMouldInfoVo> skuRelationList = baseDataContainer.getSkuMouldRelationMap()
                .get(materialDesc);
        if (CollectionUtils.isEmpty(skuRelationList)) {
            return Collections.emptyList();
        }
        Map<String, ProductionMouldInfoVo> mouldInfoMap = baseDataContainer.getMouldInfoMap();

        List<ProductionMouldInfoVo> effectiveList = new ArrayList<>();
        skuRelationList.stream().forEach(skuRelation -> {
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
                    if (productionList.stream().anyMatch(p -> StringUtils.isNotEmpty(p.getMaterialDesc())
                            && !p.getMaterialDesc().equals(materialDesc))) {
                        return; // 已经排了其他规格，则需要排除
                    }
                    Integer productionQty = productionList.stream()
                            .mapToInt(CxMouldDayProductionHelper::getProductionQty).sum(); // 合计当天的已排量
                    if (productionQty >= dayMoldQty) {
                        return; // 已经满产能，该模具当天不可用
                    }
                }
            }
            effectiveList.add(mouldInfo);
        });
        if (CollectionUtils.isEmpty(effectiveList)) {
            return Collections.emptyList();
        }
        if (effectiveList.size() < lhMouldQty) {
            return Collections.emptyList();
        }
        Integer lastDay = this.getLastDay(productionContext, day, 1);
        effectiveList.sort((m1, m2) -> this.usedMouldCompare(materialDesc, day, lastDay, m1, m2));
        return effectiveList;
    }
    
    /**
     * 获取下一个排产日
     *
     * @param contextDTO
     * @param day
     * @param beginDay
     * @return
     */
    private Integer getNextDay(TbrProductionContext productionContext, int day, int endDay) {
        Integer lastDay = 0;
        for (int i = day + 1; i <= endDay; i++) {
            if (!productionContext.getStopDays().contains(i)) { // 下一天是排产日返回，否则跳过看下一天
                lastDay = i;
                break;
            }
        }
        return lastDay;
    }

    /**
     * 获取上一个排产日
     *
     * @param contextDTO
     * @param day
     * @param beginDay
     * @return
     */
    private Integer getLastDay(TbrProductionContext productionContext, int day, int beginDay) {
        Integer lastDay = 0;
        for (int i = day - 1; i >= beginDay; i--) {
            if (!productionContext.getStopDays().contains(i)) { // 下一天是排产日返回，否则跳过看下一天
                lastDay = i;
                break;
            }
        }
        return lastDay;
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
        MpFactoryProductionVersion previousVersion = monthProductionDataService.getFinalVersion(factoryCode, year, month);
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
        List<ContinueProductInfo> continueProductionInfoList = monthProductionDataService.getContinueProductionInfo(factoryCode,
                year, month, lastDay);
        // 获取续作结构--结构转产表
        Map<String, Set<String>> continueGroupInfo = getContinueGroupInfo(context, previousVersion, lastDay);
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
     * @param context         排产上下文
     * @param previousVersion 前一个定稿版本信息
     * @param lastDay         最后一天
     * @return
     */
    private Map<String, Set<String>> getContinueGroupInfo(Context context, MpFactoryProductionVersion previousVersion, Integer lastDay) {
        List<ContinueGroupInfo> continueGroupInfoList = monthProductionDataService.getContinueGroupInfo(previousVersion, lastDay);
        TbrBeforeProductionGroupLogRecorder.addReadContinueGroupDataLog(context, continueGroupInfoList);
        if (CollectionUtils.isEmpty(continueGroupInfoList)) {
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
     * @param continueSkuInfo   续作的SKU规格
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
//            TbrBeforeProductionGroupLogRecorder.addContinueGroupNoOnLineMachineLog(context, groupName, continueSku.getMaterialDesc(), onLineMachineSet);
            continueSku.setContinueCxMachineCodeSet(onLineMachineSet);
        });
    }

    /**
     * 获取结构+主花纹的模具分配比例控制信息
     *
     * @param context
     * @return
     */
    private Map<String, MouldAllocationInfoVo> getGroupMainPatternAllocationInfo(Context context) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        List<MouldAllocationInfoVo> mouldAllocationInfoList = getDataService()
                .getMouldAllocationInfo(productionContext);
//        log.info(TbrBeforeProductionGroupLogRecorder.addReaderMouldAllocationLog(context, mouldAllocationInfoList));
        if (CollectionUtils.isEmpty(mouldAllocationInfoList)) {
            return Collections.emptyMap();
        }
        Map<String, MouldAllocationInfoVo> groupMainPatternMap = mouldAllocationInfoList.stream().collect(Collectors
                .toMap(MouldAllocationInfoVo::getDuplicateKey, Function.identity(), (before, after) -> after));
        // 根据排产周期，转换成每日量控制
        Set<Integer> productionDaySet = context.getProductionDay();
        groupMainPatternMap.forEach((controlDimension, allocationInfo) -> {
            Map<Integer, MouldAllocationDayInfoHelper> dayLimitInfoMap = new HashMap<>(productionDaySet.size());
            productionDaySet.forEach(productionDay -> {
                MouldAllocationDayInfoHelper dayLimit = MouldAllocationDayInfoHelper.buildInit(controlDimension,
                        productionDay, allocationInfo.getAllocationQty());
                dayLimitInfoMap.put(productionDay, dayLimit);
            });
            allocationInfo.setDayLimitInfoMap(dayLimitInfoMap);
        });
        return groupMainPatternMap;
    }

    /**
     * 2.1.2：根据排产信息，获取特殊原材料的配置信息 包含：
     * 1、特殊原材料的胎胚
     * 2、特殊原材料的库存及可转化的轮胎条数
     *
     * @param productionContext 排产单位
     */
    private void specialMaterialInfoHandler(TbrProductionContext productionContext) {
        List<EmbryoSpecialMaterialInfoVo> specialMaterialInfoList = getDataService().getEmbryoSpecialMaterialInfo(productionContext);
        Map<String, Map<String, BigDecimal>> embryoSpecialMaterialMap = new HashMap<>();
        Map<String, Map<Long, SpecialMaterialInfoVo>> specialMaterialInfoMap = new HashMap<>();
//        log.info(TbrBeforeProductionGroupLogRecorder.addReaderSpecialMaterialLog(productionContext, specialMaterialInfoList));
        if (CollectionUtils.isEmpty(specialMaterialInfoList)) {
            productionContext.getBaseDataContainer().setEmbryoSpecialMaterialInfoMap(embryoSpecialMaterialMap);
            productionContext.setSpecialMaterialInfoMap(specialMaterialInfoMap);
            return;
        }
        //转化胎胚号-特殊材料
        Map<String, List<EmbryoSpecialMaterialInfoVo>> allSpecialMaterialMap = specialMaterialInfoList.stream().collect(Collectors.groupingBy(EmbryoSpecialMaterialInfoVo::getEmbryoCode));
        allSpecialMaterialMap.forEach((embryoCode, rawMaterialList) -> {
            if (CollectionUtils.isEmpty(rawMaterialList)) {
                return;
            }
            Map<String, BigDecimal> rawMaterialConfigurationMap = embryoSpecialMaterialMap.get(embryoCode);
            if (null == rawMaterialConfigurationMap) {
                rawMaterialConfigurationMap = new HashMap<>();
                embryoSpecialMaterialMap.put(embryoCode, rawMaterialConfigurationMap);
            }
            for (EmbryoSpecialMaterialInfoVo embryoSpecialMaterialInfo : rawMaterialList) {
                String specialMaterialCode = embryoSpecialMaterialInfo.getChildMaterialCode();
                if (StringUtils.isBlank(specialMaterialCode)) {
                    continue;
                }
                BigDecimal dosage = embryoSpecialMaterialInfo.getDosage();
                rawMaterialConfigurationMap.put(specialMaterialCode, dosage);
            }
        });
        productionContext.getBaseDataContainer().setEmbryoSpecialMaterialInfoMap(embryoSpecialMaterialMap);
        //构建特殊原材料库存信息
        specialMaterialStockHandler(productionContext);
        // 初始化特殊材料结构关系表
        productionContext.setSpecialMaterialStructureRelationMap(new HashMap<>());
    }

    /**
     * 2.1.2.1：构建特殊原材料的库存信息对象
     *
     * @param productionContext
     */
    private void specialMaterialStockHandler(TbrProductionContext productionContext) {
        //获取特殊材料库存信息
        List<SpecialMaterialStockVo> specialMaterialStockList = getDataService().getSpecialMaterialStockInfo(productionContext);
//        log.info(TbrBeforeProductionGroupLogRecorder.addReaderSpecialMaterialStockLog(productionContext, specialMaterialStockList));
        if (CollectionUtils.isEmpty(specialMaterialStockList)) {
            productionContext.setSpecialMaterialInfoMap(new HashMap<>());
            return;
        }
        Map<String, Map<Long, SpecialMaterialInfoVo>> specialMaterialInfoMap = new HashMap<>();
        //构建库存对应的可生产量
        specialMaterialStockList.forEach(specialMaterialStockInfo -> {
            String specialMaterialCode = specialMaterialStockInfo.getMaterialCode();
            if (StringUtils.isBlank(specialMaterialCode)) {
                return;
            }
            Map<Long, SpecialMaterialInfoVo> standardLengthMap = specialMaterialInfoMap.get(specialMaterialCode);
            if (null == standardLengthMap) {
                standardLengthMap = new HashMap<>();
                specialMaterialInfoMap.put(specialMaterialCode, standardLengthMap);
            }
            standardLengthMap.put(specialMaterialStockInfo.getStandardLength(), SpecialMaterialInfoVo.createInitInfo(specialMaterialStockInfo));
        });
        productionContext.setSpecialMaterialInfoMap(specialMaterialInfoMap);
    }

    /**
     * 获取需要排产的SKU的施工配置信息
     * key = materialCode: value = List<MonthPlanProductConstructionInfoVo>
     *
     * @param productionContext
     * @return
     */
    protected Map<String, List<MonthPlanProductConstructionInfoVo>> getProductionConstructionInfo(TbrProductionContext productionContext) {
        List<MonthPlanProductConstructionInfoVo> constructionInfoList = getDataService().getProductionConstructionInfo(productionContext);
        if (CollectionUtils.isEmpty(constructionInfoList)) {
//            log.info(TbrProductionInitLogRecorder.addConstructionInfoEmptyLog(productionContext));
            return Collections.emptyMap();
        }
        return constructionInfoList.stream().collect(Collectors.groupingBy(MonthPlanProductConstructionInfoVo::getMaterialCode));
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
    
    /**
     * 往上下文临时日志添加日志信息
     * @param productionContext
     * @param logMessage
     */
    private void addTempLog(TbrProductionContext productionContext, String logMessage) {
        log.info(logMessage);
        productionContext.getTempLogBuilder().append(logMessage).append(System.lineSeparator());
    }
    
    /**
     * 更新搭配排产日志
     *
     * @param context
     */
    private void updateMatchingProductionLog(TbrProductionContext context) {
        // 1、从临时日志获取搭配排产的日志
        String tempLogContent = context.getTempLogBuilder().toString();
        if (StringUtils.isBlank(tempLogContent)) {
            return;
        }
        // 2、获取月计划原先的排产日志记录
        MouldProductionLog logInfo = CollectionUtils.firstElement(baseDao.selectByMap(MouldProductionLog.class,
                Collections.singletonMap("PRODUCTION_VERSION", context.getProductionVersion())));
        if (logInfo == null) {
            return;
        }
        // 3、搭配日志拼接到原日志后
        String newLogContent = new StringBuilder(logInfo.getLogContent()).append(tempLogContent).toString();
        logInfo.setLogContent(newLogContent);
        baseDao.update(logInfo);
    }

    /**
     * 数据服务
     *
     * @return
     */
    public ProductionMdmDataService getDataService() {
        return productionSchedulingDataService;
    }
}
