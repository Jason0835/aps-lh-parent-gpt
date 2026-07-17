package com.zlt.aps.tc.service;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 胎侧发布反馈事务应用结果。
 */
@Getter
public class FeedbackApplyResult {

    /** 受影响任务ID。 */
    private final Set<String> taskIdSet = new LinkedHashSet<>();

    /** 处理摘要。 */
    private final Map<String, Object> summary = new LinkedHashMap<>();

    /** 实际应用数量。 */
    private int appliedCount;

    /** 重复反馈数量。 */
    private int duplicateCount;

    /** 忽略数量。 */
    private int ignoredCount;

    /** 实际应用数量加一。 */
    public void incrementAppliedCount() {
        this.appliedCount++;
    }

    /** 重复反馈数量加一。 */
    public void incrementDuplicateCount() {
        this.duplicateCount++;
    }

    /** 忽略数量加一。 */
    public void incrementIgnoredCount() {
        this.ignoredCount++;
    }

    /** 构造对外摘要。 */
    public void finishSummary() {
        this.summary.put("schemaVersion", 1);
        this.summary.put("appliedCount", this.appliedCount);
        this.summary.put("duplicateCount", this.duplicateCount);
        this.summary.put("ignoredCount", this.ignoredCount);
    }
}
