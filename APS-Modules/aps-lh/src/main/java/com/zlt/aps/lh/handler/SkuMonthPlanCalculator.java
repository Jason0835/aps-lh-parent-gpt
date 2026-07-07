package com.zlt.aps.lh.handler;

import cn.hutool.core.date.DateUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.lh.api.domain.entity.LhDayFinishQty;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
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
     * 排产日字段模板
     */
    private static String FIELD_NAME_FORMAT = "day%s";

    /**
     * 根据年，月，在月的天数，得到日期
     *
     * @param localDate 本地日期
     * @return
     */
    public static Date getDate(LocalDate localDate) {
        ZoneId zoneId = ZoneId.systemDefault();
        Instant instantTime = localDate.atStartOfDay(zoneId).toInstant();
        return new Date(instantTime.toEpochMilli());
    }

    /**
     * 将date转换成LocalDate
     *
     * @param date
     * @return
     */
    public static LocalDate getDate(Date date) {
        if (null == date) {
            return null;
        }
        ZoneId zoneId = ZoneId.systemDefault();
        return date.toInstant().atZone(zoneId).toLocalDate();
    }

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
     * 获取下一个年月
     *
     * @param allProductionDate
     * @return
     */
    public static YearMonth getNextMonth(List<Date> allProductionDate) {
        if (CollectionUtils.isEmpty(allProductionDate)) {
            return null;
        }
        YearMonth firstYearMonth = getFirstYearMonth(allProductionDate);
        boolean isCrossMonth = isCrossMonthByProductionDateInfo(allProductionDate);
        YearMonth nextMonth;
        if (isCrossMonth) {
            nextMonth = SkuMonthPlanCalculator.getLastYearMonth(allProductionDate);
        } else {
            nextMonth = firstYearMonth.plusMonths(BigDecimal.ONE.longValue());
        }
        return nextMonth;
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
        if (CollectionUtils.isEmpty(allMonthPlanList) || null == skuInfo) {
            return Collections.emptyMap();
        }
        Map<YearMonth, Integer> result = Maps.newHashMap();
        YearMonth firstMonth = getFirstYearMonth(allProductionList);
        //如果跨月，只看后面的
        if (isCrossMonthByProductionDateInfo(allProductionList)) {
            result.put(firstMonth, BigDecimal.ZERO.intValue());
            YearMonth lastMonth = getLastYearMonth(allProductionList);
            result.put(lastMonth, getOverdueProduction(allMonthPlanList, skuInfo, lastMonth));
            return result;
        }
        //当月
        if (isNextMonthFinal) {
            result.put(firstMonth, BigDecimal.ZERO.intValue());
            return result;
        }
        result.put(firstMonth, getOverdueProduction(allMonthPlanList, skuInfo, firstMonth));
        return result;
    }

    /**
     * 获取硫化余量
     * 硫化余量 = 计划量 - 已完成量 + 超欠产
     *
     * @param productionYearMonth  当前排产年-月
     * @param allProductionDate    日排产周期日
     * @param hasProductionPlanMap 三日内排产计划信息
     * @param monthOverdueQtyMap   年-月超欠产信息
     * @param yearMonthPlanQtyMap  年-月计划量信息
     * @param finishedQty          已完成量
     * @return
     */
    public static Integer getSurplusQty(YearMonth productionYearMonth, List<Date> allProductionDate, Map<YearMonth, FactoryMonthPlanProductionFinalResult> hasProductionPlanMap, Map<YearMonth, Integer> monthOverdueQtyMap, Map<YearMonth, Integer> yearMonthPlanQtyMap, Integer finishedQty) {
        if (null == finishedQty) {
            finishedQty = BigDecimal.ZERO.intValue();
        }
        YearMonth firstMonth = getFirstYearMonth(allProductionDate);
        boolean isCrossMonth = isCrossMonthByProductionDateInfo(allProductionDate);
        //三天内有计划量,不跨月
        if (!CollectionUtils.isEmpty(hasProductionPlanMap) && !isCrossMonth) {
            Integer overdueQty = monthOverdueQtyMap.get(productionYearMonth);
            if (null == overdueQty) {
                overdueQty = BigDecimal.ZERO.intValue();
            }
            Integer planQty = yearMonthPlanQtyMap.get(productionYearMonth);
            if (null == planQty) {
                planQty = BigDecimal.ZERO.intValue();
            }
            return planQty - finishedQty + overdueQty;
        }
        //三天内有计划量，跨月
        if (!CollectionUtils.isEmpty(hasProductionPlanMap) && isCrossMonth) {
            YearMonth lastMonth = getLastYearMonth(allProductionDate);
            boolean isCrossMonthPlanQty = hasProductionPlanMap.containsKey(lastMonth);
            if (isCrossMonthPlanQty) {
                FactoryMonthPlanProductionFinalResult lastYearPlan = hasProductionPlanMap.get(lastMonth);
                Integer sumPlanQty = getSumPlanQty(yearMonthPlanQtyMap);
                Integer overdueQty = BigDecimal.ZERO.intValue();
                if (YesOrNoEnum.NO.getCode().equals(lastYearPlan.getLastMonthValidFlag()) && null != lastYearPlan.getLastMonthOverdueQty()) {
                    overdueQty = -lastYearPlan.getLastMonthOverdueQty();
                }
                return sumPlanQty - finishedQty + overdueQty;
            }
            //没有跨月计划量
            Integer planQty = yearMonthPlanQtyMap.get(firstMonth);
            if (null == planQty) {
                planQty = BigDecimal.ZERO.intValue();
            }
            return planQty - finishedQty;
        }
        //三天内没有计划量
        Integer productionYearMonthPlanQty;
        if (firstMonth.equals(productionYearMonth)) {
            productionYearMonthPlanQty = yearMonthPlanQtyMap.get(productionYearMonth);
        } else {
            productionYearMonthPlanQty = yearMonthPlanQtyMap.get(firstMonth);
        }
        if (null == productionYearMonthPlanQty) {
            productionYearMonthPlanQty = BigDecimal.ZERO.intValue();
        }
        Integer overdueQty;
        int surplus;
        if (isCrossMonth) {
            overdueQty = monthOverdueQtyMap.get(productionYearMonth);
        } else {
            overdueQty = monthOverdueQtyMap.get(firstMonth);
        }
        surplus = productionYearMonthPlanQty - finishedQty + overdueQty;
        if (surplus <= BigDecimal.ZERO.intValue() && isCrossMonth) {
            YearMonth lastMonth = SkuMonthPlanCalculator.getLastYearMonth(allProductionDate);
            Integer lastMonthPlanQty = yearMonthPlanQtyMap.get(lastMonth);
            if (null != lastMonthPlanQty) {
                surplus = surplus + lastMonthPlanQty;
            }
        }
        return surplus;
    }

    /**
     * 根据Sku日排产周期内的月计划安排情况，获取Sku对应的计划量
     * 需要看日排产周期是否存在跨月
     * 1、不存在跨月
     * 1.1、看下个月是否定稿
     * 1.1.1、如果定稿则计划量计算起始日为下一个月定稿对应需求的库存抓取日
     * 1.1.2、如果没有定稿，则计划量计算起始日为当月计划的第一天
     * 1.2、看日排产周期内是否有计划量
     * 1.2.1、没有计划量，则取当前周期日之前的所有月计划量(计划量计算起始日~当前周期日)
     * 1.2.2、有计划量，则取得最晚计划量日，从最晚日往后找，找到第一个没有计划量日前一日，统计从计划量计算起始日~找到的日之间的计划量
     * 2、存在跨月
     * 2.1、日排产周期内是否有计划量
     * 2.1.1、没有计划量，则取前一个月的所有计划量(计划量计算起始日~当月月底)
     * 2.1.2、有计划量，则看最晚一个计划量所处月
     * 2.1.2.1、如果最晚日计划量所处月份为后一个月，则从最晚日开始，查找后一个月最晚日往后，第一个没有计划量日前一日，统计前一个月的所有计划量(计划量计算起始日~当月月底)+后一个月开始日~找到的日之间的计划量
     * 2.1.2.2、如果最晚日计划量所处月份为前一个月，则统计前一个月的所有计划量(计划量计算起始日~当月月底)
     *
     * @param allProductionDate      日排产周期信息(通常为三天8个班)
     * @param allMonthPlanList       所有月计划量
     * @param skuMonthProductionInfo Sku信息
     * @param startDay               第一个月的计划量起始日
     * @return
     */
    public static Map<YearMonth, Integer> getPlanQty(List<Date> allProductionDate, List<FactoryMonthPlanProductionFinalResult> allMonthPlanList, FactoryMonthPlanProductionFinalResult skuMonthProductionInfo, Integer startDay) {
        if (CollectionUtils.isEmpty(allProductionDate) || CollectionUtils.isEmpty(allMonthPlanList) || null == skuMonthProductionInfo || null == startDay) {
            return Collections.emptyMap();
        }
        if (startDay < BigDecimal.ONE.intValue()) {
            return Collections.emptyMap();
        }
        return getPlanQtyByMonthPlan(skuMonthProductionInfo, startDay, allProductionDate, allMonthPlanList);
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
     * 获取对应的排产计划
     *
     * @param allMonthPlanList  所有排产计划
     * @param allProductionList 日排产周期信息
     * @param factoryCode       工厂
     * @param materialCode      物料
     * @param productStatus     计划类型
     * @return
     */
    public static Map<YearMonth, FactoryMonthPlanProductionFinalResult> getHasProductionPlan(List<FactoryMonthPlanProductionFinalResult> allMonthPlanList, List<Date> allProductionList, String factoryCode, String materialCode, String productStatus) {
        Map<YearMonth, List<Date>> yearMonthMap = getYearMonthProductionDateInfo(allProductionList);
        FactoryMonthPlanProductionFinalResult skuInfo = new FactoryMonthPlanProductionFinalResult();
        skuInfo.setFactoryCode(factoryCode);
        skuInfo.setMaterialCode(materialCode);
        skuInfo.setProductStatus(productStatus);
        return getHasProductionPlan(allMonthPlanList, yearMonthMap, skuInfo);
    }

    /**
     * 20260701+ 获取排产日期所在排产年份-月份
     *
     * @param productionDate
     * @return
     */
    public static YearMonth getProductionYearAndMonth(Date productionDate) {
        //年份
        int year = DateUtil.year(productionDate);
        //月份 0~11，故而要+1
        int month = DateUtil.month(productionDate) + BigDecimal.ONE.intValue();
        return YearMonth.of(year, month);
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
     * @param startDay               前一个月份计划量计算起始日
     * @param allProductionList      日排产周期
     * @param allMonthPlanList       所有月排产计划
     * @return
     */
    private static Map<YearMonth, Integer> getPlanQtyByMonthPlan(FactoryMonthPlanProductionFinalResult skuMonthProductionInfo, Integer startDay, List<Date> allProductionList, List<FactoryMonthPlanProductionFinalResult> allMonthPlanList) {
        if (null == skuMonthProductionInfo || CollectionUtils.isEmpty(allProductionList) || CollectionUtils.isEmpty(allMonthPlanList)) {
            return Collections.emptyMap();
        }
        if (!isCrossMonthByProductionDateInfo(allProductionList)) {
            //非跨月
            YearMonth yearMonth = getFirstYearMonth(allProductionList);
            FactoryMonthPlanProductionFinalResult skuYearMonth = getSkuYearMonthFinal(allMonthPlanList, skuMonthProductionInfo, yearMonth);
            if (null == skuYearMonth) {
                return Collections.emptyMap();
            }
            Map<YearMonth, Integer> result = Maps.newHashMap();
            Integer planQty = getEarliestContinuousPlanQty(skuYearMonth, startDay, allProductionList);
            result.put(yearMonth, planQty);
            return result;
        }
        //取得跨月的年、月信息
        YearMonth firstYearMonth = getFirstYearMonth(allProductionList);
        YearMonth lastYearMonth = getLastYearMonth(allProductionList);
        //各月排产日分组
        Map<YearMonth, List<Date>> yearMonthMap = getYearMonthProductionDateInfo(allProductionList);
        if (CollectionUtils.isEmpty(yearMonthMap)) {
            return Collections.emptyMap();
        }
        Map<YearMonth, FactoryMonthPlanProductionFinalResult> yearMonthSkuProductionMap = getHasProductionPlan(allMonthPlanList, yearMonthMap, skuMonthProductionInfo);
        if (CollectionUtils.isEmpty(yearMonthSkuProductionMap)) {
            //日排产周期内都没有计划量--取第一个月的全部计划量
            FactoryMonthPlanProductionFinalResult skuYearMonth = getSkuYearMonthFinal(allMonthPlanList, skuMonthProductionInfo, firstYearMonth);
            if (null == skuYearMonth) {
                return Collections.emptyMap();
            }
            Map<YearMonth, Integer> result = Maps.newHashMap();
            Integer planQty = statisticsPlanQtyEndDay(startDay, firstYearMonth.lengthOfMonth(), skuYearMonth);
            result.put(firstYearMonth, planQty);
            return result;
        }
        /**
         * 日排产周期内有计划量
         * 1、有跨月计划量
         * 2、不跨月计划量
         */
        Map<YearMonth, Integer> result = Maps.newHashMap();
        if (yearMonthSkuProductionMap.containsKey(lastYearMonth)) {
            //当月计划量
            FactoryMonthPlanProductionFinalResult skuYearMonth = getSkuYearMonthFinal(allMonthPlanList, skuMonthProductionInfo, firstYearMonth);
            if (null != skuYearMonth) {
                Integer firstPlanQty = statisticsPlanQtyEndDay(startDay, firstYearMonth.lengthOfMonth(), skuYearMonth);
                result.put(firstYearMonth, firstPlanQty);
            }
            //跨月计划量，到新断点为止
            FactoryMonthPlanProductionFinalResult nextYearMonth = yearMonthSkuProductionMap.get(lastYearMonth);
            Integer nextPlanQty = getEarliestContinuousPlanQty(nextYearMonth, BigDecimal.ONE.intValue(), yearMonthMap.get(lastYearMonth));
            result.put(lastYearMonth, nextPlanQty);
            return result;
        }
        //跨月没有计划量，只有当月有计划量
        FactoryMonthPlanProductionFinalResult firstMonthInfo = yearMonthSkuProductionMap.get(firstYearMonth);
        Integer planQty = getEarliestContinuousPlanQty(firstMonthInfo, startDay, yearMonthMap.get(firstYearMonth));
        result.put(firstYearMonth, planQty);
        return result;
    }

    /**
     * 根据排产日信息，获取在dateList中最晚出现计划量
     * 以此日为起始，往后查找，直到第一个没有排产量的排产日
     * 以找到的排产日，统计从计划量起始日~找到的排产日前一个日的计划量
     *
     * @param skuMonthProductionInfo Sku信息
     * @param startDay               计算计划量起始日
     * @param dateList               有计划量排产日集合
     * @return
     */
    private static Integer getEarliestContinuousPlanQty(FactoryMonthPlanProductionFinalResult skuMonthProductionInfo, Integer startDay, List<Date> dateList) {
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
            return statisticsPlanQtyEndDay(startDay, dayList.get(BigDecimal.ZERO.intValue()), skuMonthProductionInfo);
        }
        Integer earliestContinuousDay = getEarliestContinuousDay(lastDay, realYearMonth, skuMonthProductionInfo);
        //不能小于1
        if (null == earliestContinuousDay || earliestContinuousDay < BigDecimal.ONE.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        //统计计划量
        Integer sumPlanQty = statisticsPlanQtyEndDay(startDay, earliestContinuousDay, skuMonthProductionInfo);
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
     * @param allMonthPlanList       所有排产计划
     * @param yearMonthMap           年-月信息
     * @param skuMonthProductionInfo Sku信息
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
     * 统计从第startDay~endDay的所有计划量
     *
     * @param startDay               计算起始日
     * @param endDay                 结束统计日
     * @param skuMonthProductionInfo Sku月排产信息
     * @return
     */
    private static Integer statisticsPlanQtyEndDay(Integer startDay, Integer endDay, FactoryMonthPlanProductionFinalResult skuMonthProductionInfo) {
        if (null == startDay || null == endDay || null == skuMonthProductionInfo) {
            BigDecimal.ZERO.intValue();
        }
        if (startDay < BigDecimal.ONE.intValue() && endDay < BigDecimal.ONE.intValue() && startDay > endDay) {
            return BigDecimal.ZERO.intValue();
        }
        //统计计划量
        Integer planDay = startDay;
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

    /**
     * 获取对应年份的超欠产信息
     *
     * @param allMonthPlanList 所有计划
     * @param skuInfo          Sku信息
     * @param yearMonth        年-月信息
     * @return
     */
    private static Integer getOverdueProduction(List<FactoryMonthPlanProductionFinalResult> allMonthPlanList, FactoryMonthPlanProductionFinalResult skuInfo, YearMonth yearMonth) {
        FactoryMonthPlanProductionFinalResult lastMonthPlan = getSkuYearMonthFinal(allMonthPlanList, skuInfo, yearMonth);
        if (null == lastMonthPlan) {
            return BigDecimal.ZERO.intValue();
        }
        if (!YesOrNoEnum.YES.getCode().equals(lastMonthPlan.getLastMonthValidFlag())) {
            return BigDecimal.ZERO.intValue();
        }
        if (Objects.isNull(lastMonthPlan.getLastMonthOverdueQty())) {
            return BigDecimal.ZERO.intValue();
        }
        return lastMonthPlan.getLastMonthOverdueQty();
    }

    /**
     * 获取所有计划计划量
     *
     * @param yearMonthPlanQtyMap
     * @return
     */
    private static Integer getSumPlanQty(Map<YearMonth, Integer> yearMonthPlanQtyMap) {
        if (CollectionUtils.isEmpty(yearMonthPlanQtyMap)) {
            return BigDecimal.ZERO.intValue();
        }
        Integer planQty = BigDecimal.ZERO.intValue();
        for (Map.Entry<YearMonth, Integer> entry : yearMonthPlanQtyMap.entrySet()) {
            Integer monthPlanQty = entry.getValue();
            if (null != monthPlanQty) {
                planQty = planQty + monthPlanQty;
            }
        }
        return planQty;
    }
}
