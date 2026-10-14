package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.component.LhMachineSupplyStructureRule;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.ITypeBlockProductionStrategy;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;

import java.util.Objects;

/**
 * 结构切换同结构内的机台与物料组合优先级，只判断关系，不授予资源或需求资格。
 *
 * @author APS
 * @since 2026/10/12
 */
public final class StructureSwitchPriorityPolicy {
    /**
     * 精确有效的前日指定交替组合优先。
     */
    public static final int HISTORICAL = 0;
    /**
     * 可复用现有模具换活字块的组合次优先。
     */
    public static final int TYPE_BLOCK = 1;
    /**
     * 匹配配置的K22或K21专供组合第三优先。
     */
    public static final int DEDICATED_SUPPLY = 2;
    /**
     * 普通合法组合最后参与，同级沿用SKU业务顺序。
     */
    public static final int ORDINARY = 3;

    /**
     * 禁止实例化无状态优先级策略。
     */
    private StructureSwitchPriorityPolicy() {
    }

    /**
     * 按指定交替、换活字块、配置专供及普通关系解析组合等级。
     *
     * @param context 当前资源及配置上下文
     * @param sku 待排物料与产品状态
     * @param machine 当前运行态机台
     * @param historicalPlan 调用方已精确校验且尚未承接的历史关系
     * @param typeBlockStrategy 现有换活字块只读判断入口
     * @param supplyRule 当前结构专供配置判断入口
     * @return 组合优先级，数值越小越优先
     */
    public static int resolveRank(LhScheduleContext context, SkuScheduleDTO sku, MachineScheduleDTO machine,
            LhMouldChangePlan historicalPlan, ITypeBlockProductionStrategy typeBlockStrategy,
            LhMachineSupplyStructureRule supplyRule) {
        if (Objects.nonNull(historicalPlan)) {
            return HISTORICAL;
        }
        if (typeBlockStrategy.isSpecialMaterialSubstitutionTypeBlockApplicable(context, machine, sku)) {
            return TYPE_BLOCK;
        }
        String physicalMachine = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machine.getMachineCode());
        if ((physicalMachine.startsWith("K22") || physicalMachine.startsWith("K21"))
                && supplyRule.isPreferredMachine(context, machine.getMachineCode(), sku)) {
            return DEDICATED_SUPPLY;
        }
        return ORDINARY;
    }
}
