package com.zlt.aps.common.engine.schedule.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TM/TC 自动排程公共异常收集器。
 *
 * <p>负责异常对象创建、追加、错误判断和副本返回；产品枚举和 API 映射由领域收集器处理。</p>
 */
public class ScheduleIssueCollector {

    /** 当前排程轮次收集的内部异常。 */
    private final List<ScheduleIssueModel> issues = new ArrayList<>();

    /**
     * 创建包含公共字段的异常对象。
     *
     * @param level 异常级别
     * @param stageCode 阶段编码
     * @param stageName 阶段名称
     * @param category 异常类别
     * @param message 异常说明
     * @return 已填充公共字段的异常对象
     */
    public ScheduleIssueModel createIssue(String level, String stageCode, String stageName,
                                          String category, String message) {
        ScheduleIssueModel issue = new ScheduleIssueModel();
        issue.setLevel(level);
        issue.setStageCode(stageCode);
        issue.setStageName(stageName);
        issue.setCategory(category);
        issue.setMessage(message);
        return issue;
    }

    /**
     * 追加一条内部异常。
     *
     * @param issue 待追加异常
     */
    public void append(ScheduleIssueModel issue) {
        if (issue != null) {
            this.issues.add(issue);
        }
    }

    /**
     * 判断指定阶段是否存在指定级别异常。
     *
     * @param errorLevel 错误级别编码
     * @param stageCode 阶段编码；为空时只按级别判断
     * @return 存在匹配异常时返回 true
     */
    public boolean hasError(String errorLevel, String stageCode) {
        return this.issues.stream().anyMatch(issue ->
                errorLevel != null && errorLevel.equals(issue.getLevel())
                        && (stageCode == null || stageCode.equals(issue.getStageCode())));
    }

    /**
     * 返回内部异常的浅拷贝列表。
     *
     * @return 异常副本；没有异常时返回不可变空列表
     */
    public List<ScheduleIssueModel> copyIssues() {
        if (this.issues.isEmpty()) {
            return Collections.emptyList();
        }
        return this.issues.stream().map(this::copy).collect(Collectors.toList());
    }

    /**
     * 复制单条异常，避免接口边界映射修改收集器内部对象。
     *
     * @param source 来源异常
     * @return 异常副本
     */
    private ScheduleIssueModel copy(ScheduleIssueModel source) {
        ScheduleIssueModel target = new ScheduleIssueModel();
        target.setLevel(source.getLevel());
        target.setStageCode(source.getStageCode());
        target.setStageName(source.getStageName());
        target.setCategory(source.getCategory());
        target.setSourceOrderNo(source.getSourceOrderNo());
        target.setEmbryoCode(source.getEmbryoCode());
        target.setProcessCode(source.getProcessCode());
        target.setRecipeNo(source.getRecipeNo());
        target.setShiftOrder(source.getShiftOrder());
        target.setFieldName(source.getFieldName());
        target.setMessage(source.getMessage());
        return target;
    }
}
