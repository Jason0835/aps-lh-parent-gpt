package com.zlt.aps.tm.autoplan;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.zlt.aps.tm.api.domain.entity.*;
import com.zlt.aps.tm.engine.domain.TmMachineCandidate;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

/**
 * 胎面自动排程 JSON 步骤级测试。
 *
 * <p>复用完整场景 JSON，验证自动排程 Step2~Step19 中当前代码可观察的加载、需求计算、
 * 库存、排序、派机和落库转换边界。</p>
 */
public class TmAutoPlanStepTest {

    private final TmAutoPlanTestDataLoader loader = new TmAutoPlanTestDataLoader();

    private final TmAutoPlanMockFactory mockFactory = new TmAutoPlanMockFactory();

    private final TmAutoPlanAssertHelper assertHelper = new TmAutoPlanAssertHelper();

    /**
     * 初始化 MyBatisPlus 表信息缓存，避免步骤测试依赖其他测试类执行顺序。
     */
    @BeforeClass
    public static void initMybatisPlusTableInfo() {
        initTableInfo(TmScheduleResult.class);
        initTableInfo(TmParams.class);
        initTableInfo(TmMachineInfo.class);
        initTableInfo(TmMouthPlate.class);
        initTableInfo(TmGlueMachineReal.class);
        initTableInfo(TmSpecifyMachine.class);
        initTableInfo(TmMachineSpeed.class);
        initTableInfo(TmMachineMaintenance.class);
        initTableInfo(TmCurlRoll.class);
        initTableInfo(TmLossSetting.class);
    }

    /**
     * 测试内容：验证 JSON 数据加载会生成机台候选和任务草稿。
     */
    @Test
    public void shouldLoadBaseDataAndTaskDraftFromJson() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_01_normal_six_shift_auto_plan.json");

        // When
        TmScheduleContext context = mockFactory.loadContextOnly(mockContext);

        // Then
        assertHelper.assertLoadedContext(mockContext.getScenario(), context);
        assertEquals(6, context.getTaskDraftList().size());
        assertEquals(2, context.getMachineCandidateList().size());
        TmMachineCandidate candidate = findCandidate(context, "TM01");
        assertEquals("TM01", candidate.getMachineCode());
        assertTrue(candidate.getMouthPlateCodes().contains("MP-A"));
    }

    /**
     * 测试内容：验证算法 1 在数据加载层按前三班最大成型量生成胎面需求。
     */
    @Test
    public void shouldCalculateAlgorithmOneMaxClassDemandFromJson() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_05_algorithm_1_max_class_demand.json");

        // When
        TmScheduleContext context = mockFactory.loadContextOnly(mockContext);

        // Then
        TmTaskDraft firstShiftTask = assertHelper.findTask(context, "TR-A1", 1);
        assertBigDecimalEquals("50", firstShiftTask.getCurrentShiftDemandQty());
        assertBigDecimalEquals("70", firstShiftTask.getGuardDemandQty());
        assertEquals("ORD-A1-CLASS1", firstShiftTask.getOrderNo());
    }

    /**
     * 测试内容：验证算法 2 在数据加载层按下一班成型量生成胎面需求。
     */
    @Test
    public void shouldCalculateAlgorithmTwoNextShiftDemand() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_06_algorithm_2_next_shift_demand.json");

        // When
        TmScheduleContext context = mockFactory.loadContextOnly(mockContext);

        // Then
        TmTaskDraft firstShiftTask = assertHelper.findTask(context, "TR-A2", 1);
        TmTaskDraft sixthShiftTask = assertHelper.findTask(context, "TR-A2", 6);
        assertBigDecimalEquals("20", firstShiftTask.getCurrentShiftDemandQty());
        assertBigDecimalEquals("60", sixthShiftTask.getCurrentShiftDemandQty());
    }

    /**
     * 测试内容：验证库存不足场景在完整入口中产出库存缺口和补库计划。
     */
    @Test
    public void shouldCalculateStockGapAndPlanQtyForShortageScenario() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_07_stock_guard_shift_shortage.json");

        // When / Then
        assertHelper.executeAndAssert(mockContext);
        TmTaskDraft firstShiftTask = assertHelper.findTask(mockContext.getLastContext(), "TR-S1", 1);
        assertTrue(firstShiftTask.getStockGapQty().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(firstShiftTask.getPlanQty().compareTo(BigDecimal.ZERO) > 0);
    }

    /**
     * 测试内容：验证非收尾规格按卷曲长度向上取整。
     */
    @Test
    public void shouldRoundNonTailPlanQtyByCurlRollLength() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_10_non_tail_roll_rounding.json");

        // When / Then
        assertHelper.executeAndAssert(mockContext);
        TmTaskDraft firstShiftTask = assertHelper.findTask(mockContext.getLastContext(), "TR-R", 1);
        assertBigDecimalEquals("50", firstShiftTask.getCurlRollLength());
        assertBigDecimalEquals("100", firstShiftTask.getPlanQty());
    }

    /**
     * 测试内容：验证工装限制会在计划量计算阶段截断计划量。
     */
    @Test
    public void shouldApplyToolLimitDuringPlanQtyCalculation() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_11_tool_limit_cut_plan_qty.json");

        // When / Then
        assertHelper.executeAndAssert(mockContext);
        TmTaskDraft firstShiftTask = assertHelper.findTask(mockContext.getLastContext(), "TR-TOOL", 1);
        assertBigDecimalEquals("1", firstShiftTask.getTotalToolQty());
        assertBigDecimalEquals("50", firstShiftTask.getPlanQty());
    }

    /**
     * 测试内容：验证检修资料会扣减候选机台剩余产能。
     */
    @Test
    public void shouldDeductMachineRemainCapacityByMaintenance() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_12_machine_maintenance_capacity_deduct.json");

        // When / Then
        assertHelper.executeAndAssert(mockContext);
        TmMachineCandidate candidate = findCandidate(mockContext.getLastContext(), "TM01");
        assertBigDecimalEquals("2.000000", candidate.getMaintenanceHours());
    }

    /**
     * 测试内容：验证口型板不匹配时写入未排原因。
     */
    @Test
    public void shouldMarkUnplannedWhenMouthPlateNotMatch() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_13_mouth_plate_not_match_unplanned.json");

        // When / Then
        assertHelper.executeAndAssert(mockContext);
        TmTaskDraft firstShiftTask = assertHelper.findTask(mockContext.getLastContext(), "TR-MP", 1);
        assertNotNull(firstShiftTask.getUnplannedReasonCode());
        assertTrue(firstShiftTask.isUnassigned());
    }

    /**
     * 测试内容：验证同分机台按机台编码升序稳定选择。
     */
    @Test
    public void shouldChooseLowerMachineCodeWhenMachineScoreSame() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_16_machine_score_stable_order.json");

        // When / Then
        assertHelper.executeAndAssert(mockContext);
        TmTaskDraft firstShiftTask = assertHelper.findTask(mockContext.getLastContext(), "TR-ST", 1);
        assertEquals("TM01", firstShiftTask.getMachineCode());
    }

    /**
     * 测试内容：验证停产需求在数据加载层重分配。
     */
    @Test
    public void shouldRedistributeShutdownDemandAtDataLoadStep() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_20_stop_production_demand_redistribute.json");

        // When
        TmScheduleContext context = mockFactory.loadContextOnly(mockContext);

        // Then
        assertEquals(5, context.getTaskDraftList().size());
        assertBigDecimalEquals("32.500000", assertHelper.findTask(context, "TR-SD", 1).getCurrentShiftDemandQty());
        assertBigDecimalEquals("62.500000", assertHelper.findTask(context, "TR-SD", 4).getCurrentShiftDemandQty());
    }

    /**
     * 测试内容：验证落库失败会在完整入口上抛异常。
     */
    @Test
    public void shouldRejectWhenPersistFails() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_19_transaction_rollback_when_persist_error.json");

        // When / Then
        assertHelper.executeAndAssert(mockContext);
    }

    private TmAutoPlanMockFactory.MockContext buildContext(String fileName) {
        TmAutoPlanScenario scenario = loader.load(fileName);
        return mockFactory.create(scenario);
    }

    private TmMachineCandidate findCandidate(TmScheduleContext context, String machineCode) {
        for (TmMachineCandidate candidate : context.getMachineCandidateList()) {
            if (machineCode.equals(candidate.getMachineCode())) {
                return candidate;
            }
        }
        throw new AssertionError("未找到候选机台：" + machineCode);
    }

    private void assertBigDecimalEquals(String expected, BigDecimal actual) {
        assertNotNull("实际数值不能为空", actual);
        assertEquals("BigDecimal 数值不一致", 0, new BigDecimal(expected).compareTo(actual));
    }

    private static void initTableInfo(Class<?> entityClass) {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), entityClass.getName()),
                entityClass);
    }
}
