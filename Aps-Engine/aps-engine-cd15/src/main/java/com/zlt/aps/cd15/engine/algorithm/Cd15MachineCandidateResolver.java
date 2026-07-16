package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineMaintenancePlan;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineRollMapping;
import com.zlt.aps.cd15.api.domain.entity.Cd15SpecifyMachine;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;
import com.zlt.aps.cd15.engine.model.Cd15SplitCutGroup;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.enums.ThreeShiftEnum;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CD15 机台硬条件候选过滤。
 */
@Component
public class Cd15MachineCandidateResolver {

    public static final String WIDTH_MISMATCH = "WIDTH_MISMATCH";
    public static final String NO_AVAILABLE_MACHINE = "NO_AVAILABLE_MACHINE";

    /**
     * 兼容单班试排的最小过滤：启用状态与 CLOTH_WIDTH_MIN/CLOTH_WIDTH_MAX 宽度过滤。
     */
    public Optional<Cd15MachineInfo> resolve(List<Cd15MachineInfo> machines, BigDecimal effectiveWidth) {
        return this.safe(machines).stream()
                .filter(machine -> this.supports(machine, effectiveWidth))
                .sorted(Comparator.comparing(Cd15MachineInfo::getMachineCode))
                .findFirst();
    }

    /**
     * 按完整输入约束筛选单条候选机台。
     */
    public Optional<Cd15MachineInfo> resolve(Cd15AutoScheduleInput input,
                                             Cd15ScheduleCandidate candidate,
                                             BigDecimal effectiveWidth) {
        if (candidate == null) {
            return Optional.empty();
        }
        return this.resolveInternal(input, candidate.getBigRollCode(), candidate.getCuttingAngle(),
                effectiveWidth, candidate.getClassIndex(), Collections.singletonList(candidate.getSteelStripCode()));
    }

    /**
     * 按完整输入约束筛选分裁组合机台。
     */
    public Optional<Cd15MachineInfo> resolve(Cd15AutoScheduleInput input, Cd15SplitCutGroup splitGroup) {
        if (splitGroup == null || splitGroup.getFirstCandidate() == null || splitGroup.getSecondCandidate() == null) {
            return Optional.empty();
        }
        Cd15ScheduleCandidate firstCandidate = splitGroup.getFirstCandidate();
        return this.resolveInternal(input, firstCandidate.getBigRollCode(), firstCandidate.getCuttingAngle(),
                splitGroup.getCombinedWidth(), firstCandidate.getClassIndex(),
                Arrays.asList(firstCandidate.getSteelStripCode(), splitGroup.getSecondCandidate().getSteelStripCode()));
    }

    public boolean supports(Cd15MachineInfo machine, BigDecimal effectiveWidth) {
        return machine != null
                && ApsConstant.APS_STRING_1.equals(machine.getStatus())
                && this.widthMatched(machine, effectiveWidth);
    }

    public String resolveFailureReason(Cd15AutoScheduleInput input,
                                       Cd15ScheduleCandidate candidate,
                                       BigDecimal effectiveWidth) {
        if (candidate == null) {
            return NO_AVAILABLE_MACHINE;
        }
        return this.resolveFailureReason(input, candidate.getBigRollCode(), candidate.getCuttingAngle(),
                effectiveWidth, candidate.getClassIndex(), Collections.singletonList(candidate.getSteelStripCode()));
    }

    public String resolveFailureReason(Cd15AutoScheduleInput input, Cd15SplitCutGroup splitGroup) {
        if (splitGroup == null || splitGroup.getFirstCandidate() == null || splitGroup.getSecondCandidate() == null) {
            return NO_AVAILABLE_MACHINE;
        }
        Cd15ScheduleCandidate firstCandidate = splitGroup.getFirstCandidate();
        return this.resolveFailureReason(input, firstCandidate.getBigRollCode(), firstCandidate.getCuttingAngle(),
                splitGroup.getCombinedWidth(), firstCandidate.getClassIndex(),
                Arrays.asList(firstCandidate.getSteelStripCode(), splitGroup.getSecondCandidate().getSteelStripCode()));
    }

    public String resolveFailureReason(Cd15MachineInfo machine, BigDecimal effectiveWidth) {
        if (machine != null && ApsConstant.APS_STRING_1.equals(machine.getStatus())
                && !this.widthMatched(machine, effectiveWidth)) {
            return WIDTH_MISMATCH;
        }
        return NO_AVAILABLE_MACHINE;
    }

    private Optional<Cd15MachineInfo> resolveInternal(Cd15AutoScheduleInput input,
                                                      String bigRollCode,
                                                      String cuttingAngle,
                                                      BigDecimal effectiveWidth,
                                                      int classIndex,
                                                      List<String> steelStripCodes) {
        MachineFilterContext context = this.context(input, bigRollCode, cuttingAngle,
                effectiveWidth, classIndex, steelStripCodes);
        return this.safe(input == null ? Collections.emptyList() : input.getMachines()).stream()
                .filter(machine -> this.machineMatched(machine, context))
                .sorted(Comparator.comparing((Cd15MachineInfo machine) -> this.preferred(machine, context)).reversed()
                        .thenComparing(Cd15MachineInfo::getMachineCode))
                .findFirst();
    }

    /**
     * 候选为空时，只有存在“除宽度外其它硬约束均通过”的机台时才返回 WIDTH_MISMATCH。
     */
    private String resolveFailureReason(Cd15AutoScheduleInput input,
                                        String bigRollCode,
                                        String cuttingAngle,
                                        BigDecimal effectiveWidth,
                                        int classIndex,
                                        List<String> steelStripCodes) {
        MachineFilterContext context = this.context(input, bigRollCode, cuttingAngle,
                effectiveWidth, classIndex, steelStripCodes);
        boolean onlyWidthMismatch = this.safe(input == null ? Collections.emptyList() : input.getMachines()).stream()
                .anyMatch(machine -> this.machineMatchedExceptWidth(machine, context)
                        && !this.widthMatched(machine, effectiveWidth));
        return onlyWidthMismatch ? WIDTH_MISMATCH : NO_AVAILABLE_MACHINE;
    }

    private boolean machineMatched(Cd15MachineInfo machine, MachineFilterContext context) {
        return this.machineMatchedExceptWidth(machine, context)
                && this.widthMatched(machine, context.effectiveWidth);
    }

    private boolean machineMatchedExceptWidth(Cd15MachineInfo machine, MachineFilterContext context) {
        return machine != null
                && ApsConstant.APS_STRING_1.equals(machine.getStatus())
                && this.boundMatched(machine, context)
                && this.openShiftMatched(machine.getOpenMachineClass(), context.shiftCode)
                && !context.prohibitedMachineCodes.contains(machine.getMachineCode())
                && !this.maintenanceOverlapped(machine, context)
                && this.angleWidthMatched(context);
    }

    private boolean boundMatched(Cd15MachineInfo machine, MachineFilterContext context) {
        return !context.hasRollBinding || context.boundMachineCodes.contains(machine.getMachineCode());
    }

    private boolean preferred(Cd15MachineInfo machine, MachineFilterContext context) {
        return context.preferredMachineCodes.contains(machine.getMachineCode());
    }

    private boolean angleWidthMatched(MachineFilterContext context) {
        if (!StringUtils.hasText(context.cuttingAngle)) {
            return false;
        }
        BigDecimal maxWidth = context.angleWidthMaxByAngle.get(context.cuttingAngle);
        return maxWidth != null && maxWidth.signum() > 0
                && context.effectiveWidth != null
                && context.effectiveWidth.compareTo(maxWidth) <= 0;
    }

    /**
     * 机台上下限为空或为 0 时视为未限制。
     */
    private boolean widthMatched(Cd15MachineInfo machine, BigDecimal effectiveWidth) {
        if (machine == null || effectiveWidth == null || effectiveWidth.signum() <= 0) {
            return false;
        }
        BigDecimal min = this.toBigDecimal(machine.getClothWidthMin());
        BigDecimal max = this.toBigDecimal(machine.getClothWidthMax());
        return (min == null || BigDecimal.ZERO.compareTo(min) == 0 || effectiveWidth.compareTo(min) >= 0)
                && (max == null || BigDecimal.ZERO.compareTo(max) == 0 || effectiveWidth.compareTo(max) <= 0);
    }

    private MachineFilterContext context(Cd15AutoScheduleInput input,
                                         String bigRollCode,
                                         String cuttingAngle,
                                         BigDecimal effectiveWidth,
                                         int classIndex,
                                         List<String> steelStripCodes) {
        String shiftCode = this.classIndexToShiftCode(classIndex);
        List<String> normalizedSteelStripCodes = this.safe(steelStripCodes).stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toList());
        List<Cd15MachineRollMapping> rollMappings = this.safe(input == null ? null : input.getMachineRollMappings());
        Set<String> boundMachineCodes = rollMappings.stream()
                .filter(item -> Objects.equals(this.trim(item.getBigRollCode()), this.trim(bigRollCode)))
                .filter(item -> this.shiftMatched(item.getShiftCode(), shiftCode))
                .map(Cd15MachineRollMapping::getMachineCode)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toSet());
        List<Cd15SpecifyMachine> restrictions = this.safe(input == null ? null : input.getSpecifyMachines()).stream()
                .filter(item -> normalizedSteelStripCodes.contains(this.trim(item.getSteelStripCode())))
                .collect(Collectors.toList());
        Set<String> prohibitedMachineCodes = restrictions.stream()
                .filter(item -> ApsConstant.APS_STRING_1.equals(item.getJobType()))
                .map(Cd15SpecifyMachine::getMachineCode)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toSet());
        Set<String> preferredMachineCodes = restrictions.stream()
                .filter(item -> ApsConstant.APS_STRING_0.equals(item.getJobType()))
                .map(Cd15SpecifyMachine::getMachineCode)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toSet());
        LocalDate shiftDate = this.shiftDate(input, classIndex);
        LocalDateTime shiftStart = this.shiftStart(shiftDate, shiftCode);
        LocalDateTime shiftEnd = this.shiftEnd(shiftDate, shiftCode);
        return new MachineFilterContext(bigRollCode, cuttingAngle, effectiveWidth, shiftCode,
                input == null || input.getAngleWidthMaxByAngle() == null
                        ? Collections.emptyMap() : input.getAngleWidthMaxByAngle(),
                !boundMachineCodes.isEmpty(), boundMachineCodes, prohibitedMachineCodes, preferredMachineCodes,
                this.safe(input == null ? null : input.getMaintenancePlans()), shiftStart, shiftEnd);
    }

    private LocalDate shiftDate(Cd15AutoScheduleInput input, int classIndex) {
        Date baseDate = input == null ? null : input.getScheduleDate();
        LocalDate scheduleDate = this.toLocalDate(baseDate);
        if (scheduleDate == null) {
            return null;
        }
        if (classIndex <= 1) {
            return scheduleDate.minusDays(1);
        }
        return scheduleDate.plusDays((classIndex - 2) / 3);
    }

    private String classIndexToShiftCode(int classIndex) {
        return Cd15ShiftDisplayHelper.classIndexToShiftCode(classIndex);
    }

    private LocalDateTime shiftStart(LocalDate shiftDate, String shiftCode) {
        if (shiftDate == null) {
            return null;
        }
        ThreeShiftEnum shift = ThreeShiftEnum.getByCode(shiftCode);
        if (shift == null) {
            return null;
        }
        LocalDate startDate = shift.isCrossDay() ? shiftDate.minusDays(1) : shiftDate;
        return LocalDateTime.of(startDate, shift.getStartTime());
    }

    private LocalDateTime shiftEnd(LocalDate shiftDate, String shiftCode) {
        if (shiftDate == null) {
            return null;
        }
        ThreeShiftEnum shift = ThreeShiftEnum.getByCode(shiftCode);
        if (shift == null) {
            return null;
        }
        return LocalDateTime.of(shiftDate, shift.getEndTime());
    }

    private boolean openShiftMatched(String openMachineClass, String shiftCode) {
        return StringUtils.hasText(openMachineClass) && StringUtils.hasText(shiftCode)
                && Arrays.stream(openMachineClass.split(","))
                .map(String::trim)
                .anyMatch(shiftCode::equals);
    }

    private boolean shiftMatched(String configShiftCodes, String shiftCode) {
        return !StringUtils.hasText(configShiftCodes) || Arrays.stream(configShiftCodes.split(","))
                .map(String::trim)
                .anyMatch(shiftCode::equals);
    }

    private boolean maintenanceOverlapped(Cd15MachineInfo machine, MachineFilterContext context) {
        if (context.shiftStart == null || context.shiftEnd == null) {
            return false;
        }
        return context.maintenancePlans.stream()
                .filter(item -> Objects.equals(this.trim(item.getMachineCode()), this.trim(machine.getMachineCode())))
                .anyMatch(item -> this.overlaps(item, context.shiftStart, context.shiftEnd));
    }

    private boolean overlaps(Cd15MachineMaintenancePlan plan,
                             LocalDateTime shiftStart,
                             LocalDateTime shiftEnd) {
        LocalDateTime downtimeStart = this.toLocalDateTime(plan.getDowntimeStartTime());
        LocalDateTime downtimeEnd = this.toLocalDateTime(plan.getDowntimeEndTime());
        return downtimeStart != null && downtimeEnd != null
                && downtimeStart.isBefore(shiftEnd) && downtimeEnd.isAfter(shiftStart);
    }

    private LocalDate toLocalDate(Date value) {
        LocalDateTime dateTime = this.toLocalDateTime(value);
        return dateTime == null ? null : dateTime.toLocalDate();
    }

    private LocalDateTime toLocalDateTime(Date value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.sql.Date && !(value instanceof Timestamp)) {
            return ((java.sql.Date) value).toLocalDate().atStartOfDay();
        }
        return LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault());
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private static class MachineFilterContext {
        private final String bigRollCode;
        private final String cuttingAngle;
        private final BigDecimal effectiveWidth;
        private final String shiftCode;
        private final Map<String, BigDecimal> angleWidthMaxByAngle;
        private final boolean hasRollBinding;
        private final Set<String> boundMachineCodes;
        private final Set<String> prohibitedMachineCodes;
        private final Set<String> preferredMachineCodes;
        private final List<Cd15MachineMaintenancePlan> maintenancePlans;
        private final LocalDateTime shiftStart;
        private final LocalDateTime shiftEnd;

        private MachineFilterContext(String bigRollCode,
                                     String cuttingAngle,
                                     BigDecimal effectiveWidth,
                                     String shiftCode,
                                     Map<String, BigDecimal> angleWidthMaxByAngle,
                                     boolean hasRollBinding,
                                     Set<String> boundMachineCodes,
                                     Set<String> prohibitedMachineCodes,
                                     Set<String> preferredMachineCodes,
                                     List<Cd15MachineMaintenancePlan> maintenancePlans,
                                     LocalDateTime shiftStart,
                                     LocalDateTime shiftEnd) {
            this.bigRollCode = bigRollCode;
            this.cuttingAngle = cuttingAngle;
            this.effectiveWidth = effectiveWidth;
            this.shiftCode = shiftCode;
            this.angleWidthMaxByAngle = angleWidthMaxByAngle;
            this.hasRollBinding = hasRollBinding;
            this.boundMachineCodes = boundMachineCodes;
            this.prohibitedMachineCodes = prohibitedMachineCodes;
            this.preferredMachineCodes = preferredMachineCodes;
            this.maintenancePlans = maintenancePlans;
            this.shiftStart = shiftStart;
            this.shiftEnd = shiftEnd;
        }
    }
}