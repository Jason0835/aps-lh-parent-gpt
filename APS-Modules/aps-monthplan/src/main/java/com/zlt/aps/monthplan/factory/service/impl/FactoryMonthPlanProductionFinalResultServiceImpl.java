package com.zlt.aps.monthplan.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.monthplan.api.domain.entity.FactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MdmMonthSurplus;
import com.zlt.aps.monthplan.factory.mapper.FactoryMonthPlanProductionFinalResultEntityMapper;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProductionFinalResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
  public Map<String, Long> calculateMonthSurplus(String requireVersion) {
    // 获取当前年月
    YearMonth currentYearMonth = YearMonth.now();
    String yearMonth = String.format("%s%02d", currentYearMonth.getYear(), currentYearMonth.getMonthValue());
    LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper = Wrappers.lambdaQuery(FactoryMonthPlanProductionFinalResult.class)
        .ge(FactoryMonthPlanProductionFinalResult::getYearMonth, Integer.valueOf(yearMonth))
        .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, ApsConstant.APS_YES_NO_0);
    List<FactoryMonthPlanProductionFinalResult> factoryMonthPlanProdFinals = this.list(queryWrapper);
    if (CollectionUtils.isEmpty(factoryMonthPlanProdFinals)) {
      return Collections.emptyMap();
    }
    List<MdmMonthSurplus> result = Lists.newArrayList();
    Map<String, List<FactoryMonthPlanProductionFinalResult>> groupByMaterialCode = this.getGroupMonthProdFinalPlanByMaterialCode(factoryMonthPlanProdFinals);
    groupByMaterialCode.forEach((key, value) -> {
      MdmMonthSurplus entity = new MdmMonthSurplus();
      entity.setBaseVale(null);
      entity.setIsDelete(ApsConstant.APS_YES_NO_0);
      long planSurplusQty = value.stream().mapToLong(FactoryMonthPlanProductionFinalResult::getTotalQty).sum();
      entity.setPlanSurplusQty(planSurplusQty);
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
    });
    return calculateMonthSurplus(result);
  }

  @Override
  public List<FactoryMonthPlanProductionFinalResult> findProductionFinalResult(FactoryProductionVersion finalVersion) {
    LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper = Wrappers.lambdaQuery(FactoryMonthPlanProductionFinalResult.class)
        .eq(FactoryMonthPlanProductionFinalResult::getMonthPlanVersion, finalVersion.getMonthPlanVersion())
        .ge(FactoryMonthPlanProductionFinalResult::getProductionVersion, finalVersion.getProductionVersion())
        .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, ApsConstant.APS_YES_NO_0);
    return this.list(queryWrapper);
  }

  private Map<String, Long> calculateMonthSurplus(List<MdmMonthSurplus> monthSurpluses) {
    if (CollectionUtils.isEmpty(monthSurpluses)) {
      return Collections.emptyMap();
    }
    return monthSurpluses.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.groupingBy(
            MdmMonthSurplus::getGroupKey,
            Collectors.summingLong(MdmMonthSurplus::getPlanSurplusQty)
        ));
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
