package com.zlt.aps.cx.vo;

import java.util.List;

public class ValidationSummary {
    private int errorCount;
    private int warningCount;
    private List<ValidationDetail> errors;
    private List<ValidationDetail> warnings;

    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
    public int getWarningCount() { return warningCount; }
    public void setWarningCount(int warningCount) { this.warningCount = warningCount; }
    public List<ValidationDetail> getErrors() { return errors; }
    public void setErrors(List<ValidationDetail> errors) { this.errors = errors; }
    public List<ValidationDetail> getWarnings() { return warnings; }
    public void setWarnings(List<ValidationDetail> warnings) { this.warnings = warnings; }

}
