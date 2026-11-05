package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.enums.UnscheduledReasonEnum;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.component.UnscheduledResultCollector;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.factory.ScheduleStrategyFactory;
import com.zlt.aps.lh.engine.strategy.impl.ContinuousProductionStrategy;
import com.zlt.aps.lh.engine.strategy.impl.NewSpecProductionStrategy;
import com.zlt.aps.lh.engine.strategy.support.StructureEndingContinuationPolicy;
import com.zlt.aps.lh.engine.strategy.support.StructureEndingContinuationState;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;

/**
 * 结构切换后的续作增机及原机回补编排，原需求与资源仍由公共内核消费。
 *
 * @author Codex
 * @date 2026/10/29
 */
@Service
public class StructureEndingContinuationService {
    /**
     * 指定历史关系及扩展承接原入口。
     */
    @Resource
    private PreviousAlternatePlanReuseService historicalReuseService;
    /**
     * 公共新增、跨日与单组合提交策略。
     */
    @Resource
    private NewSpecProductionStrategy newProductionStrategy;
    /**
     * 原机真实产能、增量扣账和结果同步能力。
     */
    @Resource
    private ContinuousProductionStrategy continuationStrategy;
    /**
     * 既有匹配、换模、首检和产能策略工厂。
     */
    @Resource
    private ScheduleStrategyFactory strategyFactory;
    /**
     * 唯一物料状态消费账本。
     */
    @Resource
    private TargetScheduleQtyResolver quantityResolver;
    /**
     * 统一未排需求归属及去重。
     */
    @Resource
    private UnscheduledResultCollector unscheduledCollector;

    /**
     * 完成所有允许的新增尝试后，逐台复核并追加原机合法后缀。
     *
     * @param context 结构切换完成后的实时上下文
     */
    public void schedule(LhScheduleContext context) {
        if (context.getStructureEndingContinuationStates().values().stream()
                .allMatch(StructureEndingContinuationState::isCompleted)) {
            return;
        }
        boolean previousScope = context.isStructureEndingContinuationScheduling();
        context.setStructureEndingContinuationScheduling(true);
        try {
            // 历史和普通资格分别保留，但对所有SKU都先完成增机，再进入原机回补。
            historicalReuseService.reuse(context);
            newProductionStrategy.scheduleStructureEndingExpansion(context,
                    strategyFactory.getMachineMatchStrategy(), strategyFactory.getMouldChangeBalanceStrategy(),
                    strategyFactory.getFirstInspectionBalanceStrategy(), strategyFactory
                            .getCapacityCalculateStrategy());
            if (context.isInterrupted()) {
                return;
            }
            for (String key : new ArrayList<>(context.getStructureEndingContinuationStates().keySet())) {
                boolean backfilled = this.backfillOriginalMachines(context, key);
                if (context.isInterrupted()) {
                    return;
                }
                StructureEndingContinuationState state = context.getStructureEndingContinuationStates().get(key);
                if (!backfilled) {
                    continuationStrategy.refreshStructureEndingContinuationFacts(context, key);
                }
                state.setCompleted(true);
                int remaining = quantityResolver.previewProductionRemainingQty(context, state.getSourceSku());
                if (remaining > 0) {
                    unscheduledCollector.addOrReplace(context, state.getSourceSku(), unscheduledCollector.buildResult(
                            context, state.getSourceSku(), remaining, UnscheduledReasonEnum.PARTIAL_UNSCHEDULED,
                            null, null));
                } else {
                    unscheduledCollector.remove(context, state.getSourceSku());
                }
                PriorityTraceLogHelper.appendProcessLog(context, "结构收尾续作三步收口",
                        String.format("工厂=%s，日期=%s，批次=%s，物料=%s，状态=%s，原机=%s，新增机=%s，"
                                        + "第一截止=%s，最终截止=%s，原机累计=%s，真实剩余=%s，规则=不预留原机，只追加合法后缀",
                                context.getFactoryCode(), context.getScheduleDate(), context.getBatchNo(),
                                state.getSourceSku().getMaterialCode(), state.getSourceSku().getProductStatus(),
                                state.getOriginalPhysicalMachines(), state.getAddedPhysicalMachines(),
                                state.getFirstDeadline(), state.getFinalDeadline(),
                                state.getOriginalCommittedQuantities(), remaining));
            }
        } finally {
            context.setStructureEndingContinuationScheduling(previousScope);
        }
    }

    /**
     * 新增全部失败也扫描全部原机，后缀统一分量并使用完整快照原子提交。
     *
     * @param context 当前资源及共享余额
     * @param key 冻结物料状态键
     * @return 已提交并刷新完整原机事实时为真
     */
    private boolean backfillOriginalMachines(LhScheduleContext context, String key) {
        StructureEndingContinuationState state = context.getStructureEndingContinuationStates().get(key);
        if (context.isInterrupted() || state.isCompleted()
                || quantityResolver.previewProductionRemainingQty(context, state.getSourceSku()) <= 0) {
            return false;
        }
        ScheduleSubstitutionAttemptSnapshot snapshot = ScheduleSubstitutionAttemptSnapshot.capture(
                context, state.getSourceSku());
        boolean committed = false;
        try {
            int quantity = continuationStrategy.appendStructureEndingOriginalMachines(context, key);
            if (!StructureEndingContinuationPolicy.withinDeadlines(context)) {
                throw new IllegalStateException("结构收尾原机回补越过已冻结生产截止");
            }
            committed = quantity > 0;
            return committed;
        } finally {
            if (!committed) {
                snapshot.restore(context);
            }
        }
    }
}
