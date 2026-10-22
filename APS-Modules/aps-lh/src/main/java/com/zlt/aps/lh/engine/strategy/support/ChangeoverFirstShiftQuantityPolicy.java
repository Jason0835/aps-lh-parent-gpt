package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.context.LhScheduleConfig;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 实际换模或换活字块首班的公共数量上限，复用原首检班次和数量单位，不维护独立扣量账本。
 */
@Slf4j
public final class ChangeoverFirstShiftQuantityPolicy {

    /**
     * 单控整机预演的配对侧数，参数限制按物理整机合计解释。
     */
    private static final int WHOLE_MACHINE_SIDES = 2;

    /**
     * 既有数量单位解析器，不重新实现普通双模、单控及胎胚收尾的归整规则。
     */
    private static final TargetScheduleQtyResolver QUANTITY_POLICY = new TargetScheduleQtyResolver();

    /**
     * 禁止实例化只读数量策略。
     */
    private ChangeoverFirstShiftQuantityPolicy() {
    }

    /**
     * 冻结实际切换的首个计量班次及本行上限，并在写入前检查能否保留完整首检。
     *
     * @param context 本批配置及机台模式
     * @param sku 当前SKU
     * @param result 待分班结果
     * @param shifts 本次排产班次
     * @param inspectionShift 原逻辑确定的首检归属班次
     * @param inspectionPlan 原首检时间及数量计划
     * @param startTime 原正式生产开始时间
     * @param actualChangeover 是否实际发生换模或换活字块
     * @return 上限足以保留完整首检或不启用限制时返回true
     */
    public static boolean bind(LhScheduleContext context, SkuScheduleDTO sku, LhScheduleResult result,
            List<LhShiftConfigVO> shifts, LhShiftConfigVO inspectionShift,
            FirstInspectionAllocationPlan inspectionPlan, Date startTime, boolean actualChangeover) {
        if (!actualChangeover || Objects.nonNull(result.getChangeoverFirstShiftStartTime())) {
            return true;
        }
        // 正式排程读取已挂载快照；独立调用沿用上下文原始参数入口，不额外查询配置。
        LhScheduleConfig config = Objects.nonNull(context.getScheduleConfig())
                ? context.getScheduleConfig() : new LhScheduleConfig(context.getLhParamsMap());
        int limit = config.getFirstShiftPlanQtyLimit();
        if (limit < 0) {
            return true;
        }
        LhShiftConfigVO firstShift = resolveFirstShift(shifts, inspectionShift, inspectionPlan, startTime);
        if (Objects.isNull(firstShift)) {
            return true;
        }
        int sides = LhSingleControlMachineUtil.isConfiguredSingleControlMachine(context, result.getLhMachineCode())
                && LhSingleControlMachineUtil.isWholeMachineGranularitySku(context, sku) ? WHOLE_MACHINE_SIDES : 1;
        result.setChangeoverFirstShiftStartTime(firstShift.getShiftStartDateTime());
        result.setChangeoverFirstShiftQtyLimit(limit / sides);
        if (Objects.nonNull(inspectionPlan)) {
            for (FirstInspectionShiftAllocation allocation : inspectionPlan.getQuantityAllocations()) {
                if (!canRetainInspection(context, result, allocation.getShift(), allocation.getQuantity())) {
                    return false;
                }
            }
        }
        log.debug("换模首班数量上限冻结, factoryCode={}, scheduleDate={}, batchNo={}, sku={}, status={}, "
                        + "machine={}, firstShift={}, physicalLimit={}, sides={}, rowLimit={}",
                context.getFactoryCode(), context.getScheduleDate(), context.getBatchNo(), result.getMaterialCode(),
                result.getProductStatus(), result.getLhMachineCode(), firstShift.getShiftStartDateTime(),
                limit, sides, result.getChangeoverFirstShiftQtyLimit());
        return true;
    }

    /**
     * 按冻结的单班首检计量定位首班；无计件首检时沿用归属班次，未提供归属时按开产时间定位。
     *
     * @param shifts 原排产班次
     * @param inspectionShift 原首检归属班次
     * @param inspectionPlan 原首检计划
     * @param startTime 原正式生产开始时间
     * @return 原有首个计量班次，窗口内没有对应班次时为空
     */
    private static LhShiftConfigVO resolveFirstShift(List<LhShiftConfigVO> shifts,
            LhShiftConfigVO inspectionShift, FirstInspectionAllocationPlan inspectionPlan, Date startTime) {
        if (Objects.nonNull(inspectionPlan)) {
            if (inspectionPlan.isSingleShiftQuantity() && !inspectionPlan.getQuantityAllocations().isEmpty()) {
                return inspectionPlan.getQuantityAllocations().get(0).getShift();
            }
            LhShiftConfigVO first = inspectionPlan.getQuantityAllocations().stream()
                    .filter(allocation -> allocation.getQuantity() > 0)
                    .map(FirstInspectionShiftAllocation::getShift)
                    .min(Comparator.comparing(LhShiftConfigVO::getShiftStartDateTime)).orElse(null);
            if (Objects.nonNull(first)) {
                return first;
            }
        }
        if (Objects.nonNull(inspectionShift)) {
            return inspectionShift;
        }
        return shifts.stream().filter(shift -> Objects.nonNull(startTime)
                        && !startTime.before(shift.getShiftStartDateTime())
                        && startTime.before(shift.getShiftEndDateTime()))
                .findFirst().orElse(null);
    }

    /**
     * 判断当前班次是否命中本次实际切换冻结的首班限制，回裁或跨日不能重新定位首班。
     *
     * @param result 当前结果
     * @param shift 当前班次
     * @return 本行具有有效首班上限且班次坐标一致时返回true
     */
    public static boolean isLimited(LhScheduleResult result, LhShiftConfigVO shift) {
        return Objects.nonNull(result) && Objects.nonNull(shift)
                && Objects.nonNull(result.getChangeoverFirstShiftQtyLimit())
                && Objects.nonNull(result.getChangeoverFirstShiftStartTime())
                && result.getChangeoverFirstShiftStartTime().equals(shift.getShiftStartDateTime());
    }

    /**
     * 对已计算的含首检总量应用首班上限，只有实际裁量时才按既有数量单位向下归整。
     *
     * @param context 原数量单位上下文
     * @param result 当前结果或整机合计结果
     * @param shift 当前班次
     * @param proposedTotal 已执行原规则的候选总量
     * @return 首班合法总量；非首班或关闭限制时原样返回
     */
    public static int capTotal(LhScheduleContext context, LhScheduleResult result,
            LhShiftConfigVO shift, int proposedTotal) {
        if (!isLimited(result, shift) || proposedTotal <= result.getChangeoverFirstShiftQtyLimit()) {
            return proposedTotal;
        }
        int mouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(result.getMouldQty());
        int multiple = QUANTITY_POLICY.resolveAllocationMultiple(context, result, mouldQty);
        int retained = result.getChangeoverFirstShiftQtyLimit();
        retained -= retained % multiple;
        log.info("换模首班计划量限量, factoryCode={}, scheduleDate={}, batchNo={}, sku={}, status={}, "
                        + "machine={}, shift={}, firstShift={}, proposedTotal={}, rowLimit={}, finalTotal={}",
                context.getFactoryCode(), context.getScheduleDate(), context.getBatchNo(), result.getMaterialCode(),
                result.getProductStatus(), result.getLhMachineCode(), shift.getShiftIndex(),
                result.getChangeoverFirstShiftStartTime(), proposedTotal,
                result.getChangeoverFirstShiftQtyLimit(), retained);
        return retained;
    }

    /**
     * 限制同班追加量，已写首检及普通量只计算一次，差额留给原后续分班和余量逻辑。
     *
     * @param context 当前排程上下文
     * @param result 当前结果
     * @param shift 当前班次
     * @param proposed 原候选增量
     * @return 可实际追加的数量
     */
    public static int capIncrement(LhScheduleContext context, LhScheduleResult result,
            LhShiftConfigVO shift, int proposed) {
        if (!isLimited(result, shift) || proposed <= 0) {
            return proposed;
        }
        int existing = StructureSwitchSchedulingPolicy.quantity(result, shift.getShiftIndex());
        return Math.max(0, capTotal(context, result, shift, existing + proposed) - existing);
    }

    /**
     * 检查完整首检是否可保留，不通过截断首检数量满足新上限。
     *
     * @param context 当前上下文
     * @param result 当前结果
     * @param shift 原首检所在班次
     * @param quantity 原首检条数
     * @return 首检不突破本行首班上限时返回true
     */
    public static boolean canRetainInspection(LhScheduleContext context, LhScheduleResult result,
            LhShiftConfigVO shift, int quantity) {
        if (!isLimited(result, shift) || quantity <= result.getChangeoverFirstShiftQtyLimit()) {
            return true;
        }
        log.info("换模首班上限不足完整首检，当前候选不提交, factoryCode={}, batchNo={}, sku={}, "
                        + "status={}, machine={}, shift={}, inspectionQty={}, rowLimit={}",
                context.getFactoryCode(), context.getBatchNo(), result.getMaterialCode(), result.getProductStatus(),
                result.getLhMachineCode(), shift.getShiftIndex(), quantity, result.getChangeoverFirstShiftQtyLimit());
        return false;
    }

    /**
     * 将单侧冻结上限换算为整机合计口径，配合既有合计扣账及均分回侧流程。
     *
     * @param groupResult 已由单侧复制的整机合计结果
     */
    public static void scaleToWholeMachine(LhScheduleResult groupResult) {
        Integer limit = groupResult.getChangeoverFirstShiftQtyLimit();
        if (Objects.nonNull(limit)) {
            groupResult.setChangeoverFirstShiftQtyLimit(limit * WHOLE_MACHINE_SIDES);
        }
    }

    /**
     * 为跨日临时增量继承首班剩余额度，原结果已写首检或生产量不能在清空副本后再次领取。
     *
     * @param sourceResult 已提交原结果
     * @param deltaResult 已复制业务字段的临时增量结果
     * @param shifts 原完整班次坐标
     */
    public static void inheritRemainingLimit(LhScheduleResult sourceResult, LhScheduleResult deltaResult,
            List<LhShiftConfigVO> shifts) {
        if (Objects.isNull(sourceResult.getChangeoverFirstShiftQtyLimit())) {
            return;
        }
        int used = shifts.stream().filter(shift -> isLimited(sourceResult, shift))
                .mapToInt(shift -> StructureSwitchSchedulingPolicy.quantity(sourceResult, shift.getShiftIndex()))
                .sum();
        deltaResult.setChangeoverFirstShiftQtyLimit(Math.max(0,
                sourceResult.getChangeoverFirstShiftQtyLimit() - used));
    }

    /**
     * 保存前只读核对最终首班量，记录可关联的逐行证据及汇总，不重新分配数量或资源。
     *
     * @param context 已完成全部后处理的排程上下文
     */
    public static void auditResults(LhScheduleContext context) {
        int checked = 0;
        int exceeded = 0;
        for (LhScheduleResult result : context.getScheduleResultList()) {
            for (LhShiftConfigVO shift : context.getScheduleWindowShifts()) {
                if (!isLimited(result, shift)) {
                    continue;
                }
                int quantity = StructureSwitchSchedulingPolicy.quantity(result, shift.getShiftIndex());
                checked++;
                boolean excess = quantity > result.getChangeoverFirstShiftQtyLimit();
                if (excess) {
                    exceeded++;
                }
                log.info("换模首班最终数量核对, factoryCode={}, batchNo={}, sku={}, status={}, machine={}, "
                                + "shift={}, firstShift={}, finalQty={}, rowLimit={}, exceeded={}",
                        context.getFactoryCode(), context.getBatchNo(), result.getMaterialCode(),
                        result.getProductStatus(),
                        result.getLhMachineCode(), shift.getShiftIndex(), result.getChangeoverFirstShiftStartTime(),
                        quantity, result.getChangeoverFirstShiftQtyLimit(), excess);
            }
        }
        String detail = "工厂=" + context.getFactoryCode() + "，批次=" + context.getBatchNo()
                + "，核对结果行=" + checked + "，超限结果行=" + exceeded;
        PriorityTraceLogHelper.appendProcessLog(context, "换模首班计划量上限核对", detail);
        if (exceeded > 0) {
            log.warn("换模首班最终数量超限, {}", detail);
        } else {
            log.info("换模首班最终数量核对完成, {}", detail);
        }
    }
}
