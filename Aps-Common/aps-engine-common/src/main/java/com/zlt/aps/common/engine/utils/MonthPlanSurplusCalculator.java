package com.zlt.aps.common.engine.utils;

import cn.hutool.core.date.DateUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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
 * 月计划硫化余量共享计算器
 * <p>
 * 从 aps-lh 的 {@code SkuMonthPlanCalculator} 提取的纯计算逻辑，不依赖任何 Service Context。
 * 硫化、成型等模块均可调用，确保余量计算口径一致。
 * </p>
 * <p>
 * 核心公式：硫化余量 = Max(计划量(断点日累加) + 超欠产 - 已完成量, 0)
 * </p>
 *
 * @author ZLT
 * @date 20260715
 */
@Slf4j
public class MonthPlanSurplusCalculator {

    private static final String MATERIAL_STATUS_KEY_SEPARATOR = "|*|";
    private static final String YES_CODE = "1";
    private static final String NO_CODE = "0";

    /**
     * 构建物料与产品状态复合键。
     *
     * <p>产品状态为空时保留空状态段，避免运行时同时出现物料单键与复合键两种格式。</p>
     *
     * @param materialCode  物料编码
     * @param productStatus 产品状态/计划类型
     * @return 物料与产品状态复合键
     */
    public static String buildMaterialStatusKey(String materialCode, String productStatus) {
        String normalizedMaterialCode = materialCode == null ? "" : materialCode.trim();
        String normalizedProductStatus = productStatus == null ? "" : productStatus.trim();
        return normalizedMaterialCode + MATERIAL_STATUS_KEY_SEPARATOR + normalizedProductStatus;
    }

    /**
     * 根据年、月、日，构造Date
     *
     * @param localDate 本地日期
     * @return Date对象
     */
    public static Date getDate(LocalDate localDate) {
        ZoneId zoneId = ZoneId.systemDefault();
        Instant instantTime = localDate.atStartOfDay(zoneId).toInstant();
        return new Date(instantTime.toEpochMilli());
    }

    /**
     * 将Date转换成LocalDate
     *
     * @param date Date对象
     * @return LocalDate
     */
    public static LocalDate getDate(Date date) {
        if (null == date) {
            return null;
        }
        ZoneId zoneId = ZoneId.systemDefault();
        return date.toInstant().atZone(zoneId).toLocalDate();
    }

    /**
     * 获取排产日期所在排产年份-月份
     *
     * @param productionDate 排产日期
     * @return 年-月
     */
    public static YearMonth getProductionYearAndMonth(Date productionDate) {
        //年份
        int year = DateUtil.year(productionDate);
        //月份 0~11，故而要+1
        int month = DateUtil.month(productionDate) + BigDecimal.ONE.intValue();
        return YearMonth.of(year, month);
    }

    /**
     * 判断当前排程周期是否存在跨月
     *
     * @param allProductionDateList 日排产周期日期集合
     * @return true=跨月, false=不跨月
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
     * 获取排产周期内第一个月份
     *
     * @param allProductionDateList 日排产周期日期集合
     * @return 第一个年-月
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
     * 获取排产周期内最后一个月份
     *
     * @param allProductionDateList 日排产周期日期集合
     * @return 最后一个年-月
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
     * 获取下一个年月
     *
     * @param allProductionDate 日排产周期日期集合
     * @return 下一个年-月
     */
    public static YearMonth getNextMonth(List<Date> allProductionDate) {
        if (CollectionUtils.isEmpty(allProductionDate)) {
            return null;
        }
        YearMonth firstYearMonth = getFirstYearMonth(allProductionDate);
        boolean isCrossMonth = isCrossMonthByProductionDateInfo(allProductionDate);
        YearMonth nextMonth;
        if (isCrossMonth) {
            nextMonth = getLastYearMonth(allProductionDate);
        } else {
            nextMonth = firstYearMonth.plusMonths(BigDecimal.ONE.longValue());
        }
        return nextMonth;
    }

    /**
     * 获取对应年、月的月计划排产计划
     *
     * @param allMonthPlanList       所有月计划信息
     * @param skuMonthProductionInfo 需要查找的SKU信息
     * @param yearMonth              年、月
     * @return 匹配的月计划记录，未找到返回null
     */
    public static FactoryMonthPlanProductionFinalResult getSkuYearMonthFinal(
            List<FactoryMonthPlanProductionFinalResult> allMonthPlanList,
            FactoryMonthPlanProductionFinalResult skuMonthProductionInfo, YearMonth yearMonth) {
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
                //不同SKU + 计划类型
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
     * 如果已经跨月，则前一个月的超欠产直接忽略；
     * 否则，如果isNextMonthFinal = true，则忽略；
     * 否则看第一月的欠产
     *
     * @param isNextMonthFinal  是否下个月已定稿
     * @param allProductionList 日排产周期
     * @param allMonthPlanList  所有月计划信息
     * @param skuInfo           SKU信息
     * @return 年-月 -> 超欠产量
     */
    public static Map<YearMonth, Integer> getOverdueProduction(boolean isNextMonthFinal, List<Date> allProductionList,
                                                               List<FactoryMonthPlanProductionFinalResult> allMonthPlanList,
                                                               FactoryMonthPlanProductionFinalResult skuInfo) {
        if (CollectionUtils.isEmpty(allMonthPlanList) || null == skuInfo) {
            return Collections.emptyMap();
        }
        Map<YearMonth, Integer> result = Maps.newHashMap();
        YearMonth firstMonth = getFirstYearMonth(allProductionList);
        Integer firstMonthOverdueQty = getOverdueProduction(allMonthPlanList, skuInfo, firstMonth);
        //如果跨月，还要看后面的
        if (isCrossMonthByProductionDateInfo(allProductionList)) {
            result.put(firstMonth, firstMonthOverdueQty);
            YearMonth lastMonth = getLastYearMonth(allProductionList);
            result.put(lastMonth, getOverdueProduction(allMonthPlanList, skuInfo, lastMonth));
            return result;
        }
        //不跨月，且下个月定稿
        if (isNextMonthFinal) {
            result.put(firstMonth, BigDecimal.ZERO.intValue());
            return result;
        }
        result.put(firstMonth, firstMonthOverdueQty);
        return result;
    }

    /**
     * 获取硫化余量
     * <p>
     * 硫化余量 = 计划量 - 已完成量 + 超欠产
     * </p>
     *
     * @param productionYearMonth  当前排产年-月
     * @param allProductionDate    日排产周期日
     * @param hasProductionPlanMap 排产周期内有计划量的月计划信息
     * @param monthOverdueQtyMap   年-月超欠产信息
     * @param yearMonthPlanQtyMap  年-月计划量信息
     * @param finishedQty          已完成量
     * @return 硫化余量
     */
    public static Integer getSurplusQty(YearMonth productionYearMonth, List<Date> allProductionDate,
                                        Map<YearMonth, FactoryMonthPlanProductionFinalResult> hasProductionPlanMap,
                                        Map<YearMonth, Integer> monthOverdueQtyMap,
                                        Map<YearMonth, Integer> yearMonthPlanQtyMap, Integer finishedQty) {
        if (null == finishedQty) {
            finishedQty = BigDecimal.ZERO.intValue();
        }
        YearMonth firstMonth = getFirstYearMonth(allProductionDate);
        boolean isCrossMonth = isCrossMonthByProductionDateInfo(allProductionDate);
        //当前排产日所在年月的上月超欠产
        Integer overdueQty = monthOverdueQtyMap.get(productionYearMonth);
        if (null == overdueQty) {
            overdueQty = BigDecimal.ZERO.intValue();
        }
        //排产周期内有计划量，不跨月
        if (!CollectionUtils.isEmpty(hasProductionPlanMap) && !isCrossMonth) {
            Integer planQty = yearMonthPlanQtyMap.get(productionYearMonth);
            if (null == planQty) {
                planQty = BigDecimal.ZERO.intValue();
            }
            return planQty - finishedQty + overdueQty;
        }
        //排产周期内有计划量，且排产周期跨月
        if (!CollectionUtils.isEmpty(hasProductionPlanMap) && isCrossMonth) {
//            boolean isCrossMonthPlanQty = isCrossMonthByCycleRange(productionYearMonth, hasProductionPlanMap);
            Integer sumPlanQty = sumQty(yearMonthPlanQtyMap);
//            //计划量跨月
//            if (isCrossMonthPlanQty) {
//                overdueQty = sumQty(monthOverdueQtyMap);
//                return sumPlanQty - finishedQty + overdueQty;
//            }
//            //计划量没有跨月
            return sumPlanQty - finishedQty + overdueQty;
        }
        //排产周期内没有计划量
        Integer productionYearMonthPlanQty;
        if (firstMonth.equals(productionYearMonth)) {
            productionYearMonthPlanQty = yearMonthPlanQtyMap.get(productionYearMonth);
        } else {
            productionYearMonthPlanQty = yearMonthPlanQtyMap.get(firstMonth);
        }
        if (null == productionYearMonthPlanQty) {
            productionYearMonthPlanQty = BigDecimal.ZERO.intValue();
        }
        int surplus;
        if (isCrossMonth) {
            overdueQty = monthOverdueQtyMap.get(productionYearMonth);
        } else {
            overdueQty = monthOverdueQtyMap.get(firstMonth);
        }
        surplus = productionYearMonthPlanQty - finishedQty + overdueQty;
        if (surplus <= BigDecimal.ZERO.intValue() && isCrossMonth) {
            YearMonth lastMonth = getLastYearMonth(allProductionDate);
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
    public static Map<YearMonth, Integer> getPlanQty(List<Date> allProductionDate,
                                                     List<FactoryMonthPlanProductionFinalResult> allMonthPlanList,
                                                     FactoryMonthPlanProductionFinalResult skuMonthProductionInfo, Integer startDay) {
        if (CollectionUtils.isEmpty(allProductionDate) || CollectionUtils.isEmpty(allMonthPlanList)
                || null == skuMonthProductionInfo || null == startDay) {
            return Collections.emptyMap();
        }
        if (startDay < BigDecimal.ONE.intValue()) {
            return Collections.emptyMap();
        }
        return getPlanQtyByMonthPlan(skuMonthProductionInfo, startDay, allProductionDate, allMonthPlanList);
    }

    /**
     * 统计从startPlanDate开始，在allMonthPlanList中SKU的所有计划量
     *
     * @param skuInfo          需统计SKU信息
     * @param startPlanDate    开始统计日
     * @param allMonthPlanList 所有计划
     * @return 年-月 -> 计划量
     */
    public static Map<YearMonth, Integer> statisticsSumPlanQtyBySku(FactoryMonthPlanProductionFinalResult skuInfo,
                                                                    Date startPlanDate,
                                                                    List<FactoryMonthPlanProductionFinalResult> allMonthPlanList) {
        if (null == skuInfo || null == startPlanDate || CollectionUtils.isEmpty(allMonthPlanList)) {
            return Collections.emptyMap();
        }
        List<FactoryMonthPlanProductionFinalResult> findPlanList = allMonthPlanList.stream()
                .filter(single -> single.getMaterialStatusKey().equals(skuInfo.getMaterialStatusKey()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(findPlanList)) {
            return Collections.emptyMap();
        }
        Map<YearMonth, Integer> sumMap = Maps.newHashMap();
        YearMonth firstMonth = getProductionYearAndMonth(startPlanDate);
        Integer startDay = getDate(startPlanDate).getDayOfMonth();
        findPlanList.forEach(singleMonthPlan -> {
            YearMonth productionYearMonth = YearMonth.of(singleMonthPlan.getYear(), singleMonthPlan.getMonth());
            Integer monthEndDay = productionYearMonth.lengthOfMonth();
            //同年-月
            Integer sumQty;
            if (productionYearMonth.equals(firstMonth)) {
                sumQty = statisticsPlanQtyEndDay(startDay, monthEndDay, singleMonthPlan);
            } else {
                //下一个年月
                sumQty = statisticsPlanQtyEndDay(BigDecimal.ONE.intValue(), monthEndDay, singleMonthPlan);
            }
            sumMap.put(productionYearMonth, sumQty);
        });
        return sumMap;
    }

    /**
     * 获取对应的排产计划（排产周期内有计划量的月计划）
     *
     * @param allMonthPlanList  所有排产计划
     * @param allProductionList 日排产周期信息
     * @param factoryCode       工厂
     * @param materialCode      物料
     * @param productStatus     计划类型
     * @return 年-月 -> 有计划量的月计划记录
     */
    public static Map<YearMonth, FactoryMonthPlanProductionFinalResult> getHasProductionPlan(
            List<FactoryMonthPlanProductionFinalResult> allMonthPlanList, List<Date> allProductionList,
            String factoryCode, String materialCode, String productStatus) {
        Map<YearMonth, List<Date>> yearMonthMap = getYearMonthProductionDateInfo(allProductionList);
        FactoryMonthPlanProductionFinalResult skuInfo = new FactoryMonthPlanProductionFinalResult();
        skuInfo.setFactoryCode(factoryCode);
        skuInfo.setMaterialCode(materialCode);
        skuInfo.setProductStatus(productStatus);
        return getHasProductionPlan(allMonthPlanList, yearMonthMap, skuInfo);
    }

    /**
     * 汇总月数据量
     *
     * @param needSumQtyMap 需汇总月数量集合
     * @return 汇总值
     */
    public static int sumQty(Map<YearMonth, Integer> needSumQtyMap) {
        if (CollectionUtils.isEmpty(needSumQtyMap)) {
            return BigDecimal.ZERO.intValue();
        }
        return needSumQtyMap.values().stream().mapToInt(Integer::intValue).sum();
    }

    // ==================== 以下为内部计算方法 ====================

    /**
     * 在排产周期内，计划是否跨月
     *
     * @param productionYearMonth  当前排产日所处年月
     * @param hasProductionPlanMap 周期内有计划的年月计划
     * @return
     */
    private static boolean isCrossMonthByCycleRange(YearMonth productionYearMonth, Map<YearMonth, FactoryMonthPlanProductionFinalResult> hasProductionPlanMap) {
        if (null == productionYearMonth || CollectionUtils.isEmpty(hasProductionPlanMap)) {
            return false;
        }
        Set<YearMonth> yearMonthSet = hasProductionPlanMap.keySet();
        if (yearMonthSet.size() <= BigDecimal.ONE.intValue()) {
            return false;
        }
        return hasProductionPlanMap.containsKey(productionYearMonth);
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
    private static Map<YearMonth, Integer> getPlanQtyByMonthPlan(FactoryMonthPlanProductionFinalResult skuMonthProductionInfo,
                                                                 Integer startDay, List<Date> allProductionList,
                                                                 List<FactoryMonthPlanProductionFinalResult> allMonthPlanList) {
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
            //日排产周期内都没有计划量——取第一个月的全部计划量
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
     * 根据排产日信息，获取在dateList中最晚出现计划量的日期
     * 1、以此日为起始，往后查找，直到第一个没有排产量的排产日
     * 以找到的排产日，统计从计划量起始日~找到的排产日前一个日的计划量
     * 2、如果在dateList中都没有计划量，则从dateList之后一个日开始，
     * 找到最早一段连续排产量的最后一个有计划量的排产日，统计从计划量起始日startDay~找到的排产日的计划量
     *
     * @param skuMonthProductionInfo Sku信息
     * @param startDay               计算计划量起始日
     * @param dateList               有计划量排产日集合
     * @return
     */
    private static Integer getEarliestContinuousPlanQty(FactoryMonthPlanProductionFinalResult skuMonthProductionInfo,
                                                        Integer startDay, List<Date> dateList) {
        if (null == skuMonthProductionInfo || CollectionUtils.isEmpty(dateList)) {
            return BigDecimal.ZERO.intValue();
        }
        //取得日期所在月的天数即年份、月份
        List<Integer> dayList = Lists.newArrayList();
        Set<YearMonth> yearMonthSet = Sets.newHashSet();
        dateList.forEach(date -> {
            int day = DateUtil.dayOfMonth(date);
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
            //都没有计划量，取排产日中最后一天
            Integer nextStartDay = dayList.get(dayList.size() - BigDecimal.ONE.intValue());
            Integer nextContinueEndDay = getEarliestContinuousEndDay(nextStartDay, realYearMonth, skuMonthProductionInfo);
            if (null == nextContinueEndDay) {
                return statisticsPlanQtyEndDay(startDay, nextStartDay, skuMonthProductionInfo);
            }
            return statisticsPlanQtyEndDay(startDay, nextContinueEndDay, skuMonthProductionInfo);
        }
        Integer earliestContinuousDay = getEarliestContinuousDay(lastDay, realYearMonth, skuMonthProductionInfo);
        //不能小于1
        if (null == earliestContinuousDay || earliestContinuousDay < BigDecimal.ONE.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        //统计计划量
        return statisticsPlanQtyEndDay(startDay, earliestContinuousDay, skuMonthProductionInfo);
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
    private static Map<YearMonth, FactoryMonthPlanProductionFinalResult> getHasProductionPlan(
            List<FactoryMonthPlanProductionFinalResult> allMonthPlanList, Map<YearMonth, List<Date>> yearMonthMap,
            FactoryMonthPlanProductionFinalResult skuMonthProductionInfo) {
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
        return dateList.stream()
                .map(DateUtil::dayOfMonth)
                .collect(Collectors.toList());
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
            Integer value = skuMonthProductionInfo.getDayQty(dateIndex);
            if (null == value || value <= BigDecimal.ZERO.intValue()) {
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
    private static Integer getEarliestContinuousDay(Integer startDay, YearMonth yearMonth,
                                                    FactoryMonthPlanProductionFinalResult skuMonthProductionInfo) {
        //不能小于1
        if (startDay < BigDecimal.ONE.intValue() || null == skuMonthProductionInfo || null == yearMonth) {
            return null;
        }
        Integer maxDay = yearMonth.lengthOfMonth();
        Integer earliestContinuousDay = startDay;
        for (; earliestContinuousDay <= maxDay; ) {
            Integer value = skuMonthProductionInfo.getDayQty(earliestContinuousDay);
            if (null == value || value <= BigDecimal.ZERO.intValue()) {
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
     * 获取从startDay开始，连续有计划量的最后一个排产日：
     * 即获取最早没有计划排产量的排产日的前一日
     *
     * @param startDay               开始日
     * @param yearMonth              年份-月份
     * @param skuMonthProductionInfo sku月排产信息
     * @return
     */
    private static Integer getEarliestContinuousEndDay(Integer startDay, YearMonth yearMonth,
                                                       FactoryMonthPlanProductionFinalResult skuMonthProductionInfo) {
        if (null == startDay || null == yearMonth || null == skuMonthProductionInfo) {
            return null;
        }
        Integer monthEndDay = yearMonth.lengthOfMonth();
        if (startDay >= monthEndDay) {
            return null;
        }
        Integer planStartDay = startDay;
        for (; planStartDay <= monthEndDay; ) {
            Integer value = skuMonthProductionInfo.getDayQty(planStartDay);
            if (null != value && value >= BigDecimal.ZERO.intValue()) {
                break;
            }
            planStartDay = planStartDay + BigDecimal.ONE.intValue();
        }
        if (planStartDay.equals(startDay) || planStartDay > monthEndDay) {
            return null;
        }
        return getEarliestContinuousDay(planStartDay, yearMonth, skuMonthProductionInfo);
    }

    /**
     * 统计从第startDay~endDay的所有计划量
     *
     * @param startDay               计算起始日
     * @param endDay                 结束统计日
     * @param skuMonthProductionInfo Sku月排产信息
     * @return
     */
    private static Integer statisticsPlanQtyEndDay(Integer startDay, Integer endDay,
                                                   FactoryMonthPlanProductionFinalResult skuMonthProductionInfo) {
        if (null == startDay || null == endDay || null == skuMonthProductionInfo) {
            return BigDecimal.ZERO.intValue();
        }
        if (startDay < BigDecimal.ONE.intValue() && endDay < BigDecimal.ONE.intValue() && startDay > endDay) {
            return BigDecimal.ZERO.intValue();
        }
        //统计计划量
        Integer planDay = startDay;
        Integer sumPlanQty = BigDecimal.ZERO.intValue();
        for (; planDay <= endDay; ) {
            Integer value = skuMonthProductionInfo.getDayQty(planDay);
            if (null != value && value >= BigDecimal.ZERO.intValue()) {
                sumPlanQty = sumPlanQty + value;
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
    private static Integer getOverdueProduction(List<FactoryMonthPlanProductionFinalResult> allMonthPlanList,
                                                FactoryMonthPlanProductionFinalResult skuInfo, YearMonth yearMonth) {
        FactoryMonthPlanProductionFinalResult lastMonthPlan = getSkuYearMonthFinal(allMonthPlanList, skuInfo, yearMonth);
        if (null == lastMonthPlan) {
            return BigDecimal.ZERO.intValue();
        }
        if (!YES_CODE.equals(lastMonthPlan.getLastMonthValidFlag())) {
            return BigDecimal.ZERO.intValue();
        }
        if (Objects.isNull(lastMonthPlan.getLastMonthOverdueQty())) {
            return BigDecimal.ZERO.intValue();
        }
        return lastMonthPlan.getLastMonthOverdueQty();
    }
}
