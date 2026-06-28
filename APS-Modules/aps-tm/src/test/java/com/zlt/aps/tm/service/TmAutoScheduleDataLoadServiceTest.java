package com.zlt.aps.tm.service;

import cn.hutool.core.date.DateUtil;
import com.zlt.aps.tm.api.domain.entity.*;
import com.zlt.aps.tm.domain.vo.TmFormingDemandRowVo;
import com.zlt.aps.tm.domain.vo.TmWorkCalendarRowVo;
import com.zlt.aps.tm.engine.domain.TmMachineCandidate;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.mapper.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 胎面自动排程数据加载服务测试。
 *
 * <p>验证自动排程上下文中必须具备策略默认参数和候选机台基础数据。</p>
 */
@RunWith(MockitoJUnitRunner.class)
public class TmAutoScheduleDataLoadServiceTest {

    @Mock
    private TmParamsMapper tmParamsMapper;

    @Mock
    private TmMachineInfoMapper tmMachineInfoMapper;

    @Mock
    private TmAutoScheduleDataLoadMapper tmAutoScheduleDataLoadMapper;

    @Mock
    private TmMouthPlateMapper tmMouthPlateMapper;

    @Mock
    private TmGlueMachineRealMapper tmGlueMachineRealMapper;

    @Mock
    private TmSpecifyMachineMapper tmSpecifyMachineMapper;

    @Mock
    private TmMachineSpeedMapper tmMachineSpeedMapper;

    @Mock
    private TmMachineMaintenanceMapper tmMachineMaintenanceMapper;

    @Mock
    private TmCurlRollMapper tmCurlRollMapper;

    @Mock
    private TmStockMapper tmStockMapper;

    @Mock
    private TmScheduleResultMapper tmScheduleResultMapper;
    /**
     * 测试内容：验证数据加载会补齐默认策略参数并生成候选机台。
     * 测试场景：参数表无配置，机台表返回一台启用机台，成型需求为空。
     * 预期结果：上下文中存在默认策略参数和候选机台，废弃参数不再补默认值。
     *
     * @throws Exception 反射注入依赖或加载数据失败时由测试框架抛出
     */
    @Test
    public void loadAllDataShouldFillDefaultStrategyParamsAndMachineCandidates() throws Exception {
        // 准备被测服务并通过反射注入 mapper mock，避免启动 Spring 容器。
        TmAutoScheduleDataLoadService service = new TmAutoScheduleDataLoadService();
        setField(service, "tmParamsMapper", tmParamsMapper);
        setField(service, "tmMachineInfoMapper", tmMachineInfoMapper);
        setField(service, "tmAutoScheduleDataLoadMapper", tmAutoScheduleDataLoadMapper);
        // 参数表返回空，验证服务内部默认参数兜底逻辑。
        when(tmParamsMapper.selectList(any())).thenReturn(Collections.emptyList());
        // 机台表返回一台正常机台，验证候选机台基础数据写入上下文。
        TmMachineInfo machineInfo = new TmMachineInfo();
        machineInfo.setFactoryCode("F1");
        machineInfo.setMachineCode("TM01");
        machineInfo.setMachineStatus("1");
        machineInfo.setMaxCapacity(new BigDecimal("1000"));
        when(tmMachineInfoMapper.selectList(any())).thenReturn(Collections.singletonList(machineInfo));
        when(tmAutoScheduleDataLoadMapper.selectFormingDemandRows(any(), any())).thenReturn(Collections.emptyList());
        // 构造最小排程上下文，提供工厂、日期和操作人。
        TmScheduleContext context = new TmScheduleContext();
        context.setFactoryCode("F1");
        context.setScheduleDate(DateUtil.parseDate("2026-06-18"));
        context.setOperator("tester");

        // 执行数据加载。
        service.loadAllData(context);

        // 断言默认参数和候选机台写入上下文。
        assertEquals("DEFAULT", context.getParamMap().get("TM_PLAN_QTY_STRATEGY").getEffectiveValue());
        assertEquals("DEFAULT", context.getParamMap().get("TM_TASK_SORT_STRATEGY").getEffectiveValue());
        assertEquals("1", context.getParamMap().get("TM_ALGORITHM_SWITCH").getEffectiveValue());
        assertNull(context.getParamMap().get("DEMAND_QTY_CALCULATE_TYPE"));
        assertFalse(context.getMachineCandidateList().isEmpty());
        assertEquals("TM01", context.getMachineCandidateList().get(0).getMachineCode());
    }

    /**
     * 测试内容：验证参数、机台和工作日历等基础数据会复用短 TTL 缓存。
     * 测试场景：连续两次使用同工厂同日期上下文调用数据加载。
     * 预期结果：参数和机台 mapper 只查询一次，第二次命中缓存。
     *
     * @throws Exception 反射注入依赖或加载数据失败时由测试框架抛出
     */
    @Test
    public void loadAllDataShouldReuseShortTtlCacheForParamsMachinesAndCalendar() throws Exception {
        // 准备被测服务和 mock 依赖。
        TmAutoScheduleDataLoadService service = new TmAutoScheduleDataLoadService();
        setField(service, "tmParamsMapper", tmParamsMapper);
        setField(service, "tmMachineInfoMapper", tmMachineInfoMapper);
        setField(service, "tmAutoScheduleDataLoadMapper", tmAutoScheduleDataLoadMapper);
        // 参数和机台数据固定返回，用于验证重复调用时不会重复查询。
        when(tmParamsMapper.selectList(any())).thenReturn(Collections.emptyList());
        TmMachineInfo machineInfo = new TmMachineInfo();
        machineInfo.setFactoryCode("F1");
        machineInfo.setMachineCode("TM01");
        machineInfo.setMachineStatus("1");
        when(tmMachineInfoMapper.selectList(any())).thenReturn(Collections.singletonList(machineInfo));
        when(tmAutoScheduleDataLoadMapper.selectFormingDemandRows(any(), any())).thenReturn(Collections.emptyList());

        // 构造两个相同工厂、日期的上下文，模拟短时间内重复自动排程加载。
        TmScheduleContext first = buildContext();
        TmScheduleContext second = buildContext();

        // 连续执行两次加载。
        service.loadAllData(first);
        service.loadAllData(second);

        // 断言公共基础数据只查询一次，说明缓存生效。
        verify(tmParamsMapper, times(1)).selectList(any());
        verify(tmMachineInfoMapper, times(1)).selectList(any());
    }

    /**
     * 测试内容：验证算法 2 在成型班次偏移量为 0 时会为 1-6 班生成对应胎面任务。
     * 测试场景：成型需求返回一条订单，6 个班次计划量分别为 10 到 60，工作日历为空表示默认开班。
     * 预期结果：生成 6 条任务，班次、订单号和需求量按同序号成型班次映射正确。
     *
     * @throws Exception 反射注入依赖或加载数据失败时由测试框架抛出
     */
    @Test
    public void loadAllDataShouldBuildTasksForEveryShift() throws Exception {
        // 准备算法 2 参数、同序号成型班次偏移量和公共机台 mock。
        TmAutoScheduleDataLoadService service = buildServiceWithCommonMocks(Collections.singletonList(algorithmParam("2")));
        // 成型需求返回 1-6 班数量，用于验证任务拆分。
        when(tmAutoScheduleDataLoadMapper.selectFormingDemandRows(any(), any()))
                .thenReturn(Collections.singletonList(buildDemandRow("ORD-ALL", "10", "20", "30", "40", "50", "60")));
        // 工作日历为空，表示不触发停产重分配。
        when(tmAutoScheduleDataLoadMapper.selectWorkCalendarRows(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        TmScheduleContext context = buildContext();

        // 执行数据加载和任务生成。
        service.loadAllData(context);

        // 断言 1-6 班都生成任务，并按同序号成型班次映射需求量。
        assertEquals(6, context.getTaskDraftList().size());
        for (int shiftOrder = 1; shiftOrder <= 6; shiftOrder++) {
            TmTaskDraft task = context.getTaskDraftList().get(shiftOrder - 1);
            assertEquals(Integer.valueOf(shiftOrder), task.getShiftOrder());
            assertEquals("ORD-ALL-CLASS" + shiftOrder, task.getOrderNo());
            assertBigDecimalEquals(new BigDecimal(shiftOrder * 10), task.getDemandQty());
        }
    }

    /**
     * 测试内容：验证同胎面、同胶料、同口型板的成型需求保留原来源任务。
     * 测试场景：两个成型工单对应同一个胎面规格，算法 2 按下一班需求生成任务。
     * 预期结果：生成 12 条胎面任务，同班次来源工单号和业务键均保持独立。
     *
     * @throws Exception 反射注入依赖或加载数据失败时由测试框架抛出
     */
    @Test
    public void loadAllDataShouldKeepSameTreadSourceTasksBeforeResultMerge() throws Exception {
        // 准备算法 2 和两条同胎面成型需求，验证解释追踪需要保留原成型来源粒度。
        TmAutoScheduleDataLoadService service = buildServiceWithCommonMocks(Collections.singletonList(algorithmParam("2")));
        when(tmAutoScheduleDataLoadMapper.selectFormingDemandRows(any(), any()))
                .thenReturn(Arrays.asList(
                        buildDemandRow("CX-ORD-001", "10", "20", "30", "40", "50", "60"),
                        buildDemandRow("CX-ORD-002", "5", "7", "9", "11", "13", "15")));
        when(tmAutoScheduleDataLoadMapper.selectWorkCalendarRows(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        TmScheduleContext context = buildContext();

        // 执行数据加载。
        service.loadAllData(context);

        // 断言同规格不在数据加载阶段聚合，1 班保留两个来源任务，便于解释表追溯原成型排程。
        assertEquals(12, context.getTaskDraftList().size());
        List<TmTaskDraft> firstShiftTaskList = context.getTaskDraftList().stream()
                .filter(task -> Integer.valueOf(1).equals(task.getShiftOrder()))
                .collect(Collectors.toList());
        assertEquals(2, firstShiftTaskList.size());
        assertBigDecimalEquals(new BigDecimal("10"), firstShiftTaskList.get(0).getDemandQty());
        assertBigDecimalEquals(new BigDecimal("5"), firstShiftTaskList.get(1).getDemandQty());
        assertEquals("CX-ORD-001", firstShiftTaskList.get(0).getSourceOrderNos());
        assertEquals("CX-ORD-002", firstShiftTaskList.get(1).getSourceOrderNos());
        assertNotEquals(firstShiftTaskList.get(0).getBusinessKey(), firstShiftTaskList.get(1).getBusinessKey());
        assertEquals("TR-215-001", firstShiftTaskList.get(0).getTreadCode());
        assertEquals("GL-A", firstShiftTaskList.get(0).getGlueCode());
        assertEquals("MP-A", firstShiftTaskList.get(0).getMouthPlateCode());
    }

    /**
     * 测试内容：验证算法 1 从需求起点连续 3 个成型班次取最大值生成每班需求。
     * 测试场景：成型需求 1-6 班分别为 10 到 60，算法开关为 1。
     * 预期结果：6 个排程班次的当前需求量按同序号起点连续 3 班最大值计算。
     *
     * @throws Exception 反射注入依赖或加载数据失败时由测试框架抛出
     */
    @Test
    public void loadAllDataShouldUseAlgorithmOneMaxFromFirstThreeShifts() throws Exception {
        // 准备算法 1 和一条完整成型需求。
        TmAutoScheduleDataLoadService service = buildServiceWithCommonMocks(Collections.singletonList(algorithmParam("1")));
        when(tmAutoScheduleDataLoadMapper.selectFormingDemandRows(any(), any()))
                .thenReturn(Collections.singletonList(buildDemandRow("ORD-A1", "10", "20", "30", "40", "50", "60")));
        when(tmAutoScheduleDataLoadMapper.selectWorkCalendarRows(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        TmScheduleContext context = buildContext();

        // 执行数据加载。
        service.loadAllData(context);

        // 断言算法 1 从每个班次的需求起点开始连续 3 班取最大成型量。
        assertEquals(6, context.getTaskDraftList().size());
        BigDecimal[] expectedDemandQtyArray = new BigDecimal[]{
                new BigDecimal("30"), new BigDecimal("40"), new BigDecimal("50"),
                new BigDecimal("60"), new BigDecimal("60"), new BigDecimal("60")
        };
        for (int index = 0; index < expectedDemandQtyArray.length; index++) {
            assertBigDecimalEquals(expectedDemandQtyArray[index], context.getTaskDraftList().get(index).getCurrentShiftDemandQty());
        }
    }

    /**
     * 测试内容：验证成型查询异常时返回空任务列表。
     * 测试场景：成型需求 mapper 抛出运行时异常。
     * 预期结果：数据加载不中断，taskDraftList 为空。
     *
     * @throws Exception 反射注入依赖或加载数据失败时由测试框架抛出
     */
    @Test
    public void loadAllDataShouldReturnEmptyTasksWhenFormingQueryFails() throws Exception {
        // 准备公共基础数据，成型查询抛异常。
        TmAutoScheduleDataLoadService service = buildServiceWithCommonMocks(Collections.singletonList(algorithmParam("2")));
        when(tmAutoScheduleDataLoadMapper.selectFormingDemandRows(any(), any()))
                .thenThrow(new RuntimeException("forming query failed"));
        TmScheduleContext context = buildContext();

        // 执行数据加载。
        service.loadAllData(context);

        // 断言异常被降级为无任务，不影响参数和候选机台加载。
        assertTrue(context.getTaskDraftList().isEmpty());
        assertFalse(context.getMachineCandidateList().isEmpty());
    }

    /**
     * 测试内容：验证无启用机台时候选机台为空。
     * 测试场景：机台 mapper 返回空列表，成型需求为空。
     * 预期结果：上下文候选机台列表为空。
     *
     * @throws Exception 反射注入依赖或加载数据失败时由测试框架抛出
     */
    @Test
    public void loadAllDataShouldKeepMachineCandidateEmptyWhenNoMachineExists() throws Exception {
        // 准备只注入三类基础 mapper 的服务。
        TmAutoScheduleDataLoadService service = new TmAutoScheduleDataLoadService();
        setField(service, "tmParamsMapper", tmParamsMapper);
        setField(service, "tmMachineInfoMapper", tmMachineInfoMapper);
        setField(service, "tmAutoScheduleDataLoadMapper", tmAutoScheduleDataLoadMapper);
        when(tmParamsMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(tmMachineInfoMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(tmAutoScheduleDataLoadMapper.selectFormingDemandRows(any(), any())).thenReturn(Collections.emptyList());
        TmScheduleContext context = buildContext();

        // 执行数据加载。
        service.loadAllData(context);

        // 断言无启用机台时不生成候选机台。
        assertTrue(context.getMachineCandidateList().isEmpty());
    }

    /**
     * 测试内容：验证成型关联施工关键字段缺失时聚合全部错误。
     * 测试场景：同一成型需求缺胎面编码、胎面长、口型板和胶料。
     * 预期结果：一次性抛出包含四类缺失信息的异常。
     *
     * @throws Exception 反射注入依赖或加载数据失败时由测试框架抛出
     */
    @Test
    public void loadAllDataShouldAggregateMissingDemandFieldErrors() throws Exception {
        // 准备缺少关键字段的成型需求。
        TmAutoScheduleDataLoadService service = buildServiceWithCommonMocks(Collections.singletonList(algorithmParam("2")));
        TmFormingDemandRowVo row = new TmFormingDemandRowVo();
        row.setEmbryoCode("CX-EMPTY");
        row.setClass1PlanQty(BigDecimal.TEN);
        when(tmAutoScheduleDataLoadMapper.selectFormingDemandRows(any(), any()))
                .thenReturn(Collections.singletonList(row));
        TmScheduleContext context = buildContext();

        try {
            // 执行数据加载时应聚合字段缺失异常。
            service.loadAllData(context);
            fail("缺少关键字段时应抛出异常");
        } catch (RuntimeException ex) {
            // 断言异常中包含所有关键字段缺失说明。
            assertTrue(ex.getMessage().contains("胎面代码为空"));
            assertTrue(ex.getMessage().contains("胎面长为空"));
            assertTrue(ex.getMessage().contains("胎面口型板为空"));
            assertTrue(ex.getMessage().contains("胎面胶料为空"));
        }
    }

    /**
     * 测试内容：验证零需求班次会被跳过。
     * 测试场景：算法 2 下 1-6 班成型需求全部为 0。
     * 预期结果：不生成任务草稿。
     *
     * @throws Exception 反射注入依赖或加载数据失败时由测试框架抛出
     */
    @Test
    public void loadAllDataShouldSkipZeroDemandRows() throws Exception {
        // 准备全部为零的成型需求。
        TmAutoScheduleDataLoadService service = buildServiceWithCommonMocks(Collections.singletonList(algorithmParam("2")));
        when(tmAutoScheduleDataLoadMapper.selectFormingDemandRows(any(), any()))
                .thenReturn(Collections.singletonList(buildDemandRow("ORD-ZERO", "0", "0", "0", "0", "0", "0")));
        when(tmAutoScheduleDataLoadMapper.selectWorkCalendarRows(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        TmScheduleContext context = buildContext();

        // 执行数据加载。
        service.loadAllData(context);

        // 断言零需求不进入后续排程任务。
        assertTrue(context.getTaskDraftList().isEmpty());
    }

    /**
     * 测试内容：验证口型、胶料、定点、禁排、速度、检修和卷长数据会补齐到上下文。
     * 测试场景：一台启用机台配置完整候选规则，一条成型需求配置卷长。
     * 预期结果：候选机台和任务草稿都带上对应辅助数据。
     *
     * @throws Exception 反射注入依赖或加载数据失败时由测试框架抛出
     */
    @Test
    public void loadAllDataShouldFillCandidateRulesSpeedMaintenanceAndCurlLength() throws Exception {
        // 准备注入全部候选规则 mapper 的服务。
        TmAutoScheduleDataLoadService service = buildServiceWithCandidateMocks(Collections.singletonList(algorithmParam("2")));
        when(tmAutoScheduleDataLoadMapper.selectFormingDemandRows(any(), any()))
                .thenReturn(Collections.singletonList(buildDemandRow("ORD-AUX", "0", "10", "0", "0", "0", "0")));
        when(tmAutoScheduleDataLoadMapper.selectWorkCalendarRows(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(tmMouthPlateMapper.selectList(any())).thenReturn(Collections.singletonList(mouthPlate("TM01", "MP-A")));
        when(tmGlueMachineRealMapper.selectList(any())).thenReturn(Collections.singletonList(glueRule("TM01", "GL-FORBID")));
        when(tmSpecifyMachineMapper.selectList(any())).thenReturn(Arrays.asList(
                specifyRule("TM01", "TR-ALLOW", "0"), specifyRule("TM01", "TR-FORBID", "1")));
        when(tmMachineSpeedMapper.selectList(any())).thenReturn(Arrays.asList(
                machineSpeed("TM01", null, "120"), machineSpeed(null, "TR-215-001", "80")));
        when(tmMachineMaintenanceMapper.selectList(any())).thenReturn(Collections.singletonList(maintenance("TM01")));
        when(tmCurlRollMapper.selectList(any())).thenReturn(Collections.singletonList(curlRoll("TR-215-001", "220")));
        TmScheduleContext context = buildContext();

        // 执行数据加载。
        service.loadAllData(context);

        // 断言候选机台规则和任务卷长均已补齐。
        assertEquals(1, context.getMachineCandidateList().size());
        TmMachineCandidate candidate = context.getMachineCandidateList().get(0);
        assertTrue(candidate.getMouthPlateCodes().contains("MP-A"));
        assertTrue(candidate.getForbiddenGlueCodes().contains("GL-FORBID"));
        assertTrue(candidate.getFixedAllowTreadCodes().contains("TR-ALLOW"));
        assertTrue(candidate.getFixedForbidTreadCodes().contains("TR-FORBID"));
        assertBigDecimalEquals(new BigDecimal("120"), candidate.getMachineSpeed());
        assertBigDecimalEquals(new BigDecimal("80"), candidate.getTreadSpeedMap().get("TR-215-001"));
        assertBigDecimalEquals(new BigDecimal("2.500000"), candidate.getMaintenanceHours());
        assertEquals(1, context.getTaskDraftList().size());
        assertBigDecimalEquals(new BigDecimal("220"), context.getTaskDraftList().get(0).getCurlRollLength());
    }

    /**
     * 测试内容：验证胎面某班停产时，需求会重分配到同日其他可排班次。
     * 测试场景：胎面日历关闭 1 班和 4 班对应班次，成型日历全部开班。
     * 预期结果：关闭班次不生成正常任务，停产需求均摊到 2、3、5、6 班。
     *
     * @throws Exception 反射注入依赖或加载数据失败时由测试框架抛出
     */
    @Test
    public void loadAllDataShouldRedistributeShutdownShiftDemandToAvailableShifts() throws Exception {
        // 准备算法 2 和一条含 6 班需求的成型订单。
        TmAutoScheduleDataLoadService service = buildServiceWithCommonMocks(Collections.singletonList(algorithmParam("2")));
        when(tmAutoScheduleDataLoadMapper.selectFormingDemandRows(any(), any()))
                .thenReturn(Collections.singletonList(buildDemandRow("ORD-SHUT", "10", "20", "30", "40", "50", "60")));
        // 第一次返回胎面日历，第二次返回成型日历；胎面 1 班和 4 班关闭，成型全部开班。
        when(tmAutoScheduleDataLoadMapper.selectWorkCalendarRows(any(), any(), any()))
                .thenReturn(Collections.singletonList(workCalendar("0", "0", "1", "1")),
                        Collections.singletonList(workCalendar("1", "1", "1", "1")));
        TmScheduleContext context = buildContext();

        // 执行数据加载，触发停产班次重分配。
        service.loadAllData(context);

        // 断言仅生成可排班次任务，并验证停产需求均摊后的需求量。
        assertEquals(4, context.getTaskDraftList().size());
        assertShiftDemand(context.getTaskDraftList(), 2, new BigDecimal("32.500000"));
        assertShiftDemand(context.getTaskDraftList(), 3, new BigDecimal("42.500000"));
        assertShiftDemand(context.getTaskDraftList(), 5, new BigDecimal("62.500000"));
        assertShiftDemand(context.getTaskDraftList(), 6, new BigDecimal("72.500000"));
    }

    /**
     * 测试内容：验证胎面 6 个班次全部停产且无可分配班次时生成未排任务。
     * 测试场景：胎面日历全关，成型日历全开。
     * 预期结果：6 条任务全部带 TM_SHUTDOWN_NO_AVAILABLE_SHIFT 未排原因。
     *
     * @throws Exception 反射注入依赖或加载数据失败时由测试框架抛出
     */
    @Test
    public void loadAllDataShouldMarkEveryShiftUnplannedWhenTmShutdownHasNoAvailableShift() throws Exception {
        // 准备算法 2 和一条含 6 班需求的成型订单。
        TmAutoScheduleDataLoadService service = buildServiceWithCommonMocks(Collections.singletonList(algorithmParam("2")));
        when(tmAutoScheduleDataLoadMapper.selectFormingDemandRows(any(), any()))
                .thenReturn(Collections.singletonList(buildDemandRow("ORD-NONE", "10", "20", "30", "40", "50", "60")));
        // 胎面日历全停产，成型日历全开，验证无法重分配时进入未排。
        when(tmAutoScheduleDataLoadMapper.selectWorkCalendarRows(any(), any(), any()))
                .thenReturn(Collections.singletonList(workCalendar("0", "0", "0", "0")),
                        Collections.singletonList(workCalendar("1", "1", "1", "1")));
        TmScheduleContext context = buildContext();

        // 执行数据加载，触发全班次停产未排逻辑。
        service.loadAllData(context);

        // 断言每个班次都生成未排任务，并给出明确未排原因。
        assertEquals(6, context.getTaskDraftList().size());
        for (TmTaskDraft task : context.getTaskDraftList()) {
            assertEquals("TM_SHUTDOWN_NO_AVAILABLE_SHIFT", task.getUnplannedReasonCode());
            assertEquals("胎面停产且无可分配班次，成型需求无法重分配", task.getUnplannedReasonDesc());
        }
    }

    /**
     * 测试内容：验证停产重分配开关关闭时不重分配。
     * 测试场景：胎面全停产、成型开班，但参数关闭停产重分配。
     * 预期结果：按原始需求生成任务，且不写停产未排原因。
     *
     * @throws Exception 反射注入依赖或加载数据失败时由测试框架抛出
     */
    @Test
    public void loadAllDataShouldSkipShutdownRedistributionWhenSwitchOff() throws Exception {
        // 准备关闭停产重分配参数。
        TmAutoScheduleDataLoadService service = buildServiceWithCommonMocks(Arrays.asList(
                algorithmParam("2"), param("TM_SHUTDOWN_REDISTRIBUTION_ENABLED", "0")));
        when(tmAutoScheduleDataLoadMapper.selectFormingDemandRows(any(), any()))
                .thenReturn(Collections.singletonList(buildDemandRow("ORD-SWITCH", "10", "20", "30", "40", "50", "60")));
        when(tmAutoScheduleDataLoadMapper.selectWorkCalendarRows(any(), any(), any()))
                .thenReturn(Collections.singletonList(workCalendar("0", "0", "0", "0")),
                        Collections.singletonList(workCalendar("1", "1", "1", "1")));
        TmScheduleContext context = buildContext();

        // 执行数据加载。
        service.loadAllData(context);

        // 断言关闭开关后不做停产重分配，也不标记未排。
        assertEquals(6, context.getTaskDraftList().size());
        assertTrue(context.getTaskDraftList().stream().allMatch(task -> task.getUnplannedReasonCode() == null));
    }

    /**
     * 测试内容：验证成型也停产时不触发胎面停产重分配。
     * 测试场景：胎面、成型日历均全停产。
     * 预期结果：按原始需求生成任务，且不写停产未排原因。
     *
     * @throws Exception 反射注入依赖或加载数据失败时由测试框架抛出
     */
    @Test
    public void loadAllDataShouldSkipShutdownRedistributionWhenCxAlsoShutdown() throws Exception {
        // 准备算法 2 和一条完整需求。
        TmAutoScheduleDataLoadService service = buildServiceWithCommonMocks(Collections.singletonList(algorithmParam("2")));
        when(tmAutoScheduleDataLoadMapper.selectFormingDemandRows(any(), any()))
                .thenReturn(Collections.singletonList(buildDemandRow("ORD-CX-SHUT", "10", "20", "30", "40", "50", "60")));
        when(tmAutoScheduleDataLoadMapper.selectWorkCalendarRows(any(), any(), any()))
                .thenReturn(Collections.singletonList(workCalendar("0", "0", "0", "0")),
                        Collections.singletonList(workCalendar("0", "0", "0", "0")));
        TmScheduleContext context = buildContext();

        // 执行数据加载。
        service.loadAllData(context);

        // 断言成型也停产时不做胎面侧重分配。
        assertEquals(6, context.getTaskDraftList().size());
        assertTrue(context.getTaskDraftList().stream().allMatch(task -> task.getUnplannedReasonCode() == null));
    }

    /**
     * 通过反射注入测试依赖，避免启动 Spring 容器。
     *
     * @param target    被测服务
     * @param fieldName 字段名
     * @param value     字段值
     * @throws Exception 字段不存在或不可访问时抛出
     */
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = TmAutoScheduleDataLoadService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private TmAutoScheduleDataLoadService buildServiceWithCommonMocks(List<TmParams> paramsList) throws Exception {
        TmAutoScheduleDataLoadService service = new TmAutoScheduleDataLoadService();
        setField(service, "tmParamsMapper", tmParamsMapper);
        setField(service, "tmMachineInfoMapper", tmMachineInfoMapper);
        setField(service, "tmAutoScheduleDataLoadMapper", tmAutoScheduleDataLoadMapper);
        setField(service, "tmStockMapper", tmStockMapper);
        setField(service, "tmScheduleResultMapper", tmScheduleResultMapper);
        when(tmParamsMapper.selectList(any())).thenReturn(withLegacyFormingShiftOffset(paramsList));
        when(tmStockMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(tmScheduleResultMapper.selectList(any()))
                .thenReturn(Collections.singletonList(historyScheduleResult("TR-215-001")));
        TmMachineInfo machineInfo = new TmMachineInfo();
        machineInfo.setFactoryCode("F1");
        machineInfo.setMachineCode("TM01");
        machineInfo.setMachineStatus("1");
        machineInfo.setMaxCapacity(new BigDecimal("1000"));
        when(tmMachineInfoMapper.selectList(any())).thenReturn(Collections.singletonList(machineInfo));
        return service;
    }

    private TmAutoScheduleDataLoadService buildServiceWithCandidateMocks(List<TmParams> paramsList) throws Exception {
        TmAutoScheduleDataLoadService service = buildServiceWithCommonMocks(paramsList);
        setField(service, "tmMouthPlateMapper", tmMouthPlateMapper);
        setField(service, "tmGlueMachineRealMapper", tmGlueMachineRealMapper);
        setField(service, "tmSpecifyMachineMapper", tmSpecifyMachineMapper);
        setField(service, "tmMachineSpeedMapper", tmMachineSpeedMapper);
        setField(service, "tmMachineMaintenanceMapper", tmMachineMaintenanceMapper);
        setField(service, "tmCurlRollMapper", tmCurlRollMapper);
        return service;
    }

    /**
     * 为旧数据加载单测补齐成型班次偏移量。
     *
     * @param paramsList 原始参数列表
     * @return 带同序号偏移参数的测试参数列表
     */
    private List<TmParams> withLegacyFormingShiftOffset(List<TmParams> paramsList) {
        List<TmParams> effectiveParamsList = new ArrayList<>(paramsList);
        boolean configured = effectiveParamsList.stream()
                .anyMatch(params -> "TM_FORMING_SHIFT_OFFSET".equals(params.getParamCode()));
        if (!configured) {
            effectiveParamsList.add(param("TM_FORMING_SHIFT_OFFSET", "0"));
        }
        return effectiveParamsList;
    }
    private TmParams algorithmParam(String value) {
        return param("TM_ALGORITHM_SWITCH", value);
    }

    private TmScheduleResult historyScheduleResult(String treadCode) {
        TmScheduleResult result = new TmScheduleResult();
        result.setFactoryCode("F1");
        result.setTreadCode(treadCode);
        result.setScheduleDate(DateUtil.parseDate("2026-06-17"));
        result.setClass1PlanQty(BigDecimal.ONE);
        result.setIsDelete(0);
        return result;
    }
    private TmParams param(String paramCode, String value) {
        TmParams params = new TmParams();
        params.setFactoryCode("F1");
        params.setParamCode(paramCode);
        params.setParamValue(value);
        params.setEnableStatus("1");
        return params;
    }

    private TmMouthPlate mouthPlate(String machineCode, String mouthPlateCode) {
        TmMouthPlate mouthPlate = new TmMouthPlate();
        mouthPlate.setFactoryCode("F1");
        mouthPlate.setMachineCode(machineCode);
        mouthPlate.setMouthPlateCode(mouthPlateCode);
        return mouthPlate;
    }

    private TmGlueMachineReal glueRule(String machineCode, String glueCode) {
        TmGlueMachineReal glueRule = new TmGlueMachineReal();
        glueRule.setFactoryCode("F1");
        glueRule.setMachineCode(machineCode);
        glueRule.setGlueCode(glueCode);
        glueRule.setAllowFlag("0");
        glueRule.setEnableStatus("1");
        return glueRule;
    }

    private TmSpecifyMachine specifyRule(String machineCode, String treadCode, String jobType) {
        TmSpecifyMachine specifyMachine = new TmSpecifyMachine();
        specifyMachine.setFactoryCode("F1");
        specifyMachine.setMachineCode(machineCode);
        specifyMachine.setTreadCode(treadCode);
        specifyMachine.setJobType(jobType);
        specifyMachine.setEnableStatus("1");
        return specifyMachine;
    }

    private TmMachineSpeed machineSpeed(String machineCode, String treadCode, String productSpeed) {
        TmMachineSpeed speed = new TmMachineSpeed();
        speed.setFactoryCode("F1");
        speed.setMachineCode(machineCode);
        speed.setTreadCode(treadCode);
        speed.setProductSpeed(new BigDecimal(productSpeed));
        return speed;
    }

    private TmMachineMaintenance maintenance(String machineCode) {
        TmMachineMaintenance maintenance = new TmMachineMaintenance();
        maintenance.setFactoryCode("F1");
        maintenance.setMachineCode(machineCode);
        maintenance.setStopStartTime(DateUtil.parse("2026-06-18 10:00:00"));
        maintenance.setStopEndTime(DateUtil.parse("2026-06-18 12:30:00"));
        return maintenance;
    }

    private TmCurlRoll curlRoll(String treadCode, String curlLength) {
        TmCurlRoll curlRoll = new TmCurlRoll();
        curlRoll.setFactoryCode("F1");
        curlRoll.setTreadCode(treadCode);
        curlRoll.setCurlLength(new BigDecimal(curlLength));
        return curlRoll;
    }

    private TmFormingDemandRowVo buildDemandRow(String orderNo, String... classQty) {
        TmFormingDemandRowVo row = new TmFormingDemandRowVo();
        row.setOrderNo(orderNo);
        row.setTreadCode("TR-215-001");
        row.setTreadShoulderLength(BigDecimal.ONE);
        row.setTreadMouthPlate("MP-A");
        row.setTreadRubberCategory("GL-A");
        List<String> qtyList = Arrays.asList(classQty);
        row.setClass1PlanQty(new BigDecimal(qtyList.get(0)));
        row.setClass2PlanQty(new BigDecimal(qtyList.get(1)));
        row.setClass3PlanQty(new BigDecimal(qtyList.get(2)));
        row.setClass4PlanQty(new BigDecimal(qtyList.get(3)));
        row.setClass5PlanQty(new BigDecimal(qtyList.get(4)));
        row.setClass6PlanQty(new BigDecimal(qtyList.get(5)));
        return row;
    }

    private TmWorkCalendarRowVo workCalendar(String dayFlag, String oneShiftFlag,
                                             String twoShiftFlag, String threeShiftFlag) {
        TmWorkCalendarRowVo row = new TmWorkCalendarRowVo();
        row.setDayFlag(dayFlag);
        row.setOneShiftFlag(oneShiftFlag);
        row.setTwoShiftFlag(twoShiftFlag);
        row.setThreeShiftFlag(threeShiftFlag);
        return row;
    }

    private void assertShiftDemand(List<TmTaskDraft> taskList, int shiftOrder, BigDecimal expected) {
        TmTaskDraft task = taskList.stream()
                .filter(item -> Integer.valueOf(shiftOrder).equals(item.getShiftOrder()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到班次任务：" + shiftOrder));
        assertBigDecimalEquals(expected, task.getDemandQty());
    }

    private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual));
    }

    private TmScheduleContext buildContext() {
        TmScheduleContext context = new TmScheduleContext();
        context.setFactoryCode("F1");
        context.setScheduleDate(DateUtil.parseDate("2026-06-18"));
        context.setOperator("tester");
        return context;
    }
}
