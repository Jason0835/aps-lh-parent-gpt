package com.zlt.aps.monthplan.common.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Sets;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.ProductionPlanType;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.service.IMdmMaterialInfoService;
import com.zlt.aps.maindata.service.IMdmMonCycleSchStruConfService;
import com.zlt.aps.maindata.service.IMdmProductStockService;
import com.zlt.aps.maindata.service.IMpMonthlySaleQtyService;
import com.zlt.aps.maindata.service.IMpOverdueSkuService;
import com.zlt.aps.monthplan.api.domain.entity.FactoryParam;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmMonCycleSchStruConf;
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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 周期排产储备
 * @author Yelq
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CycleStockUpService {

  private static final String DEFAULT_FACTORY_CODE = FactoryConstant.DEFAULT_FACTORY_CODE;

  private static final int DAYS_PER_MONTH = 30;

  // 注入的依赖服务
  private final SupplyOrderPoolEntityMapper supplyOrderPoolEntityMapper;

  private final IMdmMonCycleSchStruConfService mdmMonCycleSchStruConfService;
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

  private final BaseDao baseDao;

  // 自定义线程池，避免使用默认的ForkJoinPool
  private final Executor ioExecutor;

  /**
   * 创建周期性备货
   *
   * @param supplyOrderPool 供应链订单池
   */
  @Transactional(rollbackFor = Exception.class)
  public List<SupplyOrderPool> createCycleStockUp(SupplyOrderPool supplyOrderPool,boolean validateFlag) {
    log.info("开始创建周期性备货, 工厂编码: {}", supplyOrderPool.getFactoryCode());
    try {
      if(validateFlag) {
        // 1. 准备基础数据
        YearMonth nextMonth = YearMonth.now().plusMonths(1);
        supplyOrderPool.setFactoryCode(resolveFactoryCode(supplyOrderPool));
        supplyOrderPool.setYear(nextMonth.getYear());
        supplyOrderPool.setMonth(nextMonth.getMonthValue());
        supplyOrderPool.setSourceType(ProductionPlanType.NORMAL.getPlanType());
      }
      // 2. 验证前置条件
      Set<String> validStructures = validatePrerequisites(supplyOrderPool);
      if(CollectionUtils.isEmpty(validStructures)) {
        if(validateFlag) {
          throw new BusinessException(I18nUtil.getMessage("ui.message.createCycleStockUp.notExist.cycleProductionStructureConfig"));
        }
        return Collections.emptyList();
      }
      // 3. 获取符合条件的SKU集合
      Set<String> eligibleSkus = getEligibleSkus(supplyOrderPool.getFactoryCode(), validStructures);
      if (CollectionUtils.isEmpty(eligibleSkus)) {
        if(validateFlag) {
          throw new BusinessException(I18nUtil.getMessage("ui.message.createCycleStockUp.notExist.cycleStockUpMaterial"));
        }
        return Collections.emptyList();
      }
      // 4. 重新创建供应链订单池
      return recreateSupplyOrderPoolsForCycleStockUp(supplyOrderPool, eligibleSkus);
    } catch (BusinessException e) {
      log.error("业务异常导致周期性备货创建失败", e);
      throw e;
    } catch (Exception e) {
      log.error("周期性备货创建失败", e);
      throw new BusinessException("周期性备货创建失败: " + e.getMessage(), e);
    }
  }

  /**
   * 重新创建供应链订单池
   */
  private List<SupplyOrderPool> recreateSupplyOrderPoolsForCycleStockUp(
      SupplyOrderPool supplyOrderPool,
      Set<String> skus) {
    log.info("开始重新创建供应链订单池, 工厂: {}, 年: {}, 月: {}, SKU数量: {}", supplyOrderPool.getFactoryCode(), supplyOrderPool.getYear(),supplyOrderPool.getMonth(), skus.size());
    try {
      // 3.1 清理旧数据（使用物理删除或逻辑删除）
      safeDeleteByCondition(supplyOrderPool);
      // 3.2 准备计算所需数据
      CalculationData calculationData = prepareCalculationData(supplyOrderPool, skus);
      // 3.3 批量构建并插入订单池数据
      List<SupplyOrderPool> supplyOrderPools = buildSupplyOrderPools(supplyOrderPool, skus, calculationData);
      batchInsertSupplyOrderPools(supplyOrderPools);
      return supplyOrderPools;
    } catch (Exception e) {
      log.error("重新创建供应链订单池失败", e);
      throw new BusinessException("重新创建供应链订单池失败: " + e.getMessage(), e);
    }
  }

  private List<SupplyOrderPool> buildSupplyOrderPools(SupplyOrderPool supplyOrderPool, Set<String> skus, CalculationData calculationData) {
    return skus.stream()
        .map(sku -> buildSupplyOrderPool(supplyOrderPool,
            sku,
            calculationData))
        .collect(Collectors.toList());
  }

  private SupplyOrderPool buildSupplyOrderPool(SupplyOrderPool supplyOrderPool, String materialCode, CalculationData calculationData) {
    // 1. 构建基础订单信息
    SupplyOrderPool order = buildBaseOrderInfo(supplyOrderPool,materialCode,calculationData);
    // 2. 计算销售相关数据
    calculateSalesMetrics(order, calculationData);
    // 3. 计算库存相关数据
    calculateStockMetrics(order, calculationData);
    // 4. 计算最终订单数量
    calculateFinalQuantity(order, calculationData);
    return order;
  }

  /**
   * 计算最终订单数量
   */
  private void calculateFinalQuantity(SupplyOrderPool order, CalculationData data) {
    MpMonthlySaleQty monthlySaleQty = data.getSku2AverageSaleQty().get(order.getMaterialCode());
    order.setSaleArea(null != monthlySaleQty && StringUtils.isNotBlank(monthlySaleQty.getSaleArea())?monthlySaleQty.getSaleArea():StringUtils.EMPTY);
    int turnoverMonth = getTurnoverMonth(order.getStructureName(),data);

    if (null  != monthlySaleQty && null != monthlySaleQty.getAverageSaleQty() && turnoverMonth > 0) {
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
    if(StringUtils.isBlank(structureName) || CollectionUtils.isEmpty(data.getStructure2TurnoverMonthMap())) {
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

  private SupplyOrderPool buildBaseOrderInfo(SupplyOrderPool param, String materialCode, CalculationData calculationData) {
    SupplyOrderPool supplyOrderPool =  new SupplyOrderPool();
    supplyOrderPool.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
    supplyOrderPool.setOrderType(SupplyOrderTypeEnum.CYCLE_PRODUCTION_STOCK.getCode());
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

      CompletableFuture<List<MdmMonCycleSchStruConf>> cycleSchStruConfFuture =
          executeAsync(() -> mdmMonCycleSchStruConfService.findCurrentCycleSchStruConf(supplyOrderPool),
              "月周期结构配置");
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
              List<MdmMonCycleSchStruConf> cycleSchStruConfs = cycleSchStruConfFuture.get();
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
              log.error("准备计算数据失败", e);
              throw new BusinessException("准备计算数据失败: " + e.getMessage(), e);
            }
          }).join();
    } catch (Exception e) {
      log.error("准备计算数据失败", e);
      throw new BusinessException("准备计算数据失败: " + e.getMessage(), e);
    }
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
    return stockAllocationService.calculateStockWithoutOrder(finishedProductStocks,salesOrderPools);
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
        .eq(SupplyOrderPool::getOrderType, SupplyOrderTypeEnum.CYCLE_PRODUCTION_STOCK.getCode())
        .eq(SupplyOrderPool::getSourceType, supplyOrderPool.getSourceType());
    // 按ID排序，避免死锁
    queryWrapper.orderByAsc(SupplyOrderPool::getId)
        .last("LIMIT " + limit);
    return this.supplyOrderPoolEntityMapper.selectList(queryWrapper)
        .stream()
        .map(SupplyOrderPool::getId)
        .collect(Collectors.toList());
  }


  /**
   * 批量插入供应链订单池数据
   */
  private void batchInsertSupplyOrderPools(List<SupplyOrderPool> supplyOrderPools) {
    if (CollectionUtils.isEmpty(supplyOrderPools)) {
      log.warn("没有需要插入的供应链订单池数据");
      return;
    }

    log.info("开始批量插入供应链订单池数据, 数量: {}", supplyOrderPools.size());

    // 分批插入，避免单次插入数据量过大
    int batchSize = 1000;
    int total = supplyOrderPools.size();

    for (int i = 0; i < total; i += batchSize) {
      int end = Math.min(i + batchSize, total);
      List<SupplyOrderPool> batch = supplyOrderPools.subList(i, end);
      try {
        this.baseDao.insertBatch(batch);
        log.debug("批量插入进度: {}/{}", end, total);
      } catch (Exception e) {
        log.error("批量插入失败, 批次范围: {}-{}, 错误: {}", i, end, e.getMessage(), e);
        throw new BusinessException("批量插入供应链订单池数据失败", e);
      }
    }

    log.info("批量插入完成, 总记录数: {}", total);
  }

  /**
   * 验证所有必要的前置条件
   */
  private Set<String> validatePrerequisites(SupplyOrderPool supplyOrderPool) {
    log.debug("开始验证周期性排产结构配置, 工厂: {}, 年: {}, 月: {}", supplyOrderPool.getFactoryCode(), supplyOrderPool.getYear(),supplyOrderPool.getMonth());
    List<MdmMonCycleSchStruConf> cycleSchStruConfs = mdmMonCycleSchStruConfService.findCurrentCycleSchStruConf(supplyOrderPool);
    if (CollectionUtils.isEmpty(cycleSchStruConfs)) {
      log.error("未找到周期性排产结构配置, 工厂: {}, 年: {}, 月: {}", supplyOrderPool.getFactoryCode(), supplyOrderPool.getYear(),supplyOrderPool.getMonth());
      return Sets.newHashSet();
    }
    Set<String> validStructures = cycleSchStruConfs.stream()
        .map(MdmMonCycleSchStruConf::getStructureName)
        .filter(StringUtils::isNotBlank)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    if (CollectionUtils.isEmpty(validStructures)) {
      log.error("周期性排产结构配置中的结构名称为空, 工厂: {}, 年: {}, 月: {}", supplyOrderPool.getFactoryCode(), supplyOrderPool.getYear(),supplyOrderPool.getMonth());
      return Sets.newHashSet();
    }
    log.debug("验证通过, 有效结构数量: {}", validStructures.size());
    return validStructures;
  }

  /**
   * 获取符合条件的SKU集合
   */
  private Set<String> getEligibleSkus(String factoryCode, Set<String> validStructures) {
    log.debug("开始获取符合条件的SKU, 工厂: {}, 结构数量: {}", factoryCode, validStructures.size());
    // 2.1 根据结构筛选SKU
    List<MdmMaterialInfo> materialInfos = materialInfoService.findMaterialInfoByStructureNames(factoryCode, validStructures);
    if (CollectionUtils.isEmpty(materialInfos)) {
      log.warn("未找到指定结构的物料信息, 工厂: {}, 结构: {}", factoryCode, validStructures);
      return Collections.emptySet();
    }
    Set<String> structureSkus = materialInfos.stream()
        .map(MdmMaterialInfo::getMaterialCode)
        .filter(StringUtils::isNotBlank)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    if (CollectionUtils.isEmpty(structureSkus)) {
      log.warn("物料信息中的物料编码为空, 工厂: {}", factoryCode);
      return Collections.emptySet();
    }
    log.debug("根据结构筛选到的SKU数量: {}", structureSkus.size());
    // 2.2 排除超期SKU
    Set<String> eligibleSkus = excludeOverdueCycleProduction(structureSkus);
    log.debug("排除超期SKU后剩余数量: {}", eligibleSkus.size());
    return eligibleSkus;
  }

  /**
   * 解析工厂编码
   */
  private String resolveFactoryCode(SupplyOrderPool supplyOrderPool) {
    String factoryCode = supplyOrderPool.getFactoryCode();
    return StringUtils.isNotBlank(factoryCode) ? factoryCode : DEFAULT_FACTORY_CODE;
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

  public void validateEnableCreate(SupplyOrderPool supplyOrderPool) {
    // 2. 验证前置条件
    Set<String> validStructures = validatePrerequisites(supplyOrderPool);
    if(CollectionUtils.isEmpty(validStructures)) {
      throw new BusinessException(I18nUtil.getMessage("ui.message.createCycleStockUp.notExist.cycleProductionStructureConfig"));
    }
    // 3. 获取符合条件的SKU集合
    Set<String> eligibleSkus = getEligibleSkus(supplyOrderPool.getFactoryCode(), validStructures);
    if (CollectionUtils.isEmpty(eligibleSkus)) {
      throw new BusinessException(I18nUtil.getMessage("ui.message.createCycleStockUp.notExist.cycleStockUpMaterial"));
    }
    if(!eligibleSkus.contains(supplyOrderPool.getMaterialCode())) {
      throw new BusinessException(I18nUtil.getMessage("ui.message.createCycleStockUp.notExist.cycleStockUpMaterial"));
    }
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
}
