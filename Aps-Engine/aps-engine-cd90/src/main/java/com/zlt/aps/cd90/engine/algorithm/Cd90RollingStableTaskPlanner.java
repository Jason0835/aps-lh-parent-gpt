package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90RollingPendingTask;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleCandidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 定时滚动排程的锁定区和稳定顺序规划器。 */
@Component
@RequiredArgsConstructor
public class Cd90RollingStableTaskPlanner {

    private final Cd90ScheduleCandidateSorter candidateSorter;

    /** 按固定分区输出稳定任务链，不修改输入集合。 */
    public List<Cd90RollingPendingTask> plan(
            List<Cd90RollingPendingTask> lockedTasks,
            List<Cd90RollingPendingTask> carryOverTasks,
            List<Cd90RollingPendingTask> originalTasks,
            List<Cd90RollingPendingTask> newTasks,
            List<Cd90ScheduleCandidate> urgentCandidates) {
        List<Cd90RollingPendingTask> original = stableSort(originalTasks);
        List<Cd90RollingPendingTask> newItems = safe(newTasks);
        Map<String, Cd90RollingPendingTask> newByKey = newItems.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getTaskKey() != null)
                .collect(Collectors.toMap(Cd90RollingPendingTask::getTaskKey,
                        Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<Cd90RollingPendingTask> urgent = candidateSorter.sort(safeCandidates(urgentCandidates))
                .stream()
                .map(Cd90ScheduleCandidate::getRollingTaskKey)
                .map(newByKey::get)
                .filter(Objects::nonNull)
                .filter(Cd90RollingPendingTask::isUrgentCurrentShiftShortage)
                .collect(Collectors.toList());
        List<Cd90RollingPendingTask> normalNew = newItems.stream()
                .filter(Objects::nonNull)
                .filter(item -> !item.isUrgentCurrentShiftShortage())
                .sorted(stableComparator())
                .collect(Collectors.toList());

        Map<String, Cd90RollingPendingTask> result = new LinkedHashMap<>();
        append(result, stableSort(lockedTasks));
        append(result, stableSort(carryOverTasks));
        append(result, urgent);
        append(result, original);
        append(result, normalNew);
        return new ArrayList<>(result.values());
    }

    /**
     * 按稳定任务顺序逐条消耗帘布净需求，防止同帘布多条旧记录重复读取整段需求。
     */
    public void allocateRequestedQuantity(List<Cd90RollingPendingTask> orderedTasks,
                                          Map<String, BigDecimal> demandByCloth,
                                          List<Cd90ScheduleCandidate> candidates) {
        Map<String, BigDecimal> remainingDemand = demandByCloth == null
                ? new LinkedHashMap<>() : demandByCloth.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> nonNegative(entry.getValue()),
                        (left, right) -> left, LinkedHashMap::new));
        Map<String, Cd90ScheduleCandidate> candidateByKey = safeCandidates(candidates).stream()
                .filter(item -> item.getRollingTaskKey() != null)
                .collect(Collectors.toMap(Cd90ScheduleCandidate::getRollingTaskKey,
                        Function.identity(), (left, right) -> left, LinkedHashMap::new));
        candidateByKey.values().forEach(item ->
                item.setRollingRequestedQuantity(BigDecimal.ZERO));
        safe(orderedTasks).stream().filter(Objects::nonNull).forEach(task -> {
            Cd90ScheduleCandidate candidate = candidateByKey.get(task.getTaskKey());
            if (candidate == null || task.getClothCode() == null) {
                return;
            }
            BigDecimal demand = remainingDemand.getOrDefault(
                    task.getClothCode(), BigDecimal.ZERO);
            BigDecimal capacity = nonNegative(task.getRemainingQuantity());
            BigDecimal requested = demand.min(capacity);
            candidate.setRollingRequestedQuantity(requested);
            remainingDemand.put(task.getClothCode(), demand.subtract(requested));
        });
    }

    private void append(Map<String, Cd90RollingPendingTask> target,
                        Collection<Cd90RollingPendingTask> source) {
        source.stream().filter(Objects::nonNull).forEach(item -> {
            String key = item.getTaskKey() == null
                    ? "ANONYMOUS-" + target.size() : item.getTaskKey();
            target.putIfAbsent(key, item);
        });
    }

    private List<Cd90RollingPendingTask> stableSort(List<Cd90RollingPendingTask> tasks) {
        return safe(tasks).stream().filter(Objects::nonNull)
                .sorted(stableComparator()).collect(Collectors.toList());
    }

    private Comparator<Cd90RollingPendingTask> stableComparator() {
        return Comparator.comparingInt(item -> item.getStableOrder() <= 0
                ? Integer.MAX_VALUE : item.getStableOrder());
    }

    private List<Cd90RollingPendingTask> safe(List<Cd90RollingPendingTask> tasks) {
        return tasks == null ? Collections.emptyList() : tasks;
    }

    private List<Cd90ScheduleCandidate> safeCandidates(List<Cd90ScheduleCandidate> candidates) {
        return candidates == null ? Collections.emptyList() : candidates;
    }

    private BigDecimal nonNegative(BigDecimal value) {
        return value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
    }
}
