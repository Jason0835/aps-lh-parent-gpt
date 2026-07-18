package com.zlt.aps.cd15.engine.task;

import com.zlt.aps.cd15.engine.constant.Cd15ScheduleTaskStatus;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 斜裁自动排程任务状态机测试。
 */
public class Cd15ScheduleTaskStateMachineTest {

    /**
     * 等待执行任务只能进入执行中或失败状态。
     */
    @Test
    public void pendingShouldOnlyMoveToRunningOrFailed() {
        assertTrue(Cd15ScheduleTaskStatus.canTransition("PENDING", "RUNNING"));
        assertTrue(Cd15ScheduleTaskStatus.canTransition("PENDING", "FAILED"));
        assertFalse(Cd15ScheduleTaskStatus.canTransition("PENDING", "SUCCESS"));
    }

    /**
     * 执行中任务只能进入成功或失败终态。
     */
    @Test
    public void runningShouldOnlyMoveToTerminalStatus() {
        assertTrue(Cd15ScheduleTaskStatus.canTransition("RUNNING", "SUCCESS"));
        assertTrue(Cd15ScheduleTaskStatus.canTransition("RUNNING", "FAILED"));
        assertFalse(Cd15ScheduleTaskStatus.canTransition("RUNNING", "PENDING"));
    }

    /**
     * 成功和失败状态均不可再次转换。
     */
    @Test
    public void terminalStatusShouldNotMoveAgain() {
        assertFalse(Cd15ScheduleTaskStatus.canTransition("SUCCESS", "FAILED"));
        assertFalse(Cd15ScheduleTaskStatus.canTransition("FAILED", "RUNNING"));
    }
}
