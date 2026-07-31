package com.zlt.aps.tm.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleIssueVo;
import com.zlt.aps.tm.api.domain.vo.TmReleaseFeedbackItemVo;
import com.zlt.aps.tm.api.domain.vo.TmReleaseFeedbackVo;
import com.zlt.aps.tm.api.domain.vo.TmReleaseTaskVo;
import com.zlt.aps.tm.api.enums.TmScheduleReleaseStatusEnum;
import com.zlt.aps.tm.domain.TmAutoScheduleTask;
import com.zlt.aps.tm.domain.TmReleaseCallbackLog;
import com.zlt.aps.tm.domain.TmReleaseTaskDetail;
import com.zlt.aps.tm.mapper.TmReleaseCallbackLogMapper;
import com.zlt.aps.tm.mapper.TmReleaseTaskDetailMapper;
import com.zlt.aps.tm.mapper.TmScheduleResultMapper;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;

/**
 * 胎面发布MES反馈去重、状态回写和任务收口服务（对齐胎侧 TcReleaseFeedbackService）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TmReleaseFeedbackService {

    /** MES反馈状态到结果发布状态的映射。 */
    private static final Map<String, String> RELEASE_STATUS_MAP;

    /** 终态反馈集合。 */
    private static final Set<String> TERMINAL_FEEDBACK_STATUS_SET = new LinkedHashSet<>(
            Arrays.asList("SUCCESS", "FAILED", "TIMEOUT", "IGNORED"));

    static {
        Map<String, String> releaseStatusMap = new LinkedHashMap<>();
        releaseStatusMap.put("SUCCESS", TmScheduleReleaseStatusEnum.RELEASED.getCode());
        releaseStatusMap.put("FAILED", TmScheduleReleaseStatusEnum.RELEASE_FAILED.getCode());
        releaseStatusMap.put("TIMEOUT", TmScheduleReleaseStatusEnum.TIMEOUT_FAILED.getCode());
        RELEASE_STATUS_MAP = Collections.unmodifiableMap(releaseStatusMap);
    }

    private final TmReleaseTaskDetailMapper detailMapper;
    private final TmReleaseCallbackLogMapper callbackLogMapper;
    private final TmScheduleResultMapper scheduleResultMapper;
    private final TmOperationTaskService operationTaskService;
    private final BaseDao baseDao;
    private final PlatformTransactionManager transactionManager;

    /**
     * 应用MES发布反馈，重复回调仅记录一次且不会覆盖新版本结果。
     *
     * @param feedback MES反馈
     * @return 处理摘要
     * @throws ServiceException 数据版本或反馈项为空时抛出
     */
    public AjaxResult applyFeedback(TmReleaseFeedbackVo feedback) {
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
    private FeedbackApplyResult applyInTransaction(TmReleaseFeedbackVo feedback) {
        FeedbackApplyResult applyResult = new FeedbackApplyResult();
        List<TmReleaseCallbackLog> callbackLogList = new ArrayList<>();
        String callbackVersion = StrUtil.blankToDefault(feedback.getCallbackVersion(), feedback.getDataVersion());
        for (TmReleaseFeedbackItemVo item : feedback.getItems()) {
            if (item == null || StrUtil.isBlank(item.getIdempotencyKey())) {
                continue;
            }
            Long duplicateCount = this.callbackLogMapper.selectCount(
                    new LambdaQueryWrapper<TmReleaseCallbackLog>()
                            .eq(TmReleaseCallbackLog::getIdempotencyKey, item.getIdempotencyKey())
                            .eq(TmReleaseCallbackLog::getCallbackVersion, callbackVersion));
            if (duplicateCount != null && duplicateCount > 0L) {
                applyResult.incrementDuplicateCount();
                continue;
            }
            TmReleaseTaskDetail detail = this.detailMapper.selectOne(
                    new LambdaQueryWrapper<TmReleaseTaskDetail>()
                            .eq(TmReleaseTaskDetail::getIdempotencyKey, item.getIdempotencyKey())
                            .orderByDesc(TmReleaseTaskDetail::getCreateTime)
                            .last("limit 1"));
            TmReleaseCallbackLog callbackLog = this.buildCallbackLog(item, callbackVersion);
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
                LambdaUpdateWrapper<TmScheduleResult> resultUpdateWrapper =
                        new LambdaUpdateWrapper<TmScheduleResult>()
                        .eq(TmScheduleResult::getId, detail.getResultId())
                        .eq(TmScheduleResult::getReleaseStatus,
                                TmScheduleReleaseStatusEnum.RELEASING.getCode())
                        .set(TmScheduleResult::getReleaseStatus, targetReleaseStatus)
                        .set(TmScheduleResult::getUpdateTime, new java.util.Date());
                // tm TmScheduleResult 无 taskVersion 字段，按 id + 发布中状态做乐观校验
                applied = this.scheduleResultMapper.update(null, resultUpdateWrapper) == 1;
            }
            String storedFeedbackStatus = targetReleaseStatus == null ? "UNKNOWN"
                    : (applied ? feedbackStatus : "IGNORED");
            this.detailMapper.update(null, new LambdaUpdateWrapper<TmReleaseTaskDetail>()
                    .eq(TmReleaseTaskDetail::getId, detail.getId())
                    .set(TmReleaseTaskDetail::getCallbackStatus, storedFeedbackStatus)
                    .set(TmReleaseTaskDetail::getCallbackMessage, item.getMessage())
                    .set(TmReleaseTaskDetail::getCallbackVersion, callbackVersion)
                    .set(TmReleaseTaskDetail::getAfterStatus, applied ? targetReleaseStatus : detail.getAfterStatus())
                    .set(TmReleaseTaskDetail::getUpdateTime, new java.util.Date()));
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
        List<TmReleaseTaskDetail> detailList = this.detailMapper.selectList(
                new LambdaQueryWrapper<TmReleaseTaskDetail>()
                        .eq(TmReleaseTaskDetail::getTaskId, taskId)
                        .orderByAsc(TmReleaseTaskDetail::getId));
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
        TmAutoScheduleTask task = this.operationTaskService.findByTaskId(taskId);
        TmReleaseTaskVo result = this.operationTaskService.toReleaseTaskVo(task);
        if (result == null) {
            result = new TmReleaseTaskVo();
        }
        result.setSummary(summary);
        if (failedCount == 0 && timeoutCount == 0) {
            this.operationTaskService.markSuccess(taskId, result, summary, Collections.emptyList());
            return;
        }
        TmAutoScheduleIssueVo issue = new TmAutoScheduleIssueVo();
        issue.setLevel("ERROR");
        issue.setStageCode("MES_FEEDBACK");
        issue.setStageName(I18nUtil.getMessage("ui.tc.schedule.release.feedbackStage"));
        issue.setCategory(timeoutCount > 0 ? "MES_TIMEOUT" : "MES_REJECTED");
        issue.setMessage(I18nUtil.getMessage(timeoutCount > 0
                ? "ui.tc.schedule.release.timeout" : "ui.tc.schedule.release.failed"));
        this.operationTaskService.markFailed(taskId, issue.getMessage(), summary,
                Collections.singletonList(issue));
    }

    /**
     * 构造回调去重日志。
     *
     * @param item 单条反馈
     * @param callbackVersion 回调版本
     * @return 回调日志
     */
    private TmReleaseCallbackLog buildCallbackLog(TmReleaseFeedbackItemVo item, String callbackVersion) {
        TmReleaseCallbackLog callbackLog = new TmReleaseCallbackLog();
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

    /**
     * 反馈应用结果内部容器。
     */
    private static final class FeedbackApplyResult {
        private final Set<String> taskIdSet = new LinkedHashSet<>();
        private final Map<String, Object> summary = new LinkedHashMap<>();
        private int appliedCount;
        private int ignoredCount;
        private int duplicateCount;

        Set<String> getTaskIdSet() {
            return this.taskIdSet;
        }

        void incrementAppliedCount() {
            this.appliedCount++;
        }

        void incrementIgnoredCount() {
            this.ignoredCount++;
        }

        void incrementDuplicateCount() {
            this.duplicateCount++;
        }

        Map<String, Object> getSummary() {
            return this.summary;
        }

        void finishSummary() {
            this.summary.put("appliedCount", this.appliedCount);
            this.summary.put("ignoredCount", this.ignoredCount);
            this.summary.put("duplicateCount", this.duplicateCount);
        }
    }
}
