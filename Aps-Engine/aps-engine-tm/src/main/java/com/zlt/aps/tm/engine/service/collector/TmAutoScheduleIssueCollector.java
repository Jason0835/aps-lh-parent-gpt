package com.zlt.aps.tm.engine.service.collector;

import cn.hutool.core.collection.CollUtil;
import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleIssueVo;
import com.zlt.aps.tm.api.enums.TmAutoScheduleIssueCategoryEnum;
import com.zlt.aps.tm.api.enums.TmAutoScheduleIssueLevelEnum;
import com.zlt.aps.tm.api.enums.TmScheduleStepEnum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 胎面自动排程异常收集器。
 *
 * <p>在单次自动排程上下文内收集可返回前端的结构化异常，
 * 包括可继续排程的警告和阻断执行的错误。</p>
 */
public class TmAutoScheduleIssueCollector {

    private final List<TmAutoScheduleIssueVo> issues = new ArrayList<>();

    /**
     * 记录施工匹配异常。
     *
     * @param level         异常级别
     * @param category      异常类别
     * @param sourceOrderNo 来源工单号
     * @param embryoCode    胎胚代码
     * @param recipeNo      示方书编号
     * @param shiftOrder    班次顺序
     * @param fieldName     字段名称
     * @param message       异常说明
     */
    public void addConstructionIssue(TmAutoScheduleIssueLevelEnum level,
                                     TmAutoScheduleIssueCategoryEnum category,
                                     String sourceOrderNo, String embryoCode, String recipeNo,
                                     Integer shiftOrder, String fieldName, String message) {
        TmAutoScheduleIssueVo issue = this.createIssue(level, TmScheduleStepEnum.BOOTSTRAP, category, message);
        issue.setSourceOrderNo(sourceOrderNo);
        issue.setEmbryoCode(embryoCode);
        issue.setRecipeNo(recipeNo);
        issue.setShiftOrder(shiftOrder);
        issue.setFieldName(fieldName);
        this.addIssue(issue);
    }

    /**
     * 记录通用异常。
     *
     * @param level     异常级别
     * @param stepEnum  排程阶段
     * @param category  异常类别
     * @param message   异常说明
     */
    public void addIssue(TmAutoScheduleIssueLevelEnum level, TmScheduleStepEnum stepEnum,
                         TmAutoScheduleIssueCategoryEnum category, String message) {
        this.addIssue(this.createIssue(level, stepEnum, category, message));
    }

    /**
     * 记录计划量汇总组生产属性冲突。
     *
     * @param treadCode   胎面编码
     * @param shiftOrder  班次顺序
     * @param message     包含汇总组和来源业务键的冲突说明
     */
    public void addPlanGroupAttributeConflictIssue(String treadCode, Integer shiftOrder, String message) {
        TmAutoScheduleIssueVo issue = this.createIssue(TmAutoScheduleIssueLevelEnum.ERROR,
                TmScheduleStepEnum.PLAN_CALC, TmAutoScheduleIssueCategoryEnum.PLAN_GROUP_ATTRIBUTE_CONFLICT, message);
        issue.setTreadCode(treadCode);
        issue.setShiftOrder(shiftOrder);
        issue.setFieldName("productionAttributes");
        this.addIssue(issue);
    }

    /**
     * 指定阶段尚无阻断问题时记录失败明细。
     *
     * @param stepEnum 排程阶段
     * @param category 失败类别
     * @param message  前端可展示的失败消息
     */
    public void addFailureIssueIfAbsent(TmScheduleStepEnum stepEnum,
                                        TmAutoScheduleIssueCategoryEnum category, String message) {
        if (this.hasErrorIssue(stepEnum)) {
            return;
        }
        this.addIssue(TmAutoScheduleIssueLevelEnum.ERROR, stepEnum, category, message);
    }

    /**
     * 判断指定阶段是否已经存在阻断问题。
     *
     * @param stepEnum 排程阶段
     * @return 存在 ERROR 级别问题时返回 true
     */
    public boolean hasErrorIssue(TmScheduleStepEnum stepEnum) {
        if (stepEnum == null) {
            return false;
        }
        return issues.stream().anyMatch(issue ->
                TmAutoScheduleIssueLevelEnum.ERROR.getCode().equals(issue.getLevel())
                        && stepEnum.getCode().equals(issue.getStageCode()));
    }

    /**
     * 判断当前任务是否已经存在任一阻断问题。
     *
     * @return 存在 ERROR 级别问题时返回 true
     */
    public boolean hasErrorIssue() {
        return issues.stream().anyMatch(issue ->
                TmAutoScheduleIssueLevelEnum.ERROR.getCode().equals(issue.getLevel()));
    }

    /**
     * 记录缺库存快照告警。
     *
     * @param treadCode 胎面编码
     * @param message 告警说明
     */
    public void addStockMissingIssue(String treadCode, String message) {
        TmAutoScheduleIssueVo issue = this.createIssue(TmAutoScheduleIssueLevelEnum.WARN,
                TmScheduleStepEnum.INVENTORY_PREDICT, TmAutoScheduleIssueCategoryEnum.STOCK_MISSING, message);
        issue.setTreadCode(treadCode);
        issue.setFieldName("stockQty");
        this.addIssue(issue);
    }

    /**
     * 获取异常明细副本。
     *
     * @return 异常明细列表
     */
    public List<TmAutoScheduleIssueVo> getIssues() {
        if (CollUtil.isEmpty(issues)) {
            return Collections.emptyList();
        }
        return new ArrayList<>(issues);
    }

    /**
     * 创建包含统一阶段和级别字段的异常对象。
     *
     * @param level 异常级别
     * @param stepEnum 排程阶段
     * @param category 异常类别
     * @param message 异常说明
     * @return 已填充公共字段的异常对象
     */
    private TmAutoScheduleIssueVo createIssue(TmAutoScheduleIssueLevelEnum level, TmScheduleStepEnum stepEnum,
                                              TmAutoScheduleIssueCategoryEnum category, String message) {
        TmAutoScheduleIssueVo issue = new TmAutoScheduleIssueVo();
        issue.setLevel(level.getCode());
        issue.setStageCode(stepEnum.getCode());
        issue.setStageName(stepEnum.getDesc());
        issue.setCategory(category.getCode());
        issue.setMessage(message);
        return issue;
    }

    /**
     * 将异常对象追加至当前排程上下文。
     *
     * @param issue 待追加异常对象
     */
    private void addIssue(TmAutoScheduleIssueVo issue) {
        issues.add(issue);
    }
}
