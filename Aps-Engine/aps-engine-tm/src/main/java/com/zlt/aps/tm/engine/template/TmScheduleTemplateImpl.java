package com.zlt.aps.tm.engine.template;

import cn.hutool.core.date.DateUtil;
import com.zlt.aps.common.engine.schedule.IScheduleProcessLogger;
import com.zlt.aps.tm.api.enums.TmScheduleStepEnum;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.engine.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

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
        if (processLogger != null) {
            processLogger.logStepStart(context, stepEnum.getCode(), buildStepSummary(context, stepEnum, true));
        }
        runnable.run();
        if (processLogger != null) {
            processLogger.logStepEnd(context, stepEnum.getCode(), buildStepSummary(context, stepEnum, false));
        }
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
            return "context=null";
        }
        switch (stepEnum) {
            case BOOTSTRAP:
                return input ? "factoryCode=" + context.getFactoryCode() + ",scheduleDate="
                        + (context.getScheduleDate() == null ? null : DateUtil.formatDate(context.getScheduleDate()))
                        : "taskCount=" + context.getTaskDraftList().size() + ",machineCount="
                        + context.getMachineCandidateList().size() + ",paramCount=" + context.getParamMap().size();
            case INVENTORY_PREDICT:
                return input ? "treadCount=" + context.getTaskDraftList().stream()
                        .map(TmTaskDraft::getTreadCode).filter(code -> code != null && code.trim().length() > 0)
                        .collect(Collectors.toSet()).size()
                        : "stockForecastCount=" + context.getStockForecastMap().size();
            case PLAN_CALC:
                return input ? "taskCount=" + context.getTaskDraftList().size()
                        : "calculatedPlanTaskCount=" + context.getTaskDraftList().stream()
                        .filter(task -> task.getPlanQty() != null).count() + ",unplannedCount=" + context.getTaskDraftList().stream()
                        .filter(task -> task.isUnassigned() || (task.getUnplannedReasonCode() != null
                                && task.getUnplannedReasonCode().trim().length() > 0)).count();
            case TASK_SORT:
                return input ? "taskCount=" + context.getTaskDraftList().size()
                        : "taskOrder=" + context.getTaskDraftList().stream().limit(10)
                        .map(TmTaskDraft::getBusinessKey).collect(Collectors.joining(","));
            case MACHINE_ASSIGN:
                return input ? "taskCount=" + context.getTaskDraftList().size()
                        : "assignedTaskCount=" + context.getTaskDraftList().stream().filter(task -> !task.isUnassigned()).count()
                        + ",unplannedCount=" + context.getTaskDraftList().stream().filter(TmTaskDraft::isUnassigned).count()
                        + ",chainCount=" + context.getTaskChainGroup().values().size();
            case SNAPSHOT_BUILD:
                return input ? "taskCount=" + context.getTaskDraftList().size()
                        : "snapshotCount=" + context.getSnapshotMap().size()
                        + ",persistResultCount=" + (context.getPersistResult() == null ? 0 : context.getPersistResult().getResultCount())
                        + ",unplannedCount=" + (context.getPersistResult() == null ? 0 : context.getPersistResult().getUnplannedCount())
                        + ",errorCount=" + (context.getPersistResult() == null ? 0 : context.getPersistResult().getErrorCount());
            default:
                return stepEnum.getDesc();
        }
    }
}
