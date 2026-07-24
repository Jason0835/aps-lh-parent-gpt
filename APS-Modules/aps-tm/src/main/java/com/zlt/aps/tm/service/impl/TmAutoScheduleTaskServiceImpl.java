package com.zlt.aps.tm.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleIssueVo;
import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleRequestVo;
import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleResponseVo;
import com.zlt.aps.tm.api.enums.TmAutoScheduleTaskStatusEnum;
import com.zlt.aps.tm.api.enums.TmBackgroundTaskTypeEnum;
import com.zlt.aps.tm.domain.TmAutoScheduleTask;
import com.zlt.aps.tm.mapper.TmAutoScheduleTaskMapper;
import com.zlt.aps.tm.service.TmAutoScheduleTaskService;
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
 * 胎面自动排程异步任务状态服务实现。
 *
 * <p>任务状态和进度使用独立短事务保存，避免长时间自动排程事务影响前端轮询。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TmAutoScheduleTaskServiceImpl implements TmAutoScheduleTaskService {

    private final TmAutoScheduleTaskMapper taskMapper;

    /**
     * 创建等待执行的胎面自动排程任务。
     *
     * @param request  自动排程请求
     * @param response 初始排程响应
     * @return 任务对象
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public TmAutoScheduleTask createPending(TmAutoScheduleRequestVo request, TmAutoScheduleResponseVo response) {
        TmAutoScheduleTask activeTask = this.findActive(request.getFactoryCode(), request.getScheduleDate());
        if (activeTask != null) {
            if (TmBackgroundTaskTypeEnum.AUTO_PLAN.getCode().equals(activeTask.getTaskType())) {
                return activeTask;
            }
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.running"));
        }
        TmAutoScheduleTask task = new TmAutoScheduleTask();
        task.setTaskId(TmScheduleConstants.AUTO_SCHEDULE_TASK_ID_PREFIX
                + IdUtil.fastSimpleUUID().toUpperCase());
        task.setFactoryCode(request.getFactoryCode());
        task.setScheduleDate(request.getScheduleDate());
        task.setTaskType(TmBackgroundTaskTypeEnum.AUTO_PLAN.getCode());
        task.setBatchNo(response.getBatchNo());
        task.setTraceId(response.getTraceId());
        task.setTaskStatus(TmAutoScheduleTaskStatusEnum.PENDING.getCode());
        task.setProgress(0);
        task.setCurrentStage(TmAutoScheduleTaskStatusEnum.PENDING.getCode());
        task.setCurrentStageName(this.resolveTmMessage("ui.data.alert.tm.schedule.taskPending", "等待自动排程执行"));
        task.setRequestSnapshot(JSON.toJSONString(request));
        task.setCreateBy(StrUtil.blankToDefault(request.getOperator(), "system"));
        taskMapper.insert(task);
        return task;
    }

    @Override
    public TmAutoScheduleTask findByTaskId(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            return null;
        }
        return taskMapper.selectOne(new LambdaQueryWrapper<TmAutoScheduleTask>()
                .eq(TmAutoScheduleTask::getTaskId, taskId));
    }

    @Override
    public TmAutoScheduleTask findLatest(String factoryCode, Date scheduleDate) {
        return taskMapper.selectOne(new LambdaQueryWrapper<TmAutoScheduleTask>()
                .eq(TmAutoScheduleTask::getFactoryCode, factoryCode)
                .eq(TmAutoScheduleTask::getScheduleDate, scheduleDate)
                .eq(TmAutoScheduleTask::getTaskType, TmBackgroundTaskTypeEnum.AUTO_PLAN.getCode())
                .orderByDesc(TmAutoScheduleTask::getCreateTime)
                .last("limit 1"));
    }

    @Override
    public TmAutoScheduleTask findActive(String factoryCode, Date scheduleDate) {
        return taskMapper.selectOne(new LambdaQueryWrapper<TmAutoScheduleTask>()
                .eq(TmAutoScheduleTask::getFactoryCode, factoryCode)
                .eq(TmAutoScheduleTask::getScheduleDate, scheduleDate)
                .in(TmAutoScheduleTask::getTaskStatus,
                        Arrays.asList(TmAutoScheduleTaskStatusEnum.PENDING.getCode(),
                                TmAutoScheduleTaskStatusEnum.RUNNING.getCode()))
                .orderByDesc(TmAutoScheduleTask::getCreateTime)
                .last("limit 1"));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean start(String taskId) {
        Date now = new Date();
        return taskMapper.update(null, new LambdaUpdateWrapper<TmAutoScheduleTask>()
                .eq(TmAutoScheduleTask::getTaskId, taskId)
                .eq(TmAutoScheduleTask::getTaskStatus, TmAutoScheduleTaskStatusEnum.PENDING.getCode())
                .set(TmAutoScheduleTask::getTaskStatus, TmAutoScheduleTaskStatusEnum.RUNNING.getCode())
                .set(TmAutoScheduleTask::getProgress, 5)
                .set(TmAutoScheduleTask::getCurrentStage,
                        TmScheduleConstants.AUTO_SCHEDULE_STAGE_REQUEST_VALIDATED)
                .set(TmAutoScheduleTask::getCurrentStageName, this.resolveTmMessage("ui.data.alert.tm.schedule.taskRequestAccepted", "自动排程请求已接收"))
                .set(TmAutoScheduleTask::getStartTime, now)
                .set(TmAutoScheduleTask::getLastHeartbeatTime, now)) == 1;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean updateProgress(String taskId, int progress, String stage, String stageName) {
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException(this.resolveTmMessage("ui.data.alert.tm.schedule.taskProgressInvalid", "自动排程任务进度必须在0至100之间"));
        }
        return taskMapper.update(null, new LambdaUpdateWrapper<TmAutoScheduleTask>()
                .eq(TmAutoScheduleTask::getTaskId, taskId)
                .eq(TmAutoScheduleTask::getTaskStatus, TmAutoScheduleTaskStatusEnum.RUNNING.getCode())
                // 只接受不小于数据库当前值的进度，避免异步阶段回调乱序造成页面进度倒退。
                .le(TmAutoScheduleTask::getProgress, progress)
                .set(TmAutoScheduleTask::getProgress, progress)
                .set(TmAutoScheduleTask::getCurrentStage, stage)
                .set(TmAutoScheduleTask::getCurrentStageName, stageName)
                .set(TmAutoScheduleTask::getLastHeartbeatTime, new Date())) == 1;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean markSuccess(String taskId, TmAutoScheduleResponseVo response, List<TmAutoScheduleIssueVo> issues) {
        List<TmAutoScheduleIssueVo> issueList = issues == null ? new ArrayList<>() : issues;
        response.setTaskId(taskId);
        response.setTaskStatus(TmAutoScheduleTaskStatusEnum.SUCCESS.getCode());
        response.setProgress(100);
        response.setCurrentStage(TmScheduleConstants.AUTO_SCHEDULE_STAGE_COMPLETE);
        response.setCurrentStageName(this.resolveTmMessage("ui.data.alert.tm.schedule.taskCompleted", "执行完成"));
        response.setIssues(issueList);
        response.setIssueCount(issueList.size());
        return taskMapper.update(null, new LambdaUpdateWrapper<TmAutoScheduleTask>()
                .eq(TmAutoScheduleTask::getTaskId, taskId)
                .eq(TmAutoScheduleTask::getTaskStatus, TmAutoScheduleTaskStatusEnum.RUNNING.getCode())
                .set(TmAutoScheduleTask::getTaskStatus, TmAutoScheduleTaskStatusEnum.SUCCESS.getCode())
                .set(TmAutoScheduleTask::getProgress, 100)
                .set(TmAutoScheduleTask::getCurrentStage, TmScheduleConstants.AUTO_SCHEDULE_STAGE_COMPLETE)
                .set(TmAutoScheduleTask::getCurrentStageName, this.resolveTmMessage("ui.data.alert.tm.schedule.taskCompleted", "执行完成"))
                .set(TmAutoScheduleTask::getBatchNo, response.getBatchNo())
                .set(TmAutoScheduleTask::getTraceId, response.getTraceId())
                .set(TmAutoScheduleTask::getResultJson, JSON.toJSONString(response))
                .set(TmAutoScheduleTask::getIssueJson, JSON.toJSONString(issueList))
                .set(TmAutoScheduleTask::getEndTime, new Date())
                .set(TmAutoScheduleTask::getLastHeartbeatTime, new Date())) == 1;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean markFailed(String taskId, String errorMessage, List<TmAutoScheduleIssueVo> issues) {
        String summary = StrUtil.blankToDefault(errorMessage, this.resolveTmMessage("ui.data.alert.tm.schedule.taskExecuteFailed", "胎面自动排程执行失败"));
        if (summary.length() > TmScheduleConstants.MAX_ERROR_MESSAGE_LENGTH) {
            summary = summary.substring(0, TmScheduleConstants.MAX_ERROR_MESSAGE_LENGTH);
        }
        List<TmAutoScheduleIssueVo> issueList = issues == null ? new ArrayList<>() : issues;
        return taskMapper.update(null, new LambdaUpdateWrapper<TmAutoScheduleTask>()
                .eq(TmAutoScheduleTask::getTaskId, taskId)
                .in(TmAutoScheduleTask::getTaskStatus,
                        Arrays.asList(TmAutoScheduleTaskStatusEnum.PENDING.getCode(),
                                TmAutoScheduleTaskStatusEnum.RUNNING.getCode()))
                .set(TmAutoScheduleTask::getTaskStatus, TmAutoScheduleTaskStatusEnum.FAILED.getCode())
                .set(TmAutoScheduleTask::getCurrentStage, TmAutoScheduleTaskStatusEnum.FAILED.getCode())
                .set(TmAutoScheduleTask::getCurrentStageName, this.resolveTmMessage("ui.data.alert.tm.schedule.taskFailed", "执行失败"))
                .set(TmAutoScheduleTask::getErrorMessage, summary)
                .set(TmAutoScheduleTask::getIssueJson, JSON.toJSONString(issueList))
                .set(TmAutoScheduleTask::getEndTime, new Date())
                .set(TmAutoScheduleTask::getLastHeartbeatTime, new Date())) == 1;
    }

    @Override
    public TmAutoScheduleResponseVo toResponse(TmAutoScheduleTask task) {
        if (task == null) {
            return null;
        }
        TmAutoScheduleResponseVo response = StrUtil.isBlank(task.getResultJson())
                ? new TmAutoScheduleResponseVo()
                : JSON.parseObject(task.getResultJson(), TmAutoScheduleResponseVo.class);
        List<TmAutoScheduleIssueVo> issues = StrUtil.isBlank(task.getIssueJson())
                ? new ArrayList<>()
                : JSON.parseArray(task.getIssueJson(), TmAutoScheduleIssueVo.class);
        response.setTaskId(task.getTaskId());
        response.setTaskStatus(task.getTaskStatus());
        response.setProgress(task.getProgress());
        response.setCurrentStage(task.getCurrentStage());
        response.setCurrentStageName(task.getCurrentStageName());
        response.setBatchNo(StrUtil.blankToDefault(response.getBatchNo(), task.getBatchNo()));
        response.setTraceId(StrUtil.blankToDefault(response.getTraceId(), task.getTraceId()));
        response.setIssues(issues);
        response.setIssueCount(issues.size());
        if (TmAutoScheduleTaskStatusEnum.FAILED.getCode().equals(task.getTaskStatus())) {
            response.setSuccess(Boolean.FALSE);
            response.setMessage(task.getErrorMessage());
        }
        return response;
    }
    /**
     * 获取胎面自动排程多语言提示，避免缺失配置时返回空文本。
     *
     * @param key 多语言 key
     * @param fallback 缺省提示
     * @return 可展示提示
     */
    private String resolveTmMessage(String key, String fallback) {
        String message = I18nUtil.getMessage(key);
        return StrUtil.blankToDefault(message, fallback);
    }
}
