package com.zlt.aps.tc.service;

/**
 * 胎侧人工异步任务审计人上下文。
 *
 * <p>Web安全上下文不会自动传播到异步线程，因此由任务创建时固化的操作人临时传递给现有调度日志逻辑。</p>
 */
public final class TcOperationAuditContext {

    private static final ThreadLocal<String> OPERATOR_HOLDER = new ThreadLocal<>();

    private TcOperationAuditContext() {
    }

    /**
     * 获取当前异步任务操作人。
     *
     * @return 操作人
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
