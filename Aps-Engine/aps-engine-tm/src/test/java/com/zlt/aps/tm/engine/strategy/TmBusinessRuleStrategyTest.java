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
     * 测试内容：验证库存缺口小于当前班需求时，需求量取当前班需求。
     * 测试场景：最低保障需求 1100、滚动库存 1000，库存缺口 100，小于当前班需求 600。
     * 预期结果：库存缺口为 100，最终需求量为 600，并计算出保障班次数。
     */
    @Test
    public void demandShouldUseGuardStockGapWhenCurrentDemandIsLower() {
        // 准备需求量输入，构造库存缺口小于当前班需求的场景。
        TmDemandQtyInput input = new TmDemandQtyInput();
        input.setCurrentShiftDemandQty(new BigDecimal("600"));
        input.setGuardDemandQty(new BigDecimal("1100"));
        input.setRollingStockQty(new BigDecimal("1000"));
        input.setGuardRangeHours(new BigDecimal("16"));

        // 执行最低库存保障需求量策略。
        TmDemandQtyResult result = new TmGuardDemandQtyStrategy().calculate(input, null);

        // 断言最终需求量取当前班需求，而不是较小的库存缺口。
        assertEquals(new BigDecimal("100"), result.getStockGapQty());
        assertEquals(new BigDecimal("600"), result.getDemandQty());
        assertEquals(Integer.valueOf(2), result.getGuardShiftCount());
    }

    /**
     * 测试内容：验证库存缺口大于当前班需求时，需求量取库存缺口。
     * 测试场景：最低保障需求 1500、滚动库存 1000，库存缺口 500，大于当前班需求 200。
     * 预期结果：库存缺口和最终需求量都为 500。
     */
    @Test
    public void demandShouldUseStockGapWhenGapIsGreaterThanCurrentDemand() {
        // 准备库存缺口大于当前班需求的数据。
        TmDemandQtyInput input = new TmDemandQtyInput();
        input.setCurrentShiftDemandQty(new BigDecimal("200"));
        input.setGuardDemandQty(new BigDecimal("1500"));
        input.setRollingStockQty(new BigDecimal("1000"));
        input.setGuardRangeHours(new BigDecimal("16"));

        // 执行需求量策略，验证会补足更大的库存缺口。
        TmDemandQtyResult result = new TmGuardDemandQtyStrategy().calculate(input, null);

        // 断言最终需求量使用库存缺口。
        assertEquals(new BigDecimal("500"), result.getStockGapQty());
        assertEquals(new BigDecimal("500"), result.getDemandQty());
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
     * 测试内容：验证计划量策略按基础需求、工装、最小起排、尾数取整、产能限制顺序计算。
     * 测试场景：构造有滚动库存、工装、最小起排、换规格、换胶和检修时间的任务。
     * 预期结果：各调整分量和最终计划量符合当前策略顺序。
     */
    @Test
    public void planShouldApplyToolMinRoundAndCapacityInOrder() {
        // 准备综合计划量计算任务，覆盖多个调整分量同时存在的场景。
        TmTaskDraft task = new TmTaskDraft();
        task.setTreadCode("TR-PLAN-1");
        task.setCurrentShiftDemandQty(new BigDecimal("380"));
        task.setGuardDemandQty(new BigDecimal("900"));
        task.setRollingStockQty(new BigDecimal("200"));
        task.setSixClockStockQty(new BigDecimal("1000"));
        task.setTotalToolQty(new BigDecimal("8"));
        task.setCurlRollLength(new BigDecimal("200"));
        task.setDefaultCurlRollLength(new BigDecimal("180"));
        task.setMinStartQty(new BigDecimal("450"));
        task.setMachineRemainCapacity(new BigDecimal("650"));
        task.setMachineSpeed(new BigDecimal("100"));
        task.setMaintenanceHours(new BigDecimal("1"));
        task.setPreviousSpecSwitchHours(new BigDecimal("0.5"));
        task.setPreviousGlueSwitchHours(new BigDecimal("0.5"));

        // 准备上下文剩余库存 200，库存抵扣后基础需求=max(380-200,900-200,0)=700。
        TmScheduleContext context = new TmScheduleContext();
        context.getRemainingStockMap().put("TR-PLAN-1", new BigDecimal("200"));

        // 执行默认计划量策略。
        TmPlanQtyResult result = new TmDefaultPlanQtyStrategy().calculate(task, context);

        // 断言基础需求、工装、取整、产能调整和最终计划量。
        assertBigDecimalEquals(new BigDecimal("700"), result.getBaseDemandQty());
        assertBigDecimalEquals(BigDecimal.ZERO, result.getToolLimitAdjustQty());
        assertBigDecimalEquals(BigDecimal.ZERO, result.getMinStartAdjustQty());
        assertBigDecimalEquals(new BigDecimal("100"), result.getTailRoundAdjustQty());
        assertBigDecimalEquals(new BigDecimal("-350"), result.getCapacityAdjustQty());
        assertBigDecimalEquals(new BigDecimal("450"), result.getFinalPlanQty());
    }

    /**
     * 测试内容：验证可用工装限制会压低最终计划量。
     * 测试场景：需求 700，但 3 个工装和 200 卷曲长度最多支持 600。
     * 预期结果：工装调整量为 -100，最终计划量为 600。
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
        assertBigDecimalEquals(new BigDecimal("-100"), result.getToolLimitAdjustQty());
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
     * 测试内容：验证检修等不可用时间会扣减机台剩余产能。
     * 测试场景：剩余产能 500，机台速度 100，检修 1 小时后可用产能为 400。
     * 预期结果：产能调整量为 -200，最终计划量限制为 400。
     */
    @Test
    public void planShouldCapByRemainCapacityAfterDeductingUnavailableHours() {
        // 准备需求超过扣减后可用产能的任务。
        TmTaskDraft task = buildPlanTask("600");
        task.setMachineRemainCapacity(new BigDecimal("500"));
        task.setMachineSpeed(new BigDecimal("100"));
        task.setMaintenanceHours(new BigDecimal("1"));

        // 执行计划量策略。
        TmPlanQtyResult result = new TmDefaultPlanQtyStrategy().calculate(task, null);

        // 断言最终计划量被扣减后的产能上限限制。
        assertBigDecimalEquals(new BigDecimal("-200"), result.getCapacityAdjustQty());
        assertBigDecimalEquals(new BigDecimal("400"), result.getFinalPlanQty());
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
     * 测试场景：当前班需求 600、保证范围需求 600，剩余库存 200。
     * 预期结果：库存抵扣 200，基础需求=max(600-200,600-200,0)=400。
     */
    @Test
    public void planShouldDeductStockUpToRemainingWhenStockInsufficient() {
        TmTaskDraft task = new TmTaskDraft();
        task.setTreadCode("TR-DEDUCT-1");
        task.setCurrentShiftDemandQty(new BigDecimal("600"));
        task.setGuardDemandQty(new BigDecimal("600"));
        TmScheduleContext context = new TmScheduleContext();
        context.getRemainingStockMap().put("TR-DEDUCT-1", new BigDecimal("200"));

        TmPlanQtyResult result = new TmDefaultPlanQtyStrategy().calculate(task, context);

        assertBigDecimalEquals(new BigDecimal("200"), task.getStockDeductQty());
        assertBigDecimalEquals(new BigDecimal("400"), result.getBaseDemandQty());
    }

    /**
     * 测试内容：验证同一胎面多班次库存逐班递减，避免库存被重复抵扣。
     * 测试场景：两班任务当前班需求各 600，剩余库存 500；按班次顺序抵扣。
     * 预期结果：首班抵扣 500（基础需求 100），剩余库存 0；二班抵扣 0（基础需求 600）。
     */
    @Test
    public void planShouldDecrementStockAcrossShiftsWithoutRepeatDeduct() {
        TmScheduleContext context = new TmScheduleContext();
        context.getRemainingStockMap().put("TR-DEDUCT-2", new BigDecimal("500"));
        TmDefaultPlanQtyStrategy strategy = new TmDefaultPlanQtyStrategy();

        TmTaskDraft firstShift = new TmTaskDraft();
        firstShift.setTreadCode("TR-DEDUCT-2");
        firstShift.setCurrentShiftDemandQty(new BigDecimal("600"));
        firstShift.setGuardDemandQty(new BigDecimal("600"));
        TmPlanQtyResult firstResult = strategy.calculate(firstShift, context);

        TmTaskDraft secondShift = new TmTaskDraft();
        secondShift.setTreadCode("TR-DEDUCT-2");
        secondShift.setCurrentShiftDemandQty(new BigDecimal("600"));
        secondShift.setGuardDemandQty(new BigDecimal("600"));
        TmPlanQtyResult secondResult = strategy.calculate(secondShift, context);

        assertBigDecimalEquals(new BigDecimal("500"), firstShift.getStockDeductQty());
        assertBigDecimalEquals(new BigDecimal("100"), firstResult.getBaseDemandQty());
        assertBigDecimalEquals(BigDecimal.ZERO, secondShift.getStockDeductQty());
        assertBigDecimalEquals(new BigDecimal("600"), secondResult.getBaseDemandQty());
    }

    /**
     * 测试内容：验证库存充足时当前班基础需求被抵扣为 0。
     * 测试场景：当前班需求 300、保证范围需求 300，剩余库存 1000。
     * 预期结果：库存抵扣 300，基础需求为 0。
     */
    @Test
    public void planShouldDeductAllDemandWhenStockSufficient() {
        TmTaskDraft task = new TmTaskDraft();
        task.setTreadCode("TR-DEDUCT-3");
        task.setCurrentShiftDemandQty(new BigDecimal("300"));
        task.setGuardDemandQty(new BigDecimal("300"));
        TmScheduleContext context = new TmScheduleContext();
        context.getRemainingStockMap().put("TR-DEDUCT-3", new BigDecimal("1000"));

        TmPlanQtyResult result = new TmDefaultPlanQtyStrategy().calculate(task, context);

        assertBigDecimalEquals(new BigDecimal("300"), task.getStockDeductQty());
        assertBigDecimalEquals(BigDecimal.ZERO, result.getBaseDemandQty());
        // 库存扣减后剩余 700
        assertBigDecimalEquals(new BigDecimal("700"), context.getRemainingStockMap().get("TR-DEDUCT-3"));
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
