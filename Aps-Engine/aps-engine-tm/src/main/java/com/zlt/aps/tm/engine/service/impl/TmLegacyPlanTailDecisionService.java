package com.zlt.aps.tm.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.engine.service.ITmPlanTailDecisionService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 基于现有收尾标识和收尾余量的胎面兼容判定实现。
 *
 * <p>TODO：月计划余量来源表确定后，改为按工厂、日期、胎面代码查询；
 * 余量大于零为非收尾，余量小于等于零为收尾。</p>
 */
@Service
public class TmLegacyPlanTailDecisionService implements ITmPlanTailDecisionService {

    /**
     * 兼容沿用来源任务中已经加载的收尾信息。
     *
     * @param aggregateTask 汇总生产任务
     * @param sourceTaskList 原始来源任务
     */
    @Override
    public void applyTailDecision(TmTaskDraft aggregateTask, List<TmTaskDraft> sourceTaskList) {
        if (aggregateTask == null || CollUtil.isEmpty(sourceTaskList)) {
            return;
        }
        TmTaskDraft sourceTask = sourceTaskList.get(0);
        aggregateTask.setTailFlag(sourceTask.getTailFlag());
        aggregateTask.setTailBalanceQty(sourceTask.getTailBalanceQty());
    }
}
