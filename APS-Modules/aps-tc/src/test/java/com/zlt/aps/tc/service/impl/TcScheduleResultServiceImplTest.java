package com.zlt.aps.tc.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.domain.vo.TcAutoScheduleRequestVo;
import com.zlt.aps.tc.api.domain.vo.TcAutoScheduleResponseVo;
import com.zlt.aps.tc.component.TcAutoScheduleExecutionGuard;
import com.zlt.aps.tc.domain.TcAutoScheduleTask;
import com.zlt.aps.tc.mapper.TcScheduleResultMapper;
import com.zlt.aps.tc.service.TcAutoScheduleAsyncExecutor;
import com.zlt.aps.tc.service.TcAutoScheduleTaskService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

/**
 * 胎侧自动排程旧结果覆盖规则测试。
 */
@RunWith(MockitoJUnitRunner.class)
public class TcScheduleResultServiceImplTest {

    @Mock
    private TcScheduleResultMapper scheduleResultMapper;

    @Mock
    private TcAutoScheduleTaskService autoScheduleTaskService;

    @Mock
    private TcAutoScheduleAsyncExecutor autoScheduleAsyncExecutor;

    @Mock
    private TcAutoScheduleExecutionGuard autoScheduleExecutionGuard;

    /**
     * 初始化 MyBatis-Plus 元数据，支持 Lambda 条件构造。
     */
    @BeforeClass
    public static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                TcScheduleResult.class);
    }

    /**
     * 验证仅存在待发布状态旧结果时，校验接口返回需要覆盖确认。
     *
     * @throws Exception 反射注入测试依赖失败时抛出
     */
    @Test
    public void shouldRequireConfirmationForReplaceableResults() throws Exception {
        TcScheduleResult oldResult = new TcScheduleResult();
        oldResult.setReleaseStatus("0");
        when(this.scheduleResultMapper.selectList(any())).thenReturn(Collections.singletonList(oldResult));

        TcAutoScheduleResponseVo response = this.buildService().validateAutoPlan(this.buildRequest(false));

        assertTrue(Boolean.TRUE.equals(response.getSuccess()));
        assertTrue(Boolean.TRUE.equals(response.getConfirmRequired()));
    }

    /**
     * 验证存在已发布状态旧结果时阻断重排。
     *
     * @throws Exception 反射注入测试依赖失败时抛出
     */
    @Test
    public void shouldBlockPublishedResults() throws Exception {
        TcScheduleResult oldResult = new TcScheduleResult();
        oldResult.setReleaseStatus("1");
        when(this.scheduleResultMapper.selectList(any())).thenReturn(Collections.singletonList(oldResult));

        try {
            this.buildService().validateAutoPlan(this.buildRequest(true));
            fail("已发布结果存在时应阻断自动重排");
        } catch (ServiceException exception) {
            assertTrue(exception.getMessage() != null && !exception.getMessage().isEmpty());
        }
    }

    /**
     * 验证提交锁覆盖活跃任务检查和待执行任务创建，并在异步执行前释放。
     *
     * @throws Exception 反射注入测试依赖失败时抛出
     */
    @Test
    public void autoPlanShouldCreatePendingTaskInsideGuardAndReleaseBeforeExecute() throws Exception {
        when(this.scheduleResultMapper.selectList(any())).thenReturn(Collections.emptyList());
        Date scheduleDate = new Date();
        TcAutoScheduleRequestVo request = this.buildRequest(true);
        request.setScheduleDate(scheduleDate);
        TcAutoScheduleTask task = new TcAutoScheduleTask();
        task.setTaskId("TC-TASK-001");
        TcAutoScheduleResponseVo taskResponse = new TcAutoScheduleResponseVo();
        when(this.autoScheduleExecutionGuard.acquire("116", scheduleDate)).thenReturn("guard-token");
        when(this.autoScheduleTaskService.createPending(any(), any())).thenReturn(task);
        when(this.autoScheduleTaskService.toResponse(task)).thenReturn(taskResponse);

        TcAutoScheduleResponseVo response = this.buildService().autoPlan(request);

        assertTrue(Boolean.TRUE.equals(response.getSuccess()));
        org.mockito.InOrder invocationOrder = inOrder(this.autoScheduleExecutionGuard,
                this.autoScheduleTaskService, this.autoScheduleAsyncExecutor);
        invocationOrder.verify(this.autoScheduleExecutionGuard).acquire("116", scheduleDate);
        invocationOrder.verify(this.autoScheduleTaskService).findActive("116", scheduleDate);
        invocationOrder.verify(this.autoScheduleTaskService).createPending(
                any(TcAutoScheduleRequestVo.class), any(TcAutoScheduleResponseVo.class));
        invocationOrder.verify(this.autoScheduleExecutionGuard).release("116", scheduleDate, "guard-token");
        invocationOrder.verify(this.autoScheduleAsyncExecutor).execute("TC-TASK-001");
    }

    /**
     * 创建只注入覆盖校验 Mapper 的结果服务。
     *
     * @return 结果服务
     * @throws Exception 反射注入字段失败时抛出
     */
    private TcScheduleResultServiceImpl buildService() throws Exception {
        TcScheduleResultServiceImpl service = new TcScheduleResultServiceImpl();
        this.setField(service, "tcScheduleResultMapper", this.scheduleResultMapper);
        this.setField(service, "tcAutoScheduleTaskService", this.autoScheduleTaskService);
        this.setField(service, "tcAutoScheduleAsyncExecutor", this.autoScheduleAsyncExecutor);
        this.setField(service, "tcAutoScheduleExecutionGuard", this.autoScheduleExecutionGuard);
        return service;
    }

    /**
     * 反射注入结果服务测试依赖。
     *
     * @param service 结果服务
     * @param fieldName 字段名称
     * @param fieldValue 字段值
     * @throws Exception 字段不存在或无法赋值时抛出
     */
    private void setField(TcScheduleResultServiceImpl service, String fieldName, Object fieldValue) throws Exception {
        Field field = TcScheduleResultServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(service, fieldValue);
    }

    /**
     * 构造自动排程请求。
     *
     * @param confirmOverwrite 是否确认覆盖
     * @return 自动排程请求
     */
    private TcAutoScheduleRequestVo buildRequest(boolean confirmOverwrite) {
        TcAutoScheduleRequestVo request = new TcAutoScheduleRequestVo();
        request.setFactoryCode("116");
        request.setScheduleDate(new Date());
        request.setOperator("tester");
        request.setConfirmOverwrite(confirmOverwrite);
        return request;
    }
}
