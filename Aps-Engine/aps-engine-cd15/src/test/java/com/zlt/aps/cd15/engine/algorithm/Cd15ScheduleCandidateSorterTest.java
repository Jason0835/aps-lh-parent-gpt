package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;
import com.zlt.aps.cd15.engine.model.Cd15MachineTailState;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
     * 相同缺料优先级内，同大卷连续优先于跨大卷的同规格连续。
     */
    @Test
    public void shouldSortByRollBeforeSpecContinuity() {
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
        assertEquals("R1/C2", key(result.get(1)));
        assertEquals("R2/C1", key(result.get(2)));
        assertEquals("R2/C2", key(result.get(3)));
    }

    /**
     * 同大卷连续生产时，同角度候选必须先于需要切换角度的候选。
     */
    @Test
    public void shouldKeepSameAngleTogetherWithinSameRoll() {
        LocalDateTime shortageTime = LocalDateTime.of(2026, 7, 22, 0, 0);
        Cd15MachineTailState tail = Cd15MachineTailState.builder()
                .steelStripCode("211500015")
                .bigRollCode("CSSC6020")
                .cuttingAngle("24")
                .build();

        List<Cd15ScheduleCandidate> result = sorter.sort(Arrays.asList(
                candidate("211500012", "CSSC6020", "15", shortageTime),
                candidate("211500012", "CSSC6020", "24", shortageTime)
        ), tail);

        assertEquals("24", result.get(0).getCuttingAngle());
        assertEquals("15", result.get(1).getCuttingAngle());
    }

    /**
     * 同大卷需要反向切换角度时，应从当前角度向最近的反方向角度连续切换。
     */
    @Test
    public void shouldReverseAngleDirectionFromNearestAngleWithinSameRoll() {
        LocalDateTime shortageTime = LocalDateTime.of(2026, 7, 24, 14, 0);
        Cd15MachineTailState tail = Cd15MachineTailState.builder()
                .steelStripCode("CURRENT")
                .bigRollCode("CSS34524")
                .cuttingAngle("51")
                .build();

        List<Cd15ScheduleCandidate> result = sorter.sort(Arrays.asList(
                candidate("ANGLE15", "CSS34524", "15", shortageTime),
                candidate("ANGLE18", "CSS34524", "18", shortageTime)
        ), Arrays.asList(tail), "15");

        assertEquals("18", result.get(0).getCuttingAngle());
        assertEquals("15", result.get(1).getCuttingAngle());
    }

    /**
     * 角度方向只能在相同连续等级内生效，不得改变同大卷优先级。
     */
    @Test
    public void shouldKeepSameRollBeforeForwardAngleOnOtherRoll() {
        LocalDateTime shortageTime = LocalDateTime.of(2026, 7, 24, 14, 0);
        Cd15MachineTailState tail = Cd15MachineTailState.builder()
                .steelStripCode("CURRENT")
                .bigRollCode("CSS34524")
                .cuttingAngle("51")
                .build();

        List<Cd15ScheduleCandidate> result = sorter.sort(Arrays.asList(
                candidate("SAME_ROLL", "CSS34524", "15", shortageTime),
                candidate("OTHER_ROLL", "CSS44524", "60", shortageTime)
        ), Arrays.asList(tail), "15");

        assertEquals("CSS34524", result.get(0).getBigRollCode());
        assertEquals("CSS44524", result.get(1).getBigRollCode());
    }

    /**
     * 当前成型班不缺料时，未来缺料较早的远端角度不能打断单方向角度路线。
     */
    @Test
    public void shouldKeepFutureShortageBehindNonShortageAngleRoute() {
        LocalDateTime currentShift = LocalDateTime.of(2026, 7, 24, 14, 0);
        Cd15MachineTailState tail = Cd15MachineTailState.builder()
                .steelStripCode("CURRENT")
                .bigRollCode("CSS24524")
                .cuttingAngle("15")
                .build();

        List<Cd15ScheduleCandidate> result = sorter.sort(Arrays.asList(
                candidate("ANGLE51", "CSS24524", "51", currentShift.plusHours(8)),
                candidate("ANGLE24", "CSS24524", "24", currentShift.plusHours(16))
        ), Collections.singletonList(tail), "10");

        assertEquals("24", result.get(0).getCuttingAngle());
        assertEquals("51", result.get(1).getCuttingAngle());
    }

    /**
     * 非缺料大卷不得拆分，并应选择可衔接端点较多的起始卷以减少跨卷换角。
     */
    @Test
    public void shouldMinimizeAngleChangesWithoutSplittingBigRoll() {
        LocalDateTime futureShortage = LocalDateTime.of(2026, 7, 25, 6, 0);
        List<Cd15ScheduleCandidate> remaining = new ArrayList<>(Arrays.asList(
                candidate("A15", "CSS24524", "15", futureShortage),
                candidate("A51", "CSS24524", "51", futureShortage),
                candidate("B24", "CSS34524", "24", futureShortage),
                candidate("B51", "CSS34524", "51", futureShortage),
                candidate("C15", "CSSC6020", "15", futureShortage)
        ));
        List<String> route = new ArrayList<>();
        Cd15MachineTailState tail = null;
        String previousDifferentAngle = null;
        String currentAngle = null;

        while (!remaining.isEmpty()) {
            List<Cd15ScheduleCandidate> sorted = sorter.sort(
                    remaining,
                    tail == null ? Collections.emptyList() : Collections.singletonList(tail),
                    previousDifferentAngle);
            Cd15ScheduleCandidate selected = sorted.get(0);
            remaining.remove(selected);
            route.add(selected.getBigRollCode() + "/" + selected.getCuttingAngle());
            if (currentAngle != null && !currentAngle.equals(selected.getCuttingAngle())) {
                previousDifferentAngle = currentAngle;
            }
            currentAngle = selected.getCuttingAngle();
            tail = Cd15MachineTailState.builder()
                    .steelStripCode(selected.getSteelStripCode())
                    .bigRollCode(selected.getBigRollCode())
                    .cuttingAngle(selected.getCuttingAngle())
                    .build();
        }

        assertEquals(Arrays.asList(
                "CSSC6020/15",
                "CSS24524/15",
                "CSS24524/51",
                "CSS34524/51",
                "CSS34524/24"), route);
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

    private Cd15ScheduleCandidate candidate(String steelStripCode,
                                            String bigRollCode,
                                            String cuttingAngle,
                                            LocalDateTime shortageTime) {
        return Cd15ScheduleCandidate.builder()
                .steelStripCode(steelStripCode)
                .bigRollCode(bigRollCode)
                .cuttingAngle(cuttingAngle)
                .earliestShortageTime(shortageTime)
                .stockSupplyHours(BigDecimal.ONE)
                .build();
    }

    private String key(Cd15ScheduleCandidate candidate) {
        return candidate.getBigRollCode() + "/" + candidate.getSteelStripCode();
    }
}
