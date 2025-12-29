package com.zlt.aps.monthplan.factory.helper;

import com.tlt.aps.constant.StringConstant;
import com.tlt.aps.enums.LocationTypeEnum;
import com.tlt.aps.enums.SortHierarchyEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.monthplan.api.domain.dto.ProductStockInfo;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.factory.service.impl.StockUpPlanVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 销售需求计划辅助类
 *
 * @author ZLT
 * @date 20250513
 */
@Slf4j
public class SaleRequirePlanHelper {
  /**
   * 内销自己的备货方式
   */
  private final static String DOMESTIC_STOCK_UP = "Y";

  /**
   * 对月度库存，按分厂+物料维度分组，转换为外销库存及内销库存
   *
   * @param monthStockList
   * @return
   */
  public static Map<String, ProductStockInfo> getProductMonthStock(List<ProductStockMonth> monthStockList) {
    Map<String, ProductStockInfo> stockMap = new HashMap<>();
    if (CollectionUtils.isEmpty(monthStockList)) {
      return stockMap;
    }
    monthStockList.stream().forEach(productionStock -> {
      if (null == productionStock.getStockQty()) {
        productionStock.setStockQty(BigDecimal.ZERO.intValue());
      }
      String key = productionStock.getGroupKey();
      ProductStockInfo stockInfo = stockMap.get(key);
      if (null == stockInfo) {
        stockInfo = new ProductStockInfo();
        stockInfo.setFactoryCode(productionStock.getFactoryCode());
        stockInfo.setProductCode(productionStock.getProductCode());
        stockInfo.setYear(productionStock.getYear());
        stockInfo.setMonth(productionStock.getMonth());
        stockInfo.setDomesticStockQty(BigDecimal.ZERO.longValue());
        stockInfo.setForeignStockQty(BigDecimal.ZERO.longValue());
        stockInfo.setOeStockQty(BigDecimal.ZERO.longValue());
        stockInfo.setLeftOverQty(BigDecimal.ZERO.longValue());
      }
      //库位类别库存
      if (LocationTypeEnum.FOREIGN_LOCATION.getValue().equals(productionStock.getLocationType())) {
        stockInfo.setForeignStockQty(stockInfo.getForeignStockQty() + productionStock.getStockQty());
      } else if (LocationTypeEnum.OE_LOCATION.getValue().equals(productionStock.getLocationType())) {
        stockInfo.setOeStockQty(stockInfo.getOeStockQty() + productionStock.getStockQty());
      } else {
        stockInfo.setDomesticStockQty(stockInfo.getDomesticStockQty() + productionStock.getStockQty());
      }
      stockInfo.setLeftOverQty(stockInfo.getForeignStockQty() + stockInfo.getDomesticStockQty() + stockInfo.getOeStockQty());
      stockMap.put(key, stockInfo);
    });
    return stockMap;
  }

  /**
   * 构建销售需求版本库存信息
   *
   * @param stockReverseMap
   *     对冲后的库存
   * @param monthPlanVersion
   *     销售需求计划版本
   * @return
   */
  public static List<MonthPlanRequireStock> buildRequireStock(Map<String, ProductStockInfo> stockReverseMap, String monthPlanVersion) {
    if (CollectionUtils.isEmpty(stockReverseMap)) {
      return Collections.emptyList();
    }
    List<MonthPlanRequireStock> monthPlanRequireStockList = new ArrayList<>();
    stockReverseMap.entrySet().stream().forEach(entry -> {
      ProductStockInfo productStockInfo = entry.getValue();
      if (null == productStockInfo || StringUtils.isBlank(productStockInfo.getProductCode())) {
        return;
      }
      MonthPlanRequireStock requireStock = buildRequireStock(productStockInfo);
      requireStock.setMonthPlanVersion(monthPlanVersion);
      monthPlanRequireStockList.add(requireStock);
    });
    return monthPlanRequireStockList;
  }

  /**
   * 计算各库位的备货量
   *
   * @param isAddShort
   *     是否包含超欠产
   * @param groupKey
   *     分厂+物料编码
   * @param domesticStockUpType
   *     内销备货类型：Y 表示走内销自己的备货方式
   * @param locationType
   *     库位类型
   * @param needProductQtyMap
   *     净需求量
   * @param locationStockUpQtyMap
   *     库位备货量
   * @param locationExceedShortQtyMap
   *     库位欠产量
   * @param leftOverQty
   *     分厂+物料剩余的总库存
   * @return
   */
  public static StockUpPlanVo calculateLocationTypeStockUpQty(boolean isAddShort,
                                                              String groupKey,
                                                              String domesticStockUpType,
                                                              LocationTypeEnum locationType,
                                                              Map<LocationTypeEnum, Long> needProductQtyMap,
                                                              Map<String, Map<LocationTypeEnum, Long>> locationStockUpQtyMap,
                                                              Map<String, Map<LocationTypeEnum, Long>> locationExceedShortQtyMap,
                                                              Long leftOverQty) {
    //净需求量
    Long needProductQty = needProductQtyMap.get(locationType);
    if (null == needProductQty) {
      return null;
    }
    //备货量
    long stockQty = locationStockUpQtyMap.getOrDefault(groupKey, Collections.emptyMap()).getOrDefault(locationType, 0L);
    //剩余库存量 > 库位备货量，则库位不进行备货，剩余库存量 = 库存量 - 库位备货量，否则 备货 = 备货 - 剩余库存
    if (leftOverQty >= stockQty) {
      leftOverQty = leftOverQty - stockQty;
      stockQty = BigDecimal.ZERO.longValue();
    } else {
      stockQty = stockQty - leftOverQty;
      leftOverQty = BigDecimal.ZERO.longValue();
    }
    //超欠产量
    long shortQty = locationExceedShortQtyMap.getOrDefault(groupKey, Collections.emptyMap()).getOrDefault(locationType, 0L);
    Long planQty = needProductQty;
    //超欠产为负数，故而相当于加上欠产部分
    if (isAddShort) {
      planQty = planQty - shortQty;
    }
    //采用直接加备货方式
    if (!(LocationTypeEnum.DOMESTIC_LOCATION == locationType && DOMESTIC_STOCK_UP.equals(domesticStockUpType))) {
      //加入备货部分
      planQty = planQty + stockQty;
      return new StockUpPlanVo(planQty, stockQty, leftOverQty);
    }
    //内销自己的备货方式，净需求-欠产与备货量比较（净需求-欠产量）>= 理论备货量，不进行备货，销售需求计划=净需求-欠产量
    if (planQty >= stockQty) {
      stockQty = 0;
      return new StockUpPlanVo(planQty, stockQty, leftOverQty);
    }
    // （净需求-欠产量）<理论备货量，上调至理论备货量，实际备货量=理论备货量-（净需求-欠产量），销售需求计划=理论备货量
    long newPlanQty = stockQty;
    stockQty = newPlanQty - planQty;
    return new StockUpPlanVo(newPlanQty, stockQty, leftOverQty);
  }

  /**
   * 未提报的备货计划--抵冲剩余库存
   *
   * @param noSubmitList
   *     未提报的备货计划
   * @param stockReverseMap
   *     库存信息
   * @return
   */
  public static List<MdmStockUpPlan> handlerNoSubmitStockPlan(List<MdmStockUpPlan> noSubmitList, Map<String, ProductStockInfo> stockReverseMap) {
    List<MdmStockUpPlan> realNoSubmitList = new ArrayList<>();
    if (CollectionUtils.isEmpty(noSubmitList)) {
      return realNoSubmitList;
    }
    Map<String, List<MdmStockUpPlan>> groupMap = noSubmitList.stream().collect(Collectors.groupingBy(MdmStockUpPlan::getGroupKey));
    groupMap.entrySet().forEach(entry -> {
      String factoryProductKey = entry.getKey();
      List<MdmStockUpPlan> stockUpList = entry.getValue();
      Long sumStockQty = getSumStockUpQty(stockUpList);
      Long leftOverQty = BigDecimal.ZERO.longValue();
      ProductStockInfo stockInfo = stockReverseMap.get(factoryProductKey);
      if (null != stockInfo) {
        leftOverQty = stockInfo.getLeftOverQty();
      }
      //不备货
      if (leftOverQty >= sumStockQty) {
        return;
      }
      Map<String, MdmStockUpPlan> locationGroupMap = getGroupStockUp(stockUpList);
      List<LocationTypeEnum> sortList = LocationTypeEnum.getStockUpSort();
      //先外销，再OE，最后内销
      for (LocationTypeEnum locationType : sortList) {
        String locationGroupKey = getStockGroupKey(factoryProductKey, locationType.getValue());
        MdmStockUpPlan locationStockUpPlan = locationGroupMap.get(locationGroupKey);
        if (null == locationStockUpPlan) {
          continue;
        }
        Long locationStockUpQty = locationStockUpPlan.getStockQty();
        if (null == locationStockUpQty || locationStockUpQty == BigDecimal.ZERO.longValue()) {
          continue;
        }
        if (leftOverQty >= locationStockUpQty) {
          leftOverQty = leftOverQty - locationStockUpQty;
          continue;
        }
        //重新设置备货量
        locationStockUpPlan.setStockQty(locationStockUpQty - leftOverQty);
        leftOverQty = BigDecimal.ZERO.longValue();
        realNoSubmitList.add(locationStockUpPlan);
      }
    });
    return realNoSubmitList;
  }

  /**
   * 组合分厂+物料分组key、库位key
   */
  public static String getStockGroupKey(String groupKey, String locationKey) {
    return String.format("%s|*|%s", groupKey, locationKey);
  }

  /**
   * 根据分厂+物料编码的键值，获取总备货量
   *
   * @param locationStockUpQtyMap
   *     备货计划集合
   * @param groupKey
   *     分厂 + 物料编码 键值
   * @return
   */
  public static Long getSumStockUpQty(Map<String, Map<LocationTypeEnum, Long>> locationStockUpQtyMap, String groupKey) {
    if (StringUtils.isBlank(groupKey)) {
      return BigDecimal.ZERO.longValue();
    }
    Map<LocationTypeEnum, Long> locationStockUpQtyInfo = locationStockUpQtyMap.get(groupKey);
    if (CollectionUtils.isEmpty(locationStockUpQtyInfo)) {
      return BigDecimal.ZERO.longValue();
    }
    Long sum = BigDecimal.ZERO.longValue();
    for (Map.Entry<LocationTypeEnum, Long> entry : locationStockUpQtyInfo.entrySet()) {
      Long locationStockUpQty = entry.getValue();
      if (null == locationStockUpQty) {
        locationStockUpQty = BigDecimal.ZERO.longValue();
      }
      sum = sum + locationStockUpQty;
    }
    return sum;
  }


  /**
   * 根据超欠产量及库位信息，构建超欠产量的销售生产需求计划
   *
   * @param monthPlanVersion
   *     销售生产需求计划版本
   * @param require
   *     分厂规格基础信息
   * @param locationTypeEnum
   *     库位类型
   * @param shortQty
   *     超欠产量
   * @return
   */
  public static SaleMonthPlanRequire buildShortRequire(String monthPlanVersion, SaleMonthPlanRequire require, LocationTypeEnum locationTypeEnum, Long shortQty) {
    SaleMonthPlanRequire shortRequire = new SaleMonthPlanRequire();
    BeanUtils.copyProperties(require, shortRequire);
    shortRequire.setId(null);
    shortRequire.setMonthPlanVersion(monthPlanVersion);
    shortRequire.setLocationType(locationTypeEnum.getValue());
    shortRequire.setIsImportantCustom(YesOrNoEnum.NO.getValue());
    shortRequire.setIsEmergency(YesOrNoEnum.NO.getValue());
    shortRequire.setIsEnsurePlan(YesOrNoEnum.NO.getValue());
    shortRequire.setChannel(null);
    shortRequire.setOrderNo(null);
    shortRequire.setDeliveryDateDue(null);
    shortRequire.setIsDebitPlan(YesOrNoEnum.YES.getValue());
    shortRequire.setIsStockUp(YesOrNoEnum.NO.getValue());
    //负数取正
    shortRequire.setPlanQty(BigDecimal.ZERO.longValue() - shortQty);
    shortRequire.setRemark("超欠产量");
    return shortRequire;
  }

  /**
   * 根据备货量及库位信息，构建备货量的销售生产需求计划
   *
   * @param monthPlanVersion
   *     销售生产需求计划版本
   * @param require
   *     分厂规格基础信息
   * @param locationTypeEnum
   *     库位信息
   * @param stockUpQty
   *     备货量
   * @return
   */
  public static SaleMonthPlanRequire buildStockUpRequire(String monthPlanVersion, SaleMonthPlanRequire require, LocationTypeEnum locationTypeEnum, Long stockUpQty) {
    SaleMonthPlanRequire stockUpRequire = new SaleMonthPlanRequire();
    BeanUtils.copyProperties(require, stockUpRequire);
    stockUpRequire.setId(null);
    stockUpRequire.setMonthPlanVersion(monthPlanVersion);
    stockUpRequire.setLocationType(locationTypeEnum.getValue());
    stockUpRequire.setIsImportantCustom(YesOrNoEnum.NO.getValue());
    stockUpRequire.setIsEmergency(YesOrNoEnum.NO.getValue());
    stockUpRequire.setIsEnsurePlan(YesOrNoEnum.NO.getValue());
    stockUpRequire.setChannel(null);
    stockUpRequire.setOrderNo(null);
    stockUpRequire.setIsDebitPlan(YesOrNoEnum.NO.getValue());
    stockUpRequire.setIsStockUp(YesOrNoEnum.YES.getValue());
    stockUpRequire.setDeliveryDateDue(null);
    stockUpRequire.setPlanQty(stockUpQty);
    stockUpRequire.setRemark("备货量");
    return stockUpRequire;
  }

  /**
   * 根据最小批量差值-构建上调到最小批量的额外销售生产需求计划
   *
   * @param monthPlanVersion
   *     销售生产需求计划版本
   * @param require
   *     分厂规格基础信息
   * @param differenceQty
   *     上调到最小批量需要增加的差值量
   * @return
   */
  public static SaleMonthPlanRequire buildMinBatchRequire(String monthPlanVersion, SaleMonthPlanRequire require, Long differenceQty) {
    SaleMonthPlanRequire minRequire = new SaleMonthPlanRequire();
    BeanUtils.copyProperties(require, minRequire);
    minRequire.setId(null);
    minRequire.setMonthPlanVersion(monthPlanVersion);
    //暂时内销
    minRequire.setLocationType(LocationTypeEnum.DOMESTIC_LOCATION.getValue());
    minRequire.setIsImportantCustom(YesOrNoEnum.NO.getValue());
    minRequire.setIsEmergency(YesOrNoEnum.NO.getValue());
    minRequire.setIsEnsurePlan(YesOrNoEnum.NO.getValue());
    minRequire.setChannel(null);
    minRequire.setOrderNo(null);
    minRequire.setDeliveryDateDue(null);
    minRequire.setIsDebitPlan(YesOrNoEnum.NO.getValue());
    minRequire.setIsStockUp(YesOrNoEnum.YES.getValue());
    minRequire.setPlanQty(differenceQty);
    minRequire.setRemark("最小批量差值");
    return minRequire;
  }

  /**
   * 对销售订单的销售生产需求计划按分厂 + 库位 + 物料编码 + 渠道 + 品牌
   * + 是否重要客户 + 是否必保计划 + 是否紧急订单 + 是否欠产 + 期望交期的维度进行合并
   * 并加入到销售生产需求计划中
   *
   * @param saleOrderRequireList
   *     销售订单的销售生产需求计划-合并前
   * @param requireList
   *     合并后加入的销售生产需求集合集合，最终
   */
  public static void mergeSaleOrderRequire(List<SaleMonthPlanRequire> saleOrderRequireList, List<SaleMonthPlanRequire> requireList) {
    Map<String, SaleMonthPlanRequire> mergeMap = new HashMap<>();
    saleOrderRequireList.stream().forEach(saleOrderRequire -> {
      String mergeKey = saleOrderRequire.getMergeGroupKey();
      SaleMonthPlanRequire mergeRequire = mergeMap.get(mergeKey);
      if (null == mergeRequire) {
        mergeMap.put(mergeKey, saleOrderRequire);
        return;
      }
      Long planQty = mergeRequire.getPlanQty();
      if (null == planQty) {
        planQty = BigDecimal.ZERO.longValue();
      }
      if (!mergeRequire.isNeedProduct()) {
        Long needQty = mergeRequire.getQty();
        if (null == needQty) {
          needQty = BigDecimal.ZERO.longValue();
        }
        needQty = needQty + saleOrderRequire.getQty();
        mergeRequire.setQty(needQty);
      }
      planQty = planQty + saleOrderRequire.getPlanQty();
      mergeRequire.setPlanQty(planQty);
      String orderNo = mergeRequire.getOrderNo();
      List<String> orderNoList = Arrays.asList(orderNo.split(StringConstant.COMMA));
      Set<String> orderNoSet = orderNoList.stream().collect(Collectors.toSet());
      orderNoSet.add(saleOrderRequire.getOrderNo());
      mergeRequire.setOrderNo(orderNoSet.stream().collect(Collectors.joining(StringConstant.COMMA)));
      mergeMap.put(mergeKey, mergeRequire);
    });
    mergeMap.forEach((mergeKey, mergeRequire) -> {
      if (!mergeRequire.isNeedProduct()) {
        mergeRequire.setPlanQty(BigDecimal.ZERO.longValue());
        mergeRequire.setRemark("需求量：" + mergeRequire.getQty() + ";" + mergeRequire.getRemark());
      }
      requireList.add(mergeRequire);
    });
  }

  /**
   * 对月度订单，按分厂+物料维度分组
   * 并根据库存对冲顺序的第二排序，设置库位类别的排序值-locationSortValue
   * 会对库存对冲顺序配置进行校验，没有配置则会提示
   *
   * @param saleOrderList
   *     销售提报订单
   * @return
   */
  public static Map<String, List<MonthPlanSaleOrder>> getGroupOrder(List<MonthPlanSaleOrder> saleOrderList, Map<String, Map<SortHierarchyEnum, List<PlanOrderSortConfiguration>>> factoryGroupMap) {
    Map<String, List<MonthPlanSaleOrder>> saleOrderGroupMap = new HashMap<>();
    if (CollectionUtils.isEmpty(saleOrderList)) {
      return saleOrderGroupMap;
    }
    saleOrderList.stream().forEach(factorySaleOrder -> {
      String factoryCode = factorySaleOrder.getFactoryCode();
      Map<SortHierarchyEnum, List<PlanOrderSortConfiguration>> factoryConfiguration = factoryGroupMap.get(factoryCode);
      if (CollectionUtils.isEmpty(factoryConfiguration) || CollectionUtils.isEmpty(factoryConfiguration.get(SortHierarchyEnum.FIRST_HIERARCHY)) || CollectionUtils.isEmpty(factoryConfiguration.get(SortHierarchyEnum.SECOND_HIERARCHY))) {
        throw new RuntimeException(String.format("%s分厂没有配置完整的库存对冲顺序", factoryCode));
      }
      //设置库存类别的排序值，按库存对冲第二顺序配置设置
      List<PlanOrderSortConfiguration> secondSortConfiguration = factoryConfiguration.get(SortHierarchyEnum.SECOND_HIERARCHY);
      secondSortConfiguration.sort(Comparator.comparing(PlanOrderSortConfiguration::getPriority));
      String key = factorySaleOrder.getGroupKey();
      List<MonthPlanSaleOrder> factorySaleOrderList = saleOrderGroupMap.get(key);
      if (null == factorySaleOrderList) {
        factorySaleOrderList = new ArrayList<>();
      }
      setSortValue(factorySaleOrder, secondSortConfiguration);
      factorySaleOrderList.add(factorySaleOrder);
      saleOrderGroupMap.put(key, factorySaleOrderList);
    });
    return saleOrderGroupMap;
  }

  /**
   * 根据库位类别对冲顺序配置，设置其排序值
   *
   * @param factorySaleOrder
   * @param secondSortConfiguration
   * @return
   */
  private static void setSortValue(MonthPlanSaleOrder factorySaleOrder, List<PlanOrderSortConfiguration> secondSortConfiguration) {
    if (CollectionUtils.isEmpty(secondSortConfiguration)) {
      factorySaleOrder.setLocationSortValue(Integer.MAX_VALUE);
      return;
    }
    for (PlanOrderSortConfiguration sortConfiguration : secondSortConfiguration) {
      String optionCode = sortConfiguration.getOptionCode();
      String[] options = optionCode.split(StringConstant.SPLIT_SEMICOLON);
      if (check(factorySaleOrder, options[0], options[1], options.length > 2 ? options[2] : "")) {
        factorySaleOrder.setLocationSortValue(sortConfiguration.getPriority());
        break;
      }
    }
    //没有匹配到，设置成最大值，即最低
    if (null == factorySaleOrder.getLocationSortValue()) {
      factorySaleOrder.setLocationSortValue(Integer.MAX_VALUE);
    }
  }


  /**
   * 校验是否匹配
   * 库位类别严格匹配
   * 渠道*表示全匹配，
   * 品牌*表示全匹配。
   *
   * @param factorySaleOrder
   *     销售提报订单
   * @param locationType
   *     库位类型
   * @param channelCode
   *     渠道编码
   * @param brandCode
   *     品牌编码
   * @return
   */
  private static boolean check(MonthPlanSaleOrder factorySaleOrder, String locationType, String channelCode, String brandCode) {
    if (!locationType.equals(factorySaleOrder.getLocationType())) {
      return false;
    }
    if (StringConstant.ALL_MATCH.equals(channelCode) && StringConstant.ALL_MATCH.equals(brandCode)) {
      return true;
    }
    if (StringConstant.ALL_MATCH.equals(channelCode)) {
      return factorySaleOrder.getBrand().equals(brandCode);
    }
    if (StringConstant.ALL_MATCH.equals(brandCode)) {
      return factorySaleOrder.getChannel().equals(channelCode);
    }
    return factorySaleOrder.getChannel().equals(channelCode) && factorySaleOrder.getBrand().equals(brandCode);
  }

  /**
   * 获取总的备货计划
   *
   * @param stockUpList
   * @return
   */
  private static Long getSumStockUpQty(List<MdmStockUpPlan> stockUpList) {
    if (CollectionUtils.isEmpty(stockUpList)) {
      return BigDecimal.ZERO.longValue();
    }
    Long sumStockUp = BigDecimal.ZERO.longValue();
    for (MdmStockUpPlan stockUp : stockUpList) {
      Long stockQty = stockUp.getStockQty();
      if (null != stockQty) {
        sumStockUp = sumStockUp + stockQty;
      }
    }
    return sumStockUp;
  }

  /**
   * 获取按库位分组的备货计划
   *
   * @param stockUpList
   * @return
   */
  private static Map<String, MdmStockUpPlan> getGroupStockUp(List<MdmStockUpPlan> stockUpList) {
    if (CollectionUtils.isEmpty(stockUpList)) {
      return Collections.emptyMap();
    }
    Map<String, MdmStockUpPlan> locationStockUpMap = new HashMap<>();
    stockUpList.stream().forEach(locationStockUp -> {
      String locationGroupKey = getStockGroupKey(locationStockUp.getGroupKey(), String.valueOf(locationStockUp.getLocationType()));
      MdmStockUpPlan locationStockUpPlan = locationStockUpMap.get(locationGroupKey);
      if (null == locationStockUpPlan) {
        locationStockUpMap.put(locationGroupKey, locationStockUp);
        return;
      }
      locationStockUp.setStockQty(locationStockUp.getStockQty() + locationStockUpPlan.getStockQty());
    });
    return locationStockUpMap;
  }

  /**
   * 构建需求计划版本库存信息对象
   *
   * @param productStockInfo
   *     SAP库存信息
   * @return
   */
  private static MonthPlanRequireStock buildRequireStock(ProductStockInfo productStockInfo) {
    MonthPlanRequireStock requireStock = new MonthPlanRequireStock();
    BeanUtils.copyProperties(productStockInfo, requireStock);
    requireStock.setStockQty(productStockInfo.getSumStockQty().intValue());
    requireStock.setRemainingQty(productStockInfo.getLeftOverQty());
    return requireStock;
  }

  private SaleRequirePlanHelper() {

  }

  /**
   *  销售订单按SKU分组
   * @param salesOrders
   * @return
   */
  public static Map<String, List<SalesOrderPool>> getGroupSalesOrder(List<SalesOrderPool> salesOrders) {
    if (CollectionUtils.isEmpty(salesOrders)) {
      return Collections.emptyMap();
    }
    return salesOrders
        .parallelStream()
        .filter(Objects::nonNull)
        .filter(order -> order.getGroupKey() != null)
        .collect(Collectors.groupingByConcurrent(
            SalesOrderPool::getGroupKey,
            Collectors.toCollection(ArrayList::new)
        ));
  }

  /**
   * 处理净需求
   */
  public static List<DpDemandPlan> processNetDemands(
      List<DpOrderOffsetDetail> netDemands,List<MdmAreaCapaAllocation> areaCapaAllocations) {
    if (CollectionUtils.isEmpty(areaCapaAllocations)) {
      return transformAllocationsToDemandPlans(netDemands);
    }
    return processNetDemandsWithCapacity(netDemands, areaCapaAllocations);
  }

  /**
   * 处理有产能配置的净需求
   */
  private static List<DpDemandPlan> processNetDemandsWithCapacity(
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
   * 从列表尾端开始累加净需求量，直到达到或超过指定值
   * 注意：跳出循环的那个订单不修改优先级
   *
   * @param sortedOrders 排序后的净需求列表
   * @param overAreaCapacityValue 超出区域产能值
   */
  private static void processDemandPriorityExcludingLast(
      List<DpOrderOffsetDetail> sortedOrders,
      long overAreaCapacityValue) {

    if (org.apache.commons.collections.CollectionUtils.isEmpty(sortedOrders) || overAreaCapacityValue <= 0) {
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

  private static List<DpOrderOffsetDetail> sortOrdersByPriority(List<DpOrderOffsetDetail> saleOrders) {
    return saleOrders.stream()
        .sorted(getHighPerformanceComparator())
        .collect(Collectors.toList());
  }

  /**
   * 高性能自定义比较器（适用于大数据量）
   */
  private  static Comparator<DpOrderOffsetDetail> getHighPerformanceComparator() {
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
   * 转换订单分配为需求计划
   */
  private static List<DpDemandPlan> transformAllocationsToDemandPlans(
      List<DpOrderOffsetDetail> orders) {

    return orders.stream()
        .map(SaleRequirePlanHelper::buildDemandPlanFromAllocation)
        .collect(Collectors.toList());
  }

  private static DpDemandPlan buildDemandPlanFromAllocation(DpOrderOffsetDetail netDemand) {
    DpDemandPlan demandPlan = new DpDemandPlan();
    BeanUtils.copyProperties(netDemand, demandPlan);
    demandPlan.setNetQty(BigDecimal.valueOf(netDemand.getProducionQty()));
    demandPlan.setYearWeek(netDemand.getWeekYear());
    return demandPlan;
  }


}
