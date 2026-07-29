package com.zlt.aps.tm.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleIssueVo;
import com.zlt.aps.tm.api.domain.vo.TmOperationTaskVo;
import com.zlt.aps.tm.api.domain.vo.TmReleaseTaskVo;
import com.zlt.aps.tm.api.enums.TmAutoScheduleTaskStatusEnum;
import com.zlt.aps.tm.api.enums.TmBackgroundTaskTypeEnum;
import com.zlt.aps.tm.domain.TmAutoScheduleTask;
import com.zlt.aps.tm.mapper.TmAutoScheduleTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 胎面人工操作后台任务状态服务。
 *
 * <p>所有状态更新均使用独立短事务，确保人工业务事务提交前后页面都能持续读取真实进度。</p>
 */
@Service
@RequiredArgsConstructor
public class TmOperationTaskService {

    private static final String TASK_ID_PREFIX = "TM-OP-";

    private final TmAutoScheduleTaskMapper taskMapper;

    /**
     * 创建等待执行的人工操作任务。
     *
     * @param taskType 任务类型
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param requestSnapshot 请求快照
     * @param operator 操作人
     * @return 新建任务
     * @throws ServiceException 同工厂日期存在运行任务时抛出
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public TmAutoScheduleTask createPending(String taskType, String factoryCode, Date scheduleDate,
                                            Object requestSnapshot, String operator) {
        TmAutoScheduleTask activeTask = this.findActive(factoryCode, scheduleDate);
        if (activeTask != null) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.operationTaskConcurrent"));
        }
        TmAutoScheduleTask task = new TmAutoScheduleTask();
        task.setTaskId(TASK_ID_PREFIX + IdUtil.fastSimpleUUID().toUpperCase());
        task.setTaskType(taskType);
        task.setFactoryCode(factoryCode);
        task.setScheduleDate(scheduleDate);
        task.setTaskStatus(TmAutoScheduleTaskStatusEnum.PENDING.getCode());
        task.setProgress(0);
        task.setCurrentStage(TmAutoScheduleTaskStatusEnum.PENDING.getCode());
        task.setCurrentStageName(I18nUtil.getMessage("ui.data.alert.tm.schedule.operationTaskPending"));
        task.setRequestSnapshot(JSON.toJSONString(requestSnapshot));
        task.setCreateBy(StrUtil.blankToDefault(operator, "system"));
        if (this.taskMapper.insert(task) != 1) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.operationTaskCreateFailed"));
        }
        return task;
    }

    /**
     * 查询指定任务。
     *
     * @param taskId 任务编号
     * @return 任务，不存在返回null
     */
    public TmAutoScheduleTask findByTaskId(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            return null;
        }
        return this.taskMapper.selectOne(new LambdaQueryWrapper<TmAutoScheduleTask>()
                .eq(TmAutoScheduleTask::getTaskId, taskId));
    }

    /**
     * 查询最近一次人工操作任务。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 最近任务，不存在返回null
     */
    public TmAutoScheduleTask findLatest(String factoryCode, Date scheduleDate) {
        return this.taskMapper.selectOne(new LambdaQueryWrapper<TmAutoScheduleTask>()
                .eq(TmAutoScheduleTask::getFactoryCode, factoryCode)
                .eq(TmAutoScheduleTask::getScheduleDate, scheduleDate)
                .in(TmAutoScheduleTask::getTaskType, TmBackgroundTaskTypeEnum.manualOperationCodes())
                .orderByDesc(TmAutoScheduleTask::getCreateTime)
                .last("limit 1"));
    }

    /**
     * 查询同工厂日期活跃写任务。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 活跃任务
     */
    public TmAutoScheduleTask findActive(String factoryCode, Date scheduleDate) {
        return this.taskMapper.selectOne(new LambdaQueryWrapper<TmAutoScheduleTask>()
                .eq(TmAutoScheduleTask::getFactoryCode, factoryCode)
                .eq(TmAutoScheduleTask::getScheduleDate, scheduleDate)
                .in(TmAutoScheduleTask::getTaskStatus, Arrays.asList(
                        TmAutoScheduleTaskStatusEnum.PENDING.getCode(),
                        TmAutoScheduleTaskStatusEnum.RUNNING.getCode()))
                .orderByDesc(TmAutoScheduleTask::getCreateTime)
                .last("limit 1"));
    }

    /**
     * 启动任务并进入校验加载阶段。
     *
     * @param taskId 任务编号
     * @return 是否启动成功
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean start(String taskId) {
        Date now = new Date();
        return this.taskMapper.update(null, new LambdaUpdateWrapper<TmAutoScheduleTask>()
                .eq(TmAutoScheduleTask::getTaskId, taskId)
                .eq(TmAutoScheduleTask::getTaskStatus, TmAutoScheduleTaskStatusEnum.PENDING.getCode())
                .set(TmAutoScheduleTask::getTaskStatus, TmAutoScheduleTaskStatusEnum.RUNNING.getCode())
                .set(TmAutoScheduleTask::getProgress, 20)
                .set(TmAutoScheduleTask::getCurrentStage, "VALIDATING")
                .set(TmAutoScheduleTask::getCurrentStageName,
                        I18nUtil.getMessage("ui.data.alert.tm.schedule.operationTaskValidating"))
                .set(TmAutoScheduleTask::getStartTime, now)
                .set(TmAutoScheduleTask::getLastHeartbeatTime, now)) == 1;
    }

    /**
     * 更新任务进度。
     *
     * @param taskId 任务编号
     * @param progress 进度
     * @param stage 阶段编码
     * @param stageName 阶段名称
     * @return 是否更新成功
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean updateProgress(String taskId, int progress, String stage, String stageName) {
        return this.taskMapper.update(null, new LambdaUpdateWrapper<TmAutoScheduleTask>()
                .eq(TmAutoScheduleTask::getTaskId, taskId)
                .eq(TmAutoScheduleTask::getTaskStatus, TmAutoScheduleTaskStatusEnum.RUNNING.getCode())
                .le(TmAutoScheduleTask::getProgress, progress)
                .set(TmAutoScheduleTask::getProgress, progress)
                .set(TmAutoScheduleTask::getCurrentStage, stage)
                .set(TmAutoScheduleTask::getCurrentStageName, stageName)
                .set(TmAutoScheduleTask::getLastHeartbeatTime, new Date())) == 1;
    }

    /**
     * 标记任务成功。
     *
     * @param taskId 任务编号
     * @param affectedCount 影响行数
     * @return 是否更新成功
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean markSuccess(String taskId, int affectedCount) {
        JSONObject result = new JSONObject();
        result.put("affectedCount", affectedCount);
        Date now = new Date();
        return this.taskMapper.update(null, new LambdaUpdateWrapper<TmAutoScheduleTask>()
                .eq(TmAutoScheduleTask::getTaskId, taskId)
                .eq(TmAutoScheduleTask::getTaskStatus, TmAutoScheduleTaskStatusEnum.RUNNING.getCode())
                .set(TmAutoScheduleTask::getTaskStatus, TmAutoScheduleTaskStatusEnum.SUCCESS.getCode())
                .set(TmAutoScheduleTask::getProgress, 100)
                .set(TmAutoScheduleTask::getCurrentStage, "SUCCESS")
                .set(TmAutoScheduleTask::getCurrentStageName,
                        I18nUtil.getMessage("ui.data.alert.tm.schedule.operationTaskSuccess"))
                .set(TmAutoScheduleTask::getResultJson, result.toJSONString())
                .set(TmAutoScheduleTask::getEndTime, now)
                .set(TmAutoScheduleTask::getLastHeartbeatTime, now)) == 1;
    }

    /**
     * 标记任务失败。
     *
     * @param taskId 任务编号
     * @param errorMessage 错误摘要
     * @return 是否更新成功
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean markFailed(String taskId, String errorMessage) {
        String normalizedError = StrUtil.blankToDefault(errorMessage,
                I18nUtil.getMessage("ui.data.alert.tm.schedule.operationTaskFailed"));
        if (normalizedError.length() > 1000) {
            normalizedError = normalizedError.substring(0, 1000);
        }
        Date now = new Date();
        return this.taskMapper.update(null, new LambdaUpdateWrapper<TmAutoScheduleTask>()
                .eq(TmAutoScheduleTask::getTaskId, taskId)
                .in(TmAutoScheduleTask::getTaskStatus, Arrays.asList(
                        TmAutoScheduleTaskStatusEnum.PENDING.getCode(),
                        TmAutoScheduleTaskStatusEnum.RUNNING.getCode()))
                .set(TmAutoScheduleTask::getTaskStatus, TmAutoScheduleTaskStatusEnum.FAILED.getCode())
                .set(TmAutoScheduleTask::getCurrentStage, "FAILED")
                .set(TmAutoScheduleTask::getCurrentStageName,
                        I18nUtil.getMessage("ui.data.alert.tm.schedule.operationTaskFailed"))
                .set(TmAutoScheduleTask::getErrorMessage, normalizedError)
                .set(TmAutoScheduleTask::getEndTime, now)
                .set(TmAutoScheduleTask::getLastHeartbeatTime, now)) == 1;
    }

    /**
     * 转换为前端轮询响应。
     *
     * @param task 任务实体
     * @return 任务响应
     */
    public TmOperationTaskVo toResponse(TmAutoScheduleTask task) {
        if (task == null) {
            return null;
        }
        TmOperationTaskVo response = new TmOperationTaskVo();
        response.setTaskId(task.getTaskId());
        response.setTaskType(task.getTaskType());
        response.setTaskStatus(task.getTaskStatus());
        response.setProgress(task.getProgress());
        response.setCurrentStage(task.getCurrentStage());
        response.setCurrentStageName(task.getCurrentStageName());
        response.setMessage(task.getErrorMessage());
        response.setFactoryCode(task.getFactoryCode());
        response.setScheduleDate(task.getScheduleDate());
        if (StrUtil.isNotBlank(task.getResultJson())) {
            response.setAffectedCount(JSON.parseObject(task.getResultJson()).getInteger("affectedCount"));
        }
        return response;
    }

    // ===== 发布任务支持（对齐胎侧 TcBackgroundTaskService）=====

    /**
     * 发布任务启动（带阶段）。
     *
     * @param taskId 任务编号
     * @param stage 阶段编码
     * @param stageName 阶段名称
     * @return 是否启动成功
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean start(String taskId, String stage, String stageName) {
        Date now = new Date();
        return this.taskMapper.update(null, new LambdaUpdateWrapper<TmAutoScheduleTask>()
                .eq(TmAutoScheduleTask::getTaskId, taskId)
                .eq(TmAutoScheduleTask::getTaskStatus, TmAutoScheduleTaskStatusEnum.PENDING.getCode())
                .set(TmAutoScheduleTask::getTaskStatus, TmAutoScheduleTaskStatusEnum.RUNNING.getCode())
                .set(TmAutoScheduleTask::getProgress, 10)
                .set(TmAutoScheduleTask::getCurrentStage, stage)
                .set(TmAutoScheduleTask::getCurrentStageName, stageName)
                .set(TmAutoScheduleTask::getStartTime, now)
                .set(TmAutoScheduleTask::getLastHeartbeatTime, now)) == 1;
    }

    /**
     * 更新发布任务进度（带错误摘要）。
     *
     * @param taskId 任务编号
     * @param progress 进度
     * @param stage 阶段编码
     * @param stageName 阶段名称
     * @param errorMessage 错误摘要，可为空
     * @return 是否更新成功
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean updateProgress(String taskId, int progress, String stage, String stageName, String errorMessage) {
        LambdaUpdateWrapper<TmAutoScheduleTask> wrapper = new LambdaUpdateWrapper<TmAutoScheduleTask>()
                .eq(TmAutoScheduleTask::getTaskId, taskId)
                .eq(TmAutoScheduleTask::getTaskStatus, TmAutoScheduleTaskStatusEnum.RUNNING.getCode())
                .set(TmAutoScheduleTask::getProgress, progress)
                .set(TmAutoScheduleTask::getCurrentStage, stage)
                .set(TmAutoScheduleTask::getCurrentStageName, stageName)
                .set(TmAutoScheduleTask::getLastHeartbeatTime, new Date());
        if (StrUtil.isNotBlank(errorMessage)) {
            wrapper.set(TmAutoScheduleTask::getErrorMessage, this.truncateError(errorMessage));
        }
        return this.taskMapper.update(null, wrapper) == 1;
    }

    /**
     * 标记发布任务成功（带摘要和问题）。
     *
     * @param taskId 任务编号
     * @param result 结果对象
     * @param summary 摘要
     * @param issues 问题列表
     * @return 是否更新成功
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean markSuccess(String taskId, Object result, Map<String, Object> summary, List<?> issues) {
        Date now = new Date();
        return this.taskMapper.update(null, new LambdaUpdateWrapper<TmAutoScheduleTask>()
                .eq(TmAutoScheduleTask::getTaskId, taskId)
                .in(TmAutoScheduleTask::getTaskStatus, Arrays.asList(
                        TmAutoScheduleTaskStatusEnum.RUNNING.getCode(),
                        TmAutoScheduleTaskStatusEnum.SUCCESS.getCode()))
                .set(TmAutoScheduleTask::getTaskStatus, TmAutoScheduleTaskStatusEnum.SUCCESS.getCode())
                .set(TmAutoScheduleTask::getProgress, 100)
                .set(TmAutoScheduleTask::getCurrentStage, "SUCCESS")
                .set(TmAutoScheduleTask::getCurrentStageName, "SUCCESS")
                .set(TmAutoScheduleTask::getResultJson, JSON.toJSONString(result))
                .set(TmAutoScheduleTask::getSummaryJson, JSON.toJSONString(summary))
                .set(TmAutoScheduleTask::getIssueJson, JSON.toJSONString(
                        issues == null ? new ArrayList<>() : issues))
                .set(TmAutoScheduleTask::getEndTime, now)
                .set(TmAutoScheduleTask::getLastHeartbeatTime, now)) == 1;
    }

    /**
     * 标记发布任务失败（带摘要和问题）。
     *
     * @param taskId 任务编号
     * @param errorMessage 错误摘要
     * @param summary 摘要
     * @param issues 问题列表
     * @return 是否更新成功
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean markFailed(String taskId, String errorMessage, Map<String, Object> summary, List<?> issues) {
        Date now = new Date();
        String normalizedError = this.truncateError(errorMessage);
        return this.taskMapper.update(null, new LambdaUpdateWrapper<TmAutoScheduleTask>()
                .eq(TmAutoScheduleTask::getTaskId, taskId)
                .in(TmAutoScheduleTask::getTaskStatus, Arrays.asList(
                        TmAutoScheduleTaskStatusEnum.PENDING.getCode(),
                        TmAutoScheduleTaskStatusEnum.RUNNING.getCode()))
                .set(TmAutoScheduleTask::getTaskStatus, TmAutoScheduleTaskStatusEnum.FAILED.getCode())
                .set(TmAutoScheduleTask::getCurrentStage, "FAILED")
                .set(TmAutoScheduleTask::getCurrentStageName, "FAILED")
                .set(TmAutoScheduleTask::getErrorMessage, normalizedError)
                .set(TmAutoScheduleTask::getErrorSummary, normalizedError)
                .set(TmAutoScheduleTask::getSummaryJson, JSON.toJSONString(summary))
                .set(TmAutoScheduleTask::getIssueJson, JSON.toJSONString(
                        issues == null ? new ArrayList<>() : issues))
                .set(TmAutoScheduleTask::getEndTime, now)
                .set(TmAutoScheduleTask::getLastHeartbeatTime, now)) == 1;
    }

    /**
     * 按MES数据版本查询发布任务。
     *
     * @param mesDataVersion MES数据版本
     * @return 任务，不存在返回null
     */
    public TmAutoScheduleTask findByMesDataVersion(String mesDataVersion) {
        if (StrUtil.isBlank(mesDataVersion)) {
            return null;
        }
        return this.taskMapper.selectOne(new LambdaQueryWrapper<TmAutoScheduleTask>()
                .eq(TmAutoScheduleTask::getMesDataVersion, mesDataVersion)
                .orderByDesc(TmAutoScheduleTask::getCreateTime)
                .last("limit 1"));
    }

    /**
     * 查询指定类型最近一次后台任务。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param taskType 任务类型
     * @return 最近任务，不存在返回null
     */
    public TmAutoScheduleTask findLatest(String factoryCode, Date scheduleDate, String taskType) {
        return this.taskMapper.selectOne(new LambdaQueryWrapper<TmAutoScheduleTask>()
                .eq(TmAutoScheduleTask::getFactoryCode, factoryCode)
                .eq(TmAutoScheduleTask::getScheduleDate, scheduleDate)
                .eq(TmAutoScheduleTask::getTaskType, taskType)
                .orderByDesc(TmAutoScheduleTask::getCreateTime)
                .last("limit 1"));
    }

    /**
     * 转换为发布任务轮询响应。
     *
     * @param task 任务实体
     * @return 发布任务响应
     */
    @SuppressWarnings("unchecked")
    public TmReleaseTaskVo toReleaseTaskVo(TmAutoScheduleTask task) {
        if (task == null) {
            return null;
        }
        TmReleaseTaskVo response = StrUtil.isBlank(task.getResultJson())
                ? new TmReleaseTaskVo() : JSON.parseObject(task.getResultJson(), TmReleaseTaskVo.class);
        response.setTaskId(task.getTaskId());
        response.setTaskStatus(task.getTaskStatus());
        response.setProgress(task.getProgress());
        response.setCurrentStage(task.getCurrentStage());
        response.setCurrentStageName(task.getCurrentStageName());
        response.setTraceId(task.getTraceId());
        response.setDataVersion(task.getMesDataVersion());
        response.setMessage(task.getErrorMessage());
        if (StrUtil.isNotBlank(task.getIssueJson())) {
            response.setIssues(JSON.parseArray(task.getIssueJson(), TmAutoScheduleIssueVo.class));
        }
        if (StrUtil.isNotBlank(task.getSummaryJson())) {
            response.setSummary(JSON.parseObject(task.getSummaryJson(), LinkedHashMap.class));
        }
        response.setSelectedCount(this.toInteger(response.getSummary().get("selectedCount")));
        response.setSuccessCount(this.toInteger(response.getSummary().get("successCount")));
        response.setFailedCount(this.toInteger(response.getSummary().get("failedCount")));
        response.setTimeoutCount(this.toInteger(response.getSummary().get("timeoutCount")));
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
     * 截断错误摘要。
     *
     * @param errorMessage 原错误信息
     * @return 安全错误摘要
     */
    private String truncateError(String errorMessage) {
        String normalizedError = StrUtil.blankToDefault(errorMessage,
                TmAutoScheduleTaskStatusEnum.FAILED.getCode());
        return normalizedError.length() > 1000 ? normalizedError.substring(0, 1000) : normalizedError;
    }
}
