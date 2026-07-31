package com.zlt.aps.tm.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResultIssue;
import com.zlt.aps.tm.api.domain.vo.TmReleaseFeedbackItemVo;
import com.zlt.aps.tm.api.domain.vo.TmReleaseFeedbackVo;
import com.zlt.aps.tm.component.TmScheduleResultIssueAssembler;
import com.zlt.aps.tm.domain.TmAutoScheduleTask;
import com.zlt.aps.tm.domain.TmReleaseTaskDetail;
import com.zlt.aps.tm.mapper.TmReleaseTaskDetailMapper;
import com.zlt.aps.tm.mapper.TmScheduleResultMapper;
import com.zlt.aps.tm.service.TmOperationTaskService;
import com.zlt.aps.tm.service.TmReleaseAsyncExecutor;
import com.zlt.aps.tm.service.TmReleaseFeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 胎面排程异步下发MES执行器实现（对齐胎侧 TcReleaseAsyncExecutorImpl）。
 *
 * <p>网络异常保留发布中状态，由超时恢复任务继续查询MES。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TmReleaseAsyncExecutorImpl implements TmReleaseAsyncExecutor {

    private final TmOperationTaskService operationTaskService;
    private final TmReleaseTaskDetailMapper detailMapper;
    private final TmScheduleResultMapper scheduleResultMapper;
    private final TmScheduleResultIssueAssembler issueAssembler;
    private final TmReleaseFeedbackService feedbackService;
    private final IMesItfService mesItfService;

    /**
     * 异步装配并下发发布数据。网络异常保留发布中状态，由超时恢复任务继续查询MES。
     *
     * @param taskId 发布任务ID
     */
    @Async
    @Override
    public void execute(String taskId) {
        if (!this.operationTaskService.start(taskId, "RELEASE_ASSEMBLE",
                I18nUtil.getMessage("ui.tc.schedule.release.assembleStage"))) {
            return;
        }
        TmAutoScheduleTask task = this.operationTaskService.findByTaskId(taskId);
        List<TmReleaseTaskDetail> detailList = this.detailMapper.selectList(
                new LambdaQueryWrapper<TmReleaseTaskDetail>()
                        .eq(TmReleaseTaskDetail::getTaskId, taskId)
                        .orderByAsc(TmReleaseTaskDetail::getResultId));
        try {
            if (task == null || CollectionUtils.isEmpty(detailList)) {
                this.operationTaskService.markFailed(taskId,
                        I18nUtil.getMessage("ui.tc.schedule.release.detailMissing"),
                        Collections.emptyMap(), Collections.emptyList());
                return;
            }
            List<Long> resultIdList = detailList.stream().map(TmReleaseTaskDetail::getResultId)
                    .distinct().collect(Collectors.toList());
            List<TmScheduleResult> resultList = this.scheduleResultMapper.selectList(
                    new LambdaQueryWrapper<TmScheduleResult>()
                            .in(TmScheduleResult::getId, resultIdList)
                            .orderByAsc(TmScheduleResult::getId));
            if (resultList.size() != resultIdList.size()) {
                this.applyTerminalFeedback(task, detailList, "FAILED",
                        I18nUtil.getMessage("ui.tc.schedule.release.resultMissing"));
                return;
            }
            List<TmScheduleResultIssue> issueList = this.issueAssembler.assemble(
                    resultList, task.getMesDataVersion());
            if (CollectionUtils.isEmpty(issueList)) {
                this.applyTerminalFeedback(task, detailList, "FAILED",
                        I18nUtil.getMessage("ui.tc.schedule.release.noIssueData"));
                return;
            }
            this.saveIssuePayload(detailList, issueList);
            this.operationTaskService.updateProgress(taskId, 70, "RELEASE_ISSUE",
                    I18nUtil.getMessage("ui.tc.schedule.release.issueStage"), null);
            AjaxResult issueResult = FeignTokenHelper.callWithToken(
                    () -> this.mesItfService.issueTmScheduleResult(issueList));
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
                this.operationTaskService.updateProgress(taskId, 80, "RELEASE_WAIT_FEEDBACK",
                        I18nUtil.getMessage("ui.tc.schedule.release.waitFeedbackStage"), null);
                return;
            }
            String message = issueResult == null || issueResult.get(AjaxResult.MSG_TAG) == null
                    ? I18nUtil.getMessage("ui.tc.schedule.release.failed")
                    : String.valueOf(issueResult.get(AjaxResult.MSG_TAG));
            this.applyTerminalFeedback(task, detailList, "FAILED", message);
        } catch (Exception exception) {
            log.error("胎面排程发布MES调用异常，等待超时恢复，taskId={}", taskId, exception);
            this.operationTaskService.updateProgress(taskId, 80, "RELEASE_WAIT_FEEDBACK",
                    I18nUtil.getMessage("ui.tc.schedule.release.waitFeedbackStage"), exception.getMessage());
        }
    }

    /**
     * 保存发布明细实际发送的MES载荷快照。
     *
     * <p>tm TmScheduleResultIssue 无幂等键字段，按任务整体载荷写入每条明细。</p>
     *
     * @param detailList 发布明细
     * @param issueList MES载荷
     */
    private void saveIssuePayload(List<TmReleaseTaskDetail> detailList,
                                  List<TmScheduleResultIssue> issueList) {
        String payloadJson = JSON.toJSONString(issueList);
        detailList.stream().forEach(detail -> this.detailMapper.update(null,
                new LambdaUpdateWrapper<TmReleaseTaskDetail>()
                        .eq(TmReleaseTaskDetail::getTaskId, detail.getTaskId())
                        .eq(TmReleaseTaskDetail::getIdempotencyKey, detail.getIdempotencyKey())
                        .set(TmReleaseTaskDetail::getIssuePayloadJson, payloadJson)));
    }

    /**
     * 构造统一终态反馈并复用反馈服务完成结果、明细和任务收口。
     *
     * @param task 发布任务
     * @param detailList 发布明细
     * @param feedbackStatus 反馈状态
     * @param message 反馈说明
     */
    private void applyTerminalFeedback(TmAutoScheduleTask task, List<TmReleaseTaskDetail> detailList,
                                       String feedbackStatus, String message) {
        TmReleaseFeedbackVo feedback = new TmReleaseFeedbackVo();
        feedback.setDataVersion(task.getMesDataVersion());
        feedback.setCallbackVersion(task.getMesDataVersion());
        feedback.setItems(detailList.stream().map(detail -> {
            TmReleaseFeedbackItemVo item = new TmReleaseFeedbackItemVo();
            item.setIdempotencyKey(detail.getIdempotencyKey());
            item.setFeedbackStatus(feedbackStatus);
            item.setMessage(message);
            return item;
        }).collect(Collectors.toList()));
        this.feedbackService.applyFeedback(feedback);
    }
}
