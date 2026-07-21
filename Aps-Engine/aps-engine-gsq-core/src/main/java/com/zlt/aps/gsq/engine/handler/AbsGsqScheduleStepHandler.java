package com.zlt.aps.gsq.engine.handler;

import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.exception.GsqScheduleException;
import lombok.extern.slf4j.Slf4j;

/**
 * 钢丝圈排程步骤Handler抽象基类。
 *
 * <p>定义Handler的执行模板方法，统一处理：</p>
 * <ul>
 *   <li>中断检查：如果Context已被上游步骤中断，则跳过当前步骤</li>
 *   <li>耗时统计：记录每个步骤的执行耗时</li>
 *   <li>异常捕获：区分业务异常(GsqScheduleException)和系统异常</li>
 *   <li>日志输出：步骤开始/完成/异常日志</li>
 * </ul>
 *
 * <p>子类只需实现 {@link #doHandle(GsqScheduleContext)} 和 {@link #getStepName()} 即可。</p>
 *
 * @author APS
 */
@Slf4j
public abstract class AbsGsqScheduleStepHandler {

    /**
     * 执行步骤处理（模板方法，不可重写）。
     *
     * <p>执行流程：中断检查 → 日志记录 → 子类逻辑 → 异常处理 → 耗时统计</p>
     *
     * @param context 排程上下文
     */
    public final void handle(GsqScheduleContext context) {
        // 中断检查：上游步骤已中断则跳过
        if (context.isInterrupted()) {
            log.warn("[{}] 排程已中断, 跳过此步骤。中断原因: {}", getStepName(), context.getInterruptReason());
            return;
        }

        log.info("[{}] ===== 开始执行 =====", getStepName());
        long startTime = System.currentTimeMillis();

        try {
            doHandle(context);
        } catch (GsqScheduleException e) {
            // 业务异常：中断排程，记录原因
            log.error("[{}] 排程业务异常: {}", getStepName(), e.getMessage());
            context.interruptSchedule(e.getMessage());
        } catch (Exception e) {
            // 系统异常：中断排程，记录异常堆栈
            log.error("[{}] 执行异常", getStepName(), e);
            context.interruptSchedule(getStepName() + "执行异常: " + e.getMessage());
        } finally {
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[{}] ===== 执行完成, 耗时: {}ms =====", getStepName(), elapsed);
        }
    }

    /**
     * 子类实现具体的步骤处理逻辑。
     *
     * @param context 排程上下文
     */
    protected abstract void doHandle(GsqScheduleContext context);

    /**
     * 获取步骤名称，用于日志输出。
     *
     * @return 步骤名称
     */
    protected abstract String getStepName();
}
