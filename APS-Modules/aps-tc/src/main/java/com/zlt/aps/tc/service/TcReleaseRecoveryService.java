package com.zlt.aps.tc.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.entity.TcParams;
import com.zlt.aps.tc.api.domain.vo.TcReleaseFeedbackItemVo;
import com.zlt.aps.tc.api.domain.vo.TcReleaseFeedbackVo;
import com.zlt.aps.tc.api.enums.TcAutoScheduleTaskStatusEnum;
import com.zlt.aps.tc.api.enums.TcBackgroundTaskTypeEnum;
import com.zlt.aps.tc.domain.TcAutoScheduleTask;
import com.zlt.aps.tc.domain.TcReleaseTaskDetail;
import com.zlt.aps.tc.mapper.TcAutoScheduleTaskMapper;
import com.zlt.aps.tc.mapper.TcParamsMapper;
import com.zlt.aps.tc.mapper.TcReleaseTaskDetailMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 胎侧发布超时任务查询MES并恢复终态服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TcReleaseRecoveryService {

    private final TcAutoScheduleTaskMapper taskMapper;
    private final TcParamsMapper paramsMapper;
    private final TcReleaseTaskDetailMapper detailMapper;
    private final TcReleaseFeedbackService feedbackService;
    private final TcBackgroundTaskService backgroundTaskService;
    private final IMesItfService mesItfService;

    /**
     * 恢复超过默认超时时间仍处于执行中的发布任务。
     *
     * @return 已处理任务数
     */
    public int recoverTimeoutTasks() {
        List<TcAutoScheduleTask> taskList = this.taskMapper.selectList(
                new LambdaQueryWrapper<TcAutoScheduleTask>()
                        .eq(TcAutoScheduleTask::getTaskType, TcBackgroundTaskTypeEnum.RELEASE.getCode())
                        .eq(TcAutoScheduleTask::getTaskStatus, TcAutoScheduleTaskStatusEnum.RUNNING.getCode())
                        .orderByAsc(TcAutoScheduleTask::getCreateTime));
        Date currentTime = new Date();
        List<TcAutoScheduleTask> timeoutTaskList = CollectionUtils.emptyIfNull(taskList).stream()
                .filter(task -> this.isTimeout(task, currentTime)).collect(Collectors.toList());
        timeoutTaskList.stream().forEach(this::recoverTask);
        return timeoutTaskList.size();
    }

    /**
     * 判断发布任务是否超过工厂日期生效的超时阈值。
     *
     * @param task 发布任务
     * @param currentTime 当前时间
     * @return 超时返回true
     */
    private boolean isTimeout(TcAutoScheduleTask task, Date currentTime) {
        Date heartbeatTime = task.getLastHeartbeatTime() == null
                ? task.getCreateTime() : task.getLastHeartbeatTime();
        if (heartbeatTime == null) {
            return false;
        }
        int timeoutMinutes = this.resolveTimeoutMinutes(task);
        return !heartbeatTime.after(DateUtil.offsetMinute(currentTime, -timeoutMinutes));
    }

    /**
     * 读取发布超时分钟数，未配置或格式无效时使用默认值。
     *
     * @param task 发布任务
     * @return 正整数分钟数
     */
    private int resolveTimeoutMinutes(TcAutoScheduleTask task) {
        TcParams params = this.paramsMapper.selectOne(new LambdaQueryWrapper<TcParams>()
                .eq(TcParams::getFactoryCode, task.getFactoryCode())
                .eq(TcParams::getParamCode, TcScheduleConstants.PARAM_RELEASE_TIMEOUT_MINUTES)
                .eq(TcParams::getEnableStatus, "1")
                .and(condition -> condition.isNull(TcParams::getEffectiveStartTime)
                        .or().le(TcParams::getEffectiveStartTime, task.getScheduleDate()))
                .and(condition -> condition.isNull(TcParams::getEffectiveEndTime)
                        .or().ge(TcParams::getEffectiveEndTime, task.getScheduleDate()))
                .orderByDesc(TcParams::getEffectiveStartTime)
                .last("limit 1"));
        String timeoutValue = params == null ? TcScheduleConstants.DEFAULT_RELEASE_TIMEOUT_MINUTES
                : StrUtil.blankToDefault(params.getParamValue(),
                StrUtil.blankToDefault(params.getDefaultValue(),
                        TcScheduleConstants.DEFAULT_RELEASE_TIMEOUT_MINUTES));
        try {
            return Math.max(1, Integer.parseInt(timeoutValue));
        } catch (NumberFormatException exception) {
            return Integer.parseInt(TcScheduleConstants.DEFAULT_RELEASE_TIMEOUT_MINUTES);
        }
    }

    /**
     * 查询一条发布任务MES状态，无明确终态时按超时失败收口。
     *
     * @param task 发布任务
     */
    private void recoverTask(TcAutoScheduleTask task) {
        List<TcReleaseTaskDetail> detailList = this.detailMapper.selectList(
                new LambdaQueryWrapper<TcReleaseTaskDetail>()
                        .eq(TcReleaseTaskDetail::getTaskId, task.getTaskId())
                        .orderByAsc(TcReleaseTaskDetail::getResultId));
        if (CollectionUtils.isEmpty(detailList)) {
            this.backgroundTaskService.markFailed(task.getTaskId(), "DETAIL_MISSING",
                    Collections.emptyMap(), Collections.emptyList());
            return;
        }
        try {
            TcReleaseFeedbackVo mesFeedback = FeignTokenHelper.callWithToken(
                    () -> this.mesItfService.queryTcScheduleIssueStatus(task.getMesDataVersion()));
            if (mesFeedback != null && CollectionUtils.isNotEmpty(mesFeedback.getItems())) {
                this.feedbackService.applyFeedback(mesFeedback);
                return;
            }
        } catch (Exception exception) {
            log.warn("胎侧发布超时恢复查询MES失败，按超时收口，taskId={}", task.getTaskId(), exception);
        }
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
    private TcReleaseFeedbackVo buildTimeoutFeedback(TcAutoScheduleTask task,
                                                      List<TcReleaseTaskDetail> detailList,
                                                      String message) {
        TcReleaseFeedbackVo feedback = new TcReleaseFeedbackVo();
        feedback.setDataVersion(task.getMesDataVersion());
        feedback.setCallbackVersion(task.getMesDataVersion() + "-TIMEOUT");
        feedback.setItems(detailList.stream().map(detail -> {
            TcReleaseFeedbackItemVo item = new TcReleaseFeedbackItemVo();
            item.setIdempotencyKey(detail.getIdempotencyKey());
            item.setFeedbackStatus("TIMEOUT");
            item.setMessage(message);
            return item;
        }).collect(Collectors.toList()));
        return feedback;
    }
}
