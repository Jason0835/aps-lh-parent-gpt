package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90ScheduleCandidate;
import com.zlt.aps.cd90.engine.model.Cd90MachineTailState;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * 待排规格稳定排序测试。
 */
public class Cd90ScheduleCandidateSorterTest {

    private final Cd90ScheduleCandidateSorter sorter = new Cd90ScheduleCandidateSorter();

    /**
     * 当前班缺料优先，其次按最早缺料时间、供应时长和帘布代码排序。
     */
    @Test
    public void shouldSortByStableBusinessPriority() {
        LocalDateTime base = LocalDateTime.of(2026, 6, 13, 14, 0);
        List<Cd90ScheduleCandidate> result = sorter.sort(Arrays.asList(
                candidate("C3", false, base, "2"),
                candidate("C2", true, base.plusHours(8), "1"),
                candidate("C1", true, base, "3"),
                candidate("C0", true, base, "2")
        ));

        assertEquals("C0", result.get(0).getClothCode());
        assertEquals("C1", result.get(1).getClothCode());
        assertEquals("C2", result.get(2).getClothCode());
        assertEquals("C3", result.get(3).getClothCode());
    }

    /**
     * 相同缺料优先级内，直裁规格连续优先于大卷连续。
     */
    @Test
    public void shouldSortBySpecThenRollContinuity() {
        LocalDateTime shortageTime = LocalDateTime.of(2026, 6, 13, 14, 0);
        Cd90MachineTailState tail = Cd90MachineTailState.builder()
                .clothCode("C1").bigRollCode("R1").build();

        List<Cd90ScheduleCandidate> result = sorter.sort(Arrays.asList(
                candidate("C2", "R2", true, shortageTime, "1"),
                candidate("C2", "R1", true, shortageTime, "1"),
                candidate("C1", "R2", true, shortageTime, "1"),
                candidate("C1", "R1", true, shortageTime, "1")
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
        Cd90MachineTailState tail = Cd90MachineTailState.builder()
                .clothCode("C1").bigRollCode("R1").build();

        List<Cd90ScheduleCandidate> result = sorter.sort(Arrays.asList(
                candidate("C1", "R1", false, shortageTime, "1"),
                candidate("C2", "R2", true, shortageTime, "2")
        ), tail);

        assertEquals("R2/C2", key(result.get(0)));
    }

    private Cd90ScheduleCandidate candidate(String clothCode,
                                            boolean shortageInCurrentShift,
                                            LocalDateTime shortageTime,
                                            String supplyHours) {
        return Cd90ScheduleCandidate.builder()
                .clothCode(clothCode)
                .shortageInCurrentShift(shortageInCurrentShift)
                .earliestShortageTime(shortageTime)
                .stockSupplyHours(new BigDecimal(supplyHours))
                .build();
    }

    private Cd90ScheduleCandidate candidate(String clothCode,
                                            String bigRollCode,
                                            boolean shortageInCurrentShift,
                                            LocalDateTime shortageTime,
                                            String supplyHours) {
        return Cd90ScheduleCandidate.builder()
                .clothCode(clothCode).bigRollCode(bigRollCode)
                .shortageInCurrentShift(shortageInCurrentShift)
                .earliestShortageTime(shortageTime)
                .stockSupplyHours(new BigDecimal(supplyHours))
                .build();
    }

    private String key(Cd90ScheduleCandidate candidate) {
        return candidate.getBigRollCode() + "/" + candidate.getClothCode();
    }
}
