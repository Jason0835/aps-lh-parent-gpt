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
        TmAutoScheduleIssueVo issue = new TmAutoScheduleIssueVo();
        issue.setLevel(level.getCode());
        issue.setStageCode(TmScheduleStepEnum.BOOTSTRAP.getCode());
        issue.setStageName(TmScheduleStepEnum.BOOTSTRAP.getDesc());
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
    public void addIssue(TmAutoScheduleIssueLevelEnum level, TmScheduleStepEnum stepEnum,
                         TmAutoScheduleIssueCategoryEnum category, String message) {
        TmAutoScheduleIssueVo issue = new TmAutoScheduleIssueVo();
        issue.setLevel(level.getCode());
        issue.setStageCode(stepEnum.getCode());
        issue.setStageName(stepEnum.getDesc());
        issue.setCategory(category.getCode());
        issue.setMessage(message);
        issues.add(issue);
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
}
