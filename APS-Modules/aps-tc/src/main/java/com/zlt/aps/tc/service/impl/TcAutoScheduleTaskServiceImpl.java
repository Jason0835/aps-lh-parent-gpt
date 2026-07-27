package com.zlt.aps.tc.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.vo.TcAutoScheduleIssueVo;
import com.zlt.aps.tc.api.domain.vo.TcAutoScheduleRequestVo;
import com.zlt.aps.tc.api.domain.vo.TcAutoScheduleResponseVo;
import com.zlt.aps.tc.api.enums.TcAutoScheduleTaskStatusEnum;
import com.zlt.aps.tc.api.enums.TcBackgroundTaskTypeEnum;
import com.zlt.aps.tc.domain.TcAutoScheduleTask;
import com.zlt.aps.tc.mapper.TcAutoScheduleTaskMapper;
import com.zlt.aps.tc.service.TcAutoScheduleTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 胎侧自动排程异步任务状态服务实现。
 *
 * <p>任务状态和进度使用独立短事务保存，避免长时间自动排程事务影响前端轮询。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TcAutoScheduleTaskServiceImpl implements TcAutoScheduleTaskService {

    private final TcAutoScheduleTaskMapper taskMapper;

    /**
     * 创建等待执行的胎侧自动排程任务。
     *
     * @param request  自动排程请求
     * @param response 初始排程响应
     * @return 任务对象
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public TcAutoScheduleTask createPending(TcAutoScheduleRequestVo request, TcAutoScheduleResponseVo response) {
        TcAutoScheduleTask activeTask = this.findActive(request.getFactoryCode(), request.getScheduleDate());
        if (activeTask != null) {
            if (TcBackgroundTaskTypeEnum.AUTO_PLAN.getCode().equals(activeTask.getTaskType())) {
                return activeTask;
            }
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.concurrentTask"));
        }
        TcAutoScheduleTask task = new TcAutoScheduleTask();
        task.setTaskId(TcScheduleConstants.AUTO_SCHEDULE_TASK_ID_PREFIX
                + IdUtil.fastSimpleUUID().toUpperCase());
        task.setFactoryCode(request.getFactoryCode());
        task.setScheduleDate(request.getScheduleDate());
        task.setTaskType(TcBackgroundTaskTypeEnum.AUTO_PLAN.getCode());
        task.setBatchNo(response.getBatchNo());
        task.setTraceId(response.getTraceId());
        task.setTaskStatus(TcAutoScheduleTaskStatusEnum.PENDING.getCode());
        task.setProgress(0);
        task.setCurrentStage(TcAutoScheduleTaskStatusEnum.PENDING.getCode());
        task.setCurrentStageName(this.resolveTcMessage("ui.tc.schedule.taskPending"));
        task.setRequestSnapshot(JSON.toJSONString(request));
        task.setCreateBy(StrUtil.blankToDefault(request.getOperator(), "system"));
        taskMapper.insert(task);
        return task;
    }

    @Override
    public TcAutoScheduleTask findByTaskId(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            return null;
        }
        return taskMapper.selectOne(new LambdaQueryWrapper<TcAutoScheduleTask>()
                .eq(TcAutoScheduleTask::getTaskId, taskId));
    }

    @Override
    public TcAutoScheduleTask findLatest(String factoryCode, Date scheduleDate) {
        return taskMapper.selectOne(new LambdaQueryWrapper<TcAutoScheduleTask>()
                .eq(TcAutoScheduleTask::getFactoryCode, factoryCode)
                .eq(TcAutoScheduleTask::getScheduleDate, scheduleDate)
                .eq(TcAutoScheduleTask::getTaskType, TcBackgroundTaskTypeEnum.AUTO_PLAN.getCode())
                .orderByDesc(TcAutoScheduleTask::getCreateTime)
                .last("limit 1"));
    }

    @Override
    public TcAutoScheduleTask findActive(String factoryCode, Date scheduleDate) {
        return taskMapper.selectOne(new LambdaQueryWrapper<TcAutoScheduleTask>()
                .eq(TcAutoScheduleTask::getFactoryCode, factoryCode)
                .eq(TcAutoScheduleTask::getScheduleDate, scheduleDate)
                .in(TcAutoScheduleTask::getTaskStatus,
                        Arrays.asList(TcAutoScheduleTaskStatusEnum.PENDING.getCode(),
                                TcAutoScheduleTaskStatusEnum.RUNNING.getCode()))
                .orderByDesc(TcAutoScheduleTask::getCreateTime)
                .last("limit 1"));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean start(String taskId) {
        Date now = new Date();
        return taskMapper.update(null, new LambdaUpdateWrapper<TcAutoScheduleTask>()
                .eq(TcAutoScheduleTask::getTaskId, taskId)
                .eq(TcAutoScheduleTask::getTaskStatus, TcAutoScheduleTaskStatusEnum.PENDING.getCode())
                .set(TcAutoScheduleTask::getTaskStatus, TcAutoScheduleTaskStatusEnum.RUNNING.getCode())
                .set(TcAutoScheduleTask::getProgress, 0)
                .set(TcAutoScheduleTask::getCurrentStage,
                        TcScheduleConstants.AUTO_SCHEDULE_STAGE_REQUEST_VALIDATED)
                .set(TcAutoScheduleTask::getCurrentStageName, this.resolveTcMessage("ui.tc.schedule.taskRequestAccepted"))
                .set(TcAutoScheduleTask::getStartTime, now)
                .set(TcAutoScheduleTask::getLastHeartbeatTime, now)) == 1;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean updateProgress(String taskId, int progress, String stage, String stageName) {
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException(this.resolveTcMessage("ui.tc.schedule.taskProgressInvalid"));
        }
        return taskMapper.update(null, new LambdaUpdateWrapper<TcAutoScheduleTask>()
                .eq(TcAutoScheduleTask::getTaskId, taskId)
                .eq(TcAutoScheduleTask::getTaskStatus, TcAutoScheduleTaskStatusEnum.RUNNING.getCode())
                .set(TcAutoScheduleTask::getProgress, progress)
                .set(TcAutoScheduleTask::getCurrentStage, stage)
                .set(TcAutoScheduleTask::getCurrentStageName, stageName)
                .set(TcAutoScheduleTask::getLastHeartbeatTime, new Date())) == 1;
    }

    /**
     * 更新本次排程参数快照。
     *
     * @param taskId 对外任务 ID
     * @param paramSnapshot 参数快照对象
     * @return true 表示更新成功
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean updateParamSnapshot(String taskId, Object paramSnapshot) {
        return taskMapper.update(null, new LambdaUpdateWrapper<TcAutoScheduleTask>()
                .eq(TcAutoScheduleTask::getTaskId, taskId)
                .eq(TcAutoScheduleTask::getTaskStatus, TcAutoScheduleTaskStatusEnum.RUNNING.getCode())
                .set(TcAutoScheduleTask::getParamSnapshotJson, JSON.toJSONString(paramSnapshot))
                .set(TcAutoScheduleTask::getLastHeartbeatTime, new Date())) == 1;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean markSuccess(String taskId, TcAutoScheduleResponseVo response, List<TcAutoScheduleIssueVo> issues) {
        List<TcAutoScheduleIssueVo> issueList = issues == null ? new ArrayList<>() : issues;
        response.setTaskId(taskId);
        response.setTaskStatus(TcAutoScheduleTaskStatusEnum.SUCCESS.getCode());
        response.setProgress(100);
        response.setCurrentStage(TcScheduleConstants.AUTO_SCHEDULE_STAGE_COMPLETE);
        response.setCurrentStageName(this.resolveTcMessage("ui.tc.schedule.taskCompleted"));
        response.setIssues(issueList);
        response.setIssueCount(issueList.size());
        return taskMapper.update(null, new LambdaUpdateWrapper<TcAutoScheduleTask>()
                .eq(TcAutoScheduleTask::getTaskId, taskId)
                .in(TcAutoScheduleTask::getTaskStatus, Arrays.asList(
                        TcAutoScheduleTaskStatusEnum.RUNNING.getCode(),
                        TcAutoScheduleTaskStatusEnum.SUCCESS.getCode()))
                .set(TcAutoScheduleTask::getTaskStatus, TcAutoScheduleTaskStatusEnum.SUCCESS.getCode())
                .set(TcAutoScheduleTask::getProgress, 100)
                .set(TcAutoScheduleTask::getCurrentStage, TcScheduleConstants.AUTO_SCHEDULE_STAGE_COMPLETE)
                .set(TcAutoScheduleTask::getCurrentStageName, this.resolveTcMessage("ui.tc.schedule.taskCompleted"))
                .set(TcAutoScheduleTask::getBatchNo, response.getBatchNo())
                .set(TcAutoScheduleTask::getTraceId, response.getTraceId())
                .set(TcAutoScheduleTask::getResultJson, JSON.toJSONString(response))
                .set(TcAutoScheduleTask::getIssueJson, JSON.toJSONString(issueList))
                .set(TcAutoScheduleTask::getSummaryJson, JSON.toJSONString(response.getSummary()))
                .set(TcAutoScheduleTask::getEndTime, new Date())
                .set(TcAutoScheduleTask::getLastHeartbeatTime, new Date())) == 1;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean markFailed(String taskId, String errorMessage, List<TcAutoScheduleIssueVo> issues) {
        String summary = StrUtil.blankToDefault(errorMessage, this.resolveTcMessage("ui.tc.schedule.taskExecuteFailed"));
        if (summary.length() > TcScheduleConstants.MAX_ERROR_MESSAGE_LENGTH) {
            summary = summary.substring(0, TcScheduleConstants.MAX_ERROR_MESSAGE_LENGTH);
        }
        List<TcAutoScheduleIssueVo> issueList = issues == null ? new ArrayList<>() : issues;
        return taskMapper.update(null, new LambdaUpdateWrapper<TcAutoScheduleTask>()
                .eq(TcAutoScheduleTask::getTaskId, taskId)
                .in(TcAutoScheduleTask::getTaskStatus,
                        Arrays.asList(TcAutoScheduleTaskStatusEnum.PENDING.getCode(),
                                TcAutoScheduleTaskStatusEnum.RUNNING.getCode()))
                .set(TcAutoScheduleTask::getTaskStatus, TcAutoScheduleTaskStatusEnum.FAILED.getCode())
                .set(TcAutoScheduleTask::getCurrentStage, TcAutoScheduleTaskStatusEnum.FAILED.getCode())
                .set(TcAutoScheduleTask::getCurrentStageName, this.resolveTcMessage("ui.tc.schedule.taskFailed"))
                .set(TcAutoScheduleTask::getErrorMessage, summary)
                .set(TcAutoScheduleTask::getErrorSummary, summary)
                .set(TcAutoScheduleTask::getIssueJson, JSON.toJSONString(issueList))
                .set(TcAutoScheduleTask::getEndTime, new Date())
                .set(TcAutoScheduleTask::getLastHeartbeatTime, new Date())) == 1;
    }

    @Override
    public TcAutoScheduleResponseVo toResponse(TcAutoScheduleTask task) {
        if (task == null) {
            return null;
        }
        TcAutoScheduleResponseVo response = StrUtil.isBlank(task.getResultJson())
                ? new TcAutoScheduleResponseVo()
                : JSON.parseObject(task.getResultJson(), TcAutoScheduleResponseVo.class);
        List<TcAutoScheduleIssueVo> issues = StrUtil.isBlank(task.getIssueJson())
                ? new ArrayList<>()
                : JSON.parseArray(task.getIssueJson(), TcAutoScheduleIssueVo.class);
        response.setTaskId(task.getTaskId());
        response.setTaskStatus(task.getTaskStatus());
        response.setProgress(task.getProgress());
        response.setCurrentStage(task.getCurrentStage());
        response.setCurrentStageName(task.getCurrentStageName());
        response.setBatchNo(StrUtil.blankToDefault(response.getBatchNo(), task.getBatchNo()));
        response.setTraceId(StrUtil.blankToDefault(response.getTraceId(), task.getTraceId()));
        response.setIssues(issues);
        if ((response.getSummary() == null || response.getSummary().isEmpty())
                && StrUtil.isNotBlank(task.getSummaryJson())) {
            response.setSummary(JSON.parseObject(task.getSummaryJson(), java.util.LinkedHashMap.class));
        }
        response.setIssueCount(issues.size());
        if (TcAutoScheduleTaskStatusEnum.FAILED.getCode().equals(task.getTaskStatus())) {
            response.setSuccess(Boolean.FALSE);
            response.setMessage(task.getErrorMessage());
        }
        return response;
    }
    /**
     * 获取胎侧自动排程多语言提示。
     *
     * @param key 多语言 key
     * @return 可展示提示
     */
    private String resolveTcMessage(String key) {
        return I18nUtil.getMessage(key);
    }
}
