package com.zlt.aps.lh.handler;

import com.google.common.collect.Maps;
import com.zlt.aps.common.engine.utils.MonthPlanSurplusCalculator;
import com.zlt.aps.lh.api.domain.entity.LhDayFinishQty;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sku 月计划计算器
 *
 * @author ZLT
 * @date 20260701
 */
@Slf4j
public class SkuMonthPlanCalculator {

    /**
     * 根据年，月，在月的天数，得到日期
     *
     * @param localDate 本地日期
     * @return
     */
    public static Date getDate(LocalDate localDate) {
        return MonthPlanSurplusCalculator.getDate(localDate);
    }

    /**
     * 将date转换成LocalDate
     *
     * @param date
     * @return
     */
    public static LocalDate getDate(Date date) {
        return MonthPlanSurplusCalculator.getDate(date);
    }

    /**
     * 20260701+ 判断当前排程周期是否存在跨月
     * true 跨月 false 不跨月
     *
     * @return
     */
    public static boolean isCrossMonthByProductionDateInfo(List<Date> allProductionDateList) {
        return MonthPlanSurplusCalculator.isCrossMonthByProductionDateInfo(allProductionDateList);
    }

    /**
     * 获取下一个年月
     *
     * @param allProductionDate
     * @return
     */
    public static YearMonth getNextMonth(List<Date> allProductionDate) {
        return MonthPlanSurplusCalculator.getNextMonth(allProductionDate);
    }

    /**
     * 获20260701+ 取前一个月的年份-月份
     *
     * @return
     */
    public static YearMonth getFirstYearMonth(List<Date> allProductionDateList) {
        return MonthPlanSurplusCalculator.getFirstYearMonth(allProductionDateList);
    }

    /**
     * 获20260701+ 取后一个月的年份-月份
     *
     * @return
     */
    public static YearMonth getLastYearMonth(List<Date> allProductionDateList) {
        return MonthPlanSurplusCalculator.getLastYearMonth(allProductionDateList);
    }

    /**
     * 获取对应年、月的月计划排产计划
     *
     * @param skuMonthProductionInfo 需要查找的Sku信息
     * @param yearMonth              年、月
     * @return
     */
    public static FactoryMonthPlanProductionFinalResult getSkuYearMonthFinal(List<FactoryMonthPlanProductionFinalResult> allMonthPlanList, FactoryMonthPlanProductionFinalResult skuMonthProductionInfo, YearMonth yearMonth) {
        return MonthPlanSurplusCalculator.getSkuYearMonthFinal(
                allMonthPlanList, skuMonthProductionInfo, yearMonth);
    }

    /**
     * 如果已经跨月，则前一个月的超欠产直接忽略
     * 否则，如果isNextMonthFinal = true，则忽略
     * 否则看第一月的欠产
     *
     * @param isNextMonthFinal  是否下个月已定稿
     * @param allProductionList 日排产周期
     * @param allMonthPlanList  所有月计划信息
     * @param skuInfo           Sku信息
     * @return
     */
    public static Map<YearMonth, Integer> getOverdueProduction(boolean isNextMonthFinal, List<Date> allProductionList, List<FactoryMonthPlanProductionFinalResult> allMonthPlanList, FactoryMonthPlanProductionFinalResult skuInfo) {
        return MonthPlanSurplusCalculator.getOverdueProduction(
                isNextMonthFinal, allProductionList, allMonthPlanList, skuInfo);
    }


    /**
     * 需根据stockCapTureDate获取已完成量
     * 如果stockCapTureDate 为空，则从当月的第一天开始
     * 否则从stockCapTureDate开始计算
     *
     * @param skuMonthProductionInfo Sku信息
     * @param allProductionDate      日排产周期集合
     * @param dayFinishList          日完成量信息
     * @param stockCapTureDate       下个月定稿库存日期
     * @return
     */
    public static Map<YearMonth, Integer> getFinishQty(FactoryMonthPlanProductionFinalResult skuMonthProductionInfo, List<Date> allProductionDate, List<LhDayFinishQty> dayFinishList, Date stockCapTureDate) {
        if (null == skuMonthProductionInfo || CollectionUtils.isEmpty(allProductionDate) || CollectionUtils.isEmpty(dayFinishList)) {
            return Collections.emptyMap();
        }
        //不是同一年、月则没有
        YearMonth firstMonth = getFirstYearMonth(allProductionDate);
        if (null == stockCapTureDate) {
            stockCapTureDate = getDate(firstMonth.atDay(BigDecimal.ONE.intValue()));
        }
        YearMonth stockDateMonth = getProductionYearAndMonth(stockCapTureDate);
        if (!firstMonth.equals(stockDateMonth)) {
            return Collections.emptyMap();
        }
        Date compareDate = stockCapTureDate;
        String groupKey = skuMonthProductionInfo.getMaterialStatusKey();
        List<LhDayFinishQty> effectiveList = dayFinishList.stream().filter(singleDayFinish -> {
            if (!groupKey.equals(singleDayFinish.getMaterialStatusKey())) {
                return false;
            }
            if (!singleDayFinish.isAfterOrCurrent(compareDate)) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(effectiveList)) {
            return Collections.emptyMap();
        }
        Map<YearMonth, Integer> result = Maps.newHashMap();
        Integer finishQty = effectiveList.stream().mapToInt(LhDayFinishQty::getFinishQty).sum();
        result.put(firstMonth, finishQty);
        return result;
    }

    /**
     * 20260701+ 获取排产日期所在排产年份-月份
     *
     * @param productionDate
     * @return
     */
    public static YearMonth getProductionYearAndMonth(Date productionDate) {
        return MonthPlanSurplusCalculator.getProductionYearAndMonth(productionDate);
    }

    /**
     * 统计累计日完成量
     *
     * @param startDay      从 startDay开始
     * @param dayFinishList 所有已完成量
     * @return
     */
    public static Map<String, Integer> getDayFinishQty(Date startDay, List<LhDayFinishQty> dayFinishList) {
        if (null == startDay || CollectionUtils.isEmpty(dayFinishList)) {
            return Collections.emptyMap();
        }
        List<LhDayFinishQty> effectiveList = dayFinishList.stream().filter(singleDay -> singleDay.isAfterOrCurrent(startDay)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(effectiveList)) {
            return Collections.emptyMap();
        }
        Map<String, Integer> result = Maps.newHashMap();
        Map<String, List<LhDayFinishQty>> groupKeyMap = effectiveList.stream().collect(Collectors.groupingBy(LhDayFinishQty::getFactoryMaterialStatusKey));
        groupKeyMap.forEach((factoryMaterialType, dayList) -> {
            if (CollectionUtils.isEmpty(dayList)) {
                result.put(factoryMaterialType, BigDecimal.ZERO.intValue());
                return;
            }
            Integer sumFinishQty = dayList.stream().mapToInt(LhDayFinishQty::getFinishQty).sum();
            result.put(factoryMaterialType, sumFinishQty);
        });
        return result;
    }

}
