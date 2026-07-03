package com.zlt.aps.lh.handler;

import cn.hutool.core.date.DateUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;

/**
 * Sku 月计划计算器
 *
 * @author ZLT
 * @date 20260701
 */
@Slf4j
public class SkuMonthPlanCalculator {
    /**
     * 排产日字段模板
     */
    private static String FIELD_NAME_FORMAT = "day%s";

    /**
     * 20260701+ 判断当前排程周期是否存在跨月
     * true 跨月 false 不跨月
     *
     * @return
     */
    public static boolean isCrossMonthByProductionDateInfo(List<Date> allProductionDateList) {
        if (CollectionUtils.isEmpty(allProductionDateList)) {
            return false;
        }
        int size = allProductionDateList.size();
        if (size == BigDecimal.ONE.intValue()) {
            return false;
        }
        allProductionDateList.sort(Comparator.naturalOrder());
        Date first = allProductionDateList.get(BigDecimal.ZERO.intValue());
        int lastIndex = allProductionDateList.size() - BigDecimal.ONE.intValue();
        Date last = allProductionDateList.get(lastIndex);
        YearMonth firstInfo = getProductionYearAndMonth(first);
        YearMonth lastInfo = getProductionYearAndMonth(last);
        return !firstInfo.equals(lastInfo);
    }

    /**
     * 获20260701+ 取前一个月的年份-月份
     *
     * @return
     */
    public static YearMonth getFirstYearMonth(List<Date> allProductionDateList) {
        if (CollectionUtils.isEmpty(allProductionDateList)) {
            return null;
        }
        allProductionDateList.sort(Comparator.naturalOrder());
        Date first = allProductionDateList.get(BigDecimal.ZERO.intValue());
        return getProductionYearAndMonth(first);
    }

    /**
     * 获20260701+ 取后一个月的年份-月份
     *
     * @return
     */
    public static YearMonth getLastYearMonth(List<Date> allProductionDateList) {
        if (CollectionUtils.isEmpty(allProductionDateList)) {
            return null;
        }
        allProductionDateList.sort(Comparator.naturalOrder());
        Date last = allProductionDateList.get(allProductionDateList.size() - BigDecimal.ONE.intValue());
        return getProductionYearAndMonth(last);
    }

    /**
     * 获取对应年、月的月计划排产计划
     *
     * @param skuMonthProductionInfo 需要查找的Sku信息
     * @param yearMonth              年、月
     * @return
     */
    public static FactoryMonthPlanProductionFinalResult getSkuYearMonthFinal(List<FactoryMonthPlanProductionFinalResult> allMonthPlanList, FactoryMonthPlanProductionFinalResult skuMonthProductionInfo, YearMonth yearMonth) {
        if (null == skuMonthProductionInfo || null == yearMonth || CollectionUtils.isEmpty(allMonthPlanList)) {
            return null;
        }
        List<FactoryMonthPlanProductionFinalResult> resultList = Lists.newArrayList();
        allMonthPlanList.forEach(singlePlan -> {
            if (!singlePlan.isSameYearMonth(yearMonth)) {
                //不同年月
                return;
            }
            if (!skuMonthProductionInfo.getMaterialStatusKey().equals(singlePlan.getMaterialStatusKey())) {
                //不同Sku + 计划类型
                return;
            }
            resultList.add(singlePlan);
        });
        if (CollectionUtils.isEmpty(resultList)) {
            return null;
        }
        return resultList.get(BigDecimal.ZERO.intValue());
    }

    /**
     * 根据Sku日排产周期内的月计划安排情况，获取Sku对应的计划量
     * 需要看日排产周期是否存在跨月
     * 1、不存在跨月
     * 1.1、看日排产周期内是否有计划量
     * 1.1.1、没有计划量，则取当前周期日之前的所有月计划量
     * 1.1.2、有计划量，则取得最晚计划量日，从最晚日往后找，找到第一个没有计划量日前一日，统计从月周期起始日~找到的日之间的计划量
     * 2、存在跨月
     * 2.1、日排产周期内是否有计划量
     * 2.1.1、没有计划量，则取前一个月的所有计划量
     * 2.1.2、有计划量，则看最晚一个计划量所处月
     * 2.1.2.1、如果最晚日计划量所处月份为后一个月，则从最晚日开始，查找后一个月最晚日往后，第一个没有计划量日前一日，统计前一个月的所有计划量+后一个月开始日~找到的日之间的计划量
     * 2.1.2.2、如果最晚日计划量所处月份为前一个月，则统计前一个月的所有计划量
     *
     * @param allProductionDate      日排产周期信息(通常为三天8个班)
     * @param allMonthPlanList       所有月计划量
     * @param skuMonthProductionInfo Sku信息
     * @return
     */
    public static Integer getPlanQty(List<Date> allProductionDate, List<FactoryMonthPlanProductionFinalResult> allMonthPlanList, FactoryMonthPlanProductionFinalResult skuMonthProductionInfo) {
        if (CollectionUtils.isEmpty(allProductionDate) || CollectionUtils.isEmpty(allMonthPlanList) || null == skuMonthProductionInfo) {
            return BigDecimal.ZERO.intValue();
        }
        return getPlanQtyByMonthPlan(skuMonthProductionInfo, allProductionDate, allMonthPlanList);
    }

    /**
     * 20260701+ 获取排产日期所在排产年份-月份
     *
     * @param productionDate
     * @return
     */
    private static YearMonth getProductionYearAndMonth(Date productionDate) {
        //年份
        int year = DateUtil.year(productionDate);
        //月份 0~11，故而要+1
        int month = DateUtil.month(productionDate) + BigDecimal.ONE.intValue();
        return YearMonth.of(year, month);
    }

    /**
     * 根据Sku日排产周期内的月计划安排情况，获取Sku对应的计划量
     * 需要看日排产周期是否存在跨月
     * 1、不存在跨月
     * 1.1、看日排产周期内是否有计划量
     * 1.1.1、没有计划量，则取当前周期日之前的所有月计划量
     * 1.1.2、有计划量，则取得最晚计划量日，从最晚日往后找，找到第一个没有计划量日前一日，统计从月周期起始日~找到的日之间的计划量
     * 2、存在跨月
     * 2.1、日排产周期内是否有计划量
     * 2.1.1、没有计划量，则取前一个月的所有计划量
     * 2.1.2、有计划量，则看最晚一个计划量所处月
     * 2.1.2.1、如果最晚日计划量所处月份为后一个月，则从最晚日开始，查找后一个月最晚日往后，第一个没有计划量日前一日，统计前一个月的所有计划量+后一个月开始日~找到的日之间的计划量
     * 2.1.2.2、如果最晚日计划量所处月份为前一个月，则统计前一个月的所有计划量
     *
     * @param skuMonthProductionInfo Sku信息
     * @param allProductionList      日排产周期
     * @param allMonthPlanList       所有月排产计划
     * @return
     */
    private static Integer getPlanQtyByMonthPlan(FactoryMonthPlanProductionFinalResult skuMonthProductionInfo, List<Date> allProductionList, List<FactoryMonthPlanProductionFinalResult> allMonthPlanList) {
        if (null == skuMonthProductionInfo || CollectionUtils.isEmpty(allProductionList) || CollectionUtils.isEmpty(allMonthPlanList)) {
            return BigDecimal.ZERO.intValue();
        }
        if (!isCrossMonthByProductionDateInfo(allProductionList)) {
            //非跨月
            YearMonth yearMonth = getFirstYearMonth(allProductionList);
            FactoryMonthPlanProductionFinalResult skuYearMonth = getSkuYearMonthFinal(allMonthPlanList, skuMonthProductionInfo, yearMonth);
            if (null == skuYearMonth) {
                return BigDecimal.ZERO.intValue();
            }
            return getEarliestContinuousPlanQty(skuYearMonth, allProductionList);
        }
        //取得跨月的年、月信息
        YearMonth firstYearMonth = getFirstYearMonth(allProductionList);
        YearMonth lastYearMonth = getLastYearMonth(allProductionList);
        //各月排产日分组
        Map<YearMonth, List<Date>> yearMonthMap = getYearMonthProductionDateInfo(allProductionList);
        if (CollectionUtils.isEmpty(yearMonthMap)) {
            return BigDecimal.ZERO.intValue();
        }
        Map<YearMonth, FactoryMonthPlanProductionFinalResult> yearMonthSkuProductionMap = getHasProductionPlan(allMonthPlanList, yearMonthMap, skuMonthProductionInfo);
        if (CollectionUtils.isEmpty(yearMonthSkuProductionMap)) {
            //日排产周期内都没有计划量--取第一个月的全部计划量
            FactoryMonthPlanProductionFinalResult skuYearMonth = getSkuYearMonthFinal(allMonthPlanList, skuMonthProductionInfo, firstYearMonth);
            if (null == skuYearMonth) {
                return BigDecimal.ZERO.intValue();
            }
            return statisticsPlanQtyEndDay(firstYearMonth.lengthOfMonth(), skuYearMonth);
        }
        /**
         * 日排产周期内有计划量
         * 1、有跨月计划量
         * 2、不跨月计划量
         */
        if (yearMonthSkuProductionMap.containsKey(lastYearMonth)) {
            //跨月有计划量，则计划量 = 当月计划量 + 跨月计划量
            Integer sumPlanQty = BigDecimal.ZERO.intValue();
            //当月计划量
            FactoryMonthPlanProductionFinalResult skuYearMonth = getSkuYearMonthFinal(allMonthPlanList, skuMonthProductionInfo, firstYearMonth);
            if (null != skuYearMonth) {
                sumPlanQty = sumPlanQty + statisticsPlanQtyEndDay(firstYearMonth.lengthOfMonth(), skuYearMonth);
            }
            //跨月计划量，到新断点为止
            FactoryMonthPlanProductionFinalResult nextYearMonth = yearMonthSkuProductionMap.get(lastYearMonth);
            sumPlanQty = sumPlanQty + getEarliestContinuousPlanQty(nextYearMonth, yearMonthMap.get(lastYearMonth));
            return sumPlanQty;
        }
        //跨月没有计划量，只有当月有计划量
        FactoryMonthPlanProductionFinalResult firstMonthInfo = yearMonthSkuProductionMap.get(firstYearMonth);
        return getEarliestContinuousPlanQty(firstMonthInfo, yearMonthMap.get(firstYearMonth));
    }

    /**
     * 根据排产日信息，获取在dateList中最晚出现计划量
     * 以此日为起始，往后查找，直到第一个没有排产量的排产日
     * 以找到的排产日，统计从周期第一日~找到的排产日前一个日的计划量
     *
     * @param dateList
     * @return
     */
    private static Integer getEarliestContinuousPlanQty(FactoryMonthPlanProductionFinalResult skuMonthProductionInfo, List<Date> dateList) {
        if (null == skuMonthProductionInfo || CollectionUtils.isEmpty(dateList)) {
            return BigDecimal.ZERO.intValue();
        }
        //取得日期所在月的天数即年份、月份
        List<Integer> dayList = Lists.newArrayList();
        Set<YearMonth> yearMonthSet = Sets.newHashSet();
        dateList.forEach(date -> {
            int day = DateUtil.dayOfMonth(date);
            DateUtil.dayOfMonth(date);
            YearMonth yearMonth = getProductionYearAndMonth(date);
            yearMonthSet.add(yearMonth);
            dayList.add(day);
        });
        //不能存在多个月份
        if (CollectionUtils.isEmpty(yearMonthSet) || yearMonthSet.size() > BigDecimal.ONE.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        //取得年、月
        YearMonth realYearMonth = Lists.newArrayList(yearMonthSet).get(BigDecimal.ZERO.intValue());
        //年份、月份不同则为零
        if (!skuMonthProductionInfo.isSameYearMonth(realYearMonth)) {
            return BigDecimal.ZERO.intValue();
        }
        //取得有计划量的最后一天
        Integer lastDay = getLastHasPlanQtyDay(dayList, skuMonthProductionInfo);
        if (null == lastDay) {
            //都没有计划量,取到三天中任意一天
            return statisticsPlanQtyEndDay(dayList.get(BigDecimal.ZERO.intValue()), skuMonthProductionInfo);
        }
        Integer earliestContinuousDay = getEarliestContinuousDay(lastDay, realYearMonth, skuMonthProductionInfo);
        //不能小于1
        if (null == earliestContinuousDay || earliestContinuousDay < BigDecimal.ONE.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        //统计计划量
        Integer sumPlanQty = statisticsPlanQtyEndDay(earliestContinuousDay, skuMonthProductionInfo);
        return sumPlanQty;
    }

    /**
     * 排产日按年月分组处理
     *
     * @param allProductionList
     * @return
     */
    private static Map<YearMonth, List<Date>> getYearMonthProductionDateInfo(List<Date> allProductionList) {
        if (CollectionUtils.isEmpty(allProductionList)) {
            return Collections.emptyMap();
        }
        Map<YearMonth, List<Date>> yearMonthMap = Maps.newHashMap();
        allProductionList.forEach(singleDate -> {
            YearMonth yearMonth = getProductionYearAndMonth(singleDate);
            List<Date> productionList = yearMonthMap.get(yearMonth);
            if (null == productionList) {
                productionList = Lists.newArrayList();
                yearMonthMap.put(yearMonth, productionList);
            }
            productionList.add(singleDate);
        });
        return yearMonthMap;
    }

    /**
     * 排产日是否有排产计划量
     *
     * @param allMonthPlanList 所有排产计划
     * @param yearMonthMap     年-月信息
     * @return
     */
    private static Map<YearMonth, FactoryMonthPlanProductionFinalResult> getHasProductionPlan(List<FactoryMonthPlanProductionFinalResult> allMonthPlanList, Map<YearMonth, List<Date>> yearMonthMap, FactoryMonthPlanProductionFinalResult skuMonthProductionInfo) {
        if (CollectionUtils.isEmpty(yearMonthMap) || CollectionUtils.isEmpty(allMonthPlanList)) {
            return Collections.emptyMap();
        }
        Map<YearMonth, FactoryMonthPlanProductionFinalResult> hasProductionPlanMap = Maps.newHashMap();
        yearMonthMap.forEach((yearMonth, dateList) -> {
            FactoryMonthPlanProductionFinalResult skuYearMonth = getSkuYearMonthFinal(allMonthPlanList, skuMonthProductionInfo, yearMonth);
            if (null == skuYearMonth) {
                return;
            }
            Integer lastDay = getLastHasPlanQtyDay(getMonthDayList(dateList), skuYearMonth);
            if (null == lastDay) {
                return;
            }
            hasProductionPlanMap.put(yearMonth, skuYearMonth);
        });
        if (CollectionUtils.isEmpty(hasProductionPlanMap)) {
            return Collections.emptyMap();
        }
        return hasProductionPlanMap;
    }

    /**
     * 获取排日所在月份的天数集合
     *
     * @param dateList
     * @return
     */
    private static List<Integer> getMonthDayList(List<Date> dateList) {
        if (CollectionUtils.isEmpty(dateList)) {
            return Collections.emptyList();
        }
        List<Integer> dayList = Lists.newArrayList();
        dateList.forEach(singleDate -> dayList.add(DateUtil.dayOfMonth(singleDate)));
        return dayList;
    }

    /**
     * 在skuMonthProductionInfo中获取dayList集合中
     * 最后一个有计划量的排产日
     *
     * @param dayList                需要排产日集合
     * @param skuMonthProductionInfo 月排产信息
     * @return
     */
    private static Integer getLastHasPlanQtyDay(List<Integer> dayList, FactoryMonthPlanProductionFinalResult skuMonthProductionInfo) {
        if (null == skuMonthProductionInfo || CollectionUtils.isEmpty(dayList)) {
            return null;
        }
        //取得有计划量的最后一天
        Integer lastDay = null;
        for (Integer dateIndex : dayList) {
            String fieldName = String.format(FIELD_NAME_FORMAT, dateIndex);
            Object value = skuMonthProductionInfo.getFieldValueByFieldName(fieldName);
            if (null == value || (Integer) value <= BigDecimal.ZERO.intValue()) {
                continue;
            }
            if (null == lastDay || lastDay < dateIndex) {
                lastDay = dateIndex;
            }
        }
        return lastDay;
    }

    /**
     * 获取从startDay开始，连续有计划量的最后一个排产日：
     * 即获取最早没有计划排产量的排产日的前一日
     *
     * @param startDay               开始日
     * @param yearMonth              年份-月份
     * @param skuMonthProductionInfo sku月排产信息
     * @return
     */
    private static Integer getEarliestContinuousDay(Integer startDay, YearMonth yearMonth, FactoryMonthPlanProductionFinalResult skuMonthProductionInfo) {
        //不能小于1
        if (startDay < BigDecimal.ONE.intValue() || null == skuMonthProductionInfo || null == yearMonth) {
            return null;
        }
        Integer maxDay = yearMonth.lengthOfMonth();
        Integer earliestContinuousDay = startDay;
        for (; earliestContinuousDay <= maxDay; ) {
            String fieldName = String.format(FIELD_NAME_FORMAT, earliestContinuousDay);
            Object value = skuMonthProductionInfo.getFieldValueByFieldName(fieldName);
            if (null == value || (Integer) value <= BigDecimal.ZERO.intValue()) {
                earliestContinuousDay = earliestContinuousDay - BigDecimal.ONE.intValue();
                break;
            }
            earliestContinuousDay = earliestContinuousDay + BigDecimal.ONE.intValue();
        }
        if (earliestContinuousDay < BigDecimal.ONE.intValue()) {
            return null;
        }
        if (earliestContinuousDay > maxDay) {
            earliestContinuousDay = maxDay;
        }
        return earliestContinuousDay;
    }

    /**
     * 统计从第1天~endDay的所有计划量
     *
     * @param endDay                 结束统计日
     * @param skuMonthProductionInfo Sku月排产信息
     * @return
     */
    private static Integer statisticsPlanQtyEndDay(Integer endDay, FactoryMonthPlanProductionFinalResult skuMonthProductionInfo) {
        if (null == endDay || null == skuMonthProductionInfo) {
            BigDecimal.ZERO.intValue();
        }
        if (endDay < BigDecimal.ONE.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        //统计计划量
        Integer planDay = BigDecimal.ONE.intValue();
        Integer sumPlanQty = BigDecimal.ZERO.intValue();
        for (; planDay <= endDay; ) {
            String fieldName = String.format(FIELD_NAME_FORMAT, planDay);
            Object value = skuMonthProductionInfo.getFieldValueByFieldName(fieldName);
            if (null != value && (Integer) value >= BigDecimal.ZERO.intValue()) {
                sumPlanQty = sumPlanQty + (Integer) value;
            }
            planDay = planDay + BigDecimal.ONE.intValue();
        }
        return sumPlanQty;
    }
}
