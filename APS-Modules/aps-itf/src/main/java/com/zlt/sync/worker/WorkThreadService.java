package com.zlt.sync.worker;

import java.util.Map;
import java.util.concurrent.*;

public class WorkThreadService {

    private static final int THREADS = 8;

    // 工作线程池
    public final static ExecutorService pool = new StripedExecutorService(THREADS * 2);

    private static final Map<String, Object> taskMap = new ConcurrentHashMap<>();

    public static Object getTaskObject(String threadId) {
        if (taskMap.get(threadId) == null) {
            synchronized (taskMap) {
                if (taskMap.get(threadId) == null) {
                    taskMap.put(threadId, new Object());
                }
            }
        }
        return taskMap.get(threadId);
    }

    // 提交任务
    public static void submit(Task task) {
        pool.submit(new RoomWorker(getTaskObject(task.getThreadId()), task));
    }

}
