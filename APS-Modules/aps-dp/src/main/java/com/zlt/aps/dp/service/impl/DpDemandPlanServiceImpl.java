package com.zlt.aps.dp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.ProductionGroupTypeEnum;
import com.tlt.aps.enums.ProductionPlanType;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.exception.BusinessException;
import com.tlt.aps.utils.BeanCopyUtils;
import com.zlt.aps.common.core.constant.ApsConstant;

import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.DpOrderOffsetDetail;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.monthplan.api.domain.entity.FactoryParam;

import com.zlt.aps.monthplan.api.domain.entity.MdmAreaCapaAllocation;
import com.zlt.aps.monthplan.api.domain.entity.MdmCycleSchStruConf;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductStock;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MpMonthPlanMonitor;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import com.zlt.aps.monthplan.api.domain.entity.SupplyOrderPool;
import com.zlt.aps.monthplan.common.utils.BatchInsertProcessor;
import com.zlt.aps.monthplan.common.utils.CycleStockUpService;
import com.zlt.aps.monthplan.common.utils.DemandPlanGrouper;
import com.zlt.aps.monthplan.common.utils.PrecedentStockUpService;
import com.zlt.aps.monthplan.common.utils.PredictionContext;
import com.zlt.aps.monthplan.common.utils.RequirementVersionService;
import com.zlt.aps.monthplan.common.utils.SummaryDemandPlanService;
import com.zlt.aps.monthplan.common.utils.poi.AlternateMaterialSelector;
import com.zlt.aps.monthplan.demand.mapper.DpDemandPlanEntityMapper;
import com.zlt.aps.monthplan.demand.service.IDpDemandPlanService;
import com.zlt.aps.monthplan.demand.service.IDpOrderPoolSnapshotService;
import com.zlt.aps.monthplan.demand.service.ISalesOrderPoolService;
import com.zlt.aps.monthplan.demand.service.ISupplyOrderPoolService;
import com.zlt.aps.monthplan.factory.helper.PredictionAllocationHelper;
import com.zlt.aps.monthplan.factory.helper.SaleRequirePlanHelper;
import com.zlt.aps.monthplan.factory.helper.StockAllocationHelper;

import com.zlt.aps.monthplan.factory.mapper.MpFactoryProductionVersionMapper;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProductionFinalResultService;
import com.zlt.aps.monthplan.factory.service.IMpStructureAllocationService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;


import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.util.CollectionUtils;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：DpDemandPlanServiceImpl.java
 * 描    述：DpDemandPlanServiceImpl需求计划业务层处理
 *@author yelq
 *@date 2025-12-25
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class DpDemandPlanServiceImpl extends AbstractDocService<DpDemandPlan>  implements IDpDemandPlanService {
    // 净需求计划
    public static final String PREFIX = "REQ";
    // 调整需求计划
    private static final String PREFIX_ADJUST = "ADJ";

    private final static String ZERO_YEAR_WEEK = "0000";
    private final DpDemandPlanEntityMapper demandPlanEntityMapper;
    private final MpFactoryProductionVersionMapper factoryProductionVersionMapper;
    private final RequirementVersionService requirementVersionService;
    private final ISalesOrderPoolService salesOrderPoolService;
    // 成品库存
    private final IMdmProductStockService mdmProductStockService;
    // 定稿的月度排产计划
    private final IFactoryMonthPlanProductionFinalResultService factoryMonthPlanProductionFinalResultService;
    // 区域产能分配
    private final IMdmAreaCapaAllocationService mdmAreaCapaAllocationService;
    // SKU排产分类
    private final IMdmSkuScheduleCategoryService mdmSkuScheduleCategoryService;
    // 供应链订单
    private final ISupplyOrderPoolService supplyOrderPoolService;
    // 订单快照
    private final IDpOrderPoolSnapshotService dpOrderPoolSnapshotService;
    // 排产设定
    private final IFactoryParamService factoryParamService;
    // 物料信息
    private final IMdmMaterialInfoService materialInfoService;
    // 月均销量
    private final IMpMonthlySaleQtyService mpMonthlySaleQtyService;
    // 月度硫化监控
    private final IMpMonthPlanMonitorService monthPlanMonitorService;
    // 周期结构配置
    private final IMdmCycleSchStruConfService mdmCycleSchStruConfService;
    // 排产设定
    private final IFactoryParamService iFactoryParamService;
    // 汇总净需求
    private final SummaryDemandPlanService summaryDemandPlanService;
    // 周期排产储备
    private final CycleStockUpService cycleStockUpService;
    // 常规储备
    private final PrecedentStockUpService precedentStockUpService;
    // 批量插入处理器
    private final BatchInsertProcessor<DpDemandPlan> batchInsertProcessor;

    private final IMpStructureAllocationService mpStructureAllocationService;

    @Override
    protected String getDocTypeCode() {
        return "0802";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("0802");
        return sysDocType;
    }

    @Override
    public String checkUnique(DpDemandPlan docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.dpDemandPlan.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public void createMonthRequire(DpDemandPlan createCondition) {
        YearMonth tMonth;
        if(null != createCondition.getYear() && null != createCondition.getMonth()){
            tMonth = YearMonth.of(createCondition.getYear(), createCondition.getMonth());
        }else{
            // 获取操作日所在月份
            YearMonth currentMonth = YearMonth.from(LocalDate.now());
            // T月 = 当月 + 1个月
            tMonth = currentMonth.plusMonths(1);
        }
        createCondition.setFactoryCode(StringUtils.isBlank(createCondition.getFactoryCode())?FactoryConstant.DEFAULT_FACTORY_CODE:createCondition.getFactoryCode());
        createCondition.setYear(tMonth.getYear());
        createCondition.setMonth(tMonth.getMonthValue());
        createCondition.setPlanType(ProductionPlanType.NORMAL.getPlanType());
        // 1. 前置校验
        validateProductionVersionFinalized(createCondition);
        // 2. 生成版本号不能重复
        String monthPlanVersion;
        if (StringUtils.isNotBlank(createCondition.getMonthPlanVersion())) {
            // 19409 净需求计划----->点击生成需求计划需要弹框获取需求计划版本号，然后允许用户修改需求计划版本号
            monthPlanVersion = createCondition.getMonthPlanVersion();
        }else{
            monthPlanVersion = requirementVersionService.generateVersion(PREFIX);
        }
        createCondition.setMonthPlanVersion(monthPlanVersion);
        // 3. 并行获取数据
        PredictionContext data = fetchRequiredDataInParallel(createCondition);
        // 4. 处理销售订单分配
        PredictionContext.OrderAllocationResult allocationResult = processSalesOrderAllocation(tMonth,
            monthPlanVersion, data.getAllocationOrders(), data.getFinishedProductStockMap(),
            data.getMonthSurplusMap(),data.getMaterialInfoMap());

        AlternateMaterialSelector.setAlternateMaterialFlag(allocationResult.getNetDemands(),data.getFinishedProductStockMap());
        // 6. 处理需求计划生成
        List<DpDemandPlan> rawPlans = generateDemandPlans(
            createCondition, allocationResult.getNetDemands(), data);
        // 7: 计划合并和持久化
        List<DpDemandPlan> finalPlans = mergeAndPersistPlans(createCondition,data, rawPlans);
        // 8: 后续处理
        postProcess(createCondition, data, finalPlans,allocationResult);
    }

    private void postProcess(DpDemandPlan createCondition, PredictionContext data, List<DpDemandPlan> finalPlans,PredictionContext.OrderAllocationResult allocationResult) {
        // 8. 保存订单池快照
        if(!ProductionPlanType.ADJUST.getPlanType().equals(createCondition.getPlanType())){
            saveOrderPoolSnapshot(createCondition, data.getSalesOrders(), data.getSupplyOrderPools());
        }
        if(CollectionUtils.isEmpty(finalPlans)){
            return;
        }
        // 9. 保存分厂排产版本
        saveFactoryProductionVersion(finalPlans);
        // 10、汇总需求计划
        summaryDemandPlanService.summaryDemandPlan(createCondition,data, allocationResult,finalPlans);
    }



    private List<DpDemandPlan> mergeAndPersistPlans(DpDemandPlan createCondition, PredictionContext data, List<DpDemandPlan> rawPlans) {
        if(CollectionUtils.isEmpty(rawPlans)){
            return Collections.emptyList();
        }
        return saveDemandPlans(createCondition, rawPlans, data);
    }


    /**
     *  获取EUDR年周号
     * @return
     */
    private String getWeekYearForEudr() {
        FactoryParam factoryParam = new FactoryParam();
        factoryParam.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        factoryParam.setParamCode(MonthPlanEnums.EUDR_REQUIRE.getCode());
        factoryParam.setProductTypeCode(ProductTypeEnum.WHOLE_STEEL.getValue());
        FactoryParam param = iFactoryParamService.getFacParamSingle(factoryParam);
        String paramValue;
        if (param == null) {
            return StringUtils.EMPTY;
        }
        paramValue = StringUtils.isNotEmpty(param.getParamValue()) ? param.getParamValue() : param.getDefauleValue();
        return paramValue;
    }


    private void saveFactoryProductionVersion(List<DpDemandPlan> mergedDemandPlans) {
        if(CollectionUtils.isEmpty(mergedDemandPlans)) {
            return;
        }
        MpFactoryProductionVersion version = new MpFactoryProductionVersion();
        version.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        version.setYear(mergedDemandPlans.get(0).getYear());
        version.setMonth(mergedDemandPlans.get(0).getMonth());
        version.setMonthPlanVersion(mergedDemandPlans.get(0).getMonthPlanVersion());
        version.setPlanType(mergedDemandPlans.get(0).getPlanType());
        version.setIsFinal(YesOrNoEnum.NO.getCode());
        version.setProductTypeCode(ProductTypeEnum.WHOLE_STEEL.getValue());
        factoryProductionVersionMapper.insert(version);
    }

    @Override
    public List<DpDemandPlan> findDemandPlanByMonthPlanVersion(MpFactoryProductionVersion finalVersion) {
        LambdaQueryWrapper<DpDemandPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DpDemandPlan::getFactoryCode, finalVersion.getFactoryCode());
        wrapper.eq(DpDemandPlan::getYear, finalVersion.getYear());
        wrapper.eq(DpDemandPlan::getMonth, finalVersion.getMonth());
        wrapper.eq(DpDemandPlan::getMonthPlanVersion, finalVersion.getMonthPlanVersion());
        wrapper.eq(DpDemandPlan::getIsDelete, YesOrNoEnum.NO.getValue());
        return this.demandPlanEntityMapper.selectList(wrapper);
    }

    @Override
    public List<DpDemandPlan> createAdjustRequire(DpDemandPlan createCondition) {
        // 1. 前置校验
        validateFinalizedForAdjust(createCondition);
        // 3. 并行获取数据
        PredictionContext data = fetchRequiredDataInParallelByAdjust(createCondition);
        data.setPostponeOrders(null);
        // 2. 生成版本号不能重复
        String monthPlanVersion = requirementVersionService.generateVersion(PREFIX_ADJUST);
        createCondition.setFactoryCode(StringUtils.isBlank(createCondition.getFactoryCode())?FactoryConstant.DEFAULT_FACTORY_CODE:createCondition.getFactoryCode());
        createCondition.setMonthPlanVersion(monthPlanVersion);
        createCondition.setPlanType(ProductionPlanType.ADJUST.getPlanType());
        YearMonth tMonth = YearMonth.of(createCondition.getYear(), createCondition.getMonth());
        // 4. 处理销售订单分配
        PredictionContext.OrderAllocationResult allocationResult = processSalesOrderAllocation(tMonth,
            monthPlanVersion, data.getSalesOrders(), data.getFinishedProductStockMap(),
            data.getMonthSurplusMap(),data.getMaterialInfoMap());
        AlternateMaterialSelector.setAlternateMaterialFlag(allocationResult.getNetDemands(),data.getFinishedProductStockMap());
        // 6. 处理需求计划生成
        List<DpDemandPlan> rawPlans = generateDemandPlans(createCondition, allocationResult.getNetDemands(), data);
        // 7: 计划合并和持久化
        List<DpDemandPlan> finalPlans = mergeAndPersistPlans(createCondition,data, rawPlans);
        allocationResult.resetAll();
        // 8: 后续处理
        postProcess(createCondition, data, finalPlans,allocationResult);
        return finalPlans;
    }

    private PredictionContext fetchRequiredDataInParallelByAdjust(DpDemandPlan createCondition) {
        Set<String>  structureNames = mpStructureAllocationService.findStructureNames(createCondition);
        List<MdmMaterialInfo> materialInfos = this.materialInfoService.findMaterialInfoByStructureNames(createCondition.getFactoryCode(), structureNames);
        Set<String> materialCodes = this.getMaterialCodes(materialInfos);
        CompletableFuture<List<SalesOrderPool>> salesOrdersFuture =
            CompletableFuture.supplyAsync(() ->  this.salesOrderPoolService.findCurrentSalesOrderPool(createCondition.getFactoryCode(), materialCodes));
        CompletableFuture<List<MdmProductStock>> stocksFuture =
            CompletableFuture.supplyAsync(() ->  this.fetchFinishedProductStocks(createCondition.getFactoryCode()));
        CompletableFuture<Map<String, String>> productionTypeFuture =
            CompletableFuture.supplyAsync(() -> this.fetchProductionTypeMap(createCondition.getFactoryCode()));

        CompletableFuture<List<SupplyOrderPool>> supplyOrdersFuture =
            CompletableFuture.supplyAsync(() -> this.supplyOrderPoolService.findAdjustSupplyOrderPool(createCondition, materialCodes));

        CompletableFuture<Map<String, Integer>> monthlySaleQtyFuture =
            CompletableFuture.supplyAsync(() -> this.mpMonthlySaleQtyService.findAdjustMonthlySaleQty(createCondition, materialCodes));

        CompletableFuture<Integer> minProductionQtyFuture =
            CompletableFuture.supplyAsync(this::getMinProductionQty);
        CompletableFuture<Map<String, MdmMaterialInfo>> fetchMaterialInfoFuture =
            CompletableFuture.supplyAsync(() -> this.fetchMaterialInfo(createCondition));
        CompletableFuture<List<MdmCycleSchStruConf>> cycleSchStruConfFuture =
            CompletableFuture.supplyAsync(() ->  mdmCycleSchStruConfService.findAdjustCycleSchStruConf(createCondition,structureNames));
        // 等待所有任务完成
        CompletableFuture.allOf(
            salesOrdersFuture, stocksFuture, productionTypeFuture,
            supplyOrdersFuture, monthlySaleQtyFuture,minProductionQtyFuture,fetchMaterialInfoFuture,
            cycleSchStruConfFuture
        ).join();
        try {
            List<SalesOrderPool> salesOrders = salesOrdersFuture.get();
            List<MdmProductStock> finishedProductStocks = stocksFuture.get();
            Map<String, String> productionTypeMap = productionTypeFuture.get();
            List<SupplyOrderPool> supplyOrderPools = supplyOrdersFuture.get();
            Map<String, Integer>  monthlySaleQty = monthlySaleQtyFuture.get();
            int minProductionQty = minProductionQtyFuture.get();
            Map<String, MdmMaterialInfo> materialInfoMap = fetchMaterialInfoFuture.get();
            List<MdmCycleSchStruConf> cycleSchStruConf = cycleSchStruConfFuture.get();
            if(!CollectionUtils.isEmpty(finishedProductStocks)){
                finishedProductStocks.forEach(finishedProductStock -> finishedProductStock.setLeftOverQty(null == finishedProductStock.getStockQty()?BigDecimal.ZERO.intValue():finishedProductStock.getStockQty()));
            }
            // 处理成品库存映射
            Map<String, List<MdmProductStock>> finishedProductStockMap =
                CollectionUtils.isEmpty(finishedProductStocks) ?
                    new HashMap<>(16) :
                    finishedProductStocks.stream()
                        .collect(Collectors.groupingBy(MdmProductStock::getGroupKey));
            Map<String, Integer> monthSurplusMap = factoryMonthPlanProductionFinalResultService.calculateMonthSurplus(createCondition.getMonthPlanVersion(),finishedProductStocks,materialInfoMap);
            // 按优先级分离销售订单
            Map<Boolean, List<SalesOrderPool>> partitionedOrders =
                partitionSalesOrdersByPriority(salesOrders);
            Map<String,Integer> orderQtyMap = SaleRequirePlanHelper.calculateOrderQty(salesOrders);
            return new PredictionContext(
                salesOrders,
                orderQtyMap,
                finishedProductStocks,
                finishedProductStockMap,
                productionTypeMap,
                supplyOrderPools,
                partitionedOrders.get(false),
                partitionedOrders.get(true),
                monthSurplusMap,
                monthlySaleQty,
                minProductionQty,
                materialInfoMap,
                cycleSchStruConf
            );

        } catch (Exception e) {
            log.error("并行获取数据失败", e);
            throw new BusinessException("获取数据失败");
        }
    }

    private Set<String> getMaterialCodes(List<MdmMaterialInfo> materialInfos) {
        if(CollectionUtils.isEmpty(materialInfos)) {
            return Collections.emptySet();
        }
        return materialInfos.stream().map(MdmMaterialInfo::getMaterialCode).filter(materialCode -> StringUtils.isNotBlank(materialCode)).collect(Collectors.toSet());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<DpDemandPlan> createPredictionRequire(YearMonth currentMonth,DpDemandPlan createCondition,MpFactoryProductionVersion finalVersion,PredictionContext predictionContext) {
        if(CollectionUtils.isEmpty(predictionContext.getPredictOffsetDetails())) {
            return Collections.emptyList();
        }
        List<FactoryMonthPlanMouldDayResult> productionFinalResults;
        if(YesOrNoEnum.YES.getCode().equals(finalVersion.getIsFinal())) {
            productionFinalResults = factoryMonthPlanProductionFinalResultService.findFinalProductionResult(finalVersion);
        }else{
            productionFinalResults = factoryMonthPlanProductionFinalResultService.findProductionFinalResult(finalVersion);
        }
        if(CollectionUtils.isEmpty(productionFinalResults)) {
            return Collections.emptyList();
        }
        createCondition.setYear(currentMonth.getYear());
        createCondition.setMonth(currentMonth.getMonthValue());
        createCondition.setIncludePostpone(true);
        // 1、生成预测版本号(PRE+yyyymmdd+3位流水号)
        String predictionVersion = requirementVersionService.generateVersion(createCondition.getPrefix());
        createCondition.setFactoryCode(StringUtils.isBlank(createCondition.getFactoryCode())?FactoryConstant.DEFAULT_FACTORY_CODE:createCondition.getFactoryCode());
        createCondition.setMonthPlanVersion(predictionVersion);
        List<MpMonthPlanMonitor>  mpMonthPlanMonitors = this.monthPlanMonitorService.findCompleteQty(finalVersion);
        List<DpOrderOffsetDetail>   fetchSupplyOrders =  this.dpOrderPoolSnapshotService.loadSupplyOrder(createCondition,finalVersion);
        List<DpOrderOffsetDetail>  netDemands =   predictionContext.getPredictOffsetDetails();
        if(!CollectionUtils.isEmpty(fetchSupplyOrders)) {
            netDemands.addAll(fetchSupplyOrders);
        }
        List<DpOrderOffsetDetail>  leftDemands = PredictionAllocationHelper.calculateSaleOrder(createCondition,netDemands,productionFinalResults,mpMonthPlanMonitors);
        if(CollectionUtils.isEmpty(leftDemands)){
            predictionContext.setPredictOffsetDetails(Collections.emptyList());
            return Collections.emptyList();
        }
        leftDemands =   leftDemands.stream().filter(item -> null != item.getProduceQtyDue() && item.getProduceQtyDue() > 0).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(leftDemands)){
            predictionContext.setPredictOffsetDetails(Collections.emptyList());
            return Collections.emptyList();
        }
        predictionContext.setPredictOffsetDetails(leftDemands);
        List<SupplyOrderPool> supplyOrderPools = Lists.newArrayList();
        if(ProductionPlanType.PREDICTION.getPlanType().equals(createCondition.getPlanType())) {
            supplyOrderPools = this.createSupplyOrder(createCondition,currentMonth);
        }
        // 6. 处理需求计划生成
        List<DpDemandPlan> rawPlans = generateDemandPlans(createCondition,leftDemands,supplyOrderPools);
        // 7: 计划合并和持久化
        List<DpDemandPlan> finalPlans = mergeAndPersistPlans(createCondition,predictionContext, rawPlans);
        PredictionContext.OrderAllocationResult allocationResult = new PredictionContext.OrderAllocationResult(null,null,null);
        // 8: 后续处理
        postProcess(createCondition, predictionContext, finalPlans,allocationResult);
        return finalPlans;
    }

    private List<DpDemandPlan> generateDemandPlans(DpDemandPlan createCondition, List<DpOrderOffsetDetail>  leftDemands, List<SupplyOrderPool> supplyOrderPools) {
        List<DpDemandPlan> demandPlans = new ArrayList<>();
        List<DpOrderOffsetDetail> orderOffsetDetails = null;
        // 处理净需求
        if (!CollectionUtils.isEmpty(leftDemands)) {
            List<String> scmPriorities = Lists.newArrayList(ApsConstant.SAL_PRIORITY_HIGHT,ApsConstant.SAL_PRIORITY_MID,ApsConstant.SAL_PRIORITY_POSTPONE);
            orderOffsetDetails =   leftDemands.stream().filter(item -> scmPriorities.contains(item.getScmPriority()) && null != item.getProduceQtyDue() && item.getProduceQtyDue() > 0).collect(Collectors.toList());
        }
        if(!CollectionUtils.isEmpty(orderOffsetDetails)) {
            orderOffsetDetails.forEach(leftDemand -> demandPlans.add(buildDemandPlanFromAllocation(createCondition,leftDemand)));
        }
        // 处理供应链订单
        if (!CollectionUtils.isEmpty(supplyOrderPools)) {
            demandPlans.addAll(transformSupplyOrdersToDemandPlans(supplyOrderPools, createCondition));
        }
        return demandPlans;

    }


    private  DpDemandPlan buildDemandPlanFromAllocation(DpDemandPlan createCondition,DpOrderOffsetDetail netDemand) {
        DpDemandPlan demandPlan = new DpDemandPlan();
        BeanUtils.copyProperties(netDemand, demandPlan);
        demandPlan.setFactoryCode(createCondition.getFactoryCode());
        demandPlan.setYear(createCondition.getYear());
        demandPlan.setMonth(createCondition.getMonth());
        demandPlan.setMonthPlanVersion(createCondition.getMonthPlanVersion());
        demandPlan.setPlanType(createCondition.getPlanType());
        demandPlan.setOrderQty(netDemand.getOrderQty());
        demandPlan.setIsDynamicBalance(YesOrNoEnum.YES.getCode().equals(netDemand.getIsDynamicBalance())?YesOrNoEnum.YES.getCode():YesOrNoEnum.NO.getCode());
        demandPlan.setIsUniformity(YesOrNoEnum.YES.getCode().equals(netDemand.getIsUniformity())?YesOrNoEnum.YES.getCode():YesOrNoEnum.NO.getCode());
        demandPlan.setNetQty(netDemand.getProduceQtyDue());
        demandPlan.setYearWeek(StringUtils.isBlank(netDemand.getWeekYear())?ZERO_YEAR_WEEK:netDemand.getWeekYear());
        return demandPlan;
    }

    @Override
    public PredictionContext buildPredictionContext(String factoryCode) {
        DpDemandPlan param = new DpDemandPlan();
        param.setFactoryCode(factoryCode);
        CompletableFuture<List<SalesOrderPool>> salesOrdersFuture =
            CompletableFuture.supplyAsync(() -> this.fetchSalesOrderPool(factoryCode));
        CompletableFuture<List<MdmProductStock>> stocksFuture =
            CompletableFuture.supplyAsync(() ->  this.fetchFinishedProductStocks(factoryCode));
        CompletableFuture<Map<String, String>> productionTypeFuture =
            CompletableFuture.supplyAsync(() ->  this.fetchProductionTypeMap(factoryCode));
        CompletableFuture<Map<String, Integer>> monthlySaleQtyFuture =
            CompletableFuture.supplyAsync(() -> this.findCurrentMonthlySaleQty(factoryCode));
        CompletableFuture<Integer> minProductionQtyFuture =
            CompletableFuture.supplyAsync(this::getMinProductionQty);
        CompletableFuture<Map<String, MdmMaterialInfo>> fetchMaterialInfoFuture =
            CompletableFuture.supplyAsync(() -> this.fetchMaterialInfo(param));
        CompletableFuture<List<MdmCycleSchStruConf>> cycleSchStruConfFuture =
            CompletableFuture.supplyAsync(() -> mdmCycleSchStruConfService.findCycleSchStruConf(factoryCode));
        // 等待所有任务完成
        CompletableFuture.allOf(
            salesOrdersFuture, stocksFuture,productionTypeFuture,minProductionQtyFuture,fetchMaterialInfoFuture,monthlySaleQtyFuture,
            cycleSchStruConfFuture
        ).join();
        try {
            List<SalesOrderPool> salesOrders = salesOrdersFuture.get();
            List<MdmProductStock> finishedProductStocks = stocksFuture.get();
            Map<String, String> productionTypeMap = productionTypeFuture.get();
            int minProductionQty = minProductionQtyFuture.get();
            Map<String, MdmMaterialInfo> materialInfoMap = fetchMaterialInfoFuture.get();
            Map<String, Integer>  monthlySaleQty = monthlySaleQtyFuture.get();
            List<MdmCycleSchStruConf> cycleSchStruConfs = cycleSchStruConfFuture.get();
            if(!CollectionUtils.isEmpty(finishedProductStocks)){
                finishedProductStocks.forEach(finishedProductStock -> finishedProductStock.setLeftOverQty(null == finishedProductStock.getStockQty()?BigDecimal.ZERO.intValue():finishedProductStock.getStockQty()));
            }
            // 按优先级分离销售订单
            Map<Boolean, List<SalesOrderPool>> partitionedOrders =
                partitionSalesOrdersByPriority(salesOrders);

            // 处理成品库存映射
            Map<String, List<MdmProductStock>> finishedProductStockMap =
                CollectionUtils.isEmpty(finishedProductStocks) ?
                    new HashMap<>(16) :
                    finishedProductStocks.stream()
                        .collect(Collectors.groupingBy(MdmProductStock::getGroupKey));
            Map<String,Integer> orderQtyMap = SaleRequirePlanHelper.calculateOrderQty(salesOrders);

            return new PredictionContext(
                salesOrders,
                orderQtyMap,
                finishedProductStocks,
                finishedProductStockMap,
                productionTypeMap,
                null,
                partitionedOrders.get(false),
                partitionedOrders.get(true),
                null,
                monthlySaleQty,
                minProductionQty,
                materialInfoMap,
                cycleSchStruConfs
            );

        } catch (Exception e) {
            log.error("并行获取数据失败", e);
            throw new BusinessException("获取数据失败");
        }

    }

    @Override
    public List<DpDemandPlan> createInitPredictionRequire(DpDemandPlan createCondition, MpFactoryProductionVersion finalVersion, PredictionContext predictionContext) {
        YearMonth tMonth = YearMonth.of(finalVersion.getYear(), finalVersion.getMonth());
        createCondition.setYear(tMonth.getYear());
        createCondition.setMonth(tMonth.getMonthValue());
        // 1、生成预测版本号(PRE+yyyymmdd+3位流水号)
        String predictionVersion = requirementVersionService.generateVersion(createCondition.getPrefix());
        createCondition.setFactoryCode(StringUtils.isBlank(createCondition.getFactoryCode())?FactoryConstant.DEFAULT_FACTORY_CODE:createCondition.getFactoryCode());
        createCondition.setMonthPlanVersion(predictionVersion);
        Map<String, Integer> initialData = this.factoryMonthPlanProductionFinalResultService.calculateMonthSurplus(predictionVersion,predictionContext.getFinishedProductStocks(),predictionContext.getMaterialInfoMap());
        Map<String, Integer> originalMonthSurplusMap;
        Map<String, Integer> monthSurplusMap;
        if(!CollectionUtils.isEmpty(initialData)) {
            // 深度拷贝：创建新的HashMap，确保与原始数据隔离
            originalMonthSurplusMap = Collections.unmodifiableMap(
                new HashMap<>(initialData)
            );
            // 工作Map是原始数据的可修改副本
            monthSurplusMap = new HashMap<>(originalMonthSurplusMap);
        }else{
            originalMonthSurplusMap = Collections.emptyMap();
            monthSurplusMap = Collections.emptyMap();
        }
        predictionContext.setMonthSurplusMap(monthSurplusMap);
        predictionContext.setOriginalMonthSurplusMap(originalMonthSurplusMap);
        // 3. 处理销售订单分配
        PredictionContext.OrderAllocationResult allocationResult = processSalesOrderAllocation(tMonth,
            predictionVersion, predictionContext.getSalesOrders(), predictionContext.getFinishedProductStockMap(),
            predictionContext.getMonthSurplusMap(),predictionContext.getMaterialInfoMap());
        predictionContext.setPredictOffsetDetails(allocationResult.getAllocations());
        AlternateMaterialSelector.setAlternateMaterialFlag(allocationResult.getNetDemands(),predictionContext.getFinishedProductStockMap());
        List<SupplyOrderPool>   fetchSupplyOrders =  this.dpOrderPoolSnapshotService.fetchSupplyOrder(finalVersion);
        predictionContext.setSupplyOrderPools(fetchSupplyOrders);
        predictionContext.setPostponeOrders(null);
        // 6. 处理需求计划生成
        List<DpDemandPlan> rawPlans = generateDemandPlans(createCondition, allocationResult.getNetDemands(), predictionContext);
        // 7: 计划合并和持久化
        List<DpDemandPlan> finalPlans = mergeAndPersistPlans(createCondition,predictionContext, rawPlans);
        // 8: 后续处理
        postProcess(createCondition, predictionContext, finalPlans,allocationResult);
        return finalPlans;
    }

    private List<SupplyOrderPool> createSupplyOrder(DpDemandPlan createCondition,YearMonth yearMonth)  {
        // 8、按【生成周期排产】、【生成储备排产】的逻辑得到T+1月的周期排产储备和常规储备数据(此时T月的月度计划已有，故而结构最新排产月份会有变化)
        // 13、按【生成周期排产】、【生成储备排产】的逻辑得到T+2月的周期排产储备和常规储备数据(此时T+1月的月度计划已预测，故而结构最新排产月份会有变化)
        SupplyOrderPool param = new SupplyOrderPool();
        param.setFactoryCode(createCondition.getFactoryCode());
        param.setYear(yearMonth.getYear());
        param.setMonth(yearMonth.getMonthValue());
        param.setSourceType(createCondition.getPlanType());
        param.setPredictionVersion(createCondition.getMonthPlanVersion());
        // 生成周期排产储备
        List<SupplyOrderPool> cycleStockUpOrders =  cycleStockUpService.createCycleStockUp(param,false);
        // 生成常规储备
        List<SupplyOrderPool>  precedentStockUpOrders =  precedentStockUpService.createPrecedentStockUp(param,false);
        List<SupplyOrderPool> allStockUpOrders = Lists.newArrayList();
        if(!CollectionUtils.isEmpty(cycleStockUpOrders)){
            allStockUpOrders.addAll(cycleStockUpOrders);
        }
        if(!CollectionUtils.isEmpty(precedentStockUpOrders)){
            allStockUpOrders.addAll(precedentStockUpOrders);
        }
        return allStockUpOrders;
    }

    /**
     * 获取最小投产量
     * @return 最小投产量
     */
    private int getMinProductionQty() {
        FactoryParam factoryParam = new FactoryParam();
        factoryParam.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        factoryParam.setParamCode(MonthPlanEnums.MIN_PRODUCTION_QTY.getCode());
        factoryParam.setProductTypeCode(ProductTypeEnum.WHOLE_STEEL.getValue());
        FactoryParam param = factoryParamService.getFacParamSingle(factoryParam);
        int paramValue = BigDecimal.ZERO.intValue();
        if (param != null) {
            paramValue = StringUtils.isNotEmpty(param.getParamValue()) ? Integer.valueOf(param.getParamValue())
                : Integer.valueOf(param.getDefauleValue());
        }
        return paramValue;
    }


    private Map<String, MdmMaterialInfo> fetchMaterialInfo(DpDemandPlan createCondition) {
        return materialInfoService.skuToMaterialInfo(createCondition);
    }

    private void validateFinalizedForAdjust(DpDemandPlan createCondition) {
        long count = factoryProductionVersionMapper.selectCount(
            Wrappers.<MpFactoryProductionVersion>lambdaQuery()
                .eq(MpFactoryProductionVersion::getFactoryCode, createCondition.getFactoryCode())
                .eq(MpFactoryProductionVersion::getYear, createCondition.getYear())
                .eq(MpFactoryProductionVersion::getMonth, createCondition.getMonth())
                .eq(MpFactoryProductionVersion::getIsFinal,YesOrNoEnum.YES.getCode())
                .eq(MpFactoryProductionVersion::getIsDelete,YesOrNoEnum.NO.getCode())
        );
        if (count == 0) {
            // 	2、检查当月月度生产计划是否已定稿，若未定稿，提示“年月：XXX，还未定稿，不能调整！”；
            String yearMonth = String.format("%s%02d", createCondition.getYear(), createCondition.getMonth());
            String errorMsg = String.format(I18nUtil.getMessage("ui.data.alert.adjust.checkFinal"), yearMonth);
            throw new BusinessException(errorMsg);
        }
    }

    /**
     *  8. 保存订单池快照
     * @param createCondition 需求计划参数
     * @param salesOrders 销售订单
     * @param supplyOrderPools 供应链订单
     */
    private void saveOrderPoolSnapshot(DpDemandPlan createCondition, List<SalesOrderPool> salesOrders, List<SupplyOrderPool> supplyOrderPools) {
        dpOrderPoolSnapshotService.saveOrderPoolSnapshot(createCondition,salesOrders,supplyOrderPools);
    }

    /**
     * 保存需求计划
     */
    private List<DpDemandPlan> saveDemandPlans(
        DpDemandPlan createCondition,
        List<DpDemandPlan> demandPlans,
        PredictionContext data) {
        // 合并需求计划
        List<DpDemandPlan> mergedPlans = mergedDemandPlan(
            createCondition,
            demandPlans, data.getMinProductionQty(), data.getMaterialInfoMap(),
            data.getFinishedProductStockMap(), data.getOriginalMonthSurplusMap(),
            data.getProductionTypeMap(),
            data.getMonthlySaleQty(),
            data.getCycleSchStruConfs(),
            data.getOrderQtyMap());
        if (!CollectionUtils.isEmpty(mergedPlans)) {
            mergedPlans.sort(Comparator.comparing(DpDemandPlan::getMaterialCode));
            this.batchInsertProcessor.batchInsert(mergedPlans);
        }
        return mergedPlans;
    }

    /**
     * 验证生产版本是否已定稿
     */
    private void validateProductionVersionFinalized(DpDemandPlan createCondition) {
        Long count = factoryProductionVersionMapper.selectCount(
            Wrappers.<MpFactoryProductionVersion>lambdaQuery()
                .eq(MpFactoryProductionVersion::getFactoryCode, createCondition.getFactoryCode())
                .eq(MpFactoryProductionVersion::getYear, createCondition.getYear())
                .eq(MpFactoryProductionVersion::getMonth, createCondition.getMonth())
                .eq(MpFactoryProductionVersion::getIsFinal,YesOrNoEnum.YES.getCode())
        );
        if (count != null && count > 0) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.demandPlan.checkFinal"));
        }
    }

    /**
     * 并行获取所有必要数
     */
    private PredictionContext fetchRequiredDataInParallel(DpDemandPlan createCondition) {
        CompletableFuture<List<SalesOrderPool>> salesOrdersFuture =
            CompletableFuture.supplyAsync(() ->  this.fetchSalesOrderPool(createCondition.getFactoryCode()));

        CompletableFuture<List<MdmProductStock>> stocksFuture =
            CompletableFuture.supplyAsync(() ->  this.fetchFinishedProductStocks(createCondition.getFactoryCode()));

        CompletableFuture<Map<String, String>> productionTypeFuture =
            CompletableFuture.supplyAsync(() -> this.fetchProductionTypeMap(createCondition.getFactoryCode()));

        CompletableFuture<List<SupplyOrderPool>> supplyOrdersFuture =
            CompletableFuture.supplyAsync(() -> this.fetchSupplyOrderPool(createCondition));

        CompletableFuture<Map<String, Integer>> monthlySaleQtyFuture =
            CompletableFuture.supplyAsync(() -> this.findCurrentMonthlySaleQty(createCondition.getFactoryCode()));

        CompletableFuture<Integer> minProductionQtyFuture =
            CompletableFuture.supplyAsync(this::getMinProductionQty);
        CompletableFuture<Map<String, MdmMaterialInfo>> fetchMaterialInfoFuture =
            CompletableFuture.supplyAsync(() -> this.fetchMaterialInfo(createCondition));
        CompletableFuture<List<MdmCycleSchStruConf>> cycleSchStruConfFuture =
            CompletableFuture.supplyAsync(() ->  mdmCycleSchStruConfService.findCycleSchStruConf(createCondition.getFactoryCode()));
        // 等待所有任务完成
        CompletableFuture.allOf(
            salesOrdersFuture, stocksFuture, productionTypeFuture,
            supplyOrdersFuture, monthlySaleQtyFuture,minProductionQtyFuture,fetchMaterialInfoFuture,
            cycleSchStruConfFuture
        ).join();
        try {
            List<SalesOrderPool> salesOrders = salesOrdersFuture.get();
            List<MdmProductStock> finishedProductStocks = stocksFuture.get();
            Map<String, String> productionTypeMap = productionTypeFuture.get();
            List<SupplyOrderPool> supplyOrderPools = supplyOrdersFuture.get();
            Map<String, Integer>  monthlySaleQty = monthlySaleQtyFuture.get();
            int minProductionQty = minProductionQtyFuture.get();
            Map<String, MdmMaterialInfo> materialInfoMap = fetchMaterialInfoFuture.get();
            List<MdmCycleSchStruConf> cycleSchStruConf = cycleSchStruConfFuture.get();
            if(!CollectionUtils.isEmpty(finishedProductStocks)){
                finishedProductStocks.forEach(finishedProductStock -> finishedProductStock.setLeftOverQty(null == finishedProductStock.getStockQty()?BigDecimal.ZERO.intValue():finishedProductStock.getStockQty()));
            }
            // 处理成品库存映射
            Map<String, List<MdmProductStock>> finishedProductStockMap =
                CollectionUtils.isEmpty(finishedProductStocks) ?
                    new HashMap<>(16) :
                    finishedProductStocks.stream()
                        .collect(Collectors.groupingBy(MdmProductStock::getGroupKey));
            Map<String, Integer> monthSurplusMap = factoryMonthPlanProductionFinalResultService.calculateMonthSurplus(createCondition.getMonthPlanVersion(),finishedProductStocks,materialInfoMap);
            // 按优先级分离销售订单
            Map<Boolean, List<SalesOrderPool>> partitionedOrders =
                partitionSalesOrdersByPriority(salesOrders);
            Map<String,Integer> orderQtyMap = SaleRequirePlanHelper.calculateOrderQty(salesOrders);
            return new PredictionContext(
                salesOrders,
                orderQtyMap,
                finishedProductStocks,
                finishedProductStockMap,
                productionTypeMap,
                supplyOrderPools,
                partitionedOrders.get(false),
                partitionedOrders.get(true),
                monthSurplusMap,
                monthlySaleQty,
                minProductionQty,
                materialInfoMap,
                cycleSchStruConf
            );

        } catch (Exception e) {
            log.error("并行获取数据失败", e);
            throw new BusinessException("获取数据失败");
        }
    }

    /**
     * 处理销售订单分配
     */
    private PredictionContext.OrderAllocationResult processSalesOrderAllocation(
        YearMonth tMonth,
        String monthPlanVersion,
        List<SalesOrderPool> allocationOrders,
        Map<String, List<MdmProductStock>> finishedProductStockMap,
        Map<String, Integer> monthSurplusMap,
        Map<String, MdmMaterialInfo> materialInfoMap) {

        if (CollectionUtils.isEmpty(allocationOrders)) {
            return new PredictionContext.OrderAllocationResult(
                Collections.emptyList(),
                Collections.emptyList(),
                finishedProductStockMap
            );
        }
        // 分组销售订单
        Map<String, List<SalesOrderPool>> saleOrderGroupMap =
            SaleRequirePlanHelper.getGroupSalesOrder(allocationOrders);
        String weekYearForEudr = this.getWeekYearForEudr();
        // 计算库存分配
        List<DpOrderOffsetDetail> allocations = StockAllocationHelper.calculateStockAllocation(
            monthPlanVersion,tMonth, saleOrderGroupMap, finishedProductStockMap, monthSurplusMap,materialInfoMap,weekYearForEudr);
        return new PredictionContext.OrderAllocationResult(allocations, allocations, finishedProductStockMap);
    }



    /**
     * 生成需求计划
     */
    private List<DpDemandPlan> generateDemandPlans(
        DpDemandPlan createCondition,
        List<DpOrderOffsetDetail> netDemands,
        PredictionContext data) {
        List<DpDemandPlan> demandPlans = new ArrayList<>();
        // 处理净需求
        if (!CollectionUtils.isEmpty(netDemands)) {
            List<MdmAreaCapaAllocation> areaCapaAllocations =
                mdmAreaCapaAllocationService.findAreaCapaAllocation(createCondition.getYear(),createCondition.getMonth(),createCondition.getFactoryCode(), this.getDocTypeCode());
            demandPlans.addAll(SaleRequirePlanHelper.processNetDemands(createCondition,netDemands,areaCapaAllocations));
        }
        // 处理暂缓订单
        if (!CollectionUtils.isEmpty(data.getPostponeOrders())) {
            demandPlans.addAll(transformOrdersToDemandPlans(
                data.getPostponeOrders(), createCondition));
        }
        // 处理供应链订单
        if (!CollectionUtils.isEmpty(data.getSupplyOrderPools())) {
            demandPlans.addAll(transformSupplyOrdersToDemandPlans(
                data.getSupplyOrderPools(), createCondition));
        }
        return demandPlans;
    }

    /**
     * 转换销售订单为需求计划
     */
    private List<DpDemandPlan> transformOrdersToDemandPlans(
        List<SalesOrderPool> orders,
        DpDemandPlan createCondition) {

        return orders.stream()
            .map(order -> buildDemandPlan(order, createCondition))
            .collect(Collectors.toList());
    }

    /**
     * 转换供应链订单为需求计划
     */
    private List<DpDemandPlan> transformSupplyOrdersToDemandPlans(
        List<SupplyOrderPool> orders,
        DpDemandPlan createCondition) {

        return orders.stream()
            .map(order -> buildDemandPlan(order, createCondition))
            .collect(Collectors.toList());
    }

    /**
     * 获取销售订单池
     */
    private List<SalesOrderPool> fetchSalesOrderPool(String factoryCode) {
        return this.salesOrderPoolService.findCurrentSalesOrderPool(factoryCode);
    }

    /**
     * 获取成品库存
     */
    private List<MdmProductStock> fetchFinishedProductStocks(String factoryCode) {
        return this.mdmProductStockService.findCurrentFinishStock(factoryCode);
    }

    /**
     * 获取排产类型
     */
    private Map<String, String> fetchProductionTypeMap(String factoryCode) {
        return mdmSkuScheduleCategoryService.skuToProductionType(factoryCode);
    }

    /**
     * 获取供应链订单池
     */
    private List<SupplyOrderPool> fetchSupplyOrderPool(DpDemandPlan createCondition) {
        return this.supplyOrderPoolService.findCurrentSupplyOrderPool(createCondition);
    }


    private Map<String, Integer> findCurrentMonthlySaleQty(String factoryCode) {
        return  this.mpMonthlySaleQtyService.findCurrentMonthlySaleQty(factoryCode);
    }

    /**
     * 按优先级分离销售订单
     */
    private Map<Boolean, List<SalesOrderPool>> partitionSalesOrdersByPriority(List<SalesOrderPool> salesOrders) {
        if (CollectionUtils.isEmpty(salesOrders)) {
            Map<Boolean, List<SalesOrderPool>> result = new HashMap<>(2);
            result.put(Boolean.FALSE, Collections.emptyList());
            result.put(Boolean.TRUE, Collections.emptyList());
            return result;
        }

        return salesOrders.stream()
            .collect(Collectors.partitioningBy(
                item -> ApsConstant.SAL_PRIORITY_POSTPONE.equals(item.getScmPriority())
            ));
    }

    private List<DpDemandPlan> mergedDemandPlan(DpDemandPlan createCondition,List<DpDemandPlan> demandPlans,int minProductionQty,Map<String, MdmMaterialInfo> skuMap,Map<String,List<MdmProductStock>> finishedProductStockMap,Map<String,Integer> mdmMonthSurplusMap,Map<String, String> productionTypeMap,Map<String, Integer> monthlySaleQty,List<MdmCycleSchStruConf> cycleSchStruConfs,Map<String, Integer> orderQtyMap) {
        // 快速失败：空集合直接返回
        if (CollectionUtils.isEmpty(demandPlans)) {
            return Collections.emptyList();
        }
        Map<String, List<DpDemandPlan>> groupMap = DemandPlanGrouper.groupDemandPlans(createCondition,demandPlans);
        if(CollectionUtils.isEmpty(groupMap)) {
            return Collections.emptyList();
        }
        List<DpDemandPlan> list = Lists.newArrayList();
        groupMap.forEach((key, value) -> {
            // 获取基础模板（第一个元素）
            DpDemandPlan template = value.stream().filter(item -> key.equals(item.getGroupKey())).findFirst().orElse(null);
            if(null == template || !skuMap.containsKey(template.getMaterialCode())) {
                return;
            }
            list.add(buildMergedDemandPlan(
                template,
                value,
                minProductionQty,
                skuMap,
                finishedProductStockMap,
                mdmMonthSurplusMap,
                productionTypeMap,
                monthlySaleQty,
                cycleSchStruConfs,
                orderQtyMap));
        });
        log.info("groupKeys:{}",groupMap.keySet());
        return list;
    }

    private DpDemandPlan buildMergedDemandPlan(
        DpDemandPlan template,
        List<DpDemandPlan> groupPlans,
        int minProductionQty,
        Map<String, MdmMaterialInfo> skuMap,
        Map<String, List<MdmProductStock>> finishedProductStockMap,
        Map<String, Integer> mdmMonthSurplusMap,
        Map<String, String> productionTypeMap,
        Map<String, Integer> monthlySaleQty,
        List<MdmCycleSchStruConf> cycleSchStruConfs,
        Map<String, Integer> orderQtyMap) {
        // 使用构建器模式创建新对象（避免BeanCopyUtils的性能开销）
        DpDemandPlan mergedPlan = createMergedDemandPlan(template);
        // 设置替换料
        setIsAlternateMaterial(mergedPlan,groupPlans);
        // 设置物料信息（使用computeIfAbsent优化Map访问）
        setMaterialInfo(mergedPlan, skuMap,cycleSchStruConfs);
        // 设置库存和计划盈余
        setStockAndSurplusInfo(mergedPlan, finishedProductStockMap, mdmMonthSurplusMap);
        // 设置排产分类
        setProductionType(mergedPlan,productionTypeMap);
        // 计算并设置各类数量统计
        setQuantityStatistics(mergedPlan, groupPlans, minProductionQty,orderQtyMap);
        // 设置月均销量
        setAverageSaleQty(mergedPlan,monthlySaleQty);

        return mergedPlan;
    }

    private void setIsAlternateMaterial(DpDemandPlan mergedPlan, List<DpDemandPlan> groupPlans) {
        long count = groupPlans.stream().filter(item -> YesOrNoEnum.YES.getCode().equals(item.getIsAlternateMaterial())).count();
        mergedPlan.setIsAlternateMaterial(count > 0?YesOrNoEnum.YES.getCode():YesOrNoEnum.NO.getCode());
    }

    private void setAverageSaleQty(DpDemandPlan mergedPlan, Map<String, Integer> monthlySaleQty) {
        mergedPlan.setAverageSaleQty(monthlySaleQty.getOrDefault(mergedPlan.getMaterialCode(), 0));
    }

    private void setProductionType(DpDemandPlan mergedPlan, Map<String, String> productionTypeMap) {
        mergedPlan.setProductionType(productionTypeMap.getOrDefault(mergedPlan.getMaterialCode(),StringUtils.EMPTY));
    }

    /**
     * 创建合并后的需求计划对象
     * 使用浅拷贝 + 手动重置关键字段，性能优于BeanCopyUtils
     */
    private DpDemandPlan createMergedDemandPlan(DpDemandPlan template) {
        DpDemandPlan mergedPlan = BeanCopyUtils.copyBean(template,DpDemandPlan.class);
        // 重置ID和基础值
        mergedPlan.setId(null);
        mergedPlan.setIsDynamicBalance(YesOrNoEnum.YES.getCode().equals(mergedPlan.getIsDynamicBalance()) ? YesOrNoEnum.YES.getCode() : YesOrNoEnum.NO.getCode());
        mergedPlan.setIsUniformity(YesOrNoEnum.YES.getCode().equals(mergedPlan.getIsUniformity()) ? YesOrNoEnum.YES.getCode() : YesOrNoEnum.NO.getCode());
        mergedPlan.setBaseVale(null);
        return mergedPlan;
    }

    /**
     * 设置物料信息
     */
    private void setMaterialInfo(DpDemandPlan demandPlan, Map<String, MdmMaterialInfo> skuMap,List<MdmCycleSchStruConf> cycleSchStruConfs) {
        Optional.ofNullable(skuMap.get(demandPlan.getMaterialCode()))
            .ifPresent(materialInfo -> {
                demandPlan.setMaterialDesc(materialInfo.getMaterialDesc());
                demandPlan.setProductTypeCode(materialInfo.getProductTypeCode());
                demandPlan.setBrand(materialInfo.getBrand());
                demandPlan.setMesMaterialCode(materialInfo.getMesMaterialCode());
                demandPlan.setLocationType(materialInfo.getCommonType());
                demandPlan.setStructureName(materialInfo.getStructureName());
                demandPlan.setMainPattern(materialInfo.getMainPattern());
                demandPlan.setSpeed(materialInfo.getSpeed());
                demandPlan.setSpecifications(materialInfo.getSpecifications());
                demandPlan.setPattern(materialInfo.getPattern());
                demandPlan.setHierarchy(materialInfo.getHierarchy());
                demandPlan.setProSize(materialInfo.getProSize());
                demandPlan.setStructureType(this.setStructureType(demandPlan.getStructureName(),cycleSchStruConfs));
            });
    }

    private String setStructureType(String structureName, List<MdmCycleSchStruConf> cycleSchStruConfs) {
        if(StringUtils.isBlank(structureName) || CollectionUtils.isEmpty(cycleSchStruConfs)) {
            return ProductionGroupTypeEnum.CONVENTION.getGroupType();
        }
        long count = cycleSchStruConfs.stream().filter(item -> structureName.equals(item.getStructureName())).count();
        return count > 0?ProductionGroupTypeEnum.CYCLE.getGroupType():ProductionGroupTypeEnum.CONVENTION.getGroupType();
    }

    /**
     * 设置库存和计划盈余信息
     */
    private void setStockAndSurplusInfo(
        DpDemandPlan demandPlan,
        Map<String, List<MdmProductStock>> finishedProductStockMap,
        Map<String, Integer> mdmMonthSurplusMap) {

        String factoryMaterialKey = demandPlan.getGroupFactoryAndMaterialKey();

        // 计算库存数量（优化getStockQty方法）
        demandPlan.setStockQty(calculateStockQty(finishedProductStockMap, factoryMaterialKey));
        // 结余库存
        demandPlan.setRemainingQty(calculateRemainingQty(finishedProductStockMap, factoryMaterialKey));

        // 计算月底计划余量
        demandPlan.setPlannedSurplus(calculatePlannedSurplus(mdmMonthSurplusMap,factoryMaterialKey));
    }

    private int calculateRemainingQty(Map<String, List<MdmProductStock>> finishedProductStockMap, String groupKey) {
        if(CollectionUtils.isEmpty(finishedProductStockMap) || !finishedProductStockMap.containsKey(groupKey)){
            return BigDecimal.ZERO.intValue();
        }
        List<MdmProductStock> finishedProductStocks = finishedProductStockMap.get(groupKey);
        return finishedProductStocks.stream().filter(item -> null != item.getLeftOverQty()).mapToInt(MdmProductStock::getLeftOverQty).sum();
    }

    /**
     * 设置数量统计信息
     * 性能优化：单次遍历完成所有统计
     */
    private void setQuantityStatistics(
        DpDemandPlan demandPlan,
        List<DpDemandPlan> groupPlans,
        int minProductionQty,Map<String, Integer> orderQtyMap) {
        // 使用统计对象收集所有数据，避免多次遍历
        QuantityStatistics statistics = groupPlans.stream()
            .collect(QuantityStatistics::new, QuantityStatistics::accumulate, QuantityStatistics::combine);
        // 设置优先级相关数量
        // 设置基本数量
        demandPlan.setOrderQty(orderQtyMap.getOrDefault(demandPlan.getGroupFactoryAndMaterialKey(),0));
        demandPlan.setHeightQty(statistics.heightQty);
        demandPlan.setMidQty(statistics.midQty);
        demandPlan.setPostponeQty(statistics.postponeQty);
        demandPlan.setCycleReserveQty(statistics.cycleReserveQty);
        demandPlan.setConventionReserveQty(statistics.conventionReserveQty);

        // 计算派生数量
        calculateDerivedQuantities(demandPlan);

        // 设置生产和优先级标识
        setProductionAndPriorityFlags(demandPlan, minProductionQty, demandPlan.getNetQty());
    }



    private int calculateStockQty(Map<String, List<MdmProductStock>> finishedProductStockMap, String groupKey) {
        if(CollectionUtils.isEmpty(finishedProductStockMap) || !finishedProductStockMap.containsKey(groupKey)){
            return BigDecimal.ZERO.intValue();
        }
        List<MdmProductStock> finishedProductStocks = finishedProductStockMap.get(groupKey);
        return finishedProductStocks.stream().filter(item -> null != item.getStockQty()).mapToInt(MdmProductStock::getStockQty).sum();
    }

    private int calculatePlannedSurplus(Map<String, Integer> mdmMonthSurplusMap, String factoryMaterialKey) {
        if(CollectionUtils.isEmpty(mdmMonthSurplusMap) || !mdmMonthSurplusMap.containsKey(factoryMaterialKey)){
            return BigDecimal.ZERO.intValue();
        }
        return mdmMonthSurplusMap.get(factoryMaterialKey);
    }

    private DpDemandPlan buildDemandPlan(SupplyOrderPool supplyOrder, DpDemandPlan createCondition) {
        DpDemandPlan demandPlan = new DpDemandPlan();
        BeanUtils.copyProperties(supplyOrder, demandPlan);
        demandPlan.setFactoryCode(createCondition.getFactoryCode());
        demandPlan.setPlanType(createCondition.getPlanType());
        demandPlan.setYear(createCondition.getYear());
        demandPlan.setMonth(createCondition.getMonth());
        demandPlan.setYearWeek(ZERO_YEAR_WEEK);
        demandPlan.setIsDynamicBalance(YesOrNoEnum.NO.getCode());
        demandPlan.setIsUniformity(YesOrNoEnum.NO.getCode());
        demandPlan.setMonthPlanVersion(createCondition.getMonthPlanVersion());
        demandPlan.setOrderPriority(supplyOrder.getOrderType());
        demandPlan.setScmPriority(supplyOrder.getOrderType());
        demandPlan.setOrderQty(supplyOrder.getQty()==null? BigDecimal.ZERO.intValue() : supplyOrder.getQty());
        demandPlan.setNetQty(demandPlan.getOrderQty());
        return demandPlan;
    }

    private DpDemandPlan buildDemandPlan(SalesOrderPool postponeOrder, DpDemandPlan createCondition) {
        DpDemandPlan demandPlan = new DpDemandPlan();
        BeanUtils.copyProperties(postponeOrder, demandPlan);
        demandPlan.setFactoryCode(createCondition.getFactoryCode());
        demandPlan.setPlanType(createCondition.getPlanType());
        demandPlan.setYear(createCondition.getYear());
        demandPlan.setMonth(createCondition.getMonth());
        demandPlan.setMonthPlanVersion(createCondition.getMonthPlanVersion());
        demandPlan.setProductTypeCode(postponeOrder.getProductType());
        demandPlan.setMaterialCode(postponeOrder.getOriMaterialCode());
        demandPlan.setYearWeek(postponeOrder.getWeekYear());
        demandPlan.setIsDynamicBalance(YesOrNoEnum.YES.getCode().equals(postponeOrder.getIsDynamicBalance())?YesOrNoEnum.YES.getCode():YesOrNoEnum.NO.getCode());
        demandPlan.setIsUniformity(YesOrNoEnum.YES.getCode().equals(postponeOrder.getIsUniformity())?YesOrNoEnum.YES.getCode():YesOrNoEnum.NO.getCode());
        demandPlan.setOrderQty(postponeOrder.getOrdQty()==null? BigDecimal.ZERO.intValue() : postponeOrder.getOrdQty().intValue());
        demandPlan.setNetQty(demandPlan.getOrderQty());
        return demandPlan;
    }




    /**
     * 数量统计内部类
     * 使用累加器模式，单次遍历完成所有统计
     */
    private static class QuantityStatistics {
        int heightQty = 0;
        int midQty = 0;
        int postponeQty = 0;
        int cycleReserveQty = 0;
        int conventionReserveQty = 0;

        void accumulate(DpDemandPlan plan) {
            if (plan == null) {
                return;
            }
            // 根据订单优先级累加对应数量
            String priority = plan.getScmPriority();
            int netQty = plan.getNetQty()== null? BigDecimal.ZERO.intValue(): plan.getNetQty();

            if (ApsConstant.SAL_PRIORITY_HIGHT.equals(priority)) {
                heightQty += netQty;
            } else if (ApsConstant.SAL_PRIORITY_MID.equals(priority)) {
                midQty += netQty;
            } else if (ApsConstant.SAL_PRIORITY_POSTPONE.equals(priority)) {
                postponeQty += netQty;
            } else if (ApsConstant.SAL_PRIORITY_CYCLE_STOCK_UP.equals(priority)) {
                cycleReserveQty += netQty;
            } else if (ApsConstant.SAL_PRIORITY_PRECEDENT_STOCK_UP.equals(priority)) {
                conventionReserveQty += netQty;
            }
        }

        void combine(QuantityStatistics other) {
            this.heightQty += other.heightQty;
            this.midQty += other.midQty;
            this.postponeQty += other.postponeQty;
            this.cycleReserveQty += other.cycleReserveQty;
            this.conventionReserveQty += other.conventionReserveQty;
        }
    }

    /**
     * 计算派生数量
     */
    private void calculateDerivedQuantities(DpDemandPlan demandPlan) {
        // (8)净需求(含暂缓) = 高优先级净需求量 + 中优先级净需求量+暂缓订单需求量
        demandPlan.setPostponeNetQty(demandPlan.getHeightQty() + demandPlan.getMidQty() + demandPlan.getPostponeQty());

        // (9)净需求(不含暂缓) = 高优先级净需求量 + 中优先级净需求量
        demandPlan.setUnPostponeNetQty(demandPlan.getHeightQty() + demandPlan.getMidQty());
        // 实单高优先级+实单中优先级+周期排产储备
        demandPlan.setNetQty(demandPlan.getHeightQty() + demandPlan.getMidQty() + demandPlan.getCycleReserveQty());
    }

    /**
     * 设置标识
     */
    private void setProductionAndPriorityFlags(
        DpDemandPlan demandPlan,
        int minProductionQty,
        long totalNetQty) {

        // 生产标识
        demandPlan.setIsProduction(YesOrNoEnum.YES.getCode());
        // 供应链优先级
        demandPlan.setScmPriority(YesOrNoEnum.NO.getCode());
        // 是否达到最小生产量
        demandPlan.setIsReachMinProductionQty(
            totalNetQty >= minProductionQty ?
                YesOrNoEnum.YES.getCode() : YesOrNoEnum.NO.getCode());
        // 设置其他固定值
        demandPlan.setMinProductionQty(minProductionQty);
        demandPlan.setIsImport(YesOrNoEnum.NO.getCode());
    }


}

