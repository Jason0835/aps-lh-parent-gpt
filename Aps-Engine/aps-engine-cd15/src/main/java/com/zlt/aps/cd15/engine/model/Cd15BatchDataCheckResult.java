package com.zlt.aps.cd15.engine.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 斜裁自动排程批次级数据检查结果。
 */
public class Cd15BatchDataCheckResult {

    private final boolean failed;
    private final List<CheckError> errors;
    private final List<CheckError> warnings;

    private Cd15BatchDataCheckResult(boolean failed, List<CheckError> errors, List<CheckError> warnings) {
        this.failed = failed;
        this.errors = errors == null ? Collections.emptyList() : new ArrayList<>(errors);
        this.warnings = warnings == null ? Collections.emptyList() : new ArrayList<>(warnings);
    }

    public static Cd15BatchDataCheckResult ok() {
        return new Cd15BatchDataCheckResult(false, Collections.emptyList(), Collections.emptyList());
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

    /** 返回按字段聚合后的主错误消息，便于自动排程入口同步提示。 */
    public String getPrimaryMessage() {
        if (errors.isEmpty()) {
            return "";
        }
        Map<String, List<CheckError>> byField = errors.stream()
                .collect(Collectors.groupingBy(CheckError::getField,
                        LinkedHashMap::new, Collectors.toList()));
        return byField.entrySet().stream()
                .map(entry -> {
                    String field = entry.getKey();
                    int count = entry.getValue().size();
                    String firstMessage = entry.getValue().get(0).getMessage();
                    if (count == 1) {
                        return field + ": " + firstMessage;
                    }
                    return field + "(" + count + "项): " + firstMessage;
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

        public Cd15BatchDataCheckResult build() {
            return new Cd15BatchDataCheckResult(!errors.isEmpty(), errors, warnings);
        }
    }

    /** 单条检查错误或告警。 */
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