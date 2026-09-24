package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import lombok.Getter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 一次续作降模的有序选择结果，不保存班次或数量处置。 */
@Getter
public final class ReductionResult {
    /** 按最终优先级排列的物理机台决策。 */
    private final List<MachineReductionDecision> machines;

    /** @param machines 本轮选中的机台 */
    public ReductionResult(List<MachineReductionDecision> machines) {
        this.machines = Collections.unmodifiableList(new ArrayList<>(machines));
    }

    /** @return 选中的原始结果，维持决策顺序并去重 */
    public List<LhScheduleResult> getResults() {
        return machines.stream().flatMap(machine -> machine.getResults().stream()).distinct().collect(Collectors.toList());
    }

    /** @return 已选物理机台编码 */
    public Set<String> getMachineCodes() {
        return machines.stream().map(MachineReductionDecision::getMachineCode).collect(Collectors.toSet());
    }
}
