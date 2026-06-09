package com.zlt.aps.tq.engine.template;

import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.enums.TqScheduleStepEnum;
import lombok.extern.slf4j.Slf4j;

/**
 * 胎圈排程模板方法抽象类。
 *
 * <p>定义胎圈排程不可变的算法骨架：S1 → S2 → S3 → S4，
 * 每步之间检查中断状态，任何一步中断则后续步骤全部跳过。</p>
 *
 * <p>子类 {@link TqScheduleTemplateImpl} 通过注入4个Handler来填充具体实现。</p>
 *
 * <pre>
 * 执行流程：
 * S1(前置校验与数据加载) → S2(需求计算与均衡) → S3(机台分配与排序) → S4(结果校验与持久化)
 *       ↓ 中断检查              ↓ 中断检查              ↓ 中断检查
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
     *   <li>S2: 需求计算与均衡</li>
     *   <li>S3: 机台分配与排序</li>
     *   <li>S4: 结果校验与持久化</li>
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

            // S2: 需求计算与均衡
            context.setCurrentStep(TqScheduleStepEnum.S2_DEMAND_CALC.getCode());
            doDemandCalcAndBalance(context);
            if (context.isInterrupted()) {
                logInterrupt(context);
                return;
            }

            // S3: 机台分配与排序
            context.setCurrentStep(TqScheduleStepEnum.S3_MACHINE_ASSIGN.getCode());
            doMachineAssignAndSort(context);
            if (context.isInterrupted()) {
                logInterrupt(context);
                return;
            }

            // S4: 结果校验与持久化
            context.setCurrentStep(TqScheduleStepEnum.S4_RESULT_PERSIST.getCode());
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
     * S2: 需求计算与均衡。
     *
     * <p>职责：计算供应时长、计划量、收尾判断、两天均衡。</p>
     *
     * @param context 排程上下文
     */
    protected abstract void doDemandCalcAndBalance(TqScheduleContext context);

    /**
     * S3: 机台分配与排序。
     *
     * <p>职责：多维度机台过滤（定点/口型板/寸口/维修）、候选机台排序、生产顺序设置。</p>
     *
     * @param context 排程上下文
     */
    protected abstract void doMachineAssignAndSort(TqScheduleContext context);

    /**
     * S4: 结果校验与持久化。
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
