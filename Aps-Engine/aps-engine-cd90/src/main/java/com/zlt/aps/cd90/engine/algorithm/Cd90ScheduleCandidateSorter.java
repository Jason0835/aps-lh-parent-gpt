package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90ScheduleCandidate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
        List<Cd90ScheduleCandidate> result = candidates == null
                ? new ArrayList<>() : new ArrayList<>(candidates);
        result.sort(Comparator
                .comparing(Cd90ScheduleCandidate::isShortageInCurrentShift).reversed()
                .thenComparing(Cd90ScheduleCandidate::getEarliestShortageTime,
                        Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparing(item -> value(item.getStockSupplyHours()))
                .thenComparing(Cd90ScheduleCandidate::getClothCode,
                        Comparator.nullsLast(String::compareTo)));
        return result;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.valueOf(Long.MAX_VALUE) : value;
    }
}
