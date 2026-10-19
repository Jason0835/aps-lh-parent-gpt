package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 为精确历史指定试制量试承接及其原机续排统一采用正规时间轴、首检和产能规则。
 *
 * @author APS
 * @date 2026/10/15
 */
public final class PreviousAlternateNormalProductionPolicy {

    /**
     * 禁止实例化规则工具。
     */
    private PreviousAlternateNormalProductionPolicy() {
    }

    /**
     * 判断当前计算是否属于有效指定组合，或属于已正式承接的当前原机绑定。
     * 只在当前执行作用域生效，不能因同物料曾经指定而放行普通候选。
     *
     * @param context 当前排程上下文
     * @param sku 当前计算的来源对象
     * @return 当前组合采用正规承接规则时返回true
     */
    public static boolean usesNormalRules(LhScheduleContext context, SkuScheduleDTO sku) {
        if (Objects.isNull(context) || !PendingSkuUnscheduledRule.isTrialOrMassTrialSku(sku)) {
            return false;
        }
        PreviousAlternatePlanReleaseEvent event = context.getActivePreviousAlternateEvent();
        return (Objects.nonNull(event) && StringUtils.isNotBlank(event.getMachineCode())
                && context.isEligiblePreviousAlternateCombination(
                sku, event.getPlan(), event.getMachineCode())) || hasHistoricalBinding(context, sku);
    }

    /**
     * 校验当前绑定仍具有有效历史来源和正量结果，精确侧别及来源对象必须一致。
     *
     * @param context 当前结果及历史关系
     * @param sku 当前计算的来源对象
     * @return 当前原机绑定确由有效历史承接形成时返回true
     */
    private static boolean hasHistoricalBinding(LhScheduleContext context, SkuScheduleDTO sku) {
        ActiveMachineBinding binding = context.getActivePreviousAlternateProductionBinding();
        if (Objects.isNull(binding) || binding.getSku() != sku) {
            return false;
        }
        LhScheduleResult result = binding.getScheduleResult();
        LhMouldChangePlan plan = context.getPreviousAlternateResultPlanMap().get(result);
        if (Objects.isNull(plan) || StringUtils.isBlank(plan.getLhMachineCode())
                || StringUtils.isBlank(binding.getMachineCode())
                || !context.isEligiblePreviousAlternateCombination(
                sku, plan, plan.getLhMachineCode())
                || !context.getScheduleResultList().stream().anyMatch(committed -> committed == result)
                || ShiftFieldUtil.resolveScheduledQty(result) <= 0
                || !StringUtils.equals(result.getLhMachineCode(), binding.getMachineCode())
                || !context.matchesPreviousAlternateAfterMaterial(plan, result.getMaterialCode(), result.getProductStatus())) {
            return false;
        }
        String specifiedMachine = plan.getLhMachineCode();
        return StringUtils.equals(specifiedMachine, binding.getMachineCode())
                || (StringUtils.equals(specifiedMachine,
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(specifiedMachine))
                && StringUtils.equals(specifiedMachine,
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(binding.getMachineCode())));
    }

    /**
     * 建立当前原机绑定的计算作用域，正常退出、continue或异常均恢复外层状态。
     *
     * @param context 当前排程上下文
     * @param binding 当前实际执行的绑定
     * @return 自动恢复外层绑定的作用域
     */
    public static BindingScope openBindingScope(LhScheduleContext context, ActiveMachineBinding binding) {
        return new BindingScope(context, binding);
    }

    /**
     * 绑定级规则作用域，允许嵌套计算并在退出时还原原对象。
     *
     * @author APS
     * @date 2026/10/15
     */
    public static final class BindingScope implements AutoCloseable {

        /**
         * 本次计算上下文。
         */
        private final LhScheduleContext context;

        /**
         * 外层绑定，不能在退出时直接清空而破坏嵌套计算。
         */
        private final ActiveMachineBinding previousBinding;

        /**
         * 保存外层绑定并切换到当前绑定。
         *
         * @param context 当前上下文
         * @param binding 当前计算绑定
         */
        private BindingScope(LhScheduleContext context, ActiveMachineBinding binding) {
            this.context = context;
            this.previousBinding = context.getActivePreviousAlternateProductionBinding();
            context.setActivePreviousAlternateProductionBinding(binding);
        }

        /**
         * 恢复进入本作用域之前的绑定，不修改任何数量或资源账本。
         */
        @Override
        public void close() {
            this.context.setActivePreviousAlternateProductionBinding(this.previousBinding);
        }
    }
}
