package com.zlt.aps.cx.vo;

/**
 * 校验明细（用于API返回）
 *
 * @author APS Team
 */
public class ValidationDetail {
    private String dataItem;
    private String message;
    private String suggestion;

    public ValidationDetail() {}

    public ValidationDetail(String dataItem, String message, String suggestion) {
        this.dataItem = dataItem;
        this.message = message;
        this.suggestion = suggestion;
    }

    public String getDataItem() { return dataItem; }
    public void setDataItem(String dataItem) { this.dataItem = dataItem; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
}
