package com.zlt.aps.tm.service.impl;

import com.baomidou.mybatisplus.annotation.TableField;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.mapper.TmDispatcherLogMapper;
import com.zlt.aps.tm.mapper.TmMachineInfoMapper;
import com.zlt.aps.tm.mapper.TmScheduleResultExplainMapper;
import com.zlt.aps.tm.mapper.TmScheduleResultMapper;
import com.zlt.aps.tm.mapper.TmScheduleUnplannedMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
}
