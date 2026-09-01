package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.zlt.aps.common.engine.utils.MonthPlanSurplusCalculator;
import com.zlt.aps.lh.api.enums.DeleteFlagEnum;
import com.zlt.aps.lh.handler.SkuMonthPlanCalculator;
import com.zlt.aps.lh.mapper.FactoryMonthPlanProductionFinalResultMapper;
import com.zlt.aps.lh.service.ILhMonthStartService;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Date;
import java.util.List;

/**
 * 硫化月计划起始业务处理接口
 *
 * @author ZLT
 * @since 2026-08-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LhMonthStartServiceImpl implements ILhMonthStartService {

    private final FactoryMonthPlanProductionFinalResultMapper monthPlanMapper;

    @Override
    public Date getMonthPlanStartDate(String factory, YearMonth yearMonth) {
        if (StringUtils.isBlank(factory) || null == yearMonth) {
            return null;
        }
        List<Date> findStartDateList = Lists.newArrayList();
        FactoryMonthPlanProductionFinalResult currentMonthPlan = getAnyOne(factory, yearMonth);
        if (null != currentMonthPlan && null != currentMonthPlan.getStockCaptureDate()) {
            findStartDateList.add(currentMonthPlan.getStockCaptureDate());
        }
        YearMonth nextMonth = yearMonth.plusMonths(BigDecimal.ONE.longValue());
        FactoryMonthPlanProductionFinalResult nextMonthPlan = getAnyOne(factory, nextMonth);
        if (null != nextMonthPlan && null != nextMonthPlan.getStockCaptureDate()) {
            findStartDateList.add(nextMonthPlan.getStockCaptureDate());
        }
        if (CollectionUtils.isEmpty(findStartDateList)) {
            return null;
        }
        int endIndex = findStartDateList.size() - BigDecimal.ONE.intValue();
        //从小到大排序
        findStartDateList.sort(null);
        Date maxDate = findStartDateList.get(endIndex);
        YearMonth maxDateYearMonth = YearMonth.from(MonthPlanSurplusCalculator.getDate(maxDate));
        if (maxDateYearMonth.equals(yearMonth)) {
            return maxDate;
        }
        return SkuMonthPlanCalculator.getDate(yearMonth.atDay(BigDecimal.ONE.intValue()));
    }

    /**
     * 获取对应计划的任意一条
     *
     * @param factory   工厂
     * @param yearMonth 排产月份
     * @return
     */
    private FactoryMonthPlanProductionFinalResult getAnyOne(String factory, YearMonth yearMonth) {
        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, factory)
                .eq(FactoryMonthPlanProductionFinalResult::getYear, yearMonth.getYear())
                .eq(FactoryMonthPlanProductionFinalResult::getMonth, yearMonth.getMonthValue())
                .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode());
        wrapper.last("LIMIT 1");
        return monthPlanMapper.selectOne(wrapper);
    }
}
