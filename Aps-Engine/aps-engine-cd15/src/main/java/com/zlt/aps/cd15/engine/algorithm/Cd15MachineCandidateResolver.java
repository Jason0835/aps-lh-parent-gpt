package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.common.core.constant.ApsConstant;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * CD15 机台硬条件候选过滤。
 */
@Component
public class Cd15MachineCandidateResolver {

    public static final String WIDTH_MISMATCH = "WIDTH_MISMATCH";
    public static final String NO_AVAILABLE_MACHINE = "NO_AVAILABLE_MACHINE";

    /**
     * 当前步骤只做启用状态与 CLOTH_WIDTH_MIN/CLOTH_WIDTH_MAX 宽度过滤。
     */
    public Optional<Cd15MachineInfo> resolve(List<Cd15MachineInfo> machines, BigDecimal effectiveWidth) {
        if (machines == null || machines.isEmpty()) {
            return Optional.empty();
        }
        return machines.stream()
                .filter(machine -> this.supports(machine, effectiveWidth))
                .sorted(Comparator.comparing(Cd15MachineInfo::getMachineCode))
                .findFirst();
    }

    public boolean supports(Cd15MachineInfo machine, BigDecimal effectiveWidth) {
        if (machine == null || !ApsConstant.APS_STRING_1.equals(machine.getStatus())) {
            return false;
        }
        if (effectiveWidth == null || effectiveWidth.signum() <= 0) {
            return false;
        }
        BigDecimal min = this.toBigDecimal(machine.getClothWidthMin());
        BigDecimal max = this.toBigDecimal(machine.getClothWidthMax());
        boolean lowerOk = min == null || effectiveWidth.compareTo(min) >= 0;
        boolean upperOk = max == null || effectiveWidth.compareTo(max) <= 0;
        return lowerOk && upperOk;
    }

    public String resolveFailureReason(Cd15MachineInfo machine, BigDecimal effectiveWidth) {
        if (machine != null && ApsConstant.APS_STRING_1.equals(machine.getStatus())) {
            return WIDTH_MISMATCH;
        }
        return NO_AVAILABLE_MACHINE;
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}