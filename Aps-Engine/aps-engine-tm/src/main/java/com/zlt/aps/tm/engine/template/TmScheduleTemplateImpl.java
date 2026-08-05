package com.zlt.aps.tm.engine.template;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.engine.schedule.IScheduleProcessLogger;
import com.zlt.aps.tm.api.enums.TmAutoScheduleIssueCategoryEnum;
import com.zlt.aps.tm.api.enums.TmScheduleStepEnum;
import com.zlt.aps.tm.engine.domain.TmMachineCandidate;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmStockForecast;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.engine.service.*;
import com.zlt.aps.tm.engine.util.TmGlueSimilarityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
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
                        : "已排序任务数量=" + context.getTaskDraftList().size();
            case MACHINE_ASSIGN:
                return input ? "任务数量=" + context.getTaskDraftList().size()
                        : "已分配任务数量=" + context.getTaskDraftList().stream().filter(task -> !task.isUnassigned()).count()
                        + "，未排任务数量=" + context.getTaskDraftList().stream().filter(TmTaskDraft::isUnassigned).count();
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
            case INVENTORY_PREDICT:
                context.getStockForecastMap().values().forEach(stock ->
                        context.appendProcessLog(this.buildInventoryFormula(stock)));
                break;
            case MACHINE_ASSIGN:
                context.getTaskDraftList().forEach(task -> {
                    context.appendProcessLog(this.buildPlanFormula(task));
                    this.appendMachineCandidateDetail(context, task);
                    if (task.isUnassigned()) {
                        context.appendProcessLog("未排任务：胎面代码={0}（胎胚号={1}），班次={2}，计划量={3}，未排原因={4}",
                                task.getTreadCode(), this.displayEmbryoCode(task.getEmbryoCode()), task.getShiftOrder(),
                                task.getPlanQty(), task.getUnplannedReasonDesc());
                    }
                });
                break;
            default:
                break;
        }
    }

    /**
     * 构建库存预测的实际计算公式。
     *
     * @param stock 库存预测结果
     * @return 中文公式文本
     */
    private String buildInventoryFormula(TmStockForecast stock) {
        BigDecimal rawRollingStock = this.nvl(stock.getSixClockStockQty())
                .add(this.nvl(stock.getFirstShiftPlanQty()))
                .subtract(this.nvl(stock.getFirstShiftDemandQty()));
        String formula = "库存预测：胎面代码=" + stock.getTreadCode() + "，滚动库存=六点库存"
                + this.nvl(stock.getSixClockStockQty()).toPlainString() + "+前日早班计划量"
                + this.nvl(stock.getFirstShiftPlanQty()).toPlainString() + "-首班消耗量"
                + this.nvl(stock.getFirstShiftDemandQty()).toPlainString() + "="
                + rawRollingStock.toPlainString();
        return rawRollingStock.compareTo(BigDecimal.ZERO) < 0
                ? formula + "，按零取值后滚动库存=" + this.nvl(stock.getRollingStockQty()).toPlainString()
                : formula;
    }

    /**
     * 构建任务在机台分配完成后的实际计划量公式。
     *
     * @param task 排程任务
     * @return 中文公式文本
     */
    private String buildPlanFormula(TmTaskDraft task) {
        List<String> adjustmentTerms = new ArrayList<>();
        this.appendSignedTerm(adjustmentTerms, "损耗率补量", task.getLossAddQty());
        this.appendSignedTerm(adjustmentTerms, "最小起排补量", task.getMinStartAdjustQty());
        this.appendSignedTerm(adjustmentTerms, "卷长取整调整", task.getTailRoundAdjustQty());
        this.appendSignedTerm(adjustmentTerms, "工装限额调整", task.getToolLimitAdjustQty());
        this.appendSignedTerm(adjustmentTerms, "机台产能调整", task.getCapacityAdjustQty());
        String baseFormula = "基础应排量" + this.nvl(task.getBaseDemandQty()).toPlainString();
        if (task.getSourceRequiredQty() != null || task.getStockDeductQty() != null) {
            baseFormula = "需求量" + this.nvl(task.getSourceRequiredQty()).toPlainString()
                    + "-库存抵扣" + this.nvl(task.getStockDeductQty()).toPlainString()
                    + "=" + baseFormula;
        }
        return "计划量计算：胎面代码=" + task.getTreadCode()
                + "，成型代码=" + this.displayEmbryoCode(task.getEmbryoCode())
                + "，是否新规格=" + this.isNewSpec(task)
                + "，是否量试/试制=" + this.isExperimentSpec(task)
                + "，" + this.displayMachineSummary(task.getLhMachineCode(), "硫化机")
                + "，深度（备库班数）=" + this.displayGuardShiftCount(task.getGuardShiftCount())
                + "，胎面长=" + this.nvl(task.getTreadShoulderLength()).toPlainString()
                + "，当班成型消耗=" + this.nvl(task.getCurrentShiftDemandQty()).toPlainString()
                + "，库存供应时长=" + this.displaySupplyHours(task.getSupplyHours())
                + "，" + this.displayGuardWindow(task.getFormingGuardWindowQtyMap())
                + "，计划量=" + baseFormula
                + String.join("", adjustmentTerms) + "=" + this.nvl(task.getPlanQty()).toPlainString();
    }

    /**
     * 判断任务是否命中新规格规则。
     *
     * @param task 排程任务
     * @return true 表示新规格
     */
    private boolean isNewSpec(TmTaskDraft task) {
        return task != null && task.getNewSpecInfo() != null && task.getNewSpecInfo().isNewSpecHit();
    }

    /**
     * 判断任务是否命中量试/试制对应的实验规格规则。
     *
     * @param task 排程任务
     * @return true 表示量试/试制
     */
    private boolean isExperimentSpec(TmTaskDraft task) {
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
            return "成型窗口内计划合计=0";
        }
        BigDecimal total = windowQtyMap.values().stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String detail = windowQtyMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> "班" + entry.getKey() + "=" + this.nvl(entry.getValue()).stripTrailingZeros().toPlainString())
                .collect(Collectors.joining(" "));
        return "成型窗口内计划合计=" + total.stripTrailingZeros().toPlainString() + "：" + detail;
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
    private void appendMachineCandidateDetail(TmScheduleContext context, TmTaskDraft task) {
        List<TmMachineCandidate> candidateList = context.getCandidateTraceMap()
                .getOrDefault(task.getBusinessKey(), Collections.emptyList());
        Map<String, List<String>> filteredMachineMap = candidateList.stream()
                .filter(TmMachineCandidate::isFiltered)
                .collect(Collectors.groupingBy(candidate -> StrUtil.blankToDefault(candidate.getFilterReasonDesc(), "未提供原因"),
                        LinkedHashMap::new, Collectors.mapping(TmMachineCandidate::getMachineCode, Collectors.toList())));
        if (!filteredMachineMap.isEmpty()) {
            context.appendProcessLog("机台筛选：胎面代码={0}，已过滤机台={1}", task.getTreadCode(), filteredMachineMap);
        }
        candidateList.stream().filter(candidate -> !candidate.isFiltered())
                .forEach(candidate -> context.appendProcessLog(this.buildScoreDetail(task, candidate)));
    }

    /**
     * 构建候选机台的中文评分明细。
     *
     * @param task 排程任务
     * @param candidate 候选机台
     * @return 中文评分文本
     */
    private String buildScoreDetail(TmTaskDraft task, TmMachineCandidate candidate) {
        Map<String, BigDecimal> scoreItems = candidate.getScoreResult() == null
                ? Collections.emptyMap() : candidate.getScoreResult().getScoreItems();
        int baseGlueCount = TmGlueSimilarityUtils.calculateIntersectionCount(
                TmGlueSimilarityUtils.parseCodeSet(task.getBaseGlueCode()),
                TmGlueSimilarityUtils.parseCodeSet(candidate.getTailBaseGlueCode()));
        return "机台评分：胎面代码=" + task.getTreadCode() + "，机台=" + candidate.getMachineCode()
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
    private String formatScheduleDate(TmScheduleContext context) {
        return context == null || context.getScheduleDate() == null ? null : DateUtil.formatDate(context.getScheduleDate());
    }
}
