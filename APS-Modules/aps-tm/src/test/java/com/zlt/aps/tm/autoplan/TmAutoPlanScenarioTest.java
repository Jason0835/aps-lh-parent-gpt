package com.zlt.aps.tm.autoplan;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.zlt.aps.tm.api.domain.entity.*;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 胎面自动排程 JSON 场景回归测试。
 *
 * <p>逐个读取本地 JSON 场景，通过 {@link com.zlt.aps.tm.service.impl.TmScheduleResultServiceImpl#tmAutoPlan}
 * 完整服务入口执行，验证响应、结果表、解释表和已知详设差异记录。</p>
 */
public class TmAutoPlanScenarioTest {

    private final TmAutoPlanTestDataLoader loader = new TmAutoPlanTestDataLoader();

    private final TmAutoPlanMockFactory mockFactory = new TmAutoPlanMockFactory();

    private final TmAutoPlanAssertHelper assertHelper = new TmAutoPlanAssertHelper();

    /**
     * 初始化 MyBatisPlus 表信息缓存，避免无 Spring 容器下 LambdaQueryWrapper 解析字段失败。
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
    }

    /**
     * 测试内容：正常六班自动排程成功。
     */
    @Test
    public void shouldPlanNormalSixShiftScenario() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_01_normal_six_shift_auto_plan.json");

        // When / Then
        assertHelper.executeAndAssert(mockContext);
    }

    /**
     * 测试内容：未发布旧结果允许确认后覆盖重排。
     */
    @Test
    public void shouldReplanWhenExistingResultsAreUnreleased() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_02_existing_unreleased_result_replan.json");

        // When / Then
        assertHelper.executeAndAssert(mockContext);
    }

    /**
     * 测试内容：已有发布结果时拒绝重复生成。
     */
    @Test
    public void shouldRejectWhenExistingResultHasBeenReleased() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_03_existing_released_result_reject.json");

        // When / Then
        assertHelper.executeAndAssert(mockContext);
    }

    /**
     * 测试内容：成型计划为空时不生成排程结果。
     */
    @Test
    public void shouldReturnEmptyWhenNoCxScheduleResult() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_04_no_cx_schedule_result.json");

        // When / Then
        assertHelper.executeAndAssert(mockContext);
    }

    /**
     * 测试内容：算法 1 按成型班次最大计划量计算需求。
     */
    @Test
    public void shouldUseAlgorithmOneMaxClassDemand() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_05_algorithm_1_max_class_demand.json");

        // When / Then
        assertHelper.executeAndAssert(mockContext);
    }

    /**
     * 测试内容：算法 2 当前仅记录未注册策略差异。
     */
    @Test
    public void shouldRecordGapForAlgorithmTwoStrategy() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_06_algorithm_2_next_shift_demand.json");

        // When / Then
        assertHelper.executeAndAssert(mockContext);
    }

    /**
     * 测试内容：库存不足时生成补库计划。
     */
    @Test
    public void shouldPlanWhenStockGuardShiftIsShortage() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_07_stock_guard_shift_shortage.json");

        // When / Then
        assertHelper.executeAndAssert(mockContext);
    }

    /**
     * 测试内容：库存足够覆盖保证班数时允许计划量为 0。
     */
    @Test
    public void shouldAllowZeroPlanQtyWhenStockIsEnough() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_08_stock_enough_no_need_plan.json");

        // When / Then
        assertHelper.executeAndAssert(mockContext);
    }

    /**
     * 测试内容：收尾规格计划量当前按已知差异记录。
     */
    @Test
    public void shouldRecordGapForTailTreadPlanQty() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_09_tail_tread_plan_qty.json");

        // When / Then
        assertHelper.executeAndAssert(mockContext);
    }

    /**
     * 测试内容：非收尾规格按卷曲长度向上取整。
     */
    @Test
    public void shouldRoundNonTailTreadByCurlRollLength() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_10_non_tail_roll_rounding.json");

        // When / Then
        assertHelper.executeAndAssert(mockContext);
    }

    /**
     * 测试内容：工装数量限制会截断计划量。
     */
    @Test
    public void shouldCutPlanQtyByToolLimit() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_11_tool_limit_cut_plan_qty.json");

        // When / Then
        assertHelper.executeAndAssert(mockContext);
    }

    /**
     * 测试内容：检修时长扣减机台产能后选择可用机台。
     */
    @Test
    public void shouldDeductCapacityByMaintenanceHours() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_12_machine_maintenance_capacity_deduct.json");

        // When / Then
        assertHelper.executeAndAssert(mockContext);
    }

    /**
     * 测试内容：口型板不匹配时任务未排。
     */
    @Test
    public void shouldUnplanWhenMouthPlateDoesNotMatch() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_13_mouth_plate_not_match_unplanned.json");

        // When / Then
        assertHelper.executeAndAssert(mockContext);
    }

    /**
     * 测试内容：胶料机台关系不允许时任务未排。
     */
    @Test
    public void shouldUnplanWhenGlueMachineIsNotAllowed() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_14_glue_machine_not_allowed_unplanned.json");

        // When / Then
        assertHelper.executeAndAssert(mockContext);
    }

    /**
     * 测试内容：定点生产与禁排机台规则共同生效。
     */
    @Test
    public void shouldApplySpecifyMachineAndForbidMachineRules() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_15_specify_machine_and_forbid_machine.json");

        // When / Then
        assertHelper.executeAndAssert(mockContext);
    }

    /**
     * 测试内容：同分候选机台按机台编码稳定排序。
     */
    @Test
    public void shouldSortSameScoreMachineByMachineCode() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_16_machine_score_stable_order.json");

        // When / Then
        assertHelper.executeAndAssert(mockContext);
    }

    /**
     * 测试内容：未排任务仍写入结果和解释。
     */
    @Test
    public void shouldPersistUnplannedResultAndExplain() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_17_unplanned_result_persist_explain.json");

        // When / Then
        assertHelper.executeAndAssert(mockContext);
    }

    /**
     * 测试内容：成功排程时结果表和解释表均写入。
     */
    @Test
    public void shouldPersistResultAndExplainWhenPlanSuccess() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_18_persist_result_and_explain_success.json");

        // When / Then
        assertHelper.executeAndAssert(mockContext);
    }

    /**
     * 测试内容：落库失败事务回滚当前差异通过 knownGaps 记录。
     */
    @Test
    public void shouldRecordGapForPersistRollbackBehavior() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_19_transaction_rollback_when_persist_error.json");

        // When / Then
        assertHelper.executeAndAssert(mockContext);
    }

    /**
     * 测试内容：停产需求重分配当前仅步骤级验证并记录完整入口差异。
     */
    @Test
    public void shouldRecordGapForShutdownDemandRedistribution() {
        // Given
        TmAutoPlanMockFactory.MockContext mockContext = buildContext("case_20_stop_production_demand_redistribute.json");

        // When / Then
        assertHelper.executeAndAssert(mockContext);
    }

    private TmAutoPlanMockFactory.MockContext buildContext(String fileName) {
        TmAutoPlanScenario scenario = loader.load(fileName);
        return mockFactory.create(scenario);
    }

    private static void initTableInfo(Class<?> entityClass) {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), entityClass.getName()),
                entityClass);
    }
}
