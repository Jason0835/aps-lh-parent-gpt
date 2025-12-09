package com.zlt.aps.monthplan.factory.helper;

import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.BeanCopyUtils;
import com.zlt.aps.factory.utils.DateUtils;
import com.zlt.aps.monthplan.api.domain.dto.TrialProductionPlanDto;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.entity.FactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanMouldingDayResult;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanProductionFinalResult;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanProductionFinalResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * 排产计划业务工具类型
 *
 * @author ZLT
 * @date 20250702
 */
@Slf4j
public class ProductionPlanExcelUtils {

    /**
     * 构建周期日顺序excel头列表信息
     * 调整列表的日期展示
     *
     * @param version
     * @return
     */
    public static List<Integer> getCycleDayList(FactoryProductionVersion version) {
        Date startDate = version.getProductionStartDate();
        Date endDate = version.getProductionEndDate();
        List<Integer> dayList = new ArrayList<>();
        Integer startDay = DateUtils.getDaysByMonth(startDate);
        Integer endDay = DateUtils.getMaxDaysByMonth(startDate);
        for (Integer day = startDay; day <= endDay; day++) {
            dayList.add(day);
        }
        Integer cycleEndDate = DateUtils.getDaysByMonth(endDate);
        for (Integer day = BigDecimal.ONE.intValue(); day <= cycleEndDate; day++) {
            dayList.add(day);
        }
        return dayList;
    }

    /**
     * 对排产计划导入，需要根据自然月与非自然月调整day的排产量值
     *
     * @param productionVersion
     * @param list
     */
    public static void handlerProductionDayQty(FactoryProductionVersion productionVersion, List<MonthPlanMouldingDayResult> list) {
        if (null == productionVersion || CollectionUtils.isEmpty(list)) {
            return;
        }
        if (YesOrNoEnum.YES.getValue().equals(productionVersion.getIsNaturalMonth())) {
            return;
        }
        List<Integer> daySortList = getCycleDayList(productionVersion);
        Map<Integer, Integer> dayNumberMap = getDayByCycleNumber(daySortList);
        //非自然月处理
        list.stream().forEach(excelData -> handlerValue(excelData, daySortList, dayNumberMap));
    }

    /**
     * 对排产计划导入，需要根据自然月与非自然月调整day的排产量值
     *
     * @param productionVersion
     * @param list
     */
    public static void handlerFinalProductionDayQty(FactoryProductionVersion productionVersion, List<FactoryMonthPlanProdFinal> list) {
        if (null == productionVersion || CollectionUtils.isEmpty(list)) {
            return;
        }
        if (YesOrNoEnum.YES.getValue().equals(productionVersion.getIsNaturalMonth())) {
            return;
        }
        List<Integer> daySortList = getCycleDayList(productionVersion);
        Map<Integer, Integer> dayNumberMap = getDayByCycleNumber(daySortList);
        //非自然月处理
        list.stream().forEach(excelData -> handlerValue(excelData, daySortList, dayNumberMap));
    }

    /**
     * 是否超出了月份最大天数排产
     *
     * @param item        排产计划
     * @param monthMaxDay 月最大天数
     * @return
     */
    public static boolean isExceedMonthMaxDay(MonthPlanProductionFinalResult item, Integer monthMaxDay) {
        if (FactoryConstant.MONTH_MAX_DAY.equals(monthMaxDay)) {
            return false;
        }
        //超出月最大天数
        String fieldName;
        for (int day = monthMaxDay + 1; day <= monthMaxDay; day++) {
            fieldName = String.format("day%d", day);
            Long productionQty = (Long) item.getFieldValueByFieldName(fieldName);
            if (!(null == productionQty || productionQty == 0)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 统计总排产量及设置起始日、结束日
     *
     * @param item                  数据信息
     * @param isTrialProductionPlan 是否试制量试计划
     */
    public static void resetTotalProductionQty(MonthPlanProductionFinalResult item, boolean isTrialProductionPlan) {
        String fieldName;
        Long totalQty = BigDecimal.ZERO.longValue();
        Integer beginDate = null;
        Integer endDate = null;
        for (int day = FactoryConstant.MONTH_START_DAY; day <= FactoryConstant.MONTH_MAX_DAY; day++) {
            fieldName = String.format("day%d", day);
            Long productionQty = (Long) item.getFieldValueByFieldName(fieldName);
            if (null != productionQty && productionQty > BigDecimal.ZERO.longValue()) {
                if (null == beginDate) {
                    beginDate = day;
                } else {
                    beginDate = Math.min(beginDate, day);
                }
                if (null == endDate) {
                    endDate = day;
                } else {
                    endDate = Math.max(endDate, day);
                }
            }
            if (null == productionQty) {
                productionQty = BigDecimal.ZERO.longValue();
            }
            totalQty = totalQty + productionQty;
        }
        item.setTotalQty(totalQty);
        item.setBeginDate(beginDate);
        item.setEndDay(endDate);
        //试制量试导入处理
        if (isTrialProductionPlan) {
            item.setProdReqPlan(totalQty);
            item.setFactProdReqQty(totalQty);
            item.setDifferenceQty(BigDecimal.ZERO.longValue());
            return;
        }
        Long requireQty = item.getProdReqPlan();
        if (null == requireQty) {
            requireQty = BigDecimal.ZERO.longValue();
        }
        if (null == item.getFactProdReqQty()) {
            item.setFactProdReqQty(requireQty);
        }
        item.setDifferenceQty(item.getFactProdReqQty() - totalQty);
    }

    /**
     * 开始日期、结束日期要么都有值，要么都没值
     *
     * @param beginDay
     * @param endDay
     * @return
     */
    public static boolean haseDoubleDayValue(Integer beginDay, Integer endDay) {
        if (null == beginDay && null != endDay) {
            return false;
        }
        if (null != beginDay && null == endDay) {
            return false;
        }
        return true;
    }

    /**
     * 对排产计划导入，需要根据自然月与非自然月调整day的排产量值
     *
     * @param productionVersion
     * @param list
     */
    public static void handlerTrialProductionPlanDayQty(FactoryProductionVersion productionVersion, List<TrialProductionPlanDto> list) {
        if (null == productionVersion || CollectionUtils.isEmpty(list)) {
            return;
        }
        if (YesOrNoEnum.YES.getValue().equals(productionVersion.getIsNaturalMonth())) {
            return;
        }
        List<Integer> daySortList = getCycleDayList(productionVersion);
        Map<Integer, Integer> dayNumberMap = getDayByCycleNumber(daySortList);
        //非自然月处理
        list.stream().forEach(excelData -> handlerValue(excelData, daySortList, dayNumberMap));
    }

    /**
     * 对排产计划导入，需要根据自然月与非自然月调整day的排产量值
     *
     * @param productionVersion
     * @param list
     */
    public static void handlerAdjustPlanDayQty(FactoryProductionVersion productionVersion, List<MonthPlanProductionFinalResultVo> list) {
        if (null == productionVersion || CollectionUtils.isEmpty(list)) {
            return;
        }
        if (YesOrNoEnum.YES.getValue().equals(productionVersion.getIsNaturalMonth())) {
            return;
        }
        List<Integer> daySortList = getCycleDayList(productionVersion);
        Map<Integer, Integer> dayNumberMap = getDayByCycleNumber(daySortList);
        //非自然月处理
        list.stream().forEach(excelData -> handlerValue(excelData, daySortList, dayNumberMap));
    }

    /**
     * 根据版本信息，处理开始日期和结束日期
     * 自然月则不用处理，非自然月需要处理
     *
     * @param version
     * @param resultData
     */
    public static void handlerBeginAndEndDay(FactoryProductionVersion version, List<FactoryMonthPlanProdFinal> resultData) {
        if (null == version || CollectionUtils.isEmpty(resultData)) {
            return;
        }
        if (YesOrNoEnum.YES.getValue().equals(version.getIsNaturalMonth())) {
            return;
        }
        List<Integer> daySortList = getCycleDayList(version);
        Integer monthMaxDays = daySortList.size();
        resultData.stream().forEach(queryData -> {
            Integer startDay = queryData.getBeginDate();
            if (null != startDay && startDay <= monthMaxDays) {
                queryData.setBeginDate(daySortList.get(startDay - BigDecimal.ONE.intValue()));
            }
            Integer endDay = queryData.getEndDay();
            if (null != endDay && endDay <= monthMaxDays) {
                queryData.setEndDay(daySortList.get(endDay - BigDecimal.ONE.intValue()));
            }
        });
    }

    /**
     * 根据版本信息，处理开始日期和结束日期
     * 自然月则不用处理，非自然月需要处理
     *
     * @param version
     * @param resultData
     */
    public static void handlerBeginAndEndDayBySku(FactoryProductionVersion version, List<MonthPlanProductionFinalResult> resultData) {
        if (null == version || CollectionUtils.isEmpty(resultData)) {
            return;
        }
        if (YesOrNoEnum.YES.getValue().equals(version.getIsNaturalMonth())) {
            return;
        }
        List<Integer> daySortList = getCycleDayList(version);
        Integer monthMaxDays = daySortList.size();
        resultData.stream().forEach(queryData -> {
            Integer startDay = queryData.getBeginDate();
            if (null != startDay && startDay <= monthMaxDays) {
                queryData.setBeginDate(daySortList.get(startDay - BigDecimal.ONE.intValue()));
            }
            Integer endDay = queryData.getEndDay();
            if (null != endDay && endDay <= monthMaxDays) {
                queryData.setEndDay(daySortList.get(endDay - BigDecimal.ONE.intValue()));
            }
        });
    }

    /**
     * 对原有的excel数据，因非自然月导致日期排产量需要重新调整其值
     * excel中为日期，需要转化成周期所处天数值
     * 且其开始日期-结束日期重新调整为1~31的值
     *
     * @param excelData   excel原有数据
     * @param daySortList 日期顺序列表
     */
    private static void handlerValue(MonthPlanMouldingDayResult excelData, List<Integer> daySortList, Map<Integer, Integer> dayNumberMap) {
        String fieldNameFormat = "day%s";
        //值拷贝
        MonthPlanMouldingDayResult copyData = BeanCopyUtils.copyBean(excelData, MonthPlanMouldingDayResult.class);
        for (Integer day : daySortList) {
            Integer realDayNumber = dayNumberMap.get(day);
            String excelFieldName = String.format(fieldNameFormat, day);
            //真实所处天数
            String realFieldName = String.format(fieldNameFormat, realDayNumber);
            excelData.setFieldValueByFieldName(realFieldName, copyData.getFieldValueByFieldName(excelFieldName));
        }
        Integer excelStartDay = copyData.getBeginDay();
        if (null != excelStartDay) {
            excelData.setBeginDay(dayNumberMap.get(excelStartDay));
        }
        Integer excelEndDay = copyData.getEndDay();
        if (null != excelEndDay) {
            excelData.setEndDay(dayNumberMap.get(excelEndDay));
        }
    }

    /**
     * 对原有的excel数据，因非自然月导致日期排产量需要重新调整其值
     * excel中为日期，需要转化成周期所处天数值
     * 且其开始日期-结束日期重新调整为1~31的值
     *
     * @param excelData   excel原有数据
     * @param daySortList 日期顺序列表
     */
    private static void handlerValue(TrialProductionPlanDto excelData, List<Integer> daySortList, Map<Integer, Integer> dayNumberMap) {
        String fieldNameFormat = "day%s";
        //值拷贝
        TrialProductionPlanDto copyData = BeanCopyUtils.copyBean(excelData, TrialProductionPlanDto.class);
        for (Integer day : daySortList) {
            Integer realDayNumber = dayNumberMap.get(day);
            String excelFieldName = String.format(fieldNameFormat, day);
            //真实所处天数
            String realFieldName = String.format(fieldNameFormat, realDayNumber);
            excelData.setFieldValueByFieldName(realFieldName, copyData.getFieldValueByFieldName(excelFieldName));
        }
    }

    /**
     * 对原有的excel数据，因非自然月导致日期排产量需要重新调整其值
     * excel中为日期，需要转化成周期所处天数值
     * 且其开始日期-结束日期重新调整为1~31的值
     *
     * @param excelData   excel原有数据
     * @param daySortList 日期顺序列表
     */
    private static void handlerValue(MonthPlanProductionFinalResultVo excelData, List<Integer> daySortList, Map<Integer, Integer> dayNumberMap) {
        String fieldNameFormat = "day%s";
        //值拷贝
        MonthPlanProductionFinalResultVo copyData = BeanCopyUtils.copyBean(excelData, MonthPlanProductionFinalResultVo.class);
        for (Integer day : daySortList) {
            Integer realDayNumber = dayNumberMap.get(day);
            String excelFieldName = String.format(fieldNameFormat, day);
            //真实所处天数
            String realFieldName = String.format(fieldNameFormat, realDayNumber);
            excelData.setFieldValueByFieldName(realFieldName, copyData.getFieldValueByFieldName(excelFieldName));
        }
        Integer excelStartDay = copyData.getBeginDate();
        if (null != excelStartDay) {
            excelData.setBeginDate(dayNumberMap.get(excelStartDay));
        }
        Integer excelEndDay = copyData.getEndDay();
        if (null != excelEndDay) {
            excelData.setEndDay(dayNumberMap.get(excelEndDay));
        }
    }

    /**
     * 对原有的excel数据，因非自然月导致日期排产量需要重新调整其值
     * excel中为日期，需要转化成周期所处天数值
     * 且其开始日期-结束日期重新调整为1~31的值
     *
     * @param excelData   excel原有数据
     * @param daySortList 日期顺序列表
     */
    private static void handlerValue(FactoryMonthPlanProdFinal excelData, List<Integer> daySortList, Map<Integer, Integer> dayNumberMap) {
        String fieldNameFormat = "day%s";
        //值拷贝
        FactoryMonthPlanProdFinal copyData = BeanCopyUtils.copyBean(excelData, FactoryMonthPlanProdFinal.class);
        for (Integer day : daySortList) {
            Integer realDayNumber = dayNumberMap.get(day);
            String excelFieldName = String.format(fieldNameFormat, day);
            //真实所处天数
            String realFieldName = String.format(fieldNameFormat, realDayNumber);
            excelData.setFieldValueByFieldName(realFieldName, copyData.getFieldValueByFieldName(excelFieldName));
        }
        Integer excelStartDay = copyData.getBeginDate();
        if (null != excelStartDay) {
            excelData.setBeginDate(dayNumberMap.get(excelStartDay));
        }
        Integer excelEndDay = copyData.getEndDay();
        if (null != excelEndDay) {
            excelData.setEndDay(dayNumberMap.get(excelEndDay));
        }
    }

    /**
     * 根据周期，获取日期在周期所处范围
     *
     * @param daySortList 日周期顺序
     * @return
     */
    private static Map<Integer, Integer> getDayByCycleNumber(List<Integer> daySortList) {
        Map<Integer, Integer> dayNumberMap = new HashMap<>(48);
        Integer dayNumber = BigDecimal.ONE.intValue();
        for (Integer day : daySortList) {
            dayNumberMap.put(day, dayNumber);
            dayNumber = dayNumber + BigDecimal.ONE.intValue();
        }
        return dayNumberMap;
    }

    private ProductionPlanExcelUtils() {

    }
}
