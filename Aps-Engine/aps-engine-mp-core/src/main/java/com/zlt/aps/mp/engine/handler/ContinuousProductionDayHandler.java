package com.zlt.aps.mp.engine.handler;

import com.zlt.aps.mp.engine.domain.Context;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

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
     * 从sumProductionDay中取得最早的一段连续排产日
     * 1、如果只有1天，则判断是否月末，月末允许
     * 2、否则，至少需要连续两天
     *
     * @param context          排产上下文
     * @param continueDays     可连续排产日
     * @param sumProductionDay 可排产日
     * @param stopDay          停工日
     * @return
     */
    public static Set<Integer> getEarliestContinuousRange(Context context, Integer continueDays, Set<Integer> sumProductionDay, Set<Integer> stopDay) {
        if (CollectionUtils.isEmpty(sumProductionDay) || null == continueDays || continueDays < BigDecimal.ONE.intValue()) {
            return Collections.emptySet();
        }
        Set<Integer> matchStopDay = Optional.ofNullable(stopDay).orElse(new HashSet<>());
        //先取得最早的一段可连续排产时间
        Set<Integer> earliestContinuousRangeResult = getEarliestContinuousRange(sumProductionDay, stopDay);
        if (CollectionUtils.isEmpty(earliestContinuousRangeResult)) {
            return Collections.emptySet();
        }
        List<Integer> realProductionDayResult = new ArrayList<>();
        earliestContinuousRangeResult.forEach(day -> {
            if (matchStopDay.contains(day)) {
                return;
            }
            realProductionDayResult.add(day);
        });
        //真实没有排产天数
        if (CollectionUtils.isEmpty(realProductionDayResult)) {
            return Collections.emptySet();
        }
        Integer realProductionDays = realProductionDayResult.size();
        //超过continueDays天
        if (realProductionDays >= continueDays) {
            return earliestContinuousRangeResult;
        }
        Set<Integer> realProductionDaySet = realProductionDayResult.stream().collect(Collectors.toSet());
        //只有一天：只看月末
        if (realProductionDays == BigDecimal.ONE.intValue()) {
            Integer monthEndDay = context.getProductionEndDay();
            Integer realProductionDay = realProductionDayResult.get(BigDecimal.ZERO.intValue());
            if (monthEndDay.equals(realProductionDay)) {
                return realProductionDaySet;
            }
            //不是在月末，重新获取
            Set<Integer> newSumProductionDay = extractRetainDay(sumProductionDay, realProductionDaySet);
            if (CollectionUtils.isEmpty(newSumProductionDay)) {
                return Collections.emptySet();
            }
            return getEarliestContinuousRange(context, continueDays, newSumProductionDay, stopDay);
        }
        //剔除之后，重新获取
        Set<Integer> newSumProductionDay = extractRetainDay(sumProductionDay, realProductionDaySet);
        if (CollectionUtils.isEmpty(newSumProductionDay)) {
            return Collections.emptySet();
        }
        return getEarliestContinuousRange(context, continueDays, newSumProductionDay, stopDay);
    }

    /**
     * 得到满足大于dayCount的集合
     *
     * @param context     排产上下文
     * @param preDayRange 预计集合
     * @param dayCount    需要大于数
     * @return
     */
    public static Set<Integer> getGreaterDayRange(Context context, Set<Integer> preDayRange, Integer dayCount) {
        if (CollectionUtils.isEmpty(preDayRange)) {
            return Collections.emptySet();
        }
        Integer minSize = Optional.ofNullable(dayCount).orElse(BigDecimal.ONE.intValue());
        Integer days = preDayRange.size();
        if (days >= minSize) {
            return preDayRange;
        }
        List<Integer> dayRangeList = preDayRange.stream().collect(Collectors.toList());
        if (days == BigDecimal.ONE.intValue()) {
            Integer day = dayRangeList.get(BigDecimal.ZERO.intValue());
            if (context.getProductionEndDay().equals(day)) {
                return preDayRange;
            }
            return Collections.emptySet();
        }
        return Collections.emptySet();
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
        //从小到大排序
        sortList.sort(Comparator.comparing(Integer::intValue));
        Integer minDay = sortList.get(BigDecimal.ZERO.intValue());
        Integer maxDay = sortList.get(sortList.size() - BigDecimal.ONE.intValue());
        //在[minDay,maxDay]之间的停工日加入
        for (int productionDay = minDay; productionDay <= maxDay; productionDay++) {
            if (stopDay.contains(productionDay)) {
                result.add(productionDay);
            }
        }
        return getEarliestContinuousRange(result);
    }

    /**
     * 提取真实排产日期集合
     * 从allDays中移除rejectDays集合信息
     * 并得到新的集合对象
     *
     * @param allDays    所有集合信息
     * @param rejectDays 需要移除的集合信息
     * @return
     */
    public static Set<Integer> extractRetainDay(Set<Integer> allDays, Set<Integer> rejectDays) {
        if (CollectionUtils.isEmpty(allDays)) {
            return Collections.emptySet();
        }
        Set<Integer> realRetainDaySet = new HashSet<>();
        Set<Integer> matchRejectDays = Optional.ofNullable(rejectDays).orElse(Collections.emptySet());
        allDays.forEach(day -> {
            if (matchRejectDays.contains(day)) {
                return;
            }
            realRetainDaySet.add(day);
        });
        return realRetainDaySet;
    }

    /**
     * 获取有效的排产范围，月末可一天
     * 否则至少需要两天
     *
     * @param context          排产上下文
     * @param sumProductionDay 所有排产天数
     * @param stopDay          停产天数
     * @return
     */
    private static EffectiveResult getEffectiveEarliestContinuousRange(Context context, Set<Integer> sumProductionDay, Set<Integer> stopDay) {
        if (CollectionUtils.isEmpty(sumProductionDay)) {
            return new EffectiveResult(true, Collections.emptySet());
        }
        Set<Integer> rangeSet = getEarliestContinuousRange(sumProductionDay, stopDay);
        if (CollectionUtils.isEmpty(rangeSet)) {
            return new EffectiveResult(true, rangeSet);
        }
        Set<Integer> effectiveSet;
        if (CollectionUtils.isEmpty(stopDay)) {
            effectiveSet = rangeSet;
        } else {
            effectiveSet = new HashSet<>();
            rangeSet.forEach(day -> {
                if (stopDay.contains(day)) {
                    return;
                }
                effectiveSet.add(day);
            });
        }
        if (CollectionUtils.isEmpty(effectiveSet)) {
            return new EffectiveResult(true, Collections.emptySet());
        }
        if (effectiveSet.size() > BigDecimal.ONE.intValue()) {
            return new EffectiveResult(true, rangeSet);
        }
        Integer productionDay = new ArrayList<>(effectiveSet).get(BigDecimal.ZERO.intValue());
        Integer monthEndDay = context.getProductionEndDay();
        if (monthEndDay.equals(productionDay)) {
            return new EffectiveResult(true, rangeSet);
        }
        Set<Integer> newSumProductionDay = new HashSet<>();
        sumProductionDay.forEach(day -> {
            if (productionDay.equals(day)) {
                return;
            }
            newSumProductionDay.add(day);
        });
        return new EffectiveResult(false, newSumProductionDay);
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

/**
 * 有效结果
 */
class EffectiveResult {

    private boolean effectiveFlag;

    private Set<Integer> productionRange;

    public EffectiveResult(boolean effectiveFlag, Set<Integer> productionRange) {
        this.effectiveFlag = effectiveFlag;
        this.productionRange = productionRange;
    }

    public boolean isEffectiveFlag() {
        return effectiveFlag;
    }

    public Set<Integer> getProductionRange() {
        return productionRange;
    }
}
