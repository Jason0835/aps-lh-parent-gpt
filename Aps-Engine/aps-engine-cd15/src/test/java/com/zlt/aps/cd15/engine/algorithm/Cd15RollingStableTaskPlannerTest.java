package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15RollingPendingTask;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class Cd15RollingStableTaskPlannerTest {

    private final Cd15RollingStableTaskPlanner planner =
            new Cd15RollingStableTaskPlanner(new Cd15ScheduleCandidateSorter());

    @Test
    public void shouldKeepLockedCarryOverAndExistingRelativeOrder() {
        Cd15RollingPendingTask locked = task("L", 1, false);
        locked.setLocked(true);
        Cd15RollingPendingTask carryOver = task("P", 1, false);
        carryOver.setContinueFromPreviousShift(true);
        Cd15RollingPendingTask first = task("A", 2, false);
        Cd15RollingPendingTask second = task("B", 3, false);
        Cd15RollingPendingTask newTask = task("C", Integer.MAX_VALUE, false);

        List<Cd15RollingPendingTask> planned = planner.plan(
                Collections.singletonList(locked), Collections.singletonList(carryOver),
                Arrays.asList(second, first), Collections.singletonList(newTask),
                Collections.emptyList());

        assertEquals(Arrays.asList("L", "P", "A", "B", "C"), keys(planned));
    }

    @Test
    public void shouldInsertUrgentCandidatesUsingAutoScheduleSorter() {
        Cd15RollingPendingTask original = task("A", 2, false);
        Cd15RollingPendingTask urgentLater = task("U2", Integer.MAX_VALUE, true);
        Cd15RollingPendingTask urgentFirst = task("U1", Integer.MAX_VALUE, true);
        Cd15ScheduleCandidate candidateLater = candidate("U2", false,
                LocalDateTime.of(2026, 7, 3, 15, 0));
        Cd15ScheduleCandidate candidateFirst = candidate("U1", true,
                LocalDateTime.of(2026, 7, 3, 16, 0));

        List<Cd15RollingPendingTask> planned = planner.plan(
                Collections.emptyList(), Collections.emptyList(),
                Collections.singletonList(original), Arrays.asList(urgentLater, urgentFirst),
                Arrays.asList(candidateLater, candidateFirst));

        assertEquals(Arrays.asList("U1", "U2", "A"), keys(planned));
    }

    @Test
    public void shouldAllocateSteelStripDemandOnceAcrossExistingTasks() {
        Cd15RollingPendingTask first = task("A", 1, false);
        first.setSteelStripCode("C01");
        first.setMaterialKey("C01|BR1|15");
        first.setRemainingQuantity(new BigDecimal("60"));
        Cd15RollingPendingTask second = task("B", 2, false);
        second.setSteelStripCode("C01");
        second.setMaterialKey("C01|BR1|15");
        second.setRemainingQuantity(new BigDecimal("60"));
        Cd15ScheduleCandidate firstCandidate = candidate("A", false, null);
        Cd15ScheduleCandidate secondCandidate = candidate("B", false, null);

        planner.allocateRequestedQuantity(Arrays.asList(first, second),
                Collections.singletonMap("C01|BR1|15", new BigDecimal("100")),
                Arrays.asList(firstCandidate, secondCandidate));

        assertEquals(new BigDecimal("60"), firstCandidate.getRollingRequestedQuantity());
        assertEquals(new BigDecimal("40"), secondCandidate.getRollingRequestedQuantity());
    }

    private Cd15RollingPendingTask task(String key, int stableOrder, boolean urgent) {
        return Cd15RollingPendingTask.builder().taskKey(key).stableOrder(stableOrder)
                .urgentCurrentShiftShortage(urgent).build();
    }

    private Cd15ScheduleCandidate candidate(String key, boolean shortage,
                                             LocalDateTime shortageTime) {
        return Cd15ScheduleCandidate.builder().rollingTaskKey(key)
                .shortageInCurrentShift(shortage).earliestShortageTime(shortageTime).build();
    }

    private List<String> keys(List<Cd15RollingPendingTask> tasks) {
        return tasks.stream().map(Cd15RollingPendingTask::getTaskKey)
                .collect(Collectors.toList());
    }
}
