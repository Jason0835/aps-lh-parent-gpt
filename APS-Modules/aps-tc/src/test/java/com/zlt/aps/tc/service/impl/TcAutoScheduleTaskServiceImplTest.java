package com.zlt.aps.tc.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.vo.TcAutoScheduleRequestVo;
import com.zlt.aps.tc.api.domain.vo.TcAutoScheduleResponseVo;
import com.zlt.aps.tc.api.enums.TcAutoScheduleTaskStatusEnum;
import com.zlt.aps.tc.domain.TcAutoScheduleTask;
import com.zlt.aps.tc.mapper.TcAutoScheduleTaskMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 胎侧自动排程异步任务状态与并发防重测试。
 */
@RunWith(MockitoJUnitRunner.class)
public class TcAutoScheduleTaskServiceImplTest {

    @Mock
    private TcAutoScheduleTaskMapper taskMapper;

    @InjectMocks
    private TcAutoScheduleTaskServiceImpl taskService;

    /**
     * 初始化 MyBatis-Plus 元数据，支持 Lambda 条件解析。
     */
    @BeforeClass
    public static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                TcAutoScheduleTask.class);
    }

    /**
     * 验证任务从 PENDING 流转到 RUNNING 和 SUCCESS，进度最终为 100%。
     */
    @Test
    public void shouldTransitPendingRunningAndSuccess() {
        TcAutoScheduleRequestVo request = this.buildRequest();
        TcAutoScheduleResponseVo initialResponse = new TcAutoScheduleResponseVo();
        initialResponse.setBatchNo("TC-BATCH-001");
        initialResponse.setTraceId("TRACE-001");
        when(this.taskMapper.selectOne(any())).thenReturn(null);

        TcAutoScheduleTask task = this.taskService.createPending(request, initialResponse);

        assertNotNull(task.getTaskId());
        assertEquals(TcAutoScheduleTaskStatusEnum.PENDING.getCode(), task.getTaskStatus());
        verify(this.taskMapper).insert(task);
        when(this.taskMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        assertTrue(this.taskService.start(task.getTaskId()));
        TcAutoScheduleResponseVo successResponse = new TcAutoScheduleResponseVo();
        successResponse.setBatchNo("TC-BATCH-001");
        successResponse.setTraceId("TRACE-001");
        assertTrue(this.taskService.markSuccess(task.getTaskId(), successResponse, Collections.emptyList()));
        assertEquals(TcAutoScheduleTaskStatusEnum.SUCCESS.getCode(), successResponse.getTaskStatus());
        assertEquals(Integer.valueOf(100), successResponse.getProgress());
        assertEquals(TcScheduleConstants.AUTO_SCHEDULE_STAGE_COMPLETE, successResponse.getCurrentStage());
    }

    /**
     * 验证同工厂同日期已有活动任务时直接复用，不重复插入任务。
     */
    @Test
    public void shouldReuseExistingActiveTask() {
        TcAutoScheduleTask activeTask = new TcAutoScheduleTask();
        activeTask.setTaskId("TC-ACTIVE");
        when(this.taskMapper.selectOne(any())).thenReturn(activeTask);

        TcAutoScheduleTask result = this.taskService.createPending(this.buildRequest(),
                new TcAutoScheduleResponseVo());

        assertSame(activeTask, result);
    }

    /**
     * 构造合法自动排程请求。
     *
     * @return 测试请求
     */
    private TcAutoScheduleRequestVo buildRequest() {
        TcAutoScheduleRequestVo request = new TcAutoScheduleRequestVo();
        request.setFactoryCode("116");
        request.setScheduleDate(new Date());
        request.setOperator("tester");
        return request;
    }
}

