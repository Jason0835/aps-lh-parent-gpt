package com.zlt.aps.tm.service.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.engine.schedule.ScheduleOperationContext;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResultExplain;
import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleRequestVo;
import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleResponseVo;
import com.zlt.aps.tm.engine.domain.TmPersistResult;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.engine.service.TmScheduleOperationFacade;
import com.zlt.aps.tm.engine.template.TmScheduleTemplateImpl;
import com.zlt.aps.tm.mapper.*;
import com.zlt.aps.tm.service.TmAutoScheduleDataLoadService;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TmScheduleResultServiceImplTest {

    @BeforeClass
    public static void initMybatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TmScheduleResult.class);
    }

    @Mock
    private TmScheduleResultMapper tmScheduleResultMapper;

    @Mock
    private TmDispatcherLogMapper tmDispatcherLogMapper;

    @Mock
    private TmMachineInfoMapper tmMachineInfoMapper;

    @Mock
    private TmScheduleResultExplainMapper tmScheduleResultExplainMapper;

    @Mock
    private TmScheduleUnplannedMapper tmScheduleUnplannedMapper;

    @Mock
    private TmAutoScheduleDataLoadService tmAutoScheduleDataLoadService;

    @Mock
    private TmScheduleTemplateImpl tmScheduleTemplate;

    @Mock
    private TmScheduleOperationFacade tmScheduleOperationFacade;

    @InjectMocks
    private TmScheduleResultServiceImpl service;

    /**
     * 测试内容：验证排程结果实体 orderNo 字段映射到 ORDER_NO 数据库列。
     * 测试场景：通过反射读取 TmScheduleResult.orderNo 的 TableField 注解。
     * 预期结果：注解存在且列名为 ORDER_NO。
     *
     * @throws NoSuchFieldException 字段不存在时由测试框架抛出
     */
    @Test
    public void tmScheduleResultShouldMapOrderNoFieldToOrderNoColumn() throws NoSuchFieldException {
        // 读取实体字段上的 MyBatis-Plus 列映射注解。
        TableField tableField = TmScheduleResult.class.getDeclaredField("orderNo").getAnnotation(TableField.class);

        // 断言字段映射没有退化为默认驼峰转换。
        assertNotNull(tableField);
        assertEquals("ORDER_NO", tableField.value());
    }

    /**
     * 测试内容：验证班次开始时间字段映射到对应数据库列。
     * 测试场景：通过反射读取 class1StartTime 字段注解。
     * 预期结果：注解列名为 CLASS1_START_TIME。
     *
     * @throws NoSuchFieldException 字段不存在时由测试框架抛出
     */
    @Test
    public void tmScheduleResultShouldMapClassStartTimeFieldToClassStartTimeColumn() throws NoSuchFieldException {
        // 读取 1 班开始时间字段的列映射注解。
        TableField tableField = TmScheduleResult.class.getDeclaredField("class1StartTime").getAnnotation(TableField.class);

        // 断言列名与数据库字段一致。
        assertNotNull(tableField);
        assertEquals("CLASS1_START_TIME", tableField.value());
    }

    /**
     * 测试内容：验证解释表 traceId 字段映射到 TRACE_ID 数据库列。
     * 测试场景：通过反射读取 TmScheduleResultExplain.traceId 注解。
     * 预期结果：注解列名为 TRACE_ID。
     *
     * @throws NoSuchFieldException 字段不存在时由测试框架抛出
     */
    @Test
    public void tmScheduleResultExplainShouldMapTraceIdFieldToTraceIdColumn() throws NoSuchFieldException {
        // 读取解释表追踪号字段的列映射注解。
        TableField tableField = TmScheduleResultExplain.class.getDeclaredField("traceId").getAnnotation(TableField.class);

        // 断言 traceId 使用明确列名。
        assertNotNull(tableField);
        assertEquals("TRACE_ID", tableField.value());
    }

    /**
     * 测试内容：验证详设新增解释字段都具备明确数据库列映射。
     * 测试场景：逐项检查解释表上的需求、调整量、状态、锁定标记等字段。
     * 预期结果：所有字段的 TableField 注解列名都与设计列名一致。
     *
     * @throws NoSuchFieldException 字段不存在时由测试框架抛出
     */
    @Test
    public void tmScheduleResultExplainShouldMapDetailedDesignFieldsToColumns() throws NoSuchFieldException {
        // 逐个断言解释表详设字段，防止新增字段漏写列映射导致落库异常。
        assertTableField("factoryCode", "FACTORY_CODE");
        assertTableField("lastShiftSupplyQty", "LAST_SHIFT_SUPPLY_QTY");
        assertTableField("monthSurplusDeductQty", "MONTH_SURPLUS_DEDUCT_QTY");
        assertTableField("toolLimitAdjustQty", "TOOL_LIMIT_ADJUST_QTY");
        assertTableField("minStartAdjustQty", "MIN_START_ADJUST_QTY");
        assertTableField("tailRoundAdjustQty", "TAIL_ROUND_ADJUST_QTY");
        assertTableField("capacityAdjustQty", "CAPACITY_ADJUST_QTY");
        assertTableField("stockQty", "STOCK_QTY");
        assertTableField("planStockQty", "PLAN_STOCK_QTY");
        assertTableField("supplyHours", "SUPPLY_HOURS");
        assertTableField("coverageShiftCount", "COVERAGE_SHIFT_COUNT");
        assertTableField("lastShiftPlanQty", "LAST_SHIFT_PLAN_QTY");
        assertTableField("monthSurplusQty", "MONTH_SURPLUS_QTY");
        assertTableField("requiredQty", "REQUIRED_QTY");
        assertTableField("ruleSummaryDesc", "RULE_SUMMARY_DESC");
        assertTableField("selectedMachineScore", "SELECTED_MACHINE_SCORE");
        assertTableField("manualLockedFlag", "MANUAL_LOCKED_FLAG");
        assertTableField("sequenceLockFlag", "SEQUENCE_LOCK_FLAG");
        assertTableField("forceChangeFlag", "FORCE_CHANGE_FLAG");
        assertTableField("generateMode", "GENERATE_MODE");
        assertTableField("resultStatus", "RESULT_STATUS");
        assertTableField("currentStepCode", "CURRENT_STEP_CODE");
    }

    /**
     * 测试内容：验证解释表 mapper 具备基础写入能力。
     * 测试场景：检查 TmScheduleResultExplainMapper 是否继承 CommBaseMapper。
     * 预期结果：mapper 可作为通用写入 mapper 使用。
     */
    @Test
    public void tmScheduleResultExplainMapperShouldProvideBaseWriteCapability() {
        // 断言 mapper 继承结构，避免解释表无法使用通用插入能力。
        assertEquals(true, CommBaseMapper.class.isAssignableFrom(TmScheduleResultExplainMapper.class));
    }

    /**
     * 测试内容：验证已下发排程记录在计划量变化后变为待释放。
     * 测试场景：旧记录已下发，新记录同机台但 1 班计划量变化。
     * 预期结果：更新成功且 releaseStatus 被置为 WAIT_RELEASING。
     */
    @Test
    public void updateTmScheduleResultSetsWaitReleasingWhenReleasedPlanQtyChanges() {
        // 准备旧排程记录，表示该记录已存在且原计划量为 10。
        TmScheduleResult oldSchedule = new TmScheduleResult();
        oldSchedule.setId(1L);
        oldSchedule.setMachineCode("TM-01");
        oldSchedule.setClass1PlanQty(BigDecimal.TEN);

        // 准备新排程记录，模拟已下发记录计划量变更。
        TmScheduleResult newSchedule = new TmScheduleResult();
        newSchedule.setId(1L);
        newSchedule.setReleaseStatus(ApsConstant.IS_RELEASE);
        newSchedule.setMachineCode("TM-01");
        newSchedule.setClass1PlanQty(BigDecimal.valueOf(20));

        when(tmScheduleResultMapper.selectById(1L)).thenReturn(oldSchedule);
        when(tmScheduleResultMapper.updateById(newSchedule)).thenReturn(1);

        // 执行更新并断言状态变为待释放。
        assertEquals(1, service.updateTmScheduleResult(newSchedule));
        assertEquals(ApsConstant.WAIT_RELEASING, newSchedule.getReleaseStatus());
        // 验证最终仍调用 mapper 更新当前记录。
        verify(tmScheduleResultMapper).updateById(newSchedule);
    }

    /**
     * 测试内容：验证未携带下发状态的变更记录默认置为未下发。
     * 测试场景：旧记录存在，新记录未设置 releaseStatus 且机台变化。
     * 预期结果：更新成功后 releaseStatus 为 NO_RELEASE。
     */
    @Test
    public void updateTmScheduleResultSetsNoReleaseWhenChangedRecordHasBlankReleaseStatus() {
        // 准备旧排程记录。
        TmScheduleResult oldSchedule = new TmScheduleResult();
        oldSchedule.setId(2L);
        oldSchedule.setMachineCode("TM-02");
        oldSchedule.setClass2PlanQty(BigDecimal.ONE);

        // 准备未设置下发状态的新记录，模拟普通编辑保存。
        TmScheduleResult newSchedule = new TmScheduleResult();
        newSchedule.setId(2L);
        newSchedule.setMachineCode("TM-03");
        newSchedule.setClass2PlanQty(BigDecimal.ONE);

        when(tmScheduleResultMapper.selectById(2L)).thenReturn(oldSchedule);
        when(tmScheduleResultMapper.updateById(newSchedule)).thenReturn(1);

        // 执行更新并断言空状态被规范为未下发。
        assertEquals(1, service.updateTmScheduleResult(newSchedule));
        assertEquals(ApsConstant.NO_RELEASE, newSchedule.getReleaseStatus());
        // 验证最终执行数据库更新。
        verify(tmScheduleResultMapper).updateById(newSchedule);
    }

    /**
     * 测试内容：验证按工厂和日期覆盖旧批次时的逻辑删除顺序。
     * 测试场景：调用服务的批量逻辑删除入口。
     * 预期结果：先删未排表，再删解释表，最后删结果表。
     */
    @Test
    public void logicDeleteByFactoryCodeAndScheduleDateDeletesUnplannedAndExplainBeforeResult() {
        // 准备待覆盖的排程日期。
        Date scheduleDate = new Date();

        // 执行旧批次逻辑删除入口。
        service.logicDeleteByFactoryCodeAndScheduleDate("116", scheduleDate);

        // 断言删除顺序，避免先删结果导致解释或未排记录残留。
        org.mockito.InOrder inOrder = inOrder(tmScheduleUnplannedMapper, tmScheduleResultExplainMapper, tmScheduleResultMapper);
        inOrder.verify(tmScheduleUnplannedMapper).logicDeleteByFactoryCodeAndScheduleDate("116", scheduleDate);
        inOrder.verify(tmScheduleResultExplainMapper).logicDeleteByFactoryCodeAndScheduleDate("116", scheduleDate);
        inOrder.verify(tmScheduleResultMapper).logicDeleteByFactoryCodeAndScheduleDate("116", scheduleDate);
    }

    /**
     * 测试内容：验证存在旧批次且全部未发布时，校验接口要求用户确认覆盖。
     * 测试场景：旧结果列表只有未下发记录。
     * 预期结果：校验成功但 confirmRequired=true。
     */
    @Test
    public void validateAutoPlanShouldRequireConfirmWhenOldBatchAllNoRelease() {
        // 准备自动排程请求和一条未下发旧结果。
        TmAutoScheduleRequestVo request = buildAutoRequest();
        TmScheduleResult oldResult = new TmScheduleResult();
        oldResult.setReleaseStatus(ApsConstant.NO_RELEASE);
        when(tmScheduleResultMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(Collections.singletonList(oldResult));

        // 执行自动排程前置校验。
        TmAutoScheduleResponseVo response = service.validateTmAutoPlan(request);

        // 断言需要用户确认覆盖旧未下发结果。
        assertEquals(Boolean.TRUE, response.getSuccess());
        assertEquals(Boolean.TRUE, response.getConfirmRequired());
    }

    /**
     * 测试内容：验证无旧批次时自动排程校验直接通过。
     * 测试场景：旧结果查询返回空列表。
     * 预期结果：success=true，confirmRequired=false，并生成批次号和追踪号。
     */
    @Test
    public void validateAutoPlanShouldPassWhenNoOldBatchExists() {
        // 准备请求并 mock 无旧排程结果。
        TmAutoScheduleRequestVo request = buildAutoRequest();
        when(tmScheduleResultMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(Collections.emptyList());

        // 执行自动排程前置校验。
        TmAutoScheduleResponseVo response = service.validateTmAutoPlan(request);

        // 断言无需确认覆盖，且响应已补充批次和追踪信息。
        assertEquals(Boolean.TRUE, response.getSuccess());
        assertEquals(Boolean.FALSE, response.getConfirmRequired());
        assertNotNull(response.getBatchNo());
        assertNotNull(response.getTraceId());
    }

    /**
     * 测试内容：验证自动排程批次号按执行时刻生成且连续调用不重复。
     * 测试场景：同一天同一排程日期连续两次执行自动排程校验。
     * 预期结果：batchNo 符合 TMyyyyMMddHHmmssSSS 格式，且两次结果不同。
     */
    @Test
    public void validateAutoPlanShouldGenerateUniqueExecutionTimeBatchNo() {
        // 准备请求并 mock 无旧排程结果，连续两次调用校验入口。
        TmAutoScheduleRequestVo request = buildAutoRequest();
        when(tmScheduleResultMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(Collections.emptyList());

        TmAutoScheduleResponseVo firstResponse = service.validateTmAutoPlan(request);
        TmAutoScheduleResponseVo secondResponse = service.validateTmAutoPlan(request);

        // 断言批次号不再按排程日期固定生成，而是按执行时刻生成同日不重复值。
        assertEquals(true, firstResponse.getBatchNo().matches("TM\\d{17}"));
        assertEquals(true, secondResponse.getBatchNo().matches("TM\\d{17}"));
        assertEquals(false, firstResponse.getBatchNo().equals(secondResponse.getBatchNo()));
    }

    /**
     * 测试内容：验证自动排程请求不能为空。
     * 测试场景：自动排程校验入口传入 null。
     * 预期结果：抛出 ServiceException，不继续查询旧批次。
     */
    @Test(expected = ServiceException.class)
    public void validateAutoPlanShouldRejectNullRequest() {
        // 执行校验时应在参数层直接拒绝空请求。
        service.validateTmAutoPlan(null);
    }

    /**
     * 测试内容：验证自动排程工厂不能为空。
     * 测试场景：请求缺少 factoryCode。
     * 预期结果：抛出 ServiceException，不继续查询旧批次。
     */
    @Test(expected = ServiceException.class)
    public void validateAutoPlanShouldRejectBlankFactoryCode() {
        // 准备缺少工厂的请求。
        TmAutoScheduleRequestVo request = buildAutoRequest();
        request.setFactoryCode(" ");

        // 执行校验时应拒绝工厂为空的请求。
        service.validateTmAutoPlan(request);
    }

    /**
     * 测试内容：验证自动排程日期不能为空。
     * 测试场景：请求缺少 scheduleDate。
     * 预期结果：抛出 ServiceException，不继续查询旧批次。
     */
    @Test(expected = ServiceException.class)
    public void validateAutoPlanShouldRejectNullScheduleDate() {
        // 准备缺少排程日期的请求。
        TmAutoScheduleRequestVo request = buildAutoRequest();
        request.setScheduleDate(null);

        // 执行校验时应拒绝日期为空的请求。
        service.validateTmAutoPlan(request);
    }

    /**
     * 测试内容：验证存在已发布旧结果时拒绝重复生成。
     * 测试场景：旧结果列表包含已下发记录。
     * 预期结果：校验抛出 ServiceException。
     */
    @Test(expected = ServiceException.class)
    public void validateAutoPlanShouldRejectWhenOldBatchHasReleasedResult() {
        // 准备请求和一条已下发旧结果。
        TmAutoScheduleRequestVo request = buildAutoRequest();
        TmScheduleResult oldResult = new TmScheduleResult();
        oldResult.setReleaseStatus(ApsConstant.IS_RELEASE);
        when(tmScheduleResultMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(Collections.singletonList(oldResult));

        // 执行校验时应拒绝覆盖已发布排程。
        service.validateTmAutoPlan(request);
    }

    /**
     * 测试内容：验证执行自动排程时未确认覆盖旧未发布批次会被拒绝。
     * 测试场景：旧结果全部未下发，但请求没有 confirmOverwrite=true。
     * 预期结果：执行入口抛出 ServiceException，不删除旧数据、不调用模板。
     */
    @Test(expected = ServiceException.class)
    public void autoPlanShouldRejectOldNoReleaseWhenConfirmMissing() {
        // 准备一条未下发旧结果，但请求没有确认覆盖。
        TmAutoScheduleRequestVo request = buildAutoRequest();
        TmScheduleResult oldResult = new TmScheduleResult();
        oldResult.setReleaseStatus(ApsConstant.NO_RELEASE);
        when(tmScheduleResultMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(Collections.singletonList(oldResult));

        // 执行自动排程时应要求用户先确认覆盖。
        service.tmAutoPlan(request);
    }

    /**
     * 测试内容：验证用户确认覆盖后会先删除旧批次再生成新排程。
     * 测试场景：请求 confirmOverwrite=true，旧结果全部未下发，引擎返回正常结果。
     * 预期结果：响应成功且按未排表、解释表、结果表顺序逻辑删除旧数据。
     */
    @Test
    public void autoPlanShouldDeleteOldBatchAfterConfirmWhenAllNoRelease() {
        // 准备确认覆盖请求和一条未下发旧结果。
        TmAutoScheduleRequestVo request = buildAutoRequest();
        request.setConfirmOverwrite(Boolean.TRUE);
        TmScheduleResult oldResult = new TmScheduleResult();
        oldResult.setReleaseStatus(ApsConstant.NO_RELEASE);
        when(tmScheduleResultMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(Collections.singletonList(oldResult));
        // mock 引擎模板输出，避免依赖真实排程引擎和数据库。
        mockEngineOutput();

        // 执行自动排程。
        TmAutoScheduleResponseVo response = service.tmAutoPlan(request);

        // 断言响应成功且不再要求确认。
        assertEquals(Boolean.TRUE, response.getSuccess());
        assertEquals(Boolean.FALSE, response.getConfirmRequired());
        // 断言旧批次删除顺序满足覆盖要求。
        org.mockito.InOrder inOrder = inOrder(tmScheduleUnplannedMapper, tmScheduleResultExplainMapper, tmScheduleResultMapper);
        inOrder.verify(tmScheduleUnplannedMapper).logicDeleteByFactoryCodeAndScheduleDate("116", request.getScheduleDate());
        inOrder.verify(tmScheduleResultExplainMapper).logicDeleteByFactoryCodeAndScheduleDate("116", request.getScheduleDate());
        inOrder.verify(tmScheduleResultMapper).logicDeleteByFactoryCodeAndScheduleDate("116", request.getScheduleDate());
    }

    /**
     * 测试内容：验证服务层使用排程模板的持久化结果，不再直接加载和插入数据。
     * 测试场景：无旧结果，排程模板返回 1 条结果、0 条未排。
     * 预期结果：响应数量来自模板上下文，且不会直接调用数据加载或 mapper insert。
     */
    @Test
    public void autoPlanShouldUseTemplatePersistResultWithoutDirectLoadOrInsert() {
        // 准备无旧结果场景和模板输出。
        TmAutoScheduleRequestVo request = buildAutoRequest();
        when(tmScheduleResultMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(Collections.emptyList());
        mockEngineOutput(1, 0);

        // 执行自动排程入口。
        TmAutoScheduleResponseVo response = service.tmAutoPlan(request);

        // 断言响应统计来自模板执行后的 PersistResult。
        assertEquals(Boolean.TRUE, response.getSuccess());
        assertEquals(Integer.valueOf(1), response.getResultCount());
        assertEquals(Integer.valueOf(0), response.getUnplannedCount());
        // 断言服务层只调用模板，不绕过模板直接加载或插表。
        verify(tmScheduleTemplate).execute(any(TmScheduleContext.class));
        verify(tmAutoScheduleDataLoadService, never()).loadAllData(any(TmScheduleContext.class));
        verify(tmScheduleResultMapper, never()).insert(any(TmScheduleResult.class));
        verify(tmScheduleResultExplainMapper, never()).insert(any(TmScheduleResultExplain.class));
    }

    /**
     * 测试内容：验证自动排程入口输出可按追踪号串联的步骤日志。
     * 测试场景：无旧结果，模板执行后返回 2 条结果和 1 条未排任务。
     * 预期结果：入口日志包含固定前缀、追踪号、模板完成汇总和最终响应汇总。
     */
    @Test
    public void autoPlanShouldWriteTraceableStepLogs() {
        // 准备日志捕获器，避免依赖控制台输出文本。
        Logger logger = (Logger) LoggerFactory.getLogger(TmScheduleResultServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            // 准备无旧结果场景和模板输出。
            TmAutoScheduleRequestVo request = buildAutoRequest();
            when(tmScheduleResultMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(Collections.emptyList());
            mockEngineOutput(2, 1);

            // 执行自动排程入口。
            TmAutoScheduleResponseVo response = service.tmAutoPlan(request);

            // 断言响应统计与日志中的关键上下文字段一致，便于线上按 traceId 排查。
            assertEquals(Boolean.TRUE, response.getSuccess());
            assertEquals(true, appender.list.stream().map(ILoggingEvent::getFormattedMessage)
                    .anyMatch(message -> message.contains("[TM_AUTO_PLAN] step=REQUEST_VALIDATED")
                            && message.contains("traceId=TRACE-TEST")));
            assertEquals(true, appender.list.stream().map(ILoggingEvent::getFormattedMessage)
                    .anyMatch(message -> message.contains("[TM_AUTO_PLAN] step=TEMPLATE_FINISHED")
                            && message.contains("traceId=TRACE-TEST")
                            && message.contains("resultCount=2")
                            && message.contains("unplannedCount=1")));
            assertEquals(true, appender.list.stream().map(ILoggingEvent::getFormattedMessage)
                    .anyMatch(message -> message.contains("[TM_AUTO_PLAN] step=FINISHED")
                            && message.contains("traceId=TRACE-TEST")
                            && message.contains("success=true")));
        } finally {
            // 移除测试日志捕获器，避免影响同类后续用例。
            logger.detachAppender(appender);
        }
    }

    /**
     * 测试内容：验证引擎部分落库失败时，自动排程响应携带部分失败提示和最后错误。
     * 测试场景：模板执行后 PersistResult 中有正常结果和一条错误信息。
     * 预期结果：响应 success=true，同时 message 包含“部分记录落库失败”和具体订单错误。
     */
    @Test
    public void autoPlanShouldReturnLastPersistErrorWhenPartiallyFailed() {
        // 准备无旧结果场景。
        TmAutoScheduleRequestVo request = buildAutoRequest();
        when(tmScheduleResultMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(Collections.emptyList());
        // mock 模板执行，在上下文中写入部分成功、部分失败的持久化结果。
        doAnswer(invocation -> {
            TmScheduleContext context = invocation.getArgument(0);
            TmPersistResult persistResult = new TmPersistResult();
            persistResult.setResultCount(1);
            persistResult.setExplainCount(1);
            persistResult.addErrorMsg("结果表写入失败，orderNo=ORD-001");
            context.setPersistResult(persistResult);
            return new TmAutoScheduleResponseVo();
        }).when(tmScheduleTemplate).execute(any(TmScheduleContext.class));

        // 执行自动排程入口。
        TmAutoScheduleResponseVo response = service.tmAutoPlan(request);

        // 断言响应保留成功结果数量，并把最后错误信息带回前端提示。
        assertEquals(Boolean.TRUE, response.getSuccess());
        assertEquals(Integer.valueOf(1), response.getResultCount());
        assertEquals(true, response.getMessage().contains("部分记录落库失败"));
        assertEquals(true, response.getMessage().contains("ORD-001"));
    }

    /**
     * 测试内容：验证已发布记录未发生关键字段变化时不回退发布状态。
     * 测试场景：旧记录和新记录机台、六班计划量均一致。
     * 预期结果：releaseStatus 保持已发布。
     */
    @Test
    public void updateTmScheduleResultShouldKeepReleaseStatusWhenNoScheduleFieldChanged() {
        // 准备旧记录和新记录，关键排程字段完全一致。
        TmScheduleResult oldSchedule = new TmScheduleResult();
        oldSchedule.setId(10L);
        oldSchedule.setMachineCode("TM-01");
        oldSchedule.setClass1PlanQty(BigDecimal.TEN);
        TmScheduleResult newSchedule = new TmScheduleResult();
        newSchedule.setId(10L);
        newSchedule.setReleaseStatus(ApsConstant.IS_RELEASE);
        newSchedule.setMachineCode("TM-01");
        newSchedule.setClass1PlanQty(BigDecimal.TEN);
        when(tmScheduleResultMapper.selectById(10L)).thenReturn(oldSchedule);
        when(tmScheduleResultMapper.updateById(newSchedule)).thenReturn(1);

        // 执行更新。
        assertEquals(1, service.updateTmScheduleResult(newSchedule));

        // 断言状态没有被回退为待发布或未发布。
        assertEquals(ApsConstant.IS_RELEASE, newSchedule.getReleaseStatus());
    }

    /**
     * 测试内容：验证发布中记录更新时不重算发布状态。
     * 测试场景：新记录状态为发布中，关键字段变化。
     * 预期结果：跳过旧记录对比，releaseStatus 保持发布中。
     */
    @Test
    public void updateTmScheduleResultShouldSkipStatusRollbackWhenReleasing() {
        // 准备发布中记录，模拟界面保存时携带发布中状态。
        TmScheduleResult scheduleResult = new TmScheduleResult();
        scheduleResult.setId(11L);
        scheduleResult.setReleaseStatus(ApsConstant.RELEASING);
        scheduleResult.setMachineCode("TM-02");
        when(tmScheduleResultMapper.updateById(scheduleResult)).thenReturn(1);

        // 执行更新。
        assertEquals(1, service.updateTmScheduleResult(scheduleResult));

        // 断言发布中状态不被覆盖，且无需查询旧记录。
        assertEquals(ApsConstant.RELEASING, scheduleResult.getReleaseStatus());
        verify(tmScheduleResultMapper, never()).selectById(11L);
    }

    /**
     * 测试内容：验证调量缺少 ID 时直接拒绝。
     * 测试场景：调量参数为空。
     * 预期结果：抛出 ServiceException，不触发任务链。
     */
    @Test(expected = ServiceException.class)
    public void changeQtyShouldRejectNullRequest() {
        // 执行调量时应在参数层直接拒绝。
        service.changeQty(null);
    }

    /**
     * 测试内容：验证调量时旧记录不存在会被拒绝。
     * 测试场景：ID 合法且非发布保护状态，但数据库查不到旧记录。
     * 预期结果：抛出 ServiceException，不触发任务链调量。
     */
    @Test(expected = ServiceException.class)
    public void changeQtyShouldRejectWhenOldRecordMissing() {
        // 准备调量请求和发布状态校验通过场景。
        TmScheduleResult scheduleResult = new TmScheduleResult();
        scheduleResult.setId(12L);
        when(tmScheduleResultMapper.isReleasingOrTimeoutByIds(any())).thenReturn(0);
        when(tmScheduleResultMapper.selectById(12L)).thenReturn(null);

        // 执行调量时应因旧记录不存在被拒绝。
        service.changeQty(scheduleResult);
    }

    /**
     * 测试内容：验证发布中或超时失败记录不允许调量。
     * 测试场景：发布保护查询返回命中记录。
     * 预期结果：抛出 ServiceException，不查询旧记录、不触发任务链。
     */
    @Test(expected = ServiceException.class)
    public void changeQtyShouldRejectWhenRecordIsReleasingOrTimeout() {
        // 准备发布保护命中的调量请求。
        TmScheduleResult scheduleResult = new TmScheduleResult();
        scheduleResult.setId(13L);
        when(tmScheduleResultMapper.isReleasingOrTimeoutByIds(any())).thenReturn(1);

        // 执行调量时应被发布保护拒绝。
        service.changeQty(scheduleResult);
    }

    /**
     * 测试内容：验证转机台时旧记录不存在会被拒绝。
     * 测试场景：ID 合法且非发布保护状态，但数据库查不到旧记录。
     * 预期结果：抛出 ServiceException，不触发任务链转移。
     */
    @Test(expected = ServiceException.class)
    public void changeMachineShouldRejectWhenOldRecordMissing() {
        // 准备转机台请求和发布状态校验通过场景。
        TmScheduleResult scheduleResult = new TmScheduleResult();
        scheduleResult.setId(14L);
        scheduleResult.setMachineCode("TM-02");
        when(tmScheduleResultMapper.isReleasingOrTimeoutByIds(any())).thenReturn(0);
        when(tmScheduleResultMapper.selectById(14L)).thenReturn(null);

        // 执行转机台时应因旧记录不存在被拒绝。
        service.changeMachine(scheduleResult);
    }

    /**
     * 测试内容：验证发布 ID 为空时拒绝发布。
     * 测试场景：publishValidate 传入空集合。
     * 预期结果：抛出 ServiceException，不查询发布状态。
     */
    @Test(expected = ServiceException.class)
    public void publishValidateShouldRejectEmptyIds() {
        // 执行发布校验时应拒绝空 ID。
        service.publishValidate(Collections.emptyList());
    }

    /**
     * 测试内容：验证发布中或超时失败记录不允许发布。
     * 测试场景：发布保护查询返回命中记录。
     * 预期结果：抛出 ServiceException，不更新发布状态。
     */
    @Test(expected = ServiceException.class)
    public void publishValidateShouldRejectWhenRecordIsReleasingOrTimeout() {
        // 准备发布保护命中的记录 ID。
        when(tmScheduleResultMapper.isReleasingOrTimeoutByIds(any())).thenReturn(1);

        // 执行发布校验时应被保护分支拒绝。
        service.publishValidate(Collections.singletonList(15L));
    }

    /**
     * 测试内容：验证合法发布会统一更新为待发布。
     * 测试场景：发布 ID 非空且没有发布中、超时失败记录。
     * 预期结果：调用 mapper.update 并返回更新行数。
     */
    @Test
    public void publishShouldUpdateReleaseStatusToWaitReleasingWhenValid() {
        // 准备合法发布请求。
        when(tmScheduleResultMapper.isReleasingOrTimeoutByIds(any())).thenReturn(0);
        when(tmScheduleResultMapper.update(any(), any())).thenReturn(2);

        // 执行发布。
        assertEquals(2, service.publish(java.util.Arrays.asList(16L, 17L)));

        // 断言执行了更新入口。
        verify(tmScheduleResultMapper).update(any(), any());
    }

    /**
     * 测试内容：验证人工插单位置小于等于第二个在产规格时会被拒绝。
     * 测试场景：同一机台同一班次前两条记录都有完成量，新插单位置为 2。
     * 预期结果：insertTask 抛出 ServiceException。
     */
    @Test(expected = ServiceException.class)
    public void insertTaskShouldRejectWhenFinishQtyPositionIsBeforeSecondInProductionSpec() {
        // 准备两个已在产规格，模拟前两个 sequence 均已有完成量。
        TmScheduleResult runningOne = new TmScheduleResult();
        runningOne.setClass1Sequence(1);
        runningOne.setClass1FinishQty(BigDecimal.ONE);
        TmScheduleResult runningTwo = new TmScheduleResult();
        runningTwo.setClass1Sequence(2);
        runningTwo.setClass1FinishQty(BigDecimal.ONE);
        when(tmScheduleResultMapper.selectList(any())).thenReturn(java.util.Arrays.asList(runningOne, runningTwo));
        // 准备插到第二个位置的新增排程记录。
        TmScheduleResult scheduleResult = new TmScheduleResult();
        scheduleResult.setScheduleDate(new Date());
        scheduleResult.setMachineCode("TM-01");
        scheduleResult.setTreadCode("TR-001");
        scheduleResult.setClass1Sequence(2);

        // 执行插单时应被在产位置规则拒绝。
        service.insertTask(scheduleResult);
    }

    /**
     * 测试内容：验证人工插单位置允许时会委托任务链操作门面执行。
     * 测试场景：前两个规格在产，新插单位置为 3，允许插到在产规格之后。
     * 预期结果：insertTask 返回成功，并调用 TmScheduleOperationFacade.insertTask。
     */
    @Test
    public void insertTaskShouldDelegateChainOperationToFacadeWhenPositionAllowed() {
        // 准备两个已在产规格，作为插单位置校验的对照数据。
        TmScheduleResult runningOne = new TmScheduleResult();
        runningOne.setClass1Sequence(1);
        runningOne.setClass1FinishQty(BigDecimal.ONE);
        TmScheduleResult runningTwo = new TmScheduleResult();
        runningTwo.setClass1Sequence(2);
        runningTwo.setClass1FinishQty(BigDecimal.ONE);
        when(tmScheduleResultMapper.selectList(any())).thenReturn(java.util.Arrays.asList(runningOne, runningTwo));
        when(tmScheduleResultMapper.insert(any(TmScheduleResult.class))).thenReturn(1);
        // 准备插到第三个位置的新增排程记录。
        TmScheduleResult scheduleResult = new TmScheduleResult();
        scheduleResult.setScheduleDate(new Date());
        scheduleResult.setMachineCode("TM-01");
        scheduleResult.setTreadCode("TR-001");
        scheduleResult.setClass1Sequence(3);

        // 执行插单并断言数据库入口返回成功。
        assertEquals(1, service.insertTask(scheduleResult));

        // 断言允许位置会继续委托任务链门面处理链表插入。
        verify(tmScheduleOperationFacade).insertTask(any(), any(), any());
    }

    /**
     * 构造自动排程测试请求。
     *
     * @return 自动排程请求
     */
    private TmAutoScheduleRequestVo buildAutoRequest() {
        TmAutoScheduleRequestVo request = new TmAutoScheduleRequestVo();
        request.setFactoryCode("116");
        request.setScheduleDate(new Date());
        request.setTraceId("TRACE-TEST");
        request.setOperator("tester");
        return request;
    }

    /**
     * 断言解释实体字段映射到指定数据库列。
     *
     * @param fieldName  实体字段名
     * @param columnName 数据库列名
     * @throws NoSuchFieldException 字段不存在时抛出
     */
    private void assertTableField(String fieldName, String columnName) throws NoSuchFieldException {
        TableField tableField = TmScheduleResultExplain.class.getDeclaredField(fieldName).getAnnotation(TableField.class);

        assertNotNull(tableField);
        assertEquals(columnName, tableField.value());
    }

    /**
     * 构造默认引擎输出。
     */
    private void mockEngineOutput() {
        mockEngineOutput(1, 0);
    }

    /**
     * 构造自动排程引擎输出。
     *
     * @param resultCount    已生成排程结果数量
     * @param unplannedCount 未排数量
     */
    private void mockEngineOutput(Integer resultCount, Integer unplannedCount) {
        doAnswer(invocation -> {
            TmScheduleContext context = invocation.getArgument(0);
            TmTaskDraft taskDraft = new TmTaskDraft();
            taskDraft.setOrderNo("ORD-001");
            taskDraft.setTreadCode("TR-001");
            taskDraft.setGlueCode("GL-001");
            taskDraft.setMouthPlateCode("MP-001");
            taskDraft.setMachineCode("TM-01");
            taskDraft.setShiftOrder(1);
            taskDraft.setPlanQty(BigDecimal.TEN);
            context.setTaskDraftList(Collections.singletonList(taskDraft));
            LocalDate scheduleDate = context.getScheduleDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            context.getTaskChainGroup().getOrCreate("TM-01", scheduleDate, 1)
                    .append(new ScheduleTaskNode<>(taskDraft.getBusinessKey(), taskDraft, "TM-01", scheduleDate,
                                    "CLASS1", 1, taskDraft.getPlanQty()),
                            new ScheduleOperationContext("tester", "AUTO_APPEND", "TRACE-TEST"));
            TmPersistResult persistResult = new TmPersistResult();
            persistResult.setResultCount(resultCount);
            persistResult.setExplainCount(resultCount);
            persistResult.setUnplannedCount(unplannedCount);
            context.setPersistResult(persistResult);
            return new TmAutoScheduleResponseVo();
        }).when(tmScheduleTemplate).execute(any(TmScheduleContext.class));
    }
}
