package com.zlt.aps.tm.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.common.engine.schedule.ScheduleProcessTraceEvent;
import com.zlt.aps.tm.api.domain.entity.TmScheduleProcessLog;
import com.zlt.aps.tm.api.enums.TmScheduleStepEnum;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.mapper.TmScheduleProcessLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 胎面自动排程失败过程日志独立保存服务。
 */
@Service
public class TmFailedProcessLogService {

    private final TmScheduleProcessLogMapper scheduleProcessLogMapper;

    /**
     * 创建失败过程日志保存服务。
     *
     * @param scheduleProcessLogMapper 过程日志 Mapper
     */
    public TmFailedProcessLogService(TmScheduleProcessLogMapper scheduleProcessLogMapper) {
        this.scheduleProcessLogMapper = scheduleProcessLogMapper;
    }

    /**
     * 在独立短事务中保存失败批次过程日志。
     *
     * @param context     已建立的排程上下文，允许为空
     * @param batchNo    批次号
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param exception  原始排程异常
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveFailure(TmScheduleContext context, String batchNo, String factoryCode,
                            Date scheduleDate, RuntimeException exception) {
        TmScheduleContext failureContext = context == null ? this.buildMinimalContext(batchNo, factoryCode, scheduleDate) : context;
        String failureStep = StrUtil.blankToDefault(failureContext.getCurrentProcessStep(),
                context == null ? "初始化前" : "步骤边界外");
        String exceptionSummary = exception == null ? "未取得异常摘要"
                : exception.getClass().getSimpleName() + "："
                + StrUtil.blankToDefault(exception.getMessage(), "异常消息为空");
        String remainingSteps = this.resolveRemainingSteps(failureContext.getCompletedProcessSteps());
        failureContext.appendProcessLog("自动排程失败：中断步骤={0}，已完成步骤数={1}，已完成事件数={2}，异常摘要={3}，未执行的后续步骤={4}",
                failureStep, failureContext.getCompletedProcessSteps().size(),
                failureContext.getProcessLogEventCount(), exceptionSummary, remainingSteps);
        failureContext.appendFullProcessTrace(new ScheduleProcessTraceEvent(
                "失败处理", "批次级", "失败批次独立保存",
                context == null ? "自动排程任务请求和外层异常处理；计算上下文尚未建立。"
                        : "已完成的过程事件、当前步骤和外层捕获的原始异常。",
                "中断步骤=" + failureStep + "，已完成步骤=" + failureContext.getCompletedProcessSteps()
                        + "，异常摘要=" + exceptionSummary + "。",
                "主排程事务失败后，使用独立新事务保存已发生过程；失败日志异常不得替换原排程异常。",
                "已完成事件数=" + failureContext.getProcessLogEventCount() + "；未执行的后续步骤=" + remainingSteps + "。",
                context == null ? "失败日志已标记“计算尚未开始”。" : "失败日志已保留中断前全部事件。",
                "保存到本批次过程日志，供测试人员定位失败步骤；后续排程步骤不再执行。"
        ));
        String logText = failureContext.getProcessLogText();
        if (StrUtil.isBlank(logText)) {
            return;
        }
        TmScheduleProcessLog existingLog = scheduleProcessLogMapper.selectOne(new LambdaQueryWrapper<TmScheduleProcessLog>()
                .eq(TmScheduleProcessLog::getBatchNo, batchNo));
        if (existingLog == null) {
            existingLog = new TmScheduleProcessLog();
            existingLog.setBatchNo(batchNo);
            existingLog.setLogDetail(logText);
            scheduleProcessLogMapper.insert(existingLog);
        } else {
            existingLog.setLogDetail(logText);
            scheduleProcessLogMapper.updateById(existingLog);
        }
    }

    private TmScheduleContext buildMinimalContext(String batchNo, String factoryCode, Date scheduleDate) {
        TmScheduleContext context = new TmScheduleContext();
        context.setBatchNo(batchNo);
        context.setFactoryCode(factoryCode);
        context.setScheduleDate(scheduleDate);
        context.appendProcessLog("批次失败摘要：批次号={0}，工厂={1}，排程日期={2}，计算尚未开始。",
                batchNo, StrUtil.blankToDefault(factoryCode, "未提供"),
                scheduleDate == null ? "未提供" : DateUtil.formatDate(scheduleDate));
        return context;
    }

    private String resolveRemainingSteps(List<String> completedSteps) {
        List<String> completed = completedSteps == null ? java.util.Collections.emptyList() : completedSteps;
        String remaining = Arrays.asList(TmScheduleStepEnum.BOOTSTRAP, TmScheduleStepEnum.INVENTORY_PREDICT,
                        TmScheduleStepEnum.PLAN_CALC, TmScheduleStepEnum.TASK_SORT,
                        TmScheduleStepEnum.MACHINE_ASSIGN, TmScheduleStepEnum.SNAPSHOT_BUILD).stream()
                .map(TmScheduleStepEnum::getDesc)
                .filter(step -> !completed.contains(step)).collect(Collectors.joining("、"));
        return StrUtil.blankToDefault(remaining, "无");
    }
}
