package com.zlt.aps.tm.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.zlt.aps.common.engine.schedule.IScheduleProcessLogger;
import com.zlt.aps.common.engine.schedule.ScheduleChainChangeResult;
import com.zlt.aps.common.engine.schedule.ScheduleRuleResult;
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
 * <p>运行日志保留完整的规则与任务链追踪；过程日志表仅保留步骤边界和关键计算结论，
 * 最终由业务落库步骤统一写入专用过程日志表，不在引擎层直接依赖数据库 Mapper。</p>
 */
@Component
public class TmScheduleProcessLogger implements IScheduleProcessLogger<TmScheduleContext> {

    private static final Logger log = LoggerFactory.getLogger(TmScheduleProcessLogger.class);

    @Override
    public void logStepStart(TmScheduleContext context, String stepCode, String inputSummary) {
        log.info("[胎面排程-步骤开始] 批次号={}，追踪号={}，工厂编号={}，排程日期={}，步骤={}，输入摘要={}",
                this.batchNo(context), this.traceId(context), this.factoryCode(context), this.scheduleDate(context), stepCode, inputSummary);
        this.append(context, "步骤开始：步骤={0}，输入摘要={1}", stepCode, inputSummary);
    }

    @Override
    public void logStepEnd(TmScheduleContext context, String stepCode, String outputSummary) {
        log.info("[胎面排程-步骤完成] 批次号={}，追踪号={}，工厂编号={}，排程日期={}，步骤={}，输出摘要={}",
                this.batchNo(context), this.traceId(context), this.factoryCode(context), this.scheduleDate(context), stepCode, outputSummary);
        this.append(context, "步骤完成：步骤={0}，输出摘要={1}", stepCode, outputSummary);
    }

    @Override
    public void logRuleResult(TmScheduleContext context, String ruleCode, ScheduleRuleResult result) {
        if (result != null && !result.isPassed()) {
            log.warn("[胎面排程-规则未通过] 批次号={}，追踪号={}，原因={}",
                    this.batchNo(context), this.traceId(context), result.getReasonDesc());
            return;
        }
        log.debug("[胎面排程-规则通过] 批次号={}，追踪号={}", this.batchNo(context), this.traceId(context));
    }

    @Override
    public void logChainChange(TmScheduleContext context, ScheduleChainChangeResult<?> result) {
        log.info("[胎面排程-任务链变化] 批次号={}，追踪号={}，受影响任务数={}",
                this.batchNo(context), this.traceId(context),
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
        log.warn("[胎面排程-未排任务] 批次号={}，追踪号={}，工厂编号={}，排程日期={}，任务标识={}，胎面编码={}，主胶料={}，基部胶={}，口型板={}，班次={}，计划量={}，未排原因={}，候选机台拒绝汇总={}",
                this.batchNo(context), this.traceId(context), this.factoryCode(context), this.scheduleDate(context),
                task == null ? null : task.getBusinessKey(), task == null ? null : task.getTreadCode(),
                task == null ? null : task.getGlueCode(),
                task == null ? null : task.getBaseGlueCode(),
                task == null ? null : task.getMouthPlateCode(),
                task == null ? null : task.getShiftOrder(), task == null ? null : task.getPlanQty(),
                task == null ? null : task.getUnplannedReasonDesc(), this.summarizeCandidateRejects(context, task));
    }

    /**
     * 记录落库汇总。
     *
     * @param context 胎面排程上下文
     * @param result  落库结果摘要
     */
    public void logPersistSummary(TmScheduleContext context, com.zlt.aps.tm.engine.domain.TmPersistResult result) {
        log.info("[胎面排程-落库汇总] 批次号={}，追踪号={}，工厂编号={}，排程日期={}，结果数量={}，解释数量={}，未排数量={}，异常数量={}",
                this.batchNo(context), this.traceId(context), this.factoryCode(context), this.scheduleDate(context), result == null ? 0 : result.getResultCount(),
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
                .collect(Collectors.groupingBy(candidate -> StrUtil.blankToDefault(candidate.getFilterReasonDesc(),
                                "未提供原因"),
                        LinkedHashMap::new, Collectors.counting()));
    }

    /**
     * 向上下文追加中文过程记录。
     *
     * @param context 排程上下文
     * @param format  日志格式
     * @param args    日志参数
     */
    private void append(TmScheduleContext context, String format, Object... args) {
        if (context != null) {
            context.appendProcessLog(format, args);
        }
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
