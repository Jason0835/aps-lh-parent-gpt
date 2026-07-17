package com.zlt.aps.tc.service.loader;

import cn.hutool.core.date.DateUtil;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;
import com.zlt.aps.tc.api.domain.entity.TcParams;
import com.zlt.aps.tc.api.domain.entity.TcStock;
import com.zlt.aps.tc.api.enums.TcConstructionStageEnum;
import com.zlt.aps.tc.api.enums.TcScheduleRuleCodeEnum;
import com.zlt.aps.tc.api.enums.TcVersionMatchModeEnum;
import com.zlt.aps.tc.api.enums.TcYesNoEnum;
import com.zlt.aps.tc.domain.vo.*;
import com.zlt.aps.tc.engine.domain.TcRuleTraceItem;
import com.zlt.aps.tc.engine.domain.TcScheduleContext;
import com.zlt.aps.tc.engine.domain.TcTaskDraft;
import com.zlt.aps.tc.mapper.*;
import com.zlt.aps.tc.service.cache.TcAutoScheduleRedisCacheService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 胎侧自动排程数据加载规则测试。
 *
 * <p>使用 Mock Mapper 覆盖 RECIPE/B 施工版本、新规格、实验规格和停产重分配，
 * 不连接数据库、Redis、MES 或其他外部服务。</p>
 */
@RunWith(MockitoJUnitRunner.class)
public class TcAutoScheduleDataLoadServiceTest {

    @Mock
    private TcParamsMapper paramsMapper;

    @Mock
    private TcMachineInfoMapper machineInfoMapper;

    @Mock
    private TcAutoScheduleDataLoadMapper dataLoadMapper;

    @Mock
    private TcStockMapper stockMapper;

    @Mock
    private TcScheduleResultMapper scheduleResultMapper;

    @Mock
    private TcDepthConfigMapper depthConfigMapper;

    @Mock
    private TcLossSettingMapper lossSettingMapper;

    /**
     * 验证 B 模式择一施工版本、回退证据、胶料拆分和新规格提前窗口。
     *
     * @throws Exception 反射注入测试依赖失败时抛出
     */
    @Test
    public void shouldLoadBModeAndAdvanceNewSpecWithFallbackEvidence() throws Exception {
        TcAutoScheduleDataLoadService service = this.buildService(this.buildCommonParams("B"));
        TcFormingDemandRowVo row = this.buildBomRow("ORD-B", "SW-B");
        row.setClass3PlanQty(new BigDecimal("10"));
        row.setSidewallLength(new BigDecimal("2"));
        row.setBomDataVersion("BOM-REQUESTED");
        row.setConstructionVersion("BOM-FALLBACK");
        row.setSidewallRubber(" MAIN , BASE-A , BASE-B ");
        when(this.dataLoadMapper.selectFormingDemandRows(any(), any()))
                .thenReturn(Collections.singletonList(row));

        TcScheduleContext context = this.buildContext();
        service.loadAllData(context);

        assertEquals(1, context.getTaskDraftList().size());
        TcTaskDraft task = context.getTaskDraftList().get(0);
        assertEquals(Integer.valueOf(1), task.getShiftOrder());
        assertEquals("MAIN", task.getGlueCode());
        assertEquals("BASE-A,BASE-B", task.getBaseGlueCode());
        assertTrue(task.getNewSpecInfo().isNewSpecHit());
        assertEquals(0, new BigDecimal("20").compareTo(task.getDemandQty()));
        TcRuleTraceItem versionHit = context.getRuleTraceMap().get(task.getBusinessKey()).getRuleHits().stream()
                .filter(item -> TcScheduleRuleCodeEnum.VERSION_MATCH.getCode().equals(item.getRuleCode()))
                .findFirst().orElseThrow(() -> new AssertionError("未找到版本匹配证据"));
        Map<?, ?> evidence = (Map<?, ?>) versionHit.getEvidence();
        assertEquals("B", evidence.get("mode"));
        assertEquals(Boolean.TRUE, evidence.get("fallback"));
    }

    /**
     * 验证 RECIPE 模式逐班匹配，示方书为空的班次跳过并记录结构化告警。
     *
     * @throws Exception 反射注入测试依赖失败时抛出
     */
    @Test
    public void shouldLoadRecipeModeAndSkipShiftWithoutRecipe() throws Exception {
        TcAutoScheduleDataLoadService service = this.buildService(this.buildCommonParams("RECIPE"));
        TcFormingDemandRecipeRowVo row = new TcFormingDemandRecipeRowVo();
        row.setOrderNo("ORD-RECIPE");
        row.setEmbryoCode("EMBRYO-1");
        row.setClass1PlanQty(new BigDecimal("10"));
        row.setClass2PlanQty(new BigDecimal("20"));
        row.setClass1RecipeNo("R-1");
        TcConstructionSidewallRowVo construction = this.buildConstruction("EMBRYO-1", "R-1", "SW-R");
        when(this.dataLoadMapper.selectFormingDemandRowsByRecipe(any(), any()))
                .thenReturn(Collections.singletonList(row));
        when(this.dataLoadMapper.selectConstructionInfoRows(any(), any(), any()))
                .thenReturn(Collections.singletonList(construction));

        TcScheduleContext context = this.buildContext();
        service.loadAllData(context);

        assertEquals(1, context.getTaskDraftList().size());
        TcTaskDraft task = context.getTaskDraftList().get(0);
        assertEquals("SW-R", task.getSidewallCode());
        assertEquals("R-1-SIDEWALL", task.getConstructionVersion());
        assertEquals(0, new BigDecimal("15.0").compareTo(task.getDemandQty()));
        assertFalse(context.getIssueCollector().getIssues().isEmpty());
    }

    /**
     * 验证施工关键字段异常时聚合阻断，不生成脏任务。
     *
     * @throws Exception 反射注入测试依赖失败时抛出
     */
    @Test
    public void shouldRejectInvalidConstructionFields() throws Exception {
        TcAutoScheduleDataLoadService service = this.buildService(this.buildCommonParams("B"));
        TcFormingDemandRowVo row = this.buildBomRow("ORD-INVALID", "SW-INVALID");
        row.setClass1PlanQty(BigDecimal.ONE);
        row.setSidewallMouthPlate(null);
        when(this.dataLoadMapper.selectFormingDemandRows(any(), any()))
                .thenReturn(Collections.singletonList(row));
        TcScheduleContext context = this.buildContext();

        try {
            service.loadAllData(context);
            fail("施工关键字段缺失时应阻断排程");
        } catch (RuntimeException exception) {
            assertNotNull(exception.getMessage());
        }

        assertFalse(context.getIssueCollector().getIssues().isEmpty());
    }

    /**
     * 验证月计划定稿实验规格在无普通成型任务时生成独立任务。
     *
     * @throws Exception 反射注入测试依赖失败时抛出
     */
    @Test
    public void shouldBuildIndependentExperimentSpecTask() throws Exception {
        TcAutoScheduleDataLoadService service = this.buildService(this.buildCommonParams("B"));
        when(this.dataLoadMapper.selectFormingDemandRows(any(), any())).thenReturn(Collections.emptyList());
        TcExperimentSpecMonthPlanRowVo experimentRow = new TcExperimentSpecMonthPlanRowVo();
        experimentRow.setMonthPlanId(1001L);
        experimentRow.setProductionNo("EXP-001");
        experimentRow.setFactoryCode("116");
        experimentRow.setExperimentPlanDate(DateUtil.parseDate("2026-07-09"));
        experimentRow.setDayQty(BigDecimal.ONE);
        experimentRow.setEmbryoCode("EMBRYO-EXP");
        experimentRow.setConstructionStage(TcConstructionStageEnum.EXPERIMENT.getCode());
        experimentRow.setSidewallCode("SW-EXP");
        experimentRow.setConstructionVersion("EXP-V1");
        experimentRow.setSidewallLength(BigDecimal.ONE);
        experimentRow.setSidewallMouthPlate("MP-EXP");
        experimentRow.setSidewallRubber("G-EXP, B-EXP");
        when(this.dataLoadMapper.selectExperimentSpecMonthPlanRows(any(), any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(experimentRow));

        TcScheduleContext context = this.buildContext();
        service.loadAllData(context);

        assertEquals(1, context.getTaskDraftList().size());
        TcTaskDraft task = context.getTaskDraftList().get(0);
        assertNotNull(task.getExperimentSpecInfo());
        assertFalse(task.getExperimentSpecInfo().getMergedToExistingTask());
        assertEquals("G-EXP", task.getGlueCode());
        assertEquals("B-EXP", task.getBaseGlueCode());
        assertEquals(0, new BigDecimal("30").compareTo(task.getPlanQty()));
    }

    /**
     * 验证胎侧停班而成型开班时，将关闭班次需求均摊到当前开放班次。
     *
     * @throws Exception 反射注入测试依赖失败时抛出
     */
    @Test
    public void shouldRedistributeShutdownDemandToOpenShifts() throws Exception {
        TcAutoScheduleDataLoadService service = this.buildService(this.buildCommonParams("B"));
        TcFormingDemandRowVo row = this.buildBomRow("ORD-SHUTDOWN", "SW-SHUTDOWN");
        row.setClass1PlanQty(new BigDecimal("10"));
        row.setClass2PlanQty(new BigDecimal("20"));
        row.setClass3PlanQty(new BigDecimal("30"));
        row.setClass4PlanQty(new BigDecimal("40"));
        row.setClass5PlanQty(new BigDecimal("50"));
        row.setClass6PlanQty(new BigDecimal("60"));
        when(this.dataLoadMapper.selectFormingDemandRows(any(), any()))
                .thenReturn(Collections.singletonList(row));
        when(this.dataLoadMapper.selectWorkCalendarRows(any(), any(), any()))
                .thenReturn(Collections.singletonList(this.buildCalendar("0", "0", "1", "1")),
                        Collections.singletonList(this.buildCalendar("1", "1", "1", "1")));
        TcStock stock = new TcStock();
        stock.setSidewallCode("SW-SHUTDOWN");
        stock.setStockQty(BigDecimal.ONE);
        when(this.stockMapper.selectList(any())).thenReturn(Collections.singletonList(stock));

        TcScheduleContext context = this.buildContext();
        service.loadAllData(context);

        assertEquals(4, context.getTaskDraftList().size());
        this.assertShiftDemand(context.getTaskDraftList(), 2, "32.500000");
        this.assertShiftDemand(context.getTaskDraftList(), 3, "42.500000");
        this.assertShiftDemand(context.getTaskDraftList(), 5, "62.500000");
        this.assertShiftDemand(context.getTaskDraftList(), 6, "72.500000");
        assertTrue(context.getRuleTraceMap().values().stream()
                .flatMap(trace -> trace.getRuleHits().stream())
                .anyMatch(item -> TcScheduleRuleCodeEnum.CURRENT_DAY_SHUTDOWN_REDISTRIBUTION.getCode()
                        .equals(item.getRuleCode())));
    }

    /**
     * 验证版本模式对外使用 B，并兼容早期 BOM 配置值。
     */
    @Test
    public void shouldResolveBAndLegacyBomVersionMode() {
        assertEquals(TcVersionMatchModeEnum.B, TcVersionMatchModeEnum.resolve("B"));
        assertEquals(TcVersionMatchModeEnum.B, TcVersionMatchModeEnum.resolve("BOM"));
        assertEquals(TcVersionMatchModeEnum.RECIPE, TcVersionMatchModeEnum.resolve("UNKNOWN"));
    }

    /**
     * 构建被测服务并注入本轮测试需要的 Mapper。
     *
     * @param paramList 自动排程参数
     * @return 数据加载服务
     * @throws Exception 反射注入字段失败时抛出
     */
    private TcAutoScheduleDataLoadService buildService(List<TcParams> paramList) throws Exception {
        TcAutoScheduleDataLoadService service = new TcAutoScheduleDataLoadService(
                new TcAutoScheduleRedisCacheService());
        this.setField(service, "tmParamsMapper", this.paramsMapper);
        this.setField(service, "tmMachineInfoMapper", this.machineInfoMapper);
        this.setField(service, "tcAutoScheduleDataLoadMapper", this.dataLoadMapper);
        this.setField(service, "tmStockMapper", this.stockMapper);
        this.setField(service, "tcScheduleResultMapper", this.scheduleResultMapper);
        this.setField(service, "tmDepthConfigMapper", this.depthConfigMapper);
        this.setField(service, "tmLossSettingMapper", this.lossSettingMapper);
        when(this.paramsMapper.selectList(any())).thenReturn(paramList);
        TcMachineInfo machine = new TcMachineInfo();
        machine.setFactoryCode("116");
        machine.setMachineCode("TC01");
        machine.setMachineStatus(TcYesNoEnum.YES.getCode());
        machine.setMaxCapacity(new BigDecimal("5500"));
        when(this.machineInfoMapper.selectList(any())).thenReturn(Collections.singletonList(machine));
        lenient().when(this.stockMapper.selectList(any())).thenReturn(Collections.emptyList());
        lenient().when(this.scheduleResultMapper.selectList(any())).thenReturn(Collections.emptyList());
        lenient().when(this.depthConfigMapper.selectList(any())).thenReturn(Collections.emptyList());
        lenient().when(this.lossSettingMapper.selectList(any())).thenReturn(Collections.emptyList());
        lenient().when(this.dataLoadMapper.selectWorkCalendarRows(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        lenient().when(this.dataLoadMapper.selectWorkCalendarRowsByRange(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        lenient().when(this.dataLoadMapper.selectExperimentSpecMonthPlanRows(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        return service;
    }

    /**
     * 构造算法 2、同班成型偏移和指定版本模式参数。
     *
     * @param versionMode 版本匹配模式
     * @return 参数列表
     */
    private List<TcParams> buildCommonParams(String versionMode) {
        return Arrays.asList(
                this.buildParam(TcScheduleConstants.PARAM_VERSION_MATCH_MODE, versionMode),
                this.buildParam(TcScheduleConstants.PARAM_ALGORITHM_SWITCH, "2"),
                this.buildParam(TcScheduleConstants.PARAM_FORMING_SHIFT_OFFSET, "0"));
    }

    /**
     * 构造单个启用参数。
     *
     * @param paramCode 参数编码
     * @param paramValue 参数值
     * @return 参数实体
     */
    private TcParams buildParam(String paramCode, String paramValue) {
        TcParams params = new TcParams();
        params.setFactoryCode("116");
        params.setParamCode(paramCode);
        params.setParamValue(paramValue);
        params.setDefaultValue(paramValue);
        params.setEnableStatus(TcYesNoEnum.YES.getCode());
        return params;
    }

    /**
     * 构造 B 模式成型施工行。
     *
     * @param orderNo 成型工单号
     * @param sidewallCode 胎侧编码
     * @return 成型需求行
     */
    private TcFormingDemandRowVo buildBomRow(String orderNo, String sidewallCode) {
        TcFormingDemandRowVo row = new TcFormingDemandRowVo();
        row.setOrderNo(orderNo);
        row.setEmbryoCode("EMBRYO-" + sidewallCode);
        row.setSidewallCode(sidewallCode);
        row.setConstructionVersion("V1");
        row.setSidewallLength(BigDecimal.ONE);
        row.setSidewallMouthPlate("MP-1");
        row.setSidewallRubber("G-1,B-1");
        return row;
    }

    /**
     * 构造 RECIPE 模式施工胎侧行。
     *
     * @param embryoCode 胎胚编码
     * @param recipeNo 示方书编号
     * @param sidewallCode 胎侧编码
     * @return 施工胎侧行
     */
    private TcConstructionSidewallRowVo buildConstruction(String embryoCode, String recipeNo,
                                                            String sidewallCode) {
        TcConstructionSidewallRowVo row = new TcConstructionSidewallRowVo();
        row.setConstructionCode(embryoCode);
        row.setConstructionVersion(recipeNo);
        row.setSidewallVersion(recipeNo + "-SIDEWALL");
        row.setSidewallCode(sidewallCode);
        row.setSidewallLength(new BigDecimal("1.5"));
        row.setSidewallMouthPlate("MP-R");
        row.setSidewallRubber("G-R,B-R");
        return row;
    }

    /**
     * 构造工作日历行。
     *
     * @param dayFlag 工作日标识
     * @param oneShiftFlag 一班开班标识
     * @param twoShiftFlag 二班开班标识
     * @param threeShiftFlag 三班开班标识
     * @return 工作日历行
     */
    private TcWorkCalendarRowVo buildCalendar(String dayFlag, String oneShiftFlag,
                                               String twoShiftFlag, String threeShiftFlag) {
        TcWorkCalendarRowVo row = new TcWorkCalendarRowVo();
        row.setDayFlag(dayFlag);
        row.setOneShiftFlag(oneShiftFlag);
        row.setTwoShiftFlag(twoShiftFlag);
        row.setThreeShiftFlag(threeShiftFlag);
        return row;
    }

    /**
     * 构造最小排程上下文。
     *
     * @return 排程上下文
     */
    private TcScheduleContext buildContext() {
        TcScheduleContext context = new TcScheduleContext();
        context.setFactoryCode("116");
        context.setScheduleDate(DateUtil.parseDate("2026-07-14"));
        context.setBatchNo("TC-LOADER-TEST");
        context.setTraceId("TRACE-LOADER-TEST");
        context.setOperator("tester");
        return context;
    }

    /**
     * 断言指定班次的需求量。
     *
     * @param taskList 任务列表
     * @param shiftOrder 班次序号
     * @param expectedQty 期望需求量
     */
    private void assertShiftDemand(List<TcTaskDraft> taskList, int shiftOrder, String expectedQty) {
        TcTaskDraft task = taskList.stream()
                .filter(item -> Integer.valueOf(shiftOrder).equals(item.getShiftOrder()))
                .findFirst().orElseThrow(() -> new AssertionError("未找到班次任务：" + shiftOrder));
        assertEquals(0, new BigDecimal(expectedQty).compareTo(task.getDemandQty()));
    }

    /**
     * 通过反射注入测试依赖，避免启动 Spring 容器。
     *
     * @param target 被测服务
     * @param fieldName 字段名称
     * @param value 字段值
     * @throws Exception 字段不存在或不可访问时抛出
     */
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = TcAutoScheduleDataLoadService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
