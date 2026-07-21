package com.zlt.aps.tc.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResultIssue;
import com.zlt.aps.tc.api.domain.vo.TcReleaseFeedbackItemVo;
import com.zlt.aps.tc.api.domain.vo.TcReleaseFeedbackVo;
import com.zlt.aps.tc.component.TcScheduleResultIssueAssembler;
import com.zlt.aps.tc.domain.TcAutoScheduleTask;
import com.zlt.aps.tc.domain.TcReleaseTaskDetail;
import com.zlt.aps.tc.mapper.TcReleaseTaskDetailMapper;
import com.zlt.aps.tc.mapper.TcScheduleResultMapper;
import com.zlt.aps.tc.service.TcBackgroundTaskService;
import com.zlt.aps.tc.service.TcReleaseAsyncExecutor;
import com.zlt.aps.tc.service.TcReleaseFeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 胎侧排程异步下发MES执行器。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TcReleaseAsyncExecutorImpl implements TcReleaseAsyncExecutor {

    private final TcBackgroundTaskService backgroundTaskService;
    private final TcReleaseTaskDetailMapper detailMapper;
    private final TcScheduleResultMapper scheduleResultMapper;
    private final TcScheduleResultIssueAssembler issueAssembler;
    private final TcReleaseFeedbackService feedbackService;
    private final IMesItfService mesItfService;

    /**
     * 异步装配并下发发布数据。网络异常保留发布中状态，由超时恢复任务继续查询MES。
     *
     * @param taskId 发布任务ID
     */
    @Async
    @Override
    public void execute(String taskId) {
        if (!this.backgroundTaskService.start(taskId, "RELEASE_ASSEMBLE",
                I18nUtil.getMessage("ui.tc.schedule.release.assembleStage"))) {
            return;
        }
        TcAutoScheduleTask task = this.backgroundTaskService.findByTaskId(taskId);
        List<TcReleaseTaskDetail> detailList = this.detailMapper.selectList(
                new LambdaQueryWrapper<TcReleaseTaskDetail>()
                        .eq(TcReleaseTaskDetail::getTaskId, taskId)
                        .orderByAsc(TcReleaseTaskDetail::getResultId));
        try {
            if (task == null || CollectionUtils.isEmpty(detailList)) {
                this.backgroundTaskService.markFailed(taskId,
                        I18nUtil.getMessage("ui.tc.schedule.release.detailMissing"),
                        Collections.emptyMap(), Collections.emptyList());
                return;
            }
            List<Long> resultIdList = detailList.stream().map(TcReleaseTaskDetail::getResultId)
                    .distinct().collect(Collectors.toList());
            List<TcScheduleResult> resultList = this.scheduleResultMapper.selectList(
                    new LambdaQueryWrapper<TcScheduleResult>()
                            .in(TcScheduleResult::getId, resultIdList)
                            .orderByAsc(TcScheduleResult::getId));
            if (resultList.size() != resultIdList.size()) {
                this.applyTerminalFeedback(task, detailList, "FAILED",
                        I18nUtil.getMessage("ui.tc.schedule.release.resultMissing"));
                return;
            }
            List<TcScheduleResultIssue> issueList = this.issueAssembler.assemble(
                    resultList, task.getMesDataVersion());
            if (CollectionUtils.isEmpty(issueList)) {
                this.applyTerminalFeedback(task, detailList, "FAILED",
                        I18nUtil.getMessage("ui.tc.schedule.release.noIssueData"));
                return;
            }
            this.saveIssuePayload(detailList, issueList);
            this.backgroundTaskService.updateProgress(taskId, 70, "RELEASE_ISSUE",
                    I18nUtil.getMessage("ui.tc.schedule.release.issueStage"), null);
            AjaxResult issueResult = FeignTokenHelper.callWithToken(
                    () -> this.mesItfService.issueTcScheduleResult(issueList));
            String feedbackStatus = issueResult == null || issueResult.get("feedbackStatus") == null
                    ? null : String.valueOf(issueResult.get("feedbackStatus"));
            if ("SUCCESS".equals(feedbackStatus)
                    || (feedbackStatus == null && issueResult != null && Objects.equals(HttpStatus.SUCCESS,
                    issueResult.get(AjaxResult.CODE_TAG)))) {
                this.applyTerminalFeedback(task, detailList, "SUCCESS",
                        I18nUtil.getMessage("ui.tc.schedule.release.success"));
                return;
            }
            if ("TIMEOUT".equals(feedbackStatus) || "FAILED".equals(feedbackStatus)) {
                String message = issueResult.get(AjaxResult.MSG_TAG) == null
                        ? I18nUtil.getMessage("TIMEOUT".equals(feedbackStatus)
                        ? "ui.tc.schedule.release.timeout" : "ui.tc.schedule.release.failed")
                        : String.valueOf(issueResult.get(AjaxResult.MSG_TAG));
                this.applyTerminalFeedback(task, detailList, feedbackStatus, message);
                return;
            }
            if ("PENDING".equals(feedbackStatus)) {
                this.backgroundTaskService.updateProgress(taskId, 80, "RELEASE_WAIT_FEEDBACK",
                        I18nUtil.getMessage("ui.tc.schedule.release.waitFeedbackStage"), null);
                return;
            }
            String message = issueResult == null || issueResult.get(AjaxResult.MSG_TAG) == null
                    ? I18nUtil.getMessage("ui.tc.schedule.release.failed")
                    : String.valueOf(issueResult.get(AjaxResult.MSG_TAG));
            this.applyTerminalFeedback(task, detailList, "FAILED", message);
        } catch (Exception exception) {
            log.error("胎侧排程发布MES调用异常，等待超时恢复，taskId={}", taskId, exception);
            this.backgroundTaskService.updateProgress(taskId, 80, "RELEASE_WAIT_FEEDBACK",
                    I18nUtil.getMessage("ui.tc.schedule.release.waitFeedbackStage"), exception.getMessage());
        }
    }

    /**
     * 保存每条发布明细实际发送的MES载荷快照。
     *
     * @param detailList 发布明细
     * @param issueList MES载荷
     */
    private void saveIssuePayload(List<TcReleaseTaskDetail> detailList,
                                  List<TcScheduleResultIssue> issueList) {
        Map<String, List<TcScheduleResultIssue>> issueMap = issueList.stream().collect(Collectors.groupingBy(
                TcScheduleResultIssue::getIdempotencyKey, Collectors.toList()));
        detailList.stream().forEach(detail -> this.detailMapper.update(null,
                new LambdaUpdateWrapper<TcReleaseTaskDetail>()
                        .eq(TcReleaseTaskDetail::getTaskId, detail.getTaskId())
                        .eq(TcReleaseTaskDetail::getIdempotencyKey, detail.getIdempotencyKey())
                        .set(TcReleaseTaskDetail::getIssuePayloadJson,
                                JSON.toJSONString(issueMap.getOrDefault(detail.getIdempotencyKey(),
                                        Collections.emptyList())))));
    }

    /**
     * 构造统一终态反馈并复用反馈服务完成结果、明细和任务收口。
     *
     * @param task 发布任务
     * @param detailList 发布明细
     * @param feedbackStatus 反馈状态
     * @param message 反馈说明
     */
    private void applyTerminalFeedback(TcAutoScheduleTask task, List<TcReleaseTaskDetail> detailList,
                                       String feedbackStatus, String message) {
        TcReleaseFeedbackVo feedback = new TcReleaseFeedbackVo();
        feedback.setDataVersion(task.getMesDataVersion());
        feedback.setCallbackVersion(task.getMesDataVersion());
        feedback.setItems(detailList.stream().map(detail -> {
            TcReleaseFeedbackItemVo item = new TcReleaseFeedbackItemVo();
            item.setIdempotencyKey(detail.getIdempotencyKey());
            item.setFeedbackStatus(feedbackStatus);
            item.setMessage(message);
            return item;
        }).collect(Collectors.toList()));
        this.feedbackService.applyFeedback(feedback);
    }
}
