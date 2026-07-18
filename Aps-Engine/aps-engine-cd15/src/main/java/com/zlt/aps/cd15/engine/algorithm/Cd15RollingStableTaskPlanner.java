package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15RollingPendingTask;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;
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
public class Cd15RollingStableTaskPlanner {

    private final Cd15ScheduleCandidateSorter candidateSorter;

    /** 按固定分区输出稳定任务链，不修改输入集合。 */
    public List<Cd15RollingPendingTask> plan(
            List<Cd15RollingPendingTask> lockedTasks,
            List<Cd15RollingPendingTask> carryOverTasks,
            List<Cd15RollingPendingTask> originalTasks,
            List<Cd15RollingPendingTask> newTasks,
            List<Cd15ScheduleCandidate> urgentCandidates) {
        List<Cd15RollingPendingTask> original = stableSort(originalTasks);
        List<Cd15RollingPendingTask> newItems = safe(newTasks);
        Map<String, Cd15RollingPendingTask> newByKey = newItems.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getTaskKey() != null)
                .collect(Collectors.toMap(Cd15RollingPendingTask::getTaskKey,
                        Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<Cd15RollingPendingTask> urgent = candidateSorter.sort(safeCandidates(urgentCandidates))
                .stream()
                .map(Cd15ScheduleCandidate::getRollingTaskKey)
                .map(newByKey::get)
                .filter(Objects::nonNull)
                .filter(Cd15RollingPendingTask::isUrgentCurrentShiftShortage)
                .collect(Collectors.toList());
        List<Cd15RollingPendingTask> normalNew = newItems.stream()
                .filter(Objects::nonNull)
                .filter(item -> !item.isUrgentCurrentShiftShortage())
                .sorted(stableComparator())
                .collect(Collectors.toList());

        Map<String, Cd15RollingPendingTask> result = new LinkedHashMap<>();
        append(result, stableSort(lockedTasks));
        append(result, stableSort(carryOverTasks));
        append(result, urgent);
        append(result, original);
        append(result, normalNew);
        return new ArrayList<>(result.values());
    }

    /**
     * 按稳定任务顺序逐条消耗施工材料净需求，防止同钢带多条旧记录重复读取整段需求。
     */
    public void allocateRequestedQuantity(List<Cd15RollingPendingTask> orderedTasks,
                                          Map<String, BigDecimal> demandByMaterial,
                                          List<Cd15ScheduleCandidate> candidates) {
        Map<String, BigDecimal> remainingDemand = demandByMaterial == null
                ? new LinkedHashMap<>() : demandByMaterial.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> nonNegative(entry.getValue()),
                        (left, right) -> left, LinkedHashMap::new));
        Map<String, Cd15ScheduleCandidate> candidateByKey = safeCandidates(candidates).stream()
                .filter(item -> item.getRollingTaskKey() != null)
                .collect(Collectors.toMap(Cd15ScheduleCandidate::getRollingTaskKey,
                        Function.identity(), (left, right) -> left, LinkedHashMap::new));
        candidateByKey.values().forEach(item ->
                item.setRollingRequestedQuantity(BigDecimal.ZERO));
        safe(orderedTasks).stream().filter(Objects::nonNull).forEach(task -> {
            Cd15ScheduleCandidate candidate = candidateByKey.get(task.getTaskKey());
            if (candidate == null || task.getMaterialKey() == null) {
                return;
            }
            BigDecimal demand = remainingDemand.getOrDefault(
                    task.getMaterialKey(), BigDecimal.ZERO);
            BigDecimal capacity = nonNegative(task.getRemainingQuantity());
            BigDecimal requested = demand.min(capacity);
            candidate.setRollingRequestedQuantity(requested);
            remainingDemand.put(task.getMaterialKey(), demand.subtract(requested));
        });
    }

    private void append(Map<String, Cd15RollingPendingTask> target,
                        Collection<Cd15RollingPendingTask> source) {
        source.stream().filter(Objects::nonNull).forEach(item -> {
            String key = item.getTaskKey() == null
                    ? "ANONYMOUS-" + target.size() : item.getTaskKey();
            target.putIfAbsent(key, item);
        });
    }

    private List<Cd15RollingPendingTask> stableSort(List<Cd15RollingPendingTask> tasks) {
        return safe(tasks).stream().filter(Objects::nonNull)
                .sorted(stableComparator()).collect(Collectors.toList());
    }

    private Comparator<Cd15RollingPendingTask> stableComparator() {
        return Comparator.comparingInt(item -> item.getStableOrder() <= 0
                ? Integer.MAX_VALUE : item.getStableOrder());
    }

    private List<Cd15RollingPendingTask> safe(List<Cd15RollingPendingTask> tasks) {
        return tasks == null ? Collections.emptyList() : tasks;
    }

    private List<Cd15ScheduleCandidate> safeCandidates(List<Cd15ScheduleCandidate> candidates) {
        return candidates == null ? Collections.emptyList() : candidates;
    }

    private BigDecimal nonNegative(BigDecimal value) {
        return value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
    }
}
