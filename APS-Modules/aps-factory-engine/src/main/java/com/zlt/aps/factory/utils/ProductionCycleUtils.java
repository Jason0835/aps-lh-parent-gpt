package com.zlt.aps.factory.utils;

import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.domain.vo.ProductionDayInfoVo;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
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
        Set<Integer> productionDaySet = new HashSet<>();
        productionDayInfo.forEach(singleProductionDayInfo -> {
            Date productionDate = singleProductionDayInfo.getProductionDate();
            String dayFlag = singleProductionDayInfo.getDayFlag();
            Integer productionDay = DateUtils.getIntervalDays(productionStartDate, productionDate);
            Integer ratio = singleProductionDayInfo.getRate();
            if (YesOrNoEnum.YES.getCode().equals(dayFlag) && ratio > BigDecimal.ONE.intValue()) {
                productionDaySet.add(productionDay);
            }
        });
        List<Integer> productionDayList = new ArrayList<>(productionDaySet);
        if (CollectionUtils.isEmpty(productionDayList)) {
            return BigDecimal.ZERO.intValue();
        }
        //升序排序，取最后一天
        productionDayList.sort(Comparator.comparing(Integer::intValue));
        return productionDayList.get(productionDayList.size() - BigDecimal.ONE.intValue());
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

    private ProductionCycleUtils() {

    }
}
