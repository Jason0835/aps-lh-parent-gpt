package com.zlt.aps.mp.engine.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 每日结构切换计算器
 *
 * @author ZLT
 * @date 20260321
 */
@Slf4j
public class GroupChangeCalculator {

    /**
     * 计算获取首个可以进行切换结构的日期
     * 前置条件：已经需要切换结构
     *
     * @param theoryChangeDay     理论切换的日
     * @param hasChangeGroupSet   有切换能力的日
     * @param hasProductionDaySet 有排产能力的日
     * @return
     */
    public static Integer getFirstHasChangeGroupDay(Integer theoryChangeDay, Set<Integer> hasChangeGroupSet, Set<Integer> hasProductionDaySet) {
        if (CollectionUtils.isEmpty(hasChangeGroupSet) || CollectionUtils.isEmpty(hasChangeGroupSet) || null == theoryChangeDay) {
            return null;
        }
        if (hasChangeGroupSet.contains(theoryChangeDay)) {
            return theoryChangeDay;
        }
        //提取在theoryChangeDay后，首个最小的日期
        List<Integer> afterTheoryChangeDayList = hasChangeGroupSet.stream().filter(singleDay -> singleDay >= theoryChangeDay).collect(Collectors.toList());
        afterTheoryChangeDayList.sort(Comparator.comparing(Integer::intValue));
        Integer nextTheoryChangeDay = afterTheoryChangeDayList.get(BigDecimal.ZERO.intValue());
        if (hasProductionDaySet.contains(nextTheoryChangeDay)) {
            return nextTheoryChangeDay;
        }
        List<Integer> afterProductionDayList = hasProductionDaySet.stream().filter(singleProductionDay -> singleProductionDay >= nextTheoryChangeDay).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(afterProductionDayList)) {
            return null;
        }
        afterProductionDayList.sort(Comparator.comparing(Integer::intValue));
        Integer nextStartProductionDay = afterProductionDayList.get(BigDecimal.ZERO.intValue());
        return getFirstHasChangeGroupDay(nextStartProductionDay, hasChangeGroupSet, hasProductionDaySet);
    }

}
