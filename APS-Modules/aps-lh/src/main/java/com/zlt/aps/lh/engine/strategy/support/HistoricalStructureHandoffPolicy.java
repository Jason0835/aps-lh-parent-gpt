package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.cx.entity.config.CxEmbryoLhTime;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.Objects;

/**
 * 历史指定结构承接的统一状态入口，依据真实需求、正式结果及冻结等待门禁决定保留范围。
 */
@Slf4j
public final class HistoricalStructureHandoffPolicy {
    /**
     * 工具类禁止实例化。
     */
    private HistoricalStructureHandoffPolicy() {
    }

    /**
     * 读取历史关系的当前状态，正式承接和需求耗尽优先于先前等待诊断。
     *
     * @param context 本批数量和历史关系上下文
     * @param plan 已确认有效的历史关系
     * @return 本窗口承接状态
     */
    public static HistoricalStructureHandoffState resolveState(LhScheduleContext context, LhMouldChangePlan plan) {
        if (context.getPreviousAlternateResultPlanMap().containsValue(plan)) {
            return HistoricalStructureHandoffState.COMMITTED;
        }
        String key = MonthPlanDateResolver.buildMaterialStatusKey(plan.getAfterMaterialCode(), plan.getProductStatus());
        SkuScheduleDTO sku = context.getStructureSwitchCandidateSkuMap().get(key);
        if (Objects.isNull(sku) || context.getSkuProductionRemainingQtyMap()
                .getOrDefault(key, sku.getRemainingScheduleQty()) <= 0) {
            return HistoricalStructureHandoffState.NO_DEMAND;
        }
        return context.getHistoricalStructureHandoffStateMap().getOrDefault(plan,
                HistoricalStructureHandoffState.PENDING);
    }

    /**
     * 记录精确历史组合的等待证据；只有基础差值超限才能判定整个窗口不可承接。
     * 当前班次增加偏移后的拒绝，以及缺模具、名额或换模次数等临时失败均不解除保护。
     *
     * @param context 本批候选及关系快照
     * @param source 当前结构切换供胚来源
     * @param selectionInspectionStartTime 提案冻结的准备后最早首检时间
     * @param materialCode 当前后物料编码
     * @param productStatus 当前后物料状态
     * @param machineCode 当前候选运行侧或物理机台
     * @param baseWaitExceeded 不增加班次偏移时的基础供胚差值是否超限
     */
    public static void recordWaitingAdmission(LhScheduleContext context, CxEmbryoLhTime source,
            Date selectionInspectionStartTime, String materialCode, String productStatus, String machineCode,
            boolean baseWaitExceeded) {
        // 班次九上下文浅复制了原候选索引，不得改写主窗口历史保留状态。
        if (context.isIsolatedNextShiftPlan()) {
            return;
        }
        String key = MonthPlanDateResolver.buildMaterialStatusKey(materialCode, productStatus);
        CxEmbryoLhTime frozenSource = context.getStructureSwitchCandidateSourceMap().get(key);
        if (Objects.isNull(frozenSource) || !Objects.equals(frozenSource.getId(), source.getId())) {
            return;
        }
        String physicalCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        context.getEligiblePreviousAlternatePlans().stream()
                .filter(plan -> StringUtils.equals(physicalCode,
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(plan.getLhMachineCode())))
                .filter(plan -> StringUtils.equals(plan.getLhMachineCode(), physicalCode)
                        || StringUtils.equals(plan.getLhMachineCode(), machineCode))
                .filter(plan -> context.matchesPreviousAlternateAfterMaterial(plan, materialCode, productStatus))
                .filter(plan -> !context.getPreviousAlternateResultPlanMap().containsValue(plan))
                .forEach(plan -> {
                    HistoricalStructureHandoffState next = baseWaitExceeded
                            ? HistoricalStructureHandoffState.WINDOW_INFEASIBLE
                            : HistoricalStructureHandoffState.WAITING;
                    HistoricalStructureHandoffState previous = context.getHistoricalStructureHandoffStateMap().put(plan, next);
                    if (previous != next) {
                        String detail = String.format("工厂=%s, 批次=%s, 计划ID=%s, 来源ID=%s, 机台=%s, 后物料=%s, 状态=%s, "
                                        + "承接状态=%s, 冻结首检比较起点=%s, 最早供胚=%s, 解除排他保留=%s, 预演=%s",
                                context.getFactoryCode(), context.getBatchNo(), plan.getId(), source.getId(), machineCode,
                                materialCode, productStatus, next,
                                LhScheduleTimeUtil.formatDateTime(selectionInspectionStartTime),
                                LhScheduleTimeUtil.formatDateTime(source.getEarliestLhTime()), baseWaitExceeded,
                                context.isTimedMachineOffLoadPreview());
                        log.info("历史结构承接状态变更, {}", detail);
                        PriorityTraceLogHelper.appendProcessLog(context, "历史结构承接状态变更", detail);
                    }
                });
    }
}
