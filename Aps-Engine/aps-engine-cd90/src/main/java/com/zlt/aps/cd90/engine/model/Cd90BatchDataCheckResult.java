package com.zlt.aps.cd90.engine.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 直裁自动排程批次级数据检查结果。
 * <p>
 * 用于在正式进入自动排程前同步返回公共数据缺失或非法的结构化提示，
 * 不进入异步任务、不创建PENDING记录、不占用执行锁。
 * 规格级失败（施工缺失、大卷绑定缺失等）不在此处收集，仍由排程算法写入未排结果。
 * </p>
 */
public class Cd90BatchDataCheckResult {

    private final boolean failed;
    private final List<CheckError> errors;
    private final List<CheckError> warnings;

    private Cd90BatchDataCheckResult(boolean failed, List<CheckError> errors, List<CheckError> warnings) {
        this.failed = failed;
        this.errors = errors == null ? Collections.emptyList() : new ArrayList<>(errors);
        this.warnings = warnings == null ? Collections.emptyList() : new ArrayList<>(warnings);
    }

    public static Cd90BatchDataCheckResult ok() {
        return new Cd90BatchDataCheckResult(false, Collections.emptyList(), Collections.emptyList());
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isFailed() {
        return failed;
    }

    public List<CheckError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public List<CheckError> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    /** 主错误信息，用于AjaxResult.success(msg, data)的简短提示；无错误时返回空串。按field分组聚合。 */
    public String getPrimaryMessage() {
        if (errors.isEmpty()) {
            return "";
        }
        // 按 field 分组聚合，同一 field 多条时显示条数
        Map<String, List<CheckError>> byField = errors.stream()
                .collect(Collectors.groupingBy(CheckError::getField,
                        LinkedHashMap::new, Collectors.toList()));
        return byField.entrySet().stream()
                .map(entry -> {
                    String field = entry.getKey();
                    int count = entry.getValue().size();
                    String firstMsg = entry.getValue().get(0).getMessage();
                    if (count == 1) {
                        return field + ": " + firstMsg;
                    }
                    return field + "(" + count + "项): " + firstMsg;
                })
                .collect(Collectors.joining("; "));
    }

    public static class Builder {
        private final List<CheckError> errors = new ArrayList<>();
        private final List<CheckError> warnings = new ArrayList<>();

        public Builder addError(String field, String reasonCode, String message) {
            this.errors.add(new CheckError(field, reasonCode, message, null));
            return this;
        }

        public Builder addError(String field, String reasonCode, String message, String suggestion) {
            this.errors.add(new CheckError(field, reasonCode, message, suggestion));
            return this;
        }

        public Builder addWarning(String field, String reasonCode, String message) {
            this.warnings.add(new CheckError(field, reasonCode, message, null));
            return this;
        }

        public Builder addWarning(String field, String reasonCode, String message, String suggestion) {
            this.warnings.add(new CheckError(field, reasonCode, message, suggestion));
            return this;
        }

        public Cd90BatchDataCheckResult build() {
            return new Cd90BatchDataCheckResult(!errors.isEmpty(), errors, warnings);
        }
    }

    /**
     * 单条检查错误/告警。
     */
    public static class CheckError {
        private final String field;
        private final String reasonCode;
        private final String message;
        private final String suggestion;

        public CheckError(String field, String reasonCode, String message, String suggestion) {
            this.field = field;
            this.reasonCode = reasonCode;
            this.message = message;
            this.suggestion = suggestion;
        }

        public String getField() {
            return field;
        }

        public String getReasonCode() {
            return reasonCode;
        }

        public String getMessage() {
            return message;
        }

        public String getSuggestion() {
            return suggestion;
        }
    }
}
