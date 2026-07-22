package com.zlt.aps.mp.engine.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.mp.engine.domain.vo.ProductionDayInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * 排产周期工具类型
 * 纯计算类
 *
 * @author ZLT
 * @date 20251215
 */
@Slf4j
public class ProductionCycleUtils {

    /**
     * 根据排产版本信息及排产日历信息，得到最后排产天
     *
     * @param productionVersion 排产版本信息
     * @param productionDayInfo 对应的月份排产日历
     * @return
     */
    public static Integer getLastProductionDay(MpFactoryProductionVersion productionVersion, List<ProductionDayInfoVo> productionDayInfo) {
        Date productionStartDate = productionVersion.getProductionStartDate();
        //理论结束天数 = 月份周期最大天数
        Integer maxDay = DateUtils.getIntervalDays(productionStartDate, productionVersion.getProductionEndDate());
        if (CollectionUtils.isEmpty(productionDayInfo)) {
            return maxDay;
        }
        //提取可排产的天信息
        List<Integer> productionDayList = getEffectiveDay(productionDayInfo, productionStartDate);
        if (CollectionUtils.isEmpty(productionDayList)) {
            return BigDecimal.ZERO.intValue();
        }
        //升序排序，取最后一天
        productionDayList.sort(Comparator.comparing(Integer::intValue));
        return productionDayList.get(productionDayList.size() - BigDecimal.ONE.intValue());
    }

    /**
     * 根据排产版本信息及排产日历信息，得到最后排产天
     *
     * @param productionVersion 排产版本信息
     * @param productionDayInfo 对应的月份排产日历
     * @return
     */
    public static List<Integer> getLastProductionTowDay(MpFactoryProductionVersion productionVersion, List<ProductionDayInfoVo> productionDayInfo) {
        Date productionStartDate = productionVersion.getProductionStartDate();
        //理论结束天数 = 月份周期最大天数
        Integer maxDay = DateUtils.getIntervalDays(productionStartDate, productionVersion.getProductionEndDate());
        List<Integer> twoDays = Lists.newArrayList();
        if (CollectionUtils.isEmpty(productionDayInfo)) {
            twoDays.add(maxDay);
            twoDays.add(maxDay - BigDecimal.ONE.intValue());
            return twoDays;
        }
        //提取可排产的天信息
        List<Integer> productionDayList = getEffectiveDay(productionDayInfo, productionStartDate);
        if (CollectionUtils.isEmpty(productionDayList)) {
            return Collections.emptyList();
        }
        int days = productionDayList.size();
        if (days == BigDecimal.ONE.intValue()) {
            return productionDayList;
        }
        //降序排序，取最后两天
        productionDayList.sort(Comparator.comparing(Integer::intValue, Comparator.reverseOrder()));
        int length = BigDecimal.ONE.intValue() + BigDecimal.ONE.intValue();
        return productionDayList.subList(BigDecimal.ZERO.intValue(), length);
    }

    /**
     * 根据周期，获取日期在周期所处范围
     *
     * @param startDate 周期起始日
     * @param endDate   周期结束日
     * @return
     */
    public static Map<Integer, Integer> getDayByCycleNumber(Date startDate, Date endDate) {
        if (null == startDate || null == endDate) {
            return Collections.emptyMap();
        }
        if (startDate.after(endDate)) {
            return Collections.emptyMap();
        }
        List<Integer> daySortList = getCycleDayList(startDate, endDate);
        Map<Integer, Integer> dayNumberMap = new HashMap<>(48);
        Integer dayNumber = BigDecimal.ONE.intValue();
        for (Integer day : daySortList) {
            dayNumberMap.put(day, dayNumber);
            dayNumber = dayNumber + BigDecimal.ONE.intValue();
        }
        return dayNumberMap;
    }

    /**
     * 构建排产周期的日顺序信息
     *
     * @param startDate 周期起始日
     * @param endDate   周期结束日
     * @return
     */
    private static List<Integer> getCycleDayList(Date startDate, Date endDate) {
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
     * 获取有效排产日信息
     * 剔除停工日的影响
     *
     * @param productionDayInfo   月完整工作日历
     * @param productionStartDate 开始排产日
     * @return
     */
    private static List<Integer> getEffectiveDay(List<ProductionDayInfoVo> productionDayInfo, Date productionStartDate) {
        if (CollectionUtils.isEmpty(productionDayInfo) || null == productionStartDate) {
            return Collections.emptyList();
        }
        Set<Integer> productionDaySet = Sets.newHashSet();
        productionDayInfo.forEach(singleProductionDayInfo -> {
            Date productionDate = singleProductionDayInfo.getProductionDate();
            String dayFlag = singleProductionDayInfo.getDayFlag();
            Integer productionDay = DateUtils.getIntervalDays(productionStartDate, productionDate);
            Integer ratio = singleProductionDayInfo.getRate();
            if (YesOrNoEnum.YES.getCode().equals(dayFlag) && ratio > BigDecimal.ONE.intValue()) {
                productionDaySet.add(productionDay);
            }
        });
        return Lists.newArrayList(productionDaySet);
    }

    private ProductionCycleUtils() {

    }
}
