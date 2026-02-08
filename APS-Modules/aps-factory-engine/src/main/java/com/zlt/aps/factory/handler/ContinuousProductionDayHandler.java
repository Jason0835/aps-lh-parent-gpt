package com.zlt.aps.factory.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * 获取一段连续排产的日期
 *
 * @author ZLT
 * @date 20260109
 */
@Slf4j
public class ContinuousProductionDayHandler {

    /**
     * 获取最早一段连续排产日，结果中已经剔除了stopDay
     *
     * @param sumProductionDay 日期集合
     * @param stopDay          停工日集合
     * @return
     */
    public static Set<Integer> getEarliestContinuousRangeResultExcludeStop(Set<Integer> sumProductionDay, Set<Integer> stopDay) {
        Set<Integer> earliestContinuousRangeResult = getEarliestContinuousRange(sumProductionDay, stopDay);
        if (CollectionUtils.isEmpty(earliestContinuousRangeResult)) {
            return Collections.emptySet();
        }
        if (CollectionUtils.isEmpty(stopDay)) {
            return earliestContinuousRangeResult;
        }
        Set<Integer> excludeResult = new HashSet<>();
        earliestContinuousRangeResult.forEach(day -> {
            if (stopDay.contains(day)) {
                return;
            }
            excludeResult.add(day);
        });
        return excludeResult;
    }

    /**
     * 从sumProductionDay中获取日期最早的一段连续排产日
     * 此时返回的连续集合中还会包含stopDay
     *
     * @param sumProductionDay 日期集合
     * @param stopDay          停工集合
     * @return
     */
    public static Set<Integer> getEarliestContinuousRange(Set<Integer> sumProductionDay, Set<Integer> stopDay) {
        if (CollectionUtils.isEmpty(sumProductionDay)) {
            return Collections.emptySet();
        }
        Set<Integer> result = new HashSet<>();
        result.addAll(sumProductionDay);
        if (sumProductionDay.size() == BigDecimal.ONE.intValue()) {
            return result;
        }
        //没有停工日
        if (CollectionUtils.isEmpty(stopDay)) {
            return getEarliestContinuousRange(result);
        }
        //拼接停工日
        List<Integer> sortList = new ArrayList<>(sumProductionDay);
        //从小达到
        sortList.sort(Comparator.comparing(Integer::intValue));
        Integer minDay = sortList.get(BigDecimal.ZERO.intValue());
        Integer maxDay = sortList.get(sortList.size() - BigDecimal.ONE.intValue());
        for (int productionDay = minDay; productionDay <= maxDay; productionDay++) {
            if (stopDay.contains(productionDay)) {
                result.add(productionDay);
            }
        }
        return getEarliestContinuousRange(result);
    }

    /**
     * 从allDayInfo中取得最早的一段连续日集合
     *
     * @param allDayInfo 所有日期集合
     * @return
     */
    private static Set<Integer> getEarliestContinuousRange(Set<Integer> allDayInfo) {
        if (CollectionUtils.isEmpty(allDayInfo)) {
            return Collections.emptySet();
        }
        Set<Integer> result = new HashSet<>();
        if (allDayInfo.size() == BigDecimal.ONE.intValue()) {
            result.addAll(allDayInfo);
            return result;
        }
        //按数字升序，从小到大
        List<Integer> allList = new ArrayList<>(allDayInfo);
        Collections.sort(allList);
        //取得首个
        Integer start = allList.get(BigDecimal.ZERO.intValue());
        Integer end = start;
        Integer size = allList.size();
        for (int index = BigDecimal.ONE.intValue(); index < size; index++) {
            // 判断当前元素与前一个元素是否连续（差值为1即为连续）
            Integer currentValue = allList.get(index);
            Integer beforeValue = allList.get(index - BigDecimal.ONE.intValue());
            Integer addOne = beforeValue + BigDecimal.ONE.intValue();
            if (currentValue.equals(addOne)) {
                //扩展当前连续段的结束位置
                end = currentValue;
            } else {
                //遇到非连续元素，说明当前已找到最早的一段连续段（终止遍历，保证"最早"）
                break;
            }
        }
        //构造并返回最早的连续数字集合
        for (Integer resultIndex = start; resultIndex <= end; resultIndex++) {
            result.add(resultIndex);
        }
        return result;
    }

}
