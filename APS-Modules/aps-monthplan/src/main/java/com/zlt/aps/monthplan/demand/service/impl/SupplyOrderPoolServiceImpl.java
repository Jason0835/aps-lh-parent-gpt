package com.zlt.aps.monthplan.demand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.exception.BusinessException;
import com.tlt.aps.utils.JsonI18nConvertUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.service.*;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.api.domain.vo.AreaConvertVo;
import com.zlt.aps.monthplan.demand.mapper.SupplyOrderPoolEntityMapper;
import com.zlt.aps.monthplan.demand.service.ISalesOrderPoolService;
import com.zlt.aps.monthplan.demand.service.ISupplyOrderPoolService;
import com.zlt.aps.monthplan.enums.SupplyOrderTypeEnum;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProductionFinalResultService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.exception.QueryExprException;
import com.zlt.core.queryformulas.QueryFormulaUtil;
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

import javax.annotation.Resource;
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

    private final IMdmMonCycleSchStruConfService mdmMonCycleSchStruConfService;
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
    // 库存冲减
    @Resource
    private  StockAllocationServiceImpl stockAllocationServiceImpl;

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
        //  (1).根据SKU、订单类型进行唯一性校验，如果存在，提示信息"xxx物料的周期排产/常规储备已经存在，请确认"，系统不做处理
        //  (2). 根据选择的储备类型校验近12个月是否出现过超期周期排产储备/超期常规储备，如果出现过，则提示信息“近12个月有出现过超期胎，不可新增”
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            String notUniqueMsg =  com.ruoyi.common.utils.StringUtils.format(I18nUtil.getMessage("ui.data.alert.supplyOrderPool.notUnique"),docEntityVO.getMaterialCode());
            throw new BusinessException(notUniqueMsg);
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "materialCode","year","month"));
    }

    @Override
    public void createCycleStockUp(SupplyOrderPool supplyOrderPool) {
        YearMonth nextMonth = YearMonth.now().plusMonths(1);
        // 1. 验证前置条件
        validatePrerequisites(nextMonth);

        // 2. 获取需要处理的SKU集合
        Set<String> eligibleSkus = getEligibleSkus(nextMonth);
        if (CollectionUtils.isEmpty(eligibleSkus)) {
            throw new BusinessException(I18nUtil.getMessage(
                "ui.message.createCycleStockUp.notExist.cycleStockUpMaterial"));
        }

        // 3. 清理旧数据并批量创建新数据
        recreateSupplyOrderPools(nextMonth,eligibleSkus);
    }

    /**
     * 重新创建供应链订单池
     */
    private List<SupplyOrderPool> recreateSupplyOrderPools(YearMonth yearMonth,Set<String> skus) {
        // 3.1 清理旧数据
        deleteSupplyOrderPool(yearMonth,SupplyOrderTypeEnum.CYCLE_PRODUCTION_STOCK.getCode());
        // 3.2 准备计算所需数据
        CalculationData calculationData = prepareCalculationData(yearMonth);

        // 3.3 批量构建并插入订单池数据
        List<SupplyOrderPool> supplyOrderPools = buildSupplyOrderPoolsInParallel(yearMonth,
            skus, calculationData);
        if (CollectionUtils.isNotEmpty(supplyOrderPools)) {
            this.baseDao.insertBatch(supplyOrderPools);
        }
        return supplyOrderPools;
    }

    /**
     * 并行构建供应链订单池
     */
    private List<SupplyOrderPool> buildSupplyOrderPoolsInParallel(YearMonth yearMonth,
        Set<String> skus, CalculationData data) {

        return skus.stream()
            .map(sku -> buildSupplyOrderPool(yearMonth,
                sku,
                data))
            .collect(Collectors.toList());
    }

    /**
     * 准备计算所需的所有数据
     */
    private CalculationData prepareCalculationData(YearMonth yearMonth) {
        // 并行获取所有必要数据
        CompletableFuture<List<MpMonthlySaleQty>> monthlySaleQtyFuture =
            CompletableFuture.supplyAsync(monthlySaleQtyService::findCurrentMonthlySaleQty);

        CompletableFuture<List<MdmMaterialInfo>> materialsFuture =
            CompletableFuture.supplyAsync(this::getAllActiveMaterials);
        CompletableFuture<List<MdmMonCycleSchStruConf>> cycleSchStruConfFuture =
            CompletableFuture.supplyAsync(() -> mdmMonCycleSchStruConfService.findCurrentCycleSchStruConf(yearMonth));

        CompletableFuture<List<MdmProductStock>> stocksFuture =
            CompletableFuture.supplyAsync(mdmProductStockService::findCurrentFinishStock);

        CompletableFuture<List<SalesOrderPool>> salesOrderFuture =
            CompletableFuture.supplyAsync(salesOrderPoolService::findCurrentSalesOrderPool);

        CompletableFuture<Map<String,Integer>> calculateStructureFrequencyFuture =
            CompletableFuture.supplyAsync(
                factoryMonthPlanProductionFinalResultService::calculateStructureFrequency);

        // 合并所有结果
        return CompletableFuture.allOf(
                monthlySaleQtyFuture, materialsFuture, cycleSchStruConfFuture,
                stocksFuture, salesOrderFuture, calculateStructureFrequencyFuture)
            .thenApply(v -> {
                try {
                    List<MpMonthlySaleQty> monthlySaleQties =  monthlySaleQtyFuture.get();
                    List<MdmMaterialInfo> materials = materialsFuture.get();
                    List<MdmMonCycleSchStruConf> cycleSchStruConfs = cycleSchStruConfFuture.get();
                    List<MdmProductStock> productStocks = stocksFuture.get();
                    List<SalesOrderPool> salesOrderPools = salesOrderFuture.get();
                    Map<String,Integer> structureFrequency = calculateStructureFrequencyFuture.get();

                    Map<String, MpMonthlySaleQty> sku2AverageSaleQty = sku2AverageSaleQty(monthlySaleQties);

                    Map<String, MdmMaterialInfo> sku2StructureMap = sku2Structure(materials);

                    Map<String, Integer> structure2TurnoverMonthMap = structure2TurnoverMonth(cycleSchStruConfs);
                    Map<String,List<MdmProductStock>> stockMap = this.getProductStockMapGroupByMaterialCode(productStocks);
                    Map<String,Integer> stockWithoutOrderMap = calculateStockWithoutOrder(productStocks,salesOrderPools);

                    return new CalculationData(
                        sku2AverageSaleQty,
                        sku2StructureMap,
                        structure2TurnoverMonthMap,
                        stockMap,
                        stockWithoutOrderMap,
                        structureFrequency
                    );
                } catch (Exception e) {
                    throw new RuntimeException("Failed to prepare calculation data", e);
                }
            }).join();
    }

    /**
     * 获取符合条件的SKU集合
     */
    private Set<String> getEligibleSkus(YearMonth yearMonth) {
        // 2.1 获取所有有效物料
        List<MdmMaterialInfo> materialInfos = getAllActiveMaterials();
        if (CollectionUtils.isEmpty(materialInfos)) {
            return Collections.emptySet();
        }

        // 2.2 根据结构筛选SKU
        Set<String> structureSkus = filterSkusByStructure(materialInfos,yearMonth);
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
    private Set<String> filterSkusByStructure(List<MdmMaterialInfo> materialInfos,YearMonth yearMonth) {
        List<MdmMonCycleSchStruConf> cycleSchStruConfs =
            this.mdmMonCycleSchStruConfService.findCurrentCycleSchStruConf(yearMonth);
        // 获取有效的结构名称集合
        Set<String> validStructures = cycleSchStruConfs
            .stream()
            .map(MdmMonCycleSchStruConf::getStructureName)
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
    private void validatePrerequisites(YearMonth yearMonth) {
        // 1.1 验证周期性排产结构配置
        List<MdmMonCycleSchStruConf> cycleSchStruConfs =
            this.mdmMonCycleSchStruConfService.findCurrentCycleSchStruConf(yearMonth);

        if (CollectionUtils.isEmpty(cycleSchStruConfs)) {
            throw new BusinessException(I18nUtil.getMessage(
                "ui.message.createCycleStockUp.notExist.cycleProductionStructureConfig"));
        }

        Set<String> validStructures = cycleSchStruConfs.stream()
            .map(MdmMonCycleSchStruConf::getStructureName)
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
            YearMonth nextMonth = YearMonth.now().plusMonths(1);
            PrecedentStockUpContext context = buildContext(nextMonth);
            Set<String> eligibleSkus = findEligibleSkus(context);
            if (eligibleSkus.isEmpty()) {
                log.warn("No eligible SKUs found for precedent stock up");
                throw new BusinessException(I18nUtil.getMessage("ui.message.createPrecedentStockUp.notExist.precedentStockUpMaterial"));
            }
            // 3. 清理旧数据并批量创建新数据
            recreateSupplyOrderPools(nextMonth,eligibleSkus, context);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create precedent stock up,errorMsg:{}", e.getMessage());
        }
    }

    private List<SupplyOrderPool> recreateSupplyOrderPools(YearMonth yearMonth, Set<String> eligibleSkus, PrecedentStockUpContext context) {
        List<SupplyOrderPool> supplyOrderPools = calculateAndBuildOrders(yearMonth,eligibleSkus, context);
        // 3.1 清理旧数据
        deleteSupplyOrderPool(yearMonth,SupplyOrderTypeEnum.PRECEDENT_STOCK.getCode());
        if (CollectionUtils.isNotEmpty(supplyOrderPools)) {
            this.baseDao.insertBatch(supplyOrderPools);
        }
        log.info("Successfully created {} precedent stock up orders", supplyOrderPools.size());
        return supplyOrderPools;
    }

    private List<SupplyOrderPool> calculateAndBuildOrders(YearMonth yearMonth,
        Set<String> eligibleSkus, PrecedentStockUpContext context) {
        // 4、计算常规储备SKU的排产量： (周转天数/30) * 月均销量 - 无订单库存 无订单库存 = 成品库存 - 销售订单池提报量(需结合年周号、动平衡、均匀性)；
        List<MpMonthlySaleQty> monthlySaleQtyList =   monthlySaleQtyService.findCurrentMonthlySaleQty();
        Map<String,MpMonthlySaleQty> monthlySaleQtyMap = sku2AverageSaleQty(monthlySaleQtyList);
        List<MdmProductStock> finishedProductStocks = this.mdmProductStockService.findCurrentFinishStock();
        List<SalesOrderPool> salesOrderPools = this.salesOrderPoolService.findCurrentSalesOrderPool();
        Map<String,Integer> stockWithoutOrderMap = calculateStockWithoutOrder(finishedProductStocks,salesOrderPools);
        Map<String,List<MdmProductStock>> stockMap = this.getProductStockMapGroupByMaterialCode(finishedProductStocks);
        Map<String,Integer> structureFrequency = this.factoryMonthPlanProductionFinalResultService.calculateStructureFrequency();
        return eligibleSkus.parallelStream()
            .map(sku -> calculateOrderForSku(yearMonth,sku, context, monthlySaleQtyMap,stockMap, stockWithoutOrderMap, structureFrequency))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private Map<String, List<MdmProductStock>> getProductStockMapGroupByMaterialCode(List<MdmProductStock> finishedProductStocks) {
        if(CollectionUtils.isEmpty(finishedProductStocks)) {
            return Collections.emptyMap();
        }
        return  finishedProductStocks.stream().filter(item -> StringUtils.isNotBlank(item.getMaterialCode())).collect(Collectors.groupingBy(MdmProductStock::getMaterialCode));
    }

    private Map<String, Integer> calculateStockWithoutOrder(List<MdmProductStock> finishedProductStocks, List<SalesOrderPool> salesOrderPools) {
        if(CollectionUtils.isEmpty(finishedProductStocks)){
            return Collections.emptyMap();
        }

          // 20260110 修改原来是完全匹配年周，物料，动平衡，均匀性，现在改为物料满足, 年周满足即可, 动平衡，均匀性属于优先扣减，不满足时，再扣减其他库存
          return stockAllocationServiceImpl.calculateStockWithoutOrder(finishedProductStocks,salesOrderPools);
    }


    /**
     * 新增周期排产储备时候，输入储备数量的时候，需要加一个提示用户无订单库存有多少，月底计划余量有多少
     * @param supplyOrderPool 入参
     */
    @Override
    public AjaxResult calculateStockMsg(SupplyOrderPool supplyOrderPool) {

        String yearMonth = String.format("%s%02d", supplyOrderPool.getYear(), supplyOrderPool.getMonth());

        int days = YearMonth.of(supplyOrderPool.getYear(), supplyOrderPool.getMonth()).lengthOfMonth();
        // 获取当前年月

        // 1.计算无订单库存
        List<SalesOrderPool> salesOrderPools = this.salesOrderPoolService.findCurrentSalesOrderPool();
        List<MdmProductStock> finishedProductStocks = this.mdmProductStockService.findCurrentFinishStock();
        Map<String, Integer> stockWithoutOrderMap = calculateStockWithoutOrder(finishedProductStocks,salesOrderPools);

        StringBuilder msg = new StringBuilder();
        msg.append(I18nUtil.getMessage("ui.data.column.supplyOrderPool.noOrderQty")).append(stockWithoutOrderMap.get(supplyOrderPool.getMaterialCode()) == null ? 0 : stockWithoutOrderMap.get(supplyOrderPool.getMaterialCode()));

        // 2.计算月底计划余量
        Map<String, Integer> monthSurplusMap = this.factoryMonthPlanProductionFinalResultService.calculateMonthSurplusNoSave(finishedProductStocks, yearMonth, days);
        msg.append(I18nUtil.getMessage("ui.data.column.supplyOrderPool.monthSurplusQty")).append(monthSurplusMap.get(supplyOrderPool.getMaterialCode()) == null ? 0 : monthSurplusMap.get(supplyOrderPool.getMaterialCode()));

        return AjaxResult.success(msg.toString());
    }


    private SupplyOrderPool calculateOrderForSku(
        YearMonth yearMonth,
        String materialCode,
        PrecedentStockUpContext context,
        Map<String, MpMonthlySaleQty> monthlySaleQtyMap,
        Map<String,List<MdmProductStock>> stockMap,
        Map<String,Integer> stockWithoutOrderMap,
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

        int stockWithoutOrder = stockWithoutOrderMap.getOrDefault(materialCode, BigDecimal.ZERO.intValue());
        // 排产量 = (周转天数/30) * 月均销量 - 无订单库存
        BigDecimal productionQty = calculateProductionQty(
            monthlySaleQty.getAverageSaleQty(),
            context.getTurnOverDays(),
            stockWithoutOrder
        );

        SupplyOrderPool entity = new SupplyOrderPool();
        entity.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        entity.setOrderType(SupplyOrderTypeEnum.PRECEDENT_STOCK.getCode());
        entity.setMaterialCode(materialCode);
        entity.setBrand(materialInfo.getBrand());
        entity.setYear(yearMonth.getYear());
        entity.setMonth(yearMonth.getMonthValue());
        entity.setLocationType(materialInfo.getCommonType());
        entity.setMaterialDesc(materialInfo.getMaterialDesc());
        entity.setProductCategory(materialInfo.getProductCategory());
        entity.setProductTypeCode(materialInfo.getProductTypeCode());
        // 7、从月均销量表中取得近3个月月均销量、近6个月月均销量、近12个月的发货频次、适销区域
        Integer  passThreeMonthSaleQty = monthlySaleQty.getPassThreeMonthSaleQty();
        Integer    passSixMonthSaleQty = monthlySaleQty.getPassSixMonthSaleQty();
        Integer   deliveryFrequency = monthlySaleQty.getDeliveryFrequency();
        String    saleArea = monthlySaleQty.getSaleArea();
        BigDecimal stockLimit = calculateStockLimit(monthlySaleQty);
        entity.setStockLimit(stockLimit.intValue());
        entity.setQty(Math.max(0, productionQty.intValue()));
        entity.setBaseVale(null);
        entity.setIsDelete(YesOrNoEnum.NO.getValue());
        int threeOverdueStockQty = 0;
        int sixOverdueStockQty = 0;
        int nightOverdueStockQty = 0;
        int twelveOverdueStockQty = 0;
        // 6、查询成品库存表，汇总计算超期12个月的库存数、超期6个月的库存数、超期3个月的库存数
        if(stockMap.containsKey(materialCode)) {
            List<MdmProductStock> list = stockMap.get(materialCode);
            threeOverdueStockQty = list.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedThreeMonth())).mapToInt(MdmProductStock::getStockQty).sum();
            sixOverdueStockQty = list.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedSixMonth())).mapToInt(MdmProductStock::getStockQty).sum();
            nightOverdueStockQty = list.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedNineMonth())).mapToInt(MdmProductStock::getStockQty).sum();
            twelveOverdueStockQty  = list.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedTwelveMonth())).mapToInt(MdmProductStock::getStockQty).sum();
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
        return turnOverDays.divide(BigDecimal.valueOf(DAYS_PER_MONTH),0,RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(monthlyAverageSale))
            .subtract(BigDecimal.valueOf(stockWithoutOrder));
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

    private PrecedentStockUpContext buildContext(YearMonth yearMonth) {
        List<MdmMaterialInfo> materialInfos = findAllActive();
        List<MdmMonCycleSchStruConf> cycleSchStruConfs = this.mdmMonCycleSchStruConfService.findCurrentCycleSchStruConf(yearMonth);

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

    private Set<String> extractStructureNames(List<MdmMonCycleSchStruConf> cycleSchStruConfs) {
        if (CollectionUtils.isEmpty(cycleSchStruConfs)) {
            return Collections.emptySet();
        }
        return cycleSchStruConfs.stream()
            .map(MdmMonCycleSchStruConf::getStructureName)
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
            supplyOrderPool.setAverageSaleQty(monthlySaleQty.getAverageSaleQty());
            BigDecimal stockLimit = calculateStockLimit(monthlySaleQty);
            supplyOrderPool.setStockLimit(stockLimit.intValue());
            supplyOrderPool.setSaleArea(monthlySaleQty.getSaleArea());
            getSaleAreaByMonthlySaleQty(supplyOrderPool);
        }else{
            supplyOrderPool.setThreeAverageQty(BigDecimal.ZERO.intValue());
            supplyOrderPool.setSixAverageQty(BigDecimal.ZERO.intValue());
            supplyOrderPool.setDeliveryFrequency(BigDecimal.ZERO.intValue());
            supplyOrderPool.setAverageSaleQty(BigDecimal.ZERO.intValue());
            supplyOrderPool.setStockLimit(BigDecimal.ZERO.intValue());
        }
        //   (3)通过成品库存表，获取超期12个月的库存数、超期6个月的库存数、超期3个月的库存数
        List<MdmProductStock> finishedProductStocks = this.mdmProductStockService.getMpFinishedProductStockByMaterialCode(supplyOrderPool.getMaterialCode());
        if(CollectionUtils.isNotEmpty(finishedProductStocks)) {
            int threeOverdueStockQty = finishedProductStocks.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedThreeMonth())).mapToInt(MdmProductStock::getStockQty).sum();
            int sixOverdueStockQty = finishedProductStocks.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedSixMonth())).mapToInt(MdmProductStock::getStockQty).sum();
            int nightOverdueStockQty = finishedProductStocks.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedNineMonth())).mapToInt(MdmProductStock::getStockQty).sum();
            int twelveOverdueStockQty  = finishedProductStocks.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedTwelveMonth())).mapToInt(MdmProductStock::getStockQty).sum();
            supplyOrderPool.setThreeOverdueStockQty(threeOverdueStockQty);
            supplyOrderPool.setSixOverdueStockQty(sixOverdueStockQty);
            supplyOrderPool.setNightOverdueStockQty(nightOverdueStockQty);
            supplyOrderPool.setTwelveOverdueStockQty(twelveOverdueStockQty);
        }else{
            supplyOrderPool.setThreeOverdueStockQty(BigDecimal.ZERO.intValue());
            supplyOrderPool.setSixOverdueStockQty(BigDecimal.ZERO.intValue());
            supplyOrderPool.setNightOverdueStockQty(BigDecimal.ZERO.intValue());
            supplyOrderPool.setTwelveOverdueStockQty(BigDecimal.ZERO.intValue());
        }
        //通过月度生产计划表，获取近12个月有排产的月份个数
        // 8、12个月结构上机频次 = 从定稿的月度排产计划，获取近12个月的已排产的月份个数
        int  productionMonth = this.factoryMonthPlanProductionFinalResultService.calculateStructureFrequency(supplyOrderPool.getMaterialCode());
        supplyOrderPool.setStructureFrequency(productionMonth);
        return supplyOrderPool;
    }

    private void getSaleAreaByMonthlySaleQty(SupplyOrderPool monthlySaleQty) {
        List<SupplyOrderPool> list = new ArrayList<>();
        list.add(monthlySaleQty);
        // 把区域都转成名称
        List<AreaConvertVo> convertVoList = list.stream().map(SupplyOrderPool::getSaleArea)
                .flatMap(item -> Arrays.stream(item.split(",")))
                .distinct()
                .filter(com.ruoyi.common.utils.StringUtils::isNotBlank)
                .map(item -> {
                    AreaConvertVo areaConvertVo = new AreaConvertVo();
                    areaConvertVo.setAreaCode(item);
                    return areaConvertVo;
                })
                .sorted(Comparator.comparing(AreaConvertVo::getAreaCode))
                .collect(Collectors.toList());
        Map<String, String> areaNameMap = getAreaNameMap(convertVoList);
        for (SupplyOrderPool supplyOrderPool : list) {
            String saleArea = supplyOrderPool.getSaleArea();
            String[] areaSplitArr = saleArea.split(",");
            List<String> areaNameList = new ArrayList<>();
            for (String areaCode : areaSplitArr) {
                if (areaNameMap.containsKey(areaCode)) {
                    String name = areaNameMap.get(areaCode);
                    areaNameList.add(name);
                }
            }
            supplyOrderPool.setSaleAreaName(String.join(",", areaNameList));
        }
    }

    private Map<String, String> getAreaNameMap(List<AreaConvertVo> convertVoList) {
        // 执行表达式，转义区域
        try {
            QueryFormulaUtil.execFormula(convertVoList, new String[]{
                    "areaCodeName->getcolvaluewithcondition(t_dp_area, area_name, area_code, areaCode, is_delete = 0)",
            });
        } catch (QueryExprException e) {
            this.logger.error(e.getMessage(), e);
            throw new ServiceException("转换区域，执行查询公式时发生错误.");
        }
        JsonI18nConvertUtils.conventJsonI18n(convertVoList, AreaConvertVo.class);
        return convertVoList.stream().filter(item -> com.ruoyi.common.utils.StringUtils.isNotBlank(item.getAreaCodeNameI18n()))
                .collect(Collectors.toMap(AreaConvertVo::getAreaCode, AreaConvertVo::getAreaCodeNameI18n, (k1, k2) -> k1));
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
    public List<SupplyOrderPool> createCycleStockUp(YearMonth yearMonth) {
        // 1.1 验证周期性排产结构配置
        List<MdmMonCycleSchStruConf> cycleSchStruConfs =
            this.mdmMonCycleSchStruConfService.findCurrentCycleSchStruConf(yearMonth);
        if (CollectionUtils.isEmpty(cycleSchStruConfs)) {
           return Collections.emptyList();
        }
        Set<String> validStructures = cycleSchStruConfs.stream()
            .map(MdmMonCycleSchStruConf::getStructureName)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(validStructures)) {
            return Collections.emptyList();
        }
        // 2. 获取需要处理的SKU集合
        Set<String> eligibleSkus = getEligibleSkus(yearMonth);
        // 3. 清理旧数据并批量创建新数据
        return  recreateSupplyOrderPools(yearMonth,eligibleSkus);
    }

    @Override
    public List<SupplyOrderPool> createPrecedentStockUp(YearMonth yearMonth) {
        PrecedentStockUpContext context = buildContext(yearMonth);
        Set<String> eligibleSkus = findEligibleSkus(context);
        if (eligibleSkus.isEmpty()) {
            return Collections.emptyList();
        }
        return recreateSupplyOrderPools(yearMonth,  eligibleSkus,context);
    }

    @Override
    public AjaxResult checkOverdue(SupplyOrderPool supplyOrderPool) {
        // (2). 根据选择的储备类型校验近12个月是否出现过超期周期排产储备/超期常规储备，如果出现过，则提示信息“近12个月有出现过超期胎，不可新增”
        boolean checkOverDue = overdueSkuService.checkOverdue(supplyOrderPool);
        return checkOverDue?AjaxResult.error(I18nUtil.getMessage("ui.data.alert.supplyOrderPool.overdue")):AjaxResult.success();
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
    private void deleteSupplyOrderPool(YearMonth yearMonth,String orderType) {
        SupplyOrderPool param = new SupplyOrderPool();
        param.setOrderType(orderType);
        LambdaQueryWrapper<SupplyOrderPool> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SupplyOrderPool::getIsDelete, YesOrNoEnum.NO.getValue());
        wrapper.eq(SupplyOrderPool::getYear, yearMonth.getYear());
        wrapper.eq(SupplyOrderPool::getMonth, yearMonth.getMonthValue());
        wrapper.eq(SupplyOrderPool::getOrderType, orderType);
        this.supplyOrderPoolEntityMapper.delete(wrapper);
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

    private SupplyOrderPool buildSupplyOrderPool(YearMonth yearMonth,String materialCode,CalculationData data) {

        // 2. 构建基础订单信息
        SupplyOrderPool order = buildBaseOrderInfo(yearMonth,materialCode,data);
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
        order.setSaleArea(null != monthlySaleQty && StringUtils.isNotBlank(monthlySaleQty.getSaleArea())?monthlySaleQty.getSaleArea():StringUtils.EMPTY);
        int turnoverMonth = getTurnoverMonth(order.getStructureName(),data);

        if (monthlySaleQty != null && turnoverMonth > 0) {
            int averageSaleQty = monthlySaleQty.getAverageSaleQty();
            int notOrderStockQty = order.getNotOrderStockQty();
            // 周期性排产量 = 月均销量 × 周转月数 - 无订单库存
            int quantity = (averageSaleQty * turnoverMonth) - notOrderStockQty;
            order.setQty(Math.max(0, quantity));
        } else {
            order.setQty(0);
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
        int notOrderStockQty = data.getStockWithoutOrderMap().getOrDefault(order.getMaterialCode(), BigDecimal.ZERO.intValue());
        Map<String, List<MdmProductStock>> finishedProductStockMap =   data.getStockMap();
        int threeOverdueStockQty = 0;
        int sixOverdueStockQty = 0;
        int nightOverdueStockQty = 0;
        int twelveOverdueStockQty = 0;
        if(finishedProductStockMap.containsKey(order.getMaterialCode())) {
            List<MdmProductStock> list = finishedProductStockMap.get(order.getMaterialCode());
            threeOverdueStockQty = list.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedThreeMonth())).mapToInt(MdmProductStock::getStockQty).sum();
            sixOverdueStockQty =   list.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedSixMonth())).mapToInt(MdmProductStock::getStockQty).sum();
            nightOverdueStockQty =  list.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedNineMonth())).mapToInt(MdmProductStock::getStockQty).sum();
            twelveOverdueStockQty  = list.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedTwelveMonth())).mapToInt(MdmProductStock::getStockQty).sum();
        }
        order.setThreeOverdueStockQty(threeOverdueStockQty);
        order.setSixOverdueStockQty(sixOverdueStockQty);
        order.setNightOverdueStockQty(nightOverdueStockQty);
        order.setTwelveOverdueStockQty(twelveOverdueStockQty);
        order.setNotOrderStockQty(notOrderStockQty);
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
            order.setStockLimit(stockLimit.intValue());
        }else{
            order.setThreeAverageQty(BigDecimal.ZERO.intValue());
            order.setSixAverageQty(BigDecimal.ZERO.intValue());
            order.setDeliveryFrequency(BigDecimal.ZERO.intValue());
            order.setStockLimit(BigDecimal.ZERO.intValue());
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
    private SupplyOrderPool buildBaseOrderInfo(YearMonth yearMonth,String materialCode,CalculationData data) {
        SupplyOrderPool supplyOrderPool =  new SupplyOrderPool();
        supplyOrderPool.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        supplyOrderPool.setOrderType(SupplyOrderTypeEnum.CYCLE_PRODUCTION_STOCK.getCode());
        supplyOrderPool.setYear(yearMonth.getYear());
        supplyOrderPool.setMonth(yearMonth.getMonthValue());
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

    public Map<String, Integer> structure2TurnoverMonth(List<MdmMonCycleSchStruConf> cycleSchStruConfs) {
        if (CollectionUtils.isEmpty(cycleSchStruConfs)) {
            return Collections.emptyMap();
        }
        return cycleSchStruConfs.stream()
            .filter(Objects::nonNull)
            .filter(cycleSchStruConf -> StringUtils.isNotBlank(cycleSchStruConf.getStructureName()))
            .collect(Collectors.toMap(
                MdmMonCycleSchStruConf::getStructureName,
                MdmMonCycleSchStruConf::getTurnoverMonth,
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
        private final  Map<String,List<MdmProductStock>> stockMap;
        private final Map<String,Integer> stockWithoutOrderMap;
        private final Map<String, Integer> countSkuMap;
    }

    // ====================== 上下文对象 ======================
    @Data
    @Builder
    private static  class PrecedentStockUpContext {
        private List<MdmMaterialInfo> materialInfos;
        private List<MdmMonCycleSchStruConf> cycleSchStruConfs;
        private Set<String> structureNames;
        private Map<String, MdmMaterialInfo> materialCodeToInfoMap;
        private BigDecimal turnOverDays;
    }
}
