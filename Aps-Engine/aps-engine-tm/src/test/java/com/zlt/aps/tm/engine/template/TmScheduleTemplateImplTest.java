package com.zlt.aps.tm.engine.template;

import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.IScheduleProcessLogger;
import com.zlt.aps.common.engine.schedule.ScheduleChainChangeResult;
import com.zlt.aps.common.engine.schedule.ScheduleRuleResult;
import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleResponseVo;
import com.zlt.aps.tm.engine.domain.TmRuleTrace;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 胎面排程模板实现测试。
 *
 * <p>验证模板只负责编排步骤，并按第16章固定顺序调用各步骤服务。</p>
 */
public class TmScheduleTemplateImplTest {

    /**
     * 测试内容：验证胎面排程模板按设计顺序调用六个步骤。
     * 测试场景：为初始化、库存预测、计划计算、排序、机台分配、快照落库分别传入记录调用顺序的步骤实现。
     * 预期结果：调用顺序固定为 bootstrap、inventory、plan、sort、assign、snapshot，响应保留上下文批次号。
     */
    @Test
    public void executeShouldCallStepsInDesignedOrder() {
        List<String> calls = new ArrayList<>();
        TmScheduleTemplateImpl template = new TmScheduleTemplateImpl(
                context -> calls.add("bootstrap"),
                context -> calls.add("inventory"),
                context -> calls.add("plan"),
                context -> calls.add("sort"),
                context -> calls.add("assign"),
                context -> calls.add("snapshot"),
                null
        );
        TmScheduleContext context = new TmScheduleContext();
        context.setBatchNo("BATCH-001");
        context.setTraceId("TRACE-001");

        TmAutoScheduleResponseVo response = template.execute(context);

        assertEquals(Arrays.asList("bootstrap", "inventory", "plan", "sort", "assign", "snapshot"), calls);
        assertTrue(response.getSuccess());
        assertEquals("BATCH-001", response.getBatchNo());
    }

    /**
     * 测试内容：验证模板执行上下文不能为空。
     * 测试场景：调用模板 execute 时传入 null。
     * 预期结果：抛出业务异常，六步服务均不执行。
     */
    @Test(expected = ServiceException.class)
    public void executeShouldRejectNullContext() {
        TmScheduleTemplateImpl template = new TmScheduleTemplateImpl(
                context -> {
                },
                context -> {
                },
                context -> {
                },
                context -> {
                },
                context -> {
                },
                context -> {
                },
                null
        );

        template.execute(null);
    }

    /**
     * 测试内容：验证模板执行过程中产生的规则追溯会保留到上下文。
     * 测试场景：机台分配步骤向任务业务键写入 MACHINE_FILTER 规则命中信息。
     * 预期结果：模板执行完成后，ruleTraceMap 中存在对应业务键，解释 JSON 包含 MACHINE_FILTER。
     */
    @Test
    public void executeShouldKeepRuleTraceForSnapshotExplain() {
        TmTaskDraft task = new TmTaskDraft();
        task.setOrderNo("ORD-TRACE");
        task.setTreadCode("TR-TRACE");
        task.setGlueCode("GL-TRACE");
        task.setMouthPlateCode("MP-TRACE");
        TmScheduleContext context = new TmScheduleContext();
        context.setBatchNo("BATCH-TRACE");
        context.setTraceId("TRACE-TRACE");

        TmScheduleTemplateImpl template = new TmScheduleTemplateImpl(
                stepContext -> stepContext.getTaskDraftList().add(task),
                stepContext -> {
                },
                stepContext -> {
                },
                stepContext -> {
                },
                stepContext -> {
                    TmRuleTrace trace = new TmRuleTrace();
                    trace.addRuleHit("MACHINE_FILTER", "PASS", "TM01");
                    stepContext.getRuleTraceMap().put(task.getBusinessKey(), trace);
                },
                stepContext -> {
                },
                null
        );

        template.execute(context);

        assertTrue(context.getRuleTraceMap().containsKey(task.getBusinessKey()));
        assertTrue(context.getRuleTraceMap().get(task.getBusinessKey()).toExplainJson().contains("MACHINE_FILTER"));
    }

    /**
     * 测试内容：验证模板会记录步骤开始和结束摘要。
     * 测试场景：传入测试用过程日志实现，执行时新增 1 条任务并生成 1 条快照。
     * 预期结果：开始摘要包含工厂和排程日期，结束摘要包含任务数量和快照数量。
     */
    @Test
    public void executeShouldLogStepInputAndOutputSummary() {
        RecordingProcessLogger logger = new RecordingProcessLogger();
        TmTaskDraft task = new TmTaskDraft();
        task.setOrderNo("ORD-001");
        task.setTreadCode("TR-001");
        task.setPlanQty(BigDecimal.TEN);
        TmScheduleContext context = new TmScheduleContext();
        context.setFactoryCode("116");
        context.setScheduleDate(DateUtil.parseDate("2026-06-18"));
        context.setBatchNo("BATCH-001");
        context.setTraceId("TRACE-001");

        TmScheduleTemplateImpl template = new TmScheduleTemplateImpl(
                stepContext -> stepContext.getTaskDraftList().add(task),
                stepContext -> {
                },
                stepContext -> {
                },
                stepContext -> {
                },
                stepContext -> {
                },
                stepContext -> stepContext.getSnapshotMap().put(task.getBusinessKey(), null),
                logger
        );

        template.execute(context);

        assertTrue(logger.startSummaries.stream().anyMatch(summary -> summary.contains("factoryCode=116")
                && summary.contains("scheduleDate=2026-06-18")));
        assertTrue(logger.endSummaries.stream().anyMatch(summary -> summary.contains("taskCount=1")));
        assertTrue(logger.endSummaries.stream().anyMatch(summary -> summary.contains("snapshotCount=1")));
    }

    /**
     * 记录步骤摘要的测试日志实现。
     */
    private static class RecordingProcessLogger implements IScheduleProcessLogger<TmScheduleContext> {

        private final List<String> startSummaries = new ArrayList<>();

        private final List<String> endSummaries = new ArrayList<>();

        @Override
        public void logStepStart(TmScheduleContext context, String stepCode, String inputSummary) {
            startSummaries.add(inputSummary);
        }

        @Override
        public void logStepEnd(TmScheduleContext context, String stepCode, String outputSummary) {
            endSummaries.add(outputSummary);
        }

        @Override
        public void logRuleResult(TmScheduleContext context, String ruleCode, ScheduleRuleResult result) {
        }

        @Override
        public void logChainChange(TmScheduleContext context, ScheduleChainChangeResult<?> result) {
        }
    }
}
