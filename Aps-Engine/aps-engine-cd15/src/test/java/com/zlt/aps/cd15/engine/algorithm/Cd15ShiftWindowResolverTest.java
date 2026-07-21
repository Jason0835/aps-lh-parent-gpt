package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftConfig;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/** 斜裁多班窗口时间解析测试。 */
public class Cd15ShiftWindowResolverTest {

    private final Cd15ShiftWindowResolver resolver = new Cd15ShiftWindowResolver();

    @Test
    public void shouldResolveScheduleDayAndCrossDayInStableOrder() {
        Cd15ShiftConfig class2 = shift("01", "夜班", "CLASS2", 1, 2, 2,
                "22:00:00", "06:00:00", 1);
        Cd15ShiftConfig class1 = shift("03", "中班", "CLASS1", 1, 1, 1,
                "14:00:00", "22:00:00", 0);

        List<Cd15ShiftDescriptor> result = resolver.resolve(
                LocalDate.of(2026, 6, 13), Arrays.asList(class2, class1));

        assertEquals("CLASS1", result.get(0).getClassField());
        assertEquals(LocalDateTime.of(2026, 6, 12, 14, 0), result.get(0).getStartTime());
        assertEquals(LocalDate.of(2026, 6, 12), result.get(0).getScheduleDate());
        assertEquals("中班06/12", result.get(0).getShiftDisplayName());
        assertEquals("01", result.get(1).getShiftCode());
        assertEquals(LocalDateTime.of(2026, 6, 12, 22, 0), result.get(1).getStartTime());
        assertEquals(LocalDateTime.of(2026, 6, 13, 6, 0), result.get(1).getEndTime());
        assertEquals(LocalDate.of(2026, 6, 13), result.get(1).getScheduleDate());
        assertEquals("夜班06/13", result.get(1).getShiftDisplayName());
        assertEquals(28800, result.get(1).getDurationSeconds());
    }

    @Test
    public void shouldUseCrossDayEndDateInDisplayName() {
        Cd15ShiftConfig class5 = shift("01", "夜班", "CLASS5", 2, 2, 5,
                "22:00:00", "06:00:00", 1);

        List<Cd15ShiftDescriptor> result = resolver.resolve(
                LocalDate.of(2026, 6, 13), Arrays.asList(class5));

        assertEquals(LocalDateTime.of(2026, 6, 13, 22, 0), result.get(0).getStartTime());
        assertEquals(LocalDate.of(2026, 6, 14), result.get(0).getScheduleDate());
        assertEquals("夜班06/14", result.get(0).getShiftDisplayName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNonStandardShiftCode() {
        resolver.resolve(LocalDate.of(2026, 6, 13), Arrays.asList(
                shift("NIGHT", "夜班", "CLASS1", 1, 1, 1,
                        "22:00:00", "06:00:00", 1)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectClassFieldOutsideOneToEight() {
        resolver.resolve(LocalDate.of(2026, 6, 13), Arrays.asList(
                shift("01", "夜班", "CLASS9", 1, 1, 1,
                        "22:00:00", "06:00:00", 1)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectDuplicateClassField() {
        resolver.resolve(LocalDate.of(2026, 6, 13), Arrays.asList(
                shift("01", "夜班", "CLASS1", 1, 1, 1,
                        "22:00:00", "06:00:00", 1),
                shift("02", "早班", "CLASS1", 1, 2, 2,
                        "06:00:00", "14:00:00", 0)));
    }

    private Cd15ShiftConfig shift(String shiftCode, String shiftName, String classField,
                                  int scheduleDay, int dayShiftOrder, int shiftOrder,
                                  String startTime, String endTime, int crossDay) {
        Cd15ShiftConfig config = new Cd15ShiftConfig();
        config.setShiftCode(shiftCode);
        config.setShiftName(shiftName);
        config.setClassField(classField);
        config.setScheduleDay(scheduleDay);
        config.setDayShiftOrder(dayShiftOrder);
        config.setShiftOrder(shiftOrder);
        config.setStartTime(startTime);
        config.setEndTime(endTime);
        config.setIsCrossDay(crossDay);
        config.setIsActive(1);
        return config;
    }
}