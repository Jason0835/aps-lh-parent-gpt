package com.zlt.aps.tm.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.zlt.aps.common.engine.schedule.IScheduleProcessLogger;
import com.zlt.aps.common.engine.schedule.ScheduleChainChangeResult;
import com.zlt.aps.common.engine.schedule.ScheduleRuleResult;
import com.zlt.aps.tm.api.enums.TmMachineFilterReasonEnum;
import com.zlt.aps.tm.engine.domain.TmMachineCandidate;
import com.zlt.aps.tm.engine.domain.TmRuleTrace;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 胎面排程过程日志实现。
 *
 * <p>当前骨架只输出低敏摘要日志，不打印完整业务对象和敏感信息。后续如需要落库，
 * 可在此类中接入 `T_TM_DISPATCHER_LOG` 或专用过程日志表。</p>
 */
@Component
public class TmScheduleProcessLogger implements IScheduleProcessLogger<TmScheduleContext> {

    private static final Logger log = LoggerFactory.getLogger(TmScheduleProcessLogger.class);

    @Override
    public void logStepStart(TmScheduleContext context, String stepCode, String inputSummary) {
        log.info("[TM_SCHEDULE_STEP_START] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, stepCode={}, input={}",
                batchNo(context), traceId(context), factoryCode(context), scheduleDate(context), stepCode, inputSummary);
    }

    @Override
    public void logStepEnd(TmScheduleContext context, String stepCode, String outputSummary) {
        log.info("[TM_SCHEDULE_STEP_END] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, stepCode={}, output={}",
                batchNo(context), traceId(context), factoryCode(context), scheduleDate(context), stepCode, outputSummary);
    }

    @Override
    public void logRuleResult(TmScheduleContext context, String ruleCode, ScheduleRuleResult result) {
        if (result != null && !result.isPassed()) {
            log.warn("[TM_SCHEDULE_RULE] batchNo={}, traceId={}, ruleCode={}, reasonCode={}, reasonDesc={}",
                    batchNo(context), traceId(context), ruleCode, result.getReasonCode(), result.getReasonDesc());
            return;
        }
        log.debug("[TM_SCHEDULE_RULE] batchNo={}, traceId={}, ruleCode={}, passed=true",
                batchNo(context), traceId(context), ruleCode);
    }

    @Override
    public void logChainChange(TmScheduleContext context, ScheduleChainChangeResult<?> result) {
        log.info("[TM_SCHEDULE_CHAIN_CHANGE] batchNo={}, traceId={}, operation={}, affected={}",
                batchNo(context), traceId(context), result == null ? null : result.getOperationType(),
                result == null ? 0 : result.getAffectedNodes().size());
    }

    /**
     * 记录未排任务。
     *
     * @param task    未排任务
     * @param trace   规则证据
     * @param context 胎面排程上下文
     */
    public void logUnplanned(TmTaskDraft task, TmRuleTrace trace, TmScheduleContext context) {
        log.warn("[TM_SCHEDULE_UNPLANNED] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, businessKey={}, treadCode={}, glueCode={}, baseGlueCode={}, mouthPlateCode={}, shiftOrder={}, planQty={}, reasonCode={}, candidateRejectSummary={}, evidence={}",
                batchNo(context), traceId(context), factoryCode(context), scheduleDate(context),
                task == null ? null : task.getBusinessKey(), task == null ? null : task.getTreadCode(),
                task == null ? null : task.getGlueCode(),
                task == null ? null : task.getBaseGlueCode(),
                task == null ? null : task.getMouthPlateCode(),
                task == null ? null : task.getShiftOrder(), task == null ? null : task.getPlanQty(),
                task == null ? null : task.getUnplannedReasonCode(), summarizeCandidateRejects(context, task),
                trace == null ? null : trace.toExplainJson());
    }

    /**
     * 记录落库汇总。
     *
     * @param context 胎面排程上下文
     * @param result  落库结果摘要
     */
    public void logPersistSummary(TmScheduleContext context, com.zlt.aps.tm.engine.domain.TmPersistResult result) {
        log.info("[TM_SCHEDULE_PERSIST] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, resultCount={}, explainCount={}, unplannedCount={}, errorCount={}",
                batchNo(context), traceId(context), factoryCode(context), scheduleDate(context), result == null ? 0 : result.getResultCount(),
                result == null ? 0 : result.getExplainCount(), result == null ? 0 : result.getUnplannedCount(),
                result == null ? 0 : result.getErrorCount());
    }

    /**
     * 汇总未排任务候选机台拒绝原因。
     *
     * @param context 排程上下文
     * @param task 未排任务
     * @return 拒绝原因统计
     */
    private Map<String, Long> summarizeCandidateRejects(TmScheduleContext context, TmTaskDraft task) {
        if (context == null || task == null || context.getCandidateTraceMap() == null) {
            return Collections.emptyMap();
        }
        List<TmMachineCandidate> candidates = context.getCandidateTraceMap().get(task.getBusinessKey());
        if (CollUtil.isEmpty(candidates)) {
            return Collections.emptyMap();
        }
        return candidates.stream()
                .filter(TmMachineCandidate::isFiltered)
                .collect(Collectors.groupingBy(candidate -> StrUtil.blankToDefault(candidate.getFilterReasonCode(),
                                TmMachineFilterReasonEnum.UNKNOWN.getCode()),
                        LinkedHashMap::new, Collectors.counting()));
    }
    private String batchNo(TmScheduleContext context) {
        return context == null ? null : context.getBatchNo();
    }

    private String traceId(TmScheduleContext context) {
        return context == null ? null : context.getTraceId();
    }

    private String factoryCode(TmScheduleContext context) {
        return context == null ? null : context.getFactoryCode();
    }

    private String scheduleDate(TmScheduleContext context) {
        return context == null || context.getScheduleDate() == null ? null : DateUtil.formatDate(context.getScheduleDate());
    }
}
