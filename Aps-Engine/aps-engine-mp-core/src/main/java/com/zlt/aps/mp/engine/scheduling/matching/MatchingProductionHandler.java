package com.zlt.aps.mp.engine.scheduling.matching;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.zlt.aps.mp.engine.daylimit.MouldAllocationInfoVo;
import com.zlt.aps.mp.engine.handler.CalculateStructureCxMachineNumber;
import com.zlt.aps.mp.engine.mapper.FactoryMonthPlanMouldDayDetailMapper;
import com.zlt.aps.mp.engine.mapper.MpStructureAllocationMapper;
import com.zlt.aps.mp.engine.scheduling.cxcapacity.ProductionCapacityParamConfiguration;
import com.zlt.aps.mp.engine.service.DpRequireDataService;
import com.zlt.aps.mp.engine.service.MonthProductionDataService;
import com.zlt.aps.mp.engine.utils.MouldRelationDeduplicator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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
import com.zlt.aps.mp.engine.capacity.MpAdjustDailyCapacityLimit;
import com.zlt.aps.mp.engine.check.SkuSecondChecker;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.daylimit.DayCapacityLimitHelper;
import com.zlt.aps.mp.engine.daylimit.DayCapacityLimitVo;
import com.zlt.aps.mp.engine.daylimit.GroupPlanCxLhCapacityLimitHelper;
import com.zlt.aps.mp.engine.daylimit.MouldAllocationDayInfoHelper;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.ContinueGroupInfo;
import com.zlt.aps.mp.engine.domain.dto.ContinueProductInfo;
import com.zlt.aps.mp.engine.domain.dto.CxContinueInfoHelper;
import com.zlt.aps.mp.engine.domain.dto.CxContinueSkuInfoHelper;
import com.zlt.aps.mp.engine.domain.dto.CxLhProductionHelper;
import com.zlt.aps.mp.engine.domain.dto.CxMouldDayProductionHelper;
import com.zlt.aps.mp.engine.domain.dto.LhProductionQtyHelper;
import com.zlt.aps.mp.engine.domain.dto.MatchingMouldDayUsedHelper;
import com.zlt.aps.mp.engine.domain.dto.MatchingPlanLimitHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.domain.vo.CycleStructureMinLhMachineQtyVo;
import com.zlt.aps.mp.engine.domain.vo.EmbryoSpecialMaterialInfoVo;
import com.zlt.aps.mp.engine.domain.vo.MatchingProductionAdjuestVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductConstructionInfoVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductLhCapacityVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductMouldInfoVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanStructureLhRatioVo;
import com.zlt.aps.mp.engine.domain.vo.ProductionDayInfoVo;
import com.zlt.aps.mp.engine.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.mp.engine.domain.vo.SpecialMaterialInfoVo;
import com.zlt.aps.mp.engine.domain.vo.SpecialMaterialStockVo;
import com.zlt.aps.mp.engine.enums.DayVulcanizationModeEnum;
import com.zlt.aps.mp.engine.enums.ProductionQtyModelEnum;
import com.zlt.aps.mp.engine.handler.CxLhMouldProductionCalculator;
import com.zlt.aps.mp.engine.handler.DayProductionStatisticsHandler;
import com.zlt.aps.mp.engine.handler.MouldProductionResultHandler;
import com.zlt.aps.mp.engine.logrecorder.TbrProductionInitLogRecorder;
import com.zlt.aps.mp.engine.mapper.FactoryMouldingDayResultMapper;
import com.zlt.aps.mp.engine.mapper.MonthPlanRequireMapper;
import com.zlt.aps.mp.engine.scheduling.BaseDataContainer;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.engine.scheduling.cxcapacity.SkuNeedProductionInfo;
import com.zlt.aps.mp.engine.scheduling.init.ProductionInitParamConfiguration;
import com.zlt.aps.mp.engine.service.ProductionMdmDataService;
import com.zlt.aps.mp.engine.utils.ProductionCycleUtils;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.MdmSkuConstructionRefEntityMapper;
import com.zlt.aps.maindata.mapper.MdmSkuLhCapacityEntityMapper;
import com.zlt.aps.maindata.mapper.MpMonthPlanStatisticsEntityMapper;
import com.zlt.aps.mp.api.domain.capacity.MpDailyCapacityLimitVo;
import com.zlt.aps.mp.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.mp.api.domain.entity.DpDemandPlan;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayDetail;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.api.domain.entity.MdmProductStock;
import com.zlt.aps.mp.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.aps.mp.api.domain.entity.MdmSkuLhCapacity;
import com.zlt.aps.mp.api.domain.entity.MdmWorkCalendar;
import com.zlt.aps.mp.api.domain.entity.MpAdjustStructureIn;
import com.zlt.aps.mp.api.domain.entity.MpAdjustStructureOut;
import com.zlt.aps.mp.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.mp.api.domain.entity.MpMonthPlanStatistics;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.api.domain.vo.DailyMouldAvailabilityResult;
import com.zlt.aps.mp.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.utils.SpringBeanUtils;
import com.zlt.core.dao.basedao.BaseDao;

import lombok.extern.slf4j.Slf4j;

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
    private FactoryMouldingDayResultMapper factoryMouldingDayResultMapper;
    @Autowired
    private FactoryMonthPlanMouldDayDetailMapper factoryMonthPlanMouldDayDetailMapper;
    @Autowired
    private MonthPlanRequireMapper monthPlanRequireMapper;
    @Autowired
    private MpStructureAllocationMapper mpStructureAllocationMapper;
    @Autowired
    private MdmSkuLhCapacityEntityMapper mdmSkuLhCapacityEntityMapper;
    @Autowired
    private MpMonthPlanStatisticsEntityMapper mpMonthPlanStatisticsEntityMapper;
    @Autowired
    private BaseDao baseDao;
    @Autowired
    private DpRequireDataService dpRequireDataService;
    @Autowired
    private MonthProductionDataService monthProductionDataService;
    @Autowired
    private DayProductionStatisticsHandler dayProductionStatisticsHandler;
    @Autowired
    private CalculateStructureCxMachineNumber calculateStructureCxMachineNumber;
    @Autowired
    private MdmSkuConstructionRefEntityMapper mdmSkuConstructionRefEntityMapper;
    @Autowired
    private ISysConfigService sysConfigService;
    /**
     * 月份天数上限
     */
    private final static int MAX_MONTH_DAY = 31;
    
    @Value("${debug.ignorSkip.matching:false}")
    private Boolean isIgnorSkip;

    /**
     * 搭配排产（已排产结果入口）
     *
     * @param productionVersion 生产版本
     */
    public void matchingProduction(String productionVersion) {
        try {
            String config = sysConfigService.selectConfigByKey("monthPlan.skip.matching");
            if (!isIgnorSkip && StringUtils.isNotBlank(config) && Boolean.parseBoolean(config)) {
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
        List<MonthPlanProductionRequirePlanVo> requirePlanList = this.selectRequirePlan(productionContext, planList, detailLogList); // 查询需求计划
        this.buildProductionContext(productionContext, planList, detailLogList, requirePlanList); // 填充上下文各项必要数据

        Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap = calculateStructureCxMachineNumber.calculateStructureCxMachineNumber(productionContext, requirePlanList); // 分配成型产能
        productionContext.setGroupProductionInfo(estimateGroupCxAllocationMap);
        this.resetBeforeFormalProduction(productionContext, estimateGroupCxAllocationMap);
        Map<String, CxContinueInfoHelper> cxContinueInfoMap = this.getContinueInfo(productionContext);

        // 调用主流程的入口 -> 搭配排程算法
        Map<String, Integer> newSkuQtyMap = this.matchingProduction(productionContext, estimateGroupCxAllocationMap,
                cxContinueInfoMap);

        // 构建排产结果并保存
        this.saveMouldProductionResult(productionContext, planList, detailLogList, newSkuQtyMap);
    }
    
    /**
     * 初始化月计划调整的必要数据
     * @param contextDTO
     */
    public void initAdjustContextDTO(MpRollAdjustContextDTO contextDTO) {
        TbrProductionContext productionContext = this.initProductionContext(contextDTO); // 初始化上下文
        this.getMdmProductStock(contextDTO, productionContext);
        this.getSkuLhCapacity(contextDTO);; // 日硫化产能表，key:物料描述
        this.getMdmSkuConstructionRefMap(contextDTO); // 获取SKU与施工关系，key：物料号
    }

    /**
     * 周程滚动的结构内搭配算法
     * 
     * @param contextDTO              周程滚动调整上下文
     * @param mpProdFinalList         月计划定稿表列表（只有当前结构的记录）
     * @param isInner                 是否结构内调整
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
        
        // 结构排产的开始不能早于锁定日的校验
        Integer startDay = contextDTO.getStartDay();
        Integer endDay = contextDTO.getEndDay();
        Integer lockEndDay = contextDTO.getLockEndDay();
        Integer realBeginDay = Math.max(lockEndDay + 1, startDay);
        if (endDay <= lockEndDay) { // 结束日在锁定日结束前的结构不搭配
            return;
        }
        // 特殊材料可搭配量校验（只有结构内需要考虑）
        boolean isSpecial = isInner && Optional.ofNullable(contextDTO.getSpecStructureTotalQty()).orElse(0) > 0;
        Integer remaindSpecQty = 0;
        if (isSpecial) {
            Integer totalQty = mpProdFinalList.stream().filter(p -> p.getTotalQty() != null).mapToInt(FactoryMonthPlanFinalAdjustVo::getTotalQty).sum();
            remaindSpecQty = contextDTO.getSpecStructureTotalQty() - totalQty;
            if (remaindSpecQty <= 0) {
                return;
            }
        }
        // 取调整需求计划
        List<DpDemandPlan> demandPlanList = this.loadAdjustDemandPlanList(contextDTO);
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
        do {
            // 先执行模具续作分配
            this.doAllocationAdjuest(contextDTO, productionContext, mpProdFinalList, mpProdFinalMap, demandPlanList, dayProductionMap,
                    realBeginDay, endDay, isSpecial, remaindSpecQty, true);
            // 再执行新增模具分配
            int totalAllocationQty = this.doAllocationAdjuest(contextDTO, productionContext, mpProdFinalList, mpProdFinalMap, demandPlanList, dayProductionMap,
                    realBeginDay, endDay, isSpecial, remaindSpecQty, false);
            if (isSpecial && totalAllocationQty > 0) {
                remaindSpecQty -= totalAllocationQty; // 特殊结构，更新剩余可分配量量
            }
            if (totalAllocationQty <= 0) { // 没有搭配，结束
                break;
            }
        } while (true);
        log.info("周程滚动搭配算法end");
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
                                                                          List<DpDemandPlan> demandPlanList) {
        Map<String, MdmSkuLhCapacity> mdmSkuLhCapacityMap = this.getSkuLhCapacity(contextDTO); // 日硫化产能表，key:物料描述
        Map<String, MdmSkuConstructionRef> mdmSkuConstructionRefMap = this.getMdmSkuConstructionRefMap(contextDTO); // 获取SKU与施工关系，key：物料号
        // 构建Map
        Map<String, FactoryMonthPlanFinalAdjustVo> mpProdFinalMap = mpProdFinalList.stream()
                .filter(p -> StringUtils.isNotEmpty(p.getMaterialDesc())).collect(Collectors
                        .toMap(FactoryMonthPlanFinalAdjustVo::getMaterialDesc, Function.identity(), (p1, p2) -> p1)); // key：规格描述
        
        for (DpDemandPlan demandPlan : demandPlanList) {
            FactoryMonthPlanFinalAdjustVo plan = mpProdFinalMap.get(demandPlan.getMaterialDesc()); // 获取排产结果
            if (plan != null) {
                continue;
            }
            // 如果没有，说明是新增规格，需要新增记录
            FactoryMonthPlanFinalAdjustVo firstPlan = CollectionUtils.firstElement(mpProdFinalList);
            plan = new FactoryMonthPlanFinalAdjustVo();
            if (firstPlan != null) {
                SpringBeanUtils.copyPropertiesIgnoreNull(firstPlan, plan);
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
            this.reCaculateInventorySalesRatio(contextDTO, plan,
                    Collections.singletonMap(demandPlan.getMaterialCode(), demandPlan.getStockQty())); // 计算库销比
            for (int day = 1; day <= MAX_MONTH_DAY; day++) {
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
     * @param demandPlanList    需求计划列表
     * @param dayProductionMap  日生产量统计列表
     * @param beginDay          结构排产开始日期
     * @param endDay            结构排产结束日期
     * @param isSpecial         是否特殊结构
     * @param remaindSpecQty    特殊结构剩余量
     * @param isCheckContinue   是否处理续作，每个SKU都是从续作开始检查
     * @return
     */
    private int doAllocationAdjuest(MpRollAdjustContextDTO contextDTO, TbrProductionContext productionContext,
                           List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList,
                           Map<String, FactoryMonthPlanFinalAdjustVo> mpProdFinalMap, List<DpDemandPlan> demandPlanList,
                           Map<Integer, List<MatchingProductionAdjuestVo>> dayProductionMap, Integer beginDay,
                           Integer endDay, boolean isSpecial, int remaindSpecQty,
                           boolean isCheckContinue) {
        int totalAllocationQty = 0; // 本次遍历总搭配量
        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitMap = contextDTO.getDailyCapacityLimitVoMap(); // 每日产能统计
        Set<String> scheduleMaterialDesc = new HashSet<>(); // 记录已排规格，防止重复执行死循环
        do {
            // 获取最高优先级的可搭配调整规格
            String materialDesc = this.getHeightPriorityAdjuestMaterial(demandPlanList, mpProdFinalMap,
                    productionContext, scheduleMaterialDesc);
            if (StringUtils.isEmpty(materialDesc)) {
                break;
            }
            scheduleMaterialDesc.add(materialDesc); // 选中的规格加入已排产列表（无论是否能排上，下次轮询均不再处理该规格）
            List<DpDemandPlan> needProductPlanList = demandPlanList.stream()
                    .filter(p -> materialDesc.equals(p.getMaterialDesc())).collect(Collectors.toList());
            FactoryMonthPlanFinalAdjustVo plan = mpProdFinalMap.get(materialDesc); // 获取定稿计划
            int capacity = plan.getDayVulcanizationQty() * ProductionConstant.DOUBLE_MOULD_PRODUCTION; // 产能都从final表获取
            boolean isNewPlan = plan.getBeginDay() == null;
            
            int unAllocationQty = needProductPlanList.stream().mapToInt(DpDemandPlan::getConventionReserveQty).sum(); // 未搭配量 = 储备池的量
            unAllocationQty = isSpecial? Math.min(unAllocationQty, remaindSpecQty): unAllocationQty; // 如果包含特殊材料，不能超过特殊材料的总数量
            List<FactoryMonthPlanFinalAdjustVo> safeList = new ArrayList<>();
            int lastProductDay = 0; // 本SKU的收尾日
            for (int day = endDay; day >= beginDay; day --) {
                if (Optional.ofNullable((Integer) plan.getFieldValueByFieldName(FactoryConstant.DAY_FIELD + day)).orElse(0) > 0) {
                    lastProductDay = day;
                    break;
                }
            }
            Integer realEndDay = lastProductDay; // 实际结束日期，如果SKU提前结束，则需要把结束日期也提前，防止下一次轮询后出现断层
            safeList.addAll(mpProdFinalList);
            if (isNewPlan) {
                safeList.add(plan);
            }

            out: do {
                int startUnAllocationQty = unAllocationQty;
                boolean isBegin = false; // 是否已经开始i排产的标记
                for (int day = beginDay; day <= realEndDay; day++) { // 遍历结构排产日，如果锁定日超过开始i日期，从锁定日下一天开始
                    // 是主销产品，切剩余天数在可搭配补量的天数范围内，需要补量
                    if (unAllocationQty <= 0) {
                        break out;
                    }
                    if (!this.checkDayCanProduct(contextDTO, day)) { // 检查生产日历，停产日不处理
                        continue;
                    }
                    this.reCalcAdjustDailyCapacityLimit(contextDTO, safeList, plan, day); // 先重算产能限制
                    // 检查模具是否有剩余产能
                    MpDailyCapacityLimitVo dailyCapacityLimitVo = dailyCapacityLimitMap.get(day);
                    Integer mouldRemaindCapacity = this.getMouldRemaindCapacity(contextDTO, plan, capacity, day, beginDay, realEndDay, dayProductionMap, dailyCapacityLimitMap); // 获取模具剩余产能
                    if (mouldRemaindCapacity <=0 && dailyCapacityLimitVo.getMaxLhMachines() <= dailyCapacityLimitVo.getUsedLhMachines()) { // 如果模具产能已满，且当天硫化机已经满足条件，则直接跳过
                        if (isBegin) { // 防止中断不连续的问题出现
                            realEndDay = day; // 中断后记录当前日期作为下一次轮询的结束日
                            break;
                        }
                        continue;
                    }
                    // 检查胎胚数是否满足条件
                    if (dailyCapacityLimitVo.getMaxEmbryoTypes() <= dailyCapacityLimitVo.getUsedEmbryoTypes()) { // 胎胚数已达上限，则不能继续添加新胎胚
                        Set<String> embryoCodes = dailyCapacityLimitVo.getEmbryoCodes();
                        if (!embryoCodes.contains(plan.getEmbryoCode())) {
                            if (isBegin) { // 防止中断不连续的问题出现
                                realEndDay = day; // 中断后记录当前日期作为下一次轮询的结束日
                                break;
                            }
                            continue;
                        }
                    }
                    // 检查当天如果搭配是否会导致二次上机
                    if (!isNewPlan && !this.checkSecOnlineAdjuest(contextDTO, plan, day)) { // 非新增SKU需要检查二次上机
                        continue;
                    }
                    
                    // 为当天分配搭配量
                    int allocationQty = this.allcatAdjustProductQty(contextDTO, day, beginDay, realEndDay, plan, safeList,
                            dayProductionMap, unAllocationQty, capacity, dailyCapacityLimitMap, mouldRemaindCapacity, isCheckContinue);
                    if (allocationQty > 0) { // 有分配量，说明成功搭配排产，需要更新相关数据
                        if (mouldRemaindCapacity == 0) {
                            isBegin = true; // 非补模具余量的，才需要标记为开始
                        }
                        totalAllocationQty += allocationQty;
                        unAllocationQty -= allocationQty;
                        if (lastProductDay < day) { // 如果延后了SKU的收尾日，更新收尾日
                            lastProductDay = day;
                        }
                        this.reCalcAdjustDailyCapacityLimit(contextDTO, safeList, plan, day); // 有搭配，则再次重算产能限制
                    } else if (isBegin) { // 防止中断不连续的问题出现
                        realEndDay = day; // 中断后记录当前日期作为下一次轮询的结束日
                        break;
                    }
                }
                if (startUnAllocationQty == unAllocationQty) { // 如果遍历后没有发生变化
                    // 如果结束日期在SKU收尾日与结构收尾日之间，则把结束期推后一天再尝试排产一次。目的是在SKU收尾日搭配满后，才尝试往后一天搭配
                    if (realEndDay >= lastProductDay && realEndDay < endDay) {
                        Integer nextDay = this.getNextDay(contextDTO, realEndDay, endDay);
                        if (nextDay > 0) {
                            realEndDay = nextDay;
                            continue;
                        }
                    }
                    break out; // 结束本规格的搭配
                }
            } while (true);
            if (isNewPlan && plan.getBeginDay() != null) { // 排上的规格添加导列表中
                mpProdFinalList.add(plan);
            }
        } while (true);
        return totalAllocationQty;
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
        // 1、检查当天排产情况
        Integer dayProductQty = Optional.ofNullable((Integer) plan.getFieldValueByFieldName(FactoryConstant.DAY_FIELD + checkDay)).orElse(0);
        if (dayProductQty > 0) { // 如果当天没有排产才需要继续检查是否引起二次上机
            return isSecOnLine;
        }
        Integer skuSecondProduction = (Integer) contextDTO.getParamMap().get(MonthPlanEnums.SKU_SECOND_PRODUCTION.getCode());
        Integer skuBeginDay = plan.getBeginDay();
        Integer skuEndDay = plan.getEndDay();
        // 2、向前看是否有超出二次上机限制
        for (Integer i = checkDay - 1; i >= skuBeginDay; i--) {
            if (i == 0){
                break;
            }
            Integer checkDayProductQty = Optional.ofNullable((Integer) plan.getFieldValueByFieldName(FactoryConstant.DAY_FIELD + i)).orElse(0);
            if (checkDayProductQty > 0) { // 如果有排产，则检查是否超过限制
                isSecOnLine = checkDay - checkDayProductQty - 1 <= skuSecondProduction;
                break;
            }
        }
        if (!isSecOnLine) { // 校验不通过，则直接结束
            return isSecOnLine;
        }

        // 3、先向后看是否有超出二次上机限制
        for (Integer i = checkDay + 1; i <= skuEndDay; i++) {
            Integer checkDayProductQty = Optional.ofNullable((Integer) plan.getFieldValueByFieldName(FactoryConstant.DAY_FIELD + i)).orElse(0);
            if (checkDayProductQty > 0) { // 如果有排产，则检查是否超过限制
                isSecOnLine = checkDayProductQty - checkDay - 1 <= skuSecondProduction;
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
        BigDecimal inventorySalesRatio = Optional.ofNullable(plan.getInventorySalesRatio()).orElse(BigDecimal.ZERO);
        int averageSaleQty1 = Optional.ofNullable(plan.getAverageSaleQty()).orElse(0);
        int stock1 = stockMap.getOrDefault(plan.getMaterialDesc(), 0);
        int planQty1 = this.getSumPlanQtyLockEndDay(plan, lockEndDay);
        if (averageSaleQty1 != 0) {
            inventorySalesRatio = BigDecimalUtils.div(stock1 + planQty1, averageSaleQty1);
        }
        plan.setInventorySalesRatio(inventorySalesRatio);
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
        Integer specStructureTotalQty = Optional.ofNullable(contextDTO.getSpecStructureTotalQty()).orElse(0);
        boolean isSpecial = specStructureTotalQty > 0;
        Integer unAllocatSpecStructureQty = this.getUnAllocatSpecStructureQty(specStructureTotalQty, mpProdFinalList);
        if (isSpecial && unAllocatSpecStructureQty <= 0) {
            return;
        }
        // 加载上下文中的各项必要数据
        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitMap = contextDTO.getDailyCapacityLimitVoMap(); // 每日产能统计
        Integer startDay = contextDTO.getStartDay();
        Integer endDay = contextDTO.getEndDay();
        Integer realStartDay = Math.max(startDay, endDay - (matchingBoostDay - 1)); // 实际开始日期要看补量天数的设置，哪个晚用哪个
        Integer bootsQty =  0; // 总补量
        
        out:
        for (FactoryMonthPlanFinalAdjustVo plan: mpProdFinalList) {
            Integer actualAdjustQty = Optional.ofNullable(plan.getActualAdjustQty()).orElse(0); // 实际调整量
            if (!boostProductionTypeSet.contains(plan.getProductionType())) { // 非主销规格不补量
                continue;
            }
            if (actualAdjustQty <= 0) { // 减量或者不调整的SKU不补量
                continue;
            }
            // 统计补量日期各天排产量
            Map<Integer, Integer> dayProductionQtyMap = this.getDayProductionQtyMap(plan, realStartDay, endDay);
            // 补量期间任意一天有量，都需要补量
            if (dayProductionQtyMap.values().stream().noneMatch(qty -> qty > 0)) {
                continue;
            }
            // 统计在产硫化机数
            Map<Integer, Integer> bootsDayLhMachineMap = this.getBootsDayLhMachineMap(contextDTO, plan, realStartDay,
                    endDay, dailyCapacityLimitMap); // 每日已使用硫化机数
            // 统计最大补量机台数，以包括补量天以及前一天的最大硫化机为准
            Integer maxLhMachineCount = bootsDayLhMachineMap.values().stream().max(Integer::compareTo).orElse(0);
            if (maxLhMachineCount <= 0) {
                continue;
            }
            
            // 遍历补量开始日到收尾日之间的生产量，并尝试开始补量
            for (int day = realStartDay; day <= endDay; day++) {// 根据日产比例限制产能
                // 如果当天有排产，在不加模的前提下检查是否已经占满
                MpDailyCapacityLimitVo dailyCapacityLimit = dailyCapacityLimitMap.get(day);
                if (dailyCapacityLimit == null) {
                    continue;
                }
                Integer useMachineCount = bootsDayLhMachineMap.getOrDefault(day, 0);
                Integer remainMachineCount = dailyCapacityLimit.getMaxLhMachines() - dailyCapacityLimit.getUsedLhMachines(); // 当天剩余可用机台，超了就是负数（异常情况）
                Integer bootsMachineCount = remainMachineCount + useMachineCount; // 补量相关机台数 = 剩余机台 + 可用机台
                bootsMachineCount = bootsMachineCount > 0? bootsMachineCount: 0;
                Integer productionQty = dayProductionQtyMap.getOrDefault(day, 0); // 当天已排产量
                int capacity = plan.getDayVulcanizationQty() * ProductionConstant.DOUBLE_MOULD_PRODUCTION; // 单机产能
                Integer allocationQty = capacity * Math.min(maxLhMachineCount, bootsMachineCount) - productionQty; // 计算分配量 = 产能 *机台 - 已排量
                Integer realAllocationQty = isSpecial? Math.min(unAllocatSpecStructureQty, allocationQty): allocationQty; // 如果是特殊材料需要控制不能超过总量
                if (realAllocationQty <= 0) {
                    continue;
                }
                Integer realProductionQty = productionQty + realAllocationQty;
                plan.setFieldValueByFieldName(FactoryConstant.DAY_FIELD + day, realProductionQty);
                plan.setTotalQty(plan.getTotalQty() + realAllocationQty);
                plan.setEndDay(plan.getEndDay() < day? day: plan.getEndDay());
                // 更新各项统计数据
                bootsQty += realAllocationQty;
                unAllocatSpecStructureQty -= realAllocationQty; // 更新待分配特殊材料总数
                dayProductionQtyMap.put(day, realProductionQty); // 更新当天排产量统计
                contextDTO.getLogDetail().append(String.format("结构:%s,【收尾补量】物料编码:%s,排产日:%s,补量:%s",contextDTO.getStructureName(),plan.getMaterialCode(),day,realAllocationQty)).append(ApsConstant.DIVISION); // 记录日志
                // 如果是特殊结构，且特殊结构已分配完，则结束
                if (isSpecial && unAllocatSpecStructureQty <= 0) {
                    break out;
                }
            }
        }
        if (isSpecial) {
            contextDTO.setSpecStructureTotalQty(specStructureTotalQty - bootsQty);
        }
    }

    /**
     * 获取特殊结构待分配量
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
    private Map<Integer, Integer> getBootsDayLhMachineMap(MpRollAdjustContextDTO contextDTO, FactoryMonthPlanFinalAdjustVo plan,
                                              Integer startDay, Integer endDay,
                                              Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitMap) {
        Map<Integer, Integer> bootsDayLhMachineMap = new HashMap<>();
        for (int day = startDay; day <= endDay; day ++) {
            bootsDayLhMachineMap.put(day, this.getDayUsedLhMachines(contextDTO, plan, day, dailyCapacityLimitMap));
        }
        return bootsDayLhMachineMap;
    }

    /**
     * 统计各天排产量
     * @param plan
     * @param startDay
     * @param endDay 
     * @return
     */
    private Map<Integer, Integer> getDayProductionQtyMap(FactoryMonthPlanFinalAdjustVo plan, Integer startDay,
                                                         Integer endDay) {
        Map<Integer, Integer> dayProductionQtyMap = new HashMap<>();
        for (int day = startDay - 1; day <= endDay; day++) {
            if (day > 0) {
                dayProductionQtyMap.put(day, Optional.ofNullable((Integer)plan.getFieldValueByFieldName(FactoryConstant.DAY_FIELD + day)).orElse(0));
            }
        }
        return dayProductionQtyMap;
    }

    /**
     * 获取新的型腔数量
     * @param contextDTO 周程滚动上下文
     * @param mpFinalVo 定稿Vo
     * @param iDay 当前天
     * @return 型腔数量
     */
    private int getNewCavityQty(MpRollAdjustContextDTO contextDTO,FactoryMonthPlanFinalAdjustVo mpFinalVo,int iDay){
        DailyMouldAvailabilityResult cavity2BlockVo = contextDTO.getCavity2BlockMap().get(iDay);
        if (cavity2BlockVo != null && cavity2BlockVo.getCavityResults() != null){
            Integer cavityQty = cavity2BlockVo.getCavityResults().get(mpFinalVo.getStructureName()+mpFinalVo.getMainPattern());
            return cavityQty != null ? cavityQty:mpFinalVo.getMouldCavityQty();
        }
        return mpFinalVo.getMouldCavityQty();
    }

    /**
     * 检查模具满足情况
     *
     * @param dailyCapacityLimitVo 产能限制Vo
     * @param cavityQty 型腔数
     * @return true-满足，false-不满足
     */
    private boolean checkMouldSatisfy(MpDailyCapacityLimitVo dailyCapacityLimitVo,int cavityQty){
        //型腔台数
        int patternCount = cavityQty / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        //主花纹向下所有SKU的模具数量 < 主花纹.型腔数量
        return dailyCapacityLimitVo.getPatternUsedLhMachines() < patternCount;
    }

    /**
     * 
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
     * @param contextDTO
     * @param productionContext
     * @return
     */
    private List<MdmProductStock> getMdmProductStock(MpRollAdjustContextDTO contextDTO,
                                                     TbrProductionContext productionContext) {
        List<MdmProductStock> stockList = contextDTO.getMdmProductStockList(); // 库存
        if (CollectionUtils.isEmpty(stockList)) {
            stockList = getDataService().getMdmProductStock(productionContext); // 如果没有需要加载库存
            contextDTO.setMdmProductStockList(stockList);
        }
        return stockList;
    }

    /**
     * 获取模具剩余产能
     * @param contextDTO
     * @param plan
     * @param capacity
     * @param day
     * @param beginDay
     * @param endDay
     * @param dayProductionMap
     * @param dailyCapacityLimitMap
     * @return
     */
    private Integer getMouldRemaindCapacity(MpRollAdjustContextDTO contextDTO,
                                            FactoryMonthPlanFinalAdjustVo plan, int capacity, 
                                            int day, int beginDay, int endDay,
                                            Map<Integer, List<MatchingProductionAdjuestVo>> dayProductionMap,
                                            Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitMap) {
        String materialDesc = plan.getMaterialDesc();
        Integer remaindCapacity = 0;
        // 1、检查当天排产情况
        MatchingProductionAdjuestVo dayProduction = this.getMatchingProductionAdjuest(materialDesc, day, dayProductionMap);
        if (dayProduction == null) {
            return remaindCapacity;
        }
        Integer changeMouldFirstQty = (Integer) contextDTO.getParamMap()
                .get(MonthPlanEnums.CHANGE_MOULD_FIRST_QTY.getCode()); // 换模首日可排产量
        int realCapacity = dayProduction.getIsFirstDay() ? changeMouldFirstQty : capacity;
        if (realCapacity == 0) {
            return remaindCapacity;
        }
        Integer useCapacity = BigDecimalUtils.valueOf(dayProduction.getProductionQty())
                .remainder(BigDecimalUtils.valueOf(realCapacity)).intValue(); // 计算余数
        if (useCapacity == 1) { // 余数是1只会是因为奇数转偶数引起的，可以忽略
            return remaindCapacity;
        } else if (useCapacity > 0) { // 余数大于0，说明最后一个硫化机没有排满，优先补满逞能剩余的量
            // 2、先判断后续天数是否满产能排产
            Integer nextDay = this.getNextDay(contextDTO, day, endDay);
            if (nextDay > 0) { // 非收尾日，需要判断下一天产能是否占满
                MpDailyCapacityLimitVo dailyCapacityLimitVo = dailyCapacityLimitMap.get(nextDay);
                if (dailyCapacityLimitVo != null && dailyCapacityLimitVo.getMaxLhMachines() == dailyCapacityLimitVo.getUsedLhMachines()) { // 下一天产能占满，则当天不需要搭配补量
                    return remaindCapacity;
                }
            }
            // 3、判断如果今天的硫化机数比昨天多，说明有新增模具，不能补量
            Integer lastDay = this.getLastDay(contextDTO, day, beginDay);
            if (lastDay > 0) {
                Integer lastDayUsedLhMachines = this.getDayUsedLhMachines(contextDTO, plan, lastDay, dailyCapacityLimitMap); // 上一天已使用的硫化机数量
                Integer todayDayUsedLhMachines = this.getDayUsedLhMachines(contextDTO, plan, day, dailyCapacityLimitMap); // 当天已使用的硫化机数量
                if (todayDayUsedLhMachines > lastDayUsedLhMachines) { // 今天的硫化机数比昨天多
                    return remaindCapacity;
                }
            }
            return realCapacity - useCapacity;
        }
        return remaindCapacity;
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
     * @param contextDTO
     * @param day
     * @param endDay
     * @return
     */
    private Integer getNextDay(MpRollAdjustContextDTO contextDTO, int day, int endDay) {
        Integer nextDay = 0;
        for (int i = day + 1; i <= endDay; i ++) {
            if (this.checkDayCanProduct(contextDTO, i)) { // 下一天是排产日返回，否则跳过看下一天
                nextDay = i;
                break;
            }
        }
        return nextDay;
    }

    /**
     * 获取上一个排产日
     * @param contextDTO
     * @param day
     * @param beginDay
     * @return
     */
    private Integer getLastDay(MpRollAdjustContextDTO contextDTO, int day, int beginDay) {
        Integer lastDay = 0;
        for (int i = day - 1; i >= beginDay; i --) {
            if (this.checkDayCanProduct(contextDTO, i)) { // 下一天是排产日返回，否则跳过看下一天
                lastDay = i;
                break;
            }
        }
        return lastDay;
    }

    /**
     * 获取下一个排产日
     * @param contextDTO
     * @param day
     * @param beginDay
     * @return
     */
    private Integer getNextDay(TbrProductionContext productionContext, int day, int endDay) {
        Integer lastDay = 0;
        for (int i = day + 1; i <= endDay; i ++) {
            if (!productionContext.getStopDays().contains(i)) { // 下一天是排产日返回，否则跳过看下一天
                lastDay = i;
                break;
            }
        }
        return lastDay;
    }

    /**
     * 获取上一个排产日
     * @param contextDTO
     * @param day
     * @param beginDay
     * @return
     */
    private Integer getLastDay(TbrProductionContext productionContext, int day, int beginDay) {
        Integer lastDay = 0;
        for (int i = day - 1; i >= beginDay; i --) {
            if (!productionContext.getStopDays().contains(i)) { // 下一天是排产日返回，否则跳过看下一天
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
        if (mdmSkuLhCapacity == null){
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
        if (capacity == null){
            throw new BusinessException(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.monthPlanFinalRecord.notDayLhQty"),
                    materialDesc));
        }
        return capacity;
    }

    /**
     * 加载需求计划
     * @param contextDTO
     * @return
     */
    private List<DpDemandPlan> loadAdjustDemandPlanList(MpRollAdjustContextDTO contextDTO) {
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
        demandQueryWrapper.eq(DpDemandPlan::getStructureName, contextDTO.getStructureName()); // 过滤空结构的数据
//        demandQueryWrapper.gt(DpDemandPlan::getConventionReserveQty, 0);
        List<DpDemandPlan> demandPlanList = monthPlanRequireMapper.selectList(demandQueryWrapper);
        return demandPlanList;
    }
    
    /**
     * 日硫化产能表
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
    private String getHeightPriorityAdjuestMaterial(List<DpDemandPlan> demandPlanList,
                                                    Map<String, FactoryMonthPlanFinalAdjustVo> mpProdFinalMap,
                                                    TbrProductionContext productionContext,
                                                    Set<String> scheduleMaterialDesc) {
        String materialDesc = demandPlanList.stream().filter(p -> !scheduleMaterialDesc.contains(p.getMaterialDesc()))
                .min((p1, p2) -> {
                    if (Objects.equals(p1.getMaterialDesc(), p2.getMaterialDesc())) { // 同规格的，不需要比较
                        return 0;
                    }
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
                }).map(DpDemandPlan::getMaterialDesc).orElse(null);
        return materialDesc;
    }

    /**
     * 取截至锁定日前的排产量汇总值
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
            for (int day = 1; day <= lockEndDay; day ++) {
                sumPlanQty += Optional.ofNullable((Integer)plan.getFieldValueByFieldName(FactoryConstant.DAY_FIELD + day)).orElse(0);
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
     * @param beginDay              结构排产开始日
     * @param endDay                结构排产结束日
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
        int remainDayTotalCapacity = this.getRemainDayTotalCapacity(contextDTO, dailyCapacityLimitVo, scheduleDay);
        if (remainDayTotalCapacity <= 0) {
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
            // 2、当天满足上机条件按，但是下一天不满足上机条件的，也不允许上机
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
            dayProduct.setIsFirstDay(isChangeMould);
            dayProductionList.add(dayProduct);
        }
        
        // 计算排产量
        int allocationQty = capacity; // 本次排产量，默认是双模*模具产能
        if (isChangeMould) { // 如果是换模具，则只能增加首日排产量
            Integer changeMouldFirstQty = new MpAdjustDailyCapacityLimit().getFirstDayQty(
                    contextDTO.getFactoryMonthPlanProdFinalList(), scheduleDay, dailyCapacityLimitVo,
                    contextDTO.getParamMap(), plan.getMainPattern());
            allocationQty = Optional.ofNullable(changeMouldFirstQty).orElse(0); // 每次仅新增一台硫化机
        }
        if (allocationQty <= 0) {
            return 0;
        }
        if (isRemaindCapacity) {
            allocationQty = Math.min(allocationQty, mouldRemaindCapacity); // 如果当天模具有剩余产能的，优先补满
        }
        allocationQty = Math.min(Math.min(allocationQty, unAllocationQty), remainDayTotalCapacity); // 分配量不能超过未分配量以及剩余产能
        if (((dayProduct.getProductionQty() + allocationQty)& 1) != 0) { // 如果原排产量 + 新排产量为奇数，则排产量需要 + 1
            allocationQty ++;
        }
        Integer oldProductionQty = Optional.ofNullable((Integer) plan.getFieldValueByFieldName(FactoryConstant.DAY_FIELD + scheduleDay)).orElse(0);
        plan.setFieldValueByFieldName(FactoryConstant.DAY_FIELD + scheduleDay, allocationQty + oldProductionQty);
        plan.setTotalQty(Optional.ofNullable(plan.getTotalQty()).orElse(0) + allocationQty);
        plan.setConventionProductionQty(Optional.ofNullable(plan.getConventionProductionQty()).orElse(0) + allocationQty);
        plan.setActualAdjustQty(Optional.ofNullable(plan.getActualAdjustQty()).orElse(0) + allocationQty);
        if (plan.getBeginDay() == null) {
            plan.setBeginDay(scheduleDay);
        }
        if (plan.getEndDay() == null || plan.getEndDay() < scheduleDay) {
            plan.setEndDay(scheduleDay);
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
        String logDetail = String.format("结构:%s,【搭配排产】物料编码:%s,排产日:%s,搭配排产量:%s",contextDTO.getStructureName(),plan.getMaterialCode(),scheduleDay,allocationQty);
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
        int remainNextDayTotalCapacity = this.getRemainDayTotalCapacity(contextDTO, nextDailyCapacityLimitVo, nextDay);
        if (remainNextDayTotalCapacity <= 0) {
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
    private int getRemainDayTotalCapacity(MpRollAdjustContextDTO contextDTO,
                                               MpDailyCapacityLimitVo dailyCapacityLimitVo, Integer day) {
        int dayTotalCapacityLimit = Optional.ofNullable(dailyCapacityLimitVo.getMaxDayProductionQty()).orElse(0);
        List<FactoryMonthPlanFinalAdjustVo> mpPlanFinalAdjustList = contextDTO.getFactoryMonthPlanProdFinalList();
        if (CollectionUtils.isEmpty(mpPlanFinalAdjustList)) {
            return dayTotalCapacityLimit;
        }
        // 1.计算检查日的汇总值
        String dayField = FactoryConstant.DAY_FIELD + day;
        int totalPlanQty = mpPlanFinalAdjustList.stream().filter(Objects::nonNull).mapToInt(x -> {
            Object val = x.getFieldValueByFieldName(dayField);
            return val instanceof Number ? ((Number) val).intValue() : 0;
        }).sum();

        // 2.检查日的汇总值 小于等于 日总产能限制
        if (totalPlanQty < dayTotalCapacityLimit) {
            return dayTotalCapacityLimit - totalPlanQty;
        } else {
            return 0;
        }
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
                Integer productionQty = Optional.ofNullable((Integer) plan.getFieldValueByFieldName(FactoryConstant.DAY_FIELD + day)).orElse(0);
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
     * 搭配排产（主流程入口）
     *
     * @param context                      上下文
     * @param estimateGroupCxAllocationMap 需求计划列表
     * @param cxContinueInfoMap            续作规格
     * @return 本次排产各规格描述的排产数量统计
     */
    public Map<String, Integer> matchingProduction(Context context,
                                                   Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap,
                                                   Map<String, CxContinueInfoHelper> cxContinueInfoMap) {
        log.info(context.getProductionVersion() + "搭配排产start");
        TbrProductionContext productionContext = (TbrProductionContext) context;
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
            // 处理需求计划
            List<MonthPlanProductionRequirePlanVo> productionPlanList = groupInfo.getGroupPlanData();
            
            // 处理特殊材料
            Integer allcateMaxDay = groupInfo.getDayProductionLimitInfo().keySet().stream().max(Integer::compareTo).orElse(0);
            Integer allcateMinDay = groupInfo.getDayProductionLimitInfo().keySet().stream().min(Integer::compareTo).orElse(0);
            if (allcateMaxDay > allcateMinDay) {
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
            
            productionPlanList.forEach(plan -> {
                // 生产量替换成搭配量
                int productionQty = plan.getConventionReserveQty();
                //因搭配量只有一条计划，故而可以直接处理奇数，遇到奇数直接+1
                productionQty = (productionQty & 1) == 0 ? productionQty : productionQty + 1;
                plan.setProductionQty(productionQty);
                if (productionQty > 0) {
                    plan.setProductionFlag(YesOrNoEnum.YES.getCode()); // 设置成应生产
                }
                plan.setHeightProductionQty(0); // 高优先级
                if (plan.getDayVulcanizationQty() == null) { // 硫化日产能空的赋值为0，防止报错
                    plan.setDayVulcanizationQty(0);
                }
            }); // 待排产量要加上常规储备
            if (CollectionUtils.isEmpty(mouldDayProductionList)) {
                continue; // 成型或模具排程任意一个找不到数据都要跳过这个结构
            }

            // 取出最早成型硫化配比不足的日期
            // 本结构按天汇总的日排产量，计算产量限制以及模具限制，需要按key(日期)排序
            TreeMap<Integer, MatchingPlanLimitHelper> limitMap = this.caculateProductDay(productionContext, groupInfo,
                    ratioVo, mouldDayProductionList, allSinglePlanMap, continueInfo); // 计算排产日
            Integer startDay = limitMap.firstKey();
            Integer endDay = limitMap.lastKey();
            if (startDay.compareTo(endDay) > 0) { // 如果开始时间>结束时间，说明该结构满产，直接看下一个结构
                continue;
            }
            do {
                // 续作规格搭配排产
                this.matchingScheduleContinue(productionContext, newSkuQtyMap, groupInfo, limitMap);
                // 新增模具搭配排产
                Set<String> newMouldCodeSet = this.matchingScheduleNewMould(productionContext, newSkuQtyMap, groupInfo,
                        continueInfo, limitMap);
                if (CollectionUtils.isEmpty(newMouldCodeSet)) { // 如果有新增模具，则再跑一次续作；没有新增模具则结束。
                    break;
                }
                log.info("搭配新增模具" + newMouldCodeSet);
            } while(true);
        }
        log.info(context.getProductionVersion() + "搭配排产end");
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
     * @return
     */
    private Set<String> matchingScheduleNewMould(TbrProductionContext productionContext,
                                               Map<String, Integer> newSkuQtyMap, ProductionPlanGroupInfo groupInfo,
                                               CxContinueInfoHelper continueInfo,
                                               TreeMap<Integer, MatchingPlanLimitHelper> limitMap) {
        TreeMap<Integer, MatchingPlanLimitHelper> copyLimitMap = new TreeMap<>(limitMap); // 先复制一份产能限制列表，筛选SKU时会根据本次轮询对列表进行删减
        Set<String> newMouldCodeSet = new HashSet<>(); // 新增模具
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
                    scheduleMaterialDesc);
            if (StringUtils.isBlank(materialDesc)) {
                break;
            }
            scheduleMaterialDesc.add(materialDesc);
            // 判断如果是新增SKU，则需要检查成型机胎胚总数限制
            CxMachineBaseInfoVo cxMachineInfo = this.getNewSkuCxMachine(productionContext, groupInfo, copyLimitMap,
                    materialDesc);
            if (cxMachineInfo == null) {
                continue;
            }
            // 计算需要排产的量
            SkuNeedProductionInfo needProductionInfo = this.getNeedProductionQty(productionPlanList, materialDesc);
            if (null == needProductionInfo) {
                continue;
            }
            // 执行搭配排产算法
            Set<String> tempMouldCodeSet = this.matchingScheduleNewSchedule(productionContext, materialDesc, needProductionInfo, newSkuQtyMap, groupInfo,
                    continueInfo, copyLimitMap);
            if (!CollectionUtils.isEmpty(tempMouldCodeSet)) {
                newMouldCodeSet.addAll(tempMouldCodeSet);
                break; // 只要有新增模具，则直接结束走续作逻辑
            }
        } while (true);
        return newMouldCodeSet;
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
     * 获取新增SKU的可排产成型机台
     * @param productionContext
     * @param groupInfo
     * @param limitMap
     * @param materialDesc
     * @return
     */
    private CxMachineBaseInfoVo getNewSkuCxMachine(TbrProductionContext productionContext,
                                                   ProductionPlanGroupInfo groupInfo,
                                                   TreeMap<Integer, MatchingPlanLimitHelper> limitMap,
                                                   String materialDesc) {
        Map<String, CxMachineBaseInfoVo> cxMachineBaseInfo = productionContext.getBaseDataContainer()
                .getCxMachineBaseInfo();
        Set<String> cxMachineInfoSet = groupInfo.getAllocationCxMachineCodeSet();
        Integer startDay = limitMap.firstKey();
        Integer endDay = limitMap.lastKey();
        CxMachineBaseInfoVo cxMachineInfo = null;
        boolean isNewSku = false;
        for (String machineCode : cxMachineInfoSet) {
            cxMachineInfo = cxMachineBaseInfo.get(machineCode);
            if (cxMachineInfo == null) {
                continue;
            }
            if (cxMachineInfo.getAllocationList() == null) {
                continue;
            }
            isNewSku &= cxMachineInfo.getAllocationList().stream().anyMatch(allocation -> {
                if (allocation.getAllocationDay() < startDay || allocation.getAllocationDay() > endDay) {
                    return false;
                }
                if (allocation.getRealProductionPlanList().stream()
                        .noneMatch(p -> materialDesc.equals(p.getMaterialDesc()))) {
                    return false;
                }
                long embryoCodeCount = allocation.getRealProductionPlanList().stream().map(p -> p.getEmbryoCode())
                        .distinct().count();
                if (allocation.getMaxEmbryoCodeCount() <= embryoCodeCount) {
                    return false;
                }
                return true;
            });
            if (isNewSku) {
                break;
            }
        }
        return cxMachineInfo;
    }

    /**
     * 符合搭配条件的日期在机模具试制
     * 
     * @param productionContext
     * @param newSkuQtyMap
     * @param groupInfo
     * @param limitMap
     */
    private void matchingScheduleContinue(TbrProductionContext productionContext, Map<String, Integer> newSkuQtyMap,
                                          ProductionPlanGroupInfo groupInfo,
                                          TreeMap<Integer, MatchingPlanLimitHelper> limitMap) {
        Integer startDay = limitMap.firstKey();
        Integer endDay = limitMap.lastKey();
        List<MonthPlanProductionRequirePlanVo> groupPlanData = groupInfo.getGroupPlanData();
        Map<String, List<MonthPlanProductionRequirePlanVo>> productionPlanMap = groupPlanData.stream()
                .collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc));
        Set<String> scheduleMaterialDesc = new HashSet<>(); // 记录已排规格，防止重复执行死循环
        do {
            // 获取优先级最高的SKU信息
            String materialDesc = this.getSelectedAddSku(productionContext, startDay, endDay, groupPlanData,
                    scheduleMaterialDesc);
            if (StringUtils.isBlank(materialDesc)) {
                break;
            }
            scheduleMaterialDesc.add(materialDesc);
            // 计算需要排产的量
            SkuNeedProductionInfo needProductionInfo = this.getNeedProductionQty(groupPlanData, materialDesc);
            if (null == needProductionInfo) {
                continue;
            }
            List<MonthPlanProductionRequirePlanVo> productionPlanList = productionPlanMap.get(materialDesc);
            Integer maxProductionQty = needProductionInfo.getDayMaxProductionQty();
            Integer productionQty = needProductionInfo.getSumNeedProductionQty(); // 需求量
            List<MatchingMouldDayUsedHelper> mouldDayUsedList = this.caculateMouldDayUsed(productionContext,
                    materialDesc, maxProductionQty, startDay, endDay); // 统计每一天所有可用模具
            for (MatchingMouldDayUsedHelper mouldDayUsed : mouldDayUsedList) { // 遍历各模具可用列表
                if (productionQty <= 0) {
                    break;
                }
                Integer usedBeginDate = mouldDayUsed.getBeginDate();
                Integer usedEndDate = mouldDayUsed.getEndDate();
                MatchingPlanLimitHelper limitHelper = limitMap.get(usedBeginDate);
                if (limitHelper == null || !limitHelper.isProduct()) {
                    continue; // 当天已经无法添加排产，跳过
                }
                // 统计当天的已排产胎胚
                Set<String> embryoCodeSet = this.getEmbryoCodeSet(productionContext, groupInfo, usedBeginDate);
                String embryoCode = CollectionUtils.firstElement(productionPlanList).getEmbryoCode();
                if (!embryoCodeSet.contains(embryoCode)) {
                    GroupPlanCxLhCapacityLimitHelper limist = groupInfo.getDayProductionLimitInfo().get(usedBeginDate);
                    if (limist != null && limist.getMaxEmbryoCodeCount() != null && embryoCodeSet.size() >= limist.getMaxEmbryoCodeCount()) {
                        continue; // 已经达到最大胎胚数，跳过
                    }
                }
                List<ProductionMouldInfoVo> doubleMouldList = mouldDayUsed.getMouldInfoList();

                // 判断是否续作
                List<ProductionMouldInfoVo> continueMouldList = new ArrayList<>(); // 续作模具
                List<ProductionMouldInfoVo> twoMouldList = new ArrayList<>(); // 一次添加双模
                Integer lastDay = this.getLastDay(productionContext, usedBeginDate, startDay); // 上一个排产日
                for (ProductionMouldInfoVo mouldInfo : doubleMouldList) {
                    inner: 
                    // 判断上一天有排产，当天没排产
                    if (this.checkMouldHasProuct(mouldInfo, lastDay, materialDesc)) { // 上一天有排产该物料，说明是续作
                        boolean hasProduct = this.checkMouldHasProuct(mouldInfo, usedBeginDate, materialDesc);
                        // 判断如果今天的模具已经达到最大模具数，则不能加模具，只能补量，则当天没有排产本规格的模具跳过
                        if (limitHelper.getMaxMouldQty() <= limitHelper.getMouldQty() && !hasProduct) {
                            break inner;
                        }
                        // 模具今天明天没有排产，如果从n+2 至 n+二次上机天数之间有任意一天有排产，则不允许搭配，防止造成二次排产
                        if (this.checkIsSecOnline(productionContext, mouldInfo, usedBeginDate, endDay)) {
                            break inner;
                        }
                        twoMouldList.add(mouldInfo);
                    }
                    if (twoMouldList.size() == ProductionConstant.DOUBLE_MOULD_PRODUCTION) { // 凑够双模才添加
                        continueMouldList.addAll(twoMouldList);
                        twoMouldList.clear();
                    }
                }
                // 执行非首日续作排程算法
                if (!CollectionUtils.isEmpty(continueMouldList)) {
                    continueMouldList.stream().forEach(mould -> {
                        if (this.checkMouldHasProuct(mould, usedBeginDate, materialDesc)) { // 当天没有排产该规格，则当天的排产模具数+1
                            limitHelper.setMouldQty(limitHelper.getMouldQty() + 1);
                        }
                    });

                    // 获取实际日产量
                    Integer realMaxProductionQty = this.getRealDayMaxProductionQty(productionContext, usedBeginDate, maxProductionQty);
                    // 续做排程
                    Integer totalProductionQty = this.matchingScheduleNextDayContinue(productionContext, materialDesc,
                            newSkuQtyMap, groupInfo, productionPlanList, productionQty, realMaxProductionQty, usedBeginDate,
                            usedEndDate, continueMouldList);
                    Integer unProductQty = totalProductionQty;
                    for (MonthPlanProductionRequirePlanVo plan: productionPlanList) {
                        Integer conventionReserveQty = plan.getConventionReserveQty();
                        if (conventionReserveQty > unProductQty) {
                            conventionReserveQty -= unProductQty;
                            unProductQty = 0;
                        } else {
                            conventionReserveQty = 0;
                            unProductQty -= conventionReserveQty;
                        }
                        plan.setConventionReserveQty(conventionReserveQty);
                        if (unProductQty == 0) {
                            break;
                        }
                    }
                    productionQty -= totalProductionQty;
                    limitHelper.setPlanQty(limitHelper.getPlanQty() + totalProductionQty);
                }
            }
            
        } while (true);
    }

    /**
     * 判断当天如果上机是否会导致二次上机
     * 
     * @param productionContext 上下文
     * @param mouldInfo         模具
     * @param day               待排产日期
     * @param endDay            收尾日
     * @return
     */
    private boolean checkIsSecOnline(TbrProductionContext productionContext, ProductionMouldInfoVo mouldInfo,
                                     Integer day, Integer endDay) {
        // 获取二次上机校验天数
        int skuSecondProductionDays = productionContext.getBaseDataContainer().getParamConfiguration()
                .getSkuSecondProduction();
        if (skuSecondProductionDays <= 0) {
            return false;
        }
        Integer nextDay = this.getNextDay(productionContext, day, endDay); // 下一个排产日
        if (nextDay <= 0) {
            return false;
        }
        Integer afterDay = this.getNextDay(productionContext, nextDay, endDay); // n+2个排产日
        if (afterDay > 0) {
            return false;
        }
        boolean isTodayProduct = this.checkMouldHasProuct(mouldInfo, day);
        boolean isNextdayProduct = this.checkMouldHasProuct(mouldInfo, nextDay);
        if (isTodayProduct || isNextdayProduct) { // 今天活明天模具有排产，则今天排产不会导致二次上机
            return false;
        }
        // 模具在n+2天后有任意一天有排产，都会导致二次上机
        Integer skuSecondProductEndDay = 0;
        int countDays = 2; // 天数计数器，由于逻辑走到这里则今天明天都没有排产，因此从2开始
        for (int i = day + 1; i <= endDay; i++) {
            if (countDays >= skuSecondProductionDays) { // 计数器达到二次上机控制天数则结束
                break;
            }
            if (!productionContext.getStopDays().contains(i)) { // 下一天是排产日返回，否则跳过看下一天
                skuSecondProductEndDay = i;
                countDays++;
                break;
            }
        }
        if (skuSecondProductEndDay <= 0) {
            return false;
        }
        int realEndDay = skuSecondProductEndDay;
        return mouldInfo.getDayProductionInfo().keySet().stream()
                .anyMatch(d -> d >= afterDay && d <= realEndDay && this.checkMouldHasProuct(mouldInfo, d));
    }

    /**
     * 检查模具某天是否有排产
     * 
     * @param mouldInfo    待检查模具
     * @param day          待检查排产日
     * @return
     */
    private boolean checkMouldHasProuct(ProductionMouldInfoVo mouldInfo, Integer day) {
        return this.checkMouldHasProuct(mouldInfo, day, null);
    }

    /**
     * 检查模具某天是否有排产指定规格
     * 
     * @param mouldInfo    待检查模具
     * @param day          待检查排产日
     * @param materialDesc 指定规格，如果传空值，则有任意一个规格有排产都符合条件
     * @return
     */
    private boolean checkMouldHasProuct(ProductionMouldInfoVo mouldInfo, Integer day, String materialDesc) {
        List<CxMouldDayProductionHelper> dayProduction = mouldInfo.getDayProductionInfo().get(day);
        if (dayProduction == null) {
            return false;
        }
        return dayProduction.stream()
                .filter(s -> StringUtils.isEmpty(materialDesc) || materialDesc.equals(s.getMaterialDesc()))
                .mapToInt(CxMouldDayProductionHelper::getProductionQty).sum() > 0;
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
        Integer lhNum = BigDecimalUtils.multiply(cxNum, rate, true).intValue(); // 最大硫化机数
        Integer maxMouldNum = lhNum * lhMouldQty; // 换算成模具数 = 硫化机* 2（双模排产）
        Integer firstQty = Optional
                .ofNullable(productionContext.getBaseDataContainer().getParamConfiguration().getChangeMouldFirstQty())
                .orElse(0); // 新模首日排产量（双模）
        Integer firtOneMouldQty = firstQty / lhMouldQty; // 单模首日排产量

        // 统计每一天的已排产量
        TreeMap<Integer, MatchingPlanLimitHelper> dayPlanMap = new TreeMap<>();
        for (Entry<Integer, List<CxMouldDayProductionHelper>> modPlan : dayModPlanMap.entrySet()) {
            Integer day = modPlan.getKey();
            List<CxMouldDayProductionHelper> planList = modPlan.getValue();

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
                if (day == 1 && continueInfo != null) { // 首日
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
                int mouldPlanQty = Optional.ofNullable(dayPlan.getProductionQty()).orElse(0);
                if (mouldPlanQty <= 0) { // 当天没有排产量的跳过
                    continue;
                }
                firstDayMouldQty += isContinue? 0: 1; // 记录新增模具数
                spliceMouldQty += isContinue && dayPlan.getProductionQty() < dayVulcanizationQty - firtOneMouldQty? 1: 0; // 续作模具，且排产量低于产能 - 首日排产量的，记录可拼机台数量
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
        return this.buildProductDayLimitMap(productionContext, groupInfo, allSinglePlanMap, dayModPlanMap, maxMouldNum,
                firtOneMouldQty, dayPlanMap);
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
                                                                              Integer maxMouldNum,
                                                                              Integer firtOneMouldQty,
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
     */
    private Set<String> matchingScheduleNewSchedule(TbrProductionContext productionContext, String materialDesc,
                                                    SkuNeedProductionInfo needProductionInfo,
                                                    Map<String, Integer> newSkuQtyMap,
                                                    ProductionPlanGroupInfo groupInfo,
                                                    CxContinueInfoHelper continueInfo,
                                                    TreeMap<Integer, MatchingPlanLimitHelper> limitMap) {
        Integer lhMouldQty = ProductionConstant.DOUBLE_MOULD_PRODUCTION; // 硫化机模具配比
        // 如果结构有设置模具分配比例，需要限制不允许超过模具分配数
        int allocationMouldNum = lhMouldQty;
        String structureName = groupInfo.getGroupName();
        MouldAllocationInfoVo mouldAllocationControlInfo = productionContext
                .getMouldAllocationInfo(CollectionUtils.firstElement(needProductionInfo.getNeedProductionList()));
        if (mouldAllocationControlInfo != null) {
            // 获取本结构已分配模具列表
            List<ProductionMouldInfoVo> allocationMouldList = productionContext.getBaseDataContainer().getMouldInfoMap()
                    .values().stream()
                    .filter(m -> m.getDayProductionInfo().values().stream().anyMatch(
                            planList -> planList.stream().anyMatch(p -> structureName.equals(p.getStructureName()))))
                    .collect(Collectors.toList());
            if (allocationMouldList.size() >= mouldAllocationControlInfo.getAllocationQty()) {
                return new HashSet<>(0);
            }
            allocationMouldNum = mouldAllocationControlInfo.getAllocationQty() - allocationMouldList.size();
        }

        // 检查最早可以在那一天开始加模
        Set<String> newMouldCodeSet = new HashSet<>(); // 新增模具号
        int startDay = limitMap.firstKey();
        int endDay = limitMap.lastKey();
        Integer productionQty = needProductionInfo.getSumNeedProductionQty(); // 需求量
        Integer maxProductionQty = needProductionInfo.getDayMaxProductionQty(); // 单机台硫化上限
        List<MatchingMouldDayUsedHelper> mouldDayUsedList = this.caculateMouldDayUsed(productionContext, materialDesc,
                maxProductionQty, startDay, endDay); // 统计每一天所有可用模具
        for (MatchingMouldDayUsedHelper mouldDayUsed : mouldDayUsedList) { // 遍历各模具可用列表
            Integer usedBeginDate = mouldDayUsed.getBeginDate();
            Integer usedEndDate = mouldDayUsed.getEndDate();
            List<ProductionMouldInfoVo> doubleMouldList = mouldDayUsed.getMouldInfoList();
            if (CollectionUtils.isEmpty(doubleMouldList)) {
                continue;
            }
            // 统计当天的已排产胎胚
            Set<String> embryoCodeSet = this.getEmbryoCodeSet(productionContext, groupInfo, usedBeginDate);
            String embryoCode = CollectionUtils.firstElement(needProductionInfo.getNeedProductionList()).getEmbryoCode();
            if (!embryoCodeSet.contains(embryoCode)) {
                GroupPlanCxLhCapacityLimitHelper limist = groupInfo.getDayProductionLimitInfo().get(usedBeginDate);
                if (limist != null && limist.getMaxEmbryoCodeCount() != null && embryoCodeSet.size() >= limist.getMaxEmbryoCodeCount()) {
                    continue; // 已经达到最大胎胚数，跳过
                }
            }

            // 根据剩余可排模具限制模具数量
            MatchingPlanLimitHelper limitHelper = limitMap.get(usedBeginDate);
            Integer newMouldNum = limitHelper.getMaxMouldQty() - limitHelper.getMouldQty(); // 可新增模具数
            newMouldNum = BigDecimalUtils.least(newMouldNum, allocationMouldNum, lhMouldQty).intValue(); // 一次最多新增一台硫化机

            List<ProductionMouldInfoVo> newDoubleMouldList = new ArrayList<>(); // 新上模具
            if (newMouldNum > 0) { // 可新增模具
                List<ProductionMouldInfoVo> twoMouldList = new ArrayList<>(); // 一次添加双模
                for (ProductionMouldInfoVo mould: doubleMouldList) {
                    if (newMouldNum == 0) {
                        break;
                    }
                    List<CxMouldDayProductionHelper> dayProductionList = mould.getDayProductionInfo().get(usedBeginDate);
                    if (CollectionUtils.isEmpty(dayProductionList)) { // 当天没有排产才添加模具，一次加两幅
                        twoMouldList.add(mould);
                        newMouldNum --;
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
                Integer sumProductionQty = (productionQty & 1) == 0? productionQty: productionQty + 1; // 处理奇数，遇到奇数直接+1;

                Integer dayMaxProductionQty = this.getRealDayMaxProductionQty(productionContext, usedBeginDate, needProductionInfo.getDayMaxProductionQty()); // 获取实际日产量
                Integer realSumProductionQty = newSkuQtyMap.getOrDefault(materialDesc, 0); // 已排产量
                Set<String> cxMachineInfoSet = groupInfo.getAllocationCxMachineCodeSet();
                // 查找是否有相同模具的已关联成型硫化组
//                CxLhProductionHelper cxLhGroup = this.findCxLhGroup(doubleMouldList, cxMachineBaseInfo, cxMachineInfoSet);
                
                // 查找模具上一天的生产计划，并构建硫化分组
                CxLhProductionHelper cxLhGroup = CxLhProductionHelper.createEmptyLhGroup(groupInfo.getGroupName(),
                        1, cxMachineInfoSet);
                cxLhGroup.setDayMaxProductionQty(dayMaxProductionQty);
                cxLhGroup.setProductionMouldSet(newMouldCodeSet);
                Integer lastDay = this.getLastDay(productionContext, usedBeginDate, startDay);
                for (ProductionMouldInfoVo mould: newDoubleMouldList) {
                    List<CxMouldDayProductionHelper> latestPlanList = mould.getDayProductionInfo().get(lastDay); // 上一天计划
                    CxMouldDayProductionHelper production = CollectionUtils.firstElement(latestPlanList);
                    if (production != null) {
                        cxLhGroup.setMaterialCode(production.getMaterialCode());
                        cxLhGroup.setMaterialDesc(production.getMaterialDesc());
                        cxLhGroup.setProductionQty(0);
                    }
                }
                
                // 走新模排产逻辑
                LhProductionQtyHelper lhProductionQtyHelper = new LhProductionQtyHelper(groupInfo, cxMachineInfoSet,
                        cxLhGroup, sumProductionQty, realSumProductionQty, dayMaxProductionQty);
                CxLhMouldProductionCalculator.lhProductionByGroupHandler(productionContext, lhProductionQtyHelper,
                        usedBeginDate, usedEndDate, newDoubleMouldList, needProductionInfo.getNeedProductionList(),
                        false);
                Integer realProductionQty = lhProductionQtyHelper.getRealSumProductionQty() - realSumProductionQty;
                if (realProductionQty > 0) {
                    Integer unProductQty = realProductionQty;
                    for (MonthPlanProductionRequirePlanVo plan: needProductionInfo.getNeedProductionList()) {
                        Integer conventionReserveQty = plan.getConventionReserveQty();
                        if (conventionReserveQty > unProductQty) {
                            conventionReserveQty -= unProductQty;
                            unProductQty = 0;
                        } else {
                            conventionReserveQty = 0;
                            unProductQty -= conventionReserveQty;
                        }
                        plan.setConventionReserveQty(conventionReserveQty);
                        if (unProductQty == 0) {
                            break;
                        }
                    }
                    newSkuQtyMap.put(materialDesc, lhProductionQtyHelper.getRealSumProductionQty()); // 累计已排量
                    // 更新模具与排产量的累计量
                    limitHelper.setMouldQty(limitHelper.getMouldQty() + newDoubleMouldList.size());
                    limitHelper.setPlanQty(limitHelper.getPlanQty() + realProductionQty);
                    newMouldCodeSet.addAll(newDoubleMouldList.stream().map(ProductionMouldInfoVo::getMouldCode).distinct().collect(Collectors.toSet()));
                    break; // 新增模具后直接结束，后面走续作逻辑
                }
            }
        }
        return newMouldCodeSet;
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
     * 模具排序，按今天或上一天有排本规格的模具优先
     * 
     * @param materialDesc
     * @param usedBeginDate
     * @param lastDay
     * @param m1
     * @param m2
     * @return
     */
    private int usedMouldCompare(String materialDesc, Integer usedBeginDate, Integer lastDay,
                                 ProductionMouldInfoVo m1, ProductionMouldInfoVo m2) {
        List<CxMouldDayProductionHelper> dayProduction1 = m1.getDayProductionInfo().get(usedBeginDate);
        List<CxMouldDayProductionHelper> dayProduction2 = m2.getDayProductionInfo().get(usedBeginDate);
        Boolean sameMaterial1 = dayProduction1 != null
                && dayProduction1.stream().anyMatch(s -> Objects.equals(materialDesc, s.getMaterialDesc()));
        Boolean sameMaterial2 = dayProduction2 != null
                && dayProduction2.stream().anyMatch(s -> Objects.equals(materialDesc, s.getMaterialDesc()));
        int result = sameMaterial2.compareTo(sameMaterial1); // boolean是true比false大，因此需要倒序
        if (result != 0) {
            return result;
        }
        dayProduction1 = m1.getDayProductionInfo().get(lastDay);
        dayProduction2 = m2.getDayProductionInfo().get(lastDay);
        sameMaterial1 = dayProduction1 != null
                && dayProduction1.stream().anyMatch(s -> Objects.equals(materialDesc, s.getMaterialDesc()));
        sameMaterial2 = dayProduction2 != null
                && dayProduction2.stream().anyMatch(s -> Objects.equals(materialDesc, s.getMaterialDesc()));
        return sameMaterial2.compareTo(sameMaterial1); // boolean是true比false大，因此需要倒序
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
     * @param beginDate
     * @param endDate
     * @param continueMouldList
     */
    private Integer matchingScheduleNextDayContinue(TbrProductionContext productionContext, String materialDesc,
                                                 Map<String, Integer> newSkuQtyMap, ProductionPlanGroupInfo groupInfo,
                                                 List<MonthPlanProductionRequirePlanVo> productionPlanList,
                                                 Integer productionQty, Integer maxProductionQty, Integer beginDate,
                                                 Integer endDate, List<ProductionMouldInfoVo> continueMouldList) {
        // 续作规格排产
        if (CollectionUtils.isEmpty(continueMouldList)) {
            return 0;
        }
        Integer lhMouldQty = ProductionConstant.DOUBLE_MOULD_PRODUCTION;
//        Integer sumProductionQty = productionContext.getSpecialMaterialBatchRemainQty(groupInfo, productionQty, true); // 待排量 = 需求量如果是特殊材料需要以特殊材料为准
        Integer sumProductionQty = productionQty;
        // 构建续作规格对象
        CxContinueSkuInfoHelper continueSkuInfo = CxContinueSkuInfoHelper.buildContinueProductInfo(materialDesc,
                productionPlanList, new HashMap<>());
        continueSkuInfo.setOnLineCxMachineSet(groupInfo.getAllocationCxMachineCodeSet());
        continueSkuInfo.setDayVulcanizationQty(maxProductionQty);
        continueSkuInfo.setContinueSkuPlanList(productionPlanList);
        Integer totalProductionQty = 0;
        for (int productionDay = beginDate; productionDay <= endDate; productionDay++) { // 按顺序每天续作排产
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
                Integer realProductionQty = NumberUtils.min(sumProductionQty, capacityQty, maxProductionQty); // 取计划量、产能剩余量、最大排产量的最小值
                if (realProductionQty <= 0) {
                    continue;
                }
                realProductionQty = (realProductionQty & 1) == 0? realProductionQty: realProductionQty + 1; // 处理奇数，遇到奇数直接+1
                CxLhMouldProductionCalculator.continueSkuLhProductionHandler(productionContext, groupInfo,
                        continueSkuInfo, productionDay, realProductionQty, remainMouldList);
                realProductionQty *= lhQty; // 合计计划量时，要乘上硫化机数
                newSkuQtyMap.put(materialDesc, newSkuQtyMap.getOrDefault(materialDesc, 0) + realProductionQty); // 累计已排量
                sumProductionQty -= realProductionQty; // 待排量扣减已排量，要乘上硫化机数
                totalProductionQty += realProductionQty;
            }
        }
        return totalProductionQty;
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
            // 根据日产能比例重算日产能
            Integer realDayMoldQty = dayMoldQty;
            Integer realDayVulcanizationQty = this.getRealDayMaxProductionQty(productionContext, day, dayVulcanizationQty);
            if (realDayVulcanizationQty.compareTo(dayVulcanizationQty) != 0) { // 如果日产能有发生变化，重算日单模产量
                realDayMoldQty = realDayVulcanizationQty / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
            }
            List<ProductionMouldInfoVo> canUseMould = this.selectedAllMouldByDay(productionContext, materialDesc,
                    realDayMoldQty, day);
            if (!CollectionUtils.isEmpty(canUseMould)) {
                mouldDayUsedList.add(new MatchingMouldDayUsedHelper(canUseMould, day, day)); // 记录可用时间段
            }
        }
        return mouldDayUsedList;
    }

    /**
     * 构建排产结果并保存
     *
     * @param productionContext
     * @param resultList
     * @param detailList
     * @param newSkuQtyMap
     */
    @Transactional
    public void saveMouldProductionResult(TbrProductionContext productionContext,
                                           List<FactoryMonthPlanMouldDayResult> resultList,
                                           List<FactoryMonthPlanMouldDayDetail> detailList,
                                          Map<String, Integer> newSkuQtyMap) {
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
        List<FactoryMonthPlanMouldDayResult> mouldResultList = this.buildMouldResultList(dayResultList, resultList,
                newSkuQtyMap);
        List<FactoryMonthPlanMouldDayDetail> detailResultList = this.buildDetailResultList(detailLogList, detailList,
                productionContext, newSkuQtyMap);
//        List<MpMonthPlanStatistics> productionStatisticsList = this.buildProductionStatisticsList(productionContext,
//                mouldResultList, detailResultList);
        
        baseDao.saveBatch(detailResultList);
        baseDao.saveBatch(mouldResultList);
//        baseDao.saveBatch(productionStatisticsList);
    }

    /**
     * 构建排产统计信息
     * 
     * @param productionContext 上下文
     * @param mouldResultList   模具排产结果列表
     * @return
     */
    private List<MpMonthPlanStatistics> buildProductionStatisticsList(TbrProductionContext productionContext,
                                                                      List<FactoryMonthPlanMouldDayResult> mouldResultList,
                                                                      List<FactoryMonthPlanMouldDayDetail> detailResultList) {
        // 更新各结构的每日生产统计
        Map<String, ProductionPlanGroupInfo> allGroupPlanList = productionContext.getGroupProductionInfo();
        Map<String, List<FactoryMonthPlanMouldDayResult>> mouldResultStructureNameMap = mouldResultList.stream().collect(Collectors.groupingBy(FactoryMonthPlanMouldDayResult::getStructureName));
//        detailResultList.stream().collect(Collectors.groupingBy(classifier))
        for (ProductionPlanGroupInfo groupPlanInfo: allGroupPlanList.values()) {
            List<FactoryMonthPlanMouldDayResult> mouldDayResultList = mouldResultStructureNameMap.get(groupPlanInfo.getGroupName());
            for (FactoryMonthPlanMouldDayResult mouldDayResult: mouldDayResultList) {
                Integer beginDay = Optional.ofNullable(mouldDayResult.getBeginDay()).orElse(0);
                Integer endDay = Optional.ofNullable(mouldDayResult.getEndDay()).orElse(0);
                for (int day = beginDay; day <= endDay; day ++) {
                    Integer realDayProductionQty = Optional.ofNullable((Integer)mouldDayResult.getFieldValueByFieldName(FactoryConstant.DAY_FIELD + day)).orElse(0);
//                    SkuDayProductionInfoHelper skuDayProductionInfo = SkuDayProductionInfoHelper.buildEmpty(day, productionPlan, realDayProductionQty, 0, usedMouldSet);
//                    groupPlanInfo.addDayProductionInfo(skuDayProductionInfo);
                }
            }
        }
        // 根据上下文生成新统计信息
        List<MpMonthPlanStatistics> newProductionStatisticsList = dayProductionStatisticsHandler
                .buildDayProductionStatisticsResult(productionContext);
        // 本次搭配涉及的结构
        List<String> structureNameList = mouldResultList.stream().map(FactoryMonthPlanMouldDayResult::getStructureName)
                .distinct().collect(Collectors.toList());
        // 加载本次版本已生成的统计记录
        LambdaQueryWrapper<MpMonthPlanStatistics> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MpMonthPlanStatistics::getFactoryCode, productionContext.getFactoryCode());
        queryWrapper.eq(MpMonthPlanStatistics::getIsDelete, YesOrNoEnum.NO.getValue());
        queryWrapper.eq(MpMonthPlanStatistics::getProductionVersion, productionContext.getProductionVersion());
        List<MpMonthPlanStatistics> oldProductionStatisticsList = mpMonthPlanStatisticsEntityMapper
                .selectList(queryWrapper);
        Map<String, MpMonthPlanStatistics> oldProductionStatisticsMap = oldProductionStatisticsList.stream()
                .filter(s -> StringUtils.isNoneEmpty(s.getStructureName())).collect(
                        Collectors.toMap(MpMonthPlanStatistics::getStructureName, Function.identity(), (s1, s2) -> s1));
        // 根据结构取出本次需要保存的统计信息，原有结构的统计记录直接覆盖更新
        List<MpMonthPlanStatistics> productionStatisticsList = new ArrayList<>();
        for (MpMonthPlanStatistics newStatistics : newProductionStatisticsList) {
            String structureName = newStatistics.getStructureName();
            if (!structureNameList.contains(structureName)) {
                continue;
            }
            MpMonthPlanStatistics oldStatistics = oldProductionStatisticsMap.get(structureName);
            if (oldStatistics != null) {
                newStatistics.setId(oldStatistics.getId());
                newStatistics.setCreateBy(oldStatistics.getCreateBy());
                newStatistics.setCreateTime(oldStatistics.getCreateTime());
            }
            productionStatisticsList.add(newStatistics);
        }
        return productionStatisticsList;
    }

    /**
     * 构建待保存的排程明细记录
     * @param detailLogList
     * @param detailList
     * @param productionContext
     * @param newSkuQtyMap
     * @return
     */
    private List<FactoryMonthPlanMouldDayDetail> buildDetailResultList(List<FactoryMonthPlanMouldDayDetail> detailLogList,
                                                                       List<FactoryMonthPlanMouldDayDetail> detailList,
                                                                       TbrProductionContext productionContext,
                                                                       Map<String, Integer> newSkuQtyMap) {
        List<FactoryMonthPlanMouldDayDetail> detailResultList = new ArrayList<>();
        LambdaQueryWrapper<FactoryMonthPlanMouldDayDetail> queryWrapper = new LambdaQueryWrapper<FactoryMonthPlanMouldDayDetail>();
        queryWrapper.eq(FactoryMonthPlanMouldDayDetail::getProductionVersion, productionContext.getProductionVersion());
        Map<String, FactoryMonthPlanMouldDayDetail> oldDetailMap = detailList.stream().collect(Collectors
                .toMap(detail -> this.getMouldKey(detail), Function.identity(), (detail1, detail2) -> detail1));
        for (FactoryMonthPlanMouldDayDetail detail : detailLogList) {
            if (!newSkuQtyMap.containsKey(detail.getMaterialDesc())) {
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
     * @param dayResultList
     * @param resultList
     * @param newSkuQtyMap
     * @return
     */
    private List<FactoryMonthPlanMouldDayResult> buildMouldResultList(List<FactoryMonthPlanMouldDayResult> dayResultList,
                                                                      List<FactoryMonthPlanMouldDayResult> resultList,
                                                                      Map<String, Integer> newSkuQtyMap) {
        List<FactoryMonthPlanMouldDayResult> saveResultList = new ArrayList<>();
        Map<String, FactoryMonthPlanMouldDayResult> oldPlanMap = resultList.stream()
                .collect(Collectors.toMap(FactoryMonthPlanMouldDayResult::getMaterialCode, Function.identity()));
        Long productionSequence = resultList.stream().map(FactoryMonthPlanMouldDayResult::getProductionSequence)
                .filter(Objects::nonNull).max(Long::compareTo).orElse(0L);
        for (FactoryMonthPlanMouldDayResult plan : dayResultList) {
            if (!newSkuQtyMap.containsKey(plan.getMaterialDesc())) {
                continue;
            }
            FactoryMonthPlanMouldDayResult firstPlan = CollectionUtils.firstElement(resultList);
            // 原有记录有同规格的更新（有ID）；没有的说明是新搭配的规格，需要新增（无ID）
            FactoryMonthPlanMouldDayResult oldPlan = oldPlanMap.get(plan.getMaterialCode());
            if (oldPlan != null) {
                plan.setConventionProductionQty(newSkuQtyMap.get(plan.getMaterialDesc()));
                plan.setTotalQty(oldPlan.getTotalQty() + plan.getConventionProductionQty());
                plan.setHeightProductionQty(oldPlan.getHeightProductionQty());
                plan.setMidProductionQty(oldPlan.getMidProductionQty());
                plan.setCycleProductionQty(oldPlan.getCycleProductionQty());
                plan.setPostponeProductionQty(oldPlan.getPostponeProductionQty());
                plan.setDifferenceQty(oldPlan.getDifferenceQty());
                plan.setMouldCavityQty(oldPlan.getMouldCavityQty());
                plan.setTypeBlockQty(oldPlan.getTypeBlockQty());
                plan.setFactProdReqQty(oldPlan.getFactProdReqQty());
                plan.setReason(oldPlan.getReason());
                plan.setId(oldPlan.getId());
            } else {
                plan.setConventionProductionQty(newSkuQtyMap.get(plan.getMaterialDesc()));
                plan.setTotalQty(plan.getConventionProductionQty());
                plan.setHeightProductionQty(0);
                plan.setMidProductionQty(0);
                plan.setCycleProductionQty(0);
                if (plan.getProductionSequence() == null) {
                    productionSequence++;
                    plan.setProductionSequence(productionSequence);
                }
                plan.setPostponeProductionQty(0);
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
     * 加载需求计划列表(合并后)
     *
     * @param productionContext 月计划生产版本
     * @return
     */
    private List<MonthPlanProductionRequirePlanVo> selectRequirePlan(TbrProductionContext productionContext,
                                                                            List<FactoryMonthPlanMouldDayResult> planList,
                                                                            List<FactoryMonthPlanMouldDayDetail> detailLogList) {
//        Map<String, MonthPlanProductionRequirePlanVo> requirePlanMap = new HashMap<>();
        List<MonthPlanProductionRequirePlanVo> requirePlanList = new ArrayList<>();
        String productionVersion = productionContext.getProductionVersion();
        String monthPlanVersion = productionContext.getMonthPlanVersion();
        Map<String, List<MonthPlanProductConstructionInfoVo>> constructionInfoMap = getProductionConstructionInfo(productionContext);
        Map<String, List<FactoryMonthPlanMouldDayResult>> planMap = planList.stream()
                .collect(Collectors.groupingBy(FactoryMonthPlanMouldDayResult::getMaterialCode));
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
//            MonthPlanProductionRequirePlanVo requirePlan = requirePlanMap.get(materialCode);
            MonthPlanProductionRequirePlanVo requirePlan = null; 
            if (requirePlan == null) {// 不存在直接转换
                requirePlan = MonthPlanProductionRequirePlanVo.buildInitProductionPlan(null, productionVersion,
                        demandPlan);
                requirePlan.setHeightLossQty(demandPlan.getMidQty());
                requirePlan.setFactProdReqQty(demandPlan.getNetQty());
                requirePlan.setVulcanizationInfo(lhCapacityMap.get(demandPlan.getMaterialDesc())); // 设置硫化信息
                requirePlan.setInventorySalesRatio(0D);// 默认0
                requirePlan.setConstructionInfo(constructionInfoMap.get(materialCode)); //加载施工
                requirePlan.setMonthPlanId(demandPlan.getId());
                List<FactoryMonthPlanMouldDayDetail> detailLogs = detailLogMap.get(demandPlan.getId());
                List<FactoryMonthPlanMouldDayResult> plans = planMap.get(materialCode);
                int productionQty = 0;
                if (!CollectionUtils.isEmpty(detailLogs)) {
                    productionQty = detailLogs.stream().filter(d -> d.getTotalQty() != null)
                            .mapToInt(FactoryMonthPlanMouldDayDetail::getTotalQty).sum();
                } else if (!CollectionUtils.isEmpty(plans)) {
                    FactoryMonthPlanMouldDayResult tempPlan = CollectionUtils.firstElement(plans);
                    productionQty = tempPlan.getTotalQty();
                }
                requirePlan.setOriginProductionQty(productionQty);
                requirePlan.setProductionQty(productionQty);
                requirePlan.resetProductionDataInfo();
//                requirePlan.setHeightProductionQty(demandPlan.getHeightQty());
//                requirePlanMap.put(materialCode, requirePlan);
                requirePlanList.add(requirePlan);
                continue;
            }
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

        productionContext.getBaseDataContainer().setParamConfiguration(this.createParamConfiguration(productionContext)); // 排程参数
        this.setProductionCycleInfo(productionContext); // 设置生产周期
        this.setMonthProductionDays(productionContext); // 设置生产日
        this.buildDayCapacityLimitInfo(productionContext); // 初始化日产能限制

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
     * 将定稿计划和需求计划构建成算法要求的上下文结构
     *
     * @param planList       定稿计划
     * @param requirePlanList 需求计划
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
        container.setCxMachineBaseInfo(this.getDataService().getCxMachineBaseInfo(productionContext)); // 已排结构排程
        container.setMouldInfoMap(this.buildMouldInfoMap(productionContext, detailLogList, requirePlanMap)); // 已排模具计划
        container.setGroupMainPatternAllocationLimitMap(this.getGroupMainPatternAllocationInfo(productionContext)); // 结构模具分配配比
        this.specialMaterialInfoHandler(productionContext);
//        this.buildCxLhRatioMap(productionContext, container.getMouldInfoMap(), requirePlanMap); // 构建成型硫化组
        productionContext.setOverSixMonthStockMap(this.overSixMonthStockHandler(productionContext,
                getDataService().getMdmProductStock(productionContext))); // 超6个成品库存信息
        this.fillMouldRelationStructureName(productionContext, requirePlanList); // 补充模具关系中的物料结构名
        this.buildGroupMainPatternInfo(productionContext);

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
        DayCapacityLimitVo changeMouldLimitHandler = productionContext.getBaseDataContainer().getDayCapacityLimit(); // 每日产能限制
        
        // 先模具初始化
        List<MonthPlanProductMouldInfoVo> allProductMouldInfoList = new ArrayList<>();
        skuMouldRelationMap.values().forEach(list -> allProductMouldInfoList.addAll(list));
        Date boardingDate = productionContext.getProductionStartDate();
        for (MonthPlanProductMouldInfoVo mould: allProductMouldInfoList) {
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
            Set<String> mouldCodeSet = Collections.singleton(mouldCode);
            Integer beginDay = Optional.ofNullable(detail.getBeginDay()).orElse(1);
            Integer endDay = Optional.ofNullable(detail.getEndDay()).orElse(productionContext.getProductionEndDay());
            Set<String> cxMachineCodeInfo; // 成型机台
            if (StringUtils.isEmpty(detail.getCxMachineCode())) {
                cxMachineCodeInfo = new HashSet<>();
            } else {
                cxMachineCodeInfo = new HashSet<>(Arrays.asList(StringUtils.split(detail.getCxMachineCode(), StringConstant.COMMA))); // 成型机台
            }
            for (int day = beginDay; day <= endDay; day ++) {
                Integer productionQty = (Integer) detail.getFieldValueByFieldName(FactoryConstant.DAY_FIELD + day); // 根据日期取对应日计划量
                if (productionQty == null || productionQty <= 0) {
                    continue;
                }
                productionQty = productionQty * ProductionConstant.DOUBLE_MOULD_PRODUCTION; // 双模排产
                productionMouldInfo.addProductionInfo(day, requirePlan, false, productionQty,
                        cxMachineCodeInfo);
                // 根据模具使用情况初始化每日产能限制
                changeMouldLimitHandler.addChangeMouldUsedQty(productionContext, day, detail.getMaterialDesc(), mouldCodeSet);
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
     * @param scheduleMaterialDesc 已排
     * @return
     */
    private String getSelectedAddSku(TbrProductionContext productionContext, Integer startDay, Integer endDay,
                                     List<MonthPlanProductionRequirePlanVo> productionPlanList,
                                     Set<String> scheduleMaterialDesc) {
        if (CollectionUtils.isEmpty(productionPlanList)) {
            return "";
        }
        // 提取所有sku的物料描述
//        Set<String> allMaterialDescSet = productionPlanList.stream()
//                .map(MonthPlanProductionRequirePlanVo::getMaterialDesc).collect(Collectors.toSet());
//        Set<String> enableMaterialDescSet = productionContext
//                .getHasMouldCapacity(ProductionConstant.DOUBLE_MOULD_PRODUCTION, allMaterialDescSet, startDay, endDay);
//        if (CollectionUtils.isEmpty(enableMaterialDescSet)) {
//            return "";
//        }
//        List<MonthPlanProductionRequirePlanVo> enablePlanList = productionPlanList.stream()
//                .filter(plan -> enableMaterialDescSet.contains(plan.getMaterialDesc())).collect(Collectors.toList());
//        if (CollectionUtils.isEmpty(enablePlanList)) {
//            return "";
//        }
        List<MonthPlanProductionRequirePlanVo> enablePlanList = productionPlanList;
        // 只看有常规储备的sku
        List<MonthPlanProductionRequirePlanVo> hasReserveQtyPlanList = enablePlanList.stream()
                .filter(s -> !scheduleMaterialDesc.contains(s.getMaterialDesc())) // 已经排过的跳过
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
        paramCodeList.add(MonthPlanEnums.SKU_SECOND_PRODUCTION.getCode());
        paramCodeList.add(MonthPlanEnums.BOOST_PRODUCTION_TYPE_VALUE.getCode());
        paramCodeList.add(MonthPlanEnums.MAX_BOOST_DAY.getCode());
        paramCodeList.add(MonthPlanEnums.MATCHING_BOOST_DAY.getCode());
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
        // 周程滚动相关
        paramCodeList.add(MonthPlanEnums.SINGLE_CX_MACHINE_LOCK_DAYS.getCode());
        paramCodeList.add(MonthPlanEnums.MULTI_CX_MACHINE_LOCK_DAYS.getCode());
        paramCodeList.add(MonthPlanEnums.WEEK_ROLL_ADJUST_DATE.getCode());
        // 获取数据
        Map<String, Object> paramConfigurationMap = getDataService().getFactoryParamByCondition(productionContext, paramCodeList);
        return this.buildParam(paramConfigurationMap);
    }

    /**
     * 业务的参数封装未配置对象
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
     * 检查二次上机
     *
     * @param productionPlanInfo 排产计划信息
     * @param productionContext  排产上下文
     * @param materialDesc     规格描述
     * @param startDay       上机日
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
     * 获取排产日结构的已排胎胚列表
     *
     * @param productionContext 上下文
     * @param groupInfo         结构
     * @param day               排产日
     * @return
     */
    private Set<String> getEmbryoCodeSet(TbrProductionContext productionContext, ProductionPlanGroupInfo groupInfo,
                                         Integer day) {
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        List<MonthPlanProductionRequirePlanVo> groupPlanData = groupInfo.getGroupPlanData();
        List<String> materialDescList = groupPlanData.stream().map(MonthPlanProductionRequirePlanVo::getMaterialDesc)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<String, ProductionMouldInfoVo> mouldInfoMap = baseDataContainer.getMouldInfoMap();
        Set<String> embryoCodeSet = new HashSet<>();
        for (String materialDesc : materialDescList) {
            List<MonthPlanProductMouldInfoVo> skuRelationList = baseDataContainer.getSkuMouldRelationMap()
                    .get(materialDesc);
            if (CollectionUtils.isEmpty(skuRelationList)) {
                continue;
            }
            skuRelationList.stream().forEach(skuRelation -> {
                ProductionMouldInfoVo mouldInfo = mouldInfoMap.get(skuRelation.getMouldCode());
                if (null == mouldInfo) {
                    return;
                }
                Map<Integer, List<CxMouldDayProductionHelper>> dayProductionInfo = mouldInfo.getDayProductionInfo();
                if (CollectionUtils.isEmpty(dayProductionInfo)) {
                    return;
                }
                List<CxMouldDayProductionHelper> productionList = dayProductionInfo.get(day);
                if (CollectionUtils.isEmpty(productionList)) {
                    return;
                }
                Set<String> tempEmbryoCodeSet = productionList.stream()
                        .filter(p -> Optional.ofNullable(p.getProductionQty()).orElse(0) > 0)
                        .map(CxMouldDayProductionHelper::getEmbryoCode).filter(Objects::nonNull).distinct()
                        .collect(Collectors.toSet());
                embryoCodeSet.addAll(tempEmbryoCodeSet);
            });
        }
        return embryoCodeSet;
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
                // 如果上一天排满了其他sku，当天不可用
                List<CxMouldDayProductionHelper> latestProductionList = dayProductionInfo.get(this.getLastDay(productionContext, day, 1));
                if (!CollectionUtils.isEmpty(latestProductionList)) {
                    Integer productionQty = latestProductionList.stream()
                            .mapToInt(CxMouldDayProductionHelper::getProductionQty).sum(); // 合计当天的已排量
                    boolean isMaxProduct = productionQty >= dayMoldQty;
                    boolean hasOtherSku = latestProductionList.stream().anyMatch(p -> StringUtils.isNotEmpty(p.getMaterialDesc())
                            && !p.getMaterialDesc().equals(materialDesc));
                    if (isMaxProduct && hasOtherSku) {
                        return;
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
        List<ContinueGroupInfo> continueGroupInfoList = monthProductionDataService.getContinueGroupInfo(factoryCode, year, month,
                lastDay);
//        log.info(TbrBeforeProductionGroupLogRecorder.addReadContinueGroupDataLog(context, continueGroupInfoList));
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
//            log.warn(TbrBeforeProductionGroupLogRecorder.addContinueGroupNoOnLineMachineLog(context, groupName, continueSku.getMaterialDesc(), onLineMachineSet));
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
            log.info(TbrProductionInitLogRecorder.addConstructionInfoEmptyLog(productionContext));
            return Collections.emptyMap();
        }
        return constructionInfoList.stream().collect(Collectors.groupingBy(MonthPlanProductConstructionInfoVo::getMaterialCode));
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
