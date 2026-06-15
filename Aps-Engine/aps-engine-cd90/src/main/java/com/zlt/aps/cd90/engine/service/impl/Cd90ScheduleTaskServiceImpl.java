package com.zlt.aps.cd90.engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zlt.aps.cd90.engine.constant.Cd90ScheduleTaskStage;
import com.zlt.aps.cd90.engine.constant.Cd90ScheduleTaskStatus;
import com.zlt.aps.cd90.engine.domain.Cd90ScheduleTask;
import com.zlt.aps.cd90.engine.mapper.Cd90ScheduleTaskMapper;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleTaskService;
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
 * 直裁自动排程任务状态服务实现。
 *
 * <p>每次状态和进度变更均使用独立短事务，不包裹算法全过程。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd90ScheduleTaskServiceImpl implements Cd90ScheduleTaskService {

    private static final int MAX_ERROR_LENGTH = 2000;

    private final Cd90ScheduleTaskMapper taskMapper;

    /**
     * 创建等待执行的直裁自动排程任务。
     *
     * <p>该方法使用独立事务（REQUIRES_NEW），确保任务记录的插入不受外部事务回滚影响。
     * 创建前会先检查同工厂、同日期是否已有进行中（PENDING 或 RUNNING）的任务：
     * 如果存在活跃任务则直接返回该任务，避免重复创建，实现幂等性。
     *
     * <p>任务状态流转：PENDING → RUNNING（异步执行器领取后）→ SUCCESS / FAILED
     *
     * @param factoryCode     工厂编码，标识排程所属工厂
     * @param scheduleDate    排程日期，即本次自动排程的目标日期
     * @param triggerType     触发类型（MANUAL=手动触发 / AUTO=系统自动触发），用于区分排程来源
     * @param requestSnapshot 请求快照，记录触发时的关键参数（如"factoryCode=XX,scheduleDate=XX,forceRegenerate=true"），
     *                        便于事后审计排程触发条件
     * @param createBy        创建人标识，手动触发时记录操作人，自动触发时为 null
     * @return 新创建的任务对象；如果同工厂、同日期的活跃任务已存在，则返回已有任务（幂等）
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Cd90ScheduleTask createPending(String factoryCode, Date scheduleDate, String triggerType,
                                          String requestSnapshot, String createBy) {
        // --- 1. 幂等性检查：查询同工厂、同日期是否已有进行中的任务 ---
        // findActive 查询条件：factoryCode + scheduleDate + taskStatus IN (PENDING, RUNNING)
        // 如果查询到活跃任务，说明已有任务在处理该日期的排程，直接返回已有任务，防止重复创建
        Cd90ScheduleTask activeTask = findActive(factoryCode, scheduleDate);
        if (activeTask != null) {
            // 幂等返回：已有活跃任务，无需重复创建
            return activeTask;
        }

        // --- 2. 构建新任务对象 ---
        Cd90ScheduleTask task = new Cd90ScheduleTask();

        // 2.1 生成全局唯一任务ID，格式: "CD90-" + 32位大写UUID（无连字符）
        // 示例: CD90-A1B2C3D4E5F6789012345678ABCDEF01
        task.setTaskId("CD90-" + UUID.randomUUID().toString().replace("-", "").toUpperCase());

        // 2.2 工厂编码
        task.setFactoryCode(factoryCode);
        // 2.3 排程日期
        task.setScheduleDate(scheduleDate);
        // 2.4 触发类型（MANUAL / AUTO）
        task.setTriggerType(triggerType);

        // 2.5 初始状态：PENDING（等待异步执行器领取）
        // 状态机：PENDING → RUNNING → SUCCESS / FAILED
        task.setTaskStatus(Cd90ScheduleTaskStatus.PENDING);

        // 2.6 初始进度 0%
        task.setProgress(0);
        // 2.7 当前阶段：等待基础数据校验
        task.setCurrentStage(Cd90ScheduleTaskStage.VALIDATE_DATA);
        // 2.8 阶段名称（中文展示）
        task.setCurrentStageName("等待基础数据校验");
        // 2.9 请求快照，用于事后审计
        task.setRequestSnapshot(requestSnapshot);
        // 2.10 创建人
        task.setCreateBy(createBy);

        // --- 3. 持久化：插入 cd90_schedule_task 表 ---
        // 使用独立事务（REQUIRES_NEW），即使外部调用方事务回滚，任务记录也会被保留
        taskMapper.insert(task);

        // --- 4. 记录创建日志 ---
        log.info("[直裁自动排程] 创建等待执行任务, taskId={}, factoryCode={}, scheduleDate={}, triggerType={}",
                task.getTaskId(), factoryCode, scheduleDate, triggerType);

        return task;
    }

    @Override
    public Cd90ScheduleTask findByTaskId(String taskId) {
        return taskMapper.selectOne(new LambdaQueryWrapper<Cd90ScheduleTask>()
                .eq(Cd90ScheduleTask::getTaskId, taskId));
    }

    @Override
    public Cd90ScheduleTask findLatest(String factoryCode, Date scheduleDate) {
        return taskMapper.selectOne(new LambdaQueryWrapper<Cd90ScheduleTask>()
                .eq(Cd90ScheduleTask::getFactoryCode, factoryCode)
                .eq(Cd90ScheduleTask::getScheduleDate, scheduleDate)
                .orderByDesc(Cd90ScheduleTask::getCreateTime)
                .last("limit 1"));
    }

    @Override
    public Cd90ScheduleTask findActive(String factoryCode, Date scheduleDate) {
        return taskMapper.selectOne(new LambdaQueryWrapper<Cd90ScheduleTask>()
                .eq(Cd90ScheduleTask::getFactoryCode, factoryCode)
                .eq(Cd90ScheduleTask::getScheduleDate, scheduleDate)
                .in(Cd90ScheduleTask::getTaskStatus,
                        Arrays.asList(Cd90ScheduleTaskStatus.PENDING, Cd90ScheduleTaskStatus.RUNNING))
                .orderByDesc(Cd90ScheduleTask::getCreateTime)
                .last("limit 1"));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean start(String taskId) {
        Date now = new Date();
        boolean updated = taskMapper.update(null, new LambdaUpdateWrapper<Cd90ScheduleTask>()
                .eq(Cd90ScheduleTask::getTaskId, taskId)
                .eq(Cd90ScheduleTask::getTaskStatus, Cd90ScheduleTaskStatus.PENDING)
                .set(Cd90ScheduleTask::getTaskStatus, Cd90ScheduleTaskStatus.RUNNING)
                .set(Cd90ScheduleTask::getProgress, 5)
                .set(Cd90ScheduleTask::getCurrentStage, Cd90ScheduleTaskStage.VALIDATE_DATA)
                .set(Cd90ScheduleTask::getCurrentStageName, "基础数据校验")
                .set(Cd90ScheduleTask::getStartTime, now)
                .set(Cd90ScheduleTask::getLastHeartbeatTime, now)) == 1;
        log.info("[直裁自动排程] 任务启动状态更新, taskId={}, updated={}", taskId, updated);
        return updated;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean updateProgress(String taskId, int progress, String stage, String stageName) {
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("自动排程任务进度必须在0至100之间");
        }
        return taskMapper.update(null, new LambdaUpdateWrapper<Cd90ScheduleTask>()
                .eq(Cd90ScheduleTask::getTaskId, taskId)
                .eq(Cd90ScheduleTask::getTaskStatus, Cd90ScheduleTaskStatus.RUNNING)
                .set(Cd90ScheduleTask::getProgress, progress)
                .set(Cd90ScheduleTask::getCurrentStage, stage)
                .set(Cd90ScheduleTask::getCurrentStageName, stageName)
                .set(Cd90ScheduleTask::getLastHeartbeatTime, new Date())) == 1;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean markSuccess(String taskId, String batchNo) {
        return updateSuccess(taskId, batchNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markSuccessInCurrentTransaction(String taskId, String batchNo) {
        return updateSuccess(taskId, batchNo);
    }

    private boolean updateSuccess(String taskId, String batchNo) {
        return taskMapper.update(null, new LambdaUpdateWrapper<Cd90ScheduleTask>()
                .eq(Cd90ScheduleTask::getTaskId, taskId)
                .eq(Cd90ScheduleTask::getTaskStatus, Cd90ScheduleTaskStatus.RUNNING)
                .set(Cd90ScheduleTask::getTaskStatus, Cd90ScheduleTaskStatus.SUCCESS)
                .set(Cd90ScheduleTask::getProgress, 100)
                .set(Cd90ScheduleTask::getCurrentStage, Cd90ScheduleTaskStage.COMPLETE)
                .set(Cd90ScheduleTask::getCurrentStageName, "执行完成")
                .set(Cd90ScheduleTask::getBatchNo, batchNo)
                .set(Cd90ScheduleTask::getEndTime, new Date())
                .set(Cd90ScheduleTask::getLastHeartbeatTime, new Date())) == 1;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean markFailed(String taskId, String errorMessage) {
        String summary = errorMessage == null ? "自动排程执行失败" : errorMessage;
        if (summary.length() > MAX_ERROR_LENGTH) {
            summary = summary.substring(0, MAX_ERROR_LENGTH);
        }
        return taskMapper.update(null, new LambdaUpdateWrapper<Cd90ScheduleTask>()
                .eq(Cd90ScheduleTask::getTaskId, taskId)
                .in(Cd90ScheduleTask::getTaskStatus,
                        Arrays.asList(Cd90ScheduleTaskStatus.PENDING, Cd90ScheduleTaskStatus.RUNNING))
                .set(Cd90ScheduleTask::getTaskStatus, Cd90ScheduleTaskStatus.FAILED)
                .set(Cd90ScheduleTask::getErrorMessage, summary)
                .set(Cd90ScheduleTask::getEndTime, new Date())
                .set(Cd90ScheduleTask::getLastHeartbeatTime, new Date())) == 1;
    }

    @Override
    public List<Cd90ScheduleTask> findRunningTasks(int limit) {
        int queryLimit = limit <= 0 ? 500 : Math.min(limit, 1000);
        return taskMapper.selectList(new LambdaQueryWrapper<Cd90ScheduleTask>()
                .eq(Cd90ScheduleTask::getTaskStatus, Cd90ScheduleTaskStatus.RUNNING)
                .orderByAsc(Cd90ScheduleTask::getLastHeartbeatTime)
                .last("limit " + queryLimit));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean markTimeoutFailed(String taskId, String errorMessage) {
        String summary = errorMessage == null ? "自动排程任务心跳超时" : errorMessage;
        if (summary.length() > MAX_ERROR_LENGTH) summary = summary.substring(0, MAX_ERROR_LENGTH);
        return taskMapper.update(null, new LambdaUpdateWrapper<Cd90ScheduleTask>()
                .eq(Cd90ScheduleTask::getTaskId, taskId)
                .eq(Cd90ScheduleTask::getTaskStatus, Cd90ScheduleTaskStatus.RUNNING)
                .set(Cd90ScheduleTask::getTaskStatus, Cd90ScheduleTaskStatus.FAILED)
                .set(Cd90ScheduleTask::getCurrentStageName, "心跳超时补偿失败")
                .set(Cd90ScheduleTask::getErrorMessage, summary)
                .set(Cd90ScheduleTask::getEndTime, new Date())) == 1;
    }
}
