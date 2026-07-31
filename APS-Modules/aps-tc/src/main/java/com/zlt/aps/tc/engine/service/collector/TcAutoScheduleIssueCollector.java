package com.zlt.aps.tc.engine.service.collector;

import cn.hutool.core.collection.CollUtil;
import com.zlt.aps.tc.api.domain.vo.TcAutoScheduleIssueVo;
import com.zlt.aps.tc.api.enums.TcAutoScheduleIssueCategoryEnum;
import com.zlt.aps.tc.api.enums.TcAutoScheduleIssueLevelEnum;
import com.zlt.aps.tc.api.enums.TcScheduleStepEnum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 胎侧自动排程异常收集器。
 *
 * <p>在单次自动排程上下文内收集可返回前端的结构化异常，
 * 包括可继续排程的警告和阻断执行的错误。</p>
 */
public class TcAutoScheduleIssueCollector {

    private final List<TcAutoScheduleIssueVo> issues = new ArrayList<>();

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
    public void addConstructionIssue(TcAutoScheduleIssueLevelEnum level,
                                     TcAutoScheduleIssueCategoryEnum category,
                                     String sourceOrderNo, String embryoCode, String recipeNo,
                                     Integer shiftOrder, String fieldName, String message) {
        TcAutoScheduleIssueVo issue = new TcAutoScheduleIssueVo();
        issue.setLevel(level.getCode());
        issue.setStageCode(TcScheduleStepEnum.BOOTSTRAP.getCode());
        issue.setStageName(TcScheduleStepEnum.BOOTSTRAP.getDesc());
        issue.setCategory(category.getCode());
        issue.setSourceOrderNo(sourceOrderNo);
        issue.setEmbryoCode(embryoCode);
        issue.setRecipeNo(recipeNo);
        issue.setShiftOrder(shiftOrder);
        issue.setFieldName(fieldName);
        issue.setMessage(message);
        issues.add(issue);
    }

    /**
     * 记录通用异常。
     *
     * @param level     异常级别
     * @param stepEnum  排程阶段
     * @param category  异常类别
     * @param message   异常说明
     */
    public void addIssue(TcAutoScheduleIssueLevelEnum level, TcScheduleStepEnum stepEnum,
                         TcAutoScheduleIssueCategoryEnum category, String message) {
        TcAutoScheduleIssueVo issue = new TcAutoScheduleIssueVo();
        issue.setLevel(level.getCode());
        issue.setStageCode(stepEnum.getCode());
        issue.setStageName(stepEnum.getDesc());
        issue.setCategory(category.getCode());
        issue.setMessage(message);
        issues.add(issue);
    }

    /**
     * 记录计划量汇总组生产属性冲突。
     *
     * @param sidewallCode 胎侧编码
     * @param shiftOrder   班次顺序
     * @param message      包含汇总组和来源业务键的冲突说明
     */
    public void addPlanGroupAttributeConflictIssue(String sidewallCode, Integer shiftOrder, String message) {
        TcAutoScheduleIssueVo issue = new TcAutoScheduleIssueVo();
        issue.setLevel(TcAutoScheduleIssueLevelEnum.ERROR.getCode());
        issue.setStageCode(TcScheduleStepEnum.PLAN_CALC.getCode());
        issue.setStageName(TcScheduleStepEnum.PLAN_CALC.getDesc());
        issue.setCategory(TcAutoScheduleIssueCategoryEnum.PLAN_GROUP_ATTRIBUTE_CONFLICT.getCode());
        issue.setSidewallCode(sidewallCode);
        issue.setShiftOrder(shiftOrder);
        issue.setFieldName("productionAttributes");
        issue.setMessage(message);
        issues.add(issue);
    }

    /**
     * 指定阶段尚无阻断问题时记录失败明细。
     *
     * @param stepEnum 排程阶段
     * @param category 失败类别
     * @param message  前端可展示的失败消息
     */
    public void addFailureIssueIfAbsent(TcScheduleStepEnum stepEnum,
                                        TcAutoScheduleIssueCategoryEnum category, String message) {
        if (this.hasErrorIssue(stepEnum)) {
            return;
        }
        this.addIssue(TcAutoScheduleIssueLevelEnum.ERROR, stepEnum, category, message);
    }

    /**
     * 判断指定阶段是否已经存在阻断问题。
     *
     * @param stepEnum 排程阶段
     * @return 存在 ERROR 级别问题时返回 true
     */
    public boolean hasErrorIssue(TcScheduleStepEnum stepEnum) {
        if (stepEnum == null) {
            return false;
        }
        return issues.stream().anyMatch(issue ->
                TcAutoScheduleIssueLevelEnum.ERROR.getCode().equals(issue.getLevel())
                        && stepEnum.getCode().equals(issue.getStageCode()));
    }

    /**
     * 判断当前任务是否已经存在任一阻断问题。
     *
     * @return 存在 ERROR 级别问题时返回 true
     */
    public boolean hasErrorIssue() {
        return issues.stream().anyMatch(issue ->
                TcAutoScheduleIssueLevelEnum.ERROR.getCode().equals(issue.getLevel()));
    }

    /**
     * 记录缺库存快照告警。
     *
     * @param sidewallCode 胎侧编码
     * @param message 告警说明
     */
    public void addStockMissingIssue(String sidewallCode, String message) {
        TcAutoScheduleIssueVo issue = new TcAutoScheduleIssueVo();
        issue.setLevel(TcAutoScheduleIssueLevelEnum.WARN.getCode());
        issue.setStageCode(TcScheduleStepEnum.INVENTORY_PREDICT.getCode());
        issue.setStageName(TcScheduleStepEnum.INVENTORY_PREDICT.getDesc());
        issue.setCategory(TcAutoScheduleIssueCategoryEnum.STOCK_MISSING.getCode());
        issue.setSidewallCode(sidewallCode);
        issue.setFieldName("stockQty");
        issue.setMessage(message);
        issues.add(issue);
    }

    /**
     * 获取异常明细副本。
     *
     * @return 异常明细列表
     */
    public List<TcAutoScheduleIssueVo> getIssues() {
        if (CollUtil.isEmpty(issues)) {
            return Collections.emptyList();
        }
        return new ArrayList<>(issues);
    }
}
