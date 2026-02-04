package com.zlt.aps.monthplan.common.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tlt.aps.constant.StringConstant;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlanSum;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductStock;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 汇总净需求计划
 * @author Yelq
 */
@Service
@RequiredArgsConstructor
public class SummaryDemandPlanService {

  // 批量插入处理器
  private final BatchInsertProcessor<DpDemandPlanSum> batchInsertProcessor;

  private final SaveAllocationResultService saveAllocationResultService;

  public void summaryDemandPlan(DpDemandPlan createCondition, PredictionContext.OrderAllocationResult allocationResult, List<DpDemandPlan> finalPlans) {
    Map<String,List<DpDemandPlan>> map = finalPlans.stream().collect(Collectors.groupingBy(DpDemandPlan::getMonthPlanVersionKey));
    Map<String, Map<String, Integer>> stockQtyMap = calculateStockQty(allocationResult.getStockMap());
    List<DpDemandPlanSum> datas = Lists.newArrayList();
    map.forEach((key, value) -> {
      Map<String,Integer> stockMap = stockQtyMap.getOrDefault(key, Collections.emptyMap());
      DpDemandPlanSum entity = new DpDemandPlanSum();
      BeanUtils.copyProperties(value.get(0), entity);
      entity.setId(null);
      entity.setBaseVale(null);
      entity.setStockQty(value.get(0).getStockQty());
      entity.setCurrentYearStockQty(stockMap.getOrDefault(StringConstant.ONE,BigDecimal.ZERO.intValue()));
      entity.setSub1YearStockQty(stockMap.getOrDefault(StringConstant.TWO,BigDecimal.ZERO.intValue()));
      entity.setSub2YearStockQty(stockMap.getOrDefault(StringConstant.THREE,BigDecimal.ZERO.intValue()));
      entity.setNetQty(calculateNetQty(value));
      entity.setPostponeNetQty(calculatePostponeNetQty(value));
      entity.setUnPostponeNetQty(calculateUnPostponeNetQty(value));
      entity.setHeightQty(calculateHeightQty(value));
      entity.setMidQty(calculateMidQty(value));
      entity.setPostponeQty(calculatePostponeQty(value));
      entity.setCycleReserveQty(calculateCycleReserveQty(value));
      entity.setConventionReserveQty(calculateConventionReserveQty(value));
      entity.setIsReachMinProductionQty(entity.getNetQty() >= entity.getMinProductionQty()? YesOrNoEnum.YES.getCode() : YesOrNoEnum.NO.getCode());
      datas.add(entity);
    });
    datas.sort(Comparator.comparing(DpDemandPlanSum::getMaterialCode));
    this.batchInsertProcessor.batchInsert(datas);
    this.saveAllocationResultService.saveAllocationResults(createCondition,createCondition.getMonthPlanVersion(),allocationResult);
  }

  private Map<String, Map<String, Integer>> calculateStockQty(Map<String, List<MdmProductStock>> finishProductStockMap) {
    if(CollectionUtils.isEmpty(finishProductStockMap)){
      return Collections.emptyMap();
    }
    YearMonth now = YearMonth.now();
    YearMonth lastOneYear = now.minusYears(BigDecimal.ONE.intValue());
    YearMonth lastTwoYear = now.minusYears(BigDecimal.ONE.intValue() + BigDecimal.ONE.intValue());
    Map<String, Map<String, Integer>> result = new HashMap<>();
    finishProductStockMap.forEach((key,value)->{
      Map<String, Integer> map = Maps.newHashMap();
      int totalStockQty = value.stream().filter(item -> null != item.getStockQty()).mapToInt(MdmProductStock::getStockQty).sum();
      int currentStockQty = value.stream().filter(item -> filter(item,now)).mapToInt(MdmProductStock::getStockQty).sum();
      int lastOneYearStockQty = value.stream().filter(item -> filter(item,lastOneYear)).mapToInt(MdmProductStock::getStockQty).sum();
      int lastTwoYearStockQty = value.stream().filter(item -> filter(item,lastTwoYear)).mapToInt(MdmProductStock::getStockQty).sum();
      map.put(StringConstant.ZERO,totalStockQty);
      map.put(StringConstant.ONE,currentStockQty);
      map.put(StringConstant.TWO,lastOneYearStockQty);
      map.put(StringConstant.THREE,lastTwoYearStockQty);
      result.put(key, map);
    });
    return result;
  }

  private boolean filter(MdmProductStock item, YearMonth yearMonth) {
    if(StringUtils.isBlank(item.getWeekYear()) || null == item.getStockQty()){
      return false;
    }
    if(yearMonth.equals(YearMonth.now())){
      String currentYearMonthStr = String.format("%s%02d", StringUtils.substring(String.valueOf(yearMonth.getYear()),2,4) ,Integer.valueOf(StringConstant.ONE));
      String transformed = item.getWeekYear().substring(2) + item.getWeekYear().substring(0,2);
      int yearWeek = Integer.parseInt(transformed);
      return yearWeek >= Integer.parseInt(currentYearMonthStr);
    }
    if(yearMonth.equals(YearMonth.now().minusYears(BigDecimal.ONE.intValue()))){
      String currentYearMonthStr = String.format("%s%02d", StringUtils.substring(String.valueOf(yearMonth.getYear()),2,4) ,Integer.valueOf(StringConstant.ONE));
      String transformed = item.getWeekYear().substring(2) + item.getWeekYear().substring(0,2);
      int yearWeek = Integer.parseInt(transformed);
      YearMonth now = YearMonth.now();
      String nowYearMonthStr = String.format("%s%02d", StringUtils.substring(String.valueOf(now.getYear()),2,4) ,Integer.valueOf(StringConstant.ONE));
      return yearWeek >= Integer.parseInt(currentYearMonthStr) && yearWeek < Integer.parseInt(nowYearMonthStr);
    }
    YearMonth lastOneYearWeek = YearMonth.now().minusYears(BigDecimal.ONE.intValue());
    String currentYearMonthStr = String.format("%s%02d", StringUtils.substring(String.valueOf(lastOneYearWeek.getYear()),2,4) ,Integer.valueOf(StringConstant.ONE));
    String transformed = item.getWeekYear().substring(2) + item.getWeekYear().substring(0,2);
    int yearWeek = Integer.parseInt(transformed);
    return yearWeek < Integer.parseInt(currentYearMonthStr);
  }

  private int calculateConventionReserveQty(List<DpDemandPlan> dataList) {
    return dataList.stream()
        .filter(Objects::nonNull)
        .filter(demandPlan ->  demandPlan.getConventionReserveQty() != null)
        .mapToInt(DpDemandPlan::getConventionReserveQty).sum();
  }

  private int calculateCycleReserveQty(List<DpDemandPlan> dataList) {
    return dataList.stream()
        .filter(Objects::nonNull)
        .filter(demandPlan ->  demandPlan.getCycleReserveQty() != null)
        .mapToInt(DpDemandPlan::getCycleReserveQty).sum();
  }

  private int calculatePostponeQty(List<DpDemandPlan> dataList) {
    return dataList.stream()
        .filter(Objects::nonNull)
        .filter(demandPlan ->  demandPlan.getPostponeQty() != null)
        .mapToInt(DpDemandPlan::getPostponeQty).sum();
  }

  private int calculateMidQty(List<DpDemandPlan> dataList) {
    return dataList.stream()
        .filter(Objects::nonNull)
        .filter(demandPlan ->  demandPlan.getMidQty() != null)
        .mapToInt(DpDemandPlan::getMidQty).sum();
  }

  private int calculateHeightQty(List<DpDemandPlan> dataList) {
    return dataList.stream()
        .filter(Objects::nonNull)
        .filter(demandPlan ->  demandPlan.getHeightQty() != null)
        .mapToInt(DpDemandPlan::getHeightQty).sum();
  }

  private int calculateUnPostponeNetQty(List<DpDemandPlan> dataList) {
    return dataList.stream()
        .filter(Objects::nonNull)
        .filter(demandPlan ->  demandPlan.getUnPostponeNetQty() != null)
        .mapToInt(DpDemandPlan::getUnPostponeNetQty).sum();
  }

  private int calculateNetQty(List<DpDemandPlan> dataList) {
    return dataList.stream()
        .filter(Objects::nonNull)
        .filter(demandPlan ->  demandPlan.getNetQty() != null)
        .mapToInt(DpDemandPlan::getNetQty).sum();
  }

  private int calculatePostponeNetQty(List<DpDemandPlan> dataList) {
    return dataList.stream()
        .filter(Objects::nonNull)
        .filter(demandPlan ->  demandPlan.getPostponeNetQty() != null)
        .mapToInt(DpDemandPlan::getPostponeNetQty).sum();
  }

}
