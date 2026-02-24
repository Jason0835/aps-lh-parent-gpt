package com.tlt.aps.utils;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.function.Supplier;

/**
 * 上下文传递工具类：封装 CompletableFuture 异步任务的 RequestAttributes 传递逻辑
 * 支持：1. 有返回值任务（Supplier） 2. 无返回值任务（Runnable/void）
 */
public class SpringContextSupplierUtil {

    /**
     * 包装 Supplier 接口（有返回值），自动传递 RequestAttributes 上下文
     * @param supplier 原业务逻辑 Supplier
     * @param <T> 业务返回值类型
     * @return 包装后的 Supplier
     */
    public static <T> Supplier<T> wrap(Supplier<T> supplier) {
        // 父线程捕获上下文快照
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        // 返回包装后的Supplier
        return () -> {
            // 子线程恢复上下文
            RequestContextHolder.setRequestAttributes(requestAttributes);
            try {
                // 执行原业务逻辑
                return supplier.get();
            } finally {
                // 执行完毕清除上下文，避免线程池复用污染
                RequestContextHolder.resetRequestAttributes();
            }
        };
    }

    /**
     * 包装 Runnable 接口（无返回值/void），自动传递 RequestAttributes 上下文
     * @param runnable 原业务逻辑 Runnable
     * @return 包装后的 Runnable
     */
    public static Runnable wrap(Runnable runnable) {
        // 父线程捕获上下文快照
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        // 返回包装后的Runnable
        return () -> {
            // 子线程恢复上下文
            RequestContextHolder.setRequestAttributes(requestAttributes);
            try {
                // 执行原业务逻辑
                runnable.run();
            } finally {
                // 执行完毕清除上下文，避免线程池复用污染
                RequestContextHolder.resetRequestAttributes();
            }
        };
    }
}