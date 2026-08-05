package com.zlt.aps.gsq.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.gsq.constant.GsqScheduleConstants;
import com.zlt.aps.gsq.domain.GsqAutoScheduleTask;
import com.zlt.aps.gsq.mapper.GsqAutoScheduleTaskMapper;
import com.zlt.aps.gsq.enums.GsqAutoScheduleTaskStatusEnum;
import com.zlt.aps.gsq.enums.GsqBackgroundTaskTypeEnum;
import com.zlt.aps.gsq.api.domain.vo.GsqOperationTaskVo;
import com.zlt.aps.gsq.api.domain.vo.GsqRollingTaskVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 钢丝圈自动滚动与人工操作后台任务状态服务。
 *
 * <p>负责维护 {@link GsqAutoScheduleTask} 任务记录的状态机：
 * 等待执行 -> 执行中 -> 成功/失败。所有状态更新使用独立短事务，避免被外部大事务回滚。</p>
 *
 * <p>对齐胎侧 {@code TcBackgroundTaskService}，同时支持：</p>
 * <ul>
 *   <li>自动滚动：{@link #create(GsqAutoScheduleTask)}、{@link #start(String, String, String)}、
 *       {@link #markSuccess(String, Object, Map, List)}、{@link #markFailed(String, String, Map, List)}；</li>
 *   <li>人工操作：{@link #createOperationPending(String, String, Date, Object, String)}、
 *       {@link #startOperation(String)}、{@link #markOperationSuccess(String, int)}、
 *       {@link #markOperationFailed(String, String)}、{@link #findLatestOperation(String, Date)}、
 *       {@link #toOperationTaskVo(GsqAutoScheduleTask)}。</li>
 * </ul>
 *
 * @author APS
 */
@Service
@RequiredArgsConstructor
public class GsqBackgroundTaskService {

    private final GsqAutoScheduleTaskMapper taskMapper;

    /**
     * 创建等待执行的人工操作任务。
     *
     * <p>对齐胎侧 {@code TcBackgroundTaskService.createOperationPending}，用于4类人工操作
     * （插单/调量/转机台/删除）异步任务入口。请求快照序列化为 JSON 存入
     * {@link GsqAutoScheduleTask#getRequestSnapshot()}，由异步执行器反序列化后执行。</p>
     *
     * @param taskType        任务类型编码，见 {@link GsqBackgroundTaskTypeEnum}
     * @param factoryCode     工厂编码
     * @param scheduleDate    排程日期
     * @param requestSnapshot 请求快照对象（将被 JSON 序列化）
     * @param operator        操作人，空时填充为 system
     * @return 新建任务
     * @throws ServiceException 已有同工厂日期活跃任务或入库失败时抛出
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public GsqAutoScheduleTask createOperationPending(String taskType, String factoryCode, Date scheduleDate,
                                                     Object requestSnapshot, String operator) {
        if (this.findActive(factoryCode, scheduleDate) != null) {
            throw new ServiceException(I18nUtil.getMessage("ui.gsq.schedule.concurrentTask"));
        }
        GsqAutoScheduleTask task = new GsqAutoScheduleTask();
        task.setTaskId(GsqScheduleConstants.OPERATION_TASK_ID_PREFIX + IdUtil.fastSimpleUUID().toUpperCase());
        task.setTaskType(taskType);
        task.setFactoryCode(factoryCode);
        task.setScheduleDate(scheduleDate);
        task.setTaskStatus(GsqAutoScheduleTaskStatusEnum.PENDING.getCode());
        task.setProgress(0);
        task.setCurrentStage(GsqAutoScheduleTaskStatusEnum.PENDING.getCode());
        task.setCurrentStageName(I18nUtil.getMessage("ui.gsq.schedule.operationTaskPending"));
        task.setRequestSnapshot(JSON.toJSONString(requestSnapshot));
        task.setCreateBy(StrUtil.blankToDefault(operator, "system"));
        if (this.taskMapper.insert(task) != 1) {
            throw new ServiceException(I18nUtil.getMessage("ui.gsq.schedule.operationTaskCreateFailed"));
        }
        return task;
    }

    /**
     * 启动人工操作任务（PENDING -> RUNNING）。
     *
     * <p>对齐胎侧 {@code TcBackgroundTaskService.startOperation}，使用 VALIDATING 阶段作为初始阶段，
     * 与自动滚动的 CALCULATING 阶段区分。</p>
     *
     * @param taskId 任务编号
     * @return 是否启动成功；任务已不在 PENDING 状态时返回 false
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean startOperation(String taskId) {
        Date now = new Date();
        return this.taskMapper.update(null, new LambdaUpdateWrapper<GsqAutoScheduleTask>()
                .eq(GsqAutoScheduleTask::getTaskId, taskId)
                .eq(GsqAutoScheduleTask::getTaskStatus, GsqAutoScheduleTaskStatusEnum.PENDING.getCode())
                .set(GsqAutoScheduleTask::getTaskStatus, GsqAutoScheduleTaskStatusEnum.RUNNING.getCode())
                .set(GsqAutoScheduleTask::getProgress, 20)
                .set(GsqAutoScheduleTask::getCurrentStage, "VALIDATING")
                .set(GsqAutoScheduleTask::getCurrentStageName,
                        I18nUtil.getMessage("ui.gsq.schedule.operationTaskValidating"))
                .set(GsqAutoScheduleTask::getStartTime, now)
                .set(GsqAutoScheduleTask::getLastHeartbeatTime, now)) == 1;
    }

    /**
     * 标记人工操作任务成功。
     *
     * <p>对齐胎侧 {@code TcBackgroundTaskService.markOperationSuccess}，将受影响行数写入
     * {@code resultJson} 的 {@code affectedCount} 字段，供前端轮询读取。</p>
     *
     * @param taskId        任务编号
     * @param affectedCount 受影响行数
     * @return 是否更新成功；任务已不在 RUNNING 状态时返回 false
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean markOperationSuccess(String taskId, int affectedCount) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("affectedCount", affectedCount);
        Date now = new Date();
        return this.taskMapper.update(null, new LambdaUpdateWrapper<GsqAutoScheduleTask>()
                .eq(GsqAutoScheduleTask::getTaskId, taskId)
                .eq(GsqAutoScheduleTask::getTaskStatus, GsqAutoScheduleTaskStatusEnum.RUNNING.getCode())
                .set(GsqAutoScheduleTask::getTaskStatus, GsqAutoScheduleTaskStatusEnum.SUCCESS.getCode())
                .set(GsqAutoScheduleTask::getProgress, 100)
                .set(GsqAutoScheduleTask::getCurrentStage, "SUCCESS")
                .set(GsqAutoScheduleTask::getCurrentStageName,
                        I18nUtil.getMessage("ui.gsq.schedule.operationTaskSuccess"))
                .set(GsqAutoScheduleTask::getResultJson, JSON.toJSONString(result))
                .set(GsqAutoScheduleTask::getEndTime, now)
                .set(GsqAutoScheduleTask::getLastHeartbeatTime, now)) == 1;
    }

    /**
     * 标记人工操作任务失败。
     *
     * <p>对齐胎侧 {@code TcBackgroundTaskService.markOperationFailed}，允许 PENDING/RUNNING 状态迁移到 FAILED，
     * 并通过 {@link #truncateError(String)} 截断错误信息后写入数据库。</p>
     *
     * @param taskId       任务编号
     * @param errorMessage 错误摘要
     * @return 是否更新成功
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean markOperationFailed(String taskId, String errorMessage) {
        Date now = new Date();
        String normalizedError = this.truncateError(errorMessage);
        return this.taskMapper.update(null, new LambdaUpdateWrapper<GsqAutoScheduleTask>()
                .eq(GsqAutoScheduleTask::getTaskId, taskId)
                .in(GsqAutoScheduleTask::getTaskStatus, Arrays.asList(
                        GsqAutoScheduleTaskStatusEnum.PENDING.getCode(),
                        GsqAutoScheduleTaskStatusEnum.RUNNING.getCode()))
                .set(GsqAutoScheduleTask::getTaskStatus, GsqAutoScheduleTaskStatusEnum.FAILED.getCode())
                .set(GsqAutoScheduleTask::getCurrentStage, "FAILED")
                .set(GsqAutoScheduleTask::getCurrentStageName,
                        I18nUtil.getMessage("ui.gsq.schedule.operationTaskFailed"))
                .set(GsqAutoScheduleTask::getErrorMessage, normalizedError)
                .set(GsqAutoScheduleTask::getErrorSummary, normalizedError)
                .set(GsqAutoScheduleTask::getEndTime, now)
                .set(GsqAutoScheduleTask::getLastHeartbeatTime, now)) == 1;
    }

    /**
     * 查询最近一次人工操作任务。
     *
     * <p>对齐胎侧 {@code TcBackgroundTaskService.findLatestOperation}，按 taskType IN
     * {@link GsqBackgroundTaskTypeEnum#manualOperationCodes()} 过滤，用于前端"最近任务"轮询查询。</p>
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 排程日期
     * @return 最近任务，不存在返回 null
     */
    public GsqAutoScheduleTask findLatestOperation(String factoryCode, Date scheduleDate) {
        return this.taskMapper.selectOne(new LambdaQueryWrapper<GsqAutoScheduleTask>()
                .eq(GsqAutoScheduleTask::getFactoryCode, factoryCode)
                .eq(GsqAutoScheduleTask::getScheduleDate, scheduleDate)
                .in(GsqAutoScheduleTask::getTaskType, GsqBackgroundTaskTypeEnum.manualOperationCodes())
                .orderByDesc(GsqAutoScheduleTask::getCreateTime)
                .last("limit 1"));
    }

    /**
     * 将任务转换为人工操作轮询响应。
     *
     * <p>对齐胎侧 {@code TcBackgroundTaskService.toOperationTaskVo}，从 {@code resultJson} 解析
     * {@code affectedCount} 供前端展示。</p>
     *
     * @param task 任务实体
     * @return 人工操作任务响应；入参为 null 时返回 null
     */
    public GsqOperationTaskVo toOperationTaskVo(GsqAutoScheduleTask task) {
        if (task == null) {
            return null;
        }
        GsqOperationTaskVo response = new GsqOperationTaskVo();
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

    /**
     * 按任务ID查询后台任务。
     *
     * @param taskId 任务ID
     * @return 任务，不存在时返回null
     */
    public GsqAutoScheduleTask findByTaskId(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            return null;
        }
        return this.taskMapper.selectOne(new LambdaQueryWrapper<GsqAutoScheduleTask>()
                .eq(GsqAutoScheduleTask::getTaskId, taskId));
    }

    /**
     * 查询同工厂日期当前任意活跃任务。
     *
     * @param factoryCode   工厂编码
     * @param scheduleDate  排程日期
     * @return 活跃任务，不存在时返回null
     */
    public GsqAutoScheduleTask findActive(String factoryCode, Date scheduleDate) {
        return this.taskMapper.selectOne(new LambdaQueryWrapper<GsqAutoScheduleTask>()
                .eq(GsqAutoScheduleTask::getFactoryCode, factoryCode)
                .eq(GsqAutoScheduleTask::getScheduleDate, scheduleDate)
                .in(GsqAutoScheduleTask::getTaskStatus, Arrays.asList(
                        GsqAutoScheduleTaskStatusEnum.PENDING.getCode(),
                        GsqAutoScheduleTaskStatusEnum.RUNNING.getCode()))
                .orderByDesc(GsqAutoScheduleTask::getCreateTime)
                .last("limit 1"));
    }

    /**
     * 查询指定类型最近一次后台任务。
     *
     * @param factoryCode   工厂编码
     * @param scheduleDate  排程日期
     * @param taskType      任务类型
     * @return 最近任务，不存在时返回null
     */
    public GsqAutoScheduleTask findLatest(String factoryCode, Date scheduleDate, String taskType) {
        return this.taskMapper.selectOne(new LambdaQueryWrapper<GsqAutoScheduleTask>()
                .eq(GsqAutoScheduleTask::getFactoryCode, factoryCode)
                .eq(GsqAutoScheduleTask::getScheduleDate, scheduleDate)
                .eq(GsqAutoScheduleTask::getTaskType, taskType)
                .orderByDesc(GsqAutoScheduleTask::getCreateTime)
                .last("limit 1"));
    }

    /**
     * 按幂等键查询最近一次任务。
     *
     * @param idempotencyKey 输入幂等键
     * @return 最近任务，不存在时返回null
     */
    public GsqAutoScheduleTask findByIdempotencyKey(String idempotencyKey) {
        if (StrUtil.isBlank(idempotencyKey)) {
            return null;
        }
        return this.taskMapper.selectOne(new LambdaQueryWrapper<GsqAutoScheduleTask>()
                .eq(GsqAutoScheduleTask::getIdempotencyKey, idempotencyKey)
                .orderByDesc(GsqAutoScheduleTask::getCreateTime)
                .last("limit 1"));
    }

    /**
     * 创建新的后台任务记录（状态为等待执行）。
     *
     * @param task 任务记录
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void create(GsqAutoScheduleTask task) {
        if (StrUtil.isBlank(task.getTaskStatus())) {
            task.setTaskStatus(GsqAutoScheduleTaskStatusEnum.PENDING.getCode());
        }
        if (task.getProgress() == null) {
            task.setProgress(0);
        }
        Date now = new Date();
        if (task.getCreateTime() == null) {
            task.setCreateTime(now);
        }
        task.setLastHeartbeatTime(now);
        this.taskMapper.insert(task);
    }

    /**
     * 将后台任务从等待执行置为执行中。
     *
     * @param taskId    任务ID
     * @param stage     阶段编码
     * @param stageName 阶段名称
     * @return 是否更新成功
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean start(String taskId, String stage, String stageName) {
        Date now = new Date();
        return this.taskMapper.update(null, new LambdaUpdateWrapper<GsqAutoScheduleTask>()
                .eq(GsqAutoScheduleTask::getTaskId, taskId)
                .eq(GsqAutoScheduleTask::getTaskStatus, GsqAutoScheduleTaskStatusEnum.PENDING.getCode())
                .set(GsqAutoScheduleTask::getTaskStatus, GsqAutoScheduleTaskStatusEnum.RUNNING.getCode())
                .set(GsqAutoScheduleTask::getProgress, 10)
                .set(GsqAutoScheduleTask::getCurrentStage, stage)
                .set(GsqAutoScheduleTask::getCurrentStageName, stageName)
                .set(GsqAutoScheduleTask::getStartTime, now)
                .set(GsqAutoScheduleTask::getLastHeartbeatTime, now)) == 1;
    }

    /**
     * 更新后台任务进度和阶段。
     *
     * @param taskId       任务ID
     * @param progress     进度
     * @param stage        阶段编码
     * @param stageName    阶段名称
     * @param errorMessage 当前错误摘要，可为空
     * @return 是否更新成功
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean updateProgress(String taskId, int progress, String stage, String stageName, String errorMessage) {
        LambdaUpdateWrapper<GsqAutoScheduleTask> wrapper = new LambdaUpdateWrapper<GsqAutoScheduleTask>()
                .eq(GsqAutoScheduleTask::getTaskId, taskId)
                .eq(GsqAutoScheduleTask::getTaskStatus, GsqAutoScheduleTaskStatusEnum.RUNNING.getCode())
                .set(GsqAutoScheduleTask::getProgress, progress)
                .set(GsqAutoScheduleTask::getCurrentStage, stage)
                .set(GsqAutoScheduleTask::getCurrentStageName, stageName)
                .set(GsqAutoScheduleTask::getLastHeartbeatTime, new Date());
        if (StrUtil.isNotBlank(errorMessage)) {
            wrapper.set(GsqAutoScheduleTask::getErrorMessage, this.truncateError(errorMessage));
        }
        return this.taskMapper.update(null, wrapper) == 1;
    }

    /**
     * 更新心跳时间，用于存活探测。
     *
     * @param taskId 任务ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void heartbeat(String taskId) {
        this.taskMapper.update(null, new LambdaUpdateWrapper<GsqAutoScheduleTask>()
                .eq(GsqAutoScheduleTask::getTaskId, taskId)
                .eq(GsqAutoScheduleTask::getTaskStatus, GsqAutoScheduleTaskStatusEnum.RUNNING.getCode())
                .set(GsqAutoScheduleTask::getLastHeartbeatTime, new Date()));
    }

    /**
     * 标记后台任务执行成功。
     *
     * @param taskId  任务ID
     * @param result  结果对象
     * @param summary 摘要
     * @param issues  问题列表
     * @return 是否更新成功
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean markSuccess(String taskId, Object result, Map<String, Object> summary, List<String> issues) {
        Date now = new Date();
        return this.taskMapper.update(null, new LambdaUpdateWrapper<GsqAutoScheduleTask>()
                .eq(GsqAutoScheduleTask::getTaskId, taskId)
                .in(GsqAutoScheduleTask::getTaskStatus, Arrays.asList(
                        GsqAutoScheduleTaskStatusEnum.RUNNING.getCode(),
                        GsqAutoScheduleTaskStatusEnum.SUCCESS.getCode()))
                .set(GsqAutoScheduleTask::getTaskStatus, GsqAutoScheduleTaskStatusEnum.SUCCESS.getCode())
                .set(GsqAutoScheduleTask::getProgress, 100)
                .set(GsqAutoScheduleTask::getCurrentStage, GsqScheduleConstants.ROLLING_STAGE_COMPLETE)
                .set(GsqAutoScheduleTask::getCurrentStageName, GsqScheduleConstants.ROLLING_STAGE_COMPLETE)
                .set(GsqAutoScheduleTask::getResultJson, JSON.toJSONString(result))
                .set(GsqAutoScheduleTask::getSummaryJson, JSON.toJSONString(summary))
                .set(GsqAutoScheduleTask::getIssueJson, JSON.toJSONString(
                        issues == null ? new ArrayList<>() : issues))
                .set(GsqAutoScheduleTask::getEndTime, now)
                .set(GsqAutoScheduleTask::getLastHeartbeatTime, now)) == 1;
    }

    /**
     * 标记后台任务失败。
     *
     * @param taskId       任务ID
     * @param errorMessage 错误摘要
     * @param summary      摘要
     * @param issues       问题列表
     * @return 是否更新成功
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean markFailed(String taskId, String errorMessage, Map<String, Object> summary, List<String> issues) {
        Date now = new Date();
        String normalizedError = this.truncateError(errorMessage);
        return this.taskMapper.update(null, new LambdaUpdateWrapper<GsqAutoScheduleTask>()
                .eq(GsqAutoScheduleTask::getTaskId, taskId)
                .in(GsqAutoScheduleTask::getTaskStatus, Arrays.asList(
                        GsqAutoScheduleTaskStatusEnum.PENDING.getCode(),
                        GsqAutoScheduleTaskStatusEnum.RUNNING.getCode()))
                .set(GsqAutoScheduleTask::getTaskStatus, GsqAutoScheduleTaskStatusEnum.FAILED.getCode())
                .set(GsqAutoScheduleTask::getCurrentStage, GsqAutoScheduleTaskStatusEnum.FAILED.getCode())
                .set(GsqAutoScheduleTask::getCurrentStageName, GsqAutoScheduleTaskStatusEnum.FAILED.getCode())
                .set(GsqAutoScheduleTask::getErrorMessage, normalizedError)
                .set(GsqAutoScheduleTask::getErrorSummary, normalizedError)
                .set(GsqAutoScheduleTask::getSummaryJson, JSON.toJSONString(summary))
                .set(GsqAutoScheduleTask::getIssueJson, JSON.toJSONString(
                        issues == null ? new ArrayList<>() : issues))
                .set(GsqAutoScheduleTask::getEndTime, now)
                .set(GsqAutoScheduleTask::getLastHeartbeatTime, now)) == 1;
    }

    /**
     * 将任务转换为滚动任务响应。
     *
     * @param task 后台任务
     * @return 滚动任务响应
     */
    @SuppressWarnings("unchecked")
    public GsqRollingTaskVo toRollingTaskVo(GsqAutoScheduleTask task) {
        if (task == null) {
            return null;
        }
        GsqRollingTaskVo response = StrUtil.isBlank(task.getResultJson())
                ? new GsqRollingTaskVo() : JSON.parseObject(task.getResultJson(), GsqRollingTaskVo.class);
        response.setTaskId(task.getTaskId());
        response.setTaskStatus(task.getTaskStatus());
        response.setProgress(task.getProgress());
        response.setCurrentStage(task.getCurrentStage());
        response.setCurrentStageName(task.getCurrentStageName());
        response.setFactoryCode(task.getFactoryCode());
        response.setTargetShiftOrder(task.getTargetShiftOrder());
        response.setBatchNo(task.getBatchNo());
        response.setInputVersion(task.getInputVersion());
        if (StrUtil.isNotBlank(task.getIssueJson())) {
            response.setIssues(JSON.parseArray(task.getIssueJson(), String.class));
        }
        if (StrUtil.isNotBlank(task.getSummaryJson())) {
            response.setSummary(JSON.parseObject(task.getSummaryJson(), LinkedHashMap.class));
        }
        return response;
    }

    /**
     * 截断数据库错误摘要。
     *
     * @param errorMessage 原错误信息
     * @return 安全错误摘要
     */
    private String truncateError(String errorMessage) {
        String normalizedError = StrUtil.blankToDefault(errorMessage, GsqAutoScheduleTaskStatusEnum.FAILED.getCode());
        return normalizedError.length() > GsqScheduleConstants.MAX_ERROR_MESSAGE_LENGTH
                ? normalizedError.substring(0, GsqScheduleConstants.MAX_ERROR_MESSAGE_LENGTH) : normalizedError;
    }
}
