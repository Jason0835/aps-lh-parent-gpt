package com.zlt.aps.tm.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.enums.TmScheduleErrorCodeEnum;
import com.zlt.aps.tm.api.enums.TmScheduleStrategyEnum;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.engine.service.ITmTaskSortService;
import com.zlt.aps.tm.engine.strategy.ITmTaskSortStrategy;
import com.zlt.aps.tm.engine.strategy.TmStrategyRegistry;
import com.zlt.aps.tm.engine.util.TmScheduleContextValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * 胎面待排任务默认排序步骤服务。
 *
 * <p>通过 {@link TmStrategyRegistry} 获取排序策略，替代直接按 businessKey 排序。
 * 排序策略编码从上下文参数读取，参数键和默认策略分别由
 * {@link TmScheduleConstants#PARAM_TASK_SORT_STRATEGY}、{@link TmScheduleStrategyEnum#DEFAULT} 统一定义。</p>
 */
@Slf4j
@Service
public class TmTaskSortService implements ITmTaskSortService {

    private final TmStrategyRegistry strategyRegistry;

    /** 任务排序规则证据记录组件。 */
    private final RuleTraceRecorder ruleTraceRecorder = new RuleTraceRecorder();

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
        // 读取排序策略编码，缺省 DEFAULT。
        String strategyCode = readParam(context, TmScheduleConstants.PARAM_TASK_SORT_STRATEGY,
                TmScheduleStrategyEnum.DEFAULT.getCode());
        String beforeOrder = summarizeTaskOrder(context);
        boolean planCalcOrderReady = context.getTaskDraftList().stream()
                .allMatch(task -> task != null && task.getPlanCalcOrderIndex() != null);
        String sortSource = planCalcOrderReady ? "PLAN_CALC_ORDER" : "LEGACY_TASK_SORT";
        if (planCalcOrderReady) {
            // 主流程已由计划量计算阶段确定顺序，此处只复用并固化该顺序，避免计划量完成后再次抢占任务。
            context.getTaskDraftList().sort(Comparator
                    .comparing(TmTaskDraft::getPlanCalcOrderIndex, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(task -> StrUtil.blankToDefault(task.getBusinessKey(), "")));
        } else {
            // 独立调用或旧测试上下文未提供计划量顺序时保留兼容排序。
            ITmTaskSortStrategy sortStrategy = strategyRegistry.getTaskSortStrategy(strategyCode);
            Comparator<TmTaskDraft> comparator = this.buildStartupAwareComparator(context,
                    sortStrategy.buildComparator(context));
            context.getTaskDraftList().sort(comparator);
        }
        String afterOrder = summarizeTaskOrder(context);
        log.info("[TM_TASK_SORT] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, strategyCode={}, sortSource={}, taskCount={}, beforeOrder={}, afterOrder={}",
                context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), formatScheduleDate(context),
                strategyCode, sortSource, context.getTaskDraftList().size(), beforeOrder, afterOrder);
        for (int i = 0; i < context.getTaskDraftList().size(); i++) {
            TmTaskDraft task = context.getTaskDraftList().get(i);
            int sortIndex = planCalcOrderReady && task.getPlanCalcOrderIndex() != null
                    ? task.getPlanCalcOrderIndex() : i + 1;
            task.setBaseSortIndex(sortIndex);
            ruleTraceRecorder.recordTaskSort(context, task, strategyCode, sortSource, sortIndex,
                    this.isStartupShift(context, task));
            this.logTaskSortDetail(context, task, strategyCode, sortSource, sortIndex);
        }
    }

    /**
     * 按固定字段顺序输出单任务排序明细日志。
     *
     * @param context 排程上下文
     * @param task 已确定排序位置的任务
     * @param strategyCode 排序策略编码
     * @param sortSource 排序来源
     * @param sortIndex 最终基础排序号
     */
    private void logTaskSortDetail(TmScheduleContext context, TmTaskDraft task, String strategyCode,
                                   String sortSource, int sortIndex) {
        log.info("[TM_TASK_SORT_DETAIL] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, strategyCode={}, sortSource={}, sortIndex={}, planCalcOrderIndex={}, businessKey={}, treadCode={}, shiftOrder={}, supplyHours={}, glueCode={}, baseGlueCode={}, mouthPlateCode={}, planQty={}, demandQty={}",
                context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), this.formatScheduleDate(context),
                strategyCode, sortSource, sortIndex, task.getPlanCalcOrderIndex(), task.getBusinessKey(),
                task.getTreadCode(), task.getShiftOrder(), task.getSupplyHours(), task.getGlueCode(),
                task.getBaseGlueCode(), task.getMouthPlateCode(), task.getPlanQty(), task.getDemandQty());
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
    private Comparator<TmTaskDraft> buildStartupAwareComparator(TmScheduleContext context,
                                                                 Comparator<TmTaskDraft> originalComparator) {
        return Comparator
                .comparing(TmTaskDraft::getShiftOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TmTaskDraft::getSupplyHours,
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
    private boolean isStartupShift(TmScheduleContext context, TmTaskDraft task) {
        return context != null && task != null && context.getStartupShiftOrderSet() != null
                && context.getStartupShiftOrderSet().contains(task.getShiftOrder());
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
                .limit(TmScheduleConstants.TASK_ORDER_SUMMARY_LIMIT)
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
        return TmScheduleContextValueUtils.formatScheduleDate(context);
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
        return TmScheduleContextValueUtils.readParam(context, paramCode, defaultValue, false);
    }
}
