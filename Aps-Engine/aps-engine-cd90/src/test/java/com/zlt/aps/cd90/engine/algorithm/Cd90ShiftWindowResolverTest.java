package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.api.domain.entity.Cd90ShiftConfig;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDescriptor;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/** 直裁多班窗口时间解析测试。 */
public class Cd90ShiftWindowResolverTest {

    private final Cd90ShiftWindowResolver resolver = new Cd90ShiftWindowResolver();

    @Test
    public void shouldResolveScheduleDayAndCrossDayInStableOrder() {
        Cd90ShiftConfig class2 = shift("NIGHT", "CLASS2", 2, 1, 2,
                "22:00:00", "06:00:00", 1);
        Cd90ShiftConfig class1 = shift("MIDDLE", "CLASS1", 1, 3, 1,
                "14:00:00", "22:00:00", 0);

        List<Cd90ShiftDescriptor> result = resolver.resolve(
                LocalDate.of(2026, 6, 13), Arrays.asList(class2, class1));

        assertEquals("CLASS1", result.get(0).getClassField());
        assertEquals(LocalDateTime.of(2026, 6, 12, 14, 0), result.get(0).getStartTime());
        assertEquals("NIGHT", result.get(1).getShiftCode());
        assertEquals(LocalDateTime.of(2026, 6, 13, 22, 0), result.get(1).getStartTime());
        assertEquals(LocalDateTime.of(2026, 6, 14, 6, 0), result.get(1).getEndTime());
        assertEquals(28800, result.get(1).getDurationSeconds());
    }

    private Cd90ShiftConfig shift(String shiftCode, String classField, int scheduleDay,
                                  int dayShiftOrder, int shiftOrder, String startTime,
                                  String endTime, int crossDay) {
        Cd90ShiftConfig config = new Cd90ShiftConfig();
        config.setShiftCode(shiftCode);
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
