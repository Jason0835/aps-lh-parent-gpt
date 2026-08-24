package com.zlt.aps.tc.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.engine.quantity.PlanQuantityAllocationItem;
import com.zlt.aps.common.engine.quantity.PlanQuantityAllocationUtils;
import com.zlt.aps.common.engine.schedule.ScheduleSupplyDurationCalculator;
import com.zlt.aps.common.engine.schedule.ScheduleSupplyDurationResult;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.enums.*;
import com.zlt.aps.tc.engine.domain.*;
import com.zlt.aps.tc.engine.service.ITcPlanCalcService;
import com.zlt.aps.tc.engine.service.ITcPlanTailDecisionService;
import com.zlt.aps.tc.engine.strategy.ITcDemandQtyStrategy;
import com.zlt.aps.tc.engine.strategy.ITcPlanQtyStrategy;
import com.zlt.aps.tc.engine.strategy.ITcTaskSortStrategy;
import com.zlt.aps.tc.engine.strategy.TcStrategyRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 胎侧需求量和计划量默认计算步骤服务。
 *
 * <p>通过 {@link TcStrategyRegistry} 获取计划量策略，替代直接 new 策略对象。
 * 计划量策略编码从上下文参数读取，参数键和默认策略分别由
 * {@link TcScheduleConstants#PARAM_PLAN_QTY_STRATEGY}、{@link TcScheduleStrategyEnum#DEFAULT} 统一定义。
 * 计划量计算使用当前任务班初 rollingStockQty，同一胎侧按班次逐班回写交接班库存。</p>
 */
@Slf4j
@Service
public class TcPlanCalcService implements ITcPlanCalcService {

    private final TcStrategyRegistry strategyRegistry;

    private final ITcPlanTailDecisionService planTailDecisionService;

    /**
     * 创建计划量计算服务。
     *
     * @param strategyRegistry 胎侧策略注册表
     */
    public TcPlanCalcService(TcStrategyRegistry strategyRegistry) {
        this(strategyRegistry, new TcLegacyPlanTailDecisionService());
    }

    /**
     * 创建支持可替换收尾判定的计划量计算服务。
     *
     * @param strategyRegistry 胎侧策略注册表
     * @param planTailDecisionService 收尾判定服务
     */
    @Autowired
    public TcPlanCalcService(TcStrategyRegistry strategyRegistry,
                             ITcPlanTailDecisionService planTailDecisionService) {
        this.strategyRegistry = strategyRegistry;
        this.planTailDecisionService = planTailDecisionService;
    }

    @Override
    public void calculate(TcScheduleContext context) {
        if (context == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_CONTEXT_EMPTY.getDefaultMessage());
        }
        if (CollUtil.isEmpty(context.getTaskDraftList())) {
            return;
        }

        // 在计划量计算前按胎侧编码和班次生成唯一生产任务，原始来源任务保留在上下文中供解释落库。
        this.aggregateTaskDrafts(context);

        // 获取库存预测结果
        Map<String, TcStockForecast> stockForecastMap = context.getStockForecastMap();

        // 读取计划量策略编码，缺省 DEFAULT
        String planQtyStrategyCode = readParam(context, TcScheduleConstants.PARAM_PLAN_QTY_STRATEGY,
                TcScheduleStrategyEnum.DEFAULT.getCode());
        ITcPlanQtyStrategy planQtyStrategy = strategyRegistry.getPlanQtyStrategy(planQtyStrategyCode);
        String demandQtyAlgorithmCode = readAlgorithmCode(context);
        ITcDemandQtyStrategy demandQtyStrategy = strategyRegistry.getDemandQtyStrategy(demandQtyAlgorithmCode);

        // 初始化 per-sidewall 班初滚动库存（初值取14点预计库存），逐班回写交接班库存。
        Map<String, BigDecimal> remainingStockMap = new HashMap<>();
        if (stockForecastMap != null) {
            for (Map.Entry<String, TcStockForecast> entry : stockForecastMap.entrySet()) {
                BigDecimal rollingStock = entry.getValue().getRollingStockQty();
                remainingStockMap.put(entry.getKey(), rollingStock != null ? rollingStock : BigDecimal.ZERO);
            }
        }
        context.setInitialStockMap(new HashMap<>(remainingStockMap));
        context.setProductShiftShortageMap(new LinkedHashMap<>());
        context.setRemainingStockMap(remainingStockMap);

        // 先按班次、胎侧代码和业务键建立稳定输入，保证同班供应时长计算有确定的原始顺序。
        context.getTaskDraftList().sort(Comparator
                .comparing(TcTaskDraft::getShiftOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TcTaskDraft::getSidewallCode, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(task -> StrUtil.blankToDefault(task.getBusinessKey(), "")));
        BigDecimal remainingToolQty = this.initializeGlobalAvailableToolQty(context, stockForecastMap);
        context.setInitialAvailableToolQty(remainingToolQty);
        context.setCurrentAvailableToolQty(remainingToolQty);

        // 计划量计算按班次分两阶段执行：先确定供应时长和任务顺序，再滚动扣减计划量、库存和工装。
        Map<Integer, List<TcTaskDraft>> taskGroupByShift = new LinkedHashMap<>();
        for (TcTaskDraft task : context.getTaskDraftList()) {
            taskGroupByShift.computeIfAbsent(task == null ? null : task.getShiftOrder(), key -> new ArrayList<>())
                    .add(task);
        }
        List<TcTaskDraft> orderedTaskDraftList = new ArrayList<>();
        int planCalcOrderIndex = 0;
        for (List<TcTaskDraft> shiftTaskList : taskGroupByShift.values()) {
            this.prepareShiftDemandAndSupply(context, shiftTaskList, stockForecastMap, remainingStockMap,
                    demandQtyStrategy, demandQtyAlgorithmCode);
            this.sortPlanCalcShiftTasks(context, shiftTaskList);
            for (TcTaskDraft task : shiftTaskList) {
                planCalcOrderIndex++;
                task.setPlanCalcOrderIndex(planCalcOrderIndex);
                orderedTaskDraftList.add(task);
                remainingToolQty = this.calculatePlanQtyForTask(context, task, stockForecastMap,
                        remainingStockMap, remainingToolQty, planQtyStrategy, planQtyStrategyCode,
                        demandQtyAlgorithmCode);
            }
        }
        context.setTaskDraftList(orderedTaskDraftList);
    }

    /**
     * 预计算当前班次任务的需求量和库存供应时长，不提前扣减滚动库存、工装或计划量。
     *
     * @param context 排程上下文
     * @param shiftTaskList 当前班次任务
     * @param stockForecastMap 库存预测结果
     * @param remainingStockMap 当前胎侧滚动库存
     * @param demandQtyStrategy 需求量策略
     * @param demandQtyAlgorithmCode 需求量算法编码
     */
    private void prepareShiftDemandAndSupply(TcScheduleContext context, List<TcTaskDraft> shiftTaskList,
                                             Map<String, TcStockForecast> stockForecastMap,
                                             Map<String, BigDecimal> remainingStockMap,
                                             ITcDemandQtyStrategy demandQtyStrategy,
                                             String demandQtyAlgorithmCode) {
        for (TcTaskDraft task : shiftTaskList) {
            // 最晚开始时间依赖最终计划量，计划量前置排序不得复用旧值。
            task.setLatestStartTime(null);
            // 6点库存保留预测快照；班初滚动库存必须从上一任务回写的交接班库存读取。
            if (stockForecastMap != null && task.getSidewallCode() != null) {
                TcStockForecast forecast = stockForecastMap.get(task.getSidewallCode());
                if (forecast != null) {
                    task.setSixClockStockQty(forecast.getSixClockStockQty());
                }
            }
            if (task.getSidewallCode() != null) {
                BigDecimal rollingStock = remainingStockMap.get(task.getSidewallCode());
                if (rollingStock == null) {
                    rollingStock = nvl(task.getRollingStockQty());
                    remainingStockMap.put(task.getSidewallCode(), rollingStock);
                }
                task.setRollingStockQty(rollingStock);
            }
            // 旧骨架数据只提供 demandQty 时，将其作为当前班基础需求，避免默认策略按空值计算为 0。
            if (task.getCurrentShiftDemandQty() == null && task.getDemandQty() != null) {
                task.setCurrentShiftDemandQty(task.getDemandQty());
            }
            if (task.getOriginalCurrentShiftDemandQty() == null) {
                task.setOriginalCurrentShiftDemandQty(task.getCurrentShiftDemandQty());
            }

            TcDemandQtyResult demandQtyResult = demandQtyStrategy.calculate(buildDemandQtyInput(task), context);
            applyDemandQtyResult(task, demandQtyResult);
            addNewSpecTrace(context, task);
            addExperimentSpecTrace(context, task);
            addDemandTrace(context, task, demandQtyAlgorithmCode);
        }
    }

    /**
     * 按计划量计算阶段可获得的数据确定当前班次任务顺序。
     *
     * @param context 排程上下文
     * @param shiftTaskList 当前班次任务
     */
    private void sortPlanCalcShiftTasks(TcScheduleContext context, List<TcTaskDraft> shiftTaskList) {
        String strategyCode = readParam(context, TcScheduleConstants.PARAM_TASK_SORT_STRATEGY,
                TcScheduleStrategyEnum.DEFAULT.getCode());
        ITcTaskSortStrategy sortStrategy = strategyRegistry.getTaskSortStrategy(strategyCode);
        Comparator<TcTaskDraft> comparator = Comparator
                .comparing(TcTaskDraft::getSupplyHours, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(sortStrategy.buildComparator(context))
                .thenComparing(task -> StrUtil.blankToDefault(task.getBusinessKey(), ""));
        shiftTaskList.sort(comparator);
        log.info("[TC_PLAN_CALC_TASK_ORDER] batchNo={}, traceId={}, shiftOrder={}, strategyCode={}, order={}, rule=supplyHoursAsc_then_continuity_then_businessKey",
                context.getBatchNo(), context.getTraceId(),
                shiftTaskList.isEmpty() ? null : shiftTaskList.get(0).getShiftOrder(), strategyCode,
                shiftTaskList.stream().map(TcTaskDraft::getBusinessKey).collect(Collectors.joining(",")));
    }

    /**
     * 按已确定的计划量计算顺序完成单个任务的计划量、滚动库存和解释日志。
     *
     * @param context 排程上下文
     * @param task 当前任务
     * @param stockForecastMap 库存预测结果
     * @param remainingStockMap 当前胎侧滚动库存
     * @param remainingToolQty 当前可用工装数量
     * @param planQtyStrategy 计划量策略
     * @param planQtyStrategyCode 计划量策略编码
     * @param demandQtyAlgorithmCode 需求量算法编码
     * @return 当前任务计算完成后的可用工装数量
     */
    private BigDecimal calculatePlanQtyForTask(TcScheduleContext context, TcTaskDraft task,
                                               Map<String, TcStockForecast> stockForecastMap,
                                               Map<String, BigDecimal> remainingStockMap,
                                               BigDecimal remainingToolQty,
                                               ITcPlanQtyStrategy planQtyStrategy,
                                               String planQtyStrategyCode,
                                               String demandQtyAlgorithmCode) {
        if (stockForecastMap != null && task.getSidewallCode() != null) {
            TcStockForecast forecast = stockForecastMap.get(task.getSidewallCode());
            if (forecast != null) {
                task.setSixClockStockQty(forecast.getSixClockStockQty());
            }
        }
        if (task.getSidewallCode() != null) {
            BigDecimal rollingStock = remainingStockMap.get(task.getSidewallCode());
            if (rollingStock == null) {
                rollingStock = nvl(task.getRollingStockQty());
                remainingStockMap.put(task.getSidewallCode(), rollingStock);
            }
            task.setRollingStockQty(rollingStock);
        }
        if (task.getCurrentShiftDemandQty() == null && task.getDemandQty() != null) {
            task.setCurrentShiftDemandQty(task.getDemandQty());
        }

        // 计划量策略只读取当前任务班初全局可用工装，工装池滚动状态由本服务统一维护。
        task.setAvailableToolQty(remainingToolQty);
        BigDecimal beforeRollingStockQty = task.getRollingStockQty();
        BigDecimal beforeAvailableToolQty = remainingToolQty;

        // 打印需求量计算公式和关键中间量，便于按批次和业务键还原计划量入口。
        log.info("[TC_DEMAND_QTY_CALC] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, planCalcOrderIndex={}, businessKey={}, sidewallCode={}, shiftOrder={}, algorithmCode={}, formula=currentShiftDemandQty-rollingStockQty=>currentShiftStockGapQty,guardDemandQty-rollingStockQty=>stockGapQty,max(currentShiftStockGapQty,stockGapQty)=>demandQty",
                    context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), formatScheduleDate(context),
                    task.getPlanCalcOrderIndex(), task.getBusinessKey(), task.getSidewallCode(), task.getShiftOrder(), demandQtyAlgorithmCode);
        log.info("[TC_DEMAND_QTY_CALC_DETAIL] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, planCalcOrderIndex={}, businessKey={}, sidewallCode={}, shiftOrder={}, guardDemandQty={}, rollingStockQty={}, currentShiftStockGapQty={}, stockGapQty={}, currentShiftDemandQty={}, demandQty={}",
                    context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), formatScheduleDate(context),
                    task.getPlanCalcOrderIndex(), task.getBusinessKey(), task.getSidewallCode(), task.getShiftOrder(),
                    task.getGuardDemandQty(), task.getRollingStockQty(), task.getCurrentShiftStockGapQty(), task.getStockGapQty(),
                    task.getCurrentShiftDemandQty(), task.getDemandQty());
        // 打印供应时长计算公式和关键中间量，便于解释排序中的库存紧急度。
        log.info("[TC_DEMAND_QTY_SUPPLY] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, planCalcOrderIndex={}, businessKey={}, sidewallCode={}, shiftOrder={}, formula=逐班扣减滚动库存，完整覆盖累计实际班次时长，首个不能完整覆盖的班次按剩余库存/该班需求*该班实际时长折算后停止",
                context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), formatScheduleDate(context),
                task.getPlanCalcOrderIndex(), task.getBusinessKey(), task.getSidewallCode(), task.getShiftOrder());
        log.info("[TC_DEMAND_QTY_SUPPLY_DETAIL] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, planCalcOrderIndex={}, businessKey={}, sidewallCode={}, shiftOrder={}, supplyHours={}, rollingStockQty={}, formingGuardWindowQtyMap={}, formingGuardWindowHoursMap={}",
                context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), formatScheduleDate(context),
                task.getPlanCalcOrderIndex(), task.getBusinessKey(), task.getSidewallCode(), task.getShiftOrder(),
                task.getSupplyHours(), task.getRollingStockQty(),
                task.getFormingGuardWindowQtyMap(), task.getFormingGuardWindowHoursMap());

        // 已有计划量表示上游已完成特殊业务调整，此处保持不变。
        if (task.getPlanQty() == null) {
            TcPlanQtyResult planQtyResult = planQtyStrategy.calculate(task, context);
            applyPlanQtyResult(task, planQtyResult);
        }
        this.addTwoShiftStockCoverageTrace(context, task);
        if (!Boolean.TRUE.equals(task.getTwoShiftStockCovered())) {
            this.applyStartupThreshold(context, task);
        }
        this.applyPlanGroupResult(context, task);
        this.calculateLatestStartPriority(context, task);
        task.setToolUsedQty(BigDecimal.ZERO.setScale(TcScheduleConstants.DECIMAL_CALCULATION_SCALE,
                RoundingMode.HALF_UP));
        task.setRemainingToolQty(remainingToolQty);
        context.setCurrentAvailableToolQty(remainingToolQty);
        updateRollingStockState(context, task);
        addPlanQtyTrace(context, task, planQtyStrategyCode);
        // 打印计划量计算公式、分量和滚动状态，减少人工二次推导。
        if (task.getPlanQty() != null) {
            log.info("[TC_PLAN_QTY_CALC] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, planCalcOrderIndex={}, businessKey={}, sidewallCode={}, shiftOrder={}, strategyCode={}, calcFormulaDesc={}",
                        context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), formatScheduleDate(context),
                        task.getPlanCalcOrderIndex(), task.getBusinessKey(), task.getSidewallCode(), task.getShiftOrder(), planQtyStrategyCode,
                        task.getCalcFormulaDesc());
            log.info("[TC_PLAN_QTY_CALC_DETAIL] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, planCalcOrderIndex={}, businessKey={}, sidewallCode={}, shiftOrder={}, demandQty={}, stockDeductQty={}, baseDemandQty={}, lossAddQty={}, toolLimitAdjustQty={}, toolOverflowQty={}, minStartAdjustQty={}, tailRoundAdjustQty={}, capacityAdjustQty={}, availableToolQty={}, toolUsedQty={}, remainingToolQty={}, planStockQty={}, planQty={}, calcFormulaDesc={}",
                        context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), formatScheduleDate(context),
                        task.getPlanCalcOrderIndex(), task.getBusinessKey(), task.getSidewallCode(), task.getShiftOrder(),
                        task.getDemandQty(), task.getStockDeductQty(), task.getBaseDemandQty(),
                        task.getLossAddQty(), task.getToolLimitAdjustQty(), task.getToolOverflowQty(),
                        task.getMinStartAdjustQty(), task.getTailRoundAdjustQty(),
                        task.getCapacityAdjustQty(), task.getAvailableToolQty(),
                        task.getToolUsedQty(), task.getRemainingToolQty(), task.getPlanStockQty(), task.getPlanQty(),
                        task.getCalcFormulaDesc());
            log.info("[TC_PLAN_QTY_STATE] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, planCalcOrderIndex={}, businessKey={}, sidewallCode={}, shiftOrder={}, beforeRollingStockQty={}, afterRollingStockQty={}, beforeAvailableToolQty={}, afterRemainingToolQty={}, planStockQty={}, planQty={}",
                        context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), formatScheduleDate(context),
                        task.getPlanCalcOrderIndex(), task.getBusinessKey(), task.getSidewallCode(), task.getShiftOrder(), beforeRollingStockQty,
                        context.getRemainingStockMap().get(task.getSidewallCode()), beforeAvailableToolQty,
                        task.getRemainingToolQty(), task.getPlanStockQty(), task.getPlanQty());
            }
        return remainingToolQty;
    }

    /**
     * 按胎侧编码和班次汇总原始成型来源任务。
     *
     * @param context 排程上下文
     * @throws ServiceException 同组生产属性不一致时抛出
     */
    private void aggregateTaskDrafts(TcScheduleContext context) {
        if (CollUtil.isNotEmpty(context.getPlanTaskGroupMap())
                && CollUtil.isNotEmpty(context.getSourceTaskDraftList())) {
            return;
        }
        List<TcTaskDraft> originalTaskList = new ArrayList<>(context.getTaskDraftList());
        Map<String, List<TcTaskDraft>> groupedTaskMap = originalTaskList.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(task -> this.buildPlanGroupKey(context, task),
                        LinkedHashMap::new, Collectors.toList()));
        List<String> groupConflictMessageList = new ArrayList<>();
        groupedTaskMap.forEach((planGroupKey, sourceTaskList) -> {
            String conflictMessage = this.buildGroupAttributeConflictMessage(planGroupKey, sourceTaskList);
            if (StrUtil.isBlank(conflictMessage)) {
                return;
            }
            // 每个冲突组单独写入问题明细，便于前端按胎侧编码和班次筛选定位。
            TcTaskDraft referenceTask = sourceTaskList.get(0);
            context.getIssueCollector().addPlanGroupAttributeConflictIssue(
                    referenceTask.getSidewallCode(), referenceTask.getShiftOrder(), conflictMessage);
            groupConflictMessageList.add(conflictMessage);
        });
        if (CollUtil.isNotEmpty(groupConflictMessageList)) {
            String summaryTemplate = this.resolveI18nTemplate(
                    "ui.tc.schedule.planGroupAttributeConflictSummary",
                    "ui.tc.schedule.planGroupAttributeConflictSummary: {0}");
            throw new ServiceException(MessageFormat.format(
                    summaryTemplate,
                    String.join("；", groupConflictMessageList)));
        }
        List<TcTaskDraft> aggregateTaskList = new ArrayList<>();
        List<TcTaskDraft> sourceTaskList = new ArrayList<>();
        Map<String, TcPlanTaskGroup> planTaskGroupMap = new LinkedHashMap<>();
        for (Map.Entry<String, List<TcTaskDraft>> entry : groupedTaskMap.entrySet()) {
            String planGroupKey = entry.getKey();
            List<TcTaskDraft> groupSourceList = entry.getValue();
            TcTaskDraft aggregateTask = groupSourceList.size() == 1
                    ? groupSourceList.get(0) : new TcTaskDraft();
            if (groupSourceList.size() > 1) {
                BeanUtils.copyProperties(groupSourceList.get(0), aggregateTask);
            }
            this.planTailDecisionService.applyTailDecision(aggregateTask, groupSourceList);
            boolean formingShutdownCloseOut = groupSourceList.stream()
                    .allMatch(task -> Boolean.TRUE.equals(task.getFormingShutdownCloseOutFlag()));
            aggregateTask.setFormingShutdownCloseOutFlag(formingShutdownCloseOut);
            aggregateTask.setFormingShutdownCloseOutDemandQty(formingShutdownCloseOut
                    ? groupSourceList.stream().map(TcTaskDraft::getFormingShutdownCloseOutDemandQty)
                    .map(this::nvl).reduce(BigDecimal.ZERO, BigDecimal::add)
                    : BigDecimal.ZERO);
            if (formingShutdownCloseOut) {
                aggregateTask.setTailFlag(TcYesNoEnum.YES.getCode());
            }
            List<TcTaskDraft> sourceSnapshotList = groupSourceList.stream()
                    .map(sourceTask -> this.copySourceTask(sourceTask, planGroupKey))
                    .collect(Collectors.toList());
            BigDecimal currentShiftDemandQty = groupSourceList.stream()
                    .map(TcTaskDraft::getCurrentShiftDemandQty).map(this::nvl)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal originalCurrentShiftDemandQty = groupSourceList.stream()
                    .map(this::resolveOriginalCurrentShiftDemandQty).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal nextShiftDemandQty = groupSourceList.stream()
                    .map(TcTaskDraft::getNextShiftDemandQty).map(this::nvl)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal guardDemandQty = this.resolveGroupGuardDemandQty(groupSourceList,
                    currentShiftDemandQty);
            aggregateTask.setPlanGroupKey(planGroupKey);
            aggregateTask.setSourceTaskBusinessKeyList(sourceSnapshotList.stream()
                    .map(TcTaskDraft::getBusinessKey).collect(Collectors.toList()));
            aggregateTask.setSourceExplainTask(Boolean.FALSE);
            if (groupSourceList.size() > 1) {
                aggregateTask.setBusinessKeySuffix("PLAN_GROUP_" + Integer.toHexString(planGroupKey.hashCode()));
            }
            aggregateTask.setSourceOrderNos(groupSourceList.stream()
                    .map(TcTaskDraft::getSourceOrderNos)
                    .filter(StrUtil::isNotBlank)
                    .flatMap(value -> Arrays.stream(value.split(",")))
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .collect(Collectors.joining(",")));
            // 成型代码和成型机取来源任务去重合并值，多个编码使用英文逗号分隔，供过程日志和结果追溯。
            aggregateTask.setEmbryoCode(this.mergeDistinctText(groupSourceList, TcTaskDraft::getEmbryoCode));
            aggregateTask.setCxMachineCode(this.mergeDistinctText(groupSourceList, TcTaskDraft::getCxMachineCode));
            aggregateTask.setCurrentShiftDemandQty(currentShiftDemandQty);
            aggregateTask.setOriginalCurrentShiftDemandQty(originalCurrentShiftDemandQty);
            aggregateTask.setNextShiftDemandQty(nextShiftDemandQty);
            aggregateTask.setGuardDemandQty(guardDemandQty);
            Map<Integer, BigDecimal> groupGuardWindowQtyMap = this.resolveGroupGuardWindowQtyMap(groupSourceList);
            aggregateTask.setFormingGuardWindowQtyMap(groupGuardWindowQtyMap);
            aggregateTask.setFormingGuardWindowHoursMap(
                    this.resolveGroupGuardWindowHoursMap(context, groupGuardWindowQtyMap));
            aggregateTask.setDemandQty(null);
            if (groupSourceList.size() > 1) {
                aggregateTask.setPlanQty(null);
            }

            TcPlanTaskGroup taskGroup = new TcPlanTaskGroup();
            taskGroup.setPlanGroupKey(planGroupKey);
            taskGroup.setAggregateTask(aggregateTask);
            taskGroup.setSourceTaskList(sourceSnapshotList);
            taskGroup.setGroupCurrentShiftDemandQty(currentShiftDemandQty);
            taskGroup.setGroupNextShiftDemandQty(nextShiftDemandQty);
            taskGroup.setGroupGuardDemandQty(guardDemandQty);
            planTaskGroupMap.put(planGroupKey, taskGroup);
            aggregateTaskList.add(aggregateTask);
            sourceTaskList.addAll(sourceSnapshotList);
        }
        context.setPlanTaskGroupMap(planTaskGroupMap);
        context.setSourceTaskDraftList(sourceTaskList);
        context.setTaskDraftList(aggregateTaskList);
    }

    /**
     * 计算计划组的库存保证窗口需求量。
     *
     * <p>普通规格沿用来源任务逐项累加口径。新规格的多个来源班次提前到同一目标班次后，
     * 各来源任务的保证窗口会相互重叠；此时以最早正常目标班次的窗口为锚点，并与组内当前班
     * 需求合计取较大值，避免重复累计重叠的成型需求。</p>
     *
     * @param groupSourceList       计划组来源任务
     * @param currentShiftDemandQty 计划组当前班需求合计
     * @return 计划组库存保证窗口需求量
     */
    private BigDecimal resolveGroupGuardDemandQty(List<TcTaskDraft> groupSourceList,
                                                   BigDecimal currentShiftDemandQty) {
        boolean allNewSpec = groupSourceList.stream()
                .allMatch(task -> task.getNewSpecInfo() != null && task.getNewSpecInfo().isNewSpecHit());
        if (!allNewSpec) {
            return groupSourceList.stream()
                    .map(TcTaskDraft::getGuardDemandQty)
                    .map(this::nvl)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        BigDecimal anchorGuardDemandQty = groupSourceList.stream()
                .min(Comparator.comparing(task -> Optional.ofNullable(task.getNewSpecInfo().getNormalTargetShift())
                        .orElse(Integer.MAX_VALUE)))
                .map(TcTaskDraft::getGuardDemandQty)
                .map(this::nvl)
                .orElse(BigDecimal.ZERO);
        return anchorGuardDemandQty.max(this.nvl(currentShiftDemandQty));
    }

    /**
     * 汇总计划组逐班成型需求窗口。
     *
     * @param groupSourceList 计划组来源任务
     * @return 按逻辑班次汇总的成型需求量
     */
    private Map<Integer, BigDecimal> resolveGroupGuardWindowQtyMap(List<TcTaskDraft> groupSourceList) {
        boolean allNewSpec = groupSourceList.stream()
                .allMatch(task -> task.getNewSpecInfo() != null && task.getNewSpecInfo().isNewSpecHit());
        if (allNewSpec) {
            return groupSourceList.stream()
                    .min(Comparator.comparing(task -> Optional.ofNullable(task.getNewSpecInfo().getNormalTargetShift())
                            .orElse(Integer.MAX_VALUE)))
                    .map(TcTaskDraft::getFormingGuardWindowQtyMap)
                    .orElse(Collections.emptyMap());
        }
        Map<Integer, BigDecimal> resultMap = new LinkedHashMap<>();
        groupSourceList.stream().map(TcTaskDraft::getFormingGuardWindowQtyMap)
                .filter(Objects::nonNull).forEach(windowQtyMap -> windowQtyMap.forEach(
                        (shiftOrder, demandQty) -> resultMap.merge(shiftOrder, this.nvl(demandQty), BigDecimal::add)));
        return resultMap;
    }

    /**
     * 读取计划组逐班实际时长窗口。
     *
     * @param context 排程上下文
     * @param groupGuardWindowQtyMap 计划组逐班成型需求窗口
     * @return 与逐班需求同键的实际时长
     */
    private Map<Integer, BigDecimal> resolveGroupGuardWindowHoursMap(
            TcScheduleContext context, Map<Integer, BigDecimal> groupGuardWindowQtyMap) {
        Map<Integer, BigDecimal> resultMap = new LinkedHashMap<>();
        if (CollUtil.isEmpty(groupGuardWindowQtyMap)) {
            return resultMap;
        }
        groupGuardWindowQtyMap.keySet().stream()
                .filter(Objects::nonNull)
                .forEach(logicalShiftOrder -> {
                    int mappedShiftOrder = this.mapGuardLogicalShiftOrder(logicalShiftOrder);
                    BigDecimal shiftHours = context == null || context.getShiftHoursMap() == null
                            ? null : context.getShiftHoursMap().get(mappedShiftOrder);
                    resultMap.put(logicalShiftOrder, shiftHours);
                });
        return resultMap;
    }

    /**
     * 将库存供应窗口的逻辑班次映射到工厂实际班次配置。
     *
     * <p>逻辑班次一至六直接使用对应配置，超过六班后按三班日周期映射到一至三班，
     * 与需求加载阶段的班次时长映射保持一致。</p>
     *
     * @param logicalShiftOrder 从一开始连续增长的逻辑班次
     * @return 工厂班次配置中的实际班次顺序
     */
    private int mapGuardLogicalShiftOrder(int logicalShiftOrder) {
        if (logicalShiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER) {
            return logicalShiftOrder;
        }
        return ((logicalShiftOrder - TcScheduleConstants.TC_MAX_SHIFT_ORDER - 1) % 3) + 1;
    }

    /**
     * 构建计划量汇总组业务键。
     *
     * @param context 排程上下文
     * @param task    原始来源任务
     * @return 稳定组键；预置计划量任务追加来源业务键保持独立
     */
    private String buildPlanGroupKey(TcScheduleContext context, TcTaskDraft task) {
        String groupKey = StrUtil.blankToDefault(context.getFactoryCode(), "")
                + "|" + formatScheduleDate(context)
                + "|" + StrUtil.blankToDefault(task.getSidewallCode(), "")
                + "|" + String.valueOf(task.getShiftOrder());
        if (task.getPlanQty() != null) {
            return groupKey + "|PRESET|" + task.getBusinessKey();
        }
        return Boolean.TRUE.equals(task.getFormingShutdownCloseOutFlag())
                ? groupKey + "|FORMING_SHUTDOWN_CLOSE_OUT" : groupKey;
    }

    /**
     * 复制原始来源任务作为解释快照。
     *
     * @param sourceTask   原始来源任务
     * @param planGroupKey 汇总组业务键
     * @return 不参与后续机台分配的来源任务快照
     */
    private TcTaskDraft copySourceTask(TcTaskDraft sourceTask, String planGroupKey) {
        TcTaskDraft sourceSnapshot = new TcTaskDraft();
        BeanUtils.copyProperties(sourceTask, sourceSnapshot);
        sourceSnapshot.setPlanGroupKey(planGroupKey);
        sourceSnapshot.setSourceExplainTask(Boolean.TRUE);
        sourceSnapshot.setSourceTaskBusinessKeyList(null);
        return sourceSnapshot;
    }

    /**
     * 合并来源任务的编码字段并去重。
     *
     * <p>来源值本身可能包含多个编码（英文或中文逗号分隔），先拆分再统一去重。</p>
     *
     * @param sourceTaskList 计划组来源任务列表
     * @param valueGetter 编码取值方法引用
     * @return 英文逗号分隔的去重编码；全部为空时返回 null
     */
    private String mergeDistinctText(List<TcTaskDraft> sourceTaskList,
            Function<TcTaskDraft, String> valueGetter) {
        String mergedText = sourceTaskList.stream()
                .map(valueGetter)
                .filter(StrUtil::isNotBlank)
                .flatMap(value -> Arrays.stream(value.split("[,，]")))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.joining(","));
        return StrUtil.blankToDefault(mergedText, null);
    }

    /**
     * 构建同胎侧同班次生产属性冲突消息。
     *
     * @param planGroupKey   汇总组业务键
     * @param sourceTaskList 来源任务列表
     * @return 无冲突时返回空字符串；存在冲突时返回单个汇总组的国际化消息
     */
    private String buildGroupAttributeConflictMessage(String planGroupKey, List<TcTaskDraft> sourceTaskList) {
        if (sourceTaskList.size() <= 1) {
            return StrUtil.EMPTY;
        }
        List<String> allSourceBusinessKeyList = sourceTaskList.stream()
                .map(TcTaskDraft::getBusinessKey)
                .collect(Collectors.toList());
        if (new LinkedHashSet<>(allSourceBusinessKeyList).size() != allSourceBusinessKeyList.size()) {
            return this.formatPlanGroupAttributeConflictItem(planGroupKey, allSourceBusinessKeyList);
        }
        TcTaskDraft referenceTask = sourceTaskList.get(0);
        List<String> conflictBusinessKeyList = sourceTaskList.stream()
                .filter(task -> !Objects.equals(referenceTask.getConstructionVersion(), task.getConstructionVersion())
                        || !Objects.equals(referenceTask.getSidewallCraft(), task.getSidewallCraft())
                        || !Objects.equals(referenceTask.getGlueCode(), task.getGlueCode())
                        || !Objects.equals(referenceTask.getBaseGlueCode(), task.getBaseGlueCode())
                        || !Objects.equals(referenceTask.getMouthPlateCode(), task.getMouthPlateCode())
                        || !this.quantityEquals(referenceTask.getSidewallLength(), task.getSidewallLength())
                        || !this.quantityEquals(referenceTask.getSidewallWeight(), task.getSidewallWeight())
                        || !this.quantityEquals(referenceTask.getSidewallWearpRubberWeight(), task.getSidewallWearpRubberWeight())
                        || !this.quantityEquals(referenceTask.getCurlRollLength(), task.getCurlRollLength())
                        || !this.quantityEquals(referenceTask.getDefaultCurlRollLength(), task.getDefaultCurlRollLength())
                        || !this.quantityEquals(referenceTask.getMinStartQty(), task.getMinStartQty())
                        // 成型余量属于来源行级数据，汇总组会按独立来源累加，不作为生产属性比较。
                        || !Objects.equals(referenceTask.getTailFlag(), task.getTailFlag()))
                .map(TcTaskDraft::getBusinessKey)
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(conflictBusinessKeyList)) {
            return this.formatPlanGroupAttributeConflictItem(planGroupKey, allSourceBusinessKeyList);
        }
        return StrUtil.EMPTY;
    }

    /**
     * 格式化单个计划量汇总组的生产属性冲突消息。
     *
     * @param planGroupKey              汇总组业务键
     * @param sourceBusinessKeyList     冲突来源业务键列表
     * @return 国际化单组冲突消息
     */
    private String formatPlanGroupAttributeConflictItem(String planGroupKey,
                                                         List<String> sourceBusinessKeyList) {
        String itemTemplate = this.resolveI18nTemplate(
                "ui.tc.schedule.planGroupAttributeConflictItem",
                "ui.tc.schedule.planGroupAttributeConflictItem: {0} ({1})");
        return MessageFormat.format(itemTemplate,
                planGroupKey, String.join(",", sourceBusinessKeyList));
    }

    /**
     * 解析国际化消息模板，在脱离 Spring 国际化上下文的单元测试中保留业务参数。
     *
     * @param messageKey       国际化键
     * @param fallbackTemplate 国际化组件未解析时使用的参数化模板
     * @return 可供 {@link MessageFormat} 格式化的消息模板
     */
    private String resolveI18nTemplate(String messageKey, String fallbackTemplate) {
        String messageTemplate = I18nUtil.getMessage(messageKey);
        if (StrUtil.isBlank(messageTemplate) || messageKey.equals(messageTemplate)) {
            return fallbackTemplate;
        }
        return messageTemplate;
    }

    /**
     * 将组级计算结果分摊回原始来源任务。
     *
     * @param context       排程上下文
     * @param aggregateTask 汇总生产任务
     */
    private void applyPlanGroupResult(TcScheduleContext context, TcTaskDraft aggregateTask) {
        TcPlanTaskGroup taskGroup = context.getPlanTaskGroupMap().get(aggregateTask.getPlanGroupKey());
        if (taskGroup == null || CollUtil.isEmpty(taskGroup.getSourceTaskList())) {
            return;
        }
        boolean twoShiftStockCovered = Boolean.TRUE.equals(aggregateTask.getTwoShiftStockCovered());
        boolean useCurrentShiftDemand = nvl(aggregateTask.getCurrentShiftDemandQty())
                .compareTo(nvl(aggregateTask.getGuardDemandQty())) >= 0;
        Map<String, BigDecimal> sourceWeightMap = taskGroup.getSourceTaskList().stream()
                .collect(Collectors.toMap(TcTaskDraft::getBusinessKey,
                        sourceTask -> Boolean.TRUE.equals(aggregateTask.getFormingShutdownCloseOutFlag())
                                ? nvl(sourceTask.getFormingShutdownCloseOutDemandQty())
                                : twoShiftStockCovered
                                ? nvl(sourceTask.getCurrentShiftDemandQty()).add(nvl(sourceTask.getNextShiftDemandQty()))
                                : useCurrentShiftDemand
                                        ? nvl(sourceTask.getCurrentShiftDemandQty())
                                        : nvl(sourceTask.getGuardDemandQty()),
                        BigDecimal::add, LinkedHashMap::new));
        taskGroup.setSourceWeightMap(sourceWeightMap);
        taskGroup.setGroupBaseDemandQty(aggregateTask.getBaseDemandQty());
        taskGroup.setGroupMinStartAdjustQty(aggregateTask.getMinStartAdjustQty());
        taskGroup.setGroupRoundAdjustQty(aggregateTask.getTailRoundAdjustQty());
        taskGroup.setGroupFinalPlanQty(aggregateTask.getPlanQty());
        this.fillGroupFields(aggregateTask, taskGroup);

        Map<String, BigDecimal> stockDeductAllocationMap = this.allocateByWeight(
                aggregateTask.getStockDeductQty(), sourceWeightMap);
        Map<String, BigDecimal> baseDemandAllocationMap = this.allocateByWeight(
                aggregateTask.getBaseDemandQty(), sourceWeightMap);
        Map<String, BigDecimal> minStartAllocationMap = this.allocateByWeight(
                aggregateTask.getMinStartAdjustQty(), sourceWeightMap);
        Map<String, BigDecimal> roundAllocationMap = this.allocateByWeight(
                aggregateTask.getTailRoundAdjustQty(), sourceWeightMap);
        Map<String, BigDecimal> finalPlanAllocationMap = this.allocateByWeight(
                aggregateTask.getPlanQty(), sourceWeightMap);
        Map<String, BigDecimal> planStockAllocationMap = this.allocateByWeight(
                aggregateTask.getPlanStockQty(), sourceWeightMap);
        for (TcTaskDraft sourceTask : taskGroup.getSourceTaskList()) {
            String sourceBusinessKey = sourceTask.getBusinessKey();
            sourceTask.setSourceRequiredQty(sourceWeightMap.get(sourceBusinessKey));
            sourceTask.setStockDeductQty(stockDeductAllocationMap.get(sourceBusinessKey));
            sourceTask.setBaseDemandQty(baseDemandAllocationMap.get(sourceBusinessKey));
            sourceTask.setMinStartAdjustQty(minStartAllocationMap.get(sourceBusinessKey));
            sourceTask.setTailRoundAdjustQty(roundAllocationMap.get(sourceBusinessKey));
            sourceTask.setPlanQty(finalPlanAllocationMap.get(sourceBusinessKey));
            sourceTask.setPlanStockQty(planStockAllocationMap.get(sourceBusinessKey));
            sourceTask.setTwoShiftDemandQty(aggregateTask.getTwoShiftDemandQty());
            sourceTask.setTwoShiftStockGapQty(aggregateTask.getTwoShiftStockGapQty());
            sourceTask.setTwoShiftStockCovered(aggregateTask.getTwoShiftStockCovered());
            if (twoShiftStockCovered) {
                sourceTask.setLossAddQty(BigDecimal.ZERO);
                sourceTask.setToolLimitAdjustQty(BigDecimal.ZERO);
                sourceTask.setToolOverflowQty(BigDecimal.ZERO);
                sourceTask.setCapacityAdjustQty(BigDecimal.ZERO);
                sourceTask.setPreLossPlanQty(BigDecimal.ZERO);
                sourceTask.setPlanQtyBeforeToolLimit(BigDecimal.ZERO);
            }
            sourceTask.setCalcFormulaDesc("同胎侧同班次汇总后按来源需求分摊");
            this.fillGroupFields(sourceTask, taskGroup);
            Map<String, Object> sourceEvidence = this.buildPlanGroupEvidence(taskGroup);
            sourceEvidence.put("sourceBusinessKey", sourceBusinessKey);
            sourceEvidence.put("sourceWeight", sourceWeightMap.get(sourceBusinessKey));
            sourceEvidence.put("allocatedPlanQty", sourceTask.getPlanQty());
            traceOf(context, sourceTask).addRuleHit(TcScheduleRuleCodeEnum.PLAN_QTY_SOURCE_ALLOCATE,
                    TcScheduleRuleResultEnum.PASS, sourceEvidence);
            if (aggregateTask.getTwoShiftDemandQty() != null) {
                Map<String, Object> coverageEvidence = this.buildTwoShiftCoverageEvidence(aggregateTask);
                coverageEvidence.put("sourceBusinessKey", sourceBusinessKey);
                coverageEvidence.put("sourceNextShiftDemandQty", sourceTask.getNextShiftDemandQty());
                coverageEvidence.put("twoShiftLeadTask", sourceTask.getTwoShiftLeadTask());
                traceOf(context, sourceTask).addRuleHit(TcScheduleRuleCodeEnum.TWO_SHIFT_STOCK_COVERAGE,
                        this.resolveTwoShiftCoverageResult(aggregateTask), coverageEvidence);
            }
        }
        traceOf(context, aggregateTask).addRuleHit(TcScheduleRuleCodeEnum.PLAN_QTY_AGGREGATE,
                TcScheduleRuleResultEnum.PASS, this.buildPlanGroupEvidence(taskGroup));
        log.info("[TC_PLAN_QTY_AGGREGATE] batchNo={}, traceId={}, planGroupKey={}, sourceCount={}, currentShiftDemandQty={}, guardDemandQty={}, stockDeductQty={}, baseDemandQty={}, minStartAdjustQty={}, roundAdjustQty={}, finalPlanQty={}",
                context.getBatchNo(), context.getTraceId(), taskGroup.getPlanGroupKey(),
                taskGroup.getSourceTaskList().size(), taskGroup.getGroupCurrentShiftDemandQty(),
                taskGroup.getGroupGuardDemandQty(), aggregateTask.getStockDeductQty(),
                taskGroup.getGroupBaseDemandQty(), taskGroup.getGroupMinStartAdjustQty(),
                taskGroup.getGroupRoundAdjustQty(), taskGroup.getGroupFinalPlanQty());
    }

    /**
     * 填充任务的组级解释字段。
     *
     * @param task      待填充任务
     * @param taskGroup 计划量汇总组
     */
    private void fillGroupFields(TcTaskDraft task, TcPlanTaskGroup taskGroup) {
        task.setPlanGroupKey(taskGroup.getPlanGroupKey());
        task.setGroupSourceCount(taskGroup.getSourceTaskList().size());
        BigDecimal groupRequiredQty = Boolean.TRUE.equals(task.getFormingShutdownCloseOutFlag())
                ? nvl(task.getFormingShutdownCloseOutDemandQty())
                : Boolean.TRUE.equals(task.getTwoShiftStockCovered())
                ? nvl(taskGroup.getGroupCurrentShiftDemandQty()).add(nvl(taskGroup.getGroupNextShiftDemandQty()))
                : nvl(taskGroup.getGroupCurrentShiftDemandQty()).max(nvl(taskGroup.getGroupGuardDemandQty()));
        task.setGroupRequiredQty(groupRequiredQty);
        task.setGroupBaseDemandQty(taskGroup.getGroupBaseDemandQty());
        task.setGroupMinStartAdjustQty(taskGroup.getGroupMinStartAdjustQty());
        task.setGroupRoundAdjustQty(taskGroup.getGroupRoundAdjustQty());
        task.setGroupFinalPlanQty(taskGroup.getGroupFinalPlanQty());
    }

    /**
     * 构建组级规则证据。
     *
     * @param taskGroup 计划量汇总组
     * @return 可序列化规则证据
     */
    private Map<String, Object> buildPlanGroupEvidence(TcPlanTaskGroup taskGroup) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("planGroupKey", taskGroup.getPlanGroupKey());
        evidence.put("sourceCount", taskGroup.getSourceTaskList().size());
        evidence.put("groupCurrentShiftDemandQty", taskGroup.getGroupCurrentShiftDemandQty());
        evidence.put("groupNextShiftDemandQty", taskGroup.getGroupNextShiftDemandQty());
        evidence.put("groupGuardDemandQty", taskGroup.getGroupGuardDemandQty());
        evidence.put("groupBaseDemandQty", taskGroup.getGroupBaseDemandQty());
        evidence.put("groupMinStartAdjustQty", taskGroup.getGroupMinStartAdjustQty());
        evidence.put("groupRoundAdjustQty", taskGroup.getGroupRoundAdjustQty());
        evidence.put("groupFinalPlanQty", taskGroup.getGroupFinalPlanQty());
        evidence.put("tailDecisionMode", "LEGACY_TAIL_FLAG");
        return evidence;
    }

    /**
     * 按来源权重分摊数量。
     *
     * @param totalQty        汇总数量
     * @param sourceWeightMap 来源权重
     * @return key=来源业务键、value=分摊数量
     */
    private Map<String, BigDecimal> allocateByWeight(BigDecimal totalQty,
                                                     Map<String, BigDecimal> sourceWeightMap) {
        List<PlanQuantityAllocationItem> allocationItemList = sourceWeightMap.entrySet().stream()
                .map(entry -> new PlanQuantityAllocationItem(entry.getKey(), entry.getValue(), BigDecimal.ZERO))
                .collect(Collectors.toList());
        return PlanQuantityAllocationUtils.allocate(totalQty, allocationItemList,
                        TcScheduleConstants.DECIMAL_CALCULATION_SCALE).stream()
                .collect(Collectors.toMap(PlanQuantityAllocationItem::getSourceBusinessKey,
                        PlanQuantityAllocationItem::getAllocatedQty,
                        BigDecimal::add, LinkedHashMap::new));
    }

    /**
     * 比较两个可空数量。
     *
     * @param first  第一个数量
     * @param second 第二个数量
     * @return 数值相等返回 true
     */
    private boolean quantityEquals(BigDecimal first, BigDecimal second) {
        return nvl(first).compareTo(nvl(second)) == 0;
    }

    /**
     * 格式化排程日期，避免日志中直接打印Date对象造成排查口径不统一。
     *
     * @param context 排程上下文
     * @return yyyy-MM-dd格式日期；日期为空时返回null
     */
    private String formatScheduleDate(TcScheduleContext context) {
        return context == null || context.getScheduleDate() == null ? null : DateUtil.formatDate(context.getScheduleDate());
    }

    /**
     * 对整日停产后的首个开班应用计划量阈值上限。
     *
     * @param context 排程上下文
     * @param task    当前任务
     */
    private void applyStartupThreshold(TcScheduleContext context, TcTaskDraft task) {
        if (context.getStartupShiftOrderSet() == null
                || !context.getStartupShiftOrderSet().contains(task.getShiftOrder())) {
            return;
        }
        BigDecimal threshold = this.resolveStartupThreshold(context);
        BigDecimal currentShiftDemandQty = nvl(task.getCurrentShiftDemandQty());
        BigDecimal originalPlanQty = nvl(task.getPlanQty());
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("ruleCode", TcScheduleRuleCodeEnum.STARTUP_THRESHOLD_ADJUST.getCode());
        evidence.put("phase", "PLAN_CALC");
        evidence.put("detectionScope", "PREVIOUS_FULL_DAY_SHUTDOWN");
        evidence.put("date", this.formatScheduleDate(context));
        evidence.put("sourceShiftOrder", task.getShiftOrder());
        evidence.put("targetShiftOrder", task.getShiftOrder());
        evidence.put("shiftOrder", task.getShiftOrder());
        evidence.put("currentShiftDemandQty", currentShiftDemandQty);
        evidence.put("rollingStockQty", task.getRollingStockQty());
        evidence.put("supplyHours", task.getSupplyHours());
        evidence.put("threshold", threshold);
        evidence.put("thresholdSource", this.resolveStartupThresholdSource(context));
        evidence.put("originalPlanQty", originalPlanQty);
        if (currentShiftDemandQty.compareTo(BigDecimal.ZERO) <= 0) {
            evidence.put("skipReason", "CURRENT_SHIFT_DEMAND_NOT_POSITIVE");
            evidence.put("finalPlanQty", originalPlanQty);
            traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.STARTUP_THRESHOLD_ADJUST,
                    TcScheduleRuleResultEnum.SKIP, evidence);
            return;
        }
        BigDecimal planQtyLimit = currentShiftDemandQty.multiply(threshold)
                .subtract(nvl(task.getRollingStockQty())).max(BigDecimal.ZERO);
        BigDecimal finalPlanQty = originalPlanQty.min(planQtyLimit);
        BigDecimal originalPreLossPlanQty = task.getPreLossPlanQty() == null
                ? originalPlanQty : nvl(task.getPreLossPlanQty());
        BigDecimal originalPlanQtyBeforeToolLimit = task.getPlanQtyBeforeToolLimit() == null
                ? originalPlanQty : nvl(task.getPlanQtyBeforeToolLimit());
        task.setPlanQty(finalPlanQty);
        task.setPreLossPlanQty(originalPreLossPlanQty.min(planQtyLimit));
        task.setPlanQtyBeforeToolLimit(originalPlanQtyBeforeToolLimit.min(planQtyLimit));
        task.setPlanStockQty(nvl(task.getRollingStockQty()).add(finalPlanQty)
                .subtract(currentShiftDemandQty).max(BigDecimal.ZERO));
        // 开产阈值截断计划量后，同步最小起排与卷曲取整分量，使 baseDemand + 分量 = finalPlanQty 保持闭合，
        // 避免 applyPlanGroupResult 用未更新的分量分摊导致 plan_qty_breakdown 不闭合。
        if (finalPlanQty.compareTo(originalPlanQty) < 0) {
            task.setMinStartAdjustQty(BigDecimal.ZERO);
            task.setTailRoundAdjustQty(finalPlanQty.subtract(nvl(task.getBaseDemandQty())));
        }
        evidence.put("planQtyLimit", planQtyLimit);
        evidence.put("adjustedQty", finalPlanQty.subtract(originalPlanQty));
        evidence.put("finalPlanQty", finalPlanQty);
        traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.STARTUP_THRESHOLD_ADJUST,
                finalPlanQty.compareTo(originalPlanQty) < 0
                        ? TcScheduleRuleResultEnum.PASS : TcScheduleRuleResultEnum.SKIP,
                evidence);
    }

    /**
     * 读取开产库存覆盖阈值，非正数统一回退默认值1。
     *
     * @param context 排程上下文
     * @return 有效开产阈值
     */
    private BigDecimal resolveStartupThreshold(TcScheduleContext context) {
        BigDecimal threshold = this.readDecimalParam(context,
                TcScheduleConstants.PARAM_OPEN_SHIFT_THRESHOLD, BigDecimal.ONE);
        if (threshold.compareTo(BigDecimal.ZERO) > 0) {
            return threshold;
        }
        log.warn("[TC_PARAM_PARSE] batchNo={}, traceId={}, paramCode={}, paramValue={}, reason=NOT_POSITIVE, fallback=1",
                context.getBatchNo(), context.getTraceId(), TcScheduleConstants.PARAM_OPEN_SHIFT_THRESHOLD, threshold);
        return BigDecimal.ONE;
    }

    /**
     * 解析开产阈值来源，非法配置统一标记为默认回退。
     *
     * @param context 排程上下文
     * @return 参数来源说明
     */
    private String resolveStartupThresholdSource(TcScheduleContext context) {
        TcParamValue paramValue = context.getParamMap().get(TcScheduleConstants.PARAM_OPEN_SHIFT_THRESHOLD);
        if (paramValue == null || StrUtil.isBlank(paramValue.getEffectiveValue())) {
            return "DEFAULT";
        }
        try {
            if (new BigDecimal(paramValue.getEffectiveValue().trim()).compareTo(BigDecimal.ZERO) <= 0) {
                return "DEFAULT_INVALID";
            }
        } catch (NumberFormatException exception) {
            return "DEFAULT_INVALID";
        }
        return StrUtil.blankToDefault(paramValue.getSource(), "CONFIG");
    }
    /**
     * 计算库存不足时间、预计生产时长和最晚开始时间，并写入排序规则证据。
     *
     * <p>统一默认速度未配置或非正数、班次开始时间无法解析时不阻断排程，
     * 仅记录跳过原因并保持既有排序结果。</p>
     *
     * @param context 排程上下文
     * @param task    当前任务
     */
    private void calculateLatestStartPriority(TcScheduleContext context, TcTaskDraft task) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        BigDecimal defaultSpeed = this.readDecimalParam(context,
                TcScheduleConstants.PARAM_DEFAULT_PRODUCTION_SPEED, BigDecimal.ZERO);
        BigDecimal standingHours = this.readDecimalParam(context,
                TcScheduleConstants.PARAM_PROCESS_STANDING_HOURS, BigDecimal.ZERO);
        evidence.put("defaultProductionSpeed", defaultSpeed);
        evidence.put("processStandingHours", standingHours);
        evidence.put("planQty", task.getPlanQty());
        evidence.put("supplyHours", task.getSupplyHours());
        Date shiftStartTime = this.resolveShiftStartTime(context, task.getShiftOrder());
        evidence.put("shiftStartTime", shiftStartTime);
        // 详设§4.3 速度链尾部：TC_DEFAULT_PRODUCTION_SPEED 未配(<=0)时，用 max(启用机台MAX_CAPACITY)/当前班shiftHours 兜底
        BigDecimal resolvedSpeed = defaultSpeed;
        if (resolvedSpeed.compareTo(BigDecimal.ZERO) <= 0) {
            BigDecimal fallbackSpeed = this.resolveFallbackProductionSpeed(context, task.getShiftOrder());
            evidence.put("fallbackProductionSpeed", fallbackSpeed);
            resolvedSpeed = fallbackSpeed;
        }
        evidence.put("resolvedProductionSpeed", resolvedSpeed);
        if (resolvedSpeed.compareTo(BigDecimal.ZERO) <= 0) {
            evidence.put("reason", TcScheduleConstants.SKIP_REASON_DEFAULT_PRODUCTION_SPEED_NON_POSITIVE);
            traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.LATEST_START_PRIORITY,
                    TcScheduleRuleResultEnum.SKIP, evidence);
            return;
        }
        if (shiftStartTime == null) {
            evidence.put("reason", TcScheduleConstants.SKIP_REASON_SHIFT_START_TIME_INVALID);
            traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.LATEST_START_PRIORITY,
                    TcScheduleRuleResultEnum.SKIP, evidence);
            return;
        }
        BigDecimal supplyHours = nvl(task.getSupplyHours());
        BigDecimal estimatedProductionHours = nvl(task.getPlanQty())
                .divide(resolvedSpeed, TcScheduleConstants.DECIMAL_CALCULATION_SCALE, RoundingMode.HALF_UP);
        Date stockShortageTime = this.offsetHours(shiftStartTime, supplyHours);
        Date latestStartTime = this.offsetHours(stockShortageTime,
                standingHours.add(estimatedProductionHours).negate());
        task.setStockShortageTime(stockShortageTime);
        task.setEstimatedProductionHours(estimatedProductionHours);
        task.setLatestStartTime(latestStartTime);
        evidence.put("stockShortageTime", stockShortageTime);
        evidence.put("estimatedProductionHours", estimatedProductionHours);
        evidence.put("latestStartTime", latestStartTime);
        evidence.put("formula", "shiftStart+supplyHours-standingHours-planQty/defaultSpeed");
        traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.LATEST_START_PRIORITY,
                TcScheduleRuleResultEnum.PASS, evidence);
    }

    /**
     * 兜底解析生产速度：TC_DEFAULT_PRODUCTION_SPEED 未配置(<=0)时，按详设§4.3 速度链尾部取
     * max(启用机台 MAX_CAPACITY) / 当前班 shiftHours；机台容量无效时回退 DEFAULT_MACHINE_MAX_CAPACITY。
     *
     * @param context    排程上下文
     * @param shiftOrder 当前班次序号
     * @return 兜底生产速度(米/小时)；班次时长或机台配置缺失时返回 0
     */
    private BigDecimal resolveFallbackProductionSpeed(TcScheduleContext context, Integer shiftOrder) {
        if (context == null || shiftOrder == null || context.getShiftHoursMap() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal shiftHours = context.getShiftHoursMap().get(shiftOrder);
        if (shiftHours == null || shiftHours.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal maxCapacity = context.getMachineCandidateList().stream()
                .filter(candidate -> candidate != null
                        && !Boolean.FALSE.equals(candidate.getEnabled())
                        && candidate.getMaxCapacity() != null
                        && candidate.getMaxCapacity().signum() > 0)
                .map(candidate -> candidate.getMaxCapacity())
                .reduce(BigDecimal.ZERO, BigDecimal::max);
        if (maxCapacity.compareTo(BigDecimal.ZERO) <= 0) {
            maxCapacity = new BigDecimal(TcScheduleConstants.DEFAULT_MACHINE_MAX_CAPACITY);
        }
        return maxCapacity.divide(shiftHours, TcScheduleConstants.DECIMAL_CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 解析任务班次开始时间，第二天三个班次按班次顺序偏移一天。
     *
     * @param context    排程上下文
     * @param shiftOrder 六班任务顺序
     * @return 班次开始时间；配置缺失或格式非法时返回 null
     */
    private Date resolveShiftStartTime(TcScheduleContext context, Integer shiftOrder) {
        if (context == null || context.getScheduleDate() == null || shiftOrder == null
                || context.getShiftTimeWindowMap() == null) {
            return null;
        }
        TcShiftTimeWindow window = context.getShiftTimeWindowMap().get(shiftOrder);
        if (window == null || StrUtil.isBlank(window.getPlanStartTime())) {
            return null;
        }
        try {
            Date shiftDate = DateUtil.offsetDay(context.getScheduleDate(), (shiftOrder - 1) / 3);
            return DateUtil.parse(DateUtil.formatDate(shiftDate) + " " + window.getPlanStartTime());
        } catch (RuntimeException exception) {
            log.warn("[TC_LATEST_START_PRIORITY] batchNo={}, traceId={}, shiftOrder={}, planStartTime={}, reason=SHIFT_START_PARSE_FAILED",
                    context.getBatchNo(), context.getTraceId(), shiftOrder, window.getPlanStartTime(), exception);
            return null;
        }
    }

    /**
     * 按小时偏移时间。
     *
     * @param source 原时间
     * @param hours  偏移小时数，可为负数
     * @return 偏移后的时间
     */
    private Date offsetHours(Date source, BigDecimal hours) {
        long offsetMillis = hours.multiply(BigDecimal.valueOf(TcScheduleConstants.MILLIS_PER_HOUR))
                .setScale(0, RoundingMode.HALF_UP).longValue();
        return new Date(source.getTime() + offsetMillis);
    }

    /**
     * 读取非必填数值参数，无法解析时使用缺省值。
     *
     * @param context      排程上下文
     * @param paramCode    参数编码
     * @param defaultValue 缺省值
     * @return 参数数值
     */
    private BigDecimal readDecimalParam(TcScheduleContext context, String paramCode, BigDecimal defaultValue) {
        String value = readParam(context, paramCode, null);
        if (StrUtil.isBlank(value)) {
            return defaultValue;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            log.warn("[TC_PARAM_PARSE] batchNo={}, traceId={}, paramCode={}, paramValue={}, reason=INVALID_DECIMAL",
                    context.getBatchNo(), context.getTraceId(), paramCode, value, exception);
            return defaultValue;
        }
    }
    /**
     * 写入新规格判断和提前排产窗口证据。
     *
     * @param context 排程上下文
     * @param task    任务草稿
     */
    private void addNewSpecTrace(TcScheduleContext context, TcTaskDraft task) {
        TcNewSpecInfo info = task.getNewSpecInfo();
        if (info == null) {
            return;
        }
        Map<String, Object> detectEvidence = new LinkedHashMap<>();
        detectEvidence.put("newSpec", info.getNewSpec());
        detectEvidence.put("lookbackDays", info.getLookbackDays());
        detectEvidence.put("lookbackDaysSource", info.getLookbackDaysSource());
        detectEvidence.put("previousStockDate", info.getPreviousStockDate());
        detectEvidence.put("previousDayStockQty", info.getPreviousDayStockQty());
        detectEvidence.put("previousDayStockExists", info.getPreviousDayStockExists());
        detectEvidence.put("historyStartDate", info.getHistoryStartDate());
        detectEvidence.put("historyEndDate", info.getHistoryEndDate());
        detectEvidence.put("historySchedulePlanExists", info.getHistorySchedulePlanExists());
        detectEvidence.put("reason", info.getReason());
        traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.NEW_SPEC_DETECT,
                Boolean.TRUE.equals(info.getNewSpec())
                        ? TcScheduleRuleResultEnum.PASS : TcScheduleRuleResultEnum.SKIP,
                detectEvidence);
        if (!info.isNewSpecHit()) {
            return;
        }
        Map<String, Object> windowEvidence = new LinkedHashMap<>();
        windowEvidence.put("advanceShiftCount", info.getAdvanceShiftCount());
        windowEvidence.put("advanceShiftCountSource", info.getAdvanceShiftCountSource());
        windowEvidence.put("baseGuardShiftCount", info.getBaseGuardShiftCount());
        windowEvidence.put("effectiveGuardShiftCount", info.getEffectiveGuardShiftCount());
        windowEvidence.put("formingWindowStartClass", info.getFormingWindowStartClass());
        windowEvidence.put("formingWindowEndClass", info.getFormingWindowEndClass());
        windowEvidence.put("formingWindowEstimatedShiftCount", info.getFormingWindowEstimatedShiftCount());
        windowEvidence.put("normalTargetShift", info.getNormalTargetShift());
        windowEvidence.put("adjustedTargetShift", info.getAdjustedTargetShift());
        windowEvidence.put("adjustedTargetWindow", info.getAdjustedTargetWindow());
        windowEvidence.put("demandShift", info.getDemandShift());
        windowEvidence.put("demandQty", info.getDemandQty());
        traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.NEW_SPEC_ADVANCE_WINDOW,
                TcScheduleRuleResultEnum.PASS, windowEvidence);
    }

    /**
     * 写入实验规格判断和固定计划量证据。
     *
     * @param context 排程上下文
     * @param task    任务草稿
     */
    private void addExperimentSpecTrace(TcScheduleContext context, TcTaskDraft task) {
        TcExperimentSpecInfo info = task.getExperimentSpecInfo();
        if (info == null) {
            return;
        }
        Map<String, Object> detectEvidence = new LinkedHashMap<>();
        detectEvidence.put("experimentSpec", info.getExperimentSpec());
        detectEvidence.put("lookbackDays", info.getLookbackDays());
        detectEvidence.put("lookbackDaysSource", info.getLookbackDaysSource());
        detectEvidence.put("scheduleDate", info.getScheduleDate());
        detectEvidence.put("experimentPlanDate", info.getExperimentPlanDate());
        detectEvidence.put("monthPlanDayQty", info.getMonthPlanDayQty());
        detectEvidence.put("monthPlanIds", info.getMonthPlanIds());
        detectEvidence.put("productionNos", info.getProductionNos());
        detectEvidence.put("embryoCodes", info.getEmbryoCodes());
        detectEvidence.put("mergedToExistingTask", info.getMergedToExistingTask());
        detectEvidence.put("reason", info.getReason());
        traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.EXPERIMENT_SPEC_DETECT,
                info.isExperimentSpecHit() ? TcScheduleRuleResultEnum.PASS : TcScheduleRuleResultEnum.SKIP,
                detectEvidence);
        if (!info.isExperimentSpecHit()) {
            return;
        }
        Map<String, Object> planQtyEvidence = new LinkedHashMap<>();
        planQtyEvidence.put("planQty", info.getPlanQty());
        planQtyEvidence.put("planQtySource", info.getPlanQtySource());
        planQtyEvidence.put("finalTaskPlanQty", task.getPlanQty());
        planQtyEvidence.put("currentShiftDemandQty", task.getCurrentShiftDemandQty());
        planQtyEvidence.put("guardDemandQty", task.getGuardDemandQty());
        traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.EXPERIMENT_SPEC_PLAN_QTY,
                TcScheduleRuleResultEnum.PASS, planQtyEvidence);
    }
    /**
     * 写入需求量计算规则证据。
     *
     * @param context              排程上下文
     * @param task                 任务草稿
     * @param demandAlgorithmCode  需求量算法编码
     */
    private void addDemandTrace(TcScheduleContext context, TcTaskDraft task, String demandAlgorithmCode) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("algorithmCode", demandAlgorithmCode);
        evidence.put("currentShiftDemandQty", task.getCurrentShiftDemandQty());
        evidence.put("guardDemandQty", task.getGuardDemandQty());
        evidence.put("rollingStockQty", task.getRollingStockQty());
        evidence.put("currentShiftStockGapQty", task.getCurrentShiftStockGapQty());
        evidence.put("stockGapQty", task.getStockGapQty());
        evidence.put("demandQty", task.getDemandQty());
        evidence.put("formingGuardWindowQtyMap", task.getFormingGuardWindowQtyMap());
        evidence.put("formingGuardWindowHoursMap", task.getFormingGuardWindowHoursMap());
        boolean supplyWindowKeyConsistent = task.getFormingGuardWindowQtyMap() != null
                && task.getFormingGuardWindowHoursMap() != null
                && task.getFormingGuardWindowQtyMap().keySet()
                .equals(task.getFormingGuardWindowHoursMap().keySet());
        ScheduleSupplyDurationResult supplyDurationResult = ScheduleSupplyDurationCalculator.calculate(
                task.getRollingStockQty(), task.getFormingGuardWindowQtyMap(), task.getFormingGuardWindowHoursMap());
        evidence.put("supplyWindowKeyConsistent", supplyWindowKeyConsistent);
        evidence.put("supplyHoursInvalidReason", supplyDurationResult.getInvalidReason());
        evidence.put("supplyHours", task.getSupplyHours());
        evidence.put("sourceOrderNos", task.getSourceOrderNos());
        traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.DEMAND_QTY_CALC,
                TcScheduleRuleResultEnum.PASS, evidence);
    }

    /**
     * 写入计划量计算规则证据。
     *
     * @param context             排程上下文
     * @param task                任务草稿
     * @param planQtyStrategyCode 计划量策略编码
     */
    private void addPlanQtyTrace(TcScheduleContext context, TcTaskDraft task, String planQtyStrategyCode) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("strategyCode", planQtyStrategyCode);
        evidence.put("nextShiftDemandQty", task.getNextShiftDemandQty());
        evidence.put("twoShiftDemandQty", task.getTwoShiftDemandQty());
        evidence.put("twoShiftStockGapQty", task.getTwoShiftStockGapQty());
        evidence.put("twoShiftStockCovered", task.getTwoShiftStockCovered());
        evidence.put("planQty", task.getPlanQty());
        evidence.put("demandQty", task.getDemandQty());
        evidence.put("stockDeductQty", task.getStockDeductQty());
        evidence.put("planStockQty", task.getPlanStockQty());
        evidence.put("tailFlag", task.getTailFlag());
        evidence.put("formingShutdownCloseOutFlag", task.getFormingShutdownCloseOutFlag());
        evidence.put("formingShutdownCloseOutDemandQty", task.getFormingShutdownCloseOutDemandQty());
        evidence.put("toolOverflowQty", task.getToolOverflowQty());
        evidence.put("totalToolQty", task.getTotalToolQty());
        evidence.put("availableToolQty", task.getAvailableToolQty());
        evidence.put("toolUsedQty", task.getToolUsedQty());
        evidence.put("remainingToolQty", task.getRemainingToolQty());
        evidence.put("curlRollLength", task.getCurlRollLength());
        evidence.put("lossRate", task.getResolvedLossRate() == null ? task.getLossRate() : task.getResolvedLossRate());
        evidence.put("lossMatchLevel", task.getLossMatchLevel());
        evidence.put("lossMatchSource", task.getLossMatchSource());
        evidence.put("preLossPlanQty", task.getPreLossPlanQty());
        evidence.put("planQtyBeforeToolLimit", task.getPlanQtyBeforeToolLimit());
        evidence.put("calcFormulaDesc", task.getCalcFormulaDesc());
        traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.PLAN_QTY_CALC,
                TcScheduleRuleResultEnum.PASS, evidence);
    }

    /**
     * 写入两班库存覆盖判断证据。
     *
     * @param context 胎侧排程上下文
     * @param task    汇总后的胎侧任务
     */
    private void addTwoShiftStockCoverageTrace(TcScheduleContext context, TcTaskDraft task) {
        if (task.getTwoShiftDemandQty() == null) {
            return;
        }
        traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.TWO_SHIFT_STOCK_COVERAGE,
                this.resolveTwoShiftCoverageResult(task), this.buildTwoShiftCoverageEvidence(task));
        log.info("[TC_TWO_SHIFT_STOCK_COVERAGE] batchNo={}, traceId={}, businessKey={}, sidewallCode={}, shiftOrder={}, currentShiftDemandQty={}, nextShiftDemandQty={}, rollingStockQty={}, twoShiftDemandQty={}, twoShiftStockGapQty={}, stockCovered={}, planQty={}",
                context.getBatchNo(), context.getTraceId(), task.getBusinessKey(), task.getSidewallCode(),
                task.getShiftOrder(), task.getCurrentShiftDemandQty(), task.getNextShiftDemandQty(),
                task.getRollingStockQty(), task.getTwoShiftDemandQty(), task.getTwoShiftStockGapQty(),
                task.getTwoShiftStockCovered(), task.getPlanQty());
    }

    /**
     * 构建两班库存覆盖判断的结构化证据。
     *
     * @param task 胎侧任务
     * @return 可写入解释 JSON 的证据
     */
    private Map<String, Object> buildTwoShiftCoverageEvidence(TcTaskDraft task) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        boolean newSpecBypass = task.getNewSpecInfo() != null && task.getNewSpecInfo().isNewSpecHit();
        boolean experimentSpecBypass = task.getExperimentSpecInfo() != null
                && task.getExperimentSpecInfo().isExperimentSpecHit();
        evidence.put("applicable", !newSpecBypass && !experimentSpecBypass && task.getPlanQty() != null);
        evidence.put("bypassReason", newSpecBypass ? "NEW_SPEC_ADVANCE"
                : experimentSpecBypass ? "EXPERIMENT_SPEC_PLAN_QTY" : null);
        evidence.put("currentShiftDemandQty", task.getCurrentShiftDemandQty());
        evidence.put("nextShiftDemandQty", task.getNextShiftDemandQty());
        evidence.put("rollingStockQty", task.getRollingStockQty());
        evidence.put("twoShiftDemandQty", task.getTwoShiftDemandQty());
        evidence.put("twoShiftStockGapQty", task.getTwoShiftStockGapQty());
        evidence.put("stockCovered", task.getTwoShiftStockCovered());
        evidence.put("twoShiftLeadTask", task.getTwoShiftLeadTask());
        evidence.put("finalPlanQty", task.getPlanQty());
        return evidence;
    }

    /**
     * 解析两班库存覆盖规则的执行结果。
     *
     * @param task 胎侧任务
     * @return 库存完全覆盖两班时返回 PASS，其他情况返回 SKIP
     */
    private TcScheduleRuleResultEnum resolveTwoShiftCoverageResult(TcTaskDraft task) {
        return Boolean.TRUE.equals(task.getTwoShiftStockCovered())
                ? TcScheduleRuleResultEnum.PASS : TcScheduleRuleResultEnum.SKIP;
    }

    /**
     * 获取任务规则证据对象，不存在时创建。
     *
     * @param context 排程上下文
     * @param task    任务草稿
     * @return 规则证据对象
     */
    private TcRuleTrace traceOf(TcScheduleContext context, TcTaskDraft task) {
        return context.getRuleTraceMap().computeIfAbsent(task.getBusinessKey(), key -> new TcRuleTrace());
    }

    /**
     * 根据任务草稿构建需求量策略输入。
     *
     * @param task    任务草稿
     * @return 需求量策略输入
     */
    private TcDemandQtyInput buildDemandQtyInput(TcTaskDraft task) {
        TcDemandQtyInput input = new TcDemandQtyInput();
        input.setSidewallCode(task.getSidewallCode());
        input.setCurrentShiftDemandQty(task.getCurrentShiftDemandQty());
        input.setGuardDemandQty(task.getGuardDemandQty());
        input.setRollingStockQty(task.getRollingStockQty());
        input.setGuardShiftCount(task.getGuardShiftCount());
        input.setGuardRangeHours(task.getGuardRangeHours());
        input.setFormingGuardWindowQtyMap(task.getFormingGuardWindowQtyMap());
        input.setFormingGuardWindowHoursMap(task.getFormingGuardWindowHoursMap());
        return input;
    }

    /**
     * 将需求量策略结果回填到任务草稿。
     *
     * @param task   任务草稿
     * @param result 需求量策略结果
     */
    private void applyDemandQtyResult(TcTaskDraft task, TcDemandQtyResult result) {
        if (result == null) {
            return;
        }
        task.setCurrentShiftDemandQty(result.getCurrentShiftDemandQty());
        task.setGuardDemandQty(result.getGuardDemandQty());
        task.setRollingStockQty(result.getRollingStockQty());
        task.setCurrentShiftStockGapQty(result.getCurrentShiftStockGapQty());
        task.setStockGapQty(result.getStockGapQty());
        task.setDemandQty(result.getDemandQty());
        task.setGuardShiftCount(result.getGuardShiftCount());
        task.setSupplyHours(result.getSupplyHours());
    }

    /**
     * 初始化全局可用工装数量。
     *
     * <p>首个任务的可用工装数量等于总工装数量乘以整车率，再减去所有胎侧14点预计库存折算的占用工装数量，
     * 结果最低为0。工装数量是全局池，因此不能按单个胎侧重复使用总工装数量。</p>
     *
     * @param context          排程上下文
     * @param stockForecastMap 胎侧库存预测结果
     * @return 首个任务计算前的全局可用工装数量；未配置总工装时返回 null 表示不启用工装限制
     */
    private BigDecimal initializeGlobalAvailableToolQty(TcScheduleContext context, Map<String, TcStockForecast> stockForecastMap) {
        BigDecimal totalToolQty = this.resolveGlobalTotalToolQty(context);
        if (totalToolQty == null || totalToolQty.compareTo(BigDecimal.ZERO) <= 0) {
            context.appendProcessLog("全局工装池初始化：TC_TOOL_TOTAL_QTY未配置或非正数，未启用工装约束；初始可用工装数量=未计算。");
            return null;
        }
        Map<String, TcTaskDraft> representativeTaskMap = new LinkedHashMap<>();
        for (TcTaskDraft task : context.getTaskDraftList()) {
            if (task.getSidewallCode() != null && !representativeTaskMap.containsKey(task.getSidewallCode())) {
                representativeTaskMap.put(task.getSidewallCode(), task);
            }
        }
        BigDecimal initialUsedToolQty = BigDecimal.ZERO;
        List<String> inventoryToolDetailList = new ArrayList<>();
        for (Map.Entry<String, TcTaskDraft> entry : representativeTaskMap.entrySet()) {
            BigDecimal curlLength = this.resolveCurlLength(entry.getValue());
            if (curlLength.compareTo(BigDecimal.ZERO) <= 0) {
                inventoryToolDetailList.add(entry.getKey()
                        + "：有效卷曲长度无效，14点预计库存无法折算，按现有计算口径不计入库存占用");
                continue;
            }
            BigDecimal forecastStockQty = this.resolveForecastRollingStock(entry.getKey(), entry.getValue(), stockForecastMap);
            BigDecimal productUsedToolQty = forecastStockQty.divide(curlLength,
                    TcScheduleConstants.DECIMAL_CALCULATION_SCALE, RoundingMode.HALF_UP);
            initialUsedToolQty = initialUsedToolQty.add(productUsedToolQty);
            inventoryToolDetailList.add(entry.getKey() + "：14点预计库存"
                    + this.displayQuantity(forecastStockQty) + "米÷有效卷曲长度"
                    + this.displayQuantity(curlLength) + "米/套="
                    + this.displayQuantity(productUsedToolQty) + "套");
        }
        BigDecimal vehicleRate = this.readDecimalParam(context, TcScheduleConstants.PARAM_VEHICLE_RATE,
                BigDecimal.ONE).max(BigDecimal.ZERO);
        BigDecimal initialAvailableToolQty = totalToolQty.multiply(vehicleRate).subtract(initialUsedToolQty)
                .max(BigDecimal.ZERO)
                .setScale(TcScheduleConstants.DECIMAL_CALCULATION_SCALE, RoundingMode.HALF_UP);
        context.appendProcessLog("全局工装池初始化：总工装数量={0}套。", this.displayQuantity(totalToolQty));
        context.appendProcessLog("14点预计库存：\n{0}", inventoryToolDetailList.isEmpty()
                ? "无有效胎侧库存占用" : String.join("\n", inventoryToolDetailList));
        context.appendProcessLog("初始可用工装数量=max(总工装数量{0}套×TC_VEHICLE_RATE {1}-14点预计库存占用工装合计{2}套,0)={3}套。",
                this.displayQuantity(totalToolQty), this.displayQuantity(vehicleRate),
                this.displayQuantity(initialUsedToolQty), this.displayQuantity(initialAvailableToolQty));
        return initialAvailableToolQty;
    }

    /**
     * 格式化工装初始化日志中的数量，移除无意义的小数末尾零。
     *
     * @param quantity 待展示数量
     * @return 普通十进制数量文本；空值返回未计算
     */
    private String displayQuantity(BigDecimal quantity) {
        return quantity == null ? "未计算" : quantity.stripTrailingZeros().toPlainString();
    }

    /**
     * 解析全局总工装数量，并校验同一轮排程携带的总工装数量一致。
     *
     * @param context 排程上下文
     * @return 全局总工装数量；未配置时返回 null
     */
    private BigDecimal resolveGlobalTotalToolQty(TcScheduleContext context) {
        BigDecimal totalToolQty = null;
        for (TcTaskDraft task : context.getTaskDraftList()) {
            if (task.getTotalToolQty() == null) {
                continue;
            }
            if (totalToolQty == null) {
                totalToolQty = task.getTotalToolQty();
                continue;
            }
            if (totalToolQty.compareTo(task.getTotalToolQty()) != 0) {
                throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.toolTotalMismatch"));
            }
        }
        return totalToolQty;
    }

    /**
     * 解析胎侧14点预计库存。
     *
     * @param sidewallCode        胎侧编码
     * @param task             任务草稿
     * @param stockForecastMap 胎侧库存预测结果
     * @return 14点预计库存，空值按0处理
     */
    private BigDecimal resolveForecastRollingStock(String sidewallCode, TcTaskDraft task, Map<String, TcStockForecast> stockForecastMap) {
        if (stockForecastMap != null) {
            TcStockForecast forecast = stockForecastMap.get(sidewallCode);
            if (forecast != null && forecast.getRollingStockQty() != null) {
                return forecast.getRollingStockQty();
            }
        }
        return nvl(task.getRollingStockQty());
    }

    /**
     * 解析卷曲长度。
     *
     * @param task 任务草稿
     * @return 卷曲长度，无法取得时返回0
     */
    private BigDecimal resolveCurlLength(TcTaskDraft task) {
        if (task.getCurlRollLength() != null && task.getCurlRollLength().compareTo(BigDecimal.ZERO) > 0) {
            return task.getCurlRollLength();
        }
        return nvl(task.getDefaultCurlRollLength());
    }
    /**
     * 将计划量策略结果回填到任务草稿，便于解释表落库。
     *
     * @param task   任务草稿
     * @param result 计划量策略结果
     */
    private void applyPlanQtyResult(TcTaskDraft task, TcPlanQtyResult result) {
        if (result == null) {
            return;
        }
        task.setBaseDemandQty(result.getBaseDemandQty());
        task.setLossAddQty(result.getLossAddQty());
        task.setToolLimitAdjustQty(result.getToolLimitAdjustQty());
        task.setToolOverflowQty(result.getToolOverflowQty());
        task.setMinStartAdjustQty(result.getMinStartAdjustQty());
        task.setTailRoundAdjustQty(result.getTailRoundAdjustQty());
        task.setCapacityAdjustQty(result.getCapacityAdjustQty());
        task.setPreLossPlanQty(result.getPreLossPlanQty());
        task.setPlanQtyBeforeToolLimit(result.getPlanQtyBeforeToolLimit());
        task.setPlanQty(result.getFinalPlanQty());
        task.setCalcFormulaDesc(result.getCalcFormulaDesc());
    }


    /**
     * 回写同一胎侧的下一任务班初库存状态。
     *
     * @param context 胎侧排程上下文
     * @param task    任务草稿
     */
    private void updateRollingStockState(TcScheduleContext context, TcTaskDraft task) {
        if (context == null || context.getRemainingStockMap() == null || task == null || task.getSidewallCode() == null) {
            return;
        }
        BigDecimal handoverStock = task.getPlanStockQty();
        if (handoverStock == null && task.getPlanQty() != null) {
            handoverStock = nvl(task.getRollingStockQty()).add(nvl(task.getPlanQty()))
                    .subtract(nvl(task.getCurrentShiftDemandQty())).max(BigDecimal.ZERO);
            task.setPlanStockQty(handoverStock);
        }
        context.getRemainingStockMap().put(task.getSidewallCode(), nvl(handoverStock));
    }
    /**
     * 空值转 0。
     *
     * @param value 原始数值
     * @return 非空数值
     */
    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 获取任务原始成型当班需求量，兼容未初始化日志追溯字段的旧测试任务。
     *
     * @param task 待读取任务
     * @return 原始成型当班需求量
     */
    private BigDecimal resolveOriginalCurrentShiftDemandQty(TcTaskDraft task) {
        if (task == null) {
            return BigDecimal.ZERO;
        }
        return task.getOriginalCurrentShiftDemandQty() == null
                ? this.nvl(task.getCurrentShiftDemandQty()) : task.getOriginalCurrentShiftDemandQty();
    }

    /**
     * 从上下文读取参数值，缺省时返回默认值。
     *
     * @param context      胎侧排程上下文
     * @param paramCode    参数编码
     * @param defaultValue 缺省值
     * @return 参数有效值
     */
    private String readParam(TcScheduleContext context, String paramCode, String defaultValue) {
        TcParamValue paramValue = context.getParamMap().get(paramCode);
        if (paramValue == null || StrUtil.isBlank(paramValue.getEffectiveValue())) {
            return defaultValue;
        }
        return paramValue.getEffectiveValue();
    }

    /**
     * 读取需求量算法参数 TC_ALGORITHM_SWITCH。
     *
     * @param context 胎侧排程上下文
     * @return 需求量算法编码
     */
    public String readAlgorithmCode(TcScheduleContext context) {
        return readParam(context, TcScheduleConstants.PARAM_ALGORITHM_SWITCH,
                TcScheduleConstants.DEFAULT_ALGORITHM_SWITCH);
    }
}
