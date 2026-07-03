package com.zlt.aps.tm.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 胎面待排任务默认排序步骤服务。
 *
 * <p>通过 {@link TmStrategyRegistry} 获取排序策略，替代直接按 businessKey 排序。
 * 排序策略编码从上下文参数读取（参数码 {@code TM_TASK_SORT_STRATEGY}，缺省 {@code "DEFAULT"}）。</p>
 */
@Slf4j
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
        String beforeOrder = summarizeTaskOrder(context);
        context.getTaskDraftList().sort(comparator);
        String afterOrder = summarizeTaskOrder(context);
        log.info("[TM_TASK_SORT] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, strategyCode={}, taskCount={}, beforeOrder={}, afterOrder={}",
                context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), formatScheduleDate(context),
                strategyCode, context.getTaskDraftList().size(), beforeOrder, afterOrder);
        for (int i = 0; i < context.getTaskDraftList().size(); i++) {
            TmTaskDraft task = context.getTaskDraftList().get(i);
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("strategyCode", strategyCode);
            evidence.put("sortIndex", i + 1);
            evidence.put("supplyHours", task.getSupplyHours());
            evidence.put("glueCode", task.getGlueCode());
            evidence.put("baseGlueCode", task.getBaseGlueCode());
            evidence.put("mouthPlateCode", task.getMouthPlateCode());
            traceOf(context, task).addRuleHit("TASK_SORT", "PASS", evidence);
            log.info("[TM_TASK_SORT_DETAIL] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, strategyCode={}, sortIndex={}, businessKey={}, treadCode={}, shiftOrder={}, supplyHours={}, glueCode={}, baseGlueCode={}, mouthPlateCode={}, planQty={}, demandQty={}",
                    context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), formatScheduleDate(context),
                    strategyCode, i + 1, task.getBusinessKey(), task.getTreadCode(), task.getShiftOrder(),
                    task.getSupplyHours(), task.getGlueCode(), task.getBaseGlueCode(), task.getMouthPlateCode(),
                    task.getPlanQty(), task.getDemandQty());
        }
    }

    /**
     * 汇总当前任务顺序，最多保留前20个业务键，避免日志过大。
     *
     * @param context 排程上下文
     * @return 任务顺序摘要
     */
    private String summarizeTaskOrder(TmScheduleContext context) {
        if (context == null || context.getTaskDraftList() == null) {
            return "";
        }
        return context.getTaskDraftList().stream()
                .limit(20)
                .map(TmTaskDraft::getBusinessKey)
                .collect(Collectors.joining(","));
    }

    /**
     * 格式化排程日期，避免日志中直接打印Date对象造成排查口径不统一。
     *
     * @param context 排程上下文
     * @return yyyy-MM-dd格式日期；日期为空时返回null
     */
    private String formatScheduleDate(TmScheduleContext context) {
        return context == null || context.getScheduleDate() == null ? null : DateUtil.formatDate(context.getScheduleDate());
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
