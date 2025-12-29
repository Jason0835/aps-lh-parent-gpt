package com.zlt.aps.monthplan.demand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.service.*;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.demand.mapper.SupplyOrderPoolEntityMapper;
import com.zlt.aps.monthplan.demand.service.ISalesOrderPoolService;
import com.zlt.aps.monthplan.demand.service.ISupplyOrderPoolService;
import com.zlt.aps.monthplan.enums.SupplyOrderTypeEnum;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProductionFinalResultService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：SupplyOrderPoolServiceImpl.java
 * 描    述：SupplyOrderPoolServiceImpl供应链订单池业务层处理
 *@author yelq
 *@date 2025-12-22
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
@Transactional(rollbackFor = Exception.class)
public class SupplyOrderPoolServiceImpl extends AbstractDocService<SupplyOrderPool>  implements ISupplyOrderPoolService {
    private static final int DAYS_PER_MONTH = 30;

    private final SupplyOrderPoolEntityMapper supplyOrderPoolEntityMapper;

    private final IMdmCycleSchStruConfService mdmCycleSchStruConfService;
    // 物料信息
    private final MdmMaterialInfoEntityMapper mdmMaterialInfoEntityMapper;
    // 超期SKU
    private final IMpOverdueSkuService overdueSkuService;
    // 月均销量
    private final IMpMonthlySaleQtyService monthlySaleQtyService;
    // 成品库存
    private final IMdmProductStockService mdmProductStockService;
    // 销售订单
    private final ISalesOrderPoolService salesOrderPoolService;
    // 定稿的月度排产计划
    private final IFactoryMonthPlanProductionFinalResultService factoryMonthPlanProductionFinalResultService;
    // 历史销售记录
    private final IMpHistorySaleRecordService historySaleRecordService;
    // 排产设定
    private final IFactoryParamService iFactoryParamService;

    @Override
    protected String getDocTypeCode() {
        return "2025122214";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("2025122214");
        return sysDocType;
    }

    @Override
    public String checkUnique(SupplyOrderPool docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.supplyOrderPool.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public void createCycleStockUp(SupplyOrderPool supplyOrderPool) {
        // 1. 验证前置条件
        validatePrerequisites();

        // 2. 获取需要处理的SKU集合
        Set<String> eligibleSkus = getEligibleSkus();
        if (CollectionUtils.isEmpty(eligibleSkus)) {
            throw new BusinessException(I18nUtil.getMessage(
                "ui.message.createCycleStockUp.notExist.cycleStockUpMaterial"));
        }

        // 3. 清理旧数据并批量创建新数据
        recreateSupplyOrderPools(eligibleSkus);
    }

    /**
     * 重新创建供应链订单池
     */
    private void recreateSupplyOrderPools(Set<String> skus) {
        // 3.1 清理旧数据
        deleteSupplyOrderPool(SupplyOrderTypeEnum.CYCLE_PRODUCTION_STOCK.getCode());

        // 3.2 准备计算所需数据
        CalculationData calculationData = prepareCalculationData();

        // 3.3 批量构建并插入订单池数据
        List<SupplyOrderPool> supplyOrderPools = buildSupplyOrderPoolsInParallel(
            skus, calculationData);

        if (CollectionUtils.isNotEmpty(supplyOrderPools)) {
            this.baseDao.insertBatch(supplyOrderPools);
        }
    }

    /**
     * 并行构建供应链订单池
     */
    private List<SupplyOrderPool> buildSupplyOrderPoolsInParallel(
        Set<String> skus, CalculationData data) {

        return skus.parallelStream()
            .map(sku -> buildSupplyOrderPool(
                sku,
                data))
            .collect(Collectors.toList());
    }

    /**
     * 准备计算所需的所有数据
     */
    private CalculationData prepareCalculationData() {
        // 并行获取所有必要数据
        CompletableFuture<List<MpMonthlySaleQty>> monthlySaleQtyFuture =
            CompletableFuture.supplyAsync(monthlySaleQtyService::findCurrentMonthlySaleQty);

        CompletableFuture<List<MdmMaterialInfo>> materialsFuture =
            CompletableFuture.supplyAsync(this::getAllActiveMaterials);
        CompletableFuture<List<MdmCycleSchStruConf>> cycleSchStruConfFuture =
            CompletableFuture.supplyAsync(mdmCycleSchStruConfService::findCycleSchStruConf);

        CompletableFuture<List<MdmProductStock>> stocksFuture =
            CompletableFuture.supplyAsync(mdmProductStockService::findCurrentFinishStock);

        CompletableFuture<List<SalesOrderPool>> salesOrderFuture =
            CompletableFuture.supplyAsync(salesOrderPoolService::findCurrentSalesOrderPool);

        CompletableFuture<List<FactoryMonthPlanProductionFinalResult>> planFuture =
            CompletableFuture.supplyAsync(
                factoryMonthPlanProductionFinalResultService::findLastTwelveMonthProdFinalPlan);

        // 合并所有结果
        return CompletableFuture.allOf(
                monthlySaleQtyFuture, materialsFuture, cycleSchStruConfFuture,
                stocksFuture, salesOrderFuture, planFuture)
            .thenApply(v -> {
                try {
                    Map<String, MpMonthlySaleQty> sku2AverageSaleQty =
                        sku2AverageSaleQty(monthlySaleQtyFuture.get());

                    Map<String, MdmMaterialInfo> sku2StructureMap =
                        sku2Structure(materialsFuture.get());

                    Map<String, Integer> structure2TurnoverMonthMap =
                        structure2TurnoverMonth(cycleSchStruConfFuture.get());

                    Map<String, Long> stockMap =
                        convertToGroupedSumStockQtyMap(stocksFuture.get());

                    Map<String, Long> saleOrderMap =
                        convertToGroupedSumOrderQtyMap(salesOrderFuture.get());

                    Map<String, List<MdmProductStock>> finishedProductStockMap =
                        getFinishedProductMap(stocksFuture.get());

                    Map<String, Integer> countSkuMap =
                        countSkuMap(planFuture.get());

                    return new CalculationData(
                        sku2AverageSaleQty,
                        sku2StructureMap,
                        structure2TurnoverMonthMap,
                        stockMap,
                        saleOrderMap,
                        finishedProductStockMap,
                        countSkuMap
                    );
                } catch (Exception e) {
                    throw new RuntimeException("Failed to prepare calculation data", e);
                }
            }).join();
    }

    /**
     * 获取符合条件的SKU集合
     */
    private Set<String> getEligibleSkus() {
        // 2.1 获取所有有效物料
        List<MdmMaterialInfo> materialInfos = getAllActiveMaterials();
        if (CollectionUtils.isEmpty(materialInfos)) {
            return Collections.emptySet();
        }

        // 2.2 根据结构筛选SKU
        Set<String> structureSkus = filterSkusByStructure(materialInfos);
        if (CollectionUtils.isEmpty(structureSkus)) {
            return Collections.emptySet();
        }

        // 2.3 排除超期SKU
        return excludeOverdueCycleProduction(structureSkus);
    }

    /**
     * 排除超期SKU
     */
    private Set<String> excludeOverdueCycleProduction(Set<String> skus) {
        Set<String> overdueSkus = overdueSkuService.excludeOverdueCycleProduction();
        if (CollectionUtils.isEmpty(overdueSkus)) {
            return skus;
        }
        return skus.stream()
            .filter(sku -> !overdueSkus.contains(sku))
            .collect(Collectors.toSet());
    }

    /**
     * 根据结构筛选SKU
     */
    private Set<String> filterSkusByStructure(List<MdmMaterialInfo> materialInfos) {
        // 获取有效的结构名称集合
        Set<String> validStructures = mdmCycleSchStruConfService.findCycleSchStruConf()
            .stream()
            .map(MdmCycleSchStruConf::getStructureName)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());

        return materialInfos.stream()
            .filter(material -> validStructures.contains(material.getStructureName()))
            .map(MdmMaterialInfo::getMaterialCode)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());
    }

    /**
     * 获取所有活跃的物料信息
     */
    private List<MdmMaterialInfo> getAllActiveMaterials() {
        LambdaQueryWrapper<MdmMaterialInfo> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MdmMaterialInfo::getIsDelete, YesOrNoEnum.NO.getValue());
        return mdmMaterialInfoEntityMapper.selectList(wrapper);
    }

    /**
     * 验证所有必要的前置条件
     */
    private void validatePrerequisites() {
        // 1.1 验证周期性排产结构配置
        List<MdmCycleSchStruConf> cycleSchStruConfs =
            mdmCycleSchStruConfService.findCycleSchStruConf();

        if (CollectionUtils.isEmpty(cycleSchStruConfs)) {
            throw new BusinessException(I18nUtil.getMessage(
                "ui.message.createCycleStockUp.notExist.cycleProductionStructureConfig"));
        }

        Set<String> validStructures = cycleSchStruConfs.stream()
            .map(MdmCycleSchStruConf::getStructureName)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());

        if (CollectionUtils.isEmpty(validStructures)) {
            throw new BusinessException(I18nUtil.getMessage(
                "ui.message.createCycleStockUp.notExist.cycleProductionStructureConfig"));
        }
    }

    @Override
    public void createPrecedentStockUp(SupplyOrderPool supplyOrderPool) {
        try {
            PrecedentStockUpContext context = buildContext();
            Set<String> eligibleSkus = findEligibleSkus(context);
            if (eligibleSkus.isEmpty()) {
                log.warn("No eligible SKUs found for precedent stock up");
                throw new BusinessException(I18nUtil.getMessage("ui.message.createPrecedentStockUp.notExist.precedentStockUpMaterial"));
            }
            List<SupplyOrderPool> supplyOrderPools = calculateAndBuildOrders(eligibleSkus, context);
            // 3.1 清理旧数据
            deleteSupplyOrderPool(SupplyOrderTypeEnum.PRECEDENT_STOCK.getCode());
            if (CollectionUtils.isNotEmpty(supplyOrderPools)) {
                this.baseDao.insertBatch(supplyOrderPools);
            }
            log.info("Successfully created {} precedent stock up orders", supplyOrderPools.size());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create precedent stock up,errorMsg:{}", e.getMessage());
        }
    }

    private List<SupplyOrderPool> calculateAndBuildOrders(
        Set<String> eligibleSkus, PrecedentStockUpContext context) {
        // 4、计算常规储备SKU的排产量： (周转天数/30) * 月均销量 - 无订单库存 无订单库存 = 成品库存 - 销售订单池提报量(需结合年周号、动平衡、均匀性)；
        List<MpMonthlySaleQty> monthlySaleQtyList =   monthlySaleQtyService.findCurrentMonthlySaleQty();
        Map<String,MpMonthlySaleQty> monthlySaleQtyMap = sku2AverageSaleQty(monthlySaleQtyList);
        List<MdmProductStock> finishedProductStocks = this.mdmProductStockService.findCurrentFinishStock();
        Map<String,List<MdmProductStock>> stockMap  = this.getFinishedProductMap(finishedProductStocks);
        List<SalesOrderPool> salesOrderPools = this.salesOrderPoolService.findCurrentSalesOrderPool();
        Map<String, Long> salesOrderMap = this.convertToGroupedSumOrderQtyMap(salesOrderPools);
        List<FactoryMonthPlanProductionFinalResult> factoryMonthPlanProdFinals = this.factoryMonthPlanProductionFinalResultService.findLastTwelveMonthProdFinalPlan();
        Map<String,Integer> productionCountMap = this.countSkuMap(factoryMonthPlanProdFinals);
        return eligibleSkus.parallelStream()
            .map(sku -> calculateOrderForSku(sku, context, monthlySaleQtyMap,
                stockMap, salesOrderMap, productionCountMap))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private SupplyOrderPool calculateOrderForSku(
        String materialCode,
        PrecedentStockUpContext context,
        Map<String, MpMonthlySaleQty> monthlySaleQtyMap,
        Map<String, List<MdmProductStock>> stockMap,
        Map<String, Long> salesOrderMap,
        Map<String, Integer> productionCountMap) {

        MdmMaterialInfo materialInfo = context.getMaterialCodeToInfoMap().get(materialCode);
        if (materialInfo == null) {
            log.warn("Material info not found for SKU: {}", materialCode);
            return null;
        }

        MpMonthlySaleQty monthlySaleQty = monthlySaleQtyMap.get(materialCode);
        if (monthlySaleQty == null || monthlySaleQty.getAverageSaleQty() <= 0) {
            return null;
        }
        long stockWithoutOrder = calculateStockWithoutOrder(materialCode,
            stockMap,
            salesOrderMap);
        BigDecimal productionQty = calculateProductionQty(
            monthlySaleQty.getAverageSaleQty(),
            context.getTurnOverDays(),
            stockWithoutOrder
        );

        if (productionQty.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        SupplyOrderPool entity = new SupplyOrderPool();
        // SKU的周期性排产量：月均销量 * 周转月数 - 无订单库存
        entity.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        entity.setOrderType(SupplyOrderTypeEnum.PRECEDENT_STOCK.getCode());
        entity.setMaterialCode(materialCode);
        entity.setBrand(materialInfo.getBrand());
        // 获取当前年月
        YearMonth nextYearMonth = YearMonth.now().plusMonths(1);
        entity.setYear(nextYearMonth.getYear());
        entity.setMonth(nextYearMonth.getMonthValue());
        entity.setLocationType(materialInfo.getCommonType());
        entity.setMaterialDesc(materialInfo.getMaterialDesc());
        entity.setProductCategory(materialInfo.getProductCategory());
        entity.setProductTypeCode(materialInfo.getProductTypeCode());
        // 7、从月均销量表中取得近3个月月均销量、近6个月月均销量、近12个月的发货频次、适销区域
        Long  passThreeMonthSaleQty = monthlySaleQty.getPassThreeMonthSaleQty();
        Long    passSixMonthSaleQty = monthlySaleQty.getPassSixMonthSaleQty();
        Integer   deliveryFrequency = monthlySaleQty.getDeliveryFrequency();
        String    saleArea = monthlySaleQty.getSaleArea();
        long stockLimit = BigDecimalUtils.multiply(context.getTurnOverDays(),BigDecimal.valueOf(monthlySaleQty.getAverageSaleQty()))
                .divideToIntegralValue(BigDecimal.valueOf(30)).longValue();
        entity.setStockLimit(stockLimit);
        entity.setQty(productionQty.longValue());
        entity.setBaseVale(null);
        entity.setIsDelete(YesOrNoEnum.NO.getValue());
        long threeOverdueStockQty = 0;
        long sixOverdueStockQty = 0;
        long nightOverdueStockQty = 0;
        long twelveOverdueStockQty = 0;
        // 6、查询成品库存表，汇总计算超期12个月的库存数、超期6个月的库存数、超期3个月的库存数
        if(!org.springframework.util.CollectionUtils.isEmpty(stockMap)) {
            for(Map.Entry<String, List<MdmProductStock>> entry : stockMap.entrySet()) {
                if(entry.getKey().contains(materialCode)) {
                    threeOverdueStockQty = entry.getValue().stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedThreeMonth())).mapToLong(MdmProductStock::getStockQty).sum();
                    sixOverdueStockQty = entry.getValue().stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedSixMonth())).mapToLong(MdmProductStock::getStockQty).sum();
                    nightOverdueStockQty = entry.getValue().stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedNineMonth())).mapToLong(MdmProductStock::getStockQty).sum();
                    twelveOverdueStockQty  = entry.getValue().stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedTwelveMonth())).mapToLong(MdmProductStock::getStockQty).sum();
                }
            }
        }
        entity.setThreeOverdueStockQty(threeOverdueStockQty);
        entity.setSixOverdueStockQty(sixOverdueStockQty);
        entity.setNightOverdueStockQty(nightOverdueStockQty);
        entity.setTwelveOverdueStockQty(twelveOverdueStockQty);
        // 7、从月均销量表中取得近3个月月均销量、近6个月月均销量、近12个月的发货频次、适销区域
        entity.setThreeAverageQty(passThreeMonthSaleQty);
        entity.setSixAverageQty(passSixMonthSaleQty);
        entity.setDeliveryFrequency(deliveryFrequency);
        entity.setSaleArea(saleArea);
        // 8、12个月结构上机频次 = 从月度排产计划，获取近12个月的已排产的月份个数
        entity.setStructureFrequency(productionCountMap.getOrDefault(materialCode,0));
        return entity;
    }

    /**
     *  排产量 = (周转天数/30) * 月均销量 - 无订单库存
     * @param monthlyAverageSale 月均销量
     * @param turnOverDays 周转天数
     * @param stockWithoutOrder 无订单库存
     * @return 排产量
     */
    private BigDecimal calculateProductionQty(
        long monthlyAverageSale,
        BigDecimal turnOverDays,
        long stockWithoutOrder) {
        // 排产量 = (周转天数/30) * 月均销量 - 无订单库存
        return BigDecimal.valueOf(monthlyAverageSale).multiply(turnOverDays)
            .subtract(BigDecimal.valueOf(stockWithoutOrder))
            .setScale(0, RoundingMode.HALF_UP);
    }

    private long calculateStockWithoutOrder(String materialCode, Map<String, List<MdmProductStock>> stockMap, Map<String, Long> salesOrderMap) {
        long notOrderStockQty = BigDecimal.ZERO.longValue();
        if(org.springframework.util.CollectionUtils.isEmpty(stockMap)) {
            return notOrderStockQty;
        }
        for(Map.Entry<String,List<MdmProductStock>> entry:stockMap.entrySet()) {
            if(entry.getKey().contains(materialCode)) {
                long totalStockQty = entry.getValue().stream().filter(stock -> null != stock.getStockQty()).mapToLong(MdmProductStock::getStockQty).sum();
                long saleOrderQty = salesOrderMap.getOrDefault(entry.getKey(), 0L);
                notOrderStockQty += totalStockQty - saleOrderQty;
            }
        }
        // 无订单库存 = 成品库存 - 销售订单池提报量
        return notOrderStockQty;
    }

    private Set<String> findEligibleSkus(PrecedentStockUpContext context) {
        // 1. 获取不在周期排产结构配置表中的SKU
        Set<String> skusExcludingStructure = filterSkusExcludingStructure(context);
        if (skusExcludingStructure.isEmpty()) {
            return Collections.emptySet();
        }
        // 2. 获取近12个月销售活跃的SKU
        Set<String> activeSalesSkus = historySaleRecordService.findSkuInLastTwelveMonth();
        if (activeSalesSkus.isEmpty()) {
            return Collections.emptySet();
        }
        // 3. 取交集
        Set<String> intersectedSkus = SetUtils.intersection(skusExcludingStructure, activeSalesSkus);
        if (intersectedSkus.isEmpty()) {
            return Collections.emptySet();
        }
        // 4. 排除超期SKU
        return excludeOverduePrecedentProduction(intersectedSkus);
    }

    private Set<String> excludeOverduePrecedentProduction(Set<String> intersectedSkus) {
        Set<String> overdueSkus = overdueSkuService.excludeOverduePrecedentProduction();
        if (CollectionUtils.isEmpty(overdueSkus)) {
            return intersectedSkus;
        }
        return intersectedSkus.stream()
            .filter(sku -> !overdueSkus.contains(sku))
            .collect(Collectors.toSet());
    }

    private Set<String> filterSkusExcludingStructure(PrecedentStockUpContext context) {
        return context.getMaterialInfos().stream()
            .filter(material -> !context.getStructureNames().contains(material.getStructureName()))
            .map(MdmMaterialInfo::getMaterialCode)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private PrecedentStockUpContext buildContext() {
        List<MdmMaterialInfo> materialInfos = findAllActive();
        List<MdmCycleSchStruConf> cycleSchStruConfs = mdmCycleSchStruConfService.findCycleSchStruConf();

        return PrecedentStockUpContext.builder()
            .materialInfos(materialInfos)
            .cycleSchStruConfs(cycleSchStruConfs)
            .structureNames(extractStructureNames(cycleSchStruConfs))
            .materialCodeToInfoMap(buildMaterialCodeMap(materialInfos))
            .turnOverDays(getTurnOverDays())
            .build();
    }

    private Map<String, MdmMaterialInfo> buildMaterialCodeMap(List<MdmMaterialInfo> materialInfos) {
        return materialInfos.stream()
            .collect(Collectors.toMap(
                MdmMaterialInfo::getMaterialCode,
                Function.identity(),
                (existing, replacement) -> existing
            ));
    }

    private Set<String> extractStructureNames(List<MdmCycleSchStruConf> cycleSchStruConfs) {
        if (CollectionUtils.isEmpty(cycleSchStruConfs)) {
            return Collections.emptySet();
        }
        return cycleSchStruConfs.stream()
            .map(MdmCycleSchStruConf::getStructureName)
            .collect(Collectors.toSet());
    }

    private List<MdmMaterialInfo> findAllActive() {
        LambdaQueryWrapper<MdmMaterialInfo> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MdmMaterialInfo::getIsDelete, YesOrNoEnum.NO.getValue());
        return  mdmMaterialInfoEntityMapper.selectList(wrapper);
    }


    @Override
    public SupplyOrderPool queryRelationByMaterialCode(SupplyOrderPool supplyOrderPool) {
        LambdaQueryWrapper<MdmMaterialInfo> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MdmMaterialInfo::getMaterialCode, supplyOrderPool.getMaterialCode());
        wrapper.eq(MdmMaterialInfo::getIsDelete, YesOrNoEnum.NO.getValue());
        List<MdmMaterialInfo>  materialInfos =   mdmMaterialInfoEntityMapper.selectList(wrapper);
        if(CollectionUtils.isEmpty(materialInfos)){
            throw new BusinessException(I18nUtil.getMessage("ui.message.supplyOrderPool.notFound.materialInfo"));
        }
        // (1)通过物料表，带出物料描述、品牌、产品品类
        MdmMaterialInfo materialInfo = materialInfos.get(0);
        // 获取当前年月 2、工厂：默认116；年-月：当前系统日所在年月；内外销：默认外销；
        supplyOrderPool.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        YearMonth now = YearMonth.now();
        supplyOrderPool.setYear(now.getYear());
        supplyOrderPool.setMonth(now.getMonthValue());
        supplyOrderPool.setLocationType(materialInfo.getCommonType());
        supplyOrderPool.setMaterialDesc(materialInfo.getMaterialDesc());
        supplyOrderPool.setBrand(materialInfo.getBrand());
        supplyOrderPool.setProductTypeCode(materialInfo.getProductTypeCode());
        supplyOrderPool.setProductCategory(materialInfo.getProductCategory());
        // (2)通过月均销量表，带出近3个月月均销量、近6个月月均销量、近12个月的发货频次、适销区域、备库上限/月均销量 * 30 = 30（天）
        MpMonthlySaleQty monthlySaleQty =   monthlySaleQtyService.getMpMonthlySaleQtyByMaterialCode(supplyOrderPool.getMaterialCode());
        if(null != monthlySaleQty) {
            supplyOrderPool.setThreeAverageQty(monthlySaleQty.getPassThreeMonthSaleQty());
            supplyOrderPool.setSixAverageQty(monthlySaleQty.getPassSixMonthSaleQty());
            supplyOrderPool.setDeliveryFrequency(monthlySaleQty.getDeliveryFrequency());
            supplyOrderPool.setSaleArea(monthlySaleQty.getSaleArea());
            supplyOrderPool.setAverageSaleQty(monthlySaleQty.getAverageSaleQty());
            // 周转天数
            BigDecimal turnOverDays = this.getTurnOverDays();
            long stockLimit = BigDecimalUtils.multiply(turnOverDays,BigDecimal.valueOf(monthlySaleQty.getAverageSaleQty()))
                .divideToIntegralValue(BigDecimal.valueOf(30)).longValue();
            supplyOrderPool.setStockLimit(stockLimit);
        }
        //   (3)通过成品库存表，获取超期12个月的库存数、超期6个月的库存数、超期3个月的库存数
        List<MdmProductStock> finishedProductStocks = this.mdmProductStockService.getMpFinishedProductStockByMaterialCode(supplyOrderPool.getMaterialCode());
        if(CollectionUtils.isNotEmpty(finishedProductStocks)) {
            long threeOverdueStockQty = finishedProductStocks.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedThreeMonth())).mapToLong(MdmProductStock::getStockQty).sum();
            long sixOverdueStockQty = finishedProductStocks.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedSixMonth())).mapToLong(MdmProductStock::getStockQty).sum();
            long nightOverdueStockQty = finishedProductStocks.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedNineMonth())).mapToLong(MdmProductStock::getStockQty).sum();
            long twelveOverdueStockQty  = finishedProductStocks.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedTwelveMonth())).mapToLong(MdmProductStock::getStockQty).sum();
            supplyOrderPool.setThreeOverdueStockQty(threeOverdueStockQty);
            supplyOrderPool.setSixOverdueStockQty(sixOverdueStockQty);
            supplyOrderPool.setNightOverdueStockQty(nightOverdueStockQty);
            supplyOrderPool.setTwelveOverdueStockQty(twelveOverdueStockQty);
        }
        //通过月度生产计划表，获取近12个月有排产的月份个数
        // 8、12个月结构上机频次 = 从定稿的月度排产计划，获取近12个月的已排产的月份个数
        int  productionMonth = this.factoryMonthPlanProductionFinalResultService.getProductionMonthInLastTwelveMonth(supplyOrderPool.getMaterialCode());
        supplyOrderPool.setStructureFrequency(productionMonth);
        return supplyOrderPool;
    }

    @Override
    public List<SupplyOrderPool> findCurrentSupplyOrderPool() {
        YearMonth yearMonth = YearMonth.now();
        LambdaQueryWrapper<SupplyOrderPool> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SupplyOrderPool::getIsDelete, YesOrNoEnum.NO.getValue());
        wrapper.eq(SupplyOrderPool::getYear, yearMonth.getYear());
        wrapper.eq(SupplyOrderPool::getMonth, yearMonth.getMonthValue());
        return this.supplyOrderPoolEntityMapper.selectList(wrapper);
    }

    @Override
    public List<SupplyOrderPool> createCycleStockUp() {
        // 1.1 验证周期性排产结构配置
        List<MdmCycleSchStruConf> cycleSchStruConfs =
            mdmCycleSchStruConfService.findCycleSchStruConf();
        if (CollectionUtils.isEmpty(cycleSchStruConfs)) {
           return Collections.emptyList();
        }
        Set<String> validStructures = cycleSchStruConfs.stream()
            .map(MdmCycleSchStruConf::getStructureName)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(validStructures)) {
            return Collections.emptyList();
        }
        // 2. 获取需要处理的SKU集合
        Set<String> eligibleSkus = getEligibleSkus();
        // 3.2 准备计算所需数据
        CalculationData calculationData = prepareCalculationData();
        // 3.3 批量构建
        return buildSupplyOrderPoolsInParallel(
            eligibleSkus, calculationData);
    }

    @Override
    public List<SupplyOrderPool> createPrecedentStockUp() {
        PrecedentStockUpContext context = buildContext();
        Set<String> eligibleSkus = findEligibleSkus(context);
        if (eligibleSkus.isEmpty()) {
            return Collections.emptyList();
        }
        return calculateAndBuildOrders(eligibleSkus, context);
    }

    /**
     * 获取配置信息
     *
     * @return 周转天数
     */
    private BigDecimal getTurnOverDays() {
        FactoryParam factoryParam = new FactoryParam();
        factoryParam.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        factoryParam.setParamCode(MonthPlanEnums.TURN_OVER_DAYS.getCode());
        factoryParam.setProductTypeCode(ProductTypeEnum.WHOLE_STEEL.getValue());
        FactoryParam param = iFactoryParamService.getFacParamSingle(factoryParam);
        String paramValue;
        if (param == null) {
          return BigDecimal.ZERO;
        }
        paramValue = StringUtils.isNotEmpty(param.getParamValue()) ? param.getParamValue() : param.getDefauleValue();
        return BigDecimalUtils.valueOf(paramValue);
    }

    /**
     *  删除
     */
    private void deleteSupplyOrderPool(String orderType) {
        SupplyOrderPool param = new SupplyOrderPool();
        param.setOrderType(orderType);
        // 获取当前年月
        YearMonth nextYearMonth = YearMonth.now().plusMonths(1);
        LambdaQueryWrapper<SupplyOrderPool> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SupplyOrderPool::getIsDelete, YesOrNoEnum.NO.getValue());
        wrapper.eq(SupplyOrderPool::getYear, nextYearMonth.getYear());
        wrapper.eq(SupplyOrderPool::getMonth, nextYearMonth.getMonthValue());
        wrapper.eq(SupplyOrderPool::getOrderType, orderType);
        this.supplyOrderPoolEntityMapper.delete(wrapper);
    }


    private Map<String, Integer> countSkuMap(List<FactoryMonthPlanProductionFinalResult> factoryMonthPlanProdFinals) {
        if (CollectionUtils.isEmpty(factoryMonthPlanProdFinals)) {
            return Collections.emptyMap();
        }

        return factoryMonthPlanProdFinals.stream()
            .filter(Objects::nonNull)
            .filter(item -> item.getMaterialCode() != null)
            .filter(item -> item.getYearMonth() != null)
            .collect(Collectors.groupingBy(
                FactoryMonthPlanProductionFinalResult::getMaterialCode,
                Collectors.mapping(
                    FactoryMonthPlanProductionFinalResult::getYearMonth,
                    Collectors.collectingAndThen(
                        Collectors.toSet(),
                        Set::size
                    )
                )
            ));
    }


    private Map<String, Long> convertToGroupedSumOrderQtyMap(List<SalesOrderPool> salesOrderPools) {
        if (CollectionUtils.isEmpty(salesOrderPools)) {
            return Collections.emptyMap();
        }

        return salesOrderPools.stream()
            .filter(Objects::nonNull)
            .filter(salesOrder -> salesOrder.getOriMaterialCode() != null && salesOrder.getWeekYear() != null)
            .collect(Collectors.groupingBy(
                salesOrder -> createCompositeKey(
                    salesOrder.getOriMaterialCode(),
                    salesOrder.getWeekYear(),
                    salesOrder.getIsDynamicBalance(),
                    salesOrder.getIsUniformity()
                ),
                Collectors.summingLong(item -> item.getOrdQty().longValue())
            ));
    }

    private Map<String,List<MdmProductStock>> getFinishedProductMap(List<MdmProductStock> finishedProductStocks) {
        if (CollectionUtils.isEmpty(finishedProductStocks)) {
            return Collections.emptyMap();
        }
        return finishedProductStocks.stream()
            .filter(Objects::nonNull)
            .filter(monthlySaleQty -> StringUtils.isNotBlank(monthlySaleQty.getMaterialCode()))
            .collect(Collectors.groupingBy(
                stock -> createCompositeKey(
                    stock.getMaterialCode(),
                    stock.getWeekYear(),
                    stock.getIsDynamicBalance(),
                    stock.getIsUniformity()
                )));
    }

    public Map<String, Long> convertToGroupedSumStockQtyMap(List<MdmProductStock> finishedProductStocks) {
        if (CollectionUtils.isEmpty(finishedProductStocks)) {
            return Collections.emptyMap();
        }

        return finishedProductStocks.stream()
            .filter(Objects::nonNull)
            .filter(stock -> stock.getMaterialCode() != null && stock.getWeekYear() != null)
            .collect(Collectors.groupingBy(
                stock -> createCompositeKey(
                    stock.getMaterialCode(),
                    stock.getWeekYear(),
                    stock.getIsDynamicBalance(),
                    stock.getIsUniformity()
                ),
                Collectors.summingLong(MdmProductStock::getStockQty)
            ));
    }

    private String createCompositeKey(String materialCode, String weekYear,
                                      String dynamicBalance, String uniformity) {
        // 使用分隔符连接四个维度，确保唯一性
        // 使用特殊分隔符避免与内容冲突
        return String.join("|",
            Optional.ofNullable(materialCode).orElse(""),
            Optional.ofNullable(weekYear).orElse(""),
            Optional.ofNullable(dynamicBalance).orElse(""),
            Optional.ofNullable(uniformity).orElse("")
        );
    }

    private Map<String, MpMonthlySaleQty> sku2AverageSaleQty(List<MpMonthlySaleQty> monthlySaleQtyList) {
        if (CollectionUtils.isEmpty(monthlySaleQtyList)) {
            return Collections.emptyMap();
        }
        return monthlySaleQtyList.stream()
            .filter(Objects::nonNull)
            .filter(monthlySaleQty -> StringUtils.isNotBlank(monthlySaleQty.getMaterialCode()))
            .collect(Collectors.toMap(
                MpMonthlySaleQty::getMaterialCode,
                monthlySaleQty -> monthlySaleQty,
                (existing, replacement) -> existing
            ));
    }

    private SupplyOrderPool buildSupplyOrderPool(String materialCode,CalculationData data) {

        // 2. 构建基础订单信息
        SupplyOrderPool order = buildBaseOrderInfo(materialCode,data);
        // 3. 计算销售相关数据
        calculateSalesMetrics(order, data);

        // 4. 计算库存相关数据
        calculateStockMetrics(order, data);

        // 5. 计算最终订单数量
        calculateFinalQuantity(order, data);

        return order;
    }

    /**
     * 计算最终订单数量
     */
    private void calculateFinalQuantity(SupplyOrderPool order, CalculationData data) {
        MpMonthlySaleQty monthlySaleQty = data.getSku2AverageSaleQty().get(order.getMaterialCode());
        int turnoverMonth = getTurnoverMonth(order.getStructureName(),data);

        if (monthlySaleQty != null && turnoverMonth > 0) {
            long averageSaleQty = monthlySaleQty.getAverageSaleQty();
            long notOrderStockQty = order.getNotOrderStockQty();
            // 周期性排产量 = 月均销量 × 周转月数 - 无订单库存
            long quantity = (averageSaleQty * turnoverMonth) - notOrderStockQty;
            order.setQty(Math.max(0, quantity));
        } else {
            order.setQty(0L);
        }
        // 设置结构上机频次
        int structureFrequency = data.getCountSkuMap()
            .getOrDefault(order.getMaterialCode(), 0);
        order.setStructureFrequency(structureFrequency);
    }

    /**
     * 获取周转月数
     */
    private int getTurnoverMonth(String structureName, CalculationData data) {
        if(StringUtils.isBlank(structureName) || org.springframework.util.CollectionUtils.isEmpty(data.getStructure2TurnoverMonthMap())) {
            return BigDecimal.ZERO.intValue();
        }
        return  data.getStructure2TurnoverMonthMap().getOrDefault(structureName,BigDecimal.ZERO.intValue());
    }

    /**
     * 计算库存相关指标
     */
    private void calculateStockMetrics(SupplyOrderPool order, CalculationData data) {
        // 计算无订单库存
        long notOrderStockQty = calculateNotOrderStockQty(order.getMaterialCode(),data);
        Map<String, List<MdmProductStock>> finishedProductStockMap =   data.getFinishedProductStockMap();
        long threeOverdueStockQty = 0;
        long sixOverdueStockQty = 0;
        long nightOverdueStockQty = 0;
        long twelveOverdueStockQty = 0;
        if(!org.springframework.util.CollectionUtils.isEmpty(finishedProductStockMap)) {
            for(Map.Entry<String,List<MdmProductStock>> entry: finishedProductStockMap.entrySet()) {
                if(entry.getKey().contains(order.getMaterialCode())) {
                    threeOverdueStockQty = entry.getValue().stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedThreeMonth())).mapToLong(MdmProductStock::getStockQty).sum();
                    sixOverdueStockQty =   entry.getValue().stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedSixMonth())).mapToLong(MdmProductStock::getStockQty).sum();
                    nightOverdueStockQty =  entry.getValue().stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedNineMonth())).mapToLong(MdmProductStock::getStockQty).sum();
                    twelveOverdueStockQty  = entry.getValue().stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedTwelveMonth())).mapToLong(MdmProductStock::getStockQty).sum();
                }
            }
        }
        order.setThreeOverdueStockQty(threeOverdueStockQty);
        order.setSixOverdueStockQty(sixOverdueStockQty);
        order.setNightOverdueStockQty(nightOverdueStockQty);
        order.setTwelveOverdueStockQty(twelveOverdueStockQty);
        order.setNotOrderStockQty(notOrderStockQty);
    }

    /**
     * 计算无订单库存
     */
    private long calculateNotOrderStockQty(String materialCode,CalculationData data) {
        long notOrderStockQty = BigDecimal.ZERO.longValue();
        if(org.springframework.util.CollectionUtils.isEmpty(data.getStockMap())) {
            return notOrderStockQty;
        }
        for(Map.Entry<String,Long> entry:data.getStockMap().entrySet()) {
            if(entry.getKey().contains(materialCode)) {
                long saleOrderQty = data.getSaleOrderMap().getOrDefault(entry.getKey(), 0L);
                notOrderStockQty += entry.getValue() - saleOrderQty;
            }
        }
        // 无订单库存 = 成品库存 - 销售订单池提报量
        return notOrderStockQty;
    }

    /**
     * 计算销售相关指标
     */
    private void calculateSalesMetrics(SupplyOrderPool order, CalculationData data) {
        MpMonthlySaleQty monthlySaleQty = data.getSku2AverageSaleQty().get(order.getMaterialCode());
        if (monthlySaleQty != null) {
            order.setThreeAverageQty(monthlySaleQty.getPassThreeMonthSaleQty());
            order.setSixAverageQty(monthlySaleQty.getPassSixMonthSaleQty());
            order.setDeliveryFrequency(monthlySaleQty.getDeliveryFrequency());
            order.setSaleArea(monthlySaleQty.getSaleArea());
            // 计算备库上限 备库上限值 = 周转天数(全局参数) * 月均销量 / 30
            // 5、计算备库上限：备库上限/月均销量 * 30 = 30（天）注：第1个30，月度天数（固定）；第2个30，周转天数（可配置）；月均销量（6个月）。
            BigDecimal stockLimit = calculateStockLimit(monthlySaleQty);
            order.setStockLimit(stockLimit.longValue());
        }
    }

    /**
     * 计算备库上限值
     */
    private BigDecimal calculateStockLimit(MpMonthlySaleQty monthlySaleQty) {
        BigDecimal turnoverDays = getTurnOverDays();
        return turnoverDays.multiply(BigDecimal.valueOf(monthlySaleQty.getAverageSaleQty()))
            .divide(BigDecimal.valueOf(DAYS_PER_MONTH), 0, RoundingMode.HALF_UP);
    }

    /**
     * 构建基础订单信息
     */
    private SupplyOrderPool buildBaseOrderInfo(String materialCode,CalculationData data) {
        YearMonth nextMonth = YearMonth.now().plusMonths(1);

        SupplyOrderPool supplyOrderPool =  new SupplyOrderPool();
        supplyOrderPool.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        supplyOrderPool.setOrderType(SupplyOrderTypeEnum.CYCLE_PRODUCTION_STOCK.getCode());
        supplyOrderPool.setYear(nextMonth.getYear());
        supplyOrderPool.setMonth(nextMonth.getMonthValue());
        supplyOrderPool.setIsDelete(YesOrNoEnum.NO.getValue());
        supplyOrderPool.setBaseVale(null);
        MdmMaterialInfo  materialInfo = data.getSku2StructureMap().get(materialCode);
        if(null != materialInfo) {
            supplyOrderPool.setMaterialCode(materialCode);
            supplyOrderPool.setMaterialDesc(materialInfo.getMaterialDesc());
            supplyOrderPool.setBrand(materialInfo.getBrand());
            supplyOrderPool.setStructureName(materialInfo.getStructureName());
            supplyOrderPool.setProductCategory(materialInfo.getProductCategory());
            supplyOrderPool.setProductTypeCode(materialInfo.getProductTypeCode());
            supplyOrderPool.setLocationType(materialInfo.getCommonType());
        }
        return supplyOrderPool;
    }


    public Map<String, MdmMaterialInfo> sku2Structure(List<MdmMaterialInfo> materialList) {
        if (CollectionUtils.isEmpty(materialList)) {
            return Collections.emptyMap();
        }
        return materialList.stream()
            .filter(Objects::nonNull)
            .filter(material -> StringUtils.isNotBlank(material.getMaterialCode()))
            .collect(Collectors.toMap(MdmMaterialInfo::getMaterialCode,
                material -> material,
                (existing, replacement) -> existing
            ));
    }

    public Map<String, Integer> structure2TurnoverMonth(List<MdmCycleSchStruConf> cycleSchStruConfs) {
        if (CollectionUtils.isEmpty(cycleSchStruConfs)) {
            return Collections.emptyMap();
        }
        return cycleSchStruConfs.stream()
            .filter(Objects::nonNull)
            .filter(cycleSchStruConf -> StringUtils.isNotBlank(cycleSchStruConf.getStructureName()))
            .collect(Collectors.toMap(
                MdmCycleSchStruConf::getStructureName,
                MdmCycleSchStruConf::getTurnoverMonth,
                (existing, replacement) -> existing
            ));
    }

    /**
     * 计算数据容器类
     */
    @Builder
    @Getter
    private static class CalculationData {
        private final Map<String, MpMonthlySaleQty> sku2AverageSaleQty;
        private final Map<String, MdmMaterialInfo> sku2StructureMap;
        private final Map<String, Integer> structure2TurnoverMonthMap;
        private final Map<String, Long> stockMap;
        private final Map<String, Long> saleOrderMap;
        private final Map<String, List<MdmProductStock>> finishedProductStockMap;
        private final Map<String, Integer> countSkuMap;
    }

    // ====================== 上下文对象 ======================
    @Data
    @Builder
    private static  class PrecedentStockUpContext {
        private List<MdmMaterialInfo> materialInfos;
        private List<MdmCycleSchStruConf> cycleSchStruConfs;
        private Set<String> structureNames;
        private Map<String, MdmMaterialInfo> materialCodeToInfoMap;
        private BigDecimal turnOverDays;
    }
}
