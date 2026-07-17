package com.zlt.aps.tc.engine.template;

import cn.hutool.core.date.DateUtil;
import com.zlt.aps.common.engine.schedule.IScheduleProcessLogger;
import com.zlt.aps.tc.api.enums.TcScheduleStepEnum;
import com.zlt.aps.tc.engine.domain.TcScheduleContext;
import com.zlt.aps.tc.engine.domain.TcTaskDraft;
import com.zlt.aps.tc.engine.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * 胎侧自动排程模板实现。
 *
 * <p>该类只负责编排步骤服务和记录步骤日志，不直接实现复杂业务规则。具体算法由各步骤服务、
 * 策略和规则链扩展。</p>
 */
@Service
public class TcScheduleTemplateImpl extends AbsTcScheduleTemplate {

    private final ITcPlanBootstrapService bootstrapService;

    private final ITcInventoryPredictService inventoryPredictService;

    private final ITcPlanCalcService planCalcService;

    private final ITcTaskSortService taskSortService;

    private final ITcMachineAssignService machineAssignService;

    private final ITcSnapshotAndPersistService snapshotAndPersistService;

    private final IScheduleProcessLogger<TcScheduleContext> processLogger;

    /**
     * 创建胎侧自动排程模板实现。
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
    public TcScheduleTemplateImpl(ITcPlanBootstrapService bootstrapService,
                                  ITcInventoryPredictService inventoryPredictService,
                                  ITcPlanCalcService planCalcService,
                                  ITcTaskSortService taskSortService,
                                  ITcMachineAssignService machineAssignService,
                                  ITcSnapshotAndPersistService snapshotAndPersistService,
                                  @Nullable IScheduleProcessLogger<TcScheduleContext> processLogger) {
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
    protected void doBootstrap(TcScheduleContext context) {
        runStep(context, TcScheduleStepEnum.BOOTSTRAP, () -> bootstrapService.bootstrap(context));
    }

    /**
     * 计算预计库存
     * @param context 上下文
     */
    @Override
    protected void doInventoryPredict(TcScheduleContext context) {
        runStep(context, TcScheduleStepEnum.INVENTORY_PREDICT, () -> inventoryPredictService.predict(context));
    }

    @Override
    protected void doDemandAndPlanCalc(TcScheduleContext context) {
        runStep(context, TcScheduleStepEnum.PLAN_CALC, () -> planCalcService.calculate(context));
    }

    /**
     * 待排任务排序
     * @param context 上下文
     */
    @Override
    protected void doTaskSort(TcScheduleContext context) {
        runStep(context, TcScheduleStepEnum.TASK_SORT, () -> taskSortService.sort(context));
    }

    /**
     * 机台分配
     * @param context 上下文
     */
    @Override
    protected void doMachineAssign(TcScheduleContext context) {
        runStep(context, TcScheduleStepEnum.MACHINE_ASSIGN, () -> machineAssignService.assign(context));
    }

    /**
     * 执行解释快照构建和落库
     * @param context 上下文
     */
    @Override
    protected void doSnapshotAndPersist(TcScheduleContext context) {
        runStep(context, TcScheduleStepEnum.SNAPSHOT_BUILD, () -> snapshotAndPersistService.snapshotAndPersist(context));
    }

    private void runStep(TcScheduleContext context, TcScheduleStepEnum stepEnum, Runnable runnable) {
        if (processLogger != null) {
            processLogger.logStepStart(context, stepEnum.getCode(), buildStepSummary(context, stepEnum, true));
        }
        // 快照与落库阶段开始前先上报 90%，核心短事务成功后再原子更新为 100%。
        if (TcScheduleStepEnum.SNAPSHOT_BUILD == stepEnum) {
            this.updateProgress(context, stepEnum);
        }
        runnable.run();
        if (TcScheduleStepEnum.SNAPSHOT_BUILD != stepEnum) {
            this.updateProgress(context, stepEnum);
        }
        if (processLogger != null) {
            processLogger.logStepEnd(context, stepEnum.getCode(), buildStepSummary(context, stepEnum, false));
        }
    }

    /**
     * 根据步骤更新自动排程进度。
     *
     * @param context  排程上下文
     * @param stepEnum 步骤枚举
     */
    private void updateProgress(TcScheduleContext context, TcScheduleStepEnum stepEnum) {
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
                progress = 0;
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
    private String buildStepSummary(TcScheduleContext context, TcScheduleStepEnum stepEnum, boolean input) {
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
                return input ? "sidewallCount=" + context.getTaskDraftList().stream()
                        .map(TcTaskDraft::getSidewallCode).filter(code -> code != null && code.trim().length() > 0)
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
                        .map(TcTaskDraft::getBusinessKey).collect(Collectors.joining(","));
            case MACHINE_ASSIGN:
                return input ? "taskCount=" + context.getTaskDraftList().size()
                        : "assignedTaskCount=" + context.getTaskDraftList().stream().filter(task -> !task.isUnassigned()).count()
                        + ",unplannedCount=" + context.getTaskDraftList().stream().filter(TcTaskDraft::isUnassigned).count()
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
