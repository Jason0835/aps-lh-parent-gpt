package com.zlt.aps.tm.engine.service;

import cn.hutool.core.collection.CollUtil;
import com.zlt.aps.tm.api.enums.TmUnplannedReasonEnum;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import org.springframework.stereotype.Service;

/**
 * 胎面需求量和计划量默认计算步骤服务。
 *
 * <p>当前不实现第15章未确认算法。若任务已有计划量则保持不变；若计划量为空且需求量存在，
 * 使用需求量作为骨架阶段计划量；需求量也缺失时标记未排原因。</p>
 */
@Service
public class TmPlanCalcService implements ITmPlanCalcService {

    @Override
    public void calculate(TmScheduleContext context) {
        if (context == null) {
            throw new IllegalArgumentException("胎面排程上下文不能为空");
        }
        if (CollUtil.isEmpty(context.getTaskDraftList())) {
            return;
        }
        for (TmTaskDraft task : context.getTaskDraftList()) {
            if (task.getPlanQty() != null) {
                continue;
            }
            if (task.getDemandQty() != null) {
                task.setPlanQty(task.getDemandQty());
                continue;
            }
            task.setUnplannedReasonCode(TmUnplannedReasonEnum.DEMAND_MISSING.getCode());
            task.setUnplannedReasonDesc(TmUnplannedReasonEnum.DEMAND_MISSING.getDesc());
        }
    }
}
