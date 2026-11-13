package com.zlt.aps.common.engine.schedule.engine;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * TM/TC新行公共工作区策略，逐机规则及挂链仍由本模块服务执行。
 *
 * @param <C> 领域上下文
 * @param <T> 领域任务
 */
public final class ScheduleExtraCreateDomainPolicy<C extends ScheduleContextModel<T, ?, ?, ?, ?>,
        T extends ScheduleTaskDraftModel> implements ScheduleExtraCreatePolicy<C, T> {
    private final String enabledKey;
    private final Function<C, C> copier;
    private final BiConsumer<C, C> publisher;
    private final BiFunction<C, T, List<ScheduleExtraCreateCandidateEvaluation<T>>> evaluator;
    private final BiPredicate<C, ScheduleExtraCreateCandidateEvaluation<T>> appender;
    private final BiConsumer<C, T> skipRecorder;
    private final BiConsumer<C, List<ScheduleExtraCreateSkipReason>> skipDetailRecorder;

    /**
     * @param enabledKey         本模块已冻结的工厂开关
     * @param copier             领域工作区复制器
     * @param publisher          已验证工作区发布器
     * @param evaluator          逐机隔离试算器
     * @param appender           最新状态复核及即时挂链器
     * @param skipRecorder       本模块国际化拒绝日志
     * @param skipDetailRecorder 本模块逐机未建行原因日志
     */
    public ScheduleExtraCreateDomainPolicy(String enabledKey, Function<C, C> copier,
                                           BiConsumer<C, C> publisher,
                                           BiFunction<C, T, List<ScheduleExtraCreateCandidateEvaluation<T>>> evaluator,
                                           BiPredicate<C, ScheduleExtraCreateCandidateEvaluation<T>> appender,
                                           BiConsumer<C, T> skipRecorder,
                                           BiConsumer<C, List<ScheduleExtraCreateSkipReason>> skipDetailRecorder) {
        this.enabledKey = enabledKey;
        this.copier = copier;
        this.publisher = publisher;
        this.evaluator = evaluator;
        this.appender = appender;
        this.skipRecorder = skipRecorder;
        this.skipDetailRecorder = skipDetailRecorder;
    }

    /**
     * @param context 本批上下文 @return 两阶段及本工厂新行开关是否同时启用
     */
    @Override
    public boolean isEnabled(C context) {
        ScheduleParamValueModel parameter = context.getParamMap().get(this.enabledKey);
        return context.isTwoPhaseEnabled() && parameter != null && "1".equals(parameter.getEffectiveValue());
    }

    /**
     * @param context 上下文 @param shiftOrder 班次；记录关闭状态供批次解释核对
     */
    @Override
    public void disabled(C context, int shiftOrder) {
        context.getQualitySummary().put("extraCreateDisabledShift" + shiftOrder, this.enabledKey);
    }

    /**
     * @param context 原上下文 @param shiftOrder 班次 @return 独立工作区
     */
    @Override
    public C openWorkspace(C context, int shiftOrder) {
        this.assertActive(context, shiftOrder);
        return this.copier.apply(context);
    }

    /**
     * 只读检查零基础任务是否存在正备库请求，正请求包括需要记录超窗拒绝的任务。
     *
     * @param context    主上下文
     * @param shiftOrder 当前班次
     * @return 存在正请求时需要创建工作区
     */
    @Override
    public boolean requiresWorkspace(C context, int shiftOrder) {
        return context.getTaskDraftList().stream()
                .filter(task -> this.isPreliminaryCandidate(context, task, shiftOrder))
                .anyMatch(task -> ScheduleExtraCreateWindowSupport.resolveRequest(context, task).signum() > 0);
    }

    /**
     * 复用预检和正式候选的静态条件，保证两条路径的任务范围一致。
     *
     * @param context    排程上下文
     * @param task       候选任务
     * @param shiftOrder 当前班次
     * @return 是否符合净请求计算前的条件
     */
    private boolean isPreliminaryCandidate(C context, T task, int shiftOrder) {
        return task != null && Objects.equals(task.getShiftOrder(), shiftOrder)
                && !Boolean.TRUE.equals(task.getSourceExplainTask())
                && task.getSettlementMode() == ScheduleSettlementMode.NORMAL
                && task.getExtraPolicy() == ScheduleExtraPolicy.ALLOW
                && BigDecimalUtils.valueOf(task.getPlanQty()).signum() == 0
                && BigDecimalUtils.valueOf(task.getBaseRequestQty()).signum() == 0
                && BigDecimalUtils.valueOf(task.getBaseRemaining()).signum() == 0
                && context.getPlanTaskGroupMap().containsKey(task.getPlanGroupKey());
    }

    /**
     * @param workspace 工作区 @param shiftOrder 当前班 @return 零基础、正备库、有来源的普通候选
     */
    @Override
    public List<T> candidates(C workspace, int shiftOrder) {
        return workspace.getTaskDraftList().stream()
                .filter(task -> this.isPreliminaryCandidate(workspace, task, shiftOrder))
                // 普通库存覆盖只抑制普通生产；独立新行按批准窗口重建净请求，不能读取其已清零的目标。
                .filter(task -> this.rebuildCandidateRequest(workspace, task))
                .collect(Collectors.toList());
    }

    /**
     * 为零基础候选恢复独立备库请求，普通覆盖标志及基础阶段历史证据保持原值。
     *
     * @param workspace 工作区 @param task 普通零基础候选
     * @return 批准窗口有正净缺口且在纯备库支持范围内时返回true
     */
    private boolean rebuildCandidateRequest(C workspace, T task) {
        BigDecimal request = ScheduleExtraCreateWindowSupport.resolveRequest(workspace, task);
        if (request.signum() <= 0) {
            return false;
        }
        // 仅限制真正需要新建纯备库行的任务，超范围只拒绝本候选，不使普通基础排程或整个批次失败。
        if (!ScheduleExtraCreateWindowSupport.isApprovedWindowSupported(task)) {
            this.skip(workspace, task, "EXTRA_CREATE_COVERAGE_RANGE_EXCEEDED");
            return false;
        }
        workspace.getPlanTaskGroupMap().get(task.getPlanGroupKey()).setSourceWeightMap(
                ScheduleExtraCreateWindowSupport.requestSourceWeights(workspace, task));
        task.setTargetRequestQty(request);
        return true;
    }

    /**
     * @param task 候选 @return 物料和工艺稳定键
     */
    @Override
    public String candidateKey(T task) {
        return String.join("|", Objects.toString(task.getProcessCode(), ""),
                Objects.toString(task.getGlueCode(), ""), Objects.toString(task.getMouthPlateCode(), ""));
    }

    /**
     * @param workspace 工作区 @param task 候选 @return 本班同物料是否已有正节点，禁止A-B-A合并
     */
    @Override
    public boolean hasExistingRow(C workspace, T task) {
        return workspace.getTaskChainGroup().values().stream().flatMap(chain -> chain.toList().stream())
                .anyMatch(node -> Objects.equals(task.getShiftOrder(), node.getShiftOrder())
                        && node.getTask() != null
                        && Objects.equals(task.getProcessCode(), node.getTask().getProcessCode())
                        && BigDecimalUtils.valueOf(node.getPlanQty()).signum() > 0);
    }

    /**
     * @param workspace 工作区 @param task 候选 @return 本模块逐机试算证明
     */
    @Override
    public List<ScheduleExtraCreateCandidateEvaluation<T>> evaluate(C workspace, T task) {
        return this.evaluator.apply(workspace, task);
    }

    /**
     * @param workspace 最新工作区 @param evaluation 旧证明 @return 最新试算并即时挂链成功
     */
    @Override
    public boolean recheckAndAppend(C workspace, ScheduleExtraCreateCandidateEvaluation<T> evaluation) {
        return !this.hasExistingRow(workspace, evaluation.getTask()) && this.appender.test(workspace, evaluation);
    }

    /**
     * @param workspace 工作区 @param task 候选 @param reasonCode 稳定拒绝码
     */
    @Override
    public void skip(C workspace, T task, String reasonCode) {
        T current = workspace.getTaskDraftList().stream()
                .filter(value -> Objects.equals(value.getBusinessKey(), task.getBusinessKey())).findFirst().orElse(task);
        new ScheduleTwoPhaseQuantityCalculator().addReason(current, reasonCode);
        this.skipRecorder.accept(workspace, current);
    }

    /**
     * 回写逐机未建行原因：原因码写回任务供解释追溯，逐机明细交给模块日志输出。
     *
     * <p>工作区最终会整体发布，因此只写工作区即可随发布进入主上下文，
     * 不会因为本轮没有任何建行成功而丢失原因。</p>
     *
     * @param workspace  工作区
     * @param shiftOrder 当前班次
     * @param skipped    未建行的候选及机台维度原因
     */
    @Override
    public void recordSkipped(C workspace, int shiftOrder, List<ScheduleExtraCreateSkipReason> skipped) {
        if (workspace == null || skipped == null || skipped.isEmpty()) {
            return;
        }
        Map<String, List<ScheduleExtraCreateSkipReason>> reasonByBusinessKey = skipped.stream()
                .filter(Objects::nonNull)
                .filter(value -> value.getBusinessKey() != null)
                .collect(Collectors.groupingBy(ScheduleExtraCreateSkipReason::getBusinessKey,
                        LinkedHashMap::new, Collectors.toList()));
        if (reasonByBusinessKey.isEmpty()) {
            return;
        }
        ScheduleTwoPhaseQuantityCalculator quantities = new ScheduleTwoPhaseQuantityCalculator();
        workspace.getTaskDraftList().stream().filter(Objects::nonNull).forEach(task -> {
            List<ScheduleExtraCreateSkipReason> reasonList = reasonByBusinessKey.get(task.getBusinessKey());
            if (reasonList == null || reasonList.isEmpty()) {
                return;
            }
            reasonList.stream().map(ScheduleExtraCreateSkipReason::getReasonCode).filter(Objects::nonNull)
                    .distinct().forEach(reasonCode -> quantities.addReason(task, reasonCode));
        });
        this.skipDetailRecorder.accept(workspace, skipped);
    }

    /**
     * @param workspace 工作区 @param shiftOrder 班次 @throws CancellationException 线程取消时拒绝发布
     */
    @Override
    public void assertActive(C workspace, int shiftOrder) {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException(I18nUtil.getMessage("ui.schedule.extraCreate.cancelled"));
        }
    }

    /**
     * 分配阶段核对备库行结构、数量和来源；延迟模式的时间完整性由统一终算后检查。
     *
     * @param workspace 工作区 @throws IllegalStateException 新行节点、来源或分量不守恒时阻止发布
     */
    @Override
    public void validate(C workspace) {
        if (!new ScheduleTwoPhaseInvariant().validAssignments(workspace.getTaskDraftList())) {
            throw new IllegalStateException(I18nUtil.getMessage("ui.schedule.extraCreate.invalidWorkspace"));
        }
        workspace.getTaskDraftList().stream().filter(Objects::nonNull)
                .filter(task -> Boolean.TRUE.equals(task.getExtraCreateTask()))
                .filter(task -> task.getQuantityStage() == ScheduleQuantityStage.ASSIGNED).forEach(task -> {
                    ScheduleTaskNode<T> node = workspace.getTaskNode(task.getBusinessKey());
                    SchedulePlanTaskGroup<T> group = workspace.getPlanTaskGroupMap().get(task.getPlanGroupKey());
                    if (node == null || node.getOwnerList() == null || node.getTask() != task
                            || !Objects.equals(task.getShiftOrder(), node.getShiftOrder())
                            || node.getPlanQty().compareTo(task.getPlanQty()) != 0
                            || (!workspace.isChainTimeDeferred()
                            && (node.getStartTime() == null || node.getEndTime() == null))
                            || group == null || group.getSourceTaskList().isEmpty()
                            || group.getSourceWeightMap().isEmpty()
                            || group.getSourceWeightMap().values().stream().filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add).signum() <= 0) {
                        throw new IllegalStateException(I18nUtil.getMessage("ui.schedule.extraCreate.invalidWorkspace"));
                    }
                });
    }

    /**
     * @param context 主上下文 @param workspace 已校验工作区；不在此关账
     */
    @Override
    public void publish(C context, C workspace) {
        this.publisher.accept(context, workspace);
    }

    /**
     * 未取得完整恢复证明时让原失败链处理，不吞异常、不发布失败副本。
     *
     * @param context 主上下文 @param workspace 失败副本 @param failure 原异常
     * @return false，保留原异常
     */
    @Override
    public boolean discardAndVerify(C context, C workspace, RuntimeException failure) {
        return false;
    }
}
