package com.zlt.aps.tc.service.impl;

import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.ScheduleOperationContext;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResultExplain;
import com.zlt.aps.tc.engine.domain.TcScheduleContext;
import com.zlt.aps.tc.engine.domain.TcSnapshotBuildResult;
import com.zlt.aps.tc.engine.domain.TcTaskDraft;
import com.zlt.aps.tc.engine.service.impl.TcPersistService;
import com.zlt.aps.tc.engine.service.impl.TcScheduleQualitySummaryService;
import com.zlt.aps.tc.engine.service.impl.TcSnapshotBuildService;
import com.zlt.aps.tc.mapper.TcAutoScheduleTaskMapper;
import com.zlt.aps.tc.mapper.TcScheduleResultExplainMapper;
import com.zlt.aps.tc.mapper.TcScheduleResultMapper;
import com.zlt.aps.tc.mapper.TcScheduleUnplannedMapper;
import com.zlt.core.dao.basedao.BaseDao;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * 胎侧自动排程核心短事务回滚测试。
 */
@RunWith(MockitoJUnitRunner.class)
public class TcBizSnapshotAndPersistServiceTest {

    @Mock
    private TcSnapshotBuildService snapshotBuildService;

    @Mock
    private TcPersistService persistService;

    @Mock
    private TcScheduleResultMapper scheduleResultMapper;

    @Mock
    private TcScheduleResultExplainMapper scheduleResultExplainMapper;

    @Mock
    private TcScheduleUnplannedMapper scheduleUnplannedMapper;

    @Mock
    private TcAutoScheduleTaskMapper autoScheduleTaskMapper;

    @Mock
    private BaseDao baseDao;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TransactionStatus transactionStatus;

    /**
     * 初始化事务状态和旧结果空集合。
     */
    @Before
    public void setUp() {
        when(this.transactionManager.getTransaction(any())).thenReturn(this.transactionStatus);
        when(this.transactionStatus.createSavepoint()).thenReturn(new Object());
        when(this.scheduleResultMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(this.scheduleUnplannedMapper.selectList(any())).thenReturn(Collections.emptyList());
    }

    /**
     * 验证结果批量和单行写入均失败时，整次核心事务回滚且不继续写解释表。
     */
    @Test
    public void shouldRollbackWholeTransactionWhenResultPersistFails() {
        TcTaskDraft task = this.buildAssignedTask();
        TcScheduleContext context = this.buildAssignedContext(task);
        TcScheduleResult result = this.buildScheduleResult(task);
        when(this.snapshotBuildService.buildTaskExplain(task, context)).thenReturn(new TcSnapshotBuildResult());
        when(this.persistService.convertChainToResult(any(), any())).thenReturn(Collections.singletonList(result));
        when(this.baseDao.saveBatch(anyList())).thenThrow(new RuntimeException("batch failed"));
        when(this.baseDao.save(any(BaseEntity.class))).thenThrow(new RuntimeException("row failed"));

        try {
            this.buildService().snapshotAndPersist(context);
            fail("核心结果写入失败时应抛出业务异常");
        } catch (ServiceException exception) {
            assertTrue(exception.getMessage() != null && !exception.getMessage().isEmpty());
        }

        verify(this.transactionManager).rollback(this.transactionStatus);
        verify(this.transactionStatus).rollbackToSavepoint(any());
    }

    /**
     * 验证同机台、同胎侧、同施工版本和工艺的跨班任务归并为一行结果，
     * 工单号使用批次号加四位序号，解释记录仍分别关联来源任务。
     */
    @Test
    public void shouldMergeCompatibleResultsAndKeepExplainRelation() {
        TcTaskDraft firstTask = this.buildAssignedTask();
        firstTask.setOrderNo("SOURCE-001");
        firstTask.setSourceOrderNos("SOURCE-001");
        firstTask.setShiftOrder(1);
        firstTask.setPlanQty(new BigDecimal("100"));
        TcTaskDraft secondTask = this.buildAssignedTask();
        secondTask.setOrderNo("SOURCE-002");
        secondTask.setSourceOrderNos("SOURCE-002");
        secondTask.setShiftOrder(2);
        secondTask.setPlanQty(new BigDecimal("200"));
        TcScheduleContext context = this.buildAssignedContext(firstTask);
        context.setTaskDraftList(Arrays.asList(firstTask, secondTask));
        this.appendAssignedTask(context, secondTask);

        TcScheduleResult firstResult = this.buildScheduleResult(firstTask);
        firstResult.setClass1PlanQty(new BigDecimal("100"));
        TcScheduleResult secondResult = this.buildScheduleResult(secondTask);
        secondResult.setClass1PlanQty(null);
        secondResult.setClass1Sequence(null);
        secondResult.setClass2PlanQty(new BigDecimal("200"));
        secondResult.setClass2Sequence(1);
        when(this.snapshotBuildService.buildTaskExplain(any(), any()))
                .thenReturn(new TcSnapshotBuildResult());
        when(this.persistService.convertChainToResult(any(), any()))
                .thenReturn(Collections.singletonList(firstResult), Collections.singletonList(secondResult));
        when(this.persistService.convertExplain(any(), any(), any())).thenAnswer(invocation -> {
            TcTaskDraft task = invocation.getArgument(0);
            TcScheduleResultExplain explain = new TcScheduleResultExplain();
            explain.setTaskBusinessKey(task.getBusinessKey());
            explain.setShiftOrder(task.getShiftOrder());
            return explain;
        });
        when(this.baseDao.saveBatch(anyList())).thenAnswer(invocation -> {
            List<BaseEntity> entityList = invocation.getArgument(0);
            entityList.stream()
                    .filter(TcScheduleResult.class::isInstance)
                    .map(TcScheduleResult.class::cast)
                    .forEach(result -> result.setId(501L));
            return entityList.size();
        });

        this.buildService().snapshotAndPersist(context);

        ArgumentCaptor<List> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(this.baseDao, times(2)).saveBatch(batchCaptor.capture());
        List<TcScheduleResult> resultList = batchCaptor.getAllValues().stream()
                .flatMap(List::stream)
                .filter(TcScheduleResult.class::isInstance)
                .map(TcScheduleResult.class::cast)
                .collect(java.util.stream.Collectors.toList());
        List<TcScheduleResultExplain> explainList = batchCaptor.getAllValues().stream()
                .flatMap(List::stream)
                .filter(TcScheduleResultExplain.class::isInstance)
                .map(TcScheduleResultExplain.class::cast)
                .collect(java.util.stream.Collectors.toList());
        assertEquals(1, resultList.size());
        assertEquals("TC-ROLLBACK-001-0001", resultList.get(0).getOrderNo());
        assertEquals(0, new BigDecimal("100").compareTo(resultList.get(0).getClass1PlanQty()));
        assertEquals(0, new BigDecimal("200").compareTo(resultList.get(0).getClass2PlanQty()));
        assertEquals(2, explainList.size());
        assertTrue(explainList.stream().allMatch(explain -> Long.valueOf(501L).equals(explain.getResultId())));
    }

    /**
     * 验证零计划任务不写结果和未排表，只保留不关联结果行的解释记录。
     */
    @Test
    public void shouldPersistOnlyExplainForZeroPlanTask() {
        TcTaskDraft task = this.buildAssignedTask();
        task.setPlanQty(BigDecimal.ZERO);
        task.setMachineCode(null);
        task.setUnplannedReasonCode(null);
        TcScheduleContext context = new TcScheduleContext();
        context.setFactoryCode("116");
        context.setBatchNo("TC-ZERO-001");
        context.setScheduleDate(java.sql.Date.valueOf(LocalDate.of(2026, 7, 14)));
        context.setTaskDraftList(Collections.singletonList(task));
        when(this.snapshotBuildService.buildTaskExplain(task, context)).thenReturn(new TcSnapshotBuildResult());
        when(this.persistService.convertExplain(any(), any(), any())).thenAnswer(invocation -> {
            TcScheduleResultExplain explain = new TcScheduleResultExplain();
            explain.setTaskBusinessKey(task.getBusinessKey());
            return explain;
        });
        when(this.baseDao.saveBatch(anyList())).thenAnswer(invocation -> {
            List<BaseEntity> entityList = invocation.getArgument(0);
            return entityList.size();
        });

        this.buildService().snapshotAndPersist(context);

        ArgumentCaptor<List> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(this.baseDao).saveBatch(batchCaptor.capture());
        assertEquals(1, batchCaptor.getValue().size());
        assertTrue(batchCaptor.getValue().get(0) instanceof TcScheduleResultExplain);
        assertNull(((TcScheduleResultExplain) batchCaptor.getValue().get(0)).getResultId());
        assertEquals(0, context.getPersistResult().getResultCount());
        assertEquals(0, context.getPersistResult().getUnplannedCount());
        assertEquals(1, context.getPersistResult().getExplainCount());
    }

    /**
     * 创建被测快照落库服务。
     *
     * @return 被测服务
     */
    private TcBizSnapshotAndPersistService buildService() {
        return new TcBizSnapshotAndPersistService(this.snapshotBuildService, this.persistService,
                this.scheduleResultMapper, this.scheduleResultExplainMapper, this.scheduleUnplannedMapper,
                this.autoScheduleTaskMapper, new TcScheduleQualitySummaryService(), this.baseDao,
                this.transactionManager);
    }

    /**
     * 构造已分配的正计划任务。
     *
     * @return 任务草稿
     */
    private TcTaskDraft buildAssignedTask() {
        TcTaskDraft task = new TcTaskDraft();
        task.setOrderNo("SOURCE-001");
        task.setSourceOrderNos("SOURCE-001");
        task.setSidewallCode("SW-001");
        task.setConstructionVersion("V1");
        task.setSidewallCraft("CRAFT-1");
        task.setGlueCode("G-001");
        task.setBaseGlueCode("B-001");
        task.setMouthPlateCode("MP-001");
        task.setMachineCode("TC01");
        task.setShiftOrder(1);
        task.setPlanQty(BigDecimal.valueOf(100));
        return task;
    }

    /**
     * 构造包含一条机台班次任务链的排程上下文。
     *
     * @param task 已分配任务
     * @return 排程上下文
     */
    private TcScheduleContext buildAssignedContext(TcTaskDraft task) {
        TcScheduleContext context = new TcScheduleContext();
        context.setFactoryCode("116");
        context.setBatchNo("TC-ROLLBACK-001");
        context.setScheduleDate(java.sql.Date.valueOf(LocalDate.of(2026, 7, 14)));
        context.setTaskDraftList(Collections.singletonList(task));
        ScheduleTaskLinkedList<TcTaskDraft> chain = context.getTaskChainGroup()
                .getOrCreate("TC01", LocalDate.of(2026, 7, 14), 1);
        ScheduleTaskNode<TcTaskDraft> node = new ScheduleTaskNode<>(task.getBusinessKey(), task,
                "TC01", LocalDate.of(2026, 7, 14), "CLASS1", 1, BigDecimal.valueOf(100));
        chain.append(node, new ScheduleOperationContext("tester", "AUTO_APPEND", "TRACE-ROLLBACK"));
        return context;
    }

    /**
     * 将任务追加到对应机台班次任务链。
     *
     * @param context 排程上下文
     * @param task 待追加任务
     */
    private void appendAssignedTask(TcScheduleContext context, TcTaskDraft task) {
        ScheduleTaskLinkedList<TcTaskDraft> chain = context.getTaskChainGroup()
                .getOrCreate(task.getMachineCode(), LocalDate.of(2026, 7, 14), task.getShiftOrder());
        ScheduleTaskNode<TcTaskDraft> node = new ScheduleTaskNode<>(task.getBusinessKey(), task,
                task.getMachineCode(), LocalDate.of(2026, 7, 14), "CLASS" + task.getShiftOrder(),
                task.getShiftOrder(), task.getPlanQty());
        chain.append(node, new ScheduleOperationContext("tester", "AUTO_APPEND", "TRACE-ROLLBACK"));
    }

    /**
     * 构造批量保存使用的结果实体。
     *
     * @param task 任务草稿
     * @return 排程结果
     */
    private TcScheduleResult buildScheduleResult(TcTaskDraft task) {
        TcScheduleResult result = new TcScheduleResult();
        result.setFactoryCode("116");
        result.setBatchNo("TC-ROLLBACK-001");
        result.setOrderNo("TC-ROLLBACK-001-0001");
        result.setScheduleDate(java.sql.Date.valueOf(LocalDate.of(2026, 7, 14)));
        result.setMachineCode(task.getMachineCode());
        result.setSidewallCode(task.getSidewallCode());
        result.setConstructionVersion(task.getConstructionVersion());
        result.setSidewallCraft(task.getSidewallCraft());
        result.setGlueCode(task.getGlueCode());
        result.setBaseGlueCode(task.getBaseGlueCode());
        result.setMouthPlateCode(task.getMouthPlateCode());
        if (Integer.valueOf(1).equals(task.getShiftOrder())) {
            result.setClass1Sequence(1);
            result.setClass1PlanQty(task.getPlanQty());
        }
        if (Integer.valueOf(2).equals(task.getShiftOrder())) {
            result.setClass2Sequence(1);
            result.setClass2PlanQty(task.getPlanQty());
        }
        return result;
    }
}
