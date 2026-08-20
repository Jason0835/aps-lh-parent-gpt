package com.zlt.aps.tc.engine.template;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.engine.schedule.*;
import com.zlt.aps.common.engine.schedule.constraint.ScheduleToolLedgerSnapshot;
import com.zlt.aps.tc.api.enums.*;
import com.zlt.aps.tc.engine.domain.*;
import com.zlt.aps.tc.engine.service.*;
import com.zlt.aps.tc.engine.util.TcGlueSimilarityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        try {
            context.setCurrentProcessStep(stepEnum.getDesc());
            if (processLogger != null) {
                processLogger.logStepStart(context, stepEnum.getDesc(), buildStepSummary(context, stepEnum, true));
            }
            // 快照与落库阶段开始前先上报 90%，核心短事务成功后再原子更新为 100%。
            if (TcScheduleStepEnum.SNAPSHOT_BUILD == stepEnum) {
                this.updateProgress(context, stepEnum);
            }
            runnable.run();
            boolean completedInsideService = TcScheduleStepEnum.SNAPSHOT_BUILD == stepEnum
                    && context.getCompletedProcessSteps().contains(stepEnum.getDesc());
            if (!completedInsideService) {
                this.appendStepCalculationDetail(context, stepEnum);
                if (TcScheduleStepEnum.SNAPSHOT_BUILD != stepEnum) {
                    this.updateProgress(context, stepEnum);
                }
                context.getCompletedProcessSteps().add(stepEnum.getDesc());
                if (processLogger != null) {
                    processLogger.logStepEnd(context, stepEnum.getDesc(), buildStepSummary(context, stepEnum, false));
                }
                context.setCurrentProcessStep(null);
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
    private void recordStepFailure(TcScheduleContext context, TcScheduleStepEnum stepEnum,
                                   RuntimeException exception) {
        if (context == null || context.getIssueCollector() == null) {
            return;
        }
        boolean businessError = exception instanceof ServiceException;
        TcAutoScheduleIssueCategoryEnum category = businessError
                ? TcAutoScheduleIssueCategoryEnum.AUTO_SCHEDULE_BUSINESS_ERROR
                : TcAutoScheduleIssueCategoryEnum.AUTO_SCHEDULE_SYSTEM_ERROR;
        String message = StrUtil.blankToDefault(exception.getMessage(),
                I18nUtil.getMessage("ui.tc.schedule.taskExecuteFailed"));
        context.getIssueCollector().addFailureIssueIfAbsent(stepEnum, category, message);
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
            return "排程上下文为空";
        }
        switch (stepEnum) {
            case BOOTSTRAP:
                return input ? "工厂编号=" + context.getFactoryCode() + "，排程日期="
                        + (context.getScheduleDate() == null ? null : DateUtil.formatDate(context.getScheduleDate()))
                        : "任务数量=" + context.getTaskDraftList().size() + "，机台数量="
                        + context.getMachineCandidateList().size() + "，参数数量=" + context.getParamMap().size();
            case INVENTORY_PREDICT:
                return input ? "胎侧数量=" + context.getTaskDraftList().stream()
                        .map(TcTaskDraft::getSidewallCode).filter(code -> code != null && code.trim().length() > 0)
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
                        : "已排序任务数量=" + context.getTaskDraftList().size();
            case MACHINE_ASSIGN:
                return input ? "任务数量=" + context.getTaskDraftList().size()
                        : "已分配任务数量=" + context.getTaskDraftList().stream().filter(task -> !task.isUnassigned()).count()
                        + "，未排任务数量=" + context.getTaskDraftList().stream().filter(TcTaskDraft::isUnassigned).count();
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
    private void appendStepCalculationDetail(TcScheduleContext context, TcScheduleStepEnum stepEnum) {
        if (context == null) {
            return;
        }
        switch (stepEnum) {
            case INVENTORY_PREDICT:
                // 库存预测的规格事件同样需要等待 TASK_SORT 名次，避免 FULL 日志按规格编码输出。
                break;
            case PLAN_CALC:
                // 计划量初算已完成，按库存供应时长升序输出初算日志。
                this.appendInitialPlanCalculationDetail(context);
                break;
            case TASK_SORT:
                this.appendFullStepDetail(context, TcScheduleStepEnum.INVENTORY_PREDICT);
                this.appendShiftRollingInventory(context);
                this.appendFullStepDetail(context, stepEnum);
                break;
            case MACHINE_ASSIGN:
                this.appendFullStepDetail(context, stepEnum);
                this.appendShiftMachineCalculationDetail(context);
                break;
            default:
                this.appendFullStepDetail(context, stepEnum);
                break;
        }
    }

    /**
     * 按班次归集机台分配后的计划量、候选机台和未排明细。
     *
     * @param context 胎侧排程上下文
     */
    private void appendShiftMachineCalculationDetail(TcScheduleContext context) {
        Map<Integer, List<TcTaskDraft>> shiftTaskMap = this.sortedLogTaskStream(context)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(TcTaskDraft::getShiftOrder, TreeMap::new, Collectors.toList()));
        shiftTaskMap.forEach((shiftOrder, shiftTaskList) -> {
            // 仅改变过程日志展示顺序：已排任务按最终机台任务链连续成块，未排任务稳定排在最后。
            shiftTaskList.sort(this.buildMachineProcessLogTaskComparator(context));
            shiftTaskList.forEach(task -> {
                if (!task.isUnassigned()) {
                    context.appendShiftProcessLog(shiftOrder, ScheduleProcessLogSection.TOOL_LIMIT,
                            this.buildToolUsageSummary(task, context));
                }
                this.appendMachineCandidateDetail(context, task);
                if (!task.isUnassigned() && StrUtil.isNotBlank(task.getMachineCode())) {
                    context.appendShiftProcessLog(shiftOrder, ScheduleProcessLogSection.MACHINE_SELECTION_CAPACITY,
                            "机台确认：胎侧代码={0}，机台={1}",
                            task.getSidewallCode(), task.getMachineCode());
                    context.appendShiftProcessLog(shiftOrder, ScheduleProcessLogSection.MACHINE_SELECTION_CAPACITY,
                            "最终计划量重算（选机后计划量定稿）：{0}", this.buildPlanFormula(context, task));
                    this.appendFinalPlanCalculationFullDetail(context, task);
                }
                context.flushDeferredTaskProcessLogs(task);
                String unplannedReasonDesc = this.resolveProcessUnplannedReasonDesc(task);
                if (StrUtil.isNotBlank(unplannedReasonDesc)) {
                    context.appendShiftProcessLog(shiftOrder, ScheduleProcessLogSection.UNPLANNED_TASK,
                            "未排任务：胎侧代码={0}（胎胚号={1}），班次={2}，计划量={3}，未排原因={4}",
                            task.getSidewallCode(), this.displayEmbryoCode(task.getEmbryoCode()), task.getShiftOrder(),
                            task.getPlanQty(), unplannedReasonDesc);
                }
            });
        });
        context.flushRemainingDeferredTaskProcessLogs();
    }

    /**
     * 构建机台分配过程日志任务排序器。
     *
     * <p>已排任务严格按最终任务链的机台和班内序号输出；未排任务排在已排任务之后，
     * 并沿用原待排名次和业务键稳定排序。该排序器仅用于日志展示。</p>
     *
     * @param context 排程上下文
     * @return 机台分配过程日志任务排序器
     */
    private Comparator<TcTaskDraft> buildMachineProcessLogTaskComparator(TcScheduleContext context) {
        return Comparator.comparing((TcTaskDraft task) -> this.resolveFinalTaskNode(context, task) == null ? 1 : 0)
                .thenComparing(task -> this.resolveFinalMachineCode(context, task),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(task -> this.resolveFinalTaskSequence(context, task),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(this.buildLogTaskComparator());
    }

    /**
     * 获取任务对应的最终任务链节点。
     *
     * @param context 排程上下文
     * @param task    当前任务
     * @return 最终任务链节点；未进入任务链时返回 null
     */
    private ScheduleTaskNode<TcTaskDraft> resolveFinalTaskNode(TcScheduleContext context, TcTaskDraft task) {
        if (context == null || task == null || StrUtil.isBlank(task.getBusinessKey())) {
            return null;
        }
        return context.getTaskNode(task.getBusinessKey());
    }

    /**
     * 获取任务最终机台编码。
     *
     * @param context 排程上下文
     * @param task    当前任务
     * @return 最终机台编码
     */
    private String resolveFinalMachineCode(TcScheduleContext context, TcTaskDraft task) {
        ScheduleTaskNode<TcTaskDraft> node = this.resolveFinalTaskNode(context, task);
        return node == null ? null : node.getMachineCode();
    }

    /**
     * 获取任务最终班内生产序号。
     *
     * @param context 排程上下文
     * @param task    当前任务
     * @return 最终班内生产序号
     */
    private Integer resolveFinalTaskSequence(TcScheduleContext context, TcTaskDraft task) {
        ScheduleTaskNode<TcTaskDraft> node = this.resolveFinalTaskNode(context, task);
        return node == null ? null : node.getSequence();
    }

    /**
     * 解析过程日志使用的胎侧未排中文原因。
     *
     * @param task 当前排程任务
     * @return 实际中文未排原因；无法确认时返回 null
     */
    private String resolveProcessUnplannedReasonDesc(TcTaskDraft task) {
        if (task == null || !task.isUnassigned()) {
            return null;
        }
        if (StrUtil.isNotBlank(task.getUnplannedReasonDesc())) {
            return task.getUnplannedReasonDesc();
        }
        if (StrUtil.isNotBlank(task.getUnplannedReasonCode())) {
            return Arrays.stream(TcUnplannedReasonEnum.values())
                    .filter(reason -> reason.getCode().equals(task.getUnplannedReasonCode()))
                    .map(TcUnplannedReasonEnum::getDesc)
                    .findFirst()
                    .orElse(null);
        }
        if (task.getToolOverflowQty() != null
                && task.getToolOverflowQty().compareTo(BigDecimal.ZERO) > 0) {
            return TcUnplannedReasonEnum.TOOL_NOT_ENOUGH.getDesc();
        }
        return null;
    }

    /**
     * 按产品记录当前班次的库存滚动结果，班次一的班初库存为库存预测计算结果。
     *
     * @param context       胎侧排程上下文
     * @param shiftOrder    班次顺序
     * @param shiftTaskList 当前班次任务
     */
    private void appendShiftRollingInventory(TcScheduleContext context, Integer shiftOrder,
                                             List<TcTaskDraft> shiftTaskList) {
        Map<String, TcTaskDraft> productTaskMap = shiftTaskList.stream()
                .filter(task -> StrUtil.isNotBlank(task.getSidewallCode()))
                .sorted(this.buildLogTaskComparator())
                .collect(Collectors.toMap(TcTaskDraft::getSidewallCode, task -> task, (left, right) -> left,
                        LinkedHashMap::new));
        productTaskMap.forEach((sidewallCode, task) -> {
            context.appendShiftProcessLog(shiftOrder, ScheduleProcessLogSection.INVENTORY_ROLLING,
                    this.buildRollingInventoryFormula(context, task));
        });
    }

    /**
     * 在计划量计算完成后按班次记录库存滚动日志，确保机台评分和产能扣减日志在其后输出。
     *
     * @param context 胎侧排程上下文
     */
    private void appendShiftRollingInventory(TcScheduleContext context) {
        Map<Integer, List<TcTaskDraft>> shiftTaskMap = this.sortedLogTaskStream(context)
                .filter(Objects::nonNull)
                .filter(task -> task.getShiftOrder() != null)
                .collect(Collectors.groupingBy(TcTaskDraft::getShiftOrder, TreeMap::new, Collectors.toList()));
        shiftTaskMap.forEach((shiftOrder, shiftTaskList) ->
                this.appendShiftRollingInventory(context, shiftOrder, shiftTaskList));
    }

    /**
     * 获取仅用于过程日志展示的待排任务流，不修改排程上下文中的实际任务顺序。
     *
     * @param context 胎侧排程上下文
     * @return 按待排名次稳定排序的任务流
     */
    private Stream<TcTaskDraft> sortedLogTaskStream(TcScheduleContext context) {
        if (context == null || context.getTaskDraftList() == null) {
            return Stream.empty();
        }
        return context.getTaskDraftList().stream().filter(Objects::nonNull).sorted(this.buildLogTaskComparator());
    }

    /**
     * 获取计划量服务实际计算顺序的任务流，不重新排序。
     *
     * @param context 胎侧排程上下文
     * @return 按计划量计算实际顺序排列的任务流
     */
    private Stream<TcTaskDraft> planCalculationTaskStream(TcScheduleContext context) {
        if (context == null || context.getTaskDraftList() == null) {
            return Stream.empty();
        }
        return context.getTaskDraftList().stream().filter(Objects::nonNull);
    }

    /**
     * 构建计划量初算日志任务流，按库存供应时长升序排列；供应时长为空的任务排在末尾，时长相同时沿用计划量计算循环顺序。
     *
     * @param context 胎侧排程上下文
     * @return 按库存供应时长升序排列的计划量初算日志任务流
     */
    private Stream<TcTaskDraft> planCalculationLogTaskStream(TcScheduleContext context) {
        return this.planCalculationTaskStream(context)
                .sorted(Comparator.comparing(TcTaskDraft::getSupplyHours,
                        Comparator.nullsLast(Comparator.naturalOrder())));
    }

    /**
     * 构建过程日志任务排序器，以 TASK_SORT 名次为主，班次和业务键仅用于稳定兜底。
     *
     * @return 过程日志任务排序器
     */
    private Comparator<TcTaskDraft> buildLogTaskComparator() {
        return Comparator.comparing(TcTaskDraft::getBaseSortIndex,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TcTaskDraft::getShiftOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TcTaskDraft::getBusinessKey, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    /**
     * 构建班次库存滚动日志。
     *
     * @param context 胎侧排程上下文
     * @param task    当前产品班次任务
     * @return 中文库存滚动日志
     */
    private String buildRollingInventoryFormula(TcScheduleContext context, TcTaskDraft task) {
        return "库存滚动：胎侧代码=" + task.getSidewallCode() + "，班次=" + task.getShiftOrder()
                + "，班初滚动库存=" + this.buildOpeningStockFormula(context, task);
    }

    /**
     * 构建当前任务班初滚动库存的可审计计算公式。
     *
     * @param context 排程上下文
     * @param task    当前胎侧班次任务
     * @return 班初滚动库存计算公式
     */
    private String buildOpeningStockFormula(TcScheduleContext context, TcTaskDraft task) {
        TcTaskDraft previousTask = context.getTaskDraftList().stream()
                .filter(candidate -> candidate != null
                        && Objects.equals(task.getSidewallCode(), candidate.getSidewallCode())
                        && candidate.getShiftOrder() != null
                        && task.getShiftOrder() != null
                        && candidate.getShiftOrder() < task.getShiftOrder())
                .max(Comparator.comparing(TcTaskDraft::getShiftOrder))
                .orElse(null);
        if (previousTask != null) {
            BigDecimal previousOpeningStockQty = this.nvl(previousTask.getRollingStockQty());
            BigDecimal previousPlanQty = this.nvl(previousTask.getPlanQty());
            BigDecimal previousDemandQty = this.nvl(previousTask.getCurrentShiftDemandQty());
            BigDecimal rawOpeningStockQty = previousOpeningStockQty.add(previousPlanQty).subtract(previousDemandQty);
            return this.nvl(task.getRollingStockQty()).toPlainString() + "=上一班班初库存"
                    + previousOpeningStockQty.toPlainString() + "+上一班计划量"
                    + previousPlanQty.toPlainString() + "-上一班成型消耗" + previousDemandQty.toPlainString()
                    + "=" + rawOpeningStockQty.toPlainString();
        }
        TcStockForecast stockForecast = context.getStockForecastMap().get(task.getSidewallCode());
        if (stockForecast != null) {
            BigDecimal sixClockStockQty = this.nvl(stockForecast.getSixClockStockQty());
            BigDecimal previousPlanQty = this.nvl(stockForecast.getFirstShiftPlanQty());
            BigDecimal formingDemandQty = this.nvl(stockForecast.getFirstShiftDemandQty());
            BigDecimal rawOpeningStockQty = sixClockStockQty.add(previousPlanQty).subtract(formingDemandQty);
            return this.nvl(task.getRollingStockQty()).toPlainString() + "=6点库存"
                    + sixClockStockQty.toPlainString() + "+前日早班计划量"
                    + previousPlanQty.toPlainString() + "-成型消耗" + formingDemandQty.toPlainString()
                    + "=" + rawOpeningStockQty.toPlainString();
        }
        return this.nvl(task.getRollingStockQty()).toPlainString();
    }

    /**
     * 将当前步骤即时产生的结构化证据转换为中文 FULL 事件。
     *
     * @param context  排程上下文
     * @param stepEnum 已完成步骤
     */
    private void appendFullStepDetail(TcScheduleContext context, TcScheduleStepEnum stepEnum) {
        if (TcScheduleStepEnum.BOOTSTRAP == stepEnum) {
            this.appendBootstrapFullDetail(context);
        }
        if (TcScheduleStepEnum.INVENTORY_PREDICT == stepEnum) {
            this.sortedLogTaskStream(context)
                    .filter(task -> StrUtil.isNotBlank(task.getSidewallCode()))
                    .collect(Collectors.toMap(TcTaskDraft::getSidewallCode, task -> task, (left, right) -> left,
                            LinkedHashMap::new))
                    .keySet().stream()
                    .map(context.getStockForecastMap()::get)
                    .filter(Objects::nonNull)
                    .forEach(stock -> context.appendShiftFullProcessTrace(1, ScheduleProcessLogSection.INVENTORY_ROLLING,
                            new ScheduleProcessTraceEvent(
                            stepEnum.getDesc(), StrUtil.blankToDefault(stock.getSidewallCode(), "未知胎侧"), "班次库存滚动",
                            "排程日前库存快照、前日首班计划和成型首班需求。",
                            "六点库存=" + this.nvl(stock.getSixClockStockQty()) + "米，前日早班计划量="
                                    + this.nvl(stock.getFirstShiftPlanQty()) + "米，首班消耗量="
                                    + this.nvl(stock.getFirstShiftDemandQty()) + "米。",
                            "滚动库存=max(六点库存+前日早班计划量-首班消耗量,0)。",
                            this.buildInventoryFormula(stock),
                            "滚动库存=" + this.nvl(stock.getRollingStockQty()) + "米。",
                            "作为该胎侧首个待排任务的班初库存，并随任务逐班滚动。"
                    )));
        }
        this.appendUnrenderedRuleTrace(context, stepEnum);
        if (TcScheduleStepEnum.PLAN_CALC == stepEnum) {
            this.planCalculationLogTaskStream(context).forEach(task -> context.appendShiftFullProcessTrace(
                    task.getShiftOrder(), ScheduleProcessLogSection.PLAN_QTY_CALCULATION,
                    this.buildPlanCalculationFullEvent(context, task, false)));
        }
        if (TcScheduleStepEnum.MACHINE_ASSIGN == stepEnum) {
            this.sortedLogTaskStream(context)
                    .filter(task -> !task.isUnassigned())
                    .forEach(task -> context.appendShiftFullProcessTrace(task.getShiftOrder(),
                            ScheduleProcessLogSection.TOOL_LIMIT, this.buildToolUsageFullEvent(context, task)));
        }
        if (TcScheduleStepEnum.PLAN_CALC == stepEnum) {
            this.planCalculationLogTaskStream(context).forEach(task -> context.appendShiftFullProcessTrace(task.getShiftOrder(),
                    ScheduleProcessLogSection.PLAN_QTY_CALCULATION, new ScheduleProcessTraceEvent(
                    stepEnum.getDesc(), task.getBusinessKey(), "库存供应时长计算",
                    "当前班班初滚动库存、保证范围内成型需求和保证范围总小时数。",
                    "滚动库存=" + this.nvl(task.getRollingStockQty()) + "米，逐班成型需求="
                            + task.getFormingGuardWindowQtyMap() + "，逐班实际时长="
                            + task.getFormingGuardWindowHoursMap() + "小时。",
                    "按成型班次顺序逐班扣减库存：完整覆盖则累计该班实际时长；首个不能完整覆盖的班次按剩余库存占该班需求的比例折算时长并停止。",
                    this.buildSupplyHoursFormula(task),
                    "库存供应时长=" + this.displaySupplyHours(task.getSupplyHours()) + "。",
                    "作为任务排序的库存紧急度指标，并用于后续缺料时点推算。"
            )));
        }
        if (TcScheduleStepEnum.TASK_SORT == stepEnum) {
            for (int index = 0; index < context.getTaskDraftList().size(); index++) {
                TcTaskDraft task = context.getTaskDraftList().get(index);
                context.appendShiftFullProcessTrace(task.getShiftOrder(), ScheduleProcessLogSection.PLAN_QTY_CALCULATION,
                        new ScheduleProcessTraceEvent(
                        stepEnum.getDesc(), task.getBusinessKey(), "任务排序最终名次",
                        "任务排序策略和任务结构化规则证据。",
                        "库存供应时长=" + this.displaySupplyHours(task.getSupplyHours()) + "，班次="
                                + task.getShiftOrder() + "，胎侧=" + task.getSidewallCode() + "。",
                        "按配置的排序策略比较库存紧急度、开班优先级、胶料和口型连续性；空值按规则证据中的缺省口径处理。",
                        "当前任务与前序任务逐项比较后位于第" + (index + 1) + "名。",
                        "最终排序名次=" + (index + 1) + "。",
                        "按此顺序依次进入机台过滤和评分。"
                ));
            }
        }
        if (TcScheduleStepEnum.MACHINE_ASSIGN == stepEnum) {
            this.sortedLogTaskStream(context)
                    .filter(task -> StrUtil.isNotBlank(this.resolveProcessUnplannedReasonDesc(task)))
                    .forEach(task -> {
                        String unplannedReasonDesc = this.resolveProcessUnplannedReasonDesc(task);
                        context.appendShiftFullProcessTrace(task.getShiftOrder(), ScheduleProcessLogSection.UNPLANNED_TASK,
                                new ScheduleProcessTraceEvent(
                                stepEnum.getDesc(), task.getBusinessKey(), "未排判定",
                                "机台过滤、胎侧/垫胶共用机台约束、评分、产能拆分和顺延后的任务状态。",
                                "计划量=" + this.nvl(task.getPlanQty()) + "米。",
                                "没有满足全部硬约束且具备可用产能的目标时，保存剩余计划量为未排任务；胎侧机台容量无效时按5500米回退。",
                                "候选机台逐项处理后仍有剩余量=" + this.nvl(task.getPlanQty()) + "米。",
                                "未排原因=" + unplannedReasonDesc + "。",
                                "写入未排记录和解释记录，不写入已排结果。"
                        ));
                    });
        }
    }

    /**
     * 记录批次初始化、有效参数及基础资料装载来源。
     *
     * @param context 排程上下文
     */
    private void appendBootstrapFullDetail(TcScheduleContext context) {
        context.appendFullProcessTrace(new ScheduleProcessTraceEvent(
                TcScheduleStepEnum.BOOTSTRAP.getDesc(), "批次级", "批次初始化",
                "自动排程请求和胎侧基础资料加载服务。",
                "批次=" + context.getBatchNo() + "，工厂=" + context.getFactoryCode() + "，排程日期="
                        + (context.getScheduleDate() == null ? "未提供" : DateUtil.formatDate(context.getScheduleDate())) + "。",
                "按工厂和排程日期装载有效参数、成型需求、施工资料、库存、班次、胎侧/垫胶共用机台和可用机台。",
                "来源任务=" + context.getSourceTaskDraftList().size() + "条，待排任务="
                        + context.getTaskDraftList().size() + "条，可用机台=" + context.getMachineCandidateList().size() + "台。",
                "初始化完成，参数=" + context.getParamMap().size() + "项，日志级别=" + context.getProcessLogLevel()
                        + "；整车率使用 TC_VEHICLE_RATE，机台容量无效时回退5500米。",
                "进入库存预测；所有后续事件沿用本批次和任务业务键。"
        ));
        context.getParamMap().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            TcParamValue value = entry.getValue();
            context.appendFullProcessTrace(new ScheduleProcessTraceEvent(
                    TcScheduleStepEnum.BOOTSTRAP.getDesc(), "批次级", "有效参数快照",
                    "胎侧参数表或代码缺省值（" + entry.getKey() + "）。",
                    "配置值=" + StrUtil.blankToDefault(value.getParamValue(), "未配置") + "，默认值="
                            + StrUtil.blankToDefault(value.getDefaultValue(), "未配置") + "。",
                    "参数表有有效值时优先使用参数表，否则使用默认值。",
                    "有效值=" + StrUtil.blankToDefault(value.getEffectiveValue(), "未提供") + "，来源="
                            + StrUtil.blankToDefault(value.getSource(), "未提供") + "。",
                    "本批次固定使用有效值=" + StrUtil.blankToDefault(value.getEffectiveValue(), "未提供") + "。",
                    "供后续对应业务规则计算使用，批次运行中不再重新读取。"
            ));
        });
    }

    /**
     * 消费当前步骤新增的规则证据，保证同一证据只写入一次 FULL 日志。
     *
     * @param context  排程上下文
     * @param stepEnum 当前步骤
     */
    private void appendUnrenderedRuleTrace(TcScheduleContext context, TcScheduleStepEnum stepEnum) {
        context.getRuleTraceMap().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            TcRuleTrace trace = entry.getValue();
            if (trace == null) {
                return;
            }
            int startIndex = context.getProcessRuleTraceCursorMap().getOrDefault(entry.getKey(), 0);
            List<TcRuleTraceItem> ruleHits = trace.getRuleHits();
            for (int index = startIndex; index < ruleHits.size(); index++) {
                TcRuleTraceItem item = ruleHits.get(index);
                String ruleName = Arrays.stream(TcScheduleRuleCodeEnum.values())
                        .filter(rule -> rule.getCode().equals(item.getRuleCode()))
                        .map(TcScheduleRuleCodeEnum::getDesc).findFirst().orElse("补充业务规则");
                String resultName = Arrays.stream(TcScheduleRuleResultEnum.values())
                        .filter(result -> result.getCode().equals(item.getResult()))
                        .map(TcScheduleRuleResultEnum::getDesc).findFirst().orElse("未说明结果");
                String evidenceText = ScheduleProcessEvidenceFormatter.format(item.getEvidence());
                context.appendShiftFullProcessTrace(this.resolveTaskShiftOrder(context, entry.getKey()),
                        this.resolveStepProcessLogSection(stepEnum), new ScheduleProcessTraceEvent(
                        stepEnum.getDesc(), entry.getKey(), ruleName,
                        "当前计算位置即时写入的结构化规则证据（" + item.getRuleCode() + "）。",
                        evidenceText,
                        "执行“" + ruleName + "”并保留调整前值、规则参数和业务条件。",
                        "按证据字段代入本规则：" + evidenceText,
                        "规则结果=" + resultName + "（" + item.getResult() + "）。",
                        this.resolveRuleDestination(stepEnum)
                ));
            }
            context.getProcessRuleTraceCursorMap().put(entry.getKey(), ruleHits.size());
        });
    }

    /**
     * 获取规则证据在各步骤之后的结果去向。
     *
     * @param stepEnum 当前步骤
     * @return 中文结果去向
     */
    private String resolveRuleDestination(TcScheduleStepEnum stepEnum) {
        switch (stepEnum) {
            case BOOTSTRAP:
                return "进入库存预测和计划量计算。";
            case INVENTORY_PREDICT:
                return "回写任务滚动库存并进入计划量计算。";
            case PLAN_CALC:
                return "回写当前计划量分量并进入任务排序。";
            case TASK_SORT:
                return "确定基础排序名次并进入机台分配。";
            case MACHINE_ASSIGN:
                return "更新机台、班次、数量或未排状态并进入落库。";
            default:
                return "写入结果和解释证据。";
        }
    }

    /**
     * 输出计划量初算过程日志。
     *
     * @param context 胎侧排程上下文
     */
    private void appendInitialPlanCalculationDetail(TcScheduleContext context) {
        this.appendFullStepDetail(context, TcScheduleStepEnum.PLAN_CALC);
        this.planCalculationLogTaskStream(context).forEach(task ->
                context.appendShiftProcessLog(task.getShiftOrder(), ScheduleProcessLogSection.PLAN_QTY_CALCULATION,
                        "计划量初算：{0}", this.buildPlanFormula(context, task)));
    }

    /**
     * 输出机台确认后的最终计划量 FULL 事件。
     *
     * @param context 胎侧排程上下文
     * @param task    已完成机台确认的任务
     */
    private void appendFinalPlanCalculationFullDetail(TcScheduleContext context, TcTaskDraft task) {
        context.appendShiftFullProcessTrace(task.getShiftOrder(), ScheduleProcessLogSection.MACHINE_SELECTION_CAPACITY,
                this.buildPlanCalculationFullEvent(context, task, true));
    }

    /**
     * 构建计划量初算或机台确认后的最终计划量 FULL 事件。
     *
     * @param context            胎侧排程上下文
     * @param task               排程任务
     * @param finalRecalculation 是否为机台确认后的最终重算
     * @return 计划量计算过程事件
     */
    private ScheduleProcessTraceEvent buildPlanCalculationFullEvent(TcScheduleContext context, TcTaskDraft task,
                                                                     boolean finalRecalculation) {
        return new ScheduleProcessTraceEvent(
                finalRecalculation ? TcScheduleStepEnum.MACHINE_ASSIGN.getDesc()
                        : TcScheduleStepEnum.PLAN_CALC.getDesc(),
                task.getBusinessKey(), finalRecalculation ? "最终计划量重算（选机后计划量定稿）" : "计划量初算",
                "成型来源任务、滚动库存、施工资料、本批次参数和即时规则证据。",
                "胎侧=" + task.getSidewallCode() + "，班次=" + task.getShiftOrder()
                        + "，来源需求=" + this.nvl(task.getSourceRequiredQty()) + "米，库存抵扣="
                        + this.nvl(task.getStockDeductQty()) + "米。",
                "依次执行需求汇总、库存抵扣、新规格/实验规格、损耗、最小起排、卷长取整、工装和产能约束。",
                this.buildPlanFormula(context, task),
                "当前最终计划量=" + this.nvl(task.getPlanQty()) + "米，未排标记="
                        + (task.isUnassigned() ? "是" : "否") + "。",
                finalRecalculation ? "进入结果、未排和解释记录的数量分摊。" : "进入任务排序和机台候选计算。"
        );
    }

    /**
     * 将 FULL 规则证据步骤映射到班次过程日志分区。
     *
     * @param stepEnum 排程步骤
     * @return 过程日志分区
     */
    private ScheduleProcessLogSection resolveStepProcessLogSection(TcScheduleStepEnum stepEnum) {
        if (TcScheduleStepEnum.INVENTORY_PREDICT == stepEnum) {
            return ScheduleProcessLogSection.INVENTORY_ROLLING;
        }
        if (TcScheduleStepEnum.MACHINE_ASSIGN == stepEnum) {
            return ScheduleProcessLogSection.MACHINE_SELECTION_CAPACITY;
        }
        return ScheduleProcessLogSection.PLAN_QTY_CALCULATION;
    }

    /**
     * 根据任务业务键定位规则证据所属班次。
     *
     * @param context     胎侧排程上下文
     * @param businessKey 任务业务键
     * @return 班次顺序；无法定位时返回 null 并按批次级输出
     */
    private Integer resolveTaskShiftOrder(TcScheduleContext context, String businessKey) {
        return context.getTaskDraftList().stream()
                .filter(Objects::nonNull)
                .filter(task -> Objects.equals(businessKey, task.getBusinessKey()))
                .map(TcTaskDraft::getShiftOrder)
                .findFirst().orElse(null);
    }

    /**
     * 构建库存预测的实际计算公式。
     *
     * @param stock 库存预测结果
     * @return 中文公式文本
     */
    private String buildInventoryFormula(TcStockForecast stock) {
        BigDecimal rawRollingStock = this.nvl(stock.getSixClockStockQty())
                .add(this.nvl(stock.getFirstShiftPlanQty()))
                .subtract(this.nvl(stock.getFirstShiftDemandQty()));
        String formula = "库存滚动：胎侧代码=" + stock.getSidewallCode() + "，班次=1，班初滚动库存=六点库存"
                + this.nvl(stock.getSixClockStockQty()).toPlainString() + "+前日早班计划量"
                + this.nvl(stock.getFirstShiftPlanQty()).toPlainString() + "-成型消耗"
                + this.nvl(stock.getFirstShiftDemandQty()).toPlainString() + "="
                + rawRollingStock.toPlainString();
        return formula;
    }

    /**
     * 获取过程日志使用的原始成型当班需求量，避免产能拆分任务的结算值覆盖原始需求。
     *
     * @param task 当前排程任务
     * @return 原始成型当班需求量，单位米
     */
    private BigDecimal resolveOriginalCurrentShiftDemandQty(TcTaskDraft task) {
        if (task == null) {
            return BigDecimal.ZERO;
        }
        return task.getOriginalCurrentShiftDemandQty() == null
                ? this.nvl(task.getCurrentShiftDemandQty()) : task.getOriginalCurrentShiftDemandQty();
    }

    /**
     * 构建任务计划量及计划调整公式。
     *
     * @param task 排程任务
     * @return 中文公式文本
     */
    private String buildPlanFormula(TcScheduleContext context, TcTaskDraft task) {
        List<String> adjustmentTerms = new ArrayList<>();
        this.appendSignedTerm(adjustmentTerms, "损耗率补量", task.getLossAddQty());
        this.appendSignedTerm(adjustmentTerms, "最小起排补量", task.getMinStartAdjustQty());
        this.appendSignedTerm(adjustmentTerms, "卷长取整调整", task.getTailRoundAdjustQty());
        this.appendSignedTerm(adjustmentTerms, "工装限额调整", task.getToolLimitAdjustQty());
        this.appendSignedTerm(adjustmentTerms, "机台产能调整", task.getCapacityAdjustQty());
        BigDecimal originalCurrentShiftDemandQty = this.resolveOriginalCurrentShiftDemandQty(task);
        BigDecimal currentShiftDemandQty = this.nvl(task.getCurrentShiftDemandQty());
        String baseFormula = "基础应排量" + this.nvl(task.getBaseDemandQty()).toPlainString();
        if (task.getSourceRequiredQty() != null || task.getStockDeductQty() != null) {
            baseFormula = "需求量" + this.nvl(task.getSourceRequiredQty()).toPlainString()
                    + "-库存抵扣" + this.nvl(task.getStockDeductQty()).toPlainString()
                    + "=" + baseFormula;
        }
        return "计划量计算：胎侧代码=" + task.getSidewallCode()
                + "，成型代码=" + this.displayEmbryoCode(task.getEmbryoCode())
                + "，是否新规格=" + this.isNewSpec(task)
                + "，是否量试/试制=" + this.isExperimentSpec(task)
                + "，是否收尾=" + TcYesNoEnum.YES.getCode().equals(task.getTailFlag())
                + "，" + this.displayMachineSummary(task.getCxMachineCode(), "成型机")
                + "，深度（备库班数）=" + this.displayGuardShiftCount(task.getGuardShiftCount())
                + "，胎侧长=" + this.nvl(task.getSidewallLength()).toPlainString()
                + "，当班成型消耗=" + originalCurrentShiftDemandQty.toPlainString()
                + "，当前任务结算成型消耗=" + currentShiftDemandQty.toPlainString()
                + "，\n库存供应时长=" + this.displaySupplyHours(task.getSupplyHours())
                + "，" + this.displayGuardWindow(task.getFormingGuardWindowQtyMap())
                + "，" + this.buildSupplyHoursFormula(task)
                + "，\n" + this.buildStockDeductFormula(task)
                + "，计划量=" + baseFormula
                + String.join("", adjustmentTerms) + "=" + this.nvl(task.getPlanQty()).toPlainString();
    }

    /**
     * 按排程阶段构建计划量过程公式；工装账本只在机台分配完成后追加。
     *
     * @param context  排程上下文
     * @param task     排程任务
     * @param stepEnum 当前排程阶段
     * @return 当前阶段对应的计划量及工装过程公式
     */
    private String buildStepCalculationFormula(TcScheduleContext context, TcTaskDraft task,
                                               TcScheduleStepEnum stepEnum) {
        String planFormula = this.buildPlanFormula(context, task);
        if (TcScheduleStepEnum.MACHINE_ASSIGN != stepEnum || task.isUnassigned()) {
            return planFormula;
        }
        return planFormula + "，\n" + this.buildToolUsageSummary(task, context);
    }

    /**
     * 构建 FULL 模式的工装限制过程事件。
     *
     * @param context 排程上下文
     * @param task    排程任务
     * @return 工装限制过程事件
     */
    private ScheduleProcessTraceEvent buildToolUsageFullEvent(TcScheduleContext context, TcTaskDraft task) {
        return new ScheduleProcessTraceEvent(
                TcScheduleStepEnum.MACHINE_ASSIGN.getDesc(), task.getBusinessKey(), "工装限制",
                "工装总量、当前任务计划量、当班需求量和有效卷曲长度。",
                "胎侧=" + task.getSidewallCode() + "，计划量=" + this.nvl(task.getPlanQty())
                        + "米，当班需求量=" + this.nvl(task.getCurrentShiftDemandQty()) + "米。",
                "按现有工装账本计算当前任务净占用和下一任务可用工装。",
                this.buildToolUsageSummary(task, context),
                "工装限制计算结果已写入工装分区。",
                "进入机台筛选、评分和确认。"
        );
    }

    /**
     * 构建任务工装账本摘要，便于过程日志直接审计工装池占用和卷曲长度口径。
     *
     * @param context 排程上下文
     * @param task 当前排程任务
     * @return 工装账本中文摘要
     */
    private String buildToolUsageSummary(TcTaskDraft task, TcScheduleContext context) {
        if (task.getTotalToolQty() == null) {
            return "工装限制：可用工装米数=未计算（未启用工装约束），本任务净占用工装米数=未计算，剩余工装米数=未计算，有效卷曲长度=未计算；TC_TOOL_TOTAL_QTY未配置或非正数，可用工装数量、工装允许最大计划量、净占用工装数量和剩余工装数量均未计算";
        }
        boolean taskCurlLengthEffective = task.getCurlRollLength() != null
                && task.getCurlRollLength().compareTo(BigDecimal.ZERO) > 0;
        BigDecimal effectiveCurlLength = taskCurlLengthEffective ? task.getCurlRollLength()
                : task.getDefaultCurlRollLength();
        if (effectiveCurlLength == null || effectiveCurlLength.compareTo(BigDecimal.ZERO) <= 0) {
            return "工装限制：可用工装米数=未计算，本任务净占用工装米数=未计算，剩余工装米数=未计算，有效卷曲长度=未计算；任务卷曲长度和默认卷曲长度均未配置或非正数，无法计算可用工装数量对应产量、净占用工装数量和剩余工装数量";
        }
        ScheduleToolLedgerSnapshot snapshot = context == null ? null
                : context.getToolLedgerSnapshotMap().get(task.getBusinessKey());
        BigDecimal availableToolQty = snapshot == null ? task.getAvailableToolQty() : snapshot.getAvailableToolQty();
        BigDecimal toolUsedQty = snapshot == null ? task.getToolUsedQty() : snapshot.getToolUsedQty();
        BigDecimal remainingToolQty = snapshot == null ? task.getRemainingToolQty() : snapshot.getRemainingToolQty();
        return "工装限制：可用工装数量=" + this.displayToolQuantity(availableToolQty) + "套"
                + "；本任务净占用工装数量=(" + this.displayToolQuantity(task.getPlanQty())
                + "-" + this.displayToolQuantity(task.getCurrentShiftDemandQty()) + ")÷"
                + this.displayToolQuantity(effectiveCurlLength) + "="
                + this.displayToolQuantity(toolUsedQty) + "套"
                + "；下一任务可用工装数量=min(max(" + this.displayToolQuantity(availableToolQty)
                + "套-" + this.displaySubtractedToolQuantity(toolUsedQty) + ",0),"
                + this.displayToolQuantity(task.getTotalToolQty()) + "套)="
                + this.displayToolQuantity(remainingToolQty) + "套";
    }

    /**
     * 格式化剩余工装公式中的被减数，负数使用括号避免出现连续减号。
     *
     * @param quantity 净占用工装数量
     * @return 带套数单位的公式文本
     */
    private String displaySubtractedToolQuantity(BigDecimal quantity) {
        String quantityText = this.displayToolQuantity(quantity) + "套";
        return quantity != null && quantity.compareTo(BigDecimal.ZERO) < 0
                ? "(" + quantityText + ")" : quantityText;
    }

    /**
     * 将工装数量按有效卷曲长度换算为过程日志使用的米数。
     *
     * @param toolQuantity 工装数量
     * @param curlLength 有效卷曲长度
     * @return 工装米数；缺失输入时返回未计算
     */
    private String displayToolMeter(BigDecimal toolQuantity, BigDecimal curlLength) {
        if (toolQuantity == null || curlLength == null || curlLength.compareTo(BigDecimal.ZERO) <= 0) {
            return "未计算";
        }
        return toolQuantity.multiply(curlLength).stripTrailingZeros().toPlainString();
    }

    /**
     * 格式化工装账本数值，缺失值不以零替代，避免误导工装约束是否生效。
     *
     * @param quantity 待展示数值
     * @return 去除无效小数位后的数值文本；未计算时返回对应标记
     */
    private String displayToolQuantity(BigDecimal quantity) {
        return quantity == null ? "未计算" : quantity.stripTrailingZeros().toPlainString();
    }

    /**
     * 构建库存供应时长的实际代入公式。
     *
     * @param task 排程任务
     * @return 中文公式文本
     */
    private String buildSupplyHoursFormula(TcTaskDraft task) {
        ScheduleSupplyDurationResult result = ScheduleSupplyDurationCalculator.calculate(task.getRollingStockQty(),
                task.getFormingGuardWindowQtyMap(), task.getFormingGuardWindowHoursMap());
        return result.getCalculationDetail();
    }

    /**
     * 构建库存抵扣的实际代入公式，明确抵扣上限为参与计划量判断的需求而不是全部班初库存。
     *
     * @param task 排程任务
     * @return 中文库存抵扣公式
     */
    private String buildStockDeductFormula(TcTaskDraft task) {
        BigDecimal rollingStockQty = this.nvl(task.getRollingStockQty());
        BigDecimal stockDeductQty = this.nvl(task.getStockDeductQty());
        if (Boolean.TRUE.equals(task.getTwoShiftStockCovered())) {
            BigDecimal twoShiftDemandQty = this.nvl(task.getTwoShiftDemandQty());
            // 两班库存足够时直接输出业务判断，避免将需求抵扣量误解为滚动库存总量。
            String coverageRelation = twoShiftDemandQty.compareTo(rollingStockQty) < 0 ? "<" : "=";
            return "库存抵扣判断：当班及下一班需求" + twoShiftDemandQty.stripTrailingZeros().toPlainString() + "米"
                    + coverageRelation + "班初滚动库存" + rollingStockQty.stripTrailingZeros().toPlainString()
                    + "米，不需排产。";
        }
        BigDecimal currentShiftDemandQty = this.nvl(task.getCurrentShiftDemandQty());
        BigDecimal guardDemandQty = this.nvl(task.getGuardDemandQty());
        return "库存抵扣（默认策略）=min(班初滚动库存" + rollingStockQty.toPlainString()
                + "米,max(当班成型需求" + currentShiftDemandQty.toPlainString() + "米,保证范围需求"
                + guardDemandQty.toPlainString() + "米))=" + stockDeductQty.toPlainString() + "米。";
    }

    /**
     * 判断任务是否命中新规格规则。
     *
     * @param task 排程任务
     * @return true 表示新规格
     */
    private boolean isNewSpec(TcTaskDraft task) {
        return task != null && task.getNewSpecInfo() != null && task.getNewSpecInfo().isNewSpecHit();
    }

    /**
     * 判断任务是否命中量试/试制对应的实验规格规则。
     *
     * @param task 排程任务
     * @return true 表示量试/试制
     */
    private boolean isExperimentSpec(TcTaskDraft task) {
        return task != null && task.getExperimentSpecInfo() != null && task.getExperimentSpecInfo().isExperimentSpecHit();
    }

    /**
     * 展示库存保证班数，避免日志中出现空值。
     *
     * @param guardShiftCount 库存保证班数
     * @return 可展示的库存保证班数
     */
    private String displayGuardShiftCount(Integer guardShiftCount) {
        return guardShiftCount == null ? "未提供" : String.valueOf(guardShiftCount);
    }

    /** 格式化成型来源机台数量和去重后的编码列表。
     *
     * @param machineText 来源机台编码，使用英文逗号分隔
     * @param machineLabel 日志中的机台名称
     * @return 机台数量及编码展示文本
     */
    private String displayMachineSummary(String machineText, String machineLabel) {
        List<String> machineCodes = StrUtil.isBlank(machineText) ? Collections.emptyList()
                : Arrays.stream(machineText.split("[,，]"))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        return machineLabel + " " + machineCodes.size() + "台="
                + (machineCodes.isEmpty() ? "未提供" : String.join("，", machineCodes));
    }

    /** 格式化库存供应时长并追加小时单位。
     *
     * @param supplyHours 库存供应时长
     * @return 去除无意义末尾零后的时长文本
     */
    private String displaySupplyHours(BigDecimal supplyHours) {
        return supplyHours == null ? "未提供" : supplyHours.stripTrailingZeros().toPlainString() + "H";
    }

    /** 格式化成型备库窗口班次明细；明细值由加载路径按有效需求和 LH_REMAIN_QTY 封顶后写入。
     *
     * @param windowQtyMap 窗口班次到换算后长度的映射
     * @return 合计及按班次顺序排列的明细
     */
    private String displayGuardWindow(Map<Integer, BigDecimal> windowQtyMap) {
        if (windowQtyMap == null || windowQtyMap.isEmpty()) {
            return "库存供应计算窗口内成型需求合计=0";
        }
        BigDecimal total = windowQtyMap.values().stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String detail = windowQtyMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> "班" + entry.getKey() + "=" + this.nvl(entry.getValue()).stripTrailingZeros().toPlainString())
                .collect(Collectors.joining(" "));
        return "库存供应计算窗口内成型需求合计=" + total.stripTrailingZeros().toPlainString() + "：" + detail;
    }

    /**
     * 追加实际参与的计划量调整项。
     *
     * @param terms 调整项文本集合
     * @param name 调整项名称
     * @param value 调整值
     */
    private void appendSignedTerm(List<String> terms, String name, BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        terms.add((value.compareTo(BigDecimal.ZERO) > 0 ? "+" : "-") + name
                + value.abs().toPlainString());
    }

    /**
     * 追加候选机台的筛选汇总和实际评分明细。
     *
     * @param context 排程上下文
     * @param task 排程任务
     */
    private void appendMachineCandidateDetail(TcScheduleContext context, TcTaskDraft task) {
        List<TcMachineCandidate> candidateList = context.getCandidateTraceMap()
                .getOrDefault(task.getBusinessKey(), Collections.emptyList());
        if (ScheduleProcessLogLevel.FULL == context.getProcessLogLevel()) {
            candidateList.forEach(candidate -> {
                String evidenceText = ScheduleProcessEvidenceFormatter.format(candidate.getFilterEvidence());
                context.appendShiftFullProcessTrace(task.getShiftOrder(), ScheduleProcessLogSection.MACHINE_SELECTION_CAPACITY,
                        new ScheduleProcessTraceEvent(
                    TcScheduleStepEnum.MACHINE_ASSIGN.getDesc(), task.getBusinessKey(), "候选机台逐项过滤",
                    "机台候选清单、过滤规则顺序、规则开关、胎侧/垫胶共用机台配置和候选结构化证据。",
                    "机台=" + StrUtil.blankToDefault(candidate.getMachineCode(), "未提供") + "，计划量="
                            + this.nvl(task.getPlanQty()) + "米，过滤证据=" + evidenceText + "。",
                    "按配置顺序执行启用的硬约束；关闭的规则跳过，共用机台还需通过允许胎侧班次约束。",
                    "逐项检查结果由过滤策略现场写入候选证据，本日志直接翻译该证据，不重新计算：" + evidenceText,
                    candidate.isFiltered() ? "机台被拒绝；原因="
                            + StrUtil.blankToDefault(candidate.getFilterReasonDesc(), "未提供具体原因") + "。"
                            : "机台通过全部启用的过滤规则。",
                    candidate.isFiltered() ? "保留为拒绝证据，不参与后续评分。" : "进入逐评分项计算和机台优选。"
                ));
                if (!candidate.isFiltered() && candidate.getScoreResult() != null) {
                    candidate.getScoreResult().getScoreItems().forEach((scoreCode, scoreValue) ->
                        context.appendShiftFullProcessTrace(task.getShiftOrder(), ScheduleProcessLogSection.MACHINE_SELECTION_CAPACITY,
                                new ScheduleProcessTraceEvent(
                                TcScheduleStepEnum.MACHINE_ASSIGN.getDesc(), task.getBusinessKey(),
                                this.resolveScoreItemName(scoreCode),
                                "机台评分策略、任务输入、候选机台状态和参数权重（评分项编码=" + scoreCode + "）。",
                                "机台=" + candidate.getMachineCode() + "，计划量=" + this.nvl(task.getPlanQty())
                                        + "米，剩余产能=" + this.nvl(candidate.getRemainCapacity()) + "米。",
                                "评分值由当前评分策略按本批次权重和候选输入现场计算，并写入结构化评分结果。",
                                "评分项=" + scoreCode + "，现场计算得分=" + this.nvl(scoreValue) + "。",
                                "本评分项得分=" + this.nvl(scoreValue) + "；候选总分="
                                        + this.nvl(candidate.getScoreResult().getTotalScore()) + "。",
                                "与该机台其他评分项求和后参加最终机台比较。"
                            )));
                }
            });
        }
        Map<String, List<String>> filteredMachineMap = candidateList.stream()
                .filter(TcMachineCandidate::isFiltered)
                .collect(Collectors.groupingBy(candidate -> StrUtil.blankToDefault(candidate.getFilterReasonDesc(), "未提供原因"),
                        LinkedHashMap::new, Collectors.mapping(TcMachineCandidate::getMachineCode, Collectors.toList())));
        if (!filteredMachineMap.isEmpty()) {
            context.appendShiftProcessLog(task.getShiftOrder(), ScheduleProcessLogSection.MACHINE_SELECTION_CAPACITY,
                    "机台筛选：胎侧代码={0}，已过滤机台={1}", task.getSidewallCode(), filteredMachineMap);
        }
        candidateList.stream().filter(candidate -> !candidate.isFiltered())
                .forEach(candidate -> context.appendShiftProcessLog(task.getShiftOrder(),
                        ScheduleProcessLogSection.MACHINE_SELECTION_CAPACITY, this.buildScoreDetail(task, candidate)));
        if (ScheduleProcessLogLevel.FULL == context.getProcessLogLevel()) {
            context.appendShiftFullProcessTrace(task.getShiftOrder(), ScheduleProcessLogSection.MACHINE_SELECTION_CAPACITY,
                    new ScheduleProcessTraceEvent(
                    TcScheduleStepEnum.MACHINE_ASSIGN.getDesc(), task.getBusinessKey(), "最终机台选择",
                    "通过过滤的候选机台、各项评分、总分以及当前机台和班次状态。",
                    "通过候选=" + candidateList.stream().filter(candidate -> !candidate.isFiltered())
                            .map(candidate -> candidate.getMachineCode() + "=" + this.nvl(candidate.getScore()))
                            .collect(Collectors.joining("，", "[", "]")) + "。",
                    "优先选择总分最优机台；总分并列时沿用既有策略的稳定顺序和连续生产决胜口径。",
                    "比较候选总分与并列决胜条件后，回写任务机台。",
                    "最终机台=" + StrUtil.blankToDefault(task.getMachineCode(), "未选中") + "，班次="
                            + task.getShiftOrder() + "。",
                    StrUtil.isBlank(task.getMachineCode()) ? "进入顺延或未排处理。" : "进入产能扣减、拆分、合并和落库映射。"
            ));
        }
    }

    /**
     * 获取评分项中文名称。
     *
     * @param scoreCode 评分项编码
     * @return 评分项中文名称
     */
    private String resolveScoreItemName(String scoreCode) {
        Map<String, String> scoreNameMap = new LinkedHashMap<>();
        scoreNameMap.put("capacityScore", "剩余产能适配评分");
        scoreNameMap.put("mainGlueScore", "主胶料连续评分");
        scoreNameMap.put("baseGlueScore", "基部胶连续评分");
        scoreNameMap.put("mouthPlateScore", "口型连续评分");
        scoreNameMap.put("switchCostScore", "切换成本评分");
        scoreNameMap.put("fixedScore", "定点生产评分");
        return scoreNameMap.getOrDefault(scoreCode, "补充评分项（" + scoreCode + "）");
    }

    /**
     * 构建候选机台的中文评分明细。
     *
     * @param task 排程任务
     * @param candidate 候选机台
     * @return 中文评分文本
     */
    private String buildScoreDetail(TcTaskDraft task, TcMachineCandidate candidate) {
        Map<String, BigDecimal> scoreItems = candidate.getScoreResult() == null
                ? Collections.emptyMap() : candidate.getScoreResult().getScoreItems();
        int baseGlueCount = TcGlueSimilarityUtils.calculateIntersectionCount(
                TcGlueSimilarityUtils.parseCodeSet(task.getBaseGlueCode()),
                TcGlueSimilarityUtils.parseCodeSet(candidate.getTailBaseGlueCode()));
        return "机台评分：胎侧代码=" + task.getSidewallCode() + "，机台=" + candidate.getMachineCode()
                + "，剩余产能适配（计划量=" + this.nvl(task.getPlanQty()).toPlainString() + "，剩余产能="
                + this.nvl(candidate.getRemainCapacity()).toPlainString() + "）得分=" + this.scoreValue(scoreItems, "capacityScore")
                + "，主胶料连续（当前=" + StrUtil.blankToDefault(task.getGlueCode(), "无") + "，链尾="
                + StrUtil.blankToDefault(candidate.getTailMainGlueCode(), "无") + "）得分=" + this.scoreValue(scoreItems, "mainGlueScore")
                + "，基部胶相同个数=" + baseGlueCount + "，得分=" + this.scoreValue(scoreItems, "baseGlueScore")
                + "，口型连续得分=" + this.scoreValue(scoreItems, "mouthPlateScore")
                + "，切换成本（小时=" + this.nvl(candidate.getSwitchCostHours()).toPlainString() + "）得分="
                + this.scoreValue(scoreItems, "switchCostScore") + "，定点生产得分=" + this.scoreValue(scoreItems, "fixedScore")
                + "，总分=" + this.nvl(candidate.getScore()).toPlainString();
    }

    /**
     * 获取评分项值并转为普通数字文本。
     *
     * @param scoreItems 评分项集合
     * @param scoreKey 评分项键
     * @return 普通数字文本
     */
    private String scoreValue(Map<String, BigDecimal> scoreItems, String scoreKey) {
        return this.nvl(scoreItems.get(scoreKey)).toPlainString();
    }

    /**
     * 将空胎胚号转换为可追溯的中文占位文本。
     *
     * @param embryoCode 胎胚号
     * @return 可展示胎胚号
     */
    private String displayEmbryoCode(String embryoCode) {
        return StrUtil.blankToDefault(embryoCode, "未提供");
    }

    /**
     * 空数值按零处理。
     *
     * @param value 数值
     * @return 非空数值
     */
    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 格式化排程日期，避免测试或异常上下文未提供日期时中断过程日志记录。
     *
     * @param context 排程上下文
     * @return 格式化后的排程日期
     */
    private String formatScheduleDate(TcScheduleContext context) {
        return context == null || context.getScheduleDate() == null ? null : DateUtil.formatDate(context.getScheduleDate());
    }
}
