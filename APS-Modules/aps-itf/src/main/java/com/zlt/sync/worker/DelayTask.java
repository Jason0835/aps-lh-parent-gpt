package com.zlt.sync.worker;

public class DelayTask implements Runnable {

    private final Object stripe;
    private Task task;
    
    public DelayTask(Object stripe, Task task) {
        this.stripe = stripe;
        this.task = task;
    }
    
    @Override
    public void run() {
        System.out.println(toString());
        WorkThreadService.pool.submit(new RoomWorker(stripe, task));
    };
    
    @Override
    public String toString() {
        return "time: " + System.currentTimeMillis() / 1000 + ", stripe: " + stripe + ", " + task;
    }
}
