package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.FirstInspectionQtyUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** 最终交替衔接只读复核。只写诊断日志，不修改结果、库存、计划量、资源或消费账本。 */
@Slf4j
public final class ContinuationEndingHandoffAudit {
    private ContinuationEndingHandoffAudit() {
    }

    /**
     * 所有后料、首检和后置裁量完成后，对照续作提交快照。
     * @param context 最终排程上下文
     * @return 存在差异的结果行数与生产收尾异常组数，供聚焦回归复核
     */
    public static int audit(LhScheduleContext context) {
        if (Objects.isNull(context)) {
            return 0;
        }
        // 全量空班审计不依赖收尾快照或IS_END，降模退出及普通新增同样必须覆盖。
        auditInternalIdleShifts(context);
        if (CollectionUtils.isEmpty(context.getContinuationSurplusEndingSnapshotMap())) {
            return 0;
        }
        Map<String, List<LhScheduleResult>> byMachine = context.getScheduleResultList().stream()
                .filter(result -> StringUtils.isNotEmpty(result.getLhMachineCode()))
                .collect(Collectors.groupingBy(result -> LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode())));
        List<LhScheduleResult> originals = new ArrayList<>(context.getContinuationSurplusEndingSnapshotMap().keySet());
        Set<LhScheduleResult> finalResults = java.util.Collections.newSetFromMap(
                new java.util.IdentityHashMap<LhScheduleResult, Boolean>(context.getScheduleResultList().size()));
        finalResults.addAll(context.getScheduleResultList());
        originals.sort(Comparator.comparing(LhScheduleResult::getLhMachineCode));
        Map<String, Map<String, Integer>> actualGroups = new HashMap<>(originals.size());
        Map<String, Set<String>> expectedGroups = new HashMap<>(originals.size());
        Map<String, Map<String, Integer>> productionGroups = new HashMap<>(originals.size());
        Set<String> invalidProductionGroups = new HashSet<>(originals.size());
        int differences = 0;
        for (LhScheduleResult original : originals) {
            ContinuationEndingAllocationSnapshot snapshot = context.getContinuationSurplusEndingSnapshotMap().get(original);
            if (Arrays.stream(snapshot.getShiftQuantities()).sum() <= 0) {
                continue;
            }
            String machine = LhSingleControlMachineUtil.resolvePhysicalMachineCode(original.getLhMachineCode());
            expectedGroups.computeIfAbsent(snapshot.getGroupKey(), key -> new HashSet<String>(4)).add(machine);
            LhScheduleResult following = findFollowing(context, original, snapshot, byMachine.get(machine));
            List<String> reasons = inspectResult(context, original, snapshot, following);
            boolean retained = finalResults.contains(original);
            if (!retained) {
                reasons.add("续作原结果已被后续阶段移除或替换，快照量不能冒充最终量");
                invalidProductionGroups.add(snapshot.getGroupKey());
            } else {
                // 生产收尾审计不依赖后料；同物理机台L/R取最后有量班次的较晚值。
                int endingShift = ShiftFieldUtil.resolveLastPlannedShiftIndex(original);
                if (endingShift > 0) {
                    productionGroups.computeIfAbsent(snapshot.getGroupKey(), key -> new HashMap<String, Integer>(4))
                            .merge(machine, endingShift, Math::max);
                    if (Objects.isNull(lastEnd(original)) || (Objects.nonNull(snapshot.getFinishDeadline())
                            && lastEnd(original).after(snapshot.getFinishDeadline()))) {
                        invalidProductionGroups.add(snapshot.getGroupKey());
                        reasons.add("生产收尾越过新规则指定节点或结束时间缺失");
                    }
                } else {
                    invalidProductionGroups.add(snapshot.getGroupKey());
                }
            }
            Date actualChange = Objects.nonNull(following) ? following.getMouldChangeStartTime() : null;
            int actualShift = resolveShift(context.getScheduleWindowShifts(), actualChange);
            if (retained && actualShift > 0) {
                actualGroups.computeIfAbsent(snapshot.getGroupKey(), key -> new HashMap<String, Integer>(4)).put(machine, actualShift);
            }
            long waitingMinutes = Objects.nonNull(actualChange) && Objects.nonNull(snapshot.getReleaseTime())
                    ? Math.max(0, actualChange.getTime() - snapshot.getReleaseTime().getTime()) / 60000L : -1;
            String detail = String.format("factoryCode=%s, batchNo=%s, materialCode=%s, productStatus=%s, machineCode=%s, "
                            + "提交班次量=%s, 最终班次量=%s, 提交释放=%s, 最终生产结束=%s, 预测交替=%s, 实际交替=%s, "
                            + "实际交替班次=%s, 等待分钟=%s, 后物料=%s, 后料首个有量开始=%s, 差异原因=%s",
                    context.getFactoryCode(), context.getBatchNo(), original.getMaterialCode(), original.getProductStatus(),
                    original.getLhMachineCode(), Arrays.toString(snapshot.getShiftQuantities()),
                    retained ? Arrays.toString(readQuantities(original, context.getScheduleWindowShifts())) : "原结果已移除或替换",
                    LhScheduleTimeUtil.formatDateTime(snapshot.getReleaseTime()),
                    LhScheduleTimeUtil.formatDateTime(retained ? lastEnd(original) : null),
                    LhScheduleTimeUtil.formatDateTime(snapshot.getPredictedChangeTime()), LhScheduleTimeUtil.formatDateTime(actualChange),
                    actualShift, waitingMinutes, Objects.nonNull(following) ? following.getMaterialCode() : null,
                    LhScheduleTimeUtil.formatDateTime(Objects.nonNull(following) ? firstStart(following) : null),
                    reasons.isEmpty() ? "一致" : String.join("；", reasons));
            PriorityTraceLogHelper.appendProcessLog(context, "续作余量收尾实际衔接复核", detail);
            log.info("续作余量收尾实际衔接复核, {}", detail);
            if (!reasons.isEmpty()) {
                differences++;
            }
        }
        expectedGroups.forEach((group, machines) -> {
            Map<String, Integer> actual = actualGroups.getOrDefault(group, java.util.Collections.emptyMap());
            int span = actual.isEmpty() ? -1 : java.util.Collections.max(actual.values()) - java.util.Collections.min(actual.values());
            String conclusion;
            if (actual.size() < machines.size()) {
                conclusion = "部分机台无可核对的正式衔接，不能认定错峰成功";
            } else {
                conclusion = "实际交替仅供资源衔接核对，不用于判定生产收尾跨度";
            }
            String detail = String.format("group=%s, 应核对物理机台=%s, 实际交替班次=%s, 实际跨度=%s, 实际结论=%s",
                    group, machines, actual, span, conclusion);
            PriorityTraceLogHelper.appendProcessLog(context, "续作余量收尾实际组级跨度", detail);
        });
        differences += auditProductionEndingGroups(context, expectedGroups, productionGroups, invalidProductionGroups);
        return differences;
    }

    /**
     * 合并同物理机台所有物料及左右模，核对首次和末次正量之间的连续整班空档。
     * @param context 保存前最终上下文
     * @return 存在中间空班的物理机台数；正常准备与业务等待也记录，不直接认定为缺陷
     */
    public static int auditInternalIdleShifts(LhScheduleContext context) {
        if (Objects.isNull(context) || CollectionUtils.isEmpty(context.getScheduleWindowShifts())
                || CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return 0;
        }
        List<LhShiftConfigVO> shifts = context.getScheduleWindowShifts().stream()
                .sorted(Comparator.comparing(LhShiftConfigVO::getShiftStartDateTime)).collect(Collectors.toList());
        Map<String, List<LhScheduleResult>> machines = context.getScheduleResultList().stream()
                .filter(Objects::nonNull).filter(result -> StringUtils.isNotEmpty(result.getLhMachineCode()))
                .collect(Collectors.groupingBy(result -> LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                        result.getLhMachineCode()), java.util.TreeMap::new, Collectors.toList()));
        int machineCount = 0;
        int idleShiftCount = 0;
        for (Map.Entry<String, List<LhScheduleResult>> entry : machines.entrySet()) {
            List<LhScheduleResult> results = entry.getValue();
            int[] quantities = shifts.stream().mapToInt(shift -> results.stream()
                    .map(result -> ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex()))
                    .filter(Objects::nonNull).mapToInt(quantity -> Math.max(0, quantity)).sum()).toArray();
            int previousPositive = -1;
            int machineIdleShifts = 0;
            for (int position = 0; position < quantities.length; position++) {
                if (quantities[position] <= 0) {
                    continue;
                }
                if (previousPositive >= 0 && position > previousPositive + 1) {
                    machineIdleShifts += position - previousPositive - 1;
                    // 相邻两个正量槽位之间才属于中间空班，首段和尾段空闲不混入。
                    recordInternalIdleGap(context, entry.getKey(), results, shifts, quantities,
                            previousPositive, position);
                }
                previousPositive = position;
            }
            if (machineIdleShifts > 0) {
                machineCount++;
                idleShiftCount += machineIdleShifts;
            }
        }
        String summary = String.format("工厂=%s, 批次=%s, 物理机台数=%s, 中间空班机台数=%s, 整班空档数=%s, "
                        + "口径=全部物料及左右模合并，空班包含准备和业务等待，不等于可追回产能",
                context.getFactoryCode(), context.getBatchNo(), machines.size(), machineCount, idleShiftCount);
        PriorityTraceLogHelper.appendProcessLog(context, "机台中间空班审计汇总", summary);
        log.info("机台中间空班审计汇总, {}", summary);
        return machineCount;
    }

    /**
     * 记录每段空班的真实生产边界与交替时刻，不用SPEC_END_TIME或有后料代替连续利用证明。
     * @param context 最终上下文
     * @param machineCode 物理机台编码
     * @param results 同机全部结果
     * @param shifts 时间排序后的班次
     * @param quantities 合并班次量
     * @param beforePosition 空档前正量槽位
     * @param afterPosition 空档后正量槽位
     */
    private static void recordInternalIdleGap(LhScheduleContext context, String machineCode,
            List<LhScheduleResult> results, List<LhShiftConfigVO> shifts, int[] quantities,
            int beforePosition, int afterPosition) {
        int beforeIndex = shifts.get(beforePosition).getShiftIndex();
        int afterIndex = shifts.get(afterPosition).getShiftIndex();
        Date productionEnd = results.stream().filter(result -> hasPositiveQuantity(result, beforeIndex))
                .map(result -> ShiftFieldUtil.getShiftEndTime(result, beforeIndex)).filter(Objects::nonNull)
                .max(Date::compareTo).orElse(null);
        Date productionStart = results.stream().filter(result -> hasPositiveQuantity(result, afterIndex))
                .map(result -> ShiftFieldUtil.getShiftStartTime(result, afterIndex)).filter(Objects::nonNull)
                .min(Date::compareTo).orElse(null);
        Date changeoverStart = results.stream().filter(result -> hasPositiveQuantity(result, afterIndex))
                .map(LhScheduleResult::getMouldChangeStartTime).filter(Objects::nonNull)
                .filter(time -> Objects.nonNull(productionEnd) && !time.before(productionEnd))
                .min(Date::compareTo).orElse(null);
        List<Integer> idleShifts = shifts.subList(beforePosition + 1, afterPosition).stream()
                .map(LhShiftConfigVO::getShiftIndex).collect(Collectors.toList());
        String detail = String.format("工厂=%s, 批次=%s, 物理机台=%s, 班次量=%s, 中间空班=%s, 前物料=%s, 后物料=%s, "
                        + "前料最后有量结束=%s, 后料首个有量开始=%s, 无生产分钟=%s, 实际交替=%s, "
                        + "换模前等待分钟=%s, 说明=有后续排产仍存在整班空档；准备、维护、配额及候选限制需结合对应决策日志核对",
                context.getFactoryCode(), context.getBatchNo(), machineCode, Arrays.toString(quantities), idleShifts,
                materialStatusAtShift(results, beforeIndex), materialStatusAtShift(results, afterIndex),
                LhScheduleTimeUtil.formatDateTime(productionEnd), LhScheduleTimeUtil.formatDateTime(productionStart),
                elapsedMinutes(productionEnd, productionStart), LhScheduleTimeUtil.formatDateTime(changeoverStart),
                elapsedMinutes(productionEnd, changeoverStart));
        PriorityTraceLogHelper.appendProcessLog(context, "机台中间空班审计", detail);
        log.info("机台中间空班审计, {}", detail);
    }

    /** @param result 结果 @param shiftIndex 班次 @return 当前班次是否有正量 */
    private static boolean hasPositiveQuantity(LhScheduleResult result, int shiftIndex) {
        Integer quantity = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
        return Objects.nonNull(quantity) && quantity > 0;
    }

    /** @param results 同机结果 @param shiftIndex 班次 @return 该班实际生产的物料及状态，左右模同料去重 */
    private static String materialStatusAtShift(List<LhScheduleResult> results, int shiftIndex) {
        return results.stream().filter(result -> hasPositiveQuantity(result, shiftIndex))
                .map(result -> result.getMaterialCode() + "/" + result.getProductStatus()).distinct().sorted()
                .collect(Collectors.joining(","));
    }

    /** @param startTime 开始 @param endTime 结束 @return 等待分钟，缺少时间时为null，不冒充零等待 */
    private static Long elapsedMinutes(Date startTime, Date endTime) {
        return Objects.nonNull(startTime) && Objects.nonNull(endTime)
                ? Math.max(0L, endTime.getTime() - startTime.getTime()) / 60000L : null;
    }

    /**
     * 按最终有量槽位及各台节点核对收尾；不再要求旧规则的相邻班次跨度。
     * @param context 最终上下文
     * @param expectedGroups 提交时参与生产的物理机台
     * @param productionGroups 最终保留结果的生产收尾班次
     * @param invalidGroups 缺失结果、无量或不满足中班截止的组
     * @return 生产收尾约束未满足的组数
     */
    private static int auditProductionEndingGroups(LhScheduleContext context,
            Map<String, Set<String>> expectedGroups, Map<String, Map<String, Integer>> productionGroups,
            Set<String> invalidGroups) {
        int differences = 0;
        for (Map.Entry<String, Set<String>> entry : expectedGroups.entrySet()) {
            Map<String, Integer> actual = productionGroups.getOrDefault(entry.getKey(), java.util.Collections.emptyMap());
            int span = actual.isEmpty() ? -1 : java.util.Collections.max(actual.values()) - java.util.Collections.min(actual.values());
            boolean satisfied = actual.size() == entry.getValue().size() && !invalidGroups.contains(entry.getKey())
                    && span >= 0;
            String detail = String.format("factoryCode=%s, batchNo=%s, group=%s, 应核对物理机台=%s, "
                            + "生产收尾班次=%s, 生产收尾跨度=%s, 生产收尾约束满足=%s",
                    context.getFactoryCode(), context.getBatchNo(), entry.getKey(), entry.getValue(), actual, span, satisfied);
            PriorityTraceLogHelper.appendProcessLog(context, "续作余量收尾生产组级复核", detail);
            if (!satisfied) {
                differences++;
                log.warn("续作余量收尾生产组级复核, {}", detail);
            } else {
                log.info("续作余量收尾生产组级复核, {}", detail);
            }
        }
        return differences;
    }

    /** @param context 上下文 @param original 原续作 @param snapshot 提交快照 @param assigned 同物理机台结果 @return 最早后续动作 */
    private static LhScheduleResult findFollowing(LhScheduleContext context, LhScheduleResult original,
            ContinuationEndingAllocationSnapshot snapshot, List<LhScheduleResult> assigned) {
        if (CollectionUtils.isEmpty(assigned)) {
            return null;
        }
        return assigned.stream().filter(result -> result != original && Objects.nonNull(result.getMouldChangeStartTime()))
                .filter(result -> {
                    ContinuationEndingAllocationSnapshot other = context.getContinuationSurplusEndingSnapshotMap().get(result);
                    return Objects.isNull(other) || !StringUtils.equals(other.getGroupKey(), snapshot.getGroupKey());
                })
                .filter(result -> Objects.isNull(snapshot.getProductionStartTime())
                        || !result.getMouldChangeStartTime().before(snapshot.getProductionStartTime()))
                .min(Comparator.comparing(LhScheduleResult::getMouldChangeStartTime)).orElse(null);
    }

    /** @param context 上下文 @param result 最终前料 @param snapshot 提交快照 @param following 正式后料 @return 只读差异原因 */
    private static List<String> inspectResult(LhScheduleContext context, LhScheduleResult result,
            ContinuationEndingAllocationSnapshot snapshot, LhScheduleResult following) {
        List<String> reasons = new ArrayList<>(6);
        if (!Arrays.equals(snapshot.getShiftQuantities(), readQuantities(result, context.getScheduleWindowShifts()))) {
            reasons.add("后续阶段改变了已提交班次量或跨日归属");
        }
        Date finalEnd = lastEnd(result);
        if (!Objects.equals(snapshot.getProductionEndTime(), finalEnd)) {
            reasons.add("后置时间轴与续作提交快照不同");
        }
        if (Objects.isNull(following)) {
            return reasons;
        }
        Date change = following.getMouldChangeStartTime();
        if ((Objects.nonNull(finalEnd) && change.before(finalEnd))
                || (Objects.nonNull(snapshot.getReleaseTime()) && change.before(snapshot.getReleaseTime()))) {
            reasons.add("实际交替早于前料生产结束或物理释放边界");
        }
        boolean typeBlock = StringUtils.equals("1", following.getIsTypeBlock());
        int duration = typeBlock ? LhScheduleTimeUtil.getTypeBlockChangeTotalHours(context)
                : LhScheduleTimeUtil.getMouldChangeTotalHours(context);
        if (Objects.nonNull(snapshot.getPredictedChangeTime()) && !Objects.equals(change, snapshot.getPredictedChangeTime())) {
            reasons.add(duration != snapshot.getPredictedDurationHours()
                    ? "后料动作/时长与通用预测不同" : "正式资源快照、动作边界或候选顺序与续作预测不同");
        }
        if (LhScheduleTimeUtil.isNoMouldChangeTime(context, change)) {
            reasons.add("正式交替开始落入禁换模时段");
        }
        Date changeComplete = LhScheduleTimeUtil.addHours(change, duration);
        int firstShiftIndex = firstIndex(following);
        Date productionStart = firstStart(following);
        // 首检工具存储的是归属班次范围，不能把班次起点冒充独立首检时刻；核对班次是否能承接交替完成。
        // 精确识别普通首检原因项，喷砂清洗首检使用独立时间轴，不能按子串误判。
        if (ShiftFieldUtil.hasShiftAnalysis(following, firstShiftIndex,
                FirstInspectionQtyUtil.FIRST_INSPECTION_ANALYSIS)) {
            Date firstEnd = ShiftFieldUtil.getShiftEndTime(following, firstShiftIndex);
            if (Objects.isNull(firstEnd) || firstEnd.before(changeComplete)) {
                reasons.add("首检归属班次结束早于交替完成或时间缺失");
            }
        } else if (Objects.isNull(productionStart) || productionStart.before(changeComplete)) {
            reasons.add("后料首个生产时间早于交替完成或时间缺失");
        }
        return reasons;
    }

    /** @param result 结果 @param shifts 班次 @return 当前班次量 */
    private static int[] readQuantities(LhScheduleResult result, List<LhShiftConfigVO> shifts) {
        return shifts.stream().mapToInt(shift -> {
            Integer quantity = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
            return Objects.nonNull(quantity) ? quantity : 0;
        }).toArray();
    }

    /** @param result 结果 @return 首个生产开始时间 */
    private static Date firstStart(LhScheduleResult result) {
        int index = firstIndex(result);
        return index > 0 ? ShiftFieldUtil.getShiftStartTime(result, index) : null;
    }

    /** @param result 结果 @return 首个有量槽位 */
    private static int firstIndex(LhScheduleResult result) {
        for (int index = 1; index <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; index++) {
            Integer quantity = ShiftFieldUtil.getShiftPlanQty(result, index);
            if (Objects.nonNull(quantity) && quantity > 0) {
                return index;
            }
        }
        return -1;
    }

    /** @param result 结果 @return 最后生产结束时间 */
    private static Date lastEnd(LhScheduleResult result) {
        int index = ShiftFieldUtil.resolveLastPlannedShiftIndex(result);
        return index > 0 ? ShiftFieldUtil.getShiftEndTime(result, index) : null;
    }

    /** @param shifts 班次 @param time 真实时间 @return 所在班次，窗口外为-1 */
    private static int resolveShift(List<LhShiftConfigVO> shifts, Date time) {
        if (Objects.nonNull(time)) {
            for (LhShiftConfigVO shift : shifts) {
                if (!time.before(shift.getShiftStartDateTime()) && time.before(shift.getShiftEndDateTime())) {
                    return shift.getShiftIndex();
                }
            }
        }
        return -1;
    }
}
