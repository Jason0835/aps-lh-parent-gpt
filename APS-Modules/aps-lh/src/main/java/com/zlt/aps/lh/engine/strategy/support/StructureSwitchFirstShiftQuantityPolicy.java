package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.cx.entity.config.CxEmbryoLhTime;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 结构切换供胚首班共享数量限制。配置只读，已用量直接取实际结果，试排回滚无需维护第二套账本。
 * 与每台机的大换英寸P0分开：只限制供胚时间所在班次，首检和普通生产共同消费额度。
 */
@Slf4j
public final class StructureSwitchFirstShiftQuantityPolicy {
    /** 复用既有单模、单控和胎胚收尾数量单位解析；这里只调用无资源副作用的方法。 */
    private static final TargetScheduleQtyResolver QUANTITY_POLICY = new TargetScheduleQtyResolver();
    /** 单控整机候选按两侧计量，提交后的独立结果行直接汇总。 */
    private static final int WHOLE_MACHINE_SIDES = 2;

    private StructureSwitchFirstShiftQuantityPolicy() {
    }

    /**
     * 读取本次切换在指定班次的上限，未配置或非供胚首班返回空。
     * @param context 本批配置
     * @param source 同一有效结构切换来源
     * @param embryoCode 胎胚编码
     * @param shift 真实班次
     * @return 含首检上限，零表示不能排产
     */
    private static Integer resolveLimit(LhScheduleContext context, CxEmbryoLhTime source,
            String embryoCode, LhShiftConfigVO shift) {
        if (!StructureSwitchSchedulingPolicy.isEnabled(context)
                || Objects.isNull(source) || Objects.isNull(source.getEarliestLhTime())
                || Objects.isNull(shift) || StringUtils.isEmpty(embryoCode)
                || source.getEarliestLhTime().before(shift.getShiftStartDateTime())
                || !source.getEarliestLhTime().before(shift.getShiftEndDateTime())) {
            return null;
        }
        return context.getStructureTreadCountMap()
                .getOrDefault(source.getNextStructureName(), Collections.emptyMap()).get(embryoCode);
    }

    /**
     * 判断供胚所在首班，数量时间上限不依赖整车胎面限额是否配置。
     *
     * @param context 本批开关
     * @param source 有效切换来源
     * @param shift 当前班次
     * @return 是否属于结构切换供胚首班
     */
    public static boolean isSupplyFirstShift(LhScheduleContext context, CxEmbryoLhTime source,
            LhShiftConfigVO shift) {
        return StructureSwitchSchedulingPolicy.isEnabled(context) && Objects.nonNull(source)
                && Objects.nonNull(source.getEarliestLhTime()) && Objects.nonNull(shift)
                && !source.getEarliestLhTime().before(shift.getShiftStartDateTime())
                && source.getEarliestLhTime().before(shift.getShiftEndDateTime());
    }

    /**
     * 读取冻结来源，后续回裁不重新推导场景；无冻结计划的普通结果不扩大适用范围。
     * 普通续作仅参与结构机台与已用量统计，不因来源识别扩展而回裁既有数量。
     *
     * @param context 当前上下文
     * @param result 当前结果
     * @return 已确认的切换来源，无切换身份为空
     */
    private static CxEmbryoLhTime resolveSource(LhScheduleContext context, LhScheduleResult result) {
        StructureSwitchPlan plan = context.getStructureSwitchResultPlanMap().get(result);
        if (Objects.nonNull(plan)) {
            return plan.getSource();
        }
        if (isOrdinaryContinuation(context, result)) {
            return null;
        }
        SkuScheduleDTO sku = context.getScheduleResultSourceSkuMap().get(result);
        return Objects.isNull(sku) ? null : StructureSwitchSchedulingPolicy.resolveSource(context, sku);
    }

    /**
     * 判断未绑定切换计划的普通续作，保留其原生产数量处理。
     *
     * @param context 当前切换计划绑定关系
     * @param result 待判断的结果
     * @return 未绑定切换计划且不是换活字块的续作结果时为 true
     */
    private static boolean isOrdinaryContinuation(LhScheduleContext context, LhScheduleResult result) {
        return !context.getStructureSwitchResultPlanMap().containsKey(result)
                && StringUtils.equals(ScheduleTypeEnum.CONTINUOUS.getCode(), result.getScheduleType())
                && !StringUtils.equals("1", result.getIsTypeBlock());
    }

    /**
     * 汇总同结构同胎胚的本班结果，排除当前对象以防当前首检或回裁原量重复计算。
     * @param context 已提交结果及独立班次历史快照
     * @param structure 结构名称
     * @param embryo 胎胚编码
     * @param shift 受限班次
     * @param excluded 当前对象，预演时为空
     * @return 其他结果已用量，包含首检且不区分SKU和产品状态
     */
    private static long usedQuantity(LhScheduleContext context, String structure, String embryo,
            LhShiftConfigVO shift, LhScheduleResult excluded) {
        long baseline = context.getStructureTreadBaselineQtyMap()
                .getOrDefault(structure, Collections.emptyMap()).getOrDefault(embryo, 0);
        return baseline + context.getScheduleResultList().stream()
                .filter(result -> result != excluded)
                .filter(result -> StringUtils.equals(structure, result.getStructureName())
                        && StringUtils.equals(embryo, result.getEmbryoCode()))
                .mapToLong(result -> StructureSwitchSchedulingPolicy.quantity(result, shift.getShiftIndex())).sum();
    }

    /**
     * 首检预演检查完整首检能否放入共享额度，不能截断首检凑额度。
     * @param context 当前批次
     * @param sku 候选SKU
     * @param machineCode 候选机台
     * @param plan 完整首检计划
     * @return 原有效计划或带明确原因的无效计划
     */
    public static FirstInspectionAllocationPlan validateInspection(LhScheduleContext context,
            SkuScheduleDTO sku, String machineCode, FirstInspectionAllocationPlan plan) {
        if (Objects.isNull(context) || context.getStructureTreadCountMap().isEmpty()) {
            return plan;
        }
        CxEmbryoLhTime source = StructureSwitchSchedulingPolicy.resolveSource(context, sku);
        if (Objects.isNull(source) || !plan.isValid()) {
            return plan;
        }
        int sides = resolvePreviewSides(context, sku, machineCode);
        for (FirstInspectionShiftAllocation allocation : plan.getQuantityAllocations()) {
            Integer limit = resolveLimit(context, source, sku.getEmbryoCode(), allocation.getShift());
            if (Objects.nonNull(limit) && usedQuantity(context, sku.getStructureName(), sku.getEmbryoCode(),
                    allocation.getShift(), null) + (long) allocation.getQuantity() * sides > limit) {
                log.warn("结构胎胚首班额度不足完整首检, batchNo={}, sku={}, machine={}, shift={}, limit={}",
                        context.getBatchNo(), sku.getMaterialCode(), machineCode, allocation.getShift().getShiftIndex(), limit);
                return FirstInspectionAllocationPlan.invalid("结构胎胚首班剩余额度不足完整首检",
                        plan.getCountingShift(), plan.getInspectionEndTime());
            }
        }
        return plan;
    }

    /**
     * 冻结首检写入前复核额度，防止预演之后其他提案已消费同组额度。
     *
     * @param context 本批上下文
     * @param result 当前候选结果
     * @param plan 冻结首检计划
     * @return 全部首检分摊都能容纳时为true
     */
    public static boolean canWriteInspection(LhScheduleContext context, LhScheduleResult result,
            FirstInspectionAllocationPlan plan) {
        CxEmbryoLhTime source = resolveSource(context, result);
        // 首检早于结果来源登记，读取冻结模式时仍须保留独立历史指定的精确侧别范围。
        int sides = LhSingleControlMachineUtil.isConfiguredSingleControlMachine(context, result.getLhMachineCode())
                && LhSingleControlMachineUtil.isWholeMachineGranularityResult(context, result)
                ? WHOLE_MACHINE_SIDES : 1;
        for (FirstInspectionShiftAllocation allocation : plan.getQuantityAllocations()) {
            LhShiftConfigVO shift = allocation.getShift();
            Integer limit = resolveLimit(context, source, result.getEmbryoCode(), shift);
            long current = StructureSwitchSchedulingPolicy.quantity(result, shift.getShiftIndex());
            if (Objects.nonNull(limit) && usedQuantity(context, result.getStructureName(), result.getEmbryoCode(),
                    shift, result) + (current + allocation.getQuantity()) * sides > limit) {
                return false;
            }
        }
        return true;
    }

    /**
     * 限制本次普通生产增量，当前结果已写首检只参与一次累计与最终模数归整。
     * 未绑定切换计划的普通续作保持原增量，不因同结构新候选进入切换而追加数量限制。
     *
     * @param context 当前上下文
     * @param sku 当前SKU
     * @param result 当前候选结果
     * @param shift 当前班次
     * @param proposed 原普通生产增量
     * @return 可追加量
     */
    public static int capIncrement(LhScheduleContext context, SkuScheduleDTO sku, LhScheduleResult result,
            LhShiftConfigVO shift, int proposed) {
        if (isOrdinaryContinuation(context, result)) {
            return Math.max(0, proposed);
        }
        CxEmbryoLhTime source = resolveSource(context, result);
        if (Objects.isNull(source)) {
            source = StructureSwitchSchedulingPolicy.resolveSource(context, sku);
        }
        int existing = StructureSwitchSchedulingPolicy.quantity(result, shift.getShiftIndex());
        int sides = resolvePreviewSides(context, sku, result.getLhMachineCode());
        int total = capTotal(context, source, result, shift, existing + Math.max(0, proposed), sides, true);
        return Math.max(0, total - existing);
    }

    /**
     * 判断当前结果是否受到供胚首班时间上限，供日计划回裁与数量归整共用。
     *
     * @param context 当前批次
     * @param result 当前结果
     * @param shift 当前班次
     * @return 是否需要执行首班时间上限及含首检归整
     */
    public static boolean isLimited(LhScheduleContext context, LhScheduleResult result, LhShiftConfigVO shift) {
        CxEmbryoLhTime source = resolveSource(context, result);
        return Objects.nonNull(resolveLimit(context, source, result.getEmbryoCode(), shift))
                || (isSupplyFirstShift(context, source, shift) && !StructureSwitchSchedulingPolicy.isLargeInch(source));
    }

    /**
     * 限制回裁后的含首检总量；不会把当前结果的原量当作其他机台占额。
     * @param context 当前上下文
     * @param result 当前结果
     * @param shift 当前班次
     * @param proposedTotal 候选含首检总量
     * @return 合法总量
     */
    public static int capRetainedTotal(LhScheduleContext context, LhScheduleResult result,
            LhShiftConfigVO shift, int proposedTotal) {
        return capTotal(context, resolveSource(context, result), result, shift, proposedTotal, 1, true);
    }

    /**
     * 在跨日临时副本清空原班次后，冻结原结果已消费后的供胚首班剩余时间产能。
     * 配对侧共用同次分班时取两侧较小剩余额度，禁止临时结果再次取得完整残班产能。
     *
     * @param context 当前排程上下文及已提交结果
     * @param sourceResult 原已提交结果
     * @param pairedSourceResult 同次分班的配对侧原结果，普通机台为空
     * @param deltaResult 只承载本次增量的临时结果
     * @param shifts 完整窗口班次
     */
    public static void inheritRemainingCapacity(LhScheduleContext context, LhScheduleResult sourceResult,
            LhScheduleResult pairedSourceResult, LhScheduleResult deltaResult, List<LhShiftConfigVO> shifts) {
        deltaResult.setCarryOverSupplyFirstShiftStartTime(null);
        deltaResult.setCarryOverSupplyFirstShiftQtyLimit(null);
        CxEmbryoLhTime source = resolveSource(context, sourceResult);
        for (LhShiftConfigVO shift : shifts) {
            if (!isSupplyFirstShift(context, source, shift)) {
                continue;
            }
            int remaining = resolveRemainingCapacity(context, sourceResult, shift);
            if (Objects.nonNull(pairedSourceResult)) {
                remaining = Math.min(remaining, resolveRemainingCapacity(context, pairedSourceResult, shift));
            }
            deltaResult.setCarryOverSupplyFirstShiftStartTime(shift.getShiftStartDateTime());
            deltaResult.setCarryOverSupplyFirstShiftQtyLimit(remaining);
            return;
        }
    }

    /**
     * 以原结果的首检和真实起点计算整班合法总量，再扣除原结果已经落地的数量。
     *
     * @param context 当前共享额度及数量规则
     * @param sourceResult 原已提交结果
     * @param shift 供胚首班
     * @return 本侧允许追加的非负数量
     */
    private static int resolveRemainingCapacity(LhScheduleContext context, LhScheduleResult sourceResult,
            LhShiftConfigVO shift) {
        int legalTotal = capTotal(context, resolveSource(context, sourceResult), sourceResult,
                shift, Integer.MAX_VALUE, 1, false);
        return Math.max(0, legalTotal - StructureSwitchSchedulingPolicy.quantity(sourceResult, shift.getShiftIndex()));
    }

    /**
     * 将临时副本继承的单侧供胚首班额度换算为L/R整机合计，绝对班次坐标保持不变。
     *
     * @param groupResult 已复制单侧字段的整机临时结果
     */
    public static void scaleCarryOverLimitToWholeMachine(LhScheduleResult groupResult) {
        Integer remaining = groupResult.getCarryOverSupplyFirstShiftQtyLimit();
        if (Objects.nonNull(remaining)) {
            groupResult.setCarryOverSupplyFirstShiftQtyLimit(
                    (int) Math.min(Integer.MAX_VALUE, (long) remaining * WHOLE_MACHINE_SIDES));
        }
    }

    /**
     * 判断当前班次是否承接了原结果已经消耗后的供胚首班增量上限。
     *
     * @param result 当前临时结果
     * @param shift 当前真实班次
     * @return 存在同一绝对班次的继承额度时返回true
     */
    public static boolean isCarryOverLimited(LhScheduleResult result, LhShiftConfigVO shift) {
        return Objects.nonNull(result) && Objects.nonNull(shift)
                && Objects.nonNull(result.getCarryOverSupplyFirstShiftQtyLimit())
                && Objects.nonNull(result.getCarryOverSupplyFirstShiftStartTime())
                && Objects.equals(result.getCarryOverSupplyFirstShiftStartTime(), shift.getShiftStartDateTime());
    }

    /**
     * 先叠加共享额度及本机实际时间上限，再按含首检总量向下归整，保留原数量单位例外。
     *
     * @param context 当前上下文
     * @param source 冻结来源
     * @param result 当前结果
     * @param shift 受限班次
     * @param proposedTotal 当前候选含首检总量
     * @param sides 预演整机倍率，提交后独立行固定为1
     * @param logAdjustment 是否输出本次实际数量收口日志，剩余产能预计算时关闭
     * @return 当前行或预演单侧可排总量
     */
    private static int capTotal(LhScheduleContext context, CxEmbryoLhTime source, LhScheduleResult result,
            LhShiftConfigVO shift, int proposedTotal, int sides, boolean logAdjustment) {
        Integer limit = resolveLimit(context, source, result.getEmbryoCode(), shift);
        if (!isSupplyFirstShift(context, source, shift)) {
            return proposedTotal;
        }
        long used = usedQuantity(context, result.getStructureName(), result.getEmbryoCode(), shift, result);
        int available = Objects.isNull(limit) ? Integer.MAX_VALUE : (int) Math.max(0L, limit - used) / sides;
        int mouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(
                Objects.isNull(result.getMouldQty()) ? 0 : result.getMouldQty());
        int multiple = QUANTITY_POLICY.resolveAllocationMultiple(context, result, mouldQty);
        int total = Math.min(Math.max(0, proposedTotal), available);
        if (isCarryOverLimited(result, shift)) {
            // 临时副本只代表增量，原班次已用产能不能因副本数量清零而重新分配。
            total = Math.min(total, result.getCarryOverSupplyFirstShiftQtyLimit());
        }
        // 含首检量必须受真实时间上限约束，不能对已经向上补成偶数的数量再次取偶了事。
        Date start = StructureSwitchSchedulingPolicy.later(source.getEarliestLhTime(),
                ShiftFieldUtil.getShiftStartTime(result, shift.getShiftIndex()));
        StructureSwitchPlan plan = context.getStructureSwitchResultPlanMap().get(result);
        if (Objects.nonNull(plan) && Objects.nonNull(plan.getInspectionPlan())) {
            for (FirstInspectionShiftAllocation allocation : plan.getInspectionPlan().getShiftAllocations()) {
                if (Objects.equals(allocation.getShift().getShiftIndex(), shift.getShiftIndex())) {
                    Date inspectionStart = StructureSwitchSchedulingPolicy.later(
                            source.getEarliestLhTime(), allocation.getOverlapStartTime());
                    if (inspectionStart.before(start)) {
                        start = inspectionStart;
                    }
                }
            }
        }
        int capacity = Objects.isNull(result.getSingleMouldShiftQty()) ? 0 : result.getSingleMouldShiftQty();
        if (capacity > 0) {
            long seconds = Math.max(0L, (shift.getShiftEndDateTime().getTime() - start.getTime()) / 1000L);
            total = Math.min(total, ShiftCapacityResolverUtil.resolveShiftCapacity(capacity, 0,
                    mouldQty, ShiftCapacityResolverUtil.resolveShiftDurationSeconds(shift), seconds, true));
        }
        total -= total % multiple;
        if (logAdjustment && total != proposedTotal && log.isInfoEnabled()) {
            log.info("结构胎胚首班限量, factoryCode={}, batchNo={}, sourceId={}, structure={}, embryo={}, "
                            + "sku={}, status={}, machine={}, shift={}, supplyTime={}, limit={}, used={}, proposed={}, finalQty={}",
                    context.getFactoryCode(), context.getBatchNo(), source.getId(), result.getStructureName(),
                    result.getEmbryoCode(), result.getMaterialCode(), result.getProductStatus(), result.getLhMachineCode(),
                    shift.getShiftIndex(), source.getEarliestLhTime(), limit, used, proposedTotal, total);
        }
        return total;
    }

    /**
     * 提交及保存前复核共享额度、真实产能和数量单位；候选提交负责原子回滚，保存前仅告警。
     *
     * @param context 当前上下文
     * @return 失败原因，通过为空
     */
    public static String validateResults(LhScheduleContext context) {
        for (LhScheduleResult result : context.getScheduleResultList()) {
            CxEmbryoLhTime source = resolveSource(context, result);
            for (LhShiftConfigVO shift : context.getScheduleWindowShifts()) {
                Integer limit = resolveLimit(context, source, result.getEmbryoCode(), shift);
                if (!isSupplyFirstShift(context, source, shift)) {
                    continue;
                }
                long total = usedQuantity(context, result.getStructureName(), result.getEmbryoCode(), shift, null);
                int plannedQty = StructureSwitchSchedulingPolicy.quantity(result, shift.getShiftIndex());
                if ((Objects.nonNull(limit) && total > limit)
                        || capRetainedTotal(context, result, shift, plannedQty) != plannedQty) {
                    return MessageFormat.format("结构{0}胎胚{1}班次{2}的含首检数量不合法，上限{3}，累计{4}",
                            result.getStructureName(), result.getEmbryoCode(), shift.getShiftIndex(), limit, total);
                }
            }
        }
        return null;
    }

    /**
     * 将原窗口已消费额度按供胚班次冻结给班次9副本，避免副本清空结果后重复获得首班额度。
     * @param sourceContext 原窗口上下文
     * @return 结构与胎胚的已用量只读快照
     */
    public static Map<String, Map<String, Integer>> snapshotUsedQuantities(LhScheduleContext sourceContext) {
        Map<String, Map<String, Integer>> quantities = new LinkedHashMap<>(16);
        sourceContext.getStructureSwitchSourceMap().forEach((structure, source) -> {
            for (LhShiftConfigVO shift : sourceContext.getScheduleWindowShifts()) {
                Map<String, Integer> embryos = new LinkedHashMap<>(4);
                sourceContext.getStructureTreadCountMap().getOrDefault(structure, Collections.emptyMap())
                        .forEach((embryo, limit) -> {
                            if (Objects.nonNull(resolveLimit(sourceContext, source, embryo, shift))) {
                                embryos.put(embryo, Math.toIntExact(usedQuantity(sourceContext, structure, embryo, shift, null)));
                            }
                        });
                if (!embryos.isEmpty()) {
                    quantities.put(structure, Collections.unmodifiableMap(embryos));
                }
            }
        });
        return Collections.unmodifiableMap(quantities);
    }

    /**
     * 复用整机SKU与单控配置识别，预演单侧数量需折成物理整机量。
     * @param context 当前配置
     * @param sku 候选SKU
     * @param machineCode 机台编码
     * @return 普通候选1，单控整机候选2
     */
    private static int resolvePreviewSides(LhScheduleContext context, SkuScheduleDTO sku, String machineCode) {
        return Objects.nonNull(sku) && LhSingleControlMachineUtil.isConfiguredSingleControlMachine(context, machineCode)
                && LhSingleControlMachineUtil.isWholeMachineGranularitySku(context, sku) ? WHOLE_MACHINE_SIDES : 1;
    }
}
