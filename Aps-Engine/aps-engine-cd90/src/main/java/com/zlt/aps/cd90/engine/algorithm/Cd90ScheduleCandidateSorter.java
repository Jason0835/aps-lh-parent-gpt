package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90ScheduleCandidate;
import com.zlt.aps.cd90.engine.model.Cd90MachineTailState;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 当前班次待排规格稳定排序器。
 */
@Component
public class Cd90ScheduleCandidateSorter {

    /**
     * 按缺料、最早缺料时点、库存供应时长和帘布代码排序。
     *
     * @param candidates 待排规格
     * @return 新的有序列表，不修改输入列表
     */
    public List<Cd90ScheduleCandidate> sort(List<Cd90ScheduleCandidate> candidates) {
        return sort(candidates, Collections.emptyList());
    }

    /**
     * 兼容单一连续生产参照；当前班已有任务时使用该入口。
     */
    public List<Cd90ScheduleCandidate> sort(List<Cd90ScheduleCandidate> candidates,
                                            Cd90MachineTailState tail) {
        return sort(candidates, tail == null
                ? Collections.emptyList() : Collections.singletonList(tail));
    }

    /**
     * 相同缺料优先级内，对每个候选取全部机台尾状态中可获得的最佳连续等级。
     */
    public List<Cd90ScheduleCandidate> sort(List<Cd90ScheduleCandidate> candidates,
                                            Collection<Cd90MachineTailState> tails) {
        List<Cd90ScheduleCandidate> result = candidates == null
                ? new ArrayList<>() : new ArrayList<>(candidates);
        result.sort(Comparator
                .comparing(Cd90ScheduleCandidate::isShortageInCurrentShift).reversed()
                .thenComparing(Cd90ScheduleCandidate::getEarliestShortageTime,
                        Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparingInt(item -> continuityRank(item, tails))
                .thenComparing(item -> value(item.getStockSupplyHours()))
                .thenComparing(Cd90ScheduleCandidate::getClothCode,
                        Comparator.nullsLast(String::compareTo)));
        return result;
    }

    /** 候选在任意一台机台上可连续生产时，采用其中最优的连续等级。 */
    private int continuityRank(Cd90ScheduleCandidate item,
                               Collection<Cd90MachineTailState> tails) {
        if (item == null || tails == null || tails.isEmpty()) {
            return 3;
        }
        return tails.stream()
                .filter(Objects::nonNull)
                .mapToInt(tail -> continuityRank(item, tail))
                .min()
                .orElse(3);
    }

    private int continuityRank(Cd90ScheduleCandidate item, Cd90MachineTailState tail) {
        boolean sameSpec = Objects.equals(tail.getClothCode(), item.getClothCode());
        boolean sameRoll = Objects.equals(tail.getBigRollCode(), item.getBigRollCode());
        if (sameSpec && sameRoll) {
            return 0;
        }
        if (sameSpec) {
            return 1;
        }
        return sameRoll ? 2 : 3;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.valueOf(Long.MAX_VALUE) : value;
    }
}