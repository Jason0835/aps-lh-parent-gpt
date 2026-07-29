package com.zlt.aps.tm.service;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.domain.vo.TmReleaseFeedbackItemVo;
import com.zlt.aps.tm.api.domain.vo.TmReleaseFeedbackVo;
import com.zlt.aps.tm.api.enums.TmAutoScheduleTaskStatusEnum;
import com.zlt.aps.tm.api.enums.TmBackgroundTaskTypeEnum;
import com.zlt.aps.tm.domain.TmAutoScheduleTask;
import com.zlt.aps.tm.domain.TmReleaseTaskDetail;
import com.zlt.aps.tm.mapper.TmAutoScheduleTaskMapper;
import com.zlt.aps.tm.mapper.TmReleaseTaskDetailMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 胎面发布超时任务查询MES并恢复终态服务（对齐胎侧 TcReleaseRecoveryService）。
 *
 * <p>超时阈值暂用 {@link TmScheduleConstants#DEFAULT_RELEASE_TIMEOUT_MINUTES} 默认值，
 * 待后续对接 tm 参数加载器读取 TM_RELEASE_TIMEOUT_MINUTES。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TmReleaseRecoveryService {

    private final TmAutoScheduleTaskMapper taskMapper;
    private final TmReleaseTaskDetailMapper detailMapper;
    private final TmReleaseFeedbackService feedbackService;
    private final TmOperationTaskService operationTaskService;
    private final IMesItfService mesItfService;

    /**
     * 恢复超过默认超时时间仍处于执行中的发布任务。
     *
     * @return 已处理任务数
     */
    public int recoverTimeoutTasks() {
        List<TmAutoScheduleTask> taskList = this.taskMapper.selectList(
                new LambdaQueryWrapper<TmAutoScheduleTask>()
                        .eq(TmAutoScheduleTask::getTaskType, TmBackgroundTaskTypeEnum.RELEASE.getCode())
                        .eq(TmAutoScheduleTask::getTaskStatus, TmAutoScheduleTaskStatusEnum.RUNNING.getCode())
                        .orderByAsc(TmAutoScheduleTask::getCreateTime));
        Date currentTime = new Date();
        List<TmAutoScheduleTask> timeoutTaskList = CollectionUtils.emptyIfNull(taskList).stream()
                .filter(task -> this.isTimeout(task, currentTime)).collect(Collectors.toList());
        timeoutTaskList.stream().forEach(this::recoverTask);
        return timeoutTaskList.size();
    }

    /**
     * 判断发布任务是否超过超时阈值。
     *
     * @param task 发布任务
     * @param currentTime 当前时间
     * @return 超时返回true
     */
    private boolean isTimeout(TmAutoScheduleTask task, Date currentTime) {
        Date heartbeatTime = task.getLastHeartbeatTime() == null
                ? task.getCreateTime() : task.getLastHeartbeatTime();
        if (heartbeatTime == null) {
            return false;
        }
        int timeoutMinutes = this.resolveTimeoutMinutes();
        return !heartbeatTime.after(DateUtil.offsetMinute(currentTime, -timeoutMinutes));
    }

    /**
     * 读取发布超时分钟数，暂用默认值。
     *
     * @return 正整数分钟数
     */
    private int resolveTimeoutMinutes() {
        try {
            return Math.max(1, Integer.parseInt(TmScheduleConstants.DEFAULT_RELEASE_TIMEOUT_MINUTES));
        } catch (NumberFormatException exception) {
            return 10;
        }
    }

    /**
     * 查询一条发布任务MES状态，无明确终态时按超时失败收口。
     *
     * @param task 发布任务
     */
    private void recoverTask(TmAutoScheduleTask task) {
        List<TmReleaseTaskDetail> detailList = this.detailMapper.selectList(
                new LambdaQueryWrapper<TmReleaseTaskDetail>()
                        .eq(TmReleaseTaskDetail::getTaskId, task.getTaskId())
                        .orderByAsc(TmReleaseTaskDetail::getResultId));
        if (CollectionUtils.isEmpty(detailList)) {
            this.operationTaskService.markFailed(task.getTaskId(), "DETAIL_MISSING",
                    Collections.emptyMap(), Collections.emptyList());
            return;
        }
        // tm 简化：超时直接按 TIMEOUT 收口（待后续对接 itf queryTmScheduleIssueStatus 回查 MES）
        this.feedbackService.applyFeedback(this.buildTimeoutFeedback(task, detailList, "MES_TIMEOUT"));
    }

    /**
     * 为任务全部明细构造超时反馈。
     *
     * @param task 发布任务
     * @param detailList 发布明细
     * @param message 超时说明
     * @return 超时反馈
     */
    private TmReleaseFeedbackVo buildTimeoutFeedback(TmAutoScheduleTask task,
                                                      List<TmReleaseTaskDetail> detailList,
                                                      String message) {
        TmReleaseFeedbackVo feedback = new TmReleaseFeedbackVo();
        feedback.setDataVersion(task.getMesDataVersion());
        feedback.setCallbackVersion(task.getMesDataVersion() + "-TIMEOUT");
        feedback.setItems(detailList.stream().map(detail -> {
            TmReleaseFeedbackItemVo item = new TmReleaseFeedbackItemVo();
            item.setIdempotencyKey(detail.getIdempotencyKey());
            item.setFeedbackStatus("TIMEOUT");
            item.setMessage(message);
            return item;
        }).collect(Collectors.toList()));
        return feedback;
    }
}
