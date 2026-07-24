package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftConfig;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import com.zlt.aps.mdm.api.domain.entity.MdmWorkCalendar;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 实际复产班次识别测试。 */
public class Cd15RestartShiftResolverTest {

    private final Cd15ShiftWindowResolver shiftWindowResolver =
            new Cd15ShiftWindowResolver();
    private final Cd15RestartShiftResolver resolver =
            new Cd15RestartShiftResolver(shiftWindowResolver);

    @Test
    public void explicitlyOpenShiftAfterClosedShiftShouldBeRestartShift() {
        List<Cd15ShiftConfig> configs = shiftConfigs();
        Cd15ShiftDescriptor current = shiftWindowResolver
                .resolveCurrentResourceShift(
                        LocalDateTime.of(2026, 7, 24, 8, 0), configs);
        MdmWorkCalendar calendar = calendar("2026-07-24", "0", "1", "1", "1");

        resolver.markRestartShifts(
                Collections.singletonList(current), configs,
                Collections.singletonList(calendar));

        assertTrue(current.isRestartStockMode());
    }

    @Test
    public void missingCalendarShouldRemainNormalShift() {
        List<Cd15ShiftConfig> configs = shiftConfigs();
        Cd15ShiftDescriptor current = shiftWindowResolver
                .resolveCurrentResourceShift(
                        LocalDateTime.of(2026, 7, 24, 8, 0), configs);

        resolver.markRestartShifts(
                Collections.singletonList(current), configs,
                Collections.emptyList());

        assertFalse(current.isRestartStockMode());
    }

    @Test
    public void continuouslyOpenShiftShouldRemainNormalShift() {
        List<Cd15ShiftConfig> configs = shiftConfigs();
        Cd15ShiftDescriptor current = shiftWindowResolver
                .resolveCurrentResourceShift(
                        LocalDateTime.of(2026, 7, 24, 8, 0), configs);
        MdmWorkCalendar calendar = calendar("2026-07-24", "1", "1", "1", "1");

        resolver.markRestartShifts(
                Collections.singletonList(current), configs,
                Collections.singletonList(calendar));

        assertFalse(current.isRestartStockMode());
    }

    private List<Cd15ShiftConfig> shiftConfigs() {
        return Arrays.asList(
                shift("01", "夜班", "CLASS1", "22:00:00", "06:00:00", 1),
                shift("02", "早班", "CLASS2", "06:00:00", "14:00:00", 0),
                shift("03", "中班", "CLASS3", "14:00:00", "22:00:00", 0));
    }

    private Cd15ShiftConfig shift(
            String shiftCode, String shiftName, String classField,
            String startTime, String endTime, int crossDay) {
        Cd15ShiftConfig config = new Cd15ShiftConfig();
        config.setShiftCode(shiftCode);
        config.setShiftName(shiftName);
        config.setClassField(classField);
        config.setScheduleDay(1);
        config.setDayShiftOrder(Integer.parseInt(shiftCode));
        config.setShiftOrder(Integer.parseInt(shiftCode));
        config.setStartTime(startTime);
        config.setEndTime(endTime);
        config.setIsCrossDay(crossDay);
        config.setIsActive(1);
        return config;
    }

    private MdmWorkCalendar calendar(
            String date, String oneShiftFlag, String twoShiftFlag,
            String threeShiftFlag, String dayFlag) {
        MdmWorkCalendar calendar = new MdmWorkCalendar();
        calendar.setProductionDate(Date.from(
                java.time.LocalDate.parse(date).atStartOfDay(
                        ZoneId.systemDefault()).toInstant()));
        calendar.setOneShiftFlag(oneShiftFlag);
        calendar.setTwoShiftFlag(twoShiftFlag);
        calendar.setThreeShiftFlag(threeShiftFlag);
        calendar.setDayFlag(dayFlag);
        return calendar;
    }
}
