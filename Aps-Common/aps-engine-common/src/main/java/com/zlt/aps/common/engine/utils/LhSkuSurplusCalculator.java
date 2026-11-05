package com.zlt.aps.common.engine.utils;

import cn.hutool.core.date.DateUtil;
import com.zlt.aps.common.engine.domain.*;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 硫化余量计算：
 * <p>
 * 以Sku的月计划（按月计划拆分到每日计划量）为基础，结合排产周期内的排产日、计划计算起始日、
 * 硫化日计划调整量以及完成量，计算Sku的硫化余量（前段余量/后段余量）。
 * </p>
 *
 * @author ZLT
 * @date 20261103
 */
@Slf4j
public class LhSkuSurplusCalculator {

    /**
     * 硫化余量计算：
     * <p>
     * 1、忽略calculateSkuInfo中的monthOverdueQtyMap超欠产，不参与计算；从allMonthPlanList中取出
     * 与skuInfo同Sku（工厂|*|物料编码|*|施工类型）的月计划信息；
     * 2、把取到的月计划按"每日日期 + 计划量"（计划量空也保留）组装成SkuLhMonthPlanInfoHelper对象；
     * 3、从排产周期的排产日realProductionCycleList中取最早的排产日firstScheduleDate；
     * 4、从planStartDate取得计划计算的起始日；
     * 5、从allLhDayAdjustList中取出同Sku的硫化日计划调整信息并汇总调整量；
     * 6、取排产周期排产日对应的计划量：
     * 6.1、排产周期内都没有计划量：按planStartDate~firstScheduleDate汇总计划量，
     * 汇总计划量 + 日计划调整量 - 完成量 > 0 时返回并标记overdueSurplusFlag=true（前段余量）；
     * 6.1.2、否则从最后一个排产日往后找第一个有计划量的日期作为往后查找计划的起始日；
     * 6.1.3、从起始日往后查找终止日（不限制间隔：第一个没有计划量的日期；
     * 限制间隔：第一个连续skuMaxDiscontinueDays+1天没有计划量的日期）；
     * 6.1.4、按planStartDate~终止日汇总计划量，返回并标记overdueSurplusFlag=false（后段余量）；
     * 6.2、排产周期内计划量存在：取排产周期内有计划量的最大日，往后看skuMaxDiscontinueDays+1天，
     * 有则取其中日期最大者、无则取该最大日作为起始日，再按6.1.3口径找终止日，
     * 按planStartDate~终止日汇总计划量，返回并标记overdueSurplusFlag=false（后段余量）。
     * </p>
     *
     * @param productionDateInfo 排产日信息
     * @param calculateSkuInfo   排产Sku信息
     * @return 硫化余量结果；参数非法返回null
     */
    public static LhSurplusResultVo getSurplusInfo(LhSkuSurplusControlParam productionDateInfo, LhSurplusSkuInfo calculateSkuInfo) {
        //参数校验
        if (null == productionDateInfo || null == calculateSkuInfo) {
            return null;
        }
        FactoryMonthPlanProductionFinalResult skuInfo = calculateSkuInfo.getSkuInfo();
        List<Date> realProductionCycleList = productionDateInfo.getRealProductionCycleList();
        if (null == skuInfo || CollectionUtils.isEmpty(realProductionCycleList)) {
            return null;
        }
        //排产周期的排产日，按日期从早到晚升序
        List<LocalDate> cycleDateList = realProductionCycleList.stream()
                .filter(Objects::nonNull)
                .map(LhSkuSurplusCalculator::toLocalDate)
                .sorted()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(cycleDateList)) {
            return null;
        }
        //步骤1、2：忽略超欠产；取同Sku月计划并拆分成"日期-计划量"
        SkuLhMonthPlanInfoHelper monthPlanInfoHelper = buildSkuMonthPlanInfo(skuInfo, calculateSkuInfo.getAllMonthPlanList());
        Map<LocalDate, Integer> dayPlanQtyMap = getDayPlanQtyMap(monthPlanInfoHelper);
        //步骤3：排产周期内最早的排产日
        LocalDate firstScheduleDate = getFirstScheduleDate(realProductionCycleList);
        //步骤4：计划计算起始日
        LocalDate planStartDate = getPlanStartDate(productionDateInfo);
        //步骤5：日硫化计划调整量汇总
        Integer lhDayAdjustQty = sumLhDayAdjustQty(skuInfo, calculateSkuInfo.getAllLhDayAdjustList());
        //完成量，空值按0处理
        Integer finishedQty = Optional.ofNullable(calculateSkuInfo.getFinishedQty()).orElse(BigDecimal.ZERO.intValue());
        //计划计算起始日之后的全部月计划量合计（作为总计划量）
        Integer allPlanQty = sumPlanQty(dayPlanQtyMap, planStartDate, getLastPlanDate(dayPlanQtyMap)) + lhDayAdjustQty;
        //Sku连续排产最大间隔天数
        Integer skuMaxDiscontinueDays = productionDateInfo.getSkuMaxDiscontinueDays();
        //步骤6：排产周期的排产日是否有计划量
        boolean cycleHasPlanQty = cycleDateList.stream()
                .anyMatch(cycleDate -> hasPlanQty(dayPlanQtyMap.get(cycleDate)));
        if (!cycleHasPlanQty) {
            //6.1 排产周期内都没有计划量：先按前段余量（planStartDate~firstScheduleDate）计算
            Integer beforePlanQty = sumPlanQty(dayPlanQtyMap, planStartDate, firstScheduleDate) + lhDayAdjustQty;
            if (beforePlanQty - finishedQty > BigDecimal.ZERO.intValue()) {
                //6.1 前段有余量：返回结果并标记前段余量
                LhSurplusResultVo beforeSurplusInfo = buildSurplusResult(skuInfo, beforePlanQty, allPlanQty, finishedQty);
                beforeSurplusInfo.markBeforeSurplusQty();
                return beforeSurplusInfo;
            }
            //6.1.2 前段没有余量：从最后一个排产日往后找第一个有计划量的日期作为查找起始日
            LocalDate lastCycleDate = cycleDateList.get(cycleDateList.size() - BigDecimal.ONE.intValue());
            LocalDate forwardStartDate = findFirstHasPlanQtyDate(lastCycleDate, dayPlanQtyMap);
            //6.1.3 从起始日往后查找终止日
            LocalDate forwardEndDate = findPlanEndDate(forwardStartDate, skuMaxDiscontinueDays, dayPlanQtyMap);
            if(null == forwardEndDate){
                //表示后面都没有计划量
                forwardEndDate = lastCycleDate;
            }
            //6.1.4 后段余量：按planStartDate~终止日汇总计划量
            Integer forwardPlanQty = sumPlanQty(dayPlanQtyMap, planStartDate, forwardEndDate) + lhDayAdjustQty;
            return buildSurplusResult(skuInfo, forwardPlanQty, allPlanQty, finishedQty);
        }
        //6.2 排产周期内有计划量：取排产周期内有计划量的最大日
        LocalDate maxCyclePlanDate = getMaxHasPlanQtyDate(cycleDateList, dayPlanQtyMap);
        //6.2 往后看skuMaxDiscontinueDays+1天的计划量，确定查找起始日
        LocalDate forwardStartDate = findPlanQtyStartDate(maxCyclePlanDate, skuMaxDiscontinueDays, dayPlanQtyMap);
        //6.2.1 从起始日往后查找终止日
        LocalDate forwardEndDate = findPlanEndDate(forwardStartDate, skuMaxDiscontinueDays, dayPlanQtyMap);
        //6.2.2 后段余量：按planStartDate~终止日汇总计划量
        Integer forwardPlanQty = sumPlanQty(dayPlanQtyMap, planStartDate, forwardEndDate) + lhDayAdjustQty;
        return buildSurplusResult(skuInfo, forwardPlanQty, allPlanQty, finishedQty);
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
     * 获取排产日期所在排产年份-月份
     *
     * @param productionDate 排产日期
     * @return 年-月
     */
    public static YearMonth getProductionYearAndMonth(Date productionDate) {
        //年份
        int year = cn.hutool.core.date.DateUtil.year(productionDate);
        //月份 0~11，故而要+1
        int month = DateUtil.month(productionDate) + BigDecimal.ONE.intValue();
        return YearMonth.of(year, month);
    }

    /**
     * 组装Sku的月计划信息：
     * 从所有月计划中取出与skuInfo同Sku（工厂|*|物料编码|*|施工类型）的月计划，
     * 再按每个月的每一天拆分成"日期-计划量"，没有计划量的日期也保留。
     *
     * @param skuInfo          需要计算的Sku信息
     * @param allMonthPlanList 所有月计划信息
     * @return Sku月计划信息
     */
    private static SkuLhMonthPlanInfoHelper buildSkuMonthPlanInfo(FactoryMonthPlanProductionFinalResult skuInfo,
                                                                  List<FactoryMonthPlanProductionFinalResult> allMonthPlanList) {
        SkuLhMonthPlanInfoHelper monthPlanInfoHelper = new SkuLhMonthPlanInfoHelper();
        monthPlanInfoHelper.setSkuInfo(skuInfo);
        List<SkuLhDayMonthPlanQtyVo> allProductionDayList = new ArrayList<>();
        monthPlanInfoHelper.setAllProductionDayList(allProductionDayList);
        if (null == skuInfo || CollectionUtils.isEmpty(allMonthPlanList)) {
            return monthPlanInfoHelper;
        }
        //Sku匹配Key：工厂|*|物料编码|*|施工类型
        String skuKey = skuInfo.getMaterialStatusKey();
        List<FactoryMonthPlanProductionFinalResult> matchedPlanList = allMonthPlanList.stream()
                .filter(Objects::nonNull)
                .filter(monthPlan -> null != monthPlan.getYear() && null != monthPlan.getMonth())
                .filter(monthPlan -> StringUtils.equals(skuKey, monthPlan.getMaterialStatusKey()))
                //按月从早到晚排序，保证拆分后的日期有序
                .sorted(Comparator.comparing((FactoryMonthPlanProductionFinalResult monthPlan) -> YearMonth.of(monthPlan.getYear(), monthPlan.getMonth())))
                .collect(Collectors.toList());
        for (FactoryMonthPlanProductionFinalResult monthPlan : matchedPlanList) {
            YearMonth monthPlanYearMonth = YearMonth.of(monthPlan.getYear(), monthPlan.getMonth());
            int monthEndDay = monthPlanYearMonth.lengthOfMonth();
            for (int day = BigDecimal.ONE.intValue(); day <= monthEndDay; day++) {
                SkuLhDayMonthPlanQtyVo dayPlanQtyVo = new SkuLhDayMonthPlanQtyVo();
                dayPlanQtyVo.setMaterialCode(monthPlan.getMaterialCode());
                dayPlanQtyVo.setMaterialDesc(monthPlan.getMaterialDesc());
                dayPlanQtyVo.setConstructionStage(monthPlan.getConstructionStage());
                dayPlanQtyVo.setPlanDate(getDate(monthPlanYearMonth.atDay(day)));
                //没有排产量的日期也保留，保证日期连续
                dayPlanQtyVo.setPlanQty(monthPlan.getDayQty(day));
                allProductionDayList.add(dayPlanQtyVo);
            }
        }
        return monthPlanInfoHelper;
    }

    /**
     * 把Sku月计划信息转换成"日期-计划量"的映射，日期从早到晚升序
     *
     * @param monthPlanInfoHelper Sku月计划信息
     * @return 日期-计划量映射，空返回空Map
     */
    private static Map<LocalDate, Integer> getDayPlanQtyMap(SkuLhMonthPlanInfoHelper monthPlanInfoHelper) {
        Map<LocalDate, Integer> dayPlanQtyMap = new TreeMap<>();
        if (null == monthPlanInfoHelper || CollectionUtils.isEmpty(monthPlanInfoHelper.getAllProductionDayList())) {
            return dayPlanQtyMap;
        }
        for (SkuLhDayMonthPlanQtyVo dayPlanQtyVo : monthPlanInfoHelper.getAllProductionDayList()) {
            if (null == dayPlanQtyVo || null == dayPlanQtyVo.getPlanDate()) {
                continue;
            }
            dayPlanQtyMap.put(toLocalDate(dayPlanQtyVo.getPlanDate()), dayPlanQtyVo.getPlanQty());
        }
        return dayPlanQtyMap;
    }

    /**
     * 汇总对应Sku的日硫化计划调整量
     *
     * @param skuInfo            需要计算的Sku信息
     * @param allLhDayAdjustList 所有硫化日计划调整信息
     * @return 调整量合计，无调整返回0
     */
    private static Integer sumLhDayAdjustQty(FactoryMonthPlanProductionFinalResult skuInfo, List<LhDayPlanAdjustVo> allLhDayAdjustList) {
        if (null == skuInfo || CollectionUtils.isEmpty(allLhDayAdjustList)) {
            return BigDecimal.ZERO.intValue();
        }
        //Sku匹配Key：物料编码|*|施工类型
        String skuKey = skuInfo.getMaterialStatusKey();
        return allLhDayAdjustList.stream()
                .filter(Objects::nonNull)
                .filter(adjustVo -> StringUtils.equals(skuKey, adjustVo.getMaterialStatusKey()))
                .mapToInt(LhDayPlanAdjustVo::getPlanQtyValue)
                .sum();
    }

    /**
     * 从排产周期的排产日中取最早（计划计算起始）的日期
     *
     * @param realProductionCycleList 排产周期的排产日集合
     * @return 最早的排产日
     */
    private static LocalDate getFirstScheduleDate(List<Date> realProductionCycleList) {
        if (CollectionUtils.isEmpty(realProductionCycleList)) {
            return null;
        }
        return realProductionCycleList.stream()
                .filter(Objects::nonNull)
                .min(Date::compareTo)
                .map(LhSkuSurplusCalculator::toLocalDate)
                .orElse(null);
    }

    /**
     * 获取计划计算的起始日：
     * 优先取planStartDate（最新库存抓取日），为空时按排产月份的计划计算起始日（正常为1日）兜底。
     *
     * @param productionDateInfo 排产日信息
     * @return 计划计算起始日
     */
    private static LocalDate getPlanStartDate(LhSkuSurplusControlParam productionDateInfo) {
        if (null == productionDateInfo) {
            return null;
        }
        if (null != productionDateInfo.getPlanStartDate()) {
            return toLocalDate(productionDateInfo.getPlanStartDate());
        }
        return null;
    }

    /**
     * 从fromDate（含）开始往后，找到第一个有排产量的日期
     *
     * @param fromDate      查找的起始日期
     * @param dayPlanQtyMap 日期-计划量映射
     * @return 第一个有计划量的日期，未找到返回null
     */
    private static LocalDate findFirstHasPlanQtyDate(LocalDate fromDate, Map<LocalDate, Integer> dayPlanQtyMap) {
        if (null == fromDate || CollectionUtils.isEmpty(dayPlanQtyMap)) {
            return null;
        }
        //dayPlanQtyMap为TreeMap，已按日期升序
        return dayPlanQtyMap.entrySet().stream()
                .filter(entry -> !entry.getKey().isBefore(fromDate))
                .filter(entry -> hasPlanQty(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    /**
     * 取排产周期的排产日中，有计划量的最大日期
     *
     * @param cycleDateList 排产周期的排产日集合（按日期升序）
     * @param dayPlanQtyMap 日期-计划量映射
     * @return 有计划量的最大日期，未找到返回null
     */
    private static LocalDate getMaxHasPlanQtyDate(List<LocalDate> cycleDateList, Map<LocalDate, Integer> dayPlanQtyMap) {
        if (CollectionUtils.isEmpty(cycleDateList) || CollectionUtils.isEmpty(dayPlanQtyMap)) {
            return null;
        }
        return cycleDateList.stream()
                .filter(cycleDate -> hasPlanQty(dayPlanQtyMap.get(cycleDate)))
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    /**
     * 确定往后查找计划的起始日：
     * 从maxPlanQtyDate往后看skuMaxDiscontinueDays+1天的计划量，其中有计划量的取日期最大者作为起始日，
     * 都没有计划量时以maxPlanQtyDate作为起始日。
     *
     * @param maxPlanQtyDate        排产周期内有计划量的最大日期
     * @param skuMaxDiscontinueDays Sku连续排产最大间隔天数
     * @param dayPlanQtyMap         日期-计划量映射
     * @return 往后查找计划的起始日
     */
    private static LocalDate findPlanQtyStartDate(LocalDate maxPlanQtyDate, Integer skuMaxDiscontinueDays, Map<LocalDate, Integer> dayPlanQtyMap) {
        if (null == maxPlanQtyDate || CollectionUtils.isEmpty(dayPlanQtyMap)) {
            return maxPlanQtyDate;
        }
        //往后查看的天数：限制间隔时为 skuMaxDiscontinueDays + 1，不限制间隔时只看当天后的1天
        int lookAheadDays = isLimit(skuMaxDiscontinueDays) ? skuMaxDiscontinueDays + BigDecimal.ONE.intValue() : BigDecimal.ONE.intValue();
        LocalDate lastHasPlanQtyDate = null;
        for (int addDays = BigDecimal.ONE.intValue(); addDays <= lookAheadDays; addDays++) {
            LocalDate nextDate = maxPlanQtyDate.plusDays(addDays);
            if (!hasPlanQty(dayPlanQtyMap.get(nextDate))) {
                continue;
            }
            //取得日期最大的计划量日期
            lastHasPlanQtyDate = nextDate;
        }
        if (null == lastHasPlanQtyDate) {
            return maxPlanQtyDate;
        }
        return lastHasPlanQtyDate;
    }

    /**
     * 从startDate开始往后查找计划汇总的终止日
     * <p>
     * 不限制间隔（skuMaxDiscontinueDays为空或小于等于0）：第一个没有计划量的日期为终止日；
     * 限制间隔（skuMaxDiscontinueDays大于0）：第一个连续 skuMaxDiscontinueDays+1 天没有计划量的日期为终止日。
     * </p>
     *
     * @param startDate             查找起始日
     * @param skuMaxDiscontinueDays Sku连续排产最大间隔天数
     * @param dayPlanQtyMap         日期-计划量映射
     * @return 终止日；起始日为空返回null
     */
    private static LocalDate findPlanEndDate(LocalDate startDate, Integer skuMaxDiscontinueDays, Map<LocalDate, Integer> dayPlanQtyMap) {
        if (null == startDate) {
            return null;
        }
        if (CollectionUtils.isEmpty(dayPlanQtyMap)) {
            return startDate;
        }
        //起始日（含）之后的日期，按日期升序
        List<LocalDate> planDateList = dayPlanQtyMap.keySet().stream()
                .filter(planDate -> !planDate.isBefore(startDate))
                .sorted()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(planDateList)) {
            return startDate;
        }
        LocalDate lastHasPlanQtyDate = null;
        //往后检查的天数：限制间隔时为 skuMaxDiscontinueDays + 1
        int discontinueDays = BigDecimal.ONE.intValue();
        if (isLimit(skuMaxDiscontinueDays)) {
            discontinueDays = skuMaxDiscontinueDays + BigDecimal.ONE.intValue();
        }
        for (LocalDate planDate : planDateList) {
            if (!hasPlanQty(dayPlanQtyMap.get(planDate))) {
                if (!isLimit(skuMaxDiscontinueDays)) {
                    //不限制间隔：第一个没有计划量的日期即终止日
                    return planDate;
                }
                continue;
            }
            //判断往后连续 discontinueDays 天内是否都没有计划量
            boolean allDiscontinue = true;
            for (int addDays = BigDecimal.ONE.intValue(); addDays <= discontinueDays; addDays++) {
                if (hasPlanQty(dayPlanQtyMap.get(planDate.plusDays(addDays)))) {
                    allDiscontinue = false;
                    break;
                }
            }
            if (allDiscontinue) {
                //第一个出现连续无计划量的日期即终止日
                return planDate;
            }
            lastHasPlanQtyDate = planDate;
        }
        //一直到计划末尾都未出现连续无计划量，以最后一个有计划量的日期为终止日
        if (null == lastHasPlanQtyDate) {
            return startDate;
        }
        return lastHasPlanQtyDate;
    }

    /**
     * 汇总startDate~endDate（含首尾）的计划量
     *
     * @param dayPlanQtyMap 日期-计划量映射
     * @param startDate     汇总起始日
     * @param endDate       汇总终止日，为空时按startDate处理
     * @return 计划量合计
     */
    private static Integer sumPlanQty(Map<LocalDate, Integer> dayPlanQtyMap, LocalDate startDate, LocalDate endDate) {
        if (null == startDate || CollectionUtils.isEmpty(dayPlanQtyMap)) {
            return BigDecimal.ZERO.intValue();
        }
        LocalDate sumEndDate = (null == endDate) ? startDate : endDate;
        if (sumEndDate.isBefore(startDate)) {
            return BigDecimal.ZERO.intValue();
        }
        return dayPlanQtyMap.entrySet().stream()
                .filter(entry -> !entry.getKey().isBefore(startDate) && !entry.getKey().isAfter(sumEndDate))
                .mapToInt(entry -> Optional.ofNullable(entry.getValue()).orElse(BigDecimal.ZERO.intValue()))
                .sum();
    }

    /**
     * 取日期-计划量映射中的最后一个日期
     *
     * @param dayPlanQtyMap 日期-计划量映射
     * @return 最后一个日期，空返回null
     */
    private static LocalDate getLastPlanDate(Map<LocalDate, Integer> dayPlanQtyMap) {
        if (CollectionUtils.isEmpty(dayPlanQtyMap)) {
            return null;
        }
        return Collections.max(dayPlanQtyMap.keySet());
    }

    /**
     * 组装硫化余量结果
     *
     * @param skuInfo     Sku信息
     * @param planQty     当前计划量（已含日硫化计划调整量）
     * @param sumPlanQty  总计划量（已含日硫化计划调整量）
     * @param finishedQty 已完成量
     * @return 硫化余量结果
     */
    private static LhSurplusResultVo buildSurplusResult(FactoryMonthPlanProductionFinalResult skuInfo,
                                                        Integer planQty,
                                                        Integer sumPlanQty,
                                                        Integer finishedQty) {
        //超欠产不生效，固定按0处理
        Integer monthOverdueQty = BigDecimal.ZERO.intValue();
        return new LhSurplusResultVo(skuInfo, monthOverdueQty, planQty, sumPlanQty, finishedQty);
    }

    /**
     * 根据年、月、日，构造Date
     *
     * @param localDate 本地日期
     * @return Date对象
     */
    private static Date getDate(LocalDate localDate) {
        ZoneId zoneId = ZoneId.systemDefault();
        Instant instantTime = localDate.atStartOfDay(zoneId).toInstant();
        return new Date(instantTime.toEpochMilli());
    }

    /**
     * 将Date转换成LocalDate
     * <p>
     * 统一按时间戳转换，兼容java.sql.Date（其toInstant方法会抛异常）。
     * </p>
     *
     * @param date 日期
     * @return 本地日期，空返回null
     */
    private static LocalDate toLocalDate(Date date) {
        if (null == date) {
            return null;
        }
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * 是否有计划量
     *
     * @param planQty 计划量
     * @return true表示有计划量
     */
    private static boolean hasPlanQty(Integer planQty) {
        return null != planQty && planQty > BigDecimal.ZERO.intValue();
    }

    /**
     * 是否限制间隔天数
     * false：为空或小于等于0表示不限制；true：大于0表示限制
     *
     * @param skuMaxDiscontinueDays Sku连续排产最大间隔天数
     * @return true表示限制
     */
    private static boolean isLimit(Integer skuMaxDiscontinueDays) {
        if (null == skuMaxDiscontinueDays) {
            return false;
        }
        return skuMaxDiscontinueDays > BigDecimal.ZERO.intValue();
    }

}