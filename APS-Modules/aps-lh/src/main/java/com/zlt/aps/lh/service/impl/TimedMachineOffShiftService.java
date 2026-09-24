package com.zlt.aps.lh.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.ShiftEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.engine.strategy.support.OffMachineDecision;
import com.zlt.aps.lh.engine.strategy.support.TimedMachineOffRequest;
import com.zlt.aps.lh.util.LhMouldCodeUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.lh.util.ResultShiftTimeUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 按时间下机的独立班次决策与提交服务。
 * 不选机、不改变下机优先级、不计算余量收尾、不选择后料，也不预占未来换模次数。
 * 所有状态存于批次上下文；本服务不保存跨批次可变状态。
 */
@Slf4j
@Service
public class TimedMachineOffShiftService {
    /** 次数账本早班槽位，沿用正式换模统计口径。 */
    private static final int MORNING_INDEX = 0;
    /** 次数账本中班槽位。 */
    private static final int AFTERNOON_INDEX = 1;
    /** 结果分析中统一标明独立时间边界。 */
    private static final String TIMED_OFF_ANALYSIS = "按时间下机边界调整";
    /** 仅调用无副作用余量预览，正式扣账仍由原续作入口完成。 */
    private final TargetScheduleQtyResolver quantityResolver = new TargetScheduleQtyResolver();

    /**
     * 从需求日逐日读取正式次数，选择窗口内合法的早班或中班。
     * @param context 本批上下文，包含参数、真实班次和正式次数账本
     * @param startDate 前置选机确定的需求日期
     * @param machineCode 已选定机台，仅用于日志
     * @param sku 已选定机台的来源 SKU，仅用于日志
     * @return 决策；无合法班次时下机时间为空，占用保持到窗口末端
     */
    public OffMachineDecision resolveOffMachineTime(LhScheduleContext context, LocalDate startDate,
            String machineCode, SkuScheduleDTO sku) {
        return this.resolveOffMachineTime(context, startDate, machineCode, sku, null);
    }

    /**
     * 正式计算复用同一日期循环，额外校验真实画像的物理交接边界。
     * @param context 上下文
     * @param startDate 下机需求日
     * @param machineCode 已选机台
     * @param sku 来源SKU
     * @param request 已冻结画像；纯班次规则预览时为空
     * @return 日期、班次、实际下机或窗口占用决策
     */
    private OffMachineDecision resolveOffMachineTime(LhScheduleContext context, LocalDate startDate,
            String machineCode, SkuScheduleDTO sku, TimedMachineOffRequest request) {
        List<LhShiftConfigVO> shifts = context.getScheduleWindowShifts();
        if (Objects.isNull(startDate) || CollectionUtils.isEmpty(shifts)) {
            throw new IllegalArgumentException("按时间下机缺少需求日期或排程班次");
        }
        LocalDate lastDate = shifts.stream().map(shift -> this.toLocalDate(shift.getWorkDate()))
                .max(LocalDate::compareTo).get();
        for (LocalDate date = startDate; !date.isAfter(lastDate); date = date.plusDays(1)) {
            // 逐日重新读取限额及其中位阈值，不沿用顺延前日期的历史上限。
            int morningMax = LhScheduleTimeUtil.getMorningMouldChangeLimit(context, date.toString());
            int middleMax = LhScheduleTimeUtil.getAfternoonMouldChangeLimit(context, date.toString());
            int dailyMax = LhScheduleTimeUtil.getDailyMouldChangeLimit(context, date.toString());

            int[] counts = context.getDailyMouldChangeCountMap().get(date.toString());
            int morning = Objects.nonNull(counts) ? counts[MORNING_INDEX] : 0;
            int middle = Objects.nonNull(counts) ? counts[AFTERNOON_INDEX] : 0;
            // 日额度独立于班次额度；用长整数避免求和或中位比较溢出。
            if ((long) morning + middle >= dailyMax || (morning >= morningMax && middle >= middleMax)) {
                this.logDecision(context, sku, machineCode, date, morning, middle, morningMax,
                        middleMax, dailyMax, null, "当日换模额度已满，继续判断下一日");
                continue;
            }
            boolean afternoon = morning >= morningMax
                    || (2L * morning > morningMax && 2L * middle <= morningMax);
            String reason = this.resolveRuleReason(morning, middle, morningMax);
            String shiftType = afternoon ? ShiftEnum.AFTERNOON_SHIFT.getCode() : ShiftEnum.MORNING_SHIFT.getCode();
            LhShiftConfigVO candidate = this.findShift(shifts, date, shiftType);
            // 非默认参数下，中班上限可能小于早班中位值；不允许区间规则越过硬上限。
            if ((afternoon ? middle >= middleMax : morning >= morningMax) || Objects.isNull(candidate)
                    || !candidate.isAllowMouldChange() || LhScheduleTimeUtil.isNoMouldChangeTime(context, candidate.getShiftStartDateTime())) {
                this.logDecision(context, sku, machineCode, date, morning, middle, morningMax,
                        middleMax, dailyMax, null, "规则班次无合法额度或不在可换模窗口，继续下一日");
                continue;
            }
            OffMachineDecision decision = new OffMachineDecision();
            decision.setPlanDate(date);
            decision.setShift(shiftType);
            decision.setOffMachineTime(candidate.getShiftStartDateTime());
            decision.setOccupancyEndTime(candidate.getShiftStartDateTime());
            decision.setReason(reason);
            // 生产量已在节点内结束仍可能存在胶囊等物理占用，交接不可早于真实可释放时间。
            if (Objects.nonNull(request) && !this.isPhysicalReleaseFeasible(context, request, decision)) {
                this.logDecision(context, sku, machineCode, date, morning, middle, morningMax,
                        middleMax, dailyMax, null, "规则班次早于机台真实可交接时间，继续判断下一日");
                continue;
            }
            this.logDecision(context, sku, machineCode, date, morning, middle, morningMax,
                    middleMax, dailyMax, decision, reason);
            return decision;
        }
        OffMachineDecision decision = new OffMachineDecision();
        decision.setOccupancyEndTime(shifts.stream().map(LhShiftConfigVO::getShiftEndDateTime)
                .max(Date::compareTo).get());
        decision.setReason("排程窗口内无合法下机班次，保留机台占用至窗口末端");
        String detail = String.format("SKU=%s, 机台=%s, 最终下机时间=无, 占用截止=%s, 原因=%s",
                sku.getMaterialCode(), machineCode, LhScheduleTimeUtil.formatDateTime(decision.getOccupancyEndTime()),
                decision.getReason());
        log.info("按时间下机窗口未安排, {}", detail);
        PriorityTraceLogHelper.appendProcessLog(context, "按时间下机窗口未安排", detail);
        return decision;
    }

    /**
     * 余量收尾后、统一扣账前执行；原选机顺序和物理机台分组均不改变。
     * @param context 本批上下文
     */
    public void resolveAndApply(LhScheduleContext context) {
        if (CollectionUtils.isEmpty(context.getTimedMachineOffRequests())) {
            return;
        }
        if (context.isContinuousDailyQuotaSynced() || !CollectionUtils.isEmpty(context.getTimedMachineOffDecisionMap())) {
            throw new IllegalStateException("按时间下机必须在统一扣账前执行且不可重复提交");
        }
        for (TimedMachineOffRequest request : context.getTimedMachineOffRequests()) {
            if (this.hasFixedRelease(context, request)) {
                PriorityTraceLogHelper.appendProcessLog(context, "按时间下机硬边界保留",
                        "SKU=" + request.getSourceSku().getMaterialCode() + ", 机台="
                                + request.getProfile().getOriginals().get(0).getLhMachineCode()
                                + ", 原因=已有强制交替、故障、喷砂、精度或后续占用，不搬动既定边界");
                continue;
            }
            OffMachineDecision decision = this.resolveOffMachineTime(context, request.getStartDate(),
                    request.getProfile().getOriginals().get(0).getLhMachineCode(), request.getSourceSku(), request);
            this.applyProductionBoundary(context, request, decision);
        }
    }

    /**
     * 检查候选节点能否完成物理交接；复用画像内的胶囊、停机、单控释放约束。
     * @param context 上下文
     * @param request 清零前画像
     * @param decision 待检查的下机节点
     * @return 实际物理可释放时间不晚于候选节点
     */
    private boolean isPhysicalReleaseFeasible(LhScheduleContext context, TimedMachineOffRequest request,
            OffMachineDecision decision) {
        LhShiftConfigVO firstMorning = this.findShift(context.getScheduleWindowShifts(), request.getStartDate(),
                ShiftEnum.MORNING_SHIFT.getCode());
        if (Objects.isNull(firstMorning)) {
            throw new IllegalStateException("按时间下机需求日缺少早班配置");
        }
        int quantity = this.resolveProfileQuantity(context, request, firstMorning, decision);
        Date physicalReady = request.getProfile().switchReadyTime(quantity);
        return Objects.nonNull(physicalReady) && !physicalReady.after(decision.getOffMachineTime());
    }

    /**
     * 按真实画像补齐等待期间生产，只改需求日起的班次；原前缀和余量收尾不受影响。
     * @param context 上下文
     * @param request 已选物理机台及清零前画像
     * @param decision 日期循环计算出的边界
     */
    private void applyProductionBoundary(LhScheduleContext context, TimedMachineOffRequest request,
            OffMachineDecision decision) {
        List<LhScheduleResult> originals = request.getProfile().getOriginals();
        List<LhScheduleResult> proposed = originals.stream()
                .map(result -> BeanUtil.copyProperties(result, LhScheduleResult.class)).collect(Collectors.toList());
        List<LhShiftConfigVO> shifts = context.getScheduleWindowShifts();
        LhShiftConfigVO firstMorning = this.findShift(shifts, request.getStartDate(), ShiftEnum.MORNING_SHIFT.getCode());
        if (Objects.isNull(firstMorning)) {
            throw new IllegalStateException("按时间下机需求日缺少早班配置");
        }
        // 原画像提供真实产能，SKU共享余量限制实际补量；不得挤占其他已保留机台或虚增尾量。
        int quantity = this.resolveProfileQuantity(context, request, firstMorning, decision);
        request.getProfile().writePlan(proposed, quantity);
        for (int side = 0; side < originals.size(); side++) {
            LhScheduleResult result = originals.get(side);
            LhScheduleResult proposal = proposed.get(side);
            for (LhShiftConfigVO shift : shifts) {
                if (shift.getShiftStartDateTime().before(firstMorning.getShiftStartDateTime())) {
                    continue;
                }
                int index = shift.getShiftIndex();
                ShiftFieldUtil.setShiftPlanQty(result, index, ShiftFieldUtil.getShiftPlanQty(proposal, index),
                        ShiftFieldUtil.getShiftStartTime(proposal, index), ShiftFieldUtil.getShiftEndTime(proposal, index));
                ShiftFieldUtil.appendShiftAnalysis(result, index, TIMED_OFF_ANALYSIS);
            }
            context.getTimedMachineOffDecisionMap().put(result, decision);
            // 原对象身份保留，来源 SKU 与机台分配索引继续引用同一份结果。
            ResultShiftTimeUtil.refreshSummary(context, result);
            this.updateReleaseBoundary(context, result, decision);
            context.getEndingFillAllowedOverQtyMap().remove(result);
            context.getEndingFillBeforeQtyMap().remove(result);
            context.getSharedEmbryoEndingStaggerAllowedOverQtyMap().remove(result);
        }
    }

    /**
     * 保留需求日前实际前缀，仅用同SKU未分配余量补足等待下机的生产，双侧按画像统一归整。
     * @param context 上下文
     * @param request 当前物理机台请求
     * @param firstMorning 需求日早班
     * @param decision 决策边界
     * @return 用于原画像写入的累计数量，不改变正式账本
     */
    private int resolveProfileQuantity(LhScheduleContext context, TimedMachineOffRequest request,
            LhShiftConfigVO firstMorning, OffMachineDecision decision) {
        List<LhScheduleResult> own = request.getProfile().getOriginals();
        SkuScheduleDTO sku = request.getSourceSku();
        long otherQty = context.getScheduleResultList().stream().filter(result -> !own.contains(result))
                .filter(result -> StringUtils.equals(sku.getMaterialCode(), result.getMaterialCode())
                        && StringUtils.equals(sku.getProductStatus(), result.getProductStatus()))
                .mapToLong(ShiftFieldUtil::resolveScheduledQty).sum();
        long prefixQty = own.stream().mapToLong(result -> context.getScheduleWindowShifts().stream()
                .filter(shift -> shift.getShiftStartDateTime().before(firstMorning.getShiftStartDateTime()))
                .mapToLong(shift -> {
                    Integer qty = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
                    return Objects.nonNull(qty) ? qty : 0;
                }).sum()).sum();
        long tailBudget = Math.max(0L, this.quantityResolver.previewProductionRemainingQty(context, sku)
                - otherQty - prefixQty);
        long profileLimit = request.getProfile().quantityUntil(firstMorning.getShiftStartDateTime()) + tailBudget;
        return request.getProfile().floorQuantity((int) Math.min(profileLimit,
                request.getProfile().quantityUntil(decision.getOccupancyEndTime())));
    }

    /**
     * 保留强制下机、已提交其他物料及同物料多状态专用链的原有时间语义。
     * @param context 上下文
     * @param request 已选物理机台
     * @return 是否有不能搬动的既定时间边界
     */
    private boolean hasFixedRelease(LhScheduleContext context, TimedMachineOffRequest request) {
        List<LhScheduleResult> own = request.getProfile().getOriginals();
        for (LhScheduleResult result : own) {
            String code = result.getLhMachineCode();
            MachineScheduleDTO machine = context.getMachineScheduleMap().get(code);
            if (context.getPreviousAlternateReleaseEventMap().containsKey(code)
                    || context.getOnlySandBlastContinuationReleaseWindowMap().containsKey(code)
                    || context.getContinuationTemporaryFaultTransferEventMap().containsKey(code)
                    || context.isContinuousStopHoldMachine(code)
                    || context.getContinuationSurplusEndingAllocatedResults().contains(result)
                    || (Objects.nonNull(machine) && !CollectionUtils.isEmpty(machine.getMaintenanceWindowList())
                    && machine.getMaintenanceWindowList().stream().anyMatch(window -> window.isForceDown()))) {
                return true;
            }
            if (context.getScheduleResultList().stream().anyMatch(other -> !own.contains(other)
                    && StringUtils.equals(code, other.getLhMachineCode())
                    && (ShiftFieldUtil.resolveScheduledQty(other) > 0 || Objects.nonNull(other.getMouldChangeStartTime())))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 统一扣账及机台状态汇总后发布事实，避免零量清理或末班反算覆盖真实交接时间。
     * @param context 已完成统一续作扣账的上下文
     */
    public void publishReleasedState(LhScheduleContext context) {
        for (TimedMachineOffRequest request : context.getTimedMachineOffRequests()) {
            for (LhScheduleResult result : request.getProfile().getOriginals()) {
                OffMachineDecision decision = context.getTimedMachineOffDecisionMap().get(result);
                if (Objects.isNull(decision)) {
                    continue;
                }
                this.updateReleaseBoundary(context, result, decision);
                MachineScheduleDTO machine = context.getMachineScheduleMap().get(result.getLhMachineCode());
                machine.setEstimatedEndTime(decision.getOccupancyEndTime());
                machine.setCurrentMaterialCode(result.getMaterialCode());
                machine.setCurrentMaterialDesc(result.getMaterialDesc());
                machine.setEnding(Objects.nonNull(decision.getOffMachineTime()));
                LhMouldCodeUtil.resolveInMachineMouldCodeSet(context, result.getLhMachineCode()).forEach(mould ->
                        context.getPreScheduledMouldReleaseTimeMap().put(mould, decision.getOccupancyEndTime()));
                this.refreshCapacityAndIndex(context, request, result, decision);
                if (ShiftFieldUtil.resolveScheduledQty(result) > 0) {
                    result.setSpecEndTime(decision.getOccupancyEndTime());
                    result.setTdaySpecEndTime(decision.getOccupancyEndTime());
                }
            }
        }
    }

    /**
     * 同步释放班次及分组释放日期；窗口未安排时禁止提前释放。
     * @param context 上下文
     * @param result 原机台结果
     * @param decision 最终决策
     */
    private void updateReleaseBoundary(LhScheduleContext context, LhScheduleResult result, OffMachineDecision decision) {
        int lastProductionShift = context.getScheduleWindowShifts().stream()
                .filter(shift -> !shift.getShiftEndDateTime().after(decision.getOccupancyEndTime()))
                .mapToInt(LhShiftConfigVO::getShiftIndex).max().orElse(0);
        context.registerContinuousReducedMachineReleaseBoundary(result.getLhMachineCode(), lastProductionShift);
        context.markContinuousStopHoldMachineReleased(result.getLhMachineCode());
        // 该键与既有降模分组一致，正式补偿阶段从新的释放业务日评估需求。
        String groupKey = MonthPlanDateResolver.buildMaterialStatusKey(result.getMaterialCode(), result.getProductStatus());
        LocalDate releaseDate = this.toLocalDate(decision.getOccupancyEndTime());
        LocalDate previousDate = context.getReducedContinuationGroupLastReleaseDate(groupKey);
        if (Objects.isNull(previousDate) || releaseDate.isAfter(previousDate)) {
            context.registerReducedContinuationGroupLastReleaseDate(groupKey, releaseDate);
        }
    }

    /**
     * 以资源占用而非最后正量刷新剩余容量；下机前闲置也不能提前交给后料。
     * @param context 上下文
     * @param request 原物理产能画像
     * @param result 当前侧结果
     * @param decision 实际占用边界
     */
    private void refreshCapacityAndIndex(LhScheduleContext context, TimedMachineOffRequest request,
            LhScheduleResult result, OffMachineDecision decision) {
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(result.getLhMachineCode());
        int[] machineCapacity = machine.getShiftRemainingCapacity();
        int[] contextCapacity = context.getMachineShiftCapacityMap().get(result.getLhMachineCode());
        List<LhShiftConfigVO> shifts = context.getScheduleWindowShifts();
        for (int position = 0; position < shifts.size(); position++) {
            LhShiftConfigVO shift = shifts.get(position);
            int remaining = shift.getShiftStartDateTime().before(decision.getOccupancyEndTime()) ? 0
                    : request.getProfile().getCapacities()[position] / request.getProfile().getOriginals().size();
            int index = shift.getShiftIndex();
            if (Objects.nonNull(machineCapacity) && index < machineCapacity.length) {
                machineCapacity[index] = remaining;
            }
            if (Objects.nonNull(contextCapacity) && index < contextCapacity.length) {
                contextCapacity[index] = remaining;
            }
            LocalDate date = this.toLocalDate(shift.getWorkDate());
            boolean occupied = shifts.stream().filter(candidate -> Objects.equals(candidate.getWorkDate(), shift.getWorkDate()))
                    .map(candidate -> ShiftFieldUtil.getShiftPlanQty(result, candidate.getShiftIndex()))
                    .filter(Objects::nonNull).anyMatch(quantity -> quantity > 0);
            if (occupied) {
                context.recordScheduledMachine(date, result.getStructureName(), result.getMaterialCode(),
                        result.getProductStatus(), result.getLhMachineCode());
            } else {
                context.removeScheduledMachine(date, result.getStructureName(), result.getMaterialCode(),
                        result.getProductStatus(), result.getLhMachineCode());
            }
        }
        ResultShiftTimeUtil.refreshMachine(context, result);
        // 零量行已被清理时，通用汇总不能推翻独立释放事实。
        machine.setEstimatedEndTime(decision.getOccupancyEndTime());
    }

    /** @param morning 早班占用 @param middle 中班占用 @param morningMax 早班上限 @return 与优先级一致的命中原因 */
    private String resolveRuleReason(int morning, int middle, int morningMax) {
        if (morning >= morningMax) {
            return "早班已满，优先中班";
        }
        if (2L * morning <= morningMax) {
            return "早班不超过中位值";
        }
        if (2L * middle <= morningMax) {
            return "早班超过中位值，中班不超过中位值";
        }
        return "早中班均超过中位值，早班未满";
    }

    /** @param shifts 实际班次 @param date 业务日 @param type 班次类型 @return 对应班次，窗口不存在则为空 */
    private LhShiftConfigVO findShift(List<LhShiftConfigVO> shifts, LocalDate date, String type) {
        return shifts.stream().filter(shift -> date.equals(this.toLocalDate(shift.getWorkDate()))
                && StringUtils.equals(type, shift.getShiftType())).findFirst().orElse(null);
    }

    /** @param time 业务时间 @return 所属本地日期 */
    private LocalDate toLocalDate(Date time) {
        return time.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /** @param context 上下文 @param sku 来源SKU @param machine 机台 @param date 判断日 @param morning 早班占用
     * @param middle 中班占用 @param morningMax 早班上限 @param middleMax 中班上限 @param dailyMax 每日上限
     * @param decision 最终决策，顺延时为空 @param reason 命中规则 */
    private void logDecision(LhScheduleContext context, SkuScheduleDTO sku, String machine, LocalDate date,
            int morning, int middle, int morningMax, int middleMax, int dailyMax,
            OffMachineDecision decision, String reason) {
        String detail = String.format("工厂=%s, 批次=%s, SKU=%s, 状态=%s, 机台=%s, 判断日期=%s, "
                        + "早班=%s/%s, 中班=%s/%s, 每日上限=%s, 中位阈值=%s, 最终班次=%s, 最终下机时间=%s, 规则=%s",
                context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(), sku.getProductStatus(), machine,
                date, morning, morningMax, middle, middleMax, dailyMax, morningMax / 2.0,
                Objects.nonNull(decision) ? decision.getShift() : null,
                Objects.nonNull(decision) ? LhScheduleTimeUtil.formatDateTime(decision.getOffMachineTime()) : null, reason);
        log.info("按时间下机班次决策, {}", detail);
        PriorityTraceLogHelper.appendProcessLog(context, "按时间下机班次决策", detail);
    }
}
