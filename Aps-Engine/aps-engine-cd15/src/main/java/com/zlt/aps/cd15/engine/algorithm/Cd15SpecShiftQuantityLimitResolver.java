package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleParameters;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import com.zlt.aps.cd15.engine.model.Cd15ShiftResourceState;
import com.zlt.aps.cd15.engine.model.Cd15ShiftScheduleTask;
import com.zlt.aps.cd15.engine.model.Cd15SpecShiftQuantityLimit;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** 解析当前班同一钢带代码的累计计划量上限及剩余额度。 */
@Component
public class Cd15SpecShiftQuantityLimitResolver {

    /**
     * 普通班次使用SYS0601007；明确停班后恢复的实际复产班次改用SYS0601021。
     * 已排量按钢带代码汇总，不允许通过更换机台、大卷或施工层位绕过班内上限。
     */
    public Cd15SpecShiftQuantityLimit resolve(
            Cd15ShiftDescriptor shift,
            Cd15ShiftResourceState state,
            Cd15AutoScheduleParameters parameters,
            String steelStripCode) {
        if (shift == null || parameters == null
                || !StringUtils.hasText(steelStripCode)) {
            throw new IllegalArgumentException("班次、参数和钢带代码不能为空");
        }
        boolean restartStockMode = shift.isRestartStockMode();
        BigDecimal shiftLimit = restartStockMode
                ? parameters.getRestartStockThreshold()
                : parameters.getEqualShareThreshold();
        if (shiftLimit == null || shiftLimit.signum() <= 0) {
            throw new IllegalArgumentException(restartStockMode
                    ? "实际复产库存阈值必须大于0"
                    : "单规格均分及班产上限阈值必须大于0");
        }
        List<Cd15ShiftScheduleTask> tasks = state == null
                || state.getTasks() == null
                        ? Collections.emptyList() : state.getTasks();
        BigDecimal scheduledQuantity = tasks.stream()
                .filter(Objects::nonNull)
                .filter(task -> steelStripCode.equals(task.getSteelStripCode()))
                .map(Cd15ShiftScheduleTask::getPlanQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remainingQuantity = shiftLimit.subtract(
                scheduledQuantity).max(BigDecimal.ZERO);
        return Cd15SpecShiftQuantityLimit.builder()
                .shiftLimit(shiftLimit)
                .scheduledQuantity(scheduledQuantity)
                .remainingQuantity(remainingQuantity)
                .restartStockMode(restartStockMode)
                .build();
    }
}
