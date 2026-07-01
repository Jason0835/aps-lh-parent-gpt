package com.zlt.aps.tm.engine.strategy;

import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.ScheduleRuleResult;
import com.zlt.aps.common.engine.schedule.ScheduleScoreResult;
import com.zlt.aps.tm.engine.domain.*;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 胎面自动排程业务规则策略测试。
 *
 * <p>使用具体模拟数据验证 6 点库存、库存最低保证班数、计划量分量和机台评分口径。</p>
 */
public class TmBusinessRuleStrategyTest {

    /**
     * 测试内容：验证当前班库存充足但保证范围存在缺口时，需求量取保证范围缺口。
     * 测试场景：当前班需求 600、最低保障需求 1100、滚动库存 1000。
     * 预期结果：当前班缺口为 0，库存保证缺口为 100，最终需求量为 100。
     */
    @Test
    public void demandShouldUseGuardStockGapWhenCurrentShiftIsCovered() {
        // 准备需求量输入，构造当前班已被库存覆盖但保证范围仍有缺口的场景。
        TmDemandQtyInput input = new TmDemandQtyInput();
        input.setCurrentShiftDemandQty(new BigDecimal("600"));
        input.setGuardDemandQty(new BigDecimal("1100"));
        input.setRollingStockQty(new BigDecimal("1000"));
        input.setGuardRangeHours(new BigDecimal("16"));

        // 执行最低库存保障需求量策略。
        TmDemandQtyResult result = new TmGuardDemandQtyStrategy().calculate(input, null);

        // 断言最终需求量取保证范围缺口，而不是当前班毛需求。
        assertEquals(new BigDecimal("100"), result.getStockGapQty());
        assertEquals(new BigDecimal("100"), result.getDemandQty());
        assertEquals(Integer.valueOf(2), result.getGuardShiftCount());
    }

    /**
     * 测试内容：验证当前班缺口大于保证范围缺口时，需求量取当前班缺口。
     * 测试场景：当前班需求 600、最低保障需求 500、滚动库存 300。
     * 预期结果：当前班缺口 300 大于库存保证缺口 200，最终需求量为 300。
     */
    @Test
    public void demandShouldUseCurrentShiftGapWhenItIsGreaterThanGuardGap() {
        // 准备当前班缺口大于保证范围缺口的数据。
        TmDemandQtyInput input = new TmDemandQtyInput();
        input.setCurrentShiftDemandQty(new BigDecimal("600"));
        input.setGuardDemandQty(new BigDecimal("500"));
        input.setRollingStockQty(new BigDecimal("300"));
        input.setGuardRangeHours(new BigDecimal("16"));

        // 执行需求量策略，验证会补足更大的当前班缺口。
        TmDemandQtyResult result = new TmGuardDemandQtyStrategy().calculate(input, null);

        // 断言最终需求量使用当前班库存缺口。
        assertEquals(new BigDecimal("200"), result.getStockGapQty());
        assertEquals(new BigDecimal("300"), result.getDemandQty());
    }

    /**
     * 测试内容：验证库存已覆盖保障需求且当前班无需求时不额外排产。
     * 测试场景：当前班需求为 0，滚动库存 1000 大于保障需求 500。
     * 预期结果：库存缺口和最终需求量都为 0。
     */
    @Test
    public void demandShouldReturnZeroWhenStockCoversGuardDemandAndCurrentDemandIsZero() {
        // 准备库存充足且当前班需求为 0 的输入。
        TmDemandQtyInput input = new TmDemandQtyInput();
        input.setCurrentShiftDemandQty(BigDecimal.ZERO);
        input.setGuardDemandQty(new BigDecimal("500"));
        input.setRollingStockQty(new BigDecimal("1000"));
        input.setGuardRangeHours(new BigDecimal("16"));

        // 执行需求量策略。
        TmDemandQtyResult result = new TmGuardDemandQtyStrategy().calculate(input, null);

        // 断言不产生额外计划需求。
        assertEquals(BigDecimal.ZERO, result.getStockGapQty());
        assertEquals(BigDecimal.ZERO, result.getDemandQty());
    }

    /**
     * 测试内容：验证未来需求为 0 时不会发生除零异常。
     * 测试场景：当前班需求、保障需求都为 0。
     * 预期结果：需求量为 0，供应时长为空。
     */
    @Test
    public void demandShouldNotDivideByZeroWhenFutureDemandIsZero() {
        // 准备全需求为 0 的输入，覆盖供应时长计算的除零边界。
        TmDemandQtyInput input = new TmDemandQtyInput();
        input.setCurrentShiftDemandQty(BigDecimal.ZERO);
        input.setGuardDemandQty(BigDecimal.ZERO);
        input.setRollingStockQty(new BigDecimal("1000"));
        input.setGuardRangeHours(new BigDecimal("16"));

        // 执行需求量策略。
        TmDemandQtyResult result = new TmGuardDemandQtyStrategy().calculate(input, null);

        // 断言需求为 0 且供应时长不计算。
        assertEquals(BigDecimal.ZERO, result.getDemandQty());
        assertNull(result.getSupplyHours());
    }

    /**
     * 测试内容：验证需求量策略入参为空时直接拒绝。
     * 测试场景：调用最低库存保障需求量策略时传入 null。
     * 预期结果：抛出业务异常，避免后续空指针。
     */
    @Test(expected = ServiceException.class)
    public void demandShouldRejectNullInput() {
        new TmGuardDemandQtyStrategy().calculate(null, null);
    }

    /**
     * 测试内容：验证计划量策略按基础需求、最小起排、尾数取整、工装限制顺序计算。
     * 测试场景：基础需求 100，最小起排补到 450，卷曲长度向上取整到 600，工装上限压到 400。
     * 预期结果：各调整分量和最终计划量符合当前策略顺序。
     */
    @Test
    public void planShouldApplyToolMinRoundAndCapacityInOrder() {
        // 准备先补最小起排和整卷，再由工装硬上限压缩的任务。
        TmTaskDraft task = buildPlanTask("100");
        task.setTotalToolQty(new BigDecimal("2"));
        task.setCurlRollLength(new BigDecimal("200"));
        task.setDefaultCurlRollLength(new BigDecimal("180"));
        task.setMinStartQty(new BigDecimal("450"));

        // 准备排程上下文，计划量策略当前不依赖上下文。
        TmScheduleContext context = new TmScheduleContext();

        // 执行默认计划量策略。
        TmPlanQtyResult result = new TmDefaultPlanQtyStrategy().calculate(task, context);

        // 断言先从 100 补到 450，再取整到 600，最后被 400 工装上限压缩。
        assertBigDecimalEquals(new BigDecimal("100"), result.getBaseDemandQty());
        assertBigDecimalEquals(new BigDecimal("350"), result.getMinStartAdjustQty());
        assertBigDecimalEquals(new BigDecimal("150"), result.getTailRoundAdjustQty());
        assertBigDecimalEquals(new BigDecimal("-200"), result.getToolLimitAdjustQty());
        assertBigDecimalEquals(BigDecimal.ZERO, result.getCapacityAdjustQty());
        assertBigDecimalEquals(new BigDecimal("400"), result.getFinalPlanQty());
        assertBigDecimalEquals(new BigDecimal("200"), result.getToolOverflowQty());
    }
    /**
     * 测试内容：验证可用工装限制会压低最终计划量。
     * 测试场景：需求 700 先按 200 卷曲长度取整到 800，但 3 个工装最多支持 600。
     * 预期结果：工装调整量为 -200，工装顺延量为 200，最终计划量为 600。
     */
    @Test
    public void planShouldLimitByAvailableTools() {
        // 准备需求高于可用工装承载量的任务。
        TmTaskDraft task = buildPlanTask("700");
        task.setTotalToolQty(new BigDecimal("3"));
        task.setCurlRollLength(new BigDecimal("200"));

        // 执行计划量策略。
        TmPlanQtyResult result = new TmDefaultPlanQtyStrategy().calculate(task, null);

        // 断言工装限制生效并压低最终计划量。
        assertBigDecimalEquals(new BigDecimal("700"), result.getBaseDemandQty());
        assertBigDecimalEquals(new BigDecimal("-200"), result.getToolLimitAdjustQty());
        assertBigDecimalEquals(new BigDecimal("200"), result.getToolOverflowQty());
        assertBigDecimalEquals(new BigDecimal("600"), result.getFinalPlanQty());
    }

    /**
     * 测试内容：验证正需求低于最小起排量时会补足到最小起排。
     * 测试场景：基础需求 100，最小起排量 450。
     * 预期结果：最小起排调整量为 350，最终计划量为 450。
     */
    @Test
    public void planShouldRaisePositiveDemandToMinStartQty() {
        // 准备低于最小起排量的正需求任务。
        TmTaskDraft task = buildPlanTask("100");
        task.setMinStartQty(new BigDecimal("450"));

        // 执行计划量策略。
        TmPlanQtyResult result = new TmDefaultPlanQtyStrategy().calculate(task, null);

        // 断言低需求被补到最小起排量。
        assertBigDecimalEquals(new BigDecimal("350"), result.getMinStartAdjustQty());
        assertBigDecimalEquals(new BigDecimal("450"), result.getFinalPlanQty());
    }

    /**
     * 测试内容：验证计划量策略不再提前按机台产能压缩。
     * 测试场景：剩余产能 500，机台速度 100，检修 1 小时后可用产能为 400。
     * 预期结果：计划量仍保持 600，产能压缩交由机台分配阶段处理。
     */
    @Test
    public void planShouldNotCapByRemainCapacityBeforeMachineAssign() {
        // 准备需求超过扣减后可用产能的任务。
        TmTaskDraft task = buildPlanTask("600");
        task.setMachineRemainCapacity(new BigDecimal("500"));
        task.setMachineSpeed(new BigDecimal("100"));
        task.setMaintenanceHours(new BigDecimal("1"));

        // 执行计划量策略。
        TmPlanQtyResult result = new TmDefaultPlanQtyStrategy().calculate(task, null);

        // 断言策略阶段不再提前压缩产能，避免丢失后续顺延量。
        assertBigDecimalEquals(BigDecimal.ZERO, result.getCapacityAdjustQty());
        assertBigDecimalEquals(new BigDecimal("600"), result.getFinalPlanQty());
    }

    /**
     * 测试内容：验证工装上限低于最小起排量时，工装限制作为硬上限。
     * 测试场景：基础需求 100，最小起排 450，工装上限 200。
     * 预期结果：最终计划量为 200，低于最小起排的 250 作为工装顺延量。
     */
    @Test
    public void planShouldAllowFinalQtyBelowMinStartWhenToolLimitIsLower() {
        // 准备工装上限低于最小起排量的任务。
        TmTaskDraft task = buildPlanTask("100");
        task.setMinStartQty(new BigDecimal("450"));
        task.setTotalToolQty(BigDecimal.ONE);
        task.setCurlRollLength(new BigDecimal("200"));

        // 执行计划量策略。
        TmPlanQtyResult result = new TmDefaultPlanQtyStrategy().calculate(task, null);

        // 断言工装限制在最小起排和卷数取整之后执行，可压到低于最小起排量。
        assertBigDecimalEquals(new BigDecimal("350"), result.getMinStartAdjustQty());
        assertBigDecimalEquals(new BigDecimal("-400"), result.getToolLimitAdjustQty());
        assertBigDecimalEquals(new BigDecimal("200"), result.getFinalPlanQty());
        assertBigDecimalEquals(new BigDecimal("400"), result.getToolOverflowQty());
    }

    /**
     * 测试内容：验证收尾规格在损耗补偿后受工装上限压缩。
     * 测试场景：收尾余量 10 条，肩长 50，损耗 10%，工装上限 400。
     * 预期结果：收尾量 550 被压到 400，不执行最小起排和卷数取整。
     */
    @Test
    public void planShouldApplyToolLimitAfterTailLossWithoutMinStartAndRound() {
        // 准备收尾规格任务，收尾损耗后超过工装上限。
        TmTaskDraft task = buildPlanTask("600");
        task.setTailFlag("1");
        task.setTailBalanceQty(new BigDecimal("10"));
        task.setTreadShoulderLength(new BigDecimal("50"));
        task.setLossRate(new BigDecimal("10"));
        task.setMinStartQty(new BigDecimal("700"));
        task.setTotalToolQty(new BigDecimal("2"));
        task.setCurlRollLength(new BigDecimal("200"));

        // 执行计划量策略。
        TmPlanQtyResult result = new TmDefaultPlanQtyStrategy().calculate(task, null);

        // 断言收尾损耗后直接受工装硬上限压缩，不再补最小起排和整卷。
        assertBigDecimalEquals(new BigDecimal("50.000000"), result.getLossAddQty());
        assertBigDecimalEquals(BigDecimal.ZERO, result.getMinStartAdjustQty());
        assertBigDecimalEquals(new BigDecimal("-150.000000"), result.getToolLimitAdjustQty());
        assertBigDecimalEquals(new BigDecimal("400"), result.getFinalPlanQty());
        assertBigDecimalEquals(new BigDecimal("150.000000"), result.getToolOverflowQty());
    }
    /**
     * 测试内容：验证计划量策略任务为空时直接拒绝。
     * 测试场景：调用默认计划量策略时传入 null 任务。
     * 预期结果：抛出业务异常，避免生成无业务键计划。
     */
    @Test(expected = ServiceException.class)
    public void planShouldRejectNullTask() {
        new TmDefaultPlanQtyStrategy().calculate(null, null);
    }

    /**
     * 测试内容：验证库存抵扣当前班生产量，库存不足时只抵扣到库存量。
     * 测试场景：当前班需求 600、保证范围需求 600，班初滚动库存 200。
     * 预期结果：库存抵扣 200，基础需求=max(600-200,600-200,0)=400。
     */
    @Test
    public void planShouldDeductStockUpToRemainingWhenStockInsufficient() {
        TmTaskDraft task = new TmTaskDraft();
        task.setTreadCode("TR-DEDUCT-1");
        task.setCurrentShiftDemandQty(new BigDecimal("600"));
        task.setGuardDemandQty(new BigDecimal("600"));
        task.setRollingStockQty(new BigDecimal("200"));

        TmPlanQtyResult result = new TmDefaultPlanQtyStrategy().calculate(task, null);

        assertBigDecimalEquals(new BigDecimal("200"), task.getStockDeductQty());
        assertBigDecimalEquals(new BigDecimal("400"), result.getBaseDemandQty());
    }

    /**
     * 测试内容：验证计划量策略把任务完成后的库存写为交接班预计库存。
     * 测试场景：班初滚动库存 500、当前班需求 600、最终计划量按基础需求得到 100。
     * 预期结果：交接班预计库存=max(500+100-600,0)=0。
     */
    @Test
    public void planShouldWritePlanStockQtyAsShiftHandoverStock() {
        TmTaskDraft task = new TmTaskDraft();
        task.setTreadCode("TR-DEDUCT-2");
        task.setCurrentShiftDemandQty(new BigDecimal("600"));
        task.setGuardDemandQty(new BigDecimal("600"));
        task.setRollingStockQty(new BigDecimal("500"));

        TmPlanQtyResult result = new TmDefaultPlanQtyStrategy().calculate(task, null);

        assertBigDecimalEquals(new BigDecimal("500"), task.getStockDeductQty());
        assertBigDecimalEquals(new BigDecimal("100"), result.getBaseDemandQty());
        assertBigDecimalEquals(BigDecimal.ZERO, task.getPlanStockQty());
    }

    /**
     * 测试内容：验证库存充足时当前班基础需求被抵扣为 0。
     * 测试场景：当前班需求 300、保证范围需求 300，班初滚动库存 1000。
     * 预期结果：库存抵扣 300，基础需求为 0。
     */
    @Test
    public void planShouldDeductAllDemandWhenStockSufficient() {
        TmTaskDraft task = new TmTaskDraft();
        task.setTreadCode("TR-DEDUCT-3");
        task.setCurrentShiftDemandQty(new BigDecimal("300"));
        task.setGuardDemandQty(new BigDecimal("300"));
        task.setRollingStockQty(new BigDecimal("1000"));

        TmPlanQtyResult result = new TmDefaultPlanQtyStrategy().calculate(task, null);

        assertBigDecimalEquals(new BigDecimal("300"), task.getStockDeductQty());
        assertBigDecimalEquals(BigDecimal.ZERO, result.getBaseDemandQty());
        // 库存扣减后交接班预计库存为 700
        assertBigDecimalEquals(new BigDecimal("700"), task.getPlanStockQty());
    }

    /**
     * 测试内容：验证机台未启用时按首个硬约束过滤。
     * 测试场景：候选机台启用状态为 false，其余条件均满足。
     * 预期结果：过滤失败，原因码为 MACHINE_DISABLED。
     */
    @Test
    public void machineFilterShouldRejectDisabledMachineFirst() {
        assertRejectedBy(candidateWith("TM-DISABLED", Boolean.FALSE, null, null, null, null),
                "MACHINE_DISABLED");
    }

    /**
     * 测试内容：验证机台过滤会按规则返回首个失败原因。
     * 测试场景：候选机台启用但剩余产能为 0。
     * 预期结果：过滤失败，候选机台标记为已过滤，原因码为 NO_REMAIN_CAPACITY。
     */
    @Test
    public void machineFilterShouldRejectFirstFailedRule() {
        // 准备剩余产能为 0 的候选机台。
        TmMachineCandidate candidate = enabledCandidate("TM01");
        candidate.setRemainCapacity(BigDecimal.ZERO);

        // 准备机台规则上下文，放入当前任务。
        TmMachineRuleContext ruleContext = new TmMachineRuleContext();
        ruleContext.setTaskDraft(buildTask());

        // 执行默认机台过滤规则。
        ScheduleRuleResult result = new TmDefaultMachineFilterRule().evaluate(candidate, ruleContext);

        // 断言首个失败原因被写回候选机台。
        assertFalse(result.isPassed());
        assertTrue(candidate.getFiltered());
        assertEquals("NO_REMAIN_CAPACITY", candidate.getFilterReasonCode());
    }

    /**
     * 测试内容：验证口型板、胶料、定点和禁排规则都会过滤机台。
     * 测试场景：分别构造四个只命中单一失败规则的候选机台。
     * 预期结果：每个候选机台返回对应失败原因码。
     */
    @Test
    public void machineFilterShouldRejectMouthPlateGlueFixedAndExcludedRules() {
        // 分别断言各类业务过滤条件，避免某一规则缺失或原因码错位。
        assertRejectedBy(candidateWith("TM-MP", null, Boolean.FALSE, null, null, null),
                "MOUTH_PLATE_NOT_MATCH");
        assertRejectedBy(candidateWith("TM-GLUE", null, null, Boolean.FALSE, null, null),
                "GLUE_MACHINE_NOT_MATCH");
        assertRejectedBy(candidateWith("TM-FIX", null, null, null, Boolean.FALSE, null),
                "FIXED_MACHINE_NOT_SELECTED");
        assertRejectedBy(candidateWith("TM-FORBID", null, null, null, null, Boolean.TRUE),
                "FIXED_MACHINE_EXCLUDED");
    }

    /**
     * 测试内容：验证可选过滤标识未显式为 false 时不误过滤。
     * 测试场景：候选机台未设置口型、胶料、定点选择和禁排布尔标识。
     * 预期结果：候选机台通过默认过滤规则。
     */
    @Test
    public void machineFilterShouldPassWhenOptionalMatchFlagsAreBlank() {
        TmMachineCandidate candidate = enabledCandidate("TM-PASS");
        candidate.setMouthPlateMatched(null);
        candidate.setGlueMachineMatched(null);
        candidate.setFixedMachineSelected(null);
        candidate.setFixedMachineExcluded(null);
        TmMachineRuleContext ruleContext = new TmMachineRuleContext();
        ruleContext.setTaskDraft(buildTask());

        ScheduleRuleResult result = new TmDefaultMachineFilterRule().evaluate(candidate, ruleContext);

        assertTrue(result.isPassed());
        assertFalse(candidate.getFiltered());
    }

    /**
     * 测试内容：验证机台评分优先主胶连续，其次基部胶相似。
     * 测试场景：两个候选机台产能相同，一个主胶匹配，一个仅基部胶匹配。
     * 预期结果：主胶匹配机台总分高于基部胶匹配机台。
     */
    @Test
    public void machineScoreShouldPreferMainGlueThenBaseGlueSimilarity() {
        // 准备任务计划量和胶料信息。
        TmTaskDraft task = buildTask();
        task.setPlanQty(new BigDecimal("500"));

        // 准备主胶连续的候选机台。
        TmMachineCandidate mainGlue = enabledCandidate("TM01");
        mainGlue.setRemainCapacity(new BigDecimal("600"));
        mainGlue.setTailMainGlueCode("GL-A");
        mainGlue.setTailBaseGlueCode("BASE-X");

        // 准备仅基部胶相似的候选机台。
        TmMachineCandidate baseGlue = enabledCandidate("TM02");
        baseGlue.setRemainCapacity(new BigDecimal("600"));
        baseGlue.setTailMainGlueCode("GL-B");
        baseGlue.setTailBaseGlueCode("BASE-A");

        TmDefaultMachineScoreStrategy strategy = new TmDefaultMachineScoreStrategy();
        TmMachineRuleContext ruleContext = new TmMachineRuleContext();
        ruleContext.setTaskDraft(task);
        // 分别计算两个候选机台得分。
        ScheduleScoreResult mainScore = strategy.score(mainGlue, ruleContext);
        ScheduleScoreResult baseScore = strategy.score(baseGlue, ruleContext);

        // 断言主胶连续优先级高于基部胶相似。
        assertTrue(mainScore.getTotalScore().compareTo(baseScore.getTotalScore()) > 0);
    }

    /**
     * 测试内容：验证已被过滤的机台不参与评分。
     * 测试场景：候选机台 filtered=true，即使产能和胶料条件都满足。
     * 预期结果：总分为 0，并写入“不参与评分”说明。
     */
    @Test
    public void machineScoreShouldReturnZeroWhenCandidateAlreadyFiltered() {
        TmMachineCandidate candidate = enabledCandidate("TM-FILTERED");
        candidate.setFiltered(Boolean.TRUE);
        candidate.setRemainCapacity(new BigDecimal("600"));
        TmTaskDraft task = buildTask();
        task.setPlanQty(new BigDecimal("500"));
        TmMachineRuleContext ruleContext = new TmMachineRuleContext();
        ruleContext.setTaskDraft(task);

        ScheduleScoreResult scoreResult = new TmDefaultMachineScoreStrategy().score(candidate, ruleContext);

        assertBigDecimalEquals(BigDecimal.ZERO, scoreResult.getTotalScore());
        assertTrue(scoreResult.getDescription().contains("不参与评分"));
    }

    /**
     * 测试内容：验证任务排序按供应时长紧急程度优先，再按胶料分组等规则排序。
     * 测试场景：构造三条不同胶料和供应时长的任务。
     * 预期结果：供应时长最短任务排第一，同胶料任务按规则排在普通胶料任务前。
     */
    @Test
    public void taskSortShouldUseGlueGroupEarliestSupplyHoursBeforeGlueCodeName() {
        // 准备三条排序任务，覆盖紧急供应时长和同胶料分组。
        TmTaskDraft urgentGlueB = sortTask("ORD-3", "GL-B", "BASE-1", "MP-2", "2");
        TmTaskDraft normalGlueA = sortTask("ORD-1", "GL-A", "BASE-1", "MP-1", "5");
        TmTaskDraft laterGlueB = sortTask("ORD-2", "GL-B", "BASE-2", "MP-2", "9");
        TmScheduleContext context = new TmScheduleContext();
        context.setTaskDraftList(Arrays.asList(normalGlueA, laterGlueB, urgentGlueB));

        // 构造默认任务排序比较器并对任务列表排序。
        Comparator<TmTaskDraft> comparator = new TmDefaultTaskSortStrategy().buildComparator(context);
        List<TmTaskDraft> sorted = context.getTaskDraftList();
        sorted.sort(comparator);

        // 断言排序结果，确保紧急任务和同胶料分组规则生效。
        assertEquals("ORD-3", sorted.get(0).getOrderNo());
        assertEquals("ORD-2", sorted.get(1).getOrderNo());
        assertEquals("ORD-1", sorted.get(2).getOrderNo());
    }

    /**
     * 构造测试任务。
     *
     * @return 胎面任务草稿
     */
    private TmTaskDraft buildTask() {
        TmTaskDraft task = new TmTaskDraft();
        task.setOrderNo("ORD-001");
        task.setGlueCode("GL-A");
        task.setBaseGlueCode("BASE-A");
        task.setMouthPlateCode("MP-A");
        return task;
    }

    private TmTaskDraft buildPlanTask(String demandQty) {
        TmTaskDraft task = new TmTaskDraft();
        task.setCurrentShiftDemandQty(new BigDecimal(demandQty));
        task.setGuardDemandQty(new BigDecimal(demandQty));
        task.setRollingStockQty(BigDecimal.ZERO);
        return task;
    }

    private TmTaskDraft sortTask(String orderNo, String glueCode, String baseGlueCode,
                                 String mouthPlateCode, String supplyHours) {
        TmTaskDraft task = new TmTaskDraft();
        task.setOrderNo(orderNo);
        task.setTreadCode("TR-" + orderNo);
        task.setGlueCode(glueCode);
        task.setBaseGlueCode(baseGlueCode);
        task.setMouthPlateCode(mouthPlateCode);
        task.setShiftOrder(1);
        task.setSupplyHours(new BigDecimal(supplyHours));
        return task;
    }

    /**
     * 构造默认通过基础过滤条件的候选机台。
     *
     * @param machineCode 机台编码
     * @return 候选机台
     */
    private TmMachineCandidate enabledCandidate(String machineCode) {
        TmMachineCandidate candidate = new TmMachineCandidate();
        candidate.setMachineCode(machineCode);
        candidate.setEnabled(Boolean.TRUE);
        candidate.setRemainCapacity(new BigDecimal("1000"));
        candidate.setMouthPlateMatched(Boolean.TRUE);
        candidate.setGlueMachineMatched(Boolean.TRUE);
        candidate.setFixedMachineSelected(Boolean.TRUE);
        candidate.setFixedMachineExcluded(Boolean.FALSE);
        return candidate;
    }

    private TmMachineCandidate candidateWith(String machineCode, Boolean enabled, Boolean mouthPlateMatched,
                                             Boolean glueMachineMatched, Boolean fixedMachineSelected,
                                             Boolean fixedMachineExcluded) {
        TmMachineCandidate candidate = enabledCandidate(machineCode);
        if (enabled != null) {
            candidate.setEnabled(enabled);
        }
        if (mouthPlateMatched != null) {
            candidate.setMouthPlateMatched(mouthPlateMatched);
        }
        if (glueMachineMatched != null) {
            candidate.setGlueMachineMatched(glueMachineMatched);
        }
        if (fixedMachineSelected != null) {
            candidate.setFixedMachineSelected(fixedMachineSelected);
        }
        if (fixedMachineExcluded != null) {
            candidate.setFixedMachineExcluded(fixedMachineExcluded);
        }
        return candidate;
    }

    private void assertRejectedBy(TmMachineCandidate candidate, String reasonCode) {
        TmMachineRuleContext ruleContext = new TmMachineRuleContext();
        ruleContext.setTaskDraft(buildTask());

        ScheduleRuleResult result = new TmDefaultMachineFilterRule().evaluate(candidate, ruleContext);

        assertFalse(result.isPassed());
        assertTrue(candidate.getFiltered());
        assertEquals(reasonCode, candidate.getFilterReasonCode());
    }

    /**
     * 按数值比较 BigDecimal，忽略小数位格式差异。
     *
     * @param expected 期望值
     * @param actual   实际值
     */
    private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual));
    }
}
