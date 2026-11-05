package com.zlt.aps.common.engine.schedule.engine;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;

/**
 * 物料驱动的单轮备库新行编排。一次物料失败不能封禁机台；不回溯已成功物料。
 *
 * @param <C> 隔离上下文类型
 * @param <T> 领域任务类型
 */
public final class ScheduleExtraCreateEngine<C, T extends ScheduleTaskDraftModel> {

    /**
     * 在已有行扩增后、唯一关账前执行。领域未接入时保持默认无动作。
     *
     * @param context    主上下文
     * @param shiftOrder 当前班
     * @param policy     领域端口，null表示尚未接入
     * @throws RuntimeException 不可恢复故障、取消或恢复证明失败时向原任务失败链传播
     */
    public void create(C context, int shiftOrder, ScheduleExtraCreatePolicy<C, T> policy) {
        if (policy == null) {
            return;
        }
        if (!policy.isEnabled(context)) {
            policy.disabled(context, shiftOrder);
            return;
        }
        // 无正请求时仍执行取消检查和不变量校验，外层班末关账照常执行。
        policy.assertActive(context, shiftOrder);
        if (!policy.requiresWorkspace(context, shiftOrder)) {
            policy.assertActive(context, shiftOrder);
            policy.validate(context);
            policy.recordSkipped(context, shiftOrder, Collections.emptyList());
            return;
        }
        C workspace = policy.openWorkspace(context, shiftOrder);
        if (workspace == null || workspace == context) {
            throw new IllegalStateException("Standalone reserve requires an isolated workspace");
        }
        List<ScheduleExtraCreateSkipReason> skippedList = new ArrayList<>();
        try {
            List<T> tasks = new ArrayList<>(policy.candidates(workspace, shiftOrder));
            tasks.removeIf(Objects::isNull);
            tasks.sort(Comparator.<T, Integer>comparing(ScheduleTaskDraftModel::getExtraPrioritySnapshot,
                            Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(ScheduleTaskDraftModel::getSupplyHours, Comparator.nullsLast(BigDecimal::compareTo))
                    .thenComparing(ScheduleTaskDraftModel::getTargetRequestQty, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(ScheduleTaskDraftModel::getProcessCode, Comparator.nullsLast(String::compareTo))
                    .thenComparing(policy::candidateKey, Comparator.nullsLast(String::compareTo)));
            Set<String> visited = new HashSet<>();
            for (T task : tasks) {
                policy.assertActive(workspace, shiftOrder);
                String key = policy.candidateKey(task);
                if (key == null || key.trim().isEmpty() || !visited.add(key) || policy.hasExistingRow(workspace, task)) {
                    policy.skip(workspace, task, "EXTRA_CREATE_RESULT_PROJECTION_CONFLICT");
                    skippedList.add(ScheduleExtraCreateSkipReason.of(task.getBusinessKey(), shiftOrder, null,
                            "EXTRA_CREATE_RESULT_PROJECTION_CONFLICT", null));
                    continue;
                }
                // 保留未通过试算的机台证据，用于逐机说明“本班为什么没有建行”。
                List<ScheduleExtraCreateCandidateEvaluation<T>> evaluations = policy.evaluate(workspace, task).stream()
                        .filter(Objects::nonNull)
                        .filter(value -> value.getMachineCode() != null && !value.getMachineCode().trim().isEmpty())
                        .sorted(Comparator.comparing(ScheduleExtraCreateCandidateEvaluation<T>::getScore,
                                        Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(ScheduleExtraCreateCandidateEvaluation<T>::getMachineCode))
                        .collect(Collectors.toList());
                List<ScheduleExtraCreateCandidateEvaluation<T>> feasibleEvaluations = evaluations.stream()
                        .filter(this::feasible)
                        .collect(Collectors.toList());
                if (feasibleEvaluations.isEmpty()) {
                    this.appendMachineSkips(skippedList, task, evaluations, shiftOrder);
                    policy.skip(workspace, task, "EXTRA_CREATE_NO_FEASIBLE_MACHINE");
                    continue;
                }
                Set<String> attempted = new HashSet<>();
                boolean assigned = false;
                for (ScheduleExtraCreateCandidateEvaluation<T> evaluation : feasibleEvaluations) {
                    policy.assertActive(workspace, shiftOrder);
                    if (!attempted.add(evaluation.getMachineCode())) {
                        continue;
                    }
                    if (policy.hasExistingRow(workspace, task)) {
                        break;
                    }
                    evaluation.getTask().setExtraCreateWindowProof(evaluation.getWindowProof().copy());
                    if (policy.recheckAndAppend(workspace, evaluation)) {
                        assigned = true;
                        break;
                    }
                }
                if (!assigned) {
                    this.appendMachineSkips(skippedList, task, evaluations, shiftOrder);
                    policy.skip(workspace, task, "EXTRA_CREATE_NO_FEASIBLE_MACHINE");
                }
            }
            policy.assertActive(workspace, shiftOrder);
            policy.validate(workspace);
            policy.recordSkipped(workspace, shiftOrder, skippedList);
        } catch (RuntimeException failure) {
            if (failure instanceof CancellationException || Thread.currentThread().isInterrupted()
                    || !policy.discardAndVerify(context, workspace, failure)) {
                throw failure;
            }
            return;
        }
        // 发布异常不做通用降级：此时主上下文可能部分改变，必须由原失败路径阻止落库。
        policy.publish(context, workspace);
    }

    /**
     * 把未通过试算的机台原因收敛为逐机证据。
     *
     * <p>同一个候选可能有多台机台失败；有具体拒绝码时按机台记录，缺少拒绝码时只保留一条通用证据，
     * 避免同一物料重复输出无信息量的空原因。</p>
     *
     * @param skippedList 已收集的未建行证据
     * @param task        当前候选
     * @param evaluations 全部机台试算结果
     * @param shiftOrder  当前班次
     */
    private void appendMachineSkips(List<ScheduleExtraCreateSkipReason> skippedList,
                                    T task,
                                    List<ScheduleExtraCreateCandidateEvaluation<T>> evaluations,
                                    int shiftOrder) {
        if (task == null) {
            return;
        }
        boolean recorded = false;
        for (ScheduleExtraCreateCandidateEvaluation<T> evaluation : evaluations) {
            if (evaluation == null || evaluation.getRejectReasonCode() == null) {
                continue;
            }
            skippedList.add(ScheduleExtraCreateSkipReason.of(task.getBusinessKey(), shiftOrder,
                    evaluation.getMachineCode(), evaluation.getRejectReasonCode(), evaluation.getRejectDetail()));
            recorded = true;
        }
        if (!recorded) {
            skippedList.add(ScheduleExtraCreateSkipReason.of(task.getBusinessKey(), shiftOrder, null,
                    "EXTRA_CREATE_NO_FEASIBLE_MACHINE", null));
        }
    }

    /**
     * 检查最终量和用途；评分为0不等于不可行，未证实的资源不能凭评分放行。
     *
     * @param evaluation 单机隔离证明
     * @return 是否有明确非负起排配置及合法正备库承接量
     */
    private boolean feasible(ScheduleExtraCreateCandidateEvaluation<T> evaluation) {
        T task = evaluation.getTask();
        return evaluation.getRejectReasonCode() == null && evaluation.isProofsReady()
                && evaluation.getMachineCode() != null
                && evaluation.getWindowProof() != null && evaluation.getWindowProof().isReady()
                && !evaluation.getMachineCode().trim().isEmpty()
                && task != null && Boolean.TRUE.equals(task.getExtraCreateTask())
                && task.getTargetRequestQty() != null && task.getTargetRequestQty().signum() > 0
                && task.getTargetRequestQty().compareTo(evaluation.getWindowProof().getEligibleDemandQty()) <= 0
                && Boolean.TRUE.equals(task.getExtraEligibleTask())
                && task.getQuantityStage() == ScheduleQuantityStage.POST_LOSS_CANDIDATE
                && task.getMinStartQty() != null && task.getMinStartQty().signum() >= 0
                && evaluation.getAllowedQty() != null && evaluation.getAllowedQty().signum() > 0
                && evaluation.getAllowedQty().stripTrailingZeros().scale() <= 0
                && task.getExtraTargetQty() != null && task.getExtraAllowedQty() != null
                && evaluation.getAllowedQty().compareTo(task.getExtraTargetQty()) <= 0
                && evaluation.getAllowedQty().compareTo(task.getExtraAllowedQty()) <= 0
                && evaluation.getAllowedQty().compareTo(task.getMinStartQty()) >= 0;
    }
}
