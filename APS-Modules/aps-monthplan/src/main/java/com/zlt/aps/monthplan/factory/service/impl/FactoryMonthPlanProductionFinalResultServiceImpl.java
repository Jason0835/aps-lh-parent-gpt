package com.zlt.aps.monthplan.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.monthplan.factory.mapper.FactoryMonthPlanProductionFinalResultEntityMapper;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProductionFinalResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

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
}
