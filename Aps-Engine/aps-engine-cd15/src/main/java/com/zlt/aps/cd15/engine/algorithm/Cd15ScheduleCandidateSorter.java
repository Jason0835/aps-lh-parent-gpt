package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;
import com.zlt.aps.cd15.engine.model.Cd15MachineTailState;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
public class Cd15ScheduleCandidateSorter {

    /**
     * 按缺料、最早缺料时点、库存供应时长、钢带代码和裁断角度排序。
     *
     * @param candidates 待排规格
     * @return 新的有序列表，不修改输入列表
     */
    public List<Cd15ScheduleCandidate> sort(List<Cd15ScheduleCandidate> candidates) {
        return sort(candidates, Collections.emptyList());
    }

    /**
     * 兼容单一连续生产参照；当前班已有任务时使用该入口。
     */
    public List<Cd15ScheduleCandidate> sort(List<Cd15ScheduleCandidate> candidates,
                                            Cd15MachineTailState tail) {
        return sort(candidates, tail == null
                ? Collections.emptyList() : Collections.singletonList(tail));
    }

    /**
     * 相同缺料优先级内，对每个候选取全部机台尾状态中可获得的最佳连续等级。
     */
    public List<Cd15ScheduleCandidate> sort(List<Cd15ScheduleCandidate> candidates,
                                            Collection<Cd15MachineTailState> tails) {
        List<Cd15ScheduleCandidate> result = candidates == null
                ? new ArrayList<>() : new ArrayList<>(candidates);
        result.sort(Comparator
                .comparing(Cd15ScheduleCandidate::isShortageInCurrentShift, Comparator.reverseOrder())
                .thenComparing(Cd15ScheduleCandidate::isContinueFromPreviousShift, Comparator.reverseOrder())
                .thenComparing(Cd15ScheduleCandidate::isNewSpecAdvance, Comparator.reverseOrder())
                .thenComparing(Cd15ScheduleCandidate::getEarliestShortageTime,
                        Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparingInt(item -> continuityRank(item, tails))
                .thenComparing(Cd15ScheduleCandidate::getBigRollCode,
                        Comparator.nullsLast(String::compareTo))
                .thenComparing(Cd15ScheduleCandidate::getCuttingAngle,
                        Comparator.nullsLast(String::compareTo))
                .thenComparing(item -> value(item.getStockSupplyHours()))
                .thenComparing(Cd15ScheduleCandidate::getSteelStripCode,
                        Comparator.nullsLast(String::compareTo))
                .thenComparing(Cd15ScheduleCandidate::getMaterialKey,
                        Comparator.nullsLast(String::compareTo)));
        return result;
    }

    /** 候选在任意一台机台上可连续生产时，采用其中最优的连续等级。 */
    private int continuityRank(Cd15ScheduleCandidate item,
                               Collection<Cd15MachineTailState> tails) {
        if (item == null || tails == null || tails.isEmpty()) {
            return 3;
        }
        return tails.stream()
                .filter(Objects::nonNull)
                .mapToInt(tail -> continuityRank(item, tail))
                .min()
                .orElse(3);
    }

    private int continuityRank(Cd15ScheduleCandidate item, Cd15MachineTailState tail) {
        boolean sameSpec = StringUtils.hasText(tail.getMaterialKey())
                && StringUtils.hasText(item.getMaterialKey())
                ? Objects.equals(tail.getMaterialKey(), item.getMaterialKey())
                : Objects.equals(tail.getSteelStripCode(), item.getSteelStripCode())
                        && Objects.equals(tail.getCuttingAngle(), item.getCuttingAngle());
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