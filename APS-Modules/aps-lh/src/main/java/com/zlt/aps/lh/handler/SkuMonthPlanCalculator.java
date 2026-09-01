package com.zlt.aps.lh.handler;

import com.google.common.collect.Maps;
import com.zlt.aps.common.engine.domain.LhDayPlanAdjustVo;
import com.zlt.aps.common.engine.utils.MonthPlanSurplusCalculator;
import com.zlt.aps.lh.api.domain.entity.LhDayFinishQty;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
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
     * 获取硫化余量
     * 硫化余量 = 计划量(月计划量 + 硫化日调整量) - 已完成量 + 超欠产
     *
     * @param productionYearMonth  当前排产年-月
     * @param allProductionDate    日排产周期日
     * @param hasProductionPlanMap 三日内排产计划信息
     * @param monthOverdueQtyMap   年-月超欠产信息
     * @param yearMonthPlanQtyMap  年-月计划量信息(包含对应的日计划调整量)
     * @param finishedQty          已完成量
     * @return
     */
    public static Integer getSurplusQty(YearMonth productionYearMonth, List<Date> allProductionDate, Map<YearMonth, FactoryMonthPlanProductionFinalResult> hasProductionPlanMap, Map<YearMonth, Integer> monthOverdueQtyMap, Map<YearMonth, Integer> yearMonthPlanQtyMap, Integer finishedQty) {
        return MonthPlanSurplusCalculator.getSurplusQty(
                productionYearMonth, allProductionDate, hasProductionPlanMap,
                monthOverdueQtyMap, yearMonthPlanQtyMap, finishedQty);
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
     * @param maxDiscontinueDays     排产周期内间隔天数
     * @param allMonthPlanList       所有月计划量
     * @param allLhDayAdjustList     所有月份-硫化日计划调整量
     * @param skuMonthProductionInfo Sku信息
     * @param startDay               第一个月的计划量起始日
     * @return
     */
    public static Map<YearMonth, Integer> getPlanQty(List<Date> allProductionDate,
                                                     Integer maxDiscontinueDays,
                                                     List<FactoryMonthPlanProductionFinalResult> allMonthPlanList,
                                                     List<LhDayPlanAdjustVo> allLhDayAdjustList,
                                                     FactoryMonthPlanProductionFinalResult skuMonthProductionInfo,
                                                     Integer startDay) {
        return MonthPlanSurplusCalculator.getPlanQty(allProductionDate, maxDiscontinueDays, allMonthPlanList, allLhDayAdjustList, skuMonthProductionInfo, startDay);
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
     * 汇总月数据量
     *
     * @param needSumQtyMap 需汇总月数量集合
     * @return
     */
    public static int sumQty(Map<YearMonth, Integer> needSumQtyMap) {
        return MonthPlanSurplusCalculator.sumQty(needSumQtyMap);
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
        return MonthPlanSurplusCalculator.getHasProductionPlan(
                allMonthPlanList, allProductionList, factoryCode, materialCode, productStatus);
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

    /**
     * 统计从startPlanDate开始，在allMonthPlanList中的所有计划量
     *
     * @param skuInfo            需统计Sku信息
     * @param startPlanDate      开始统计日
     * @param allMonthPlanList   所有计划
     * @param allLhDayAdjustList 所有月份-日计划调整量
     * @return
     */
    public static Map<YearMonth, Integer> statisticsSumPlanQtyBySku(FactoryMonthPlanProductionFinalResult skuInfo, Date startPlanDate, List<FactoryMonthPlanProductionFinalResult> allMonthPlanList, List<LhDayPlanAdjustVo> allLhDayAdjustList) {
        return MonthPlanSurplusCalculator.statisticsSumPlanQtyBySku(
                skuInfo, startPlanDate, allMonthPlanList, allLhDayAdjustList);
    }

}
