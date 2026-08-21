package com.zlt.aps.tm.engine.service.impl;

import com.zlt.aps.tm.api.enums.TmScheduleRuleCodeEnum;
import com.zlt.aps.tm.api.enums.TmScheduleRuleResultEnum;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.engine.util.TmScheduleContextValueUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 胎面规则证据记录器。
 *
 * <p>集中维护固定结构的规则证据键和写入顺序；每个业务服务仍在原计算完成位置立即调用，
 * 不缓存、延迟或重排规则命中记录。</p>
 */
final class RuleTraceRecorder {

    /**
     * 记录任务排序阶段的固定证据模板。
     *
     * @param context 排程上下文
     * @param task 已确定排序位置的任务
     * @param strategyCode 排序策略编码
     * @param sortSource 排序来源
     * @param sortIndex 最终基础排序号
     * @param startupShift 是否属于开产班次
     */
    void recordTaskSort(TmScheduleContext context, TmTaskDraft task, String strategyCode,
                        String sortSource, int sortIndex, boolean startupShift) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("strategyCode", strategyCode);
        evidence.put("sortSource", sortSource);
        evidence.put("sortIndex", sortIndex);
        evidence.put("planCalcOrderIndex", task.getPlanCalcOrderIndex());
        evidence.put("supplyHours", task.getSupplyHours());
        evidence.put("startupShift", startupShift);
        evidence.put("startupSortPriority", "SUPPLY_HOURS_ASC");
        evidence.put("glueCode", task.getGlueCode());
        evidence.put("baseGlueCode", task.getBaseGlueCode());
        evidence.put("mouthPlateCode", task.getMouthPlateCode());
        TmScheduleContextValueUtils.traceOf(context, task).addRuleHit(TmScheduleRuleCodeEnum.TASK_SORT,
                TmScheduleRuleResultEnum.PASS, evidence);
    }
}
