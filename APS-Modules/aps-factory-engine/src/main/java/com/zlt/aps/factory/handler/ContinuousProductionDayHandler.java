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
     * 从sumProductionDay中获取日期最早的一段连续排产日
     *
     * @param sumProductionDay
     * @param stopDay
     * @return
     */
    public static Set<Integer> getEarliestContinuousRange(Set<Integer> sumProductionDay, Set<Integer> stopDay) {
        if (CollectionUtils.isEmpty(sumProductionDay)) {
            return Collections.emptySet();
        }
        Set<Integer> result = new HashSet<>();
        if (sumProductionDay.size() == BigDecimal.ONE.intValue()) {
            result.addAll(sumProductionDay);
            return result;
        }
        if (!CollectionUtils.isEmpty(stopDay)) {
            sumProductionDay.addAll(stopDay);
        }
        //按日期升序
        List<Integer> allList = new ArrayList<>(sumProductionDay);
        Collections.sort(allList);
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
