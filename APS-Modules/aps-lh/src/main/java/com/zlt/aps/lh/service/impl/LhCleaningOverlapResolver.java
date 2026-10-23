package com.zlt.aps.lh.service.impl;
import com.zlt.aps.lh.api.domain.dto.*;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.*;
import com.zlt.aps.lh.util.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import java.util.*;
import java.util.stream.Collectors;
/**
 * 只读清洗重叠协调器，统一返回实际事件、显式处置与本场景容量窗口。
 * 容量区间并集继续由现有内核计算，本组件不分配资源或消费数量账本。
 *
 * @author APS
 * @since 2026/10/19
 */
@Component
public class LhCleaningOverlapResolver {
    /**
     * 解析机台尚须执行的在机清洗，不将离机事件重新放回容量约束。
     *
     * @param context 当前批次
     * @param machineCode 运行侧机台
     * @return 当前仍须执行的在机窗口
     */
    public List<MachineCleaningWindowDTO> resolveMachineWindows(LhScheduleContext context, String machineCode) {
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
        if (machine == null || CollectionUtils.isEmpty(machine.getCleaningWindowList())) {
            return new ArrayList<>();
        }
        return CleaningDispositionResolver.resolveEffectiveWindows(context, machineCode,
                machine.getCleaningWindowList());
    }
    /**
     * 只读评估当前切换中的清洗；实际事件不会因容量过滤而丢失。
     *
     * @param context 当前批次
     * @param machineCode 运行侧机台
     * @param switchStart 切换开始时间，可为空
     * @param switchEnd 本场景真实切换结束时间
     * @param skipCapacity 当前候选已符合原收尾跳过条件
     * @return 各来源事件与本场景有效容量窗口
     */
    public CleaningPlanResult resolveForScheduling(LhScheduleContext context, String machineCode,
            Date switchStart, Date switchEnd, boolean skipCapacity) {
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
        List<MachineCleaningWindowDTO> source = machine == null || machine.getCleaningWindowList() == null
                ? Collections.emptyList() : machine.getCleaningWindowList();
        MachineCleaningWindowDTO detached = context.getContinuationSandBlastWindowMap().get(machineCode);
        if (source.isEmpty() && (detached == null || !detached.isOffMachineCleaning())) {
            return CleaningPlanResult.empty();
        }
        List<CleaningPlanResult.Event> events = new ArrayList<>(source.size() + 1);
        List<MachineCleaningWindowDTO> active = new ArrayList<>(source.size());
        boolean offMachine = detached != null && detached.isOffMachineCleaning();
        // 一次扫描同时生成事件和容量输入，避免同一候选重复查询正式处置或构造中间集合。
        for (MachineCleaningWindowDTO window : source) {
            if (window == null) {
                continue;
            }
            CleaningPlanResult.Event event = this.resolveEvent(context, machineCode, window);
            if (!offMachine || !Objects.equals(window.getSourcePlanId(), detached.getSourcePlanId())) {
                events.add(event);
            }
            if (!skipCapacity && event.getStatus() == CleaningPlanStatus.SCHEDULED) {
                active.add(window);
            }
        }
        if (offMachine) {
            events.add(this.resolveEvent(context, machineCode, detached));
        }
        List<MachineCleaningWindowDTO> capacity = active.isEmpty() ? Collections.emptyList()
                : MachineCleaningOverlapUtil.excludeOverlapWindows(active, switchStart, switchEnd);
        return new CleaningPlanResult(events, capacity);
    }
    /**
     * 为当前物料追加同 SKU 置换准备窗口，其他物料只承接机台占用而不领取置换首检。
     *
     * @param context 当前批次
     * @param machineCode 运行侧机台
     * @param materialCode 当前计算物料
     * @param productStatus 当前产品状态
     * @param windows 原有容量窗口
     * @return 包含置换准备的独立容量窗口集合，无置换时复用原集合
     */
    public List<MachineCleaningWindowDTO> appendReplacementCapacityWindow(LhScheduleContext context,
            String machineCode, String materialCode, String productStatus, List<MachineCleaningWindowDTO> windows) {
        MachineCleaningWindowDTO replacement = context.getContinuationSandBlastWindowMap().get(machineCode);
        if (replacement == null || replacement.getReplacementReadyTime() == null) {
            return windows;
        }
        CleaningPlanResult.Event event = this.resolveEvent(context, machineCode, replacement);
        if (event.getStatus() != CleaningPlanStatus.SCHEDULED) {
            return windows;
        }
        boolean sameSku = StringUtils.equals(materialCode, replacement.getContinuationMaterialCode())
                && StringUtils.equals(productStatus, replacement.getContinuationProductStatus());
        MachineCleaningWindowDTO projection = event.getTimeline().toReplacementCapacityWindow(replacement, sameSku);
        if (projection == null) {
            return windows;
        }
        List<MachineCleaningWindowDTO> capacity = new ArrayList<>(windows.size() + 1);
        capacity.addAll(windows);
        capacity.add(projection);
        return capacity;
    }
    /**
     * 由正式结果解析单个来源事件，候选换模本身不能取消清洗。
     *
     * @param context 当前批次
     * @param machineCode 当前运行侧
     * @param window 实际清洗事实
     * @return 显式处置及时间结果
     */
    public CleaningPlanResult.Event resolveEvent(LhScheduleContext context, String machineCode,
            MachineCleaningWindowDTO window) {
        CleaningDispositionResolver.Disposition disposition =
                CleaningDispositionResolver.resolve(context, machineCode, window).getDisposition();
        CleaningPlanStatus status = CleaningPlanStatus.SCHEDULED;
        String reason = window.isOffMachineCleaning() ? "离机清洗" : "独立清洗";
        if (disposition == CleaningDispositionResolver.Disposition.ENDING_CANCELLED) {
            status = CleaningPlanStatus.ENDING_CANCELLED;
            reason = "来源SKU收尾取消清洗";
        } else if (disposition == CleaningDispositionResolver.Disposition.MOULD_CHANGE_CANCELLED) {
            status = CleaningPlanStatus.MOULD_CHANGE_CANCELLED;
            reason = "已提交正规换模取消清洗";
        }
        CleaningPlanResult.Event published = context.getCleaningPlanRuntimeState().getAppliedEventMap()
                .get(window.getSourcePlanId() + "|" + window.getLhCode());
        return published != null && published.matches(window, status, reason) ? published
                : new CleaningPlanResult.Event(window, status, reason);
    }
    /**
     * 按原结果场景确定切换区间，不以首检所在班次截断真实准备时长。
     *
     * @param context 当前批次
     * @param result 当前排程结果
     * @param firstProduction 首个有量班次起点
     * @param typeBlock 是否使用换活字块时长；续作沿用原正规换模时长
     * @return 本次只读清洗结果
     */
    public CleaningPlanResult resolveForResult(LhScheduleContext context, LhScheduleResult result,
            Date firstProduction, boolean typeBlock) {
        if (result == null) {
            return CleaningPlanResult.empty();
        }
        Date end = result.getMouldChangeStartTime() == null ? firstProduction
                : LhScheduleTimeUtil.addHours(result.getMouldChangeStartTime(), typeBlock
                        ? LhScheduleTimeUtil.getTypeBlockChangeTotalHours(context)
                        : LhScheduleTimeUtil.getMouldChangeTotalHours(context));
        CleaningPlanResult cleaning = this.resolveForScheduling(context, result.getLhMachineCode(),
                result.getMouldChangeStartTime(), end, CleaningScheduleRuleUtil.shouldSkipCleaningByResultEnding(result));
        MachineCleaningWindowDTO replacement = context.getContinuationSandBlastWindowMap().get(result.getLhMachineCode());
        if (replacement == null || replacement.getReplacementReadyTime() == null) {
            return cleaning;
        }
        List<MachineCleaningWindowDTO> original = cleaning.getEffectiveWindows();
        List<MachineCleaningWindowDTO> capacity = this.appendReplacementCapacityWindow(context,
                result.getLhMachineCode(), result.getMaterialCode(), result.getProductStatus(), original);
        return capacity == original ? cleaning : new CleaningPlanResult(cleaning.getEvents(), capacity);
    }
    /**
     * 清洗与换活字块实际重叠时，开产时间取两者最晚结束时间。
     *
     * @param context 当前批次上下文，用于过滤已取消清洗
     * @param machine 机台
     * @param switchStartTime 换活字块开始时间
     * @param productionStartTime 原换活字块完成后的开产时间
     * @return 合并清洗重叠后的开产时间
     */
    public Date resolveTypeBlockProductionStartTime(LhScheduleContext context,
                                                           MachineScheduleDTO machine,
                                                           Date switchStartTime,
                                                           Date productionStartTime) {
        if (Objects.isNull(machine)
                || Objects.isNull(switchStartTime)
                || Objects.isNull(productionStartTime)
                || !switchStartTime.before(productionStartTime)) {
            return productionStartTime;
        }
        List<MachineCleaningWindowDTO> effectiveCleaningWindows =
                this.resolveMachineWindows(context, machine.getMachineCode());
        Date resolvedStartTime = productionStartTime;
        Date switchEndTime = LhScheduleTimeUtil.addHours(switchStartTime,
                LhScheduleTimeUtil.getTypeBlockChangeTotalHours(context));
        Date combinedReadyTime = LhCleaningRepairOverlapPolicy.resolvePreparationEnd(
                context, machine.getMachineCode(), switchStartTime, switchEndTime, true);
        if (combinedReadyTime != null && combinedReadyTime.after(resolvedStartTime)) {
            resolvedStartTime = combinedReadyTime;
        }
        for (MachineCleaningWindowDTO cleaningWindow : effectiveCleaningWindows) {
            MachineCleaningWindowDTO combinedWindow = LhCleaningRepairOverlapPolicy.projectWindow(
                    context, cleaningWindow, switchStartTime, switchEndTime, true);
            Date cleaningOccupationEndTime = MachineCleaningOverlapUtil
                    .resolveEffectiveCleanEndTime(combinedWindow == null ? cleaningWindow : combinedWindow);
            if (Objects.isNull(cleaningWindow)
                    || Objects.isNull(cleaningWindow.getCleanStartTime())
                    || Objects.isNull(cleaningOccupationEndTime)
                    || !cleaningWindow.getCleanStartTime().before(cleaningOccupationEndTime)) {
                continue;
            }
            // 只有清洗与换活字块实际相交时才并行取最大结束时间，未重叠场景不改变原开产时间。
            if (cleaningWindow.getCleanStartTime().before(productionStartTime)
                    && cleaningOccupationEndTime.after(switchStartTime)
                    && cleaningOccupationEndTime.after(resolvedStartTime)) {
                resolvedStartTime = cleaningOccupationEndTime;
            }
        }
        return resolvedStartTime;
    }
    /**
     * 精度组合扩展口，保持精度现有原始窗口及物理机台范围，拒绝仍由精度调用方执行。
     *
     * @param context 当前批次
     * @param physicalMachine 物理机台
     * @param start 精度组合起点
     * @param end 精度恢复时间
     * @return 真实相交清洗的未支持结果；无相交事件时返回空集合
     */
    public List<CleaningPlanResult.Event> resolveWithPrecision(LhScheduleContext context,
            String physicalMachine, Date start, Date end) {
        return context.getMachineScheduleMap().values().stream().filter(Objects::nonNull)
                .filter(side -> StringUtils.equals(
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(physicalMachine),
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(side.getMachineCode())))
                .flatMap(side -> side.getCleaningWindowList().stream()).filter(Objects::nonNull)
                .filter(window -> this.overlaps(start, end, window.getCleanStartTime(), window.getReadyTime()))
                .map(window -> new CleaningPlanResult.Event(window, CleaningPlanStatus.UNSUPPORTED,
                        "精度并行与清洗组合尚未接入")).collect(Collectors.toList());
    }
    /**
     * 检查精度当前运行侧的清洗组合，保持原逐侧诊断顺序。
     *
     * @param side 当前运行侧
     * @param start 精度组合开始
     * @param end 精度恢复时间
     * @return 尚未接入的清洗组合结论
     */
    public List<CleaningPlanResult.Event> resolveWithPrecisionForSide(MachineScheduleDTO side, Date start, Date end) {
        if (side.getCleaningWindowList().isEmpty()) {
            return Collections.emptyList();
        }
        return side.getCleaningWindowList().stream().filter(Objects::nonNull)
                .filter(window -> this.overlaps(start, end, window.getCleanStartTime(), window.getReadyTime()))
                .map(window -> new CleaningPlanResult.Event(window, CleaningPlanStatus.UNSUPPORTED,
                        "精度并行与清洗组合尚未接入")).collect(Collectors.toList());
    }
    /**
     * 维修组合扩展口；当前继续使用容量内核的占用并集，不新增组合拒绝。
     *
     * @param context 当前批次
     * @param machineCode 运行侧
     * @param start 当前切换起点
     * @param end 当前切换终点
     * @return 沿用原占用路径的清洗结果
     */
    public CleaningPlanResult resolveWithRepair(LhScheduleContext context, String machineCode, Date start, Date end) {
        return this.resolveForScheduling(context, machineCode, start, end, false);
    }
    /**
     * 多事项组合扩展口；不把已有两两规则拼接成新的准入或取消条件。
     *
     * @param context 当前批次
     * @param machineCode 运行侧
     * @param start 当前切换起点
     * @param end 当前切换终点
     * @return 沿用原调用路径的清洗结果
     */
    public CleaningPlanResult resolveMultipleOverlaps(
            LhScheduleContext context, String machineCode, Date start, Date end) {
        return this.resolveForScheduling(context, machineCode, start, end, false);
    }
    /**
     * 判断两个实际区间严格相交，端点相接不算重叠。
     *
     * @param start 组合开始
     * @param end 组合结束
     * @param otherStart 清洗开始
     * @param otherEnd 清洗恢复
     * @return 是否严格相交
     */
    private boolean overlaps(Date start, Date end, Date otherStart, Date otherEnd) {
        return start != null && end != null && otherStart != null && otherEnd != null
                && start.before(end) && otherStart.before(otherEnd)
                && start.before(otherEnd) && otherStart.before(end);
    }
}
