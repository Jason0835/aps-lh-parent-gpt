package com.zlt.aps.tc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.domain.entity.TcScheduleUnplanned;
import com.zlt.aps.tc.mapper.TcScheduleResultExplainMapper;
import com.zlt.aps.tc.mapper.TcScheduleResultMapper;
import com.zlt.aps.tc.mapper.TcScheduleUnplannedMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 胎侧人工调整 JSON 场景测试。
 *
 * <p>测试以 JSON 驱动真实横表滚动服务，外部数据库、Redis 和 MES 均由 Mock 隔离。</p>
 */
@RunWith(MockitoJUnitRunner.class)
public class TcManualScheduleJsonScenarioTest {

    @Mock
    private TcScheduleResultMapper scheduleResultMapper;

    @Mock
    private TcManualMachineRuleValidator machineRuleValidator;

    @Mock
    private TcScheduleUnplannedMapper scheduleUnplannedMapper;

    @Mock
    private TcScheduleResultExplainMapper scheduleResultExplainMapper;

    /**
     * 执行人工插单和调量的全部 JSON 场景。
     */
    @Test
    public void shouldPassAllManualScheduleJsonScenarios() {
        TcManualScheduleJsonScenarioLoader loader = new TcManualScheduleJsonScenarioLoader();
        assertNotNull(loader.loadAll());
        loader.loadAll().forEach(scenario -> {
            this.resetMocks();
            if ("INSERT".equals(scenario.getOperation())) {
                this.executeInsertScenario(scenario);
            } else if ("CHANGE_QTY".equals(scenario.getOperation())) {
                this.executeChangeQtyScenario(scenario);
            } else {
                fail("不支持的胎侧人工调整场景类型：" + scenario.getOperation());
            }
        });
    }

    /**
     * 执行插单场景并校验横表计划量及第六班溢出。
     *
     * @param scenario 插单场景
     */
    private void executeInsertScenario(TcManualScheduleJsonScenario scenario) {
        Map<String, Object> input = scenario.getInput();
        Map<String, Object> expected = scenario.getExpected();
        TcScheduleResult insertResult = this.baseResult(null, "SW-INSERT");
        TcScheduleResult occupiedResult = null;
        if (input.containsKey("class1PlanQty")) {
            insertResult.setClass1PlanQty(this.decimal(input, "class1PlanQty"));
            insertResult.setClass1Sequence(this.integer(input, "class1Sequence"));
            insertResult.setClass2PlanQty(this.decimal(input, "class2PlanQty"));
            insertResult.setClass2Sequence(this.integer(input, "class2Sequence"));
        } else {
            int shiftOrder = this.integer(input, "shiftOrder");
            insertResult.setFieldValueByFieldName(String.format("class%dPlanQty", shiftOrder),
                    this.decimal(input, "planQty"));
            insertResult.setFieldValueByFieldName(String.format("class%dSequence", shiftOrder),
                    this.integer(input, "sequence"));
            occupiedResult = this.baseResult(1L, "SW-OCCUPIED");
            occupiedResult.setFieldValueByFieldName(String.format("class%dPlanQty", shiftOrder),
                    this.decimal(input, "occupiedQty"));
            occupiedResult.setFieldValueByFieldName(String.format("class%dSequence", shiftOrder), 1);
        }
        when(this.scheduleResultMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(
                occupiedResult == null ? Collections.emptyList() : Collections.singletonList(occupiedResult));
        when(this.machineRuleValidator.resolveRollingCapacity(any(TcScheduleResult.class),
                any(String.class), any(Integer.class))).thenReturn(this.decimal(input, "capacity"));
        when(this.scheduleResultMapper.insert(any(TcScheduleResult.class))).thenReturn(1);
        when(this.scheduleResultMapper.updateById(any(TcScheduleResult.class))).thenReturn(1);
        when(this.scheduleUnplannedMapper.insert(any(TcScheduleUnplanned.class))).thenReturn(1);
        when(this.scheduleResultExplainMapper.insert(any())).thenReturn(1);

        this.service().insertAndRoll(insertResult);

        ArgumentCaptor<TcScheduleResult> insertedCaptor = ArgumentCaptor.forClass(TcScheduleResult.class);
        verify(this.scheduleResultMapper).insert(insertedCaptor.capture());
        TcScheduleResult inserted = insertedCaptor.getValue();
        this.assertDecimalIfPresent(expected, "class1PlanQty", inserted.getClass1PlanQty(), scenario.getCaseName());
        this.assertDecimalIfPresent(expected, "class2PlanQty", inserted.getClass2PlanQty(), scenario.getCaseName());
        this.assertDecimalIfPresent(expected, "class6PlanQty", inserted.getClass6PlanQty(), scenario.getCaseName());
        this.assertObjectIfPresent(expected, "releaseStatus", inserted.getReleaseStatus(), scenario.getCaseName());
        this.assertLongIfPresent(expected, "taskVersion", inserted.getTaskVersion(), scenario.getCaseName());
        if (expected.containsKey("unplannedQty")) {
            ArgumentCaptor<TcScheduleUnplanned> unplannedCaptor = ArgumentCaptor.forClass(TcScheduleUnplanned.class);
            verify(this.scheduleUnplannedMapper).insert(unplannedCaptor.capture());
            assertEquals(scenario.getCaseName(), this.decimal(expected, "unplannedQty"),
                    unplannedCaptor.getValue().getPlanQty());
            assertEquals(scenario.getCaseName(), String.valueOf(expected.get("unplannedReasonCode")),
                    unplannedCaptor.getValue().getUnplannedReasonCode());
        } else {
            verify(this.scheduleUnplannedMapper, never()).insert(any(TcScheduleUnplanned.class));
        }
    }

    /**
     * 执行调量场景并校验完成量、发布状态和行版本规则。
     *
     * @param scenario 调量场景
     */
    private void executeChangeQtyScenario(TcManualScheduleJsonScenario scenario) {
        Map<String, Object> input = scenario.getInput();
        Map<String, Object> expected = scenario.getExpected();
        TcScheduleResult current = this.baseResult(1L, "SW-CHANGE");
        current.setClass1PlanQty(this.decimal(input, "currentPlanQty"));
        current.setClass1FinishQty(this.decimal(input, "finishQty"));
        current.setClass1Sequence(1);
        current.setReleaseStatus(String.valueOf(input.get("releaseStatus")));
        current.setTaskVersion(this.longValue(input, "taskVersion"));
        TcScheduleResult changeResult = new TcScheduleResult();
        changeResult.setId(current.getId());
        changeResult.setClass1PlanQty(this.decimal(input, "newPlanQty"));
        when(this.scheduleResultMapper.selectById(current.getId())).thenReturn(current);

        if (Boolean.TRUE.equals(expected.get("error"))) {
            try {
                this.service().changeQtyAndRoll(changeResult);
                fail("预期场景失败：" + scenario.getCaseName());
            } catch (ServiceException exception) {
                assertNotNull(scenario.getCaseName(), exception.getMessage());
            }
            return;
        }

        when(this.scheduleResultMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(current));
        when(this.machineRuleValidator.resolveRollingCapacity(any(TcScheduleResult.class),
                any(String.class), any(Integer.class))).thenReturn(this.decimal(input, "capacity"));
        when(this.scheduleResultMapper.updateById(any(TcScheduleResult.class))).thenReturn(1);
        this.service().changeQtyAndRoll(changeResult);

        ArgumentCaptor<TcScheduleResult> updatedCaptor = ArgumentCaptor.forClass(TcScheduleResult.class);
        verify(this.scheduleResultMapper).updateById(updatedCaptor.capture());
        TcScheduleResult updated = updatedCaptor.getValue();
        assertEquals(scenario.getCaseName(), this.decimal(expected, "class1PlanQty"), updated.getClass1PlanQty());
        assertEquals(scenario.getCaseName(), String.valueOf(expected.get("releaseStatus")),
                updated.getReleaseStatus());
        assertEquals(scenario.getCaseName(), this.longValue(expected, "taskVersion"), updated.getTaskVersion());
    }

    /**
     * 清理场景间的 Mock 调用和桩数据。
     */
    private void resetMocks() {
        reset(this.scheduleResultMapper, this.scheduleUnplannedMapper,
                this.scheduleResultExplainMapper, this.machineRuleValidator);
    }

    /**
     * 创建待测横表滚动服务。
     *
     * @return 横表滚动服务
     */
    private TcManualInsertRollingService service() {
        return new TcManualInsertRollingService(this.scheduleResultMapper,
                this.scheduleUnplannedMapper, this.scheduleResultExplainMapper, this.machineRuleValidator);
    }

    /**
     * 创建排程结果基础字段。
     *
     * @param resultId 结果 ID
     * @param sidewallCode 胎侧编码
     * @return 排程结果
     */
    private TcScheduleResult baseResult(Long resultId, String sidewallCode) {
        TcScheduleResult result = new TcScheduleResult();
        result.setId(resultId);
        result.setFactoryCode("116");
        result.setScheduleDate(Date.valueOf(LocalDate.of(2026, 7, 15)));
        result.setBatchNo("TC-MANUAL-JSON");
        result.setMachineCode("TC01");
        result.setSidewallCode(sidewallCode);
        result.setConstructionVersion("V1");
        result.setGlueCode("G1");
        result.setBaseGlueCode("BG1");
        result.setMouthPlateCode("MP1");
        result.setReleaseStatus("0");
        result.setTaskVersion(0L);
        return result;
    }

    /**
     * 读取十进制场景字段。
     *
     * @param values 字段集合
     * @param key 字段名
     * @return 十进制值，字段不存在时返回 null
     */
    private BigDecimal decimal(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? null : new BigDecimal(String.valueOf(value));
    }

    /**
     * 读取整数字段。
     *
     * @param values 字段集合
     * @param key 字段名
     * @return 整数值
     */
    private Integer integer(Map<String, Object> values, String key) {
        return Integer.valueOf(String.valueOf(values.get(key)));
    }

    /**
     * 读取长整数字段。
     *
     * @param values 字段集合
     * @param key 字段名
     * @return 长整数值
     */
    private Long longValue(Map<String, Object> values, String key) {
        return Long.valueOf(String.valueOf(values.get(key)));
    }

    /**
     * 期望中存在字段时校验十进制值。
     *
     * @param expected 期望集合
     * @param key 字段名
     * @param actual 实际值
     * @param caseName 场景编码
     */
    private void assertDecimalIfPresent(Map<String, Object> expected, String key, BigDecimal actual,
                                        String caseName) {
        if (expected.containsKey(key)) {
            assertEquals(caseName, this.decimal(expected, key), actual);
        }
    }

    /**
     * 期望中存在字段时校验普通值。
     *
     * @param expected 期望集合
     * @param key 字段名
     * @param actual 实际值
     * @param caseName 场景编码
     */
    private void assertObjectIfPresent(Map<String, Object> expected, String key, Object actual,
                                       String caseName) {
        if (expected.containsKey(key)) {
            assertEquals(caseName, String.valueOf(expected.get(key)), actual);
        }
    }

    /**
     * 期望中存在字段时校验长整型值。
     *
     * @param expected 期望集合
     * @param key 字段名
     * @param actual 实际值
     * @param caseName 场景编码
     */
    private void assertLongIfPresent(Map<String, Object> expected, String key, Long actual,
                                     String caseName) {
        if (expected.containsKey(key)) {
            assertEquals(caseName, this.longValue(expected, key), actual);
        }
    }
}
