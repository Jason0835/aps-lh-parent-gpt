package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.cx.entity.config.CxEmbryoLhTime;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 普通排产入口的供胚首班新增物理机台限制，与独立结构切换编排资格分开。
 *
 * <p>已有同结构续作或原始结构计划非零时，普通新增仍遵守有效供胚来源的新增名额。
 * 原有续作不占新增名额；首检正量计入，纯准备和零量不计入。名额直接由当前结果派生，
 * 提交回滚及后置回裁无需维护第二套扣账状态。</p>
 *
 * @author APS
 * @date 2026/10/19
 */
@Slf4j
public final class EmbryoSupplyNewMachineLimitPolicy {

    /**
     * 换活字块结果标识。
     */
    private static final String TYPE_BLOCK_RESULT_YES = "1";

    /**
     * 工具类不允许实例化。
     */
    private EmbryoSupplyNewMachineLimitPolicy() {
    }

    /**
     * 识别普通入口需要约束的同一有效供胚来源，不授予独立切换或提前生产资格。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @return 普通入口有效来源；既有切换策略接管或供胚班不在判定窗口时为空
     */
    public static CxEmbryoLhTime resolveSource(LhScheduleContext context, SkuScheduleDTO sku) {
        if (!StructureSwitchSchedulingPolicy.isEnabled(context) || context.isStructureSwitchSelectionProbe()
                || Objects.isNull(sku) || StringUtils.isEmpty(sku.getStructureName())
                || Objects.nonNull(StructureSwitchSchedulingPolicy.resolveSource(context, sku))) {
            return null;
        }
        CxEmbryoLhTime source = context.getStructureSwitchSourceMap().get(sku.getStructureName());
        // 独立步骤已冻结的同来源整组仍由原S0/S1接管，审计或其他SKU身份不能另建普通入口口径。
        if (Objects.nonNull(source) && context.getStructureSwitchCandidateSourceMap().values().stream()
                .anyMatch(frozen -> Objects.equals(source.getId(), frozen.getId()))) {
            return null;
        }
        return Objects.nonNull(source) && Objects.nonNull(source.getEarliestLhTime())
                && Objects.nonNull(firstShift(context, source)) ? source : null;
    }

    /**
     * 按供胚及新增名额后移首检占用起点，准备动作仍由原时间轴管理。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param machineCode 候选机台或运行侧
     * @param requestedStart 原首检或生产起点
     * @return 最早合法起点；不适用保持原值，窗口内无名额时为空
     */
    public static Date resolveEarliestOccupationTime(LhScheduleContext context, SkuScheduleDTO sku,
            String machineCode, Date requestedStart) {
        CxEmbryoLhTime source = resolveSource(context, sku);
        if (Objects.isNull(source) || Objects.isNull(requestedStart)
                || baselineMachines(context, source).contains(physical(machineCode))) {
            return requestedStart;
        }
        Date notBefore = requestedStart.after(source.getEarliestLhTime())
                ? requestedStart : source.getEarliestLhTime();
        for (LhShiftConfigVO shift : StructureSwitchSchedulingPolicy.orderedShifts(context)) {
            if (!notBefore.before(shift.getShiftEndDateTime())) {
                continue;
            }
            Set<String> machines = newMachines(context, source, shift);
            machines.add(physical(machineCode));
            if (machines.size() <= limit(context, source, shift)) {
                return notBefore.after(shift.getShiftStartDateTime()) ? notBefore : shift.getShiftStartDateTime();
            }
        }
        return null;
    }

    /**
     * 校验冻结提案的首检和正式生产占用，避免正式开产在下一班时漏掉前班首检。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param machineCode 候选机台或运行侧
     * @param inspection 已冻结的首检分摊
     * @param productionStart 正式生产起点
     * @return 拒绝原因；通过或不适用为空
     */
    public static String validatePlan(LhScheduleContext context, SkuScheduleDTO sku, String machineCode,
            FirstInspectionAllocationPlan inspection, Date productionStart) {
        CxEmbryoLhTime source = resolveSource(context, sku);
        if (Objects.isNull(source) || baselineMachines(context, source).contains(physical(machineCode))) {
            return null;
        }
        Date occupationStart = Objects.nonNull(inspection) && inspection.isValid() && inspection.getInspectionQty() > 0
                ? inspection.getInspectionStartTime() : productionStart;
        if (Objects.nonNull(occupationStart) && occupationStart.before(source.getEarliestLhTime())) {
            return "供胚新增机台首检或生产早于最早供胚时间";
        }
        for (LhShiftConfigVO shift : StructureSwitchSchedulingPolicy.orderedShifts(context)) {
            boolean inspectionOccupies = Objects.nonNull(inspection) && inspection.isValid()
                    && inspection.getShiftAllocations().stream().anyMatch(allocation -> allocation.getQuantity() > 0
                    && Objects.equals(allocation.getShift().getShiftIndex(), shift.getShiftIndex()));
            boolean productionOccupies = Objects.nonNull(productionStart) && productionStart.before(shift.getShiftEndDateTime());
            if (inspectionOccupies || productionOccupies) {
                Set<String> machines = newMachines(context, source, shift);
                machines.add(physical(machineCode));
                if (machines.size() > limit(context, source, shift)) {
                    return rejection(source, shift, machines);
                }
            }
        }
        return null;
    }

    /**
     * 按当前真实正量结果复核所有普通入口，覆盖未绑定独立切换计划的新增结果。
     *
     * @param context 排程上下文
     * @return 首个供胚时间或新增名额拒绝原因；全部通过为空
     */
    public static String validateResults(LhScheduleContext context) {
        if (!StructureSwitchSchedulingPolicy.isEnabled(context)) {
            return null;
        }
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (isContinuation(result)) {
                continue;
            }
            SkuScheduleDTO sku = resultSku(result);
            CxEmbryoLhTime source = resolveSource(context, sku);
            if (Objects.isNull(source) || baselineMachines(context, source).contains(physical(result.getLhMachineCode()))) {
                continue;
            }
            for (LhShiftConfigVO shift : StructureSwitchSchedulingPolicy.orderedShifts(context)) {
                if (StructureSwitchSchedulingPolicy.quantity(result, shift.getShiftIndex()) <= 0) {
                    continue;
                }
                Date start = ShiftFieldUtil.getShiftStartTime(result, shift.getShiftIndex());
                if (Objects.isNull(start) || start.before(source.getEarliestLhTime())) {
                    return "供胚新增机台结果起点缺失或早于最早供胚时间，物料=" + result.getMaterialCode()
                            + "，机台=" + result.getLhMachineCode();
                }
                Set<String> machines = newMachines(context, source, shift);
                if (machines.size() > limit(context, source, shift)) {
                    return rejection(source, shift, machines);
                }
            }
        }
        return null;
    }

    /**
     * 输出原有续作、新增名额及最终正量证据，不依赖独立切换结果绑定。
     *
     * @param context 最终结果上下文
     */
    public static void appendFinalAudit(LhScheduleContext context) {
        if (!StructureSwitchSchedulingPolicy.isEnabled(context)) {
            return;
        }
        for (CxEmbryoLhTime source : context.getStructureSwitchSourceMap().values()) {
            SkuScheduleDTO sku = new SkuScheduleDTO();
            sku.setStructureName(source.getNextStructureName());
            if (Objects.isNull(resolveSource(context, sku))) {
                continue;
            }
            for (LhShiftConfigVO shift : StructureSwitchSchedulingPolicy.orderedShifts(context)) {
                int shiftLimit = limit(context, source, shift);
                if (shiftLimit == Integer.MAX_VALUE) {
                    continue;
                }
                Set<String> added = newMachines(context, source, shift);
                String detail = "batchNo=" + context.getBatchNo() + "，factoryCode=" + context.getFactoryCode()
                        + "，sourceId=" + source.getId() + "，结构=" + source.getNextStructureName()
                        + "，班次=class" + shift.getShiftIndex() + "，供胚=" + LhScheduleTimeUtil.formatDateTime(source.getEarliestLhTime())
                        + "，原有续作机台=" + baselineMachines(context, source) + "，新增机台=" + added
                        + "，新增台数=" + added.size() + "，新增上限=" + shiftLimit + "，通过=" + (added.size() <= shiftLimit);
                PriorityTraceLogHelper.appendProcessLog(context, "供胚首班新增机台审计", detail);
                log.info("供胚首班新增机台审计, {}", detail);
            }
        }
        String failure = validateResults(context);
        if (StringUtils.isNotEmpty(failure)) {
            log.error("供胚首班新增机台保存前审计失败, factoryCode={}, batchNo={}, reason={}",
                    context.getFactoryCode(), context.getBatchNo(), failure);
        }
    }

    /**
     * 定位供胚首班，窗口前T日晚班供胚映射窗口首班，不按实际首产后移。
     *
     * @param context 班次上下文
     * @param source 有效供胚来源
     * @return 供胚对应窗口首班；无法映射时为空
     */
    private static LhShiftConfigVO firstShift(LhScheduleContext context, CxEmbryoLhTime source) {
        List<LhShiftConfigVO> shifts = StructureSwitchSchedulingPolicy.orderedShifts(context);
        if (shifts.isEmpty()) {
            return null;
        }
        LhShiftConfigVO supply = LhScheduleTimeUtil.resolveShiftByTime(
                StructureSwitchFirstShiftPolicy.decisionShifts(context), source.getEarliestLhTime());
        return Objects.nonNull(supply) && supply.getShiftStartDateTime().before(shifts.get(0).getShiftStartDateTime())
                ? shifts.get(0) : supply;
    }

    /**
     * 读取供胚首班及紧邻第二班的新增上限，后续班次不叠加额外限制。
     *
     * @param context 班次上下文
     * @param source 有效来源
     * @param shift 待判断班次
     * @return 当前班新增上限；非受限班返回最大整数
     */
    private static int limit(LhScheduleContext context, CxEmbryoLhTime source, LhShiftConfigVO shift) {
        LhShiftConfigVO first = firstShift(context, source);
        if (Objects.isNull(first)) {
            return Integer.MAX_VALUE;
        }
        if (Objects.equals(first.getShiftStartDateTime(), shift.getShiftStartDateTime())) {
            return StructureSwitchSchedulingPolicy.shiftLimit(source.getIsWaitNotify(), 0);
        }
        LhShiftConfigVO second = StructureSwitchSchedulingPolicy.nextShift(context, first.getShiftStartDateTime());
        return Objects.nonNull(second) && Objects.equals(second.getShiftStartDateTime(), shift.getShiftStartDateTime())
                ? StructureSwitchSchedulingPolicy.shiftLimit(source.getIsWaitNotify(), 1) : Integer.MAX_VALUE;
    }

    /**
     * 从当前有效续作正量识别原有物理机台，换活字块不冒充续作基线。
     *
     * @param context 当前结果上下文
     * @param source 有效来源
     * @return 同结构原有物理机台集合
     */
    private static Set<String> baselineMachines(LhScheduleContext context, CxEmbryoLhTime source) {
        return context.getScheduleResultList().stream().filter(EmbryoSupplyNewMachineLimitPolicy::isContinuation)
                .filter(result -> StringUtils.equals(source.getNextStructureName(), result.getStructureName()))
                .filter(result -> ShiftFieldUtil.resolveScheduledQty(result) > 0)
                .map(result -> physical(result.getLhMachineCode())).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 跨SKU累计同来源在本班的新增正量机台，剔除原续作并按物理机台合并L/R。
     *
     * @param context 当前结果上下文
     * @param source 有效来源
     * @param shift 真实班次
     * @return 当前班新增物理机台集合
     */
    private static Set<String> newMachines(LhScheduleContext context, CxEmbryoLhTime source, LhShiftConfigVO shift) {
        Set<String> baseline = baselineMachines(context, source);
        return context.getScheduleResultList().stream().filter(result -> !isContinuation(result))
                .filter(result -> StringUtils.equals(source.getNextStructureName(), result.getStructureName()))
                .filter(result -> StructureSwitchSchedulingPolicy.quantity(result, shift.getShiftIndex()) > 0)
                .map(result -> physical(result.getLhMachineCode())).filter(machine -> !baseline.contains(machine))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 判断真实续作身份，避免换活字块结果占用原有续作基线。
     *
     * @param result 当前结果
     * @return 是否为普通续作结果
     */
    private static boolean isContinuation(LhScheduleResult result) {
        return StringUtils.equals(ScheduleTypeEnum.CONTINUOUS.getCode(), result.getScheduleType())
                && !StringUtils.equals(TYPE_BLOCK_RESULT_YES, result.getIsTypeBlock());
    }

    /**
     * 归一化运行侧编码为物理机台编码。
     *
     * @param machineCode 机台或运行侧编码
     * @return 对应物理机台编码
     */
    private static String physical(String machineCode) {
        return LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
    }

    /**
     * 从结果恢复来源识别所需的SKU身份。
     *
     * @param result 当前结果
     * @return 物料、状态及结构身份
     */
    private static SkuScheduleDTO resultSku(LhScheduleResult result) {
        SkuScheduleDTO sku = new SkuScheduleDTO();
        sku.setMaterialCode(result.getMaterialCode());
        sku.setProductStatus(result.getProductStatus());
        sku.setStructureName(result.getStructureName());
        return sku;
    }

    /**
     * 生成可关联来源、班次及实际机台的公共拒绝原因。
     *
     * @param source 有效来源
     * @param shift 受限班次
     * @param machines 本次提案加入后的新增物理机台
     * @return 公共限台拒绝原因
     */
    private static String rejection(CxEmbryoLhTime source, LhShiftConfigVO shift, Set<String> machines) {
        return "供胚首班新增机台上限拒绝，sourceId=" + source.getId() + "，结构=" + source.getNextStructureName()
                + "，班次=class" + shift.getShiftIndex() + "，新增机台=" + machines;
    }
}
