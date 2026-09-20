package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.cx.entity.config.CxEmbryoLhTime;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.constant.LhScheduleParamConstant;
import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineMaintenanceWindowDTO;
import com.zlt.aps.lh.api.domain.dto.ShiftProductionControlDTO;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import com.zlt.aps.lh.util.ShiftProductionControlUtil;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.FirstInspectionQtyUtil;
import com.zlt.aps.lh.util.FirstInspectionAllocationUtil;
import com.zlt.aps.lh.util.FirstInspectionTimingMode;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 结构切换公共策略：只读试算、分班准入、局部扩机排序和提交后的首班登记。
 * 现有结构资格、设备、模具和数量账本仍由原服务管理；本类不建立平行资源计数。
 */
@Slf4j
public final class StructureSwitchSchedulingPolicy {
    /** 明确标识为1才启用；历史空值按0处理。 */
    private static final int YES = 1;
    /** 首个上机班上限。 */
    private static final int FIRST_SHIFT_LIMIT = 1;
    /** 等结果第二班上限。 */
    private static final int SECOND_SHIFT_LIMIT = 2;
    /** 批量计划量必须为偶数。 */
    private static final int EVEN_UNIT = 2;

    private StructureSwitchSchedulingPolicy() {
    }

    /**
     * 统一读取本批次开关；缺少参数快照的既有调用按默认开启，空上下文不执行策略。
     * @param context 排程上下文
     * @return 是否启用本次结构切换规则
     */
    public static boolean isEnabled(LhScheduleContext context) {
        return Objects.nonNull(context) && (Objects.isNull(context.getScheduleConfig())
                || context.getScheduleConfig().isStructureSwitchSchedulingEnabled());
    }

    /**
     * 复用原始结构切换识别，已落地切换可跨日继续执行递进限制。
     * @param context 本次排程上下文
     * @param sku 候选SKU
     * @return 同一有效来源记录；非适用场景返回null
     */
    public static CxEmbryoLhTime resolveSource(LhScheduleContext context, SkuScheduleDTO sku) {
        if (!isEnabled(context) || context.isStructureSwitchSelectionProbe()
                || Objects.isNull(sku) || StringUtils.isEmpty(sku.getStructureName())) {
            return null;
        }
        CxEmbryoLhTime source = context.getStructureSwitchSourceMap().get(sku.getStructureName());
        if (Objects.isNull(source) || Objects.isNull(source.getEarliestLhTime())) {
            return null;
        }
        StructureSwitchRuntimeState state = context.getStructureSwitchRuntimeMap().get(sku.getStructureName());
        if (Objects.nonNull(state) && Objects.equals(source.getId(), state.getSourceId())) {
            return source;
        }
        if (NewSpecEmbryoAvailableTimeResolver.isStructureScheduledInCurrentContinuation(context, sku)) {
            return null;
        }
        Date businessDate = Objects.nonNull(context.getCurrentScheduleDate())
                ? context.getCurrentScheduleDate() : context.getScheduleDate();
        if (Objects.isNull(businessDate)) {
            return null;
        }
        LocalDate currentDate = businessDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate futureDate = EarlyProductionChecker.resolveFirstFuturePlanDate(context, sku, currentDate);
        if (EarlyProductionChecker.isStructureSwitchEarlyProduction(context, sku, currentDate, futureDate)) {
            return source;
        }
        // 时间轴顺延到有计划日后，仍使用本窗口此前的原始结构切换资格；只定位场景，不登记S0。
        if (Objects.isNull(context.getScheduleDate())) {
            return null;
        }
        LocalDate sourceDate = context.getScheduleDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        while (sourceDate.isBefore(currentDate)) {
            LocalDate sourceFutureDate = EarlyProductionChecker.resolveFirstFuturePlanDate(context, sku, sourceDate);
            if (EarlyProductionChecker.isStructureSwitchEarlyProduction(context, sku, sourceDate, sourceFutureDate)) {
                return source;
            }
            sourceDate = sourceDate.plusDays(1);
        }
        return null;
    }

    /**
     * 判断当前候选是否应跳过普通结构班次机台数门禁。
     *
     * <p>结构切换一旦取得有效来源，即使时间轴已从提前生产推进到正常资源竞争，仍由
     * S0/S1 专用逐班名额约束接管，不能重新叠加普通结构机台数门禁。结构收尾提前生产
     * 继续沿用既有专用准入，其他普通新增和普通提前生产保持原门禁不变。</p>
     *
     * @param context 排程上下文
     * @param sku 当前候选SKU
     * @param phase 当前排程阶段
     * @param earlyProductionDecision 当前提前生产准入结论
     * @return true-跳过普通结构班次门禁；false-继续执行普通结构班次门禁
     */
    public static boolean shouldSkipNormalStructureShiftLimit(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            DailySchedulePhase phase,
            EarlyProductionDecision earlyProductionDecision) {
        if (Objects.nonNull(resolveSource(context, sku))) {
            return true;
        }
        if (phase != DailySchedulePhase.EARLY_PRODUCTION
                || Objects.isNull(earlyProductionDecision)
                || !earlyProductionDecision.isEarlyProduction()
                || !earlyProductionDecision.isAllowed()) {
            return false;
        }
        return StringUtils.equals(
                EarlyProductionDecision.SCENE_STRUCTURE_SWITCH,
                earlyProductionDecision.getSceneType())
                || StringUtils.equals(
                EarlyProductionDecision.SCENE_STRUCTURE_ENDING,
                earlyProductionDecision.getSceneType());
    }

    /**
     * 判断记录是否明确标记大换英寸。
     * @param source 有效供胚记录
     * @return 是否启用大换英寸规则
     */
    public static boolean isLargeInch(CxEmbryoLhTime source) {
        return Objects.nonNull(source) && Objects.equals(YES, source.getIsBigInchChange());
    }

    /**
     * 大换英寸候选统一按含首检的真实可开产时间计算；每台只在首次上机时叠加延迟。
     * @param context 排程上下文
     * @param sku 候选SKU
     * @param readyTime 原机台就绪时刻
     * @return 是否需要结构切换首次开产时间轴
     */
    public static boolean requiresLargeTimeline(LhScheduleContext context, SkuScheduleDTO sku, Date readyTime) {
        return Objects.nonNull(readyTime) && isLargeInch(resolveSource(context, sku));
    }

    /**
     * 按真实起止时间取得连续班次，跨日不按班别重新计数。
     * @param context 排程上下文
     * @return 按时间升序的完整窗口班次
     */
    public static List<LhShiftConfigVO> orderedShifts(LhScheduleContext context) {
        return context.getScheduleWindowShifts().stream().filter(Objects::nonNull)
                .sorted(Comparator.comparing(LhShiftConfigVO::getShiftStartDateTime))
                .collect(Collectors.toList());
    }

    /**
     * 在完整窗口内解析结构分班名额允许的最早首检或生产时间，准备动作不随名额后移。
     * @param context 排程上下文
     * @param sku 候选SKU
     * @param machineCode 候选物理机台或运行侧
     * @param requestedStart 首检或生产请求起点
     * @return 最早允许占用时间；非结构切换保持原值，窗口内无名额返回null
     */
    public static Date resolveEarliestOccupationTime(LhScheduleContext context, SkuScheduleDTO sku,
            String machineCode, Date requestedStart) {
        CxEmbryoLhTime source = resolveSource(context, sku);
        if (Objects.isNull(source) || Objects.isNull(requestedStart)) {
            return requestedStart;
        }
        for (LhShiftConfigVO shift : orderedShifts(context)) {
            if (!requestedStart.before(shift.getShiftEndDateTime())) {
                continue;
            }
            Date allowedStart = later(requestedStart, shift.getShiftStartDateTime());
            StructureSwitchPlan candidate = createPlan(context, source, null, shift, requestedStart, allowedStart);
            // 复用预演和提交的同一计数规则，首班一台、等结果第二班两台；单控按物理机台去重。
            if (Objects.isNull(evaluate(context, candidate, sku, machineCode, null))) {
                return allowedStart;
            }
        }
        return null;
    }

    /**
     * 构建冻结提案，普通结构切换不修改原首检和生产时刻。
     * @param context 排程上下文
     * @param source 已匹配记录
     * @param inspection 首检计划
     * @param formalShift 正式生产班次
     * @param baseStart 未叠加延迟的起点
     * @param productionStart 正式生产起点
     * @return 切换计划；无正量落点时返回null
     */
    public static StructureSwitchPlan createPlan(LhScheduleContext context, CxEmbryoLhTime source,
            FirstInspectionAllocationPlan inspection, LhShiftConfigVO formalShift,
            Date baseStart, Date productionStart) {
        if (!isEnabled(context)) {
            return null;
        }
        LhShiftConfigVO first = formalShift;
        if (Objects.nonNull(inspection) && inspection.isValid() && inspection.getInspectionQty() > 0) {
            first = inspection.getShiftAllocations().stream().filter(item -> item.getQuantity() > 0)
                    .map(FirstInspectionShiftAllocation::getShift)
                    .min(Comparator.comparing(LhShiftConfigVO::getShiftStartDateTime)).orElse(formalShift);
        }
        if (Objects.isNull(source) || Objects.isNull(first)) {
            return null;
        }
        // S0/S1按首个正量维护；跨班首检不能被批量取偶裁掉，P0按本候选正式生产班次定位。
        LhShiftConfigVO bulk = null;
        if (isLargeInch(source)) {
            bulk = Objects.nonNull(formalShift) ? formalShift : first;
        }
        return new StructureSwitchPlan(source, first, bulk, baseStart, productionStart, inspection, false);
    }

    /**
     * 获取S0后紧邻的班次，不跳过零产能班次。
     * @param context 排程上下文
     * @param firstShiftStart S0开始时间
     * @return 紧邻班次，超出窗口返回null
     */
    public static LhShiftConfigVO nextShift(LhScheduleContext context, Date firstShiftStart) {
        return orderedShifts(context).stream().filter(shift -> shift.getShiftStartDateTime().after(firstShiftStart))
                .findFirst().orElse(null);
    }

    /**
     * 生成大换英寸含首检的首次开产时间轴；首检仍使用现有数量、时长和设备产能校验。
     * @param context 排程上下文
     * @param sku 候选SKU
     * @param machineCode 机台编码
     * @param readyTime 设备及准备完成时间
     * @param changeStart 换模开始
     * @param changeEnd 换模总时长结束（包含首检）
     * @param capacity 运行态班产
     * @param remaining 当前可消费量
     * @param scheduleType 原排程类型
     * @param capacityResolver 现有首检设备容量校验器
     * @return 合法冻结时间轴；首检不能完整落入窗口或设备容量不足时返回null
     */
    public static FirstInspectionTimelinePlan buildLargeTimeline(LhScheduleContext context, SkuScheduleDTO sku,
            String machineCode, Date readyTime, Date changeStart, Date changeEnd, int capacity,
            int remaining, String scheduleType,
            Function<FirstInspectionAllocationPlan, Map<Integer, Integer>> capacityResolver) {
        return buildLargeTimeline(context, sku, machineCode, readyTime, changeStart, changeEnd, capacity,
                remaining, scheduleType, null, capacityResolver);
    }

    /**
     * 在现有硬性开产门禁之后构建大换英寸时间轴。
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param machineCode 机台
     * @param readyTime 设备就绪
     * @param changeStart 换模开始
     * @param changeEnd 换模完成
     * @param capacity 班产
     * @param remaining 可消费量
     * @param scheduleType 排程类型
     * @param inspectionNotBeforeTime 现有SKU、结构占用及释放门禁
     * @param capacityResolver 原设备产能校验
     * @return 合法冻结时间轴，不可落地返回null
     */
    public static FirstInspectionTimelinePlan buildLargeTimeline(LhScheduleContext context, SkuScheduleDTO sku,
            String machineCode, Date readyTime, Date changeStart, Date changeEnd, int capacity,
            int remaining, String scheduleType,
            Date inspectionNotBeforeTime, Function<FirstInspectionAllocationPlan, Map<Integer, Integer>> capacityResolver) {
        CxEmbryoLhTime source = resolveSource(context, sku);
        if (!isLargeInch(source)) {
            return null;
        }
        List<LhShiftConfigVO> shifts = orderedShifts(context);
        // 按真实班次有限枚举，使倒推时长与最终首检归属自洽，避免4条倒推后按2条执行。
        FirstInspectionAllocationPlan inspection = resolveLargeInspectionPlan(context, sku, machineCode,
                readyTime, changeStart, changeEnd, capacity, remaining, scheduleType, inspectionNotBeforeTime);
        if (Objects.isNull(inspection)) {
            return null;
        }
        Date inspectionStart = inspection.getInspectionStartTime();
        Date machineStart = resolveLargeMachineStart(readyTime, changeStart, changeEnd,
                inspection.getInspectionDurationSeconds());
        Date baseStart = later(machineStart, source.getEarliestLhTime());
        Date formulaStart = resolveLargeFormulaStart(context, baseStart);
        inspection = FirstInspectionAllocationUtil.buildPlan(context, sku, shifts, changeEnd,
                inspectionStart, capacity, remaining, scheduleType, machineCode, capacityResolver.apply(inspection),
                FirstInspectionTimingMode.START_AT_OCCUPATION_BOUNDARY);
        if (!inspection.isValid() || inspection.getInspectionQty() <= 0) {
            return null;
        }
        Date formalStart = later(changeEnd, inspection.getInspectionEndTime());
        LhShiftConfigVO formalShift = FirstInspectionQtyUtil.resolveAttributionShift(shifts, formalStart);
        LhShiftConfigVO occupationShift = FirstInspectionQtyUtil.resolveAttributionShift(shifts, inspectionStart);
        StructureSwitchPlan preliminary = createPlan(context, source, inspection, formalShift, baseStart, formalStart);
        LhShiftConfigVO firstShift = preliminary.getFirstQuantityShift();
        StructureSwitchPlan plan = new StructureSwitchPlan(source, firstShift, preliminary.getFirstBatchShift(),
                baseStart, formalStart, inspection, true);
        log.info("结构切换含首检时间轴, sourceId={}, structure={}, sku={}, machine={}, changeStart={}, changeEnd={}, "
                        + "machineStart={}, embryoTime={}, formulaStart={}, inspectionStart={}, formalStart={}, firstShift={}",
                source.getId(), source.getNextStructureName(), sku.getMaterialCode(), machineCode, changeStart, changeEnd,
                machineStart, source.getEarliestLhTime(), formulaStart, inspectionStart, formalStart, firstShift.getShiftIndex());
        return FirstInspectionTimelinePlan.of(inspection, FirstInspectionTimingMode.START_AT_OCCUPATION_BOUNDARY,
                "按实际首检可开始时间与供胚取晚后加延迟，名额只后移生产，首检包含在班次总量内",
                changeStart, changeEnd, inspectionStart, formalStart, formalShift, occupationShift).withStructureSwitchPlan(plan);
    }

    /**
     * 解析数量、计数班次和公式起点一致的最早首检计划；每个真实班次至多试算一次。
     * @param context 排程上下文
     * @param sku 结构切换SKU
     * @param machineCode 候选机台
     * @param readyTime 额外设备准备下限
     * @param changeStart 已校验的切换开始时间
     * @param changeEnd 已校验的切换完成时间
     * @param capacity 运行态班产
     * @param remaining 当前可消费量
     * @param scheduleType 原排产类型
     * @param notBefore 首检或生产的既有门禁
     * @return 自洽且完整落在窗口内的计划；不存在合法班次时返回null
     */
    private static FirstInspectionAllocationPlan resolveLargeInspectionPlan(LhScheduleContext context,
            SkuScheduleDTO sku, String machineCode, Date readyTime, Date changeStart, Date changeEnd,
            int capacity, int remaining, String scheduleType, Date notBefore) {
        List<LhShiftConfigVO> shifts = orderedShifts(context);
        Date embryoTime = resolveSource(context, sku).getEarliestLhTime();
        for (LhShiftConfigVO shift : shifts) {
            if (!changeStart.before(shift.getShiftEndDateTime())
                    || FirstInspectionQtyUtil.isTrialTimeBasedFirstInspection(sku, shift, scheduleType)) {
                continue;
            }
            int sequence = FirstInspectionQtyUtil.resolveNextFirstInspectionSequence(context, shift);
            int inspectionQty = Math.min(Math.max(0, remaining), Math.max(0,
                    FirstInspectionQtyUtil.resolveAdjustedFirstInspectionQty(context, sequence, machineCode, false)));
            long duration = FirstInspectionAllocationUtil.resolveInspectionDurationSeconds(inspectionQty,
                    ShiftCapacityResolverUtil.resolveShiftDurationSeconds(shift), capacity);
            if (duration <= 0L) {
                continue;
            }
            Date machineStart = resolveLargeMachineStart(readyTime, changeStart, changeEnd, duration);
            Date formulaStart = resolveLargeFormulaStart(context, later(machineStart, embryoTime));
            // 名额和计数班次都只抬高起点，不能在顺延后的班次重新追加一小时。
            Date requestedStart = later(later(formulaStart, notBefore), shift.getShiftStartDateTime());
            if (!requestedStart.before(shift.getShiftEndDateTime())) {
                continue;
            }
            Date allowedStart = resolveEarliestOccupationTime(context, sku, machineCode, requestedStart);
            if (Objects.isNull(allowedStart) || !allowedStart.before(shift.getShiftEndDateTime())) {
                continue;
            }
            FirstInspectionAllocationPlan inspection = FirstInspectionAllocationUtil.buildPlan(context, sku,
                    shifts, changeEnd, allowedStart, capacity, remaining, scheduleType, machineCode, null,
                    FirstInspectionTimingMode.START_AT_OCCUPATION_BOUNDARY);
            if (inspection.isValid() && inspection.getInspectionDurationSeconds() == duration
                    && Objects.equals(shift.getShiftIndex(), inspection.getCountingShift().getShiftIndex())) {
                return inspection;
            }
        }
        return null;
    }

    /**
     * 从切换总时长倒推实际首检可开始时间，同时保留晚于切换的设备准备约束。
     * @param readyTime 额外设备准备时间
     * @param changeStart 切换开始时间
     * @param changeEnd 包含首检的切换完成时间
     * @param durationSeconds 最终计数班次对应的首检时长
     * @return 机台真实可开始首检的下限
     */
    private static Date resolveLargeMachineStart(Date readyTime, Date changeStart, Date changeEnd, long durationSeconds) {
        Date machineStart = later(changeStart,
                new Date(changeEnd.getTime() - TimeUnit.SECONDS.toMillis(durationSeconds)));
        return Objects.nonNull(readyTime) && readyTime.after(changeEnd) ? later(machineStart, readyTime) : machineStart;
    }

    /**
     * 在设备与供胚共同下限上应用一次大换英寸延迟。
     * @param context 参数快照
     * @param baseStart 设备与供胚取晚后的起点
     * @return 含首检的公式起点
     */
    private static Date resolveLargeFormulaStart(LhScheduleContext context, Date baseStart) {
        int delay = Objects.isNull(context.getScheduleConfig()) ? LhScheduleConstant.LARGE_INCH_FIRST_BATCH_DELAY_HOURS
                : context.getScheduleConfig().getLargeInchFirstBatchDelayHours();
        return new Date(baseStart.getTime() + TimeUnit.HOURS.toMillis(delay));
    }

    /**
     * 给普通结构切换计划附加状态，不改变任何原时间轴。
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param availability 原可用性计划
     * @return 携带切换元数据的计划或原非适用计划
     */
    public static NewSpecMachineAvailabilityPlan attachPlan(LhScheduleContext context, SkuScheduleDTO sku,
            NewSpecMachineAvailabilityPlan availability) {
        if (!isEnabled(context)) {
            return availability;
        }
        CxEmbryoLhTime source = resolveSource(context, sku);
        if (Objects.isNull(source) || Objects.isNull(availability) || !availability.isAvailable()
                || Objects.nonNull(availability.getStructureSwitchPlan())) {
            return availability;
        }
        StructureSwitchPlan plan = createPlan(context, source, availability.getFirstInspectionPlan(),
                availability.getFormalTargetShift(), availability.getMachineReadyTime(),
                availability.getFormalAvailableProductionTime());
        FirstInspectionTimelinePlan timeline = availability.getFirstInspectionTimelinePlan();
        if (Objects.isNull(timeline)) {
            timeline = FirstInspectionTimelinePlan.of(availability.getFirstInspectionPlan(),
                    FirstInspectionTimingMode.INCLUDED_IN_CHANGEOVER, "沿用普通结构切换时间轴",
                    availability.getChangeoverStartTime(), availability.getChangeoverEndTime(),
                    availability.getMachineReadyTime(), availability.getFormalAvailableProductionTime(),
                    availability.getFormalTargetShift(), availability.getProductionOccupationShift());
        }
        return availability.withFirstInspectionTimelinePlan(timeline.withStructureSwitchPlan(plan));
    }

    /**
     * 返回临时分班上限，恢复正常使用最大值表示不增加限制。
     * @param waitNotify 等结果标识，空按不等结果
     * @param shiftOffset 距离S0的连续班次偏移
     * @return 附加上限
     */
    public static int shiftLimit(Integer waitNotify, int shiftOffset) {
        if (shiftOffset == 0) {
            return FIRST_SHIFT_LIMIT;
        }
        return shiftOffset == 1 && Objects.equals(YES, waitNotify) ? SECOND_SHIFT_LIMIT : Integer.MAX_VALUE;
    }

    /**
     * 校验完整计划会涉及的受限班次，包含历史回看导致S0前移的已有结果。
     * @param context 排程上下文
     * @param plan 冻结切换计划
     * @param machineCode 候选机台
     * @param actualQuantities 实际数量；预演为空时核验首检和正式开始覆盖的受限班次
     * @return 拒绝原因；空值表示通过
     */
    public static String validate(LhScheduleContext context, StructureSwitchPlan plan, String machineCode,
            Map<Integer, Integer> actualQuantities) {
        StructureMachineLimitDecision decision = evaluate(context, plan, null, machineCode, actualQuantities);
        return Objects.isNull(decision) ? null : decision.getReason();
    }

    /**
     * 返回真实拒绝班次的准入决策，供现有候选失败键及日志复用。
     * @param context 排程上下文
     * @param plan 冻结切换计划
     * @param sku 当前SKU
     * @param machineCode 物理机台或运行侧
     * @param actualQuantities 实际数量，预演为空
     * @return 拒绝决策；通过返回null
     */
    public static StructureMachineLimitDecision evaluate(LhScheduleContext context, StructureSwitchPlan plan,
            SkuScheduleDTO sku, String machineCode, Map<Integer, Integer> actualQuantities) {
        if (!isEnabled(context)) {
            return null;
        }
        if (Objects.isNull(plan)) {
            return null;
        }
        String structure = plan.getSource().getNextStructureName();
        Date anchor = plan.getFirstQuantityShift().getShiftStartDateTime();
        StructureSwitchRuntimeState state = context.getStructureSwitchRuntimeMap().get(structure);
        if (Objects.nonNull(state) && (Objects.nonNull(actualQuantities) || state.getFirstShiftStart().before(anchor))) {
            anchor = state.getFirstShiftStart();
        }
        LhShiftConfigVO secondShift = Objects.nonNull(state) && Objects.equals(anchor, state.getFirstShiftStart())
                ? state.getSecondShift() : nextShift(context, anchor);
        String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        for (LhShiftConfigVO shift : orderedShifts(context)) {
            int offset = SECOND_SHIFT_LIMIT;
            if (Objects.equals(shift.getShiftStartDateTime(), anchor)) {
                offset = 0;
            } else if (Objects.nonNull(secondShift)
                    && Objects.equals(shift.getShiftStartDateTime(), secondShift.getShiftStartDateTime())) {
                offset = FIRST_SHIFT_LIMIT;
            }
            int limit = shiftLimit(plan.getSource().getIsWaitNotify(), offset);
            if (limit == Integer.MAX_VALUE) {
                continue;
            }
            Set<String> machines = new LinkedHashSet<>(8);
            if (Objects.nonNull(context.getStructureShiftInMachineIndex())) {
                machines.addAll(context.getStructureShiftInMachineIndex()
                        .resolveInMachinePhysicalCodes(structure, shift.getShiftIndex()));
            }
            // 正式提交尚未刷新索引时，合并真实正量结果；仍按物理机台去重。
            context.getScheduleResultList().stream().filter(result -> StringUtils.equals(structure, result.getStructureName()))
                    .filter(result -> quantity(result, shift.getShiftIndex()) > 0)
                    .map(result -> LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode()))
                    .forEach(machines::add);
            int scheduledCount = machines.size();
            boolean occupies = Objects.nonNull(actualQuantities)
                    ? actualQuantities.getOrDefault(shift.getShiftIndex(), 0) > 0
                    : occupies(plan, shift);
            if (occupies) {
                machines.add(physical);
            }
            boolean allowed = machines.size() <= limit;
            log.info("结构切换分班准入, sourceId={}, structure={}, machine={}, waitNotify={}, largeInch={}, "
                            + "S0={}, P0={}, targetShift={}, scheduledCount={}, limit={}, allowed={}",
                    plan.getSource().getId(), structure, machineCode,
                    Objects.equals(YES, plan.getSource().getIsWaitNotify()) ? YES : 0,
                    isLargeInch(plan.getSource()) ? YES : 0, anchor,
                    Objects.isNull(plan.getFirstBatchShift()) ? null : plan.getFirstBatchShift().getShiftStartDateTime(),
                    shift.getShiftIndex(), scheduledCount, limit, allowed);
            if (!allowed) {
                String reason = new StringBuilder("结构切换分班机台上限拒绝，结构=").append(structure)
                        .append("，班次=").append(shift.getShiftIndex()).append("，已排=").append(scheduledCount)
                        .append("，合计=").append(machines.size()).append("，上限=").append(limit).toString();
                return new StructureMachineLimitDecision(true, false, DailySchedulePhase.EARLY_PRODUCTION,
                        shift.getWorkDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                        shift.getShiftIndex(), shift.getShiftStartDateTime(), shift.getShiftEndDateTime(),
                        machineCode, physical, Objects.isNull(sku) ? null : sku.getMaterialCode(), structure,
                        null, machines, machines, machines.size() - scheduledCount, limit, reason);
            }
        }
        return null;
    }

    /**
     * 判断预演在受限班次是否上机；纯准备不计数。
     * @param plan 冻结计划
     * @param shift 目标班次
     * @return 是否有首检或批量计划
     */
    private static boolean occupies(StructureSwitchPlan plan, LhShiftConfigVO shift) {
        if (Objects.nonNull(plan.getInspectionPlan()) && plan.getInspectionPlan().getShiftAllocations().stream()
                .anyMatch(item -> item.getQuantity() > 0
                        && Objects.equals(item.getShift().getShiftIndex(), shift.getShiftIndex()))) {
            return true;
        }
        return Objects.nonNull(plan.getProductionStart())
                && plan.getProductionStart().before(shift.getShiftEndDateTime());
    }

    /**
     * 提交完成后重建首班状态，结果被回裁或回滚后不会保留虚假S0。
     * @param context 排程上下文
     */
    public static void rebuildCommittedState(LhScheduleContext context) {
        if (!isEnabled(context)) {
            return;
        }
        Map<String, StructureSwitchRuntimeState> states = new LinkedHashMap<>(context.getStructureSwitchBaselineRuntimeMap());
        List<LhScheduleResult> results = context.getScheduleResultList().stream()
                .filter(result -> context.getStructureSwitchResultPlanMap().containsKey(result))
                .sorted(Comparator.comparing(result -> firstActualTime(context, result)))
                .collect(Collectors.toList());
        for (LhScheduleResult result : results) {
            StructureSwitchPlan plan = context.getStructureSwitchResultPlanMap().get(result);
            if (Objects.isNull(plan)) {
                continue;
            }
            for (LhShiftConfigVO shift : orderedShifts(context)) {
                if (quantity(result, shift.getShiftIndex()) <= 0) {
                    continue;
                }
                String structure = plan.getSource().getNextStructureName();
                StructureSwitchRuntimeState existing = states.get(structure);
                if (Objects.isNull(existing) || shift.getShiftStartDateTime().before(existing.getFirstShiftStart())) {
                    StructureSwitchRuntimeState previous = context.getStructureSwitchRuntimeMap().get(structure);
                    LhShiftConfigVO second = Objects.nonNull(previous)
                            && Objects.equals(previous.getFirstShiftStart(), shift.getShiftStartDateTime())
                            ? previous.getSecondShift() : nextShift(context, shift.getShiftStartDateTime());
                    states.put(structure, new StructureSwitchRuntimeState(plan.getSource().getId(),
                            shift.getShiftStartDateTime(), second, result.getMaterialCode(), result.getProductStatus(), result.getEmbryoCode()));
                }
                break;
            }
        }
        context.setStructureSwitchRuntimeMap(states);
    }

    /**
     * 独立下一班已取得真实相邻班次时，补齐原窗口末班S0的S1身份。
     * @param state 原窗口已提交首班
     * @param endingShift 原窗口末班
     * @param adjacentShift 已由班次服务解析的紧邻下一班
     * @return 原状态或补齐相邻班次的新状态，不修改原窗口
     */
    public static StructureSwitchRuntimeState withAdjacentShift(StructureSwitchRuntimeState state,
            LhShiftConfigVO endingShift, LhShiftConfigVO adjacentShift) {
        if (Objects.nonNull(state.getSecondShift())
                || !Objects.equals(state.getFirstShiftStart(), endingShift.getShiftStartDateTime())) {
            return state;
        }
        return new StructureSwitchRuntimeState(state.getSourceId(), state.getFirstShiftStart(), adjacentShift,
                state.getMaterialCode(), state.getProductStatus(), state.getEmbryoCode());
    }

    /**
     * 获取首个正量的真实起点，用于首班SKU参照的稳定选择。
     * @param context 排程上下文
     * @param result 已提交结果
     * @return 首个生产时间；无正量排在最后
     */
    private static Date firstActualTime(LhScheduleContext context, LhScheduleResult result) {
        for (LhShiftConfigVO shift : orderedShifts(context)) {
            if (quantity(result, shift.getShiftIndex()) > 0) {
                Date start = ShiftFieldUtil.getShiftStartTime(result, shift.getShiftIndex());
                return Objects.nonNull(start) ? start : shift.getShiftStartDateTime();
            }
        }
        return new Date(Long.MAX_VALUE);
    }

    /**
     * 大换英寸保留本候选首检及普通生产所涉及的提交班次，仍只取完整排程窗口内班次。
     * @param context 排程上下文
     * @param availability 冻结计划
     * @param original 原业务日窗口
     * @return 原窗口或扩展到P0的窗口
     */
    public static List<LhShiftConfigVO> resolveCommitShifts(LhScheduleContext context,
            NewSpecMachineAvailabilityPlan availability, List<LhShiftConfigVO> original) {
        if (!isEnabled(context)) {
            return original;
        }
        StructureSwitchPlan plan = Objects.isNull(availability) ? null : availability.getStructureSwitchPlan();
        if (Objects.isNull(plan) || !plan.isLargeTimeline() || Objects.isNull(plan.getFirstBatchShift())) {
            return original;
        }
        Set<Integer> indices = original.stream().map(LhShiftConfigVO::getShiftIndex).collect(Collectors.toSet());
        indices.add(plan.getFirstQuantityShift().getShiftIndex());
        indices.add(plan.getFirstBatchShift().getShiftIndex());
        StructureSwitchRuntimeState state = context.getStructureSwitchRuntimeMap().get(plan.getSource().getNextStructureName());
        if (Objects.isNull(state) || Objects.equals(state.getFirstShiftStart(), plan.getFirstQuantityShift().getShiftStartDateTime())) {
            // 首台同时提交紧邻第二班的连续生产，保证下一业务日扩机时先计入首台占用，不虚增第三台。
            LhShiftConfigVO second = nextShift(context, plan.getFirstQuantityShift().getShiftStartDateTime());
            if (Objects.nonNull(second)) {
                indices.add(second.getShiftIndex());
            }
        }
        plan.getInspectionPlan().getShiftAllocations().stream()
                .map(item -> item.getShift().getShiftIndex()).forEach(indices::add);
        if (Objects.nonNull(availability.getFormalTargetShift())) {
            indices.add(availability.getFormalTargetShift().getShiftIndex());
        }
        return orderedShifts(context).stream().filter(shift -> indices.contains(shift.getShiftIndex()))
                .collect(Collectors.toList());
    }

    /**
     * 实际提交复用大换英寸冻结批量起点。
     * @param context 本批次开关快照
     * @param availability 冻结计划
     * @param original 原计算结果
     * @return 大换英寸冻结起点，普通场景保持原值
     */
    public static Date resolveFormalStart(LhScheduleContext context, NewSpecMachineAvailabilityPlan availability, Date original) {
        StructureSwitchPlan plan = Objects.isNull(availability) ? null : availability.getStructureSwitchPlan();
        return isEnabled(context) && Objects.nonNull(plan) && plan.isLargeTimeline()
                ? availability.getFormalAvailableProductionTime() : original;
    }

    /**
     * 在正式分量作用域获取已冻结计划，不通过动态场景重新识别。
     * @param context 排程上下文
     * @param machineCode 运行侧机台
     * @return 冻结计划，非适用为空
     */
    public static StructureSwitchPlan attemptPlan(LhScheduleContext context, String machineCode) {
        if (!isEnabled(context)) {
            return null;
        }
        return context.getStructureSwitchAttemptPlanMap().get(machineCode);
    }

    /**
     * 正式分量时绑定结果与提案，首班状态仍在提交成功后登记。
     * @param context 排程上下文
     * @param result 本次结果
     * @return 切换计划，非适用为空
     */
    public static StructureSwitchPlan bindResult(LhScheduleContext context, LhScheduleResult result) {
        StructureSwitchPlan plan = attemptPlan(context, result.getLhMachineCode());
        if (Objects.nonNull(plan)) {
            context.getStructureSwitchResultPlanMap().put(result, plan);
        }
        return plan;
    }

    /**
     * 判断当前班次是否为该次切换的特殊P0。
     * @param context 本批次开关快照
     * @param plan 冻结计划
     * @param shift 当前班次
     * @return 是否应用P0数量限制
     */
    public static boolean isFirstBatch(LhScheduleContext context, StructureSwitchPlan plan, LhShiftConfigVO shift) {
        return isEnabled(context) && Objects.nonNull(plan) && isLargeInch(plan.getSource()) && Objects.nonNull(plan.getFirstBatchShift())
                && Objects.equals(plan.getFirstBatchShift().getShiftStartDateTime(), shift.getShiftStartDateTime());
    }

    /**
     * 正式P0不得由班次管控重新提前到晚班起点；P0之后不重复叠加延迟。
     * @param context 本批次开关快照
     * @param plan 冻结提案
     * @param shift 当前班次
     * @param original 原有效起点
     * @return 本班真实起点
     */
    public static Date effectiveStart(LhScheduleContext context, StructureSwitchPlan plan, LhShiftConfigVO shift, Date original) {
        return isFirstBatch(context, plan, shift) ? later(original, plan.getProductionStart()) : original;
    }

    /**
     * P0沿用全部停机与实际模数计算，仅关闭向上补模并按完整班长折算。
     * @param context 排程上下文
     * @param plan 冻结提案
     * @param shift 当前班次
     * @param control 班次管控
     * @param machineCode 机台
     * @param capacity 班产
     * @param cycleSeconds 硫化周期
     * @param mouldQty 模数
     * @param cleaning 清洗窗口
     * @param maintenance 保养窗口
     * @param original 原算法上限
     * @param scheduleType 排程类型
     * @return P0未向上补齐的数量上限，普通班次保持原值
     */
    public static int capCapacity(LhScheduleContext context, StructureSwitchPlan plan, LhShiftConfigVO shift,
            ShiftProductionControlDTO control, String machineCode, int capacity, int cycleSeconds, int mouldQty,
            List<MachineCleaningWindowDTO> cleaning, List<MachineMaintenanceWindowDTO> maintenance,
            int original, String scheduleType) {
        if (!isFirstBatch(context, plan, shift)) {
            return original;
        }
        Date start = effectiveStart(context, plan, shift, control.getEffectiveStartTime());
        int actualCapacity = ShiftCapacityResolverUtil.resolveActualShiftPlanQty(capacity, shift,
                ShiftCapacityResolverUtil.resolveOddShiftCapacityPlusShiftType(context), scheduleType);
        int rawCapacity = ShiftCapacityResolverUtil.resolveShiftCapacityWithDowntime(context.getDevicePlanShutList(),
                cleaning, maintenance, machineCode, start, control.getEffectiveEndTime(), actualCapacity, cycleSeconds,
                mouldQty, ShiftCapacityResolverUtil.resolveShiftDurationSeconds(shift),
                context.getParamIntValue(LhScheduleParamConstant.DRY_ICE_LOSS_QTY, LhScheduleConstant.DRY_ICE_LOSS_QTY),
                context.getParamIntValue(LhScheduleParamConstant.DRY_ICE_DURATION_HOURS, LhScheduleConstant.DRY_ICE_DURATION_HOURS),
                context.getParamIntValue(LhScheduleParamConstant.PLANNED_REPAIR_FIXED_QTY, LhScheduleConstant.PLANNED_REPAIR_FIXED_QTY), true);
        // 管控比例仍沿用原口径，最后一步取偶前不能先按模数向上补量。
        rawCapacity = ShiftProductionControlUtil.deductCapacityByControl(control, rawCapacity, 1);
        return Math.min(original, rawCapacity);
    }

    /**
     * 在实际余量、班次总量和产能限制之后收敛P0数量；单控整机先按物理组取偶再分侧。
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param result 当前结果
     * @param shift 当前班次
     * @param proposed 原分配数量
     * @param capacity 未向上补量的产能
     * @param remaining 当前真实剩余量
     * @return 实际允许数量
     */
    public static int capQuantity(LhScheduleContext context, SkuScheduleDTO sku, LhScheduleResult result,
            LhShiftConfigVO shift, int proposed, int capacity, int remaining) {
        StructureSwitchPlan plan = context.getStructureSwitchResultPlanMap().get(result);
        if (!isFirstBatch(context, plan, shift)) {
            return proposed;
        }
        int sides = LhSingleControlMachineUtil.isConfiguredSingleControlMachine(context, result.getLhMachineCode())
                && LhSingleControlMachineUtil.isWholeMachineGranularitySku(context, sku) ? EVEN_UNIT : 1;
        int quantity = Math.max(0, Math.min(proposed, Math.min(capacity, remaining)));
        NewSpecShiftTotalQtyConstraint constraint = new NewSpecShiftTotalQtyConstraint();
        int limit = constraint.resolveShiftLimit(context);
        if (limit > 0) {
            int existing = constraint.resolveCurrentShiftQty(context, shift.getShiftIndex());
            quantity = Math.min(quantity, Math.max(0, limit - existing) / sides);
        }
        // 首检已经写入结果，按“首检+普通生产”总量取偶后再扣回已写量，单控两侧按物理整机收敛。
        int existingQty = quantity(result, shift.getShiftIndex());
        int finalQty = Math.max(0, floorEven(BigDecimalUtils.valueOf((long) (quantity + existingQty) * sides))
                / sides - existingQty);
        StructureSwitchRuntimeState state = context.getStructureSwitchRuntimeMap().get(result.getStructureName());
        Date firstShiftStart = Objects.isNull(state) ? plan.getFirstQuantityShift().getShiftStartDateTime() : state.getFirstShiftStart();
        log.info("大换英寸P0分量, structure={}, sku={}, machine={}, waitNotify={}, largeInch=1, S0={}, P0={}, "
                        + "targetShift={}, baseStart={}, productionStart={}, capacity={}, remaining={}, finalQty={}",
                result.getStructureName(), result.getMaterialCode(), result.getLhMachineCode(),
                Objects.equals(YES, plan.getSource().getIsWaitNotify()) ? YES : 0,
                firstShiftStart, plan.getFirstBatchShift().getShiftStartDateTime(),
                shift.getShiftIndex(), plan.getBaseStart(), plan.getProductionStart(), capacity, remaining, finalQty);
        return finalQty;
    }

    /**
     * 验证实际结果覆盖的每个受限班次，再统一登记S0；准备或终局未排不产生首班。
     * @param context 排程上下文
     * @return 失败原因；通过为空
     */
    public static String validateCommittedResults(LhScheduleContext context) {
        return validateCommittedResults(context, 0);
    }

    /**
     * 提交阶段按结构去重复核，仅为本次新结果打印提交明细，避免重复打印历史结果。
     * @param context 排程上下文
     * @param firstNewResultIndex 本次提交前的结果数量
     * @return 拒绝原因；通过为空
     */
    public static String validateCommittedResults(LhScheduleContext context, int firstNewResultIndex) {
        if (!isEnabled(context)) {
            return null;
        }
        // 日计划回裁可能移除原首班，先依据实际正量重新定位，再检查新的S0/S1。
        rebuildCommittedState(context);
        Set<String> validatedStructures = new LinkedHashSet<>(16);
        int resultIndex = 0;
        for (LhScheduleResult result : context.getScheduleResultList()) {
            boolean newResult = resultIndex++ >= firstNewResultIndex;
            StructureSwitchPlan plan = context.getStructureSwitchResultPlanMap().get(result);
            if (Objects.isNull(plan)) {
                continue;
            }
            Map<Integer, Integer> quantities = new LinkedHashMap<>(9);
            for (LhShiftConfigVO shift : orderedShifts(context)) {
                quantities.put(shift.getShiftIndex(), quantity(result, shift.getShiftIndex()));
            }
            if (quantities.values().stream().noneMatch(value -> value > 0)) {
                // 纯准备或已回裁为零的结果不产生切换首班，也不占用新的结构名额。
                continue;
            }
            String timelineFailure = validateLargeTimelineResult(context, plan, result);
            if (StringUtils.isNotEmpty(timelineFailure)) {
                log.info("结构切换结果时间轴拒绝, sku={}, machine={}, reason={}",
                        result.getMaterialCode(), result.getLhMachineCode(), timelineFailure);
                return timelineFailure;
            }
            String failure = validatedStructures.add(plan.getSource().getNextStructureName())
                    ? validate(context, plan, result.getLhMachineCode(), quantities) : null;
            if (StringUtils.isNotEmpty(failure)) {
                return failure;
            }
            if (!newResult) {
                continue;
            }
            StructureSwitchRuntimeState state = context.getStructureSwitchRuntimeMap().get(result.getStructureName());
            log.info("结构切换实际提交, structure={}, sku={}, machine={}, waitNotify={}, largeInch={}, "
                            + "S0={}, P0={}, baseStart={}, productionStart={}, finalShiftQuantities={}",
                    result.getStructureName(), result.getMaterialCode(), result.getLhMachineCode(),
                    Objects.equals(YES, plan.getSource().getIsWaitNotify()) ? YES : 0,
                    isLargeInch(plan.getSource()) ? YES : 0, Objects.isNull(state) ? null : state.getFirstShiftStart(),
                    Objects.isNull(plan.getFirstBatchShift()) ? null : plan.getFirstBatchShift().getShiftStartDateTime(),
                    plan.getBaseStart(), plan.getProductionStart(), quantities);
        }
        return null;
    }

    /**
     * 校验首检和普通生产合并后的结果没有提前到冻结的公式及结构名额起点之前。
     * @param context 排程上下文
     * @param plan 冻结计划
     * @param result 实际结果
     * @return 不一致原因；通过为空
     */
    private static String validateLargeTimelineResult(LhScheduleContext context, StructureSwitchPlan plan,
            LhScheduleResult result) {
        if (!plan.isLargeTimeline()) {
            return null;
        }
        FirstInspectionAllocationPlan inspection = plan.getInspectionPlan();
        Map<Integer, Integer> inspectionQuantities = FirstInspectionAllocationUtil.toShiftQtyMap(inspection);
        for (LhShiftConfigVO shift : orderedShifts(context)) {
            int plannedQty = quantity(result, shift.getShiftIndex());
            if (plannedQty <= 0) {
                continue;
            }
            // 首检跨班时，普通生产尚未开始的前一班只能保留真实首检分摊，不能提前补普通量。
            if (!plan.getProductionStart().before(shift.getShiftEndDateTime())
                    && plannedQty > inspectionQuantities.getOrDefault(shift.getShiftIndex(), 0)) {
                return "结构切换普通生产起点之前只允许冻结的首检数量";
            }
            Date actualStart = ShiftFieldUtil.getShiftStartTime(result, shift.getShiftIndex());
            Date allowedStart = later(shift.getShiftStartDateTime(), inspection.getInspectionStartTime());
            if (Objects.isNull(actualStart) || actualStart.before(allowedStart)) {
                return "结构切换实际起点早于含首检公式或结构名额允许时间";
            }
        }
        return null;
    }

    /**
     * 等结果S1的局部扩机优先级；非适用返回同等级，不改变原排序。
     * @param context 排程上下文
     * @param sku 当前候选
     * @param plan 冻结提案
     * @return 同SKU为0、同胎胚为1、同结构为2；不适用为-1
     */
    public static int expansionRank(LhScheduleContext context, SkuScheduleDTO sku, StructureSwitchPlan plan) {
        if (!isEnabled(context) || Objects.isNull(plan) || !Objects.equals(YES, plan.getSource().getIsWaitNotify())) {
            return -1;
        }
        StructureSwitchRuntimeState state = context.getStructureSwitchRuntimeMap().get(sku.getStructureName());
        if (Objects.isNull(state)) {
            return -1;
        }
        LhShiftConfigVO second = state.getSecondShift();
        if (Objects.isNull(second) || !Objects.equals(second.getShiftStartDateTime(), plan.getFirstQuantityShift().getShiftStartDateTime())) {
            return -1;
        }
        if (StringUtils.equals(state.getMaterialCode(), sku.getMaterialCode())
                && StringUtils.equals(state.getProductStatus(), sku.getProductStatus())) {
            return 0;
        }
        return StringUtils.equals(state.getEmbryoCode(), sku.getEmbryoCode()) ? 1 : 2;
    }

    /**
     * 收集同一切换S1的最优合格提案，业务等级优先，同等级复用调用方原排序。
     * @param context 排程上下文
     * @param proposal 已通过硬约束的提案
     * @param selected 当前机台的切换组
     * @param originalComparator 原有同层级比较器
     * @return 是否被切换组接管
     */
    public static boolean collectExpansionProposal(LhScheduleContext context, NewSpecScheduleProposal proposal,
            Map<String, NewSpecScheduleProposal> selected, Comparator<NewSpecScheduleProposal> originalComparator) {
        StructureSwitchPlan plan = proposal.getAvailabilityPlan().getStructureSwitchPlan();
        int rank = expansionRank(context, proposal.getCandidate().getSku(), plan);
        if (rank < 0) {
            return false;
        }
        String structure = plan.getSource().getNextStructureName();
        NewSpecScheduleProposal previous = selected.get(structure);
        int previousRank = Objects.isNull(previous) ? Integer.MAX_VALUE
                : expansionRank(context, previous.getCandidate().getSku(), previous.getAvailabilityPlan().getStructureSwitchPlan());
        if (rank < previousRank || (rank == previousRank && originalComparator.compare(proposal, previous) < 0)) {
            selected.put(structure, proposal);
        }
        return true;
    }

    /**
     * 多机台提案同时竞争时，同一次切换只保留最高业务等级，不改变其它结构提案顺序。
     * @param context 排程上下文
     * @param proposals 已通过硬约束的提案
     * @return 局部分级后的原顺序列表
     */
    public static List<NewSpecScheduleProposal> retainExpansionPriority(LhScheduleContext context,
            List<NewSpecScheduleProposal> proposals) {
        if (!isEnabled(context)) {
            return proposals;
        }
        Map<String, Integer> bestRanks = new LinkedHashMap<>(4);
        for (NewSpecScheduleProposal proposal : proposals) {
            int rank = expansionRank(context, proposal.getCandidate().getSku(), proposal.getAvailabilityPlan().getStructureSwitchPlan());
            if (rank >= 0) {
                bestRanks.merge(proposal.getCandidate().getSku().getStructureName(), rank, Math::min);
            }
        }
        return proposals.stream().filter(proposal -> {
            int rank = expansionRank(context, proposal.getCandidate().getSku(), proposal.getAvailabilityPlan().getStructureSwitchPlan());
            return rank < 0 || rank == bestRanks.get(proposal.getCandidate().getSku().getStructureName());
        }).collect(Collectors.toList());
    }

    /**
     * 实际余量回裁时P0再次向下取偶，防止尾量被通用模数规则补回。
     * @param context 排程上下文
     * @param result 正常结果或整机合计结果
     * @param shift 当前班次
     * @param original 原回裁数量
     * @param remaining 当前真实未消费余量
     * @return P0按物理整机取偶后的保留量，单控每侧允许奇数，其他班次保持原口径
     */
    public static int capRetainedQuantity(LhScheduleContext context, LhScheduleResult result,
            LhShiftConfigVO shift, int original, int remaining) {
        if (!isFirstBatch(context, context.getStructureSwitchResultPlanMap().get(result), shift)) {
            return original;
        }
        int sides = LhSingleControlMachineUtil.isConfiguredSingleControlMachine(context, result.getLhMachineCode())
                ? EVEN_UNIT : 1;
        return floorEven(BigDecimalUtils.valueOf((long) Math.min(original, Math.max(0, remaining)) * sides)) / sides;
    }

    /**
     * 批量数量在全部硬约束之后向下取偶。
     * @param availableQty 未向上补齐的可排量
     * @return 非负偶数计划量
     */
    public static int floorEven(BigDecimal availableQty) {
        return availableQty.max(BigDecimal.ZERO).divide(BigDecimalUtils.valueOf(EVEN_UNIT), 0, RoundingMode.DOWN)
                .multiply(BigDecimalUtils.valueOf(EVEN_UNIT)).intValue();
    }

    /**
     * 读取班次非负正计划量。
     * @param result 排程结果
     * @param shiftIndex 班次索引
     * @return 非负数量
     */
    public static int quantity(LhScheduleResult result, int shiftIndex) {
        Integer qty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
        return Objects.isNull(qty) ? 0 : Math.max(0, qty);
    }

    /**
     * 取两个已有时间的较晚值。
     * @param first 时间一
     * @param second 时间二
     * @return 较晚时间
     */
    public static Date later(Date first, Date second) {
        if (Objects.isNull(first)) {
            return second;
        }
        return Objects.isNull(second) || first.after(second) ? first : second;
    }
}
