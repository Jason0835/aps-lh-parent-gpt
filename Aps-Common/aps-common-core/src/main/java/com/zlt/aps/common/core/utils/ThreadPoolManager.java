package com.zlt.aps.common.core.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolManager {
    private static ThreadPoolManager instance = new ThreadPoolManager();
    private int maximumPoolSize;//最大线程池数量，表示当缓冲队列满的时候能继续容纳的等待任务的数量
    private int corePoolSize;
    private long keepAliveTime = 60;//存活时间
    private TimeUnit unit = TimeUnit.SECONDS;
    private ThreadPoolExecutor executor;



    private ThreadPoolManager() {
        corePoolSize = Runtime.getRuntime().availableProcessors() * 2 + 1;
        maximumPoolSize = corePoolSize;
        executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, new LinkedBlockingQueue<Runnable>(5), Executors.defaultThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());
        executor.allowCoreThreadTimeOut(true);
    }

    public static ThreadPoolManager getInstance() {
        if (instance == null) {
            instance = new ThreadPoolManager();
        }
        return instance;
    }



    /**
     * 获取线程池中活动线程的数量
     *
     * @return
     */
    public int getActiveThreadCount() {

        return executor.getActiveCount();
    }

    /**
     * 获取当前线程池的线程数量
     *
     * @return
     */
    public int getCurrentThreadPoolSize() {
        return executor.getPoolSize();
    }

    /**
     * 执行任务
     */
    public void execute(Runnable runnable) {
        if (runnable == null){
            return;
        }

        executor.execute(runnable);
    }

    /**
     * 执行任务
     */
    public Future<?> submit(Callable callable) {
        if (callable == null){
            return null;
        }

       return executor.submit(callable);
    }

    /**
     * 从线程池中移除任务
     */
    public void remove(Runnable runnable) {
        if (runnable == null) {
            return;
        }

        executor.remove(runnable);
    }

}
