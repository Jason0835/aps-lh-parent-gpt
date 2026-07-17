package com.zlt.aps.tc.autoplan;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zlt.aps.common.engine.schedule.ScheduleRuleResult;
import com.zlt.aps.common.engine.schedule.ScheduleScoreResult;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.enums.TcScheduleStepEnum;
import com.zlt.aps.tc.engine.domain.*;
import com.zlt.aps.tc.engine.service.impl.TcMachineAssignService;
import com.zlt.aps.tc.engine.service.impl.TcSnapshotBuildService;
import com.zlt.aps.tc.engine.service.impl.TcTaskChainScheduleService;
import com.zlt.aps.tc.engine.strategy.*;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * 胎侧自动排程独立 JSON 场景测试。
 *
 * <p>场景直接驱动 TC 需求、计划量、过滤、评分、快照及机台分配实现，
 * 不启动 Spring 容器，也不连接数据库、Redis、MES 或外部服务。</p>
 */
public class TcAutoPlanJsonScenarioTest {

    private final TcAutoPlanJsonScenarioLoader loader = new TcAutoPlanJsonScenarioLoader();

    private final ObjectMapper objectMapper = this.loader.getObjectMapper();

    /**
     * 执行 JSON 文件中的全部胎侧规则场景。
     */
    @Test
    public void shouldPassAllTcAutoPlanJsonScenarios() {
        List<TcAutoPlanJsonScenario> scenarios = this.loader.loadAll();
        assertTrue("胎侧自动排程 JSON 场景不能为空", scenarios.size() >= 20);
        scenarios.forEach(this::executeScenario);
    }

    /**
     * 根据场景类型调用对应生产代码并执行断言。
     *
     * @param scenario 当前 JSON 场景
     * @throws AssertionError 场景类型未知或断言失败时抛出
     */
    private void executeScenario(TcAutoPlanJsonScenario scenario) {
        try {
            if ("DEMAND_ALGORITHM_1".equals(scenario.getType())) {
                this.executeDemandScenario(scenario, new TcGuardDemandQtyStrategy());
                return;
            }
            if ("DEMAND_ALGORITHM_2".equals(scenario.getType())) {
                this.executeDemandScenario(scenario, new TcNextShiftDemandQtyStrategy());
                return;
            }
            if ("PLAN_QTY".equals(scenario.getType())) {
                this.executePlanQtyScenario(scenario);
                return;
            }
            if ("FILTER".equals(scenario.getType())) {
                this.executeFilterScenario(scenario);
                return;
            }
            if ("SCORE".equals(scenario.getType())) {
                this.executeScoreScenario(scenario);
                return;
            }
            if ("SNAPSHOT".equals(scenario.getType())) {
                this.executeSnapshotScenario(scenario);
                return;
            }
            if ("ASSIGN".equals(scenario.getType())) {
                this.executeAssignScenario(scenario);
                return;
            }
            if ("CONSTANTS".equals(scenario.getType())) {
                this.executeConstantsScenario(scenario);
                return;
            }
            fail("未知胎侧自动排程 JSON 场景类型：" + scenario.getType());
        } catch (AssertionError assertionError) {
            throw new AssertionError("场景失败：" + scenario.getCaseName() + "，" + assertionError.getMessage(),
                    assertionError);
        }
    }

    /**
     * 执行需求算法场景。
     *
     * @param scenario JSON 场景
     * @param strategy 需求算法策略
     */
    private void executeDemandScenario(TcAutoPlanJsonScenario scenario, ITcDemandQtyStrategy strategy) {
        TcDemandQtyInput input = this.objectMapper.convertValue(scenario.getInput(), TcDemandQtyInput.class);
        TcDemandQtyResult result = strategy.calculate(input, this.buildContext(scenario));
        this.assertDecimal(scenario, "demandQty", result.getDemandQty());
        this.assertDecimal(scenario, "supplyHours", result.getSupplyHours());
        if (scenario.getExpected().containsKey("guardShiftCount")) {
            assertEquals(this.expectedInteger(scenario, "guardShiftCount"), result.getGuardShiftCount());
        }
    }

    /**
     * 执行计划量计算场景。
     *
     * @param scenario JSON 场景
     */
    private void executePlanQtyScenario(TcAutoPlanJsonScenario scenario) {
        TcTaskDraft task = this.objectMapper.convertValue(scenario.getTask(), TcTaskDraft.class);
        TcPlanQtyResult result = new TcDefaultPlanQtyStrategy().calculate(task, this.buildContext(scenario));
        this.assertDecimal(scenario, "planQty", result.getFinalPlanQty());
        this.assertDecimal(scenario, "stockDeductQty", task.getStockDeductQty());
        this.assertDecimal(scenario, "planStockQty", task.getPlanStockQty());
    }

    /**
     * 执行候选机台过滤场景。
     *
     * @param scenario JSON 场景
     */
    private void executeFilterScenario(TcAutoPlanJsonScenario scenario) {
        TcScheduleContext context = this.buildContext(scenario);
        TcTaskDraft task = this.objectMapper.convertValue(scenario.getTask(), TcTaskDraft.class);
        TcMachineCandidate candidate = this.objectMapper.convertValue(scenario.getCandidate(), TcMachineCandidate.class);
        TcMachineRuleContext ruleContext = new TcMachineRuleContext();
        ruleContext.setTaskDraft(task);
        ruleContext.setScheduleContext(context);
        ScheduleRuleResult result = new TcDefaultMachineFilterRule().evaluate(candidate, ruleContext);
        assertEquals(this.expectedBoolean(scenario, "passed"), result.isPassed());
        if (scenario.getExpected().containsKey("reasonCode")) {
            assertEquals(this.expectedString(scenario, "reasonCode"), result.getReasonCode());
        }
    }

    /**
     * 执行候选机台评分场景。
     *
     * @param scenario JSON 场景
     */
    private void executeScoreScenario(TcAutoPlanJsonScenario scenario) {
        TcScheduleContext context = this.buildContext(scenario);
        TcTaskDraft task = this.objectMapper.convertValue(scenario.getTask(), TcTaskDraft.class);
        TcMachineCandidate candidate = this.objectMapper.convertValue(scenario.getCandidate(), TcMachineCandidate.class);
        TcMachineRuleContext ruleContext = new TcMachineRuleContext();
        ruleContext.setTaskDraft(task);
        ruleContext.setScheduleContext(context);
        ScheduleScoreResult result = new TcDefaultMachineScoreStrategy().score(candidate, ruleContext);
        this.assertDecimal(scenario, "score", result.getTotalScore());
    }

    /**
     * 执行解释快照场景，验证 schemaVersion 和实际选中机台。
     *
     * @param scenario JSON 场景
     */
    private void executeSnapshotScenario(TcAutoPlanJsonScenario scenario) {
        TcScheduleContext context = this.buildContext(scenario);
        TcTaskDraft task = this.objectMapper.convertValue(scenario.getTask(), TcTaskDraft.class);
        List<TcMachineCandidate> candidates = scenario.getCandidates().stream()
                .map(value -> this.objectMapper.convertValue(value, TcMachineCandidate.class))
                .collect(Collectors.toList());
        context.getCandidateTraceMap().put(task.getBusinessKey(), candidates);
        TcSnapshotBuildResult snapshot = new TcSnapshotBuildService().buildTaskExplain(task, context);
        if (scenario.getExpected().containsKey("selectedMachineCode")) {
            cn.hutool.json.JSONObject candidateJson = JSONUtil.parseObj(snapshot.getCandidateMachineJson());
            assertEquals("1", candidateJson.getStr("schemaVersion"));
            assertEquals(this.expectedString(scenario, "selectedMachineCode"),
                    candidateJson.getJSONObject("selected").getStr("machineCode"));
        }
        if (scenario.getExpected().containsKey("assignStatus")) {
            assertEquals(this.expectedString(scenario, "assignStatus"), snapshot.getAssignStatus());
        }
    }

    /**
     * 执行机台分配、同班拆分、跨班顺延和第六班未排场景。
     *
     * @param scenario JSON 场景
     */
    private void executeAssignScenario(TcAutoPlanJsonScenario scenario) {
        TcScheduleContext context = this.buildContext(scenario);
        List<TcTaskDraft> tasks = new ArrayList<>();
        if (!scenario.getTasks().isEmpty()) {
            tasks.addAll(scenario.getTasks().stream()
                    .map(value -> this.objectMapper.convertValue(value, TcTaskDraft.class))
                    .collect(Collectors.toList()));
        } else {
            tasks.add(this.objectMapper.convertValue(scenario.getTask(), TcTaskDraft.class));
        }
        context.setTaskDraftList(tasks);
        context.setMachineCandidateList(scenario.getCandidates().stream()
                .map(value -> this.objectMapper.convertValue(value, TcMachineCandidate.class))
                .collect(Collectors.toList()));
        TcDefaultMachineFilterRule filterRule = new TcDefaultMachineFilterRule();
        TcDefaultMachineScoreStrategy scoreStrategy = new TcDefaultMachineScoreStrategy();
        TcStrategyRegistry strategyRegistry = new TcStrategyRegistry(
                Collections.<ITcDemandQtyStrategy>emptyList(),
                Collections.<ITcPlanQtyStrategy>emptyList(),
                Collections.<ITcMachineFilterRule>singletonList(filterRule),
                Collections.<ITcMachineScoreStrategy>singletonList(scoreStrategy),
                Collections.<ITcTaskSortStrategy>emptyList());
        new TcMachineAssignService(new TcTaskChainScheduleService(), strategyRegistry).assign(context);

        Map<Integer, BigDecimal> plannedByShift = context.getTaskDraftList().stream()
                .filter(task -> task.getMachineCode() != null)
                .collect(Collectors.groupingBy(TcTaskDraft::getShiftOrder,
                        Collectors.reducing(BigDecimal.ZERO, TcTaskDraft::getPlanQty, this::addDecimal)));
        Map<String, Object> expectedByShift = this.expectedMap(scenario, "plannedByShift");
        expectedByShift.forEach((shiftOrder, expectedQty) -> assertEquals(0,
                new BigDecimal(String.valueOf(expectedQty))
                        .compareTo(plannedByShift.getOrDefault(Integer.valueOf(shiftOrder), BigDecimal.ZERO))));
        BigDecimal unplannedQty = context.getTaskDraftList().stream()
                .filter(task -> task.getMachineCode() == null && task.getUnplannedReasonCode() != null)
                .map(TcTaskDraft::getPlanQty)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.assertDecimal(scenario, "unplannedQty", unplannedQty);
        if (scenario.getExpected().containsKey("sameMachineForAllTasks")) {
            long machineCount = context.getTaskDraftList().stream()
                    .filter(task -> task.getMachineCode() != null)
                    .map(TcTaskDraft::getMachineCode)
                    .distinct()
                    .count();
            assertEquals(this.expectedBoolean(scenario, "sameMachineForAllTasks"), machineCount <= 1);
        }
        if (scenario.getExpected().containsKey("assignedTaskCount")) {
            long assignedTaskCount = context.getTaskDraftList().stream()
                    .filter(task -> task.getMachineCode() != null)
                    .count();
            assertEquals(this.expectedInteger(scenario, "assignedTaskCount").longValue(), assignedTaskCount);
        }
    }

    /**
     * 验证 TC 六班及独有默认参数常量。
     *
     * @param scenario JSON 场景
     */
    private void executeConstantsScenario(TcAutoPlanJsonScenario scenario) {
        assertEquals(this.expectedInteger(scenario, "maxShiftOrder").intValue(),
                TcScheduleConstants.TC_MAX_SHIFT_ORDER);
        assertEquals(this.expectedString(scenario, "shiftMaxCapacity"),
                TcScheduleConstants.DEFAULT_SHIFT_MAX_CAPACITY);
        assertEquals(this.expectedString(scenario, "minStockClass"),
                TcScheduleConstants.DEFAULT_MIN_STOCK_CLASS);
        assertEquals(this.expectedString(scenario, "vehicleRate"),
                TcScheduleConstants.DEFAULT_VEHICLE_RATE);
        assertEquals(this.expectedString(scenario, "stockMissingPolicy"),
                TcScheduleConstants.DEFAULT_STOCK_MISSING_POLICY);
        assertEquals("SNAPSHOT_PERSIST", TcScheduleStepEnum.SNAPSHOT_BUILD.getCode());
    }

    /**
     * 构造包含参数和六班时间窗的测试上下文。
     *
     * @param scenario JSON 场景
     * @return 隔离外部环境的内存排程上下文
     */
    private TcScheduleContext buildContext(TcAutoPlanJsonScenario scenario) {
        TcScheduleContext context = new TcScheduleContext();
        context.setFactoryCode("116");
        context.setScheduleDate(DateUtil.parseDate("2026-07-14"));
        context.setBatchNo("TC-JSON-TEST");
        context.setTraceId(scenario.getCaseName());
        Map<String, String> params = new HashMap<>(scenario.getParams());
        params.putIfAbsent(TcScheduleConstants.PARAM_MACHINE_FILTER_STRATEGY, "DEFAULT");
        params.putIfAbsent(TcScheduleConstants.PARAM_MACHINE_SCORE_STRATEGY, "DEFAULT");
        params.putIfAbsent(TcScheduleConstants.PARAM_CHAIN_TASK_PRIORITY_STRATEGY, "CONTINUITY_FIRST");
        params.putIfAbsent(TcScheduleConstants.PARAM_SHIFT_MAX_CAPACITY,
                TcScheduleConstants.DEFAULT_SHIFT_MAX_CAPACITY);
        Map<String, TcParamValue> paramMap = params.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> this.buildParam(entry.getKey(), entry.getValue())));
        context.setParamMap(paramMap);
        for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
            TcShiftTimeWindow shiftTimeWindow = new TcShiftTimeWindow();
            shiftTimeWindow.setShiftOrder(shiftOrder);
            shiftTimeWindow.setShiftCode(scenario.getShiftCodes().getOrDefault(String.valueOf(shiftOrder),
                    "CLASS" + shiftOrder));
            shiftTimeWindow.setPlanStartTime(String.format("%02d:00:00", (shiftOrder - 1) * 4));
            shiftTimeWindow.setPlanEndTime(String.format("%02d:00:00", shiftOrder * 4 % 24));
            shiftTimeWindow.setShiftHours(BigDecimal.valueOf(4));
            context.getShiftTimeWindowMap().put(shiftOrder, shiftTimeWindow);
            context.getShiftHoursMap().put(shiftOrder, BigDecimal.valueOf(4));
        }
        return context;
    }

    /**
     * 构造测试参数快照。
     *
     * @param paramCode 参数编码
     * @param paramValue 参数值
     * @return 参数快照
     */
    private TcParamValue buildParam(String paramCode, String paramValue) {
        TcParamValue value = new TcParamValue();
        value.setParamCode(paramCode);
        value.setParamValue(paramValue);
        value.setDefaultValue(paramValue);
        value.setSource("JSON_TEST");
        return value;
    }

    /**
     * 对可空数值求和。
     *
     * @param left 左值
     * @param right 右值
     * @return 求和结果
     */
    private BigDecimal addDecimal(BigDecimal left, BigDecimal right) {
        return (left == null ? BigDecimal.ZERO : left).add(right == null ? BigDecimal.ZERO : right);
    }

    /**
     * 按键断言 BigDecimal；期望键不存在时跳过。
     *
     * @param scenario JSON 场景
     * @param key 期望键
     * @param actual 实际数值
     */
    private void assertDecimal(TcAutoPlanJsonScenario scenario, String key, BigDecimal actual) {
        if (!scenario.getExpected().containsKey(key)) {
            return;
        }
        Object expectedValue = scenario.getExpected().get(key);
        if (expectedValue == null) {
            assertNull(actual);
            return;
        }
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(String.valueOf(expectedValue)).compareTo(actual));
    }

    /**
     * 读取字符串类型期望值。
     *
     * @param scenario JSON 场景
     * @param key 期望键
     * @return 字符串期望值
     */
    private String expectedString(TcAutoPlanJsonScenario scenario, String key) {
        return String.valueOf(scenario.getExpected().get(key));
    }

    /**
     * 读取整数类型期望值。
     *
     * @param scenario JSON 场景
     * @param key 期望键
     * @return 整数期望值
     */
    private Integer expectedInteger(TcAutoPlanJsonScenario scenario, String key) {
        return Integer.valueOf(this.expectedString(scenario, key));
    }

    /**
     * 读取布尔类型期望值。
     *
     * @param scenario JSON 场景
     * @param key 期望键
     * @return 布尔期望值
     */
    private boolean expectedBoolean(TcAutoPlanJsonScenario scenario, String key) {
        return Boolean.parseBoolean(this.expectedString(scenario, key));
    }

    /**
     * 读取对象类型期望值。
     *
     * @param scenario JSON 场景
     * @param key 期望键
     * @return 对象期望值
     * @throws AssertionError 期望值不是对象时抛出
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> expectedMap(TcAutoPlanJsonScenario scenario, String key) {
        Object value = scenario.getExpected().get(key);
        assertTrue("期望字段必须为对象：" + key, value instanceof Map);
        return (Map<String, Object>) value;
    }
}
