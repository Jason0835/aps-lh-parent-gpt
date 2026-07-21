package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;
import com.zlt.aps.cd15.engine.model.Cd15MachineTailState;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * 待排规格稳定排序测试。
 */
public class Cd15ScheduleCandidateSorterTest {

    private final Cd15ScheduleCandidateSorter sorter = new Cd15ScheduleCandidateSorter();

    /**
     * 当前班缺料优先，其次按续作、最早缺料时间、供应时长和钢带代码排序。
     */
    @Test
    public void shouldSortByStableBusinessPriority() {
        LocalDateTime base = LocalDateTime.of(2026, 6, 13, 14, 0);
        List<Cd15ScheduleCandidate> result = sorter.sort(Arrays.asList(
                candidate("C3", false, false, base, "2"),
                candidate("C2", true, false, base.plusHours(8), "1"),
                candidate("C1", true, false, base, "3"),
                candidate("C0", true, false, base, "2")
        ));

        assertEquals("C0", result.get(0).getSteelStripCode());
        assertEquals("C1", result.get(1).getSteelStripCode());
        assertEquals("C2", result.get(2).getSteelStripCode());
        assertEquals("C3", result.get(3).getSteelStripCode());
    }

    /**
     * 相同缺料优先级内，斜裁规格连续优先于大卷连续。
     */
    @Test
    public void shouldSortBySpecThenRollContinuity() {
        LocalDateTime shortageTime = LocalDateTime.of(2026, 6, 13, 14, 0);
        Cd15MachineTailState tail = Cd15MachineTailState.builder()
                .steelStripCode("C1").bigRollCode("R1").build();

        List<Cd15ScheduleCandidate> result = sorter.sort(Arrays.asList(
                candidate("C2", "R2", false, false, shortageTime, "1"),
                candidate("C2", "R1", false, false, shortageTime, "1"),
                candidate("C1", "R2", false, false, shortageTime, "1"),
                candidate("C1", "R1", false, false, shortageTime, "1")
        ), tail);

        assertEquals("R1/C1", key(result.get(0)));
        assertEquals("R2/C1", key(result.get(1)));
        assertEquals("R1/C2", key(result.get(2)));
        assertEquals("R2/C2", key(result.get(3)));
    }

    /**
     * 连续生产规则不能覆盖更紧急的本班缺料候选。
     */
    @Test
    public void urgentShortageShouldRemainFirst() {
        LocalDateTime shortageTime = LocalDateTime.of(2026, 6, 13, 14, 0);
        Cd15MachineTailState tail = Cd15MachineTailState.builder()
                .steelStripCode("C1").bigRollCode("R1").build();

        List<Cd15ScheduleCandidate> result = sorter.sort(Arrays.asList(
                candidate("C1", "R1", false, false, shortageTime, "1"),
                candidate("C2", "R2", true, false, shortageTime, "2")
        ), tail);

        assertEquals("R2/C2", key(result.get(0)));
    }

    /**
     * 同缺料状态下，续作规格排在新启动规格之前。
     */
    @Test
    public void shouldSortContinueOrderBeforeNewSpec() {
        LocalDateTime base = LocalDateTime.of(2026, 6, 13, 14, 0);
        List<Cd15ScheduleCandidate> result = sorter.sort(Arrays.asList(
                candidate("NEW1", false, false, base, "2"),
                candidate("CONT1", false, true, base, "2"),
                candidate("NEW2", false, false, base, "2"),
                candidate("CONT2", false, true, base, "2")
        ));

        assertEquals("CONT1", result.get(0).getSteelStripCode());
        assertEquals("CONT2", result.get(1).getSteelStripCode());
        assertEquals("NEW1", result.get(2).getSteelStripCode());
        assertEquals("NEW2", result.get(3).getSteelStripCode());
    }

    /**
     * 缺料优先级仍在续作之前：本班缺料的新规格排在非缺料的续作之前。
     */
    @Test
    public void shouldKeepShortageBeforeContinue() {
        LocalDateTime base = LocalDateTime.of(2026, 6, 13, 14, 0);
        List<Cd15ScheduleCandidate> result = sorter.sort(Arrays.asList(
                candidate("CONT", false, true, base.plusHours(8), "2"),
                candidate("NEW", true, false, base, "2")
        ));

        assertEquals("NEW", result.get(0).getSteelStripCode());
        assertEquals("CONT", result.get(1).getSteelStripCode());
    }

    /** 验证当班缺料、真实续作、新增规格提前生产和普通候选的顺序。 */
    @Test
    public void shouldSortShortageThenContinueThenNewSpecAdvance() {
        List<Cd15ScheduleCandidate> result = this.sorter.sort(Arrays.asList(
                Cd15ScheduleCandidate.builder().steelStripCode("NORMAL").build(),
                Cd15ScheduleCandidate.builder().steelStripCode("ADVANCE")
                        .newSpecAdvance(true).build(),
                Cd15ScheduleCandidate.builder().steelStripCode("CONTINUE")
                        .continueFromPreviousShift(true).build(),
                Cd15ScheduleCandidate.builder().steelStripCode("SHORTAGE")
                        .shortageInCurrentShift(true).build()));

        assertEquals("SHORTAGE", result.get(0).getSteelStripCode());
        assertEquals("CONTINUE", result.get(1).getSteelStripCode());
        assertEquals("ADVANCE", result.get(2).getSteelStripCode());
        assertEquals("NORMAL", result.get(3).getSteelStripCode());
    }
    private Cd15ScheduleCandidate candidate(String steelStripCode,
                                            boolean shortageInCurrentShift,
                                            boolean continueFromPreviousShift,
                                            LocalDateTime shortageTime,
                                            String supplyHours) {
        return Cd15ScheduleCandidate.builder()
                .steelStripCode(steelStripCode)
                .shortageInCurrentShift(shortageInCurrentShift)
                .continueFromPreviousShift(continueFromPreviousShift)
                .earliestShortageTime(shortageTime)
                .stockSupplyHours(new BigDecimal(supplyHours))
                .build();
    }

    private Cd15ScheduleCandidate candidate(String steelStripCode,
                                            String bigRollCode,
                                            boolean shortageInCurrentShift,
                                            boolean continueFromPreviousShift,
                                            LocalDateTime shortageTime,
                                            String supplyHours) {
        return Cd15ScheduleCandidate.builder()
                .steelStripCode(steelStripCode).bigRollCode(bigRollCode)
                .shortageInCurrentShift(shortageInCurrentShift)
                .continueFromPreviousShift(continueFromPreviousShift)
                .earliestShortageTime(shortageTime)
                .stockSupplyHours(new BigDecimal(supplyHours))
                .build();
    }

    private String key(Cd15ScheduleCandidate candidate) {
        return candidate.getBigRollCode() + "/" + candidate.getSteelStripCode();
    }
}
