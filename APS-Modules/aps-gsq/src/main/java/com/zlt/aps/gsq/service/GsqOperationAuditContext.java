package com.zlt.aps.gsq.service;

/**
 * 钢丝圈人工异步任务审计人上下文。
 *
 * <p>对齐胎侧 {@code TcOperationAuditContext}。</p>
 *
 * <p>Web 安全上下文（{@code SecurityUtils.getLoginUser()}）不会自动传播到 {@code @Async} 异步线程，
 * 因此由任务创建时固化的操作人临时传递给现有调度日志逻辑。
 * 异步执行器在 {@code execute} 入口调用 {@link #setOperator} 设置操作人，
 * 在 {@code finally} 块调用 {@link #clear} 清理，避免线程复用造成审计人串用。</p>
 *
 * @author APS
 */
public final class GsqOperationAuditContext {

    /** 操作人 ThreadLocal 持有器。 */
    private static final ThreadLocal<String> OPERATOR_HOLDER = new ThreadLocal<>();

    /**
     * 工具类不允许实例化。
     */
    private GsqOperationAuditContext() {
    }

    /**
     * 获取当前异步任务操作人。
     *
     * @return 操作人，未设置时返回 null
     */
    public static String getOperator() {
        return OPERATOR_HOLDER.get();
    }

    /**
     * 设置当前异步任务操作人。
     *
     * @param operator 操作人
     */
    public static void setOperator(String operator) {
        OPERATOR_HOLDER.set(operator);
    }

    /**
     * 清理异步线程上下文，避免线程复用造成审计人串用。
     */
    public static void clear() {
        OPERATOR_HOLDER.remove();
    }
}
