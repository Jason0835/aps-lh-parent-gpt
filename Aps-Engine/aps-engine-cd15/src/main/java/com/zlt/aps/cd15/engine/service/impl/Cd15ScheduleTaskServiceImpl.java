package com.zlt.aps.cd15.engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zlt.aps.cd15.engine.constant.Cd15ScheduleTaskStage;
import com.zlt.aps.cd15.engine.constant.Cd15ScheduleTaskStatus;
import com.zlt.aps.cd15.engine.domain.Cd15ScheduleTask;
import com.zlt.aps.cd15.engine.mapper.Cd15ScheduleTaskMapper;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import java.util.List;

/**
 * 斜裁自动排程任务状态服务实现。
 *
 * <p>每次状态和进度变更均使用独立短事务，不包裹算法全过程。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd15ScheduleTaskServiceImpl implements Cd15ScheduleTaskService {

    private static final int MAX_ERROR_LENGTH = 2000;

    private final Cd15ScheduleTaskMapper taskMapper;

    /**
     * 创建等待执行的斜裁自动排程任务。
     *
     * <p>该方法使用独立事务（REQUIRES_NEW），确保任务记录的插入不受外部事务回滚影响。
     * 创建前会先检查同工厂、同日期是否已有进行中（PENDING 或 RUNNING）的任务：
     * 如果存在活跃任务则直接返回该任务，避免重复创建，实现幂等性。
     *
     * <p>任务状态流转：PENDING → RUNNING（异步执行器领取后）→ SUCCESS / FAILED
     *
     * @param factoryCode     工厂编码，标识排程所属工厂
     * @param scheduleDate    排程日期，即本次自动排程的目标日期
     * @param taskType        任务类型（自动排程或插单滚动重排）
     * @param triggerType     触发类型（MANUAL=手动触发 / TIMER=定时触发），用于区分触发来源
     * @param requestSnapshot 请求快照，记录触发时的关键参数（如"factoryCode=XX,scheduleDate=XX,forceRegenerate=true"），
     *                        便于事后审计排程触发条件
     * @param createBy        创建人标识，手动触发时记录操作人，自动触发时为 null
     * @return 新创建的任务对象；如果同工厂、同日期的活跃任务已存在，则返回已有任务（幂等）
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Cd15ScheduleTask createPending(String factoryCode, Date scheduleDate, String taskType,
                                          String triggerType, String requestSnapshot, String createBy) {
        // --- 1. 幂等性检查：查询同工厂、同日期是否已有进行中的任务 ---
        // findActive 查询条件：factoryCode + scheduleDate + taskStatus IN (PENDING, RUNNING)
        // 如果查询到活跃任务，说明已有任务在处理该日期的排程，直接返回已有任务，防止重复创建
        Cd15ScheduleTask activeTask = findActive(factoryCode, scheduleDate);
        if (activeTask != null) {
            // 幂等返回：已有活跃任务，无需重复创建
            return activeTask;
        }

        // --- 2. 构建新任务对象 ---
        Cd15ScheduleTask task = new Cd15ScheduleTask();

        // 2.1 生成全局唯一任务ID，格式: "CD15-" + 32位大写UUID（无连字符）
        // 示例: CD15-A1B2C3D4E5F6789012345678ABCDEF01
        task.setTaskId("CD15-" + UUID.randomUUID().toString().replace("-", "").toUpperCase());

        // 2.2 工厂编码
        task.setFactoryCode(factoryCode);
        // 2.3 排程日期
        task.setScheduleDate(scheduleDate);
        // 2.4 任务类型（AUTO_SCHEDULE / INSERT_ORDER）
        task.setTaskType(taskType);
        // 2.5 触发类型（MANUAL / TIMER）
        task.setTriggerType(triggerType);

        // 2.6 初始状态：PENDING（等待异步执行器领取）
        // 状态机：PENDING → RUNNING → SUCCESS / FAILED
        task.setTaskStatus(Cd15ScheduleTaskStatus.PENDING);

        // 2.7 初始进度 0%
        task.setProgress(0);
        // 2.8 当前阶段：等待基础数据校验
        task.setCurrentStage(Cd15ScheduleTaskStage.VALIDATE_DATA);
        // 2.9 阶段名称（中文展示）
        task.setCurrentStageName("等待基础数据校验");
        // 2.10 请求快照，用于事后审计
        task.setRequestSnapshot(requestSnapshot);
        // 2.11 创建人
        task.setCreateBy(createBy);

        // --- 3. 持久化：插入 cd15_schedule_task 表 ---
        // 使用独立事务（REQUIRES_NEW），即使外部调用方事务回滚，任务记录也会被保留
        taskMapper.insert(task);

        // --- 4. 记录创建日志 ---
        log.info("[斜裁排程任务] 创建等待执行任务, taskId={}, factoryCode={}, scheduleDate={}, "
                        + "taskType={}, triggerType={}",
                task.getTaskId(), factoryCode, scheduleDate, taskType, triggerType);

        return task;
    }

    /**
     * 根据任务ID查询任务记录。
     *
     * @param taskId 任务唯一标识
     * @return 任务对象，不存在时返回 null
     */
    @Override
    public Cd15ScheduleTask findByTaskId(String taskId) {
        return taskMapper.selectOne(new LambdaQueryWrapper<Cd15ScheduleTask>()
                .eq(Cd15ScheduleTask::getTaskId, taskId));
    }

    /**
     * 查询指定工厂、指定日期的最新任务记录（按创建时间降序取第一条）。
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 排程日期
     * @return 最新任务对象，不存在时返回 null
     */
    @Override
    public Cd15ScheduleTask findLatest(String factoryCode, Date scheduleDate) {
        return taskMapper.selectOne(new LambdaQueryWrapper<Cd15ScheduleTask>()
                .eq(Cd15ScheduleTask::getFactoryCode, factoryCode)
                .eq(Cd15ScheduleTask::getScheduleDate, scheduleDate)
                .orderByDesc(Cd15ScheduleTask::getCreateTime)
                .last("limit 1"));
    }

    /**
     * 查询指定工厂、指定日期是否有进行中的任务（PENDING 或 RUNNING）。
     * <p>用于幂等性检查，防止同一日期重复创建排程任务。</p>
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 排程日期
     * @return 进行中的任务对象，不存在时返回 null
     */
    @Override
    public Cd15ScheduleTask findActive(String factoryCode, Date scheduleDate) {
        return taskMapper.selectOne(new LambdaQueryWrapper<Cd15ScheduleTask>()
                .eq(Cd15ScheduleTask::getFactoryCode, factoryCode)
                .eq(Cd15ScheduleTask::getScheduleDate, scheduleDate)
                .in(Cd15ScheduleTask::getTaskStatus,
                        Arrays.asList(Cd15ScheduleTaskStatus.PENDING, Cd15ScheduleTaskStatus.RUNNING))
                .orderByDesc(Cd15ScheduleTask::getCreateTime)
                .last("limit 1"));
    }

    /**
     * 将任务状态从 PENDING 更新为 RUNNING，标记任务开始执行。
     * <p>使用乐观锁方式：通过 WHERE taskStatus = PENDING 条件确保只有等待中的任务才能被启动，
     * 避免重复领取。更新同时设置初始进度 5% 和基础数据校验阶段。</p>
     *
     * @param taskId 任务唯一标识
     * @return true 表示更新成功（任务被领取），false 表示任务已被其他执行器领取或状态异常
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean start(String taskId) {
        Date now = new Date();
        boolean updated = taskMapper.update(null, new LambdaUpdateWrapper<Cd15ScheduleTask>()
                .eq(Cd15ScheduleTask::getTaskId, taskId)
                .eq(Cd15ScheduleTask::getTaskStatus, Cd15ScheduleTaskStatus.PENDING)
                .set(Cd15ScheduleTask::getTaskStatus, Cd15ScheduleTaskStatus.RUNNING)
                .set(Cd15ScheduleTask::getProgress, 5)
                .set(Cd15ScheduleTask::getCurrentStage, Cd15ScheduleTaskStage.VALIDATE_DATA)
                .set(Cd15ScheduleTask::getCurrentStageName, "基础数据校验")
                .set(Cd15ScheduleTask::getStartTime, now)
                .set(Cd15ScheduleTask::getLastHeartbeatTime, now)) == 1;
        log.info("[斜裁自动排程] 任务启动状态更新, taskId={}, updated={}", taskId, updated);
        return updated;
    }

    /**
     * 更新任务进度和当前阶段。
     * <p>仅在任务状态为 RUNNING 时允许更新，更新同时刷新心跳时间。</p>
     *
     * @param taskId    任务唯一标识
     * @param progress  进度百分比（0~100）
     * @param stage     阶段编码，对应 {@link Cd15ScheduleTaskStage}
     * @param stageName 阶段中文名称
     * @return true 表示更新成功，false 表示任务状态不是 RUNNING 或任务不存在
     * @throws IllegalArgumentException 当 progress 不在 0~100 范围内时抛出
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean updateProgress(String taskId, int progress, String stage, String stageName) {
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("自动排程任务进度必须在0至100之间");
        }
        return taskMapper.update(null, new LambdaUpdateWrapper<Cd15ScheduleTask>()
                .eq(Cd15ScheduleTask::getTaskId, taskId)
                .eq(Cd15ScheduleTask::getTaskStatus, Cd15ScheduleTaskStatus.RUNNING)
                .set(Cd15ScheduleTask::getProgress, progress)
                .set(Cd15ScheduleTask::getCurrentStage, stage)
                .set(Cd15ScheduleTask::getCurrentStageName, stageName)
                .set(Cd15ScheduleTask::getLastHeartbeatTime, new Date())) == 1;
    }

    /**
     * 将任务标记为执行成功（使用独立短事务）。
     * <p>适用于算法执行完毕后，在独立事务中提交成功状态，不受外部事务回滚影响。</p>
     *
     * @param taskId  任务唯一标识
     * @param batchNo 排程生成的批次号
     * @return true 表示更新成功
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean markSuccess(String taskId, String batchNo) {
        return updateSuccess(taskId, batchNo);
    }

    /**
     * 将任务标记为执行成功（使用当前事务）。
     * <p>适用于在调用方事务内同步提交成功状态，与外部事务保持一致。</p>
     *
     * @param taskId  任务唯一标识
     * @param batchNo 排程生成的批次号
     * @return true 表示更新成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markSuccessInCurrentTransaction(String taskId, String batchNo) {
        return updateSuccess(taskId, batchNo);
    }

    /**
     * 更新任务为成功状态的公共方法。
     * <p>仅在任务状态为 RUNNING 时允许更新，设置进度 100%、完成阶段、批次号和结束时间。</p>
     *
     * @param taskId  任务唯一标识
     * @param batchNo 排程生成的批次号
     * @return true 表示更新成功
     */
    private boolean updateSuccess(String taskId, String batchNo) {
        return taskMapper.update(null, new LambdaUpdateWrapper<Cd15ScheduleTask>()
                .eq(Cd15ScheduleTask::getTaskId, taskId)
                .eq(Cd15ScheduleTask::getTaskStatus, Cd15ScheduleTaskStatus.RUNNING)
                .set(Cd15ScheduleTask::getTaskStatus, Cd15ScheduleTaskStatus.SUCCESS)
                .set(Cd15ScheduleTask::getProgress, 100)
                .set(Cd15ScheduleTask::getCurrentStage, Cd15ScheduleTaskStage.COMPLETE)
                .set(Cd15ScheduleTask::getCurrentStageName, "执行完成")
                .set(Cd15ScheduleTask::getBatchNo, batchNo)
                .set(Cd15ScheduleTask::getEndTime, new Date())
                .set(Cd15ScheduleTask::getLastHeartbeatTime, new Date())) == 1;
    }

    /**
     * 将任务标记为执行失败（使用独立短事务）。
     * <p>允许从 PENDING 或 RUNNING 状态转换为 FAILED。错误信息超过 2000 字符时自动截断。</p>
     *
     * @param taskId       任务唯一标识
     * @param errorMessage 失败原因描述
     * @return true 表示更新成功
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean markFailed(String taskId, String errorMessage) {
        String summary = errorMessage == null ? "自动排程执行失败" : errorMessage;
        if (summary.length() > MAX_ERROR_LENGTH) {
            summary = summary.substring(0, MAX_ERROR_LENGTH);
        }
        return taskMapper.update(null, new LambdaUpdateWrapper<Cd15ScheduleTask>()
                .eq(Cd15ScheduleTask::getTaskId, taskId)
                .in(Cd15ScheduleTask::getTaskStatus,
                        Arrays.asList(Cd15ScheduleTaskStatus.PENDING, Cd15ScheduleTaskStatus.RUNNING))
                .set(Cd15ScheduleTask::getTaskStatus, Cd15ScheduleTaskStatus.FAILED)
                .set(Cd15ScheduleTask::getErrorMessage, summary)
                .set(Cd15ScheduleTask::getEndTime, new Date())
                .set(Cd15ScheduleTask::getLastHeartbeatTime, new Date())) == 1;
    }

    /**
     * 查询所有 RUNNING 状态的任务，按心跳时间升序排列。
     * <p>用于心跳超时检测，优先处理长时间未更新心跳的任务。</p>
     *
     * @param limit 最大返回条数，0 或负数时使用默认值 500，上限 1000
     * @return RUNNING 状态的任务列表
     */
    @Override
    public List<Cd15ScheduleTask> findRunningTasks(int limit) {
        int queryLimit = limit <= 0 ? 500 : Math.min(limit, 1000);
        return taskMapper.selectList(new LambdaQueryWrapper<Cd15ScheduleTask>()
                .eq(Cd15ScheduleTask::getTaskStatus, Cd15ScheduleTaskStatus.RUNNING)
                .orderByAsc(Cd15ScheduleTask::getLastHeartbeatTime)
                .last("limit " + queryLimit));
    }

    /**
     * 将心跳超时的任务标记为失败（使用独立短事务）。
     * <p>由心跳超时补偿任务调用，仅处理 RUNNING 状态的任务。错误信息超过 2000 字符时自动截断。</p>
     *
     * @param taskId       任务唯一标识
     * @param errorMessage 超时原因描述
     * @return true 表示更新成功
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean markTimeoutFailed(String taskId, String errorMessage) {
        String summary = errorMessage == null ? "自动排程任务心跳超时" : errorMessage;
        if (summary.length() > MAX_ERROR_LENGTH) {
            summary = summary.substring(0, MAX_ERROR_LENGTH);
        }
        return taskMapper.update(null, new LambdaUpdateWrapper<Cd15ScheduleTask>()
                .eq(Cd15ScheduleTask::getTaskId, taskId)
                .eq(Cd15ScheduleTask::getTaskStatus, Cd15ScheduleTaskStatus.RUNNING)
                .set(Cd15ScheduleTask::getTaskStatus, Cd15ScheduleTaskStatus.FAILED)
                .set(Cd15ScheduleTask::getCurrentStageName, "心跳超时补偿失败")
                .set(Cd15ScheduleTask::getErrorMessage, summary)
                .set(Cd15ScheduleTask::getEndTime, new Date())) == 1;
    }
}
