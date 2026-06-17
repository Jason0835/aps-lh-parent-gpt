package com.zlt.aps.tm.service.impl;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.engine.schedule.ScheduleOperationContext;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResultExplain;
import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleRequestVo;
import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleResponseVo;
import com.zlt.aps.tm.engine.domain.TmPersistResult;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.engine.template.TmScheduleTemplateImpl;
import com.zlt.aps.tm.mapper.*;
import com.zlt.aps.tm.service.TmAutoScheduleDataLoadService;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TmScheduleResultServiceImplTest {

    @Mock
    private TmScheduleResultMapper tmScheduleResultMapper;

    @Mock
    private TmDispatcherLogMapper tmDispatcherLogMapper;

    @Mock
    private TmMachineInfoMapper tmMachineInfoMapper;

    @Mock
    private TmScheduleResultExplainMapper tmScheduleResultExplainMapper;

    @Mock
    private TmScheduleUnplannedMapper tmScheduleUnplannedMapper;

    @Mock
    private TmAutoScheduleDataLoadService tmAutoScheduleDataLoadService;

    @Mock
    private TmScheduleTemplateImpl tmScheduleTemplate;

    @InjectMocks
    private TmScheduleResultServiceImpl service;

    @Test
    public void tmScheduleResultShouldMapOrderNoFieldToOrderNoColumn() throws NoSuchFieldException {
        TableField tableField = TmScheduleResult.class.getDeclaredField("orderNo").getAnnotation(TableField.class);

        assertNotNull(tableField);
        assertEquals("ORDER_NO", tableField.value());
    }

    @Test
    public void tmScheduleResultShouldMapClassStartTimeFieldToClassStartTimeColumn() throws NoSuchFieldException {
        TableField tableField = TmScheduleResult.class.getDeclaredField("class1StartTime").getAnnotation(TableField.class);

        assertNotNull(tableField);
        assertEquals("CLASS1_START_TIME", tableField.value());
    }

    @Test
    public void tmScheduleResultExplainShouldMapTraceIdFieldToTraceIdColumn() throws NoSuchFieldException {
        TableField tableField = TmScheduleResultExplain.class.getDeclaredField("traceId").getAnnotation(TableField.class);

        assertNotNull(tableField);
        assertEquals("TRACE_ID", tableField.value());
    }

    @Test
    public void tmScheduleResultExplainShouldMapDetailedDesignFieldsToColumns() throws NoSuchFieldException {
        assertTableField("factoryCode", "FACTORY_CODE");
        assertTableField("lastShiftSupplyQty", "LAST_SHIFT_SUPPLY_QTY");
        assertTableField("monthSurplusDeductQty", "MONTH_SURPLUS_DEDUCT_QTY");
        assertTableField("toolLimitAdjustQty", "TOOL_LIMIT_ADJUST_QTY");
        assertTableField("minStartAdjustQty", "MIN_START_ADJUST_QTY");
        assertTableField("tailRoundAdjustQty", "TAIL_ROUND_ADJUST_QTY");
        assertTableField("capacityAdjustQty", "CAPACITY_ADJUST_QTY");
        assertTableField("stockQty", "STOCK_QTY");
        assertTableField("planStockQty", "PLAN_STOCK_QTY");
        assertTableField("supplyHours", "SUPPLY_HOURS");
        assertTableField("coverageShiftCount", "COVERAGE_SHIFT_COUNT");
        assertTableField("lastShiftPlanQty", "LAST_SHIFT_PLAN_QTY");
        assertTableField("monthSurplusQty", "MONTH_SURPLUS_QTY");
        assertTableField("requiredQty", "REQUIRED_QTY");
        assertTableField("ruleSummaryDesc", "RULE_SUMMARY_DESC");
        assertTableField("selectedMachineScore", "SELECTED_MACHINE_SCORE");
        assertTableField("manualLockedFlag", "MANUAL_LOCKED_FLAG");
        assertTableField("sequenceLockFlag", "SEQUENCE_LOCK_FLAG");
        assertTableField("forceChangeFlag", "FORCE_CHANGE_FLAG");
        assertTableField("generateMode", "GENERATE_MODE");
        assertTableField("resultStatus", "RESULT_STATUS");
        assertTableField("currentStepCode", "CURRENT_STEP_CODE");
    }

    @Test
    public void tmScheduleResultExplainMapperShouldProvideBaseWriteCapability() {
        assertEquals(true, CommBaseMapper.class.isAssignableFrom(TmScheduleResultExplainMapper.class));
    }

    @Test
    public void updateTmScheduleResultSetsWaitReleasingWhenReleasedPlanQtyChanges() {
        TmScheduleResult oldSchedule = new TmScheduleResult();
        oldSchedule.setId(1L);
        oldSchedule.setMachineCode("TM-01");
        oldSchedule.setClass1PlanQty(BigDecimal.TEN);

        TmScheduleResult newSchedule = new TmScheduleResult();
        newSchedule.setId(1L);
        newSchedule.setReleaseStatus(ApsConstant.IS_RELEASE);
        newSchedule.setMachineCode("TM-01");
        newSchedule.setClass1PlanQty(BigDecimal.valueOf(20));

        when(tmScheduleResultMapper.selectById(1L)).thenReturn(oldSchedule);
        when(tmScheduleResultMapper.updateById(newSchedule)).thenReturn(1);

        assertEquals(1, service.updateTmScheduleResult(newSchedule));
        assertEquals(ApsConstant.WAIT_RELEASING, newSchedule.getReleaseStatus());
        verify(tmScheduleResultMapper).updateById(newSchedule);
    }

    @Test
    public void updateTmScheduleResultSetsNoReleaseWhenChangedRecordHasBlankReleaseStatus() {
        TmScheduleResult oldSchedule = new TmScheduleResult();
        oldSchedule.setId(2L);
        oldSchedule.setMachineCode("TM-02");
        oldSchedule.setClass2PlanQty(BigDecimal.ONE);

        TmScheduleResult newSchedule = new TmScheduleResult();
        newSchedule.setId(2L);
        newSchedule.setMachineCode("TM-03");
        newSchedule.setClass2PlanQty(BigDecimal.ONE);

        when(tmScheduleResultMapper.selectById(2L)).thenReturn(oldSchedule);
        when(tmScheduleResultMapper.updateById(newSchedule)).thenReturn(1);

        assertEquals(1, service.updateTmScheduleResult(newSchedule));
        assertEquals(ApsConstant.NO_RELEASE, newSchedule.getReleaseStatus());
        verify(tmScheduleResultMapper).updateById(newSchedule);
    }

    @Test
    public void logicDeleteByFactoryCodeAndScheduleDateDeletesUnplannedAndExplainBeforeResult() {
        Date scheduleDate = new Date();

        service.logicDeleteByFactoryCodeAndScheduleDate("116", scheduleDate);

        org.mockito.InOrder inOrder = inOrder(tmScheduleUnplannedMapper, tmScheduleResultExplainMapper, tmScheduleResultMapper);
        inOrder.verify(tmScheduleUnplannedMapper).logicDeleteByFactoryCodeAndScheduleDate("116", scheduleDate);
        inOrder.verify(tmScheduleResultExplainMapper).logicDeleteByFactoryCodeAndScheduleDate("116", scheduleDate);
        inOrder.verify(tmScheduleResultMapper).logicDeleteByFactoryCodeAndScheduleDate("116", scheduleDate);
    }

    @Test
    public void validateAutoPlanShouldRequireConfirmWhenOldBatchAllNoRelease() {
        TmAutoScheduleRequestVo request = buildAutoRequest();
        TmScheduleResult oldResult = new TmScheduleResult();
        oldResult.setReleaseStatus(ApsConstant.NO_RELEASE);
        when(tmScheduleResultMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(Collections.singletonList(oldResult));

        TmAutoScheduleResponseVo response = service.validateAutoPlan(request);

        assertEquals(Boolean.TRUE, response.getSuccess());
        assertEquals(Boolean.TRUE, response.getConfirmRequired());
    }

    @Test(expected = ServiceException.class)
    public void validateAutoPlanShouldRejectWhenOldBatchHasReleasedResult() {
        TmAutoScheduleRequestVo request = buildAutoRequest();
        TmScheduleResult oldResult = new TmScheduleResult();
        oldResult.setReleaseStatus(ApsConstant.IS_RELEASE);
        when(tmScheduleResultMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(Collections.singletonList(oldResult));

        service.validateAutoPlan(request);
    }

    @Test
    public void autoPlanShouldDeleteOldBatchAfterConfirmWhenAllNoRelease() {
        TmAutoScheduleRequestVo request = buildAutoRequest();
        request.setConfirmOverwrite(Boolean.TRUE);
        TmScheduleResult oldResult = new TmScheduleResult();
        oldResult.setReleaseStatus(ApsConstant.NO_RELEASE);
        when(tmScheduleResultMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(Collections.singletonList(oldResult));
        mockEngineOutput();

        TmAutoScheduleResponseVo response = service.autoPlan(request);

        assertEquals(Boolean.TRUE, response.getSuccess());
        assertEquals(Boolean.FALSE, response.getConfirmRequired());
        org.mockito.InOrder inOrder = inOrder(tmScheduleUnplannedMapper, tmScheduleResultExplainMapper, tmScheduleResultMapper);
        inOrder.verify(tmScheduleUnplannedMapper).logicDeleteByFactoryCodeAndScheduleDate("116", request.getScheduleDate());
        inOrder.verify(tmScheduleResultExplainMapper).logicDeleteByFactoryCodeAndScheduleDate("116", request.getScheduleDate());
        inOrder.verify(tmScheduleResultMapper).logicDeleteByFactoryCodeAndScheduleDate("116", request.getScheduleDate());
    }

    @Test
    public void autoPlanShouldUseTemplatePersistResultWithoutDirectLoadOrInsert() {
        TmAutoScheduleRequestVo request = buildAutoRequest();
        when(tmScheduleResultMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(Collections.emptyList());
        mockEngineOutput(1, 0);

        TmAutoScheduleResponseVo response = service.autoPlan(request);

        assertEquals(Boolean.TRUE, response.getSuccess());
        assertEquals(Integer.valueOf(1), response.getResultCount());
        assertEquals(Integer.valueOf(0), response.getUnplannedCount());
        verify(tmScheduleTemplate).execute(any(TmScheduleContext.class));
        verify(tmAutoScheduleDataLoadService, never()).loadAllData(any(TmScheduleContext.class));
        verify(tmScheduleResultMapper, never()).insert(any(TmScheduleResult.class));
        verify(tmScheduleResultExplainMapper, never()).insert(any(TmScheduleResultExplain.class));
    }

    @Test(expected = ServiceException.class)
    public void insertTaskShouldRejectWhenSequenceIsNotAfterSecondInProductionSpec() {
        TmScheduleResult scheduleResult = new TmScheduleResult();
        scheduleResult.setScheduleDate(new Date());
        scheduleResult.setTreadCode("TR-001");
        scheduleResult.setClass1Sequence(2);

        service.insertTask(scheduleResult);
    }

    /**
     * 构造自动排程测试请求。
     *
     * @return 自动排程请求
     */
    private TmAutoScheduleRequestVo buildAutoRequest() {
        TmAutoScheduleRequestVo request = new TmAutoScheduleRequestVo();
        request.setFactoryCode("116");
        request.setScheduleDate(new Date());
        request.setTraceId("TRACE-TEST");
        request.setOperator("tester");
        return request;
    }

    /**
     * 断言解释实体字段映射到指定数据库列。
     *
     * @param fieldName  实体字段名
     * @param columnName 数据库列名
     * @throws NoSuchFieldException 字段不存在时抛出
     */
    private void assertTableField(String fieldName, String columnName) throws NoSuchFieldException {
        TableField tableField = TmScheduleResultExplain.class.getDeclaredField(fieldName).getAnnotation(TableField.class);

        assertNotNull(tableField);
        assertEquals(columnName, tableField.value());
    }

    /**
     * 构造默认引擎输出。
     */
    private void mockEngineOutput() {
        mockEngineOutput(1, 0);
    }

    /**
     * 构造自动排程引擎输出。
     *
     * @param resultCount    已生成排程结果数量
     * @param unplannedCount 未排数量
     */
    private void mockEngineOutput(Integer resultCount, Integer unplannedCount) {
        doAnswer(invocation -> {
            TmScheduleContext context = invocation.getArgument(0);
            TmTaskDraft taskDraft = new TmTaskDraft();
            taskDraft.setOrderNo("ORD-001");
            taskDraft.setTreadCode("TR-001");
            taskDraft.setGlueCode("GL-001");
            taskDraft.setMouthPlateCode("MP-001");
            taskDraft.setMachineCode("TM-01");
            taskDraft.setShiftOrder(1);
            taskDraft.setPlanQty(BigDecimal.TEN);
            context.setTaskDraftList(Collections.singletonList(taskDraft));
            LocalDate scheduleDate = context.getScheduleDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            context.getTaskChainGroup().getOrCreate("TM-01", scheduleDate, 1)
                    .append(new ScheduleTaskNode<>(taskDraft.getBusinessKey(), taskDraft, "TM-01", scheduleDate,
                                    "CLASS1", 1, taskDraft.getPlanQty()),
                            new ScheduleOperationContext("tester", "AUTO_APPEND", "TRACE-TEST"));
            TmPersistResult persistResult = new TmPersistResult();
            persistResult.setResultCount(resultCount);
            persistResult.setExplainCount(resultCount);
            persistResult.setUnplannedCount(unplannedCount);
            context.setPersistResult(persistResult);
            return new TmAutoScheduleResponseVo();
        }).when(tmScheduleTemplate).execute(any(TmScheduleContext.class));
    }
}
