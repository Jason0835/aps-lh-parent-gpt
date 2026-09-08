package com.zlt.aps.common.engine.schedule.engine;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.domain.vo.AutoScheduleIssueVo;
import com.zlt.aps.common.engine.enums.ScheduleIssueCategoryEnum;
import com.zlt.aps.common.engine.enums.ScheduleIssueLevelEnum;
import com.zlt.aps.common.engine.enums.ScheduleStepEnum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TM/TC 自动排程公共异常收集器。
 *
 * <p>负责异常对象创建、追加、错误判断、运行态副本和公共接口对象转换。</p>
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
        issue.setSuggestion(this.resolveSuggestion(category));
        return issue;
    }

    /**
     * 记录施工匹配异常。
     *
     * @param level 异常级别
     * @param category 异常类别
     * @param sourceOrderNo 来源工单号
     * @param embryoCode 胎胚代码
     * @param recipeNo 示方书编号
     * @param shiftOrder 班次顺序
     * @param fieldName 字段名称
     * @param message 异常说明
     */
    public void addConstructionIssue(ScheduleIssueLevelEnum level, ScheduleIssueCategoryEnum category,
                                     String sourceOrderNo, String embryoCode, String recipeNo,
                                     Integer shiftOrder, String fieldName, String message) {
        ScheduleIssueModel issue = this.createIssue(level, ScheduleStepEnum.BOOTSTRAP, category, message);
        issue.setSourceOrderNo(sourceOrderNo);
        issue.setEmbryoCode(embryoCode);
        issue.setRecipeNo(recipeNo);
        issue.setShiftOrder(shiftOrder);
        issue.setFieldName(fieldName);
        this.append(issue);
    }

    /**
     * 记录通用异常。
     *
     * @param level 异常级别
     * @param stepEnum 排程阶段
     * @param category 异常类别
     * @param message 异常说明
     */
    public void addIssue(ScheduleIssueLevelEnum level, ScheduleStepEnum stepEnum,
                         ScheduleIssueCategoryEnum category, String message) {
        this.append(this.createIssue(level, stepEnum, category, message));
    }

    /**
     * 记录计划量汇总组生产属性冲突。
     *
     * @param processCode 产品工序编码
     * @param shiftOrder 班次顺序
     * @param message 冲突说明
     */
    public void addPlanGroupAttributeConflictIssue(String processCode, Integer shiftOrder, String message) {
        ScheduleIssueModel issue = this.createIssue(ScheduleIssueLevelEnum.ERROR,
                ScheduleStepEnum.PLAN_CALC, ScheduleIssueCategoryEnum.PLAN_GROUP_ATTRIBUTE_CONFLICT, message);
        issue.setProcessCode(processCode);
        issue.setShiftOrder(shiftOrder);
        issue.setFieldName("productionAttributes");
        this.append(issue);
    }

    /**
     * 指定阶段尚无阻断问题时记录失败明细。
     *
     * @param stepEnum 排程阶段
     * @param category 失败类别
     * @param message 前端可展示的失败消息
     */
    public void addFailureIssueIfAbsent(ScheduleStepEnum stepEnum,
                                        ScheduleIssueCategoryEnum category, String message) {
        if (this.hasErrorIssue(stepEnum)) {
            return;
        }
        this.addIssue(ScheduleIssueLevelEnum.ERROR, stepEnum, category, message);
    }

    /**
     * 判断指定阶段是否已经存在阻断问题。
     *
     * @param stepEnum 排程阶段
     * @return 存在阻断问题时返回 true
     */
    public boolean hasErrorIssue(ScheduleStepEnum stepEnum) {
        return stepEnum != null && this.hasError(ScheduleIssueLevelEnum.ERROR.getCode(), stepEnum.getCode());
    }

    /**
     * 判断当前排程是否已经存在阻断问题。
     *
     * @return 存在阻断问题时返回 true
     */
    public boolean hasErrorIssue() {
        return this.hasError(ScheduleIssueLevelEnum.ERROR.getCode(), null);
    }

    /**
     * 记录缺库存快照告警。
     *
     * @param processCode 产品工序编码
     * @param message 告警说明
     */
    public void addStockMissingIssue(String processCode, String message) {
        ScheduleIssueModel issue = this.createIssue(ScheduleIssueLevelEnum.WARN,
                ScheduleStepEnum.INVENTORY_PREDICT, ScheduleIssueCategoryEnum.STOCK_MISSING, message);
        issue.setProcessCode(processCode);
        issue.setFieldName("stockQty");
        this.append(issue);
    }

    /**
     * 获取自动排程异常明细副本。
     *
     * @return 自动排程异常明细
     */
    public List<AutoScheduleIssueVo> getIssues() {
        return this.copyIssues().stream().map(this::toIssueVo).collect(Collectors.toList());
    }

    /**
     * 使用公共枚举创建异常对象。
     *
     * @param level 异常级别
     * @param stepEnum 排程阶段
     * @param category 异常类别
     * @param message 异常说明
     * @return 公共异常对象
     */
    private ScheduleIssueModel createIssue(ScheduleIssueLevelEnum level, ScheduleStepEnum stepEnum,
                                           ScheduleIssueCategoryEnum category, String message) {
        return this.createIssue(level.getCode(), stepEnum.getCode(), stepEnum.getDesc(),
                category.getCode(), message);
    }

    /**
     * 将内部异常转换为公共接口对象。
     *
     * @param source 内部异常
     * @return 公共接口异常对象
     */
    private AutoScheduleIssueVo toIssueVo(ScheduleIssueModel source) {
        AutoScheduleIssueVo target = new AutoScheduleIssueVo();
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
        target.setSuggestion(source.getSuggestion());
        return target;
    }

    /**
     * 按自动排程问题类别读取建议处理国际化文案。
     *
     * @param category 问题类别编码
     * @return 建议处理文案
     */
    private String resolveSuggestion(String category) {
        ScheduleIssueCategoryEnum issueCategory = ScheduleIssueCategoryEnum.fromCode(category);
        String messageKey = issueCategory == null
                ? "ui.schedule.issue.suggestion.default" : issueCategory.getSuggestion();
        return I18nUtil.getMessage(messageKey);
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
        target.setSuggestion(source.getSuggestion());
        return target;
    }
}
