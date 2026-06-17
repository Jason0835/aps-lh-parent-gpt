package com.zlt.aps.tm.engine.strategy;

import com.zlt.aps.tm.engine.domain.*;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

/**
 * 胎面自动排程业务规则策略测试。
 *
 * <p>使用具体模拟数据验证 6 点库存、库存最低保证班数、计划量分量和机台评分口径。</p>
 */
public class TmBusinessRuleStrategyTest {

    @Test
    public void demandShouldUseGuardStockGapWhenCurrentDemandIsLower() {
        TmDemandQtyInput input = new TmDemandQtyInput();
        input.setCurrentShiftDemandQty(new BigDecimal("600"));
        input.setGuardDemandQty(new BigDecimal("1100"));
        input.setRollingStockQty(new BigDecimal("1000"));
        input.setGuardRangeHours(new BigDecimal("16"));

        TmDemandQtyResult result = new TmGuardDemandQtyStrategy().calculate(input);

        assertEquals(new BigDecimal("100"), result.getStockGapQty());
        assertEquals(new BigDecimal("600"), result.getDemandQty());
        assertEquals(Integer.valueOf(2), result.getGuardShiftCount());
    }

    @Test
    public void demandShouldNotDivideByZeroWhenFutureDemandIsZero() {
        TmDemandQtyInput input = new TmDemandQtyInput();
        input.setCurrentShiftDemandQty(BigDecimal.ZERO);
        input.setGuardDemandQty(BigDecimal.ZERO);
        input.setRollingStockQty(new BigDecimal("1000"));
        input.setGuardRangeHours(new BigDecimal("16"));

        TmDemandQtyResult result = new TmGuardDemandQtyStrategy().calculate(input);

        assertEquals(BigDecimal.ZERO, result.getDemandQty());
        assertNull(result.getSupplyHours());
    }

    @Test
    public void planShouldApplyToolMinRoundAndCapacityInOrder() {
        TmTaskDraft task = new TmTaskDraft();
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

        TmPlanQtyResult result = new TmDefaultPlanQtyStrategy().calculate(task);

        assertBigDecimalEquals(new BigDecimal("700"), result.getBaseDemandQty());
        assertBigDecimalEquals(new BigDecimal("-100"), result.getToolLimitAdjustQty());
        assertBigDecimalEquals(BigDecimal.ZERO, result.getMinStartAdjustQty());
        assertBigDecimalEquals(BigDecimal.ZERO, result.getTailRoundAdjustQty());
        assertBigDecimalEquals(new BigDecimal("-150"), result.getCapacityAdjustQty());
        assertBigDecimalEquals(new BigDecimal("450"), result.getFinalPlanQty());
    }

    @Test
    public void machineFilterShouldRejectFirstFailedRule() {
        TmMachineCandidate candidate = enabledCandidate("TM01");
        candidate.setRemainCapacity(BigDecimal.ZERO);

        boolean passed = new TmDefaultMachineFilterRule().filter(buildTask(), candidate);

        assertFalse(passed);
        assertTrue(candidate.getFiltered());
        assertEquals("NO_REMAIN_CAPACITY", candidate.getFilterReasonCode());
    }

    @Test
    public void machineScoreShouldPreferMainGlueThenBaseGlueSimilarity() {
        TmTaskDraft task = buildTask();
        task.setPlanQty(new BigDecimal("500"));

        TmMachineCandidate mainGlue = enabledCandidate("TM01");
        mainGlue.setRemainCapacity(new BigDecimal("600"));
        mainGlue.setTailMainGlueCode("GL-A");
        mainGlue.setTailBaseGlueCode("BASE-X");

        TmMachineCandidate baseGlue = enabledCandidate("TM02");
        baseGlue.setRemainCapacity(new BigDecimal("600"));
        baseGlue.setTailMainGlueCode("GL-B");
        baseGlue.setTailBaseGlueCode("BASE-A");

        TmDefaultMachineScoreStrategy strategy = new TmDefaultMachineScoreStrategy();
        BigDecimal mainScore = strategy.score(task, mainGlue);
        BigDecimal baseScore = strategy.score(task, baseGlue);

        assertTrue(mainScore.compareTo(baseScore) > 0);
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
