package com.zlt.aps.common.engine.schedule.engine;

import java.util.List;

/**
 * 独立备库新行的领域端口。资源、链和来源都必须属于同一个隔离工作区。
 *
 * @param <C> 领域上下文及工作区类型
 * @param <T> 领域任务类型
 */
public interface ScheduleExtraCreatePolicy<C, T extends ScheduleTaskDraftModel> {
    /**
     * @param context 主上下文 @return 本批冻结的新行开关是否开启
     */
    boolean isEnabled(C context);

    /**
     * @param context 主上下文 @param shiftOrder 班次；仅记录一次关闭事件
     */
    void disabled(C context, int shiftOrder);

    /**
     * 判断本班是否必须建立隔离工作区；默认保留已有调用方的完整复制行为。
     *
     * @param context    主上下文
     * @param shiftOrder 当前班次
     * @return true 表示需要创建工作区
     */
    default boolean requiresWorkspace(C context, int shiftOrder) {
        return true;
    }

    /**
     * @param context 主上下文 @param shiftOrder 班次 @return 无可变对象共享的隔离工作区
     */
    C openWorkspace(C context, int shiftOrder);

    /**
     * @param workspace 工作区 @param shiftOrder 班次 @return 已补齐来源并去重的候选
     */
    List<T> candidates(C workspace, int shiftOrder);

    /**
     * @param task 候选 @return 稳定物料/工艺/来源组键，禁止随机键
     */
    String candidateKey(T task);

    /**
     * @param workspace 工作区 @param task 候选 @return 本班该物料是否已有正节点
     */
    boolean hasExistingRow(C workspace, T task);

    /**
     * 逐机独立试算；未通过门禁的机台必须返回带拒绝码的评估，供过程日志说明原因。
     *
     * @param workspace 工作区 @param task 候选 @return 按最新链逐机独立试算的结果，无副作用
     */
    List<ScheduleExtraCreateCandidateEvaluation<T>> evaluate(C workspace, T task);

    /**
     * 用最新状态复核并一次提交；true返回前必须完成挂链、索引、来源和资源结算。
     * Q或生产时点变化时必须重算windowProof并更新任务证据，不能沿用旧窗口放行。
     *
     * @param workspace  工作区；后续候选必须读取此工作区的新链
     * @param evaluation 当前机台证明
     * @return 真实成功返回true；无副作用的正常复核失效返回false
     * @throws RuntimeException 半提交异常不得伪装成false，交由阶段恢复
     */
    boolean recheckAndAppend(C workspace, ScheduleExtraCreateCandidateEvaluation<T> evaluation);

    /**
     * @param workspace 工作区 @param task 候选 @param reasonCode 稳定拒绝码
     */
    void skip(C workspace, T task, String reasonCode);

    /**
     * 回写本轮未建行的逐机原因，保证“本班为什么没有备库行”在过程日志中可追溯。
     *
     * <p>建行成功与否都必须调用；实现方只能把已有证据写入任务原因码和过程日志，
     * 不得重新查询资料、重新计算数量或占用资源。</p>
     *
     * @param context    工作区或主上下文
     * @param shiftOrder 当前班次
     * @param skipped    未建行的候选及机台维度原因
     */
    default void recordSkipped(C context, int shiftOrder, List<ScheduleExtraCreateSkipReason> skipped) {
    }

    /**
     * @param workspace 工作区 @param shiftOrder 班次；检查取消、锁及版本，失效必须抛异常
     */
    void assertActive(C workspace, int shiftOrder);

    /**
     * @param workspace 工作区；发布前验证全部链、资源、分摊及宽表可表达性
     */
    void validate(C workspace);

    /**
     * @param context 主上下文 @param workspace 已验证工作区；一次发布，不重复挂链或关账
     */
    void publish(C context, C workspace);

    /**
     * 丢弃工作区并验证主基线完整；锁/并发失效、取消和未知恢复状态必须返回false。
     *
     * @param context   主上下文
     * @param workspace 失败工作区
     * @param failure   原异常，必须记录完整堆栈及本地化降级事件
     * @return 只有已验证恢复成功且已记录告警才返回true，否则由公共引擎重新抛出
     */
    boolean discardAndVerify(C context, C workspace, RuntimeException failure);
}
