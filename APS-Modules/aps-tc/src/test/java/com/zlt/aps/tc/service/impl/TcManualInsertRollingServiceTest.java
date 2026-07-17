package com.zlt.aps.tc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.domain.entity.TcScheduleUnplanned;
import com.zlt.aps.tc.mapper.TcScheduleResultExplainMapper;
import com.zlt.aps.tc.mapper.TcScheduleResultMapper;
import com.zlt.aps.tc.mapper.TcScheduleUnplannedMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 胎侧人工排程横表滚动服务测试。
 */
@RunWith(MockitoJUnitRunner.class)
public class TcManualInsertRollingServiceTest {

    @Mock
    private TcScheduleResultMapper scheduleResultMapper;

    @Mock
    private TcManualMachineRuleValidator machineRuleValidator;

    @Mock
    private TcScheduleUnplannedMapper scheduleUnplannedMapper;

    @Mock
    private TcScheduleResultExplainMapper scheduleResultExplainMapper;

    /**
     * 默认所有班次使用 5500 米有效产能。
     */
    @Before
    public void setUp() {
        org.mockito.Mockito.lenient().when(this.machineRuleValidator.resolveRollingCapacity(
                any(TcScheduleResult.class), any(String.class), any(Integer.class)))
                .thenReturn(new BigDecimal("5500"));
    }

    /**
     * 验证插单后同班次后续任务顺序后移，并写入插单来源。
     */
    @Test
    public void insertAndRollShouldShiftLaterTasks() {
        TcScheduleResult first = this.result(1L, "TC01", "SW-A", 1, 1, "100", null);
        TcScheduleResult second = this.result(2L, "TC01", "SW-B", 1, 2, "100", null);
        TcScheduleResult third = this.result(3L, "TC01", "SW-C", 1, 3, "100", null);
        TcScheduleResult insert = this.result(null, "TC01", "SW-D", 1, 3, "100", null);

        when(this.scheduleResultMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(first, second, third));
        when(this.scheduleResultMapper.insert(any(TcScheduleResult.class))).thenReturn(1);
        when(this.scheduleResultMapper.updateById(any(TcScheduleResult.class))).thenReturn(1);

        assertEquals(1, this.service().insertAndRoll(insert));

        ArgumentCaptor<TcScheduleResult> inserted = ArgumentCaptor.forClass(TcScheduleResult.class);
        verify(this.scheduleResultMapper).insert(inserted.capture());
        assertEquals("INSERT", inserted.getValue().getDataSource());
        assertEquals("0", inserted.getValue().getReleaseStatus());
        assertEquals(Long.valueOf(0L), inserted.getValue().getTaskVersion());
        ArgumentCaptor<TcScheduleResult> updated = ArgumentCaptor.forClass(TcScheduleResult.class);
        verify(this.scheduleResultMapper, atLeastOnce()).updateById(updated.capture());
        TcScheduleResult shifted = updated.getAllValues().stream()
                .filter(item -> Long.valueOf(3L).equals(item.getId()))
                .findFirst().orElseThrow(AssertionError::new);
        assertEquals(Integer.valueOf(4), shifted.getClass1Sequence());
        assertEquals(Long.valueOf(1L), shifted.getTaskVersion());
    }

    /**
     * 验证第六班溢出写未排表并保留稳定任务业务键。
     */
    @Test
    public void insertAndRollShouldWriteUnplannedAfterSixthShift() {
        TcScheduleResult occupied = this.result(1L, "TC01", "SW-A", 6, 1, "5400", null);
        TcScheduleResult insert = this.result(null, "TC01", "SW-B", 6, 2, "300", null);
        when(this.scheduleResultMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(occupied));
        when(this.scheduleResultMapper.insert(any(TcScheduleResult.class))).thenReturn(1);
        when(this.scheduleResultMapper.updateById(any(TcScheduleResult.class))).thenReturn(1);
        when(this.scheduleUnplannedMapper.insert(any(TcScheduleUnplanned.class))).thenReturn(1);
        when(this.scheduleResultExplainMapper.insert(any())).thenReturn(1);

        this.service().insertAndRoll(insert);

        ArgumentCaptor<TcScheduleUnplanned> unplanned = ArgumentCaptor.forClass(TcScheduleUnplanned.class);
        verify(this.scheduleUnplannedMapper).insert(unplanned.capture());
        assertEquals(new BigDecimal("200"), unplanned.getValue().getPlanQty());
        assertEquals("116-SW-B-6-RESULTNEW-INSERT-NEW", unplanned.getValue().getTaskBusinessKey());
        assertEquals("CAPACITY_NOT_ENOUGH", unplanned.getValue().getUnplannedReasonCode());
    }

    /**
     * 验证多班插单计划不会被提前合并到最早班次。
     */
    @Test
    public void insertAndRollShouldKeepRequestedMinimumShiftForEachInsertTask() {
        TcScheduleResult insert = this.result(null, "TC01", "SW-MULTI", 1, 3, "100", null);
        insert.setClass2PlanQty(new BigDecimal("200"));
        insert.setClass2Sequence(2);
        when(this.scheduleResultMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        when(this.scheduleResultMapper.insert(any(TcScheduleResult.class))).thenReturn(1);

        this.service().insertAndRoll(insert);

        ArgumentCaptor<TcScheduleResult> inserted = ArgumentCaptor.forClass(TcScheduleResult.class);
        verify(this.scheduleResultMapper).insert(inserted.capture());
        assertEquals(new BigDecimal("100"), inserted.getValue().getClass1PlanQty());
        assertEquals(new BigDecimal("200"), inserted.getValue().getClass2PlanQty());
    }

    /**
     * 验证人工滚动使用生效的胎侧单班最大可排量参数。
     */
    @Test
    public void insertAndRollShouldUseConfiguredShiftCapacityLimit() {
        TcScheduleResult insert = this.result(null, "TC01", "SW-LIMIT", 1, 1, "1200", null);
        when(this.scheduleResultMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        when(this.machineRuleValidator.resolveRollingCapacity(any(TcScheduleResult.class),
                any(String.class), any(Integer.class))).thenReturn(new BigDecimal("1000"));
        when(this.scheduleResultMapper.insert(any(TcScheduleResult.class))).thenReturn(1);

        this.service().insertAndRoll(insert);

        ArgumentCaptor<TcScheduleResult> inserted = ArgumentCaptor.forClass(TcScheduleResult.class);
        verify(this.scheduleResultMapper).insert(inserted.capture());
        assertEquals(new BigDecimal("1000"), inserted.getValue().getClass1PlanQty());
        assertEquals(new BigDecimal("200"), inserted.getValue().getClass2PlanQty());
    }

    /**
     * 验证调量不得小于完成量。
     */
    @Test(expected = ServiceException.class)
    public void changeQtyAndRollShouldRejectQtyLessThanFinishQty() {
        TcScheduleResult current = this.result(1L, "TC01", "SW-A", 1, 1, "100", "80");
        TcScheduleResult change = this.result(1L, "TC01", "SW-A", 1, 1, "70", null);
        when(this.scheduleResultMapper.selectById(1L)).thenReturn(current);

        this.service().changeQtyAndRoll(change);
    }

    /**
     * 验证已发布结果调量后回退待发布状态并递增版本。
     */
    @Test
    public void changeQtyAndRollShouldRollbackPublishedStatusAndIncreaseVersion() {
        TcScheduleResult current = this.result(1L, "TC01", "SW-A", 1, 1, "100", "20");
        current.setReleaseStatus("1");
        current.setTaskVersion(3L);
        TcScheduleResult change = this.result(1L, "TC01", "SW-A", 1, 1, "120", null);
        when(this.scheduleResultMapper.selectById(1L)).thenReturn(current);
        when(this.scheduleResultMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(current));
        when(this.scheduleResultMapper.updateById(any(TcScheduleResult.class))).thenReturn(1);

        this.service().changeQtyAndRoll(change);

        ArgumentCaptor<TcScheduleResult> updated = ArgumentCaptor.forClass(TcScheduleResult.class);
        verify(this.scheduleResultMapper).updateById(updated.capture());
        assertEquals("5", updated.getValue().getReleaseStatus());
        assertEquals(Long.valueOf(4L), updated.getValue().getTaskVersion());
    }

    /**
     * 验证转机只迁移选中班次，源结果其他班次保持不变。
     */
    @Test
    public void changeMachineAndRollShouldKeepOtherSourceShifts() {
        TcScheduleResult source = this.result(1L, "TC01", "SW-A", 3, 1, "300", null);
        source.setClass1PlanQty(new BigDecimal("100"));
        source.setClass1Sequence(1);
        source.setClass2PlanQty(new BigDecimal("200"));
        source.setClass2Sequence(1);
        source.setClass2FinishQty(new BigDecimal("20"));
        source.setTaskVersion(4L);
        TcScheduleResult targetPrefix = this.result(2L, "TC02", "SW-B", 3, 1, "100", null);
        TcScheduleResult transfer = this.result(1L, "TC02", "SW-A", 3, 2, "300", null);

        when(this.scheduleResultMapper.selectById(1L)).thenReturn(source);
        when(this.scheduleResultMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(source))
                .thenReturn(Collections.singletonList(targetPrefix));
        when(this.scheduleResultMapper.updateById(any(TcScheduleResult.class))).thenReturn(1);
        when(this.scheduleResultMapper.insert(any(TcScheduleResult.class))).thenReturn(1);

        this.service().changeMachineAndRoll(transfer);

        ArgumentCaptor<TcScheduleResult> updated = ArgumentCaptor.forClass(TcScheduleResult.class);
        verify(this.scheduleResultMapper, atLeastOnce()).updateById(updated.capture());
        TcScheduleResult changedSource = updated.getAllValues().stream()
                .filter(item -> Long.valueOf(1L).equals(item.getId()))
                .findFirst().orElseThrow(AssertionError::new);
        assertEquals("TC01", changedSource.getMachineCode());
        assertEquals(new BigDecimal("100"), changedSource.getClass1PlanQty());
        assertEquals(new BigDecimal("200"), changedSource.getClass2PlanQty());
        assertEquals(new BigDecimal("20"), changedSource.getClass2FinishQty());
        assertEquals(null, changedSource.getClass3PlanQty());
        assertEquals(Long.valueOf(5L), changedSource.getTaskVersion());

        ArgumentCaptor<TcScheduleResult> inserted = ArgumentCaptor.forClass(TcScheduleResult.class);
        verify(this.scheduleResultMapper).insert(inserted.capture());
        TcScheduleResult target = inserted.getValue();
        assertEquals("TC02", target.getMachineCode());
        assertEquals(null, target.getClass1PlanQty());
        assertEquals(null, target.getClass2PlanQty());
        assertEquals(new BigDecimal("300"), target.getClass3PlanQty());
    }

    /**
     * 创建待测滚动服务。
     *
     * @return 胎侧人工滚动服务
     */
    private TcManualInsertRollingService service() {
        return new TcManualInsertRollingService(this.scheduleResultMapper,
                this.scheduleUnplannedMapper, this.scheduleResultExplainMapper, this.machineRuleValidator);
    }

    /**
     * 构造排程结果。
     *
     * @param id 结果 ID
     * @param machineCode 机台编码
     * @param sidewallCode 胎侧编码
     * @param shiftOrder 班次顺序
     * @param sequence 班内顺序
     * @param planQty 计划量
     * @param finishQty 完成量
     * @return 排程结果
     */
    private TcScheduleResult result(Long id, String machineCode, String sidewallCode, int shiftOrder,
                                    int sequence, String planQty, String finishQty) {
        TcScheduleResult result = new TcScheduleResult();
        result.setId(id);
        result.setFactoryCode("116");
        result.setBatchNo("TC202607150001");
        result.setScheduleDate(Date.valueOf(LocalDate.of(2026, 7, 15)));
        result.setMachineCode(machineCode);
        result.setSidewallCode(sidewallCode);
        result.setConstructionVersion("V1");
        result.setGlueCode("G-" + sidewallCode);
        result.setBaseGlueCode("BG-" + sidewallCode);
        result.setMouthPlateCode("M-" + sidewallCode);
        result.setReleaseStatus("0");
        result.setDataSource("AUTO");
        result.setTaskVersion(0L);
        result.setFieldValueByFieldName(String.format("class%dSequence", shiftOrder), sequence);
        result.setFieldValueByFieldName(String.format("class%dPlanQty", shiftOrder), new BigDecimal(planQty));
        if (finishQty != null) {
            result.setFieldValueByFieldName(String.format("class%dFinishQty", shiftOrder), new BigDecimal(finishQty));
        }
        return result;
    }

}
