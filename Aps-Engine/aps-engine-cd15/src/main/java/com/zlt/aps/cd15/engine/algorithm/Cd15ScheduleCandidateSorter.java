package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * CD15 待排候选稳定排序器。
 */
@Component
public class Cd15ScheduleCandidateSorter {

    /**
     * 按 CLASS 自然顺序优先，再按缺料、续作、角度、钢带和大卷稳定排序。
     * 角度排序先采用字典序，后续接入机台尾料/角度切换成本时只替换本排序器。
     *
     * @param candidates 待排候选
     * @return 排序后的新列表
     */
    public List<Cd15ScheduleCandidate> sort(List<Cd15ScheduleCandidate> candidates) {
        List<Cd15ScheduleCandidate> result = candidates == null
                ? new ArrayList<>() : new ArrayList<>(candidates);
        result.sort(Comparator.comparing(Cd15ScheduleCandidate::getClassIndex)
                .thenComparing(Cd15ScheduleCandidate::isShortageInCurrentShift, Comparator.reverseOrder())
                .thenComparing(Cd15ScheduleCandidate::isContinueFromPreviousShift, Comparator.reverseOrder())
                .thenComparing(Cd15ScheduleCandidate::getCuttingAngle, Comparator.nullsLast(String::compareTo))
                .thenComparing(Cd15ScheduleCandidate::getSteelStripCode, Comparator.nullsLast(String::compareTo))
                .thenComparing(Cd15ScheduleCandidate::getBigRollCode, Comparator.nullsLast(String::compareTo)));
        return Collections.unmodifiableList(result);
    }
}