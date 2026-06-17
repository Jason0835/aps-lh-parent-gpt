package com.zlt.aps.tm.engine.service;

import cn.hutool.core.collection.CollUtil;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import org.springframework.stereotype.Service;

import java.util.Comparator;

/**
 * 胎面待排任务默认排序步骤服务。
 *
 * <p>当前只按任务稳定业务键排序，保证同一输入下顺序可重复；胶料优先级、库存紧急度等
 * 业务排序策略留给后续 `ITmTaskSortStrategy` 实现。</p>
 */
@Service
public class TmTaskSortService implements ITmTaskSortService {

    @Override
    public void sort(TmScheduleContext context) {
        if (context == null) {
            throw new IllegalArgumentException("胎面排程上下文不能为空");
        }
        if (CollUtil.isEmpty(context.getTaskDraftList())) {
            return;
        }
        context.getTaskDraftList().sort(Comparator.comparing(TmTaskDraft::getBusinessKey));
    }
}
