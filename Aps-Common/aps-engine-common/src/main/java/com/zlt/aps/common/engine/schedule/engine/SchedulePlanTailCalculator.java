package com.zlt.aps.common.engine.schedule.engine;

import com.zlt.aps.common.core.utils.SixShiftWorkCalendarUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TM/TC 自动排程成型来源收尾班次计算器。
 *
 * <p>该类只根据任务草稿中的原始成型八班计划和成型余量计算运行态元数据，
 * 不参与库存、产能、工装或计划量计算，避免收尾判定与数量计算相互耦合。</p>
 */
public final class SchedulePlanTailCalculator {

    /**
     * 成型原始计划最大班次。
     */
    private static final int FORMING_MAX_SHIFT_ORDER = 8;
    /**
     * 成型来源无法确定收尾班次。
     */
    private static final String REASON_SOURCE_TAIL_UNRESOLVED = "SOURCE_TAIL_UNRESOLVED";
    /**
     * 最终成型需求无可承接班次。
     */
    private static final String REASON_FINAL_TAIL_DEMAND_NO_TARGET = "FINAL_TAIL_DEMAND_NO_TARGET";
    /**
     * 最终收尾按未来需求最后承接班次判定。
     */
    private static final String REASON_FUTURE_LAST_RECEIVING_SHIFT = "FUTURE_LAST_RECEIVING_SHIFT";
    /**
     * 普通实际覆盖窗口判定。
     */
    private static final String REASON_NORMAL_COVERAGE_WINDOW = "NORMAL_COVERAGE_WINDOW";

    private SchedulePlanTailCalculator() {
    }

    /**
     * 为本次排程的来源任务准备来源收尾班次及产品最终收尾班次。
     *
     * <p>同一成型来源键只计算一次；同一产品代码下任一来源无法确定收尾班次，
     * 该产品整体标记为暂不收尾。没有成型来源键的预置或兼容任务不写入新元数据，
     * 由领域策略继续执行原有兼容逻辑。</p>
     *
     * @param taskList 本次排程原始任务列表
     */
    public static void prepareTailMetadata(List<? extends ScheduleTaskDraftModel> taskList) {
        if (taskList == null || taskList.isEmpty()) {
            return;
        }
        taskList.stream().filter(SchedulePlanTailCalculator::hasFormingSource)
                .forEach(SchedulePlanTailCalculator::ensureSourceSnapshot);
        Map<String, ScheduleFormingTailSourceModel> sourceMap = new LinkedHashMap<>();
        taskList.stream().filter(SchedulePlanTailCalculator::hasFormingSource)
                .flatMap(task -> task.getFormingTailSourceList().stream())
                .forEach(source -> sourceMap.merge(resolveSourceIdentity(source), source,
                        SchedulePlanTailCalculator::mergeSourceSnapshot));
        sourceMap.values().forEach(SchedulePlanTailCalculator::calculateSourceTail);

        Map<String, List<ScheduleFormingTailSourceModel>> productSourceMap = sourceMap.values().stream()
                .filter(source -> hasText(source.getProcessCode()))
                .collect(Collectors.groupingBy(source -> source.getProcessCode().trim(),
                        LinkedHashMap::new, Collectors.toList()));
        Map<String, ScheduleProductTailDecisionModel> productDecisionMap = new LinkedHashMap<>();
        productSourceMap.forEach((productCode, sourceList) -> productDecisionMap.put(productCode,
                calculateProductDecision(productCode, sourceList, taskList)));
        taskList.stream().filter(SchedulePlanTailCalculator::hasFormingSource).forEach(task -> {
            ScheduleFormingTailSourceModel source = sourceMap.get(resolveSourceIdentity(task));
            if (source != null) {
                task.setFormingTailShiftOrder(source.getTailShiftKey() == null
                        ? null : source.getTailShiftKey().getShiftOrder());
                List<ScheduleFormingTailSourceModel> taskSourceList = task.getFormingTailSourceList().stream()
                        .map(item -> sourceMap.get(resolveSourceIdentity(item)))
                        .filter(Objects::nonNull)
                        .collect(Collectors.collectingAndThen(Collectors.toMap(
                                        SchedulePlanTailCalculator::resolveSourceIdentity,
                                        item -> item, (first, ignored) -> first, LinkedHashMap::new),
                                map -> new ArrayList<>(map.values())));
                if (taskSourceList.isEmpty()) {
                    taskSourceList = Collections.singletonList(source);
                }
                task.setFormingTailSourceList(taskSourceList);
            }
            String productCode = hasText(task.getProcessCode()) ? task.getProcessCode().trim() : null;
            ScheduleProductTailDecisionModel decision = productCode == null
                    ? buildUnavailableDecision(REASON_SOURCE_TAIL_UNRESOLVED)
                    : productDecisionMap.get(productCode);
            applyProductDecision(task, decision);
        });
    }

    /**
     * 合并汇总组的来源、前移分配和实际覆盖班次运行态信息。
     *
     * @param aggregateTask  汇总任务
     * @param sourceTaskList 汇总组来源任务
     */
    public static void mergeRuntimeMetadata(ScheduleTaskDraftModel aggregateTask,
                                            List<? extends ScheduleTaskDraftModel> sourceTaskList) {
        if (aggregateTask == null || sourceTaskList == null || sourceTaskList.isEmpty()) {
            return;
        }
        Map<String, ScheduleFormingTailSourceModel> sourceMap = new LinkedHashMap<>();
        List<ScheduleFutureDemandAllocationModel> allocationList = new ArrayList<>();
        Set<ScheduleFormingShiftKey> coverageSet = new LinkedHashSet<>();
        for (ScheduleTaskDraftModel sourceTask : sourceTaskList) {
            if (sourceTask == null) {
                continue;
            }
            ensureSourceSnapshot(sourceTask);
            sourceTask.getFormingTailSourceList().forEach(source ->
                    sourceMap.putIfAbsent(resolveSourceIdentity(source), source));
            allocationList.addAll(sourceTask.getFutureDemandAllocationList());
            coverageSet.addAll(sourceTask.getFormingCoverageShiftKeySet());
            if (aggregateTask.getTargetProductionShiftKey() == null
                    && sourceTask.getTargetProductionShiftKey() != null) {
                aggregateTask.setTargetProductionShiftKey(sourceTask.getTargetProductionShiftKey());
            }
        }
        aggregateTask.setFormingTailSourceList(new ArrayList<>(sourceMap.values()));
        aggregateTask.setFutureDemandAllocationList(deduplicateAllocations(allocationList));
        aggregateTask.setFormingCoverageShiftKeySet(coverageSet);
    }

    /**
     * 判断任务是否为最终成型收尾需求的最后承接任务，或普通窗口收尾任务。
     *
     * @param aggregateTask 汇总任务
     * @return true表示当前任务可以按收尾公式计算
     */
    public static boolean isTailReceivingTask(ScheduleTaskDraftModel aggregateTask) {
        ScheduleProductTailDecisionModel decision = aggregateTask == null
                ? null : aggregateTask.getProductTailDecision();
        if (decision == null || !Boolean.TRUE.equals(decision.getDecisionAvailable())
                || decision.getFinalFormingTailShiftKey() == null) {
            return false;
        }
        if (Boolean.TRUE.equals(decision.getFutureTailDemandPresent())) {
            ScheduleFormingShiftKey lastReceivingKey = decision.getLastTailReceivingShiftKey();
            if (lastReceivingKey == null || !lastReceivingKey.equals(aggregateTask.getTargetProductionShiftKey())) {
                return false;
            }
            return aggregateTask.getFutureDemandAllocationList().stream()
                    .filter(SchedulePlanTailCalculator::isPositiveAllocation)
                    .filter(allocation -> lastReceivingKey.equals(allocation.getTargetProductionShiftKey()))
                    .anyMatch(allocation -> allocation.getSourceDemandShiftKeys() != null
                            && allocation.getSourceDemandShiftKeys()
                            .contains(decision.getFinalFormingTailShiftKey()));
        }
        if (aggregateTask.getFormingCoverageShiftKeySet() != null
                && aggregateTask.getFormingCoverageShiftKeySet().contains(decision.getFinalFormingTailShiftKey())) {
            return true;
        }
        Integer legacyTailOrder = decision.getFinalFormingTailShiftKey().getShiftOrder();
        return legacyTailOrder != null && aggregateTask.getFormingGuardWindowQtyMap() != null
                && aggregateTask.getFormingGuardWindowQtyMap().containsKey(legacyTailOrder);
    }

    /**
     * 将来源排程日期和原始成型CLASS统一映射为实际成型班次。
     *
     * @param sourceScheduleDate 来源排程日期
     * @param formingShiftOrder  原始成型班次
     * @return 实际成型班次；日期为空时保留班次序号用于旧任务兼容
     */
    public static ScheduleFormingShiftKey resolveFormingShiftKey(LocalDate sourceScheduleDate,
                                                                 Integer formingShiftOrder) {
        if (formingShiftOrder == null || formingShiftOrder < 1
                || formingShiftOrder > FORMING_MAX_SHIFT_ORDER) {
            return null;
        }
        LocalDate productionDate = sourceScheduleDate == null ? null
                : SixShiftWorkCalendarUtil.resolveFormingProductionDate(sourceScheduleDate, formingShiftOrder);
        return new ScheduleFormingShiftKey(productionDate, formingShiftOrder);
    }

    /**
     * 构建当前需求算法实际覆盖的成型班次集合。
     *
     * @param sourceScheduleDate 来源排程日期
     * @param startShiftOrder    需求起始成型班次
     * @param shiftCount         实际需求算法覆盖班次数
     * @return 实际需求覆盖班次
     */
    public static List<ScheduleFormingShiftKey> resolveDemandShiftKeyList(LocalDate sourceScheduleDate,
                                                                          Integer startShiftOrder,
                                                                          int shiftCount) {
        if (startShiftOrder == null || shiftCount <= 0) {
            return Collections.emptyList();
        }
        List<ScheduleFormingShiftKey> resultList = new ArrayList<>();
        for (int index = 0; index < shiftCount; index++) {
            ScheduleFormingShiftKey shiftKey = resolveFormingShiftKey(sourceScheduleDate,
                    startShiftOrder + index);
            if (shiftKey != null) {
                resultList.add(shiftKey);
            }
        }
        return resultList;
    }

    /**
     * 按原始成型班次累计计划量，确定首次达到成型余量的班次。
     *
     * @param classQtyMap CLASS1~CLASS8 原始计划条数
     * @param remainQty   成型收尾余量
     * @return 来源收尾班次；数据不足时返回 null
     */
    public static Integer resolveSourceTailShift(Map<Integer, BigDecimal> classQtyMap,
                                                 BigDecimal remainQty) {
        if (remainQty == null || remainQty.compareTo(BigDecimal.ZERO) < 0
                || classQtyMap == null || classQtyMap.isEmpty()) {
            return null;
        }
        BigDecimal cumulativeQty = BigDecimal.ZERO;
        for (int shiftOrder = 1; shiftOrder <= FORMING_MAX_SHIFT_ORDER; shiftOrder++) {
            BigDecimal classQty = classQtyMap.get(shiftOrder);
            if (classQty == null || classQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            cumulativeQty = cumulativeQty.add(classQty);
            if (cumulativeQty.compareTo(remainQty) >= 0) {
                return shiftOrder;
            }
        }
        return null;
    }

    /**
     * 计算单个来源的实际收尾班次。
     *
     * @param source 来源快照
     */
    private static void calculateSourceTail(ScheduleFormingTailSourceModel source) {
        if ("SOURCE_SNAPSHOT_CONFLICT".equals(source.getUnresolvedReason())) {
            source.setTailShiftKey(null);
            return;
        }
        Integer tailShiftOrder = resolveSourceTailShift(source.getOriginalClassQtyMap(),
                source.getFormingTailRemainQty());
        if (tailShiftOrder == null) {
            source.setTailShiftKey(null);
            source.setUnresolvedReason(resolveSourceUnresolvedReason(source));
            return;
        }
        source.setTailShiftKey(resolveFormingShiftKey(source.getSourceScheduleDate(), tailShiftOrder));
        source.setUnresolvedReason(null);
    }

    /**
     * 根据产品来源及前移分配计算最终收尾判定。
     *
     * @param productCode 产品编码
     * @param sourceList  产品来源快照
     * @param taskList    完整任务列表
     * @return 产品级收尾判定
     */
    private static ScheduleProductTailDecisionModel calculateProductDecision(
            String productCode, List<ScheduleFormingTailSourceModel> sourceList,
            List<? extends ScheduleTaskDraftModel> taskList) {
        ScheduleProductTailDecisionModel decision = new ScheduleProductTailDecisionModel();
        boolean available = !sourceList.isEmpty()
                && sourceList.stream().allMatch(source -> source.getTailShiftKey() != null);
        decision.setDecisionAvailable(available);
        if (!available) {
            decision.setFutureTailDemandPresent(Boolean.FALSE);
            decision.setDecisionReason(REASON_SOURCE_TAIL_UNRESOLVED);
            return decision;
        }
        ScheduleFormingShiftKey finalTailKey = sourceList.stream()
                .map(ScheduleFormingTailSourceModel::getTailShiftKey)
                .max(ScheduleFormingShiftKey::compareTo).orElse(null);
        decision.setFinalFormingTailShiftKey(finalTailKey);
        List<ScheduleFutureDemandAllocationModel> finalAllocationList = taskList.stream()
                .filter(task -> task != null && productCode.equals(normalize(task.getProcessCode())))
                .flatMap(task -> task.getFutureDemandAllocationList().stream())
                .filter(allocation -> productCode.equals(normalize(allocation.getProcessCode())))
                .filter(allocation -> allocation.getSourceDemandShiftKeys() != null
                        && allocation.getSourceDemandShiftKeys().contains(finalTailKey))
                .collect(Collectors.toList());
        decision.setFutureTailDemandPresent(!finalAllocationList.isEmpty());
        if (finalAllocationList.isEmpty()) {
            decision.setDecisionReason(REASON_NORMAL_COVERAGE_WINDOW);
            return decision;
        }
        if (finalAllocationList.stream().anyMatch(allocation ->
                ScheduleFutureDemandAllocationModel.STATUS_NO_TARGET.equals(allocation.getAllocationStatus()))) {
            decision.setDecisionReason(REASON_FINAL_TAIL_DEMAND_NO_TARGET);
            return decision;
        }
        decision.setLastTailReceivingShiftKey(finalAllocationList.stream()
                .filter(SchedulePlanTailCalculator::isPositiveAllocation)
                .map(ScheduleFutureDemandAllocationModel::getTargetProductionShiftKey)
                .filter(Objects::nonNull)
                .max(ScheduleFormingShiftKey::compareTo).orElse(null));
        decision.setDecisionReason(decision.getLastTailReceivingShiftKey() == null
                ? REASON_FINAL_TAIL_DEMAND_NO_TARGET : REASON_FUTURE_LAST_RECEIVING_SHIFT);
        return decision;
    }

    /**
     * 确保旧任务也能生成来源快照。
     *
     * @param task 成型来源任务
     */
    private static void ensureSourceSnapshot(ScheduleTaskDraftModel task) {
        if (task.getFormingTailSourceList() != null && !task.getFormingTailSourceList().isEmpty()) {
            ensureCoverageShiftKeys(task);
            return;
        }
        ScheduleFormingTailSourceModel source = new ScheduleFormingTailSourceModel();
        source.setSourceKey(task.getFormingSourceKey());
        source.setSourceScheduleDate(task.getFormingSourceScheduleDate());
        source.setProcessCode(task.getProcessCode());
        source.setEmbryoCode(task.getEmbryoCode());
        source.setSourceOrderNo(task.getSourceOrderNos());
        source.setOriginalClassQtyMap(task.getFormingClassQtyMap());
        source.setFormingTailRemainQty(task.getFormingTailRemainQty());
        task.setFormingTailSourceList(Collections.singletonList(source));
        ensureCoverageShiftKeys(task);
    }

    /**
     * 从实际供应窗口构建普通收尾判定使用的成型班次集合。
     *
     * @param task 任务
     */
    private static void ensureCoverageShiftKeys(ScheduleTaskDraftModel task) {
        if (Boolean.TRUE.equals(task.getFutureShutdownSupplementTask())) {
            return;
        }
        if (task.getFormingDemandShiftKey() == null && task.getFormingLogicalShiftOrder() != null) {
            task.setFormingDemandShiftKey(resolveFormingShiftKey(task.getFormingSourceScheduleDate(),
                    task.getFormingLogicalShiftOrder()));
        }
        if (task.getFormingDemandShiftKeyList() == null || task.getFormingDemandShiftKeyList().isEmpty()) {
            task.setFormingDemandShiftKeyList(task.getFormingDemandShiftKey() == null
                    ? Collections.emptyList() : Collections.singletonList(task.getFormingDemandShiftKey()));
        }
        if (task.getFormingCoverageShiftKeySet() != null
                && !task.getFormingCoverageShiftKeySet().isEmpty()) {
            return;
        }
        if (task.getFormingGuardWindowQtyMap() == null) {
            return;
        }
        Set<ScheduleFormingShiftKey> coverageSet = new LinkedHashSet<>();
        task.getFormingGuardWindowQtyMap().keySet().stream()
                .map(order -> resolveFormingShiftKey(task.getFormingSourceScheduleDate(), order))
                .filter(Objects::nonNull)
                .forEach(coverageSet::add);
        task.setFormingCoverageShiftKeySet(coverageSet);
    }

    /**
     * 校验并保留重复来源快照，避免按遍历顺序静默覆盖冲突数据。
     *
     * @param first  已登记来源
     * @param second 重复来源
     * @return 保留的来源
     */
    private static ScheduleFormingTailSourceModel mergeSourceSnapshot(
            ScheduleFormingTailSourceModel first, ScheduleFormingTailSourceModel second) {
        boolean same = Objects.equals(first.getSourceScheduleDate(), second.getSourceScheduleDate())
                && Objects.equals(first.getProcessCode(), second.getProcessCode())
                && Objects.equals(first.getFormingTailRemainQty(), second.getFormingTailRemainQty())
                && Objects.equals(first.getOriginalClassQtyMap(), second.getOriginalClassQtyMap());
        if (!same) {
            first.setTailShiftKey(null);
            first.setUnresolvedReason("SOURCE_SNAPSHOT_CONFLICT");
        }
        return first;
    }

    /**
     * 解析来源唯一标识。
     *
     * @param task 来源任务
     * @return 来源日期和来源键组合
     */
    private static String resolveSourceIdentity(ScheduleTaskDraftModel task) {
        return resolveSourceIdentity(task.getFormingSourceScheduleDate(), task.getFormingSourceKey(),
                task.getProcessCode());
    }

    /**
     * 解析来源快照唯一标识。
     *
     * @param source 来源快照
     * @return 来源日期和来源键组合
     */
    private static String resolveSourceIdentity(ScheduleFormingTailSourceModel source) {
        return resolveSourceIdentity(source.getSourceScheduleDate(), source.getSourceKey(),
                source.getProcessCode());
    }

    /**
     * 组合来源日期、来源键和产品编码，隔离不同日期或不同产品解析结果的兜底键。
     *
     * @param sourceDate  来源日期
     * @param sourceKey   来源键
     * @param processCode 产品编码
     * @return 运行态来源标识
     */
    private static String resolveSourceIdentity(LocalDate sourceDate, String sourceKey, String processCode) {
        return String.valueOf(sourceDate) + "|" + (sourceKey == null ? "" : sourceKey.trim())
                + "|" + (processCode == null ? "" : processCode.trim());
    }

    private static String resolveSourceUnresolvedReason(ScheduleFormingTailSourceModel source) {
        if (source.getFormingTailRemainQty() == null) {
            return "FORMING_TAIL_REMAIN_QTY_MISSING";
        }
        if (source.getFormingTailRemainQty().compareTo(BigDecimal.ZERO) < 0) {
            return "FORMING_TAIL_REMAIN_QTY_NEGATIVE";
        }
        if (source.getOriginalClassQtyMap() == null || source.getOriginalClassQtyMap().isEmpty()) {
            return "FORMING_CLASS_QTY_MISSING";
        }
        return "FORMING_CLASS_QTY_INSUFFICIENT";
    }

    /**
     * 将产品级判定同步到任务兼容字段和新运行态字段。
     *
     * @param task     任务
     * @param decision 产品判定
     */
    private static void applyProductDecision(ScheduleTaskDraftModel task,
                                             ScheduleProductTailDecisionModel decision) {
        task.setProductTailDecision(decision);
        task.setProductTailDecisionAvailable(decision == null ? Boolean.FALSE : decision.getDecisionAvailable());
        ScheduleFormingShiftKey finalTailKey = decision == null ? null : decision.getFinalFormingTailShiftKey();
        task.setProductTailShiftOrder(finalTailKey == null ? null : finalTailKey.getShiftOrder());
    }

    /**
     * 构造不可判定的产品收尾结果。
     *
     * @param reason 不可判定原因
     * @return 不可收尾的产品判定
     */
    private static ScheduleProductTailDecisionModel buildUnavailableDecision(String reason) {
        ScheduleProductTailDecisionModel decision = new ScheduleProductTailDecisionModel();
        decision.setDecisionAvailable(Boolean.FALSE);
        decision.setFutureTailDemandPresent(Boolean.FALSE);
        decision.setDecisionReason(reason);
        return decision;
    }

    private static List<ScheduleFutureDemandAllocationModel> deduplicateAllocations(
            List<ScheduleFutureDemandAllocationModel> allocationList) {
        Map<String, ScheduleFutureDemandAllocationModel> allocationMap = new LinkedHashMap<>();
        for (ScheduleFutureDemandAllocationModel allocation : allocationList) {
            if (allocation == null) {
                continue;
            }
            String key = String.valueOf(allocation.getSourceKey()) + "|"
                    + String.valueOf(allocation.getSourceDemandShiftKeys()) + "|"
                    + String.valueOf(allocation.getTargetProductionShiftKey()) + "|"
                    + String.valueOf(allocation.getAllocationStatus()) + "|"
                    + String.valueOf(allocation.getAllocatedQty());
            allocationMap.putIfAbsent(key, allocation);
        }
        return new ArrayList<>(allocationMap.values());
    }

    /**
     * 判断前移记录是否为正数量的实际分配。
     *
     * @param allocation 前移记录
     * @return true表示已分配且数量为正
     */
    private static boolean isPositiveAllocation(ScheduleFutureDemandAllocationModel allocation) {
        return allocation != null
                && ScheduleFutureDemandAllocationModel.STATUS_ALLOCATED.equals(allocation.getAllocationStatus())
                && allocation.getAllocatedQty() != null
                && allocation.getAllocatedQty().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 标准化产品编码。
     *
     * @param value 原始编码
     * @return 去除首尾空格后的编码
     */
    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * 判断任务是否携带可参与新收尾判定的成型来源键。
     *
     * @param task 待判断任务
     * @return true 表示有成型来源
     */
    private static boolean hasFormingSource(ScheduleTaskDraftModel task) {
        return task != null && hasText(task.getFormingSourceKey());
    }

    /**
     * 判断文本是否非空。
     *
     * @param value 待判断文本
     * @return true 表示非空
     */
    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
