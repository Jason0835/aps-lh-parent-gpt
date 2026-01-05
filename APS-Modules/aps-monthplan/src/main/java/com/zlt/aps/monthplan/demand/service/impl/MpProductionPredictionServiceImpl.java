package com.zlt.aps.monthplan.demand.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.ProductionPlanType;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.exception.BusinessException;
import com.tlt.aps.utils.BeanCopyUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.service.*;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.common.utils.MonthCalculator;
import com.zlt.aps.monthplan.common.utils.RequirementVersionService;
import com.zlt.aps.monthplan.demand.service.*;
import com.zlt.aps.monthplan.factory.helper.SaleRequirePlanHelper;
import com.zlt.aps.monthplan.factory.helper.StockAllocationHelper;
import com.zlt.aps.monthplan.factory.mapper.MpFactoryProductionVersionMapper;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProductionFinalResultService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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

    private final MpFactoryProductionVersionMapper factoryProductionVersionMapper;
    // 销售订单
    private final ISalesOrderPoolService salesOrderPoolService;
    // 成品库存
    private final IMdmProductStockService mdmProductStockService;
    // 版本库存
    private final IDpStockVersionService dpStockVersionService;
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
    // 供应链订单
    private final ISupplyOrderPoolService supplyOrderPoolService;
    // 订单快照
    private final IDpOrderPoolSnapshotService dpOrderPoolSnapshotService;
    // 月均销量
    private final IMpMonthlySaleQtyService mpMonthlySaleQtyService;



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
        List<MpFactoryProductionVersion> finalVersions =  validateProductionVersionFinalized(monthRangeResult.getTMonth());
        if (CollectionUtils.isEmpty(finalVersions)) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.productionPrediction.checkFinal"));
        }
        MpFactoryProductionVersion finalVersion =  finalVersions.get(0);
        Map<String, Integer>  tMonthSaleQty =   this.mpMonthlySaleQtyService.findMonthlySaleQtyGroupByMaterialCode();
        // 生成T月模拟需求计划
        // T月需求要生成,订单-库存冲减-月底计划余量(T-1月)+T月（快照周期+常规)
        // T+1月需求生成：T月需求-T月已排+T+1（周期+常规）
        // 对冲规则：供应链优先级+提报日期逐笔扣除(先冲实单)
        List<DpDemandPlan> tMonthDemands = createDemandPlan(monthRangeResult.getTMonth(),tMonthSaleQty,finalVersion);
        // 剩余需求量
        List<DpDemandPlan> leftDemands = calculateLeftDemand(tMonthDemands,finalVersion);
        List<DpDemandPlan> tPlus1MonthDemands = createDemandPlan(leftDemands,monthRangeResult.getTPlus1Month(),tMonthSaleQty);
        MpFactoryProductionVersion finalVersionByTplus1Month = createProductionVersion(tPlus1MonthDemands);
        // 剩余需求量
        List<DpDemandPlan> leftDemandsByTplus1Month = calculateLeftDemand(tPlus1MonthDemands,finalVersionByTplus1Month);
        List<DpDemandPlan> tPlus2MonthDemands = createDemandPlan(leftDemandsByTplus1Month,monthRangeResult.getTPlus2Month(),tMonthSaleQty);
        Map<String, MdmMaterialInfo> materialInfoMap = fetchMaterialInfo();
        List<MpProductionPrediction> list = buildProductionPrediction(finalVersion,tPlus1MonthDemands,tPlus2MonthDemands,materialInfoMap);
        if(CollectionUtils.isNotEmpty(list)) {
            this.baseDao.insertBatch(list);
        }
        return AjaxResult.success();
    }

    private MpFactoryProductionVersion createProductionVersion(List<DpDemandPlan> tPlus1MonthDemands) {
        if(CollectionUtils.isEmpty(tPlus1MonthDemands)) {
            return null;
        }
        MpFactoryProductionVersion productionVersion = new MpFactoryProductionVersion();
        productionVersion.setFactoryCode(tPlus1MonthDemands.get(0).getFactoryCode());
        productionVersion.setYear(tPlus1MonthDemands.get(0).getYear());
        productionVersion.setMonth(tPlus1MonthDemands.get(0).getMonth());
        productionVersion.setMonthPlanVersion(tPlus1MonthDemands.get(0).getMonthPlanVersion());
        return productionVersion;
    }

    private List<DpDemandPlan> createDemandPlan(List<DpDemandPlan> leftDemands, YearMonth yearMonth, Map<String, Integer> monthSaleQty) {
        if(CollectionUtils.isEmpty(leftDemands)) {
            return Collections.emptyList();
        }
        List<DpDemandPlan> netDemands =  leftDemands.stream().filter(item -> item.getUnPostponeNetQty() > 0).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(netDemands)) {
            return Collections.emptyList();
        }
        // 1、生成预测版本号(PRE+yyyymmdd+3位流水号)
        String predictionVersion = requirementVersionService.generateVersion(PREFIX);
        // 2. 并行获取数据
        DataCollection data = fetchRequiredDataInParallel(predictionVersion,yearMonth);

        // 6. 处理需求计划生成
        List<DpDemandPlan> demandPlans = generatePlans(predictionVersion,yearMonth, netDemands, data);
        List<DpDemandPlan> mergedDemandPlans = Lists.newArrayList();
        // 7. 合并并保存需求计划
        if (CollectionUtils.isNotEmpty(demandPlans)) {
            mergedDemandPlans = saveDemandPlans(demandPlans, data,monthSaleQty);
        }
        // 8. 保存分厂排产版本
        saveFactoryProductionVersion(yearMonth,predictionVersion);
        return mergedDemandPlans;
    }

    private List<DpDemandPlan> generatePlans(String predictionVersion, YearMonth yearMonth, List<DpDemandPlan> netDemands, DataCollection data) {
        List<DpDemandPlan> demandPlans = new ArrayList<>();
        // 处理净需求
        if (CollectionUtils.isNotEmpty(netDemands)) {
            demandPlans.addAll(netDemands);
        }
        // 处理供应链订单
        if (CollectionUtils.isNotEmpty(data.getSupplyOrders())) {
            demandPlans.addAll(transformSupplyOrdersToDemandPlans(data.getSupplyOrders(), predictionVersion, yearMonth));
        }
        return demandPlans;
    }

    private DataCollection fetchRequiredDataInParallel(String predictionVersion, YearMonth yearMonth) {
        CompletableFuture<List<MdmProductStock>> stocksFuture =
            CompletableFuture.supplyAsync(this::fetchFinishedProductStocks);
        CompletableFuture<Map<String, String>> productionTypeFuture =
            CompletableFuture.supplyAsync(this::fetchProductionTypeMap);
        CompletableFuture<Integer> minProductionQtyFuture =
            CompletableFuture.supplyAsync(this::getMinProductionQty);
        CompletableFuture<Map<String, MdmMaterialInfo>> fetchMaterialInfoFuture =
            CompletableFuture.supplyAsync(this::fetchMaterialInfo);

        // 等待所有任务完成
        CompletableFuture.allOf(stocksFuture,productionTypeFuture,minProductionQtyFuture,fetchMaterialInfoFuture
        ).join();
        try {
            List<MdmProductStock> finishedProductStocks = stocksFuture.get();
            Map<String, String> productionTypeMap = productionTypeFuture.get();
            int minProductionQty = minProductionQtyFuture.get();
            Map<String, MdmMaterialInfo> materialInfoMap = fetchMaterialInfoFuture.get();
            List<SupplyOrderPool> supplyOrders = this.createSupplyOrder(predictionVersion,yearMonth);
            if(CollectionUtils.isNotEmpty(finishedProductStocks)){
                finishedProductStocks.forEach(finishedProductStock -> finishedProductStock.setLeftOverQty(null == finishedProductStock.getStockQty()?BigDecimal.ZERO.intValue():finishedProductStock.getStockQty()));
            }
            // 处理成品库存映射
            Map<String, List<MdmProductStock>> finishedProductStockMap =
                CollectionUtils.isEmpty(finishedProductStocks) ?
                    new HashMap<>(16) :
                    finishedProductStocks.stream()
                        .collect(Collectors.groupingBy(MdmProductStock::getGroupKey));
            Map<String, Integer> monthSurplusMap = this.factoryMonthPlanProductionFinalResultService.calculateMonthSurplus(predictionVersion,finishedProductStocks);
            return new DataCollection(
                null,
                supplyOrders,
                finishedProductStocks,
                finishedProductStockMap,
                monthSurplusMap,
                productionTypeMap,
                minProductionQty,
                materialInfoMap
            );

        } catch (Exception e) {
            log.error("并行获取数据失败", e);
            throw new BusinessException("获取数据失败");
        }
    }

    private List<SupplyOrderPool> createSupplyOrder(String predictionVersion, YearMonth yearMonth) {
        // 8、按【生成周期排产】、【生成储备排产】的逻辑得到T+1月的周期排产储备和常规储备数据(此时T月的月度计划已有，故而结构最新排产月份会有变化)
        // 13、按【生成周期排产】、【生成储备排产】的逻辑得到T+2月的周期排产储备和常规储备数据(此时T+1月的月度计划已预测，故而结构最新排产月份会有变化)
        // 生成周期排产储备
        List<SupplyOrderPool> cycleStockUpOrders =  supplyOrderPoolService.createCycleStockUp(yearMonth);
        // 生成常规储备
        List<SupplyOrderPool>  precedentStockUpOrders =  supplyOrderPoolService.createPrecedentStockUp(yearMonth);
        List<SupplyOrderPool> allStockUpOrders = Lists.newArrayList();
        if(CollectionUtils.isNotEmpty(cycleStockUpOrders)){
            allStockUpOrders.addAll(cycleStockUpOrders);
        }
        if(CollectionUtils.isNotEmpty(precedentStockUpOrders)){
            allStockUpOrders.addAll(precedentStockUpOrders);
        }
        // (1) 生成的T+1月的周期排产储备和常规储备数据，保存录入(预测版本号)订单池快照表
        saveOrderPoolSnapshot(predictionVersion,yearMonth,allStockUpOrders);
        return allStockUpOrders;
    }

    private List<DpDemandPlan> calculateLeftDemand(List<DpDemandPlan> tMonthDemands, MpFactoryProductionVersion finalVersion) {
        if(CollectionUtils.isEmpty(tMonthDemands)  || null == finalVersion) {
            return Collections.emptyList();
        }
        Map<String, List<FactoryMonthPlanProductionFinalResult>> productionFinalResults  = calculateMonthDemandQty(finalVersion);
        if(org.springframework.util.CollectionUtils.isEmpty(productionFinalResults)) {
            return tMonthDemands;
        }
        // 获取排序配置并排序订单
        List<DpDemandPlan> sortedDemandPlans = getSortedDemandPlans(tMonthDemands);
        sortedDemandPlans.forEach(plan -> calculdateDemandQty(plan,productionFinalResults));
        return sortedDemandPlans;
    }

    private void calculdateDemandQty(DpDemandPlan plan, Map<String, List<FactoryMonthPlanProductionFinalResult>> productionFinalResults) {
        List<FactoryMonthPlanProductionFinalResult> list = productionFinalResults.get(plan.getMaterialCode());
        if(CollectionUtils.isEmpty(list)) {
            return;
        }
        int heightQty =  null == plan.getHeightQty()?BigDecimal.ZERO.intValue():plan.getHeightQty();
        int leftHeightQty = calculateHeightQty(heightQty,list);

        int midQty = null == plan.getMidQty()?BigDecimal.ZERO.intValue():plan.getMidQty();
        int leftMidQty = calculateMidQty(midQty,list);

        int postponeQty = null == plan.getPostponeQty()?BigDecimal.ZERO.intValue():plan.getPostponeQty();
        int leftPostponeQty = calculatePostponeQty(postponeQty,list);

        int cycleReserveQty = null == plan.getCycleReserveQty()?BigDecimal.ZERO.intValue():plan.getCycleReserveQty();
        int leftCycleReserveQty = calculateCycleReserveQty(cycleReserveQty,list);

        int conventionReserveQty = null == plan.getConventionReserveQty()?BigDecimal.ZERO.intValue():plan.getConventionReserveQty();
        int leftConventionReserveQty = calculateConventionReserveQty(conventionReserveQty,list);

        int netQty =  null == plan.getNetQty()?BigDecimal.ZERO.intValue():plan.getNetQty();
        int leftNetQty = calculateNetQty(netQty,list);
        plan.setHeightQty(leftHeightQty);
        plan.setMidQty(leftMidQty);
        plan.setPostponeQty(leftPostponeQty);
        plan.setCycleReserveQty(leftCycleReserveQty);
        plan.setConventionReserveQty(leftConventionReserveQty);
        plan.setNetQty(leftNetQty);
        // (8)净需求(含暂缓) = 高优先级净需求量 + 中优先级净需求量+暂缓订单需求量
        plan.setPostponeQty(leftHeightQty + leftMidQty + leftPostponeQty);
        // (9)净需求(不含暂缓) = 高优先级净需求量 + 中优先级净需求量
        plan.setUnPostponeNetQty(leftHeightQty + leftMidQty);
    }

    private int calculateNetQty(int netQty, List<FactoryMonthPlanProductionFinalResult> list) {
        if (netQty  <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        BigDecimal remainingQty = BigDecimal.valueOf(netQty);
        for (FactoryMonthPlanProductionFinalResult stock : list) {
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            // 获取当前库存数量
            BigDecimal stockQty = stock.getTotalQty() == null?BigDecimal.ZERO:BigDecimal.valueOf(stock.getTotalQty());
            if(stockQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (stockQty.compareTo(remainingQty) >= 0) {
                // 当前库存足够冲减
                stock.setTotalQty(stockQty.subtract(remainingQty).intValue());
                remainingQty = BigDecimal.ZERO;
            } else {
                // 当前库存不足，全部冲减
                stock.setTotalQty(0);
                remainingQty = remainingQty.subtract(stockQty);
            }
        }
        return remainingQty.intValue();
    }

    private int calculateHeightQty(int heightQty, List<FactoryMonthPlanProductionFinalResult> list) {
        if (heightQty  <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        BigDecimal remainingQty = BigDecimal.valueOf(heightQty);
        for (FactoryMonthPlanProductionFinalResult stock : list) {
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            // 获取当前库存数量
            BigDecimal stockQty = stock.getHeightProductionQty() == null?BigDecimal.ZERO:BigDecimal.valueOf(stock.getHeightProductionQty());
            if(stockQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (stockQty.compareTo(remainingQty) >= 0) {
                // 当前库存足够冲减
                stock.setHeightProductionQty(stockQty.subtract(remainingQty).intValue());
                remainingQty = BigDecimal.ZERO;
            } else {
                // 当前库存不足，全部冲减
                stock.setHeightProductionQty(0);
                remainingQty = remainingQty.subtract(stockQty);
            }
        }
        return remainingQty.intValue();
    }

    private int calculateMidQty(int midQty, List<FactoryMonthPlanProductionFinalResult> list) {
        if (midQty  <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        BigDecimal remainingQty = BigDecimal.valueOf(midQty);
        for (FactoryMonthPlanProductionFinalResult stock : list) {
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            // 获取当前库存数量
            BigDecimal stockQty = stock.getMidProductionQty() == null?BigDecimal.ZERO:BigDecimal.valueOf(stock.getMidProductionQty());
            if(stockQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (stockQty.compareTo(remainingQty) >= 0) {
                // 当前库存足够冲减
                stock.setMidProductionQty(stockQty.subtract(remainingQty).intValue());
                remainingQty = BigDecimal.ZERO;
            } else {
                // 当前库存不足，全部冲减
                stock.setMidProductionQty(0);
                remainingQty = remainingQty.subtract(stockQty);
            }
        }
        return remainingQty.intValue();
    }

    private int calculatePostponeQty(int postponeQty, List<FactoryMonthPlanProductionFinalResult> list) {
        if (postponeQty  <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        BigDecimal remainingQty = BigDecimal.valueOf(postponeQty);
        for (FactoryMonthPlanProductionFinalResult stock : list) {
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            // 获取当前库存数量
            BigDecimal stockQty = stock.getPostponeProductionQty() == null?BigDecimal.ZERO:BigDecimal.valueOf(stock.getPostponeProductionQty());
            if(stockQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (stockQty.compareTo(remainingQty) >= 0) {
                // 当前库存足够冲减
                stock.setPostponeProductionQty(stockQty.subtract(remainingQty).intValue());
                remainingQty = BigDecimal.ZERO;
            } else {
                // 当前库存不足，全部冲减
                stock.setPostponeProductionQty(0);
                remainingQty = remainingQty.subtract(stockQty);
            }
        }
        return remainingQty.intValue();
    }

    private int calculateCycleReserveQty(int cycleReserveQty, List<FactoryMonthPlanProductionFinalResult> list) {
        if (cycleReserveQty  <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        BigDecimal remainingQty = BigDecimal.valueOf(cycleReserveQty);
        for (FactoryMonthPlanProductionFinalResult stock : list) {
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            // 获取当前库存数量
            BigDecimal stockQty = stock.getCycleProductionQty() == null?BigDecimal.ZERO:BigDecimal.valueOf(stock.getCycleProductionQty());
            if(stockQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (stockQty.compareTo(remainingQty) >= 0) {
                // 当前库存足够冲减
                stock.setCycleProductionQty(stockQty.subtract(remainingQty).intValue());
                remainingQty = BigDecimal.ZERO;
            } else {
                // 当前库存不足，全部冲减
                stock.setCycleProductionQty(0);
                remainingQty = remainingQty.subtract(stockQty);
            }
        }
        return remainingQty.intValue();
    }

    private int calculateConventionReserveQty(int conventionReserveQty, List<FactoryMonthPlanProductionFinalResult> list) {
        if (conventionReserveQty  <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        BigDecimal remainingQty = BigDecimal.valueOf(conventionReserveQty);
        for (FactoryMonthPlanProductionFinalResult stock : list) {
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            // 获取当前库存数量
            BigDecimal stockQty = stock.getConventionProductionQty() == null?BigDecimal.ZERO:BigDecimal.valueOf(stock.getConventionProductionQty());
            if(stockQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (stockQty.compareTo(remainingQty) >= 0) {
                // 当前库存足够冲减
                stock.setConventionProductionQty(stockQty.subtract(remainingQty).intValue());
                remainingQty = BigDecimal.ZERO;
            } else {
                // 当前库存不足，全部冲减
                stock.setConventionProductionQty(0);
                remainingQty = remainingQty.subtract(stockQty);
            }
        }
        return remainingQty.intValue();
    }

    private List<DpDemandPlan> getSortedDemandPlans(List<DpDemandPlan> tMonthDemands) {
        return tMonthDemands.stream()
            .sorted(getHighPerformanceComparator())
            .collect(Collectors.toList());
    }

    /**
     * 高性能自定义比较器（适用于大数据量）
     */
    private static Comparator<DpDemandPlan> getHighPerformanceComparator() {
        return new DemandPlanComparator();
    }


    private List<MpProductionPrediction> buildProductionPrediction(MpFactoryProductionVersion finalVersion,List<DpDemandPlan> tPlus1MonthDemands, List<DpDemandPlan> tPlus2MonthDemands, Map<String, MdmMaterialInfo> materialInfoMap) {
        List<FactoryMonthPlanProductionFinalResult> productionFinalResults =   this.factoryMonthPlanProductionFinalResultService.findProductionFinalResult(finalVersion);
        if(CollectionUtils.isEmpty(productionFinalResults)) {
            return Collections.emptyList();
        }
        Map<String,Integer> tMonthDemandQty = this.getMonthQty(productionFinalResults);
        MpFactoryProductionVersion productionVersionByTplus1Month = this.createProductionVersion(tPlus1MonthDemands);
        MpFactoryProductionVersion productionVersionByTplus2Month = this.createProductionVersion(tPlus2MonthDemands);
        List<FactoryMonthPlanProductionFinalResult> productionFinalResultsByTplus1Month =   this.factoryMonthPlanProductionFinalResultService.findProductionFinalResult(productionVersionByTplus1Month);
        List<FactoryMonthPlanProductionFinalResult> productionFinalResultsTplus2Month =   this.factoryMonthPlanProductionFinalResultService.findProductionFinalResult(productionVersionByTplus2Month);
        Map<String,Integer> tPlus1MonthDemandQty = this.getMonthQty(productionFinalResultsByTplus1Month);
        Map<String,Integer> tPlus2MonthDemandQty = this.getMonthQty(productionFinalResultsTplus2Month);
        List<MpProductionPrediction> list = Lists.newArrayList();
        YearMonth yearMonth = YearMonth.now();
        tMonthDemandQty.forEach((materialCode, productionQty) -> {
                if(!materialInfoMap.containsKey(materialCode)) {
                    return;
                }
                MdmMaterialInfo materialInfo = materialInfoMap.get(materialCode);
                MpProductionPrediction productionPrediction = new MpProductionPrediction();
                BeanUtils.copyProperties(materialInfo,productionPrediction);
                productionPrediction.setId(null);
                productionPrediction.setBaseVale(null);
                productionPrediction.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
                productionPrediction.setYear(yearMonth.getYear());
                productionPrediction.setMonth(yearMonth.getMonthValue());
                productionPrediction.setLocationType(materialInfo.getCommonType());
                productionPrediction.setMonthPlanVersion(finalVersion.getMonthPlanVersion());
                productionPrediction.setProductionVersion(finalVersion.getProductionVersion());
                productionPrediction.setMonth1(productionQty);
                productionPrediction.setMonth2(tPlus1MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.intValue()));
                productionPrediction.setMonth3(tPlus2MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.intValue()));
                list.add(productionPrediction);
        });
        return list;
    }

    private Map<String, Integer> getMonthQty(List<FactoryMonthPlanProductionFinalResult> productionFinalResults) {
        if (CollectionUtils.isEmpty(productionFinalResults)) {
            return Collections.emptyMap();
        }
        return productionFinalResults.stream()
            .filter(Objects::nonNull)
            .filter(productionFinalResult -> StringUtils.isNotBlank(productionFinalResult.getMaterialCode()) && productionFinalResult.getTotalQty() != null)
            .collect(Collectors.groupingBy(
                FactoryMonthPlanProductionFinalResult::getMaterialCode,
                Collectors.summingInt(FactoryMonthPlanProductionFinalResult::getTotalQty)
            ));
    }

    private Map<String, List<FactoryMonthPlanProductionFinalResult>> calculateMonthDemandQty(MpFactoryProductionVersion finalVersion) {
        List<FactoryMonthPlanProductionFinalResult> productionFinalResults = factoryMonthPlanProductionFinalResultService.findProductionFinalResult(finalVersion);
        if(CollectionUtils.isEmpty(productionFinalResults)) {
            return Collections.emptyMap();
        }
        return productionFinalResults.stream()
            .filter(Objects::nonNull)
            .filter(productionFinalResult -> StringUtils.isNotBlank(productionFinalResult.getMaterialCode()))
            .collect(Collectors.groupingBy(FactoryMonthPlanProductionFinalResult::getMaterialCode));
    }

    private List<DpDemandPlan> createDemandPlan(YearMonth yearMonth,Map<String, Integer>  monthSaleQty,MpFactoryProductionVersion finalVersion) {
        // 1、生成预测版本号(PRE+yyyymmdd+3位流水号)
        String predictionVersion = requirementVersionService.generateVersion(PREFIX);
        // 2. 并行获取数据
        DataCollection data = fetchRequiredDataInParallel(predictionVersion,finalVersion);
        // 3. 处理销售订单分配
        OrderAllocationResult allocationResult = processSalesOrderAllocation(
            predictionVersion,yearMonth,data.getSalesOrders(), data.getFinishedProductStockMap(),
            data.getMonthSurplusMap());

        //  (2) 同时，保存预测版本号T月的订单分配结果
        saveAllocationResults(predictionVersion,yearMonth,allocationResult);
        // 6. 处理需求计划生成
        List<DpDemandPlan> demandPlans = generateDemandPlans(predictionVersion,yearMonth, allocationResult.getNetDemands(), data);
        List<DpDemandPlan> mergedDemandPlans = Lists.newArrayList();
        // 7. 合并并保存需求计划
        if (CollectionUtils.isNotEmpty(demandPlans)) {
            mergedDemandPlans = saveDemandPlans(demandPlans, data,monthSaleQty);
        }
        // 8. 保存分厂排产版本
        saveFactoryProductionVersion(yearMonth,predictionVersion);
        return mergedDemandPlans;
    }

    private List<DpDemandPlan> saveDemandPlans(List<DpDemandPlan> demandPlans, DataCollection data,Map<String, Integer>  monthSaleQty) {
        // 获取最小投产量
        int minProductionQty = getMinProductionQty();
        // 获取SKU映射
        Map<String, MdmMaterialInfo> skuMap = materialInfoService.skuToMaterialInfo();
        // 合并需求计划
        List<DpDemandPlan> mergedPlans = mergedDemandPlan(
            demandPlans, minProductionQty, skuMap,
            data.getFinishedProductStockMap(), data.getMonthSurplusMap(),data.getProductionTypeMap(),monthSaleQty);

        if (CollectionUtils.isNotEmpty(mergedPlans)) {
            this.baseDao.insertBatch(mergedPlans);
        }
        return mergedPlans;
    }

    private List<DpDemandPlan> mergedDemandPlan(List<DpDemandPlan> demandPlans,int minProductionQty,Map<String, MdmMaterialInfo> skuMap,Map<String,List<MdmProductStock>> finishedProductStockMap,Map<String,Integer> mdmMonthSurplusMap,Map<String, String> productionTypeMap,Map<String, Integer> monthlySaleQty) {
        // 快速失败：空集合直接返回
        if (CollectionUtils.isEmpty(demandPlans)) {
            return Collections.emptyList();
        }
        List<DpDemandPlan> list = Lists.newArrayList();
        Set<String>  groupKeys = demandPlans.stream().map(DpDemandPlan::getGroupKey).collect(Collectors.toSet());
        groupKeys.forEach(groupKey -> {
            List<DpDemandPlan> groupPlans = demandPlans.stream().filter(demandPlan -> groupKey.equals(demandPlan.getGroupKey())).collect(Collectors.toList());
            // 获取基础模板（第一个元素）
            DpDemandPlan template = groupPlans.get(0);
            if(!skuMap.containsKey(template.getMaterialCode())) {
                return;
            }
            list.add(buildMergedDemandPlan(
                groupPlans,
                minProductionQty,
                skuMap,
                finishedProductStockMap,
                mdmMonthSurplusMap,
                productionTypeMap,monthlySaleQty));
        });
        log.info("groupKeys:{}",groupKeys);
        return list;
    }

    private DpDemandPlan buildMergedDemandPlan(
        List<DpDemandPlan> groupPlans,
        int minProductionQty,
        Map<String, MdmMaterialInfo> skuMap,
        Map<String, List<MdmProductStock>> finishedProductStockMap,
        Map<String, Integer> mdmMonthSurplusMap,
        Map<String, String> productionTypeMap,
        Map<String, Integer> monthlySaleQty) {
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
        // 设置月均销量
        setAverageSaleQty(mergedPlan,monthlySaleQty);

        return mergedPlan;
    }


    private void saveOrderPoolSnapshot(String predictionVersion, YearMonth yearMonth, List<SupplyOrderPool> allStockUpOrders) {
        dpOrderPoolSnapshotService.saveOrderPoolSnapshot(predictionVersion,yearMonth,allStockUpOrders);
    }

    private void setAverageSaleQty(DpDemandPlan mergedPlan, Map<String, Integer> monthlySaleQty) {
        mergedPlan.setAverageSaleQty(monthlySaleQty.getOrDefault(mergedPlan.getMaterialCode(), 0));
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
        setProductionAndPriorityFlags(demandPlan, minProductionQty, statistics.totalNetQty);
    }

    private void setProductionType(DpDemandPlan mergedPlan, Map<String, String> productionTypeMap) {
        mergedPlan.setProductionType(productionTypeMap.getOrDefault(mergedPlan.getMaterialCode(),StringUtils.EMPTY));
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

    private int calculateStockQty(Map<String, List<MdmProductStock>> finishedProductStockMap, String groupKey) {
        if(org.springframework.util.CollectionUtils.isEmpty(finishedProductStockMap) || !finishedProductStockMap.containsKey(groupKey)){
            return BigDecimal.ZERO.intValue();
        }
        List<MdmProductStock> finishedProductStocks = finishedProductStockMap.get(groupKey);
        return finishedProductStocks.stream().filter(item -> null != item.getStockQty()).mapToInt(MdmProductStock::getStockQty).sum();
    }

    private int calculateRemainingQty(Map<String, List<MdmProductStock>> finishedProductStockMap, String groupKey) {
        if(org.springframework.util.CollectionUtils.isEmpty(finishedProductStockMap) || !finishedProductStockMap.containsKey(groupKey)){
            return BigDecimal.ZERO.intValue();
        }
        List<MdmProductStock> finishedProductStocks = finishedProductStockMap.get(groupKey);
        return finishedProductStocks.stream().filter(item -> null != item.getLeftOverQty()).mapToInt(MdmProductStock::getLeftOverQty).sum();
    }

    private int calculatePlannedSurplus(Map<String, Integer> mdmMonthSurplusMap, String factoryMaterialKey) {
        if(org.springframework.util.CollectionUtils.isEmpty(mdmMonthSurplusMap) || !mdmMonthSurplusMap.containsKey(factoryMaterialKey)){
            return BigDecimal.ZERO.intValue();
        }
        return mdmMonthSurplusMap.get(factoryMaterialKey);
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

    /**
     * 生成需求计划
     */
    private List<DpDemandPlan> generateDemandPlans(
        String predictionVersion,
        YearMonth yearMonth,
        List<DpOrderOffsetDetail> netDemands,
        DataCollection data) {
        List<DpDemandPlan> demandPlans = new ArrayList<>();
        // 处理净需求
        if (CollectionUtils.isNotEmpty(netDemands)) {
            List<MdmAreaCapaAllocation> areaCapaAllocations = mdmAreaCapaAllocationService.findAreaCapaAllocation(yearMonth.getYear(),yearMonth.getMonthValue());
            demandPlans.addAll(SaleRequirePlanHelper.processNetDemands(netDemands,areaCapaAllocations));
        }
        // 处理供应链订单
        if (CollectionUtils.isNotEmpty(data.getSupplyOrders())) {
            demandPlans.addAll(transformSupplyOrdersToDemandPlans(data.getSupplyOrders(), predictionVersion,yearMonth));
        }

        return demandPlans;
    }

    private List<DpDemandPlan> transformSupplyOrdersToDemandPlans(List<SupplyOrderPool> supplyOrders, String predictionVersion, YearMonth yearMonth) {
        return supplyOrders.stream()
            .map(order -> buildDemandPlan(order, predictionVersion,yearMonth))
            .collect(Collectors.toList());
    }

    private DpDemandPlan buildDemandPlan(SupplyOrderPool supplyOrder, String predictionVersion, YearMonth yearMonth) {
        DpDemandPlan demandPlan = new DpDemandPlan();
        BeanUtils.copyProperties(supplyOrder, demandPlan);
        demandPlan.setYear(yearMonth.getYear());
        demandPlan.setMonth(yearMonth.getMonthValue());
        demandPlan.setMonthPlanVersion(predictionVersion);
        demandPlan.setOrderPriority(supplyOrder.getOrderType());
        demandPlan.setScmPriority(supplyOrder.getOrderType());
        demandPlan.setOrderQty(supplyOrder.getQty()==null? BigDecimal.ZERO.intValue() : supplyOrder.getQty());
        demandPlan.setNetQty(demandPlan.getOrderQty());
        return demandPlan;
    }

    /**
     * 处理销售订单分配
     */
    private OrderAllocationResult processSalesOrderAllocation(
        String monthPlanVersion,
        YearMonth yearMonth,
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
        // 计算库存分配
        List<DpOrderOffsetDetail> allocations = StockAllocationHelper.calculateStockAllocation(
            monthPlanVersion,yearMonth, saleOrderGroupMap, finishedProductStockMap, monthSurplusMap);
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
        String predictionVersion,
        YearMonth yearMonth,
        OrderAllocationResult allocationResult) {
        // 批量插入分配结果
        if (CollectionUtils.isNotEmpty(allocationResult.getAllocations())) {
            this.baseDao.insertBatch(allocationResult.getAllocations());
        }
        // 批量插入库存版本
        dpStockVersionService.insertBatchData(predictionVersion,yearMonth,allocationResult.getStockMap());
    }

    /**
     * 并行获取所有必要数
     */
    private DataCollection fetchRequiredDataInParallel(String predictionVersion,MpFactoryProductionVersion finalVersion) {
        CompletableFuture<List<SalesOrderPool>> salesOrdersFuture =
            CompletableFuture.supplyAsync(this::fetchSalesOrderPool);
        CompletableFuture<List<MdmProductStock>> stocksFuture =
            CompletableFuture.supplyAsync(this::fetchFinishedProductStocks);
        CompletableFuture<Map<String, String>> productionTypeFuture =
            CompletableFuture.supplyAsync(this::fetchProductionTypeMap);
        CompletableFuture<Integer> minProductionQtyFuture =
            CompletableFuture.supplyAsync(this::getMinProductionQty);
        CompletableFuture<Map<String, MdmMaterialInfo>> fetchMaterialInfoFuture =
            CompletableFuture.supplyAsync(this::fetchMaterialInfo);
        CompletableFuture<List<SupplyOrderPool>> supplyOrdersFuture =
            CompletableFuture.supplyAsync(() -> this.fetchSupplyOrderPool(finalVersion));

        // 等待所有任务完成
        CompletableFuture.allOf(
            salesOrdersFuture, stocksFuture,productionTypeFuture,minProductionQtyFuture,fetchMaterialInfoFuture
        ).join();

        try {
            List<SalesOrderPool> salesOrders = salesOrdersFuture.get();
            List<SupplyOrderPool> supplyOrders = supplyOrdersFuture.get();
            List<MdmProductStock> finishedProductStocks = stocksFuture.get();
            Map<String, String> productionTypeMap = productionTypeFuture.get();
            int minProductionQty = minProductionQtyFuture.get();
            Map<String, MdmMaterialInfo> materialInfoMap = fetchMaterialInfoFuture.get();
            if(CollectionUtils.isNotEmpty(finishedProductStocks)){
                finishedProductStocks.forEach(finishedProductStock -> finishedProductStock.setLeftOverQty(null == finishedProductStock.getStockQty()?BigDecimal.ZERO.intValue():finishedProductStock.getStockQty()));
            }
            // 处理成品库存映射
            Map<String, List<MdmProductStock>> finishedProductStockMap =
                CollectionUtils.isEmpty(finishedProductStocks) ?
                    new HashMap<>(16) :
                    finishedProductStocks.stream()
                        .collect(Collectors.groupingBy(MdmProductStock::getGroupKey));
            Map<String, Integer> monthSurplusMap = this.factoryMonthPlanProductionFinalResultService.calculateMonthSurplus(predictionVersion,finishedProductStocks);
            return new DataCollection(
                salesOrders,
                supplyOrders,
                finishedProductStocks,
                finishedProductStockMap,
                monthSurplusMap,
                productionTypeMap,
                minProductionQty,
                materialInfoMap
            );

        } catch (Exception e) {
            log.error("并行获取数据失败", e);
            throw new BusinessException("获取数据失败");
        }
    }

    private List<SupplyOrderPool> fetchSupplyOrderPool(MpFactoryProductionVersion finalVersion) {
        return this.dpOrderPoolSnapshotService.fetchSupplyOrderPool(finalVersion);
    }

    private Map<String, MdmMaterialInfo> fetchMaterialInfo() {
        return materialInfoService.skuToMaterialInfo();
    }

    /**
     * 获取排产类型
     */
    private Map<String, String> fetchProductionTypeMap() {
        return mdmSkuScheduleCategoryService.skuToProductionType();
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
    private List<MpFactoryProductionVersion> validateProductionVersionFinalized(YearMonth tMonth) {
        return factoryProductionVersionMapper.selectList(
            Wrappers.<MpFactoryProductionVersion>lambdaQuery()
                .eq(MpFactoryProductionVersion::getFactoryCode, FactoryConstant.DEFAULT_FACTORY_CODE)
                .eq(MpFactoryProductionVersion::getYear, tMonth.getYear())
                .eq(MpFactoryProductionVersion::getMonth, tMonth.getMonthValue())
                .eq(MpFactoryProductionVersion::getIsFinal,YesOrNoEnum.YES.getCode())
        );
    }

    /**
     * 数据集合
     */
    @Getter
    private static class DataCollection {
        private final List<SalesOrderPool> salesOrders;
        private final List<SupplyOrderPool> supplyOrders;
        private final List<MdmProductStock> finishedProductStocks;
        private final Map<String, List<MdmProductStock>> finishedProductStockMap;
        private final Map<String, Integer> monthSurplusMap;
        private final Map<String, String> productionTypeMap;
        private final Integer minProductionQty;
        private final Map<String, MdmMaterialInfo> materialInfoMap;


        public DataCollection(
            List<SalesOrderPool> salesOrders,
            List<SupplyOrderPool> supplyOrders,
            List<MdmProductStock> finishedProductStocks,
            Map<String, List<MdmProductStock>> finishedProductStockMap,
            Map<String, Integer> monthSurplusMap,
            Map<String, String> productionTypeMap,
            Integer minProductionQty,
            Map<String, MdmMaterialInfo> materialInfoMap) {
            this.salesOrders = CollectionUtils.isNotEmpty(salesOrders)? salesOrders : Collections.emptyList();
            this.supplyOrders = CollectionUtils.isNotEmpty(supplyOrders)? supplyOrders : Collections.emptyList();
            this.finishedProductStocks = finishedProductStocks != null ? finishedProductStocks : Collections.emptyList();
            this.finishedProductStockMap = finishedProductStockMap != null ? finishedProductStockMap : new HashMap<>();
            this.monthSurplusMap = monthSurplusMap != null ? monthSurplusMap : new HashMap<>();
            this.productionTypeMap = productionTypeMap != null ? productionTypeMap : new HashMap<>();
            this.minProductionQty = minProductionQty;
            this.materialInfoMap = materialInfoMap != null ? materialInfoMap : new HashMap<>();
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

            totalOrderQty += plan.getOrderQty() == null?BigDecimal.ZERO.intValue():plan.getOrderQty();
            totalNetQty += plan.getNetQty()== null?BigDecimal.ZERO.intValue():plan.getNetQty();
            // 根据订单优先级累加对应数量
            String priority = plan.getScmPriority();
            int netQty = plan.getNetQty() == null?BigDecimal.ZERO.intValue():plan.getNetQty();

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
        demandPlan.setPlanType(ProductionPlanType.PREDICTION.getPlanType());
        demandPlan.setIsImport(YesOrNoEnum.NO.getCode());
    }

    private void saveFactoryProductionVersion(YearMonth yearMonth, String monthPlanVersion) {
        MpFactoryProductionVersion version = new MpFactoryProductionVersion();
        version.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        version.setYear(yearMonth.getYear());
        version.setMonth(yearMonth.getMonthValue());
        version.setMonthPlanVersion(monthPlanVersion);
        version.setPlanType(ProductionPlanType.PREDICTION.getPlanType());
        version.setIsFinal(YesOrNoEnum.NO.getCode());
        version.setProductTypeCode(ProductTypeEnum.WHOLE_STEEL.getValue());
        factoryProductionVersionMapper.insert(version);
    }

    /**
     * 自定义高性能比较器实现
     * 避免重复解析和lambda开销
     */
    private static class DemandPlanComparator implements Comparator<DpDemandPlan> {

        @Override
        public int compare(DpDemandPlan o1, DpDemandPlan o2) {
            // 1. 比较供应链优先级
            int scmPriorityCompare = compareScmPriority(o1, o2);
            if (scmPriorityCompare != 0) {
                return scmPriorityCompare;
            }

            // 2. 比较提报日期
            int dateCompare = compareYearWeek(o1, o2);
            if (dateCompare != 0) {
                return dateCompare;
            }

            // 3. 比较提报量
            return compareOrdQty(o1, o2);
        }

        private int compareScmPriority(DpDemandPlan o1, DpDemandPlan o2) {
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

        private int compareYearWeek(DpDemandPlan o1, DpDemandPlan o2) {
            Integer d1 = parseYearWeek(o1.getYearWeek()) ;
            Integer d2 = parseYearWeek(o2.getYearWeek()) ;

            if (d1 == null && d2 == null) {
                return 0;
            }
            if (d1 == null) {
                return 1; // null排最后
            }
            if (d2 == null) {
                return -1;
            }
            return Integer.compare(d1, d2);
        }

        private int compareOrdQty(DpDemandPlan o1, DpDemandPlan o2) {
            Integer q1 = o1.getPostponeQty();
            Integer q2 = o2.getPostponeQty();

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

        private Integer parseYearWeek(String yearWeek) {
            if (StringUtils.isEmpty(yearWeek)) {
                return null;
            }
            try {
                return Integer.parseInt(yearWeek.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
