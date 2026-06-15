package com.zlt.aps.tm.engine.template;

import com.zlt.aps.common.engine.schedule.IScheduleProcessLogger;
import com.zlt.aps.tm.api.enums.TmScheduleStepEnum;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

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

    @Override
    protected void doBootstrap(TmScheduleContext context) {
        runStep(context, TmScheduleStepEnum.BOOTSTRAP, () -> bootstrapService.bootstrap(context));
    }

    @Override
    protected void doInventoryPredict(TmScheduleContext context) {
        runStep(context, TmScheduleStepEnum.INVENTORY_PREDICT, () -> inventoryPredictService.predict(context));
    }

    @Override
    protected void doDemandAndPlanCalc(TmScheduleContext context) {
        runStep(context, TmScheduleStepEnum.PLAN_CALC, () -> planCalcService.calculate(context));
    }

    @Override
    protected void doTaskSort(TmScheduleContext context) {
        runStep(context, TmScheduleStepEnum.TASK_SORT, () -> taskSortService.sort(context));
    }

    @Override
    protected void doMachineAssign(TmScheduleContext context) {
        runStep(context, TmScheduleStepEnum.MACHINE_ASSIGN, () -> machineAssignService.assign(context));
    }

    @Override
    protected void doSnapshotAndPersist(TmScheduleContext context) {
        runStep(context, TmScheduleStepEnum.SNAPSHOT_BUILD, () -> snapshotAndPersistService.snapshotAndPersist(context));
    }

    private void runStep(TmScheduleContext context, TmScheduleStepEnum stepEnum, Runnable runnable) {
        if (processLogger != null) {
            processLogger.logStepStart(context, stepEnum.getCode(), stepEnum.getDesc());
        }
        runnable.run();
        if (processLogger != null) {
            processLogger.logStepEnd(context, stepEnum.getCode(), stepEnum.getDesc());
        }
    }
}
