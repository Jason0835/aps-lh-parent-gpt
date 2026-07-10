package com.zlt.aps.cx.vo;

import com.zlt.aps.cx.entity.schedule.CxScheduleResult;

import java.time.LocalDate;
import java.util.List;

/**
 * 排程执行结果
 *
 * @author APS Team
 */
public class ScheduleResult {
    /** 排程锁冲突错误码 */
    public final String ERROR_CODE_LOCK_CONFLICT = "LOCK_CONFLICT";

    private boolean success;
    private String message;
    private String errorCode;
    private LocalDate scheduleDate;
    private List<CxScheduleResult> results;
    private List<ValidationDetail> validationErrors;
    private List<ValidationDetail> validationWarnings;

    public ScheduleResult() {}

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public LocalDate getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(LocalDate scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public List<CxScheduleResult> getResults() {
        return results;
    }

    public void setResults(List<CxScheduleResult> results) {
        this.results = results;
    }

    public List<ValidationDetail> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<ValidationDetail> validationErrors) {
        this.validationErrors = validationErrors;
    }

    public List<ValidationDetail> getValidationWarnings() {
        return validationWarnings;
    }

    public void setValidationWarnings(List<ValidationDetail> validationWarnings) {
        this.validationWarnings = validationWarnings;
    }
}
