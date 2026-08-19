package com.zlt.aps.tc.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.enums.TcScheduleErrorCodeEnum;
import com.zlt.aps.tc.api.enums.TcScheduleRuleCodeEnum;
import com.zlt.aps.tc.api.enums.TcScheduleRuleResultEnum;
import com.zlt.aps.tc.api.enums.TcScheduleStrategyEnum;
import com.zlt.aps.tc.engine.domain.TcParamValue;
import com.zlt.aps.tc.engine.domain.TcRuleTrace;
import com.zlt.aps.tc.engine.domain.TcScheduleContext;
import com.zlt.aps.tc.engine.domain.TcTaskDraft;
import com.zlt.aps.tc.engine.service.ITcTaskSortService;
import com.zlt.aps.tc.engine.strategy.ITcTaskSortStrategy;
import com.zlt.aps.tc.engine.strategy.TcStrategyRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 胎侧待排任务默认排序步骤服务。
 *
 * <p>通过 {@link TcStrategyRegistry} 获取排序策略，替代直接按 businessKey 排序。
 * 排序策略编码从上下文参数读取，参数键和默认策略分别由
 * {@link TcScheduleConstants#PARAM_TASK_SORT_STRATEGY}、{@link TcScheduleStrategyEnum#DEFAULT} 统一定义。</p>
 */
@Slf4j
@Service
public class TcTaskSortService implements ITcTaskSortService {

    private final TcStrategyRegistry strategyRegistry;

    /**
     * 创建任务排序服务。
     *
     * @param strategyRegistry 胎侧策略注册表
     */
    public TcTaskSortService(TcStrategyRegistry strategyRegistry) {
        this.strategyRegistry = strategyRegistry;
    }

    @Override
    public void sort(TcScheduleContext context) {
        if (context == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_CONTEXT_EMPTY.getDefaultMessage());
        }
        if (CollUtil.isEmpty(context.getTaskDraftList())) {
            return;
        }
        // 读取排序策略编码，缺省 DEFAULT
        String strategyCode = readParam(context, TcScheduleConstants.PARAM_TASK_SORT_STRATEGY,
                TcScheduleStrategyEnum.DEFAULT.getCode());
        ITcTaskSortStrategy sortStrategy = strategyRegistry.getTaskSortStrategy(strategyCode);
        Comparator<TcTaskDraft> comparator = this.buildStartupAwareComparator(context,
                sortStrategy.buildComparator(context));
        String beforeOrder = summarizeTaskOrder(context);
        context.getTaskDraftList().sort(comparator);
        String afterOrder = summarizeTaskOrder(context);
        log.info("[TC_TASK_SORT] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, strategyCode={}, taskCount={}, beforeOrder={}, afterOrder={}",
                context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), formatScheduleDate(context),
                strategyCode, context.getTaskDraftList().size(), beforeOrder, afterOrder);
        for (int i = 0; i < context.getTaskDraftList().size(); i++) {
            TcTaskDraft task = context.getTaskDraftList().get(i);
            task.setBaseSortIndex(i + 1);
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("strategyCode", strategyCode);
            evidence.put("sortIndex", i + 1);
            evidence.put("supplyHours", task.getSupplyHours());
            evidence.put("startupShift", this.isStartupShift(context, task));
            evidence.put("startupSortPriority", "SUPPLY_HOURS_ASC");
            evidence.put("glueCode", task.getGlueCode());
            evidence.put("baseGlueCode", task.getBaseGlueCode());
            evidence.put("mouthPlateCode", task.getMouthPlateCode());
            traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.TASK_SORT,
                    TcScheduleRuleResultEnum.PASS, evidence);
            log.info("[TC_TASK_SORT_DETAIL] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, strategyCode={}, sortIndex={}, businessKey={}, sidewallCode={}, shiftOrder={}, supplyHours={}, glueCode={}, baseGlueCode={}, mouthPlateCode={}, planQty={}, demandQty={}",
                    context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), formatScheduleDate(context),
                    strategyCode, i + 1, task.getBusinessKey(), task.getSidewallCode(), task.getShiftOrder(),
                    task.getSupplyHours(), task.getGlueCode(), task.getBaseGlueCode(), task.getMouthPlateCode(),
                    task.getPlanQty(), task.getDemandQty());
        }
    }

    /**
     * 构建库存供应时长优先的任务比较器。
     *
     * <p>班次顺序保持第一优先级；同一班次内所有任务均先按库存供应成型时长升序排序，
     * 时长为空的任务排在有值任务之后，供应时长相同时再执行原排序策略。</p>
     *
     * @param context            排程上下文
     * @param originalComparator 原任务排序比较器
     * @return 库存供应时长增强后的比较器
     */
    private Comparator<TcTaskDraft> buildStartupAwareComparator(TcScheduleContext context,
                                                                 Comparator<TcTaskDraft> originalComparator) {
        return Comparator
                .comparing(TcTaskDraft::getShiftOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TcTaskDraft::getSupplyHours,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(originalComparator);
    }

    /**
     * 判断任务是否属于整日停产后的首个开放班次。
     *
     * @param context 排程上下文
     * @param task    任务草稿
     * @return 属于开产班次返回true
     */
    private boolean isStartupShift(TcScheduleContext context, TcTaskDraft task) {
        return context != null && task != null && context.getStartupShiftOrderSet() != null
                && context.getStartupShiftOrderSet().contains(task.getShiftOrder());
    }

    /**
     * 汇总当前任务顺序，最多保留前20个业务键，避免日志过大。
     *
     * @param context 排程上下文
     * @return 任务顺序摘要
     */
    private String summarizeTaskOrder(TcScheduleContext context) {
        if (context == null || context.getTaskDraftList() == null) {
            return "";
        }
        return context.getTaskDraftList().stream()
                .limit(TcScheduleConstants.TASK_ORDER_SUMMARY_LIMIT)
                .map(TcTaskDraft::getBusinessKey)
                .collect(Collectors.joining(","));
    }

    /**
     * 格式化排程日期，避免日志中直接打印Date对象造成排查口径不统一。
     *
     * @param context 排程上下文
     * @return yyyy-MM-dd格式日期；日期为空时返回null
     */
    private String formatScheduleDate(TcScheduleContext context) {
        return context == null || context.getScheduleDate() == null ? null : DateUtil.formatDate(context.getScheduleDate());
    }

    /**
     * 获取任务规则证据对象，不存在时创建。
     *
     * @param context 排程上下文
     * @param task    任务草稿
     * @return 规则证据对象
     */
    private TcRuleTrace traceOf(TcScheduleContext context, TcTaskDraft task) {
        return context.getRuleTraceMap().computeIfAbsent(task.getBusinessKey(), key -> new TcRuleTrace());
    }

    /**
     * 从上下文读取参数值，缺省时返回默认值。
     *
     * @param context      胎侧排程上下文
     * @param paramCode    参数编码
     * @param defaultValue 缺省值
     * @return 参数有效值
     */
    private String readParam(TcScheduleContext context, String paramCode, String defaultValue) {
        TcParamValue paramValue = context.getParamMap().get(paramCode);
        if (paramValue == null || StrUtil.isBlank(paramValue.getEffectiveValue())) {
            return defaultValue;
        }
        return paramValue.getEffectiveValue();
    }
}
