package com.zlt.aps.tc.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.domain.vo.TcAutoScheduleIssueVo;
import com.zlt.aps.tc.api.domain.vo.TcReleaseFeedbackItemVo;
import com.zlt.aps.tc.api.domain.vo.TcReleaseFeedbackVo;
import com.zlt.aps.tc.api.domain.vo.TcReleaseTaskVo;
import com.zlt.aps.tc.api.enums.TcScheduleReleaseStatusEnum;
import com.zlt.aps.tc.domain.TcAutoScheduleTask;
import com.zlt.aps.tc.domain.TcReleaseCallbackLog;
import com.zlt.aps.tc.domain.TcReleaseTaskDetail;
import com.zlt.aps.tc.mapper.TcReleaseCallbackLogMapper;
import com.zlt.aps.tc.mapper.TcReleaseTaskDetailMapper;
import com.zlt.aps.tc.mapper.TcScheduleResultMapper;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;

/**
 * 胎侧发布MES反馈去重、状态回写和任务收口服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TcReleaseFeedbackService {

    /** MES反馈状态到结果发布状态的映射。 */
    private static final Map<String, String> RELEASE_STATUS_MAP;

    /** 终态反馈集合。 */
    private static final Set<String> TERMINAL_FEEDBACK_STATUS_SET = new LinkedHashSet<>(
            Arrays.asList("SUCCESS", "FAILED", "TIMEOUT", "IGNORED"));

    static {
        Map<String, String> releaseStatusMap = new LinkedHashMap<>();
        releaseStatusMap.put("SUCCESS", TcScheduleReleaseStatusEnum.RELEASED.getCode());
        releaseStatusMap.put("FAILED", TcScheduleReleaseStatusEnum.RELEASE_FAILED.getCode());
        releaseStatusMap.put("TIMEOUT", TcScheduleReleaseStatusEnum.TIMEOUT_FAILED.getCode());
        RELEASE_STATUS_MAP = Collections.unmodifiableMap(releaseStatusMap);
    }

    private final TcReleaseTaskDetailMapper detailMapper;
    private final TcReleaseCallbackLogMapper callbackLogMapper;
    private final TcScheduleResultMapper scheduleResultMapper;
    private final TcBackgroundTaskService backgroundTaskService;
    private final BaseDao baseDao;
    private final PlatformTransactionManager transactionManager;

    /**
     * 应用MES发布反馈，重复回调仅记录一次且不会覆盖新版本结果。
     *
     * @param feedback MES反馈
     * @return 处理摘要
     * @throws ServiceException 数据版本或反馈项为空时抛出
     */
    public AjaxResult applyFeedback(TcReleaseFeedbackVo feedback) {
        if (feedback == null || StrUtil.isBlank(feedback.getDataVersion())
                || CollectionUtils.isEmpty(feedback.getItems())) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.release.feedbackInvalid"));
        }
        TransactionTemplate transactionTemplate = new TransactionTemplate(this.transactionManager);
        FeedbackApplyResult applyResult = transactionTemplate.execute(status -> this.applyInTransaction(feedback));
        if (applyResult == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.release.feedbackFailed"));
        }
        applyResult.getTaskIdSet().stream().forEach(this::finishTaskIfTerminal);
        return AjaxResult.success(I18nUtil.getMessage("ui.tc.schedule.release.feedbackSuccess"),
                applyResult.getSummary());
    }

    /**
     * 在短事务中去重并更新结果、明细和回调日志。
     *
     * @param feedback MES反馈
     * @return 应用结果
     */
    private FeedbackApplyResult applyInTransaction(TcReleaseFeedbackVo feedback) {
        FeedbackApplyResult applyResult = new FeedbackApplyResult();
        List<TcReleaseCallbackLog> callbackLogList = new ArrayList<>();
        String callbackVersion = StrUtil.blankToDefault(feedback.getCallbackVersion(), feedback.getDataVersion());
        for (TcReleaseFeedbackItemVo item : feedback.getItems()) {
            if (item == null || StrUtil.isBlank(item.getIdempotencyKey())) {
                continue;
            }
            Long duplicateCount = this.callbackLogMapper.selectCount(
                    new LambdaQueryWrapper<TcReleaseCallbackLog>()
                            .eq(TcReleaseCallbackLog::getIdempotencyKey, item.getIdempotencyKey())
                            .eq(TcReleaseCallbackLog::getCallbackVersion, callbackVersion));
            if (duplicateCount != null && duplicateCount > 0L) {
                applyResult.incrementDuplicateCount();
                continue;
            }
            TcReleaseTaskDetail detail = this.detailMapper.selectOne(
                    new LambdaQueryWrapper<TcReleaseTaskDetail>()
                            .eq(TcReleaseTaskDetail::getIdempotencyKey, item.getIdempotencyKey())
                            .orderByDesc(TcReleaseTaskDetail::getCreateTime)
                            .last("limit 1"));
            TcReleaseCallbackLog callbackLog = this.buildCallbackLog(item, callbackVersion);
            if (detail == null) {
                callbackLog.setAppliedFlag("0");
                callbackLog.setIgnoredReason("DETAIL_NOT_FOUND");
                callbackLogList.add(callbackLog);
                applyResult.incrementIgnoredCount();
                continue;
            }
            applyResult.getTaskIdSet().add(detail.getTaskId());
            String feedbackStatus = StrUtil.blankToDefault(item.getFeedbackStatus(), "UNKNOWN").toUpperCase();
            String targetReleaseStatus = RELEASE_STATUS_MAP.get(feedbackStatus);
            boolean applied = false;
            if (targetReleaseStatus != null) {
                LambdaUpdateWrapper<TcScheduleResult> resultUpdateWrapper =
                        new LambdaUpdateWrapper<TcScheduleResult>()
                        .eq(TcScheduleResult::getId, detail.getResultId())
                        .eq(TcScheduleResult::getReleaseStatus,
                                TcScheduleReleaseStatusEnum.RELEASING.getCode())
                        .set(TcScheduleResult::getReleaseStatus, targetReleaseStatus)
                        .set(TcScheduleResult::getUpdateTime, new Date());
                if (detail.getTaskVersion() == null || detail.getTaskVersion() == 0L) {
                    resultUpdateWrapper.and(wrapper -> wrapper.isNull(TcScheduleResult::getTaskVersion)
                            .or().eq(TcScheduleResult::getTaskVersion, 0L));
                } else {
                    resultUpdateWrapper.eq(TcScheduleResult::getTaskVersion, detail.getTaskVersion());
                }
                applied = this.scheduleResultMapper.update(null, resultUpdateWrapper) == 1;
            }
            String storedFeedbackStatus = targetReleaseStatus == null ? "UNKNOWN"
                    : (applied ? feedbackStatus : "IGNORED");
            this.detailMapper.update(null, new LambdaUpdateWrapper<TcReleaseTaskDetail>()
                    .eq(TcReleaseTaskDetail::getId, detail.getId())
                    .set(TcReleaseTaskDetail::getCallbackStatus, storedFeedbackStatus)
                    .set(TcReleaseTaskDetail::getCallbackMessage, item.getMessage())
                    .set(TcReleaseTaskDetail::getCallbackVersion, callbackVersion)
                    .set(TcReleaseTaskDetail::getAfterStatus, applied ? targetReleaseStatus : detail.getAfterStatus())
                    .set(TcReleaseTaskDetail::getUpdateTime, new Date()));
            callbackLog.setAppliedFlag(applied ? "1" : "0");
            callbackLog.setIgnoredReason(applied ? null
                    : (targetReleaseStatus == null ? "UNKNOWN_STATUS" : "STALE_RESULT_VERSION"));
            callbackLogList.add(callbackLog);
            if (applied) {
                applyResult.incrementAppliedCount();
            } else {
                applyResult.incrementIgnoredCount();
            }
        }
        if (CollectionUtils.isNotEmpty(callbackLogList)) {
            this.baseDao.saveBatch(callbackLogList);
        }
        applyResult.finishSummary();
        return applyResult;
    }

    /**
     * 当发布任务全部明细进入终态时更新任务终态。
     *
     * @param taskId 发布任务ID
     */
    private void finishTaskIfTerminal(String taskId) {
        List<TcReleaseTaskDetail> detailList = this.detailMapper.selectList(
                new LambdaQueryWrapper<TcReleaseTaskDetail>()
                        .eq(TcReleaseTaskDetail::getTaskId, taskId)
                        .orderByAsc(TcReleaseTaskDetail::getId));
        if (CollectionUtils.isEmpty(detailList) || detailList.stream().anyMatch(detail ->
                !TERMINAL_FEEDBACK_STATUS_SET.contains(detail.getCallbackStatus()))) {
            return;
        }
        int successCount = (int) detailList.stream().filter(detail -> "SUCCESS".equals(
                detail.getCallbackStatus())).count();
        int timeoutCount = (int) detailList.stream().filter(detail -> "TIMEOUT".equals(
                detail.getCallbackStatus())).count();
        int failedCount = detailList.size() - successCount - timeoutCount;
        Map<String, Object> summary = this.buildTaskSummary(detailList.size(), successCount,
                failedCount, timeoutCount);
        TcAutoScheduleTask task = this.backgroundTaskService.findByTaskId(taskId);
        TcReleaseTaskVo result = this.backgroundTaskService.toReleaseTaskVo(task);
        if (result == null) {
            result = new TcReleaseTaskVo();
        }
        result.setSummary(summary);
        if (failedCount == 0 && timeoutCount == 0) {
            this.backgroundTaskService.markSuccess(taskId, result, summary, Collections.emptyList());
            return;
        }
        TcAutoScheduleIssueVo issue = new TcAutoScheduleIssueVo();
        issue.setLevel("ERROR");
        issue.setStageCode("MES_FEEDBACK");
        issue.setStageName(I18nUtil.getMessage("ui.tc.schedule.release.feedbackStage"));
        issue.setCategory(timeoutCount > 0 ? "MES_TIMEOUT" : "MES_REJECTED");
        issue.setMessage(I18nUtil.getMessage(timeoutCount > 0
                ? "ui.tc.schedule.release.timeout" : "ui.tc.schedule.release.failed"));
        this.backgroundTaskService.markFailed(taskId, issue.getMessage(), summary,
                Collections.singletonList(issue));
    }

    /**
     * 构造回调去重日志。
     *
     * @param item 单条反馈
     * @param callbackVersion 回调版本
     * @return 回调日志
     */
    private TcReleaseCallbackLog buildCallbackLog(TcReleaseFeedbackItemVo item, String callbackVersion) {
        TcReleaseCallbackLog callbackLog = new TcReleaseCallbackLog();
        callbackLog.setIdempotencyKey(item.getIdempotencyKey());
        callbackLog.setCallbackVersion(callbackVersion);
        callbackLog.setCallbackStatus(StrUtil.blankToDefault(item.getFeedbackStatus(), "UNKNOWN").toUpperCase());
        callbackLog.setCallbackJson(JSON.toJSONString(item));
        callbackLog.setCreateBy("MES");
        return callbackLog;
    }

    /**
     * 构造发布任务摘要。
     *
     * @param selectedCount 选择数量
     * @param successCount 成功数量
     * @param failedCount 失败数量
     * @param timeoutCount 超时数量
     * @return 任务摘要
     */
    private Map<String, Object> buildTaskSummary(int selectedCount, int successCount,
                                                  int failedCount, int timeoutCount) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("schemaVersion", 1);
        summary.put("selectedCount", selectedCount);
        summary.put("successCount", successCount);
        summary.put("failedCount", failedCount);
        summary.put("timeoutCount", timeoutCount);
        return summary;
    }
}
