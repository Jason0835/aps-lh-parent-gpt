package com.zlt.aps.monthplan.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ruoyi.common.utils.StringUtils;
import com.tlt.aps.constant.FactoryConstant;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.factory.utils.DateUtils;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.monthplan.api.domain.entity.FactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MdmMonthSurplus;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductStock;
import com.zlt.aps.monthplan.factory.mapper.FactoryMonthPlanProductionFinalResultEntityMapper;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProductionFinalResultService;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryMonthPlanProductionFinalResultServiceImpl.java
 * 描    述：FactoryMonthPlanProductionFinalResultServiceImpl工厂月生产计划-最终排产计划定稿业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class FactoryMonthPlanProductionFinalResultServiceImpl extends ServiceImpl<FactoryMonthPlanProductionFinalResultEntityMapper, FactoryMonthPlanProductionFinalResult> implements IFactoryMonthPlanProductionFinalResultService {
  private final BaseDao baseDao;
  @Override
  public List<FactoryMonthPlanProductionFinalResult> findLastTwelveMonthProdFinalPlan() {
    // 获取当前年月
    YearMonth currentYearMonth = YearMonth.now();
    YearMonth startYearMonth = currentYearMonth.minusMonths(12);
    String yearMonth = String.format("%s%02d", startYearMonth.getYear(), startYearMonth.getMonthValue());
    LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper = Wrappers.lambdaQuery(FactoryMonthPlanProductionFinalResult.class)
        .ge(FactoryMonthPlanProductionFinalResult::getYearMonth, Integer.valueOf(yearMonth))
        .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, ApsConstant.APS_YES_NO_0);
    return this.list(queryWrapper);
  }

  @Override
  public int getProductionMonthInLastTwelveMonth(String materialCode) {
    // 获取当前年月
    YearMonth currentYearMonth = YearMonth.now();
    YearMonth startYearMonth = currentYearMonth.minusMonths(12);
    String yearMonth = String.format("%s%02d", startYearMonth.getYear(), startYearMonth.getMonthValue());
    LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper = Wrappers.lambdaQuery(FactoryMonthPlanProductionFinalResult.class)
        .eq(FactoryMonthPlanProductionFinalResult::getMaterialCode, materialCode)
        .ge(FactoryMonthPlanProductionFinalResult::getYearMonth, Integer.valueOf(yearMonth))
        .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, ApsConstant.APS_YES_NO_0);
    List<FactoryMonthPlanProductionFinalResult> factoryMonthPlanProdFinals = this.list(queryWrapper);
    return CollectionUtils.isEmpty(factoryMonthPlanProdFinals) ? 0 : factoryMonthPlanProdFinals.size();
  }

  @Override
  public Map<String, Long> calculateMonthSurplus(String requireVersion,List<MdmProductStock> finishedProductStocks) {
    if(CollectionUtils.isEmpty(finishedProductStocks)){
      return Collections.emptyMap();
    }
    List<Date> stockDates = finishedProductStocks.stream().map(MdmProductStock::getStockDate).filter(Objects::nonNull).collect(Collectors.toList());
    if(CollectionUtils.isEmpty(stockDates)){
      return Collections.emptyMap();
    }
    int year = DateUtils.getYear(stockDates.get(0));
    int month = DateUtils.getMonthsByYear(stockDates.get(0));
    int stockDay = DateUtils.getDaysByMonth(stockDates.get(0));
    // 获取当前年月
    String yearMonth = String.format("%s%02d",year, month);
    LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper = Wrappers.lambdaQuery(FactoryMonthPlanProductionFinalResult.class)
        .ge(FactoryMonthPlanProductionFinalResult::getYearMonth, Integer.valueOf(yearMonth))
        .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, ApsConstant.APS_YES_NO_0);
    List<FactoryMonthPlanProductionFinalResult> factoryMonthPlanProdFinals = this.list(queryWrapper);
    if (CollectionUtils.isEmpty(factoryMonthPlanProdFinals)) {
      return Collections.emptyMap();
    }
    Map<String, Long> monthSurplusMap = Maps.newHashMap();
    List<MdmMonthSurplus> result = Lists.newArrayList();
    Map<String, List<FactoryMonthPlanProductionFinalResult>> groupByMaterialCode = this.getGroupMonthProdFinalPlanByMaterialCode(factoryMonthPlanProdFinals);
    groupByMaterialCode.forEach((key, value) -> {
      long planSurplusQty = this.calculateMonthSurplus(value,stockDay);
      if(planSurplusQty <= BigDecimal.ZERO.longValue()) {
        return;
      }
      MdmMonthSurplus entity = new MdmMonthSurplus();
      entity.setBaseVale(null);
      entity.setIsDelete(ApsConstant.APS_YES_NO_0);
      entity.setPlanSurplusQty(BigDecimal.valueOf(planSurplusQty));
      entity.setFactoryCode(value.get(0).getFactoryCode());
      entity.setYear(value.get(0).getYear());
      entity.setMonth(value.get(0).getMonth());
      entity.setRequireVersion(requireVersion);
      entity.setProductTypeCode(value.get(0).getProductTypeCode());
      entity.setBrand(value.get(0).getBrand());
      entity.setMaterialCode(value.get(0).getMaterialCode());
      entity.setMaterialDesc(value.get(0).getMaterialDesc());
      entity.setStructureName(value.get(0).getStructureName());
      result.add(entity);
      monthSurplusMap.put(key,planSurplusQty);
    });
    if(CollectionUtils.isNotEmpty(result)){
      this.baseDao.insertBatch(result);
    }
    return monthSurplusMap;
  }

  private long calculateMonthSurplus(List<FactoryMonthPlanProductionFinalResult> productionFinalResults, int stockDay) {
      long totalMonthSuplus = BigDecimal.ZERO.longValue();
      //统计汇总值
      Integer[] dayList = FactoryConstant.PRODUCTION_CYCLE;
      String  fieldName;
      long dayValue;
      for(FactoryMonthPlanProductionFinalResult productionFinalResult : productionFinalResults){
        for (Integer day : dayList) {
          if(day < stockDay){
            continue;
          }
          fieldName = "day" + Math.abs(day);

          Object value = productionFinalResult.getFieldValueByFieldName(fieldName);
          if (null == value) {
            dayValue = BigDecimal.ZERO.longValue();
          } else {
            dayValue = (Integer) value;
          }
          totalMonthSuplus = totalMonthSuplus + dayValue;
        }
      }
      return totalMonthSuplus;
  }


  @Override
  public List<FactoryMonthPlanProductionFinalResult> findProductionFinalResult(FactoryProductionVersion finalVersion) {
    LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper = Wrappers.lambdaQuery(FactoryMonthPlanProductionFinalResult.class)
        .eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, finalVersion.getFactoryCode())
        .eq(FactoryMonthPlanProductionFinalResult::getYear, finalVersion.getYear())
        .eq(FactoryMonthPlanProductionFinalResult::getMonth, finalVersion.getMonth())
        .eq(FactoryMonthPlanProductionFinalResult::getMonthPlanVersion, finalVersion.getMonthPlanVersion())
        .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, ApsConstant.APS_YES_NO_0);
    return this.list(queryWrapper);
  }

  private Map<String, List<FactoryMonthPlanProductionFinalResult>> getGroupMonthProdFinalPlanByMaterialCode(List<FactoryMonthPlanProductionFinalResult> factoryMonthPlanProdFinals) {
    if (CollectionUtils.isEmpty(factoryMonthPlanProdFinals)) {
      return Collections.emptyMap();
    }
    return factoryMonthPlanProdFinals
        .parallelStream()
        .filter(Objects::nonNull)
        .filter(item -> StringUtils.isNotBlank(item.getMaterialCode()))
        .collect(Collectors.groupingByConcurrent(
            FactoryMonthPlanProductionFinalResult::getMaterialCode,
            Collectors.toCollection(ArrayList::new)
        ));
  }
}
