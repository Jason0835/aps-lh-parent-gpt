package com.zlt.aps.monthplan.demand.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.Constant;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.service.IMdmAreaCapaAllocationService;
import com.zlt.aps.maindata.service.IMdmMaterialInfoService;
import com.zlt.aps.maindata.service.IMdmProductStockService;
import com.zlt.aps.maindata.service.IMdmSkuScheduleCategoryService;
import com.zlt.aps.maindata.service.IMpMonthlySaleQtyService;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.DpOrderOffsetDetail;
import com.zlt.aps.monthplan.api.domain.entity.FactoryParam;
import com.zlt.aps.monthplan.api.domain.entity.FactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MdmAreaCapaAllocation;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmMonthSurplus;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductStock;
import com.zlt.aps.monthplan.api.domain.entity.MpProductionPrediction;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import com.zlt.aps.monthplan.common.utils.MonthCalculator;
import com.zlt.aps.monthplan.common.utils.RequirementVersionService;
import com.zlt.aps.monthplan.demand.service.IDpDemandPlanService;
import com.zlt.aps.monthplan.demand.service.IDpOrderOffsetDetailService;
import com.zlt.aps.monthplan.demand.service.IDpStockVersionService;
import com.zlt.aps.monthplan.demand.service.IMpProductionPredictionService;
import com.zlt.aps.monthplan.demand.service.ISalesOrderPoolService;
import com.zlt.aps.monthplan.factory.helper.SaleRequirePlanHelper;
import com.zlt.aps.monthplan.factory.helper.StockAllocationHelper;
import com.zlt.aps.monthplan.factory.mapper.FactoryProductionVersionMapper;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProductionFinalResultService;
import com.zlt.aps.monthplan.factory.service.IMonthPlanSurplusService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpProductionPredictionServiceImpl.java
 * 描    述：MpProductionPredictionServiceImplS2-1002.未来产量预测业务层处理
 *@author yelq
 *@date 2025-12-28
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
public class MpProductionPredictionServiceImpl extends AbstractDocService<MpProductionPrediction>  implements IMpProductionPredictionService {
    private static final String PREFIX = "PRE";
    private final RequirementVersionService requirementVersionService;

    private final FactoryProductionVersionMapper factoryProductionVersionMapper;
    // 销售订单
    private final ISalesOrderPoolService salesOrderPoolService;
    // 成品库存
    private final IMdmProductStockService mdmProductStockService;
    // 月底计划余量
    private final IMonthPlanSurplusService monthPlanSurplusService;
    // 订单分配表
    private final IDpOrderOffsetDetailService dpOrderOffsetDetailService;
    // 版本库存
    private final IDpStockVersionService dpStockVersionService;
    // 需求计划
    private final IDpDemandPlanService dpDemandPlanService;
    // 定稿的月度排产计划
    private final IFactoryMonthPlanProductionFinalResultService factoryMonthPlanProductionFinalResultService;
    // 区域产能分配
    private final IMdmAreaCapaAllocationService mdmAreaCapaAllocationService;
    // 排产设定
    private final IFactoryParamService factoryParamService;
    // 物料信息
    private final IMdmMaterialInfoService materialInfoService;
    // SKU排产分类
    private final IMdmSkuScheduleCategoryService mdmSkuScheduleCategoryService;
    // 月均销量
    private final IMpMonthlySaleQtyService monthlySaleQtyService;

    @Override
    protected String getDocTypeCode() {
        return "2025122822";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("2025122822");
        return sysDocType;
    }

    @Override
    public String checkUnique(MpProductionPrediction docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mpProductionPrediction.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public AjaxResult createMonthPrediction(MpProductionPrediction createCondition) {
        // 2、得到T月、T+1月、T+2月。T月 = 当前操作日所在年月(当月) +1 ；T+1月 = 在T月的基础上+1个月；T+2月 = 在T月的基础上+2个月
        MonthCalculator.MonthRangeResult monthRangeResult = MonthCalculator.calculateMonthRanges();
        // 3、检查是否已有T月月度计划(定稿)
        //   (1) 若 不存在T月月度计划，则提示"T月月度生产计划还未定稿，请先生成及定稿！"，系统不做任何处理。
        List<FactoryProductionVersion> finalVersions =  validateProductionVersionFinalized(monthRangeResult.getTMonth());
        if (CollectionUtils.isEmpty(finalVersions)) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.productionPrediction.checkFinal"));
        }
        FactoryProductionVersion finalVersion =  finalVersions.get(0);
        createCondition.setMonthPlanVersion(finalVersion.getMonthPlanVersion());
        // 4、生成预测版本号(PRE+yyyymmdd+3位流水号)
        String predictionVersion = requirementVersionService.generateVersion(PREFIX);
        createCondition.setPredictionVersion(predictionVersion);
        // 5. 并行获取数据
        DataCollection data = fetchRequiredDataInParallel();
        // 6. 处理销售订单分配
        OrderAllocationResult allocationResult = processSalesOrderAllocation(
            predictionVersion, data.getSalesOrders(), data.getFinishedProductStockMap(),
            data.getMonthSurplusMap());
        //  (1) 得到对冲后的销售订单净需求数据(包含暂缓订单+高优先级+中优先级的净需求)
        // 7. 处理需求计划生成
        List<DpDemandPlan> demandPlans = generateDemandPlans(
            createCondition, allocationResult.getNetDemands(), data);
        // 8. 合并并保存需求计划
        if (CollectionUtils.isNotEmpty(demandPlans)) {
            // saveDemandPlans(predictionVersion,monthRangeResult.getTPlus1Month(), demandPlans, data);
        }
        //  (2) 同时，保存预测版本号T月的订单分配结果
        saveAllocationResults(createCondition,allocationResult);

        return null;
    }

    private void saveTPlus1MonthDemandPlans(String predictionVersion,YearMonth yearMonth, List<DpDemandPlan> demandPlans, DataCollection data) {
        // 获取最小投产量
        long minProductionQty = getMinProductionQty(FactoryConstant.DEFAULT_FACTORY_CODE, ProductTypeEnum.WHOLE_STEEL.getValue());
        // 获取SKU映射
        Map<String, MdmMaterialInfo> skuMap = materialInfoService.skuToMaterialInfo();

        // 合并需求计划
        /*List<DpDemandPlan> mergedPlans = mergedDemandPlan(predictionVersion,
            demandPlans, minProductionQty, skuMap,
            data.getFinishedProductStockMap(), data.getMonthSurplusMap(),data.getProductionTypeMap(),data.getMonthlySaleQty());
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(mergedPlans)) {
            this.baseDao.insertBatch(mergedPlans);
        }*/
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

    private List<DpDemandPlan> generateDemandPlans(MpProductionPrediction createCondition, List<DpOrderOffsetDetail> netDemands, DataCollection data) {
        List<DpDemandPlan> demandPlans = new ArrayList<>();
        // 处理净需求
        if (CollectionUtils.isNotEmpty(netDemands)) {
            demandPlans.addAll(processNetDemands(createCondition, netDemands));
        }
        return demandPlans;
    }

    private List<DpDemandPlan> processNetDemands(MpProductionPrediction createCondition, List<DpOrderOffsetDetail> netDemands) {
        List<MdmAreaCapaAllocation> areaCapaAllocations =
            mdmAreaCapaAllocationService.findAreaCapaAllocation(createCondition.getYear(),createCondition.getMonth());

        if (org.apache.commons.collections.CollectionUtils.isEmpty(areaCapaAllocations)) {
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

    private DpDemandPlan buildDemandPlanFromAllocation(DpOrderOffsetDetail netDemand) {
        DpDemandPlan demandPlan = new DpDemandPlan();
        BeanUtils.copyProperties(netDemand, demandPlan);
        demandPlan.setNetQty(BigDecimal.valueOf(netDemand.getProducionQty()));
        demandPlan.setYearWeek(netDemand.getWeekYear());
        return demandPlan;
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

            if (org.apache.commons.collections.CollectionUtils.isEmpty(areaCapacities)) {
                result.addAll(transformAllocationsToDemandPlans(sortedOrders));
                return;
            }

            // 计算总产能和总需求
            long totalCapacity = areaCapacities.stream().filter(item -> null != item.getCapacityAllocation())
                .mapToLong(item -> item.getCapacityAllocation().longValue())
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
        MpProductionPrediction createCondition,
        OrderAllocationResult allocationResult) {
        // 批量插入分配结果
        if (CollectionUtils.isNotEmpty(allocationResult.getAllocations())) {
            this.dpOrderOffsetDetailService.insertBatchData(allocationResult.getAllocations());
        }
        // 批量插入库存版本
        dpStockVersionService.insertBatchData(createCondition,allocationResult.getStockMap());
    }

    /**
     * 并行获取所有必要数
     */
    private DataCollection fetchRequiredDataInParallel() {
        CompletableFuture<List<SalesOrderPool>> salesOrdersFuture =
            CompletableFuture.supplyAsync(this::fetchSalesOrderPool);
        CompletableFuture<List<MdmProductStock>> stocksFuture =
            CompletableFuture.supplyAsync(this::fetchFinishedProductStocks);
        CompletableFuture<Map<String, Long>> monthSurplusFuture =
            CompletableFuture.supplyAsync(this::fetchMonthSurplusMap);
        CompletableFuture<Map<String, String>> productionTypeFuture =
            CompletableFuture.supplyAsync(this::fetchProductionTypeMap);
        CompletableFuture<Map<String, Long>> monthlySaleQtyFuture =
            CompletableFuture.supplyAsync(this::findMonthlySaleQtyGroupByMaterialCode);
        // 等待所有任务完成
        CompletableFuture.allOf(
            salesOrdersFuture, stocksFuture, monthSurplusFuture,productionTypeFuture,monthlySaleQtyFuture
        ).join();

        try {
            List<SalesOrderPool> salesOrders = salesOrdersFuture.get();
            List<MdmProductStock> finishedProductStocks = stocksFuture.get();
            Map<String, Long> monthSurplusMap = monthSurplusFuture.get();
            Map<String, String> productionTypeMap = productionTypeFuture.get();
            Map<String, Long>  monthlySaleQty = monthlySaleQtyFuture.get();
            // 处理成品库存映射
            Map<String, List<MdmProductStock>> finishedProductStockMap =
                CollectionUtils.isEmpty(finishedProductStocks) ?
                    new HashMap<>(16) :
                    finishedProductStocks.stream()
                        .collect(Collectors.groupingBy(MdmProductStock::getGroupKey));

            return new DataCollection(
                salesOrders,
                finishedProductStocks,
                finishedProductStockMap,
                monthSurplusMap,
                productionTypeMap,
                monthlySaleQty
            );

        } catch (Exception e) {
            log.error("并行获取数据失败", e);
            throw new BusinessException("获取数据失败");
        }
    }

    private Map<String, Long> findMonthlySaleQtyGroupByMaterialCode() {
        return monthlySaleQtyService.findMonthlySaleQtyGroupByMaterialCode();
    }

    /**
     * 获取排产类型
     */
    private Map<String, String> fetchProductionTypeMap() {
        return mdmSkuScheduleCategoryService.skuToProductionType();
    }

    /**
     *  获取T-1月新的月底计划余量(如果库存日期 > T-1月，则月底计划余量 = 0)；
     * @return
     */
    private Map<String, Long> fetchMonthSurplusMap() {
        List<MdmMonthSurplus> mdmMonthSurpluses = monthPlanSurplusService.findCurrentMonthPlanSurplus();
        if(CollectionUtils.isEmpty(mdmMonthSurpluses)){
            return Collections.emptyMap();
        }
        return mdmMonthSurpluses.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(
                MdmMonthSurplus::getGroupKey,
                Collectors.summingLong(MdmMonthSurplus::getPlanSurplusQty)
            ));
    }

    /**
     *  从成品库存表中获取库存
     * @return 成品库存
     */
    private List<MdmProductStock> fetchFinishedProductStocks() {
        return mdmProductStockService.findCurrentFinishStock();
    }

    /**
     *  5、查询截止预测日，在销售订单池中的所有订单；
     * @return 销售订单
     */
    private List<SalesOrderPool> fetchSalesOrderPool() {
        return salesOrderPoolService.findCurrentSalesOrderPool();
    }

    /**
     *   3、检查是否已有T月月度计划(定稿)
     *       (1) 若 不存在T月月度计划，则提示"T月月度生产计划还未定稿，请先生成及定稿！"，系统不做任何处理。
     * @param tMonth T月
     */
    private List<FactoryProductionVersion> validateProductionVersionFinalized(YearMonth tMonth) {
        return factoryProductionVersionMapper.selectList(
            Wrappers.<FactoryProductionVersion>lambdaQuery()
                .eq(FactoryProductionVersion::getFactoryCode, FactoryConstant.DEFAULT_FACTORY_CODE)
                .eq(FactoryProductionVersion::getYear, tMonth.getYear())
                .eq(FactoryProductionVersion::getMonth, tMonth.getMonthValue())
                .eq(FactoryProductionVersion::getIsFinal, Constant.TRUE)
        );
    }

    /**
     * 数据集合
     */
    @Getter
    private static class DataCollection {
        private final List<SalesOrderPool> salesOrders;
        private final List<MdmProductStock> finishedProductStocks;
        private final Map<String, List<MdmProductStock>> finishedProductStockMap;
        private final Map<String, Long> monthSurplusMap;
        private final Map<String, String> productionTypeMap;
        private final Map<String, Long>  monthlySaleQty;

        public DataCollection(
            List<SalesOrderPool> salesOrders,
            List<MdmProductStock> finishedProductStocks,
            Map<String, List<MdmProductStock>> finishedProductStockMap,
            Map<String, Long> monthSurplusMap,
            Map<String, String> productionTypeMap,
            Map<String, Long>  monthlySaleQty) {
            this.salesOrders = CollectionUtils.isNotEmpty(salesOrders)? salesOrders : Collections.emptyList();
            this.finishedProductStocks = finishedProductStocks != null ? finishedProductStocks : Collections.emptyList();
            this.finishedProductStockMap = finishedProductStockMap != null ? finishedProductStockMap : new HashMap<>();
            this.monthSurplusMap = monthSurplusMap != null ? monthSurplusMap : new HashMap<>();
            this.productionTypeMap = productionTypeMap != null ? productionTypeMap : new HashMap<>();
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
}
