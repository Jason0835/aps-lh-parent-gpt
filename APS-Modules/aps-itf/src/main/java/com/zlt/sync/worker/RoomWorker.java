package com.zlt.sync.worker;

public class RoomWorker implements Runnable {

    private final Object stripe;
    private Task task;

    public RoomWorker(Object stripe, Task task) {
        this.stripe = stripe;
        this.task = task;
    }

    @Override
    public void run() {
        IProcessor iProcessor = (new ProcessorFactory()).createProcessor(this.task.getTaskType());
        iProcessor.handle(this.task);
    }

    @Override
    public String toString() {
        return "time: " + System.currentTimeMillis() / 1000 + ", stripe: " + stripe + ", " + task;
    }
}
