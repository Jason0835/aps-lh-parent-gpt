package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Collections;

/**
 * 普通续作SKU的结构收尾、延期产能及首个可排业务日增机统一判断，不修改正式结果和消费账本。
 *
 * @author APS
 * @date 2026/10/27
 */
@Slf4j
public final class SkuEndingDeferredExpansionPolicy {
    /**
     * 只读消费余量入口，不调用会懒加载或同步数量账本的方法。
     */
    private static final TargetScheduleQtyResolver QUANTITY_READER = new TargetScheduleQtyResolver();
    /**
     * 复用续作真实班产、日标准量、设备占用和班次管控的计算公式。
     */
    private static final ContinuationMachineCapacitySupport CAPACITY_SUPPORT = new ContinuationMachineCapacitySupport();

    /**
     * 工具类禁止实例化。
     */
    private SkuEndingDeferredExpansionPolicy() {
    }

    /**
     * 统一评估已收尾结构的普通续作需求，只读取稳定结果及独立产能快照。
     *
     * @param context 正式排程上下文
     * @param sku 来源续作或已绑定本规则的补偿SKU
     * @return 适用性、真实余量、有效机台产能及剩余缺口
     */
    public static EndingDeferredExpansionDecision evaluate(LhScheduleContext context, SkuScheduleDTO sku) {
        LocalDate confirmedDate = resolveFirstProductionDate(context, sku);
        LocalDate evaluationDate = confirmedDate;
        if (evaluationDate == null && context != null && context.getScheduleDate() != null) {
            evaluationDate = toDate(context.getScheduleDate());
        }
        return evaluate(context, sku, evaluationDate);
    }

    /**
     * 按当前尝试业务日评估未覆盖产能，防止后续日重试时重复使用已经过去的空闲时段。
     *
     * @param context 正式排程上下文
     * @param sku 来源续作或本规则补偿SKU
     * @param evaluationDate 当前尝试或已确认的首个增机业务日
     * @return 不修改结果和账本的产能缺口
     */
    public static EndingDeferredExpansionDecision evaluate(LhScheduleContext context, SkuScheduleDTO sku,
            LocalDate evaluationDate) {
        EndingDeferredExpansionDecision decision = new EndingDeferredExpansionDecision();
        if (context == null || sku == null || context.isTimedMachineOffLoadPreview()
                || context.isIsolatedNextShiftPlan() || context.isStructureSwitchScheduling()
                || context.getEndingDeferredCapacitySnapshot() == null
                || context.getScheduleDate() == null || context.getWindowEndDate() == null || evaluationDate == null) {
            return decision;
        }
        LocalDate endingDate = context.getStructureMaxEndingDateMap().get(sku.getStructureName());
        if (endingDate == null || endingDate.isAfter(toDate(context.getWindowEndDate()))
                || !context.getMonthPlanList().stream().filter(Objects::nonNull)
                .anyMatch(plan -> sameSku(sku, plan.getMaterialCode(), plan.getProductStatus()))
                || !context.getContinuousSkuList().stream().filter(Objects::nonNull)
                .anyMatch(source -> sameSku(sku, source.getMaterialCode(), source.getProductStatus()))
                || context.isTemporaryFaultTransferSku(sku)
                || context.resolveForcedRequeueNotBeforeTime(sku) != null) {
            return decision;
        }
        decision.setApplicable(true);
        decision.setStructureEndingDate(endingDate);
        int remainingQty = resolveRemainingQty(context, sku);
        Map<String, LhScheduleResult> latestResults = resolveLatestMachineResults(context);
        Set<String> physicalMachines = new LinkedHashSet<String>();
        Map<String, List<Map<Integer, Integer>>> physicalCapacities = new LinkedHashMap<>();
        Set<String> pairedMachines = new LinkedHashSet<>();
        for (LhScheduleResult result : latestResults.values()) {
            MachineScheduleDTO machine = context.getMachineScheduleMap().get(result.getLhMachineCode());
            if (!sameSku(sku, result.getMaterialCode(), result.getProductStatus())
                    || machine == null || !StringUtils.equals(sku.getMaterialCode(), machine.getCurrentMaterialCode())
                    || context.resolveForcedReleasedPhysicalMachineCodes(sku).contains(
                    LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode()))) {
                continue;
            }
            String physicalCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode());
            physicalMachines.add(physicalCode);
            physicalCapacities.computeIfAbsent(physicalCode, key -> new ArrayList<>())
                    .add(resolveRemainingCapacity(context, result, evaluationDate));
            if (LhSingleControlMachineUtil.isConfiguredSingleControlMachine(context, result.getLhMachineCode())
                    && LhSingleControlMachineUtil.isWholeMachineGranularitySku(context, sku)) {
                pairedMachines.add(physicalCode);
            }
        }
        long capacityQty = 0L;
        for (Map.Entry<String, List<Map<Integer, Integer>>> entry : physicalCapacities.entrySet()) {
            List<Map<Integer, Integer>> sides = entry.getValue();
            if (!pairedMachines.contains(entry.getKey())) {
                capacityQty += sides.stream().flatMap(side -> side.values().stream()).mapToLong(Integer::longValue).sum();
            } else if (sides.size() == 2) {
                // 整机双侧必须逐班共同生产，单侧故障不能被另一侧的富余产能抵消。
                for (Map.Entry<Integer, Integer> shift : sides.get(0).entrySet()) {
                    capacityQty += 2L * Math.min(shift.getValue(), sides.get(1).getOrDefault(shift.getKey(), 0));
                }
            }
        }
        decision.setRemainingQty(remainingQty);
        decision.setCommittedCapacityQty((int) Math.min(Integer.MAX_VALUE, capacityQty));
        decision.setCommittedMachineCount(physicalMachines.size());
        decision.setShortageQty(Math.max(0, remainingQty - decision.getCommittedCapacityQty()));
        return decision;
    }

    /**
     * 按当前有效台数只开放下一台机会，提交后重算，避免冻结台数与多轮排量重复使用。
     *
     * @param context 正式排程上下文
     * @param sku 待增机SKU
     * @param productionDate 实际竞争业务日
     * @param ordinaryTarget 原有统一Map目标台数
     * @return 本规则的动态总台数上限；其他来源返回原目标
     */
    public static int resolveTargetMachineCount(LhScheduleContext context, SkuScheduleDTO sku,
            LocalDate productionDate, int ordinaryTarget) {
        if (sku == null || !sku.isEndingDeferredExpansion()) {
            return ordinaryTarget;
        }
        int committed = DailyMachineExpansionPlanner.countCommittedMachineDemand(context, sku, productionDate);
        return committed + (allowsExpansion(context, sku, productionDate) ? 1 : 0);
    }

    /**
     * 判断当前业务日能否尝试新机台；首日未确认时逐日重试，确认后只允许同日追加。
     *
     * @param context 排程上下文
     * @param sku 当前候选
     * @param productionDate 本次实际生产业务日
     * @return 日期合法且延期后仍有缺口时放行，其他来源沿用原规则
     */
    public static boolean allowsExpansion(LhScheduleContext context, SkuScheduleDTO sku, LocalDate productionDate) {
        if (sku == null || !sku.isEndingDeferredExpansion()) {
            return true;
        }
        return isFirstDayAllowed(context, sku, productionDate)
                && evaluate(context, sku, productionDate).needsExpansion();
    }

    /**
     * 日期门禁独立于缺口判断，供提交后余量已扣减时核对首次实际开产日期。
     *
     * @param context 排程上下文
     * @param sku 本次需求
     * @param productionDate 实际首检或生产业务日
     * @return 窗口内尚未锁日或与已确认首日一致时返回true
     */
    public static boolean isFirstDayAllowed(LhScheduleContext context, SkuScheduleDTO sku, LocalDate productionDate) {
        if (!isInScheduleWindow(context, productionDate)) {
            return false;
        }
        LocalDate firstDate = resolveFirstProductionDate(context, sku);
        return firstDate == null || firstDate.equals(productionDate);
    }

    /**
     * 读取同一物料状态共享的实际首日，不借用候选池日期或原机续作日期。
     *
     * @param context 本批上下文
     * @param sku 需求SKU
     * @return 已确认首日；尚无成功增机时为空
     */
    public static LocalDate resolveFirstProductionDate(LhScheduleContext context, SkuScheduleDTO sku) {
        if (context == null || sku == null) {
            return null;
        }
        return context.getEndingDeferredFirstProductionDateMap().get(buildSkuKey(sku));
    }

    /**
     * 解析当前允许继续竞争的日期；尚未成功时返回不早于调用日起点的窗口日期。
     *
     * @param context 当前批次
     * @param sku 本规则需求
     * @param requestedDate 当前扫描起点，可为空
     * @return 有缺口且日期合法时返回尝试日期，否则为空；不确认实际首日
     */
    public static LocalDate resolveAttemptDate(LhScheduleContext context, SkuScheduleDTO sku, LocalDate requestedDate) {
        if (context == null || context.getScheduleDate() == null) {
            return null;
        }
        LocalDate windowStart = toDate(context.getScheduleDate());
        LocalDate attemptDate = requestedDate == null || requestedDate.isBefore(windowStart) ? windowStart : requestedDate;
        LocalDate firstDate = resolveFirstProductionDate(context, sku);
        if (firstDate != null) {
            if (attemptDate.isAfter(firstDate)) {
                return null;
            }
            attemptDate = firstDate;
        }
        return allowsExpansion(context, sku, attemptDate) ? attemptDate : null;
    }

    /**
     * 日驱动预演只接受本业务日真实可开产的提案，较晚候选留到对应日并复用准备回看。
     *
     * @param context 当前批次
     * @param sku 当前候选
     * @param attemptDate 正在扫描的业务日
     * @param actualProductionDate 完整时间轴确定的首个实际生产业务日
     * @return 普通来源放行，本规则要求按日顺序且首日一致
     */
    public static boolean allowsProposal(LhScheduleContext context, SkuScheduleDTO sku,
            LocalDate attemptDate, LocalDate actualProductionDate) {
        return sku == null || !sku.isEndingDeferredExpansion()
                || (Objects.equals(attemptDate, actualProductionDate)
                && allowsExpansion(context, sku, actualProductionDate));
    }

    /**
     * 复核本次新增正量结果并原子确认SKU首日，零量或失败候选不会锁日。
     *
     * @param context 实际提交后的上下文
     * @param selectedSku 当前提案来源，可为空时从结果来源映射读取
     * @param beforeResultCount 提交前结果数
     * @return 全部本规则结果首日一致时返回true；失败时调用方恢复完整快照
     */
    public static boolean confirmCommittedFirstDates(LhScheduleContext context, SkuScheduleDTO selectedSku,
            int beforeResultCount) {
        Map<String, LocalDate> pendingDates = new LinkedHashMap<>();
        for (int index = beforeResultCount; index < context.getScheduleResultList().size(); index++) {
            LhScheduleResult result = context.getScheduleResultList().get(index);
            SkuScheduleDTO source = context.getScheduleResultSourceSkuMap().get(result);
            if (source == null && selectedSku != null
                    && sameSku(selectedSku, result.getMaterialCode(), result.getProductStatus())) {
                source = selectedSku;
            }
            if (source == null || !source.isEndingDeferredExpansion() || ShiftFieldUtil.resolveScheduledQty(result) <= 0) {
                continue;
            }
            LocalDate actualDate = resolveFirstActualProductionDate(context, result);
            if (!isFirstDayAllowed(context, source, actualDate)) {
                return false;
            }
            String key = buildSkuKey(source);
            LocalDate previous = pendingDates.putIfAbsent(key, actualDate);
            if (previous != null && !previous.equals(actualDate)) {
                return false;
            }
        }
        pendingDates.forEach((key, date) -> {
            if (context.getEndingDeferredFirstProductionDateMap().putIfAbsent(key, date) == null) {
                String detail = "factoryCode=" + context.getFactoryCode() + ", batchNo=" + context.getBatchNo()
                        + ", sku=" + key + ", firstProductionDate=" + date;
                PriorityTraceLogHelper.appendProcessLog(context, "SKU收尾延期增机首日确认", detail);
                if (log.isInfoEnabled()) {
                    log.info("SKU收尾延期增机首日确认, factoryCode: {}, batchNo: {}, sku: {}, firstProductionDate: {}",
                            context.getFactoryCode(), context.getBatchNo(), key, date);
                }
            }
        });
        return true;
    }

    /**
     * 读取实际首检占用日；没有计件首检时读取首个正量结果班次，避免计量前移改变日期。
     *
     * @param context 正式结果及实际首检计划
     * @param result 新增正量结果
     * @return 首个实际生产业务日，无法确定时为空
     */
    private static LocalDate resolveFirstActualProductionDate(LhScheduleContext context, LhScheduleResult result) {
        FirstInspectionAllocationPlan inspection = context.getFirstInspectionResultPlanMap().get(result);
        if (inspection != null && inspection.isValid() && inspection.getInspectionQty() > 0) {
            for (FirstInspectionShiftAllocation allocation : inspection.getShiftAllocations()) {
                if (allocation != null && allocation.getQuantity() > 0 && allocation.getShift() != null) {
                    return toDate(allocation.getShift().getWorkDate());
                }
            }
        }
        for (LhShiftConfigVO shift : context.getScheduleWindowShifts()) {
            Integer qty = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
            if (qty != null && qty > 0) {
                return toDate(shift.getWorkDate());
            }
        }
        return null;
    }

    /**
     * 输出可关联批次、SKU和延期边界的正式决策日志。
     *
     * @param context 正式排程上下文
     * @param sku 来源SKU
     * @param decision 本次只读判断
     */
    public static void trace(LhScheduleContext context, SkuScheduleDTO sku, EndingDeferredExpansionDecision decision) {
        if (decision.isApplicable() && log.isInfoEnabled()) {
            EndingDeferredCapacitySnapshot snapshot = context.getEndingDeferredCapacitySnapshot();
            log.info("SKU收尾延期增机判断, factoryCode: {}, batchNo: {}, T: {}, materialCode: {}, productStatus: {}, "
                            + "structureName: {}, structureEndingDate: {}, delayDays: {}, windowEnd: {}, deferredEnd: {}, "
                            + "remainingQty: {}, committedMachineCount: {}, committedCapacityQty: {}, shortageQty: {}, "
                            + "nextMachineOpportunity: {}, firstProductionDate: {}",
                    context.getFactoryCode(), context.getBatchNo(), toDate(context.getScheduleDate()), sku.getMaterialCode(),
                    sku.getProductStatus(), sku.getStructureName(), decision.getStructureEndingDate(),
                    context.getScheduleConfig().getSkuEndingDeferredExpansionDays(), snapshot.getWindowEndTime(),
                    snapshot.getDeferredEndTime(), decision.getRemainingQty(), decision.getCommittedMachineCount(),
                    decision.getCommittedCapacityQty(), decision.getShortageQty(), decision.needsExpansion() ? 1 : 0,
                    resolveFirstProductionDate(context, sku));
        }
    }

    /**
     * 复用非严格补偿的真实硫化余量口径，严格场景读取中心剩余且不再次扣已排量。
     *
     * @param context 当前批次
     * @param sku 来源SKU
     * @return 不重复扣账的合法剩余量
     */
    private static int resolveRemainingQty(LhScheduleContext context, SkuScheduleDTO sku) {
        if (sku.getSurplusQty() > 0 && !ProductionQuantityPolicy.from(sku, sku.isStrictTargetQty()).isStrictUpperLimit()) {
            long scheduledQty = context.getScheduleResultList().stream().filter(Objects::nonNull)
                    .filter(result -> sameSku(sku, result.getMaterialCode(), result.getProductStatus()))
                    .mapToLong(ShiftFieldUtil::resolveScheduledQty).sum();
            return (int) Math.max(0L, sku.getSurplusQty() - scheduledQty);
        }
        return QUANTITY_READER.previewProductionRemainingQty(context, sku);
    }

    /**
     * 每个运行侧只保留最后一个有量生产段，防止已换料的历史正量继续贡献产能。
     *
     * @param context 当前批次
     * @return 运行侧编码到最新有效结果的映射
     */
    private static Map<String, LhScheduleResult> resolveLatestMachineResults(LhScheduleContext context) {
        Map<String, LhScheduleResult> latest = new LinkedHashMap<String, LhScheduleResult>();
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (result == null || ShiftFieldUtil.resolveScheduledQty(result) <= 0) {
                continue;
            }
            LhScheduleResult previous = latest.get(result.getLhMachineCode());
            if (previous == null || (result.getSpecEndTime() != null && previous.getSpecEndTime() != null
                    && !result.getSpecEndTime().before(previous.getSpecEndTime()))) {
                latest.put(result.getLhMachineCode(), result);
            }
        }
        return latest;
    }

    /**
     * 计算本侧尚未提交的后续业务日及延期产能，已提交日期不再重复计入。
     *
     * @param context 正式上下文
     * @param result 该运行侧最新生产段
     * @param evaluationDate 当前尝试业务日，之前未利用的产能不得再计入
     * @return 后续各班可完成量，数据缺失时为空
     */
    private static Map<Integer, Integer> resolveRemainingCapacity(LhScheduleContext context, LhScheduleResult result,
            LocalDate evaluationDate) {
        if (result.getSingleMouldShiftQty() == null || result.getSingleMouldShiftQty() <= 0
                || result.getLhTime() == null || result.getLhTime() <= 0
                || result.getMouldQty() == null || result.getMouldQty() <= 0) {
            return Collections.emptyMap();
        }
        EndingDeferredCapacitySnapshot snapshot = context.getEndingDeferredCapacitySnapshot();
        Date coveredEnd = null;
        for (LhShiftConfigVO shift : context.getScheduleWindowShifts()) {
            Integer quantity = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
            if (quantity != null && quantity > 0) {
                Date dayEnd = context.getScheduleWindowShifts().stream()
                        .filter(sameDay -> Objects.equals(sameDay.getWorkDate(), shift.getWorkDate()))
                        .map(LhShiftConfigVO::getShiftEndDateTime).max(Date::compareTo).orElse(shift.getShiftEndDateTime());
                if (coveredEnd == null || dayEnd.after(coveredEnd)) {
                    coveredEnd = dayEnd;
                }
            }
        }
        if (coveredEnd == null) {
            return Collections.emptyMap();
        }
        LhScheduleContext projection = snapshot.getCapacityContext();
        List<LhShiftConfigVO> remainingShifts = new ArrayList<LhShiftConfigVO>();
        for (LhShiftConfigVO shift : projection.getScheduleWindowShifts()) {
            if (!shift.getShiftStartDateTime().before(coveredEnd)
                    && !toDate(shift.getWorkDate()).isBefore(evaluationDate)) {
                remainingShifts.add(shift);
            }
        }
        Map<Integer, Integer> capacities = CAPACITY_SUPPORT.calculateDailyStandardShiftCapacityMap(projection, result,
                remainingShifts, coveredEnd, result.getSingleMouldShiftQty(), result.getLhTime(), result.getMouldQty(),
                CAPACITY_SUPPORT.resolveEffectiveCleaningWindowList(projection, result, coveredEnd),
                CAPACITY_SUPPORT.resolveMachineMaintenanceWindowList(projection, result.getLhMachineCode()),
                "SKU收尾延期只读评估");
        return capacities;
    }

    /**
     * 按物料和归一化产品状态隔离需求。
     *
     * @param sku 待判断SKU
     * @param materialCode 对比物料
     * @param productStatus 对比状态
     * @return 同一物料状态时返回true
     */
    private static boolean sameSku(SkuScheduleDTO sku, String materialCode, String productStatus) {
        return StringUtils.equals(sku.getMaterialCode(), materialCode)
                && StringUtils.equals(StringUtils.defaultIfBlank(sku.getProductStatus(), "S"),
                StringUtils.defaultIfBlank(productStatus, "S"));
    }

    /**
     * 检查增机实际日期属于正式排程窗口，延期评估区间不授予窗口外排产资格。
     *
     * @param context 正式上下文
     * @param date 实际业务日
     * @return 位于窗口起止日内时返回true
     */
    private static boolean isInScheduleWindow(LhScheduleContext context, LocalDate date) {
        return context != null && date != null && context.getScheduleDate() != null && context.getWindowEndDate() != null
                && !date.isBefore(toDate(context.getScheduleDate())) && !date.isAfter(toDate(context.getWindowEndDate()));
    }

    /**
     * 复用物料状态中心键，空状态按正规归一化以便跨入口共享首日。
     *
     * @param sku 来源SKU
     * @return 本批物料状态键
     */
    private static String buildSkuKey(SkuScheduleDTO sku) {
        return MonthPlanDateResolver.buildMaterialStatusKey(sku.getMaterialCode(),
                StringUtils.defaultIfBlank(sku.getProductStatus(), "S"));
    }

    /**
     * 将日期按项目运行时区转换为自然日。
     *
     * @param date 原日期
     * @return 对应自然日
     */
    private static LocalDate toDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
