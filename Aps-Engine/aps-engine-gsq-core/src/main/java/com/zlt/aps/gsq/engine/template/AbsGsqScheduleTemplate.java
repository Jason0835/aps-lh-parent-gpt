package com.zlt.aps.gsq.engine.template;

import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.enums.GsqScheduleStepEnum;
import lombok.extern.slf4j.Slf4j;

/**
 * 钢丝圈排程模板方法抽象基类。
 *
 * <p>定义排程的标准10阶段执行骨架，子类通过绑定10个Handler实现具体逻辑：</p>
 *
 * <pre>
 * S1 前置校验与数据加载
 *   ↓
 * S2.1 库存预测
 *   ↓
 * S2.2 需求量计算
 *   ↓
 * S2.3 计划量计算
 *   ↓
 * S3 班次排产分配
 *   ↓
 * S4 胎圈/钢丝圈停产协调
 *   ↓
 * S5 班次均衡调整
 *   ↓
 * S5.5 定额校验与顺序重置
 *   ↓
 * S5.6 最终剩余产能回填
 *   ↓
 * S6 结果校验与持久化
 * </pre>
 *
 * <p>每个阶段执行前会校验上下文是否已被中断，中断则跳过后续步骤。</p>
 *
 * @author APS
 */
@Slf4j
public abstract class AbsGsqScheduleTemplate {

    /**
     * 执行排程模板方法（不可重写）。
     *
     * <p>按 S1 → S2.1 → S2.2 → S2.3 → S3 → S4 → S5 → S5.5 → S5.6 → S6 顺序执行10个阶段。</p>
     *
     * @param context 排程上下文
     */
    public final void execute(GsqScheduleContext context) {
        context.setStartTime(new java.util.Date());
        log.info("========== 钢丝圈自动排程开始, batchNo={}, scheduleDate={}, factoryCode={} ==========",
                context.getBatchNo(), context.getScheduleDate(), context.getFactoryCode());

        // S1: 前置校验与数据加载
        context.setCurrentStep(GsqScheduleStepEnum.S1_PRE_VALIDATION.getCode());
        doPreValidation(context);

        // S2.1: 库存预测
        context.setCurrentStep(GsqScheduleStepEnum.S2_1_STOCK_PREDICT.getCode());
        doStockPredict(context);

        // S2.2: 需求量计算
        context.setCurrentStep(GsqScheduleStepEnum.S2_2_DEMAND_QTY.getCode());
        doDemandQty(context);

        // S2.3: 计划量计算
        context.setCurrentStep(GsqScheduleStepEnum.S2_3_PLAN_QTY.getCode());
        doPlanQty(context);

        // S3: 班次排产分配
        context.setCurrentStep(GsqScheduleStepEnum.S3_MACHINE_ASSIGN.getCode());
        doMachineAssign(context);

        // S4: 胎圈/钢丝圈停产协调
        context.setCurrentStep(GsqScheduleStepEnum.S4_STOP_COORDINATION.getCode());
        doStopCoordination(context);

        // S5: 班次均衡调整
        context.setCurrentStep(GsqScheduleStepEnum.S5_BALANCE.getCode());
        doBalance(context);

        // S5.5: 定额校验与顺序重置
        context.setCurrentStep(GsqScheduleStepEnum.S5_5_QUOTA_VALIDATE.getCode());
        doQuotaValidate(context);

        // S5.6: 最终剩余产能回填（移至此执行，避免 S4/S5/S5.5 修改计划量后回填结果被覆盖）
        context.setCurrentStep(GsqScheduleStepEnum.S5_6_FINAL_RESIDUAL_CAPACITY.getCode());
        doResidualCapacity(context);

        // S6: 结果校验与持久化
        context.setCurrentStep(GsqScheduleStepEnum.S6_RESULT_PERSIST.getCode());
        doResultValidationAndSave(context);

        context.setEndTime(new java.util.Date());
        long elapsed = context.getEndTime().getTime() - context.getStartTime().getTime();
        log.info("========== 钢丝圈自动排程结束, batchNo={}, 总耗时: {}ms ==========", context.getBatchNo(), elapsed);
    }

    /** S1: 前置校验与数据加载 */
    protected abstract void doPreValidation(GsqScheduleContext context);

    /** S2.1: 库存预测 */
    protected abstract void doStockPredict(GsqScheduleContext context);

    /** S2.2: 需求量计算 */
    protected abstract void doDemandQty(GsqScheduleContext context);

    /** S2.3: 计划量计算 */
    protected abstract void doPlanQty(GsqScheduleContext context);

    /** S3: 班次排产分配 */
    protected abstract void doMachineAssign(GsqScheduleContext context);

    /** S4: 胎圈/钢丝圈停产协调 */
    protected abstract void doStopCoordination(GsqScheduleContext context);

    /** S5: 班次均衡调整 */
    protected abstract void doBalance(GsqScheduleContext context);

    /** S5.5: 定额校验与顺序重置 */
    protected abstract void doQuotaValidate(GsqScheduleContext context);

    /** S5.6: 最终剩余产能回填（在 S5.5 之后执行，避免回填结果被覆盖） */
    protected abstract void doResidualCapacity(GsqScheduleContext context);

    /** S6: 结果校验与持久化 */
    protected abstract void doResultValidationAndSave(GsqScheduleContext context);
}
