package com.zlt.aps.cx.service.impl.validation;

import com.ruoyi.common.i18n.utils.I18nUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class ScheduleDataValidationResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean passed = true;

    private String summary;

    private int errorCount;

    private int warnCount;

    private int infoCount;

    private List<ValidationDetail> details = new ArrayList<>();

    public ScheduleDataValidationResult() {
    }

    public void addError(String dataItem, String message, String suggestion) {
        addDetail(ValidationLevel.ERROR, dataItem, message, suggestion);
    }

    public void addWarn(String dataItem, String message, String suggestion) {
        addDetail(ValidationLevel.WARN, dataItem, message, suggestion);
    }

    public void addInfo(String dataItem, String message, String suggestion) {
        addDetail(ValidationLevel.INFO, dataItem, message, suggestion);
    }

    public void addDetail(ValidationLevel level, String dataItem, String message, String suggestion) {
        ValidationDetail detail = new ValidationDetail();
        detail.setLevel(level);
        detail.setDataItem(dataItem);
        detail.setMessage(message);
        detail.setSuggestion(suggestion);
        this.details.add(detail);

        switch (level) {
            case ERROR:
                this.errorCount++;
                this.passed = false;
                break;
            case WARN:
                this.warnCount++;
                break;
            case INFO:
                this.infoCount++;
                break;
        }
    }

    public String generateSummary() {
        StringBuilder sb = new StringBuilder();
        if (passed) {
            sb.append(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.summary.passed"));
        } else {
            sb.append(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.summary.notPassed"));
        }
        if (errorCount > 0) {
            sb.append(StrUtil.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.summary.errorCount"), errorCount));
        }
        if (warnCount > 0) {
            sb.append(StrUtil.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.summary.warnCount"), warnCount));
        }
        if (infoCount > 0) {
            sb.append(StrUtil.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.summary.infoCount"), infoCount));
        }
        this.summary = sb.toString();
        return this.summary;
    }

    @Data
    public static class ValidationDetail implements Serializable {
        private static final long serialVersionUID = 1L;

        private ValidationLevel level;

        private String dataItem;

        private String message;

        private String suggestion;
    }

    public enum ValidationLevel {
        ERROR,
        WARN,
        INFO
    }
}
