package com.tlt.aps.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * 线程池工具类（IO密集型任务适用）
 * @author wengpc
 */
@Slf4j
public class ThreadPoolUtil {

    /**
     * 等待队列长度
     */
    private static final int BLOCKING_QUEUE_LENGTH = 100;

    /**
     * 闲置线程存活时间
     */
    private static final int KEEP_ALIVE_TIME = 60;

    /**
     * 闲置线程存活时间单位
     */
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

    /**
     * 线程池执行器
     */
    private static volatile ThreadPoolExecutor executor;

    private ThreadPoolUtil() {}


    /**
     * 获取单例的线程池对象(单例的双重检查式)
     * @return 线程池实例
     */
    public static ThreadPoolExecutor getThreadPool() {
        if (executor == null) {
            synchronized (ThreadPoolUtil.class) {
                if (executor == null) {
                    log.info("创建自定义线程池...");
                    executor = createThreadPool();
                }
            }
        }
        return executor;
    }


    /**
     * 创建线程池
     * @return 线程池实例
     */
    private static ThreadPoolExecutor createThreadPool() {
        // 获取处理器数量
        int cpuNum = Runtime.getRuntime().availableProcessors();
        // 核心线程数（理论的值，根据实际情况进行调整）
        int corePoolSize = cpuNum * 2;
        // 最大线程数（理论的值，根据实际情况进行调整）
        int maxPoolSize = cpuNum * 4;
        // 自定义线程池
        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                new ArrayBlockingQueue<>(BLOCKING_QUEUE_LENGTH),
                new ThreadFactoryBuilder().setNameFormat("custom-pool-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * 执行任务（有返回值）
     * @param task
     * @param <T>
     * @return
     */
    public static <T> Future<T> submit(Callable<T> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        log.info("提交线程池任务（有返回值）...");
        return getThreadPool().submit(task);
    }

    /**
     * 执行任务
     * @param task
     */
    public static void execute(Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }
        log.info("提交线程池任务...");
        getThreadPool().execute(task);
    }


    /**
     * 关闭线程池
     */
    public static void shutdown() {
        if (executor != null) {
            executor.shutdown();
            executor = null;
            log.info("线程池已关闭");
        }
    }

}