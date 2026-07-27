package com.zlt.aps.tm.service;

/**
 * 胎面人工异步任务审计人上下文。
 *
 * <p>异步线程不继承Web安全上下文，由任务创建时固化的操作人补充调度日志审计字段。</p>
 */
public final class TmOperationAuditContext {

    private static final ThreadLocal<String> OPERATOR_HOLDER = new ThreadLocal<>();

    private TmOperationAuditContext() {
    }

    /**
     * 获取当前任务操作人。
     *
     * @return 操作人
     */
    public static String getOperator() {
        return OPERATOR_HOLDER.get();
    }

    /**
     * 设置当前任务操作人。
     *
     * @param operator 操作人
     */
    public static void setOperator(String operator) {
        OPERATOR_HOLDER.set(operator);
    }

    /**
     * 清理线程上下文。
     */
    public static void clear() {
        OPERATOR_HOLDER.remove();
    }
}
