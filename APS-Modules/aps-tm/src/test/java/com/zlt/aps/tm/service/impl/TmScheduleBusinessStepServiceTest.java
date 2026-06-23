package com.zlt.aps.tm.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.zlt.aps.common.engine.schedule.ScheduleOperationContext;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResultExplain;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmSnapshotBuildResult;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.engine.service.impl.TmPersistService;
import com.zlt.aps.tm.engine.service.impl.TmPlanBootstrapService;
import com.zlt.aps.tm.engine.service.impl.TmSnapshotBuildService;
import com.zlt.aps.tm.mapper.TmScheduleResultExplainMapper;
import com.zlt.aps.tm.mapper.TmScheduleResultMapper;
import com.zlt.aps.tm.service.TmAutoScheduleDataLoadService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 胎面自动排程业务步骤测试。
 *
 * <p>验证业务模块接管模板初始化的数据加载职责，以及解释快照步骤的实际落库职责。</p>
 */
@RunWith(MockitoJUnitRunner.class)
public class TmScheduleBusinessStepServiceTest {

    @Mock
    private TmAutoScheduleDataLoadService dataLoadService;

    @Mock
    private TmScheduleResultMapper scheduleResultMapper;

    @Mock
    private TmScheduleResultExplainMapper scheduleResultExplainMapper;

    @Mock
    private TmPersistService persistService;

    @Test
    public void bizBootstrapShouldInitializeContextAndLoadAllData() {
        TmScheduleContext context = buildContext();
        TmBizPlanBootstrapService service = new TmBizPlanBootstrapService(new TmPlanBootstrapService(), dataLoadService);

        service.bootstrap(context);

        assertTrue(StrUtil.isNotBlank(context.getBatchNo()));
        assertTrue(StrUtil.isNotBlank(context.getTraceId()));
        verify(dataLoadService).loadAllData(context);
    }

    @Test
    public void bizSnapshotAndPersistShouldBuildSnapshotInsertRowsAndFillPersistResult() {
        TmScheduleContext context = buildContext();
        TmTaskDraft taskDraft = buildTask();
        context.setTaskDraftList(Collections.singletonList(taskDraft));
        LocalDate scheduleDate = DateUtil.toLocalDateTime(context.getScheduleDate()).toLocalDate();
        context.getTaskChainGroup().getOrCreate("TM-01", scheduleDate, 1)
                .append(new ScheduleTaskNode<>(taskDraft.getBusinessKey(), taskDraft, "TM-01", scheduleDate,
                                "CLASS1", 1, taskDraft.getPlanQty()),
                        new ScheduleOperationContext("tester", "AUTO_APPEND", "TRACE-TEST"));
        TmScheduleResult result = new TmScheduleResult();
        result.setMachineCode("TM-01");
        TmScheduleResultExplain explain = new TmScheduleResultExplain();
        when(persistService.convertChainToResult(any(), any(TmScheduleContext.class)))
                .thenReturn(Collections.singletonList(result));
        when(persistService.convertExplain(any(TmTaskDraft.class), any(TmSnapshotBuildResult.class), any(TmScheduleContext.class)))
                .thenReturn(explain);
        when(scheduleResultMapper.insert(result)).thenAnswer(invocation -> {
            result.setId(1L);
            return 1;
        });

        new TmBizSnapshotAndPersistService(new TmSnapshotBuildService(), persistService,
                scheduleResultMapper, scheduleResultExplainMapper).snapshotAndPersist(context);

        verify(scheduleResultMapper).insert(result);
        verify(scheduleResultExplainMapper).insert(explain);
        assertEquals(1, context.getPersistResult().getResultCount());
        assertEquals(1, context.getPersistResult().getExplainCount());
        assertEquals(0, context.getPersistResult().getUnplannedCount());
    }

    /**
     * 构造自动排程上下文。
     *
     * @return 自动排程上下文
     */
    private TmScheduleContext buildContext() {
        TmScheduleContext context = new TmScheduleContext();
        context.setFactoryCode("116");
        context.setScheduleDate(DateUtil.parseDate("2026-06-17"));
        context.setOperator("tester");
        return context;
    }

    /**
     * 构造已分配机台的任务草稿。
     *
     * @return 任务草稿
     */
    private TmTaskDraft buildTask() {
        TmTaskDraft taskDraft = new TmTaskDraft();
        taskDraft.setOrderNo("ORD-001");
        taskDraft.setTreadCode("TR-001");
        taskDraft.setGlueCode("GL-001");
        taskDraft.setMouthPlateCode("MP-001");
        taskDraft.setMachineCode("TM-01");
        taskDraft.setShiftOrder(1);
        taskDraft.setPlanQty(BigDecimal.TEN);
        taskDraft.setDemandQty(BigDecimal.TEN);
        return taskDraft;
    }
}
