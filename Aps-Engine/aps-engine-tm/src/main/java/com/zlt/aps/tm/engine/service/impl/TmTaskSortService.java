package com.zlt.aps.tm.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.tm.api.enums.TmScheduleErrorCodeEnum;
import com.zlt.aps.tm.engine.domain.TmParamValue;
import com.zlt.aps.tm.engine.domain.TmRuleTrace;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.engine.service.ITmTaskSortService;
import com.zlt.aps.tm.engine.strategy.ITmTaskSortStrategy;
import com.zlt.aps.tm.engine.strategy.TmStrategyRegistry;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 胎面待排任务默认排序步骤服务。
 *
 * <p>通过 {@link TmStrategyRegistry} 获取排序策略，替代直接按 businessKey 排序。
 * 排序策略编码从上下文参数读取（参数码 {@code TM_TASK_SORT_STRATEGY}，缺省 {@code "DEFAULT"}）。</p>
 */
@Service
public class TmTaskSortService implements ITmTaskSortService {

    /** 任务排序策略编码参数码 */
    private static final String PARAM_TASK_SORT_STRATEGY = "TM_TASK_SORT_STRATEGY";

    /** 默认排序策略编码 */
    private static final String DEFAULT_TASK_SORT_STRATEGY = "DEFAULT";

    private final TmStrategyRegistry strategyRegistry;

    /**
     * 创建任务排序服务。
     *
     * @param strategyRegistry 胎面策略注册表
     */
    public TmTaskSortService(TmStrategyRegistry strategyRegistry) {
        this.strategyRegistry = strategyRegistry;
    }

    @Override
    public void sort(TmScheduleContext context) {
        if (context == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_CONTEXT_EMPTY.getDefaultMessage());
        }
        if (CollUtil.isEmpty(context.getTaskDraftList())) {
            return;
        }
        // 读取排序策略编码，缺省 DEFAULT
        String strategyCode = readParam(context, PARAM_TASK_SORT_STRATEGY, DEFAULT_TASK_SORT_STRATEGY);
        ITmTaskSortStrategy sortStrategy = strategyRegistry.getTaskSortStrategy(strategyCode);
        Comparator<TmTaskDraft> comparator = sortStrategy.buildComparator(context);
        context.getTaskDraftList().sort(comparator);
        for (int i = 0; i < context.getTaskDraftList().size(); i++) {
            TmTaskDraft task = context.getTaskDraftList().get(i);
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("strategyCode", strategyCode);
            evidence.put("sortIndex", i + 1);
            traceOf(context, task).addRuleHit("TASK_SORT", "PASS", evidence);
        }
    }

    /**
     * 获取任务规则证据对象，不存在时创建。
     *
     * @param context 排程上下文
     * @param task    任务草稿
     * @return 规则证据对象
     */
    private TmRuleTrace traceOf(TmScheduleContext context, TmTaskDraft task) {
        return context.getRuleTraceMap().computeIfAbsent(task.getBusinessKey(), key -> new TmRuleTrace());
    }

    /**
     * 从上下文读取参数值，缺省时返回默认值。
     *
     * @param context      胎面排程上下文
     * @param paramCode    参数编码
     * @param defaultValue 缺省值
     * @return 参数有效值
     */
    private String readParam(TmScheduleContext context, String paramCode, String defaultValue) {
        TmParamValue paramValue = context.getParamMap().get(paramCode);
        if (paramValue == null || StrUtil.isBlank(paramValue.getEffectiveValue())) {
            return defaultValue;
        }
        return paramValue.getEffectiveValue();
    }
}
