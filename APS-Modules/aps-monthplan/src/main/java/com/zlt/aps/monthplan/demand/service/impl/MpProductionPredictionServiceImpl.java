package com.zlt.aps.monthplan.demand.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
    // 需求计划
    private final IDpDemandPlanService dpDemandPlanService;
    // 历史销售记录
    private final IMpHistorySaleRecordService mpHistorySaleRecordService;



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
        Map<String, Long>  tMonthSaleQty =   this.mpHistorySaleRecordService.calculateMonthSaleQty(6);
        // 生成T月模拟需求计划
        List<DpDemandPlan> tMonthDemands = createDemandPlan(monthRangeResult.getTMonth(),tMonthSaleQty);
        // 预测T+1月需求量
        Map<String,Long> tPlus1MonthDemandQty = this.calculateMonthDemandQty(tMonthDemands,monthRangeResult.getTPlus1Month(),finalVersion);


        Map<String, Long>  tPlus1MonthSaleQty =   this.mpHistorySaleRecordService.calculateMonthSaleQty(5);
        // 生成T月模拟需求计划
        List<DpDemandPlan> tPlus1MonthDemands = createDemandPlan(monthRangeResult.getTPlus1Month(),tPlus1MonthSaleQty);
        // 调用接口生成月度排产   12、以第11步的T+1月的需求量，按月度排产逻辑进行排产(此时暂缓订单需要排产)，得到T+1月的月排产计划
        // T+1月排产结束后：计算得到T+2月的(实单+暂缓+周期储备排产)未排量 = 高优先级未排产量 + 中优先级未排产量 + 暂缓订单未排量 + 周期排产储备未排产量
        MpFactoryProductionVersion tPlus1MonthProductionVersion = new MpFactoryProductionVersion();
        tPlus1MonthProductionVersion.setFactoryCode(tMonthDemands.get(0).getFactoryCode());
        tPlus1MonthProductionVersion.setYear(tMonthDemands.get(0).getYear());
        tPlus1MonthProductionVersion.setMonth(tMonthDemands.get(0).getMonth());
        tPlus1MonthProductionVersion.setMonthPlanVersion(tMonthDemands.get(0).getMonthPlanVersion());
        // 预测T+2月需求量
        Map<String,Long> tPlus2MonthDemandQty = this.calculateMonthDemandQty(tPlus1MonthDemands,monthRangeResult.getTPlus2Month(),tPlus1MonthProductionVersion);
        // 15、将T月的月度计划及预测排产的T+1月、T+2月的排产计划，合并汇总得到各SKU的T月、T+1月、T+2月的排产量
        Map<String,Long> tMonthDemandQty = this.calculateMonthDemandQty(finalVersion);
        Map<String, MdmMaterialInfo> materialInfoMap = fetchMaterialInfo();
        List<MpProductionPrediction> list = buildProductionPrediction(tMonthDemandQty,tPlus1MonthDemandQty,tPlus2MonthDemandQty,materialInfoMap,finalVersion);
        if(CollectionUtils.isNotEmpty(list)) {
            this.baseDao.insertBatch(list);
        }
        return AjaxResult.success();
    }

    private List<MpProductionPrediction> buildProductionPrediction(Map<String, Long> tMonthDemandQty, Map<String, Long> tPlus1MonthDemandQty, Map<String, Long> tPlus2MonthDemandQty, Map<String, MdmMaterialInfo> materialInfoMap,MpFactoryProductionVersion finalVersion) {
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
                productionPrediction.setMonth1(productionQty);
                productionPrediction.setMonth2(tPlus1MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.longValue()));
                productionPrediction.setMonth3(tPlus2MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.longValue()));
                list.add(productionPrediction);
        });
        return list;
    }

    private Map<String, Long> calculateMonthDemandQty(MpFactoryProductionVersion finalVersion) {
        List<FactoryMonthPlanProductionFinalResult> productionFinalResults = factoryMonthPlanProductionFinalResultService.findProductionFinalResult(finalVersion);
        if(CollectionUtils.isEmpty(productionFinalResults)) {
            return Collections.emptyMap();
        }
        return productionFinalResults.stream()
            .filter(Objects::nonNull)
            .filter(productionFinalResult -> StringUtils.isNotBlank(productionFinalResult.getMaterialCode()) && productionFinalResult.getTotalQty() != null)
            .collect(Collectors.groupingBy(FactoryMonthPlanProductionFinalResult::getMaterialCode,
                Collectors.summingLong(FactoryMonthPlanProductionFinalResult::getTotalQty)
            ));
    }

    private List<DpDemandPlan> createDemandPlan(YearMonth yearMonth,Map<String, Long>  monthSaleQty) {
        // 4、生成预测版本号(PRE+yyyymmdd+3位流水号)
        String predictionVersion = requirementVersionService.generateVersion(PREFIX);
        // 5. 并行获取数据
        DataCollection data = fetchRequiredDataInParallel(predictionVersion);
        // 6. 处理销售订单分配
        OrderAllocationResult allocationResult = processSalesOrderAllocation(
            predictionVersion,yearMonth,data.getSalesOrders(), data.getFinishedProductStockMap(),
            data.getMonthSurplusMap());
        //  (1) 得到对冲后的销售订单净需求数据(包含暂缓订单+高优先级+中优先级的净需求)
        // 7. 处理需求计划生成
        List<DpDemandPlan> demandPlans = generateDemandPlans(
            yearMonth,allocationResult.getNetDemands());
        // 8. 合并并保存需求计划
        List<DpDemandPlan> mergedDemandPlans =  saveDemandPlans(predictionVersion,yearMonth, demandPlans, data,monthSaleQty);
        //  (2) 同时，保存预测版本号T月的订单分配结果
        saveAllocationResults(predictionVersion,yearMonth,allocationResult);
        // 保存分厂排产版本
        saveFactoryProductionVersion(yearMonth,predictionVersion, data.getSalesOrders());
        return mergedDemandPlans;
    }

    private Map<String, Long> calculateMonthDemandQty(List<DpDemandPlan>  demandPlans,YearMonth yearMonth,MpFactoryProductionVersion finalVersion) {
        if(CollectionUtils.isEmpty(demandPlans) || null == finalVersion) {
            return Collections.emptyMap();
        }
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
        saveOrderPoolSnapshot(demandPlans.get(0).getMonthPlanVersion(),yearMonth,allStockUpOrders);
        // T月月度计划对应实单已排产量（销售订单）+ T月已生产量
        List<FactoryMonthPlanProductionFinalResult> productionFinalResults = factoryMonthPlanProductionFinalResultService.findProductionFinalResult(finalVersion);
        //  T月实单未排产量：	300	(高优先级净需求+中优先级净需求+暂缓订单净需求-T月实单排产量+T月实单已完成量)
        Map<String,Long> unProductionQty = this.getUnProductionQtyMap(demandPlans,productionFinalResults);
        // 10、从月度计划定稿版本获取对应的周期储备排产，按SKU扣减T月月度计划对应周期储备已排产量得到T月周期储备还未排产量
        // T月周期储备未排产量：	0	    SKU:T月定稿版本的周期排产储备量 - T月月度计划周期储备已排产量 + T月周期已完成量
        Map<String,Long> unProductionQtyByCycleStockUp = this.getUnProductionQtyByCycleStockUp(demandPlans,productionFinalResults);
        // T+1月的储备订单(包含周期排产储备+常规储备)
        Map<String,Long> stockUpOrderQty = this.getStockUpOrderQtyMap(allStockUpOrders);
        // 11、计算T+1月的需求量	 = 第9步骤中T月实单未排产量 + 第10步骤中T月周期储备未排产量 + 第8步中的T+1月的储备订单(包含周期排产储备+常规储备)
        return  calculateDemandQty(unProductionQty,unProductionQtyByCycleStockUp,stockUpOrderQty);
    }

    private Map<String, Long> getStockUpOrderQtyMap(List<SupplyOrderPool> allStockUpOrders) {
        if(CollectionUtils.isEmpty(allStockUpOrders)){
            return Collections.emptyMap();
        }
        return allStockUpOrders.stream()
            .filter(Objects::nonNull)
            .filter(supplyOrder -> StringUtils.isNotBlank(supplyOrder.getMaterialCode()) && null != supplyOrder.getQty())
            .collect(Collectors.groupingBy(SupplyOrderPool::getMaterialCode, Collectors.summingLong(SupplyOrderPool::getQty)));
    }

    private Map<String,Long> calculateDemandQty(Map<String, Long> unProductionQty, Map<String, Long> unProductionQtyByCycleStockUp, Map<String,Long> stockUpOrderQty) {
        Map<String,Long> tPlus1MonthDemandQty = Maps.newHashMap();
        //  = 第9步骤中T月实单未排产量 + 第10步骤中T月周期储备未排产量+ 第8步中的T+1月的储备订单(包含周期排产储备+常规储备)
        unProductionQty.forEach((key,value) -> tPlus1MonthDemandQty.put(key,value
            + unProductionQtyByCycleStockUp.getOrDefault(key,BigDecimal.ZERO.longValue())
            + stockUpOrderQty.getOrDefault(key,BigDecimal.ZERO.longValue())));
        return tPlus1MonthDemandQty;
    }

    private Map<String, Long> getUnProductionQtyByCycleStockUp(List<DpDemandPlan> tMonthDemandPlans, List<FactoryMonthPlanProductionFinalResult> productionFinalResults) {
        Map<String, Long> unProductioCycleReserveQtyMap = Maps.newHashMap();
        Map<String,Long> cycleReserveQtyMap = this.getCycleReserveQtyMap(tMonthDemandPlans);
        Map<String,Long> productionCycleReserveQtyMap = this.productionCycleReserveQtyMap(productionFinalResults);
        cycleReserveQtyMap.forEach((key,value) -> unProductioCycleReserveQtyMap.put(key,value - productionCycleReserveQtyMap.getOrDefault(key,BigDecimal.ZERO.longValue())));
        return unProductioCycleReserveQtyMap;
    }

    private Map<String, Long> productionCycleReserveQtyMap(List<FactoryMonthPlanProductionFinalResult> productionFinalResults) {
        if(CollectionUtils.isEmpty(productionFinalResults)){
            return Collections.emptyMap();
        }
        return productionFinalResults.stream()
            .filter(Objects::nonNull)
            .filter(productionFinalResult -> StringUtils.isNotBlank(productionFinalResult.getMaterialCode()) && null != productionFinalResult.getCycleProductionQty())
            .collect(Collectors.groupingBy(FactoryMonthPlanProductionFinalResult::getMaterialCode, Collectors.summingLong(FactoryMonthPlanProductionFinalResult::getCycleProductionQty)));
    }

    private Map<String, Long> getCycleReserveQtyMap(List<DpDemandPlan> tMonthDemandPlans) {
        return tMonthDemandPlans.stream()
            .filter(Objects::nonNull)
            .filter(demandPlan -> StringUtils.isNotBlank(demandPlan.getMaterialCode()) && null != demandPlan.getCycleReserveQty())
            .collect(Collectors.groupingBy(DpDemandPlan::getMaterialCode, Collectors.summingLong(item -> item.getCycleReserveQty().longValue())));
    }


    private Map<String, Long> getUnProductionQtyMap(List<DpDemandPlan> tPlus1MonthDemandPlans, List<FactoryMonthPlanProductionFinalResult> productionFinalResults) {

        Map<String, Long> unProductionQtyMap = Maps.newHashMap();
        Map<String,Long> postponeNetQtyMap = this.getPostponeNetQtyMap(tPlus1MonthDemandPlans);
        Map<String,Long> productionQtyMap = this.getProductionQtyMap(productionFinalResults);
        postponeNetQtyMap.forEach((key,value) -> unProductionQtyMap.put(key,value - productionQtyMap.getOrDefault(key,BigDecimal.ZERO.longValue())));
        return unProductionQtyMap;
    }

    private Map<String, Long> getProductionQtyMap(List<FactoryMonthPlanProductionFinalResult> productionFinalResults) {
        if(CollectionUtils.isEmpty(productionFinalResults)) {
            return Maps.newHashMap();
        }
        return productionFinalResults.stream()
            .filter(Objects::nonNull)
            .filter(productionFinalResult -> StringUtils.isNotBlank(productionFinalResult.getMaterialCode()) && null != productionFinalResult.getTotalQty())
            .collect(Collectors.groupingBy(FactoryMonthPlanProductionFinalResult::getMaterialCode, Collectors.summingLong(FactoryMonthPlanProductionFinalResult::getTotalQty)));
    }

    private Map<String, Long> getPostponeNetQtyMap(List<DpDemandPlan> tPlus1MonthDemandPlans) {
        return tPlus1MonthDemandPlans.stream()
            .filter(Objects::nonNull)
            .filter(demandPlan -> StringUtils.isNotBlank(demandPlan.getMaterialCode()) && null != demandPlan.getPostponeNetQty())
            .collect(Collectors.groupingBy(DpDemandPlan::getMaterialCode, Collectors.summingLong(item -> item.getPostponeNetQty().longValue())));
    }

    private void saveOrderPoolSnapshot(String predictionVersion, YearMonth yearMonth, List<SupplyOrderPool> allStockUpOrders) {
        dpOrderPoolSnapshotService.saveOrderPoolSnapshot(predictionVersion,yearMonth,allStockUpOrders);
    }

    private  List<DpDemandPlan> saveDemandPlans(String predictionVersion,YearMonth yearMonth, List<DpDemandPlan> demandPlans, DataCollection data,Map<String, Long>  monthSaleQty) {
        // 合并需求计划
        List<DpDemandPlan> mergedPlans = mergedDemandPlan(predictionVersion,yearMonth, demandPlans, data,monthSaleQty);
        if (CollectionUtils.isNotEmpty(mergedPlans)) {
            this.baseDao.insertBatch(mergedPlans);
        }
        return mergedPlans;
    }

    private List<DpDemandPlan> mergedDemandPlan(String monthPlanVersion, YearMonth yearMonth, List<DpDemandPlan> demandPlans, DataCollection data,Map<String, Long>  monthSaleQty) {
        // 快速失败：空集合直接返回
        if (CollectionUtils.isEmpty(demandPlans)) {
            return Collections.emptyList();
        }
        return demandPlans.parallelStream()
            .collect(Collectors.groupingByConcurrent(DpDemandPlan::getGroupKey))
            .values()
            .stream()
            .map(dpDemandPlans -> buildMergedDemandPlan(
                monthPlanVersion,
                yearMonth,
                dpDemandPlans
                ,data,monthSaleQty))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private DpDemandPlan buildMergedDemandPlan(String monthPlanVersion, YearMonth yearMonth, List<DpDemandPlan> groupPlans, DataCollection data,Map<String, Long>  monthSaleQty) {
        // 验证分组数据有效性
        if (CollectionUtils.isEmpty(groupPlans)) {
            return null;
        }

        // 获取基础模板（第一个元素）
        DpDemandPlan template = groupPlans.get(0);
        if(!data.materialInfoMap.containsKey(template.getMaterialCode())) {
            return null;
        }
        // 使用构建器模式创建新对象（避免BeanCopyUtils的性能开销）
        DpDemandPlan mergedPlan = createMergedDemandPlan(template,monthPlanVersion, yearMonth);
        // 设置物料信息（使用computeIfAbsent优化Map访问）
        setMaterialInfo(mergedPlan, data.materialInfoMap);
        // 设置库存和计划盈余
        setStockAndSurplusInfo(mergedPlan, data.finishedProductStockMap, data.monthSurplusMap);
        // 设置排产分类
        setProductionType(mergedPlan,data.productionTypeMap);
        // 计算并设置各类数量统计
        setQuantityStatistics(mergedPlan, groupPlans, data.minProductionQty);
        // 设置月均销量
        setAverageSaleQty(mergedPlan,monthSaleQty);

        return mergedPlan;
    }

    private void setAverageSaleQty(DpDemandPlan mergedPlan, Map<String, Long> monthlySaleQty) {
        mergedPlan.setAverageSaleQty(BigDecimal.valueOf(monthlySaleQty.getOrDefault(mergedPlan.getMaterialCode(), 0L)));
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
        demandPlan.setOrderQty(BigDecimal.valueOf(statistics.totalOrderQty));
        demandPlan.setNetQty(BigDecimal.valueOf(statistics.totalNetQty));

        // 设置优先级相关数量
        demandPlan.setHeightQty(BigDecimal.valueOf(statistics.heightQty));
        demandPlan.setMidQty(BigDecimal.valueOf(statistics.midQty));
        demandPlan.setPostponeQty(BigDecimal.valueOf(statistics.postponeQty));
        demandPlan.setCycleReserveQty(BigDecimal.valueOf(statistics.cycleReserveQty));
        demandPlan.setConventionReserveQty(BigDecimal.valueOf(statistics.conventionReserveQty));

        // 计算派生数量
        calculateDerivedQuantities(demandPlan, statistics);

        // 设置生产和优先级标识
        setProductionAndPriorityFlags(demandPlan, groupPlans, minProductionQty, statistics.totalNetQty);
    }

    private void setProductionType(DpDemandPlan mergedPlan, Map<String, String> productionTypeMap) {
        mergedPlan.setProductionType(productionTypeMap.getOrDefault(mergedPlan.getGroupKey(),StringUtils.EMPTY));
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
        demandPlan.setStockQty(BigDecimal.valueOf(calculateStockQty(finishedProductStockMap, factoryMaterialKey)));
        // 结余库存
        demandPlan.setRemainingQty(BigDecimal.valueOf(calculateRemainingQty(finishedProductStockMap, factoryMaterialKey)));

        // 计算月底计划余量
        demandPlan.setPlannedSurplus(BigDecimal.valueOf(calculatePlannedSurplus(mdmMonthSurplusMap, factoryMaterialKey)));
    }

    private Long calculateStockQty(Map<String, List<MdmProductStock>> finishedProductStockMap, String groupKey) {
        if(org.springframework.util.CollectionUtils.isEmpty(finishedProductStockMap) || !finishedProductStockMap.containsKey(groupKey)){
            return BigDecimal.ZERO.longValue();
        }
        List<MdmProductStock> finishedProductStocks = finishedProductStockMap.get(groupKey);
        return finishedProductStocks.stream().mapToLong(MdmProductStock::getStockQty).sum();
    }

    private Long calculateRemainingQty(Map<String, List<MdmProductStock>> finishedProductStockMap, String groupKey) {
        if(org.springframework.util.CollectionUtils.isEmpty(finishedProductStockMap) || !finishedProductStockMap.containsKey(groupKey)){
            return BigDecimal.ZERO.longValue();
        }
        List<MdmProductStock> finishedProductStocks = finishedProductStockMap.get(groupKey);
        return finishedProductStocks.stream().filter(item -> null != item.getLeftOverQty()).mapToLong(MdmProductStock::getLeftOverQty).sum();
    }

    private Long calculatePlannedSurplus(Map<String, Long> mdmMonthSurplusMap, String groupFactoryAndMaterialKey) {
        if(org.springframework.util.CollectionUtils.isEmpty(mdmMonthSurplusMap) || !mdmMonthSurplusMap.containsKey(groupFactoryAndMaterialKey)){
            return BigDecimal.ZERO.longValue();
        }
        return mdmMonthSurplusMap.get(groupFactoryAndMaterialKey);
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
    private DpDemandPlan createMergedDemandPlan(DpDemandPlan template, String monthPlanVersion, YearMonth yearMonth) {
        DpDemandPlan mergedPlan = BeanCopyUtils.copyBean(template,DpDemandPlan.class);
        mergedPlan.setMonthPlanVersion(monthPlanVersion);
        mergedPlan.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        mergedPlan.setYear(yearMonth.getYear());
        mergedPlan.setMonth(yearMonth.getMonthValue());
        // 重置ID和基础值
        mergedPlan.setId(null);
        mergedPlan.setBaseVale(null);
        return mergedPlan;
    }

    /**
     * 获取最小投产量
     * @return 最小投产量
     */
    private long getMinProductionQty() {
        FactoryParam factoryParam = new FactoryParam();
        factoryParam.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        factoryParam.setParamCode(MonthPlanEnums.MIN_PRODUCTION_QTY.getCode());
        factoryParam.setProductTypeCode(ProductTypeEnum.WHOLE_STEEL.getValue());
        FactoryParam param = factoryParamService.getFacParamSingle(factoryParam);
        long paramValue = BigDecimal.ZERO.longValue();
        if (param != null) {
            paramValue = StringUtils.isNotEmpty(param.getParamValue()) ? Long.valueOf(param.getParamValue())
                : Long.valueOf(param.getDefauleValue());
        }
        return paramValue;
    }

    private List<DpDemandPlan> generateDemandPlans(YearMonth yearMonth, List<DpOrderOffsetDetail> netDemands) {
        List<DpDemandPlan> demandPlans = new ArrayList<>();
        // 处理净需求
        if (CollectionUtils.isNotEmpty(netDemands)) {
            List<MdmAreaCapaAllocation> areaCapaAllocations =
                mdmAreaCapaAllocationService.findAreaCapaAllocation(yearMonth.getYear(),yearMonth.getMonthValue());
            demandPlans.addAll(SaleRequirePlanHelper.processNetDemands(netDemands, areaCapaAllocations));
        }
        return demandPlans;
    }

    /**
     * 处理销售订单分配
     */
    private OrderAllocationResult processSalesOrderAllocation(
        String monthPlanVersion,
        YearMonth yearMonth,
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
    private DataCollection fetchRequiredDataInParallel(String predictionVersion) {
        CompletableFuture<List<SalesOrderPool>> salesOrdersFuture =
            CompletableFuture.supplyAsync(this::fetchSalesOrderPool);
        CompletableFuture<List<MdmProductStock>> stocksFuture =
            CompletableFuture.supplyAsync(this::fetchFinishedProductStocks);
        CompletableFuture<Map<String, String>> productionTypeFuture =
            CompletableFuture.supplyAsync(this::fetchProductionTypeMap);
        CompletableFuture<Long> minProductionQtyFuture =
            CompletableFuture.supplyAsync(this::getMinProductionQty);
        CompletableFuture<Map<String, MdmMaterialInfo>> fetchMaterialInfoFuture =
            CompletableFuture.supplyAsync(this::fetchMaterialInfo);

        // 等待所有任务完成
        CompletableFuture.allOf(
            salesOrdersFuture, stocksFuture,productionTypeFuture,minProductionQtyFuture,fetchMaterialInfoFuture
        ).join();

        try {
            List<SalesOrderPool> salesOrders = salesOrdersFuture.get();
            List<MdmProductStock> finishedProductStocks = stocksFuture.get();
            Map<String, String> productionTypeMap = productionTypeFuture.get();
            long minProductionQty = minProductionQtyFuture.get();
            Map<String, MdmMaterialInfo> materialInfoMap = fetchMaterialInfoFuture.get();
            // 处理成品库存映射
            Map<String, List<MdmProductStock>> finishedProductStockMap =
                CollectionUtils.isEmpty(finishedProductStocks) ?
                    new HashMap<>(16) :
                    finishedProductStocks.stream()
                        .collect(Collectors.groupingBy(MdmProductStock::getGroupKey));
            Map<String, Long> monthSurplusMap = this.factoryMonthPlanProductionFinalResultService.calculateMonthSurplus(predictionVersion,finishedProductStocks);
            return new DataCollection(
                salesOrders,
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
        private final List<MdmProductStock> finishedProductStocks;
        private final Map<String, List<MdmProductStock>> finishedProductStockMap;
        private final Map<String, Long> monthSurplusMap;
        private final Map<String, String> productionTypeMap;
        private final long minProductionQty;
        private final Map<String, MdmMaterialInfo> materialInfoMap;


        public DataCollection(
            List<SalesOrderPool> salesOrders,
            List<MdmProductStock> finishedProductStocks,
            Map<String, List<MdmProductStock>> finishedProductStockMap,
            Map<String, Long> monthSurplusMap,
            Map<String, String> productionTypeMap,
            long minProductionQty,
            Map<String, MdmMaterialInfo> materialInfoMap) {
            this.salesOrders = CollectionUtils.isNotEmpty(salesOrders)? salesOrders : Collections.emptyList();
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

            totalOrderQty += plan.getOrderQty().longValue();
            totalNetQty += plan.getNetQty().longValue();

            // 根据订单优先级累加对应数量
            String priority = plan.getOrderPriority();
            long netQty = plan.getNetQty().longValue();

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
        demandPlan.setPostponeNetQty(BigDecimal.valueOf(statistics.heightQty + statistics.midQty + statistics.postponeQty));

        // (9)净需求(不含暂缓) = 高优先级净需求量 + 中优先级净需求量
        demandPlan.setUnPostponeNetQty(BigDecimal.valueOf(statistics.heightQty + statistics.midQty));
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
        demandPlan.setPlanType(ProductionPlanType.PREDICTION.getPlanType());
        demandPlan.setIsImport(YesOrNoEnum.NO.getCode());
    }

    private void saveFactoryProductionVersion(YearMonth yearMonth, String monthPlanVersion, List<SalesOrderPool> salesOrders) {
        MpFactoryProductionVersion version = new MpFactoryProductionVersion();
        version.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        version.setYear(yearMonth.getYear());
        version.setMonth(yearMonth.getMonthValue());
        version.setMonthPlanVersion(monthPlanVersion);
        version.setPlanType(ProductionPlanType.PREDICTION.getPlanType());
        version.setIsFinal(YesOrNoEnum.NO.getCode());
        // 取销售订单的胎别
        if (CollectionUtils.isNotEmpty(salesOrders)) {
            SalesOrderPool saleOrder = salesOrders.get(0);
            version.setProductTypeCode(saleOrder.getProductType());
        }
        factoryProductionVersionMapper.insert(version);
    }
}
