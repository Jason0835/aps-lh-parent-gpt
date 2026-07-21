package com.zlt.aps.cd15.service.impl;

import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResultIssue;
import com.zlt.aps.cd15.component.Cd15ScheduleExecutionGuard;
import com.zlt.aps.cd15.component.Cd15ScheduleResultIssueAssembler;
import com.zlt.aps.cd15.service.Cd15ScheduleResultPublishService;
import com.zlt.aps.cd15.service.ICd15ScheduleResultService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.itf.mes.IMesItfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** 斜裁排程结果发布业务编排实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd15ScheduleResultPublishServiceImpl
        implements Cd15ScheduleResultPublishService {

    private static final int MAX_RETRIES = 3;
    private static final long[] RETRY_DELAY_MILLIS =
            {1000L, 2000L, 4000L};

    private final ICd15ScheduleResultService scheduleResultService;
    private final Cd15ScheduleResultIssueAssembler issueAssembler;
    private final Cd15ScheduleExecutionGuard scheduleExecutionGuard;
    private final IMesItfService mesItfService;

    @Override
    public AjaxResult publish(Cd15ScheduleResult request, String ids) {
        if (request == null || request.getScheduleDate() == null
                || StringUtils.isBlank(request.getFactoryCode())) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.publish.invalidRequest"));
        }
        Date scheduleDate = request.getScheduleDate();
        String factoryCode = request.getFactoryCode();
        String traceId = UUID.randomUUID().toString();
        log.info("斜裁排程发布开始, traceId={}, factoryCode={}, "
                        + "scheduleDate={}, ids={}",
                traceId, factoryCode, scheduleDate, ids);

        List<Cd15ScheduleResult> allResults =
                scheduleResultService.selectByDateAndFactory(
                        scheduleDate, factoryCode);
        if (allResults.isEmpty()) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.publish.noEligibleData"));
        }

        List<Cd15ScheduleResult> selectedResults;
        try {
            selectedResults = this.filterByIds(allResults, ids);
            selectedResults = this.expandSplitGroups(
                    allResults, selectedResults);
        } catch (IllegalArgumentException exception) {
            return AjaxResult.error(exception.getMessage());
        }
        if (selectedResults.isEmpty()) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.publish.noEligibleData"));
        }

        Predicate<Cd15ScheduleResult> publishablePredicate =
                this::isPublishable;
        List<Cd15ScheduleResult> publishable = selectedResults.stream()
                .filter(publishablePredicate)
                .collect(Collectors.toList());
        if (publishable.isEmpty()) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.publish.noEligibleData"));
        }
        if (this.hasSplitStatusConflict(selectedResults, publishable)) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.publish.splitStatusConflict"));
        }
        List<Cd15ScheduleResult> invalidMachines = publishable.stream()
                .filter(result -> StringUtils.isBlank(result.getMachineCode())
                        || result.getMachineCode().contains(","))
                .collect(Collectors.toList());
        if (!invalidMachines.isEmpty()) {
            log.warn("斜裁排程发布机台校验失败, traceId={}, values={}",
                    traceId, invalidMachines.stream()
                            .map(result -> "ID=" + result.getId()
                                    + ",machineCode=" + result.getMachineCode())
                            .collect(Collectors.joining(";")));
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.publish.invalidMachine"));
        }

        String lockToken = scheduleExecutionGuard.acquireIssueLock(
                factoryCode, scheduleDate);
        if (lockToken == null) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.publish.inProgress"));
        }
        try {
            List<Cd15ScheduleResultIssue> issueList =
                    issueAssembler.assemble(publishable, scheduleDate,
                            factoryCode, traceId);
            if (issueList.isEmpty()) {
                return AjaxResult.error(I18nUtil.getMessage(
                        "ui.cd15.publish.noIssueData"));
            }
            scheduleResultService.batchUpdateReleaseStatus(
                    publishable, ApsConstant.RELEASING);
            return this.issueWithRetry(
                    publishable, issueList, traceId);
        } catch (Exception exception) {
            log.error("斜裁排程发布异常, traceId={}", traceId, exception);
            this.markFailure(publishable, traceId);
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.publish.failed"));
        } finally {
            scheduleExecutionGuard.releaseIssueLock(
                    factoryCode, scheduleDate, lockToken);
        }
    }

    /** 调用 MES，并对网络或服务端异常执行指数退避重试。 */
    private AjaxResult issueWithRetry(
            List<Cd15ScheduleResult> publishable,
            List<Cd15ScheduleResultIssue> issueList,
            String traceId) {
        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                AjaxResult issueResult = FeignTokenHelper.callWithToken(
                        () -> mesItfService.issueCd15ScheduleResult(issueList));
                if (issueResult != null && Objects.equals(
                        HttpStatus.SUCCESS,
                        issueResult.get(AjaxResult.CODE_TAG))) {
                    scheduleResultService.batchUpdateReleaseStatus(
                            publishable, ApsConstant.IS_RELEASE);
                    log.info("斜裁排程发布成功, traceId={}, attempt={}, resultCount={}",
                            traceId, retryCount + 1, publishable.size());
                    return AjaxResult.success(I18nUtil.getMessage(
                            "ui.cd15.publish.success"));
                }
                if (this.isBusinessError(issueResult)) {
                    scheduleResultService.batchUpdateReleaseStatus(
                            publishable, ApsConstant.FAILURE_RELEASE);
                    String message = issueResult.get(AjaxResult.MSG_TAG) == null
                            ? I18nUtil.getMessage("ui.cd15.publish.failed")
                            : String.valueOf(issueResult.get(
                                    AjaxResult.MSG_TAG));
                    return AjaxResult.error(message);
                }
                retryCount++;
                log.warn("斜裁排程发布失败，准备重试, traceId={}, attempt={}, result={}",
                        traceId, retryCount, issueResult);
                this.sleepBeforeRetry(retryCount);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                log.error("斜裁排程发布重试被中断, traceId={}", traceId, exception);
                break;
            } catch (Exception exception) {
                retryCount++;
                log.error("斜裁排程发布调用异常, traceId={}, attempt={}",
                        traceId, retryCount, exception);
                try {
                    this.sleepBeforeRetry(retryCount);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        scheduleResultService.batchUpdateReleaseStatus(
                publishable, ApsConstant.FAILURE_RELEASE);
        return AjaxResult.error(I18nUtil.getMessage(
                "ui.cd15.publish.failed"));
    }

    /** 重试前等待；最后一次失败后不再等待。 */
    private void sleepBeforeRetry(int retryCount)
            throws InterruptedException {
        if (retryCount < MAX_RETRIES) {
            Thread.sleep(RETRY_DELAY_MILLIS[retryCount - 1]);
        }
    }

    /** 发布异常后尽力恢复为发布失败状态。 */
    private void markFailure(List<Cd15ScheduleResult> results,
                             String traceId) {
        try {
            scheduleResultService.batchUpdateReleaseStatus(
                    results, ApsConstant.FAILURE_RELEASE);
        } catch (Exception statusException) {
            log.error("斜裁排程发布失败状态回写异常, traceId={}",
                    traceId, statusException);
        }
    }

    /** 按页面选中主键过滤结果。 */
    private List<Cd15ScheduleResult> filterByIds(
            List<Cd15ScheduleResult> allResults, String ids) {
        if (StringUtils.isBlank(ids)) {
            return new ArrayList<>(allResults);
        }
        Set<Long> idSet;
        try {
            idSet = Arrays.stream(ids.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(Long::valueOf)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(I18nUtil.getMessage(
                    "ui.cd15.publish.invalidIds"), exception);
        }
        return allResults.stream()
                .filter(result -> idSet.contains(result.getId()))
                .collect(Collectors.toList());
    }

    /**
     * 页面选中分裁任一条时自动补齐同组另一条，并校验组合完整性。
     */
    private List<Cd15ScheduleResult> expandSplitGroups(
            List<Cd15ScheduleResult> allResults,
            List<Cd15ScheduleResult> selectedResults) {
        Map<Long, Cd15ScheduleResult> expanded = new LinkedHashMap<>();
        selectedResults.forEach(result -> expanded.put(result.getId(), result));
        List<Cd15ScheduleResult> selectedSnapshot =
                new ArrayList<>(selectedResults);
        for (Cd15ScheduleResult selected : selectedSnapshot) {
            if (!this.isSplit(selected)) {
                continue;
            }
            if (StringUtils.isBlank(selected.getGroupNo())) {
                throw new IllegalArgumentException(I18nUtil.getMessage(
                        "ui.cd15.publish.splitGroupInvalid"));
            }
            List<Cd15ScheduleResult> group = allResults.stream()
                    .filter(this::isSplit)
                    .filter(result -> Objects.equals(
                            selected.getFactoryCode(), result.getFactoryCode()))
                    .filter(result -> Objects.equals(
                            selected.getScheduleDate(), result.getScheduleDate()))
                    .filter(result -> Objects.equals(
                            selected.getGroupNo(), result.getGroupNo()))
                    .collect(Collectors.toList());
            if (!this.isValidSplitGroup(group)) {
                throw new IllegalArgumentException(I18nUtil.getMessage(
                        "ui.cd15.publish.splitGroupInvalid"));
            }
            group.forEach(result -> expanded.put(result.getId(), result));
        }
        return new ArrayList<>(expanded.values());
    }

    /** 校验分裁两条结果共用机台、工单、大卷和角度。 */
    private boolean isValidSplitGroup(List<Cd15ScheduleResult> group) {
        if (group.size() == 1) {
            return true;
        }
        if (group.size() != 2
                || group.stream().map(Cd15ScheduleResult::getSteelStripCode)
                .filter(StringUtils::isNotBlank).distinct().count() != 2L) {
            return false;
        }
        Cd15ScheduleResult first = group.get(0);
        return group.stream().allMatch(result ->
                Objects.equals(first.getOrderNo(), result.getOrderNo())
                        && Objects.equals(first.getMachineCode(),
                                result.getMachineCode())
                        && Objects.equals(first.getBigRollCode(),
                                result.getBigRollCode())
                        && Objects.equals(first.getCuttingAngle(),
                                result.getCuttingAngle()));
    }

    /**
     * 同一分裁组只能两条一起发布；两条均不可发布时整体忽略。
     */
    private boolean hasSplitStatusConflict(
            List<Cd15ScheduleResult> selectedResults,
            List<Cd15ScheduleResult> publishable) {
        Set<Long> publishableIds = publishable.stream()
                .map(Cd15ScheduleResult::getId)
                .collect(Collectors.toSet());
        Map<String, List<Cd15ScheduleResult>> groups =
                selectedResults.stream()
                        .filter(this::isSplit)
                        .collect(Collectors.groupingBy(this::splitGroupKey));
        return groups.values().stream().anyMatch(group -> {
            long publishableCount = group.stream()
                    .filter(result -> publishableIds.contains(result.getId()))
                    .count();
            return publishableCount != 0L
                    && publishableCount != group.size();
        });
    }

    /** 是否属于可发布状态。 */
    private boolean isPublishable(Cd15ScheduleResult result) {
        return ApsConstant.NO_RELEASE.equals(result.getReleaseStatus())
                || ApsConstant.FAILURE_RELEASE.equals(
                        result.getReleaseStatus())
                || ApsConstant.TIMEOUT_FAILURE.equals(
                        result.getReleaseStatus())
                || ApsConstant.WAIT_RELEASING.equals(
                        result.getReleaseStatus());
    }

    /** 是否分裁结果。 */
    private boolean isSplit(Cd15ScheduleResult result) {
        return result != null
                && "SPLIT".equalsIgnoreCase(result.getCutMode());
    }

    /** 构造分裁组键。 */
    private String splitGroupKey(Cd15ScheduleResult result) {
        return String.valueOf(result.getFactoryCode()) + "|"
                + String.valueOf(result.getScheduleDate()) + "|"
                + String.valueOf(result.getGroupNo());
    }

    /** 判断 MES 返回是否为明确业务错误。 */
    private boolean isBusinessError(AjaxResult result) {
        return result != null
                && !Objects.equals(HttpStatus.SUCCESS,
                        result.get(AjaxResult.CODE_TAG))
                && result.get(AjaxResult.MSG_TAG) != null;
    }
}
