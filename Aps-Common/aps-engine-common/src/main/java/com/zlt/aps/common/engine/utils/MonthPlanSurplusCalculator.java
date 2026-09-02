package com.zlt.aps.common.engine.utils;

import cn.hutool.core.date.DateUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zlt.aps.common.engine.domain.*;
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
     * 硫化余量计算：
     * 1、月计划断开的定义：月计划前日有值，当日没有值，视为断开；示例：6.5（48） 6.6（空） 6.7（8），则断点日： 6.5
     * 2、为兼容月计划断开以及跨月的场景，硫化余量的计算： 日期定义示例：今天（6.8），排3天，6.8、6.9、6.10
     * 2.1）对于硫化排产3天内有出现月计划量的，获取max(3天内最晚出现月计划量的那天（X天）, 在（X天）向后间隔2天有再出现月计划量那天) 往后在月计划中找到断点；(注：间隔2天是参数)
     * 硫化余量 = 断点日（含）之前的计划量合计（1-断点日） - 已完成量 + 上月超欠产（有效标志=是）；
     * 示例：今天（6.8），6.8（48） 6.9（空）6.10（空）6.11（32） 6.12（18） 6.13（空），
     * 3天内最晚（6.10），间隔2天再出现月计划量的那天（6.11），二者取大，按大者查找断点日（6.12），
     * 则硫化余量=断点日（含）之前的计划量合计（1-12） - 已完成量 + 上月超欠产（有效标志=是） = 98 - 0 +0；
     * 2.1.1）若max(硫化排产3天内最晚出现月计划量的那天（X天）, 在（X天）向后间隔2天有再出现月计划量那天)，其若是跨月的，则按最晚跨月的那天往后在跨月的月计划中找到断点；
     * 硫化余量 = 当月的硫化余量+跨月断点日（含）之前的计划量合计（跨月1-跨月断点日）；
     * 示例：今天（6.29），6.29（48） 6.30（空）6.31（空）7.1（32） 7.2（18） 7.3（空），设当月的硫化余量 = 48；
     * 3天内最晚（6.29），间隔2天再出现月计划量（7.1），二者取大，按大者查找断点日，断点日（7.2），
     * 则硫化余量=当月的硫化余量+跨月断点日（含）之前的计划量合计（7.1-7.2）= 48 + 50 = 98；
     * 2.2）对于硫化排产3天内没有出现月计划量的：
     * 2.2.1）若当日之前还有余量，则硫化余量 = 当日（含）之前的计划量合计（1-当日） - 已完成量 + 上月超欠产（有效标志=是）；
     * 2.2.2）若当日之前没有余量，则按当日往后在当月计划中找到断点；
     * 硫化余量 = 断点日（含）之前的计划量合计（1-当月断点日） - 已完成量 + 上月超欠产（有效标志=是）
     *
     * @param productionDateInfo 排产日信息
     * @param calculateSkuInfo   排产Sku信息
     * @return
     */
    public static LhSurplusResultVo getSurplusInfo(LhSurplusProductionDayInfo productionDateInfo, LhSurplusSkuInfo calculateSkuInfo) {
        //校验参数
        if (null == productionDateInfo || null == calculateSkuInfo) {
            return null;
        }
        FactoryMonthPlanProductionFinalResult skuInfo = calculateSkuInfo.getSkuInfo();
        YearMonth productionYearMonth = productionDateInfo.getProductionYearMonth();
        Integer startDay = productionDateInfo.getStartDay();
        List<Date> realProductionCycleList = productionDateInfo.getRealProductionCycleList();
        Integer maxDiscontinueDays = productionDateInfo.getMaxDiscontinueDays();
        List<FactoryMonthPlanProductionFinalResult> allMonthPlanList = calculateSkuInfo.getAllMonthPlanList();
        Map<YearMonth, Integer> monthOverdueQtyMap = calculateSkuInfo.getMonthOverdueQtyMap();
        Integer finishedQty = calculateSkuInfo.getFinishedQty();
        List<LhDayPlanAdjustVo> allLhDayAdjustList = calculateSkuInfo.getAllLhDayAdjustList();
        if (null == productionYearMonth || CollectionUtils.isEmpty(realProductionCycleList) || null == skuInfo) {
            return null;
        }
        Map<YearMonth, FactoryMonthPlanProductionFinalResult> montPlanMap = getYearMonthPlan(allMonthPlanList, skuInfo);
        //下个月
        YearMonth nextMonth = productionYearMonth.plusMonths(BigDecimal.ONE.longValue());
        //20260817+ 硫化日计划调整信息
        YearMonthLhDayAdjustVo yearMonthLhDayAdjustInfo = getYearMonthLhDayAdjustInfo(skuInfo, allLhDayAdjustList);
        //周期排产日内信息
        Map<YearMonth, List<Date>> realProductionCycleMap = getYearMonthProductionDateInfo(realProductionCycleList);
        Map<YearMonth, FactoryMonthPlanProductionFinalResult> realProductionCycleMonthPlanMap = getHasProductionPlan(allMonthPlanList, realProductionCycleMap, skuInfo);
        LhSurplusResultVo surplusInfo = buildSurplusResult(skuInfo, productionYearMonth, startDay, maxDiscontinueDays, realProductionCycleMap, montPlanMap, yearMonthLhDayAdjustInfo, monthOverdueQtyMap, finishedQty);
        if (CollectionUtils.isEmpty(realProductionCycleMonthPlanMap) && surplusInfo.getSurplusQty() > BigDecimal.ZERO.intValue()) {
            //周期排产内没有计划量且有余量
            return surplusInfo;
        }
        //周期排产内有计划量或是没有计划也没有余量：需要扩展判断计划量的日期
        List<Date> calculateDayQtyList = getAllProductionDateByDiscontinueDays(realProductionCycleList, maxDiscontinueDays);
        Map<YearMonth, List<Date>> calculateProductionDayMap = getYearMonthProductionDateInfo(calculateDayQtyList);
        Map<YearMonth, FactoryMonthPlanProductionFinalResult> calculateProductionDayMonthPlanMap = getHasProductionPlan(allMonthPlanList, calculateProductionDayMap, skuInfo);
        //周期排产内有计划量，扩展到间隔天数，看符合条件的排产日信息
        Date lastDate = getLastHasPlanQtyDate(calculateDayQtyList, calculateProductionDayMonthPlanMap, calculateProductionDayMap, maxDiscontinueDays);
        boolean isFindNextMonth = isNextMonth(lastDate, calculateProductionDayMap, nextMonth);
        List<Date> effectiveDayQtyList = getEffectiveDateByLastDate(calculateDayQtyList, lastDate);
        Map<YearMonth, List<Date>> effectiveProductionDayMap = getYearMonthProductionDateInfo(effectiveDayQtyList);
        //第一个月
        FactoryMonthPlanProductionFinalResult firstMonthPlan = montPlanMap.get(productionYearMonth);
        List<Date> firstMonthDateList = effectiveProductionDayMap.get(productionYearMonth);
        Integer firstMonthEndDay = productionYearMonth.lengthOfMonth();
        Integer planQty = getMonthPlanQty(firstMonthPlan, startDay, firstMonthDateList, yearMonthLhDayAdjustInfo, productionYearMonth, maxDiscontinueDays);
        Integer sumQty = getMonthPlanSumQty(firstMonthPlan, productionYearMonth, startDay, firstMonthEndDay, yearMonthLhDayAdjustInfo);
        Integer monthOverdueQty = BigDecimal.ZERO.intValue();
        if (!CollectionUtils.isEmpty(monthOverdueQtyMap) && null != monthOverdueQtyMap.get(productionYearMonth)) {
            monthOverdueQty = monthOverdueQty + monthOverdueQtyMap.get(productionYearMonth);
        }
        if (!isFindNextMonth) {
            return new LhSurplusResultVo(skuInfo, monthOverdueQty, planQty, sumQty, finishedQty);
        }
        //第二个月
        FactoryMonthPlanProductionFinalResult nextMonthPlan = montPlanMap.get(nextMonth);
        List<Date> nextMonthDateList = effectiveProductionDayMap.get(nextMonth);
        Integer nextMonthEndDay = productionYearMonth.lengthOfMonth();
        planQty = planQty + getMonthPlanQty(nextMonthPlan, BigDecimal.ONE.intValue(), nextMonthDateList, yearMonthLhDayAdjustInfo, nextMonth, maxDiscontinueDays);
        sumQty = sumQty + getMonthPlanSumQty(nextMonthPlan, nextMonth, BigDecimal.ONE.intValue(), nextMonthEndDay, yearMonthLhDayAdjustInfo);
        if (!CollectionUtils.isEmpty(monthOverdueQtyMap) && null != monthOverdueQtyMap.get(nextMonth)) {
            monthOverdueQty = monthOverdueQty + monthOverdueQtyMap.get(nextMonth);
        }
        return new LhSurplusResultVo(skuInfo, monthOverdueQty, planQty, sumQty, finishedQty);
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
     * 获取对应Sku的年-月硫化日计划调整量
     *
     * @param skuMonthProductionInfo
     * @param allLhDayAdjustList
     * @return
     */
    public static Map<YearMonth, Integer> getYearMonthLhDayAdjustQty(FactoryMonthPlanProductionFinalResult skuMonthProductionInfo, List<LhDayPlanAdjustVo> allLhDayAdjustList) {
        if (null == skuMonthProductionInfo || CollectionUtils.isEmpty(allLhDayAdjustList)) {
            return Collections.emptyMap();
        }
        Map<String, YearMonthLhDayAdjustVo> skuYearMonthLhDayAdjustMap = getLhDayPlanAdjustInfo(allLhDayAdjustList);
        return getYearMonthLhDayAdjustQty(skuMonthProductionInfo, skuYearMonthLhDayAdjustMap);
    }

    /**
     * 获取硫化日计划年-月的硫化日计划调整信息
     * key : 物料编码 + 产品状态 value：年-月的调整量
     *
     * @param allLhDayAdjustList
     * @return
     */
    public static Map<String, YearMonthLhDayAdjustVo> getLhDayPlanAdjustInfo(List<LhDayPlanAdjustVo> allLhDayAdjustList) {
        if (CollectionUtils.isEmpty(allLhDayAdjustList)) {
            return Collections.emptyMap();
        }
        List<YearMonthLhDayAdjustVo> allSkuYearMonthLhDayAdjustInfo = getLhDayAdjustByYearMonth(allLhDayAdjustList);
        if (CollectionUtils.isEmpty(allSkuYearMonthLhDayAdjustInfo)) {
            return Collections.emptyMap();
        }
        Map<String, YearMonthLhDayAdjustVo> result = Maps.newHashMap();
        allSkuYearMonthLhDayAdjustInfo.forEach(singleSkuInfo -> result.put(singleSkuInfo.getMaterialStatusKey(), singleSkuInfo));
        return result;
    }

    /**
     * 获取对应Sku的年-月硫化日计划调整量
     *
     * @param skuMonthProductionInfo
     * @param skuYearMonthLhDayAdjustMap
     * @return
     */
    public static Map<YearMonth, Integer> getYearMonthLhDayAdjustQty(FactoryMonthPlanProductionFinalResult skuMonthProductionInfo, Map<String, YearMonthLhDayAdjustVo> skuYearMonthLhDayAdjustMap) {
        if (null == skuMonthProductionInfo || CollectionUtils.isEmpty(skuYearMonthLhDayAdjustMap)) {
            return Collections.emptyMap();
        }
        YearMonthLhDayAdjustVo yearMonthLhDayAdjustInfo = skuYearMonthLhDayAdjustMap.get(skuMonthProductionInfo.getMaterialStatusKey());
        if (null == yearMonthLhDayAdjustInfo) {
            return Collections.emptyMap();
        }
        Map<YearMonth, Integer> yearMonthAdjustQtyMap = yearMonthLhDayAdjustInfo.getYearMonthDayLhAdjustQtyMap();
        if (CollectionUtils.isEmpty(yearMonthAdjustQtyMap)) {
            return Collections.emptyMap();
        }
        return yearMonthAdjustQtyMap;
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
     * @param yearMonthPlanQtyMap  年-月计划量信息(已经包含了对应的日计划调整量)
     * @param finishedQty          已完成量
     * @return 硫化余量
     */
    public static Integer getSurplusQty(YearMonth productionYearMonth,
                                        List<Date> allProductionDate,
                                        Map<YearMonth, FactoryMonthPlanProductionFinalResult> hasProductionPlanMap,
                                        Map<YearMonth, Integer> monthOverdueQtyMap,
                                        Map<YearMonth, Integer> yearMonthPlanQtyMap,
                                        Integer finishedQty) {
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
        if (!CollectionUtils.isEmpty(hasProductionPlanMap) && !isCrossMonth) {
            //排产周期内有计划量，不跨月
            Integer planQty = yearMonthPlanQtyMap.get(productionYearMonth);
            if (null == planQty) {
                planQty = BigDecimal.ZERO.intValue();
            }
            return planQty - finishedQty + overdueQty;
        }
        if (!CollectionUtils.isEmpty(hasProductionPlanMap) && isCrossMonth) {
            //排产周期内有计划量，且排产周期跨月
            Integer sumPlanQty = sumQty(yearMonthPlanQtyMap);
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
     * @param maxDiscontinueDays     排产周期内间隔天数
     * @param allMonthPlanList       所有月计划量
     * @param allLhDayAdjustList     所有月份的硫化日计划调整量
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
        if (CollectionUtils.isEmpty(allProductionDate) || CollectionUtils.isEmpty(allMonthPlanList)
                || null == skuMonthProductionInfo || null == startDay) {
            return Collections.emptyMap();
        }
        if (startDay < BigDecimal.ONE.intValue()) {
            return Collections.emptyMap();
        }
        return getPlanQtyByMonthPlan(skuMonthProductionInfo, startDay, allProductionDate, maxDiscontinueDays, allMonthPlanList, allLhDayAdjustList);
    }

    /**
     * 统计从startPlanDate开始，在allMonthPlanList中SKU的所有计划量
     *
     * @param skuInfo            需统计SKU信息
     * @param startPlanDate      开始统计日
     * @param allMonthPlanList   所有计划
     * @param allLhDayAdjustList 所有月份-日计划调整量
     * @return 年-月 -> 计划量
     */
    public static Map<YearMonth, Integer> statisticsSumPlanQtyBySku(FactoryMonthPlanProductionFinalResult skuInfo,
                                                                    Date startPlanDate,
                                                                    List<FactoryMonthPlanProductionFinalResult> allMonthPlanList,
                                                                    List<LhDayPlanAdjustVo> allLhDayAdjustList) {
        if (null == skuInfo || null == startPlanDate || CollectionUtils.isEmpty(allMonthPlanList)) {
            return Collections.emptyMap();
        }
        List<FactoryMonthPlanProductionFinalResult> findPlanList = allMonthPlanList.stream()
                .filter(single -> single.getMaterialStatusKey().equals(skuInfo.getMaterialStatusKey()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(findPlanList)) {
            return Collections.emptyMap();
        }
        YearMonthLhDayAdjustVo yearMonthLhDayAdjust = getYearMonthLhDayAdjustInfo(skuInfo, allLhDayAdjustList);
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
            //20260817+ 增加日计划的调整量
            sumQty = sumQty + getYearMonthLhDayAdjustQty(yearMonthLhDayAdjust, productionYearMonth);
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
     * 根据信息构建某个月份的余量信息
     *
     * @param skuInfo                  排产Sku信息
     * @param productionYearMonth      排产月份
     * @param startDay                 计算起始日，正常为1
     * @param maxDiscontinueDays       最大间隔天数
     * @param monthProductionDateMap   月份排产日信息
     * @param monthPlanMap             月份计划
     * @param yearMonthLhDayAdjustInfo 日硫化调整信息
     * @param monthOverdueQtyMap       超欠产
     * @param finishedQty              完成量
     * @return
     */
    private static LhSurplusResultVo buildSurplusResult(FactoryMonthPlanProductionFinalResult skuInfo,
                                                        YearMonth productionYearMonth,
                                                        Integer startDay,
                                                        Integer maxDiscontinueDays,
                                                        Map<YearMonth, List<Date>> monthProductionDateMap,
                                                        Map<YearMonth, FactoryMonthPlanProductionFinalResult> monthPlanMap,
                                                        YearMonthLhDayAdjustVo yearMonthLhDayAdjustInfo,
                                                        Map<YearMonth, Integer> monthOverdueQtyMap,
                                                        Integer finishedQty) {
        FactoryMonthPlanProductionFinalResult monthPlan = monthPlanMap.get(productionYearMonth);
        List<Date> monthProductionDateList = monthProductionDateMap.get(productionYearMonth);
        Integer monthEndDay = productionYearMonth.lengthOfMonth();
        Integer planQty = getMonthPlanQty(monthPlan, startDay, monthProductionDateList, yearMonthLhDayAdjustInfo, productionYearMonth, maxDiscontinueDays);
        Integer sumQty = getMonthPlanSumQty(monthPlan, productionYearMonth, startDay, monthEndDay, yearMonthLhDayAdjustInfo);
        Integer monthOverdueQty = BigDecimal.ZERO.intValue();
        if (!CollectionUtils.isEmpty(monthOverdueQtyMap) && null != monthOverdueQtyMap.get(productionYearMonth)) {
            monthOverdueQty = monthOverdueQtyMap.get(productionYearMonth);
        }
        return new LhSurplusResultVo(skuInfo, monthOverdueQty, planQty, sumQty, finishedQty);
    }

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
     * @param maxDiscontinueDays     最大允许的间隔天数
     * @param allMonthPlanList       所有月排产计划
     * @param allLhDayAdjustList     所有对应年-月日计划调整量信息
     * @return
     */
    private static Map<YearMonth, Integer> getPlanQtyByMonthPlan(FactoryMonthPlanProductionFinalResult skuMonthProductionInfo,
                                                                 Integer startDay,
                                                                 List<Date> allProductionList,
                                                                 Integer maxDiscontinueDays,
                                                                 List<FactoryMonthPlanProductionFinalResult> allMonthPlanList,
                                                                 List<LhDayPlanAdjustVo> allLhDayAdjustList) {
        if (null == skuMonthProductionInfo || CollectionUtils.isEmpty(allProductionList) || CollectionUtils.isEmpty(allMonthPlanList)) {
            return Collections.emptyMap();
        }
        //20260817+ 硫化日计划调整信息
        YearMonthLhDayAdjustVo yearMonthLhDayAdjustInfo = getYearMonthLhDayAdjustInfo(skuMonthProductionInfo, allLhDayAdjustList);
        if (!isCrossMonthByProductionDateInfo(allProductionList)) {
            //非跨月
            YearMonth yearMonth = getFirstYearMonth(allProductionList);
            FactoryMonthPlanProductionFinalResult skuYearMonth = getSkuYearMonthFinal(allMonthPlanList, skuMonthProductionInfo, yearMonth);
            if (null == skuYearMonth) {
                return Collections.emptyMap();
            }
            Map<YearMonth, Integer> result = Maps.newHashMap();
            Integer planQty = getEarliestContinuousPlanQty(skuYearMonth, startDay, allProductionList, maxDiscontinueDays);
            //20260817+ 增加日计划的调整量
            planQty = planQty + getYearMonthLhDayAdjustQty(yearMonthLhDayAdjustInfo, yearMonth);
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
            //20260817+ 增加日计划的调整量
            planQty = planQty + getYearMonthLhDayAdjustQty(yearMonthLhDayAdjustInfo, firstYearMonth);
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
            boolean isFindNextMonth = isNextMonthPlanQty(allProductionList, lastYearMonth, yearMonthSkuProductionMap, yearMonthMap, maxDiscontinueDays);
            //当月计划量
            FactoryMonthPlanProductionFinalResult skuYearMonth = getSkuYearMonthFinal(allMonthPlanList, skuMonthProductionInfo, firstYearMonth);
            if (null != skuYearMonth) {
                Integer firstPlanQty = statisticsPlanQtyEndDay(startDay, firstYearMonth.lengthOfMonth(), skuYearMonth);
                //20260817+ 增加日计划的调整量
                firstPlanQty = firstPlanQty + getYearMonthLhDayAdjustQty(yearMonthLhDayAdjustInfo, firstYearMonth);
                result.put(firstYearMonth, firstPlanQty);
            }
            if (!isLimit(maxDiscontinueDays) || isFindNextMonth) {
                //跨月计划量，到新断点为止
                FactoryMonthPlanProductionFinalResult nextYearMonth = yearMonthSkuProductionMap.get(lastYearMonth);
                Integer nextPlanQty = getEarliestContinuousPlanQty(nextYearMonth, BigDecimal.ONE.intValue(), yearMonthMap.get(lastYearMonth), maxDiscontinueDays);
                //20260817+ 增加日计划的调整量
                nextPlanQty = nextPlanQty + getYearMonthLhDayAdjustQty(yearMonthLhDayAdjustInfo, lastYearMonth);
                result.put(lastYearMonth, nextPlanQty);
            }
            return result;
        }
        //跨月没有计划量，只有当月有计划量
        FactoryMonthPlanProductionFinalResult firstMonthInfo = yearMonthSkuProductionMap.get(firstYearMonth);
        Integer planQty = getEarliestContinuousPlanQty(firstMonthInfo, startDay, yearMonthMap.get(firstYearMonth), maxDiscontinueDays);
        //20260817+ 增加日计划的调整量
        planQty = planQty + getYearMonthLhDayAdjustQty(yearMonthLhDayAdjustInfo, firstYearMonth);
        result.put(firstYearMonth, planQty);
        return result;
    }

    /**
     * 20260830+
     * 得到余量计划的排产计划量
     *
     * @param skuYearMonth             月份计划
     * @param startDay                 计划计算起始日
     * @param monthProductionDateList  对应月排产日集合
     * @param yearMonthLhDayAdjustInfo 日硫化计划日调整信息
     * @param yearMonth                排产月份
     * @param maxDiscontinueDays       最大间隔天数
     * @return
     */
    private static Integer getMonthPlanQty(FactoryMonthPlanProductionFinalResult skuYearMonth, Integer startDay, List<Date> monthProductionDateList, YearMonthLhDayAdjustVo yearMonthLhDayAdjustInfo, YearMonth yearMonth, Integer maxDiscontinueDays) {
        if (null == skuYearMonth) {
            return BigDecimal.ZERO.intValue();
        }
        Integer planQty = getEarliestContinuousPlanQty(skuYearMonth, startDay, monthProductionDateList, maxDiscontinueDays);
        //增加日计划的调整量
        planQty = planQty + getYearMonthLhDayAdjustQty(yearMonthLhDayAdjustInfo, yearMonth);
        return planQty;
    }

    /**
     * 获取从startDay~endDay的计划量总和
     * 包含对应年月的日硫化计划调整量
     *
     * @param skuMonthProductionInfo   月份计划
     * @param yearMonth                年月
     * @param startDay                 开始日
     * @param endDay                   结束日
     * @param yearMonthLhDayAdjustInfo 日硫化计划调整
     * @return
     */
    private static Integer getMonthPlanSumQty(FactoryMonthPlanProductionFinalResult skuMonthProductionInfo,
                                              YearMonth yearMonth,
                                              Integer startDay,
                                              Integer endDay,
                                              YearMonthLhDayAdjustVo yearMonthLhDayAdjustInfo) {
        //同年-月
        Integer sumQty = statisticsPlanQtyEndDay(startDay, endDay, skuMonthProductionInfo);
        //增加日计划的调整量
        sumQty = sumQty + getYearMonthLhDayAdjustQty(yearMonthLhDayAdjustInfo, yearMonth);
        return sumQty;
    }

    /**
     * 判断符合条件最后一个计划量的排产日是否为下一个月
     *
     * @param allProductionList         所有排产日
     * @param lastYearMonth             跨月时，下一个月
     * @param yearMonthSkuProductionMap 年-月的月份计划
     * @param yearMonthMap              年-月的排产日集合
     * @param maxDiscontinueDays        最大间隔日期
     * @return
     */
    private static boolean isNextMonthPlanQty(List<Date> allProductionList, YearMonth lastYearMonth, Map<YearMonth, FactoryMonthPlanProductionFinalResult> yearMonthSkuProductionMap, Map<YearMonth, List<Date>> yearMonthMap, Integer maxDiscontinueDays) {
        if (!isLimit(maxDiscontinueDays)) {
            return false;
        }
        if (CollectionUtils.isEmpty(allProductionList) || CollectionUtils.isEmpty(yearMonthSkuProductionMap) || CollectionUtils.isEmpty(yearMonthMap)) {
            return false;
        }
        Map<Date, Integer> dayPlanQtyMap = getAllProductionDatePlanQty(yearMonthSkuProductionMap, yearMonthMap);
        if (CollectionUtils.isEmpty(dayPlanQtyMap)) {
            return false;
        }
        Date lastDate = getLastDate(maxDiscontinueDays, allProductionList, dayPlanQtyMap);
        return isNextMonth(lastDate, yearMonthMap, lastYearMonth);
    }

    /**
     * 获取符合条件的最后一个排产日
     *
     * @param allProductionList         所有排产日集合
     * @param yearMonthSkuProductionMap 月份排产集合
     * @param yearMonthMap              年月对应的排产日信息
     * @param maxDiscontinueDays        连续隔离天数
     * @return
     */
    private static Date getLastHasPlanQtyDate(List<Date> allProductionList, Map<YearMonth, FactoryMonthPlanProductionFinalResult> yearMonthSkuProductionMap, Map<YearMonth, List<Date>> yearMonthMap, Integer maxDiscontinueDays) {
        if (CollectionUtils.isEmpty(allProductionList) || CollectionUtils.isEmpty(yearMonthSkuProductionMap) || CollectionUtils.isEmpty(yearMonthMap)) {
            return null;
        }
        Map<Date, Integer> dayPlanQtyMap = getAllProductionDatePlanQty(yearMonthSkuProductionMap, yearMonthMap);
        if (CollectionUtils.isEmpty(dayPlanQtyMap)) {
            return null;
        }
        //获取符合条件的最后一个有计划量的排产日
        Date lastDate;
        if (isLimit(maxDiscontinueDays)) {
            lastDate = getLastDate(maxDiscontinueDays, allProductionList, dayPlanQtyMap);
        } else {
            lastDate = getLastDate(allProductionList, dayPlanQtyMap);
        }
        return lastDate;
    }

    /**
     * 最后一个排产日是否下个月
     *
     * @param lastDate     最后一个排产日
     * @param yearMonthMap 年月对应排产日信息
     * @param nextMonth    下个月信息
     * @return
     */
    private static boolean isNextMonth(Date lastDate, Map<YearMonth, List<Date>> yearMonthMap, YearMonth nextMonth) {
        if (null == lastDate || CollectionUtils.isEmpty(yearMonthMap) || null == nextMonth) {
            return false;
        }
        YearMonth yearMonth = null;
        for (Map.Entry<YearMonth, List<Date>> entry : yearMonthMap.entrySet()) {
            YearMonth findYearMonth = entry.getKey();
            List<Date> dateList = entry.getValue();
            if (CollectionUtils.isEmpty(dateList)) {
                continue;
            }
            List<Date> findList = dateList.stream().filter(singleDate -> singleDate.equals(lastDate)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(findList)) {
                continue;
            }
            yearMonth = findYearMonth;
            break;
        }
        return yearMonth.equals(nextMonth);
    }

    /**
     * 获取排产周期每日的计划量
     *
     * @param yearMonthSkuProductionMap 月份计划
     * @param yearMonthMap              排产周期日信息
     * @return
     */
    private static Map<Date, Integer> getAllProductionDatePlanQty(Map<YearMonth, FactoryMonthPlanProductionFinalResult> yearMonthSkuProductionMap, Map<YearMonth, List<Date>> yearMonthMap) {
        if (CollectionUtils.isEmpty(yearMonthSkuProductionMap) || CollectionUtils.isEmpty(yearMonthMap)) {
            return Collections.emptyMap();
        }
        Map<Date, Integer> dayProductionPlanQtyMap = Maps.newHashMap();
        yearMonthMap.forEach((yearMonth, dateList) -> {
            if (CollectionUtils.isEmpty(dateList)) {
                return;
            }
            FactoryMonthPlanProductionFinalResult skuMonthProductionInfo = yearMonthSkuProductionMap.get(yearMonth);
            if (null == skuMonthProductionInfo) {
                return;
            }
            dateList.forEach(date -> {
                int dateIndex = DateUtil.dayOfMonth(date);
                Integer planQty = skuMonthProductionInfo.getDayQty(dateIndex);
                dayProductionPlanQtyMap.put(date, planQty);
            });
        });
        if (CollectionUtils.isEmpty(dayProductionPlanQtyMap)) {
            return Collections.emptyMap();
        }
        return dayProductionPlanQtyMap;
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
     * @param maxDiscontinueDays     周期日最大可允许间隔天数
     * @return
     */
    private static Integer getEarliestContinuousPlanQty(FactoryMonthPlanProductionFinalResult skuMonthProductionInfo,
                                                        Integer startDay,
                                                        List<Date> dateList,
                                                        Integer maxDiscontinueDays) {
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
        Integer lastDay = getLastHasPlanQtyDay(dayList, skuMonthProductionInfo, maxDiscontinueDays);
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
     * 获取Sku的年-月硫化日计划调整信息
     *
     * @param skuMonthProductionInfo
     * @param allLhDayAdjustList     所有Sku年月的硫化日计划调整信息
     * @return
     */
    private static YearMonthLhDayAdjustVo getYearMonthLhDayAdjustInfo(FactoryMonthPlanProductionFinalResult skuMonthProductionInfo, List<LhDayPlanAdjustVo> allLhDayAdjustList) {
        if (CollectionUtils.isEmpty(allLhDayAdjustList) || null == skuMonthProductionInfo) {
            return null;
        }
        List<YearMonthLhDayAdjustVo> allSkuYearMonthLhDayAdjustInfo = getLhDayAdjustByYearMonth(allLhDayAdjustList);
        if (CollectionUtils.isEmpty(allSkuYearMonthLhDayAdjustInfo)) {
            return null;
        }
        List<YearMonthLhDayAdjustVo> skuAdjustInfo = allSkuYearMonthLhDayAdjustInfo.stream().filter(singleSku -> singleSku.getMaterialStatusKey().equals(skuMonthProductionInfo.getMaterialStatusKey())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(skuAdjustInfo)) {
            return null;
        }
        return skuAdjustInfo.get(BigDecimal.ZERO.intValue());
    }

    /**
     * 获取某个年月的日计划调整量
     *
     * @param yearMonthLhDayAdjustInfo
     * @param yearMonth
     * @return
     */
    private static Integer getYearMonthLhDayAdjustQty(YearMonthLhDayAdjustVo yearMonthLhDayAdjustInfo, YearMonth yearMonth) {
        if (null == yearMonthLhDayAdjustInfo || null == yearMonth) {
            return BigDecimal.ZERO.intValue();
        }
        return yearMonthLhDayAdjustInfo.getYearMonthDayLhAdjustQty(yearMonth);
    }

    /**
     * 获取硫化日计划调整，按年-月 进行汇总
     *
     * @param allLhDayAdjustList
     * @return
     */
    private static List<YearMonthLhDayAdjustVo> getLhDayAdjustByYearMonth(List<LhDayPlanAdjustVo> allLhDayAdjustList) {
        if (CollectionUtils.isEmpty(allLhDayAdjustList)) {
            return Collections.emptyList();
        }
        Map<String, List<LhDayPlanAdjustVo>> skuAndStatusGroup = allLhDayAdjustList.stream().collect(Collectors.groupingBy(LhDayPlanAdjustVo::getMaterialStatusKey));
        List<YearMonthLhDayAdjustVo> groupResult = Lists.newArrayList();
        skuAndStatusGroup.forEach((groupKey, adjustList) -> {
            if (CollectionUtils.isEmpty(adjustList)) {
                return;
            }
            LhDayPlanAdjustVo skuInfo = adjustList.get(BigDecimal.ZERO.intValue());
            YearMonthLhDayAdjustVo yearMonthLhDayAdjust = new YearMonthLhDayAdjustVo();
            yearMonthLhDayAdjust.setMaterialCode(skuInfo.getMaterialCode());
            yearMonthLhDayAdjust.setMaterialDesc(skuInfo.getMaterialDesc());
            yearMonthLhDayAdjust.setProductStatus(skuInfo.getProductStatus());
            Map<YearMonth, List<LhDayPlanAdjustVo>> yearMonthGroup = adjustList.stream().collect(Collectors.groupingBy(LhDayPlanAdjustVo::getYearMonth));
            Map<YearMonth, Integer> yearMonthAdjustQtyMap = Maps.newHashMap();
            yearMonthGroup.forEach((yearMonth, yearMonthAdjustList) -> {
                if (CollectionUtils.isEmpty(yearMonthAdjustList)) {
                    return;
                }
                Integer yearMonthAdjustQty = yearMonthAdjustList.stream().mapToInt(LhDayPlanAdjustVo::getPlanQtyValue).sum();
                yearMonthAdjustQtyMap.put(yearMonth, yearMonthAdjustQty);
            });
            yearMonthLhDayAdjust.setYearMonthDayLhAdjustQtyMap(yearMonthAdjustQtyMap);
            groupResult.add(yearMonthLhDayAdjust);
        });
        if (CollectionUtils.isEmpty(groupResult)) {
            return Collections.emptyList();
        }
        return groupResult;
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
    private static Map<YearMonth, FactoryMonthPlanProductionFinalResult> getHasProductionPlan(List<FactoryMonthPlanProductionFinalResult> allMonthPlanList,
                                                                                              Map<YearMonth, List<Date>> yearMonthMap,
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
     * 获取对应排产月份的月计划数据
     *
     * @param allMonthPlanList       所有计划信息
     * @param skuMonthProductionInfo 排产Sku信息
     * @return
     */
    private static Map<YearMonth, FactoryMonthPlanProductionFinalResult> getYearMonthPlan(List<FactoryMonthPlanProductionFinalResult> allMonthPlanList,
                                                                                          FactoryMonthPlanProductionFinalResult skuMonthProductionInfo) {
        if (null == skuMonthProductionInfo || CollectionUtils.isEmpty(allMonthPlanList)) {
            return Collections.emptyMap();
        }
        List<FactoryMonthPlanProductionFinalResult> findSkuList = allMonthPlanList.stream().filter(singleSkuMonthPlan -> skuMonthProductionInfo.getMaterialStatusKey().equals(singleSkuMonthPlan.getMaterialStatusKey())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(findSkuList)) {
            return Collections.emptyMap();
        }
        Map<YearMonth, FactoryMonthPlanProductionFinalResult> monthPlanMap = Maps.newHashMap();
        findSkuList.forEach(singleSkuMonth -> {
            YearMonth yearMonth = YearMonth.of(singleSkuMonth.getYear(), singleSkuMonth.getMonth());
            monthPlanMap.put(yearMonth, singleSkuMonth);
        });
        if (CollectionUtils.isEmpty(monthPlanMap)) {
            return Collections.emptyMap();
        }
        return monthPlanMap;
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
     * @param maxDiscontinueDays     排产日集合内最大间隔天数
     * @return
     */
    private static Integer getLastHasPlanQtyDay(List<Integer> dayList, FactoryMonthPlanProductionFinalResult skuMonthProductionInfo, Integer maxDiscontinueDays) {
        if (null == maxDiscontinueDays || maxDiscontinueDays <= BigDecimal.ZERO.intValue()) {
            return getLastHasPlanQtyDay(dayList, skuMonthProductionInfo);
        }
        if (null == skuMonthProductionInfo || CollectionUtils.isEmpty(dayList)) {
            return null;
        }
        Integer stepSize = maxDiscontinueDays + BigDecimal.ONE.intValue();
        Integer maxDateIndex = dayList.get(dayList.size() - BigDecimal.ONE.intValue());
        //取得有计划量的最后一天
        Integer lastDay = null;
        for (Integer dateIndex : dayList) {
            Integer value = skuMonthProductionInfo.getDayQty(dateIndex);
            boolean isFindNextDay = hasFindNextDay(dateIndex, stepSize, maxDateIndex, skuMonthProductionInfo);
            // 只有当前排产日确实有计划量时才记录为"最后一个有计划量日"，
            // 否则窗口内无计划量时会把窗口末日误判为有计划量日，漏走"往后找断点"分支，
            // 导致计划起始日在排产窗口之后的物料硫化余量被算成 0。
            if (null != value && value > BigDecimal.ZERO.intValue() && (null == lastDay || lastDay < dateIndex)) {
                lastDay = dateIndex;
            }
            // 后面间隔天数内没有计划，无需再看后续排产日
            if (!isFindNextDay) {
                break;
            }
        }
        return lastDay;
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
     * 查找符合条件的最后一天
     *
     * @param maxDiscontinueDays 最大允许间隔天数
     * @param allProductionList  排产周期排产日集合
     * @param dayPlanQtyMap      排产周期日计划量集合
     * @return
     */
    private static Date getLastDate(Integer maxDiscontinueDays, List<Date> allProductionList, Map<Date, Integer> dayPlanQtyMap) {
        Integer stepSize = maxDiscontinueDays + BigDecimal.ONE.intValue();
        //日期从早到晚-升序
        allProductionList.sort(null);
        Integer lastDayIndex = null;
        Integer maxDateIndex = allProductionList.size();
        //从第一天开始
        for (Integer dayIndex = BigDecimal.ZERO.intValue(); dayIndex < maxDateIndex; dayIndex++) {
            Date currentDate = allProductionList.get(dayIndex);
            Integer value = dayPlanQtyMap.get(currentDate);
            //是否需要继续下一天
            boolean isFindNextDay = hasFindNextDay(dayIndex, stepSize, allProductionList, dayPlanQtyMap);
            if (!isFindNextDay) {
                //后面间隔天数内没有计划
                if (null == value || value <= BigDecimal.ZERO.intValue()) {
                    break;
                }
                lastDayIndex = dayIndex;
                break;
            }
            //后面间隔天数内有计划
            if (null == lastDayIndex || lastDayIndex < dayIndex) {
                lastDayIndex = dayIndex;
            }
        }
        if (null == lastDayIndex) {
            return null;
        }
        return allProductionList.get(lastDayIndex);
    }

    /**
     * 从allProductionList中取得最后一个有计划量的排产日
     *
     * @param allProductionList 所有排产日
     * @param dayPlanQtyMap     各排产日的计划量
     * @return
     */
    private static Date getLastDate(List<Date> allProductionList, Map<Date, Integer> dayPlanQtyMap) {
        if (CollectionUtils.isEmpty(allProductionList) || CollectionUtils.isEmpty(dayPlanQtyMap)) {
            return null;
        }
        //日期从早到晚升序排序
        allProductionList.sort(null);
        Integer lastDayIndex = null;
        Integer maxDateIndex = allProductionList.size();
        //从第一天开始
        for (Integer dayIndex = BigDecimal.ZERO.intValue(); dayIndex < maxDateIndex; dayIndex++) {
            Date currentDate = allProductionList.get(dayIndex);
            Integer value = dayPlanQtyMap.get(currentDate);
            if (null == value || value <= BigDecimal.ZERO.intValue()) {
                continue;
            }
            if (null == lastDayIndex || lastDayIndex < dayIndex) {
                lastDayIndex = dayIndex;
            }
        }
        if (null == lastDayIndex) {
            return null;
        }
        return allProductionList.get(lastDayIndex);
    }

    /**
     * 获取是否需要查找下一天
     * 从当前天看下一天，连续看stepSize天，如果有值则需要看下一天
     * 否则无需看下一天
     *
     * @param currentDateIndex       当前天
     * @param maxDiscontinueDays     需要往后看的天数
     * @param maxDateIndex           最大天
     * @param skuMonthProductionInfo 日排产信息对象
     * @return
     */
    private static boolean hasFindNextDay(Integer currentDateIndex, Integer maxDiscontinueDays, Integer maxDateIndex, FactoryMonthPlanProductionFinalResult skuMonthProductionInfo) {
        if (null == currentDateIndex || null == maxDateIndex || null == maxDiscontinueDays || null == skuMonthProductionInfo) {
            return false;
        }
        Integer currentValue = skuMonthProductionInfo.getDayQty(currentDateIndex);
        if (null == currentValue || currentValue <= BigDecimal.ZERO.intValue()) {
            return true;
        }
        Integer lastDays = BigDecimal.ONE.intValue();
        for (; lastDays <= maxDiscontinueDays; lastDays++) {
            Integer lastDateIndex = currentDateIndex + lastDays;
            if (lastDateIndex > maxDateIndex) {
                return true;
            }
            Integer lastDayValue = skuMonthProductionInfo.getDayQty(lastDateIndex);
            if (null == lastDayValue || lastDayValue <= BigDecimal.ZERO.intValue()) {
                continue;
            }
        }
        return false;
    }

    /**
     * 获取是否需要查找下一天
     * 从当前天看下一天，连续看stepSize天，如果都有值则需要看下一天
     * 否则无需看下一天
     *
     * @param currentDateIndex   当前天下标，正常为0
     * @param maxDiscontinueDays 需要往后看的天数
     * @param allProductionList  所有排产周期天
     * @param dayPlanQtyInfo     排产周期天的计划量
     * @return
     */
    private static boolean hasFindNextDay(Integer currentDateIndex, Integer maxDiscontinueDays, List<Date> allProductionList, Map<Date, Integer> dayPlanQtyInfo) {
        if (null == currentDateIndex || null == maxDiscontinueDays || CollectionUtils.isEmpty(allProductionList) || CollectionUtils.isEmpty(dayPlanQtyInfo)) {
            return false;
        }
        Date currentDate = allProductionList.get(currentDateIndex);
        Integer currentValue = dayPlanQtyInfo.get(currentDate);
        if (null == currentValue || currentValue <= BigDecimal.ZERO.intValue()) {
            return true;
        }
        List<Date> needFindNextDateInfo = Lists.newArrayList();
        int maxIndex = allProductionList.size();
        for (int index = BigDecimal.ONE.intValue(); index < maxDiscontinueDays; index++) {
            int findIndex = currentDateIndex + index;
            if (findIndex < maxIndex) {
                needFindNextDateInfo.add(allProductionList.get(findIndex));
            }
        }
        if (CollectionUtils.isEmpty(needFindNextDateInfo)) {
            return false;
        }
        boolean hasPlan = false;
        for (Date productionDate : needFindNextDateInfo) {
            Integer lastDayValue = dayPlanQtyInfo.get(productionDate);
            if (null == lastDayValue || lastDayValue <= BigDecimal.ZERO.intValue()) {
                continue;
            }
            hasPlan = true;
            break;
        }
        return hasPlan;
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

    /**
     * 获取有效排产日，因找到的最后排产日期
     *
     * @param allFindQtyDateList
     * @param lastDate
     * @return
     */
    private static List<Date> getEffectiveDateByLastDate(List<Date> allFindQtyDateList, Date lastDate) {
        if (CollectionUtils.isEmpty(allFindQtyDateList)) {
            return Collections.emptyList();
        }
        if (null == lastDate) {
            return allFindQtyDateList;
        }
        List<Date> effectiveDateList = Lists.newArrayList();
        allFindQtyDateList.forEach(singleDate -> {
            if (singleDate.after(lastDate)) {
                return;
            }
            effectiveDateList.add(singleDate);
        });
        if (CollectionUtils.isEmpty(effectiveDateList)) {
            return Collections.emptyList();
        }
        return effectiveDateList;
    }

    /**
     * 获取间断排产天数所有排产周期日
     *
     * @param realProductionCycleList 当前周期排产日(3天8个班)
     * @param maxDiscontinueDays
     * @return
     */
    private static List<Date> getAllProductionDateByDiscontinueDays(List<Date> realProductionCycleList, Integer maxDiscontinueDays) {
        if (CollectionUtils.isEmpty(realProductionCycleList)) {
            return Collections.emptyList();
        }
        List<Date> discontinueDateList = Lists.newArrayList();
        realProductionCycleList.forEach(realProductionDate -> discontinueDateList.add(realProductionDate));
        if (!isLimit(maxDiscontinueDays)) {
            return discontinueDateList;
        }
        //按日期从早到晚升序排序
        realProductionCycleList.sort(null);
        int endIndex = realProductionCycleList.size() - BigDecimal.ONE.intValue();
        Date realProductionEndDate = realProductionCycleList.get(endIndex);
        LocalDate addStartDatge = getDate(realProductionEndDate);
        for (int addDays = BigDecimal.ONE.intValue(); addDays <= maxDiscontinueDays; addDays++) {
            LocalDate addDate = addStartDatge.plusDays(addDays);
            Date addNextDate = getDate(addDate);
            discontinueDateList.add(addNextDate);
        }
        return discontinueDateList;
    }

    /**
     * 是否有限制，
     * false：为空或是小于等于零表示不限制
     * true：为限制,值大于零
     *
     * @param maxDiscontinueDays
     * @return
     */
    private static boolean isLimit(Integer maxDiscontinueDays) {
        if (null == maxDiscontinueDays) {
            return false;
        }
        return maxDiscontinueDays > BigDecimal.ZERO.intValue();
    }

}
