package com.zlt.aps.common.engine.schedule.engine;

/** 自动排程公共主流程领域端口。 */
public interface AutoSchedulePolicy<C, R> {

    void validateContext(C context);

    void bootstrap(C context);

    void predictInventory(C context);

    void calculatePlan(C context);

    void sortTasks(C context);

    void assignMachines(C context);

    void buildSnapshotAndPersist(C context);

    R buildResponse(C context);
}
