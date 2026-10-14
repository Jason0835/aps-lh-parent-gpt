package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.cx.entity.config.CxEmbryoLhTime;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.lh.util.FirstInspectionQtyUtil;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 结构切换新机台的供胚等待准入，预演和提交复用原提案冻结的准备后最早首检时间。
 *
 * @author APS
 * @date 2026/10/13
 */
@Slf4j
public final class StructureSwitchSelectionTimePolicy {
    /**
     * 相邻竞争班次的供胚比较偏移，单位为小时。
     */
    private static final int SHIFT_OFFSET_HOURS = 8;

    /**
     * 工具类不允许实例化。
     */
    private StructureSwitchSelectionTimePolicy() {
    }

    /**
     * 检查完整提案，使用准备后的最早首检起点，不使用被供胚或名额等待推迟后的时间缩短等待差。
     *
     * @param context 本批上下文及参数快照
     * @param sku 当前候选物料
     * @param availability 已冻结的机台时间轴
     * @return 拒绝原因；非结构切换或允许时为空
     */
    public static String validate(LhScheduleContext context, SkuScheduleDTO sku,
            NewSpecMachineAvailabilityPlan availability) {
        CxEmbryoLhTime source = StructureSwitchSchedulingPolicy.resolveSource(context, sku);
        if (Objects.isNull(source) || Objects.isNull(availability) || !availability.isAvailable()) {
            return null;
        }
        if (Objects.isNull(availability.getSkuSelectionStartTime())) {
            return I18nUtil.getMessage("ui.lh.structureSwitch.selectionInspectionMissing");
        }
        // 班次偏移取实际竞争的首检班次；比较基准独立冻结，不能反向挪到该班起点。
        // 旧时间轴可能仍将首检放在供胚前，候选班次至少从供胚时刻所在班开始。
        Date occupation = StructureSwitchSchedulingPolicy.later(
                availability.getProductionOccupationStartTime(), source.getEarliestLhTime());
        LhShiftConfigVO shift = FirstInspectionQtyUtil.resolveAttributionShift(
                context.getScheduleWindowShifts(), occupation);
        StructureSwitchPlan plan = availability.getStructureSwitchPlan();
        StructureSwitchFirstShiftDecision decision = Objects.isNull(plan) ? null : plan.getFirstShiftDecision();
        if (Objects.isNull(decision)) {
            decision = StructureSwitchFirstShiftPolicy.resolve(context, source, sku,
                    availability.getMachine().getMachineCode());
        }
        if (Objects.nonNull(decision) && Objects.isNull(decision.getFirstShift())) {
            return decision.getReason();
        }
        return validate(context, source, availability.getSkuSelectionStartTime(), shift,
                sku.getMaterialCode(), sku.getProductStatus(), availability.getMachine().getMachineCode(), decision);
    }

    /**
     * 按首个实际切换班次计算候选偏移；首台、历史承接和普通组合执行相同门禁。
     * 已上机连续生产不调用此方法；只记录本窗口历史承接诊断，不登记资源。
     *
     * @param context 本批上下文
     * @param source 同一有效供胚记录
     * @param selectionInspectionStartTime 完成必要切换准备后冻结的最早首检开始时间
     * @param shift 新候选首检或生产所在班次
     * @param materialCode 物料编码
     * @param productStatus 产品状态
     * @param machineCode 机台编码
     * @return 超过阈值时的诊断原因，其余为空
     */
    public static String validate(LhScheduleContext context, CxEmbryoLhTime source,
            Date selectionInspectionStartTime, LhShiftConfigVO shift, String materialCode,
            String productStatus, String machineCode) {
        return validate(context, source, selectionInspectionStartTime, shift, materialCode,
                productStatus, machineCode, null);
    }

    /**
     * 使用同一规则首班计算等待偏移，不以实际首产班替换时间产能判定。
     *
     * @param context 本批上下文
     * @param source 有效供胚来源
     * @param selectionInspectionStartTime 准备后冻结的最早首检起点
     * @param shift 当前占用班次
     * @param materialCode 物料编码
     * @param productStatus 产品状态
     * @param machineCode 运行态机台
     * @param decision 候选规则首班判定
     * @return 拒绝原因，通过时为空
     */
    private static String validate(LhScheduleContext context, CxEmbryoLhTime source,
            Date selectionInspectionStartTime, LhShiftConfigVO shift, String materialCode,
            String productStatus, String machineCode, StructureSwitchFirstShiftDecision decision) {
        if (!StructureSwitchSchedulingPolicy.isEnabled(context) || Objects.isNull(source)
                || Objects.isNull(source.getEarliestLhTime()) || Objects.isNull(shift)
                || Objects.isNull(selectionInspectionStartTime)) {
            return null;
        }
        StructureSwitchRuntimeState state = context.getStructureSwitchRuntimeMap().get(source.getNextStructureName());
        Date anchor = shift.getShiftStartDateTime();
        if (Objects.nonNull(state) && Objects.equals(state.getSourceId(), source.getId())) {
            anchor = state.getFirstShiftStart();
        } else if (Objects.nonNull(decision) && Objects.nonNull(decision.getFirstShift())) {
            anchor = decision.getFirstShift().getShiftStartDateTime();
        }
        Date firstShiftStart = anchor;
        List<LhShiftConfigVO> shifts = StructureSwitchFirstShiftPolicy.decisionShifts(context);
        if (Objects.nonNull(state) && Objects.equals(state.getSourceId(), source.getId())
                && Objects.nonNull(state.getFirstShiftDecision())) {
            shifts = state.getFirstShiftDecision().getReferenceShifts();
        } else if (Objects.nonNull(decision)) {
            shifts = decision.getReferenceShifts();
        }
        // 使用冻结的原班次坐标，隔离班次9不能因只保留两个槽位而被误判为第二班。
        int offset = (int) shifts.stream().filter(item -> !item.getShiftStartDateTime().before(firstShiftStart)
                && item.getShiftStartDateTime().before(shift.getShiftStartDateTime())).count();
        int offsetHours = resolveOffsetHours(source.getIsWaitNotify(), offset);
        BigDecimal threshold = Objects.nonNull(context.getScheduleConfig())
                ? context.getScheduleConfig().getMachineSkuEmbryoMaxWaitHours()
                : LhScheduleConstant.MACHINE_SKU_EMBRYO_MAX_WAIT_HOURS;
        long gapMillis = source.getEarliestLhTime().getTime() + TimeUnit.HOURS.toMillis(offsetHours)
                - selectionInspectionStartTime.getTime();
        boolean allowed = BigDecimalUtils.valueOf(gapMillis).compareTo(
                threshold.multiply(BigDecimalUtils.valueOf(TimeUnit.HOURS.toMillis(1)))) <= 0;
        // 后续班次偏移只会增加差值；基础差值超限才是本窗口确定无法承接的证据。
        long baseGapMillis = source.getEarliestLhTime().getTime() - selectionInspectionStartTime.getTime();
        boolean baseWaitExceeded = BigDecimalUtils.valueOf(baseGapMillis).compareTo(
                threshold.multiply(BigDecimalUtils.valueOf(TimeUnit.HOURS.toMillis(1)))) > 0;
        HistoricalStructureHandoffPolicy.recordWaitingAdmission(context, source, selectionInspectionStartTime,
                materialCode, productStatus, machineCode, baseWaitExceeded);
        log.info("结构切换候选等待准入, factoryCode={}, batchNo={}, sourceId={}, sku={}, status={}, machine={}, "
                        + "shift={}, anchor={}, waitNotify={}, embryoTime={}, offsetHours={}, "
                        + "selectionInspectionStartTime={}, "
                        + "gapMillis={}, thresholdHours={}, allowed={}",
                context.getFactoryCode(), context.getBatchNo(), source.getId(), materialCode, productStatus,
                machineCode, shift.getShiftIndex(), anchor, source.getIsWaitNotify(), source.getEarliestLhTime(),
                offsetHours, selectionInspectionStartTime, gapMillis, threshold, allowed);
        if (allowed) {
            return null;
        }
        return MessageFormat.format(I18nUtil.getMessage("ui.lh.structureSwitch.embryoTimeGapExceeded"),
                threshold.stripTrailingZeros().toPlainString());
    }

    /**
     * 仅为已明确的第二、第三班增加偏移，其他班次仍执行原始供胚时间差门禁。
     *
     * @param waitNotify 是否等结果
     * @param shiftOffset 相对结构实际首班的连续班次偏移
     * @return 比较用小时偏移，不改变生产时间轴
     */
    static int resolveOffsetHours(Integer waitNotify, int shiftOffset) {
        if (shiftOffset == 1 || (shiftOffset == 2 && Objects.equals(1, waitNotify))) {
            return shiftOffset * SHIFT_OFFSET_HOURS;
        }
        return 0;
    }
}
