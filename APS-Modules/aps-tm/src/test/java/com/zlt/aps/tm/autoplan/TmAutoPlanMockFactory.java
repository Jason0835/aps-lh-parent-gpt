package com.zlt.aps.tm.autoplan;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResultExplain;
import com.zlt.aps.tm.domain.vo.TmWorkCalendarRowVo;
import com.zlt.aps.tm.engine.domain.TmInventoryPredictQtyVo;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.engine.mapper.TmEngineInventoryPredictMapper;
import com.zlt.aps.tm.engine.mapper.TmEngineStockMapper;
import com.zlt.aps.tm.engine.service.ITmPlanBootstrapService;
import com.zlt.aps.tm.engine.service.TmScheduleOperationFacade;
import com.zlt.aps.tm.engine.service.TmScheduleProcessLogger;
import com.zlt.aps.tm.engine.service.impl.*;
import com.zlt.aps.tm.engine.strategy.*;
import com.zlt.aps.tm.engine.template.TmScheduleTemplateImpl;
import com.zlt.aps.tm.mapper.*;
import com.zlt.aps.tm.service.TmAutoScheduleDataLoadService;
import com.zlt.aps.tm.service.impl.TmBizSnapshotAndPersistService;
import com.zlt.aps.tm.service.impl.TmScheduleResultServiceImpl;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 胎面自动排程 JSON 场景 Mock 工厂。
 *
 * <p>根据本地 JSON 场景构造服务入口、模板步骤服务和 Mapper Mock。
 * 所有写入都会进入内存列表，禁止连接真实数据库、Redis、MES 或外部接口。</p>
 */
public class TmAutoPlanMockFactory {

    /**
     * 根据场景构造可执行的测试上下文。
     *
     * @param scenario JSON 场景
     * @return Mock 上下文
     */
    public MockContext create(TmAutoPlanScenario scenario) {
        MockContext mockContext = new MockContext(scenario);
        TmAutoScheduleDataLoadService dataLoadService = buildDataLoadService(scenario);
        mockContext.setDataLoadService(dataLoadService);

        TmScheduleResultMapper scheduleResultMapper = mock(TmScheduleResultMapper.class);
        TmScheduleResultExplainMapper explainMapper = mock(TmScheduleResultExplainMapper.class);
        TmScheduleUnplannedMapper unplannedMapper = mock(TmScheduleUnplannedMapper.class);
        TmDispatcherLogMapper dispatcherLogMapper = mock(TmDispatcherLogMapper.class);
        TmMachineInfoMapper serviceMachineInfoMapper = mock(TmMachineInfoMapper.class);
        TmScheduleOperationFacade operationFacade = mock(TmScheduleOperationFacade.class);

        when(scheduleResultMapper.selectList(any())).thenReturn(nullToEmpty(scenario.getOldScheduleResults()));
        when(scheduleResultMapper.insert(any(TmScheduleResult.class))).thenAnswer(invocation -> {
            if (Boolean.TRUE.equals(scenario.getMockResultInsertFailure())) {
                throw new RuntimeException("mock result insert failed");
            }
            TmScheduleResult result = invocation.getArgument(0);
            result.setId((long) mockContext.getInsertedResults().size() + 1L);
            mockContext.getInsertedResults().add(result);
            return 1;
        });
        when(explainMapper.insert(any(TmScheduleResultExplain.class))).thenAnswer(invocation -> {
            if (Boolean.TRUE.equals(scenario.getMockExplainInsertFailure())) {
                throw new RuntimeException("mock explain insert failed");
            }
            TmScheduleResultExplain explain = invocation.getArgument(0);
            mockContext.getInsertedExplains().add(explain);
            return 1;
        });

        TmScheduleTemplateImpl template = buildTemplate(scenario, dataLoadService, scheduleResultMapper,
                explainMapper, mockContext);

        TmScheduleResultServiceImpl service = new TmScheduleResultServiceImpl();
        setField(service, "tmScheduleResultMapper", scheduleResultMapper);
        setField(service, "tmDispatcherLogMapper", dispatcherLogMapper);
        setField(service, "tmMachineInfoMapper", serviceMachineInfoMapper);
        setField(service, "tmScheduleResultExplainMapper", explainMapper);
        setField(service, "tmScheduleUnplannedMapper", unplannedMapper);
        setField(service, "tmScheduleTemplate", template);
        setField(service, "tmScheduleOperationFacade", operationFacade);
        mockContext.setService(service);
        return mockContext;
    }

    private TmScheduleTemplateImpl buildTemplate(TmAutoPlanScenario scenario,
                                                 TmAutoScheduleDataLoadService dataLoadService,
                                                 TmScheduleResultMapper scheduleResultMapper,
                                                 TmScheduleResultExplainMapper explainMapper,
                                                 MockContext mockContext) {
        TmEngineStockMapper stockMapper = mock(TmEngineStockMapper.class);
        TmEngineInventoryPredictMapper inventoryPredictMapper = mock(TmEngineInventoryPredictMapper.class);
        when(stockMapper.selectList(any())).thenReturn(nullToEmpty(scenario.getStocks()));
        when(inventoryPredictMapper.selectFirstShiftDemandRows(any(), any(), any()))
                .thenReturn(buildFirstShiftDemandRows(scenario));
        when(inventoryPredictMapper.selectFirstShiftPlanRows(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        ITmPlanBootstrapService bootstrapService = context -> {
            new TmPlanBootstrapService().bootstrap(context);
            dataLoadService.loadAllData(context);
            applyTaskOverrides(scenario, context);
            mockContext.setLastContext(context);
        };
        TmStrategyRegistry registry = buildStrategyRegistry();
        TmTaskChainScheduleService taskChainScheduleService = new TmTaskChainScheduleService();
        return new TmScheduleTemplateImpl(bootstrapService,
                new TmInventoryPredictService(stockMapper, inventoryPredictMapper),
                new TmPlanCalcService(registry),
                new TmTaskSortService(registry),
                new TmMachineAssignService(taskChainScheduleService, registry),
                new TmBizSnapshotAndPersistService(new TmSnapshotBuildService(), new TmPersistService(),
                        scheduleResultMapper, explainMapper),
                new TmScheduleProcessLogger());
    }

    private TmAutoScheduleDataLoadService buildDataLoadService(TmAutoPlanScenario scenario) {
        TmAutoScheduleDataLoadService service = new TmAutoScheduleDataLoadService();
        TmParamsMapper paramsMapper = mock(TmParamsMapper.class);
        TmMachineInfoMapper machineInfoMapper = mock(TmMachineInfoMapper.class);
        TmAutoScheduleDataLoadMapper dataLoadMapper = mock(TmAutoScheduleDataLoadMapper.class);
        TmStockMapper stockMapper = mock(TmStockMapper.class);
        TmScheduleResultMapper scheduleResultMapper = mock(TmScheduleResultMapper.class);
        TmMouthPlateMapper mouthPlateMapper = mock(TmMouthPlateMapper.class);
        TmGlueMachineRealMapper glueMachineRealMapper = mock(TmGlueMachineRealMapper.class);
        TmSpecifyMachineMapper specifyMachineMapper = mock(TmSpecifyMachineMapper.class);
        TmMachineSpeedMapper machineSpeedMapper = mock(TmMachineSpeedMapper.class);
        TmMachineMaintenanceMapper maintenanceMapper = mock(TmMachineMaintenanceMapper.class);
        TmCurlRollMapper curlRollMapper = mock(TmCurlRollMapper.class);
        TmLossSettingMapper lossSettingMapper = mock(TmLossSettingMapper.class);

        when(paramsMapper.selectList(any())).thenReturn(nullToEmpty(scenario.getParams()));
        when(machineInfoMapper.selectList(any())).thenReturn(nullToEmpty(scenario.getMachineInfos()));
        when(dataLoadMapper.selectFormingDemandRows(any(), any())).thenReturn(nullToEmpty(scenario.getCxScheduleResults()));
        when(stockMapper.selectList(any())).thenReturn(nullToEmpty(scenario.getStocks()));
        when(scheduleResultMapper.selectList(any())).thenReturn(resolveHistoryScheduleResults(scenario));
        when(dataLoadMapper.selectWorkCalendarRows(any(), any(), any())).thenAnswer(invocation ->
                workCalendarRows(scenario, invocation.getArgument(1)));
        when(mouthPlateMapper.selectList(any())).thenReturn(nullToEmpty(scenario.getMouthPlates()));
        when(glueMachineRealMapper.selectList(any())).thenReturn(nullToEmpty(scenario.getGlueMachineReals()));
        when(specifyMachineMapper.selectList(any())).thenReturn(nullToEmpty(scenario.getSpecifyMachines()));
        when(machineSpeedMapper.selectList(any())).thenReturn(nullToEmpty(scenario.getMachineSpeeds()));
        when(maintenanceMapper.selectList(any())).thenReturn(nullToEmpty(scenario.getMachineMaintenances()));
        when(curlRollMapper.selectList(any())).thenReturn(nullToEmpty(scenario.getCurlRolls()));
        when(lossSettingMapper.selectList(any())).thenReturn(nullToEmpty(scenario.getLossSettings()));

        setField(service, "tmParamsMapper", paramsMapper);
        setField(service, "tmMachineInfoMapper", machineInfoMapper);
        setField(service, "tmAutoScheduleDataLoadMapper", dataLoadMapper);
        setField(service, "tmStockMapper", stockMapper);
        setField(service, "tmScheduleResultMapper", scheduleResultMapper);
        setField(service, "tmMouthPlateMapper", mouthPlateMapper);
        setField(service, "tmGlueMachineRealMapper", glueMachineRealMapper);
        setField(service, "tmSpecifyMachineMapper", specifyMachineMapper);
        setField(service, "tmMachineSpeedMapper", machineSpeedMapper);
        setField(service, "tmMachineMaintenanceMapper", maintenanceMapper);
        setField(service, "tmCurlRollMapper", curlRollMapper);
        setField(service, "tmLossSettingMapper", lossSettingMapper);
        return service;
    }

    /**
     * 仅执行业务初始化和数据加载，用于步骤级测试观察中间任务。
     *
     * @param mockContext Mock 上下文
     * @return 已加载任务的排程上下文
     */
    public TmScheduleContext loadContextOnly(MockContext mockContext) {
        TmScheduleContext context = new TmScheduleContext();
        context.setFactoryCode(mockContext.getScenario().getAutoPlanRequest().getFactoryCode());
        context.setScheduleDate(mockContext.getScenario().getAutoPlanRequest().getScheduleDate());
        context.setOperator(mockContext.getScenario().getAutoPlanRequest().getOperator());
        context.setTraceId(mockContext.getScenario().getAutoPlanRequest().getTraceId());
        new TmPlanBootstrapService().bootstrap(context);
        mockContext.getDataLoadService().loadAllData(context);
        applyTaskOverrides(mockContext.getScenario(), context);
        mockContext.setLastContext(context);
        return context;
    }

    private TmStrategyRegistry buildStrategyRegistry() {
        return new TmStrategyRegistry(Arrays.asList(new TmGuardDemandQtyStrategy(), new TmNextShiftDemandQtyStrategy()),
                Collections.singletonList(new TmDefaultPlanQtyStrategy()),
                Collections.singletonList(new TmDefaultMachineFilterRule()),
                Collections.singletonList(new TmDefaultMachineScoreStrategy()),
                Collections.singletonList(new TmDefaultTaskSortStrategy()));
    }

    private List<TmInventoryPredictQtyVo> buildFirstShiftDemandRows(TmAutoPlanScenario scenario) {
        Map<String, BigDecimal> qtyMap = new LinkedHashMap<>();
        nullToEmpty(scenario.getCxScheduleResults()).forEach(row -> {
            if (StrUtil.isBlank(row.getTreadCode())) {
                return;
            }
            BigDecimal classOneQty = nvl(row.getClass1PlanQty());
            BigDecimal treadLength = nvl(row.getTreadShoulderLength());
            qtyMap.merge(row.getTreadCode(), classOneQty.multiply(treadLength), BigDecimal::add);
        });
        return qtyMap.entrySet().stream().map(entry -> {
            TmInventoryPredictQtyVo row = new TmInventoryPredictQtyVo();
            row.setTreadCode(entry.getKey());
            row.setQty(entry.getValue());
            return row;
        }).collect(Collectors.toList());
    }

    private List<TmScheduleResult> resolveHistoryScheduleResults(TmAutoPlanScenario scenario) {
        if (Boolean.TRUE.equals(scenario.getForceEmptyHistoryScheduleResults()) || hasNewSpecParam(scenario)
                || !nullToEmpty(scenario.getHistoryScheduleResults()).isEmpty()) {
            return nullToEmpty(scenario.getHistoryScheduleResults());
        }
        Map<String, TmScheduleResult> historyMap = new LinkedHashMap<>();
        for (com.zlt.aps.tm.domain.vo.TmFormingDemandRowVo demandRow : nullToEmpty(scenario.getCxScheduleResults())) {
            if (StrUtil.isBlank(demandRow.getTreadCode())) {
                continue;
            }
            TmScheduleResult result = new TmScheduleResult();
            result.setFactoryCode(scenario.getAutoPlanRequest().getFactoryCode());
            result.setScheduleDate(DateUtil.offsetDay(scenario.getAutoPlanRequest().getScheduleDate(), -1));
            result.setTreadCode(demandRow.getTreadCode());
            result.setClass1PlanQty(BigDecimal.ONE);
            historyMap.put(demandRow.getTreadCode(), result);
        }
        return new ArrayList<>(historyMap.values());
    }

    private boolean hasNewSpecParam(TmAutoPlanScenario scenario) {
        return nullToEmpty(scenario.getParams()).stream()
                .anyMatch(param -> "TM_NEW_SPEC_LOOKBACK_DAYS".equals(param.getParamCode())
                        || "TM_NEW_SPEC_ADVANCE_SHIFT_COUNT".equals(param.getParamCode()));
    }
    private List<TmWorkCalendarRowVo> workCalendarRows(TmAutoPlanScenario scenario, String procCode) {
        return nullToEmpty(scenario.getWorkCalendars()).stream()
                .filter(item -> procCode == null || procCode.equals(item.getProcCode()))
                .map(this::toWorkCalendarRow)
                .collect(Collectors.toList());
    }

    private TmWorkCalendarRowVo toWorkCalendarRow(TmAutoPlanScenario.WorkCalendarData data) {
        TmWorkCalendarRowVo row = new TmWorkCalendarRowVo();
        row.setDayFlag(data.getDayFlag());
        row.setOneShiftFlag(data.getOneShiftFlag());
        row.setTwoShiftFlag(data.getTwoShiftFlag());
        row.setThreeShiftFlag(data.getThreeShiftFlag());
        return row;
    }

    private void applyTaskOverrides(TmAutoPlanScenario scenario, TmScheduleContext context) {
        if (scenario.getTaskOverrides() == null || scenario.getTaskOverrides().isEmpty()) {
            return;
        }
        for (TmTaskDraft task : context.getTaskDraftList()) {
            for (TmAutoPlanScenario.TaskOverride override : scenario.getTaskOverrides()) {
                if (!matches(task, override)) {
                    continue;
                }
                applyTaskOverride(task, override);
            }
        }
    }

    private boolean matches(TmTaskDraft task, TmAutoPlanScenario.TaskOverride override) {
        if (StrUtil.isNotBlank(override.getOrderNo()) && !override.getOrderNo().equals(task.getOrderNo())) {
            return false;
        }
        if (StrUtil.isNotBlank(override.getTreadCode()) && !override.getTreadCode().equals(task.getTreadCode())) {
            return false;
        }
        if (override.getShiftOrder() != null && !override.getShiftOrder().equals(task.getShiftOrder())) {
            return false;
        }
        return true;
    }

    private void applyTaskOverride(TmTaskDraft task, TmAutoPlanScenario.TaskOverride override) {
        if (override.getBaseGlueCode() != null) {
            task.setBaseGlueCode(override.getBaseGlueCode());
        }
        if (override.getPlanQty() != null) {
            task.setPlanQty(override.getPlanQty());
        }
        if (override.getCurrentShiftDemandQty() != null) {
            task.setCurrentShiftDemandQty(override.getCurrentShiftDemandQty());
        }
        if (override.getGuardDemandQty() != null) {
            task.setGuardDemandQty(override.getGuardDemandQty());
        }
        if (override.getRollingStockQty() != null) {
            task.setRollingStockQty(override.getRollingStockQty());
        }
        if (override.getTailFlag() != null) {
            task.setTailFlag(override.getTailFlag());
        }
        if (override.getTailBalanceQty() != null) {
            task.setTailBalanceQty(override.getTailBalanceQty());
        }
        if (override.getTreadShoulderLength() != null) {
            task.setTreadShoulderLength(override.getTreadShoulderLength());
        }
        if (override.getLossRate() != null) {
            task.setLossRate(override.getLossRate());
        }
        if (override.getSupplyHours() != null) {
            task.setSupplyHours(override.getSupplyHours());
        }
        if (override.getTotalToolQty() != null) {
            task.setTotalToolQty(override.getTotalToolQty());
        }
        if (override.getMachineRemainCapacity() != null) {
            task.setMachineRemainCapacity(override.getMachineRemainCapacity());
        }
        if (override.getMachineSpeed() != null) {
            task.setMachineSpeed(override.getMachineSpeed());
        }
        if (override.getMaintenanceHours() != null) {
            task.setMaintenanceHours(override.getMaintenanceHours());
        }
        if (override.getPreviousSpecSwitchHours() != null) {
            task.setPreviousSpecSwitchHours(override.getPreviousSpecSwitchHours());
        }
        if (override.getPreviousGlueSwitchHours() != null) {
            task.setPreviousGlueSwitchHours(override.getPreviousGlueSwitchHours());
        }
        if (override.getUnplannedReasonCode() != null) {
            task.setUnplannedReasonCode(override.getUnplannedReasonCode());
        }
        if (override.getUnplannedReasonDesc() != null) {
            task.setUnplannedReasonDesc(override.getUnplannedReasonDesc());
        }
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("注入测试依赖失败：" + fieldName, ex);
        }
    }

    private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ex) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static <T> List<T> nullToEmpty(List<T> value) {
        return value == null ? Collections.emptyList() : value;
    }

    /**
     * 单个 JSON 场景的 Mock 运行上下文。
     */
    public static class MockContext {

        private final TmAutoPlanScenario scenario;

        private final List<TmScheduleResult> insertedResults = new ArrayList<>();

        private final List<TmScheduleResultExplain> insertedExplains = new ArrayList<>();

        private TmScheduleResultServiceImpl service;

        private TmAutoScheduleDataLoadService dataLoadService;

        private TmScheduleContext lastContext;

        MockContext(TmAutoPlanScenario scenario) {
            this.scenario = scenario;
        }

        public TmAutoPlanScenario getScenario() {
            return scenario;
        }

        public List<TmScheduleResult> getInsertedResults() {
            return insertedResults;
        }

        public List<TmScheduleResultExplain> getInsertedExplains() {
            return insertedExplains;
        }

        public TmScheduleResultServiceImpl getService() {
            return service;
        }

        void setService(TmScheduleResultServiceImpl service) {
            this.service = service;
        }

        public TmAutoScheduleDataLoadService getDataLoadService() {
            return dataLoadService;
        }

        void setDataLoadService(TmAutoScheduleDataLoadService dataLoadService) {
            this.dataLoadService = dataLoadService;
        }

        public TmScheduleContext getLastContext() {
            return lastContext;
        }

        void setLastContext(TmScheduleContext lastContext) {
            this.lastContext = lastContext;
        }

        public String scenarioDateText() {
            if (scenario.getAutoPlanRequest() == null || scenario.getAutoPlanRequest().getScheduleDate() == null) {
                return null;
            }
            return DateUtil.formatDate(scenario.getAutoPlanRequest().getScheduleDate());
        }
    }
}
