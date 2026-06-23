package com.zlt.aps.tm.autoplan;

import cn.hutool.core.util.StrUtil;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResultExplain;
import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleResponseVo;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 胎面自动排程 JSON 场景断言助手。
 *
 * <p>封装响应、排程结果和解释信息断言，使每个业务场景测试保持 Given/When/Then 清晰结构。</p>
 */
public class TmAutoPlanAssertHelper {

    /**
     * 执行完整自动排程入口并断言响应和落库结果。
     *
     * @param mockContext Mock 运行上下文
     */
    public void executeAndAssert(TmAutoPlanMockFactory.MockContext mockContext) {
        TmAutoPlanScenario scenario = mockContext.getScenario();
        TmAutoPlanExpectedResult expected = scenario.getExpected();
        if (Boolean.TRUE.equals(scenario.getSkipScenarioRun())) {
            return;
        }
        try {
            TmAutoScheduleResponseVo response = mockContext.getService().tmAutoPlan(scenario.getAutoPlanRequest());
            if (Boolean.FALSE.equals(expected.getSuccess())) {
                fail("场景应拒绝自动排程，但实际执行成功：" + scenario.getCaseName());
            }
            assertResponse(scenario, response);
            assertPersistedResults(scenario, mockContext.getInsertedResults());
            assertPersistedExplains(scenario, mockContext.getInsertedExplains());
            assertPersistSummary(scenario, mockContext.getLastContext());
        } catch (RuntimeException ex) {
            if (!Boolean.FALSE.equals(expected.getSuccess())) {
                throw ex;
            }
            assertRejectMessage(scenario, ex);
        }
    }

    /**
     * 断言数据加载后的任务中间结果。
     *
     * @param scenario 场景
     * @param context  自动排程上下文
     */
    public void assertLoadedContext(TmAutoPlanScenario scenario, TmScheduleContext context) {
        assertNotNull("上下文不能为空：" + scenario.getCaseName(), context);
        assertEquals(scenario.getAutoPlanRequest().getFactoryCode(), context.getFactoryCode());
        assertEquals(scenario.getAutoPlanRequest().getScheduleDate(), context.getScheduleDate());
        if (scenario.getExpected().getResultCount() != null && !Boolean.TRUE.equals(scenario.getSkipScenarioRun())) {
            assertTrue("任务数量应不少于归并后的最终结果数量：" + scenario.getCaseName(),
                    context.getTaskDraftList().size() >= scenario.getExpected().getResultCount());
        }
    }

    /**
     * 按胎面和班次查找任务草稿。
     *
     * @param context    上下文
     * @param treadCode  胎面编码
     * @param shiftOrder 班次
     * @return 任务草稿
     */
    public TmTaskDraft findTask(TmScheduleContext context, String treadCode, Integer shiftOrder) {
        for (TmTaskDraft task : context.getTaskDraftList()) {
            if (treadCode.equals(task.getTreadCode()) && shiftOrder.equals(task.getShiftOrder())) {
                return task;
            }
        }
        fail("未找到任务，treadCode=" + treadCode + "，shiftOrder=" + shiftOrder);
        return null;
    }

    private void assertResponse(TmAutoPlanScenario scenario, TmAutoScheduleResponseVo response) {
        TmAutoPlanExpectedResult expected = scenario.getExpected();
        assertEquals(Boolean.TRUE, response.getSuccess());
        assertNotNull("batchNo 不能为空：" + scenario.getCaseName(), response.getBatchNo());
        assertNotNull("traceId 不能为空：" + scenario.getCaseName(), response.getTraceId());
        if (expected.getConfirmRequired() != null) {
            assertEquals(expected.getConfirmRequired(), response.getConfirmRequired());
        }
        if (expected.getMessageContains() != null) {
            assertTrue(response.getMessage().contains(expected.getMessageContains()));
        }
        if (expected.getResultCount() != null) {
            assertEquals(expected.getResultCount(), response.getResultCount());
        }
        if (expected.getUnassignedCount() != null) {
            assertEquals(expected.getUnassignedCount(), response.getUnplannedCount());
        }
    }

    private void assertRejectMessage(TmAutoPlanScenario scenario, RuntimeException ex) {
        String messageContains = scenario.getExpected().getRejectMessageContains();
        assertTrue("拒绝消息不符合预期：" + ex.getMessage(),
                messageContains == null || ex.getMessage().contains(messageContains));
    }

    private void assertPersistedResults(TmAutoPlanScenario scenario, List<TmScheduleResult> resultList) {
        TmAutoPlanExpectedResult expected = scenario.getExpected();
        if (expected.getResultCount() != null) {
            assertEquals("结果表写入数量不符合预期：" + scenario.getCaseName(),
                    expected.getResultCount().intValue(), resultList.size());
        }
        if (expected.getAssignedCount() != null) {
            long assignedCount = resultList.stream().filter(item -> StrUtil.isNotBlank(item.getMachineCode())).count();
            assertEquals(expected.getAssignedCount().intValue(), (int) assignedCount);
        }
        if (expected.getUnassignedCount() != null) {
            long unassignedCount = resultList.stream().filter(item -> StrUtil.isBlank(item.getMachineCode())).count();
            assertEquals(expected.getUnassignedCount().intValue(), (int) unassignedCount);
        }
        for (TmScheduleResult result : resultList) {
            assertTrue("自动排程结果工单号格式不符合胎面唯一键要求：" + result.getOrderNo(),
                    result.getOrderNo() != null && result.getOrderNo().matches("TM\\d{17}-\\d{4}"));
        }
        for (TmAutoPlanExpectedResult.ExpectedScheduleResult expectedResult : expected.getExpectedResults()) {
            TmScheduleResult result = findResult(resultList, expectedResult);
            assertEquals(expectedResult.getMachineCode(), result.getMachineCode());
            if (expectedResult.getPlanQty() != null) {
                assertBigDecimalEquals(expectedResult.getPlanQty(), readPlanQty(result, expectedResult.getShiftOrder()));
            }
            if (expectedResult.getSequence() != null) {
                assertEquals(expectedResult.getSequence(), readSequence(result, expectedResult.getShiftOrder()));
            }
        }
    }

    private void assertPersistedExplains(TmAutoPlanScenario scenario, List<TmScheduleResultExplain> explainList) {
        TmAutoPlanExpectedResult expected = scenario.getExpected();
        if (expected.getExplainCount() != null) {
            assertEquals("解释表写入数量不符合预期：" + scenario.getCaseName(),
                    expected.getExplainCount().intValue(), explainList.size());
        }
        for (TmAutoPlanExpectedResult.ExpectedExplain expectedExplain : expected.getExpectedExplains()) {
            TmScheduleResultExplain explain = findExplain(explainList, expectedExplain);
            if (expectedExplain.getFinalPlanQty() != null) {
                assertBigDecimalEquals(expectedExplain.getFinalPlanQty(), explain.getFinalPlanQty());
            }
            if (expectedExplain.getUnplannedReasonCode() != null) {
                assertEquals(expectedExplain.getUnplannedReasonCode(), explain.getUnplannedReasonCode());
            }
            if (expectedExplain.getRuleHitContains() != null) {
                assertTrue(explain.getRuleHitJson().contains(expectedExplain.getRuleHitContains()));
            }
            if (expectedExplain.getCandidateMachineContains() != null) {
                assertTrue(explain.getCandidateMachineJson() != null
                        && explain.getCandidateMachineJson().contains(expectedExplain.getCandidateMachineContains()));
            }
            if (expectedExplain.getMachineSelectReasonContains() != null) {
                assertTrue(explain.getMachineSelectReason() != null
                        && explain.getMachineSelectReason().contains(expectedExplain.getMachineSelectReasonContains()));
            }
            if (expectedExplain.getAssignStatus() != null) {
                assertEquals(expectedExplain.getAssignStatus(), explain.getAssignStatus());
            }
            if (expectedExplain.getSelectedMachineScore() != null) {
                assertBigDecimalEquals(expectedExplain.getSelectedMachineScore(), explain.getSelectedMachineScore());
            }
        }
    }

    private void assertPersistSummary(TmAutoPlanScenario scenario, TmScheduleContext context) {
        Integer expectedErrorCount = scenario.getExpected().getErrorCount();
        if (expectedErrorCount == null || context == null || context.getPersistResult() == null) {
            return;
        }
        assertEquals(expectedErrorCount.intValue(), context.getPersistResult().getErrorCount());
    }

    private TmScheduleResult findResult(List<TmScheduleResult> resultList,
                                        TmAutoPlanExpectedResult.ExpectedScheduleResult expectedResult) {
        for (TmScheduleResult result : resultList) {
            if (matches(result, expectedResult)) {
                return result;
            }
        }
        fail("未找到期望排程结果：" + expectedResult);
        return null;
    }

    private boolean matches(TmScheduleResult result, TmAutoPlanExpectedResult.ExpectedScheduleResult expectedResult) {
        if (StrUtil.isNotBlank(expectedResult.getTreadCode()) && !expectedResult.getTreadCode().equals(result.getTreadCode())) {
            return false;
        }
        if (expectedResult.getMachineCode() != null && !expectedResult.getMachineCode().equals(result.getMachineCode())) {
            return false;
        }
        return true;
    }

    private TmScheduleResultExplain findExplain(List<TmScheduleResultExplain> explainList,
                                                TmAutoPlanExpectedResult.ExpectedExplain expectedExplain) {
        for (TmScheduleResultExplain explain : explainList) {
            if (expectedExplain.getTreadCode() == null || expectedExplain.getTreadCode().equals(explain.getRemark())) {
                return explain;
            }
        }
        if (!explainList.isEmpty()) {
            return explainList.get(0);
        }
        fail("未找到期望解释信息：" + expectedExplain);
        return null;
    }

    private BigDecimal readPlanQty(TmScheduleResult result, Integer shiftOrder) {
        if (Integer.valueOf(1).equals(shiftOrder)) {
            return result.getClass1PlanQty();
        }
        if (Integer.valueOf(2).equals(shiftOrder)) {
            return result.getClass2PlanQty();
        }
        if (Integer.valueOf(3).equals(shiftOrder)) {
            return result.getClass3PlanQty();
        }
        if (Integer.valueOf(4).equals(shiftOrder)) {
            return result.getClass4PlanQty();
        }
        if (Integer.valueOf(5).equals(shiftOrder)) {
            return result.getClass5PlanQty();
        }
        return result.getClass6PlanQty();
    }

    private Integer readSequence(TmScheduleResult result, Integer shiftOrder) {
        if (Integer.valueOf(1).equals(shiftOrder)) {
            return result.getClass1Sequence();
        }
        if (Integer.valueOf(2).equals(shiftOrder)) {
            return result.getClass2Sequence();
        }
        if (Integer.valueOf(3).equals(shiftOrder)) {
            return result.getClass3Sequence();
        }
        if (Integer.valueOf(4).equals(shiftOrder)) {
            return result.getClass4Sequence();
        }
        if (Integer.valueOf(5).equals(shiftOrder)) {
            return result.getClass5Sequence();
        }
        return result.getClass6Sequence();
    }

    private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        assertNotNull("实际数值不能为空", actual);
        assertEquals("BigDecimal 数值不一致", 0, expected.compareTo(actual));
    }
}
