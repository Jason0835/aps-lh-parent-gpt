package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftConfig;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import com.zlt.aps.common.core.enums.ThreeShiftEnum;
import com.zlt.aps.mdm.api.domain.entity.MdmWorkCalendar;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 根据斜裁工作日历识别停班后恢复生产的实际复产班次。 */
@Component
@RequiredArgsConstructor
public class Cd15RestartShiftResolver {

    private static final String ENABLED = "1";
    private static final String DISABLED = "0";

    private final Cd15ShiftWindowResolver shiftWindowResolver;

    /**
     * 仅当当前班在日历中明确开班，且前一自然班明确停班时，标记当前班为实际复产班次。
     * 工作日历缺失按普通开班处理，不触发SYS0601021例外。
     */
    public void markRestartShifts(
            List<Cd15ShiftDescriptor> shifts,
            List<Cd15ShiftConfig> enabledShiftConfigs,
            List<MdmWorkCalendar> calendars) {
        if (shifts == null || shifts.isEmpty()) {
            return;
        }
        Map<LocalDate, MdmWorkCalendar> calendarByDate = this.safe(calendars).stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getProductionDate() != null)
                .collect(Collectors.toMap(
                        item -> this.toLocalDate(item.getProductionDate()),
                        Function.identity(), (first, second) -> first,
                        LinkedHashMap::new));
        shifts.stream().filter(Objects::nonNull).forEach(shift -> {
            Cd15ShiftDescriptor previousShift = this.shiftWindowResolver
                    .resolveCurrentResourceShift(
                            shift.getStartTime().minusNanos(1),
                            enabledShiftConfigs);
            MdmWorkCalendar currentCalendar = calendarByDate.get(
                    shift.getScheduleDate());
            MdmWorkCalendar previousCalendar = calendarByDate.get(
                    previousShift.getScheduleDate());
            boolean restartStockMode = this.isExplicitlyOpen(
                    currentCalendar, shift.getShiftCode())
                    && this.isExplicitlyClosed(
                            previousCalendar, previousShift.getShiftCode());
            shift.setRestartStockMode(restartStockMode);
        });
    }

    private boolean isExplicitlyOpen(
            MdmWorkCalendar calendar, String shiftCode) {
        return calendar != null
                && !DISABLED.equals(calendar.getDayFlag())
                && ENABLED.equals(this.shiftFlag(calendar, shiftCode));
    }

    private boolean isExplicitlyClosed(
            MdmWorkCalendar calendar, String shiftCode) {
        return calendar != null
                && (DISABLED.equals(calendar.getDayFlag())
                        || DISABLED.equals(this.shiftFlag(calendar, shiftCode)));
    }

    private String shiftFlag(MdmWorkCalendar calendar, String shiftCode) {
        ThreeShiftEnum shift = ThreeShiftEnum.getByCode(shiftCode);
        if (shift == null) {
            throw new IllegalArgumentException("工作日历班次编码必须为01、02、03: " + shiftCode);
        }
        switch (shift) {
            case NIGHT:
                return calendar.getOneShiftFlag();
            case MORNING:
                return calendar.getTwoShiftFlag();
            case MIDDLE:
                return calendar.getThreeShiftFlag();
            default:
                throw new IllegalArgumentException("未支持的工作日历班次编码: " + shiftCode);
        }
    }

    private LocalDate toLocalDate(Date value) {
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private List<MdmWorkCalendar> safe(List<MdmWorkCalendar> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
