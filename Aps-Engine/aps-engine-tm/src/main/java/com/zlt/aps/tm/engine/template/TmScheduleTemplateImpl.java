package com.zlt.aps.tm.engine.template;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.engine.schedule.IScheduleProcessLogger;
import com.zlt.aps.tm.api.enums.TmAutoScheduleIssueCategoryEnum;
import com.zlt.aps.tm.api.enums.TmScheduleStepEnum;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.engine.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.stream.Collectors;

/**
 * 胎面自动排程模板实现。
 *
 * <p>该类只负责编排步骤服务和记录步骤日志，不直接实现复杂业务规则。具体算法由各步骤服务、
 * 策略和规则链扩展。</p>
 */
@Service
public class TmScheduleTemplateImpl extends AbsTmScheduleTemplate {

    private final ITmPlanBootstrapService bootstrapService;

    private final ITmInventoryPredictService inventoryPredictService;

    private final ITmPlanCalcService planCalcService;

    private final ITmTaskSortService taskSortService;

    private final ITmMachineAssignService machineAssignService;

    private final ITmSnapshotAndPersistService snapshotAndPersistService;

    private final IScheduleProcessLogger<TmScheduleContext> processLogger;

    /**
     * 创建胎面自动排程模板实现。
     *
     * @param bootstrapService          初始化步骤服务
     * @param inventoryPredictService   库存预测步骤服务
     * @param planCalcService           计划量计算步骤服务
     * @param taskSortService           任务排序步骤服务
     * @param machineAssignService      机台分配步骤服务
     * @param snapshotAndPersistService 快照和落库步骤服务
     * @param processLogger             过程日志实现，允许为空
     */
    @Autowired
    public TmScheduleTemplateImpl(ITmPlanBootstrapService bootstrapService,
                                  ITmInventoryPredictService inventoryPredictService,
                                  ITmPlanCalcService planCalcService,
                                  ITmTaskSortService taskSortService,
                                  ITmMachineAssignService machineAssignService,
                                  ITmSnapshotAndPersistService snapshotAndPersistService,
                                  @Nullable IScheduleProcessLogger<TmScheduleContext> processLogger) {
        this.bootstrapService = bootstrapService;
        this.inventoryPredictService = inventoryPredictService;
        this.planCalcService = planCalcService;
        this.taskSortService = taskSortService;
        this.machineAssignService = machineAssignService;
        this.snapshotAndPersistService = snapshotAndPersistService;
        this.processLogger = processLogger;
    }

    /**
     * 初始化
     * @param context 上下文
     */
    @Override
    protected void doBootstrap(TmScheduleContext context) {
        runStep(context, TmScheduleStepEnum.BOOTSTRAP, () -> bootstrapService.bootstrap(context));
    }

    /**
     * 计算预计库存
     * @param context 上下文
     */
    @Override
    protected void doInventoryPredict(TmScheduleContext context) {
        runStep(context, TmScheduleStepEnum.INVENTORY_PREDICT, () -> inventoryPredictService.predict(context));
    }

    @Override
    protected void doDemandAndPlanCalc(TmScheduleContext context) {
        runStep(context, TmScheduleStepEnum.PLAN_CALC, () -> planCalcService.calculate(context));
    }

    /**
     * 待排任务排序
     * @param context 上下文
     */
    @Override
    protected void doTaskSort(TmScheduleContext context) {
        runStep(context, TmScheduleStepEnum.TASK_SORT, () -> taskSortService.sort(context));
    }

    /**
     * 机台分配
     * @param context 上下文
     */
    @Override
    protected void doMachineAssign(TmScheduleContext context) {
        runStep(context, TmScheduleStepEnum.MACHINE_ASSIGN, () -> machineAssignService.assign(context));
    }

    /**
     * 执行解释快照构建和落库
     * @param context 上下文
     */
    @Override
    protected void doSnapshotAndPersist(TmScheduleContext context) {
        runStep(context, TmScheduleStepEnum.SNAPSHOT_BUILD, () -> snapshotAndPersistService.snapshotAndPersist(context));
    }

    private void runStep(TmScheduleContext context, TmScheduleStepEnum stepEnum, Runnable runnable) {
        try {
            if (processLogger != null) {
                processLogger.logStepStart(context, stepEnum.getDesc(), buildStepSummary(context, stepEnum, true));
            }
            runnable.run();
            this.appendStepCalculationDetail(context, stepEnum);
            this.updateProgress(context, stepEnum);
            if (processLogger != null) {
                processLogger.logStepEnd(context, stepEnum.getDesc(), buildStepSummary(context, stepEnum, false));
            }
        } catch (RuntimeException exception) {
            this.recordStepFailure(context, stepEnum, exception);
            throw exception;
        }
    }

    /**
     * 记录当前排程步骤的阻断异常，已有结构化错误时不重复追加。
     *
     * @param context   排程上下文
     * @param stepEnum 排程步骤
     * @param exception 原始异常
     */
    private void recordStepFailure(TmScheduleContext context, TmScheduleStepEnum stepEnum,
                                   RuntimeException exception) {
        if (context == null || context.getIssueCollector() == null) {
            return;
        }
        boolean businessError = exception instanceof ServiceException;
        TmAutoScheduleIssueCategoryEnum category = businessError
                ? TmAutoScheduleIssueCategoryEnum.AUTO_SCHEDULE_BUSINESS_ERROR
                : TmAutoScheduleIssueCategoryEnum.AUTO_SCHEDULE_SYSTEM_ERROR;
        String message = StrUtil.blankToDefault(exception.getMessage(),
                I18nUtil.getMessage("ui.data.alert.tm.schedule.taskExecuteFailed"));
        context.getIssueCollector().addFailureIssueIfAbsent(stepEnum, category, message);
    }

    /**
     * 根据步骤更新自动排程进度。
     *
     * @param context  排程上下文
     * @param stepEnum 步骤枚举
     */
    private void updateProgress(TmScheduleContext context, TmScheduleStepEnum stepEnum) {
        if (context == null || context.getProgressListener() == null) {
            return;
        }
        int progress;
        switch (stepEnum) {
            case BOOTSTRAP:
                progress = 10;
                break;
            case INVENTORY_PREDICT:
                progress = 25;
                break;
            case PLAN_CALC:
                progress = 45;
                break;
            case TASK_SORT:
                progress = 60;
                break;
            case MACHINE_ASSIGN:
                progress = 75;
                break;
            case SNAPSHOT_BUILD:
                progress = 90;
                break;
            default:
                progress = 5;
                break;
        }
        context.getProgressListener().update(progress, stepEnum.getCode(), stepEnum.getDesc());
    }
    /**
     * 构建步骤输入或输出摘要。
     *
     * @param context 排程上下文
     * @param stepEnum 步骤枚举
     * @param input    true 表示输入摘要，false 表示输出摘要
     * @return 摘要文本
     */
    private String buildStepSummary(TmScheduleContext context, TmScheduleStepEnum stepEnum, boolean input) {
        if (context == null) {
            return "排程上下文为空";
        }
        switch (stepEnum) {
            case BOOTSTRAP:
                return input ? "工厂编号=" + context.getFactoryCode() + "，排程日期="
                        + (context.getScheduleDate() == null ? null : DateUtil.formatDate(context.getScheduleDate()))
                        : "任务数量=" + context.getTaskDraftList().size() + "，机台数量="
                        + context.getMachineCandidateList().size() + "，参数数量=" + context.getParamMap().size();
            case INVENTORY_PREDICT:
                return input ? "胎面数量=" + context.getTaskDraftList().stream()
                        .map(TmTaskDraft::getTreadCode).filter(code -> code != null && code.trim().length() > 0)
                        .collect(Collectors.toSet()).size()
                        : "库存预测数量=" + context.getStockForecastMap().size();
            case PLAN_CALC:
                return input ? "任务数量=" + context.getTaskDraftList().size()
                        : "已计算计划量任务数量=" + context.getTaskDraftList().stream()
                        .filter(task -> task.getPlanQty() != null).count() + "，未排任务数量=" + context.getTaskDraftList().stream()
                        .filter(task -> task.isUnassigned() || (task.getUnplannedReasonCode() != null
                                && task.getUnplannedReasonCode().trim().length() > 0)).count();
            case TASK_SORT:
                return input ? "任务数量=" + context.getTaskDraftList().size()
                        : "任务排序=" + context.getTaskDraftList().stream().limit(10)
                        .map(TmTaskDraft::getBusinessKey).collect(Collectors.joining(","));
            case MACHINE_ASSIGN:
                return input ? "任务数量=" + context.getTaskDraftList().size()
                        : "已分配任务数量=" + context.getTaskDraftList().stream().filter(task -> !task.isUnassigned()).count()
                        + "，未排任务数量=" + context.getTaskDraftList().stream().filter(TmTaskDraft::isUnassigned).count()
                        + "，任务链数量=" + context.getTaskChainGroup().values().size();
            case SNAPSHOT_BUILD:
                return input ? "任务数量=" + context.getTaskDraftList().size()
                        : "解释快照数量=" + context.getSnapshotMap().size()
                        + "，结果数量=" + (context.getPersistResult() == null ? 0 : context.getPersistResult().getResultCount())
                        + "，未排数量=" + (context.getPersistResult() == null ? 0 : context.getPersistResult().getUnplannedCount())
                        + "，异常数量=" + (context.getPersistResult() == null ? 0 : context.getPersistResult().getErrorCount());
            default:
                return stepEnum.getDesc();
        }
    }

    /**
     * 按阶段将已产生的关键计算结果写入中文过程日志。
     *
     * @param context  排程上下文
     * @param stepEnum 已完成的排程阶段
     */
    private void appendStepCalculationDetail(TmScheduleContext context, TmScheduleStepEnum stepEnum) {
        if (context == null) {
            return;
        }
        switch (stepEnum) {
            case BOOTSTRAP:
                context.appendProcessLog("初始化完成：工厂编号={0}，排程日期={1}，批次号={2}，参数数量={3}，来源任务数量={4}，候选机台数量={5}",
                        context.getFactoryCode(), this.formatScheduleDate(context), context.getBatchNo(),
                        context.getParamMap().size(), context.getSourceTaskDraftList().size(), context.getMachineCandidateList().size());
                context.getTaskDraftList().forEach(task -> context.appendProcessLog(
                        "初始化任务：任务标识={0}，胎面编码={1}，来源工单={2}，来源班次={3}，本班需求量={4}",
                        task.getBusinessKey(), task.getTreadCode(), task.getSourceOrderNos(), task.getSourceShiftOrder(),
                        task.getCurrentShiftDemandQty()));
                break;
            case INVENTORY_PREDICT:
                context.getStockForecastMap().values().forEach(stock -> context.appendProcessLog(
                        "库存预测：胎面编码={0}，六点库存={1}，首班需求量={2}，首班计划量={3}，滚动库存={4}",
                        stock.getTreadCode(), stock.getSixClockStockQty(), stock.getFirstShiftDemandQty(),
                        stock.getFirstShiftPlanQty(), stock.getRollingStockQty()));
                break;
            case PLAN_CALC:
                context.getTaskDraftList().forEach(task -> context.appendProcessLog(
                        "计划量计算：任务标识={0}，胎面编码={1}，需求量={2}，滚动库存={3}，库存缺口={4}，损耗前计划量={5}，工装限额前计划量={6}，可用工装量={7}，已用工装量={8}，剩余工装量={9}，最终计划量={10}，计算说明={11}",
                        task.getBusinessKey(), task.getTreadCode(), task.getDemandQty(), task.getRollingStockQty(),
                        task.getStockGapQty(), task.getPreLossPlanQty(), task.getPlanQtyBeforeToolLimit(),
                        task.getAvailableToolQty(), task.getToolUsedQty(), task.getRemainingToolQty(), task.getPlanQty(),
                        task.getCalcFormulaDesc()));
                break;
            case TASK_SORT:
                context.getTaskDraftList().forEach((task) -> context.appendProcessLog(
                        "任务排序：任务标识={0}，胎面编码={1}，排序序号={2}，班次={3}，计划量={4}",
                        task.getBusinessKey(), task.getTreadCode(), task.getBaseSortIndex(), task.getShiftOrder(), task.getPlanQty()));
                break;
            case MACHINE_ASSIGN:
                context.getTaskDraftList().forEach(task -> {
                    context.appendProcessLog("机台分配：任务标识={0}，胎面编码={1}，计划量={2}，最终机台={3}，班次={4}，剩余产能={5}，未排原因={6}",
                            task.getBusinessKey(), task.getTreadCode(), task.getPlanQty(), task.getMachineCode(),
                            task.getShiftOrder(), task.getMachineRemainCapacity(), task.getUnplannedReasonDesc());
                    context.getCandidateTraceMap().getOrDefault(task.getBusinessKey(), Collections.emptyList())
                            .forEach(candidate -> context.appendProcessLog(
                                    "候选机台：任务标识={0}，机台编码={1}，是否过滤={2}，过滤原因={3}，剩余产能={4}，评分={5}",
                                    task.getBusinessKey(), candidate.getMachineCode(), candidate.isFiltered() ? "是" : "否",
                                    candidate.getFilterReasonDesc(), candidate.getRemainCapacity(), candidate.getScore()));
                });
                break;
            case SNAPSHOT_BUILD:
                context.appendProcessLog("快照构建完成：解释快照数量={0}，结果数量={1}，未排数量={2}，解释数量={3}",
                        context.getSnapshotMap().size(), context.getPersistResult() == null ? 0 : context.getPersistResult().getResultCount(),
                        context.getPersistResult() == null ? 0 : context.getPersistResult().getUnplannedCount(),
                        context.getPersistResult() == null ? 0 : context.getPersistResult().getExplainCount());
                break;
            default:
                break;
        }
    }

    /**
     * 格式化排程日期，避免测试或异常上下文未提供日期时中断过程日志记录。
     *
     * @param context 排程上下文
     * @return 格式化后的排程日期
     */
    private String formatScheduleDate(TmScheduleContext context) {
        return context == null || context.getScheduleDate() == null ? null : DateUtil.formatDate(context.getScheduleDate());
    }
}
