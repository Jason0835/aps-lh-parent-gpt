package com.zlt.aps.tq.engine.template;

import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.enums.TqScheduleStepEnum;
import lombok.extern.slf4j.Slf4j;

/**
 * 胎圈排程模板方法抽象类。
 *
 * <p>定义胎圈排程不可变的算法骨架：S1 → S2 → S3 → S3.5 → S4 → S5 → S5.5 → S6，
 * 每步之间检查中断状态，任何一步中断则后续步骤全部跳过。</p>
 *
 * <p>子类 {@link TqScheduleTemplateImpl} 通过注入7个Handler来填充具体实现。</p>
 *
 * <pre>
 * 执行流程：
 * S1(前置校验与数据加载) → S2(需求计算) → S3(班次排产分配) → S3.5(剩余产能分配) → S4(成型/胎圈停产协调) → S5(班次均衡调整) → S5.5(定额校验与顺序重置) → S6(结果校验与持久化)
 *       ↓ 中断检查              ↓ 中断检查         ↓ 中断检查              ↓ 中断检查              ↓ 中断检查              ↓ 中断检查
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
     *   <li>S1: 前置校验与数据加载</li>
     *   <li>S2: 需求计算</li>
     *   <li>S3: 班次排产分配</li>
     *   <li>S3.5: 剩余产能分配</li>
     *   <li>S4: 成型/胎圈停产协调</li>
     *   <li>S5: 班次均衡调整</li>
     *   <li>S5.5: 定额校验与顺序重置</li>
     *   <li>S6: 结果校验与持久化</li>
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

            // S2: 需求计算
            context.setCurrentStep(TqScheduleStepEnum.S2_DEMAND_CALC.getCode());
            doDemandCalc(context);
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

            // S3.5: 剩余产能分配
            context.setCurrentStep(TqScheduleStepEnum.S3_5_RESIDUAL_CAPACITY.getCode());
            doResidualCapacity(context);
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
     * S2: 需求计算。
     *
     * <p>职责：计算供应时长、计划量、收尾判断（基于胎胚关联汇总）。</p>
     *
     * @param context 排程上下文
     */
    protected abstract void doDemandCalc(TqScheduleContext context);

    /**
     * S3: 班次排产分配。
     *
     * <p>职责：3步排产策略（当前机台→切换机台→延后）、机台定额约束、生产顺序设置。</p>
     *
     * @param context 排程上下文
     */
    protected abstract void doMachineAssign(TqScheduleContext context);

    /**
     * S3.5: 剩余产能分配。
     *
     * <p>职责：S3机台分配完成后，回收每个机台每班的剩余产能（quota - 已排产量），
     * 按三级优先级回填到该机台上的规格：</p>
     * <ul>
     *   <li>Priority-1: 触发备库胎圈（backupTriggerClass > 0 且 backupRemainingQty > 0），按触发班次升序+供应时长升序</li>
     *   <li>Priority-2: 未触发备库但供应时长 &lt; 阈值的规格，按供应时长升序</li>
     *   <li>Priority-3: 未触发备库且供应时长 ≥ 阈值的规格，按供应时长升序</li>
     * </ul>
     * <p>第6班特殊处理：把所有剩余量（含跨班次备库剩余量）塞入第6班，避免超出6班范围丢失量。</p>
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
