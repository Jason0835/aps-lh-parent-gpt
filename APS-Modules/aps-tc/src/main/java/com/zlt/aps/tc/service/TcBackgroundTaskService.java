package com.zlt.aps.tc.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.vo.TcAutoScheduleIssueVo;
import com.zlt.aps.tc.api.domain.vo.TcReleaseTaskVo;
import com.zlt.aps.tc.api.enums.TcAutoScheduleTaskStatusEnum;
import com.zlt.aps.tc.domain.TcAutoScheduleTask;
import com.zlt.aps.tc.mapper.TcAutoScheduleTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 胎侧发布和自动滚动后台任务状态服务。
 *
 * <p>自动排程保留原有强类型任务服务，本服务只承接新增后台任务并以独立短事务更新状态。</p>
 */
@Service
@RequiredArgsConstructor
public class TcBackgroundTaskService {

    private final TcAutoScheduleTaskMapper taskMapper;

    /**
     * 按任务ID查询后台任务。
     *
     * @param taskId 任务ID
     * @return 任务，不存在时返回null
     */
    public TcAutoScheduleTask findByTaskId(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            return null;
        }
        return this.taskMapper.selectOne(new LambdaQueryWrapper<TcAutoScheduleTask>()
                .eq(TcAutoScheduleTask::getTaskId, taskId));
    }

    /**
     * 查询同工厂日期当前任意活跃写任务。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 活跃任务，不存在时返回null
     */
    public TcAutoScheduleTask findActive(String factoryCode, Date scheduleDate) {
        return this.taskMapper.selectOne(new LambdaQueryWrapper<TcAutoScheduleTask>()
                .eq(TcAutoScheduleTask::getFactoryCode, factoryCode)
                .eq(TcAutoScheduleTask::getScheduleDate, scheduleDate)
                .in(TcAutoScheduleTask::getTaskStatus, Arrays.asList(
                        TcAutoScheduleTaskStatusEnum.PENDING.getCode(),
                        TcAutoScheduleTaskStatusEnum.RUNNING.getCode()))
                .orderByDesc(TcAutoScheduleTask::getCreateTime)
                .last("limit 1"));
    }

    /**
     * 查询指定类型最近一次后台任务。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param taskType 任务类型
     * @return 最近任务，不存在时返回null
     */
    public TcAutoScheduleTask findLatest(String factoryCode, Date scheduleDate, String taskType) {
        return this.taskMapper.selectOne(new LambdaQueryWrapper<TcAutoScheduleTask>()
                .eq(TcAutoScheduleTask::getFactoryCode, factoryCode)
                .eq(TcAutoScheduleTask::getScheduleDate, scheduleDate)
                .eq(TcAutoScheduleTask::getTaskType, taskType)
                .orderByDesc(TcAutoScheduleTask::getCreateTime)
                .last("limit 1"));
    }

    /**
     * 查询指定MES数据版本任务。
     *
     * @param mesDataVersion MES数据版本
     * @return 任务，不存在时返回null
     */
    public TcAutoScheduleTask findByMesDataVersion(String mesDataVersion) {
        if (StrUtil.isBlank(mesDataVersion)) {
            return null;
        }
        return this.taskMapper.selectOne(new LambdaQueryWrapper<TcAutoScheduleTask>()
                .eq(TcAutoScheduleTask::getMesDataVersion, mesDataVersion)
                .orderByDesc(TcAutoScheduleTask::getCreateTime)
                .last("limit 1"));
    }

    /**
     * 将后台任务从等待执行置为执行中。
     *
     * @param taskId 任务ID
     * @param stage 阶段编码
     * @param stageName 阶段名称
     * @return 是否更新成功
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean start(String taskId, String stage, String stageName) {
        Date now = new Date();
        return this.taskMapper.update(null, new LambdaUpdateWrapper<TcAutoScheduleTask>()
                .eq(TcAutoScheduleTask::getTaskId, taskId)
                .eq(TcAutoScheduleTask::getTaskStatus, TcAutoScheduleTaskStatusEnum.PENDING.getCode())
                .set(TcAutoScheduleTask::getTaskStatus, TcAutoScheduleTaskStatusEnum.RUNNING.getCode())
                .set(TcAutoScheduleTask::getProgress, 10)
                .set(TcAutoScheduleTask::getCurrentStage, stage)
                .set(TcAutoScheduleTask::getCurrentStageName, stageName)
                .set(TcAutoScheduleTask::getStartTime, now)
                .set(TcAutoScheduleTask::getLastHeartbeatTime, now)) == 1;
    }

    /**
     * 更新后台任务进度和阶段。
     *
     * @param taskId 任务ID
     * @param progress 进度
     * @param stage 阶段编码
     * @param stageName 阶段名称
     * @param errorMessage 当前错误摘要，可为空
     * @return 是否更新成功
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean updateProgress(String taskId, int progress, String stage, String stageName, String errorMessage) {
        LambdaUpdateWrapper<TcAutoScheduleTask> wrapper = new LambdaUpdateWrapper<TcAutoScheduleTask>()
                .eq(TcAutoScheduleTask::getTaskId, taskId)
                .eq(TcAutoScheduleTask::getTaskStatus, TcAutoScheduleTaskStatusEnum.RUNNING.getCode())
                .set(TcAutoScheduleTask::getProgress, progress)
                .set(TcAutoScheduleTask::getCurrentStage, stage)
                .set(TcAutoScheduleTask::getCurrentStageName, stageName)
                .set(TcAutoScheduleTask::getLastHeartbeatTime, new Date());
        if (StrUtil.isNotBlank(errorMessage)) {
            wrapper.set(TcAutoScheduleTask::getErrorMessage, this.truncateError(errorMessage));
        }
        return this.taskMapper.update(null, wrapper) == 1;
    }

    /**
     * 标记后台任务执行成功。
     *
     * @param taskId 任务ID
     * @param result 结果对象
     * @param summary 摘要
     * @param issues 问题列表
     * @return 是否更新成功
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean markSuccess(String taskId, Object result, Map<String, Object> summary,
                               List<TcAutoScheduleIssueVo> issues) {
        Date now = new Date();
        return this.taskMapper.update(null, new LambdaUpdateWrapper<TcAutoScheduleTask>()
                .eq(TcAutoScheduleTask::getTaskId, taskId)
                .in(TcAutoScheduleTask::getTaskStatus, Arrays.asList(
                        TcAutoScheduleTaskStatusEnum.RUNNING.getCode(),
                        TcAutoScheduleTaskStatusEnum.SUCCESS.getCode()))
                .set(TcAutoScheduleTask::getTaskStatus, TcAutoScheduleTaskStatusEnum.SUCCESS.getCode())
                .set(TcAutoScheduleTask::getProgress, 100)
                .set(TcAutoScheduleTask::getCurrentStage, TcScheduleConstants.AUTO_SCHEDULE_STAGE_COMPLETE)
                .set(TcAutoScheduleTask::getCurrentStageName, TcScheduleConstants.AUTO_SCHEDULE_STAGE_COMPLETE)
                .set(TcAutoScheduleTask::getResultJson, JSON.toJSONString(result))
                .set(TcAutoScheduleTask::getSummaryJson, JSON.toJSONString(summary))
                .set(TcAutoScheduleTask::getIssueJson, JSON.toJSONString(
                        issues == null ? new ArrayList<>() : issues))
                .set(TcAutoScheduleTask::getEndTime, now)
                .set(TcAutoScheduleTask::getLastHeartbeatTime, now)) == 1;
    }

    /**
     * 标记后台任务失败。
     *
     * @param taskId 任务ID
     * @param errorMessage 错误摘要
     * @param summary 摘要
     * @param issues 问题列表
     * @return 是否更新成功
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean markFailed(String taskId, String errorMessage, Map<String, Object> summary,
                              List<TcAutoScheduleIssueVo> issues) {
        Date now = new Date();
        String normalizedError = this.truncateError(errorMessage);
        return this.taskMapper.update(null, new LambdaUpdateWrapper<TcAutoScheduleTask>()
                .eq(TcAutoScheduleTask::getTaskId, taskId)
                .in(TcAutoScheduleTask::getTaskStatus, Arrays.asList(
                        TcAutoScheduleTaskStatusEnum.PENDING.getCode(),
                        TcAutoScheduleTaskStatusEnum.RUNNING.getCode()))
                .set(TcAutoScheduleTask::getTaskStatus, TcAutoScheduleTaskStatusEnum.FAILED.getCode())
                .set(TcAutoScheduleTask::getCurrentStage, TcAutoScheduleTaskStatusEnum.FAILED.getCode())
                .set(TcAutoScheduleTask::getCurrentStageName, TcAutoScheduleTaskStatusEnum.FAILED.getCode())
                .set(TcAutoScheduleTask::getErrorMessage, normalizedError)
                .set(TcAutoScheduleTask::getErrorSummary, normalizedError)
                .set(TcAutoScheduleTask::getSummaryJson, JSON.toJSONString(summary))
                .set(TcAutoScheduleTask::getIssueJson, JSON.toJSONString(
                        issues == null ? new ArrayList<>() : issues))
                .set(TcAutoScheduleTask::getEndTime, now)
                .set(TcAutoScheduleTask::getLastHeartbeatTime, now)) == 1;
    }

    /**
     * 将任务转换为发布轮询响应。
     *
     * @param task 后台任务
     * @return 发布任务响应
     */
    @SuppressWarnings("unchecked")
    public TcReleaseTaskVo toReleaseTaskVo(TcAutoScheduleTask task) {
        if (task == null) {
            return null;
        }
        TcReleaseTaskVo response = StrUtil.isBlank(task.getResultJson())
                ? new TcReleaseTaskVo() : JSON.parseObject(task.getResultJson(), TcReleaseTaskVo.class);
        response.setTaskId(task.getTaskId());
        response.setTaskStatus(task.getTaskStatus());
        response.setProgress(task.getProgress());
        response.setCurrentStage(task.getCurrentStage());
        response.setCurrentStageName(task.getCurrentStageName());
        response.setTraceId(task.getTraceId());
        response.setDataVersion(task.getMesDataVersion());
        response.setMessage(task.getErrorMessage());
        if (StrUtil.isNotBlank(task.getIssueJson())) {
            response.setIssues(JSON.parseArray(task.getIssueJson(), TcAutoScheduleIssueVo.class));
        }
        if (StrUtil.isNotBlank(task.getSummaryJson())) {
            response.setSummary(JSON.parseObject(task.getSummaryJson(), LinkedHashMap.class));
        }
        Object selectedCount = response.getSummary().get("selectedCount");
        Object successCount = response.getSummary().get("successCount");
        Object failedCount = response.getSummary().get("failedCount");
        Object timeoutCount = response.getSummary().get("timeoutCount");
        response.setSelectedCount(this.toInteger(selectedCount));
        response.setSuccessCount(this.toInteger(successCount));
        response.setFailedCount(this.toInteger(failedCount));
        response.setTimeoutCount(this.toInteger(timeoutCount));
        return response;
    }

    /**
     * 将摘要数值转换为整数。
     *
     * @param value 摘要值
     * @return 整数，空值返回0
     */
    private Integer toInteger(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    /**
     * 截断数据库错误摘要。
     *
     * @param errorMessage 原错误信息
     * @return 安全错误摘要
     */
    private String truncateError(String errorMessage) {
        String normalizedError = StrUtil.blankToDefault(errorMessage, TcAutoScheduleTaskStatusEnum.FAILED.getCode());
        return normalizedError.length() > TcScheduleConstants.MAX_ERROR_MESSAGE_LENGTH
                ? normalizedError.substring(0, TcScheduleConstants.MAX_ERROR_MESSAGE_LENGTH) : normalizedError;
    }
}
