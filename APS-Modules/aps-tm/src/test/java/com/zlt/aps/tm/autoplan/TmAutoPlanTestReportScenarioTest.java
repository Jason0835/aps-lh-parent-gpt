package com.zlt.aps.tm.autoplan;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.zlt.aps.tm.api.domain.entity.*;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 胎面自动排程测试报告 25 个业务场景回归测试。
 *
 * <p>每个测试方法读取一个独立 JSON 场景，通过
 * {@link com.zlt.aps.tm.service.impl.TmScheduleResultServiceImpl#tmAutoPlan}
 * 完整服务入口执行，验证测试报告要求中的场景矩阵是否具备可重复自动化回归数据。</p>
 */
public class TmAutoPlanTestReportScenarioTest {

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
        initTableInfo(TmLossSetting.class);
    }

    /**
     * 测试内容：TM_SCENE_001 正常自动排程。
     */
    @Test
    public void shouldReportScene001NormalAutoPlan() {
        executeScene("tm_scene_001_normal_auto_plan.json");
    }

    /**
     * 测试内容：TM_SCENE_002 同主胶料优先连续排产。
     */
    @Test
    public void shouldReportScene002SameMainGlueContinuousPlan() {
        executeScene("tm_scene_002_same_main_glue_continuous_plan.json");
    }

    /**
     * 测试内容：TM_SCENE_003 不同主胶料下基部胶相同数量多者优先。
     */
    @Test
    public void shouldReportScene003BaseGlueCountPriority() {
        executeScene("tm_scene_003_base_glue_count_priority.json");
    }

    /**
     * 测试内容：TM_SCENE_004 综合排序规则。
     */
    @Test
    public void shouldReportScene004CompositeSortRules() {
        executeScene("tm_scene_004_composite_sort_rules.json");
    }

    /**
     * 测试内容：TM_SCENE_005 库存不足触发排产。
     */
    @Test
    public void shouldReportScene005StockShortageTriggersPlan() {
        executeScene("tm_scene_005_stock_shortage_triggers_plan.json");
    }

    /**
     * 测试内容：TM_SCENE_006 库存充足允许计划量为 0。
     */
    @Test
    public void shouldReportScene006StockEnoughZeroPlanQty() {
        executeScene("tm_scene_006_stock_enough_zero_plan_qty.json");
    }

    /**
     * 测试内容：TM_SCENE_007 算法 1 需求量计算。
     */
    @Test
    public void shouldReportScene007AlgorithmOneDemandQty() {
        executeScene("tm_scene_007_algorithm_one_demand_qty.json");
    }

    /**
     * 测试内容：TM_SCENE_008 算法 2 需求量计算。
     */
    @Test
    public void shouldReportScene008AlgorithmTwoDemandQty() {
        executeScene("tm_scene_008_algorithm_two_demand_qty.json");
    }

    /**
     * 测试内容：TM_SCENE_009 非收尾规格卷曲长度取整。
     */
    @Test
    public void shouldReportScene009NonTailCurlRounding() {
        executeScene("tm_scene_009_non_tail_curl_rounding.json");
    }

    /**
     * 测试内容：TM_SCENE_010 收尾规格不按卷曲长度取整。
     */
    @Test
    public void shouldReportScene010TailNoCurlRounding() {
        executeScene("tm_scene_010_tail_no_curl_rounding.json");
    }

    /**
     * 测试内容：TM_SCENE_011 工装数量限制计划量。
     */
    @Test
    public void shouldReportScene011ToolLimitPlanQty() {
        executeScene("tm_scene_011_tool_limit_plan_qty.json");
    }

    /**
     * 测试内容：TM_SCENE_012 口型板匹配机台。
     */
    @Test
    public void shouldReportScene012MouthPlateMachineMatch() {
        executeScene("tm_scene_012_mouth_plate_machine_match.json");
    }

    /**
     * 测试内容：TM_SCENE_013 胶料可投机台关系。
     */
    @Test
    public void shouldReportScene013GlueMachineRelation() {
        executeScene("tm_scene_013_glue_machine_relation.json");
    }

    /**
     * 测试内容：TM_SCENE_014 定点机台优先与禁排机台过滤。
     */
    @Test
    public void shouldReportScene014SpecifyAndForbidMachine() {
        executeScene("tm_scene_014_specify_and_forbid_machine.json");
    }

    /**
     * 测试内容：TM_SCENE_015 机台停用或班次不开班。
     */
    @Test
    public void shouldReportScene015MachineDisabledOrShiftClosed() {
        executeScene("tm_scene_015_machine_disabled_or_shift_closed.json");
    }

    /**
     * 测试内容：TM_SCENE_016 检修停机扣减产能。
     */
    @Test
    public void shouldReportScene016MaintenanceDeductCapacity() {
        executeScene("tm_scene_016_maintenance_deduct_capacity.json");
    }

    /**
     * 测试内容：TM_SCENE_017 当前班产能不足，未排量顺延到下一班同机台承接。
     */
    @Test
    public void shouldReportScene017SameShiftOtherMachineOverflow() {
        executeScene("tm_scene_017_same_shift_other_machine_overflow.json");
    }

    /**
     * 测试内容：TM_SCENE_018 当前机台产能不足，下一班同机台优先后继续顺延承接。
     */
    @Test
    public void shouldReportScene018NextShiftOverflow() {
        executeScene("tm_scene_018_next_shift_overflow.json");
    }

    /**
     * 测试内容：TM_SCENE_019 6 班后仍不足写未排。
     */
    @Test
    public void shouldReportScene019SixShiftCapacityUnplanned() {
        executeScene("tm_scene_019_six_shift_capacity_unplanned.json");
    }

    /**
     * 测试内容：TM_SCENE_020 无成型需求。
     */
    @Test
    public void shouldReportScene020NoFormingDemand() {
        executeScene("tm_scene_020_no_forming_demand.json");
    }

    /**
     * 测试内容：TM_SCENE_021 无施工或 BOM 数据。
     */
    @Test
    public void shouldReportScene021MissingConstructionBom() {
        executeScene("tm_scene_021_missing_construction_bom.json");
    }

    /**
     * 测试内容：TM_SCENE_022 新规格基础资料缺失。
     */
    @Test
    public void shouldReportScene022NewSpecBaseDataMissing() {
        executeScene("tm_scene_022_new_spec_base_data_missing.json");
    }

    /**
     * 测试内容：TM_SCENE_023 旧结果未发布允许覆盖。
     */
    @Test
    public void shouldReportScene023UnreleasedResultCanOverwrite() {
        executeScene("tm_scene_023_unreleased_result_can_overwrite.json");
    }

    /**
     * 测试内容：TM_SCENE_024 旧结果存在已发布拒绝重排。
     */
    @Test
    public void shouldReportScene024ReleasedResultRejectsReplan() {
        executeScene("tm_scene_024_released_result_rejects_replan.json");
    }

    /**
     * 测试内容：TM_SCENE_025 未排解释完整性。
     */
    @Test
    public void shouldReportScene025UnplannedExplainIntegrity() {
        executeScene("tm_scene_025_unplanned_explain_integrity.json");
    }

    /**
     * 测试内容：TM_SCENE_026 新规格额外提前排产。
     */
    @Test
    public void shouldReportScene026NewSpecAdvancePlan() {
        executeScene("tm_scene_026_new_spec_advance_plan.json");
    }

    /**
     * 测试内容：TM_SCENE_027 前一天有有效库存时不按新规格提前。
     */
    @Test
    public void shouldReportScene027NotNewSpecStockExists() {
        executeScene("tm_scene_027_not_new_spec_stock_exists.json");
    }

    /**
     * 测试内容：TM_SCENE_028 前一天无库存但历史有计划量时不按新规格提前。
     */
    @Test
    public void shouldReportScene028NotNewSpecHistoryExists() {
        executeScene("tm_scene_028_not_new_spec_history_exists.json");
    }

    /**
     * 测试内容：TM_SCENE_029 前一天有库存且历史有计划量时保持正常班次。
     */
    @Test
    public void shouldReportScene029NotNewSpecStockAndHistoryExists() {
        executeScene("tm_scene_029_not_new_spec_stock_and_history_exists.json");
    }

    /**
     * 测试内容：TM_SCENE_030 新规格参数缺失时使用默认值。
     */
    @Test
    public void shouldReportScene030NewSpecDefaultParams() {
        executeScene("tm_scene_030_new_spec_default_params.json");
    }

    /**
     * 测试内容：TM_SCENE_031 新规格参数配置为 10 天和 3 班。
     */
    @Test
    public void shouldReportScene031NewSpecCustomParams() {
        executeScene("tm_scene_031_new_spec_custom_params.json");
    }

    /**
     * 测试内容：TM_SCENE_032 提前窗口产能不足时剩余滚动。
     */
    @Test
    public void shouldReportScene032NewSpecWindowCapacityShortageRoll() {
        executeScene("tm_scene_032_new_spec_window_capacity_shortage_roll.json");
    }

    /**
     * 测试内容：TM_SCENE_033 六班总产能不足时写未排。
     */
    @Test
    public void shouldReportScene033NewSpecSixShiftCapacityUnplanned() {
        executeScene("tm_scene_033_new_spec_six_shift_capacity_unplanned.json");
    }

    /**
     * 测试内容：TM_SCENE_034 新规格与非新规格混合排产。
     */
    @Test
    public void shouldReportScene034NewAndOldSpecMixed() {
        executeScene("tm_scene_034_new_and_old_spec_mixed.json");
    }

    /**
     * 测试内容：TM_SCENE_035 实验规格无成型需求时新增独立一班任务。
     */
    @Test
    public void shouldReportScene035ExperimentSpecCreatesIndependentTask() {
        executeScene("tm_scene_035_experiment_spec_independent_task.json");
    }

    /**
     * 测试内容：TM_SCENE_036 实验规格命中同胎面时叠加到一班任务。
     */
    @Test
       public void shouldReportScene036ExperimentSpecMergeSameTreadTask() {
        executeScene("tm_scene_036_experiment_spec_merge_same_tread.json");
    }

    /**
     * 测试内容：TM_SCENE_037 实验规格自定义参数和非实验跳过。
     */
    @Test
    public void shouldReportScene037ExperimentSpecCustomParamsAndSkipNonExperiment() {
        executeScene("tm_scene_037_experiment_spec_custom_params_skip.json");
    }

    /**
     * 测试内容：TM_SCENE_038 同机台前置任务链连续排序。
     */
    @Test
    public void shouldReportScene038MachinePredecessorChainSort() {
        executeScene("tm_scene_038_machine_predecessor_chain_sort.json");
    }

    /**
     * TM_SCENE_039：小胶种参数命中后连续生产规则写入解释证据。
     */
    @Test
    public void shouldReportScene039SmallGlueContinuous() {
        executeScene("tm_scene_039_small_glue_continuous.json");
    }
    /**
     * 读取并执行单个报告场景。
     *
     * @param fileName 场景 JSON 文件名
     */
    private void executeScene(String fileName) {
        TmAutoPlanScenario scenario = loader.load(fileName);
        TmAutoPlanMockFactory.MockContext mockContext = mockFactory.create(scenario);
        assertHelper.executeAndAssert(mockContext);
    }

    private static void initTableInfo(Class<?> entityClass) {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), entityClass.getName()),
                entityClass);
    }
}
