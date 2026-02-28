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
import com.zlt.aps.mp.engine.logrecorder.TbrBeforeProductionGroupLogRecorder;
import com.zlt.aps.mp.engine.mapper.FactoryMonthPlanMouldDayDetailMapper;
import com.zlt.aps.mp.engine.mapper.MpStructureAllocationMapper;
import com.zlt.aps.mp.engine.scheduling.cxcapacity.ProductionCapacityParamConfiguration;
import com.zlt.aps.mp.engine.service.DpRequireDataService;
import com.zlt.aps.mp.engine.service.MonthProductionDataService;
import com.zlt.aps.mp.engine.utils.MouldRelationDeduplicator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.zlt.aps.mp.engine.check.SkuSecondChecker;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.daylimit.DayCapacityLimitHelper;
import com.zlt.aps.mp.engine.daylimit.DayCapacityLimitVo;
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
    private BaseDao baseDao;
    @Autowired
    private DpRequireDataService dpRequireDataService;
    @Autowired
    private MonthProductionDataService monthProductionDataService;
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
     * 周程滚动的结构内搭配算法
     * 
     * @param contextDTO              周程滚动调整上下文
     * @param mpAdjustStructureInList 结构内调整记录列表
     * @param mpProdFinalList         月计划定稿表列表（只有当前结构的记录）
     * @param isInner                 是否结构内调整
     */
    public void matchingAdjustProduction(MpRollAdjustContextDTO contextDTO,
                                         List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList) {
        try {
            String config = sysConfigService.selectConfigByKey("monthAdjust.skip.matching");
            if (StringUtils.isNotBlank(config) && Boolean.parseBoolean(config)) {
                return; // 跳过搭配开关打开，则直接返回
            }
        } catch (Exception e) {
            log.error("获取配置失败", e);
        }
        
        // 结构排产的开始不能早于锁定日的校验
        Integer startDay = contextDTO.getStartDay();
        Integer endDay = contextDTO.getEndDay();
        Integer lockEndDay = contextDTO.getLockEndDay();
        if (endDay <= lockEndDay) { // 结束日在锁定日结束前的结构不搭配
            return;
        }
        // 特殊材料可搭配量校验
        boolean isSpecial = Optional.ofNullable(contextDTO.getSpecStructureTotalQty()).orElse(0) > 0;
        Integer unAllocatSpecStructureTotalQty = 0;
        if (isSpecial) {
            Integer totalQty = mpProdFinalList.stream().filter(p -> p.getTotalQty() != null).mapToInt(FactoryMonthPlanFinalAdjustVo::getTotalQty).sum();
            unAllocatSpecStructureTotalQty = contextDTO.getSpecStructureTotalQty() - totalQty;
            if (unAllocatSpecStructureTotalQty <= 0) {
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
        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitMap = contextDTO.getDailyCapacityLimitVoMap(); // 每日产能统计
        Map<String, MdmSkuLhCapacity> mdmSkuLhCapacityMap = this.getSkuLhCapacity(contextDTO);; // 日硫化产能表，key:物料描述
        Map<String, MdmSkuConstructionRef> mdmSkuConstructionRefMap = this.getMdmSkuConstructionRefMap(contextDTO); // 获取sku与施工关系，key：物料号
        productionContext.setOverSixMonthStockMap(this.overSixMonthStockHandler(productionContext, stockList)); // 超6个成品库存信息
        productionContext.getBaseDataContainer().setParamConfiguration(this.buildParam(contextDTO.getParamMap()));
        Integer matchingBoostDay = productionContext.getBaseDataContainer().getParamConfiguration().getMatchingBoostDay(); // SKU收尾日离结构收尾日可搭配补量的天数
        Set<String> boostProductionTypeSet = productionContext.getBaseDataContainer().getParamConfiguration().getBoostProductionType(); // 可补量的排产分类集合
        // 部分list转换成map，方便取数
        Map<String, Integer> stockMap = stockList.stream()
                .filter(s -> StringUtils.isNotEmpty(s.getMaterialDesc())).collect(Collectors
                        .toMap(MdmProductStock::getMaterialDesc, MdmProductStock::getStockQty, (q1, q2) -> q1 + q2)); // key：规格描述，value：库存
        Map<String, FactoryMonthPlanFinalAdjustVo> mpProdFinalMap = mpProdFinalList.stream()
                .filter(p -> StringUtils.isNotEmpty(p.getMaterialDesc())).collect(Collectors
                        .toMap(FactoryMonthPlanFinalAdjustVo::getMaterialDesc, Function.identity(), (p1, p2) -> p1)); // key：规格描述
        
        // 按天统计已排产量
        Map<Integer, List<MatchingProductionAdjuestVo>> dayProductionMap = this.buildDayProductionMap(mpProdFinalList,
                startDay, endDay);
        Set<String> scheduleMaterialDesc = new HashSet<>(); // 记录已排规格，防止重复执行死循环
        do {
            // 获取最高优先级的可搭配调整规格
            String materialDesc = this.getHeightPriorityAdjuestMaterial(demandPlanList, mpProdFinalMap, stockMap,
                    productionContext, scheduleMaterialDesc, lockEndDay);
            if (StringUtils.isEmpty(materialDesc)) {
                break;
            }
            scheduleMaterialDesc.add(materialDesc); // 选中的规格加入已排产列表（无论是否能排上，下次轮询均不再处理该规格）
            List<DpDemandPlan> needProductPlanList = demandPlanList.stream()
                    .filter(p -> materialDesc.equals(p.getMaterialDesc())).collect(Collectors.toList());
            FactoryMonthPlanFinalAdjustVo plan = this.getFinalPlanByMaterialDesc(contextDTO, materialDesc,
                    mpProdFinalList, needProductPlanList, mdmSkuConstructionRefMap); // 获取定稿计划
            
            int unAllocationQty = needProductPlanList.stream().mapToInt(DpDemandPlan::getConventionReserveQty).sum(); // 储备量
            unAllocationQty = isSpecial? Math.min(unAllocationQty, unAllocatSpecStructureTotalQty): unAllocationQty; // 如果包含特殊材料，不能超过特殊材料的总数量
            int capacity = this.getMdmSkuLhCapacity(contextDTO, plan.getMaterialCode(), mdmSkuLhCapacityMap);
            
            out: do {
                int startUnAllocationQty = unAllocationQty;
                boolean isBegin = false; // 是否已经开始i排产的标记
                for (int day = Math.max(lockEndDay + 1, startDay); day <= endDay; day++) { // 遍历结构排产日，如果锁定日超过开始i日期，从锁定日下一天开始
                    // 是主销产品，切剩余天数在可搭配补量的天数范围内，需要补量
                    if (!isSpecial && boostProductionTypeSet.contains(plan.getProductionType())) { // 特殊结构不考虑
                        if (endDay - day <= matchingBoostDay) {
                            Integer planQty = Optional.ofNullable((Integer)plan.getFieldValueByFieldName(FactoryConstant.DAY_FIELD + day)).orElse(0); // 今天的已排量
                            if (planQty > 0) {
                                unAllocationQty = Math.max(unAllocationQty, capacity);
                            }
                        }
                    }
                    if (unAllocationQty <= 0) {
                        if (!isSpecial && boostProductionTypeSet.contains(plan.getProductionType())) { // 如果是主销产品则继续往后
                            continue;
                        }
                        break out;
                    }
                    if (!this.checkDayCanProduct(contextDTO, day)) { // 检查生产日历，停产日不处理
                        continue;
                    }
                    // 检查模具是否有剩余产能
                    MpDailyCapacityLimitVo dailyCapacityLimitVo = dailyCapacityLimitMap.get(day);
                    // 根据日产比例限制产能
                    BigDecimal dayProductionRate = BigDecimalUtils.percentages2Decimals(dailyCapacityLimitVo.getDayProductionRate()); // 日产比例
                    int realCapacity = BigDecimalUtils.multiply(capacity, dayProductionRate).setScale(0, RoundingMode.DOWN).intValue();
                    Integer mouldRemaindCapacity = this.getMouldRemaindCapacity(contextDTO, dayProductionMap, materialDesc, realCapacity, day); // 获取模具剩余产能
                    if (mouldRemaindCapacity <=0 && dailyCapacityLimitVo.getMaxLhMachines() <= dailyCapacityLimitVo.getUsedLhMachines()) { // 如果模具产能已满，且当天硫化机已经满足条件，则直接跳过
                        if (isBegin) { // 防止中断不连续的问题出现
                            break;
                        }
                        continue;
                    }
                    // 检查是否足够
                    if (dailyCapacityLimitVo.getMaxEmbryoTypes() <= dailyCapacityLimitVo.getUsedEmbryoTypes()) { // 胎胚数已达上限，则不能继续添加新胎胚
                        Set<String> embryoCodes = dailyCapacityLimitVo.getEmbryoCodes();
                        if (!embryoCodes.contains(plan.getEmbryoCode())) {
                            if (isBegin) { // 防止中断不连续的问题出现
                                break;
                            }
                            continue;
                        }
                    }
                    // 为当天分配搭配量
                    int allocationQty = this.allcatAdjustProductQty(contextDTO, day, plan,
                            dayProductionMap, unAllocationQty, realCapacity, dailyCapacityLimitVo, mouldRemaindCapacity);
                    if (allocationQty > 0) { // 有分配量，说明成功搭配排产，需要更新相关数据
                        isBegin = true;
                        unAllocatSpecStructureTotalQty -= allocationQty; // 特殊材料可分配量需要扣减掉已排产量
                        unAllocationQty -= allocationQty;
                    } else if (isBegin) { // 防止中断不连续的问题出现
                        break;
                    }
                }
                if (startUnAllocationQty == unAllocationQty) { // 如果遍历后没有发生变化，说明已经无法继续排产，则结束本规格的搭配
                    break out;
                }
            } while (true);
            if (plan.getBeginDay() == null) { // 没能排上的规格需要删除掉
                mpProdFinalList.remove(plan);
            }
        } while (true);
        log.info("周程滚动搭配算法end");
    }

    /**
     * 获取sku与施工关系
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
     * 
     * @param contextDTO
     * @param dayProductionMap
     * @param materialDesc
     * @param capacity
     * @param day
     * @return
     */
    private Integer getMouldRemaindCapacity(MpRollAdjustContextDTO contextDTO,
                                            Map<Integer, List<MatchingProductionAdjuestVo>> dayProductionMap,
                                            String materialDesc, int capacity, int day) {
        Integer remaindCapacity = 0;
        List<MatchingProductionAdjuestVo> lastDayProductionList = dayProductionMap.get(day);
        if (CollectionUtils.isEmpty(lastDayProductionList)) {
            return remaindCapacity;
        }
        MatchingProductionAdjuestVo dayProduction = lastDayProductionList.stream()
                .filter(p -> materialDesc.equals(p.getMaterialDesc())).findFirst().orElse(null);
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
        if (useCapacity > 0) { // 余数大于0，说明最后一个硫化机没有排满，优先补满逞能剩余的量
            remaindCapacity = realCapacity - useCapacity;
        }
        return remaindCapacity;
    }

    /**
     * 根据产能模具获取指定sku的硫化产能
     * 
     * @param contextDTO          上下文
     * @param materialDesc        规格描述
     * @param mdmSkuLhCapacityMap 硫化产能列表
     * @return
     */
    private int getMdmSkuLhCapacity(MpRollAdjustContextDTO contextDTO, String materialDesc,
                                    Map<String, MdmSkuLhCapacity> mdmSkuLhCapacityMap) {
        int capacity = 0;
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
     * 获取指定物料描述的定稿计划
     * @param contextDTO
     * @param materialDesc
     * @param mpProdFinalList
     * @param needProductPlanList
     * @return
     */
    private FactoryMonthPlanFinalAdjustVo getFinalPlanByMaterialDesc(MpRollAdjustContextDTO contextDTO,
                                                                     String materialDesc,
                                                                     List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList,
                                                                     List<DpDemandPlan> needProductPlanList,
                                                                     Map<String, MdmSkuConstructionRef> mdmSkuConstructionRefMap) {
        FactoryMonthPlanFinalAdjustVo plan = mpProdFinalList.stream()
                .filter(p -> materialDesc.equals(p.getMaterialDesc())).findFirst().orElse(null); // 获取排产结果
        if (plan == null) { // 如果没有，说明是新增规格，需要新增记录
            FactoryMonthPlanFinalAdjustVo firstPlan = CollectionUtils.firstElement(mpProdFinalList);
            DpDemandPlan firstDemandPlan = CollectionUtils.firstElement(needProductPlanList);
            plan = new FactoryMonthPlanFinalAdjustVo();
            SpringBeanUtils.copyPropertiesIgnoreNull(firstPlan, plan);
            plan.setId(null);
            plan.setMaterialCode(firstDemandPlan.getMaterialCode());
            plan.setMaterialDesc(firstDemandPlan.getMaterialDesc());
            plan.setMainPattern(firstDemandPlan.getMainPattern());
            MdmSkuConstructionRef skuConstructionRef = mdmSkuConstructionRefMap.get(firstDemandPlan.getMaterialCode());
            if (skuConstructionRef != null) {
                plan.setMainMaterialDesc(skuConstructionRef.getMainMaterialDesc());
            }
            plan.setMesMaterialCode(firstDemandPlan.getMesMaterialCode());
            plan.setProductionType(firstDemandPlan.getProductionType());
            plan.setTotalQty(0);
            plan.setBeginDay(null);
            plan.setEndDay(null);
            for (int day = 1; day <= MAX_MONTH_DAY; day++) {
                plan.setFieldValueByFieldName(FactoryConstant.DAY_FIELD + day, null); // 清空每天排产量
            }
            mpProdFinalList.add(plan);
        }
        return plan;
    }

    /**
     * 获取最高优先级的可搭配调整规格
     * 
     * @param demandPlanList       需求计划列表
     * @param mpProdFinalMap       定稿列表，key：规格描述
     * @param stockMap             库存列表
     * @param productionContext    上下文
     * @param scheduleMaterialDesc 已排产物料描述
     * @param lockEndDay           锁定结束日期
     * @return
     */
    private String getHeightPriorityAdjuestMaterial(List<DpDemandPlan> demandPlanList, 
                                                    Map<String, FactoryMonthPlanFinalAdjustVo> mpProdFinalMap,
                                                    Map<String, Integer> stockMap,
                                                    TbrProductionContext productionContext,
                                                    Set<String> scheduleMaterialDesc,
                                                    Integer lockEndDay) {
        String materialDesc = demandPlanList.stream()
                .filter(p -> !scheduleMaterialDesc.contains(p.getMaterialDesc()))
                .min((p1, p2) -> {
                    if (Objects.equals(p1.getMaterialDesc(), p2.getMaterialDesc())) { // 同规格的，不需要比较
                        return 0;
                    }
                    // 已排产的优先搭配
                    FactoryMonthPlanFinalAdjustVo finalPlan1 = mpProdFinalMap.get(p1.getMaterialDesc());
                    FactoryMonthPlanFinalAdjustVo finalPlan2 = mpProdFinalMap.get(p2.getMaterialDesc());
                    Integer totalQty1 = Optional.ofNullable(finalPlan1).map(FactoryMonthPlanFinalAdjustVo::getTotalQty).orElse(0);
                    Integer totalQty2 = Optional.ofNullable(finalPlan2).map(FactoryMonthPlanFinalAdjustVo::getTotalQty).orElse(0);
                    Boolean hasPlanQty1 = totalQty1 > 0;
                    Boolean hasPlanQty2 = totalQty2 > 0;
                    int result = hasPlanQty2.compareTo(hasPlanQty1); // 有计划优先于无计划
                    if (result != 0) {
                        return result;
                    }
                    
                    // 排序1、优先库销比低的
                    BigDecimal inventorySalesRatio1 = BigDecimal.ZERO;
                    BigDecimal inventorySalesRatio2 = BigDecimal.ZERO;
                    int averageSaleQty1 = Optional.ofNullable(p1.getAverageSaleQty()).orElse(0);
                    int averageSaleQty2 = Optional.ofNullable(p2.getAverageSaleQty()).orElse(0);
                    int stock1 = stockMap.getOrDefault(p1.getMaterialDesc(), 0);
                    int stock2 = stockMap.getOrDefault(p2.getMaterialDesc(), 0);
                    int planQty1 = this.getSumPlanQtyLockEndDay(finalPlan1, lockEndDay);
                    int planQty2 = this.getSumPlanQtyLockEndDay(finalPlan2, lockEndDay);
                    
                    if (averageSaleQty1 != 0) {
                        inventorySalesRatio1 = BigDecimalUtils.div(stock1 + planQty1, averageSaleQty1);
                    }
                    if (averageSaleQty2 != 0) {
                        inventorySalesRatio2 = BigDecimalUtils.div(stock2 + planQty2, averageSaleQty2);
                    }
                    result = inventorySalesRatio1.compareTo(inventorySalesRatio2);
                    if (result != 0) {
                        return result;
                    }
                    // 排序2、优先超6个月库存少的
                    Integer sixStock1 = productionContext.getOverSixMonthStockMap()
                            .getOrDefault(p1.getMaterialCode(), 0);
                    Integer sixStock2 = productionContext.getOverSixMonthStockMap()
                            .getOrDefault(p2.getMaterialCode(), 0);
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
     * 分配调整生产
     * @param contextDTO
     * @param scheduleDay
     * @param plan
     * @param dayProductionMap
     * @param unAllocationQty
     * @param capacity
     * @param dailyCapacityLimitVo
     * @param mouldRemaindCapacity
     * @return
     */
    private Integer allcatAdjustProductQty(MpRollAdjustContextDTO contextDTO, Integer scheduleDay,
                                           FactoryMonthPlanFinalAdjustVo plan,
                                           Map<Integer, List<MatchingProductionAdjuestVo>> dayProductionMap,
                                           Integer unAllocationQty, Integer capacity,
                                           MpDailyCapacityLimitVo dailyCapacityLimitVo,
                                           Integer mouldRemaindCapacity) {
        Integer lhMouldQty = ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        DailyMouldAvailabilityResult cavity2Block = contextDTO.getCavity2BlockMap().get(scheduleDay); // key:结构名称 + 主花纹
        if (cavity2Block == null) {
            return 0;
        }
        String materialDesc = plan.getMaterialDesc();
        String mouldKey = contextDTO.getStructureName() + plan.getMainPattern();
        int mouldCavityQty = cavity2Block.getCavityResults().getOrDefault(mouldKey, 0); // 可用型腔数量
        int typeBlockQty = cavity2Block.getInsertResults().getOrDefault(mouldKey, 0); // 可用活块数量
        Map<String, Object> param = contextDTO.getParamMap();
        // 判断当天成型硫化比是否已经满足条件
        List<MatchingProductionAdjuestVo> dayProductionList = dayProductionMap.get(scheduleDay);
        // 统计当天的已排量，判断不能超过最大排产量限制
        Integer todayProductQty = dayProductionList.stream().mapToInt(MatchingProductionAdjuestVo::getProductionQty).sum();
        Integer maxDayProductionQty = dailyCapacityLimitVo.getMaxDayProductionQty();
        Integer canProductionQty = maxDayProductionQty - todayProductQty;
        if (canProductionQty <= 0) {
            return 0;
        }
        
        // 根据上一天的排产情况判断是否需要换模
        List<MatchingProductionAdjuestVo> lastDayProductionList = dayProductionMap.get(scheduleDay - 1);
        // 判断是否需要换模，昨天没有排相同的结构，则需要换模具
        boolean isChangeMould;
        if (lastDayProductionList == null) {
            isChangeMould = true;
        } else {
            isChangeMould = lastDayProductionList.stream().noneMatch(p -> materialDesc.equals(p.getMaterialDesc()));
        }
        // 判断是否需要换活字块
        boolean isChangeBlock = false;
//        if (lastDayProductionList != null) {
//            // 昨天没有排相同的规格，则需要换或字块
//            isChangeBlock = lastDayProductionList.stream().noneMatch(p -> materialDesc.equals(p.getMaterialDesc()));
//        }
        boolean isRemaindCapacity = mouldRemaindCapacity > 0; // 是否是补模具产能

        if (isChangeMould && mouldCavityQty < lhMouldQty) { // 需要换模，且剩余型腔数不足最低排产模具数，结束
            return 0;
        }
        if (isChangeBlock && typeBlockQty < lhMouldQty) { // 需要换活块，且剩余活块数不足最低排产模具数，结束
            return 0;
        }
        
        // 计算排产量
        int allocationQty = capacity; // 本次排产量，默认是双模*模具产能
        if (isChangeMould) { // 如果是换模具，则只能增加首日排产量
            Integer changeMouldFirstQty = (Integer) param.get(MonthPlanEnums.CHANGE_MOULD_FIRST_QTY.getCode()); // 换模首日可排产量
            allocationQty = changeMouldFirstQty; // 每次仅新增一台硫化机
        }
        if (isChangeBlock) { // 如果只是换或字块，则按换字块的逻辑处理
            Integer changeTypeBlockQty = (Integer) param.get(MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY.getCode()); // 换活可排产量
            allocationQty = changeTypeBlockQty; // 每次仅更换一台
        }
        if (allocationQty <= 0) {
            return 0;
        }
        if (isRemaindCapacity) {
            allocationQty = Math.min(allocationQty, mouldRemaindCapacity); // 如果当天模具有剩余产能的，优先补满
        }
        allocationQty = Math.min(allocationQty, unAllocationQty); // 分配量不能超过未分配量
        allocationQty = (allocationQty & 1) == 0? allocationQty: allocationQty + 1; // 处理奇数，遇到奇数直接+1;
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
        cavity2Block.getCavityResults().put(mouldKey, mouldCavityQty - lhMouldQty); // 更新可用型腔数量
        // 统计新增的排产计划
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
        contextDTO.getLogDetail().append(String.format("结构:%s,【搭配排产】物料编码:%s,排产日:%s,搭配排产量:%s",contextDTO.getStructureName(),plan.getMaterialCode(),scheduleDay,allocationQty)).append(ApsConstant.DIVISION); // 记录日志
        return allocationQty;
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
                int productionQty = plan.getConventionReserveQty() // 生产量替换成搭配量
                        .intValue();
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
     * @param productionContext
     * @param newSkuQtyMap
     * @param groupInfo
     * @param continueInfo
     * @param limitMap
     * @return
     */
    private Set<String> matchingScheduleNewMould(TbrProductionContext productionContext,
                                               Map<String, Integer> newSkuQtyMap, ProductionPlanGroupInfo groupInfo,
                                               CxContinueInfoHelper continueInfo,
                                               TreeMap<Integer, MatchingPlanLimitHelper> limitMap) {
        List<MonthPlanProductionRequirePlanVo> productionPlanList = groupInfo.getGroupPlanData();
        Integer startDay = limitMap.firstKey();
        Integer endDay = limitMap.lastKey();
        Set<String> newMouldCodeSet = new HashSet<>(); // 新增模具
        // 循环取结构向下所有符合搭配生产条件的sku进行搭配排产
        Set<String> scheduleMaterialDesc = new HashSet<>(); // 记录已排规格，防止重复执行死循环
        do {
            // 获取优先级最高的Sku信息
            String materialDesc = this.getSelectedAddSku(productionContext, startDay, endDay, productionPlanList,
                    scheduleMaterialDesc);
            if (StringUtils.isBlank(materialDesc)) {
                break;
            }
            scheduleMaterialDesc.add(materialDesc);
            // 判断如果是新增sku，则需要检查成型机胎胚总数限制
            CxMachineBaseInfoVo cxMachineInfo = this.getNewSkuCxMachine(productionContext, groupInfo, limitMap,
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
                    continueInfo, limitMap);
            if (!CollectionUtils.isEmpty(tempMouldCodeSet)) {
                newMouldCodeSet.addAll(tempMouldCodeSet);
                break; // 只要有新增模具，则直接结束走续作逻辑
            }
        } while (true);
        return newMouldCodeSet;
    }

    /**
     * 获取新增sku的可排产成型机台
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
        out:
        do {
            // 获取优先级最高的Sku信息
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
            for (int day = startDay; day <= endDay; day++) {
                startDay = day; // 开始时间等于当前校验时间，如果以下校验不通过，则开始日期会推后一天
                // 检查如果符合二次上机，则从该天开始，否则推后一天继续校验
                if (!this.checkSecOnline(groupInfo, productionContext, materialDesc, day)) {
                    if (startDay == endDay) {
                        continue out; // 最后一天都检验不通过，直接结束本sku排产
                    }
                    continue; // 校验不通过，看下一天
                }
//                // 判断剩余可换模次数
//                DayCapacityLimitVo dayCapacityLimit = productionContext.getBaseDataContainer().getDayCapacityLimit();
//                Set<Integer> hasChangeMouldDaySet = dayCapacityLimit.getHasChangeMouldProductionDay(productionContext);
//                if (CollectionUtils.isEmpty(hasChangeMouldDaySet)) { //达到换模次数限制，不通过
//                    if (startDay == endDay) {
//                        continue out; // 最后一天都检验不通过，直接结束本sku排产
//                    }
//                    continue; // 校验不通过，看下一天
//                }
                break; // 校验均通过，直接结束
            }
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
                List<ProductionMouldInfoVo> doubleMouldList = mouldDayUsed.getMouldInfoList();
                List<ProductionMouldInfoVo> limitDoubleMouldList = doubleMouldList.stream()
                        .collect(Collectors.toList());

                // 判断是否续作
                List<ProductionMouldInfoVo> continueMouldList = new ArrayList<>(); // 续作模具
                List<ProductionMouldInfoVo> twoMouldList = new ArrayList<>(); // 一次添加双模
                for (ProductionMouldInfoVo mouldInfo : limitDoubleMouldList) {
                    // 判断切换计划前上一天的排产计划
                    List<CxMouldDayProductionHelper> lastDayProductionList = mouldInfo.getDayProductionInfo()
                            .get(usedBeginDate - 1);
                    if (!CollectionUtils.isEmpty(lastDayProductionList)
                            && lastDayProductionList.stream().anyMatch(p -> materialDesc.equals(p.getMaterialDesc()))) { // 上一天有排产，且物料描述一致，说明是续作
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
                        List<CxMouldDayProductionHelper> dayProduction = mould.getDayProductionInfo()
                                .get(usedBeginDate);
                        if (dayProduction == null
                                || dayProduction.stream().filter(s -> materialDesc.equals(s.getMaterialDesc()))
                                        .mapToInt(CxMouldDayProductionHelper::getProductionQty).sum() <= 0) { // 当天没有排产
                            limitHelper.setMouldQty(limitHelper.getMouldQty() + 1);
                        }
                    });

                    Integer totalProductionQty = this.matchingScheduleNextDayContinue(productionContext, materialDesc,
                            newSkuQtyMap, groupInfo, productionPlanList, productionQty, maxProductionQty, usedBeginDate,
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
            Integer dayVulcanizationQty = 0;
            for (CxMouldDayProductionHelper dayPlan : planList) {
                // 取出单模具产能
                dayVulcanizationQty = allSinglePlanMap.get(dayPlan.getMonthPlanId()).getDayVulcanizationQty();
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
                    List<CxMouldDayProductionHelper> mouldDayList = dayModPlanMap.get(day - 1);
                    isContinue = !CollectionUtils.isEmpty(mouldDayList) && mouldDayList.stream()
                            .anyMatch(p -> Objects.equals(dayPlan.getMaterialDesc(), p.getMaterialDesc()));
                }
                BigDecimal unit = BigDecimalUtils.valueOf(isContinue ? dayVulcanizationQty : firtOneMouldQty); // 单模每日最大排产量：续作，直接按最大满产排；非续作只能按新模首日排产
                Integer newMouldQty = BigDecimalUtils.div(dayPlan.getProductionQty(), unit, 0).intValue(); // 模具数
                maxPlanQty += newMouldQty * unit.intValue();
                mouldQty += newMouldQty;
            }
            if (mouldQty < maxMouldNum) { // 如果模具数没达到上限，则把剩余模具按最大产能补到最大排产量上
                maxPlanQty = maxPlanQty + (maxMouldNum - mouldQty) * dayVulcanizationQty;
            }
            MatchingPlanLimitHelper dayLimit = dayPlanMap.get(day);
            if (dayLimit == null) {
                dayLimit = new MatchingPlanLimitHelper();
                dayPlanMap.put(day, dayLimit);
                dayLimit.setMaxPlanQty(maxPlanQty);
                dayLimit.setMaxMouldQty(maxMouldNum);
            }
            Integer productionQty = planList.stream().mapToInt(CxMouldDayProductionHelper::getProductionQty).sum();
            Integer planQty = Optional.ofNullable(dayLimit.getPlanQty()).orElse(0) + productionQty;
            dayLimit.setPlanQty(planQty);
            dayLimit.setMouldQty(mouldQty);
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
                List<CxMouldDayProductionHelper> mouldDayList = dayModPlanMap.get(day - 1);
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
     * 执行新增sku搭配排产算法
     *
     * @param productionContext  上下文
     * @param materialDesc       结构
     * @param needProductionInfo 需排产物料
     * @param newSkuQtyMap       已搭配排产规格数量
     * @param groupInfo          排产计划分组
     * @param continueInfo       首日续作规格
     * @param limitMap           排产限制
     * @param cxMachineInfo      新增sku安排的机台
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

                Integer dayMaxProductionQty = needProductionInfo.getDayMaxProductionQty();
                Integer realSumProductionQty = newSkuQtyMap.getOrDefault(materialDesc, 0); // 已排产量
                Set<String> cxMachineInfoSet = groupInfo.getAllocationCxMachineCodeSet();
                // 查找是否有相同模具的已关联成型硫化组
//                CxLhProductionHelper cxLhGroup = this.findCxLhGroup(doubleMouldList, cxMachineBaseInfo, cxMachineInfoSet);
                
                // 查找模具上一天的生产计划，并构建硫化分组
                CxLhProductionHelper cxLhGroup = CxLhProductionHelper.createEmptyLhGroup(groupInfo.getGroupName(),
                        1, cxMachineInfoSet);
                cxLhGroup.setDayMaxProductionQty(dayMaxProductionQty);
                cxLhGroup.setProductionMouldSet(newMouldCodeSet);
                for (ProductionMouldInfoVo mould: newDoubleMouldList) {
                    List<CxMouldDayProductionHelper> latestPlanList = mould.getDayProductionInfo().get(usedBeginDate - 1); // 上一天计划
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
     * 模具排序，按今天或上一天有排本规格的模具优先
     * 
     * @param materialDesc
     * @param usedBeginDate
     * @param m1
     * @param m2
     * @return
     */
    private int usedMouldCompare(String materialDesc, Integer usedBeginDate, ProductionMouldInfoVo m1,
                                 ProductionMouldInfoVo m2) {
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
        dayProduction1 = m1.getDayProductionInfo().get(usedBeginDate - 1);
        dayProduction2 = m2.getDayProductionInfo().get(usedBeginDate - 1);
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
     * @param lhMouldQty
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
//        Map<Integer, List<ProductionMouldInfoVo>> canUseMouldMap = new TreeMap<>();
        Integer dayMoldQty = dayVulcanizationQty / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
//        for (int day = startDay; day <= endDay; day++) {
//            // 选择模具
//            canUseMouldMap.put(day, this.selectedAllMouldByDay(productionContext, materialDesc, dayMoldQty, day));
//        }
        // 遍历每一天的可用模具，与前一天可用模具相同的日期分作一组，然后按组遍历排产
        for (int day = startDay; day <= endDay; day++) {
          List<ProductionMouldInfoVo> canUseMould = this.selectedAllMouldByDay(productionContext, materialDesc, dayMoldQty, day);
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
     * @param planList
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
        List<FactoryMonthPlanMouldDayResult> mouldResultList = this.buildMouldResultList(dayResultList, resultList, newSkuQtyMap);
        List<FactoryMonthPlanMouldDayDetail> detailResultList = this.buildDetailResultList(detailLogList, detailList,
                productionContext, newSkuQtyMap);
        baseDao.saveBatch(detailResultList);
        baseDao.saveBatch(mouldResultList);
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
                plan.setBaseVale(plan.getId());
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
                plan.setBaseVale(null);
            }

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
        log.info(TbrBeforeProductionGroupLogRecorder.addReaderCxLhGroupRatioLog(context, structureLhRatioList));
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
//        this.buildDayCapacityLimitInfo(productionContext); // 初始化日产能限制

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
     * @param result 定稿计划
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
     * @param demandPlanList 需求计划
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
        container.setMouldInfoMap(this.buildMouldInfoMap(productionContext, detailLogList, requirePlanMap)); // 已排模具计划（非调整）
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
     * @param planList
     * @param requirePlanList
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
     * @param realStartDay       上机日
     * @return true-允许二次上机，false-不允许二次上机
     */
    private boolean checkSecOnline(ProductionPlanGroupInfo productionPlanInfo, TbrProductionContext productionContext,
                                          String materialDesc, Integer realStartDay) {
        List<Integer> dayList = productionPlanInfo.getProductionDaySetBySku(materialDesc);
        if (CollectionUtils.isEmpty(dayList)) {
            return true;
        }
        if (dayList.contains(realStartDay)) {
            return true;
        }
        // 取最大的天数
        Integer lastCloseDay = dayList.stream().max(Integer::compareTo).get();
        int skuSecondProductionDays = productionContext.getBaseDataContainer().getParamConfiguration().getSkuSecondProduction();
        SkuSecondChecker skuSecondChecker = new SkuSecondChecker(realStartDay, lastCloseDay, skuSecondProductionDays);
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
        log.info(TbrBeforeProductionGroupLogRecorder.addReaderProductionCalendarLog(context, productionDayInfoList));
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
        log.info(TbrBeforeProductionGroupLogRecorder.addReaderStopCalendarLog(context, stopDays));
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
                List<CxMouldDayProductionHelper> latestProductionList = dayProductionInfo.get(day - 1);
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
        effectiveList.sort((m1, m2) -> this.usedMouldCompare(materialDesc, day, m1, m2));
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
        log.info(TbrBeforeProductionGroupLogRecorder.addReadContinueGroupDataLog(context, continueGroupInfoList));
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
            log.warn(TbrBeforeProductionGroupLogRecorder.addContinueGroupNoOnLineMachineLog(context, groupName, continueSku.getMaterialDesc(), onLineMachineSet));
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
        log.info(TbrBeforeProductionGroupLogRecorder.addReaderMouldAllocationLog(context, mouldAllocationInfoList));
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
        log.info(TbrBeforeProductionGroupLogRecorder.addReaderSpecialMaterialLog(productionContext, specialMaterialInfoList));
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
        log.info(TbrBeforeProductionGroupLogRecorder.addReaderSpecialMaterialStockLog(productionContext, specialMaterialStockList));
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
