package com.zlt.aps.monthplan.common.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.enums.ProductTypeEnum;
import com.zlt.aps.enums.ProductionPlanType;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.service.IMdmCycleSchStruConfService;
import com.zlt.aps.maindata.service.IMdmMaterialInfoService;
import com.zlt.aps.maindata.service.IMdmProductStockService;
import com.zlt.aps.maindata.service.IMpHistorySaleRecordService;
import com.zlt.aps.maindata.service.IMpMonthlySaleQtyService;
import com.zlt.aps.maindata.service.IMpOverdueSkuService;
import com.zlt.aps.monthplan.api.domain.entity.FactoryParam;
import com.zlt.aps.monthplan.api.domain.entity.MdmCycleSchStruConf;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductStock;
import com.zlt.aps.monthplan.api.domain.entity.MpMonthlySaleQty;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import com.zlt.aps.monthplan.api.domain.entity.SupplyOrderPool;
import com.zlt.aps.monthplan.demand.mapper.SupplyOrderPoolEntityMapper;
import com.zlt.aps.monthplan.demand.service.ISalesOrderPoolService;
import com.zlt.aps.monthplan.enums.SupplyOrderTypeEnum;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProductionFinalResultService;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 常规排产储备
 * @author Yelq
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrecedentStockUpService {
  private static final String DEFAULT_FACTORY_CODE = FactoryConstant.DEFAULT_FACTORY_CODE;

  private static final int DAYS_PER_MONTH = 30;

  // 注入的依赖服务
  private final SupplyOrderPoolEntityMapper supplyOrderPoolEntityMapper;

  private final IMdmCycleSchStruConfService mdmCycleSchStruConfService;
  // 物料信息
  private final IMdmMaterialInfoService materialInfoService;
  // 月均销量
  private final IMpMonthlySaleQtyService monthlySaleQtyService;
  // 成品库存
  private final IMdmProductStockService mdmProductStockService;
  // 销售订单
  private final ISalesOrderPoolService salesOrderPoolService;
  // 定稿的月度排产计划
  private final IFactoryMonthPlanProductionFinalResultService factoryMonthPlanProductionFinalResultService;
  // 超期SKU
  private final IMpOverdueSkuService overdueSkuService;
  private final StockAllocationService stockAllocationService;
  // 排产设定
  private final IFactoryParamService factoryParamService;
  // 历史销售记录
  private final IMpHistorySaleRecordService historySaleRecordService;

  private final BaseDao baseDao;

  // 自定义线程池，避免使用默认的ForkJoinPool
  private final Executor ioExecutor;
  // 批量插入处理器
  private final BatchInsertProcessor<SupplyOrderPool> batchInsertProcessor;

  /**
   * 创建周期性备货
   *
   * @param supplyOrderPool 供应链订单池
   */
  @Transactional(rollbackFor = Exception.class)
  public List<SupplyOrderPool> createPrecedentStockUp(SupplyOrderPool supplyOrderPool,boolean validateFlag) {
    log.info("开始创建常规备货, 工厂编码: {}", supplyOrderPool.getFactoryCode());
    try {
      if(validateFlag) {
        // 1. 准备基础数据
        YearMonth nextMonth = YearMonth.now().plusMonths(1);
        supplyOrderPool.setFactoryCode(resolveFactoryCode(supplyOrderPool));
        supplyOrderPool.setYear(nextMonth.getYear());
        supplyOrderPool.setMonth(nextMonth.getMonthValue());
        supplyOrderPool.setSourceType(ProductionPlanType.NORMAL.getPlanType());
      }
      PrecedentStockUpContext context = buildContext(supplyOrderPool);
      // 2. 验证前置条件
      Set<String> eligibleSkus = findEligibleSkus(context);
      if (CollectionUtils.isEmpty(eligibleSkus)) {
        log.warn("有效SKU为空, 工厂: {}, 年: {}, 月: {}", supplyOrderPool.getFactoryCode(), supplyOrderPool.getYear(),supplyOrderPool.getMonth());
        if(validateFlag) {
          throw new BusinessException(I18nUtil.getMessage("ui.message.createPrecedentStockUp.notExist.precedentStockUpMaterial"));
        }
        return Collections.emptyList();
      }
      // 4. 重新创建供应链订单池
      return recreateSupplyOrderPoolsForCycleStockUp(supplyOrderPool, eligibleSkus,context);
    } catch (BusinessException e) {
      log.error("业务异常导致常规备货创建失败", e);
      throw e;
    } catch (Exception e) {
      log.error("常规备货创建失败", e);
      throw new BusinessException("常规备货创建失败: " + e.getMessage(), e);
    }
  }

  private PrecedentStockUpContext buildContext(SupplyOrderPool supplyOrderPool) {
    log.info("开始准备计算数据, 工厂: {}, 年: {}, 月: {}", supplyOrderPool.getFactoryCode(), supplyOrderPool.getYear(),supplyOrderPool.getMonth());
    try {
      CompletableFuture<List<MdmMaterialInfo>> materialsFuture =
          executeAsync(() -> this.materialInfoService.findMaterialInfo(supplyOrderPool.getFactoryCode()),
              "物料信息");
      CompletableFuture<Integer> monthSaleQtyMonthsFuture =
          executeAsync(this::getMonthSaleQtyMonths,
              "查询近12个月的月均销量大于零的月份数");
      CompletableFuture<List<MdmCycleSchStruConf>> cycleSchStruConfFuture =
          executeAsync(() -> mdmCycleSchStruConfService.findCycleSchStruConf(supplyOrderPool.getFactoryCode()),
              "月周期结构配置");
      // 合并所有结果
      return CompletableFuture.allOf(
               materialsFuture,
              monthSaleQtyMonthsFuture, cycleSchStruConfFuture)
          .thenApply(v -> {
            try {
              List<MdmMaterialInfo> materialInfos = materialsFuture.get();
              Integer monthSaleQtyMonths = monthSaleQtyMonthsFuture.get();
              List<MdmCycleSchStruConf> cycleSchStruConfs = cycleSchStruConfFuture.get();
              return PrecedentStockUpContext.builder()
                  .materialInfos(materialInfos)
                  .cycleSchStruConfs(cycleSchStruConfs)
                  .structureNames(extractStructureNames(cycleSchStruConfs))
                  .materialCodeToInfoMap(buildMaterialCodeMap(materialInfos))
                  .turnOverDays(getTurnOverDays())
                  .months(monthSaleQtyMonths)
                  .build();
            } catch (Exception e) {
              log.error("准备计算数据失败", e);
              throw new BusinessException("准备计算数据失败: " + e.getMessage(), e);
            }
          }).join();
    } catch (Exception e) {
      log.error("准备计算数据失败", e);
      throw new BusinessException("准备计算数据失败: " + e.getMessage(), e);
    }
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

  /**
   * 获取配置信息
   *
   * @return 周转天数
   */
  private int getMonthSaleQtyMonths() {
    FactoryParam factoryParam = new FactoryParam();
    factoryParam.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
    factoryParam.setParamCode(MonthPlanEnums.MONTH_SALE_QTY_MONTH.getCode());
    factoryParam.setProductTypeCode(ProductTypeEnum.WHOLE_STEEL.getValue());
    FactoryParam param = factoryParamService.getFacParamSingle(factoryParam);
    String paramValue;
    if (param == null) {
      return BigDecimal.ZERO.intValue();
    }
    paramValue = StringUtils.isNotEmpty(param.getParamValue()) ? param.getParamValue() : param.getDefauleValue();
    return Integer.parseInt(paramValue);
  }

  /**
   * 重新创建供应链订单池
   */
  private List<SupplyOrderPool>  recreateSupplyOrderPoolsForCycleStockUp(
      SupplyOrderPool supplyOrderPool,
      Set<String> skus,PrecedentStockUpContext context) {
    log.info("开始重新创建供应链订单池, 工厂: {}, 年: {}, 月: {}, SKU数量: {}", supplyOrderPool.getFactoryCode(), supplyOrderPool.getYear(),supplyOrderPool.getMonth(), skus.size());
    try {
      // 3.1 清理旧数据（使用物理删除或逻辑删除）
      safeDeleteByCondition(supplyOrderPool);
      // 3.2 准备计算所需数据
      CalculationData calculationData = prepareCalculationData(supplyOrderPool, skus);
      // 3.3 批量构建并插入订单池数据
      List<SupplyOrderPool> supplyOrderPools = buildSupplyOrderPools(supplyOrderPool, skus,context, calculationData);
      List<SupplyOrderPool> filterSupplyOrderPools = null;
      if(!CollectionUtils.isEmpty(supplyOrderPools)) {
        filterSupplyOrderPools  = supplyOrderPools.stream().filter(item -> null !=item.getQty() && item.getQty() > 0).collect(Collectors.toList());
      }
      if(!CollectionUtils.isEmpty(filterSupplyOrderPools)) {
        filterSupplyOrderPools.sort(Comparator.comparing(SupplyOrderPool::getMaterialCode));
        this.batchInsertProcessor.batchInsert(supplyOrderPools);
      }
      return filterSupplyOrderPools;
    } catch (Exception e) {
      log.error("重新创建供应链订单池失败", e);
      throw new BusinessException("重新创建供应链订单池失败: " + e.getMessage(), e);
    }
  }

  /**
   *  准备计算所需的所有数据
   * @param supplyOrderPool 参数
   * @param skus 月周期排产结构SKU
   * @return 准备的数据
   */
  private CalculationData prepareCalculationData(SupplyOrderPool supplyOrderPool, Set<String> skus) {
    log.info("开始准备计算数据, 工厂: {}, 年: {}, 月: {}, SKU数量: {}", supplyOrderPool.getFactoryCode(), supplyOrderPool.getYear(),supplyOrderPool.getMonth(), skus.size());
    try {
      // 使用并行处理，但控制超时时间
      CompletableFuture<List<MpMonthlySaleQty>> monthlySaleQtyFuture =
          executeAsync(() -> monthlySaleQtyService.findCurrentMonthlySaleQty(
                  supplyOrderPool.getFactoryCode(), skus),
              "月度销量数据");
      CompletableFuture<List<MdmMaterialInfo>> materialsFuture =
          executeAsync(() -> materialInfoService.findMaterialInfo(
                  supplyOrderPool.getFactoryCode(), skus),
              "物料信息");

      CompletableFuture<List<MdmProductStock>> stocksFuture =
          executeAsync(() -> mdmProductStockService.findCurrentFinishStock(
                  supplyOrderPool.getFactoryCode(), skus),
              "成品库存");

      CompletableFuture<List<SalesOrderPool>> salesOrderFuture =
          executeAsync(() -> salesOrderPoolService.findCurrentSalesOrderPool(
                  supplyOrderPool.getFactoryCode(), skus),
              "销售订单池");

      CompletableFuture<Map<String, Integer>> calculateStructureFrequencyFuture =
          executeAsync(() -> factoryMonthPlanProductionFinalResultService.calculateStructureFrequency(
                  supplyOrderPool.getFactoryCode(), skus),
              "结构频率计算");

      // 合并所有结果
      return CompletableFuture.allOf(
              monthlySaleQtyFuture, materialsFuture,
              stocksFuture, salesOrderFuture, calculateStructureFrequencyFuture)
          .thenApply(v -> {
            try {
              List<MpMonthlySaleQty> monthlySaleQties =  monthlySaleQtyFuture.get();
              List<MdmMaterialInfo> materials = materialsFuture.get();
              List<MdmProductStock> productStocks = stocksFuture.get();
              List<SalesOrderPool> salesOrderPools = salesOrderFuture.get();
              Map<String,Integer> structureFrequency = calculateStructureFrequencyFuture.get();
              Map<String, MpMonthlySaleQty> sku2AverageSaleQty = sku2AverageSaleQty(monthlySaleQties);
              Map<String, MdmMaterialInfo> sku2StructureMap = sku2Structure(materials);
              Map<String,List<MdmProductStock>> stockMap = this.getProductStockMapGroupByMaterialCode(productStocks);
              Map<String,Integer> stockWithoutOrderMap = calculateStockWithoutOrder(productStocks,salesOrderPools);
              return new CalculationData(
                  sku2AverageSaleQty,
                  sku2StructureMap,
                  stockMap,
                  stockWithoutOrderMap,
                  structureFrequency
              );
            } catch (Exception e) {
              log.error("准备计算数据失败", e);
              throw new BusinessException("准备计算数据失败: " + e.getMessage(), e);
            }
          }).join();
    } catch (Exception e) {
      log.error("准备计算数据失败", e);
      throw new BusinessException("准备计算数据失败: " + e.getMessage(), e);
    }
  }

  private Map<String, Integer> calculateStockWithoutOrder(List<MdmProductStock> finishedProductStocks, List<SalesOrderPool> salesOrderPools) {
    if(CollectionUtils.isEmpty(finishedProductStocks)){
      return Collections.emptyMap();
    }
    // 20260110 修改原来是完全匹配年周，物料，动平衡，均匀性，现在改为物料满足, 年周满足即可, 动平衡，均匀性属于优先扣减，不满足时，再扣减其他库存
    return stockAllocationService.calculateStockWithoutOrder(finishedProductStocks,salesOrderPools);
  }

  private Map<String, List<MdmProductStock>> getProductStockMapGroupByMaterialCode(List<MdmProductStock> finishedProductStocks) {
    if(CollectionUtils.isEmpty(finishedProductStocks)) {
      return Collections.emptyMap();
    }
    return  finishedProductStocks.stream().filter(item -> StringUtils.isNotBlank(item.getMaterialCode())).collect(Collectors.groupingBy(MdmProductStock::getMaterialCode));
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


  private List<SupplyOrderPool> buildSupplyOrderPools(SupplyOrderPool supplyOrderPool, Set<String> skus,PrecedentStockUpContext context, CalculationData calculationData) {
    return skus.stream()
        .map(sku -> buildSupplyOrderPool(supplyOrderPool,
            sku,
            context,
            calculationData))
        .collect(Collectors.toList());
  }

  private SupplyOrderPool buildSupplyOrderPool(SupplyOrderPool supplyOrderPool, String materialCode,PrecedentStockUpContext context, CalculationData calculationData) {
    // 1. 构建基础订单信息
    SupplyOrderPool order = buildBaseOrderInfo(supplyOrderPool,materialCode,calculationData);
    // 2. 计算销售相关数据
    calculateSalesMetrics(order,context,calculationData);
    // 3. 计算库存相关数据
    calculateStockMetrics(order, calculationData);
    // 4. 计算最终订单数量
    calculateFinalQuantity(order,context, calculationData);
    return order;
  }

  private SupplyOrderPool buildBaseOrderInfo(SupplyOrderPool param, String materialCode, CalculationData calculationData) {
    SupplyOrderPool supplyOrderPool =  new SupplyOrderPool();
    supplyOrderPool.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
    supplyOrderPool.setOrderType(SupplyOrderTypeEnum.PRECEDENT_STOCK.getCode());
    supplyOrderPool.setYear(param.getYear());
    supplyOrderPool.setMonth(param.getMonth());
    supplyOrderPool.setIsDelete(YesOrNoEnum.NO.getValue());
    supplyOrderPool.setId(null);
    supplyOrderPool.setBaseVale(null);
    supplyOrderPool.setSourceType(param.getSourceType());
    supplyOrderPool.setPredictionVersion(param.getPredictionVersion());
    MdmMaterialInfo  materialInfo = calculationData.getSku2StructureMap().get(materialCode);
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

  /**
   * 计算最终订单数量
   */
  private void calculateFinalQuantity(SupplyOrderPool order,PrecedentStockUpContext context, CalculationData data) {
    // 设置结构上机频次
    int structureFrequency = data.getCountSkuMap()
        .getOrDefault(order.getMaterialCode(), 0);
    order.setStructureFrequency(structureFrequency);
    MpMonthlySaleQty monthlySaleQty = data.getSku2AverageSaleQty().get(order.getMaterialCode());
    if(null == monthlySaleQty) {
      order.setQty(BigDecimal.ZERO.intValue());
      return;
    }
    order.setSaleArea(StringUtils.isNotBlank(monthlySaleQty.getSaleArea())?monthlySaleQty.getSaleArea():StringUtils.EMPTY);
    // 排产量 = (周转天数/30) * 月均销量 - 无订单库存
    BigDecimal productionQty = calculateProductionQty(
        monthlySaleQty.getAverageSaleQty(),
        context.getTurnOverDays(),
        order.getNotOrderStockQty()
    );
    order.setQty(Math.max(0, productionQty.intValue()));
  }

  /**
   *  排产量 = (周转天数/30) * 月均销量 - 无订单库存
   * @param monthlyAverageSale 月均销量
   * @param turnOverDays 周转天数
   * @param stockWithoutOrder 无订单库存
   * @return 排产量
   */
  private BigDecimal calculateProductionQty(
      Integer monthlyAverageSale,
      BigDecimal turnOverDays,
      long stockWithoutOrder) {
    if(null == turnOverDays || null == monthlyAverageSale) {
      return BigDecimal.ZERO;
    }
    // 排产量 = (周转天数/30) * 月均销量 - 无订单库存
    return turnOverDays.divide(BigDecimal.valueOf(DAYS_PER_MONTH),0,RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(monthlyAverageSale))
        .subtract(BigDecimal.valueOf(stockWithoutOrder));
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
  private void calculateSalesMetrics(SupplyOrderPool order, PrecedentStockUpContext context,CalculationData data) {
    MpMonthlySaleQty monthlySaleQty = data.getSku2AverageSaleQty().get(order.getMaterialCode());
    if (monthlySaleQty != null) {
      order.setThreeAverageQty(monthlySaleQty.getPassThreeMonthSaleQty());
      order.setSixAverageQty(monthlySaleQty.getPassSixMonthSaleQty());
      order.setDeliveryFrequency(monthlySaleQty.getDeliveryFrequency());
      order.setSaleArea(monthlySaleQty.getSaleArea());
      // 计算备库上限 备库上限值 = 周转天数(全局参数) * 月均销量 / 30
      // 5、计算备库上限：备库上限/月均销量 * 30 = 30（天）注：第1个30，月度天数（固定）；第2个30，周转天数（可配置）；月均销量（6个月）。
      BigDecimal stockLimit = calculateStockLimit(monthlySaleQty,context);
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
  private BigDecimal calculateStockLimit(MpMonthlySaleQty monthlySaleQty, PrecedentStockUpContext context) {
    BigDecimal turnoverDays = context.turnOverDays;
    if(null == turnoverDays || null == monthlySaleQty.getAverageSaleQty()) {
      return BigDecimal.ZERO;
    }
    return turnoverDays.multiply(BigDecimal.valueOf(monthlySaleQty.getAverageSaleQty()))
        .divide(BigDecimal.valueOf(DAYS_PER_MONTH), 0, RoundingMode.HALF_UP);
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
    FactoryParam param = factoryParamService.getFacParamSingle(factoryParam);
    String paramValue;
    if (param == null) {
      return BigDecimal.ZERO;
    }
    paramValue = StringUtils.isNotEmpty(param.getParamValue()) ? param.getParamValue() : param.getDefauleValue();
    return BigDecimalUtils.valueOf(paramValue);
  }


  /**
   * 执行异步任务
   */
  private <T> CompletableFuture<T> executeAsync(Supplier<T> task, String taskName) {
    return CompletableFuture.supplyAsync(() -> {
      log.debug("开始执行异步任务: {}", taskName);
      long startTime = System.currentTimeMillis();
      try {
        T result = task.get();
        long endTime = System.currentTimeMillis();
        log.debug("异步任务 {} 执行完成, 耗时: {}ms", taskName, endTime - startTime);
        return result;
      } catch (Exception e) {
        log.error("异步任务 {} 执行失败", taskName, e);
        throw new CompletionException(taskName + "任务执行失败", e);
      }
    }, ioExecutor);
  }

  @Transactional(rollbackFor = Exception.class, timeout = 30)
  public void safeDeleteByCondition(SupplyOrderPool supplyOrderPool) throws InterruptedException {
    int totalDeleted = 0;
    int batchSize = 1000;
    boolean hasMore = true;

    while (hasMore) {
      // 1. 先查询要删除的ID（使用索引避免锁表）
      List<Long> idsToDelete = queryIdsForDeletion(supplyOrderPool, batchSize);
      if (idsToDelete.isEmpty()) {
        hasMore = false;
        break;
      }
      // 2. 使用主键批量删除（效率高，锁粒度小）
      int deleted = batchDeleteByIds(idsToDelete);
      totalDeleted += deleted;

      // 3. 记录日志并短暂休眠，释放锁
      log.info("已删除 {} 条记录，累计 {}", deleted, totalDeleted);

      if (deleted < batchSize) {
        hasMore = false;
      } else {
        // 短暂休眠，让其他事务有机会执行
        Thread.sleep(50);
      }
    }
  }

  /**
   * 批量删除方法（使用IN查询，但限制数量）
   */
  private int batchDeleteByIds(List<Long> ids) {
    if (ids.isEmpty()) {
      return 0;
    }

    // 方法1：使用MyBatis-Plus的deleteBatchIds
    return this.baseDao.deleteByIds(SupplyOrderPool.class, ids);

    // 方法2：或者使用自定义SQL（性能更好）
    // return mapper.deleteByIds(ids);
  }

  private List<Long> queryIdsForDeletion(SupplyOrderPool supplyOrderPool, int limit) {
    LambdaQueryWrapper<SupplyOrderPool> queryWrapper = Wrappers.lambdaQuery();
    queryWrapper.select(SupplyOrderPool::getId)
        .eq(SupplyOrderPool::getIsDelete, YesOrNoEnum.NO.getValue())
        .eq(SupplyOrderPool::getYear, supplyOrderPool.getYear())
        .eq(SupplyOrderPool::getMonth, supplyOrderPool.getMonth())
        .eq(SupplyOrderPool::getOrderType, SupplyOrderTypeEnum.PRECEDENT_STOCK.getCode())
        .eq(SupplyOrderPool::getSourceType, supplyOrderPool.getSourceType());
    // 按ID排序，避免死锁
    queryWrapper.orderByAsc(SupplyOrderPool::getId)
        .last("LIMIT " + limit);
    return this.supplyOrderPoolEntityMapper.selectList(queryWrapper)
        .stream()
        .map(SupplyOrderPool::getId)
        .collect(Collectors.toList());
  }


  private Set<String> findEligibleSkus(PrecedentStockUpContext context) {
    // 1. 获取不在周期排产结构配置表中的SKU
    Set<String> skusExcludingStructure = filterSkusExcludingStructure(context);
    if (skusExcludingStructure.isEmpty()) {
      return Collections.emptySet();
    }
    // 2. 获取近12个月销售活跃的SKU
    Set<String> activeSalesSkus = historySaleRecordService.findSkuInLastTwelveMonth(context.getMonths());
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

  /**
   * 解析工厂编码
   */
  private String resolveFactoryCode(SupplyOrderPool supplyOrderPool) {
    String factoryCode = supplyOrderPool.getFactoryCode();
    return StringUtils.isNotBlank(factoryCode) ? factoryCode : DEFAULT_FACTORY_CODE;
  }

  public void validateEnableCreate(SupplyOrderPool supplyOrderPool) {
    MdmMaterialInfo  materialInfo = materialInfoService.getMaterialInfoByMaterialCode(supplyOrderPool.getFactoryCode(),supplyOrderPool.getMaterialCode());
    if(null == materialInfo){
      throw new BusinessException(I18nUtil.getMessage("ui.message.supplyOrderPool.notFound.materialInfo"));
    }
    if(StringUtils.isBlank(materialInfo.getStructureName())) {
      return;
    }
    List<MdmCycleSchStruConf>  monCycleSchStruConfs =   mdmCycleSchStruConfService.findCycleSchStruConf(supplyOrderPool.getFactoryCode());
    if(CollectionUtils.isEmpty(monCycleSchStruConfs)) {
      return;
    }
    Set<String> structureNames = monCycleSchStruConfs.stream().map(MdmCycleSchStruConf::getStructureName).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
    if(CollectionUtils.isEmpty(structureNames)) {
      return;
    }
    if(structureNames.contains(materialInfo.getStructureName())) {
      throw new BusinessException(I18nUtil.getMessage("ui.message.createPrecedentStockUp.notExist.precedentStockUpMaterial"));
    }
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
    private Integer months;
  }

  /**
   * 计算数据容器类
   */
  @Builder
  @Getter
  private static class CalculationData {
    private final Map<String, MpMonthlySaleQty> sku2AverageSaleQty;
    private final Map<String, MdmMaterialInfo> sku2StructureMap;
    private final  Map<String,List<MdmProductStock>> stockMap;
    private final Map<String,Integer> stockWithoutOrderMap;
    private final Map<String, Integer> countSkuMap;
  }
}
