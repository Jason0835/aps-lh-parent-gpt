package com.zlt.aps.lh.engine.strategy.support;

import cn.hutool.core.bean.BeanUtil;
import com.zlt.aps.cx.entity.config.CxEmbryoLhTime;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 按供胚到班末的时间产能统一判定结构切换规则首班，试算不登记资源。
 *
 * @author APS
 * @date 2026/10/14
 */
@Slf4j
public final class StructureSwitchFirstShiftPolicy {
    /**
     * 普通机台首班至少完整完成两条。
     */
    private static final int NORMAL_MINIMUM_QUANTITY = 2;
    /**
     * 单控运行侧首班至少完整完成一条。
     */
    private static final int SINGLE_CONTROL_MINIMUM_QUANTITY = 1;
    /**
     * T日晚班只作判定坐标，不新增结果班次槽位。
     */
    private static final int HISTORICAL_SHIFT_INDEX = 0;
    /**
     * 既有夜班解析器要求正偏移表示前一自然日晚间，历史模板以T-1为基准并偏移一日。
     */
    private static final int HISTORICAL_NIGHT_DATE_OFFSET = 1;

    /**
     * 工具类不允许实例化。
     */
    private StructureSwitchFirstShiftPolicy() {
    }

    /**
     * 在原八班前补齐T日晚班；复用当前工厂晚班模板，独立后置副本不扩展窗口。
     *
     * @param context 已加载班次配置的上下文
     * @return 按真实时间排序的判定班次
     */
    public static List<LhShiftConfigVO> decisionShifts(LhScheduleContext context) {
        List<LhShiftConfigVO> shifts = new ArrayList<>(StructureSwitchSchedulingPolicy.orderedShifts(context));
        if (context.isIsolatedNextShiftPlan() || shifts.isEmpty() || Objects.isNull(context.getScheduleDate())) {
            return shifts;
        }
        LhShiftConfigVO template = shifts.stream().filter(LhShiftConfigVO::isNightShift).findFirst().orElse(null);
        if (Objects.nonNull(template)) {
            LhShiftConfigVO historical = new LhShiftConfigVO();
            BeanUtil.copyProperties(template, historical);
            historical.setScheduleBaseDate(LhScheduleTimeUtil.addDays(context.getScheduleDate(),
                    -HISTORICAL_NIGHT_DATE_OFFSET));
            historical.setDateOffset(HISTORICAL_NIGHT_DATE_OFFSET);
            historical.setShiftIndex(HISTORICAL_SHIFT_INDEX);
            historical.setShiftName("T日晚班");
            if (historical.getShiftEndDateTime().equals(shifts.get(0).getShiftStartDateTime())) {
                shifts.add(historical);
            }
        }
        shifts.sort(Comparator.comparing(LhShiftConfigVO::getShiftStartDateTime));
        return shifts;
    }

    /**
     * 为当前SKU和机台取得共享首班判定，运行侧班产沿用正式数量内核的拆分规则。
     *
     * @param context 本批上下文
     * @param source 有效供胚来源
     * @param sku 当前候选
     * @param machineCode 运行态机台编码
     * @return 只读判定，非适用范围返回空
     */
    public static StructureSwitchFirstShiftDecision resolve(LhScheduleContext context, CxEmbryoLhTime source,
            SkuScheduleDTO sku, String machineCode) {
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
        int capacity = Objects.isNull(sku) ? 0 : sku.getShiftCapacity();
        if (Objects.isNull(machine)) {
            // 某些固定组合入口只传编码；仍通过现有班产解析器取得单控侧班产。
            machine = new MachineScheduleDTO();
            machine.setMachineCode(machineCode);
        }
        return resolve(context, source, sku, machineCode,
                ShiftCapacityResolverUtil.resolveRuntimeShiftCapacity(context, machine, capacity));
    }

    /**
     * 按供胚后的原始时间产能认定首班，已成功提交的同来源判定跨SKU共享。
     *
     * @param context 本批上下文和班次配置
     * @param source 有效来源
     * @param sku 参照候选SKU
     * @param machineCode 参照运行态机台
     * @param runtimeCapacity 正式内核使用的运行态完整班产
     * @return 判定结果；非结构切换或供胚不在适用范围时为空
     */
    public static StructureSwitchFirstShiftDecision resolve(LhScheduleContext context, CxEmbryoLhTime source,
            SkuScheduleDTO sku, String machineCode, int runtimeCapacity) {
        if (!StructureSwitchSchedulingPolicy.isEnabled(context) || Objects.isNull(source)
                || Objects.isNull(source.getEarliestLhTime()) || Objects.isNull(sku)) {
            return null;
        }
        StructureSwitchRuntimeState state = context.getStructureSwitchRuntimeMap().get(source.getNextStructureName());
        if (Objects.nonNull(state) && Objects.equals(source.getId(), state.getSourceId())
                && Objects.nonNull(state.getFirstShiftDecision())) {
            return state.getFirstShiftDecision();
        }
        if (context.isIsolatedNextShiftPlan()) {
            return null;
        }
        List<LhShiftConfigVO> shifts = decisionShifts(context);
        LhShiftConfigVO supplyShift = LhScheduleTimeUtil.resolveShiftByTime(shifts, source.getEarliestLhTime());
        if (Objects.isNull(supplyShift)) {
            return null;
        }
        boolean singleControl = LhSingleControlMachineUtil.isConfiguredSingleControlMachine(context, machineCode);
        int minimum = singleControl ? SINGLE_CONTROL_MINIMUM_QUANTITY : NORMAL_MINIMUM_QUANTITY;
        // 复用数量内核的完整班长基准，不能因有效窗口缩短而把主数据班产对应速度放大。
        long duration = ShiftCapacityResolverUtil.resolveShiftDurationSeconds(supplyShift);
        long remaining = TimeUnit.MILLISECONDS.toSeconds(
                supplyShift.getShiftEndDateTime().getTime() - source.getEarliestLhTime().getTime());
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
        int mouldQty = sku.getMouldQty();
        if (singleControl) {
            mouldQty = SINGLE_CONTROL_MINIMUM_QUANTITY;
        } else if (Objects.nonNull(machine)) {
            mouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(machine);
        }
        int actualCapacity = ShiftCapacityResolverUtil.resolveActualShiftPlanQty(runtimeCapacity, supplyShift,
                ShiftCapacityResolverUtil.resolveOddShiftCapacityPlusShiftType(context),
                ScheduleTypeEnum.NEW_SPEC.getCode());
        boolean capacityKnown = duration > 0 && (actualCapacity > 0
                || (sku.getLhTimeSeconds() > 0 && mouldQty > 0));
        int quantity = capacityKnown ? ShiftCapacityResolverUtil.resolveShiftCapacity(actualCapacity,
                sku.getLhTimeSeconds(), mouldQty, duration, remaining, true) : 0;
        LhShiftConfigVO first = null;
        String reason = "首班缺少有效产能依据";
        if (capacityKnown) {
            first = quantity >= minimum ? supplyShift : shifts.stream()
                    .filter(shift -> shift.getShiftStartDateTime().after(supplyShift.getShiftStartDateTime()))
                    .findFirst().orElse(null);
            if (quantity >= minimum) {
                reason = "供胚班时间产能达标";
            } else if (Objects.nonNull(first)) {
                reason = "供胚班时间产能不足，首班顺延紧邻下一班";
            } else {
                reason = "首班超出本次判定范围";
            }
        }
        log.debug("结构切换首班判定, batchNo={}, sourceId={}, sku={}, machine={}, supplyTime={}, "
                        + "supplyShift={}, durationSeconds={}, remainingSeconds={}, runtimeCapacity={}, "
                        + "minimumQty={}, availableQty={}, firstShift={}, reason={}",
                context.getBatchNo(), source.getId(), sku.getMaterialCode(), machineCode, source.getEarliestLhTime(),
                supplyShift.getShiftName(), duration, remaining, actualCapacity, minimum, quantity,
                Objects.isNull(first) ? null : first.getShiftName(), reason);
        return new StructureSwitchFirstShiftDecision(source.getId(), source.getEarliestLhTime(), supplyShift,
                first, machineCode, actualCapacity, minimum, quantity, reason,
                Collections.unmodifiableList(new ArrayList<>(shifts)));
    }
}
