package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;

/**
 * 历史指定增机的一次性需求；仅供精确指定动作使用，不改写普通增机目标。
 * <p>值对象在登记后不可变，候选、关联预演和整批负荷预演均由通用快照恢复映射。</p>
 */
@Getter
public final class PreviousAlternateAdditionalMachineDemand {
    /** 本批已校验的历史计划，按对象身份关联，避免同机另一指令误用。 */
    private final LhMouldChangePlan plan;
    /** 后物料及产品状态业务键。 */
    private final String skuKey;
    /** 本次指定承接允许生产的业务日起点。 */
    private final LocalDate productionDate;
    /** 已有有效承载加本指定物理机的一次性目标份数。 */
    private final int targetMachineCount;
    /** 没有下机依据、必须保留的原续作物理机台，用于业务对账。 */
    private final Set<String> retainedMachineCodes;

    /**
     * @param plan 精确历史指令
     * @param skuKey 后物料状态键
     * @param productionDate 需求生效业务日
     * @param targetMachineCount 本指定动作的目标总份数
     * @param retainedMachineCodes 保留的原续作机台
     */
    public PreviousAlternateAdditionalMachineDemand(LhMouldChangePlan plan, String skuKey,
            LocalDate productionDate, int targetMachineCount, Set<String> retainedMachineCodes) {
        this.plan = plan;
        this.skuKey = skuKey;
        this.productionDate = productionDate;
        this.targetMachineCount = targetMachineCount;
        this.retainedMachineCodes = Collections.unmodifiableSet(new LinkedHashSet<>(retainedMachineCodes));
    }
}
