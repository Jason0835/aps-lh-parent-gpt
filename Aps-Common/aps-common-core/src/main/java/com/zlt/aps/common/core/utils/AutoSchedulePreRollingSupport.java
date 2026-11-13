package com.zlt.aps.common.core.utils;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.domain.vo.AutoScheduleIssueVo;
import com.zlt.aps.common.core.domain.vo.AutoSchedulePreRollingResult;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 胎面、胎侧自动排程前置滚动的公共状态转换工具。
 */
public final class AutoSchedulePreRollingSupport {

    /**
     * 前置滚动问题所属阶段编码。
     */
    public static final String STAGE_CODE = "PRE_AUTO_ROLLING";

    /**
     * 前置滚动问题类别编码。
     */
    public static final String ISSUE_CATEGORY = "PRE_AUTO_ROLLING";

    private AutoSchedulePreRollingSupport() {
    }

    /**
     * 将现有自动滚动轻量响应转换为自动排程门禁结果。
     *
     * @param targetScheduleDate 目标早班归属排程日期
     * @param rollingResponse    现有主侧自动滚动响应
     * @return 含技术状态、业务状态、放行结论及提示的问题结果
     */
    public static AutoSchedulePreRollingResult fromResponse(LocalDate targetScheduleDate,
                                                            Map<String, Object> rollingResponse) {
        AutoSchedulePreRollingResult result = new AutoSchedulePreRollingResult();
        result.setEnabled(true);
        result.setTargetScheduleDate(targetScheduleDate);
        result.setTargetShiftOrder(3);
        if (rollingResponse == null) {
            result.setStatus("FAILED");
            result.setExecutionState("FAILED");
            return block(result, "ui.autoRolling.preAutoPlan.emptyResponse",
                    new Object[]{targetScheduleDate, "-"});
        }
        result.setExecutionId(asText(rollingResponse.get("executionId")));
        result.setExecutionState(asText(rollingResponse.get("executionState")));
        result.setStatus(asText(rollingResponse.get("status")));

        if ("FAILED".equals(result.getExecutionState())) {
            String detail = resolveFailureDetail(rollingResponse,
                    fallback(result.getStatus(), "FAILED"));
            return block(result, "ui.autoRolling.preAutoPlan.failed",
                    new Object[]{targetScheduleDate, detail, fallbackExecutionId(result.getExecutionId())});
        }
        if (!"COMPLETED".equals(result.getExecutionState())) {
            return block(result, "ui.autoRolling.preAutoPlan.incompleteExecution",
                    new Object[]{targetScheduleDate, fallbackExecutionId(result.getExecutionId()),
                            fallback(result.getExecutionState(), "UNKNOWN")});
        }
        if ("NO_ACTIVE_SCHEDULE_BATCH".equals(asText(rollingResponse.get("reasonCode")))) {
            return block(result, "ui.autoRolling.preAutoPlan.failed",
                    new Object[]{targetScheduleDate,
                            I18nUtil.getMessage("ui.autoRolling.preAutoPlan.noActiveBatch"),
                            fallbackExecutionId(result.getExecutionId())});
        }
        if ("SUCCESS".equals(result.getStatus()) || "SKIPPED".equals(result.getStatus())) {
            result.setAllowedToContinue(true);
            result.setMessage(format("ui.autoRolling.preAutoPlan.completed",
                    targetScheduleDate, fallbackExecutionId(result.getExecutionId())));
            return result;
        }
        if ("PARTIAL_SUCCESS".equals(result.getStatus())) {
            return partial(result, rollingResponse);
        }
        if ("FAILED".equals(result.getStatus()) || "DATA_BLOCKED".equals(result.getStatus())) {
            String detail = resolveFailureDetail(rollingResponse, result.getStatus());
            return block(result, "ui.autoRolling.preAutoPlan.failed",
                    new Object[]{targetScheduleDate, detail, fallbackExecutionId(result.getExecutionId())});
        }
        return block(result, "ui.autoRolling.preAutoPlan.unknownStatus",
                new Object[]{targetScheduleDate, fallbackExecutionId(result.getExecutionId()),
                        fallback(result.getStatus(), "UNKNOWN")});
    }

    /**
     * 将窗口解析失败转换为阻断结果。
     *
     * @param result     当前前置滚动结果
     * @param reasonCode 班次映射失败原因码
     * @return 已包含 ERROR 问题的阻断结果
     */
    public static AutoSchedulePreRollingResult invalidWindow(AutoSchedulePreRollingResult result,
                                                             String reason) {
        result.setEnabled(true);
        result.setStatus("DATA_BLOCKED");
        result.setExecutionState("NOT_STARTED");
        String detail = format("ui.autoRolling.preAutoPlan.invalidWindow", reason);
        return setBlocked(result, format("ui.autoRolling.preAutoPlan.failed",
                result.getTargetScheduleDate(), detail, "-"));
    }

    /**
     * 将技术异常转换为阻断结果；调用方需另行记录原始异常堆栈。
     *
     * @param result 当前前置滚动结果
     * @return 已包含 ERROR 问题的阻断结果
     */
    public static AutoSchedulePreRollingResult technicalFailure(AutoSchedulePreRollingResult result) {
        result.setEnabled(true);
        result.setStatus("FAILED");
        result.setExecutionState("FAILED");
        return setBlocked(result, format("ui.autoRolling.preAutoPlan.failed",
                result.getTargetScheduleDate(),
                I18nUtil.getMessage("ui.autoRolling.preAutoPlan.technicalFailure"),
                fallbackExecutionId(result.getExecutionId())));
    }

    /**
     * 合并模板尚未创建时的失败问题和前置滚动问题。
     *
     * @param baseIssues       自动排程本身的失败问题
     * @param preRollingResult 前置滚动结果
     * @return 去重后的任务问题清单
     */
    public static List<AutoScheduleIssueVo> mergeIssues(List<AutoScheduleIssueVo> baseIssues,
                                                        AutoSchedulePreRollingResult preRollingResult) {
        List<AutoScheduleIssueVo> merged = new ArrayList<>();
        if (preRollingResult != null && preRollingResult.getIssues() != null) {
            preRollingResult.getIssues().stream().filter(Objects::nonNull).forEach(merged::add);
        }
        if (baseIssues == null) {
            return merged;
        }
        baseIssues.stream()
                .filter(Objects::nonNull)
                .filter(issue -> merged.stream().noneMatch(existing -> sameIssue(existing, issue)))
                .forEach(merged::add);
        return merged;
    }

    /**
     * 构造显示目标早班的任务阶段文字。
     *
     * @param targetScheduleDate 目标早班归属排程日期
     * @return 当前语言的阶段名称
     */
    public static String progressStage(LocalDate targetScheduleDate) {
        return format("ui.autoRolling.preAutoPlan.progress", targetScheduleDate);
    }

    /**
     * 前置失败尚无滚动执行编号时，在任务问题中补充自动排程任务编号。
     *
     * @param result 已转换的前置滚动结果
     * @param taskId 自动排程后台任务编号
     * @return 带有可定位任务编号的前置滚动结果
     */
    public static AutoSchedulePreRollingResult withTaskReference(AutoSchedulePreRollingResult result, String taskId) {
        if (result == null || result.isAllowedToContinue() || result.getExecutionId() != null
                || taskId == null || taskId.trim().isEmpty()) {
            return result;
        }
        String message = result.getMessage() + " "
                + format("ui.autoRolling.preAutoPlan.taskReference", taskId);
        result.setMessage(message);
        if (result.getIssues() != null) {
            result.getIssues().stream().filter(Objects::nonNull)
                    .filter(issue -> STAGE_CODE.equals(issue.getStageCode()))
                    .forEach(issue -> issue.setMessage(message));
        }
        return result;
    }

    /**
     * 构造可继续排程的部分成功警告。
     *
     * @param result          当前滚动结果
     * @param rollingResponse 主侧自动滚动响应
     * @return 保留WARN并允许排程继续的结果
     */
    private static AutoSchedulePreRollingResult partial(AutoSchedulePreRollingResult result,
                                                        Map<String, Object> rollingResponse) {
        Object reasonSummary = rollingResponse.get("reasonSummary");
        Object gapQty = rollingResponse.get("gapQty");
        String reasonText = formatReasonSummary(reasonSummary);
        result.setAllowedToContinue(true);
        if (hasPositiveGap(gapQty)) {
            result.setMessage(format("ui.autoRolling.preAutoPlan.partial",
                    result.getTargetScheduleDate(), reasonText, String.valueOf(gapQty),
                    fallbackExecutionId(result.getExecutionId())));
        } else {
            result.setMessage(format("ui.autoRolling.preAutoPlan.partialNoGap",
                    result.getTargetScheduleDate(), reasonText, fallbackExecutionId(result.getExecutionId())));
        }
        result.getIssues().add(buildIssue("WARN", result.getMessage()));
        return result;
    }

    /**
     * 将汇总原因编码转换为可读的国际化说明，并保留未知编码用于定位。
     *
     * @param reasonSummary 自动滚动返回的原因汇总
     * @return 可展示的原因摘要
     */
    private static String formatReasonSummary(Object reasonSummary) {
        if (reasonSummary instanceof Map) {
            Map<?, ?> summaryMap = (Map<?, ?>) reasonSummary;
            if (summaryMap.isEmpty()) {
                return I18nUtil.getMessage("ui.autoRolling.preAutoPlan.partialReasonUnknown");
            }
            return summaryMap.entrySet().stream()
                    .map(entry -> format("ui.autoRolling.preAutoPlan.unresolvedReasonItem",
                            translateReasonCode(asText(entry.getKey())), asText(entry.getValue())))
                    .collect(Collectors.joining("; "));
        }
        String summaryText = asText(reasonSummary);
        if (summaryText == null || summaryText.trim().isEmpty()) {
            return I18nUtil.getMessage("ui.autoRolling.preAutoPlan.partialReasonUnknown");
        }
        if (summaryText.matches("[A-Z0-9_]+")) {
            return translateReasonCode(summaryText);
        }
        return summaryText;
    }

    /**
     * 判断响应中的汇总缺口是否为正数，避免对无数量缺口的资料问题显示虚构数量。
     *
     * @param gapQty 自动滚动响应中的汇总缺口
     * @return 缺口为正数时返回true
     */
    private static boolean hasPositiveGap(Object gapQty) {
        if (gapQty == null) {
            return false;
        }
        try {
            return BigDecimalUtils.valueOf(gapQty).compareTo(BigDecimal.ZERO) > 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    /**
     * 使用指定国际化模板阻断自动排程。
     *
     * @param result     当前滚动结果
     * @param messageKey 国际化消息键
     * @param arguments  国际化格式参数
     * @return 已包含ERROR问题的阻断结果
     */
    private static AutoSchedulePreRollingResult block(AutoSchedulePreRollingResult result, String messageKey,
                                                      Object[] arguments) {
        return setBlocked(result, format(messageKey, arguments));
    }

    /**
     * 将滚动结果标记为阻断并添加前置阶段ERROR问题。
     *
     * @param result  当前滚动结果
     * @param message 面向用户的失败说明
     * @return 已设置阻断结论的结果
     */
    private static AutoSchedulePreRollingResult setBlocked(AutoSchedulePreRollingResult result, String message) {
        result.setAllowedToContinue(false);
        result.setMessage(message);
        result.getIssues().add(buildIssue("ERROR", message));
        return result;
    }

    /**
     * 创建统一归属昨日早班阶段的问题对象。
     *
     * @param level   问题级别
     * @param message 问题说明
     * @return 自动排程问题对象
     */
    private static AutoScheduleIssueVo buildIssue(String level, String message) {
        AutoScheduleIssueVo issue = new AutoScheduleIssueVo();
        issue.setLevel(level);
        issue.setStageCode(STAGE_CODE);
        issue.setStageName(I18nUtil.getMessage("ui.autoRolling.preAutoPlan.stageName"));
        issue.setCategory(ISSUE_CATEGORY);
        issue.setShiftOrder(3);
        issue.setMessage(message);
        issue.setSuggestion(I18nUtil.getMessage("ui.autoRolling.preAutoPlan.suggestion"));
        return issue;
    }

    /**
     * 从现有轻量滚动响应中提取业务失败原因。
     *
     * @param rollingResponse 主侧自动滚动响应
     * @param status          自动滚动业务状态
     * @return 已本地化的主要失败说明
     */
    private static String resolveFailureDetail(Map<String, Object> rollingResponse, String status) {
        String reasonCode = asText(rollingResponse.get("reasonCode"));
        if ("NO_ACTIVE_SCHEDULE_BATCH".equals(reasonCode)) {
            return I18nUtil.getMessage("ui.autoRolling.preAutoPlan.noActiveBatch");
        }
        String message = asText(rollingResponse.get("message"));
        if (reasonCode != null) {
            String reason = translateReasonCode(reasonCode);
            if (message != null && !reason.equals(reasonCode)) {
                return reason + ": " + message;
            }
            return reason;
        }
        if (message != null && !message.trim().isEmpty()) {
            return message;
        }
        String reasonSummary = formatReasonSummary(rollingResponse.get("reasonSummary"));
        return format("ui.autoRolling.preAutoPlan.statusDetail", translateBusinessStatus(status), reasonSummary);
    }

    /**
     * 将滚动核心稳定原因码映射为国际化业务说明。
     *
     * @param reasonCode 自动滚动原因编码
     * @return 已翻译原因说明；未知编码使用通用说明并保留原码
     */
    private static String translateReasonCode(String reasonCode) {
        if (reasonCode == null || reasonCode.trim().isEmpty()) {
            return I18nUtil.getMessage("ui.autoRolling.preAutoPlan.partialReasonUnknown");
        }
        String messageKey;
        switch (reasonCode) {
            case "BUSINESS_NO_CAPACITY":
                messageKey = "ui.autoRolling.preAutoPlan.reasonNoCapacity";
                break;
            case "NO_ELIGIBLE_MACHINE":
                messageKey = "ui.autoRolling.preAutoPlan.reasonNoMachine";
                break;
            case "BUSINESS_NOT_ADJUSTABLE":
                messageKey = "ui.autoRolling.preAutoPlan.reasonNotAdjustable";
                break;
            case "WORK_ORDER_COMPLETED":
                messageKey = "ui.autoRolling.preAutoPlan.reasonWorkOrderCompleted";
                break;
            case "SYSTEM_ERROR":
                return I18nUtil.getMessage("ui.autoRolling.preAutoPlan.technicalFailure");
            default:
                return format("ui.autoRolling.preAutoPlan.unknownReason", reasonCode);
        }
        return I18nUtil.getMessage(messageKey);
    }

    /**
     * 将自动滚动业务终态转换为国际化说明。
     *
     * @param status 自动滚动业务状态
     * @return 已翻译的业务状态说明
     */
    private static String translateBusinessStatus(String status) {
        if ("FAILED".equals(status)) {
            return I18nUtil.getMessage("ui.autoRolling.preAutoPlan.statusFailed");
        }
        if ("DATA_BLOCKED".equals(status)) {
            return I18nUtil.getMessage("ui.autoRolling.preAutoPlan.statusDataBlocked");
        }
        return I18nUtil.getMessage("ui.autoRolling.preAutoPlan.statusUnknown");
    }

    /**
     * 使用项目国际化资源和MessageFormat格式化提示。
     *
     * @param key       国际化消息键
     * @param arguments 格式参数
     * @return 当前语言的格式化消息
     */
    private static String format(String key, Object... arguments) {
        return MessageFormat.format(I18nUtil.getMessage(key), arguments);
    }

    /**
     * 将响应值安全转换为文本。
     *
     * @param value 响应字段值
     * @return 文本值；空值返回null
     */
    private static String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 将空白文本替换为指定缺省值。
     *
     * @param value        原始文本
     * @param defaultValue 缺省文本
     * @return 非空文本或缺省值
     */
    private static String fallback(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }

    /**
     * 为未取得滚动编号的错误提供可显示占位符。
     *
     * @param executionId 自动滚动执行编号
     * @return 执行编号或短横线
     */
    private static String fallbackExecutionId(String executionId) {
        return fallback(executionId, "-");
    }

    /**
     * 比较前置错误和常规失败问题是否重复。
     *
     * @param first  已有问题
     * @param second 待合并问题
     * @return 级别和说明均相同时返回true
     */
    private static boolean sameIssue(AutoScheduleIssueVo first, AutoScheduleIssueVo second) {
        return Objects.equals(first.getLevel(), second.getLevel())
                && Objects.equals(first.getStageCode(), second.getStageCode())
                && Objects.equals(first.getCategory(), second.getCategory())
                && Objects.equals(first.getSourceOrderNo(), second.getSourceOrderNo())
                && Objects.equals(first.getEmbryoCode(), second.getEmbryoCode())
                && Objects.equals(first.getProcessCode(), second.getProcessCode())
                && Objects.equals(first.getMachineCode(), second.getMachineCode())
                && Objects.equals(first.getRecipeNo(), second.getRecipeNo())
                && Objects.equals(first.getShiftOrder(), second.getShiftOrder())
                && Objects.equals(first.getMessage(), second.getMessage());
    }
}
