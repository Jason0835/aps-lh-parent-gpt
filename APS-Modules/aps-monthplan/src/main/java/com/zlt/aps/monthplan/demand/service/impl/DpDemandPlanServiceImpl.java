package com.zlt.aps.monthplan.demand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.ProductionPlanType;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.exception.BusinessException;
import com.tlt.aps.utils.BeanCopyUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.service.IMdmAreaCapaAllocationService;
import com.zlt.aps.maindata.service.IMdmMaterialInfoService;
import com.zlt.aps.maindata.service.IMdmProductStockService;
import com.zlt.aps.maindata.service.IMdmSkuScheduleCategoryService;
import com.zlt.aps.maindata.service.IMpHistorySaleRecordService;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.DpOrderOffsetDetail;
import com.zlt.aps.monthplan.api.domain.entity.FactoryParam;

import com.zlt.aps.monthplan.api.domain.entity.MdmAreaCapaAllocation;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductStock;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import com.zlt.aps.monthplan.api.domain.entity.SupplyOrderPool;
import com.zlt.aps.monthplan.common.utils.RequirementVersionService;
import com.zlt.aps.monthplan.demand.mapper.DpDemandPlanEntityMapper;
import com.zlt.aps.monthplan.demand.service.IDpDemandPlanService;
import com.zlt.aps.monthplan.demand.service.IDpOrderPoolSnapshotService;
import com.zlt.aps.monthplan.demand.service.IDpStockVersionService;
import com.zlt.aps.monthplan.demand.service.ISalesOrderPoolService;
import com.zlt.aps.monthplan.demand.service.ISupplyOrderPoolService;
import com.zlt.aps.monthplan.factory.helper.SaleRequirePlanHelper;
import com.zlt.aps.monthplan.factory.helper.StockAllocationHelper;

import com.zlt.aps.monthplan.factory.mapper.MpFactoryProductionVersionMapper;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProductionFinalResultService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;

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
    private static final String PREFIX = "REQ";

    private final DpDemandPlanEntityMapper demandPlanEntityMapper;
    private final MpFactoryProductionVersionMapper factoryProductionVersionMapper;
    private final RequirementVersionService requirementVersionService;
    private final ISalesOrderPoolService salesOrderPoolService;
    // 成品库存
    private final IMdmProductStockService mdmProductStockService;
    // 定稿的月度排产计划
    private final IFactoryMonthPlanProductionFinalResultService factoryMonthPlanProductionFinalResultService;
    // 版本库存
    private final IDpStockVersionService dpStockVersionService;
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
    // 历史销售记录
    private final IMpHistorySaleRecordService mpHistorySaleRecordService;

    @Override
    protected String getDocTypeCode() {
        return "2025122521";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("2025122521");
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
        createCondition.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        // 获取操作日所在月份
        YearMonth currentMonth = YearMonth.from(LocalDate.now());
        // T月 = 当月 + 1个月
        YearMonth tMonth = currentMonth.plusMonths(1);
        createCondition.setYear(tMonth.getYear());
        createCondition.setMonth(tMonth.getMonthValue());
        // 1. 前置校验
        validateProductionVersionFinalized(createCondition);

        // 2. 生成版本号
        String monthPlanVersion = requirementVersionService.generateVersion(PREFIX);
        createCondition.setMonthPlanVersion(monthPlanVersion);

        // 3. 并行获取数据
        DataCollection data = fetchRequiredDataInParallel(monthPlanVersion);

        // 4. 处理销售订单分配
        OrderAllocationResult allocationResult = processSalesOrderAllocation(
            monthPlanVersion, data.getAllocationOrders(), data.getFinishedProductStockMap(),
            data.getMonthSurplusMap());

        // 5. 批量保存分配结果
        saveAllocationResults(createCondition, monthPlanVersion, allocationResult);

        // 6. 处理需求计划生成
        List<DpDemandPlan> demandPlans = generateDemandPlans(
            createCondition, allocationResult.getNetDemands(), data);

        // 7. 合并并保存需求计划
        if (CollectionUtils.isNotEmpty(demandPlans)) {
            saveDemandPlans(createCondition, demandPlans, data);
        }

        // 8. 保存订单池快照
        saveOrderPoolSnapshot(createCondition, data.getSalesOrders(), data.getSupplyOrderPools());
        // 9. 保存分厂排产版本
        saveFactoryProductionVersion(tMonth,monthPlanVersion,data.getSalesOrders());
    }

    private void saveFactoryProductionVersion(YearMonth yearMonth, String monthPlanVersion, List<SalesOrderPool> salesOrders) {
        MpFactoryProductionVersion version = new MpFactoryProductionVersion();
        version.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        version.setYear(yearMonth.getYear());
        version.setMonth(yearMonth.getMonthValue());
        version.setMonthPlanVersion(monthPlanVersion);
        version.setPlanType(ProductionPlanType.NORMAL.getPlanType());
        version.setIsFinal(YesOrNoEnum.NO.getCode());
        // 取销售订单的胎别
        if (CollectionUtils.isNotEmpty(salesOrders)) {
            SalesOrderPool saleOrder = salesOrders.get(0);
            version.setProductTypeCode(saleOrder.getProductType());
        }
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
    private void saveDemandPlans(
        DpDemandPlan createCondition,
        List<DpDemandPlan> demandPlans,
        DataCollection data) {

        // 获取最小投产量
        int minProductionQty = getMinProductionQty(
            createCondition.getFactoryCode(), ProductTypeEnum.WHOLE_STEEL.getValue());

        // 获取SKU映射
        Map<String, MdmMaterialInfo> skuMap = materialInfoService.skuToMaterialInfo();

        // 合并需求计划
        List<DpDemandPlan> mergedPlans = mergedDemandPlan(
            demandPlans, minProductionQty, skuMap,
            data.getFinishedProductStockMap(), data.getMonthSurplusMap(),data.getProductionTypeMap(),data.getMonthlySaleQty());

        if (CollectionUtils.isNotEmpty(mergedPlans)) {
            this.baseDao.insertBatch(mergedPlans);
        }
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
    private DataCollection fetchRequiredDataInParallel(String monthPlanVersion) {
        CompletableFuture<List<SalesOrderPool>> salesOrdersFuture =
            CompletableFuture.supplyAsync(this::fetchSalesOrderPool);

        CompletableFuture<List<MdmProductStock>> stocksFuture =
            CompletableFuture.supplyAsync(this::fetchFinishedProductStocks);

        CompletableFuture<Map<String, String>> productionTypeFuture =
            CompletableFuture.supplyAsync(this::fetchProductionTypeMap);

        CompletableFuture<List<SupplyOrderPool>> supplyOrdersFuture =
            CompletableFuture.supplyAsync(this::fetchSupplyOrderPool);

        CompletableFuture<Map<String, Integer>> monthlySaleQtyFuture =
            CompletableFuture.supplyAsync(this::findMonthlySaleQtyGroupByMaterialCode);

        // 等待所有任务完成
        CompletableFuture.allOf(
            salesOrdersFuture, stocksFuture, productionTypeFuture,
            supplyOrdersFuture, monthlySaleQtyFuture
        ).join();

        try {
            List<SalesOrderPool> salesOrders = salesOrdersFuture.get();
            List<MdmProductStock> finishedProductStocks = stocksFuture.get();
            Map<String, String> productionTypeMap = productionTypeFuture.get();
            List<SupplyOrderPool> supplyOrderPools = supplyOrdersFuture.get();

            Map<String, Integer>  monthlySaleQty = monthlySaleQtyFuture.get();
            // 处理成品库存映射
            Map<String, List<MdmProductStock>> finishedProductStockMap =
                CollectionUtils.isEmpty(finishedProductStocks) ?
                    new HashMap<>(16) :
                    finishedProductStocks.stream()
                        .collect(Collectors.groupingBy(MdmProductStock::getGroupKey));
            Map<String, Integer> monthSurplusMap = factoryMonthPlanProductionFinalResultService.calculateMonthSurplus(monthPlanVersion,finishedProductStocks);
            // 按优先级分离销售订单
            Map<Boolean, List<SalesOrderPool>> partitionedOrders =
                partitionSalesOrdersByPriority(salesOrders);

            return new DataCollection(
                salesOrders,
                finishedProductStocks,
                finishedProductStockMap,
                productionTypeMap,
                supplyOrderPools,
                partitionedOrders.get(false),
                partitionedOrders.get(true),
                monthSurplusMap,
                monthlySaleQty
            );

        } catch (Exception e) {
            log.error("并行获取数据失败", e);
            throw new BusinessException("获取数据失败");
        }
    }

    /**
     * 处理销售订单分配
     */
    private OrderAllocationResult processSalesOrderAllocation(
        String monthPlanVersion,
        List<SalesOrderPool> allocationOrders,
        Map<String, List<MdmProductStock>> finishedProductStockMap,
        Map<String, Integer> monthSurplusMap) {

        if (CollectionUtils.isEmpty(allocationOrders)) {
            return new OrderAllocationResult(
                Collections.emptyList(),
                Collections.emptyList(),
                finishedProductStockMap
            );
        }

        // 分组销售订单
        Map<String, List<SalesOrderPool>> saleOrderGroupMap =
            SaleRequirePlanHelper.getGroupSalesOrder(allocationOrders);
        // 获取操作日所在月份
        YearMonth currentMonth = YearMonth.from(LocalDate.now());
        // T月 = 当月 + 1个月
        YearMonth tMonth = currentMonth.plusMonths(1);
        // 计算库存分配
        List<DpOrderOffsetDetail> allocations = StockAllocationHelper.calculateStockAllocation(
            monthPlanVersion,tMonth, saleOrderGroupMap, finishedProductStockMap, monthSurplusMap);

        // 过滤净需求
        List<DpOrderOffsetDetail> netDemands = allocations.stream()
            .filter(allocation -> allocation.getProduceQtyDue() > 0)
            .collect(Collectors.toList());

        return new OrderAllocationResult(allocations, netDemands, finishedProductStockMap);
    }

    /**
     * 批量保存分配结果
     */
    private void saveAllocationResults(
        DpDemandPlan createCondition,
        String monthPlanVersion,
        OrderAllocationResult allocationResult) {

        // 批量插入分配结果
        if (CollectionUtils.isNotEmpty(allocationResult.getAllocations())) {
            this.baseDao.insertBatch(allocationResult.getAllocations());
        }

        // 批量插入库存版本
        dpStockVersionService.insertBatchData(
            createCondition, monthPlanVersion, allocationResult.getStockMap());
    }

    /**
     * 生成需求计划
     */
    private List<DpDemandPlan> generateDemandPlans(
        DpDemandPlan createCondition,
        List<DpOrderOffsetDetail> netDemands,
        DataCollection data) {

        List<DpDemandPlan> demandPlans = new ArrayList<>();

        // 处理净需求
        if (CollectionUtils.isNotEmpty(netDemands)) {
            List<MdmAreaCapaAllocation> areaCapaAllocations =
                mdmAreaCapaAllocationService.findAreaCapaAllocation(createCondition.getYear(),createCondition.getMonth());
            demandPlans.addAll(SaleRequirePlanHelper.processNetDemands(netDemands,areaCapaAllocations));
        }

        // 处理暂缓订单
        if (CollectionUtils.isNotEmpty(data.getPostponeOrders())) {
            demandPlans.addAll(transformOrdersToDemandPlans(
                data.getPostponeOrders(), createCondition));
        }

        // 处理供应链订单
        if (CollectionUtils.isNotEmpty(data.getSupplyOrderPools())) {
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
    private List<SalesOrderPool> fetchSalesOrderPool() {
        return this.salesOrderPoolService.findCurrentSalesOrderPool();
    }

    /**
     * 获取成品库存
     */
    private List<MdmProductStock> fetchFinishedProductStocks() {
        return this.mdmProductStockService.findCurrentFinishStock();
    }

    /**
     * 获取排产类型
     */
    private Map<String, String> fetchProductionTypeMap() {
        return mdmSkuScheduleCategoryService.skuToProductionType();
    }

    /**
     * 获取供应链订单池
     */
    private List<SupplyOrderPool> fetchSupplyOrderPool() {
        return this.supplyOrderPoolService.findCurrentSupplyOrderPool();
    }


    private Map<String, Integer> findMonthlySaleQtyGroupByMaterialCode() {
        return mpHistorySaleRecordService.calculateMonthSaleQty(6);
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
                item -> ApsConstant.SAL_PRIORITY_POSTPONE.equals(item.getOrderPriority())
            ));
    }

    private List<DpDemandPlan> mergedDemandPlan(List<DpDemandPlan> demandPlans,int minProductionQty,Map<String, MdmMaterialInfo> skuMap,Map<String,List<MdmProductStock>> finishedProductStockMap,Map<String,Integer> mdmMonthSurplusMap,Map<String, String> productionTypeMap,Map<String, Integer> monthlySaleQty) {
        // 快速失败：空集合直接返回
        if (CollectionUtils.isEmpty(demandPlans)) {
            return Collections.emptyList();
        }
        return demandPlans.parallelStream()
            .collect(Collectors.groupingByConcurrent(DpDemandPlan::getGroupKey))
            .values()
            .stream()
            .map(dpDemandPlans -> buildMergedDemandPlan(
                dpDemandPlans,
                minProductionQty,
                skuMap,
                finishedProductStockMap,
                mdmMonthSurplusMap,
                productionTypeMap,monthlySaleQty))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private DpDemandPlan buildMergedDemandPlan(
        List<DpDemandPlan> groupPlans,
        int minProductionQty,
        Map<String, MdmMaterialInfo> skuMap,
        Map<String, List<MdmProductStock>> finishedProductStockMap,
        Map<String, Integer> mdmMonthSurplusMap,
        Map<String, String> productionTypeMap,
        Map<String, Integer> monthlySaleQty) {

        // 验证分组数据有效性
        if (CollectionUtils.isEmpty(groupPlans)) {
            return null;
        }

        // 获取基础模板（第一个元素）
        DpDemandPlan template = groupPlans.get(0);
        if(!skuMap.containsKey(template.getMaterialCode())) {
            return null;
        }
        // 使用构建器模式创建新对象（避免BeanCopyUtils的性能开销）
        DpDemandPlan mergedPlan = createMergedDemandPlan(template);
        // 设置物料信息（使用computeIfAbsent优化Map访问）
        setMaterialInfo(mergedPlan, skuMap);
        // 设置库存和计划盈余
        setStockAndSurplusInfo(mergedPlan, finishedProductStockMap, mdmMonthSurplusMap);
        // 设置排产分类
        setProductionType(mergedPlan,productionTypeMap);
        // 计算并设置各类数量统计
        setQuantityStatistics(mergedPlan, groupPlans, minProductionQty);
        // 设置月均销量
        setAverageSaleQty(mergedPlan,monthlySaleQty);

        return mergedPlan;
    }

    private void setAverageSaleQty(DpDemandPlan mergedPlan, Map<String, Integer> monthlySaleQty) {
        mergedPlan.setAverageSaleQty(monthlySaleQty.getOrDefault(mergedPlan.getMaterialCode(), 0));
    }

    private void setProductionType(DpDemandPlan mergedPlan, Map<String, String> productionTypeMap) {
        mergedPlan.setProductionType(productionTypeMap.getOrDefault(mergedPlan.getGroupKey(),StringUtils.EMPTY));
    }

    /**
     * 创建合并后的需求计划对象
     * 使用浅拷贝 + 手动重置关键字段，性能优于BeanCopyUtils
     */
    private DpDemandPlan createMergedDemandPlan(DpDemandPlan template) {
        DpDemandPlan mergedPlan = BeanCopyUtils.copyBean(template,DpDemandPlan.class);
        // 重置ID和基础值
        mergedPlan.setId(null);
        mergedPlan.setBaseVale(null);
        return mergedPlan;
    }

    /**
     * 设置物料信息
     */
    private void setMaterialInfo(DpDemandPlan demandPlan, Map<String, MdmMaterialInfo> skuMap) {
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
            });
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
        demandPlan.setPlannedSurplus(calculatePlannedSurplus(mdmMonthSurplusMap, factoryMaterialKey));
    }

    private int calculateRemainingQty(Map<String, List<MdmProductStock>> finishedProductStockMap, String groupKey) {
        if(org.springframework.util.CollectionUtils.isEmpty(finishedProductStockMap) || !finishedProductStockMap.containsKey(groupKey)){
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
        int minProductionQty) {

        // 使用统计对象收集所有数据，避免多次遍历
        QuantityStatistics statistics = groupPlans.stream()
            .collect(QuantityStatistics::new, QuantityStatistics::accumulate, QuantityStatistics::combine);

        // 设置基本数量
        demandPlan.setOrderQty(statistics.totalOrderQty);
        demandPlan.setNetQty(statistics.totalNetQty);

        // 设置优先级相关数量
        demandPlan.setHeightQty(statistics.heightQty);
        demandPlan.setMidQty(statistics.midQty);
        demandPlan.setPostponeQty(statistics.postponeQty);
        demandPlan.setCycleReserveQty(statistics.cycleReserveQty);
        demandPlan.setConventionReserveQty(statistics.conventionReserveQty);

        // 计算派生数量
        calculateDerivedQuantities(demandPlan, statistics);

        // 设置生产和优先级标识
        setProductionAndPriorityFlags(demandPlan, groupPlans, minProductionQty, statistics.totalNetQty);
    }



    private int calculateStockQty(Map<String, List<MdmProductStock>> finishedProductStockMap, String groupKey) {
        if(org.springframework.util.CollectionUtils.isEmpty(finishedProductStockMap) || !finishedProductStockMap.containsKey(groupKey)){
            return BigDecimal.ZERO.intValue();
        }
        List<MdmProductStock> finishedProductStocks = finishedProductStockMap.get(groupKey);
        return finishedProductStocks.stream().filter(item -> null != item.getStockQty()).mapToInt(MdmProductStock::getStockQty).sum();
    }

    private int calculatePlannedSurplus(Map<String, Integer> mdmMonthSurplusMap, String groupFactoryAndMaterialKey) {
        if(org.springframework.util.CollectionUtils.isEmpty(mdmMonthSurplusMap) || !mdmMonthSurplusMap.containsKey(groupFactoryAndMaterialKey)){
            return BigDecimal.ZERO.intValue();
        }
        return mdmMonthSurplusMap.get(groupFactoryAndMaterialKey);
    }

    private DpDemandPlan buildDemandPlan(SupplyOrderPool supplyOrder, DpDemandPlan createCondition) {
        DpDemandPlan demandPlan = new DpDemandPlan();
        BeanUtils.copyProperties(supplyOrder, demandPlan);
        demandPlan.setYear(createCondition.getYear());
        demandPlan.setMonth(createCondition.getMonth());
        demandPlan.setMonthPlanVersion(createCondition.getMonthPlanVersion());
        demandPlan.setOrderPriority(supplyOrder.getOrderType());
        demandPlan.setOrderQty(supplyOrder.getQty()==null? BigDecimal.ZERO.intValue() : supplyOrder.getQty());
        demandPlan.setNetQty(demandPlan.getOrderQty());
        return demandPlan;
    }

    private DpDemandPlan buildDemandPlan(SalesOrderPool postponeOrder, DpDemandPlan createCondition) {
        DpDemandPlan demandPlan = new DpDemandPlan();
        BeanUtils.copyProperties(postponeOrder, demandPlan);
        demandPlan.setYear(createCondition.getYear());
        demandPlan.setMonth(createCondition.getMonth());
        demandPlan.setMonthPlanVersion(createCondition.getMonthPlanVersion());
        demandPlan.setProductTypeCode(postponeOrder.getProductType());
        demandPlan.setMaterialCode(postponeOrder.getOriMaterialCode());
        demandPlan.setYearWeek(postponeOrder.getWeekYear());
        demandPlan.setIsDynamicBalance(postponeOrder.getIsDynamicBalance());
        demandPlan.setIsUniformity(postponeOrder.getIsUniformity());
        demandPlan.setOrderQty(postponeOrder.getOrdQty()==null? BigDecimal.ZERO.intValue() : postponeOrder.getOrdQty().intValue());
        demandPlan.setNetQty(demandPlan.getOrderQty());
        return demandPlan;
    }


    /**
     * 获取最小投产量
     * @return 最小投产量
     */
    private int getMinProductionQty(String factoryCode, String productTypeCode) {
        FactoryParam factoryParam = new FactoryParam();
        factoryParam.setFactoryCode(factoryCode);
        factoryParam.setParamCode(MonthPlanEnums.MIN_PRODUCTION_QTY.getCode());
        factoryParam.setProductTypeCode(productTypeCode);
        FactoryParam param = factoryParamService.getFacParamSingle(factoryParam);
        int paramValue = BigDecimal.ZERO.intValue();
        if (param != null) {
            paramValue = StringUtils.isNotEmpty(param.getParamValue()) ? Integer.valueOf(param.getParamValue())
                : Integer.valueOf(param.getDefauleValue());
        }
        return paramValue;
    }

    /**
     * 数据集合
     */
    @Getter
    private static class DataCollection {
        private final List<SalesOrderPool> salesOrders;
        private final List<MdmProductStock> finishedProductStocks;
        private final Map<String, List<MdmProductStock>> finishedProductStockMap;
        private final Map<String, String> productionTypeMap;
        private final List<SupplyOrderPool> supplyOrderPools;
        private final List<SalesOrderPool> allocationOrders;
        private final List<SalesOrderPool> postponeOrders;
        private final Map<String, Integer> monthSurplusMap;
        private final Map<String, Integer>  monthlySaleQty;

        public DataCollection(
            List<SalesOrderPool> salesOrders,
            List<MdmProductStock> finishedProductStocks,
            Map<String, List<MdmProductStock>> finishedProductStockMap,
            Map<String, String> productionTypeMap,
            List<SupplyOrderPool> supplyOrderPools,
            List<SalesOrderPool> allocationOrders,
            List<SalesOrderPool> postponeOrders,
            Map<String, Integer> monthSurplusMap,
            Map<String, Integer>  monthlySaleQty) {
            this.salesOrders = salesOrders != null ? salesOrders : Collections.emptyList();
            this.finishedProductStocks = finishedProductStocks != null ? finishedProductStocks : Collections.emptyList();
            this.finishedProductStockMap = finishedProductStockMap != null ? finishedProductStockMap : new HashMap<>();
            this.productionTypeMap = productionTypeMap != null ? productionTypeMap : new HashMap<>();
            this.supplyOrderPools = supplyOrderPools != null ? supplyOrderPools : Collections.emptyList();
            this.allocationOrders = allocationOrders != null ? allocationOrders : Collections.emptyList();
            this.postponeOrders = postponeOrders != null ? postponeOrders : Collections.emptyList();
            this.monthSurplusMap = monthSurplusMap != null ? monthSurplusMap : new HashMap<>();
            this.monthlySaleQty = monthlySaleQty != null ? monthlySaleQty : new HashMap<>();
        }
    }

    /**
     * 订单分配结果
     */
    @Getter
    private static class OrderAllocationResult {
        private final List<DpOrderOffsetDetail> allocations;
        private final List<DpOrderOffsetDetail> netDemands;
        private final Map<String, List<MdmProductStock>> stockMap;

        public OrderAllocationResult(
            List<DpOrderOffsetDetail> allocations,
            List<DpOrderOffsetDetail> netDemands,
            Map<String, List<MdmProductStock>> stockMap) {
            this.allocations = allocations != null ? allocations : Collections.emptyList();
            this.netDemands = netDemands != null ? netDemands : Collections.emptyList();
            this.stockMap = stockMap != null ? stockMap : new HashMap<>();
        }
    }

    /**
     * 数量统计内部类
     * 使用累加器模式，单次遍历完成所有统计
     */
    private static class QuantityStatistics {
        int totalOrderQty = 0;
        int totalNetQty = 0;
        int heightQty = 0;
        int midQty = 0;
        int postponeQty = 0;
        int cycleReserveQty = 0;
        int conventionReserveQty = 0;

        void accumulate(DpDemandPlan plan) {
            if (plan == null) {
                return;
            }

            totalOrderQty += plan.getOrderQty() == null? BigDecimal.ZERO.intValue(): plan.getOrderQty();
            totalNetQty += plan.getNetQty()== null? BigDecimal.ZERO.intValue(): plan.getNetQty();

            // 根据订单优先级累加对应数量
            String priority = plan.getOrderPriority();
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
            this.totalOrderQty += other.totalOrderQty;
            this.totalNetQty += other.totalNetQty;
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
    private void calculateDerivedQuantities(DpDemandPlan demandPlan, QuantityStatistics statistics) {
        // (8)净需求(含暂缓) = 高优先级净需求量 + 中优先级净需求量+暂缓订单需求量
        demandPlan.setPostponeNetQty(statistics.heightQty + statistics.midQty + statistics.postponeQty);

        // (9)净需求(不含暂缓) = 高优先级净需求量 + 中优先级净需求量
        demandPlan.setUnPostponeNetQty(statistics.heightQty + statistics.midQty);
    }

    /**
     * 设置标识
     */
    private void setProductionAndPriorityFlags(
        DpDemandPlan demandPlan,
        List<DpDemandPlan> groupPlans,
        int minProductionQty,
        long totalNetQty) {

        // 生产标识
        demandPlan.setIsProduction(YesOrNoEnum.YES.getCode());

        // 供应链优先级
        if (groupPlans.size() > 1) {
            demandPlan.setScmPriority(StringUtils.EMPTY);
        }

        // 是否达到最小生产量
        demandPlan.setIsReachMinProductionQty(
            totalNetQty >= minProductionQty ?
                YesOrNoEnum.YES.getCode() : YesOrNoEnum.NO.getCode());
        // 设置其他固定值
        demandPlan.setMinProductionQty(minProductionQty);
        demandPlan.setPlanType(ProductionPlanType.NORMAL.getPlanType());
        demandPlan.setIsImport(YesOrNoEnum.NO.getCode());
    }


}
