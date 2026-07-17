package com.zlt.aps.tc.service.impl;

import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.component.TcAutoScheduleExecutionGuard;
import com.zlt.aps.tc.domain.TcAutoScheduleTask;
import com.zlt.aps.tc.mapper.TcDispatcherLogMapper;
import com.zlt.aps.tc.mapper.TcScheduleResultMapper;
import com.zlt.aps.tc.service.TcAutoScheduleTaskService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 胎侧人工排程统一门面测试。
 */
@RunWith(MockitoJUnitRunner.class)
public class TcManualOperationFacadeTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TransactionStatus transactionStatus;

    @Mock
    private TcScheduleResultMapper scheduleResultMapper;

    @Mock
    private TcDispatcherLogMapper dispatcherLogMapper;

    @Mock
    private TcManualInsertRollingService rollingService;

    @Mock
    private TcManualMachineRuleValidator machineRuleValidator;

    @Mock
    private TcAutoScheduleExecutionGuard autoScheduleExecutionGuard;

    @Mock
    private TcAutoScheduleTaskService autoScheduleTaskService;

    /**
     * 初始化短事务模拟。
     */
    @Before
    public void setUp() {
        when(this.transactionManager.getTransaction(any())).thenReturn(this.transactionStatus);
    }

    /**
     * 验证多机台锁键规范化、去重并稳定排序。
     */
    @Test
    public void buildMachineLockKeysShouldSortAndDeduplicate() {
        List<String> keys = this.facade().buildMachineLockKeys("116", DateUtil.parseDate("2026-07-15"),
                Arrays.asList("TC02", " TC01 ", "TC02"));

        assertEquals(Arrays.asList(
                "TC_SCHEDULE:OPER_LOCK:116:2026-07-15:TC01",
                "TC_SCHEDULE:OPER_LOCK:116:2026-07-15:TC02"), keys);
    }

    /**
     * 验证过期任务版本被拒绝。
     */
    @Test(expected = ServiceException.class)
    public void validateExpectedVersionShouldRejectStaleVersion() {
        TcScheduleResult current = new TcScheduleResult();
        current.setTaskVersion(3L);

        this.facade().validateExpectedVersion(2L, current);
    }

    /**
     * 验证整行删除只允许未发布、发布失败和待发布状态。
     */
    @Test(expected = ServiceException.class)
    public void validateDeleteReleaseStatusShouldRejectPublishedResult() {
        TcScheduleResult current = new TcScheduleResult();
        current.setReleaseStatus("1");

        this.facade().validateDeleteReleaseStatus(Collections.singletonList(current));
    }

    /**
     * 验证多班插单的每个班次都必须位于第二个在产规格之后。
     */
    @Test(expected = ServiceException.class)
    public void validateInsertPositionShouldCheckEveryRequestedShift() {
        TcScheduleResult first = new TcScheduleResult();
        first.setClass1Sequence(1);
        first.setClass1FinishQty(BigDecimal.ONE);
        first.setClass2Sequence(1);
        first.setClass2FinishQty(BigDecimal.ONE);
        TcScheduleResult second = new TcScheduleResult();
        second.setClass1Sequence(2);
        second.setClass1FinishQty(BigDecimal.ONE);
        second.setClass2Sequence(2);
        second.setClass2FinishQty(BigDecimal.ONE);
        TcScheduleResult insert = new TcScheduleResult();
        insert.setClass1PlanQty(new BigDecimal("100"));
        insert.setClass1Sequence(3);
        insert.setClass2PlanQty(new BigDecimal("100"));
        insert.setClass2Sequence(2);

        this.facade().validateInsertAfterSecondProduction(insert, Arrays.asList(first, second));
    }

    /**
     * 验证业务或审计失败时短事务回滚并继续抛出异常。
     */
    @Test
    public void executeInTransactionShouldRollbackWhenActionFails() {
        Supplier<Integer> action = () -> {
            throw new ServiceException("audit failed");
        };

        try {
            this.facade().executeInTransaction(action);
            fail("预期人工操作事务失败");
        } catch (ServiceException exception) {
            assertEquals("audit failed", exception.getMessage());
        }

        verify(this.transactionManager).rollback(this.transactionStatus);
    }

    /**
     * 验证已存在等待或执行中的自动排程任务时拒绝人工操作并释放日期锁。
     */
    @Test
    public void insertTaskShouldRejectActiveAutoScheduleTaskAndReleaseGuard() {
        TcScheduleResult insertResult = new TcScheduleResult();
        insertResult.setFactoryCode("116");
        insertResult.setScheduleDate(DateUtil.parseDate("2026-07-15"));
        insertResult.setMachineCode("TC01");
        TcAutoScheduleTask activeTask = new TcAutoScheduleTask();
        activeTask.setTaskId("TC-TASK-001");
        when(this.autoScheduleExecutionGuard.acquire("116", insertResult.getScheduleDate()))
                .thenReturn("guard-token");
        when(this.autoScheduleTaskService.findActive("116", insertResult.getScheduleDate()))
                .thenReturn(activeTask);

        try {
            this.facade().insertTask(insertResult, "自动排程期间禁止插单");
            fail("存在活跃自动排程任务时应拒绝人工操作");
        } catch (ServiceException exception) {
            assertTrue(exception.getMessage() != null && !exception.getMessage().isEmpty());
        }

        verify(this.autoScheduleExecutionGuard).release(
                "116", insertResult.getScheduleDate(), "guard-token");
    }

    /**
     * 创建待测人工操作门面。
     *
     * @return 人工操作门面
     */
    private TcManualOperationFacade facade() {
        return new TcManualOperationFacade(this.redissonClient, this.transactionManager,
                this.scheduleResultMapper, this.dispatcherLogMapper, this.rollingService,
                this.machineRuleValidator, this.autoScheduleExecutionGuard, this.autoScheduleTaskService);
    }
}
