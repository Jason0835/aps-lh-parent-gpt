package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.enums.SkuScheduleSourceTypeEnum;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 历史指定试制量试的单控侧别承接判断，物理机台名额与侧别完成状态分别核对。
 *
 * @author APS
 * @date 2026/10/15
 */
public final class PreviousAlternateTrialSideAdmissionPolicy {

    /**
     * 工具类不允许实例化。
     */
    private PreviousAlternateTrialSideAdmissionPolicy() {
    }

    /**
     * 判断当前指定侧是否可继续使用已落实物理机台的另一侧承接共享余量。
     * 仅返回侧别机会，不修改目标台数、数量账本或资源；后续准入和资源校验继续执行。
     * 完成状态从仍在结果列表中的正量历史关联推导，随现有尝试快照一起恢复。
     *
     * @param context 当前预演或正式提交的排程上下文
     * @param sku 无窗口日计划的历史指定试制量试专用需求
     * @return 当前侧未提交、配对侧已按有效历史计划提交且共享余额为正时返回 true
     */
    public static boolean canCompleteSpecifiedSide(LhScheduleContext context, SkuScheduleDTO sku) {
        if (Objects.isNull(context) || Objects.isNull(sku)
                || !StringUtils.equals(SkuScheduleSourceTypeEnum.PREVIOUS_ALTERNATE_TRIAL.getCode(),
                        sku.getSourceType())
                || !PendingSkuUnscheduledRule.isTrialOrMassTrialSku(sku)) {
            return false;
        }
        PreviousAlternatePlanReleaseEvent event = context.getActivePreviousAlternateEvent();
        if (Objects.isNull(event) || !context.isPreviousAlternateAction(sku, event.getMachineCode())
                || !context.isEligiblePreviousAlternateCombination(sku, event.getPlan(), event.getMachineCode())
                || !LhSingleControlMachineUtil.isConfiguredSingleControlMachine(context, event.getMachineCode())) {
            return false;
        }
        String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(sku.getMaterialCode(), sku.getProductStatus());
        Integer remainingQty = context.getSkuProductionRemainingQtyMap().get(skuKey);
        if (Objects.isNull(remainingQty) || remainingQty <= 0) {
            return false;
        }
        String machineCode = event.getMachineCode();
        // 当前侧已有真实产量时不能重复开模，跨日原机续排仍由既有绑定处理。
        boolean sideScheduled = context.getScheduleResultList().stream()
                .filter(result -> matchesPositiveSkuResult(context, event.getPlan(), result))
                .anyMatch(result -> StringUtils.equals(machineCode, result.getLhMachineCode()));
        if (sideScheduled) {
            return false;
        }
        String pairMachineCode = LhSingleControlMachineUtil.resolvePairMachineCode(machineCode);
        // 只认配对侧的有效指定成功事实，普通结果、另一物理机和回滚关联都不能提供补侧资格。
        return context.getPreviousAlternateResultPlanMap().entrySet().stream()
                .anyMatch(entry -> context.getScheduleResultList().contains(entry.getKey())
                        && matchesPositiveSkuResult(context, event.getPlan(), entry.getKey())
                        && StringUtils.equals(pairMachineCode, entry.getKey().getLhMachineCode())
                        && context.isEligiblePreviousAlternateCombination(sku, entry.getValue(), pairMachineCode));
    }

    /**
     * 核对结果的物料状态与真实产量，零量预演不得作为侧别完成证据。
     *
     * @param context 排程上下文
     * @param plan 当前指定计划
     * @param result 已生成结果
     * @return 结果属于当前物料状态且具有正计划量时返回 true
     */
    private static boolean matchesPositiveSkuResult(LhScheduleContext context,
            LhMouldChangePlan plan, LhScheduleResult result) {
        return Objects.nonNull(result)
                && context.matchesPreviousAlternateAfterMaterial(
                        plan, result.getMaterialCode(), result.getProductStatus())
                && ShiftFieldUtil.resolveScheduledQty(result) > 0;
    }
}
