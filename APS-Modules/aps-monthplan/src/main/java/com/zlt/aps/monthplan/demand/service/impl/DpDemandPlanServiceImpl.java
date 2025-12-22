package com.zlt.aps.monthplan.demand.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.datasource.service.BaseService;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.Constant;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.exception.BusinessException;
import com.tlt.aps.utils.BeanCopyUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.service.IMdmAreaCapaAllocationService;
import com.zlt.aps.maindata.service.IMdmMaterialInfoService;
import com.zlt.aps.maindata.service.IMdmProductStockService;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.common.utils.RequirementVersionService;
import com.zlt.aps.monthplan.demand.mapper.DpDemandPlanEntityMapper;
import com.zlt.aps.monthplan.demand.service.*;
import com.zlt.aps.monthplan.factory.helper.SaleRequirePlanHelper;
import com.zlt.aps.monthplan.factory.helper.StockAllocationHelper;
import com.zlt.aps.monthplan.factory.mapper.FactoryProductionVersionMapper;
import com.zlt.aps.monthplan.factory.service.IMpMonthPlanProdFinalService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：DpDemandPlanServiceImpl.java
 * 描    述：DpDemandPlanServiceImpl需求计划业务层处理
 *@author yelq
 *@date 2025-12-12
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DpDemandPlanServiceImpl extends BaseService<DpDemandPlan>  implements IDpDemandPlanService
{
    private static final String PREFIX = "REQ";
    private final DpDemandPlanEntityMapper dpDemandPlanEntityMapper;
    private final FactoryProductionVersionMapper factoryProductionVersionMapper;
    private final RequirementVersionService requirementVersionService;
    private final ISalesOrderPoolService salesOrderPoolService;
    // 成品库存
    private final IMdmProductStockService mdmProductStockService;
    // 定稿的月度排产计划
    private final IMpMonthPlanProdFinalService mpMonthPlanProdFinalService;
    // 订单分配表
    private final IDpOrderOffsetDetailService dpOrderOffsetDetailService;
    // 版本库存
    private final IDpStockVersionService dpStockVersionService;
    // 区域产能分配
    private final IMdmAreaCapaAllocationService mdmAreaCapaAllocationService;
    // SKU排产分类
    private final IMpSkuProductionTypeService mpSkuProductionTypeService;
    // 供应链订单
    private final ISupplyOrderPoolService supplyOrderPoolService;
    // 订单快照
    private final IDpOrderPoolSnapshotService dpOrderPoolSnapshotService;
    // 排产设定
    private final IFactoryParamService factoryParamService;
    // 物料信息
    private final IMdmMaterialInfoService materialInfoService;

    /**
     * 查询需求计划
     *
     * @param id 需求计划主键
     * @return 需求计划
     */
    @Override
    public DpDemandPlan selectDpDemandPlanById(Long id)
    {
        return dpDemandPlanEntityMapper.selectDpDemandPlanById(id);
    }

    /**
     * 查询需求计划列表
     *
     * @param dpDemandPlan 需求计划
     * @return 需求计划
     */
    @Override
    public List<DpDemandPlan> selectDpDemandPlanList(DpDemandPlan dpDemandPlan)
    {
        return dpDemandPlanEntityMapper.selectDpDemandPlanList(dpDemandPlan);
    }

    /**
     * 批量查询需求计划列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 需求计划集合
     */
    @Override
    public List<DpDemandPlan> selectDpDemandPlanByIds(List<Long> ids)
    {
        return super.executeSelectIn(
            dpDemandPlanEntityMapper::selectDpDemandPlanByIds
                    ,ids
        );
    }


    /**
     * 新增需求计划
     *
     * @param dpDemandPlan 需求计划
     * @return 结果
     */
    @Override
    public int insertDpDemandPlan(DpDemandPlan dpDemandPlan)
    {
        dpDemandPlan.setBaseVale(null);
        return dpDemandPlanEntityMapper.insert(dpDemandPlan);
    }

    /**
     * 修改需求计划
     *
     * @param dpDemandPlan 需求计划
     * @return 结果
     */
    @Override
    public int updateDpDemandPlan(DpDemandPlan dpDemandPlan)
    {
        dpDemandPlan.setBaseVale(dpDemandPlan.getId());
        return dpDemandPlanEntityMapper.update(dpDemandPlan);
    }

    /**
     * 批量删除需求计划
     *
     * @param ids 需要删除的需求计划主键
     * @return 结果
     */
    @Override
    public int deleteDpDemandPlanByIds(Long[] ids)
    {
        return dpDemandPlanEntityMapper.deleteDpDemandPlanByIds(ids);
    }

    /**
     * 批量删除需求计划
     *
     * @param ids 需要删除的需求计划主键
     * @return 结果
     */
    @Override
    public int deleteDpDemandPlanByIds(List<Long> ids)
    {
        Long[] arrayids = ids.toArray(new Long[0]);

        return this.deleteDpDemandPlanByIds(arrayids);
    }

    /**
     * 删除需求计划信息
     *
     * @param id 需求计划主键
     * @return 结果
     */
    @Override
    public int deleteDpDemandPlanById(Long id)
    {
        return dpDemandPlanEntityMapper.deleteDpDemandPlanById(id);
    }

    @Override
    public void insertBatchData(Collection<DpDemandPlan> dataList) {

        this.insertBatchData(dataList, DpDemandPlanEntityMapper.class);
    }

    @Override
    public void updateBatchData(Collection<DpDemandPlan> dataList) {

        this.updateBatchData(dataList, DpDemandPlanEntityMapper.class);
    }

    @Override
    public void mergerIntoBatchData(List<DpDemandPlan> list) {
        this.mergerIntoBatchData(list, DpDemandPlanEntityMapper.class);
    }

    /**
     * 校验需求计划唯一性
     */
    @Override
    public String checkDpDemandPlanUnique(DpDemandPlan dpDemandPlan) {
        if (dpDemandPlan == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<DpDemandPlan> list = dpDemandPlanEntityMapper.selectDpDemandPlanList(dpDemandPlan);
        if (CollectionUtils.isNotEmpty(list)) {
            long iCount = list.stream().filter(x->!x.getId().equals(dpDemandPlan.getId())).count();
            return iCount == 0 ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
    /**
     * 导入需求计划数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<DpDemandPlan> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<DpDemandPlan> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            DpDemandPlan dpDemandPlan = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, dpDemandPlan);
            ImportExcelValidatedUtils.validatedRepeat(list,dpDemandPlan,i,2,importLogId,validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                dpDemandPlan.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                dpDemandPlan.setBaseVale(null);
                importList.add(dpDemandPlan);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                dpDemandPlanEntityMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    DpDemandPlan dpDemandPlan = list.get(i);
                    // 错误记录跳过
                    if (dpDemandPlan.getId() != null && dpDemandPlan.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkDpDemandPlanUnique(dpDemandPlan);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertDpDemandPlan(dpDemandPlan);
                    } else {
                        failureNum++;
                        //TODO:此处需手动填写唯一校验失败国际化信息
                        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");
                        ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.REPEAT.getCode(),i + 2,
                                String.format(uniqueMsg, i + 2), importErrorLogs);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    @Override
    public void createMonthRequire(DpDemandPlan createCondition) {
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
        long minProductionQty = getMinProductionQty(
            createCondition.getFactoryCode(), ProductTypeEnum.WHOLE_STEEL.getValue());

        // 获取SKU映射
        Map<String, MdmMaterialInfo> skuMap = materialInfoService.skuToMaterialInfo();

        // 合并需求计划
        List<DpDemandPlan> mergedPlans = mergedDemandPlan(
            demandPlans, minProductionQty, skuMap,
            data.getFinishedProductStockMap(), data.getMonthSurplusMap(),data.getProductionTypeMap());

        if (CollectionUtils.isNotEmpty(mergedPlans)) {
            insertBatchData(mergedPlans);
        }
    }

    /**
     * 验证生产版本是否已定稿
     */
    private void validateProductionVersionFinalized(DpDemandPlan createCondition) {
        Long count = factoryProductionVersionMapper.selectCount(
            Wrappers.<FactoryProductionVersion>lambdaQuery()
                .eq(FactoryProductionVersion::getFactoryCode, createCondition.getFactoryCode())
                .eq(FactoryProductionVersion::getYear, createCondition.getYear())
                .eq(FactoryProductionVersion::getMonth, createCondition.getMonth())
                .eq(FactoryProductionVersion::getIsFinal, Constant.TRUE)
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

        CompletableFuture<Map<String, Long>> monthSurplusFuture =
            CompletableFuture.supplyAsync(() -> this.fetchMonthSurplusMap(monthPlanVersion));

        // 等待所有任务完成
        CompletableFuture.allOf(
            salesOrdersFuture, stocksFuture, productionTypeFuture,
            supplyOrdersFuture, monthSurplusFuture
        ).join();

        try {
            List<SalesOrderPool> salesOrders = salesOrdersFuture.get();
            List<MdmProductStock> finishedProductStocks = stocksFuture.get();
            Map<String, String> productionTypeMap = productionTypeFuture.get();
            List<SupplyOrderPool> supplyOrderPools = supplyOrdersFuture.get();
            Map<String, Long> monthSurplusMap = monthSurplusFuture.get();

            // 处理成品库存映射
            Map<String, List<MdmProductStock>> finishedProductStockMap =
                CollectionUtils.isEmpty(finishedProductStocks) ?
                    new HashMap<>(16) :
                    finishedProductStocks.stream()
                        .collect(Collectors.groupingBy(MdmProductStock::getGroupKey));

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
                monthSurplusMap
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
        Map<String, Long> monthSurplusMap) {

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

        // 计算库存分配
        List<DpOrderOffsetDetail> allocations = StockAllocationHelper.calculateStockAllocation(
            monthPlanVersion, saleOrderGroupMap, finishedProductStockMap, monthSurplusMap);

        // 过滤净需求
        List<DpOrderOffsetDetail> netDemands = allocations.stream()
            .filter(allocation -> allocation.getProducionQty() > 0)
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
            this.dpOrderOffsetDetailService.insertBatchData(allocationResult.getAllocations());
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
            demandPlans.addAll(processNetDemands(createCondition, netDemands));
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
     * 处理净需求
     */
    private List<DpDemandPlan> processNetDemands(
        DpDemandPlan createCondition,
        List<DpOrderOffsetDetail> netDemands) {

        List<MdmAreaCapaAllocation> areaCapaAllocations =
            mdmAreaCapaAllocationService.findAreaCapaAllocation(createCondition);

        if (CollectionUtils.isEmpty(areaCapaAllocations)) {
            return transformAllocationsToDemandPlans(netDemands);
        }

        return processNetDemandsWithCapacity(netDemands, areaCapaAllocations);
    }

    /**
     * 转换订单分配为需求计划
     */
    private List<DpDemandPlan> transformAllocationsToDemandPlans(
        List<DpOrderOffsetDetail> orders) {

        return orders.stream()
            .map(this::buildDemandPlanFromAllocation)
            .collect(Collectors.toList());
    }

    /**
     * 处理有产能配置的净需求
     */
    private List<DpDemandPlan> processNetDemandsWithCapacity(
        List<DpOrderOffsetDetail> netDemands,
        List<MdmAreaCapaAllocation> areaCapaAllocations) {
        List<DpDemandPlan> result = new ArrayList<>();
        // 按区域分组净需求
        Map<String, List<DpOrderOffsetDetail>> demandsByArea = netDemands.stream()
            .collect(Collectors.groupingBy(DpOrderOffsetDetail::getAreaCode));
        // 按区域分组产能配置
        Map<String, List<MdmAreaCapaAllocation>> capacityByArea = areaCapaAllocations.stream()
            .collect(Collectors.groupingBy(MdmAreaCapaAllocation::getAreaCode));
        // 处理每个区域
        demandsByArea.forEach((areaCode, orders) -> {
            List<DpOrderOffsetDetail> sortedOrders = sortOrdersByPriority(orders);
            List<MdmAreaCapaAllocation> areaCapacities = capacityByArea.get(areaCode);

            if (CollectionUtils.isEmpty(areaCapacities)) {
                result.addAll(transformAllocationsToDemandPlans(sortedOrders));
                return;
            }

            // 计算总产能和总需求
            long totalCapacity = areaCapacities.stream()
                .mapToLong(MdmAreaCapaAllocation::getCapacityAllocation)
                .sum();

            long totalDemand = sortedOrders.stream()
                .mapToLong(DpOrderOffsetDetail::getProducionQty)
                .sum();

            // 调整优先级
            if (totalDemand >= totalCapacity) {
                processDemandPriorityExcludingLast(sortedOrders, totalDemand - totalCapacity);
            } else {
                sortedOrders.forEach(order ->
                    order.setOrderPriority(ApsConstant.SAL_PRIORITY_HIGHT));
            }

            result.addAll(transformAllocationsToDemandPlans(sortedOrders));
        });

        return result;
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
        return mpSkuProductionTypeService.skuToProductionType();
    }

    /**
     * 获取供应链订单池
     */
    private List<SupplyOrderPool> fetchSupplyOrderPool() {
        return this.supplyOrderPoolService.findCurrentSupplyOrderPool();
    }

    /**
     * 计算月底计划余量 查询获取所有成品库存；同时计算月底计划余量：库存抓取日~（同月）月底的月度计划量汇总
     */
    private Map<String, Long> fetchMonthSurplusMap(String monthPlanVersion) {
        return mpMonthPlanProdFinalService.calculateMonthSurplus(monthPlanVersion);
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

    private List<DpDemandPlan> mergedDemandPlan(List<DpDemandPlan> demandPlans,long minProductionQty,Map<String, MdmMaterialInfo> skuMap,Map<String,List<MdmProductStock>> finishedProductStockMap,Map<String,Long> mdmMonthSurplusMap,Map<String, String> productionTypeMap) {
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
                productionTypeMap))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private DpDemandPlan buildMergedDemandPlan(
        List<DpDemandPlan> groupPlans,
        long minProductionQty,
        Map<String, MdmMaterialInfo> skuMap,
        Map<String, List<MdmProductStock>> finishedProductStockMap,
        Map<String, Long> mdmMonthSurplusMap,
        Map<String, String> productionTypeMap) {

        // 验证分组数据有效性
        if (CollectionUtils.isEmpty(groupPlans)) {
            return null;
        }

        // 获取基础模板（第一个元素）
        DpDemandPlan template = groupPlans.get(0);
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

        return mergedPlan;
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
        Map<String, Long> mdmMonthSurplusMap) {

        String factoryMaterialKey = demandPlan.getGroupFactoryAndMaterialKey();

        // 计算库存数量（优化getStockQty方法）
        demandPlan.setStockQty(calculateStockQty(finishedProductStockMap, factoryMaterialKey));

        // 计算月底计划余量
        demandPlan.setPlannedSurplus(calculatePlannedSurplus(mdmMonthSurplusMap, factoryMaterialKey));
    }

    /**
     * 设置数量统计信息
     * 性能优化：单次遍历完成所有统计
     */
    private void setQuantityStatistics(
        DpDemandPlan demandPlan,
        List<DpDemandPlan> groupPlans,
        long minProductionQty) {

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



    private Long calculateStockQty(Map<String, List<MdmProductStock>> finishedProductStockMap, String groupKey) {
        if(org.springframework.util.CollectionUtils.isEmpty(finishedProductStockMap) || !finishedProductStockMap.containsKey(groupKey)){
            return BigDecimal.ZERO.longValue();
        }
        List<MdmProductStock> finishedProductStocks = finishedProductStockMap.get(groupKey);
        return finishedProductStocks.stream().mapToLong(MdmProductStock::getStockQty).sum();
    }

    private Long calculatePlannedSurplus(Map<String, Long> mdmMonthSurplusMap, String groupFactoryAndMaterialKey) {
        if(org.springframework.util.CollectionUtils.isEmpty(mdmMonthSurplusMap) || !mdmMonthSurplusMap.containsKey(groupFactoryAndMaterialKey)){
            return BigDecimal.ZERO.longValue();
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
        demandPlan.setOrderQty(supplyOrder.getQty()==null? BigDecimal.ZERO.longValue() : supplyOrder.getQty());
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
        demandPlan.setOrderQty(postponeOrder.getOrdQty()==null? BigDecimal.ZERO.longValue() : postponeOrder.getOrdQty().longValue());
        demandPlan.setNetQty(demandPlan.getOrderQty());
        return demandPlan;
    }

    /**
     * 从列表尾端开始累加净需求量，直到达到或超过指定值
     * 注意：跳出循环的那个订单不修改优先级
     *
     * @param sortedOrders 排序后的净需求列表
     * @param overAreaCapacityValue 超出区域产能值
     */
    public void processDemandPriorityExcludingLast(
        List<DpOrderOffsetDetail> sortedOrders,
        long overAreaCapacityValue) {

        if (CollectionUtils.isEmpty(sortedOrders) || overAreaCapacityValue <= 0) {
            return;
        }
        long accumulatedQty = 0;
        // 从列表尾端开始遍历
        for (int i = sortedOrders.size() - 1; i >= 0; i--) {
            DpOrderOffsetDetail order = sortedOrders.get(i);
            // 跳过已处理或无效的订单
            if (order == null || order.getProducionQty() == null || order.getProducionQty() <= 0) {
                continue;
            }
            // 检查当前累加值是否已经达到或超过阈值
            // 注意：先检查，再累加
            long currentOrderQty = order.getProducionQty();
            if (accumulatedQty + currentOrderQty >= overAreaCapacityValue) {
                break;
            } else {
                // 累加净需求量并设置优先级
                accumulatedQty += currentOrderQty;
                order.setOrderPriority(ApsConstant.SAL_PRIORITY_MID);
            }
        }
    }

    private List<DpOrderOffsetDetail> sortOrdersByPriority(List<DpOrderOffsetDetail> saleOrders) {
        return saleOrders.stream()
            .sorted(getHighPerformanceComparator())
            .collect(Collectors.toList());
    }

    /**
     * 高性能自定义比较器（适用于大数据量）
     */
    private  Comparator<DpOrderOffsetDetail> getHighPerformanceComparator() {
        return new SalesOrderComparator();
    }

    /**
     * 自定义高性能比较器实现
     * 避免重复解析和lambda开销
     */
    private static class SalesOrderComparator implements Comparator<DpOrderOffsetDetail> {

        @Override
        public int compare(DpOrderOffsetDetail o1, DpOrderOffsetDetail o2) {
            // 1. 比较供应链优先级
            int scmPriorityCompare = compareScmPriority(o1, o2);
            if (scmPriorityCompare != 0) {
                return scmPriorityCompare;
            }

            // 2. 比较提报日期
            int dateCompare = compareBillDate(o1, o2);
            if (dateCompare != 0) {
                return dateCompare;
            }

            // 3. 比较提报量
            return compareOrdQty(o1, o2);
        }

        private int compareScmPriority(DpOrderOffsetDetail o1, DpOrderOffsetDetail o2) {
            Integer p1 = parseScmPriority(o1.getScmPriority());
            Integer p2 = parseScmPriority(o2.getScmPriority());

            if (p1 == null && p2 == null) {
                return 0;
            }
            if (p1 == null) {
                return 1; // null排最后
            }
            if (p2 == null) {
                return -1;
            }

            return Integer.compare(p1, p2);
        }

        private int compareBillDate(DpOrderOffsetDetail o1, DpOrderOffsetDetail o2) {
            Date d1 = o1.getBillDate();
            Date d2 = o2.getBillDate();

            if (d1 == null && d2 == null) {
                return 0;
            }
            // null排最后
            if (d1 == null) {
                return 1;
            }
            if (d2 == null) {
                return -1;
            }

            return d1.compareTo(d2);
        }

        private int compareOrdQty(DpOrderOffsetDetail o1, DpOrderOffsetDetail o2) {
            Long q1 = o1.getProducionQty();
            Long q2 = o2.getProducionQty();

            if (q1 == null && q2 == null) {
                return 0;
            }
            // null排最后
            if (q1 == null) {
                return 1;
            }
            if (q2 == null) {
                return -1;
            }
            return q1.compareTo(q2);
        }

        private Integer parseScmPriority(String scmPriority) {
            if (scmPriority == null || scmPriority.trim().isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(scmPriority.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    private DpDemandPlan buildDemandPlanFromAllocation(DpOrderOffsetDetail netDemand) {
        DpDemandPlan demandPlan = new DpDemandPlan();
        BeanUtils.copyProperties(netDemand, demandPlan);
        demandPlan.setNetQty(netDemand.getProducionQty());
        demandPlan.setYearWeek(netDemand.getWeekYear());
        return demandPlan;
    }

    /**
     * 获取最小投产量
     * @return 最小投产量
     */
    private long getMinProductionQty(String factoryCode, String productTypeCode) {
        FactoryParam factoryParam = new FactoryParam();
        factoryParam.setFactoryCode(factoryCode);
        factoryParam.setParamCode(MonthPlanEnums.MIN_PRODUCTION_QTY.getCode());
        factoryParam.setProductTypeCode(productTypeCode);
        FactoryParam param = factoryParamService.getFacParamSingle(factoryParam);
        long paramValue = BigDecimal.ZERO.longValue();
        if (param != null) {
            paramValue = StringUtils.isNotEmpty(param.getParamValue()) ? Long.valueOf(param.getParamValue())
                : Long.valueOf(param.getDefauleValue());
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
        private final Map<String, Long> monthSurplusMap;

        public DataCollection(
            List<SalesOrderPool> salesOrders,
            List<MdmProductStock> finishedProductStocks,
            Map<String, List<MdmProductStock>> finishedProductStockMap,
            Map<String, String> productionTypeMap,
            List<SupplyOrderPool> supplyOrderPools,
            List<SalesOrderPool> allocationOrders,
            List<SalesOrderPool> postponeOrders,
            Map<String, Long> monthSurplusMap) {
            this.salesOrders = salesOrders != null ? salesOrders : Collections.emptyList();
            this.finishedProductStocks = finishedProductStocks != null ? finishedProductStocks : Collections.emptyList();
            this.finishedProductStockMap = finishedProductStockMap != null ? finishedProductStockMap : new HashMap<>();
            this.productionTypeMap = productionTypeMap != null ? productionTypeMap : new HashMap<>();
            this.supplyOrderPools = supplyOrderPools != null ? supplyOrderPools : Collections.emptyList();
            this.allocationOrders = allocationOrders != null ? allocationOrders : Collections.emptyList();
            this.postponeOrders = postponeOrders != null ? postponeOrders : Collections.emptyList();
            this.monthSurplusMap = monthSurplusMap != null ? monthSurplusMap : new HashMap<>();
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
        long totalOrderQty = 0L;
        long totalNetQty = 0L;
        long heightQty = 0L;
        long midQty = 0L;
        long postponeQty = 0L;
        long cycleReserveQty = 0L;
        long conventionReserveQty = 0L;

        void accumulate(DpDemandPlan plan) {
            if (plan == null) {
              return;
            }

            totalOrderQty += plan.getOrderQty();
            totalNetQty += plan.getNetQty();

            // 根据订单优先级累加对应数量
            String priority = plan.getOrderPriority();
            long netQty = plan.getNetQty();

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
        long minProductionQty,
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
        demandPlan.setPlanType(ApsConstant.APS_ZERO_1);
        demandPlan.setIsImport(YesOrNoEnum.NO.getCode());
    }


}
