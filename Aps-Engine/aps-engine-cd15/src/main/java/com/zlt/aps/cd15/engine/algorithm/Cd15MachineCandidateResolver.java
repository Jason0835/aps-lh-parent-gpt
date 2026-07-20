package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15MachineCandidate;
import com.zlt.aps.cd15.engine.model.Cd15MachineResource;
import com.zlt.aps.cd15.engine.model.Cd15MachineRestriction;
import com.zlt.aps.cd15.engine.model.Cd15MachineRollBinding;
import com.zlt.aps.cd15.engine.model.Cd15MachineCandidateResolution;
import com.zlt.aps.common.core.constant.ApsConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 候选机台硬约束过滤器。
 */
@Slf4j
@Component
public class Cd15MachineCandidateResolver {

    /**
     * 按大卷绑定、机台状态、开机班次、不可作业和检修约束生成候选机台。
     *
     * @param steelStripCode 钢带代码
     * @param bigRollCode 大卷代码，对应施工CORD_SPEC
     * @param shiftCode 当前班次编码
     * @param shiftStart 班次开始时间
     * @param shiftEnd 班次结束时间
     * @param machines 机台资源
     * @param bindings 大卷机台绑定
     * @param restrictions 定点及不可作业配置
     * @param machinePriority 参数机台优先顺序
     * @return 已过滤并排序的候选机台
     */
    public List<Cd15MachineCandidate> resolve(String steelStripCode,
                                              String bigRollCode,
                                              String shiftCode,
                                              LocalDateTime shiftStart,
                                              LocalDateTime shiftEnd,
                                              List<Cd15MachineResource> machines,
                                              List<Cd15MachineRollBinding> bindings,
                                              List<Cd15MachineRestriction> restrictions,
                                              List<String> machinePriority) {
        return resolveDetailed(steelStripCode, bigRollCode, shiftCode, shiftStart, shiftEnd,
                machines, bindings, restrictions, machinePriority).getCandidates();
    }

    public List<Cd15MachineCandidate> resolve(String steelStripCode,
                                              String bigRollCode,
                                              BigDecimal craftWidth,
                                              String shiftCode,
                                              LocalDateTime shiftStart,
                                              LocalDateTime shiftEnd,
                                              List<Cd15MachineResource> machines,
                                              List<Cd15MachineRollBinding> bindings,
                                              List<Cd15MachineRestriction> restrictions,
                                              List<String> machinePriority) {
        return resolveDetailed(steelStripCode, bigRollCode, craftWidth, shiftCode, shiftStart, shiftEnd,
                machines, bindings, restrictions, machinePriority).getCandidates();
    }

    /** 生成候选机台并区分无绑定与绑定机台全部不可作业。 */
    public Cd15MachineCandidateResolution resolveDetailed(String steelStripCode,
                                              String bigRollCode,
                                              String shiftCode,
                                              LocalDateTime shiftStart,
                                              LocalDateTime shiftEnd,
                                              List<Cd15MachineResource> machines,
                                              List<Cd15MachineRollBinding> bindings,
                                              List<Cd15MachineRestriction> restrictions,
                                              List<String> machinePriority) {
        return resolveDetailed(steelStripCode, bigRollCode, null, shiftCode, shiftStart, shiftEnd,
                machines, bindings, restrictions, machinePriority);
    }

    /** 兼容不需要角度宽度约束的独立调用。 */
    public Cd15MachineCandidateResolution resolveDetailed(String steelStripCode,
                                              String bigRollCode,
                                              BigDecimal craftWidth,
                                              String shiftCode,
                                              LocalDateTime shiftStart,
                                              LocalDateTime shiftEnd,
                                              List<Cd15MachineResource> machines,
                                              List<Cd15MachineRollBinding> bindings,
                                              List<Cd15MachineRestriction> restrictions,
                                              List<String> machinePriority) {
        return this.resolveDetailed(steelStripCode, bigRollCode, craftWidth,
                null, Collections.emptyMap(), shiftCode, shiftStart, shiftEnd,
                machines, bindings, restrictions, machinePriority);
    }

    /** 生成候选机台并执行角度最大宽度约束。 */
    public Cd15MachineCandidateResolution resolveDetailed(String steelStripCode,
                                              String bigRollCode,
                                              BigDecimal craftWidth,
                                              String cuttingAngle,
                                              Map<String, BigDecimal> angleWidthMaxByAngle,
                                              String shiftCode,
                                              LocalDateTime shiftStart,
                                              LocalDateTime shiftEnd,
                                              List<Cd15MachineResource> machines,
                                              List<Cd15MachineRollBinding> bindings,
                                              List<Cd15MachineRestriction> restrictions,
                                              List<String> machinePriority) {
        List<Cd15MachineRollBinding> bigRollBindings = safe(bindings).stream()
                .filter(item -> bigRollCode != null && bigRollCode.equals(item.getBigRollCode()))
                .collect(Collectors.toList());
        Set<String> boundMachines = bigRollBindings.stream()
                .filter(item -> this.shiftMatched(item.getShiftCode(), shiftCode))
                .map(Cd15MachineRollBinding::getMachineCode)
                .collect(Collectors.toSet());
        // 大卷维护过绑定时必须命中当前班次；只维护其它班次不能放开到全部机台。
        boolean hasBinding = !bigRollBindings.isEmpty();
        Set<String> prohibited = safe(restrictions).stream()
                .filter(item -> steelStripCode != null && steelStripCode.equals(item.getSteelStripCode()))
                .filter(item -> ApsConstant.APS_STRING_1.equals(item.getJobType()))
                .map(Cd15MachineRestriction::getMachineCode)
                .collect(Collectors.toSet());
        Set<String> preferred = safe(restrictions).stream()
                .filter(item -> steelStripCode != null && steelStripCode.equals(item.getSteelStripCode()))
                .filter(item -> ApsConstant.APS_STRING_0.equals(item.getJobType()))
                .map(Cd15MachineRestriction::getMachineCode)
                .collect(Collectors.toSet());

        List<String> priority = machinePriority == null ? Collections.emptyList() : machinePriority;
        boolean angleConfigured = !StringUtils.hasText(cuttingAngle)
                || angleWidthMaxByAngle != null
                && angleWidthMaxByAngle.get(cuttingAngle.trim()) != null
                && angleWidthMaxByAngle.get(cuttingAngle.trim()).signum() > 0;
        boolean angleWidthMatched = angleConfigured
                && this.angleWidthMatched(cuttingAngle, craftWidth, angleWidthMaxByAngle);
        List<Cd15MachineCandidate> candidates = safe(machines).stream()
                .filter(item -> !hasBinding || boundMachines.contains(item.getMachineCode()))
                .filter(item -> ApsConstant.APS_STRING_1.equals(item.getStatus()))
                .filter(item -> widthMatched(item, craftWidth))
                .filter(item -> angleWidthMatched)
                .filter(item -> openShiftMatched(item.getOpenMachineClass(), shiftCode))
                .filter(item -> !prohibited.contains(item.getMachineCode()))
                .map(item -> Cd15MachineCandidate.builder()
                        .machineCode(item.getMachineCode())
                        .preferredMachine(preferred.contains(item.getMachineCode()))
                        .priorityOrder(priorityIndex(priority, item.getMachineCode()))
                        .build())
                .sorted(Comparator.comparing(Cd15MachineCandidate::isPreferredMachine).reversed()
                        .thenComparingInt(Cd15MachineCandidate::getPriorityOrder)
                        .thenComparing(Cd15MachineCandidate::getMachineCode))
                .collect(Collectors.toList());
        if (candidates.isEmpty() && !safe(machines).isEmpty()) {
            for (Cd15MachineResource item : safe(machines)) {
                if (!hasBinding || boundMachines.contains(item.getMachineCode())) {
                    log.warn("[斜裁自动排程] 机台被硬约束过滤, steelStripCode={}, bigRollCode={}, machineCode={}, "
                                    + "status={}, openMachineClass={}, shiftCode={}, craftWidth={}, "
                                    + "clothWidthMin={}, clothWidthMax={}, widthMatched={}, openShiftMatched={}, "
                                    + "prohibited={}, maintenanceStart={}, maintenanceEnd={}",
                            steelStripCode, bigRollCode, item.getMachineCode(), item.getStatus(),
                            item.getOpenMachineClass(), shiftCode, craftWidth,
                            item.getClothWidthMin(), item.getClothWidthMax(),
                            widthMatched(item, craftWidth),
                            openShiftMatched(item.getOpenMachineClass(), shiftCode),
                            prohibited.contains(item.getMachineCode()),
                            item.getMaintenanceStart(), item.getMaintenanceEnd());
                }
            }
        }
        String failureReason = null;
        if (!angleConfigured) {
            failureReason = "ANGLE_WIDTH_CONFIG_MISSING";
        } else if (!angleWidthMatched) {
            failureReason = "ANGLE_WIDTH_MISMATCH";
        } else if (hasBinding && boundMachines.stream().allMatch(prohibited::contains)) {
            failureReason = "MACHINE_PROHIBITED";
        } else if (candidates.isEmpty()) {
            failureReason = resolveEmptyFailureReason(machines, hasBinding, boundMachines,
                    craftWidth, shiftCode, prohibited, shiftStart, shiftEnd);
        }
        return Cd15MachineCandidateResolution.builder().candidates(candidates)
                .boundMachineCodes(boundMachines.stream().sorted().collect(Collectors.toList()))
                .failureReason(failureReason).build();
    }

    /**
     * 候选集合为空时按硬约束过滤原因细分失败编码。
     * 若至少存在一台机台仅因宽度不匹配被排除（其它硬约束均通过），返回 WIDTH_MISMATCH；
     * 否则返回 NO_AVAILABLE_MACHINE，由上层按动态状态或产能约束兜底。
     */
    private String resolveEmptyFailureReason(List<Cd15MachineResource> machines,
                                             boolean hasBinding,
                                             Set<String> boundMachines,
                                             BigDecimal craftWidth,
                                             String shiftCode,
                                             Set<String> prohibited,
                                             LocalDateTime shiftStart,
                                             LocalDateTime shiftEnd) {
        if (craftWidth == null) {
            return "NO_AVAILABLE_MACHINE";
        }
        for (Cd15MachineResource item : safe(machines)) {
            if (hasBinding && !boundMachines.contains(item.getMachineCode())) {
                continue;
            }

            // 若有机台仅因宽度不匹配被排除，但其它硬约束均通过，则视为宽度不匹配场景。
            if (ApsConstant.APS_STRING_1.equals(item.getStatus())
                    && openShiftMatched(item.getOpenMachineClass(), shiftCode)
                    && !prohibited.contains(item.getMachineCode())
                    && !widthMatched(item, craftWidth)) {
                return "WIDTH_MISMATCH";
            }
        }
        return "NO_AVAILABLE_MACHINE";

    }

    /**
     * 斜裁宽度来自施工表TIRE_FABRIC_CRAFT1/2/3；CORD_WIDTH是大卷宽、斜裁长，不参与宽度适配。
     * 机台上下限为空时视为未限制，避免历史基础数据未维护宽度时把全部机台过滤掉。
     */
    private boolean widthMatched(Cd15MachineResource machine, BigDecimal craftWidth) {
        if (craftWidth == null || machine == null) {
            return true;
        }
        BigDecimal min = machine.getClothWidthMin();
        BigDecimal max = machine.getClothWidthMax();
        return (min == null || BigDecimal.ZERO.compareTo(min) == 0 || craftWidth.compareTo(min) >= 0)
                && (max == null || BigDecimal.ZERO.compareTo(max) == 0 || craftWidth.compareTo(max) <= 0);
    }

    /** 检查施工宽度是否不超过当前裁断角度允许的最大宽度。 */
    private boolean angleWidthMatched(String cuttingAngle,
                                      BigDecimal craftWidth,
                                      Map<String, BigDecimal> angleWidthMaxByAngle) {
        if (!StringUtils.hasText(cuttingAngle)) {
            return true;
        }
        if (angleWidthMaxByAngle == null) {
            return false;
        }
        BigDecimal maxWidth = angleWidthMaxByAngle.get(cuttingAngle.trim());
        return maxWidth != null && maxWidth.signum() > 0
                && craftWidth != null && craftWidth.signum() > 0
                && craftWidth.compareTo(maxWidth) <= 0;
    }

    /** 机台开机班次支持逗号多选存储，例如01,02,03；匹配时按完整班次编码精确比较。 */
    /** 大卷机台绑定班次支持逗号多选存储。 */
    private boolean shiftMatched(String configuredShiftCodes, String shiftCode) {
        return configuredShiftCodes != null && shiftCode != null
                && Arrays.stream(configuredShiftCodes.split(","))
                .map(String::trim)
                .anyMatch(shiftCode::equals);
    }
    private boolean openShiftMatched(String openMachineClass, String shiftCode) {
        if (openMachineClass == null || shiftCode == null) {
            return false;
        }
        return Arrays.stream(openMachineClass.split(","))
                .map(String::trim)
                .anyMatch(shiftCode::equals);
    }

    private boolean overlaps(Cd15MachineResource machine,
                             LocalDateTime shiftStart,
                             LocalDateTime shiftEnd) {
        if (machine.getMaintenanceStart() == null || machine.getMaintenanceEnd() == null) {
            return false;
        }
        return machine.getMaintenanceStart().isBefore(shiftEnd)
                && machine.getMaintenanceEnd().isAfter(shiftStart);
    }

    private int priorityIndex(List<String> priorities, String machineCode) {
        int index = priorities.indexOf(machineCode);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
