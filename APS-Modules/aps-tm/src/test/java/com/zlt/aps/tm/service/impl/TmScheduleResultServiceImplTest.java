package com.zlt.aps.tm.service.impl;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResultExplain;
import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleRequestVo;
import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleResponseVo;
import com.zlt.aps.tm.mapper.*;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

        TmAutoScheduleResponseVo response = service.autoPlan(request);

        assertEquals(Boolean.TRUE, response.getSuccess());
        assertEquals(Boolean.FALSE, response.getConfirmRequired());
        org.mockito.InOrder inOrder = inOrder(tmScheduleUnplannedMapper, tmScheduleResultExplainMapper, tmScheduleResultMapper);
        inOrder.verify(tmScheduleUnplannedMapper).logicDeleteByFactoryCodeAndScheduleDate("116", request.getScheduleDate());
        inOrder.verify(tmScheduleResultExplainMapper).logicDeleteByFactoryCodeAndScheduleDate("116", request.getScheduleDate());
        inOrder.verify(tmScheduleResultMapper).logicDeleteByFactoryCodeAndScheduleDate("116", request.getScheduleDate());
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
        return request;
    }
}
