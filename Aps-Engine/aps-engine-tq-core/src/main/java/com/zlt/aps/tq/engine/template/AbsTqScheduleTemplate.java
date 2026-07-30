package com.zlt.aps.tq.engine.template;

import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.enums.TqScheduleStepEnum;
import lombok.extern.slf4j.Slf4j;

/**
 * 胎圈排程模板方法抽象类。
 *
 * <p>定义胎圈排程不可变的算法骨架：S1 → S2.1 → S2.2 → S2.3 → S3 → S4 → S5 → S5.5 → S5.6 → S6，
 * 每步之间检查中断状态，任何一步中断则后续步骤全部跳过。</p>
 *
 * <p>子类 {@link TqScheduleTemplateImpl} 通过注入10个Handler来填充具体实现。</p>
 *
 * <p>S2 阶段拆分（Phase 1 重构）：</p>
 * <ul>
 *   <li>S2.1 库存预测：供应时长计算（策略可插拔：BY_STOCK / BY_SHIFT）+ 算法1模式筛选</li>
 *   <li>S2.2 需求量计算：收尾判断（策略可插拔：DEFAULT）+ 收尾提示和生产状态设置</li>
 *   <li>S2.3 计划量计算：6 班滚动 + 备库触发判断与分摊 + 取整与工装限制（策略可插拔：DEFAULT）</li>
 * </ul>
 *
 * <p>S5.6 执行顺序调整说明（Phase 2 重构）：</p>
 * <ul>
 *   <li>原 S3.5 位于 S3 之后，但 S4/S5/S5.5 会修改计划量，导致 S3.5 回填结果被覆盖</li>
 *   <li>现将剩余产能回填移至 S5.5 定额校验之后（即 S5.6），确保回填结果为最终结果</li>
 *   <li>S5.6 执行前会重新计算 backupRemainingQty，基于实际已排产量修正</li>
 * </ul>
 *
 * <pre>
 * 执行流程：
 * S1(前置校验) → S2.1(库存预测) → S2.2(需求量计算) → S2.3(计划量计算) → S3(班次排产分配) → S4(停产协调) → S5(均衡调整) → S5.5(定额校验) → S5.6(剩余产能回填) → S6(持久化)
 * </pre>
 *
 * @author APS
 */
@Slf4j
public abstract class AbsTqScheduleTemplate {

    /**
     * 胎圈排程模板方法（定义不可变的算法骨架）。
     *
     * <p>执行流程：</p>
     * <ol>
     *   <li>S1:   前置校验与数据加载</li>
     *   <li>S2.1: 库存预测（供应时长计算）</li>
     *   <li>S2.2: 需求量计算（收尾判断）</li>
     *   <li>S2.3: 计划量计算（6 班滚动 + 备库分摊）</li>
     *   <li>S3:   班次排产分配</li>
     *   <li>S4:   成型/胎圈停产协调</li>
     *   <li>S5:   班次均衡调整</li>
     *   <li>S5.5: 定额校验与顺序重置</li>
     *   <li>S5.6: 最终剩余产能回填（原 S3.5，移至此处避免被 S4/S5/S5.5 覆盖）</li>
     *   <li>S6:   结果校验与持久化</li>
     * </ol>
     *
     * <p>每步之间检查中断状态，任何一步中断则后续步骤全部跳过。</p>
     *
     * @param context 排程上下文，必须预先设置scheduleDate
     */
    public final void execute(TqScheduleContext context) {
        long startTime = System.currentTimeMillis();
        log.info("========== 胎圈排程开始, 排程日期:{} ==========", context.getScheduleDate());

        try {
            // S1: 前置校验与数据加载
            context.setCurrentStep(TqScheduleStepEnum.S1_PRE_VALIDATION.getCode());
            doPreValidation(context);
            if (context.isInterrupted()) {
                logInterrupt(context);
                return;
            }

            // S2.1: 库存预测（供应时长计算）
            context.setCurrentStep(TqScheduleStepEnum.S2_1_STOCK_PREDICT.getCode());
            doStockPredict(context);
            if (context.isInterrupted()) {
                logInterrupt(context);
                return;
            }

            // S2.2: 需求量计算（收尾判断）
            context.setCurrentStep(TqScheduleStepEnum.S2_2_DEMAND_QTY_CALC.getCode());
            doDemandQtyCalc(context);
            if (context.isInterrupted()) {
                logInterrupt(context);
                return;
            }

            // S2.3: 计划量计算（6 班滚动 + 备库分摊）
            context.setCurrentStep(TqScheduleStepEnum.S2_3_PLAN_QTY_CALC.getCode());
            doPlanQtyCalc(context);
            if (context.isInterrupted()) {
                logInterrupt(context);
                return;
            }

            // S3: 班次排产分配
            context.setCurrentStep(TqScheduleStepEnum.S3_MACHINE_ASSIGN.getCode());
            doMachineAssign(context);
            if (context.isInterrupted()) {
                logInterrupt(context);
                return;
            }

            // S4: 成型/胎圈停产协调
            context.setCurrentStep(TqScheduleStepEnum.S4_STOP_COORDINATION.getCode());
            doStopCoordination(context);
            if (context.isInterrupted()) {
                logInterrupt(context);
                return;
            }

            // S5: 班次均衡调整
            context.setCurrentStep(TqScheduleStepEnum.S5_BALANCE.getCode());
            doBalance(context);
            if (context.isInterrupted()) {
                logInterrupt(context);
                return;
            }

            // S5.5: 定额校验与顺序重置（在S4/S5修改计划量之后统一校验，防止超定额且重置生产顺序）
            context.setCurrentStep(TqScheduleStepEnum.S5_5_QUOTA_VALIDATE.getCode());
            doQuotaValidate(context);
            if (context.isInterrupted()) {
                logInterrupt(context);
                return;
            }

            // S5.6: 最终剩余产能回填（原 S3.5，移至 S5.5 之后执行，避免回填结果被 S4/S5/S5.5 覆盖）
            context.setCurrentStep(TqScheduleStepEnum.S5_6_RESIDUAL_CAPACITY.getCode());
            doResidualCapacity(context);
            if (context.isInterrupted()) {
                logInterrupt(context);
                return;
            }

            // S6: 结果校验与持久化
            context.setCurrentStep(TqScheduleStepEnum.S6_RESULT_PERSIST.getCode());
            doResultValidationAndSave(context);

        } catch (Exception e) {
            log.error("胎圈排程异常, 当前步骤:{}, 排程日期:{}", context.getCurrentStep(), context.getScheduleDate(), e);
            throw e;
        } finally {
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("========== 胎圈排程结束, 批次号:{}, 耗时:{}ms ==========",
                    context.getBatchNo(), elapsed);
        }
    }

    /**
     * S1: 前置校验与数据加载。
     *
     * <p>职责：校验施工信息完整性、加载全部基础数据到Context。</p>
     *
     * @param context 排程上下文
     */
    protected abstract void doPreValidation(TqScheduleContext context);

    /**
     * S2.1: 库存预测。
     *
     * <p>职责：根据供应时长策略计算每条记录的库存供应时长，并在算法1模式下筛选保证班数不足的规格。</p>
     *
     * @param context 排程上下文
     */
    protected abstract void doStockPredict(TqScheduleContext context);

    /**
     * S2.2: 需求量计算。
     *
     * <p>职责：根据需求量策略做收尾判断，设置 closeOutSpecFlag；并设置收尾提示标识和生产状态。</p>
     *
     * @param context 排程上下文
     */
    protected abstract void doDemandQtyCalc(TqScheduleContext context);

    /**
     * S2.3: 计划量计算。
     *
     * <p>职责：根据计划量策略执行 6 班滚动计划量计算（含备库触发判断、备库分摊、取整与工装限制）。</p>
     *
     * @param context 排程上下文
     */
    protected abstract void doPlanQtyCalc(TqScheduleContext context);

    /**
     * S3: 班次排产分配。
     *
     * <p>职责：3步排产策略（当前机台→切换机台→延后）、机台定额约束、生产顺序设置。</p>
     *
     * @param context 排程上下文
     */
    protected abstract void doMachineAssign(TqScheduleContext context);

    /**
     * S5.6: 最终剩余产能回填（原 S3.5）。
     *
     * <p>职责：在 S5.5 定额校验完成、所有计划量修改结束后，回收每个机台每班的剩余产能（quota - 已排产量），
     * 按三级优先级回填到该机台上的规格：</p>
     * <ul>
     *   <li>Priority-1: 触发备库胎圈（backupTriggerClass > 0 且 backupRemainingQty > 0），按触发班次升序+供应时长升序</li>
     *   <li>Priority-2: 未触发备库但供应时长 &lt; 阈值的规格，按供应时长升序</li>
     *   <li>Priority-3: 未触发备库且供应时长 ≥ 阈值的规格，按供应时长升序</li>
     * </ul>
     * <p>第6班特殊处理：把所有剩余量（含跨班次备库剩余量）塞入第6班，避免超出6班范围丢失量。</p>
     *
     * <p>执行顺序说明：原 S3.5 位于 S3 之后，但 S4/S5/S5.5 会修改计划量导致回填结果被覆盖。
     * 现移至 S5.5 之后执行（即 S5.6），确保回填结果为最终结果且不被覆盖。
     * 执行前会重新计算 backupRemainingQty，基于实际已排产量修正（解决 S3 阶段 deferToNextClass 不扣减的问题）。</p>
     *
     * @param context 排程上下文
     */
    protected abstract void doResidualCapacity(TqScheduleContext context);

    /**
     * S4: 成型/胎圈停产协调。
     *
     * <p>职责：成型停产提前备货、胎圈停产前移、停产交集开产逻辑。</p>
     *
     * @param context 排程上下文
     */
    protected abstract void doStopCoordination(TqScheduleContext context);

    /**
     * S5: 班次均衡调整。
     *
     * <p>职责：按机台定额约束调整各班次产量、交接班库存平衡、同日班次差额调整。</p>
     *
     * @param context 排程上下文
     */
    protected abstract void doBalance(TqScheduleContext context);

    /**
     * S5.5: 定额校验与顺序重置。
     *
     * <p>职责：在S4(停产协调)和S5(班次均衡)修改计划量之后，统一校验所有机台所有班次是否超定额，
     * 超出部分按优先级延后到下一班次；最后统一重置6个班次的生产顺序值。</p>
     *
     * <p>背景：S4/S5修改计划量时不检查机台定额，可能导致单机台单班次总排产量超过quota。
     * 本步骤作为所有计划量修改的最终校验出口，确保数据一致性。</p>
     *
     * @param context 排程上下文
     */
    protected abstract void doQuotaValidate(TqScheduleContext context);

    /**
     * S6: 结果校验与持久化。
     *
     * <p>职责：外协分离、历史合并、数据落库、日志记录。</p>
     *
     * @param context 排程上下文
     */
    protected abstract void doResultValidationAndSave(TqScheduleContext context);

    /**
     * 记录中断日志
     */
    private void logInterrupt(TqScheduleContext context) {
        log.warn("胎圈排程在步骤[{}]被中断, 原因:{}", context.getCurrentStep(), context.getInterruptReason());
    }
}
