package com.zlt.aps.tc.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.domain.vo.TcReleaseFeedbackItemVo;
import com.zlt.aps.tc.api.domain.vo.TcReleaseFeedbackVo;
import com.zlt.aps.tc.api.domain.vo.TcReleaseTaskVo;
import com.zlt.aps.tc.domain.TcAutoScheduleTask;
import com.zlt.aps.tc.domain.TcReleaseTaskDetail;
import com.zlt.aps.tc.mapper.TcReleaseCallbackLogMapper;
import com.zlt.aps.tc.mapper.TcReleaseTaskDetailMapper;
import com.zlt.aps.tc.mapper.TcScheduleResultMapper;
import com.zlt.core.dao.basedao.BaseDao;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.util.Collections;

/**
 * 胎侧MES发布反馈去重和任务收口测试。
 */
public class TcReleaseFeedbackServiceTest {

    /**
     * 成功反馈应更新排程结果，并在全部明细终态后收口发布任务。
     */
    @Test
    public void shouldApplySuccessFeedbackAndFinishReleaseTask() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                TcScheduleResult.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                TcReleaseTaskDetail.class);
        TcReleaseTaskDetailMapper detailMapper = Mockito.mock(TcReleaseTaskDetailMapper.class);
        TcReleaseCallbackLogMapper callbackLogMapper = Mockito.mock(TcReleaseCallbackLogMapper.class);
        TcScheduleResultMapper scheduleResultMapper = Mockito.mock(TcScheduleResultMapper.class);
        TcBackgroundTaskService backgroundTaskService = Mockito.mock(TcBackgroundTaskService.class);
        BaseDao baseDao = Mockito.mock(BaseDao.class);
        PlatformTransactionManager transactionManager = Mockito.mock(PlatformTransactionManager.class);
        Mockito.when(transactionManager.getTransaction(ArgumentMatchers.any(TransactionDefinition.class)))
                .thenReturn(Mockito.mock(TransactionStatus.class));

        TcReleaseTaskDetail detail = new TcReleaseTaskDetail();
        detail.setId(1L);
        detail.setTaskId("TC-REL-001");
        detail.setResultId(10L);
        detail.setTaskVersion(2L);
        detail.setIdempotencyKey("B001|TC001|2");
        detail.setCallbackStatus("PENDING");
        Mockito.when(callbackLogMapper.selectCount(ArgumentMatchers.any())).thenReturn(0L);
        Mockito.when(detailMapper.selectOne(ArgumentMatchers.any())).thenReturn(detail);
        TcReleaseTaskDetail terminalDetail = new TcReleaseTaskDetail();
        terminalDetail.setCallbackStatus("SUCCESS");
        Mockito.when(detailMapper.selectList(ArgumentMatchers.any()))
                .thenReturn(Collections.singletonList(terminalDetail));
        Mockito.when(scheduleResultMapper.update(ArgumentMatchers.isNull(), ArgumentMatchers.any()))
                .thenReturn(1);
        TcAutoScheduleTask task = new TcAutoScheduleTask();
        task.setTaskId("TC-REL-001");
        Mockito.when(backgroundTaskService.findByTaskId("TC-REL-001")).thenReturn(task);
        Mockito.when(backgroundTaskService.toReleaseTaskVo(task)).thenReturn(new TcReleaseTaskVo());

        TcReleaseFeedbackService service = new TcReleaseFeedbackService(detailMapper, callbackLogMapper,
                scheduleResultMapper, backgroundTaskService, baseDao, transactionManager);
        TcReleaseFeedbackItemVo feedbackItem = new TcReleaseFeedbackItemVo();
        feedbackItem.setIdempotencyKey("B001|TC001|2");
        feedbackItem.setFeedbackStatus("SUCCESS");
        TcReleaseFeedbackVo feedback = new TcReleaseFeedbackVo();
        feedback.setDataVersion("TCREL-001");
        feedback.setCallbackVersion("CALLBACK-001");
        feedback.setItems(Collections.singletonList(feedbackItem));

        AjaxResult result = service.applyFeedback(feedback);

        Assert.assertEquals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG));
        Mockito.verify(backgroundTaskService).markSuccess(
                ArgumentMatchers.eq("TC-REL-001"), ArgumentMatchers.any(),
                ArgumentMatchers.anyMap(), ArgumentMatchers.anyList());
    }
}
