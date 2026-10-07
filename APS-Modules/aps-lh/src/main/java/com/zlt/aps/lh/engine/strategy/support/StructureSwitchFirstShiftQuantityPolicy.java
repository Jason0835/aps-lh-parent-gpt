package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.cx.entity.config.CxEmbryoLhTime;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.api.enums.SingleControlMachineModeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
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
     * 读取冻结来源，后续回裁不重新推导场景；无冻结计划的普通结果不扩大适用范围。
     * @param context 当前上下文
     * @param result 当前结果
     * @return 已确认的切换来源，无切换身份为空
     */
    private static CxEmbryoLhTime resolveSource(LhScheduleContext context, LhScheduleResult result) {
        StructureSwitchPlan plan = context.getStructureSwitchResultPlanMap().get(result);
        if (Objects.nonNull(plan)) {
            return plan.getSource();
        }
        SkuScheduleDTO sku = context.getScheduleResultSourceSkuMap().get(result);
        return Objects.isNull(sku) ? null : StructureSwitchSchedulingPolicy.resolveSource(context, sku);
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
        for (FirstInspectionShiftAllocation allocation : plan.getShiftAllocations()) {
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
     * @param context 本批上下文
     * @param result 当前候选结果
     * @param plan 冻结首检计划
     * @return 全部首检分摊都能容纳时为true
     */
    public static boolean canWriteInspection(LhScheduleContext context, LhScheduleResult result,
            FirstInspectionAllocationPlan plan) {
        CxEmbryoLhTime source = resolveSource(context, result);
        // 首检写入早于结果来源SKU登记，直接读取S4.3冻结的同一物料状态模式。
        String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(result.getMaterialCode(), result.getProductStatus());
        int sides = LhSingleControlMachineUtil.isConfiguredSingleControlMachine(context, result.getLhMachineCode())
                && SingleControlMachineModeEnum.WHOLE_PAIR == context.getSingleControlModeSnapshotMap().get(skuKey)
                ? WHOLE_MACHINE_SIDES : 1;
        for (FirstInspectionShiftAllocation allocation : plan.getShiftAllocations()) {
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
     * @param context 当前上下文
     * @param sku 当前SKU
     * @param result 当前候选结果
     * @param shift 当前班次
     * @param proposed 原普通生产增量
     * @return 可追加量
     */
    public static int capIncrement(LhScheduleContext context, SkuScheduleDTO sku, LhScheduleResult result,
            LhShiftConfigVO shift, int proposed) {
        CxEmbryoLhTime source = resolveSource(context, result);
        if (Objects.isNull(source)) {
            source = StructureSwitchSchedulingPolicy.resolveSource(context, sku);
        }
        int existing = StructureSwitchSchedulingPolicy.quantity(result, shift.getShiftIndex());
        int sides = resolvePreviewSides(context, sku, result.getLhMachineCode());
        int total = capTotal(context, source, result, shift, existing + Math.max(0, proposed), sides);
        return Math.max(0, total - existing);
    }

    /**
     * 判断当前结果是否受到供胚首班限量，供日计划回裁与数量归整共用。
     * @param context 当前批次
     * @param result 当前结果
     * @param shift 当前班次
     * @return 是否存在生效的首班额度
     */
    public static boolean isLimited(LhScheduleContext context, LhScheduleResult result, LhShiftConfigVO shift) {
        return Objects.nonNull(resolveLimit(context, resolveSource(context, result), result.getEmbryoCode(), shift));
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
        return capTotal(context, resolveSource(context, result), result, shift, proposedTotal, 1);
    }

    /**
     * 先叠加其他机台共享额度，再按最终含首检量向下归整，保留原数量单位例外。
     * @param context 当前上下文
     * @param source 冻结来源
     * @param result 当前结果
     * @param shift 受限班次
     * @param proposedTotal 当前候选含首检总量
     * @param sides 预演整机倍率，提交后独立行固定为1
     * @return 当前行或预演单侧可排总量
     */
    private static int capTotal(LhScheduleContext context, CxEmbryoLhTime source, LhScheduleResult result,
            LhShiftConfigVO shift, int proposedTotal, int sides) {
        Integer limit = resolveLimit(context, source, result.getEmbryoCode(), shift);
        if (Objects.isNull(limit)) {
            return proposedTotal;
        }
        long used = usedQuantity(context, result.getStructureName(), result.getEmbryoCode(), shift, result);
        int available = (int) Math.max(0L, limit - used) / sides;
        int multiple = QUANTITY_POLICY.resolveAllocationMultiple(context, result,
                ShiftCapacityResolverUtil.resolveMachineMouldQty(result.getMouldQty()));
        int total = Math.min(Math.max(0, proposedTotal), available);
        total -= total % multiple;
        if (total != proposedTotal) {
            log.info("结构胎胚首班限量, factoryCode={}, batchNo={}, sourceId={}, structure={}, embryo={}, "
                            + "sku={}, status={}, machine={}, shift={}, supplyTime={}, limit={}, used={}, proposed={}, finalQty={}",
                    context.getFactoryCode(), context.getBatchNo(), source.getId(), result.getStructureName(),
                    result.getEmbryoCode(), result.getMaterialCode(), result.getProductStatus(), result.getLhMachineCode(),
                    shift.getShiftIndex(), source.getEarliestLhTime(), limit, used, proposedTotal, total);
        }
        return total;
    }

    /**
     * 提交及保存前按实际结果复核共享总量和普通双模总量；候选提交负责原子回滚，保存前仅告警。
     * @param context 当前上下文
     * @return 失败原因，通过为空
     */
    public static String validateResults(LhScheduleContext context) {
        if (context.getStructureTreadCountMap().isEmpty()) {
            return null;
        }
        for (LhScheduleResult result : context.getScheduleResultList()) {
            CxEmbryoLhTime source = resolveSource(context, result);
            for (LhShiftConfigVO shift : context.getScheduleWindowShifts()) {
                Integer limit = resolveLimit(context, source, result.getEmbryoCode(), shift);
                if (Objects.isNull(limit)) {
                    continue;
                }
                long total = usedQuantity(context, result.getStructureName(), result.getEmbryoCode(), shift, null);
                int multiple = QUANTITY_POLICY.resolveAllocationMultiple(context, result,
                        ShiftCapacityResolverUtil.resolveMachineMouldQty(result.getMouldQty()));
                if (total > limit || StructureSwitchSchedulingPolicy.quantity(result, shift.getShiftIndex()) % multiple != 0) {
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
